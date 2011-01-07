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

package com.opensourcestrategies.crmsfa.opportunities;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilDate;
import org.opentaps.common.util.UtilMessage;

import com.opensourcestrategies.crmsfa.party.PartyHelper;
import com.opensourcestrategies.crmsfa.security.CrmsfaSecurity;

/**
 * Opportunities services. The service documentation is in services_opportunities.xml.
 */
public final class OpportunitiesServices {

    private OpportunitiesServices() { }

    private static final String MODULE = OpportunitiesServices.class.getName();

    // TODO: the input for this service should be vastly simplified when AJAX autocomplete is finished: only input should be internalPartyId
    public static Map<String, Object> createOpportunity(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        TimeZone timeZone = UtilCommon.getTimeZone(context);

        String internalPartyId = (String) context.get("internalPartyId");
        String accountPartyId = (String) context.get("accountPartyId");
        String contactPartyId = (String) context.get("contactPartyId");
        String leadPartyId = (String) context.get("leadPartyId");
        String accountOrLeadPartyId = (String) context.get("accountOrLeadPartyId");

        try {
            if (UtilValidate.isNotEmpty(accountOrLeadPartyId)) {
                String roleTypeId = PartyHelper.getFirstValidRoleTypeId(accountOrLeadPartyId, Arrays.asList("ACCOUNT", "PROSPECT"), delegator);
                if ("ACCOUNT".equals(roleTypeId)) {
                    accountPartyId = accountOrLeadPartyId;
                }
                if ("PROSPECT".equals(roleTypeId)) {
                    leadPartyId = accountOrLeadPartyId;
                }
            }
            // if internal not supplied, then make sure either an account or lead is supplied, but not both
            if (UtilValidate.isEmpty(internalPartyId) && ((UtilValidate.isEmpty(accountPartyId) && UtilValidate.isEmpty(leadPartyId)) || (UtilValidate.isNotEmpty(accountPartyId) && UtilValidate.isNotEmpty(leadPartyId)))) {
                return UtilMessage.createAndLogServiceError("Please specify an account or a lead (not both).", "CrmErrorCreateOpportunityFail", locale, MODULE);
            }

            // track which partyId we're using, the account or the lead
            String partyId = null;
            if (UtilValidate.isNotEmpty(accountPartyId)) {
                partyId = accountPartyId;
            }
            if (UtilValidate.isNotEmpty(leadPartyId)) {
                partyId = leadPartyId;
            }
            if (UtilValidate.isNotEmpty(internalPartyId)) {
                partyId = internalPartyId;
            }

            // make sure userLogin has CRMSFA_OPP_CREATE permission for the account or lead
            if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_OPP", "_CREATE", userLogin, partyId)) {
                return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
            }


            // set the accountPartyId or leadPartyId according to the role of internalPartyId
            if (UtilValidate.isNotEmpty(internalPartyId)) {
                String roleTypeId = PartyHelper.getFirstValidInternalPartyRoleTypeId(internalPartyId, delegator);
                if ("ACCOUNT".equals(roleTypeId)) {
                    accountPartyId = internalPartyId;
                }
                if ("PROSPECT".equals(roleTypeId)) {
                    leadPartyId = internalPartyId;
                }
            }

            // make sure the lead is qualified if we're doing initial lead
            if (UtilValidate.isNotEmpty(leadPartyId)) {
                GenericValue party = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", leadPartyId));
                if (party == null) {
                    return UtilMessage.createAndLogServiceError("CrmErrorLeadNotFound", UtilMisc.toMap("leadPartyId", leadPartyId), locale, MODULE);
                }
                if (!"PTYLEAD_QUALIFIED".equals(party.get("statusId"))) {
                    return UtilMessage.createAndLogServiceError("CrmErrorLeadNotQualified", UtilMisc.toMap("leadPartyId", leadPartyId), locale, MODULE);
                }
            }

            // set estimatedCloseDate to 23:59:59.999 so that it's at the end of the day
            String estimatedCloseDateString = (String) context.get("estimatedCloseDate");
            Timestamp inputEstimatedCloseDate = UtilDateTime.stringToTimeStamp(estimatedCloseDateString, UtilDate.getDateFormat(estimatedCloseDateString, locale), timeZone, locale);
            Timestamp estimatedCloseDate = UtilDateTime.getDayEnd(inputEstimatedCloseDate, timeZone, locale);

            // create the opportunity
            String salesOpportunityId = delegator.getNextSeqId("SalesOpportunity");
            GenericValue opportunity = delegator.makeValue("SalesOpportunity", UtilMisc.toMap("salesOpportunityId", salesOpportunityId));
            opportunity.setNonPKFields(context);
            opportunity.set("estimatedCloseDate", estimatedCloseDate);
            opportunity.set("createdByUserLogin", userLogin.getString("userLoginId"));

            // if an opportunityStageId is present, set the estimated probability to that of the related stage
            String opportunityStageId = (String) context.get("opportunityStageId");
            if (opportunityStageId != null) {
                GenericValue stage = opportunity.getRelatedOne("SalesOpportunityStage");
                opportunity.set("estimatedProbability", stage.getDouble("defaultProbability"));
            }

            // store it
            opportunity.create();

            // copy to history
            UtilOpportunity.createSalesOpportunityHistory(opportunity, delegator, context);

            // assign the initial account
            if (UtilValidate.isNotEmpty(accountPartyId)) {
                Map<String, Object> serviceResults = dispatcher.runSync("crmsfa.assignOpportunityToAccount",
                        UtilMisc.toMap("salesOpportunityId", salesOpportunityId, "accountPartyId", accountPartyId, "userLogin", userLogin));
                if (ServiceUtil.isError(serviceResults)) {
                    return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorCreateOpportunityFail", locale, MODULE);
                }
            }

            // assign the initial lead
            if (UtilValidate.isNotEmpty(leadPartyId)) {
                Map<String, Object> serviceResults = dispatcher.runSync("crmsfa.assignOpportunityToLead",
                        UtilMisc.toMap("salesOpportunityId", salesOpportunityId, "leadPartyId", leadPartyId, "userLogin", userLogin));
                if (ServiceUtil.isError(serviceResults)) {
                    return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorCreateOpportunityFail", locale, MODULE);
                }
            }

            // assign the initial contact, but only if account was specified
            if (UtilValidate.isNotEmpty(contactPartyId) && UtilValidate.isNotEmpty(accountPartyId)) {
                Map<String, Object> serviceResults = dispatcher.runSync("crmsfa.addContactToOpportunity",
                        UtilMisc.toMap("salesOpportunityId", salesOpportunityId, "contactPartyId", contactPartyId, "userLogin", userLogin));
                if (ServiceUtil.isError(serviceResults)) {
                    return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorCreateOpportunityFail", locale, MODULE);
                }
            }

            // update forecasts as the system user, so we can update all forecasts for all team members that need updating
            GenericValue system = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "system"));
            Map<String, Object> serviceResults = dispatcher.runSync("crmsfa.updateForecastsRelatedToOpportunity",
                    UtilMisc.toMap("salesOpportunityId", salesOpportunityId, "userLogin", system));
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorCreateOpportunityFail", locale, MODULE);
            }

            // return the resulting opportunity ID
            Map<String, Object> results = ServiceUtil.returnSuccess();
            results.put("salesOpportunityId", salesOpportunityId);
            return results;
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorCreateOpportunityFail", locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorCreateOpportunityFail", locale, MODULE);
        } catch (ParseException pe) {
            return UtilMessage.createAndLogServiceError(pe, locale, MODULE);
        }
    }

    public static Map<String, Object> updateOpportunity(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        TimeZone timeZone = UtilCommon.getTimeZone(context);

        String salesOpportunityId = (String) context.get("salesOpportunityId");

        try {
            GenericValue opportunity = delegator.findByPrimaryKey("SalesOpportunity", UtilMisc.toMap("salesOpportunityId", salesOpportunityId));
            if (opportunity == null) {
                return UtilMessage.createAndLogServiceError("CrmErrorUpdateOpportunityFail", locale, MODULE);
            }

            // for security, we need to get the accountPartyId or leadPartyId for this opportunity
            String partyId = UtilOpportunity.getOpportunityAccountOrLeadPartyId(opportunity);

            // make sure userLogin has CRMSFA_OPP_UPDATE permission for this account or lead
            if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_OPP", "_UPDATE", userLogin, partyId)) {
                return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
            }

            // get the new and old stages
            String stageId = opportunity.getString("opportunityStageId");
            String newStageId = (String) context.get("opportunityStageId");
            if (stageId == null) stageId = "";
            if (newStageId == null) newStageId = "";

            // this is needed for updating forecasts
            Timestamp previousEstimatedCloseDate = opportunity.getTimestamp("estimatedCloseDate");

            // update the fields
            opportunity.setNonPKFields(context);

            // set estimatedCloseDate to 23:59:59.999 so that it's at the end of the day
            String estimatedCloseDateString = (String) context.get("estimatedCloseDate");
            Timestamp estimatedCloseDate = UtilDateTime.getDayEnd(UtilDateTime.stringToTimeStamp(estimatedCloseDateString, UtilDateTime.getDateFormat(locale), timeZone, locale), timeZone, locale);
            opportunity.set("estimatedCloseDate", estimatedCloseDate);

            // if the stage changed, set the probability to the one of the stage
            if (!stageId.equals(newStageId)) {
                opportunity.set("estimatedProbability", opportunity.getRelatedOne("SalesOpportunityStage").getDouble("defaultProbability"));
            }

            // store
            opportunity.store();

            // copy the _new_ opportunity into history
            UtilOpportunity.createSalesOpportunityHistory(opportunity, delegator, context);

            // update forecasts as the system user, so we can update all forecasts for all team members that need updating
            GenericValue system = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "system"));
            Map serviceResults = dispatcher.runSync("crmsfa.updateForecastsRelatedToOpportunity",
                    UtilMisc.toMap("salesOpportunityId", salesOpportunityId, "previousEstimatedCloseDate", previousEstimatedCloseDate,
                            "changeNote", context.get("changeNote"), "userLogin", system));
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorUpdateOpportunityFail", locale, MODULE);
            }
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorCreateOpportunityFail", locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorUpdateOpportunityFail", locale, MODULE);
        } catch (ParseException pe) {
            return UtilMessage.createAndLogServiceError(pe, locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map assignOpportunityToAccount(DispatchContext dctx, Map context) {
        return assignOpportunityToPartyHelper(dctx, context, (String) context.get("accountPartyId"), "ACCOUNT", "CRMSFA_ACCOUNT");
    }

    public static Map assignOpportunityToLead(DispatchContext dctx, Map context) {
        return assignOpportunityToPartyHelper(dctx, context, (String) context.get("leadPartyId"), "PROSPECT", "CRMSFA_LEAD");
    }

    /** Helper method to assign an opportunity to an account/lead party */
    private static Map assignOpportunityToPartyHelper(DispatchContext dctx, Map context, String partyId, String roleTypeId, String permissionId) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String salesOpportunityId = (String) context.get("salesOpportunityId");

        // check if userLogin has update permission for this party
        if (!CrmsfaSecurity.hasPartyRelationSecurity(security, permissionId, "_UPDATE", userLogin, partyId)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
        }
        try {
            // create a SalesOpportunityRole with salesOpportunityId, partyId and roleTypeId
            GenericValue role = delegator.makeValue("SalesOpportunityRole", UtilMisc.toMap("salesOpportunityId", salesOpportunityId,
                        "partyId", partyId, "roleTypeId", roleTypeId));
            role.create();
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorAssignFail", locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> addContactToOpportunity(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String salesOpportunityId = (String) context.get("salesOpportunityId");
        String contactPartyId = (String) context.get("contactPartyId");

        try {
            GenericValue opportunity = delegator.findByPrimaryKey("SalesOpportunity", UtilMisc.toMap("salesOpportunityId", salesOpportunityId));
            if (opportunity == null) {
                return UtilMessage.createAndLogServiceError("CrmErrorAddContactToOpportunity", locale, MODULE);
            }

            // for security, we need to get the accountPartyId for this opportunity
            String accountPartyId = UtilOpportunity.getOpportunityAccountPartyId(opportunity);

            // if no account exists, don't add contact to opportunity (rationale: this is a weird case and we don't know if convert lead will copy these)
            if (accountPartyId == null) {
                String leadPartyId = UtilOpportunity.getOpportunityLeadPartyId(opportunity);
                if (leadPartyId != null) {
                    return UtilMessage.createAndLogServiceError("Cannot add contact to a lead opportunity.", "CrmErrorAddContactToOpportunity", locale, MODULE);
                } else {
                    return UtilMessage.createAndLogServiceError("Cound not find account for opportunity ["+salesOpportunityId+"].", "CrmErrorAddContactToOpportunity", locale, MODULE);
                }
            }

            // check if userLogin has CRMSFA_OPP_UPDATE permission for this contact
            if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_OPP", "_UPDATE", userLogin, accountPartyId)) {
                return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
            }

            // check first that this contact is associated with the account
            List<GenericValue> candidates = EntityUtil.filterByDate(delegator.findByAnd("PartyRelationship", UtilMisc.toMap("partyIdFrom", contactPartyId,
                            "partyIdTo", accountPartyId, "partyRelationshipTypeId", "CONTACT_REL_INV"), UtilMisc.toList("fromDate DESC")));
            if (candidates.size() == 0) {
                return UtilMessage.createAndLogServiceError("Contact with ID [" + contactPartyId + "] is not associated with Account with ID [" +
                        accountPartyId + "]", "CrmErrorAddContactToOpportunity", locale, MODULE);
            }

            // avoid duplicates
            Map<String, String> keys = UtilMisc.toMap("salesOpportunityId", salesOpportunityId, "partyId", contactPartyId, "roleTypeId", "CONTACT");
            GenericValue role = delegator.findByPrimaryKey("SalesOpportunityRole", keys);
            if (role != null) {
                return UtilMessage.createAndLogServiceError("Contact is already associated with this Opportunity.", "CrmErrorAddContactToOpportunity", locale, MODULE);
            }

            // create a SalesOpportunityRole with salesOpportunityId and partyId=contactPartyId and roleTypeId=CONTACT
            role = delegator.makeValue("SalesOpportunityRole", keys);
            role.create();
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorAddContactToOpportunity", locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> removeContactFromOpportunity(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String salesOpportunityId = (String) context.get("salesOpportunityId");
        String contactPartyId = (String) context.get("contactPartyId");

        try {
            GenericValue opportunity = delegator.findByPrimaryKey("SalesOpportunity", UtilMisc.toMap("salesOpportunityId", salesOpportunityId));
            if (opportunity == null) {
                return UtilMessage.createAndLogServiceError("No opportunity with ID [" + salesOpportunityId + "] found.",
                        "CrmErrorRemoveContactFromOpportunity", locale, MODULE);
            }

            // for security, we need to get the accountPartyId for this opportunity
            String accountPartyId = UtilOpportunity.getOpportunityAccountPartyId(opportunity);

            // check if userLogin has CRMSFA_OPP_UPDATE permission for this contact
            if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_OPP", "_UPDATE", userLogin, accountPartyId)) {
                return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
            }

            // delete the SalesOpportunityRole with salesOpportunityId and partyId=contactPartyId and roleTypeId=CONTACT
            GenericValue role = delegator.findByPrimaryKey("SalesOpportunityRole", UtilMisc.toMap("salesOpportunityId", salesOpportunityId,
                        "partyId", contactPartyId, "roleTypeId", "CONTACT"));
            if (role == null) {
                return UtilMessage.createAndLogServiceError("Could not find contact with ID [" +
                        contactPartyId + "] for the opportunity with ID [" + salesOpportunityId + "].", "CrmErrorRemoveContactFromOpportunity", locale, MODULE);
            }
            role.remove();
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorRemoveContactFromOpportunity", locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map addQuoteToOpportunity(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String salesOpportunityId = (String) context.get("salesOpportunityId");
        String quoteId = (String) context.get("quoteId");

        try {
            // for security, we need to get the account or lead partyId for this opportunity
            String partyId = UtilOpportunity.getOpportunityAccountOrLeadPartyId(delegator.findByPrimaryKey("SalesOpportunity",
                        UtilMisc.toMap("salesOpportunityId", salesOpportunityId)));

            // make sure userLogin has CRMSFA_OPP_UPDATE permission for this account
            if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_OPP", "_UPDATE", userLogin, partyId)) {
                return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
            }

            // There's no service in ofbiz to create SalesOpportunityQuote entries, so we do it by hand
            Map input = UtilMisc.toMap("quoteId", quoteId);
            GenericValue relation = delegator.findByPrimaryKeyCache("Quote", input);
            if (relation == null) {
                return UtilMessage.createAndLogServiceError("CrmErrorQuoteNotFound", UtilMisc.toMap("quoteId", quoteId), locale, MODULE);
            }

            // see if the relation already exists and if not, create it
            input.put("salesOpportunityId", salesOpportunityId);
            relation = delegator.findByPrimaryKeyCache("SalesOpportunityQuote", input);
            if (relation == null) {
                relation = delegator.makeValue("SalesOpportunityQuote", input);
                relation.create();
            }
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorAddQuoteFail", locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map removeQuoteFromOpportunity(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String salesOpportunityId = (String) context.get("salesOpportunityId");
        String quoteId = (String) context.get("quoteId");

        try {
            // for security, we need to get the account or lead partyId for this opportunity
            String partyId = UtilOpportunity.getOpportunityAccountOrLeadPartyId(delegator.findByPrimaryKey("SalesOpportunity",
                        UtilMisc.toMap("salesOpportunityId", salesOpportunityId)));

            // make sure userLogin has CRMSFA_OPP_UPDATE permission for this account
            if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_OPP", "_UPDATE", userLogin, partyId)) {
                return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
            }

            // There's no service in ofbiz to remove SalesOpportunityQuote entries, so we do it by hand

            // see if the relation already exists and if so, remove it
            Map input = UtilMisc.toMap("salesOpportunityId", salesOpportunityId, "quoteId", quoteId);
            GenericValue relation = delegator.findByPrimaryKey("SalesOpportunityQuote", input);
            if (relation != null) {
                relation.remove();
            }
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorRemoveQuoteFail", locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

}
