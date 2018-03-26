package org.alfresco.repo.domain.node.ibatis.cqrs;

import org.alfresco.repo.domain.node.NodeEntity;
import org.alfresco.repo.domain.node.ibatis.cqrs.utils.Logger;

import java.util.List;

/**
 * Created by mmuller on 26/03/2018.
 */
public class IbatisCqrsWriter implements CqrsWriter
{
    private String name;
    private IbatisNodeInsertCqrsServiceImpl ibatisCqrsService;

    public IbatisCqrsWriter(String name, IbatisNodeInsertCqrsServiceImpl ibatisCqrsService)
    {
        this.name = name;
        this.ibatisCqrsService = ibatisCqrsService;
    }

    public void notifyWriter(List<Event> events)
    {
        Logger.logDebug(name + " detected " + events.size() + " new events:" , ibatisCqrsService.getContext());
        events.forEach(d -> {
            Object passStatementObject = d.getDiffObject();
            Logger.logDebug("  ---------------------------------", ibatisCqrsService.getContext());
            Logger.logDebug("  " + d.toString(), ibatisCqrsService.getContext());
            Logger.logDebug("  ---------------------------------", ibatisCqrsService.getContext());
            Logger.logDebug("  Writing ibatis object to database", ibatisCqrsService.getContext());
            ibatisCqrsService.getNodeDAOImpl().insertNode((NodeEntity) passStatementObject);
        });
    }

    public String getName()
    {
        return name;
    }
}
