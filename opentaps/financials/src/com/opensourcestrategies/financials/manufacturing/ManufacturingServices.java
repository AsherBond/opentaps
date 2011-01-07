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
package com.opensourcestrategies.financials.manufacturing;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

/**
 * ManufacturingServices - Services to manage production run costs
 *
 */

public class ManufacturingServices {

    public static final String module = ManufacturingServices.class.getName();
    
    private static BigDecimal ZERO = BigDecimal.ZERO;
    private static BigDecimal ONE = BigDecimal.ONE;
    private static int decimals = -1;
    private static int rounding = -1;
    static {
        decimals = UtilNumber.getBigDecimalScale("order.decimals");
        rounding = UtilNumber.getBigDecimalRoundingMode("order.rounding");
        // set zero to the proper scale
        ZERO.setScale(decimals);
        ONE.setScale(decimals);
    }

    public static Map createWorkEffortCosts(DispatchContext ctx, Map context) {
        Map result = new HashMap();
        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String workEffortId = (String)context.get("workEffortId");
        String workEffortTemplateId = null;
        try {
            GenericValue workEffort = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId));
            if (workEffort == null) {
                return ServiceUtil.returnError("Cannot find work effort [" + workEffortId + "]");
            }
            // Get the template (aka routing task) of the work effort
            GenericValue workEffortTemplate = EntityUtil.getFirst(EntityUtil.filterByDate(delegator.findByAnd("WorkEffortAssoc",
                                                                  UtilMisc.toMap("workEffortIdTo", workEffortId,
                                                                  "workEffortAssocTypeId", "WORK_EFF_TEMPLATE"))));
            if (workEffortTemplate == null) {
                workEffortTemplateId = workEffortId;
            } else {
                workEffortTemplateId = workEffortTemplate.getString("workEffortIdFrom");
            }
            // Get all the valid CostComponentCalc entries
            List workEffortCostCalcs = EntityUtil.filterByDate(delegator.findByAnd("WorkEffortCostCalc",
                                                          UtilMisc.toMap("workEffortId", workEffortTemplateId)));
            Iterator workEffortCostCalcsIt = workEffortCostCalcs.iterator();
            while (workEffortCostCalcsIt.hasNext()) {
                GenericValue workEffortCostCalc = (GenericValue)workEffortCostCalcsIt.next();
                GenericValue costComponentCalc = workEffortCostCalc.getRelatedOne("CostComponentCalc");
                GenericValue customMethod = costComponentCalc.getRelatedOne("CustomMethod");
                if (customMethod == null) {
                    BigDecimal fixedCost = costComponentCalc.getBigDecimal("fixedCost");
                    if (fixedCost == null) {
                        fixedCost = ZERO;
                    }
                    BigDecimal variableCost = costComponentCalc.getBigDecimal("variableCost");
                    if (variableCost == null) {
                        variableCost = ZERO;
                    }
                    Long perMilliSecond = costComponentCalc.getLong("perMilliSecond");
                    if (perMilliSecond == null) {
                        perMilliSecond = new Long(1);
                    }
                    Double actualMilliSeconds = workEffort.getDouble("actualMilliSeconds");
                    if (actualMilliSeconds == null) {
                        actualMilliSeconds = new Double(0);
                    }
                    Double actualSetupMillis = workEffort.getDouble("actualSetupMillis");
                    if (actualSetupMillis == null) {
                        actualSetupMillis = new Double(0);
                    }
                    Double totalTime = new Double((actualMilliSeconds.doubleValue() + actualSetupMillis.doubleValue()) / perMilliSecond.intValue()); // TODO: should we use BigDecimals here?
                    BigDecimal totalCost = fixedCost.add(variableCost.multiply(new BigDecimal(totalTime.doubleValue()))).setScale(decimals, rounding);
                    // store the cost in the cost component entity
                    Map inputMap = UtilMisc.toMap("userLogin", userLogin, "workEffortId", workEffortId, "cost", new Double(totalCost.doubleValue()));
                    inputMap.put("costComponentTypeId", "ACTUAL_" + workEffortCostCalc.getString("costComponentTypeId"));
                    inputMap.put("costUomId", costComponentCalc.getString("currencyUomId"));
                    inputMap.put("costComponentCalcId", costComponentCalc.getString("costComponentCalcId"));
                    Map outputMap = dispatcher.runSync("createCostComponent", inputMap);

                } else {
                    // invoke the custom method
                    Map inputMap = UtilMisc.toMap("userLogin", userLogin, "workEffort", workEffort);
                    inputMap.put("workEffortCostCalc", workEffortCostCalc);
                    inputMap.put("costComponentCalc", costComponentCalc);
                    Map outputMap = dispatcher.runSync(customMethod.getString("customMethodName"), inputMap);
                }
            }
            // The costs for the materials consumed by the work effort are also created
            BigDecimal totalMaterialsCost = ZERO;
            String prevCurrencyUomId = null;
            List allInventoryAssigned = delegator.findByAnd("WorkEffortAndInventoryAssign", UtilMisc.toMap("workEffortId", workEffortId), UtilMisc.toList("currencyUomId"));
            Iterator allInventoryAssignedIt = allInventoryAssigned.iterator();
            while (allInventoryAssignedIt.hasNext()) {
                GenericValue inventoryAssigned = (GenericValue)allInventoryAssignedIt.next();
                String currencyUomId = inventoryAssigned.getString("currencyUomId");
                if (prevCurrencyUomId != null && !currencyUomId.equals(prevCurrencyUomId)) {
                    if (ZERO.compareTo(totalMaterialsCost) != 0) {
                        Map inputMap = UtilMisc.toMap("userLogin", userLogin, "workEffortId", workEffortId, "cost", new Double(totalMaterialsCost.doubleValue()));
                        inputMap.put("costComponentTypeId", "ACTUAL_MAT_COST");
                        inputMap.put("costUomId", prevCurrencyUomId);
                        Map outputMap = dispatcher.runSync("createCostComponent", inputMap);
                    }
                    totalMaterialsCost = ZERO;
                }
                prevCurrencyUomId = currencyUomId;
                BigDecimal unitCost = inventoryAssigned.getBigDecimal("unitCost");
                BigDecimal quantity = inventoryAssigned.getBigDecimal("quantity");
                totalMaterialsCost = totalMaterialsCost.add(unitCost.multiply(quantity)).setScale(decimals, rounding);
            }
            if (ZERO.compareTo(totalMaterialsCost) != 0) {
                Map inputMap = UtilMisc.toMap("userLogin", userLogin, "workEffortId", workEffortId, "cost", new Double(totalMaterialsCost.doubleValue()));
                inputMap.put("costComponentTypeId", "ACTUAL_MAT_COST");
                inputMap.put("costUomId", prevCurrencyUomId);
                Map outputMap = dispatcher.runSync("createCostComponent", inputMap);
            }
        } catch(Exception exc) {
            return ServiceUtil.returnError("Cannot create costs for work effort [" + workEffortId + "]: " + exc.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }
}
