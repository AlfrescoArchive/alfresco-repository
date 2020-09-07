package org.alfresco.repo.search.impl.querymodel.impl.db.queryset.impl;

import java.util.Arrays;
import java.util.List;

import org.alfresco.repo.search.impl.querymodel.impl.db.DBQuery;
import org.alfresco.repo.search.impl.querymodel.impl.db.DBTableInfo;
import org.alfresco.repo.search.impl.querymodel.impl.db.queryset.QuerySet;
import org.alfresco.repo.search.impl.querymodel.impl.db.queryset.QuerySetConfigurationService;
import org.alfresco.service.namespace.QName;
import org.apache.ibatis.session.SqlSession;

/**
 * Just a temporary implementation while we wait for Jamal's real implementation of the QuerySetConfigurationService
 * @author nana
 *
 */
public class DbQuerySetConfigurationService implements QuerySetConfigurationService
{

    private final DbQuerySet querySet;

    public DbQuerySetConfigurationService(String tableName)
    {
        this.querySet = new DbQuerySet(tableName);
    }

    @Override
    public List<QuerySet> getQuerySets()
    {
        return Arrays.asList(querySet);
    }
    
    public void refresh(SqlSession session) {
        querySet.refresh(session);
    }

    public class DbQuerySet implements QuerySet
    {
        private final String tableName;
        private final DBTableInfo denormTable;

        public DbQuerySet(String tableName)
        {
            this.tableName = tableName;
            this.denormTable = new DBTableInfo(tableName);
        }
        
        public void refresh(SqlSession session)
        {
            denormTable.refresh(session);
        }

        @Override
        public String getTableName()
        {
            return tableName;
        }

        @Override
        public String getColumnName(QName qname)
        {
            return getLocalName(qname);
        }
        
        private String getLocalName(QName qname)
        {
            String fieldName = DBQuery.getFieldNameIfAudit(qname);
            if (fieldName == null)
                fieldName = qname.getLocalName();
            
            if(denormTable.getFieldNames().contains(fieldName))
                return fieldName;
            
            return null;
        }
    }
}
