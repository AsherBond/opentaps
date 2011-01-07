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

import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.math.BigDecimal;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.base.util.UtilNumber;

import com.opensourcestrategies.crmsfa.opportunities.UtilOpportunity;

/**
 * Forecast utility methods.
 *
 * @author     <a href="mailto:leon@opensourcestrategies.com">Leon Torres</a>
 * @version    $Rev: 488 $
 */
public final class UtilForecast {

    private UtilForecast() { }

    private static final String MODULE = UtilForecast.class.getName();

    // BigDecimal scale and rounding mode

    /** Configured rounding to use for forecast amounts. */
    public static final int BD_FORECAST_ROUNDING = UtilNumber.getBigDecimalRoundingMode("crmsfa.properties", "crmsfa.forecast.percent.rounding");
    /** Configured scaling to use for forecast amounts. */
    public static final int BD_FORECAST_DECIMALS = UtilNumber.getBigDecimalScale("crmsfa.properties", "crmsfa.forecast.decimals");
    /** Configured rounding to use when calculating forecast amounts from quota percentages. */
    public static final int BD_FORECAST_PERCENT_ROUNDING = UtilNumber.getBigDecimalRoundingMode("crmsfa.properties", "crmsfa.forecast.rounding");
    /** Configured scaling to use when calculating forecast amounts from quota percentages. */
    public static final int BD_FORECAST_PERCENT_DECIMALS = UtilNumber.getBigDecimalScale("crmsfa.properties", "crmsfa.forecast.percent.decimals");

    /**
     * Computes the forecast for the indicated time period.
     *
     * @param quotaAmount a <code>BigDecimal</code> value
     * @param organizationPartyId a <code>String</code> value
     * @param internalPartyId a <code>String</code> value
     * @param currencyUomId a <code>String</code> value
     * @param customTimePeriodId a <code>String</code> value
     * @param delegator a <code>Delegator</code> value
     * @return <code>Map</code> of SalesForecast fields with the computed amounts as <code>BigDecimal</code>
     * @exception GenericEntityException if an error occurs
     */
    public static Map<String, BigDecimal> computeForecastByOpportunities(BigDecimal quotaAmount, String organizationPartyId, String internalPartyId, String currencyUomId, String customTimePeriodId, Delegator delegator) throws GenericEntityException {

        // computation variables
        BigDecimal closedAmount = BigDecimal.ZERO;
        BigDecimal bestCaseAmount = BigDecimal.ZERO;
        BigDecimal forecastAmount = BigDecimal.ZERO;
        BigDecimal percentOfQuotaForecast = BigDecimal.ZERO;
        BigDecimal percentOfQuotaClosed = BigDecimal.ZERO;

        // determine the minimum percet for computing forecast amounts
        BigDecimal minimumProbability = BigDecimal.ZERO;
        try {
            minimumProbability = new BigDecimal(UtilProperties.getPropertyValue("crmsfa.properties", "crmsfa.forecast.minProbability"));
        } catch (NumberFormatException ne) {
            Debug.logWarning("Failed to parse property \"crmsfa.forecast.minProbability\": " + ne.getMessage() + "\nSetting minimum probability to 0.0.", MODULE);
        }

        // get the opportunities in the time period and loop through them
        EntityListIterator opportunitiesELI = UtilOpportunity.getOpportunitiesForInternalParty(organizationPartyId, internalPartyId, customTimePeriodId, null, null, delegator);
        List<GenericValue> opportunities = opportunitiesELI.getCompleteList();

        if (opportunities.size() == 0) {
            Debug.logWarning("no opportunities were found for the internalParty [" + internalPartyId + "] and organizationParty [" + organizationPartyId + "] over time period id [" + customTimePeriodId + "]", MODULE);
        }

        for (Iterator<GenericValue> iter = opportunities.iterator(); iter.hasNext();) {
            GenericValue opportunity = iter.next();
            if (Debug.verboseOn()) {
                Debug.logVerbose("for timePeriod [" + customTimePeriodId + "] and party [" + internalPartyId + "].  Now working with opportunity: " + opportunity, MODULE);
            }
            BigDecimal amount = opportunity.getBigDecimal("estimatedAmount");
            BigDecimal probability = opportunity.getBigDecimal("estimatedProbability");

            if (amount == null) {
                continue;
            }

            // TODO: conversion
            String oppCurrencyUomId = opportunity.getString("currencyUomId");
            if ((oppCurrencyUomId == null) || !oppCurrencyUomId.equals(currencyUomId)) {
                Debug.logWarning("Forecast currency unit of measure [" + currencyUomId + "] is different from opportunity ID [" + opportunity.getString("salesOpportunityId") + "] currency which is [" + oppCurrencyUomId + "]. However, conversion is not currently implemented. Forecast will be incorrect.", MODULE);
            }

            if (opportunity.getString("opportunityStageId").equals("SOSTG_CLOSED")) {

                // closed amount, which is the sum of the closed opportunity amounts
                closedAmount = closedAmount.add(amount).setScale(BD_FORECAST_DECIMALS, BD_FORECAST_ROUNDING);
            } else {

                // best case amount, which is the sum of amounts from opportunities scheduled to be closed in the time period plus the closed amount
                bestCaseAmount = bestCaseAmount.add(amount).setScale(BD_FORECAST_DECIMALS, BD_FORECAST_ROUNDING);

                // for probabilities above the threshold, add to forecast amount
                if ((probability != null) && (probability.compareTo(minimumProbability) >= 0)) {
                    forecastAmount = forecastAmount.add(probability.multiply(amount)).setScale(BD_FORECAST_DECIMALS, BD_FORECAST_ROUNDING);
                }
            }
            if (Debug.verboseOn()) {
                Debug.logVerbose("Now for timePeriod [" + customTimePeriodId + "] after opportunity [" + opportunity.getString("salesOpportunityId") + "] " + "closedAmouont = [" + closedAmount + "] bestCaseAmount = [" + bestCaseAmount + "] forecastAmount = [" + forecastAmount + "]" , MODULE);
            }
        }

        bestCaseAmount = bestCaseAmount.add(closedAmount).setScale(BD_FORECAST_DECIMALS, BD_FORECAST_ROUNDING);
        forecastAmount = forecastAmount.add(closedAmount).setScale(BD_FORECAST_DECIMALS, BD_FORECAST_ROUNDING);

        // if the quota was provided, compute percent of quota forecast and closed
        if (quotaAmount != null && quotaAmount.signum() > 0) {
            percentOfQuotaForecast = forecastAmount.divide(quotaAmount, BD_FORECAST_PERCENT_DECIMALS, BD_FORECAST_PERCENT_ROUNDING);
            percentOfQuotaClosed = closedAmount.divide(quotaAmount, BD_FORECAST_PERCENT_DECIMALS, BD_FORECAST_PERCENT_ROUNDING);
        }

        // reutrn the computation as one big map which is ready to be set as fields of SalesForecast
        return UtilMisc.toMap("closedAmount", closedAmount,
                              "bestCaseAmount", bestCaseAmount,
                              "forecastAmount", forecastAmount,
                              "percentOfQuotaForecast", percentOfQuotaForecast,
                              "percentOfQuotaClosed", percentOfQuotaClosed);
    }
    /**
     * Computes the forecast for the indicated time period.
     *
     * @param parentPeriodId a <code>String</code> value
     * @param organizationPartyId a <code>String</code> value
     * @param internalPartyId a <code>String</code> value
     * @param currencyUomId a <code>String</code> value
     * @param delegator a <code>Delegator</code> value
     * @return <code>Map</code> of SalesForecast fields with the computed amounts as <code>BigDecimal</code>
     * @exception GenericEntityException if an error occurs
     */
    public static Map<String, BigDecimal> computeForecastByChildren(String parentPeriodId, String organizationPartyId, String internalPartyId, String currencyUomId, Delegator delegator) throws GenericEntityException {

        // computation variables
        BigDecimal quotaAmount = BigDecimal.ZERO;
        BigDecimal closedAmount = BigDecimal.ZERO;
        BigDecimal bestCaseAmount = BigDecimal.ZERO;
        BigDecimal forecastAmount = BigDecimal.ZERO;
        BigDecimal percentOfQuotaForecast = BigDecimal.ZERO;
        BigDecimal percentOfQuotaClosed = BigDecimal.ZERO;

        // get the children and loop through them
        // TODO: conversion
        List<GenericValue> forecasts = delegator.findByAnd("SalesForecastAndCustomTimePeriod", UtilMisc.toMap("parentPeriodId", parentPeriodId,
                    "organizationPartyId", organizationPartyId, "internalPartyId", internalPartyId));
        for (Iterator<GenericValue> iter = forecasts.iterator(); iter.hasNext();) {
            GenericValue forecast = iter.next();

            BigDecimal childQuotaAmount = forecast.getBigDecimal("quotaAmount");
            if (childQuotaAmount != null) {
                quotaAmount = quotaAmount.add(childQuotaAmount).setScale(BD_FORECAST_DECIMALS, BD_FORECAST_ROUNDING);
            }

            BigDecimal childClosedAmount = forecast.getBigDecimal("closedAmount");
            if (childClosedAmount != null) {
                closedAmount = closedAmount.add(childClosedAmount).setScale(BD_FORECAST_DECIMALS, BD_FORECAST_ROUNDING);
            }

            BigDecimal childForecastAmount = forecast.getBigDecimal("forecastAmount");
            if (childForecastAmount != null) {
                forecastAmount = forecastAmount.add(childForecastAmount).setScale(BD_FORECAST_DECIMALS, BD_FORECAST_ROUNDING);
            }

            BigDecimal childBestCaseAmount = forecast.getBigDecimal("bestCaseAmount");
            if (childBestCaseAmount != null) {
                bestCaseAmount = bestCaseAmount.add(childBestCaseAmount).setScale(BD_FORECAST_DECIMALS, BD_FORECAST_ROUNDING);
            }
        }

        // if the quota is non-zero, set the percentages
        if (quotaAmount.signum() != 0) {
            percentOfQuotaForecast = forecastAmount.divide(quotaAmount, BD_FORECAST_PERCENT_DECIMALS, BD_FORECAST_PERCENT_ROUNDING);
            percentOfQuotaClosed = closedAmount.divide(quotaAmount, BD_FORECAST_PERCENT_DECIMALS, BD_FORECAST_PERCENT_ROUNDING);
        }

        // reutrn the computation as one big map which is ready to be set as fields of SalesForecast
        return UtilMisc.toMap("closedAmount", closedAmount,
                              "bestCaseAmount", bestCaseAmount,
                              "forecastAmount", forecastAmount,
                              "percentOfQuotaForecast", percentOfQuotaForecast,
                              "percentOfQuotaClosed", percentOfQuotaClosed,
                              "quotaAmount", quotaAmount);
    }
}
