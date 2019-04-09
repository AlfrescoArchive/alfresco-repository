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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The results of executing a SOLR ACL REPORT action
 *
 * @author aborroy
 * @since 6.2
 */
public class SolrActionAclReportResult extends AbstractJSONAPIResult
{
    private static final Log logger = LogFactory.getLog(SolrActionAclReportResult.class);
    
    /**
     * Parses the JSON to set this Java Object values
     * @param json JSONObject returned by SOLR API
     */
    public SolrActionAclReportResult(JSONObject json)
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
     * @see org.alfresco.repo.search.impl.lucene.AbstractSolrActionAPIResult#processCoresInfoJson(org.json.JSONObject)
     */
    @Override
    protected void processCoresInfoJson(JSONObject json) throws JSONException
    {

        cores = new ArrayList<>();
        coresInfo = new HashMap<>();
        
        if (json.has("report")) 
        {
        
            JSONObject coreList = json.getJSONObject("report");
            JSONArray coreNameList = coreList.names();
            for(int i = 0; i < coreNameList.length(); i++)
            {
                
                String coreName = String.valueOf(coreNameList.get(i));
                JSONObject core = coreList.getJSONObject(coreName);
                cores.add(coreName);
                
                Map<String, Object> coreInfo = new HashMap<>();
                JSONArray nodesPropertyNameList = core.names();
                for (int j = 0; j < nodesPropertyNameList.length(); j++)
                {
                    String propertyName = String.valueOf(nodesPropertyNameList.get(j));
                    Object propertyValue = core.get(propertyName);
                    if (propertyValue != JSONObject.NULL)
                    {
                        // MBeans Objects are defined as Long types, so we need casting to provide the expected type
                        if (propertyValue instanceof Integer)
                        {
                            propertyValue = Long.valueOf(propertyValue.toString());
                        }
                        coreInfo.put(propertyName, propertyValue);
                    }
                }
                coresInfo.put(coreName, coreInfo);
                
            }

        }
        
    }
    
}
