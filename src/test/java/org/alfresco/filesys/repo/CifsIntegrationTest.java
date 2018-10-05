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
package org.alfresco.filesys.repo;

import org.alfresco.jlan.server.config.ServerConfigurationAccessor;
import org.alfresco.jlan.server.filesys.DiskSharedDevice;
import org.alfresco.jlan.server.filesys.FilesystemsConfigSection;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.util.BaseAlfrescoTestCase;
import org.junit.Test;

/**
 * Checks that the required configuration details are obtainable from the CIFS components.
 * 
 * @author Derek Hulley
 */
public class CifsIntegrationTest extends BaseAlfrescoTestCase
{

    @org.junit.Test
    public void testGetServerName()
    {
        ServerConfigurationAccessor config = (ServerConfigurationAccessor) applicationContext.getBean("fileServerConfiguration");
        assertNotNull("No file server config available", config);
        // the server might, quite legitimately, not start
        if (!config.isServerRunning( "CIFS"))
        {
            return;
        }
        
        // get the server name
        String serverName = config.getServerName();
        assertNotNull("No server name available", serverName);
        assertTrue("No server name available (zero length)", serverName.length() > 0);

        // Get the primary filesystem, might be null if the home folder mapper is configured
        
        FilesystemsConfigSection filesysConfig = (FilesystemsConfigSection) config.getConfigSection(FilesystemsConfigSection.SectionName);
        DiskSharedDevice mainFilesys = (DiskSharedDevice) filesysConfig.getShares().enumerateShares().nextElement();
        
        if ( mainFilesys != null)
        {
            // Check the share name
            
            String shareName = mainFilesys.getName();
            assertNotNull("No share name available", shareName);
            assertTrue("No share name available (zero length)", shareName.length() > 0);

            // Check that the context is valid
            
            ContentContext filesysapplicationContext = (ContentContext) mainFilesys.getContext();
            assertNotNull("Content context is null", filesysapplicationContext);
            assertNotNull("Store id is null", filesysapplicationContext.getStoreName());
            assertNotNull("Root path is null", filesysapplicationContext.getRootPath());
            assertNotNull("Root node is null", filesysapplicationContext.getRootNode());
            
            // Check the root node
            
            NodeService nodeService = (NodeService) applicationContext.getBean(ServiceRegistry.NODE_SERVICE.getLocalName());
            // get the share root node and check that it exists
            NodeRef shareNodeRef = filesysapplicationContext.getRootNode();
            assertNotNull("No share root node available", shareNodeRef);
            assertTrue("Share root node doesn't exist", nodeService.exists(shareNodeRef));
        }
    }
}
