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
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.alfresco.service.cmr.search.SuggesterResult;
import org.alfresco.util.Pair;
import org.alfresco.util.ParameterCheck;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Jamal Kaabi-Mofrad
 * @since 5.0
 */
public class SolrSuggesterResult implements SuggesterResult
{
    private static final Log logger = LogFactory.getLog(SolrSuggesterResult.class);

    private Long numberFound;
    private List<Pair<String, Integer>> suggestions = new ArrayList<>();

    public SolrSuggesterResult()
    {
    }

    public SolrSuggesterResult(JsonNode jsonObject)
    {
        try
        {
            processJson(jsonObject);
        }
        catch (Exception e)
        {
            logger.info(e.getMessage());
        }
    }

    /**
     * Parses the json returned from the suggester
     * 
     * @param json the JSON object
     */
    @SuppressWarnings("rawtypes")
    protected void processJson(JsonNode json)
    {
        ParameterCheck.mandatory("json", json);

        if (logger.isDebugEnabled())
        {
            logger.debug("Suggester JSON response: " + json);
        }

        JsonNode suggest = json.get("suggest");
        for (Iterator<String> suggestIterator = suggest.fieldNames(); suggestIterator.hasNext(); /**/)
        {
            String dictionary = suggestIterator.next();

            JsonNode dictionaryJsonObject = suggest.get(dictionary);
            for (Iterator<String> dicIterator = dictionaryJsonObject.fieldNames(); dicIterator.hasNext(); /**/)
            {
                String termStr = dicIterator.next();

                JsonNode termJsonObject = dictionaryJsonObject.get(termStr);
                // number found
                this.numberFound = termJsonObject.get("numFound").longValue();

                // the suggested terms
                ArrayNode suggestion = (ArrayNode) termJsonObject.get("suggestions");
                for (int i = 0, length = suggestion.size(); i < length; i++)
                {
                    JsonNode data = suggestion.get(i);
                    this.suggestions.add(new Pair<String, Integer>(data.get("term").textValue(), data.get("weight").intValue()));
                }
            }
        }
    }

    @Override
    public long getNumberFound()
    {
        return this.numberFound;
    }

    @Override
    public List<Pair<String, Integer>> getSuggestions()
    {
        return this.suggestions;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder(250);
        builder.append("SolrSuggesterResult [numberFound=").append(this.numberFound).append(", suggestions=")
                    .append(this.suggestions).append("]");
        return builder.toString();
    }
}
