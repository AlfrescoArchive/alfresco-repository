package org.alfresco.repo.rendition2;

import org.alfresco.service.cmr.repository.NodeRef;

public interface RenditionService2
{

    public void render(NodeRef sourceNodeRef, String renditionName);

}