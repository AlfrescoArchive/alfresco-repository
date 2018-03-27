package org.alfresco.repo.domain.node.ibatis.cqrs;

import org.alfresco.repo.domain.node.NodeEntity;
import org.alfresco.repo.domain.node.ibatis.cqrs.utils.Logger;

import java.util.LinkedList;
import java.util.List;

/**
 * Writer which simply caches the node.
 *
 * Created by mmuller on 26/03/2018.
 */
public class IbatisNodeInsertCqrsWriter2 implements CqrsWriter
{
    private String name;
    private IbatisNodeInsertCqrsServiceImpl ibatisCqrsService;
    private LinkedList<NodeEntity> nodes;

    public IbatisNodeInsertCqrsWriter2(String name, IbatisNodeInsertCqrsServiceImpl ibatisCqrsService)
    {
        this.name = name;
        this.ibatisCqrsService = ibatisCqrsService;
        nodes = new LinkedList<>();
    }

    public void notifyWriter(List<Event> events)
    {
        Logger.logDebug(name + " detected " + events.size() + " new events:" , ibatisCqrsService.getContext());
        events.forEach(e -> {
            Object passStatementObject = e.getDiffObject();
            Logger.logDebug("  ---------------------------------", ibatisCqrsService.getContext());
            Logger.logDebug("  " + e.toString(), ibatisCqrsService.getContext());
            Logger.logDebug("  ---------------------------------", ibatisCqrsService.getContext());
            Logger.logDebug("  Writing ibatis object to database", ibatisCqrsService.getContext());
            nodes.add((NodeEntity) passStatementObject);
        });
    }

    public String getName()
    {
        return name;
    }
}
