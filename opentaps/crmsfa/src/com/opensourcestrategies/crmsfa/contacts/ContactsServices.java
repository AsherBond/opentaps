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
package com.opensourcestrategies.crmsfa.contacts;

import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.opensourcestrategies.crmsfa.party.PartyHelper;
import com.opensourcestrategies.crmsfa.security.CrmsfaSecurity;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.base.constants.PartyRelationshipTypeConstants;

/**
 * Contacts services. The service documentation is in services_contacts.xml.
 *
 * @author     <a href="mailto:leon@opensourcestrategies.com">Leon Torres</a>
 */
public final class ContactsServices {

    private ContactsServices() { }

    private static final String MODULE = ContactsServices.class.getName();

    public static Map<String, Object> createContact(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        if (!security.hasPermission("CRMSFA_CONTACT_CREATE", userLogin)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
        }

        // the net result of creating an contact is the generation of a Contact partyId
        String contactPartyId = (String) context.get("partyId");
        try {
            // make sure user has the right crmsfa roles defined.  otherwise the contact will be created as deactivated.
            if (UtilValidate.isEmpty(PartyHelper.getFirstValidTeamMemberRoleTypeId(userLogin.getString("partyId"), delegator))) {
                return UtilMessage.createAndLogServiceError("CrmError_NoRoleForCreateParty", UtilMisc.toMap("userPartyName", org.ofbiz.party.party.PartyHelper.getPartyName(delegator, userLogin.getString("partyId"), false), "requiredRoleTypes", PartyHelper.TEAM_MEMBER_ROLES), locale, MODULE);
            }

            // if we're given the partyId to create, then verify it is free to use
            if (contactPartyId != null) {
                Map<String, Object> findMap = UtilMisc.<String, Object>toMap("partyId", contactPartyId);
                GenericValue party = delegator.findByPrimaryKey("Party", findMap);
                if (party != null) {
                    return UtilMessage.createAndLogServiceError("person.create.person_exists", findMap, locale, MODULE);
                }
            }

            // create the Party and Person, which results in a partyId
            Map<String, Object> input = UtilMisc.<String, Object>toMap("firstName", context.get("firstName"), "lastName", context.get("lastName"));
            if (contactPartyId != null) {
                input.put("partyId", contactPartyId);
            }
            input.put("firstNameLocal", context.get("firstNameLocal"));
            input.put("lastNameLocal", context.get("lastNameLocal"));
            input.put("personalTitle", context.get("personalTitle"));
            input.put("preferredCurrencyUomId", context.get("preferredCurrencyUomId"));
            input.put("description", context.get("description"));
            input.put("birthDate", context.get("birthDate"));
            Map<String, Object> serviceResults = dispatcher.runSync("createPerson", input);
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorCreateContactFail", locale, MODULE);
            }
            contactPartyId = (String) serviceResults.get("partyId");

            // create a PartyRole for the resulting Contact partyId with roleTypeId = CONTACT
            serviceResults = dispatcher.runSync("createPartyRole", UtilMisc.toMap("partyId", contactPartyId, "roleTypeId", "CONTACT", "userLogin", userLogin));
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorCreateContactFail", locale, MODULE);
            }

            // create PartySupplementalData
            GenericValue partyData = delegator.makeValue("PartySupplementalData", UtilMisc.toMap("partyId", contactPartyId));
            partyData.setNonPKFields(context);
            partyData.create();

            // create a party relationship between the userLogin and the Contact with partyRelationshipTypeId RESPONSIBLE_FOR
            createResponsibleContactRelationshipForParty(userLogin.getString("partyId"), contactPartyId, userLogin, delegator, dispatcher);

            // if initial marketing campaign is provided, add it
            String marketingCampaignId = (String) context.get("marketingCampaignId");
            if (marketingCampaignId != null) {
                serviceResults = dispatcher.runSync("crmsfa.addContactMarketingCampaign",
                        UtilMisc.toMap("partyId", contactPartyId, "marketingCampaignId", marketingCampaignId, "userLogin", userLogin));
                if (ServiceUtil.isError(serviceResults)) {
                    return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorCreateContactFail", locale, MODULE);
                }
            }

            // create basic contact info
            ModelService service = dctx.getModelService("crmsfa.createBasicContactInfoForParty");
            input = service.makeValid(context, "IN");
            input.put("partyId", contactPartyId);
            serviceResults = dispatcher.runSync(service.name, input);
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorCreateContactFail", locale, MODULE);
            }

        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorCreateContactFail", locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorCreateContactFail", locale, MODULE);
        }

        // return the partyId of the newly created Contact
        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put("partyId", contactPartyId);
        results.put("contactPartyId", contactPartyId);
        return results;
    }

    public static Map<String, Object> updateContact(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String contactPartyId = (String) context.get("partyId");

        // make sure userLogin has CRMSFA_CONTACT_UPDATE permission for this contact
        if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_CONTACT", "_UPDATE", userLogin, contactPartyId)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
        }
        try {
            // update the Party and Person
            Map<String, Object> input = UtilMisc.<String, Object>toMap("partyId", contactPartyId, "firstName", context.get("firstName"), "lastName", context.get("lastName"));
            input.put("firstNameLocal", context.get("firstNameLocal"));
            input.put("lastNameLocal", context.get("lastNameLocal"));
            input.put("personalTitle", context.get("personalTitle"));
            input.put("preferredCurrencyUomId", context.get("preferredCurrencyUomId"));
            input.put("description", context.get("description"));
            input.put("birthDate", context.get("birthDate"));
            input.put("userLogin", userLogin);
            Map<String, Object> serviceResults = dispatcher.runSync("updatePerson", input);
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorUpdateContactFail", locale, MODULE);
            }

            // update PartySupplementalData
            GenericValue partyData = delegator.findByPrimaryKey("PartySupplementalData", UtilMisc.toMap("partyId", contactPartyId));
            if (partyData == null) {
                // create a new one
                partyData = delegator.makeValue("PartySupplementalData", UtilMisc.toMap("partyId", contactPartyId));
                partyData.create();
            }
            partyData.setNonPKFields(context);
            partyData.store();

        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorUpdateContactFail", locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorUpdateContactFail", locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> assignContactToAccount(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String contactPartyId = (String) context.get("contactPartyId");
        String accountPartyId = (String) context.get("accountPartyId");

        try {
            // check if this contact is already a contact of this account
            EntityCondition searchConditions = EntityCondition.makeCondition(EntityOperator.AND,
                        EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, contactPartyId),
                        EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, accountPartyId),
                        EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.EQUALS, "CONTACT"),
                        EntityCondition.makeCondition("roleTypeIdTo", EntityOperator.EQUALS, "ACCOUNT"),
                        EntityCondition.makeCondition("partyRelationshipTypeId", EntityOperator.EQUALS, "CONTACT_REL_INV"),
                        EntityUtil.getFilterByDateExpr());
            List<GenericValue> existingRelationships = delegator.findByCondition("PartyRelationship", searchConditions, null, null);
            if (existingRelationships.size() > 0) {
                return UtilMessage.createAndLogServiceError("CrmErrorContactAlreadyAssociatedToAccount", locale, MODULE);
            }

            // check if userLogin has CRMSFA_ACCOUNT_UPDATE permission for this account
            if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_ACCOUNT", "_UPDATE", userLogin, accountPartyId)) {
                return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
            }
            // create the party relationship between the Contact and the Account
            PartyHelper.createNewPartyToRelationship(accountPartyId, contactPartyId, "CONTACT", "CONTACT_REL_INV",
                    null, UtilMisc.toList("ACCOUNT"), false, userLogin, delegator, dispatcher);

        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorAssignContactToAccountFail", locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorAssignContactToAccountFail", locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> reassignContactResponsibleParty(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String contactPartyId = (String) context.get("contactPartyId");
        String newPartyId = (String) context.get("newPartyId");

        // ensure reassign permission on this contact
        if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_CONTACT", "_REASSIGN", userLogin, contactPartyId)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
        }
        try {
            // we need to expire all the active ASSIGNED_TO relationships from the contact party to the new owner party
            List<GenericValue> activeAssignedToRelationships = EntityUtil.filterByDate(
                   delegator.findByAnd("PartyRelationship", UtilMisc.toMap(
                                                              "partyIdFrom", contactPartyId,
                                                              "roleTypeIdFrom", "CONTACT",
                                                              "partyIdTo", newPartyId,
                                                              "partyRelationshipTypeId", "ASSIGNED_TO")));
            PartyHelper.expirePartyRelationships(activeAssignedToRelationships, UtilDateTime.nowTimestamp(), dispatcher, userLogin);

            // reassign relationship using a helper method
            boolean result = createResponsibleContactRelationshipForParty(newPartyId, contactPartyId, userLogin, delegator, dispatcher);
            if (!result) {
                return UtilMessage.createAndLogServiceError("CrmErrorReassignFail", locale, MODULE);
            }
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorReassignFail", locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorReassignFail", locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> removeContactFromAccount(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String contactPartyId = (String) context.get("contactPartyId");
        String accountPartyId = (String) context.get("accountPartyId");

        // ensure update permission on account
        if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_ACCOUNT", "_UPDATE", userLogin, accountPartyId)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
        }
        try {
            // find and expire all contact relationships between the contact and account
            List<GenericValue> relations = delegator.findByAnd("PartyRelationship", UtilMisc.toMap("partyIdTo", accountPartyId,
                        "partyIdFrom", contactPartyId, "partyRelationshipTypeId", PartyRelationshipTypeConstants.CONTACT_REL_INV));
            PartyHelper.expirePartyRelationships(relations, UtilDateTime.nowTimestamp(), dispatcher, userLogin);
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorRemoveContactFail", locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorRemoveContactFail", locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> deactivateContact(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        // what contact we're expiring
        String contactPartyId = (String) context.get("partyId");

        // check that userLogin has CRMSFA_CONTACT_DEACTIVATE permission for this contact
        if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_CONTACT", "_DEACTIVATE", userLogin, contactPartyId)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
        }

        // when to expire the contact
        Timestamp expireDate = (Timestamp) context.get("expireDate");
        if (expireDate == null) {
            expireDate = UtilDateTime.nowTimestamp();
        }

        // in order to deactivate a contact, we expire all party relationships on the expire date
        try {
            List<GenericValue> partyRelationships = delegator.findByAnd("PartyRelationship", UtilMisc.toMap("partyIdFrom", contactPartyId, "roleTypeIdFrom", "CONTACT"));
            PartyHelper.expirePartyRelationships(partyRelationships, expireDate, dispatcher, userLogin);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorDeactivateContactFail", locale, MODULE);
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorDeactivateContactFail", locale, MODULE);
        }

        // set the party statusId to PARTY_DISABLED and register the PartyDeactivation
        try {
            GenericValue contactParty = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", contactPartyId));
            contactParty.put("statusId", "PARTY_DISABLED");
            contactParty.store();

            delegator.create("PartyDeactivation", UtilMisc.toMap("partyId", contactPartyId, "deactivationTimestamp", expireDate));
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorDeactivateAccountFail", locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }


    /**************************************************************************/
    /**                            Helper Methods                           ***/
    /**************************************************************************/

    /**
     * Creates an contact relationship of a given type for the given party and removes all previous relationships of that type.
     * This method helps avoid semantic mistakes and typos from the repeated use of this code pattern.
     */
    public static boolean createResponsibleContactRelationshipForParty(String partyId, String contactPartyId,
            GenericValue userLogin, Delegator delegator, LocalDispatcher dispatcher)
        throws GenericServiceException, GenericEntityException {
        return PartyHelper.createNewPartyToRelationship(partyId, contactPartyId, "CONTACT", "RESPONSIBLE_FOR",
                "CONTACT_OWNER", PartyHelper.TEAM_MEMBER_ROLES, true, userLogin, delegator, dispatcher);
    }
}
