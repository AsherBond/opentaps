/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 *
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
 */
package com.opensourcestrategies.crmsfa.party;

import com.opensourcestrategies.crmsfa.security.CrmsfaSecurity;
import org.ofbiz.base.util.*;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelField;
import org.ofbiz.entity.model.ModelRelation;
import org.ofbiz.entity.model.ModelViewEntity;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.common.util.UtilCommon;

import java.util.*;
import java.sql.Timestamp;

/**
 * Services common to CRM parties such as accounts/contacts/leads. The service documentation is in services_party.xml.
 *
 * @author     <a href="mailto:leon@opensourcestrategies.com">Leon Torres</a>
 */

public final class PartyServices {

    private PartyServices() { }

    private static final String MODULE = PartyServices.class.getName();

    /**
     * Merging function for two unique GenericValues.
     * @param entityName the name of the <code>GenericValue</code> entity
     * @param fromKeys <code>Map</code> representing the primary key of the entity to merge from
     * @param toKeys <code>Map</code> representing the primary key of the entity to merge to
     * @param delegator a <code>GenericDelegator</code> value
     * @exception GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    private static void mergeTwoValues(String entityName, Map fromKeys, Map toKeys, GenericDelegator delegator) throws GenericEntityException {
        GenericValue from = delegator.findByPrimaryKey(entityName, fromKeys);
        GenericValue to = delegator.findByPrimaryKey(entityName, toKeys);
        if (from == null || to == null) {
            return;
        }
        from.setNonPKFields(to.getAllFields());
        to.setNonPKFields(from.getAllFields());
        to.store();
    }

    /**
     * Ensures two parties can be merged. Returns service error if they cannot. A merge requires CRMSFA_${type}_UPDATE where
     * type is the roleTypeId of the party, such as ACCOUNT, CONTACT, or LEAD. Also, the input must be two different partyIds
     * with the same roleTypeId.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map validateMergeCrmParties(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        String partyIdFrom = (String) context.get("partyIdFrom");
        String partyIdTo = (String) context.get("partyIdTo");
        try {
            // ensure that merging parties are the same type (ACCOUNT, CONTACT, PROSPECT)
            String fromPartyType = PartyHelper.getFirstValidInternalPartyRoleTypeId(partyIdFrom, delegator);
            String toPartyType = PartyHelper.getFirstValidInternalPartyRoleTypeId(partyIdTo, delegator);
                if ((fromPartyType == null) || !fromPartyType.equals(toPartyType)) {
                    return UtilMessage.createAndLogServiceError("Cannot merge party [" + partyIdFrom + "] of type [" + fromPartyType + "] with party [" + partyIdTo + "] of type [" + toPartyType + "] because they are not the same type.", "CrmErrorMergePartiesFail", locale, MODULE);
                }
            if (partyIdFrom.equals(partyIdTo)) {
                return UtilMessage.createAndLogServiceError("Cannot merge party [" + partyIdFrom + "] to itself!", "CrmErrorMergeParties", locale, MODULE);
            }

            // convert ACCOUNT/CONTACT/PROSPECT to ACCOUNT/CONTACT/LEAD
            String partyTypeCrm = (fromPartyType.equals("PROSPECT") ? "LEAD" : fromPartyType);

            // make sure userLogin has CRMSFA_${partyTypeCrm}_UPDATE permission for both parties TODO: and delete, check security config
            if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_" + partyTypeCrm, "_UPDATE", userLogin, partyIdFrom)
                    || !CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_" + partyTypeCrm, "_UPDATE", userLogin, partyIdTo)) {
                return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied" + ": CRMSFA_" + partyTypeCrm + "_UPDATE", locale, MODULE);
            }
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorMergePartiesFail", locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Merge two parties. Checks crmsfa.validateMergeCrmParties as a precaution if the validate parameter is not set to N. The From party will be deleted after the merge.
     * TODO: avoid merging parties with particular statuses. implement with an array.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map mergeCrmParties(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        String partyIdFrom = (String) context.get("partyIdFrom");
        String partyIdTo = (String) context.get("partyIdTo");
        String validate = (String) context.get("validate");

        try {

            Map serviceResults = null;

            if (!"N".equalsIgnoreCase(validate)) {
                // validate again
                serviceResults = dispatcher.runSync("crmsfa.validateMergeCrmParties",
                                                    UtilMisc.toMap("userLogin", userLogin, "partyIdFrom", partyIdFrom, "partyIdTo", partyIdTo));
                if (ServiceUtil.isError(serviceResults)) {
                    return serviceResults;
                }
            }

            // merge the party objects
            mergeTwoValues("PartySupplementalData", UtilMisc.toMap("partyId", partyIdFrom), UtilMisc.toMap("partyId", partyIdTo), delegator);
            mergeTwoValues("Person", UtilMisc.toMap("partyId", partyIdFrom), UtilMisc.toMap("partyId", partyIdTo), delegator);
            mergeTwoValues("PartyGroup", UtilMisc.toMap("partyId", partyIdFrom), UtilMisc.toMap("partyId", partyIdTo), delegator);
            mergeTwoValues("Party", UtilMisc.toMap("partyId", partyIdFrom), UtilMisc.toMap("partyId", partyIdTo), delegator);

            List toRemove = new ArrayList();

            // Get a list of entities related to the Party entity, in descending order by relation
            List relatedEntities = getRelatedEntities("Party", delegator);

            // Go through the related entities in forward order - this makes sure that parent records are created before child records
            Iterator reit = relatedEntities.iterator();
            while (reit.hasNext()) {
                ModelEntity modelEntity = (ModelEntity) reit.next();

                // Examine each field of the entity
                Iterator mefit = modelEntity.getFieldsIterator();
                while (mefit.hasNext()) {
                    ModelField modelField = (ModelField) mefit.next();
                    if (modelField.getName().matches(".*[pP]artyId.*")) {

                        // If the name of the field has something to do with a partyId, get all the existing records from that entity which have the
                        //  partyIdFrom in that particular field
                        List existingRecords = delegator.findByAnd(modelEntity.getEntityName(), UtilMisc.toMap(modelField.getName(), partyIdFrom));
                        if (existingRecords.size() > 0) {
                            Iterator eit = existingRecords.iterator();
                            while (eit.hasNext()) {
                                GenericValue existingRecord = (GenericValue) eit.next();
                                if (modelField.getIsPk()) {

                                    // If the partyId field is part of a primary key, create a new record with the partyIdTo in place of the partyIdFrom
                                    GenericValue newRecord = delegator.makeValue(modelEntity.getEntityName(), existingRecord.getAllFields());
                                    newRecord.set(modelField.getName(), partyIdTo);

                                    // Create the new record if a record with the same primary key doesn't already exist
                                    if (delegator.findByPrimaryKey(newRecord.getPrimaryKey()) == null) {
                                        newRecord.create();
                                    }

                                    // Add the old record to the list of records to remove
                                    toRemove.add(existingRecord);
                                } else {

                                    // If the partyId field is not party of a primary key, simply update the field with the new value and store it
                                    existingRecord.set(modelField.getName(), partyIdTo);
                                    existingRecord.store();
                                }
                            }
                        }
                    }
                }
            }

            // Go through the list of records to remove in REVERSE order! Since they're still in descending order of relation to the Party
            //  entity, reversing makes sure that child records are removed before parent records, all the way back to the original Party record
            ListIterator rit = toRemove.listIterator(toRemove.size());
            while (rit.hasPrevious()) {
                GenericValue existingRecord = (GenericValue) rit.previous();
                existingRecord.remove();
            }

        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorMergePartiesFail", locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorMergePartiesFail", locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Identifies parties which should be merged based on identical names and postal addresses (alphanumeric portions of address1, postalCode, countryGeoId)
     * or identical email addresses, and creates records in PartyMergeCandidates for later merging using the crmsfa.mergeCrmParties service.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map findCrmPartiesForMerge(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");

        try {

            Map fullMerge = new HashMap();

            // Find parties that already have an entry in PartyMergeCandidates
            List existingMergeCandidates = delegator.findAll("PartyMergeCandidates");
            List existingMergeCandidateFromParties = EntityUtil.getFieldListFromEntityList(existingMergeCandidates, "partyIdFrom", true);

            // Find parties with similar postal addresses
            List postalAddressMergeCandidateConditions = new ArrayList();
            postalAddressMergeCandidateConditions.add(new EntityExpr("address1", EntityOperator.NOT_EQUAL, null));
            postalAddressMergeCandidateConditions.add(new EntityExpr("postalCode", EntityOperator.NOT_EQUAL, null));
            postalAddressMergeCandidateConditions.add(new EntityExpr("countryGeoId", EntityOperator.NOT_EQUAL, null));
            postalAddressMergeCandidateConditions.add(new EntityExpr("contactMechId", EntityOperator.NOT_EQUAL, null));

            TransactionUtil.begin();
            EntityListIterator partyAndPostalAddresses = delegator.findListIteratorByCondition("PartyAndPostalAddress", new EntityConditionList(postalAddressMergeCandidateConditions, EntityOperator.AND), null, null);
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
            Iterator pit = postalAddressMerge.keySet().iterator();
            while (pit.hasNext()) {
                String countryGeoId = (String) pit.next();
                Map postalCodes = (Map) postalAddressMerge.get(countryGeoId);
                Iterator pcit = postalCodes.keySet().iterator();
                while (pcit.hasNext()) {
                    String postalCode = (String) pcit.next();
                    Map addresses = (Map) postalCodes.get(postalCode);
                    Iterator ait = addresses.keySet().iterator();
                    while (ait.hasNext()) {
                        String address = (String) ait.next();
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
            List emailAddressMergeCandidateConditions = new ArrayList();
            emailAddressMergeCandidateConditions.add(new EntityExpr("contactMechTypeId", EntityOperator.EQUALS, "EMAIL_ADDRESS"));
            emailAddressMergeCandidateConditions.add(new EntityExpr("infoString", EntityOperator.NOT_EQUAL, null));

            TransactionUtil.begin();
            EntityListIterator partyAndEmailAddresses = delegator.findListIteratorByCondition("PartyAndContactMech", new EntityConditionList(emailAddressMergeCandidateConditions, EntityOperator.AND), null, null);
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
            Iterator eit = emailAddressMerge.keySet().iterator();
            while (eit.hasNext()) {
                String address = (String) eit.next();
                TreeMap parties = (TreeMap) emailAddressMerge.get(address);
                if (parties.size() > 1) {
                    String toPartyId = (String) parties.firstKey();
                    GenericValue toContactMech = (GenericValue) parties.get(toPartyId);
                    parties.remove(parties.firstKey());
                    fullMerge.put(toPartyId, UtilMisc.toMap("toContactMech", toContactMech, "partiesToMerge", parties));
                }
            }

            // Iterate through the full set of groups of parties with similar contact info
            Iterator fit = fullMerge.keySet().iterator();
            while (fit.hasNext()) {

                // Use the key as the toPartyId
                String toPartyId = (String) fit.next();

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
                Iterator pmit = partiesToMerge.keySet().iterator();
                while (pmit.hasNext()) {

                    // Get the fromPartyId and name, and format the address
                    String fromPartyId = (String) pmit.next();
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
     * If validate is set to "N", then crmsfa.validateMergeCrmParties will not be run.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map autoMergeParties(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        String validate = (String) context.get("validate");

        int successfulMerges = 0;  // counter of how many merges were done

        try {

            // Find parties that have an entry in PartyMergeCandidates without a processedTimestamp or a doNotMerge == Y
            EntityConditionList orConditions = new EntityConditionList(UtilMisc.toList(new EntityExpr("doNotMerge", EntityOperator.NOT_EQUAL, "Y"), new EntityExpr("doNotMerge", EntityOperator.EQUALS, null)), EntityOperator.OR);
            EntityConditionList andConditions = new EntityConditionList(UtilMisc.toList(orConditions, new EntityExpr("processedTimestamp", EntityOperator.EQUALS, null)), EntityOperator.AND);
            List existingMergeCandidates = delegator.findByCondition("PartyMergeCandidates", andConditions, null, null);

            if (existingMergeCandidates != null) {
                Iterator emcit = existingMergeCandidates.iterator();
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

                        Map mergeResult = dispatcher.runSync("crmsfa.mergeCrmParties", UtilMisc.toMap("partyIdFrom", partyIdFrom, "partyIdTo", partyIdTo, "validate", validate, "userLogin", userLogin));
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

        Map results = ServiceUtil.returnSuccess();
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
    @SuppressWarnings("unchecked")
    public static Map setViewPreference(DispatchContext dctx, Map context) {
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
    @SuppressWarnings("unchecked")
    public static Map updatePartyPassword(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

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
            Map srcResults = dispatcher.runSync("updatePassword", UtilMisc.toMap("userLogin", UtilCommon.getSystemUserLogin(delegator), "userLoginId", userLoginId, "newPassword", newPassword, "newPasswordVerify", confirmPassword, "passwordHint", passwordHint));

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
    @SuppressWarnings("unchecked")
    public static Map autoAssignParty(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

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

            // Check to see if the party has the BILL_TO_CUSTOMER role
            GenericValue billToRole = delegator.findByPrimaryKey("PartyRole", UtilMisc.toMap("partyId", partyId, "roleTypeId", "BILL_TO_CUSTOMER"));


            // Check to see if the party already has a CRM client role
            String crmRoleTypeId = PartyHelper.getFirstValidRoleTypeId(partyId, PartyHelper.CLIENT_PARTY_ROLES, delegator);

            // If the party already has a role, there's nothing to do
            if (UtilValidate.isNotEmpty(crmRoleTypeId)) {
                UtilMessage.logServiceInfo("crmsfa.autoAssignParty_CrmRoleExists", UtilMisc.toMap("partyId", partyId, "crmRoleTypeId", crmRoleTypeId), locale, MODULE);
                return ServiceUtil.returnSuccess();
            }

            // Create the CRM role
            crmRoleTypeId = "PERSON".equals(party.getString("partyTypeId")) ? "CONTACT" : "ACCOUNT";
            Map createPartyRoleResult = dispatcher.runSync("createPartyRole", UtilMisc.toMap("partyId", partyId, "roleTypeId", crmRoleTypeId, "userLogin", userLogin));
            if (ServiceUtil.isError(createPartyRoleResult)) {
                return createPartyRoleResult;
            }

            // Assign responsibility for the party to the AutoResponsibleParty from the seed data
            Map reassignServiceContext = UtilMisc.toMap("newPartyId", autoResponsiblePartyId, "userLogin", userLogin);

            String subjectPartyIdKey = "PERSON".equals(party.getString("partyTypeId")) ? "contactPartyId" : "accountPartyId";
            reassignServiceContext.put(subjectPartyIdKey, partyId);

            String serviceName = "PERSON".equals(party.getString("partyTypeId")) ? "crmsfa.reassignContactResponsibleParty" : "crmsfa.reassignAccountResponsibleParty";
            Map reassignServiceResult = dispatcher.runSync(serviceName, reassignServiceContext);
            if (ServiceUtil.isError(reassignServiceResult)) {
                return reassignServiceResult;
            }

        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorSetPreferenceFail", locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorSetPreferenceFail", locale, MODULE);
        }

        return ServiceUtil.returnSuccess();

    }

    @SuppressWarnings("unchecked")
    private static List getRelatedEntities(String parentEntityName, GenericDelegator delegator) {
        ModelEntity parentEntity = delegator.getModelEntity(parentEntityName);

        // Start the recursion
        return getRelatedEntities(new ArrayList(), parentEntity, delegator);
    }

    /**
     * Recursive method to map relations from a single entity.
     * @param relatedEntities List of related ModelEntity objects in descending order of relation from the parent entity
     * @param parentEntity Root ModelEntity for deriving relations
     * @param delegator GenericDelegator
     * @return List of ModelEntity objects in descending order of relation from the original parent entity
     */
    @SuppressWarnings("unchecked")
    private static List getRelatedEntities(List relatedEntities, ModelEntity parentEntity, GenericDelegator delegator) {

        // Do nothing if the parent entity has already been mapped
        if (relatedEntities.contains(parentEntity)) {
            return relatedEntities;
        }

        relatedEntities.add(parentEntity);
        Iterator reit = parentEntity.getRelationsIterator();

        // Recurse for each relation from the parent entity that doesn't refer to a view-entity
        while (reit.hasNext()) {
            ModelRelation relation = (ModelRelation) reit.next();
            String relatedEntityName = relation.getRelEntityName();
            ModelEntity relatedEntity = delegator.getModelEntity(relatedEntityName);
            if (!(relatedEntity instanceof ModelViewEntity)) {
                relatedEntities = getRelatedEntities(relatedEntities, relatedEntity, delegator);
            }
        }
        return relatedEntities;
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
    public static Map convertOfbizParties(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
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

            EntityCondition conditions = new EntityConditionList(UtilMisc.toList(
                    new EntityExpr("orderTypeId", EntityOperator.EQUALS, "SALES_ORDER"),
                    new EntityExpr("roleTypeId", EntityOperator.EQUALS, "BILL_TO_CUSTOMER")
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
                    conditions = new EntityConditionList(UtilMisc.toList(
                            new EntityExpr("partyIdFrom", EntityOperator.EQUALS, partyId),
                            new EntityExpr("roleTypeIdFrom", EntityOperator.EQUALS, roleTypeId),
                            new EntityExpr("securityGroupId", EntityOperator.EQUALS, roleTypeId + "_OWNER"),
                            new EntityExpr("partyRelationshipTypeId", EntityOperator.EQUALS, "RESPONSIBLE_FOR"),
                            EntityUtil.getFilterByDateExpr()
                    ), EntityOperator.AND);
                    List relations = delegator.findByCondition("PartyRelationship", conditions, null, null);
                    if (relations.size() == 0) {
                        PartyHelper.createNewPartyToRelationship(assignToPartyId, partyId, roleTypeId, "RESPONSIBLE_FOR",
                            roleTypeId + "_OWNER", PartyHelper.TEAM_MEMBER_ROLES, true, userLogin, delegator, dispatcher);
                    }
                } else {
                    input.put("userLogin", userLogin);
                    Map results = dispatcher.runSync("createPartyRole", input);
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
    public static Map assignParty(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        Locale locale = (Locale) context.get("locale");
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
    @SuppressWarnings("unchecked")
    public static Map unassignParty(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        Locale locale = (Locale) context.get("locale");
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
            List relations = delegator.findByAnd("PartyRelationship", UtilMisc.toMap("partyIdTo", unassignPartyId,
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
    @SuppressWarnings("unchecked")
    public static Map createPartyTaxAuthInfo(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        Locale locale = (Locale) context.get("locale");
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
    @SuppressWarnings("unchecked")
    public static Map updatePartyTaxAuthInfo(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        Locale locale = (Locale) context.get("locale");
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

            Map pk = UtilMisc.toMap("partyId", partyId, "taxAuthPartyId", taxAuthPartyId, "taxAuthGeoId", taxAuthGeoId, "fromDate", fromDate);
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
