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

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * JSON returned from SOLR API Parser
 * This class defines common properties and performs response header parsing.
 * An abstract method is provided for implementers to parse Core Information.
 *
 * @author aborroy
 * @since 6.2
 */
public abstract class AbstractJSONAPIResult implements JSONAPIResult
{
    
    private static final Log logger = LogFactory.getLog(AbstractJSONAPIResult.class);
    
    protected Long status; 
    protected Long queryTime;
    protected List<String> cores;
    protected Map<String, Map<String, Object>> coresInfo;
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.search.impl.lucene.JSONActionResult#getQueryTime()
     */
    public Long getQueryTime()
    {
        return queryTime;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.search.impl.lucene.JSONActionResult#getStatus()
     */
    public Long getStatus()
    {
        return status;
    }

    /* (non-Javadoc)
     * @see org.alfresco.repo.search.impl.lucene.JSONAPIResult#getCores()
     */
    @Override
    public List<String> getCores()
    {
        return cores;
    }
    
    /* (non-Javadoc)
     * @see org.alfresco.repo.search.impl.lucene.JSONAPIResult#getCoresInfo()
     */
    @Override
    public Map<String, Map<String, Object>> getCoresInfo() {
        return coresInfo;
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
        
        processCoresInfoJson(json);

    }
    
    /**
     * Parses the JSON to set this Java Object values related to Core Information
     * @param json JSONObject returned by SOLR API
     * @throws JSONException
     */
    protected abstract void processCoresInfoJson(JSONObject json) throws JSONException;

}
