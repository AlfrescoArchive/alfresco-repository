/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
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
package org.alfresco.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.alfresco.util.json.JsonUtil;
import org.springframework.extensions.surf.util.ISO8601DateFormat;

/**
 * Utility to convert JSON to Freemarker-compatible data model
 * 
 * @author janv
 */
public final class JSONtoFmModel
{
    public static String ROOT_ARRAY = "root";
    
    // note: current format is dependent on ISO8601DateFormat.parser, eg. YYYY-MM-DDThh:mm:ss.sssTZD
    private static String REGEXP_ISO8061 = "^([0-9]{4})-([0-9]{2})-([0-9]{2})T([0-9]{2}):([0-9]{2}):([0-9]{2})(.([0-9]){3})?(Z|[\\+\\-]([0-9]{2}):([0-9]{2}))$";
    private static Pattern matcherISO8601 = Pattern.compile(REGEXP_ISO8061);
    
    public static boolean autoConvertISO8601 = true;

    /**
     * Convert JSON Object string to Freemarker-compatible data model
     * 
     * @param jsonString String
     * @return model
     */
    public static Map<String, Object> convertJSONObjectToMap(String jsonString) throws IOException
    {
        ObjectNode jo = (ObjectNode) JsonUtil.getObjectMapper().readTree(jsonString);
        return convertJSONObjectToMap(jo);
    }
    
    /**
     * JSONObject is an unordered collection of name/value pairs -> convert to Map (equivalent to Freemarker "hash")
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> convertJSONObjectToMap(ObjectNode jo) throws IOException
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
                if (autoConvertISO8601 && (matcherISO8601.matcher(valueNode.textValue()).matches()))
                {
                    return ISO8601DateFormat.parse(valueNode.textValue());
                }
                else
                {
                    return valueNode.textValue();
                }
            case BOOLEAN:
                return valueNode.booleanValue();
            case POJO:
                return ((POJONode) valueNode).getPojo();
            case NULL:
            default:
                return null; // note: http://freemarker.org/docs/dgui_template_exp.html#dgui_template_exp_missing
        }
    }

    /**
     * Convert JSON Array string to Freemarker-compatible data model
     * 
     * @param jsonString String
     * @return model
     */
    public static Map<String, Object> convertJSONArrayToMap(String jsonString) throws IOException
    {
        Map<String, Object> model = new HashMap<String, Object>();
        ArrayNode ja = (ArrayNode) JsonUtil.getObjectMapper().readTree(jsonString);
        model.put(ROOT_ARRAY, convertJSONArrayToList(ja));
        return model;
    }
    
    /**
     * JSONArray is an ordered sequence of values -> convert to List (equivalent to Freemarker "sequence")
     */
    public static List<Object> convertJSONArrayToList(ArrayNode ja) throws IOException
    {
        List<Object> model = new ArrayList<Object>();

        for (int i = 0; i < ja.size(); i++)
        {
            Object o = ja.get(i);

            if (o instanceof ArrayNode)
            {
                model.add(convertJSONArrayToList((ArrayNode) o));
            }
            else if (o instanceof ObjectNode)
            {
                model.add(convertJSONObjectToMap((ObjectNode) o));
            }
            else if (o instanceof ValueNode)
            {
                model.add(convertJSONValue((ValueNode) o));
            }
        }

        return model;
    }
   
    // for debugging only
    public static String toString(Map<String, Object> map)
    {
        return JSONtoFmModel.toStringBuffer(map, 0).toString();
    }
    
    @SuppressWarnings("unchecked")
    private static StringBuffer toStringBuffer(Map<String, Object> unsortedMap, int indent)
    {      
        StringBuffer tabs = new StringBuffer();
        for (int i = 0; i < indent; i++)
        {
            tabs.append("\t");
        }
        
        StringBuffer sb = new StringBuffer();
        
        SortedMap<String, Object> map = new TreeMap<String, Object>();
        map.putAll(unsortedMap);
        
        for (Map.Entry<String, Object> entry : map.entrySet())
        {
            if (entry.getValue() instanceof Map)
            {
                sb.append(tabs).append(entry.getKey()).append(":").append(entry.getValue().getClass()).append("\n");
                sb.append(JSONtoFmModel.toStringBuffer((Map<String, Object>)entry.getValue(), indent+1));
            }
            else if (entry.getValue() instanceof List)
            {
                sb.append(tabs).append("[\n");
                List l = (List)entry.getValue();
                for (int i = 0; i < l.size(); i++)
                {
                    sb.append(tabs).append(l.get(i)).append(":").append((l.get(i) != null) ? l.get(i).getClass() : "null").append("\n");
                }
                sb.append(tabs).append("]\n");
            }
            else
            {
                sb.append(tabs).append(entry.getKey()).append(":").append(entry.getValue()).append(":").append((entry.getValue() != null ? entry.getValue().getClass() : "null")).append("\n");         
            }
        }
        
        return sb;
    }
}
