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
import org.alfresco.service.license.LicenseDescriptor;
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
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

    @Before
    public void setUp()
    {
        mockDataSenderService = mock(HBDataSenderService.class);

        dataCollectorService = spy(new HBDataCollectorServiceImpl(true));
        dataCollectorService.setHbDataSenderService(mockDataSenderService);
    }

    @Test
    public void testInitialEnabledEqualsDefaultState() throws Exception
    {
        HBDataCollectorServiceImpl dataCollectorService = new HBDataCollectorServiceImpl(true);
        assertTrue(dataCollectorService.isEnabledByDefault());

        dataCollectorService = new HBDataCollectorServiceImpl(false);
        assertFalse(dataCollectorService.isEnabledByDefault());
    }

    private void activateHB(HBDataCollectorServiceImpl dataCollectorService, boolean activate)
    {
        LicenseDescriptor mockLicenseDescriptor = mock(LicenseDescriptor.class);
        when(mockLicenseDescriptor.isHeartBeatDisabled()).thenReturn(!activate);
        dataCollectorService.onLicenseChange(mockLicenseDescriptor);
    }

    @Test
    public void testWrongCronExpression() throws Exception
    {
        HBDataCollectorServiceImpl dataCollectorService = new HBDataCollectorServiceImpl(true);

        activateHB(dataCollectorService, true);

        Scheduler scheduler = spy(StdSchedulerFactory.getDefaultScheduler());
        dataCollectorService.setScheduler(scheduler);

        // try with correct cron expression
        SimpleHBDataCollector collector1 = new SimpleHBDataCollector("collector1");
        collector1.setCronExpression("0 0 0/1 * * ?");

        dataCollectorService.registerCollector(collector1);

        verifyNumbersOfUnschedulesAndSchedules(scheduler, 1, 1);

        // try with wrong cron expression
        SimpleHBDataCollector collector2 = new SimpleHBDataCollector("collector2");
        collector2.setCronExpression("0 0 0/1 wrong cron expr ?");

        dataCollectorService.registerCollector(collector2);

        verifyNumbersOfUnschedulesAndSchedules(scheduler, 2, 1);
    }

    @Test
    public void testSchedulingWithDefaultEnabled() throws Exception
    {
        // scenario 1: hb enabled per default
        HBDataCollectorServiceImpl dataCollectorService = new HBDataCollectorServiceImpl(true);
        activateHB(dataCollectorService, true);

        // Setup scheduler
        Scheduler scheduler = spy(StdSchedulerFactory.getDefaultScheduler());
        dataCollectorService.setScheduler(scheduler);

        SimpleHBDataCollector collector1 = new SimpleHBDataCollector("collector1");
        collector1.setCronExpression("0 0 0/1 * * ?");

        dataCollectorService.registerCollector(collector1);
        verifyNumbersOfUnschedulesAndSchedules(scheduler, 1, 1);

        // licensing fails and hb is per default enabled but hb was enabled before
        dataCollectorService.onLicenseFail();
        verifyNumbersOfUnschedulesAndSchedules(scheduler, 1, 1);

        // deactivate hb via license
        activateHB(dataCollectorService, false);
        verifyNumbersOfUnschedulesAndSchedules(scheduler, 2, 1);

        // licensing fails and hb is per default enabled but hb was disabled before
        dataCollectorService.onLicenseFail();
        verifyNumbersOfUnschedulesAndSchedules(scheduler, 3, 2);

        // scenario 2: hb not enabled per default
        dataCollectorService = new HBDataCollectorServiceImpl(false);
        activateHB(dataCollectorService, true);

        // Setup scheduler
        scheduler = spy(StdSchedulerFactory.getDefaultScheduler());
        dataCollectorService.setScheduler(scheduler);

        dataCollectorService.registerCollector(collector1);
        verifyNumbersOfUnschedulesAndSchedules(scheduler, 1, 1);

        // licensing fails and hb is per default disabled
        dataCollectorService.onLicenseFail();
        dataCollectorService.registerCollector(collector1);
        verifyNumbersOfUnschedulesAndSchedules(scheduler, 2, 1);
    }

    @Test
    public void testRegisterAndScheduleCollectors() throws Exception
    {
        boolean enabledByDefault = true;
        HBDataCollectorServiceImpl dataCollectorService = new HBDataCollectorServiceImpl(enabledByDefault);
        activateHB(dataCollectorService, true);

        // Setup scheduler
        Scheduler scheduler = spy(StdSchedulerFactory.getDefaultScheduler());
        dataCollectorService.setScheduler(scheduler);

        SimpleHBDataCollector collector1 = new SimpleHBDataCollector("collector1");
        collector1.setCronExpression("0 0 0/1 * * ?");

        dataCollectorService.registerCollector(collector1);

        verifyNumbersOfUnschedulesAndSchedules(scheduler, 1, 1);

        // try to register the same collector again
        dataCollectorService.registerCollector(collector1);
        verifyNumbersOfUnschedulesAndSchedules(scheduler, 1, 1);

        // register collector 2
        SimpleHBDataCollector collector2 = new SimpleHBDataCollector("collector2");
        collector2.setCronExpression("0 0 0/1 * * ?");

        dataCollectorService.registerCollector(collector2);

        verifyNumbersOfUnschedulesAndSchedules(scheduler, 2, 2);

        // disable heartbeat
        activateHB(dataCollectorService, false);

        // all collector jobs should be unscheduled
        verifyNumbersOfUnschedulesAndSchedules(scheduler, 4, 2);

        // you can not schedule successfully a new collector when heartbeat is disabled
        SimpleHBDataCollector collector3 = new SimpleHBDataCollector("collector3");
        collector3.setCronExpression("0 0 0/1 * * ?");
        dataCollectorService.registerCollector(collector3);
        verifyNumbersOfUnschedulesAndSchedules(scheduler, 5, 2);

        // HB enabled after license fail because enabled by default is true
        // as well the already registered collectors are scheduling now again
        dataCollectorService.onLicenseFail();
        verifyNumbersOfUnschedulesAndSchedules(scheduler, 8, 5);
    }

    private void verifyNumbersOfUnschedulesAndSchedules(Scheduler scheduler, int unschedules, int schedules) throws Exception
    {
        verify(scheduler, Mockito.times(unschedules)).unscheduleJob(any(String.class), any(String.class));
        verify(scheduler, Mockito.times(schedules)).scheduleJob(any(JobDetail.class), any(Trigger.class));
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
}
