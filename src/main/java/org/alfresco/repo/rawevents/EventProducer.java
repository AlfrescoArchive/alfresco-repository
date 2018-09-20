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
package org.alfresco.repo.rawevents;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.util.GUID;
import org.alfresco.util.transaction.TransactionListenerAdapter;
import org.apache.activemq.transport.amqp.message.AmqpMessageSupport;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A client uses an <code>EventProducer</code> to send events to an endpoint.
 * The <code>EventProducer</code> acts as a wrapper that provides marshalling
 * for a Camel <code>ProducerTemplate</code>. <br/>
 * <p/>
 * A client has the option of creating an event producer without supplying an
 * endpoint. In this case, a endpoint must be provided with every send
 * operation. <br/>
 * <p/>
 * A client also has the option to provide an <code>ObjectMapper</code> that
 * will be used to marshal basic POJOs (Plain Old Java Objects) to JSON before
 * sending the event.
 * <p/>
 */
public class EventProducer
{
    private static final String POST_TRANSACTION_PENDING_REQUESTS = "postTransactionPendingEventRequests";
    protected static final String ERROR_SENDING = "Could not send event";

    private static Log logger = LogFactory.getLog(EventProducer.class);

    private TransactionListener transactionListener = new TransactionListener();
    protected ProducerTemplate producer;
    protected String endpoint;
    protected ObjectMapper objectMapper;

    public void setProducer(ProducerTemplate producer)
    {
        this.producer = producer;
    }

    public void setEndpoint(String endpoint)
    {
        this.endpoint = endpoint;
    }

    public void setObjectMapper(ObjectMapper objectMapper)
    {
        this.objectMapper = objectMapper;
    }

    private Map<String, Object> addHeaders(Map<String, Object> origHeaders)
    {
        if (origHeaders == null)
        {
            origHeaders = new HashMap<>();
        }

        origHeaders.put(AmqpMessageSupport.JMS_AMQP_MESSAGE_FORMAT, AmqpMessageSupport.AMQP_UNKNOWN);
        return origHeaders;
    }

    public void send(String endpointUri, Object event)
    {
        send(endpointUri, event, null);
    }

    public void send(String endpointUri, Object event, Map<String, Object> headers)
    {
        try
        {
            if (StringUtils.isEmpty(endpointUri))
            {
                endpointUri = this.endpoint;
            }

            if (this.objectMapper != null && !(event instanceof String))
            {
                event = this.objectMapper.writeValueAsString(event);
            }

            this.producer.sendBodyAndHeaders(endpointUri, event, this.addHeaders(headers));
        }
        catch (Exception e)
        {
            throw new AlfrescoRuntimeException(ERROR_SENDING, e);
        }
    }

    public void sendAfterCommit(String endpointUri, Object event)
    {
        sendAfterCommit(endpointUri, event, null);
    }

    public void sendAfterCommit(String endpointUri, Object event, Map<String, Object> headers)
    {
        AlfrescoTransactionSupport.bindListener(transactionListener);
        List<PendingRequest> pendingRequests = AlfrescoTransactionSupport.getResource(POST_TRANSACTION_PENDING_REQUESTS);

        if (pendingRequests == null)
        {
            pendingRequests = new LinkedList<>();
            AlfrescoTransactionSupport.bindResource(POST_TRANSACTION_PENDING_REQUESTS, pendingRequests);
        }

        PendingRequest pendingRequest = new PendingRequest(endpointUri, event, headers);
        pendingRequests.add(pendingRequest);
    }

    private class PendingRequest
    {
        private String endpointUri;
        private Object event;
        private Map<String, Object> headers;

        private PendingRequest(String endpointUri, Object event, Map<String, Object> headers)
        {
            this.endpointUri = endpointUri;
            this.event = event;
            this.headers = headers;
        }

        void send()
        {
            try
            {
                EventProducer.this.send(endpointUri, event, headers);
            }
            catch (Exception e)
            {
                logger.debug(e.getMessage());
                // consume exception
            }
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(o instanceof EventProducer.PendingRequest))
            {
                return false;
            }
            EventProducer.PendingRequest that = (EventProducer.PendingRequest) o;
            return Objects.equals(endpointUri, that.endpointUri) && Objects.equals(event, that.event) && Objects.equals(headers, that.headers);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(endpointUri, event, headers);
        }
    }

    private class TransactionListener extends TransactionListenerAdapter implements org.alfresco.repo.transaction.TransactionListener
    {
        private final String id = GUID.generate();

        @Override
        public void afterCommit()
        {
            for (EventProducer.PendingRequest pendingRequest : (List<PendingRequest>) AlfrescoTransactionSupport.getResource(POST_TRANSACTION_PENDING_REQUESTS))
            {
                pendingRequest.send();
            }
        }

        @Override
        public void flush()
        {
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(o instanceof EventProducer.TransactionListener))
            {
                return false;
            }
            EventProducer.TransactionListener that = (EventProducer.TransactionListener) o;
            return Objects.equals(id, that.id);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(id);
        }
    }
}
