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
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.ContentTransformService;
import org.alfresco.service.cmr.repository.NoTransformerException;
import org.alfresco.service.cmr.repository.TransformationOptions;

import java.util.List;
import java.util.Map;

/**
 * Contains deprecated code originally from {@link org.alfresco.repo.content.ContentServiceImpl} that is used to perform
 * Legacy transforms.
 *
 * @author adavis
 */
@Deprecated
public class ContentTransformServiceAdaptor implements ContentTransformService
{
    private ContentTransformer imageMagickContentTransformer;
    private LegacySynchronousTransformClient legacySynchronousTransformClient;

    @Deprecated
    public void setImageMagickContentTransformer(ContentTransformer imageMagickContentTransformer)
    {
        this.imageMagickContentTransformer = imageMagickContentTransformer;
    }

    public void setLegacySynchronousTransformClient(LegacySynchronousTransformClient legacySynchronousTransformClient)
    {
        this.legacySynchronousTransformClient = legacySynchronousTransformClient;
    }

    @Deprecated
    @Override
    public void transform(ContentReader reader, ContentWriter writer)
    {
        this.transform(reader, writer, new TransformationOptions());
    }

    @Deprecated
    @Override
    public void transform(ContentReader reader, ContentWriter writer, Map<String, Object> options)
            throws NoTransformerException, ContentIOException
    {
        transform(reader, writer, new TransformationOptions(options));
    }

    @Deprecated
    @Override
    public void transform(ContentReader reader, ContentWriter writer, TransformationOptions options) // TODO replace calls
            throws NoTransformerException, ContentIOException
    {
        legacySynchronousTransformClient.transform(reader, writer, options);
    }

    @Deprecated
    @Override
    public ContentTransformer getTransformer(String sourceMimetype, String targetMimetype)
    {
        return getTransformer(null, sourceMimetype, -1, targetMimetype, new TransformationOptions());
    }

    @Deprecated
    @Override
    public ContentTransformer getTransformer(String sourceMimetype, String targetMimetype, TransformationOptions options)
    {
        return getTransformer(null, sourceMimetype, -1, targetMimetype, options);
    }

    @Deprecated
    @Override
    public ContentTransformer getTransformer(String sourceUrl, String sourceMimetype, long sourceSize, String targetMimetype, TransformationOptions options) // TODO replace calls
    {
        return legacySynchronousTransformClient.getTransformer(sourceUrl, sourceMimetype, sourceSize, targetMimetype, options);
    }

    @Deprecated
    @Override
    public List<ContentTransformer> getTransformers(String sourceUrl, String sourceMimetype, long sourceSize, String targetMimetype, TransformationOptions options)
    {
        return legacySynchronousTransformClient.getTransformers(sourceUrl, sourceMimetype, sourceSize, targetMimetype, options);
    }

    @Deprecated
    @Override
    public long getMaxSourceSizeBytes(String sourceMimetype, String targetMimetype, TransformationOptions options) // TODO replace call
    {
        return legacySynchronousTransformClient.getMaxSourceSizeBytes(sourceMimetype, targetMimetype, options);
    }

    @Deprecated
    @Override
    public List<ContentTransformer> getActiveTransformers(String sourceMimetype, String targetMimetype, TransformationOptions options)
    {
        return getActiveTransformers(sourceMimetype, -1, targetMimetype, options);
    }

    @Deprecated
    @Override
    public List<ContentTransformer> getActiveTransformers(String sourceMimetype, long sourceSize, String targetMimetype, TransformationOptions options)
    {
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
        return isTransformable(reader, writer, new TransformationOptions());
    }

    @Deprecated
    @Override
    public boolean isTransformable(ContentReader reader, ContentWriter writer, TransformationOptions options)
    {
        return legacySynchronousTransformClient.isSupported(reader, writer.getMimetype(), legacySynchronousTransformClient.convertOptions(options), null, null);
    }
}
