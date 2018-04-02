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

package org.alfresco.util.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;
import com.fasterxml.jackson.databind.node.ValueNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This class provide utility methods for JSON conversion
 */
public class JsonUtil
{
    // outputs pretty JSON strings
    private static ObjectMapper prettyObjectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public static String toPrettyJson(String jsonString)
    {
        String result = jsonString;
        try
        {
            result = prettyObjectMapper.readTree(jsonString).toString();
        }
        catch (IOException ignored)
        {
            // Intentionally empty
        }
        return result;
    }

    /**
     * Converts ArrayNode to Java List of Objects
     */
    public static List<Object> convertJSONArrayToList(ArrayNode arrayNode)
    {
        List<Object> result = new ArrayList<Object>();

        for (int i = 0; i < arrayNode.size(); i++)
        {
            Object o = arrayNode.get(i);

            if (o instanceof ArrayNode)
            {
                result.add(convertJSONArrayToList((ArrayNode) o));
            }
            else if (o instanceof ObjectNode)
            {
                result.add(convertJSONObjectToMap((ObjectNode) o));
            }
            else if (o instanceof ValueNode)
            {
                result.add(convertJSONValue((ValueNode) o));
            }
        }

        return result;
    }

    /**
     * Extract Java object from JSON
     *
     * @return object extracted from ValueNode
     */
    public static Object convertJSONValue(ValueNode valueNode)
    {
        switch(valueNode.getNodeType())
        {
            case NUMBER:
                return valueNode.numberValue();
            case STRING:
                return valueNode.textValue();
            case BOOLEAN:
                return valueNode.booleanValue();
            case POJO:
                return ((POJONode) valueNode).getPojo();
            case NULL:
            default:
                return null;
        }
    }

    /**
     * convert JSONObject to Map
     */
    public static Map<String, Object> convertJSONObjectToMap(ObjectNode jo)
    {
        Map<String, Object> model = new HashMap<String, Object>();

        Iterator<Map.Entry<String, JsonNode>> itr = jo.fields();
        while (itr.hasNext())
        {
            Map.Entry<String, JsonNode> element = itr.next();

            JsonNode o = element.getValue();
            if (o instanceof ObjectNode)
            {
                model.put(element.getKey(), convertJSONObjectToMap((ObjectNode) o));
            }
            else if (o instanceof ArrayNode)
            {
                model.put(element.getKey(), convertJSONArrayToList((ArrayNode) o));
            }
            else if (o instanceof ValueNode)
            {
                model.put(element.getKey(), convertJSONValue((ValueNode) o));
            }
        }
        return model;
    }
}
