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
import org.junit.Test;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * @author Iulian Aftene
 */

public class DeleteRepoEventIT extends AbstractContextAwareRepoEvent {

    @Test
    public void deleteContent() throws Exception {

        NodeRef nodeRef = createNode(ContentModel.TYPE_CONTENT);
        Thread.sleep(1000); // wait up to 1 second for the event


        CompletableFuture<String> futureResult = new CompletableFuture<>()  ;
        subscribe(futureResult::complete, String.class);

        deleteNode(nodeRef);

        final RepoEvent resultRepoEvent = OBJECT_MAPPER.readValue(futureResult.get(5, SECONDS), new TypeReference<>()
        {
        });

        assertEquals("Repo event type", "org.alfresco.event.node.Deleted", resultRepoEvent.getType());
    }

    @Test
    public void deleteFolderWithContent() throws Exception {

        NodeRef parentNodeRef = createNode(ContentModel.TYPE_CONTAINER);
        createNode(ContentModel.TYPE_FOLDER, parentNodeRef);
        createNode(ContentModel.TYPE_CONTENT, parentNodeRef);
        createNode(ContentModel.TYPE_CONTENT, parentNodeRef);
        Thread.sleep(1000); // wait up to 1 second for the event


        final Set<String> receivedMessages = new ConcurrentSkipListSet<>();
        subscribe(receivedMessages::add, String.class);

        deleteNode(parentNodeRef);
        Thread.sleep( 1000); // wait up to 1 second for the event

        assertFalse(receivedMessages.isEmpty());
        assertEquals("Content was not deleted. ", 3,receivedMessages.size());
    }
}
