/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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
package com.opensourcestrategies.crmsfa.leads;

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

import com.opensourcestrategies.crmsfa.party.PartyHelper;
import com.opensourcestrategies.crmsfa.security.CrmsfaSecurity;

/**
 * Leads services. The service documentation is in services_leads.xml.
 *
 * @author     <a href="mailto:leon@opensourcestrategies.com">Leon Torres</a>
 */

public class LeadsServices {

    public static final String module = LeadsServices.class.getName();
    public static final String notificationResource = "notification";

    public static Map<String, ?> createLead(DispatchContext dctx, Map<String, ?> context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        if (!security.hasPermission("CRMSFA_LEAD_CREATE", userLogin)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, module);
        }

        try {
            if (UtilValidate.isNotEmpty(context.get("parentPartyId")))
                PartyHelper.isActive((String) context.get("parentPartyId"), delegator);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError("CrmErrorLeadNotFound", UtilMisc.toMap("leadPartyId", context.get("parentPartyId")), locale, module);
        }

        // the net result of creating an lead is the generation of a Lead partyId
        String leadPartyId = null;
        try {
            // make sure user has the right crmsfa roles defined.  otherwise the lead could be created but then once converted the account will be deactivated.
            if (UtilValidate.isEmpty(PartyHelper.getFirstValidTeamMemberRoleTypeId(userLogin.getString("partyId"), delegator))) {
                return UtilMessage.createAndLogServiceError("CrmError_NoRoleForCreateParty", UtilMisc.toMap("userPartyName", org.ofbiz.party.party.PartyHelper.getPartyName(delegator, userLogin.getString("partyId"), false), "requiredRoleTypes", PartyHelper.TEAM_MEMBER_ROLES), locale, module);
            }

            // set statusId is PTYLEAD_ASSIGNED, because we are assigning to the user down below.
            // perhaps a better alternative is to create the lead as NEW, call the reassignLeadResponsibleParty service below, and have it update it to ASSIGNED if not already so.
            String statusId = "PTYLEAD_ASSIGNED";

            // create the Party and Person, which results in a partyId
            Map<String, Object> input = UtilMisc.toMap("firstName", context.get("firstName"), "lastName", context.get("lastName"));
            input.put("firstNameLocal", context.get("firstNameLocal"));
            input.put("lastNameLocal", context.get("lastNameLocal"));
            input.put("personalTitle", context.get("personalTitle"));
            input.put("preferredCurrencyUomId", context.get("currencyUomId"));
            input.put("description", context.get("description"));
            input.put("birthDate", context.get("birthDate"));
            input.put("statusId", statusId); // initial status
            Map<String, Object> serviceResults = dispatcher.runSync("createPerson", input);
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorCreateLeadFail", locale, module);
            }
            leadPartyId = (String) serviceResults.get("partyId");

            // create a PartyRole for the resulting Lead partyId with roleTypeId = PROSPECT
            serviceResults = dispatcher.runSync("createPartyRole", UtilMisc.toMap("partyId", leadPartyId, "roleTypeId", "PROSPECT", "userLogin", userLogin));
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorCreateLeadFail", locale, module);
            }

            // create PartySupplementalData
            GenericValue partyData = delegator.makeValue("PartySupplementalData", UtilMisc.toMap("partyId", leadPartyId));
            partyData.setNonPKFields(context);
            partyData.create();

            // create a party relationship between the userLogin and the Lead with partyRelationshipTypeId RESPONSIBLE_FOR
            PartyHelper.createNewPartyToRelationship(userLogin.getString("partyId"), leadPartyId, "PROSPECT", "RESPONSIBLE_FOR",
                    "LEAD_OWNER", PartyHelper.TEAM_MEMBER_ROLES, true, userLogin, delegator, dispatcher);

            // if initial data source is provided, add it
            String dataSourceId = (String) context.get("dataSourceId");
            if (dataSourceId != null) {
                serviceResults = dispatcher.runSync("crmsfa.addLeadDataSource", 
                        UtilMisc.toMap("partyId", leadPartyId, "dataSourceId", dataSourceId, "userLogin", userLogin));
                if (ServiceUtil.isError(serviceResults)) {
                    return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorCreateLeadFail", locale, module);
                }
            }

            // if initial marketing campaign is provided, add it
            String marketingCampaignId = (String) context.get("marketingCampaignId");
            if (marketingCampaignId != null) {
                serviceResults = dispatcher.runSync("crmsfa.addLeadMarketingCampaign",
                        UtilMisc.toMap("partyId", leadPartyId, "marketingCampaignId", marketingCampaignId, "userLogin", userLogin));
                if (ServiceUtil.isError(serviceResults)) {
                    return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorCreateLeadFail", locale, module);
                }
            }

            // create basic contact info
            ModelService service = dctx.getModelService("crmsfa.createBasicContactInfoForParty");
            input = service.makeValid(context, "IN");
            input.put("partyId", leadPartyId);
            serviceResults = dispatcher.runSync(service.name, input);
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorCreateLeadFail", locale, module);
            }

            // Send email re: responsible party to all involved parties
            dispatcher.runSync("crmsfa.sendLeadNotificationEmails", UtilMisc.toMap("newPartyId", userLogin.getString("partyId"), "leadPartyId", leadPartyId, "userLogin", userLogin));

        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorCreateLeadFail", locale, module);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorCreateLeadFail", locale, module);
        }

        // return the partyId of the newly created Lead
        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put("partyId", leadPartyId);
        return results;
    }

    public static Map<String, ?> updateLead(DispatchContext dctx, Map<String, ?> context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String leadPartyId = (String) context.get("partyId");

        // make sure userLogin has CRMSFA_LEAD_UPDATE permission for this lead
        if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_LEAD", "_UPDATE", userLogin, leadPartyId)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, module);
        }

        try {
            if (UtilValidate.isNotEmpty(context.get("parentPartyId")))
                PartyHelper.isActive((String) context.get("parentPartyId"), delegator);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError("CrmErrorLeadNotFound", UtilMisc.toMap("leadPartyId", context.get("parentPartyId")), locale, module);
        }

        try {
            // get the party
            GenericValue party = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", leadPartyId));
            if (party == null) {
                return UtilMessage.createAndLogServiceError("CrmErrorUpdateLeadFail", locale, module);
            }

            // change status if passed in statusId is different
            String statusId = (String) context.get("statusId");
            if ((statusId != null) && (!statusId.equals(party.getString("statusId")))) {
                Map<String, Object> serviceResults = dispatcher.runSync("setPartyStatus", UtilMisc.toMap("partyId", leadPartyId, "statusId", statusId, "userLogin", userLogin));
                if (ServiceUtil.isError(serviceResults)) {
                    return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorUpdateLeadFail", locale, module);
                }
            } 

            // update the Party and Person
            Map<String, Object> input = UtilMisc.toMap("partyId", leadPartyId, "firstName", context.get("firstName"), "lastName", context.get("lastName"));
            input.put("firstNameLocal", context.get("firstNameLocal"));
            input.put("lastNameLocal", context.get("lastNameLocal"));
            input.put("personalTitle", context.get("personalTitle"));
            input.put("preferredCurrencyUomId", context.get("currencyUomId"));
            input.put("description", context.get("description"));
            input.put("birthDate", context.get("birthDate"));
            input.put("userLogin", userLogin);
            Map<String, Object> serviceResults = dispatcher.runSync("updatePerson", input);
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorUpdateLeadFail", locale, module);
            }

            // update PartySupplementalData
            GenericValue partyData = delegator.findByPrimaryKey("PartySupplementalData", UtilMisc.toMap("partyId", leadPartyId));
            if (partyData == null) {
                // create a new one
                partyData = delegator.makeValue("PartySupplementalData", UtilMisc.toMap("partyId", leadPartyId));
                partyData.create();
            }
            partyData.setNonPKFields(context);
            partyData.store();

        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorUpdateLeadFail", locale, module);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorUpdateLeadFail", locale, module);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, ?> convertLead(DispatchContext dctx, Map<String, ?> context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String leadPartyId = (String) context.get("leadPartyId");
        String accountPartyId = (String) context.get("accountPartyId");

        // make sure userLogin has CRMSFA_LEAD_UPDATE permission for this lead
        if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_LEAD", "_UPDATE", userLogin, leadPartyId)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, module);
        }

        Map<String, Object> input = null;  // used later for service inputs
        try {
            GenericValue lead = delegator.findByPrimaryKey("PartySummaryCRMView", UtilMisc.toMap("partyId", leadPartyId));

            // create a PartyRole of type CONTACT for the lead
            Map<String, Object> serviceResults = dispatcher.runSync("createPartyRole", UtilMisc.toMap("partyId", leadPartyId, "roleTypeId", "CONTACT", "userLogin", userLogin));
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorConvertLeadFail", locale, module);
            }

            // if no account was given, then create an account based on the PartySupplementalData of the lead
            if (accountPartyId == null) {
                input = UtilMisc.toMap("accountName", lead.getString("companyName"), "description", lead.getString("description"), "userLogin", userLogin);
                input.put("parentPartyId", lead.getString("parentPartyId"));
                input.put("annualRevenue", lead.getDouble("annualRevenue"));
                input.put("currencyUomId", lead.getString("currencyUomId"));
                input.put("numberEmployees", lead.getLong("numberEmployees"));
                input.put("industryEnumId", lead.getString("industryEnumId"));
                input.put("ownershipEnumId", lead.getString("ownershipEnumId"));
                input.put("importantNote", lead.getString("importantNote")); // The important note will be stored for account and contact
                input.put("sicCode", lead.getString("sicCode"));
                input.put("tickerSymbol", lead.getString("tickerSymbol"));
                serviceResults = dispatcher.runSync("crmsfa.createAccount", input);
                if (ServiceUtil.isError(serviceResults)) {
                    return serviceResults;
                }  
                accountPartyId = (String) serviceResults.get("partyId");

                // copy all the datasources over to the new account
                List<GenericValue> dataSources = delegator.findByAnd("PartyDataSource", UtilMisc.toMap("partyId", leadPartyId));
                for (GenericValue dataSource : dataSources) {
                    serviceResults = dispatcher.runSync("crmsfa.addAccountDataSource", UtilMisc.toMap("partyId", accountPartyId, 
                            "dataSourceId", dataSource.getString("dataSourceId"), "userLogin", userLogin));
                    if (ServiceUtil.isError(serviceResults)) {
                        return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorConvertLeadFail", locale, module);
                    }
                }

                // copy all the marketing campaigns over to the new account
                List<GenericValue> marketingCampaigns = delegator.findByAnd("MarketingCampaignRole", UtilMisc.toMap("partyId", leadPartyId, "roleTypeId", "PROSPECT"));
                for (GenericValue marketingCampaign : marketingCampaigns) {
                    serviceResults = dispatcher.runSync("crmsfa.addAccountMarketingCampaign", UtilMisc.toMap("partyId", accountPartyId,
                            "marketingCampaignId", marketingCampaign.getString("marketingCampaignId"), "userLogin", userLogin));
                    if (ServiceUtil.isError(serviceResults)) {
                        return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorConvertLeadFail", locale, module);
                    }
                }

                // copy all the contact mechs to the account
                serviceResults = dispatcher.runSync("copyPartyContactMechs", UtilMisc.toMap("partyIdFrom", leadPartyId, "partyIdTo", accountPartyId, "userLogin", userLogin));
                if (ServiceUtil.isError(serviceResults)) {
                    return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorConvertLeadFail", locale, module);
                }
            }

            // erase (null out) the PartySupplementalData fields from the lead
            GenericValue leadSupplementalData = delegator.findByPrimaryKey("PartySupplementalData", UtilMisc.toMap("partyId", leadPartyId));
            leadSupplementalData.set("parentPartyId", null);
            leadSupplementalData.set("annualRevenue", null);
            leadSupplementalData.set("currencyUomId", null);
            leadSupplementalData.set("numberEmployees", null);
            leadSupplementalData.set("industryEnumId", null);
            leadSupplementalData.set("ownershipEnumId", null);
            leadSupplementalData.set("sicCode", null);
            leadSupplementalData.set("tickerSymbol", null);
            leadSupplementalData.store();

            // assign the lead, who is now a contact, to the account
            input = UtilMisc.toMap("contactPartyId", leadPartyId, "accountPartyId", accountPartyId, "userLogin", userLogin);
            serviceResults = dispatcher.runSync("crmsfa.assignContactToAccount", input);
            if (ServiceUtil.isError(serviceResults)) {
                return serviceResults;
            }

            // expire all lead party relationships (roleTypeIdFrom = PROSPECT)
            List<GenericValue> partyRelationships = delegator.findByAnd("PartyRelationship", UtilMisc.toMap("partyIdFrom", leadPartyId, "roleTypeIdFrom", "PROSPECT"));
            PartyHelper.expirePartyRelationships(partyRelationships, UtilDateTime.nowTimestamp(), dispatcher, userLogin);

            // make the userLogin a RESPONSIBLE_FOR CONTACT_OWNER of the CONTACT
            PartyHelper.createNewPartyToRelationship(userLogin.getString("partyId"), leadPartyId, "CONTACT", "RESPONSIBLE_FOR", "CONTACT_OWNER", 
                    PartyHelper.TEAM_MEMBER_ROLES, true, userLogin, delegator, dispatcher);

            // now we need to assign the account and contact to the lead's work efforts and expire all the lead ones
            List<GenericValue> associations = EntityUtil.filterByDate(delegator.findByAnd("WorkEffortPartyAssignment", UtilMisc.toMap("partyId", leadPartyId)));
            for (GenericValue wepa : associations) {
                ModelService service = dctx.getModelService("assignPartyToWorkEffort");
                input = service.makeValid(wepa, "IN");
                input.put("userLogin", userLogin);

                // expire the current lead association (done by hand because service is suspect)
                wepa.set("thruDate", UtilDateTime.nowTimestamp());
                wepa.store();

                // assign the account to the work effort
                input.put("partyId", accountPartyId);
                input.put("fromDate", null);
                input.put("thruDate", null);
                input.put("roleTypeId", "ACCOUNT");
                serviceResults = dispatcher.runSync("assignPartyToWorkEffort", input);
                if (ServiceUtil.isError(serviceResults)) {
                    return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorConvertLeadFail", locale, module);
                }

                // assign the contact to the work effort
                input.put("partyId", leadPartyId);
                input.put("fromDate", null);
                input.put("thruDate", null);
                input.put("roleTypeId", "CONTACT");
                serviceResults = dispatcher.runSync("assignPartyToWorkEffort", input);
                if (ServiceUtil.isError(serviceResults)) {
                    return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorConvertLeadFail", locale, module);
                }
            }

            // opportunities assigned to the lead have to be updated to refer to both contact and account
            List<GenericValue> oppRoles = delegator.findByAnd("SalesOpportunityRole", UtilMisc.toMap("partyId", leadPartyId, "roleTypeId", "PROSPECT"));
            for (GenericValue oppRole : oppRoles) {
                // create a CONTACT role using the leadPartyId
                input = UtilMisc.toMap("partyId", leadPartyId, "salesOpportunityId", oppRole.get("salesOpportunityId"), "roleTypeId", "CONTACT");
                GenericValue contactOppRole = delegator.makeValue("SalesOpportunityRole", input);
                contactOppRole.create();

                // create an ACCOUNT role for the new accountPartyId
                input = UtilMisc.toMap("partyId", accountPartyId, "salesOpportunityId", oppRole.get("salesOpportunityId"), "roleTypeId", "ACCOUNT");
                GenericValue accountOppRole = delegator.makeValue("SalesOpportunityRole", input);
                accountOppRole.create();

                // delete the PROSPECT role
                oppRole.remove();
            }

            // associate any lead files and bookmarks with both account and contact
            List<EntityCondition> conditions = UtilMisc.toList(
                    EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, leadPartyId),
                    EntityCondition.makeCondition("roleTypeId", EntityOperator.EQUALS, "PROSPECT"),
                    EntityUtil.getFilterByDateExpr()
            );
            List<GenericValue> contentRoles = delegator.findByAnd("ContentRole", conditions);
            for (GenericValue contentRole : contentRoles) {
                contentRole.set("thruDate", UtilDateTime.nowTimestamp());
                contentRole.store();

                GenericValue contactContentRole = delegator.makeValue("ContentRole");
                contactContentRole.set("partyId", leadPartyId);
                contactContentRole.set("contentId", contentRole.get("contentId"));
                contactContentRole.set("roleTypeId", "CONTACT");
                contactContentRole.set("fromDate", UtilDateTime.nowTimestamp());
                contactContentRole.create();

                GenericValue accountContent = delegator.makeValue("PartyContent");
                accountContent.set("partyId", accountPartyId);
                accountContent.set("contentId", contentRole.get("contentId"));
                accountContent.set("contentPurposeEnumId", "PTYCNT_CRMSFA");
                accountContent.set("partyContentTypeId", "USERDEF");
                accountContent.set("fromDate", UtilDateTime.nowTimestamp());
                accountContent.create();

                GenericValue accountContentRole = delegator.makeValue("ContentRole");
                accountContentRole.set("partyId", accountPartyId);
                accountContentRole.set("contentId", contentRole.get("contentId"));
                accountContentRole.set("roleTypeId", "ACCOUNT");
                accountContentRole.set("fromDate", UtilDateTime.nowTimestamp());
                accountContentRole.create();
            }

            // set the status of the lead to PTYLEAD_CONVERTED
            serviceResults = dispatcher.runSync("setPartyStatus", UtilMisc.toMap("partyId", leadPartyId, "statusId", "PTYLEAD_CONVERTED", "userLogin", userLogin));
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorConvertLeadFail", locale, module);
            }
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorUpdateLeadFail", locale, module);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorUpdateLeadFail", locale, module);
        }
        // put leadPartyId as partyId
        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put("partyId", leadPartyId);
        return results;
    }

    public static Map<String, ?> reassignLeadResponsibleParty(DispatchContext dctx, Map<String, ?> context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String leadPartyId = (String) context.get("leadPartyId");
        String newPartyId = (String) context.get("newPartyId");

        // ensure reassign permission on this lead
        if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_LEAD", "_REASSIGN", userLogin, leadPartyId)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, module);
        }

        try {
            if (UtilValidate.isNotEmpty(newPartyId))
                PartyHelper.isActive(newPartyId, delegator);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError("CrmErrorLeadNotFound", UtilMisc.toMap("leadPartyId", newPartyId), locale, module);
        }

        try {
            // reassign relationship with this helper method, which expires previous ones
            boolean result = PartyHelper.createNewPartyToRelationship(newPartyId, leadPartyId, "PROSPECT", "RESPONSIBLE_FOR",
                    "LEAD_OWNER", PartyHelper.TEAM_MEMBER_ROLES, true, userLogin, delegator, dispatcher);
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
     * Delete a "new" lead. A new lead has status PTYLEAD_NEW, PTYLEAD_ASSIGNED or PTYLEAD_QUALIFIED.
     * This will physically remove the lead from the Party entity and related entities.
     * If the party was successfully deleted, the method will return a service success, otherwise it
     * will return a service error with the reason.
     */
    public static Map<String, ?> deleteLead(DispatchContext dctx, Map<String, ?> context) {
        GenericDelegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String leadPartyId = (String) context.get("leadPartyId");

        // ensure delete permission on this lead
        if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_LEAD", "_DELETE", userLogin, leadPartyId)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, module);
        }

        try {
            // first ensure the lead is "new" (note that there's no need to check for role because only leads can have these statuses)
            GenericValue lead = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", leadPartyId));
            if (lead == null) {
                return UtilMessage.createAndLogServiceError("Lead [" + leadPartyId + "] not found.",
                        "CrmErrorDeleteLeadFail", locale, module);
            }
            String statusId = lead.getString("statusId");
            if (statusId == null || !(statusId.equals("PTYLEAD_NEW") || statusId.equals("PTYLEAD_ASSIGNED") || statusId.equals("PTYLEAD_QUALIFIED"))) {
                return UtilMessage.createAndLogServiceError("Lead [" + leadPartyId + "] cannot be deleted. Only new, assigned or qualified leads may be deleted.", 
                        "CrmErrorDeleteLeadFail", locale, module);
            }

            // record deletion (note this entity has no primary key on partyId)
            delegator.create("PartyDeactivation", UtilMisc.toMap("partyId", leadPartyId, "deactivationTimestamp", UtilDateTime.nowTimestamp()));

            // delete!
            PartyHelper.deleteCrmParty(leadPartyId, delegator);

        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorDeleteLeadFail", locale, module);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, ?> createCatalogRequestWithSurvey(DispatchContext dctx, Map<String, ?> context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = UtilCommon.getLocale(context);
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        try {
            // if an external customer is filling in the form, then perform this service as the special AUTO_REQ_TAKER Party
            if (userLogin == null) {
                userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "autorqtaker"));
                if (userLogin == null) {
                    return UtilMessage.createAndLogServiceError("CrmErrorAutoReqTakerMissing", locale, module);
                }
            }

            // see if we're creating a Lead association or a Contact
            String companyName = (String) context.get("companyName");
            boolean partyIsLead = (companyName != null && companyName.trim().length() > 0);

            // create a Lead or Contact using the input variables
            String serviceName = (partyIsLead ? "crmsfa.createLead" : "crmsfa.createContact");

            ModelService service = dctx.getModelService(serviceName);
            Map<String, Object> input = service.makeValid(context, "IN");
            input.put("userLogin", userLogin);
            // construct the name on the postal address from the company name/first name/last name from user 
            String firstName = (String) context.get("firstName");
            String lastName = (String) context.get("lastName");
            if (UtilValidate.isNotEmpty(companyName)) {
                input.put("generalToName", companyName);
                input.put("generalAttnName", firstName + " " + lastName);
            } else {
                input.put("generalToName", firstName + " " + lastName);
            }

            Map<String, Object> results = dispatcher.runSync(serviceName, input);
            if (ServiceUtil.isError(results)) return results;
            String partyId = (String) results.get("partyId");
            String fulfillContactMechId = (String) results.get("generalAddressContactMechId");

            // create a PartyNote using comments field
            String comments = (String) context.get("comments");
            if (comments != null) {
                String note = "Catalog Request Comments: " + comments; 
                input = UtilMisc.toMap("partyId", partyId, "note", note, "userLogin", userLogin);
                results = dispatcher.runSync("createPartyNote", input);
                if (ServiceUtil.isError(results)) return results;
            }

            // create a survey response from the answers_ parameters
            service = dctx.getModelService("createSurveyResponse");
            input = service.makeValid(context, "IN");
            input.put("userLogin", userLogin);
            results = dispatcher.runSync("createSurveyResponse", input);
            if (ServiceUtil.isError(results)) return results;
            String surveyId = (String) results.get("surveyId");
            String surveyResponseId = (String) results.get("surveyResponseId");

            // create a CustRequest of the given type for the party with address as the fulfillment location
            String custRequestTypeId = (String) context.get("custRequestTypeId");
            input = UtilMisc.<String, Object>toMap("custRequestTypeId", custRequestTypeId);
            input.put("userLogin", userLogin);
            input.put("fromPartyId", partyId);
            input.put("statusId", "CRQ_SUBMITTED");
            input.put("custRequestDate", UtilDateTime.nowTimestamp());
            input.put("custRequestName", "Catalog Request");
            input.put("description", "Catalog Request for " + context.get("firstName") + " " + context.get("lastName") + " (" + partyId + ")");
            input.put("fulfillContactMechId", fulfillContactMechId);
            results = dispatcher.runSync("createCustRequest", input);
            if (ServiceUtil.isError(results)) return results;
            String custRequestId = (String) results.get("custRequestId");

            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("partyId", partyId);
            result.put("surveyId", surveyId);
            result.put("surveyResponseId", surveyResponseId);
            result.put("custRequestId", custRequestId);
            return result;
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorCreateLeadSurveyResponseFail", locale, module);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorCreateLeadSurveyResponseFail", locale, module);
        }
    }

    /**
     *  Prepares context for crmsfa.sendCrmNotificationEmails service with email subject, body parameters, and list of parties to email. 
     */
    public static Map<String, ?> sendLeadNotificationEmails(DispatchContext dctx, Map<String, ?> context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String newPartyId = (String) context.get("newPartyId");
        String leadPartyId = (String) context.get("leadPartyId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        try {

            String newPartyName = org.ofbiz.party.party.PartyHelper.getPartyName(delegator, newPartyId, false);
            String leadPartyName = org.ofbiz.party.party.PartyHelper.getPartyName(delegator, leadPartyId, false);

            Map messageMap = UtilMisc.toMap("newPartyId", newPartyId, "newPartyName", newPartyName, "leadPartyId", leadPartyId, "leadPartyName", leadPartyName);
            String url = UtilProperties.getMessage(notificationResource, "crmsfa.url.lead", messageMap, locale);
            messageMap.put("url", url);
            String subject = UtilProperties.getMessage(notificationResource, "subject.lead", messageMap, locale);

            Map bodyParameters = UtilMisc.toMap("eventType", "lead");
            bodyParameters.putAll(messageMap);

            Map sendEmailsResult = dispatcher.runSync("crmsfa.sendCrmNotificationEmails", UtilMisc.toMap("notifyPartyIds", UtilMisc.toList(newPartyId), "eventType", "lead", "subject", subject, "bodyParameters", bodyParameters, "userLogin", userLogin));
            if (ServiceUtil.isError(sendEmailsResult)) {
                return sendEmailsResult; 
            }
        } catch (GenericServiceException ex) {
            return UtilMessage.createAndLogServiceError(ex, locale, module);
        }
        return ServiceUtil.returnSuccess();
    }
}
