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

import org.alfresco.model.RenditionModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.transaction.TransactionService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.FileNotFoundException;
import java.util.List;

import static java.lang.Thread.sleep;

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
    private TransactionService transactionService;

    private static final String ADMIN = "admin";
    private static final String DOC_LIB = "doclib";

    @Before
    public void setUp()
    {
        AuthenticationUtil.setRunAsUser(AuthenticationUtil.getAdminUserName());
        assertTrue("The RenditionService2 needs to be enabled", renditionService2.isEnabled());
        assertTrue("A wrong type of transform client detected", transformClient instanceof LocalTransformClient);
    }

    private void checkRendition(String testFileName, String renditionName, boolean expectedToPass) throws InterruptedException
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
    private NodeRef createSource(String user, String testFileName) throws InterruptedException
    {
        return AuthenticationUtil.runAs((AuthenticationUtil.RunAsWork<NodeRef>) () ->
                transactionService.getRetryingTransactionHelper().doInTransaction(() ->
                {
                    return createSource(testFileName);
                }), user);
    }

    // Creates a new source node as the current user in the current transaction.
    private NodeRef createSource(String testFileName) throws FileNotFoundException
    {
        NodeRef sourceNodeRef = createContentNodeFromQuickFile(testFileName);
        return sourceNodeRef;
    }

    // Requests a new rendition as the given user in its own transaction.
    private void render(String user, NodeRef sourceNode, String renditionName) throws InterruptedException
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
    private NodeRef wait(String user, NodeRef sourceNodeRef, String renditionName) throws InterruptedException
    {
        return AuthenticationUtil.runAs((AuthenticationUtil.RunAsWork<NodeRef>) () ->
                wait(sourceNodeRef, renditionName), user);
    }

    // As the current user wait for a rendition to appear. Creates new transactions to do this.
    private NodeRef wait(NodeRef sourceNodeRef, String renditionName) throws InterruptedException
    {
        ChildAssociationRef assoc = null;
        for (int i = 0; i < 20; i++)
        {
            // Must create a new transaction in order to see changes that take place after this method started.
            assoc = transactionService.getRetryingTransactionHelper().doInTransaction(() ->
            {
                return renditionService2.getRenditionByName(sourceNodeRef, renditionName);
            }, true, true);
            if (assoc != null)
            {
                break;
            }
            else
            {
                sleep(500);
            }
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
    public void testLocalRenderPdfToJpegMedium() throws Exception
    {
        checkRendition("quick.pdf", "medium", true);
    }

    @Test
    public void testLocalRenderPdfToDoclib() throws Exception
    {
        checkRendition("quick.pdf", "doclib", true);
    }

    @Test
    public void testLocalRenderPdfJpegImgpreview() throws Exception
    {
        checkRendition("quick.pdf", "imgpreview", true);
    }

    @Test
    public void testLocalRenderPdfPngAvatar() throws Exception
    {
        checkRendition("quick.pdf", "avatar", true);
    }

    @Test
    public void testLocalRenderPdfPngAvatar32() throws Exception
    {
        checkRendition("quick.pdf", "avatar32", true);
    }

    @Test
    public void testLocalRenderPdfFlashWebpreview() throws Exception
    {
        checkRendition("quick.pdf", "webpreview", false);
    }

    // DOCX transformation

    @Test
    public void testLocalRenderDocxJpegMedium() throws Exception
    {
        checkRendition("quick.docx", "medium", true);
    }

    @Test
    public void testLocalRenderDocxDoclib() throws Exception
    {
        checkRendition("quick.docx", "doclib", true);
    }

    @Test
    public void testLocalRenderDocxJpegImgpreview() throws Exception
    {
        checkRendition("quick.docx", "imgpreview", true);
    }

    @Test
    public void testLocalRenderDocxPngAvatar() throws Exception
    {
        checkRendition("quick.docx", "avatar", true);
    }

    @Test
    public void testLocalRenderDocxPngAvatar32() throws Exception
    {
        checkRendition("quick.docx", "avatar32", true);
    }

    @Test
    public void testLocalRenderDocxFlashWebpreview() throws Exception
    {
        checkRendition("quick.docx", "webpreview", false);
    }

    @Test
    public void testLocalRenderDocxPdf() throws Exception
    {
        checkRendition("quick.docx", "pdf", true);
    }

    @Test
    public void basicRendition() throws Exception
    {
        NodeRef sourceNodeRef = createSource(ADMIN, "quick.jpg");
        render(ADMIN, sourceNodeRef, DOC_LIB);
        wait(ADMIN, sourceNodeRef, DOC_LIB);
    }

    @Test
    public void basicRenditionX() throws Exception
    {
        NodeRef sourceNodeRef = createSource(ADMIN, "quick.jpg");
        render(ADMIN, sourceNodeRef, DOC_LIB);
        NodeRef renditionNodeRef = wait(ADMIN, sourceNodeRef, DOC_LIB);
    }
}