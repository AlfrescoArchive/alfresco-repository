/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2018 Alfresco Software Limited
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
package org.alfresco.util.json.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.lang.Nullable;

/**
 * This class uses singleton {@link ObjectMapper} with default configuration
 * and provide access to {@link ObjectReader}, {@link ObjectWriter}, {@link ObjectNode} and {@link ArrayNode}.
 * {@link AlfrescoDefaultObjectMapper} should be reused by all classes
 * as it takes time to create a new {@link ObjectMapper} and if it is exposed directly
 * one can alter it's configuration via {@link ObjectMapper#configure(JsonGenerator.Feature, boolean)} and others.
 * .
 */
public class AlfrescoDefaultObjectMapper
{
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static ObjectReader getReader()
    {
        return objectMapper.reader();
    }

    public static ObjectWriter getWriter()
    {
        return objectMapper.writer();
    }

    public static ObjectNode createObjectNode()
    {
        return objectMapper.createObjectNode();
    }

    public static ArrayNode createArrayNode()
    {
        return objectMapper.createArrayNode();
    }

    public static <T> T convertValue(@Nullable Object fromValue, Class<T> toValueType)
    {
        return objectMapper.convertValue(fromValue, toValueType);
    }

    public static String writeValueAsString(Object value) throws JsonProcessingException
    {
        return objectMapper.writeValueAsString(value);
    }
}
