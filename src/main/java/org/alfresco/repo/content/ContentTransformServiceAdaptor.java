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
package org.alfresco.repo.content;

import org.alfresco.repo.content.transform.ContentTransformer;
import org.alfresco.repo.rendition2.LegacySynchronousTransformClient;
import org.alfresco.repo.rendition2.SynchronousTransformClient;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentTransformService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NoTransformerException;
import org.alfresco.service.cmr.repository.TransformationOptions;
import org.alfresco.transform.client.registry.TransformServiceRegistry;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Contains deprecated methods originally from {@link org.alfresco.repo.content.ContentServiceImpl} that is used to
 * perform Legacy transforms.
 *
 * @author adavis
 */
@Deprecated
public class ContentTransformServiceAdaptor implements ContentTransformService
{
    private ContentTransformer imageMagickContentTransformer;
    private LegacySynchronousTransformClient legacySynchronousTransformClient;
    private SynchronousTransformClient synchronousTransformClient;
    private TransformServiceRegistry localTransformServiceRegistry;

    @Deprecated
    public void setImageMagickContentTransformer(ContentTransformer imageMagickContentTransformer)
    {
        this.imageMagickContentTransformer = imageMagickContentTransformer;
    }

    public void setLegacySynchronousTransformClient(LegacySynchronousTransformClient legacySynchronousTransformClient)
    {
        this.legacySynchronousTransformClient = legacySynchronousTransformClient;
    }

    public void setSynchronousTransformClient(SynchronousTransformClient synchronousTransformClient)
    {
        this.synchronousTransformClient = synchronousTransformClient;
    }

    public void setLocalTransformServiceRegistry(TransformServiceRegistry localTransformServiceRegistry)
    {
        this.localTransformServiceRegistry = localTransformServiceRegistry;
    }

    @Deprecated
    @Override
    public void transform(ContentReader reader, ContentWriter writer)
    {
        synchronousTransformClient.transform(reader, writer, Collections.emptyMap(), null, null);
    }

    @Deprecated
    @Override
    public void transform(ContentReader reader, ContentWriter writer, Map<String, Object> legacyOptionsMap)
            throws NoTransformerException, ContentIOException
    {
        TransformationOptions transformationOptions = new TransformationOptions(legacyOptionsMap);
        Map<String, Object> options = synchronousTransformClient.convertOptions(transformationOptions);
        synchronousTransformClient.transform(reader, writer, options, null, null);
    }

    @Deprecated
    @Override
    public void transform(ContentReader reader, ContentWriter writer, TransformationOptions transformationOptions) // TODO replace calls
            throws NoTransformerException, ContentIOException
    {
        Map<String, Object> options = synchronousTransformClient.convertOptions(transformationOptions);
        synchronousTransformClient.transform(reader, writer, options, null, null);
    }

    @Deprecated
    @Override
    public ContentTransformer getTransformer(String sourceMimetype, String targetMimetype)
    {
        return legacySynchronousTransformClient.getTransformer(null, sourceMimetype, -1,
                targetMimetype, new TransformationOptions());
    }

    @Deprecated
    @Override
    public ContentTransformer getTransformer(String sourceMimetype, String targetMimetype, TransformationOptions options)
    {
        return legacySynchronousTransformClient.getTransformer(null, sourceMimetype, -1, targetMimetype, options);
    }

    @Deprecated
    @Override
    public ContentTransformer getTransformer(String sourceUrl, String sourceMimetype, long sourceSize,
                                             String targetMimetype, TransformationOptions options)
    {
        return legacySynchronousTransformClient.getTransformer(sourceUrl, sourceMimetype, sourceSize, targetMimetype, options);
    }

    @Deprecated
    @Override
    // Same as getActiveTransformers, but with debug
    public List<ContentTransformer> getTransformers(String sourceUrl, String sourceMimetype, long sourceSize,
                                                    String targetMimetype, TransformationOptions options)
    {
        return legacySynchronousTransformClient.getTransformers(sourceUrl, sourceMimetype, sourceSize, targetMimetype, options);
    }

    @Deprecated
    @Override
    public long getMaxSourceSizeBytes(String sourceMimetype,
                                      String targetMimetype, TransformationOptions transformationOptions)
    {
        Map<String, String> options = synchronousTransformClient.convertOptions(transformationOptions);
        return localTransformServiceRegistry.findMaxSize(sourceMimetype, targetMimetype, options, null);
    }

    @Deprecated
    @Override
    // Same as getTransformers, but without debug
    public List<ContentTransformer> getActiveTransformers(String sourceMimetype,
                                                          String targetMimetype, TransformationOptions options)
    {
        return getActiveTransformers(sourceMimetype, -1, targetMimetype, options);
    }

    @Deprecated
    @Override
    // Same as getTransformers, but without debug
    public List<ContentTransformer> getActiveTransformers(String sourceMimetype, long sourceSize,
                                                          String targetMimetype, TransformationOptions options)
    {
        // TODO if Local transforms are going to be used, create a ContentTransformer wrapper for each of them.
        //      Also need to do it for other methods in this class that return ContentTransformers.
        return legacySynchronousTransformClient.getActiveTransformers(sourceMimetype, sourceSize, targetMimetype, options);
    }

    @Deprecated
    @Override
    public ContentTransformer getImageTransformer()
    {
        return imageMagickContentTransformer;
    }

    @Deprecated
    @Override
    public boolean isTransformable(ContentReader reader, ContentWriter writer)
    {
        return synchronousTransformClient.isSupported(reader, writer.getMimetype(), Collections.emptyMap(),
                null, null);
    }

    @Deprecated
    @Override
    public boolean isTransformable(ContentReader reader, ContentWriter writer, TransformationOptions transformationOptions)
    {
        Map<String, String> options = synchronousTransformClient.convertOptions(transformationOptions);
        return synchronousTransformClient.isSupported(reader, writer.getMimetype(), options,
                null, null);
    }
}
