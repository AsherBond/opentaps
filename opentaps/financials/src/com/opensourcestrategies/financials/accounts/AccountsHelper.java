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

package com.opensourcestrategies.financials.accounts;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.base.entities.PostalAddress;
import org.opentaps.common.agreement.AgreementReader;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.domain.billing.invoice.Invoice;
import org.opentaps.domain.billing.invoice.InvoiceRepositoryInterface;
import org.opentaps.domain.party.Party;
import org.opentaps.domain.party.PartyRepositoryInterface;
import org.opentaps.financials.domain.billing.invoice.InvoiceRepository;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.ofbiz.Repository;

public class AccountsHelper {

    private static String MODULE = AccountsHelper.class.getName();

    public static int decimals = UtilNumber.getBigDecimalScale("fin_arithmetic.properties", "financial.statements.decimals");
    public static int rounding = UtilNumber.getBigDecimalRoundingMode("fin_arithmetic.properties", "financial.statements.rounding");
    protected static final BigDecimal ZERO = BigDecimal.ZERO;

    // what gl accounts to use for the customer, vendor and commission statements and balances reports
    public static final List<String> CUSTOMER_RECEIVABLE_ACCTS = UtilMisc.toList("ACCOUNTS_RECEIVABLE", "CUSTOMER_CREDIT", "CUSTOMER_DEPOSIT", "INTRSTINC_RECEIVABLE");
    public static final List<String> VENDOR_PAYABLE_ACCTS = UtilMisc.toList("ACCOUNTS_PAYABLE", "PREPAID_EXPENSES");
    public static final List<String> COMMISSION_PAYABLE_ACCTS = UtilMisc.toList("COMMISSIONS_PAYABLE");

    /**
     * Find the sum of transaction entry amounts by partyId for the given parameters.
     *
     * @param organizationPartyId filter transaction entry by this organizationPartyId
     * @param glAccountTypeIds filter transaction entry by these glAccountTypeIds
     * @param glFiscalTypeId filter transaction entry by this glFiscalTypeId
     * @param debitCreditFlag filter transaction entry by this debitCreditFlag
     * @param partyId filter transaction entry by this partyId, optional
     * @param roleTypeId currently not used because it seems to cause problems when there is a receivable from a supplier or such.
     * @param asOfDate filter transaction entry by transaction date less than or equal to this date
     * @param delegator a <code>Delegator</code>
     * @return the list of <code>AcctgTransEntryPartySum</code> <code>GenericValue</code> found
     * @throws GenericEntityException if an error occurs
     */
    public static List<GenericValue> getAcctgTransPartySums(String organizationPartyId, List<String> glAccountTypeIds, String glFiscalTypeId, String debitCreditFlag, String partyId, String roleTypeId, Timestamp asOfDate, Delegator delegator) throws GenericEntityException {

        List<EntityCondition> conditions = UtilMisc.<EntityCondition>toList(
                EntityCondition.makeCondition("organizationPartyId", EntityOperator.EQUALS, organizationPartyId),
                EntityCondition.makeCondition("debitCreditFlag", EntityOperator.EQUALS, debitCreditFlag),
                EntityCondition.makeCondition("isPosted", EntityOperator.EQUALS, "Y"),
                EntityCondition.makeCondition("glAccountTypeId", EntityOperator.IN, glAccountTypeIds),
                EntityCondition.makeCondition("glFiscalTypeId", EntityOperator.EQUALS, glFiscalTypeId),
                EntityCondition.makeCondition("transactionDate", EntityOperator.LESS_THAN_EQUAL_TO, asOfDate));
        conditions.add(EntityCondition.makeCondition("partyId", EntityOperator.NOT_EQUAL, null));
        if (partyId != null) {
            conditions.add(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId));
        }
        /* SC 20070515 commented this out as it seems to cause problems when there is a receivable from a supplier
        if (roleTypeId != null) {
            conditions.add(EntityCondition.makeCondition("roleTypeId", EntityOperator.EQUALS, roleTypeId));
        }
        */
        EntityCondition findConditions = EntityCondition.makeCondition(conditions, EntityOperator.AND);

        List<String> fieldsToGet = UtilMisc.toList("partyId", "amount");

        List<GenericValue> transactionEntries = delegator.findByCondition("AcctgTransEntryPartySum", findConditions,
                fieldsToGet, // get these fields
                UtilMisc.toList("partyId")); // order by these fields

        return transactionEntries;
    }

    /**
     * As above, but for single glAccountTypeId.
     * Convenience method.
     *
     * @param organizationPartyId filter transaction entry by this organizationPartyId
     * @param glAccountTypeId filter transaction entry by this glAccountTypeId
     * @param glFiscalTypeId filter transaction entry by this glFiscalTypeId
     * @param debitCreditFlag filter transaction entry by this debitCreditFlag
     * @param partyId filter transaction entry by this partyId, optional
     * @param roleTypeId currently not used because it seems to cause problems when there is a receivable from a supplier or such.
     * @param asOfDate filter transaction entry by transaction date less than or equal to this date
     * @param delegator a <code>Delegator</code>
     * @return the list of <code>AcctgTransEntryPartySum</code> <code>GenericValue</code> found
     * @throws GenericEntityException if an error occurs
     */
    public static List<GenericValue> getAcctgTransPartySums(String organizationPartyId, String glAccountTypeId, String glFiscalTypeId, String debitCreditFlag, String partyId, String roleTypeId, Timestamp asOfDate, Delegator delegator) throws GenericEntityException {
        return getAcctgTransPartySums(organizationPartyId, UtilMisc.toList(glAccountTypeId), glFiscalTypeId, debitCreditFlag,
                partyId, roleTypeId, asOfDate, delegator);
    }

    /**
     * Canonical method to get transaction balances by glAccountTypeId.
     * Use one of the helper methods defined after this method.
     *
     * @param   glAccountTypeIds Account types to sum over. this determines the sign of the resulting balance.
     *                          For instance, in the case of receivables (incoming), sum of all debit - sum of all credit.
     *                          In the case of payables (outgoing),  sum of all credit - sum of all debit.
     * @param   organizationPartyId the organization party ID
     * @param   partyId         If specified, limit the search result to the given partyId
     * @param   roleTypeId
     * @param   glFiscalTypeId
     * @param   asOfDateTime    Timestamp to sum up to. TODO: investigate the boundary conditions
     * @param   delegator       a <code>Delegator</code> instance
     * @return a <code>Map</code> of partyId -> balance amount
     * @throws  GenericEntityException if an error occurs
     */
    public static Map<String, BigDecimal> getBalancesHelper(List<String> glAccountTypeIds, String organizationPartyId, String partyId, String roleTypeId, String glFiscalTypeId,
            Timestamp asOfDateTime, Delegator delegator) throws GenericEntityException {

        // Set up convenience boolean for testing receivables/payables TODO this is not robust, can break in future
        boolean isReceivable = (glAccountTypeIds.contains("ACCOUNTS_RECEIVABLE") ? true : false);

        // Query for debit and credit balances
        // NOTE: the set of accounts defined so far for commissions, customers and vendors are all the same type: receivalbes or payables.
        // Thus, it is ok to grab all Debits/Credits at once as done here.  If this assumption changes, will need to alter this logic to deal with the asymmetry.
        List<GenericValue> debitBalances = getAcctgTransPartySums(organizationPartyId, glAccountTypeIds, glFiscalTypeId, "D", partyId, roleTypeId, asOfDateTime, delegator);
        List<GenericValue> creditBalances = getAcctgTransPartySums(organizationPartyId, glAccountTypeIds, glFiscalTypeId, "C", partyId, roleTypeId, asOfDateTime, delegator);

        // return map has key partyId to value balance
        Map<String, BigDecimal> balances = FastMap.newInstance();

        // go through debits and put either the (debitBalance) for receivables or (-debitBalance) for payables
        for (Iterator<GenericValue> iter = debitBalances.iterator(); iter.hasNext();) {
            GenericValue balance = iter.next();
            BigDecimal balanceAmount = balance.getBigDecimal("amount").setScale(decimals, rounding);
            if (!isReceivable) {
                balanceAmount = balanceAmount.negate();
            }

            // see if a debitBalance exists, otherwise default to ZERO
            BigDecimal debitBalance = balances.get(balance.getString("partyId"));
            if (debitBalance == null) {
                debitBalance = ZERO;
            }

            // add them together and put in balance map
            balanceAmount = balanceAmount.add(debitBalance);
            balances.put(balance.getString("partyId"), balanceAmount);
        }

        // now go through credits and add to debitBalance (default ZERO) the following: (creditBalance) for payables or (-creditBalance) for receivables
        for (Iterator<GenericValue> iter = creditBalances.iterator(); iter.hasNext();) {
            GenericValue balance = iter.next();
            BigDecimal balanceAmount = balance.getBigDecimal("amount").setScale(decimals, rounding);
            if (isReceivable) {
                balanceAmount = balanceAmount.negate();
            }

            // see if a creditBalance exists, otherwise default to ZERO
            BigDecimal creditBalance = balances.get(balance.getString("partyId"));
            if (creditBalance == null) {
                creditBalance = ZERO;
            }

            // add them together and put in balance map
            balanceAmount = balanceAmount.add(creditBalance);
            balances.put(balance.getString("partyId"), balanceAmount);
        }

        return balances;
    }

    /** Gets ACCOUNTS_RECEIVABLE balances for all customers in an organizatoin up to the asOfDateTime. Returns a Map of partyId keys to BigDecimal balance values */
    public static Map<String, BigDecimal> getBalancesForAllCustomers(String organizationPartyId, String glFiscalTypeId, Timestamp asOfDateTime, Delegator delegator) throws GenericEntityException {
        return getBalancesHelper(CUSTOMER_RECEIVABLE_ACCTS, organizationPartyId, null, "BILL_TO_CUSTOMER", glFiscalTypeId, asOfDateTime, delegator);
    }

    /** Gets ACCOUNTS_PAYABLE balances for all vendors in an organizatoin up to the asOfDateTime. Returns a Map of partyId keys to BigDecimal balance values */
    public static Map<String, BigDecimal> getBalancesForAllVendors(String organizationPartyId, String glFiscalTypeId, Timestamp asOfDateTime, Delegator delegator) throws GenericEntityException {
        return getBalancesHelper(VENDOR_PAYABLE_ACCTS, organizationPartyId, null, "BILL_FROM_VENDOR", glFiscalTypeId, asOfDateTime, delegator);
    }

    /** Gets COMMISSIONS_PAYABLE balances for all sales rep in an organizatoin up to the asOfDateTime. Returns a Map of partyId keys to BigDecimal balance values */
    public static Map<String, BigDecimal> getBalancesForAllCommissions(String organizationPartyId, String glFiscalTypeId, Timestamp asOfDateTime, Delegator delegator) throws GenericEntityException {
        return getBalancesHelper(COMMISSION_PAYABLE_ACCTS, organizationPartyId, null, "SALES_REP", glFiscalTypeId, asOfDateTime, delegator);
    }

    /** Gets ACCOUNTS_RECEIVABLE balance for a given customer in an organizatoin up to the asOfDateTime. Returns the balance as a BigDecimal.  If there is no balance information, returns ZERO. */
    public static BigDecimal getBalanceForCustomerPartyId(String customerPartyId, String organizationPartyId, String glFiscalTypeId, Timestamp asOfDateTime, Delegator delegator)
        throws GenericEntityException {
        Map<String, BigDecimal> balances = getBalancesHelper(CUSTOMER_RECEIVABLE_ACCTS, organizationPartyId, customerPartyId, "BILL_TO_CUSTOMER", glFiscalTypeId, asOfDateTime, delegator);
        BigDecimal balance = balances.get(customerPartyId);
        return balance == null ? BigDecimal.ZERO : balance;
    }

    /** Gets ACCOUNTS_PAYABLE balance for a given vendor in an organizatoin up to the asOfDateTime. Returns the balance as a BigDecimal.  If there is no balance information, returns ZERO. */
    public static BigDecimal getBalanceForVendorPartyId(String vendorPartyId, String organizationPartyId, String glFiscalTypeId, Timestamp asOfDateTime, Delegator delegator)
        throws GenericEntityException {
        Map<String, BigDecimal> balances = getBalancesHelper(VENDOR_PAYABLE_ACCTS, organizationPartyId, vendorPartyId, "BILL_FROM_VENDOR", glFiscalTypeId, asOfDateTime, delegator);
        BigDecimal balance = balances.get(vendorPartyId);
        return balance == null ? BigDecimal.ZERO : balance;
    }

    /** Gets COMMISSIONS_PAYABLE balance for a given sales rep in an organizatoin up to the asOfDateTime. Returns the balance as a BigDecimal. If there is no balance information, returns ZERO. */
    public static BigDecimal getBalanceForCommissionPartyId(String commissionPartyId, String organizationPartyId, String glFiscalTypeId, Timestamp asOfDateTime, Delegator delegator)
        throws GenericEntityException {
        Map<String, BigDecimal> balances = getBalancesHelper(COMMISSION_PAYABLE_ACCTS, organizationPartyId, commissionPartyId, "SALES_REP", glFiscalTypeId, asOfDateTime, delegator);
        BigDecimal balance = balances.get(commissionPartyId);
        return balance == null ? BigDecimal.ZERO : balance;
    }

    /**
     * Gets unpaid invoice balances for customer (SALES_INVOICE)
     * See getUnpaidInvoicesHelper for parameter information
     */
    public static Map<Integer, List<Invoice>> getUnpaidInvoicesForCustomers(String organizationPartyId, List<Integer> daysOutstandingPoints, Timestamp asOfDateTime, Delegator delegator, TimeZone timeZone, Locale locale)
        throws GenericEntityException, RepositoryException {
        return getUnpaidInvoicesHelper(organizationPartyId, "SALES_INVOICE", daysOutstandingPoints, asOfDateTime, delegator, timeZone, locale);
    }

    public static Map<Integer, List<Invoice>> getUnpaidInvoicesForCustomers(String organizationPartyId, List<Integer> daysOutstandingPoints, Timestamp asOfDateTime, List<String> invoiceStatusIds, Delegator delegator, TimeZone timeZone, Locale locale)
        throws GenericEntityException, RepositoryException {
        return getUnpaidInvoicesHelper(organizationPartyId, null, "SALES_INVOICE", daysOutstandingPoints, asOfDateTime, invoiceStatusIds, delegator, timeZone, locale);
    }

    /**
     * Gets unpaid invoice balances for single customer (SALES_INVOICE)
     * See getUnpaidInvoicesHelper for parameter information
     */
    public static Map<Integer, List<Invoice>> getUnpaidInvoicesForCustomer(String organizationPartyId, String payeePartyId, List<Integer> daysOutstandingPoints, Timestamp asOfDateTime, Delegator delegator, TimeZone timeZone, Locale locale)
        throws GenericEntityException, RepositoryException {
        return getUnpaidInvoicesHelper(organizationPartyId, payeePartyId, "SALES_INVOICE", daysOutstandingPoints, asOfDateTime, delegator, timeZone, locale);
    }

    public static Map<Integer, List<Invoice>> getUnpaidInvoicesForCustomer(String organizationPartyId, String payeePartyId, List<Integer> daysOutstandingPoints, Timestamp asOfDateTime, List<String> invoiceStatusIds, Delegator delegator, TimeZone timeZone, Locale locale)
        throws GenericEntityException, RepositoryException {
        return getUnpaidInvoicesHelper(organizationPartyId, payeePartyId, "SALES_INVOICE", daysOutstandingPoints, asOfDateTime, invoiceStatusIds, delegator, timeZone, locale);
    }

    public static Map<Integer, List<Invoice>> getUnpaidInvoicesForCustomer(String organizationPartyId, String payeePartyId, List<Integer> daysOutstandingPoints, Timestamp asOfDateTime, List<String> invoiceStatusIds, Delegator delegator, TimeZone timeZone, Locale locale, boolean useAgingDate)
        throws GenericEntityException, RepositoryException {
        return getUnpaidInvoicesHelper(organizationPartyId, payeePartyId, "SALES_INVOICE", daysOutstandingPoints, asOfDateTime, invoiceStatusIds, delegator, timeZone, locale, useAgingDate);
    }

    /**
     * Gets unpaid invoice balances for vendor (PURCHASE_INVOICE)
     * See getUnpaidInvoicesHelper for parameter information
     */
    public static Map<Integer, List<Invoice>> getUnpaidInvoicesForVendors(String organizationPartyId, List<Integer> daysOutstandingPoints, Timestamp asOfDateTime, Delegator delegator, TimeZone timeZone, Locale locale)
        throws GenericEntityException, RepositoryException {
        return getUnpaidInvoicesHelper(organizationPartyId, "PURCHASE_INVOICE", daysOutstandingPoints, asOfDateTime, delegator, timeZone, locale);
    }

    public static Map<Integer, List<Invoice>> getUnpaidInvoicesForVendors(String organizationPartyId, List<Integer> daysOutstandingPoints, Timestamp asOfDateTime, List<String> invoiceStatusIds, Delegator delegator, TimeZone timeZone, Locale locale)
        throws GenericEntityException, RepositoryException {
        return getUnpaidInvoicesHelper(organizationPartyId, null, "PURCHASE_INVOICE", daysOutstandingPoints, asOfDateTime, invoiceStatusIds, delegator, timeZone, locale);
    }

    /**
     * Gets unpaid invoice balances for single vendor (PURCHASE_INVOICE)
     * See getUnpaidInvoicesHelper for parameter information
     */
    public static Map<Integer, List<Invoice>> getUnpaidInvoicesForVendor(String organizationPartyId, String partyId, List<Integer> daysOutstandingPoints, Timestamp asOfDateTime, Delegator delegator, TimeZone timeZone, Locale locale)
        throws GenericEntityException, RepositoryException {
        return getUnpaidInvoicesHelper(organizationPartyId, partyId, "PURCHASE_INVOICE", daysOutstandingPoints, asOfDateTime, delegator, timeZone, locale);
    }

    public static Map<Integer, List<Invoice>> getUnpaidInvoicesForVendor(String organizationPartyId, String partyId, List<Integer> daysOutstandingPoints, Timestamp asOfDateTime, List<String> invoiceStatusIds, Delegator delegator, TimeZone timeZone, Locale locale)
        throws GenericEntityException, RepositoryException {
        return getUnpaidInvoicesHelper(organizationPartyId, partyId, "PURCHASE_INVOICE", daysOutstandingPoints, asOfDateTime, invoiceStatusIds, delegator, timeZone, locale);
    }

    /**
     * Gets unpaid invoice balances for commission (COMMISSION_INVOICE)
     * See getUnpaidInvoicesHelper for parameter information
     */
    public static Map<Integer, List<Invoice>> getUnpaidInvoicesForCommissions(String organizationPartyId, List<Integer> daysOutstandingPoints, Timestamp asOfDateTime, Delegator delegator, TimeZone timeZone, Locale locale)
        throws GenericEntityException, RepositoryException {
        return getUnpaidInvoicesHelper(organizationPartyId, "COMMISSION_INVOICE", daysOutstandingPoints, asOfDateTime, delegator, timeZone, locale);
    }

    public static Map<Integer, List<Invoice>> getUnpaidInvoicesForCommissions(String organizationPartyId, List<Integer> daysOutstandingPoints, Timestamp asOfDateTime, List<String> invoiceStatusIds, Delegator delegator, TimeZone timeZone, Locale locale)
        throws GenericEntityException, RepositoryException {
        return getUnpaidInvoicesHelper(organizationPartyId, null, "COMMISSION_INVOICE", daysOutstandingPoints, asOfDateTime, invoiceStatusIds, delegator, timeZone, locale);
    }

    public static Map<Integer, List<Invoice>> getUnpaidInvoicesHelper(String organizationPartyId, String invoiceTypeId, List<Integer> daysOutstandingPoints,
            Timestamp asOfDateTime, Delegator delegator, TimeZone timeZone, Locale locale) throws GenericEntityException, RepositoryException {
        return getUnpaidInvoicesHelper(organizationPartyId, null, invoiceTypeId, daysOutstandingPoints, asOfDateTime, delegator, timeZone, locale);
    }

    public static Map<Integer, List<Invoice>> getUnpaidInvoicesHelper(String organizationPartyId, String payeePartyId, String invoiceTypeId, List<Integer> daysOutstandingPoints,
            Timestamp asOfDateTime, Delegator delegator, TimeZone timeZone, Locale locale) throws GenericEntityException, RepositoryException {
        return getUnpaidInvoicesHelper(organizationPartyId, payeePartyId, invoiceTypeId, daysOutstandingPoints, asOfDateTime, null, delegator, timeZone, locale, false);
    }

    public static Map<Integer, List<Invoice>> getUnpaidInvoicesHelper(String organizationPartyId, String payeePartyId, String invoiceTypeId, List<Integer> daysOutstandingPoints,
            Timestamp asOfDateTime, Delegator delegator, TimeZone timeZone, Locale locale, boolean useAgingDate) throws GenericEntityException, RepositoryException {
        return getUnpaidInvoicesHelper(organizationPartyId, payeePartyId, invoiceTypeId, daysOutstandingPoints, asOfDateTime, null, delegator, timeZone, locale, useAgingDate);
    }

    /**
     * Returns a Map of Integer (not int) days outstanding breakpoints and List of InvoiceWithOutstandingBalance objects for invoices whose days outstanding
     * is just less than the breakpoint days, but greater than the preceding (smaller) days outstanding breakpoint.
     *
     * Note:  The passed in asOfDate will be expanded to the end of that day.  This means, if your asOfDate is 2008-10-23 13:00:00, it will
     * be reinterpreted as 2008-10-23 23:59:59.999.  This prevents issues such as an invoice being created in the middle of the day
     * not showing up in some reports, such as customer statement, where the asOfDate is entered as 2008-10-23 00:00:00.000.
     *
     * @param organizationPartyId
     * @param invoiceTypeId
     * @param daysOutstandingPoints List of Integer (not int) days outstanding breakpoints, ie: UtilMisc.toList(new Integer(0), new Integer(30), new Integer(60), new Integer(90)
     * @param asOfDateTime
     * @param invoiceStatusIds List of Strings to filter invoice statuses
     * @param delegator
     * @param timeZone
     * @param locale
     * @param useAgingDate Whether to use the aging date or just the invoice date (for reference, legacy methods did not use aging date)
     * @return
     * @throws GenericEntityException
     */
    public static Map<Integer, List<Invoice>> getUnpaidInvoicesHelper(String organizationPartyId, String payeePartyId, String invoiceTypeId, List<Integer> daysOutstandingPoints,
            Timestamp asOfDateTime, List<String> invoiceStatusIds, Delegator delegator, TimeZone timeZone, Locale locale, boolean useAgingDate) throws GenericEntityException, RepositoryException {

        // make sure the as of date is at the end of the day, this normalization prevents issues such as invoices that were just created missing from a report
        asOfDateTime = UtilDateTime.getDayEnd(asOfDateTime);

        // future asOfDateTime results are correct but unintuitive, so limit time range from anywhere in past to now
        Timestamp now = UtilDateTime.getDayEnd( UtilDateTime.nowTimestamp(), timeZone, locale );
        if (asOfDateTime.after(now)) asOfDateTime = now;

        // which field is equal to the organizationPartyId?  Depends on the invoice type
        String organizationInvoiceField = null;
        String payeeInvoiceField = null;
        if (invoiceTypeId.equals("PURCHASE_INVOICE") || invoiceTypeId.equals("COMMISSION_INVOICE")) {
            organizationInvoiceField = "partyId";
            payeeInvoiceField = "partyIdFrom" ;
        } else {
            organizationInvoiceField = "partyIdFrom";
            payeeInvoiceField = "partyId" ;
        }

        // used to select invoices which are not paid by the as of date time but which are created before the as of date
        EntityCondition invoiceDateConditions = EntityCondition.makeCondition(EntityOperator.OR,
                EntityCondition.makeCondition("paidDate", EntityOperator.GREATER_THAN, asOfDateTime),
                EntityCondition.makeCondition("paidDate", EntityOperator.EQUALS, null));

        List<EntityCondition> conditionList = UtilMisc.<EntityCondition>toList(
                EntityCondition.makeCondition("statusId", EntityOperator.NOT_IN, UtilMisc.toList("INVOICE_VOIDED", "INVOICE_WRITEOFF", "INVOICE_CANCELLED", "INVOICE_PAID")),
                EntityCondition.makeCondition("invoiceTypeId", EntityOperator.EQUALS, invoiceTypeId),
                EntityCondition.makeCondition("invoiceDate", EntityOperator.LESS_THAN_EQUAL_TO, asOfDateTime),
                EntityCondition.makeCondition(organizationInvoiceField, EntityOperator.EQUALS, organizationPartyId),
                invoiceDateConditions) ;

        if (payeePartyId != null) {
            conditionList.add(EntityCondition.makeCondition(payeeInvoiceField, EntityOperator.EQUALS, payeePartyId)) ;
        }

        if (UtilValidate.isNotEmpty(invoiceStatusIds)) {
            conditionList.add(EntityCondition.makeCondition("statusId", EntityOperator.IN, invoiceStatusIds));
        }

        EntityCondition conditions = EntityCondition.makeCondition(conditionList, EntityOperator.AND);

        // search and sort results on invoiceDate ascending
        List<GenericValue> invoices = delegator.findByCondition("Invoice", conditions, null, UtilMisc.toList("invoiceDate"));

        // group the results into date buckets using the aging date as a basis
        return groupInvoicesByDateBucket(invoices, asOfDateTime, daysOutstandingPoints, delegator, useAgingDate);
    }

    public static Map<Integer, List<Invoice>> getUnpaidInvoicesHelper(String organizationPartyId, String payeePartyId, String invoiceTypeId, List<Integer> daysOutstandingPoints,
        Timestamp asOfDateTime, List<String> invoiceStatusIds, Delegator delegator, TimeZone timeZone, Locale locale) throws GenericEntityException, RepositoryException {
        return getUnpaidInvoicesHelper(organizationPartyId, payeePartyId, invoiceTypeId, daysOutstandingPoints, asOfDateTime, invoiceStatusIds, delegator, timeZone, locale, false);
    }

    /**
     * Gets unpaid invoice balances for parties of a given classificaiton.
     *
     * We have two new view entities InvoiceAndPartyClassificationDisbursement and InvoiceAndPartyClassificationReceipt.  It's impossible to have one view entity
     * that can handle both types at once, so we'll have to base our queries on these two entities.  It would help if the group can be associated with an invoice type
     * so that we can pick which view entity to use.  This would help make sense of the results, as there is only one invoice type on the list, rather than a mix.
     * Alternatively, a heuristic could be used to detect the invoice type based on the group members. This is prone to being wrong.  Finally we can ignore type and
     * just do a lookup across both view entities and let the user deal with the possibility of mixed invoice types.
     *
     * Right now it's done by looking up only the receipts, as that's what we need.
     */
    public static Map<Integer, List<Invoice>> getUnpaidInvoicesForPartyClassificationGroup(String organizationPartyId, String partyClassificationGroupId, List<Integer> daysOutstandingPoints, Timestamp asOfDateTime, Delegator delegator, boolean useAgingDate) throws GenericEntityException, RepositoryException {
        EntityCondition mainConditions = EntityCondition.makeCondition(EntityOperator.AND,
                    EntityCondition.makeCondition("partyClassificationGroupId", EntityOperator.EQUALS, partyClassificationGroupId),
                    EntityUtil.getFilterByDateExpr());

        // find receipts
        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                                        mainConditions,
                                        EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, organizationPartyId));
        List<GenericValue> invoices = delegator.findByAnd("InvoiceAndPartyClassificationReceipt", conditions);

        // group the results into date buckets
        return groupInvoicesByDateBucket(invoices, asOfDateTime, daysOutstandingPoints, delegator, useAgingDate);
    }

    private static Map<Integer, List<Invoice>> groupInvoicesByDateBucket(List<GenericValue> invoiceList, Timestamp asOfDateTime, List<Integer> daysOutstandingPoints, Delegator delegator, boolean useAgingDate) throws GenericEntityException, RepositoryException {

        // create a temporary bridge to our refactoring
        InvoiceRepository repository = new InvoiceRepository(delegator);
        List<Invoice> invoices = (List<Invoice>) Repository.loadFromGeneric(Invoice.class, invoiceList, repository);

        // Create outstandingInvoices map, populate with empty buckets, and store the maximum days outstanding of all the break points
        Map<Integer, List<Invoice>> outstandingInvoicesByAge = FastMap.newInstance();
        int maxDaysOutstanding = 0;
        for (Iterator<Integer> it = daysOutstandingPoints.iterator(); it.hasNext();) {
            Integer nextDaysOutstanding = it.next();
            outstandingInvoicesByAge.put(nextDaysOutstanding, new LinkedList<Invoice>());
            if (nextDaysOutstanding.intValue() > maxDaysOutstanding) {
                maxDaysOutstanding = nextDaysOutstanding.intValue();
            }
        }

        // loop through invoices and put them into the right date bucket
        for (Invoice invoice : invoices) {

            // skip zero balance invoices
            if (invoice.getOpenAmount().compareTo(ZERO) == 0) continue;

            // Calculate number of days elapsed
            Integer numberOfDays = useAgingDate ? invoice.getDaysAged(asOfDateTime) : invoice.getDaysOutstanding(asOfDateTime);

            // Put this invoice into the List at the first days outstanding (DSO) break point which is greater than the current one, or
            // the last DSO break point in the list of DSO break points, whichever one comes first
            boolean foundDaysOutstandingPoint = false;
            Iterator<Integer> it = daysOutstandingPoints.iterator();
            while ((it.hasNext()) && (!foundDaysOutstandingPoint)) {
                Integer daysOutstandingPoint = (Integer) it.next();
                if (numberOfDays.compareTo(daysOutstandingPoint) == -1) { // -1 is less than
                    List<Invoice> invoicesByDaysOutstanding = (LinkedList<Invoice>) outstandingInvoicesByAge.get(daysOutstandingPoint);
                    invoicesByDaysOutstanding.add(invoice);
                    foundDaysOutstandingPoint = true;
                }
            }
            if (!foundDaysOutstandingPoint) {
                List<Invoice> invoicesByDaysOutstanding = (LinkedList<Invoice>) outstandingInvoicesByAge.get(new Integer(maxDaysOutstanding));
                invoicesByDaysOutstanding.add(invoice);
            }
        }
        return outstandingInvoicesByAge;
    }

    /**
     * Calculates finance charges as of the current date and time using method below
     * @param delegator
     * @param organizationPartyId
     * @param invoiceToPartyId
     * @param partyClassificationGroupId
     * @param interestRate
     * @param gracePeriod
     * @param timeZone
     * @param locale
     * @return
     * @throws GenericEntityException
     */
    public static Map<Invoice, Map<String, BigDecimal>> calculateFinanceCharges(Delegator delegator, String organizationPartyId, String invoiceToPartyId, String partyClassificationGroupId, BigDecimal interestRate, int gracePeriod, TimeZone timeZone, Locale locale) throws GenericEntityException, RepositoryException {
        return calculateFinanceCharges(delegator, organizationPartyId, invoiceToPartyId, partyClassificationGroupId, interestRate, UtilDateTime.nowTimestamp(), gracePeriod, timeZone, locale);

    }

    /**
     * Calculates days outstanding, outstanding amount, total interest previously charged, and total interest due on all outstanding sales invoices for an organization. May be
     *  limited by invoiceToPartyId and/or party classification of the invoiceToParty. Returns a map in the same order returned by the AccountsHelper.getUnpaidInvoicesFor* methods
     *  where the keys are invoices and the values are maps of data (days outstanding, interest charged, etc.)
     */
    public static Map<Invoice, Map<String, BigDecimal>> calculateFinanceCharges(Delegator delegator, String organizationPartyId, String invoiceToPartyId, String partyClassificationGroupId, BigDecimal interestRate, Timestamp asOfDateTime, int gracePeriod, TimeZone timeZone, Locale locale) throws GenericEntityException, RepositoryException {

        BigDecimal dailyInterestRate = interestRate.movePointLeft(2).setScale(100, BigDecimal.ROUND_HALF_UP).divide(new BigDecimal("365.25"), BigDecimal.ROUND_HALF_UP);

        Map<Integer, List<Invoice>> invoicesByAge = null;
        if (UtilValidate.isNotEmpty(invoiceToPartyId)) {
            invoicesByAge = AccountsHelper.getUnpaidInvoicesForCustomer(organizationPartyId, invoiceToPartyId, UtilMisc.toList(gracePeriod, gracePeriod + 1), asOfDateTime, delegator, timeZone, locale);
        } else if (UtilValidate.isNotEmpty(partyClassificationGroupId)) {
            invoicesByAge = AccountsHelper.getUnpaidInvoicesForPartyClassificationGroup(organizationPartyId, partyClassificationGroupId, UtilMisc.toList(gracePeriod, gracePeriod + 1), asOfDateTime, delegator, true);
        } else {
            invoicesByAge = AccountsHelper.getUnpaidInvoicesForCustomers(organizationPartyId, UtilMisc.toList(gracePeriod, gracePeriod + 1), asOfDateTime, delegator, timeZone, locale);
        }

        // The AccountsHelper.getUnpaidInvoices* methods return a map of lists of Invoice objects separated into aged tiers. We've instructed
        //  the method to use two tiers, so get the one which holds all invoices older than the grace period:
        List<Invoice> invoicesWithOutstanding = invoicesByAge.get(gracePeriod + 1);

        // Now we have a list of InvoiceWithOutstandingBalance objects, so we need to loop through them and assemble a list of actual invoices, and
        //  a map of supporting data keyed by invoiceId, for the form widget to use
        Map<Invoice, Map<String, BigDecimal>> invoiceData = new LinkedHashMap<Invoice, Map<String, BigDecimal>>();

        for (Invoice invoice : invoicesWithOutstanding) {

            // We only care about invoices in the INVOICE_READY state
            if ("INVOICE_READY".equals(invoice.getString("statusId"))) {

                // Days outstanding is the number of *whole* days outstanding. Always rounded down, even if the invoice is only 1ms short of a full day.
                BigDecimal daysOutstanding = UtilCommon.asBigDecimal(invoice.getDaysOutstanding(asOfDateTime)); // legacy requires BigDecimal
                BigDecimal outstandingAmount = invoice.getOpenAmount(asOfDateTime);
                BigDecimal interestCharged = invoice.getInterestCharged();

                // Calculate the interest owing (days older than grace period * outstanding amount - interest already charged for the invoice)
                BigDecimal daysPastDue = new BigDecimal(daysOutstanding.intValue() - gracePeriod).setScale(0, BigDecimal.ROUND_UNNECESSARY);
                BigDecimal interestAmount = dailyInterestRate.multiply(daysPastDue).multiply(outstandingAmount).subtract(interestCharged).setScale(2, BigDecimal.ROUND_HALF_UP);

                invoiceData.put(invoice, UtilMisc.toMap("daysOutstanding", daysOutstanding, "outstandingAmount", outstandingAmount, "interestCharged", interestCharged, "interestAmount", interestAmount));
            }
        }

        return invoiceData;
    }

    /**
     *
     * @param partyIdTo The partyId of the customer
     * @param partyIdFrom The partyId of the organization
     * @param amountToCheck The amount to check against the customer's current balance and available credit
     * @param currencyUomId Currency of the amount to check
     * @return true if the amount to check exceeds the customer's available credit, false if the amount does not
     *          exceed the available credit or if the parties have no credit limit sales agrement
     * @throws GenericEntityException
     * @throws org.ofbiz.service.GenericServiceException
     */
    public static boolean amountExceedsAvailableCreditAgreement(Delegator delegator, LocalDispatcher dispatcher, String partyIdTo, String partyIdFrom, BigDecimal amountToCheck, String currencyUomId) throws GenericEntityException, GenericServiceException {

        amountToCheck = amountToCheck.setScale(decimals, rounding);

        // Check if party has an active credit limit agreement
        AgreementReader reader = AgreementReader.findAgreement(partyIdFrom, partyIdTo, "SALES_AGREEMENT", "CREDIT_LIMIT", "AGR_ACTIVE", delegator);
        if (reader == null) {
            return false;
        }
        Timestamp now = UtilDateTime.nowTimestamp();

        // Get the credit limit and convert it to the organization currency
        BigDecimal limit = reader.getTermValueBigDecimal("CREDIT_LIMIT");
        if (limit == null) {
            throw new GenericServiceException("No credit limit value specified for agreement [" + reader.getAgreementId() + "].");
        }
        String agreementCurrencyUomId = reader.getTermCurrency("CREDIT_LIMIT");
        if (agreementCurrencyUomId == null) {
            throw new GenericServiceException("No currency defined for credit limit term");
        }
        if (!agreementCurrencyUomId.equals(currencyUomId)) {
            Map<String, Object> result = dispatcher.runSync("convertUom", UtilMisc.toMap("originalValue", limit, "uomId", agreementCurrencyUomId, "uomIdTo", currencyUomId, "asOfDate", now));
            if (ServiceUtil.isError(result)) {
                throw new GenericServiceException(ServiceUtil.getErrorMessage(result));
            }
            limit = new BigDecimal((Double) result.get("convertedValue"));
        }
        limit = limit.setScale(decimals, rounding);
        Debug.logInfo("Found a credit limit of [" + limit + "] from party [" + partyIdFrom + "] to party [" + partyIdTo + "] in agreement [" + reader.getAgreementId() + "]", MODULE);

        // Get the GL balance of this customer in this organization, which will be in the organization's currency
        BigDecimal balance = AccountsHelper.getBalanceForCustomerPartyId(partyIdTo, partyIdFrom, "ACTUAL", now, delegator);
        if (balance == null) {
            balance = BigDecimal.ZERO;
        }
        balance = balance.setScale(decimals, rounding);

        // If the GL balance + amountToCheck exceeds credit limit, then customer has insufficient funds in the pipe to pay this amount
        return limit.compareTo(balance.add(amountToCheck)) < 0;
    }

    /**
     * Assembles a list of a GL account's parents in the heirarchy
     * @param delegator
     * @param glAccountId
     * @return Reverse-order list of GenericValues representing parents of the glAccountId - E.G. [517000, 510000, 500000] for 517100
     * @throws GenericEntityException
     */
    public static List<GenericValue> getAccountParents(Delegator delegator, String glAccountId) throws GenericEntityException {
        HashMap<String, GenericValue> parents = getAccountParentsRec(delegator, glAccountId, null);
        parents.remove(glAccountId);
        List<GenericValue> parentList = EntityUtil.orderBy(parents.values(), UtilMisc.toList("glAccountId"));
        Collections.reverse(parentList);
        return parentList;
    }

    private static HashMap<String, GenericValue> getAccountParentsRec(Delegator delegator, String glAccountId, HashMap<String, GenericValue> parents) throws GenericEntityException {
        if (parents == null) parents = new HashMap<String, GenericValue>();
        if (glAccountId == null) {
            return parents;
        }
        GenericValue account = delegator.findByPrimaryKey("GlAccount", UtilMisc.toMap("glAccountId", glAccountId));
        if (account == null) {
            return parents;
        }
        parents.put(glAccountId, account);
        GenericValue parent = EntityUtil.getFirst(delegator.findByAnd("GlAccount", UtilMisc.toMap("glAccountId", account.getString("parentGlAccountId"))));
        if (parent == null) {
            return parents;
        }
        parents.put(account.getString("parentGlAccountId"), parent);
        return getAccountParentsRec(delegator, parent.getString("glAccountId"), parents);
    }

    /**
     * Prepares data to generate customer statement report.
     *
     * @param dl
     *     Domain loader
     * @param organizationPartyId
     *     Internal organization identifier
     * @param partyIds
     *     List of party ids which have to be included to report.
     * @param asOfDate
     * @param statementPeriod
     * @param useAgingDate if set to true, will use due date (where available) for invoices to determine the age of the invoice; if set to false, will always use invoice date
     * @param partyData
     *     <b>Out parameter</b> Pass empty <code>Map</code>.
     * @param locale
     * @param timeZone
     * @return
     *     Returns data that are used to create JasperReport data source. <br/>
     *     Also, partyData contains report parameters after this method called.
     * @throws RepositoryException
     * @throws GenericEntityException
     * @throws EntityNotFoundException
     */
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> customerStatement(DomainsLoader dl, String organizationPartyId, Set<String> partyIds, Timestamp asOfDate, int statementPeriod, boolean useAgingDate, Map partyData, Locale locale, TimeZone timeZone) throws RepositoryException, GenericEntityException, EntityNotFoundException {

        Delegator delegator = dl.getInfrastructure().getDelegator();

        // we will be getting more data from these repositories
        DomainsDirectory domains = dl.loadDomainsDirectory();
        PartyRepositoryInterface partyRepository = domains.getPartyDomain().getPartyRepository();
        InvoiceRepositoryInterface invoiceRepository = domains.getBillingDomain().getInvoiceRepository();

        // the start date of the statement (30 days ago)
        Timestamp startDate = UtilDateTime.getDayStart(asOfDate, -statementPeriod);

        List<Map<String, Object>> report = FastList.newInstance();

        if (UtilValidate.isNotEmpty(partyIds)) {
            for (String partyId : partyIds) {

                Map partyReport = new TreeMap();

                // we will need to get the closed invoices, which will be the complement of all invoices and open invoices
                Set<String> openInvoiceIds = FastSet.newInstance();
                Set<String> allInvoiceIds = FastSet.newInstance();

                // amounts we need to calculate for the party
                BigDecimal totalOpen = BigDecimal.ZERO;
                BigDecimal current = BigDecimal.ZERO;
                BigDecimal over1N = BigDecimal.ZERO;
                BigDecimal over2N = BigDecimal.ZERO;
                BigDecimal over3N = BigDecimal.ZERO;
                BigDecimal over4N = BigDecimal.ZERO;

                // get all sales invoices in ready or sent state grouped by date bucket from the as of date time
                List<Integer> periodList = Arrays.asList(statementPeriod, 2 * statementPeriod, 3 * statementPeriod, 4 * statementPeriod, 999);
                Map<Integer, List<Invoice>> dateBuckets = AccountsHelper.getUnpaidInvoicesForCustomer(organizationPartyId, partyId, periodList, asOfDate, UtilMisc.toList("INVOICE_READY", "INVOICE_SENT"), delegator, timeZone, locale, useAgingDate);

                // iterate through the date buckets and add up the total open amount while keeping track of bucket sums
                for (Integer bucket : dateBuckets.keySet()) {
                    List<Invoice> invoices = (List<Invoice>) dateBuckets.get(bucket);
                    for (Invoice invoice : invoices) {
                        Map<String, Object> invoiceRow = createInvoiceRow(invoice, true, partyId, useAgingDate);
                        partyReport.put((String) invoice.get("invoiceId"), invoiceRow);
                        openInvoiceIds.add((String) invoice.get("invoiceId"));

                        // compute sums (note that these are brackets, they don't include amounts from other brackets)
                        BigDecimal openAmount = (BigDecimal) invoiceRow.get("open_amount");
                        if (bucket == statementPeriod) { current = current.add(openAmount); }
                        else if (bucket == 2 * statementPeriod) { over1N = over1N.add(openAmount); }
                        else if (bucket == 3 * statementPeriod) { over2N = over2N.add(openAmount); }
                        else if (bucket == 4 * statementPeriod) { over3N = over3N.add(openAmount); }
                        else if (bucket == 999)                 { over4N = over4N.add(openAmount); }
                        totalOpen = totalOpen.add(openAmount);
                    }
                }

                // next get all received or confirmed payments applied to an invoice between the parties since the start date
                EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                        EntityCondition.makeCondition("invoiceId", EntityOperator.NOT_EQUAL, null),
                        EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, organizationPartyId),
                        EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, partyId),
                        EntityCondition.makeCondition("statusId", EntityOperator.IN, UtilMisc.toList("PMNT_RECEIVED", "PMNT_CONFIRMED")),
                        EntityCondition.makeCondition("effectiveDate", EntityOperator.GREATER_THAN, startDate)
                );
                List<GenericValue> payments = delegator.findByCondition("PaymentAndApplication", conditions, null, null);
                for (GenericValue payment : payments) {
                    partyReport.put(((String) payment.get("invoiceId")) + ((String) payment.get("paymentId")), createPaymentRow(payment, partyId));
                    allInvoiceIds.add((String) payment.get("invoiceId"));
                }

                // get the closed invoices that we must also list, which are those associated with settled payments
                // we can obtain these Ids by the complement of the open invoices and the list of all invoices
                allInvoiceIds.removeAll(openInvoiceIds);
                if (allInvoiceIds.size() > 0) {
                    List<Invoice> invoices = invoiceRepository.getInvoicesByIds(allInvoiceIds);
                    for (Invoice invoice : invoices) {
                        partyReport.put(invoice.getInvoiceId(), createInvoiceRow(invoice, false, partyId, useAgingDate));
                    }
                }

                // load the domain entity
                Party party = partyRepository.getPartyById(partyId);

                // get the party billing address and save it as a preformatted string
                PostalAddress address = party.getBillingAddress();
                if (address != null) {
                    StringBuffer sb = new StringBuffer();
                    if (address.getToName() != null) {
                        sb.append(address.getToName()).append("\n");
                    }
                    if (address.getAttnName() != null) {
                        sb.append(address.getAttnName()).append("\n");
                    }
                    sb.append(address.getAddress1()).append("\n");
                    if (address.getAddress2() != null) {
                        sb.append(address.getAddress2()).append("\n");
                    }
                    // TODO for now this is using the state province geo id, but when we use party domain it should be more robust
                    sb.append(address.getCity()).append(", ").append(address.getStateProvinceGeoId());
                    sb.append(" ").append(address.getPostalCode());
                    partyData.put(partyId + "billing_address", sb.toString());
                } else {
                    partyData.put(partyId + "billing_address", "Address not on file.");
                }

                // save the per party data (for the overages, we only show them if they're non zero)
                partyData.put(partyId + "total_open", totalOpen);
                if (current.signum() > 0) { partyData.put(partyId + "current", current); }
                if (over1N.signum() > 0) { partyData.put(partyId + "over_1N", over1N); }
                if (over2N.signum() > 0) { partyData.put(partyId + "over_2N", over2N); }
                if (over3N.signum() > 0) { partyData.put(partyId + "over_3N", over3N); }
                if (over4N.signum() > 0) { partyData.put(partyId + "over_4N", over4N); }

                // print is past due if any of the over brackets have some amount
                partyData.put(partyId + "is_past_due", (over1N.add(over2N).add(over3N).add(over4N).signum() > 0 ? Boolean.TRUE : Boolean.FALSE));

                // append the sorted list of statement lines to our report
                report.addAll(partyReport.values());
            }
        }
        return report;
    }

    /**
     *  Function to generate the data for a payment row (note that field names have to be identical
     *  to keep things simple).
     */
    // TODO: create and use a Payment domain class object
    private static Map<String, Object> createPaymentRow(GenericValue payment, String partyId) {
        Map<String, Object> row = FastMap.newInstance();
        row.put("type", "PMT");
        row.put("invoice_id", payment.get("invoiceId"));
        row.put("party_id", partyId);
        row.put("transaction_date", payment.get("effectiveDate"));
        row.put("invoice_total", payment.getBigDecimal("amountApplied"));
        return row;
    }

    /**
     * Function to generate the data for an invoice row.
     * @throws RepositoryException
     */
    private static Map<String, Object> createInvoiceRow(Invoice invoice, boolean open, String partyId, boolean useAgingDate) throws RepositoryException {
        Map<String, Object> row = FastMap.newInstance();
        row.put("type", "INV");
        row.put("invoice_id", invoice.getInvoiceId());
        row.put("party_id", partyId);
        row.put("transaction_date", invoice.getInvoiceDate());
        Timestamp dueDate = invoice.getDueDate();
        if (dueDate != null) { row.put("due_date", dueDate); }
        // TODO: this isn't robust in the case of closed invoices because the payments we list might not completely cover a very old invoice paid bit by bit
        row.put("invoice_total", invoice.getInvoiceTotal());
        if (open) {
            row.put("invoice_id_2", invoice.getInvoiceId()); // only print open invoice ID on the receipt part
            row.put("open_amount", invoice.getOpenAmount());
            // only show age date if it's greater than 0
            Integer ageDate = useAgingDate ? invoice.getDaysAged() : invoice.getDaysOutstanding();
            if (ageDate > 0) {
                row.put("age_date", ageDate);
            }
        }
        return row;
    }

}
