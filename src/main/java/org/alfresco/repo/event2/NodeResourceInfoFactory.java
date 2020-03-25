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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.event2.filter.EventFilter;
import org.alfresco.repo.event2.filter.EventFilterRegistry;
import org.alfresco.repo.event2.filter.NodeAspectFilter;
import org.alfresco.repo.event2.filter.NodePropertyFilter;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.PathUtil;

/**
 * Factory for {@link NodeResourceInfo} objects.
 *
 * @author Jamal Kaabi-Mofrad
 */
public class NodeResourceInfoFactory
{
    private final NodeService nodeService;
    private final NamespaceService namespaceService;
    private final DictionaryService dictionaryService;
    private final EventFilter nodeAspectFilter;
    private final EventFilter nodePropertyFilter;

    public NodeResourceInfoFactory(NodeService nodeService, NamespaceService namespaceService,
                                   DictionaryService dictionaryService, EventFilterRegistry eventFilterRegistry)
    {
        this.nodeService = nodeService;
        this.namespaceService = namespaceService;
        this.dictionaryService = dictionaryService;
        this.nodeAspectFilter = eventFilterRegistry.getFilter(NodeAspectFilter.class)
                    .orElseThrow(getExpSupplier(NodeAspectFilter.class));
        this.nodePropertyFilter = eventFilterRegistry.getFilter(NodePropertyFilter.class)
                    .orElseThrow(getExpSupplier(NodePropertyFilter.class));
    }

    private Supplier<AlfrescoRuntimeException> getExpSupplier(Class<?> clazz)
    {
        return () -> new AlfrescoRuntimeException("The filter '" + clazz.getName() + "' is not registered.");
    }

    public NodeResourceInfo getNodeInfo(NodeRef nodeRef)
    {
        return new NodeResourceInfo(nodeRef);
    }

    /**
     * Holds information about a nodeRef.
     */
    protected class NodeResourceInfo
    {
        private final String name;
        private final NodeRef nodeRef;
        private final String id;
        private final String nodeType;
        private final boolean isFile;
        private final boolean isFolder;
        private final List<String> nodePaths;
        private final Map<String, Serializable> properties;
        private final List<String> aspectNames;

        public NodeResourceInfo(NodeRef nodeRef)
        {
            final Map<QName, Serializable> props = nodeService.getProperties(nodeRef);
            final QName type = nodeService.getType(nodeRef);
            final Path path = nodeService.getPath(nodeRef);
            final Set<QName> aspects = nodeService.getAspects(nodeRef);

            this.nodeRef = nodeRef;
            this.id = nodeRef.getId();
            this.name = (String) props.get(ContentModel.PROP_NAME);
            this.nodeType = type.toPrefixString(namespaceService);
            this.nodePaths = PathUtil.getNodeIdsInReverse(path, false);
            this.isFile = isSubClass(type, ContentModel.TYPE_CONTENT);
            this.isFolder = isSubClass(type, ContentModel.TYPE_FOLDER);
            this.properties = mapToNodeProperties(props);
            this.aspectNames = mapToNodeAspects(aspects);
        }

        private boolean isSubClass(QName className, QName ofClassQName)
        {
            return dictionaryService.isSubClass(className, ofClassQName);
        }

        private Map<String, Serializable> mapToNodeProperties(Map<QName, Serializable> props)
        {
            Map<String, Serializable> filteredProps = new HashMap<>(props.size());

            props.forEach((k, v) -> {
                if (!nodePropertyFilter.isExcluded(k) && v != null)
                {
                    if (isNotEmptyString(v))
                    {
                        filteredProps.put(k.toPrefixString(namespaceService), v);
                    }
                }
            });

            return filteredProps;
        }

        private List<String> mapToNodeAspects(Set<QName> aspects)
        {
            List<String> filteredAspects = new ArrayList<>(aspects.size());

            aspects.forEach(q -> {
                if (!nodeAspectFilter.isExcluded(q))
                {
                    filteredAspects.add(q.toPrefixString(namespaceService));
                }
            });

            return filteredAspects;
        }

        private boolean isNotEmptyString(Serializable ser)
        {
            return !(ser instanceof String) || !((String) ser).isEmpty();
        }

        public String getName()
        {
            return name;
        }

        public NodeRef getNodeRef()
        {
            return nodeRef;
        }

        public String getId()
        {
            return id;
        }

        public String getNodeType()
        {
            return nodeType;
        }

        public boolean isFile()
        {
            return isFile;
        }

        public boolean isFolder()
        {
            return isFolder;
        }

        public List<String> getNodePaths()
        {
            return nodePaths;
        }

        public Map<String, Serializable> getProperties()
        {
            return properties;
        }

        public List<String> getAspectNames()
        {
            return aspectNames;
        }
    }
}
