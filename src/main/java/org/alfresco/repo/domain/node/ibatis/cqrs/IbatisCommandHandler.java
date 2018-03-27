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
        try
        {
            validateIbatisCommand(ibatisCommandObject);
        }
        catch(IllegalArgumentException e)
        {
            return new CommandHandlerResult(ibatisCommandObject, false);
        }
        return new CommandHandlerResult(ibatisCommandObject, true);
    }

    public void validateIbatisCommand(Object ibatisCommandOBject) throws IllegalArgumentException
    {
        // validate object
    }
}
