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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.service.cmr.transfer.TransferException;
import org.alfresco.util.json.jackson.AlfrescoDefaultObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ExceptionJsonSerializer implements JsonSerializer<Throwable, String>
{
    private final static Log log = LogFactory.getLog(ExceptionJsonSerializer.class);

    @Override
    public Throwable deserialize(String errorString) throws IOException
    {
        if (errorString == null)
        {
            return null;
        }
        JsonNode errorJSON = AlfrescoDefaultObjectMapper.getReader().readTree(errorString);
        
        Throwable result = null;
        Object createdObject = null;

        //errorType and errorMessage should always be reported
        String errorType = errorJSON.has("errorType") ?
                errorJSON.get("errorType").textValue() : Exception.class.getName();
        String errorMessage = errorJSON.has("errorMessage") ?
                errorJSON.get("errorMessage").textValue() : "";

        //alfrescoErrorId and alfrescoErrorParams will only appear if the
        //throwable object was of a subclass of AlfrescoRuntimeException
        String errorId = errorJSON.has("alfrescoMessageId") ?
                errorJSON.get("alfrescoMessageId").asText() : null;
        Object[] errorParams = new Object[0];
        ArrayNode errorParamArray = errorJSON.has("alfrescoMessageParams") ?
                (ArrayNode) errorJSON.get("alfrescoMessageParams"): null;
        if (errorParamArray != null)
        {
            int length = errorParamArray.size();
            errorParams = new Object[length];
            for (int i = 0; i < length; ++i)
            {
                errorParams[i] = errorParamArray.get(i).asText();
            }
        }
        Class<?> errorClass;
        try
        {
            errorClass = Class.forName(errorType);
        }
        catch (ClassNotFoundException e)
        {
            errorClass = Exception.class;
        }
        Constructor<?> constructor = null;
        try
        {
            try
            {
                constructor = errorClass.getConstructor(String.class, Object[].class);
                createdObject = constructor.newInstance(errorId, errorParams);
            }
            catch (NoSuchMethodException e)
            {
                try
                {
                    constructor = errorClass.getConstructor(String.class);
                    createdObject = constructor.newInstance(errorId == null ? errorMessage : errorId);
                }
                catch (NoSuchMethodException e1)
                {
                    try
                    {
                        constructor = errorClass.getConstructor();
                        createdObject = constructor.newInstance();
                    }
                    catch (NoSuchMethodException e2)
                    {
                    }
                }
            }
        }
        catch(Exception ex)
        {
            //We don't need to do anything here. Code below will fix things up
        }
        if (createdObject == null || !Throwable.class.isAssignableFrom(createdObject.getClass()))
        {
            result = new TransferException(errorId == null ? errorMessage : errorId, errorParams);
        }
        else
        {
            result = (Throwable)createdObject;
        }
        return result;
    }

    @Override
    public String serialize(Throwable object) throws IOException
    {
        ObjectNode errorObject = AlfrescoDefaultObjectMapper.createObjectNode();

        errorObject.put("errorType", object.getClass().getName());
        errorObject.put("errorMessage", object.getMessage());
        if (AlfrescoRuntimeException.class.isAssignableFrom(object.getClass()))
        {
            AlfrescoRuntimeException alfEx = (AlfrescoRuntimeException)object;
            errorObject.put("alfrescoMessageId", alfEx.getMsgId());
            Object[] msgParams = alfEx.getMsgParams();
            List<Object> params = msgParams == null ? Collections.emptyList() : Arrays.asList(msgParams);
            errorObject.put("alfrescoMessageParams", AlfrescoDefaultObjectMapper.convertValue(params, JsonNode.class));
        }

        return errorObject.toString();
    }
}
