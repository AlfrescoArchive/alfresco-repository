package org.alfresco.util;

import org.alfresco.rest.framework.resource.parameters.Params;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class TransformServiceDictionary {

    private static TransformServiceDictionary instance = new TransformServiceDictionary();

    public static TransformServiceDictionary getInstance()
    {
        return instance;
    }

    private TransformServiceDictionary()
    {
        //Initialize Trans service
        Properties defaultProperties = new Properties();
        InputStream propertiesStream = getClass().getClassLoader().getResourceAsStream(DICTIONARY_PROPERTIES);
        if (propertiesStream != null)
        {
            try
            {
                defaultProperties.load(propertiesStream);
            }
            catch (IOException e)
            {
                logger.error("Could not read "+DICTIONARY_PROPERTIES+" so all properties will appear to be overridden by the customer", e);
            }
        }
        else
        {
            logger.error("Could not find "+DICTIONARY_PROPERTIES+" so all properties will appear to be overridden by the customer");
        }

    this.properties = defaultProperties;
    }

    public Properties getProperties() {
        return properties;
    }

    //private final Map<String , Map<String, List<Params>>> dictionary;
    private final Properties properties;
    private static final String DICTIONARY_PROPERTIES = "alfresco/subsystems/Transformers/default/dictionary.properties";
    private static Log logger = LogFactory.getLog(TransformServiceDictionary.class);
}

