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
package com.opensourcestrategies.crmsfa.forecasts;

import java.util.*;
import java.sql.Timestamp;

import org.ofbiz.base.util.*;
import org.ofbiz.base.util.collections.ResourceBundleMapWrapper;
import org.ofbiz.entity.*;
import org.ofbiz.entity.condition.*;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.*;
import org.ofbiz.security.Security;

import com.opensourcestrategies.crmsfa.party.PartyHelper;

import org.opentaps.common.util.UtilConfig;
import org.opentaps.common.util.UtilMessage;
import com.opensourcestrategies.crmsfa.opportunities.UtilOpportunity;

/**
 * Forecasts services. The service documentation is in services_forecasts.xml.
 *
 * @author     <a href="mailto:leon@opensourcestrategies.com">Leon Torres</a>
 * @author     <a href="mailto:sichen@opensourcestrategies.com">Si Chen</a>
 * @version    $Rev: 488 $
 */

public class ForecastsServices {

    public static final String module = ForecastsServices.class.getName();
    
    protected static final String FORECAST_CHANGE_NOTE_PREFIX_UILABEL = "CrmForecastChangeNotePrefix";

    public static Map updateForecast(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        String salesForecastId = (String) context.get("salesForecastId");
        Double quotaAmount = (Double) context.get("quotaAmount");
        try {
            GenericValue forecast = delegator.findByPrimaryKey("SalesForecast", UtilMisc.toMap("salesForecastId", salesForecastId));
            if (forecast == null) {
                return UtilMessage.createAndLogServiceError("Forecast with ID [" + salesForecastId + "] not found.", "CrmErrorComputeForecastFail", locale, module);
            }

            // compute the fields for the forecast (use the internalPartyId of the existing forecast)
            Map computed = UtilForecast.computeForecastByOpportunities(quotaAmount, forecast.getString("organizationPartyId"), 
                    forecast.getString("internalPartyId"),  forecast.getString("currencyUomId"), forecast.getString("customTimePeriodId"), delegator);

            // make the service input map from the context
            ModelService service = dctx.getModelService("updateSalesForecast");
            Map input = service.makeValid(context, "IN"); 

            // add rest of fields (in this case we preserve the previous internalPartyId)
            input.put("salesForecastId", salesForecastId); 
            input.putAll(computed); 
            input.put("userLogin", userLogin);

            // run our update/create service
            Map serviceResults = dispatcher.runSync("updateSalesForecast", input);
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorComputeForecastFail", locale, module);
            }

            // now recompute the parent forecast by calling our service for this with the parent as input
            // TODO: normally we could use: GenericValue parent = forecast.getRelatedOne("ParentSalesForecast");
            // TODO: but we can't yet untill we re-arrange the way forecasts are created (create all of them once quarter is selected, then compute values)
            // TODO: so this is a complete hack:
            GenericValue period = forecast.getRelatedOne("CustomTimePeriod");
            List parents = delegator.findByAnd("SalesForecastAndCustomTimePeriod", UtilMisc.toMap("customTimePeriodId", period.getString("parentPeriodId"),
                        "internalPartyId", forecast.getString("internalPartyId"), "organizationPartyId", forecast.getString("organizationPartyId"),
                        "periodTypeId", "FISCAL_QUARTER")); // XXX HACK
            GenericValue parent = (GenericValue) parents.get(0); // XXX HACK

            service = dctx.getModelService("crmsfa.computeForecastParentPeriod");
            input = service.makeValid(parent.getAllFields(), "IN"); 
            input.put("userLogin", userLogin);
            input.put("parentPeriodId", period.getString("parentPeriodId"));
            input.put("changeNote", context.get("changeNote")); // also update the change note in parent
            serviceResults = dispatcher.runSync("crmsfa.computeForecastParentPeriod", input);
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorComputeForecastFail", locale, module);
            }
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorComputeForecastFail", locale, module);
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorComputeForecastFail", locale, module);
        }
        return ServiceUtil.returnSuccess();

    }

    // TODO: The next two methods have a lot of overlapping code.  We should try to re-factor.
    public static Map computeForecastPeriod(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        String customTimePeriodId = (String) context.get("customTimePeriodId");
        String organizationPartyId = (String) context.get("organizationPartyId");
        String currencyUomId = (String) context.get("currencyUomId");

        // set the quota to 0.00 if the user didn't supply it, that way we get all forecasts for a set of periods rather than a few that had quotas defined
        Double quotaAmount = (Double) context.get("quotaAmount");
        if (quotaAmount == null) quotaAmount = new Double(0.00);

        try {
            String salesForecastId = (String) context.get("salesForecastId");
            String serviceName = null;
            String internalPartyId = null;

            // see if we were passed a salesForecastId and determine service to run and party for whom forecast is computed
            if (salesForecastId == null) {
                serviceName = "createSalesForecast";
                internalPartyId = userLogin.getString("partyId");
            } else {
                serviceName = "updateSalesForecast";
                GenericValue forecast = delegator.findByPrimaryKey("SalesForecast", UtilMisc.toMap("salesForecastId", salesForecastId));
                if ((forecast != null) && (forecast.getString("internalPartyId") != null)) {
                    internalPartyId = forecast.getString("internalPartyId");
                } else {
                    return UtilMessage.createAndLogServiceError("CrmErrorInvalidForecast", UtilMisc.toMap("salesForecastId", salesForecastId), locale, module);
                }
            }

            // compute the fields for the forecast
            Map computed = UtilForecast.computeForecastByOpportunities(quotaAmount, organizationPartyId, internalPartyId, 
                    currencyUomId, customTimePeriodId, delegator);

            // make the service input map from the context
            ModelService service = dctx.getModelService(serviceName);
            Map input = service.makeValid(context, "IN"); 

            // add our computed fields and the userlogin
            input.putAll(computed); 
            input.put("userLogin", userLogin);
            input.put("internalPartyId", internalPartyId);

            // run our update/create service
            Map serviceResults = dispatcher.runSync(serviceName, input);
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorComputeForecastFail", locale, module);
            }
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorComputeForecastFail", locale, module);
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorComputeForecastFail", locale, module);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map computeForecastParentPeriod(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        String parentPeriodId = (String) context.get("parentPeriodId");
        String organizationPartyId = (String) context.get("organizationPartyId");
        String currencyUomId = (String) context.get("currencyUomId");

        try {
            // see if we were passed a salesForecastId
            String salesForecastId = (String) context.get("salesForecastId");
            String serviceName = null;
            String internalPartyId = null;

            // see if we were passed a salesForecastId and determine service to run and party for whom forecast is computed
            if (salesForecastId == null) {
                serviceName = "createSalesForecast";
                internalPartyId = userLogin.getString("partyId");
            } else {
                serviceName = "updateSalesForecast";
                GenericValue forecast = delegator.findByPrimaryKey("SalesForecast", UtilMisc.toMap("salesForecastId", salesForecastId));
                if ((forecast != null) && (forecast.getString("internalPartyId") != null)) {
                    internalPartyId = forecast.getString("internalPartyId");
                } else {
                    return UtilMessage.createAndLogServiceError("CrmErrorInvalidForecast", UtilMisc.toMap("salesForecastId", salesForecastId), locale, module);
                }
            }

            // compute the fields for the forecast
            Map computed = UtilForecast.computeForecastByChildren(parentPeriodId, organizationPartyId, internalPartyId, currencyUomId, delegator);

            // make the service input map from the context
            ModelService service = dctx.getModelService(serviceName);
            Map input = service.makeValid(context, "IN"); 
            input.put("customTimePeriodId", parentPeriodId); // the parent period is the custom time period

            // add our computed fields and the userlogin
            input.putAll(computed); 
            input.put("userLogin", userLogin);
            input.put("internalPartyId", internalPartyId);

            // run our update/create service
            Map serviceResults = dispatcher.runSync(serviceName, input);
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorComputeForecastFail", locale, module);
            }
            // if we had no sales forecast, then return the one we just created
            if (salesForecastId == null) {
                salesForecastId = (String) serviceResults.get("salesForecastId");
            }
            
            Map results = ServiceUtil.returnSuccess();
            results.put("salesForecastId", salesForecastId);
            return results;
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorComputeForecastFail", locale, module);
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorComputeForecastFail", locale, module);
        }
    }

    public static Map updateForecastsRelatedToOpportunity(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        ResourceBundleMapWrapper uiLabelMap = (ResourceBundleMapWrapper) UtilProperties.getResourceBundleMap("CRMSFAUiLabels", locale);

        String salesOpportunityId = (String) context.get("salesOpportunityId");
        String changeNote = (String) context.get("changeNote");
        Timestamp previousEstimatedCloseDate = (Timestamp) context.get("previousEstimatedCloseDate");
        
        // get the organization from the properties
        String organizationPartyId = UtilConfig.getPropertyValue("opentaps", "organizationPartyId");

        try {
            GenericValue opportunity = delegator.findByPrimaryKey("SalesOpportunity", UtilMisc.toMap("salesOpportunityId", salesOpportunityId));
            
            // construct the changeNote with a link back to the opportunity (for errors/messages)
            StringBuffer changeNoteBuff = new StringBuffer((String) uiLabelMap.get(FORECAST_CHANGE_NOTE_PREFIX_UILABEL));
            changeNoteBuff.append("<a class='buttontext' href='viewOpportunity?salesOpportunityId=").append(salesOpportunityId).append("'>")
                .append(opportunity.getString("opportunityName")).append(" (").append(salesOpportunityId).append(")</a>");
            if (changeNote != null) {
                changeNoteBuff.append(": ").append(changeNote);
            }

            /*
             * Strategy: Build a list of partyIds whose forecasts would be affected by opportunity.
             *
             * For account opportunities, get the team members for the one related account.
             * For lead opportunities, get the LEAD_OWNER partyId.
             *
             * Then, find all forecasts for these partyId's in the "affected" periods.  An 
             * "affected" time period falls in the opportunity's estimatedCloseDate and the last 
             * estimatedCloseDate (determined from SalesOpportunityHistory).  For each FISCAL_MONTH 
             * forecast found, the compute forecast service is called with all the values of that 
             * forecast, as if the forecast were being updated. After doing months, compute the 
             * quarters (FISCAL_QUARTER) in the same manner.
             */

            String accountPartyId = (String) UtilOpportunity.getOpportunityAccountPartyId(opportunity);
            String leadPartyId = (String) UtilOpportunity.getOpportunityLeadPartyId(opportunity);
            boolean isAccountOpportunity = (accountPartyId != null ? true : false);
            Set partyIds = new HashSet();

            if (isAccountOpportunity) {
                // get all the team members and collect their IDs into a Set
                List teamMembers = UtilOpportunity.getOpportunityTeamMembers(salesOpportunityId, delegator);
                for (Iterator iter = teamMembers.iterator(); iter.hasNext(); ) {
                    GenericValue teamMember = (GenericValue) iter.next();
                    partyIds.add(teamMember.getString("partyId"));
                }
            } else {
                // get the LEAD_OWNER partyId
                GenericValue leadOwner = PartyHelper.getCurrentLeadOwner(leadPartyId, delegator);
                if (leadOwner == null) {
                    return UtilMessage.createAndLogServiceError("No LEAD_OWNER for lead ["+leadPartyId+"] found!", locale, module); 
                }
                partyIds.add(leadOwner.getString("partyId"));
            }

            // if no parties found, then we're done
            if (partyIds.size() == 0) {
                return ServiceUtil.returnSuccess();
            }

            // We want all time periods that contain the new estimatedCloseDate, and the old one if it's different
            List periodConditions = new ArrayList();
            periodConditions.add(EntityUtil.getFilterByDateExpr(opportunity.getTimestamp("estimatedCloseDate")));
            if (previousEstimatedCloseDate != null) {
                // because this condition will be joined by OR, we don't need to worry about them being different
                periodConditions.add(EntityUtil.getFilterByDateExpr(previousEstimatedCloseDate));
            }

            // get the forecasts (ideally we want a distinct, but the way the query is constructed should guarantee a distinct set anyway)
            EntityConditionList conditions = new EntityConditionList( UtilMisc.toList(
                        new EntityConditionList(periodConditions, EntityOperator.OR), // join the periods by OR
                        new EntityExpr("organizationPartyId", EntityOperator.EQUALS, organizationPartyId),
                        new EntityExpr("internalPartyId", EntityOperator.IN, partyIds)
                        ), EntityOperator.AND);

            List forecasts = delegator.findByCondition("SalesForecastAndCustomTimePeriod", conditions, null, null);

            // update forecasts of type FISCAL_MONTH first
            for (Iterator iter = forecasts.iterator(); iter.hasNext(); ) {
                GenericValue forecast = (GenericValue) iter.next();
                if (!forecast.getString("periodTypeId").equals("FISCAL_MONTH")) continue;

                Map input = UtilMisc.toMap("userLogin", userLogin, "organizationPartyId", organizationPartyId, "currencyUomId", forecast.get("currencyUomId"));
                input.put("customTimePeriodId", forecast.getString("customTimePeriodId"));
                input.put("salesForecastId", forecast.getString("salesForecastId"));
                input.put("quotaAmount", forecast.getDouble("quotaAmount"));
                input.put("changeNote", changeNoteBuff.toString());
                Map serviceResults = dispatcher.runSync("crmsfa.computeForecastPeriod", input);
                if (ServiceUtil.isError(serviceResults)) {
                    return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorComputeForecastFail", locale, module);
                }
                iter.remove(); // this helps speed up the next iteration
            }

            // update forecasts of type FISCAL_QUARTER
            for (Iterator iter = forecasts.iterator(); iter.hasNext(); ) {
                GenericValue forecast = (GenericValue) iter.next();
                if (!forecast.getString("periodTypeId").equals("FISCAL_QUARTER")) continue;

                Map input = UtilMisc.toMap("userLogin", userLogin, "organizationPartyId", organizationPartyId, "currencyUomId", forecast.get("currencyUomId"));
                input.put("parentPeriodId", forecast.getString("customTimePeriodId"));
                input.put("salesForecastId", forecast.getString("salesForecastId"));
                input.put("changeNote", changeNoteBuff.toString());
                Map serviceResults = dispatcher.runSync("crmsfa.computeForecastParentPeriod", input);
                if (ServiceUtil.isError(serviceResults)) {
                    return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorComputeForecastFail", locale, module);
                }
            }
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorComputeForecastFail", locale, module);
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorComputeForecastFail", locale, module);
        }
        return ServiceUtil.returnSuccess();
    }
}
