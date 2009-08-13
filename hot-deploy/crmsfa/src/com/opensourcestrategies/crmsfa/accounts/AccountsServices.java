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
/* Copyright (c) 2005-2006 Open Source Strategies, Inc. */

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
package com.opensourcestrategies.crmsfa.accounts;

import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilMessage;

import com.opensourcestrategies.crmsfa.party.PartyHelper;
import com.opensourcestrategies.crmsfa.security.CrmsfaSecurity;

/**
 * Accounts services. The service documentation is in services_accounts.xml.
 *
 * @author     <a href="mailto:leon@opensourcestrategies.com">Leon Torres</a>
 * @version    $Rev: 488 $
 */

public class AccountsServices {

    public static final String module = AccountsServices.class.getName();
    public static final String notificationResource = "notification";

    public static Map createAccount(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        if (!security.hasPermission("CRMSFA_ACCOUNT_CREATE", userLogin)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, module);
        }

        // the net result of creating an account is the generation of an Account partyId
        String accountPartyId = (String) context.get("partyId");
        try {
            // make sure user has the right crmsfa roles defined.  otherwise the account will be created as deactivated.
            if (UtilValidate.isEmpty(PartyHelper.getFirstValidTeamMemberRoleTypeId(userLogin.getString("partyId"), delegator))) {
                return UtilMessage.createAndLogServiceError("CrmError_NoRoleForCreateParty", UtilMisc.toMap("userPartyName", org.ofbiz.party.party.PartyHelper.getPartyName(delegator, userLogin.getString("partyId"), false), "requiredRoleTypes", PartyHelper.TEAM_MEMBER_ROLES), locale, module);
            }

            // if we're given the partyId to create, then verify it is free to use
            if (accountPartyId != null) {
                Map findMap =  UtilMisc.toMap("partyId", accountPartyId);
                GenericValue party = delegator.findByPrimaryKey("Party", findMap);
                if (party != null) {
                    // TODO maybe a more specific message such as "Account already exists"
                    return UtilMessage.createAndLogServiceError("person.create.person_exists", findMap, locale, module);
                }
            }

            // create the Party and PartyGroup, which results in a partyId
            Map input = UtilMisc.toMap("groupName", context.get("accountName"), "groupNameLocal", context.get("groupNameLocal"),
                    "officeSiteName", context.get("officeSiteName"), "description", context.get("description"), "partyId", accountPartyId);
            Map serviceResults = dispatcher.runSync("createPartyGroup", input);
            if (ServiceUtil.isError(serviceResults)) {
                return serviceResults;
            }
            accountPartyId = (String) serviceResults.get("partyId");

            // create a PartyRole for the resulting Account partyId with roleTypeId = ACCOUNT
            serviceResults = dispatcher.runSync("createPartyRole", UtilMisc.toMap("partyId", accountPartyId, "roleTypeId", "ACCOUNT", "userLogin", userLogin));
            if (ServiceUtil.isError(serviceResults)) {
                return serviceResults;
            }

            // create PartySupplementalData
            GenericValue partyData = delegator.makeValue("PartySupplementalData", UtilMisc.toMap("partyId", accountPartyId));
            partyData.setNonPKFields(context);
            partyData.create();

            // create a unique party relationship between the userLogin and the Account with partyRelationshipTypeId RESPONSIBLE_FOR
            createResponsibleAccountRelationshipForParty(userLogin.getString("partyId"), accountPartyId, userLogin, delegator, dispatcher);

            // if initial data source is provided, add it
            String dataSourceId = (String) context.get("dataSourceId");
            if (dataSourceId != null) {
                serviceResults = dispatcher.runSync("crmsfa.addAccountDataSource", 
                        UtilMisc.toMap("partyId", accountPartyId, "dataSourceId", dataSourceId, "userLogin", userLogin));
                if (ServiceUtil.isError(serviceResults)) {
                    return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorCreateAccountFail", locale, module);
                }
            }

            // if initial marketing campaign is provided, add it
            String marketingCampaignId = (String) context.get("marketingCampaignId");
            if (marketingCampaignId != null) {
                serviceResults = dispatcher.runSync("crmsfa.addAccountMarketingCampaign",
                        UtilMisc.toMap("partyId", accountPartyId, "marketingCampaignId", marketingCampaignId, "userLogin", userLogin));
                if (ServiceUtil.isError(serviceResults)) {
                    return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorCreateAccountFail", locale, module);
                }
            }
 
            // if there's an initialTeamPartyId, assign the team to the account
            String initialTeamPartyId = (String) context.get("initialTeamPartyId");
            if (initialTeamPartyId != null) {
                serviceResults = dispatcher.runSync("crmsfa.assignTeamToAccount", UtilMisc.toMap("accountPartyId", accountPartyId, 
                            "teamPartyId", initialTeamPartyId, "userLogin", userLogin));
                if (ServiceUtil.isError(serviceResults)) {
                    return serviceResults;
                }
            }

            // create basic contact info
            ModelService service = dctx.getModelService("crmsfa.createBasicContactInfoForParty");
            input = service.makeValid(context, "IN");
            input.put("partyId", accountPartyId);
            serviceResults = dispatcher.runSync(service.name, input);
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorCreateAccountFail", locale, module);
            }

            // Send email re: responsible party to all involved parties
            Map sendEmailResult = dispatcher.runSync("crmsfa.sendAccountResponsibilityNotificationEmails", UtilMisc.toMap("newPartyId", userLogin.getString("partyId"), "accountPartyId", accountPartyId, "userLogin", userLogin));

        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorCreateAccountFail", locale, module);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorCreateAccountFail", locale, module);
        }

        // return the partyId of the newly created Account
        Map results = ServiceUtil.returnSuccess();
        results.put("partyId", accountPartyId);
        return results;
    }

    public static Map updateAccount(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        String accountPartyId = (String) context.get("partyId");

        // make sure userLogin has CRMSFA_ACCOUNT_UPDATE permission for this account
        if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_ACCOUNT", "_UPDATE", userLogin, accountPartyId)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, module);
        }
        try {
            // update the Party and PartyGroup
            Map input = UtilMisc.toMap("groupName", context.get("accountName"), "groupNameLocal", context.get("groupNameLocal"), 
                    "officeSiteName", context.get("officeSiteName"), "description", context.get("description"));
            input.put("partyId", accountPartyId);
            input.put("userLogin", userLogin);
            Map serviceResults = dispatcher.runSync("updatePartyGroup", input);
            if (ServiceUtil.isError(serviceResults)) {
                return serviceResults;
            }

            // update PartySupplementalData
            GenericValue partyData = delegator.findByPrimaryKey("PartySupplementalData", UtilMisc.toMap("partyId", accountPartyId));
            if (partyData == null) {
                // create a new one
                partyData = delegator.makeValue("PartySupplementalData", UtilMisc.toMap("partyId", accountPartyId));
                partyData.create();
            }
            partyData.setNonPKFields(context);
            partyData.store();

        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorUpdateAccountFail", locale, module);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorUpdateAccountFail", locale, module);
        }
        return ServiceUtil.returnSuccess();
    }


    public static Map deactivateAccount(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        // what account we're expiring
        String accountPartyId = (String) context.get("partyId");

        // check that userLogin has CRMSFA_ACCOUNT_DEACTIVATE permission for this account
        if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_ACCOUNT", "_DEACTIVATE", userLogin, accountPartyId)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, module);
        }

        // when to expire the account
        Timestamp expireDate = (Timestamp) context.get("expireDate");
        if (expireDate == null) {
            expireDate = UtilDateTime.nowTimestamp();
        }

        // in order to deactivate an account, we expire all party relationships on the expire date
        try {
            List partyRelationships = delegator.findByAnd("PartyRelationship", UtilMisc.toMap("partyIdFrom", accountPartyId, "roleTypeIdFrom", "ACCOUNT"));
            PartyHelper.expirePartyRelationships(partyRelationships, expireDate, dispatcher, userLogin);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorDeactivateAccountFail", locale, module);
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorDeactivateAccountFail", locale, module);
        }

        // set the account party statusId to PARTY_DISABLED and register PartyDeactivation
        // TODO: improve this to support disabling on a future expireDate
        try {
            GenericValue accountParty = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", accountPartyId));
            accountParty.put("statusId", "PARTY_DISABLED");
            accountParty.store();
            
            delegator.create("PartyDeactivation", UtilMisc.toMap("partyId", accountPartyId, "deactivationTimestamp", expireDate));            
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorDeactivateAccountFail", locale, module);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map reassignAccountResponsibleParty(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        String accountPartyId = (String) context.get("accountPartyId");
        String newPartyId = (String) context.get("newPartyId");

        // ensure reassign permission on this account
        if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_ACCOUNT", "_REASSIGN", userLogin, accountPartyId)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, module);
        }
        try {
            // reassign relationship using a helper method
            boolean result = createResponsibleAccountRelationshipForParty(newPartyId, accountPartyId, userLogin, delegator, dispatcher);
            if (result == false) {
                return UtilMessage.createAndLogServiceError("CrmErrorReassignFail", locale, module);
            }
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorReassignFail", locale, module);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorReassignFail", locale, module);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     *  Prepares context for crmsfa.sendCrmNotificationEmails service with email subject, body parameters, and list of parties to email. 
     */
    public static Map sendAccountResponsibilityNotificationEmails(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String newPartyId = (String) context.get("newPartyId");
        String accountPartyId = (String) context.get("accountPartyId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        try {

            String newPartyName = org.ofbiz.party.party.PartyHelper.getPartyName(delegator, newPartyId, false);
            String accountPartyName = org.ofbiz.party.party.PartyHelper.getPartyName(delegator, accountPartyId, false);

            // Get all team members for account
            List teamMemberRelations = delegator.findByAnd("PartyToSummaryByRelationship", UtilMisc.toMap("partyIdFrom", accountPartyId, "roleTypeIdFrom", "ACCOUNT", "partyRelationshipTypeId", "ASSIGNED_TO"));
            teamMemberRelations = EntityUtil.filterByDate(teamMemberRelations);
            List teamMembers = EntityUtil.getFieldListFromEntityList(teamMemberRelations, "partyIdTo", true);
            teamMembers.add(newPartyId);
            
            Map messageMap = UtilMisc.toMap("newPartyId", newPartyId, "newPartyName", newPartyName, "accountPartyId", accountPartyId, "accountPartyName", accountPartyName);
            String url = UtilProperties.getMessage(notificationResource, "crmsfa.url.account", messageMap, locale);
            messageMap.put("url", url);
            String subject = UtilProperties.getMessage(notificationResource, "subject.account.responsible", messageMap, locale);
            
            Map bodyParameters = UtilMisc.toMap("eventType", "account");
            bodyParameters.putAll(messageMap);

            Map sendEmailsResult = dispatcher.runSync("crmsfa.sendCrmNotificationEmails", UtilMisc.toMap("notifyPartyIds", teamMembers, "eventType", "account.responsible", "subject", subject, "bodyParameters", bodyParameters, "userLogin", userLogin));
            if (ServiceUtil.isError(sendEmailsResult)) {
                return sendEmailsResult; 
            }
        } catch (GenericEntityException ex) {
            return UtilMessage.createAndLogServiceError(ex, locale, module);
        } catch (GenericServiceException ex) {
            return UtilMessage.createAndLogServiceError(ex, locale, module);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     *  Prepares context for crmsfa.sendCrmNotificationEmails service with email subject, body parameters, and list of parties to email. 
     */
    public static Map sendAccountTeamMemberNotificationEmails(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String teamMemberPartyId = (String) context.get("teamMemberPartyId");
        String accountTeamPartyId = (String) context.get("accountTeamPartyId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        try {

            String teamMemberPartyName = org.ofbiz.party.party.PartyHelper.getPartyName(delegator, teamMemberPartyId, false);
            String accountPartyName = org.ofbiz.party.party.PartyHelper.getPartyName(delegator, accountTeamPartyId, false);

            // Get all team members for account
            List teamMemberRelations = delegator.findByAnd("PartyToSummaryByRelationship", UtilMisc.toMap("partyIdFrom", accountTeamPartyId, "roleTypeIdFrom", "ACCOUNT", "partyRelationshipTypeId", "ASSIGNED_TO"));
            teamMemberRelations = EntityUtil.filterByDate(teamMemberRelations);
            List teamMembers = EntityUtil.getFieldListFromEntityList(teamMemberRelations, "partyIdTo", true);

            // Include the current responsible party for the account
            GenericValue responsibleParty = PartyHelper.getCurrentResponsibleParty(accountTeamPartyId, "ACCOUNT", delegator);
            if (responsibleParty != null && responsibleParty.getString("partyId") != null) {
                teamMembers.add(responsibleParty.getString("partyId"));
            }
            
            String eventType = null;
            if (teamMembers.contains(teamMemberPartyId)) {
                eventType = "account.addParty";
            } else {
                eventType = "account.removeParty";
    
                // The party who was just removed should get an email about it, so add them back in
                teamMembers.add(teamMemberPartyId);
            }

            Map messageMap = UtilMisc.toMap("teamMemberPartyId", teamMemberPartyId, "teamMemberPartyName", teamMemberPartyName, "accountPartyId", accountTeamPartyId, "accountPartyName", accountPartyName);
            String url = UtilProperties.getMessage(notificationResource, "crmsfa.url." + eventType, messageMap, locale);
            messageMap.put("url", url);
            String subject = UtilProperties.getMessage(notificationResource, "subject." + eventType, messageMap, locale);
            
            Map bodyParameters = UtilMisc.toMap("eventType", eventType);
            bodyParameters.putAll(messageMap);

            Map sendEmailsResult = dispatcher.runSync("crmsfa.sendCrmNotificationEmails", UtilMisc.toMap("notifyPartyIds", teamMembers, "eventType", eventType, "subject", subject, "bodyParameters", bodyParameters, "userLogin", userLogin));
            if (ServiceUtil.isError(sendEmailsResult)) {
                return sendEmailsResult; 
            }
        } catch (GenericEntityException ex) {
            return UtilMessage.createAndLogServiceError(ex, locale, module);
        } catch (GenericServiceException ex) {
            return UtilMessage.createAndLogServiceError(ex, locale, module);
        }
        return ServiceUtil.returnSuccess();
    }

    /**************************************************************************/
    /**                            Helper Methods                           ***/
    /**************************************************************************/

    /**
     * Creates an account relationship of a given type for the given party and removes all previous relationships of that type. 
     * This method helps avoid semantic mistakes and typos from the repeated use of this code pattern.
     */
    public static boolean createResponsibleAccountRelationshipForParty(String partyId, String accountPartyId,  
            GenericValue userLogin, GenericDelegator delegator, LocalDispatcher dispatcher)
        throws GenericServiceException, GenericEntityException {
        return PartyHelper.createNewPartyToRelationship(partyId, accountPartyId, "ACCOUNT", "RESPONSIBLE_FOR",
                "ACCOUNT_OWNER", PartyHelper.TEAM_MEMBER_ROLES, true, userLogin, delegator, dispatcher);
    }

}
