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

import org.alfresco.model.ContentModel;
import org.alfresco.model.RenditionModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import static java.lang.Thread.sleep;
import static org.alfresco.model.ContentModel.PROP_CONTENT;

/**
 * Integration tests for {@link RenditionService2}
 */
public class RenditionService2IntegrationTest extends AbstractRenditionIntegrationTest
{
    @Autowired
    private RenditionService2 renditionService2;
    @Autowired
    private TransformClient transformClient;
    @Autowired
    private PermissionService permissionService;

    private static final String ADMIN = "admin";
    private static final String DOC_LIB = "doclib";

    @Before
    public void setUp()
    {
        assertTrue("The RenditionService2 needs to be enabled", renditionService2.isEnabled());
        assertTrue("A wrong type of transform client detected", transformClient instanceof LocalTransformClient);
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
            wait(ADMIN, sourceNodeRef, renditionName);
        }
        catch(UnsupportedOperationException uoe)
        {
            if (expectedToPass)
            {
                fail("The " + renditionName + " rendition should be supported for " + testFileName);
            }
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

    // Changes the content of a source node as the given user in its own transaction.
    private void updateContent(String user, NodeRef sourceNodeRef, String testFileName)
    {
        AuthenticationUtil.runAs((AuthenticationUtil.RunAsWork<Void>) () ->
                transactionService.getRetryingTransactionHelper().doInTransaction(() ->
                {
                    updateContent(sourceNodeRef, testFileName);
                    return null;
                }), user);
    }

    // Changes the content of a source node as the current user in the current transaction.
    private NodeRef updateContent(NodeRef sourceNodeRef, String testFileName) throws FileNotFoundException
    {
        File file = ResourceUtils.getFile("classpath:quick/" + testFileName);
        nodeService.setProperty(sourceNodeRef, ContentModel.PROP_NAME, testFileName);

        ContentWriter contentWriter = contentService.getWriter(sourceNodeRef, ContentModel.PROP_CONTENT, true);
        contentWriter.setMimetype(mimetypeService.guessMimetype(testFileName));
        contentWriter.putContent(file);

        return sourceNodeRef;
    }

    // Clears the content of a source node as the given user in its own transaction.
    private void clearContent(String user, NodeRef sourceNodeRef)
    {
        AuthenticationUtil.runAs((AuthenticationUtil.RunAsWork<Void>) () ->
                transactionService.getRetryingTransactionHelper().doInTransaction(() ->
                {
                    clearContent(sourceNodeRef);
                    return null;
                }), user);
    }

    // Clears the content of a source node as the current user in the current transaction.
    private void clearContent(NodeRef sourceNodeRef)
    {
        nodeService.removeProperty(sourceNodeRef, PROP_CONTENT);
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

    // As a given user wait for a rendition to appear. Creates new transactions to do this.
    private NodeRef wait(String user, NodeRef sourceNodeRef, String renditionName)
    {
        return AuthenticationUtil.runAs(() -> wait(sourceNodeRef, renditionName), user);
    }

    // As the current user wait for a rendition to appear. Creates new transactions to do this.
    private NodeRef wait(NodeRef sourceNodeRef, String renditionName) throws InterruptedException
    {
        long maxMillis = 20000;
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

    // Debugs a source node looking for child associations and indicates if the have the rendition aspects.
    private void debug(String prefix, NodeRef sourceNodeRef)
    {
        if (sourceNodeRef == null)
        {
            System.err.println(prefix+" debug sourceNodeRef=null");
        }
        else if (!nodeService.exists(sourceNodeRef))
        {
            System.err.println(prefix+" debug sourceNodeRef does not exist");
        }
        else
        {
            List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(sourceNodeRef);
            boolean hasRenditionedAspect = nodeService.hasAspect(sourceNodeRef, RenditionModel.ASPECT_RENDITIONED);
            System.err.println(prefix+" debug assocs=" + childAssocs.size() + " hasRenditionedAspect=" + hasRenditionedAspect);
            for (ChildAssociationRef assoc : childAssocs)
            {
                NodeRef childRef = assoc.getChildRef();
                boolean hasRenditionAspect = nodeService.hasAspect(childRef, RenditionModel.ASPECT_RENDITION);
                boolean hasRendition2Aspect = nodeService.hasAspect(childRef, RenditionModel.ASPECT_RENDITION2);
                System.err.println(prefix+" debug child=" + childRef + " hasRenditionAspect=" + hasRenditionAspect + " hasRendition2Aspect=" + hasRendition2Aspect);
            }
        }
    }

    // PDF transformation

    @Test
    public void testLocalRenderPdfToJpegMedium() 
    {
        checkRendition("quick.pdf", "medium", true);
    }

    @Test
    public void testLocalRenderPdfToDoclib() 
    {
        checkRendition("quick.pdf", "doclib", true);
    }

    @Test
    public void testLocalRenderPdfJpegImgpreview() 
    {
        checkRendition("quick.pdf", "imgpreview", true);
    }

    @Test
    public void testLocalRenderPdfPngAvatar() 
    {
        checkRendition("quick.pdf", "avatar", true);
    }

    @Test
    public void testLocalRenderPdfPngAvatar32() 
    {
        checkRendition("quick.pdf", "avatar32", true);
    }

    @Test
    public void testLocalRenderPdfFlashWebpreview() 
    {
        checkRendition("quick.pdf", "webpreview", false);
    }

    // DOCX transformation

    @Test
    public void testLocalRenderDocxJpegMedium() 
    {
        checkRendition("quick.docx", "medium", true);
    }

    @Test
    public void testLocalRenderDocxDoclib() 
    {
        checkRendition("quick.docx", "doclib", true);
    }

    @Test
    public void testLocalRenderDocxJpegImgpreview() 
    {
        checkRendition("quick.docx", "imgpreview", true);
    }

    @Test
    public void testLocalRenderDocxPngAvatar() 
    {
        checkRendition("quick.docx", "avatar", true);
    }

    @Test
    public void testLocalRenderDocxPngAvatar32() 
    {
        checkRendition("quick.docx", "avatar32", true);
    }

    @Test
    public void testLocalRenderDocxFlashWebpreview() 
    {
        checkRendition("quick.docx", "webpreview", false);
    }

    @Test
    public void testLocalRenderDocxPdf() 
    {
        checkRendition("quick.docx", "pdf", true);
    }

    @Test
    public void basicRendition() 
    {
        NodeRef sourceNodeRef = createSource(ADMIN, "quick.jpg");
        render(ADMIN, sourceNodeRef, DOC_LIB);
        wait(ADMIN, sourceNodeRef, DOC_LIB);
    }

    @Test
    public void changedSourceToNullContent() 
    {
        NodeRef sourceNodeRef = createSource(ADMIN, "quick.jpg");
        render(ADMIN, sourceNodeRef, DOC_LIB);
        wait(ADMIN, sourceNodeRef, DOC_LIB);

        clearContent(ADMIN, sourceNodeRef);
        render(ADMIN, sourceNodeRef, DOC_LIB);
        ChildAssociationRef assoc = AuthenticationUtil.runAs(() ->
                renditionService2.getRenditionByName(sourceNodeRef, DOC_LIB), ADMIN);
        assertNull("There should be no rendition as there was no content", assoc);
    }

    @Test
    public void changedSourceToNonNull() 
    {
        NodeRef sourceNodeRef = createSource(ADMIN, "quick.jpg");
        render(ADMIN, sourceNodeRef, DOC_LIB);
        wait(ADMIN, sourceNodeRef, DOC_LIB);

        clearContent(ADMIN, sourceNodeRef);
        render(ADMIN, sourceNodeRef, DOC_LIB);
        ChildAssociationRef assoc = AuthenticationUtil.runAs(() ->
                renditionService2.getRenditionByName(sourceNodeRef, DOC_LIB), ADMIN);
        assertNull("There should be no rendition as there was no content", assoc);

        updateContent(ADMIN, sourceNodeRef, "quick.png");
        wait(ADMIN, sourceNodeRef, DOC_LIB);
    }

    @Test
    public void testCreateRenditionByUser() 
    {
        String userName = createRandomUser();
        NodeRef sourceNodeRef = createSource(userName, "quick.jpg");
        render(userName, sourceNodeRef, DOC_LIB);
        NodeRef renditionNodeRef = wait(userName, sourceNodeRef, DOC_LIB);
        assertNotNull("The rendition was not generated for non-admin user", renditionNodeRef);
    }

    @Test
    public void testReadRenditionByOtherUser() 
    {
        String ownerUserName = createRandomUser();
        NodeRef sourceNodeRef = createSource(ownerUserName, "quick.jpg");
        String otherUserName = createRandomUser();
        permissionService.setPermission(sourceNodeRef, otherUserName, PermissionService.READ, true);
        render(ownerUserName, sourceNodeRef, DOC_LIB);
        NodeRef renditionNodeRef = wait(ownerUserName, sourceNodeRef, DOC_LIB);
        assertNotNull("The rendition is not visible for owner of source node", renditionNodeRef);
        renditionNodeRef = wait(otherUserName, sourceNodeRef, DOC_LIB);
        assertNotNull("The rendition is not visible for non-owner user with read permissions", renditionNodeRef);
        assertEquals("The creator of the rendition is not correct",
                ownerUserName, nodeService.getProperty(sourceNodeRef, ContentModel.PROP_CREATOR));
    }

    @Test
    public void testRenderByReader() 
    {
        String ownerUserName = createRandomUser();
        NodeRef sourceNodeRef = createSource(ownerUserName, "quick.jpg");
        String otherUserName = createRandomUser();
        permissionService.setPermission(sourceNodeRef, otherUserName, PermissionService.READ, true);
        render(otherUserName, sourceNodeRef, DOC_LIB);
        NodeRef renditionNodeRef = wait(ownerUserName, sourceNodeRef, DOC_LIB);
        assertNotNull("The rendition is not visible for owner of source node", renditionNodeRef);
        renditionNodeRef = wait(otherUserName, sourceNodeRef, DOC_LIB);
        assertNotNull("The rendition is not visible for owner of rendition node", renditionNodeRef);
        assertEquals("The creator of the rendition is not correct",
                ownerUserName, nodeService.getProperty(sourceNodeRef, ContentModel.PROP_CREATOR));
    }

    @Test
    public void testAccessWithNoPermissions() 
    {
        String ownerUserName = createRandomUser();
        NodeRef sourceNodeRef = createSource(ownerUserName, "quick.jpg");
        render(ownerUserName, sourceNodeRef, DOC_LIB);
        String noPermissionsUser = createRandomUser();
        permissionService.setPermission(sourceNodeRef, noPermissionsUser, PermissionService.ALL_PERMISSIONS, false);
        try
        {
            wait(noPermissionsUser, sourceNodeRef, DOC_LIB);
            fail("The rendition should not be visible for user with no permissions");
        }
        catch (AccessDeniedException ade)
        {
            // expected
        }
    }
}