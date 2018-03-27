package org.alfresco.repo.domain.node.ibatis.cqrs;

import org.alfresco.repo.domain.node.NodeEntity;
import org.alfresco.repo.domain.node.ibatis.cqrs.utils.Logger;

import java.util.List;

/**
 * Reader which writes in his own database and simply caches the last node id. Not that good idea.
 *
 * Created by mmuller on 26/03/2018.
 */
public class IbatisNodeInsertCqrsReader2 implements CqrsReader {
    private String name;
    private IbatisNodeInsertCqrsServiceImpl ibatisCqrsService;
    private Long cachedLastId;

    public IbatisNodeInsertCqrsReader2(String name, IbatisNodeInsertCqrsServiceImpl ibatisCqrsService) {
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
            cachedLastId = ibatisCqrsService.getNodeDAOImpl().insertNode((NodeEntity) passStatementObject);
        });
    }

    public String getValue(String col, Object node)
    {
        if(col.equalsIgnoreCase("id"))
        {
            return cachedLastId.toString();
        }
        return null;
    }

    public String getName()
    {
        return name;
    }
}
