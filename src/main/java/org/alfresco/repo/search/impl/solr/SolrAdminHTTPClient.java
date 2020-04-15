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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.alfresco.httpclient.HttpClientFactory;
import org.alfresco.repo.search.impl.lucene.LuceneQueryParserException;
import org.alfresco.util.ParameterCheck;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.json.JSONObject;

/**
 * @author Andy
 */
public class SolrAdminHTTPClient extends AbstractSolrAdminHTTPClient
{
    private String adminUrl;

    private String baseUrl;

    private HttpClient httpClient;
    private HttpClientFactory httpClientFactory;

    public SolrAdminHTTPClient()
    {
    }

    public void setBaseUrl(String baseUrl)
    {
        this.baseUrl = baseUrl;
    }

    public void init()
    {
        ParameterCheck.mandatory("baseUrl", baseUrl);

        adminUrl = baseUrl + "/admin/cores";

        httpClient = httpClientFactory.getHttpClient();
        HttpClientParams params = httpClient.getParams();
        params.setBooleanParameter(HttpClientParams.PREEMPTIVE_AUTHENTICATION, true);
        httpClient.getState().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
            new UsernamePasswordCredentials("admin", "admin"));
    }

    public void setHttpClientFactory(HttpClientFactory httpClientFactory)
    {
        this.httpClientFactory = httpClientFactory;
    }

    public JSONObject execute(Map<String, String> args)
    {
        return execute(adminUrl, args);
    }

    public JSONObject execute(String relativeHandlerPath, Map<String, String> args)
    {
        ParameterCheck.mandatory("relativeHandlerPath", relativeHandlerPath);
        ParameterCheck.mandatory("args", args);

        final String path = getPath(relativeHandlerPath);
        try
        {
            return getOperation(httpClient, buildUrl(path, args));
        }
        catch (IOException e)
        {
            throw new LuceneQueryParserException("", e);
        }
    }

    private String getPath(String path)
    {
        if (path.startsWith(baseUrl))
        {
            return path;
        }
        if (path.startsWith("/"))
        {
            return baseUrl + path;
        }
        return baseUrl + '/' + path;
    }

    private static String buildUrl(final String path, final Map<String, String> args)
        throws UnsupportedEncodingException
    {
        final URLCodec encoder = new URLCodec();
        final StringBuilder url = new StringBuilder();

        for (String key : args.keySet())
        {
            String value = args.get(key);
            if (url.length() == 0)
            {
                url.append(path)
                   .append("?")
                   .append(encoder.encode(key, "UTF-8"))
                   .append("=")
                   .append(encoder.encode(value, "UTF-8"));
            }
            else
            {
                url.append("&")
                   .append(encoder.encode(key, "UTF-8"))
                   .append("=")
                   .append(encoder.encode(value, "UTF-8"));
            }
        }
        return url.toString();
    }
}
