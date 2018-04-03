/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2018 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

package org.alfresco.repo.domain.node.cqrs;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.alfresco.repo.domain.node.AbstractNodeDAOImpl;
import org.alfresco.repo.domain.node.cqrs.utils.Context;
import org.alfresco.repo.domain.node.cqrs.utils.CqrsContext;
import org.alfresco.repo.domain.node.cqrs.utils.Logger;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Service which provides CQRS functionality
 *
 * Created by mmuller on 26/03/2018.
 */
public class NodeInsertCqrsServiceImpl implements CqrsService
{
    /** simple in-memory event store */
    private ObservableList<Event> eventStore;

    private LinkedList<NodeInsertCqrsWriterAbstract> writers;
    private LinkedList<NodeInsertCqrsReaderAbstract> readers;

    private NodeInsertCommandHandler nodeInsertCommandHandler;

    /** context in which the service is called */
    private Context context;

    /** For using node insert stuff */
    private AbstractNodeDAOImpl nodeDAOImpl;

    public NodeInsertCqrsServiceImpl(Context context)
    {
        Logger.logDebug("Init CQRS service", context);

        this.context = context;
        eventStore = FXCollections.observableList(new ArrayList<Event>());
        nodeInsertCommandHandler = new NodeInsertCommandHandler(this);
        writers = new LinkedList<>();
        readers = new LinkedList<>();

        addReaders();
        addWriters();

        addCreateListener();

        Logger.logDebug("Init CQRS service finished waiting for commands and queries ...", context);
        Logger.logDebug("", context);
    }

    private void addWriters()
    {
        // add writer 1.
        NodeInsertCqrsWriter1 writer1 = new NodeInsertCqrsWriter1("writer1", this);
        Logger.logDebug("Add writer: " + writer1.getName() + " as event source writer", context);

        writers.add(writer1);

        // add writer 2
    }

    private void addReaders()
    {
        // add reader 1 . Reads from NodeEntity the id
        NodeInsertCqrsReader1 reader1 = new NodeInsertCqrsReader1("reader1", this);
        Logger.logDebug("Add reader: " + reader1.getName() + " as event source reader", context);

        readers.add(reader1);

        // add reader 2

        // add reader 3
    }

    private void addCreateListener()
    {
        // Listeners are implemented for Writers and Readers
        LinkedList<CqrsWriterAndReader> wr = new LinkedList<CqrsWriterAndReader>(writers);
        wr.addAll(readers);

        for (CqrsWriterAndReader writerOrReader : wr)
        {
            Logger.logDebug("Add Create Listener for: " + writerOrReader.getName(), context);
            eventStore.addListener((ListChangeListener.Change<? extends Event> c) ->
            {
                c.next();
                writerOrReader.onCreate((List<Event>) c.getAddedSubList());
            });
        }
    }

    public void executeCommand(Object commandObject)
    {
        Logger.logDebug("COMMAND detected:", context);
        if(commandObject!=null)
        {
            Logger.logDebug("Execute command with encapsuled object: " + commandObject.toString(), context);
        }
        else
        {
            Logger.logDebug("Execute command with encapsuled object: null", context);
        }

        CommandHandlerResult result = nodeInsertCommandHandler.handleCommand(commandObject, context);
        Logger.logDebug("Handle command result: " + result, context);
        // execute was accepted
        if(result.isAccepted())
        {
            Logger.logDebug("Command was accepted", context);

            // save in event store with addition create timestamp an even id
            Event e = new Event(result.getDiffObject());
            Logger.logDebug("Following event will be added to event store: " + e.toString(), context);
            eventStore.add(e);
        }
        else
        {
            Logger.logDebug("Command was not accepted. No adding to Event Source", context);
        }
        Logger.logDebug("COMMAND finished", context);
        Logger.logDebug("", context);
    }

    // TODO return result object
    public String query(String readerName, String col, Object node) throws IllegalAccessException
    {
        try
        {
            Logger.logDebug("", context);
            Logger.logDebug("QUERY detected:", context);
            for(NodeInsertCqrsReaderAbstract reader : readers)
            {
                if(reader.getName().equalsIgnoreCase(readerName))
                {
                    return reader.getValue(col, node);
                }
            }
            IllegalAccessException e = new IllegalAccessException("Reader with name: " + readerName + " don't exists");
            Logger.logError(e, context);
            throw e;
        }
        finally
        {
            Logger.logDebug("QUERY finished", context);
            Logger.logDebug("", context);
        }
    }

    public Context getContext()
    {
        return context;
    }

    public void setContext(Context context)
    {
        this.context = context;
    }

    public void setNodeDAOImpl(AbstractNodeDAOImpl nodeDAOImpl)
    {
        this.nodeDAOImpl = nodeDAOImpl;
    }

    public AbstractNodeDAOImpl getNodeDAOImpl()
    {
        return nodeDAOImpl;
    }

    public ObservableList<Event> getEventStore()
    {
        return eventStore;
    }

    public LinkedList<NodeInsertCqrsWriterAbstract> getWriters()
    {
        return writers;
    }

    public static void main(String[] args) throws IllegalAccessException
    {
        CqrsContext context = new CqrsContext();
        NodeInsertCqrsServiceImpl cqrsService = new NodeInsertCqrsServiceImpl(context);

        String[] cmds = {"cmd1", "cmd2", "cmd3"};

        cqrsService.query("reader1", "self", cmds[0]);

        cqrsService.executeCommand(cmds[0]);
        cqrsService.executeCommand(cmds[1]);
        cqrsService.executeCommand(null);
        cqrsService.executeCommand(cmds[2]);

        cqrsService.query("reader1", "self", cmds[0]);
        cqrsService.query("reader1", "self", cmds[1]);
        cqrsService.query("reader1", "self", cmds[2]);

        cqrsService.query("reader1", "notDefined123", cmds[2]);

        // col = "id" is defined but diffObject is from type String and node NodeEntity
        cqrsService.query("reader1", "id", cmds[2]);

        try
        {
            cqrsService.query("reader2", "id", cmds[2]);
            // it should fail because reader2 was never instantiated or added.
        }
        catch(IllegalAccessException e)
        {

        }
    }
}
