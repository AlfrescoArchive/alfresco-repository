package org.alfresco.repo.domain.node.ibatis.cqrs;

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
     * @param command
     * @return the result of the command
     */
    public R handleCommand(C command);
}
