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

/**
 * Encapsulate the diff object (e.g. ibatis statement)
 *
 * Created by mmuller on 26/03/2018.
 */
public class CommandHandlerResult
{

    /** The command can be rejected or accepted */
    private boolean accepted = false;
    /** Contains the diff object (e.g. ibatis statement) */
    private Object diffObject;

    /**
     *
     * @param diffObject Contains the diff object (e.g. ibatis statement)
     * @param accepted The command can be rejected or accepted
     */
    public CommandHandlerResult(Object diffObject, boolean accepted)
    {
        this.diffObject = diffObject;
        this.accepted = accepted;
    }

    public boolean isAccepted()
    {
        return accepted;
    }

    public Object getDiffObject()
    {
        return diffObject;
    }

    @Override
    public String toString()
    {
        String commandObjectString = "null";
        if(diffObject != null)
        {
            commandObjectString = diffObject.toString();
        }
        return "Accepted=" + accepted + ", the diffOjbect=" + commandObjectString;
    }
}
