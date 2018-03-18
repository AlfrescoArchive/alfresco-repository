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

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import org.alfresco.repo.search.impl.lucene.LuceneQueryParserException;
import org.alfresco.util.json.JsonUtil;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpMethodParams;

public abstract class AbstractSolrQueryHTTPClient
{
    public static final int DEFAULT_SAVEPOST_BUFFER = 4096;
    
    protected JsonNode postQuery(HttpClient httpClient, String url, JsonNode body) throws IOException
    {
        PostMethod post = new PostMethod(url);
        String bodyString = body.toString();
        if (bodyString.length() > DEFAULT_SAVEPOST_BUFFER)
        {
            post.getParams().setBooleanParameter(HttpMethodParams.USE_EXPECT_CONTINUE, true);
        }
        StringRequestEntity requestEntity = new StringRequestEntity(bodyString, "application/json", "UTF-8");
        post.setRequestEntity(requestEntity);
        try
        {
            httpClient.executeMethod(post);
            if(post.getStatusCode() == HttpStatus.SC_MOVED_PERMANENTLY || post.getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY)
            {
                Header locationHeader = post.getResponseHeader("location");
                if (locationHeader != null)
                {
                    String redirectLocation = locationHeader.getValue();
                    post.setURI(new URI(redirectLocation, true));
                    httpClient.executeMethod(post);
                }
            }
            if (post.getStatusCode() != HttpServletResponse.SC_OK)
            {
                throw new LuceneQueryParserException("Request failed " + post.getStatusCode() + " " + url.toString());
            }

            return JsonUtil.getObjectMapper().readTree(post.getResponseBodyAsStream());
        }
        finally
        {
            post.releaseConnection();
        }
    }
}
