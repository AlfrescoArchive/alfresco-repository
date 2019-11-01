/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2019 Alfresco Software Limited
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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.ParameterCheck;
import org.apache.camel.Exchange;
import org.apache.camel.NoSuchEndpointException;
import org.apache.camel.Processor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.Map;

/**
 * Transform Request processor executes transformation based on TransformRequest event.
 *
 * @author aepure
 */
public class TransformRequestProcessor implements Processor
{
    private static Log logger = LogFactory.getLog(RenditionEventProcessor.class);

    private RenditionService2Impl renditionService2;
    private ObjectMapper messagingObjectMapper;
    private TransactionService transactionService;

    public void setRenditionService2(RenditionService2Impl renditionService2)
    {
        this.renditionService2 = renditionService2;
    }

    public void setMessagingObjectMapper(ObjectMapper messagingObjectMapper)
    {
        this.messagingObjectMapper = messagingObjectMapper;
    }

    public void setTransactionService(TransactionService transactionService)
    {
        this.transactionService = transactionService;
    }

    @Override public void process(Exchange exchange) throws Exception
    {
        String body = (String) exchange.getMessage().getBody();

        if (logger.isDebugEnabled())
        {
            logger.info("Processing message [thread=" + Thread.currentThread().getId() + ", body=" + body + "]");
        }
        if (body == null || body.isEmpty())
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Exchange message is null or empty");
            }
            return;
        }
        try
        {
            TransformRequest event;
            try
            {
                event = messagingObjectMapper.readValue(body, TransformRequest.class);
            }
            catch (IOException e)
            {
                logger.error("Failed to unmarshal event [" + body + "]", e);
                throw new AlfrescoRuntimeException("Failed to unmarshal event, skipping processing of this event.\"");
            }
            processEvent(event);
        }
        catch (Exception e)
        {
            logger.error("Failed to process transform request event: " + body, e);
        }
    }

    private void validateEvent(TransformRequest event)
    {
        ParameterCheck.mandatory("event", event);
        ParameterCheck.mandatoryString("requestId", event.getRequestId());
        ParameterCheck.mandatoryString("nodeRef", event.getNodeRef());
        ParameterCheck.mandatoryString("targetMediaType", event.getTargetMediaType());

        ParameterCheck.mandatoryString("replyQueue", event.getReplyQueue());
        if (!event.getReplyQueue().startsWith("jms:"))
        {
            throw new NoSuchEndpointException(event.getReplyQueue(), "ensure that protocol is specified for the return queue.");
        }
    }

    private void processEvent(TransformRequest event)
    {
        validateEvent(event);

        TransformDefinition eventDefinition = createTransformDefinition(event.getTransformName(), event.getTargetMediaType(), event.getTransformOptions(), event.getClientData(), event.getReplyQueue());

        AuthenticationUtil.runAs((AuthenticationUtil.RunAsWork<Void>) () -> transactionService.getRetryingTransactionHelper().doInTransaction(() -> {

            renditionService2.transform(new NodeRef(event.getNodeRef()), eventDefinition);

            return null;
        }), AuthenticationUtil.getSystemUserName());
    }

    TransformDefinition createTransformDefinition(String transformName, String targetMimetype, Map<String, String> transformOptions, String clientData, String replyQueue)
    {
        return new TransformDefinition(transformName, targetMimetype, transformOptions, clientData, replyQueue);
    }
}
