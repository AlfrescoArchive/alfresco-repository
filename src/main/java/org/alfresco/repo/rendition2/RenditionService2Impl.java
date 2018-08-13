package org.alfresco.repo.rendition2;

import org.alfresco.enterprise.repo.rendition.RenditionDefinition2;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;

public class RenditionService2Impl implements RenditionService2
{
    private RenditionDefinitionRegistry2 registry;
    private ContentService contentService;

    public void setContentService(ContentService contentService)
    {
        this.contentService = contentService;
    }

    public void setRegistry(RenditionDefinitionRegistry2 registry)
    {
        this.registry = registry;
    }

    public void render(NodeRef sourceNodeRef, String renditionName)
    {
        RenditionDefinition2 renditionDefinition = registry.getDefinition(renditionName);

        // extract the options from definition and pass them to content service

        // TODO use Future for async
        contentService.transform();
    }



//    private boolean isTransformationSupported();

//    private Object delegateToRemoteTransformService(Action action, NodeRef nodeRef);
}
