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
/* Copyright (c) Open Source Strategies, Inc. */

/*
 *  $Id:$
 *
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.opensourcestrategies.crmsfa.party;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javolution.util.FastList;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericEntityNotFoundException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.order.order.OrderReadHelper;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.builder.EntityListBuilder;
import org.opentaps.common.builder.ListBuilder;
import org.opentaps.common.order.UtilOrder;
import org.opentaps.common.party.PartyContactHelper;
import org.opentaps.common.template.freemarker.FreemarkerUtil;
import org.opentaps.common.util.UtilCommon;

import freemarker.template.TemplateException;

/**
 * Party Helper methods which are designed to provide a consistent set of APIs that can be reused by
 * higher level services.
 *
 * Many of the methods have been migrated to org.opentaps.common.party.PartyHelper.  However, this class also has a lot of
 * CRMSFA specific functionality, so the code inside of these methods have been replaced to reference the common PartyHelper,
 * but we will keep this class and its methods.
 */
public final class PartyHelper {

    private PartyHelper() { }

    private static final String MODULE = PartyHelper.class.getName();

    public static List<String> TEAM_MEMBER_ROLES = UtilMisc.toList("ACCOUNT_MANAGER", "ACCOUNT_REP", "CUST_SERVICE_REP");
    public static List<String> CLIENT_PARTY_ROLES = UtilMisc.toList("ACCOUNT", "CONTACT", "PROSPECT", "PARTNER");
    public static List<String> FIND_PARTY_FIELDS = Arrays.asList(new String[]{"firstName", "lastName", "groupName", "partyId", "companyName", "primaryEmailId", "primaryPostalAddressId", "primaryTelecomNumberId", "primaryCity", "primaryStateProvinceGeoId", "primaryCountryGeoId", "primaryEmail", "primaryCountryCode", "primaryAreaCode", "primaryContactNumber"});

    /**
     * A helper method which finds the first valid roleTypeId for a partyId, using a List of possible roleTypeIds.
     *
     * @param partyId
     * @param possibleRoleTypeIds a List of roleTypeIds
     * @param delegator
     * @return the first roleTypeId from possibleRoleTypeIds which is actually found in PartyRole for the given partyId
     * @throws GenericEntityException
     */
    public static String getFirstValidRoleTypeId(String partyId, List<String> possibleRoleTypeIds, Delegator delegator) throws GenericEntityException {
        return org.opentaps.common.party.PartyHelper.getFirstValidRoleTypeId(partyId, possibleRoleTypeIds, delegator);

    }

    /**
     * As above, but pass in the list of internal party roles, such as ACCOUNT, CONTACT, PROSPECT.
     */
    public static String getFirstValidInternalPartyRoleTypeId(String partyId, Delegator delegator) throws GenericEntityException {
        return getFirstValidRoleTypeId(partyId, CLIENT_PARTY_ROLES, delegator);
    }

    /**
     * As above, but pass in the list of team member roles such as ACCOUNT_REP, etc.
     */
    public static String getFirstValidTeamMemberRoleTypeId(String partyId, Delegator delegator) throws GenericEntityException {
        return getFirstValidRoleTypeId(partyId, TEAM_MEMBER_ROLES, delegator);
    }

    /** Find the first valid role of the party, whether it be a team member or client party. */
    public static String getFirstValidCrmsfaPartyRoleTypeId(String partyId, Delegator delegator) throws GenericEntityException {
        String roleTypeId = getFirstValidRoleTypeId(partyId, TEAM_MEMBER_ROLES, delegator);
        if (roleTypeId == null) {
            roleTypeId = getFirstValidRoleTypeId(partyId, CLIENT_PARTY_ROLES, delegator);
        }
        return roleTypeId;
    }

    /**
     * A helper method for creating a PartyRelationship entity from partyIdTo to partyIdFrom with specified partyRelationshipTypeId, roleTypeIdFrom,
     * a List of valid roles for the to-party, and a flag to expire any existing relationships between the two parties of the same
     * type.   The idea is that several services would do validation and then use this method to do all the work.
     *
     * @param partyIdTo
     * @param partyIdFrom
     * @param roleTypeIdFrom
     * @param partyRelationshipTypeId
     * @param securityGroupId
     * @param validToPartyRoles  List of roleTypeIds which are valid for the partyIdTo in the create relationship.  It will cycle
     * through until the first of these roles is actually associated with partyIdTo and then create a PartyRelationship using that
     * roleTypeId.  If none of these are associated with partyIdTo, then it will return false
     * @param fromDate
     * @param expireExistingRelationships  If set to true, will look for all existing PartyRelationships of partyIdFrom, partyRelationshipTypeId
     * and expire all of them as of the passed in fromDate
     * @return false if no relationship was created or true if operation succeeds
     */
    public static boolean createNewPartyToRelationship(String partyIdTo, String partyIdFrom, String roleTypeIdFrom,
            String partyRelationshipTypeId, String securityGroupId, List<String> validToPartyRoles, Timestamp fromDate,
            boolean expireExistingRelationships, GenericValue userLogin, Delegator delegator, LocalDispatcher dispatcher)
            throws GenericEntityException, GenericServiceException {

        return org.opentaps.common.party.PartyHelper.createNewPartyToRelationship(partyIdTo, partyIdFrom, roleTypeIdFrom, partyRelationshipTypeId, securityGroupId,
                validToPartyRoles, fromDate, expireExistingRelationships, userLogin, delegator, dispatcher);
    }

    /**
     * Same as above except uses a default of now for the timestamp.
     */
    public static boolean createNewPartyToRelationship(String partyIdTo, String partyIdFrom, String roleTypeIdFrom,
            String partyRelationshipTypeId, String securityGroupId, List<String> validToPartyRoles,
            boolean expireExistingRelationships, GenericValue userLogin, Delegator delegator, LocalDispatcher dispatcher)
            throws GenericEntityException, GenericServiceException {
        return createNewPartyToRelationship(partyIdTo, partyIdFrom, roleTypeIdFrom,
                partyRelationshipTypeId, securityGroupId, validToPartyRoles, UtilDateTime.nowTimestamp(),
                expireExistingRelationships, userLogin, delegator, dispatcher);
    }

    /**
     * Expires a list of PartyRelationships that are still active on expireDate.
     */
    public static void expirePartyRelationships(List<GenericValue> partyRelationships, Timestamp expireDate, LocalDispatcher dispatcher, GenericValue userLogin)
        throws GenericServiceException {
        org.opentaps.common.party.PartyHelper.expirePartyRelationships(partyRelationships, expireDate, dispatcher, userLogin);
    }

    /**
     * Method to get the current non-expired party responsible for the given account/contact/lead.
     *
     * @param   partyIdFrom     The partyId of the account/contact/lead
     * @param   roleTypeIdFrom  The role of the account/contact/lead (e.g., ACCOUNT, CONTACT, LEAD)
     * @return  First non-expired PartySummaryCRMView or null if none found
     */
    public static GenericValue getCurrentResponsibleParty(String partyIdFrom, String roleTypeIdFrom, Delegator delegator) throws GenericEntityException {
        return getActivePartyByRole("RESPONSIBLE_FOR", partyIdFrom, roleTypeIdFrom, UtilDateTime.nowTimestamp(), delegator);
    }

    /** Method to get the current lead owner of a lead. */
    public static GenericValue getCurrentLeadOwner(String leadPartyId, Delegator delegator) throws GenericEntityException {
        return getActivePartyByRole("RESPONSIBLE_FOR", leadPartyId, "PROSPECT", "LEAD_OWNER", UtilDateTime.nowTimestamp(), delegator);
    }

    /**
     * Common method used by getCurrentlyResponsibleParty and related methods. This method will obtain the first PartyRelationship found with the given criteria
     * and return the PartySummaryCRMView with partyId = partyRelationship.partyIdTo.
     *
     * @param   partyRelationshipTypeId         The party relationship (e.g., reps that are RESPONSIBLE_FOR an account)
     * @param   partyIdFrom                     The partyId of the account/contact/lead
     * @param   roleTypeIdFrom                  The role of the account/contact/lead (e.g., ACCOUNT, CONTACT, LEAD)
     * @param   securityGroupId                 Optional securityGroupId of the relationship
     * @param   activeDate                      Check only for active relationships as of this timestamp
     * @return  First non-expired PartySummaryCRMView or null if none found
     */
    public static GenericValue getActivePartyByRole(String partyRelationshipTypeId, String partyIdFrom, String roleTypeIdFrom, String securityGroupId,
            Timestamp activeDate, Delegator delegator)
            throws GenericEntityException {
        return org.opentaps.common.party.PartyHelper.getActivePartyByRole(partyRelationshipTypeId, partyIdFrom, roleTypeIdFrom, securityGroupId, activeDate, delegator);
    }

    /** As above but without security group Id specified */
    public static GenericValue getActivePartyByRole(String partyRelationshipTypeId, String partyIdFrom, String roleTypeIdFrom,
            Timestamp activeDate, Delegator delegator)
            throws GenericEntityException {
        return getActivePartyByRole(partyRelationshipTypeId, partyIdFrom, roleTypeIdFrom, null, activeDate, delegator);
    }

    /**
     * Method to copy all "To" relationships of a From party to another From party. For instance, use this method to copy all relationships of an
     * account (or optionally a specific relationship), such as the managers and reps, over to a team.
     * NOTE: This service works on unexpired relationships as of now and will need to be refactored for other Dates.
     *
     * @param   partyIdFrom
     * @param   roleTypeIdFrom
     * @param   partyRelationshipTypeId         optional
     * @param   newPartyIdFrom
     * @param   newRoleTypeIdFrom
     */
    public static void copyToPartyRelationships(String partyIdFrom, String roleTypeIdFrom, String partyRelationshipTypeId,
            String newPartyIdFrom, String newRoleTypeIdFrom, GenericValue userLogin, Delegator delegator, LocalDispatcher dispatcher)
            throws GenericEntityException, GenericServiceException {

        org.opentaps.common.party.PartyHelper.copyToPartyRelationships(partyIdFrom, roleTypeIdFrom, partyRelationshipTypeId, newPartyIdFrom, newRoleTypeIdFrom, userLogin, delegator, dispatcher);

    }

    /**
     * Same as above, but passes partyRelationshipTypeId = null so that all relationship types are selected.
     */
    public static void copyToPartyRelationships(String partyIdFrom, String roleTypeIdFrom, String newPartyIdFrom, String newRoleTypeIdFrom,
            GenericValue userLogin, Delegator delegator, LocalDispatcher dispatcher)
            throws GenericEntityException, GenericServiceException {

        copyToPartyRelationships(partyIdFrom, roleTypeIdFrom, null, newPartyIdFrom, newRoleTypeIdFrom, userLogin, delegator, dispatcher);
    }

    /**
     * This array determines the entities in which to delete the party and the order of deletion.
     * The second element in each row denotes the partyId field to check.
     * XXX Note: We are deleting historical data. For instance, activity records
     * involving the partyId will be gone forever!
     */
    private static String[][] CRM_PARTY_DELETE_CASCADE = {
        {"CustRequestRole", "partyId"},
        {"PartyNote", "partyId"},
        {"PartyDataSource", "partyId"},
        {"WorkEffortPartyAssignment", "partyId"},
        {"PartyContactMechPurpose", "partyId"},
        {"PartyContactMech", "partyId"},
        {"PartySupplementalData", "partyId"},
        {"PartyNameHistory", "partyId"},
        {"PartyGroup", "partyId"},
        {"PartyRelationship", "partyIdFrom"},
        {"PartyRelationship", "partyIdTo"},
        {"Person", "partyId"},
        {"CommunicationEventRole", "partyId"},
        {"ContentRole", "partyId"},
        {"FacilityParty", "partyId"},
        {"MarketingCampaignRole", "partyId"},
        {"PartyRole", "partyId"},
        {"PartyContent", "partyId"},
        {"PartyStatus", "partyId"}
    };

    /**
     * Performs a cascade delete on a party.
     *
     * One reason this method can fail is that there were relationships with entities that are not being deleted.
     * If a party is not being deleted like it should, the developer should take a look at the exception thrown
     * by this method to see if any relations were violated. If there were violations, consider adding
     * the entities to the CASCADE array above.
     *
     * XXX Warning, this method is very brittle. It is essentially emulating the ON DELETE CASCADE functionality
     * of well featured databases, but very poorly. As the datamodel evolves, this method would have to be updated.
     */
    public static void deleteCrmParty(String partyId, Delegator delegator) throws GenericEntityException {
        // remove related entities from constant list
        for (int i = 0; i < CRM_PARTY_DELETE_CASCADE.length; i++) {
            String entityName = CRM_PARTY_DELETE_CASCADE[i][0];
            String fieldName = CRM_PARTY_DELETE_CASCADE[i][1];

            Map<String, Object> input = UtilMisc.<String, Object>toMap(fieldName, partyId);
            delegator.removeByAnd(entityName, input);
        }

        // remove communication events
        GenericValue party = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", partyId));
        List<GenericValue> commEvnts = FastList.<GenericValue>newInstance();
        commEvnts.addAll(party.getRelated("ToCommunicationEvent"));
        commEvnts.addAll(party.getRelated("FromCommunicationEvent"));
        for (GenericValue commEvnt : commEvnts) {
            commEvnt.removeRelated("CommunicationEventRole");
            commEvnt.removeRelated("CommunicationEventWorkEff");
            commEvnt.removeRelated("CommEventContentAssoc");
            delegator.removeValue(commEvnt);
        }

        // finally remove party
        delegator.removeValue(party);
    }

    /**
     * Generates a party name in the standard CRMSFA style.  Input is a PartySummaryCRMView or any
     * view entity with fields partyId, groupName, firstName and lastName.
     */
    public static String getCrmsfaPartyName(GenericValue party) {
        if (party == null) {
            return null;
        }
        StringBuffer name = new StringBuffer();
        if (party.get("groupName") != null) {
            name.append(party.get("groupName")).append(" ");
        }
        if (party.get("firstName") != null) {
            name.append(party.get("firstName")).append(" ");
        }
        if (party.get("lastName") != null) {
            name.append(party.get("lastName")).append(" ");
        }
        name.append("(").append(party.get("partyId")).append(")");
        return name.toString();
    }

    /**
     * As above, but does a lookup on PartySummaryCRMView for an input partyId.
     */
    public static String getCrmsfaPartyName(Delegator delegator, String partyId) throws GenericEntityException {
        GenericValue party = delegator.findByPrimaryKey("PartySummaryCRMView", UtilMisc.toMap("partyId", partyId));
        return getCrmsfaPartyName(party);
    }

    /**
     * Retrieve the view url with partyId.
     * @param partyId
     * @param delegator
     * @param externalLoginKey
     * @return view page url
     * @deprecated Use <code>org.opentaps.domain.party.Party.createViewPageURL()</code>
     */
    public static String createViewPageURL(String partyId, Delegator delegator, String externalLoginKey) throws GenericEntityException {
        GenericValue party = delegator.findByPrimaryKey("PartySummaryCRMView", UtilMisc.toMap("partyId", partyId));

        return org.opentaps.common.party.PartyHelper.createViewPageURL(party, CLIENT_PARTY_ROLES, externalLoginKey);
    }

    /**
     * Generates a hyperlink to the correct view profile page for the given party with the standard CRM party using createViewPageURL
     * description string ${groupName} ${firstName} ${lastName} (${partyId}).  Some pages show list of
     * all kinds of parties, including Leads, Accounts, and non-CRM parties.  This method generate a hyperlink to
     * the correct view page, such as viewAccount for Accounts, or partymgr viewprofile for non-CRM parties.
     * @param partyId
     * @param delegator
     * @param externalLoginKey
     * @return view page url
     * @deprecated Use <code>org.opentaps.domain.party.Party.createViewPageLink()</code>
     */
    public static String createViewPageLink(String partyId, Delegator delegator, String externalLoginKey) throws GenericEntityException {
        GenericValue party = delegator.findByPrimaryKeyCache("PartySummaryCRMView", UtilMisc.toMap("partyId", partyId));
        if (party == null) {
            Debug.logError("No PartySummaryCRMView found for partyId [" + partyId + "], cannot create link", MODULE);
            return "";
        }

        // generate the contents of href=""
        String uri = org.opentaps.common.party.PartyHelper.createViewPageURL(party, CLIENT_PARTY_ROLES, externalLoginKey);
        // generate the display name
        StringBuffer name = new StringBuffer(getCrmsfaPartyName(party));

        // put everything together
        StringBuffer buff = new StringBuffer("<a class=\"linktext\" href=\"");
        buff.append(uri).append("\">");
        buff.append(name).append("</a>");
        return buff.toString();
    }

    /**
     * Retrieve the oldest current email address with a PRIMARY_EMAIL purposeTypeId for a party.
     * @param partyId
     * @param delegator
     * @return The email address
     * @deprecated Use <code>org.opentaps.domain.party.Party.getPrimaryEmail()</code>
     */
    @Deprecated public static String getPrimaryEmailForParty(String partyId, Delegator delegator) {
        return org.opentaps.common.party.PartyHelper.getPrimaryEmailForParty(partyId, delegator);
    }

    /**
     * Find active "from" parties such as accounts, contacts, and leads based on PartyFromSummaryByRelationship.
     * These parties can be thought of the main subjects or clients tracked by CRMSFA.
     * Uses the prepareFind service to help build search conditions.
     *
     * The ordering of the fields is controlled by a parameter named activeOrderBy, which may take on
     * the values "lastName" or "companyName".  The default is to order by groupName.
     *
     * @param parameters    A map of fields and condition parameters to be consumed by prepareFind service.
     * @param ec            Optional EntityCondition to filter the results by
     * @param roles         Optional list of CRMSFA roles to limit to.  If roles is null or empty, uses the default CLIENT_PARTY_ROLES.
     * @return EntityListIterator of results or null if the find condition is nothing.
     */
    public static EntityListIterator findActiveClientParties(Delegator delegator, LocalDispatcher dispatcher, Map<String, ?> parameters, List<String> roles, EntityCondition ec) throws GeneralException {
        EntityCondition conditions = getActiveClientPartiesCondition(dispatcher, parameters, roles, ec);
        List<String> orderBy = getActiveClientPartiesOrderBy(parameters);
        return delegator.findListIteratorByCondition("PartyFromSummaryByRelationship", conditions, null, FIND_PARTY_FIELDS,
                orderBy,
                UtilCommon.DISTINCT_READ_OPTIONS);
    }

    private static EntityCondition getActiveClientPartiesCondition(LocalDispatcher dispatcher, Map<String, ?> parameters, List<String> roles, EntityCondition ec) throws GeneralException {
        Map<String, Object> results = dispatcher.runSync("prepareFind", UtilMisc.toMap("entityName", "PartyFromSummaryByRelationship", "inputFields", parameters,
                "filterByDate", "Y", "noConditionFind", "N"));
        if (ServiceUtil.isError(results)) {
            throw new GenericServiceException(ServiceUtil.getErrorMessage(results));
        }
        EntityCondition findConditions = (EntityCondition) results.get("entityConditionList");
        if (findConditions == null) {
            return null;
        }

        List<String> conditionRoles = (roles == null || roles.size() == 0 ? CLIENT_PARTY_ROLES : roles);
        List<EntityCondition> combinedConditions = UtilMisc.<EntityCondition>toList(findConditions, EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.IN, conditionRoles));
        if (ec != null) {
            combinedConditions.add(ec);
        }

        return EntityCondition.makeCondition(combinedConditions, EntityOperator.AND);
    }

    private static List<String> getActiveClientPartiesOrderBy(Map<String, ?> parameters) {
        List<String> orderBy = UtilMisc.toList("groupName", "lastName", "companyName"); // fields to order by (default)

        // see if we're given a different order by
        String requestOrderBy = (String) parameters.get("activeOrderBy");
        if ("lastName".equals(requestOrderBy)) {
            orderBy = UtilMisc.toList("lastName", "groupName", "companyName");
        } else if ("companyName".equals(requestOrderBy)) {
            orderBy = UtilMisc.toList("companyName", "groupName", "lastName");
        }
        return orderBy;
    }

    /** As above, but only consider the search parameters with optional conditions. */
    public static EntityListIterator findActiveClientParties(Delegator delegator, LocalDispatcher dispatcher, Map<String, ?> parameters, EntityCondition ec) throws GeneralException {
        return findActiveClientParties(delegator, dispatcher, parameters, null, ec);
    }

    /** Finds all active accounts, contacts, leads and so on of a given partyId */
    public static EntityListIterator findActiveClientParties(Delegator delegator, LocalDispatcher dispatcher, String partyId) throws GeneralException {
        Map<String, Object> parameters = UtilMisc.<String, Object>toMap("partyIdTo", partyId);
        return findActiveClientParties(delegator, dispatcher, parameters, null, null);
    }

    /** Finds active Accounts for a party. */
    public static EntityListIterator findActiveAccounts(Delegator delegator, LocalDispatcher dispatcher, String partyId) throws GeneralException {
        return findActiveClientParties(delegator, dispatcher, UtilMisc.toMap("partyIdTo", partyId), UtilMisc.toList("ACCOUNT"), null);
    }

    /** Finds active Contacts for a party. */
    public static EntityListIterator findActiveContacts(Delegator delegator, LocalDispatcher dispatcher, String partyId) throws GeneralException {
        return findActiveClientParties(delegator, dispatcher, UtilMisc.toMap("partyIdTo", partyId), UtilMisc.toList("CONTACT"), null);
    }

    /** Finds active Leads for a party. */
    public static EntityListIterator findActiveLeads(Delegator delegator, LocalDispatcher dispatcher, String partyId) throws GeneralException {
        return findActiveClientParties(delegator, dispatcher, UtilMisc.toMap("partyIdTo", partyId), UtilMisc.toList("PROSPECT"), null);
    }

    /**
     * As findActiveClientParties, but returns a ListBuilder for use in pagination.
     */
    public static ListBuilder findActiveClientPartiesListBuilder(LocalDispatcher dispatcher, Map<String, ?> parameters, List<String> roles, EntityCondition ec) throws GeneralException {
        EntityCondition conditions = getActiveClientPartiesCondition(dispatcher, parameters, roles, ec);
        List<String> orderBy = getActiveClientPartiesOrderBy(parameters);
        return new EntityListBuilder("PartyFromSummaryByRelationship", conditions, FIND_PARTY_FIELDS, orderBy);
    }

    public static Map<String, Object> assembleCrmsfaFormMergeContext(Delegator delegator, Locale locale, String partyId, String orderId, String shipGroupSeqId, String shipmentId, TimeZone timeZone) {
        Map<String, Object> templateContext = assembleCrmsfaGenericFormMergeContext(timeZone, locale);
        templateContext.putAll(assembleCrmsfaPartyFormMergeContext(delegator, partyId));
        templateContext.putAll(assembleCrmsfaOrderFormMergeContext(delegator, orderId));
        templateContext.putAll(assembleCrmsfaShipmentFormMergeContext(delegator, orderId, shipGroupSeqId, shipmentId, locale));
        return templateContext;
    }

    public static Map<String, Object> assembleCrmsfaShipmentFormMergeContext(Delegator delegator, String orderId, String shipGroupSeqId, String shipmentId, Locale locale) {
        Map<String, Object> templateContext = new HashMap<String, Object>();

        try {

            // Prefer shipment data if shipmentId is provided
            if (UtilValidate.isNotEmpty(shipmentId)) {

                GenericValue shipment = delegator.findByPrimaryKey("Shipment", UtilMisc.toMap("shipmentId", shipmentId));
                if (UtilValidate.isNotEmpty(shipment)) {

                    GenericValue shipLoc = shipment.getRelatedOne("DestinationPostalAddress");
                    if (UtilValidate.isNotEmpty(shipLoc)) {
                        templateContext.put("orderShippingAddress1", shipLoc.get("address1"));
                        templateContext.put("orderShippingAddress2", shipLoc.get("address2"));
                        templateContext.put("orderShippingCity", shipLoc.get("city"));
                        templateContext.put("orderShippingPostalCode", shipLoc.get("postalCode"));

                        GenericValue stateProvGeo = shipLoc.getRelatedOne("StateProvinceGeo");
                        if (UtilValidate.isNotEmpty(stateProvGeo)) {
                            templateContext.put("orderShippingStateProvince", stateProvGeo.get("geoName"));
                        }
                        GenericValue countryGeo = shipLoc.getRelatedOne("CountryGeo");
                        if (UtilValidate.isNotEmpty(countryGeo)) {
                            templateContext.put("orderShippingCountry", countryGeo.get("geoName"));
                        }
                    }

                    GenericValue phoneNumber = shipment.getRelatedOne("DestinationTelecomNumber");
                    if (UtilValidate.isNotEmpty(phoneNumber)) {

                        String phoneNumberString = UtilValidate.isEmpty(phoneNumber.getString("countryCode")) ? "" : phoneNumber.getString("countryCode") + " ";
                        if (UtilValidate.isNotEmpty(phoneNumber.getString("areaCode"))) {
                            phoneNumberString += phoneNumber.getString("areaCode") + " ";
                        }
                        if (UtilValidate.isNotEmpty(phoneNumber.getString("contactNumber"))) {
                            phoneNumberString += phoneNumber.getString("contactNumber");
                        }
                        templateContext.put("orderShippingPhone", phoneNumberString);
                    }

                    GenericValue statusItem = shipment.getRelatedOne("StatusItem");
                    if (UtilValidate.isNotEmpty(statusItem)) {
                        templateContext.put("shipmentStatus", statusItem.get("description", locale));
                    }
                }

            } else if (UtilValidate.isNotEmpty(orderId)) {

                OrderReadHelper orh = new OrderReadHelper(delegator, orderId);
                GenericValue shipGroup = orh.getOrderItemShipGroup(shipGroupSeqId);
                if (UtilValidate.isEmpty(shipGroup)) {

                    // Default to the first ship group if no shipGroupSeqId is provided
                    List shipGroups = orh.getOrderItemShipGroups();
                    if (UtilValidate.isNotEmpty(shipGroups)) {
                        shipGroup = (GenericValue) shipGroups.get(0);
                    }
                }

                if (UtilValidate.isNotEmpty(shipGroup)) {
                    GenericValue shipLoc = shipGroup.getRelatedOne("PostalAddress");
                    if (UtilValidate.isNotEmpty(shipLoc)) {
                        templateContext.put("orderShippingAddress1", shipLoc.get("address1"));
                        templateContext.put("orderShippingAddress2", shipLoc.get("address2"));
                        templateContext.put("orderShippingCity", shipLoc.get("city"));
                        templateContext.put("orderShippingPostalCode", shipLoc.get("postalCode"));

                        GenericValue stateProvGeo = shipLoc.getRelatedOne("StateProvinceGeo");
                        if (UtilValidate.isNotEmpty(stateProvGeo)) {
                            templateContext.put("orderShippingStateProvince", stateProvGeo.get("geoName"));
                        }
                        GenericValue countryGeo = shipLoc.getRelatedOne("CountryGeo");
                        if (UtilValidate.isNotEmpty(countryGeo)) {
                            templateContext.put("orderShippingCountry", countryGeo.get("geoName"));
                        }
                    }

                    GenericValue phoneNumber = shipGroup.getRelatedOne("TelecomTelecomNumber");
                    if (UtilValidate.isNotEmpty(phoneNumber)) {

                        String phoneNumberString = UtilValidate.isEmpty(phoneNumber.getString("countryCode")) ? "" : phoneNumber.getString("countryCode") + " ";
                        if (UtilValidate.isNotEmpty(phoneNumber.getString("areaCode"))) {
                            phoneNumberString += phoneNumber.getString("areaCode") + " ";
                        }
                        if (UtilValidate.isNotEmpty(phoneNumber.getString("contactNumber"))) {
                            phoneNumberString += phoneNumber.getString("contactNumber");
                        }
                        templateContext.put("orderShippingPhone", phoneNumberString);
                    }

                    // Find any shipments relating to this ship group
                    List<GenericValue> shipments = delegator.findByAnd("Shipment", UtilMisc.toMap("primaryOrderId", orderId, "primaryShipGroupSeqId", shipGroup.getString("shipGroupSeqId")), UtilMisc.toList("createdStamp DESC"));
                    GenericValue shipment = EntityUtil.getFirst(shipments);
                    if (UtilValidate.isNotEmpty(shipment)) {
                        GenericValue statusItem = shipment.getRelatedOne("StatusItem");
                        if (UtilValidate.isNotEmpty(statusItem)) {
                            templateContext.put("shipmentStatus", statusItem.get("description", locale));
                        }
                    }
                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        return templateContext;
    }

    public static Map<String, Object> assembleCrmsfaOrderFormMergeContext(Delegator delegator, String orderId) {
        Map<String, Object> templateContext = new HashMap<String, Object>();
        if (UtilValidate.isNotEmpty(orderId)) {
            try {
                OrderReadHelper orh = new OrderReadHelper(delegator, orderId);
                templateContext.put("orderId", orderId);
                templateContext.put("externalOrderId", orh.getOrderHeader().get("externalId"));
                templateContext.put("orderDate", orh.getOrderHeader().getTimestamp("orderDate"));

                GenericValue billingParty = orh.getBillToParty();
                if (UtilValidate.isNotEmpty(billingParty)) {
                    if ("Person".equalsIgnoreCase(billingParty.getEntityName())) {
                        templateContext.put("orderBillingFirstName", billingParty.get("firstName"));
                        templateContext.put("orderBillingLastName", billingParty.get("lastName"));
                    }
                    templateContext.put("orderPartyId", billingParty.get("partyId"));
                    templateContext.put("orderBillingFullName", org.ofbiz.party.party.PartyHelper.getPartyName(billingParty, false));
                }

                templateContext.put("orderSubtotal", orh.getOrderItemsSubTotal());
                templateContext.put("orderTaxTotal", orh.getTaxTotal());
                templateContext.put("orderShippingTotal", orh.getShippingTotal());
                templateContext.put("orderGrandTotal", orh.getOrderGrandTotal());
                templateContext.put("orderPaymentTotal", orh.getOrderGrandTotal().subtract(UtilOrder.getOrderOpenAmount(orh)));

                GenericValue shippingParty = orh.getShipToParty();
                if (UtilValidate.isNotEmpty(shippingParty)) {
                    if ("Person".equalsIgnoreCase(shippingParty.getEntityName())) {
                        templateContext.put("orderShippingFirstName", shippingParty.get("firstName"));
                        templateContext.put("orderShippingLastName", shippingParty.get("lastName"));
                    } else {
                        templateContext.put("orderShippingCompanyName", shippingParty.get("groupName"));
                    }
                    templateContext.put("orderShippingFullName", org.ofbiz.party.party.PartyHelper.getPartyName(shippingParty, false));
                }

                List<GenericValue> orderItemVals = orh.getOrderItems();
                List<Map<String, Object>> orderItems = new ArrayList<Map<String, Object>>();
                for (GenericValue orderItemVal : orderItemVals) {
                    Map<String, Object> orderItem = orderItemVal.getAllFields();
                    GenericValue product = orderItemVal.getRelatedOne("Product");
                    if (UtilValidate.isEmpty(product)) {
                        continue;
                    }
                    for (String fieldName : (Set<String>) product.keySet()) {
                        orderItem.put(fieldName, product.get(fieldName));
                    }
                    orderItems.add(orderItem);
                }
                templateContext.put("orderItems", orderItems);

            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }
        }
        return templateContext;
    }

    public static Map<String, Object> assembleCrmsfaGenericFormMergeContext(TimeZone timeZone, Locale locale) {
        Map<String, Object> templateContext = new HashMap<String, Object>();

        Calendar now = Calendar.getInstance(timeZone, locale);
        String mmddyyyy = new java.text.SimpleDateFormat("MM/dd/yyyy").format(now.getTime());
        String mmddyyyy2 = new java.text.SimpleDateFormat("MM-dd-yyyy").format(now.getTime());
        String yyyymmdd = new java.text.SimpleDateFormat("yyyy/MM/dd").format(now.getTime());
        String yyyymmdd2 = new java.text.SimpleDateFormat("yyyy-MM-dd").format(now.getTime());
        Integer month = Integer.valueOf(now.get(Calendar.MONTH));
        month++;
        String monthStr = month.toString();
        if (monthStr.length() == 1) {
            monthStr = "0" + monthStr;
        }
        //TODO: oandreyev. Test this code more carefully.
        ArrayList<String> monthNames = (ArrayList<String>)UtilDateTime.getMonthNames(locale);
        String monthName = monthNames.get(month - 1);
        //String monthLabel = null;
        //if (month == 1) {
        //    monthLabel = "CommonJanuary";
        //} else if (month == 2) {
        //    monthLabel = "CommonFebruary";
        //} else if (month == 3) {
        //    monthLabel = "CommonMarch";
        //} else if (month == 4) {
        //    monthLabel = "CommonApril";
        //} else if (month == 5) {
        //    monthLabel = "CommonMay";
        //} else if (month == 6) {
        //    monthLabel = "CommonJune";
        //} else if (month == 7) {
        //    monthLabel = "CommonJuly";
        //} else if (month == 8) {
        //    monthLabel = "CommonAugust";
        //} else if (month == 9) {
        //    monthLabel = "CommonSeptember";
        //} else if (month == 10) {
        //    monthLabel = "CommonOctober";
        //} else if (month == 11) {
        //    monthLabel = "CommonNovember";
        //} else if (month == 12) {
        //    monthLabel = "CommonDecember";
        //}
        //if (UtilValidate.isNotEmpty(monthLabel)) {
        //    monthName = UtilProperties.getMessage("CommonUiLabels", monthLabel, locale);
        //}

        templateContext.put("mmddyyyy", mmddyyyy);
        templateContext.put("mmddyyyy2", mmddyyyy2);
        templateContext.put("yyyymmdd", yyyymmdd);
        templateContext.put("yyyymmdd2", yyyymmdd2);
        templateContext.put("month", monthStr);
        templateContext.put("monthName", monthName);
        templateContext.put("day", new Integer(now.get(Calendar.DAY_OF_MONTH)).toString());
        templateContext.put("year", new Integer(now.get(Calendar.YEAR)).toString());

        return templateContext;
    }

    public static Map<String, Object> assembleCrmsfaPartyFormMergeContext(Delegator delegator, String partyId) {
        Map<String, Object> templateContext = new HashMap<String, Object>();
        if (UtilValidate.isNotEmpty(partyId)) {
            try {
                String email = PartyContactHelper.getElectronicAddressByPurpose(partyId, "EMAIL_ADDRESS", "PRIMARY_EMAIL", delegator);
                if (UtilValidate.isNotEmpty(email)) {
                    templateContext.put("email", email);
                }
                GenericValue address = PartyContactHelper.getPostalAddressValueByPurpose(partyId, "PRIMARY_LOCATION", true, delegator);
                if (UtilValidate.isNotEmpty(address)) {
                    templateContext.put("attnName", address.get("attnName"));
                    templateContext.put("toName", address.get("toName"));
                    templateContext.put("address1", address.get("address1"));
                    templateContext.put("address2", address.get("address2"));
                    templateContext.put("city", address.get("city"));
                    templateContext.put("zip", address.get("postalCode"));

                    GenericValue stateProvGeo = address.getRelatedOne("StateProvinceGeo");
                    if (UtilValidate.isNotEmpty(stateProvGeo)) {
                        templateContext.put("state", stateProvGeo.get("geoName") );
                    }
                    GenericValue countryGeo = address.getRelatedOne("CountryGeo");
                    if (UtilValidate.isNotEmpty(countryGeo)) {
                        templateContext.put("country", countryGeo.get("geoName") );
                    }
                }
                GenericValue party = delegator.findByPrimaryKey("PartySummaryCRMView", UtilMisc.toMap("partyId", partyId));
                Map<String, Object> partyMap = party.getAllFields();
                if (UtilValidate.isNotEmpty(partyMap)) {
                    Iterator<String> pmf = partyMap.keySet().iterator();
                    while (pmf.hasNext()) {
                        String fieldName = pmf.next();
                        Object value = partyMap.get(fieldName);
                        if (UtilValidate.isNotEmpty(value)) {
                            templateContext.put(fieldName, value);
                        }
                    }
                }

                templateContext.put("fullName", org.ofbiz.party.party.PartyHelper.getPartyName(party, false));

            } catch (GenericEntityException ge) {
                Debug.logError(ge, MODULE);
            }
        }
        return templateContext;
    }

    public static Map<String, String> mergePartyWithForm(Delegator delegator, String mergeFormId, String partyId, String orderId, String shipGroupSeqId, String shipmentId, Locale locale, boolean leaveTags, TimeZone timeZone) throws GenericEntityException {
        return mergePartyWithForm(delegator, mergeFormId, partyId, orderId, shipGroupSeqId, shipmentId, locale, leaveTags, timeZone, true);
    }

    public static Map<String, String> mergePartyWithForm(Delegator delegator, String mergeFormId, String partyId, String orderId, String shipGroupSeqId, String shipmentId, Locale locale, boolean leaveTags, TimeZone timeZone, boolean highlightTags) throws GenericEntityException {
        Map<String, Object> mergeContext = PartyHelper.assembleCrmsfaFormMergeContext(delegator, locale, partyId, orderId, shipGroupSeqId, shipmentId, timeZone);
        GenericValue mergeForm = delegator.findByPrimaryKey("MergeForm", UtilMisc.toMap("mergeFormId", mergeFormId));
        if (mergeForm == null) return null;
        String mergeFormText = mergeForm.getString("mergeFormText");
        String mergeFormSubject = mergeForm.getString("subject");
        Writer wr = new StringWriter();
        Map<String, String> output = new HashMap<String, String>();
        try {
            FreemarkerUtil.renderTemplateWithTags("MergeForm", mergeFormText, mergeContext, wr, leaveTags, highlightTags);
            output.put("mergeFormText", wr.toString());
            wr = new StringWriter();
            if (UtilValidate.isNotEmpty(mergeForm.getString("subject"))) {
                FreemarkerUtil.renderTemplateWithTags("MergeForm", mergeFormSubject, mergeContext, wr, leaveTags, false);
                output.put("subject", wr.toString());
            } else {
                output.put("subject", mergeForm.getString("mergeFormName"));
            }
        } catch (TemplateException e) {
            Debug.logError(e, MODULE);
            return null;
        } catch (IOException e) {
            Debug.logError(e, MODULE);
            return null;
        }
        return output;
    }

    /**
     * Retrieve the last deactivation date if the party is currently deactivated.
     * @param partyId
     * @param delegator
     * @return the timestamp of last deactivation, null if the party is not deactivated
     * @throws GenericEntityNotFoundException
     */
    public static Timestamp getDeactivationDate(String partyId, Delegator delegator) throws GenericEntityException {
        // check party current status:
        if (isActive(partyId, delegator)) {
            return null;
        }

        // party is currently deactivated, get the deactivation date
        try {

            List<GenericValue> deactivationDates = delegator.findByAnd("PartyDeactivation", UtilMisc.toMap("partyId", partyId), UtilMisc.toList("-deactivationTimestamp"));
            if (UtilValidate.isNotEmpty(deactivationDates)) {
                return (Timestamp) deactivationDates.get(0).get("deactivationTimestamp");
            } else {
                Debug.logWarning("The party [" + partyId + "] status is disabled but there is no registered deactivation date.", MODULE);
            }

        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        return null;
    }

    /**
     * Check if the party has been deactivated.
     * @param partyId
     * @param delegator
     * @return is active
     * @throws GenericEntityNotFoundException
     */
    public static boolean isActive(String partyId, Delegator delegator) throws GenericEntityException {
        GenericValue party = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", partyId));
        if (party == null) {
            throw new GenericEntityNotFoundException("No Party found with ID: " + partyId);
        }
        return (!"PARTY_DISABLED".equals(party.getString("statusId")));
    }

    /** Checks if the given party with role is unassigned. */
    public static boolean isUnassigned(Delegator delegator, String partyId, String roleTypeId) throws GenericEntityException {
        List<GenericValue> activeRelationships = EntityUtil.filterByDate(delegator.findByAnd("PartyRelationship", UtilMisc.toMap("partyIdFrom", partyId, "roleTypeIdFrom", roleTypeId,
                "partyRelationshipTypeId", "ASSIGNED_TO")));
        return activeRelationships.size() == 0;
    }

    /** Checks if the given party with role is assigned to the user login. */
    public static boolean isAssignedToUserLogin(String partyId, String roleTypeId, GenericValue userLogin) throws GenericEntityException {
        Delegator delegator = userLogin.getDelegator();
        String roleTypeIdTo = getFirstValidTeamMemberRoleTypeId(userLogin.getString("partyId"), delegator);
        if (roleTypeIdTo == null) {
            return false;
        }
        List<GenericValue> activeRelationships = EntityUtil.filterByDate(delegator.findByAnd("PartyRelationship", UtilMisc.toMap("partyIdFrom", partyId, "roleTypeIdFrom", roleTypeId,
                "partyIdTo", userLogin.get("partyId"), "roleTypeIdTo", roleTypeIdTo,
                "partyRelationshipTypeId", "ASSIGNED_TO")));
        return activeRelationships.size() > 0;
    }

    /**
     * Find the active ASSIGNED_TO party relationships with given From and To party IDs, and the role type ID of From party such as 'CONTACT'.
     *
     * @param delegator a Delegator instance
     * @param partyIdFrom a String object that represents the From party ID
     * @param roleTypeIdFrom a String object that represents the role type ID of From party
     * @param partyIdTo a String object that represents the To party ID
     * @return a List of GenericValue objects
     */
    public static List<GenericValue> findActiveAssignedToPartyRelationships(final Delegator delegator, final String partyIdFrom, final String roleTypeIdFrom, final String partyIdTo) throws GenericEntityException {
        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                EntityCondition.makeCondition("partyIdFrom", partyIdFrom),
                EntityCondition.makeCondition("roleTypeIdFrom", roleTypeIdFrom),
                EntityCondition.makeCondition("partyIdTo", partyIdTo),
                EntityCondition.makeCondition("partyRelationshipTypeId", "ASSIGNED_TO"),
                EntityUtil.getFilterByDateExpr());
        return delegator.findByCondition("PartyRelationship", conditions, null, null);
    }

}
