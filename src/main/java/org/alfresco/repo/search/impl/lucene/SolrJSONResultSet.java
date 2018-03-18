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
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.alfresco.repo.domain.node.NodeDAO;
import org.alfresco.repo.search.SimpleResultSetMetaData;
import org.alfresco.repo.search.impl.solr.facet.facetsresponse.GenericBucket;
import org.alfresco.repo.search.impl.solr.facet.facetsresponse.GenericFacetResponse;
import org.alfresco.repo.search.impl.solr.facet.facetsresponse.GenericFacetResponse.FACET_TYPE;
import org.alfresco.repo.search.impl.solr.facet.facetsresponse.ListMetric;
import org.alfresco.repo.search.impl.solr.facet.facetsresponse.Metric;
import org.alfresco.repo.search.impl.solr.facet.facetsresponse.Metric.METRIC_TYPE;
import org.alfresco.repo.search.impl.solr.facet.facetsresponse.PercentileMetric;
import org.alfresco.repo.search.impl.solr.facet.facetsresponse.RangeResultMapper;
import org.alfresco.repo.search.impl.solr.facet.facetsresponse.SimpleMetric;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.LimitBy;
import org.alfresco.service.cmr.search.PermissionEvaluationMode;
import org.alfresco.service.cmr.search.RangeParameters;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetMetaData;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SpellCheckResult;
import org.alfresco.util.Pair;
import org.alfresco.util.json.JsonUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Andy
 */
public class SolrJSONResultSet implements ResultSet, JSONResult
{
    private static final Log logger = LogFactory.getLog(SolrJSONResultSet.class);
    
    private NodeService nodeService;
    
    private ArrayList<Pair<Long, Float>> page;
    
    private ArrayList<NodeRef> refs;
    
    private ResultSetMetaData rsmd;
    
    private Long status;
    
    private Long queryTime;
    
    private Long numberFound;
    
    private Long start;
    
    private Float maxScore;

    private SimpleResultSetMetaData resultSetMetaData;
    
    private HashMap<String, List<Pair<String, Integer>>> fieldFacets = new HashMap<String, List<Pair<String, Integer>>>(1);
    
    private Map<String, Integer> facetQueries = new HashMap<String, Integer>();

    private Map<NodeRef, List<Pair<String, List<String>>>> highlighting = new HashMap<>();

    private Map<String, List<Pair<String, Integer>>> facetIntervals = new HashMap<String, List<Pair<String, Integer>>>(1);
    
    private Map<String,List<Map<String,String>>> facetRanges = new HashMap<String,List<Map<String,String>>>();

    private List<GenericFacetResponse> pivotFacets = new ArrayList<>();

    private Map<String, Set<Metric>> stats = new HashMap<>();

    private NodeDAO nodeDao;
    
    private long lastIndexedTxId;
    
    private SpellCheckResult spellCheckResult;
    
    private boolean processedDenies;
    
    /**
     * Detached result set based on that provided
     * @param json JsonNode
     * @param searchParameters SearchParameters
     * @param nodeService NodeService
     * @param nodeDao NodeDAO
     * @param limitBy LimitBy
     * @param maxResults int
     */
    public SolrJSONResultSet(JsonNode json, SearchParameters searchParameters, NodeService nodeService, NodeDAO nodeDao, LimitBy limitBy, int maxResults)
    {
        // Note all properties are returned as multi-valued from the WildcardField "*" definition in the SOLR schema.xml
        this.nodeService = nodeService;
        this.nodeDao = nodeDao;
        JsonNode responseHeader = json.get("responseHeader");
        status = responseHeader.get("status").longValue();
        queryTime = responseHeader.get("QTime").longValue();

        JsonNode response = json.get("response");
        numberFound = response.get("numFound").longValue();
        start = response.get("start").longValue();
        Double d = response.get("maxScore").doubleValue();
        maxScore = d.floatValue();
        if (json.has("lastIndexedTx"))
        {
            lastIndexedTxId = json.get("lastIndexedTx").longValue();
        }
        if (json.has("processedDenies"))
        {
            processedDenies = json.get("processedDenies").booleanValue();
        }
        ArrayNode docs = (ArrayNode) response.get("docs");

        int numDocs = docs.size();

        ArrayList<Long> rawDbids = new ArrayList<Long>(numDocs);
        ArrayList<Float> rawScores = new ArrayList<Float>(numDocs);
        for(int i = 0; i < numDocs; i++)
        {
            ObjectNode doc = (ObjectNode) docs.get(i);
            ArrayNode dbids = doc.has("DBID") ? (ArrayNode) doc.get("DBID") : null;
            if(dbids != null)
            {
                Long dbid = dbids.get(0).longValue();
                Float score = Float.valueOf((float)doc.get("score").doubleValue());
                rawDbids.add(dbid);
                rawScores.add(score);
            }
            else
            {
                Long dbid = doc.has("DBID") ? doc.get("DBID").longValue() : null;
                if(dbid != null)
                {
                    Float score = Float.valueOf((float)doc.get("score").doubleValue());
                    rawDbids.add(dbid);
                    rawScores.add(score);
                }
                else
                {
                    // No DBID found
                    throw new LuceneQueryParserException("No DBID found for doc ...");
                }
            }

        }

        // bulk load
        if (searchParameters.isBulkFetchEnabled())
        {
            nodeDao.cacheNodesById(rawDbids);
        }

        // filter out rubbish

        page = new ArrayList<Pair<Long, Float>>(numDocs);
        refs = new ArrayList<NodeRef>(numDocs);
        Map<Long,NodeRef> dbIdNodeRefs = new HashMap<>(numDocs);

        for(int i = 0; i < numDocs; i++)
        {
            Long dbid = rawDbids.get(i);
            NodeRef nodeRef = nodeService.getNodeRef(dbid);

            if(nodeRef != null)
            {
                page.add(new Pair<Long, Float>(dbid, rawScores.get(i)));
                refs.add(nodeRef);
                dbIdNodeRefs.put(dbid, nodeRef);
            }
        }

        //Process hightlight response
        if(json.has("highlighting"))
        {
            ObjectNode highObj = (ObjectNode) json.get("highlighting");
            for(Iterator<String> it = highObj.fieldNames(); it.hasNext(); /**/)
            {
                Long nodeKey = null;
                String aKey = it.next();
                ObjectNode high = (ObjectNode) highObj.get(aKey);
                List< Pair<String, List<String>> > highFields = new ArrayList<>(high.size());
                for(Iterator<Map.Entry<String,JsonNode>> hit = high.fields(); hit.hasNext(); /**/)
                {
                    String highKey = hit.next().getKey();
                    if ("DBID".equals(highKey))
                    {
                        nodeKey = high.get("DBID").longValue();
                    }
                    else
                    {
                        ArrayNode highVal = (ArrayNode) high.get(highKey);
                        List<String> highValues = new ArrayList<>(highVal.size());
                        for (int i = 0, length = highVal.size(); i < length; i++)
                        {
                            highValues.add(highVal.get(i).textValue());
                        }
                        Pair<String, List<String>> highPair = new Pair<String, List<String>>(highKey, highValues);
                        highFields.add(highPair);
                    }
                }
                NodeRef nodefRef = dbIdNodeRefs.get(nodeKey);
                if (nodefRef != null && !highFields.isEmpty())
                {
                    highlighting.put(nodefRef, highFields);
                }
            }
        }
        if(json.has("facet_counts"))
        {
            ObjectNode facet_counts = (ObjectNode) json.get("facet_counts");
            if(facet_counts.has("facet_queries"))
            {
                ObjectNode facet_queries = (ObjectNode) facet_counts.get("facet_queries");
                for(Iterator<Map.Entry<String,JsonNode>> it = facet_queries.fields(); it.hasNext(); /**/)
                {
                    String fq = it.next().getValue().textValue();
                    Integer count = Integer.valueOf(facet_queries.get(fq).intValue());
                    facetQueries.put(fq, count);
                }
            }
            if(facet_counts.has("facet_fields"))
            {
                JsonNode facet_fields = facet_counts.get("facet_fields");
                for(Iterator<String> it = facet_fields.fieldNames(); it.hasNext(); /**/)
                {
                    String fieldName = it.next();
                    ArrayNode facets = (ArrayNode) facet_fields.get(fieldName);
                    int facetArraySize = facets.size();
                    ArrayList<Pair<String, Integer>> facetValues = new ArrayList<Pair<String, Integer>>(facetArraySize/2);
                    for(int i = 0; i < facetArraySize; i+=2)
                    {
                        String facetEntryName = facets.get(i).textValue();
                        Integer facetEntryCount = Integer.valueOf(facets.get(i+1).intValue());
                        Pair<String, Integer> pair = new Pair<String, Integer>(facetEntryName, facetEntryCount);
                        facetValues.add(pair);
                    }
                    fieldFacets.put(fieldName, facetValues);
                }
            }
            if(facet_counts.has("facet_intervals"))
            {
                JsonNode facet_intervals = facet_counts.get("facet_intervals");
                for(Iterator<String> it = facet_intervals.fieldNames(); it.hasNext(); /**/)
                {
                    String fieldName = it.next();
                    JsonNode intervals = facet_intervals.get(fieldName);

                    ArrayList<Pair<String, Integer>> intervalValues = new ArrayList<Pair<String, Integer>>(intervals.size());
                    for(Iterator<String> itk = intervals.fieldNames(); itk.hasNext(); /**/)
                    {
                        String key = (String) itk.next();
                        Integer count = Integer.valueOf(intervals.get(key).intValue());
                        intervalValues.add(new Pair<String, Integer>(key, count));
                    }
                    facetIntervals.put(fieldName,intervalValues);
                }
            }
            if(facet_counts.has("facet_pivot"))
            {
                JsonNode facet_pivot = facet_counts.get("facet_pivot");
                for(Iterator<String> it = facet_pivot.fieldNames(); it.hasNext(); /**/)
                {
                    String pivotName = it.next();
                    pivotFacets.addAll(buildPivot(facet_pivot, pivotName, searchParameters.getRanges()));
                }
            }

            if(facet_counts.has("facet_ranges"))
            {
                JsonNode facet_ranges = facet_counts.get("facet_ranges");
                for(Iterator it = facet_ranges.fieldNames(); it.hasNext();)
                {
                    String fieldName = (String) it.next();
                    String end = facet_ranges.get(fieldName).get("end").asText();
                    ArrayNode rangeCollection = (ArrayNode) facet_ranges.get(fieldName).get("counts");
                    List<Map<String, String>> buckets = new ArrayList<Map<String, String>>();
                    for(int i = 0; i < rangeCollection.size(); i += 2)
                    {
                        String position = i == 0 ? "head":"body";
                        if( i+2 == rangeCollection.size())
                        {
                            position = "tail";
                        }
                        Map<String,String> rangeMap = new HashMap<String,String>(3);
                        String rangeFrom = rangeCollection.get(i).textValue();
                        String facetRangeCount = String.valueOf(rangeCollection.get(i+1).intValue());
                        String rangeTo = (i+2 < rangeCollection.size() ? rangeCollection.get(i+2).asText():end);
                        String label = rangeFrom + " - " + rangeTo;
                        rangeMap.put(GenericFacetResponse.LABEL, label);
                        rangeMap.put(GenericFacetResponse.COUNT, facetRangeCount);
                        rangeMap.put(GenericFacetResponse.START, rangeFrom);
                        rangeMap.put(GenericFacetResponse.END, rangeTo);
                        rangeMap.put("bucketPosition", position);
                        buckets.add(rangeMap);
                    }
                    facetRanges.put(fieldName, buckets);
                }
                Map<String, List<Map<String, String>>> builtRanges = buildRanges(facet_ranges);
                builtRanges.forEach((pKey, buckets) -> {
                    facetRanges.put(pKey, buckets);
                });
            }
        }

        if(json.has("stats"))
        {
            JsonNode statsObj = json.get("stats");
            Map<String, Map<String, JsonNode>> builtStats = buildStats(statsObj);
            builtStats.forEach((pKey, pVal) -> {
                stats.put(pKey, getMetrics(pVal));
            });
        }

        // process Spell check
        if (json.has("spellcheck"))
        {
            JsonNode spellCheckJson = json.get("spellcheck");
            List<String> list = new ArrayList<>(3);
            String flag = "";
            boolean searchedFor = false;
            if (spellCheckJson.has("searchInsteadFor"))
            {
                flag = "searchInsteadFor";
                searchedFor = true;
                list.add(spellCheckJson.get(flag).textValue());

            }
            else if (spellCheckJson.has("didYouMean"))
            {
                flag = "didYouMean";
                ArrayNode suggestions = (ArrayNode) spellCheckJson.get(flag);
                for (int i = 0, lenght = suggestions.size(); i < lenght; i++)
                {
                    list.add(suggestions.get(i).textValue());
                }
            }

            spellCheckResult = new SpellCheckResult(flag, list, searchedFor);

        }
        else
        {
            spellCheckResult = new SpellCheckResult(null, null, false);
        }
        // We'll say we were unlimited if we got a number less than the limit
        this.resultSetMetaData = new SimpleResultSetMetaData(
                maxResults > 0 && numberFound < maxResults ? LimitBy.UNLIMITED : limitBy,
                PermissionEvaluationMode.EAGER, searchParameters);
    }

    protected Map<String,List<Map<String,String>>> buildRanges(JsonNode facet_ranges)
    {
        Map<String,List<Map<String,String>>> ranges = new HashMap<>();

        for(Iterator it = facet_ranges.fieldNames(); it.hasNext();)
        {
            String fieldName = (String) it.next();
            String end = facet_ranges.get(fieldName).get("end").asText();
            ArrayNode rangeCollection = (ArrayNode) facet_ranges.get(fieldName).get("counts");
            List<Map<String, String>> buckets = new ArrayList<Map<String, String>>();
            for(int i = 0; i < rangeCollection.size(); i+=2)
            {
                String position = i == 0 ? "head":"body";
                if( i+2 == rangeCollection.size())
                {
                    position = "tail";
                }
                Map<String,String> rangeMap = new HashMap<String,String>(3);
                String rangeFrom = rangeCollection.get(i).textValue();
                int facetRangeCount = rangeCollection.get(i+1).intValue();
                String rangeTo = (i+2 < rangeCollection.size() ? rangeCollection.get(i+2).textValue():end);
                String label = rangeFrom + " - " + rangeTo;
                rangeMap.put(GenericFacetResponse.LABEL, label);
                rangeMap.put(GenericFacetResponse.COUNT, String.valueOf(facetRangeCount));
                rangeMap.put(GenericFacetResponse.START, rangeFrom);
                rangeMap.put(GenericFacetResponse.END, rangeTo);
                rangeMap.put("bucketPosition", position);
                buckets.add(rangeMap);
            }
            ranges.put(fieldName, buckets);
        }

        return ranges;
    }

    protected Map<String, Map<String, JsonNode>> buildStats(JsonNode statsObj)
    {
        if(statsObj.has("stats_fields"))
        {
            Map<String, Map<String, JsonNode>> statsMap = new HashMap<>();
            JsonNode statsFields = statsObj.get("stats_fields");
            for(Iterator<String> itk = statsFields.fieldNames(); itk.hasNext(); /**/)
            {
                String fieldName = itk.next();
                JsonNode theStats = statsFields.get(fieldName);
                Map<String, JsonNode> fieldStats = new HashMap<>(statsFields.size());
                for(Iterator<String> it = theStats.fieldNames(); it.hasNext(); /**/)
                {
                    String key = it.next();
                    JsonNode val = theStats.get(key);
                    if ("count".equals(key)) key = METRIC_TYPE.countValues.toString();
                    fieldStats.put(key, val);
                }
                statsMap.put(fieldName, fieldStats);
            }
            return statsMap;
        }
        return Collections.emptyMap();
    }

    protected List<GenericFacetResponse> buildPivot(JsonNode facet_pivot, String pivotName, List<RangeParameters> rangeParameters)
    {
        if (!facet_pivot.has(pivotName)) return Collections.emptyList();

        ArrayNode pivots = (ArrayNode) facet_pivot.get(pivotName);
        Map<String,List<GenericBucket>> pivotBuckets = new HashMap<>(pivots.size());
        List<GenericFacetResponse> facetResponses = new ArrayList<>();
        for(int i = 0; i < pivots.size(); i++)
        {
            JsonNode piv = pivots.get(i);
            Set<Metric> metrics = new HashSet<>(1);
            List<GenericFacetResponse> nested = new ArrayList<>();
            String field = piv.get("field").textValue();
            String value = piv.get("value").textValue();
            if (piv.has("stats"))
            {
                JsonNode stats = piv.get("stats");
                Map<String, Map<String, JsonNode>> pivotStats = buildStats(stats);
                pivotStats.forEach((pKey, pVal) -> {
                   metrics.addAll(getMetrics(pVal));
                });
            }

            Integer count = Integer.valueOf(piv.get("count").intValue());
            metrics.add(new SimpleMetric(METRIC_TYPE.count,count));
            nested.addAll(buildPivot(piv, "pivot", rangeParameters));

            if (piv.has("ranges"))
            {
                JsonNode ranges = piv.get("ranges");
                Map<String, List<Map<String, String>>> builtRanges = buildRanges(ranges);
                List<GenericFacetResponse> rangefacets = RangeResultMapper.getGenericFacetsForRanges(builtRanges,rangeParameters);
                nested.addAll(rangefacets);
            }

            GenericBucket buck = new GenericBucket(value, field+":\""+value+"\"", null, metrics, nested);
            List<GenericBucket> listBucks = pivotBuckets.containsKey(field)?pivotBuckets.get(field):new ArrayList<>();
            listBucks.add(buck);
            pivotBuckets.put(field, listBucks);
        }

        for (Map.Entry<String, List<GenericBucket>> entry : pivotBuckets.entrySet()) {
            facetResponses.add(new GenericFacetResponse(FACET_TYPE.pivot,entry.getKey(),entry.getValue()));
        }

        if (!facetResponses.isEmpty()) return facetResponses;

        return Collections.emptyList();
    }

    protected Set<Metric> getMetrics(Map<String, JsonNode> metrics)
    {
        if(metrics != null && !metrics.isEmpty())
        {
            return metrics.entrySet().stream().map(aStat -> {
                METRIC_TYPE metricType = METRIC_TYPE.valueOf(aStat.getKey());
                JsonNode val = aStat.getValue();
                if (val.isNull()) return null;

                switch (metricType)
                {
                    case distinctValues:
                        return new ListMetric(metricType, val);
                    case percentiles:
                        return new PercentileMetric(metricType, val);
                    case facets:
                    	return null;
                    case mean:
                        if ("NaN".equals(String.valueOf(val))) return null; //else fall through
                    default:
                        return new SimpleMetric(metricType, val);
                }
            }).filter(Objects::nonNull).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    public NodeService getNodeService()
    {
        return nodeService;
    }


    /*
     * (non-Javadoc)
     * @see org.alfresco.service.cmr.search.ResultSetSPI#close()
     */
    @Override
    public void close()
    {
        // NO OP
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.service.cmr.search.ResultSetSPI#getBulkFetch()
     */
    @Override
    public boolean getBulkFetch()
    {
        return true;
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.service.cmr.search.ResultSetSPI#getBulkFetchSize()
     */
    @Override
    public int getBulkFetchSize()
    {
        return Integer.MAX_VALUE;
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.service.cmr.search.ResultSetSPI#getChildAssocRef(int)
     */
    @Override
    public ChildAssociationRef getChildAssocRef(int n)
    {
        ChildAssociationRef primaryParentAssoc = nodeService.getPrimaryParent(getNodeRef(n));
        if(primaryParentAssoc != null)
        {
            return primaryParentAssoc;
        }
        else
        {
            return null;
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.alfresco.service.cmr.search.ResultSetSPI#getChildAssocRefs()
     */
    @Override
    public List<ChildAssociationRef> getChildAssocRefs()
    {
        ArrayList<ChildAssociationRef> refs = new ArrayList<ChildAssociationRef>(page.size());
        for(int i = 0; i < page.size(); i++ )
        {
            refs.add( getChildAssocRef(i));
        }
        return refs;
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.service.cmr.search.ResultSetSPI#getNodeRef(int)
     */
    @Override
    public NodeRef getNodeRef(int n)
    {
        return refs.get(n);
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.service.cmr.search.ResultSetSPI#getNodeRefs()
     */
    @Override
    public List<NodeRef> getNodeRefs()
    {
        return Collections.unmodifiableList(refs);
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.service.cmr.search.ResultSetSPI#getResultSetMetaData()
     */
    @Override
    public ResultSetMetaData getResultSetMetaData()
    {
        return resultSetMetaData;
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.service.cmr.search.ResultSetSPI#getRow(int)
     */
    @Override
    public ResultSetRow getRow(int i)
    {
       return new SolrJSONResultSetRow(this, i);
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.service.cmr.search.ResultSetSPI#getScore(int)
     */
    @Override
    public float getScore(int n)
    {
        return page.get(n).getSecond();
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.service.cmr.search.ResultSetSPI#getStart()
     */
    @Override
    public int getStart()
    {
        return start.intValue();
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.service.cmr.search.ResultSetSPI#hasMore()
     */
    @Override
    public boolean hasMore()
    {
       return numberFound.longValue() > (start.longValue() + page.size());
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.service.cmr.search.ResultSetSPI#length()
     */
    @Override
    public int length()
    {
       return page.size();
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.service.cmr.search.ResultSetSPI#setBulkFetch(boolean)
     */
    @Override
    public boolean setBulkFetch(boolean bulkFetch)
    {
         return bulkFetch;
    }

    /*
     * (non-Javadoc)
     * @see org.alfresco.service.cmr.search.ResultSetSPI#setBulkFetchSize(int)
     */
    @Override
    public int setBulkFetchSize(int bulkFetchSize)
    {
        return bulkFetchSize;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<ResultSetRow> iterator()
    {
        return new SolrJSONResultSetRowIterator(this);
    }


    /**
     * @return the queryTime
     */
    public Long getQueryTime()
    {
        return queryTime;
    }


    /**
     * @return the numberFound
     */
    public long getNumberFound()
    {
        return numberFound.longValue();
    }

    @Override
    public List<Pair<String, Integer>> getFieldFacet(String field)
    {
        List<Pair<String, Integer>> answer = fieldFacets.get(field);
        if(answer != null)
        {
            return answer;
        }
        else
        {
            return Collections.<Pair<String, Integer>>emptyList();
        }
    }

    public Map<String, List<Pair<String, Integer>>> getFieldFacets()
    {
        return Collections.unmodifiableMap(fieldFacets);
    }

    public Map<String, List<Pair<String, Integer>>> getFacetIntervals()
    {
        return Collections.unmodifiableMap(facetIntervals);
    }

    public List<GenericFacetResponse> getPivotFacets()
    {
        return pivotFacets;
    }

    public Map<String, Set<Metric>> getStats()
    {
        return Collections.unmodifiableMap(stats);
    }

    public long getLastIndexedTxId()
    {
        return lastIndexedTxId;
    }

    @Override
    public Map<String, Integer> getFacetQueries()
    {
        return Collections.unmodifiableMap(facetQueries);
    }

    @Override
    public Map<NodeRef, List<Pair<String, List<String>>>> getHighlighting()
    {
        return Collections.unmodifiableMap(highlighting);
    }

    @Override
    public SpellCheckResult getSpellCheckResult()
    {
        return this.spellCheckResult;
    }

    public boolean getProcessedDenies()
    {
        return processedDenies;
    }

    public Map<String,List<Map<String,String>>> getFacetRanges()
    {
        return facetRanges;
    }
    
}
