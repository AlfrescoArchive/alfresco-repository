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

package org.alfresco.repo.domain.node.cqrs;

import org.alfresco.repo.domain.node.cqrs.utils.Context;
import org.alfresco.repo.domain.node.cqrs.utils.Logger;

/**
 * Created by mmuller on 26/03/2018.
 */
public class NodeInsertCommandHandler implements CommandHandler<Object, CommandHandlerResult>
{
    private NodeInsertCqrsServiceImpl nodeqrsService;

    public NodeInsertCommandHandler(NodeInsertCqrsServiceImpl nodeqrsService)
    {
        this.nodeqrsService = nodeqrsService;
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
            Logger.logError(e, nodeqrsService.getContext());
            return new CommandHandlerResult(commandObject, false);
        }
        return new CommandHandlerResult(commandObject, true);
    }

    private void validateCommand(Object commandObject) throws IllegalArgumentException
    {
        String commandObjectString = "null";
        if(commandObject != null)
        {
            commandObjectString = commandObject.toString();
        }
        // use writer1 for validate object
        NodeInsertCqrsWriter1 writer1 = (NodeInsertCqrsWriter1) nodeqrsService.getWriters().getFirst();
        Object store = null;
        if(writer1.getUsedStores() != null)
        {
            store = writer1.getUsedStores().get(0);
        }
        if(store == null)
        {
            Logger.logDebug(writer1.getName() + " validates the command: " + commandObjectString + ", without a store", nodeqrsService.getContext());
        }
        else
        {
            Logger.logDebug(writer1.getName() + " validates the command: " + commandObjectString + ", against the store: "
                    + store.toString(), nodeqrsService.getContext());
            // Do something with the store for validate the command object
            // Throw the IllegalArgumentException if you are not happy with the command
        }
        if(commandObject == null)
        {
            throw new IllegalArgumentException("Command object is null");
        }
    }
}
