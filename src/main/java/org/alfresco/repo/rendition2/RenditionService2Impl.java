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
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.PropertyCheck;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;

/**
 * The Async Rendition service.
 *
 * @author adavis
 */
public class RenditionService2Impl implements RenditionService2, InitializingBean, ContentServicePolicies.OnContentUpdatePolicy
{
    private RenditionDefinitionRegistry2 renditionDefinitionRegistry2;
    private TransformClient transformClient;
    private PolicyComponent policyComponent;

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
            // TODO some nodes should not have renditions - see original code - checkSourceNodeForPreventionClass(sourceNode);

            RenditionDefinition2 renditionDefinition = renditionDefinitionRegistry2.getRenditionDefinition(renditionName);
            if (renditionDefinition == null)
            {
                throw new IllegalArgumentException("The rendition "+renditionName+" has not been registered.");
            }

            transformClient.transform(sourceNodeRef, renditionDefinition);
        }
        catch (Exception e)
        {
            // TODO log exceptions
            throw e;
        }
    }
    @Override
    public void onContentUpdate(NodeRef nodeRef, boolean newContent)
    {
        // TODO
    }
}
