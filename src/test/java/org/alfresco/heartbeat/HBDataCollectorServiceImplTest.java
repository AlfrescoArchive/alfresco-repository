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
import org.alfresco.service.cmr.repository.HBDataCollectorService;
import org.alfresco.service.license.LicenseDescriptor;
import org.alfresco.service.license.LicenseException;
import org.alfresco.service.license.LicenseService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author eknizat
 */
public class HBDataCollectorServiceImplTest
{

    private HBDataCollectorServiceImpl dataCollectorService;
    private HBDataSenderService mockDataSenderService;
    private LicenseService mockLicenseService;
    private JobLockService mockJobLockService;

    @Before
    public void setUp()
    {
        mockDataSenderService = mock(HBDataSenderService.class);
        mockLicenseService = mock(LicenseService.class);
        mockJobLockService = mock(JobLockService.class);

        dataCollectorService = spy(new HBDataCollectorServiceImpl(true));
        dataCollectorService.setHbDataSenderService(mockDataSenderService);
        dataCollectorService.setJobLockService(mockJobLockService);

        mockLicenseService.registerOnLicenseChange(dataCollectorService);
    }

    @Test
    public void testInitialEnabledEqualsDefaultState() throws Exception
    {
        HBDataCollectorService dataCollectorService = new HBDataCollectorServiceImpl(true);
        assertTrue(dataCollectorService.isEnabledByDefault());

        dataCollectorService = new HBDataCollectorServiceImpl(false);
        assertFalse(dataCollectorService.isEnabledByDefault());
    }

    @Test
    public void testHBDataSenderServiceEnabledChange() throws Exception
    {
        dataCollectorService.enabled(false);
        verify(mockDataSenderService).enable(false);

        dataCollectorService.enabled(true);
        verify(mockDataSenderService).enable(true);
    }

    @Test
    public void testRegisterAndScheduleCollectors() throws Exception
    {
        LicenseDescriptor mockLicenseDescriptor = mock(LicenseDescriptor.class);
        when(mockLicenseDescriptor.isHeartBeatDisabled()).thenReturn(false);

        HBDataCollectorServiceImpl dataCollectorService = new HBDataCollectorServiceImpl(true);
        // activate HB
        dataCollectorService.onLicenseChange(mockLicenseDescriptor);
        // Setup scheduler
        Scheduler scheduler = spy(StdSchedulerFactory.getDefaultScheduler());
        dataCollectorService.setScheduler(scheduler);

        SimpleHBDataCollector collector = new SimpleHBDataCollector("collectorId123");
        collector.setCronExpression("0 0 0/1 * * ?");

        dataCollectorService.registerCollector(collector);

        verify(scheduler, Mockito.times(1)).unscheduleJob(any(String.class), any(String.class));
        verify(scheduler, Mockito.times(1)).scheduleJob(any(JobDetail.class), any(Trigger.class));

        // try to register the same collector again
        dataCollectorService.registerCollector(collector);
        verify(scheduler, Mockito.times(1)).unscheduleJob(any(String.class), any(String.class));
        verify(scheduler, Mockito.times(1)).scheduleJob(any(JobDetail.class), any(Trigger.class));

        // use wrong cron exp
        collector.setCronExpression("not correct cron Expr");
        verify(scheduler, Mockito.times(1)).scheduleJob(any(JobDetail.class), any(Trigger.class));

        // register other collector
        SimpleHBDataCollector otherCollector = new SimpleHBDataCollector("collectorIdABC");
        otherCollector.setCronExpression("0 0 0/1 * * ?");

        dataCollectorService.registerCollector(otherCollector);

        verify(scheduler, Mockito.times(2)).unscheduleJob(any(String.class), any(String.class));
        verify(scheduler, Mockito.times(2)).scheduleJob(any(JobDetail.class), any(Trigger.class));
    }

    private class SimpleHBDataCollector extends HBBaseDataCollector
    {

        public SimpleHBDataCollector(String collectorId)
        {
            super(collectorId);
        }

        public List<HBData> collectData()
        {
            List<HBData> result = new LinkedList<>();
            result.add(new HBData("systemId2", this.getCollectorId(), "1", new Date()));
            return result;
        }
    }

//    @Test
//    public void testOnLicensFail() throws Exception
//    {
//        mockLicenseService.registerOnLicenseChange(dataCollectorService);
//        doThrow(new LicenseException("")).when(mockLicenseService).verifyLicense();
//
//        try
//        {
//            mockLicenseService.verifyLicense();
//                        fail("LicenseException should have thrown");
//        }
//        catch(LicenseException le)
//        {
//            verify(dataCollectorService, Mockito.times(1)).onLicenseFail();
//        }
//    }
//
//    @Test
//    public void testOnLicensChange() throws Exception
//    {
//        mockLicenseService.registerOnLicenseChange(dataCollectorService);
//
//        mockLicenseService.verifyLicense();
//
//        verify(dataCollectorService, Mockito.times(1)).onLicenseChange(any(LicenseDescriptor.class));
//    }
}
