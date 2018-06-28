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
import org.alfresco.util.ParameterCheck;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.*;

import java.util.List;


/**
 * This scheduler schedules jobs which execute without any locking.
 * All repository nodes in a cluster will send data for collectors which have jobs scheduled by this scheduler.
 *
 * @author eknizat
 *
 */
public class NonLockingHeartbeatJob implements HeartBeatJobScheduler, Job
{

    /** The logger. */
    private static final Log logger = LogFactory.getLog(NonLockingHeartbeatJob.class);

    private static final String COLLECTOR_KEY = "collector";
    private static final String DATA_SENDER_SERVICE_KEY = "hbDataSenderService";
    private HBDataSenderService hbDataSenderService;
    private Scheduler scheduler;
    private boolean testMode = false;
    private final String testCronExpression = "0 0/1 * * * ?";


    public void setHbDataSenderService(HBDataSenderService hbDataSenderService)
    {
        this.hbDataSenderService = hbDataSenderService;
    }

    public void setScheduler(Scheduler scheduler)
    {
        this.scheduler = scheduler;
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

    @Override
    public void scheduleJob(final HBBaseDataCollector collector)
    {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(NonLockingHeartbeatJob.COLLECTOR_KEY, collector);
        jobDataMap.put(NonLockingHeartbeatJob.DATA_SENDER_SERVICE_KEY, hbDataSenderService);
        final JobDetail jobDetail = JobBuilder.newJob()
                .withIdentity(getJobName(collector.getCollectorId()))
                .usingJobData(jobDataMap)
                .ofType(NonLockingHeartbeatJob.class)
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
    public void execute(final JobExecutionContext jobExecutionContext) throws JobExecutionException
    {
        final JobDataMap dataMap = jobExecutionContext.getJobDetail().getJobDataMap();
        final HBBaseDataCollector collector = (HBBaseDataCollector) dataMap.get(COLLECTOR_KEY);
        final HBDataSenderService hbDataSenderService = (HBDataSenderService) dataMap.get(DATA_SENDER_SERVICE_KEY);

        ParameterCheck.mandatory( COLLECTOR_KEY, collector);
        ParameterCheck.mandatory( DATA_SENDER_SERVICE_KEY, hbDataSenderService);

        try
        {
            List<HBData> data = collector.collectData();
            hbDataSenderService.sendData(data);

            if (logger.isDebugEnabled())
            {
                logger.debug("Finished collector job. ID:" + collector.getCollectorId());
            }
        }
        catch (final Exception e)
        {
            // Log the error but don't rethrow, collector errors are non fatal
            logger.error("Heartbeat failed to collect data for collector ID: " + collector.getCollectorId(), e);
        }
    }
}
