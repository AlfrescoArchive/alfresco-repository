package org.alfresco.repo.domain.node.ibatis.cqrs;

import org.alfresco.repo.domain.node.NodeEntity;
import org.alfresco.repo.domain.node.ibatis.cqrs.utils.Logger;

import java.util.List;

/**
 * Reader which uses our implementation for retrieve the node from his own.
 * Uses org.alfresco.repo.domain.node.AbstractNodeDAOImpl#getNodePair(java.lang.Long) for retrieve the id.
 *
 * Created by mmuller on 26/03/2018.
 */
public class IbatisNodeInsertCqrsReader3 implements CqrsReader {
    private String name;
    private IbatisNodeInsertCqrsServiceImpl ibatisCqrsService;

    public IbatisNodeInsertCqrsReader3(String name, IbatisNodeInsertCqrsServiceImpl ibatisCqrsService) {
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
            ibatisCqrsService.getNodeDAOImpl().insertNode((NodeEntity) passStatementObject);
        });
    }

    public String getValue(String col, Object node)
    {
        if(col.equalsIgnoreCase("id"))
        {
            Long searchId = ((NodeEntity) node).getId();
            return ibatisCqrsService.getNodeDAOImpl().getNodePair(searchId).getFirst().toString();
        }
        return null;
    }

    public String getName()
    {
        return name;
    }
}
