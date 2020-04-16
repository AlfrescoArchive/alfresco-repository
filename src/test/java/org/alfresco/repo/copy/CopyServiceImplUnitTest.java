/*-
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
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

package org.alfresco.repo.copy;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Locale;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.GUID;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.extensions.surf.util.I18NUtil;

/**
 * Unit tests for {@link CopyServiceImpl} class.
 *
 * @author Sara Aspery
 */
public class CopyServiceImplUnitTest
{
    private static final String FILE_NAME = "Test File";
    private static final String FILE_EXTENSION = ".txt";
    private static final QName ASSOC_TYPE_QNAME = QName.createQName("assoc://type/");

    /* I18N labels used by the tests */
    private static final String COPY_OF_LABEL = "copy_service.copy_of_label";

    @Mock
    private NodeService internalNodeServiceMock;

    private CopyServiceImpl copyServiceImpl;

    private Locale preservedLocale;
    private String copyOfLabelTranslated;
    private NodeRef sourceNodeRef;
    private NodeRef destinationParent;
    private NodeRef childNodeRef1;
    private NodeRef childNodeRef2;

    @Before
    public void setup()
    {
        I18NUtil.registerResourceBundle("alfresco/messages/copy-service");
        this.preservedLocale = I18NUtil.getLocale();
        this.copyOfLabelTranslated = I18NUtil.getMessage(COPY_OF_LABEL, "");

        this.sourceNodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, GUID.generate());
        this.destinationParent = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, GUID.generate());
        this.childNodeRef1 = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, GUID.generate());
        this.childNodeRef2 = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, GUID.generate());

        // Mock the required services
        this.internalNodeServiceMock = mock(NodeService.class);

        this.copyServiceImpl = new CopyServiceImpl();
        copyServiceImpl.setInternalNodeService(internalNodeServiceMock);
    }

    @Test
    public void testBuildNewName_FileWithExtension()
    {
        String originalName = FILE_NAME + FILE_EXTENSION;
        String[] namesOfCopies = generateCopyNames(FILE_NAME, FILE_EXTENSION,3,true);

        testSingleFileCopy(originalName, namesOfCopies);
        testMultipleFileCopies(originalName, namesOfCopies);
    }

    @Test
    public void testBuildNewName_FileWithoutExtension()
    {
        String originalName = FILE_NAME;
        String[] namesOfCopies = generateCopyNames(FILE_NAME, "",3,true);

        testSingleFileCopy(originalName, namesOfCopies);
        testMultipleFileCopies(originalName, namesOfCopies);
    }

    @Test
    public void testBuildNewName_FileWithExtension_JapaneseLocale()
    {
        switchLocale(Locale.JAPANESE);

        String originalName = FILE_NAME + FILE_EXTENSION;
        String[] namesOfCopies = generateCopyNames(FILE_NAME, FILE_EXTENSION,3,false);

        testSingleFileCopy(originalName, namesOfCopies);
        testMultipleFileCopies(originalName, namesOfCopies);

        restoreLocale();
    }

    @Test
    public void testBuildNewName_FileWithoutExtension_JapaneseLocale()
    {
        switchLocale(Locale.JAPANESE);

        String originalName = FILE_NAME;
        String[] namesOfCopies = generateCopyNames(FILE_NAME, "",3,false);

        testSingleFileCopy(originalName, namesOfCopies);
        testMultipleFileCopies(originalName, namesOfCopies);

        restoreLocale();
    }

    /*
     * Helper method to generate the file names for multiple copies of a file
     */
     private String[] generateCopyNames(String fileName, String fileExtension, int nbrOfCopies, boolean isPrefix)
     {
         StringBuilder copyOfLabels = new StringBuilder();
         String[] namesOfCopies = new String[nbrOfCopies + 1];
         for (int i = 0; i < nbrOfCopies + 1; i++)
         {
             if (isPrefix)
             {
                 namesOfCopies[i] = copyOfLabels + fileName + fileExtension;
             } else
             {
                 namesOfCopies[i] = fileName + copyOfLabels + fileExtension;
             }
             copyOfLabels.append(copyOfLabelTranslated);
         }
         return namesOfCopies;
     }

    /*
     * Helper method to test and assert for a single file copy
     */
    private void testSingleFileCopy(String originalName, String[] namesOfCopies)
    {
        when(internalNodeServiceMock.getChildByName(destinationParent, ASSOC_TYPE_QNAME, originalName)).thenReturn(sourceNodeRef);
        assertEquals(namesOfCopies[1], copyServiceImpl.buildNewName(destinationParent, ASSOC_TYPE_QNAME, originalName));
    }

    /*
     * Helper method to test and assert for multiple file copies
     */
    private void testMultipleFileCopies(String originalName, String[] namesOfCopies)
    {
        when(internalNodeServiceMock.getChildByName(destinationParent, ASSOC_TYPE_QNAME, namesOfCopies[1])).thenReturn(childNodeRef1);
        when(internalNodeServiceMock.getChildByName(destinationParent, ASSOC_TYPE_QNAME, namesOfCopies[2])).thenReturn(childNodeRef2);
        assertEquals(namesOfCopies[3], copyServiceImpl.buildNewName(destinationParent, ASSOC_TYPE_QNAME, originalName));
    }

    /*
     * Helper method to switch Locale
     */
    private void switchLocale(Locale newLocale)
    {
        I18NUtil.setLocale(newLocale);
        copyOfLabelTranslated = I18NUtil.getMessage(COPY_OF_LABEL, "");
    }

    /*
     * Helper method to restore Locale
     */
    private void restoreLocale()
    {
        I18NUtil.setLocale(preservedLocale);
        copyOfLabelTranslated = I18NUtil.getMessage(COPY_OF_LABEL, "");
    }
}
