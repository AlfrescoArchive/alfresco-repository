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

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.rendition2.RenditionDefinition2;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.transform.client.model.config.ExtensionMap;
import org.alfresco.util.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;

/**
 * A local transformer using flat transform options. Instances are automatically created for transformers defined in
 * {@code}local-transform-service-config.json{@code}. The transforms take place in a separate process (typically a
 * Docker container).
 */
public class LocalTransformerImpl implements LocalTransformer
{
    private static final Log log = LogFactory.getLog(LocalTransformerImpl.class);
    
    private String name;
    private ExtensionMap extensionMap;
    private TransformerDebug transformerDebug;
    private RemoteTransformerClient remoteTransformerClient;

    private boolean available = false;
    private static ThreadLocal<Integer> depth = new ThreadLocal<Integer>()
    {
        @Override
        protected Integer initialValue()
        {
            return 0;
        }
    };

    public LocalTransformerImpl(String name, String baseUrl, int startupRetryPeriodSeconds,
                                ExtensionMap extensionMap, TransformerDebug transformerDebug)
    {
        this.name = name;
        this.extensionMap = extensionMap;
        this.transformerDebug = transformerDebug;

        remoteTransformerClient = new RemoteTransformerClient(name, baseUrl);
        remoteTransformerClient.setStartupRetryPeriodSeconds(startupRetryPeriodSeconds);

        checkAvailability();
    }

    public String getName()
    {
        return name;
    }

    boolean remoteTransformerClientConfigured()
    {
        return remoteTransformerClient.getBaseUrl() != null;
    }

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
        // check availability
        if (remoteTransformerClientConfigured())
        {
            try
            {
                Pair<Boolean, String> result = remoteTransformerClient.check(log);
                Boolean isAvailable = result.getFirst();
                String msg = result.getSecond() == null ? "" : result.getSecond();
                if (isAvailable != null && isAvailable)
                {
                    String versionString = msg;
                    setAvailable(true);
                    log.info("Using local transformer " + getName() + ": " + versionString);
                }
                else
                {
                    setAvailable(false);
                    String message = "Local transformer " + getName() + " is not available. " + msg;
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
                log.error("Local transformer " + getName() + " is not available: " + (e.getMessage() != null ? e.getMessage() : ""));
                // debug so that we can trace the issue if required
                log.debug(e);
            }
        }
        else
        {
            setAvailable(false);
        }
    }

    @Override
    public void transform(ContentReader reader, ContentWriter writer, Map<String, String> transformOptions,
                          String renditionName, NodeRef sourceNodeRef)
            throws Exception
    {
        if (isAvailable())
        {
//          if (remoteTransformerClientConfigured()) - should always be true if isAvailable().
//                                It might have been false with legacy local transformers in ACS 6.1.
            {
                String sourceMimetype = reader.getMimetype();
                String targetMimetype = writer.getMimetype();
                String targetEncoding = writer.getEncoding();

                String sourceExtension = extensionMap.toExtension(sourceMimetype);
                String targetExtension = extensionMap.toExtension(targetMimetype);
                if (sourceExtension == null || targetExtension == null)
                {
                    throw new AlfrescoRuntimeException("Unknown extensions for mimetypes: \n" +
                            "   source mimetype: " + sourceMimetype + "\n" +
                            "   source extension: " + sourceExtension + "\n" +
                            "   target mimetype: " + targetMimetype + "\n" +
                            "   target extension: " + targetExtension + "\n" +
                            "   target encoding: " + targetEncoding);
                }

                transformWithDebug(reader, writer, transformOptions, renditionName, sourceNodeRef, sourceMimetype,
                        targetMimetype, targetEncoding, sourceExtension, targetExtension);
            }

            if (log.isDebugEnabled())
            {
                log.debug("Local transformation completed: \n" +
                        "   source: " + reader + "\n" +
                        "   target: " + writer + "\n" +
                        "   options: " + transformOptions);
            }
        }
        else
        {
            log.debug("Local transformer not available: \n" +
                    "   source: " + reader + "\n" +
                    "   target: " + writer + "\n" +
                    "   options: " + transformOptions);
        }
    }

    private void transformWithDebug(ContentReader reader, ContentWriter writer, Map<String, String> transformOptions,
                                    String renditionName, NodeRef sourceNodeRef, String sourceMimetype, String targetMimetype,
                                    String targetEncoding, String sourceExtension, String targetExtension) throws Exception
    {

        long before = System.currentTimeMillis();
        try
        {
            depth.set(depth.get()+1);

            // TODO strictMimetypeCheck?

            if (transformerDebug.isEnabled())
            {
                transformerDebug.pushTransform(name, reader.getContentUrl(), sourceMimetype,
                        targetMimetype, reader.getSize(), renditionName, sourceNodeRef);
            }

            transformRemote(remoteTransformerClient, reader, writer, transformOptions, sourceMimetype,
                    targetMimetype, sourceExtension, targetExtension, targetEncoding, renditionName, sourceNodeRef);
        }
        catch (Throwable e)
        {
            // TODO retryTransformOnDifferentMimeType?
        }
        finally
        {
            transformerDebug.popTransform();
            depth.set(depth.get()-1);
        }
    }

    private Log getLogger()
    {
        return log;
    }

    private void transformRemote(RemoteTransformerClient remoteTransformerClient, ContentReader reader,
                                 ContentWriter writer, Map<String, String> transformOptions,
                                 String sourceMimetype, String targetMimetype,
                                 String sourceExtension, String targetExtension,
                                 String targetEncoding, String renditionName, NodeRef sourceNodeRef) throws Exception
    {
        // Build an array of option names and values and extract the timeout.
        long timeoutMs = 0;
        int nonOptions = transformOptions.containsKey(RenditionDefinition2.TIMEOUT) ? 1 : 0;
        int size = (transformOptions.size() - nonOptions) * 2;
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

        remoteTransformerClient.request(reader, writer, sourceMimetype, sourceExtension, targetExtension,
                timeoutMs, log, args);
    }
}
