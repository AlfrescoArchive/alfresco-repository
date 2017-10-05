/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2017 Alfresco Software Limited
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
package org.alfresco;

import org.alfresco.heartbeat.HeartBeatTest;
import org.alfresco.repo.content.caching.FullTest;
import org.alfresco.repo.content.caching.cleanup.CachedContentCleanupJobTest;
import org.alfresco.repo.content.caching.quota.StandardQuotaStrategyTest;
import org.alfresco.repo.content.caching.test.ConcurrentCachingStoreTest;
import org.alfresco.repo.content.caching.test.SlowContentStoreTest;
import org.alfresco.repo.content.metadata.ConcurrencyPdfBoxMetadataExtracterTest;
import org.alfresco.repo.content.metadata.DWGMetadataExtracterTest;
import org.alfresco.repo.content.metadata.HtmlMetadataExtracterTest;
import org.alfresco.repo.content.metadata.MP3MetadataExtracterTest;
import org.alfresco.repo.content.metadata.MailMetadataExtracterTest;
import org.alfresco.repo.content.metadata.OfficeMetadataExtracterTest;
import org.alfresco.repo.content.metadata.OpenDocumentMetadataExtracterTest;
import org.alfresco.repo.content.metadata.OpenOfficeMetadataExtracterTest;
import org.alfresco.repo.content.metadata.PdfBoxMetadataExtracterTest;
import org.alfresco.repo.content.metadata.PoiMetadataExtracterTest;
import org.alfresco.repo.content.metadata.RFC822MetadataExtracterTest;
import org.alfresco.repo.content.metadata.TikaAutoMetadataExtracterTest;
import org.alfresco.repo.content.transform.AbstractContentTransformerLimitsTest;
import org.alfresco.repo.content.transform.AppleIWorksContentTransformerTest;
import org.alfresco.repo.content.transform.ArchiveContentTransformerTest;
import org.alfresco.repo.content.transform.BinaryPassThroughContentTransformerTest;
import org.alfresco.repo.content.transform.ComplexContentTransformerTest;
import org.alfresco.repo.content.transform.ContentTransformerRegistryTest;
import org.alfresco.repo.content.transform.EMLTransformerTest;
import org.alfresco.repo.content.transform.HtmlParserContentTransformerTest;
import org.alfresco.repo.content.transform.MailContentTransformerTest;
import org.alfresco.repo.content.transform.MediaWikiContentTransformerTest;
import org.alfresco.repo.content.transform.OpenOfficeContentTransformerTest;
import org.alfresco.repo.content.transform.PdfBoxContentTransformerTest;
import org.alfresco.repo.content.transform.PoiContentTransformerTest;
import org.alfresco.repo.content.transform.PoiHssfContentTransformerTest;
import org.alfresco.repo.content.transform.PoiOOXMLContentTransformerTest;
import org.alfresco.repo.content.transform.RuntimeExecutableContentTransformerTest;
import org.alfresco.repo.content.transform.StringExtractingContentTransformerTest;
import org.alfresco.repo.content.transform.TextMiningContentTransformerTest;
import org.alfresco.repo.content.transform.TextToPdfContentTransformerTest;
import org.alfresco.repo.content.transform.TikaAutoContentTransformerTest;
import org.alfresco.repo.content.transform.magick.ImageMagickContentTransformerTest;
import org.alfresco.repo.domain.query.CannedQueryDAOTest;
import org.alfresco.repo.publishing.ChannelServiceImplTest;
import org.alfresco.repo.publishing.PublishingEventHelperTest;
import org.alfresco.util.ApplicationContextHelper;
import org.alfresco.util.testing.category.DBTests;
import org.alfresco.util.testing.category.NonBuildTests;
import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.springframework.context.ApplicationContext;

/**
 * Repository project tests using the various application contexts including the minimal context
 * alfresco/minimal-context.xml but not the main one alfresco/application-context.xml.
 * Tests marked as DBTests are automatically excluded and are run as part of {@link AllDBTestsTestSuite}.
 */
@RunWith(Categories.class)
@Categories.ExcludeCategory({DBTests.class, NonBuildTests.class})
@Suite.SuiteClasses({

    // ----------------------------------------------------------------------
    // Minimum context [classpath:alfresco/minimal-context.xml]
    // ----------------------------------------------------------------------

    // Limits
    AbstractContentTransformerLimitsTest.class,

    // Transform tests
    BinaryPassThroughContentTransformerTest.class,
    ComplexContentTransformerTest.class,
    ContentTransformerRegistryTest.class,
    HtmlParserContentTransformerTest.class,
    MailContentTransformerTest.class,
    EMLTransformerTest.class,
    MediaWikiContentTransformerTest.class,
    OpenOfficeContentTransformerTest.class,
    PdfBoxContentTransformerTest.class,
    PoiContentTransformerTest.class,
    PoiHssfContentTransformerTest.class,
    PoiOOXMLContentTransformerTest.class,
    RuntimeExecutableContentTransformerTest.class,
    StringExtractingContentTransformerTest.class,
    TextMiningContentTransformerTest.class,
    TextToPdfContentTransformerTest.class,
    TikaAutoContentTransformerTest.class,
    ImageMagickContentTransformerTest.class,
    AppleIWorksContentTransformerTest.class,
    ArchiveContentTransformerTest.class,

    // Metadata tests
    DWGMetadataExtracterTest.class,
    HtmlMetadataExtracterTest.class,
    MailMetadataExtracterTest.class,
    MP3MetadataExtracterTest.class,
    OfficeMetadataExtracterTest.class,
    OpenDocumentMetadataExtracterTest.class,
    OpenOfficeMetadataExtracterTest.class,
    PdfBoxMetadataExtracterTest.class,
    ConcurrencyPdfBoxMetadataExtracterTest.class,
    PoiMetadataExtracterTest.class,
    RFC822MetadataExtracterTest.class,
    TikaAutoMetadataExtracterTest.class,

    org.alfresco.repo.content.metadata.MappingMetadataExtracterTest.class,

    // ----------------------------------------------------------------------
    // Misc contexts
    // ----------------------------------------------------------------------

    // [classpath:alfresco/node-locator-context.xml, classpath:test-nodeLocatorServiceImpl-context.xml]
    org.alfresco.repo.nodelocator.NodeLocatorServiceImplTest.class,

    // [classpath*:alfresco/ibatis/ibatis-test-context.xml, classpath:alfresco/application-context.xml,
    // classpath:alfresco/test/global-integration-test-context.xml]
    CannedQueryDAOTest.class,
    // REPO-2783 only passes on a dirty DB. fails to pass on a clean DB - testConcurrentArchive
    org.alfresco.repo.node.NodeServiceTest.class,

    // [classpath:alfresco/application-context.xml, classpath:alfresco/minimal-context.xml]
    org.alfresco.RepositoryStartStopTest.class,

    // [classpath:cachingstore/test-context.xml]
    FullTest.class,

    // [classpath:cachingstore/test-cleaner-context.xml]
    CachedContentCleanupJobTest.class,

    // [classpath:cachingstore/test-std-quota-context.xml]
    StandardQuotaStrategyTest.class,

    // [classpath:cachingstore/test-slow-context.xml]
    SlowContentStoreTest.class,
    ConcurrentCachingStoreTest.class,

    // [classpath:org/alfresco/repo/jscript/test-context.xml]
    org.alfresco.repo.jscript.ScriptBehaviourTest.class,

    // [module/module-component-test-beans.xml]
    org.alfresco.repo.module.ComponentsTest.class,

    // TODO can we remove this? Was it EOLed?
    // [classpath:test/alfresco/test-web-publishing-context.xml]
    ChannelServiceImplTest.class,
    PublishingEventHelperTest.class,

    // [alfresco/scheduler-core-context.xml, org/alfresco/util/test-scheduled-jobs-context.xml]
    org.alfresco.util.CronTriggerBeanTest.class,

    // [alfresco/scheduler-core-context.xml, org/alfresco/heartbeat/test-heartbeat-context.xml]
    HeartBeatTest.class,

    // ----------------------------------------------------------------------
    // Transformer/Rendition contexts
    //
    // The following tests can be extracted in a separate test suite
    // if/when we decide to move the transformations in a separate component
    // ----------------------------------------------------------------------


    // [classpath:alfresco/application-context.xml, classpath:org/alfresco/repo/thumbnail/test-thumbnail-context.xml]
    // some tests fail locally - on windows
    org.alfresco.repo.thumbnail.ThumbnailServiceImplTest.class,

    // [classpath:/test/alfresco/test-renditions-context.xml, classpath:alfresco/application-context.xml,
    // classpath:alfresco/test/global-integration-test-context.xml]
    // this does NOT passes locally
    org.alfresco.repo.rendition.RenditionServicePermissionsTest.class
})
public class MiscContextTestSuite
{
   /**
    * Asks {@link ApplicationContextHelper} to give us a
    *  suitable, perhaps cached context for use in our tests
    */
   public static ApplicationContext getMinimalContext() {
      ApplicationContextHelper.setUseLazyLoading(false);
      ApplicationContextHelper.setNoAutoStart(true);
      return ApplicationContextHelper.getApplicationContext(
           new String[] { "classpath:alfresco/minimal-context.xml" }
      );
   }

   static
   {
       getMinimalContext();
   }
}
