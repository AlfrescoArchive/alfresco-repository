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
package org.alfresco.repo.search.impl.lucene;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The results of executing a SOLR BACKUP command
 *
 * @author aborroy
 * @since 6.2
 */
public class SolrCommandBackupResult implements JSONAPIResult
{
    private static final Log logger = LogFactory.getLog(SolrCommandBackupResult.class);
    
    private Long status; 
    private Long queryTime;
    
    /**
     * Parses the JSON to create a new result object
     * @param json JSONObject returned by SOLR API
     */
    public SolrCommandBackupResult(JSONObject json)
    {
        try 
        {
            processJson(json);
        }
        catch (NullPointerException | JSONException e)
        {
           logger.info(e.getMessage());
        }
    }
    
    /**
     * Parses the json
     * @param json JSONObject
     * @throws JSONException
     */
    protected void processJson(JSONObject json) throws JSONException
    {

        if (logger.isDebugEnabled())
        {
            logger.debug("JSON response: "+json);
        }
        
        JSONObject responseHeader = json.getJSONObject("responseHeader");
        status = responseHeader.getLong("status");
        queryTime = responseHeader.getLong("QTime");
        
        // No other property is deserialized, but many other properties are available in the response  
        
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.search.impl.lucene.JSONAPIResult#getStatus()
     */
    public Long getStatus()
    {
        return this.status;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.search.impl.lucene.JSONAPIResult#getQueryTime()
     */
    public Long getQueryTime()
    {
        return this.queryTime;
    }
    
}
