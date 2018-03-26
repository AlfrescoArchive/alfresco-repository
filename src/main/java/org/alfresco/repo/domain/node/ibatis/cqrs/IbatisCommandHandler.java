package org.alfresco.repo.domain.node.ibatis.cqrs;

import org.alfresco.repo.domain.node.ibatis.cqrs.utils.Context;

/**
 * Created by mmuller on 26/03/2018.
 */
public class IbatisCommandHandler implements CommandHandler<Object, CommandHandlerResult>
{
    private IbatisNodeInsertCqrsServiceImpl ibatisCqrsService;

    public IbatisCommandHandler(IbatisNodeInsertCqrsServiceImpl ibatisCqrsService)
    {
        this.ibatisCqrsService = ibatisCqrsService;
    }

    public CommandHandlerResult handleCommand(Object ibatisCommandObject, Context context)
    {
        // check if the command is valid
        // we accept the command for now
        return new CommandHandlerResult(ibatisCommandObject, true);
    }
}
