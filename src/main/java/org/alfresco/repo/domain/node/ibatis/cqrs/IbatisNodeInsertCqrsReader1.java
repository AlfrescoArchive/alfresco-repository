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

import java.util.List;

/**
 * A Reader which returns the node id from the NodeEntity object. It doesn't need ibatis features at all.
 *
 * Created by mmuller on 26/03/2018.
 */
public class IbatisNodeInsertCqrsReader1 extends IbatisNodeInsertCqrsReaderAbstract {
    private IbatisNodeInsertCqrsServiceImpl ibatisCqrsService;

    public IbatisNodeInsertCqrsReader1(String name, IbatisNodeInsertCqrsServiceImpl ibatisCqrsService)
    {
        super(name);
        this.ibatisCqrsService = ibatisCqrsService;
    }

    @Override
    public String getValue(String col, Object node)
    {
        if(node == null || col == null || col.isEmpty())
        {
            return null;
        }

        Logger.logDebug(this.getName() + " getValue with col=" + col + ", object=" + node.toString(), ibatisCqrsService.getContext());
        // check instance
        String result = null;
        if(node instanceof NodeEntity && col.equalsIgnoreCase("id"))
        {
            result = ((NodeEntity) node).getId().toString();
        }
        else if(node instanceof String && col.equalsIgnoreCase("self"))
        {
            result = node.toString();
        }
        Logger.logDebug(this.getName() + " getValue returns=" + result, ibatisCqrsService.getContext());
        return result;
    }

    @Override
    public void onUpdate(List<Event> events)
    {
        // not implemented yet
    }

    @Override
    public void onCreate(List<Event> events)
    {
        Logger.logDebug("", ibatisCqrsService.getContext());
        Logger.logDebug(this.getName() + " detected " + events.size() + " new events:", ibatisCqrsService.getContext());
        events.forEach(e -> {
            Object passStatementObject = e.getDiffObject();
            Logger.logDebug("", ibatisCqrsService.getContext());
            Logger.logDebug("  ---------------------------------", ibatisCqrsService.getContext());
            Logger.logDebug("  " + e.toString(), ibatisCqrsService.getContext());
            Logger.logDebug("  ---------------------------------", ibatisCqrsService.getContext());
            Logger.logDebug("", ibatisCqrsService.getContext());
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
        return null;
    }
}
