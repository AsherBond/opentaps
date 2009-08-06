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

package com.opensourcestrategies.financials.payment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.accounting.payment.PaymentWorker;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilObject;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
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
import org.opentaps.domain.DomainsLoader;
import org.opentaps.domain.billing.invoice.Invoice;
import org.opentaps.domain.billing.invoice.InvoiceRepositoryInterface;
import org.opentaps.domain.organization.AccountingTagConfigurationForOrganizationAndUsage;
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
    private static final String resource = "FinancialsUiLabels";

    /**
     * Wrapper service for the OFBiz createPayment service, to ensure that the paymentMethodTypeId is passed correctly.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map createPayment(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericDelegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        Security security = dctx.getSecurity();

        if (!(security.hasEntityPermission("FINANCIALS", "_AP_PCRTE", userLogin) || security.hasEntityPermission("FINANCIALS", "_AR_PCRTE", userLogin))) {
            return ServiceUtil.returnError(UtilProperties.getMessage("FinancialsUiLabels", "FinancialsServiceErrorNoPermission", locale));
        }

        Map result = ServiceUtil.returnSuccess();

        try {
            // if return require field error, then throw a service error
            List<AccountingTagConfigurationForOrganizationAndUsage> missings = validateTagParameters(dctx, context);
            if (!missings.isEmpty()) {
                return UtilMessage.createAndLogServiceError("OpentapsError_ServiceErrorRequiredTagNotFound", UtilMisc.toMap("tagName", missings.get(0).getDescription()), locale, MODULE);
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (RepositoryException e) {
            return ServiceUtil.returnError(e.getMessage());
        }

        try {
            // a convoluted way to check the parties exist
            try {
                new PartyReader((String) context.get("partyIdFrom"), delegator);
            } catch (PartyNotFoundException e) {
                return UtilMessage.createAndLogServiceError("OpentapsError_PartyNotFound", UtilMisc.toMap("partyId", context.get("partyIdFrom")), locale, MODULE);
            }
            try {
                new PartyReader((String) context.get("partyIdTo"), delegator);
            } catch (PartyNotFoundException e) {
                return UtilMessage.createAndLogServiceError("OpentapsError_PartyNotFound", UtilMisc.toMap("partyId", context.get("partyIdTo")), locale, MODULE);
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
    @SuppressWarnings("unchecked")
    private static List<AccountingTagConfigurationForOrganizationAndUsage> validateTagParameters(DispatchContext dctx, Map context) throws GenericEntityException, RepositoryException {
        GenericDelegator delegator = dctx.getDelegator();
        String paymentTypeId = (String) context.get("paymentTypeId");
        if (paymentTypeId != null) {
            //get parentTypeId of PaymentType
            GenericValue paymentType = delegator.findByPrimaryKey("PaymentType", UtilMisc.toMap("paymentTypeId", paymentTypeId));
            String accountingTagUsageTypeId = UtilObject.equalsHelper(paymentType.getString("parentTypeId"), "DISBURSEMENT") ? UtilAccountingTags.DISBURSEMENT_PAYMENT_TAG : UtilAccountingTags.RECEIPT_PAYMENT_TAG;
            String organizationPartyId = UtilObject.equalsHelper(paymentType.getString("parentTypeId"), "DISBURSEMENT") ?  (String) context.get("partyIdFrom") : (String) context.get("partyIdTo");
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
     * Wrapper service for the OFBiz updatePayment service, to ensure that the paymentMethodTypeId is passed correctly.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map updatePayment(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        Security security = dctx.getSecurity();

        if (!(security.hasEntityPermission("FINANCIALS", "_AP_PUPDT", userLogin) || security.hasEntityPermission("FINANCIALS", "_AR_PUPDT", userLogin))) {
            return ServiceUtil.returnError(UtilProperties.getMessage("FinancialsUiLabels", "FinancialsServiceErrorNoPermission", locale));
        }


        Map result = ServiceUtil.returnSuccess();

        try {
            // if return require field error, then throw a service error
            List<AccountingTagConfigurationForOrganizationAndUsage> missings = validateTagParameters(dctx, context);
            if (!missings.isEmpty()) {
                return UtilMessage.createAndLogServiceError("OpentapsError_ServiceErrorRequiredTagNotFound", UtilMisc.toMap("tagName", missings.get(0).getDescription()), locale, MODULE);
            }
        } catch (GenericEntityException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (RepositoryException e) {
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
    private static Map resolvePaymentMethodTypeId(DispatchContext dctx, Map context) {

        String paymentMethodId = (String) context.get("paymentMethodId");
        String paymentMethodTypeId = (String) context.get("paymentMethodTypeId");
        GenericDelegator delegator = dctx.getDelegator();
        Locale locale = (Locale) context.get("locale");
        Map result = ServiceUtil.returnSuccess();

        try {

            // If the payment method is supplied, make sure it exists
            if (UtilValidate.isNotEmpty(paymentMethodId)) {
                GenericValue paymentMethod = delegator.findByPrimaryKeyCache("PaymentMethod", UtilMisc.toMap("paymentMethodId", paymentMethodId));
                if (paymentMethod == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, "FinancialsServiceErrorPaymentMethodNotFound", UtilMisc.toMap("paymentMethodId", paymentMethodId), locale));
                } else if (paymentMethod.get("paymentMethodTypeId") == null) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(resource, "FinancialsServiceErrorPaymentMethodTypeNotFound", UtilMisc.toMap("paymentMethodId", paymentMethodId), locale));
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
    public static Map createPaymentApplication(DispatchContext dctx, Map context) throws GenericServiceException {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        // get parameters
        String paymentId = (String) context.get("paymentId");
        Double amountAppliedDbl = (Double) context.get("amountApplied");
        BigDecimal amountApplied = BigDecimal.ZERO;
        if (amountAppliedDbl != null) {
            amountApplied = BigDecimal.valueOf(amountAppliedDbl);
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
        try {
            payment = delegator.findByPrimaryKeyCache("Payment", UtilMisc.toMap("paymentId", paymentId));
        } catch (GenericEntityException e) {
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
            paymentApplication.create();
        } catch (GenericEntityException e) {
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
    public static Map updatePaymentApplication(DispatchContext dctx, Map context) throws GenericServiceException {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String invoiceId = (String) context.get("invoiceId");
        String paymentApplicationId = (String) context.get("paymentApplicationId");
        Double amountApplied = (Double) context.get("amountApplied");

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

        // call the ofbiz service
        Map serviceResults = org.ofbiz.accounting.invoice.InvoiceServices.updatePaymentApplication(dctx, context);
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
     * Sets all transaction entries related to a payment to partially reconciled when the payment is confirmed.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map partiallyReconcilePayment(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();

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
    public static Map setPaymentStatus(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
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
                    delegator.removeByAnd("PaymentApplication", UtilMisc.toMap("paymentId", paymentId));
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
}
