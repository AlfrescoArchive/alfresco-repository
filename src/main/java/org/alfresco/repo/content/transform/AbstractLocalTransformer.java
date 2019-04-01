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
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.transform.client.model.config.ExtensionMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;

/**
 * Abstract supper class for local transformer using flat transform options.
 */
public abstract class AbstractLocalTransformer implements LocalTransformer
{
    protected static final Log log = LogFactory.getLog(LocalTransformer.class);

    protected String name;
    protected ExtensionMap extensionMap;
    protected TransformerDebug transformerDebug;
    private static ThreadLocal<Integer> depth = new ThreadLocal<Integer>()
    {
        @Override
        protected Integer initialValue()
        {
            return 0;
        }
    };

    public AbstractLocalTransformer(String name, ExtensionMap extensionMap, TransformerDebug transformerDebug)
    {
        this.name = name;
        this.extensionMap = extensionMap;
        this.transformerDebug = transformerDebug;
    }

    public abstract boolean isAvailable();

    protected abstract void transformImpl(ContentReader reader,
                                          ContentWriter writer, Map<String, String> transformOptions,
                                          String sourceMimetype, String targetMimetype,
                                          String sourceExtension, String targetExtension,
                                          String targetEncoding, String renditionName, NodeRef sourceNodeRef)
                                          throws Exception;

    @Override
    public void transform(ContentReader reader, ContentWriter writer, Map<String, String> transformOptions,
                          String renditionName, NodeRef sourceNodeRef)
            throws Exception
    {
        if (isAvailable())
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

            transformImpl(reader, writer, transformOptions, sourceMimetype,
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
}
