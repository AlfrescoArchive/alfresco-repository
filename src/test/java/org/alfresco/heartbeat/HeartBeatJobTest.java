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
package org.alfresco.heartbeat;

import org.alfresco.heartbeat.datasender.HBData;
import org.alfresco.heartbeat.datasender.HBDataSenderService;
import org.alfresco.repo.lock.JobLockService;
import org.alfresco.repo.lock.LockAcquisitionException;
import org.alfresco.repo.scheduler.AlfrescoSchedulerFactory;
import org.alfresco.service.namespace.QName;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.quartz.*;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by mmuller on 27/10/2017.
 */
public class HeartBeatJobTest
{

    private HBDataSenderService mockDataSenderService;
    private JobLockService mockJobLockService;
    private Scheduler scheduler;
    HeartBeatJob hbJobScheduler = new HeartBeatJob();

    @Before
    public void setUp() throws Exception
    {
        // Create scheduler
        SchedulerFactoryBean sfb = new SchedulerFactoryBean();
        sfb.setSchedulerFactoryClass(AlfrescoSchedulerFactory.class);
        sfb.setAutoStartup(false);
        sfb.afterPropertiesSet();
        scheduler = sfb.getScheduler();

        hbJobScheduler.setHbDataSenderService(mockDataSenderService);
        hbJobScheduler.setJobLockService(mockJobLockService);
        hbJobScheduler.setScheduler(scheduler);

        mockDataSenderService = mock(HBDataSenderService.class);
        mockJobLockService = mock(JobLockService.class);
    }

    private class SimpleHBDataCollector extends HBBaseDataCollector
    {

        public SimpleHBDataCollector(String collectorId, String cron)
        {
            super(collectorId,"1.0",cron);
        }

        public List<HBData> collectData()
        {
            List<HBData> result = new LinkedList<>();
            result.add(new HBData("systemId2", this.getCollectorId(), "1", new Date()));
            return result;
        }
    }

    @Test
    public void testJobInClusterNotLocked() throws Exception
    {
        // mock the job context
        JobExecutionContext mockJobExecutionContext = mock(JobExecutionContext.class);
        // create the hb collector
        SimpleHBDataCollector simpleCollector = spy(new SimpleHBDataCollector("simpleCollector","0 0 0 ? * *"));
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("collector", simpleCollector);
        jobDataMap.put("hbDataSenderService", mockDataSenderService);
        jobDataMap.put("jobLockService", mockJobLockService);
        JobDetail jobDetail = JobBuilder.newJob()
                .setJobData(jobDataMap)
                .ofType(HeartBeatJob.class)
                .build();
        when(mockJobExecutionContext.getJobDetail()).thenReturn(jobDetail);


        // collector job is not locked from an other collector
        String lockToken = "locked";

        Runnable r1 = () ->
        {
            // if a second job tries to get the lock before we finished that will raise the exception
            when(mockJobLockService.getLock(isA(QName.class), anyLong())).thenReturn(lockToken).thenThrow(new LockAcquisitionException("", ""));
            try
            {
                new HeartBeatJob().execute(mockJobExecutionContext);
            }
            catch (JobExecutionException e)
            {
                //
            }
            finally
            {
                // when we are finished an other job can have the lock
                when(mockJobLockService.getLock(isA(QName.class), anyLong())).thenReturn(lockToken);
            }
        };
        Runnable r2 = () ->
        {
            try
            {
                new HeartBeatJob().execute(mockJobExecutionContext);
            }
            catch (JobExecutionException e)
            {
                //
            }
        };

        Thread t1 = new Thread(r1);
        Thread t2 = new Thread(r2);

        t1.start();
        Thread.sleep(500);
        t2.start();

        // Wait for threads to finish before testing
        Thread.sleep(1000);

        // verify that we collected and send data but just one time
        verify(simpleCollector, Mockito.times(2)).collectData();
        verify(mockDataSenderService, Mockito.times(2)).sendData(any(List.class));
        verify(mockDataSenderService, Mockito.times(0)).sendData(any(HBData.class));
        verify(mockJobLockService, Mockito.times(2)).getLock(any(QName.class), anyLong());
        verify(mockJobLockService, Mockito.times(2)).refreshLock(eq(lockToken), any(QName.class), anyLong(), any(
                JobLockService.JobLockRefreshCallback.class));
    }


    @Test
    public void testJobLocking() throws Exception
    {
        HBBaseDataCollector simpleCollector = mock(HBBaseDataCollector.class);
        when(simpleCollector.getCollectorId()).thenReturn("c1");
        when(simpleCollector.getCronExpression()).thenReturn("0 0 0 ? * *");

        // mock the job context
        JobExecutionContext mockJobExecutionContext = mock(JobExecutionContext.class);
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("collector", simpleCollector);
        jobDataMap.put("hbDataSenderService", mockDataSenderService);
        jobDataMap.put("jobLockService", mockJobLockService);
        JobDetail jobDetail = JobBuilder.newJob()
                .setJobData(jobDataMap)
                .ofType(HeartBeatJob.class)
                .build();
        when(mockJobExecutionContext.getJobDetail()).thenReturn(jobDetail);

        // Simulate job lock service
        String lockToken = "token";
        when(mockJobLockService.getLock(isA(QName.class), anyLong()))
                .thenReturn(lockToken)                                    // first job gets the lock
                .thenThrow(new LockAcquisitionException("", ""));         // second job doesn't get the lock

        // Run two heart beat jobs
        new HeartBeatJob().execute(mockJobExecutionContext);
        new HeartBeatJob().execute(mockJobExecutionContext);

        // Verify that the collector only collects data once, since only one job got the lock
        verify(simpleCollector, Mockito.times(1)).collectData();
        // Verify that data was passed to data sender
        verify(mockDataSenderService, Mockito.times(1)).sendData(any(List.class));
        verify(mockDataSenderService, Mockito.times(0)).sendData(any(HBData.class));
        // Verify that both jobs tried to get the lock
        verify(mockJobLockService, Mockito.times(2)).getLock(any(QName.class), anyLong());
        // Verify that a callback was registered once
        verify(mockJobLockService, Mockito.times(1)).refreshLock(eq(lockToken), any(QName.class),
                anyLong(),
                any(JobLockService.JobLockRefreshCallback.class));
    }

    /**
     *
     * Jobs are scheduled with cron expressions provided by collectors
     *
     */
    @Test
    public void testJobsScheduledWithDifferentCronExpressions() throws Exception
    {

        final String cron1 = "0 0/1 * * * ?";
        final String cron2 = "0 0/2 * * * ?";
        final String cron3 = "0 0/3 * * * ?";

        final HBBaseDataCollector c1 = new SimpleHBDataCollector("c1", cron1);
        final HBBaseDataCollector c2 = new SimpleHBDataCollector("c2", cron2);
        final HBBaseDataCollector c3 = new SimpleHBDataCollector("c3", cron3);

        final String triggerName1 = "heartbeat-" + c1.getCollectorId() + "-Trigger";
        final String triggerName2 = "heartbeat-" + c2.getCollectorId() + "-Trigger";
        final String triggerName3 = "heartbeat-" + c3.getCollectorId() + "-Trigger";

        // Register 3 collectors with 3 different cron expressions
        hbJobScheduler.scheduleJob(c1);
        hbJobScheduler.scheduleJob(c2);
        hbJobScheduler.scheduleJob(c3);

        String testCron1 = ((CronTrigger) scheduler.getTrigger(new TriggerKey(triggerName1, Scheduler.DEFAULT_GROUP))).getCronExpression();
        String testCron2 = ((CronTrigger) scheduler.getTrigger(new TriggerKey(triggerName2, Scheduler.DEFAULT_GROUP))).getCronExpression();
        String testCron3 = ((CronTrigger) scheduler.getTrigger(new TriggerKey(triggerName3, Scheduler.DEFAULT_GROUP))).getCronExpression();

        assertEquals("Cron expression doesn't match", cron1, testCron1);
        assertEquals("Cron expression doesn't match", cron2, testCron2);
        assertEquals("Cron expression doesn't match", cron3, testCron3);
    }

    @Test
    public void testUnscheduling() throws Exception
    {
        final String cron1 = "0 0/1 * * * ?";
        final String cron2 = "0 0/2 * * * ?";
        final String cron3 = "0 0/3 * * * ?";

        final HBBaseDataCollector c1 = new SimpleHBDataCollector("c1", cron1);
        final HBBaseDataCollector c2 = new SimpleHBDataCollector("c2", cron2);
        final HBBaseDataCollector c3 = new SimpleHBDataCollector("c3", cron3);

        final String triggerName3 = "heartbeat-" + c3.getCollectorId() + "-Trigger";

        // Register 3 collectors with 3 different cron expressions
        hbJobScheduler.scheduleJob(c1);
        hbJobScheduler.scheduleJob(c2);
        hbJobScheduler.scheduleJob(c3);

        // Unschedule 2
        hbJobScheduler.unscheduleJob(c1);
        hbJobScheduler.unscheduleJob(c2);

        // 1 & 2 gone, 3 is still there
        assertFalse(isJobScheduledForCollector(c1.getCollectorId(),scheduler));
        assertFalse(isJobScheduledForCollector(c2.getCollectorId(),scheduler));
        assertTrue(isJobScheduledForCollector(c3.getCollectorId(),scheduler));
    }

    @Test(expected=RuntimeException.class)
    public void testInvalidCronExpression() throws Exception
    {

        // Register collector with invalid cron expression
        SimpleHBDataCollector c2 = new SimpleHBDataCollector("c2", "Ivalidcron");
        hbJobScheduler.scheduleJob(c2);
    }

    private boolean isJobScheduledForCollector(String collectorId, Scheduler scheduler) throws Exception
    {
        String jobName = "heartbeat-" + collectorId;
        String triggerName = jobName + "-Trigger";
        return scheduler.checkExists(new JobKey(jobName, Scheduler.DEFAULT_GROUP))
                && scheduler.checkExists(new TriggerKey(triggerName, Scheduler.DEFAULT_GROUP));
    }

}
