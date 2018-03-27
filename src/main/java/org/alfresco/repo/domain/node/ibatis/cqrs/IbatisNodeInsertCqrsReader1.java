package org.alfresco.repo.domain.node.ibatis.cqrs;

import org.alfresco.repo.domain.node.NodeEntity;
import org.alfresco.repo.domain.node.ibatis.cqrs.utils.Logger;

import java.util.List;

/**
 * A Reader which returns the node id from the NodeEntity object. It doesn't need ibatis features at all.
 *
 * Created by mmuller on 26/03/2018.
 */
public class IbatisNodeInsertCqrsReader1 extends IbatisNodeInsertCqrsReaderAbstract {
    private IbatisNodeInsertCqrsServiceImpl ibatisCqrsService;

    public IbatisNodeInsertCqrsReader1(String name, IbatisNodeInsertCqrsServiceImpl ibatisCqrsService)
    {
        super(name);
        this.ibatisCqrsService = ibatisCqrsService;
    }

    @Override
    public String getValue(String col, Object node)
    {
        if(node == null || col == null || col.isEmpty())
        {
            return null;
        }

        Logger.logDebug(this.getName() + " getValue with col: " + col + ", object: " + node.toString(), ibatisCqrsService.getContext());
        // check instance
        String result = null;
        if(node instanceof NodeEntity && col.equalsIgnoreCase("id"))
        {
            result = ((NodeEntity) node).getId().toString();
        }
        else if(node instanceof String && col.equalsIgnoreCase("self"))
        {
            result = node.toString();
        }
        Logger.logDebug(this.getName() + " getValue returns: " + result, ibatisCqrsService.getContext());
        return result;
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
        Logger.logDebug(this.getName() + " detected " + events.size() + " new events:", ibatisCqrsService.getContext());
        events.forEach(e -> {
            Object passStatementObject = e.getDiffObject();
            Logger.logDebug("", ibatisCqrsService.getContext());
            Logger.logDebug("  ---------------------------------", ibatisCqrsService.getContext());
            Logger.logDebug("  " + e.toString(), ibatisCqrsService.getContext());
            Logger.logDebug("  ---------------------------------", ibatisCqrsService.getContext());
            Logger.logDebug("", ibatisCqrsService.getContext());
        });
    }

    @Override
    public void onDelete(List<Event> events)
    {
        // not implemented yet
    }

    @Override
    public List<Object> getUsedStores()
    {
        return null;
    }
}
