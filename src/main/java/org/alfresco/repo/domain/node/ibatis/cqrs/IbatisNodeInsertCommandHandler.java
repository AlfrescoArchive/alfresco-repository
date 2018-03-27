/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2018 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

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
