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

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.alfresco.repo.event.v1.model.NodeResource;
import org.alfresco.repo.event.v1.model.RepoEvent;
import org.alfresco.repo.event.v1.model.Resource;
import org.junit.Test;

/**
 * Tests {@link EventSelector} class.
 *
 * @author Jamal Kaabi-Mofrad
 */
public class EventSelectorTest
{
    @Test
    public void nodeCreatedSelector()
    {
        List<RepoEvent<? extends Resource>> events = new ArrayList<>(3);

        RepoEvent<NodeResource> repoEvent1 = RepoEvent.<NodeResource>builder().setId(UUID.randomUUID().toString())
                    .setSource(URI.create("/" + UUID.randomUUID().toString()))
                    .setTime(ZonedDateTime.now())
                    .setType("org.alfresco.event.node.Updated")
                    .build();
        RepoEvent<NodeResource> repoEvent2 = RepoEvent.<NodeResource>builder().setId(UUID.randomUUID().toString())
                    .setSource(URI.create("/" + UUID.randomUUID().toString()))
                    .setTime(ZonedDateTime.now())
                    .setType("org.alfresco.event.node.Created")
                    .build();
        RepoEvent<NodeResource> repoEvent3 = RepoEvent.<NodeResource>builder().setId(UUID.randomUUID().toString())
                    .setSource(URI.create("/" + UUID.randomUUID().toString()))
                    .setTime(ZonedDateTime.now())
                    .setType("org.alfresco.event.node.Updated")
                    .build();

        events.add(repoEvent1);
        events.add(repoEvent2);
        events.add(repoEvent3);

        assertEquals("Node 'Created' event takes precedence over node 'Updated' event", repoEvent2,
                     EventSelector.getEventToSend(events));
    }

    @Test
    public void nodeDeletedSelector()
    {
        List<RepoEvent<? extends Resource>> events = new ArrayList<>(3);

        RepoEvent<NodeResource> repoEvent1 = RepoEvent.<NodeResource>builder().setId(UUID.randomUUID().toString())
                    .setSource(URI.create("/" + UUID.randomUUID().toString()))
                    .setTime(ZonedDateTime.now())
                    .setType("org.alfresco.event.node.Created")
                    .build();
        RepoEvent<NodeResource> repoEvent2 = RepoEvent.<NodeResource>builder().setId(UUID.randomUUID().toString())
                    .setSource(URI.create("/" + UUID.randomUUID().toString()))
                    .setTime(ZonedDateTime.now())
                    .setType("org.alfresco.event.node.Updated")
                    .build();
        RepoEvent<NodeResource> repoEvent3 = RepoEvent.<NodeResource>builder().setId(UUID.randomUUID().toString())
                    .setSource(URI.create("/" + UUID.randomUUID().toString()))
                    .setTime(ZonedDateTime.now())
                    .setType("org.alfresco.event.node.Deleted")
                    .build();

        events.add(repoEvent1);
        events.add(repoEvent2);
        events.add(repoEvent3);

        assertEquals("Node 'Deleted' event takes precedence over node 'Created' and 'Updated' event", repoEvent3,
                     EventSelector.getEventToSend(events));
    }

    @Test
    public void nodeUpdatedSelector()
    {
        List<RepoEvent<? extends Resource>> events = new ArrayList<>(2);
        RepoEvent<NodeResource> repoEvent1 = RepoEvent.<NodeResource>builder().setId(UUID.randomUUID().toString())
                    .setSource(URI.create("/" + UUID.randomUUID().toString()))
                    .setTime(ZonedDateTime.now())
                    .setType("org.alfresco.event.node.Updated")
                    .build();
        RepoEvent<NodeResource> repoEvent2 = RepoEvent.<NodeResource>builder().setId(UUID.randomUUID().toString())
                    .setSource(URI.create("/" + UUID.randomUUID().toString()))
                    .setTime(ZonedDateTime.now())
                    .setType("org.alfresco.event.node.Updated")
                    .build();

        events.add(repoEvent1);
        events.add(repoEvent2);

        assertEquals(repoEvent1, EventSelector.getEventToSend(events));
    }
}