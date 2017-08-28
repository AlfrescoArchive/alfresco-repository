/*
 * #%L
 * Alfresco Remote API
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
package org.alfresco.rest.api.tests;

import java.util.UUID;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationComponent;
import org.alfresco.service.cmr.quickshare.QuickShareService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.cmr.site.SiteVisibility;
import org.alfresco.util.BaseAlfrescoSpringTest;
import org.junit.Test;

/**
 * Test SharedLink
 *
 * @author Alexandru Epure
 */
@SuppressWarnings("deprecation")
public class SharedLinkApiTest extends BaseAlfrescoSpringTest
{
    private SiteService siteService;
    private QuickShareService quickShareService;

    protected void onSetUpInTransaction() throws Exception
    {
        super.onSetUpInTransaction();
        // Get the required services
        this.authenticationComponent = (AuthenticationComponent) this.applicationContext.getBean("authenticationComponent");
        this.siteService = (SiteService) this.applicationContext.getBean("SiteService");
        this.quickShareService = (QuickShareService) this.applicationContext.getBean("QuickShareService");
    }

    //Test SharedLink deletion by admin user based on REPO-2819
    @Test
    public void testCanDeleteSharedLinkWithAdminUserForAPrivateNode() throws Exception
    {
        String currentUser = authenticationComponent.getCurrentUserName();

        // Create normal user
        String userName = "user" + UUID.randomUUID();
        createUser(userName);

        // Create a private site
        authenticationComponent.setCurrentUser(userName);
        String siteName = "testSite" + UUID.randomUUID();
        siteService.createSite("site-dashboard", siteName, "Title for " + siteName,
                "Description for " + siteName, SiteVisibility.PRIVATE);

        // Create a node on the private site
        String nodeName = "testNode" + UUID.randomUUID();
        NodeRef createdNodeRef = createNode(siteService.getSite(siteName).getNodeRef(), nodeName,
                ContentModel.TYPE_CONTENT);

        // Verify if the admin user "canDeleteSharedLink" 
        authenticationComponent.setCurrentUser("admin");
        boolean canDeleteSharedLink = quickShareService.canDeleteSharedLink(createdNodeRef,
                userName);
        assertEquals(true, canDeleteSharedLink);

        // Clean up
        siteService.deleteSite(siteName);
        deleteUser(userName);
        if (currentUser != null)
        {
            authenticationComponent.setCurrentUser(currentUser);
        }
    }

}
