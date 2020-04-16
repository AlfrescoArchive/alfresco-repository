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

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import javax.jms.ConnectionFactory;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.event.databind.ObjectMapperFactory;
import org.alfresco.repo.event.v1.model.NodeResource;
import org.alfresco.repo.event.v1.model.RepoEvent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Iulian Aftene
 */

public abstract class AbstractContextAwareRepoEvent extends BaseSpringTest
{
    private static final   String       TEST_NAMESPACE       = "http://www.alfresco.org/test/ContextAwareRepoEvent";
    private static final   String       CAMEL_BASE_TOPIC_URI = "jms:topic:";
    private static final   String       BROKER_URL           = "tcp://localhost:61616";
    private static final   String       TOPIC                = "alfresco.repo.event2";
    protected static final ObjectMapper OBJECT_MAPPER        = ObjectMapperFactory.createInstance();

    protected CompletableFuture<String> futureResult = new CompletableFuture<>();
    protected NodeRef rootNodeRef;

    @Autowired
    protected RetryingTransactionHelper retryingTransactionHelper;

    @Autowired
    protected NodeService nodeService;

    @Before
    public void setUp() throws Exception
    {
        // authenticate as admin
        AuthenticationUtil.setAdminUserAsFullyAuthenticatedUser();

        this.rootNodeRef = retryingTransactionHelper.doInTransaction(() -> {
            // create a store and get the root node
            StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE,
                this.getClass().getName());
            if (!nodeService.exists(storeRef))
            {
                storeRef = nodeService.createStore(storeRef.getProtocol(),
                    storeRef.getIdentifier());
            }
            return nodeService.getRootNode(storeRef);
        });
    }

    protected NodeRef createNode(QName contentType)
    {
        return retryingTransactionHelper.doInTransaction(() -> nodeService.createNode(
            rootNodeRef,
            ContentModel.ASSOC_CHILDREN,
            QName.createQName(TEST_NAMESPACE, GUID.generate()),
            contentType).getChildRef());
    }

    protected NodeRef createNode(QName contentType, NodeRef parentRef)
    {
        return retryingTransactionHelper.doInTransaction(() -> nodeService.createNode(
            parentRef,
            ContentModel.ASSOC_CHILDREN,
            QName.createQName(TEST_NAMESPACE, GUID.generate()),
            contentType).getChildRef());
    }

    protected NodeRef createNode(QName contentType, PropertyMap propertyMap)
    {
        return retryingTransactionHelper.doInTransaction(() -> nodeService.createNode(
            rootNodeRef,
            ContentModel.ASSOC_CHILDREN,
            QName.createQName(TEST_NAMESPACE, GUID.generate()),
            contentType,
            propertyMap).getChildRef());
    }

    protected void deleteNode(NodeRef nodeRef)
    {
        retryingTransactionHelper.doInTransaction(() -> {
            nodeService.deleteNode(nodeRef);
            return null;
        });
    }

    protected <T> CamelContext subscribe(Consumer<T> handler, Class<T> type) throws Exception
    {
        final CamelContext ctx = new DefaultCamelContext();
        final ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(BROKER_URL);
        ctx.addComponent("jms", JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));

        ctx.addRoutes(new RouteBuilder()
        {
            @Override
            public void configure()
            {
                from(CAMEL_BASE_TOPIC_URI + TOPIC)
                    .process(exchange -> {
                        if (exchange.getMessage().getBody() != null)
                        {
                            handler.accept(exchange.getMessage().getBody(type));
                        }
                    });
            }
        });

        ctx.start();
        return ctx;
    }

    protected RepoEvent<NodeResource> getFutureResult() throws Exception
    {
        return OBJECT_MAPPER.readValue(futureResult.get(5, SECONDS),
            new TypeReference<RepoEvent<NodeResource>>()
            {
            });
    }
}


