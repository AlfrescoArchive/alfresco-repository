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

import java.util.Locale;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.GUID;
import org.junit.Before;
import org.junit.Test;
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

    private CopyServiceImpl copyServiceImpl;

    private Locale preservedLocale;
    private String copyOfLabelTranslated;
    private NodeRef destinationParent;

    @Before
    public void setup()
    {
        I18NUtil.registerResourceBundle("alfresco/messages/copy-service");
        this.preservedLocale = I18NUtil.getLocale();
        this.copyOfLabelTranslated = I18NUtil.getMessage(COPY_OF_LABEL, "");
        this.destinationParent = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, GUID.generate());

        this.copyServiceImpl = new CopyServiceImpl();
    }

    @Test
    public void testBuildNewName_FileWithExtension()
    {
        String[] expectedNamesOfCopies = generateCopyNames(FILE_NAME, FILE_EXTENSION,3,true);
        testAndAssertMultipleFileCopies(expectedNamesOfCopies);
    }

    @Test
    public void testBuildNewName_FileWithoutExtension()
    {
        String[] expectedNamesOfCopies = generateCopyNames(FILE_NAME, "",3,true);
        testAndAssertMultipleFileCopies(expectedNamesOfCopies);
    }

    @Test
    public void testBuildNewName_FileWithExtension_JapaneseLocale()
    {
        switchLocale(Locale.JAPANESE);

        String[] expectedNamesOfCopies = generateCopyNames(FILE_NAME, FILE_EXTENSION,3,false);
        testAndAssertMultipleFileCopies(expectedNamesOfCopies);

        restoreLocale();
    }

    @Test
    public void testBuildNewName_FileWithoutExtension_JapaneseLocale()
    {
        switchLocale(Locale.JAPANESE);

        String[] expectedNamesOfCopies = generateCopyNames(FILE_NAME, "",3,false);
        testAndAssertMultipleFileCopies(expectedNamesOfCopies);

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
     * Helper method to test and assert for multiple file copies
     */
    private void testAndAssertMultipleFileCopies(String[] namesOfCopies)
    {
        for(int i = 0; i < namesOfCopies.length - 1; i++)
        {
            assertEquals(namesOfCopies[i+1], copyServiceImpl.buildNewName(destinationParent, ASSOC_TYPE_QNAME, namesOfCopies[i]));
        }
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
