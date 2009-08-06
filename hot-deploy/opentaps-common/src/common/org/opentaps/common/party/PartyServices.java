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
/*
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
/* Portions of this file came from Apache OFBIZ.  This file has been modified by Open Source Strategies, Inc. */

package org.opentaps.common.party;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javolution.util.FastList;

import org.apache.commons.validator.GenericValidator;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilMessage;

/**
 * General party services for opentaps applications.
 */
public class PartyServices {

    public static String module = PartyServices.class.getName();

    public static Map createViewPreference(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        String userLoginId = (String) context.get("userLoginId");
        if (! userLogin.get("userLoginId").equals(userLoginId)) {
            if (! security.hasEntityPermission("PARTYMGR", "_VPREF_UPDATE", userLogin)) {
                return UtilMessage.createServiceError("PartyCreateUserLoginViewPrefPermissionError", locale);
            }
        }
        try {
            GenericValue pref = delegator.makeValue("ViewPreference");
            pref.setPKFields(context);
            pref.setNonPKFields(context);
            pref.create();
        } catch (GenericEntityException e) {
            UtilMessage.createAndLogServiceError(e, module);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map updateViewPreference(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        String userLoginId = (String) context.get("userLoginId");
        if (! userLogin.get("userLoginId").equals(userLoginId)) {
            if (! security.hasEntityPermission("PARTYMGR", "_VPREF_UPDATE", userLogin)) {
                return UtilMessage.createServiceError("PartyCreateUserLoginViewPrefPermissionError", locale);
            }
        }
        try {
            Map input = UtilMisc.toMap("userLoginId", userLoginId, "viewPrefTypeId", context.get("viewPrefTypeId"));
            GenericValue pref = delegator.findByPrimaryKey("ViewPreference", input);
            if (pref == null) {
                return UtilMessage.createServiceError("OpentapsError_ViewPrefNotFound", locale, input);
            }
            pref.setNonPKFields(context);
            pref.store();
        } catch (GenericEntityException e) {
            UtilMessage.createAndLogServiceError(e, module);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map setViewPreference(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        String viewPrefTypeId = (String) context.get("viewPrefTypeId");
        String value = (String) context.get("viewPrefValue");
        if (value != null && value.trim().length() == 0) value = null;
        try {
            // if preference already exists, we'll be doing an update, otherwise a create
            GenericValue pref = ViewPrefWorker.getViewPreferenceValue(userLogin, viewPrefTypeId);

            // prepare the preference
            Map input = UtilMisc.toMap("userLogin", userLogin, "userLoginId", userLogin.get("userLoginId"), "viewPrefTypeId", viewPrefTypeId);

            // decide whether value is an enum or a string -- start with default to string
            input.put("viewPrefValueTypeId", "VPREF_VALTYPE_STRING");
            input.put("viewPrefString", value);

            // and if we find an enumeration, reset the fields to store an enum
            if (value != null) {
                GenericValue enumeration = delegator.findByPrimaryKeyCache("Enumeration", UtilMisc.toMap("enumId", value));
                if (enumeration != null) {
                    input.put("viewPrefValueTypeId", "VPREF_VALTYPE_ENUM");
                    input.put("viewPrefString", null);
                    input.put("viewPrefEnumId", value);
                }
            }
            return dispatcher.runSync(pref != null ? "crmsfa.updateViewPreference" : "crmsfa.createViewPreference", input);

        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorSetPreferenceFail", locale, module);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorSetPreferenceFail", locale, module);
        }
    }

    public static Map checkReceiveEmailOwnerUniqueness(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        Locale locale = (Locale)context.get("locale");

        String partyId = (String)context.get("partyId");
        String contactMechId = (String)context.get("contactMechId");
        String contactMechPurposeTypeId = (String)context.get("contactMechPurposeTypeId");

        if (!"RECEIVE_EMAIL_OWNER".equals(contactMechPurposeTypeId)) return ServiceUtil.returnSuccess();

        try {

            GenericValue contactMech = delegator.findByPrimaryKeyCache("ContactMech", UtilMisc.toMap("contactMechId", contactMechId));
            String contactEmail = null;
            if (contactMech != null) contactEmail = contactMech.getString("infoString");

            List<EntityCondition> conditions = new ArrayList<EntityCondition>();
            conditions.add(new EntityExpr("contactMechPurposeTypeId", EntityOperator.EQUALS, "RECEIVE_EMAIL_OWNER"));
            conditions.add(new EntityExpr("contactMechTypeId", EntityOperator.EQUALS, "EMAIL_ADDRESS"));
            Timestamp now = UtilDateTime.nowTimestamp();
            conditions.add(EntityUtil.getFilterByDateExpr(now, "contactFromDate", "contactThruDate"));
            conditions.add(EntityUtil.getFilterByDateExpr(now, "purposeFromDate", "purposeThruDate"));

            List<GenericValue> contactMechs = delegator.findByCondition("PartyContactWithPurpose", new EntityConditionList(conditions, EntityOperator.AND), null, null);

            for (GenericValue current  : contactMechs) {
                String currentEmail = current.getString("infoString");
                if (currentEmail.equalsIgnoreCase(contactEmail) && !contactMechId.equals(current.getString("contactMechId")))
                    return UtilMessage.createAndLogServiceError("OpentapsError_EmailOwnerExist", locale, module);;
            }

        } catch (GenericEntityException gee) {
            return UtilMessage.createAndLogServiceError(gee, locale, module);
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Expires any existing PartyClassifications for a partyId where partyClassificationGroupId is any partyClassificationGroupId related to partyClassificationTypeId,
     *  and creates a new PartyClassification
     */
    public static Map expireAndCreatePartyClassification(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String partyId = (String) context.get("partyId");
        String partyClassificationGroupId = (String) context.get("partyClassificationGroupId");
        String partyClassificationTypeId = (String) context.get("partyClassificationTypeId");

        Timestamp now = UtilDateTime.nowTimestamp();
        try {
            List partyClassGroups = delegator.findByAnd("PartyClassificationGroup", UtilMisc.toMap("partyClassificationTypeId", partyClassificationTypeId));
            List<String> partyClassGroupIds = EntityUtil.getFieldListFromEntityList(partyClassGroups, "partyClassificationGroupId", true);

            List cond = UtilMisc.toList(new EntityExpr("partyId", EntityOperator.EQUALS, partyId),
                    new EntityExpr("partyClassificationGroupId", EntityOperator.IN, partyClassGroupIds));
            List<GenericValue> partyClassifications = delegator.findByCondition("PartyClassification", new EntityConditionList(cond, EntityOperator.AND), null, null);
            partyClassifications = EntityUtil.filterByDate(partyClassifications, now);
            for (GenericValue partyClassification : partyClassifications) {
                Map serviceResult = dispatcher.runSync("updatePartyClassification", UtilMisc.toMap("partyId", partyId, "partyClassificationGroupId", partyClassification.get("partyClassificationGroupId"), "fromDate", partyClassification.get("fromDate"), "thruDate", now, "userLogin", userLogin));
                if (ServiceUtil.isError(serviceResult)) return serviceResult;
            }
            Map serviceResult = dispatcher.runSync("createPartyClassification", UtilMisc.toMap("partyId", partyId, "partyClassificationGroupId", partyClassificationGroupId, "userLogin", userLogin));
            if (ServiceUtil.isError(serviceResult)) return serviceResult;
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, module);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, module);
        }

        return ServiceUtil.returnSuccess();
    }


    /*
     * opentaps.sendInternalMessage to send a message.
     */
    public static Map sendInternalMessage(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        // Get the sender and verify he is able to send im
        String partyIdFrom = (String) context.get("partyIdFrom");
        if (UtilValidate.isEmpty(partyIdFrom))
            partyIdFrom = userLogin.getString("partyId");

        if (!PartyHelper.isInternalMessageSender(partyIdFrom, delegator))
            return UtilMessage.createServiceError("OpentapsError_InternalMessageNoSenderPermission", locale, context);

        // Get the recipients
        List<String> partyIdTo = (List<String>) context.get("partyIdTo");
        if (partyIdTo == null) partyIdTo = FastList.newInstance();

        String partyIdToAsString = (String)context.get("partyIdToAsString");
        String[] toAddrs = partyIdToAsString.split("[,;]");
        if (toAddrs.length > 0)
            for (String addr : toAddrs) {
                if (GenericValidator.isBlankOrNull(addr)) continue;
                String addresseeId = (addr.indexOf("<") != -1 && addr.indexOf(">") != -1) ? addr.substring(addr.indexOf("<") + 1, addr.indexOf(">")) : addr; 
                partyIdTo.add(addresseeId.trim());
            }

        // Get subject of the message
        String subject = (String) context.get("subject");

        // Get the message body
        String message = (String) context.get("message");

        if (UtilValidate.isEmpty(message))
            return UtilMessage.createServiceError("OpentapsError_InternalMessageBodyRequired", locale);

        // Send im to every body
        Iterator parties = partyIdTo.iterator();
        while (parties.hasNext()) {
            // verify the recipient is able to receive internal messages
            String party = (String) parties.next();
            if (!PartyHelper.isInternalMessageRecipient(party, delegator)) {
                Debug.logError(UtilMessage.expandLabel("OpentapsError_InternalMessageNoRecipientPermission", locale, UtilMisc.toMap("partyId", party)), module);
                continue;
            }
            Map<String, Object> input = UtilMisc.<String, Object>toMap("userLogin", userLogin);
            input.put("communicationEventTypeId", "INTERNAL_MESSAGE");
            input.put("statusId", "COM_ENTERED");
            input.put("partyIdFrom", partyIdFrom);
            input.put("partyIdTo", party);
            input.put("entryDate", UtilDateTime.nowTimestamp());
            input.put("content", message);
            if (UtilValidate.isNotEmpty(subject)) {
                input.put("subject", subject);
            }

            try {
                dispatcher.runAsync("createCommunicationEvent", input);
            } catch (GenericServiceException ex) {
                return UtilMessage.createAndLogServiceFailure("OpentapsError_InternalMessageNoRecipientPermission", UtilMisc.toMap("partyId", party), locale, module);
            }
        }

        return ServiceUtil.returnSuccess();
    }

    /*
     * opentaps.receiveInternalMessage to receive a message.
     */
    public static Map receiveInternalMessage(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        // get filter options
        List<String> partyIdsFrom = (List<String>) context.get("partyIdFrom");
        String partyIdTo = (String) context.get("partyIdTo");
        if (UtilValidate.isEmpty(partyIdTo))
            return UtilMessage.createServiceError("OpentapsError_InternalMessageNoRecipients", locale);
        List<String> messageContains = (List<String>) context.get("messageContains");
        List<String> subjectContains = (List<String>) context.get("subjectContains");
        Boolean isRead = (Boolean) context.get("isRead");
        Timestamp dateFrom = (Timestamp) context.get("dateFrom");
        Timestamp dateTo = (Timestamp) context.get("dateTo");

        List<EntityCondition> conditions = new ArrayList<EntityCondition>();

        if (UtilValidate.isNotEmpty(partyIdsFrom))
            for (String partyId : partyIdsFrom)
                conditions.add(new EntityExpr("partyIdFrom", EntityOperator.EQUALS, partyId));

        conditions.add(new EntityExpr("partyIdTo", EntityOperator.EQUALS, partyIdTo));

        if (UtilValidate.isNotEmpty(messageContains))
            for (String incWord : messageContains)
                conditions.add(new EntityExpr("content", EntityOperator.LIKE, "%" + incWord + "%"));

        if (UtilValidate.isNotEmpty(subjectContains))
            for (String excWord : subjectContains)
                conditions.add(new EntityExpr("subject", EntityOperator.LIKE, "%" + excWord + "%"));

        if (UtilValidate.isNotEmpty(isRead))
            conditions.add(new EntityExpr("statusId", EntityOperator.EQUALS, isRead.booleanValue() ? "COM_COMPLETE" : "COM_ENTERED"));

        if (UtilValidate.isNotEmpty(dateFrom))
            conditions.add(new EntityExpr("entryDate", EntityOperator.GREATER_THAN_EQUAL_TO, dateFrom ));

        if (UtilValidate.isNotEmpty(dateTo))
            conditions.add(new EntityExpr("entryDate", EntityOperator.LESS_THAN_EQUAL_TO, dateTo ));

        List<String> orderBy = Arrays.asList("entryDate");
        List<String> selectList = Arrays.asList("communicationEventId", "communicationEventTypeId", "statusId", "roleTypeIdFrom", "roleTypeIdTo", "partyIdFrom", "partyIdTo", "entryDate", "subject", "content");

        Map<String, Object> result = ServiceUtil.returnSuccess();

        // get messages
        try {

            List<GenericValue> messages = delegator.findByCondition("CommunicationEvent", new EntityConditionList(conditions, EntityOperator.AND), selectList, orderBy);
            result.put("messages", messages);

        } catch (GenericEntityException ex) {
            return UtilMessage.createAndLogServiceError(ex, module);
        }

        return result;
    }

    public static Map removeParty(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        if (! security.hasEntityPermission("PARTYMGR", "_ADMIN", userLogin)) {
            return ServiceUtil.returnError("You do not have permission to remove a product.  CATALOG_ADMIN permission is required.");
        }

        String partyId = (String) context.get("partyId");
        Map removeParams = UtilMisc.toMap("partyId", partyId);
        try {
            // WARNING: DO NOT ADD MORE ENTITIES TO THIS LIST WITHOUT THINKING AND ASKING ABOUT ITS IMPLICATIONS
            delegator.removeByAnd("PartySupplementalData", removeParams);
            delegator.removeByAnd("PartyRelationship", UtilMisc.toMap("partyIdFrom", partyId));
            delegator.removeByAnd("PartyRelationship", UtilMisc.toMap("partyIdTo", partyId));
            delegator.removeByAnd("PartyRole", removeParams);
            // this will however leave the contact info in the system still, but not associated with a party
            delegator.removeByAnd("PartyContactMech", removeParams);
            delegator.removeByAnd("PartyContactMechPurpose", removeParams);
            delegator.removeByAnd("PartyNameHistory", removeParams);
            delegator.removeByAnd("ServerHit", removeParams);
            delegator.removeByAnd("Visit", removeParams);
            delegator.removeByAnd("Visitor", removeParams);
            delegator.removeByAnd("ContactListParty", removeParams);
            delegator.removeByAnd("Person", removeParams);
            delegator.removeByAnd("PartyGroup", removeParams);
            delegator.removeByAnd("PartyStatus", removeParams);
            delegator.removeByAnd("Party", removeParams);

            return ServiceUtil.returnSuccess();
        } catch (GeneralException ex) {
            return UtilMessage.createAndLogServiceError(ex, module);
        }
    }

    /**
     * Creates a PartyContactMechPurpose
     * <b>security check</b>: userLogin partyId must equal partyId, or must have PARTYMGR_CREATE permission
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map createPartyContactMechPurpose(DispatchContext ctx, Map context) {

        GenericDelegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        Map result = new HashMap();
        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, "PARTYMGR", "_PCM_CREATE");
        Locale locale = (Locale) context.get("locale");
        Timestamp fromDate = UtilDateTime.nowTimestamp();

        if (result.size() > 0) {
            return result;
        }

        result = ServiceUtil.returnSuccess();
        result.put("fromDate", fromDate);

        // required parameters
        String contactMechId = (String) context.get("contactMechId");
        String contactMechPurposeTypeId = (String) context.get("contactMechPurposeTypeId");

        try {
            Map input = UtilMisc.toMap("partyId", partyId, "contactMechId", contactMechId, "contactMechPurposeTypeId", contactMechPurposeTypeId);
            List<GenericValue> allPCMPs = EntityUtil.filterByDate(delegator.findByAnd("PartyContactMechPurpose", input), true);
            GenericValue partyContactMechPurpose = EntityUtil.getFirst(allPCMPs);

            if (partyContactMechPurpose != null) {
                // exists already with valid date, show warning
                String errMsg = UtilProperties.getMessage("PartyUiLabels", "contactmechservices.could_not_create_new_purpose_already_exists", locale);
                errMsg += ": " + partyContactMechPurpose.getPrimaryKey().toString();
                return ServiceUtil.returnError(errMsg);
            }

            // no entry with a valid date range exists, create new with open thruDate
            input = UtilMisc.toMap("partyId", partyId, "contactMechId", contactMechId, "contactMechPurposeTypeId", contactMechPurposeTypeId, "fromDate", fromDate);
            partyContactMechPurpose = delegator.makeValue("PartyContactMechPurpose", input);
            delegator.create(partyContactMechPurpose);

            // check the PartyContactMech not to be expired
            List<EntityCondition> conditions = new ArrayList<EntityCondition>();
            conditions.add(new EntityExpr("partyId", EntityOperator.EQUALS, partyId));
            conditions.add(new EntityExpr("contactMechId", EntityOperator.EQUALS, contactMechId));
            conditions.add(EntityUtil.getFilterByDateExpr());
            List<GenericValue> partyContactMechs = delegator.findByCondition("PartyContactMech", new EntityConditionList(conditions, EntityOperator.AND), null, null);
            GenericValue partyContactMech = EntityUtil.getFirst(partyContactMechs);
            if (partyContactMech == null)
                return result;

            // get the ContactMechTypePurpose
            input = UtilMisc.toMap("contactMechPurposeTypeId", contactMechPurposeTypeId);
            List<GenericValue> contactMechTypePurposes = delegator.findByAnd("ContactMechTypePurpose", input);
            GenericValue contactMechTypePurpose = EntityUtil.getFirst(contactMechTypePurposes);
            String contactMechTypeId = contactMechTypePurpose.getString("contactMechTypeId");
            String fieldToUpdate = null;
            if ("POSTAL_ADDRESS".equals(contactMechTypeId)) {
                fieldToUpdate = "primaryPostalAddressId";
            }
            if ("TELECOM_NUMBER".equals(contactMechTypeId)) {
                fieldToUpdate = "primaryTelecomNumberId";
            }
            if ("EMAIL_ADDRESS".equals(contactMechTypeId)) {
                fieldToUpdate = "primaryEmailId";
            }
            if (fieldToUpdate == null) {
                // show warning, field updated unknown
                Debug.logWarning("Unknown " + contactMechTypeId + " contact Type to update.", module);
                return result;
            }

            // get the associated partySupplementalData
            Debug.logInfo("Updating partySupplementalData for partyId " + partyId, module);
            input = UtilMisc.toMap("partyId", partyId);
            GenericValue partySupplementalData = delegator.findByPrimaryKey("PartySupplementalData", input);

            if ("GENERAL_LOCATION".equals(contactMechPurposeTypeId)
                || "PRIMARY_PHONE".equals(contactMechPurposeTypeId)
                || "PRIMARY_EMAIL".equals(contactMechPurposeTypeId)) {
                if (partySupplementalData == null) {
                    // create a new partySupplementalData
                    input = UtilMisc.toMap("partyId", partyId, fieldToUpdate, contactMechId);
                    partySupplementalData = delegator.makeValue("PartySupplementalData", input);
                    partySupplementalData.create();
                    return result;
                }

                // create or update the field
                partySupplementalData.set(fieldToUpdate, contactMechId);
                partySupplementalData.store();
                return result;
            }

        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            Map messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            String errMsg = UtilProperties.getMessage("PartyUiLabels", "contactmechservices.could_not_add_purpose_write", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        return result;
    }

    /**
     * Deletes the PartyContactMechPurpose corresponding to the parameters in the context
     * <b>security check</b>: userLogin partyId must equal partyId, or must have PARTYMGR_DELETE permission
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map deletePartyContactMechPurpose(DispatchContext ctx, Map context) {

        GenericDelegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        Map result = new HashMap();
        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, "PARTYMGR", "_PCM_DELETE");
        Locale locale = (Locale) context.get("locale");

        if (result.size() > 0)
            return result;

        result = ServiceUtil.returnSuccess();

        // required parameters
        String contactMechId = (String) context.get("contactMechId");
        String contactMechPurposeTypeId = (String) context.get("contactMechPurposeTypeId");
        Timestamp fromDate = (Timestamp) context.get("fromDate");

        try {
            Map input = UtilMisc.toMap("partyId", partyId, "contactMechId", contactMechId, "contactMechPurposeTypeId", contactMechPurposeTypeId, "fromDate", fromDate);
            GenericValue partyContactMechPurpose = delegator.findByPrimaryKey("PartyContactMechPurpose", input);
            if (partyContactMechPurpose == null) {
                String errMsg = UtilProperties.getMessage("PartyUiLabels", "contactmechservices.could_not_delete_purpose_from_contact_mechanism_not_found", locale);
                return ServiceUtil.returnError(errMsg);
            }

            partyContactMechPurpose.set("thruDate", UtilDateTime.nowTimestamp());
            partyContactMechPurpose.store();

            // get the ContactMechTypePurpose
            input = UtilMisc.toMap("contactMechPurposeTypeId", contactMechPurposeTypeId);
            List<GenericValue> contactMechTypePurposes = delegator.findByAnd("ContactMechTypePurpose", input);
            GenericValue contactMechTypePurpose = EntityUtil.getFirst(contactMechTypePurposes);
            String contactMechTypeId = contactMechTypePurpose.getString("contactMechTypeId");
            String fieldToUpdate = null;
            if ("POSTAL_ADDRESS".equals(contactMechTypeId)) {
                fieldToUpdate = "primaryPostalAddressId";
            }
            if ("TELECOM_NUMBER".equals(contactMechTypeId)) {
                fieldToUpdate = "primaryTelecomNumberId";
            }
            if ("EMAIL_ADDRESS".equals(contactMechTypeId)) {
                fieldToUpdate = "primaryEmailId";
            }
            if (fieldToUpdate == null) {
                // show warning, field updated unknown
                Debug.logWarning("Unknown " + contactMechTypeId + " contact Type to update.", module);
                return result;
            }

            // get the associated partySupplementalData
            Debug.logInfo("Updating partySupplementalData for partyId " + partyId, module);
            input = UtilMisc.toMap("partyId", partyId);
            GenericValue partySupplementalData = delegator.findByPrimaryKey("PartySupplementalData", input);

            if ("GENERAL_LOCATION".equals(contactMechPurposeTypeId)
                || "PRIMARY_PHONE".equals(contactMechPurposeTypeId)
                || "PRIMARY_EMAIL".equals(contactMechPurposeTypeId)) {
                if (partySupplementalData != null) {
                    // create or update the field
                    partySupplementalData.set(fieldToUpdate, null);
                    partySupplementalData.store();
                    return result;
                }
            }

        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), module);
            Map messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            String errMsg = UtilProperties.getMessage("PartyUiLabels", "contactmechservices.could_not_add_purpose_write", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        return result;
    }
    
    public static Map setSupplementalDataForAllParties(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale)context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        Map result = ServiceUtil.returnSuccess();
        int partyUpdated = 0;
        try {

            List<GenericValue> parties = delegator.findAll("Party");
            for (GenericValue party : parties) {
                boolean updated = false;
                // Update PartySupplementalData
                TransactionUtil.begin();
                updated = PartyHelper.updatePartySupplementalData(delegator, party.getString("partyId"));
                TransactionUtil.commit();
                if (updated) {
                    partyUpdated++;
                }
            }

        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, module);
        }
        result.put("partiesUpdated", new Integer(partyUpdated));
        return result;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, ? extends Object> createPartyCarrierAccount(DispatchContext dctx, Map<String, ? extends Object> context) {
        GenericDelegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");

        String partyId = (String) context.get("partyId");
        String carrierPartyId = (String) context.get("carrierPartyId");
        Timestamp fromDate = (Timestamp) context.get("fromDate");
        Timestamp thruDate = (Timestamp) context.get("thruDate");
        String accountNumber = (String) context.get("accountNumber");
        String postalCode = (String) context.get("postalCode");
        String countryGeoCode = (String) context.get("countryGeoCode");
        String isDefault = (String) context.get("isDefault");

        try {

            // Apply default values
            if (fromDate == null)
                fromDate = UtilDateTime.nowTimestamp();
            if (isDefault == null)
                isDefault = "N";

            // Only default account may exist for partyId & carrierPartyId combination
            if ("Y".equals(isDefault)) {
                List<EntityCondition> conditions = FastList.newInstance();
                conditions.add(new EntityExpr("partyId", EntityOperator.EQUALS, partyId));
                conditions.add(new EntityExpr("carrierPartyId", EntityOperator.EQUALS, carrierPartyId));
                conditions.add(new EntityExpr("isDefault", EntityOperator.EQUALS, "Y"));
                conditions.add(EntityUtil.getFilterByDateExpr());
                delegator.storeByCondition("PartyCarrierAccount", UtilMisc.toMap("isDefault", "N"), new EntityConditionList(conditions, EntityOperator.AND));
            }

            GenericValue partyCarrierAccount = delegator.makeValue("PartyCarrierAccount");
            partyCarrierAccount.set("partyId", partyId);
            partyCarrierAccount.set("carrierPartyId", carrierPartyId);
            partyCarrierAccount.set("fromDate", fromDate);
            partyCarrierAccount.set("thruDate", thruDate);
            partyCarrierAccount.set("accountNumber", accountNumber);
            partyCarrierAccount.set("postalCode", postalCode);
            partyCarrierAccount.set("countryGeoCode", countryGeoCode);
            partyCarrierAccount.set("isDefault", isDefault);

            partyCarrierAccount.create();

        } catch (GenericEntityException e) {
            UtilMessage.createAndLogServiceError(e, locale, module);
        }   

        return ServiceUtil.returnSuccess();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, ? extends Object> updatePartyCarrierAccount(DispatchContext dctx, Map<String, ? extends Object> context) {
        GenericDelegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");

        String partyId = (String) context.get("partyId");
        String carrierPartyId = (String) context.get("carrierPartyId");
        Timestamp fromDate = (Timestamp) context.get("fromDate");
        Timestamp thruDate = (Timestamp) context.get("thruDate");
        String accountNumber = (String) context.get("accountNumber");
        String postalCode = (String) context.get("postalCode");
        String countryGeoCode = (String) context.get("countryGeoCode");
        String isDefault = (String) context.get("isDefault");

        try {

            GenericValue partyCarrierAccount = delegator.findByPrimaryKey(
                    "PartyCarrierAccount", 
                    UtilMisc.toMap(
                            "partyId", partyId,
                            "carrierPartyId", carrierPartyId,
                            "fromDate", fromDate
                    )
            );

            if (UtilValidate.isEmpty(partyCarrierAccount))
                return UtilMessage.createAndLogServiceFailure("OpentapsError_PartyCarrierAccountNotFound", context, locale, module);

            // Only default account may exist for partyId & carrierPartyId combination
            if ("Y".equals(isDefault)) {
                List<EntityCondition> conditions = FastList.newInstance();
                conditions.add(new EntityExpr("partyId", EntityOperator.EQUALS, partyId));
                conditions.add(new EntityExpr("carrierPartyId", EntityOperator.EQUALS, carrierPartyId));
                conditions.add(new EntityExpr("isDefault", EntityOperator.EQUALS, "Y"));
                conditions.add(EntityUtil.getFilterByDateExpr());
                delegator.storeByCondition("PartyCarrierAccount", UtilMisc.toMap("isDefault", "N"), new EntityConditionList(conditions, EntityOperator.AND));
            }

            partyCarrierAccount.set("thruDate", thruDate);
            partyCarrierAccount.set("accountNumber", accountNumber);
            partyCarrierAccount.set("postalCode", postalCode);
            partyCarrierAccount.set("countryGeoCode", countryGeoCode);
            partyCarrierAccount.set("isDefault", isDefault);

            partyCarrierAccount.store();

        } catch (GenericEntityException e) {
            UtilMessage.createAndLogServiceError(e, locale, module);
        }

        return ServiceUtil.returnSuccess();
    }
}
