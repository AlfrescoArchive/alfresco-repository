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
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
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

    private LocalTransformServiceRegistry localTransformServiceRegistry;
    private MimetypeService mimetypeService;
    private boolean strictMimeTypeCheck;
    private boolean retryTransformOnDifferentMimeType;
    private static ThreadLocal<Integer> depth = new ThreadLocal<Integer>()
    {
        @Override
        protected Integer initialValue()
        {
            return 0;
        }
    };

    public AbstractLocalTransformer(String name, ExtensionMap extensionMap, TransformerDebug transformerDebug,
                                    MimetypeService mimetypeService, boolean strictMimeTypeCheck,
                                    boolean retryTransformOnDifferentMimeType,
                                    LocalTransformServiceRegistry localTransformServiceRegistry)
    {
        this.name = name;
        this.extensionMap = extensionMap;
        this.transformerDebug = transformerDebug;
        this.mimetypeService = mimetypeService;
        this.strictMimeTypeCheck = strictMimeTypeCheck;
        this.retryTransformOnDifferentMimeType = retryTransformOnDifferentMimeType;
        this.localTransformServiceRegistry = localTransformServiceRegistry;
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

            if (transformerDebug.isEnabled())
            {
                transformerDebug.pushTransform(name, reader.getContentUrl(), sourceMimetype,
                        targetMimetype, reader.getSize(), renditionName, sourceNodeRef);
            }

            strictMimetypeCheck(reader, sourceNodeRef, sourceMimetype);
            transformImpl(reader, writer, transformOptions, sourceMimetype,
                    targetMimetype, sourceExtension, targetExtension, targetEncoding, renditionName, sourceNodeRef);

            long after = System.currentTimeMillis();
            recordTime(sourceMimetype, targetMimetype, after - before);
        }
        catch (Throwable e)
        {
            long after = System.currentTimeMillis();
            recordError(sourceMimetype, targetMimetype, after - before);
            retryWithDifferentMimetype(reader, writer, targetMimetype, transformOptions, renditionName, sourceNodeRef, e);
        }
        finally
        {
            transformerDebug.popTransform();
            depth.set(depth.get()-1);
        }
    }

    private void strictMimetypeCheck(ContentReader reader, NodeRef sourceNodeRef, String sourceMimetype)
            throws UnsupportedTransformationException
    {
        // TODO Add transformerConfig to supply the list of allowed sourceMimetype, differentType combinations
        //      that will allow a transform to take place.
//        if (mimetypeService != null && transformerConfig != null && strictMimeTypeCheck && depth.get() == 1)
//        {
//            String differentType = mimetypeService.getMimetypeIfNotMatches(reader.getReader());
//
//            if (!transformerConfig.strictMimetypeCheck(sourceMimetype, differentType))
//            {
//                String fileName = transformerDebug.getFileName(sourceNodeRef, true, 0);
//                String readerSourceMimetype = reader.getMimetype();
//                String message = "Transformation of ("+fileName+
//                        ") has not taken place because the declared mimetype ("+
//                        readerSourceMimetype+") does not match the detected mimetype ("+
//                        differentType+").";
//                log.warn(message);
//                throw new UnsupportedTransformationException(message);
//            }
//        }
    }

    private void retryWithDifferentMimetype(ContentReader reader, ContentWriter writer, String targetMimetype,
                                            Map<String, String> transformOptions, String renditionName,
                                            NodeRef sourceNodeRef, Throwable e) throws Exception
    {
        if (mimetypeService != null && localTransformServiceRegistry != null)
        {
            String differentType = mimetypeService.getMimetypeIfNotMatches(reader.getReader());
            if (differentType == null)
            {
                transformerDebug.debug("          Failed", e);
                throw new ContentIOException("Content conversion failed: \n" +
                        "   reader: " + reader + "\n" +
                        "   writer: " + writer + "\n" +
                        "   options: " + transformOptions,
                        e);
            }
            else
            {
                transformerDebug.debug("          Failed: Mime type was '" + differentType + "'", e);
                String claimedMimetype = reader.getMimetype();

                if (retryTransformOnDifferentMimeType)
                {
                    reader = reader.getReader();
                    reader.setMimetype(differentType);
                    long sourceSizeInBytes = reader.getSize();

                    LocalTransformer localTransformer = localTransformServiceRegistry.getLocalTransformer(
                            transformOptions, renditionName, differentType, targetMimetype, sourceSizeInBytes);
                    if (localTransformer == null)
                    {
                        transformerDebug.debug("          Failed", e);
                        throw new ContentIOException("Content conversion failed: \n" +
                                "   reader: " + reader + "\n" +
                                "   writer: " + writer + "\n" +
                                "   options: " + transformOptions + "\n" +
                                "   claimed mime type: " + claimedMimetype + "\n" +
                                "   detected mime type: " + differentType + "\n" +
                                "   transformer not found" + "\n",
                                e
                        );
                    }
                    localTransformer.transform(reader, writer, transformOptions, renditionName, sourceNodeRef);
                }
                else
                {
                    throw new ContentIOException("Content conversion failed: \n" +
                            "   reader: " + reader + "\n" +
                            "   writer: " + writer + "\n" +
                            "   options: " + transformOptions + "\n" +
                            "   claimed mime type: " + claimedMimetype + "\n" +
                            "   detected mime type: " + differentType,
                            e
                    );
                }
            }
        }
    }

    private synchronized void recordTime(String sourceMimetype, String targetMimetype, long transformationTime)
    {
        // TODO Do we wish to gather this information? If so we we will need very similar classes.
    }

    protected synchronized void recordError(String sourceMimetype, String targetMimetype, long transformationTime)
    {
        // TODO Do we wish to gather this information? If so we we will need very similar classes.
    }
}
