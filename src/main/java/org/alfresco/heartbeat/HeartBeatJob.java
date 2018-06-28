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
package org.alfresco.heartbeat;

import org.alfresco.heartbeat.datasender.HBData;
import org.alfresco.heartbeat.datasender.HBDataSenderService;
import org.alfresco.repo.lock.JobLockService;
import org.alfresco.repo.lock.LockAcquisitionException;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ParameterCheck;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * The scheduler responsible for scheduling and unscheduling of locking Heartbeat jobs.
 * This scheduler schedules jobs which lock execution on collector id.
 * Only one repository node in a cluster will send data for collectors with this type of job
 */

public class HeartBeatJob implements HeartBeatJobScheduler, Job
{
    /** The logger. */
    private static final Log logger = LogFactory.getLog(HeartBeatJob.class);

    /** Time to live 5 seconds */
    private static final long LOCK_TTL = 5000L;
    /** schedule set for all jobs scheduled with this scheduler if testMode is on */
    private boolean testMode = false;
    private final String testCronExpression = "0 0/1 * * * ?";
    /** Services needed to schedule and execute this job */
    private JobLockService jobLockService;
    private HBDataSenderService hbDataSenderService;
    private Scheduler scheduler;

    private static final String COLLECTOR_KEY = "collector";
    private static final String DATA_SENDER_SERVICE_KEY = "hbDataSenderService";
    private static final String JOB_LOCK_SERVICE_KEY = "jobLockService";

    public void setScheduler(Scheduler scheduler)
    {
        this.scheduler = scheduler;
    }

    public void setJobLockService(JobLockService jobLockService)
    {
        this.jobLockService = jobLockService;
    }

    public void setHbDataSenderService(HBDataSenderService hbDataSenderService)
    {
        this.hbDataSenderService = hbDataSenderService;
    }

    public void setTestMode(boolean testMode)
    {
        this.testMode = testMode;
    }

    public String getJobName(String collectorId)
    {
        return "heartbeat-" + collectorId;
    }

    public String getTriggerName(String collectorId)
    {
        return getJobName(collectorId) + "-Trigger";
    }

    /**
     *
     * Schedules a job for the provided collector, the scheduled job will lock execution causing it to
     * execute once in a clustered environment
     * @param collector
     */
    @Override
    public void scheduleJob(final HBBaseDataCollector collector)
    {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(HeartBeatJob.COLLECTOR_KEY, collector);
        jobDataMap.put(HeartBeatJob.DATA_SENDER_SERVICE_KEY, hbDataSenderService);
        jobDataMap.put(HeartBeatJob.JOB_LOCK_SERVICE_KEY, jobLockService);
        final JobDetail jobDetail = JobBuilder.newJob()
                .withIdentity(getJobName(collector.getCollectorId()))
                .usingJobData(jobDataMap)
                .ofType(HeartBeatJob.class)
                .build();

        final String cronExpression = testMode ? testCronExpression : collector.getCronExpression();
        // Schedule job
        final CronTrigger cronTrigger = TriggerBuilder.newTrigger()
                .withIdentity(getTriggerName(collector.getCollectorId()))
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                .build();

        try{
            // Ensure the job wasn't already scheduled in an earlier retry of this transaction
            scheduler.unscheduleJob(cronTrigger.getKey());
            scheduler.scheduleJob(jobDetail, cronTrigger);

            if (logger.isDebugEnabled())
            {
                logger.debug("HeartBeat job scheduled for collector: " +
                        collector.getCollectorId());
            }
        }
        catch (SchedulerException e)
        {
            throw new RuntimeException("Heartbeat failed to schedule job for collector: "
                    + collector.getCollectorId(), e);
        }
    }

    @Override
    public void unscheduleJob(final HBBaseDataCollector collector)
    {
        try
        {
            scheduler.unscheduleJob(new TriggerKey(getTriggerName(collector.getCollectorId())));

            if (logger.isDebugEnabled())
            {
                logger.debug("HeartBeat unscheduled job for collector: " +
                        collector.getCollectorId());
            }
        }
        catch (SchedulerException e)
        {
            throw new RuntimeException("Heartbeat failed to unschedule job for collector: "
                    + collector.getCollectorId(), e);
        }
    }

    @Override
    public void execute(final JobExecutionContext jobexecutioncontext) throws JobExecutionException
    {
        final JobDataMap dataMap = jobexecutioncontext.getJobDetail().getJobDataMap();
        final HBBaseDataCollector collector = (HBBaseDataCollector) dataMap.get(COLLECTOR_KEY);
        final HBDataSenderService hbDataSenderService = (HBDataSenderService) dataMap.get(DATA_SENDER_SERVICE_KEY);
        final JobLockService jobLockService = (JobLockService) dataMap.get(JOB_LOCK_SERVICE_KEY);

        ParameterCheck.mandatory( COLLECTOR_KEY, collector);
        ParameterCheck.mandatory(DATA_SENDER_SERVICE_KEY, hbDataSenderService);
        ParameterCheck.mandatory(JOB_LOCK_SERVICE_KEY, jobLockService);

        QName lockQname = QName.createQName(NamespaceService.SYSTEM_MODEL_1_0_URI, collector.getCollectorId());
        LockCallback lockCallback = new LockCallback(lockQname);
        try
        {
            // Get lock
            String lockToken = jobLockService.getLock(lockQname, LOCK_TTL);

            // Register the refresh callback which will keep the lock alive.
            // The lock will not be released manually,
            // instead the job lock service will check the callback (running) flag every LOCK_TTL/2 ms from lock acquisition
            // and release the lock when the flag is set to false.
            jobLockService.refreshLock(lockToken, lockQname, LOCK_TTL, lockCallback);

            if (logger.isDebugEnabled())
            {
                logger.debug("Lock acquired: " + lockQname + ": " + lockToken);
            }

            // Collect data and pass it to the data sender service
            collectAndSendData(collector, hbDataSenderService);
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
            if (logger.isDebugEnabled())
            {
                logger.debug("Finished collector job. ID:" + collector.getCollectorId());
            }
            lockCallback.running.set(false);
        }
    }

    private void collectAndSendData(final HBBaseDataCollector collector, final HBDataSenderService hbDataSenderService) throws JobExecutionException
    {
        try
        {
            List<HBData> data = collector.collectData();
            hbDataSenderService.sendData(data);
        }
        catch (final Exception e)
        {
            // Log the error but don't rethrow, collector errors are non fatal
            logger.error("Heartbeat failed to collect data for collector ID: " + collector.getCollectorId(), e);
        }
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
}
