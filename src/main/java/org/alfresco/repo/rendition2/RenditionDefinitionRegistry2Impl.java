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
package org.alfresco.repo.rendition2;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A registry of rendition definitions.
 *
 * @author adavis
 */
public class RenditionDefinitionRegistry2Impl implements RenditionDefinitionRegistry2
{
    private final Map<String, RenditionDefinition2> renditionDefinitions = new HashMap();

    /**
     * Obtains a {@link RenditionDefinition2} by name.
     * @param renditionName to be returned
     * @return the {@link RenditionDefinition2} or null if not registered.
     */
    public RenditionDefinition2 getDefinition(String renditionName)
    {
        return renditionDefinitions.get(renditionName);
    }

    @Override
    public void register(RenditionDefinition2 renditionDefinition)
    {
        String renditionName = renditionDefinition.getRenditionName();
        RenditionDefinition2 original = getDefinition(renditionName);
        if (original != null)
        {
            throw new IllegalArgumentException("RenditionDefinition "+renditionName+" was already registered.");
        }
        renditionDefinitions.put(renditionName, renditionDefinition);
    }

    @Override
    public void unregister(String renditionName)
    {
        if (renditionDefinitions.remove(renditionName) == null)
        {
            throw new IllegalArgumentException("RenditionDefinition "+renditionName+" was not registered.");
        }
    }

    @Override
    public Set<String> getRenditionNames()
    {
        return renditionDefinitions.keySet();
    }

    @Override
    public RenditionDefinition2 getRenditionDefinition(String renditionName)
    {
        return renditionDefinitions.get(renditionName);
    }
}
