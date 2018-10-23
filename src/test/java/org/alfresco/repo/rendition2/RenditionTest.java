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
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.thumbnail.ThumbnailDefinition;
import org.alfresco.repo.thumbnail.ThumbnailRegistry;
import org.alfresco.service.cmr.rendition.RenditionService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.util.ApplicationContextHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ResourceUtils;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import static java.lang.Thread.sleep;
import static junit.framework.TestCase.assertEquals;
import static org.alfresco.repo.content.MimetypeMap.EXTENSION_BINARY;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test it is possible to create renditions from the quick files.
 *
 * @author adavis
 */
public class RenditionTest extends AbstractRenditionIntegrationTest
{
    @Autowired
    private MimetypeMap mimetypeMap;
    @Autowired
    private RenditionService2Impl renditionService2;
    @Autowired
    private TransformClient transformClient;
    @Autowired
    private PermissionService permissionService;
    @Autowired
    private RenditionService renditionService;
    @Autowired
    private ThumbnailRegistry thumbnailRegistry;

    private static final String ADMIN = "admin";

    @BeforeClass
    public static void before()
    {
        // Ensure other applications contexts are closed...
        // Multiple consumers not supported for same direct vm in different Camel contexts.
        ApplicationContextHelper.closeApplicationContext();
    }

    @Before
    public void setUp()
    {
        assertTrue("The RenditionService2 needs to be enabled", renditionService2.isEnabled());
    }

    @After
    public void cleanUp()
    {
        AuthenticationUtil.clearCurrentSecurityContext();
    }

    private void checkRendition(String testFileName, String renditionName, boolean expectedToPass)
    {
        try
        {
            NodeRef sourceNodeRef = createSource(ADMIN, testFileName);
            render(ADMIN, sourceNodeRef, renditionName);
            waitForRendition(ADMIN, sourceNodeRef, renditionName);
            if (!expectedToPass)
            {
                fail("The " + renditionName + " rendition should NOT be supported for " + testFileName);
            }
        }
        catch(UnsupportedOperationException e)
        {
            if (expectedToPass)
            {
                fail("The " + renditionName + " rendition SHOULD be supported for " + testFileName);
            }
        }
        catch (AssertionFailedError e)
        {
            throw e;
        }
    }

    // Creates a new source node as the given user in its own transaction.
    private NodeRef createSource(String user, String testFileName)
    {
        return AuthenticationUtil.runAs(() ->
                transactionService.getRetryingTransactionHelper().doInTransaction(() ->
                        createSource(testFileName)), user);
    }

    // Creates a new source node as the current user in the current transaction.
    private NodeRef createSource(String testFileName) throws FileNotFoundException
    {
        return createContentNodeFromQuickFile(testFileName);
    }

    // Requests a new rendition as the given user in its own transaction.
    private void render(String user, NodeRef sourceNode, String renditionName)
    {
        AuthenticationUtil.runAs((AuthenticationUtil.RunAsWork<Void>) () ->
                transactionService.getRetryingTransactionHelper().doInTransaction(() ->
                {
                    render(sourceNode, renditionName);
                    return null;
                }), user);
    }

    // Requests a new rendition as the current user in the current transaction.
    private void render(NodeRef sourceNodeRef, String renditionName)
    {
        renditionService2.render(sourceNodeRef, renditionName);
    }

    // As a given user waitForRendition for a rendition to appear. Creates new transactions to do this.
    private NodeRef waitForRendition(String user, NodeRef sourceNodeRef, String renditionName) throws AssertionFailedError
    {
        try
        {
            return AuthenticationUtil.runAs(() -> waitForRendition(sourceNodeRef, renditionName), user);
        }
        catch (RuntimeException e)
        {
            Throwable cause = e.getCause();
            if (cause instanceof AssertionFailedError)
            {
                throw (AssertionFailedError)cause;
            }
            throw e;
        }
    }

    // As the current user waitForRendition for a rendition to appear. Creates new transactions to do this.
    private NodeRef waitForRendition(NodeRef sourceNodeRef, String renditionName) throws InterruptedException
    {
        long maxMillis = 2000;
        ChildAssociationRef assoc = null;
        for (int i = (int)(maxMillis / 500); i >= 0; i--)
        {
            // Must create a new transaction in order to see changes that take place after this method started.
            assoc = transactionService.getRetryingTransactionHelper().doInTransaction(() ->
                    renditionService2.getRenditionByName(sourceNodeRef, renditionName), true, true);
            if (assoc != null)
            {
                break;
            }
            logger.debug("RenditionService2.getRenditionByName(...) sleep "+i);
            sleep(500);
        }
        assertNotNull("Rendition " + renditionName + " failed", assoc);
        return assoc.getChildRef();
    }

    private String getTestFileName(String sourceMimetype) throws FileNotFoundException
    {
        String extension = mimetypeMap.getExtension(sourceMimetype);
        String testFileName = extension.equals(EXTENSION_BINARY) ? null : "quick."+extension;
        if (testFileName != null)
        {
            try
            {
                ResourceUtils.getFile("classpath:quick/" + testFileName);
            }
            catch (FileNotFoundException e)
            {
                testFileName = null;
            }
        }
        return testFileName;
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
                Set<String> renditionNamesFromSourceMimetype = renditionDefinitionRegistry2.getRenditionNamesFrom(sourceMimetype, -1);
                List<ThumbnailDefinition> thumbnailDefinitions = thumbnailRegistry.getThumbnailDefinitions(sourceMimetype, -1);
                assertEquals("There should be the same number of renditions ("+renditionNamesFromSourceMimetype+
                        ") as deprecated thumbnails ("+thumbnailDefinitions+")",
                        renditionNamesFromSourceMimetype.size(), thumbnailDefinitions.size());

                renditionCount += renditionNamesFromSourceMimetype.size();
                for (String renditionName : renditionNamesFromSourceMimetype)
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
                Collections.emptyList(), Collections.emptyList(), 7, 0, 7);
    }

}
