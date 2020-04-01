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
package org.alfresco.repo.event2.filter;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Holds {@link AbstractEventFilter} implementations.
 *
 * @author Jamal Kaabi-Mofrad
 */
public class EventFilterRegistry
{
    private final ConcurrentMap<String, AbstractEventFilter> registry;

    public EventFilterRegistry()
    {
        this.registry = new ConcurrentHashMap<>();
    }

    /**
     * Registers an instance of {@code AbstractEventFilter}.
     *
     * @param eventFilter the event filter
     */
    public void addFilter(AbstractEventFilter eventFilter)
    {
        registry.putIfAbsent(eventFilter.getClass().getName(), eventFilter);
    }

    /**
     * Gets the event filter.
     *
     * @param filterType the event filter type to perform the lookup
     * @return an {@code Optional} describing the found filter,
     * or an empty {@code Optional} if no filter is found.
     */
    public Optional<EventFilter> getFilter(Class<? extends AbstractEventFilter> filterType)
    {
        return Optional.ofNullable(registry.get(filterType.getName()));
    }
}
