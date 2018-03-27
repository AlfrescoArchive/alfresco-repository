package org.alfresco.repo.domain.node.ibatis.cqrs;

import org.alfresco.repo.domain.node.NodeEntity;
import org.alfresco.repo.domain.node.ibatis.cqrs.utils.Logger;

import java.util.List;

/**
 * Reader which writes in his own database and simply caches the last node id. Not that good idea.
 * NOTICE Thise reader isn't used in this example
 *
 * Created by mmuller on 26/03/2018.
 */
public class IbatisNodeInsertCqrsReader2 extends IbatisNodeInsertCqrsReaderAbstract {
    private IbatisNodeInsertCqrsServiceImpl ibatisCqrsService;
    private Long cachedLastId;

    public IbatisNodeInsertCqrsReader2(String name, IbatisNodeInsertCqrsServiceImpl ibatisCqrsService) {
        super(name);
        this.ibatisCqrsService = ibatisCqrsService;
    }

    @Override
    public String getValue(String col, Object node)
    {
        if(col.equalsIgnoreCase("id"))
        {
            return cachedLastId.toString();
        }
        return null;
    }

    @Override
    public void onUpdate(List<Event> events)
    {
        // not implemented yet
    }

    @Override
    public void onCreate(List<Event> events)
    {
        Logger.logDebug(this.getName() + " detected " + events.size() + " new events:", ibatisCqrsService.getContext());
        events.forEach(e -> {
            Object passStatementObject = e.getDiffObject();
            Logger.logDebug("  ---------------------------------", ibatisCqrsService.getContext());
            Logger.logDebug("  " + e.toString(), ibatisCqrsService.getContext());
            Logger.logDebug("  ---------------------------------", ibatisCqrsService.getContext());
            Logger.logDebug("  Cache node id", ibatisCqrsService.getContext());
            cachedLastId = ibatisCqrsService.getNodeDAOImpl().insertNode((NodeEntity) passStatementObject);
        });
    }

    @Override
    public void onDelete(List<Event> events)
    {
        // not implemented yet
    }
}
