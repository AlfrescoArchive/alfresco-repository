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
package org.alfresco.repo.search.impl.solr;

import static java.util.Collections.emptyMap;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.apache.commons.httpclient.HttpStatus.SC_OK;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.alfresco.error.AlfrescoRuntimeException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.tika.io.IOUtils;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.internal.stubbing.answers.AnswersWithDelay;
import org.mockito.internal.stubbing.answers.Returns;
import org.springframework.test.util.ReflectionTestUtils;

public class SolrAdminHTTPClientTest
{
    @Mock
    private HttpClient httpClient;
    @Spy @InjectMocks
    private SolrAdminHTTPClient client;

    @Before
    public void setup() throws IOException
    {
        MockitoAnnotations.initMocks(this);
        //ReflectionTestUtils.setField(client, "httpClient", httpClient);
        ReflectionTestUtils.setField(client, "threadPoolExecutor", newSingleThreadExecutor());
        ReflectionTestUtils.setField(client, "baseUrl", "baseurl");
        ReflectionTestUtils.setField(client, "adminUrl", "baseurl/admin");
        ReflectionTestUtils.setField(client, "solrConnectTimeout", 1000); // 1 sec

        final GetMethod get = mock(GetMethod.class);
        doReturn(get).when(client).getMethod(anyString());
        doReturn(IOUtils.toInputStream("{}")).when(get).getResponseBodyAsStream();
        doReturn("UTF-8").when(get).getResponseCharSet();
    }

    @Test
    public void testFlowWhenSolrReachable() throws IOException
    {
        doReturn(SC_OK).when(httpClient).executeMethod(any());

        final JSONObject result = client.execute(emptyMap());
        assertNotNull(result);
    }

    @Test
    public void testFlowWhenSolrUnreachable() throws IOException
    {
        doAnswer(new AnswersWithDelay(2 * 1000, new Returns(SC_OK)))
            .when(httpClient).executeMethod(any());

        try
        {
            client.execute(emptyMap());
            fail("Expected Timeout exception");
        }
        catch (AlfrescoRuntimeException e)
        {
            assertTrue(e.getCause() instanceof TimeoutException);
        }
    }
}
