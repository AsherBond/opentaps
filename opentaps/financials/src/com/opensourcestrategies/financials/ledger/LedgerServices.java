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

package com.opensourcestrategies.financials.ledger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.opensourcestrategies.financials.payroll.PaycheckReader;
import com.opensourcestrategies.financials.util.UtilCOGS;
import com.opensourcestrategies.financials.util.UtilFinancial;
import javolution.util.FastList;
import javolution.util.FastSet;

import org.apache.batik.dom.GenericEntity;
import org.ofbiz.accounting.AccountingException;
import org.ofbiz.accounting.invoice.InvoiceWorker;
import org.ofbiz.accounting.util.UtilAccounting;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
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
import org.ofbiz.workeffort.workeffort.WorkEffortSearch;
import org.opentaps.domain.manufacturing.OpentapsProductionRun;
import org.opentaps.common.party.PartyHelper;
import org.opentaps.common.product.UtilProduct;
import org.opentaps.common.util.UtilAccountingTags;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.base.entities.AcctgTransEntry;
import org.opentaps.base.entities.InvoiceAdjustment;
import org.opentaps.base.entities.PaymentApplication;
import org.opentaps.base.services.GetTrialBalanceForDateService;
import org.opentaps.domain.billing.invoice.Invoice;
import org.opentaps.domain.billing.invoice.InvoiceRepositoryInterface;
import org.opentaps.domain.billing.payment.Payment;
import org.opentaps.domain.billing.payment.PaymentRepositoryInterface;
import org.opentaps.domain.inventory.InventoryItem;
import org.opentaps.domain.inventory.InventoryRepositoryInterface;
import org.opentaps.domain.ledger.InvoiceLedgerServiceInterface;
import org.opentaps.domain.organization.Organization;
import org.opentaps.domain.organization.OrganizationRepositoryInterface;
import org.opentaps.domain.product.Product;
import org.opentaps.domain.product.ProductRepositoryInterface;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.ofbiz.Repository;
import org.opentaps.foundation.service.ServiceException;

/**
 * LedgerServices - Services for posting transactions to the general ledger.
 *
 * @author     <a href="mailto:sichen@opensourcestrategies.com">Si Chen</a>
 * @version    $Rev$
 * @since      2.2
 */
public final class LedgerServices {

    private LedgerServices() { }

    private static final String MODULE = LedgerServices.class.getName();
    public static final String INVOICE_PRODUCT_ITEM_TYPE = "INV_FPROD_ITEM";    // invoiceTypeId for invoice items which are products
    public static final String PURCHINV_PRODUCT_ITEM_TYPE = "PINV_FPROD_ITEM";    // invoiceTypeId for invoice items which are products
    public static final String RETINV_PRODUCT_ITEM_TYPE = "RINV_FPROD_ITEM";    // invoiceTypeId for invoice items which are products
    public static final String PURCHNV_SUPPLIES_ITEM_TYPE = "PINV_SUPLPRD_ITEM";   // supplies on purchase invoices

    // TODO: replace code that uses epsilon with BigDecimal and also set BigDecimal config in some common class/properties file
    private static final BigDecimal EPSILON = new BigDecimal("0.000001");   // smallest allowable rounding error in accounting transactions

    private static BigDecimal ZERO = BigDecimal.ZERO;
    private static int decimals = -1;
    private static int rounding = -1;
    static {
        decimals = UtilNumber.getBigDecimalScale("invoice.decimals");
        rounding = UtilNumber.getBigDecimalRoundingMode("invoice.rounding");
        // set zero to the proper scale
        ZERO.setScale(decimals);
    }

    /**
     * Posts invoices to General Ledger, using the createAcctgTransAndEntries service from above.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map postInvoiceToGl(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String invoiceId = (String) context.get("invoiceId");
        try {

            // get the invoice and make sure it has an invoiceTypeId
            GenericValue invoice = delegator.findByPrimaryKeyCache("Invoice", UtilMisc.toMap("invoiceId", invoiceId));
            if (invoice == null) {
                return ServiceUtil.returnError("No invoice found for invoiceId of " + invoiceId);
            } else if (invoice.get("invoiceTypeId") == null) {
                return ServiceUtil.returnError("Invoice " + invoiceId + " has a null invoice type and cannot be processed");
            }
            String invoiceTypeId = (String) invoice.get("invoiceTypeId");

            // this domain driven architecture code is used only for posting invoice adjustments right now, but later we'll refactor rest of this method
            DomainsLoader dl = new DomainsLoader(new Infrastructure(dispatcher), new User(userLogin));
            InvoiceRepositoryInterface invoiceRepository = dl.loadDomainsDirectory().getBillingDomain().getInvoiceRepository();
            Invoice invoiceObj = invoiceRepository.getInvoiceById(invoiceId);

            // accounting data
            String transactionPartyId = null;

            String transactionPartyRoleTypeId = null;
            String acctgTransTypeId = null;
            String organizationPartyId = null;
            String offsettingGlAccountTypeId = null;
            String defaultDebitCreditFlag = null;
            String offsettingDebitCreditFlag = null;
            String defaultGlAccountTypeId = null;

            // set accounting data according to invoiceTypeId
            if ("SALES_INVOICE".equals(invoiceTypeId)) {
                acctgTransTypeId = "INVOICE_SALES_ATX";
                offsettingGlAccountTypeId = "ACCOUNTS_RECEIVABLE";
                organizationPartyId = invoice.getString("partyIdFrom");
                transactionPartyId = invoice.getString("partyId");
                transactionPartyRoleTypeId = "BILL_TO_CUSTOMER";
                defaultDebitCreditFlag = "C";
                offsettingDebitCreditFlag = "D";
                defaultGlAccountTypeId = "SALES_ACCOUNT";
            } else if ("PURCHASE_INVOICE".equals(invoiceTypeId)) {
                acctgTransTypeId = "INVOICE_PURCH_ATX";
                offsettingGlAccountTypeId = "ACCOUNTS_PAYABLE";
                organizationPartyId = invoice.getString("partyId");
                transactionPartyId = invoice.getString("partyIdFrom");
                transactionPartyRoleTypeId = "BILL_FROM_VENDOR";
                defaultDebitCreditFlag = "D";
                offsettingDebitCreditFlag = "C";
                defaultGlAccountTypeId = "UNINVOICED_SHIP_RCPT";
            } else if ("CUST_RTN_INVOICE".equals(invoiceTypeId)) {
                acctgTransTypeId = "INVOICE_CREDIT_ATX";
                offsettingGlAccountTypeId = "CUSTOMER_CREDIT";
                organizationPartyId = invoice.getString("partyId");
                transactionPartyId = invoice.getString("partyIdFrom");
                transactionPartyRoleTypeId = "BILL_TO_CUSTOMER";
                defaultDebitCreditFlag = "D";
                offsettingDebitCreditFlag = "C";
                defaultGlAccountTypeId = "SALES_RETURNS";
            } else if ("COMMISSION_INVOICE".equals(invoiceTypeId)) {
                acctgTransTypeId = "INVOICE_COMM_ATX";
                offsettingGlAccountTypeId = "COMMISSIONS_PAYABLE";
                organizationPartyId = invoice.getString("partyId");
                transactionPartyId = invoice.getString("partyIdFrom");
                transactionPartyRoleTypeId = "SALES_REP";
                defaultDebitCreditFlag = "D";
                offsettingDebitCreditFlag = "C";
                defaultGlAccountTypeId = "COMMISSION_EXPENSE";
            } else if ("INTEREST_INVOICE".equals(invoiceTypeId)) {
                acctgTransTypeId = "INVOICE_INTRST_ATX";
                offsettingGlAccountTypeId = "INTRSTINC_RECEIVABLE";
                organizationPartyId = invoice.getString("partyIdFrom");
                transactionPartyId = invoice.getString("partyId");
                transactionPartyRoleTypeId = "BILL_TO_CUSTOMER";
                defaultDebitCreditFlag = "C";
                offsettingDebitCreditFlag = "D";
                defaultGlAccountTypeId = "INTEREST_INCOME";
            } else {
                // return failure will return the error message but not rollback the transaction
                Debug.logWarning("Invoice [" + invoiceId + "] has an unsupported invoice type of [" + invoiceTypeId + "] and was not posted to the ledger", MODULE);
                return ServiceUtil.returnFailure();
            }

            // invoke helper method to process invoice items
            Map tmpResult = processInvoiceItems(
                    delegator, dispatcher, userLogin, invoice,
                    acctgTransTypeId, offsettingGlAccountTypeId,
                    organizationPartyId, transactionPartyId, transactionPartyRoleTypeId,
                    defaultDebitCreditFlag, defaultGlAccountTypeId
                    );

            // return on any service errors
            if (ServiceUtil.isError(tmpResult)) {
                return tmpResult;
            }

            // create a list of AcctgTransEntries based on processInvoiceItems
            int itemSeqId = 1;
            List<GenericValue> acctgTransEntries = new ArrayList<GenericValue>();
            List<Map> acctgTransEntryMaps = (List<Map>) tmpResult.get("acctgTransEntries");
            for (Iterator<Map> iter = acctgTransEntryMaps.iterator(); iter.hasNext();) {
                Map input = iter.next();

                // skip those with amount 0
                if (((BigDecimal) input.get("amount")).signum() == 0) {
                    continue;
                }

                // make the next transaction entry
                input.put("acctgTransEntrySeqId", Integer.toString(itemSeqId));
                acctgTransEntries.add(delegator.makeValue("AcctgTransEntry", input));
                itemSeqId++;

                if (Debug.verboseOn()) {
                    Debug.logVerbose(acctgTransEntries.get(acctgTransEntries.size() - 1).toString(), MODULE);
                }
            }

            // create the offsetting GL transaction entries and add to list
            // this needs to broken out by the accounting tags

            // first, get the list of eligible tags, which should be keyed by offsettingGlAccountTypeId by organization
            OrganizationRepositoryInterface orgRepo = dl.loadDomainsDirectory().getOrganizationDomain().getOrganizationRepository();
            Organization organization = orgRepo.getOrganizationById(organizationPartyId);
            Map<Integer, String> accountingTags = organization.getAccountingTagTypes(offsettingGlAccountTypeId);
            Set<Integer> validTagEnumIds = accountingTags.keySet();  // this is the set 1,2,3,4 of the accountingTagEnumIds

            // now, go through the list of AcctgTransEntry for this Invoice and group them by the eligible tags
            // so we want a Map of Map -> amount, such as:
            //  (acctgTagEnumId1-> Consumer) -> 1000
            //  (acctgTagEnumId1-> Government) -> 1500
            //  (acctgTagEnumId1-> Enterprise) -> 7500
            Map<Map, BigDecimal> amountsByTagKey = new HashMap();
            BigDecimal totalAmount = BigDecimal.ZERO;
            // iterate even if validTagEnumIds set is null, so we can build get the totalAmount and create one offsetting AcctgTransEntry -- see below
            for (GenericValue entry : acctgTransEntries) {
                // build a tags key of all the eligible tags for this glAccountTypeId from this AcctgTransEntry
                Map tagsKey = new HashMap();
                for (Integer validTagEnumId : validTagEnumIds) {
                    tagsKey.put(UtilAccountingTags.ENTITY_TAG_PREFIX + validTagEnumId, entry.get(UtilAccountingTags.ENTITY_TAG_PREFIX + validTagEnumId));
                }
                UtilCommon.addInMapOfBigDecimal(amountsByTagKey, tagsKey, entry.getBigDecimal("amount"));     // add each entry's amount to its tags key
                totalAmount = totalAmount.add(entry.getBigDecimal("amount")).setScale(decimals, rounding);  // keep a running total
            }

            // create the template AcctgTransEntry
            Map<String, Object> input = new HashMap<String, Object>();
            input.put("glAccountId", tmpResult.get("offsettingGlAccountId"));
            input.put("organizationPartyId", organizationPartyId);
            input.put("partyId", transactionPartyId);
            input.put("roleTypeId", transactionPartyRoleTypeId);
            input.put("debitCreditFlag", offsettingDebitCreditFlag);

            input.put("acctgTransEntryTypeId", "_NA_");

            // if there were no tags, then put an empty Map, so at least one offsetting AcctgTransEntry is created
            if (UtilValidate.isEmpty(amountsByTagKey)) {
                input.put("acctgTransEntrySeqId", Integer.toString(itemSeqId));
                input.put("amount", tmpResult.get("postingTotal"));    // here we can post the entire posting amount in one offsetting entry
                GenericValue offsettingAcctgTransEntry = delegator.makeValue("AcctgTransEntry", input);
                acctgTransEntries.add(offsettingAcctgTransEntry);
            } else {
                // otherwise, loop through them and create a new offsetting AcctgTransEntry for each tags combination
                for (Map acctgTagKey : amountsByTagKey.keySet()) {
                    input.put("acctgTransEntrySeqId", Integer.toString(itemSeqId));
                    input.put("amount", amountsByTagKey.get(acctgTagKey));  // here we must close the amount for each tag key
                    input.putAll(acctgTagKey);
                    GenericValue offsettingAcctgTransEntry = delegator.makeValue("AcctgTransEntry", input);
                    acctgTransEntries.add(offsettingAcctgTransEntry);
                    itemSeqId++;        // keep the itemSeqId increasing
                }
            }

            // now use createAcctgTransAndEntries to create an accounting transaction for the invoice
            input = new HashMap<String, Object>();
            input.put("acctgTransEntries", acctgTransEntries);
            input.put("invoiceId", invoiceId);
            input.put("partyId", transactionPartyId);
            input.put("roleTypeId", transactionPartyRoleTypeId);
            input.put("glFiscalTypeId", "ACTUAL");
            input.put("acctgTransTypeId", acctgTransTypeId);
            input.put("userLogin", userLogin);
            // set the transactionDate based on invoiceDate if it's available
            if (invoice.get("invoiceDate") != null) {
                input.put("transactionDate", invoice.get("invoiceDate"));
            } else {
                Debug.logWarning("No invoice date for invoice [" + invoiceId + "].  It will be posted with transaction date of now", MODULE);
                input.put("transactionDate", UtilDateTime.nowTimestamp());
            }

            tmpResult = dispatcher.runSync("createAcctgTransAndEntries", input);

            // check if there are any adjustments and post them too
            List<InvoiceAdjustment> invoiceAdjustments = invoiceRepository.getAdjustmentsApplied(invoiceObj, UtilDateTime.nowTimestamp());
            for (InvoiceAdjustment adjustment : invoiceAdjustments) {
                InvoiceLedgerServiceInterface invoiceLedgerService = dl.loadDomainsDirectory().getLedgerDomain().getInvoiceLedgerService();
                invoiceLedgerService.setInvoiceAdjustmentId(adjustment.getInvoiceAdjustmentId());
                invoiceLedgerService.postInvoiceAdjustmentToLedger();
            }

            // on success, return the acctgTransId
            if (((String) tmpResult.get(ModelService.RESPONSE_MESSAGE)).equals(ModelService.RESPOND_SUCCESS)) {
                Map result = ServiceUtil.returnSuccess();
                result.put("acctgTransId", tmpResult.get("acctgTransId"));
                return result;
            }

            // otherwise we have an error, which we pass up
            return tmpResult;
        } catch (GenericEntityException ee) {
            return ServiceUtil.returnError(ee.getMessage());
        } catch (GenericServiceException se) {
            return ServiceUtil.returnError(se.getMessage());
        } catch (RepositoryException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (EntityNotFoundException e) {
            return ServiceUtil.returnError(e.getMessage());
        } catch (ServiceException e) {
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    /**
     * Iterates through each InvoiceItem in an Invoice and creates a map based on
     * AcctgTransEntry for each invoice item. Also determines the offsettingGlAccountId
     * and keeps a running total for all the transactions. The general code for processing
     * all invoice types is here, and special processing by invoice type for each InvoiceItem
     * is handled through specialized methods. The result is a map that includes the
     * offsettingGlAccountId, the postingTotal, and a list of transaction entry Maps that
     * are ready to be turned into GenericValues.
     *
     * @param delegator a <code>Delegator</code> value
     * @param dispatcher a <code>LocalDispatcher</code> value
     * @param userLogin a <code>GenericValue</code> value
     * @param invoice a <code>GenericValue</code> value
     * @param acctgTransTypeId a <code>String</code> value
     * @param offsettingGlAccountTypeId a <code>String</code> value
     * @param organizationPartyId a <code>String</code> value
     * @param transactionPartyId a <code>String</code> value
     * @param transactionPartyRoleTypeId a <code>String</code> value
     * @param defaultDebitCreditFlag a <code>String</code> value
     * @param defaultGlAccountTypeId a <code>String</code> value
     * @return a <code>Map</code> that includes the offsettingGlAccountId, the postingTotal, and a list of transaction entry Maps that are ready to be turned into GenericValues
     * @exception GenericEntityException if an error occurs
     * @exception GenericServiceException if an error occurs
     */
    @SuppressWarnings("unchecked")
    private static Map processInvoiceItems(
            Delegator delegator, LocalDispatcher dispatcher, GenericValue userLogin,
            GenericValue invoice, String acctgTransTypeId, String offsettingGlAccountTypeId,
            String organizationPartyId, String transactionPartyId, String transactionPartyRoleTypeId,
            String defaultDebitCreditFlag, String defaultGlAccountTypeId
            )
        throws GenericEntityException, GenericServiceException {

        // get the offsetting gl account for this invoice
        String offsettingGlAccountId = UtilAccounting.getDefaultAccountId(offsettingGlAccountTypeId, organizationPartyId, delegator);
        Debug.logInfo("Posting to GL for party " + organizationPartyId + " and offsetting account " + offsettingGlAccountId, MODULE);

        // get the conversion factor for converting the invoice's amounts to the currencyUomId of the organization
        BigDecimal conversionFactor = UtilFinancial.determineUomConversionFactor(delegator, dispatcher, organizationPartyId, invoice.getString("currencyUomId"));

        // loop variables
        List acctgTransEntries = new ArrayList();    // a List of AcctgTransEntry entities to be posted to
        BigDecimal postingTotal = BigDecimal.ZERO;
        List invoiceItems = invoice.getRelatedCache("InvoiceItem", UtilMisc.toList("invoiceItemSeqId"));

        // Now, for each line item of the invoice, figure out which account on GL to post to and add it to a List
        Iterator invoiceItemIterator = invoiceItems.iterator();
        while (invoiceItemIterator.hasNext()) {
            GenericValue invoiceItem = (GenericValue) invoiceItemIterator.next();

            // default null quantities to 1.0
            if (invoiceItem.get("quantity") == null) {
                invoiceItem.set("quantity", BigDecimal.ONE);
            }

            BigDecimal amount = invoiceItem.getBigDecimal("amount");
            BigDecimal quantity = invoiceItem.getBigDecimal("quantity");

            // do not post invoice items with net zero value (usually because there's no amount)
            if ((amount == null) || (amount.signum() == 0) || (quantity.signum() == 0)) {
                continue;
            }

            // GL account for posting this invoice item, default to overrideGlAccountId if present
            String invoiceItemPostingGlAccountId = invoiceItem.getString("overrideGlAccountId");

            if (invoiceItemPostingGlAccountId == null) {

                // if this invoice item is for a product, check to see if it has a particular GL account defined in ProductGlAccount,
                // or, alternatively, in GlAccountTypeDefault
                String invoiceItemTypeId = invoiceItem.getString("invoiceItemTypeId");
                if (INVOICE_PRODUCT_ITEM_TYPE.equals(invoiceItemTypeId)
                    || PURCHINV_PRODUCT_ITEM_TYPE.equals(invoiceItemTypeId)
                    || RETINV_PRODUCT_ITEM_TYPE.equals(invoiceItemTypeId)) {
                    invoiceItemPostingGlAccountId = UtilAccounting.getProductOrgGlAccountId(invoiceItem.getString("productId"), defaultGlAccountTypeId, organizationPartyId, delegator);
                } else if (PURCHNV_SUPPLIES_ITEM_TYPE.equals(invoiceItemTypeId)) {
                    // for supplies items on purchase invoices, see if the product has a special EXPENSE account defined
                    GenericValue productGlAccount = delegator.findByPrimaryKeyCache("ProductGlAccount", UtilMisc.toMap("productId", invoiceItem.getString("productId"),
                        "organizationPartyId", organizationPartyId, "glAccountTypeId", "EXPENSE"));
                    if (UtilValidate.isNotEmpty(productGlAccount)) {
                        invoiceItemPostingGlAccountId = productGlAccount.getString("glAccountId");
                    }
                }
            }

            // specific invoice type processing for each invoice item
            // note that these methods should add acctgTransEntries and return the postingTotal in a Map
            Map tmpResult = null;
            if ("SALES_INVOICE".equals(invoice.getString("invoiceTypeId")) || "INTEREST_INVOICE".equals(invoice.getString("invoiceTypeId"))) {
                tmpResult = processSalesInvoiceItem(
                        delegator, dispatcher, userLogin,
                        invoice, invoiceItem, acctgTransEntries,
                        postingTotal, conversionFactor, amount, quantity,
                        invoiceItemPostingGlAccountId, acctgTransTypeId, offsettingGlAccountTypeId,
                        organizationPartyId, transactionPartyId, transactionPartyRoleTypeId,
                        defaultDebitCreditFlag, defaultGlAccountTypeId
                        );
            } else if ("COMMISSION_INVOICE".equals(invoice.getString("invoiceTypeId"))) {
                tmpResult = processCommissionInvoiceItem(
                        delegator, dispatcher, userLogin,
                        invoice, invoiceItem, acctgTransEntries,
                        postingTotal, conversionFactor, amount, quantity,
                        invoiceItemPostingGlAccountId, acctgTransTypeId, offsettingGlAccountTypeId,
                        organizationPartyId, transactionPartyId, transactionPartyRoleTypeId,
                        defaultDebitCreditFlag, defaultGlAccountTypeId
                        );

            } else { // for both purchase and return invoices, run processPurchaseInvoiceItem
                tmpResult = processPurchaseInvoiceItem(
                        delegator, dispatcher, userLogin,
                        invoice, invoiceItem, acctgTransEntries,
                        postingTotal, conversionFactor, amount, quantity,
                        invoiceItemPostingGlAccountId, acctgTransTypeId, offsettingGlAccountTypeId,
                        organizationPartyId, transactionPartyId, transactionPartyRoleTypeId,
                        defaultDebitCreditFlag, defaultGlAccountTypeId
                        );
            }
            if (ServiceUtil.isError(tmpResult)) {
                return tmpResult;
            }

            if (Debug.verboseOn()) {
                Debug.logVerbose("invoiceItem " + invoiceItem.getString("invoiceId") + ", " + invoiceItem.getString("invoiceItemSeqId") + ": gl account = "
                        + invoiceItemPostingGlAccountId + ", amount = " + invoiceItem.getBigDecimal("amount") + ", quantity = "
                        + invoiceItem.getBigDecimal("quantity") + ", default debit/credit flag = " + defaultDebitCreditFlag, MODULE);
            }
            postingTotal = (BigDecimal) tmpResult.get("postingTotal");
        }
        return UtilMisc.toMap("offsettingGlAccountId", offsettingGlAccountId, "acctgTransEntries", acctgTransEntries, "postingTotal", postingTotal);
    }

    /**
     * For purchase invoices, this method creates accounting transaction entries first
     * for uninvoiced shipment receipts.  Then it reconciles the aggregate value of all order items
     * related to the invoice item with the actual value of the invoice item and posts it as a
     * purchase price variance. Note that this method is also used for return invoices.
     *
     * @param delegator a <code>Delegator</code> value
     * @param dispatcher a <code>LocalDispatcher</code> value
     * @param userLogin a <code>GenericValue</code> value
     * @param invoice a <code>GenericValue</code> value
     * @param invoiceItem a <code>GenericValue</code> value
     * @param acctgTransEntries a <code>List</code> value
     * @param postingTotal a <code>BigDecimal</code> value
     * @param conversionFactor a <code>BigDecimal</code> value
     * @param amount a <code>BigDecimal</code> value
     * @param quantity a <code>BigDecimal</code> value
     * @param invoiceItemPostingGlAccountId a <code>String</code> value
     * @param acctgTransTypeId a <code>String</code> value
     * @param offsettingGlAccountTypeId a <code>String</code> value
     * @param organizationPartyId a <code>String</code> value
     * @param transactionPartyId a <code>String</code> value
     * @param transactionPartyRoleTypeId a <code>String</code> value
     * @param defaultDebitCreditFlag a <code>String</code> value
     * @param defaultGlAccountTypeId a <code>String</code> value
     * @return a <code>Map</code> value
     * @exception GenericServiceException if an error occurs
     * @exception GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    private static Map processPurchaseInvoiceItem(
            Delegator delegator, LocalDispatcher dispatcher, GenericValue userLogin,
            GenericValue invoice, GenericValue invoiceItem, List acctgTransEntries,
            BigDecimal postingTotal, BigDecimal conversionFactor, BigDecimal amount, BigDecimal quantity,
            String invoiceItemPostingGlAccountId, String acctgTransTypeId, String offsettingGlAccountTypeId,
            String organizationPartyId, String transactionPartyId, String transactionPartyRoleTypeId,
            String defaultDebitCreditFlag, String defaultGlAccountTypeId
            ) throws GenericServiceException, GenericEntityException {

        int decimals = UtilNumber.getBigDecimalScale("invoice.decimals");
        int rounding = UtilNumber.getBigDecimalRoundingMode("invoice.rounding");

        // First, determine the amount to credit and update postingTotal
        BigDecimal postingAmount = conversionFactor.multiply(amount).multiply(quantity).setScale(decimals, rounding);
        postingTotal = postingTotal.add(postingAmount);

        // Next, determine the amount which has already been debit to uninvoiced receipts, so we can reconcile the difference
        // between invoice amounts and original order amounts in Uninvoiced Item Receipt
        BigDecimal orderAmount = BigDecimal.ZERO;
        List orderItemBillings = invoiceItem.getRelated("OrderItemBilling");
        for (Iterator iter = orderItemBillings.iterator(); iter.hasNext();) {
            GenericValue orderItemBilling = (GenericValue) iter.next();
            GenericValue orderItem = orderItemBilling.getRelatedOne("OrderItem");
            orderAmount = orderAmount.add(conversionFactor
                    .multiply(orderItem.getBigDecimal("unitPrice"))
                    .multiply(orderItemBilling.getBigDecimal("quantity")) // note that we use the billing quantity, because an order item with 10 serialized products is mapped onto 10 order item billings
                    .setScale(decimals, rounding));
        }

        // If invoice was generated without an associated order, then simply use the postingAmount as the orderAmount. (There will be no variance.)
        // Note that this logic requires that OrderItemBilling absolutely be generated for purchase orders created in the system.
        if (orderItemBillings.size() == 0) {
            orderAmount = postingAmount;
        }

        // try getting the default GL account for the invoice item
        if (UtilValidate.isEmpty(invoiceItemPostingGlAccountId)) {
            invoiceItemPostingGlAccountId = getDefaultGlAccount(invoiceItem, organizationPartyId);
        }

        // if still no GL account, then it is an error
        if (UtilValidate.isEmpty(invoiceItemPostingGlAccountId)) {
            return ServiceUtil.returnError("Cannot find posting GL account for invoice " + invoiceItem.get("invoiceId") + " item " + invoiceItem.get("invoiceItemSeqId"));
        }

        // default variance is either postingAmount - orderAmount or postingAmount - standardCost
        BigDecimal varianceAmount = BigDecimal.ZERO;

        // create the transaction entry for uninvoiced receipts or the default account
        // the amount is the order item amount or standard cost if the organization uses standard costing
        Map<String, Object> acctgTransEntry = new HashMap<String, Object>();
        acctgTransEntry.put("glAccountId", invoiceItemPostingGlAccountId);
        acctgTransEntry.put("debitCreditFlag", defaultDebitCreditFlag);
        acctgTransEntry.put("organizationPartyId", organizationPartyId);
        acctgTransEntry.put("partyId", transactionPartyId);
        acctgTransEntry.put("roleTypeId", transactionPartyRoleTypeId);
        acctgTransEntry.put("acctgTransEntryTypeId", "_NA_");
        acctgTransEntry.put("productId", invoiceItem.getString("productId"));
        UtilAccountingTags.putAllAccountingTags(invoiceItem, acctgTransEntry);   // for all invoice items, put all the accounting tags from InvoiceItem to AcctgTransEntry

        // if the organization uses standard costing this will be postingAmount - standard cost
        try {
            DomainsLoader dl = new DomainsLoader(new Infrastructure(dispatcher), new User(userLogin));
            OrganizationRepositoryInterface organizationRepository = dl.loadDomainsDirectory().getOrganizationDomain().getOrganizationRepository();
            ProductRepositoryInterface productRepository = dl.loadDomainsDirectory().getProductDomain().getProductRepository();
            Organization organization = organizationRepository.getOrganizationById(organizationPartyId);
            String productId = invoiceItem.getString("productId");

            // if the invoice item is for a purchased product and has a productId, and the organization uses standard costing,
            // then check whether purchase price variance should be vs. standard cost or purchase order cost
            if ((PURCHINV_PRODUCT_ITEM_TYPE.equals(invoiceItem.getString("invoiceItemTypeId"))) && (productId != null) && (organization.usesStandardCosting())) {
                Product product = productRepository.getProductById(productId);
                BigDecimal stdCost = product.getStandardCost(organization.getPartyAcctgPreference().getBaseCurrencyUomId()).multiply(quantity).setScale(decimals, rounding);

                acctgTransEntry.put("amount", stdCost);
                varianceAmount = postingAmount.subtract(stdCost);
            // otherwise, if the invoice item is related to an order, then calculate a variance against the order item
            } else if (orderItemBillings.size() > 0) {
                acctgTransEntry.put("amount", orderAmount);
                varianceAmount = postingAmount.subtract(orderAmount);
            } else {
                // this is the default, no variance and make the transaction with postingAmount
                acctgTransEntry.put("amount", postingAmount);
            }
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }

        // adding the transaction entry for uninvoiced receipts or the default account
        acctgTransEntries.add(acctgTransEntry);

        Debug.logInfo("Purchase InvoiceItem: orderAmount [" + orderAmount + "], postingAmount [" + postingAmount + "], varianceAmount[" + varianceAmount + "]", MODULE);

        // if there's a variance
        if (varianceAmount.signum() != 0) {

            // get the inventory price variance gl account
            String varianceGlAccountId = UtilAccounting.getDefaultAccountId("PURCHASE_PRICE_VAR", organizationPartyId, delegator);

            // and debit the variance account
            acctgTransEntry = new HashMap<String, Object>(acctgTransEntry);
            acctgTransEntry.put("glAccountId", varianceGlAccountId);
            acctgTransEntry.put("amount", varianceAmount);
            UtilAccountingTags.putAllAccountingTags(invoiceItem, acctgTransEntry);   // use the same accounting tags on the variance transaction entry as on the main invoice item
            acctgTransEntries.add(acctgTransEntry);
        }

        return UtilMisc.toMap("postingTotal", postingTotal);
    }

    /**
     * For sales invoices, if an invoice item is a product, then we will need to post COGS and INVENTORY as well.
     * TODO: maybe do COGS/INVENTORY posting for other invoiceItemTypeId (easier now with this refactored method)
     *
     * @param delegator a <code>Delegator</code> value
     * @param dispatcher a <code>LocalDispatcher</code> value
     * @param userLogin a <code>GenericValue</code> value
     * @param invoice a <code>GenericValue</code> value
     * @param invoiceItem a <code>GenericValue</code> value
     * @param acctgTransEntries a <code>List</code> value
     * @param postingTotal a <code>BigDecimal</code> value
     * @param conversionFactor a <code>BigDecimal</code> value
     * @param amount a <code>BigDecimal</code> value
     * @param quantity a <code>BigDecimal</code> value
     * @param invoiceItemPostingGlAccountId a <code>String</code> value
     * @param acctgTransTypeId a <code>String</code> value
     * @param offsettingGlAccountTypeId a <code>String</code> value
     * @param organizationPartyId a <code>String</code> value
     * @param transactionPartyId a <code>String</code> value
     * @param transactionPartyRoleTypeId a <code>String</code> value
     * @param defaultDebitCreditFlag a <code>String</code> value
     * @param defaultGlAccountTypeId a <code>String</code> value
     * @return a <code>Map</code> value
     * @exception GenericServiceException if an error occurs
     * @exception GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    private static Map processSalesInvoiceItem(
            Delegator delegator, LocalDispatcher dispatcher, GenericValue userLogin,
            GenericValue invoice, GenericValue invoiceItem, List acctgTransEntries,
            BigDecimal postingTotal, BigDecimal conversionFactor, BigDecimal amount, BigDecimal quantity,
            String invoiceItemPostingGlAccountId, String acctgTransTypeId, String offsettingGlAccountTypeId,
            String organizationPartyId, String transactionPartyId, String transactionPartyRoleTypeId,
            String defaultDebitCreditFlag, String defaultGlAccountTypeId
            ) throws GenericServiceException, GenericEntityException {

        int decimals = UtilNumber.getBigDecimalScale("invoice.decimals");
        int rounding = UtilNumber.getBigDecimalRoundingMode("invoice.rounding");

        // try getting the default GL account for the invoice item
        if (UtilValidate.isEmpty(invoiceItemPostingGlAccountId)) {
            invoiceItemPostingGlAccountId = getDefaultGlAccount(invoiceItem, organizationPartyId);
        }

        // If there is still no account, try getting a default account
        if ((invoiceItemPostingGlAccountId == null) || invoiceItemPostingGlAccountId.equals("")) {
            invoiceItemPostingGlAccountId = getDefaultGlAccount(invoiceItem, organizationPartyId);
        }

        // if still no GL account, then it is an error
        if ((invoiceItemPostingGlAccountId == null) || (invoiceItemPostingGlAccountId.equals(""))) {
            Debug.logError("Canot find GL account to post for this invoice item " + invoiceItem, MODULE);
            return ServiceUtil.returnError("Cannot find posting GL account for invoice " + invoiceItem.getString("invoiceId")
                    + ", item " + invoiceItem.getString("invoiceItemSeqId"));
        }

        // prepare a transaction entry
        // TODO: if necessary, process theirPartyId, origAmount, dueDate, groupId, description, voucherRef fields of AcctgTransEntry
        Map acctgTransEntry = new HashMap();
        acctgTransEntry.put("glAccountId", invoiceItemPostingGlAccountId);
        acctgTransEntry.put("debitCreditFlag", defaultDebitCreditFlag);
        acctgTransEntry.put("organizationPartyId", organizationPartyId);
        acctgTransEntry.put("acctgTransEntryTypeId", "_NA_");
        acctgTransEntry.put("productId", invoiceItem.getString("productId"));

        // amount to be posted. NOTE: this conversion is rounded AFTER (conversion*amount*quantity)
        BigDecimal postingAmount = amount.multiply(conversionFactor).multiply(quantity).setScale(decimals, rounding);
        acctgTransEntry.put("amount", postingAmount);

        // if there is a taxAuthPartyId on the invoice item, then this invoice item is a tax transaction.
        if (invoiceItem.getString("taxAuthPartyId") != null) {
            // In that case, the partyId on the accounting trans entry should be the the taxAuthPartyId on the invoice item
            acctgTransEntry.put("partyId", invoiceItem.getString("taxAuthPartyId"));
            acctgTransEntry.put("roleTypeId", "TAX_AUTHORITY");
        } else {
            // TODO: have to determine role here
            acctgTransEntry.put("partyId", transactionPartyId);
            acctgTransEntry.put("roleTypeId", transactionPartyRoleTypeId);
        }
        UtilAccountingTags.putAllAccountingTags(invoiceItem, acctgTransEntry);   // for all invoice items, put all the accounting tags from InvoiceItem to AcctgTransEntry

        // update loop variables
        acctgTransEntries.add(acctgTransEntry);
        postingTotal = postingTotal.add(postingAmount); // this preserves the postingAmount scale (addition => scale = max(scale1, scale2))

        // next, if an invoice item is a product, then we will need to post COGS and INVENTORY as well.

        // Must check invoiceItemTypeId or you'd end up posting to COGS for adjustment entries, sales tax, etc.
        if (!(invoiceItem.getString("invoiceItemTypeId").equals("INV_FPROD_ITEM") && (invoiceItem.getString("productId") != null))) {
            return UtilMisc.toMap("postingTotal", postingTotal);
        }

        return UtilMisc.toMap("postingTotal", postingTotal);
    }

    /**
     * Process commission invoice items.  Eventually this should handle incomming commission invoices as well.
     *
     * @param delegator a <code>Delegator</code> value
     * @param dispatcher a <code>LocalDispatcher</code> value
     * @param userLogin a <code>GenericValue</code> value
     * @param invoice a <code>GenericValue</code> value
     * @param invoiceItem a <code>GenericValue</code> value
     * @param acctgTransEntries a <code>List</code> value
     * @param postingTotal a <code>BigDecimal</code> value
     * @param conversionFactor a <code>BigDecimal</code> value
     * @param amount a <code>BigDecimal</code> value
     * @param quantity a <code>BigDecimal</code> value
     * @param invoiceItemPostingGlAccountId a <code>String</code> value
     * @param acctgTransTypeId a <code>String</code> value
     * @param offsettingGlAccountTypeId a <code>String</code> value
     * @param organizationPartyId a <code>String</code> value
     * @param transactionPartyId a <code>String</code> value
     * @param transactionPartyRoleTypeId a <code>String</code> value
     * @param defaultDebitCreditFlag a <code>String</code> value
     * @param defaultGlAccountTypeId a <code>String</code> value
     * @return a <code>Map</code> value
     * @exception GenericServiceException if an error occurs
     * @exception GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    private static Map processCommissionInvoiceItem(
            Delegator delegator, LocalDispatcher dispatcher, GenericValue userLogin,
            GenericValue invoice, GenericValue invoiceItem, List acctgTransEntries,
            BigDecimal postingTotal, BigDecimal conversionFactor, BigDecimal amount, BigDecimal quantity,
            String invoiceItemPostingGlAccountId, String acctgTransTypeId, String offsettingGlAccountTypeId,
            String organizationPartyId, String transactionPartyId, String transactionPartyRoleTypeId,
            String defaultDebitCreditFlag, String defaultGlAccountTypeId
            ) throws GenericServiceException, GenericEntityException {

        int decimals = UtilNumber.getBigDecimalScale("invoice.decimals");
        int rounding = UtilNumber.getBigDecimalRoundingMode("invoice.rounding");

        // try getting the default GL account for the invoice item
        if (UtilValidate.isEmpty(invoiceItemPostingGlAccountId)) {
            invoiceItemPostingGlAccountId = getDefaultGlAccount(invoiceItem, organizationPartyId);
        }

        // If there is still no account, try getting a default account
        if ((invoiceItemPostingGlAccountId == null) || invoiceItemPostingGlAccountId.equals("")) {
            invoiceItemPostingGlAccountId = getDefaultGlAccount(invoiceItem, organizationPartyId);
        }

        // if still no GL account, then it is an error
        if ((invoiceItemPostingGlAccountId == null) || (invoiceItemPostingGlAccountId.equals(""))) {
            Debug.logError("Canot find GL account to post for this invoice item " + invoiceItem, MODULE);
            return ServiceUtil.returnError("Cannot find posting GL account for invoice " + invoiceItem.getString("invoiceId")
                    + ", item " + invoiceItem.getString("invoiceItemSeqId"));
        }

        // create the transaction entry
        Map acctgTransEntry = new HashMap();
        acctgTransEntry.put("glAccountId", invoiceItemPostingGlAccountId);
        acctgTransEntry.put("debitCreditFlag", defaultDebitCreditFlag);
        acctgTransEntry.put("organizationPartyId", organizationPartyId);
        acctgTransEntry.put("partyId", transactionPartyId);
        acctgTransEntry.put("roleTypeId", transactionPartyRoleTypeId);
        acctgTransEntry.put("acctgTransEntryTypeId", "_NA_");
        acctgTransEntry.put("productId", invoiceItem.getString("productId"));

        // amount to be posted. NOTE: this conversion is rounded AFTER (conversion*amount*quantity)
        BigDecimal postingAmount = amount.multiply(conversionFactor).multiply(quantity).setScale(decimals, rounding);
        acctgTransEntry.put("amount", postingAmount);
        UtilAccountingTags.putAllAccountingTags(invoiceItem, acctgTransEntry);   // for all invoice items, put all the accounting tags from InvoiceItem to AcctgTransEntry

        // update loop variables
        acctgTransEntries.add(acctgTransEntry);
        postingTotal = postingTotal.add(postingAmount); // this preserves the postingAmount scale (addition => scale = max(scale1, scale2))

        return UtilMisc.toMap("postingTotal", postingTotal);
    }

    /**
     * If an invoice item is not a product, or if it has no specific GL account defined,
     * check if there is a GL account defined generally for this invoice item type and
     * this accounting organization (party). If there is still no organization-specific
     * GL account for this invoice item type, then use InvoiceItemType's default GL account.
     * However, if an overrideGlAccountId is present in the invoiceItem, return that instead.
     *
     * @param invoiceItem a <code>GenericValue</code> value
     * @param organizationPartyId a <code>String</code> value
     * @return a <code>String</code> value
     * @exception GenericEntityException if an error occurs
     */
    private static String getDefaultGlAccount(GenericValue invoiceItem, String organizationPartyId) throws GenericEntityException {
        if (invoiceItem.getString("overrideGlAccountId") != null) {
            return invoiceItem.getString("overrideGlAccountId");
        }
        GenericValue invoiceItemType = invoiceItem.getRelatedOne("InvoiceItemType");
        if (invoiceItemType != null) {
            GenericValue orgInvoiceItemTypeGlAccount = EntityUtil.getFirst(invoiceItemType.getRelatedByAnd("InvoiceItemTypeGlAccount", UtilMisc.toMap("organizationPartyId", organizationPartyId)));
            if (orgInvoiceItemTypeGlAccount != null) {
                return orgInvoiceItemTypeGlAccount.getString("glAccountId");
            } else {
                return invoiceItemType.getString("defaultGlAccountId");
            }
        }
        return null;
    }


    /* ====================================================================== */
    /* =====                  END POST INVOICE TO GL                    ===== */
    /* ====================================================================== */


    /**
     * Service to post a payment other than tax payments to the General Ledger.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map postPaymentToGl(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        String paymentId = (String) context.get("paymentId");
        try {
            DomainsLoader domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(userLogin));
            DomainsDirectory domains = domainLoader.loadDomainsDirectory();
            PaymentRepositoryInterface paymentRepository = domains.getBillingDomain().getPaymentRepository();
            Payment payment = paymentRepository.getPaymentById(paymentId);
            OrganizationRepositoryInterface ori =  domains.getOrganizationDomain().getOrganizationRepository();
            Organization organization = ori.getOrganizationById(payment.getOrganizationPartyId());
            GenericValue paymentValue = delegator.findByPrimaryKey("Payment", UtilMisc.toMap("paymentId", paymentId));
            Map<String, Object> results;

            // check the payment if ready to post, else return error.  This should only return false if your organization
            // has been configured to require accounting tags for payment applications, and the payment is not fully allocated
            if (!payment.isReadyToPost()) {
                return ServiceUtil.returnError(UtilProperties.getMessage("FinancialsErrorLabels", "FinancialsError_CannotPostPartiallyAllocatedPaymentToGl", locale));
            }
            // payroll runs this other service.  the extra parameters prevent a new transaction from being opened
            if (payment.isPayCheck()) {
                results = dispatcher.runSync("postPaycheckToGl", UtilMisc.toMap("paycheck", paymentValue, "userLogin", userLogin), 60, false);
                String acctgTransId = (String) results.get("acctgTransId");
                results = ServiceUtil.returnSuccess();
                results.put("acctgTransIds", UtilMisc.toList(acctgTransId));
                return results;
            }

            // figure out the parties involved and the payment gl account
            results = dispatcher.runSync("getPaymentAccountAndParties", UtilMisc.toMap("paymentId", paymentId), -1, false);
            if (results.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_ERROR)) {
                return results;
            }
            String organizationPartyId = (String) results.get("organizationPartyId");
            String transactionPartyId = (String) results.get("transactionPartyId");
            String paymentGlAccountId = (String) results.get("glAccountId");

            // determine the amount of the payment involved
            BigDecimal conversionFactor = UtilFinancial.determineUomConversionFactor(delegator, dispatcher, organizationPartyId, payment.getCurrencyUomId());
            if ((payment.getAmount() == null) || (payment.getAmount().compareTo(BigDecimal.ZERO) == 0)) {
                Debug.logWarning("Payment [" + paymentId + "] has an amount of [" + payment.getAmount() + "], not posting", MODULE);
                return ServiceUtil.returnSuccess();
            }

            // construct transaction entries for posting
            List acctgTransEntries = FastList.newInstance();
            if (organization.allocatePaymentTagsToApplications()) {
                // if allocate payment tags in application, then it should make one transaction entry per payment application and copy the tags
                // from the payment application to the transaction entry.
                for (PaymentApplication appl : payment.getPaymentApplications()) {
                    Map offsettingGlAccountAmounts = new HashMap();

                    // skip payment applications with null amount
                    if (appl.getAmountApplied() == null) {
                        Debug.logWarning("Payment Application " + appl + " has a null amount, skipping", MODULE);
                        continue;
                    }

                    // determine the offsetting GL account for the applied amount
                    BigDecimal applAmount = appl.getAmountApplied().multiply(conversionFactor);
                    Map paymentGlAccountAmounts = UtilMisc.toMap(paymentGlAccountId, applAmount);
                    if (appl.getOverrideGlAccountId() != null) {
                        // if the payment application already has a gl account, then use it
                        offsettingGlAccountAmounts.put(appl.getOverrideGlAccountId(), applAmount);
                    } else if (appl.getTaxAuthGeoId() != null) {
                        // if payment is applied to tax auth, then get the gl account for the tax authority
                        GenericValue taxAuthGlAccount = delegator.findByPrimaryKeyCache("TaxAuthorityGlAccount",
                                            UtilMisc.toMap("organizationPartyId", organizationPartyId, "taxAuthPartyId", transactionPartyId, "taxAuthGeoId", appl.getTaxAuthGeoId()));
                        offsettingGlAccountAmounts.put(taxAuthGlAccount.getString("glAccountId"), applAmount);
                    } else {
                        // otherwise, use the offsetting GL account of the payment for the payment application's amount
                        // NOTE: this must be done here, since SALES_TAX_PAYMENT would not have offsetting gl accounts configured -- its GL is configured with tax authority
                        String offsettingGlAccountId = getOffsettingPaymentGlAccount(dispatcher, paymentValue, organizationPartyId, userLogin);
                        offsettingGlAccountAmounts.put(offsettingGlAccountId, applAmount);
                    }

                    // determine which to credit and debit
                    Map creditGlAccountAmounts = null;
                    Map debitGlAccountAmounts = null;
                    if (payment.isDisbursement()) {
                        creditGlAccountAmounts = paymentGlAccountAmounts;
                        debitGlAccountAmounts = offsettingGlAccountAmounts;
                    } else if (payment.isReceipt()) {
                        creditGlAccountAmounts = offsettingGlAccountAmounts;
                        debitGlAccountAmounts = paymentGlAccountAmounts;
                    } else {
                        return ServiceUtil.returnError("Cannot Post Payment to GL: Payment with paymentId " + paymentId + " has unsupported paymentTypeId " + payment.getPaymentTypeId() + " (Must be or have a parent type of DISBURSEMENT or RECEIPT.)");
                    }

                    if ((creditGlAccountAmounts == null) || (creditGlAccountAmounts.keySet().size() == 0)) {
                        return ServiceUtil.returnError("No credit GL accounts found for payment posting");
                    }
                    if (debitGlAccountAmounts == null || (debitGlAccountAmounts.keySet().size() == 0)) {
                        return ServiceUtil.returnError("No debit GL accounts found for payment posting");
                    }

                    // create transaction entries for this payment application, and add it to the list of accounting transaction entries for posting
                    acctgTransEntries.addAll(makePaymentEntries(appl, creditGlAccountAmounts, debitGlAccountAmounts,
                            organizationPartyId, transactionPartyId, delegator));
                }
            } else {
                // accounting tags, if any, are in payment itself.  We will need to check for application-specific gl accounts, otherwise post based on payment's gl accounts
                BigDecimal transactionAmount = conversionFactor.multiply(payment.getAmount());

                // These Maps hold glAccountId (String) -> amount (Double) pairs are designed to track how much of the payment goes to each gl account.
                Map paymentGlAccountAmounts = UtilMisc.toMap(paymentGlAccountId, transactionAmount);
                Map offsettingGlAccountAmounts = new HashMap();

                // TODO: Use BigDecimal for this.
                // Loop through all PaymentApplications and see if each one implies a specific offsetting GL account either because it is
                // specifically named or because it is part of a tax payment and if so, use the amount of that application
                BigDecimal unassignedAmount = transactionAmount;

                List paymentApplications = payment.getPaymentApplications();
                for (Iterator pAi = paymentApplications.iterator(); pAi.hasNext();) {
                    PaymentApplication appl = (PaymentApplication) pAi.next();

                    if (appl.getAmountApplied() == null) {
                        Debug.logWarning("Payment Application " + appl + " has a null amount, skipping", MODULE);
                        continue;
                    }
                    BigDecimal applAmount = appl.getAmountApplied().multiply(conversionFactor);

                    if (appl.getOverrideGlAccountId() != null) {
                        offsettingGlAccountAmounts.put(appl.getOverrideGlAccountId(), applAmount);
                        unassignedAmount = unassignedAmount.subtract(applAmount);
                    } else if (appl.getString("taxAuthGeoId") != null) {
                        GenericValue taxAuthGlAccount = delegator.findByPrimaryKeyCache("TaxAuthorityGlAccount",
                                            UtilMisc.toMap("organizationPartyId", organizationPartyId, "taxAuthPartyId", transactionPartyId, "taxAuthGeoId", appl.getTaxAuthGeoId()));
                        offsettingGlAccountAmounts.put(taxAuthGlAccount.getString("glAccountId"), applAmount);
                        unassignedAmount = unassignedAmount.subtract(applAmount);
                    }
                }

                // now put the residual into the offsettingGlAccountId
                if (unassignedAmount.compareTo(BigDecimal.ZERO) == 1) {
                    // NOTE: this must be done here, since SALES_TAX_PAYMENT would not have offsetting gl accounts configured -- its GL is configured with tax authority
                    String offsettingGlAccountId = getOffsettingPaymentGlAccount(dispatcher, paymentValue, organizationPartyId, userLogin);
                    offsettingGlAccountAmounts.put(offsettingGlAccountId, unassignedAmount);
                }

                // determine which to credit and debit
                Map creditGlAccountAmounts = null;
                Map debitGlAccountAmounts = null;
                if (payment.isDisbursement()) {
                    creditGlAccountAmounts = paymentGlAccountAmounts;
                    debitGlAccountAmounts = offsettingGlAccountAmounts;
                } else if (payment.isReceipt()) {
                    creditGlAccountAmounts = offsettingGlAccountAmounts;
                    debitGlAccountAmounts = paymentGlAccountAmounts;
                } else {
                    return ServiceUtil.returnError("Cannot Post Payment to GL: Payment with paymentId " + paymentId + " has unsupported paymentTypeId " + payment.getPaymentTypeId() + " (Must be or have a parent type of DISBURSEMENT or RECEIPT.)");
                }

                if ((creditGlAccountAmounts == null) || (creditGlAccountAmounts.keySet().size() == 0)) {
                    return ServiceUtil.returnError("No credit GL accounts found for posting payment [" + paymentId + "]");
                }
                if (debitGlAccountAmounts == null) {
                    return ServiceUtil.returnError("No debit GL accounts found for posting payment [" + paymentId + "]");
                }
                acctgTransEntries = makePaymentEntries(payment, creditGlAccountAmounts, debitGlAccountAmounts, organizationPartyId, transactionPartyId, delegator);
            }

        // Post transaction
        if (UtilValidate.isNotEmpty(acctgTransEntries)) {
            Map tmpMap = UtilMisc.toMap("acctgTransEntries", acctgTransEntries,
                    "glFiscalTypeId", "ACTUAL", "acctgTransTypeId", "PAYMENT_ACCTG_TRANS",
                    "paymentId", paymentId, "userLogin", userLogin);
            // set the transaction date to the effective date of the payment
            if (payment.getEffectiveDate() != null) {
                tmpMap.put("transactionDate", payment.getEffectiveDate());
            } else {
                Debug.logWarning("Payment [" + paymentId + "] has no effective date, transaction date will be set to now", MODULE);
                tmpMap.put("transactionDate", UtilDateTime.nowTimestamp());
            }
            tmpMap.put("partyId", transactionPartyId);
            tmpMap = dispatcher.runSync("createAcctgTransAndEntries", tmpMap);

            if (((String) tmpMap.get(ModelService.RESPONSE_MESSAGE)).equals(ModelService.RESPOND_SUCCESS)) {
                results = ServiceUtil.returnSuccess();
                String acctgTransId = (String) tmpMap.get("acctgTransId");
                results.put("acctgTransIds", UtilMisc.toList(acctgTransId));
                return results;
            } else {
                return tmpMap;
            }
        } else {
            return ServiceUtil.returnError("No accounting transaction entries created for payment [" + paymentId + "]");
        }

        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        } catch (GenericServiceException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        } catch (RepositoryException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        } catch (EntityNotFoundException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }
    }


    /**
     * Posts a paycheck to General Ledger.  This is not meant to be called directly but from inside postPaymentToGl.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map postPaycheckToGl(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        GenericValue paycheck = (GenericValue) context.get("paycheck");

        PaycheckReader paycheckReader = new PaycheckReader(paycheck);

        // we can't use createAcctgTransAndEntries because the items have different partyIds, and we want to create one big AcctgTrans for the
        // whole paycheck, so we'll just have to create the AcctgTrans and Entries ourselves and cause it to post
        try {
            // the EMPLOYEE role may have other meanings, but thinking about it, only employees get paychecks (subcontractors get paid as vendors) so this is OK
            Map svcParams = UtilMisc.toMap("acctgTransTypeId", "PAYROLL", "glFiscalTypeId", "ACTUAL",
                    "paymentId", paycheck.get("paymentId"), "userLogin", userLogin);
            if (paycheck.get("effectiveDate") != null) {
                svcParams.put("transactionDate", paycheck.get("effectiveDate"));
            } else {
                Debug.logWarning("Paycheck [" + paycheck.get("paymentId") + "] has no effective date, so transaction date will be set to now", MODULE);
                svcParams.put("transactionDate", UtilDateTime.nowTimestamp());
            }
            svcParams.put("partyId", paycheckReader.getEmployeePartyId());
            svcParams.put("roleTypeId", "EMPLOYEE");
            Map tmpResult = dispatcher.runSync("createAcctgTrans", svcParams, 60, false);  // use same transaction
            if (ServiceUtil.isError(tmpResult)) {
                return tmpResult;
            }
            String acctgTransId = (String) tmpResult.get("acctgTransId");

            // get the currency of the organization for GL postings and
            String baseCurrency = UtilCommon.getOrgBaseCurrency(paycheckReader.getOrganizationPartyId(), delegator);
            if (UtilValidate.isEmpty(baseCurrency)) {
                return ServiceUtil.returnError("No account preference or base currency found for organization [" + paycheckReader.getOrganizationPartyId() + "]");
            }
            BigDecimal conversionFactor = UtilFinancial.determineUomConversionFactor(delegator, dispatcher, paycheckReader.getOrganizationPartyId(), baseCurrency);

            tmpResult = postPaycheckEntry(acctgTransId, paycheckReader.getSalaryExpenseGlAccountId(), "D", paycheckReader.getOrganizationPartyId(), paycheckReader.getEmployeePartyId(), "EMPLOYEE",
                    paycheckReader.getEmployeePartyId(), paycheckReader.getGrossAmount().multiply(conversionFactor).setScale(decimals, rounding), baseCurrency, dispatcher, userLogin);
            if (ServiceUtil.isError(tmpResult)) { return tmpResult; }

            tmpResult = dispatcher.runSync("getPaymentAccountAndParties", UtilMisc.toMap("paymentId", paycheck.get("paymentId")));
            if (ServiceUtil.isError(tmpResult)) { return tmpResult; }
            String paymentGlAccountId = (String) tmpResult.get("glAccountId");

            tmpResult = postPaycheckEntry(acctgTransId, paymentGlAccountId, "C", paycheckReader.getOrganizationPartyId(), paycheckReader.getEmployeePartyId(), "EMPLOYEE",
                    paycheckReader.getEmployeePartyId(), paycheckReader.getNetAmount().multiply(conversionFactor).setScale(decimals, rounding), baseCurrency, dispatcher, userLogin);
            if (ServiceUtil.isError(tmpResult)) { return tmpResult; }

            // deductions are credited only to the collecting agency, such as the tax authority
            // I am using BILL_FROM_VENDOR for now, but later we might use something more sophisticated.  It should be easy to change old data with an UPDATE / SELECT
            List<GenericValue> paycheckDeductions = paycheckReader.getPaycheckItemsByClass("DEDUCTION");
            for (GenericValue deductionItem : paycheckDeductions) {
                if (UtilValidate.isNotEmpty(deductionItem.get("amount"))) {
                    tmpResult = postPaycheckEntry(acctgTransId, paycheckReader.getCreditGlAccountId(deductionItem), "C", paycheckReader.getOrganizationPartyId(), paycheckReader.getPostToPartyId(deductionItem), "PAYROLL_VENDOR",
                            paycheckReader.getEmployeePartyId(), deductionItem.getBigDecimal("amount").multiply(conversionFactor).setScale(decimals, rounding), baseCurrency, dispatcher, userLogin);
                    if (ServiceUtil.isError(tmpResult)) { return tmpResult; }
                }
            }

            // create debit and credit entries for the expense items.  all the debits are to the employee, all the expenses to their respective posting parties
            List<GenericValue> paycheckExpenses = paycheckReader.getPaycheckItemsByClass("EXPENSE");
            for (GenericValue expenseItem : paycheckExpenses) {
                if (UtilValidate.isNotEmpty(expenseItem.get("amount"))) {
                    tmpResult = postPaycheckEntry(acctgTransId, paycheckReader.getDebitGlAccountId(expenseItem), "D", paycheckReader.getOrganizationPartyId(), paycheckReader.getEmployeePartyId(), "EMPLOYEE",
                            paycheckReader.getEmployeePartyId(), expenseItem.getBigDecimal("amount").multiply(conversionFactor).setScale(decimals, rounding), baseCurrency, dispatcher, userLogin);
                    if (ServiceUtil.isError(tmpResult)) { return tmpResult; }
                }

                if (UtilValidate.isNotEmpty(expenseItem.get("amount"))) {
                    tmpResult = postPaycheckEntry(acctgTransId, paycheckReader.getCreditGlAccountId(expenseItem), "C", paycheckReader.getOrganizationPartyId(), paycheckReader.getPostToPartyId(expenseItem), "PAYROLL_VENDOR",
                            paycheckReader.getEmployeePartyId(), expenseItem.getBigDecimal("amount").multiply(conversionFactor).setScale(decimals, rounding), baseCurrency, dispatcher, userLogin);
                    if (ServiceUtil.isError(tmpResult)) { return tmpResult; }
                }
            }

            Map results = ServiceUtil.returnSuccess();
            results.put("acctgTransId", acctgTransId);
            return results;

        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        } catch (GenericServiceException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }

    }

    /**
     * A convenience method for creating an AcctgTransEntry and an AcctgTransEntryRole for the given parameters.  Will do currency conversion.
     * @param acctgTransId a <code>String</code> value
     * @param glAccountId a <code>String</code> value
     * @param debitCreditFlag a <code>String</code> value
     * @param organizationPartyId a <code>String</code> value
     * @param postingPartyId a <code>String</code> value
     * @param postingRoleTypeId a <code>String</code> value
     * @param employeePartyId a <code>String</code> value
     * @param amount a <code>BigDecimal</code> value
     * @param currencyUomId a <code>String</code> value
     * @param dispatcher a <code>LocalDispatcher</code> value
     * @param userLogin a <code>GenericValue</code> value
     * @return result of service call
     * @exception GenericServiceException if an error occurs
     * @exception GenericEntityException if an error occurs
     */
    private static Map<String, Object> postPaycheckEntry(String acctgTransId, String glAccountId, String debitCreditFlag, String organizationPartyId, String postingPartyId, String postingRoleTypeId,
                                         String employeePartyId, BigDecimal amount, String currencyUomId, LocalDispatcher dispatcher, GenericValue userLogin) throws GenericServiceException, GenericEntityException {

        // make sure the party has this role
        Map<String, Object> tmpResult = dispatcher.runSync("ensurePartyRole", UtilMisc.toMap("partyId", postingPartyId, "roleTypeId", postingRoleTypeId, "userLogin", userLogin), 60, false);
        if (ServiceUtil.isError(tmpResult)) { return tmpResult; }

        // create the ledger entries
        Map<String, Object> svcParams = UtilMisc.toMap("acctgTransId", acctgTransId, "acctgTransEntryTypeId", "_NA_", "organizationPartyId", organizationPartyId,
                           "currencyUomId", currencyUomId, "userLogin", userLogin);
        svcParams.put("partyId", postingPartyId);
        svcParams.put("glAccountId", glAccountId);
        svcParams.put("debitCreditFlag", debitCreditFlag);
        svcParams.put("roleTypeId", postingRoleTypeId);
        svcParams.put("amount", amount);
        tmpResult = dispatcher.runSync("createAcctgTransEntry", svcParams, 60, false); // use same transaction
        if (ServiceUtil.isError(tmpResult)) { return tmpResult; }

        // if successful then create AcctgTransEntryRole
        Delegator delegator = dispatcher.getDelegator();
        delegator.create("AcctgTransEntryRole", UtilMisc.toMap("acctgTransId", acctgTransId, "acctgTransEntrySeqId", tmpResult.get("acctgTransEntrySeqId"), "partyId", employeePartyId, "roleTypeId", "EMPLOYEE"));

        return tmpResult;
    }

    /**
     * Get the offsetting account for a payment.
     * @param dispatcher a <code>LocalDispatcher</code> value
     * @param payment a <code>GenericValue</code> value
     * @param organizationPartyId a <code>String</code> value
     * @param userLogin a <code>GenericValue</code> value
     * @return  The offsetting glAccountId of a payment or null if one cannot be found
     * @exception GenericServiceException if no GlAccount was configured for the organization
     * @exception GenericEntityException if an error occurs
     */
    public static String getOffsettingPaymentGlAccount(LocalDispatcher dispatcher, GenericValue payment, String organizationPartyId, GenericValue userLogin) throws GenericServiceException, GenericEntityException {
        // Payment.overrideGlAccountId overrides any other from PaymentType
        if (UtilValidate.isNotEmpty(payment.getString("overrideGlAccountId"))) {
            return payment.getString("overrideGlAccountId");
        }

        String offsettingGlAccountTypeId = getOffsettingGlAccountTypeIdForPayment(payment, organizationPartyId);

        // get the GL account from the type
        return UtilAccounting.getDefaultAccountId(offsettingGlAccountTypeId, organizationPartyId, userLogin.getDelegator());
    }

    /**
     * Find the type of the offsetting GL Account (ACCOUNTS_RECEIVABLE, INVENTORY, etc.) in GlAccountTypeDefault based on glAccoutTypeId of the PaymentType.
     * @param payment a <code>GenericValue</code> value
     * @param organizationPartyId the organization party ID
     * @return the offsetting GL Account type ID
     * @throws GenericEntityException if an error occurs
     * @throws AccountingException if no offsetting GL account is found
     */
    @SuppressWarnings("unchecked")
    private static String getOffsettingGlAccountTypeIdForPayment(GenericValue payment, String organizationPartyId) throws GenericEntityException, AccountingException {
        List tmpList = payment.getRelatedOne("PaymentType").getRelatedByAndCache("PaymentGlAccountTypeMap", UtilMisc.toMap("organizationPartyId", organizationPartyId));
        if (tmpList.size() == 0) {
            throw new AccountingException("Offsetting GL account for payment type " + payment.getString("paymentTypeId")
                    + " of organization " + organizationPartyId + " has not been configured.");
        }
        return ((GenericValue) tmpList.get(0)).getString("glAccountTypeId");
    }

    /**
     * A little helper method to postPaymentToGl.
     * This method will copy all accounting tags from Payment to each AcctgTransEntry
     * @param payment a <code>GenericValue</code> value
     * @param creditGlAccountAmounts a <code>Map</code> value
     * @param debitGlAccountAmounts a <code>Map</code> value
     * @param organizationPartyId a <code>String</code> value
     * @param transactionPartyId a <code>String</code> value
     * @param delegator a <code>Delegator</code> value
     * @return a <code>List</code> value
     * @exception GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    private static List makePaymentEntries(Payment payment, Map creditGlAccountAmounts, Map debitGlAccountAmounts,
            String organizationPartyId, String transactionPartyId, Delegator delegator) throws GenericEntityException {
        List acctgTransEntries = new LinkedList();
        int itemSeq = 1;
        for (Iterator ai = creditGlAccountAmounts.keySet().iterator(); ai.hasNext();) {
            String creditGlAccountId = (String) ai.next();
            Map tmpMap = UtilMisc.toMap("glAccountId", creditGlAccountId, "debitCreditFlag", "C",
                    "amount", creditGlAccountAmounts.get(creditGlAccountId), "acctgTransEntrySeqId", Integer.toString(itemSeq),
                    "organizationPartyId", organizationPartyId, "acctgTransEntryTypeId", "_NA_");
            tmpMap.put("partyId", transactionPartyId);
            UtilAccountingTags.putAllAccountingTags(payment, tmpMap);          // for Payments it's OK to copy the tags right over
            GenericValue creditAcctTransEntry = delegator.makeValue("AcctgTransEntry", tmpMap);
            acctgTransEntries.add(creditAcctTransEntry);
            itemSeq++;
        }

        for (Iterator ai = debitGlAccountAmounts.keySet().iterator(); ai.hasNext();) {
            String debitGlAccountId = (String) ai.next();
            Map tmpMap = UtilMisc.toMap("glAccountId", debitGlAccountId, "debitCreditFlag", "D",
                    "amount", debitGlAccountAmounts.get(debitGlAccountId), "acctgTransEntrySeqId", Integer.toString(itemSeq),
                    "organizationPartyId", organizationPartyId, "acctgTransEntryTypeId", "_NA_");
            tmpMap.put("partyId", transactionPartyId);
            UtilAccountingTags.putAllAccountingTags(payment, tmpMap);
            GenericValue debitAcctTransEntry = delegator.makeValue("AcctgTransEntry", tmpMap);
            acctgTransEntries.add(debitAcctTransEntry);
            itemSeq++;
        }

        return acctgTransEntries;
    }

    /**
     * A little helper method to postPaymentToGl.
     * This method will copy all accounting tags from Payment to each AcctgTransEntry
     * @param paymentApplication a <code>GenericValue</code> value
     * @param creditGlAccountAmounts a <code>Map</code> value
     * @param debitGlAccountAmounts a <code>Map</code> value
     * @param organizationPartyId a <code>String</code> value
     * @param transactionPartyId a <code>String</code> value
     * @param delegator a <code>Delegator</code> value
     * @return a <code>List</code> value
     * @exception GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    private static List makePaymentEntries(PaymentApplication paymentApplication, Map creditGlAccountAmounts, Map debitGlAccountAmounts,
            String organizationPartyId, String transactionPartyId, Delegator delegator) throws GenericEntityException {
        List acctgTransEntries = new LinkedList();
        int itemSeq = 1;
        for (Iterator ai = creditGlAccountAmounts.keySet().iterator(); ai.hasNext();) {
            String creditGlAccountId = (String) ai.next();
            Map tmpMap = UtilMisc.toMap("glAccountId", creditGlAccountId, "debitCreditFlag", "C",
                    "amount", creditGlAccountAmounts.get(creditGlAccountId), "acctgTransEntrySeqId", Integer.toString(itemSeq),
                    "organizationPartyId", organizationPartyId, "acctgTransEntryTypeId", "_NA_");
            tmpMap.put("partyId", transactionPartyId);
            UtilAccountingTags.putAllAccountingTags(paymentApplication, tmpMap);          // for Payments it's OK to copy the tags right over
            GenericValue creditAcctTransEntry = delegator.makeValue("AcctgTransEntry", tmpMap);
            acctgTransEntries.add(creditAcctTransEntry);
            itemSeq++;
        }

        for (Iterator ai = debitGlAccountAmounts.keySet().iterator(); ai.hasNext();) {
            String debitGlAccountId = (String) ai.next();
            Map tmpMap = UtilMisc.toMap("glAccountId", debitGlAccountId, "debitCreditFlag", "D",
                    "amount", debitGlAccountAmounts.get(debitGlAccountId), "acctgTransEntrySeqId", Integer.toString(itemSeq),
                    "organizationPartyId", organizationPartyId, "acctgTransEntryTypeId", "_NA_");
            tmpMap.put("partyId", transactionPartyId);
            UtilAccountingTags.putAllAccountingTags(paymentApplication, tmpMap);
            GenericValue debitAcctTransEntry = delegator.makeValue("AcctgTransEntry", tmpMap);
            acctgTransEntries.add(debitAcctTransEntry);
            itemSeq++;
        }

        return acctgTransEntries;
    }

    /**
     * Service to determine accounting parties and GL account of a payment.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map getPaymentAccountAndParties(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();

        String paymentId = (String) context.get("paymentId");
        // return values
        Map result = ServiceUtil.returnSuccess();
        String organizationPartyId = null;
        String transactionPartyId = null;
        String glAccountId = null;
        try {
            GenericValue payment = delegator.findByPrimaryKey("Payment", UtilMisc.toMap("paymentId", paymentId));
            if (payment == null) {
                return ServiceUtil.returnError("Payment " + paymentId + " doesn't exist!");
            }

            // step 0 figure out which party is debit vs. credit
            if (UtilAccounting.isDisbursement(payment)) {
                organizationPartyId = payment.getString("partyIdFrom");
                transactionPartyId = payment.getString("partyIdTo");
            } else if (UtilAccounting.isReceipt(payment)) {
                organizationPartyId = payment.getString("partyIdTo");
                transactionPartyId = payment.getString("partyIdFrom");
            } else {
                return ServiceUtil.returnError("Payment with paymentId " + paymentId + " has a type which is not DISBURSEMENT or RECEIPT.");
            }
            result.put("organizationPartyId", organizationPartyId);
            result.put("transactionPartyId", transactionPartyId);

            // step one, look for glAccountId in PaymentMethod
            GenericValue paymentMethod = payment.getRelatedOne("PaymentMethod");
            if (paymentMethod != null) {
                glAccountId = paymentMethod.getString("glAccountId");
                if (glAccountId != null) {
                    result.put("glAccountId", glAccountId);
                    return result;
                }
            }

            // step two: glAccountId from CreditCardTypeGlAccount
            if ("CREDIT_CARD".equals(payment.getString("paymentMethodTypeId"))) {
                GenericValue cc = payment.getRelatedOne("CreditCard");
                if (cc == null) {
                    Debug.logWarning("Cannot find Gl Account from CreditCartTypeGlAccount: Credit Card not found for Payment with paymentId " + payment.getString("paymentId") + ".  Trying Gl Account for Credit Cards or default Gl Account instead.", MODULE);
                } else {
                    GenericValue ccGlAccount = delegator.findByPrimaryKey("CreditCardTypeGlAccount", UtilMisc.toMap("organizationPartyId", organizationPartyId, "cardType", cc.getString("cardType")));
                    if (ccGlAccount != null) {
                        result.put("glAccountId", ccGlAccount.getString("glAccountId"));
                        return result;
                    }
                }
            }

            // step three: see if the payment method type has a gl account via PaymentMethodTypeGlAccount
            GenericValue paymentMethodType = payment.getRelatedOne("PaymentMethodType");
            if (UtilValidate.isNotEmpty(paymentMethodType)) {
                List tmpList = paymentMethodType.getRelatedByAnd("PaymentMethodTypeGlAccount", UtilMisc.toMap("organizationPartyId", organizationPartyId));
                if (tmpList.size() > 0) {
                    GenericValue paymentMethodTypeGlAccount = (GenericValue) tmpList.get(0);
                    glAccountId = paymentMethodTypeGlAccount.getString("glAccountId");
                    if (glAccountId != null) {
                        result.put("glAccountId", glAccountId);
                        return result;
                    }
                }
            }

            // step four: defaultGlAccountId
            if (UtilValidate.isNotEmpty(paymentMethodType)) {
                glAccountId = paymentMethodType.getString("defaultGlAccountId");
                if (glAccountId != null) {
                    result.put("glAccountId", glAccountId);
                    return result;
                }
            }

            return ServiceUtil.returnError("No GL Account found for Payment with paymentId " + paymentId);
        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }
    }

    /**
     * Service to match a Payment to an Invoice for a particular PaymentApplication.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map matchPaymentInvoiceGlPosts(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String paymentApplicationId = (String) context.get("paymentApplicationId");
        try {
            GenericValue paymentApplication = delegator.findByPrimaryKey("PaymentApplication", UtilMisc.toMap("paymentApplicationId", paymentApplicationId));
            GenericValue payment = paymentApplication.getRelatedOne("Payment");
            GenericValue invoice = paymentApplication.getRelatedOne("Invoice");
            String paymentId = payment.getString("paymentId");
            String invoiceId = invoice.getString("invoiceId");

            if (invoice == null) {
                throw new GenericServiceException("Could not find Invoice with ID [" + invoiceId + "]");
            }

            // transaction information
            String organizationPartyId = null;
            String paymentOffsetDebitCreditFlag = null;
            String invoiceOffsetDebitCreditFlag = null;
            String invoiceOffsettingGlAccountTypeId = null;
            String transactionPartyId = payment.getString("partyIdTo");
            String transactionPartyRoleTypeId = payment.getString("roleTypeIdTo");
            String acctgTransTypeId = "PAYMENT_ACCTG_TRANS";

            // determine the organization, offsetting account type, and debitCreditFlags, but first make sure that we're only
            // doing this for SALES invoices and payment receipts or PURCHASE invoices and disbursements
            if ((UtilAccounting.isDisbursement(payment)) && (invoice.getString("invoiceTypeId").equals("PURCHASE_INVOICE"))) {
                organizationPartyId = payment.getString("partyIdFrom");
                paymentOffsetDebitCreditFlag = "D";
                invoiceOffsetDebitCreditFlag = "C";
                invoiceOffsettingGlAccountTypeId = "ACCOUNTS_PAYABLE";
            } else if ((UtilAccounting.isReceipt(payment)) && (invoice.getString("invoiceTypeId").equals("SALES_INVOICE"))) { // Receipt
                organizationPartyId = payment.getString("partyIdTo");
                paymentOffsetDebitCreditFlag = "C";
                invoiceOffsetDebitCreditFlag = "D";
                invoiceOffsettingGlAccountTypeId = "ACCOUNTS_RECEIVABLE";
            } else {
                // possibly other types of payments or invoices involved, such as Customer Return Invoices and Customer Refunds.
                return ServiceUtil.returnSuccess();
            }

            // get the offsetting account of the payment
            String paymentOffsettingGlAccountId = getOffsettingPaymentGlAccount(dispatcher, payment, organizationPartyId, userLogin);

            // get the offsetting gl account of the invoice
            String invoiceOffsettingGlAccountId = UtilAccounting.getDefaultAccountId(invoiceOffsettingGlAccountTypeId, organizationPartyId, delegator);

            // if the accounts are the same, there's no need to match
            if (paymentOffsettingGlAccountId.equals(invoiceOffsettingGlAccountId)) {
                if (Debug.verboseOn()) {
                    Debug.logVerbose("Matching payment to invoice: Payment and Invoice offsetting accounts were identical. No need to match.", MODULE);
                }
                return ServiceUtil.returnSuccess();
            }

            // make the transaction entry for the offsetting payment account
            Map input = new HashMap();
            input.put("glAccountId", paymentOffsettingGlAccountId);
            input.put("acctgTransEntrySeqId", "1");
            input.put("organizationPartyId", organizationPartyId);
            input.put("partyId", transactionPartyId);
            input.put("roleTypeId", transactionPartyRoleTypeId);
            input.put("debitCreditFlag", invoiceOffsetDebitCreditFlag); // want opposite flag
            input.put("amount", paymentApplication.getBigDecimal("amountApplied"));
            input.put("acctgTransEntryTypeId", "_NA_");
            input.put("description", "Matching GL accounts for invoice " + invoiceId + " and payment " + paymentId);
            GenericValue paymentEntry = delegator.makeValue("AcctgTransEntry", input);

            // make the transaction entry for the offsetting invoice account
            input = new HashMap();
            input.put("glAccountId", invoiceOffsettingGlAccountId);
            input.put("acctgTransEntrySeqId", "2");
            input.put("organizationPartyId", organizationPartyId);
            input.put("partyId", transactionPartyId);
            input.put("roleTypeId", transactionPartyRoleTypeId);
            input.put("debitCreditFlag", paymentOffsetDebitCreditFlag); // want opposite flag
            input.put("amount", paymentApplication.getBigDecimal("amountApplied"));
            input.put("acctgTransEntryTypeId", "_NA_");
            input.put("description", "Matching GL accounts for invoice " + invoiceId + " and payment " + paymentId);
            GenericValue invoiceEntry = delegator.makeValue("AcctgTransEntry", input);

            // prepare the transaction
            input = new HashMap();
            input.put("acctgTransEntries", UtilMisc.toList(paymentEntry, invoiceEntry));
            input.put("invoiceId", invoiceId);
            input.put("partyId", transactionPartyId);
            input.put("roleTypeId", transactionPartyRoleTypeId);
            input.put("glFiscalTypeId", "ACTUAL");
            input.put("acctgTransTypeId", acctgTransTypeId);
            input.put("userLogin", userLogin);

            // create the transaction and return the acctgTransId
            return dispatcher.runSync("createAcctgTransAndEntries", input);

        } catch (GenericEntityException ee) {
            return ServiceUtil.returnError(ee.getMessage());
        } catch (GenericServiceException se) {
            return ServiceUtil.returnError(se.getMessage());
        }
    }

    /**
     * Service to post an inventory variance transaction to the General Ledger.
     * The generated transaction entries are tagged the same as the inventory item which has the variance.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map postInventoryVarianceToGl(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Locale locale = UtilCommon.getLocale(context);
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String inventoryItemId = (String) context.get("inventoryItemId");
        String physicalInventoryId = (String) context.get("physicalInventoryId");
        try {
            Map tmpMap = new HashMap();

            GenericValue inventoryVariance = delegator.findByPrimaryKey("InventoryItemVariance",
                    UtilMisc.toMap("inventoryItemId", inventoryItemId, "physicalInventoryId", physicalInventoryId));
            if (inventoryVariance == null) {
                return ServiceUtil.returnError("No InventoryVariance entity record for inventoryItemId " + inventoryItemId + " and physicalInventoryId " + physicalInventoryId);
            }

            BigDecimal quantityOnHandVar = inventoryVariance.getBigDecimal("quantityOnHandVar");
            if (quantityOnHandVar == null || quantityOnHandVar.compareTo(ZERO) == 0) {
                // no actual inventory loss or gain to account for
                return ServiceUtil.returnSuccess();
            }

            GenericValue inventoryItem = inventoryVariance.getRelatedOne("InventoryItem");
            Map<String, String> tags = new HashMap<String, String>();
            if (inventoryItem != null) {
                UtilAccountingTags.putAllAccountingTags(inventoryItem, tags);
                Debug.logInfo("Making transaction entries with accounting tags from inventory item [" + inventoryItem + "] : " + tags, MODULE);
            }
            String productId = inventoryItem.getString("productId");
            // owner of inventory item
            String ownerPartyId = inventoryItem.getString("ownerPartyId");

            if (!UtilFinancial.hasActiveLedger(delegator, ownerPartyId)) {
                return UtilMessage.createAndLogServiceFailure("FinancialsErrorNoActiveLedgerForParty", UtilMisc.toMap("partyId", ownerPartyId), locale, MODULE);
            }

            // get the inventory item unit cost
            BigDecimal unitCost = inventoryItem.getBigDecimal("unitCost");
            // get the currency conversion factor
            BigDecimal conversionFactor = UtilFinancial.determineUomConversionFactor(delegator, dispatcher, ownerPartyId, inventoryItem.getString("currencyUomId"));
            // validate the unitCost and compute transaction amount
            if (unitCost == null) {
                return ServiceUtil.returnError("Could not determine unitCost of product [" + productId + "] for inventory variance [" + physicalInventoryId + "] and inventory item [" + inventoryItemId + "]");
            }
            // convert the intentory item's unit cost into the owner's currency
            unitCost = unitCost.multiply(conversionFactor).setScale(decimals, rounding);
            // The transaction amount is: amount = quantityOnHandVar * unitCost
            BigDecimal transactionAmount = unitCost.multiply(quantityOnHandVar).setScale(decimals, rounding);

            // get owner's party COGS method
            GenericValue acctgPref = delegator.findByPrimaryKeyCache("PartyAcctgPreference", UtilMisc.toMap("partyId", ownerPartyId));
            String cogsMethodId = acctgPref.getString("cogsMethodId");

            // If method is COGS_AVG_COST, also compute the inventory adjustment amount = (prodAvgCost - unitCost) * quantityOnHandVar
            BigDecimal inventoryAdjAmount = null;
            if ((cogsMethodId != null) && (cogsMethodId.equals("COGS_AVG_COST"))) {
                BigDecimal prodAvgCost = UtilCOGS.getProductAverageCost(productId, ownerPartyId, userLogin, delegator, dispatcher);
                if (prodAvgCost == null) {
                    Debug.logWarning("Unable to find a product average cost for product [" + productId + "] in organization [" + ownerPartyId + "], no adjustment will be made in inventory variance", MODULE);
                } else {
                    // TODO: there could be rounding issues here; maybe it's better to do something like this:
                    //       (prodAvgCost * quantityOnHandVar) - (unitCost * quantityOnHandVar) and then set the scale.
                    inventoryAdjAmount = prodAvgCost.subtract(unitCost).multiply(quantityOnHandVar).setScale(decimals, rounding);
                }
            }

            // Inventory GL account
            String invGlAcctId = UtilAccounting.getProductOrgGlAccountId(productId, "INVENTORY_ACCOUNT", ownerPartyId, delegator);

            // Variance Expense GL Account
            // TODO: At some point, maybe we should add an accountTypeId field to the primary key of the VarianceReasonGlAccount so that
            //       we can set, for each and every variance reason, the account for the primary transaction and
            //       the account for the adjustment?
            //       For now I'll assume that the credit (C) accounts for the primary transaction
            //       and for the adjustment transaction are the same.
            GenericValue varianceReason = inventoryVariance.getRelatedOne("VarianceReason");
            GenericValue varExpGlAcct = EntityUtil.getFirst(varianceReason.getRelatedByAnd("VarianceReasonGlAccount", UtilMisc.toMap("organizationPartyId", ownerPartyId)));
            if (varExpGlAcct == null) {
                return ServiceUtil.returnError("Could not find Variance Expense GL Account for variance reason [" + varianceReason.get("description") + "].");
            }
            String varExpGlAcctId = (String) varExpGlAcct.get("glAccountId");

            // ===========================================================
            // Transaction to credit the variance expense GL acct
            tmpMap = UtilMisc.toMap("glAccountId", varExpGlAcctId, "debitCreditFlag", "C",
                "amount", transactionAmount, "acctgTransEntrySeqId", Integer.toString(0),
                "organizationPartyId", ownerPartyId, "acctgTransEntryTypeId", "_NA_");
            tmpMap.put("productId", productId);
            GenericValue varExpGlAcctTrans = delegator.makeValue("AcctgTransEntry", tmpMap);
            // copy the accounting tags
            UtilAccountingTags.putAllAccountingTags(tags, varExpGlAcctTrans);

            // Transaction to debit the inventory GL acct
            tmpMap = UtilMisc.toMap("glAccountId", invGlAcctId, "debitCreditFlag", "D",
                "amount", transactionAmount, "acctgTransEntrySeqId", Integer.toString(1),
                "organizationPartyId", ownerPartyId, "acctgTransEntryTypeId", "_NA_");
            tmpMap.put("productId", productId);
            GenericValue invGlAcctTrans = delegator.makeValue("AcctgTransEntry", tmpMap);
            // copy the accounting tags
            UtilAccountingTags.putAllAccountingTags(tags, invGlAcctTrans);

            List transEntries = UtilMisc.toList(varExpGlAcctTrans, invGlAcctTrans);

            // Adjustment transaction for the difference of unit cost and average cost
            if ((inventoryAdjAmount != null) && (inventoryAdjAmount.compareTo(ZERO) != 0)) {
                // Inventory GL account for inventory cost adjustments
                String invGlAcctAdjId = UtilAccounting.getProductOrgGlAccountId(productId, "INV_ADJ_AVG_COST", ownerPartyId, delegator);
                // TODO: for now I'll assume that the credit (C) account
                //       for the adjustment transaction is the same one of the
                //       primary transaction.
                // Transaction to credit the variance expense GL acct
                tmpMap = UtilMisc.toMap("glAccountId", varExpGlAcctId, "debitCreditFlag", "C",
                    "amount", inventoryAdjAmount, "acctgTransEntrySeqId", Integer.toString(0),
                    "organizationPartyId", ownerPartyId, "acctgTransEntryTypeId", "_NA_");
                tmpMap.put("productId", productId);
                GenericValue varExpGlAcctAdjTrans = delegator.makeValue("AcctgTransEntry", tmpMap);
                // copy the accounting tags
                UtilAccountingTags.putAllAccountingTags(tags, varExpGlAcctAdjTrans);
                transEntries.add(varExpGlAcctAdjTrans);

                // Transaction to debit the inventory GL acct
                tmpMap = UtilMisc.toMap("glAccountId", invGlAcctAdjId, "debitCreditFlag", "D",
                    "amount", inventoryAdjAmount, "acctgTransEntrySeqId", Integer.toString(1),
                    "organizationPartyId", ownerPartyId, "acctgTransEntryTypeId", "_NA_");
                tmpMap.put("productId", productId);
                GenericValue invGlAcctAdjTrans = delegator.makeValue("AcctgTransEntry", tmpMap);
                // copy the accounting tags
                UtilAccountingTags.putAllAccountingTags(tags, invGlAcctAdjTrans);
                transEntries.add(invGlAcctAdjTrans);
            }
            // Perform the transaction
            tmpMap = UtilMisc.toMap("acctgTransEntries", transEntries,
                    "glFiscalTypeId", "ACTUAL", "acctgTransTypeId", "ITEM_VARIANCE_ACCTG_",
                    "transactionDate", UtilDateTime.nowTimestamp(),
                    "userLogin", userLogin);
            tmpMap.put("inventoryItemId", inventoryItemId);
            tmpMap.put("physicalInventoryId", physicalInventoryId);
            tmpMap = dispatcher.runSync("createAcctgTransAndEntries", tmpMap);

            if (ServiceUtil.isError(tmpMap)) {
                return tmpMap;
            }
            Map result = ServiceUtil.returnSuccess();
            result.put("acctgTransId", tmpMap.get("acctgTransId"));
            return result;
        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        } catch (GenericServiceException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }
    }

    /**
     * Service to post outbound Shipments to GL.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map postShipmentToGl(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String shipmentId = (String) context.get("shipmentId");
        try {
            List issuances = delegator.findByAnd("ItemIssuance", UtilMisc.toMap("shipmentId", shipmentId));
            Iterator issuancesIt = issuances.iterator();
            List transEntries = new ArrayList();
            Map input = null;
            String partyIdTo = null;
            while (issuancesIt.hasNext()) {
                GenericValue itemIssuance = (GenericValue) issuancesIt.next();
                GenericValue inventoryItem = itemIssuance.getRelatedOne("InventoryItem");

                // don't process serialized items, these are handled by postInventoryItemStatusChange
                if ("SERIALIZED_INV_ITEM".equals(inventoryItem.get("inventoryItemTypeId"))) {
                    continue;
                }

                GenericValue orderHeader = itemIssuance.getRelatedOne("OrderHeader");
                GenericValue orderRole = EntityUtil.getFirst(orderHeader.getRelatedByAnd("OrderRole", UtilMisc.toMap("roleTypeId", "BILL_TO_CUSTOMER")));
                partyIdTo = orderRole.getString("partyId");

                String productId = inventoryItem.getString("productId");
                BigDecimal quantityIssued = itemIssuance.getBigDecimal("quantity");
                // get the inventory item's owner
                String ownerPartyId = inventoryItem.getString("ownerPartyId");
                // get the inventory item unit cost
                BigDecimal unitCost = inventoryItem.getBigDecimal("unitCost");
                // get the currency conversion factor
                BigDecimal conversionFactor = UtilFinancial.determineUomConversionFactor(delegator, dispatcher, ownerPartyId, inventoryItem.getString("currencyUomId"));
                // validate the unitCost and compute transaction amount
                if (unitCost == null) {
                    Debug.logWarning("Could not determine unitCost of product [" + productId + "] for item issuance [" + itemIssuance.getString("itemIssuanceId") + "] and inventory item [" + inventoryItem + "], assuming 0", MODULE);
                    unitCost = ZERO;
                }
                // convert the intentory item's unit cost into the owner's currency
                unitCost = unitCost.multiply(conversionFactor).setScale(decimals, rounding);
                BigDecimal transactionAmount = unitCost.multiply(quantityIssued).setScale(decimals, rounding);
                // Inventory GL account
                String invGlAcctId = UtilAccounting.getProductOrgGlAccountId(productId, "INVENTORY_ACCOUNT", ownerPartyId, delegator);
                String invGlAcctCogsId = UtilAccounting.getProductOrgGlAccountId(productId, "COGS_ACCOUNT", ownerPartyId, delegator);

                // Transaction to credit the inventory account
                input = UtilMisc.toMap("glAccountId", invGlAcctId, "organizationPartyId", ownerPartyId, "partyId", partyIdTo);
                input.put("productId", productId);
                input.put("amount", transactionAmount);
                input.put("acctgTransEntryTypeId", "_NA_");
                input.put("debitCreditFlag", "C");
                input.put("acctgTransEntrySeqId", Integer.toString(0));
                input.put("roleTypeId", "BILL_TO_CUSTOMER");
                GenericValue creditAcctTrans = delegator.makeValue("AcctgTransEntry", input);
                // add the inventory item tags
                UtilAccountingTags.putAllAccountingTags(inventoryItem, creditAcctTrans);
                transEntries.add(creditAcctTrans);

                // Transaction to debit the cogs account
                input = UtilMisc.toMap("glAccountId", invGlAcctCogsId, "organizationPartyId", ownerPartyId, "partyId", partyIdTo);
                input.put("productId", productId);
                input.put("amount", transactionAmount);
                input.put("acctgTransEntryTypeId", "_NA_");
                input.put("debitCreditFlag", "D");
                input.put("acctgTransEntrySeqId", Integer.toString(1));
                input.put("roleTypeId", "BILL_TO_CUSTOMER");
                GenericValue debitAcctTrans = delegator.makeValue("AcctgTransEntry", input);
                // add the inventory item tags
                UtilAccountingTags.putAllAccountingTags(inventoryItem, debitAcctTrans);
                transEntries.add(debitAcctTrans);

                // Get owner's party COGS method.  If method is COGS_AVG_COST, also compute the inventory adjustment amount = (prodAvgCost - unitCost) * quantityOnHandVar
                GenericValue acctgPref = delegator.findByPrimaryKeyCache("PartyAcctgPreference", UtilMisc.toMap("partyId", ownerPartyId));
                String cogsMethodId = acctgPref.getString("cogsMethodId");
                BigDecimal inventoryAdjAmount = null;
                if ((cogsMethodId != null) && (cogsMethodId.equals("COGS_AVG_COST"))) {
                    BigDecimal prodAvgCost = UtilCOGS.getProductAverageCost(productId, ownerPartyId, userLogin, delegator, dispatcher);
                    if (prodAvgCost == null) {
                        Debug.logWarning("Unable to find a product average cost for product [" + productId + "] in organization [" + ownerPartyId + "], no adjustment will be made for outbound shipment", MODULE);
                    } else {
                        // TODO: there could be rounding issues here; maybe it's better to do something like this:
                        //       (prodAvgCost * quantityOnHandVar) - (unitCost * quantityOnHandVar) and then set the scale.
                        inventoryAdjAmount = prodAvgCost.subtract(unitCost).multiply(quantityIssued).setScale(decimals, rounding);
                    }
                }
                // Adjustment accounting transaction
                if ((inventoryAdjAmount != null) && (inventoryAdjAmount.compareTo(ZERO) != 0)) {
                    // GL accounts for cost adjustments due to Average Cost.  Right now we're going to use a separate INVENTORY AVG_COST adjustment, but the COGS adjustment just goes to COGS
                    String invGlAcctAdjId = UtilAccounting.getProductOrgGlAccountId(productId, "INV_ADJ_AVG_COST", ownerPartyId, delegator);
                    String invGlAcctAdjCogsId = UtilAccounting.getProductOrgGlAccountId(productId, "COGS_ADJ_AVG_COST", ownerPartyId, delegator);
                    // Transaction to credit the adj inventory GL acct
                    input = UtilMisc.toMap("glAccountId", invGlAcctAdjId, "debitCreditFlag", "C",
                        "amount", inventoryAdjAmount, "acctgTransEntrySeqId", Integer.toString(0),
                        "organizationPartyId", ownerPartyId, "acctgTransEntryTypeId", "_NA_");
                    input.put("productId", productId);
                    input.put("partyId", partyIdTo);
                    input.put("roleTypeId", "BILL_TO_CUSTOMER");
                    GenericValue creditAdjAcctTrans = delegator.makeValue("AcctgTransEntry", input);
                    // add the inventory item tags
                    UtilAccountingTags.putAllAccountingTags(inventoryItem, creditAdjAcctTrans);
                    transEntries.add(creditAdjAcctTrans);

                    // Transaction to debit the adj cogs GL acct
                    input = UtilMisc.toMap("glAccountId", invGlAcctAdjCogsId, "debitCreditFlag", "D",
                        "amount", inventoryAdjAmount, "acctgTransEntrySeqId", Integer.toString(1),
                        "organizationPartyId", ownerPartyId, "acctgTransEntryTypeId", "_NA_");
                    input.put("productId", productId);
                    input.put("partyId", partyIdTo);
                    input.put("roleTypeId", "BILL_TO_CUSTOMER");
                    GenericValue debitAdjAcctTrans = delegator.makeValue("AcctgTransEntry", input);
                    // add the inventory item tags
                    UtilAccountingTags.putAllAccountingTags(inventoryItem, debitAdjAcctTrans);
                    transEntries.add(debitAdjAcctTrans);
                }
            }
            // Perform the transaction
            input = UtilMisc.toMap("transactionDate", UtilDateTime.nowTimestamp(), "userLogin", userLogin);
            input.put("acctgTransEntries", transEntries);
            input.put("glFiscalTypeId", "ACTUAL");
            input.put("acctgTransTypeId", "SHIPMENT_OUT_ATX");
            input.put("shipmentId", shipmentId);

            Map servResult = dispatcher.runSync("createAcctgTransAndEntries", input);

            if (ServiceUtil.isError(servResult)) {
                return servResult;
            }

            Map result = ServiceUtil.returnSuccess();
            result.put("acctgTransId", servResult.get("acctgTransId"));

        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        } catch (GenericServiceException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Service to post a shipment receipt to GL.
     * The generated transaction entries are tagged the same as the order item received or the inventory item received which has the variance.
     *  (note: since in case of an order item receipt the tag are only copied as a seca, they are not available in the inventory item when this is called)
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map postShipmentReceiptToGl(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String receiptId = (String) context.get("receiptId");
        try {
            GenericValue receipt = delegator.findByPrimaryKey("ShipmentReceipt", UtilMisc.toMap("receiptId", receiptId));
            String shipmentId = receipt.getString("shipmentId");
            String productId = receipt.getString("productId");
            BigDecimal quantityReceived = receipt.getBigDecimal("quantityAccepted");
            GenericValue inventoryItem = receipt.getRelatedOne("InventoryItem");
            GenericValue orderItem = receipt.getRelatedOne("OrderItem");
            String inventoryItemId = inventoryItem.getString("inventoryItemId");

            // For now, skip serialized inventory items
            if ("SERIALIZED_INV_ITEM".equals(inventoryItem.get("inventoryItemTypeId"))) {
                Debug.logInfo("postShipmentReceiptToGl:  Encountered serialized InventoryItem [" + inventoryItemId + "].  Not posting this shipment receipt to GL.", MODULE);
                return ServiceUtil.returnSuccess();
            }

            // get the inventory item's owner and party
            String organizationPartyId = inventoryItem.getString("ownerPartyId");
            // get the inventory item unit cost
            BigDecimal unitCost = inventoryItem.getBigDecimal("unitCost");

            // Skip this inventory item if the owner is not an internal organization
            if (UtilValidate.isNotEmpty(organizationPartyId) && !UtilFinancial.hasPartyRole(organizationPartyId, "INTERNAL_ORGANIZATIO", delegator)) {
                Debug.logInfo("postShipmentReceiptToGl:  Owner Party [" + organizationPartyId + "] of InventoryItem [" + inventoryItemId + "] is not an internal organization.  Not posting this shipment receipt to GL.", MODULE);
                return ServiceUtil.returnSuccess();
            }

            // get the currency conversion factor
            BigDecimal conversionFactor = UtilFinancial.determineUomConversionFactor(delegator, dispatcher, organizationPartyId, inventoryItem.getString("currencyUomId"));

            // Use the shipment to get the origin party and determine if this is actually a return, in which case the offsetting account is COGS
            String originPartyId = null;
            String offsettingGlAccountTypeId = "UNINVOICED_SHIP_RCPT";
            if (shipmentId != null) {
                GenericValue shipment = receipt.getRelatedOne("Shipment");
                originPartyId = shipment.getString("partyIdFrom");
                if ((shipment != null) && (shipment.getString("shipmentTypeId").equals("SALES_RETURN"))) {
                    offsettingGlAccountTypeId = "COGS_ACCOUNT";
                }
            }
            // validate the unitCost and compute transaction amount
            if (unitCost == null) {
                return ServiceUtil.returnError("Could not determine unitCost of product [" + productId + "] from shipment receipt [" + receiptId + "]");
            }
            BigDecimal transactionAmount = unitCost.multiply(quantityReceived).multiply(conversionFactor).setScale(decimals, rounding);

            // get the inventory GL account
            String inventoryGlAccount = UtilAccounting.getProductOrgGlAccountId(productId, "INVENTORY_ACCOUNT", organizationPartyId, delegator);
            // get the offsetting gl account, either uninvoiced receipts for purchase shipments or cogs for returns shipments
            String uninvoicedGlAccountId = UtilAccounting.getProductOrgGlAccountId(productId, offsettingGlAccountTypeId, organizationPartyId, delegator);

            // get the accounting tags from the inventory item
            Map<String, String> tags = new HashMap<String, String>();
            if (orderItem != null) {
                UtilAccountingTags.putAllAccountingTags(orderItem, tags);
                Debug.logInfo("Making transaction entries with accounting tags from order item [" + orderItem + "] : " + tags, MODULE);
            } else if (inventoryItem != null) {
                UtilAccountingTags.putAllAccountingTags(inventoryItem, tags);
                Debug.logInfo("Making transaction entries with accounting tags from inventory item [" + inventoryItem + "] : " + tags, MODULE);
            }

            // Transaction to credit the uninvoiced receipts account
            Map input = UtilMisc.toMap("glAccountId", uninvoicedGlAccountId, "organizationPartyId", organizationPartyId, "partyId", originPartyId);
            input.put("productId", productId);
            input.put("amount", transactionAmount);
            input.put("acctgTransEntryTypeId", "_NA_");
            input.put("debitCreditFlag", "C");
            input.put("acctgTransEntrySeqId", Integer.toString(0));
            input.put("roleTypeId", "BILL_FROM_VENDOR");
            GenericValue creditAcctTrans = delegator.makeValue("AcctgTransEntry", input);
            // copy the accounting tags
            UtilAccountingTags.putAllAccountingTags(tags, creditAcctTrans);

            // Transaction to debit the inventory GL acct
            input = UtilMisc.toMap("glAccountId", inventoryGlAccount, "organizationPartyId", organizationPartyId, "partyId", originPartyId);
            input.put("productId", productId);
            input.put("amount", transactionAmount);
            input.put("acctgTransEntryTypeId", "_NA_");
            input.put("debitCreditFlag", "D");
            input.put("acctgTransEntrySeqId", Integer.toString(1));
            input.put("roleTypeId", "BILL_FROM_VENDOR");
            GenericValue debitAcctTrans = delegator.makeValue("AcctgTransEntry", input);
            // copy the accounting tags
            UtilAccountingTags.putAllAccountingTags(tags, debitAcctTrans);

            // Perform the transaction
            input = UtilMisc.toMap("transactionDate", UtilDateTime.nowTimestamp(), "partyId", originPartyId, "userLogin", userLogin);
            input.put("acctgTransEntries", UtilMisc.toList(creditAcctTrans, debitAcctTrans));
            input.put("glFiscalTypeId", "ACTUAL");
            input.put("acctgTransTypeId", "SHIPMENT_RCPT_ATX");
            input.put("receiptId", receiptId);
            if (shipmentId != null) {
                input.put("shipmentId", shipmentId);
            }
            Map servResult = dispatcher.runSync("createAcctgTransAndEntries", input);

            if (((String) servResult.get(ModelService.RESPONSE_MESSAGE)).equals(ModelService.RESPOND_SUCCESS)) {
                Map result = ServiceUtil.returnSuccess();
                result.put("acctgTransId", servResult.get("acctgTransId"));
                return result;
            } else {
                return servResult;
            }
        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        } catch (GenericServiceException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }
    }

    /**
     * Service to post an inventory transaction to GL to record the change of the owner of the inventory item.
     * The generated transaction entries are tagged the same as the inventory item which has the variance.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map postInventoryItemOwnerChange(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String inventoryItemId = (String) context.get("inventoryItemId");
        String originOwnerPartyId = (String) context.get("oldOwnerPartyId");
        try {
            Map tmpMap = new HashMap();

            GenericValue inventoryItem = delegator.findByPrimaryKey("InventoryItem", UtilMisc.toMap("inventoryItemId", inventoryItemId));
            if (inventoryItem == null) {
                return ServiceUtil.returnError("No InventoryItem entity record for inventoryItemId " + inventoryItemId);
            }
            Map<String, String> tags = new HashMap<String, String>();
            UtilAccountingTags.putAllAccountingTags(inventoryItem, tags);
            Debug.logInfo("Making transaction entries with accounting tags from inventory item [" + inventoryItem + "] : " + tags, MODULE);

            String destinationOwnerPartyId = inventoryItem.getString("ownerPartyId");
            String productId = inventoryItem.getString("productId");
            BigDecimal unitCost = inventoryItem.getBigDecimal("unitCost");
            BigDecimal quantity = inventoryItem.getBigDecimal("quantityOnHandTotal");
            if (quantity == null) {
                if ("SERIALIZED_INV_ITEM".equals(inventoryItem.getString("inventoryItemTypeId"))
                        && (("INV_PROMISED".equals(inventoryItem.getString("statusId")))
                        || ("INV_AVAILABLE".equals(inventoryItem.getString("statusId"))))) {
                    quantity = new BigDecimal(1.00);
                    Debug.logInfo("Assuming a quantity of 1.0 for serialized item [" + inventoryItemId + "]", MODULE);
                } else {
                    return ServiceUtil.returnError("Unable to perform ownership change accounting because non-serialized inventory item [" + inventoryItemId + "] has a null quantity on hand value");
                }
            }

            boolean destinationOwnerIsInternalOrg = UtilFinancial.hasPartyRole(destinationOwnerPartyId, "INTERNAL_ORGANIZATIO", delegator);
            boolean originOwnerIsInternalOrg = UtilFinancial.hasPartyRole(originOwnerPartyId, "INTERNAL_ORGANIZATIO", delegator);

            List transEntries = new ArrayList();

            // Create transactions for the destination owner if it is an internal organization that we're doing GL Accounting for
            if (destinationOwnerIsInternalOrg) {
                BigDecimal destinationConversionFactor = UtilFinancial.determineUomConversionFactor(delegator, dispatcher, destinationOwnerPartyId, inventoryItem.getString("currencyUomId"));
                BigDecimal destinationTransactionAmount = unitCost.multiply(destinationConversionFactor).multiply(quantity).setScale(decimals, rounding);
                String destinationInvGlAcctId = UtilAccounting.getProductOrgGlAccountId(productId, "INVENTORY_ACCOUNT", destinationOwnerPartyId, delegator);
                String destinationInvXferGlAcctId = UtilAccounting.getProductOrgGlAccountId(productId, "INVENTORY_XFER_IN", destinationOwnerPartyId, delegator);

                // Transaction to credit the inventory xfer in GL acct
                tmpMap = UtilMisc.toMap("glAccountId", destinationInvXferGlAcctId, "debitCreditFlag", "C",
                        "amount", destinationTransactionAmount, "acctgTransEntrySeqId", Integer.toString(0),
                        "organizationPartyId", destinationOwnerPartyId, "acctgTransEntryTypeId", "_NA_");
                tmpMap.put("productId", productId);
                GenericValue creditAcctTrans = delegator.makeValue("AcctgTransEntry", tmpMap);
                // copy the accounting tags
                UtilAccountingTags.putAllAccountingTags(tags, creditAcctTrans);
                transEntries.add(creditAcctTrans);

                // Transaction to debit the inventory GL acct
                tmpMap = UtilMisc.toMap("glAccountId", destinationInvGlAcctId, "debitCreditFlag", "D",
                        "amount", destinationTransactionAmount, "acctgTransEntrySeqId", Integer.toString(1),
                        "organizationPartyId", destinationOwnerPartyId, "acctgTransEntryTypeId", "_NA_");
                tmpMap.put("productId", productId);
                GenericValue debitAcctTrans = delegator.makeValue("AcctgTransEntry", tmpMap);
                // copy the accounting tags
                UtilAccountingTags.putAllAccountingTags(tags, debitAcctTrans);
                transEntries.add(debitAcctTrans);
            } else {
                Debug.logWarning("Inventory item [" + inventoryItemId + "] is being transferred to party [" + destinationOwnerPartyId + "] which is not an internal organization, so no transactions will be created for this party", MODULE);
            }

            // Create transactions for the origin owner if it is an internal organization that we're doing GL Accounting for
            if (originOwnerIsInternalOrg) {
                BigDecimal originConversionFactor = UtilFinancial.determineUomConversionFactor(delegator, dispatcher, originOwnerPartyId, inventoryItem.getString("currencyUomId"));
                BigDecimal originTransactionAmount = unitCost.multiply(originConversionFactor).multiply(quantity).setScale(decimals, rounding);
                String originInvGlAcctId = UtilAccounting.getProductOrgGlAccountId(productId, "INVENTORY_ACCOUNT", originOwnerPartyId, delegator);
                String originInvXferGlAcctId = UtilAccounting.getProductOrgGlAccountId(productId, "INVENTORY_XFER_OUT", originOwnerPartyId, delegator);

                // Transaction to credit the inventory GL acct
                tmpMap = UtilMisc.toMap("glAccountId", originInvGlAcctId, "debitCreditFlag", "C",
                        "amount", originTransactionAmount, "acctgTransEntrySeqId", Integer.toString(0),
                        "organizationPartyId", originOwnerPartyId, "acctgTransEntryTypeId", "_NA_");
                tmpMap.put("productId", productId);
                GenericValue creditAcctTrans = delegator.makeValue("AcctgTransEntry", tmpMap);
                // copy the accounting tags
                UtilAccountingTags.putAllAccountingTags(tags, creditAcctTrans);
                transEntries.add(creditAcctTrans);

                // Transaction to debit the inventory xfer out GL acct
                tmpMap = UtilMisc.toMap("glAccountId", originInvXferGlAcctId, "debitCreditFlag", "D",
                        "amount", originTransactionAmount, "acctgTransEntrySeqId", Integer.toString(1),
                        "organizationPartyId", originOwnerPartyId, "acctgTransEntryTypeId", "_NA_");
                tmpMap.put("productId", productId);
                GenericValue debitAcctTrans = delegator.makeValue("AcctgTransEntry", tmpMap);
                // copy the accounting tags
                UtilAccountingTags.putAllAccountingTags(tags, debitAcctTrans);
                transEntries.add(debitAcctTrans);

                // Get origin owner's party COGS method.  If method is COGS_AVG_COST, also compute the inventory adjustment amount = (prodAvgCost - unitCost) * quantity
                GenericValue acctgPref = delegator.findByPrimaryKeyCache("PartyAcctgPreference", UtilMisc.toMap("partyId", originOwnerPartyId));
                String cogsMethodId = acctgPref.getString("cogsMethodId");
                BigDecimal inventoryAdjAmount = null;
                if ((cogsMethodId != null) && (cogsMethodId.equals("COGS_AVG_COST"))) {
                    BigDecimal prodAvgCost = UtilCOGS.getProductAverageCost(productId, originOwnerPartyId, userLogin, delegator, dispatcher);
                    if (prodAvgCost == null) {
                        Debug.logWarning("Unable to find a product average cost for product [" + productId + "] in organization [" + originOwnerPartyId + "], no inventory adjustment will be made for inventory owner change", MODULE);
                    } else {
                       inventoryAdjAmount = prodAvgCost.subtract(unitCost.multiply(originConversionFactor)).multiply(quantity).setScale(decimals, rounding);
                    }
                }

                // Adjustment accounting transaction
                if ((inventoryAdjAmount != null) && (inventoryAdjAmount.compareTo(ZERO) != 0)) {
                    // GL accounts for cost adjustments due to Average Cost.  Right now we're going to use a separate INVENTORY AVG_COST adjustment, but the COGS adjustment just goes to COGS
                    String invGlAcctAdjId = UtilAccounting.getProductOrgGlAccountId(productId, "INV_ADJ_AVG_COST", originOwnerPartyId, delegator);
                    // Transaction to credit the adj inventory GL acct
                    tmpMap = UtilMisc.toMap("glAccountId", invGlAcctAdjId, "debitCreditFlag", "C",
                            "amount", inventoryAdjAmount, "acctgTransEntrySeqId", Integer.toString(0),
                            "organizationPartyId", originOwnerPartyId, "acctgTransEntryTypeId", "_NA_");
                    tmpMap.put("productId", productId);
                    creditAcctTrans = delegator.makeValue("AcctgTransEntry", tmpMap);
                    // copy the accounting tags
                    UtilAccountingTags.putAllAccountingTags(tags, creditAcctTrans);
                    transEntries.add(creditAcctTrans);

                    // Transaction to debit the adj cogs GL acct
                    tmpMap = UtilMisc.toMap("glAccountId", originInvXferGlAcctId, "debitCreditFlag", "D",
                            "amount", inventoryAdjAmount, "acctgTransEntrySeqId", Integer.toString(1),
                            "organizationPartyId", originOwnerPartyId, "acctgTransEntryTypeId", "_NA_");
                    tmpMap.put("productId", productId);
                    debitAcctTrans = delegator.makeValue("AcctgTransEntry", tmpMap);
                    // copy the accounting tags
                    UtilAccountingTags.putAllAccountingTags(tags, debitAcctTrans);
                    transEntries.add(debitAcctTrans);
                }
            } else {
                Debug.logWarning("Inventory item [" + inventoryItemId + "] is being transferred from party [" + destinationOwnerPartyId + "] which is not an internal organization, so no transactions will be created for this party", MODULE);
            }

            // Perform the transaction
            tmpMap = UtilMisc.toMap("acctgTransEntries", transEntries,
                    "glFiscalTypeId", "ACTUAL", "acctgTransTypeId", "INVENTORY",
                    "transactionDate", UtilDateTime.nowTimestamp(),
                    "userLogin", userLogin);
            tmpMap.put("inventoryItemId", inventoryItemId);
            //tmpMap.put("inventoryTransferId", inventoryTransferId);
            tmpMap = dispatcher.runSync("createAcctgTransAndEntries", tmpMap);

            if (ServiceUtil.isError(tmpMap)) {
                return tmpMap;
            }

            // Products average costs are updated for the destination owner party id.
            if (destinationOwnerIsInternalOrg) {
                tmpMap = dispatcher.runSync("updateProductAverageCost",
                        UtilMisc.toMap("organizationPartyId", destinationOwnerPartyId,
                            "productId", productId,
                            "userLogin", userLogin));
                if (ServiceUtil.isError(tmpMap)) {
                    return tmpMap;
                }
            }

            Map result = ServiceUtil.returnSuccess();
            result.put("acctgTransId", tmpMap.get("acctgTransId"));
            return result;

        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        } catch (GenericServiceException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }
    }

    /**
     * Service to post an inventory transaction to GL based on inventory item status changes.
     * The generated transaction entries are tagged the same as the inventory item which has the variance.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map postInventoryItemStatusChange(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String inventoryItemId = (String) context.get("inventoryItemId");
        String oldStatusId = (String) context.get("oldStatusId");

        // if old status was null, assume it is On Order
        if (oldStatusId == null || oldStatusId.trim().length() == 0) {
            oldStatusId = "INV_ON_ORDER";
        }

        try {
            // get the inventory item and data useful to this service
            GenericValue inventoryItem = delegator.findByPrimaryKey("InventoryItem", UtilMisc.toMap("inventoryItemId", inventoryItemId));
            Map<String, String> tags = new HashMap<String, String>();
            UtilAccountingTags.putAllAccountingTags(inventoryItem, tags);
            Debug.logInfo("Making transaction entries with accounting tags from inventory item [" + inventoryItem + "] : " + tags, MODULE);
            String ownerPartyId = inventoryItem.getString("ownerPartyId");
            String productId = inventoryItem.getString("productId");
            String statusId = inventoryItem.getString("statusId");

            // if not a serialized item, simply skip and do nothing
            if (!"SERIALIZED_INV_ITEM".equals(inventoryItem.get("inventoryItemTypeId"))) {
                return ServiceUtil.returnSuccess();
            }

            // Skip this inventory item if the owner is not an internal organization
            if (UtilValidate.isNotEmpty(ownerPartyId) && !UtilFinancial.hasPartyRole(ownerPartyId, "INTERNAL_ORGANIZATIO", delegator)) {
                Debug.logInfo("postInventoryItemStatusChange:  Owner Party [" + ownerPartyId + "] of InventoryItem [" + inventoryItemId + "] is not an internal organization.  Not posting InventoryItem status change [" + oldStatusId + "->" + statusId + "] to GL.", MODULE);
                return ServiceUtil.returnSuccess();
            }

            // get the account types configured for this status change
            GenericValue config = delegator.findByPrimaryKey("InventoryStatusGlAccountType", UtilMisc.toMap("statusIdFrom", oldStatusId, "statusIdTo", statusId));
            if (config == null) {
                Debug.logWarning("Not posting serialized inventory status change to GL: InventoryItem status transition [" + oldStatusId + "] to [" + statusId + "] not supported.", MODULE);
                return ServiceUtil.returnSuccess();
            }

            // if the debit and/or credit account types are not defined, then it means we just ignore this status transition
            if (config.get("debitGlAccountTypeId") == null || config.get("creditGlAccountTypeId") == null) {
                return ServiceUtil.returnSuccess();
            }

            // get the debit and credit accounts based on our account types and the owner of the InventoryItem
            String debitGlAccountId = UtilAccounting.getProductOrgGlAccountId(productId, config.getString("debitGlAccountTypeId"), ownerPartyId, delegator);
            String creditGlAccountId = UtilAccounting.getProductOrgGlAccountId(productId, config.getString("creditGlAccountTypeId"), ownerPartyId, delegator);

            // determine and convert transaction amount (quantity is always 1.0 for serialized inventory items)
            BigDecimal amount = inventoryItem.getBigDecimal("unitCost");
            BigDecimal conversionFactor = UtilFinancial.determineUomConversionFactor(delegator, dispatcher, ownerPartyId, inventoryItem.getString("currencyUomId"));
            amount = amount.multiply(conversionFactor).setScale(decimals, rounding);

            // transaction list
            List transEntries = new ArrayList();

            // Credit transaction
            Map input = UtilMisc.toMap("glAccountId", creditGlAccountId, "organizationPartyId", ownerPartyId);
            input.put("productId", productId);
            input.put("amount", amount);
            input.put("acctgTransEntryTypeId", "_NA_");
            input.put("debitCreditFlag", "C");
            input.put("acctgTransEntrySeqId", Integer.toString(0));
            GenericValue creditAcctTrans = delegator.makeValue("AcctgTransEntry", input);
            // copy the accounting tags
            UtilAccountingTags.putAllAccountingTags(tags, creditAcctTrans);
            transEntries.add(creditAcctTrans);

            // Debit transaction
            input = UtilMisc.toMap("glAccountId", debitGlAccountId, "organizationPartyId", ownerPartyId);
            input.put("productId", productId);
            input.put("amount", amount);
            input.put("acctgTransEntryTypeId", "_NA_");
            input.put("debitCreditFlag", "D");
            input.put("acctgTransEntrySeqId", Integer.toString(1));
            GenericValue debitAcctTrans = delegator.makeValue("AcctgTransEntry", input);
            // copy the accounting tags
            UtilAccountingTags.putAllAccountingTags(tags, debitAcctTrans);
            transEntries.add(debitAcctTrans);

            // Perform the transaction
            input = UtilMisc.toMap("transactionDate", UtilDateTime.nowTimestamp(), "userLogin", userLogin);
            input.put("inventoryItemId", inventoryItemId);
            input.put("acctgTransEntries", transEntries);
            input.put("glFiscalTypeId", "ACTUAL");
            input.put("acctgTransTypeId", "INVENTORY");
            Map servResult = dispatcher.runSync("createAcctgTransAndEntries", input);
            if (ServiceUtil.isError(servResult)) {
                return servResult;
            }
            String acctgTransId = (String) servResult.get("acctgTransId");

            // Update the product's average cost
            servResult = dispatcher.runSync("updateProductAverageCost",
                                        UtilMisc.toMap("organizationPartyId", ownerPartyId,
                                                       "productId", productId,
                                                       "userLogin", userLogin));
            if (ServiceUtil.isError(servResult)) {
                return servResult;
            }

            Map results = ServiceUtil.returnSuccess();
            results.put("acctgTransId", acctgTransId);
            return results;
        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        } catch (GenericServiceException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }
    }

    /**
     * If any accounting tag of the InventoryItem change, it will post a "neutral" GL transaction to Debit and Credit INVENTORY_ACCOUNT
     * of the InventoryItem current value.
     * Only the debit entry is tagged using the old tags, and the credit transaction is tagged with the current inventory item tags.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map postInventoryItemAccountingTagChange(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        Map result = ServiceUtil.returnSuccess();

        String inventoryItemId = (String) context.get("inventoryItemId");

        try {

            // Get the inventory item
            GenericValue inventoryItem = delegator.findByPrimaryKey("InventoryItem", UtilMisc.toMap("inventoryItemId", inventoryItemId));
            if (UtilValidate.isEmpty(inventoryItem)) {
                return UtilMessage.createAndLogServiceError("OpentapsError_InventoryItemNotFound", context, locale, MODULE);
            }
            // get the current (new) inventory item tags
            Map<String, String> tags = new HashMap<String, String>();
            UtilAccountingTags.putAllAccountingTags(inventoryItem, tags);
            Debug.logInfo("Making transaction entries with accounting tags from inventory item [" + inventoryItem + "] : " + tags, MODULE);
            // get the old accounting tags
            Map<String, String> oldTags = UtilAccountingTags.getTagParameters(context, "oldTag");
            Debug.logInfo("Found old Tags [" + oldTags + "]", MODULE);

            // Ignore if no tag changed
            if (UtilAccountingTags.sameAccountingTags(inventoryItem, oldTags, "tag")) {
                Debug.logInfo("No accounting tag changed.", MODULE);
                return result;
            }

            // unit cost and owner
            String ownerPartyId = inventoryItem.getString("ownerPartyId");
            BigDecimal unitCost = UtilValidate.isNotEmpty(inventoryItem.get("unitCost")) ? inventoryItem.getBigDecimal("unitCost").setScale(4, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;

            // Ignore the change if the owner is not an internal organization
            if (UtilValidate.isNotEmpty(ownerPartyId) && !PartyHelper.isInternalOrganization(ownerPartyId, delegator)) {
                UtilMessage.logServiceInfo("FinancialsServiceError_InventoryItemValueAdj_OwnerNotInternal", inventoryItem, locale, MODULE);
                return result;
            }

            // Return if the unit cost is zero or not available
            if (UtilValidate.isEmpty(unitCost) || unitCost.compareTo(BigDecimal.ZERO) == 0) {
                UtilMessage.logServiceInfo("FinancialsServiceWarning_InventoryItemValueAdj_NoPreviousUnitCost", context, locale, MODULE);
                return result;
            }

            BigDecimal qoh = inventoryItem.getBigDecimal("quantityOnHandTotal");

            // Return if the quantity on hand is zero
            if (UtilValidate.isEmpty(qoh) || qoh.compareTo(BigDecimal.ZERO) == 0) {
                UtilMessage.logServiceInfo("FinancialsServiceWarning_InventoryItemValueAdj_ZeroOnHand", context, locale, MODULE);
                return result;
            }

            // Adjustment amount is the unit cost multiplied by the quantity on hand
            BigDecimal adjustmentAmount = unitCost.multiply(qoh).setScale(decimals, rounding);

            // Retrieve the GlAccountIds for the credit and debit types
            String glAccountId = UtilFinancial.getOrgGlAccountId(ownerPartyId, "INVENTORY_ACCOUNT", delegator);
            if (UtilValidate.isEmpty(glAccountId)) {
                UtilMessage.logServiceInfo("FinancialsServiceErrorNoGlAccountTypeDefaultFound", UtilMisc.toMap("organizationPartyId", ownerPartyId, "glAccountTypeId", "INVENTORY_ACCOUNT"), locale, MODULE);
                return result;
            }

            // Credit transaction
            Map acctgTransEntry = UtilMisc.toMap("glAccountId", glAccountId, "organizationPartyId", ownerPartyId);
            acctgTransEntry.put("productId", inventoryItem.getString("productId"));
            acctgTransEntry.put("amount", adjustmentAmount);
            acctgTransEntry.put("acctgTransEntryTypeId", "_NA_");
            acctgTransEntry.put("debitCreditFlag", "C");
            acctgTransEntry.put("acctgTransEntrySeqId", Integer.toString(0));
            GenericValue creditAcctTrans = delegator.makeValue("AcctgTransEntry", acctgTransEntry);
            // copy the new accounting tags
            UtilAccountingTags.putAllAccountingTags(tags, creditAcctTrans);

            // Debit transaction
            acctgTransEntry = UtilMisc.toMap("glAccountId", glAccountId, "organizationPartyId", ownerPartyId);
            acctgTransEntry.put("productId", inventoryItem.getString("productId"));
            acctgTransEntry.put("amount", adjustmentAmount);
            acctgTransEntry.put("acctgTransEntryTypeId", "_NA_");
            acctgTransEntry.put("debitCreditFlag", "D");
            acctgTransEntry.put("acctgTransEntrySeqId", Integer.toString(1));
            GenericValue debitAcctTrans = delegator.makeValue("AcctgTransEntry", acctgTransEntry);
            // copy the accounting tags
            UtilAccountingTags.putAllAccountingTags(oldTags, debitAcctTrans, "tag");

            // Perform the transaction
            acctgTransEntry = UtilMisc.toMap("transactionDate", UtilDateTime.nowTimestamp(), "userLogin", userLogin);
            acctgTransEntry.put("inventoryItemId", inventoryItemId);
            acctgTransEntry.put("acctgTransEntries", UtilMisc.toList(creditAcctTrans, debitAcctTrans));
            acctgTransEntry.put("glFiscalTypeId", "ACTUAL");
            acctgTransEntry.put("acctgTransTypeId", "INVENTORY");
            Map createTransResult = dispatcher.runSync("createAcctgTransAndEntries", acctgTransEntry);
            if (ServiceUtil.isError(createTransResult)) {
                return createTransResult;
            }
            String acctgTransId = (String) createTransResult.get("acctgTransId");

            result.put("acctgTransId", acctgTransId);

        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        } catch (GenericServiceException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }

        return result;
    }

    /**
     * If InventoryItem.unitCost is changed, it will post a GL transaction to Debit INVENTORY_VAL_ADJ and Credit INVENTORY_ACCOUNT
     *  of the InventoryItem.productId by the amount of the unitCost change * the quantity on hand of the inventory item (or 1.0 if
     *  it's serialized and Available). Unit cost change is calculated by referring to the most recent value in InventoryItemValueHistory
     *  which is *earlier* than the modification date of the InventoryItem itself. If no previous unit cost history is found, no GL
     *  posting will be made.
     * The generated transaction entries are tagged the same as the inventory item which has the variance.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map postInventoryItemValueAdjustment(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        Map result = ServiceUtil.returnSuccess();

        String inventoryItemId = (String) context.get("inventoryItemId");

        try {

            // Get the inventory item
            GenericValue inventoryItem = delegator.findByPrimaryKey("InventoryItem", UtilMisc.toMap("inventoryItemId", inventoryItemId));
            if (UtilValidate.isEmpty(inventoryItem)) {
                return UtilMessage.createAndLogServiceError("OpentapsError_InventoryItemNotFound", context, locale, MODULE);
            }
            // get the inventory item tags
            Map<String, String> tags = new HashMap<String, String>();
            UtilAccountingTags.putAllAccountingTags(inventoryItem, tags);
            Debug.logInfo("Making transaction entries with accounting tags from inventory item [" + inventoryItem + "] : " + tags, MODULE);
            String ownerPartyId = inventoryItem.getString("ownerPartyId");
            BigDecimal newUnitCost = UtilValidate.isNotEmpty(inventoryItem.get("unitCost")) ? inventoryItem.getBigDecimal("unitCost").setScale(4, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO ;

            // Get the last recorded unitCost for the inventoryItem which is *earlier* than the modification date of the InventoryItem
            // domain repository
            DomainsLoader dl = new DomainsLoader(new Infrastructure(dispatcher), new User(userLogin));
            InventoryRepositoryInterface inventoryRepository = dl.loadDomainsDirectory().getInventoryDomain().getInventoryRepository();
            InventoryItem domainInventoryItem = Repository.loadFromGeneric(InventoryItem.class, inventoryItem, inventoryRepository);
            BigDecimal inventoryItemOldUnitCost = domainInventoryItem.getOldUnitCost();
            BigDecimal oldUnitCost = inventoryItemOldUnitCost == null ? null : inventoryItemOldUnitCost.setScale(4, BigDecimal.ROUND_HALF_UP);

            // If there's no record of the previous unit cost, throw an error
            if (UtilValidate.isEmpty(oldUnitCost)) {
            	UtilMessage.logServiceWarning("FinancialsServiceWarning_InventoryItemValueAdj_NoPreviousUnitCost", context, locale, MODULE);
            	return result;
            }

            // Return if the unitCost hasn't changed
            if (newUnitCost.compareTo(oldUnitCost) == 0) {
                UtilMessage.logServiceInfo("FinancialsServiceWarning_InventoryItemValueAdj_UnitCostNotChanged", context, locale, MODULE);
                return result;
            }

            // Ignore the change if the owner is not an internal organization
            if (UtilValidate.isNotEmpty(ownerPartyId) && !PartyHelper.isInternalOrganization(ownerPartyId, delegator)) {
                UtilMessage.logServiceInfo("FinancialsServiceError_InventoryItemValueAdj_OwnerNotInternal", inventoryItem, locale, MODULE);
                return result;
            }

            // Ignore the change if the InventoryItem is serialized and not available
            if ("SERIALIZED_INV_ITEM".equalsIgnoreCase(inventoryItem.getString("inventoryItemTypeId")) && !"INV_AVAILABLE".equalsIgnoreCase(inventoryItem.getString("statusId"))) {
                UtilMessage.logServiceInfo("FinancialsServiceWarning_InventoryItemValueAdj_SerInvItemNotAvail", inventoryItem, locale, MODULE);
                return result;
            }

            BigDecimal qoh = inventoryItem.getBigDecimal("quantityOnHandTotal");

            // Return if the quantity on hand is zero
            if (UtilValidate.isEmpty(qoh) || qoh.compareTo(BigDecimal.ZERO) == 0) {
                UtilMessage.logServiceInfo("FinancialsServiceWarning_InventoryItemValueAdj_ZeroOnHand", context, locale, MODULE);
                return result;
            }

            // Adjustment amount is the difference in unit costs multiplied by the quantity on hand
            BigDecimal adjustmentAmount = oldUnitCost.subtract(newUnitCost).multiply(qoh).setScale(decimals, rounding);

            // Retrieve the GlAccountIds for the credit and debit types
            String creditGlAccountId = UtilFinancial.getOrgGlAccountId(ownerPartyId, "INVENTORY_ACCOUNT", delegator);
            if (UtilValidate.isEmpty(creditGlAccountId)) {
                UtilMessage.logServiceInfo("FinancialsServiceErrorNoGlAccountTypeDefaultFound", UtilMisc.toMap("organizationPartyId", ownerPartyId, "glAccountTypeId", "INVENTORY_ACCOUNT"), locale, MODULE);
                return result;
            }
            String debitGlAccountId = UtilFinancial.getOrgGlAccountId(ownerPartyId, "INVENTORY_VAL_ADJ", delegator);
            if (UtilValidate.isEmpty(debitGlAccountId)) {
                UtilMessage.logServiceInfo("FinancialsServiceErrorNoGlAccountTypeDefaultFound", UtilMisc.toMap("organizationPartyId", ownerPartyId, "glAccountTypeId", "INVENTORY_VAL_ADJ"), locale, MODULE);
                return result;
            }

            // Credit transaction
            Map acctgTransEntry = UtilMisc.toMap("glAccountId", creditGlAccountId, "organizationPartyId", ownerPartyId);
            acctgTransEntry.put("productId", inventoryItem.getString("productId"));
            acctgTransEntry.put("amount", adjustmentAmount);
            acctgTransEntry.put("acctgTransEntryTypeId", "_NA_");
            acctgTransEntry.put("debitCreditFlag", "C");
            acctgTransEntry.put("acctgTransEntrySeqId", Integer.toString(0));
            GenericValue creditAcctTrans = delegator.makeValue("AcctgTransEntry", acctgTransEntry);
            // copy the accounting tags
            UtilAccountingTags.putAllAccountingTags(tags, creditAcctTrans);

            // Debit transaction
            acctgTransEntry.put("glAccountId", debitGlAccountId);
            acctgTransEntry.put("debitCreditFlag", "D");
            acctgTransEntry.put("acctgTransEntrySeqId", Integer.toString(1));
            GenericValue debitAcctTrans = delegator.makeValue("AcctgTransEntry", acctgTransEntry);
            // copy the accounting tags
            UtilAccountingTags.putAllAccountingTags(tags, debitAcctTrans);

            // Perform the transaction
            acctgTransEntry = UtilMisc.toMap("transactionDate", UtilDateTime.nowTimestamp(), "userLogin", userLogin);
            acctgTransEntry.put("inventoryItemId", inventoryItemId);
            acctgTransEntry.put("acctgTransEntries", UtilMisc.toList(creditAcctTrans, debitAcctTrans));
            acctgTransEntry.put("glFiscalTypeId", "ACTUAL");
            acctgTransEntry.put("acctgTransTypeId", "INVENTORY");
            Map createTransResult = dispatcher.runSync("createAcctgTransAndEntries", acctgTransEntry);
            if (ServiceUtil.isError(createTransResult)) {
                return createTransResult;
            }
            String acctgTransId = (String) createTransResult.get("acctgTransId");

            result.put("acctgTransId", acctgTransId);

            // Update the product average cost
            dispatcher.runSync("updateProductAverageCost", UtilMisc.toMap("productId", inventoryItem.getString("productId"), "organizationPartyId", ownerPartyId, "userLogin", userLogin));

        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        } catch (GenericServiceException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        } catch (RepositoryException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }

        return result;
    }

    /**
     * Posts raw materials inventory issuances (to a WorkEffort) to the GL:
     * Debit WIP Inventory, Credit Raw Materials Inventory.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     * @deprecated
     */
    @SuppressWarnings("unchecked")
    public static Map postRawMaterialIssuancesToGl(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        Debug.logWarning("### WARNING ### The postRawMaterialIssuancesToGl service is deprecated.  Please use postInventoryToWorkEffortAssignToGl instead.", MODULE);

        String workEffortId = (String) context.get("workEffortId");
        String finishedProductId = (String) context.get("finishedProductId");
        List acctgTransIds = FastList.newInstance();
        try {
            GenericValue workEffort = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId));
            GenericValue facility = workEffort.getRelatedOne("Facility");
            if (facility == null) {
                return ServiceUtil.returnError("Could not find the facility for work effort [" + workEffortId + "]");
            }
            String ownerPartyId = facility.getString("ownerPartyId");

            List issuances = delegator.findByAnd("WorkEffortInventoryAssign", UtilMisc.toMap("workEffortId", workEffortId));
            Iterator issuancesIt = issuances.iterator();
            while (issuancesIt.hasNext()) {
                GenericValue itemIssuance = (GenericValue) issuancesIt.next();
                GenericValue inventoryItem = itemIssuance.getRelatedOne("InventoryItem");

                String inventoryOwnerPartyId = inventoryItem.getString("ownerPartyId");
                if (!inventoryOwnerPartyId.equals(ownerPartyId)) {
                    return ServiceUtil.returnError("The inventory item [" + inventoryOwnerPartyId + "] is not owned by the owner of the facility [" + facility.getString("facilityId") + "] in item issuance [" + itemIssuance.getString("itemIssuanceId") + "]");
                }

                // post the inventory issued
                Map input = UtilMisc.toMap("userLogin", userLogin);
                input.put("workEffortId", workEffortId);
                input.put("inventoryItemId", inventoryItem.get("inventoryItemId"));
                input.put("finishedProductId", finishedProductId);
                input.put("quantity", itemIssuance.get("quantity"));
                Map results = dispatcher.runSync("postInventoryToWorkEffortAssignToGl", input);
                if (ServiceUtil.isError(results)) {
                    return results;
                }
                acctgTransIds.add(results.get("acctgTransId"));
            }

            Map result = ServiceUtil.returnSuccess();
            result.put("acctgTransIds", acctgTransIds);
            return result;
        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        } catch (GenericServiceException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }
    }

    /**
     * Posts inventory issuance (to a WorkEffort) of given quantity to the GL: Debit WIP Inventory, Credit Raw Materials Inventory.
     * The WIP Inventory product is specified by finishedProductId and if it is not specified, then the service will attempt to
     * look up a productoin run corresponding to the given workEffortId.  If the quantity is not supplied, then the entire
     * WorkEffortInventoryAssign.quantity is used.
     *
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> postInventoryToWorkEffortAssignToGl(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String workEffortId = (String) context.get("workEffortId");
        String inventoryItemId = (String) context.get("inventoryItemId");
        BigDecimal quantityIssued = new BigDecimal((Double) context.get("quantity"));
        String finishedProductId = (String) context.get("finishedProductId");
        try {
            GenericValue inventoryItem = delegator.findByPrimaryKey("InventoryItem", UtilMisc.toMap("inventoryItemId", inventoryItemId));
            String ownerPartyId = inventoryItem.getString("ownerPartyId");
            String productId = inventoryItem.getString("productId");
            BigDecimal unitCost = inventoryItem.getBigDecimal("unitCost");
            if (unitCost == null) {
                return ServiceUtil.returnError("Cannot post inventory assignment to work effort:  No unit cost for product [" + productId + "] defined for inventory item [" + inventoryItemId + "].");
            }

            // get the production run task and production run header
            GenericValue workEffort = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId));
            if (UtilValidate.isEmpty(workEffort)) {
                return ServiceUtil.returnError("No workeffort found for workEffortId [" + workEffortId + "]");
            }
            GenericValue productionRun = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", workEffort.getString("workEffortParentId")));
            if (UtilValidate.isEmpty(productionRun)) {
                return ServiceUtil.returnError("No parent production run found for workEffortId [" + workEffortId + "]");
            }
            OpentapsProductionRun productionRunReader = new OpentapsProductionRun(productionRun.getString("workEffortId"), dispatcher);

            // convert the intentory item's unit cost into the owner's currency
            BigDecimal conversionFactor = UtilFinancial.determineUomConversionFactor(delegator, dispatcher, ownerPartyId, inventoryItem.getString("currencyUomId"));
            unitCost = unitCost.multiply(conversionFactor).setScale(decimals, rounding);
            BigDecimal transactionAmount = unitCost.multiply(quantityIssued).setScale(decimals, rounding);

            // Get owner's party COGS method.  If method is COGS_AVG_COST, also compute the inventory adjustment amount = (prodAvgCost - unitCost) * quantityIssued
            BigDecimal inventoryAdjAmount = null;
            if ("COGS_AVG_COST".equals(UtilCommon.getOrgCOGSMethodId(ownerPartyId, delegator))) {
                BigDecimal prodAvgCost = UtilCOGS.getProductAverageCost(productId, ownerPartyId, userLogin, delegator, dispatcher);
                if (prodAvgCost == null) {
                   Debug.logWarning("Unable to find a product average cost for product [" + productId + "] in organization [" + ownerPartyId + "], no adjustment will be made for item issuance", MODULE);
                } else {
                   // TODO: there could be rounding issues here; maybe it's better to do something like this:
                   //       (prodAvgCost - unitCost) * quantityOnHandVar and then set the scale.
                   inventoryAdjAmount = prodAvgCost.subtract(unitCost).multiply(quantityIssued).setScale(decimals, rounding);
                }
            }

            List transEntries = FastList.newInstance();
            if (productionRunReader.isAssembly()) {
                transEntries = getAcctgTransEntriesForInventoryIssuanceToAssembly(productionRunReader, productId, finishedProductId, ownerPartyId, transactionAmount, inventoryAdjAmount, delegator);
            } else {
                transEntries = getAcctgTransEntriesForInventoryIssuanceToDisassembly(productionRunReader, productId, ownerPartyId, transactionAmount, inventoryAdjAmount, inventoryItem.getString("currencyUomId"), dispatcher);
            }

            if (UtilValidate.isEmpty(transEntries)) {
                return ServiceUtil.returnError("Cannot post entries for issuance of inventory item [" + inventoryItemId + "] to work effort [" + workEffortId + "].  No transaction entries were created");
            }

            // Perform the transaction
            Map input = UtilMisc.toMap("transactionDate", UtilDateTime.nowTimestamp(), "userLogin", userLogin);
            input.put("acctgTransEntries", transEntries);
            input.put("glFiscalTypeId", "ACTUAL");
            input.put("acctgTransTypeId", "MANUFACTURING_ATX");
            input.put("workEffortId", workEffortId);
            input.put("inventoryItemId", inventoryItemId);
            return dispatcher.runSync("createAcctgTransAndEntries", input);
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }
    }

    /**
     * A convenience method re-factored to get the Debit/Credit AcctgTransEntries for posting the issuance of an inventory item to an assembly production run.
     * @param productionRunReader an <code>OpentapsProductionRun</code> value
     * @param productId a <code>String</code> value
     * @param finishedProductId a <code>String</code> value
     * @param ownerPartyId a <code>String</code> value
     * @param transactionAmount a <code>BigDecimal</code> value
     * @param inventoryAdjAmount a <code>BigDecimal</code> value
     * @param delegator a <code>Delegator</code> value
     * @return a <code>List</code> value
     * @exception AccountingException if an error occurs
     */
    @SuppressWarnings("unchecked")
    private static List getAcctgTransEntriesForInventoryIssuanceToAssembly(OpentapsProductionRun productionRunReader, String productId, String finishedProductId,
                                                                           String ownerPartyId, BigDecimal transactionAmount, BigDecimal inventoryAdjAmount, Delegator delegator) throws AccountingException {
        List transEntries = new ArrayList();

        // The finished product is retrieved from the production run
        if (UtilValidate.isEmpty(finishedProductId)) {
            GenericValue finishedProduct = productionRunReader.getProductProduced();
            finishedProductId = finishedProduct.getString("productId");
        }
        if (UtilValidate.isEmpty(finishedProductId)) {
            Debug.logError("No products to produce found.  Production run item issuance cannot be posted to ledger", MODULE);
            return null;
        }

        // Inventory GL account
        String creditAccountId = UtilAccounting.getProductOrgGlAccountId(productId, "RAWMAT_INVENTORY", ownerPartyId, delegator);
        String debitAccountId = UtilAccounting.getProductOrgGlAccountId(productId, "WIP_INVENTORY", ownerPartyId, delegator);

        // Transaction to credit the inventory account
        Map input = UtilMisc.toMap("glAccountId", creditAccountId, "organizationPartyId", ownerPartyId);
        input.put("productId", productId);
        input.put("amount", new Double(transactionAmount.doubleValue()));
        input.put("acctgTransEntryTypeId", "_NA_");
        input.put("debitCreditFlag", "C");
        input.put("acctgTransEntrySeqId", Integer.toString(0));
        GenericValue creditAcctTrans = delegator.makeValue("AcctgTransEntry", input);
        transEntries.add(creditAcctTrans);

        // Transaction to debit the Wip account
        input = UtilMisc.toMap("glAccountId", debitAccountId, "organizationPartyId", ownerPartyId);
        input.put("productId", finishedProductId);
        input.put("amount", new Double(transactionAmount.doubleValue()));
        input.put("acctgTransEntryTypeId", "_NA_");
        input.put("debitCreditFlag", "D");
        input.put("acctgTransEntrySeqId", Integer.toString(1));
        GenericValue debitAcctTrans = delegator.makeValue("AcctgTransEntry", input);
        transEntries.add(debitAcctTrans);

        // Adjustment for difference Average Cost Inventory
        if ((inventoryAdjAmount != null) && (inventoryAdjAmount.compareTo(ZERO) != 0)) {
            String debitGlAcctId = UtilAccounting.getProductOrgGlAccountId(productId, "WIP_INVENTORY", ownerPartyId, delegator);
            String creditGlAcctId = UtilAccounting.getProductOrgGlAccountId(productId, "RAWMAT_INVENTORY", ownerPartyId, delegator);
            // Transaction to credit the raw materials account
            input = UtilMisc.toMap("glAccountId", creditGlAcctId, "debitCreditFlag", "C",
                    "amount", new Double(inventoryAdjAmount.doubleValue()), "acctgTransEntrySeqId", Integer.toString(0),
                    "organizationPartyId", ownerPartyId, "acctgTransEntryTypeId", "_NA_");
            input.put("productId", productId);
            transEntries.add(delegator.makeValue("AcctgTransEntry", input));

            // Transaction to debit the work in progress account
            input = UtilMisc.toMap("glAccountId", debitGlAcctId, "debitCreditFlag", "D",
                    "amount", new Double(inventoryAdjAmount.doubleValue()), "acctgTransEntrySeqId", Integer.toString(1),
                    "organizationPartyId", ownerPartyId, "acctgTransEntryTypeId", "_NA_");
            input.put("productId", finishedProductId);
            transEntries.add(delegator.makeValue("AcctgTransEntry", input));
        }

        return transEntries;
    }

    /**
     * Build list of transaction entries for an item issued to a disassembly.
     * @param productionRunReader an <code>OpentapsProductionRun</code> value
     * @param productId a <code>String</code> value
     * @param ownerPartyId a <code>String</code> value
     * @param transactionAmount a <code>BigDecimal</code> value
     * @param inventoryAdjAmount a <code>BigDecimal</code> value
     * @param currencyUomId a <code>String</code> value
     * @param dispatcher a <code>LocalDispatcher</code> value
     * @return a <code>List</code> value
     * @exception GenericEntityException if an error occurs
     * @exception GenericServiceException if an error occurs
     */
    @SuppressWarnings("unchecked")
    private static List getAcctgTransEntriesForInventoryIssuanceToDisassembly(OpentapsProductionRun productionRunReader, String productId,
                                                                               String ownerPartyId, BigDecimal transactionAmount, BigDecimal inventoryAdjAmount, String currencyUomId, LocalDispatcher dispatcher)
                                                                throws GenericEntityException, GenericServiceException {
        int entryCounter = 0;
        List transEntries = new ArrayList();
        Delegator delegator = dispatcher.getDelegator();
        GenericValue productDisassembled = productionRunReader.getProductOfProductionRun();

        if (productionRunReader.getProductsToProduce() == null) {
            Debug.logError("No products to produce found.  Production run item issuance cannot be posted to ledger", MODULE);
        }

        // we need one amount with both inventory unit cost and COGS adjustment amount to allocate to all the items
        if ((inventoryAdjAmount != null) && (inventoryAdjAmount.compareTo(ZERO) != 0)) {
            transactionAmount = transactionAmount.add(inventoryAdjAmount).setScale(decimals, rounding);
        }

        // When an item is disassembled, it is usually a finished good and should be credited from the INVENTORY_ACCOUNT
        GenericValue transEntry = delegator.makeValue("AcctgTransEntry", UtilMisc.<String, Object>toMap("organizationPartyId", ownerPartyId, "productId", productId, "acctgTransEntryTypeId", "_NA_",
                "debitCreditFlag", "C", "amount", transactionAmount.doubleValue(), "glAccountId", UtilAccounting.getProductOrgGlAccountId(productId, "INVENTORY_ACCOUNT", ownerPartyId, delegator)));
        transEntry.put("acctgTransEntrySeqId", Integer.toString(entryCounter++));
        transEntries.add(transEntry);

        // then, for each item, debit its WIP inventory at a conservative valuation, and add it to the total tally of debited inventory value
        Map<String, BigDecimal> productsToProduce = productionRunReader.getProductsToProduce();
        BigDecimal inventoryValueOfProducts = ZERO;
        for (String nextProductIdProduced : productsToProduce.keySet()) {
            if (productsToProduce.get(nextProductIdProduced) == null) {
                Debug.logWarning("No quantity to produce found for [" + nextProductIdProduced + "], skipping it for inventory posting", MODULE);
                continue;
            }

            BigDecimal conservativeValue = UtilProduct.getConservativeValue(nextProductIdProduced, currencyUomId, dispatcher);
            if (conservativeValue == null) {
                conservativeValue = BigDecimal.ZERO;
                Debug.logWarning("Assumed a conservative inventory value of zero for [" + productId + "]", MODULE);
            }
            // value of this product is the unit conservative value of product * quantity to be produced
            BigDecimal quantityOfThisProduct = productsToProduce.get(nextProductIdProduced);
            BigDecimal valueOfThisProduct = conservativeValue.multiply(quantityOfThisProduct).setScale(decimals, rounding);
            transEntry = delegator.makeValue("AcctgTransEntry", UtilMisc.toMap("organizationPartyId", ownerPartyId, "productId", nextProductIdProduced, "acctgTransEntryTypeId", "_NA_",
                "debitCreditFlag", "D", "amount", new Double(valueOfThisProduct.doubleValue()), "glAccountId", UtilAccounting.getProductOrgGlAccountId(productId, "WIP_INVENTORY", ownerPartyId, delegator)));
            transEntry.put("acctgTransEntrySeqId", Integer.toString(entryCounter++));
            transEntries.add(transEntry);
            inventoryValueOfProducts = inventoryValueOfProducts.add(valueOfThisProduct).setScale(decimals + 1, rounding);    // always round intermediate add values by one more digit
        }

        // now debit the difference between the conservative value of the the raw material parts and the finished good that was used to WIP inventory, but not associated with any particular
        BigDecimal valueDifference = transactionAmount.subtract(inventoryValueOfProducts).setScale(decimals, rounding);
        transEntry = delegator.makeValue("AcctgTransEntry", UtilMisc.toMap("organizationPartyId", ownerPartyId, "acctgTransEntryTypeId", "_NA_",
                "debitCreditFlag", "D", "amount", new Double(valueDifference.doubleValue()), "glAccountId", UtilAccounting.getDefaultAccountId("MFG_EXPENSE_VARIANCE", ownerPartyId, delegator)));
        transEntry.put("productId", productDisassembled.get("productId"));
        transEntry.put("acctgTransEntrySeqId", Integer.toString(entryCounter++));
        transEntries.add(transEntry);
        return transEntries;
    }


    /**
     * Posts direct and indirect production run task costs:
     * Debit WIP Inventory, Credit (some kind of cost account).
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map postWorkEffortCostsToGl(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String workEffortId = (String) context.get("workEffortId");
        try {
            GenericValue workEffort = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId));
            GenericValue productionRun = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", workEffort.getString("workEffortParentId")));
            GenericValue finishedProduct = EntityUtil.getFirst(productionRun.getRelated("WorkEffortGoodStandard", UtilMisc.toMap("workEffortGoodStdTypeId", "PRUN_PROD_DELIV"), null));
            GenericValue facility = workEffort.getRelatedOne("Facility");
            if (facility == null) {
                return ServiceUtil.returnError("Could not find the facility for work effort [" + workEffortId + "]");
            }
            String ownerPartyId = facility.getString("ownerPartyId");
            OpentapsProductionRun productionRunReader = new OpentapsProductionRun(productionRun.getString("workEffortId"), dispatcher);

            List costComponents = EntityUtil.filterByDate(delegator.findByAnd("CostComponent", UtilMisc.toMap("workEffortId", workEffortId)));
            Iterator costComponentsIt = costComponents.iterator();
            List transEntries = new ArrayList();
            Map input = null;
            while (costComponentsIt.hasNext()) {
                GenericValue costComponent = (GenericValue) costComponentsIt.next();
                if (!"ACTUAL_MAT_COST".equals(costComponent.getString("costComponentTypeId"))) {
                    GenericValue costComponentCalc = costComponent.getRelatedOne("CostComponentCalc");
                    BigDecimal cost = costComponent.getBigDecimal("cost");
                    if (cost.compareTo(ZERO) != 0) {
                        String creditAccountTypeId = costComponentCalc.getString("costGlAccountTypeId");
                        if (UtilValidate.isEmpty(creditAccountTypeId)) {
                            throw new GenericServiceException("Cost component with ID [" + costComponentCalc.get("costComponentCalcId") + "] does not have a costGlAccountTypeId defined.  Unable to post Work Effort ID [" + workEffortId + "] to GL.");
                        }
                        String debitAccountTypeId = costComponentCalc.getString("offsettingGlAccountTypeId");
                        if (debitAccountTypeId == null) {
                            if (productionRunReader.isDisassembly()) {
                                debitAccountTypeId = "MFG_EXPENSE_VARIANCE";
                            } else {
                                debitAccountTypeId = "WIP_INVENTORY";
                            }
                        }
                        String creditAccountId = UtilAccounting.getDefaultAccountId(creditAccountTypeId, ownerPartyId, delegator);
                        String debitAccountId = UtilAccounting.getDefaultAccountId(debitAccountTypeId, ownerPartyId, delegator);
                        // get the currency conversion factor
                        BigDecimal conversionFactor = UtilFinancial.determineUomConversionFactor(delegator, dispatcher, ownerPartyId, costComponent.getString("costUomId"));
                        // convert the cost into the owner's currency
                        BigDecimal transactionAmount = cost.multiply(conversionFactor).setScale(decimals, rounding);

                        // Transaction to credit the inventory account
                        input = UtilMisc.toMap("glAccountId", creditAccountId, "organizationPartyId", ownerPartyId);
                        input.put("amount", new Double(transactionAmount.doubleValue()));
                        input.put("acctgTransEntryTypeId", "_NA_");
                        input.put("debitCreditFlag", "C");
                        input.put("acctgTransEntrySeqId", Integer.toString(0));
                        GenericValue creditAcctTrans = delegator.makeValue("AcctgTransEntry", input);
                        transEntries.add(creditAcctTrans);

                        // Transaction to debit the Wip account
                        input = UtilMisc.toMap("glAccountId", debitAccountId, "organizationPartyId", ownerPartyId);
                        input.put("amount", new Double(transactionAmount.doubleValue()));
                        input.put("acctgTransEntryTypeId", "_NA_");
                        input.put("debitCreditFlag", "D");
                        input.put("productId", finishedProduct.getString("productId"));
                        input.put("acctgTransEntrySeqId", Integer.toString(1));
                        GenericValue debitAcctTrans = delegator.makeValue("AcctgTransEntry", input);
                        transEntries.add(debitAcctTrans);
                    }
                }
            }
            // Perform the transaction
            input = UtilMisc.toMap("transactionDate", UtilDateTime.nowTimestamp(), "userLogin", userLogin);
            input.put("acctgTransEntries", transEntries);
            input.put("glFiscalTypeId", "ACTUAL");
            input.put("acctgTransTypeId", "MANUFACTURING_ATX");
            input.put("workEffortId", workEffortId);

            Map servResult = dispatcher.runSync("createAcctgTransAndEntries", input);

            if (ServiceUtil.isError(servResult)) {
                return servResult;
            }

            Map result = ServiceUtil.returnSuccess();
            result.put("acctgTransId", servResult.get("acctgTransId"));

        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        } catch (GenericServiceException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Posts inventory produced by a WorkEffort (production run) to the GL:
     * Debit Finished Goods Inventory, Credit WIP Inventory.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map postInventoryProducedToGl(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String workEffortId = (String) context.get("workEffortId");
        List inventoryItemIds = (List) context.get("inventoryItemIds");
        try {

            // domain repository
            DomainsLoader dl = new DomainsLoader(new Infrastructure(dispatcher), new User(userLogin));
            InventoryRepositoryInterface inventoryRepository = dl.loadDomainsDirectory().getInventoryDomain().getInventoryRepository();

            GenericValue workEffort = delegator.findByPrimaryKey("WorkEffort", UtilMisc.toMap("workEffortId", workEffortId));
            GenericValue facility = workEffort.getRelatedOne("Facility");
            if (facility == null) {
                return ServiceUtil.returnError("Could not find the facility for work effort [" + workEffortId + "]");
            }
            String ownerPartyId = facility.getString("ownerPartyId");

            Iterator inventoryItemIdsIt = inventoryItemIds.iterator();
            List transEntries = new ArrayList();
            Map input = null;
            while (inventoryItemIdsIt.hasNext()) {
                String inventoryItemId = (String) inventoryItemIdsIt.next();
                GenericValue inventoryItem = delegator.findByPrimaryKey("InventoryItem", UtilMisc.toMap("inventoryItemId", inventoryItemId));

                InventoryItem domainInventoryItem = Repository.loadFromGeneric(InventoryItem.class, inventoryItem, inventoryRepository);

                BigDecimal unitCost = domainInventoryItem.getUnitCost();
                String productId = domainInventoryItem.getProductId();

                // get the currency conversion factor
                BigDecimal conversionFactor = UtilFinancial.determineUomConversionFactor(delegator, dispatcher, ownerPartyId, inventoryItem.getString("currencyUomId"));

                if ((unitCost != null) && (unitCost.compareTo(ZERO) != 0) && (inventoryItem.get("quantityOnHandTotal") != null)) {
                    String creditAccountId = UtilAccounting.getProductOrgGlAccountId(productId, "WIP_INVENTORY", ownerPartyId, delegator);
                    String debitAccountId = UtilAccounting.getProductOrgGlAccountId(productId, "INVENTORY_ACCOUNT", ownerPartyId, delegator);

                    // convert the cost into the owner's currency and get the total transaction amount
                    BigDecimal transactionAmount = unitCost.multiply(conversionFactor).multiply(inventoryItem.getBigDecimal("quantityOnHandTotal")).setScale(decimals, rounding);

                    // Transaction to credit the inventory account
                    input = UtilMisc.toMap("glAccountId", creditAccountId, "organizationPartyId", ownerPartyId);
                    input.put("amount", transactionAmount);
                    input.put("acctgTransEntryTypeId", "_NA_");
                    input.put("debitCreditFlag", "C");
                    input.put("productId", productId);
                    input.put("acctgTransEntrySeqId", Integer.toString(0));
                    GenericValue creditAcctTrans = delegator.makeValue("AcctgTransEntry", input);
                    transEntries.add(creditAcctTrans);

                    // Transaction to debit the Wip account
                    input = UtilMisc.toMap("glAccountId", debitAccountId, "organizationPartyId", ownerPartyId);
                    input.put("amount", transactionAmount);
                    input.put("acctgTransEntryTypeId", "_NA_");
                    input.put("debitCreditFlag", "D");
                    input.put("productId", productId);
                    input.put("acctgTransEntrySeqId", Integer.toString(1));
                    GenericValue debitAcctTrans = delegator.makeValue("AcctgTransEntry", input);
                    transEntries.add(debitAcctTrans);

                    // update the average cost of this inventory item
                    dispatcher.runSync("updateProductAverageCost", UtilMisc.toMap("productId", inventoryItem.getString("productId"), "organizationPartyId", inventoryItem.getString("ownerPartyId"), "userLogin", userLogin));
                }
            }
            // Perform the transaction
            input = UtilMisc.toMap("transactionDate", UtilDateTime.nowTimestamp(), "userLogin", userLogin);
            input.put("acctgTransEntries", transEntries);
            input.put("glFiscalTypeId", "ACTUAL");
            input.put("acctgTransTypeId", "MANUFACTURING_ATX");
            input.put("workEffortId", workEffortId);

            Map servResult = dispatcher.runSync("createAcctgTransAndEntries", input);

            if (ServiceUtil.isError(servResult)) {
                return servResult;
            }

            Map result = ServiceUtil.returnSuccess();
            result.put("acctgTransId", servResult.get("acctgTransId"));

        } catch (GeneralException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }

        return ServiceUtil.returnSuccess();
    }

    /**
     * Service to close out a particular time period for an organization.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map closeTimePeriod(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String organizationPartyId = (String) context.get("organizationPartyId");
        String customTimePeriodId = (String) context.get("customTimePeriodId");
        Infrastructure infrastructure = new Infrastructure(dispatcher);
        
        try {
            // figure out the current time period's type (year, quarter, month)
            GenericValue timePeriod = delegator.findByPrimaryKeyCache("CustomTimePeriod", UtilMisc.toMap("customTimePeriodId", customTimePeriodId));
            if (timePeriod == null) {
                return ServiceUtil.returnError("Cannot find a time period for " + organizationPartyId + " and time period id " + customTimePeriodId);
            }

            // get the organization's accounts for profit and loss (net income) and retained earnings
            String retainedEarningsGlAccountId = UtilAccounting.getProductOrgGlAccountId(null, "RETAINED_EARNINGS", organizationPartyId, delegator);
            String profitLossGlAccountId =  UtilAccounting.getProductOrgGlAccountId(null, "PROFIT_LOSS_ACCOUNT", organizationPartyId, delegator);

            // get a trial balance as of ending date of time period
            GetTrialBalanceForDateService trialBalanceService = new GetTrialBalanceForDateService();
            trialBalanceService.setInAsOfDate(UtilDateTime.toTimestamp(timePeriod.getDate("thruDate")));  // must convert thruDate from Date to Timestamp first
            trialBalanceService.setInOrganizationPartyId(organizationPartyId);
            trialBalanceService.setInUserLogin(userLogin);
            trialBalanceService.runSyncNoNewTransaction(infrastructure);
            
            // debit REVENUE, credit EXPENSE accounts, and put the net into the retained earnings account
            Map<GenericValue, BigDecimal> accountBalancesToDebit = new HashMap();                           // in case revenue account balances are null
            accountBalancesToDebit.putAll(trialBalanceService.getOutRevenueAccountBalances());
            Map<GenericValue, BigDecimal> accountBalancesToCredit = new HashMap();
            accountBalancesToCredit.putAll(trialBalanceService.getOutExpenseAccountBalances());
            
            // also will debit INCOME accounts
            accountBalancesToDebit.putAll(trialBalanceService.getOutIncomeAccountBalances());
            
            // will ignore the OTHER accounts
            
            // this is the amount to enter into retained earnings
            BigDecimal closingNetIncome = BigDecimal.ZERO;
            int seq = 0;
            Set<GenericValue> debitAccounts = accountBalancesToDebit.keySet();
            Set<GenericValue> creditAccounts = accountBalancesToCredit.keySet();
            List<GenericValue> closingEntries = new ArrayList();

            // create a Debit transaction entry for each debit account item
            for (GenericValue debitAccount: debitAccounts) {
                BigDecimal amount = accountBalancesToDebit.get(debitAccount);
                if (amount == null) {
                    Debug.logWarning("Debit account [" + debitAccount.getString("glAccountId") + "] has a null amount and will be skipped", MODULE);
                    continue;
                }
                // IMPORTANT: Must skip the net income account or this entry would cause the trial balance to be unbalanced
                if (debitAccount.getString("glAccountId").equals(profitLossGlAccountId)) {
                    Debug.logWarning("Credit account [" + debitAccount.getString("glAccountId") + "] will be skipped because it is the net income/profit loss account", MODULE);
                    continue;
                }
                AcctgTransEntry entry = new AcctgTransEntry();
                entry.setGlAccountId(debitAccount.getString("glAccountId"));
                entry.setDebitCreditFlag("D");
                entry.setAmount(amount);
                entry.setOrganizationPartyId(organizationPartyId);
                entry.setAcctgTransEntryTypeId("_NA_");
                entry.setAcctgTransEntrySeqId(Integer.toString(seq++));
                closingEntries.add(delegator.makeValue("AcctgTransEntry", entry.toMap()));  // convert back to GenericValue for the createAcctgTransAndEntries service call below
                closingNetIncome = closingNetIncome.add(amount);
            }

            // create a Credit transaction entry for each credit account item
            for (GenericValue creditAccount: creditAccounts) {
                BigDecimal amount = accountBalancesToCredit.get(creditAccount);
                if (amount == null) {
                    Debug.logWarning("Credit account [" + creditAccount.getString("glAccountId") + "] has a null amount and will be skipped", MODULE);
                    continue;
                }
                AcctgTransEntry entry = new AcctgTransEntry();
                entry.setGlAccountId(creditAccount.getString("glAccountId"));
                entry.setDebitCreditFlag("C");
                entry.setAmount(amount);
                entry.setOrganizationPartyId(organizationPartyId);
                entry.setAcctgTransEntryTypeId("_NA_");
                entry.setAcctgTransEntrySeqId(Integer.toString(seq++));
                closingEntries.add(delegator.makeValue("AcctgTransEntry", entry.toMap()));
                closingNetIncome = closingNetIncome.subtract(amount);
            }

            closingNetIncome = closingNetIncome.setScale(decimals, rounding); // round after all the add/subtract is done
            
            // add an entry to credit the retained earnings account
            closingEntries.add(delegator.makeValue("AcctgTransEntry", UtilMisc.toMap("glAccountId", retainedEarningsGlAccountId, "debitCreditFlag", "C",
                      "amount", closingNetIncome, "acctgTransEntrySeqId", Integer.toString(seq), "organizationPartyId", organizationPartyId, "acctgTransEntryTypeId", "_NA_")));
            
            // this is subtle - the transactionDate must be right before the thruDate, or the transaction will actually overlap into the next time period.  We subtract 1 second (1000 milliseconds) to move it before
            Map tmpResult = dispatcher.runSync("createAcctgTransAndEntries", UtilMisc.toMap("acctgTransEntries", closingEntries,
                      "glFiscalTypeId", "ACTUAL", "transactionDate", new Timestamp(timePeriod.getDate("thruDate").getTime() - 1000), "acctgTransTypeId", "PERIOD_CLOSING", "userLogin", userLogin));
            

            // find the previous closed time period, in case we need to carry a balance forward.
            // NOTE: It is very important that we specify the same periodTypeId.  Otherwise, when each transaction entry is posted, it will update the
            // GlAccountHistory of all time periods which are current -- ie, the year, the quarter, the month, etc., and then when you close the end of the year
            // it will start from the a quarter in the year and add it to the year's balance, effecitvely double counting.
            String lastClosedTimePeriodId = null;
            tmpResult = dispatcher.runSync("findLastClosedDate", UtilMisc.toMap("organizationPartyId", organizationPartyId,
                      "periodTypeId", timePeriod.getString("periodTypeId"), "userLogin", userLogin), -1, false);
            if (tmpResult.get("lastClosedTimePeriod") != null) {
                 lastClosedTimePeriodId = ((GenericValue) tmpResult.get("lastClosedTimePeriod")).getString("customTimePeriodId");
            }

            // now set the ending balance for all GlAccountHistory entries for this time period and organization
            // bringing forward gl account history from previous time period for ASSET, LIABILITY, and EQUITY accounts
            // we do this by creating a Map of the current period's GlAccountHistory and then adding in those from the previous period

            // the first step is to find all GlAccountHistory of the current time period

            List<GenericValue> glAccountHistories = delegator.findByAnd("GlAccountHistory", UtilMisc.toMap("organizationPartyId", organizationPartyId,
                            "customTimePeriodId", timePeriod.getString("customTimePeriodId")));

            // now make a map of glAccountId -> GlAccountHistory, with the correct endingBalance in the GlAccountHistory values
            Map<String, GenericValue> updatedGlAccountHistories = new HashMap<String, GenericValue>();
            for (GenericValue glAccountHistory : glAccountHistories) {
                BigDecimal netBalance = UtilAccounting.getNetBalance(glAccountHistory, MODULE);
                glAccountHistory.set("endingBalance", netBalance);
                updatedGlAccountHistories.put(glAccountHistory.getString("glAccountId"), glAccountHistory);
            }

            // is there a previously closed time period?  If so, then find all of its GlAccountHistory and add their ending balances in
            if (lastClosedTimePeriodId != null) {
                // find the previous period GL account histories.  We are ONLY carrying forward ASSET, LIABILITY, EQUITY accounts
                EntityCondition previousPeriodConditions = EntityCondition.makeCondition(EntityOperator.AND,
                          EntityCondition.makeCondition("organizationPartyId", EntityOperator.EQUALS, organizationPartyId),
                          EntityCondition.makeCondition(EntityOperator.OR,
                                  UtilFinancial.getAssetExpr(delegator),
                                  UtilFinancial.getLiabilityExpr(delegator),
                                  UtilFinancial.getEquityExpr(delegator)),
                          EntityCondition.makeCondition("customTimePeriodId", EntityOperator.EQUALS, lastClosedTimePeriodId));

                List<GenericValue> previousGlAccountHistories = delegator.findByCondition("GlAccountAndHistory", previousPeriodConditions,
                    UtilMisc.toList("organizationPartyId", "customTimePeriodId", "glAccountId", "postedDebits", "postedCredits", "endingBalance"), UtilMisc.toList("glAccountId"));

                // loop and check them against current period
                for (GenericValue previousGlAccountAndHistory : previousGlAccountHistories) {
                    GenericValue previousGlAccountHistory = previousGlAccountAndHistory.getRelatedOne("GlAccountHistory");

                    // is this gl account also in the current period?
                    if (updatedGlAccountHistories.get(previousGlAccountAndHistory.getString("glAccountId")) != null) {
                        // yes: carry forward the balance
                        GenericValue updatedGlAccountHistory = updatedGlAccountHistories.get(previousGlAccountAndHistory.getString("glAccountId"));
                        BigDecimal newEndingBalance = updatedGlAccountHistory.getBigDecimal("endingBalance").add(previousGlAccountHistory.getBigDecimal("endingBalance"));
                        updatedGlAccountHistory.set("endingBalance", newEndingBalance);
                    } else {
                        // no: put it in with the previous period's balance but the current period's time period id
                        GenericValue carriedForwardGlAccountHistory = delegator.makeValue("GlAccountHistory", UtilMisc.toMap("glAccountId", previousGlAccountHistory.getString("glAccountId"),
                               "organizationPartyId", organizationPartyId, "customTimePeriodId", timePeriod.getString("customTimePeriodId"),
                               "postedDebits", BigDecimal.ZERO, "postedCredits", BigDecimal.ZERO, "endingBalance", previousGlAccountHistory.getBigDecimal("endingBalance")));
                        updatedGlAccountHistories.put(previousGlAccountHistory.getString("glAccountId"), carriedForwardGlAccountHistory);
                    }
                }
            }

            // store all of these
            List<GenericValue> toBeStored = new ArrayList<GenericValue>();
            toBeStored.addAll(updatedGlAccountHistories.values());
            delegator.storeAll(toBeStored);

            // finally, set time period to closed
            tmpResult = dispatcher.runSync("updateCustomTimePeriod", UtilMisc.toMap("customTimePeriodId", timePeriod.getString("customTimePeriodId"), "organizationPartyId", organizationPartyId,
                "isClosed", "Y", "userLogin", userLogin));

            //  one more check to make sure the accounts are in balance
            if (!UtilFinancial.areAllTrialBalancesEqual(organizationPartyId, delegator, decimals, RoundingMode.valueOf(rounding))) {
                return ServiceUtil.returnError("Cannot close time period [" + timePeriod.getString("customTimePeriodId") + "] : resulting trial balances are not equal");
            }

            return ServiceUtil.returnSuccess();

        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        } catch (GenericServiceException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        } catch (ServiceException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }
    }

    /**
     * Service closes all time periods which end on the thruDate of the specified time period.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map closeAllTimePeriods(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String organizationPartyId = (String) context.get("organizationPartyId");
        String customTimePeriodId = (String) context.get("customTimePeriodId");

        try {
            GenericValue timePeriod = delegator.findByPrimaryKeyCache("CustomTimePeriod", UtilMisc.toMap("customTimePeriodId", customTimePeriodId));
            if (timePeriod == null) {
                return ServiceUtil.returnError("Cannot find a time period for " + organizationPartyId + " and time period id " + customTimePeriodId);
            }

            closeAllTimePeriodsByType(delegator, dispatcher, organizationPartyId, "FISCAL_WEEK", timePeriod.getDate("thruDate"), userLogin);
            closeAllTimePeriodsByType(delegator, dispatcher, organizationPartyId, "FISCAL_BIWEEK", timePeriod.getDate("thruDate"), userLogin);
            closeAllTimePeriodsByType(delegator, dispatcher, organizationPartyId, "FISCAL_MONTH", timePeriod.getDate("thruDate"), userLogin);
            closeAllTimePeriodsByType(delegator, dispatcher, organizationPartyId, "FISCAL_QUARTER", timePeriod.getDate("thruDate"), userLogin);
            closeAllTimePeriodsByType(delegator, dispatcher, organizationPartyId, "FISCAL_YEAR", timePeriod.getDate("thruDate"), userLogin);

            return ServiceUtil.returnSuccess();
        } catch (GenericEntityException ex) {
           return ServiceUtil.returnError(ex.getMessage());
        } catch (GenericServiceException ex) {
           return ServiceUtil.returnError(ex.getMessage());
        }
    }

    /**
     * Resets all the postedBalance of GlAccountOrganization for an organization to 0.0 for REVENUE, INCOME, and EXPENSE GL accounts.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map resetOrgGlAccountBalances(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        try {
            String organizationPartyId = (String) context.get("organizationPartyId");
            String customTimePeriodId = (String) context.get("customTimePeriodId");

            // find the REVENUE, EXPENSE, and INCOME gl accounts for the organization
            // IMPORTANT: also make sure the PROFIT_LOSS_ACCOUNT is included, even if it is classified differently
            EntityCondition glAccountConditions = EntityCondition.makeCondition(EntityOperator.AND,
                    EntityCondition.makeCondition("organizationPartyId", EntityOperator.EQUALS, organizationPartyId),
                    EntityCondition.makeCondition(EntityOperator.OR,
                        UtilFinancial.getGlAccountClassExpr("REVENUE", delegator),
                        UtilFinancial.getGlAccountClassExpr("EXPENSE", delegator),
                        UtilFinancial.getGlAccountClassExpr("INCOME", delegator),
                        EntityCondition.makeCondition("glAccountId", EntityOperator.EQUALS, UtilAccounting.getProductOrgGlAccountId(null, "PROFIT_LOSS_ACCOUNT", organizationPartyId, delegator))),
                    EntityCondition.makeCondition("postedBalance", EntityOperator.NOT_EQUAL, BigDecimal.ZERO));

            List glAccounts = delegator.findByCondition("GlAccountOrganizationAndClass", glAccountConditions,
                    UtilMisc.toList("organizationPartyId", "glAccountId", "postedBalance"), UtilMisc.toList("glAccountId"));

            // Calculate the correct ending balances for these gl accounts, which is the sum of the transactions after the time period to be closed
            // note that we must check the last closed transaction's date, as there may be transactions dated in the future which have been posted already
            GenericValue timePeriod = delegator.findByPrimaryKeyCache("CustomTimePeriod", UtilMisc.toMap("customTimePeriodId", customTimePeriodId));
            Timestamp fromDate = UtilDateTime.toTimestamp(timePeriod.getDate("thruDate"));
            Timestamp thruDate = UtilDateTime.nowTimestamp();
            Timestamp lastTransactionDate = UtilFinancial.getTransactionDateForLastPostedTransaction(organizationPartyId, delegator);
            if ((lastTransactionDate != null) && (lastTransactionDate.after(thruDate))) {
                thruDate = UtilDateTime.getTimestamp(lastTransactionDate.getTime() + 1000);
                Debug.logInfo("Found posted transactions which closed on [" + lastTransactionDate + "] and will set GlAccountOrganization to income statement from [" + fromDate + "] to [" + thruDate + "]", MODULE);

            }
            Map tmpResult = dispatcher.runSync("getIncomeStatementAccountSumsByDate", UtilMisc.toMap("organizationPartyId", organizationPartyId,
                    "fromDate", fromDate,
                    "thruDate", thruDate,
                    "glFiscalTypeId", "ACTUAL", "userLogin", userLogin));
            Map<GenericValue, BigDecimal> glAccountSums = new HashMap<GenericValue, BigDecimal>();                // Map of GlAccount -> Sum of transactions
            if (tmpResult.get("glAccountSums") != null) {
                glAccountSums = (Map<GenericValue, BigDecimal>) tmpResult.get("glAccountSums");
            }

            // reset all these accounts' posted balances to the sum of transactions after period being closed or, if there were no transactions, to zero
            for (Iterator<GenericValue> gAi = glAccounts.iterator(); gAi.hasNext();) {
                GenericValue glAccount = gAi.next().getRelatedOne("GlAccount");
                BigDecimal newAmount = null;
                if (glAccountSums.get(glAccount) != null) {
                    newAmount = glAccountSums.get(glAccount);
                } else {
                    newAmount = BigDecimal.ZERO;
                }
                Debug.logInfo("now getting ready to reset " + glAccount + " to " + newAmount, MODULE);
                tmpResult = dispatcher.runSync("updateGlAccountOrganization", UtilMisc.<String, Object>toMap("organizationPartyId", organizationPartyId, "glAccountId", glAccount.getString("glAccountId"), "postedBalance", newAmount.doubleValue(), "userLogin", userLogin));
            }

            // check and make sure accounts are in balance before doing this
            if (!UtilFinancial.areAllTrialBalancesEqual(organizationPartyId, delegator, decimals, RoundingMode.valueOf(rounding))) {
                return ServiceUtil.returnError("Cannot reset GL account balances: resulting trial balances are not equal");
            }

            return ServiceUtil.returnSuccess();
        } catch (GenericEntityException ex) {
           return ServiceUtil.returnError(ex.getMessage());
        } catch (GenericServiceException ex) {
           return ServiceUtil.returnError(ex.getMessage());
        }
    }

    /**
     * Service to reconcile a GlAccount.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map reconcileGlAccount(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String organizationPartyId = (String) context.get("organizationPartyId");
        String glAccountId = (String) context.get("glAccountId");
        String glReconciliationName = (String) context.get("glReconciliationName");
        String description = (String) context.get("description");
        Double reconciledBalance = (Double) context.get("reconciledBalance");
        Timestamp reconciledDate = (Timestamp) context.get("reconciledDate");
        List acctgTransEntries = (List) context.get("acctgTransEntries");
        try {
            if (reconciledBalance == null) {
                return ServiceUtil.returnError("Cannot reconcile account " + glAccountId + ": No reconciled balance found.");
            }

            if (reconciledDate == null) {
                return ServiceUtil.returnError("Cannot reconcile account " + glAccountId + ": No as of date specified.");
            }

            if ((acctgTransEntries == null) || (acctgTransEntries.size() == 0)) {
                return ServiceUtil.returnError("Cannot reconcile account " + glAccountId + ": No transactions to be reconciled.");
            }

            // first, create a reconciliation
            Map params = UtilMisc.toMap("glAccountId", glAccountId, "organizationPartyId", organizationPartyId);
            params.put("glReconciliationName", glReconciliationName);
            params.put("description", description);
            params.put("reconciledBalance", reconciledBalance);
            params.put("reconciledDate", reconciledDate);
            params.put("userLogin", userLogin);
            Debug.logInfo(params.toString(), MODULE);
            Map results = dispatcher.runSync("createGlReconciliation", params);
            if (ServiceUtil.isError(results)) {
                return results;
            }
            String glReconciliationId = (String) results.get("glReconciliationId");

            /*
             * Our acctgTransId and acctgTransEntrySeqId were passed in joined together as acctgTransEntries
             * using the pipe | character as a delimiter. Here, we loop through these entries, split the IDs out
             * and 1) update each AcctgTransEntry, 2) create a GL Reconciliation entry
             */
            Iterator iter = acctgTransEntries.iterator();
            while (iter.hasNext()) {
                String entry = (String) iter.next();
                if (entry == null) {
                    continue;
                }

                // split the entry string into component transaction ids
                List tokens = StringUtil.split(entry, "|");

                // continue if data isn't our pair of IDs
                if ((tokens == null) || (tokens.size() != 2)) {
                    continue;
                }

                String acctgTransId = (String) tokens.get(0);
                String acctgTransEntrySeqId = (String) tokens.get(1);

                // Grab our transaction entry
                GenericValue acctgTransEntry = delegator.findByPrimaryKey("AcctgTransEntry", UtilMisc.toMap("acctgTransId", acctgTransId, "acctgTransEntrySeqId", acctgTransEntrySeqId));

                // to prevent doubleposts, we validate that we don't have AES_RECONCILED entries
                if (acctgTransEntry.getString("reconcileStatusId").equals("AES_RECONCILED")) {
                    return ServiceUtil.returnError("Cannot reconcile account: Submitted transaction entry is already reconciled (acctgTransId=" + acctgTransId
                            + ", acctgTransEntrySeqId=" + acctgTransEntrySeqId + ").");
                }

                // update AcctgTransEntry status to AES_RECONCILED
                params = UtilMisc.toMap("acctgTransId", acctgTransId, "acctgTransEntrySeqId", acctgTransEntrySeqId);
                params.put("userLogin", userLogin);
                params.put("reconcileStatusId", "AES_RECONCILED");
                results = dispatcher.runSync("updateAcctgTransEntry", params);
                if (ServiceUtil.isError(results)) {
                    throw new GenericServiceException("Failed to update AcctgTransEntry (acctgTransId=" + acctgTransId
                            + ", acctgTransEntrySeqId=" + acctgTransEntrySeqId + ")");
                }

                // need amount of trans entry for gl reconcile entry
                BigDecimal amount = acctgTransEntry.getBigDecimal("amount");

                // prepare input for and call createGlReconciliationEntry
                params = UtilMisc.toMap("glReconciliationId", glReconciliationId);
                params.put("userLogin", userLogin);
                params.put("acctgTransId", acctgTransId);
                params.put("acctgTransEntrySeqId", acctgTransEntrySeqId);
                params.put("reconciledAmount", amount);
                results = dispatcher.runSync("createGlReconciliationEntry", params);
                if (ServiceUtil.isError(results)) {
                    throw new GenericServiceException("Failed to create GlReconciliationEntry (glReconciliationId=" + glReconciliationId
                            + ", acctgTransId=" + acctgTransId
                            + ", acctgTransEntrySeqId=" + acctgTransEntrySeqId + ")");
                }
            }

            results = ServiceUtil.returnSuccess();
            results.put("glReconciliationId", glReconciliationId);
            return results;

        } catch (GenericEntityException ex) {
           return ServiceUtil.returnError(ex.getMessage());
        } catch (GenericServiceException ex) {
           return ServiceUtil.returnError(ex.getMessage());
        }
    }

    /**
     * Helps closing all time periods by closing out all time periods of an asOfDate and a periodTypeId for an organizationPartyId.
     * @param delegator a <code>Delegator</code> value
     * @param dispatcher a <code>LocalDispatcher</code> value
     * @param organizationPartyId a <code>String</code> value
     * @param periodTypeId a <code>String</code> value
     * @param asOfDate a <code>Date</code> value
     * @param userLogin a <code>GenericValue</code> value
     * @exception GenericEntityException if an error occurs
     * @exception GenericServiceException if an error occurs
     */
    @SuppressWarnings("unchecked")
    private static void closeAllTimePeriodsByType(Delegator delegator, LocalDispatcher dispatcher, String organizationPartyId,
            String periodTypeId, Date asOfDate, GenericValue userLogin) throws GenericEntityException, GenericServiceException {
        try {
            List timePeriods = delegator.findByAnd("CustomTimePeriod", UtilMisc.toMap("organizationPartyId", organizationPartyId, "periodTypeId", periodTypeId,
                    "thruDate", asOfDate, "isClosed", "N"));
            if ((timePeriods != null) && (timePeriods.size() > 0)) {
                for (Iterator tPi = timePeriods.iterator(); tPi.hasNext();) {
                    GenericValue timePeriod = (GenericValue) tPi.next();
                    Debug.logInfo("Now trying to close time period " + timePeriod, MODULE);
                    Map tmpResult = dispatcher.runSync("closeTimePeriod", UtilMisc.toMap("organizationPartyId", organizationPartyId,
                            "customTimePeriodId", timePeriod.getString("customTimePeriodId"), "userLogin", userLogin));
                    if (tmpResult == null) {
                        throw new GenericServiceException("Failed to close time period for " + organizationPartyId + " and time period " + timePeriod.getString("customTimePeriodId"));
                    } else if (!tmpResult.get(ModelService.RESPONSE_MESSAGE).equals(ModelService.RESPOND_SUCCESS)) {
                        throw new GenericServiceException("Failed to close time period for " + organizationPartyId + " and time period " + timePeriod.getString("customTimePeriodId")
                                + ": " + tmpResult.get(ModelService.ERROR_MESSAGE));
                    }
                }
            }
        } catch (GenericEntityException ex) {
            Debug.logError(ex.getMessage(), MODULE);
            throw new GenericEntityException(ex);
        } catch (GenericServiceException ex) {
            Debug.logError(ex.getMessage(), MODULE);
            throw new GenericServiceException(ex);
        }
    }

    /**
     * Service to post foreign exchange gain/loss to GL.
     * If invoice amount > payment amount
     *         if invoice type is sales invoice then Debit Foreign exchange loss, Credit Account receivable
     *         if invoice type is purchace invoice then Debit Account payable, Credit Foreign exchange gain
     * If invoice amount < payment amount
     *         if invoice type is sales invoice then Debit Account receivable, Credit Foreign exchange gain
     *         if invoice type is purchace invoice then Debit Foreign exchange loss, Credit Account payable
     * If invoice type is customer return or commission, then this method does nothing (yet).
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map postForeignExchangeMatchingToGl(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        String invoiceId = (String) context.get("invoiceId");
        try {
            // get the invoice and make sure it has an invoiceTypeId
            GenericValue invoice = delegator.findByPrimaryKeyCache("Invoice", UtilMisc.toMap("invoiceId", invoiceId));
            if (invoice == null) {
                return ServiceUtil.returnError("No invoice found for invoiceId of " + invoiceId);
            } else if (invoice.get("invoiceTypeId") == null) {
                return ServiceUtil.returnError("Invoice " + invoiceId + " has a null invoice type and cannot be processed");
            }
            String invoiceTypeId = (String) invoice.get("invoiceTypeId");

            // accounting data
            String transactionPartyId = null;
            String transactionPartyRoleTypeId = null;
            String organizationPartyId = null;
            String offsettingGlAccountTypeId = null;
            Map result = null;

            // set accounting data according to invoiceTypeId
            if ("SALES_INVOICE".equals(invoiceTypeId)) {
                offsettingGlAccountTypeId = "ACCOUNTS_RECEIVABLE";
                organizationPartyId = invoice.getString("partyIdFrom");
                transactionPartyId = invoice.getString("partyId");
                transactionPartyRoleTypeId = "BILL_TO_CUSTOMER";
            } else if ("PURCHASE_INVOICE".equals(invoiceTypeId)) {
                offsettingGlAccountTypeId = "ACCOUNTS_PAYABLE";
                organizationPartyId = invoice.getString("partyId");
                transactionPartyId = invoice.getString("partyIdFrom");
                transactionPartyRoleTypeId = "BILL_FROM_VENDOR";
            } else if ("CUST_RTN_INVOICE".equals(invoiceTypeId)) {
                result = ServiceUtil.returnSuccess();
                result.put("acctgTransId", null);
                return result;
            } else if ("COMMISSION_INVOICE".equals(invoiceTypeId)) {
                result = ServiceUtil.returnSuccess();
                result.put("acctgTransId", null);
                return result;
            } else if ("INTEREST_INVOICE".equals(invoiceTypeId)) {
                offsettingGlAccountTypeId = "INTRSTINC_RECEIVABLE";
                organizationPartyId = invoice.getString("partyIdFrom");
                transactionPartyId = invoice.getString("partyId");
                transactionPartyRoleTypeId = "BILL_TO_CUSTOMER";
            } else {
                return ServiceUtil.returnError("Unknown invoiceTypeId '" + invoiceTypeId + "' for Invoice [" + invoiceId + "]");
            }
            // get the invoice currency
            String invoiceCurrencyId = invoice.getString("currencyUomId");
            // get the company currency
            String companyCurrencyId = UtilCommon.getOrgBaseCurrency(organizationPartyId, delegator);

            // get the invoice transaction amount
            BigDecimal conversionFactor = UtilFinancial.determineUomConversionFactor(delegator, dispatcher, organizationPartyId, invoice.getString("currencyUomId"), invoice.getTimestamp("invoiceDate"));
            BigDecimal invoiceAmount = InvoiceWorker.getInvoiceTotal(invoice).multiply(conversionFactor).setScale(decimals, rounding);

            // get the invoice payment applications transaction amount
            BigDecimal paymentAmount = ZERO;
            List paymentApplications = delegator.findByAnd("PaymentApplication", UtilMisc.toMap("invoiceId", invoiceId));
            Iterator paymentApplicationIt = paymentApplications.iterator();
            while (paymentApplicationIt.hasNext()) {
                GenericValue paymentApplication = (GenericValue) paymentApplicationIt.next();
                GenericValue payment = paymentApplication.getRelatedOne("Payment");
                conversionFactor = UtilFinancial.determineUomConversionFactor(delegator, dispatcher, organizationPartyId, payment.getString("currencyUomId"), payment.getTimestamp("effectiveDate"));
                BigDecimal amount = paymentApplication.getBigDecimal("amountApplied");
                paymentAmount = paymentAmount.add(amount.multiply(conversionFactor)).setScale(decimals, rounding);
            }

            // accounting data
            String fxGainLossGlAccountTypeId = null;
            String fxGLCreditDebitFlag = null;
            String transCreditDebitFlag = null;

            // get the transaction amount and the foreign exchange gain/loss account type
            BigDecimal transactionAmount = invoiceAmount.subtract(paymentAmount);
            if (!invoiceCurrencyId.equals(companyCurrencyId) && (invoiceAmount.compareTo(paymentAmount) != 0)) {
                if (invoiceAmount.compareTo(paymentAmount) > 0) {
                    if ("SALES_INVOICE".equals(invoiceTypeId)) { // sales invoice
                        fxGainLossGlAccountTypeId = "FX_LOSS_ACCOUNT";
                        fxGLCreditDebitFlag = "D";
                        transCreditDebitFlag = "C";
                    } else { // purchase invoice
                        fxGainLossGlAccountTypeId = "FX_GAIN_ACCOUNT";
                        fxGLCreditDebitFlag = "C";
                        transCreditDebitFlag = "D";
                    }
                } else {
                    transactionAmount = transactionAmount.negate();
                    if ("SALES_INVOICE".equals(invoiceTypeId)) { // sales invoice
                        fxGainLossGlAccountTypeId = "FX_GAIN_ACCOUNT";
                        fxGLCreditDebitFlag = "C";
                        transCreditDebitFlag = "D";
                    } else { // purchase invoice
                        fxGainLossGlAccountTypeId = "FX_LOSS_ACCOUNT";
                        fxGLCreditDebitFlag = "D";
                        transCreditDebitFlag = "C";
                    }
                }
            } else {
                result = ServiceUtil.returnSuccess();
                result.put("acctgTransId", null);
                return result;
            }

            // get the foreign exchange gain/loss account
            String fxGlAccountId = UtilAccounting.getDefaultAccountId(fxGainLossGlAccountTypeId, organizationPartyId, delegator);
            // get the invoice transaction account
            String transGlAccountId = UtilAccounting.getDefaultAccountId(offsettingGlAccountTypeId, organizationPartyId, delegator);

            // Transaction to credit/debit the foreign exchange gain/loss account
            Map input = UtilMisc.toMap("glAccountId", fxGlAccountId, "organizationPartyId", organizationPartyId, "partyId", transactionPartyId);
            input.put("amount", new Double(transactionAmount.doubleValue()));
            input.put("acctgTransEntryTypeId", "_NA_");
            input.put("debitCreditFlag", fxGLCreditDebitFlag);
            input.put("acctgTransEntrySeqId", Integer.toString(0));
            input.put("roleTypeId", transactionPartyRoleTypeId);
            GenericValue creditAcctTrans = delegator.makeValue("AcctgTransEntry", input);

            // Transaction to debit/credit the invoice transaction account
            input = UtilMisc.toMap("glAccountId", transGlAccountId, "organizationPartyId", organizationPartyId, "partyId", transactionPartyId);
            input.put("amount", new Double(transactionAmount.doubleValue()));
            input.put("acctgTransEntryTypeId", "_NA_");
            input.put("debitCreditFlag", transCreditDebitFlag);
            input.put("acctgTransEntrySeqId", Integer.toString(1));
            input.put("roleTypeId", transactionPartyRoleTypeId);
            GenericValue debitAcctTrans = delegator.makeValue("AcctgTransEntry", input);

            // Perform the transaction
            input = UtilMisc.toMap("transactionDate", UtilDateTime.nowTimestamp(), "partyId", transactionPartyId, "userLogin", userLogin);
            input.put("acctgTransEntries", UtilMisc.toList(creditAcctTrans, debitAcctTrans));
            input.put("invoiceId", invoiceId);
            input.put("roleTypeId", transactionPartyRoleTypeId);
            input.put("glFiscalTypeId", "ACTUAL");
            input.put("acctgTransTypeId", "FX_GAINLOSS_ACCTG");
            Map servResult = dispatcher.runSync("createAcctgTransAndEntries", input);

            if (((String) servResult.get(ModelService.RESPONSE_MESSAGE)).equals(ModelService.RESPOND_SUCCESS)) {
                result = ServiceUtil.returnSuccess();
                result.put("acctgTransId", servResult.get("acctgTransId"));
                return result;
            } else {
                return servResult;
            }
        } catch (GenericEntityException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        } catch (GenericServiceException ex) {
            return ServiceUtil.returnError(ex.getMessage());
        }
    }

    /**
     * More process after reverting production run.
     *
     * @param ctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map postRevertedProductionRunToGl(DispatchContext ctx, Map context) {

        Delegator delegator = ctx.getDelegator();
        LocalDispatcher dispatcher = ctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Map result = ServiceUtil.returnSuccess();

        // Mandatory input fields
        String productionRunId = (String) context.get("productionRunId");
        Debug.logInfo("Revert production run [" + productionRunId + "].", MODULE);
        List<Map> savedParts = (List) context.get("savedParts");

        try {

            Map input = null;
            Timestamp transactionDate = UtilDateTime.nowTimestamp();
            List transEntries = null;

            int transEntrySeqId = 0;
            OpentapsProductionRun productionRunReader = new OpentapsProductionRun(productionRunId, dispatcher);
            GenericValue finishedProduct = productionRunReader.getProductProduced();
            String finishedProductId = finishedProduct.getString("productId");

            // for each part saved from the reverted production run, post DR Raw Material Inventory, CR Reverted Production Run Expense
            // and any related average cost adjustment

            for (Map savedPart : savedParts) {
                String inventoryItemId = (String) savedPart.get("inventoryItemId");
                String workEffortId = (String) savedPart.get("workEffortId");

                // get the inventory item and data useful to this service
                GenericValue inventoryItem = delegator.findByPrimaryKey("InventoryItem", UtilMisc.toMap("inventoryItemId", inventoryItemId));

                if (UtilValidate.isEmpty(inventoryItem)) {
                    Debug.logInfo("InventoryItem for part [inventoryItemId," + inventoryItemId + "],[workEffortId," + workEffortId + "] doesn't exist.", MODULE);
                    continue;
                }

                String ownerPartyId = inventoryItem.getString("ownerPartyId");
                String productId = inventoryItem.getString("productId");

                transEntries = FastList.newInstance();
                BigDecimal transactionAmount = ((BigDecimal) savedPart.get("quantity")).multiply(inventoryItem.getBigDecimal("unitCost"));
                // Create an accounting transaction entry to debit RAWMAT_INVENTORY account
                // of the organization for the savedPart.quantity * inventoryItem.unitCost
                String debitAccountId = UtilAccounting.getProductOrgGlAccountId(productId, "RAWMAT_INVENTORY", ownerPartyId, delegator);
                input = UtilMisc.toMap("glAccountId", debitAccountId, "organizationPartyId", ownerPartyId);
                input.put("productId", productId);
                input.put("amount", transactionAmount);
                input.put("acctgTransEntryTypeId", "_NA_");
                input.put("debitCreditFlag", "D");
                input.put("acctgTransEntrySeqId", Integer.toString(transEntrySeqId++));
                GenericValue debitAcctTrans = delegator.makeValue("AcctgTransEntry", input);
                transEntries.add(debitAcctTrans);

                // Create an accounting transaction entry to credit the MFG_EXPENSE_REVPRUN account
                // of the organization for the savedPart.quantity * inventoryItem.unitCost
                String creditAccountId = UtilAccounting.getProductOrgGlAccountId(productId, "MFG_EXPENSE_REVPRUN", ownerPartyId, delegator);
                input = UtilMisc.toMap("glAccountId", creditAccountId, "organizationPartyId", ownerPartyId);
                input.put("productId", finishedProductId);
                input.put("amount", transactionAmount);
                input.put("acctgTransEntryTypeId", "_NA_");
                input.put("debitCreditFlag", "C");
                input.put("acctgTransEntrySeqId", Integer.toString(transEntrySeqId++));
                GenericValue creditAcctTrans = delegator.makeValue("AcctgTransEntry", input);
                transEntries.add(creditAcctTrans);

                // in the case we use the average cost sold method
                if ("COGS_AVG_COST".equals(UtilCommon.getOrgCOGSMethodId(ownerPartyId, delegator))) {
                    BigDecimal prodAvgCost = UtilCOGS.getProductAverageCost(productId, ownerPartyId, userLogin, delegator, dispatcher);
                    BigDecimal unitCost = inventoryItem.getBigDecimal("unitCost");
                    if (prodAvgCost == null) {
                    Debug.logWarning("Unable to find a product average cost for product [" + productId + "] in organization [" + ownerPartyId + "], no adjustment will be made in inventory variance", MODULE);
                    } else {
                        BigDecimal quantity = (BigDecimal) savedPart.get("quantity");
                        transactionAmount = prodAvgCost.subtract(unitCost).multiply(quantity).setScale(decimals, rounding);

                        // Create and accounting transaction entry to debit
                        // RAWMAT_INVENTORY account of the organization for the
                        // savedPart.quantity * (average cost - inventoryItem.unitCost)
                        debitAccountId = UtilAccounting.getProductOrgGlAccountId(productId, "RAWMAT_INVENTORY", ownerPartyId, delegator);
                        input = UtilMisc.toMap("glAccountId", debitAccountId, "organizationPartyId", ownerPartyId);
                        input.put("productId", productId);
                        input.put("amount", transactionAmount);
                        input.put("acctgTransEntryTypeId", "_NA_");
                        input.put("debitCreditFlag", "D");
                        input.put("acctgTransEntrySeqId", Integer.toString(transEntrySeqId++));
                        debitAcctTrans = delegator.makeValue("AcctgTransEntry", input);
                        transEntries.add(debitAcctTrans);

                        // Create and accounting transaction entry to credit
                        // MFG_EXPENSE_REVPRUN account of the organization for the
                        // savedPart.quantity * (average cost - inventoryItem.unitCost)
                        creditAccountId = UtilAccounting.getProductOrgGlAccountId(productId, "MFG_EXPENSE_REVPRUN", ownerPartyId, delegator);
                        input = UtilMisc.toMap("glAccountId", creditAccountId, "organizationPartyId", ownerPartyId);
                        input.put("productId", productId);
                        input.put("amount", transactionAmount);
                        input.put("acctgTransEntryTypeId", "_NA_");
                        input.put("debitCreditFlag", "C");
                        input.put("acctgTransEntrySeqId", Integer.toString(transEntrySeqId++));
                        creditAcctTrans = delegator.makeValue("AcctgTransEntry", input);
                        transEntries.add(creditAcctTrans);
                    }
                }


                // we create an AcctgTrans with Entries for each task whose parts are reverted because workEffortId is available at the AcctgTrans and not Entry level
                input = UtilMisc.toMap("transactionDate", transactionDate, "userLogin", userLogin);
                input.put("acctgTransEntries", transEntries);
                input.put("glFiscalTypeId", "ACTUAL");
                input.put("acctgTransTypeId", "MANUFACTURING_ATX");
                input.put("workEffortId", workEffortId);
                input.put("inventoryItemId", inventoryItemId);
                Map output = dispatcher.runSync("createAcctgTransAndEntries", input);
                if (ServiceUtil.isError(output) || ServiceUtil.isFailure(output)) {
                    return output;
                }
            }

            // calculation of total WIP_INVENTORY transaction entries for the production run and all its sub tasks

            // WIP_INVENTORY is always posted in the productId of the finished product
            String ownerPartyId = productionRunReader.getOwnerPartyId();
            String glAccountId = UtilAccounting.getProductOrgGlAccountId(finishedProductId, "WIP_INVENTORY", ownerPartyId, delegator);

            // now get all the workEffortIds of the production run's tasks.  WIP_INVENTORY is always posted in the workEffortId of the task.
            Set<String> workEffortIds = FastSet.newInstance();
            WorkEffortSearch.getAllSubWorkEffortIds(productionRunId, workEffortIds, delegator, null);
            // but we add the production run header's workEffortId just to be sure
            workEffortIds.add(productionRunId);

            // get the sum of the WIP_INVENTORY by GL account -- there should only be one
            List<GenericValue> acctgTransAndEntries = delegator.findByAnd("AcctgTransAndEntries", UtilMisc.toList(
                    EntityCondition.makeCondition("workEffortId", EntityOperator.IN, workEffortIds),
                    EntityCondition.makeCondition("glAccountId", EntityOperator.EQUALS, glAccountId)));
            Map<GenericValue, BigDecimal> glAccountSums = new HashMap<GenericValue, BigDecimal>();
            UtilFinancial.sumBalancesByAccount(glAccountSums, acctgTransAndEntries);

            // this should never happen
            if (glAccountSums.size() != 1) {
                Debug.logWarning("There are [" + glAccountSums.size() + "] accounts corresponding to total WIP_INVENTORY sum. It should have only one.", MODULE);
            }

            transEntries = FastList.newInstance();
            // now post the DR Reverted Manufacturing Expense, CR WIP Inventory
            for (GenericValue glAccount : glAccountSums.keySet()) {
                BigDecimal sum = glAccountSums.get(glAccount);

                // Transaction to debit MFG_EXPENSE_REVPRUN
                String debitAccountId = UtilAccounting.getProductOrgGlAccountId(finishedProductId, "MFG_EXPENSE_REVPRUN", ownerPartyId, delegator);
                input = UtilMisc.toMap("glAccountId", debitAccountId, "organizationPartyId", ownerPartyId);
                input.put("productId", finishedProductId);
                input.put("amount", sum);
                input.put("acctgTransEntryTypeId", "_NA_");
                input.put("debitCreditFlag", "D");
                input.put("acctgTransEntrySeqId", Integer.toString(transEntrySeqId++));
                GenericValue debitAcctTrans = delegator.makeValue("AcctgTransEntry", input);
                transEntries.add(debitAcctTrans);

                // Transaction to credit the WIP Inventory account
                String creditAccountId = UtilAccounting.getProductOrgGlAccountId(finishedProductId, "WIP_INVENTORY", ownerPartyId, delegator);
                input = UtilMisc.toMap("glAccountId", creditAccountId, "organizationPartyId", ownerPartyId);
                input.put("productId", finishedProductId);
                input.put("amount", sum);
                input.put("acctgTransEntryTypeId", "_NA_");
                input.put("debitCreditFlag", "C");
                input.put("acctgTransEntrySeqId", Integer.toString(transEntrySeqId++));
                GenericValue creditAcctTrans = delegator.makeValue("AcctgTransEntry", input);
                transEntries.add(creditAcctTrans);
            }


            // Create the transaction and post it
            input = UtilMisc.toMap("transactionDate", transactionDate, "userLogin", userLogin);
            input.put("acctgTransEntries", transEntries);
            input.put("glFiscalTypeId", "ACTUAL");
            input.put("acctgTransTypeId", "MANUFACTURING_ATX");
            input.put("workEffortId", productionRunId);
            Map output = dispatcher.runSync("createAcctgTransAndEntries", input);
            if (ServiceUtil.isError(output) || ServiceUtil.isFailure(output)) {
                return output;
            } else {
                result.put("acctgTransId", output.get("acctgTransId"));
            }


        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, MODULE);
        }


        return result;
    }

}
