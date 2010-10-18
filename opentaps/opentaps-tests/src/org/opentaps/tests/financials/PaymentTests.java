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
import java.sql.Timestamp;
import java.util.Map;

import com.opensourcestrategies.financials.accounts.AccountsHelper;
import com.opensourcestrategies.financials.util.UtilFinancial;
import javolution.util.FastMap;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericValue;
import org.opentaps.base.entities.PaymentMethod;
import org.opentaps.base.services.CreatePaymentApplicationService;
import org.opentaps.base.services.UpdatePaymentService;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.domain.billing.payment.Payment;
import org.opentaps.domain.billing.payment.PaymentRepositoryInterface;
import org.opentaps.domain.organization.Organization;
import org.opentaps.domain.organization.OrganizationRepositoryInterface;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.tests.analytics.tests.TestObjectGenerator;

/**
 * Unit tests for Payments and PaymentApplications.
 */
public class PaymentTests extends FinancialsTestCase {

    /**
     * Tests basic Payment class methods for a customer payment.
     * @exception GeneralException if an error occurs
     */
    public void testCustomerPaymentMethods() throws GeneralException {
        PaymentRepositoryInterface repository = billingDomain.getPaymentRepository();
        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        // create a new customer party
        String customerPartyId = createPartyFromTemplate("DemoCustomer", "Cash", "Payer");
        // create a CUSTOMER_PAYMENT from any party to Company
        BigDecimal amount = new BigDecimal("1.23");
        String paymentId = fa.createPayment(amount, customerPartyId, "CUSTOMER_PAYMENT", "CASH");
        Payment payment = repository.getPaymentById(paymentId);

        // Check the Payment.isDisbursement() is false
        assertFalse("Payment with ID [" + paymentId + "] isDisbursement should be false.", payment.isDisbursement());

        // Check the Payment.isReceipt() is true
        assertTrue("Payment with ID [" + paymentId + "] isReceipt should be true.", payment.isReceipt());

        // Check the Payment.getOrganizationPartyId() is Company
        assertEquals("Payment.getOrganizationPartyId() should be " + organizationPartyId, organizationPartyId, payment.getOrganizationPartyId());
        // Check the Payment.getTransactionPartyId() is the other party
        assertEquals("Payment.getTransactionPartyId() should be " + customerPartyId, customerPartyId, payment.getTransactionPartyId());

    }

    /**
     * Tests basic Payment class methods for a vendor payment.
     * @exception GeneralException if an error occurs
     */
    public void testVendorPaymentMethods() throws GeneralException {
        PaymentRepositoryInterface repository = billingDomain.getPaymentRepository();
        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        // create a new customer party
        String supplierPartyId = createPartyFromTemplate("DemoSupplier", "COCHECKING", "Supplier");
        // create a VENDOR_PAYMENT to any party from Company
        BigDecimal amount = new BigDecimal("9.87");
        String paymentId = fa.createPaymentAndApplication(amount, organizationPartyId, supplierPartyId, "VENDOR_PAYMENT", null, "COCHECKING", null, null);
        Payment payment = repository.getPaymentById(paymentId);

        // Payment.isDisbursement() is true
        assertTrue("Payment with ID [" + paymentId + "] isDisbursement should be true.", payment.isDisbursement());

        // Check the Payment.isReceipt() is false
        assertFalse("Payment with ID [" + paymentId + "] isReceipt should be true.", payment.isReceipt());

        // Check the Payment.getOrganizationPartyId() is Company
        assertEquals("Payment.getOrganizationPartyId() should be " + organizationPartyId, organizationPartyId, payment.getOrganizationPartyId());
        // Check the Payment.getTransactionPartyId() is the other party
        assertEquals("Payment.getTransactionPartyId() should be " + supplierPartyId, supplierPartyId, payment.getTransactionPartyId());

    }

    /**
     * Tests GL accounts for customer payment with cash.
     * Create payment of type CUSTOMER_PAYMENT from any party to Company with paymentMethodType = CASH for $1.23
     * Set payment to RECEIVED
     * Verify that UNDEPOSITED_RECEIPTS +1.23, ACCOUNTS_RECEIVABLE -1.23
     * Verify that the A/R balance for customer is -1.23
     * @exception GeneralException if an error occurs
     */
    public void testCustomerPaymentWithCash() throws GeneralException {
        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);

        // create a new customer party
        String partyId = createPartyFromTemplate("DemoCustomer", "Cash", "Payer");

        // store initial balances
        Map<String, Number> beforeBalances = fa.getFinancialBalances(UtilDateTime.nowTimestamp());

        // Create payment of type CUSTOMER_PAYMENT from any party to Company with paymentMethodType = CASH for $1.23
        BigDecimal amount = new BigDecimal("1.23");
        String paymentId = fa.createPayment(amount, partyId, "CUSTOMER_PAYMENT", "CASH");

        // Set payment to RECEIVED
        fa.updatePaymentStatus(paymentId, "PMNT_RECEIVED");

        // Verify that UNDEPOSITED_RECEIPTS +1.23, ACCOUNTS_RECEIVABLE -1.23
        Map<String, Number> afterBalances = fa.getFinancialBalances(UtilDateTime.nowTimestamp());

        assertMapDifferenceCorrect(
                beforeBalances,
                afterBalances,
                UtilMisc.toMap(
                        UtilFinancial.getOrgGlAccountId(organizationPartyId, "UNDEPOSITED_RECEIPTS", delegator), amount,
                        UtilFinancial.getOrgGlAccountId(organizationPartyId, "ACCOUNTS_RECEIVABLE", delegator), amount.negate()
                )
        );

        // verify that we get a negative accounts receivable balance for this customer
        BigDecimal customerBalance = AccountsHelper.getBalanceForCustomerPartyId(partyId, organizationPartyId, "ACTUAL", UtilDateTime.nowTimestamp(), delegator);
        assertEquals("Accounts receivable balance for [" + partyId + "] is correct", customerBalance.setScale(DECIMALS, ROUNDING), amount.negate().setScale(DECIMALS, ROUNDING));

        // Set payment to CONFIRMED
        fa.updatePaymentStatus(paymentId, "PMNT_CONFIRMED");
    }

    /**
     * Tests GL accounts for customer deposit with personal check.
     * Create payment of type CUSTOMER_DEPOSIT from any party to Company with paymentMethodType = PERSONAL_CHECK for $4.56
     * Set payment to RECEIVED
     * Verify that UNDEPOSITED_RECEIPTS +4.56, CUSTOMER_DEPOSIT -4.56
     * @exception GeneralException if an error occurs
     */
    public void testCustomerDepositWithPersonalCheck() throws GeneralException {
        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);

        // store initial balances
        Map<String, Number> beforeBalances = fa.getFinancialBalances(UtilDateTime.nowTimestamp());

        // Create payment of type CUSTOMER_DEPOSIT from any party to Company with paymentMethodType = PERSONAL_CHECK for $4.56
        BigDecimal amount = new BigDecimal("4.56");
        String paymentId = fa.createPayment(amount, "DemoCustomer", "CUSTOMER_DEPOSIT", "PERSONAL_CHECK");

        // Set payment to RECEIVED
        fa.updatePaymentStatus(paymentId, "PMNT_RECEIVED");

        // Verify that UNDEPOSITED_RECEIPTS +4.56, CUSTOMER_DEPOSIT -4.56
        Map<String, Number> afterBalances = fa.getFinancialBalances(UtilDateTime.nowTimestamp());

        assertMapDifferenceCorrect(
                beforeBalances,
                afterBalances,
                UtilMisc.toMap(
                        UtilFinancial.getOrgGlAccountId(organizationPartyId, "UNDEPOSITED_RECEIPTS", delegator), amount,
                        UtilFinancial.getOrgGlAccountId(organizationPartyId, "CUSTOMER_DEPOSIT", delegator), amount.negate()
                )
        );

    }

    /**
     * Tests GL accounts for customer deposit with credit card.
     * Create a new customer
     * Create a credit card of type AmericanExpress
     * Create payment of type CUSTOMER_PAYMENT from any party to Company with paymentMethod = credit card from above for $7.89
     * Set payment to RECEIVED
     * Find CreditCardTypeGlAccount's glAccountId for AmericanExpress
     * Verify that AmericanExpress glAccountId +7.89, ACCOUNTS_RECEIVABLE -7.89
     * @exception GeneralException if an error occurs
     */
    public void testCustomerDepositWithCreditCard() throws GeneralException {
        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);

        // Create a new customer
        TestObjectGenerator generator = new TestObjectGenerator(delegator, dispatcher);
        String customerId = generator.getContacts(1).get(0);

        //Create a credit card of type AmericanExpress
        Map<String, Object> ctxt = FastMap.newInstance();
        ctxt.put("userLogin", demofinadmin);
        ctxt.put("cardNumber", "340000000000009");
        ctxt.put("cardType", "AmericanExpress");
        ctxt.put("expireDate", "02/2011");
        ctxt.put("firstNameOnCard", "For");
        ctxt.put("lastNameOnCard", "Test");
        ctxt.put("partyId", customerId);
        Map<String, Object> results = runAndAssertServiceSuccess("createCreditCard", ctxt);
        String paymentMethodId = (String) results.get("paymentMethodId");

        // store initial balances
        Map<String, Number> beforeBalances = fa.getFinancialBalances(UtilDateTime.nowTimestamp());

        //Create payment of type CUSTOMER_PAYMENT from any party to Company with paymentMethod = credit card from above for $7.89
        BigDecimal amount = new BigDecimal("7.89");
        String paymentId = fa.createPaymentAndApplication(amount, customerId, organizationPartyId, "CUSTOMER_PAYMENT", null, paymentMethodId, null, null);

        // Set payment to RECEIVED
        fa.updatePaymentStatus(paymentId, "PMNT_RECEIVED");

        // Find CreditCardTypeGlAccount's glAccountId for AmericanExpress
        GenericValue crediCardGlAccount = delegator.findByPrimaryKey("CreditCardTypeGlAccount", UtilMisc.toMap("cardType", "AmericanExpress", "organizationPartyId", organizationPartyId));

        // Verify that AmericanExpress glAccountId +7.89, ACCOUNTS_RECEIVABLE -7.89
        Map<String, Number> afterBalances = fa.getFinancialBalances(UtilDateTime.nowTimestamp());

        assertMapDifferenceCorrect(
                beforeBalances,
                afterBalances,
                UtilMisc.toMap(
                        crediCardGlAccount.getString("glAccountId"), amount,
                        UtilFinancial.getOrgGlAccountId(organizationPartyId, "ACCOUNTS_RECEIVABLE", delegator), amount.negate()
                )
        );

    }

    /**
     * Tests that the default payment method of Company is COCHECKING.
     * @exception GeneralException if an error occurs
     */
    public void testDefaultPaymentMethod() throws GeneralException {
        DomainsLoader dl = new DomainsLoader(new Infrastructure(dispatcher), new User(demofinadmin));
        OrganizationRepositoryInterface organizationRepository = dl.loadDomainsDirectory().getOrganizationDomain().getOrganizationRepository();
        Organization organization = organizationRepository.getOrganizationById(organizationPartyId);
        PaymentMethod defaultPaymentMethod = organization.getDefaultPaymentMethod();
        assertNotNull("Could not find the default payment method", defaultPaymentMethod);
        assertEquals("Unexpected default payment method", "COCHECKING", defaultPaymentMethod.getPaymentMethodId());
    }

    /**
     * Tests GL accounts for a vendor payment with a checking account.
     * Create payment of type VENDOR_PAYMENT from Company to DemoSupplier with paymentMethod = COCHECKING for $9.87
     * Set payment to SENT
     * Find PaymentMethod.glAccountId for COCHECKING
     * Verify that COCHECKING glAccountId -9.87, ACCOUNTS_PAYABLE +9.87
     * @exception GeneralException if an error occurs
     */
    public void testVendorPaymentWithCheckingAccount() throws GeneralException {
        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);

        // store initial balances
        Map<String, Number> beforeBalances = fa.getFinancialBalances(UtilDateTime.nowTimestamp());

        // Create payment of type VENDOR_PAYMENT from Company to DemoSupplier with paymentMethod = COCHECKING for $9.87
        BigDecimal amount = new BigDecimal("9.87");
        String paymentId = fa.createPaymentAndApplication(amount, organizationPartyId, "DemoSupplier", "VENDOR_PAYMENT", null, "COCHECKING", null, null);

        // Set payment to SENT
        fa.updatePaymentStatus(paymentId, "PMNT_SENT");

        // Find PaymentMethod.glAccountId for COCHECKING
        GenericValue paymentMethod = delegator.findByPrimaryKey("PaymentMethod", UtilMisc.toMap("paymentMethodId", "COCHECKING"));

        // Verify that COCHECKING glAccountId -9.87, ACCOUNTS_PAYABLE +9.87
        // NOTE: getFinancialBalances multiplies Liabilities and Equity account balances by -1, hence we need to verify Accounts Payable has increased by +9.87
        Map<String, Number> afterBalances = fa.getFinancialBalances(UtilDateTime.nowTimestamp());

        assertMapDifferenceCorrect(
                beforeBalances,
                afterBalances,
                UtilMisc.toMap(
                        paymentMethod.getString("glAccountId"), amount.negate(),
                        UtilFinancial.getOrgGlAccountId(organizationPartyId, "ACCOUNTS_PAYABLE", delegator), amount
                )
        );

    }

    /**
     * Tests GL accounts for a vendor prepayment with a credit card.
     * Create payment of type VENDOR_PREPAY from Company to DemoSupplier with paymentMethod = COAMEX for $6.54
     * Set payment to SENT
     * Find PaymentMethod.glAccountId for COAMEX
     * Verify that COAMEX glAccountId -6.54, PREPAID_EXPENSES +6.54
     * @exception GeneralException if an error occurs
     */
    public void testVendorPrepaymentWithCreditCard() throws GeneralException {
        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);

        // store initial balances
        Map<String, Number> beforeBalances = fa.getFinancialBalances(UtilDateTime.nowTimestamp());

        // Create payment of type VENDOR_PREPAY from Company to DemoSupplier with paymentMethod = COAMEX for $6.54
        BigDecimal amount = new BigDecimal("6.54");
        String paymentId = fa.createPaymentAndApplication(amount, organizationPartyId, "DemoSupplier", "VENDOR_PREPAY", null, "COAMEX", null, null);

        // Set payment to SENT
        fa.updatePaymentStatus(paymentId, "PMNT_SENT");

        // Find PaymentMethod.glAccountId for COAMEX
        GenericValue paymentMethod = delegator.findByPrimaryKey("PaymentMethod", UtilMisc.toMap("paymentMethodId", "COAMEX"));

        // Verify that COAMEX glAccountId -6.54, PREPAID_EXPENSES +6.54
        Map<String, Number> afterBalances = fa.getFinancialBalances(UtilDateTime.nowTimestamp());

        assertMapDifferenceCorrect(
                beforeBalances,
                afterBalances,
                UtilMisc.toMap(
                        paymentMethod.getString("glAccountId"), amount.negate(),
                        UtilFinancial.getOrgGlAccountId(organizationPartyId, "PREPAID_EXPENSES", delegator), amount
                )
        );

    }

    /**
     * Tests sales tax payment.
     * Create payment of type SALES_TAX_PAYMENT from Company to CA_BOE with paymentMethod = COMMKT for $3.21
     * Create PaymentApplication with taxAuthGeoId=CA for this payment for the amount of 3.21
     * Set payment to SENT
     * Find the TaxAuthorityGlAccount.glAccountId for Company, CA_BOE, CA
     * Find PaymentMethod.glAccountId for COMMKT
     * Verify that TaxAuthorityGlAccount.glAccountId -3.21, COMMKT.glAccountId -3.21
     * @exception GeneralException if an error occurs
     */
    public void testSalesTaxPayment() throws GeneralException {
        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);

        // store initial balances
        Map<String, Number> beforeBalances = fa.getFinancialBalances(UtilDateTime.nowTimestamp());

        // Create payment of type SALES_TAX_PAYMENT from Company to CA_BOE with paymentMethod = COMMKT for $3.21
        BigDecimal amount = new BigDecimal("3.21");
        String paymentId = fa.createPaymentAndApplication(amount, organizationPartyId, "CA_BOE", "SALES_TAX_PAYMENT", null, "COMMKT", null, null);

        // Create PaymentApplication with taxAuthGeoId=CA for this payment for the amount of 3.21
        Map<String, Object> ctxt = FastMap.newInstance();
        ctxt.put("userLogin", demofinadmin);
        ctxt.put("paymentId", paymentId);
        ctxt.put("amountApplied", amount);
        ctxt.put("taxAuthGeoId", "CA");
        runAndAssertServiceSuccess("createPaymentApplication", ctxt);


        // Set payment to SENT
        fa.updatePaymentStatus(paymentId, "PMNT_SENT");

        // Find the TaxAuthorityGlAccount.glAccountId for Company, CA_BOE, CA
        GenericValue taxAuthGlAccount = delegator.findByPrimaryKey(
                "TaxAuthorityGlAccount",
                UtilMisc.toMap(
                        "taxAuthGeoId", "CA",
                        "taxAuthPartyId", "CA_BOE",
                        "organizationPartyId", organizationPartyId
                )
        );

        // Find PaymentMethod.glAccountId for COMMKT
        GenericValue paymentMethod = delegator.findByPrimaryKey(
                "PaymentMethod",
                UtilMisc.toMap("paymentMethodId", "COMMKT")
        );

        // Verify that TaxAuthorityGlAccount.glAccountId +3.21, COMMKT.glAccountId -3.21
        // Again, TaxAuthorityGlAccount amount is positive because getFinancialBalances multiplies Liability + Equity accounts by -1

        // get balances after payment applied
        Map<String, Number> afterBalances = fa.getFinancialBalances(UtilDateTime.nowTimestamp());

        assertMapDifferenceCorrect(
                beforeBalances,
                afterBalances,
                UtilMisc.toMap(
                        taxAuthGlAccount.getString("glAccountId"), amount,
                        paymentMethod.getString("glAccountId"), amount.negate()
                )
        );

    }

    /**
     * Tests the application of a payment to an invoice.
     * Verifies that it is not possible the apply more than the payment outstanding amount
     *  and not more than the invoice outstanding amount.
     * Verifies that amount <= 0 are rejected when creating or updating applications
     * @exception GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testPaymentApplicationAmountCheck() throws GeneralException {
        FinancialAsserts financialAsserts = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        String customerPartyId = "DemoCustCompany";
        // create sales invoice to DemoCustCompany
        String invoiceId = financialAsserts.createInvoice(customerPartyId, "SALES_INVOICE", UtilDateTime.nowTimestamp(), "Test createPaymentApplication", "testCreatePaymentApplication1", "Test createPaymentApplication");

        // create a an invoice item of total amount 40.0
        financialAsserts.createInvoiceItem(invoiceId, "INV_PROD_ITEM", "GZ-8544", new BigDecimal("4.0"), new BigDecimal("10.0"));

        // set to Ready
        financialAsserts.updateInvoiceStatus(invoiceId, "INVOICE_READY");

        // create a payment of amount 20.0
        String paymentId = financialAsserts.createPayment(new BigDecimal("20.0"), customerPartyId, "CUSTOMER_PAYMENT", "CREDIT_CARD");

        // try to apply 30.0 from this payment
        // this should fail as the payment amount was only 20.0
        // this verifies that we cannot create an application with amountApplied > payment amount
        Map<String, Object> input = UtilMisc.<String, Object>toMap("userLogin", demofinadmin);
        input.put("paymentId", paymentId);
        input.put("invoiceId", invoiceId);
        input.put("amountApplied", new BigDecimal("30.0"));
        input.put("checkForOverApplication", Boolean.TRUE);
        runAndAssertServiceError("createPaymentApplication", input);

        // now apply 10.0, this should succeed
        input.put("amountApplied", new BigDecimal("10.0"));
        Map result = runAndAssertServiceSuccess("createPaymentApplication", input);
        // get the application id
        String paymentApplicationId = (String) result.get("paymentApplicationId");

        // try to update the payment application applied amount to 25.0
        // this should fail as the payment amount was only 20.0
        // this verifies that we cannot update an application with amountApplied > payment amount
        input = UtilMisc.<String, Object>toMap("userLogin", demofinadmin);
        input.put("paymentApplicationId", paymentApplicationId);
        input.put("amountApplied", new BigDecimal("25.0"));
        input.put("checkForOverApplication", Boolean.TRUE);
        runAndAssertServiceError("updatePaymentApplication", input);

        // try to update the payment application applied amount to 0.0
        // this should fail
        // this verifies that we cannot update an application with amountApplied = 0.0
        input.put("amountApplied", new BigDecimal("0.0"));
        runAndAssertServiceError("updatePaymentApplication", input);

        // try to update the payment application applied amount to -1.0
        // this should fail
        // this verifies that we cannot update an application with amountApplied < 0.0
        input.put("amountApplied", new BigDecimal("-1.0"));
        runAndAssertServiceError("updatePaymentApplication", input);

        // try to create another application of the same payment to the same invoice with amount 15.0
        // this should fail as the payment unapplied amount is only 10.0
        // this verifies that we cannot create an application with amountApplied > payment not applied amount
        input = UtilMisc.<String, Object>toMap("userLogin", demofinadmin);
        input.put("paymentId", paymentId);
        input.put("invoiceId", invoiceId);
        input.put("amountApplied", new BigDecimal("15.0"));
        input.put("checkForOverApplication", Boolean.TRUE);
        runAndAssertServiceError("createPaymentApplication", input);

        // try to create another application amount 0.0
        // this should fail
        // this verifies that we cannot create an application with amountApplied = 0.0
        input.put("amountApplied", new BigDecimal("0.0"));
        runAndAssertServiceError("createPaymentApplication", input);

        // try to create another application amount -1.0
        // this should fail
        // this verifies that we cannot create an application with amountApplied < 0.0
        input.put("amountApplied", new BigDecimal("-1.0"));
        runAndAssertServiceError("createPaymentApplication", input);

        // now apply 10.0, this should succeed
        input.put("amountApplied", new BigDecimal("10.0"));
        result = runAndAssertServiceSuccess("createPaymentApplication", input);
        // get the new application id
        paymentApplicationId = (String) result.get("paymentApplicationId");

        // try to update the payment application applied amount to 12.0
        // this should fail as the payment not applied amount is only 10.0
        // this verifies that we cannot update an application with amountApplied > payment not applied amount
        input = UtilMisc.<String, Object>toMap("userLogin", demofinadmin);
        input.put("paymentApplicationId", paymentApplicationId);
        input.put("amountApplied", new BigDecimal("12.0"));
        input.put("checkForOverApplication", Boolean.TRUE);
        runAndAssertServiceError("updatePaymentApplication", input);


        // at this point we have applied 20.0 to the invoice, the payment was fully applied and the invoice
        // outstanding amount is now 20.0


        // create a payment of amount 100.0
        String payment2Id = financialAsserts.createPayment(new BigDecimal("100.0"), customerPartyId, "CUSTOMER_PAYMENT", "CREDIT_CARD");

        // try to apply 30.0 from this payment
        // this should fail as the invoice outstanding amount is only 20.0
        // this verifies that we cannot create an application with amountApplied > invoice outstanding amount
        input = UtilMisc.<String, Object>toMap("userLogin", demofinadmin);
        input.put("paymentId", payment2Id);
        input.put("invoiceId", invoiceId);
        input.put("amountApplied", new BigDecimal("30.0"));
        input.put("checkForOverApplication", Boolean.TRUE);
        runAndAssertServiceError("createPaymentApplication", input);   // fails here

        // now apply 20.0, this should succeed
        input.put("amountApplied", new BigDecimal("20.0"));
        result = runAndAssertServiceSuccess("createPaymentApplication", input);
        // get the application id
        paymentApplicationId = (String) result.get("paymentApplicationId");

        // try to update the payment application applied amount to 22.0
        // this should fail as the invoice outstanding amount is only 20.0
        // this verifies that we cannot update an application with amountApplied > invoice outstanding amount
        input = UtilMisc.<String, Object>toMap("userLogin", demofinadmin);
        input.put("paymentApplicationId", paymentApplicationId);
        input.put("amountApplied", new BigDecimal("22.0"));
        input.put("checkForOverApplication", Boolean.TRUE);
        runAndAssertServiceError("updatePaymentApplication", input);

        // when the payments are received, the invoice should be automatically set to paid
        financialAsserts.updatePaymentStatus(paymentId, "PMNT_RECEIVED");
        financialAsserts.updatePaymentStatus(payment2Id, "PMNT_RECEIVED");
        financialAsserts.assertInvoiceStatus(invoiceId, "INVOICE_PAID");
    }

    /**
     * Tests the payment status changes.
     * Verifies that it is possible to create/update/delete applications for a payment marked RECEIVED or SENT.
     * Verifies that it is possible not possible create/update/delete applications for a payment marked CONFIRMED.
     * @exception GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testPaymentStatusAndApplications() throws GeneralException {
        FinancialAsserts financialAsserts = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        String customerPartyId = "DemoCustCompany";
        // create sales invoice to DemoCustCompany
        String invoiceId = financialAsserts.createInvoice(customerPartyId, "SALES_INVOICE", UtilDateTime.nowTimestamp(), "Test createPaymentApplication", "testCreatePaymentApplication1", "Test createPaymentApplication");

        // create a an invoice item of total amount 40.0
        financialAsserts.createInvoiceItem(invoiceId, "INV_PROD_ITEM", "GZ-8544", new BigDecimal("4.0"), new BigDecimal("10.0"));

        // set to Ready
        financialAsserts.updateInvoiceStatus(invoiceId, "INVOICE_READY");

        // create a payment of amount 20.0, the first status is NOT_PAID
        String paymentId = financialAsserts.createPayment(new BigDecimal("20.0"), customerPartyId, "CUSTOMER_PAYMENT", "CREDIT_CARD");
        financialAsserts.assertPaymentStatus(paymentId, "PMNT_NOT_PAID");

        // apply 10.0 from this payment
        Map<String, Object> input = UtilMisc.<String, Object>toMap("userLogin", demofinadmin);
        input.put("paymentId", paymentId);
        input.put("invoiceId", invoiceId);
        input.put("amountApplied", new BigDecimal("10.0"));
        Map result = runAndAssertServiceSuccess("createPaymentApplication", input);
        // get application id
        String paymentApplicationId = (String) result.get("paymentApplicationId");

        // change payment status to RECEIVED
        financialAsserts.updatePaymentStatus(paymentId, "PMNT_RECEIVED");

        // remove previous payment application
        input = UtilMisc.<String, Object>toMap("userLogin", demofinadmin);
        input.put("paymentApplicationId", paymentApplicationId);
        runAndAssertServiceSuccess("removePaymentApplication", input);

        // create a new application of amount 10.0
        input = UtilMisc.<String, Object>toMap("userLogin", demofinadmin);
        input.put("paymentId", paymentId);
        input.put("invoiceId", invoiceId);
        input.put("amountApplied", new BigDecimal("10.0"));
        result = runAndAssertServiceSuccess("createPaymentApplication", input);
        // get the new application id
        paymentApplicationId = (String) result.get("paymentApplicationId");

        // update the payment application to 20.0
        input = UtilMisc.<String, Object>toMap("userLogin", demofinadmin);
        input.put("paymentApplicationId", paymentApplicationId);
        input.put("paymentId", paymentId);
        input.put("invoiceId", invoiceId);
        input.put("amountApplied", new BigDecimal("20.0"));
        runAndAssertServiceSuccess("updatePaymentApplication", input);

        // verify that we can now mark the payment as CONFIRMED
        financialAsserts.updatePaymentStatus(paymentId, "PMNT_CONFIRMED");

        // try to remove the payment application
        // this should fail as the payment is now CONFIRMED
        input = UtilMisc.<String, Object>toMap("userLogin", demofinadmin);
        input.put("paymentApplicationId", paymentApplicationId);
        runAndAssertServiceError("removePaymentApplication", input);
        // same for an update
        input = UtilMisc.<String, Object>toMap("userLogin", demofinadmin);
        input.put("paymentApplicationId", paymentApplicationId);
        input.put("amountApplied", new BigDecimal("10.0"));
        runAndAssertServiceError("updatePaymentApplication", input);
        // and finally for creating another payment application
        input = UtilMisc.<String, Object>toMap("userLogin", demofinadmin);
        input.put("paymentId", paymentId);
        input.put("invoiceId", invoiceId);
        input.put("amountApplied", new BigDecimal("5.0"));
        input.put("checkForOverApplication", Boolean.TRUE);
        runAndAssertServiceError("createPaymentApplication", input);

        financialAsserts.assertPaymentStatus(paymentId, "PMNT_CONFIRMED");
    }

    /**
     * Tests the invoice status changes.
     * Verifies that an invoice is marked PAID only if fully applied and if all the payments that apply to it are set as RECEIVED or SENT.
     * Verifies that an invoice is properly marked READY when removing or updating a payment application
     * Verifies that an invoice is properly marked PAID when creating or updating a payment application
     * Verifies that the customer balance is unchanged when payment applications are changed but change accordingly when the payment is marked RECEIVED
     * @exception GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testInvoiceStatusAndApplications() throws GeneralException {
        FinancialAsserts financialAsserts = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        String customerPartyId = "DemoCustCompany";
        // create sales invoice to DemoCustCompany
        String invoiceId = financialAsserts.createInvoice(customerPartyId, "SALES_INVOICE", UtilDateTime.nowTimestamp(), "Test createPaymentApplication", "testCreatePaymentApplication1", "Test createPaymentApplication");

        // create a an invoice item of total amount 40.0
        financialAsserts.createInvoiceItem(invoiceId, "INV_PROD_ITEM", "GZ-8544", new BigDecimal("4.0"), new BigDecimal("10.0"));

        // set to Ready
        financialAsserts.updateInvoiceStatus(invoiceId, "INVOICE_READY");

        // get customer balance
        BigDecimal customerBalance1 = AccountsHelper.getBalanceForCustomerPartyId(customerPartyId, organizationPartyId, "ACTUAL",  UtilDateTime.nowTimestamp(), delegator);

        // create a payment of amount 20.0, apply it and mark as RECEIVED
        String paymentId = financialAsserts.createPayment(new BigDecimal("20.0"), customerPartyId, "CUSTOMER_PAYMENT", "CREDIT_CARD");
        Map<String, Object> input = UtilMisc.<String, Object>toMap("userLogin", demofinadmin);
        input.put("paymentId", paymentId);
        input.put("invoiceId", invoiceId);
        input.put("amountApplied", new BigDecimal("20.0"));

        pause("Workaround pause for MySQL duplicate timestamps");
        Map result = runAndAssertServiceSuccess("createPaymentApplication", input);
        String paymentApplicationId = (String) result.get("paymentApplicationId");
        financialAsserts.updatePaymentStatus(paymentId, "PMNT_RECEIVED");

        // customer balance should decrease by 20.0
        BigDecimal customerBalance2 = AccountsHelper.getBalanceForCustomerPartyId(customerPartyId, organizationPartyId, "ACTUAL",  UtilDateTime.nowTimestamp(), delegator);
        assertEquals("Customer Balance for [" + customerPartyId + "] should have decreased by 20.0.", customerBalance2, customerBalance1.subtract(new BigDecimal("20.0")));

        // create a second payment of amount 20.0, apply it
        pause("Workaround pause for MySQL duplicate timestamps");
        paymentId = financialAsserts.createPayment(new BigDecimal("20.0"), customerPartyId, "CUSTOMER_PAYMENT", "CREDIT_CARD");
        input = UtilMisc.<String, Object>toMap("userLogin", demofinadmin);
        input.put("paymentId", paymentId);
        input.put("invoiceId", invoiceId);
        input.put("amountApplied", new BigDecimal("20.0"));
        result = runAndAssertServiceSuccess("createPaymentApplication", input);
        paymentApplicationId = (String) result.get("paymentApplicationId");

        // the invoice status should still be READY as we did not mark the second payment as RECEIVED
        financialAsserts.assertInvoiceStatus(invoiceId, "INVOICE_READY");

        // customer balance should be unchanged
        BigDecimal customerBalance3 = AccountsHelper.getBalanceForCustomerPartyId(customerPartyId, organizationPartyId, "ACTUAL",  UtilDateTime.nowTimestamp(), delegator);
        assertEquals("Customer Balance for [" + customerPartyId + "] should be unchanged.", customerBalance3, customerBalance2);

        // mark the second payment as RECEIVED
        pause("Workaround pause for MySQL duplicate timestamps");
        financialAsserts.updatePaymentStatus(paymentId, "PMNT_RECEIVED");

        // customer balance should decrease by 20.0
        BigDecimal customerBalance4 = AccountsHelper.getBalanceForCustomerPartyId(customerPartyId, organizationPartyId, "ACTUAL",  UtilDateTime.nowTimestamp(), delegator);
        assertEquals("Customer Balance for [" + customerPartyId + "] should have decreased by 20.0.", customerBalance4, customerBalance3.subtract(new BigDecimal("20.0")));

        // the invoice should now be PAID as it is fully paid and all payments are RECEIVED
        financialAsserts.assertInvoiceStatus(invoiceId, "INVOICE_PAID");

        // customer balance should be unchanged
        BigDecimal customerBalance5 = AccountsHelper.getBalanceForCustomerPartyId(customerPartyId, organizationPartyId, "ACTUAL",  UtilDateTime.nowTimestamp(), delegator);
        assertEquals("Customer Balance for [" + customerPartyId + "] should be unchanged.", customerBalance5, customerBalance4);

        // update the payment application by reducing the applied amount
        pause("Workaround pause for MySQL duplicate timestamps");
        input = UtilMisc.<String, Object>toMap("userLogin", demofinadmin);
        input.put("paymentApplicationId", paymentApplicationId);
        input.put("amountApplied", new BigDecimal("18.0"));
        input.put("paymentId", paymentId);
        input.put("invoiceId", invoiceId);
        runAndAssertServiceSuccess("updatePaymentApplication", input);

        // customer balance should still be unchanged
        customerBalance5 = AccountsHelper.getBalanceForCustomerPartyId(customerPartyId, organizationPartyId, "ACTUAL",  UtilDateTime.nowTimestamp(), delegator);
        assertEquals("Customer Balance for [" + customerPartyId + "] should be unchanged.", customerBalance5, customerBalance4);

        // the invoice is not fully paid and should have status READY
        financialAsserts.assertInvoiceStatus(invoiceId, "INVOICE_READY");

        // change back the applied amount to 20.0
        pause("Workaround pause for MySQL duplicate timestamps");
        input = UtilMisc.<String, Object>toMap("userLogin", demofinadmin);
        input.put("paymentApplicationId", paymentApplicationId);
        input.put("amountApplied", new BigDecimal("20.0"));
        input.put("paymentId", paymentId);
        input.put("invoiceId", invoiceId);
        runAndAssertServiceSuccess("updatePaymentApplication", input);

        // customer balance should still be unchanged
        customerBalance5 = AccountsHelper.getBalanceForCustomerPartyId(customerPartyId, organizationPartyId, "ACTUAL",  UtilDateTime.nowTimestamp(), delegator);
        assertEquals("Customer Balance for [" + customerPartyId + "] should be unchanged.", customerBalance5, customerBalance4);

        // the invoice is fully paid and should have status PAID
        financialAsserts.assertInvoiceStatus(invoiceId, "INVOICE_PAID");

        // remove the application
        pause("Workaround pause for MySQL duplicate timestamps");
        input = UtilMisc.<String, Object>toMap("userLogin", demofinadmin);
        input.put("paymentApplicationId", paymentApplicationId);
        pause("Workaround MYSQL Timestamp PK collision");
        runAndAssertServiceSuccess("removePaymentApplication", input);

        // customer balance should still be unchanged
        customerBalance5 = AccountsHelper.getBalanceForCustomerPartyId(customerPartyId, organizationPartyId, "ACTUAL",  UtilDateTime.nowTimestamp(), delegator);
        assertEquals("Customer Balance for [" + customerPartyId + "] should be unchanged.", customerBalance5, customerBalance4);

        // the invoice is not fully paid and should have status READY
        financialAsserts.assertInvoiceStatus(invoiceId, "INVOICE_READY");

        // create new application
        pause("Workaround pause for MySQL duplicate timestamps");
        input = UtilMisc.<String, Object>toMap("userLogin", demofinadmin);
        input.put("paymentId", paymentId);
        input.put("invoiceId", invoiceId);
        input.put("amountApplied", new BigDecimal("20.0"));
        result = runAndAssertServiceSuccess("createPaymentApplication", input);

        // customer balance should still be unchanged
        customerBalance5 = AccountsHelper.getBalanceForCustomerPartyId(customerPartyId, organizationPartyId, "ACTUAL",  UtilDateTime.nowTimestamp(), delegator);
        assertEquals("Customer Balance for [" + customerPartyId + "] should be unchanged.", customerBalance5, customerBalance4);

        // the invoice is fully paid and should have status PAID
        financialAsserts.assertInvoiceStatus(invoiceId, "INVOICE_PAID");


    }

    /**
     * Test the application of customer deposit payments to an invoice.
     * 1.  get GL account balances
     * 2.  use createPartyFromTemplate to create a customer from an existing customer
     * 3.  get initial balance for customer
     * 4.  create a payment of type CUSTOMER_DEPOSIT from customer of payment method type PERSONAL_CHECK for $100, and set the payment to RECEIVED
     * 5.  create a sales invoice for customer of $100
     * 6.  set invoice to READY
     * 7.  apply payment to invoice
     * 8.  verify that the invoice is now PAID
     * 9.  get new balance for customer
     * 10. verify that new balance - initial balance = 0: ie, the balance is unchanged
     * 11. verify the following gl account balance changes: UNDEPOSITED_RECEIPTS +100, ACCOUNTS_RECEIVABLE 0, CUSTOMER_DEPOSIT 0
     * @exception GeneralException if an error occurs
     */
    public void testCustomerDepositAndInvoicing() throws GeneralException {
        // 1. get GL account balances
        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        Map<String, Number> beforeBalances = fa.getFinancialBalances(UtilDateTime.nowTimestamp());

        // 2. use createPartyFromTemplate to create a customer from an existing customer
        String customerPartyId = createPartyFromTemplate("DemoCustomer", "Customer for testCustomerDepositAndInvoicing");

        // 3. get initial balance for customer
        BigDecimal customerBalance1 = AccountsHelper.getBalanceForCustomerPartyId(customerPartyId, organizationPartyId, "ACTUAL",  UtilDateTime.nowTimestamp(), delegator);

        // 4. create a payment of type CUSTOMER_DEPOSIT from customer of payment method type PERSONAL_CHECK for $100, and set the payment to RECEIVED
        BigDecimal amount = new BigDecimal("100.0");
        String paymentId = fa.createPayment(amount, customerPartyId, "CUSTOMER_DEPOSIT", "PERSONAL_CHECK");
        fa.updatePaymentStatus(paymentId, "PMNT_RECEIVED");

        // 5. create a sales invoice for customer of $100
        String invoiceId = fa.createInvoice(customerPartyId, "SALES_INVOICE", UtilDateTime.nowTimestamp(), "testCustomerDepositAndInvoicing");
        fa.createInvoiceItem(invoiceId, "INV_PROD_ITEM", "GZ-8544", new BigDecimal("1.0"), amount);
        // 6. set invoice to READY
        fa.updateInvoiceStatus(invoiceId, "INVOICE_READY");

        // 7. apply payment to invoice
        Map<String, Object> input = UtilMisc.<String, Object>toMap("userLogin", demofinadmin);
        input.put("paymentId", paymentId);
        input.put("invoiceId", invoiceId);
        input.put("amountApplied", amount);
        runAndAssertServiceSuccess("createPaymentApplication", input);

        // 8. verify that the invoice is now PAID
        fa.assertInvoiceStatus(invoiceId, "INVOICE_PAID");

        // 9. get new balance for customer
        BigDecimal customerBalance2 = AccountsHelper.getBalanceForCustomerPartyId(customerPartyId, organizationPartyId, "ACTUAL",  UtilDateTime.nowTimestamp(), delegator);
        // 10. verify that new balance - initial balance = 0: ie, the balance is unchanged
        assertEquals("Customer Balance for [" + customerPartyId + "] should be unchanged.", customerBalance2, customerBalance1);

        // 11. verify the following gl account balance changes: UNDEPOSITED_RECEIPTS +100, ACCOUNTS_RECEIVABLE 0, CUSTOMER_DEPOSIT 0
        Map<String, Number> afterBalances = fa.getFinancialBalances(UtilDateTime.nowTimestamp());

        assertMapDifferenceCorrect(
                beforeBalances,
                afterBalances,
                UtilMisc.toMap(
                           UtilFinancial.getOrgGlAccountId(organizationPartyId, "UNDEPOSITED_RECEIPTS", delegator), amount,
                           UtilFinancial.getOrgGlAccountId(organizationPartyId, "ACCOUNTS_RECEIVABLE", delegator), BigDecimal.ZERO,
                           UtilFinancial.getOrgGlAccountId(organizationPartyId, "CUSTOMER_DEPOSIT", delegator), BigDecimal.ZERO
                       )
        );
    }

    /**
     * Test applying parent account payment to child account invoice and making sure that parent account payments cause
     * child account invoices to be PAID, and the accounts receivable balances are offset corectly.
     * @exception GeneralException if an error occurs
     */
    public void testParentPaymentForSubAccountInvoice() throws GeneralException {

        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);

        // use createPartyFromTemplate to create a account from an existing customer
        String accountPartyId = createPartyFromTemplate("DemoAccount1", "Account for testParentPaymentForSubAccountInvoice" + UtilDateTime.nowTimestamp());
        // use createPartyFromTemplate to create 2 sub accounts from an existing customer
        String subAccountPartyId1 = createPartyFromTemplate("DemoAccount1Sub", "SubAccount 1 for testParentPaymentForSubAccountInvoice" + UtilDateTime.nowTimestamp());
        String subAccountPartyId2 = createPartyFromTemplate("DemoAccount1Sub", "SubAccount 2 for testParentPaymentForSubAccountInvoice" + UtilDateTime.nowTimestamp());

        // set the parentPartyId of the sub accounts.  Note that the use case where the sub account's parent party ID is different than that of the payer is undefined
        // so we don't test it here
        setParentPartyId(subAccountPartyId1, accountPartyId);
        setParentPartyId(subAccountPartyId2, accountPartyId);

        // create sales invoice #1 for $100 from organization to subaccount party. Set the invoice to ready.
        String invoiceId1 = fa.createInvoice(subAccountPartyId1, "SALES_INVOICE", UtilDateTime.nowTimestamp(), "testParentPaymentForSubAccountInvoice");
        fa.createInvoiceItem(invoiceId1, "INV_PROD_ITEM", "GZ-8544", new BigDecimal("2.0"), new BigDecimal("50.0"));
        fa.updateInvoiceStatus(invoiceId1, "INVOICE_READY");

        // create sales invoice #2 for $150 from organization to subaccount party. Set the invoice to ready.
        String invoiceId2 = fa.createInvoice(subAccountPartyId2, "SALES_INVOICE", UtilDateTime.nowTimestamp(), "testParentPaymentForSubAccountInvoice");
        fa.createInvoiceItem(invoiceId2, "INV_PROD_ITEM", "GZ-8544", new BigDecimal("2.0"), new BigDecimal("75.0"));
        fa.updateInvoiceStatus(invoiceId2, "INVOICE_READY");

        // Create a customer payment for $200 from Parent account party to organization.
        String paymentId1 = fa.createPayment(new BigDecimal("200.0"), accountPartyId, "CUSTOMER_PAYMENT", "CREDIT_CARD");

        // apply the payment to the invoice 1 and invoice 2 by creating a PaymentApplication
        Map<String, Object> input = UtilMisc.<String, Object>toMap("userLogin", demofinadmin);
        input.put("paymentId", paymentId1);
        input.put("invoiceId", invoiceId1);
        input.put("amountApplied", new BigDecimal("100.0"));
        input.put("checkForOverApplication", Boolean.TRUE);
        runAndAssertServiceSuccess("createPaymentApplication", input);

        input = UtilMisc.<String, Object>toMap("userLogin", demofinadmin);
        input.put("paymentId", paymentId1);
        input.put("invoiceId", invoiceId2);
        input.put("amountApplied", new BigDecimal("100.0"));
        input.put("checkForOverApplication", Boolean.TRUE);
        runAndAssertServiceSuccess("createPaymentApplication", input);

        // set the payment to received
        fa.updatePaymentStatus(paymentId1, "PMNT_RECEIVED");

        // Verify that the sales invoice #1 is now PAID and sales invoice #2 is still READY
        fa.assertInvoiceStatus(invoiceId1, "INVOICE_PAID");
        fa.assertInvoiceStatus(invoiceId2, "INVOICE_READY");

        // Receive a payment for $50 from sub account 2 and apply it to invoice 2
        String paymentId2 = fa.createPayment(new BigDecimal("50.0"), subAccountPartyId2, "CUSTOMER_PAYMENT", "PERSONAL_CHECK");
        input = UtilMisc.<String, Object>toMap("userLogin", demofinadmin);
        input.put("paymentId", paymentId2);
        input.put("invoiceId", invoiceId2);
        input.put("amountApplied", new BigDecimal("50.0"));
        input.put("checkForOverApplication", Boolean.TRUE);
        runAndAssertServiceSuccess("createPaymentApplication", input);

        // set payment 2 to RECEIVED
        fa.updatePaymentStatus(paymentId2, "PMNT_RECEIVED");

        // check that invoice 2 is PAID
        fa.assertInvoiceStatus(invoiceId2, "INVOICE_PAID");

        // at this point the parent account should have a balance of -200 and each sub account should have a balance of 100
        assertEquals("Parent Account balance is correct", AccountsHelper.getBalanceForCustomerPartyId(accountPartyId, organizationPartyId, "ACTUAL", UtilDateTime.nowTimestamp(), delegator), new BigDecimal(-200.00));
        assertEquals("Sub account 1 balance is correct", AccountsHelper.getBalanceForCustomerPartyId(subAccountPartyId1, organizationPartyId, "ACTUAL", UtilDateTime.nowTimestamp(), delegator), new BigDecimal(100.00));
        assertEquals("Sub account 2 balance is correct", AccountsHelper.getBalanceForCustomerPartyId(subAccountPartyId2, organizationPartyId, "ACTUAL", UtilDateTime.nowTimestamp(), delegator), new BigDecimal(100.00));

        // now set the two payments to CONFIRMED, which should cause the parent/child balances to be netted
        fa.updatePaymentStatus(paymentId1, "PMNT_CONFIRMED");
        fa.updatePaymentStatus(paymentId2, "PMNT_CONFIRMED");

        // accounts receivables balances should all be net to zero now
        assertEquals("Parent Account balance is correct", AccountsHelper.getBalanceForCustomerPartyId(accountPartyId, organizationPartyId, "ACTUAL", UtilDateTime.nowTimestamp(), delegator), new BigDecimal(0.00));
        assertEquals("Sub account 1 balance is correct", AccountsHelper.getBalanceForCustomerPartyId(subAccountPartyId1, organizationPartyId, "ACTUAL", UtilDateTime.nowTimestamp(), delegator), new BigDecimal(0.00));
        assertEquals("Sub account 2 balance is correct", AccountsHelper.getBalanceForCustomerPartyId(subAccountPartyId2, organizationPartyId, "ACTUAL", UtilDateTime.nowTimestamp(), delegator), new BigDecimal(0.00));
    }

    /**
     * This test verifies that the correct GL account postings take place
     * when a payment is split between an override GL account and an invoice.
     * @throws GeneralException if an error occurs
     */
    public void testPaymentWithOverrideGlAccount() throws GeneralException {
        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);

        // this payment will be split between the invoice and an override gl account
        BigDecimal invoiceAmount = new BigDecimal("1000.0");
        BigDecimal overrideAmount = new BigDecimal("1500.0");
        BigDecimal paymentAmount = invoiceAmount.add(overrideAmount);

        // create the invoice
        String pi1 = fa.createInvoice("DemoSupplier", "PURCHASE_INVOICE", UtilDateTime.nowTimestamp(), null, null, null);
        fa.createInvoiceItem(pi1, "PINV_SUPLPRD_ITEM", null, new BigDecimal("1.0"), invoiceAmount, null, null);
        fa.updateInvoiceStatus(pi1, "INVOICE_READY");

        // get the initial balances.  Payment will be split between consumer and gov, and enterprise should be zero throughout
        Timestamp start = UtilDateTime.nowTimestamp();
        Map initialBalances = fa.getFinancialBalances(start);

        //create a vendor payment of $2500 to DemoSupplier
        String paymentId = fa.createPaymentAndApplication(paymentAmount, organizationPartyId, "DemoSupplier", "VENDOR_PAYMENT", "CASH", null, null, null);
        //apply $1000 of payment to PI1 with tags DIV_CONSUMER
        runAndAssertServiceSuccess("createPaymentApplication", UtilMisc.toMap("userLogin", demofinadmin, "paymentId", paymentId, "invoiceId", pi1, "amountApplied", invoiceAmount));

        // apply $1500 to 600000 with tag DIV_GOV
        String overrideGlAccountId = "600000";
        Map<String, Object> ctxt = FastMap.newInstance();
        ctxt.put("userLogin", demofinadmin);
        ctxt.put("paymentId", paymentId);
        ctxt.put("amountApplied", overrideAmount);
        ctxt.put("overrideGlAccountId", overrideGlAccountId);
        runAndAssertServiceSuccess("createPaymentApplication", ctxt);

        // post the payment
        fa.updatePaymentStatus(paymentId, "PMNT_SENT");

        Timestamp finish = UtilDateTime.nowTimestamp();
        Map finalBalances = fa.getFinancialBalances(finish);

        // overall, ACCOUNTS_PAYABLE +1000 and override GL account +1500
        Map expectedChange = UtilFinancial.replaceGlAccountTypeWithGlAccountForOrg(organizationPartyId, UtilMisc.toMap("ACCOUNTS_PAYABLE", invoiceAmount), delegator);
        expectedChange.put(overrideGlAccountId, overrideAmount);
        assertMapDifferenceCorrect(initialBalances, finalBalances, expectedChange);
    }

    /**
     * This test verifies the Payment calculated fields.
     * @throws GeneralException if an error occurs
     */
    public void testPaymentFieldsCalculation() throws GeneralException {
        PaymentRepositoryInterface repository = billingDomain.getPaymentRepository();
        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        // create a new customer party
        String customerPartyId = createPartyFromTemplate("DemoCustomer", "Cash", "Payer");
        // create a payment
        String paymentId = fa.createPayment(new BigDecimal("100"), customerPartyId, "CUSTOMER_PAYMENT", "CASH");

        // check the fields
        Payment payment = repository.getPaymentById(paymentId);
        assertEquals("Payment [" + paymentId + "] open amount incorrect.", payment.getOpenAmount(), new BigDecimal("100"));
        assertEquals("Payment [" + paymentId + "] applied amount incorrect.", payment.getAppliedAmount(), BigDecimal.ZERO);

        // make an application of 25
        CreatePaymentApplicationService service = new CreatePaymentApplicationService();
        service.setInUserLogin(demofinadmin);
        service.setInPaymentId(paymentId);
        service.setInAmountApplied(new BigDecimal("25"));
        service.setInTaxAuthGeoId("CA");
        runAndAssertServiceSuccess(service);

        // check the fields
        payment = repository.getPaymentById(paymentId);
        assertEquals("Payment [" + paymentId + "] open amount incorrect.", payment.getOpenAmount(), new BigDecimal("75"));
        assertEquals("Payment [" + paymentId + "] applied amount incorrect.", payment.getAppliedAmount(), new BigDecimal("25"));

        // make another application of 30
        service = new CreatePaymentApplicationService();
        service.setInUserLogin(demofinadmin);
        service.setInPaymentId(paymentId);
        service.setInAmountApplied(new BigDecimal("30"));
        service.setInTaxAuthGeoId("CA");
        runAndAssertServiceSuccess(service);

        // check the fields
        payment = repository.getPaymentById(paymentId);
        assertEquals("Payment [" + paymentId + "] open amount incorrect.", payment.getOpenAmount(), new BigDecimal("45"));
        assertEquals("Payment [" + paymentId + "] applied amount incorrect.", payment.getAppliedAmount(), new BigDecimal("55"));

        // change the payment amount to be 80
        UpdatePaymentService service2 = new UpdatePaymentService();
        service2.setInUserLogin(demofinadmin);
        service2.setInPaymentId(paymentId);
        service2.setInAmount(new BigDecimal("80"));
        runAndAssertServiceSuccess(service2);

        // check the fields
        payment = repository.getPaymentById(paymentId);
        assertEquals("Payment [" + paymentId + "] open amount incorrect.", payment.getOpenAmount(), new BigDecimal("25"));
        assertEquals("Payment [" + paymentId + "] applied amount incorrect.", payment.getAppliedAmount(), new BigDecimal("55"));
    }

    // sets PartySupplementalData.parentPartyId for the given partyId
    private void setParentPartyId(String partyId, String parentPartyId) throws GeneralException {
        // TODO: re-factor this to domain objects
        GenericValue partySupplementalData = delegator.findByPrimaryKey("PartySupplementalData", UtilMisc.toMap("partyId", partyId));
        if (partySupplementalData != null) {
            partySupplementalData.set("parentPartyId", parentPartyId);
        } else {
            partySupplementalData = delegator.create("PartySupplementalData", UtilMisc.toMap("partyId", partyId, "parentPartyId", parentPartyId));
        }
        partySupplementalData.store();
    }
}
