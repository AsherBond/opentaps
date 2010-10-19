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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.opentaps.common.order.PurchaseOrderFactory;
import org.opentaps.common.order.SalesOrderFactory;
import org.opentaps.common.util.UtilAccountingTags;
import org.opentaps.base.entities.AcctgTransEntry;
import org.opentaps.domain.ledger.AccountingTransaction;
import org.opentaps.domain.ledger.EncumbranceRepositoryInterface;
import org.opentaps.domain.ledger.LedgerRepositoryInterface;
import org.opentaps.domain.order.Order;
import org.opentaps.domain.order.OrderItem;
import org.opentaps.domain.order.OrderRepositoryInterface;

/**
 * test of the encumbrance feature.
 */
public class EncumbranceTests extends FinancialsTestCase {

    private static final String MODULE = EncumbranceTests.class.getName();

    protected static final String DEMO_SUPPLIER = "DemoSupplier";
    static final String FACILITY_CONTACT_MECH_ID = "9200";

    private GenericValue demopurch1 = null;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        demopurch1 = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "demopurch1"));
        // set a default User
        User = demopurch1;
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * This is a complex encumbrance test and shows the effect of combining PO's which have been
     * completed, cancelled, received, still outstanding plus general ledger entries on encumbrances.
     * @throws GeneralException if an error occurs
     */
    public void testComplexEncumbranceProcess() throws GeneralException {
        // make a copy of "Company" with all its accounting tags.  all subsequent transactions will
        // use this organizationPartyId
        organizationPartyId = createOrganizationFromTemplate("Company", "Complex Encumbrance Testing Organization");

        // create different test products of types FINISHED_GOOD and SUPPLIES
        GenericValue physicalProduct = createTestProduct("Finished good for testComplexEncumbranceProcess", "FINISHED_GOOD", admin);
        String physicalProductId = physicalProduct.getString("productId");
        createMainSupplierForProduct(physicalProductId, DEMO_SUPPLIER, new BigDecimal("100.0"), "USD", new BigDecimal("0.0"), admin);

        GenericValue supplyProduct = createTestProduct("Suppliy product for testComplexEncumbranceProcess", "SUPPLIES", admin);
        String supplyProductId = supplyProduct.getString("productId");
        createMainSupplierForProduct(supplyProductId, DEMO_SUPPLIER, new BigDecimal("200.0"), "USD", new BigDecimal("0.0"), admin);

        // this is used to a CREATED PO
        // Create a PO #1 for 10 physical item @ $100 each and 1 supplies item for $200 with division tag=ENTERPRISE
        PurchaseOrderFactory pof1 = createDefaultPurchaseOrderFactory(organizationPartyId, "PO1 for testComplexEncumbranceProcess tests");
        pof1.addProduct(physicalProduct, "PRODUCT_ORDER_ITEM", new BigDecimal("10.0"), "00001", UtilMisc.toMap("price", new BigDecimal("100.0")), UtilMisc.toMap("acctgTagEnumId1", "DIV_ENTERPRISE"));
        pof1.addProduct(supplyProduct, "SUPPLIES_ORDER_ITEM", new BigDecimal("1.0"), "00001", UtilMisc.toMap("price", new BigDecimal("200.0")), UtilMisc.toMap("acctgTagEnumId1", "DIV_ENTERPRISE"));
        pof1.storeOrder();

        // this is used to test a CANCELLED PO
        // Create a PO #2 for 5 physical item @ $100 each with division tag=ENTERPRISE
        PurchaseOrderFactory pof2 = createDefaultPurchaseOrderFactory(organizationPartyId, "PO2 for testComplexEncumbranceProcess tests");
        pof2.addProduct(physicalProduct, "PRODUCT_ORDER_ITEM", new BigDecimal("5.0"), "00001", UtilMisc.toMap("price", new BigDecimal("100.0")), UtilMisc.toMap("acctgTagEnumId1", "DIV_ENTERPRISE"));
        pof2.storeOrder();
        // Cancel PO #2
        pof2.cancelOrder();

        // this is used to test a COMPLETED PO
        // Create a PO #3 for 7 physical item @ $300 each with division tag=CONSUMER
        PurchaseOrderFactory pof3 = createDefaultPurchaseOrderFactory(organizationPartyId, "PO3 for testComplexEncumbranceProcess tests");
        pof3.addProduct(physicalProduct, "PRODUCT_ORDER_ITEM", new BigDecimal("7.0"), "00001", UtilMisc.toMap("price", new BigDecimal("300.0")), UtilMisc.toMap("acctgTagEnumId1", "DIV_CONSUMER"));
        pof3.storeOrder();
        // Add a shipping charge order adjustment of $19.95
        runAndAssertServiceSuccess("createOrderAdjustment", UtilMisc.toMap("orderId", pof3.getOrderId(), "orderAdjustmentTypeId", "SHIPPING_CHARGES", "amount", new BigDecimal("19.95"), "userLogin", demopurch1));
        // Approve PO #3
        pof3.approveOrder();
        // Fully receive PO #3
        GenericValue orderHeader3 = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", pof3.getOrderId()));
        Map<String, Object> ctxt = createTestInputParametersForReceiveInventoryAgainstPurchaseOrder(orderHeader3, demopurch1);
        runAndAssertServiceSuccess("warehouse.issueOrderItemToShipmentAndReceiveAgainstPO", ctxt);

        // this is also used to test a COMPLETED PO
        // Add a shipping charge order adjustment of $19.95
        // Create PO #4 for 3 supplies item for $500 each with division tag=SMALL_BUSINESS
        PurchaseOrderFactory pof4 = createDefaultPurchaseOrderFactory(organizationPartyId, "PO4 for testComplexEncumbranceProcess tests");
        pof4.addProduct(supplyProduct, "SUPPLIES_ORDER_ITEM", new BigDecimal("3.0"), "00001", UtilMisc.toMap("price", new BigDecimal("500.0")), UtilMisc.toMap("acctgTagEnumId1", "DIV_SMALL_BIZ"));
        pof4.storeOrder();
        // Approve PO #4
        pof4.approveOrder();
        // Fully invoice PO #4
        String orderId4 = pof4.getOrderId();
        List<Map<String, Object>> orderData = FastList.newInstance();
        List<GenericValue> orderItems = delegator.findByAnd("OrderItem", UtilMisc.toMap("orderId", orderId4, "productId", supplyProductId));
        assertEquals("One orderItem exists for [" + orderId4 + "] and product [" + supplyProductId + "]", orderItems.size(), 1.0);
        GenericValue orderItem = EntityUtil.getFirst(orderItems);
        orderData.add(UtilMisc.toMap("orderId", orderId4, "orderItemSeqId", orderItem.get("orderItemSeqId"), "quantity", "3.00"));
        dispatcher.runSync("invoiceSuppliesOrWorkEffortOrderItems", UtilMisc.toMap("orderData", orderData, "userLogin", demopurch1));

        // This is to test a partially
        // Create PO #5 for 1000 physical item for $10 each with division tag=GOVERNMENT and 1000 supplies item for $1.5 each with division tag=SMALL_BUSINESS
        PurchaseOrderFactory pof5 = createDefaultPurchaseOrderFactory(organizationPartyId, "PO5 for testComplexEncumbranceProcess tests");
        pof5.addProduct(physicalProduct, "PRODUCT_ORDER_ITEM", new BigDecimal("1000.0"), "00001", UtilMisc.toMap("price", new BigDecimal("10.0")), UtilMisc.toMap("acctgTagEnumId1", "DIV_GOV"));
        pof5.addProduct(supplyProduct, "SUPPLIES_ORDER_ITEM", new BigDecimal("1000.0"), "00001", UtilMisc.toMap("price", new BigDecimal("1.5")), UtilMisc.toMap("acctgTagEnumId1", "DIV_SMALL_BIZ"));
        pof5.storeOrder();
        String orderId5 = pof5.getOrderId();

        OrderRepositoryInterface repository = getOrderRepository(admin);
        Order order = repository.getOrderById(orderId5);
        List<OrderItem> ordItems = order.getOrderItems();
        String supplyOrderItemSeqId = null;
        String physicalOrderItemSeqId = null;
        for (OrderItem ordItem : ordItems) {
            if (physicalProductId.equals(ordItem.getProductId())) {
                physicalOrderItemSeqId = ordItem.getOrderItemSeqId();
            } else if (supplyProductId.equals(ordItem.getProductId())) {
                supplyOrderItemSeqId = ordItem.getOrderItemSeqId();
            }
        }

        // Update physical item to 1500 at $20 each
        updateOrderItem(orderId5, physicalOrderItemSeqId, "1500.0", "20.0", "Item after new quantity and price updated",  demopurch1);
        // Update supplies item to 2000 at $0.75 each
        updateOrderItem(orderId5, supplyOrderItemSeqId, "2000.0", "0.75", "Item after new quantity and price updated",  demopurch1);
        // Add shipping charge of $19.95
        runAndAssertServiceSuccess("createOrderAdjustment", UtilMisc.toMap("orderId", orderId5, "orderAdjustmentTypeId", "SHIPPING_CHARGES", "amount", new BigDecimal("19.95"), "userLogin", demopurch1));
        // Receive 1000 of the physical item, but do not close the PO -- keep it open
        GenericValue orderHeader5 = delegator.findByPrimaryKey("OrderHeader", UtilMisc.toMap("orderId", orderId5));
        ctxt = createTestInputParametersForReceiveInventoryAgainstPurchaseOrder(orderHeader5, UtilMisc.toMap(physicalOrderItemSeqId, "1000.0"), false, demopurch1);
        runAndAssertServiceSuccess("warehouse.issueOrderItemToShipmentAndReceiveAgainstPO", ctxt);
        // Invoice 1000 of the supplies item
        orderData.clear();
        orderData.add(UtilMisc.<String, Object>toMap("orderId", orderId5, "orderItemSeqId", supplyOrderItemSeqId, "quantity", "1000.00"));
        dispatcher.runSync("invoiceSuppliesOrWorkEffortOrderItems", UtilMisc.toMap("orderData", orderData, "userLogin", demopurch1));

        // Create PO #6 for 5000 physical item for $9 each with division tag=CONSUMER
        PurchaseOrderFactory pof6 = createDefaultPurchaseOrderFactory(organizationPartyId, "PO6 for testComplexEncumbranceProcess tests");
        pof6.addProduct(physicalProduct, "PRODUCT_ORDER_ITEM", new BigDecimal("5000.0"), "00001", UtilMisc.toMap("price", new BigDecimal("9.0")), UtilMisc.toMap("acctgTagEnumId1", "DIV_CONSUMER"));
        pof6.storeOrder();
        pof6.approveOrder();
        String orderId6 = pof6.getOrderId();
        // Cancel 2000 of the physical item--ie cancelQuantity=2000
        orderItems = delegator.findByAnd("OrderItem", UtilMisc.toMap("orderId", orderId6, "productId", physicalProductId));
        cancelOrderItem(orderId6, (String) (EntityUtil.getFirst(orderItems).get("orderItemSeqId")), "00001", new BigDecimal("2000.0"), demopurch1);
        // Add shipping charges of $838.43
        runAndAssertServiceSuccess("createOrderAdjustment", UtilMisc.toMap("orderId", pof6.getOrderId(), "orderAdjustmentTypeId", "SHIPPING_CHARGES", "amount", new BigDecimal("838.43"), "userLogin", demopurch1));
        // Put PO #6 on hold
        runAndAssertServiceSuccess("changeOrderStatus", UtilMisc.toMap("orderId", orderId6, "statusId", "ORDER_HOLD", "userLogin", demopurch1));

        // Test the effects of accounting transactions on the encumbrance ledger (glFiscalTypeId=ENCUMBRANCE)
        // AT-1 and AT-2 will be part of the encumbrance total, AT-3 and AT-4 will not because AT-3 is on the ACTUAL ledger, and AT-4 has not been posted yet.

        LedgerRepositoryInterface ledgerMethods = domainsDirectory.getLedgerDomain().getLedgerRepository();

        // Create an Accounting Transaction (AT-1) with glFiscalTypeId=ENCUMBRANCE the following entries:
        AccountingTransaction at1 = new AccountingTransaction();
        at1.setAcctgTransTypeId("INTERNAL_ACCTG_TRANS");
        at1.setGlFiscalTypeId("ENCUMBRANCE");

        // Debit 601000 $1000 tag=CONSUMER
        AcctgTransEntry de1 = makeDebitEntry(organizationPartyId, "601000", new BigDecimal("1000"), "DIV_CONSUMER");
        // Debit 601000 $2000 tag=ENTERPRISE
        AcctgTransEntry de2 = makeDebitEntry(organizationPartyId, "601000", new BigDecimal("2000"), "DIV_ENTERPRISE");
        // Debit 680000 $3000 tag=CONSUMER
        AcctgTransEntry de3 = makeDebitEntry(organizationPartyId, "680000", new BigDecimal("3000"), "DIV_CONSUMER");
        // Credit 221000 $1000 tag=CONSUMER
        AcctgTransEntry ce1 = makeCreditEntry(organizationPartyId, "221000", new BigDecimal("1000"), "DIV_CONSUMER");
        // Credit 221000 $2000 tag=ENTERPRISE
        AcctgTransEntry ce2 = makeCreditEntry(organizationPartyId, "221000", new BigDecimal("2000"), "DIV_ENTERPRISE");
        // Credit 229000 $3000 tag=CONSUMER
        AcctgTransEntry ce3 = makeCreditEntry(organizationPartyId, "229000", new BigDecimal("3000"), "DIV_CONSUMER");
        // Post AT-1
        ledgerMethods.storeAcctgTransAndEntries(at1, Arrays.asList(de1, de2, de3, ce1, ce2, ce3));

        // Create an Accounting Transaction (AT-2) with glFiscalTypeId=ENCUMBRANCE
        AccountingTransaction at2 = new AccountingTransaction();
        at2.setAcctgTransTypeId("INTERNAL_ACCTG_TRANS");
        at2.setGlFiscalTypeId("ENCUMBRANCE");

        // Debit 610000 $2000 tag=ENTERPRISE
        AcctgTransEntry de4 = makeDebitEntry(organizationPartyId, "610000", new BigDecimal("2000"), "DIV_ENTERPRISE");
        // Debit 610000 $2000 tag=CONSUMER
        AcctgTransEntry de5 = makeDebitEntry(organizationPartyId, "610000", new BigDecimal("2000"), "DIV_CONSUMER");
        // Debit 610000 $2000 tag=GOVERNMENT
        AcctgTransEntry de6 = makeDebitEntry(organizationPartyId, "610000", new BigDecimal("2000"), "DIV_GOV");
        // Credit 229000 $2000 tag=ENTERPRISE
        AcctgTransEntry ce4 = makeCreditEntry(organizationPartyId, "229000", new BigDecimal("2000"), "DIV_ENTERPRISE");
        // Credit 229000 $2000 tag=CONSUMER
        AcctgTransEntry ce5 = makeCreditEntry(organizationPartyId, "229000", new BigDecimal("2000"), "DIV_CONSUMER");
        // Credit 229000 $2000 tag=GOVERNMENT
        AcctgTransEntry ce6 = makeCreditEntry(organizationPartyId, "229000", new BigDecimal("2000"), "DIV_GOV");
        // Post AT-2
        ledgerMethods.storeAcctgTransAndEntries(at2, Arrays.asList(de4, de5, de6, ce4, ce5, ce6));

        // Create an Accounting Transaction (AT-3) with glFiscalTypeId=ACTUAL:
        AccountingTransaction at3 = new AccountingTransaction();
        at3.setAcctgTransTypeId("INTERNAL_ACCTG_TRANS");
        at3.setGlFiscalTypeId("ACTUAL");

        // Debit 111100 $1000000 tag=ENTERPRISE
        AcctgTransEntry de7 = makeDebitEntry(organizationPartyId, "111100", new BigDecimal("1000000"), "DIV_ENTERPRISE");
        // Credit 330000 $1000000 tag=ENTERPRISE
        AcctgTransEntry ce7 = makeCreditEntry(organizationPartyId, "330000", new BigDecimal("1000000"), "DIV_ENTERPRISE");
        // Post AT-3
        ledgerMethods.storeAcctgTransAndEntries(at3, Arrays.asList(de7, ce7));

        // Create an Accounting Transaction (AT-4) with glFiscalTypeId=ACTUAL:
        AccountingTransaction at4 = new AccountingTransaction();
        at4.setAcctgTransTypeId("INTERNAL_ACCTG_TRANS");
        at4.setGlFiscalTypeId("ACTUAL");

        // Debit 680000 $100000 tag=ENTERPRISE
        AcctgTransEntry de8 = makeDebitEntry(organizationPartyId, "680000", new BigDecimal("100000"), "DIV_ENTERPRISE");
        // Credit 229000 $100000 tag=ENTERPRISE
        AcctgTransEntry ce8 = makeCreditEntry(organizationPartyId, "229000", new BigDecimal("100000"), "DIV_ENTERPRISE");
        // Do not post AT-4
        at4.setAutoPost(false);
        ledgerMethods.storeAcctgTransAndEntries(at4, Arrays.asList(de8, ce8));

        // Run the createEncumbranceSnapshotAndDetail service
        ctxt = UtilMisc.toMap("organizationPartyId", organizationPartyId, "userLogin", demofinadmin);
        runAndAssertServiceSuccess("createEncumbranceSnapshotAndDetail", ctxt);

        // Get total encumbered value with EncumbranceRepositoryInterface.getTotalEncumberedValue
        EncumbranceRepositoryInterface encumbranceMethods = domainsDirectory.getLedgerDomain().getEncumbranceRepository();
        Timestamp moment = UtilDateTime.nowTimestamp();

        // for all fund tags, verify value is $51788.43
        BigDecimal encumberedValue = null;
        encumberedValue = encumbranceMethods.getTotalEncumberedValue(organizationPartyId, null, moment);
        assertEquals("Wrong encumbered value for all accounting tags", new BigDecimal("51788.43"), encumberedValue);

        // for all tag=CONSUMER, verify value is $33000
        encumberedValue = encumbranceMethods.getTotalEncumberedValue(organizationPartyId, UtilMisc.toMap("acctgTagEnumId1", "DIV_CONSUMER"), moment);
        assertEquals("Wrong encumbered value for CUSTOMER tag", new BigDecimal("33000.0"), encumberedValue);

        // for all tag=ENTERPRISE, verify value is $5200
        encumberedValue = encumbranceMethods.getTotalEncumberedValue(organizationPartyId, UtilMisc.toMap("acctgTagEnumId1", "DIV_ENTERPRISE"), moment);
        assertEquals("Wrong encumbered value for ENTERPRISE tag", new BigDecimal("5200.0"), encumberedValue);

        // for all tag=GOVERNMENT, verify value is $12000
        encumberedValue = encumbranceMethods.getTotalEncumberedValue(organizationPartyId, UtilMisc.toMap("acctgTagEnumId1", "DIV_GOV"), moment);
        assertEquals("Wrong encumbered value for GOVERNMENT tag", new BigDecimal("12000.0"), encumberedValue);

        // for all tag=SMALL_BUSINESS, verify value is $750
        encumberedValue = encumbranceMethods.getTotalEncumberedValue(organizationPartyId, UtilMisc.toMap("acctgTagEnumId1", "DIV_SMALL_BIZ"), moment);
        assertEquals("Wrong encumbered value for SMALL_BUSINESS tag", new BigDecimal("750.0"), encumberedValue);

        // for all tag=EDUCATION, verify value is 0
        encumberedValue = encumbranceMethods.getTotalEncumberedValue(organizationPartyId, UtilMisc.toMap("acctgTagEnumId1", "DIV_EDU"), moment);
        assertEquals("Wrong encumbered value for EDUCATION tag", BigDecimal.ZERO, encumberedValue);

        // for all tag=untagged, verify value is $838.43
        encumberedValue = encumbranceMethods.getTotalEncumberedValue(organizationPartyId, UtilMisc.toMap("acctgTagEnumId1", "NULL_TAG"), moment);
        assertEquals("Wrong encumbered value for untagged case", new BigDecimal("838.43"), encumberedValue);

    }

    /**
     * This test is for verifying the features of the income/budget/encumbrances balances report.
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testIncomeBudgetEncumbrancesBalancesReport() throws Exception {
        // make a copy of "Company" with all its accounting tags.  all subsequent transactions will
        // use this organizationPartyId
        organizationPartyId = createOrganizationFromTemplate("Company", "Income Budget Encumbrances Balances Report Organization");

        // create 2 finished good test products
        GenericValue physicalProduct = createTestProduct("First test product for testIncomeBudgetEncumbrancesBalancesReport", "FINISHED_GOOD", admin);
        String physicalProductId = physicalProduct.getString("productId");
        createMainSupplierForProduct(physicalProductId, DEMO_SUPPLIER, new BigDecimal("100.0"), "USD", new BigDecimal("0.0"), admin);
        assignDefaultPrice(physicalProduct, new BigDecimal("100.0"), admin);

        GenericValue physicalProduct2 = createTestProduct("Second test product for testIncomeBudgetEncumbrancesBalancesReport", "FINISHED_GOOD", admin);
        String physicalProductId2 = physicalProduct2.getString("productId");
        createMainSupplierForProduct(physicalProductId2, DEMO_SUPPLIER, new BigDecimal("100.0"), "USD", new BigDecimal("0.0"), admin);
        assignDefaultPrice(physicalProduct2, new BigDecimal("25.0"), admin);

        // create a supplies test product
        GenericValue suppliesProduct = createTestProduct("Supplies test product for testIncomeBudgetEncumbrancesBalancesReport", "SUPPLIES", admin);
        String suppliesProductId = suppliesProduct.getString("productId");
        createMainSupplierForProduct(suppliesProductId, DEMO_SUPPLIER, new BigDecimal("1.0"), "USD", BigDecimal.ZERO, admin);

        LedgerRepositoryInterface ledgerMethods = domainsDirectory.getLedgerDomain().getLedgerRepository();

        // some shared data
        Timestamp timestamp = UtilDateTime.nowTimestamp();

        // these transactions create 2 budgets

        // create a budget for the ENTERPRISE tag: an AccountingTransaction with AcctgTransEntries for organizationPartyId with glFiscalTypeId=BUDGET
        // all the AcctgTransEntry should have tag DIV_ENTERPRISE
        AccountingTransaction b1 = new AccountingTransaction();
        b1.setAcctgTransTypeId("INTERNAL_ACCTG_TRANS");
        b1.setGlFiscalTypeId("BUDGET");
        b1.setTransactionDate(UtilDateTime.nowTimestamp());
        // Credit glAccountId=401000 100000
        AcctgTransEntry b1c1 = makeCreditEntry(organizationPartyId, "401000", new BigDecimal("100000"), "DIV_ENTERPRISE");
        // Debit glAccountId=500000   50000
        AcctgTransEntry b1d1 = makeDebitEntry(organizationPartyId, "500000", new BigDecimal("50000"), "DIV_ENTERPRISE");
        // Debit glAccountId=601000   15000
        AcctgTransEntry b1d2 = makeDebitEntry(organizationPartyId, "601000", new BigDecimal("15000"), "DIV_ENTERPRISE");
        // Debit glAccountId=610000    5000
        AcctgTransEntry b1d3 = makeDebitEntry(organizationPartyId, "610000", new BigDecimal("5000"), "DIV_ENTERPRISE");
        // Debit glAccountId=680000    5000
        AcctgTransEntry b1d4 = makeDebitEntry(organizationPartyId, "680000", new BigDecimal("5000"), "DIV_ENTERPRISE");
        // Debit glAccountId=900000   10000
        AcctgTransEntry b1d5 = makeDebitEntry(organizationPartyId, "900000", new BigDecimal("10000"), "DIV_ENTERPRISE");
        // Debit glAccountId=890000  15000  (budgeted net income)
        AcctgTransEntry b1d6 = makeDebitEntry(organizationPartyId, "890000", new BigDecimal("15000"), "DIV_ENTERPRISE");
        // => total debit = 10000 + 5000 + 5000 + 15000 + 50000 +15000 = 100000
        // post this accounting transaction
        ledgerMethods.storeAcctgTransAndEntries(b1, Arrays.asList(b1c1, b1d1, b1d2, b1d3, b1d4, b1d5, b1d6));

        // create a budget for the CONSUMER tag: an AccountingTransaction with AcctgTransEntries for organizationPartyId with glFiscalTypeId=BUDGET
        // all the AcctgTransEntry should have tag DIV_CONSUMER
        AccountingTransaction b2 = new AccountingTransaction();
        b2.setAcctgTransTypeId("INTERNAL_ACCTG_TRANS");
        b2.setGlFiscalTypeId("BUDGET");
        b2.setTransactionDate(UtilDateTime.nowTimestamp());
        // Credit glAccountId=401000 200000
        AcctgTransEntry b2c1 = makeCreditEntry(organizationPartyId, "401000", new BigDecimal("200000"), "DIV_CONSUMER");
        b2c1.setAcctgTagEnumId2("DPT_MANUFACTURING"); // adding another tag, it should not appear in the report
        // Debit glAccountId=500000 70000
        AcctgTransEntry b2d1 = makeDebitEntry(organizationPartyId, "500000", new BigDecimal("70000"), "DIV_CONSUMER");
        b2d1.setAcctgTagEnumId2("DPT_MANUFACTURING"); // adding another tag, it should not appear in the report
        // Debit glAccountId=601000 60000
        AcctgTransEntry b2d2 = makeDebitEntry(organizationPartyId, "601000", new BigDecimal("60000"), "DIV_CONSUMER");
        b2d2.setAcctgTagEnumId2("DPT_MANUFACTURING"); // adding another tag, it should not appear in the report
        // Debit glAccountId=610000 20000
        AcctgTransEntry b2d3 = makeDebitEntry(organizationPartyId, "610000", new BigDecimal("20000"), "DIV_CONSUMER");
        b2d3.setAcctgTagEnumId2("DPT_MANUFACTURING"); // adding another tag, it should not appear in the report
        // Debit glAccountId=680000 10000
        AcctgTransEntry b2d4 = makeDebitEntry(organizationPartyId, "680000", new BigDecimal("10000"), "DIV_CONSUMER");
        b2d4.setAcctgTagEnumId2("DPT_MANUFACTURING"); // adding another tag, it should not appear in the report
        // Debit glAccountId=900000 16000
        AcctgTransEntry b2d5 = makeDebitEntry(organizationPartyId, "900000", new BigDecimal("16000"), "DIV_CONSUMER");
        b2d5.setAcctgTagEnumId2("DPT_MANUFACTURING"); // adding another tag, it should not appear in the report
        // Debit glAccountId=890000 24000  (budgeted net income)
        AcctgTransEntry b2d6 = makeDebitEntry(organizationPartyId, "890000", new BigDecimal("24000"), "DIV_CONSUMER");
        b2d6.setAcctgTagEnumId2("DPT_MANUFACTURING"); // adding another tag, it should not appear in the report
        // post this accounting transaction
        ledgerMethods.storeAcctgTransAndEntries(b2, Arrays.asList(b2c1, b2d1, b2d2, b2d3, b2d4, b2d5, b2d6));

        // these transactions create a real income statement

        // receive 2000 of the physicalProduct at 50, tag DIV_ENTERPRISE
        Map input = UtilMisc.toMap("userLogin", admin);
        input.put("facilityId", "WebStoreWarehouse");
        input.put("productId", physicalProductId);
        input.put("inventoryItemTypeId", "NON_SERIAL_INV_ITEM");
        input.put("quantityAccepted", new BigDecimal("2000.0"));
        input.put("quantityRejected", new BigDecimal("0.0"));
        input.put("datetimeReceived", UtilDateTime.nowTimestamp());
        input.put("unitCost", new BigDecimal("50.00"));
        input.put("ownerPartyId", organizationPartyId); // this is important -- the inventory must be owned by the organization of the test to show up as COGS
        input.put("acctgTagEnumId1", "DIV_ENTERPRISE");
        input.put("acctgTagEnumId2", "DPT_CORPORATE"); // adding another tag, it should not appear in the report
        runAndAssertServiceSuccess("receiveInventoryProduct", input);
        // receive 5000 of the physicalProduct2 at 10, tag DIV_CONSUMER
        input = UtilMisc.toMap("userLogin", admin);
        input.put("facilityId", "WebStoreWarehouse");
        input.put("productId", physicalProductId2);
        input.put("inventoryItemTypeId", "NON_SERIAL_INV_ITEM");
        input.put("quantityAccepted", new BigDecimal("5000.0"));
        input.put("quantityRejected", new BigDecimal("0.0"));
        input.put("datetimeReceived", UtilDateTime.nowTimestamp());
        input.put("unitCost", new BigDecimal("10.00"));
        input.put("ownerPartyId", organizationPartyId);
        input.put("acctgTagEnumId1", "DIV_CONSUMER");
        input.put("acctgTagEnumId2", "DPT_MANUFACTURING"); // adding another tag, it should not appear in the report
        runAndAssertServiceSuccess("receiveInventoryProduct", input);

        final String productStoreId = "9000";

        // copy a customer from DemoAccount1
        String customerPartyId = createPartyFromTemplate("DemoAccount1", "Customer for testIncomeBudgetEncumbrancesBalancesReport");
        // create a sales order for 750 of physicalProduct at $100, tag DIV_ENTERPRISE
        SalesOrderFactory salesOrder = new SalesOrderFactory(delegator, dispatcher, admin, organizationPartyId, customerPartyId, productStoreId);
        salesOrder.addProduct(physicalProduct, "PRODUCT_ORDER_ITEM", new BigDecimal("750.0"), salesOrder.getFirstShipGroup(), UtilMisc.toMap("price", new BigDecimal("100.0"), "listPrice", new BigDecimal("100.0")), UtilMisc.toMap("acctgTagEnumId1", "DIV_ENTERPRISE"));
        // approve sales order and ship it
        salesOrder.approveOrder();
        runAndAssertServiceSuccess("testShipOrder", UtilMisc.toMap("orderId", salesOrder.getOrderId(), "facilityId", "WebStoreWarehouse", "userLogin", admin));

        // create a sales order for 4000 of physicalProduct2 at $25, tag DIV_CONSUMER
        salesOrder = new SalesOrderFactory(delegator, dispatcher, admin, organizationPartyId, customerPartyId, productStoreId);
        salesOrder.addProduct(physicalProduct2, "PRODUCT_ORDER_ITEM", new BigDecimal("4000.0"), salesOrder.getFirstShipGroup(), UtilMisc.toMap("price", new BigDecimal("25.0"), "listPrice", new BigDecimal("25.0")), UtilMisc.toMap("acctgTagEnumId1", "DIV_CONSUMER"));
        // approve sales order and ship it
        salesOrder.approveOrder();
        runAndAssertServiceSuccess("testShipOrder", UtilMisc.toMap("orderId", salesOrder.getOrderId(), "facilityId", "WebStoreWarehouse", "userLogin", admin));

        // book expenses
        // create an accounting transaction of glFiscalTypeId=ACTUAL
        AccountingTransaction e1 = new AccountingTransaction();
        e1.setAcctgTransTypeId("INTERNAL_ACCTG_TRANS");
        e1.setGlFiscalTypeId("ACTUAL");
        e1.setTransactionDate(UtilDateTime.nowTimestamp());
        // Credit glAccountId=210000 27600 tag DIV_ENTERPRISE
        AcctgTransEntry e1c1 = makeCreditEntry(organizationPartyId, "210000", new BigDecimal("27600"), "DIV_ENTERPRISE");
        // Debit glAccountId=601000 12000 tag DIV_ENTERPRISE
        AcctgTransEntry e1d1 = makeDebitEntry(organizationPartyId, "601000", new BigDecimal("12000"), "DIV_ENTERPRISE");
        // Debit glAccountId=610000 6000 tag DIV_ENTERPRISE
        AcctgTransEntry e1d2 = makeDebitEntry(organizationPartyId, "610000", new BigDecimal("6000"), "DIV_ENTERPRISE");
        // Debit glAccountId=680000 3000 tag DIV_ENTERPRISE
        AcctgTransEntry e1d3 = makeDebitEntry(organizationPartyId, "680000", new BigDecimal("3000"), "DIV_ENTERPRISE");
        // Debit glAccountId=900000 6600 tag DIV_ENTERPRISE
        AcctgTransEntry e1d4 = makeDebitEntry(organizationPartyId, "900000", new BigDecimal("6600"), "DIV_ENTERPRISE");
        // post the accounting transaction
        ledgerMethods.storeAcctgTransAndEntries(e1, Arrays.asList(e1c1, e1d1, e1d2, e1d3, e1d4));

        // create an accounting transaction of glFiscalTypeId=ACTUAL
        AccountingTransaction e2 = new AccountingTransaction();
        e2.setAcctgTransTypeId("INTERNAL_ACCTG_TRANS");
        e2.setGlFiscalTypeId("ACTUAL");
        e2.setTransactionDate(UtilDateTime.nowTimestamp());
        // Credit glAccountId=900000 2000 tag DIV_CONSUMER
        AcctgTransEntry e2c1 = makeCreditEntry(organizationPartyId, "900000", new BigDecimal("2000"), "DIV_CONSUMER");
        // Credit glAccountId=210000 63000 tag DIV_CONSUMER
        AcctgTransEntry e2c2 = makeCreditEntry(organizationPartyId, "210000", new BigDecimal("63000"), "DIV_CONSUMER");
        // Debit glAccountId=601000 50000 tag DIV_CONSUMER
        AcctgTransEntry e2d1 = makeDebitEntry(organizationPartyId, "601000", new BigDecimal("50000"), "DIV_CONSUMER");
        // Debit glAccountId=610000 10000 tag DIV_CONSUMER
        AcctgTransEntry e2d2 = makeDebitEntry(organizationPartyId, "610000", new BigDecimal("10000"), "DIV_CONSUMER");
        // Debit glAccountId=680000 5000 tag DIV_CONSUMER
        AcctgTransEntry e2d3 = makeDebitEntry(organizationPartyId, "680000", new BigDecimal("5000"), "DIV_CONSUMER");
        // post the accounting transaction
        ledgerMethods.storeAcctgTransAndEntries(e2, Arrays.asList(e2c1, e2c2, e2d1, e2d2, e2d3));

        // create an accounting transaction of glFiscalTypeId=ACTUAL
        AccountingTransaction e3 = new AccountingTransaction();
        e3.setAcctgTransTypeId("INTERNAL_ACCTG_TRANS");
        e3.setGlFiscalTypeId("ACTUAL");
        e3.setTransactionDate(UtilDateTime.nowTimestamp());
        // debit 601000 123456789 tag DIV_ENTERPRISE
        AcctgTransEntry e3d1 = makeDebitEntry(organizationPartyId, "601000", new BigDecimal("123456789"), "DIV_ENTERPRISE");
        // credit 210000 123456789 tag DIV_ENTERPRISE
        AcctgTransEntry e3c1 = makeCreditEntry(organizationPartyId, "210000", new BigDecimal("123456789"), "DIV_ENTERPRISE");
        // BUT DO NOT POST THIS TRANSACTION -- we will verify that unposted transactions have no effect
        e3.setAutoPost(false);
        ledgerMethods.storeAcctgTransAndEntries(e3, Arrays.asList(e3c1, e3d1));

        // Create a PO (po1) for 2000 of physicalProduct at 50 tag DIV_ENTERPRISE and 5000 of physicalProduct2 at 10 tag DIV_CONSUMER
        PurchaseOrderFactory po = createDefaultPurchaseOrderFactory(organizationPartyId, "PO 1 for testIncomeBudgetEncumbrancesBalancesReport");
        po.addProduct(physicalProduct, "PRODUCT_ORDER_ITEM", new BigDecimal("2000.0"), "00001", UtilMisc.toMap("price", new BigDecimal("50.0")), UtilMisc.toMap("acctgTagEnumId1", "DIV_ENTERPRISE"));
        po.addProduct(physicalProduct2, "PRODUCT_ORDER_ITEM", new BigDecimal("5000.0"), "00001", UtilMisc.toMap("price", new BigDecimal("10.0")), UtilMisc.toMap("acctgTagEnumId1", "DIV_CONSUMER"));
        po.storeOrder();
        po.approveOrder();

        // Create a PO (po2) for 5000 of suppliesProduct at 10 tag DIV_CONSUMER
        PurchaseOrderFactory po2 = createDefaultPurchaseOrderFactory(organizationPartyId, "PO 2 for testIncomeBudgetEncumbrancesBalancesReport");
        po2.addProduct(suppliesProduct, "SUPPLIES_ORDER_ITEM", new BigDecimal("5000.0"), "00001", UtilMisc.toMap("price", new BigDecimal("10.0")), UtilMisc.toMap("acctgTagEnumId1", "DIV_CONSUMER"));
        po2.storeOrder();
        // approve the PO
        po2.approveOrder();

        // Run the financials.collectEncumbranceAndTransEntryFacts service
        runAndAssertServiceSuccess("financials.collectEncumbranceAndTransEntryFacts", UtilMisc.toMap("organizationPartyId", organizationPartyId, "userLogin", demofinadmin));

        // run the balances report.
        Map results = runAndAssertServiceSuccess("balanceStatementReport", UtilMisc.toMap("userLogin", admin, "organizationPartyId", organizationPartyId, "fromDate", new Timestamp(timestamp.getTime() - 10000), "thruDate", new Timestamp(UtilDateTime.nowTimestamp().getTime() + 10000), "includeBudgetIncome", "Y"));

        // check the results:
        // for CONSUMER tag: Budget=24000 Income=100000 Expense=103000 Liens=100000 Balance=Budget + Income - Expense - Liens = -79000
        // for ENTERPRISE tag: Budget=15000 Income=75000 Expense=65100 Liens=100000 Balance= -75100
        List<Map<String, Object>> data = (List<Map<String, Object>>) results.get("reportData");
        BigDecimal consumerBudgetTotal = BigDecimal.ZERO;
        BigDecimal consumerIncomeTotal = BigDecimal.ZERO;
        BigDecimal consumerExpenseTotal = BigDecimal.ZERO;
        BigDecimal consumerLiensTotal = BigDecimal.ZERO;
        BigDecimal consumerBalanceTotal = BigDecimal.ZERO;
        BigDecimal enterpriseBudgetTotal = BigDecimal.ZERO;
        BigDecimal enterpriseIncomeTotal = BigDecimal.ZERO;
        BigDecimal enterprisExpenseTotal = BigDecimal.ZERO;
        BigDecimal enterprisLiensTotal = BigDecimal.ZERO;
        BigDecimal enterprisBalanceTotal = BigDecimal.ZERO;
        for (Map<String, Object> line : data) {
            Debug.logInfo("balanceStatementReport line: " + line, MODULE);
            String tag1 = (String) line.get("acctgTagEnumId1");
            // note, the enum codes are in the report data instead of the enum id
            if ("CONSUMER".equals(tag1)) {
                consumerBudgetTotal = consumerBudgetTotal.add((BigDecimal) line.get("budget"));
                consumerIncomeTotal = consumerIncomeTotal.add((BigDecimal) line.get("income"));
                consumerExpenseTotal = consumerExpenseTotal.add((BigDecimal) line.get("expense"));
                consumerLiensTotal = consumerLiensTotal.add((BigDecimal) line.get("liens"));
                consumerBalanceTotal = consumerBalanceTotal.add((BigDecimal) line.get("balance"));
            } else if ("ENTERPRISE".equals(tag1)) {
                enterpriseBudgetTotal = enterpriseBudgetTotal.add((BigDecimal) line.get("budget"));
                enterpriseIncomeTotal = enterpriseIncomeTotal.add((BigDecimal) line.get("income"));
                enterprisExpenseTotal = enterprisExpenseTotal.add((BigDecimal) line.get("expense"));
                enterprisLiensTotal = enterprisLiensTotal.add((BigDecimal) line.get("liens"));
                enterprisBalanceTotal = enterprisBalanceTotal.add((BigDecimal) line.get("balance"));
            } else {
                fail("Unexpected tag in report data: " + tag1);
            }
        }

        assertEquals("Budget value incorrect for tag CONSUMER.", new BigDecimal("24000"), consumerBudgetTotal);
        assertEquals("Income value incorrect for tag CONSUMER.", new BigDecimal("100000"), consumerIncomeTotal);
        assertEquals("Expense value incorrect for tag CONSUMER.", new BigDecimal("103000"), consumerExpenseTotal);
        assertEquals("Liens value incorrect for tag CONSUMER.", new BigDecimal("100000"), consumerLiensTotal);
        assertEquals("Balance value incorrect for tag CONSUMER.", new BigDecimal("-79000"), consumerBalanceTotal);

        assertEquals("Budget value incorrect for tag ENTERPRISE.", new BigDecimal("15000"), enterpriseBudgetTotal);
        assertEquals("Income value incorrect for tag ENTERPRISE.", new BigDecimal("75000"), enterpriseIncomeTotal);
        assertEquals("Expense value incorrect for tag ENTERPRISE.", new BigDecimal("65100"), enterprisExpenseTotal);
        assertEquals("Liens value incorrect for tag ENTERPRISE.", new BigDecimal("100000"), enterprisLiensTotal);
        assertEquals("Balance value incorrect for tag ENTERPRISE.", new BigDecimal("-75100"), enterprisBalanceTotal);

        // now verify that the GlAccountTransEntryFact transformations are correct
        // first verify the BUDGET accounting transactions
        verifyAcctgTransEntryFact(b1c1, organizationPartyId, "401000", new BigDecimal("100000.0"), BigDecimal.ZERO, BigDecimal.ZERO);
        verifyAcctgTransEntryFact(b1d1, organizationPartyId, "500000", new BigDecimal("50000"), BigDecimal.ZERO, BigDecimal.ZERO);
        verifyAcctgTransEntryFact(b1d2, organizationPartyId, "601000", new BigDecimal("15000"), BigDecimal.ZERO, BigDecimal.ZERO);
        verifyAcctgTransEntryFact(b1d3, organizationPartyId, "610000", new BigDecimal("5000"), BigDecimal.ZERO, BigDecimal.ZERO);
        verifyAcctgTransEntryFact(b1d4, organizationPartyId, "680000", new BigDecimal("5000"), BigDecimal.ZERO, BigDecimal.ZERO);
        verifyAcctgTransEntryFact(b1d5, organizationPartyId, "900000", new BigDecimal("10000"), BigDecimal.ZERO, BigDecimal.ZERO);
        verifyAcctgTransEntryFact(b1d6, organizationPartyId, "890000", new BigDecimal("15000"), BigDecimal.ZERO, BigDecimal.ZERO);

        verifyAcctgTransEntryFact(b2c1, organizationPartyId, "401000", new BigDecimal("200000"), BigDecimal.ZERO, BigDecimal.ZERO);
        verifyAcctgTransEntryFact(b2d1, organizationPartyId, "500000", new BigDecimal("70000"), BigDecimal.ZERO, BigDecimal.ZERO);
        verifyAcctgTransEntryFact(b2d2, organizationPartyId, "601000", new BigDecimal("60000"), BigDecimal.ZERO, BigDecimal.ZERO);
        verifyAcctgTransEntryFact(b2d3, organizationPartyId, "610000", new BigDecimal("20000"), BigDecimal.ZERO, BigDecimal.ZERO);
        verifyAcctgTransEntryFact(b2d4, organizationPartyId, "680000", new BigDecimal("10000"), BigDecimal.ZERO, BigDecimal.ZERO);
        verifyAcctgTransEntryFact(b2d5, organizationPartyId, "900000", new BigDecimal("16000"), BigDecimal.ZERO, BigDecimal.ZERO);
        verifyAcctgTransEntryFact(b2d6, organizationPartyId, "890000", new BigDecimal("24000"), BigDecimal.ZERO, BigDecimal.ZERO);

        // now verify the ACTUAL accounting transactions
        verifyAcctgTransEntryFact(e1c1, organizationPartyId, "210000", BigDecimal.ZERO, new BigDecimal("27600"), BigDecimal.ZERO);
        verifyAcctgTransEntryFact(e1d1, organizationPartyId, "601000", BigDecimal.ZERO, new BigDecimal("12000"), BigDecimal.ZERO);
        verifyAcctgTransEntryFact(e1d2, organizationPartyId, "610000", BigDecimal.ZERO, new BigDecimal("6000"), BigDecimal.ZERO);
        verifyAcctgTransEntryFact(e1d3, organizationPartyId, "680000", BigDecimal.ZERO, new BigDecimal("3000"), BigDecimal.ZERO);
        verifyAcctgTransEntryFact(e1d4, organizationPartyId, "900000", BigDecimal.ZERO, new BigDecimal("6600"), BigDecimal.ZERO);

        verifyAcctgTransEntryFact(e2c1, organizationPartyId, "900000", BigDecimal.ZERO, new BigDecimal("-2000"), BigDecimal.ZERO);
        verifyAcctgTransEntryFact(e2c2, organizationPartyId, "210000", BigDecimal.ZERO, new BigDecimal("63000"), BigDecimal.ZERO);
        verifyAcctgTransEntryFact(e2d1, organizationPartyId, "601000", BigDecimal.ZERO, new BigDecimal("50000"), BigDecimal.ZERO);
        verifyAcctgTransEntryFact(e2d2, organizationPartyId, "610000", BigDecimal.ZERO, new BigDecimal("10000"), BigDecimal.ZERO);
        verifyAcctgTransEntryFact(e2d3, organizationPartyId, "680000", BigDecimal.ZERO, new BigDecimal("5000"), BigDecimal.ZERO);

        OrderRepositoryInterface orderRepository = getOrderRepository(demopurch1);
        // verify the POs
        // find po1 order item1 and order item2.  these are orders for physical items
        Order purchOrder1 = orderRepository.getOrderById(po.getOrderId());
        OrderItem orderItemPo1_1 = orderRepository.getOrderItem(purchOrder1, "00001");
        OrderItem orderItemPo1_2 = orderRepository.getOrderItem(purchOrder1, "00002");
        verifyOrderItemEntryFact(orderItemPo1_1, organizationPartyId, "140000", BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("100000"));
        verifyOrderItemEntryFact(orderItemPo1_2, organizationPartyId, "140000", BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("50000"));
        // find po2 order item1.  this is a supplies item and should point to 650000
        Order purchOrder2 = orderRepository.getOrderById(po2.getOrderId());
        OrderItem orderItemPo2_1 = orderRepository.getOrderItem(purchOrder2, "00001");
        verifyOrderItemEntryFact(orderItemPo2_1, organizationPartyId, "650000", BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("50000"));

        // we use the sums to check the above accounting transactions and PO encumbrances
        // then we use the sums to check the results of receiving inventory and shipping products
        // we verify both aggregated and by accounting tag values

        // revenue
        verifyGlAcctTransEntryFactSums(organizationPartyId, "401000", null, new BigDecimal("300000"), BigDecimal.ZERO, BigDecimal.ZERO);

        // inventory
        verifyGlAcctTransEntryFactSums(organizationPartyId, "140000", null, BigDecimal.ZERO, new BigDecimal("72500"), new BigDecimal("150000"));
        verifyGlAcctTransEntryFactSums(organizationPartyId, "140000", UtilMisc.toMap("acctgTagEnumId1", "DIV_ENTERPRISE"), BigDecimal.ZERO, new BigDecimal("62500"), new BigDecimal("100000"));
        verifyGlAcctTransEntryFactSums(organizationPartyId, "140000", UtilMisc.toMap("acctgTagEnumId1", "DIV_CONSUMER"), BigDecimal.ZERO, new BigDecimal("10000"), new BigDecimal("50000"));

        // wages
        verifyGlAcctTransEntryFactSums(organizationPartyId, "601000", null, new BigDecimal("75000"), new BigDecimal("62000"), BigDecimal.ZERO);
        verifyGlAcctTransEntryFactSums(organizationPartyId, "601000", UtilMisc.toMap("acctgTagEnumId1", "DIV_CONSUMER"), new BigDecimal("60000"), new BigDecimal("50000"), BigDecimal.ZERO);
        verifyGlAcctTransEntryFactSums(organizationPartyId, "601000", UtilMisc.toMap("acctgTagEnumId1", "DIV_ENTERPRISE"), new BigDecimal("15000"), new BigDecimal("12000"), BigDecimal.ZERO);

        // cogs
        verifyGlAcctTransEntryFactSums(organizationPartyId, "500000", null, new BigDecimal("120000"), new BigDecimal("77500"), BigDecimal.ZERO);

        // tax expense
        verifyGlAcctTransEntryFactSums(organizationPartyId, "900000", null, new BigDecimal("26000"), new BigDecimal("4600"), BigDecimal.ZERO);
        // budget net amount is temporary changed from 100000.0 to 10000.0
        verifyGlAcctTransEntryFactSums(organizationPartyId, "900000", UtilMisc.toMap("acctgTagEnumId1", "DIV_ENTERPRISE"), new BigDecimal("10000"), new BigDecimal("6600"), BigDecimal.ZERO);
        verifyGlAcctTransEntryFactSums(organizationPartyId, "900000", UtilMisc.toMap("acctgTagEnumId1", "DIV_CONSUMER"), new BigDecimal("16000"), new BigDecimal("-2000"), BigDecimal.ZERO);

    }

    private AcctgTransEntry makeEntry(String organizationPartyId, String glAccountId, String flag, BigDecimal amount, String tag1) {
        AcctgTransEntry entry = new AcctgTransEntry();
        entry.setGlAccountId(glAccountId);
        entry.setOrganizationPartyId(organizationPartyId);
        entry.setDebitCreditFlag(flag);
        entry.setCurrencyUomId("USD");
        entry.setAmount(amount);
        entry.setAcctgTagEnumId1(tag1);
        return entry;
    }

    private AcctgTransEntry makeDebitEntry(String organizationPartyId, String glAccountId, BigDecimal amount, String tag1) {
        return makeEntry(organizationPartyId, glAccountId, "D", amount, tag1);
    }

    private AcctgTransEntry makeCreditEntry(String organizationPartyId, String glAccountId, BigDecimal amount, String tag1) {
        return makeEntry(organizationPartyId, glAccountId, "C", amount, tag1);
    }

    private PurchaseOrderFactory createDefaultPurchaseOrderFactory(String organizationPartyId, String orderName) throws GenericEntityException {
        PurchaseOrderFactory pof = new PurchaseOrderFactory(delegator, dispatcher, demopurch1, DEMO_SUPPLIER, organizationPartyId, FACILITY_CONTACT_MECH_ID);
        pof.setOrderName(orderName);
        pof.setCurrencyUomId("USD");
        pof.addPaymentMethod("EXT_OFFLINE");
        pof.addShippingGroup("UPS", "NEXT_DAY");
        return pof;
    }

    /**
     * This method verifies that the accounting transaction entry is recorded in correctly in the GlAccountTransEntryFact entity.
     * @param acctgTransEntry
     * @param organizationPartyId
     * @param glAccountId
     * @param netBudgetAmount
     * @param netActualAmount
     * @param netEncumberedAmount
     * @throws GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    private void verifyAcctgTransEntryFact(AcctgTransEntry acctgTransEntry, String organizationPartyId, String glAccountId, BigDecimal netBudgetAmount, BigDecimal netActualAmount, BigDecimal netEncumberedAmount) throws Exception {
        // find the GlAccountTransEntryFact for the acctgTransEntry and the organizationPartyId
        List<GenericValue> transEntryFacts = delegator.findByAnd("GlAccountTransEntryFact", UtilMisc.toMap("acctgTransId", acctgTransEntry.getAcctgTransId(), "acctgTransEntrySeqId", acctgTransEntry.getAcctgTransEntrySeqId(), "organizationPartyId", organizationPartyId));

        // verify that one and only one GlAccountTransEntryFact exists
        assertEquals("Transaction entry should be unique for GlAccountTransEntryFact entity", 1, transEntryFacts.size());
        GenericValue transEntryFact = EntityUtil.getFirst(transEntryFacts);

        // verify that the glAccountId, netBudgetAmount, netActualAmount, and netEncumberedAmount in GlAccountTransEntryFact are the same as the method parameters
        assertEquals("Wrong GL account ID", glAccountId, transEntryFact.getString("glAccountId"));
        assertEquals("Wrong budget net amount", netBudgetAmount, transEntryFact.getBigDecimal("budgetNetAmount"));
        assertEquals("Wrong actual net amount", netActualAmount, transEntryFact.getBigDecimal("actualNetAmount"));
        assertEquals("Wrong encumbered net amount", netEncumberedAmount, transEntryFact.getBigDecimal("encumberedNetAmount"));

        // verify that the accounting tags for GlAccountTransEntryFact are the same as those for the acctgTransEntry
        assertAccountingTagsEqual(acctgTransEntry, transEntryFact.getAllFields());
    }

    /**
     * This method verifies that the order item is recorded in correctly in the GlAccountTransEntryFact entity.
     * @param orderItem
     * @param organizationPartyId
     * @param glAccountId
     * @param netBudgetAmount
     * @param netActualAmount
     * @param netEncumberedAmount
     * @throws Exception if an error occurs
     */
    @SuppressWarnings("unchecked")
    private void verifyOrderItemEntryFact(OrderItem orderItem, String organizationPartyId, String glAccountId, BigDecimal netBudgetAmount, BigDecimal netActualAmount, BigDecimal netEncumberedAmount) throws Exception {
        // find the GlAccountTransEntryFact for the orderItem and the organizationPartyId
        List<GenericValue> orderItemFacts = delegator.findByAnd("GlAccountTransEntryFact", UtilMisc.toMap("orderId", orderItem.getOrderId(), "orderItemSeqId", orderItem.getOrderItemSeqId(), "organizationPartyId", organizationPartyId));

        // verify that one and only one GlAccountTransEntryFact exists
        assertEquals("Order item should be unique for GlAccountTransEntryFact entity", 1, orderItemFacts.size());
        GenericValue orderItemFact = EntityUtil.getFirst(orderItemFacts);

        // verify that the glAccountId, netBudgetAmount, netActualAmount, and netEncumberedAmount in GlAccountTransEntryFact are the same as the method parameters
        assertEquals("Wrong GL account ID", glAccountId, orderItemFact.getString("glAccountId"));
        assertEquals("Wrong budget net amount", netBudgetAmount, orderItemFact.getBigDecimal("budgetNetAmount"));
        assertEquals("Wrong actual net amount", netActualAmount, orderItemFact.getBigDecimal("actualNetAmount"));
        assertEquals("Wrong encumbered net amount", netEncumberedAmount, orderItemFact.getBigDecimal("encumberedNetAmount"));

        // verify that the accounting tags for GlAccountTransEntryFact are the same as those for the acctgTransEntry
        assertAccountingTagsEqual(orderItem, orderItemFact.getAllFields());
    }

    /**
     * This method verifies the sum of all the GlAccountTransEntryFact.
     * @param organizationPartyId
     * @param glAccountId
     * @param accountingTags if null then the method checks for any accounting tags
     * @param netBudgetAmount
     * @param netActualAmount
     * @param netEncumberedAmount
     * @throws Exception if an error occurs
     */
    private void verifyGlAcctTransEntryFactSums(String organizationPartyId, String glAccountId, Map accountingTags, BigDecimal netBudgetAmount, BigDecimal netActualAmount, BigDecimal netEncumberedAmount) throws Exception {
        // find all GlAccountTransEntryFact for organizationPartyId and glAccountId
        Map<String, Object> conditions = UtilMisc.<String, Object>toMap("glAccountId", glAccountId, "organizationPartyId", organizationPartyId);
        if (accountingTags != null) {
            conditions.putAll(accountingTags);
        }
        List<GenericValue> facts = delegator.findByAnd("GlAccountTransEntryFact", conditions);

        // calculate sum of netBudgetAmount, netActualAmount, and netEncumberedAmount for all GlAccountTransEntryFact
        BigDecimal budgetNetAmountSum = BigDecimal.ZERO;
        BigDecimal actualNetAmountSum = BigDecimal.ZERO;
        BigDecimal encumberedNetAmountSum = BigDecimal.ZERO;
        for (GenericValue fact : facts) {
            budgetNetAmountSum = budgetNetAmountSum.add(fact.getBigDecimal("budgetNetAmount"));
            actualNetAmountSum = actualNetAmountSum.add(fact.getBigDecimal("actualNetAmount"));
            encumberedNetAmountSum = encumberedNetAmountSum.add(fact.getBigDecimal("encumberedNetAmount"));
        }

        // verify the sums are the same as the parameters
        assertEquals("Wrong budget net amount total for account [" + glAccountId + "]", netBudgetAmount, budgetNetAmountSum);
        assertEquals("Wrong actual net amounttotal for account [" + glAccountId + "]", netActualAmount, actualNetAmountSum);
        assertEquals("Wrong encumbered net amount total for account [" + glAccountId + "]", netEncumberedAmount, encumberedNetAmountSum);
    }
}
