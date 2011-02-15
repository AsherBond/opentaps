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
package com.opensourcestrategies.financials.accounts;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.accounting.payment.PaymentGatewayServices;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilAccountingTags;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.domain.organization.AccountingTagConfigurationForOrganizationAndUsage;
import org.opentaps.domain.organization.Organization;
import org.opentaps.domain.organization.OrganizationRepositoryInterface;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;

/**
 * Billing accounts related services.
 */
public final class BillingAccountServices {

    private BillingAccountServices() { }

    private static String MODULE = BillingAccountServices.class.getName();

    public static Map<String, Object> createCustomerBillingAccount(DispatchContext dctx, Map<String, ?> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        String organizationPartyId = (String) context.get("organizationPartyId");
        String customerPartyId = (String) context.get("customerPartyId");
        Double accountLimit = (Double) context.get("accountLimit");
        String description = (String) context.get("description");
        Timestamp fromDate = (Timestamp) context.get("fromDate");
        Timestamp thruDate = (Timestamp) context.get("thruDate");

        GenericValue userLogin = (GenericValue) context.get("userLogin");

        try {
            String billingAccountId = null;

            // set the default accountLimit if needed
            if (accountLimit == null) {
                Debug.logWarning("No account limit specified for new billing account, assuming zero", MODULE);
                accountLimit = 0.0;
            }

            // get the currencyUomId of the organizationPartyId
            String baseCurrencyUomId = UtilCommon.getOrgBaseCurrency(organizationPartyId, delegator);
            if (baseCurrencyUomId == null) {
                return ServiceUtil.returnError("No base currency configured for organization [" + organizationPartyId + "]");
            }

            // create the billing account
            Map<String, Object> billingAccountParams = UtilMisc.<String, Object>toMap("accountLimit", accountLimit,
                    "description", description, "fromDate", fromDate, "thruDate", thruDate, "userLogin", userLogin);
            billingAccountParams.put("accountCurrencyUomId", baseCurrencyUomId);
            Map<String, Object> tmpResult = dispatcher.runSync("createBillingAccount", billingAccountParams);
            if (ServiceUtil.isError(tmpResult)) {
                return tmpResult;
            } else {
                billingAccountId = (String) tmpResult.get("billingAccountId");
            }

            // check if the customer party is already a BILL_TO_CUSTOMER.  If not create that role for him first  No caching - make sure it's up to date
            GenericValue customerRole = delegator.findByPrimaryKey("PartyRole", UtilMisc.toMap("partyId", customerPartyId, "roleTypeId", "BILL_TO_CUSTOMER"));
            if (customerRole == null) {
                tmpResult = dispatcher.runSync("createPartyRole", UtilMisc.toMap("partyId", customerPartyId, "roleTypeId", "BILL_TO_CUSTOMER", "userLogin", userLogin));
                if (ServiceUtil.isError(tmpResult)) {
                    return tmpResult;
                }
            }

            // associate the customer party with billing account as BILL_TO_CUSTOMER
            tmpResult = dispatcher.runSync("createBillingAccountRole", UtilMisc.toMap("billingAccountId", billingAccountId, "partyId", customerPartyId,
                    "roleTypeId", "BILL_TO_CUSTOMER", "fromDate", fromDate, "thruDate", thruDate, "userLogin", userLogin));
            if (ServiceUtil.isError(tmpResult)) {
                return tmpResult;
            }

            tmpResult = ServiceUtil.returnSuccess();
            tmpResult.put("billingAccountId", billingAccountId);
            return tmpResult;

        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        } catch (GenericServiceException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }

    }

    public static Map<String, Object> receiveBillingAccountPayment(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Map<String, Object> results = ServiceUtil.returnSuccess();

        try {
            // create a received payment
            ModelService service = dctx.getModelService("createPayment");
            Map<String, Object> input = service.makeValid(context, "IN");
            input.put("statusId", "PMNT_RECEIVED");
            input.put("userLogin", userLogin);
            input.put("effectiveDate", context.get("effectiveDate") == null ? UtilDateTime.nowTimestamp() : context.get("effectiveDate"));
            Map<String, Object> serviceResults = dispatcher.runSync("createPayment", input);
            if (ServiceUtil.isError(serviceResults)) {
                return serviceResults;
            }
            String paymentId = (String) serviceResults.get("paymentId");
            results.put("paymentId", paymentId);

            // create a payment application for this amount
            input = UtilMisc.toMap("paymentId", paymentId, "billingAccountId", context.get("billingAccountId"), "amountApplied", context.get("amount"), "userLogin", userLogin);
            serviceResults = dispatcher.runSync("createPaymentApplication", input);
            if (ServiceUtil.isError(serviceResults)) {
                return serviceResults;
            }

        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError("Failed to receive payment: " + e.getMessage(), MODULE);

        }
        return results;
    }

    public static Map<String, Object> captureBillingAccountPayment(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = UtilCommon.getLocale(context);
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String invoiceId = (String) context.get("invoiceId");
        String billingAccountId = (String) context.get("billingAccountId");
        BigDecimal captureAmount = (BigDecimal) context.get("captureAmount");
        String limitToAvailableBalance = (String) context.get("limitToAvailableBalance");
        String orderId = (String) context.get("orderId");
        Map<String, Object> results = ServiceUtil.returnSuccess();

        try {
            // check the amount against the billing account balance if required
            if ("Y".equalsIgnoreCase(limitToAvailableBalance)) {
                BigDecimal balance = com.opensourcestrategies.financials.accounts.BillingAccountWorker.getBillingAccountAvailableBalance(delegator, billingAccountId);
                if (balance.compareTo(captureAmount) < 0) {
                    return UtilMessage.createAndLogServiceError("Insufficient balance on billing account [" + billingAccountId + "], remaining balance is " + balance + " but tried to capture " + captureAmount, MODULE);
                }
            }

            // get the invoice
            GenericValue invoice = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", invoiceId));

            DomainsLoader domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(userLogin));
            DomainsDirectory domains = domainLoader.loadDomainsDirectory();
            OrganizationRepositoryInterface ori =  domains.getOrganizationDomain().getOrganizationRepository();
            Organization organization = ori.getOrganizationById(invoice.getString("partyIdFrom"));
            // note: tags are set on the Payment if !allocatePaymentTagsToApplications, else they are set on the PaymentApplication
            // so tags are always required here, validate them
            Map<String, String> tags = new HashMap<String, String>();
            UtilAccountingTags.putAllAccountingTags(context, tags);
            List<AccountingTagConfigurationForOrganizationAndUsage> missings = ori.validateTagParameters(tags, organization.getPartyId(), UtilAccountingTags.RECEIPT_PAYMENT_TAG, UtilAccountingTags.ENTITY_TAG_PREFIX);
            if (!missings.isEmpty()) {
                return UtilMessage.createAndLogServiceError("OpentapsError_ServiceErrorRequiredTagNotFound", UtilMisc.toMap("tagName", missings.get(0).getDescription()), locale, MODULE);
            }

            // Note that the partyIdFrom of the Payment should be the partyIdTo of the invoice, since you're receiving a payment from the party you billed
            Map<String, Object> paymentParams = UtilMisc.<String, Object>toMap("paymentTypeId", "CUSTOMER_PAYMENT", "paymentMethodTypeId", "EXT_BILLACT",
                    "partyIdFrom", invoice.getString("partyId"), "partyIdTo", invoice.getString("partyIdFrom"),
                    "statusId", "PMNT_NOT_PAID", "effectiveDate", UtilDateTime.nowTimestamp());
            if (!organization.allocatePaymentTagsToApplications()) {
                UtilAccountingTags.putAllAccountingTags(context, paymentParams);
            }
            paymentParams.put("amount", captureAmount);
            paymentParams.put("currencyUomId", invoice.getString("currencyUomId"));
            paymentParams.put("userLogin", userLogin);
            Map<String, Object> tmpResult = dispatcher.runSync("createPayment", paymentParams);
            if (ServiceUtil.isError(tmpResult)) {
                return tmpResult;
            }

            String paymentId = (String) tmpResult.get("paymentId");
            Map<String, Object> input = UtilMisc.toMap("paymentId", paymentId, "invoiceId", invoiceId, "billingAccountId", billingAccountId, "amountApplied", captureAmount, "checkForOverApplication", Boolean.TRUE, "userLogin", userLogin);
            if (organization.allocatePaymentTagsToApplications()) {
                UtilAccountingTags.putAllAccountingTags(context, input);
            }
            tmpResult = dispatcher.runSync("createPaymentApplication", input);
            if (ServiceUtil.isError(tmpResult)) {
                return tmpResult;
            }
            if (paymentId == null) {
                return UtilMessage.createAndLogServiceError("No payment created for invoice [" + invoiceId + "] and billing account [" + billingAccountId + "]", MODULE);
            }
            results.put("paymentId", paymentId);
            results.put("captureAmount", captureAmount);

            // now set the payment to received
            tmpResult = dispatcher.runSync("setPaymentStatus", UtilMisc.toMap("paymentId", paymentId, "statusId", "PMNT_RECEIVED", "userLogin", userLogin));
            if (ServiceUtil.isError(tmpResult)) {
                return tmpResult;
            }

            if (orderId != null && captureAmount.signum() > 0) {
                // Create a paymentGatewayResponse, if necessary
                GenericValue order = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
                if (order == null) {
                    return UtilMessage.createAndLogServiceError("No paymentGatewayResponse created for invoice [" + invoiceId + "] and billing account [" + billingAccountId + "]: Order with ID [" + orderId + "] not found!", MODULE);
                }
                // See if there's an orderPaymentPreference - there should be only one OPP for EXT_BILLACT per order
                List<GenericValue> orderPaymentPreferences = delegator.findByAnd("OrderPaymentPreference", UtilMisc.toMap("orderId", orderId, "paymentMethodTypeId", "EXT_BILLACT"));
                if (orderPaymentPreferences != null && orderPaymentPreferences.size() > 0) {
                    GenericValue orderPaymentPreference = EntityUtil.getFirst(orderPaymentPreferences);

                    // Check the productStore setting to see if we need to do this explicitly
                    GenericValue productStore = order.getRelatedOne("ProductStore");
                    if (productStore.getString("manualAuthIsCapture") == null || (! productStore.getString("manualAuthIsCapture").equalsIgnoreCase("Y"))) {
                        String responseId = delegator.getNextSeqId("PaymentGatewayResponse");
                        GenericValue pgResponse = delegator.makeValue("PaymentGatewayResponse");
                        pgResponse.set("paymentGatewayResponseId", responseId);
                        pgResponse.set("paymentServiceTypeEnumId", PaymentGatewayServices.CAPTURE_SERVICE_TYPE);
                        pgResponse.set("orderPaymentPreferenceId", orderPaymentPreference.getString("orderPaymentPreferenceId"));
                        pgResponse.set("paymentMethodTypeId", "EXT_BILLACT");
                        pgResponse.set("transCodeEnumId", "PGT_CAPTURE");
                        pgResponse.set("amount", captureAmount);
                        pgResponse.set("currencyUomId", invoice.getString("currencyUomId"));
                        pgResponse.set("transactionDate", UtilDateTime.nowTimestamp());
                        // referenceNum holds the relation to the order.
                        // todo: Extend PaymentGatewayResponse with a billingAccountId field?
                        pgResponse.set("referenceNum", billingAccountId);
                        pgResponse.set("gatewayMessage", "Applied [" + captureAmount + "] towards invoice [" + invoiceId + "] from order [" + orderId + "]");

                        // save the response.
                        tmpResult = dispatcher.runSync("savePaymentGatewayResponse", UtilMisc.toMap("paymentGatewayResponse", pgResponse, "userLogin", userLogin));

                        // Update the orderPaymentPreference
                        orderPaymentPreference.set("statusId", "PAYMENT_SETTLED");
                        orderPaymentPreference.store();

                        results.put("paymentGatewayResponseId", responseId);
                    }
                }
            }
        } catch (GeneralException ex) {
            return UtilMessage.createAndLogServiceError(ex, MODULE);
        }

        return results;
    }

    public static Map<String, Object> calcBillingAccountBalance(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        String billingAccountId = (String) context.get("billingAccountId");
        Map<String, Object> result = ServiceUtil.returnSuccess();

        try {
            GenericValue billingAccount = delegator.findByPrimaryKey("BillingAccount", UtilMisc.toMap("billingAccountId", billingAccountId));
            if (billingAccount == null) {
                return ServiceUtil.returnError("Unable to locate billing account #" + billingAccountId);
            }

            result.put("billingAccount", billingAccount);
            result.put("accountBalance", com.opensourcestrategies.financials.accounts.BillingAccountWorker.getBillingAccountAvailableBalance(billingAccount));

            return result;
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError("Error getting billing account or calculating balance for billing account #" + billingAccountId);
        }
    }

}
