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
package org.alfresco.repo.domain.schema.script;

import org.alfresco.util.LogUtil;
import org.alfresco.util.Pair;
import org.alfresco.util.PropertyCheck;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * The
 * <code>--DELETE_NOT_EXISTS primaryTable.columnName,secondaryTable1.columnName1,...,secondaryTableN.columnNameN batch.size.property</code>
 * statement is used to delete all the items that don't have any corresponding
 * key in any of the secondary tables (e.g. secondaryTable1.columnName1,...,secondaryTableN.columnNameN).
 * <p/>
 * The processing of the tables and the actual deletes are done in batches to support a high volume of data. It can be influenced using: <br>
 * <code>deleteNotExistsExecutor.setMaxBatchSize</code>
 * and/or<br>
 * <code>deleteNotExistsExecutor.setMaxDeleteBatchSize</code>
 * <p/>
 * The statement can be executed in read only mode using: <br>
 * <code>new DeleteNotExistsExecutor(connection, sql, line, scriptFile, true)</code>
 * <p/>
 * The statement 
 * 
 * @author Cristian Turlica
 */
public class DeleteNotExistsExecutor implements StatementExecutor
{
    private static Log logger = LogFactory.getLog(DeleteNotExistsExecutor.class);

    private static final String MSG_EXECUTING_STATEMENT = "schema.update.msg.executing_statement";
    private static final String ERR_STATEMENT_FAILED = "schema.update.err.statement_failed";
    private static final String MSG_OPTIONAL_STATEMENT_FAILED = "schema.update.msg.optional_statement_failed";

    public static final String PROPERTY_DEFAULT_BATCH_SIZE = "system.upgrade.default.batchsize";
    public static final String PROPERTY_DEFAULT_DELETE_BATCH_SIZE = "system.upgrade.default.delete.batchsize";

    private Connection connection;
    private String sql;
    private int line;
    private File scriptFile;
    private Properties globalProperties;
    
    private boolean readOnly = false;
    private int deleteBatchSize;
    private int batchSize;

    public DeleteNotExistsExecutor(Connection connection, String sql, int line, File scriptFile, boolean readOnly, Properties globalProperties)
    {
        this.connection = connection;
        this.sql = sql;
        this.line = line;
        this.scriptFile = scriptFile;
        this.readOnly = readOnly;
        this.globalProperties = globalProperties;
    }

    public void checkProperties()
    {
         PropertyCheck.mandatory(this, "globalProperties", globalProperties);
    }

    public void execute() throws Exception
    {
        checkProperties();

        logger.info("**sql**: " + sql);

        // --DELETE_NOT_EXISTS table.column batch.size.property
        String[] args = sql.split("[ \\t]+");
        int sepIndex;
        if (args.length == 3 && (sepIndex = args[1].indexOf('.')) != -1)
        {
            String[] tableColumnArgs = args[1].split(",");
            if (tableColumnArgs.length >= 2)
            {
                // Read the batch size from the named property
                String batchSizeString = globalProperties.getProperty(args[2]);
                // Fall back to the default property
                if (batchSizeString == null)
                {
                    batchSizeString = globalProperties.getProperty(PROPERTY_DEFAULT_BATCH_SIZE);
                }

                batchSize = batchSizeString == null ? 10000 : Integer.parseInt(batchSizeString);


                // Read the batch size from the named property
                String deleteBatchSizeString = globalProperties.getProperty(PROPERTY_DEFAULT_DELETE_BATCH_SIZE);
                deleteBatchSize = deleteBatchSizeString == null ? 10000 : Integer.parseInt(deleteBatchSizeString);
                
                
                // Compute upper limits
                int[] tableUpperLimits = new int[tableColumnArgs.length];
                Pair<String, String>[] tableColumn = new Pair[tableColumnArgs.length];
                for (int i = 0; i < tableColumnArgs.length; i++)
                {
                    int index = tableColumnArgs[i].indexOf('.');
                    String tableName = tableColumnArgs[i].substring(0, index);
                    String columnName = tableColumnArgs[i].substring(index + 1);

                    tableColumn[i] = new Pair<>(tableName, columnName);
                    tableUpperLimits[i] = getBatchUpperLimit(connection, tableName, columnName, line, scriptFile);

                    logger.info("BatchUpperLimit " + tableUpperLimits[i] + " for " + tableName + "." + columnName);
                }

                process(tableColumn, tableUpperLimits);
            }
        }
    }

    private void process(Pair<String, String>[] tableColumn, int[] tableUpperLimits) throws Exception
    {
        String primaryTableName = tableColumn[0].getFirst();
        String primaryColumnName = tableColumn[0].getSecond();

        Long primaryId = 0L;
        PreparedStatement primaryPrepStmt = null;
        PreparedStatement[] secondaryPrepStmts = null;
        Set<Long> deleteIds = new HashSet<>();
        try
        {
            primaryPrepStmt = connection.prepareStatement(createPreparedSelectStatement(primaryTableName, primaryColumnName));
            primaryPrepStmt.setObject(1, primaryId);
            primaryPrepStmt.setObject(2, tableUpperLimits[0]);

            boolean hasResults = primaryPrepStmt.execute();

            if (hasResults)
            {

                secondaryPrepStmts = new PreparedStatement[tableColumn.length];
                for (int i = 1; i < tableColumn.length; i++)
                {
                    PreparedStatement secStmt = connection.prepareStatement(createPreparedSelectStatement(tableColumn[i].getFirst(), tableColumn[i].getSecond()));
                    secStmt.setObject(1, primaryId);
                    secStmt.setObject(2, tableUpperLimits[i]);

                    secondaryPrepStmts[i] = secStmt;
                }

                while (hasResults && primaryId != null)
                {
                    // Process batch
                    primaryId = processPrimaryTableResultSet(primaryPrepStmt, secondaryPrepStmts, deleteIds, primaryTableName, primaryColumnName, tableColumn);

                    // Prepare for next batch
                    primaryPrepStmt.setObject(1, primaryId);
                    primaryPrepStmt.setObject(2, tableUpperLimits[0]);

                    for (int i = 1; i < tableColumn.length; i++)
                    {
                        PreparedStatement secStmt = secondaryPrepStmts[i];
                        secStmt.setObject(1, primaryId);
                        secStmt.setObject(2, tableUpperLimits[i]);
                    }

                    hasResults = primaryPrepStmt.execute();
                }
            }
        }
        finally
        {
            if (secondaryPrepStmts != null)
            {
                closeQuietly(secondaryPrepStmts);
            }

            closeQuietly(primaryPrepStmt);
        }

        // Check if we have any more ids to delete
        if (!deleteIds.isEmpty())
        {
            deleteFromPrimaryTable(deleteIds, primaryTableName, primaryColumnName);
        }
    }

    private Long processPrimaryTableResultSet(PreparedStatement primaryPrepStmt, PreparedStatement[] secondaryPrepStmts, Set<Long> deleteIds, String primaryTableName,
            String primaryColumnName, Pair<String, String>[] tableColumn) throws Exception
    {
        int rowsProcessed = 0;
        Long primaryId = null;
        ResultSet[] secondaryResultSets = null;
        try (ResultSet resultSet = primaryPrepStmt.getResultSet())
        {
            secondaryResultSets = getSecondaryResultSets(secondaryPrepStmts);
            Long[] secondaryIds = getSecondaryIds(secondaryResultSets, tableColumn);

            while (resultSet.next())
            {
                ++rowsProcessed;
                primaryId = resultSet.getLong(primaryColumnName);

//                logger.info("Processing row " + rowsProcessed + " rows from table " + primaryTableName + ".");

                while (isLess(primaryId, secondaryIds))
                {
                    deleteIds.add(primaryId);

                    if (deleteIds.size() == deleteBatchSize)
                    {
                        deleteFromPrimaryTable(deleteIds, primaryTableName, primaryColumnName);
                    }

                    if (!resultSet.next())
                    {
                        break;
                    }

                    ++rowsProcessed;
                    primaryId = resultSet.getLong(primaryColumnName);

//                    logger.info("Processing row " + rowsProcessed + " rows from table " + primaryTableName + ".");

                    // Try to limit processing to a reasonable size.
                    if (rowsProcessed == batchSize)
                    {
                        break;
                    }
                }

                updateSecondaryIds(primaryId, secondaryIds, secondaryResultSets, tableColumn);

                // Try to limit processing to a reasonable size.
                if (rowsProcessed == batchSize)
                {
                    break;
                }
            }
        }
        finally
        {
            closeQuietly(secondaryResultSets);
        }

        return primaryId;
    }

    private void deleteFromPrimaryTable(Set<Long> deleteIds, String primaryTableName, String primaryColumnName) throws Exception
    {
        if (!readOnly)
        {
            createAndExecuteDeleteStatement(connection, primaryTableName, primaryColumnName, deleteIds, deleteBatchSize, line, scriptFile);
        }
        else
        {
            logger.debug("Script would have deleted " + deleteIds.size() + " items from table " + primaryTableName + ".");
        }

        deleteIds.clear();
    }

    /**
     * Execute the given SQL statement, absorbing exceptions that we expect during
     * schema creation or upgrade.
     *
     * @param fetchColumnName the name of the column value to return
     */
    private Object executeStatement(
            Connection connection,
            String sql,
            String fetchColumnName,
            boolean optional,
            int line,
            File file) throws Exception {
//        StringBuilder executedStatements = executedStatementsThreadLocal.get();
//        if (executedStatements == null)
//        {
//            throw new IllegalArgumentException("The executedStatementsThreadLocal must be populated");
//        }

        Statement stmt = connection.createStatement();
        Object ret = null;
        try {
            if (logger.isDebugEnabled()) {
                LogUtil.debug(logger, MSG_EXECUTING_STATEMENT, sql);
            }
            boolean haveResults = stmt.execute(sql);
            // Record the statement
//            executedStatements.append(sql).append(";\n\n");
            if (haveResults && fetchColumnName != null) {
                ResultSet rs = stmt.getResultSet();
                if (rs.next()) {
                    // Get the result value
                    ret = rs.getObject(fetchColumnName);
                }
            }
        } catch (SQLException e) {
            if (optional) {
                // it was marked as optional, so we just ignore it
                LogUtil.debug(logger, MSG_OPTIONAL_STATEMENT_FAILED, sql, e.getMessage(), file.getAbsolutePath(), line);
            } else {
                LogUtil.error(logger, ERR_STATEMENT_FAILED, sql, e.getMessage(), file.getAbsolutePath(), line);
                throw e;
            }
        } finally {
            try {
                stmt.close();
            } catch (Throwable e) {
            }
        }
        return ret;
    }

    private int getBatchUpperLimit(Connection connection, String tableName, String columnName, int line, File scriptFile) throws Exception {
        int batchUpperLimit = 0;

        String stmt = "SELECT MAX(" + columnName + ") AS upper_limit FROM " + tableName;
        Object fetchedVal = executeStatement(connection, stmt, "upper_limit", false, line, scriptFile);

        if (fetchedVal instanceof Number) {
            batchUpperLimit = ((Number) fetchedVal).intValue();
        }

        return batchUpperLimit;
    }

    private boolean isLess(Long primaryId, Long[] secondaryIds) {
        for (Long secondaryId : secondaryIds) {
            if (secondaryId != null && primaryId >= secondaryId) {
                return false;
            }
        }

        return true;
    }

    private String createPreparedSelectStatement(String tableName, String columnName)
    {
        return "SELECT " + columnName + " FROM " + tableName + " WHERE " + columnName + " > ? AND " + columnName + " <= ? ORDER BY " + columnName + " ASC";
    }

    private void createAndExecuteDeleteStatement(Connection connection, String tableName, String idColumnName, Set<Long> deleteIds, int maxBatchSize, int line, File scriptFile) throws Exception {
        if (deleteIds.isEmpty()) {
            return;
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Prepare to delete " + deleteIds.size() + " items from table " + tableName + ".");
        }

        StringBuilder stmtBuilder = new StringBuilder("DELETE FROM " + tableName + " WHERE " + idColumnName + " IN ");
        stmtBuilder.append("(");
        int i = 1;
        for (Long deleteId : deleteIds) {
            if (i < maxBatchSize) {
                stmtBuilder.append("?,");
            } else {
                stmtBuilder.append("?");
            }

            i++;
        }

        for (int j = i; j <= maxBatchSize; j++) {
            if (j < maxBatchSize) {
                stmtBuilder.append("?,");
            } else {
                stmtBuilder.append("?");
            }
        }
        stmtBuilder.append(")");


//        StringBuilder executedStatements = executedStatementsThreadLocal.get();
//        if (executedStatements == null)
//        {
//            throw new IllegalArgumentException("The executedStatementsThreadLocal must be populated");
//        }

        String sql = stmtBuilder.toString();
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(sql);

            i = 1;
            for (Long deleteId : deleteIds) {
                stmt.setObject(i, deleteId);
                i++;
            }

            for (int j = i; j <= maxBatchSize; j++) {
                stmt.setObject(j, 0);
            }


            if (logger.isDebugEnabled()) {
                LogUtil.debug(logger, MSG_EXECUTING_STATEMENT, sql);
            }

            stmt.execute();
            // Record the statement
//            executedStatements.append(sql).append(";\n\n");
        } catch (SQLException e) {
            LogUtil.error(logger, ERR_STATEMENT_FAILED, sql, e.getMessage(), scriptFile.getAbsolutePath(), line);
            throw e;
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (Exception e) {
                    // Little can be done at this stage.
                }
            }
        }

        logger.info("Finished deleting " + deleteIds.size() + " items from table " + tableName + ".");
    }

    private Long getColumnValueById(ResultSet resultSet, String columnId) throws SQLException
    {
        Long columnValue = null;
        if (resultSet != null && resultSet.next())
        {
            columnValue = resultSet.getLong(columnId);
        }

        return columnValue;
    }

    private ResultSet[] getSecondaryResultSets(PreparedStatement[] preparedStatements) throws SQLException
    {
        ResultSet[] secondaryResultSets = new ResultSet[preparedStatements.length];
        for (int i = 1; i < preparedStatements.length; i++)
        {
            PreparedStatement secStmt = preparedStatements[i];

            boolean secHasResults = secStmt.execute();
            secondaryResultSets[i] = secHasResults ? secStmt.getResultSet() : null;
        }

        return secondaryResultSets;
    }
    
    private Long[] getSecondaryIds(ResultSet[] secondaryResultSets, Pair<String, String>[] tableColumn) throws SQLException
    {
        Long[] secondaryIds = new Long[tableColumn.length];

        for (int i = 1; i < tableColumn.length; i++)
        {
            ResultSet resultSet = secondaryResultSets[i];
            String columnId = tableColumn[i].getSecond();

            secondaryIds[i] = getColumnValueById(resultSet, columnId);
        }

        return secondaryIds;
    }

    private void updateSecondaryIds(Long primaryId, Long[] secondaryIds, ResultSet[] secondaryResultSets, Pair<String, String>[] tableColumn) throws SQLException
    {
        for (int i = 1; i < tableColumn.length; i++)
        {
            Long secondaryId = secondaryIds[i];
            while (secondaryId != null && primaryId >= secondaryId)
            {
                ResultSet resultSet = secondaryResultSets[i];
                String columnId = tableColumn[i].getSecond();

                secondaryId = getColumnValueById(resultSet, columnId);
                secondaryIds[i] = secondaryId;
            }
        }
    }

    private void closeQuietly(Statement statement)
    {
        if (statement != null)
        {
            try
            {
                statement.close();
            }
            catch (Exception e)
            {
                // Little can be done at this stage.
            }
        }
    }

    private void closeQuietly(Statement[] statements)
    {
        if (statements != null)
        {
            for (Statement statement : statements)
            {
                closeQuietly(statement);
            }
        }
    }

    private void closeQuietly(ResultSet resultSet)
    {
        if (resultSet != null)
        {
            try
            {
                resultSet.close();
            }
            catch (Exception e)
            {
                // Little can be done at this stage.
            }
        }
    }

    private void closeQuietly(ResultSet[] resultSets)
    {
        if (resultSets != null)
        {
            for (ResultSet resultSet : resultSets)
            {
                closeQuietly(resultSet);
            }
        }
    }
}
