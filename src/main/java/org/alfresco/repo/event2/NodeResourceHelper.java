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

import org.alfresco.model.ContentModel;
import org.alfresco.repo.event.v1.model.NodeResource;
import org.alfresco.repo.event2.filter.EventFilterRegistry;
import org.alfresco.repo.event2.filter.NodeAspectFilter;
import org.alfresco.repo.event2.filter.NodePropertyFilter;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.MLText;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.namespace.NamespaceException;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.PathUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Helper for {@link NodeResource} objects.
 *
 * @author Jamal Kaabi-Mofrad
 */
public class NodeResourceHelper
{
    private static final Log LOGGER = LogFactory.getLog(NodeResourceHelper.class);

    private final NodeService nodeService;
    private final NamespaceService namespaceService;
    private final DictionaryService dictionaryService;
    private final NodeAspectFilter nodeAspectFilter;
    private final NodePropertyFilter nodePropertyFilter;

    public NodeResourceHelper(NodeService nodeService, NamespaceService namespaceService,
                              DictionaryService dictionaryService, EventFilterRegistry eventFilterRegistry)
    {
        this.nodeService = nodeService;
        this.namespaceService = namespaceService;
        this.dictionaryService = dictionaryService;
        this.nodeAspectFilter = eventFilterRegistry.getNodeAspectFilter();
        this.nodePropertyFilter = eventFilterRegistry.getNodePropertyFilter();
    }

    public NodeResource.Builder createNodeResourceBuilder(NodeRef nodeRef)
    {
        final QName type = nodeService.getType(nodeRef);
        final Path path = nodeService.getPath(nodeRef);

        return NodeResource.builder().setId(nodeRef.getId())
                           .setNodeType(type.toPrefixString(namespaceService))
                           .setIsFile(isSubClass(type, ContentModel.TYPE_CONTENT))
                           .setIsFolder(isSubClass(type, ContentModel.TYPE_FOLDER))
                           .setPrimaryHierarchy(PathUtil.getNodeIdsInReverse(path, false));
    }

    private boolean isSubClass(QName className, QName ofClassQName)
    {
        return dictionaryService.isSubClass(className, ofClassQName);
    }

    public Map<String, Serializable> mapToNodeProperties(Map<QName, Serializable> props)
    {
        Map<String, Serializable> filteredProps = new HashMap<>(props.size());

        props.forEach((k, v) -> {
            if (!nodePropertyFilter.isExcluded(k) && v != null)
            {
                if (v instanceof MLText)
                {
                    //TODO - should we send all of the values if multiple locales exist?
                    v = ((MLText) v).getDefaultValue();
                }

                if (isNotEmptyString(v))
                {
                    String key = k.getNamespaceURI();
                    try
                    {
                        key = k.toPrefixString(namespaceService);
                    }
                    catch (NamespaceException e)
                    {
                        LOGGER.debug(e);
                    }
                    filteredProps.put(key, v);
                }
            }
        });

        return filteredProps;
    }

    public List<String> mapToNodeAspects(Set<QName> aspects)
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

    public QName getNodeType(NodeRef nodeRef)
    {
       return nodeService.getType(nodeRef);
    }

    public Serializable getProperty(NodeRef nodeRef, QName qName)
    {
        return nodeService.getProperty(nodeRef, qName);
    }

    public Map<String, Serializable> getMappedProperties(NodeRef nodeRef)
    {
        return mapToNodeProperties(nodeService.getProperties(nodeRef));
    }

    public Map<QName, Serializable> getProperties(NodeRef nodeRef)
    {
        return nodeService.getProperties(nodeRef);
    }

    public List<String> getMappedAspects(NodeRef nodeRef)
    {
        return mapToNodeAspects(nodeService.getAspects(nodeRef));
    }
}