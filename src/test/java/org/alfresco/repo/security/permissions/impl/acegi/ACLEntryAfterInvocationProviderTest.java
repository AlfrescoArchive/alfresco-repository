package org.alfresco.repo.security.permissions.impl.acegi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetMetaData;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.junit.Before;
import org.junit.Test;

public class ACLEntryAfterInvocationProviderTest
{
    private final static int RESULT_SET_MAX_PERMISSION_CHECKS = Integer.MAX_VALUE;
    private final static long RESULT_SET_MAX_PERMISSION_CHECK_TIME_MILLIS = Long.MAX_VALUE;
    private final static int MAX_ITEMS = 100;
    
    private ACLEntryAfterInvocationProvider provider;

    private PermissionService permissionService;

    @Before
    public void init() throws Exception
    {
        provider = new ACLEntryAfterInvocationProvider();
        permissionService = mock(PermissionService.class);
        provider.setPermissionService(permissionService);
    }

    @Test
    public void shouldDecidePermissionReturnNULLWhenResultSetIsNULL()
    {
        ResultSet resultSet = null;

        ResultSet result = provider.decidePermissions(resultSet, null);

        assertEquals(null, result);
    }

    @Test
    public void shouldDecidePermissionIncludeOneNodeInTheResultSet()
    {
        // create ResultSet with one node
        // we have access permissions on this node
        NodeRef expected = createNodeRefWithAccessPermission(0, true);
        ResultSet resultSet = createResultSet(Arrays.asList(expected));

        // call decidePermissions with our ResultSet and no
        // ConfigAttributeDefintions
        ResultSet result = provider.decidePermissions(resultSet, null);

        // the returned ResultSet should contain our node
        assertEquals(1, result.length());
        assertEquals(expected, result.getNodeRef(0));
    }

    @Test
    public void shouldResultSetIncludeThreeNodesWhenWeAreDeniedAccessOnTwoNodes() {
        ResultSet resultSet = createResultSet(
                Arrays.asList(
                    createNodeRefWithAccessPermission(0, false),
                    createNodeRefWithAccessPermission(1, false),
                    createNodeRefWithAccessPermission(2, true),
                    createNodeRefWithAccessPermission(3, true),
                    createNodeRefWithAccessPermission(4, true)
                )
        );
        
        ResultSet result = provider.decidePermissions(resultSet, null);

        assertEquals(3, result.length());
        assertNodePresent(result, 2);
        assertNodePresent(result, 3);
        assertNodePresent(result, 4);
    }
    
    @Test
    public void shouldResultSetWorkWithMaxItemsAndSkipCount() {
        int skipCount = 2;
        int maxItems = 3;
        int expectedLength = maxItems + skipCount +1;
        ResultSet resultSet = createResultSet(createListOfAccessibleNodes(1,10));
        withSkipCount(resultSet, skipCount);
        withMaxItems(resultSet, maxItems);
        
        ResultSet result = provider.decidePermissions(resultSet, null);

        assertEquals(expectedLength, result.length());
        assertNodePresent(result, 2);
        assertNodePresent(result, 3);
        assertNodePresent(result, 4);
    }
    
    @Test
    public void shouldResultSetContainAtMostAllAccessibleNodesWhenMaxItemsCannotBeFulfilled() {
        List<NodeRef> nodes = createListOfInaccessibleNodes(1, 10);
        nodes.addAll(createListOfAccessibleNodes(11, 20));
        ResultSet resultSet = createResultSet(nodes);
        withSkipCount(resultSet, 2);
        withMaxItems(resultSet, 50);
        
        ResultSet result = provider.decidePermissions(resultSet, null);

        assertEquals(10, result.length());
        assertNodeNotPresent(result, 5);
        assertNodeNotPresent(result, 10);
        assertNodePresent(result, 11);
        assertNodePresent(result, 15);
        assertNodePresent(result, 20);
    }
    
    @Test
    public void shouldResultSetContainAllAccessibleNodesWhenXLSLocaleIsProvided() {
        ResultSet resultSet = createResultSet(createListOfAccessibleNodes(1, 10));
        withMaxItems(resultSet, 3);
        withLocale(resultSet, "xsl");
        
        ResultSet result = provider.decidePermissions(resultSet, null);
        
        assertEquals(10, result.length());
    }

    private void withLocale(ResultSet resultSet, String lang)
    {
        List<Locale> locales = Arrays.asList(new Locale(lang));
        SearchParameters sp = resultSet.getResultSetMetaData().getSearchParameters();
        when(sp.getLocales()).thenReturn(locales);
        
    }

    private List<NodeRef> createListOfAccessibleNodes(int startId, int endId)
    {
        return createListOfNodes(startId, endId, true);
    }

    private List<NodeRef> createListOfInaccessibleNodes(int startId, int endId)
    {
        return createListOfNodes(startId, endId, false);
    }

    private List<NodeRef> createListOfNodes(int startId, int endId, boolean accessPerm)
    {
        List<NodeRef> nodes = new ArrayList<>();
        
        for (int i = startId; i <= endId; i++)
        {
            nodes.add(createNodeRefWithAccessPermission(i, accessPerm));
        }
        
        return nodes;
    }
    
    private void withMaxItems(ResultSet rs, int maxItems)
    {
        SearchParameters sp = rs.getResultSetMetaData().getSearchParameters();
        when(sp.getMaxItems())
            .thenReturn(maxItems);
    }

    private void withSkipCount(ResultSet rs, int skipCount) 
    {
        when(rs.getResultSetMetaData().getSearchParameters().getSkipCount())
            .thenReturn(skipCount);
    }
    
    private void assertNodePresent(ResultSet result, int id)
    {
        String expectedId = String.valueOf(id);
        for (int i = 0; i < result.length(); i++)
        {
            NodeRef node = result.getNodeRef(i);
            if (node.getId().equals(expectedId))
            {
                return;
            }
        }
        fail("Could not find node with id " + id);
    }
    
    private void assertNodeNotPresent(ResultSet result, int id)
    {
        String expectedId = String.valueOf(id);
        for (int i = 0; i < result.length(); i++)
        {
            NodeRef node = result.getNodeRef(i);
            if (node.getId().equals(expectedId))
            {
                fail("Could not find node with id " + id);
            }
        }
    }

    private NodeRef createNodeRefWithAccessPermission(int id, boolean havePermissionOverIt) {
       NodeRef node = mock(NodeRef.class);
       when(node.getId()).thenReturn(String.valueOf(id));
       when(node.toString()).thenReturn(String.valueOf(id));
       when(permissionService.hasReadPermission(node)).thenReturn(havePermissionOverIt ? AccessStatus.ALLOWED : AccessStatus.DENIED);
       return node;
    }
    
    private ResultSet createResultSet(List<NodeRef> nodes)
    {
        ResultSet resultSet = createResultSet();
        
        for (int i = 0; i < nodes.size(); i++)
        {
            when(resultSet.getNodeRef(i)).thenReturn(nodes.get(i));
        }
        when(resultSet.length()).thenReturn(nodes.size());
        
        return resultSet;
    }
    
    public ResultSet createResultSet()
    {
        ResultSet rs = mock(ResultSet.class);
        SearchParameters sp = mock(SearchParameters.class);
        ResultSetMetaData rsm = mock(ResultSetMetaData.class);
        when(rs.getResultSetMetaData()).thenReturn(rsm);
        when(sp.getMaxPermissionChecks()).thenReturn(RESULT_SET_MAX_PERMISSION_CHECKS);
        when(sp.getMaxPermissionCheckTimeMillis()).thenReturn(RESULT_SET_MAX_PERMISSION_CHECK_TIME_MILLIS);
        when(sp.getMaxItems()).thenReturn(MAX_ITEMS);
        when(rsm.getSearchParameters()).thenReturn(sp);
        return rs;
    }
}
