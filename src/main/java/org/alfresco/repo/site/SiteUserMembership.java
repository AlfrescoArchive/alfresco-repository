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
package org.alfresco.repo.site;

import org.alfresco.api.AlfrescoPublicApi;
import org.alfresco.query.CannedQuerySortDetails;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.util.Pair;

import java.util.Comparator;
import java.util.List;


@AlfrescoPublicApi
public class SiteUserMembership extends SiteMembership
{
    private final String firstName;
    private final String lastName;
    private final boolean isMemberOfGroup;

    public SiteUserMembership(SiteInfo siteInfo, String id, String firstName, String lastName, String role, boolean isMemberOfGroup)
    {
        super(siteInfo, id, firstName, lastName, role);
        this.firstName = firstName;
        this.lastName = lastName;
        this.isMemberOfGroup = isMemberOfGroup;
    }

    @Override
    public String getFirstName()
    {
        return firstName;
    }

    @Override
    public String getLastName()
    {
        return lastName;
    }

    public boolean isMemberOfGroup()
    {
        return isMemberOfGroup;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SiteUserMembership that = (SiteUserMembership) o;
        return firstName.equals(that.firstName) &&
                lastName.equals(that.lastName);
    }

    @Override
    public String toString()
    {
        return "SiteUserMembership{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                '}';
    }


    static int compareTo(List<Pair<? extends Object, CannedQuerySortDetails.SortOrder>> sortPairs, SiteUserMembership o1, SiteUserMembership o2)
    {
        String personId1 = o1.getPersonId();
        String personId2 = o2.getPersonId();
        String firstName1 = o1.getFirstName();
        String firstName2 = o2.getFirstName();
        String lastName1 = o1.getLastName();
        String lastName2 = o2.getLastName();
        String siteRole1 = o1.getRole();
        String siteRole2 = o2.getRole();
        String shortName1 = o1.getSiteInfo().getShortName();
        String shortName2 = o2.getSiteInfo().getShortName();

        int personId = SiteMembershipComparator.safeCompare(personId1, personId2);
        int firstName = SiteMembershipComparator.safeCompare(firstName1, firstName2);
        int siteShortName = SiteMembershipComparator.safeCompare(shortName1, shortName2);
        int lastName = SiteMembershipComparator.safeCompare(lastName1, lastName2);
        int siteRole = SiteMembershipComparator.safeCompare(siteRole1, siteRole2);

        if (siteRole == 0 && siteShortName == 0 && personId == 0)
        {
            // equals contract
            return 0;
        }

        return SiteMembershipComparator.compareSiteMembersBody(sortPairs, personId1, personId2, lastName1, lastName2, siteRole1, siteRole2, personId, firstName, lastName, siteRole, 0);
    }

    static Comparator<SiteUserMembership> getComparator(List<Pair<?, CannedQuerySortDetails.SortOrder>> sortPairs)
    {
        return (SiteUserMembership o1, SiteUserMembership o2) -> compareTo(sortPairs, o1, o2);
    }
}
