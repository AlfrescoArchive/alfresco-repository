package org.alfresco.util;

import org.alfresco.rest.framework.resource.parameters.Params;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        //Parse properties in order to initialize the dictionary.
        Enumeration property = defaultProperties.propertyNames();
        Pattern mimetypePattern = Pattern.compile("extensions.*.supported");
        while (property.hasMoreElements())
        {
            String key = (String) property.nextElement();
            if (defaultProperties.get(key).toString().equals("true"))
            {
                Matcher matcher = mimetypePattern.matcher(key);
                if (matcher.find())
                {
                    String keyInfo = key.replaceFirst(".*extensions.","");
                    keyInfo = keyInfo.replaceFirst(".supported","");
                    String[] mimetypes = keyInfo.split("\\.");
                    if (dictionary.containsKey(mimetypes[0]))
                    {
                        dictionary.get(mimetypes[0]).put(mimetypes[1],new ArrayList<Params>());
                    }
                    else
                    {
                        Map<String, List<Params>> targetmimetype = new HashMap<String, List<Params>>();
                        targetmimetype.put(mimetypes[1],new ArrayList<Params>());
                        dictionary.put(mimetypes[0], targetmimetype);
                    }
                }
            }
        }
    }
    public Map<String , Map<String, List<Params>>> getDictionary()
    {
        return dictionary;
    }

    private Map<String , Map<String, List<Params>>> dictionary = new HashMap<String, Map<String, List<Params>>>();
    private static final String DICTIONARY_PROPERTIES = "alfresco/subsystems/Transformers/default/dictionary.properties";
    private static Log logger = LogFactory.getLog(TransformServiceDictionary.class);
}

