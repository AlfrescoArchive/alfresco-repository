/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2018 Alfresco Software Limited
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
package org.alfresco.repo.metrics.db;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.alfresco.micrometer.PrometheusRegistryConfig;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DBMetricsReporterImpl implements DBMetricsReporter, ApplicationContextAware, InitializingBean
{
    public static final String METER_REGISTRY_BEAN_ERROR_MESSAGE = "Could not get the meterRegistry bean, essential for reporting DB metrics.";
    public static final String QUERIES_EXECUTION_TIME = "queries.execution.time";
    public static final int MAX_TAG_LENGTH = 1024;

    private Log logger = LogFactory.getLog(getClass());

    private boolean enabled;
    private boolean queryMetricsEnabled;
    private boolean queryStatementsMetricsEnabled;

    private DataSource dataSource;
    private ApplicationContext applicationContext;

    MeterRegistry meterRegistry;

    @Override
    public void afterPropertiesSet() throws Exception
    {
        try
        {
            init();
        }
        catch (Exception e)
        {
            if (logger.isWarnEnabled())
            {
                logger.warn("Could not initialize DB metrics reporter: " + e.getMessage(), e);
            }
        }
    }

    private void init()
    {
        if (!enabled)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("DB Metrics reporting is not enabled");
            }
            return;
        }
        try
        {
            //    meterRegistry = (MeterRegistry) applicationContext.getBean("meterRegistry");
            //TODO this is temporary
            final PrometheusMeterRegistry prometheus = PrometheusRegistryConfig.prometheus();
            meterRegistry = prometheus;
        }
        catch (Exception e)
        {
            logger.error(METER_REGISTRY_BEAN_ERROR_MESSAGE + " Cause: " + e.getMessage(), e);
        }

        if (meterRegistry == null)
        {
            logger.warn(METER_REGISTRY_BEAN_ERROR_MESSAGE);
            return;
        }

        initConnectionMetrics();

    }

    private void initConnectionMetrics()
    {
        if (meterRegistry != null)
        {
            meterRegistry
                .gauge("num.connections.active", Collections.emptyList(), dataSource, ConnectionGaugeDataProvider::getNumActive);
            meterRegistry
                .gauge("num.connections.idle", Collections.emptyList(), dataSource, ConnectionGaugeDataProvider::getNumIdle);
        }
    }

    @Override
    public void reportQueryExecutionTime(final long milliseconds, final String queryTpe, final String statementID)
    {
        try
        {
            if (isQueryMetricsEnabled() && meterRegistry != null && !isEmpty(queryTpe) && milliseconds >= 0)
            {
                List<Tag> tags = buildTagsForQueryExecution(queryTpe, statementID);
                meterRegistry.timer(QUERIES_EXECUTION_TIME, tags).record(milliseconds, TimeUnit.MILLISECONDS);
            }
        }
        catch (Exception e)
        {
            logMetricReportingProblem(e);
        }
    }

    private List<Tag> buildTagsForQueryExecution(String queryTpe, String statementID)
    {
        // we know that queryTpe is not empty at this point, but just for safe measure, sanitize it
        String sanitizedQueryType = sanitizeTagValue(queryTpe);
        List<Tag> tags = new ArrayList<>();
        tags.add(Tag.of("queryType", sanitizedQueryType));

        if (isQueryStatementsMetricsEnabled() && !isEmpty(statementID))
        {
            //just to be sure, sanitize the string
            String sanitizedStatementID = sanitizeTagValue(statementID);
            tags.add(Tag.of("statementID", sanitizedStatementID));
        }
        return tags;
    }

    private String sanitizeTagValue(String tagValue)
    {
        //we always assume parameter is not null
        String str = tagValue.trim();
        if (str.length() > MAX_TAG_LENGTH)
        {
            return str.substring(0, MAX_TAG_LENGTH - 1);
        }
        return str;
    }

    @Override
    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    @Override
    public boolean isQueryMetricsEnabled()
    {
        return this.enabled && queryMetricsEnabled;
    }

    public void setQueryMetricsEnabled(boolean queryMetricsEnabled)
    {
        this.queryMetricsEnabled = queryMetricsEnabled;
    }

    @Override
    public boolean isQueryStatementsMetricsEnabled()
    {
        return queryStatementsMetricsEnabled;
    }

    public void setQueryStatementsMetricsEnabled(boolean queryStatementsMetricsEnabled)
    {
        this.queryStatementsMetricsEnabled = queryStatementsMetricsEnabled;
    }

    public void setDataSource(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        this.applicationContext = applicationContext;
    }

    private void logMetricReportingProblem(Exception e)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Could not report metric: " + e.getMessage(), e);
        }
    }

    private boolean isEmpty(String tag)
    {
        return tag == null || tag.isEmpty();
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());

        sb.append(" DB Metrics Reporting is enabled:");
        sb.append(isEnabled());

        sb.append(". Query metrics enabled: ");
        sb.append(isQueryMetricsEnabled());

        sb.append(". Query statement metrics enabled: ");
        sb.append(isQueryStatementsMetricsEnabled());
        return sb.toString();
    }
}

class ConnectionGaugeDataProvider
{
    private static Log logger = LogFactory.getLog(ConnectionGaugeDataProvider.class);

    public static double getNumActive(DataSource dataSource)
    {
        try
        {
            return ((BasicDataSource) dataSource).getNumActive();
        }
        catch (Exception e)
        {
            reportException(e);
        }
        return 0;
    }

    public static double getNumIdle(DataSource dataSource)
    {
        try
        {
            return ((BasicDataSource) dataSource).getNumIdle();
        }
        catch (Exception e)
        {
            reportException(e);
        }
        return 0;
    }

    private static void reportException(Exception e)
    {
        if (logger.isWarnEnabled())
        {
            logger.warn("Exception in getting the DB connection pool data: " + e.getMessage(), e);
        }
    }

}
