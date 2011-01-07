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
package com.opensourcestrategies.crmsfa.accounts;

import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.base.entities.PartyGroup;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.domain.party.Party;
import org.opentaps.domain.party.PartyDomainInterface;
import org.opentaps.domain.party.PartyRepositoryInterface;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.repository.RepositoryException;

import com.opensourcestrategies.crmsfa.party.PartyHelper;
import com.opensourcestrategies.crmsfa.security.CrmsfaSecurity;

/**
 * Accounts services. The service documentation is in services_accounts.xml.
 *
 * @author     <a href="mailto:leon@opensourcestrategies.com">Leon Torres</a>
 * @version    $Rev: 488 $
 */
public final class AccountsServices {

    private AccountsServices() { }

    private static final String MODULE = AccountsServices.class.getName();
    public static final String notificationResource = "notification";

    public static Map<String, Object> createAccount(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        String accountName = (String) context.get("accountName");
        // the field that flag if force complete to create contact even existing same name already
        String forceComplete = context.get("forceComplete") == null ? "N" : (String) context.get("forceComplete");
        if (!security.hasPermission("CRMSFA_ACCOUNT_CREATE", userLogin)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
        }

        // the net result of creating an account is the generation of an Account partyId
        String accountPartyId = (String) context.get("partyId");
        try {
            // make sure user has the right crmsfa roles defined.  otherwise the account will be created as deactivated.
            if (UtilValidate.isEmpty(PartyHelper.getFirstValidTeamMemberRoleTypeId(userLogin.getString("partyId"), delegator))) {
                return UtilMessage.createAndLogServiceError("CrmError_NoRoleForCreateParty", UtilMisc.toMap("userPartyName", org.ofbiz.party.party.PartyHelper.getPartyName(delegator, userLogin.getString("partyId"), false), "requiredRoleTypes", PartyHelper.TEAM_MEMBER_ROLES), locale, MODULE);
            }

            // if we're given the partyId to create, then verify it is free to use
            if (accountPartyId != null) {
                Map<String, Object> findMap =  UtilMisc.<String, Object>toMap("partyId", accountPartyId);
                GenericValue party = delegator.findByPrimaryKey("Party", findMap);
                if (party != null) {
                    // TODO maybe a more specific message such as "Account already exists"
                    return UtilMessage.createAndLogServiceError("person.create.person_exists", findMap, locale, MODULE);
                }
            }

            // verify account name is use already
            if (!"Y".equals(forceComplete)) {
                DomainsLoader domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(userLogin));
                PartyDomainInterface partyDomain = domainLoader.loadDomainsDirectory().getPartyDomain();
                PartyRepositoryInterface repo = partyDomain.getPartyRepository();
                Set<PartyGroup> duplicateAccountsWithName = repo.getPartyGroupByGroupNameAndRoleType(accountName, "ACCOUNT");
                // if existing the account which have same account name, then return the conflict account and error message
                if (duplicateAccountsWithName.size() > 0 && !"Y".equals(forceComplete)) {
                    PartyGroup partyGroup = duplicateAccountsWithName.iterator().next();
                    Map results = ServiceUtil.returnError(UtilMessage.expandLabel("CrmCreateAccountDuplicateCheckFail", UtilMisc.toMap("partyId", partyGroup.getPartyId()), locale));
                    results.put("duplicateAccountsWithName", duplicateAccountsWithName);
                    return results;
                }
            }

            // create the Party and PartyGroup, which results in a partyId
            Map<String, Object> input = UtilMisc.toMap("groupName", context.get("accountName"), "groupNameLocal", context.get("groupNameLocal"),
                    "officeSiteName", context.get("officeSiteName"), "description", context.get("description"), "partyId", accountPartyId);
            Map<String, Object> serviceResults = dispatcher.runSync("createPartyGroup", input);
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
                    return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorCreateAccountFail", locale, MODULE);
                }
            }

            // if initial marketing campaign is provided, add it
            String marketingCampaignId = (String) context.get("marketingCampaignId");
            if (marketingCampaignId != null) {
                serviceResults = dispatcher.runSync("crmsfa.addAccountMarketingCampaign",
                        UtilMisc.toMap("partyId", accountPartyId, "marketingCampaignId", marketingCampaignId, "userLogin", userLogin));
                if (ServiceUtil.isError(serviceResults)) {
                    return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorCreateAccountFail", locale, MODULE);
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
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorCreateAccountFail", locale, MODULE);
            }

            // Send email re: responsible party to all involved parties
            dispatcher.runSync("crmsfa.sendAccountResponsibilityNotificationEmails", UtilMisc.toMap("newPartyId", userLogin.getString("partyId"), "accountPartyId", accountPartyId, "userLogin", userLogin));

        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorCreateAccountFail", locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorCreateAccountFail", locale, MODULE);
        } catch (RepositoryException e) {
        	return UtilMessage.createAndLogServiceError(e, "CrmErrorCreateAccountFail", locale, MODULE);
		}

        // return the partyId of the newly created Account
        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put("partyId", accountPartyId);
        return results;
    }

    public static Map<String, Object> updateAccount(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String accountPartyId = (String) context.get("partyId");

        // make sure userLogin has CRMSFA_ACCOUNT_UPDATE permission for this account
        if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_ACCOUNT", "_UPDATE", userLogin, accountPartyId)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
        }
        try {
            // update the Party and PartyGroup
            Map<String, Object> input = UtilMisc.toMap("groupName", context.get("accountName"), "groupNameLocal", context.get("groupNameLocal"),
                    "officeSiteName", context.get("officeSiteName"), "description", context.get("description"));
            input.put("partyId", accountPartyId);
            input.put("userLogin", userLogin);
            Map<String, Object> serviceResults = dispatcher.runSync("updatePartyGroup", input);
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
            return UtilMessage.createAndLogServiceError(e, "CrmErrorUpdateAccountFail", locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorUpdateAccountFail", locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }


    public static Map<String, Object> deactivateAccount(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        // what account we're expiring
        String accountPartyId = (String) context.get("partyId");

        // check that userLogin has CRMSFA_ACCOUNT_DEACTIVATE permission for this account
        if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_ACCOUNT", "_DEACTIVATE", userLogin, accountPartyId)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
        }

        // when to expire the account
        Timestamp expireDate = (Timestamp) context.get("expireDate");
        if (expireDate == null) {
            expireDate = UtilDateTime.nowTimestamp();
        }

        // in order to deactivate an account, we expire all party relationships on the expire date
        try {
            List<GenericValue> partyRelationships = delegator.findByAnd("PartyRelationship", UtilMisc.toMap("partyIdFrom", accountPartyId, "roleTypeIdFrom", "ACCOUNT"));
            PartyHelper.expirePartyRelationships(partyRelationships, expireDate, dispatcher, userLogin);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorDeactivateAccountFail", locale, MODULE);
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorDeactivateAccountFail", locale, MODULE);
        }

        // set the account party statusId to PARTY_DISABLED and register PartyDeactivation
        // TODO: improve this to support disabling on a future expireDate
        try {
            GenericValue accountParty = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", accountPartyId));
            accountParty.put("statusId", "PARTY_DISABLED");
            accountParty.store();

            delegator.create("PartyDeactivation", UtilMisc.toMap("partyId", accountPartyId, "deactivationTimestamp", expireDate));
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorDeactivateAccountFail", locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> reassignAccountResponsibleParty(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String accountPartyId = (String) context.get("accountPartyId");
        String newPartyId = (String) context.get("newPartyId");

        // ensure reassign permission on this account
        if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_ACCOUNT", "_REASSIGN", userLogin, accountPartyId)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
        }
        try {
            // reassign relationship using a helper method
            boolean result = createResponsibleAccountRelationshipForParty(newPartyId, accountPartyId, userLogin, delegator, dispatcher);
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

    /**
     *  Prepares context for crmsfa.sendCrmNotificationEmails service with email subject, body parameters, and list of parties to email.
     */
    public static Map<String, Object> sendAccountResponsibilityNotificationEmails(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String newPartyId = (String) context.get("newPartyId");
        String accountPartyId = (String) context.get("accountPartyId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        try {

            String newPartyName = org.ofbiz.party.party.PartyHelper.getPartyName(delegator, newPartyId, false);
            String accountPartyName = org.ofbiz.party.party.PartyHelper.getPartyName(delegator, accountPartyId, false);

            // Get all team members for account
            List<GenericValue> teamMemberRelations = delegator.findByAnd("PartyToSummaryByRelationship", UtilMisc.toMap("partyIdFrom", accountPartyId, "roleTypeIdFrom", "ACCOUNT", "partyRelationshipTypeId", "ASSIGNED_TO"));
            teamMemberRelations = EntityUtil.filterByDate(teamMemberRelations);
            List<String> teamMembers = EntityUtil.getFieldListFromEntityList(teamMemberRelations, "partyIdTo", true);
            teamMembers.add(newPartyId);

            Map<String, String> messageMap = UtilMisc.toMap("newPartyId", newPartyId, "newPartyName", newPartyName, "accountPartyId", accountPartyId, "accountPartyName", accountPartyName);
            String url = UtilProperties.getMessage(notificationResource, "crmsfa.url.account", messageMap, locale);
            messageMap.put("url", url);
            String subject = UtilProperties.getMessage(notificationResource, "subject.account.responsible", messageMap, locale);

            Map<String, String> bodyParameters = UtilMisc.toMap("eventType", "account");
            bodyParameters.putAll(messageMap);

            Map<String, Object> sendEmailsResult = dispatcher.runSync("crmsfa.sendCrmNotificationEmails", UtilMisc.toMap("notifyPartyIds", teamMembers, "eventType", "account.responsible", "subject", subject, "bodyParameters", bodyParameters, "userLogin", userLogin));
            if (ServiceUtil.isError(sendEmailsResult)) {
                return sendEmailsResult;
            }
        } catch (GenericEntityException ex) {
            return UtilMessage.createAndLogServiceError(ex, locale, MODULE);
        } catch (GenericServiceException ex) {
            return UtilMessage.createAndLogServiceError(ex, locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     *  Prepares context for crmsfa.sendCrmNotificationEmails service with email subject, body parameters, and list of parties to email.
     */
    public static Map<String, Object> sendAccountTeamMemberNotificationEmails(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String teamMemberPartyId = (String) context.get("teamMemberPartyId");
        String accountTeamPartyId = (String) context.get("accountTeamPartyId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        try {

            String teamMemberPartyName = org.ofbiz.party.party.PartyHelper.getPartyName(delegator, teamMemberPartyId, false);
            String accountPartyName = org.ofbiz.party.party.PartyHelper.getPartyName(delegator, accountTeamPartyId, false);

            // Get all team members for account
            List<GenericValue> teamMemberRelations = delegator.findByAnd("PartyToSummaryByRelationship", UtilMisc.toMap("partyIdFrom", accountTeamPartyId, "roleTypeIdFrom", "ACCOUNT", "partyRelationshipTypeId", "ASSIGNED_TO"));
            teamMemberRelations = EntityUtil.filterByDate(teamMemberRelations);
            List<String> teamMembers = EntityUtil.getFieldListFromEntityList(teamMemberRelations, "partyIdTo", true);

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

            Map<String, String> messageMap = UtilMisc.toMap("teamMemberPartyId", teamMemberPartyId, "teamMemberPartyName", teamMemberPartyName, "accountPartyId", accountTeamPartyId, "accountPartyName", accountPartyName);
            String url = UtilProperties.getMessage(notificationResource, "crmsfa.url." + eventType, messageMap, locale);
            messageMap.put("url", url);
            String subject = UtilProperties.getMessage(notificationResource, "subject." + eventType, messageMap, locale);

            Map<String, String> bodyParameters = UtilMisc.toMap("eventType", eventType);
            bodyParameters.putAll(messageMap);

            Map<String, Object> sendEmailsResult = dispatcher.runSync("crmsfa.sendCrmNotificationEmails", UtilMisc.toMap("notifyPartyIds", teamMembers, "eventType", eventType, "subject", subject, "bodyParameters", bodyParameters, "userLogin", userLogin));
            if (ServiceUtil.isError(sendEmailsResult)) {
                return sendEmailsResult;
            }
        } catch (GenericEntityException ex) {
            return UtilMessage.createAndLogServiceError(ex, locale, MODULE);
        } catch (GenericServiceException ex) {
            return UtilMessage.createAndLogServiceError(ex, locale, MODULE);
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
            GenericValue userLogin, Delegator delegator, LocalDispatcher dispatcher)
        throws GenericServiceException, GenericEntityException {
        return PartyHelper.createNewPartyToRelationship(partyId, accountPartyId, "ACCOUNT", "RESPONSIBLE_FOR",
                "ACCOUNT_OWNER", PartyHelper.TEAM_MEMBER_ROLES, true, userLogin, delegator, dispatcher);
    }

}
