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
import org.alfresco.repo.content.transform.ContentTransformer;
import org.alfresco.repo.content.transform.magick.ImageTransformationOptions;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.rendition.RenditionPreventionRegistry;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the RenditionService2 in a Community context where we only have local transformers.
 *
 * Also see EnterpriseRenditionService2Test.
 *
 * @author adavis
 */
@RunWith(MockitoJUnitRunner.class)
public class RenditionService2Test
{
    private RenditionService2Impl renditionService2;
    private LocalTransformClient localTransformClient;
    private RenditionDefinitionRegistry2 renditionDefinitionRegistry2;

    @Mock private NodeService nodeService;
    @Mock private ContentService contentService;
    @Mock private RenditionPreventionRegistry renditionPreventionRegistry;
    @Mock private ContentData contentData;
    @Mock private ContentTransformer contentTransformer;
    @Mock private NamespaceService namespaceService;
    @Mock private PolicyComponent policyComponent;
    @Mock private BehaviourFilter behaviourFilter;

    private NodeRef nodeRef = new NodeRef("workspace://spacesStore/test-id");
    private static final String IMGPREVIEW = "imgpreview";
    private static final String PDF = "application/pdf";
    private static final String JPEG = "image/jpeg";
    private String contentUrl = "test-content-url";

    @Before
    public void setup() throws Exception
    {
        renditionService2 = new RenditionService2Impl();
        renditionDefinitionRegistry2 = new RenditionDefinitionRegistry2Impl();

        Map<String, String> options = new HashMap<>();
        options.put("width", "960");
        options.put("height", "1024");
        new RenditionDefinition2Impl(IMGPREVIEW, JPEG, options, renditionDefinitionRegistry2);

        localTransformClient = new LocalTransformClient();
        localTransformClient.setNodeService(nodeService);
        localTransformClient.setContentService(contentService);
        localTransformClient.setRenditionService2(renditionService2);
        localTransformClient.afterPropertiesSet();

        when(nodeService.exists(nodeRef)).thenReturn(true);
        when(nodeService.getProperty(nodeRef, ContentModel.PROP_CONTENT)).thenReturn(contentData);
        when(contentData.getContentUrl()).thenReturn(contentUrl);

        renditionService2.setNodeService(nodeService);
        renditionService2.setContentService(contentService);
        renditionService2.setRenditionPreventionRegistry(renditionPreventionRegistry);
        renditionService2.setRenditionDefinitionRegistry2(renditionDefinitionRegistry2);
        renditionService2.setTransformClient(localTransformClient);
        renditionService2.setNamespaceService(namespaceService);
        renditionService2.setPolicyComponent(policyComponent);
        renditionService2.setBehaviourFilter(behaviourFilter);
        renditionService2.setEnabled(true);
        renditionService2.afterPropertiesSet();
    }

    @Captor
    ArgumentCaptor<ImageTransformationOptions> captor;

    @Test
    public void useLocalTransform() throws Throwable
    {
        when(contentService.getTransformer(anyString(), anyString(), anyLong(), anyString(), any())).thenReturn(contentTransformer);

        renditionService2.render(nodeRef, IMGPREVIEW);

        verify(contentService).getTransformer(anyString(), anyString(), anyLong(), anyString(), captor.capture());
        assertEquals(1024, captor.getValue().getResizeOptions().getHeight());
        assertEquals(960, captor.getValue().getResizeOptions().getWidth());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void noTransform() throws Throwable
    {
        when(contentService.getTransformer(anyString(), anyString(), anyLong(), anyString(), any())).thenReturn(null);

        renditionService2.render(nodeRef, IMGPREVIEW);
    }
}
