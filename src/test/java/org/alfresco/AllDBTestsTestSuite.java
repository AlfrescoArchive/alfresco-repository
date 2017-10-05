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

import org.alfresco.repo.dictionary.RepoDictionaryDAOTest;
import org.alfresco.repo.domain.audit.AuditDAOTest;
import org.alfresco.repo.domain.contentdata.ContentDataDAOTest;
import org.alfresco.repo.domain.encoding.EncodingDAOTest;
import org.alfresco.repo.domain.locale.LocaleDAOTest;
import org.alfresco.repo.domain.locks.LockDAOTest;
import org.alfresco.repo.domain.mimetype.MimetypeDAOTest;
import org.alfresco.repo.domain.node.NodeDAOTest;
import org.alfresco.repo.domain.patch.AppliedPatchDAOTest;
import org.alfresco.repo.domain.permissions.AclCrudDAOTest;
import org.alfresco.repo.domain.propval.PropertyValueCleanupTest;
import org.alfresco.repo.domain.propval.PropertyValueDAOTest;
import org.alfresco.repo.domain.qname.QNameDAOTest;
import org.alfresco.repo.domain.query.CannedQueryDAOTest;
import org.alfresco.repo.domain.solr.SOLRDAOTest;
import org.alfresco.repo.domain.tenant.TenantAdminDAOTest;
import org.alfresco.repo.domain.usage.UsageDAOTest;
import org.alfresco.repo.node.db.DbNodeServiceImplPropagationTest;
import org.alfresco.repo.node.db.DbNodeServiceImplTest;
import org.alfresco.repo.security.permissions.impl.AclDaoComponentTest;
import org.alfresco.util.schemacomp.DbToXMLTest;
import org.alfresco.util.schemacomp.ExportDbTest;
import org.alfresco.util.schemacomp.SchemaReferenceFileTest;
import org.alfresco.util.testing.category.DBTests;
import org.alfresco.util.testing.category.NonBuildTests;
import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Repository project database tests.
 */
@RunWith(Categories.class)
@Categories.IncludeCategory(DBTests.class)
@Categories.ExcludeCategory(NonBuildTests.class)
@Suite.SuiteClasses({DbToXMLTest.class,
    ExportDbTest.class,
    SchemaReferenceFileTest.class,
    AuditDAOTest.class,
    ContentDataDAOTest.class,
    EncodingDAOTest.class,
    LocaleDAOTest.class,
    LockDAOTest.class,
    MimetypeDAOTest.class,
    NodeDAOTest.class,
    AppliedPatchDAOTest.class,
    AclCrudDAOTest.class,
    PropertyValueCleanupTest.class,
    PropertyValueDAOTest.class,
    QNameDAOTest.class,
    CannedQueryDAOTest.class,
    SOLRDAOTest.class,
    TenantAdminDAOTest.class,
    UsageDAOTest.class,
    AclDaoComponentTest.class,
    RepoDictionaryDAOTest.class,
    DbNodeServiceImplPropagationTest.class,
    DbNodeServiceImplTest.class,})
public class AllDBTestsTestSuite
{
}
