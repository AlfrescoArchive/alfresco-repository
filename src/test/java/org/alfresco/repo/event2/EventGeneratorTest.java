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


import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.BaseSpringTest;
import org.alfresco.util.GUID;
import org.alfresco.util.PropertyMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;


public class EventGeneratorTest extends BaseSpringTest
{
    private static final String TEST_NAMESPACE = "http://www.alfresco.org/test/EventGeneratorTest";

    @Autowired
    private RetryingTransactionHelper retryingTransactionHelper;
    @Autowired
    private NodeService nodeService;

    private NodeRef rootNodeRef;

    @Before
    public void setUp() throws Exception
    {
        // authenticate as admin
        AuthenticationUtil.setAdminUserAsFullyAuthenticatedUser();

        // create a store and get the root node
        StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, this.getClass().getName());
        if (!nodeService.exists(storeRef))
        {
            storeRef = nodeService.createStore(storeRef.getProtocol(), storeRef.getIdentifier());
        }
        this.rootNodeRef = nodeService.getRootNode(storeRef);
    }

    @After
    public void tearDown() throws Exception
    {
        try
        {
            AuthenticationUtil.clearCurrentSecurityContext();
        }
        catch (Throwable e)
        {
            // ignore
        }
    }

    @Test
    public void createNodeEvent()
    {
        retryingTransactionHelper.doInTransaction(() -> {
            // create a content node
            ContentData contentData = new ContentData(null, "text/plain", 0L, "UTF-8");

            PropertyMap properties = new PropertyMap();
            properties.put(ContentModel.PROP_CONTENT, contentData);
            properties.put(ContentModel.PROP_NAME, "test" + System.currentTimeMillis() + ".txt");
            properties.put(ContentModel.PROP_TITLE, "test title");

            // This causes a 'org.alfresco.event.node.Created' event
            ChildAssociationRef assocRef = nodeService.createNode(rootNodeRef,
                                                                  ContentModel.ASSOC_CHILDREN,
                                                                  QName.createQName(TEST_NAMESPACE, GUID.generate()),
                                                                  ContentModel.TYPE_CONTENT, properties);
            //TODO
            // do something with the nodeRef.
            // E.g. check the generated event's resource id is indeed equivalent to this node
            NodeRef contentNodeRef = assocRef.getChildRef();

            return null;
        });
    }
}
