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

import org.alfresco.repo.domain.node.NodeEntity;
import org.alfresco.repo.domain.node.ibatis.cqrs.utils.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Reader which uses our implementation for retrieve the node from his own.
 * Uses org.alfresco.repo.domain.node.AbstractNodeDAOImpl#getNodePair(java.lang.Long) for retrieve the id.
 *
 * NOTICE Thise reader isn't used in this example
 *
 * Created by mmuller on 26/03/2018.
 */
public class IbatisNodeInsertCqrsReader3 extends IbatisNodeInsertCqrsReaderAbstract {
    private IbatisNodeInsertCqrsServiceImpl ibatisCqrsService;

    public IbatisNodeInsertCqrsReader3(String name, IbatisNodeInsertCqrsServiceImpl ibatisCqrsService) {
        super(name);
        this.ibatisCqrsService = ibatisCqrsService;
    }

    @Override
    public String getValue(String col, Object node)
    {
        if(col.equalsIgnoreCase("id"))
        {
            Long searchId = ((NodeEntity) node).getId();
            return ibatisCqrsService.getNodeDAOImpl().getNodePair(searchId).getFirst().toString();
        }
        return null;
    }

    @Override
    public void onUpdate(List<Event> events)
    {
        // not implemented yet
    }

    @Override
    public void onCreate(List<Event> events)
    {
        Logger.logDebug(this.getName() + " detected " + events.size() + " new events:", ibatisCqrsService.getContext());
        events.forEach(e -> {
            Object passStatementObject = e.getDiffObject();
            Logger.logDebug("  ---------------------------------", ibatisCqrsService.getContext());
            Logger.logDebug("  " + e.toString(), ibatisCqrsService.getContext());
            Logger.logDebug("  ---------------------------------", ibatisCqrsService.getContext());
            ibatisCqrsService.getNodeDAOImpl().insertNode((NodeEntity) passStatementObject);
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
        stores.add(ibatisCqrsService.getNodeDAOImpl());
        return stores;
    }
}
