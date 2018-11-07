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
package org.alfresco.repo.security.authentication.identityservice;

import org.alfresco.repo.security.authentication.AuthenticationContext;
import org.alfresco.repo.security.sync.UserRegistrySynchronizer;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.ApplicationContextHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.util.HttpResponseException;
import org.keycloak.representations.AccessTokenResponse;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IdentityServiceAuthenticationComponentTest
{

    ApplicationContext ctx = ApplicationContextHelper.getApplicationContext();
    private IdentityServiceAuthenticationComponent authComponent = new IdentityServiceAuthenticationComponent();
    AuthenticationContext authenticationContext;

    @Mock
    AuthzClient mockAuthzClient;

    @Before
    public void setUp()
    {

        authenticationContext = (AuthenticationContext) ctx.getBean("authenticationContext");
        TransactionService transactionService = (TransactionService) ctx.getBean("transactionService");
        UserRegistrySynchronizer userRegistrySynchronizer = (UserRegistrySynchronizer) ctx.getBean("userRegistrySynchronizer");
        NodeService nodeService = (NodeService) ctx.getBean("nodeService");
        PersonService personService = (PersonService) ctx.getBean("personService");

        authComponent.setKeycloakAuthzClient(mockAuthzClient);
        authComponent.setAuthenticationContext(authenticationContext);
        authComponent.setTransactionService(transactionService);
        authComponent.setUserRegistrySynchronizer(userRegistrySynchronizer);
        authComponent.setNodeService(nodeService);
        authComponent.setPersonService(personService);

    }

    @After
    public void tearDown()
    {
        authenticationContext.clearCurrentSecurityContext();
    }

    @Test ( expected= org.alfresco.repo.security.authentication.AuthenticationException.class)
    public void testAuthenticationFail()
    {

        when(mockAuthzClient.obtainAccessToken("username", "password"))
                .thenThrow(new HttpResponseException("Unauthorized", 401, "Unauthorized", null));

        authComponent.authenticateImpl("username", "password".toCharArray());
    }

    @Test
    public void testAuthenticationPass()
    {
        when(mockAuthzClient.obtainAccessToken("username", "password"))
                .thenReturn(new AccessTokenResponse());

        authComponent.authenticateImpl("username", "password".toCharArray());

        // Check that the authenticated user has been set
        assertEquals("User has not been set as expected.","username", authenticationContext.getCurrentUserName());
    }
}