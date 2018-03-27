package org.alfresco.repo.domain.node.ibatis.cqrs;

import org.alfresco.repo.domain.node.NodeEntity;
import org.alfresco.repo.domain.node.ibatis.cqrs.utils.Context;

/**
 * Created by mmuller on 26/03/2018.
 */
public class IbatisNodeInsertCommandHandler implements CommandHandler<Object, CommandHandlerResult>
{
    private IbatisNodeInsertCqrsServiceImpl ibatisCqrsService;

    public IbatisNodeInsertCommandHandler(IbatisNodeInsertCqrsServiceImpl ibatisCqrsService)
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
        if(ibatisCqrsService.getNodeDAOImpl().exists(((NodeEntity) ibatisCommandOBject).getId()))
        {
            throw new IllegalArgumentException("Node already exists: " + ((NodeEntity) ibatisCommandOBject).getId());
        }
        // do some more validation ...
    }
}
