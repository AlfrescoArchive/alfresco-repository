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

import org.alfresco.rest.framework.resource.parameters.Params;
import org.alfresco.util.TransformServiceDictionary;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class TransformServiceDictionaryTest
{
    private TransformServiceDictionary dictionary;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        dictionary = TransformServiceDictionary.getInstance();

    }

    @Test
    public void testDictionaryGetProperties()
    {
        Map<String , Map<String, List<Params>>> propertyMap = dictionary.getDictionary();

        assertTrue(propertyMap.containsKey("docx"));
        assertTrue(propertyMap.get("docx").containsKey("pdf"));
        assertTrue(!propertyMap.get("docx").containsKey("dummy1"));
    }
}
