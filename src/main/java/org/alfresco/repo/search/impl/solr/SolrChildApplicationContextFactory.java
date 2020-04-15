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
package org.alfresco.repo.search.impl.solr;

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Stream.concat;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeoutException;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.management.subsystems.ChildApplicationContextFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableSet;

/**
 * @author Andy
 */
public class SolrChildApplicationContextFactory extends ChildApplicationContextFactory
{
    //region Static Fields
    private static final String ALFRESCO_ACTIVE = "tracker.alfresco.active";
    private static final String ALFRESCO_LAG = "tracker.alfresco.lag";
    private static final String ALFRESCO_LAG_DURATION = "tracker.alfresco.lag.duration";
    private static final String ALFRESCO_LAST_INDEXED_TXN = "tracker.alfresco.last.indexed.txn";
    private static final String ALFRESCO_APPROX_TXNS_REMAINING = "tracker.alfresco.approx.txns.remaining";
    private static final String ALFRESCO_APPROX_INDEXING_TIME_REMAINING = "tracker.alfresco.approx.indexing.time.remaining";
    private static final String ALFRESCO_DISK = "tracker.alfresco.disk";
    private static final String ALFRESCO_MEMORY = "tracker.alfresco.memory";

    private static final String ARCHIVE_ACTIVE = "tracker.archive.active";
    private static final String ARCHIVE_LAG = "tracker.archive.lag";
    private static final String ARCHIVE_LAG_DURATION = "tracker.archive.lag.duration";
    private static final String ARCHIVE_LAST_INDEXED_TXN = "tracker.archive.last.indexed.txn";
    private static final String ARCHIVE_APPROX_TXNS_REMAINING = "tracker.archive.approx.txns.remaining";
    private static final String ARCHIVE_APPROX_INDEXING_TIME_REMAINING = "tracker.archive.approx.indexing.time.remaining";
    private static final String ARCHIVE_DISK = "tracker.archive.disk";
    private static final String ARCHIVE_MEMORY = "tracker.archive.memory";

    private static final Set<String> SOLR_TRACKER_PROPERTIES = ImmutableSet.of(
        ALFRESCO_ACTIVE,
        ALFRESCO_LAG,
        ALFRESCO_LAG_DURATION,
        ALFRESCO_LAST_INDEXED_TXN,
        ALFRESCO_APPROX_TXNS_REMAINING,
        ALFRESCO_APPROX_INDEXING_TIME_REMAINING,
        ALFRESCO_DISK,
        ALFRESCO_MEMORY,

        ARCHIVE_ACTIVE,
        ARCHIVE_LAG,
        ARCHIVE_LAG_DURATION,
        ARCHIVE_LAST_INDEXED_TXN,
        ARCHIVE_APPROX_TXNS_REMAINING,
        ARCHIVE_APPROX_INDEXING_TIME_REMAINING,
        ARCHIVE_DISK,
        ARCHIVE_MEMORY);

    /**
     * If SOLR is unreachable, this cache holds the Timeout error for a short while (1-2) seconds for subsequent
     * requests.
     * This will speed up the refresh of the "Search Services" page in the Alfresco Admin Console. Without it, the UI
     * refresh takes: nr_of_properties (~15) x timeout (~5 sec)
     */
    private static final Cache<Integer, AlfrescoRuntimeException> SOLR_CONNECTION_ERROR = CacheBuilder
        .newBuilder()
        .maximumSize(1)
        .expireAfterWrite(2, SECONDS)
        .build();
    //endregion

    @Override
    public boolean isUpdateable(final String name)
    {
        // TODO Auto-generated method stub
        return super.isUpdateable(name) && !SOLR_TRACKER_PROPERTIES.contains(name);
    }

    @Override
    public String getProperty(final String name)
    {
        // MNT-9254 fix, use search.solrAdminHTTPCLient bean to retrieve property value only if sorl subsystem is
        // active and started (application context in state should be not null)
        if (isUpdateable(name) || isSolrInactive())
        {
            // solr subsystem is not started or not active
            return getPropertyWithInactiveSolr(name);
        }

        try
        {
            final JSONObject json = retrieveSolrAdminJson();
            return readPropertyFromJSON(json, name);
        }
        catch (AlfrescoRuntimeException | IllegalArgumentException e)
        {
            return "Unavailable: " + e.getMessage();
        }
    }

    boolean isSolrInactive()
    {
        return ((ApplicationContextState) getState(false)).getApplicationContext(false) == null;
    }

    private JSONObject retrieveSolrAdminJson() throws AlfrescoRuntimeException, IllegalArgumentException
    {
        try
        {
            final AlfrescoRuntimeException e = SOLR_CONNECTION_ERROR.getIfPresent(0);
            if (e != null)
            {
                throw e;
            }
            return executeSolrAdminQuery();
        }
        catch (AlfrescoRuntimeException e)
        {
            if (e.getCause() instanceof TimeoutException)
            {
                SOLR_CONNECTION_ERROR.put(0, e);
            }
            throw e;
        }
    }

    private JSONObject executeSolrAdminQuery()
    {
        final ApplicationContext ctx = getApplicationContext();
        final SolrAdminHTTPClient adminClient = ctx.getBean("search.solrAdminHTTPCLient", SolrAdminHTTPClient.class);
        return adminClient.execute(Map.of(
            "action", "SUMMARY",
            "wt", "json"));
    }

    private String getPropertyWithInactiveSolr(final String name)
    {
        switch (name)
        {
        case ALFRESCO_ACTIVE:
        case ARCHIVE_ACTIVE:
            return "";
        case ALFRESCO_LAG:
        case ALFRESCO_LAG_DURATION:
        case ALFRESCO_LAST_INDEXED_TXN:
        case ALFRESCO_APPROX_TXNS_REMAINING:
        case ALFRESCO_APPROX_INDEXING_TIME_REMAINING:
        case ALFRESCO_DISK:
        case ALFRESCO_MEMORY:
        case ARCHIVE_APPROX_TXNS_REMAINING:
        case ARCHIVE_LAST_INDEXED_TXN:
        case ARCHIVE_LAG_DURATION:
        case ARCHIVE_LAG:
        case ARCHIVE_MEMORY:
        case ARCHIVE_DISK:
        case ARCHIVE_APPROX_INDEXING_TIME_REMAINING:
            return "Unavailable: solr subsystem not started";
        }
        return super.getProperty(name);
    }

    private static String readPropertyFromJSON(final JSONObject json, final String name)
    {
        try
        {
            final JSONObject summaryJson = json.getJSONObject("Summary");
            final JSONObject alfresco = getFieldOrNull(summaryJson, "alfresco");
            if (alfresco != null)
            {
                switch (name)
                {
                case ALFRESCO_ACTIVE:
                    String alfrescoActive = alfresco.getString("Active");
                    if (alfrescoActive == null || alfrescoActive.isEmpty())
                    {
                        // Admin Console is expecting a true/false value, not blank
                        return "false";
                    }
                    return alfrescoActive;
                case ALFRESCO_LAG:
                    return alfresco.getString("TX Lag");
                case ALFRESCO_LAG_DURATION:
                    return alfresco.getString("TX Duration");
                case ALFRESCO_LAST_INDEXED_TXN:
                    return alfresco.getString("Id for last TX in index");
                case ALFRESCO_APPROX_TXNS_REMAINING:
                    return alfresco.getString("Approx transactions remaining");
                case ALFRESCO_APPROX_INDEXING_TIME_REMAINING:
                    return alfresco.getString("Approx transaction indexing time remaining");
                case ALFRESCO_DISK:
                    return alfresco.getString("On disk (GB)");
                case ALFRESCO_MEMORY:
                    return alfresco.getString("Total Searcher Cache (GB)");
                }
            }

            final JSONObject archive = getFieldOrNull(summaryJson, "archive");
            if (archive != null)
            {
                switch (name)
                {
                case ARCHIVE_ACTIVE:
                    return archive.getString("Active");
                case ARCHIVE_LAG:
                    return archive.getString("TX Lag");
                case ARCHIVE_LAG_DURATION:
                    return archive.getString("TX Duration");
                case ARCHIVE_LAST_INDEXED_TXN:
                    return archive.getString("Id for last TX in index");
                case ARCHIVE_APPROX_TXNS_REMAINING:
                    return archive.getString("Approx transactions remaining");
                case ARCHIVE_APPROX_INDEXING_TIME_REMAINING:
                    return archive.getString("Approx transaction indexing time remaining");
                case ARCHIVE_DISK:
                    return archive.getString("On disk (GB)");
                case ARCHIVE_MEMORY:
                    return archive.getString("Total Searcher Cache (GB)");
                }
            }

            // Did not find the property in JSON or the core is turned off
            return "Unavailable";
        }
        catch (JSONException e)
        {
            return "Unavailable: " + e.getMessage();
        }
    }

    @Override
    public Set<String> getPropertyNames()
    {
        return concat(super.getPropertyNames().stream(), SOLR_TRACKER_PROPERTIES.stream())
            .collect(toCollection(TreeSet::new));
    }

    public void setProperty(String name, String value)
    {
        if (!isUpdateable(name))
        {
            throw new IllegalStateException("Illegal write to property \"" + name + "\"");
        }
        super.setProperty(name, value);
    }

    protected void destroy(boolean isPermanent)
    {
        super.destroy(isPermanent);
        doInit();
    }

    private static JSONObject getFieldOrNull(final JSONObject json, final String field)
    {
        try
        {
            return json.getJSONObject(field);
        }
        catch (JSONException ignore)
        {
            // The core might be absent.
        }
        return null;
    }
}
