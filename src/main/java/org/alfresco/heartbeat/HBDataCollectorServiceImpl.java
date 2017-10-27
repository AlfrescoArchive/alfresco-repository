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
import java.util.concurrent.atomic.AtomicBoolean;

import org.alfresco.heartbeat.datasender.HBData;
import org.alfresco.heartbeat.datasender.HBDataSenderService;
import org.alfresco.repo.lock.JobLockService;
import org.alfresco.repo.lock.LockAcquisitionException;
import org.alfresco.service.cmr.repository.HBDataCollectorService;
import org.alfresco.service.license.LicenseDescriptor;
import org.alfresco.service.license.LicenseService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

public class HBDataCollectorServiceImpl implements HBDataCollectorService, LicenseService.LicenseChangeHandler
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

    /** The heart beat job will be locked for max 30 seconds */
    private static final long LOCK_TTL = 30000L;

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
     *
     * @param collector collector to register
     */
    @Override
    public void registerCollector(HBBaseDataCollector collector)
    {
        // collectors are unique
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
                    logger.debug("Invalid cron expression " + cronExpression);
                }
            }
            scheduler.scheduleJob(jobDetail, cronTrigger);
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

    /**
     * The scheduler job responsible for triggering a heartbeat on a regular basis.
     */
    public static class HeartBeatJob implements Job
    {
        public void execute(final JobExecutionContext jobexecutioncontext) throws JobExecutionException
        {
            final JobDataMap dataMap = jobexecutioncontext.getJobDetail().getJobDataMap();
            final HBBaseDataCollector collector = (HBBaseDataCollector) dataMap.get("collector");
            final HBDataSenderService hbDataSenderService = (HBDataSenderService) dataMap.get("hbDataSenderService");
            final JobLockService jobLockService = (JobLockService) dataMap.get("jobLockService");

            if(collector == null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Exit HertBeatJob because there is no assigned HB collector");
                }
            }
            if(hbDataSenderService == null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Exit HertBeatJob because there is no HBDataSenderService");
                }
            }
            if(jobLockService == null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Exit HeartBeatJob because there is no JobLockService");
                }
            }
            QName qName = QName.createQName(NamespaceService.SYSTEM_MODEL_1_0_URI, collector.getCollectorId());
            String lockToken = null;
            LockCallback lockCallback = new LockCallback(qName);
            try
            {
                // Get a dynamic lock
                lockToken = acquireLock(lockCallback, qName, jobLockService);
                collectAndSendDataLocked(collector, hbDataSenderService);
            }
            catch (LockAcquisitionException e)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Skipping collect and send data (could not get lock): " + e.getMessage());
                }
            }
            finally
            {
                releaseLock(lockCallback, lockToken, qName, jobLockService);
            }
        }

        private void collectAndSendDataLocked(final HBBaseDataCollector collector, final HBDataSenderService hbDataSenderService) throws JobExecutionException
        {
            try
            {
                List<HBData> data = collector.collectData();
                try
                {
                    hbDataSenderService.sendData(data);
                }
                catch (Exception e)
                {
                    logger.error(e);
                }
            }
            catch (final Exception e)
            {
                if (logger.isDebugEnabled())
                {
                    // Verbose logging
                    logger.debug("Heartbeat job failure from collector: " + collector.getCollectorId(), e);
                }
                else
                {
                    // Heartbeat errors are non-fatal and will show as single line warnings
                    logger.warn(e.toString());
                    throw new JobExecutionException(e);
                }
            }
        }

        private String acquireLock(JobLockService.JobLockRefreshCallback lockCallback, QName lockQname, JobLockService jobLockService)
        {
            // Get lock
            String lockToken = jobLockService.getLock(lockQname, LOCK_TTL);

            // Register the refresh callback which will keep the lock alive
            jobLockService.refreshLock(lockToken, lockQname, LOCK_TTL, lockCallback);

            if (logger.isDebugEnabled())
            {
                logger.debug("lock acquired: " + lockQname + ": " + lockToken);
            }

            return lockToken;
        }

        private class LockCallback implements JobLockService.JobLockRefreshCallback
        {
            final AtomicBoolean running = new AtomicBoolean(true);
            private QName lockQname;

            public LockCallback(QName lockQname)
            {
                this.lockQname = lockQname;
            }

            @Override
            public boolean isActive()
            {
                return running.get();
            }

            @Override
            public void lockReleased()
            {
                running.set(false);
                if (logger.isDebugEnabled())
                {
                    logger.debug("Lock release notification: " + lockQname);
                }
            }
        }

        private void releaseLock(LockCallback lockCallback, String lockToken, QName lockQname, JobLockService jobLockService)
        {
            if (lockCallback != null)
            {
                lockCallback.running.set(false);
            }

            if (lockToken != null)
            {
                jobLockService.releaseLock(lockToken, lockQname);
                if (logger.isDebugEnabled())
                {
                    logger.debug("Lock released: " + lockQname + ": " + lockToken);
                }
            }
        }
    }
}
