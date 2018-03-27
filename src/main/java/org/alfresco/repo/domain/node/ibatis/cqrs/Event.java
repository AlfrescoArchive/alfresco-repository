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

import java.util.UUID;

/**
 * Class represents an event from stored in the Event Store. Typically this event needs to contain the changes
 * which leaded the state A to state B. Think of that like diff changes from commits.
 *
 * Created by mmuller on 26/03/2018.
 */
public class Event
{
    private long timestamp;
    /** Every event has an id */
    private UUID id;
    private Object diffObject;

    /**
     * Creates an event which can be stored in an Event Store
     *
     * @param diffObject contains the diff object (e.g. ibatis statement)
     */
    public Event(Object diffObject)
    {
        this.diffObject = diffObject;
        timestamp = System.currentTimeMillis();
        id = UUID.randomUUID();
    }

    public String toString()
    {
        return "Event id=" + id + " Created=" + timestamp + " DiffObject=" + diffObject.toString();
    }

    public long getTimestamp()
    {
        return timestamp;
    }

    public UUID getId()
    {
        return id;
    }

    public Object getDiffObject()
    {
        return diffObject;
    }
}
