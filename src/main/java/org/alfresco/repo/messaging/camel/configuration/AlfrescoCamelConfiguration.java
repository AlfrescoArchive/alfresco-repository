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
package org.alfresco.repo.messaging.camel.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.camel.CamelContext;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.core.xml.CamelJMXAgentDefinition;
import org.apache.camel.impl.ExplicitCamelContextNameStrategy;
import org.apache.camel.spring.javaconfig.CamelConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Class describes Camel configuration and supporting beans
 */
@Configuration
@ComponentScan
public class AlfrescoCamelConfiguration extends CamelConfiguration
{
    public static String CAMEL_CONTEXT_NAME = "alfrescoCamelContext";

    @Override
    protected void setupCamelContext(CamelContext camelContext) throws Exception
    {
        camelContext.setNameStrategy(new ExplicitCamelContextNameStrategy(CAMEL_CONTEXT_NAME));
        super.setupCamelContext(camelContext);
    }

    @Bean
    public CamelJMXAgentDefinition agent()
    {
        CamelJMXAgentDefinition camelJMXAgentDefinition = new CamelJMXAgentDefinition();
        camelJMXAgentDefinition.setMbeanObjectDomainName("Alfresco.Camel");
        return camelJMXAgentDefinition;
    }

    @Bean
    public ObjectMapper alfrescoEventObjectMapper()
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        return mapper;
    }

    @Bean
    public JacksonDataFormat defaultDataFormat()
    {
        return new JacksonDataFormat(alfrescoEventObjectMapper(), Object.class);
    }
}
