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

import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.extensions.surf.util.URLDecoder;
import org.springframework.extensions.surf.util.URLEncoder;

/**
 * Creates Alfresco client data. This will be echoed back to the client of a async rendition or transform request via
 * a message queue and will contain all the information needed to keep everything stateless.<p>
 *
 * This class was originally in the alfresco-enterprise-repository project, but is now in alfresco-repository as it is
 * also being used in the response to async transform requests.
 *
 * @author eknizat
 */
public class ClientData
{
    private static final String TRANSFORM_CLIENT_DATA_DELIMITER = " ";

    private final NodeRef sourceNodeRef;
    private final String renditionName;
    private final int transformContentUrlHash;
    private final String user;
    private final String userData;
    private final String replyQueue;
    private final long requested;
    private final int seq;
    private final String sourceExt;
    private final String targetExt;

    public ClientData(NodeRef sourceNodeRef, String renditionName, int transformContentUrlHash,
                      String user, String userData, String replyQueue,
                      long requested, int seq, String sourceExt, String targetExt)
    {
        this.sourceNodeRef = sourceNodeRef;
        this.renditionName = renditionName;
        this.transformContentUrlHash = transformContentUrlHash;
        this.user = user;
        this.userData = userData;
        this.replyQueue = replyQueue;
        this.requested = requested;
        this.seq = seq;
        this.sourceExt = sourceExt;
        this.targetExt = targetExt;
    }

    public ClientData(String clientData)
    {
        String[] clientDataTs  = clientData.split(TRANSFORM_CLIENT_DATA_DELIMITER);
        if (clientDataTs.length != 10)
        {
            throw new IllegalArgumentException("The clientData string has to be composed of 10 tokens " +
                    "delimited by '"+TRANSFORM_CLIENT_DATA_DELIMITER+"'");
        }
        String nodeRefStr = URLDecoder.decode(clientDataTs[0]);
        if (!NodeRef.isNodeRef(nodeRefStr)){
            throw new IllegalArgumentException("The 1st token in the clientData string has to be a valid NodeRef.");
        }
        sourceNodeRef = new NodeRef(nodeRefStr);

        renditionName = URLDecoder.decode(clientDataTs[1]);

        try
        {
            transformContentUrlHash = Integer.parseInt(clientDataTs[2]);
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException("The 3rd token in the clientData string has to be an integer.");
        }

        user = URLDecoder.decode(clientDataTs[3]);

        userData = URLDecoder.decode(clientDataTs[4]);

        replyQueue = URLDecoder.decode(clientDataTs[5]);

        try
        {
            requested = Long.parseLong(clientDataTs[6]);
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException("The 7th token in the clientData string has to be an long.");
        }
        try
        {
            seq = Integer.parseInt(clientDataTs[7]);
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException("The 8th token in the clientData string has to be an integer.");
        }

        sourceExt = URLDecoder.decode(clientDataTs[8]);

        targetExt = URLDecoder.decode(clientDataTs[9]);
    }

    public NodeRef getSourceNodeRef()
    {
        return sourceNodeRef;
    }

    public String getRenditionName()
    {
        return renditionName;
    }

    public int getTransformContentUrlHash()
    {
        return transformContentUrlHash;
    }

    public String getUser()
    {
        return user;
    }

    public String getUserData()
    {
        return userData;
    }

    public String getReplyQueue()
    {
        return replyQueue;
    }

    public long getRequested()
    {
        return requested;
    }

    public int getSeq()
    {
        return seq;
    }

    public String getSourceExt()
    {
        return sourceExt;
    }

    public String getTargetExt()
    {
        return targetExt;
    }

    @Override
    public String toString()
    {
        return  String.join(TRANSFORM_CLIENT_DATA_DELIMITER,
                URLEncoder.encode(sourceNodeRef.toString()),
                URLEncoder.encode(renditionName),
                Integer.toString(transformContentUrlHash),
                URLEncoder.encode(user),
                URLEncoder.encode(userData),
                URLEncoder.encode(replyQueue),
                Long.toString(requested),
                Integer.toString(seq),
                URLEncoder.encode(sourceExt),
                URLEncoder.encode(targetExt));
    }
}
