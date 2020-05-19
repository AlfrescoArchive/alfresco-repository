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
package org.alfresco.repo.node;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.alfresco.repo.node.NodeServicePolicies.OnDownloadNodePolicy;
import org.alfresco.repo.policy.ClassPolicyDelegate;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.tenant.TenantService;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.mozilla.classfile.ClassFileWriter.MHandle;

public class DownloadNotifierServiceImpl implements DownloadNotifierService
{
    private NodeService nodeService;
    
    private PolicyComponent policyComponent;
    private TenantService tenantService;
    private Set<String> storesToIgnorePolicies = Collections.emptySet();

    private ClassPolicyDelegate<OnDownloadNodePolicy> onDownloadNodeDelegate;

    /**
     * Registers the node policies as well as node indexing behaviour
     */
    public void init()
    {
        // Register the various policies
        onDownloadNodeDelegate = policyComponent.registerClassPolicy(NodeServicePolicies.OnDownloadNodePolicy.class);
    }

    @Override
    public void downloadNotify(NodeRef nodeRef)
    {
        
        boolean isZipDownload = isZipDownload(nodeRef);
        if (isZipDownload){
            handleZipDownload(nodeRef);
        } else {
            invokeOnDownloadNode(nodeRef);
        }
    }

    private void handleZipDownload(NodeRef nodeRef)
    {
        // TODO: Loop through all the associated nodes for the zip node and call invokeOnDownloadNode
        // Currently we are just call invokeOnDownloadNode for the zip node.
        invokeOnDownloadNode(nodeRef);
    }

    private boolean isZipDownload(NodeRef nodeRef)
    {
        // TODO: Check for download zip nodes.
        return false;
    }

    /**
     * TODO: Fix @see
     * @see NodeServicePolicies.OnDownloadNodePolicy#onDownloadNode(NodeRef)
     */
    private void invokeOnDownloadNode(NodeRef nodeRef)
    {
        if (ignorePolicy(nodeRef))
        {
            return;
        }

        // get qnames to invoke against
        Set<QName> qnames = getTypeAndAspectQNames(nodeRef);
        // execute policy for node type and aspects
        NodeServicePolicies.OnDownloadNodePolicy policy = onDownloadNodeDelegate.get(nodeRef, qnames);
        policy.onDownloadNode(nodeRef);
    }

    private boolean ignorePolicy(NodeRef nodeRef)
    {
        return (storesToIgnorePolicies.contains(tenantService.getBaseName(nodeRef.getStoreRef()).toString()));
    }

    /**
     * Get all aspect and node type qualified names
     *
     * @param nodeRef the node we are interested in
     * @return Returns a set of qualified names containing the node type and all
     * the node aspects, or null if the node no longer exists
     */
    private Set<QName> getTypeAndAspectQNames(NodeRef nodeRef)
    {
        Set<QName> qnames = null;
        try
        {
            Set<QName> aspectQNames = nodeService.getAspects(nodeRef);

            QName typeQName = nodeService.getType(nodeRef);

            qnames = new HashSet<QName>(aspectQNames.size() + 1);
            qnames.addAll(aspectQNames);
            qnames.add(typeQName);
        } catch (InvalidNodeRefException e)
        {
            qnames = Collections.emptySet();
        }
        // done
        return qnames;
    }


    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    public void setPolicyComponent(PolicyComponent policyComponent)
    {
        this.policyComponent = policyComponent;
    }

    public void setTenantService(TenantService tenantService)
    {
        this.tenantService = tenantService;
    }

    public void setStoresToIgnorePolicies(Set<String> storesToIgnorePolicies)
    {
        this.storesToIgnorePolicies = storesToIgnorePolicies;
    }
}
