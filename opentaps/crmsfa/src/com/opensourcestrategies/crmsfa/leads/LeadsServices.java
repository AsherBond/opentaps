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
package com.opensourcestrategies.crmsfa.leads;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.opensourcestrategies.crmsfa.party.PartyHelper;
import com.opensourcestrategies.crmsfa.security.CrmsfaSecurity;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
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
import org.opentaps.base.constants.EnumerationConstants;
import org.opentaps.base.constants.PartyRelationshipTypeConstants;
import org.opentaps.base.constants.RoleTypeConstants;
import org.opentaps.base.constants.SecurityGroupConstants;
import org.opentaps.base.constants.SecurityPermissionConstants;
import org.opentaps.base.constants.StatusItemConstants;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;

/**
 * Leads services. The service documentation is in services_leads.xml.
 *
 * @author     <a href="mailto:leon@opensourcestrategies.com">Leon Torres</a>
 */
public final class LeadsServices {

    private LeadsServices() { }

    private static final String MODULE = LeadsServices.class.getName();
    private static final String NOTIFICATION_RESOURCE = "notification";

    public static Map<String, ?> createLead(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        if (!security.hasPermission(SecurityPermissionConstants.CRMSFA_LEAD_CREATE, userLogin)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
        }

        try {
            if (UtilValidate.isNotEmpty(context.get("parentPartyId"))) {
                PartyHelper.isActive((String) context.get("parentPartyId"), delegator);
            }
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError("CrmErrorLeadNotFound", UtilMisc.toMap("leadPartyId", context.get("parentPartyId")), locale, MODULE);
        }

        // the net result of creating an lead is the generation of a Lead partyId
        String leadPartyId = null;
        try {
            // make sure user has the right crmsfa roles defined.  otherwise the lead could be created but then once converted the account will be deactivated.
            if (UtilValidate.isEmpty(PartyHelper.getFirstValidTeamMemberRoleTypeId(userLogin.getString("partyId"), delegator))) {
                return UtilMessage.createAndLogServiceError("CrmError_NoRoleForCreateParty", UtilMisc.toMap("userPartyName", org.ofbiz.party.party.PartyHelper.getPartyName(delegator, userLogin.getString("partyId"), false), "requiredRoleTypes", PartyHelper.TEAM_MEMBER_ROLES), locale, MODULE);
            }

            // set statusId is PTYLEAD_ASSIGNED, because we are assigning to the user down below.
            // perhaps a better alternative is to create the lead as NEW, call the reassignLeadResponsibleParty service below, and have it update it to ASSIGNED if not already so.
            String statusId = StatusItemConstants.PartyLeadStatus.PTYLEAD_ASSIGNED;

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
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorCreateLeadFail", locale, MODULE);
            }
            leadPartyId = (String) serviceResults.get("partyId");

            // create PartySupplementalData
            GenericValue partyData = delegator.makeValue("PartySupplementalData", UtilMisc.toMap("partyId", leadPartyId));
            partyData.setNonPKFields(context);
            partyData.create();

            // create a PartyRole for the resulting Lead partyId with roleTypeId = PROSPECT
            serviceResults = dispatcher.runSync("createPartyRole", UtilMisc.toMap("partyId", leadPartyId, "roleTypeId", RoleTypeConstants.PROSPECT, "userLogin", userLogin));
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorCreateLeadFail", locale, MODULE);
            }

            // create a party relationship between the userLogin and the Lead with partyRelationshipTypeId RESPONSIBLE_FOR
            PartyHelper.createNewPartyToRelationship(userLogin.getString("partyId"), leadPartyId, RoleTypeConstants.PROSPECT, PartyRelationshipTypeConstants.RESPONSIBLE_FOR, SecurityGroupConstants.LEAD_OWNER, PartyHelper.TEAM_MEMBER_ROLES, true, userLogin, delegator, dispatcher);

            // if the lead was duplicated, also create a relationship between the original lead and the new one with partyRelationshipTypeId DUPLICATED
            String duplicatingPartyId = (String) context.get("duplicatingPartyId");
            if (UtilValidate.isNotEmpty(duplicatingPartyId)) {
                input = UtilMisc.<String, Object>toMap("partyIdTo", leadPartyId, "roleTypeIdTo", RoleTypeConstants.PROSPECT, "partyIdFrom", duplicatingPartyId, "roleTypeIdFrom", RoleTypeConstants.PROSPECT);
                input.put("partyRelationshipTypeId", PartyRelationshipTypeConstants.DUPLICATED);
                input.put("userLogin", userLogin);
                serviceResults = dispatcher.runSync("createPartyRelationship", input);
                if (ServiceUtil.isError(serviceResults)) {
                    return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorCreateLeadFail", locale, MODULE);
                }
            }

            // if initial data source is provided, add it
            String dataSourceId = (String) context.get("dataSourceId");
            if (dataSourceId != null) {
                serviceResults = dispatcher.runSync("crmsfa.addLeadDataSource",
                        UtilMisc.toMap("partyId", leadPartyId, "dataSourceId", dataSourceId, "userLogin", userLogin));
                if (ServiceUtil.isError(serviceResults)) {
                    return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorCreateLeadFail", locale, MODULE);
                }
            }

            // if initial marketing campaign is provided, add it
            String marketingCampaignId = (String) context.get("marketingCampaignId");
            if (marketingCampaignId != null) {
                serviceResults = dispatcher.runSync("crmsfa.addLeadMarketingCampaign",
                        UtilMisc.toMap("partyId", leadPartyId, "marketingCampaignId", marketingCampaignId, "userLogin", userLogin));
                if (ServiceUtil.isError(serviceResults)) {
                    return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorCreateLeadFail", locale, MODULE);
                }
            }

            // create basic contact info
            ModelService service = dctx.getModelService("crmsfa.createBasicContactInfoForParty");
            input = service.makeValid(context, "IN");
            input.put("partyId", leadPartyId);
            serviceResults = dispatcher.runSync(service.name, input);
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorCreateLeadFail", locale, MODULE);
            }

            // Send email re: responsible party to all involved parties
            if ("Y".equals(context.get("notifyOwner"))) {
                dispatcher.runSync("crmsfa.sendLeadNotificationEmails", UtilMisc.toMap("newPartyId", userLogin.getString("partyId"), "leadPartyId", leadPartyId, "userLogin", userLogin));
            }

        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorCreateLeadFail", locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorCreateLeadFail", locale, MODULE);
        }

        // return the partyId of the newly created Lead
        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put("partyId", leadPartyId);
        return results;
    }

    public static Map<String, ?> updateLead(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String leadPartyId = (String) context.get("partyId");

        // make sure userLogin has CRMSFA_LEAD_UPDATE permission for this lead
        if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_LEAD", "_UPDATE", userLogin, leadPartyId)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
        }

        try {
            if (UtilValidate.isNotEmpty(context.get("parentPartyId"))) {
                PartyHelper.isActive((String) context.get("parentPartyId"), delegator);
            }
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError("CrmErrorLeadNotFound", UtilMisc.toMap("leadPartyId", context.get("parentPartyId")), locale, MODULE);
        }

        try {
            // get the party
            GenericValue party = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", leadPartyId));
            if (party == null) {
                return UtilMessage.createAndLogServiceError("CrmErrorUpdateLeadFail", locale, MODULE);
            }

            // change status if passed in statusId is different
            String statusId = (String) context.get("statusId");
            if ((statusId != null) && (!statusId.equals(party.getString("statusId")))) {
                Map<String, Object> serviceResults = dispatcher.runSync("setPartyStatus", UtilMisc.toMap("partyId", leadPartyId, "statusId", statusId, "userLogin", userLogin));
                if (ServiceUtil.isError(serviceResults)) {
                    return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorUpdateLeadFail", locale, MODULE);
                }
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
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorUpdateLeadFail", locale, MODULE);
            }

        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorUpdateLeadFail", locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorUpdateLeadFail", locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, ?> convertLead(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String leadPartyId = (String) context.get("leadPartyId");
        String accountPartyId = (String) context.get("accountPartyId");

        // make sure userLogin has CRMSFA_LEAD_UPDATE permission for this lead
        if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_LEAD", "_UPDATE", userLogin, leadPartyId)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
        }

        Map<String, Object> input = null;  // used later for service inputs
        try {
            GenericValue lead = delegator.findByPrimaryKey("PartySummaryCRMView", UtilMisc.toMap("partyId", leadPartyId));

            // create a PartyRole of type CONTACT for the lead
            Map<String, Object> serviceResults = dispatcher.runSync("createPartyRole", UtilMisc.toMap("partyId", leadPartyId, "roleTypeId", RoleTypeConstants.CONTACT, "userLogin", userLogin));
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorConvertLeadFail", locale, MODULE);
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

                // copy all the marketing campaigns over to the new account
                List<GenericValue> marketingCampaigns = delegator.findByAnd("MarketingCampaignRole", UtilMisc.toMap("partyId", leadPartyId, "roleTypeId", RoleTypeConstants.PROSPECT));
                for (GenericValue marketingCampaign : marketingCampaigns) {
                    serviceResults = dispatcher.runSync("crmsfa.addAccountMarketingCampaign", UtilMisc.toMap("partyId", accountPartyId,
                            "marketingCampaignId", marketingCampaign.getString("marketingCampaignId"), "userLogin", userLogin));
                    if (ServiceUtil.isError(serviceResults)) {
                        return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorConvertLeadFail", locale, MODULE);
                    }
                }


                // copy all the contact mechs to the account
                serviceResults = dispatcher.runSync("copyPartyContactMechs", UtilMisc.toMap("partyIdFrom", leadPartyId, "partyIdTo", accountPartyId, "userLogin", userLogin));
                if (ServiceUtil.isError(serviceResults)) {
                    return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorConvertLeadFail", locale, MODULE);
                }

            }
            // copy all the datasources over to account
            List<GenericValue> dataSources = delegator.findByAnd("PartyDataSource", UtilMisc.toMap("partyId", leadPartyId));
            for (GenericValue dataSource : dataSources) {
            	ModelService service = dctx.getModelService("crmsfa.addAccountDataSource");
            	input = service.makeValid(dataSource, "IN");
            	input.put("userLogin", userLogin);
            	input.put("partyId", accountPartyId);
            	serviceResults = dispatcher.runSync("crmsfa.addAccountDataSource", input);

                if (ServiceUtil.isError(serviceResults)) {
                    return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorConvertLeadFail", locale, MODULE);
                }
            }

            // copy all the notes to account
            List<GenericValue> notes = delegator.findByAnd("PartyNoteView", UtilMisc.toMap("targetPartyId", leadPartyId));
            for (GenericValue note : notes) {
                serviceResults = dispatcher.runSync("crmsfa.createAccountNote", UtilMisc.toMap("partyId", accountPartyId,
                        "note", note.getString("noteInfo"), "userLogin", userLogin));
                if (ServiceUtil.isError(serviceResults)) {
                    return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorConvertLeadFail", locale, MODULE);
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
            List<GenericValue> partyRelationships = delegator.findByAnd("PartyRelationship", UtilMisc.toMap("partyIdFrom", leadPartyId, "roleTypeIdFrom", RoleTypeConstants.PROSPECT));
            PartyHelper.expirePartyRelationships(partyRelationships, UtilDateTime.nowTimestamp(), dispatcher, userLogin);

            // make the userLogin a RESPONSIBLE_FOR CONTACT_OWNER of the CONTACT
            PartyHelper.createNewPartyToRelationship(userLogin.getString("partyId"), leadPartyId, RoleTypeConstants.CONTACT, PartyRelationshipTypeConstants.RESPONSIBLE_FOR, SecurityGroupConstants.CONTACT_OWNER, PartyHelper.TEAM_MEMBER_ROLES, true, userLogin, delegator, dispatcher);

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
                input.put("roleTypeId", RoleTypeConstants.ACCOUNT);
                serviceResults = dispatcher.runSync("assignPartyToWorkEffort", input);
                if (ServiceUtil.isError(serviceResults)) {
                    return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorConvertLeadFail", locale, MODULE);
                }

                // assign the contact to the work effort
                input.put("partyId", leadPartyId);
                input.put("fromDate", null);
                input.put("thruDate", null);
                input.put("roleTypeId", RoleTypeConstants.CONTACT);
                serviceResults = dispatcher.runSync("assignPartyToWorkEffort", input);
                if (ServiceUtil.isError(serviceResults)) {
                    return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorConvertLeadFail", locale, MODULE);
                }
            }

            // opportunities assigned to the lead have to be updated to refer to both contact and account
            List<GenericValue> oppRoles = delegator.findByAnd("SalesOpportunityRole", UtilMisc.toMap("partyId", leadPartyId, "roleTypeId", RoleTypeConstants.PROSPECT));
            for (GenericValue oppRole : oppRoles) {
                // create a CONTACT role using the leadPartyId
                input = UtilMisc.toMap("partyId", leadPartyId, "salesOpportunityId", oppRole.get("salesOpportunityId"), "roleTypeId", RoleTypeConstants.CONTACT);
                GenericValue contactOppRole = delegator.makeValue("SalesOpportunityRole", input);
                contactOppRole.create();

                // create an ACCOUNT role for the new accountPartyId
                input = UtilMisc.toMap("partyId", accountPartyId, "salesOpportunityId", oppRole.get("salesOpportunityId"), "roleTypeId", RoleTypeConstants.ACCOUNT);
                GenericValue accountOppRole = delegator.makeValue("SalesOpportunityRole", input);
                accountOppRole.create();

                // delete the PROSPECT role
                oppRole.remove();
            }

            // associate any lead files and bookmarks with both account and contact
            List<EntityCondition> conditions = UtilMisc.toList(
                    EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, leadPartyId),
                    EntityCondition.makeCondition("roleTypeId", EntityOperator.EQUALS, RoleTypeConstants.PROSPECT),
                    EntityUtil.getFilterByDateExpr()
            );
            List<GenericValue> contentRoles = delegator.findByAnd("ContentRole", conditions);
            for (GenericValue contentRole : contentRoles) {
                contentRole.set("thruDate", UtilDateTime.nowTimestamp());
                contentRole.store();

                GenericValue contactContentRole = delegator.makeValue("ContentRole");
                contactContentRole.set("partyId", leadPartyId);
                contactContentRole.set("contentId", contentRole.get("contentId"));
                contactContentRole.set("roleTypeId", RoleTypeConstants.CONTACT);
                contactContentRole.set("fromDate", UtilDateTime.nowTimestamp());
                contactContentRole.create();

                GenericValue accountContent = delegator.makeValue("PartyContent");
                accountContent.set("partyId", accountPartyId);
                accountContent.set("contentId", contentRole.get("contentId"));
                accountContent.set("contentPurposeEnumId", EnumerationConstants.PtycntPrpCrmsfa.PTYCNT_CRMSFA);
                accountContent.set("partyContentTypeId", "USERDEF");
                accountContent.set("fromDate", UtilDateTime.nowTimestamp());
                accountContent.create();

                GenericValue accountContentRole = delegator.makeValue("ContentRole");
                accountContentRole.set("partyId", accountPartyId);
                accountContentRole.set("contentId", contentRole.get("contentId"));
                accountContentRole.set("roleTypeId", RoleTypeConstants.ACCOUNT);
                accountContentRole.set("fromDate", UtilDateTime.nowTimestamp());
                accountContentRole.create();
            }

            // set the status of the lead to PTYLEAD_CONVERTED
            serviceResults = dispatcher.runSync("setPartyStatus", UtilMisc.toMap("partyId", leadPartyId, "statusId", StatusItemConstants.PartyLeadStatus.PTYLEAD_CONVERTED, "userLogin", userLogin));
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorConvertLeadFail", locale, MODULE);
            }
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorUpdateLeadFail", locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorUpdateLeadFail", locale, MODULE);
        }
        // put leadPartyId as partyId
        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put("partyId", leadPartyId);
        results.put("accountPartyId", accountPartyId);
        return results;
    }

    public static Map<String, ?> reassignLeadResponsibleParty(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String leadPartyId = (String) context.get("leadPartyId");
        String newPartyId = (String) context.get("newPartyId");

        // ensure reassign permission on this lead
        if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_LEAD", "_REASSIGN", userLogin, leadPartyId)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
        }

        try {
            if (UtilValidate.isNotEmpty(newPartyId)) {
                PartyHelper.isActive(newPartyId, delegator);
            }
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError("CrmErrorLeadNotFound", UtilMisc.toMap("leadPartyId", newPartyId), locale, MODULE);
        }

        try {
            // reassign relationship with this helper method, which expires previous ones
            boolean result = PartyHelper.createNewPartyToRelationship(newPartyId, leadPartyId, RoleTypeConstants.PROSPECT, PartyRelationshipTypeConstants.RESPONSIBLE_FOR, SecurityGroupConstants.LEAD_OWNER, PartyHelper.TEAM_MEMBER_ROLES, true, userLogin, delegator, dispatcher);
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
     * Delete a "new" lead. A new lead has status PTYLEAD_NEW, PTYLEAD_ASSIGNED or PTYLEAD_QUALIFIED.
     * This will physically remove the lead from the Party entity and related entities.
     * If the party was successfully deleted, the method will return a service success, otherwise it
     * will return a service error with the reason.
     */
    public static Map<String, ?> deleteLead(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String leadPartyId = (String) context.get("leadPartyId");

        // ensure delete permission on this lead
        if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_LEAD", "_DELETE", userLogin, leadPartyId)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
        }

        try {
            // first ensure the lead is "new" (note that there's no need to check for role because only leads can have these statuses)
            GenericValue lead = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", leadPartyId));
            if (lead == null) {
                return UtilMessage.createAndLogServiceError("Lead [" + leadPartyId + "] not found.",
                        "CrmErrorDeleteLeadFail", locale, MODULE);
            }
            String statusId = lead.getString("statusId");
            if (statusId == null || !(statusId.equals(StatusItemConstants.PartyLeadStatus.PTYLEAD_NEW) || statusId.equals(StatusItemConstants.PartyLeadStatus.PTYLEAD_ASSIGNED) || statusId.equals(StatusItemConstants.PartyLeadStatus.PTYLEAD_QUALIFIED))) {
                return UtilMessage.createAndLogServiceError("Lead [" + leadPartyId + "] cannot be deleted. Only new, assigned or qualified leads may be deleted.",
                        "CrmErrorDeleteLeadFail", locale, MODULE);
            }

            // record deletion (note this entity has no primary key on partyId)
            delegator.create("PartyDeactivation", UtilMisc.toMap("partyId", leadPartyId, "deactivationTimestamp", UtilDateTime.nowTimestamp()));

            // delete!
            PartyHelper.deleteCrmParty(leadPartyId, delegator);

        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorDeleteLeadFail", locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, ?> createCatalogRequestWithSurvey(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = UtilCommon.getLocale(context);
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        try {
            // if an external customer is filling in the form, then perform this service as the special AUTO_REQ_TAKER Party
            if (userLogin == null) {
                userLogin = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "autorqtaker"));
                if (userLogin == null) {
                    return UtilMessage.createAndLogServiceError("CrmErrorAutoReqTakerMissing", locale, MODULE);
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
            if (ServiceUtil.isError(results)) {
                return results;
            }
            String partyId = (String) results.get("partyId");
            String fulfillContactMechId = (String) results.get("generalAddressContactMechId");

            // create a PartyNote using comments field
            String comments = (String) context.get("comments");
            if (comments != null) {
                String note = "Catalog Request Comments: " + comments;
                input = UtilMisc.toMap("partyId", partyId, "note", note, "userLogin", userLogin);
                results = dispatcher.runSync("createPartyNote", input);
                if (ServiceUtil.isError(results)) {
                    return results;
                }
            }

            // create a survey response from the answers_ parameters
            service = dctx.getModelService("createSurveyResponse");
            input = service.makeValid(context, "IN");
            input.put("userLogin", userLogin);
            results = dispatcher.runSync("createSurveyResponse", input);
            if (ServiceUtil.isError(results)) {
                return results;
            }
            String surveyId = (String) results.get("surveyId");
            String surveyResponseId = (String) results.get("surveyResponseId");

            // create a CustRequest of the given type for the party with address as the fulfillment location
            String custRequestTypeId = (String) context.get("custRequestTypeId");
            input = UtilMisc.<String, Object>toMap("custRequestTypeId", custRequestTypeId);
            input.put("userLogin", userLogin);
            input.put("fromPartyId", partyId);
            input.put("statusId", StatusItemConstants.CustreqStts.CRQ_SUBMITTED);
            input.put("custRequestDate", UtilDateTime.nowTimestamp());
            input.put("custRequestName", "Catalog Request");
            input.put("description", "Catalog Request for " + context.get("firstName") + " " + context.get("lastName") + " (" + partyId + ")");
            input.put("fulfillContactMechId", fulfillContactMechId);
            results = dispatcher.runSync("createCustRequest", input);
            if (ServiceUtil.isError(results)) {
                return results;
            }
            String custRequestId = (String) results.get("custRequestId");

            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("partyId", partyId);
            result.put("surveyId", surveyId);
            result.put("surveyResponseId", surveyResponseId);
            result.put("custRequestId", custRequestId);
            return result;
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorCreateLeadSurveyResponseFail", locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorCreateLeadSurveyResponseFail", locale, MODULE);
        }
    }

    /**
     *  Prepares context for crmsfa.sendCrmNotificationEmails service with email subject, body parameters, and list of parties to email.
     */
    public static Map<String, ?> sendLeadNotificationEmails(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String newPartyId = (String) context.get("newPartyId");
        String leadPartyId = (String) context.get("leadPartyId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        try {

            String newPartyName = org.ofbiz.party.party.PartyHelper.getPartyName(delegator, newPartyId, false);
            String leadPartyName = org.ofbiz.party.party.PartyHelper.getPartyName(delegator, leadPartyId, false);

            Map<String, Object> messageMap = UtilMisc.<String, Object>toMap("newPartyId", newPartyId, "newPartyName", newPartyName, "leadPartyId", leadPartyId, "leadPartyName", leadPartyName);
            String url = UtilProperties.getMessage(NOTIFICATION_RESOURCE, "crmsfa.url.lead", messageMap, locale);
            messageMap.put("url", url);
            String subject = UtilProperties.getMessage(NOTIFICATION_RESOURCE, "subject.lead", messageMap, locale);

            Map<String, Object> bodyParameters = UtilMisc.<String, Object>toMap("eventType", "lead");
            bodyParameters.putAll(messageMap);

            Map<String, Object> sendEmailsResult = dispatcher.runSync("crmsfa.sendCrmNotificationEmails", UtilMisc.<String, Object>toMap("notifyPartyIds", UtilMisc.toList(newPartyId), "eventType", "lead", "subject", subject, "bodyParameters", bodyParameters, "userLogin", userLogin));
            if (ServiceUtil.isError(sendEmailsResult)) {
                return sendEmailsResult;
            }
        } catch (GenericServiceException ex) {
            return UtilMessage.createAndLogServiceError(ex, locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }
}
