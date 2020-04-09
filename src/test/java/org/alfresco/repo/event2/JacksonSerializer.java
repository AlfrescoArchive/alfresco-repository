/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
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

package org.alfresco.repo.event2;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;


/**
 * @author Iulian Aftene
 */

public class JacksonSerializer
{
    private static final ObjectMapper MAPPER;

    static
    {
        MAPPER = new ObjectMapper();
        MAPPER.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        MAPPER.registerModule(new JavaTimeModule());
    }

    public static <T> byte[] serialize(T value) throws Exception
    {
        try (final ByteArrayOutputStream stream = new ByteArrayOutputStream(1024);
            final OutputStreamWriter writer = new OutputStreamWriter(stream, UTF_8))
        {
            MAPPER.writer().writeValue(writer, value);
            return stream.toByteArray();
        }
    }

    public static <T> T deserialize(byte[] data, Class<T> cls) throws Exception
    {
        return MAPPER.readValue(data, cls);
    }

    public static <T> T deserialize(byte[] data, int len, Class<T> cls) throws Exception
    {
        return MAPPER.readValue(data, 0, len, cls);
    }

    public static <T> T deserialize(String data, Class<T> cls) throws Exception
    {
        return MAPPER.readValue(data, cls);
    }

    public static String readStringValue(String json, String key) throws Exception
    {
        JsonNode node = MAPPER.readTree(json);
        for (String k : key.split("\\."))
        {
            node = node.get(k);
        }
        return node.asText();
    }
}
