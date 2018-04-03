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

import org.alfresco.repo.domain.node.NodeEntity;
import org.alfresco.repo.domain.node.cqrs.utils.Logger;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Writer which simply caches the node.
 * NOTICE This writer isn't used in this example.
 *
 * Created by mmuller on 26/03/2018.
 */
public class NodeInsertCqrsWriter2 extends NodeInsertCqrsWriterAbstract
{
    private NodeInsertCqrsServiceImpl cqrsService;
    private LinkedList<NodeEntity> nodes;

    public NodeInsertCqrsWriter2(String name, NodeInsertCqrsServiceImpl cqrsService)
    {
        super(name);
        this.cqrsService = cqrsService;
        nodes = new LinkedList<>();
    }

    @Override
    public void onUpdate(List<Event> events)
    {
        // not implemented yet
    }

    @Override
    public void onCreate(List<Event> events)
    {
        Logger.logDebug(this.getName() + " detected " + events.size() + " new events:" , cqrsService.getContext());
        events.forEach(e -> {
            Object passStatementObject = e.getDiffObject();
            Logger.logDebug("  ---------------------------------", cqrsService.getContext());
            Logger.logDebug("  " + e.toString(), cqrsService.getContext());
            Logger.logDebug("  ---------------------------------", cqrsService.getContext());
            Logger.logDebug("  Writing object to database", cqrsService.getContext());
            nodes.add((NodeEntity) passStatementObject);
        });
    }

    @Override
    public void onDelete(List<Event> events)
    {
        // not implemented yet
    }

    @Override
    public List<Object> getUsedStores()
    {
        ArrayList<Object> stores = new ArrayList<>();
        stores.add(nodes);
        return stores;
    }
}
