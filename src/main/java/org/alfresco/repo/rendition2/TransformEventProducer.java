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

import org.alfresco.repo.rawevents.TransactionAwareEventProducer;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.transform.client.model.TransformReply;

import java.io.InputStream;

/**
 * Used to send transform response messages to remote transform clients.
 * The response is identical to that produced by the Alfresco Transform Service (ATS).<p>
 *
 * This class uses {@link TransactionAwareEventProducer} for sending messages.
 */
public class TransformEventProducer
{
    /**
     *  Takes a transformation (InputStream) and sends it to the transform reply queue.
     *  If the transformInputStream is null, this is taken to be a transform failure.
     */
    void produceTransformEvent(NodeRef sourceNodeRef, InputStream transformInputStream,
                               TransformDefinition transformDefinition, int transformContentHashCode)
    {
        // TODO REPO-4700
        //  - This is just code that pulls values that will be needed
        //  - Move this class's code into its own class like we do for transform service queue request and response.
        //  - doing

        String transformName = transformDefinition.getTransformName();
        String replyQueue = transformDefinition.getReplyQueue();
        String userData = transformDefinition.getUserData();
        String targetMimetype = transformDefinition.getTargetMimetype();
        boolean success = transformInputStream != null;

//        ContentData contentData = org.alfresco.enterprise.repo.rendition2.RenditionEventProducer.getContentData(sourceNodeRef);
//        String sourceMimetype = contentData.getMimetype();
        String sourceExt = "TODO"; // get from sourceNodeRef.  Do we even need it?
        String targetExt = "TODO"; // get from targetMimetype. Do we even need it?
        String user = "-"; // user = AuthenticationUtil.getRunAsUser(); // Use a dummy value, so we don't expose internals?
        long requested = 0; // probably a dummy value
        int seq = 0; // Increment if retry on transaction.

        ClientData clientData = new ClientData(sourceNodeRef, transformName, transformContentHashCode,
                user, userData, replyQueue, requested, seq, sourceExt, targetExt);

        TransformReply transformReply = TransformReply.builder().
                withClientData(clientData.toString()).
                // TODO add more components of the reply.
                build();
    }
}
