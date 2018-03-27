package org.alfresco.repo.domain.node.ibatis.cqrs;

import org.alfresco.repo.domain.node.ibatis.cqrs.utils.Context;
import org.alfresco.repo.domain.node.ibatis.cqrs.utils.Logger;

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

    @Override
    public CommandHandlerResult handleCommand(Object commandObject, Context context)
    {
        // check if the command is valid
        try
        {
            validateCommand(commandObject);
        }
        catch(IllegalArgumentException e)
        {
            return new CommandHandlerResult(commandObject, false);
        }
        return new CommandHandlerResult(commandObject, true);
    }

    private void validateCommand(Object commandObject) throws IllegalArgumentException
    {
        // use writer1 for validate object
        IbatisNodeInsertCqrsWriter1 writer1 = (IbatisNodeInsertCqrsWriter1) ibatisCqrsService.getWriters().getFirst();
        Object store = null;
        if(writer1.getUsedStores() != null)
        {
            store = writer1.getUsedStores().get(0);
        }
        if(store == null)
        {
            Logger.logDebug(writer1.getName() + " validates the command: " + commandObject.toString() + ", without a store", ibatisCqrsService.getContext());
        }
        else
        {
            Logger.logDebug(writer1.getName() + " validates the command: " + commandObject.toString() + ", against the store: "
                    + store.toString(), ibatisCqrsService.getContext());
            // Do something with the store for validate the command object
            // Throw the IllegalArgumentException if you are not happy with the command
        }
    }
}
