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

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Arrays;

import junit.framework.TestCase;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.security.permissions.AccessDeniedException;


public class ExceptionJsonSerializerTest extends TestCase
{
    
    private ExceptionJsonSerializer serializer;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        serializer = new ExceptionJsonSerializer();
    }

    public void testIllegalArgumentException() throws IOException
    {
        Exception expectedException = new IllegalArgumentException("This is the message");
        String obj = serializer.serialize(expectedException);
        Throwable actualException = serializer.deserialize(obj);
        assertEquals(expectedException.getClass(), actualException.getClass());
        assertEquals(expectedException.getMessage(), actualException.getMessage());
    }

    public void testAlfrescoRuntimeExceptionWithNoParams() throws IOException
    {
        AlfrescoRuntimeException expectedException = new AlfrescoRuntimeException("message id");
        String obj = serializer.serialize(expectedException);
        Throwable actualException = serializer.deserialize(obj);
        assertEquals(expectedException.getClass(), actualException.getClass());
        assertEquals(expectedException.getMsgId(), ((AlfrescoRuntimeException)actualException).getMsgId());
        assertTrue(((AlfrescoRuntimeException)actualException).getMsgParams().length == 0);
    }

    public void testAlfrescoRuntimeExceptionWithParams() throws IOException
    {
        AlfrescoRuntimeException expectedException = new AlfrescoRuntimeException("message id", 
                new Object[]{"one","two","three"});
        String obj = serializer.serialize(expectedException);
        Throwable actualException = serializer.deserialize(obj);
        assertEquals(expectedException.getClass(), actualException.getClass());
        assertEquals(expectedException.getMsgId(), ((AlfrescoRuntimeException)actualException).getMsgId());
        assertTrue(Arrays.deepEquals(expectedException.getMsgParams(), 
                ((AlfrescoRuntimeException)actualException).getMsgParams()));
    }

    public void testAccessDeniedException() throws IOException
    {
        AccessDeniedException expectedException = new AccessDeniedException("message id");
        String obj = serializer.serialize(expectedException);
        Throwable actualException = serializer.deserialize(obj);
        assertEquals(expectedException.getClass(), actualException.getClass());
        assertEquals(expectedException.getMsgId(), ((AlfrescoRuntimeException)actualException).getMsgId());
        assertTrue(expectedException.getMsgParams() == null);
    }
}
