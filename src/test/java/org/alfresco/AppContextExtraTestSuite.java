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

import junit.framework.TestSuite;
import org.alfresco.repo.action.ActionServiceImpl2Test;
import org.alfresco.repo.action.ActionServiceImplTest;
import org.alfresco.repo.action.executer.ImporterActionExecuterTest;
import org.alfresco.repo.action.executer.MailActionExecuterTest;
import org.alfresco.repo.security.authentication.ResetPasswordServiceImplTest;
import org.alfresco.repo.virtual.bundle.FileInfoPropsComparatorTest;
import org.alfresco.repo.virtual.bundle.VirtualCheckOutCheckInServiceExtensionTest;
import org.alfresco.repo.virtual.bundle.VirtualFileFolderServiceExtensionTest;
import org.alfresco.repo.virtual.bundle.VirtualLockableAspectInterceptorExtensionTest;
import org.alfresco.repo.virtual.bundle.VirtualNodeServiceExtensionTest;
import org.alfresco.repo.virtual.bundle.VirtualPermissionServiceExtensionTest;
import org.alfresco.repo.virtual.bundle.VirtualPreferenceServiceExtensionTest;
import org.alfresco.repo.virtual.bundle.VirtualRatingServiceExtensionTest;
import org.alfresco.repo.virtual.bundle.VirtualVersionServiceExtensionTest;
import org.alfresco.repo.virtual.config.NodeRefPathExpressionTest;
import org.alfresco.repo.virtual.model.SystemTemplateLocationsConstraintTest;
import org.alfresco.repo.virtual.store.SystemVirtualizationMethodTest;
import org.alfresco.repo.virtual.store.TypeVirtualizationMethodTest;
import org.alfresco.repo.virtual.store.VirtualStoreImplTest;
import org.alfresco.repo.virtual.template.ApplyTemplateMethodTest;
import org.alfresco.repo.virtual.template.TemplateFilingRuleTest;
import org.alfresco.repo.virtual.template.TemplateResourceProcessorTest;
import org.alfresco.repo.workflow.activiti.ActivitiMultitenantWorkflowTest;
import org.alfresco.util.testing.category.DBTests;
import org.alfresco.util.testing.category.NonBuildTests;
import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Repository project tests using the main context alfresco/application-context.xml PLUS some additional context.
 * Tests marked as DBTests are automatically excluded and are run as part of {@link AllDBTestsTestSuite}.
 */
@RunWith(Categories.class)
@Categories.ExcludeCategory({DBTests.class, NonBuildTests.class})
@Suite.SuiteClasses({
    // ----------------------------------------------------------------------
    // globalIntegrationTestContext [classpath:alfresco/application-context.xml, classpath:alfresco/test/global-integration-test-context.xml]
    // ----------------------------------------------------------------------

    MailActionExecuterTest.class,
    ActionServiceImpl2Test.class,
    ImporterActionExecuterTest.class,
    org.alfresco.repo.dictionary.CustomModelServiceImplTest.class,
    org.alfresco.repo.dictionary.ValueDataTypeValidatorImplTest.class,
    org.alfresco.repo.download.DownloadServiceIntegrationTest.class,
    org.alfresco.repo.forum.CommentsTest.class,
    org.alfresco.repo.jscript.ScriptNodeTest.class,
    org.alfresco.repo.preference.PreferenceServiceImplTest.class,
    org.alfresco.repo.rule.MiscellaneousRulesTest.class,
    org.alfresco.repo.rule.RuleServiceIntegrationTest.class,
    ResetPasswordServiceImplTest.class,
    org.alfresco.repo.subscriptions.SubscriptionServiceActivitiesTest.class,
    org.alfresco.util.test.junitrules.AlfrescoPersonTest.class,

    // the following test only passes on a clean DB
    org.alfresco.util.test.junitrules.TemporaryNodesTest.class,

    org.alfresco.repo.search.impl.solr.facet.SolrFacetQueriesDisplayHandlersTest.class,
    org.alfresco.repo.search.impl.solr.facet.SolrFacetServiceImplTest.class,
    org.alfresco.repo.invitation.InvitationCleanupTest.class,
    org.alfresco.repo.quickshare.QuickShareServiceIntegrationTest.class,
    org.alfresco.repo.remotecredentials.RemoteCredentialsServicesTest.class,

    // ----------------------------------------------------------------------
    // Context_extra
    // ----------------------------------------------------------------------

    // [classpath:alfresco/application-context.xml, classpath:org/alfresco/repo/site/site-custom-context.xml]
    org.alfresco.repo.site.SiteServiceImplTest.class,

    // [classpath:alfresco/application-context.xml, classpath:scriptexec/script-exec-test.xml]
    org.alfresco.repo.domain.schema.script.ScriptExecutorImplIntegrationTest.class,
    org.alfresco.repo.domain.schema.script.ScriptBundleExecutorImplIntegrationTest.class,

    // [classpath:alfresco/application-context.xml, classpath:alfresco/test/global-integration-test-context.xml,
    // classpath:org/alfresco/util/test/junitrules/dummy1-context.xml,
    // classpath:org/alfresco/util/test/junitrules/dummy2-context.xml]
    org.alfresco.util.test.junitrules.ApplicationContextInitTest.class,

    // [classpath:alfresco/application-context.xml,
    // classpath:org/alfresco/repo/client/config/test-repo-clients-apps-context.xml]
    org.alfresco.repo.client.config.ClientAppConfigTest.class,

    // [classpath:alfresco/application-context.xml,
    // classpath:org/alfresco/repo/policy/annotation/test-qname-type-editor-context.xml]
    org.alfresco.repo.policy.annotation.QNameTypeEditorTest.class,

    // [classpath:alfresco/application-context.xml, classpath:org/alfresco/repo/forms/MNT-7383-context.xml]
    org.alfresco.repo.forms.processor.action.ActionFormProcessorTest.class,

    // [classpath:alfresco/application-context.xml, classpath:alfresco/filesys/auth/cifs/test-kerberos-context.xml]
    org.alfresco.filesys.auth.cifs.CifsAuthenticatorKerberosTest.class,

    // [classpath:alfresco/application-context.xml, classpath:test-cmisinteger_modell-context.xml]
    org.alfresco.opencmis.CMISTest.class,

    // [classpath:alfresco/application-context.xml, classpath:org/alfresco/repo/action/test-action-services-context.xml]
    ActionServiceImplTest.class,

    // [classpath:alfresco/application-context.xml, classpath:alfresco/test/global-integration-test-context.xml,
    // classpath:ratings/test-RatingServiceIntegrationTest-context.xml]
    org.alfresco.repo.rating.RatingServiceIntegrationTest.class,

    // [classpath:alfresco/application-context.xml, classpath:sync-test-context.xml]
    org.alfresco.repo.security.sync.ChainingUserRegistrySynchronizerTest.class,

    // [classpath:alfresco/application-context.xml, classpath:alfresco/test/global-integration-test-context.xml,
    // classpath:sites/test-TemporarySitesTest-context.xml]
    org.alfresco.repo.site.SiteServiceImplMoreTest.class,
    org.alfresco.util.test.junitrules.TemporarySitesTest.class,

    // ======================================================================
    // any other order may lead to failing tests
    // ======================================================================

    // ----------------------------------------------------------------------
    // virtualizationTestContext [classpath:**/virtualization-test-context.xml, classpath:alfresco/application-context.xml]
    // ----------------------------------------------------------------------
    VirtualPreferenceServiceExtensionTest.class,
    VirtualLockableAspectInterceptorExtensionTest.class,
    VirtualVersionServiceExtensionTest.class,
    VirtualRatingServiceExtensionTest.class,
    VirtualCheckOutCheckInServiceExtensionTest.class,
    VirtualPermissionServiceExtensionTest.class,
    VirtualNodeServiceExtensionTest.class,
    VirtualFileFolderServiceExtensionTest.class,
    ApplyTemplateMethodTest.class,
    SystemTemplateLocationsConstraintTest.class,
    SystemVirtualizationMethodTest.class,
    TypeVirtualizationMethodTest.Integration.class,
    TemplateResourceProcessorTest.class,
    VirtualStoreImplTest.class,
    NodeRefPathExpressionTest.class,
    TemplateFilingRuleTest.class,
    FileInfoPropsComparatorTest.class,

    // ----------------------------------------------------------------------
    // testSubscriptionsContext  [classpath:alfresco/application-context.xml, classpath:test/alfresco/test-subscriptions-context.xml]
    // TODO can we remove this? Was it EOLed?
    // ----------------------------------------------------------------------
    org.alfresco.repo.subscriptions.SubscriptionServiceImplTest.class,

    // ----------------------------------------------------------------------
    // openCmisContext [classpath:alfresco/application-context.xml, classpath:opencmis/opencmistest-context.xml]
    // ----------------------------------------------------------------------
    org.alfresco.opencmis.OpenCmisLocalTest.class,

    // ----------------------------------------------------------------------
    // cacheTestContext [classpath:alfresco/application-context.xml, classpath:cache-test/cache-test-context.xml]
    // ----------------------------------------------------------------------
    org.alfresco.repo.cache.CacheTest.class,

    // ----------------------------------------------------------------------
    // mtAllContext [classpath:alfresco/application-context.xml, classpath:tenant/mt-*context.xml]
    // ----------------------------------------------------------------------
    org.alfresco.repo.tenant.MultiTDemoTest.class,
    ActivitiMultitenantWorkflowTest.class
})
public class AppContextExtraTestSuite extends TestSuite
{
}
