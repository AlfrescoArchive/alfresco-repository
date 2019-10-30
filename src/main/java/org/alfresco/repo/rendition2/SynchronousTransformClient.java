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
package org.alfresco.repo.rendition2;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.TransformationOptions;

import java.util.Map;

/**
 * Request synchronous transforms. Used in refactoring deprecated code, which called Legacy transforms, so that it will
 * first try a Local transform, falling back to Legacy if not available.
 *
 * @author adavis
 */
@Deprecated
public interface SynchronousTransformClient<T>
{
    String IS_SUPPORTED_NOT_CALLED = "isSupported was not called prior to transform in the same Thread.";

    /**
     * Works out if it is possible to transform content of a given source mimetype and size into a target mimetype
     * given a list of actual transform option names and values.
     *
     * @param sourceMimetype the mimetype of the source content
     * @param sourceSizeInBytes the size in bytes of the source content. Ignored if negative.
     * @param contentUrl The url of the content. Optional as it is only used in debug messages.
     * @param targetMimetype the mimetype of the target
     * @param actualOptions the actual name value pairs available that could be passed to the Transform Service.
     * @param transformName (optional) name for the set of options and target mimetype. If supplied is used to cache
     * results to avoid having to work out if a given transformation is supported a second time. The sourceMimetype
     * and sourceSizeInBytes may still change. In the case of ACS this is the rendition name.
     * @param sourceNodeRef the NodeRef of the source content. Optional as it is only used in debug messages.
     * @return {@code}true{@code} if it is supported.
     */
    @Deprecated
    boolean isSupported(String sourceMimetype, long sourceSizeInBytes, String contentUrl, String targetMimetype,
                        Map<String, String> actualOptions, String transformName, NodeRef sourceNodeRef);

    /**
     * Overrides and calls {@link #isSupported(String, long, String, String, Map, String, NodeRef)} to work out if a
     * transform is supported. Uses the {@code contentReader} to work out the {@code sourceMimetype},
     * {@code sourceSizeInBytes} and {@code contentUrl}.
     * @param contentReader to access the sourceNodeRef content property.
     * @param sourceNodeRef the NodeRef of the source content.
     * @param targetMimetype the mimetype of the target
     * @param actualOptions the actual name value pairs available that could be passed to the Transform Service.
     * @param transformName (optional) name for the set of options and target mimetype. If supplied is used to cache
     * results to avoid having to work out if a given transformation is supported a second time. The sourceMimetype
     * and sourceSizeInBytes may still change. In the case of ACS this is the rendition name.
     * @return {@code}true{@code} if it is supported.
     */
    @Deprecated
    // TODO include the following in the calling code, so this method may be removed.
    default boolean isSupported(ContentReader contentReader, NodeRef sourceNodeRef, String targetMimetype,
                                Map<String, String> actualOptions, String transformName)
    {
        String sourceMimetype = contentReader.getMimetype();
        long sourceSizeInBytes = contentReader.getSize();
        String contentUrl = contentReader.getContentUrl();
        return isSupported(sourceMimetype, sourceSizeInBytes, contentUrl, targetMimetype, actualOptions,
                transformName, sourceNodeRef);
    }

    /**
     * Overrides and calls {@link #isSupported(String, long, String, String, Map, String, NodeRef)} to work out if a
     * transform is supported. Uses the {@code nodeService} to read the content of the {@code sourceNodeRef} to work out
     * the {@code sourceMimetype}, {@code sourceSizeInBytes} and {@code contentUrl}.
     * @param nodeService to access the sourceNodeRef's content property.
     * @param sourceNodeRef the NodeRef of the source content.
     * @param targetMimetype the mimetype of the target
     * @param actualOptions the actual name value pairs available that could be passed to the Transform Service.
     * @param transformName (optional) name for the set of options and target mimetype. If supplied is used to cache
     * results to avoid having to work out if a given transformation is supported a second time. The sourceMimetype
     * and sourceSizeInBytes may still change. In the case of ACS this is the rendition name.
     * @return {@code}true{@code} if it is supported.
     */
    @Deprecated
    // TODO include the following in the calling code, so this method may be removed.
    default boolean isSupported(NodeService nodeService, NodeRef sourceNodeRef, String targetMimetype,
                                Map<String, String> actualOptions, String transformName)
    {
        boolean supported = false;
        ContentData contentData = (ContentData) nodeService.getProperty(sourceNodeRef, ContentModel.PROP_CONTENT);
        if (contentData != null && contentData.getContentUrl() != null)
        {
            String sourceMimetype = contentData.getMimetype();
            long sourceSizeInBytes = contentData.getSize();
            String contentUrl = contentData.getContentUrl();
            supported = isSupported(sourceMimetype, sourceSizeInBytes, contentUrl, targetMimetype, actualOptions,
                    transformName, sourceNodeRef);
        }
        return supported;
    }

    /**
     * Requests a synchronous transform. Not used for renditions.
     * The call to this method should be proceeded by a successful call to
     * {@link #isSupported(String, long, String, String, Map, String, NodeRef)} ideally in the <b>SAME</b>
     * {@code Thread}. If this is not possible, the thread that has called {@code isSupported}, should then call
     * {@link #getSupportedBy()}. The returned value then needs to used as a parameter to
     * {@link #setSupportedBy(Object)} by the thread that is about to call {@code transform}.
     * @param reader of the source content
     * @param writer to the target node's content
     * @param actualOptions the actual name value pairs available that could be passed to the Transform Service.
     * @param transformName (optional) name for the set of options and target mimetype. If supplied is used to cache
     * results to avoid having to work out if a given transformation is supported a second time. The sourceMimetype
     * and sourceSizeInBytes may still change. In the case of ACS this is the rendition name.
     * @param sourceNodeRef the source node
     */
    @Deprecated
    void transform(ContentReader reader, ContentWriter writer, Map<String, String> actualOptions,
                   String transformName, NodeRef sourceNodeRef) throws Exception;

    /**
     * Only needed if {@code isSupported} and {@code transform} are called in different {@code Threads}.
     * See the description in {@link #transform(ContentReader, ContentWriter, Map, String, NodeRef)}.
     */
    @Deprecated
    T getSupportedBy();

    /**
     * Only needed if {@code isSupported} and {@code transform} are called in different {@code Threads}.
     * See the description in {@link #transform(ContentReader, ContentWriter, Map, String, NodeRef)}.
     */
    @Deprecated
    void setSupportedBy(T t);

    // TODO Replace code that calls this method with code that uses the newer Map of transform objects.
    @Deprecated
    Map<String, String> convertOptions(TransformationOptions options);
}
