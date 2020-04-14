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
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.event.v1.model.RepoEvent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Iulian Aftene
 */

public class DeleteRepoEventIT extends ContextAwareRepoEvent
{
    @Test
    public void deleteContent() throws Exception {

        NodeRef nodeRef = createNode(ContentModel.TYPE_CONTENT);

        CompletableFuture<String> futureResult = new CompletableFuture<>()  ;
        subscribe(futureResult::complete, String.class);

        deleteNode(nodeRef);
        Thread.sleep(3 * 1000); // wait up to 3 seconds for the event

        final RepoEvent resultRepoEvent = JacksonSerializer.deserialize(futureResult.get(3, SECONDS), RepoEvent.class);
        Assert.assertTrue("Repo event retrieved type is "+ resultRepoEvent.getType()+ " .Expected \"Deleted\"",
            resultRepoEvent.getType().equals("org.alfresco.event.node.Deleted"));
    }

    @Test
    public void deleteFolderWithContent() throws Exception {

        NodeRef parentNodeRef = createNode(ContentModel.TYPE_CONTAINER);
        createNode(ContentModel.TYPE_FOLDER, parentNodeRef);
        createNode(ContentModel.TYPE_CONTENT, parentNodeRef);
        createNode(ContentModel.TYPE_CONTENT, parentNodeRef);

        final Set<String> receivedMessages = new ConcurrentSkipListSet<>();
        subscribe(receivedMessages::add, String.class);

        deleteNode(parentNodeRef);
        Thread.sleep(3 * 1000); // wait up to 3 seconds for the event

        assertFalse(receivedMessages.isEmpty());
        assertEquals("Content was not deleted. ", 3,receivedMessages.size());
    }
}
