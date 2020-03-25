/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
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
package org.alfresco.repo.event2;

import java.util.List;

import org.alfresco.repo.event.v1.model.RepoEvent;
import org.alfresco.repo.event.v1.model.Resource;

/**
 * Event selector.
 *
 * @author Jamal Kaabi-Mofrad
 */
public class EventSelector
{
    /*
     * Order of events types.
     *
     * In a list of events within a transaction, the chosen event to be sent is selected by this ordered list.
     * E.g. if within a transaction we have [NODE_CREATED, NODE_UPDATED, NODE_UPDATED], then NODE_CREATED is selected.
     */
    private static final List<EventType> EVENT_ORDER = List.of(EventType.NODE_DELETED,
                                                               EventType.NODE_CREATED,
                                                               EventType.NODE_UPDATED);

    /**
     * Selects an event from the given list of events, based on the event type.
     *
     * @param events list of events
     * @return the {@code RepoEvent} that its type takes precedence over others
     */
    public static RepoEvent<? extends Resource> getEventToSend(List<RepoEvent<? extends Resource>> events)
    {
        for (EventType eventType : EVENT_ORDER)
        {
            for (RepoEvent<?> event : events)
            {
                if (event.getType().equals(eventType.getType()))
                {
                    return event;
                }
            }
        }
        return null;
    }
}
