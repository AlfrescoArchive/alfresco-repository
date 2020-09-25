/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.admin.patch.OptionalPatchApplicationCheckBootstrapBean;
import org.alfresco.repo.dictionary.DictionaryComponent;
import org.alfresco.repo.dictionary.DictionaryNamespaceComponent;
import org.alfresco.repo.domain.node.NodeDAO;
import org.alfresco.repo.domain.qname.QNameDAO;
import org.alfresco.repo.node.db.DbNodeServiceImpl;
import org.alfresco.repo.search.impl.querymodel.Argument;
import org.alfresco.repo.search.impl.querymodel.Column;
import org.alfresco.repo.search.impl.querymodel.Constraint;
import org.alfresco.repo.search.impl.querymodel.FunctionEvaluationContext;
import org.alfresco.repo.search.impl.querymodel.Ordering;
import org.alfresco.repo.search.impl.querymodel.QueryOptions;
import org.alfresco.repo.search.impl.querymodel.Source;
import org.alfresco.repo.tenant.MultiTServiceImpl;
import org.alfresco.repo.tenant.TenantService;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mybatis.spring.SqlSessionTemplate;

public class DBQueryEngineTest
{
    private DBQueryEngine engine;
    private SqlSessionTemplate template;
    private NodeDAO nodeDAO;
    
    @Before
    public void setUp() 
    {        
        engine = createDBQueryEngine();
    }

    private DBQueryEngine createDBQueryEngine()
    {
        DBQueryEngine engine = new DBQueryEngine();
        template = mock(SqlSessionTemplate.class);
        nodeDAO = mock(NodeDAO.class);
        engine.setSqlSessionTemplate(template);
        engine.setQnameDAO(createQnameDao());
        engine.setNodeDAO(nodeDAO);
        engine.setDictionaryService(new DictionaryComponent());
        engine.setNamespaceService(new DictionaryNamespaceComponent());
        engine.setNodeService(new DbNodeServiceImpl());
        engine.setTenantService(new MultiTServiceImpl());
        engine.setMetadataIndexCheck2(new OptionalPatchApplicationCheckBootstrapBean());
        return engine;
    }
    
    @Test
    public void shouldMaxPermissionChecksBeEqualToMaxPermissionChecks() 
    {
        int maxPermissionChecks = 1000;
        engine.setMaxPermissionChecks(maxPermissionChecks);
        FunctionEvaluationContext functionContext = mock(FunctionEvaluationContext.class);  
        DBQuery query = spy(createDbQuery(functionContext));
        QueryOptions queryOptions = createQueryOptions();
        
        engine.executeQuery(query, queryOptions, functionContext);
        
        verify(query).setQueryLimit(maxPermissionChecks);
        ArgumentCaptor<DBQuery> captor = ArgumentCaptor.forClass(DBQuery.class);
        verify(template).selectList(any(String.class), captor.capture());
        assertEquals(maxPermissionChecks, captor.getValue().getQueryLimit());
    }

    private QNameDAO createQnameDao()
    {
        QNameDAO qnameDao = mock(QNameDAO.class);
        when(qnameDao.getQName(ContentModel.TYPE_DELETED)).thenReturn(new Pair<Long, QName>(0L, QName.createQName("qname")));
        return qnameDao;
    }
    
    private QueryOptions createQueryOptions()
    {
        StoreRef storeRef = createStoreRef();
        QueryOptions queryOptions = spy(new QueryOptions("select * from cmis:folder", storeRef));
        List<StoreRef> stores = Arrays.asList(storeRef);
        when(queryOptions.getStores()).thenReturn(stores);
        when(queryOptions.getSinceTxId()).thenReturn(0L);
        return queryOptions;
    }

    private StoreRef createStoreRef()
    {
        StoreRef storeRef = spy(new StoreRef("workspace://Test"));
        when(nodeDAO.getStore(any(StoreRef.class))).thenReturn(new Pair<Long, StoreRef>(0L, storeRef));
        return storeRef;
    }

    private DBQuery createDbQuery(FunctionEvaluationContext functionContext)
    {
        List<Column> columns = Arrays.asList(mock(Column.class));
        Source source = mock(Source.class);
        Constraint constraint = mock(Constraint.class);
        List<Ordering> orderings = Arrays.asList(mock(Ordering.class));
        DBQuery query = createDbQuery(columns, source, constraint, orderings);
        List<Set<String>> selectorGroups = Arrays.asList(new HashSet<String>());
        when(query.getSource().getSelectorGroups(functionContext))
            .thenReturn(selectorGroups);
        
        return query;
    }

    private DBQuery createDbQuery(List<Column> columns, Source source, Constraint constraint, List<Ordering> orderings)
    {
        return new DBQuery(columns, source, constraint, orderings)
        {
            @Override
            public void prepare(NamespaceService namespaceService, DictionaryService dictionaryService, QNameDAO qnameDAO, NodeDAO nodeDAO,
                    TenantService tenantService, Set<String> selectors, Map<String, Argument> functionArgs, FunctionEvaluationContext functionContext,
                    boolean supportBooleanFloatAndDouble)
            {
                // We don't need to execute this method in this test
            }
        };
    }
}
