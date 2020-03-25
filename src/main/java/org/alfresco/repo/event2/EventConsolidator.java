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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.alfresco.repo.event.v1.model.EventData;
import org.alfresco.repo.event.v1.model.NodeResource;
import org.alfresco.repo.event.v1.model.RepoEvent;
import org.alfresco.repo.event.v1.model.Resource;
import org.alfresco.repo.event2.NodeResourceInfoFactory.NodeResourceInfo;
import org.alfresco.service.cmr.repository.NodeRef;

/**
 * Encapsulates events occurred in a single transaction.
 *
 * @author Jamal Kaabi-Mofrad
 */
public class EventConsolidator
{
    private NodeResourceInfoFactory nodeInfoFactory;
    private List<RepoEvent<? extends Resource>> events;

    public EventConsolidator(NodeResourceInfoFactory nodeInfoFactory)
    {
        this.nodeInfoFactory = nodeInfoFactory;
        this.events = new LinkedList<>();
    }

    public void append(NodeRef nodeRef, EventInfo eventInfo)
    {
        NodeResourceInfo nodeInfo = nodeInfoFactory.getNodeInfo(nodeRef);
        events.add(toRepoEvent(nodeInfo, eventInfo));
    }

    private RepoEvent<NodeResource> toRepoEvent(NodeResourceInfo nodeInfo, EventInfo eventInfo)
    {
        NodeResource resource = NodeResource.builder()
                    .setId(nodeInfo.getId())
                    .setPrimaryHierarchy(nodeInfo.getNodePaths())
                    .setIsFile(nodeInfo.isFile())
                    .setIsFolder(nodeInfo.isFolder())
                    .setNodeType(nodeInfo.getNodeType())
                    .setProperties(nodeInfo.getProperties())
                    .build();
        EventData<NodeResource> eventData = EventData.<NodeResource>builder().setPrincipal(eventInfo.getPrincipal())
                    .setEventGroupId(eventInfo.getTxnId())
                    .setResource(resource)
                    .build();
        return RepoEvent.<NodeResource>builder().setId(eventInfo.getId())
                    .setSource(eventInfo.getSource())
                    .setTime(eventInfo.getTimestamp())
                    .setType(eventInfo.getEventType().getType())
                    .setSubject(nodeInfo.getName())
                    .setData(eventData)
                    .build();
    }

    public List<RepoEvent<? extends Resource>> getEvents()
    {
        return Collections.unmodifiableList(events);
    }

    public RepoEvent<? extends Resource> getSelectedEvent()
    {
        return EventSelector.getEventToSend(events);
    }
}
