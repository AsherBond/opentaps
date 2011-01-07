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

// A portion of this file may have come from the Apache OFBIZ project

/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/

/* This file has been modified by Open Source Strategies, Inc. */

package com.opensourcestrategies.financials.payroll;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.UtilFormatOut;
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
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;


public class PayrollServices {

    public static String module = PayrollServices.class.getName();
    
    private static final int PAYCHECK_ITEM_SEQUENCE_ID_DIGITS = 5; // this is the number of digits used for paycheckItemSeqId: 00001, 00002...    
    
    public static final String resource = "FinancialsUiLabels";    
    
    public static Map createPaycheck(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);        
        
        if (!security.hasEntityPermission("FINANCIALS", "_EMP_PCCRTE", userLogin)) {
            return UtilMessage.createAndLogServiceError("FinancialsServiceErrorNoPermission", locale, module);
        }                

        String partyIdFrom = (String) context.get("partyIdFrom");
        String paymentTypeId = (String) context.get("paymentTypeId");
        String paymentId = null;        
        try {
            // create the paycheck
            ModelService service = dctx.getModelService("createPayment");
            Map createPaycheckServiceContext = service.makeValid(context, "IN");
            Map createPaycheckServiceResult = dispatcher.runSync(service.name, createPaycheckServiceContext);
            if (ServiceUtil.isError(createPaycheckServiceResult)) {
                return UtilMessage.createAndLogServiceError(createPaycheckServiceResult,
                        ServiceUtil.getErrorMessage(createPaycheckServiceResult), locale, module);
            } 
            paymentId = (String) createPaycheckServiceResult.get("paymentId");            

            // get the paycheckItemTypes associated with this paycheck            
            List paycheckTypeItemTypeAssocList = delegator.findByAnd("PaycheckTypeItemTypeAssocAndPaycheckItemType", 
                    UtilMisc.toMap("paymentTypeId", paymentTypeId), UtilMisc.toList("defaultSeqNum"));
            if (UtilValidate.isEmpty(paycheckTypeItemTypeAssocList)) {
                return UtilMessage.createAndLogServiceError("FinancialsServiceErrorFailedToGetThePaycheckItemTypes",
                        locale, module);
            }
            List paycheckItemTypeIdList = EntityUtil.getFieldListFromEntityList(paycheckTypeItemTypeAssocList,
                    "paycheckItemTypeId", true);

            // create the paycheck items
            int paycheckItemSeqNum = 1;            
            String paycheckItemSeqId = UtilFormatOut.formatPaddedNumber(paycheckItemSeqNum, PAYCHECK_ITEM_SEQUENCE_ID_DIGITS);
            for (Iterator iter = paycheckItemTypeIdList.iterator(); iter.hasNext(); ) {
                String paycheckItemTypeId = (String) iter.next();
                Map createPaycheckItemContext = UtilMisc.toMap("paycheckItemTypeId", paycheckItemTypeId);
                createPaycheckItemContext.put("organizationPartyId", partyIdFrom);
                
                // get the due to party for this type of paycheck item
                GenericValue paycheckItemTypeGlAccount = delegator.findByPrimaryKey("PaycheckItemTypeGlAccount", 
                        UtilMisc.toMap("paycheckItemTypeId", paycheckItemTypeId, "organizationPartyId", partyIdFrom));
                createPaycheckItemContext.put("partyId", (String)paycheckItemTypeGlAccount.get("defaultDueToPartyId"));
                
                // store the paycheck item and increment the sequence number
                try {
                    GenericValue paycheckItem = delegator.makeValue("PaycheckItem", UtilMisc.toMap("paymentId", paymentId, "paycheckItemSeqId", paycheckItemSeqId));
                    paycheckItem.setNonPKFields(createPaycheckItemContext);
                    paycheckItem.create();
                } catch (GenericEntityException e) {
                    return ServiceUtil.returnError(e.getMessage());
                }
                paycheckItemSeqNum++;                
                paycheckItemSeqId = UtilFormatOut.formatPaddedNumber(paycheckItemSeqNum, PAYCHECK_ITEM_SEQUENCE_ID_DIGITS);                
            }                
         } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        } catch (GenericServiceException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }
        
        // return the paymentId of the newly created PayCheck
        Map results = ServiceUtil.returnSuccess();
        results.put("paymentId", paymentId);
        return results;        
    }
    
    public static Map updatePaycheck(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);        
        
        if (!security.hasEntityPermission("FINANCIALS", "_EMP_PCUPDT", userLogin)) {
            return UtilMessage.createAndLogServiceError("FinancialsServiceErrorNoPermission", locale, module);
        }
        
        try {        
            // update the paycheck            
            ModelService service = dctx.getModelService("updatePayment");
            Map updatePaycheckServiceContext = service.makeValid(context, "IN");
            Map updatePaycheckServiceResult = dispatcher.runSync(service.name, updatePaycheckServiceContext);
            if (ServiceUtil.isError(updatePaycheckServiceResult)) {
                return UtilMessage.createAndLogServiceError(updatePaycheckServiceResult, 
                        ServiceUtil.getErrorMessage(updatePaycheckServiceResult), locale, module);
            }                        
        } catch (GenericServiceException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }            
        
        // return the paymentId of the updated PayCheck
        String paymentId = (String) context.get("paymentId");        
        Map results = ServiceUtil.returnSuccess();
        results.put("paymentId", paymentId);
        return results;                            
    }
    
    public static Map updatePaycheckItem(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);        
        
        if (!security.hasEntityPermission("FINANCIALS", "_EMP_PCUPDT", userLogin)) {
            return UtilMessage.createAndLogServiceError("FinancialsServiceErrorNoPermission", locale, module);
        }
        
        String paymentId = (String) context.get("paymentId");
        String paycheckItemSeqId = (String) context.get("paycheckItemSeqId");        
        Double amount = (Double) context.get("amount");
        String partyId = (String) context.get("partyId");   
        String organizationPartyId = (String) context.get("organizationPartyId");
        try {
            // check that the paycheck is in NOT PAID status
            GenericValue payment = delegator.findByPrimaryKey("Payment", UtilMisc.toMap("paymentId", paymentId));
            if (!"PMNT_NOT_PAID".equals(payment.getString("statusId"))) {
                return UtilMessage.createAndLogServiceError("FinancialsServiceErrorPaycheckMustBeInNotPaidStatusToChangePaycheckItem",
                        locale, module);                
            }
            
            // check if the new witholdings amount sum exceeds the paycheck gross amount
            PaycheckReader paycheckReader = new PaycheckReader(paymentId, delegator);
            if (!paycheckReader.isPaycheckItemAmountChangeValid(paycheckItemSeqId, amount)) {
                return UtilMessage.createAndLogServiceError("FinancialsServiceErrorWitholdingsSumExceedsPaycheckGrossAmount",
                        locale, module);
            }        
            
            // update the paycheck item
            Map updatePaycheckItemServiceContext = UtilMisc.toMap("paymentId", paymentId, 
                    "paycheckItemSeqId", paycheckItemSeqId);
            updatePaycheckItemServiceContext.put("amount", amount);
            updatePaycheckItemServiceContext.put("partyId", partyId);
            updatePaycheckItemServiceContext.put("userLogin", userLogin);
            GenericValue paycheckItem = delegator.findByPrimaryKey("PaycheckItem", 
                    UtilMisc.toMap("paymentId", paymentId, "paycheckItemSeqId", paycheckItemSeqId));            
            paycheckItem.setNonPKFields(updatePaycheckItemServiceContext);
            paycheckItem.store();            
        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }            
        return ServiceUtil.returnSuccess();                            
    }
    
}
