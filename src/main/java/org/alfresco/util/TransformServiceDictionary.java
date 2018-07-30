package org.alfresco.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TransformServiceDictionary {

    //Constants used for reading and parsing dictionary properties entries.
    private static final String patternMimeTypeExtension = "extensions";
    private static final String patternMimeTypeSupported = "supported";
    private static final String patternOptions = "options";
    private static final String patternRequired = "required";
    private static final String patternEverything = ".*";
    private static final String DICTIONARY_PROPERTIES = "alfresco/subsystems/Transformers/default/dictionary.properties";

    //Dictionary.
    private Map<String , Map<String, List<Params>>> dictionary = new HashMap<String, Map<String, List<Params>>>();

    //Logger.
    private static Log logger = LogFactory.getLog(TransformServiceDictionary.class);

    // Singleton Implementation of DictionaryService.
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

        //Pattern used for matching mimetyipe.
        Pattern mimetypePattern = Pattern.compile(patternMimeTypeExtension+patternEverything+patternMimeTypeSupported);
        //Pattern used for matching options.
        Pattern propertyPattern = Pattern.compile(patternOptions+patternEverything+patternRequired);

        //Map<String, List<Params>> used for
        Map<String, List<Params>> temporaryPlaceToStoreProperties= new HashMap<String, List<Params>>();

        //Parse properties in order to initialize the dictionary.
        Enumeration property = defaultProperties.propertyNames();
        while (property.hasMoreElements())
        {
            String key = (String) property.nextElement();
            // Add mimetype to dictionary.
            Matcher mimeTypeMatcher = mimetypePattern.matcher(key);
                if (mimeTypeMatcher.find())
                {
                    if (defaultProperties.get(key).toString().equals("true"))
                    {
                        matchMimeType(key);
                    }
                }
             // Add properties to Map<String, List<Params>> To add only ones that will be used into the dictionary.
            Matcher propsTypeMatcher = propertyPattern.matcher(key);
                if (propsTypeMatcher.find())
                {
                    String keyInfo = key.replaceFirst(patternEverything + patternOptions + ".", "");

                    boolean required = Boolean.valueOf(defaultProperties.getProperty(key).toString());
                    if(key.contains("."+patternRequired))
                    {
                        keyInfo = keyInfo.replaceFirst("."+patternRequired, "");
                    }

                    String[] valueInfo = keyInfo.split("\\.");
                    String mimetypeTransformation = valueInfo[0]+"."+valueInfo[1];
                    if(temporaryPlaceToStoreProperties.containsKey(mimetypeTransformation))
                    {
                        List<Params> lalala = temporaryPlaceToStoreProperties.get(mimetypeTransformation);
                        lalala.add (new Params(valueInfo[2], required));
                        temporaryPlaceToStoreProperties.put(mimetypeTransformation, lalala);
                    }
                    else
                    {
                        List<Params> lalala = new ArrayList<>();
                        lalala.add(new Params(valueInfo[2], required));
                        temporaryPlaceToStoreProperties.put(mimetypeTransformation, lalala);
                    }
                }
        }
        // Add properties to dictionary.
        matchProps(temporaryPlaceToStoreProperties);
    }

    private void matchMimeType(String key)
    {
        String keyInfo = key.replaceFirst(patternEverything + patternMimeTypeExtension + ".","");
        keyInfo = keyInfo.replaceFirst("."+patternMimeTypeSupported,"");
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

    private void matchProps(Map<String, List<Params>> props)
    {
        for (String key : props.keySet())
        {
            String[] keyInfo = key.split("\\.");
            if(dictionary.containsKey(keyInfo[0]) && dictionary.get(keyInfo[0]).containsKey(keyInfo[1]))
            {
                dictionary.get(keyInfo[0]).put(keyInfo[1] ,props.get(key));
            }
        }
    }

    public Map<String , Map<String, List<Params>>> getDictionary()
    {
        return dictionary;
    }

    public boolean isSupported(String sourceMimetype, String targetMimetype, Map<String,String> params)
    {
        if (dictionary.containsKey(sourceMimetype))
        {
            if (dictionary.get(sourceMimetype).containsKey(targetMimetype))
            {
                List<Params> transformationParameters = dictionary.get(sourceMimetype).get(targetMimetype);
                for (Params p : transformationParameters)
                {
                    if (!params.containsKey(p.getName()) && p.isRequired())
                    {
                        return false;
                    }
                }
            }
        }
        else
        {
            return false;
        }
        return true;
    }
}