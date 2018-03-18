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

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.alfresco.service.cmr.search.StatsResultSet;
import org.alfresco.service.cmr.search.StatsResultStat;
import org.alfresco.util.json.JsonUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

/**
 * The results of executing a solr stats query 
 *
 * @author Gethin James
 * @since 5.0
 */
public class SolrStatsResult implements JSONResult, StatsResultSet
{
    private static final Log logger = LogFactory.getLog(SolrStatsResult.class);
    
    private Long status; 
    private Long queryTime;
    private Long numberFound;
    
    //Summary stats
    private Long sum;
    private Long max;
    private Long mean;
    
    private List<StatsResultStat> stats;
    private boolean nameIsADate;
    
    public SolrStatsResult(JsonNode json, boolean nameIsADate)
    {
        try 
        {
            this.nameIsADate = nameIsADate;
            stats = new ArrayList<>();
            processJson(json);
        }
        catch (NullPointerException e)
        {
           logger.info(e.getMessage());
        }
    }
    
    /**
     * Parses the json
     */
    private void processJson(JsonNode json)
    {
        JsonNode responseHeader = json.get("responseHeader");
        status = responseHeader.get("status").longValue();
        queryTime = responseHeader.get("QTime").longValue();
        
        JsonNode response = json.get("response");
        numberFound = response.get("numFound").longValue();
        
        if (logger.isDebugEnabled())
        {
            logger.debug("JSON response: "+json);
        }
        
        if(json.has("stats"))
        {
            JsonNode statsObj = json.get("stats");
            if(statsObj.has("stats_fields"))
            {
                JsonNode statsFields = statsObj.get("stats_fields");
                Iterator<String> fieldNames = statsFields.fieldNames();
                if (fieldNames.hasNext())
                {
                    String firstField = fieldNames.next();
                    // should have only one object
                    if (!fieldNames.hasNext())
                    {
                        JsonNode contentsize = statsFields.get(firstField);

                        sum = contentsize.get("sum").longValue();
                        max = contentsize.get("max").longValue();
                        mean = contentsize.get("mean").longValue();

                        if (contentsize.has("facets"))
                        {
                            JsonNode facets = contentsize.get("facets");
                            Iterator<String> facetFieldsIterator = facets.fieldNames();
                            while (facetFieldsIterator.hasNext())
                            {
                                JsonNode facetType = facets.get(facetFieldsIterator.next());
                                Iterator<String> facetValuesIterator = facetType.fieldNames();
                                while (facetValuesIterator.hasNext())
                                {
                                    String name = facetValuesIterator.next();
                                    JsonNode facetVal = facetType.get(name);
                                    stats.add(processStat(name, facetVal));
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Processes an individual stat entry
     */
    private StatsResultStat processStat(String name, JsonNode facetVal)
    {
        return new StatsResultStat(nameIsADate?formatAsDate(name):name,
                    facetVal.get("sum").longValue(),
                    facetVal.get("count").longValue(),
                    facetVal.get("min").longValue(),
                    facetVal.get("max").longValue(),
                    facetVal.get("mean").longValue());
    }

    public static String formatAsDate(String name)
    {
        if (StringUtils.hasText(name))
        {
            try
            {
                //LocalDate d = LocalDate.parse(name);
                //return d.toString();
                return name.substring(0,10);
            }
            catch (IllegalArgumentException iae)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Can't parse reponse: "+iae.getMessage());
                }
            }
        }

        //Default
        return "";
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("SolrStatsResult [status=").append(this.status).append(", queryTime=")
                    .append(this.queryTime).append(", numberFound=").append(this.numberFound)
                    .append(", sum=").append(this.sum).append(", max=").append(this.max)
                    .append(", mean=").append(this.mean).append(", stats=").append(this.stats)
                    .append("]");
        return builder.toString();
    }
    
    public Long getStatus()
    {
        return this.status;
    }
    public Long getQueryTime()
    {
        return this.queryTime;
    }
    
    @Override
    public long getNumberFound()
    {
        return this.numberFound;
    }

    @Override
    public Long getSum()
    {
        return this.sum;
    }

    @Override
    public Long getMax()
    {
        return this.max;
    }

    @Override
    public Long getMean()
    {
        return this.mean;
    }

    @Override
    public List<StatsResultStat> getStats()
    {
        return this.stats;
    }
}
