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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.HttpClient;
import org.keycloak.adapters.HttpClientBuilder;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.springframework.beans.factory.FactoryBean;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 *
 * Creates an instance of {@link AuthzClient}.
 * <br>
 * If no secret is provided through the {@link IdentityServiceConfig}, then an empty secret is added.
 * This is to allow the {@link AuthzClient} to communicate when the client referred to by the resource property is configured as public.
 *
 */
public class AuthzClientFactoryBean implements FactoryBean<AuthzClient>
{

    private static Log logger = LogFactory.getLog(AuthzClientFactoryBean.class);
    private IdentityServiceConfig identityServiceConfig;

    public void setIdentityServiceConfig(IdentityServiceConfig identityServiceConfig)
    {
        this.identityServiceConfig = identityServiceConfig;
    }

    @Override
    public AuthzClient getObject() throws Exception
    {

        // Build default http client using the keycloak client builder.
        int conTimeout = identityServiceConfig.getClientConnectionTimeout();
        int socTimeout = identityServiceConfig.getClientSocketTimeout();
        HttpClient client = new HttpClientBuilder()
                .establishConnectionTimeout(conTimeout, TimeUnit.MILLISECONDS)
                .socketTimeout(socTimeout, TimeUnit.MILLISECONDS)
                .build(this.identityServiceConfig);

        // Add secret to credentials if needed.
        // AuthzClient configuration needs credentials with a secret even if the client in Keycloak is configured as public.
        Map<String, Object> credentials = identityServiceConfig.getCredentials();
        if (credentials == null || !credentials.containsKey("secret"))
        {
            credentials = credentials == null ? new HashMap<>() : new HashMap<>(credentials);
            credentials.put("secret", "");
        }

        // Create default AuthzClient for authenticating users against keycloak
        String authServerUrl = identityServiceConfig.getAuthServerUrl();
        String realm = identityServiceConfig.getRealm();
        String resource = identityServiceConfig.getResource();
        Configuration authzConfig = new Configuration(authServerUrl, realm, resource, credentials, client);
        AuthzClient  authzClient = AuthzClient.create(authzConfig);

        if (logger.isDebugEnabled())
        {
            logger.debug(" Created Keycloak AuthzClient");
            logger.debug(" Keycloak AuthzClient server URL: " + authzClient.getConfiguration().getAuthServerUrl());
            logger.debug(" Keycloak AuthzClient realm: " + authzClient.getConfiguration().getRealm());
            logger.debug(" Keycloak AuthzClient resource: " + authzClient.getConfiguration().getResource());
        }
        return authzClient;
    }

    @Override
    public Class<?> getObjectType()
    {
        return AuthzClientFactoryBean.class;
    }

    @Override
    public boolean isSingleton()
    {
        return true;
    }
}
