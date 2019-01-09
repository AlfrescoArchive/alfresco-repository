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
import org.alfresco.transform.client.model.config.ExtensionMap;
import org.alfresco.transform.client.model.config.TransformServiceRegistry;
import org.alfresco.transform.client.model.config.TransformServiceRegistryImpl;
import org.alfresco.util.PropertyCheck;
import org.springframework.beans.factory.InitializingBean;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * Implements {@link TransformServiceRegistry} providing a mechanism of validating if a local transformation
 * (based on {@link LocalTransformer} request is supported. It also extends this interface to provide a
 * {@link #transform} method.
 * @author adavis
 */
public class LocalTransformServiceRegistry extends TransformServiceRegistryImpl implements InitializingBean
{
    private MimetypeService mimetypeService;
    private String transformServiceConfigFile;
    private boolean enabled = true;
    private boolean firstTime = true;
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

    public void setTransformerDebug(TransformerDebug transformerDebug)
    {
        this.transformerDebug = transformerDebug;
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        PropertyCheck.mandatory(this, "mimetypeService", mimetypeService);
        PropertyCheck.mandatory(this, "transformServiceConfigFile", transformServiceConfigFile);
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

    public void register(String name, LocalTransformer transformer)
    {
        transformers.put(name, transformer);
    }

    private LocalTransformer getLocalTransformer(String name)
    {
        return transformers.get(name);
    }

    @Override
    public long getMaxSize(String sourceMimetype, String targetMimetype, Map<String, String> options, String renditionName)
    {
        // This message is not logged if placed in afterPropertiesSet
        if (firstTime)
        {
            firstTime = false;
            transformerDebug.debug("Local transform Server is " + (enabled ? "enabled" : "disabled"));
        }

        return enabled
                ? super.getMaxSize(sourceMimetype, targetMimetype, options, renditionName)
                : 0;
    }

    public void transform(ContentReader reader, ContentWriter writer, Map<String, String> actualOptions, String renditionName)
    {

        String sourceMimetype = reader.getMimetype();
        String targetMimetype = writer.getMimetype();
        long sourceSizeInBytes = reader.getSize();
        String transformerName = getTransformerName(sourceMimetype, sourceSizeInBytes, targetMimetype, actualOptions, renditionName);

        // TODO
    }
}
