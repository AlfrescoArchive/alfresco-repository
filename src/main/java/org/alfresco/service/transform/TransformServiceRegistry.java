package org.alfresco.service.transform;

import java.util.Map;

public interface TransformServiceRegistry {

    /**
     * @param sourceMimetype
     * @param targetMimetype
     * @param params
     * @return
     */
    public boolean isSupported(String sourceMimetype, String targetMimetype, Map<String,String> params);
}
