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

import org.alfresco.repo.content.transform.TransformerDebug;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.transform.client.model.config.ExtensionMap;
import org.alfresco.transform.client.model.config.TransformServiceRegistry;
import org.alfresco.transform.client.model.config.TransformServiceRegistryImpl;
import org.alfresco.transform.client.model.config.Transformer;
import org.alfresco.util.PropertyCheck;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Implements {@link TransformServiceRegistry} providing a mechanism of validating if a local transformation
 * (based on {@link LocalTransformer} request is supported. It also extends this interface to provide a
 * {@link #transform} method.
 * @author adavis
 */
public class LocalTransformServiceRegistry extends TransformServiceRegistryImpl implements InitializingBean
{
    private static final Log log = LogFactory.getLog(LocalTransformerImpl.class);

    private MimetypeService mimetypeService;
    private String transformServiceConfigFile;
    private boolean enabled = true;
    private boolean firstTime = true;
    private Properties properties;
    private TransformerDebug transformerDebug;

    private Map<String, LocalTransformer> transformers = new HashMap<>();

    public void setMimetypeService(MimetypeService mimetypeService)
    {
        this.mimetypeService = mimetypeService;
    }

    public void setTransformServiceConfigFile(String transformServiceConfigFile)
    {
        this.transformServiceConfigFile = transformServiceConfigFile;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    /**
     * The Alfresco global properties.
     */
    public void setProperties(Properties properties)
    {
        this.properties = properties;
    }

    public void setTransformerDebug(TransformerDebug transformerDebug)
    {
        this.transformerDebug = transformerDebug;
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        PropertyCheck.mandatory(this, "mimetypeService", mimetypeService);
        PropertyCheck.mandatory(this, "transformServiceConfigFile", transformServiceConfigFile);
        PropertyCheck.mandatory(this, "properties", properties);
        PropertyCheck.mandatory(this, "transformerDebug", transformerDebug);

        setExtensionMap(new ExtensionMap() {
            @Override
            public String toMimetype(String extension)
            {
                return mimetypeService.getMimetype(extension);
            }
        });
        super.afterPropertiesSet();

        try (Reader reader = new BufferedReader(new InputStreamReader(getClass().getClassLoader().
                getResourceAsStream(transformServiceConfigFile))))
        {
            register(reader);
        }
    }

    @Override
    public void register(Transformer transformer)
    {
        super.register(transformer);

        // TODO handle a pipeline

        try
        {
            String name = transformer.getName();
            String baseUrl = getBaseUrl(name);
            int startupRetryPeriodSeconds = getStartupRetryPeriodSeconds(name);
            LocalTransformerImpl localTransformer =
                    new LocalTransformerImpl(name, baseUrl, startupRetryPeriodSeconds,
                            mimetypeService, transformerDebug);
            transformers.put(name, localTransformer);
        }
        catch (IllegalArgumentException ignore)
        {
            // We will have logged an error already and there is not much else we can do.
        }
    }

    private String getBaseUrl(String name)
    {
        String baseUrlName = name.toLowerCase() + ".url";
        String baseUrl = properties.getProperty(baseUrlName);
        if (baseUrl == null)
        {
            String msg = "Local transformer property " + baseUrlName + " was not set";
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }
        return baseUrl;
    }

    private int getStartupRetryPeriodSeconds(String name)
    {
        String startupRetryPeriodSecondsName = name.toLowerCase() + ".startupRetryPeriodSeconds";
        String property = properties.getProperty(startupRetryPeriodSecondsName, "0");
        int startupRetryPeriodSeconds;
        try
        {
            startupRetryPeriodSeconds = Integer.parseInt(property);
        }
        catch (NumberFormatException e)
        {
            String msg = "Local transformer property " + startupRetryPeriodSecondsName + " should be an integer";
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }
        return startupRetryPeriodSeconds;
    }

    @Override
    public long getMaxSize(String sourceMimetype, String targetMimetype, Map<String, String> options, String renditionName)
    {
        // This message is not logged if placed in afterPropertiesSet
        if (firstTime)
        {
            firstTime = false;
            transformerDebug.debug("Local transforms are " + (enabled ? "enabled" : "disabled"));
        }

        return enabled
                ? super.getMaxSize(sourceMimetype, targetMimetype, options, renditionName)
                : 0;
    }

    public void transform(ContentReader reader, ContentWriter writer, Map<String, String> actualOptions,
                          String renditionName, NodeRef sourceNodeRef) throws Exception
    {

        String sourceMimetype = reader.getMimetype();
        String targetMimetype = writer.getMimetype();
        long sourceSizeInBytes = reader.getSize();
        String name = getTransformerName(sourceMimetype, sourceSizeInBytes, targetMimetype, actualOptions, renditionName);

        LocalTransformer localTransformer = transformers.get(name);
        localTransformer.transform(reader, writer, actualOptions, renditionName, sourceNodeRef);
    }
}
