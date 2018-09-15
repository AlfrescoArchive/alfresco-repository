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

import org.alfresco.service.NotAuditable;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;

import java.util.List;

/**
 * The Async Rendition service. Replaces the original rendition services which included synchronous renditions and
 * asynchronous methods with call backs.<p/>
 *
 * Renditions are defined as {@link RenditionDefinition2}s and may be registered and looked by the associated
 * {@link RenditionDefinitionRegistry2}.
 *
 * @author adavis
 */
public interface RenditionService2
{
    /**
     * @return the {@link RenditionDefinitionRegistry2} being used by the service.
     */
    RenditionDefinitionRegistry2 getRenditionDefinitionRegistry2();

    /**
     * This method asynchronously renders content as specified by the {@code renditionName}. The content to be
     * rendered is provided by {@code sourceNodeRef}.
     *
     * @param sourceNodeRef the node from which the content is retrieved.
     * @param renditionName the rendition to be performed.
     */
    @NotAuditable
    public void render(NodeRef sourceNodeRef, String renditionName);

    /**
     * This method gets all the renditions of the {@code sourceNodeRef}.
     *
     * @return a list of {@link ChildAssociationRef}s which link the {@code sourceNodeRef} to the renditions.
     */
    @NotAuditable
    List<ChildAssociationRef> getRenditions(NodeRef sourceNodeRef);

    /**
     * This method gets the rendition of the {@code sourceNodeRef} identified by its name.
     *
     * @param sourceNodeRef the source node for the renditions
     * @param renditionName the renditionName used to identify a rendition.
     * @return the {@link ChildAssociationRef} which links the source node to the
     *         rendition or <code>null</code> if there is no such rendition.
     */
    @NotAuditable
    ChildAssociationRef getRenditionByName(NodeRef sourceNodeRef, String renditionName);

    /**
     * Indicates if renditions are enabled. Set using the {@code system.thumbnail.generate} value.
     */
    boolean isEnabled();
}