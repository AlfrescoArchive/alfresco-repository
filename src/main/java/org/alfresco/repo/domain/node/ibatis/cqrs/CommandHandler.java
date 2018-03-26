package org.alfresco.repo.domain.node.ibatis.cqrs;

import org.alfresco.repo.domain.node.ibatis.cqrs.utils.Context;

/**
 * Command handler for CQRS using plain old java object as input and output
 *
 * @param <C> The command which needs to be handled
 * @param <R> The result of the command
 */
public interface CommandHandler<C, R>
{

    /**
     * Handles the command
     *
     * @param command Command saved as object e.g. ibatis statement
     * @param context Context outside of the service
     * @return the result of the command
     */
    public R handleCommand(C command, Context context);
}
