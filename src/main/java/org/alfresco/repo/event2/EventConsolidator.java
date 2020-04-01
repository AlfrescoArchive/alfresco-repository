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

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.event.v1.model.EventData;
import org.alfresco.repo.event.v1.model.NodeResource;
import org.alfresco.repo.event.v1.model.RepoEvent;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

/**
 * Encapsulates events occurred in a single transaction.
 *
 * @author Jamal Kaabi-Mofrad
 */
public class EventConsolidator implements EventSupportedPolicies
{
    private NodeResourceHelper nodeResourceHelper;
    private NodeResource.Builder builder;
    private Deque<EventType> eventTypes;
    private Set<QName> aspectsAdded;
    private Set<QName> aspectsRemoved;
    private NodeRef nodeRef;
    private String nodeName;
    // A flag to avoid a DB call
    private boolean isPropertiesSet;

    public EventConsolidator(NodeResourceHelper nodeResourceHelper)
    {
        this.nodeResourceHelper = nodeResourceHelper;
        this.eventTypes = new ArrayDeque<>();
        this.aspectsAdded = new HashSet<>();
        this.aspectsRemoved = new HashSet<>();
    }

    /**
     * Builds and returns the {@link RepoEvent} instance.
     *
     * @param eventInfo the object holding the event information
     * @return the {@link RepoEvent} instance
     */
    public RepoEvent<NodeResource> getRepoEvent(EventInfo eventInfo)
    {
        NodeResource resource = buildNodeResource();
        EventData<NodeResource> eventData = EventData.<NodeResource>builder()
                    .setPrincipal(eventInfo.getPrincipal())
                    .setEventGroupId(eventInfo.getTxnId())
                    .setResource(resource)
                    .build();
        return RepoEvent.<NodeResource>builder()
                    .setId(eventInfo.getId())
                    .setSource(eventInfo.getSource())
                    .setTime(eventInfo.getTimestamp())
                    .setType(eventTypes.getFirst().getType())
                    .setSubject(nodeName)
                    .setData(eventData)
                    .build();
    }

    private NodeResource.Builder getBuilder(NodeRef nodeRef)
    {
        if (builder == null)
        {
            this.builder = nodeResourceHelper.createNodeResourceBuilder(nodeRef);
            this.nodeRef = nodeRef;
            this.nodeName = (String) nodeResourceHelper.getProperty(nodeRef, ContentModel.PROP_NAME);
        }
        return builder;
    }

    @Override
    public void onCreateNode(ChildAssociationRef childAssocRef)
    {
        eventTypes.add(EventType.NODE_CREATED);
        getBuilder(childAssocRef.getChildRef());
    }

    @Override
    public void onUpdateProperties(NodeRef nodeRef, Map<QName, Serializable> before, Map<QName, Serializable> after)
    {
        eventTypes.add(EventType.NODE_UPDATED);
        if (before.isEmpty() || eventTypes.getFirst() == EventType.NODE_CREATED)
        {
            // For 'Created' event we don't care about the affected properties
            getBuilder(nodeRef).setProperties(nodeResourceHelper.mapToNodeProperties(after));
        }
        else
        {
            nodeResourceHelper.setBuilderProperties(getBuilder(nodeRef), before, after);
        }
        // If this method gets invoked, then we have set the properties
        isPropertiesSet = true;
    }

    @Override
    public void beforeDeleteNode(NodeRef nodeRef)
    {
        eventTypes.add(EventType.NODE_DELETED);
        // Set the properties and aspects while the node exists
        getBuilder(nodeRef).setProperties(nodeResourceHelper.getProperties(nodeRef))
                           .setAspectNames(nodeResourceHelper.getAspects(nodeRef));
    }

    @Override
    public void onAddAspect(NodeRef nodeRef, QName aspectTypeQName)
    {
        eventTypes.add(EventType.NODE_UPDATED);
        aspectsAdded.add(aspectTypeQName);
        getBuilder(nodeRef);
    }

    @Override
    public void onRemoveAspect(NodeRef nodeRef, QName aspectTypeQName)
    {
        eventTypes.add(EventType.NODE_UPDATED);
        aspectsRemoved.add(aspectTypeQName);
        getBuilder(nodeRef);
    }

    private void setProperties()
    {
        if (!isPropertiesSet)
        {
            builder.setProperties(nodeResourceHelper.getProperties(nodeRef));
        }
    }

    private void setAspects()
    {
        if(eventTypes.getFirst() == EventType.NODE_CREATED)
        {
            builder.setAspectNames(nodeResourceHelper.getAspects(nodeRef));
        }
        else
        {
            nodeResourceHelper.setBuilderAspects(builder, nodeRef, aspectsRemoved, aspectsAdded);
        }
    }

    private NodeResource buildNodeResource()
    {
        if (builder == null)
        {
            return null;
        }

        if (eventTypes.getLast() != EventType.NODE_DELETED)
        {
            setProperties();
            setAspects();
        }
        // Now create an instance of NodeResource
        return builder.build();
    }

    /**
     * Whether or not the node has been created and then deleted, i.e. a temporary node.
     *
     * @return {@code true} if the node has been created and then deleted, otherwise false
     */
    public boolean isTemporaryNode()
    {
        return eventTypes.getFirst() == EventType.NODE_CREATED && eventTypes.getLast() == EventType.NODE_DELETED;
    }

    public NodeRef getNodeRef()
    {
        return nodeRef;
    }

    public Deque<EventType> getEventTypes()
    {
        return eventTypes;
    }
}
