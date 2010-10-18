/*
 * Copyright (c) Open Source Strategies, Inc.
 *
 * Opentaps is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Opentaps is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Opentaps.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.opentaps.search.party;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.opentaps.base.constants.RoleTypeConstants;
import org.opentaps.base.constants.StatusItemConstants;
import org.opentaps.base.entities.PartyRole;
import org.opentaps.search.HibernateSearchService;


/**
 * A common implementation for searching parties with role, Accounts, Contacts, etc ...
 */
public final class PartySearch {

    private PartySearch() { }

    /** Common set of class to query for Party related search. */
    public static final Set<Class<?>> PARTY_CLASSES = new HashSet<Class<?>>(Arrays.asList(
            PartyRole.class
        ));

    /**
     * Builds the Lucene query for a Party Group related search according to the role.
     * For example used for Accounts search.
     * @param sb the string builder instance currently building the query
     * @param roleTypeId the role of the party group to search
     */
    public static void makePartyGroupQuery(StringBuilder sb, String roleTypeId) {
        sb.append("( -party.statusId:").append(StatusItemConstants.PartyStatus.PARTY_DISABLED).append(" +id.roleTypeId:").append(roleTypeId).append(" +(");
        makePartyGroupFieldsQuery(sb);
        sb.append("))");
    }

    /**
     * Builds the Lucene query for a Person related search according to the role.
     * For example used for Contacts search.
     * @param sb the string builder instance currently building the query
     * @param roleTypeId the role of the party group to search
     */
    public static void makePersonQuery(StringBuilder sb, String roleTypeId) {
        sb.append("( -party.statusId:").append(StatusItemConstants.PartyStatus.PARTY_DISABLED).append(" +id.roleTypeId:").append(roleTypeId).append(" +(");
        makePersonFieldsQuery(sb);
        sb.append("))");
    }

    /**
     * Builds a Lucene query for each indexed field of a Person entity.
     * eg: <code>party.person.firstName:? party.person.lastName:? ...</code>
     * @param sb a <code>StringBuilder</code> value
     */
    public static void makePersonFieldsQuery(StringBuilder sb) {
        sb.append("(");
        for (String f : Arrays.asList("partyId", "firstName", "lastName", "middleName", "firstNameLocal", "lastNameLocal", "nickname")) {
            sb.append("party.person.").append(f).append(":").append(HibernateSearchService.DEFAULT_PLACEHOLDER).append(" ");
        }
        sb.append(")");
    }

    /**
     * Builds a Lucene query for each indexed field of a Lead entity.
     * In the case of a lead, this is just like a Person but we also search in companyName / companyNameLocal.
     * @param sb a <code>StringBuilder</code> value
     */
    public static void makeLeadQuery(StringBuilder sb) {
        sb.append("( -party.statusId:").append(StatusItemConstants.PartyStatus.PARTY_DISABLED).append(" +id.roleTypeId:").append(RoleTypeConstants.PROSPECT).append(" +(");
        for (String f : Arrays.asList("partyId", "firstName", "lastName", "middleName", "firstNameLocal", "lastNameLocal", "nickname")) {
            sb.append("party.person.").append(f).append(":").append(HibernateSearchService.DEFAULT_PLACEHOLDER).append(" ");
        }
        sb.append("party.partySupplementalData.companyName:").append(HibernateSearchService.DEFAULT_PLACEHOLDER).append(" ");
        sb.append("party.partySupplementalData.companyNameLocal:").append(HibernateSearchService.DEFAULT_PLACEHOLDER);
        sb.append("))");
    }

    /**
     * Builds a Lucene query for each indexed field of a Party Group entity.
     * eg: <code>party.partyGroup.partyId:? party.partyGroup.groupName:? ...</code>
     * @param sb a <code>StringBuilder</code> value
     */
    public static void makePartyGroupFieldsQuery(StringBuilder sb) {
        sb.append("(");
        for (String f : Arrays.asList("partyId", "groupName")) {
            sb.append("party.partyGroup.").append(f).append(":").append(HibernateSearchService.DEFAULT_PLACEHOLDER).append(" ");
        }
        sb.append(")");
    }

}
