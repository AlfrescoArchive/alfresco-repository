/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2017 Alfresco Software Limited
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
package org.alfresco.repo.content.transform;

import org.alfresco.service.transform.TransformServiceRegistry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:alfresco/subsystems/TransformService/transform-service-context.xml"})
public class TransformServiceRegistryImplTest
{
    private TransformServiceRegistry registry;
    @Autowired
    protected ApplicationContext applicationContext;

    @Before
    public void setUp() throws Exception
    {
        registry = (TransformServiceRegistry) applicationContext.getBean("transformServiceRegistryContext");
    }

    @Test
    public void testIsSupportedTransformationRegistry()
    {
        // +ve
        // Source, Target and Props are in dictionary.properties
        Map<String, String> props = new HashMap<>();
        props.put("timeout","636");
        Assert.assertTrue(registry.isSupported("docx", "pdf", props));

        // -ve
        // Bad Source
        Assert.assertFalse(registry.isSupported("docxBad", "pdf", props));
        // Bad Target
        Assert.assertFalse(registry.isSupported("docx", "pdfBad", props));
        // Bad Props
        Assert.assertFalse(registry.isSupported("docx", "pdf", new HashMap<>()));
        // Bad Source and Target
        Assert.assertFalse(registry.isSupported("docxBad", "pdfBad", props));
        // Bad Source and Props
        Assert.assertFalse(registry.isSupported("docxBad", "pdf", new HashMap<>()));
        // Bad Target and Props
        Assert.assertFalse(registry.isSupported("docx", "pdfBad", new HashMap<>()));
        // Bad Source Target and Props
        Assert.assertFalse(registry.isSupported("docxBad", "pdfBad", new HashMap<>()));
    }
}
