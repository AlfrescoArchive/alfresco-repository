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
package org.alfresco.repo.content.replication;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.alfresco.repo.content.AbstractWritableContentStoreTest;
import org.alfresco.repo.content.ContentContext;
import org.alfresco.repo.content.ContentStore;
import org.alfresco.repo.content.UnsupportedContentUrlException;
import org.alfresco.repo.content.filestore.FileContentStore;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.DirectAccessUrl;
import org.alfresco.test_category.OwnJVMTestsCategory;
import org.alfresco.util.GUID;
import org.alfresco.util.TempFileProvider;
import org.junit.Before;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Tests read and write functionality for the aggregating store.
 * <p>
 * 
 * @see org.alfresco.repo.content.replication.AggregatingContentStore
 * 
 * @author Derek Hulley
 * @author Mark Rogers
 */
@Category(OwnJVMTestsCategory.class)
public class AggregatingContentStoreTest extends AbstractWritableContentStoreTest
{
    private static final String SOME_CONTENT = "The No. 1 Ladies' Detective Agency";
    
    private AggregatingContentStore aggregatingStore;
    private ContentStore primaryStore;
    private List<ContentStore> secondaryStores;

    @Mock
    ContentStore primaryStoreMock;
    @Mock
    ContentStore secondaryStoreMock;

    @Before
    public void before() throws Exception
    {
        File tempDir = TempFileProvider.getTempDir();
        // create a primary file store
        String storeDir = tempDir.getAbsolutePath() + File.separatorChar + GUID.generate();
        primaryStore = new FileContentStore(ctx, storeDir);
        // create some secondary file stores
        secondaryStores = new ArrayList<ContentStore>(3);
        for (int i = 0; i < 4; i++)
        {
            storeDir = tempDir.getAbsolutePath() + File.separatorChar + GUID.generate();
            FileContentStore store = new FileContentStore(ctx, storeDir);
            secondaryStores.add(store);
        }
        // Create the aggregating store
        aggregatingStore = new AggregatingContentStore();
        aggregatingStore.setPrimaryStore(primaryStore);
        aggregatingStore.setSecondaryStores(secondaryStores);
    }

    @Override
    public ContentStore getStore()
    {
        return aggregatingStore;
    }
    
    /**
     * Get a writer into the store.  This test class assumes that the store is writable and
     * that it therefore supports the ability to write content.
     * 
     * @return
     *      Returns a writer for new content
     */
    protected ContentWriter getWriter()
    {
        ContentStore store = getStore();
        return store.getWriter(ContentStore.NEW_CONTENT_CONTEXT);
    }
    
    
    /**
     * {@inheritDoc}
     * <p>
     * This implementation creates some content in the store and returns the new content URL.
     */
    protected String getExistingContentUrl()
    {
        ContentWriter writer = getWriter();
        writer.putContent("Content for getExistingContentUrl");
        return writer.getContentUrl();
    }
    
    public void testAddContent() throws Exception
    {
        ContentWriter writer = getWriter();
        writer.putContent(SOME_CONTENT);
        String contentUrl = writer.getContentUrl();
        
        checkForUrl(contentUrl, true);
    }
    
    /**
     * Checks that the url is present in each of the stores
     * 
     * @param contentUrl String
     * @param mustExist true if the content must exist, false if it must <b>not</b> exist
     */
    private void checkForUrl(String contentUrl, boolean mustExist)
    {
        ContentReader reader = getReader(contentUrl);
        assertEquals("Reader state differs from expected: " + reader, mustExist, reader.exists());
    }
    
    public void testDelete() throws Exception
    {
        
        // write some content
        ContentWriter writer = getWriter();
        writer.putContent(SOME_CONTENT);
        String contentUrl = writer.getContentUrl();
        
        ContentReader reader = primaryStore.getReader(contentUrl);
        assertTrue("Content was not in the primary store", reader.exists());
        assertEquals("The content was incorrect", SOME_CONTENT, reader.getContentString());

        getStore().delete(contentUrl);
        checkForUrl(contentUrl, false);
    }
    
    public void testReadFromSecondaryStore()
    {
        // pick a secondary store and write some content to it
        ContentStore secondaryStore = secondaryStores.get(2);
        ContentWriter writer = secondaryStore.getWriter(ContentContext.NULL_CONTEXT);
        writer.putContent(SOME_CONTENT);
        String contentUrl = writer.getContentUrl();
        
        checkForUrl(contentUrl, true);
    }

    public void testIsDirectAccessSupported()
    {
        // Create the aggregating store
        AggregatingContentStore aggStore = new AggregatingContentStore();
        aggStore.setPrimaryStore(primaryStoreMock);
        aggStore.setSecondaryStores(List.of(secondaryStoreMock));

        // By default it is unsupported
        assertFalse(aggStore.isDirectAccessSupported());
        
        // Not supported if the primary store doesn't support it and the secondary do.
        when(secondaryStoreMock.isDirectAccessSupported()).thenReturn(true);
        assertFalse(aggStore.isDirectAccessSupported());

        // Only supported if primary store supports it
        when(primaryStoreMock.isDirectAccessSupported()).thenReturn(true);
        when(secondaryStoreMock.isDirectAccessSupported()).thenReturn(true);
        assertTrue(aggStore.isDirectAccessSupported());

        when(primaryStoreMock.isDirectAccessSupported()).thenReturn(true);
        when(secondaryStoreMock.isDirectAccessSupported()).thenReturn(false);
        assertTrue(aggStore.isDirectAccessSupported());
    }

    public void testGetDirectAccessUrl()
    {
        // Create the aggregating store
        AggregatingContentStore aggStore = new AggregatingContentStore();
        aggStore.setPrimaryStore(primaryStoreMock);
        aggStore.setSecondaryStores(List.of(secondaryStoreMock));

        // By default it is unsupported
        try
        {
            aggStore.getDirectAccessUrl("url", null);
            fail();
        }
        catch (UnsupportedOperationException e)
        {
            // Expected
        }

        when(primaryStoreMock.getDirectAccessUrl(anyString(), any())).thenReturn(new DirectAccessUrl());
        aggStore.getDirectAccessUrl("url", null);

        UnsupportedOperationException unsupportedEx = new UnsupportedOperationException("Retrieving direct access URLs is not supported by this content store.");

        when(primaryStoreMock.getDirectAccessUrl(anyString(), any())).thenThrow(unsupportedEx);
        when(secondaryStoreMock.getDirectAccessUrl(anyString(), any())).thenReturn(new DirectAccessUrl());
        aggStore.getDirectAccessUrl("url", null);

        try
        {
            when(primaryStoreMock.getDirectAccessUrl(anyString(), any())).thenThrow(unsupportedEx);
            when(secondaryStoreMock.getDirectAccessUrl(anyString(), any())).thenThrow(unsupportedEx);
            aggStore.getDirectAccessUrl("url", null);
            fail();
        }
        catch (UnsupportedContentUrlException e)
        {
            // Expected
        }
    }
}
