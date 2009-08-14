/*
 * Copyright (c) 2009 Open Source Strategies, Inc.
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
 * 643 Blair Island Road, Suite 305 - Redwood City, CA 94063, USA
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
import org.opentaps.domain.base.entities.AcctgTransEntry;
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
        createMainSupplierForProduct(physicalProductId, DEMO_SUPPLIER, 100.0, "USD", 0.0, admin);

        GenericValue supplyProduct = createTestProduct("Suppliy product for testComplexEncumbranceProcess", "SUPPLIES", admin);
        String supplyProductId = supplyProduct.getString("productId");
        createMainSupplierForProduct(supplyProductId, DEMO_SUPPLIER, 200.0, "USD", 0.0, admin);

        // this is used to a CREATED PO
        // Create a PO #1 for 10 physical item @ $100 each and 1 supplies item for $200 with division tag=ENTERPRISE
        PurchaseOrderFactory pof1 = createDefaultPurchaseOrderFactory(organizationPartyId, "PO1 for testComplexEncumbranceProcess tests");
        pof1.addProduct(physicalProduct, "PRODUCT_ORDER_ITEM", 10.0, "00001", UtilMisc.toMap("price", Double.valueOf("100.0")), UtilMisc.toMap("acctgTagEnumId1", "DIV_ENTERPRISE"));
        pof1.addProduct(supplyProduct, "SUPPLIES_ORDER_ITEM", 1.0, "00001", UtilMisc.toMap("price", Double.valueOf("200.0")), UtilMisc.toMap("acctgTagEnumId1", "DIV_ENTERPRISE"));
        pof1.storeOrder();

        // this is used to test a CANCELLED PO
        // Create a PO #2 for 5 physical item @ $100 each with division tag=ENTERPRISE
        PurchaseOrderFactory pof2 = createDefaultPurchaseOrderFactory(organizationPartyId, "PO2 for testComplexEncumbranceProcess tests");
        pof2.addProduct(physicalProduct, "PRODUCT_ORDER_ITEM", 5.0, "00001", UtilMisc.toMap("price", Double.valueOf("100.0")), UtilMisc.toMap("acctgTagEnumId1", "DIV_ENTERPRISE"));
        pof2.storeOrder();
        // Cancel PO #2
        pof2.cancelOrder();

        // this is used to test a COMPLETED PO
        // Create a PO #3 for 7 physical item @ $300 each with division tag=CONSUMER
        PurchaseOrderFactory pof3 = createDefaultPurchaseOrderFactory(organizationPartyId, "PO3 for testComplexEncumbranceProcess tests");
        pof3.addProduct(physicalProduct, "PRODUCT_ORDER_ITEM", 7.0, "00001", UtilMisc.toMap("price", Double.valueOf("300.0")), UtilMisc.toMap("acctgTagEnumId1", "DIV_CONSUMER"));
        pof3.storeOrder();
        // Add a shipping charge order adjustment of $19.95
        runAndAssertServiceSuccess("createOrderAdjustment", UtilMisc.toMap("orderId", pof3.getOrderId(), "orderAdjustmentTypeId", "SHIPPING_CHARGES", "amount", new Double(19.95), "userLogin", demopurch1));
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
        pof4.addProduct(supplyProduct, "SUPPLIES_ORDER_ITEM", 3.0, "00001", UtilMisc.toMap("price", Double.valueOf("500.0")), UtilMisc.toMap("acctgTagEnumId1", "DIV_SMALL_BIZ"));
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
        pof5.addProduct(physicalProduct, "PRODUCT_ORDER_ITEM", 1000.0, "00001", UtilMisc.toMap("price", Double.valueOf("10.0")), UtilMisc.toMap("acctgTagEnumId1", "DIV_GOV"));
        pof5.addProduct(supplyProduct, "SUPPLIES_ORDER_ITEM", 1000.0, "00001", UtilMisc.toMap("price", Double.valueOf("1.5")), UtilMisc.toMap("acctgTagEnumId1", "DIV_SMALL_BIZ"));
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
        runAndAssertServiceSuccess("createOrderAdjustment", UtilMisc.toMap("orderId", orderId5, "orderAdjustmentTypeId", "SHIPPING_CHARGES", "amount", Double.valueOf(19.95), "userLogin", demopurch1));
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
        pof6.addProduct(physicalProduct, "PRODUCT_ORDER_ITEM", 5000.0, "00001", UtilMisc.toMap("price", Double.valueOf("9.0")), UtilMisc.toMap("acctgTagEnumId1", "DIV_CONSUMER"));
        pof6.storeOrder();
        pof6.approveOrder();
        String orderId6 = pof6.getOrderId();
        // Cancel 2000 of the physical item--ie cancelQuantity=2000
        orderItems = delegator.findByAnd("OrderItem", UtilMisc.toMap("orderId", orderId6, "productId", physicalProductId));
        cancelOrderItem(orderId6, (String) (EntityUtil.getFirst(orderItems).get("orderItemSeqId")), "00001", 2000.0, demopurch1);
        // Add shipping charges of $838.43
        runAndAssertServiceSuccess("createOrderAdjustment", UtilMisc.toMap("orderId", pof6.getOrderId(), "orderAdjustmentTypeId", "SHIPPING_CHARGES", "amount", Double.valueOf(838.43), "userLogin", demopurch1));
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
        assertEquals("Wrong encumbered value for all accounting tags", BigDecimal.valueOf(51788.43), encumberedValue);

        // for all tag=CONSUMER, verify value is $33000
        encumberedValue = encumbranceMethods.getTotalEncumberedValue(organizationPartyId, UtilMisc.toMap("acctgTagEnumId1", "DIV_CONSUMER"), moment);
        assertEquals("Wrong encumbered value for CUSTOMER tag", BigDecimal.valueOf(33000.0), encumberedValue);

        // for all tag=ENTERPRISE, verify value is $5200
        encumberedValue = encumbranceMethods.getTotalEncumberedValue(organizationPartyId, UtilMisc.toMap("acctgTagEnumId1", "DIV_ENTERPRISE"), moment);
        assertEquals("Wrong encumbered value for ENTERPRISE tag", BigDecimal.valueOf(5200.0), encumberedValue);

        // for all tag=GOVERNMENT, verify value is $12000
        encumberedValue = encumbranceMethods.getTotalEncumberedValue(organizationPartyId, UtilMisc.toMap("acctgTagEnumId1", "DIV_GOV"), moment);
        assertEquals("Wrong encumbered value for GOVERNMENT tag", BigDecimal.valueOf(12000.0), encumberedValue);

        // for all tag=SMALL_BUSINESS, verify value is $750
        encumberedValue = encumbranceMethods.getTotalEncumberedValue(organizationPartyId, UtilMisc.toMap("acctgTagEnumId1", "DIV_SMALL_BIZ"), moment);
        assertEquals("Wrong encumbered value for SMALL_BUSINESS tag", BigDecimal.valueOf(750.0), encumberedValue);

        // for all tag=EDUCATION, verify value is 0
        encumberedValue = encumbranceMethods.getTotalEncumberedValue(organizationPartyId, UtilMisc.toMap("acctgTagEnumId1", "DIV_EDU"), moment);
        assertEquals("Wrong encumbered value for EDUCATION tag", BigDecimal.ZERO, encumberedValue);

        // for all tag=untagged, verify value is $838.43
        encumberedValue = encumbranceMethods.getTotalEncumberedValue(organizationPartyId, UtilMisc.toMap("acctgTagEnumId1", "NULL_TAG"), moment);
        assertEquals("Wrong encumbered value for untagged case", BigDecimal.valueOf(838.43), encumberedValue);

    }

    /**
     * This test is for verifying the features of the income/budget/encumbrances balances report.
     * @throws GeneralException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public void testIncomeBudgetEncumbrancesBalancesReport() throws GeneralException {
        // make a copy of "Company" with all its accounting tags.  all subsequent transactions will
        // use this organizationPartyId
        organizationPartyId = createOrganizationFromTemplate("Company", "Income Budget Encumbrances Balances Report Organization");

        // create 2 test products.  In this test the focus is on the report, so a finished good is sufficient.
        GenericValue physicalProduct = createTestProduct("First test product for testIncomeBudgetEncumbrancesBalancesReport", "FINISHED_GOOD", admin);
        String physicalProductId = physicalProduct.getString("productId");
        createMainSupplierForProduct(physicalProductId, DEMO_SUPPLIER, 100.0, "USD", 0.0, admin);
        assignDefaultPrice(physicalProduct, 100.0, admin);

        GenericValue physicalProduct2 = createTestProduct("Second test product for testIncomeBudgetEncumbrancesBalancesReport", "FINISHED_GOOD", admin);
        String physicalProductId2 = physicalProduct2.getString("productId");
        createMainSupplierForProduct(physicalProductId2, DEMO_SUPPLIER, 100.0, "USD", 0.0, admin);
        assignDefaultPrice(physicalProduct2, 25.0, admin);

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
        // Debit glAccountId=336000  15000
        AcctgTransEntry b1d6 = makeDebitEntry(organizationPartyId, "336000", new BigDecimal("15000"), "DIV_ENTERPRISE");
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
        // Debit glAccountId=336000 24000
        AcctgTransEntry b2d6 = makeDebitEntry(organizationPartyId, "336000", new BigDecimal("24000"), "DIV_CONSUMER");
        b2d6.setAcctgTagEnumId2("DPT_MANUFACTURING"); // adding another tag, it should not appear in the report
        // post this accounting transaction
        ledgerMethods.storeAcctgTransAndEntries(b2, Arrays.asList(b2c1, b2d1, b2d2, b2d3, b2d4, b2d5, b2d6));

        // these transactions create a real income statement

        // receive 2000 of the physicalProduct at 50, tag DIV_ENTERPRISE
        Map input = UtilMisc.toMap("userLogin", admin);
        input.put("facilityId", "WebStoreWarehouse");
        input.put("productId", physicalProductId);
        input.put("inventoryItemTypeId", "NON_SERIAL_INV_ITEM");
        input.put("quantityAccepted", 2000.0);
        input.put("quantityRejected", 0.0);
        input.put("datetimeReceived", UtilDateTime.nowTimestamp());
        input.put("unitCost", 50.00);
        input.put("ownerPartyId", organizationPartyId); // this is important -- the inventory must be owned by the organization of the test to show up as COGS
        input.put("acctgTagEnumId1", "DIV_ENTERPRISE");
        input.put("acctgTagEnumId2", "DPT_CORPORATE"); // adding another tag, it should not appear in the report
        runAndAssertServiceSuccess("receiveInventoryProduct", input);
        // receive 5000 of the physicalProduct2 at 10, tag DIV_CONSUMER
        input = UtilMisc.toMap("userLogin", admin);
        input.put("facilityId", "WebStoreWarehouse");
        input.put("productId", physicalProductId2);
        input.put("inventoryItemTypeId", "NON_SERIAL_INV_ITEM");
        input.put("quantityAccepted", 5000.0);
        input.put("quantityRejected", 0.0);
        input.put("datetimeReceived", UtilDateTime.nowTimestamp());
        input.put("unitCost", 10.00);
        input.put("ownerPartyId", organizationPartyId);
        input.put("acctgTagEnumId1", "DIV_CONSUMER");
        input.put("acctgTagEnumId2", "DPT_MANUFACTURING"); // adding another tag, it should not appear in the report
        runAndAssertServiceSuccess("receiveInventoryProduct", input);

        final String productStoreId = "9000";

        // copy a customer from DemoAccount1
        String customerPartyId = createPartyFromTemplate("DemoAccount1", "Customer for testIncomeBudgetEncumbrancesBalancesReport");
        // create a sales order for 750 of physicalProduct at $100, tag DIV_ENTERPRISE
        SalesOrderFactory salesOrder = new SalesOrderFactory(delegator, dispatcher, admin, organizationPartyId, customerPartyId, productStoreId);
        salesOrder.addProduct(physicalProduct, "PRODUCT_ORDER_ITEM", 750.0, salesOrder.getFirstShipGroup(), UtilMisc.toMap("price", 100.0, "listPrice", 100.0), UtilMisc.toMap("acctgTagEnumId1", "DIV_ENTERPRISE"));
        // approve sales order and ship it
        salesOrder.approveOrder();
        runAndAssertServiceSuccess("testShipOrder", UtilMisc.toMap("orderId", salesOrder.getOrderId(), "facilityId", "WebStoreWarehouse", "userLogin", admin));

        // create a sales order for 4000 of physicalProduct2 at $25, tag DIV_CONSUMER
        salesOrder = new SalesOrderFactory(delegator, dispatcher, admin, organizationPartyId, customerPartyId, productStoreId);
        salesOrder.addProduct(physicalProduct2, "PRODUCT_ORDER_ITEM", 4000.0, salesOrder.getFirstShipGroup(), UtilMisc.toMap("price", 25.0, "listPrice", 25.0), UtilMisc.toMap("acctgTagEnumId1", "DIV_CONSUMER"));
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

        // Create a PO for 2000 of physicalProduct at 50 tag DIV_ENTERPRISE and 10000 of physicalProduct2 at 10 tag DIV_CONSUMER
        PurchaseOrderFactory po = createDefaultPurchaseOrderFactory(organizationPartyId, "PO for testIncomeBudgetEncumbrancesBalancesReport");
        po.addProduct(physicalProduct, "PRODUCT_ORDER_ITEM", 2000.0, "00001", UtilMisc.toMap("price", Double.valueOf("50.0")), UtilMisc.toMap("acctgTagEnumId1", "DIV_ENTERPRISE"));
        po.addProduct(physicalProduct2, "PRODUCT_ORDER_ITEM", 10000.0, "00001", UtilMisc.toMap("price", Double.valueOf("10.0")), UtilMisc.toMap("acctgTagEnumId1", "DIV_CONSUMER"));
        po.storeOrder();
        po.approveOrder();

        // Run the createEncumbranceSnapshotAndDetail service
        runAndAssertServiceSuccess("createEncumbranceSnapshotAndDetail", UtilMisc.toMap("organizationPartyId", organizationPartyId, "userLogin", demofinadmin));

        // run the balances report.
        Map results = runAndAssertServiceSuccess("balanceStatementReport", UtilMisc.toMap("userLogin", admin, "organizationPartyId", organizationPartyId, "fromDate", new Timestamp(timestamp.getTime() - 10000), "thruDate", new Timestamp(UtilDateTime.nowTimestamp().getTime() + 10000)));

        // check the results:
        // for CONSUMER tag: Budget=24000 Income=100000 Expense=103000 Liens=100000 Balance=Budget + Income - Expense - Liens = -79000
        // for ENTERPRISE tag: Budget=15000 Income=75000 Expense=65100 Liens=100000 Balance= -75100
        List<Map<String, Object>> data = (List<Map<String, Object>>) results.get("reportData");
        for (Map<String, Object> line : data) {
            Debug.logInfo("balanceStatementReport line: " + line, MODULE);
            String tag1 = (String) line.get("acctgTagEnumId1");
            // note, the enum codes are in the report data instead of the enum id
            if ("CONSUMER".equals(tag1)) {
                assertEquals("Budget value incorrect for tag CONSUMER.", new BigDecimal("24000"), (BigDecimal) line.get("budget"));
                assertEquals("Income value incorrect for tag CONSUMER.", new BigDecimal("100000"), (BigDecimal) line.get("income"));
                assertEquals("Expense value incorrect for tag CONSUMER.", new BigDecimal("103000"), (BigDecimal) line.get("expense"));
                assertEquals("Liens value incorrect for tag CONSUMER.", new BigDecimal("100000"), (BigDecimal) line.get("liens"));
                assertEquals("Balance value incorrect for tag CONSUMER.", new BigDecimal("-79000"), (BigDecimal) line.get("balance"));
            } else if ("ENTERPRISE".equals(tag1)) {
                assertEquals("Budget value incorrect for tag ENTERPRISE.", new BigDecimal("15000"), (BigDecimal) line.get("budget"));
                assertEquals("Income value incorrect for tag ENTERPRISE.", new BigDecimal("75000"), (BigDecimal) line.get("income"));
                assertEquals("Expense value incorrect for tag ENTERPRISE.", new BigDecimal("65100"), (BigDecimal) line.get("expense"));
                assertEquals("Liens value incorrect for tag ENTERPRISE.", new BigDecimal("100000"), (BigDecimal) line.get("liens"));
                assertEquals("Balance value incorrect for tag ENTERPRISE.", new BigDecimal("-75100"), (BigDecimal) line.get("balance"));
            } else {
                fail("Unexpected tag in report data: " + tag1);
            }
            // check other tags are empty on each line
            for (int i = 2; i <= UtilAccountingTags.TAG_COUNT; i++) {
                assertNull("The report line should not have a tag at index " + i, line.get("acctgTagEnumId" + i));
            }
        }

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
}
