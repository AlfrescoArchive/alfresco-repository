/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2018 Alfresco Software Limited
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
package org.alfresco.repo.rendition2;

import org.alfresco.model.ContentModel;
import org.alfresco.model.RenditionModel;
import org.alfresco.repo.content.ContentServicePolicies;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.rendition.RenditionPreventionRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.util.GUID;
import org.alfresco.util.PropertyCheck;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Async Rendition service. Replaces the original deprecated RenditionService.
 *
 * @author adavis
 */
public class RenditionService2Impl implements RenditionService2, InitializingBean, ContentServicePolicies.OnContentUpdatePolicy
{
    public static final String TRANSFORMING_ERROR_MESSAGE = "Some error occurred during document transforming. Error message: ";

    public static final QName DEFAULT_RENDITION_CONTENT_PROP = ContentModel.PROP_CONTENT;
    public static final String DEFAULT_MIMETYPE = MimetypeMap.MIMETYPE_TEXT_PLAIN;
    public static final String DEFAULT_ENCODING = "UTF-8";

    private static Log logger = LogFactory.getLog(RenditionService2Impl.class);

    private NodeService nodeService;
    private ContentService contentService;
    private RenditionPreventionRegistry renditionPreventionRegistry;
    private RenditionDefinitionRegistry2 renditionDefinitionRegistry2;
    private TransformClient transformClient;
    private NamespaceService namespaceService;
    private PolicyComponent policyComponent;
    private BehaviourFilter behaviourFilter;
    private boolean enabled;

    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    public void setContentService(ContentService contentService)
    {
        this.contentService = contentService;
    }

    public void setRenditionPreventionRegistry(RenditionPreventionRegistry renditionPreventionRegistry)
    {
        this.renditionPreventionRegistry = renditionPreventionRegistry;
    }

    public void setRenditionDefinitionRegistry2(RenditionDefinitionRegistry2 renditionDefinitionRegistry2)
    {
        this.renditionDefinitionRegistry2 = renditionDefinitionRegistry2;
    }

    @Override
    public RenditionDefinitionRegistry2 getRenditionDefinitionRegistry2()
    {
        return renditionDefinitionRegistry2;
    }

    public void setNamespaceService(NamespaceService namespaceService)
    {
        this.namespaceService = namespaceService;
    }

    public void setTransformClient(TransformClient transformClient)
    {
        this.transformClient = transformClient;
    }

    public void setPolicyComponent(PolicyComponent policyComponent)
    {
        this.policyComponent = policyComponent;
    }

    public void setBehaviourFilter(BehaviourFilter behaviourFilter)
    {
        this.behaviourFilter = behaviourFilter;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        PropertyCheck.mandatory(this, "nodeService", nodeService);
        PropertyCheck.mandatory(this, "contentService", contentService);
        PropertyCheck.mandatory(this, "renditionPreventionRegistry", renditionPreventionRegistry);
        PropertyCheck.mandatory(this, "renditionDefinitionRegistry2", renditionDefinitionRegistry2);
        PropertyCheck.mandatory(this, "namespaceService", namespaceService);
        PropertyCheck.mandatory(this, "transformClient", transformClient);
        PropertyCheck.mandatory(this, "policyComponent", policyComponent);
        PropertyCheck.mandatory(this, "behaviourFilter", behaviourFilter);

        // TODO policy ...
        policyComponent.bindClassBehaviour(ContentServicePolicies.OnContentUpdatePolicy.QNAME, this, new JavaBehaviour(this, "onContentUpdate"));
    }

    public void render(NodeRef sourceNodeRef, String renditionName)
    {
        try
        {
            if (!isEnabled())
            {
                throw new RenditionService2Exception("Renditions are disabled (system.thumbnail.generate=false).");
            }
            checkSourceNodeForPreventionClass(sourceNodeRef);

            RenditionDefinition2 renditionDefinition = renditionDefinitionRegistry2.getRenditionDefinition(renditionName);
            if (renditionDefinition == null)
            {
                throw new IllegalArgumentException("The rendition "+renditionName+" has not been registered.");
            }

            transformClient.transform(sourceNodeRef, renditionDefinition);
        }
        catch (Exception e)
        {
            logger.debug(e.getMessage());
            throw e;
        }
    }

    /**
     * This method checks whether the specified source node is of a content class which has been registered for
     * rendition prevention.
     *
     * @param sourceNode the node to check.
     * @throws RenditionService2PreventedException if the source node is configured for rendition prevention.
     */
    // This code is based on the old RenditionServiceImpl.checkSourceNodeForPreventionClass(...)
    private void checkSourceNodeForPreventionClass(NodeRef sourceNode)
    {
        if (sourceNode != null && nodeService.exists(sourceNode))
        {
            // A node's content class is its type and all its aspects.
            Set<QName> nodeContentClasses = nodeService.getAspects(sourceNode);
            nodeContentClasses.add(nodeService.getType(sourceNode));

            for (QName contentClass : nodeContentClasses)
            {
                if (renditionPreventionRegistry.isContentClassRegistered(contentClass))
                {
                    String msg = "Node " + sourceNode + " cannot be renditioned as it is of class " + contentClass;
                    logger.debug(msg);
                    throw new RenditionService2PreventedException(msg);
                }
            }
        }
    }

    @Override
    public List<ChildAssociationRef> getRenditions(NodeRef sourceNodeRef)
    {
        // Copy on code from the original RenditionService.
        List<ChildAssociationRef> result = Collections.emptyList();

        // Check that the node has the renditioned aspect applied
        if (nodeService.hasAspect(sourceNodeRef, RenditionModel.ASPECT_RENDITIONED) == true)
        {
            // Get all the renditions that match the given rendition name
            result = nodeService.getChildAssocs(sourceNodeRef, RenditionModel.ASSOC_RENDITION, RegexQNamePattern.MATCH_ALL);
        }
        return result;
    }

    @Override
    public ChildAssociationRef getRenditionByName(NodeRef sourceNodeRef, String renditionName)
    {
        // Based on code from the original RenditionService. renditionName is a String rather than a QName.
        List<ChildAssociationRef> renditions = Collections.emptyList();

        // Thumbnails have a cm: prefix.
        QName renditionQName = QName.resolveToQName(namespaceService, renditionName);

        // Check that the sourceNodeRef has the renditioned aspect applied
        if (nodeService.hasAspect(sourceNodeRef, RenditionModel.ASPECT_RENDITIONED) == true)
        {
            // Get all the renditions that match the given rendition name -
            // there should only be 1 (or 0)
            renditions = this.nodeService.getChildAssocs(sourceNodeRef, RenditionModel.ASSOC_RENDITION, renditionQName);
        }
        if (renditions.isEmpty())
        {
            return null;
        }
        else
        {
            if (renditions.size() > 1 && logger.isDebugEnabled())
            {
                logger.debug("Unexpectedly found " + renditions.size() + " renditions of name " + renditionQName + " on node " + sourceNodeRef);
            }
            return renditions.get(0);
        }
    }

    @Override
    public boolean isEnabled()
    {
        return enabled;
    }

    // Takes a transformation (InputStream) and attach it as a rendition to the source node.
    void consume(NodeRef sourceNodeRef, InputStream transformInputStream, RenditionDefinition2 renditionDefinition)
    {
        logger.debug("Consume a transformation as a rendition node.");

        // TODO work out how the original code gets away with creating a new association and not deleting any previous one if updating.
        ChildAssociationRef childAssoc = createRenditionNodeAssoc(sourceNodeRef, renditionDefinition);
        NodeRef renditionNode = childAssoc.getChildRef();

        ContentWriter contentWriter = contentService.getWriter(renditionNode, DEFAULT_RENDITION_CONTENT_PROP, true);
        String targetMimetype = renditionDefinition.getTargetMimetype();
        contentWriter.setMimetype(targetMimetype);
        contentWriter.setEncoding(DEFAULT_ENCODING);
        ContentWriter renditionWriter = contentWriter;

        try
        {
            renditionWriter.putContent(transformInputStream);
        }
        catch (Exception e)
        {
            // TODO remove the association on failure? The original appears to have a delete compensating action.
            // Unwrap our cause and throw that
            Throwable transformException = e.getCause();
            if (transformException instanceof RuntimeException)
            {
                throw (RuntimeException) e.getCause();
            }
            throw new RenditionService2Exception(TRANSFORMING_ERROR_MESSAGE + e.getCause().getMessage(), e.getCause());
        }

        // TODO remove the temp file if needed.
    }

    private ChildAssociationRef createRenditionNodeAssoc(NodeRef sourceNode, RenditionDefinition2 renditionDefinition)
    {
        String renditionName = renditionDefinition.getRenditionName();

        Map<QName, Serializable> nodeProps = new HashMap<QName, Serializable>();
        nodeProps.put(ContentModel.PROP_NAME, renditionName);
        nodeProps.put(ContentModel.PROP_CONTENT_PROPERTY_NAME, ContentModel.PROP_CONTENT);
        QName assocName = QName.createQName(NamespaceService.RENDITION_MODEL_1_0_URI, GUID.generate());
        QName assocType = RenditionModel.ASSOC_RENDITION;
        QName nodeType = ContentModel.TYPE_CONTENT;

        // Ensure that the creation of rendition children does not cause updates
        // to the modified, modifier properties on the source node
        behaviourFilter.disableBehaviour(sourceNode, ContentModel.ASPECT_AUDITABLE);
        ChildAssociationRef childAssoc = null;
        try
        {
            childAssoc = nodeService.createNode(sourceNode, assocType, assocName, nodeType, nodeProps);

            // Add marker aspect for RenditionService2
            NodeRef renditionNode = childAssoc.getChildRef();
            nodeService.addAspect(renditionNode, RenditionModel.ASPECT_RENDITION2, null);

            if (logger.isDebugEnabled())
            {
                logger.debug("Created rendition node " + childAssoc + " as child of " + sourceNode + " with assoc-type " + assocType);
            }
        }
        finally
        {
            behaviourFilter.enableBehaviour(sourceNode, ContentModel.ASPECT_AUDITABLE);
        }
        return childAssoc;
    }

    @Override
    public void onContentUpdate(NodeRef nodeRef, boolean newContent)
    {
        // TODO
    }
}
