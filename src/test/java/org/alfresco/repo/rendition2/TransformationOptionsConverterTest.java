/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2019 Alfresco Software Limited
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
package org.alfresco.repo.rendition2;

import org.alfresco.repo.content.transform.magick.ImageResizeOptions;
import org.alfresco.repo.content.transform.magick.ImageTransformationOptions;
import org.alfresco.service.cmr.repository.PagedSourceOptions;
import org.alfresco.service.cmr.repository.TransformationOptions;
import org.alfresco.service.cmr.repository.TransformationSourceOptions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.alfresco.repo.content.MimetypeMap.MIMETYPE_IMAGE_JPEG;
import static org.junit.Assert.assertEquals;

@Deprecated
@RunWith(MockitoJUnitRunner.class)
public class TransformationOptionsConverterTest
{
    private TransformationOptionsConverter converter;

    private Map<String, String> newOptions = new HashMap<>();

    @Before
    public void setUp() throws Exception
    {
        converter = new TransformationOptionsConverter();
        converter.setMaxSourceSizeKBytes("-1");
        converter.setReadLimitTimeMs("-1");
        converter.setReadLimitKBytes("-1");
        converter.setPageLimit("-1");
        converter.setMaxPages("-1");
    }

    @Test
    public void testCompositeReformatAndResizeRendition()
    {
        //   args=allowEnlargement=true
        //   alphaRemove=true
        //   autoOrient=true
        //   endPage=0
        //   maintainAspectRatio=true
        //   resizeHeight=30
        //   resizeWidth=20
        //   startPage=0
        //  orig=ImageTransformationOptions [commandOptions=, resizeOptions=ImageResizeOptions [width=20, height=30, maintainAspectRatio=true, percentResize=false, resizeToThumbnail=false, allowEnlargement=true], autoOrient=true], sourceOptions={ PagedSourceOptionsPagedSourceOptions {1, 1}} ]

        ImageTransformationOptions oldOptions = new ImageTransformationOptions();
        ImageResizeOptions imageResizeOptions = new ImageResizeOptions();
        imageResizeOptions.setHeight(30);
        imageResizeOptions.setWidth(20);
        oldOptions.setResizeOptions(imageResizeOptions);
        PagedSourceOptions pagedSourceOptions = new PagedSourceOptions();
        pagedSourceOptions.setStartPageNumber(1);
        pagedSourceOptions.setEndPageNumber(1);
        Collection<TransformationSourceOptions> sourceOptionsList = Collections.singletonList(pagedSourceOptions);
        oldOptions.setSourceOptionsList(sourceOptionsList);

        String expected = "ImageTransformationOptions [commandOptions=, resizeOptions=" +
                "ImageResizeOptions [width=20, height=30, maintainAspectRatio=true, percentResize=false, " +
                "resizeToThumbnail=false, allowEnlargement=true], autoOrient=true], " +
                "sourceOptions={ PagedSourceOptionsPagedSourceOptions {1, 1}} ]";
        assertEquals(expected, oldOptions.toString());

        Map<String, String> convertedOptionsMap = converter.getOptions(oldOptions, MIMETYPE_IMAGE_JPEG, MIMETYPE_IMAGE_JPEG);
        String expectedArgs =
                "allowEnlargement=true " +
                        "alphaRemove=true " +
                        "autoOrient=true " +
                        "endPage=0 " +
                        "maintainAspectRatio=true " +
                        "resizeHeight=30 " +
                        "resizeWidth=20 " +
                        "startPage=0 " +
                        "timeout=-1 ";
        assertEquals(expectedArgs, getSortedOptions(convertedOptionsMap));

        String oldOptionsStr = getSortedOptions(oldOptions, MIMETYPE_IMAGE_JPEG, MIMETYPE_IMAGE_JPEG);

        newOptions = converter.getOptions(oldOptions, MIMETYPE_IMAGE_JPEG, MIMETYPE_IMAGE_JPEG);
        String newOptionsStr = getSortedOptions(oldOptions, MIMETYPE_IMAGE_JPEG, MIMETYPE_IMAGE_JPEG);
        oldOptions = (ImageTransformationOptions)converter.getTransformationOptions("doclib", newOptions);
        String oldOptionsStr2 = getSortedOptions(oldOptions, MIMETYPE_IMAGE_JPEG, MIMETYPE_IMAGE_JPEG);

        assertEquals(expected, oldOptions.toString());
        assertEquals(oldOptionsStr, oldOptionsStr2);
        assertEquals(oldOptionsStr, newOptionsStr);
    }

    private String getSortedOptions(TransformationOptions options, String sourceMimetype, String targetMimetype)
    {
        return getSortedOptions(converter.getOptions(options, sourceMimetype, targetMimetype));
    }

    private static String getSortedOptions(Map<String, String> options)
    {
        final List<String> list = new ArrayList<>();
        options.entrySet().forEach(e->
        {
            if (e.getValue() != null)
            {
                list.add(e.getKey() + '=' + e.getValue() + ' ');
            }
        });
        return getSortedOptions(list);
    }

    private static String getSortedOptions(String[] args)
    {
        final List<String> list = new ArrayList<>();
        for (int i=0; i<args.length; i+=2)
        {
            if (args[i+1] != null)
            {
                list.add(args[i] + "=" + args[i + 1] + ' ');
            }
        }
        return getSortedOptions(list);
    }

    private static String getSortedOptions(List<String> list)
    {
        StringBuilder sb = new StringBuilder();
        Collections.sort(list);
        list.forEach(a->sb.append(a));
        return sb.toString();
    }
}