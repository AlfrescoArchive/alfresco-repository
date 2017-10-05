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
import org.alfresco.repo.domain.audit.AuditDAOTest;
import org.alfresco.repo.domain.contentdata.ContentDataDAOTest;
import org.alfresco.repo.domain.encoding.EncodingDAOTest;
import org.alfresco.repo.domain.locale.LocaleDAOTest;
import org.alfresco.repo.domain.locks.LockDAOTest;
import org.alfresco.repo.domain.mimetype.MimetypeDAOTest;
import org.alfresco.repo.domain.node.NodeDAOTest;
import org.alfresco.repo.domain.patch.AppliedPatchDAOTest;
import org.alfresco.repo.domain.permissions.AclCrudDAOTest;
import org.alfresco.repo.domain.permissions.FixedAclUpdaterTest;
import org.alfresco.repo.domain.propval.PropertyValueCleanupTest;
import org.alfresco.repo.domain.propval.PropertyValueDAOTest;
import org.alfresco.repo.domain.qname.QNameDAOTest;
import org.alfresco.repo.domain.solr.SOLRDAOTest;
import org.alfresco.repo.domain.tenant.TenantAdminDAOTest;
import org.alfresco.repo.domain.usage.UsageDAOTest;
import org.alfresco.repo.ownable.impl.OwnableServiceTest;
import org.alfresco.repo.security.authentication.AuthenticationBootstrapTest;
import org.alfresco.repo.security.authentication.UpgradePasswordHashTest;
import org.alfresco.repo.security.authentication.external.DefaultRemoteUserMapperTest;
import org.alfresco.repo.security.authentication.external.LocalAuthenticationServiceTest;
import org.alfresco.repo.security.authentication.subsystems.SubsystemChainingFtpAuthenticatorTest;
import org.alfresco.repo.security.authority.AuthorityBridgeTableAsynchronouslyRefreshedCacheTest;
import org.alfresco.repo.security.authority.AuthorityServiceTest;
import org.alfresco.repo.security.authority.DuplicateAuthorityTest;
import org.alfresco.repo.security.authority.ExtendedPermissionServiceTest;
import org.alfresco.repo.security.permissions.dynamic.LockOwnerDynamicAuthorityTest;
import org.alfresco.repo.security.permissions.impl.AclDaoComponentTest;
import org.alfresco.repo.security.permissions.impl.PermissionServiceTest;
import org.alfresco.repo.security.permissions.impl.ReadPermissionTest;
import org.alfresco.repo.security.permissions.impl.acegi.ACLEntryAfterInvocationTest;
import org.alfresco.repo.security.permissions.impl.acegi.ACLEntryVoterTest;
import org.alfresco.repo.security.permissions.impl.model.PermissionModelTest;
import org.alfresco.repo.security.person.HomeFolderProviderSynchronizerTest;
import org.alfresco.repo.security.person.PersonTest;
import org.alfresco.util.testing.category.DBTests;
import org.alfresco.util.testing.category.NonBuildTests;
import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Repository project tests using the main context alfresco/application-context.xml.
 * To balance test jobs tests using this context have been split into multiple test suites.
 * Tests marked as DBTests are automatically excluded and are run as part of {@link AllDBTestsTestSuite}.
 */
@RunWith(Categories.class)
@Categories.ExcludeCategory({DBTests.class, NonBuildTests.class})
@Suite.SuiteClasses({
    NodeDAOTest.class,
    AuthenticationBootstrapTest.class,
    AuthorityServiceTest.class,
    DuplicateAuthorityTest.class,
    ExtendedPermissionServiceTest.class,
    LockOwnerDynamicAuthorityTest.class,
    AclDaoComponentTest.class,
    PermissionServiceTest.class,
    ACLEntryAfterInvocationTest.class,
    ACLEntryVoterTest.class,
    PermissionModelTest.class,
    PersonTest.class,
    OwnableServiceTest.class,
    ReadPermissionTest.class,
    UpgradePasswordHashTest.class,
    AuthorityBridgeTableAsynchronouslyRefreshedCacheTest.class,
    HomeFolderProviderSynchronizerTest.class,
    FixedAclUpdaterTest.class,
    DefaultRemoteUserMapperTest.class,
    SubsystemChainingFtpAuthenticatorTest.class,
    LocalAuthenticationServiceTest.class,
    ContentDataDAOTest.class,
    EncodingDAOTest.class,
    LockDAOTest.class,
    MimetypeDAOTest.class,
    LocaleDAOTest.class,
    QNameDAOTest.class,
    PropertyValueDAOTest.class,
    AppliedPatchDAOTest.class,
    AclCrudDAOTest.class,
    UsageDAOTest.class,
    SOLRDAOTest.class,
    TenantAdminDAOTest.class,

    // REOPO-1012 : run AuditDAOTest and PropertyValueCleanupTest near the end
    // because their failure can cause other tests to fail on MS SQL
    // AuditDAOTest fails if it runs after CannedQueryDAOTest so this order is a compromise
    // CannedQueryDAOTest will fail on MS SQL if either AuditDAOTest or PropertyValueCleanupTest fail
    PropertyValueCleanupTest.class,
    AuditDAOTest.class,
    org.alfresco.repo.model.ModelTestSuite.class,
    org.alfresco.repo.tenant.MultiTNodeServiceInterceptorTest.class,
    org.alfresco.repo.transfer.RepoTransferReceiverImplTest.class,
})
public class AppContext03TestSuite extends TestSuite
{
}
