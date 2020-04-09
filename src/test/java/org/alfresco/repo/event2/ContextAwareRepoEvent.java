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
import java.util.function.Consumer;
import javax.jms.ConnectionFactory;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.BaseSpringTest;
import org.alfresco.util.GUID;
import org.alfresco.util.PropertyMap;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Iulian Aftene
 */

public class ContextAwareRepoEvent extends BaseSpringTest
{
    private static final String TEST_NAMESPACE = "http://www.alfresco.org/test/ContextAwareRepoEvent";
    private static final String CAMEL_BASE_TOPIC_URI = "jms:topic:";
    private static final String BROKER_URL = "tcp://localhost:61616";
    protected static final String TOPIC = "alfresco.repo.event2";

    @Autowired
    protected RetryingTransactionHelper retryingTransactionHelper;
    @Autowired
    protected NodeService nodeService;

    protected NodeRef rootNodeRef;

    @Before
    public void setUp() throws Exception
    {
        // authenticate as admin
        AuthenticationUtil.setAdminUserAsFullyAuthenticatedUser();

        this.rootNodeRef = retryingTransactionHelper.doInTransaction(() -> {
            // create a store and get the root node
            StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, this.getClass().getName());
            if (!nodeService.exists(storeRef))
            {
                storeRef = nodeService.createStore(storeRef.getProtocol(), storeRef.getIdentifier());
            }
            return nodeService.getRootNode(storeRef);
        });
    }

    public NodeRef createNode(QName contentType)
    {
        final ChildAssociationRef[] assocRef = new ChildAssociationRef[1];
        retryingTransactionHelper.doInTransaction(() -> {
            assocRef[0] = nodeService.createNode(
                rootNodeRef,
                ContentModel.ASSOC_CHILDREN,
                QName.createQName(TEST_NAMESPACE, GUID.generate()),
                contentType);
            return null;
        });
        return assocRef[0].getChildRef();
    }

    public NodeRef createNode(QName contentType, NodeRef parentRef)
    {
        final ChildAssociationRef[] assocRef = new ChildAssociationRef[1];
        retryingTransactionHelper.doInTransaction(() -> {
            assocRef[0] = nodeService.createNode(
                parentRef,
                ContentModel.ASSOC_CHILDREN,
                QName.createQName(TEST_NAMESPACE, GUID.generate()),
                contentType);
            return null;
        });
        NodeRef contentNodeRef = assocRef[0].getChildRef();
        return contentNodeRef;
    }

    public NodeRef createNode(QName contentType, PropertyMap propertyMap)
    {
        final ChildAssociationRef[] assocRef = new ChildAssociationRef[1];
        retryingTransactionHelper.doInTransaction(() -> {
             assocRef[0] = nodeService.createNode(rootNodeRef,
                ContentModel.ASSOC_CHILDREN,
                QName.createQName(TEST_NAMESPACE, GUID.generate()),
                 contentType,
                 propertyMap);
             return null;
        });
        NodeRef contentNodeRef = assocRef[0].getChildRef();
        return contentNodeRef;
    }


    public void deleteNode(NodeRef nodeRef)
    {
        retryingTransactionHelper.doInTransaction(() -> {
            nodeService.deleteNode(nodeRef);
            return null;
        });
    }

    public  <T> CamelContext subscribe( Consumer<T> handler, Class<T> type) throws Exception
    {
        final CamelContext ctx = new DefaultCamelContext();
        final ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(BROKER_URL);
        ctx.addComponent("jms", JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));

        ctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                from(CAMEL_BASE_TOPIC_URI + TOPIC)
                    .process(exchange -> {
                        if ( exchange.getMessage().getBody() != null )
                        {
                        handler.accept(exchange.getMessage().getBody(type));
                        }
                    });
            }
        });

        ctx.start();
        return ctx;
    }
}


