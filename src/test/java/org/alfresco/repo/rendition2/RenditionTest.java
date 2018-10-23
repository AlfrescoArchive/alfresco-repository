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

import junit.framework.AssertionFailedError;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.thumbnail.ThumbnailDefinition;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Test it is possible to create renditions from the quick files.
 *
 * @author adavis
 */
public class RenditionTest extends AbstractRenditionIntegrationTest
{
    @Before
    public void setUp() throws Exception
    {
        super.setUp();
        AuthenticationUtil.setRunAsUser(AuthenticationUtil.getAdminUserName());
    }

    private Set<String> getThumbnailNames(List<ThumbnailDefinition> thumbnailDefinitions)
    {

        Set<String> names = new HashSet<>();
        for (ThumbnailDefinition thumbnailDefinition : thumbnailDefinitions)
        {
            String name = thumbnailDefinition.getName();
            names.add(name);
        }
        return names;
    }

    @Test
    public void testExpectedNumberOfRenditions() throws Exception
    {
        RenditionDefinitionRegistry2 renditionDefinitionRegistry2 = renditionService2.getRenditionDefinitionRegistry2();
        Set<String> renditionNames = renditionDefinitionRegistry2.getRenditionNames();
        assertEquals("Added or removed a definition (rendition-service2-contex.xml)?", 7, renditionNames.size());
    }

    List<String> KNOWN_FAILURES = Arrays.asList(new String[]{
            "docx jpg imgpreview",
            "docx jpg medium",

            "xlsx jpg imgpreview",
            "xlsx jpg medium",

            "key jpg imgpreview",
            "key jpg medium",

            "pages jpg imgpreview",
            "pages jpg medium",

            "numbers jpg imgpreview",
            "numbers jpg medium",
    });

    @Test
    public void testTasRestApiRenditions() throws Exception
    {
        assertRenditionsOkayFromSourceExtension(Arrays.asList("doc", "xls", "ppt", "docx", "xlsx", "pptx", "msg", "pdf", "png", "gif", "jpg"),
                Collections.emptyList(), Collections.emptyList(), 77, 6, 71);
    }

    @Test
    public void testAllSourceExtensions() throws Exception
    {
        List<String> sourceExtensions = new ArrayList<>();
        for (String sourceMimetype : mimetypeMap.getMimetypes())
        {
            String sourceExtension = mimetypeMap.getExtension(sourceMimetype);
            sourceExtensions.add(sourceExtension);
        }
        assertRenditionsOkayFromSourceExtension(sourceExtensions, KNOWN_FAILURES, Collections.emptyList(),413, 3, 400);
    }

    private void assertRenditionsOkayFromSourceExtension(List<String> sourceExtensions, List<String> excludeList, List<String> expectedToFail,
                                                         int expectedRenditionCount, int expectedFailedCount, int expectedSuccessCount) throws Exception
    {
        int renditionCount = 0;
        int failedCount = 0;
        int successCount = 0;
        RenditionDefinitionRegistry2 renditionDefinitionRegistry2 = renditionService2.getRenditionDefinitionRegistry2();
        StringJoiner failures = new StringJoiner("\n");
        StringJoiner successes = new StringJoiner("\n");

        for (String sourceExtension : sourceExtensions)
        {
            String sourceMimetype = mimetypeMap.getMimetype(sourceExtension);
            String testFileName = getTestFileName(sourceMimetype);
            if (testFileName != null)
            {
                Set<String> renditionNames = renditionDefinitionRegistry2.getRenditionNamesFrom(sourceMimetype, -1);
                List<ThumbnailDefinition> thumbnailDefinitions = thumbnailRegistry.getThumbnailDefinitions(sourceMimetype, -1);
                Set<String> thumbnailNames = getThumbnailNames(thumbnailDefinitions);
                assertEquals("There should be the same number of renditions ("+renditionNames+
                        ") as deprecated thumbnails ("+thumbnailNames+")", renditionNames, thumbnailDefinitions);

                renditionCount += renditionNames.size();
                for (String renditionName : renditionNames)
                {
                    RenditionDefinition2 renditionDefinition = renditionDefinitionRegistry2.getRenditionDefinition(renditionName);
                    String targetMimetype = renditionDefinition.getTargetMimetype();
                    String targetExtension = mimetypeMap.getExtension(targetMimetype);

                    String sourceTragetRendition = sourceExtension + ' ' + targetExtension + ' ' + renditionName;
                    if (!excludeList.contains(sourceTragetRendition))
                    {
                        String task = sourceExtension + " to " + targetExtension + " for " + renditionName;

                        try
                        {
                            checkRendition(testFileName, renditionName, expectedToFail.contains(sourceTragetRendition));
                            successes.add(task);
                            successCount++;
                        }
                        catch (AssertionFailedError e)
                        {
                            failures.add(task + " " + e.getMessage());
                            failedCount++;
                        }
                    }
                }
            }
        }
        System.out.println("FAILURES:\n"+failures+"\n");
        System.out.println("SUCCESSES:\n"+successes+"\n");

        assertEquals("Rendition count has changed", expectedRenditionCount, renditionCount);
        assertEquals("Failed rendition count has changed", expectedFailedCount, failedCount);
        assertEquals("Successful rendition count has changed", expectedSuccessCount, successCount);
//        if (failures.length() > 0)
//        {
//            fail(failures.toString());
//        }
    }

    @Test
    public void testGifRenditions() throws Exception
    {
        assertRenditionsOkayFromSourceExtension(Arrays.asList("gif"),
                Collections.emptyList(), Collections.emptyList(), 5, 0, 5);
    }

}
