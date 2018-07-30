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
import org.alfresco.service.transform.TransformServiceRegistryImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class TransformServiceRegistryImplTest
{
    private TransformServiceRegistry registry;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        registry = new TransformServiceRegistryImpl();

    }

    @Test
    public void testDictionaryGetProperties()
    {

        Map<String, String> props = new HashMap<>();
        props.put("timeout","636");
        assertTrue(registry.isSupported("docx", "pdf", props));
        assertTrue(!registry.isSupported("docx", "pdf", new HashMap<String, String>()));
        assertTrue(!registry.isSupported("pdf", "docx", props));
    }
}
