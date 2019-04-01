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

import org.alfresco.repo.content.filestore.FileContentWriter;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.transform.client.model.config.ExtensionMap;
import org.alfresco.util.TempFileProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Transformer that passes a document through a pipeline of transformations to arrive at an target mimetype.
 */
public class LocalPipelineTransformer implements LocalTransformer
{
    private final String name;
    private final List<IntermediateTransformer> transformers = new ArrayList<>();
    private final ExtensionMap extensionMap;

    private class IntermediateTransformer
    {
        LocalTransformer intermediateTransformer;
        String targetMimetype;
    }

    public LocalPipelineTransformer(String name, ExtensionMap extensionMap)
    {
        this.name = name;
        this.extensionMap = extensionMap;
    }

    public void addIntermediateTransformer(LocalTransformer intermediateTransformer, String targetExt)
    {
        IntermediateTransformer transformer = new IntermediateTransformer();
        transformer.intermediateTransformer = intermediateTransformer;
        transformer.targetMimetype = extensionMap.toMimetype(targetExt);
    }

    @Override
    public void transform(ContentReader reader, ContentWriter writer, Map<String, String> transformOptions, String renditionName, NodeRef sourceNodeRef) throws Exception
    {
        ContentReader currentReader = reader;
        int lastI = transformers.size() - 1;
        for (int i = 0; i <= lastI; i++)
        {
            IntermediateTransformer transformer = transformers.get(i);

            ContentWriter currentWriter = null;
            if (i == lastI)
            {
                currentWriter = writer;
            }
            else
            {
                // make a temp file writer with the correct extension
                String sourceExt = extensionMap.toExtension(currentReader.getMimetype());
                String targetExt = extensionMap.toExtension(transformer.targetMimetype);
                File tempFile = TempFileProvider.createTempFile(
                        "LocalPipelineTransformer_intermediate_" + sourceExt + "_",
                        "." + targetExt);
                currentWriter = new FileContentWriter(tempFile);
                currentWriter.setMimetype(transformer.targetMimetype);
            }

            transformer.intermediateTransformer.transform(currentReader, currentWriter, transformOptions, renditionName, sourceNodeRef);

            // Clear the sourceNodeRef after the first transformation to avoid later transformers thinking the
            // intermediate file is the original node.
            if (i == 0)
            {
                sourceNodeRef = null;
            }

            // Pass the output to the next transformer
            if (i < lastI)
            {
                currentReader = currentWriter.getReader();
            }
        }
    }
}
