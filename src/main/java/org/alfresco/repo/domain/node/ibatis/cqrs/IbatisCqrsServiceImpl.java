package org.alfresco.repo.domain.node.ibatis.cqrs;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.alfresco.repo.domain.node.NodeDAO;
import org.alfresco.repo.domain.node.ibatis.NodeDAOImpl;
import org.alfresco.repo.domain.node.ibatis.cqrs.utils.Context;
import org.alfresco.repo.domain.node.ibatis.cqrs.utils.CqrsContext;
import org.alfresco.repo.domain.node.ibatis.cqrs.utils.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Service which provides CQRS functionality
 *
 * Created by mmuller on 26/03/2018.
 */
public class IbatisCqrsServiceImpl implements CqrsService
{
    /** simple in-memory event store */
    private ObservableList<Event> eventStore ;

    private IbatisCommandHandler ibatisCommandHandler;

    /** context in which the service is called */
    private Context context;

    /** For using ibatis it needs the NodeDAOImpl */
    private NodeDAOImpl nodeDAOImpl;

    public IbatisCqrsServiceImpl(Context context)
    {
        Logger.logDebug("Init CQRS service", context);

        this.context = context;
        eventStore = FXCollections.observableList(new ArrayList<Event>());
        ibatisCommandHandler = new IbatisCommandHandler();

        addWriters();
        // TODO add Readers ...
    }

    private void addWriters()
    {
        IbatisCqrsWriter writer = new IbatisCqrsWriter("Ibatis CQRS Writer", this);
        Logger.logDebug("Add writer: " + writer.getName() + " as listener", context);

        eventStore.addListener((ListChangeListener.Change<? extends Event> c) -> {
            c.next();
            writer.notifyWriter((List<Event>) c.getAddedSubList());
        });
    }

    public void executeCommand(Object ibatisObject, Context context)
    {
        Logger.logDebug("Execute ibatis command with ibatisObject: " + ibatisObject.toString(), context);

        CommandHandlerResult result = ibatisCommandHandler.handleCommand(ibatisObject);
        Logger.logDebug("Handle command result: " + result, context);
        // execute was accepted
        if(result.isAccepted())
        {
            Logger.logDebug("Ibatis command was axcepted", context);

            // save in event store with addition create timestamp an even id
            Event e = new Event(result.getDiffObject());
            eventStore.add(e);

            Logger.logDebug("Following event was added to event store: " + e.toString(), context);
        }
        else
        {
            Logger.logDebug("Ibatis command was not excepted", context);
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

    public void setNodeDAOImpl(NodeDAOImpl nodeDAOImpl)
    {
        this.nodeDAOImpl = nodeDAOImpl;
    }

    public NodeDAOImpl getNodeDAOImpl()
    {
        return nodeDAOImpl;
    }

    public static void main(String[] args)
    {
        CqrsContext context = new CqrsContext();
        IbatisCqrsServiceImpl ibatisCqrsService = new IbatisCqrsServiceImpl(context);

        String[] diffStrings = {"diff1", "diff2", "diff3"};

        ibatisCqrsService.executeCommand(diffStrings[0], context);
        ibatisCqrsService.executeCommand(diffStrings[1], context);
        ibatisCqrsService.executeCommand(diffStrings[2], context);
    }
}
