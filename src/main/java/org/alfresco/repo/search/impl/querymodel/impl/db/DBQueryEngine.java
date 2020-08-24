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
package org.alfresco.repo.search.impl.querymodel.impl.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.admin.patch.OptionalPatchApplicationCheckBootstrapBean;
import org.alfresco.repo.domain.node.Node;
import org.alfresco.repo.domain.node.NodeDAO;
import org.alfresco.repo.domain.qname.QNameDAO;
import org.alfresco.repo.search.impl.lucene.PagingLuceneResultSet;
import org.alfresco.repo.search.impl.querymodel.Argument;
import org.alfresco.repo.search.impl.querymodel.Constraint;
import org.alfresco.repo.search.impl.querymodel.Function;
import org.alfresco.repo.search.impl.querymodel.FunctionEvaluationContext;
import org.alfresco.repo.search.impl.querymodel.Query;
import org.alfresco.repo.search.impl.querymodel.QueryEngine;
import org.alfresco.repo.search.impl.querymodel.QueryEngineResults;
import org.alfresco.repo.search.impl.querymodel.QueryModelException;
import org.alfresco.repo.search.impl.querymodel.QueryModelFactory;
import org.alfresco.repo.search.impl.querymodel.QueryOptions;
import org.alfresco.repo.search.impl.querymodel.impl.BaseConjunction;
import org.alfresco.repo.tenant.TenantService;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.util.StopWatch;

/**
 * @author Andy
 */
public class DBQueryEngine implements QueryEngine
{
	private static final Log logger = LogFactory.getLog(DBQueryEngine.class);
	
    private static final String SELECT_BY_DYNAMIC_QUERY = "alfresco.metadata.query.select_byDynamicQuery";
    private static final String SELECT_BY_DYNAMIC_QUERY_FAST = "alfresco.metadata.query.select_byDynamicQueryFast";
    
    private SqlSessionTemplate template;

    private QNameDAO qnameDAO;
    
    private NodeDAO nodeDAO;

    private DictionaryService dictionaryService;

    private NamespaceService namespaceService;
    
    private NodeService nodeService;

    private TenantService tenantService;
    
    private OptionalPatchApplicationCheckBootstrapBean metadataIndexCheck2;
    
    private DBTableInfo denormTable = new DBTableInfo("alf_node_props_denorm");
    
    public void setMetadataIndexCheck2(OptionalPatchApplicationCheckBootstrapBean metadataIndexCheck2)
    {
        this.metadataIndexCheck2 = metadataIndexCheck2;
    }
    
    public void setTenantService(TenantService tenantService)
    {
        this.tenantService = tenantService;
    }

    public final void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate)
    {
        this.template = sqlSessionTemplate;
    }

    /**
     * @param qnameDAO
     *            the qnameDAO to set
     */
    public void setQnameDAO(QNameDAO qnameDAO)
    {
        this.qnameDAO = qnameDAO;
    }

    /**
     * @param dictionaryService
     *            the dictionaryService to set
     */
    public void setDictionaryService(DictionaryService dictionaryService)
    {
        this.dictionaryService = dictionaryService;
    }

    /**
     * @param namespaceService
     *            the namespaceService to set
     */
    public void setNamespaceService(NamespaceService namespaceService)
    {
        this.namespaceService = namespaceService;
    }
    
    /**
     * @param nodeService the nodeService to set
     */
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    /**
     * @param nodeDAO the nodeDAO to set
     */
    public void setNodeDAO(NodeDAO nodeDAO)
    {
        this.nodeDAO = nodeDAO;
    }
    
    /*
     * (non-Javadoc)
     * @see
     * org.alfresco.repo.search.impl.querymodel.QueryEngine#executeQuery(org.alfresco.repo.search.impl.querymodel.Query,
     * org.alfresco.repo.search.impl.querymodel.QueryOptions,
     * org.alfresco.repo.search.impl.querymodel.FunctionEvaluationContext)
     */
    @Override
    public QueryEngineResults executeQuery(Query query, QueryOptions options, FunctionEvaluationContext functionContext)
    {
    	try {
    		return doExecuteQuery(query, options, functionContext);
    	} catch (Throwable ex) {
        	ex.printStackTrace();
        	throw new RuntimeException(ex);
        }
    }

	private QueryEngineResults doExecuteQuery(Query query, QueryOptions options,
			FunctionEvaluationContext functionContext) {
		Set<String> selectorGroup = null;
        if (query.getSource() != null)
        {
            List<Set<String>> selectorGroups = query.getSource().getSelectorGroups(functionContext);

            if (selectorGroups.size() == 0)
            {
                throw new QueryModelException("No selectors");
            }

            if (selectorGroups.size() > 1)
            {
                throw new QueryModelException("Advanced join is not supported");
            }

            selectorGroup = selectorGroups.get(0);
        }
        
        DBQuery dbQuery = (DBQuery)query;
        
        if(options.getStores().size() > 1)
        {
            throw new QueryModelException("Multi-store queries are not supported");
        }
        
        // MT
        StoreRef storeRef = options.getStores().get(0);
        storeRef = storeRef != null ? tenantService.getName(storeRef) : null;

        Pair<Long, StoreRef> store = nodeDAO.getStore(storeRef);
        if(store == null)
        {
        	  throw new QueryModelException("Unknown store: "+storeRef);
        }
        dbQuery.setStoreId(store.getFirst());
        Pair<Long, QName> sysDeletedType = qnameDAO.getQName(ContentModel.TYPE_DELETED);
        if(sysDeletedType == null)
        {
            dbQuery.setSysDeletedType(-1L);
        }
        else
        {
            dbQuery.setSysDeletedType(sysDeletedType.getFirst());
        }
        
        Long sinceTxId = options.getSinceTxId();
        if (sinceTxId == null)
        {
            // By default, return search results for all transactions.
            sinceTxId = -1L;
        }
        dbQuery.setSinceTxId(sinceTxId);
        
        dbQuery.prepare(namespaceService, dictionaryService, qnameDAO, nodeDAO, tenantService, selectorGroup, null, functionContext, metadataIndexCheck2.getPatchApplied());
        
        StopWatch stopWatch = new StopWatch("db query");
        
        stopWatch.start();
        List<Node> nodes = selectNodes(options, dbQuery);
        stopWatch.stop();
        logger.error("Selected " + nodes.size() + " nodes in " + stopWatch.getLastTaskTimeMillis() + "ms");
        
        stopWatch.start();
        QueryEngineResults queryResults = createQueryResults(nodes, options);
        stopWatch.stop();
        logger.error("Selected query results in " + stopWatch.getLastTaskTimeMillis() + "ms");
        
		return queryResults;
	}

	private QueryEngineResults createQueryResults(List<Node> nodes, QueryOptions options) {
		LinkedHashSet<Long> set = new LinkedHashSet<Long>(nodes.size());
        for(Node node : nodes)
        {
            set.add(node.getId());
        }
        List<Long> nodeIds = new ArrayList<Long>(set);
        ResultSet rs =  new DBResultSet(options.getAsSearchParmeters(), nodeIds, nodeDAO, nodeService, tenantService, Integer.MAX_VALUE);
        ResultSet paged = new PagingLuceneResultSet(rs, options.getAsSearchParmeters(), nodeService);
        
        Map<Set<String>, ResultSet> answer = new HashMap<Set<String>, ResultSet>();
        HashSet<String> key = new HashSet<String>();
        key.add("");
        answer.put(key, paged);
        return new QueryEngineResults(answer);
	}

	private List<Node> selectNodes(QueryOptions options, DBQuery dbQuery) 
	{
		List<Node> nodes = new ArrayList<Node>();
        boolean forDenormalizedTable = dbQuery.isForDenormalizedTable(denormTable.getFieldNames(template));
        
		if(forDenormalizedTable) 
		{
			logger.info("Using the denormalized table");
        	nodes = template.selectList(SELECT_BY_DYNAMIC_QUERY_FAST, dbQuery);
        }
        else 
        {
        	logger.info("Using the standard table");
        	nodes = template.selectList(SELECT_BY_DYNAMIC_QUERY, dbQuery);
        }
		return nodes;
	}

    /*
     * (non-Javadoc)
     * @see org.alfresco.repo.search.impl.querymodel.QueryEngine#getQueryModelFactory()
     */
    @Override
    public QueryModelFactory getQueryModelFactory()
    {
        return new DBQueryModelFactory();
    }

}
