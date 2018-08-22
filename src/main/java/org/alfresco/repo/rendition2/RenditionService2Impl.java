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

import org.alfresco.repo.content.ContentServicePolicies;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.rendition.RenditionPreventionRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.PropertyCheck;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import java.io.InputStream;
import java.util.Set;

/**
 * The Async Rendition service. Replaces the original deprecated RenditionService.
 *
 * @author adavis
 */
public class RenditionService2Impl implements RenditionService2, InitializingBean, ContentServicePolicies.OnContentUpdatePolicy
{
    private static Log logger = LogFactory.getLog(RenditionService2Impl.class);

    private NodeService nodeService;
    private RenditionPreventionRegistry renditionPreventionRegistry;
    private RenditionDefinitionRegistry2 renditionDefinitionRegistry2;
    private TransformClient transformClient;
    private PolicyComponent policyComponent;

    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    public void setRenditionPreventionRegistry(RenditionPreventionRegistry renditionPreventionRegistry)
    {
        this.renditionPreventionRegistry = renditionPreventionRegistry;
    }

    public void setRenditionDefinitionRegistry2(RenditionDefinitionRegistry2 renditionDefinitionRegistry2)
    {
        this.renditionDefinitionRegistry2 = renditionDefinitionRegistry2;
    }

    public void setTransformClient(TransformClient transformClient)
    {
        this.transformClient = transformClient;
    }

    public void setPolicyComponent(PolicyComponent policyComponent)
    {
        this.policyComponent = policyComponent;
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        PropertyCheck.mandatory(this, "nodeService", nodeService);
        PropertyCheck.mandatory(this, "renditionPreventionRegistry", renditionPreventionRegistry);
        PropertyCheck.mandatory(this, "renditionDefinitionRegistry2", renditionDefinitionRegistry2);
        PropertyCheck.mandatory(this, "transformClient", transformClient);
        PropertyCheck.mandatory(this, "policyComponent", policyComponent);

        // TODO policy ...
        policyComponent.bindClassBehaviour(ContentServicePolicies.OnContentUpdatePolicy.QNAME, this, new JavaBehaviour(this, "onContentUpdate"));
    }

    public void render(NodeRef sourceNodeRef, String renditionName)
    {
        try
        {
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
    public void onContentUpdate(NodeRef nodeRef, boolean newContent)
    {
        // TODO
    }
}
