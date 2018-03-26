package org.alfresco.repo.domain.node.ibatis.cqrs;

import org.alfresco.repo.domain.node.ibatis.cqrs.utils.Context;

/**
 * Created by mmuller on 26/03/2018.
 */
public interface CqrsService
{
    public void executeCommand(Object diffObject, Context context);

    /**
     * Represents the context from which the service was called.
     *
     * @return context in which the service is called
     */
    public Context getContext();
}
