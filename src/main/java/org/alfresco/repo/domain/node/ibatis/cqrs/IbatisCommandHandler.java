package org.alfresco.repo.domain.node.ibatis.cqrs;

/**
 * Created by mmuller on 26/03/2018.
 */
public class IbatisCommandHandler implements CommandHandler<Object, CommandHandlerResult>
{
    public CommandHandlerResult handleCommand(Object ibatisCommandObject)
    {
        // check if the command is valid
        // we accept the command for now
        return new CommandHandlerResult(ibatisCommandObject, true);
    }
}
