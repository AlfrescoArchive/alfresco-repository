package org.alfresco.repo.domain.node.ibatis.cqrs;

import org.alfresco.repo.domain.node.ibatis.cqrs.utils.Context;

/**
 * Service for implementing CQRS
 *
 * Created by mmuller on 26/03/2018.
 */
public interface CqrsService
{
    /**
     * Commands are capable of changing the database. They will be evaluated and either accepted or refused.
     * If they are refused they will not change the database.
     *
     * @param diffObject
     */
    public void executeCommand(Object diffObject);

    /**
     * Represents the context from which the service was called.
     *
     * @return context in which the service is called
     */
    public Context getContext();
}
