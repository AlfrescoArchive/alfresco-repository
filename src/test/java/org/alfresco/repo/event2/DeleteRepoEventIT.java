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
import org.alfresco.repo.event.v1.model.EventData;
import org.alfresco.repo.event.v1.model.NodeResource;
import org.alfresco.repo.event.v1.model.RepoEvent;
import org.alfresco.service.cmr.repository.NodeRef;
import org.junit.Test;

/**
 * @author Iulian Aftene
 */
public class DeleteRepoEventIT extends AbstractContextAwareRepoEvent
{
    @Test
    public void deleteContent()
    {
        NodeRef nodeRef = createNode(ContentModel.TYPE_CONTENT);
        NodeResource createdResource = getNodeResource(1);

        deleteNode(nodeRef);
        final RepoEvent<NodeResource> resultRepoEvent = getRepoEvent(2);

        assertEquals("Repo event type:", EventType.NODE_DELETED.getType(), resultRepoEvent.getType());
        assertEquals(createdResource.getId(), getNodeResource(resultRepoEvent).getId());

        // There should be no resourceBefore
        EventData<NodeResource> eventData = getEventData(resultRepoEvent);
        assertNull("There should be no 'resourceBefore' object for the Deleted event type.",
                   eventData.getResourceBefore());
    }

    @Test
    public void deleteFolderWithContent()
    {
        NodeRef grandParent = createNode(ContentModel.TYPE_FOLDER);
        NodeRef parent = createNode(ContentModel.TYPE_FOLDER, grandParent);
        createNode(ContentModel.TYPE_CONTENT, parent);
        createNode(ContentModel.TYPE_CONTENT, parent);

        // 4 Created Events
        checkNumOfEvents(4);

        deleteNode(grandParent);
        // 4 Deleted events + 4 created events
        checkNumOfEvents(8);
    }
}
