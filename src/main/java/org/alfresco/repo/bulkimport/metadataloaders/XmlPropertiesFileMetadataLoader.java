/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
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

package org.alfresco.repo.bulkimport.metadataloaders;


import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.alfresco.repo.bulkimport.MetadataLoader;
import org.alfresco.repo.bulkimport.impl.FileUtils;
import org.alfresco.service.ServiceRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * MetadataLoader that loads metadata from an (optional) "shadow" properties
 * file in XML format.  This shadow properties file must have <strong>exactly</strong>
 * the same name and extension as the file for whom it is storing metadata, but
 * with the suffix ".metadata.properties.xml".  So for example, if there is a file
 * called "IMG_1967.jpg", the "shadow" metadata file for it would be called
 * "IMG_1967.jpg.metadata.properties.xml". 
 * 
 * The metadata file itself follows the usual rules for Java properties XML
 * files, with a property with the key "type" containing the qualified name of
 * the content type to use for the file, a property with the key "aspects"
 * containing a comma-delimited list of qualified names of the aspects to
 * attach to the file, and then one Java property per metadata property, with
 * the key being the Alfresco property QName and the value being the value of
 * that property.
 * 
 * For example (note escaping rules for namespace separator!):
 * 
 * <code>
 * <?xml version="1.0" encoding="UTF-8"?>
 * <!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
 * <properties>
 *   <comment>Metadata for IMG_1967.jpg</comment>
 *   <entry key="type">cm:content</entry>
 *   <entry key="aspects">cm:versionable, custom:myAspect</entry>
 *   <entry key="cm:title">This is the value of the cm:title field.</entry>
 *   <entry key="cm:description">This is the value of the cm:description field.</entry>
 *   <entry key="cm:taggable">workspace://SpacesStore/3da6c395-3a4b-4a57-836d-8e5</entry>
 *   <entry key="custom:myProperty">This is the value of the custom:myProperty field.</entry>
 *   <entry key="custom:aDateProperty">2001-01-01T12:00:00.000+01:00</entry>
 * </properties>
 * </code>
 * 
 * Notes:
 * <ul>
 *   <li>Java XML properties files fully support Unicode characters (unlike the
 *       original properties file format), so use of this class is strongly
 *       recommended over and <code>PropertiesFileMetadataLoader</code>.</li>
 *   <li>the metadata must conform to the type and aspect definitions
 *       configured in Alfresco (including mandatory fields, constraints and data
 *       types).  Any violations will terminate the bulk import process.</li>
 *   <li>associations are not yet supported</li>
 *   <li>dates, times and date times <u>must</u> be stored in ISO8601 format
 *       (although note that Alfresco ignores timezone modifiers)</li>
 * </ul>
 *
 * @since 4.0
 * 
 * @see MetadataLoader
 */
public final class XmlPropertiesFileMetadataLoader extends AbstractMapBasedMetadataLoader
{
    private final static Log log = LogFactory.getLog(XmlPropertiesFileMetadataLoader.class);
    private final static String METADATA_FILE_EXTENSION = "properties.xml";
    private final static String PROP_VERSION_LABEL = "cm:versionLabel";
    // MNT-18001
    // list of properties to be ignored from the metadata files
    private final static Set<String> ignoredProperties;
    static
    {
        ignoredProperties = new HashSet<String>();
        // add the properties to be ignored
        ignoredProperties.add(PROP_VERSION_LABEL);
    }
    
    public XmlPropertiesFileMetadataLoader(final ServiceRegistry serviceRegistry)
    {
        super(serviceRegistry, METADATA_FILE_EXTENSION);
    }
    
    public XmlPropertiesFileMetadataLoader(final ServiceRegistry serviceRegistry, final String multiValuedSeparator)
    {
        super(serviceRegistry, multiValuedSeparator, METADATA_FILE_EXTENSION);
    }

    /**
     * @see AbstractMapBasedMetadataLoader#loadMetadataFromFile(java.io.File)
     */
    @Override
    protected Map<String,Serializable> loadMetadataFromFile(Path metadataFile)
    {
        Map<String,Serializable> result = null;
        
        try
        {
            Properties props = new Properties();
            props.loadFromXML(new BufferedInputStream(Files.newInputStream(metadataFile)));
            result = new HashMap<String,Serializable>((Map)props);
            // MNT-18001 fix
            // parse the result and remove the ignored properties
            removeIgnoredProperties(result);
        }
        catch (final IOException ioe)
        {
            if (log.isWarnEnabled()) log.warn("Metadata file '" + FileUtils.getFileName(metadataFile) + "' could not be read.", ioe);
        }
        
        return(result);
    }

    /**
     * Iterates through the list of ignoredProperties and removes all
     * occurrences in the props map
     *
     * @param props Map with the properties from metadata file
     */
    private void removeIgnoredProperties(Map<String,Serializable> props)
    {
        for (String ignoredProperty : ignoredProperties)
        {
            props.remove(ignoredProperty);
        }
    }
}
