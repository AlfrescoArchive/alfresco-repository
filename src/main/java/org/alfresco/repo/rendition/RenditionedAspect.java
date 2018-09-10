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
package org.alfresco.repo.rendition;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.alfresco.model.RenditionModel;
import org.alfresco.repo.copy.CopyBehaviourCallback;
import org.alfresco.repo.copy.CopyDetails;
import org.alfresco.repo.copy.CopyServicePolicies;
import org.alfresco.repo.copy.DefaultCopyBehaviourCallback;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.Behaviour;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.rendition.executer.AbstractRenderingEngine;
import org.alfresco.repo.rendition.executer.DeleteRenditionActionExecuter;
import org.alfresco.repo.rendition2.RenditionDefinition2;
import org.alfresco.repo.rendition2.RenditionDefinitionRegistry2;
import org.alfresco.repo.rendition2.RenditionService2;
import org.alfresco.repo.rendition2.RenditionService2Impl;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.rendition.RenderCallback;
import org.alfresco.service.cmr.rendition.RenditionDefinition;
import org.alfresco.service.cmr.rendition.RenditionService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.EqualsHelper;
import org.alfresco.util.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Renditioned aspect behaviour bean.
 * When any node with the renditioned aspect has a property updated, then all
 * associated renditions are eligible for re-rendering.
 * Each rendition (as identified by the name in its rn:rendition association) will
 * be loaded and if the renditionDefinition exists, the rendition will be updated
 * asynchronously, subject to the defined update policy.
 * 
 * @author Neil McErlean
 * @author Roy Wetherall
 */
public class RenditionedAspect implements NodeServicePolicies.OnUpdatePropertiesPolicy,
                                          CopyServicePolicies.OnCopyNodePolicy
{
    /** logger */
    private static final Log logger = LogFactory.getLog(RenditionedAspect.class);

    /** Services */
    private ActionService actionService;
    private DictionaryService dictionaryService;
    private NodeService nodeService;
    private PolicyComponent policyComponent;
    private RenditionService renditionService;
    private RenditionService2 renditionService2;

    /**
     * Set the policy component
     * 
     * @param policyComponent   policy component
     */
    public void setPolicyComponent(PolicyComponent policyComponent)
    {
        this.policyComponent = policyComponent;
    }
    
    /**
     * @since 3.4.2
     */
    public void setActionService(ActionService actionService) 
    {
        this.actionService = actionService;
    }

    /**
     * Set the node service
     * 
     * @param nodeService   node service
     */
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }
    
    /**
     * Set the rendition service
     * 
     * @param renditionService   rendition service
     */
    public void setRenditionService(RenditionService renditionService)
    {
        this.renditionService = renditionService;
    }

    public void setRenditionService2(RenditionService2 renditionService2)
    {
        this.renditionService2 = renditionService2;
    }

    /**
     * Set the dictionary service
     * 
     * @param dictionaryService     dictionary service
     */
    public void setDictionaryService(DictionaryService dictionaryService)
    {
        this.dictionaryService = dictionaryService;
    }
    
    /**
     * Initialise method
     */
    public void init()
    {
        this.policyComponent.bindClassBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "onUpdateProperties"), 
                RenditionModel.ASPECT_RENDITIONED, 
                new JavaBehaviour(this, "onUpdateProperties", Behaviour.NotificationFrequency.EVERY_EVENT));
        this.policyComponent.bindClassBehaviour(
                QName.createQName(NamespaceService.ALFRESCO_URI, "getCopyCallback"), 
                RenditionModel.ASPECT_RENDITIONED, 
                new JavaBehaviour(this, "getCopyCallback"));
    }

    /**
     * @see org.alfresco.repo.node.NodeServicePolicies.OnUpdatePropertiesPolicy#onUpdateProperties(org.alfresco.service.cmr.repository.NodeRef, java.util.Map, java.util.Map)
     */
    public void onUpdateProperties(
            NodeRef nodeRef,
            Map<QName, Serializable> before,
            Map<QName, Serializable> after)
    {
        if (this.nodeService.exists(nodeRef))
        {
            // Find the changed properties
            List<QName> changedProperties = getChangedProperties(before, after);
            
            // There may be a different policy for different rendition kinds.
            List<ChildAssociationRef> renditions = this.renditionService.getRenditions(nodeRef);
            for (ChildAssociationRef chAssRef : renditions)
            {
                final QName renditionAssocName = chAssRef.getQName();
                
                // Rendition Definitions are persisted underneath the Data Dictionary for which Group ALL
                // has Consumer access by default. However, we cannot assume that that access level applies for all deployments. See ALF-7334.
                RenditionDefinition rendDefn = AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<RenditionDefinition>()
                    {
                        @Override
                        public RenditionDefinition doWork() throws Exception
                        {
                            return renditionService.loadRenditionDefinition(renditionAssocName);
                        }
                    }, AuthenticationUtil.getSystemUserName());
                
                if (rendDefn == null)
                {
                    if (logger.isDebugEnabled())
                    {
                        StringBuilder msg = new StringBuilder();
                        msg.append("Cannot update rendition ")
                            .append(renditionAssocName)
                            .append(" on node ").append(nodeRef)
                            .append(" as the renditionDefinition could not be loaded.");
                        logger.debug(msg.toString());
                    }
                    continue;
                }
                Serializable updateRenditionsPolicy = rendDefn.getParameterValue(AbstractRenderingEngine.PARAM_UPDATE_RENDITIONS_ON_ANY_PROPERTY_CHANGE);
                boolean updateRenditionsAlways = updateRenditionsPolicy == null ? false : (Boolean)updateRenditionsPolicy;
            
                boolean renditionUpdateRequired = false;
                
                for (QName qname : changedProperties)
                {
                    try
                    {
                        PropertyDefinition propertyDef = dictionaryService.getProperty(qname);
                        if (propertyDef == null)
                        {
                            // the property is not recognised
                            continue;
                        }
                        else if (!propertyDef.getDataType().getName().equals(DataTypeDefinition.CONTENT))
                        {
                            // not a content type
                            if (updateRenditionsAlways)
                            {
                                renditionUpdateRequired = true;
                            }
                            continue;
                        }
                        else
                        {
                            // it is a content property. We always update renditions for changes to content.
                            renditionUpdateRequired = true;
                        }
                    } catch (ClassCastException ccx)
                    {
                        // the property does not confirm to the model
                        continue;
                    }
                }
                
                if (renditionUpdateRequired)
                {
                    this.queueUpdate(nodeRef, rendDefn, chAssRef);
                }
            }
        }
    }
    
    private List<QName> getChangedProperties(Map<QName, Serializable> before, Map<QName, Serializable> after)
    {
        List<QName> results = new ArrayList<QName>();
        for (QName propQName : before.keySet())
        {
            if (after.keySet().contains(propQName) == false)
            {
                // property was deleted
                results.add(propQName);
            }
            else
            {
                Serializable beforeValue = before.get(propQName);
                Serializable afterValue = after.get(propQName);
                if (EqualsHelper.nullSafeEquals(beforeValue, afterValue) == false)
                {
                    // Property was changed
                    results.add(propQName);
                }
            }
        }
        for (QName propQName : after.keySet())
        {
            if (before.containsKey(propQName) == false)
            {
                // property was added
                results.add(propQName);
            }
        }
        
        return results;
    }

    /**
     * Queue the update to happen asynchronously
     * 
     * @param sourceNodeRef           node reference
     */
    private void queueUpdate(final NodeRef sourceNodeRef, final RenditionDefinition rendDefn,
            final ChildAssociationRef renditionAssoc)
    {
        if (logger.isDebugEnabled() && rendDefn != null)
        {
            StringBuilder msg = new StringBuilder();
            msg.append("Queueing rendition update for node ").append(sourceNodeRef).append(": ").append(rendDefn.getRenditionName());
            logger.debug(msg.toString());
        }

        if (rendDefn != null && !useRenditionService2(sourceNodeRef, rendDefn))
        {
            Action deleteRendition = actionService.createAction(DeleteRenditionActionExecuter.NAME);
            deleteRendition.setParameterValue(DeleteRenditionActionExecuter.PARAM_RENDITION_DEFINITION_NAME, rendDefn.getRenditionName());
            rendDefn.setCompensatingAction(deleteRendition);
            
            renditionService.render(sourceNodeRef, rendDefn, new RenderCallback()
            {
                public void handleFailedRendition(Throwable t)
                {
                    // In the event of a failed re-rendition, we will delete the rendition node
                    if (logger.isDebugEnabled())
                    {
                        StringBuilder msg = new StringBuilder();
                        msg.append("Re-rendering of node ")
                            .append(sourceNodeRef)
                            .append(" with renditionDefinition ")
                            .append(rendDefn.getRenditionName())
                            .append(" failed. Deleting defunct rendition. ")
                            .append("The following exception is shown for informational purposes only ")
                            .append("and does not affect operation of the system.");
                        logger.debug(msg.toString(), t);
                    }
                }

                public void handleSuccessfulRendition(ChildAssociationRef primaryParentOfNewRendition)
                {
                    // Intentionally empty
                }
            });
        }
    }

    /**
     * If a rendition has been created by the newer RenditionService2, this method will return true. In this case the
     * original RenditionService should not be used to update an existing rendition if the content changes. These
     * renditions can be identified by the existence of the rendition2 aspect. RenditionService2 also listens for
     * content changes and will perform the rendition.</p>
     *
     * If RenditionService2 has a definition with the same name (matched on the local part of the supplied rendDef's
     * name) and the target mimetype matches, RenditionService2 will also be used. This is being done to help with the
     * converion of custom transformers and renditions. The basic definitions (called "medium", "doclib",
     * "imgpreview", "avatar", "avatar32", "webpreview", "pdf") already exist. Rather than writing  converter form one
     * definition to the newer one, it is suggested that a newer RenditionDefinition2 is simply defined to avoid having
     * to worry about any complexities in converting TransformationOptions into a simple flat Map structure.
     */
    private boolean useRenditionService2(NodeRef sourceNodeRef, RenditionDefinition rendDefn)
    {
        boolean useRenditionService2 = false;

        QName renditionQName = rendDefn.getRenditionName();
        String renditionName = renditionQName.getLocalName();
        RenditionDefinitionRegistry2 renditionDefinitionRegistry2 = renditionService2.getRenditionDefinitionRegistry2();
        RenditionDefinition2 renditionDefinition2 = renditionDefinitionRegistry2.getRenditionDefinition(renditionName);
        if (renditionDefinition2 != null)
        {
            String targetMimetype = (String) rendDefn.getParameterValue(AbstractRenderingEngine.PARAM_MIME_TYPE);
            String targetMimetype2 = renditionDefinition2.getTargetMimetype();
            if (targetMimetype.equals(targetMimetype2))
            {
                useRenditionService2 = true;
            }
        }

        if (!useRenditionService2 && ((RenditionService2Impl)renditionService2).useRenditionService2(sourceNodeRef, renditionName))
        {
            useRenditionService2 = true;
        }

        // TODO remove this call once RenditionService2 is listening to its own events.
        if (useRenditionService2)
        {
            renditionService2.render(sourceNodeRef, renditionName);
        }

        return useRenditionService2;
    }

    /**
     * @return              Returns CopyBehaviourCallback
     */
    public CopyBehaviourCallback getCopyCallback(QName classRef, CopyDetails copyDetails)
    {
        return RenditionedAspectCopyBehaviourCallback.INSTANCE;
    }

    /**
     * Behaviour for the {@link RenditionModel#ASPECT_RENDITIONED <b>rn:renditioned</b>} aspect.
     * 
     * @author Derek Hulley
     * @author Neil McErlean
     * @since 3.2
     */
    private static class RenditionedAspectCopyBehaviourCallback extends DefaultCopyBehaviourCallback
    {
        private static final CopyBehaviourCallback INSTANCE = new RenditionedAspectCopyBehaviourCallback();
        
        
        
        /**
         * We do not copy the {@link RenditionModel#ASPECT_RENDITIONED rn:renditioned} aspect.
         */
        @Override
        public boolean getMustCopy(QName classQName, CopyDetails copyDetails)
        {
            return false;
        }
        
        @Override
        public Pair<AssocCopySourceAction, AssocCopyTargetAction> getAssociationCopyAction(
                QName classQName,
                CopyDetails copyDetails,
                CopyAssociationDetails assocCopyDetails)
        {
            return new Pair<AssocCopySourceAction, AssocCopyTargetAction>(
                    AssocCopySourceAction.IGNORE,
                    AssocCopyTargetAction.USE_COPIED_OTHERWISE_ORIGINAL_TARGET);
        }
        
        @Override
        public ChildAssocCopyAction getChildAssociationCopyAction(
                QName classQName,
                CopyDetails copyDetails,
                CopyChildAssociationDetails childAssocCopyDetails)
        {
            return ChildAssocCopyAction.IGNORE;            
        }
    }
}
