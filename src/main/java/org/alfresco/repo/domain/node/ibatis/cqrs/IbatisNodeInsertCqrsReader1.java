package org.alfresco.repo.domain.node.ibatis.cqrs;

import org.alfresco.repo.domain.node.NodeEntity;
import org.alfresco.repo.domain.node.ibatis.cqrs.utils.Logger;

import java.util.List;

/**
 * A Reader which returns the node id from the NodeEntity object. It doesn't need ibatis features at all.
 *
 * Created by mmuller on 26/03/2018.
 */
public class IbatisNodeInsertCqrsReader1 implements CqrsReader {
    private String name;
    private IbatisNodeInsertCqrsServiceImpl ibatisCqrsService;

    public IbatisNodeInsertCqrsReader1(String name, IbatisNodeInsertCqrsServiceImpl ibatisCqrsService) {
        this.name = name;
        this.ibatisCqrsService = ibatisCqrsService;
    }

    public void notifyReader(List<Event> events) {
        Logger.logDebug(name + " detected " + events.size() + " new events:", ibatisCqrsService.getContext());
        events.forEach(e -> {
            Object passStatementObject = e.getDiffObject();
            Logger.logDebug("  ---------------------------------", ibatisCqrsService.getContext());
            Logger.logDebug("  " + e.toString(), ibatisCqrsService.getContext());
            Logger.logDebug("  ---------------------------------", ibatisCqrsService.getContext());
            Logger.logDebug("  Cache node id", ibatisCqrsService.getContext());
        });
    }

    public String getValue(String col, Object node)
    {
        // check instance
        if(col.equalsIgnoreCase("id"))
        {
            return ((NodeEntity) node).getId().toString();
        }
        return null;
    }

    public String getName()
    {
        return name;
    }
}
