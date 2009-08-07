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

package com.opensourcestrategies.financials.transactions;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilAccountingTags;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.domain.base.entities.AcctgTransEntry;
import org.opentaps.domain.base.entities.PartyAcctgPreference;
import org.opentaps.domain.ledger.LedgerRepositoryInterface;
import org.opentaps.domain.organization.AccountingTagConfigurationForOrganizationAndUsage;
import org.opentaps.domain.organization.Organization;
import org.opentaps.domain.organization.OrganizationRepositoryInterface;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;

/**
 * TransactionServices - Services for dealing with transactions.
 *
 * @author     <a href="mailto:libertine@ars-industria.com">Chris Liberty</a>
 * @version    $Rev$
 */
public final class TransactionServices {

    private TransactionServices() { }

    private static final String MODULE = TransactionServices.class.getName();

    /**
     * Create a Quick <code>AcctgTrans</code> record.
     * IsPosted is forced to "N".
     * Creates an Quick AcctgTrans and two offsetting AcctgTransEntry records.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map createQuickAcctgTrans(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        try {
            DomainsLoader dl = new DomainsLoader(new Infrastructure(dispatcher), new User(userLogin));
            OrganizationRepositoryInterface organizationRepository = dl.loadDomainsDirectory().getOrganizationDomain().getOrganizationRepository();
            String organizationPartyId = (String) context.get("organizationPartyId");
            Organization organization = organizationRepository.getOrganizationById(organizationPartyId);

            // create the accounting transaction
            Map createAcctgTransCtx = dctx.getModelService("createAcctgTrans").makeValid(context, ModelService.IN_PARAM);
            if (UtilValidate.isEmpty(createAcctgTransCtx.get("transactionDate"))) {
                createAcctgTransCtx.put("transactionDate", UtilDateTime.nowTimestamp());
            }
            Map results = dispatcher.runSync("createAcctgTrans", createAcctgTransCtx);
            if (!UtilCommon.isSuccess(results)) {
                return UtilMessage.createAndLogServiceError(results, MODULE);
            }
            String acctgTransId = (String) results.get("acctgTransId");

            // create both debit and credit entries
            String currencyUomId = (String) context.get("currencyUomId");
            if (UtilValidate.isEmpty(currencyUomId)) {
                PartyAcctgPreference partyAcctgPref = organization.getPartyAcctgPreference();
                if (partyAcctgPref != null) {
                    currencyUomId = partyAcctgPref.getBaseCurrencyUomId();
                } else {
                    Debug.logWarning("No accounting preference found for organization: " + organizationPartyId, MODULE);
                }
            }

            // debit entry, using createAcctgTransEntryManual which validate the accounting tags, the tags for are prefixed by "debitTagEnumId"
            Map createAcctgTransEntryCtx = dctx.getModelService("createAcctgTransEntryManual").makeValid(context, ModelService.IN_PARAM);
            Map debitCtx = new HashMap(createAcctgTransEntryCtx);
            UtilAccountingTags.addTagParameters(context, debitCtx, "debitTagEnumId", UtilAccountingTags.ENTITY_TAG_PREFIX);
            debitCtx.put("acctgTransId", acctgTransId);
            debitCtx.put("glAccountId", context.get("debitGlAccountId"));
            debitCtx.put("debitCreditFlag", "D");
            debitCtx.put("acctgTransEntryTypeId", "_NA_");
            debitCtx.put("currencyUomId", currencyUomId);
            results = dispatcher.runSync("createAcctgTransEntryManual", debitCtx);

            // credit entry, the tags for are prefixed by "creditTagEnumId"
            Map creditCtx = new HashMap(createAcctgTransEntryCtx);
            UtilAccountingTags.addTagParameters(context, creditCtx, "creditTagEnumId", UtilAccountingTags.ENTITY_TAG_PREFIX);
            creditCtx.put("acctgTransId", acctgTransId);
            creditCtx.put("glAccountId", context.get("creditGlAccountId"));
            creditCtx.put("debitCreditFlag", "C");
            creditCtx.put("acctgTransEntryTypeId", "_NA_");
            creditCtx.put("currencyUomId", currencyUomId);
            results = dispatcher.runSync("createAcctgTransEntryManual", creditCtx);

            results = ServiceUtil.returnSuccess();
            results.put("acctgTransId", acctgTransId);
            return results;

        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
    }

    /**
     * Service to reverse an <code>AcctgTrans</code> entity.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map reverseAcctgTrans(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Security security = dctx.getSecurity();
        Locale locale = (Locale) context.get("locale");
        String acctgTransId = (String) context.get("acctgTransId");

        if (!security.hasEntityPermission("FINANCIALS", "_REVERSE", userLogin)) {
            return ServiceUtil.returnError(UtilProperties.getMessage("FinancialsUiLabels", "FinancialsServiceErrorNoPermission", locale));
        }

        try {

            // Get the AcctgTrans record
            GenericValue acctgTrans = delegator.findByPrimaryKey("AcctgTrans", UtilMisc.toMap("acctgTransId", acctgTransId));
            if (acctgTrans == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage("FinancialsUiLabels", "FinancialsServiceErrorReverseTransactionNotFound", locale) + ":" + acctgTransId);
            }

            // Get the related AcctgTransEntry records
            List acctgTransEntries = delegator.findByAnd("AcctgTransEntry", UtilMisc.toMap("acctgTransId", acctgTransId), UtilMisc.toList("acctgTransEntrySeqId"));
            if (acctgTrans == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage("FinancialsUiLabels", "FinancialsServiceErrorReverseTransactionNoEntries", locale) + ":" + acctgTransId);
            }

            // Toggle the debit/credit flag, set the reconcileStatusId and remove the acctgTransId from each AcctgTransEntry
            Iterator ateit = acctgTransEntries.iterator();
            while (ateit.hasNext()) {
                GenericValue acctgTransEntry = (GenericValue) ateit.next();
                if (acctgTransEntry.getString("debitCreditFlag").equals("C")) {
                    acctgTransEntry.set("debitCreditFlag", "D");
                } else if (acctgTransEntry.getString("debitCreditFlag").equals("D")) {
                    acctgTransEntry.set("debitCreditFlag", "C");
                }
                acctgTransEntry.set("reconcileStatusId", "AES_NOT_RECONCILED");
                acctgTransEntry.remove("acctgTransId");
            }

            // Assemble the context for the service that creates and posts AcctgTrans and AcctgTransEntry records
            Map serviceMap = acctgTrans.getAllFields();
            serviceMap.remove("acctgTransId");
            serviceMap.remove("createdStamp");
            serviceMap.remove("createdTxStamp");
            serviceMap.remove("lastUpdatedStamp");
            serviceMap.remove("lastUpdatedTxStamp");
            serviceMap.put("transactionDate", UtilDateTime.nowTimestamp());
            serviceMap.put("description", "Reversal of Acctg Trans ID# " + acctgTransId);
            serviceMap.put("acctgTransTypeId", "REVERSE");
            serviceMap.put("acctgTransEntries", acctgTransEntries);
            serviceMap.put("userLogin", userLogin);

            // Call the service
            Map createAcctgTransAndEntriesResult = dispatcher.runSync("createAcctgTransAndEntries", serviceMap);
            if (ServiceUtil.isError(createAcctgTransAndEntriesResult)) {
                return createAcctgTransAndEntriesResult;
            }

            Map serviceResult = ServiceUtil.returnSuccess();
            serviceResult.put("acctgTransId", createAcctgTransAndEntriesResult.get("acctgTransId"));
            return serviceResult;

        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(), MODULE);
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException e) {
            Debug.logError(e.getMessage(), MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    /**
     * Service to void a payment.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map voidPayment(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Security security = dctx.getSecurity();
        Locale locale = (Locale) context.get("locale");
        String paymentId = (String) context.get("paymentId");

        if (!security.hasEntityPermission("FINANCIALS", "_REVERSE", userLogin)) {
            return ServiceUtil.returnError(UtilProperties.getMessage("FinancialsUiLabels", "FinancialsServiceErrorNoPermission", locale));
        }

        try {

            // Get the Payment record
            GenericValue payment = delegator.findByPrimaryKey("Payment", UtilMisc.toMap("paymentId", paymentId));
            if (payment == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage("FinancialsUiLabels", "FinancialsServiceErrorVoidPaymentNotFound", locale) + ":" + payment);
            }

            // Check the Payment status - only payments with status PMNT_SENT or PMNT_RECEIVED can be voided
            if (!(payment.getString("statusId").equals("PMNT_SENT") || payment.getString("statusId").equals("PMNT_RECEIVED"))) {
                return ServiceUtil.returnError(UtilProperties.getMessage("FinancialsUiLabels", "FinancialsServiceErrorVoidPaymentIncorrectStatus", locale) + ":" + payment);
            }

            // Change the Payment status to void
            Map setPaymentStatusResult = dispatcher.runSync("setPaymentStatus", UtilMisc.toMap("paymentId", paymentId, "statusId", "PMNT_VOID", "userLogin", userLogin));
            if (ServiceUtil.isError(setPaymentStatusResult)) {
                return setPaymentStatusResult;
            }

            // Iterate through related PaymentApplications
            List paymentApplications = delegator.getRelated("PaymentApplication", payment);
            Iterator pait = paymentApplications.iterator();
            while (pait.hasNext()) {
                GenericValue paymentApplication = (GenericValue) pait.next();

                // Set the status of related invoice from INVOICE_PAID to INVOICE_READY, if necessary
                if (paymentApplication.getString("invoiceId") != null) {
                    GenericValue invoice = delegator.getRelatedOne("Invoice", paymentApplication);
                    if (invoice.getString("statusId").equals("INVOICE_PAID")) {
                        invoice.set("paidDate", null);
                        delegator.store(invoice);
                        Map setInvoiceStatusResult = dispatcher.runSync("setInvoiceStatus", UtilMisc.toMap("invoiceId", invoice.getString("invoiceId"), "statusId", "INVOICE_READY", "userLogin", userLogin));
                        if (ServiceUtil.isError(setInvoiceStatusResult)) {
                            return setInvoiceStatusResult;
                        }
                    }
                }

                // Remove the PaymentApplication
                delegator.removeValue(paymentApplication);

            }

            // Iterate through related AcctgTrans, calling the reverseAcctgTrans service on each
            List acctgTransList = delegator.getRelated("AcctgTrans", payment);
            Iterator atlit = acctgTransList.iterator();
            while (atlit.hasNext()) {
                GenericValue acctgTrans = (GenericValue) atlit.next();
                Map reverseAcctgTransResult = dispatcher.runSync("reverseAcctgTrans", UtilMisc.toMap("acctgTransId", acctgTrans.getString("acctgTransId"), "userLogin", userLogin));
                if (ServiceUtil.isError(reverseAcctgTransResult)) {
                    return reverseAcctgTransResult;
                }


            }

            return ServiceUtil.returnSuccess();

        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(), MODULE);
            return ServiceUtil.returnError(e.getMessage());
        } catch (GenericServiceException e) {
            Debug.logError(e.getMessage(), MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    /**
     * Posts any AcctgTrans that are currently scheduled to be posted.  Requires ACCOUNTING_ATX_POST permission.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map postScheduledAcctgTrans(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Security security = dctx.getSecurity();
        Locale locale = (Locale) context.get("locale");

        if (!security.hasEntityPermission("ACCOUNTING", "_ATX_POST", userLogin)) {
            return ServiceUtil.returnError(UtilProperties.getMessage("FinancialsUiLabels", "FinancialsServiceErrorNoPermission", locale));
        }

        List transactions = null;
        try {
            List<EntityCondition> conditions = UtilMisc.<EntityCondition>toList(
                    EntityCondition.makeCondition("scheduledPostingDate", EntityOperator.NOT_EQUAL, null),
                    EntityCondition.makeCondition("scheduledPostingDate", EntityOperator.LESS_THAN_EQUAL_TO, UtilDateTime.nowTimestamp()),
                    EntityCondition.makeCondition("isPosted", EntityOperator.EQUALS, "N"));
            transactions = delegator.findByAnd("AcctgTrans", conditions);
        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(), MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        Map input = UtilMisc.toMap("userLogin", userLogin);
        for (Iterator iter = transactions.iterator(); iter.hasNext();) {
            GenericValue transaction = (GenericValue) iter.next();
            input.put("acctgTransId", transaction.get("acctgTransId"));
            try {
                Map results = dispatcher.runSync("postAcctgTrans", input);
                if (ServiceUtil.isError(results)) {
                    Debug.logError("Failed to post scheduled AcctgTransaction [" + transaction.get("acctgTransId") + "] due to logic error: " + ServiceUtil.getErrorMessage(results), MODULE);
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, "Failed to post scheduled AcctgTransaction [" + transaction.get("acctgTransId") + "] due to service engine error: " + e.getMessage(), MODULE);
            }
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Same as the <code>createAcctgTransEntry</code> service but add validation of accounting tags.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     * @throws GenericServiceException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public static Map createAcctgTransEntryManual(DispatchContext dctx, Map context) throws GenericServiceException {
        GenericDelegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        try {
            LedgerRepositoryInterface ledgerRepository = new DomainsLoader(new Infrastructure(dispatcher), new User(userLogin)).loadDomainsDirectory().getLedgerDomain().getLedgerRepository();
            AcctgTransEntry entry = new AcctgTransEntry();
            entry.initRepository(ledgerRepository);
            entry.setAllFields(context);

            // validate the accounting tags
            List<AccountingTagConfigurationForOrganizationAndUsage> missings = ledgerRepository.validateTagParameters(entry);
            if (!missings.isEmpty()) {
                return UtilMessage.createAndLogServiceError("OpentapsError_ServiceErrorRequiredTagNotFound", UtilMisc.toMap("tagName", missings.get(0).getDescription()), locale, MODULE);
            }

            // if not given in the parameters set the currency from the party accounting preferences
            if (UtilValidate.isEmpty(context.get("currencyUomId"))) {
                String baseCurrencyUomId = UtilCommon.getOrgBaseCurrency((String) context.get("organizationPartyId"), delegator);
                if (baseCurrencyUomId == null) {
                    return UtilMessage.createAndLogServiceError("FinancialsServiceErrorPartyAcctgPrefNotFound", UtilMisc.toMap("partyId", context.get("organizationPartyId")), locale, MODULE);
                }
                entry.setCurrencyUomId(baseCurrencyUomId);
            }
            // set reconciled status to AES_NOT_RECONCILED
            entry.setReconcileStatusId("AES_NOT_RECONCILED");
            // create
            entry.setNextSubSeqId(AcctgTransEntry.Fields.acctgTransEntrySeqId.name());
            ledgerRepository.createOrUpdate(entry);

            Map results = ServiceUtil.returnSuccess();
            results.put("acctgTransEntrySeqId", entry.getAcctgTransEntrySeqId());
            return results;
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
    }

    /**
     * Same as the <code>updateAcctgTransEntry</code> service but add validation of accounting tags.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     * @throws GenericServiceException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public static Map updateAcctgTransEntryManual(DispatchContext dctx, Map context) throws GenericServiceException {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = (Locale) context.get("locale");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        try {
            LedgerRepositoryInterface ledgerRepository = new DomainsLoader(new Infrastructure(dispatcher), new User(userLogin)).loadDomainsDirectory().getLedgerDomain().getLedgerRepository();
            AcctgTransEntry pk = new AcctgTransEntry();
            pk.setPKFields(context);
            AcctgTransEntry entry = ledgerRepository.findOneNotNull(AcctgTransEntry.class, ledgerRepository.map(AcctgTransEntry.Fields.acctgTransId, pk.getAcctgTransId(), AcctgTransEntry.Fields.acctgTransEntrySeqId, pk.getAcctgTransEntrySeqId()));
            entry.setNonPKFields(context);

            // validate the accounting tags
            List<AccountingTagConfigurationForOrganizationAndUsage> missings = ledgerRepository.validateTagParameters(entry);
            if (!missings.isEmpty()) {
                return UtilMessage.createAndLogServiceError("OpentapsError_ServiceErrorRequiredTagNotFound", UtilMisc.toMap("tagName", missings.get(0).getDescription()), locale, MODULE);
            }

            ledgerRepository.update(entry);
            return ServiceUtil.returnSuccess();
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
    }
}
