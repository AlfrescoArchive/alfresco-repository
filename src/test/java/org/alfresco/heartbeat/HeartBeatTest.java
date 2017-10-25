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

import org.alfresco.repo.lock.JobLockService;
import org.alfresco.repo.lock.LockAcquisitionException;
import org.alfresco.service.cmr.repository.HBDataCollectorService;
import org.alfresco.service.license.LicenseDescriptor;
import org.alfresco.service.license.LicenseService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Before;
import org.junit.Test;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author eknizat
 */
public class HeartBeatTest
{

    private static final String[] CONFIG_LOCATIONS = new String[] {
            "classpath:alfresco/scheduler-core-context.xml",
            "classpath:org/alfresco/heartbeat/test-heartbeat-context.xml"};
    private ApplicationContext context;

    LicenseService mockLicenseService;
    JobLockService mockJobLockService;
    HBDataCollectorService mockDataCollectorService;

    @Before
    public void setUp()
    {
        // New context with scheduler
        context = new ClassPathXmlApplicationContext(CONFIG_LOCATIONS);

        // Add services to context
        mockLicenseService = mock(LicenseService.class);
        mockJobLockService = mock(JobLockService.class);
        mockDataCollectorService = mock(HBDataCollectorService.class);

        ((ConfigurableApplicationContext) context).getBeanFactory().registerSingleton("licenseService",mockLicenseService);
        ((ConfigurableApplicationContext) context).getBeanFactory().registerSingleton("jobLockService",mockJobLockService);
        ((ConfigurableApplicationContext) context).getBeanFactory().registerSingleton("hbDataCollectorService",mockDataCollectorService);
    }

    private class LogAppender extends AppenderSkeleton
    {

        private final List<LoggingEvent> log = new ArrayList<LoggingEvent>();

        @Override
        public boolean requiresLayout()
        {
            return false;
        }

        @Override
        protected void append(final LoggingEvent loggingEvent)
        {
            log.add(loggingEvent);
        }

        @Override
        public void close()
        {
        }

        public List<LoggingEvent> getLog()
        {
            return new ArrayList<LoggingEvent>(log);
        }
    }

    @Test
    public void testInCluster() throws Exception
    {
        // Enable heartbeat in data collector service ( as if set in prop file)
        when(mockDataCollectorService.isEnabledByDefault()).thenReturn(true);

        // initialise heartBeat with static jobLockService
        HeartBeat heartBeat = new HeartBeat(context, true);

        // mock the job context
        JobExecutionContext mockJobExecutionContext = mock(JobExecutionContext.class);
        JobDetail mockJobDetail = mock(JobDetail.class);
        when(mockJobExecutionContext.getJobDetail()).thenReturn(mockJobDetail);

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("heartBeat", heartBeat);
        when(mockJobDetail.getJobDataMap()).thenReturn(jobDataMap);

        // scenario 1:
        // collector job is not locked from an other collector
        HeartBeat.HeartBeatJob hbjob = new HeartBeat.HeartBeatJob();
        hbjob.execute(mockJobExecutionContext);
        verify(mockDataCollectorService, times(1)).collectAndSendData();

        // scenario 2:
        // collector job is locked from an other collector and will throw the lock exception
        when(mockJobLockService.getLock(isA(QName.class),anyLong())).thenThrow(new LockAcquisitionException("", ""));

        // additional we check the log output
        Logger.getLogger(HeartBeat.class).setLevel(Level.DEBUG);
        // Add the log appender to the root logger
        LogAppender logAppender = new LogAppender();
        Logger.getRootLogger().addAppender(logAppender);

        hbjob = new HeartBeat.HeartBeatJob();
        hbjob.execute(mockJobExecutionContext);
        // we still have the one call from scenario 1
        verify(mockDataCollectorService, times(1)).collectAndSendData();

        assertEquals(1, logAppender.getLog().size());
        String debugMsg = (String) logAppender.getLog().get(0).getMessage();
        assertTrue("Debug message didn't contains 'Skipping': " + debugMsg, debugMsg.contains("Skipping"));
    }

    @Test
    public void testHBRegistersWithLicenceService() throws Exception
    {
        HeartBeat heartbeat = new HeartBeat(context,false);

        // Check that HearBeat registers itself with the licence service
        verify(mockLicenseService).registerOnLicenseChange(heartbeat);
    }

    @Test
    public void testJobSchedulingWhenEnabled() throws Exception
    {
        // Enable heartbeat in data collector service ( as if set in prop file)
        when(mockDataCollectorService.isEnabledByDefault()).thenReturn(true);

        HeartBeat heartbeat = new HeartBeat(context,true);

        // Check that the job is scheduled when heartbeat is enabled
        assertTrue("Job was not scheduled but HB is enabled", isJobScheduled());
    }

    @Test
    public void testJobSchedulingWhenDisabled() throws Exception
    {
        // Disable heartbeat in data collector service ( as if set in prop file)
        when(mockDataCollectorService.isEnabledByDefault()).thenReturn(false);

        HeartBeat heartbeat = new HeartBeat(context,true);

        // Check that the job is not scheduled when heartbeat is disabled
        assertFalse("Job was scheduled but HB is disabled", isJobScheduled());
    }

    /**
     * Heartbeat enabled by default but disabled in licence on onLicenseChange
     */
    @Test
    public void testOnLicenseChangeOverridesDefaultEnabled() throws Exception
    {
        // Enable heartbeat in data collector service ( as if set in prop file)
        when(mockDataCollectorService.isEnabledByDefault()).thenReturn(true);

        HeartBeat heartbeat = new HeartBeat(context,true);

        // heartbeat disabled in licence
        LicenseDescriptor mockLicenseDescriptor =  mock(LicenseDescriptor.class);
        when(mockLicenseDescriptor.isHeartBeatDisabled()).thenReturn(true);

        assertTrue(heartbeat.isEnabled());
        assertTrue("Job should be scheduled at this point.",isJobScheduled());

        heartbeat.onLicenseChange(mockLicenseDescriptor);

        // Check heartbeat is disabled and job unscheduled
        assertFalse(heartbeat.isEnabled());
        assertFalse("Job should be unscheduled.",isJobScheduled());
    }

    /**
     * heartbeat disabled by default but enabled in licence
     */
    @Test
    public void testOnLicenseChangeOverridesDefaultDisabled() throws Exception
    {
        // Disable heartbeat in data collector service ( as if set in prop file)
        when(mockDataCollectorService.isEnabledByDefault()).thenReturn(false);

        HeartBeat heartbeat = new HeartBeat(context,true);

        // heartbeat enabled in licence
        LicenseDescriptor mockLicenseDescriptor =  mock(LicenseDescriptor.class);
        when(mockLicenseDescriptor.isHeartBeatDisabled()).thenReturn(false);

        assertFalse(heartbeat.isEnabled());
        assertFalse("Job should not be scheduled at this point.",isJobScheduled());

        heartbeat.onLicenseChange(mockLicenseDescriptor);

        // Check heartbeat is enabled and job unscheduled
        assertTrue(heartbeat.isEnabled());
        assertTrue("Job should be scheduled.",isJobScheduled());
    }

    @Test
    public void testOnLicenceFailRevertsToEnabled() throws Exception
    {
        // Enable heartbeat in data collector service ( as if set in prop file)
        when(mockDataCollectorService.isEnabledByDefault()).thenReturn(true);

        HeartBeat heartbeat = new HeartBeat(context,true);

        // heartbeat disabled in licence
        LicenseDescriptor mockLicenseDescriptor =  mock(LicenseDescriptor.class);
        when(mockLicenseDescriptor.isHeartBeatDisabled()).thenReturn(true);
        heartbeat.onLicenseChange(mockLicenseDescriptor);

        assertFalse(heartbeat.isEnabled());
        assertFalse("Job should not be scheduled at this point.",isJobScheduled());

        // Revert back to default state
        heartbeat.onLicenseFail();

        // Check heartbeat is enabled and job unscheduled
        assertTrue(heartbeat.isEnabled());
        assertTrue("Job should be unscheduled.",isJobScheduled());
    }

    @Test
    public void testOnLicenceFailRevertsToDisabled() throws Exception
    {
        // Disable heartbeat in data collector service ( as if set in prop file)
        when(mockDataCollectorService.isEnabledByDefault()).thenReturn(false);

        HeartBeat heartbeat = new HeartBeat(context,true);

        // heartbeat enabled in licence
        LicenseDescriptor mockLicenseDescriptor =  mock(LicenseDescriptor.class);
        when(mockLicenseDescriptor.isHeartBeatDisabled()).thenReturn(false);
        heartbeat.onLicenseChange(mockLicenseDescriptor);

        assertTrue(heartbeat.isEnabled());
        assertTrue("Job should be scheduled at this point.",isJobScheduled());

        // Revert back to default state
        heartbeat.onLicenseFail();

        // Check heartbeat is disabled and job unscheduled
        assertFalse(heartbeat.isEnabled());
        assertFalse("Job should be unscheduled.",isJobScheduled());
    }

    private boolean isJobScheduled() throws Exception
    {
        Scheduler scheduler = (Scheduler) context.getBean("schedulerFactory");
        String[] jobs = scheduler.getJobNames( Scheduler.DEFAULT_GROUP);
        return Arrays.asList(jobs).contains("heartbeat");
    }
}