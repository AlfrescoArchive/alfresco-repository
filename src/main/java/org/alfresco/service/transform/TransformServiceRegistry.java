package org.alfresco.service.transform;

import java.util.Map;

public interface TransformServiceRegistry {

    /**
     * Validate if a transformation request can pe processed by Transformation Service.
     * @param sourceMimetype
     * @param targetMimetype
     * @param params
     * @return
     */
    public boolean isSupported(String sourceMimetype, String targetMimetype, Map<String,String> params);
}
