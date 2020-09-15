package org.alfresco.repo.search.impl.querymodel.impl.db;

import java.sql.Connection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ibatis.session.SqlSession;

public class DBTableInfo
{
    private static final long COLLECT_TIMEOUT_IN_MILLIS = Long.getLong(DBTableInfo.class.getName() + ".timeout", 60000l);

    private static final Log logger = LogFactory.getLog(DBTableInfo.class);

    private final String tableName;

    private volatile long lastCollectedTime;
    private volatile Set<String> fieldNames;

    public DBTableInfo(String tableName)
    {
        this.tableName = tableName;
        this.fieldNames = Collections.emptySet();
    }

    public Set<String> getFieldNames()
    {
        return fieldNames;
    }

    private boolean isTimeToCollect()
    { 
        long now = System.currentTimeMillis();
        long nxt = lastCollectedTime + COLLECT_TIMEOUT_IN_MILLIS;
        if (nxt < now)
        {
            lastCollectedTime = now;
            return true;
        } 
        else
        {
            return false;
        }
    }

    private void collectDenormFieldNames(SqlSession template)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Collecting field names for table " + tableName);
        }

        Set<String> names = new HashSet<>();
        try
        {
            Connection conn = template.getConnection();
            try (java.sql.ResultSet columns = conn.getMetaData().getColumns(null, null, "alf_node_props_denorm", null))
            {
                while (columns.next())
                {
                    names.add(columns.getString("COLUMN_NAME"));
                }
            }
            
            fieldNames = names;
        } 
        catch (Exception ex)
        {
            logger.error("Unexpected", ex);
        }
    }

    public void refresh(SqlSession session)
    {
        if (isTimeToCollect())
        {
            collectDenormFieldNames(session);
        }
    }
}
