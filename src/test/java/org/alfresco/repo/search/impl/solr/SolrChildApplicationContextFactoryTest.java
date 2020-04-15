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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.search.impl.lucene.LuceneQueryParserException;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.context.ApplicationContext;

public class SolrChildApplicationContextFactoryTest
{
    @Spy @InjectMocks
    private SolrChildApplicationContextFactory solrContextFactory;

    @Mock
    private ApplicationContext context;
    @Mock
    private SolrAdminHTTPClient client;

    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        doReturn(context).when(solrContextFactory).getApplicationContext();
        doReturn(false).when(solrContextFactory).isSolrInactive();
        doReturn(client).when(context).getBean(eq("search.solrAdminHTTPCLient"), eq(SolrAdminHTTPClient.class));
    }

    @After
    public void tearDown()
    {
        // Sleep two seconds between tests to ensure the error cache is reset
        sleep(2 * 1000);
    }

    @Test
    public void testGetProperty_successfulFlow() throws JSONException
    {
        doReturn(new JSONObject("{\"Summary\": {\"alfresco\": {\"TX Lag\": \"abc123\"}}}"))
            .when(client).execute(any());

        final String result = solrContextFactory.getProperty("tracker.alfresco.lag");
        assertEquals("abc123", result);

        verify(client, times(1)).execute(any());
    }

    @Test
    public void testGetProperty_whenSolrClientThrowsIllegalArgumentException()
    {
        doThrow(new IllegalArgumentException("foo")).when(client).execute(any());

        final String result = solrContextFactory.getProperty("tracker.alfresco.lag");
        assertEquals("Unavailable: foo", result);

        verify(client, times(1)).execute(any());
    }

    @Test
    public void testGetProperty_whenSolrClientThrowsAlfrescoRuntimeExceptionWithIOException()
    {
        doThrow(new LuceneQueryParserException("bar", new IOException()))
            .when(client).execute(any());

        final String result = solrContextFactory.getProperty("tracker.alfresco.lag");
        assertTrue(result.startsWith("Unavailable: "));
        assertTrue(result.endsWith(" bar"));

        verify(client, times(1)).execute(any());
    }

    @Test
    public void testGetProperty_whenSolrClientThrowsTimeoutException()
    {
        doThrow(new AlfrescoRuntimeException("foobar", new TimeoutException()))
            .when(client).execute(any());

        final String result = solrContextFactory.getProperty("tracker.alfresco.lag");
        assertTrue(result.startsWith("Unavailable: "));
        assertTrue(result.endsWith(" foobar"));

        verify(client, times(1)).execute(any());
    }

    @Test
    public void testGetProperty_exceptionCacheGetsUsed()
    {
        doThrow(new AlfrescoRuntimeException("foobar", new TimeoutException()))
            .when(client).execute(any());

        Stream
            .of("tracker.alfresco.lag",
                "tracker.archive.lag",
                "tracker.archive.disk")
            .map(solrContextFactory::getProperty)
            .forEach(r -> assertTrue(r.startsWith("Unavailable: ") && r.endsWith(" foobar")));

        verify(client, times(1)).execute(any());
    }

    @Test
    public void testGetProperty_exceptionCacheExpires()
    {
        doThrow(new AlfrescoRuntimeException("foobar", new TimeoutException()))
            .when(client).execute(any());

        Stream
            .of("tracker.alfresco.lag",
                "tracker.archive.lag",
                "tracker.archive.disk")
            .peek(e -> sleep(2001))
            .map(solrContextFactory::getProperty)
            .forEach(r -> assertTrue(r.startsWith("Unavailable: ") && r.endsWith(" foobar")));

        verify(client, times(3)).execute(any());
    }

    private static void sleep(final long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch (InterruptedException ignore)
        {
        }
    }
}
