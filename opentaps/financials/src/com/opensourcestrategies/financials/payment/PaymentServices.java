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
/* Portions of this from Apache OFBiz file(s) and have been modified by Open Source Strategies, Inc */
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

package com.opensourcestrategies.financials.payment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.accounting.invoice.InvoiceWorker;
import org.ofbiz.accounting.payment.PaymentWorker;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.base.util.UtilObject;
import org.ofbiz.base.util.UtilProperties;
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
import org.opentaps.common.domain.organization.OrganizationRepository;
import org.opentaps.common.party.PartyNotFoundException;
import org.opentaps.common.party.PartyReader;
import org.opentaps.common.util.UtilAccountingTags;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.domain.billing.invoice.Invoice;
import org.opentaps.domain.billing.invoice.InvoiceRepositoryInterface;
import org.opentaps.domain.billing.payment.Payment;
import org.opentaps.domain.billing.payment.PaymentRepositoryInterface;
import org.opentaps.domain.organization.AccountingTagConfigurationForOrganizationAndUsage;
import org.opentaps.domain.organization.Organization;
import org.opentaps.domain.organization.OrganizationRepositoryInterface;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.exception.FoundationException;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.repository.RepositoryException;

/**
 * PaymentServices - Services for creating and updating payments.
 *
 * @author     <a href="mailto:cliberty@opensourcestrategies.com">Chris Liberty</a>
 * @version    $Rev$
 */

public final class PaymentServices {

    private PaymentServices() { }

    private static final String MODULE = PaymentServices.class.getName();
    private static final int DECIMALS = UtilNumber.getBigDecimalScale("invoice.decimals");
    private static final int ROUNDING = UtilNumber.getBigDecimalRoundingMode("invoice.ROUNDING");
    public static final String ACCOUNTING_RESOURCE = "AccountingUiLabels";
    public static final int TAX_DECIMALS = UtilNumber.getBigDecimalScale("salestax.calc.decimals");
    public static final int TAX_ROUNDING = UtilNumber.getBigDecimalRoundingMode("salestax.rounding");
    /**
     * Wrapper service for the OFBiz createPayment service, to ensure that the paymentMethodTypeId is passed correctly.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> createPayment(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String paymentType = (String) context.get("paymentType");
        String partyIdFrom = (String) context.get("partyIdFrom");
        String partyIdTo = (String) context.get("partyIdTo");
        Locale locale = UtilCommon.getLocale(context);
        Security security = dctx.getSecurity();

        if (!(security.hasEntityPermission("FINANCIALS", "_AP_PCRTE", userLogin) || security.hasEntityPermission("FINANCIALS", "_AR_PCRTE", userLogin))) {
            return ServiceUtil.returnError(UtilProperties.getMessage("FinancialsUiLabels", "FinancialsServiceErrorNoPermission", locale));
        }

        Map result = ServiceUtil.returnSuccess();

        try {
            if (UtilValidate.isEmpty(paymentType) && UtilValidate.isNotEmpty(context.get("paymentTypeId"))) {
            String paymentTypeId = (String) context.get("paymentTypeId");
            GenericValue pType = delegator.findByPrimaryKey("PaymentType", UtilMisc.toMap("paymentTypeId", paymentTypeId));
            GenericValue rootPaymentType = getRootPaymentType(pType);
            paymentType = rootPaymentType.getString("paymentTypeId");
        }
        // load organization by domain interface
        DomainsLoader domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(userLogin));
        DomainsDirectory domains = domainLoader.loadDomainsDirectory();
        OrganizationRepositoryInterface ori =  domains.getOrganizationDomain().getOrganizationRepository();
        String organizationPartyId = paymentType.equals("DISBURSEMENT") ? partyIdFrom : partyIdTo;
        Organization organization = ori.getOrganizationById(organizationPartyId);
        // if return require field error, then throw a service error
        if (!organization.allocatePaymentTagsToApplications()) {
            List<AccountingTagConfigurationForOrganizationAndUsage> missings = validateTagParameters(dctx, context);
            if (!missings.isEmpty()) {
                return UtilMessage.createAndLogServiceError("OpentapsError_ServiceErrorRequiredTagNotFound", UtilMisc.toMap("tagName", missings.get(0).getDescription()), locale, MODULE);
            }
        }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (RepositoryException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (EntityNotFoundException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        try {
            // a convoluted way to check the parties exist
            try {
                new PartyReader(partyIdFrom, delegator);
            } catch (PartyNotFoundException e) {
                return UtilMessage.createAndLogServiceError("OpentapsError_PartyNotFound", UtilMisc.toMap("partyId", partyIdFrom), locale, MODULE);
            }
            try {
                new PartyReader(partyIdTo, delegator);
            } catch (PartyNotFoundException e) {
                return UtilMessage.createAndLogServiceError("OpentapsError_PartyNotFound", UtilMisc.toMap("partyId", partyIdTo), locale, MODULE);
            }

            // Override the supplied paymentMethodTypeId with the paymentMethodTypeId of the supplied PaymentMethod, if necessary
            Map resolvePaymentMethodTypeIdResult = resolvePaymentMethodTypeId(dctx, context);
            if (ServiceUtil.isError(resolvePaymentMethodTypeIdResult)) {
                return resolvePaymentMethodTypeIdResult;
            } else {
                context.put("paymentMethodTypeId", resolvePaymentMethodTypeIdResult.get("paymentMethodTypeId"));
            }

            // Call the OFBiz createPayment service
            Map createPaymentResult = dispatcher.runSync("createPayment", context);
            if (ServiceUtil.isError(createPaymentResult)) {
                return createPaymentResult;
            }

            result.put("paymentId", createPaymentResult.get("paymentId"));

        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        return result;
    }

    /**
     * Validate the required accounting tags are given as parameters.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>String</code> value
     * @throws GenericEntityException an exception occurs
     * @throws RepositoryException an exception occurs
     */
    private static List<AccountingTagConfigurationForOrganizationAndUsage> validateTagParameters(DispatchContext dctx, Map<String, Object> context) throws GenericEntityException, RepositoryException {
        Delegator delegator = dctx.getDelegator();
        String paymentTypeId = (String) context.get("paymentTypeId");
        String partyIdFrom = (String) context.get("partyIdFrom");
        String partyIdTo = (String) context.get("partyIdTo");
        if (paymentTypeId == null) {
            // if not exist paymentTypeId parameter, then get paymentTypeId/partyIdFrom/partyIdTo by GenericValue payment
            String paymentId = (String) context.get("paymentId");
            if (paymentId != null) {
                GenericValue payment = delegator.findByPrimaryKey("Payment", UtilMisc.toMap("paymentId", paymentId));
                paymentTypeId = payment.getString("paymentTypeId");
                partyIdFrom = payment.getString("partyIdFrom");
                partyIdTo = payment.getString("partyIdTo");
            }
        }
        if (paymentTypeId != null) {
            //get parentTypeId of PaymentType
            GenericValue paymentType = delegator.findByPrimaryKey("PaymentType", UtilMisc.toMap("paymentTypeId", paymentTypeId));
            GenericValue rootPaymentType = getRootPaymentType(paymentType);
            String accountingTagUsageTypeId = UtilObject.equalsHelper(rootPaymentType.getString("paymentTypeId"), "DISBURSEMENT") ? UtilAccountingTags.DISBURSEMENT_PAYMENT_TAG : UtilAccountingTags.RECEIPT_PAYMENT_TAG;
            String organizationPartyId = UtilObject.equalsHelper(paymentType.getString("parentTypeId"), "DISBURSEMENT") ?  partyIdFrom : partyIdTo;
            Debug.logInfo("paymentTypeId : " + paymentTypeId + ", organizationPartyId : " + organizationPartyId, MODULE);
            // validate the required div
            Map<String, String> tags = new HashMap<String, String>();
            for (int i = 1; i <= UtilAccountingTags.TAG_COUNT; i++) {
                tags.put(UtilAccountingTags.ENTITY_TAG_PREFIX + i, UtilCommon.getParameter(context, UtilAccountingTags.ENTITY_TAG_PREFIX + i));
            }

            OrganizationRepository repository = new OrganizationRepository(delegator);
            return repository.validateTagParameters(tags, organizationPartyId, accountingTagUsageTypeId, UtilAccountingTags.ENTITY_TAG_PREFIX);
         }
        return new ArrayList<AccountingTagConfigurationForOrganizationAndUsage>();
    }


    /**
     * Get the root PaymentType of the PaymentType.
     * @param paymentType a <code>GenericValue</code> value
     * @return a <code>GenericValue</code> value
     * @throws GenericEntityException if error occur
     */
    private static GenericValue getRootPaymentType(GenericValue paymentType) throws GenericEntityException {
      //get parentTypeId of PaymentType
        if (UtilValidate.isEmpty(paymentType.getString("parentTypeId"))) {
            return paymentType;
        } else {
            GenericValue parentPaymentType = paymentType.getRelatedOneCache("ParentPaymentType");
            return getRootPaymentType(parentPaymentType);
        }
    }

    /**
     * Limitied update service for Sent or Received payments.
     * Only the Comments or Reference number may be changed at this point.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    public static Map<String, Object> updateSentOrReceivedPayment(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        Security security = dctx.getSecurity();

        String paymentId = (String) context.get("paymentId");
        String comments = (String) context.get("comments");
        String paymentRefNum = (String) context.get("paymentRefNum");

        if (!(security.hasEntityPermission("FINANCIALS", "_AP_PUPDT", userLogin) || security.hasEntityPermission("FINANCIALS", "_AR_PUPDT", userLogin))) {
            return ServiceUtil.returnError(UtilProperties.getMessage("FinancialsUiLabels", "FinancialsServiceErrorNoPermission", locale));
        }

        try {
            DomainsLoader domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(userLogin));
            DomainsDirectory domains = domainLoader.loadDomainsDirectory();
            PaymentRepositoryInterface repository =  domains.getBillingDomain().getPaymentRepository();
            Payment payment = repository.getPaymentById(paymentId);

            payment.setComments(comments);
            payment.setPaymentRefNum(paymentRefNum);
            repository.update(payment);

        } catch (EntityNotFoundException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (RepositoryException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("paymentId", paymentId);
        return result;
    }

    /**
     * Wrapper service for the OFBiz updatePayment service, to ensure that the paymentMethodTypeId is passed correctly.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map updatePayment(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        Security security = dctx.getSecurity();
        String paymentType = (String) context.get("paymentType");
        String partyIdFrom = (String) context.get("partyIdFrom");
        String partyIdTo = (String) context.get("partyIdTo");

        if (!(security.hasEntityPermission("FINANCIALS", "_AP_PUPDT", userLogin) || security.hasEntityPermission("FINANCIALS", "_AR_PUPDT", userLogin))) {
            return ServiceUtil.returnError(UtilProperties.getMessage("FinancialsUiLabels", "FinancialsServiceErrorNoPermission", locale));
        }


        Map result = ServiceUtil.returnSuccess();

        try {
            if (UtilValidate.isEmpty(paymentType) && UtilValidate.isNotEmpty(context.get("paymentTypeId"))) {
                String paymentTypeId = (String) context.get("paymentTypeId");
                GenericValue pType = delegator.findByPrimaryKey("PaymentType", UtilMisc.toMap("paymentTypeId", paymentTypeId));
                GenericValue rootPaymentType = getRootPaymentType(pType);
                paymentType = rootPaymentType.getString("paymentTypeId");
            }
            DomainsLoader domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(userLogin));
            DomainsDirectory domains = domainLoader.loadDomainsDirectory();
            OrganizationRepositoryInterface ori =  domains.getOrganizationDomain().getOrganizationRepository();
            String organizationPartyId = paymentType.equals("DISBURSEMENT") ? partyIdFrom : partyIdTo;
            Organization organization = ori.getOrganizationById(organizationPartyId);
            // if return require field error, then throw a service error
            if (!organization.allocatePaymentTagsToApplications()) {
                List<AccountingTagConfigurationForOrganizationAndUsage> missings = validateTagParameters(dctx, context);
                if (!missings.isEmpty()) {
                    return UtilMessage.createAndLogServiceError("OpentapsError_ServiceErrorRequiredTagNotFound", UtilMisc.toMap("tagName", missings.get(0).getDescription()), locale, MODULE);
                }
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (RepositoryException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (EntityNotFoundException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        try {

            // Override the supplied paymentMethodTypeId with the paymentMethodTypeId of the supplied PaymentMethod, if necessary
            Map resolvePaymentMethodTypeIdResult = resolvePaymentMethodTypeId(dctx, context);
            if (ServiceUtil.isError(resolvePaymentMethodTypeIdResult)) {
                return resolvePaymentMethodTypeIdResult;
            } else {
                context.put("paymentMethodTypeId", resolvePaymentMethodTypeIdResult.get("paymentMethodTypeId"));
            }

            // Call the OFBiz updatePayment service
            Map updatePaymentResult = dispatcher.runSync("updatePayment", context);
            if (ServiceUtil.isError(updatePaymentResult)) {
                return updatePaymentResult;
            }

            result.put("paymentId", context.get("paymentId"));

        } catch (GenericServiceException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        return result;
    }

    /**
     * Helper method to return the correct paymentMethodTypeId, if given a paymentMethodId (or not).
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> resolvePaymentMethodTypeId(DispatchContext dctx, Map<String, Object> context) {

        String paymentMethodId = (String) context.get("paymentMethodId");
        String paymentMethodTypeId = (String) context.get("paymentMethodTypeId");
        Delegator delegator = dctx.getDelegator();
        Locale locale = UtilCommon.getLocale(context);
        Map result = ServiceUtil.returnSuccess();

        try {

            // If the payment method is supplied, make sure it exists
            if (UtilValidate.isNotEmpty(paymentMethodId)) {
                GenericValue paymentMethod = delegator.findByPrimaryKeyCache("PaymentMethod", UtilMisc.toMap("paymentMethodId", paymentMethodId));
                if (paymentMethod == null) {
                    return UtilMessage.createAndLogServiceError("FinancialsServiceErrorPaymentMethodNotFound", UtilMisc.toMap("paymentMethodId", paymentMethodId), locale, MODULE);
                } else if (paymentMethod.get("paymentMethodTypeId") == null) {
                    return UtilMessage.createAndLogServiceError("FinancialsServiceErrorPaymentMethodTypeNotFound", UtilMisc.toMap("paymentMethodId", paymentMethodId), locale, MODULE);
                }

                // If so, return the paymentMethodTypeId of the supplied PaymentMethod
                paymentMethodTypeId = paymentMethod.getString("paymentMethodTypeId");
            }

            result.put("paymentMethodTypeId", paymentMethodTypeId);

        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        return result;
    }

    /**
     * Create a payment application.
     *  It will check that the amount applied is positive and not greater than the payment outstanding amount.
     *  If applied to an Invoice, it will also check that the  amount applied is not greater than the invoice outstanding amount.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     * @exception GenericServiceException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> createPaymentApplication(DispatchContext dctx, Map<String, Object> context) throws GenericServiceException {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = UtilCommon.getLocale(context);
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        // get parameters
        String paymentId = (String) context.get("paymentId");
        BigDecimal amountApplied = (BigDecimal) context.get("amountApplied");
        if (amountApplied == null) {
            amountApplied = BigDecimal.ZERO;
        }

        // optional parameters
        String invoiceId = (String) context.get("invoiceId");
        String billingAccountId = (String) context.get("billingAccountId");
        String overrideGlAccountId = (String) context.get("overrideGlAccountId");
        String taxAuthGeoId = (String) context.get("taxAuthGeoId");
        Boolean checkForOverApplication = (Boolean) context.get("checkForOverApplication");
        if (checkForOverApplication == null) {
            checkForOverApplication = false;
        }

        // check that at least on parameter was given to determine what to apply the payment to
        if (UtilValidate.isEmpty(invoiceId)
            && UtilValidate.isEmpty(billingAccountId)
            && UtilValidate.isEmpty(taxAuthGeoId)
            && UtilValidate.isEmpty(overrideGlAccountId)) {
            return UtilMessage.createAndLogServiceError("AccountingPaymentApplicationParameterMissing", locale, MODULE);
        }

        // validate that the amountApplied is > 0
        if (amountApplied.signum() <= 0) {
            return UtilMessage.createAndLogServiceError("OpentapsError_PaymentApplicationMustBePositive", locale, MODULE);
        }


        // get Payment
        GenericValue payment;
        Organization organization;
        try {
            payment = delegator.findByPrimaryKeyCache("Payment", UtilMisc.toMap("paymentId", paymentId));
            DomainsLoader domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(userLogin));
            DomainsDirectory domains = domainLoader.loadDomainsDirectory();
            OrganizationRepositoryInterface ori =  domains.getOrganizationDomain().getOrganizationRepository();
            String paymentTypeId = payment.getString("paymentTypeId");
            GenericValue paymentType = delegator.findByPrimaryKey("PaymentType", UtilMisc.toMap("paymentTypeId", paymentTypeId));
            GenericValue rootPaymentType = getRootPaymentType(paymentType);
            String organizationPartyId = rootPaymentType.getString("paymentTypeId").equals("DISBURSEMENT") ? payment.getString("partyIdFrom") : payment.getString("partyIdTo");
            organization = ori.getOrganizationById(organizationPartyId);
            // if return require field error, then throw a service error
            if (organization.allocatePaymentTagsToApplications()) {
                List<AccountingTagConfigurationForOrganizationAndUsage> missings = validateTagParameters(dctx, context);
                if (!missings.isEmpty()) {
                    return UtilMessage.createAndLogServiceError("OpentapsError_ServiceErrorRequiredTagNotFound", UtilMisc.toMap("tagName", missings.get(0).getDescription()), locale, MODULE);
                }
            }
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        } catch (RepositoryException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        } catch (EntityNotFoundException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        }
        if (paymentId == null) {
            return UtilMessage.createAndLogServiceError("OpentapsError_PaymentNotFound", UtilMisc.toMap("paymentId", paymentId), locale, MODULE);
        }

        if (UtilValidate.isEmpty(billingAccountId)) {
            // get amount not yet applied from that payment
            BigDecimal notAppliedPayment = PaymentWorker.getPaymentNotApplied(payment);
            // validate that amountApplied <= notAppliedPayment
            if (amountApplied.compareTo(notAppliedPayment) > 0) {
                return UtilMessage.createAndLogServiceError("FinancialsError_PaymentApplicationExceedPaymentRemainingAmount", UtilMisc.toMap("paymentId", paymentId, "amountApplied", amountApplied, "notAppliedPayment", notAppliedPayment), locale, MODULE);
            }
        } else {
            // when applying to billing accounts, only check against the total payment amount
            BigDecimal paymentAmount = payment.getBigDecimal("amount");
            if (amountApplied.compareTo(paymentAmount) > 0) {
                return UtilMessage.createAndLogServiceError("FinancialsError_PaymentApplicationExceedPaymentTotalAmount", UtilMisc.toMap("paymentId", paymentId, "amountApplied", amountApplied, "paymentAmount", paymentAmount), locale, MODULE);
            }
        }

        // get Invoice if invoiceId given
        if (UtilValidate.isNotEmpty(invoiceId)) {
            try {
                InvoiceRepositoryInterface invoiceRepository = new DomainsLoader(new Infrastructure(dispatcher), new User(userLogin)).loadDomainsDirectory().getBillingDomain().getInvoiceRepository();
                Invoice invoice = invoiceRepository.getInvoiceById(invoiceId);
                // get amount not yet applied from that invoice, including those payments which are not yet received or sent (ie, not paid)
                // this is so we prevent several applications of unreceived payments from exceeding the total invoice value
                Debug.logInfo("** checking amount to apply: " + amountApplied + " against pending open amount for invoice: " + invoice.getPendingOpenAmount(), MODULE);
                if (invoice.getPendingOpenAmount().compareTo(amountApplied) < 0) {
                    if (checkForOverApplication) {
                        return UtilMessage.createAndLogServiceError("FinancialsError_PaymentApplicationExceedInvoiceRemainingAmount", UtilMisc.toMap("invoiceId", invoiceId, "amountApplied", amountApplied, "notAppliedInvoice", invoice.getOpenAmount()), locale, MODULE);
                    } else {
                        amountApplied = invoice.getOpenAmount();
                    }
                }

                // set PaymentApplication billingAccountId from the invoice billingAccountId if one is set
                String invoiceBillingAccountId = invoice.getString("billingAccountId");
                if (UtilValidate.isNotEmpty(invoiceBillingAccountId)) {
                    billingAccountId = invoiceBillingAccountId;
                }

            } catch (EntityNotFoundException e) {
                return UtilMessage.createAndLogServiceError("FinancialsError_InvoiceNotFound", UtilMisc.toMap("invoiceId", invoiceId), locale, MODULE);
            } catch (FoundationException e) {
                return UtilMessage.createAndLogServiceError(e, locale, MODULE);
            }
        }

        // make PaymentApplication (not persisted yet)
        GenericValue paymentApplication = delegator.makeValidValue("PaymentApplication", context);
        // make pk
        String paymentApplicationId = delegator.getNextSeqId("PaymentApplication");
        paymentApplication.put("paymentApplicationId", paymentApplicationId);
        // put the updated amountApplied
        paymentApplication.put("amountApplied", amountApplied);
        // persist
        try {
            if (organization.allocatePaymentTagsToApplications()) {
                UtilAccountingTags.putAllAccountingTags(context, paymentApplication);
            }
            paymentApplication.create();
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        } catch (RepositoryException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        }

        // if the payment has status SENT or RECEIVED, then we also call checkPaymentInvoices
        String paymentStatusId = payment.getString("statusId");
        if ("PMNT_SENT".equals(paymentStatusId) || "PMNT_RECEIVED".equals(paymentStatusId)) {
            Map checkPaymentInvoicesResult = dispatcher.runSync("checkPaymentInvoices", UtilMisc.toMap("paymentId", paymentId, "userLogin", userLogin));
            if (ServiceUtil.isError(checkPaymentInvoicesResult)) {
                return checkPaymentInvoicesResult;
            }
        }

        Map result = ServiceUtil.returnSuccess();
        result.put("paymentApplicationId", paymentApplicationId);
        return result;
    }


    /**
     * Wrapper to the Ofbiz updatePaymentApplication service.
     *  It will set the Invoice status from PAID to READY if the payment application applied amount is changed.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     * @exception GenericServiceException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> updatePaymentApplication(DispatchContext dctx, Map<String, Object> context) throws GenericServiceException {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        String invoiceId = (String) context.get("invoiceId");
        String paymentApplicationId = (String) context.get("paymentApplicationId");
        BigDecimal amountApplied = (BigDecimal) context.get("amountApplied");
        String paymentId = (String) context.get("paymentId");

        // get Payment
        GenericValue payment;
        Organization organization;
        try {
            payment = delegator.findByPrimaryKeyCache("Payment", UtilMisc.toMap("paymentId", paymentId));
            DomainsLoader domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(userLogin));
            DomainsDirectory domains = domainLoader.loadDomainsDirectory();
            OrganizationRepositoryInterface ori =  domains.getOrganizationDomain().getOrganizationRepository();
            String paymentTypeId = payment.getString("paymentTypeId");
            GenericValue paymentType = delegator.findByPrimaryKeyCache("PaymentType", UtilMisc.toMap("paymentTypeId", paymentTypeId));
            GenericValue rootPaymentType = getRootPaymentType(paymentType);
            String organizationPartyId = rootPaymentType.getString("paymentTypeId").equals("DISBURSEMENT") ? payment.getString("partyIdFrom") : payment.getString("partyIdTo");
            organization = ori.getOrganizationById(organizationPartyId);
            // if return require field error, then throw a service error
            if (organization.allocatePaymentTagsToApplications()) {
                List<AccountingTagConfigurationForOrganizationAndUsage> missings = validateTagParameters(dctx, context);
                if (!missings.isEmpty()) {
                    return UtilMessage.createAndLogServiceError("OpentapsError_ServiceErrorRequiredTagNotFound", UtilMisc.toMap("tagName", missings.get(0).getDescription()), locale, MODULE);
                }
            }
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        } catch (RepositoryException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        } catch (EntityNotFoundException e) {
            return UtilMessage.createAndLogServiceError(e, locale, MODULE);
        }


        // check if we have an invoiceId
        if (UtilValidate.isNotEmpty(invoiceId)) {
            try {
                // get the PaymentApplication to update
                GenericValue paymentApplication = delegator.findByPrimaryKey("PaymentApplication", UtilMisc.toMap("paymentApplicationId", paymentApplicationId));
                if (paymentApplication != null) {
                    // check if the amountApplied has changed
                    Debug.logInfo("Updating PaymentApplication [" + paymentApplicationId + "] for Invoice [" + invoiceId + "] with amountApplied=" + amountApplied, MODULE);
                    if (amountApplied != paymentApplication.get("amountApplied")) {
                        GenericValue invoice = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", invoiceId));
                        if (invoice != null) {
                            // reset invoice status to READY
                            Map serviceResults = dispatcher.runSync("setInvoiceStatus", UtilMisc.toMap("invoiceId", invoiceId, "statusId", "INVOICE_READY", "userLogin", userLogin));
                            if (ServiceUtil.isError(serviceResults)) {
                                return serviceResults;
                            }
                        }
                    }
                }
            } catch (GenericEntityException e) {
                // do nothing here, the ofbiz service will handle this later
            }
        }

        // update PaymentApplication by context
        Map serviceResults = updatePaymentApplicationBd(dctx, context);
        if (ServiceUtil.isError(serviceResults)) {
            return serviceResults;
        }

        if (UtilValidate.isNotEmpty(invoiceId)) {
            // call the checkInvoicePaymentApplications service which will set the invoice to PAID if fully applied
            Map input = UtilMisc.toMap("userLogin", userLogin, "invoiceId", invoiceId);
            Map result = dispatcher.runSync("checkInvoicePaymentApplications", input);
            if (ServiceUtil.isError(result)) {
                return result;
            }
        }

        // return the result of updatePaymentApplication now
        return serviceResults;
    }

    /**
     * This method was copied from ofbiz InvoiceServices for add accounting tags support.
     * Service to add payment application records to indicate which invoices
     * have been paid/received. For invoice processing, this service works on
     * the invoice level when 'invoiceProcessing' parameter is set to "Y" else
     * it works on the invoice item level.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    public static Map<String, Object> updatePaymentApplicationDef(DispatchContext dctx, Map<String, Object> context) {
            Delegator delegator = dctx.getDelegator();
            Locale locale = (Locale) context.get("locale");

        if (DECIMALS == -1 || ROUNDING == -1) {
            return UtilMessage.createAndLogServiceError("AccountingAritmeticPropertiesNotConfigured", locale, MODULE);
        }

        if (!context.containsKey("useHighestAmount")) {
            context.put("useHighestAmount", "Y");
        }

        String defaultInvoiceProcessing = UtilProperties.getPropertyValue("AccountingConfig", "invoiceProcessing");

        boolean debug = true; // show processing messages in the log..or not....

        // a 'y' in invoiceProssesing wil reverse the default processing
        String changeProcessing = (String) context.get("invoiceProcessing");
        String invoiceId = (String) context.get("invoiceId");
        String invoiceItemSeqId = (String) context.get("invoiceItemSeqId");
        String paymentId = (String) context.get("paymentId");
        String toPaymentId = (String) context.get("toPaymentId");
        String paymentApplicationId = (String) context.get("paymentApplicationId");
        BigDecimal amountApplied = (BigDecimal) context.get("amountApplied");
        String billingAccountId = (String) context.get("billingAccountId");
        String taxAuthGeoId = (String) context.get("taxAuthGeoId");
        String useHighestAmount = (String) context.get("useHighestAmount");
        String note = (String) context.get("note");

        List<String> errorMessageList = new LinkedList<String>();

        if (debug) {
            Debug.logInfo("updatePaymentApplicationDefBd input parameters..."
                + " defaultInvoiceProcessing: " + defaultInvoiceProcessing
                + " changeDefaultInvoiceProcessing: " + changeProcessing
                + " useHighestAmount: " + useHighestAmount
                + " paymentApplicationId: " + paymentApplicationId
                + " PaymentId: " + paymentId
                + " InvoiceId: " + invoiceId
                + " InvoiceItemSeqId: " + invoiceItemSeqId
                + " BillingAccountId: " + billingAccountId
                + " toPaymentId: " + toPaymentId
                + " amountApplied: " + amountApplied
                + " note: " + note
                + " TaxAuthGeoId: " + taxAuthGeoId, MODULE);
        }

        if (changeProcessing == null) {
            changeProcessing = "N";    // not provided, so no change
        }

        boolean invoiceProcessing = true;
        if (defaultInvoiceProcessing.equals("YY")) {
            invoiceProcessing = true;
        } else if (defaultInvoiceProcessing.equals("NN")) {
            invoiceProcessing = false;
        } else if (defaultInvoiceProcessing.equals("Y")) {
            if (changeProcessing.equals("Y")) {
                invoiceProcessing = false;
            } else {
                invoiceProcessing = true;
            }
        } else if (defaultInvoiceProcessing.equals("N")) {
            if (changeProcessing.equals("Y")) {
                invoiceProcessing = true;
            } else {
                invoiceProcessing = false;
            }
        }

        // on a new paymentApplication check if only billing or invoice or tax
        // id is provided not 2,3... BUT a combination of billingAccountId and invoiceId is permitted - that's how you use a
        // Billing Account to pay for an Invoice
        if (paymentApplicationId == null) {
            int count = 0;
            if (invoiceId != null) {
                count++;
            }
            if (toPaymentId != null) {
                count++;
            }
            if (billingAccountId != null) {
                count++;
            }
            if (taxAuthGeoId != null) {
                count++;
            }
            if ((billingAccountId != null) && (invoiceId != null)) {
                count--;
            }
            if (count != 1) {
                errorMessageList.add(UtilMessage.expandLabel("AccountingSpecifyInvoiceToPaymentBillingAccountTaxGeoId", locale));
            }
        }

        // avoid null pointer exceptions.
        if (amountApplied == null) {
            amountApplied = BigDecimal.ZERO;
        }
        // makes no sense to have an item numer without an invoice number
        if (invoiceId == null) {
            invoiceItemSeqId = null;
        }

        // retrieve all information and perform checking on the retrieved info.....

        // Payment.....
        BigDecimal paymentApplyAvailable = BigDecimal.ZERO;
        // amount available on the payment reduced by the already applied amounts
        BigDecimal amountAppliedMax = BigDecimal.ZERO;
        // the maximum that can be applied taking payment,invoice,invoiceitem,billing account in concideration
        // if maxApplied is missing, this value can be used
        GenericValue payment = null;
        if (paymentId == null || paymentId.equals("")) {
            errorMessageList.add(UtilMessage.expandLabel("AccountingPaymentIdBlankNotSupplied", locale));
        } else {
            try {
                payment = delegator.findByPrimaryKey("Payment", UtilMisc.toMap("paymentId", paymentId));
            } catch (GenericEntityException e) {
                ServiceUtil.returnError(e.getMessage());
            }
            if (payment == null) {
                errorMessageList.add(UtilMessage.expandLabel("AccountingPaymentRecordNotFound", UtilMisc.toMap("paymentId", paymentId), locale));
            }
            paymentApplyAvailable = payment.getBigDecimal("amount").subtract(PaymentWorker.getPaymentApplied(payment)).setScale(DECIMALS, ROUNDING);

            if (payment.getString("statusId").equals("PMNT_CANCELLED")) {
                errorMessageList.add(UtilMessage.expandLabel("AccountingPaymentCancelled", UtilMisc.toMap("paymentId", paymentId), locale));
            }
            if (payment.getString("statusId").equals("PMNT_CONFIRMED")) {
                errorMessageList.add(UtilMessage.expandLabel("AccountingPaymentConfirmed", UtilMisc.toMap("paymentId", paymentId), locale));
            }

            // if the amount to apply is 0 give it amount the payment still need
            // to apply
            if (amountApplied.signum() == 0) {
                amountAppliedMax = paymentApplyAvailable;
            }

            if (paymentApplicationId == null) {
                // only check for new application records, update on existing records is checked in the paymentApplication section
                if (paymentApplyAvailable.signum() == 0) {
                    errorMessageList.add(UtilMessage.expandLabel("AccountingPaymentAlreadyApplied", UtilMisc.toMap("paymentId", paymentId), locale));
                } else {
                    // check here for too much application if a new record is
                    // added (paymentApplicationId == null)
                    if (amountApplied.compareTo(paymentApplyAvailable) > 0) {
                        errorMessageList.add(UtilMessage.expandLabel("AccountingPaymentLessRequested",
                                UtilMisc.toMap("paymentId", paymentId,
                                               "paymentApplyAvailable", paymentApplyAvailable,
                                               "amountApplied", amountApplied,
                                               "isoCode", payment.getString("currencyUomId")), locale));
                    }
                }
            }

            if (debug) {
                Debug.logInfo("Payment info retrieved and checked...", MODULE);
            }
        }

        // the "TO" Payment.....
        BigDecimal toPaymentApplyAvailable = BigDecimal.ZERO;
        GenericValue toPayment = null;
        if (toPaymentId != null && !toPaymentId.equals("")) {
            try {
                toPayment = delegator.findByPrimaryKey("Payment", UtilMisc.toMap("paymentId", toPaymentId));
            } catch (GenericEntityException e) {
                ServiceUtil.returnError(e.getMessage());
            }
            if (toPayment == null) {
                errorMessageList.add(UtilMessage.expandLabel("AccountingPaymentRecordNotFound", UtilMisc.toMap("paymentId", toPaymentId), locale));
            }
            toPaymentApplyAvailable = toPayment.getBigDecimal("amount").subtract(PaymentWorker.getPaymentApplied(toPayment)).setScale(DECIMALS, ROUNDING);

            if (toPayment.getString("statusId").equals("PMNT_CANCELLED")) {
                errorMessageList.add(UtilMessage.expandLabel("AccountingPaymentCancelled", UtilMisc.toMap("paymentId", paymentId), locale));
            }
            if (toPayment.getString("statusId").equals("PMNT_CONFIRMED")) {
                errorMessageList.add(UtilMessage.expandLabel("AccountingPaymentConfirmed", UtilMisc.toMap("paymentId", paymentId), locale));
            }

            // if the amount to apply is less then required by the payment reduce it
            if (amountAppliedMax.compareTo(toPaymentApplyAvailable) > 0) {
                amountAppliedMax = toPaymentApplyAvailable;
            }

            if (paymentApplicationId == null) {
                // only check for new application records, update on existing records is checked in the paymentApplication section
                if (toPaymentApplyAvailable.signum() == 0) {
                    errorMessageList.add(UtilMessage.expandLabel("AccountingPaymentAlreadyApplied", UtilMisc.toMap("paymentId", toPaymentId), locale));
                } else {
                    // check here for too much application if a new record is
                    // added (paymentApplicationId == null)
                    if (amountApplied.compareTo(toPaymentApplyAvailable) > 0) {
                            errorMessageList.add(UtilMessage.expandLabel("AccountingPaymentLessRequested",
                                    UtilMisc.toMap("paymentId", toPaymentId,
                                                   "paymentApplyAvailable", toPaymentApplyAvailable,
                                                   "amountApplied", amountApplied,
                                                   "isoCode", payment.getString("currencyUomId")), locale));
                    }
                }
            }

            // check if at least one send is the same as one receiver on the other payment
            if (!payment.getString("partyIdFrom").equals(toPayment.getString("partyIdTo")) && !payment.getString("partyIdTo").equals(toPayment.getString("partyIdFrom"))) {
                errorMessageList.add(UtilMessage.expandLabel("AccountingFromPartySameToParty", locale));
            }

            if (debug) {
                Debug.logInfo("toPayment info retrieved and checked...", MODULE);
            }
        }

        // assign payment to billing account if the invoice is assigned to this billing account
        if (invoiceId != null) {
            GenericValue invoice = null;
            try {
                invoice = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", invoiceId));
            } catch (GenericEntityException e) {
                ServiceUtil.returnError(e.getMessage());
            }

            if (invoice == null) {
                errorMessageList.add(UtilMessage.expandLabel("AccountingInvoiceNotFound", UtilMisc.toMap("invoiceId", invoiceId), locale));
            } else {
                if (invoice.getString("billingAccountId") != null) {
                    billingAccountId = invoice.getString("billingAccountId");
                }
            }
        }

        // billing account
        GenericValue billingAccount = null;
        if (billingAccountId != null && !billingAccountId.equals("")) {
            try {
                billingAccount = delegator.findByPrimaryKey("BillingAccount", UtilMisc.toMap("billingAccountId", billingAccountId));
            } catch (GenericEntityException e) {
                ServiceUtil.returnError(e.getMessage());
            }
            if (billingAccount == null) {
                errorMessageList.add(UtilMessage.expandLabel("AccountingBillingAccountNotFound", UtilMisc.toMap("billingAccountId", billingAccountId), locale));
            }
            // check the currency
            if (billingAccount.get("accountCurrencyUomId") != null && payment.get("currencyUomId") != null && !billingAccount.getString("accountCurrencyUomId").equals(payment.getString("currencyUomId"))) {
                errorMessageList.add(UtilMessage.expandLabel("AccountingBillingAccountCurrencyProblem",
                                                               UtilMisc.toMap("billingAccountId", billingAccountId,
                                                                              "accountCurrencyUomId", billingAccount.getString("accountCurrencyUomId"),
                                                                              "paymentId", paymentId,
                                                                              "paymentCurrencyUomId", payment.getString("currencyUomId")), locale));
            }

            if (debug) {
                Debug.logInfo("Billing Account info retrieved and checked...", MODULE);
            }
        }

        // get the invoice (item) information
        BigDecimal invoiceApplyAvailable = BigDecimal.ZERO;
        // amount available on the invoice reduced by the already applied amounts
        BigDecimal invoiceItemApplyAvailable = BigDecimal.ZERO;
        // amount available on the invoiceItem reduced by the already applied amounts
        GenericValue invoice = null;
        GenericValue invoiceItem = null;
        if (invoiceId != null) {
            try {
                invoice = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", invoiceId));
            } catch (GenericEntityException e) {
                ServiceUtil.returnError(e.getMessage());
            }

            if (invoice == null) {
                errorMessageList.add(UtilMessage.expandLabel("AccountingInvoiceNotFound", UtilMisc.toMap("invoiceId", invoiceId), locale));
            } else { // check the invoice and when supplied the invoice item...

                if (invoice.getString("statusId").equals("INVOICE_CANCELLED")) {
                    errorMessageList.add(UtilMessage.expandLabel("AccountingInvoiceCancelledCannotApplyTo", UtilMisc.toMap("invoiceId", invoiceId), locale));
                }

                // check if the invoice already covered by payments
                BigDecimal invoiceTotal = InvoiceWorker.getInvoiceTotal(invoice);
                invoiceApplyAvailable = InvoiceWorker.getInvoiceNotApplied(invoice);

                // adjust the amountAppliedMax value if required....
                if (invoiceApplyAvailable.compareTo(amountAppliedMax) < 0) {
                    amountAppliedMax = invoiceApplyAvailable;
                }

                if (invoiceTotal.signum() == 0) {
                    errorMessageList.add(UtilMessage.expandLabel("AccountingInvoiceTotalZero", UtilMisc.toMap("invoiceId", invoiceId), locale));
                } else if (paymentApplicationId == null) {
                    // only check for new records here...updates are checked in the paymentApplication section
                    if (invoiceApplyAvailable.signum() == 0) {
                        errorMessageList.add(UtilMessage.expandLabel("AccountingInvoiceCompletelyApplied", UtilMisc.toMap("invoiceId", invoiceId), locale));
                    } else if (amountApplied.compareTo(invoiceApplyAvailable) > 0) {
                        // check here for too much application if a new record(s) are
                        // added (paymentApplicationId == null)
                        errorMessageList.add(UtilMessage.expandLabel("AccountingInvoiceLessRequested",
                                UtilMisc.toMap("invoiceId", invoiceId,
                                               "invoiceApplyAvailable", invoiceApplyAvailable,
                                               "amountApplied", amountApplied,
                                               "isoCode", invoice.getString("currencyUomId")), locale));
                    }
                }

                // check if at least one sender is the same as one receiver on the invoice
                if (!payment.getString("partyIdFrom").equals(invoice.getString("partyId")) && !payment.getString("partyIdTo").equals(invoice.getString("partyIdFrom")))    {
                    errorMessageList.add(UtilMessage.expandLabel("AccountingFromPartySameToParty", locale));
                }

                if (debug) {
                    Debug.logInfo("Invoice info retrieved and checked ...", MODULE);
                }
            }

            // if provided check the invoice item.
            if (invoiceItemSeqId != null) {
                // when itemSeqNr not provided delay checking on invoiceItemSeqId
                try {
                    invoiceItem = delegator.findByPrimaryKey("InvoiceItem", UtilMisc.toMap("invoiceId", invoiceId, "invoiceItemSeqId", invoiceItemSeqId));
                } catch (GenericEntityException e) {
                    ServiceUtil.returnError(e.getMessage());
                }

                if (invoiceItem == null) {
                    errorMessageList.add(UtilMessage.expandLabel("AccountingInvoiceItemNotFound", UtilMisc.toMap("invoiceId", invoiceId, "invoiceItemSeqId", invoiceItemSeqId), locale));
                } else {
                    if (invoice.get("currencyUomId") != null && payment.get("currencyUomId") != null && !invoice.getString("currencyUomId").equals(payment.getString("currencyUomId"))) {
                        errorMessageList.add(UtilMessage.expandLabel("AccountingInvoicePaymentCurrencyProblem", UtilMisc.toMap("paymentCurrencyId", payment.getString("currencyUomId"), "itemCurrency", invoice.getString("currencyUomId")), locale));
                    }

                    // get the invoice item applied value
                    BigDecimal quantity = null;
                    if (invoiceItem.get("quantity") == null) {
                        quantity = BigDecimal.ONE;
                    } else {
                        quantity = invoiceItem.getBigDecimal("quantity").setScale(DECIMALS, ROUNDING);
                    }
                    invoiceItemApplyAvailable = invoiceItem.getBigDecimal("amount").multiply(quantity).setScale(DECIMALS, ROUNDING).subtract(InvoiceWorker.getInvoiceItemApplied(invoiceItem));
                    // check here for too much application if a new record is added
                    // (paymentApplicationId == null)
                    if (paymentApplicationId == null && amountApplied.compareTo(invoiceItemApplyAvailable) > 0) {
                        // new record
                        errorMessageList.add("Invoice(" + invoiceId + ") item(" + invoiceItemSeqId + ") has  " + invoiceItemApplyAvailable + " to apply but " + amountApplied + " is requested\n");
                        String uomId = invoice.getString("currencyUomId");
                        errorMessageList.add(UtilMessage.expandLabel("AccountingInvoiceItemLessRequested",
                                UtilMisc.toMap("invoiceId", invoiceId,
                                               "invoiceItemSeqId", invoiceItemSeqId,
                                               "invoiceItemApplyAvailable", invoiceItemApplyAvailable,
                                               "amountApplied", amountApplied,
                                               "isoCode", uomId), locale));
                    }
                }
                if (debug) {
                    Debug.logInfo("InvoiceItem info retrieved and checked against the Invoice (currency and amounts) ...", MODULE);
                }
            }
        }

        // get the application record if the applicationId is supplied if not
        // create empty record.
        BigDecimal newInvoiceApplyAvailable = invoiceApplyAvailable;
        // amount available on the invoice taking into account if the invoiceItemnumber has changed
        BigDecimal newInvoiceItemApplyAvailable = invoiceItemApplyAvailable;
        // amount available on the invoiceItem taking into account if the itemnumber has changed
        BigDecimal newToPaymentApplyAvailable = toPaymentApplyAvailable;
        BigDecimal newPaymentApplyAvailable = paymentApplyAvailable;
        GenericValue paymentApplication = null;
        if (paymentApplicationId == null) {
            paymentApplication = delegator.makeValue("PaymentApplication");
            // prepare for creation
        } else { // retrieve existing paymentApplication
            try {
                paymentApplication = delegator.findByPrimaryKey("PaymentApplication", UtilMisc.toMap("paymentApplicationId", paymentApplicationId));
            } catch (GenericEntityException e) {
                ServiceUtil.returnError(e.getMessage());
            }

            if (paymentApplication == null) {
                errorMessageList.add(UtilMessage.expandLabel("AccountingPaymentApplicationNotFound", UtilMisc.toMap("paymentApplicationId", paymentApplicationId), locale));
                paymentApplicationId = null;
            } else {

                // if both invoiceId and BillingId is entered there was
                // obviously a change
                // only take the newly entered item, same for tax authority and toPayment
                if (paymentApplication.get("invoiceId") == null && invoiceId != null) {
                    billingAccountId = null;
                    taxAuthGeoId = null;
                    toPaymentId = null;
                } else if (paymentApplication.get("toPaymentId") == null && toPaymentId != null) {
                    invoiceId = null;
                    invoiceItemSeqId = null;
                    taxAuthGeoId = null;
                    billingAccountId = null;
                } else if (paymentApplication.get("billingAccountId") == null && billingAccountId != null) {
                    invoiceId = null;
                    invoiceItemSeqId = null;
                    toPaymentId = null;
                    taxAuthGeoId = null;
                } else if (paymentApplication.get("taxAuthGeoId") == null && taxAuthGeoId != null) {
                    invoiceId = null;
                    invoiceItemSeqId = null;
                    toPaymentId = null;
                    billingAccountId = null;
                }

                // check if the payment for too much application if an existing
                // application record is changed
                newPaymentApplyAvailable = paymentApplyAvailable.add(paymentApplication.getBigDecimal("amountApplied")).subtract(amountApplied).setScale(DECIMALS, ROUNDING);
                if (newPaymentApplyAvailable.compareTo(BigDecimal.ZERO) < 0) {
                    errorMessageList.add(UtilMessage.expandLabel("AccountingPaymentNotEnough", UtilMisc.toMap("paymentId", paymentId, "paymentApplyAvailable", paymentApplyAvailable.add(paymentApplication.getBigDecimal("amountApplied")), "amountApplied", amountApplied), locale));
                }

                if (invoiceId != null) {
                    // only when we are processing an invoice on existing paymentApplication check invoice item for to much application if the invoice
                    // number did not change
                    if (invoiceId.equals(paymentApplication .getString("invoiceId"))) {
                        // check if both the itemNumbers are null then this is a
                        // record for the whole invoice
                        if (invoiceItemSeqId == null && paymentApplication.get("invoiceItemSeqId") == null) {
                            newInvoiceApplyAvailable = invoiceApplyAvailable.add(paymentApplication.getBigDecimal("amountApplied")).subtract(amountApplied).setScale(DECIMALS, ROUNDING);
                            if (invoiceApplyAvailable.compareTo(BigDecimal.ZERO) < 0) {
                                errorMessageList.add(UtilMessage.expandLabel("AccountingInvoiceNotEnough", UtilMisc.toMap("tooMuch", newInvoiceApplyAvailable.negate(), "invoiceId", invoiceId), locale));
                            }
                        } else if (invoiceItemSeqId == null && paymentApplication.get("invoiceItemSeqId") != null) {
                            // check if the item number changed from a real Item number to a null value
                            newInvoiceApplyAvailable = invoiceApplyAvailable.add(paymentApplication.getBigDecimal("amountApplied")).subtract(amountApplied).setScale(DECIMALS, ROUNDING);
                            if (invoiceApplyAvailable.compareTo(BigDecimal.ZERO) < 0) {
                                errorMessageList.add(UtilMessage.expandLabel("AccountingInvoiceNotEnough", UtilMisc.toMap("tooMuch", newInvoiceApplyAvailable.negate(), "invoiceId", invoiceId), locale));
                            }
                        } else if (invoiceItemSeqId != null && paymentApplication.get("invoiceItemSeqId") == null) {
                            // check if the item number changed from a null value to
                            // a real Item number
                            newInvoiceItemApplyAvailable = invoiceItemApplyAvailable.subtract(amountApplied).setScale(DECIMALS, ROUNDING);
                            if (newInvoiceItemApplyAvailable.compareTo(BigDecimal.ZERO) < 0) {
                                errorMessageList.add(UtilMessage.expandLabel("AccountingItemInvoiceNotEnough", UtilMisc.toMap("tooMuch", newInvoiceItemApplyAvailable.negate(), "invoiceId", invoiceId, "invoiceItemSeqId", invoiceItemSeqId), locale));
                            }
                        } else if (invoiceItemSeqId.equals(paymentApplication.getString("invoiceItemSeqId"))) {
                            // check if the real item numbers the same
                            // item number the same numeric value
                            newInvoiceItemApplyAvailable = invoiceItemApplyAvailable.add(paymentApplication.getBigDecimal("amountApplied")).subtract(amountApplied).setScale(DECIMALS, ROUNDING);
                            if (newInvoiceItemApplyAvailable.compareTo(BigDecimal.ZERO) < 0) {
                                errorMessageList.add(UtilMessage.expandLabel("AccountingItemInvoiceNotEnough", UtilMisc.toMap("tooMuch", newInvoiceItemApplyAvailable.negate(), "invoiceId", invoiceId, "invoiceItemSeqId", invoiceItemSeqId), locale));
                            }
                        } else {
                            // item number changed only check new item
                            newInvoiceItemApplyAvailable = invoiceItemApplyAvailable.add(amountApplied).setScale(DECIMALS, ROUNDING);
                            if (newInvoiceItemApplyAvailable.compareTo(BigDecimal.ZERO) < 0) {
                                errorMessageList.add(UtilMessage.expandLabel("AccountingItemInvoiceNotEnough", UtilMisc.toMap("tooMuch", newInvoiceItemApplyAvailable.negate(), "invoiceId", invoiceId, "invoiceItemSeqId", invoiceItemSeqId), locale));
                            }
                        }

                        // if the amountApplied = 0 give it the higest possible
                        // value
                        if (amountApplied.signum() == 0) {
                            if (newInvoiceItemApplyAvailable.compareTo(newPaymentApplyAvailable) < 0) {
                                amountApplied = newInvoiceItemApplyAvailable;
                                // from the item number
                            } else {
                                amountApplied = newPaymentApplyAvailable;
                                // from the payment
                            }
                        }

                        // check the invoice
                        newInvoiceApplyAvailable = invoiceApplyAvailable.add(paymentApplication.getBigDecimal("amountApplied").subtract(amountApplied)).setScale(DECIMALS, ROUNDING);
                        if (newInvoiceApplyAvailable.compareTo(BigDecimal.ZERO) < 0) {
                            errorMessageList.add(UtilMessage.expandLabel("AccountingInvoiceNotEnough", UtilMisc.toMap("tooMuch", invoiceApplyAvailable.add(paymentApplication.getBigDecimal("amountApplied")).subtract(amountApplied), "invoiceId", invoiceId), locale));
                        }
                    }
                }

                // check the toPayment account when only the amountApplied has
                // changed,
                if (toPaymentId != null && toPaymentId.equals(paymentApplication.getString("toPaymentId"))) {
                    newToPaymentApplyAvailable = toPaymentApplyAvailable.subtract(paymentApplication.getBigDecimal("amountApplied")).add(amountApplied).setScale(DECIMALS, ROUNDING);
                    if (newToPaymentApplyAvailable.compareTo(BigDecimal.ZERO) < 0) {
                        errorMessageList.add(UtilMessage.expandLabel("AccountingPaymentNotEnough", UtilMisc.toMap("paymentId", toPaymentId, "paymentApplyAvailable", newToPaymentApplyAvailable, "amountApplied", amountApplied), locale));
                    }
                } else if (toPaymentId != null) {
                    // billing account entered number has changed so we have to
                    // check the new billing account number.
                    newToPaymentApplyAvailable = toPaymentApplyAvailable.add(amountApplied).setScale(DECIMALS, ROUNDING);
                    if (newToPaymentApplyAvailable.compareTo(BigDecimal.ZERO) < 0) {
                        errorMessageList.add(UtilMessage.expandLabel("AccountingPaymentNotEnough", UtilMisc.toMap("paymentId", toPaymentId, "paymentApplyAvailable", newToPaymentApplyAvailable, "amountApplied", amountApplied), locale));
                    }

                }
            }
            if (debug) {
                Debug.logInfo("paymentApplication record info retrieved and checked...", MODULE);
            }
        }

        // show the maximumus what can be added in the payment application file.
        String toMessage = null;  // prepare for success message
        if (debug) {
            String extra = "";
            if (invoiceItemSeqId != null) {
                extra = " Invoice item(" + invoiceItemSeqId + ") amount not yet applied: " + newInvoiceItemApplyAvailable;
            }
            Debug.logInfo("checking finished, start processing with the following data... ", MODULE);
            if (invoiceId != null) {
                Debug.logInfo(" Invoice(" + invoiceId + ") amount not yet applied: " + newInvoiceApplyAvailable + extra + " Payment(" + paymentId + ") amount not yet applied: " + newPaymentApplyAvailable +  " Requested amount to apply:" + amountApplied, MODULE);
                toMessage = UtilMessage.expandLabel("AccountingApplicationToInvoice", UtilMisc.toMap("invoiceId", invoiceId), locale);
                if (extra.length() > 0) {
                    toMessage = UtilMessage.expandLabel("AccountingApplicationToInvoiceItem", UtilMisc.toMap("invoiceId", invoiceId, "invoiceItemSeqId", invoiceItemSeqId), locale);
                }
            } else if (toPaymentId != null) {
                Debug.logInfo(" toPayment(" + toPaymentId + ") amount not yet applied: " + newToPaymentApplyAvailable + " Payment(" + paymentId + ") amount not yet applied: " + newPaymentApplyAvailable + " Requested amount to apply:" + amountApplied, MODULE);
                toMessage = UtilMessage.expandLabel("AccountingApplicationToPayment", UtilMisc.toMap("paymentId", toPaymentId), locale);
            } else if (taxAuthGeoId != null) {
                Debug.logInfo(" taxAuthGeoId(" + taxAuthGeoId + ")  Payment(" + paymentId + ") amount not yet applied: " + newPaymentApplyAvailable + " Requested amount to apply:" + amountApplied, MODULE);
                toMessage = UtilMessage.expandLabel("AccountingApplicationToTax", UtilMisc.toMap("taxAuthGeoId", taxAuthGeoId), locale);
            } else if (UtilValidate.isNotEmpty(paymentApplication.get("overrideGlAccountId"))) {
                Debug.logInfo(" GLAccount(" + paymentApplication.get("overrideGlAccountId") + ")  Payment(" + paymentId + ") amount not yet applied: " + newPaymentApplyAvailable + " Requested amount to apply:" + amountApplied, MODULE);
                toMessage = UtilMessage.expandLabel("FinancialsPaymentApplicationToGlAccount", UtilMisc.toMap("glAccountId", paymentApplication.get("overrideGlAccountId")), locale);
            }
        }
        // if the amount to apply was not provided or was zero fill it with the maximum possible and provide information to the user
        if (amountApplied.signum() == 0 &&  useHighestAmount.equals("Y")) {
            amountApplied = newPaymentApplyAvailable;
            if (invoiceId != null && newInvoiceApplyAvailable.compareTo(amountApplied) < 0) {
                amountApplied = newInvoiceApplyAvailable;
                toMessage = UtilMessage.expandLabel("AccountingApplicationToInvoice", UtilMisc.toMap("invoiceId", invoiceId), locale);
            }
            if (toPaymentId != null && newToPaymentApplyAvailable.compareTo(amountApplied) < 0) {
                amountApplied = newToPaymentApplyAvailable;
                toMessage = UtilMessage.expandLabel("AccountingApplicationToPayment", UtilMisc.toMap("paymentId", toPaymentId), locale);
            }
        }

        if (amountApplied.signum() == 0) {
            errorMessageList.add(UtilMessage.expandLabel("AccountingNoAmount", locale));
        } else {
            successMessage = UtilMessage.expandLabel("AccountingApplicationSuccess", UtilMisc.toMap("amountApplied", amountApplied, "paymentId", paymentId, "isoCode", payment.getString("currencyUomId"), "toMessage", toMessage), locale);
        }

        // report error messages if any
        if (errorMessageList.size() > 0) {
            return ServiceUtil.returnError(errorMessageList);
        }

        // ============ start processing ======================
        // if the application is specified it is easy, update the existing record only
        if (paymentApplicationId != null) {
            // record is already retrieved previously
            if (debug) {
                Debug.logInfo("Process an existing paymentApplication record: " + paymentApplicationId, MODULE);
            }
            // update the current record
            paymentApplication.set("invoiceId", invoiceId);
            paymentApplication.set("invoiceItemSeqId", invoiceItemSeqId);
            paymentApplication.set("paymentId", paymentId);
            paymentApplication.set("toPaymentId", toPaymentId);
            paymentApplication.set("amountApplied", amountApplied);
            paymentApplication.set("billingAccountId", billingAccountId);
            paymentApplication.set("taxAuthGeoId", taxAuthGeoId);
            paymentApplication.set("note", note);
            return storePaymentApplication(delegator, paymentApplication, locale);
        }

        // if no invoice sequence number is provided it assumed the requested paymentAmount will be
        // spread over the invoice starting with the lowest sequence number if
        // itemprocessing is on otherwise creat one record
        if (invoiceId != null && paymentId != null && (invoiceItemSeqId == null)) {
            if (invoiceProcessing) {
                // create only a single record with a null seqId
                if (debug) {
                    Debug.logInfo("Try to allocate the payment to the invoice as a whole", MODULE);
                }
                paymentApplication.set("paymentId", paymentId);
                paymentApplication.set("toPaymentId", null);
                paymentApplication.set("invoiceId", invoiceId);
                paymentApplication.set("invoiceItemSeqId", null);
                paymentApplication.set("toPaymentId", null);
                paymentApplication.set("amountApplied", amountApplied);
                paymentApplication.set("billingAccountId", billingAccountId);
                paymentApplication.set("taxAuthGeoId", null);
                paymentApplication.set("note", note);
                if (debug) {
                    Debug.logInfo("creating new paymentapplication", MODULE);
                }
                return storePaymentApplication(delegator, paymentApplication, locale);
            } else { // spread the amount over every single item number
                if (debug) {
                    Debug.logInfo("Try to allocate the payment to the itemnumbers of the invoice", MODULE);
                }
                // get the invoice items
                List<GenericValue> invoiceItems = null;
                try {
                    invoiceItems = delegator.findByAnd("InvoiceItem", UtilMisc.toMap("invoiceId", invoiceId));
                } catch (GenericEntityException e) {
                    ServiceUtil.returnError(e.getMessage());
                }
                if (invoiceItems == null || invoiceItems.size() == 0) {
                    errorMessageList.add("No invoice items found for invoice " + invoiceId + " to match payment against...\n");
                    return ServiceUtil.returnError(errorMessageList);
                } else { // we found some invoice items, start processing....
                    Iterator<GenericValue> i = invoiceItems.iterator();
                    // check if the user want to apply a smaller amount than the maximum possible on the payment
                    if (amountApplied.signum() != 0 && amountApplied.compareTo(paymentApplyAvailable) < 0)    {
                        paymentApplyAvailable = amountApplied;
                    }
                    while (i.hasNext() && paymentApplyAvailable.compareTo(BigDecimal.ZERO) > 0) {
                        // get the invoiceItem
                        invoiceItem = i.next();
                        if (debug) {
                            Debug.logInfo("Start processing item: " + invoiceItem.getString("invoiceItemSeqId"), MODULE);
                        }
                        BigDecimal itemQuantity = BigDecimal.ONE;
                        if (invoiceItem.get("quantity") != null && invoiceItem.getBigDecimal("quantity").signum() != 0) {
                            itemQuantity = invoiceItem.getBigDecimal("quantity").setScale(DECIMALS, ROUNDING);
                        }
                        BigDecimal itemAmount = invoiceItem.getBigDecimal("amount").setScale(DECIMALS, ROUNDING);
                        BigDecimal itemTotal = itemAmount.multiply(itemQuantity).setScale(DECIMALS, ROUNDING);

                        // get the application(s) already allocated to this
                        // item, if available
                        List<GenericValue> paymentApplications = null;
                        try {
                            paymentApplications = invoiceItem.getRelated("PaymentApplication");
                        } catch (GenericEntityException e) {
                            ServiceUtil.returnError(e.getMessage());
                        }
                        BigDecimal tobeApplied = BigDecimal.ZERO;
                        // item total amount - already applied (if any)
                        BigDecimal alreadyApplied = BigDecimal.ZERO;
                        if (paymentApplications != null && paymentApplications.size() > 0) {
                            // application(s) found, add them all together
                            Iterator<GenericValue> p = paymentApplications.iterator();
                            while (p.hasNext()) {
                                paymentApplication = p.next();
                                alreadyApplied = alreadyApplied.add(paymentApplication.getBigDecimal("amountApplied").setScale(DECIMALS, ROUNDING));
                            }
                            tobeApplied = itemTotal.subtract(alreadyApplied).setScale(DECIMALS, ROUNDING);
                        } else {
                            // no application connected yet
                            tobeApplied = itemTotal;
                        }
                        if (debug) {
                            Debug.logInfo("tobeApplied:(" + tobeApplied + ") = " + "itemTotal(" + itemTotal + ") - alreadyApplied(" + alreadyApplied + ") but not more then (nonapplied) paymentAmount(" + paymentApplyAvailable + ")", MODULE);
                        }

                        if (tobeApplied.signum() == 0) {
                            // invoiceItem already fully applied so look at the next one....
                            continue;
                        }

                        if (paymentApplyAvailable.compareTo(tobeApplied) > 0) {
                            paymentApplyAvailable = paymentApplyAvailable.subtract(tobeApplied);
                        } else {
                            tobeApplied = paymentApplyAvailable;
                            paymentApplyAvailable = BigDecimal.ZERO;
                        }

                        // create application payment record but check currency
                        // first if supplied
                        if (invoice.get("currencyUomId") != null && payment.get("currencyUomId") != null && !invoice.getString("currencyUomId").equals(payment.getString("currencyUomId"))) {
                            errorMessageList.add("Payment currency (" + payment.getString("currencyUomId") + ") and invoice currency(" + invoice.getString("currencyUomId") + ") not the same\n");
                        } else {
                            paymentApplication.set("paymentApplicationId", null);
                            // make sure we get a new record
                            paymentApplication.set("invoiceId", invoiceId);
                            paymentApplication.set("invoiceItemSeqId", invoiceItem.getString("invoiceItemSeqId"));
                            paymentApplication.set("paymentId", paymentId);
                            paymentApplication.set("toPaymentId", toPaymentId);
                            paymentApplication.set("amountApplied", tobeApplied);
                            paymentApplication.set("billingAccountId", billingAccountId);
                            paymentApplication.set("taxAuthGeoId", taxAuthGeoId);
                            paymentApplication.set("note", note);
                            storePaymentApplication(delegator, paymentApplication, locale);
                        }
                    }

                    if (errorMessageList.size() > 0) {
                        return ServiceUtil.returnError(errorMessageList);
                    } else {
                        if (successMessage != null) {
                            return ServiceUtil.returnSuccess(successMessage);
                        } else {
                            return ServiceUtil.returnSuccess();
                        }
                    }
                }
            }
        }

        // if no paymentApplicationId supplied create a new record with the data
        // supplied...
        if (paymentApplicationId == null && amountApplied != null) {
            paymentApplication.set("paymentApplicationId", paymentApplicationId);
            paymentApplication.set("invoiceId", invoiceId);
            paymentApplication.set("invoiceItemSeqId", invoiceItemSeqId);
            paymentApplication.set("paymentId", paymentId);
            paymentApplication.set("toPaymentId", toPaymentId);
            paymentApplication.set("amountApplied", amountApplied);
            paymentApplication.set("billingAccountId", billingAccountId);
            paymentApplication.set("taxAuthGeoId", taxAuthGeoId);
            paymentApplication.set("note", note);
            return storePaymentApplication(delegator, paymentApplication, locale);
        }

        // should never come here...
        errorMessageList.add("??unsuitable parameters passed...?? This message.... should never be shown\n");
        errorMessageList.add("--Input parameters...InvoiceId:" + invoiceId + " invoiceItemSeqId:" + invoiceItemSeqId + " PaymentId:" + paymentId + " toPaymentId:" + toPaymentId + "\n  paymentApplicationId:" + paymentApplicationId + " amountApplied:" + amountApplied);
        return ServiceUtil.returnError(errorMessageList);
    }


    /**
     * Sets all transaction entries related to a payment to partially reconciled when the payment is confirmed.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    public static Map<String, Object> partiallyReconcilePayment(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();

        // it is assumed that this service is attached as a seca, so no security is checked
        String paymentId = (String) context.get("paymentId");
        try {
            // transaction header stores the paymentId, but the entries store the reconcile status
            List<GenericValue> transactions = delegator.findByAnd("AcctgTrans", UtilMisc.toMap("paymentId", paymentId));
            List<GenericValue> entries = EntityUtil.getRelated("AcctgTransEntry", transactions);
            for (GenericValue entry : entries) {
                entry.set("reconcileStatusId", "AES_PARTLY_RECON");
            }
            delegator.storeAll(entries);
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }


    /**
     * Overrides legacy service to prevent check for applied payments on confirm.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> setPaymentStatus(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        Security security = dctx.getSecurity();

        if (!(security.hasEntityPermission("ACCOUNTING", "_UPDATE", userLogin) || security.hasEntityPermission("ACCOUNTING_ROLE", "_UPDATE", userLogin))) {
            return UtilMessage.createServiceError("AccountingPermissionError", locale);
        }

        String paymentId = (String) context.get("paymentId");
        String statusId = (String) context.get("statusId");
        try {
            GenericValue payment = delegator.findByPrimaryKey("Payment", UtilMisc.toMap("paymentId", paymentId));
            String oldStatusId = payment.getString("statusId");

            // only change status when old and new statuses are different
            if (!statusId.equals(oldStatusId)) {

                // see if it's a valid change
                GenericValue validChange = delegator.findByPrimaryKey("StatusValidChange", UtilMisc.toMap("statusId", oldStatusId, "statusIdTo", statusId));
                if (validChange == null) {
                    return UtilMessage.createServiceError("AccountingPSInvalidStatusChange", locale);
                }

                // remove applications if payment is being cancelled
                if ("PMNT_CANCELLED".equals(statusId)) {
                    // removeByXXX does not trigger the remove ECA
                    List<GenericValue> toRemove = delegator.findByAnd("PaymentApplication", UtilMisc.toMap("paymentId", paymentId));
                    for (GenericValue r : toRemove) {
                        delegator.removeValue(r);
                    }
                }

                // all set for status change
                payment.set("statusId", statusId);
                payment.store();
            }

            // return original status so other SECAs can detect a proper status change
            Map result = ServiceUtil.returnSuccess();
            result.put("oldStatusId", oldStatusId);
            return result;
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
    }



    /**
     * This method was copy from ofbiz InvoiceServices for add accounting tags support.
     * Service to add payment application records to indicate which invoices
     * have been paid/received. For invoice processing, this service works on
     * the invoice level when 'invoiceProcessing' parameter is set to "Y" else
     * it works on the invoice item level.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */

    public static Map<String, Object> updatePaymentApplicationBd(DispatchContext dctx, Map<String, Object> context) {
        BigDecimal amountApplied = (BigDecimal) context.get("amountApplied");
        if (amountApplied != null) {
            context.put("amountApplied", amountApplied);
        } else {
            context.put("amountApplied", BigDecimal.ZERO);
        }
        if (!context.containsKey("useHighestAmount")) {
            context.put("useHighestAmount", "N");
        }
        return updatePaymentApplicationDefBd(dctx, context);
    }

    private static String successMessage = null;

    /**
     * This method was copied from ofbiz InvoiceServices for add accounting tags support.
     * Service to add payment application records to indicate which invoices
     * have been paid/received. For invoice processing, this service works on
     * the invoice level when 'invoiceProcessing' parameter is set to "Y" else
     * it works on the invoice item level.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    public static Map<String, Object> updatePaymentApplicationDefBd(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Delegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");

        if (DECIMALS == -1 || ROUNDING == -1) {
            return ServiceUtil.returnError(UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingAritmeticPropertiesNotConfigured", locale));
        }

        if (!context.containsKey("useHighestAmount")) {
            context.put("useHighestAmount", "Y");
        }

        String defaultInvoiceProcessing = UtilProperties.getPropertyValue("AccountingConfig", "invoiceProcessing");

        boolean debug = true; // show processing messages in the log..or not....

        // a 'y' in invoiceProssesing wil reverse the default processing
        String changeProcessing = (String) context.get("invoiceProcessing");
        String invoiceId = (String) context.get("invoiceId");
        String invoiceItemSeqId = (String) context.get("invoiceItemSeqId");
        String paymentId = (String) context.get("paymentId");
        String toPaymentId = (String) context.get("toPaymentId");
        String paymentApplicationId = (String) context.get("paymentApplicationId");
        BigDecimal amountApplied = (BigDecimal) context.get("amountApplied");
        String billingAccountId = (String) context.get("billingAccountId");
        String taxAuthGeoId = (String) context.get("taxAuthGeoId");
        String useHighestAmount = (String) context.get("useHighestAmount");
        String note = (String) context.get("note");

        List<String> errorMessageList = new LinkedList<String>();

        if (debug) {
            String log = "updatePaymentApplicationDefBd input parameters..."
                + " defaultInvoiceProcessing: " + defaultInvoiceProcessing
                + " changeDefaultInvoiceProcessing: " + changeProcessing
                + " useHighestAmount: " + useHighestAmount
                + " paymentApplicationId: " + paymentApplicationId
                + " PaymentId: " + paymentId
                + " InvoiceId: " + invoiceId
                + " InvoiceItemSeqId: " + invoiceItemSeqId
                + " BillingAccountId: " + billingAccountId
                + " toPaymentId: " + toPaymentId
                + " amountApplied: " + amountApplied
                + " TaxAuthGeoId: " + taxAuthGeoId
                + " note: " + note;
            // add tags condition, just update the PaymentApplication who have same accounting tags
            for (int i = 1; i <= UtilAccountingTags.TAG_COUNT; i++) {
                if (UtilValidate.isNotEmpty(UtilCommon.getParameter(context, UtilAccountingTags.ENTITY_TAG_PREFIX + i)))
                    log += " " + UtilAccountingTags.ENTITY_TAG_PREFIX +  ": " + UtilCommon.getParameter(context, UtilAccountingTags.ENTITY_TAG_PREFIX + i);
            }
            Debug.logInfo(log, MODULE);
        }
        if (changeProcessing == null) {
            changeProcessing = "N";    // not provided, so no change
        }

        boolean invoiceProcessing = true;
        if (defaultInvoiceProcessing.equals("YY")) {
            invoiceProcessing = true;
        } else if (defaultInvoiceProcessing.equals("NN")) {
            invoiceProcessing = false;
        } else if (defaultInvoiceProcessing.equals("Y")) {
            if (changeProcessing.equals("Y")) {
                invoiceProcessing = false;
            } else {
                invoiceProcessing = true;
            }
        } else if (defaultInvoiceProcessing.equals("N")) {
            if (changeProcessing.equals("Y")) {
                invoiceProcessing = true;
            } else {
                invoiceProcessing = false;
            }
        }

        // on a new paymentApplication check if only billing or invoice or tax
        // id is provided not 2,3... BUT a combination of billingAccountId and invoiceId is permitted - that's how you use a
        // Billing Account to pay for an Invoice
        if (paymentApplicationId == null) {
            int count = 0;
            if (invoiceId != null) { count++; }
            if (toPaymentId != null) { count++; }
            if (billingAccountId != null) { count++; }
            if (taxAuthGeoId != null) { count++; }
            if ((billingAccountId != null) && (invoiceId != null)) { count--; }
            if (count != 1) {
                errorMessageList.add(UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingSpecifyInvoiceToPaymentBillingAccountTaxGeoId", locale));
            }
        }

        // avoid null pointer exceptions.
        if (amountApplied == null) {
            amountApplied = BigDecimal.ZERO;
        }
        // makes no sense to have an item numer without an invoice number
        if (invoiceId == null) {
            invoiceItemSeqId = null;
        }

        // retrieve all information and perform checking on the retrieved info.....

        // Payment.....
        BigDecimal paymentApplyAvailable = BigDecimal.ZERO;
        // amount available on the payment reduced by the already applied amounts
        BigDecimal amountAppliedMax = BigDecimal.ZERO;
        // the maximum that can be applied taking payment,invoice,invoiceitem,billing account in concideration
        // if maxApplied is missing, this value can be used
        GenericValue payment = null;
        if (paymentId == null || paymentId.equals("")) {
            errorMessageList.add(UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingPaymentIdBlankNotSupplied", locale));
        } else {
            try {
                payment = delegator.findByPrimaryKey("Payment", UtilMisc.toMap("paymentId", paymentId));
            } catch (GenericEntityException e) {
                ServiceUtil.returnError(e.getMessage());
            }
            if (payment == null) {
                errorMessageList.add(UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingPaymentRecordNotFound", UtilMisc.toMap("paymentId", paymentId), locale));
            }
            paymentApplyAvailable = payment.getBigDecimal("amount").subtract(getPaymentAppliedBd(payment)).setScale(DECIMALS, ROUNDING);

            if (payment.getString("statusId").equals("PMNT_CANCELLED")) {
                errorMessageList.add(UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingPaymentCancelled", UtilMisc.toMap("paymentId", paymentId), locale));
            }
            if (payment.getString("statusId").equals("PMNT_CONFIRMED")) {
                errorMessageList.add(UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingPaymentConfirmed", UtilMisc.toMap("paymentId", paymentId), locale));
            }

            // if the amount to apply is 0 give it amount the payment still need
            // to apply
            if (amountApplied.signum() == 0) {
                amountAppliedMax = paymentApplyAvailable;
            }

            if (paymentApplicationId == null) {
                // only check for new application records, update on existing records is checked in the paymentApplication section
                if (paymentApplyAvailable.signum() == 0) {
                    errorMessageList.add(UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingPaymentAlreadyApplied", UtilMisc.toMap("paymentId", paymentId), locale));
                } else {
                    // check here for too much application if a new record is
                    // added (paymentApplicationId == null)
                    if (amountApplied.compareTo(paymentApplyAvailable) > 0) {
                        errorMessageList.add(UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingPaymentLessRequested",
                                UtilMisc.toMap("paymentId", paymentId,
                                            "paymentApplyAvailable", paymentApplyAvailable,
                                            "amountApplied", amountApplied, "isoCode", payment.getString("currencyUomId")), locale));
                    }
                }
            }

            if (debug) {
                Debug.logInfo("Payment info retrieved and checked...", MODULE);
            }
        }

        // the "TO" Payment.....
        BigDecimal toPaymentApplyAvailable = BigDecimal.ZERO;
        GenericValue toPayment = null;
        if (toPaymentId != null && !toPaymentId.equals("")) {
            try {
                toPayment = delegator.findByPrimaryKey("Payment", UtilMisc.toMap("paymentId", toPaymentId));
            } catch (GenericEntityException e) {
                ServiceUtil.returnError(e.getMessage());
            }
            if (toPayment == null) {
                errorMessageList.add(UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingPaymentRecordNotFound", UtilMisc.toMap("paymentId", toPaymentId), locale));
            }
            toPaymentApplyAvailable = toPayment.getBigDecimal("amount").subtract(getPaymentAppliedBd(toPayment)).setScale(DECIMALS, ROUNDING);

            if (toPayment.getString("statusId").equals("PMNT_CANCELLED")) {
                errorMessageList.add(UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingPaymentCancelled", UtilMisc.toMap("paymentId", paymentId), locale));
            }
            if (toPayment.getString("statusId").equals("PMNT_CONFIRMED")) {
                errorMessageList.add(UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingPaymentConfirmed", UtilMisc.toMap("paymentId", paymentId), locale));
            }

            // if the amount to apply is less then required by the payment reduce it
            if (amountAppliedMax.compareTo(toPaymentApplyAvailable) > 0) {
                amountAppliedMax = toPaymentApplyAvailable;
            }

            if (paymentApplicationId == null) {
                // only check for new application records, update on existing records is checked in the paymentApplication section
                if (toPaymentApplyAvailable.signum() == 0) {
                    errorMessageList.add(UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingPaymentAlreadyApplied", UtilMisc.toMap("paymentId", toPaymentId), locale));
                } else {
                    // check here for too much application if a new record is
                    // added (paymentApplicationId == null)
                    if (amountApplied.compareTo(toPaymentApplyAvailable) > 0) {
                            errorMessageList.add(UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingPaymentLessRequested",
                                    UtilMisc.toMap("paymentId", toPaymentId,
                                                "paymentApplyAvailable", toPaymentApplyAvailable,
                                                "amountApplied", amountApplied, "isoCode", payment.getString("currencyUomId")), locale));
                    }
                }
            }

            // check if at least one send is the same as one receiver on the other payment
            if (!payment.getString("partyIdFrom").equals(toPayment.getString("partyIdTo"))
                 && !payment.getString("partyIdTo").equals(toPayment.getString("partyIdFrom"))) {
                errorMessageList.add(UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingFromPartySameToParty", locale));
            }

            if (debug) {
                Debug.logInfo("toPayment info retrieved and checked...", MODULE);
            }
        }

        // assign payment to billing account if the invoice is assigned to this billing account
        if (invoiceId != null) {
            GenericValue invoice = null;
            try {
                invoice = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", invoiceId));
            } catch (GenericEntityException e) {
                ServiceUtil.returnError(e.getMessage());
            }

            if (invoice == null) {
                errorMessageList.add(UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingInvoiceNotFound", UtilMisc.toMap("invoiceId", invoiceId), locale));
            } else {
                if (invoice.getString("billingAccountId") != null) {
                    billingAccountId = invoice.getString("billingAccountId");
                }
            }
        }

        // billing account
        GenericValue billingAccount = null;
        if (billingAccountId != null && !billingAccountId.equals("")) {
            try {
                billingAccount = delegator.findByPrimaryKey("BillingAccount", UtilMisc.toMap("billingAccountId", billingAccountId));
            } catch (GenericEntityException e) {
                ServiceUtil.returnError(e.getMessage());
            }
            if (billingAccount == null) {
                errorMessageList.add(UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingBillingAccountNotFound", UtilMisc.toMap("billingAccountId", billingAccountId), locale));
            }
            // check the currency
            if (billingAccount.get("accountCurrencyUomId") != null && payment.get("currencyUomId") != null
                    && !billingAccount.getString("accountCurrencyUomId").equals(payment.getString("currencyUomId"))) {
                errorMessageList.add(UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingBillingAccountCurrencyProblem",
                        UtilMisc.toMap("billingAccountId", billingAccountId, "accountCurrencyUomId", billingAccount.getString("accountCurrencyUomId"),
                                "paymentId", paymentId, "paymentCurrencyUomId", payment.getString("currencyUomId")), locale));
            }

            if (debug) {
                Debug.logInfo("Billing Account info retrieved and checked...", MODULE);
            }
        }

        // get the invoice (item) information
        BigDecimal invoiceApplyAvailable = BigDecimal.ZERO;
        // amount available on the invoice reduced by the already applied amounts
        BigDecimal invoiceItemApplyAvailable = BigDecimal.ZERO;
        // amount available on the invoiceItem reduced by the already applied amounts
        GenericValue invoice = null;
        GenericValue invoiceItem = null;
        if (invoiceId != null) {
            try {
                invoice = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", invoiceId));
            } catch (GenericEntityException e) {
                ServiceUtil.returnError(e.getMessage());
            }

            if (invoice == null) {
                errorMessageList.add(UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingInvoiceNotFound", UtilMisc.toMap("invoiceId", invoiceId), locale));
            } else { // check the invoice and when supplied the invoice item...
                if (invoice.getString("statusId").equals("INVOICE_CANCELLED")) {
                    errorMessageList.add(UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingInvoiceCancelledCannotApplyTo", UtilMisc.toMap("invoiceId", invoiceId), locale));
                }
                // check if the invoice already covered by payments
                BigDecimal invoiceTotal = InvoiceWorker.getInvoiceTotal(invoice);
                invoiceApplyAvailable = InvoiceWorker.getInvoiceNotApplied(invoice);

                // adjust the amountAppliedMax value if required....
                if (invoiceApplyAvailable.compareTo(amountAppliedMax) < 0) {
                    amountAppliedMax = invoiceApplyAvailable;
                }

                if (invoiceTotal.signum() == 0) {
                    errorMessageList.add(UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingInvoiceTotalZero", UtilMisc.toMap("invoiceId", invoiceId), locale));
                } else if (paymentApplicationId == null) {
                    // only check for new records here...updates are checked in the paymentApplication section
                    if (invoiceApplyAvailable.signum() == 0) {
                        errorMessageList.add(UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingInvoiceCompletelyApplied", UtilMisc.toMap("invoiceId", invoiceId), locale));
                    }
                    // check here for too much application if a new record(s) are
                    // added (paymentApplicationId == null)
                    else if (amountApplied.compareTo(invoiceApplyAvailable) > 0) {
                        errorMessageList.add(UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingInvoiceLessRequested",
                                UtilMisc.toMap("invoiceId", invoiceId,
                                            "invoiceApplyAvailable", invoiceApplyAvailable,
                                            "amountApplied", amountApplied, "isoCode", invoice.getString("currencyUomId")), locale));
                    }
                }

                // check if at least one sender is the same as one receiver on the invoice
                if (!payment.getString("partyIdFrom").equals(invoice.getString("partyId"))
                        && !payment.getString("partyIdTo").equals(invoice.getString("partyIdFrom")))    {
                    errorMessageList.add(UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingFromPartySameToParty", locale));
                }

                if (debug) {
                    Debug.logInfo("Invoice info retrieved and checked ...", MODULE);
                }
            }

            // if provided check the invoice item.
            if (invoiceItemSeqId != null) {
                // when itemSeqNr not provided delay checking on invoiceItemSeqId
                try {
                    invoiceItem = delegator.findByPrimaryKey("InvoiceItem", UtilMisc.toMap("invoiceId", invoiceId, "invoiceItemSeqId", invoiceItemSeqId));
                } catch (GenericEntityException e) {
                    ServiceUtil.returnError(e.getMessage());
                }

                if (invoiceItem == null) {
                    errorMessageList.add(UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingInvoiceItemNotFound", UtilMisc.toMap("invoiceId", invoiceId, "invoiceItemSeqId", invoiceItemSeqId), locale));
                } else {
                    if (invoice.get("currencyUomId") != null && payment.get("currencyUomId") != null && !invoice.getString("currencyUomId").equals(payment.getString("currencyUomId"))) {
                        errorMessageList.add(UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingInvoicePaymentCurrencyProblem", UtilMisc.toMap("paymentCurrencyId", payment.getString("currencyUomId"), "itemCurrency", invoice.getString("currencyUomId")), locale));
                    }

                    // get the invoice item applied value
                    BigDecimal quantity = null;
                    if (invoiceItem.get("quantity") == null) {
                        quantity = BigDecimal.ONE;
                    } else {
                        quantity = invoiceItem.getBigDecimal("quantity").setScale(DECIMALS, ROUNDING);
                    }
                    invoiceItemApplyAvailable = invoiceItem.getBigDecimal("amount").multiply(quantity).setScale(DECIMALS, ROUNDING).subtract(getInvoiceItemAppliedBd(invoiceItem));
                    // check here for too much application if a new record is added
                    // (paymentApplicationId == null)
                    if (paymentApplicationId == null && amountApplied.compareTo(invoiceItemApplyAvailable) > 0) {
                        // new record
                        errorMessageList.add("Invoice(" + invoiceId + ") item(" + invoiceItemSeqId + ") has  " + invoiceItemApplyAvailable + " to apply but " + amountApplied + " is requested\n");
                        String uomId = invoice.getString("currencyUomId");
                        errorMessageList.add(UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingInvoiceItemLessRequested",
                                UtilMisc.toMap("invoiceId", invoiceId, "invoiceItemSeqId", invoiceItemSeqId,
                                            "invoiceItemApplyAvailable", invoiceItemApplyAvailable,
                                            "amountApplied", amountApplied, "isoCode", uomId), locale));
                    }
                }
                if (debug) {
                    Debug.logInfo("InvoiceItem info retrieved and checked against the Invoice (currency and amounts) ...", MODULE);
                }
            }
        }

        // get the application record if the applicationId is supplied if not
        // create empty record.
        BigDecimal newInvoiceApplyAvailable = invoiceApplyAvailable;
        // amount available on the invoice taking into account if the invoiceItemnumber has changed
        BigDecimal newInvoiceItemApplyAvailable = invoiceItemApplyAvailable;
        // amount available on the invoiceItem taking into account if the itemnumber has changed
        BigDecimal newToPaymentApplyAvailable = toPaymentApplyAvailable;
        BigDecimal newPaymentApplyAvailable = paymentApplyAvailable;
        GenericValue paymentApplication = null;
        if (paymentApplicationId == null) {
            paymentApplication = delegator.makeValue("PaymentApplication");
            // prepare for creation
        } else { // retrieve existing paymentApplication
            try {
                paymentApplication = delegator.findByPrimaryKey("PaymentApplication", UtilMisc.toMap("paymentApplicationId", paymentApplicationId));
            } catch (GenericEntityException e) {
                ServiceUtil.returnError(e.getMessage());
            }

            if (paymentApplication == null) {
                errorMessageList.add(UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingPaymentApplicationNotFound", UtilMisc.toMap("paymentApplicationId", paymentApplicationId), locale));
                paymentApplicationId = null;
            } else {

                // if both invoiceId and BillingId is entered there was
                // obviously a change
                // only take the newly entered item, same for tax authority and toPayment
                if (paymentApplication.get("invoiceId") == null && invoiceId != null) {
                    billingAccountId = null;
                    taxAuthGeoId = null;
                    toPaymentId = null;
                } else if (paymentApplication.get("toPaymentId") == null && toPaymentId != null) {
                    invoiceId = null;
                    invoiceItemSeqId = null;
                    taxAuthGeoId = null;
                    billingAccountId = null;
                } else if (paymentApplication.get("billingAccountId") == null && billingAccountId != null) {
                    invoiceId = null;
                    invoiceItemSeqId = null;
                    toPaymentId = null;
                    taxAuthGeoId = null;
                } else if (paymentApplication.get("taxAuthGeoId") == null && taxAuthGeoId != null) {
                    invoiceId = null;
                    invoiceItemSeqId = null;
                    toPaymentId = null;
                    billingAccountId = null;
                }

                // check if the payment for too much application if an existing
                // application record is changed
                newPaymentApplyAvailable = paymentApplyAvailable.add(paymentApplication.getBigDecimal("amountApplied")).subtract(amountApplied).setScale(DECIMALS, ROUNDING);
                if (newPaymentApplyAvailable.compareTo(BigDecimal.ZERO) < 0) {
                    errorMessageList.add(UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingPaymentNotEnough", UtilMisc.toMap("paymentId", paymentId, "paymentApplyAvailable", paymentApplyAvailable.add(paymentApplication.getBigDecimal("amountApplied")), "amountApplied", amountApplied), locale));
                }

                if (invoiceId != null) {
                    // only when we are processing an invoice on existing paymentApplication check invoice item for to much application if the invoice
                    // number did not change
                    if (invoiceId.equals(paymentApplication .getString("invoiceId"))) {
                        // check if both the itemNumbers are null then this is a
                        // record for the whole invoice
                        if (invoiceItemSeqId == null && paymentApplication.get("invoiceItemSeqId") == null) {
                            newInvoiceApplyAvailable = invoiceApplyAvailable.add(paymentApplication.getBigDecimal("amountApplied")).subtract(amountApplied).setScale(DECIMALS, ROUNDING);
                            if (invoiceApplyAvailable.compareTo(BigDecimal.ZERO) < 0) {
                                errorMessageList.add(UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingInvoiceNotEnough", UtilMisc.toMap("tooMuch", newInvoiceApplyAvailable.negate(), "invoiceId", invoiceId), locale));
                            }
                        } else if (invoiceItemSeqId == null && paymentApplication.get("invoiceItemSeqId") != null) {
                            // check if the item number changed from a real Item number to a null value
                            newInvoiceApplyAvailable = invoiceApplyAvailable.add(paymentApplication.getBigDecimal("amountApplied")).subtract(amountApplied).setScale(DECIMALS, ROUNDING);
                            if (invoiceApplyAvailable.compareTo(BigDecimal.ZERO) < 0) {
                                errorMessageList.add(UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingInvoiceNotEnough", UtilMisc.toMap("tooMuch", newInvoiceApplyAvailable.negate(), "invoiceId", invoiceId), locale));
                            }
                        } else if (invoiceItemSeqId != null && paymentApplication.get("invoiceItemSeqId") == null) {
                            // check if the item number changed from a null value to
                            // a real Item number
                            newInvoiceItemApplyAvailable = invoiceItemApplyAvailable.subtract(amountApplied).setScale(DECIMALS, ROUNDING);
                            if (newInvoiceItemApplyAvailable.compareTo(BigDecimal.ZERO) < 0) {
                                errorMessageList.add(UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingItemInvoiceNotEnough", UtilMisc.toMap("tooMuch", newInvoiceItemApplyAvailable.negate(), "invoiceId", invoiceId, "invoiceItemSeqId", invoiceItemSeqId), locale));
                            }
                        } else if (invoiceItemSeqId.equals(paymentApplication.getString("invoiceItemSeqId"))) {
                            // check if the real item numbers the same
                            // item number the same numeric value
                            newInvoiceItemApplyAvailable = invoiceItemApplyAvailable.add(paymentApplication.getBigDecimal("amountApplied")).subtract(amountApplied).setScale(DECIMALS, ROUNDING);
                            if (newInvoiceItemApplyAvailable.compareTo(BigDecimal.ZERO) < 0) {
                                errorMessageList.add(UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingItemInvoiceNotEnough", UtilMisc.toMap("tooMuch", newInvoiceItemApplyAvailable.negate(), "invoiceId", invoiceId, "invoiceItemSeqId", invoiceItemSeqId), locale));
                            }
                        } else {
                            // item number changed only check new item
                            newInvoiceItemApplyAvailable = invoiceItemApplyAvailable.add(amountApplied).setScale(DECIMALS, ROUNDING);
                            if (newInvoiceItemApplyAvailable.compareTo(BigDecimal.ZERO) < 0) {
                                errorMessageList.add(UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingItemInvoiceNotEnough", UtilMisc.toMap("tooMuch", newInvoiceItemApplyAvailable.negate(), "invoiceId", invoiceId, "invoiceItemSeqId", invoiceItemSeqId), locale));
                            }
                        }

                        // if the amountApplied = 0 give it the higest possible
                        // value
                        if (amountApplied.signum() == 0) {
                            if (newInvoiceItemApplyAvailable.compareTo(newPaymentApplyAvailable) < 0) {
                                amountApplied = newInvoiceItemApplyAvailable;
                                // from the item number
                            } else {
                                amountApplied = newPaymentApplyAvailable;
                                // from the payment
                            }
                        }

                        // check the invoice
                        newInvoiceApplyAvailable = invoiceApplyAvailable.add(paymentApplication.getBigDecimal("amountApplied").subtract(amountApplied)).setScale(DECIMALS, ROUNDING);
                        if (newInvoiceApplyAvailable.compareTo(BigDecimal.ZERO) < 0) {
                            errorMessageList.add(UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingInvoiceNotEnough", UtilMisc.toMap("tooMuch", invoiceApplyAvailable.add(paymentApplication.getBigDecimal("amountApplied")).subtract(amountApplied), "invoiceId", invoiceId), locale));
                        }
                    }
                }

                // check the toPayment account when only the amountApplied has
                // changed,
                if (toPaymentId != null && toPaymentId.equals(paymentApplication.getString("toPaymentId"))) {
                    newToPaymentApplyAvailable = toPaymentApplyAvailable.subtract(paymentApplication.getBigDecimal("amountApplied")).add(amountApplied).setScale(DECIMALS, ROUNDING);
                    if (newToPaymentApplyAvailable.compareTo(BigDecimal.ZERO) < 0) {
                        errorMessageList.add(UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingPaymentNotEnough", UtilMisc.toMap("paymentId", toPaymentId, "paymentApplyAvailable", newToPaymentApplyAvailable, "amountApplied", amountApplied), locale));
                    }
                } else if (toPaymentId != null) {
                    // billing account entered number has changed so we have to
                    // check the new billing account number.
                    newToPaymentApplyAvailable = toPaymentApplyAvailable.add(amountApplied).setScale(DECIMALS, ROUNDING);
                    if (newToPaymentApplyAvailable.compareTo(BigDecimal.ZERO) < 0) {
                        errorMessageList.add(UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingPaymentNotEnough", UtilMisc.toMap("paymentId", toPaymentId, "paymentApplyAvailable", newToPaymentApplyAvailable, "amountApplied", amountApplied), locale));
                    }

                }
            }
            if (debug) {
                Debug.logInfo("paymentApplication record info retrieved and checked...", MODULE);
            }
        }

        // show the maximumus what can be added in the payment application file.
        String toMessage = null;  // prepare for success message
        if (debug) {
            String extra = "";
            if (invoiceItemSeqId != null) {
                extra = " Invoice item(" + invoiceItemSeqId + ") amount not yet applied: " + newInvoiceItemApplyAvailable;
            }
            Debug.logInfo("checking finished, start processing with the following data... ", MODULE);
            if (invoiceId != null) {
                Debug.logInfo(" Invoice(" + invoiceId + ") amount not yet applied: " + newInvoiceApplyAvailable + extra + " Payment(" + paymentId + ") amount not yet applied: " + newPaymentApplyAvailable +  " Requested amount to apply:" + amountApplied, MODULE);
                toMessage = UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingApplicationToInvoice", UtilMisc.toMap("invoiceId", invoiceId), locale);
                if (extra.length() > 0) {
                    toMessage = UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingApplicationToInvoiceItem", UtilMisc.toMap("invoiceId", invoiceId, "invoiceItemSeqId", invoiceItemSeqId), locale);
                }
            }
            if (toPaymentId != null) {
                Debug.logInfo(" toPayment(" + toPaymentId + ") amount not yet applied: " + newToPaymentApplyAvailable + " Payment(" + paymentId + ") amount not yet applied: " + newPaymentApplyAvailable + " Requested amount to apply:" + amountApplied, MODULE);
                toMessage = UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingApplicationToPayment", UtilMisc.toMap("paymentId", toPaymentId), locale);
            }
            if (taxAuthGeoId != null) {
                Debug.logInfo(" taxAuthGeoId(" + taxAuthGeoId + ")  Payment(" + paymentId + ") amount not yet applied: " + newPaymentApplyAvailable + " Requested amount to apply:" + amountApplied, MODULE);
                toMessage = UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingApplicationToTax", UtilMisc.toMap("taxAuthGeoId", taxAuthGeoId), locale);
            }
        }
        // if the amount to apply was not provided or was zero fill it with the maximum possible and provide information to the user
        if (amountApplied.signum() == 0 &&  useHighestAmount.equals("Y")) {
            amountApplied = newPaymentApplyAvailable;
            if (invoiceId != null && newInvoiceApplyAvailable.compareTo(amountApplied) < 0) {
                amountApplied = newInvoiceApplyAvailable;
                toMessage = UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingApplicationToInvoice", UtilMisc.toMap("invoiceId", invoiceId), locale);
            }
            if (toPaymentId != null && newToPaymentApplyAvailable.compareTo(amountApplied) < 0) {
                amountApplied = newToPaymentApplyAvailable;
                toMessage = UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingApplicationToPayment", UtilMisc.toMap("paymentId", toPaymentId), locale);
            }
        }

        if (amountApplied.signum() == 0) {
            errorMessageList.add(UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingNoAmount", locale));
        } else {
            successMessage = UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingApplicationSuccess", UtilMisc.toMap("amountApplied", amountApplied, "paymentId", paymentId, "isoCode", payment.getString("currencyUomId"), "toMessage", toMessage), locale);
        }

        // report error messages if any
        if (errorMessageList.size() > 0) {
            return ServiceUtil.returnError(errorMessageList);
        }
        Boolean allocatePaymentTagsToApplications = false;
        try {
            DomainsLoader domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(userLogin));
            DomainsDirectory domains = domainLoader.loadDomainsDirectory();
            OrganizationRepositoryInterface ori =  domains.getOrganizationDomain().getOrganizationRepository();
            String paymentTypeId = payment.getString("paymentTypeId");
            GenericValue paymentType = delegator.findByPrimaryKeyCache("PaymentType", UtilMisc.toMap("paymentTypeId", paymentTypeId));
            GenericValue rootPaymentType = getRootPaymentType(paymentType);
            String organizationPartyId = rootPaymentType.getString("paymentTypeId").equals("DISBURSEMENT") ? payment.getString("partyIdFrom") : payment.getString("partyIdTo");
            Organization organization = ori.getOrganizationById(organizationPartyId);
            allocatePaymentTagsToApplications = organization.allocatePaymentTagsToApplications();
        } catch (RepositoryException e) {
            Debug.logError(e, MODULE);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        } catch (EntityNotFoundException e) {
            Debug.logError(e, MODULE);
        }

        // ============ start processing ======================
        // set accounting tags for PaymentApplication when allocatePaymentTagsToApplications is true, else set null as value
        for (int i = 1; i <= UtilAccountingTags.TAG_COUNT; i++) {
            if (allocatePaymentTagsToApplications) {
                paymentApplication.set(UtilAccountingTags.ENTITY_TAG_PREFIX + i, UtilCommon.getParameter(context, UtilAccountingTags.ENTITY_TAG_PREFIX + i));
            } else {
                paymentApplication.set(UtilAccountingTags.ENTITY_TAG_PREFIX + i, null);
            }
        }
           // if the application is specified it is easy, update the existing record only
        if (paymentApplicationId != null) {
            // record is already retrieved previously
            if (debug) {
                Debug.logInfo("Process an existing paymentApplication record: " + paymentApplicationId, MODULE);
            }
            // update the current record
            paymentApplication.set("invoiceId", invoiceId);
            paymentApplication.set("invoiceItemSeqId", invoiceItemSeqId);
            paymentApplication.set("paymentId", paymentId);
            paymentApplication.set("toPaymentId", toPaymentId);
            paymentApplication.set("amountApplied", amountApplied);
            paymentApplication.set("billingAccountId", billingAccountId);
            paymentApplication.set("taxAuthGeoId", taxAuthGeoId);
            paymentApplication.set("note", note);
            return storePaymentApplication(delegator, paymentApplication, locale);
        }

        // if no invoice sequence number is provided it assumed the requested paymentAmount will be
        // spread over the invoice starting with the lowest sequence number if
        // itemprocessing is on otherwise creat one record
        if (invoiceId != null && paymentId != null && (invoiceItemSeqId == null)) {
            if (invoiceProcessing) {
                // create only a single record with a null seqId
                if (debug) {
                    Debug.logInfo("Try to allocate the payment to the invoice as a whole", MODULE);
                }
                paymentApplication.set("paymentId", paymentId);
                paymentApplication.set("toPaymentId", null);
                paymentApplication.set("invoiceId", invoiceId);
                paymentApplication.set("invoiceItemSeqId", null);
                paymentApplication.set("toPaymentId", null);
                paymentApplication.set("amountApplied", amountApplied);
                paymentApplication.set("billingAccountId", billingAccountId);
                paymentApplication.set("taxAuthGeoId", null);
                paymentApplication.set("note", note);
                if (debug) {
                    Debug.logInfo("creating new paymentapplication", MODULE);
                }
                return storePaymentApplication(delegator, paymentApplication, locale);
            } else { // spread the amount over every single item number
                if (debug) {
                    Debug.logInfo("Try to allocate the payment to the itemnumbers of the invoice", MODULE);
                }
                // get the invoice items
                List<GenericValue> invoiceItems = null;
                try {
                    invoiceItems = delegator.findByAnd("InvoiceItem", UtilMisc.toMap("invoiceId", invoiceId));
                } catch (GenericEntityException e) {
                    ServiceUtil.returnError(e.getMessage());
                }
                if (invoiceItems == null || invoiceItems.size() == 0) {
                    errorMessageList.add("No invoice items found for invoice " + invoiceId + " to match payment against...\n");
                    return ServiceUtil.returnError(errorMessageList);
                } else { // we found some invoice items, start processing....
                    Iterator<GenericValue> i = invoiceItems.iterator();
                    // check if the user want to apply a smaller amount than the maximum possible on the payment
                    if (amountApplied.signum() != 0 && amountApplied.compareTo(paymentApplyAvailable) < 0)    {
                        paymentApplyAvailable = amountApplied;
                    }
                    while (i.hasNext() && paymentApplyAvailable.compareTo(BigDecimal.ZERO) > 0) {
                        // get the invoiceItem
                        invoiceItem = i.next();
                        if (debug) {
                            Debug.logInfo("Start processing item: " + invoiceItem.getString("invoiceItemSeqId"), MODULE);
                        }
                        BigDecimal itemQuantity = BigDecimal.ONE;
                        if (invoiceItem.get("quantity") != null && invoiceItem.getBigDecimal("quantity").signum() != 0) {
                            itemQuantity = new BigDecimal(invoiceItem.getString("quantity")).setScale(DECIMALS, ROUNDING);
                        }
                        BigDecimal itemAmount = invoiceItem.getBigDecimal("amount").setScale(DECIMALS, ROUNDING);
                        BigDecimal itemTotal = itemAmount.multiply(itemQuantity).setScale(DECIMALS, ROUNDING);

                        // get the application(s) already allocated to this
                        // item, if available
                        List<GenericValue> paymentApplications = null;
                        try {
                            paymentApplications = invoiceItem.getRelated("PaymentApplication");
                        } catch (GenericEntityException e) {
                            ServiceUtil.returnError(e.getMessage());
                        }
                        BigDecimal tobeApplied = BigDecimal.ZERO;
                        // item total amount - already applied (if any)
                        BigDecimal alreadyApplied = BigDecimal.ZERO;
                        if (paymentApplications != null && paymentApplications.size() > 0) {
                            // application(s) found, add them all together
                            Iterator<GenericValue> p = paymentApplications.iterator();
                            while (p.hasNext()) {
                                paymentApplication = p.next();
                                alreadyApplied = alreadyApplied.add(paymentApplication.getBigDecimal("amountApplied").setScale(DECIMALS, ROUNDING));
                            }
                            tobeApplied = itemTotal.subtract(alreadyApplied).setScale(DECIMALS, ROUNDING);
                        } else {
                            // no application connected yet
                            tobeApplied = itemTotal;
                        }
                        if (debug) {
                            Debug.logInfo("tobeApplied:(" + tobeApplied + ") = " + "itemTotal(" + itemTotal + ") - alreadyApplied(" + alreadyApplied + ") but not more then (nonapplied) paymentAmount(" + paymentApplyAvailable + ")", MODULE);
                        }

                        if (tobeApplied.signum() == 0) {
                            // invoiceItem already fully applied so look at the next one....
                            continue;
                        }

                        if (paymentApplyAvailable.compareTo(tobeApplied) > 0) {
                            paymentApplyAvailable = paymentApplyAvailable.subtract(tobeApplied);
                        } else {
                            tobeApplied = paymentApplyAvailable;
                            paymentApplyAvailable = BigDecimal.ZERO;
                        }

                        // create application payment record but check currency
                        // first if supplied
                        if (invoice.get("currencyUomId") != null && payment.get("currencyUomId") != null && !invoice.getString("currencyUomId").equals(payment.getString("currencyUomId"))) {
                            errorMessageList.add("Payment currency (" + payment.getString("currencyUomId") + ") and invoice currency(" + invoice.getString("currencyUomId") + ") not the same\n");
                        } else {
                            paymentApplication.set("paymentApplicationId", null);
                            // make sure we get a new record
                            paymentApplication.set("invoiceId", invoiceId);
                            paymentApplication.set("invoiceItemSeqId", invoiceItem.getString("invoiceItemSeqId"));
                            paymentApplication.set("paymentId", paymentId);
                            paymentApplication.set("toPaymentId", toPaymentId);
                            paymentApplication.set("amountApplied", tobeApplied);
                            paymentApplication.set("billingAccountId", billingAccountId);
                            paymentApplication.set("taxAuthGeoId", taxAuthGeoId);
                            paymentApplication.set("note", note);
                            storePaymentApplication(delegator, paymentApplication, locale);
                        }
                    }

                    if (errorMessageList.size() > 0) {
                        return ServiceUtil.returnError(errorMessageList);
                    } else {
                        if (successMessage != null) {
                            return ServiceUtil.returnSuccess(successMessage);
                        } else {
                            return ServiceUtil.returnSuccess();
                        }
                    }
                }
            }
        }

        // if no paymentApplicationId supplied create a new record with the data
        // supplied...
        if (paymentApplicationId == null && amountApplied != null) {
            paymentApplication.set("paymentApplicationId", paymentApplicationId);
            paymentApplication.set("invoiceId", invoiceId);
            paymentApplication.set("invoiceItemSeqId", invoiceItemSeqId);
            paymentApplication.set("paymentId", paymentId);
            paymentApplication.set("toPaymentId", toPaymentId);
            paymentApplication.set("amountApplied", amountApplied);
            paymentApplication.set("billingAccountId", billingAccountId);
            paymentApplication.set("taxAuthGeoId", taxAuthGeoId);
            paymentApplication.set("note", note);
            return storePaymentApplication(delegator, paymentApplication, locale);
        }

        // should never come here...
        errorMessageList.add("??unsuitable parameters passed...?? This message.... should never be shown\n");
        errorMessageList.add("--Input parameters...InvoiceId:" + invoiceId + " invoiceItemSeqId:" + invoiceItemSeqId + " PaymentId:" + paymentId + " toPaymentId:" + toPaymentId + "\n  paymentApplicationId:" + paymentApplicationId + " amountApplied:" + amountApplied);
        return ServiceUtil.returnError(errorMessageList);
    }

    /**
     * This method was copy from ofbiz InvoiceServices for add accounting tags support.
     * Update/add to the paymentApplication table and making sure no duplicate record exist.
     *
     * @param delegator a <code>Delegator</code> value
     * @param paymentApplication a <code>GenericValue</code> value
     * @param locale a <code>Locale</code> value
     * @return a <code>Map<String,Object></code> value
     */
    private static Map<String, Object> storePaymentApplication(Delegator delegator, GenericValue paymentApplication, Locale locale) {
        Map<String, Object> results = ServiceUtil.returnSuccess(successMessage);
        boolean debug = true;
        if (debug) {
            Debug.logInfo("Start updating the paymentApplication table ", MODULE);
        }

        if (DECIMALS == -1 || ROUNDING == -1) {
            return ServiceUtil.returnError("Arithmetic properties for Invoice services not configured properly. Cannot proceed.");
        }

        // check if a record already exists with this data
        List<GenericValue> checkAppls = null;
        try {
            Map<String, Object> condition = UtilMisc.<String, Object>toMap(
                    "invoiceId", paymentApplication.get("invoiceId"),
                    "invoiceItemSeqId", paymentApplication.get("invoiceItemSeqId"),
                    "billingAccountId", paymentApplication.get("billingAccountId"),
                    "paymentId", paymentApplication.get("paymentId"),
                    "toPaymentId", paymentApplication.get("toPaymentId"),
                    "taxAuthGeoId", paymentApplication.get("taxAuthGeoId"),
                    "note", paymentApplication.get("note"));
            // add tags condition, just update the PaymentApplication who have same accounting tags
            for (int i = 1; i <= UtilAccountingTags.TAG_COUNT; i++) {
                condition.put(UtilAccountingTags.ENTITY_TAG_PREFIX + i, paymentApplication.get(UtilAccountingTags.ENTITY_TAG_PREFIX + i));
            }
            checkAppls = delegator.findByAnd("PaymentApplication", condition);
        } catch (GenericEntityException e) {
            ServiceUtil.returnError(e.getMessage());
        }
        if (checkAppls != null && checkAppls.size() > 0) {
            if (debug) {
                Debug.logInfo(checkAppls.size() + " records already exist", MODULE);
            }
            // 1 record exists just update and if diffrent ID delete other record and add together.
            GenericValue checkAppl = checkAppls.get(0);
            // if new record  add to the already existing one.
            if (paymentApplication.get("paymentApplicationId") == null) {
                // add 2 amounts together
                checkAppl.set("amountApplied", paymentApplication.getBigDecimal("amountApplied").
                        add(checkAppl.getBigDecimal("amountApplied")).setScale(DECIMALS, ROUNDING));
                // replace the note
                checkAppl.set("note", paymentApplication.getString("note"));
                if (debug) {
                    Debug.logInfo("Update paymentApplication record: " + checkAppl.getString("paymentApplicationId") + " with appliedAmount:" + checkAppl.getBigDecimal("amountApplied"), MODULE);
                }
                try {
                    checkAppl.store();
                } catch (GenericEntityException e) {
                    ServiceUtil.returnError(e.getMessage());
                }
            } else if (paymentApplication.getString("paymentApplicationId").equals(checkAppl.getString("paymentApplicationId"))) {
                // update existing record inplace
                checkAppl.set("amountApplied", paymentApplication.getBigDecimal("amountApplied"));
                // replace the note
                checkAppl.set("note", paymentApplication.getString("note"));
                if (debug) {
                    Debug.logInfo("Update paymentApplication record: " + checkAppl.getString("paymentApplicationId") + " with appliedAmount:" + checkAppl.getBigDecimal("amountApplied"), MODULE);
                }
                try {
                    checkAppl.store();
                } catch (GenericEntityException e) {
                    ServiceUtil.returnError(e.getMessage());
                }
            } else    { // two existing records, an updated one added to the existing one
                // add 2 amounts together
                checkAppl.set("amountApplied", paymentApplication.getBigDecimal("amountApplied").
                        add(checkAppl.getBigDecimal("amountApplied")).setScale(DECIMALS, ROUNDING));
                // replace the note
                checkAppl.set("note", paymentApplication.getString("note"));
                // delete paymentApplication record and update the checkAppls one.
                if (debug) {
                    Debug.logInfo("Delete paymentApplication record: " + paymentApplication.getString("paymentApplicationId") + " with appliedAmount:" + paymentApplication.getBigDecimal("amountApplied"), MODULE);
                }
                try {
                    paymentApplication.remove();
                } catch (GenericEntityException e) {
                    ServiceUtil.returnError(e.getMessage());
                }
                // update amount existing record
                if (debug) {
                    Debug.logInfo("Update paymentApplication record: " + checkAppl.getString("paymentApplicationId") + " with appliedAmount:" + checkAppl.getBigDecimal("amountApplied"), MODULE);
                }
                try {
                    checkAppl.store();
                } catch (GenericEntityException e) {
                    ServiceUtil.returnError(e.getMessage());
                }
            }
        } else {
            if (debug) {
                Debug.logInfo("No records found with paymentId,invoiceid..etc probaly changed one of them...", MODULE);
            }
            // create record if ID null;
            if (paymentApplication.get("paymentApplicationId") == null) {
                paymentApplication.set("paymentApplicationId", delegator.getNextSeqId("PaymentApplication"));
                if (debug) {
                    Debug.logInfo("Create new paymentAppication record: " + paymentApplication.getString("paymentApplicationId") + " with appliedAmount:" + paymentApplication.getBigDecimal("amountApplied"), MODULE);
                }
                try {
                    paymentApplication.create();
                } catch (GenericEntityException e) {
                    ServiceUtil.returnError(e.getMessage());
                }
            } else {
                // update existing record (could not be found because a non existing combination of paymentId/invoiceId/invoiceSeqId/ etc... was provided
                if (debug) {
                    Debug.logInfo("Update existing paymentApplication record: " + paymentApplication.getString("paymentApplicationId") + " with appliedAmount:" + paymentApplication.getBigDecimal("amountApplied"), MODULE);
                }
                try {
                    paymentApplication.store();
                } catch (GenericEntityException e) {
                    ServiceUtil.returnError(e.getMessage());
                }
            }
        }
        successMessage = successMessage.concat(UtilProperties.getMessage(ACCOUNTING_RESOURCE, "AccountingSuccessFull", locale));
        return results;
    }

    private static BigDecimal getInvoiceItemAppliedBd(GenericValue invoiceItem) {
        BigDecimal invoiceItemApplied = BigDecimal.ZERO;
        List<GenericValue> paymentApplications = null;
        try {
            paymentApplications = invoiceItem.getRelated("PaymentApplication");
        } catch (GenericEntityException e) {
            Debug.logError(e, "Trouble getting paymentApplicationlist", MODULE);
        }
        if (paymentApplications != null && paymentApplications.size() > 0) {
            Iterator<GenericValue> p = paymentApplications.iterator();
            while (p.hasNext()) {
                GenericValue paymentApplication = p.next();
                invoiceItemApplied = invoiceItemApplied.add(paymentApplication.getBigDecimal("amountApplied")).setScale(DECIMALS, ROUNDING);
            }
        }
        return invoiceItemApplied;
    }

    private static BigDecimal getPaymentAppliedBd(GenericValue payment) {
        BigDecimal paymentApplied = BigDecimal.ZERO;
        List<GenericValue> paymentApplications = null;
        try {
            paymentApplications = payment.getRelated("PaymentApplication");
        } catch (GenericEntityException e) {
            Debug.logError(e, "Trouble getting paymentApplicationlist", MODULE);
        }
        if (paymentApplications != null && paymentApplications.size() > 0) {
            for (GenericValue paymentApplication : paymentApplications) {
                paymentApplied = paymentApplied.add(paymentApplication.getBigDecimal("amountApplied")).setScale(DECIMALS, ROUNDING);
            }
        }
        // check for payment to payment applications
        paymentApplications = null;
        try {
            paymentApplications = payment.getRelated("ToPaymentApplication");
        } catch (GenericEntityException e) {
            Debug.logError(e, "Trouble getting the 'to' paymentApplicationlist", MODULE);
        }
        if (paymentApplications != null && paymentApplications.size() > 0) {
            for (GenericValue paymentApplication : paymentApplications) {
                paymentApplied = paymentApplied.add(paymentApplication.getBigDecimal("amountApplied")).setScale(DECIMALS, ROUNDING);
            }
        }
        return paymentApplied;
    }

}
