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
package org.alfresco.repo.content.transform2;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.content.transform.RemoteTransformerClient;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.util.Pair;
import org.alfresco.util.PropertyCheck;
import org.apache.commons.logging.Log;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;

import java.util.Map;

/**
 * Abstract local transformer using flat transform options, with support for transforms within the JVM (not encouraged)
 * or in a separate process (typically a Docker container).
 */
public abstract class AbstractLocalTransformer implements LocalTransformer, BeanNameAware, InitializingBean
{
    private String name;

    private RemoteTransformerClient remoteTransformerClient;

    private LocalTransformServiceRegistry registry;

    private MimetypeService mimetypeService;

    private boolean available = false;

    @Override
    public void setBeanName(String beanName)
    {
        this.name = beanName;
    }

    public String getName()
    {
        return (name == null) ? getClass().getSimpleName() : name;
    }

    public void setRemoteTransformerClient(RemoteTransformerClient remoteTransformerClient)
    {
        this.remoteTransformerClient = remoteTransformerClient;
    }

    public void setRegistry(LocalTransformServiceRegistry registry)
    {
        this.registry = registry;
    }

    public void setMimetypeService(MimetypeService mimetypeService)
    {
        this.mimetypeService = mimetypeService;
    }

    boolean remoteTransformerClientConfigured()
    {
        return remoteTransformerClient != null && remoteTransformerClient.getBaseUrl() != null;
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        PropertyCheck.mandatory(this, "remoteTransformerClient", remoteTransformerClient);
        PropertyCheck.mandatory(this, "registry", registry);
        PropertyCheck.mandatory(this, "mimetypeService", mimetypeService);

        String name = getName();
        registry.register(name, this);
        checkAvailability();
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
            Log logger = getLogger();
            try
            {
                Pair<Boolean, String> result = remoteTransformerClient.check(logger);
                Boolean isAvailable = result.getFirst();
                String msg = result.getSecond() == null ? "" : result.getSecond();
                if (isAvailable != null && isAvailable)
                {
                    String versionString = msg;
                    setAvailable(true);
                    logger.info("Using remote " + getName() + ": " + versionString);
                }
                else
                {
                    setAvailable(false);
                    String message = "Remote " + getName() + " is not available for transformations. " + msg;
                    if (isAvailable == null)
                    {
                        logger.debug(message);
                    }
                    else
                    {
                        logger.error(message);
                    }
                }
            }
            catch (Throwable e)
            {
                setAvailable(false);
                logger.error("Remote " + getName() + " is not available: " + (e.getMessage() != null ? e.getMessage() : ""));
                // debug so that we can trace the issue if required
                logger.debug(e);
            }
        }
        else
        {
            available = true;
        }
    }

    @Override
    public void transform(ContentReader reader, ContentWriter writer, Map<String, String> transformOptions)
            throws Exception
    {
        Log logger = getLogger();
        if (isAvailable())
        {
            if (remoteTransformerClientConfigured())
            {
                String sourceMimetype = reader.getMimetype();
                String targetMimetype = writer.getMimetype();
                String targetEncoding = writer.getEncoding();

                String sourceExtension = mimetypeService.getExtension(sourceMimetype);
                String targetExtension = mimetypeService.getExtension(targetMimetype);
                if (sourceExtension == null || targetExtension == null)
                {
                    throw new AlfrescoRuntimeException("Unknown extensions for mimetypes: \n" +
                            "   source mimetype: " + sourceMimetype + "\n" +
                            "   source extension: " + sourceExtension + "\n" +
                            "   target mimetype: " + targetMimetype + "\n" +
                            "   target extension: " + targetExtension + "\n" +
                            "   target encoding: " + targetEncoding);
                }

                transformRemote(remoteTransformerClient, reader, writer, transformOptions, sourceMimetype, targetMimetype,
                        sourceExtension, targetExtension, targetEncoding);
            }
            else
            {
                transformLocal(reader, writer, transformOptions);
            }

            if (logger.isDebugEnabled())
            {
                logger.debug("Transformation completed: \n" +
                        "   source: " + reader + "\n" +
                        "   target: " + writer + "\n" +
                        "   options: " + transformOptions);
            }
        }
        else
        {
            logger.debug("Transformer not available: \n" +
                    "   source: " + reader + "\n" +
                    "   target: " + writer + "\n" +
                    "   options: " + transformOptions);
        }
    }

    protected abstract Log getLogger();

    protected abstract void transformLocal(ContentReader reader, ContentWriter writer, Map<String, String>  transformOptions)
            throws Exception;

    protected abstract void transformRemote(RemoteTransformerClient remoteTransformerClient, ContentReader reader,
                                            ContentWriter writer, Map<String, String>  transformOptions,
                                            String sourceMimetype, String targetMimetype,
                                            String sourceExtension, String targetExtension,
                                            String targetEncoding) throws Exception;
}
