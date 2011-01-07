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
package com.opensourcestrategies.crmsfa.party;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;

import com.opensourcestrategies.crmsfa.security.CrmsfaSecurity;

/**
 * Services common to CRM parties such as accounts/contacts/leads. The service documentation is in services_party.xml.
 *
 * @author     <a href="mailto:leon@opensourcestrategies.com">Leon Torres</a>
 */

public final class PartyServices {

    private PartyServices() { }

    private static final String MODULE = PartyServices.class.getName();

    /**
     * Identifies parties which should be merged based on identical names and postal addresses (alphanumeric portions of address1, postalCode, countryGeoId)
     * or identical email addresses, and creates records in PartyMergeCandidates for later merging using the crmsfa.mergeCrmParties service.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> findCrmPartiesForMerge(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);

        try {

            Map fullMerge = new HashMap();

            // Find parties that already have an entry in PartyMergeCandidates
            List<GenericValue> existingMergeCandidates = delegator.findAll("PartyMergeCandidates");
            List<String> existingMergeCandidateFromParties = EntityUtil.getFieldListFromEntityList(existingMergeCandidates, "partyIdFrom", true);

            // Find parties with similar postal addresses
            List<EntityExpr> postalAddressMergeCandidateConditions = new ArrayList<EntityExpr>();
            postalAddressMergeCandidateConditions.add(EntityCondition.makeCondition("address1", EntityOperator.NOT_EQUAL, null));
            postalAddressMergeCandidateConditions.add(EntityCondition.makeCondition("postalCode", EntityOperator.NOT_EQUAL, null));
            postalAddressMergeCandidateConditions.add(EntityCondition.makeCondition("countryGeoId", EntityOperator.NOT_EQUAL, null));
            postalAddressMergeCandidateConditions.add(EntityCondition.makeCondition("contactMechId", EntityOperator.NOT_EQUAL, null));

            TransactionUtil.begin();
            EntityListIterator partyAndPostalAddresses = delegator.findListIteratorByCondition("PartyAndPostalAddress", EntityCondition.makeCondition(postalAddressMergeCandidateConditions, EntityOperator.AND), null, null);
            TransactionUtil.commit();

            // Iterate through the partyAndPostalAddress records, constructing a four-level-deep map with countryGeoId as the first level, postalCode as the second,
            //  the numeric portions of address1 as the third, and partyId->GenericValue as the fourth.
            Map postalAddressMerge = new TreeMap();
            GenericValue partyAndPostalAddress = null;
            while ((partyAndPostalAddress = (GenericValue) partyAndPostalAddresses.next()) != null) {

                // We're comparing only the alphanumeric portions of address1 - ignoring punctuation, symbols and spaces
                String address = partyAndPostalAddress.getString("address1").toUpperCase().replaceAll("[^0-9A-Z]", "");
                if (UtilValidate.isNotEmpty(address)) {
                    String partyId = partyAndPostalAddress.getString("partyId");

                    // Ignore any parties that are already in some state of merge candidacy
                    if (existingMergeCandidateFromParties.contains(partyId)) {
                        continue;
                    }

                    String countryGeoId = partyAndPostalAddress.getString("countryGeoId").toUpperCase();
                    String postalCode = partyAndPostalAddress.getString("postalCode").toUpperCase();
                    Map countryMap = (Map) postalAddressMerge.get(countryGeoId);
                    if (countryMap == null) {
                        countryMap = new TreeMap();
                        postalAddressMerge.put(countryGeoId, countryMap);
                    }
                    Map postalCodeMap = (Map) countryMap.get(postalCode);
                    if (postalCodeMap == null) {
                        postalCodeMap = new TreeMap();
                        countryMap.put(postalCode, postalCodeMap);
                    }
                    Map partyIds = (Map) postalCodeMap.get(address);
                    if (partyIds == null) {
                        partyIds = new TreeMap();
                        postalCodeMap.put(address, partyIds);
                    }
                    partyIds.put(partyId, partyAndPostalAddress);
                }
            }
            partyAndPostalAddresses.close();

            // Iterate through the resolved postal address map, checking which of the groups of similar addresses have more than one party associated with them.
            Iterator<String> pit = postalAddressMerge.keySet().iterator();
            while (pit.hasNext()) {
                String countryGeoId = pit.next();
                Map postalCodes = (Map) postalAddressMerge.get(countryGeoId);
                Iterator<String> pcit = postalCodes.keySet().iterator();
                while (pcit.hasNext()) {
                    String postalCode = pcit.next();
                    Map addresses = (Map) postalCodes.get(postalCode);
                    Iterator<String> ait = addresses.keySet().iterator();
                    while (ait.hasNext()) {
                        String address = ait.next();
                        TreeMap parties = (TreeMap) addresses.get(address);
                        if (parties.size() > 1) {

                            // If so, take the first party and use it as the map key in fullMerge
                            String toPartyId = (String) parties.firstKey();
                            GenericValue toContactMech = (GenericValue) parties.get(toPartyId);
                            parties.remove(parties.firstKey());

                            // The fullMerge value is the rest of the stuff
                            fullMerge.put(toPartyId, UtilMisc.toMap("toContactMech", toContactMech, "partiesToMerge", parties));

                            // Add all the parties which are going to be merged with the toParty into the existingMergeCandidateFromParties list,
                            //  so that we don't try to merge them again when examining email addresses
                            existingMergeCandidateFromParties.addAll(parties.keySet());
                        }
                    }
                }
            }

            // Find parties with similar email addresses
            List<EntityExpr> emailAddressMergeCandidateConditions = new ArrayList<EntityExpr>();
            emailAddressMergeCandidateConditions.add(EntityCondition.makeCondition("contactMechTypeId", EntityOperator.EQUALS, "EMAIL_ADDRESS"));
            emailAddressMergeCandidateConditions.add(EntityCondition.makeCondition("infoString", EntityOperator.NOT_EQUAL, null));

            TransactionUtil.begin();
            EntityListIterator partyAndEmailAddresses = delegator.findListIteratorByCondition("PartyAndContactMech", EntityCondition.makeCondition(emailAddressMergeCandidateConditions, EntityOperator.AND), null, null);
            TransactionUtil.commit();

            // Iterate through the partyAndContactMech records, constructing an address->(map of partyId->genericValue)
            Map emailAddressMerge = new TreeMap();
            GenericValue partyAndEmailAddress = null;
            while ((partyAndEmailAddress = (GenericValue) partyAndEmailAddresses.next()) != null) {
                String address = partyAndEmailAddress.getString("infoString").toUpperCase().replaceAll(" ", "");
                if (UtilValidate.isNotEmpty(address)) {
                    String partyId = partyAndEmailAddress.getString("partyId");

                    // Ignore any parties that are already in some state of merge candidacy
                    if (existingMergeCandidateFromParties.contains(partyId)) {
                        continue;
                    }

                    Map partyIds = (Map) emailAddressMerge.get(address);
                    if (partyIds == null) {
                        partyIds = new TreeMap();
                        emailAddressMerge.put(address, partyIds);
                    }
                    partyIds.put(partyId, partyAndEmailAddress);
                }
            }
            partyAndEmailAddresses.close();

            // Iterate through the resolved email address map, checking which of the groups of similar addresses have more than one party associated with them.
            Iterator<String> eit = emailAddressMerge.keySet().iterator();
            while (eit.hasNext()) {
                String address = eit.next();
                TreeMap parties = (TreeMap) emailAddressMerge.get(address);
                if (parties.size() > 1) {
                    String toPartyId = (String) parties.firstKey();
                    GenericValue toContactMech = (GenericValue) parties.get(toPartyId);
                    parties.remove(parties.firstKey());
                    fullMerge.put(toPartyId, UtilMisc.toMap("toContactMech", toContactMech, "partiesToMerge", parties));
                }
            }

            // Iterate through the full set of groups of parties with similar contact info
            Iterator<String> fit = fullMerge.keySet().iterator();
            while (fit.hasNext()) {

                // Use the key as the toPartyId
                String toPartyId = fit.next();

                // Get the name of the toParty
                String toPartyName = org.ofbiz.party.party.PartyHelper.getPartyName(delegator, toPartyId, false);
                String toPartyNameDisplay = toPartyName + " (" + toPartyId + ")";
                Map partiesMergeMap = (Map) fullMerge.get(toPartyId);
                GenericValue toContactMech = (GenericValue) partiesMergeMap.get("toContactMech");
                String toPartyTypeId = toContactMech.getString("partyTypeId");

                // Format the postal/email address as a string
                String toContactMechString = "";
                if ("POSTAL_ADDRESS".equals(toContactMech.getString("contactMechTypeId"))) {
                    toContactMechString += toContactMech.getString("address1");
                    if (UtilValidate.isNotEmpty(toContactMech.get("address2"))) {
                        toContactMechString += " " + toContactMech.get("address2");
                    }
                    if (UtilValidate.isNotEmpty(toContactMech.get("city"))) {
                        toContactMechString += " " + toContactMech.get("city");
                    }
                    if (UtilValidate.isNotEmpty(toContactMech.get("stateProvinceGeoId"))) {
                        toContactMechString += ", " + toContactMech.get("stateProvinceGeoId");
                    }
                    toContactMechString += " " + toContactMech.getString("postalCode");
                    toContactMechString += " " + toContactMech.getString("countryGeoId");
                } else if ("EMAIL_ADDRESS".equals(toContactMech.getString("contactMechTypeId"))) {
                    toContactMechString = toContactMech.getString("infoString");
                }

                // Get a map of all the other parties which will be merged with the toParty
                Map partiesToMerge = (Map) partiesMergeMap.get("partiesToMerge");

                // Iterate through them
                Iterator<String> pmit = partiesToMerge.keySet().iterator();
                while (pmit.hasNext()) {

                    // Get the fromPartyId and name, and format the address
                    String fromPartyId = pmit.next();
                    String fromPartyName = org.ofbiz.party.party.PartyHelper.getPartyName(delegator, fromPartyId, false);
                    String fromPartyNameDisplay = fromPartyName + " (" + fromPartyId + ")";
                    GenericValue fromContactMech = (GenericValue) partiesToMerge.get(fromPartyId);
                    String fromPartyTypeId = fromContactMech.getString("partyTypeId");

                    // Ignore parties of different types (eg. PERSON vs. PARTY_GROUP)
                    if (!toPartyTypeId.equalsIgnoreCase(fromPartyTypeId)) {
                        String skipRationale = UtilMessage.expandLabel("crmsfa.findCrmPartiesForMerge_skipDueToType", UtilMisc.toMap("toPartyName", toPartyNameDisplay, "fromPartyName", fromPartyNameDisplay), locale);
                        GenericValue existingPartyMergeCandidate = delegator.findByPrimaryKey("PartyMergeCandidates", UtilMisc.toMap("partyIdTo", toPartyId, "partyIdFrom", fromPartyId));
                        if (existingPartyMergeCandidate != null) {
                            existingPartyMergeCandidate.set("doNotMerge", "Y");
                            existingPartyMergeCandidate.set("comments", skipRationale);
                            existingPartyMergeCandidate.store();
                        }
                        Debug.logInfo(skipRationale, MODULE);
                        continue;
                    }

                    // Ignore parties with different names (considering only alphanumeric characters)
                    if (!toPartyName.toUpperCase().replaceAll("[^0-9A-Z]", "").equals(fromPartyName.toUpperCase().replaceAll("[^0-9A-Z]", ""))) {
                        String skipRationale = UtilMessage.expandLabel("crmsfa.findCrmPartiesForMerge_skipDueToName", UtilMisc.toMap("toPartyName", toPartyNameDisplay, "fromPartyName", fromPartyNameDisplay), locale);
                        GenericValue existingPartyMergeCandidate = delegator.findByPrimaryKey("PartyMergeCandidates", UtilMisc.toMap("partyIdTo", toPartyId, "partyIdFrom", fromPartyId));
                        if (existingPartyMergeCandidate != null) {
                            existingPartyMergeCandidate.set("doNotMerge", "Y");
                            existingPartyMergeCandidate.set("comments", skipRationale);
                            existingPartyMergeCandidate.store();
                        }
                        Debug.logInfo(skipRationale, MODULE);
                        continue;
                    }

                    String fromContactMechString = "";
                    String mergeRationale = "";
                    if ("POSTAL_ADDRESS".equals(fromContactMech.getString("contactMechTypeId"))) {
                        fromContactMechString += fromContactMech.getString("address1");
                        if (UtilValidate.isNotEmpty(fromContactMech.get("address2"))) {
                            fromContactMechString += " " + fromContactMech.get("address2");
                        }
                        if (UtilValidate.isNotEmpty(fromContactMech.get("city"))) {
                            fromContactMechString += " " + fromContactMech.get("city");
                        }
                        if (UtilValidate.isNotEmpty(fromContactMech.get("stateProvinceGeoId"))) {
                            fromContactMechString += ", " + fromContactMech.get("stateProvinceGeoId");
                        }
                        fromContactMechString += " " + fromContactMech.get("postalCode");
                        fromContactMechString += " " + fromContactMech.get("countryGeoId");
                        mergeRationale = UtilMessage.expandLabel("crmsfa.findCrmPartiesForMerge_mergeRationalePostal", UtilMisc.toMap("toPartyName", toPartyNameDisplay, "toContactMechString", toContactMechString, "fromPartyName", fromPartyNameDisplay, "fromContactMechString", fromContactMechString), locale);
                    } else if ("EMAIL_ADDRESS".equals(fromContactMech.getString("contactMechTypeId"))) {
                        fromContactMechString = fromContactMech.getString("infoString");
                        mergeRationale = UtilMessage.expandLabel("crmsfa.findCrmPartiesForMerge_mergeRationaleEmail", UtilMisc.toMap("toPartyName", toPartyNameDisplay, "toContactMechString", toContactMechString, "fromPartyName", fromPartyNameDisplay, "fromContactMechString", fromContactMechString), locale);
                    }
                    String hasOrderRoles = (delegator.findCountByAnd("OrderRole", UtilMisc.toMap("partyId", fromPartyId)) > 0) ? "Y" : "N";

                    // Make a record in PartyMergeCandidates to hold the candidate pair
                    delegator.create("PartyMergeCandidates", UtilMisc.toMap("partyIdTo", toPartyId, "partyIdFrom", fromPartyId, "hasOrderRoles", hasOrderRoles, "mergeRationale", mergeRationale));
                }
            }
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorFindPartiesForMergeFail", locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Merge all PartyMergeCandidate pairs with a null processedTimestamp and doNotMerge != "Y" and updates the record on success. Each pair is transactional inside the service.
     * If validate is set to "N", then common.validateMergeParties will not be run.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    public static Map<String, Object> autoMergeParties(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String validate = (String) context.get("validate");

        int successfulMerges = 0;  // counter of how many merges were done

        try {

            // Find parties that have an entry in PartyMergeCandidates without a processedTimestamp or a doNotMerge == Y
            EntityConditionList<EntityExpr> orConditions = EntityCondition.makeCondition(UtilMisc.toList(EntityCondition.makeCondition("doNotMerge", EntityOperator.NOT_EQUAL, "Y"), EntityCondition.makeCondition("doNotMerge", EntityOperator.EQUALS, null)), EntityOperator.OR);
            EntityConditionList<EntityCondition> andConditions = EntityCondition.makeCondition(UtilMisc.toList(orConditions, EntityCondition.makeCondition("processedTimestamp", EntityOperator.EQUALS, null)), EntityOperator.AND);
            List<GenericValue> existingMergeCandidates = delegator.findByCondition("PartyMergeCandidates", andConditions, null, null);

            if (existingMergeCandidates != null) {
                Iterator<GenericValue> emcit = existingMergeCandidates.iterator();
                while (emcit.hasNext()) {
                    GenericValue partyMergeCandidate = (GenericValue) emcit.next();

                    // Just for safety, a double-check
                    if ("Y".equalsIgnoreCase(partyMergeCandidate.getString("doNotMerge")) || partyMergeCandidate.get("processedTimestamp") != null) {
                        continue;
                    }

                    String partyIdFrom = partyMergeCandidate.getString("partyIdFrom");
                    String partyIdTo = partyMergeCandidate.getString("partyIdTo");

                    try {

                        // Call the party merge service inside a transaction
                        TransactionUtil.begin();

                        Map<String, Object> mergeResult = dispatcher.runSync("common.mergeParties", UtilMisc.toMap("partyIdFrom", partyIdFrom, "partyIdTo", partyIdTo, "validate", validate, "userLogin", userLogin));
                        if (ServiceUtil.isError(mergeResult)) {
                            TransactionUtil.rollback();
                            UtilMessage.logServiceError("crmsfa.autoMergePartiesError", UtilMisc.toMap("partyIdFrom", partyIdFrom, "partyIdTo", partyIdTo), locale, MODULE);
                            continue;
                        }

                        UtilMessage.logServiceInfo("crmsfa.autoMergePartiesSuccess", UtilMisc.toMap("partyIdFrom", partyIdFrom, "partyIdTo", partyIdTo), locale, MODULE);

                        // Update the PartyMergeCandidate record
                        partyMergeCandidate.set("processedTimestamp", UtilDateTime.nowTimestamp());
                        delegator.store(partyMergeCandidate);
                        successfulMerges++;

                        TransactionUtil.commit();

                    } catch (GenericEntityException e) {
                        TransactionUtil.rollback();
                        UtilMessage.logServiceError(e, "crmsfa.autoMergePartiesError", UtilMisc.toMap("partyIdFrom", partyIdFrom, "partyIdTo", partyIdTo), locale, MODULE);
                    } catch (Exception e) {
                        TransactionUtil.rollback();
                        UtilMessage.logServiceError(e, "crmsfa.autoMergePartiesError", UtilMisc.toMap("partyIdFrom", partyIdFrom, "partyIdTo", partyIdTo), locale, MODULE);
                    }
                }
            }
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorAutoMergePartiesFail", locale, MODULE);
        }

        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put("successfulMerges", new Integer(successfulMerges));
        return results;
    }

    /**
     * Sets the given view preference to the given value. If no value is given or the value is the empty string,
     * then the preference will be erased.  The value may be either a viewPrefEnumId or a viewPrefString.  The service will
     * automatically detect which type it is.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    public static Map<String, Object> setViewPreference(DispatchContext dctx, Map<String, Object> context) {
        Debug.logInfo("Use of crmsfa.setViewPreference is deprecated.  Please use opentasp.setviewPreference instead.", MODULE);
        return org.opentaps.common.party.PartyServices.setViewPreference(dctx, context);
    }

    /**
     * Change a Party password, this is used by CRM users to reset a customer password wuthout having to
     *  know the original password.
     * The user must have CRMSFA_PASS_UPDATE permission.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    public static Map<String, Object> updatePartyPassword(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String partyId = (String) context.get("partyId");
        String userLoginId = (String) context.get("userLoginId");
        String newPassword = (String) context.get("newpassword");
        String confirmPassword = (String) context.get("confirmpassword");
        String passwordHint = (String) context.get("passwordhint");

        // permission to update password
        if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_PASS", "_UPDATE", userLogin, partyId)) {
            return UtilMessage.createServiceError("CrmErrorPasswordUpdatePermission", locale, context);
        }

        // that is the only check we need here, is newPassword is empty, then updatePassword will only update the Hint
        // checks that newPassword is not empty
        if (UtilValidate.isEmpty(newPassword)) {
            return UtilMessage.createServiceError("CrmErrorNewPasswordRequired", locale, context);
        }

        try {
            // Get the PartyAndUserLogin
            GenericValue partyAndUserLogin = delegator.findByPrimaryKey("PartyAndUserLogin", UtilMisc.toMap("partyId", partyId, "userLoginId", userLoginId));
            if (UtilValidate.isEmpty(partyAndUserLogin)) {
                return UtilMessage.createAndLogServiceError("CrmErrorPartyNotFound", context, locale, MODULE);
            }

            // Call ofbiz service updatePassword as System (to bypass the service security checks)
            Map<String, Object> srcResults = dispatcher.runSync("updatePassword", UtilMisc.toMap("userLogin", UtilCommon.getSystemUserLogin(delegator), "userLoginId", userLoginId, "newPassword", newPassword, "newPasswordVerify", confirmPassword, "passwordHint", passwordHint));

            // Checks on the password are done in the updatePassword service, so we return the error message to the user
            // (this include checking password min length, is equal to verify, no password in hint, etc...)
            if (ServiceUtil.isError(srcResults) || ServiceUtil.isFailure(srcResults)) {
                return srcResults;
            }
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorSetPreferenceFail", locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorSetPreferenceFail", locale, MODULE);
        }
        return UtilMessage.createServiceSuccess("CrmPasswordChangeSuccess", locale, context);
    }

    /**
     * Adds a CRM client role to parties given the BILL_TO_CUSTOMER role in OFBiz, and assigns responsibility to
     *  the AutoResponsibleParty.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    public static Map<String, Object> autoAssignParty(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String partyId = (String) context.get("partyId");

        // From the CRMSFA seed data
        String autoResponsiblePartyId = "AutoResponsibleParty";

        try {

            // Get the party
            GenericValue party = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", partyId));
            if (UtilValidate.isEmpty(party)) {
                return UtilMessage.createAndLogServiceError("CrmErrorPartyNotFound", context, locale, MODULE);
            }

            // Make sure the party isn't an internal organization
            if (org.opentaps.common.party.PartyHelper.isInternalOrganization(partyId, delegator)) {
                UtilMessage.logServiceInfo("OpentapsError_IgnoringInternalOrg", UtilMisc.toMap("partyId", partyId), locale, MODULE);
                return ServiceUtil.returnSuccess();
            }

            // Check to see if the party already has a CRM client role
            String crmRoleTypeId = PartyHelper.getFirstValidRoleTypeId(partyId, PartyHelper.CLIENT_PARTY_ROLES, delegator);

            // Create the CRM role unless it has it already
            if (UtilValidate.isEmpty(crmRoleTypeId)) {
                crmRoleTypeId = "PERSON".equals(party.getString("partyTypeId")) ? "CONTACT" : "ACCOUNT";
                Map<String, Object> createPartyRoleResult = dispatcher.runSync("createPartyRole", UtilMisc.toMap("partyId", partyId, "roleTypeId", crmRoleTypeId, "userLogin", userLogin));
                if (ServiceUtil.isError(createPartyRoleResult)) {
                    return createPartyRoleResult;
                }
            }

            // Since ofbiz 09.04: now they use party roles CONTACT or ACCOUNT and it's possible
            // a party already has right crmsfa relationships. We must establish relationship to autoresponsible
            // party only if the party has no ASSIGNET_TO (for person) or RESPONSIBLE_FOR (for account)
            // relationship already.
            List<EntityCondition> relationshipConditions = null;
            if ("PERSON".equals(party.getString("partyTypeId"))) {
                relationshipConditions = UtilMisc.toList(
                        EntityCondition.makeCondition("partyIdFrom", partyId),
                        EntityCondition.makeCondition("roleTypeIdFrom", "CONTACT"),
                        EntityCondition.makeCondition("partyRelationshipTypeId", "ASSIGNED_TO"),
                        EntityUtil.getFilterByDateExpr()
                );
            } else {
                relationshipConditions = UtilMisc.toList(
                        EntityCondition.makeCondition("partyIdFrom", partyId),
                        EntityCondition.makeCondition("roleTypeIdFrom", "ACCOUNT"),
                        EntityCondition.makeCondition("partyRelationshipTypeId", "RESPONSIBLE_FOR"),
                        EntityUtil.getFilterByDateExpr()
                );
            }

            List<GenericValue> relationships = delegator.findByAnd("PartyRelationship", relationshipConditions);

            // Assign responsibility for the party to the AutoResponsibleParty from the seed data
            if (UtilValidate.isEmpty(relationships)) {
                Map<String, Object> reassignServiceContext = UtilMisc.toMap("newPartyId", autoResponsiblePartyId, "userLogin", userLogin);

                String subjectPartyIdKey = "PERSON".equals(party.getString("partyTypeId")) ? "contactPartyId" : "accountPartyId";
                reassignServiceContext.put(subjectPartyIdKey, partyId);

                String serviceName = "PERSON".equals(party.getString("partyTypeId")) ? "crmsfa.reassignContactResponsibleParty" : "crmsfa.reassignAccountResponsibleParty";
                Map<String, Object> reassignServiceResult = dispatcher.runSync(serviceName, reassignServiceContext);
                if (ServiceUtil.isError(reassignServiceResult)) {
                    return reassignServiceResult;
                }
            }

        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorSetPreferenceFail", locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorSetPreferenceFail", locale, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Adds role and relationship information so that OFBiz parties can be used in CRMSFA.
     * This service will create a CONTACT role for Persons and ACCOUNT role for PartyGroups.
     * All converted parties will be assigned to the user login, who must have the role
     * ACCOUNT_MANAGER and be in the SALES_MANAGER security group.  This allows the sales
     * manager to reassign the converted parties to specific reps from within CRMSFA.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> convertOfbizParties(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = UtilCommon.getLocale(context);
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String assignToPartyId = userLogin.getString("partyId");
        try {
            // ensure that the assignee is an ACCOUNT_MANAGER with SALES_MANAGER security group
            GenericValue managerRole = delegator.findByPrimaryKey("PartyRole", UtilMisc.toMap("partyId", assignToPartyId, "roleTypeId", "ACCOUNT_MANAGER"));
            if (managerRole == null) {
                return UtilMessage.createServiceError("OpentapsError_PermissionDenied", locale);
            }
            List<GenericValue> securityGroups = delegator.findByAnd("UserLoginSecurityGroup", UtilMisc.toMap("userLoginId", userLogin.get("userLoginId"), "groupId", "SALES_MANAGER"));
            securityGroups = EntityUtil.filterByDate(securityGroups);
            if (securityGroups.size() == 0) {
                return UtilMessage.createServiceError("OpentapsError_PermissionDenied", locale);
            }

            EntityCondition conditions = EntityCondition.makeCondition(UtilMisc.toList(
                    EntityCondition.makeCondition("orderTypeId", EntityOperator.EQUALS, "SALES_ORDER"),
                    EntityCondition.makeCondition("roleTypeId", EntityOperator.EQUALS, "BILL_TO_CUSTOMER")
            ), EntityOperator.AND);
            EntityListIterator orderRoles = delegator.findListIteratorByCondition("OrderHeaderAndRoles", conditions, null, null);
            GenericValue orderRole;
            while ((orderRole = (GenericValue) orderRoles.next()) != null) {
                GenericValue party = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", orderRole.get("partyId")));

                String roleTypeId = null;
                String partyTypeId = party.getString("partyTypeId");
                if ("PERSON".equals(partyTypeId)) {
                    roleTypeId = "CONTACT";
                } else if ("PARTY_GROUP".equals(partyTypeId)) {
                    roleTypeId = "ACCOUNT";
                } else {
                    continue;
                }

                String partyId = party.getString("partyId");
                Map input = UtilMisc.toMap("partyId", partyId, "roleTypeId", roleTypeId);
                GenericValue role = delegator.findByPrimaryKey("PartyRole", input);
                if (role != null) {
                    // see if there is anyone responsible for this party, and if not make the assignToPartyId responsible
                    conditions = EntityCondition.makeCondition(UtilMisc.toList(
                            EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, partyId),
                            EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.EQUALS, roleTypeId),
                            EntityCondition.makeCondition("securityGroupId", EntityOperator.EQUALS, roleTypeId + "_OWNER"),
                            EntityCondition.makeCondition("partyRelationshipTypeId", EntityOperator.EQUALS, "RESPONSIBLE_FOR"),
                            EntityUtil.getFilterByDateExpr()
                    ), EntityOperator.AND);
                    List<GenericValue> relations = delegator.findByCondition("PartyRelationship", conditions, null, null);
                    if (relations.size() == 0) {
                        PartyHelper.createNewPartyToRelationship(assignToPartyId, partyId, roleTypeId, "RESPONSIBLE_FOR",
                                roleTypeId + "_OWNER", PartyHelper.TEAM_MEMBER_ROLES, true, userLogin, delegator, dispatcher);
                    }
                } else {
                    input.put("userLogin", userLogin);
                    Map<String, Object> results = dispatcher.runSync("createPartyRole", input);
                    if (ServiceUtil.isError(results)) {
                        return results;
                    }

                    PartyHelper.createNewPartyToRelationship(assignToPartyId, partyId, roleTypeId, "RESPONSIBLE_FOR",
                            roleTypeId + "_OWNER", PartyHelper.TEAM_MEMBER_ROLES, true, userLogin, delegator, dispatcher);
                }

                // Update PartySupplementalData
                org.opentaps.common.party.PartyHelper.updatePartySupplementalData(delegator, partyId);
            }
            orderRoles.close();
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Creates an ASSIGN_TO relationship between an active party with a role to the assignToPartyId.
     * If the assignToPartyId is not given, the userLogin party will be used.  This operation will fail if the
     * party is already assigned to someone else or if the assignToPartyId does not have CRMSFA_${roleTypeId}_VIEW permission.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> assignParty(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        Locale locale = UtilCommon.getLocale(context);
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String partyId = (String) context.get("partyId");
        String roleTypeId = (String) context.get("roleTypeId");
        String assignToPartyId = (String) context.get("assignToPartyId");
        if (UtilValidate.isEmpty(assignToPartyId)) {
            assignToPartyId = userLogin.getString("partyId");
        }
        try {
            if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_" + roleTypeId, "_VIEW", userLogin, partyId)) {
                return UtilMessage.createServiceError("OpentapsError_PermissionDenied", locale);
            }
            if (!PartyHelper.isActive(partyId, delegator)) {
                return UtilMessage.createServiceError("OpentapsError_PartyDeactivated", locale, UtilMisc.toMap("partyId", partyId));
            }

            // expire any active ASSIGNED_TO relationship from the contact party to the user login
            // this shouldn't be really necessary, but just in case
            List activeAssignedToRelationships = PartyHelper.findActiveAssignedToPartyRelationships(
                    delegator,
                    partyId,
                    roleTypeId,
                    assignToPartyId
            );
            PartyHelper.expirePartyRelationships(activeAssignedToRelationships, UtilDateTime.nowTimestamp(), dispatcher, userLogin);

            // now create the new ASSIGNED_TO relationship by calling createPartyRelationship service
            String roleTypeIdTo = org.opentaps.common.party.PartyHelper.getFirstValidRoleTypeId(assignToPartyId, PartyHelper.TEAM_MEMBER_ROLES, delegator);
            Map input = UtilMisc.toMap(
                    "partyIdTo", assignToPartyId,
                    "roleTypeIdTo", roleTypeIdTo,
                    "partyIdFrom", partyId,
                    "roleTypeIdFrom", roleTypeId
            );
            input.put("partyRelationshipTypeId", "ASSIGNED_TO");
            input.put("securityGroupId", null);
            input.put("fromDate", UtilDateTime.nowTimestamp());
            input.put("userLogin", userLogin);
            dispatcher.runSync("createPartyRelationship", input);
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Expires any ASSIGN_TO relationship between an active party with a role and the unassignPartyId.
     * If the unassignPartyId is not given, the userLogin party will be used.  This operation will fail
     * if the unassignPartyId does not have CRMSFA_${roleTypeId}_VIEW permission.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    public static Map<String, Object> unassignParty(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        Locale locale = UtilCommon.getLocale(context);
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String partyId = (String) context.get("partyId");
        String roleTypeId = (String) context.get("roleTypeId");
        String unassignPartyId = (String) context.get("unassignPartyId");
        if (UtilValidate.isEmpty(unassignPartyId)) {
            unassignPartyId = userLogin.getString("partyId");
        }
        try {
            if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_" + roleTypeId, "_VIEW", userLogin, partyId)) {
                return UtilMessage.createServiceError("OpentapsError_PermissionDenied", locale);
            }
            if (!PartyHelper.isActive(partyId, delegator)) {
                return UtilMessage.createServiceError("OpentapsError_PartyDeactivated", locale, UtilMisc.toMap("partyId", partyId));
            }
            List<GenericValue> relations = delegator.findByAnd("PartyRelationship", UtilMisc.toMap("partyIdTo", unassignPartyId,
                    "partyIdFrom", partyId, "roleTypeIdFrom", roleTypeId, "partyRelationshipTypeId", "ASSIGNED_TO"));
            PartyHelper.expirePartyRelationships(relations, UtilDateTime.nowTimestamp(), dispatcher, userLogin);
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Creates PartyTaxAuthInfo, replaces the ofbiz simple method service.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    public static Map<String, Object> createPartyTaxAuthInfo(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        Locale locale = UtilCommon.getLocale(context);
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String partyId = (String) context.get("partyId");
        Timestamp fromDate = (Timestamp) context.get("fromDate");
        Timestamp thruDate = (Timestamp) context.get("thruDate");

        if (fromDate == null) {
            Calendar now = Calendar.getInstance();
            now.set(Calendar.SECOND, 0);
            now.set(Calendar.MILLISECOND, 0);
            fromDate = new Timestamp(now.getTimeInMillis());
            context.put("fromDate", fromDate);
        }

        try {
            if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_PAY", "_UPDATE", userLogin, partyId)) {
                return UtilMessage.createServiceError("OpentapsError_PermissionDenied", locale);
            }
            if (!PartyHelper.isActive(partyId, delegator)) {
                return UtilMessage.createServiceError("OpentapsError_PartyDeactivated", locale, UtilMisc.toMap("partyId", partyId));
            }

            if (thruDate != null && thruDate.before(fromDate)) {
                return UtilMessage.createAndLogServiceError("OpentapsError_ThruDateAfterFromDate", locale, MODULE);
            }

            GenericValue partyTaxAuthInfo = delegator.makeValue("PartyTaxAuthInfo");
            partyTaxAuthInfo.setPKFields(context);
            partyTaxAuthInfo.setNonPKFields(context);
            partyTaxAuthInfo.create();

        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Updates PartyTaxAuthInfo, replaces the ofbiz simple method service.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    public static Map<String, Object> updatePartyTaxAuthInfo(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        Locale locale = UtilCommon.getLocale(context);
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String partyId = (String) context.get("partyId");
        String taxAuthGeoId = (String) context.get("taxAuthGeoId");
        String taxAuthPartyId = (String) context.get("taxAuthPartyId");
        Timestamp fromDate = (Timestamp) context.get("fromDate");
        Timestamp thruDate = (Timestamp) context.get("thruDate");

        try {
            if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_PAY", "_UPDATE", userLogin, partyId)) {
                return UtilMessage.createServiceError("OpentapsError_PermissionDenied", locale);
            }
            if (!PartyHelper.isActive(partyId, delegator)) {
                return UtilMessage.createServiceError("OpentapsError_PartyDeactivated", locale, UtilMisc.toMap("partyId", partyId));
            }

            Map<String, Object> pk = UtilMisc.toMap("partyId", partyId, "taxAuthPartyId", taxAuthPartyId, "taxAuthGeoId", taxAuthGeoId, "fromDate", fromDate);
            GenericValue partyTaxAuthInfo = delegator.findByPrimaryKey("PartyTaxAuthInfo", pk);
            if (partyTaxAuthInfo == null) {
                return UtilMessage.createAndLogServiceError("Could not find the PartyTaxAuthInfo with PK [" + pk + "]", MODULE);
            }

            if (thruDate != null && thruDate.before(fromDate)) {
                return UtilMessage.createAndLogServiceError("OpentapsError_ThruDateAfterFromDate", locale, MODULE);
            }

            partyTaxAuthInfo.setNonPKFields(context);
            partyTaxAuthInfo.store();

        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }
}
