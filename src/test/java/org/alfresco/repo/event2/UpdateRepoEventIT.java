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

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.event.v1.model.EventData;
import org.alfresco.repo.event.v1.model.NodeResource;
import org.alfresco.repo.event.v1.model.RepoEvent;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.junit.Test;

/**
 * @author Iulian Aftene
 */

public class UpdateRepoEventIT extends AbstractContextAwareRepoEvent
{
    @Test
    public void testUpdateNodeResourceContent() throws Exception
    {
        ContentService contentService = (ContentService) applicationContext.getBean(
            "contentService");
        NodeRef nodeRef = createNode(ContentModel.TYPE_CONTENT);
        Thread.sleep(2000); // wait up to 2 second for the event

        subscribe(futureResult::complete, String.class);

        retryingTransactionHelper.doInTransaction(() -> {
            ContentWriter writer = contentService.getWriter(nodeRef, ContentModel.TYPE_CONTENT,
                true);
            writer.setMimetype(MimetypeMap.MIMETYPE_PDF);
            writer.setEncoding("UTF-8");
            writer.putContent("content");
            return null;
        });

        final RepoEvent<NodeResource> resultRepoEvent = getFutureResult();

        assertEquals("Repo event type", "org.alfresco.event.node.Updated",
            resultRepoEvent.getType());

        EventData<NodeResource> eventData = resultRepoEvent.getData();
        NodeResource nodeResource = eventData.getResource();
        String affectedPropertiesAfter = OBJECT_MAPPER.writeValueAsString(
            nodeResource.getAffectedPropertiesAfter());

        assertTrue(affectedPropertiesAfter.contains("application/pdf"));
    }

    @Test
    public void testUpdateContentTitle() throws Exception
    {
        NodeRef nodeRef = createNode(ContentModel.TYPE_CONTENT);
        Thread.sleep(2000); // wait up to 2 second for the event

        subscribe(futureResult::complete, String.class);

        //update content cm:title property with "new_title" value
        retryingTransactionHelper.doInTransaction(() -> {
            nodeService.setProperty(nodeRef, ContentModel.PROP_TITLE, "new_title");
            return null;
        });

        final RepoEvent<NodeResource> resultRepoEvent = getFutureResult();

        assertTrue("Ttile was not updated. ",
            resultRepoEvent.getData()
                           .getResource()
                           .getAffectedPropertiesAfter()
                           .containsValue("new_title"));
    }

    @Test
    public void testUpdateContentDescription() throws Exception
    {
        NodeRef nodeRef = createNode(ContentModel.TYPE_CONTENT);
        Thread.sleep(2000); // wait up to 2 second for the event

        subscribe(futureResult::complete, String.class);

        //update content cm:description property with "test_description" value
        retryingTransactionHelper.doInTransaction(() -> {
            nodeService.setProperty(nodeRef, ContentModel.PROP_DESCRIPTION, "test_description");
            return null;
        });

        final RepoEvent<NodeResource> resultRepoEvent = getFutureResult();

        assertTrue("Description was not updated. ",
            resultRepoEvent.getData()
                           .getResource()
                           .getAffectedPropertiesAfter()
                           .containsValue("test_description"));
    }

    @Test
    public void testUpdateContentName() throws Exception
    {
        NodeRef nodeRef = createNode(ContentModel.TYPE_CONTENT);
        Thread.sleep(2000); // wait up to 2 second for the event

        subscribe(futureResult::complete, String.class);

        //update cm:name property with "test_new_name" value
        retryingTransactionHelper.doInTransaction(() -> {
            nodeService.setProperty(nodeRef, ContentModel.PROP_NAME, "test_new_name");
            return null;
        });

        final RepoEvent<NodeResource> resultRepoEvent = getFutureResult();

        assertTrue("Name was not updated. ",
            resultRepoEvent.getData()
                           .getResource()
                           .getAffectedPropertiesAfter()
                           .containsValue("test_new_name"));
    }

    @Test
    public void testAddAspectToContent() throws Exception
    {
        NodeRef nodeRef = createNode(ContentModel.TYPE_CONTENT);
        Thread.sleep(2000); // wait up to 2 second for the event

        subscribe(futureResult::complete, String.class);

        // Add cm:versionable aspect with default value
        retryingTransactionHelper.doInTransaction(() -> {
            nodeService.addAspect(nodeRef, ContentModel.ASPECT_VERSIONABLE, null);
            return null;
        });

        final RepoEvent<NodeResource> resultRepoEvent = getFutureResult();

        assertTrue("Aspect was not added. ",
            resultRepoEvent.getData()
                           .getResource()
                           .getAspectNamesAfter()
                           .contains("cm:versionable"));
    }

    @Test
    public void removeAspectFromContentTest() throws Exception
    {
        NodeRef nodeRef = createNode(ContentModel.TYPE_CONTENT);
        Thread.sleep(2000); // wait up to 2 second for the event

        subscribe(futureResult::complete, String.class);

        //remove sys:referenceable aspect
        retryingTransactionHelper.doInTransaction(() -> {
            nodeService.removeAspect(nodeRef, ContentModel.ASPECT_REFERENCEABLE);
            return null;
        });

        final RepoEvent<NodeResource> resultRepoEvent = getFutureResult();

        assertTrue("Aspect was not removed. ",
            resultRepoEvent.getData()
                           .getResource()
                           .getAspectNamesAfter()
                           .isEmpty());
    }
}
