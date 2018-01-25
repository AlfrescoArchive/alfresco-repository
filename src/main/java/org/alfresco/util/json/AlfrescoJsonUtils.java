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

package org.alfresco.util.json;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;

public class AlfrescoJsonUtils
{
    private final static Log log = LogFactory.getLog(AlfrescoJsonUtils.class);

    /**
     * Simulates what previous versions( like 20090211) of the json lib did in terms of getting the data stored as a string
     * Try to avoid using this method. It is here only to keep the compatibility with the old code that used the old implementation
     * of JSONObject.getString()
     *
     * @return the string interpretation of the value stored for that key. It never returns null
     * @throws JSONException        in case the value stored in the map is not found or that value is null. Also if "key" is null.
     * @throws NullPointerException if jsonObject is null
     * @deprecated - use the standard JSONObject.get* methods - or the getStringOrNull or getObjectOrNull below if you don't care about
     * exceptions and are ok with getting back null whenever proper values are not present
     */
    @Deprecated static public String getString(JSONObject jsonObject, String key) throws JSONException
    {
        Object obj = jsonObject.get(key);
        return obj.toString();
    }

    static public String getStringOrNull(JSONObject jsonObject, String key)
    {
        Object obj = getObjectOrNull(jsonObject, key);
        return (obj != null) ? obj.toString() : null;
    }

    static public Object getObjectOrNull(JSONObject jsonObject, String key)
    {
        if (jsonObject == null || key == null)
        {
            return null;
        }
        Object value = null;
        try
        {
            value = jsonObject.get(key);
        }
        catch (JSONException e)
        {
            // we don't care
            if (log.isDebugEnabled())
            {
                log.debug(e.getMessage(), e);
            }
        }
        return value;
    }
}
