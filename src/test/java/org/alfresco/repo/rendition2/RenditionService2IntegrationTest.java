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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.BaseSpringTest;
import org.alfresco.util.GUID;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ResourceUtils;

/**
 * Integration tests for {@link RenditionService2}
 */
public class RenditionService2IntegrationTest extends BaseSpringTest
{
    @Autowired
    private RenditionService2 renditionService2;
    @Autowired
    private ContentService contentService;
    @Autowired
    private MimetypeService mimetypeService;
    @Autowired
    private NodeService nodeService;
    @Autowired
    private TransformClient transformClient;
    @Autowired
    private TransactionService transactionService;

    @Before
    public void setUp()
    {
        AuthenticationUtil.setRunAsUser(AuthenticationUtil.getAdminUserName());
        assertTrue("The RenditionService2 needs to be enabled", renditionService2.isEnabled());
        assertTrue("A wrong type of transform client detected", transformClient instanceof LocalTransformClient);
    }

    // PDF transformation

    @Test
    public void testLocalRenderPdfToJpegMedium()
    {
        checkRendition("quick.pdf", "medium");
    }

    @Test
    public void testLocalRenderPdfToDoclib()
    {
        checkRendition("quick.pdf", "doclib");
    }

    @Test
    public void testLocalRenderPdfJpegImgpreview()
    {
        checkRendition("quick.pdf", "imgpreview");
    }

    @Test
    public void testLocalRenderPdfPngAvatar()
    {
        checkRendition("quick.pdf", "avatar");
    }

    @Test
    public void testLocalRenderPdfPngAvatar32()
    {
        checkRendition("quick.pdf", "avatar32");
    }

    @Test
    public void testLocalRenderPdfFlashWebpreview()
    {
        checkRendition("quick.pdf", "webpreview");
    }

    // DOCX transformation

    @Test
    public void testLocalRenderDocxJpegMedium()
    {
        checkRendition("quick.docx", "medium");
    }

    @Test
    public void testLocalRenderDocxDoclib()
    {
        checkRendition("quick.docx", "doclib");
    }

    @Test
    public void testLocalRenderDocxJpegImgpreview()
    {
        checkRendition("quick.docx", "imgpreview");
    }

    @Test
    public void testLocalRenderDocxPngAvatar()
    {
        checkRendition("quick.docx", "avatar");
    }

    @Test
    public void testLocalRenderDocxPngAvatar32()
    {
        checkRendition("quick.docx", "avatar32");
    }

    @Test
    public void testLocalRenderDocxFlashWebpreview()
    {
        checkRendition("quick.docx", "webpreview");
    }

    @Test
    public void testLocalRenderDocxPdf()
    {
        checkRendition("quick.docx", "pdf");
    }

    private void checkRendition(String testFileName, String renditionDefinitionName)
    {
        NodeRef sourceNode = transactionService.getRetryingTransactionHelper().doInTransaction(() ->
        {
            NodeRef contentNode = createContentNode(testFileName);
            renditionService2.render(contentNode, renditionDefinitionName);
            return contentNode;
        });
        ChildAssociationRef childAssociationRef = renditionService2.getRenditionByName(sourceNode, renditionDefinitionName);
        assertNotNull("The " + renditionDefinitionName + " rendition failed for " + testFileName, childAssociationRef);
    }

    private NodeRef createContentNode(String fileName) throws FileNotFoundException
    {
        NodeRef rootNodeRef = nodeService.getRootNode(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
        NodeRef folderNodeRef = nodeService.createNode(
                rootNodeRef,
                ContentModel.ASSOC_CHILDREN,
                QName.createQName(getName() + GUID.generate()),
                ContentModel.TYPE_FOLDER).getChildRef();

        File file = ResourceUtils.getFile("classpath:quick/" + fileName);
        NodeRef contentRef = nodeService.createNode(
                folderNodeRef,
                ContentModel.ASSOC_CONTAINS,
                ContentModel.ASSOC_CONTAINS,
                ContentModel.TYPE_CONTENT,
                Collections.singletonMap(ContentModel.PROP_NAME, fileName))
                .getChildRef();
        ContentWriter contentWriter = contentService.getWriter(contentRef, ContentModel.PROP_CONTENT, true);
        contentWriter.setMimetype(mimetypeService.guessMimetype(fileName));
        contentWriter.putContent(file);

        return contentRef;
    }
}