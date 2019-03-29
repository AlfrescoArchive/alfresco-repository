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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The results of executing a SOLR STATUS action
 *
 * @author aborroy
 * @since 6.2
 */
public class SolrActionStatusResult implements JSONAPIResult
{
    private static final Log logger = LogFactory.getLog(SolrActionStatusResult.class);
    
    private Long status; 
    private Long queryTime;
    private List<String> cores;
    
    /**
     * Parses the JSON to set this Java Object values
     * @param json JSONObject returned by SOLR API
     */
    public SolrActionStatusResult(JSONObject json)
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
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.search.impl.lucene.JSONActionResult#getStatus()
     */
    public Long getStatus()
    {
        return this.status;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.search.impl.lucene.JSONActionResult#getQueryTime()
     */
    public Long getQueryTime()
    {
        return this.queryTime;
    }
    
    /**
     * Name of the cores managed by SOLR
     * @return A list with the names of the cores
     */
    public List<String> getCores() {
        return this.cores;
    }
    
    /**
     * Parses the JSON to set this Java Object values
     * @param json JSONObject returned by SOLR API
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
        
        cores = new ArrayList<>();
        
        // Partial deserialization just to find core names, many other properties are available in the response
        if (json.has("status")) 
        {
        
            JSONObject coreList = json.getJSONObject("status");
            JSONArray coreNameList = coreList.names();
            for(int i = 0; i < coreNameList.length(); i++)
            {
                JSONObject core = coreList.getJSONObject(String.valueOf(coreNameList.get(i)));
                cores.add(core.getString("name"));
            }

        }
        
    }
    
}
