package org.alfresco.repo.domain.node.ibatis.cqrs;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.alfresco.repo.domain.node.AbstractNodeDAOImpl;
import org.alfresco.repo.domain.node.NodeEntity;
import org.alfresco.repo.domain.node.ibatis.cqrs.utils.Context;
import org.alfresco.repo.domain.node.ibatis.cqrs.utils.CqrsContext;
import org.alfresco.repo.domain.node.ibatis.cqrs.utils.Logger;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Service which provides CQRS functionality
 *
 * Created by mmuller on 26/03/2018.
 */
public class IbatisNodeInsertCqrsServiceImpl implements CqrsService
{
    /** simple in-memory event store */
    private ObservableList<Event> eventStore;

    private LinkedList<CqrsWriter> writers;
    private LinkedList<CqrsReader> readers;

    private IbatisNodeInsertCommandHandler ibatisNodeInsertCommandHandler;

    /** context in which the service is called */
    private Context context;

    /** For using ibatis it needs the NodeDAOImpl */
    private AbstractNodeDAOImpl nodeDAOImpl;

//    /** In-memory storage for id returns after insert. Avoids sql query for it */
//    private HashMap<UUID, Long> nodeIdMap;

    public IbatisNodeInsertCqrsServiceImpl(Context context)
    {
        Logger.logDebug("Init CQRS service", context);

        this.context = context;
        eventStore = FXCollections.observableList(new ArrayList<Event>());
        ibatisNodeInsertCommandHandler = new IbatisNodeInsertCommandHandler(this);

//        nodeIdMap = new HashMap<>();

        addWriters();
        addReaders();
    }

    private void addWriters()
    {
        // add writer 1
        IbatisNodeInsertCqrsWriter1 writer1 = new IbatisNodeInsertCqrsWriter1("writer1", this);
        Logger.logDebug("Add writer: " + writer1.getName() + " as listener", context);

        writers.add(writer1);

        eventStore.addListener((ListChangeListener.Change<? extends Event> c) -> {
            c.next();
            writer1.notifyWriter((List<Event>) c.getAddedSubList());
        });

        // add writer 2
    }

    private void addReaders()
    {
        // add reader 1 . Reads from NodeEntity the id
        IbatisNodeInsertCqrsReader1 reader1 = new IbatisNodeInsertCqrsReader1("reader1", this);
        Logger.logDebug("Add reader: " + reader1.getName() + " as listener", context);

        readers.add(reader1);

        eventStore.addListener((ListChangeListener.Change<? extends Event> c) -> {
            c.next();
            reader1.notifyReader((List<Event>) c.getAddedSubList());
        });

        // add reader 2

        // add reader 3
    }

    public void executeCommand(Object ibatisObject)
    {
        Logger.logDebug("Execute ibatis command with ibatisObject: " + ibatisObject.toString(), context);

        CommandHandlerResult result = ibatisNodeInsertCommandHandler.handleCommand(ibatisObject, context);
        Logger.logDebug("Handle command result: " + result, context);
        // execute was accepted
        if(result.isAccepted())
        {
            Logger.logDebug("Ibatis command was accepted", context);

            // save in event store with addition create timestamp an even id
            Event e = new Event(result.getDiffObject());
            eventStore.add(e);

            Logger.logDebug("Following event was added to event store: " + e.toString(), context);
        }
        else
        {
            Logger.logDebug("Ibatis command was not accepted", context);
        }
    }

    // TODO return result object
    public String query(String readerName, String col, NodeEntity node) throws IllegalAccessException
    {
        for(CqrsReader reader : readers)
        {
            if(reader.getName().equalsIgnoreCase(readerName))
            {
                return reader.getValue(col, node);
            }
        }
        throw new IllegalAccessException("Reader with name: " + readerName + " don't exists");
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

    public static void main(String[] args)
    {
        CqrsContext context = new CqrsContext();
        IbatisNodeInsertCqrsServiceImpl ibatisCqrsService = new IbatisNodeInsertCqrsServiceImpl(context);

        String[] diffStrings = {"diff1", "diff2", "diff3"};

        ibatisCqrsService.executeCommand(diffStrings[0]);
        ibatisCqrsService.executeCommand(diffStrings[1]);
        ibatisCqrsService.executeCommand(diffStrings[2]);
    }
}
