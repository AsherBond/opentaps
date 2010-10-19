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
import org.ofbiz.entity.Delegator;
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
import org.opentaps.base.services.OpentapsGetViewPreferenceService;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;

/**
 * General party services for opentaps applications.
 */
public final class PartyServices {

    private PartyServices() { }

    private static String MODULE = PartyServices.class.getName();

    public static Map<String, Object> createViewPreference(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String userLoginId = (String) context.get("userLoginId");
        if (!userLogin.get("userLoginId").equals(userLoginId)) {
            if (!security.hasEntityPermission("PARTYMGR", "_VPREF_UPDATE", userLogin)) {
                return UtilMessage.createServiceError("PartyCreateUserLoginViewPrefPermissionError", locale);
            }
        }
        try {
            GenericValue pref = delegator.makeValue("ViewPreference");
            pref.setPKFields(context);
            pref.setNonPKFields(context);
            pref.create();
        } catch (GenericEntityException e) {
            UtilMessage.createAndLogServiceError(e, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> updateViewPreference(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String userLoginId = (String) context.get("userLoginId");
        if (!userLogin.get("userLoginId").equals(userLoginId)) {
            if (!security.hasEntityPermission("PARTYMGR", "_VPREF_UPDATE", userLogin)) {
                return UtilMessage.createServiceError("PartyCreateUserLoginViewPrefPermissionError", locale);
            }
        }
        try {
            Map<String, Object> input = UtilMisc.toMap("userLoginId", userLoginId, "viewPrefTypeId", context.get("viewPrefTypeId"));
            GenericValue pref = delegator.findByPrimaryKey("ViewPreference", input);
            if (pref == null) {
                return UtilMessage.createServiceError("OpentapsError_ViewPrefNotFound", locale, input);
            }
            pref.setNonPKFields(context);
            pref.store();
        } catch (GenericEntityException e) {
            UtilMessage.createAndLogServiceError(e, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> getViewPreference(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        Locale locale = UtilCommon.getLocale(context);

        OpentapsGetViewPreferenceService service = OpentapsGetViewPreferenceService.fromInput(context);
        GenericValue userLogin = service.getInUserLogin();

        try {
            Map<String, Object> input = UtilMisc.<String, Object>toMap("userLoginId", userLogin.get("userLoginId"), "viewPrefTypeId", service.getInViewPrefTypeId());
            GenericValue pref = delegator.findByPrimaryKey("ViewPreference", input);
            if (pref != null) {
                String prefString = pref.getString("viewPrefString");
                if (UtilValidate.isNotEmpty(prefString)) {
                    service.setOutViewPrefValue(prefString);
                } else {
                    service.setOutViewPrefValue(pref.getString("viewPrefEnumId"));
                }
            }
        } catch (GenericEntityException e) {
            UtilMessage.createAndLogServiceError(e, MODULE);
        }

        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.putAll(service.outputMap());
        return results;
    }

    public static Map<String, Object> setViewPreference(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String viewPrefTypeId = (String) context.get("viewPrefTypeId");
        String value = (String) context.get("viewPrefValue");
        if (value != null && value.trim().length() == 0) {
            value = null;
        }
        try {
            // if preference already exists, we'll be doing an update, otherwise a create
            GenericValue pref = ViewPrefWorker.getViewPreferenceValue(userLogin, viewPrefTypeId);

            // prepare the preference
            Map<String, Object> input = UtilMisc.toMap("userLogin", userLogin, "userLoginId", userLogin.get("userLoginId"), "viewPrefTypeId", viewPrefTypeId);

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
            return UtilMessage.createAndLogServiceError(e, "CrmErrorSetPreferenceFail", locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorSetPreferenceFail", locale, MODULE);
        }
    }

    public static Map<String, Object> checkReceiveEmailOwnerUniqueness(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);

        String contactMechId = (String) context.get("contactMechId");
        String contactMechPurposeTypeId = (String) context.get("contactMechPurposeTypeId");

        if (!"RECEIVE_EMAIL_OWNER".equals(contactMechPurposeTypeId)) {
            return ServiceUtil.returnSuccess();
        }

        try {

            GenericValue contactMech = delegator.findByPrimaryKeyCache("ContactMech", UtilMisc.toMap("contactMechId", contactMechId));
            String contactEmail = null;
            if (contactMech != null) {
                contactEmail = contactMech.getString("infoString");
            }

            Timestamp now = UtilDateTime.nowTimestamp();
            EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                    EntityCondition.makeCondition("contactMechPurposeTypeId", "RECEIVE_EMAIL_OWNER"),
                    EntityCondition.makeCondition("contactMechTypeId", "EMAIL_ADDRESS"),
                    EntityUtil.getFilterByDateExpr(now, "contactFromDate", "contactThruDate"),
                    EntityUtil.getFilterByDateExpr(now, "purposeFromDate", "purposeThruDate"));

            List<GenericValue> contactMechs = delegator.findByCondition("PartyContactWithPurpose", conditions, null, null);

            for (GenericValue current  : contactMechs) {
                String currentEmail = current.getString("infoString");
                if (currentEmail.equalsIgnoreCase(contactEmail) && !contactMechId.equals(current.getString("contactMechId"))) {
                    return UtilMessage.createAndLogServiceError("OpentapsError_EmailOwnerExist", locale, MODULE);
                }
            }

        } catch (GenericEntityException gee) {
            return UtilMessage.createAndLogServiceError(gee, locale, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Expires any existing PartyClassifications for a partyId where partyClassificationGroupId is any partyClassificationGroupId related to partyClassificationTypeId,
     *  and creates a new PartyClassification.
     */
    public static Map<String, Object> expireAndCreatePartyClassification(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String partyId = (String) context.get("partyId");
        String partyClassificationGroupId = (String) context.get("partyClassificationGroupId");
        String partyClassificationTypeId = (String) context.get("partyClassificationTypeId");

        Timestamp now = UtilDateTime.nowTimestamp();
        try {
            List<GenericValue> partyClassGroups = delegator.findByAnd("PartyClassificationGroup", UtilMisc.toMap("partyClassificationTypeId", partyClassificationTypeId));
            List<String> partyClassGroupIds = EntityUtil.<String>getFieldListFromEntityList(partyClassGroups, "partyClassificationGroupId", true);

            EntityCondition cond = EntityCondition.makeCondition(EntityOperator.AND,
                    EntityCondition.makeCondition("partyId", partyId),
                    EntityCondition.makeCondition("partyClassificationGroupId", EntityOperator.IN, partyClassGroupIds));
            List<GenericValue> partyClassifications = delegator.findByCondition("PartyClassification", cond, null, null);
            partyClassifications = EntityUtil.filterByDate(partyClassifications, now);
            for (GenericValue partyClassification : partyClassifications) {
                Map<String, Object> serviceResult = dispatcher.runSync("updatePartyClassification", UtilMisc.toMap("partyId", partyId, "partyClassificationGroupId", partyClassification.get("partyClassificationGroupId"), "fromDate", partyClassification.get("fromDate"), "thruDate", now, "userLogin", userLogin));
                if (ServiceUtil.isError(serviceResult)) {
                    return serviceResult;
                }
            }
            Map<String, Object> serviceResult = dispatcher.runSync("createPartyClassification", UtilMisc.toMap("partyId", partyId, "partyClassificationGroupId", partyClassificationGroupId, "userLogin", userLogin));
            if (ServiceUtil.isError(serviceResult)) {
                return serviceResult;
            }
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }


    /*
     * opentaps.sendInternalMessage to send a message.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> sendInternalMessage(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        // Get the sender and verify he is able to send im
        String partyIdFrom = (String) context.get("partyIdFrom");
        if (UtilValidate.isEmpty(partyIdFrom)) {
            partyIdFrom = userLogin.getString("partyId");
        }

        if (!PartyHelper.isInternalMessageSender(partyIdFrom, delegator)) {
            return UtilMessage.createServiceError("OpentapsError_InternalMessageNoSenderPermission", locale, context);
        }

        // Get the recipients
        List<String> partyIdTo = (List<String>) context.get("partyIdTo");
        if (partyIdTo == null) {
            partyIdTo = FastList.newInstance();
        }

        String partyIdToAsString = (String) context.get("partyIdToAsString");
        String[] toAddrs = partyIdToAsString.split("[,;]");
        if (toAddrs.length > 0) {
            for (String addr : toAddrs) {
                if (GenericValidator.isBlankOrNull(addr)) {
                    continue;
                }
                String addresseeId = (addr.indexOf("<") != -1 && addr.indexOf(">") != -1) ? addr.substring(addr.indexOf("<") + 1, addr.indexOf(">")) : addr;
                partyIdTo.add(addresseeId.trim());
            }
        }

        // Get subject of the message
        String subject = (String) context.get("subject");

        // Get the message body
        String message = (String) context.get("message");

        if (UtilValidate.isEmpty(message)) {
            return UtilMessage.createServiceError("OpentapsError_InternalMessageBodyRequired", locale);
        }

        // Send im to every body
        Iterator<String> parties = partyIdTo.iterator();
        while (parties.hasNext()) {
            // verify the recipient is able to receive internal messages
            String party = parties.next();
            if (!PartyHelper.isInternalMessageRecipient(party, delegator)) {
                Debug.logError(UtilMessage.expandLabel("OpentapsError_InternalMessageNoRecipientPermission", locale, UtilMisc.toMap("partyId", party)), MODULE);
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
                return UtilMessage.createAndLogServiceFailure("OpentapsError_InternalMessageNoRecipientPermission", UtilMisc.toMap("partyId", party), locale, MODULE);
            }
        }

        return ServiceUtil.returnSuccess();
    }

    /*
     * opentaps.receiveInternalMessage to receive a message.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> receiveInternalMessage(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);

        // get filter options
        List<String> partyIdsFrom = (List<String>) context.get("partyIdFrom");
        String partyIdTo = (String) context.get("partyIdTo");
        if (UtilValidate.isEmpty(partyIdTo)) {
            return UtilMessage.createServiceError("OpentapsError_InternalMessageNoRecipients", locale);
        }
        List<String> messageContains = (List<String>) context.get("messageContains");
        List<String> subjectContains = (List<String>) context.get("subjectContains");
        Boolean isRead = (Boolean) context.get("isRead");
        Timestamp dateFrom = (Timestamp) context.get("dateFrom");
        Timestamp dateTo = (Timestamp) context.get("dateTo");

        List<EntityCondition> conditions = new ArrayList<EntityCondition>();

        if (UtilValidate.isNotEmpty(partyIdsFrom)) {
            for (String partyId : partyIdsFrom) {
                conditions.add(EntityCondition.makeCondition("partyIdFrom", partyId));
            }
        }

        conditions.add(EntityCondition.makeCondition("partyIdTo", partyIdTo));

        if (UtilValidate.isNotEmpty(messageContains)) {
            for (String incWord : messageContains) {
                conditions.add(EntityCondition.makeCondition("content", EntityOperator.LIKE, "%" + incWord + "%"));
            }
        }

        if (UtilValidate.isNotEmpty(subjectContains)) {
            for (String excWord : subjectContains) {
                conditions.add(EntityCondition.makeCondition("subject", EntityOperator.LIKE, "%" + excWord + "%"));
            }
        }

        if (UtilValidate.isNotEmpty(isRead)) {
            conditions.add(EntityCondition.makeCondition("statusId", isRead.booleanValue() ? "COM_COMPLETE" : "COM_ENTERED"));
        }

        if (UtilValidate.isNotEmpty(dateFrom)) {
            conditions.add(EntityCondition.makeCondition("entryDate", EntityOperator.GREATER_THAN_EQUAL_TO, dateFrom));
        }

        if (UtilValidate.isNotEmpty(dateTo)) {
            conditions.add(EntityCondition.makeCondition("entryDate", EntityOperator.LESS_THAN_EQUAL_TO, dateTo));
        }

        List<String> orderBy = Arrays.asList("entryDate");
        List<String> selectList = Arrays.asList("communicationEventId", "communicationEventTypeId", "statusId", "roleTypeIdFrom", "roleTypeIdTo", "partyIdFrom", "partyIdTo", "entryDate", "subject", "content");

        Map<String, Object> result = ServiceUtil.returnSuccess();

        // get messages
        try {

            List<GenericValue> messages = delegator.findByCondition("CommunicationEvent", EntityCondition.makeCondition(conditions, EntityOperator.AND), selectList, orderBy);
            result.put("messages", messages);

        } catch (GenericEntityException ex) {
            return UtilMessage.createAndLogServiceError(ex, MODULE);
        }

        return result;
    }

    public static Map<String, Object> removeParty(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        if (!security.hasEntityPermission("PARTYMGR", "_ADMIN", userLogin)) {
            return ServiceUtil.returnError("You do not have permission to remove a product.  CATALOG_ADMIN permission is required.");
        }

        String partyId = (String) context.get("partyId");
        Map<String, Object> removeParams = UtilMisc.<String, Object>toMap("partyId", partyId);
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
            return UtilMessage.createAndLogServiceError(ex, MODULE);
        }
    }

    /**
     * Creates a PartyContactMechPurpose.
     * <b>security check</b>: userLogin partyId must equal partyId, or must have PARTYMGR_CREATE permission.
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> createPartyContactMechPurpose(DispatchContext ctx, Map<String, Object> context) {

        Delegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        Map<String, Object> result = new HashMap<String, Object>();
        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, "PARTYMGR", "_PCM_CREATE");
        Locale locale = UtilCommon.getLocale(context);
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
            Map<String, Object> input = UtilMisc.<String, Object>toMap("partyId", partyId, "contactMechId", contactMechId, "contactMechPurposeTypeId", contactMechPurposeTypeId);
            List<GenericValue> allPCMPs = EntityUtil.filterByDate(delegator.findByAnd("PartyContactMechPurpose", input), true);
            GenericValue partyContactMechPurpose = EntityUtil.getFirst(allPCMPs);

            if (partyContactMechPurpose != null) {
                // exists already with valid date, show warning
                String errMsg = UtilProperties.getMessage("PartyErrorUiLabels", "contactmechservices.could_not_create_new_purpose_already_exists", locale);
                errMsg += ": " + partyContactMechPurpose.getPrimaryKey().toString();
                return ServiceUtil.returnError(errMsg);
            }

            // no entry with a valid date range exists, create new with open thruDate
            input = UtilMisc.toMap("partyId", partyId, "contactMechId", contactMechId, "contactMechPurposeTypeId", contactMechPurposeTypeId, "fromDate", fromDate);
            partyContactMechPurpose = delegator.makeValue("PartyContactMechPurpose", input);
            delegator.create(partyContactMechPurpose);

            // check the PartyContactMech not to be expired
            EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                    EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId),
                    EntityCondition.makeCondition("contactMechId", EntityOperator.EQUALS, contactMechId),
                    EntityUtil.getFilterByDateExpr());
            List<GenericValue> partyContactMechs = delegator.findByCondition("PartyContactMech", conditions, null, null);
            GenericValue partyContactMech = EntityUtil.getFirst(partyContactMechs);
            if (partyContactMech == null) {
                return result;
            }

            // get the ContactMechTypePurpose
            input = UtilMisc.<String, Object>toMap("contactMechPurposeTypeId", contactMechPurposeTypeId);
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
                Debug.logWarning("Unknown " + contactMechTypeId + " contact Type to update.", MODULE);
                return result;
            }

            // get the associated partySupplementalData
            Debug.logInfo(UtilMessage.expandLabel("OpentapsInfo_UpdatePartySupplimentalData", UtilMisc.toMap("partyId", partyId), locale), MODULE);
            input = UtilMisc.<String, Object>toMap("partyId", partyId);
            GenericValue partySupplementalData = delegator.findByPrimaryKey("PartySupplementalData", input);

            if ("GENERAL_LOCATION".equals(contactMechPurposeTypeId)
                    || "PRIMARY_PHONE".equals(contactMechPurposeTypeId)
                    || "PRIMARY_EMAIL".equals(contactMechPurposeTypeId)) {
                if (partySupplementalData == null) {
                    // create a new partySupplementalData
                    input = UtilMisc.<String, Object>toMap("partyId", partyId, fieldToUpdate, contactMechId);
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
            Debug.logWarning(e.getMessage(), MODULE);
            Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            String errMsg = UtilProperties.getMessage("PartyUiLabels", "contactmechservices.could_not_add_purpose_write", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        return result;
    }

    /**
     * Deletes the PartyContactMechPurpose corresponding to the parameters in the context.
     * <b>security check</b>: userLogin partyId must equal partyId, or must have PARTYMGR_DELETE permission.
     *@param ctx The DispatchContext that this service is operating in
     *@param context Map containing the input parameters
     *@return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> deletePartyContactMechPurpose(DispatchContext ctx, Map<String, Object> context) {

        Delegator delegator = ctx.getDelegator();
        Security security = ctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        Map<String, Object> result = new HashMap<String, Object>();
        String partyId = ServiceUtil.getPartyIdCheckSecurity(userLogin, security, context, result, "PARTYMGR", "_PCM_DELETE");
        Locale locale = UtilCommon.getLocale(context);

        if (result.size() > 0) {
            return result;
        }

        result = ServiceUtil.returnSuccess();

        // required parameters
        String contactMechId = (String) context.get("contactMechId");
        String contactMechPurposeTypeId = (String) context.get("contactMechPurposeTypeId");
        Timestamp fromDate = (Timestamp) context.get("fromDate");

        try {
            Map<String, Object> input = UtilMisc.toMap("partyId", partyId, "contactMechId", contactMechId, "contactMechPurposeTypeId", contactMechPurposeTypeId, "fromDate", fromDate);
            GenericValue partyContactMechPurpose = delegator.findByPrimaryKey("PartyContactMechPurpose", input);
            if (partyContactMechPurpose == null) {
                String errMsg = UtilProperties.getMessage("PartyUiLabels", "contactmechservices.could_not_delete_purpose_from_contact_mechanism_not_found", locale);
                return ServiceUtil.returnError(errMsg);
            }

            partyContactMechPurpose.set("thruDate", UtilDateTime.nowTimestamp());
            partyContactMechPurpose.store();

            // get the ContactMechTypePurpose
            input = UtilMisc.<String, Object>toMap("contactMechPurposeTypeId", contactMechPurposeTypeId);
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
                Debug.logWarning("Unknown " + contactMechTypeId + " contact Type to update.", MODULE);
                return result;
            }

            // get the associated partySupplementalData
            Debug.logInfo(UtilMessage.expandLabel("OpentapsInfo_UpdatePartySupplimentalData", UtilMisc.toMap("partyId", partyId), locale), MODULE);
            input = UtilMisc.<String, Object>toMap("partyId", partyId);
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
            Debug.logWarning(e.getMessage(), MODULE);
            Map<String, String> messageMap = UtilMisc.toMap("errMessage", e.getMessage());
            String errMsg = UtilProperties.getMessage("PartyUiLabels", "contactmechservices.could_not_add_purpose_write", messageMap, locale);
            return ServiceUtil.returnError(errMsg);
        }

        return result;
    }

    /**
     * Updates PartySupplementalData contact mech ids running as SECA on updatePartyContactMechService.
     *
     * @param dctx The DispatchContext that this service is operating in
     * @param context Map containing the input parameters
     * @return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> updatePartySupplementalData(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);
        Map<String, Object> results = ServiceUtil.returnSuccess();

        String partyId = (String) context.get("partyId");
        String contactMechId = (String) context.get("contactMechId");
        String contactMechTypeId = (String) context.get("contactMechTypeId");

        String purpose = null;
        String fieldToUpdate = null;
        if ("POSTAL_ADDRESS".equals(contactMechTypeId)) {
            purpose = "GENERAL_LOCATION";
            fieldToUpdate = "primaryPostalAddressId";
        } else if ("TELECOM_NUMBER".equals(contactMechTypeId)) {
            purpose = "PRIMARY_PHONE";
            fieldToUpdate = "primaryTelecomNumberId";
        } else if ("EMAIL_ADDRESS".equals(contactMechTypeId)) {
            purpose = "PRIMARY_EMAIL";
            fieldToUpdate = "primaryEmailId";
        } else {
            return results;
        }

        try {
            EntityConditionList<EntityCondition> conditionList = EntityCondition.makeCondition(
                    UtilMisc.toList(
                            EntityCondition.makeCondition("partyId", partyId),
                            EntityCondition.makeCondition("contactMechId", contactMechId),
                            EntityCondition.makeCondition("contactMechTypeId", contactMechTypeId),
                            EntityUtil.getFilterByDateExpr("contactFromDate", "contactThruDate"),
                            EntityUtil.getFilterByDateExpr("purposeFromDate", "purposeThruDate")
                    ), EntityOperator.AND
            );
            List<GenericValue> contactMechAndPurpose = delegator.findList("PartyContactWithPurpose", conditionList, UtilMisc.toSet("contactMechPurposeTypeId"), UtilMisc.toList("purposeFromDate DESC"), null, false);
            if (UtilValidate.isNotEmpty(contactMechAndPurpose)) {
                for (GenericValue contactMechPurpose : contactMechAndPurpose) {
                    if (purpose.equals(contactMechPurpose.getString("contactMechPurposeTypeId"))) {
                        GenericValue partySupplData = delegator.findByPrimaryKey("PartySupplementalData", UtilMisc.toMap("partyId", partyId));
                        if (partySupplData != null && !contactMechId.equals(partySupplData.getString(fieldToUpdate))) {
                            Debug.logInfo(UtilMessage.expandLabel("OpentapsInfo_UpdatePartySupplimentalData", UtilMisc.toMap("partyId", partyId), locale), MODULE);
                            partySupplData.set(fieldToUpdate, contactMechId);
                            partySupplData.store();
                        }
                    }
                }
            }

        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }

        return results;
    }

    /**
     * Sets PartySupplementalData contact mech ids to null if corresponding contact mech is deleted.<br>
     * It is called as SECA on deletePartyContactMech service.
     *
     * @param dctx The DispatchContext that this service is operating in
     * @param context Map containing the input parameters
     * @return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> clearPartySupplementalData(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);
        Map<String, Object> results = ServiceUtil.returnSuccess();

        String partyId = (String) context.get("partyId");
        String contactMechId = (String) context.get("contactMechId");

        try {
            EntityConditionList<EntityExpr> conditionList = EntityCondition.makeCondition(
                    UtilMisc.toList(
                            EntityCondition.makeCondition("partyId", partyId),
                            EntityCondition.makeCondition("contactMechId", contactMechId)
                    ), EntityOperator.AND
            );
            GenericValue partyContactMech = EntityUtil.getFirst(delegator.findList("PartyContactMech", conditionList, null, UtilMisc.toList("fromDate DESC"), null, false));
            if (UtilValidate.isNotEmpty(partyContactMech)) {
                List<GenericValue> partyContactMechPurps = partyContactMech.getRelated("PartyContactMechPurpose", UtilMisc.toList("fromDate DESC"));
                if (UtilValidate.isNotEmpty(partyContactMechPurps)) {
                    for (GenericValue contactMechPurpose : partyContactMechPurps) {
                        String purposeTypeId = contactMechPurpose.getString("contactMechPurposeTypeId");
                        String fieldToUpdate = null;
                        if ("GENERAL_LOCATION".equals(purposeTypeId)) {
                            fieldToUpdate = "primaryPostalAddressId";
                        } else if ("PRIMARY_PHONE".equals(purposeTypeId)) {
                            fieldToUpdate = "primaryTelecomNumberId";
                        } else if ("PRIMARY_EMAIL".equals(purposeTypeId)) {
                            fieldToUpdate = "primaryEmailId";
                        } else {
                            return results;
                        }

                        GenericValue partySupplData = delegator.findByPrimaryKey("PartySupplementalData", UtilMisc.toMap("partyId", partyId));
                        if (partySupplData != null && UtilValidate.isNotEmpty(partySupplData.getString(fieldToUpdate))) {
                            Debug.logInfo(UtilMessage.expandLabel("OpentapsInfo_UpdatePartySupplimentalData", UtilMisc.toMap("partyId", partyId), locale), MODULE);
                            partySupplData.set(fieldToUpdate, null);
                            partySupplData.store();
                        }
                    }
                }
            }
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }

        return results;
    }

    public static Map<String, Object> setSupplementalDataForAllParties(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();

        Map<String, Object> result = ServiceUtil.returnSuccess();
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
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
        result.put("partiesUpdated", new Integer(partyUpdated));
        return result;
    }

    public static Map<String, Object> createPartyCarrierAccount(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);

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
            if (fromDate == null) {
                fromDate = UtilDateTime.nowTimestamp();
            }
            if (isDefault == null) {
                isDefault = "N";
            }

            // Only default account may exist for partyId & carrierPartyId combination
            if ("Y".equals(isDefault)) {
                EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                        EntityCondition.makeCondition("partyId", partyId),
                        EntityCondition.makeCondition("carrierPartyId", carrierPartyId),
                        EntityCondition.makeCondition("isDefault", "Y"),
                        EntityUtil.getFilterByDateExpr());
                delegator.storeByCondition("PartyCarrierAccount", UtilMisc.toMap("isDefault", "N"), conditions);
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
            UtilMessage.createAndLogServiceError(e, locale, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> updatePartyCarrierAccount(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);

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

            if (UtilValidate.isEmpty(partyCarrierAccount)) {
                return UtilMessage.createAndLogServiceFailure("OpentapsError_PartyCarrierAccountNotFound", context, locale, MODULE);
            }

            // Only default account may exist for partyId & carrierPartyId combination
            if ("Y".equals(isDefault)) {
                EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                        EntityCondition.makeCondition("partyId", partyId),
                        EntityCondition.makeCondition("carrierPartyId", carrierPartyId),
                        EntityCondition.makeCondition("isDefault", "Y"),
                        EntityUtil.getFilterByDateExpr());
                delegator.storeByCondition("PartyCarrierAccount", UtilMisc.toMap("isDefault", "N"), conditions);
            }

            partyCarrierAccount.set("thruDate", thruDate);
            partyCarrierAccount.set("accountNumber", accountNumber);
            partyCarrierAccount.set("postalCode", postalCode);
            partyCarrierAccount.set("countryGeoCode", countryGeoCode);
            partyCarrierAccount.set("isDefault", isDefault);

            partyCarrierAccount.store();

        } catch (GenericEntityException e) {
            UtilMessage.createAndLogServiceError(e, locale, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }
}
