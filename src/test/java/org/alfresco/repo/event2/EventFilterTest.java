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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.alfresco.model.ContentModel;
import org.alfresco.model.ForumModel;
import org.alfresco.repo.event2.filter.AbstractEventFilter;
import org.alfresco.repo.event2.filter.EventFilter;
import org.alfresco.repo.event2.filter.EventFilterRegistry;
import org.alfresco.repo.event2.filter.NodeAspectFilter;
import org.alfresco.repo.event2.filter.NodePropertyFilter;
import org.alfresco.repo.event2.filter.NodeTypeFilter;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.namespace.NamespaceException;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.OneToManyHashBiMap;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.stubbing.Answer;

/**
 * Tests event filters.
 *
 * @author Jamal Kaabi-Mofrad
 */
public class EventFilterTest
{

    private static EventFilterRegistry registry;
    private static NamespaceService namespaceService;

    @BeforeClass
    public static void setUp()
    {
        registry = new EventFilterRegistry();

        DictionaryService dictionaryService = mock(DictionaryService.class);
        when(dictionaryService.getSubTypes(any(), anyBoolean())).thenAnswer((Answer<Collection<QName>>) invocation -> {
            QName name = invocation.getArgument(0);
            return Collections.singleton(name);
        });

        namespaceService = new MockNamespaceServiceImpl();
        namespaceService.registerNamespace(NamespaceService.SYSTEM_MODEL_PREFIX,
                                           NamespaceService.SYSTEM_MODEL_1_0_URI);
        namespaceService.registerNamespace(NamespaceService.CONTENT_MODEL_PREFIX,
                                           NamespaceService.CONTENT_MODEL_1_0_URI);
        namespaceService.registerNamespace(NamespaceService.FORUMS_MODEL_PREFIX,
                                           NamespaceService.FORUMS_MODEL_1_0_URI);

        NodePropertyFilter propertyFilter = new NodePropertyFilter();
        propertyFilter.setEventFilterRegistry(registry);
        propertyFilter.setNamespaceService(namespaceService);
        propertyFilter.setDictionaryService(dictionaryService);
        propertyFilter.init();

        NodeTypeFilter typeFilter = new NodeTypeFilter("sys:*, fm:*, cm:thumbnail");
        typeFilter.setEventFilterRegistry(registry);
        typeFilter.setNamespaceService(namespaceService);
        typeFilter.setDictionaryService(dictionaryService);
        typeFilter.init();

        NodeAspectFilter aspectFilter = new NodeAspectFilter("cm:workingcopy");
        aspectFilter.setEventFilterRegistry(registry);
        aspectFilter.setNamespaceService(namespaceService);
        aspectFilter.setDictionaryService(dictionaryService);
        aspectFilter.init();
    }

    @Test
    public void nodePropertyFilter()
    {
        NodePropertyFilter propertyFilter = getFilter(NodePropertyFilter.class);

        assertTrue("System properties are excluded by default.",
                   propertyFilter.isExcluded(ContentModel.PROP_NODE_UUID));

        assertTrue("System properties are excluded by default.",
                   propertyFilter.isExcluded(ContentModel.PROP_NODE_DBID));

        assertFalse(propertyFilter.isExcluded(ContentModel.PROP_TITLE));
    }

    @Test
    public void nodeTypeFilter()
    {
        NodeTypeFilter typeFilter = getFilter(NodeTypeFilter.class);

        assertTrue("Thumbnail node type should have been filtered.",
                   typeFilter.isExcluded(ContentModel.TYPE_THUMBNAIL));

        assertTrue("System folder node types are excluded by default.",
                   typeFilter.isExcluded(ContentModel.TYPE_SYSTEM_FOLDER));

        assertTrue("System node type should have been filtered (sys:*).",
                   typeFilter.isExcluded(QName.createQName("sys:testSomeSystemType", namespaceService)));

        assertTrue("Forum node type should have been filtered (fm:*).",
                   typeFilter.isExcluded(ForumModel.TYPE_POST));

        assertFalse(typeFilter.isExcluded(ContentModel.TYPE_FOLDER));
    }

    @Test
    public void nodeAspectFilter()
    {
        NodeAspectFilter aspectFilter = getFilter(NodeAspectFilter.class);

        assertTrue("Working copy aspect should have been filtered.",
                   aspectFilter.isExcluded(ContentModel.ASPECT_WORKING_COPY));

        assertFalse(aspectFilter.isExcluded(ContentModel.ASPECT_TITLED));
    }

    private <T extends AbstractEventFilter> T getFilter(Class<T> glazz)
    {
        Optional<EventFilter> filter = registry.getFilter(glazz);
        assertTrue(filter.isPresent());
        return glazz.cast(filter.get());
    }

    /**
     * Mock Namespace service
     */
    public static class MockNamespaceServiceImpl implements NamespaceService
    {
        private final OneToManyHashBiMap<String, String> map = new OneToManyHashBiMap<>();

        public void registerNamespace(String prefix, String uri)
        {
            this.map.putSingleValue(uri, prefix);
        }

        public void unregisterNamespace(String prefix)
        {
            this.map.removeValue(prefix);
        }

        public String getNamespaceURI(String prefix) throws NamespaceException
        {
            return this.map.getKey(prefix);
        }

        public Collection<String> getPrefixes(String namespaceURI) throws NamespaceException
        {
            return this.map.get(namespaceURI);
        }

        public Collection<String> getPrefixes()
        {
            return this.map.flatValues();
        }

        public Collection<String> getURIs()
        {
            return this.map.keySet();
        }
    }
}