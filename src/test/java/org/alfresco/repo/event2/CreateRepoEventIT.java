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

import static java.util.concurrent.TimeUnit.SECONDS;
import java.util.concurrent.CompletableFuture;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.event.v1.model.EventData;
import org.alfresco.repo.event.v1.model.NodeResource;
import org.alfresco.repo.event.v1.model.RepoEvent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.GUID;
import org.alfresco.util.PropertyMap;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Iulian Aftene
 */

public class CreateRepoEventIT extends ContextAwareRepoEvent
{
    private CompletableFuture<String> futureResult;

    @Before
    public void dataPreparation() throws Exception {

        futureResult = new CompletableFuture<>();
        subscribe(futureResult::complete, String.class);
    }

    @Test
    public void testCreateEvent() throws Exception {

        createNode(ContentModel.TYPE_CONTENT);
        Thread.sleep(3 * 1000); // 3 seconds time-out

        final RepoEvent<NodeResource> resultRepoEvent = JacksonSerializer.deserialize(futureResult.get(5, SECONDS),RepoEvent.class);

        assertTrue("Repo event retrieved type is "+ resultRepoEvent.getType()+ " .Expected \"Created\"",
            resultRepoEvent.getType().equals("org.alfresco.event.node.Created"));
        assertFalse("Repo event ID is not available. ", resultRepoEvent.getId().isEmpty());
        assertFalse("Repo event source is not available. ", resultRepoEvent.getSource().toString().isEmpty());
        assertFalse("Repo event subject is not available. ", resultRepoEvent.getSubject().isEmpty());
        assertFalse("Repo event creation time is not available. ",resultRepoEvent.getTime().toString().isEmpty());
        assertTrue("Repo event datacontenttype is not application/json. ", resultRepoEvent.getDatacontenttype().equals("application/json"));

        EventData<NodeResource> nodeResourceEventData = resultRepoEvent.getData();
        assertTrue("Wrong event data principal. ", nodeResourceEventData.getPrincipal().contains("admin"));
        assertFalse("Event data group ID is not available. ", nodeResourceEventData.getEventGroupId().isEmpty());

        NodeResource nodeResource = nodeResourceEventData.getResource();
        assertTrue("Resource type is not available. ", nodeResource.getType().toString().contains("NodeResource"));

        assertFalse("Default aspects were not added. ", nodeResource.getAspectNames().isEmpty());
        assertNull("affectedPropertiesBefore property is not available", nodeResource.getAffectedPropertiesBefore());
        assertNull("affectedPropertiesAfter property is not available", nodeResource.getAffectedPropertiesAfter());
        assertNull("aspectNamesBefore property is not available", nodeResource.getAspectNamesBefore());
        assertNull("aspectNamesAfter property is not available", nodeResource.getAspectNamesAfter());

        String properties = nodeResource.getProperties().toString();
        assertTrue("Wrong node creator, expecting: admin ", properties.contains("cm:creator=admin"));
        assertTrue("Wrong node modifier, expecting: admin ", properties.contains("cm:modifier=admin"));
        assertTrue("Node cm:created property is not available", properties.contains("cm:created"));
        assertTrue("Node cm:name property is not available", properties.contains("cm:name"));
        assertTrue("Node cm:modified property is not available", properties.contains("cm:modified"));
    }

    @Test
    public void createContentInFolderStructure() throws Exception {

        NodeRef parentRef = createNode(ContentModel.TYPE_CONTAINER);
        createNode(ContentModel.TYPE_FOLDER,parentRef);
        Thread.sleep(3 * 1000); // 3 seconds time-out

        final RepoEvent resultRepoEvent= JacksonSerializer.deserialize(futureResult.get(5, SECONDS),RepoEvent.class);

        assertEquals("Wrong hierarchy",2, resultRepoEvent
            .getData()
            .getResource()
            .getPrimaryHierarchy()
            .size());
    }

    @Test
    public void testCreateNodeWithId() throws Exception {

        final String uuid = GUID.generate();
        PropertyMap properties = new PropertyMap();
        properties.put(ContentModel.PROP_NODE_UUID, uuid);

        // create a node with an explicit UUID
        createNode(ContentModel.TYPE_CONTENT, properties);
        Thread.sleep(3 * 1000); // 3 seconds time-out

        final RepoEvent resultRepoEvent= JacksonSerializer.deserialize(futureResult.get(5, SECONDS),RepoEvent.class);

        assertEquals("Failed to create node with a chosen ID",
            uuid,
            resultRepoEvent
                .getData()
                .getResource()
                .getId());
    }

    @Test
    public void testFolderNodeType() throws Exception {

        createNode(ContentModel.TYPE_FOLDER);
        Thread.sleep(3 * 1000); // wait up to 3 seconds for the event

        final RepoEvent resultRepoEvent= JacksonSerializer.deserialize(futureResult.get(5, SECONDS),RepoEvent.class);
        EventData<NodeResource> eventData = resultRepoEvent.getData();
        NodeResource nodeResource = eventData.getResource();

        assertEquals("cm:content node type was not found","cm:folder", nodeResource.getNodeType());
        assertFalse("isFile flag should be FALSE for nodeType=cm:folder. ",nodeResource.isFile());
        assertTrue("isFolder flag should be TRUE for nodeType=cm:folder. ",nodeResource.isFolder());
    }

    @Test
    public void testFileNodeType() throws Exception {

        createNode(ContentModel.TYPE_CONTENT);
        Thread.sleep(3 * 1000); // 3 seconds time-out

        final RepoEvent resultRepoEvent= JacksonSerializer.deserialize(futureResult.get(5, SECONDS),RepoEvent.class);
        EventData<NodeResource> eventData = resultRepoEvent.getData();
        NodeResource nodeResource = eventData.getResource();

        assertEquals("cm:content node type was not found","cm:content", nodeResource.getNodeType());
        assertTrue("isFile flag should be TRUE for nodeType=cm:content. ",nodeResource.isFile());
        assertFalse("isFolder flag should be FALSE for nodeType=cm:content. ",nodeResource.isFolder());
    }
}
