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
package org.alfresco.repo.rendition2;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

import static org.alfresco.repo.content.MimetypeMap.MIMETYPE_OPENXML_WORDPROCESSING;
import static org.alfresco.repo.content.MimetypeMap.MIMETYPE_PDF;

/**
 * Integration tests for {@link LegacyLocalTransformServiceRegistry}
 */
public class LegacyLocalTransformServiceRegistryTest extends AbstractRenditionIntegrationTest
{
    @Autowired
    private LegacyLocalTransformServiceRegistry legacyLocalTransformServiceRegistry;

    @Test
    public void testIsSupported()
    {
        // +ve
        // Source, Target and Props are in dictionary.properties
        Map<String, String> badValidationProps = new HashMap<>();
        badValidationProps.put("timeout", "true");
        // No props
        Assert.assertTrue(legacyLocalTransformServiceRegistry.isSupported(MIMETYPE_OPENXML_WORDPROCESSING, -1, MIMETYPE_PDF, new HashMap<>(), null));

        // -ve
        // Bad Source
        Assert.assertFalse(legacyLocalTransformServiceRegistry.isSupported("docxBad", -1, MIMETYPE_PDF, new HashMap<>(), ""));
        // Bad Target
        Assert.assertFalse(legacyLocalTransformServiceRegistry.isSupported(MIMETYPE_OPENXML_WORDPROCESSING, -1, "pdfBad", new HashMap<>(), null));

        // Good MaxSize docx max size is 768K
        Assert.assertTrue(legacyLocalTransformServiceRegistry.isSupported(MIMETYPE_OPENXML_WORDPROCESSING, 768L*1024, MIMETYPE_PDF, new HashMap<>(), null));

        // -ve
        // Bad MaxSize docx max size is 768K
        Assert.assertFalse(legacyLocalTransformServiceRegistry.isSupported(MIMETYPE_OPENXML_WORDPROCESSING, 768L*1024+1, MIMETYPE_PDF, new HashMap<>(), null));
    }

    @Test
    public void testEnabledDisabled()
    {
        Assert.assertTrue(legacyLocalTransformServiceRegistry.isSupported(MIMETYPE_OPENXML_WORDPROCESSING, -1, MIMETYPE_PDF, new HashMap<>(), null));
        try
        {
            legacyLocalTransformServiceRegistry.setEnabled("false");
            Assert.assertFalse(legacyLocalTransformServiceRegistry.isSupported(MIMETYPE_OPENXML_WORDPROCESSING, -1, MIMETYPE_PDF, new HashMap<>(), null));
        }
        finally
        {
            legacyLocalTransformServiceRegistry.setEnabled("true");
        }
        Assert.assertTrue(legacyLocalTransformServiceRegistry.isSupported(MIMETYPE_OPENXML_WORDPROCESSING, -1, MIMETYPE_PDF, new HashMap<>(), null));
    }
}
