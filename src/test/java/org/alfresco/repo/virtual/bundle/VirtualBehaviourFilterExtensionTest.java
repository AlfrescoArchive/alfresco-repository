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

package org.alfresco.repo.virtual.bundle;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.virtual.VirtualizationConfigTestBootstrap;
import org.alfresco.repo.virtual.VirtualizationIntegrationTest;
import org.alfresco.repo.virtual.ref.Reference;
import org.alfresco.repo.virtual.store.VirtualStoreImpl;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.ApplicationContextHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VirtualBehaviourFilterExtensionTest extends VirtualizationIntegrationTest
{


    NodeRef virtualNodeRef;

    BehaviourFilter behaviourFilter;
    VirtualStoreImpl smartStore;


    @Before
    @Override
    public void setUp() throws Exception
    {
        ctx = ApplicationContextHelper.getApplicationContext(CONFIG_LOCATIONS);

        virtualizationConfigTestBootstrap = ctx.getBean(VIRTUALIZATION_CONFIG_TEST_BOOTSTRAP_BEAN_ID,
                VirtualizationConfigTestBootstrap.class);
        virtualizationConfigTestBootstrap.setVirtualFoldersEnabled(true);

        super.setUp();

        behaviourFilter = ctx.getBean("policyBehaviourFilter", BehaviourFilter.class);
        smartStore = ctx.getBean("smartStore", VirtualStoreImpl.class);


        NodeRef nodeRef = nodeService.getChildByName(
                virtualFolder1NodeRef,
                ContentModel.ASSOC_CONTAINS,
                "Node1");

        virtualNodeRef = createContent(
                nodeRef,
                "actualContentName",
                "0",
                MimetypeMap.MIMETYPE_TEXT_PLAIN,
                "UTF-8").getChildRef();

    }

    @After
    @Override
    public void tearDown() throws Exception
    {
        super.tearDown();

    }

    /**
     *
     */
//    @Test
    public void auditableAspectOfActualNodesShouldBeEnableByDefault()
    {
        assertTrue("The auditable aspect for the virtual node must be enable by default",
                behaviourFilter.isEnabled(virtualNodeRef, ContentModel.ASPECT_AUDITABLE));

        NodeRef actualNodeRef = smartStore.materialize(Reference.fromNodeRef(virtualNodeRef));

        assertTrue("The auditable aspect for the actual node must be enable by default",
                behaviourFilter.isEnabled(actualNodeRef, ContentModel.ASPECT_AUDITABLE));

    }

    /**
     *
     */
//    @Test
    public void shouldDisbaleAuditableAspectForTheActualNode()
    {
        assertTrue("The auditable aspect for the virtual node must be enable since it hasn't been disbaled yet",
                behaviourFilter.isEnabled(virtualNodeRef, ContentModel.ASPECT_AUDITABLE));

        NodeRef actualNodeRef = smartStore.materialize(Reference.fromNodeRef(virtualNodeRef));
        assertTrue("The auditable aspect for the actual node must be enable since it hasn't been disbaled yet",
                behaviourFilter.isEnabled(actualNodeRef, ContentModel.ASPECT_AUDITABLE));

        // Disabling the aspect auditable for the virtual node
        behaviourFilter.disableBehaviour(virtualNodeRef, ContentModel.ASPECT_AUDITABLE);

        assertFalse("The auditable aspect for the actual node must not be enable since it has been disbaled",
                behaviourFilter.isEnabled(actualNodeRef, ContentModel.ASPECT_AUDITABLE));



    }

}
