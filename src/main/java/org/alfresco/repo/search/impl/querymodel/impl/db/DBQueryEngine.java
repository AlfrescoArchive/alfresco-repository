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

import java.io.Serializable;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.admin.patch.OptionalPatchApplicationCheckBootstrapBean;
import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.repo.cache.lookup.EntityLookupCache;
import org.alfresco.repo.cache.lookup.EntityLookupCache.EntityLookupCallbackDAOAdaptor;
import org.alfresco.repo.domain.node.Node;
import org.alfresco.repo.domain.node.NodeDAO;
import org.alfresco.repo.domain.qname.QNameDAO;
import org.alfresco.repo.search.SimpleResultSetMetaData;
import org.alfresco.repo.search.impl.lucene.PagingLuceneResultSet;
import org.alfresco.repo.search.impl.querymodel.FunctionEvaluationContext;
import org.alfresco.repo.search.impl.querymodel.Query;
import org.alfresco.repo.search.impl.querymodel.QueryEngine;
import org.alfresco.repo.search.impl.querymodel.QueryEngineResults;
import org.alfresco.repo.search.impl.querymodel.QueryModelException;
import org.alfresco.repo.search.impl.querymodel.QueryModelFactory;
import org.alfresco.repo.search.impl.querymodel.QueryOptions;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.impl.acegi.FilteringResultSet;
import org.alfresco.repo.tenant.TenantService;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.LimitBy;
import org.alfresco.service.cmr.search.PermissionEvaluationMode;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.EqualsHelper;
import org.alfresco.util.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mybatis.spring.SqlSessionTemplate;

/**
 * @author Andy
 */
public class DBQueryEngine implements QueryEngine
{
    protected static final Log logger = LogFactory.getLog(DBQueryEngine.class);
    
    private static final String SELECT_BY_DYNAMIC_QUERY = "alfresco.metadata.query.select_byDynamicQuery";
    
    private SqlSessionTemplate template;

    private QNameDAO qnameDAO;
    
    private NodeDAO nodeDAO;

    private DictionaryService dictionaryService;

    private NamespaceService namespaceService;
    
    private NodeService nodeService;

    private TenantService tenantService;
    
    private OptionalPatchApplicationCheckBootstrapBean metadataIndexCheck2;
    
    private EntityLookupCache<Long, Node, NodeRef> nodesCache;

    private PermissionService permissionService;

    private OwnableService ownableService;

    
    public void setOwnableService(OwnableService ownableService)
    {
        this.ownableService = ownableService;
    }

    public void setTemplate(SqlSessionTemplate template)
    {
        this.template = template;
    }

    public void setNodesCache(EntityLookupCache<Long, Node, NodeRef> nodesCache)
    {
        this.nodesCache = nodesCache;
    }

    public void setPermissionService(PermissionService permissionService)
    {
        this.permissionService = permissionService;
    }

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
        
        List<Node> nodes = template.selectList(SELECT_BY_DYNAMIC_QUERY, dbQuery);
                
        DBResultSet rs =  new DBResultSet(options.getAsSearchParmeters(), nodes, nodeDAO, nodeService, tenantService, Integer.MAX_VALUE);
        
        FilteringResultSet frs = decidePermissions(rs);

//        FilteringResultSet frs = new FilteringResultSet(rs, BitSet.valueOf(new long[] {0xffffffffl}));
//        frs.setResultSetMetaData(new SimpleResultSetMetaData(LimitBy.UNLIMITED, PermissionEvaluationMode.EAGER, rs.getResultSetMetaData().getSearchParameters()));
 
        ResultSet paged = new PagingLuceneResultSet(frs, options.getAsSearchParmeters(), nodeService);

        return asQueryEngineResults(paged);
    }

    private QueryEngineResults asQueryEngineResults(ResultSet paged)
    {
        HashSet<String> key = new HashSet<String>();
        key.add("");
        Map<Set<String>, ResultSet> answer = new HashMap<Set<String>, ResultSet>();
        answer.put(key, paged);

        return new QueryEngineResults(answer);
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

    /* 
     * Injection of nodes cache for pre-population 
     */
    private static final String CACHE_REGION_NODES = "N.N";
    private class NodesCacheCallbackDAO extends EntityLookupCallbackDAOAdaptor<Long, Node, NodeRef>
    {
        /**
         * @throws UnsupportedOperationException        Nodes are created externally
         */
        public Pair<Long, Node> createValue(Node value)
        {
            throw new UnsupportedOperationException("Node creation is done externally: " + value);
        }

        /**
         * @param nodeId            the key node ID
         */
        public Pair<Long, Node> findByKey(Long nodeId)
        {
            return null;
        }

        /**
         * @return                  Returns the Node's NodeRef
         */
        @Override
        public NodeRef getValueKey(Node value)
        {
            return value.getNodeRef();
        }

        /**
         * Looks the node up based on the NodeRef of the given node
         */
        @Override
        public Pair<Long, Node> findByValue(Node node)
        {
                return null;
        }
    }
    public void setNodesCache(SimpleCache<Serializable, Serializable> cache)
    {
        this.nodesCache = new EntityLookupCache<Long, Node, NodeRef>(
                cache,
                CACHE_REGION_NODES,
                new NodesCacheCallbackDAO());
    }

    FilteringResultSet decidePermissions(DBResultSet resultSet)
    {
        if (resultSet == null)
        {
            return null;
        }

        int maxChecks = Integer.MAX_VALUE;
        if (resultSet.getResultSetMetaData().getSearchParameters().getMaxPermissionChecks() >= 0)
        {
            maxChecks = resultSet.getResultSetMetaData().getSearchParameters().getMaxPermissionChecks();
        }

        long maxCheckTime = Long.MAX_VALUE;
        if (resultSet.getResultSetMetaData().getSearchParameters().getMaxPermissionCheckTimeMillis() >= 0)
        {
            maxCheckTime = resultSet.getResultSetMetaData().getSearchParameters().getMaxPermissionCheckTimeMillis();
        }

        FilteringResultSet filteringResultSet = new FilteringResultSet(resultSet);

        // record the start time
        long startTimeMillis = System.currentTimeMillis();
        filteringResultSet.setResultSetMetaData(new SimpleResultSetMetaData(LimitBy.UNLIMITED, PermissionEvaluationMode.EAGER, resultSet.getResultSetMetaData()
                .getSearchParameters()));

        int includedCount = 0;
        int discardedNodes = 0;
        int numberOfRequiredItems = computeNumberOfRequiredItems(resultSet);

        for (int index = 0; index < resultSet.length() && includedCount < numberOfRequiredItems; index++)
        {
            long currentTimeMillis = System.currentTimeMillis();

            if (index >= maxChecks)
            {
                logger.warn("maxChecks exceeded (" + maxChecks + ")", new Exception("Back Trace"));
                filteringResultSet.setResultSetMetaData(new SimpleResultSetMetaData(LimitBy.NUMBER_OF_PERMISSION_EVALUATIONS,
                        PermissionEvaluationMode.EAGER, resultSet.getResultSetMetaData().getSearchParameters()));
                break;
            }
            else if ((currentTimeMillis - startTimeMillis) > maxCheckTime)
            {
                logger.warn("maxCheckTime exceeded (" + (currentTimeMillis - startTimeMillis) + " milliseconds)", new Exception("Back Trace"));
                filteringResultSet.setResultSetMetaData(new SimpleResultSetMetaData(LimitBy.NUMBER_OF_PERMISSION_EVALUATIONS,
                        PermissionEvaluationMode.EAGER, resultSet.getResultSetMetaData().getSearchParameters()));
                break;
            }

            boolean isIncluded = isIncluded(resultSet.getNode(index));
            filteringResultSet.setIncluded(index, isIncluded);
            if (isIncluded)
            {
                includedCount++;
            }
            else
            {
                discardedNodes++;
            }
        }
        
        if (logger.isDebugEnabled()) logger.debug("included="+includedCount+", excluded="+discardedNodes);

        return filteringResultSet;
    }

    private int computeNumberOfRequiredItems(ResultSet resultSet)
    {
        int numberOfRequiredItems = computeNumberOfRequiredItems(resultSet.getResultSetMetaData().getSearchParameters()) + 1;
        if (!resultSet.getResultSetMetaData().getSearchParameters().getLocales().isEmpty())
        {
            if (resultSet.getResultSetMetaData().getSearchParameters().getLocales().get(0).getLanguage().equals("xsl"))
            {
                numberOfRequiredItems = Integer.MAX_VALUE;
            }
        }
        return numberOfRequiredItems;
    }
    
    private Integer computeNumberOfRequiredItems(SearchParameters searchParameters)
    {
        Integer maxSize = Integer.MAX_VALUE;
        if (searchParameters.getMaxItems() >= 0)
        {
            maxSize = searchParameters.getMaxItems() + searchParameters.getSkipCount();
        }
        else if (searchParameters.getLimitBy() == LimitBy.FINAL_SIZE)
        {
            maxSize = searchParameters.getLimit() + searchParameters.getSkipCount();
        }

        return maxSize;
    }

    private boolean isIncluded(Node node)
    { 
        return  canRead(node.getAclId()) ||
                adminRead() ||
                ownerRead(node.getNodeRef());
    }
    
    private boolean ownerRead(NodeRef nodeRef)
    {
        String username = AuthenticationUtil.getRunAsUser();

        String owner = ownableService.getOwner(nodeRef);
        if(EqualsHelper.nullSafeEquals(username, owner))
        {
            return true;
        } 
        else 
        {
            return false;
        }
    }

    private boolean adminRead()
    {
        Set<String> authorisations = permissionService.getAuthorisations();
        return authorisations.contains(AuthenticationUtil.getAdminRoleName());
    }
    
    private boolean canRead(Long aclId)
    {
        Set<String> authorities = permissionService.getAuthorisations();

        Set<String> aclReadersDenied = permissionService.getReadersDenied(aclId);
        for(String auth : aclReadersDenied)
        {
            if(authorities.contains(auth))
            {
                return false;
            }
        }

        Set<String> aclReaders = permissionService.getReaders(aclId);
        for(String auth : aclReaders)
        {
            if(authorities.contains(auth))
            {
                return true;
            }
        }

        return false;
    }
    
    

}


