/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2017 Alfresco Software Limited
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
package org.alfresco.heartbeat;

import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;

import org.alfresco.heartbeat.datasender.HBDataSenderService;
import org.alfresco.repo.lock.JobLockService;
import org.alfresco.service.cmr.repository.HBDataCollectorService;
import org.alfresco.service.license.LicenseDescriptor;
import org.alfresco.service.license.LicenseService.LicenseChangeHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

public class HBDataCollectorServiceImpl implements HBDataCollectorService, LicenseChangeHandler
{
    /** The logger. */
    private static final Log logger = LogFactory.getLog(HBDataCollectorServiceImpl.class);

    /** List of collectors registered with this service */
    private List<HBBaseDataCollector> collectors = new LinkedList<>();

    /** The service responsible for sending the collected data */
    private HBDataSenderService hbDataSenderService;
    private JobLockService jobLockService;

    /** The default enable state */
    private final boolean defaultHbState;

    private Scheduler scheduler;

    private boolean testMode = true;

    /** Current enabled state */
    private boolean enabled = false;

    /**
     *
     * @param defaultHeartBeatState the default enabled state of heartbeat
     *
     */
    public HBDataCollectorServiceImpl (boolean defaultHeartBeatState)
    {
        this.defaultHbState = defaultHeartBeatState;
    }

    public void setHbDataSenderService(HBDataSenderService hbDataSenderService)
    {
        this.hbDataSenderService = hbDataSenderService;
    }

    public void setJobLockService(JobLockService jobLockService)
    {
        this.jobLockService = jobLockService;
    }

    public void setScheduler(Scheduler scheduler)
    {
        this.scheduler = scheduler;
    }

    public void setTestMode(boolean testMode)
    {
        this.testMode = testMode;
    }

    /**
     *
     * Register data collector with this service and start the schedule.
     * The registered collector will be called to provide heartbeat data.
     * The collector must be unique with using the collector id.
     *
     * @param collector collector to register
     */
    @Override
    public void registerCollector(HBBaseDataCollector collector)
    {
        for(HBBaseDataCollector col : collectors)
        {
            if(col.getCollectorId().equals(collector.getCollectorId()))
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Didn't register collector because it already exists: " + collector.getCollectorId());
                }
                return;
            }
        }
        this.collectors.add(collector);
        try
        {
            scheduleCollector(collector);
        }
        catch (Exception e)
        {
            logger.error("Unable to schedule heart beat", e);
        }
    }

    /**
     * Listens for license changes.  If a license is change or removed, the heartbeat job is rescheduled.
     */
    public synchronized void onLicenseChange(LicenseDescriptor licenseDescriptor)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Update license called");
        }

        boolean newEnabled = !licenseDescriptor.isHeartBeatDisabled();

        if (newEnabled != this.enabled)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("State change of heartbeat");
            }
            this.enabled = newEnabled;
            restartAllCollectorSchedules();
        }
    }

    /**
     * License load failure resets the heartbeat back to the default state
     */
    public synchronized void onLicenseFail()
    {
        boolean newEnabled = isEnabledByDefault();

        if (newEnabled != this.enabled)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("State change of heartbeat");
            }
            this.enabled = newEnabled;
            restartAllCollectorSchedules();
        }
    }

    private void restartAllCollectorSchedules()
    {
        try
        {
            for(HBBaseDataCollector collector : collectors)
            {
                scheduleCollector(collector);
            }
        }
        catch (Exception e)
        {
            logger.error("Unable to schedule heart beat", e);
        }
    }

    /**
     * Start or stop the hertbeat job depending on whether the heartbeat is enabled or not
     * @throws SchedulerException
     */
    private synchronized void scheduleCollector(HBBaseDataCollector collector) throws SchedulerException
    {
        // Schedule the heart beat to run regularly
        final String jobName = "heartbeat-" + collector.getCollectorId();
        final String triggerName = jobName + "-Trigger";
        if(this.enabled)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("heartbeat job scheduled");
            }
            final JobDetail jobDetail = new JobDetail(jobName, Scheduler.DEFAULT_GROUP, HeartBeatJob.class);
            jobDetail.getJobDataMap().put("collector", collector);
            jobDetail.getJobDataMap().put("hbDataSenderService", hbDataSenderService);
            jobDetail.getJobDataMap().put("jobLockService", jobLockService);

            // Ensure the job wasn't already scheduled in an earlier retry of this transaction
            scheduler.unscheduleJob(triggerName, Scheduler.DEFAULT_GROUP);

            String cronExpression = collector.getCronExpression();
            CronTrigger cronTrigger = null;
            try
            {
                cronTrigger = new CronTrigger(triggerName , Scheduler.DEFAULT_GROUP, cronExpression);
            }
            catch (ParseException e)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Skipping scheduling because of invalid cron expression: " + cronExpression);
                }
            }
            if(cronTrigger != null)
            {
                scheduler.scheduleJob(jobDetail, cronTrigger);
            }
        }
        else
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("heartbeat job unscheduled");
            }
            scheduler.unscheduleJob(triggerName, Scheduler.DEFAULT_GROUP);
        }
    }

    @Override
    public boolean isEnabledByDefault()
    {
        return defaultHbState;
    }

    @Override
    public synchronized void enabled(boolean enabled)
    {
        this.hbDataSenderService.enable(enabled);
    }

}
