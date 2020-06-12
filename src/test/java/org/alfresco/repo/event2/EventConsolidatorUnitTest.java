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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;
import org.alfresco.model.ContentModel;
import org.junit.Test;

public class EventConsolidatorUnitTest extends TestCase
{
    private NodeResourceHelper nodeResourceHelper = mock(NodeResourceHelper.class);
    
    @Test
    public void testGetMappedAspectsBeforeRemovedAndAddedEmpty()
    {
        EventConsolidator eventConsolidator = new EventConsolidator(nodeResourceHelper);
        
        Set<String> currentApects = new HashSet<>();
        currentApects.add("cm:geographic");
        currentApects.add("cm:auditable");

        Set<String> mappedAspectsBefore = eventConsolidator.getMappedAspectsBefore(currentApects);

        assertEquals(0, mappedAspectsBefore.size());
    }

    @Test
    public void testGetMappedAspectsBefore_AspectRemoved()
    {
        EventConsolidator eventConsolidator = new EventConsolidator(nodeResourceHelper);
        eventConsolidator.addAspect(ContentModel.ASSOC_CONTAINS);

        Set<String> currentApects = new HashSet<>();
        currentApects.add("cm:geographic");
        currentApects.add("cm:auditable");

        Set<String> removed = new HashSet<>();
        Set<String> added = new HashSet<>();
        removed.add("cm:contains");

        when(nodeResourceHelper.mapToNodeAspects(eventConsolidator.getAspectsRemoved())).thenReturn(removed);
        when(nodeResourceHelper.mapToNodeAspects(eventConsolidator.getAspectsAdded())).thenReturn(added);

        Set<String> mappedAspectsBefore = eventConsolidator.getMappedAspectsBefore(currentApects);

        assertEquals(3, mappedAspectsBefore.size());
        System.out.println(mappedAspectsBefore);
    }

    @Test
    public void testGetMappedAspectsBefore_AspectAdded()
    {
        EventConsolidator eventConsolidator = new EventConsolidator(nodeResourceHelper);
        eventConsolidator.addAspect(ContentModel.ASSOC_CONTAINS);

        Set<String> currentApects = new HashSet<>();
        currentApects.add("cm:geographic");
        currentApects.add("cm:auditable");

        Set<String> removed = new HashSet<>();
        Set<String> added = new HashSet<>();
        added.add("cm:auditable");

        when(nodeResourceHelper.mapToNodeAspects(eventConsolidator.getAspectsRemoved())).thenReturn(removed);
        when(nodeResourceHelper.mapToNodeAspects(eventConsolidator.getAspectsAdded())).thenReturn(added);

        Set<String> mappedAspectsBefore = eventConsolidator.getMappedAspectsBefore(currentApects);

        assertEquals(1, mappedAspectsBefore.size());
        System.out.println(mappedAspectsBefore);
    }


    @Test
    public void testGetMappedAspectsBefore_AspectAddedAndRemoved()
    {
        EventConsolidator eventConsolidator = new EventConsolidator(nodeResourceHelper);
        eventConsolidator.addAspect(ContentModel.ASSOC_CONTAINS);

        Set<String> currentApects = new HashSet<>();
        currentApects.add("cm:geographic");
        currentApects.add("cm:auditable");

        Set<String> removed = new HashSet<>();
        removed.add("cm:contains");
        Set<String> added = new HashSet<>();
        added.add("cm:contains");
        
        when(nodeResourceHelper.mapToNodeAspects(eventConsolidator.getAspectsRemoved())).thenReturn(removed);
        when(nodeResourceHelper.mapToNodeAspects(eventConsolidator.getAspectsAdded())).thenReturn(added);
        
        Set<String> mappedAspectsBefore = eventConsolidator.getMappedAspectsBefore(currentApects);
        
        assertEquals(2, mappedAspectsBefore.size());
        System.out.println(mappedAspectsBefore);
    }

    @Test
    public void testGetMappedAspectsBefore_AspectRemovedAndAdded()
    {
        EventConsolidator eventConsolidator = new EventConsolidator(nodeResourceHelper);
        eventConsolidator.addAspect(ContentModel.ASSOC_CONTAINS);
        eventConsolidator.removeAspect(ContentModel.ASSOC_CONTAINS);

        Set<String> currentApects = new HashSet<>();
        currentApects.add("cm:geographic");
        currentApects.add("cm:auditable");
        currentApects.add("cm:contains");
        
        Set<String> removed = new HashSet<>();
        removed.add("cm:contains");
        Set<String> added = new HashSet<>();
        added.add("cm:contains");
        
        when(nodeResourceHelper.mapToNodeAspects(eventConsolidator.getAspectsRemoved())).thenReturn(removed);
        when(nodeResourceHelper.mapToNodeAspects(eventConsolidator.getAspectsAdded())).thenReturn(added);
        
        Set<String> mappedAspectsBefore = eventConsolidator.getMappedAspectsBefore(currentApects);
        
        System.out.println(mappedAspectsBefore);
        assertEquals(0, mappedAspectsBefore.size());
    }

    @Test
    public void testGetMappedAspectsBefore_AspectAddedTwiceRemovedOnce()
    {
        EventConsolidator eventConsolidator = new EventConsolidator(nodeResourceHelper);

        eventConsolidator.addAspect(ContentModel.ASSOC_CONTAINS);
        eventConsolidator.addAspect(ContentModel.ASSOC_CONTAINS);

        eventConsolidator.removeAspect(ContentModel.ASSOC_CONTAINS);

        Set<String> currentApects = new HashSet<>();
        currentApects.add("cm:geographic");
        currentApects.add("cm:auditable");
        currentApects.add("cm:contains");
        
        Set<String> removed = new HashSet<>();
        Set<String> added = new HashSet<>();
        added.add("cm:contains");
        
        when(nodeResourceHelper.mapToNodeAspects(eventConsolidator.getAspectsRemoved())).thenReturn(removed);
        when(nodeResourceHelper.mapToNodeAspects(eventConsolidator.getAspectsAdded())).thenReturn(added);
        
        Set<String> mappedAspectsBefore = eventConsolidator.getMappedAspectsBefore(currentApects);
        
        System.out.println("Before: " + mappedAspectsBefore);
        assertEquals(2, mappedAspectsBefore.size());
    }


    @Test
    public void testGetMappedAspectsBefore_AspectRemovedTwiceAddeddOnce()
    {
        EventConsolidator eventConsolidator = new EventConsolidator(nodeResourceHelper);

        eventConsolidator.addAspect(ContentModel.ASSOC_CONTAINS);
        eventConsolidator.addAspect(ContentModel.ASSOC_CONTAINS);

        eventConsolidator.removeAspect(ContentModel.ASSOC_CONTAINS);

        Set<String> currentApects = new HashSet<>();
        currentApects.add("cm:geographic");
        currentApects.add("cm:auditable");
        currentApects.add("cm:contains");

        Set<String> removed = new HashSet<>();
        removed.add("cm:contains");
        removed.add("cm:contains");
        Set<String> added = new HashSet<>();
        added.add("cm:contains");
        
        when(nodeResourceHelper.mapToNodeAspects(eventConsolidator.getAspectsRemoved())).thenReturn(removed);
        when(nodeResourceHelper.mapToNodeAspects(eventConsolidator.getAspectsAdded())).thenReturn(added);
        
        Set<String> mappedAspectsBefore = eventConsolidator.getMappedAspectsBefore(currentApects);
        
        System.out.println(mappedAspectsBefore);
        assertEquals(2, mappedAspectsBefore.size());
    }
    
    @Test
    public void testGetMappedAspectsBefore_FilteredAspectAdded()
    {
        EventConsolidator eventConsolidator = new EventConsolidator(nodeResourceHelper);
        eventConsolidator.addAspect(ContentModel.ASPECT_COPIEDFROM);

        Set<String> currentApects = new HashSet<>();
        currentApects.add("cm:geographic");
        currentApects.add("cm:auditable");

        Set<String> removed = new HashSet<>();
        Set<String> added = new HashSet<>();

        when(nodeResourceHelper.mapToNodeAspects(eventConsolidator.getAspectsRemoved())).thenReturn(removed);
        when(nodeResourceHelper.mapToNodeAspects(eventConsolidator.getAspectsAdded())).thenReturn(added);

        Set<String> mappedAspectsBefore = eventConsolidator.getMappedAspectsBefore(currentApects);

        System.out.println(mappedAspectsBefore);
        assertEquals(0, mappedAspectsBefore.size());
    }

    @Test
    public void testAddAspect()
    {
        EventConsolidator eventConsolidator = new EventConsolidator(nodeResourceHelper);
        eventConsolidator.addAspect(ContentModel.ASSOC_CONTAINS);
        
        assertEquals(1, eventConsolidator.getAspectsAdded().size());
        assertEquals(0, eventConsolidator.getAspectsRemoved().size());
        assertTrue(eventConsolidator.getAspectsAdded().contains(ContentModel.ASSOC_CONTAINS));
    }

    @Test
    public void testRemoveAspect()
    {
        EventConsolidator eventConsolidator = new EventConsolidator(nodeResourceHelper);
        eventConsolidator.removeAspect(ContentModel.ASSOC_CONTAINS);

        assertEquals(0, eventConsolidator.getAspectsAdded().size());
        assertEquals(1, eventConsolidator.getAspectsRemoved().size());
        assertTrue(eventConsolidator.getAspectsRemoved().contains(ContentModel.ASSOC_CONTAINS));
    }

    @Test
    public void testAddAspectRemoveAspect()
    {
        EventConsolidator eventConsolidator = new EventConsolidator(nodeResourceHelper);
        eventConsolidator.addAspect(ContentModel.ASSOC_CONTAINS);
        eventConsolidator.removeAspect(ContentModel.ASSOC_CONTAINS);

        assertEquals(0, eventConsolidator.getAspectsAdded().size());
        assertEquals(0, eventConsolidator.getAspectsRemoved().size());
    }

    @Test
    public void testRemvoeAspectAddAspect()
    {
        EventConsolidator eventConsolidator = new EventConsolidator(nodeResourceHelper);
        eventConsolidator.removeAspect(ContentModel.ASSOC_CONTAINS);
        eventConsolidator.addAspect(ContentModel.ASSOC_CONTAINS);

        assertEquals(0, eventConsolidator.getAspectsAdded().size());
        assertEquals(0, eventConsolidator.getAspectsRemoved().size());
    }

    @Test
    public void testAddAspectTwiceRemoveAspectOnce()
    {
        EventConsolidator eventConsolidator = new EventConsolidator(nodeResourceHelper);
        eventConsolidator.addAspect(ContentModel.ASSOC_CONTAINS);
        eventConsolidator.removeAspect(ContentModel.ASSOC_CONTAINS);
        eventConsolidator.addAspect(ContentModel.ASSOC_CONTAINS);

        assertEquals(1, eventConsolidator.getAspectsAdded().size());
        assertEquals(0, eventConsolidator.getAspectsRemoved().size());
        assertTrue(eventConsolidator.getAspectsAdded().contains(ContentModel.ASSOC_CONTAINS));
    }

    @Test
    public void testAddAspectOnceRemoveAspectTwice()
    {
        EventConsolidator eventConsolidator = new EventConsolidator(nodeResourceHelper);
        eventConsolidator.removeAspect(ContentModel.ASSOC_CONTAINS);
        eventConsolidator.addAspect(ContentModel.ASSOC_CONTAINS);
        eventConsolidator.removeAspect(ContentModel.ASSOC_CONTAINS);

        assertEquals(0, eventConsolidator.getAspectsAdded().size());
        assertEquals(1, eventConsolidator.getAspectsRemoved().size());
        assertTrue(eventConsolidator.getAspectsRemoved().contains(ContentModel.ASSOC_CONTAINS));
    }
}
