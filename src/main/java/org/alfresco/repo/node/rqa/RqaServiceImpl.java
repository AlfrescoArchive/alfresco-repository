/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
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
package org.alfresco.repo.node.rqa;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.domain.rqa.RqaDAO;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Relational Query Accelerator service
 *
 * @author Chris Shields
 */
public class RqaServiceImpl implements RqaService
{
    private static Log logger = LogFactory.getLog(RqaServiceImpl.class);

    private NodeService nodeService;
    private RQADefinition rqaDefinition;
    private RqaDAO rqaDAO;
    
    public void init()
    {
        rqaDefinition = new RQADefinition();
        
        // Set the types
        QName[] typeArray = { ContentModel.TYPE_CONTENT };
        rqaDefinition.setTypes(new HashSet<>(Arrays.asList(typeArray)));

        // Set the aspects
        QName[] aspectArray = { ContentModel.ASPECT_DUBLINCORE };
        rqaDefinition.setAspects(new HashSet<>(Arrays.asList(aspectArray)));

        // Set the properties

        QName[] propertyArray = { 
                QName.createQName("http://www.alfresco.org/model/content/1.0", "publisher"),
                QName.createQName("http://www.alfresco.org/model/content/1.0", "type"),
                QName.createQName("http://www.alfresco.org/model/content/1.0", "contributor"),
                QName.createQName("http://www.alfresco.org/model/content/1.0", "coverage")
        };
        rqaDefinition.setProperties(new HashSet<>(Arrays.asList(propertyArray)));
    }

    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    public void setRqaDAO(RqaDAO rqaDAO)
    {
        this.rqaDAO = rqaDAO;
    }

    @Override
    public void processNodeCreated(ChildAssociationRef childAssocRef)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("RQA process node created: " + childAssocRef.getQName());
        }
        
        // Get the child node
        NodeRef childNodeRef = childAssocRef.getChildRef();
        
        // Get the child node type
        QName typeQname = nodeService.getType(childNodeRef);
        if (logger.isDebugEnabled())
        {
            logger.debug("RQA process node created type: " + typeQname);
        }
        
        // Check the node type
        // TODO: How do we check that a type is a subtype
        if (rqaDefinition.getTypes().contains(typeQname)){
            createNodeDenorm(childAssocRef);
        }
        else 
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("RQA NOT creating record in table for node: " + childAssocRef.getQName());
            }
        }
    }

    private void createNodeDenorm(ChildAssociationRef childAssocRef)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("RQA create a record in table for node: " + childAssocRef.getQName());
        }

        // Get the properties for the node
        Map<QName, Serializable> properties = nodeService.getProperties(childAssocRef.getChildRef());
        for (Map.Entry<QName, Serializable> property : properties.entrySet()){
            if (logger.isDebugEnabled())
            {
                logger.debug("Property on node: " + property.getKey() + " : " + property.getValue());
            }
            
            // Check property is on denorm table
            if (rqaDefinition.containsProperty(property.getKey()))
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Property is on definition: " + property.getKey() + " : " + property.getValue());
                }
            }
        }
        
        // Get the node's db id
        Serializable nodeDbIdSer = properties.get(ContentModel.PROP_NODE_DBID);
        Long nodeDbId = (Long) nodeDbIdSer;
        
        Map<QName, Serializable> props = new HashMap<>();
        Map<QName, String> columns = new HashMap<>();
        String tablename = "";
        
        rqaDAO.createNode(nodeDbId, properties, tablename, columns);
    }
    
    class RQADefinition
    {
        Set<QName> types = new HashSet<>();
        Set<QName> aspects = new HashSet<>();
        Set<QName> properties = new HashSet<>();

        public Set<QName> getTypes()
        {
            return types;
        }

        public void setTypes(Set<QName> types)
        {
            this.types = types;
        }

        public Set<QName> getAspects()
        {
            return aspects;
        }

        public void setAspects(Set<QName> aspects)
        {
            this.aspects = aspects;
        }

        public Set<QName> getProperties()
        {
            return properties;
        }

        public void setProperties(Set<QName> properties)
        {
            this.properties = properties;
        }
        
        public boolean containsProperty(QName propertyQName)
        {
            for (QName property : properties)
            {
                if (property.equals(propertyQName))
                {
                    return true;
                }
            }
            return false;
        }
    }

}
