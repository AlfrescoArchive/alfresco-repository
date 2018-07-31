package org.alfresco.service.transform;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TransformServiceRegistryImpl implements TransformServiceRegistry {

    // Constants used for reading and parsing registry properties entries.
    private static final String PATTERN_MIME_TYPE_EXTENSION = "extensions";
    private static final String PATTERN_MIME_TYPE_SUPPORTED = "supported";
    private static final String PATTERN_PARAMS_OPTIONS = "options";
    private static final String PATTERN_PARAMS_REQUIRED = "required";
    private static final String PATTERN_ANY_CHARACTERS = ".*";
    private static final String REGISTRY_PROPERTIES = "alfresco/subsystems/TransformService/registry.properties";

    /** Registry.
     * docx(source) |─> pdf(target) |-> timeout (property)
     *              |               └─> true (required)
     *              └─> png(target) |-> timeout (property)
     *                              └─> true (required)
     */
    private Map<String , Map<String, List<Params>>> registry = new HashMap<>();

    // Logger.
    private static Log logger = LogFactory.getLog(TransformServiceRegistryImpl.class);

    private TransformServiceRegistryImpl()
    {
        // Initialize Registry service
        Properties defaultProperties = new Properties();
        InputStream propertiesStream = getClass().getClassLoader().getResourceAsStream(REGISTRY_PROPERTIES);
        if (propertiesStream != null)
        {
            try
            {
                defaultProperties.load(propertiesStream);
            }
            catch (IOException e)
            {
                logger.error("Could not read "+REGISTRY_PROPERTIES+" so all properties will appear to be overridden by the customer", e);
            }
        }
        else
        {
            logger.error("Could not find "+REGISTRY_PROPERTIES+" so all properties will appear to be overridden by the customer");
        }

        // Pattern used for matching mimetyipe.
        Pattern mimetypePattern = Pattern.compile(PATTERN_MIME_TYPE_EXTENSION + PATTERN_ANY_CHARACTERS + PATTERN_MIME_TYPE_SUPPORTED);
        // Pattern used for matching params/options.
        Pattern propertyPattern = Pattern.compile(PATTERN_PARAMS_OPTIONS + PATTERN_ANY_CHARACTERS + PATTERN_PARAMS_REQUIRED);

        // Map<String, List<Params>> used for holding params and add them to the corresponding
        // mimetypes once all of them are added into the registry.
        Map<String, List<Params>> temporaryPlaceToStoreProperties= new HashMap<>();

        // Parse properties read from file (REGISTRY_PROPERTIES) in order to initialize the registry.
        Enumeration propertyEntry = defaultProperties.propertyNames();
        while (propertyEntry.hasMoreElements())
        {
            String propertyEntryKey = (String) propertyEntry.nextElement();
            // Add mimetype to registry.
            Matcher mimeTypeMatcher = mimetypePattern.matcher(propertyEntryKey);
                if (mimeTypeMatcher.find())
                {
                    if (defaultProperties.get(propertyEntryKey).toString().equals("true"))
                    {
                        matchMimeType(propertyEntryKey);
                    }
                }
             // Add properties to Map<String, List<Params>> To add only ones that are relevant to the registry.
            Matcher propsTypeMatcher = propertyPattern.matcher(propertyEntryKey);
                if (propsTypeMatcher.find())
                {
                    parseProp(temporaryPlaceToStoreProperties,propertyEntryKey,defaultProperties.getProperty(propertyEntryKey));
                }
        }
        // Add properties/options to registry.
        matchProps(temporaryPlaceToStoreProperties);
    }

    private void matchMimeType(String key)
    {
        String keyInfo = key.replaceFirst(PATTERN_ANY_CHARACTERS + PATTERN_MIME_TYPE_EXTENSION + ".","");
        keyInfo = keyInfo.replaceFirst("."+ PATTERN_MIME_TYPE_SUPPORTED,"");
        // keyInfo = sourceMimetype + targetMimetype
        String[] mimetypes = keyInfo.split("\\.");

        if (registry.containsKey(mimetypes[0]))
        {
        registry.get(mimetypes[0]).put(mimetypes[1],new ArrayList<>());
        }
        else
        {
        Map<String, List<Params>> targetMimetype = new HashMap<>();
        targetMimetype.put(mimetypes[1],new ArrayList<>());
        registry.put(mimetypes[0], targetMimetype);
        }
    }

    private void  parseProp(Map<String, List<Params>> temporaryPlaceToStoreProperties, String propertyEntryKey, String propValue)
    {
        String paramsEntryKey = propertyEntryKey.replaceFirst(PATTERN_ANY_CHARACTERS + PATTERN_PARAMS_OPTIONS + ".", "");

        boolean required = Boolean.valueOf(propValue);
        if(propertyEntryKey.contains("."+ PATTERN_PARAMS_REQUIRED))
        {
            paramsEntryKey = paramsEntryKey.replaceFirst("."+ PATTERN_PARAMS_REQUIRED, "");
        }

        // propertyInfo = sourceMimetype + . + targetMimetype + . + option
        String[] propertyInfo = paramsEntryKey.split("\\.");
        String mimetypeTransformation = propertyInfo[0]+"."+propertyInfo[1];

        if(temporaryPlaceToStoreProperties.containsKey(mimetypeTransformation))
        {
            List<Params> propertyList = temporaryPlaceToStoreProperties.get(mimetypeTransformation);
            propertyList.add (new Params(propertyInfo[2], required));
            temporaryPlaceToStoreProperties.put(mimetypeTransformation, propertyList);
        }
        else
        {
            List<Params> propertyList = new ArrayList<>();
            propertyList.add(new Params(propertyInfo[2], required));
            temporaryPlaceToStoreProperties.put(mimetypeTransformation, propertyList);
        }
    }

    private void matchProps(Map<String, List<Params>> props)
    {
        for (String propertyKey : props.keySet())
        {
            String[] keyInfo = propertyKey.split("\\.");
            if(registry.containsKey(keyInfo[0]) && registry.get(keyInfo[0]).containsKey(keyInfo[1]))
            {
                registry.get(keyInfo[0]).put(keyInfo[1] ,props.get(propertyKey));
            }
        }
    }

    public boolean isSupported(String sourceMimetype, String targetMimetype, Map<String,String> params)
    {
        if (registry.containsKey(sourceMimetype) && registry.get(sourceMimetype).containsKey(targetMimetype))
        {
            List<Params> transformationParameters = registry.get(sourceMimetype).get(targetMimetype);
            for (Params p : transformationParameters)
            {
                // Verify that all the required parameters are part of the transformation request
                if (!params.containsKey(p.getName()) && p.isRequired())
                {
                    return false;
                }
            }
            // TO DO: Implement validation of bad request parameters
        }
        else
        {
            return false;
        }
        return true;
    }
}