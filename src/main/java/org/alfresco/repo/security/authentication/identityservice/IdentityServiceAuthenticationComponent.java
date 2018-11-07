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

import org.alfresco.repo.management.subsystems.ActivateableBean;
import org.alfresco.repo.security.authentication.AbstractAuthenticationComponent;
import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.util.PropertyCheck;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.util.HttpResponseException;
import org.springframework.beans.factory.InitializingBean;

/**
 *
 * Authenticates a user against Keycloak.
 * Keycloak's {@link AuthzClient} is used to retrieve an access token for the provided user credentials,
 * user is set as the current user if the user's access token can be obtained.
 *
 */
public class IdentityServiceAuthenticationComponent extends AbstractAuthenticationComponent implements InitializingBean,
        ActivateableBean
{
    private final Log logger = LogFactory.getLog(IdentityServiceAuthenticationComponent.class);
    private AuthzClient authzClient;
    private boolean active;

    public void setKeycloakAuthzClient(AuthzClient keycloakAuthzClient)
    {
        this.authzClient = keycloakAuthzClient;
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        PropertyCheck.mandatory(this, "authzClient", authzClient);
    }

    public void authenticateImpl(String userName, char[] password) throws AuthenticationException
    {
        try
        {
            // Attempt to get an access token using the user credentials
            authzClient.obtainAccessToken(userName, new String(password));

            // Successfully obtained access token so treat as authenticated user
            setCurrentUser(userName);
        }
        catch (HttpResponseException e)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Failed to authenticate user against Keycloak. Status: " + e.getStatusCode() + " Reason: "+ e.getReasonPhrase());
            }

            throw new AuthenticationException("Failed to authenticate user " + userName + " against Keycloak.", e);
        }
    }

    public void setActive(boolean active)
    {
        this.active = active;
    }

    @Override
    public boolean isActive()
    {
        return active;
    }

    @Override
    protected boolean implementationAllowsGuestLogin()
    {
        return true;
    }
}
