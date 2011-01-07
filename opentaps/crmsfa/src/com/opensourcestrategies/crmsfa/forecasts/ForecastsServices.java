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
package com.opensourcestrategies.crmsfa.forecasts;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.opensourcestrategies.crmsfa.opportunities.UtilOpportunity;
import com.opensourcestrategies.crmsfa.party.PartyHelper;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.collections.ResourceBundleMapWrapper;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilConfig;
import org.opentaps.common.util.UtilMessage;

/**
 * Forecasts services. The service documentation is in services_forecasts.xml.
 *
 * @author     <a href="mailto:leon@opensourcestrategies.com">Leon Torres</a>
 * @author     <a href="mailto:sichen@opensourcestrategies.com">Si Chen</a>
 */
public final class ForecastsServices {

    private ForecastsServices() { }

    private static final String MODULE = ForecastsServices.class.getName();

    protected static final String FORECAST_CHANGE_NOTE_PREFIX_UILABEL = "CrmForecastChangeNotePrefix";

    public static Map<String, Object> updateForecast(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String salesForecastId = (String) context.get("salesForecastId");
        BigDecimal quotaAmount = (BigDecimal) context.get("quotaAmount");
        try {
            GenericValue forecast = delegator.findByPrimaryKey("SalesForecast", UtilMisc.toMap("salesForecastId", salesForecastId));
            if (forecast == null) {
                return UtilMessage.createAndLogServiceError("Forecast with ID [" + salesForecastId + "] not found.", "CrmErrorComputeForecastFail", locale, MODULE);
            }

            // compute the fields for the forecast (use the internalPartyId of the existing forecast)
            Map<String, BigDecimal> computed = UtilForecast.computeForecastByOpportunities(quotaAmount, forecast.getString("organizationPartyId"),
                    forecast.getString("internalPartyId"),  forecast.getString("currencyUomId"), forecast.getString("customTimePeriodId"), delegator);

            // make the service input map from the context
            ModelService service = dctx.getModelService("updateSalesForecast");
            Map<String, Object> input = service.makeValid(context, "IN");

            // add rest of fields (in this case we preserve the previous internalPartyId)
            input.put("salesForecastId", salesForecastId);
            input.putAll(computed);
            input.put("userLogin", userLogin);

            // run our update/create service
            Map<String, Object> serviceResults = dispatcher.runSync("updateSalesForecast", input);
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorComputeForecastFail", locale, MODULE);
            }

            // now recompute the parent forecast by calling our service for this with the parent as input
            // TODO: normally we could use: GenericValue parent = forecast.getRelatedOne("ParentSalesForecast"); but we can't yet untill we re-arrange the way forecasts are created (create all of them once quarter is selected, then compute values) so this is a complete hack:
            GenericValue period = forecast.getRelatedOne("CustomTimePeriod");
            List<GenericValue> parents = delegator.findByAnd("SalesForecastAndCustomTimePeriod", UtilMisc.toMap("customTimePeriodId", period.getString("parentPeriodId"),
                        "internalPartyId", forecast.getString("internalPartyId"), "organizationPartyId", forecast.getString("organizationPartyId"),
                        "periodTypeId", "FISCAL_QUARTER")); // XXX HACK
            GenericValue parent = parents.get(0); // XXX HACK

            service = dctx.getModelService("crmsfa.computeForecastParentPeriod");
            input = service.makeValid(parent.getAllFields(), "IN");
            input.put("userLogin", userLogin);
            input.put("parentPeriodId", period.getString("parentPeriodId"));
            input.put("changeNote", context.get("changeNote")); // also update the change note in parent
            serviceResults = dispatcher.runSync("crmsfa.computeForecastParentPeriod", input);
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorComputeForecastFail", locale, MODULE);
            }
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorComputeForecastFail", locale, MODULE);
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorComputeForecastFail", locale, MODULE);
        }
        return ServiceUtil.returnSuccess();

    }

    // TODO: The next two methods have a lot of overlapping code.  We should try to re-factor.
    public static Map<String, Object> computeForecastPeriod(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String customTimePeriodId = (String) context.get("customTimePeriodId");
        String organizationPartyId = (String) context.get("organizationPartyId");
        String currencyUomId = (String) context.get("currencyUomId");

        // set the quota to 0.00 if the user didn't supply it, that way we get all forecasts for a set of periods rather than a few that had quotas defined
        BigDecimal quotaAmount = (BigDecimal) context.get("quotaAmount");
        if (quotaAmount == null) {
            quotaAmount = BigDecimal.ZERO;
        }

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
                    return UtilMessage.createAndLogServiceError("CrmErrorInvalidForecast", UtilMisc.toMap("salesForecastId", salesForecastId), locale, MODULE);
                }
            }

            // compute the fields for the forecast
            Map<String, BigDecimal> computed = UtilForecast.computeForecastByOpportunities(quotaAmount, organizationPartyId, internalPartyId, currencyUomId, customTimePeriodId, delegator);

            // make the service input map from the context
            ModelService service = dctx.getModelService(serviceName);
            Map<String, Object> input = service.makeValid(context, "IN");

            // add our computed fields and the userlogin
            input.putAll(computed);
            input.put("userLogin", userLogin);
            input.put("internalPartyId", internalPartyId);

            // run our update/create service
            Map<String, Object> serviceResults = dispatcher.runSync(serviceName, input);
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorComputeForecastFail", locale, MODULE);
            }
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorComputeForecastFail", locale, MODULE);
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorComputeForecastFail", locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> computeForecastParentPeriod(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

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
                    return UtilMessage.createAndLogServiceError("CrmErrorInvalidForecast", UtilMisc.toMap("salesForecastId", salesForecastId), locale, MODULE);
                }
            }

            // compute the fields for the forecast
            Map<String, BigDecimal> computed = UtilForecast.computeForecastByChildren(parentPeriodId, organizationPartyId, internalPartyId, currencyUomId, delegator);

            // make the service input map from the context
            ModelService service = dctx.getModelService(serviceName);
            Map<String, Object> input = service.makeValid(context, "IN");
            input.put("customTimePeriodId", parentPeriodId); // the parent period is the custom time period

            // add our computed fields and the userlogin
            input.putAll(computed);
            input.put("userLogin", userLogin);
            input.put("internalPartyId", internalPartyId);

            // run our update/create service
            Map<String, Object> serviceResults = dispatcher.runSync(serviceName, input);
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorComputeForecastFail", locale, MODULE);
            }
            // if we had no sales forecast, then return the one we just created
            if (salesForecastId == null) {
                salesForecastId = (String) serviceResults.get("salesForecastId");
            }

            Map<String, Object> results = ServiceUtil.returnSuccess();
            results.put("salesForecastId", salesForecastId);
            return results;
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorComputeForecastFail", locale, MODULE);
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorComputeForecastFail", locale, MODULE);
        }
    }

    public static Map<String, Object> updateForecastsRelatedToOpportunity(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        ResourceBundleMapWrapper uiLabelMap = UtilProperties.getResourceBundleMap("CRMSFAUiLabels", locale);

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

            String accountPartyId = UtilOpportunity.getOpportunityAccountPartyId(opportunity);
            String leadPartyId = UtilOpportunity.getOpportunityLeadPartyId(opportunity);
            boolean isAccountOpportunity = (accountPartyId != null ? true : false);
            Set<String> partyIds = new HashSet<String>();

            if (isAccountOpportunity) {
                // get all the team members and collect their IDs into a Set
                List<GenericValue> teamMembers = UtilOpportunity.getOpportunityTeamMembers(salesOpportunityId, delegator);
                for (GenericValue teamMember : teamMembers) {
                    partyIds.add(teamMember.getString("partyId"));
                }
            } else {
                // get the LEAD_OWNER partyId
                GenericValue leadOwner = PartyHelper.getCurrentLeadOwner(leadPartyId, delegator);
                if (leadOwner == null) {
                    return UtilMessage.createAndLogServiceError("No LEAD_OWNER for lead [" + leadPartyId + "] found!", locale, MODULE);
                }
                partyIds.add(leadOwner.getString("partyId"));
            }

            // if no parties found, then we're done
            if (partyIds.size() == 0) {
                return ServiceUtil.returnSuccess();
            }

            // We want all time periods that contain the new estimatedCloseDate, and the old one if it's different
            List<EntityCondition> periodConditions = new ArrayList<EntityCondition>();
            periodConditions.add(EntityUtil.getFilterByDateExpr(opportunity.getTimestamp("estimatedCloseDate")));
            if (previousEstimatedCloseDate != null) {
                // because this condition will be joined by OR, we don't need to worry about them being different
                periodConditions.add(EntityUtil.getFilterByDateExpr(previousEstimatedCloseDate));
            }

            // get the forecasts (ideally we want a distinct, but the way the query is constructed should guarantee a distinct set anyway)
            EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                        EntityCondition.makeCondition(periodConditions, EntityOperator.OR), // join the periods by OR
                        EntityCondition.makeCondition("organizationPartyId", EntityOperator.EQUALS, organizationPartyId),
                        EntityCondition.makeCondition("internalPartyId", EntityOperator.IN, partyIds));

            List<GenericValue> forecasts = delegator.findByCondition("SalesForecastAndCustomTimePeriod", conditions, null, null);

            // update forecasts of type FISCAL_MONTH first
            for (Iterator<GenericValue> iter = forecasts.iterator(); iter.hasNext();) {
                GenericValue forecast = iter.next();
                if (!forecast.getString("periodTypeId").equals("FISCAL_MONTH")) {
                    continue;
                }

                Map<String, Object> input = UtilMisc.toMap("userLogin", userLogin, "organizationPartyId", organizationPartyId, "currencyUomId", forecast.get("currencyUomId"));
                input.put("customTimePeriodId", forecast.getString("customTimePeriodId"));
                input.put("salesForecastId", forecast.getString("salesForecastId"));
                input.put("quotaAmount", forecast.getBigDecimal("quotaAmount"));
                input.put("changeNote", changeNoteBuff.toString());
                Map<String, Object> serviceResults = dispatcher.runSync("crmsfa.computeForecastPeriod", input);
                if (ServiceUtil.isError(serviceResults)) {
                    return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorComputeForecastFail", locale, MODULE);
                }
                iter.remove(); // this helps speed up the next iteration
            }

            // update forecasts of type FISCAL_QUARTER
            for (Iterator<GenericValue> iter = forecasts.iterator(); iter.hasNext();) {
                GenericValue forecast = iter.next();
                if (!forecast.getString("periodTypeId").equals("FISCAL_QUARTER")) {
                    continue;
                }

                Map<String, Object> input = UtilMisc.toMap("userLogin", userLogin, "organizationPartyId", organizationPartyId, "currencyUomId", forecast.get("currencyUomId"));
                input.put("parentPeriodId", forecast.getString("customTimePeriodId"));
                input.put("salesForecastId", forecast.getString("salesForecastId"));
                input.put("changeNote", changeNoteBuff.toString());
                Map<String, Object> serviceResults = dispatcher.runSync("crmsfa.computeForecastParentPeriod", input);
                if (ServiceUtil.isError(serviceResults)) {
                    return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorComputeForecastFail", locale, MODULE);
                }
            }
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorComputeForecastFail", locale, MODULE);
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorComputeForecastFail", locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }
}
