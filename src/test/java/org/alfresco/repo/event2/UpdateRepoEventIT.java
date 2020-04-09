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

public class UpdateRepoEventIT extends ContextAwareRepoEvent
{

    @Test
    public void testUpdateNodeResourceContent() throws Exception {

        ContentService contentService=(ContentService) applicationContext.getBean("contentService");
        NodeRef nodeRef = createNode(ContentModel.TYPE_CONTENT);

        CompletableFuture<String> futureResult = new CompletableFuture<>();
        subscribe(futureResult::complete, String.class);

        retryingTransactionHelper.doInTransaction(() -> {
            ContentWriter writer = contentService.getWriter(nodeRef, ContentModel.TYPE_CONTENT, true);
            writer.setMimetype(MimetypeMap.MIMETYPE_PDF);
            writer.setEncoding("UTF-8");
            writer.putContent("content");
            return null;
        });

        final RepoEvent resultRepoEvent= JacksonSerializer.deserialize(futureResult.get(5, SECONDS),RepoEvent.class);

        assertTrue("Repo event retrieved type is "+ resultRepoEvent.getType()+ " .Expected \"Updated\"",
            resultRepoEvent.getType().equals("org.alfresco.event.node.Updated"));

        EventData<NodeResource> eventData = resultRepoEvent.getData();
        NodeResource nodeResource = eventData.getResource();
        String affectedPropertiesAfter = new String(JacksonSerializer.serialize(nodeResource.getAffectedPropertiesAfter()));
        assertTrue(affectedPropertiesAfter.contains("application/pdf"));
    }


    @Test
    public void testUpdateContentTitle() throws Exception {

        NodeRef nodeRef = createNode(ContentModel.TYPE_CONTENT);

        CompletableFuture<String> futureResult = new CompletableFuture<>();
        subscribe(futureResult::complete, String.class);

        retryingTransactionHelper.doInTransaction(() -> {
            nodeService.setProperty(nodeRef, ContentModel.PROP_TITLE, "new_title");
            return null;
        });

        final String result = futureResult.get(5, SECONDS);
        final RepoEvent<NodeResource> resultRepoEvent= JacksonSerializer.deserialize(futureResult.get(5, SECONDS),RepoEvent.class);

        assertTrue("Ttile was not updated. ",
            resultRepoEvent.getData()
                .getResource()
                .getAffectedPropertiesAfter()
                .containsValue("new_title"));
    }

    @Test
    public void testUpdateContentDescription() throws Exception {

        NodeRef nodeRef = createNode(ContentModel.TYPE_CONTENT);

        CompletableFuture<String> futureResult = new CompletableFuture<>();
        subscribe(futureResult::complete, String.class);

        retryingTransactionHelper.doInTransaction(() -> {
            nodeService.setProperty(nodeRef, ContentModel.PROP_DESCRIPTION, "test_description");
            return null;
        });

        final String result = futureResult.get(5, SECONDS);
        final RepoEvent<NodeResource> resultRepoEvent= JacksonSerializer.deserialize(futureResult.get(5, SECONDS),RepoEvent.class);

        assertTrue("Description was not updated. ",
            resultRepoEvent.getData()
                .getResource()
                .getAffectedPropertiesAfter()
                .containsValue("test_description"));
    }



    @Test
    public void testAddAspectToContent() throws Exception {

        NodeRef nodeRef = createNode(ContentModel.TYPE_CONTENT);

        CompletableFuture<String> futureResult = new CompletableFuture<>();
        subscribe(futureResult::complete, String.class);

        // Add cm:versionable aspect with default value
        retryingTransactionHelper.doInTransaction(() -> {
            nodeService.addAspect(nodeRef, ContentModel.ASPECT_VERSIONABLE, null);
            return null;
        });

        final RepoEvent<NodeResource> resultRepoEvent= JacksonSerializer.deserialize(futureResult.get(5, SECONDS),RepoEvent.class);
        assertTrue("Aspect was not added. ",
            resultRepoEvent.getData()
                .getResource()
                .getAspectNamesAfter()
                .contains("cm:versionable"));
    }

    @Test
    public void removeAspectFromContentTest() throws Exception {

        NodeRef nodeRef = createNode(ContentModel.TYPE_CONTENT);

        CompletableFuture<String> futureResult = new CompletableFuture<>();
        subscribe(futureResult::complete, String.class);

        //remove sys:referenceable aspect
        retryingTransactionHelper.doInTransaction(() -> {
            nodeService.removeAspect(nodeRef,ContentModel.ASPECT_REFERENCEABLE);
            return null;
        });

        final RepoEvent<NodeResource> resultRepoEvent= JacksonSerializer.deserialize(futureResult.get(5, SECONDS),RepoEvent.class);
        assertTrue("Aspect was not removed. ",
            resultRepoEvent.getData()
                .getResource()
                .getAspectNamesAfter()
                .isEmpty());
    }
}
