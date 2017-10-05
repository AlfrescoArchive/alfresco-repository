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

import org.alfresco.heartbeat.HBDataCollectorServiceImplTest;
import org.alfresco.repo.action.ActionConditionDefinitionImplTest;
import org.alfresco.repo.action.ActionConditionImplTest;
import org.alfresco.repo.action.ActionDefinitionImplTest;
import org.alfresco.repo.action.ActionImplTest;
import org.alfresco.repo.action.CompositeActionConditionImplTest;
import org.alfresco.repo.action.CompositeActionImplTest;
import org.alfresco.repo.action.ParameterDefinitionImplTest;
import org.alfresco.repo.audit.AuditableAnnotationTest;
import org.alfresco.repo.audit.PropertyAuditFilterTest;
import org.alfresco.repo.content.ContentDataTest;
import org.alfresco.repo.content.caching.CachingContentStoreTest;
import org.alfresco.repo.content.caching.ContentCacheImplTest;
import org.alfresco.repo.content.caching.quota.StandardQuotaStrategyMockTest;
import org.alfresco.repo.content.caching.quota.UnlimitedQuotaStrategyTest;
import org.alfresco.repo.content.filestore.SpoofedTextContentReaderTest;
import org.alfresco.repo.content.metadata.MetadataExtracterLimitsTest;
import org.alfresco.repo.content.transform.TransformerConfigTestSuite;
import org.alfresco.repo.domain.propval.PropertyTypeConverterTest;
import org.alfresco.repo.search.DocumentNavigatorTest;
import org.alfresco.repo.search.MLAnaysisModeExpansionTest;
import org.alfresco.repo.search.impl.lucene.MultiReaderTest;
import org.alfresco.repo.search.impl.lucene.index.IndexInfoTest;
import org.alfresco.repo.search.impl.parsers.CMIS_FTSTest;
import org.alfresco.repo.search.impl.parsers.FTSTest;
import org.alfresco.repo.security.authentication.AlfrescoSSLSocketFactoryTest;
import org.alfresco.repo.security.authentication.AuthorizationTest;
import org.alfresco.repo.security.authentication.ChainingAuthenticationServiceTest;
import org.alfresco.repo.security.authentication.NameBasedUserNameGeneratorTest;
import org.alfresco.repo.security.permissions.impl.acegi.FilteringResultSetTest;
import org.alfresco.repo.version.common.VersionHistoryImplTest;
import org.alfresco.repo.version.common.VersionImplTest;
import org.alfresco.repo.version.common.versionlabel.SerialVersionLabelPolicyTest;
import org.alfresco.repo.workflow.WorkflowSuiteContextShutdownTest;
import org.alfresco.repo.workflow.activiti.WorklfowObjectFactoryTest;
import org.alfresco.service.cmr.repository.TemporalSourceOptionsTest;
import org.alfresco.service.cmr.repository.TransformationOptionLimitsTest;
import org.alfresco.service.cmr.repository.TransformationOptionPairTest;
import org.alfresco.util.NumericEncodingTest;
import org.alfresco.util.testing.category.DBTests;
import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * All Repository project UNIT test classes (no application context) should be added to this test suite.
 * Tests marked as DBTests are automatically excluded and are run as part of {@link AllDBTestsTestSuite}.
 */
@RunWith(Categories.class)
@Categories.ExcludeCategory(DBTests.class)
@Suite.SuiteClasses({
        org.alfresco.repo.site.SiteMembershipTest.class,
        org.alfresco.encryption.EncryptorTest.class,
        org.alfresco.encryption.KeyStoreKeyProviderTest.class,
        org.alfresco.filesys.config.ServerConfigurationBeanTest.class,
        org.alfresco.filesys.repo.CIFSContentComparatorTest.class,
        org.alfresco.filesys.repo.rules.ShuffleTest.class,
        org.alfresco.repo.admin.Log4JHierarchyInitTest.class,
        org.alfresco.repo.attributes.PropTablesCleanupJobTest.class,
        org.alfresco.repo.cache.DefaultCacheFactoryTest.class,
        org.alfresco.repo.cache.DefaultSimpleCacheTest.class,
        org.alfresco.repo.cache.lookup.EntityLookupCacheTest.class,
        org.alfresco.repo.calendar.CalendarHelpersTest.class,
        org.alfresco.repo.dictionary.RepoDictionaryDAOTest.class,
        org.alfresco.repo.forms.processor.node.FieldProcessorTest.class,
        org.alfresco.repo.forms.processor.workflow.TaskFormProcessorTest.class,
        org.alfresco.repo.forms.processor.workflow.WorkflowFormProcessorTest.class,
        org.alfresco.repo.invitation.site.InviteSenderTest.class,
        org.alfresco.repo.invitation.site.InviteModeratedSenderTest.class,
        org.alfresco.repo.lock.LockUtilsTest.class,
        org.alfresco.repo.lock.mem.LockStoreImplTest.class,
        org.alfresco.repo.module.ModuleDetailsImplTest.class,
        org.alfresco.repo.module.ModuleVersionNumberTest.class,
        org.alfresco.repo.module.tool.ModuleManagementToolTest.class,
        org.alfresco.repo.module.tool.WarHelperImplTest.class,
        org.alfresco.repo.module.tool.ModuleServiceImplTest.class,
        org.alfresco.repo.policy.MTPolicyComponentTest.class,
        org.alfresco.repo.policy.PolicyComponentTest.class,
        org.alfresco.repo.rendition.RenditionNodeManagerTest.class,
        org.alfresco.repo.rendition.RenditionServiceImplTest.class,
        org.alfresco.repo.replication.ReplicationServiceImplTest.class,
        org.alfresco.repo.service.StoreRedirectorProxyFactoryTest.class,
        org.alfresco.repo.site.RoleComparatorImplTest.class,
        org.alfresco.repo.thumbnail.ThumbnailServiceImplParameterTest.class,
        org.alfresco.repo.transfer.ContentChunkerImplTest.class,
        org.alfresco.repo.transfer.HttpClientTransmitterImplTest.class,
        org.alfresco.repo.transfer.manifest.TransferManifestTest.class,
        org.alfresco.repo.urlshortening.BitlyUrlShortenerTest.class,
        org.alfresco.service.cmr.calendar.CalendarRecurrenceHelperTest.class,
        org.alfresco.service.cmr.calendar.CalendarTimezoneHelperTest.class,
        org.alfresco.tools.RenameUserTest.class,
        org.alfresco.util.FileNameValidatorTest.class,
        org.alfresco.util.HttpClientHelperTest.class,
        org.alfresco.util.JSONtoFmModelTest.class,
        org.alfresco.util.ModelUtilTest.class,
        org.alfresco.util.PropertyMapTest.class,
        org.alfresco.util.ValueProtectingMapTest.class,
        org.alfresco.util.json.ExceptionJsonSerializerTest.class,
        org.alfresco.util.collections.CollectionUtilsTest.class,
        org.alfresco.util.schemacomp.DbObjectXMLTransformerTest.class,
        org.alfresco.util.schemacomp.DbPropertyTest.class,
        org.alfresco.util.schemacomp.DefaultComparisonUtilsTest.class,
        org.alfresco.util.schemacomp.DifferenceTest.class,
        org.alfresco.util.schemacomp.MultiFileDumperTest.class,
        org.alfresco.util.schemacomp.RedundantDbObjectTest.class,
        org.alfresco.util.schemacomp.SchemaComparatorTest.class,
        org.alfresco.util.schemacomp.SchemaToXMLTest.class,
        org.alfresco.util.schemacomp.ValidatingVisitorTest.class,
        org.alfresco.util.schemacomp.ValidationResultTest.class,
        org.alfresco.util.schemacomp.XMLToSchemaTest.class,
        org.alfresco.util.schemacomp.model.ColumnTest.class,
        org.alfresco.util.schemacomp.model.ForeignKeyTest.class,
        org.alfresco.util.schemacomp.model.IndexTest.class,
        org.alfresco.util.schemacomp.model.PrimaryKeyTest.class,
        org.alfresco.util.schemacomp.model.SchemaTest.class,
        org.alfresco.util.schemacomp.model.SequenceTest.class,
        org.alfresco.util.schemacomp.model.TableTest.class,
        org.alfresco.util.schemacomp.validator.IndexColumnsValidatorTest.class,
        org.alfresco.util.schemacomp.validator.NameValidatorTest.class,
        org.alfresco.util.schemacomp.validator.SchemaVersionValidatorTest.class,
        org.alfresco.util.schemacomp.validator.TypeNameOnlyValidatorTest.class,
        org.alfresco.util.test.junitrules.TemporaryMockOverrideTest.class,
        org.alfresco.repo.search.impl.solr.SolrQueryHTTPClientTest.class,
        org.alfresco.repo.search.impl.solr.SolrStatsResultTest.class,
        org.alfresco.repo.search.impl.solr.facet.SolrFacetComparatorTest.class,
        org.alfresco.repo.search.impl.solr.facet.FacetQNameUtilsTest.class,
        org.alfresco.util.BeanExtenderUnitTest.class,
        org.alfresco.repo.search.impl.solr.SpellCheckDecisionManagerTest.class,
        org.alfresco.repo.search.impl.solr.SolrStoreMappingWrapperTest.class,
        org.alfresco.repo.security.authentication.CompositePasswordEncoderTest.class,
        org.alfresco.repo.security.authentication.PasswordHashingTest.class,
        org.alfresco.traitextender.TraitExtenderIntegrationTest.class,
        org.alfresco.traitextender.AJExtensionsCompileTest.class,

        org.alfresco.repo.virtual.page.PageCollatorTest.class,
        org.alfresco.repo.virtual.ref.GetChildByIdMethodTest.class,
        org.alfresco.repo.virtual.ref.GetParentReferenceMethodTest.class,
        org.alfresco.repo.virtual.ref.NewVirtualReferenceMethodTest.class,
        org.alfresco.repo.virtual.ref.PlainReferenceParserTest.class,
        org.alfresco.repo.virtual.ref.PlainStringifierTest.class,
        org.alfresco.repo.virtual.ref.ProtocolTest.class,
        org.alfresco.repo.virtual.ref.ReferenceTest.class,
        org.alfresco.repo.virtual.ref.ResourceParameterTest.class,
        org.alfresco.repo.virtual.ref.StringParameterTest.class,
        org.alfresco.repo.virtual.ref.VirtualProtocolTest.class,
        org.alfresco.repo.virtual.store.ReferenceComparatorTest.class,

        org.alfresco.repo.virtual.ref.ZeroReferenceParserTest.class,
        org.alfresco.repo.virtual.ref.ZeroStringifierTest.class,

        org.alfresco.repo.virtual.ref.HashStringifierTest.class,
        org.alfresco.repo.virtual.ref.NodeRefRadixHasherTest.class,
        org.alfresco.repo.virtual.ref.NumericPathHasherTest.class,
        org.alfresco.repo.virtual.ref.StoredPathHasherTest.class,

        org.alfresco.repo.virtual.template.VirtualQueryImplTest.class,
        org.alfresco.repo.virtual.store.TypeVirtualizationMethodTest.Unit.class,

        org.alfresco.repo.security.authentication.AuthenticationServiceImplTest.class,
        org.alfresco.util.EmailHelperTest.class,
        ParameterDefinitionImplTest.class,
        ActionDefinitionImplTest.class,
        ActionConditionDefinitionImplTest.class,
        ActionImplTest.class,
        ActionConditionImplTest.class,
        CompositeActionImplTest.class,
        CompositeActionConditionImplTest.class,
        AuditableAnnotationTest.class,
        PropertyAuditFilterTest.class,
        SpoofedTextContentReaderTest.class,
        ContentDataTest.class,
        TransformationOptionLimitsTest.class,
        TransformationOptionPairTest.class,
        TransformerConfigTestSuite.class,
        TemporalSourceOptionsTest.class,
        MetadataExtracterLimitsTest.class,
        StandardQuotaStrategyMockTest.class,
        UnlimitedQuotaStrategyTest.class,
        CachingContentStoreTest.class,
        ContentCacheImplTest.class,
        PropertyTypeConverterTest.class,
        MLAnaysisModeExpansionTest.class,
        DocumentNavigatorTest.class,
        MultiReaderTest.class,
        IndexInfoTest.class,
        NumericEncodingTest.class,
        CMIS_FTSTest.class,
        org.alfresco.repo.search.impl.parsers.CMISTest.class,
        FTSTest.class,
        AlfrescoSSLSocketFactoryTest.class,
        AuthorizationTest.class,
        FilteringResultSetTest.class,
        ChainingAuthenticationServiceTest.class,
        NameBasedUserNameGeneratorTest.class,
        VersionImplTest.class,
        VersionHistoryImplTest.class,
        SerialVersionLabelPolicyTest.class,
        WorklfowObjectFactoryTest.class,
        WorkflowSuiteContextShutdownTest.class,
        org.alfresco.repo.search.impl.lucene.analysis.PathTokenFilterTest.class,
        HBDataCollectorServiceImplTest.class
})
public class AllUnitTestsSuite
{
}
