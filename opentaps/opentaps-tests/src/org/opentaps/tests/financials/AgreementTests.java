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

package org.opentaps.tests.financials;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import com.ibm.icu.util.Calendar;

import javolution.util.FastList;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.common.agreement.UtilAgreement;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.tests.OpentapsTestCase;

public class AgreementTests extends OpentapsTestCase {

    public static final String module = AgreementTests.class.getName();

    GenericValue demofinadmin = null;
    GenericValue DemoSalesManager = null;
    GenericValue demowarehouse1 = null;
    GenericValue demopurch1 = null;

    String organizationPartyId = "Company";

    TimeZone timeZone = TimeZone.getDefault();
    Locale locale = Locale.getDefault();

    public class InvoiceAction {
        private List<InvoiceItemAction> items;
        private boolean expectFailure = false;

        @SuppressWarnings("unchecked")
        public InvoiceAction(boolean expectFailure) {
            items = FastList.newInstance();
            this.expectFailure = expectFailure;
        }

        public List<InvoiceItemAction> getItems() {
            return items;
        }

        public void addItem(InvoiceItemAction item) {
            items.add(item);
        }

        public boolean hasExpectFailure() {
            return expectFailure;
        }
    }

    public class InvoiceItemAction {
        private String invoiceItemTypeId;
        private String productId;
        private Double quantity;
        private Double amount;

        public InvoiceItemAction(String invoiceItemTypeId, String productId, Double quantity, Double amount) {
            this.invoiceItemTypeId = invoiceItemTypeId;
            this.quantity = quantity;
            this.amount = amount;
            this.productId = productId;
        }

        public String getInvoiceItemTypeId() {
            return invoiceItemTypeId;
        }

        public String getProductId() {
            return productId;
        }

        public Double getQuantity() {
            return quantity;
        }

        public Double getAmount() {
            return amount;
        }

    }

    public class PaymentAction {
        private Double amount;
        private boolean inAdvance = false;

        public PaymentAction(Double amount) {
            this.amount =  amount;
        }

        public PaymentAction(Double amount, boolean inAdvance) {
            this.amount =  amount;
            this.inAdvance = inAdvance;
        }

        public Double getAmount() {
            return amount;
        }

        public boolean hasInAdvance() {
            return inAdvance;
        }

    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        demofinadmin        = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "demofinadmin"));
        DemoSalesManager    = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesManager"));
        demowarehouse1      = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "demowarehouse1"));
        demopurch1          = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "demopurch1"));
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        demofinadmin        = null;
        DemoSalesManager    = null;
        demowarehouse1      = null;
        demopurch1          = null;
    }

    public void testTermTypeFields() {
        assertTrue("Fields for FIN_PAYMENT_TERM are correct", UtilCommon.isEquivalent(UtilMisc.toList("termDays"), UtilAgreement.getValidFields("FIN_PAYMENT_TERM", delegator)));
        assertTrue("Fields for PROD_CAT_COMMISSION are correct", UtilCommon.isEquivalent(UtilMisc.toList("termValue", "productCategoryId", "description", "minQuantity", "maxQuantity"), UtilAgreement.getValidFields("PROD_CAT_COMMISSION", delegator)));
        assertTrue("Fields for PARTNER_SVC_PROD are correct", UtilCommon.isEquivalent(UtilMisc.toList("productId", "valueEnumId"), UtilAgreement.getValidFields("PARTNER_SVC_PROD", delegator)));
    }

    /**
     * This test checks if a party classification group plus net days agreement is working or not.
     * @throws GeneralException if an error occurs
     */
    public void testSalesAgreementNetDaysToGroup() throws GeneralException {

        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);

        // create sales invoice to democlass2
        String invoiceId = fa.createInvoice("democlass2", "SALES_INVOICE");

        // verify that due date on the new invoice is at the end (ie, 23:59:59) of 30 days from today
        GenericValue invoice = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", invoiceId));
        Calendar invoiceDate = UtilDateTime.toCalendar(invoice.getTimestamp("invoiceDate"), timeZone, locale);
        Calendar dueDate = UtilDateTime.toCalendar(invoice.getTimestamp("dueDate"));
        Calendar expectedDate = (Calendar) invoiceDate.clone();
        expectedDate.add(Calendar.DATE, 30);
        expectedDate.set(Calendar.HOUR_OF_DAY, 23);
        expectedDate.set(Calendar.MINUTE, 59);
        expectedDate.set(Calendar.SECOND, 59);
        expectedDate.set(Calendar.MILLISECOND, 999);
        assertDatesEqual(String.format("Invoice %1$s: Due Date is not set at the end of 30 days from today (ie, 23:59:59).", invoiceId), expectedDate, dueDate);
    }

    /**
     * This test checks if a sales agreement for a day of the month due date is correct or not.
     * @throws GeneralException if an error occurs
     */
    public void testSalesAgreementOnDayOfMonthToGroup() throws GeneralException {

        // set AgreementTerm.minQuantity=31 for agreementTermId=AGRTRM_DUE_10TH so that it is always greater than today
        Map<String, Object> callCtxt = new HashMap<String, Object>();
        callCtxt.put("userLogin", demofinadmin);
        callCtxt.put("agreementTermId", "AGRTRM_DUE_10TH");
        callCtxt.put("minQuantity", new Double(31.0));
        runAndAssertServiceSuccess("updateAgreementTerm", callCtxt);

        // create sales invoice for democlass1
        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        String invoiceId = fa.createInvoice("democlass1", "SALES_INVOICE");

        // verify that the sales invoice's due date is at the end of the 10th day of the next month, ie if today is 2007-12-05, the invoice is due 2008-01-10
        GenericValue invoice = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", invoiceId));
        Calendar invoiceDate = UtilDateTime.toCalendar(invoice.getTimestamp("invoiceDate"));
        Calendar dueDate = UtilDateTime.toCalendar(invoice.getTimestamp("dueDate"));
        Calendar expectedDate = (Calendar) invoiceDate.clone();
        expectedDate.add(Calendar.MONTH, 1);
        expectedDate.set(Calendar.DAY_OF_MONTH, 10);
        expectedDate.set(Calendar.HOUR_OF_DAY, 23);
        expectedDate.set(Calendar.MINUTE, 59);
        expectedDate.set(Calendar.SECOND, 59);
        expectedDate.set(Calendar.MILLISECOND, 999);
        assertDatesEqual(String.format("Invoice %1$s: Due Date is not set at the end of the 10th day of the next month, ie if today is 2007-12-05, the invoice is due 2008-01-10", invoiceId), expectedDate, dueDate);

        // set AgreementTerm.minQuantity=0 for agreementTermId=AGRTRM_DUE_10TH so that it is always greater than today
        callCtxt = new HashMap<String, Object>();
        callCtxt.put("userLogin", demofinadmin);
        callCtxt.put("agreementTermId", "AGRTRM_DUE_10TH");
        callCtxt.put("minQuantity", new Double(0.0));
        runAndAssertServiceSuccess("updateAgreementTerm", callCtxt);

        // create sales invoice for democlass1
        invoiceId = fa.createInvoice("democlass1", "SALES_INVOICE");

        // verify that the sales invoice's due date is at the end of the 10th day of the two months later, ie if today is 2007-12-05, the invoice is due 2008-02-10
        invoice = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", invoiceId));
        invoiceDate = UtilDateTime.toCalendar(invoice.getTimestamp("invoiceDate"));
        dueDate = UtilDateTime.toCalendar(invoice.getTimestamp("dueDate"));
        expectedDate = (Calendar) invoiceDate.clone();
        expectedDate.add(Calendar.MONTH, 2);
        expectedDate.set(Calendar.DAY_OF_MONTH, 10);
        expectedDate.set(Calendar.HOUR_OF_DAY, 23);
        expectedDate.set(Calendar.MINUTE, 59);
        expectedDate.set(Calendar.SECOND, 59);
        expectedDate.set(Calendar.MILLISECOND, 999);
        assertDatesEqual(String.format("Invoice %1$s: Due Date is not set at the end of the 10th day of the two months later, ie if today is 2007-12-05, the invoice is due 2008-02-10", invoiceId), expectedDate, dueDate);

    }

    /**
     * This test verifies if a sales agreement for a particular party is working.
     * @throws GeneralException if an error occurs
     */
    public void testSalesAgreementNetDaysToParty() throws GeneralException {
        performSalesAgreementNetDaysToPartyTest("democlass3", 60);
    }

    /**
     * This test verifies that purchasing agreements work.
     * @throws GeneralException if an error occurs
     */
    public void testPurchaseAgreementNetDaysAndTermsToParty() throws GeneralException {

        // create purchase invoice from DemoSupplier to Company
        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        String invoiceId = fa.createInvoice("DemoSupplier", "PURCHASE_INVOICE");

        // verify that the invoice due date is at the end of 30 days from today
        GenericValue invoice = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", invoiceId));
        assertNotNull(invoice.get("dueDate"));
        Calendar invoiceDate = UtilDateTime.toCalendar(invoice.getTimestamp("invoiceDate"));
        Calendar dueDate = UtilDateTime.toCalendar(invoice.getTimestamp("dueDate"));
        invoiceDate.add(Calendar.DATE, 30);
        assertEquals("Difference between invoiceDate and dueDate is 30 days.", invoiceDate.get(Calendar.DATE), dueDate.get(Calendar.DATE));

        // verify that the invoice has invoiceTerm.termTypeId=PURCH_VENDOR_ID and PURCH_FREIGHT whose text value equals those of AgreementTerm id "1003" and "1004"
        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                                           EntityCondition.makeCondition("invoiceId", EntityOperator.EQUALS, invoiceId),
                                           EntityCondition.makeCondition("termTypeId", EntityOperator.IN, Arrays.asList("PURCH_VENDOR_ID", "PURCH_FREIGHT")));
        List<GenericValue> invoiceTerms = delegator.findByCondition("InvoiceTerm", conditions, null, null);
        String agreementTermId = null;
        for (GenericValue term : invoiceTerms) {
            agreementTermId = "PURCH_VENDOR_ID".equals(term.getString("termTypeId")) ? "1003" : "PURCH_FREIGHT".equals(term.getString("termTypeId")) ? "1004" : null;
            if (UtilValidate.isEmpty(agreementTermId)) {
                break;
            }

            GenericValue agreementTerm = delegator.findByPrimaryKey("AgreementTerm", UtilMisc.toMap("agreementTermId", agreementTermId));
            String agreementTextValue = agreementTerm.getString("textValue");
            String invoiceTextValue = term.getString("textValue");
            assertEquals(String.format("Text values of agreement term (\"%1$s\") and corresponding invoice term (\"%2$s\") aren't equals.", agreementTextValue, invoiceTextValue), agreementTextValue, invoiceTextValue);
        }
        assertEquals(String.format("Error. Both PURCH_VENDOR_ID and PURCH_FREIGHT terms should be assigned to invoice %1$s.", invoiceId), true, UtilValidate.isNotEmpty(agreementTermId));
    }

    /**
     * This test checks the following:
     * 1.  A credit limit is enforced so that customer invoices cannot be made READY above it
     * 2.  By making a payment and reducing the limit, the customer invoice can be made READY again
     * 3.  Receiving a customer payment in advance will cause the invoice to pass successfully as well
     * @throws GeneralException if an error occurs
     */
    public void testCreditLimit() throws GeneralException {
        performCreditLimitTest("accountlimit100");
    }

    /**
     * The method performs credit limit test. Actually it creates invoices and payments in sequence
     * specified by actions argument.
     *
     * <strong>Important note!</strong> Payments are always applied to last invoice.
     *
     * @param partyId <code>partyIdTo</code> for invoices created.
     * @throws GeneralException if an error occurs
     * @see org.opentaps.tests.financials.AgreementTests.InvoiceAction
     * @see org.opentaps.tests.financials.AgreementTests.InvoiceItemAction
     * @see org.opentaps.tests.financials.AgreementTests.PaymentAction
     */
    public void performCreditLimitTest(String partyId) throws GeneralException {

        // create sales invoice from Company to specified party with one invoice item for $50
        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        String invoiceId1 = fa.createInvoice(partyId, "SALES_INVOICE");
        fa.createInvoiceItem(invoiceId1, "INV_FPROD_ITEM", "WG-1111", new BigDecimal("1.0"), new BigDecimal("50.0"));

        // set invoice status to READY is successful
        fa.updateInvoiceStatus(invoiceId1, "INVOICE_READY");

        // create a second sales invoice from Company to accountlimit100 with one invoice item for $49
        String invoiceId2 = fa.createInvoice(partyId, "SALES_INVOICE");
        fa.createInvoiceItem(invoiceId2, "INV_FPROD_ITEM", "WG-1111", new BigDecimal("1.0"), new BigDecimal("49.0"));

        // set invoice status to READY is successful
        fa.updateInvoiceStatus(invoiceId2, "INVOICE_READY");

        // create a third sales invoice from Company to accountlimit100 with one invoice item for $5
        String invoiceId3 = fa.createInvoice(partyId, "SALES_INVOICE");
        fa.createInvoiceItem(invoiceId3, "INV_FPROD_ITEM", "WG-1111", new BigDecimal("1.0"), new BigDecimal("5.0"));

        // set invoice status to READY should fail.  (The message is "Cannot mark invoice as ready. Customer credit limit exceeded.")
        Map<String, Object> callCtxt = new HashMap<String, Object>();
        callCtxt.put("userLogin", demofinadmin);
        callCtxt.put("invoiceId", invoiceId3);
        callCtxt.put("statusId", "INVOICE_READY");
        callCtxt.put("statusDate", UtilDateTime.nowTimestamp());
        runAndAssertServiceError("setInvoiceStatus", callCtxt);

        // create payment of type CUSTOMER_PAYMENT from accountlimit100 to Company of $5 and apply to the first sales invoice, set its status to RECEIVED
        fa.createPaymentAndApplication(new BigDecimal("5.0"), partyId, organizationPartyId, "CUSTOMER_PAYMENT", "CREDIT_CARD", null, invoiceId1, "PMNT_RECEIVED");

        // set the third invoice status to READY again and this time it should succeed because the payment has brought the balance down
        fa.updateInvoiceStatus(invoiceId3, "INVOICE_READY");

        // receive a payment of type CUSTOMER_DEPOSIT from accountlimit100 to Company of $100 and set its status to RECEIVED, but do not apply it to any invoice
        fa.createPayment(new BigDecimal("100.0"), partyId, "CUSTOMER_DEPOSIT", "CREDIT_CARD", "PMNT_RECEIVED");

        // create a fourth sales invoice to accountlimit100 with one item for $100
        String invoiceId4 = fa.createInvoice(partyId, "SALES_INVOICE");
        fa.createInvoiceItem(invoiceId4, "INV_FPROD_ITEM", "WG-1111", new BigDecimal("1.0"), new BigDecimal("100.0"));

        // set it to READY and it should succeed because there is a payment already
        fa.updateInvoiceStatus(invoiceId4, "INVOICE_READY");
    }

    /**
     * This method is implementation of the test to verify if a sales agreement for a particular
     * party and net payments days term is working.
     *
     * @param partyId <code>partyIdTo</code> for invoices created.
     * @param netPaymentDays Net payments days term value.
     * @throws GeneralException if an error occurs
     */
    public void performSalesAgreementNetDaysToPartyTest(String partyId, int netPaymentDays) throws GeneralException {

        // create sales invoice for specified partyId
        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        String invoiceId = fa.createInvoice(partyId, "SALES_INVOICE");

        // verify that the invoice due date is the invoice date plus the specified netPaymentDays
        GenericValue invoice = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", invoiceId));
        Calendar invoiceDate = UtilDateTime.toCalendar(invoice.getTimestamp("invoiceDate"));
        Calendar dueDate = UtilDateTime.toCalendar(invoice.getTimestamp("dueDate"));
        invoiceDate.add(Calendar.DATE, netPaymentDays);
        assertEquals("Difference between invoiceDate and dueDate isn't equals " + Integer.valueOf(netPaymentDays).toString() + " days.", invoiceDate.get(Calendar.DATE), dueDate.get(Calendar.DATE));

    }

}
