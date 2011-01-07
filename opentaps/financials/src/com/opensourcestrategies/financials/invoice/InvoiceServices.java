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

package com.opensourcestrategies.financials.invoice;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import com.opensourcestrategies.financials.accounts.AccountsHelper;
import com.opensourcestrategies.financials.security.FinancialsSecurity;
import com.opensourcestrategies.financials.util.UtilFinancial;
import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;
import org.ofbiz.accounting.invoice.InvoiceWorker;
import org.ofbiz.accounting.payment.PaymentWorker;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilFormatOut;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.order.order.OrderReadHelper;
import org.ofbiz.party.party.PartyHelper;
import org.ofbiz.product.product.ProductWorker;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.service.calendar.RecurrenceInfo;
import org.ofbiz.service.calendar.RecurrenceInfoException;
import org.opentaps.base.entities.InvoiceRole;
import org.opentaps.base.entities.PostalAddress;
import org.opentaps.common.agreement.AgreementInvoiceFactory;
import org.opentaps.common.agreement.UtilAgreement;
import org.opentaps.common.invoice.InvoiceHelper;
import org.opentaps.common.util.UtilAccountingTags;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilDate;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.domain.billing.invoice.Invoice;
import org.opentaps.domain.billing.invoice.InvoiceRepositoryInterface;
import org.opentaps.domain.order.Order;
import org.opentaps.domain.order.OrderItem;
import org.opentaps.domain.order.OrderItemShipGroup;
import org.opentaps.domain.order.OrderRepositoryInterface;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.ofbiz.Repository;

/**
 * InvoiceServices - Services for creating invoices.
 *
 * @author     <a href="mailto:cliberty@opensourcestrategies.com">Chris Liberty</a>
 * @version    $Rev:780 $
 */
public final class InvoiceServices {

    private InvoiceServices() { }

    private static final String MODULE = InvoiceServices.class.getName();

    private static BigDecimal ZERO = BigDecimal.ZERO;
    private static int decimals = UtilNumber.getBigDecimalScale("invoice.decimals");
    private static int rounding = UtilNumber.getBigDecimalRoundingMode("invoice.rounding");
    private static int taxDecimals = UtilNumber.getBigDecimalScale("salestax.calc.decimals");
    private static int taxRounding = UtilNumber.getBigDecimalRoundingMode("salestax.rounding");
    private static final int INVOICE_ITEM_SEQUENCE_ID_DIGITS = 5; // this is the number of digits used for invoiceItemSeqId: 00001, 00002...

    private static final String financialsResource = "financials";

    /**
     * Creates a <code>SALES_INVOICE</code> between the organization and the partner based on a <code>PARTNER_AGREEMENT</code>.
     * If no agreement is specified, the first active partner agreement between the organizationPartyId and partnerPartyId is used.
     * The line items are generated from a provided set of <code>PARTNER_INVOICEs</code> between the partner and its customers.
     * Note that an invoice might not be generated if the total amount is zero or there is nothing to process.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service parameters <code>Map</code>
     * @return the service response <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> createPartnerSalesInvoice(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String agreementId = (String) context.get("agreementId");
        String organizationPartyId = (String) context.get("organizationPartyId");
        String partnerPartyId = (String) context.get("partnerPartyId");
        List<String> invoiceIds = (List<String>) context.get("invoiceIds");

        // validation
        FinancialsSecurity fsecurity = new FinancialsSecurity(security, userLogin, organizationPartyId);
        if (!fsecurity.hasCreatePartnerAgreementPermission()) {
            return UtilMessage.createAndLogServiceError("OpentapsError_PermissionDenied", locale, MODULE);
        }
        if (invoiceIds.size() == 0) {
            return UtilMessage.createAndLogServiceError("FinancialsError_PartnerInvoicesMissing", locale, MODULE);
        }

        // note that since there is no intermediate step where we can store the input invoices and add or remove to that
        // list, we have to return an error if there are any issues, otherwise we're just pushing work on the user
        try {
            // grab each invoice and verify they fit basic criteria, otherwise error out
            Collection<GenericValue> invoices = FastList.newInstance();
            for (String invoiceId : invoiceIds) {
                GenericValue invoice = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", invoiceId));
                if (invoice == null) {
                    return UtilMessage.createServiceError("FinancialsError_InvoiceNotFound", locale, UtilMisc.toMap("invoiceId", invoiceId));
                }
                if (!"PARTNER_INVOICE".equals(invoice.get("invoiceTypeId"))) {
                    return UtilMessage.createServiceError("FinancialsError_InvoiceTypeNotSupported", locale, UtilMisc.toMap("invoiceId", invoiceId, "invoiceTypeId", invoice.get("invoiceTypeId")));
                }
                if (!"INVOICE_READY".equals(invoice.get("statusId"))) {
                    GenericValue requiredStatus = delegator.findByPrimaryKeyCache("StatusItem", UtilMisc.toMap("statusId", "INVOICE_READY"));
                    return UtilMessage.createServiceError("FinancialsError_InvoiceStatusUnsupported", locale, UtilMisc.toMap("invoiceId", invoiceId, "requiredStatus", requiredStatus.get("description", locale)));
                }
                invoices.add(invoice);
            }

            // verify the input invoice or find the earliest defined active partner invoice
            GenericValue agreement = null;
            if (UtilValidate.isNotEmpty(agreementId)) {
                List<EntityCondition> conditions = UtilMisc.<EntityCondition>toList(
                        EntityCondition.makeCondition("agreementId", EntityOperator.EQUALS, agreementId),
                        EntityUtil.getFilterByDateExpr()
                );
                agreement = EntityUtil.getFirst(delegator.findByAnd("Agreement", conditions));
                if (agreement == null) {
                    return UtilMessage.createServiceError("OpentapsError_AgreementNotFoundOrExpired", locale, UtilMisc.toMap("agreementId", agreementId));
                }
                if (!"PARTNER_AGREEMENT".equals(agreement.get("agreementTypeId"))) {
                    GenericValue requiredAgreement = delegator.findByPrimaryKeyCache("AgreementType", UtilMisc.toMap("agreementTypeId", "PARTNER_AGREEMENT"));
                    return UtilMessage.createServiceError("", locale, UtilMisc.toMap("agreementId", agreementId, "requiredAgreement", requiredAgreement.get("description", locale)));
                }
            } else if (UtilValidate.isNotEmpty(organizationPartyId) && UtilValidate.isNotEmpty(partnerPartyId)) {
                List<EntityCondition> conditions = UtilMisc.<EntityCondition>toList(
                        EntityCondition.makeCondition("agreementTypeId", EntityOperator.EQUALS, "PARTNER_AGREEMENT"),
                        EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, organizationPartyId),
                        EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, partnerPartyId),
                        EntityUtil.getFilterByDateExpr()
                );
                agreement = EntityUtil.getFirst(delegator.findByAnd("Agreement", conditions, UtilMisc.toList("fromDate DESC")));
                if (agreement == null) {
                    // TODO move crmsfa's party name generation into commons
                    String partnerName = PartyHelper.getPartyName(delegator, partnerPartyId, false) + "(" + partnerPartyId + ")";
                    return UtilMessage.createServiceError("OpentapsError_AgreementNotFoundForParties", locale, UtilMisc.toMap("partnerName", partnerName));
                }
            } else {
                return UtilMessage.createServiceError("FinancialsError_AgreementOrPartiesMissing", locale);
            }

            // use the factory to handle all the complex details of creating the invoice from an agreement
            String currencyUomId = UtilCommon.getOrgBaseCurrency(agreement.getString("partyIdFrom"), dctx.getDelegator());
            Map<String, Object> results = AgreementInvoiceFactory.createInvoiceFromAgreement(dctx, context, agreement, invoices, "SALES_INVOICE", "PARTNER", currencyUomId, false, true);
            if (ServiceUtil.isError(results)) {
                return results;
            }

            // mark every input invoice as invoiced to partner
            Map<String, Object> input = UtilMisc.<String, Object>toMap("userLogin", context.get("userLogin"), "statusId", "INVOICE_INV_PTNR");
            for (String invoiceId : invoiceIds) {
                input.put("invoiceId", invoiceId);
                Map<String, Object> statusChangeResults = dispatcher.runSync("setInvoiceStatus", input);
                if (ServiceUtil.isError(statusChangeResults)) {
                    return statusChangeResults;
                }
            }

            // return the invoice id that was created, which might be none
            return results;
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
    }

    /**
     * Creates an invoice reflecting finance charges, of type INTEREST_INVOICE, with one InvoiceItem of type INV_INTRST_CHRG.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service parameters <code>Map</code>
     * @return the service response <code>Map</code>
     */
    public static Map<String, Object> createInterestInvoice(DispatchContext dctx, Map<String, ?> context) {

        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        Security security = dctx.getSecurity();

        if (!security.hasEntityPermission("FINANCIALS", "_AR_INCRTE", userLogin)) {
            return UtilMessage.createAndLogServiceError("OpentapsError_PermissionDenied", locale, MODULE);
        }

        String partyIdFrom = (String) context.get("partyIdFrom");
        String partyIdTo = (String) context.get("partyIdTo");
        String description = (String) context.get("description");
        String currencyUomId = (String) context.get("currencyUomId");
        String parentInvoiceId = (String) context.get("parentInvoiceId");
        BigDecimal amount = (BigDecimal) context.get("amount");
        Timestamp dueDate = (Timestamp) context.get("dueDate");
        Timestamp invoiceDate = (Timestamp) context.get("invoiceDate");
        if (invoiceDate == null) {
            invoiceDate = UtilDateTime.nowTimestamp();
        }

        // Don't create an interest invoice if the amount of interest is not positive
        if (amount.signum() <= 0) {
            Debug.logWarning(UtilMessage.expandLabel("FinancialsServiceWarningIgnoreNonPositiveAmount", locale, UtilMisc.toMap("amount", amount, "parentInvoiceId", parentInvoiceId)), MODULE);
            return ServiceUtil.returnSuccess();
        }

        try {
            Map<String, Object> createInvoiceServiceMap = new HashMap<String, Object>();
            createInvoiceServiceMap.put("invoiceTypeId", "INTEREST_INVOICE");
            createInvoiceServiceMap.put("partyIdFrom", partyIdFrom);
            createInvoiceServiceMap.put("partyId", partyIdTo);
            createInvoiceServiceMap.put("statusId", "INVOICE_IN_PROCESS");
            createInvoiceServiceMap.put("invoiceDate", invoiceDate);
            createInvoiceServiceMap.put("description", description);
            createInvoiceServiceMap.put("currencyUomId", currencyUomId);
            createInvoiceServiceMap.put("userLogin", userLogin);
            if (dueDate != null) {
                createInvoiceServiceMap.put("dueDate", dueDate);
            }

            // Create the interest invoice
            Map<String, Object> createInvoiceResult = dispatcher.runSync("createInvoice", createInvoiceServiceMap);
            if (ServiceUtil.isError(createInvoiceResult)) {
                return UtilMessage.createAndLogServiceError(createInvoiceResult, "FinancialsServiceErrorCreatingFinanceCharge", locale, MODULE);
            }
            String invoiceId = (String) createInvoiceResult.get("invoiceId");

            Map<String, Object> createInvoiceItemServiceMap = new HashMap<String, Object>();
            createInvoiceItemServiceMap.put("invoiceId", invoiceId);
            createInvoiceItemServiceMap.put("parentInvoiceId", parentInvoiceId);
            createInvoiceItemServiceMap.put("description", description);
            createInvoiceItemServiceMap.put("invoiceItemTypeId", "INV_INTRST_CHRG");
            createInvoiceItemServiceMap.put("amount", amount);
            createInvoiceItemServiceMap.put("quantity", BigDecimal.ONE);
            createInvoiceItemServiceMap.put("userLogin", userLogin);

            // Create a single InvoiceItem for the finance charge
            Map<String, Object> createInvoiceItemResult = dispatcher.runSync("createInvoiceItem", createInvoiceItemServiceMap);
            if (ServiceUtil.isError(createInvoiceItemResult)) {
                return ServiceUtil.returnError(UtilMessage.expandLabel("FinancialsServiceErrorCreatingFinanceChargeItem", locale, createInvoiceItemServiceMap), null, null, createInvoiceItemResult);
            }

            // Set the invoice status to ready to trigger the GL posting services
            Map<String, Object> setInvoiceStatusMap = UtilMisc.toMap("invoiceId", invoiceId, "statusId", "INVOICE_READY", "userLogin", userLogin);
            Map<String, Object> setInvoiceStatusResult = dispatcher.runSync("setInvoiceStatus", setInvoiceStatusMap);
            if (ServiceUtil.isError(setInvoiceStatusResult)) {
                return ServiceUtil.returnError(UtilMessage.expandLabel("FinancialsServiceErrorFinanceChargeStatus", locale, setInvoiceStatusMap), null, null, setInvoiceStatusResult);
            }

            Map<String, Object> serviceResult = ServiceUtil.returnSuccess();
            serviceResult.put("invoiceId", invoiceId);
            return serviceResult;

        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "FinancialsServiceErrorCreatingFinanceCharge", locale, MODULE);
        }
    }

    /**
     * Sets the default billing address on the given invoice, from the invoice party billing address.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service parameters <code>Map</code>
     * @return the service response <code>Map</code>
     */
    public static Map<String, Object> setInvoiceDefaultBillingAddress(DispatchContext dctx, Map<String, ?> context) {

        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        Security security = dctx.getSecurity();

        if (!(security.hasEntityPermission("FINANCIALS", "_AP_INUPDT", userLogin) || security.hasEntityPermission("FINANCIALS", "_AR_INUPDT", userLogin))) {
            return UtilMessage.createAndLogServiceError("OpentapsError_PermissionDenied", locale, MODULE);
        }

        String invoiceId = (String) context.get("invoiceId");

        try {
            DomainsLoader dl = new DomainsLoader(new Infrastructure(dispatcher), new User(userLogin));
            DomainsDirectory domains = dl.loadDomainsDirectory();
            Invoice invoice = domains.getBillingDomain().getInvoiceRepository().getInvoiceById(invoiceId);

            // get the current invoice billing address and set it to ensure the InvoiceContactMech is in sync
            invoice.setBillingAddress(invoice.getBillingAddress());

        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }

        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put("invoiceId", invoiceId);
        return results;
    }

    /**
     * Updates an invoice and its related billing/shipping address.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service parameters <code>Map</code>
     * @return the service response <code>Map</code>
     */
    public static Map<String, Object> updateInvoiceAndBillingAddress(DispatchContext dctx, Map<String, ?> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        Security security = dctx.getSecurity();

        if (!(security.hasEntityPermission("FINANCIALS", "_AP_INUPDT", userLogin) || security.hasEntityPermission("FINANCIALS", "_AR_INUPDT", userLogin))) {
            return UtilMessage.createAndLogServiceError("OpentapsError_PermissionDenied", locale, MODULE);
        }

        String contactMechId = (String) context.get("contactMechId");
        String shippingContactMechId = (String) context.get("shippingContactMechId");
        String invoiceId = (String) context.get("invoiceId");
        try {
            ModelService service = dctx.getModelService("updateInvoice");
            Map<String, Object> input = service.makeValid(context, "IN");
            Map<String, Object> results = dispatcher.runSync("updateInvoice", input);
            if (ServiceUtil.isError(results)) {
                return results;
            }

            DomainsLoader dl = new DomainsLoader(new Infrastructure(dispatcher), new User(userLogin));
            DomainsDirectory domains = dl.loadDomainsDirectory();
            InvoiceRepositoryInterface repository = domains.getBillingDomain().getInvoiceRepository();
            Invoice invoice = repository.getInvoiceById(invoiceId);
            PostalAddress billingAddress = repository.findOne(PostalAddress.class, repository.map(PostalAddress.Fields.contactMechId, contactMechId));
            PostalAddress shippingAddress = repository.findOne(PostalAddress.class, repository.map(PostalAddress.Fields.contactMechId, shippingContactMechId));

            if (billingAddress != null) {
                // remove existing billing addresses, and set the new one
                invoice.setBillingAddress(billingAddress);
            } else {
                // remove existing billing addresses, and set the default billing address
                invoice.setBillingAddress(null);
                billingAddress = invoice.getBillingAddress();
                if (billingAddress != null) {
                    invoice.setBillingAddress(billingAddress);
                }
            }

            invoice.setShippingAddress(shippingAddress);

            results = ServiceUtil.returnSuccess();
            results.put("invoiceId", invoiceId);
            return results;
        } catch (GeneralException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    /**
     * Sends an email with invoice attached.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service parameters <code>Map</code>
     * @return the service response <code>Map</code>
     */
    public static Map<String, Object> sendInvoiceEmail(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String invoiceId = (String) context.get("invoiceId");
        try {
            // determine which Invoice party field is the organization's
            GenericValue invoice = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", invoiceId));
            String whichPartyField = (UtilFinancial.isReceivableInvoice(invoice) ? "partyIdFrom" : "partyId");

            List<Map<String, Object>> invoiceLines = InvoiceHelper.getInvoiceLinesForPresentation(delegator, invoiceId);

            // context parameters for the invoice fo.ftl
            Map<String, Object> bodyParameters = UtilMisc.<String, Object>toMap("invoiceId", invoiceId, "userLogin", userLogin, "locale", UtilCommon.getLocale(context), "invoiceLines", invoiceLines);
            bodyParameters.putAll(UtilCommon.getOrganizationHeaderInfo(invoice.getString(whichPartyField), delegator));

            ModelService service = dctx.getModelService("sendMailFromScreen");
            Map<String, Object> input = service.makeValid(context, "IN");
            input.put("xslfoAttachScreenLocation", "component://financials/widget/financials/screens/invoices/InvoiceScreens.xml#InvoicePDF");
            input.put("bodyParameters", bodyParameters);
            input.put("attachmentName", "Invoice" + invoiceId + ".pdf");
            dispatcher.runAsync("sendMailFromScreen", input);
            String message = "Email for invoice #" + invoiceId + " scheduled to be sent.";
            return ServiceUtil.returnSuccess(message);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    /**
     * Creates check payments and paymentApplications based on the partyIdFrom, invoiceTypeId
     *  and total amounts applied to invoices for that party/invoiceType.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service parameters <code>Map</code>
     * @return the service response <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> processCheckRun(DispatchContext dctx, Map<String, ?> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        Security security = dctx.getSecurity();

        if (!security.hasEntityPermission("FINANCIALS", "_AR_PCRTE", userLogin)) {
            return UtilMessage.createAndLogServiceError("OpentapsError_PermissionDenied", locale, MODULE);
        }

        Map invoiceIds = (Map) context.get("invoiceIds");
        Map invoiceTypeIds = (Map) context.get("invoiceTypeIds");
        Map partyIdsFrom = (Map) context.get("partyIdsFrom");
        Map currencyUomIds = (Map) context.get("currencyUomIds");
        Map amounts = (Map) context.get("amounts");
        Map _rowSubmits = (Map) context.get("_rowSubmit");
        String organizationPartyId = (String) context.get("organizationPartyId");
        String paymentMethodId = (String) context.get("paymentMethodId");

        Map paymentTypeIds = UtilMisc.toMap("PURCHASE_INVOICE", "VENDOR_PAYMENT", "COMMISSION_INVOICE", "COMMISSION_PAYMENT", "CUST_RTN_INVOICE", "CUSTOMER_REFUND");

        // Make sure there's only one currency involved
        Set distinctCurrencyUomIds = new HashSet(currencyUomIds.values());
        if (distinctCurrencyUomIds.size() > 1) {
            return UtilMessage.createAndLogServiceError("FinancialsServiceErrorCheckRunMultipleCurrencies", locale, MODULE);
        }
        String currencyUomId = (String) distinctCurrencyUomIds.toArray()[0];

        // Make sure we can increment the initalCheckNumber
        String currentCheckNumberStr = null;
        String initialCheckNumberStr = (String) context.get("initialCheckNumber");
        int currentCheckNumber = 0;
        try {
            currentCheckNumber = Integer.parseInt(initialCheckNumberStr);
        } catch (NumberFormatException e) {
            return UtilMessage.createAndLogServiceError(e, "FinancialsServiceErrorCheckRunNonNumericInitialCheckNumber", locale, MODULE);
        }

        Map partyInvoices = new HashMap();

        // Construct a map of partyId -> invoiceTypeId -> invoiceId : invoiceAmount
        Iterator<String> rit = _rowSubmits.keySet().iterator();
        while (rit.hasNext()) {

            String rowNumber = rit.next();

            // Ignore unchecked rows
            if (!"Y".equals(_rowSubmits.get(rowNumber))) {
                continue;
            }

            String invoiceId = (String) invoiceIds.get(rowNumber);
            String partyIdFrom = (String) partyIdsFrom.get(rowNumber);
            if (UtilValidate.isEmpty(invoiceId) || UtilValidate.isEmpty(partyIdFrom)) {
                return UtilMessage.createAndLogServiceError("FinancialsServiceErrorCheckRunMissingInvoiceData", UtilMisc.toMap("invoiceId", invoiceId), locale, MODULE);
            }

            String invoiceTypeId = (String) invoiceTypeIds.get(rowNumber);
            String paymentTypeId = (String) paymentTypeIds.get(invoiceTypeId);
            if (UtilValidate.isEmpty(invoiceTypeId) || UtilValidate.isEmpty(paymentTypeId)) {
                return UtilMessage.createAndLogServiceError("FinancialsServiceErrorCheckRunUnknownInvoiceType", UtilMisc.toMap("invoiceId", invoiceId, "invoiceTypeId", invoiceTypeId), locale, MODULE);
            }

            BigDecimal invoiceAmount = ZERO;

            // Ignore empty amounts
            if (UtilValidate.isEmpty((String) amounts.get(rowNumber))) {
                continue;
            }

            try {
                invoiceAmount = invoiceAmount.add(UtilCommon.parseLocalizedNumber(locale, (String) amounts.get(rowNumber))).setScale(decimals, rounding);
            } catch (ParseException e) {
                return ServiceUtil.returnError(e.getMessage());
            }

            // Ignore zero amounts
            if (invoiceAmount.signum() <= 0) {
                continue;
            }

            Map partyInvoiceTypes = UtilValidate.isEmpty(partyInvoices.get(partyIdFrom)) ? new HashMap() : (Map) partyInvoices.get(partyIdFrom);

            // Add the amount for the invoice to the amount being paid to the party
            Map invoiceAmounts = UtilValidate.isEmpty(partyInvoiceTypes.get(invoiceTypeId)) ? new HashMap() : (Map) partyInvoiceTypes.get(invoiceTypeId);
            invoiceAmounts.put(invoiceId, invoiceAmount);

            partyInvoiceTypes.put(invoiceTypeId, invoiceAmounts);
            partyInvoices.put(partyIdFrom, partyInvoiceTypes);

        }

        Map paymentContext = FastMap.newInstance();
        paymentContext.put("paymentMethodTypeId", "COMPANY_CHECK");
        paymentContext.put("paymentMethodId", paymentMethodId);
        paymentContext.put("statusId", "PMNT_NOT_PAID");
        paymentContext.put("currencyUomId", currencyUomId);
        paymentContext.put("userLogin", userLogin);

        Map paymentApplicationContext = FastMap.newInstance();
        paymentApplicationContext.put("userLogin", userLogin);
        UtilAccountingTags.addTagParameters(context, paymentApplicationContext, "acctgTagEnumId", "acctgTagEnumId");

        try {

            Iterator pii = partyInvoices.keySet().iterator();
            while (pii.hasNext()) {
                String partyId = (String) pii.next();
                Map partyInvoiceTypes = (Map) partyInvoices.get(partyId);

                Iterator piti = partyInvoiceTypes.keySet().iterator();
                while (piti.hasNext()) {
                    String invoiceTypeId = (String) piti.next();
                    Map invoiceAmounts = (Map) partyInvoiceTypes.get(invoiceTypeId);

                    BigDecimal partyAmount = ZERO;
                    Iterator iamit = invoiceAmounts.values().iterator();
                    while (iamit.hasNext()) {
                        partyAmount = partyAmount.add((BigDecimal) iamit.next());
                    }

                    // Pad the current check number with zeros if the initial check number had leading zeros
                    currentCheckNumberStr = "" + currentCheckNumber;
                    if (currentCheckNumberStr.length() < initialCheckNumberStr.length()) {
                        currentCheckNumberStr = UtilFormatOut.padString(currentCheckNumberStr, initialCheckNumberStr.length(), false, '0');
                    }

                    paymentContext.put("paymentTypeId", paymentTypeIds.get(invoiceTypeId));
                    paymentContext.put("partyIdTo", partyId);
                    paymentContext.put("partyIdFrom", organizationPartyId);
                    paymentContext.put("paymentRefNum", currentCheckNumberStr);
                    paymentContext.put("amount", partyAmount);
                    UtilAccountingTags.addTagParameters(context, paymentContext, "acctgTagEnumId", "acctgTagEnumId");

                    // Create the payment
                    Map createPaymentResult = dispatcher.runSync("createPayment", paymentContext);
                    if (ServiceUtil.isError(createPaymentResult)) {
                        Debug.logError(ServiceUtil.getErrorMessage(createPaymentResult), MODULE);
                        return createPaymentResult;
                    }
                    String paymentId = (String) createPaymentResult.get("paymentId");

                    //Increment the check number
                    currentCheckNumber++;

                    Iterator iai = invoiceAmounts.keySet().iterator();
                    while (iai.hasNext()) {
                        String invoiceId = (String) iai.next();
                        BigDecimal amountApplied = (BigDecimal) invoiceAmounts.get(invoiceId);

                        paymentApplicationContext.put("paymentId", paymentId);
                        paymentApplicationContext.put("invoiceId", invoiceId);
                        paymentApplicationContext.put("amountApplied", amountApplied);

                        // Create the payment application
                        Map createPaymentApplicationResult = dispatcher.runSync("createPaymentApplication", paymentApplicationContext);
                        if (ServiceUtil.isError(createPaymentApplicationResult)) {
                            Debug.logError(ServiceUtil.getErrorMessage(createPaymentApplicationResult), MODULE);
                            return createPaymentApplicationResult;
                        }
                    }
                }
            }

            Map results = ServiceUtil.returnSuccess();

            // Pad the current check number with zeros if the initial check number had leading zeros
            currentCheckNumberStr = "" + currentCheckNumber;
            if (currentCheckNumberStr.length() < initialCheckNumberStr.length()) {
                currentCheckNumberStr = UtilFormatOut.padString(currentCheckNumberStr, initialCheckNumberStr.length(), false, '0');
            }

            // Add the last check number to the outgoing context for the next run
            results.put("initialCheckNumber", currentCheckNumberStr);

            return results;
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    /**
     * Updates the <code>InvoiceRecurrence</code> values for an invoice.
     * Requires <code>FINANCIALS_RECUR_INV</code> permission.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service parameters <code>Map</code>
     * @return the service response <code>Map</code>
     */
    public static Map<String, Object> updateInvoiceRecurrence(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        String invoiceId = (String) context.get("invoiceId");
        String recurrenceRuleId = (String) context.get("recurrenceRuleId");
        String recurrenceInfoId = (String) context.get("recurrenceInfoId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        Security security = dctx.getSecurity();
        Timestamp now = UtilDateTime.nowTimestamp();

        try {

            if (!security.hasEntityPermission("FINANCIALS", "_RECUR_INV", userLogin)) {
                return UtilMessage.createAndLogServiceError("OpentapsError_PermissionDenied", locale, MODULE);
            }

            // Get the current set of involved records
            GenericValue invoice = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", invoiceId));
            GenericValue invoiceRecurrence = delegator.findByPrimaryKey("InvoiceRecurrence", UtilMisc.toMap("invoiceId", invoiceId, "recurrenceInfoId", recurrenceInfoId));
            GenericValue recurrenceInfo = null;
            if (UtilValidate.isNotEmpty(recurrenceInfoId)) {
                recurrenceInfo = delegator.findByPrimaryKey("RecurrenceInfo", UtilMisc.toMap("recurrenceInfoId", recurrenceInfoId));
            }
            GenericValue recurrenceRule = null;
            if (UtilValidate.isNotEmpty(recurrenceInfo)) {
                recurrenceRule = recurrenceInfo.getRelatedOne("RecurrenceRule");
            }
            if (UtilValidate.isNotEmpty(recurrenceInfo)) {

                // Make sure we're changing something - IE that recurrenceRuleId is not unchanged
                if (UtilValidate.isNotEmpty(recurrenceRuleId) && UtilValidate.isNotEmpty(recurrenceRule) && recurrenceRuleId.equals(recurrenceRule.getString("recurrenceRuleId"))) {
                    return ServiceUtil.returnSuccess();
                }

                // Expire the current invoiceRecurrence
                invoiceRecurrence.set("thruDate", now);
                delegator.store(invoiceRecurrence);
            }

            if (UtilValidate.isEmpty(recurrenceRuleId)) {
                return ServiceUtil.returnSuccess();
            }

            // Make a new RecurrenceInfo based on the recurrenceRuleId provided
            GenericValue newRecurrenceInfoVO = delegator.makeValue("RecurrenceInfo", UtilMisc.toMap("startDateTime", invoice.getTimestamp("invoiceDate"), "recurrenceRuleId", recurrenceRuleId, "recurrenceCount", new Long(1)));
            RecurrenceInfo newRecurrenceInfo = new RecurrenceInfo(newRecurrenceInfoVO);
            String newRecurrenceInfoId = delegator.getNextSeqId("RecurrenceInfo");
            newRecurrenceInfoVO.set("recurrenceInfoId", newRecurrenceInfoId);
            long startDateTime = newRecurrenceInfo.next();
            newRecurrenceInfoVO.set("startDateTime", startDateTime > 0 ? new Timestamp(startDateTime) : UtilDateTime.nowTimestamp());
            newRecurrenceInfoVO.set("recurrenceCount", new Long(0));
            delegator.create(newRecurrenceInfoVO);

            // Make a new InvoiceRecurrence based on the new RecurrenceInfo value
            delegator.create("InvoiceRecurrence", UtilMisc.toMap("invoiceId", invoiceId, "recurrenceInfoId", newRecurrenceInfoId, "lastRecurrenceDate", invoice.getTimestamp("invoiceDate"), "fromDate", now));

            return ServiceUtil.returnSuccess();
        } catch (RecurrenceInfoException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    /**
     * Service to be run by the job scheduler.
     * Responsible for automatically creating copies of invoices which are set to recur, but looking in
     *  the <code>InvoiceRecurrence</code> entity and comparing the lastRecurrenceDate field and the defined recurrenceInfo with the current time.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service parameters <code>Map</code>
     * @return the service response <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> runInvoiceRecurrence(DispatchContext dctx, Map<String, ?> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        TimeZone timeZone = UtilCommon.getTimeZone(context);

        try {
            Timestamp now = UtilDateTime.nowTimestamp();

            EntityCondition fromDateConditionList = EntityCondition.makeCondition(EntityOperator.AND, EntityCondition.makeCondition("fromDate", EntityOperator.NOT_EQUAL, null), EntityCondition.makeCondition("fromDate", EntityOperator.LESS_THAN_EQUAL_TO, now));

            EntityCondition thruDateConditionList = EntityCondition.makeCondition(EntityOperator.OR, EntityCondition.makeCondition("thruDate", EntityOperator.EQUALS, null), EntityCondition.makeCondition("thruDate", EntityOperator.GREATER_THAN, now));

            EntityCondition fullConditionList = EntityCondition.makeCondition(EntityOperator.AND, fromDateConditionList, thruDateConditionList);

            TransactionUtil.begin();
            EntityListIterator invoiceRecurrences = delegator.findListIteratorByCondition("InvoiceRecurrence", fullConditionList, null, null);
            TransactionUtil.commit();

            GenericValue invoiceRecurrence = null;
            Map cloneInvoiceResult = null;
            while ((invoiceRecurrence = invoiceRecurrences.next()) != null) {

                String invoiceId = invoiceRecurrence.getString("invoiceId");

                GenericValue recurrenceInfoVO = invoiceRecurrence.getRelatedOne("RecurrenceInfo");

                Timestamp lastRecurrenceDate = invoiceRecurrence.getTimestamp("lastRecurrenceDate");
                if (UtilValidate.isEmpty(lastRecurrenceDate)) {
                    lastRecurrenceDate = recurrenceInfoVO.getTimestamp("startDate");
                }

                // Ignore invoiceRecurrences with future lastRecurrenceDates
                if (UtilValidate.isEmpty(lastRecurrenceDate) || lastRecurrenceDate.after(now)) {
                    UtilMessage.logServiceInfo("FinancialsServiceErrorRunInvoiceRecurrenceIgnoreFutureLastRecurrence", locale, MODULE);
                    continue;
                }

                // Construct the RecurrenceInfo object
                RecurrenceInfo recurrenceInfo = null;
                try {
                    recurrenceInfo = new RecurrenceInfo(recurrenceInfoVO);
                } catch (RecurrenceInfoException e) {
                    UtilMessage.logServiceInfo("FinancialsServiceErrorRunInvoiceRecurrenceInvalid", locale, MODULE);
                    continue;
                }
                if (UtilValidate.isEmpty(recurrenceInfo)) {
                    UtilMessage.logServiceInfo("FinancialsServiceErrorRunInvoiceRecurrenceInvalid", locale, MODULE);
                    continue;
                }

                // Ignore invoiceRecurrences with future next recurrences
                Timestamp nextRecurrenceDate = new Timestamp(recurrenceInfo.next(UtilValidate.isEmpty(lastRecurrenceDate) ? now.getTime() : lastRecurrenceDate.getTime()));
                if (UtilDateTime.getDayStart(nextRecurrenceDate, timeZone, locale).after(now)) {
                    UtilMessage.logServiceInfo("FinancialsServiceErrorRunInvoiceRecurrenceIgnoreFutureNextRecurrence", locale, MODULE);
                    continue;
                }

                cloneInvoiceResult = null;
                try {
                    TransactionUtil.begin();
                    cloneInvoiceResult = dispatcher.runSync("cloneInvoice", UtilMisc.toMap("invoiceId", invoiceRecurrence.getString("invoiceId"), "userLogin", userLogin));
                    if (ServiceUtil.isError(cloneInvoiceResult)) {
                        UtilMessage.logServiceWarning("FinancialsServiceErrorRunInvoiceRecurrence", UtilMisc.toMap("invoiceId", invoiceId), locale, MODULE);
                        TransactionUtil.rollback();
                    }

                    // Update the lastRecurrenceDate on the InvoiceRecurrence value
                    invoiceRecurrence.set("lastRecurrenceDate", now);
                    delegator.store(invoiceRecurrence);

                    // Update the recurrenceCount on the RecurrenceInfo value
                    if (UtilValidate.isEmpty(recurrenceInfoVO.get("recurrenceCount"))) {
                        recurrenceInfoVO.set("recurrenceCount", new Long(1));
                    } else {
                        recurrenceInfoVO.set("recurrenceCount", new Long(recurrenceInfoVO.getLong("recurrenceCount").longValue() + 1));
                    }
                    delegator.store(recurrenceInfoVO);

                    TransactionUtil.commit();
                } catch (Exception e) {
                    TransactionUtil.rollback();
                    UtilMessage.logServiceWarning("FinancialsServiceErrorRunInvoiceRecurrence", UtilMisc.toMap("invoiceId", invoiceId), locale, MODULE);
                }
            }
            invoiceRecurrences.close();

            return ServiceUtil.returnSuccess();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    /**
     * Creates a deep copy of an invoice.
     * Values in <code>Invoice</code>, <code>InvoiceAttribute</code>, <code>InvoiceItem</code>, <code>InvoiceItemAttribute</code>, <code>InvoiceTerm</code> and <code>InvoiceTermAttribute</code> are copied and <code>InvoiceStatus</code> entries are generated.
     * <code>InvoiceDate</code> is set to the current time and dueDate is set ahead of invoiceDate by an equal amount to the old invoice.
     * Requires <code>FINANCIALS_RECUR_INV</code> permission.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service parameters <code>Map</code>
     * @return the service response <code>Map</code>
     */
    public static Map<String, Object> cloneInvoice(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String invoiceId = (String) context.get("invoiceId");
        Locale locale = UtilCommon.getLocale(context);
        Security security = dctx.getSecurity();

        Timestamp now = UtilDateTime.nowTimestamp();

        try {

            if (!security.hasEntityPermission("FINANCIALS", "_RECUR_INV", userLogin)) {
                return UtilMessage.createAndLogServiceError("OpentapsError_PermissionDenied", locale, MODULE);
            }

            // Get the invoice
            GenericValue invoice = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", invoiceId));

            // Construct the new invoice from the old invoice
            String descriptionString = UtilMessage.expandLabel("FinancialsClonedInvoiceDescription", locale, UtilMisc.toMap("invoiceId", invoiceId));
            GenericValue newInvoice = delegator.makeValue("Invoice", invoice);
            newInvoice.set("statusId", "INVOICE_IN_PROCESS");
            newInvoice.remove("paidDate");
            newInvoice.set("invoiceDate", now);
            newInvoice.remove("dueDate");
            newInvoice.set("description", UtilValidate.isNotEmpty(invoice.getString("description")) ? invoice.getString("description") + "(" + descriptionString + ")" : descriptionString);
            if (UtilValidate.isNotEmpty(invoice.getString("invoiceDate")) && UtilValidate.isNotEmpty(invoice.getString("dueDate"))) {

                // Set the new due date ahead of the new invoice date by the same interval as the old due date was ahead of the old invoice date
                Timestamp invoiceDate = invoice.getTimestamp("invoiceDate");
                Timestamp dueDate = invoice.getTimestamp("dueDate");
                Timestamp newDueDate = new Timestamp(now.getTime() + (dueDate.getTime() - invoiceDate.getTime()));
                newInvoice.set("dueDate", newDueDate);
            }

            String newInvoiceId = delegator.getNextSeqId("Invoice");
            newInvoice.set("invoiceId", newInvoiceId);
            delegator.create(newInvoice);

            // Create the initial InvoiceStatus
            delegator.create("InvoiceStatus", UtilMisc.toMap("invoiceId", newInvoiceId, "statusId", "INVOICE_IN_PROCESS", "statusDate", now));

            updateValues(invoice.getRelated("InvoiceAttribute"), "invoiceId", newInvoiceId, delegator);
            updateValues(invoice.getRelated("InvoiceItem"), "invoiceId", newInvoiceId, delegator);
            updateValues(delegator.findByAnd("InvoiceItemAttribute", UtilMisc.toMap("invoiceId", invoiceId)), "invoiceId", newInvoiceId, delegator);
            updateValues(invoice.getRelated("InvoiceContactMech"), "invoiceId", newInvoiceId, delegator);

            List<GenericValue> invoiceTerms = invoice.getRelated("InvoiceTerm");
            for (GenericValue invoiceTerm : invoiceTerms) {
                List<GenericValue> invoiceTermAttributes = invoiceTerm.getRelated("InvoiceTermAttribute");
                String newInvoiceTermId = delegator.getNextSeqId("InvoiceTerm");
                invoiceTerm.set("invoiceId", newInvoiceId);
                invoiceTerm.set("invoiceTermId", newInvoiceTermId);
                delegator.create(invoiceTerm);
                updateValues(invoiceTermAttributes, "invoiceTermId", newInvoiceTermId, delegator);
            }

            // Update the invoice status for non-purchase orders
            if (!"PURCHASE_INVOICE".equals(invoice.getString("invoiceTypeId"))) {
                Map<String, Object> setInvoiceStatusResult = dispatcher.runSync("setInvoiceStatus", UtilMisc.toMap("invoiceId", invoiceId, "statusId", "INVOICE_READY", "userLogin", userLogin));
                if (ServiceUtil.isError(setInvoiceStatusResult)) {
                    return setInvoiceStatusResult;
                }
            }

            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("invoiceId", newInvoiceId);
            return result;
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    private static void updateValues(List<GenericValue> values, String fieldName, String newFieldValue, Delegator delegator) throws GenericEntityException {
        if (values == null) {
            return;
        }
        for (GenericValue value : values) {
            value.set(fieldName, newFieldValue);
            delegator.create(value);
        }
    }

    /**
     * Converts an Accounts Payable invoice to a Billing Account. (Also known as Store Credit).
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service parameters <code>Map</code>
     * @return the service response <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map convertToBillingAccount(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        Security security = dctx.getSecurity();

        String invoiceId = (String) context.get("invoiceId");
        String billingAccountId = (String) context.get("billingAccountId");
        BigDecimal amount = (BigDecimal) context.get("amount");
        Timestamp now = UtilDateTime.nowTimestamp();
        try {
            if (!security.hasEntityPermission("FINANCIALS", "_AP_INUPDT", userLogin)) {
                return UtilMessage.createAndLogServiceError("OpentapsError_PermissionDenied", locale, MODULE);
            }
            GenericValue invoice = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", invoiceId));

            // verify that the invoice has value
            BigDecimal total = InvoiceWorker.getInvoiceNotApplied(invoice);
            if (total.signum() != 1) {
                return UtilMessage.createAndLogServiceError("FinancialsErrorNoInvoiceValue", UtilMisc.toMap("invoiceId", invoiceId), locale, MODULE);
            }
            if ((amount == null) || (amount.compareTo(total) == 1)) {
                amount = total;
            }

            // verify that the invoice type is supported
            String paymentTypeId = null;
            if ("PURCHASE_INVOICE".equals(invoice.get("invoiceTypeId"))) {
                paymentTypeId = "VENDOR_PAYMENT";
            } else if ("COMMISSION_INVOICE".equals(invoice.get("invoiceTypeId"))) {
                paymentTypeId = "COMMISSION_PAYMENT";
            } else if ("CUST_RTN_INVOICE".equals(invoice.get("invoiceTypeId"))) {
                paymentTypeId = "CUSTOMER_REFUND";
            }
            if (paymentTypeId == null) {
                return UtilMessage.createAndLogServiceError("FinancialsErrorInvoiceTypeNotSupported", UtilMisc.toMap("invoiceTypeId", invoice.get("invoiceTypeId")), locale, MODULE);
            }

            // create a billing account if none specified (the service should create the appropriate role)
            if (billingAccountId == null) {
                Map input = UtilMisc.toMap("userLogin", userLogin, "fromDate", now, "roleTypeId", "BILL_TO_CUSTOMER", "partyId", invoice.get("partyIdFrom"));
                input.put("accountCurrencyUomId", invoice.get("currencyUomId"));
                input.put("accountLimit", new BigDecimal(0.0));
                input.put("description", UtilMessage.expandLabel("FinancialsCreditForInvoice", UtilMisc.toMap("invoiceId", invoiceId), locale));
                Map results = dispatcher.runSync("createBillingAccount", input);
                if (ServiceUtil.isError(results)) {
                    return results;
                }
                billingAccountId = (String) results.get("billingAccountId");
            } else {
                // check that the billing account is in the same currency as the invoice
                GenericValue billingAccount = delegator.findByPrimaryKey("BillingAccount", UtilMisc.toMap("billingAccountId", billingAccountId));
                if (UtilValidate.isEmpty(billingAccount)) {
                    return UtilMessage.createAndLogServiceError("FinancialsErrorBillingAccountNotFound", UtilMisc.toMap("billingAccountId", billingAccountId), locale, MODULE);
                } else {
                    // converting the currency would be complicated so just throw an error for now
                    if (!billingAccount.getString("accountCurrencyUomId").equals(invoice.getString("currencyUomId"))) {
                        return UtilMessage.createAndLogServiceError("FinancialsErrorBillingAccountCurrencyDifferent",
                                           UtilMisc.toMap("billingAccountId", billingAccountId, "billingAccountCurrency", billingAccount.getString("accountCurrencyUomId"),
                                                          "invoiceId", invoiceId, "invoiceCurrencyUomId", invoice.getString("currencyUomId")), locale, MODULE);
                    }
                }
            }

            // create a payment
            Map input = UtilMisc.toMap("userLogin", userLogin, "partyIdFrom", invoice.get("partyId"), "partyIdTo", invoice.get("partyIdFrom"), "paymentTypeId", paymentTypeId);
            input.put("currencyUomId", invoice.getString("currencyUomId"));
            input.put("paymentMethodTypeId", "EXT_BILLACT");
            input.put("statusId", "PMNT_NOT_PAID");
            input.put("amount", amount);
            Map results = dispatcher.runSync("createPayment", input);
            if (ServiceUtil.isError(results)) {
                return results;
            }
            String paymentId = (String) results.get("paymentId");

            // make application to invoie for invoice total
            input = UtilMisc.toMap("userLogin", userLogin, "invoiceId", invoiceId, "paymentId", paymentId, "amountApplied", amount);
            results = dispatcher.runSync("createPaymentApplication", input);
            if (ServiceUtil.isError(results)) {
                return results;
            }

            // make application to billing account for input amount
            input = UtilMisc.toMap("userLogin", userLogin, "billingAccountId", billingAccountId, "paymentId", paymentId, "amountApplied", amount);
            results = dispatcher.runSync("createPaymentApplication", input);
            if (ServiceUtil.isError(results)) {
                return results;
            }

            // send the payment, so that the SECA will trigger the invoice as paid if fully paid
            results = dispatcher.runSync("setPaymentStatus", UtilMisc.toMap("userLogin", userLogin, "paymentId", paymentId, "statusId", "PMNT_SENT"));
            if (ServiceUtil.isError(results)) {
                return results;
            }

            results = ServiceUtil.returnSuccess();
            results.put("billingAccountId", billingAccountId);
            return results;
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    /**
     * Creates <code>InvoiceTerms</code> based on the <code>AgreementTerms</code> between the parties of the invoice.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service parameters <code>Map</code>
     * @return the service response <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map createInvoiceTerms(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String invoiceId = (String) context.get("invoiceId");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        try {
            // Get the agreement terms that apply to the invoice
            List terms = InvoiceHelper.getAgreementTermsForInvoice(delegator, invoiceId);

            // make InvoiceTerms based off these AgreementTerms
            for (Iterator iter = terms.iterator(); iter.hasNext();) {
                GenericValue term = (GenericValue) iter.next();

                // Assemble the context for the createInvoiceTerm service from the AgreementTerm
                ModelService modelService = dctx.getModelService("createInvoiceTerm");
                Map createInvoiceTermContext = modelService.makeValid(term, "IN");
                createInvoiceTermContext.putAll(UtilMisc.toMap("userLogin", userLogin, "locale", locale, "invoiceId", invoiceId));

                // Call the createInvoiceTerm service
                Map createInvoiceTermResult = dispatcher.runSync("createInvoiceTerm", createInvoiceTermContext);
                if (ServiceUtil.isFailure(createInvoiceTermResult)) {
                    String logMessage = UtilMessage.expandLabel("FinancialsServiceErrorSkippingAgreementTerm",
                            UtilMisc.toMap("invoiceId", invoiceId, "agreementTermId", term.get("agreementTermId"), "termTypeId", term.get("termTypeId")), locale)
                            + ": " + ServiceUtil.getErrorMessage(createInvoiceTermResult);
                    Debug.logInfo(logMessage, MODULE);
                } else if (ServiceUtil.isError(createInvoiceTermResult)) {
                    Debug.logError(ServiceUtil.getErrorMessage(createInvoiceTermResult), MODULE);
                    return createInvoiceTermResult;
                }
            }

            return ServiceUtil.returnSuccess();

        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    /**
     * Creates InvoiceTerms based on the AgreementTerms between the parties of the invoice.
     * Will return failure if a term of the same type exists, or if the term to be created
     *  is a payment term (governed by InvoiceHelper.invoiceDueDateAgreementTermTypeIds) and a term of that sort exists.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service parameters <code>Map</code>
     * @return the service response <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map createInvoiceTerm(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        Security security = dctx.getSecurity();

        if ((!security.hasEntityPermission("FINANCIALS", "_AP_INUPDT", userLogin)) && (!security.hasEntityPermission("FINANCIALS", "_AR_INUPDT", userLogin))) {
            return UtilMessage.createAndLogServiceError("FinancialsServiceErrorNoPermission", locale, MODULE);
        }

        String invoiceId = (String) context.get("invoiceId");

        try {

            // Get the invoice terms
            List invoiceTerms = delegator.findByAnd("InvoiceTerm", UtilMisc.toMap("invoiceId", invoiceId));

            // Check to make sure that the term doesn't already exist
            List existingTerms = EntityUtil.filterByAnd(invoiceTerms, UtilMisc.toMap("termTypeId", context.get("termTypeId")));
            if (UtilValidate.isNotEmpty(existingTerms)) {
                return UtilMessage.createAndLogServiceFailure("FinancialsServiceErrorInvoiceTermOfTypeExists", context, locale, MODULE);
            }

            // There should only be one term allowed to govern payment due date, so exclude these type of terms if one of them
            //  already exists for the invoice
            List dueDateTerms = EntityUtil.filterByCondition(invoiceTerms, EntityCondition.makeCondition("termTypeId", EntityOperator.IN, InvoiceHelper.invoiceDueDateAgreementTermTypeIds));
            if (InvoiceHelper.invoiceDueDateAgreementTermTypeIds.contains(context.get("termTypeId")) && UtilValidate.isNotEmpty(dueDateTerms)) {
                return UtilMessage.createAndLogServiceFailure("FinancialsServiceErrorInvoiceTermOfPaymentTypeExists", context, locale, MODULE);
            }

            // Create the term
            String invoiceTermId = delegator.getNextSeqId("InvoiceTerm");
            GenericValue invoiceTerm = delegator.makeValue("InvoiceTerm", UtilMisc.toMap("invoiceTermId", invoiceTermId));
            invoiceTerm.setNonPKFields(context);
            invoiceTerm.create();

        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Updates an InvoiceTerm.
     * Will return failure if a term of the new type exists, or if the new term type
     *  is a payment term (governed by InvoiceHelper.invoiceDueDateAgreementTermTypeIds) and a term of that sort exists.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service parameters <code>Map</code>
     * @return the service response <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map updateInvoiceTerm(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        Security security = dctx.getSecurity();

        if ((!security.hasEntityPermission("FINANCIALS", "_AP_INUPDT", userLogin)) && (!security.hasEntityPermission("FINANCIALS", "_AR_INUPDT", userLogin))) {
            return UtilMessage.createAndLogServiceError("FinancialsServiceErrorNoPermission", locale, MODULE);
        }

        String invoiceTermId = (String) context.get("invoiceTermId");

        try {

            // Get the invoiceTerm
            GenericValue invoiceTerm = delegator.findByPrimaryKey("InvoiceTerm", UtilMisc.toMap("invoiceTermId", invoiceTermId));
            String invoiceId = invoiceTerm.getString("invoiceId");

            // Get the other invoice terms
            List invoiceTerms = delegator.findByAnd("InvoiceTerm", UtilMisc.toMap("invoiceId", invoiceId));
            invoiceTerms = EntityUtil.filterOutByCondition(invoiceTerms, EntityCondition.makeCondition("invoiceTermId", EntityOperator.EQUALS, invoiceTermId));

            // If the termTypeId is being changed, check to make sure that a term doesn't already exist with the new termTypeId
            String termTypeId = (String) context.get("termTypeId");
            if (UtilValidate.isNotEmpty(termTypeId)) {
                List existingTerms = EntityUtil.filterByAnd(invoiceTerms, UtilMisc.toMap("termTypeId", termTypeId));
                if (UtilValidate.isNotEmpty(existingTerms)) {
                    return UtilMessage.createAndLogServiceFailure("FinancialsServiceErrorInvoiceTermOfTypeExists", context, locale, MODULE);
                }
            }

            // There should only be one term allowed to govern payment due date, so don't update the term if the a term already exists which is of one
            //  of these types
            List dueDateTerms = EntityUtil.filterByCondition(invoiceTerms, EntityCondition.makeCondition("termTypeId", EntityOperator.IN, InvoiceHelper.invoiceDueDateAgreementTermTypeIds));
            if (InvoiceHelper.invoiceDueDateAgreementTermTypeIds.contains(termTypeId) && UtilValidate.isNotEmpty(dueDateTerms)) {
                return UtilMessage.createAndLogServiceFailure("FinancialsServiceErrorInvoiceTermOfPaymentTypeExists", context, locale, MODULE);
            }

            // Update the term
            invoiceTerm.setNonPKFields(context);
            invoiceTerm.store();

        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Sets invoice due date based on the InvoiceTerms.
     * If one of the terms is FIN_PAYMENT_TERM or FIN_PAYMENT_FIXDAY and no due date is set, the term will be used to calculate the due date of the invoice.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service parameters <code>Map</code>
     * @return the service response <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map setInvoiceDueDate(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        String invoiceId = (String) context.get("invoiceId");
        TimeZone timeZone = UtilCommon.getTimeZone(context);
        Locale locale = UtilCommon.getLocale(context);

        try {

            GenericValue invoice = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", invoiceId));

            if (UtilValidate.isNotEmpty(invoice.get("dueDate"))) {
                return ServiceUtil.returnSuccess();
            }

            // Get the InvoiceTerms in order
            List terms = invoice.getRelated("InvoiceTerm", UtilMisc.toList("invoiceTermId"));

            // Find the invoice due date term, using the first found (most recent)
            Long days = null;
            Long cutoffDayOfMonth = null;
            for (Iterator iter = terms.iterator(); iter.hasNext();) {
                GenericValue term = (GenericValue) iter.next();

                if ("FIN_PAYMENT_TERM".equals(term.get("termTypeId"))) {
                    if (UtilValidate.isEmpty(term.get("termDays"))) {
                        Debug.logWarning("Skipping term [" + term + "] because no termValue was set", MODULE);
                        continue;
                    }
                    days = term.getLong("termDays");
                    break;
                } else if ("FIN_PAYMENT_FIXDAY".equals(term.get("termTypeId"))) {
                    if (UtilValidate.isEmpty(term.get("termValue"))) {
                        Debug.logWarning("Skipping term [" + term + "] because no termValue was set", MODULE);
                        continue;
                    }
                    days = new Long((int) term.getDouble("termValue").doubleValue());
                    // use a default cutoff day which is same as the due day of the month: ie invoices before 18th are due the next month, invoices after 18th are due in 2 months.
                    if (UtilValidate.isEmpty(term.get("minQuantity"))) {
                        Debug.logWarning("No cutoff day was set in AgreementTerm.minOrderQuantity for FIN_PAYMENT_FIXDAY.  Using an assumed cut off day of today which is the same as due day of the month [" + term.getDouble("termValue") + "]", MODULE);
                        cutoffDayOfMonth = new Long((int) term.getDouble("termValue").doubleValue());
                    } else {
                        cutoffDayOfMonth = new Long((int) term.getDouble("minQuantity").doubleValue());
                    }
                    break;
                }
            }

            // set the due date
            Timestamp dueDate = null;
            if (UtilValidate.isNotEmpty(cutoffDayOfMonth)) {
                Calendar cal = UtilDate.toCalendar(invoice.getTimestamp("invoiceDate"), timeZone, locale);

                // If the invoice is created on or before the cutoff day of the month, then the invoice is due in the coming month.
                //  Otherwise it's due in the month after the coming month.
                int monthsToAdd = cal.get(Calendar.DAY_OF_MONTH) <= cutoffDayOfMonth.intValue() ? 1 : 2;
                cal.add(Calendar.MONTH, monthsToAdd);
                cal.set(Calendar.DAY_OF_MONTH, days.intValue());
                dueDate = UtilDateTime.getDayEnd(new Timestamp(cal.getTime().getTime()), timeZone, locale);
            } else if (UtilValidate.isNotEmpty(days)) {
                dueDate = UtilDateTime.getDayEnd(UtilDateTime.nowTimestamp(), days, timeZone, locale);
            }

            if (UtilValidate.isNotEmpty(dueDate)) {
                Debug.logInfo("Setting invoice dueDate to " + dueDate, MODULE);
                invoice.set("dueDate", dueDate);
                invoice.store();
            }

            return ServiceUtil.returnSuccess();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    /**
     * Creates an Invoice Item and an AgreementTermBilling.
     * This service must be used when creating invoices based on agreement terms.
     * This service should be called whenever an invoice item is being created via AgreementInvoiceFactory.
     * It will create an AgreementInvoiceBilling record, which stores a historical snapshot of what items were commissioned,
     *  how much they originally were, what the rate was, what the commission amout is, and who it was commissioned to.
     * Note that this is sort of a data warehouse feature, but it is meant to be used dynamically within the system.
     * TODO: to support product category commissions correctly, we need two new fields: origMinQuantity and origMaxQuantity
     * so we can track what the original term values for these were (in case they change and also for speed).
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service parameters <code>Map</code>
     * @return the service response <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map invoiceAgreementTerm(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        String invoiceId = (String) context.get("invoiceId");
        String agreementTermId = (String) context.get("agreementTermId");
        String paymentApplicationId = (String) context.get("paymentApplicationId");
        try {
            ModelService service = dctx.getModelService("createInvoiceItem");
            Map input = service.makeValid(context, "IN");
            Map results = dispatcher.runSync("createInvoiceItem", input);
            if (ServiceUtil.isError(results)) {
                return results;
            }

            // the out map is based on createInvoiceItem
            Map out = ServiceUtil.returnSuccess();
            out.putAll(service.makeValid(results, "OUT"));

            // get the agreement term data
            GenericValue agreementTerm = delegator.findByPrimaryKeyCache("AgreementTerm", UtilMisc.toMap("agreementTermId", agreementTermId));
            GenericValue agreement = agreementTerm.getRelatedOneCache("Agreement");

            // create the AgreementTermBilling directly for now TODO make this a service of its own
            input = UtilMisc.toMap("agreementTermId", agreementTermId, "invoiceId", invoiceId, "invoiceItemSeqId", context.get("invoiceItemSeqId"), "amount", context.get("amount"));
            input.put("agreementTermBillingId", delegator.getNextSeqId("AgreementTermBilling"));
            input.put("origPaymentApplicationId", paymentApplicationId);
            input.put("origInvoiceId", context.get("parentInvoiceId"));
            input.put("origInvoiceItemSeqId", context.get("parentInvoiceItemSeqId"));
            input.put("origCommissionRate", agreementTerm.get("termValue"));
            input.put("agentPartyId", agreement.get("partyIdTo"));
            input.put("billingDatetime", UtilDateTime.nowTimestamp());

            // record the original amount that is being commissioned for reference
            if (UtilValidate.isNotEmpty(paymentApplicationId)) {
                GenericValue application = delegator.findByPrimaryKeyCache("PaymentApplication", UtilMisc.toMap("paymentApplicationId", paymentApplicationId));
                input.put("origAmount", application.get("amountApplied"));
            } else {
                GenericValue invoiceItem = delegator.findByPrimaryKeyCache("InvoiceItem", UtilMisc.toMap("invoiceId", context.get("parentInvoiceId"), "invoiceItemSeqId", context.get("parentInvoiceItemSeqId")));
                BigDecimal quantity = (invoiceItem.get("quantity") == null ? BigDecimal.ZERO : invoiceItem.getBigDecimal("quantity"));
                BigDecimal amount = invoiceItem.getBigDecimal("amount").multiply(quantity).setScale(decimals, rounding);
                input.put("origAmount", amount);
            }
            GenericValue billing = delegator.makeValue("AgreementTermBilling", input);
            billing.create();

            return out;
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    /**
     * Saves an invoice PDF to file in a location designated in financials.properties.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service parameters <code>Map</code>
     * @return the service response <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map saveInvoicePDFToFile(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String screenLocation = (String) context.get("screenLocation");
        String invoiceId = (String) context.get("invoiceId");

        // Check the screen location for the invoice PDF
        if (UtilValidate.isEmpty(screenLocation)) {
            screenLocation = UtilProperties.getPropertyValue(financialsResource, "financials.invoice.pdf.screenLocation");
        }
        if (UtilValidate.isEmpty(screenLocation)) {
            return UtilMessage.createAndLogServiceError("FinancialsErrorPropertyNotConfigured", UtilMisc.toMap("propertyName", "financials.invoice.pdf.screenLocation", "resource", financialsResource), locale, MODULE);
        }

        // Check the save-location path property
        String savePath = UtilProperties.getPropertyValue(financialsResource, "financials.invoice.pdf.saveLocation");
        if (UtilValidate.isEmpty(savePath)) {
            return UtilMessage.createAndLogServiceError("FinancialsErrorPropertyNotConfigured", UtilMisc.toMap("propertyName", "financials.invoice.pdf.saveLocation", "resource", financialsResource), locale, MODULE);
        }
        if (!savePath.endsWith(System.getProperty("file.separator"))) {
            savePath += System.getProperty("file.separator");
        }

        // Get the filename for the PDF
        String fileName = UtilProperties.getMessage(financialsResource, "financials.invoice.pdf.fileNamePattern", context, locale);
        if (UtilValidate.isEmpty(fileName)) {
            return UtilMessage.createAndLogServiceError("FinancialsErrorPropertyNotConfigured", UtilMisc.toMap("propertyName", "financials.invoice.pdf.fileNamePattern", "resource", financialsResource), locale, MODULE);
        }

        // Call the service to save the location to a PDF file
        Map savePDFContext = new HashMap();
        savePDFContext.put("screenLocation", screenLocation);
        savePDFContext.put("savePath", savePath);
        savePDFContext.put("fileName", fileName);
        savePDFContext.put("screenContext", context);
        savePDFContext.put("screenParameters", UtilMisc.toMap("invoiceId", invoiceId));
        savePDFContext.put("userLogin", userLogin);
        Map savePDFResult = null;
        String errorMessage = UtilMessage.expandLabel("FinancialsErrorSavingInvoicePDF", UtilMisc.toMap("invoiceId", invoiceId, "path", savePath + fileName), locale);
        try {
            savePDFResult = dispatcher.runSync("saveFOScreenToPDFFile", savePDFContext);
        } catch (GenericServiceException e) {
            Debug.logError(errorMessage, MODULE);
            return ServiceUtil.returnError(errorMessage + ": " + e.getMessage());
        }
        if (ServiceUtil.isError(savePDFResult)) {
            Debug.logError(errorMessage, MODULE);
            return ServiceUtil.returnError(errorMessage);
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Service to bill a third party instead of the store's pay to party while keeping a record of what the third party owes to the pay to party.
     * This works by taking a regular sales invoice between a customer and the pay to party as an input.
     * It will determine what invoice items should be billed to a third party based on the ProductStore.billToThirdPartyId for the orders covered by the invoice.
     * Each invoice item is copied to a special sales invoice that is from the pay to party to the third party.
     * Then the sales invoice with the customer is paid in full and closed.
     * Only orders whose sole payment method is EXT_BILL_3RDPTY are covered.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service parameters <code>Map</code>
     * @return the service response <code>Map</code>
     */
    @SuppressWarnings("unchecked")

    public static Map billToThirdParty(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        Locale locale = UtilCommon.getLocale(context);
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        // check that user can create AR payments and invoices
        if (!(security.hasEntityPermission("FINANCIALS", "_AR_INCRTE", userLogin) && security.hasEntityPermission("FINANCIALS", "_AR_PCRTE", userLogin))) {
            return UtilMessage.createAndLogServiceError("OpentapsError_PermissionDenied", locale, MODULE);
        }

        String invoiceId = (String) context.get("invoiceId");
        try {
            // The sales invoice is between the customer and Company
            GenericValue invoice = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", invoiceId));
            if (!"SALES_INVOICE".equals(invoice.get("invoiceTypeId"))) {
                Debug.logInfo("Invoice [" + invoiceId + "] is not a sales invoice and will not be billed to a third party", MODULE);
                return ServiceUtil.returnSuccess();
            }

            // We'll assume that the currency for all Company invoices are the same (this impacts third party invoice creation)
            String currencyUomId = invoice.getString("currencyUomId");

            // group the invoice items by third party (note that we treat the case where an invoice can span multiple orders)
            int totalInvoiceLines = 0;
            List billings = invoice.getRelated("OrderItemBilling", UtilMisc.toList("orderId", "invoiceItemSeqId"));
            Map orderToBillingParty = FastMap.newInstance();
            Map itemsToCopy = FastMap.newInstance();
            for (Iterator iter = billings.iterator(); iter.hasNext();) {
                GenericValue billing = (GenericValue) iter.next();
                String orderId = billing.getString("orderId");
                String billToThirdPartyId = (String) orderToBillingParty.get(orderId);

                if (billToThirdPartyId == null) {
                    GenericValue order = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));

                    // make sure the order is paid with only one EXT_BILL_3RDPTY payment method
                    List prefs = order.getRelated("OrderPaymentPreference");
                    if (prefs.size() != 1) {
                        continue;
                    }
                    GenericValue pref = EntityUtil.getFirst(prefs);
                    if (!"EXT_BILL_3RDPTY".equals(pref.get("paymentMethodTypeId"))) {
                        continue;
                    }

                    // look up the bill to third party
                    GenericValue productStore = order.getRelatedOneCache("ProductStore");
                    billToThirdPartyId = productStore.getString("billToThirdPartyId");
                    if (UtilValidate.isEmpty(billToThirdPartyId)) {
                        continue;
                    }

                    // we are processing this order
                    orderToBillingParty.put(orderId, billToThirdPartyId);
                }

                List items = (List) itemsToCopy.get(billToThirdPartyId);
                if (items == null) {
                    items = FastList.newInstance();
                }
                items.add(billing.getRelatedOne("InvoiceItem"));
                itemsToCopy.put(billToThirdPartyId, items);

                totalInvoiceLines += 1;
            }

            // if no items, then invoice is not subject to the third party billing, so return here
            if (totalInvoiceLines == 0) {
                Debug.logInfo("Invoice [" + invoiceId + "] has no items to bill to third party and will be skipped for third party billing", MODULE);
                return ServiceUtil.returnSuccess();
            }

            // Next add any adjustment invoice items (note that we treat the case where an invoice can span multiple orders)
            billings = invoice.getRelated("OrderAdjustmentBilling");
            for (Iterator iter = billings.iterator(); iter.hasNext();) {
                GenericValue billing = (GenericValue) iter.next();
                GenericValue adjustment = billing.getRelatedOne("OrderAdjustment");
                String billToThirdPartyId = (String) orderToBillingParty.get(adjustment.get("orderId"));
                if (UtilValidate.isEmpty(billToThirdPartyId)) {
                    Debug.logInfo("Order adjustment [" + adjustment.getString("orderAdjustmentId") + "] is not from an order billed to a third party and will be skipped", MODULE);
                    continue;
                }

                // some adjustments don't have usable descriptions, so flesh them out here
                GenericValue item = billing.getRelatedOne("InvoiceItem");
                if (UtilValidate.isEmpty(item.getString("description"))) {
                    StringBuffer description = new StringBuffer();
                    GenericValue itemType = item.getRelatedOneCache("InvoiceItemType");
                    description.append(itemType.get("description"));

                    // if not a whole order adjustment, describe the item using adjustment.comments and adjustment.description
                    if (!"_NA_".equals(adjustment.get("orderItemSeqId"))) {
                        String comments = adjustment.getString("comments");
                        String desc = adjustment.getString("description");
                        if (UtilValidate.isNotEmpty(comments)) {
                            description.append(" - ");
                            description.append(comments);
                        }
                        if (UtilValidate.isNotEmpty(desc)) {
                            description.append(" - ");
                            description.append(desc);
                        }
                    }
                    item.set("description", description.toString());
                }

                List items = (List) itemsToCopy.get(billToThirdPartyId);
                if (items == null) {
                    items = FastList.newInstance();
                }
                items.add(item);
                itemsToCopy.put(billToThirdPartyId, items);
            }

            // Keep track of amount billed
            BigDecimal amountToBill = ZERO;
            BigDecimal taxToBill = ZERO;

            // process each third party separately
            for (Iterator thidPartyIter = itemsToCopy.keySet().iterator(); thidPartyIter.hasNext();) {
                String billToThirdPartyId = (String) thidPartyIter.next();
                List invoiceItems = (List) itemsToCopy.get(billToThirdPartyId);
                if (invoiceItems == null || invoiceItems.size() == 0) {
                    Debug.logInfo("Third party [" + billToThirdPartyId + "] has no invoice items to bill", MODULE);
                    continue;
                }

                // Find the first active invoice (in process) between Company and the third party.
                Map conditions = UtilMisc.toMap("invoiceTypeId", "SALES_INVOICE", "partyIdFrom", invoice.get("partyIdFrom"), "partyId", billToThirdPartyId, "statusId", "INVOICE_IN_PROCESS");
                List invoices = delegator.findByAnd("Invoice", conditions, UtilMisc.toList("invoiceDate ASC"));
                GenericValue thirdPartyInvoice = EntityUtil.getFirst(invoices);

                // If it doesn't exist yet, create one
                String thirdPartyInvoiceId = null;
                if (thirdPartyInvoice == null) {
                    Map input = UtilMisc.toMap("partyIdFrom", invoice.get("partyIdFrom"), "thirdPartyId", billToThirdPartyId, "currencyUomId", currencyUomId);
                    Map results = dispatcher.runSync("createThirdPartySalesInvoice", input);
                    if (ServiceUtil.isError(results)) {
                        return results;
                    }
                    thirdPartyInvoiceId = (String) results.get("invoiceId");
                    thirdPartyInvoice = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", invoiceId));
                } else {
                    thirdPartyInvoiceId = thirdPartyInvoice.getString("invoiceId");
                }

                // for each customer invoice line, create a copy for the third party invoice
                Map input = UtilMisc.toMap("invoiceId", thirdPartyInvoiceId, "userLogin", userLogin, "uomId", currencyUomId);
                GenericValue person = delegator.findByPrimaryKey("Person", UtilMisc.toMap("partyId", invoice.get("partyId")));

                // prevent NPE in case the Person is not found
                if (person == null) {
                    return ServiceUtil.returnError("Person entity not found with id [" + invoice.getString("partyId") + "] related to invoice [" + invoice.getString("invoiceId") + "]");
                }

                StringBuffer descriptionPrefix = new StringBuffer(person.getString("firstName")).append(" ").append(person.getString("lastName")).append(" - ");
                for (Iterator iter = invoiceItems.iterator(); iter.hasNext();) {
                    GenericValue item = (GenericValue) iter.next();

                    StringBuffer description = new StringBuffer(descriptionPrefix);
                    description.append(invoice.get("invoiceDate"));
                    description.append(" - ");
                    description.append(item.get("description"));

                    input.put("invoiceItemTypeId", "ITM_BILL_FROM_CUST");
                    input.put("inventoryItemId", item.get("inventoryItemId"));
                    input.put("productId", item.get("productId"));
                    input.put("productFeatureId", item.get("productFeatureId"));
                    input.put("parentInvoiceId", item.get("invoiceId"));
                    input.put("parentInvoiceItemSeqId", item.get("invoiceItemSeqId"));
                    input.put("taxableFlag", item.get("taxableFlag"));
                    input.put("quantity", item.get("quantity"));
                    input.put("amount", item.get("amount"));
                    input.put("taxAuthPartyId", item.get("taxAuthPartyId"));
                    input.put("taxAuthGeoId", item.get("taxAuthGeoId"));
                    input.put("taxAuthorityRateSeqId", item.get("taxAuthorityRateSeqId"));
                    input.put("description", description.toString());

                    Map results = dispatcher.runSync("createInvoiceItem", input);
                    if (ServiceUtil.isError(results)) {
                        return results;
                    }

                    if ("INV_SALES_TAX".equals(item.get("invoiceItemTypeId")) || "ITM_SALES_TAX".equals(item.get("invoiceItemTypeId"))) {
                        taxToBill = taxToBill.add(item.getBigDecimal("amount")).setScale(taxDecimals, taxRounding);
                    } else {
                        amountToBill = amountToBill.add(item.getBigDecimal("amount")).setScale(decimals, rounding);
                    }
                }
                amountToBill = amountToBill.add(taxToBill).setScale(decimals, rounding);
            }

            // pay off the customer sales invoice with a special third party payment method type
            Map paymentParams = UtilMisc.toMap("paymentTypeId", "CUSTOMER_PAYMENT", "paymentMethodTypeId", "EXT_BILL_3RDPTY", "partyIdFrom", invoice.get("partyId"), "partyIdTo", invoice.get("partyIdFrom"), "effectiveDate", UtilDateTime.nowTimestamp());
            paymentParams.put("statusId", "PMNT_RECEIVED");
            paymentParams.put("amount", amountToBill);
            paymentParams.put("currencyUomId", invoice.getString("currencyUomId"));
            paymentParams.put("userLogin", userLogin);
            Map results = dispatcher.runSync("createPayment", paymentParams);
            if (ServiceUtil.isError(results)) {
                return results;
            }
            String paymentId = (String) results.get("paymentId");

            // apply it to our invoice
            paymentParams = UtilMisc.toMap("userLogin", userLogin, "invoiceId", invoiceId, "paymentId", paymentId, "amountApplied", amountToBill);
            results = dispatcher.runSync("createPaymentApplication", paymentParams);
            if (ServiceUtil.isError(results)) {
                return results;
            }

        } catch (GeneralException e) {
            Debug.logError(e, e.getMessage(), MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Creates a new open invoice from the input party to the third party.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service parameters <code>Map</code>
     * @return the service response <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map createThirdPartySalesInvoice(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();

        String partyIdFrom = (String) context.get("partyIdFrom");
        String thirdPartyId  = (String) context.get("thirdPartyId");
        String currencyUomId = (String) context.get("currencyUomId");
        try {
            GenericValue system = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "system"));

            Map input = FastMap.newInstance();
            input.put("invoiceTypeId", "SALES_INVOICE");
            input.put("partyIdFrom", partyIdFrom);
            input.put("partyId", thirdPartyId);
            input.put("statusId", "INVOICE_IN_PROCESS");
            input.put("currencyUomId", currencyUomId);
            input.put("userLogin", system);
            Map createResults = dispatcher.runSync("createInvoice", input);
            if (ServiceUtil.isError(createResults)) {
                return createResults;
            }

            Map result = ServiceUtil.returnSuccess();
            result.put("invoiceId", createResults.get("invoiceId"));
            return result;
        } catch (GeneralException e) {
            Debug.logError(e, e.getMessage(), MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    /**
     * Creates and approves an invoice to the carrier with itemized charges for COD payments, and creates a payment and application for each sales invoice of type EXT_BILL_3RDPTY_COD.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service parameters <code>Map</code>
     * @return the service response <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map processCODReceipt(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        TimeZone timeZone = UtilCommon.getTimeZone(context);
        Security security = dctx.getSecurity();

        if (!(security.hasEntityPermission("FINANCIALS", "_AR_INCRTE", userLogin) && security.hasEntityPermission("FINANCIALS", "_AR_PCRTE", userLogin))) {
            return UtilMessage.createAndLogServiceError("OpentapsError_PermissionDenied", locale, MODULE);
        }

        Map invoiceIds = (Map) context.get("invoiceIds");
        Map amounts = (Map) context.get("amounts");
        Map paymentRefNums = (Map) context.get("paymentRefNums");
        Map trackingCodes = (Map) context.get("trackingCodes");
        Map _rowSubmits = (Map) context.get("_rowSubmits");
        String carrierPartyId = (String) context.get("carrierPartyId");
        String currencyUomId = (String) context.get("currencyUomId");
        String referenceNumber = (String) context.get("referenceNumber");
        String organizationPartyId = (String) context.get("organizationPartyId");
        String invoiceDateString = (String) context.get("invoiceDate");

        Map result = ServiceUtil.returnSuccess();
        if (UtilValidate.isEmpty(_rowSubmits)) {
            return ServiceUtil.returnFailure();
        }

        try {

            // Construct an invoice from organization party to carrier party
            Map createInvoiceContext = UtilMisc.toMap("invoiceTypeId", "SALES_INVOICE", "statusId", "INVOICE_IN_PROCESS", "partyIdFrom", organizationPartyId, "partyId", carrierPartyId);
            createInvoiceContext.put("currencyUomId", currencyUomId);
            createInvoiceContext.put("referenceNumber", referenceNumber);
            createInvoiceContext.put("userLogin", userLogin);
            createInvoiceContext.put("invoiceDate", UtilDate.toTimestamp(invoiceDateString, timeZone, locale));
            Map createInvoiceResult = dispatcher.runSync("createInvoice", createInvoiceContext);
            if (ServiceUtil.isError(createInvoiceResult)) {
                return createInvoiceResult;
            }
            String codInvoiceId = (String) createInvoiceResult.get("invoiceId");
            result.put("invoiceId", codInvoiceId);

            BigDecimal adjustmentAmount = null;
            if (UtilValidate.isNotEmpty(context.get("adjustmentAmount"))) {
                try {
                    adjustmentAmount = new BigDecimal((String) context.get("adjustmentAmount"));
                } catch (Exception e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(e.getMessage());
                }
            }

            // Create an invoice item for the adjustment amount, if necessary
            if (adjustmentAmount != null && adjustmentAmount.doubleValue() != 0) {
                Map createInvoiceItemContext = UtilMisc.toMap("invoiceId", codInvoiceId, "invoiceItemTypeId", "ITM_BILL_FRM_COD_ADJ", "userLogin", userLogin);
                createInvoiceItemContext.put("amount", adjustmentAmount.negate());
                Map createInvoiceItemResult = dispatcher.runSync("createInvoiceItem", createInvoiceItemContext);
                if (ServiceUtil.isError(createInvoiceItemResult)) {
                    return createInvoiceItemResult;
                }
            }

            Iterator rit = _rowSubmits.keySet().iterator();
            while (rit.hasNext()) {

                String rowNumber = (String) rit.next();

                // Ignore unchecked rows
                if (UtilValidate.isEmpty(_rowSubmits.get(rowNumber))) {
                    continue;
                }
                if (UtilValidate.isEmpty((String) amounts.get(rowNumber))) {
                    continue;
                }

                String invoiceId = (String) invoiceIds.get(rowNumber);
                String paymentRefNum = (String) paymentRefNums.get(rowNumber);
                String trackingCode = (String) trackingCodes.get(rowNumber);
                BigDecimal amount = null;
                try {
                    amount = UtilCommon.parseLocalizedNumber(locale, (String) amounts.get(rowNumber));
                } catch (ParseException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(e.getMessage());
                }

                if (amount.signum() <= 0) {
                    continue;
                }

                GenericValue invoice = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", invoiceId));
                if (UtilValidate.isEmpty(invoice)) {
                    return UtilMessage.createAndLogServiceError("FinancialsError_InvoiceNotFound", UtilMisc.toMap("invoiceId", invoiceId), locale, MODULE);
                }

                // Create an invoice item for the COD invoice to the carrier
                Map createInvoiceItemContext = UtilMisc.toMap("invoiceId", codInvoiceId, "invoiceItemTypeId", "ITM_BILL_FRM_COD", "amount", amount, "userLogin", userLogin);
                createInvoiceItemContext.put("description", trackingCode);
                Map createInvoiceItemResult = dispatcher.runSync("createInvoiceItem", createInvoiceItemContext);
                if (ServiceUtil.isError(createInvoiceItemResult)) {
                    return createInvoiceItemResult;
                }

                // Construct a payment of type EXT_BILL_3RDPTY_COD and apply it to the sales invoice
                Map paymentContext = UtilMisc.toMap("paymentMethodTypeId", "EXT_BILL_3RDPTY_COD", "statusId", "PMNT_RECEIVED", "currencyUomId", currencyUomId, "userLogin", userLogin);
                paymentContext.put("paymentTypeId", "CUSTOMER_PAYMENT");
                paymentContext.put("paymentRefNum", paymentRefNum);
                paymentContext.put("amount", amount);
                paymentContext.put("partyIdTo", organizationPartyId);
                paymentContext.put("partyIdFrom", invoice.get("partyId"));
                Map createPaymentResult = dispatcher.runSync("createPayment", paymentContext);
                if (ServiceUtil.isError(createPaymentResult)) {
                    return createPaymentResult;
                }
                String paymentId = (String) createPaymentResult.get("paymentId");

                Map paymentApplicationContext = UtilMisc.toMap("paymentId", paymentId, "invoiceId", invoiceId, "amountApplied", amount, "userLogin", userLogin);
                Map createPaymentApplicationResult = dispatcher.runSync("createPaymentApplication", paymentApplicationContext);
                if (ServiceUtil.isError(createPaymentApplicationResult)) {
                    return createPaymentApplicationResult;
                }
            }

            // TODO: Create a payment from the carrier to the organization to cover the invoice just created?

        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        return result;
    }

    /**
     * Checks if an invoice can be set to ready.
     * Checks a number of things:<ul>
     * <lo>If the payment applications add up to the invoice (same as checkInvoicePaymentApplications)</lo>
     * <lo>If the invoice billing party has a credit limit agreement, then ensure it is not exceeded.</lo>
     * </ul>
     * If the inovoice cannot be set ready, this service returns a service error.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service parameters <code>Map</code>
     * @return the service response <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map isInvoiceReady(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String invoiceId = (String) context.get("invoiceId");
        try {
            Map input =  UtilMisc.toMap("invoiceId", invoiceId);
            GenericValue invoice = delegator.findByPrimaryKey("Invoice", input);

            // check if the payment applications add up (also checks if invoice exists)
            input.put("userLogin", userLogin);
            Map results = dispatcher.runSync("checkInvoicePaymentApplications", input);
            if (ServiceUtil.isError(results)) {
                return results;
            }

            // get the invoice total in the currency of the invoice
            BigDecimal invoiceTotal = InvoiceWorker.getInvoiceTotal(invoice);

            // We need to subtract from invoice total what will be captured.  The only way to do this at this
            // stage is to sum up the authorized payments towards this invoice.
            BigDecimal authorizedAmount = InvoiceHelper.getInvoiceAuthorizedAmount(invoice);
            invoiceTotal = invoiceTotal.subtract(authorizedAmount);

            // check that the invoice total doesn't exceed the invoice party's available credit, if they have a credit limit agreement
            if ("SALES_INVOICE".equals(invoice.get("invoiceTypeId"))) {

                // get the currency of the organization the sales invoice is from
                String orgCurrencyUomId = UtilCommon.getOrgBaseCurrency(invoice.getString("partyIdFrom"), delegator);
                if (orgCurrencyUomId == null) {
                    return ServiceUtil.returnError("Party [" + invoice.getString("partyIdFrom") + "] must have a currency defined for accounting.  Please see PartyAcctgPreference.baseCurrencyUomId.");
                }

                // if different, we neet to convert the invoice total
                BigDecimal convertedInvoiceTotal;
                String invoiceCurrencyUomId = invoice.getString("currencyUomId");
                if (UtilValidate.isEmpty(invoiceCurrencyUomId)) {
                    return UtilMessage.createServiceError("OpentapsError_InvoiceCurrencyNotSet", locale, UtilMisc.toMap("invoiceId", invoiceId));
                }
                if (!orgCurrencyUomId.equals(invoiceCurrencyUomId)) {
                    // TODO need a unit test for conversion
                    BigDecimal conversionFactor = UtilFinancial.determineUomConversionFactor(delegator, dispatcher, invoice.getString("partyIdFrom"), invoiceCurrencyUomId);
                    convertedInvoiceTotal = invoiceTotal.multiply(conversionFactor);
                } else {
                    convertedInvoiceTotal = invoiceTotal;
                }
                convertedInvoiceTotal = convertedInvoiceTotal.setScale(decimals, rounding);

                // now we can compare the credit limit of the customer (in organization's currency) against this converted total
                if (AccountsHelper.amountExceedsAvailableCreditAgreement(delegator, dispatcher, invoice.getString("partyId"), invoice.getString("partyIdFrom"), convertedInvoiceTotal, orgCurrencyUomId)) {
                    return UtilMessage.createServiceError("FinancialsError_CreditLimitExceeded", locale);
                }
            }

            // if all tests pass, we can mark invoice as ready
            return ServiceUtil.returnSuccess();
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
    }

    /**
     * Sets the <code>Invoice</code> processing status.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service parameters <code>Map</code>
     * @return the service response <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map setInvoiceProcessingStatus(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();

        String invoiceId = (String) context.get("invoiceId");
        String statusId = (String) context.get("statusId");

        Map result = ServiceUtil.returnSuccess();
        result.put("invoiceId", invoiceId);

        try {
            Map input =  UtilMisc.toMap("invoiceId", invoiceId);
            GenericValue invoice = delegator.findByPrimaryKey("Invoice", input);

            invoice.set("processingStatusId", statusId);
            invoice.store();

            // if all tests pass, we can mark invoice as ready
            return result;
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
    }

    /**
     * Note that in this service we are breaking the pattern of using StatusValidChange.  The rationale for this is
     * to prevent the legacy setInvoiceStatus from allowing an invoice to be voided.  This means that in order to void
     * an invoice, this service must be used.  Hence, we ensure that the transactions will always be reversed when an
     * invoice is voided.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service parameters <code>Map</code>
     * @return the service response <code>Map</code>
     */
    public static Map<String, Object> voidInvoice(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String invoiceId = (String) context.get("invoiceId");

        // TODO: check some kind of security?
        try {
            GenericValue invoice = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", invoiceId));
            if (invoice == null) {
                return UtilMessage.createServiceError("FinancialsInvoiceNotFoundWithId", locale, UtilMisc.toMap("invoiceId", invoiceId));
            }
            if (!"INVOICE_READY".equals(invoice.get("statusId"))) {
                return UtilMessage.createServiceError("FinancialsError_CannotVoidInvoiceInvalidStatus", locale, UtilMisc.toMap("invoiceId", invoiceId, "statusId", invoice.get("statusId")));
            }

            // check to see if there are any payment applications
            List<GenericValue> applications = invoice.getRelated("PaymentApplication");
            if (applications.size() > 0) {
                return UtilMessage.createServiceError("FinancialsError_CannotVoidInvoiceExistingPayments", locale, UtilMisc.toMap("invoiceId", invoiceId));
            }
            Debug.logInfo("Voiding invoice [" + invoiceId + "]", MODULE);

            // set invoice status by hand instead of going through legacy service (TODO: watch out for EECAs)
            invoice.set("statusId", "INVOICE_VOIDED");
            invoice.store();

            // reverse accounting transactions when invoice was set to ready
            List<GenericValue> transactions = invoice.getRelated("AcctgTrans");
            for (GenericValue transaction : transactions) {
                String acctgTransId = transaction.getString("acctgTransId");
                Debug.logInfo("Reversing transaction [" + acctgTransId + "] triggered by voiding of invoice [" + invoiceId + "]", MODULE);
                Map<String, Object> results = dispatcher.runSync("reverseAcctgTrans", UtilMisc.toMap("acctgTransId", acctgTransId, "postImmediately", "Y", "userLogin", userLogin));
                if (ServiceUtil.isError(results)) {
                    return UtilMessage.createAndLogServiceError(results, MODULE);
                }
            }

            // sets quantity of related order item billing records to zero.
            dispatcher.runSync("opentaps.cancelOrderItemBilling", UtilMisc.toMap("invoiceId", invoiceId, "userLogin", userLogin), -1, false);

        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Associates shipment addresses with invoices.
     * This is meant to be triggered after createInvoicesFromShipments.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service parameters <code>Map</code>
     * @return the service response <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map createShippingInvoiceContactMech(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        List<String> shipmentIds = (List<String>) context.get("shipmentIds");
        List<String> invoiceIds = (List<String>) context.get("invoicesCreated");
        if (invoiceIds.size() == 0) {
            Debug.logInfo("No invoices created for shipments " + shipmentIds, MODULE);
            return ServiceUtil.returnSuccess();
        }

        // create InvoiceContactMech for each invoice that each shipment appears in
        try {
            for (String shipmentId : shipmentIds) {
                GenericValue shipment = delegator.findByPrimaryKey("Shipment", UtilMisc.toMap("shipmentId", shipmentId));
                String contactMechId = shipment.getString("destinationContactMechId");
                if (contactMechId == null) {
                    continue;
                }

                List conditions = UtilMisc.toList(
                        EntityCondition.makeCondition("shipmentId", EntityOperator.EQUALS, shipmentId),
                        EntityCondition.makeCondition("invoiceId", EntityOperator.IN, invoiceIds)
                );
                List<GenericValue> billings = delegator.findByAnd("ShipmentItemBilling", conditions);
                List<String> invoices = EntityUtil.getFieldListFromEntityList(billings, "invoiceId", true);
                for (String invoiceId : invoices) {
                    Map<String, Object> input = UtilMisc.<String, Object>toMap("invoiceId", invoiceId);
                    input.put("contactMechId", contactMechId);
                    input.put("userLogin", userLogin);
                    input.put("contactMechPurposeTypeId", "SHIPPING_LOCATION");
                    Map results = dispatcher.runSync("createInvoiceContactMech", input);
                    if (ServiceUtil.isError(results)) {
                        return results;
                    }
                }
            }
            return ServiceUtil.returnSuccess();
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
    }

    /*
    // TODO: perhaps this belongs in Purchasing application
    public static Map updateSupplierProduct(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        Security security = dctx.getSecurity();

        String invoiceId = (String) context.get("invoiceId");
        try {
            // TODO: is this right security for updating SupplierProduct?  Maybe a purchasing permission?
            if (!security.hasEntityPermission("FINANCIALS", "_AP_INUPDT", userLogin)) {
                return ServiceUtil.returnError(UtilProperties.getMessage(financialsLabels, "FinancialsServiceErrorNoPermission", locale));
            }
            GenericValue invoice = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", invoiceId));

            // Verify PO invoice, and if not don't return error because SECAS might not have a way to test the invoiceTypeId.
            if (!"PURCHASE_INVOICE".equals(invoice.get("invoiceTypeId"))) {
                return ServiceUtil.returnSuccess();
            }

            // create the supplier role if not yet specified
            Map results = dispatcher.runSync("ensurePartyRole", UtilMisc.toMap("partyId", invoice.get("partyIdFrom"), "roleTypeId", "SUPPLIER"));
            if (ServiceUtil.isError(results)) return results;

            // build a list of productIds that are in the invoice
            Set productIds = FastSet.newInstance();
            for (Iterator iter = invoiceItems.iterator(); iter.hasNext(); ) {
                GenericValue invoiceItem = (GenericValue) iter.next();
                if ((invoiceItem.get("productId") != null) && UtilFinancial.PRODUCT_INVOICE_ITEM_TYPES.contains(invoiceItem.get("invoiceItemTypeId"))) {
                    productIds.add(invoiceItem.get("productId"));
                }
            }

            // get existing supplier products
            List conditions = UtilMisc.toList(
                    EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, invoice.get("partyIdFrom")),
                    EntityCondition.makeCondition("productId", EntityOperator.IN, productIds),
                    EntityUtil.getFilterByDateExpr("availableFromDate", "availableThruDate")
                    );
            List supplierProducts = delegator.findByAnd("SupplierProduct", conditions);

            // er.. complexity

        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
    }
     */

    public static Map<String, ? extends Object> createCommissionInvoicesOnConfirmedPayment(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String paymentId = (String) context.get("paymentId");

        try {

            DomainsLoader dl = new DomainsLoader(new Infrastructure(dispatcher), new User(userLogin));
            DomainsDirectory domains = dl.loadDomainsDirectory();

            List<GenericValue> paymentsAndApplications = delegator.findByAnd("PaymentAndApplication", UtilMisc.toMap("paymentId", paymentId));
            if (UtilValidate.isEmpty(paymentsAndApplications)) {
                Debug.logInfo("No PaymentAndApplication records found for payment ID [" + paymentId + "], so cannot create commission invoices for this payment", MODULE);
            }
            for (GenericValue paymentApplication : paymentsAndApplications) {
                String invoiceId = paymentApplication.getString("invoiceId");
                // a PaymentApplication may be to a GL Account instead of an Invoice
                if (invoiceId == null) {
                    Debug.logInfo("PaymentApplication with ID [" + paymentApplication.get("paymentApplicationId") + "] is not applied to any invoice, so now commission will be created for this application", MODULE);
                    continue;
                }

                Invoice invoice = domains.getBillingDomain().getInvoiceRepository().getInvoiceById(invoiceId);
                String organizationPartyId = invoice.getOrganizationPartyId();

                // At the moment, we only support sales invoices.  This service should not cause an error if an unsupported invoice
                // is supplied because SECAs that implement this service may not be able to check the type.
                if (!"SALES_INVOICE".equals(invoice.getInvoiceTypeId())) {
                    Debug.logInfo("Invoice [" + invoice.getInvoiceTypeId() + "] is not a sales invoice and will not be commissioned.", MODULE);
                    break;
                }

                Set<String> agentIds = FastSet.newInstance();

                // get any agents with InvoiceRole
                List<InvoiceRole> agents = invoice.getRelated(InvoiceRole.class);
                for (InvoiceRole agent : agents) {
                    if ("COMMISSION_AGENT".equals(agent.getRoleTypeId())) {
                        agentIds.add(agent.getString("partyId"));
                    }
                }

                // get any agents that can earn commission for this party
                agentIds.addAll(UtilAgreement.getCommissionAgentIdsForCustomer(invoice.getPartyIdFrom(), invoice.getPartyId(), delegator));

                // create a commission invoice for each commission agent associated with the invoice
                for (String agentPartyId : agentIds) {
                    List<EntityCondition> conditions = Arrays.asList(
                            EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, organizationPartyId),
                            EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.EQUALS, "INTERNAL_ORGANIZATIO"),
                            EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, agentPartyId),
                            EntityCondition.makeCondition("roleTypeIdTo", EntityOperator.EQUALS, "COMMISSION_AGENT"),
                            EntityCondition.makeCondition("agreementTypeId", EntityOperator.EQUALS, "COMMISSION_AGREEMENT"),
                            EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "AGR_ACTIVE"),
                            EntityUtil.getFilterByDateExpr()
                    );
                    List<GenericValue> agreements = delegator.findByAnd("Agreement", conditions, UtilMisc.toList("fromDate ASC"));
                    if (UtilValidate.isNotEmpty(agreements)) {
                        GenericValue invoiceValue = Repository.genericValueFromEntity(delegator, invoice);
                        context.put("paymentApplicationId", paymentApplication.get("paymentApplicationId"));
                        context.put("paymentInvoiceTotal", invoice.getInvoiceTotal());
                        for (GenericValue agreement : agreements) {
                            if (UtilAgreement.isCommissionEarnedOnPayment(agreement) && UtilAgreement.isInvoiceCoveredByAgreement(invoiceValue, agreement)) {
                                AgreementInvoiceFactory.createInvoiceFromAgreement(dctx, context, agreement, Arrays.asList(invoiceValue),
                                        "COMMISSION_INVOICE", "COMMISSION_AGENT", invoice.getCurrencyUomId(), true, false);
                            }
                        }
                    } else {
                        Debug.logWarning("No applicable commission agreement found for agent with party ID [" + agentPartyId + "] and organization ["  + organizationPartyId + "]", MODULE);
                    }
                }
            }

        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        } catch (InfrastructureException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        } catch (EntityNotFoundException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        } catch (RepositoryException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Sets the invoice to READY using the setInvoiceStatus, and then calls checkInvoicePaymentApplications to see if should be set as PAID.
     * This is meant for the financials webapp to set invoices to READY and will cause them to be automatically set to PAID if the invoice already has payments applied to it, such as from the CRMSFA's order view screen's receive payment page.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service parameters <code>Map</code>
     * @return the service response <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map setInvoiceReadyAndCheckIfPaid(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String invoiceId = (String) context.get("invoiceId");

        try {
            // don't start new transactions inside this service
            Map tmpResult = dispatcher.runSync("setInvoiceStatus", UtilMisc.toMap("invoiceId", invoiceId, "statusId", "INVOICE_READY", "userLogin", userLogin), 1, false);
            if (ServiceUtil.isError(tmpResult) || ServiceUtil.isFailure(tmpResult)) {
                return tmpResult;
            }
            tmpResult = dispatcher.runSync("checkInvoicePaymentApplications", UtilMisc.toMap("invoiceId", invoiceId, "userLogin", userLogin), 1, false);
            if (ServiceUtil.isError(tmpResult) || ServiceUtil.isFailure(tmpResult)) {
                return tmpResult;
            }
            return ServiceUtil.returnSuccess();
        } catch (GeneralException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }
    }

    /**
     * Creates invoices from the orderData, which is a Collection of Maps with the fields orderId, orderItemSeqId, workEffortId, quantity.
     * All field values, including quantity, should be string.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context the service parameters <code>Map</code>
     * @return the service response <code>Map</code>
     */
    @SuppressWarnings("unchecked")
    public static Map invoiceSuppliesOrWorkEffortOrderItems(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        Delegator delegator = dispatcher.getDelegator();

        // invoice created by the service
        String invoiceId = null;

        // NOTE: We build a list of OrderItems to submit to the createInvoiceForOrder service, however
        // that servcie will attempt to invoice the full quantity.  By changing the quantity field to that
        // of the input, we trick the service to invoicing only that amount.

        String orderId = null;  // this service supports only one orderId
        List orderItems = FastList.newInstance();
        Set orderItemSeqIdCompleted = FastSet.newInstance(); // for items that will be complete after invoicing
        Set workEffortIdCompleted = FastSet.newInstance(); // for work efforts that will be complete after invoicing (this service supports outsourced tasks only for now)

        Collection orderData = (Collection) context.get("orderData");
        for (Iterator iter = orderData.iterator(); iter.hasNext();) {
            Map row = (Map) iter.next();
            if (orderId == null) {
                orderId = (String) row.get("orderId");
            }
            String orderItemSeqId = (String) row.get("orderItemSeqId");
            String workEffortId = (String) row.get("workEffortId");
            String quantityString = (String) row.get("quantity");

            // make sure some quantity exists
            if (UtilValidate.isEmpty(quantityString)) {
                continue;
            }
            double quantity = 0;
            try {
                quantity = Double.parseDouble(quantityString);
            } catch (NumberFormatException e) {
                // TODO: actually show the bad value
                return UtilMessage.createAndLogServiceError("OpentapsFieldError_BadDoubleFormat", locale, MODULE);
            }
            if (quantity == 0) {
                continue;
            }

            try {
                GenericValue orderItem = delegator.findByPrimaryKey("OrderItem", UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId));
                double quantityOrdered = orderItem.getDouble("quantity").doubleValue();
                double quantityCancelled = (orderItem.get("cancelQuantity") == null ? 0.0 : orderItem.getDouble("cancelQuantity").doubleValue());
                double quantityFulfilled = 0.0;

                // if we're invoicing a work effort, then get the fulfillment
                GenericValue fulfillment = null;
                if (UtilValidate.isNotEmpty(workEffortId)) {
                    Map params = UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId, "workEffortId", workEffortId);
                    fulfillment = delegator.findByPrimaryKey("WorkOrderItemFulfillment", params);
                }

                // figure out what is the most that can be invoiced
                double maximum = quantityOrdered - quantityCancelled;
                if (fulfillment != null) {
                    quantityFulfilled = (fulfillment.get("quantityFulfilled") == null ? 0.0 : fulfillment.getDouble("quantityFulfilled").doubleValue());
                    maximum -= quantityFulfilled;
                } else {
                    double invoiced = org.opentaps.common.order.UtilOrder.getInvoicedQuantity(orderItem);
                    maximum -= invoiced;
                }

                if (maximum == 0.0) {
                    // if for some reason the item is already fully invoiced but still approved, complete it
                    if ("ITEM_APPROVED".equals(orderItem.get("statusId"))) {
                        orderItemSeqIdCompleted.add(orderItem.get("orderItemSeqId"));
                    }
                    // if for some reason the work effort is already fulfilled but is still qualified, complete it
                    if (fulfillment != null) {
                        GenericValue workEffort = fulfillment.getRelatedOne("WorkEffort");
                        if ("PRUN_OUTSRCD".equals(workEffort.get("currentStatusId"))) {
                            workEffortIdCompleted.add(workEffortId);
                        }
                    }
                    continue;
                }
                if (quantity > maximum) {
                    quantity = maximum;
                }
                if (quantity == maximum) {
                    orderItemSeqIdCompleted.add(orderItem.get("orderItemSeqId")); // schedule item to be completed
                    if (fulfillment != null) {
                        workEffortIdCompleted.add(workEffortId); // schedule work effort to be completed
                    }
                }

                // update fulfillment quantity
                if (fulfillment != null) {
                    fulfillment.set("quantityFulfilled", new Double(quantityFulfilled + quantity));
                    fulfillment.store();
                }

                // mark this order item to be invoiced for this quantity
                orderItem.set("quantity", new Double(quantity));
                orderItems.add(orderItem);

            } catch (GenericEntityException e) {
                return UtilMessage.createAndLogServiceError(e, MODULE);
            }
        }
        if (orderItems.size() == 0) {
            return UtilMessage.createAndLogServiceError("OpentapsError_NoItemsToProcess", locale, MODULE);
        }

        try {
            // try to create the invoice for this list of items
            Map input = UtilMisc.toMap("userLogin", userLogin, "orderId", orderId, "billItems", orderItems);
            Map results = dispatcher.runSync("createInvoiceForOrder", input);
            if (ServiceUtil.isError(results)) {
                return results;
            }
            invoiceId = (String) results.get("invoiceId");

            // mark each completed item as ITEM_COMPLETED
            for (Iterator iter = orderItemSeqIdCompleted.iterator(); iter.hasNext();) {
                String orderItemSeqId = (String) iter.next();
                input = UtilMisc.toMap("userLogin", userLogin, "orderId", orderId, "orderItemSeqId", orderItemSeqId, "fromStatusId", "ITEM_APPROVED", "statusId", "ITEM_COMPLETED");
                results = dispatcher.runSync("changeOrderItemStatus", input);
                if (ServiceUtil.isError(results)) {
                    return results;
                }
            }

            // mark each completed outsourced task as PRUN_RUNNING by hand, then change the status to PRUN_COMPLETED (this is kind of a hack)
            for (Iterator iter = workEffortIdCompleted.iterator(); iter.hasNext();) {
                String workEffortId = (String) iter.next();
                GenericValue workEffort = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId));
                workEffort.set("currentStatusId", "PRUN_RUNNING");
                workEffort.store();

                GenericValue productionRun = workEffort.getRelatedOneCache("ParentWorkEffort");
                results = dispatcher.runSync("changeProductionRunTaskStatus", UtilMisc.toMap("userLogin", userLogin, "workEffortId", workEffortId, "productionRunId", productionRun.get("workEffortId"), "statusId", "PRUN_COMPLETED"));
                if (ServiceUtil.isError(results)) {
                    return results;
                }
            }
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }

        Map results = ServiceUtil.returnSuccess();
        results.put("invoiceId", invoiceId);
        return results;
    }


    /**
     * Create an invoice from existing order.
     * TODO: refactor this monster.
     * This is the service from ofbiz, the only change is to properly set the accounting
     *  tags on the invoice items:
     *  - items that are from an order item are tagged the same as the original order item
     *  - the related adjustments are tagged the same as the related order item as well
     *  - the global adjustments are tagged the same as the first order item found
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map createInvoiceForOrder(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        if (decimals == -1 || rounding == -1) {
            return UtilMessage.createAndLogServiceError("AccountingAritmeticPropertiesNotConfigured", locale, MODULE);
        }

        String orderId = (String) context.get("orderId");
        List billItems = (List) context.get("billItems");
        boolean previousInvoiceFound = false;

        if (billItems == null || billItems.size() == 0) {
            return UtilMessage.createAndLogServiceSuccess("AccountingNoOrderItemsToInvoice", locale, MODULE);
        }

        try {

            GenericValue orderHeader = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId));
            if (orderHeader == null) {
                return UtilMessage.createAndLogServiceError("AccountingNoOrderHeader", locale, MODULE);
            }

            DomainsLoader domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(userLogin));
            DomainsDirectory dd = domainLoader.getDomainsDirectory();
            OrderRepositoryInterface orderRepository = dd.getOrderDomain().getOrderRepository();
            Order order = orderRepository.getOrderById(orderId);

            // get list of previous invoices for the order
            List billedItems = delegator.findByAnd("OrderItemBilling", UtilMisc.toMap("orderId", orderId));
            if (billedItems != null && billedItems.size() > 0) {
                boolean nonDigitalInvoice = false;
                Iterator bii = billedItems.iterator();
                while (bii.hasNext() && !nonDigitalInvoice) {
                    GenericValue orderItemBilling = (GenericValue) bii.next();
                    GenericValue invoiceItem = orderItemBilling.getRelatedOne("InvoiceItem");
                    if (invoiceItem != null) {
                        String invoiceItemType = invoiceItem.getString("invoiceItemTypeId");
                        if (invoiceItemType != null) {
                            if ("INV_FPROD_ITEM".equals(invoiceItemType) || "INV_PROD_FEATR_ITEM".equals(invoiceItemType)) {
                                nonDigitalInvoice = true;
                            }
                        }
                    }
                }
                if (nonDigitalInvoice) {
                    previousInvoiceFound = true;
                }
            }

            // figure out the invoice type
            String invoiceType = null;

            String orderType = orderHeader.getString("orderTypeId");
            if (orderType.equals("SALES_ORDER")) {
                invoiceType = "SALES_INVOICE";
            } else if (orderType.equals("PURCHASE_ORDER")) {
                invoiceType = "PURCHASE_INVOICE";
            }

            // Set the precision depending on the type of invoice
            int invoiceTypeDecimals = UtilNumber.getBigDecimalScale("invoice." + invoiceType + ".decimals");
            if (invoiceTypeDecimals == -1) {
                invoiceTypeDecimals = decimals;
            }

            // Make an order read helper from the order
            OrderReadHelper orh = new OrderReadHelper(orderHeader);

            // get the product store
            GenericValue productStore = delegator.findByPrimaryKey("ProductStore", UtilMisc.toMap("productStoreId", orh.getProductStoreId()));

            // get the shipping adjustment mode (Y = Pro-Rate; N = First-Invoice)
            String prorateShipping = productStore.getString("prorateShipping");
            if (prorateShipping == null) {
                prorateShipping = "Y";
            }

            // get the billing parties
            String billToCustomerPartyId = orh.getBillToParty().getString("partyId");
            String billFromVendorPartyId = orh.getBillFromParty().getString("partyId");

            // get some quantity totals
            BigDecimal totalItemsInOrder = orh.getTotalOrderItemsQuantity();

            // get some price totals
            BigDecimal shippableAmount = orh.getShippableTotal(null);
            BigDecimal orderSubTotal = orh.getOrderItemsSubTotal();

            // these variables are for pro-rating order amounts across invoices, so they should not be rounded off for maximum accuracy
            BigDecimal invoiceShipProRateAmount = ZERO;
            BigDecimal invoiceSubTotal = ZERO;
            BigDecimal invoiceQuantity = ZERO;

            GenericValue billingAccount = orderHeader.getRelatedOne("BillingAccount");
            String billingAccountId = billingAccount != null ? billingAccount.getString("billingAccountId") : null;

            // TODO: ideally this should be the same time as when a shipment is sent and be passed in as a parameter
            Timestamp invoiceDate = UtilDateTime.nowTimestamp();
            // TODO: perhaps consider billing account net days term as well?
            Long orderTermNetDays = orh.getOrderTermNetDays();
            Timestamp dueDate = null;
            if (orderTermNetDays != null) {
                dueDate = UtilDateTime.getDayEnd(invoiceDate, orderTermNetDays);
            }

            // create the invoice record
            Map createInvoiceContext = FastMap.newInstance();
            createInvoiceContext.put("partyId", billToCustomerPartyId);
            createInvoiceContext.put("partyIdFrom", billFromVendorPartyId);
            createInvoiceContext.put("billingAccountId", billingAccountId);
            createInvoiceContext.put("invoiceDate", invoiceDate);
            createInvoiceContext.put("dueDate", dueDate);
            createInvoiceContext.put("invoiceTypeId", invoiceType);
            // start with INVOICE_IN_PROCESS, in the INVOICE_READY we can't change the invoice (or shouldn't be able to...)
            createInvoiceContext.put("statusId", "INVOICE_IN_PROCESS");
            createInvoiceContext.put("currencyUomId", orderHeader.getString("currencyUom"));
            createInvoiceContext.put("userLogin", userLogin);

            // store the invoice first
            Map createInvoiceResult = dispatcher.runSync("createInvoice", createInvoiceContext);
            if (ServiceUtil.isError(createInvoiceResult)) {
                return UtilMessage.createAndLogServiceError(createInvoiceResult, "AccountingErrorCreatingInvoiceFromOrder", locale, MODULE);
            }

            // call service, not direct entity op: delegator.create(invoice);
            String invoiceId = (String) createInvoiceResult.get("invoiceId");

            // order roles to invoice roles
            List orderRoles = orderHeader.getRelated("OrderRole");
            if (orderRoles != null) {
                Iterator orderRolesIt = orderRoles.iterator();
                Map createInvoiceRoleContext = FastMap.newInstance();
                createInvoiceRoleContext.put("invoiceId", invoiceId);
                createInvoiceRoleContext.put("userLogin", userLogin);
                while (orderRolesIt.hasNext()) {
                    GenericValue orderRole = (GenericValue) orderRolesIt.next();
                    createInvoiceRoleContext.put("partyId", orderRole.getString("partyId"));
                    createInvoiceRoleContext.put("roleTypeId", orderRole.getString("roleTypeId"));
                    Map createInvoiceRoleResult = dispatcher.runSync("createInvoiceRole", createInvoiceRoleContext);
                    if (ServiceUtil.isError(createInvoiceRoleResult)) {
                        return UtilMessage.createAndLogServiceError(createInvoiceRoleResult, "AccountingErrorCreatingInvoiceFromOrder", locale, MODULE);
                    }
                }
            }

            // order terms to invoice terms.  Implemented for purchase orders, although it may be useful
            // for sales orders as well.  Later it might be nice to filter OrderTerms to only copy over financial terms.
            List orderTerms = orh.getOrderTerms();
            createInvoiceTerms(delegator, dispatcher, invoiceId, orderTerms, userLogin, locale);

            // billing accounts
            List billingAccountTerms = null;
            // for billing accounts we will use related information
            if (billingAccount != null) {
                // get the billing account terms
                billingAccountTerms = billingAccount.getRelated("BillingAccountTerm");

                // set the invoice terms as defined for the billing account
                createInvoiceTerms(delegator, dispatcher, invoiceId, billingAccountTerms, userLogin, locale);

                // set the invoice bill_to_customer from the billing account
                List billToRoles = billingAccount.getRelated("BillingAccountRole", UtilMisc.toMap("roleTypeId", "BILL_TO_CUSTOMER"), null);
                Iterator billToIter = billToRoles.iterator();
                while (billToIter.hasNext()) {
                    GenericValue billToRole = (GenericValue) billToIter.next();
                    if (!(billToRole.getString("partyId").equals(billToCustomerPartyId))) {
                        Map createInvoiceRoleContext = UtilMisc.toMap("invoiceId", invoiceId, "partyId", billToRole.get("partyId"),
                                                                           "roleTypeId", "BILL_TO_CUSTOMER", "userLogin", userLogin);
                        Map createInvoiceRoleResult = dispatcher.runSync("createInvoiceRole", createInvoiceRoleContext);
                        if (ServiceUtil.isError(createInvoiceRoleResult)) {
                            return UtilMessage.createAndLogServiceError(createInvoiceRoleResult, "AccountingErrorCreatingInvoiceRoleFromOrder", locale, MODULE);
                        }
                    }
                }

                // set the bill-to contact mech as the contact mech of the billing account
                if (UtilValidate.isNotEmpty(billingAccount.getString("contactMechId"))) {
                    // check that it is not already set
                    if (delegator.findByPrimaryKey("InvoiceContactMech", UtilMisc.toMap("invoiceId", invoiceId, "contactMechId", billingAccount.getString("contactMechId"), "contactMechPurposeTypeId", "BILLING_LOCATION")) == null) {
                        Map createBillToContactMechContext = UtilMisc.toMap("invoiceId", invoiceId, "contactMechId", billingAccount.getString("contactMechId"),
                                                                       "contactMechPurposeTypeId", "BILLING_LOCATION", "userLogin", userLogin);
                        Map createBillToContactMechResult = dispatcher.runSync("createInvoiceContactMech", createBillToContactMechContext);
                        if (ServiceUtil.isError(createBillToContactMechResult)) {
                            return UtilMessage.createAndLogServiceError(createBillToContactMechResult, "AccountingErrorCreatingInvoiceContactMechFromOrder", locale, MODULE);
                        }
                    }
                }
            } else {
                List billingLocations = orh.getBillingLocations();
                if (UtilValidate.isNotEmpty(billingLocations)) {
                    Iterator bli = billingLocations.iterator();
                    while (bli.hasNext()) {
                        GenericValue ocm = (GenericValue) bli.next();
                        // check that it is not already set
                        if (delegator.findByPrimaryKey("InvoiceContactMech", UtilMisc.toMap("invoiceId", invoiceId, "contactMechId", ocm.getString("contactMechId"), "contactMechPurposeTypeId", "BILLING_LOCATION")) == null) {
                            Map createBillToContactMechContext = UtilMisc.toMap("invoiceId", invoiceId, "contactMechId", ocm.getString("contactMechId"),
                                                                           "contactMechPurposeTypeId", "BILLING_LOCATION", "userLogin", userLogin);
                            Map createBillToContactMechResult = dispatcher.runSync("createInvoiceContactMech", createBillToContactMechContext);
                            if (ServiceUtil.isError(createBillToContactMechResult)) {
                                return UtilMessage.createAndLogServiceError(createBillToContactMechResult, "AccountingErrorCreatingInvoiceContactMechFromOrder", locale, MODULE);
                            }
                        }
                    }
                } else {
                    Debug.logWarning("No billing locations found for order [" + orderId + "] and none were created for Invoice [" + invoiceId + "]", MODULE);
                }
            }

            // get a list of the payment method types
            //DEJ20050705 doesn't appear to be used: List paymentPreferences = orderHeader.getRelated("OrderPaymentPreference");

            // create the bill-from (or pay-to) contact mech as the primary PAYMENT_LOCATION of the party from the store
            GenericValue payToAddress = null;
            if (invoiceType.equals("PURCHASE_INVOICE")) {
                // for purchase orders, the pay to address is the BILLING_LOCATION of the vendor
                GenericValue billFromVendor = orh.getPartyFromRole("BILL_FROM_VENDOR");
                if (billFromVendor != null) {
                    List billingContactMechs = billFromVendor.getRelatedOne("Party").getRelatedByAnd("PartyContactMechPurpose",
                            UtilMisc.toMap("contactMechPurposeTypeId", "BILLING_LOCATION"));
                    if ((billingContactMechs != null) && (billingContactMechs.size() > 0)) {
                        payToAddress = (GenericValue) billingContactMechs.get(0);
                    }
                }
            } else {
                // for sales orders, it is the payment address on file for the store
                payToAddress = PaymentWorker.getPaymentAddress(delegator, productStore.getString("payToPartyId"));
            }
            if (payToAddress != null) {
                Map createPayToContactMechContext = UtilMisc.toMap("invoiceId", invoiceId, "contactMechId", payToAddress.getString("contactMechId"),
                                                                   "contactMechPurposeTypeId", "PAYMENT_LOCATION", "userLogin", userLogin);
                Map createPayToContactMechResult = dispatcher.runSync("createInvoiceContactMech", createPayToContactMechContext);
                if (ServiceUtil.isError(createPayToContactMechResult)) {
                    return UtilMessage.createAndLogServiceError(createPayToContactMechResult, "AccountingErrorCreatingInvoiceContactMechFromOrder", locale, MODULE);
                }
            }

            // sequence for items - all OrderItems or InventoryReservations + all Adjustments
            int invoiceItemSeqNum = 1;
            String invoiceItemSeqId = UtilFormatOut.formatPaddedNumber(invoiceItemSeqNum, INVOICE_ITEM_SEQUENCE_ID_DIGITS);

            // later the other global adjustments will be tagged the same as the first order item
            GenericValue firstOrderItem = null;
            Map globalAccountingTags = new HashMap();

            // create the item records
            if (billItems != null) {
                Iterator itemIter = billItems.iterator();
                while (itemIter.hasNext()) {
                    GenericValue itemIssuance = null;
                    GenericValue orderItem = null;
                    GenericValue shipmentReceipt = null;
                    GenericValue currentValue = (GenericValue) itemIter.next();
                    if ("ItemIssuance".equals(currentValue.getEntityName())) {
                        itemIssuance = currentValue;
                    } else if ("OrderItem".equals(currentValue.getEntityName())) {
                        orderItem = currentValue;
                    } else if ("ShipmentReceipt".equals(currentValue.getEntityName())) {
                        shipmentReceipt = currentValue;
                    } else {
                        Debug.logError("Unexpected entity " + currentValue + " of type " + currentValue.getEntityName(), MODULE);
                    }

                    if (orderItem == null && itemIssuance != null) {
                        orderItem = itemIssuance.getRelatedOne("OrderItem");
                    } else if ((orderItem == null) && (shipmentReceipt != null)) {
                        orderItem = shipmentReceipt.getRelatedOne("OrderItem");
                    } else if ((orderItem == null) && (itemIssuance == null) && (shipmentReceipt == null)) {
                        Debug.logError("Cannot create invoice when orderItem, itemIssuance, and shipmentReceipt are all null", MODULE);
                        return UtilMessage.createAndLogServiceError("AccountingIllegalValuesPassedToCreateInvoiceService", locale, MODULE);
                    }

                    if (firstOrderItem == null) {
                        firstOrderItem = orderItem;
                        UtilAccountingTags.putAllAccountingTags(firstOrderItem, globalAccountingTags);
                    }

                    GenericValue product = null;
                    if (orderItem.get("productId") != null) {
                        product = orderItem.getRelatedOne("Product");
                    }

                    // get some quantities
                    BigDecimal orderedQuantity = orderItem.getBigDecimal("quantity");
                    BigDecimal billingQuantity = null;
                    if (itemIssuance != null) {
                        billingQuantity = itemIssuance.getBigDecimal("quantity");
                    } else if (shipmentReceipt != null) {
                        billingQuantity = shipmentReceipt.getBigDecimal("quantityAccepted");
                    } else {
                        billingQuantity = orderedQuantity;
                    }
                    if (orderedQuantity == null) {
                        orderedQuantity = ZERO;
                    }
                    if (billingQuantity == null) {
                        billingQuantity = ZERO;
                    }

                    // check if shipping applies to this item.  Shipping is calculated for sales invoices, not purchase invoices.
                    boolean shippingApplies = false;
                    if ((product != null) && (ProductWorker.shippingApplies(product)) && (invoiceType.equals("SALES_INVOICE"))) {
                        shippingApplies = true;
                    }

                    BigDecimal billingAmount = orderItem.getBigDecimal("unitPrice").setScale(invoiceTypeDecimals, rounding);

                    Map createInvoiceItemContext = FastMap.newInstance();
                    createInvoiceItemContext.put("invoiceId", invoiceId);
                    createInvoiceItemContext.put("invoiceItemSeqId", invoiceItemSeqId);
                    createInvoiceItemContext.put("invoiceItemTypeId", getInvoiceItemType(delegator, (orderItem == null ? null : orderItem.getString("orderItemTypeId")), (product == null ? null : product.getString("productTypeId")), invoiceType, "INV_FPROD_ITEM"));
                    createInvoiceItemContext.put("description", orderItem.get("itemDescription"));
                    createInvoiceItemContext.put("quantity", billingQuantity);
                    createInvoiceItemContext.put("amount", billingAmount);
                    createInvoiceItemContext.put("productId", orderItem.get("productId"));
                    createInvoiceItemContext.put("productFeatureId", orderItem.get("productFeatureId"));
                    createInvoiceItemContext.put("overrideGlAccountId", orderItem.get("overrideGlAccountId"));
                    createInvoiceItemContext.put("userLogin", userLogin);
                    // copy the accounting tags from the related order item
                    UtilAccountingTags.putAllAccountingTags(orderItem, createInvoiceItemContext);

                    String itemIssuanceId = null;
                    if (itemIssuance != null && itemIssuance.get("inventoryItemId") != null) {
                        itemIssuanceId = itemIssuance.getString("itemIssuanceId");
                        createInvoiceItemContext.put("inventoryItemId", itemIssuance.get("inventoryItemId"));
                    }
                    // similarly, tax only for purchase invoices
                    if ((product != null) && (invoiceType.equals("SALES_INVOICE"))) {
                        createInvoiceItemContext.put("taxableFlag", product.get("taxable"));
                    }

                    Map createInvoiceItemResult = dispatcher.runSync("createInvoiceItem", createInvoiceItemContext);
                    if (ServiceUtil.isError(createInvoiceItemResult)) {
                        UtilMessage.createAndLogServiceError(createInvoiceItemResult, "AccountingErrorCreatingInvoiceItemFromOrder", locale, MODULE);
                    }

                    // this item total
                    BigDecimal thisAmount = billingAmount.multiply(billingQuantity).setScale(invoiceTypeDecimals, rounding);

                    // add to the ship amount only if it applies to this item
                    if (shippingApplies) {
                        invoiceShipProRateAmount = invoiceShipProRateAmount.add(thisAmount).setScale(invoiceTypeDecimals, rounding);
                    }

                    // increment the invoice subtotal
                    invoiceSubTotal = invoiceSubTotal.add(thisAmount).setScale(100, rounding);

                    // increment the invoice quantity
                    invoiceQuantity = invoiceQuantity.add(billingQuantity).setScale(invoiceTypeDecimals, rounding);

                    // create the OrderItemBilling record
                    Map createOrderItemBillingContext = FastMap.newInstance();
                    createOrderItemBillingContext.put("invoiceId", invoiceId);
                    createOrderItemBillingContext.put("invoiceItemSeqId", invoiceItemSeqId);
                    createOrderItemBillingContext.put("orderId", orderItem.get("orderId"));
                    createOrderItemBillingContext.put("orderItemSeqId", orderItem.get("orderItemSeqId"));
                    createOrderItemBillingContext.put("itemIssuanceId", itemIssuanceId);
                    createOrderItemBillingContext.put("quantity", billingQuantity);
                    createOrderItemBillingContext.put("amount", billingAmount);
                    createOrderItemBillingContext.put("userLogin", userLogin);
                    if ((shipmentReceipt != null) && (shipmentReceipt.getString("receiptId") != null)) {
                        createOrderItemBillingContext.put("shipmentReceiptId", shipmentReceipt.getString("receiptId"));
                    }

                    Map createOrderItemBillingResult = dispatcher.runSync("createOrderItemBilling", createOrderItemBillingContext);
                    if (ServiceUtil.isError(createOrderItemBillingResult)) {
                        UtilMessage.createAndLogServiceError(createOrderItemBillingResult, "AccountingErrorCreatingOrderItemBillingFromOrder", locale, MODULE);
                    }

                    if ("ItemIssuance".equals(currentValue.getEntityName())) {

                        // create the ShipmentItemBilling record
                        GenericValue shipmentItemBilling = delegator.makeValue("ShipmentItemBilling", UtilMisc.toMap("invoiceId", invoiceId, "invoiceItemSeqId", invoiceItemSeqId));
                        shipmentItemBilling.put("shipmentId", currentValue.get("shipmentId"));
                        shipmentItemBilling.put("shipmentItemSeqId", currentValue.get("shipmentItemSeqId"));
                        shipmentItemBilling.create();
                    }

                    String parentInvoiceItemSeqId = invoiceItemSeqId;
                    // increment the counter
                    invoiceItemSeqNum++;
                    invoiceItemSeqId = UtilFormatOut.formatPaddedNumber(invoiceItemSeqNum, INVOICE_ITEM_SEQUENCE_ID_DIGITS);

                    // Get the original order item from the DB, in case the quantity has been overridden
                    OrderItem originalOrderItem = order.getOrderItem(orderItem.getString("orderItemSeqId"));

                    // create the item adjustment as line items
                    List itemAdjustments = OrderReadHelper.getOrderItemAdjustmentList(orderItem, orh.getAdjustments());
                    Iterator itemAdjIter = itemAdjustments.iterator();
                    while (itemAdjIter.hasNext()) {
                        GenericValue adj = (GenericValue) itemAdjIter.next();

                        Debug.logInfo("For OrderAdjustment [" + adj.get("orderAdjustmentId") + "] of type " + adj.get("orderAdjustmentTypeId"), MODULE);
                        // Check against OrderAdjustmentBilling to see how much of this adjustment has already been invoiced
                        BigDecimal adjAlreadyInvoicedAmount = null;
                        try {
                            Map checkResult = dispatcher.runSync("calculateInvoicedAdjustmentTotal", UtilMisc.toMap("orderAdjustment", adj));
                            adjAlreadyInvoicedAmount = (BigDecimal) checkResult.get("invoicedTotal");
                            Debug.logInfo("amount already invoiced for this adjustment is : " + adjAlreadyInvoicedAmount, MODULE);
                        } catch (GenericServiceException e) {
                            UtilMessage.createAndLogServiceError(e, "AccountingTroubleCallingCalculateInvoicedAdjustmentTotalService", locale, MODULE);
                        }

                        // to determine if the full adjustment was already invoiced
                        BigDecimal adjFullAmount = ZERO;
                        // determine the amount to bill for this adjustment, as it is pro rated according to the quantity
                        //  of the item it totally applies to vs. the quantity of items billed
                        BigDecimal amount = ZERO;
                        // get the original quantity of the item in the adjustment ship group (if a ship group is set)
                        BigDecimal originalOrderItemQty = originalOrderItem.getOrderedQuantity();
                        String shipGroupSeqId = adj.getString("shipGroupSeqId");
                        OrderItemShipGroup shipGroup = null;
                        // some adjustment have _NA_ as the shipGroupSeqId
                        if (UtilValidate.isNotEmpty(shipGroupSeqId) && !"_NA_".equals(shipGroupSeqId)) {
                            shipGroup = order.getOrderItemShipGroup(shipGroupSeqId);
                            originalOrderItemQty = originalOrderItem.getOrderedQuantity(shipGroup);
                        }
                        // get the quantity to apply the adjustment to, usually it is the billingQuantity unless the adjustment has a appliesToQuantity set
                        BigDecimal appliesToQty = billingQuantity;
                        if (adj.getBigDecimal("appliesToQuantity") != null) {
                            // then applies to the minimum of those quantities
                            appliesToQty = adj.getBigDecimal("appliesToQuantity").min(billingQuantity);
                            // and limit the pro rating
                            originalOrderItemQty = adj.getBigDecimal("appliesToQuantity").min(originalOrderItemQty);
                        }

                        if (adj.get("amount") != null) {
                            amount = adj.getBigDecimal("amount");
                            adjFullAmount = amount;
                            // pro-rate the amount
                            // set decimals = 100 means we don't round this intermediate value, which is very important
                            amount = amount.divide(originalOrderItemQty, 100, rounding);
                            amount = amount.multiply(appliesToQty);
                            // Tax needs to be rounded differently from other order adjustments
                            if (adj.getString("orderAdjustmentTypeId").equals("SALES_TAX")) {
                                amount = amount.setScale(taxDecimals, taxRounding);
                            } else {
                                amount = amount.setScale(invoiceTypeDecimals, rounding);
                            }
                            Debug.logInfo("the adjustment amount was originally : " + adj.get("amount") + " and for this item is pro-rated to : " + amount, MODULE);
                        } else if (adj.get("sourcePercentage") != null) {
                            // pro-rate the amount
                            // set decimals = 100 means we don't round this intermediate value, which is very important
                            BigDecimal percent = adj.getBigDecimal("sourcePercentage");
                            percent = percent.divide(new BigDecimal(100), 100, rounding);
                            amount = billingAmount.multiply(percent);
                            adjFullAmount = amount.multiply(originalOrderItemQty);
                            amount = amount.multiply(appliesToQty);
                            amount = amount.setScale(invoiceTypeDecimals, rounding);
                            Debug.logInfo("the adjustment amount was originally null, percentage is : " + adj.get("sourcePercentage") + " and for this item is pro-rated to : " + amount, MODULE);
                        }

                        // JLR 17/4/7 : fix a bug coming from POS in case of use of a discount (on item(s) or sale, item(s) here) and a cash amount higher than total (hence issuing change)
                        if (null == amount) {
                            Debug.logWarning("Null amount, skipping this adjustment.", MODULE);
                            continue;
                        }
                        // If the absolute invoiced amount >= the abs of the adjustment full amount, the full amount has already been invoiced,
                        //  so skip this adjustment
                        Debug.logInfo("Comparing adjFullAmount.abs =" + adjFullAmount.setScale(invoiceTypeDecimals, rounding).abs() + " and adjAlreadyInvoicedAmount.abs =" + adjAlreadyInvoicedAmount.abs() + " >> " + adjAlreadyInvoicedAmount.abs().compareTo(adjFullAmount.setScale(invoiceTypeDecimals, rounding).abs()), MODULE);
                        if (adjAlreadyInvoicedAmount.abs().compareTo(adjFullAmount.setScale(invoiceTypeDecimals, rounding).abs()) >= 0) {
                            Debug.logWarning("Absolute invoiced amount : " + adjAlreadyInvoicedAmount.abs() + " >= the absolute full amount of the adjustment : " + adjFullAmount.setScale(invoiceTypeDecimals, rounding).abs() + ", the full amount has already been invoiced, skipping this adjustment [" + adj.get("orderAdjustmentId") + "].", MODULE);
                            continue;
                        }

                        if (amount.signum() != 0) {
                            Map createInvoiceItemAdjContext = FastMap.newInstance();
                            createInvoiceItemAdjContext.put("invoiceId", invoiceId);
                            createInvoiceItemAdjContext.put("invoiceItemSeqId", invoiceItemSeqId);
                            createInvoiceItemAdjContext.put("invoiceItemTypeId", getInvoiceItemType(delegator, adj.getString("orderAdjustmentTypeId"), null, invoiceType, "INVOICE_ITM_ADJ"));
                            createInvoiceItemAdjContext.put("quantity", BigDecimal.ONE);
                            createInvoiceItemAdjContext.put("amount", amount);
                            createInvoiceItemAdjContext.put("productId", orderItem.get("productId"));
                            createInvoiceItemAdjContext.put("productFeatureId", orderItem.get("productFeatureId"));
                            createInvoiceItemAdjContext.put("overrideGlAccountId", adj.get("overrideGlAccountId"));
                            createInvoiceItemAdjContext.put("parentInvoiceId", invoiceId);
                            createInvoiceItemAdjContext.put("parentInvoiceItemSeqId", parentInvoiceItemSeqId);
                            //createInvoiceItemAdjContext.put("uomId", "");
                            createInvoiceItemAdjContext.put("userLogin", userLogin);
                            createInvoiceItemAdjContext.put("taxAuthPartyId", adj.get("taxAuthPartyId"));
                            createInvoiceItemAdjContext.put("taxAuthGeoId", adj.get("taxAuthGeoId"));
                            createInvoiceItemAdjContext.put("taxAuthorityRateSeqId", adj.get("taxAuthorityRateSeqId"));
                            // copy the accounting tags from the related order item
                            UtilAccountingTags.putAllAccountingTags(orderItem, createInvoiceItemAdjContext);

                            // some adjustments fill out the comments field instead
                            String description = (UtilValidate.isEmpty(adj.getString("description")) ? adj.getString("comments") : adj.getString("description"));
                            createInvoiceItemAdjContext.put("description", description);

                            // invoice items for sales tax are not taxable themselves
                            // TODO: This is not an ideal solution. Instead, we need to use OrderAdjustment.includeInTax when it is implemented
                            if (!(adj.getString("orderAdjustmentTypeId").equals("SALES_TAX"))) {
                                createInvoiceItemAdjContext.put("taxableFlag", product.get("taxable"));
                            }

                            Map createInvoiceItemAdjResult = dispatcher.runSync("createInvoiceItem", createInvoiceItemAdjContext);
                            if (ServiceUtil.isError(createInvoiceItemAdjResult)) {
                                UtilMessage.createAndLogServiceError(createInvoiceItemAdjResult, "AccountingErrorCreatingInvoiceItemFromOrder", locale, MODULE);
                            }

                            // Create the OrderAdjustmentBilling record
                            Map createOrderAdjustmentBillingContext = FastMap.newInstance();
                            createOrderAdjustmentBillingContext.put("orderAdjustmentId", adj.getString("orderAdjustmentId"));
                            createOrderAdjustmentBillingContext.put("invoiceId", invoiceId);
                            createOrderAdjustmentBillingContext.put("invoiceItemSeqId", invoiceItemSeqId);
                            createOrderAdjustmentBillingContext.put("amount", amount);
                            createOrderAdjustmentBillingContext.put("userLogin", userLogin);

                            Map createOrderAdjustmentBillingResult = dispatcher.runSync("createOrderAdjustmentBilling", createOrderAdjustmentBillingContext);
                            if (ServiceUtil.isError(createOrderAdjustmentBillingResult)) {
                                UtilMessage.createAndLogServiceError(createOrderAdjustmentBillingResult, "AccountingErrorCreatingOrderAdjustmentBillingFromOrder", locale, MODULE);
                            }

                            // this adjustment amount
                            BigDecimal thisAdjAmount = new BigDecimal(amount.doubleValue());

                            // adjustments only apply to totals when they are not tax or shipping adjustments
                            if (!"SALES_TAX".equals(adj.getString("orderAdjustmentTypeId")) && !"SHIPPING_ADJUSTMENT".equals(adj.getString("orderAdjustmentTypeId"))) {
                                // increment the invoice subtotal
                                invoiceSubTotal = invoiceSubTotal.add(thisAdjAmount).setScale(100, rounding);

                                // add to the ship amount only if it applies to this item
                                if (shippingApplies) {
                                    invoiceShipProRateAmount = invoiceShipProRateAmount.add(thisAdjAmount).setScale(invoiceTypeDecimals, rounding);
                                }
                            }

                            // increment the counter
                            invoiceItemSeqNum++;
                            invoiceItemSeqId = UtilFormatOut.formatPaddedNumber(invoiceItemSeqNum, INVOICE_ITEM_SEQUENCE_ID_DIGITS);
                        }
                    }
                }
            }

            // create header adjustments as line items -- always to tax/shipping last
            Map shipAdjustments = new HashMap();
            Map taxAdjustments = new HashMap();

            List headerAdjustments = orh.getOrderHeaderAdjustments();
            Iterator headerAdjIter = headerAdjustments.iterator();
            while (headerAdjIter.hasNext()) {
                GenericValue adj = (GenericValue) headerAdjIter.next();
                Debug.logInfo("For OrderAdjustment [" + adj.get("orderAdjustmentId") + "] of type " + adj.get("orderAdjustmentTypeId"), MODULE);

                // Check against OrderAdjustmentBilling to see how much of this adjustment has already been invoiced
                BigDecimal adjAlreadyInvoicedAmount = null;
                try {
                    Map checkResult = dispatcher.runSync("calculateInvoicedAdjustmentTotal", UtilMisc.toMap("orderAdjustment", adj));
                    adjAlreadyInvoicedAmount = ((BigDecimal) checkResult.get("invoicedTotal")).setScale(invoiceTypeDecimals, rounding);
                    Debug.logInfo("amount already invoiced for this adjustment is : " + adjAlreadyInvoicedAmount, MODULE);
                } catch (GenericServiceException e) {
                    UtilMessage.createAndLogServiceError("AccountingTroubleCallingCalculateInvoicedAdjustmentTotalService", locale, MODULE);
                }

                // If the absolute invoiced amount >= the abs of the adjustment amount, the full amount has already been invoiced,
                //  so skip this adjustment
                if (null == adj.get("amount")) { // JLR 17/4/7 : fix a bug coming from POS in case of use of a discount (on item(s) or sale, sale here) and a cash amount higher than total (hence issuing change)
                    Debug.logWarning("Null amount, skipping this adjustment.", MODULE);
                    continue;
                }
                Debug.logInfo("Comparing adjFullAmount.abs =" + adj.getBigDecimal("amount").setScale(invoiceTypeDecimals, rounding).abs() + " and adjAlreadyInvoicedAmount.abs =" + adjAlreadyInvoicedAmount.abs() + " >> " + adjAlreadyInvoicedAmount.abs().compareTo(adj.getBigDecimal("amount").setScale(invoiceTypeDecimals, rounding).abs()), MODULE);
                if (adjAlreadyInvoicedAmount.abs().compareTo(adj.getBigDecimal("amount").setScale(invoiceTypeDecimals, rounding).abs()) >= 0) {
                    Debug.logWarning("Absolute invoiced amount : " + adjAlreadyInvoicedAmount.abs() + " >= the absolute full amount of the adjustment : " + adj.getBigDecimal("amount").setScale(invoiceTypeDecimals, rounding).abs() + ", the full amount has already been invoiced, skipping this adjustment [" + adj.get("orderAdjustmentId") + "].", MODULE);
                    continue;
                }

                if ("SHIPPING_CHARGES".equals(adj.getString("orderAdjustmentTypeId"))) {
                    Debug.logInfo("Defer SHIPPING_CHARGES adj [" + adj.get("orderAdjustmentId") + "] with adjAlreadyInvoicedAmount = " + adjAlreadyInvoicedAmount, MODULE);
                    shipAdjustments.put(adj, adjAlreadyInvoicedAmount);
                } else if ("SALES_TAX".equals(adj.getString("orderAdjustmentTypeId"))) {
                    Debug.logInfo("Defer SALES_TAX adj [" + adj.get("orderAdjustmentId") + "] with adjAlreadyInvoicedAmount = " + adjAlreadyInvoicedAmount, MODULE);
                    taxAdjustments.put(adj, adjAlreadyInvoicedAmount);
                } else {
                    // these will effect the shipping pro-rate (unless commented)
                    // other adjustment type
                    BigDecimal adjAmount = calcHeaderAdj(delegator, adj, globalAccountingTags, invoiceType, invoiceId, invoiceItemSeqId,
                            orderSubTotal, invoiceSubTotal, adj.getBigDecimal("amount").setScale(invoiceTypeDecimals, rounding), invoiceTypeDecimals, rounding, userLogin, dispatcher, locale);
                    // invoiceShipProRateAmount += adjAmount;
                    // do adjustments compound or are they based off subtotal? Here we will (unless commented)
                    // invoiceSubTotal += adjAmount;

                    // increment the counter
                    invoiceItemSeqNum++;
                    invoiceItemSeqId = UtilFormatOut.formatPaddedNumber(invoiceItemSeqNum, INVOICE_ITEM_SEQUENCE_ID_DIGITS);
                }
            }

            // next do the shipping adjustments.  Note that we do not want to add these to the invoiceSubTotal or orderSubTotal for pro-rating tax later, as that would cause
            // numerator/denominator problems when the shipping is not pro-rated but rather charged all on the first invoice
            Iterator shipAdjIter = shipAdjustments.keySet().iterator();
            while (shipAdjIter.hasNext()) {
                GenericValue adj = (GenericValue) shipAdjIter.next();
                BigDecimal adjAlreadyInvoicedAmount = (BigDecimal) shipAdjustments.get(adj);
                Debug.logInfo("For OrderAdjustment [" + adj.get("orderAdjustmentId") + "] of type " + adj.get("orderAdjustmentTypeId") + ", amount already invoiced for this adjustment is : " + adjAlreadyInvoicedAmount, MODULE);

                if ("N".equalsIgnoreCase(prorateShipping)) {

                    // Set the divisor and multiplier to 1 to avoid prorating
                    BigDecimal divisor = BigDecimal.ONE;
                    BigDecimal multiplier = BigDecimal.ONE;

                    // The base amount in this case is the adjustment amount minus the total already invoiced for that adjustment, since
                    //  it won't be prorated
                    BigDecimal baseAmount = adj.getBigDecimal("amount").setScale(invoiceTypeDecimals, rounding).subtract(adjAlreadyInvoicedAmount);
                    BigDecimal adjAmount = calcHeaderAdj(delegator, adj, globalAccountingTags, invoiceType, invoiceId, invoiceItemSeqId,
                            divisor, multiplier, baseAmount, invoiceTypeDecimals, rounding, userLogin, dispatcher, locale);
                } else {

                    // Pro-rate the shipping amount based on shippable information
                    BigDecimal divisor = shippableAmount;
                    BigDecimal multiplier = invoiceShipProRateAmount;

                    // The base amount in this case is the adjustment amount, since we want to prorate based on the full amount
                    BigDecimal baseAmount = adj.getBigDecimal("amount").setScale(invoiceTypeDecimals, rounding);
                    BigDecimal adjAmount = calcHeaderAdj(delegator, adj, globalAccountingTags, invoiceType, invoiceId, invoiceItemSeqId,
                            divisor, multiplier, baseAmount, invoiceTypeDecimals, rounding, userLogin, dispatcher, locale);
                }

                // Increment the counter
                invoiceItemSeqNum++;
                invoiceItemSeqId = UtilFormatOut.formatPaddedNumber(invoiceItemSeqNum, INVOICE_ITEM_SEQUENCE_ID_DIGITS);
            }

            // last do the tax adjustments
            String prorateTaxes = productStore.getString("prorateTaxes");
            if (prorateTaxes == null) {
                prorateTaxes = "Y";
            }
            Iterator taxAdjIter = taxAdjustments.keySet().iterator();
            while (taxAdjIter.hasNext()) {
                GenericValue adj = (GenericValue) taxAdjIter.next();
                BigDecimal adjAlreadyInvoicedAmount = (BigDecimal) taxAdjustments.get(adj);
                Debug.logInfo("For OrderAdjustment [" + adj.get("orderAdjustmentId") + "] of type " + adj.get("orderAdjustmentTypeId") + ", amount already invoiced for this adjustment is : " + adjAlreadyInvoicedAmount, MODULE);
                BigDecimal adjAmount = null;
                // for example some adjustments like offsetting adjustments that were created after an order change
                // cannot be prorated (because they did not exist when the order was first billed).
                Debug.logInfo("Product Store prorating setting is : " + prorateTaxes + ", adj neverProrate = " + adj.get("neverProrate"), MODULE);

                if ("N".equalsIgnoreCase(prorateTaxes) || "Y".equalsIgnoreCase(adj.getString("neverProrate"))) {

                    // Set the divisor and multiplier to 1 to avoid prorating
                    BigDecimal divisor = BigDecimal.ONE;
                    BigDecimal multiplier = BigDecimal.ONE;

                    // The base amount in this case is the adjustment amount minus the total already invoiced for that adjustment, since
                    //  it won't be prorated
                    //  Note this should use invoice decimals & rounding instead of taxDecimals and taxRounding for tax adjustments, because it will be added to the invoice
                    BigDecimal baseAmount = adj.getBigDecimal("amount").setScale(invoiceTypeDecimals, rounding).subtract(adjAlreadyInvoicedAmount);
                    adjAmount = calcHeaderAdj(delegator, adj, globalAccountingTags, invoiceType, invoiceId, invoiceItemSeqId, divisor, multiplier, baseAmount, invoiceTypeDecimals, rounding, userLogin, dispatcher, locale);
                    Debug.logInfo("Not prorated global tax, billed = " + adjAmount, MODULE);
                } else {

                    // Pro-rate the tax amount based on shippable information
                    BigDecimal divisor = orderSubTotal;
                    BigDecimal multiplier = invoiceSubTotal;

                    // The base amount in this case is the adjustment amount, since we want to prorate based on the full amount
                    //  Note this should use invoice decimals & rounding instead of taxDecimals and taxRounding for tax adjustments, because it will be added to the invoice
                    BigDecimal baseAmount = adj.getBigDecimal("amount").setScale(invoiceTypeDecimals, rounding);
                    adjAmount = calcHeaderAdj(delegator, adj, globalAccountingTags, invoiceType, invoiceId, invoiceItemSeqId, divisor, multiplier, baseAmount, invoiceTypeDecimals, rounding, userLogin, dispatcher, locale);
                    Debug.logInfo("Prorated global tax based on orderSubTotal = " + orderSubTotal + " and invoiceSubTotal = " + invoiceSubTotal + ", billed = " + adjAmount, MODULE);
                }
                invoiceSubTotal = invoiceSubTotal.add(adjAmount).setScale(invoiceTypeDecimals, rounding);

                // Increment the counter
                invoiceItemSeqNum++;
                invoiceItemSeqId = UtilFormatOut.formatPaddedNumber(invoiceItemSeqNum, INVOICE_ITEM_SEQUENCE_ID_DIGITS);
            }

            // check for previous order payments
            List orderPaymentPrefs = delegator.findByAnd("OrderPaymentPreference", UtilMisc.toMap("orderId", orderId));
            if (orderPaymentPrefs != null) {
                List currentPayments = new ArrayList();
                Iterator opi = orderPaymentPrefs.iterator();
                while (opi.hasNext()) {
                    GenericValue paymentPref = (GenericValue) opi.next();
                    List payments = paymentPref.getRelated("Payment");
                    currentPayments.addAll(payments);
                }
                if (currentPayments.size() > 0) {
                    // apply these payments to the invoice if they have any remaining amount to apply
                    Iterator cpi = currentPayments.iterator();
                    while (cpi.hasNext()) {
                        GenericValue payment = (GenericValue) cpi.next();
                        BigDecimal notApplied = PaymentWorker.getPaymentNotApplied(payment);
                        if (notApplied.signum() > 0) {
                            Map appl = new HashMap();
                            appl.put("paymentId", payment.get("paymentId"));
                            appl.put("invoiceId", invoiceId);
                            appl.put("billingAccountId", billingAccountId);
                            appl.put("amountApplied", notApplied);
                            appl.put("userLogin", userLogin);
                            Map createPayApplResult = dispatcher.runSync("createPaymentApplication", appl);
                            if (ServiceUtil.isError(createPayApplResult)) {
                                return UtilMessage.createAndLogServiceError(createPayApplResult, "AccountingErrorCreatingInvoiceFromOrder", locale, MODULE);
                            }
                        }
                    }
                }
            }

            // Should all be in place now. Depending on the ProductStore.autoApproveInvoice setting, set status to INVOICE_READY (unless it's a purchase
            //  invoice, which we set to INVOICE_IN_PROCESS)
            boolean autoApproveInvoice = UtilValidate.isEmpty(productStore.get("autoApproveInvoice")) || "Y".equals(productStore.getString("autoApproveInvoice"));
            if (autoApproveInvoice) {
                String nextStatusId = "INVOICE_READY";
                if (invoiceType.equals("PURCHASE_INVOICE")) {
                    nextStatusId = "INVOICE_IN_PROCESS";
                }
                Map setInvoiceStatusResult = dispatcher.runSync("setInvoiceStatus", UtilMisc.toMap("invoiceId", invoiceId, "statusId", nextStatusId, "userLogin", userLogin));
                if (ServiceUtil.isError(setInvoiceStatusResult)) {
                    return UtilMessage.createAndLogServiceError(setInvoiceStatusResult, "AccountingErrorCreatingInvoiceFromOrder", locale, MODULE);
                }
            }

            // check to see if we are all paid up
            Map checkResp = dispatcher.runSync("checkInvoicePaymentApplications", UtilMisc.toMap("invoiceId", invoiceId, "userLogin", userLogin));
            if (ServiceUtil.isError(checkResp)) {
                return UtilMessage.createAndLogServiceError(checkResp, "AccountingErrorCreatingInvoiceFromOrderCheckPaymentAppl", locale, MODULE);
            }

            Map resp = ServiceUtil.returnSuccess();
            resp.put("invoiceId", invoiceId);
            resp.put("invoiceTypeId", invoiceType);
            return resp;
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError("AccountingServiceOtherProblemCreatingInvoiceFromOrderItems", UtilMisc.toMap("reason", e.toString()), locale, MODULE);
        }
    }

    /* Creates InvoiceTerm entries for a list of terms, which can be BillingAccountTerms, OrderTerms, etc. Required by createInvoiceForOrder */
    @SuppressWarnings("unchecked")
    private static void createInvoiceTerms(Delegator delegator, LocalDispatcher dispatcher, String invoiceId, List<GenericValue> terms, GenericValue userLogin, Locale locale) {
        if ((terms != null) && (terms.size() > 0)) {
            for (GenericValue term : terms) {
                Map createInvoiceTermContext = FastMap.newInstance();
                createInvoiceTermContext.put("invoiceId", invoiceId);
                createInvoiceTermContext.put("invoiceItemSeqId", "_NA_");
                createInvoiceTermContext.put("termTypeId", term.get("termTypeId"));
                createInvoiceTermContext.put("termValue", term.get("termValue"));
                createInvoiceTermContext.put("termDays", term.get("termDays"));
                if (!"BillingAccountTerm".equals(term.getEntityName())) {
                    createInvoiceTermContext.put("textValue", term.get("textValue"));
                    createInvoiceTermContext.put("description", term.get("description"));
                }
                createInvoiceTermContext.put("uomId", term.get("uomId"));
                createInvoiceTermContext.put("userLogin", userLogin);

                Map createInvoiceTermResult = null;
                try {
                    createInvoiceTermResult = dispatcher.runSync("createInvoiceTerm", createInvoiceTermContext);
                } catch (GenericServiceException e) {
                    UtilMessage.createAndLogServiceError(e, "AccountingServiceErrorCreatingInvoiceTermFromOrder", locale, MODULE);
                }
                if (ServiceUtil.isError(createInvoiceTermResult)) {
                    UtilMessage.createAndLogServiceError(createInvoiceTermResult, "AccountingServiceErrorCreatingInvoiceTermFromOrder", locale, MODULE);
                }
            }
        }
    }

    /* Required by createInvoiceForOrder */
    @SuppressWarnings("unchecked")
    private static BigDecimal calcHeaderAdj(Delegator delegator, GenericValue adj, Map accountingTags, String invoiceTypeId, String invoiceId, String invoiceItemSeqId,
            BigDecimal divisor, BigDecimal multiplier, BigDecimal baseAmount, int decimals, int rounding, GenericValue userLogin, LocalDispatcher dispatcher, Locale locale) {
        BigDecimal adjAmount = ZERO;
        if (adj.get("amount") != null) {

            // pro-rate the amount
            BigDecimal amount = ZERO;
            // make sure the divisor is not 0 to avoid NaN problems; just leave the amount as 0 and skip it in essense
            if (divisor.signum() != 0) {
                // multiply first then divide to avoid rounding errors
                amount = baseAmount.multiply(multiplier).divide(divisor, decimals, rounding);
            }
            if (amount.signum() != 0) {
                Map createInvoiceItemContext = FastMap.newInstance();
                createInvoiceItemContext.put("invoiceId", invoiceId);
                createInvoiceItemContext.put("invoiceItemSeqId", invoiceItemSeqId);
                createInvoiceItemContext.put("invoiceItemTypeId", getInvoiceItemType(delegator, adj.getString("orderAdjustmentTypeId"), null, invoiceTypeId, "INVOICE_ADJ"));
                createInvoiceItemContext.put("description", adj.get("description"));
                createInvoiceItemContext.put("quantity", BigDecimal.ONE);
                createInvoiceItemContext.put("amount", amount);
                createInvoiceItemContext.put("overrideGlAccountId", adj.get("overrideGlAccountId"));
                //createInvoiceItemContext.put("productId", orderItem.get("productId"));
                //createInvoiceItemContext.put("productFeatureId", orderItem.get("productFeatureId"));
                //createInvoiceItemContext.put("uomId", "");
                //createInvoiceItemContext.put("taxableFlag", product.get("taxable"));
                createInvoiceItemContext.put("taxAuthPartyId", adj.get("taxAuthPartyId"));
                createInvoiceItemContext.put("taxAuthGeoId", adj.get("taxAuthGeoId"));
                createInvoiceItemContext.put("taxAuthorityRateSeqId", adj.get("taxAuthorityRateSeqId"));
                createInvoiceItemContext.put("userLogin", userLogin);
                // use the given tags
                UtilAccountingTags.putAllAccountingTags(accountingTags, createInvoiceItemContext);

                Map createInvoiceItemResult = null;
                try {
                    createInvoiceItemResult = dispatcher.runSync("createInvoiceItem", createInvoiceItemContext);
                } catch (GenericServiceException e) {
                    UtilMessage.createAndLogServiceError(e, "AccountingServiceErrorCreatingInvoiceItemFromOrder", locale, MODULE);
                }
                if (ServiceUtil.isError(createInvoiceItemResult)) {
                    UtilMessage.createAndLogServiceError(createInvoiceItemResult, "AccountingErrorCreatingInvoiceItemFromOrder", locale, MODULE);
                }

                // Create the OrderAdjustmentBilling record
                Map createOrderAdjustmentBillingContext = FastMap.newInstance();
                createOrderAdjustmentBillingContext.put("orderAdjustmentId", adj.getString("orderAdjustmentId"));
                createOrderAdjustmentBillingContext.put("invoiceId", invoiceId);
                createOrderAdjustmentBillingContext.put("invoiceItemSeqId", invoiceItemSeqId);
                createOrderAdjustmentBillingContext.put("amount", amount);
                createOrderAdjustmentBillingContext.put("userLogin", userLogin);

                try {
                    dispatcher.runSync("createOrderAdjustmentBilling", createOrderAdjustmentBillingContext);
                } catch (GenericServiceException e) {
                    UtilMessage.createAndLogServiceError("AccountingErrorCreatingOrderAdjustmentBillingFromOrder", createOrderAdjustmentBillingContext, locale, MODULE);
                }

            }
            amount = amount.setScale(decimals, rounding);
            adjAmount = amount;
        } else if (adj.get("sourcePercentage") != null) {
            // pro-rate the amount
            BigDecimal percent = adj.getBigDecimal("sourcePercentage");
            percent = percent.divide(new BigDecimal(100), 100, rounding);
            BigDecimal amount = ZERO;
            // make sure the divisor is not 0 to avoid NaN problems; just leave the amount as 0 and skip it in essense
            if (divisor.signum() != 0) {
                // multiply first then divide to avoid rounding errors
                amount = percent.multiply(divisor);
            }
            if (amount.signum() != 0) {
                Map createInvoiceItemContext = FastMap.newInstance();
                createInvoiceItemContext.put("invoiceId", invoiceId);
                createInvoiceItemContext.put("invoiceItemSeqId", invoiceItemSeqId);
                createInvoiceItemContext.put("invoiceItemTypeId", getInvoiceItemType(delegator, adj.getString("orderAdjustmentTypeId"), null, invoiceTypeId, "INVOICE_ADJ"));
                createInvoiceItemContext.put("description", adj.get("description"));
                createInvoiceItemContext.put("quantity", BigDecimal.ONE);
                createInvoiceItemContext.put("amount", amount);
                createInvoiceItemContext.put("overrideGlAccountId", adj.get("overrideGlAccountId"));
                //createInvoiceItemContext.put("productId", orderItem.get("productId"));
                //createInvoiceItemContext.put("productFeatureId", orderItem.get("productFeatureId"));
                //createInvoiceItemContext.put("uomId", "");
                //createInvoiceItemContext.put("taxableFlag", product.get("taxable"));
                createInvoiceItemContext.put("taxAuthPartyId", adj.get("taxAuthPartyId"));
                createInvoiceItemContext.put("taxAuthGeoId", adj.get("taxAuthGeoId"));
                createInvoiceItemContext.put("taxAuthorityRateSeqId", adj.get("taxAuthorityRateSeqId"));
                createInvoiceItemContext.put("userLogin", userLogin);

                Map createInvoiceItemResult = null;
                try {
                    createInvoiceItemResult = dispatcher.runSync("createInvoiceItem", createInvoiceItemContext);
                } catch (GenericServiceException e) {
                    UtilMessage.createAndLogServiceError(e, "AccountingServiceErrorCreatingInvoiceItemFromOrder", locale, MODULE);
                }
                if (ServiceUtil.isError(createInvoiceItemResult)) {
                    UtilMessage.createAndLogServiceError(createInvoiceItemResult, "AccountingErrorCreatingInvoiceItemFromOrder", locale, MODULE);
                }

                // Create the OrderAdjustmentBilling record
                Map createOrderAdjustmentBillingContext = FastMap.newInstance();
                createOrderAdjustmentBillingContext.put("orderAdjustmentId", adj.getString("orderAdjustmentId"));
                createOrderAdjustmentBillingContext.put("invoiceId", invoiceId);
                createOrderAdjustmentBillingContext.put("invoiceItemSeqId", invoiceItemSeqId);
                createOrderAdjustmentBillingContext.put("amount", amount);
                createOrderAdjustmentBillingContext.put("userLogin", userLogin);

                try {
                    dispatcher.runSync("createOrderAdjustmentBilling", createOrderAdjustmentBillingContext);
                } catch (GenericServiceException e) {
                    UtilMessage.createAndLogServiceError(createInvoiceItemResult, "AccountingErrorCreatingInvoiceItemFromOrder", locale, MODULE);
                }

            }
            amount = amount.setScale(decimals, rounding);
            adjAmount = amount;
        }

        Debug.logInfo("adjAmount: " + adjAmount + ", divisor: " + divisor + ", multiplier: " + multiplier + ", invoiceTypeId: " + invoiceTypeId + ", invoiceId: " + invoiceId + ", itemSeqId: " + invoiceItemSeqId + ", decimals: " + decimals + ", rounding: " + rounding + ", adj: " + adj, MODULE);
        return adjAmount;
    }

    /* Required by createInvoiceForOrder */
    private static String getInvoiceItemType(Delegator delegator, String key1, String key2, String invoiceTypeId, String defaultValue) {
        GenericValue itemMap = null;
        try {
            if (UtilValidate.isNotEmpty(key1)) {
                itemMap = delegator.findByPrimaryKeyCache("InvoiceItemTypeMap", UtilMisc.toMap("invoiceItemMapKey", key1, "invoiceTypeId", invoiceTypeId));
            }
            if (itemMap == null && UtilValidate.isNotEmpty(key2)) {
                itemMap = delegator.findByPrimaryKeyCache("InvoiceItemTypeMap", UtilMisc.toMap("invoiceItemMapKey", key2, "invoiceTypeId", invoiceTypeId));
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Trouble getting InvoiceItemTypeMap entity record", MODULE);
            return defaultValue;
        }
        if (itemMap != null) {
            return itemMap.getString("invoiceItemTypeId");
        } else {
            return defaultValue;
        }
    }

}
