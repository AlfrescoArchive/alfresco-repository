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

import org.alfresco.repo.content.transform.LocalTransform;
import org.alfresco.repo.content.transform.LocalTransformServiceRegistry;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.PropertyCheck;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.Map;

/**
 * Request synchronous transforms.
 *
 * Transforms take place using transforms available on the local machine (based on {@link LocalTransform}).
 *
 * @author adavis
 */
public class LocalSynchronousTransformClient extends AbstractSynchronousTransformClient<LocalTransform>
        implements SynchronousTransformClient<LocalTransform>, InitializingBean
{
    private static final String TRANSFORM = "Local synchronous transform ";
    private static Log logger = LogFactory.getLog(LocalTransformClient.class);

    private LocalTransformServiceRegistry localTransformServiceRegistry;

    public void setLocalTransformServiceRegistry(LocalTransformServiceRegistry localTransformServiceRegistry)
    {
        this.localTransformServiceRegistry = localTransformServiceRegistry;
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        PropertyCheck.mandatory(this, "localTransformServiceRegistry", localTransformServiceRegistry);
    }

    @Override
    public boolean isSupported(String sourceMimetype, long sourceSizeInBytes, String contentUrl, String targetMimetype,
                               Map<String, String> actualOptions, String transformName, NodeRef sourceNodeRef)
    {
        String renditionName = TransformDefinition.convertToRenditionName(transformName);
        LocalTransform transform = localTransformServiceRegistry.getLocalTransform(sourceMimetype,
                sourceSizeInBytes, targetMimetype, actualOptions, renditionName);
        setSupportedBy(transform);

        if (logger.isDebugEnabled())
        {
            logger.debug(TRANSFORM + renditionName + " from " + sourceMimetype +
                    (transform == null ? " is unsupported" : " is supported"));
        }
        return transform != null;
    }

    @Override
    public void transform(ContentReader reader, ContentWriter writer, Map<String, String> actualOptions,
                          String transformName, NodeRef sourceNodeRef)
    {
        String renditionName = TransformDefinition.convertToRenditionName(transformName);
        try
        {
            LocalTransform transform = getSupportedBy(reader, writer, actualOptions, transformName, sourceNodeRef);

            if (null == reader || !reader.exists())
            {
                throw new IllegalArgumentException("sourceNodeRef "+sourceNodeRef+" has no content.");
            }

            if (logger.isDebugEnabled())
            {
                logger.debug(TRANSFORM + " requested " + renditionName);
            }

            transform.transform(reader, writer, actualOptions, renditionName, sourceNodeRef);

            if (logger.isDebugEnabled())
            {
                logger.debug(TRANSFORM + " created " + renditionName);
            }
        }
        catch (Exception e)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(TRANSFORM + " failed " + renditionName, e);
            }
            throw e;
        }
    }
}
