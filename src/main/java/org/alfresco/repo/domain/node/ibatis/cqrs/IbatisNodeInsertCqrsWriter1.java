package org.alfresco.repo.domain.node.ibatis.cqrs;

import org.alfresco.repo.domain.node.NodeEntity;
import org.alfresco.repo.domain.node.ibatis.cqrs.utils.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Writer which uses the ibatis insert node feature for storing the node in its database
 *
 * Created by mmuller on 26/03/2018.
 */
public class IbatisNodeInsertCqrsWriter1 extends IbatisNodeInsertCqrsWriterAbstract
{
    private String name;
    private IbatisNodeInsertCqrsServiceImpl ibatisCqrsService;

    public IbatisNodeInsertCqrsWriter1(String name, IbatisNodeInsertCqrsServiceImpl ibatisCqrsService)
    {
        super(name);
        this.name = name;
        this.ibatisCqrsService = ibatisCqrsService;
    }

    @Override
    public void onUpdate(List<Event> events)
    {
        // not implemented yet
    }

    @Override
    public void onCreate(List<Event> events)
    {
        Logger.logDebug("", ibatisCqrsService.getContext());
        Logger.logDebug(name + " detected " + events.size() + " new events:" , ibatisCqrsService.getContext());
        events.forEach(e -> {
            Object passStatementObject = e.getDiffObject();
            Logger.logDebug("", ibatisCqrsService.getContext());
            Logger.logDebug("  ---------------------------------", ibatisCqrsService.getContext());
            Logger.logDebug("  " + e.toString(), ibatisCqrsService.getContext());
            Logger.logDebug("  ---------------------------------", ibatisCqrsService.getContext());
            Logger.logDebug("", ibatisCqrsService.getContext());
            if(passStatementObject instanceof NodeEntity)
            {
                Logger.logDebug("  Writing NodeEntity object to database", ibatisCqrsService.getContext());
                ibatisCqrsService.getNodeDAOImpl().insertNode((NodeEntity) passStatementObject);
            }
        });
    }

    @Override
    public List<Object> getUsedStores()
    {
        ArrayList<Object> stores = new ArrayList<>();
        stores.add(ibatisCqrsService.getNodeDAOImpl());
        return stores;
    }

    @Override
    public void onDelete(List<Event> events)
    {
        // not implemented yet
    }
}
