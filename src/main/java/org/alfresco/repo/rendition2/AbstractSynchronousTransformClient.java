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
package org.alfresco.repo.rendition2;

import org.alfresco.repo.content.transform.UnsupportedTransformationException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;

/**
 * Provides methods to cache results in the same Thread from isSupported for use in transform.
 *
 * @author adavis
 */
public abstract class AbstractSynchronousTransformClient<T> implements SynchronousTransformClient<T>
{
    private static Log logger = LogFactory.getLog(SynchronousTransformClient.class);

    private ThreadLocal<T> threadLocalSupportedBy = new ThreadLocal<>();

    protected T getSupportedBy(ContentReader reader, ContentWriter writer, Map<String, String> actualOptions,
                               String transformName, NodeRef sourceNodeRef)
    {
        T supportedBy = threadLocalSupportedBy.get();
        threadLocalSupportedBy.set(null);
        if (supportedBy == null)
        {
            String sourceMimetype = reader.getMimetype();
            long sourceSizeInBytes = reader.getSize();
            String targetMimetype = writer.getMimetype();
            if (!isSupported(sourceMimetype, sourceSizeInBytes, null, targetMimetype,
                    actualOptions, transformName, sourceNodeRef))
            {
                throw new UnsupportedTransformationException("Transformation of " + sourceMimetype +
                        (sourceSizeInBytes > 0 ? " size "+sourceSizeInBytes : "")+ " to " + targetMimetype +
                        " unsupported");
            }
            supportedBy = threadLocalSupportedBy.get();
        }
        return supportedBy;
    }

    T getSupportedBy()
    {
        return threadLocalSupportedBy.get();
    }

    void setSupportedBy(T t)
    {
        threadLocalSupportedBy.set(t);
    }
}
