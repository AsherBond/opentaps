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

package org.opentaps.gwt.common.client.lookup.configuration;

import java.util.Arrays;
import java.util.List;

/**
 * Defines the interface between the server and client for the PartyLookupService
 * Technically not a java interface, but it defines all the constants needed on both sides
 *  which makes the code more robust.
 */
public abstract class PartyLookupConfiguration {

    private PartyLookupConfiguration() { }

    public static final String URL_FIND_CONTACTS = "gwtFindContacts";
    public static final String URL_FIND_ACCOUNTS = "gwtFindAccounts";
    public static final String URL_FIND_LEADS = "gwtFindLeads";
    public static final String URL_FIND_PARTNERS = "gwtFindPartners";
    public static final String URL_FIND_SUPPLIERS = "gwtFindSuppliers";
    public static final String URL_SUGGEST_CONTACTS = "gwtSuggestContacts";
    public static final String URL_SUGGEST_ACCOUNTS = "gwtSuggestAccounts";
    public static final String URL_SUGGEST_LEADS = "gwtSuggestLeads";
    public static final String URL_SUGGEST_ACCOUNTS_OR_LEADS = "gwtSuggestAccountsOrLeads";
    public static final String URL_SUGGEST_ACCOUNTS_OR_QUALIFIED_LEADS = "gwtSuggestAccountsOrQualifiedLeads";
    public static final String URL_SEARCH_CONTACTS = "gwtSearchContacts";
    public static final String URL_SEARCH_ACCOUNTS = "gwtSearchAccounts";
    public static final String URL_SEARCH_LEADS = "gwtSearchLeads";
    public static final String URL_SEARCH_PARTNERS = "gwtSearchPartners";
    public static final String URL_SEARCH_SUPPLIERS = "gwtSearchSuppliers";

    public static final String IN_RESPONSIBILTY = "MyOrTeamResponsibility";
    public static final String MY_VALUES = "MY_VALUES";
    public static final String TEAM_VALUES = "TEAM_VALUES";

    public static final String INOUT_PARTY_ID = "partyId";
    public static final String INOUT_COMPANY_NAME = "companyName";
    public static final String INOUT_GROUP_NAME = "groupName";
    public static final String INOUT_FIRST_NAME = "firstName";
    public static final String INOUT_LAST_NAME = "lastName";
    public static final String INOUT_ADDRESS = "primaryAddress1";
    public static final String INOUT_COUNTRY = "primaryCountryAbbreviation";
    public static final String INOUT_STATE = "primaryStateProvinceAbbreviation";
    public static final String INOUT_CITY = "primaryCity";
    public static final String INOUT_POSTAL_CODE = "primaryPostalCode";
    public static final String INOUT_PHONE_COUNTRY_CODE = "primaryCountryCode";
    public static final String INOUT_PHONE_AREA_CODE = "primaryAreaCode";
    public static final String INOUT_PHONE_NUMBER = "primaryContactNumber";
    public static final String INOUT_FORMATED_PHONE_NUMBER = "formatedPrimaryPhone";
    public static final String INOUT_FRIENDLY_PARTY_NAME = "friendlyPartyName";
    public static final String IN_CLASSIFICATION = "partyClassificationGroupId";
    public static final String INOUT_TO_NAME = "primaryToName";
    public static final String OUT_ADDRESS_ID = "primaryPostalAddressId";
    public static final String INOUT_ATTENTION_NAME = "primaryAttnName";
    public static final String OUT_ADDRESS_2 = "primaryAddress2";
    public static final String OUT_POSTAL_CODE_EXT = "primaryPostalCodeExt";
    public static final String OUT_PHONE_ID = "primaryTelecomNumberId";
    public static final String INOUT_EMAIL = "primaryEmail";
    public static final String OUT_EMAIL_CONTACT_MECH_ID = "primaryEmailContactMechId";
    public static final String OUT_EMAIL_ID = "primaryEmailId";
    public static final String IN_PARTY_ID_TO = "partyIdTo";
    public static final String IN_PARTY_ID_FROM = "partyIdFrom";
    public static final String IN_ROLE_TO = "roleTypeIdTo";
    public static final String IN_RELATIONSHIP_TYPE_ID = "partyRelationshipTypeId";
    public static final String IN_SALES_OPPORTUNITY_ID = "salesOpportunityId";
    public static final String IN_ACTIVE_PARTIES_ONLY = "activeOnly";
    public static final String OUT_VOIP_ENABLED = "voipEnabled";
    public static final String OUT_PARTY_STATUS_DESCRIPTION = "statusDescription";

    public static final List<String> LIST_OUT_FIELDS = Arrays.asList(
        INOUT_PARTY_ID,
        INOUT_COMPANY_NAME,
        INOUT_GROUP_NAME,
        INOUT_FIRST_NAME,
        INOUT_LAST_NAME,
        INOUT_ADDRESS,
        INOUT_COUNTRY,
        INOUT_STATE,
        INOUT_CITY,
        INOUT_POSTAL_CODE,
        INOUT_PHONE_COUNTRY_CODE,
        INOUT_PHONE_AREA_CODE,
        INOUT_PHONE_NUMBER,
        INOUT_TO_NAME,
        INOUT_ATTENTION_NAME,
        OUT_ADDRESS_2,
        OUT_POSTAL_CODE_EXT,
        INOUT_EMAIL,
        OUT_EMAIL_CONTACT_MECH_ID,
        OUT_EMAIL_ID,
        OUT_PHONE_ID,
        OUT_ADDRESS_ID,
        OUT_PARTY_STATUS_DESCRIPTION
    );

}
