package org.alfresco.repo.domain.node.ibatis.cqrs;

import org.alfresco.repo.domain.node.NodeEntity;
import org.alfresco.repo.domain.node.ibatis.cqrs.utils.Logger;

import java.util.LinkedList;
import java.util.List;

/**
 * Writer which simply caches the node.
 * NOTICE This writer isn't used in this example.
 *
 * Created by mmuller on 26/03/2018.
 */
public class IbatisNodeInsertCqrsWriter2 extends IbatisNodeInsertCqrsWriterAbstract
{
    private IbatisNodeInsertCqrsServiceImpl ibatisCqrsService;
    private LinkedList<NodeEntity> nodes;

    public IbatisNodeInsertCqrsWriter2(String name, IbatisNodeInsertCqrsServiceImpl ibatisCqrsService)
    {
        super(name);
        this.ibatisCqrsService = ibatisCqrsService;
        nodes = new LinkedList<>();
    }

    public void onUpdate(List<Event> events)
    {
        // not implemented yet
    }

    public void onCreate(List<Event> events)
    {
        Logger.logDebug(this.getName() + " detected " + events.size() + " new events:" , ibatisCqrsService.getContext());
        events.forEach(e -> {
            Object passStatementObject = e.getDiffObject();
            Logger.logDebug("  ---------------------------------", ibatisCqrsService.getContext());
            Logger.logDebug("  " + e.toString(), ibatisCqrsService.getContext());
            Logger.logDebug("  ---------------------------------", ibatisCqrsService.getContext());
            Logger.logDebug("  Writing ibatis object to database", ibatisCqrsService.getContext());
            nodes.add((NodeEntity) passStatementObject);
        });
    }

    public void onDelete(List<Event> events)
    {
        // not implemented yet
    }
}
