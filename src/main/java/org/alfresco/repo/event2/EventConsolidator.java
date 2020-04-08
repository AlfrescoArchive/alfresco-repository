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
import java.util.Collections;
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
import org.alfresco.util.Pair;
import org.alfresco.util.PropertyMap;

/**
 * Encapsulates events occurred in a single transaction.
 *
 * @author Jamal Kaabi-Mofrad
 */
public class EventConsolidator implements EventSupportedPolicies
{
    private NodeResourceHelper helper;
    private NodeResource.Builder builder;
    private Deque<EventType> eventTypes;
    private Map<QName, Serializable> propertiesBefore;
    private Map<QName, Serializable> propertiesAfter;
    private Set<QName> aspectsAdded;
    private Set<QName> aspectsRemoved;
    private NodeRef nodeRef;
    private QName nodeType;
    private String nodeName;

    public EventConsolidator(NodeResourceHelper nodeResourceHelper)
    {
        this.helper = nodeResourceHelper;
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
                    .setType(getDerivedEvent().getType())
                    .setSubject(nodeName)
                    .setData(eventData)
                    .build();
    }

    /**
     * Creates a builder instance if absent or {@code forceUpdate} is requested.
     * It also, sets the required fields.
     *
     * @param nodeRef     the nodeRef in the txn
     * @param forceUpdate if {@code true}, will get the latest node info and ignores
     *                    the existing builder object.
     */
    private void createBuilderIfAbsent(NodeRef nodeRef, boolean forceUpdate)
    {
        if (builder == null || forceUpdate)
        {
            this.builder = helper.createNodeResourceBuilder(nodeRef);
            this.nodeRef = nodeRef;
            this.nodeType = helper.getNodeType(nodeRef);
            this.nodeName = (String) helper.getProperty(nodeRef, ContentModel.PROP_NAME);
        }
    }

    /**
     * Creates a builder instance if absent, and sets the required fields.
     *
     * @param nodeRef the nodeRef in the txn
     */
    private void createBuilderIfAbsent(NodeRef nodeRef)
    {
        createBuilderIfAbsent(nodeRef, false);
    }

    @Override
    public void onCreateNode(ChildAssociationRef childAssocRef)
    {
        eventTypes.add(EventType.NODE_CREATED);

        NodeRef nodeRef = childAssocRef.getChildRef();
        createBuilderIfAbsent(nodeRef);

        // Sometimes onCreateNode policy is out of order
        this.propertiesBefore = null;
        setBeforeProperties(Collections.emptyMap());
        setAfterProperties(helper.getProperties(nodeRef));
    }

    @Override
    public void onUpdateProperties(NodeRef nodeRef, Map<QName, Serializable> before, Map<QName, Serializable> after)
    {
        eventTypes.add(EventType.NODE_UPDATED);

        // Sometime we don't get the 'before', so just use the latest
        if (before.isEmpty() && this.propertiesAfter != null)
        {
            before = this.propertiesAfter;
        }
        createBuilderIfAbsent(nodeRef);
        setBeforeProperties(before);
        setAfterProperties(after);
    }

    @Override
    public void beforeDeleteNode(NodeRef nodeRef)
    {
        eventTypes.add(EventType.NODE_DELETED);
        createBuilderIfAbsent(nodeRef, true);
        // Set the node properties and aspects while the node exists
        builder.setProperties(helper.getMappedProperties(nodeRef));
        builder.setAspectNames(helper.getMappedAspects(nodeRef));
    }

    @Override
    public void onAddAspect(NodeRef nodeRef, QName aspectTypeQName)
    {
        eventTypes.add(EventType.NODE_UPDATED);
        aspectsAdded.add(aspectTypeQName);
        createBuilderIfAbsent(nodeRef);
    }

    @Override
    public void onRemoveAspect(NodeRef nodeRef, QName aspectTypeQName)
    {
        eventTypes.add(EventType.NODE_UPDATED);
        aspectsRemoved.add(aspectTypeQName);
        createBuilderIfAbsent(nodeRef);
    }

    private void setAfterProperties(Map<QName, Serializable> after)
    {
        propertiesAfter = after;
    }

    private void setBeforeProperties(Map<QName, Serializable> before)
    {
        // Don't overwrite the original value if there are multiple calls.
        if (propertiesBefore == null)
        {
            propertiesBefore = before;
        }
    }

    private void setPropertiesAndAspects()
    {
        if (eventTypes.contains(EventType.NODE_CREATED))
        {
            // Set properties
            builder.setProperties(helper.mapToNodeProperties(propertiesAfter));

            // Set aspects
            builder.setAspectNames(helper.getMappedAspects(nodeRef));
        }
        else if (eventTypes.contains(EventType.NODE_UPDATED))
        {
            // Set 'before' and 'after' properties - if any
            Pair<Map<QName, Serializable>, Map<QName, Serializable>> beforeAndAfterProps = PropertyMap
                        .getBeforeAndAfterMapsForChanges(propertiesBefore, propertiesAfter);
            if (!beforeAndAfterProps.getFirst().isEmpty() || !beforeAndAfterProps.getSecond().isEmpty())
            {
                builder.setAffectedPropertiesBefore(helper.mapToNodeProperties(beforeAndAfterProps.getFirst()))
                       .setAffectedPropertiesAfter(helper.mapToNodeProperties(beforeAndAfterProps.getSecond()));
            }

            // Set 'before' and 'after' aspects - if any
            if (!aspectsRemoved.isEmpty() || !aspectsAdded.isEmpty())
            {
                builder.setAspectNamesBefore(helper.mapToNodeAspects(aspectsRemoved))
                       .setAspectNamesAfter(helper.mapToNodeAspects(aspectsAdded));
            }
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
            // Check the node still exists.
            // This could happen in tests where a node is deleted before the afterCommit code is
            // executed (For example, see ThumbnailServiceImplTest#testIfNodesExistsAfterCreateThumbnail).
            if (helper.nodeExists(nodeRef))
            {
                // We are setting the details at the end of the Txn by getting the latest info
                createBuilderIfAbsent(nodeRef, true);
                setPropertiesAndAspects();
            }
        }
        // Now create an instance of NodeResource
        return builder.build();
    }

    /**
     * @return a derived event for a transaction.
     */
    private EventType getDerivedEvent()
    {
        if (isTemporaryNode())
        {
            // This event will be filtered out, but we set the correct
            // event type anyway for debugging purposes
            return EventType.NODE_DELETED;
        }
        else if (eventTypes.contains(EventType.NODE_CREATED))
        {
            return EventType.NODE_CREATED;
        }
        else if (eventTypes.getLast() == EventType.NODE_DELETED)
        {
            return EventType.NODE_DELETED;
        }
        else
        {
            // Default to first event
            return eventTypes.getFirst();
        }
    }

    /**
     * Whether or not the node has been created and then deleted, i.e. a temporary node.
     *
     * @return {@code true} if the node has been created and then deleted, otherwise false
     */
    public boolean isTemporaryNode()
    {
        return eventTypes.contains(EventType.NODE_CREATED) && eventTypes.getLast() == EventType.NODE_DELETED;
    }

    public QName getNodeType()
    {
        return nodeType;
    }

    public String getNodeName()
    {
        return nodeName;
    }

    public Deque<EventType> getEventTypes()
    {
        return eventTypes;
    }
}