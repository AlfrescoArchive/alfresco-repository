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

import org.alfresco.heartbeat.datasender.HBDataSenderService;
import org.alfresco.repo.lock.JobLockService;
import org.alfresco.service.cmr.repository.HBDataCollectorService;
import org.alfresco.service.license.LicenseService;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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

        dataCollectorService = new HBDataCollectorServiceImpl(true);
        dataCollectorService.setHbDataSenderService(mockDataSenderService);
        dataCollectorService.setJobLockService(mockJobLockService);

        mockLicenseService.registerOnLicenseChange(dataCollectorService);
    }

    @Test
    public void testInitialEnabledEqualsDefaultState()
    {
        HBDataCollectorService dataCollectorService = new HBDataCollectorServiceImpl(true);
        assertTrue(dataCollectorService.isEnabledByDefault());

        dataCollectorService = new HBDataCollectorServiceImpl(false);
        assertFalse(dataCollectorService.isEnabledByDefault());
    }


    @Test
    public void testHBDataSenderServiceEnabledChange()
    {
        dataCollectorService.enabled(false);
        verify(mockDataSenderService).enable(false);

        dataCollectorService.enabled(true);
        verify(mockDataSenderService).enable(true);
    }
}
