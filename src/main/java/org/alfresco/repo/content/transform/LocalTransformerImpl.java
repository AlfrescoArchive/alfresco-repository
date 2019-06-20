/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2019 Alfresco Software Limited
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
package org.alfresco.repo.content.transform;

import org.alfresco.repo.rendition2.RenditionDefinition2;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.Pair;

import java.util.Map;
import java.util.Set;

/**
 * A local transformer using flat transform options.
 *
 * Instances are automatically created for transformers identified by alfresco/transform json files and returned from
 * T-Engines which are themselves identified by global properties or system properties the match the pattern
 * localTransformer.&lt;name>.url. The transforms take place in a separate process (typically a Docker container).
 */
public class LocalTransformerImpl extends AbstractLocalTransformer
{
    private RemoteTransformerClient remoteTransformerClient;

    private boolean available = false;

    public LocalTransformerImpl(String name, TransformerDebug transformerDebug,
                                MimetypeService mimetypeService, boolean strictMimeTypeCheck,
                                Map<String, Set<String>> strictMimetypeExceptions,
                                boolean retryTransformOnDifferentMimeType,
                                LocalTransformServiceRegistry localTransformServiceRegistry, String baseUrl,
                                int startupRetryPeriodSeconds)
    {
        super(name, transformerDebug, mimetypeService, strictMimeTypeCheck, strictMimetypeExceptions,
                retryTransformOnDifferentMimeType, localTransformServiceRegistry);
        remoteTransformerClient = new RemoteTransformerClient(name, baseUrl);
        remoteTransformerClient.setStartupRetryPeriodSeconds(startupRetryPeriodSeconds);

        checkAvailability();
    }

    private boolean remoteTransformerClientConfigured()
    {
        return remoteTransformerClient.getBaseUrl() != null;
    }

    @Override
    public boolean isAvailable()
    {
        if (remoteTransformerClientConfigured() && !remoteTransformerClient.isAvailable())
        {
            checkAvailability();
        }

        return available;
    }

    private void setAvailable(boolean available)
    {
        this.available = available;
    }

    private void checkAvailability()
    {
        if (remoteTransformerClientConfigured())
        {
            try
            {
                Pair<Boolean, String> result = remoteTransformerClient.check(log);
                Boolean isAvailable = result.getFirst();
                String msg = result.getSecond() == null ? "" : result.getSecond();
                if (isAvailable != null && isAvailable)
                {
                    setAvailable(true);
                    log.debug(getAvailableMessage(true, null));
                    log.trace(msg);
                }
                else
                {
                    setAvailable(false);
                    String message = getAvailableMessage(false, msg);
                    if (isAvailable == null)
                    {
                        log.debug(message);
                    }
                    else
                    {
                        log.error(message);
                    }
                }
            }
            catch (Throwable e)
            {
                setAvailable(false);
                log.error(getAvailableMessage(false, e.getMessage()));
                log.debug(e);
            }
        }
        else
        {
            setAvailable(false);
        }
    }

    private String getAvailableMessage(boolean available, String suffix)
    {
        return "Local transformer " + name + " on " + remoteTransformerClient.getBaseUrl() +
                " is " + (available ? "" : "not ") + "available" + (suffix == null ? "." : ": "+suffix);
    }

    @Override
    protected void transformImpl(ContentReader reader,
                                 ContentWriter writer, Map<String, String> transformOptions,
                                 String sourceMimetype, String targetMimetype,
                                 String sourceExtension, String targetExtension,
                                 String renditionName, NodeRef sourceNodeRef) throws Exception
    {
        boolean removeSourceEncoding = false;
        try
        {
            // At some point in the future, we may decide to only pass the sourceEncoding and other dynamic values like
            // it if they were supplied in the rendition definition without a value. The sourceEncoding value is also
            // supplied in the TransformRequest (message to the T-Router).
            if (transformOptions.get("sourceEncoding") == null)
            {
                String sourceEncoding = reader.getEncoding();
                transformOptions.put("sourceEncoding", sourceEncoding);
                removeSourceEncoding = true;
            }

            // Build an array of option names and values and extract the timeout.
            long timeoutMs = 0;
            int nonOptions = transformOptions.containsKey(RenditionDefinition2.TIMEOUT) ? 1 : 0;
            int size = (transformOptions.size() - nonOptions + 3) * 2;
            String[] args = new String[size];
            int i = 0;
            for (Map.Entry<String, String> option : transformOptions.entrySet())
            {
                String name = option.getKey();
                String value = option.getValue();
                if (RenditionDefinition2.TIMEOUT.equals(name))
                {
                    if (value != null)
                    {
                        timeoutMs = Long.parseLong(value);
                    }
                }
                else
                {
                    args[i++] = name;
                    args[i++] = value;
                }
            }

            // These 3 values are commonly needed and are always supplied in the TransformRequest (message to the T-Router).
            // The targetExtension is also supplied in the TransformRequest, but in the case of local and legacy transformers
            // is added by the remoteTransformerClient.request call for historic reasons, so does not need to be added here.
            args[i++] = "sourceMimetype";
            args[i++] = sourceMimetype;
            args[i++] = "sourceExtension";
            args[i++] = sourceExtension;
            args[i++] = "targetMimetype";
            args[i++] = targetMimetype;

            remoteTransformerClient.request(reader, writer, sourceMimetype, sourceExtension, targetExtension,
                    timeoutMs, log, args);
        }
        finally
        {
            if (removeSourceEncoding)
            {
                transformOptions.remove("sourceEncoding");
            }
        }
    }
}
