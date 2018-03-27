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

import java.util.List;

/**
 * Implements the same context of Viewer and Reader in the CQRS pattern. For this example they both can have names.
 * And they are listen to the Event Source with EventListener.
 *
 * As well the CQRS pattern allows us that we can share storage ressources (e.g. in-memory) across Reader and Writer.
 *
 * Created by mmuller on 26/03/2018.
 */
public abstract class CqrsWriterAndReader implements EventListener
{
    private String name;

    public CqrsWriterAndReader(String name)
    {
        this.name = name;
    }

    /**
     * Returns the name of the Writer or Reader
     *
     * @return
     */
    public String getName()
    {
        return name;
    }

    /**
     * Returns the store which was used for the writes or readers.
     *
     * @return the used store as object. Can be null if no store was used.
     */
    public abstract List<Object> getUsedStores();
}
