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
package org.opentaps.tests.dataimport;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.base.entities.AcctgTrans;
import org.opentaps.base.entities.AcctgTransEntry;
import org.opentaps.base.entities.DataImportCustomer;
import org.opentaps.base.entities.DataImportGlAccount;
import org.opentaps.base.entities.DataImportInventory;
import org.opentaps.base.entities.DataImportProduct;
import org.opentaps.base.entities.DataImportSupplier;
import org.opentaps.base.entities.GlAccount;
import org.opentaps.base.entities.GlAccountOrganization;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.domain.dataimport.AccountingDataImportRepositoryInterface;
import org.opentaps.domain.dataimport.DataImportDomainInterface;
import org.opentaps.domain.dataimport.GlAccountImportServiceInterface;
import org.opentaps.domain.dataimport.PartyDataImportRepositoryInterface;
import org.opentaps.domain.dataimport.ProductDataImportRepositoryInterface;
import org.opentaps.domain.ledger.AccountingTransaction;
import org.opentaps.domain.ledger.LedgerDomainInterface;
import org.opentaps.domain.ledger.LedgerRepositoryInterface;
import org.opentaps.domain.ledger.LedgerServiceInterface;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.tests.OpentapsTestCase;
import org.opentaps.tests.crmsfa.orders.OrderTests;
import org.opentaps.tests.financials.AgreementTests;
import org.opentaps.tests.financials.FinancialAsserts;

/**
 * Tests for the data import component.
 */
public class DataImportTests extends OpentapsTestCase {

    private static final String MODULE = DataImportTests.class.getName();

    private GenericValue demofinadmin       = null;
    private GenericValue DemoSalesManager   = null;
    private GenericValue demowarehouse1     = null;
    private GenericValue demopurch1         = null;
    private GenericValue product            = null;

    private String organizationPartyId          = "Company";
    private static final String productStoreId  = "9000";
    private static final String productId       = "GZ-1005";
    private static final String decimalDigitsId = "1234567890";


    @Override
    public void setUp() throws Exception {
        super.setUp();
        demofinadmin        = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "demofinadmin"));
        DemoSalesManager    = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesManager"));
        demowarehouse1      = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "demowarehouse1"));
        demopurch1          = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "demopurch1"));
        product             = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId));
        User                = DemoSalesManager;
    }

    @Override
    public void tearDown() throws Exception {
        // obtain domains and repositories which is needed to remove records
        DomainsLoader domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(this.admin));
        DataImportDomainInterface imp_domain = domainLoader.loadDomainsDirectory().getDataImportDomain();
        ProductDataImportRepositoryInterface product_repos = imp_domain.getProductDataImportRepository();
        PartyDataImportRepositoryInterface party_repos = imp_domain.getPartyDataImportRepository();
        AccountingDataImportRepositoryInterface acc_repos = imp_domain.getAccountingDataImportRepository();

        // remove all DataImportProduct, _Supplier, _Customer, _GlAccount, _Inventory entities whose primary keys begin with excel
        String id_suffix = "excel%";
        product_repos.remove(product_repos.findList(DataImportProduct.class, EntityCondition.makeCondition(DataImportProduct.Fields.productId.name(), EntityOperator.LIKE, id_suffix)));
        product_repos.remove(product_repos.findList(DataImportInventory.class, EntityCondition.makeCondition(DataImportInventory.Fields.itemId.name(), EntityOperator.LIKE, id_suffix)));
        party_repos.remove(party_repos.findList(DataImportSupplier.class, EntityCondition.makeCondition(DataImportSupplier.Fields.supplierId.name(), EntityOperator.LIKE, id_suffix)));
        party_repos.remove(party_repos.findList(DataImportCustomer.class, EntityCondition.makeCondition(DataImportCustomer.Fields.customerId.name(), EntityOperator.LIKE, id_suffix)));
        acc_repos.remove(acc_repos.findList(DataImportGlAccount.class, EntityCondition.makeCondition(DataImportGlAccount.Fields.glAccountId.name(), EntityOperator.LIKE, id_suffix)));
        // remove all DataImportProduct, _Supplier, _Customer, _GlAccount, _Inventory entities whose primary keys equals to decimalDigitsId
        product_repos.remove(product_repos.findOne(DataImportProduct.class, product_repos.map(DataImportProduct.Fields.productId, decimalDigitsId)));
        product_repos.remove(product_repos.findOne(DataImportInventory.class, product_repos.map(DataImportInventory.Fields.itemId, decimalDigitsId)));
        party_repos.remove(party_repos.findOne(DataImportSupplier.class, party_repos.map(DataImportSupplier.Fields.supplierId, decimalDigitsId)));
        party_repos.remove(party_repos.findOne(DataImportCustomer.class, party_repos.map(DataImportCustomer.Fields.customerId, decimalDigitsId)));
        acc_repos.remove(acc_repos.findOne(DataImportGlAccount.class, acc_repos.map(DataImportGlAccount.Fields.glAccountId, decimalDigitsId)));
        //end of remove

        super.tearDown();
        demofinadmin        = null;
        DemoSalesManager    = null;
        demowarehouse1      = null;
        demopurch1          = null;
        product             = null;


    }

    /**
     * Performs import of customers, prepares data for rest of the tests.
     */
    public void testImportCustomers() {
        // run import of customers
        runAndAssertServiceSuccess("importCustomers", UtilMisc.toMap("initialResponsiblePartyId", DemoSalesManager.getString("userLoginId"), "organizationPartyId", organizationPartyId, "userLogin", User));
    }

    /**
     * Shortened version of credit limit test. Its purpose is verify if credit limit feature
     * works for some imported customer.
     * @exception Exception if an error occurs
     */
    public void testImportedCustomerCreditLimit() throws Exception  {

        AgreementTests agreementUnitTests = new AgreementTests();
        agreementUnitTests.setUp();

        List<GenericValue> candidateParties =  delegator.findByAnd("PartyGroup", UtilMisc.toMap("groupName", "Import Customer for Automated Testing (keep this name unique)"));
        String partyId = null;
        if (UtilValidate.isNotEmpty(candidateParties)) {
            partyId = EntityUtil.getFirst(candidateParties).getString("partyId");
        }

        agreementUnitTests.performCreditLimitTest(partyId);
    }

    /**
     * The method verifies if sales agreement & net payment days term created and work correctly
     * for imported customer.
     * @exception Exception if an error occurs
     */
    public void testImportedCustomerSalesAgreementNetDaysToParty() throws Exception {

        AgreementTests agreementUnitTests = new AgreementTests();
        agreementUnitTests.setUp();

        List<GenericValue> candidateParties =  delegator.findByAnd("PartyGroup", UtilMisc.toMap("groupName", "Import Customer for Automated Testing (keep this name unique)"));
        String partyId = null;
        if (UtilValidate.isNotEmpty(candidateParties)) {
            partyId = EntityUtil.getFirst(candidateParties).getString("partyId");
        }

        GenericValue customer = delegator.findByPrimaryKey("DataImportCustomer", UtilMisc.toMap("customerId", "9007"));
        Long netPaymentDays = customer.getLong("netPaymentDays");
        agreementUnitTests.performSalesAgreementNetDaysToPartyTest(partyId, netPaymentDays.intValue());

    }

    /**
     * Test impossibility of shipment for imported customer in don't ship classification.
     * @exception Exception if an error occurs
     */
    public void testImportedDontShipCustomer() throws Exception {

        OrderTests orderTests = new OrderTests();
        orderTests.setUp();

        // find a customer with disabled shipping
        List<GenericValue> dontshipParties = delegator.findByAnd("PartyClassification", UtilMisc.toMap("partyClassificationGroupId", "DONOTSHIP_CUSTOMERS"));
        List<GenericValue> candidateParties = delegator.findByCondition("PartyGroup", EntityCondition.makeCondition(EntityOperator.AND, EntityCondition.makeCondition("groupName", EntityOperator.EQUALS, " "), EntityCondition.makeCondition("partyId", EntityOperator.IN, EntityUtil.getFieldListFromEntityList(dontshipParties, "partyId", true))), null, null);
        GenericValue customer = null;
        if (UtilValidate.isNotEmpty(candidateParties)) {
            customer = EntityUtil.getFirst(candidateParties);
        }

        // run test
        orderTests.performDoNotShipTest(customer.getString("partyId"));

    }

    /**
     * Import Commissions Tests
     * 1.  Run importCustomers
     * 2.  Run importCustomerCommissions
     * 3.  For each primaryPartyId of customerIds 9005, 9006, 9007
     * 3a.  Create sales invoice and set it ready
     * 3b.  Verify that a sales commission invoice from primaryPartyId of customerId 9010 to Company is created for 10% of the value of sales invoice in 3a
     * @exception Exception if an error occurs
     */
    public void testImportCustomersCommissions() throws Exception {
        // 1.  Run importCustomers, in case they have not been
        runAndAssertServiceSuccess("importCustomers", UtilMisc.toMap("initialResponsiblePartyId", DemoSalesManager.getString("userLoginId"), "organizationPartyId", organizationPartyId, "userLogin", User));

        // 2.  Run importCustomerCommissions
        // Importing services should check the importedRecords, because they always return success
        // here, we just check that non-zero records were imported, so we don't have to keep modifying this test as the data changes
        Map<String, Object> results = dispatcher.runSync("importCustomersCommissions", UtilMisc.toMap("userLogin", demofinadmin, "organizationPartyId", organizationPartyId));
        assertTrue("importCustomersCommissions did not run successfully", UtilCommon.isSuccess(results));
        assertNotEquals("importCustomersCommissions did not import any records", new BigDecimal((Integer) results.get("importedRecords")), BigDecimal.ZERO);

        // 3.  For each primaryPartyId of customerIds 9005, 9006, 9007
        FinancialAsserts fa = new FinancialAsserts(this, organizationPartyId, demofinadmin);
        String[] customerIds = {"9005", "9006", "9007"};
        GenericValue commissionBroker = delegator.findByPrimaryKey("DataImportCustomer", UtilMisc.toMap("customerId", "9010"));

        for (String customerId : customerIds) {
            GenericValue dataImportCustomer = delegator.findByPrimaryKey("DataImportCustomer", UtilMisc.toMap("customerId", customerId));
            // 3a.  Create sales invoice and set it oready
            String invoiceId = fa.createInvoice(dataImportCustomer.getString("primaryPartyId"), org.opentaps.base.constants.InvoiceTypeConstants.SALES_INVOICE, UtilDateTime.nowTimestamp());
            fa.createInvoiceItem(invoiceId, org.opentaps.base.constants.InvoiceItemTypeConstants.INV_FPROD_ITEM, "WG-1111", new BigDecimal("1.0"), new BigDecimal("100.0"));
            fa.updateInvoiceStatus(invoiceId, org.opentaps.base.constants.StatusItemConstants.InvoiceStatus.INVOICE_READY);

            // 3b.  Verify that a sales commission invoice from primaryPartyId of customerId 9010 to Company is created for 10% of the value of sales invoice in 3a
            List<GenericValue> commissionInvoiceItems = delegator.findByAnd("InvoiceItem", UtilMisc.toMap("parentInvoiceId", invoiceId));
            assertNotNull("There is no commission invoice item for party [" + commissionBroker.getString("primaryPartyId") + "] and invoice [" + invoiceId + "]", commissionInvoiceItems);
            assertEquals("Incorrect number of commission invoice items for party [" + commissionBroker.getString("primaryPartyId") + "] and parent invoice [" + invoiceId + "] of customer party[" + dataImportCustomer.getString("primaryPartyId") + "] imported from DataImportCustomer customerId [" + customerId + "]", 1, commissionInvoiceItems.size());
            GenericValue commissionInvoiceItem = EntityUtil.getFirst(commissionInvoiceItems);
            assertEquals("Commission invoice item [" + commissionInvoiceItem.getString("invoiceId") + "] for party [" + commissionBroker.getString("primaryPartyId") + "] and invoice [" + invoiceId + "] is not on the right product", "WG-1111", commissionInvoiceItem.getString("productId"));
            assertEquals("Commission invoice item [" + commissionInvoiceItem.getString("invoiceId") + "] for party [" + commissionBroker.getString("primaryPartyId") + "] and invoice [" + invoiceId + "] has not the right quantity", 1.0, commissionInvoiceItem.getDouble("quantity"));
            assertEquals("Commission invoice item [" + commissionInvoiceItem.getString("invoiceId") + "] for party [" + commissionBroker.getString("primaryPartyId") + "] and invoice [" + invoiceId + "] has not on the amount", 10.0, commissionInvoiceItem.getDouble("amount"));

            GenericValue commissionInvoice = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", commissionInvoiceItem.getString("invoiceId")));
            assertEquals("Commission invoice [" + commissionInvoice.getString("invoiceId") + "] for party [" + commissionBroker.getString("primaryPartyId") + "] and invoice [" + invoiceId + "] is not an commission invoice", "COMMISSION_INVOICE", commissionInvoice.getString("invoiceTypeId"));
            assertEquals("Commission invoice [" + commissionInvoice.getString("invoiceId") + "] for party [" + commissionBroker.getString("primaryPartyId") + "] and invoice [" + invoiceId + "] has not the right role", "COMMISSION_AGENT", commissionInvoice.getString("roleTypeId"));
            assertEquals("Commission invoice [" + commissionInvoice.getString("invoiceId") + "] for party [" + commissionBroker.getString("primaryPartyId") + "] and invoice [" + invoiceId + "] is not for the right party", organizationPartyId, commissionInvoice.getString("partyId"));
            assertEquals("Commission invoice [" + commissionInvoice.getString("invoiceId") + "] for party [" + commissionBroker.getString("primaryPartyId") + "] and invoice [" + invoiceId + "] has not the right party from", commissionBroker.getString("primaryPartyId"), commissionInvoice.getString("partyIdFrom"));

        }

    }

    /**
     * Verify that the importGlAccounts can successfully import GL accounts so that transactions can be posted to them.
     * 1. Verify importGlAccounts service success.
     * 2. Run service postAcctgTrans and verify success.
     * @exception Exception if an error occurs
     */
    public void testImportGlAccount() throws Exception {
        //obtain domains and repositories which is needed in the test
        DomainsLoader domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(this.admin));
        DataImportDomainInterface imp_domain = domainLoader.loadDomainsDirectory().getDataImportDomain();
        AccountingDataImportRepositoryInterface imp_repos = imp_domain.getAccountingDataImportRepository();
        LedgerDomainInterface ledger_domain = domainLoader.loadDomainsDirectory().getLedgerDomain();
        LedgerRepositoryInterface leger_repos = ledger_domain.getLedgerRepository();

        // 1a. use delegator.getNextSeqId to get 3 glAccountIds, gl1, gl2, gl3

        String gl1 = delegator.getNextSeqId("GlAccount");
        String gl2 = Integer.valueOf(Integer.valueOf(gl1).intValue() + 1).toString();
        String gl3 = Integer.valueOf(Integer.valueOf(gl2).intValue() + 1).toString();

        // 1b. create DataImportGlAccount values for gl1, gl2, gl3, with gl1 being the parent of gl2, with classifications:
        //   gl1 - CURRENT_ASSET
        //   gl2 - ACCOUNTS_RECEIVABLE
        //   gl3 - ACCOUNTS_PAYABLE

        DataImportGlAccount dataImportGlAccount1 = new DataImportGlAccount();
        dataImportGlAccount1.setGlAccountId(gl1);
        dataImportGlAccount1.setClassification(org.opentaps.base.constants.GlAccountClassTypeMapConstants.CURRENT_ASSET);
        imp_repos.createOrUpdate(dataImportGlAccount1);

        DataImportGlAccount dataImportGlAccount2 = new DataImportGlAccount();
        dataImportGlAccount2.setGlAccountId(gl2);
        dataImportGlAccount2.setParentGlAccountId(gl1);
        dataImportGlAccount2.setClassification(org.opentaps.base.constants.GlAccountClassTypeMapConstants.ACCOUNTS_RECEIVABLE);
        imp_repos.createOrUpdate(dataImportGlAccount2);

        DataImportGlAccount dataImportGlAccount3 = new DataImportGlAccount();
        dataImportGlAccount3.setGlAccountId(gl3);
        dataImportGlAccount3.setClassification(org.opentaps.base.constants.GlAccountClassTypeMapConstants.ACCOUNTS_PAYABLE);
        imp_repos.createOrUpdate(dataImportGlAccount3);

        // 1c. run the importGlAccounts test with organizationPartyId=Company, verify that it's successful

        GlAccountImportServiceInterface glAccountImportService = imp_domain.getGlAccountImportService();
        glAccountImportService.setOrganizationPartyId(organizationPartyId);
        glAccountImportService.importGlAccounts();

        Integer imported = glAccountImportService.getImportedRecords();
        assertTrue("Imported GL accounts (" + imported + ") is less than 2, should be at least 3.", imported.compareTo(Integer.valueOf(2)) > 0);
        GlAccount glAccount1 = leger_repos.findOne(GlAccount.class, leger_repos.map(GlAccount.Fields.glAccountId, gl1));
        assertNotNull("There is no imported GlAccount[" + gl1 + "] .", glAccount1);
        GlAccount glAccount2 = leger_repos.findOne(GlAccount.class, leger_repos.map(GlAccount.Fields.glAccountId, gl2));
        assertNotNull("There is no imported GlAccount[" + gl2 + "] .", glAccount2);
        GlAccount glAccount3 = leger_repos.findOne(GlAccount.class, leger_repos.map(GlAccount.Fields.glAccountId, gl3));
        assertNotNull("There is no imported GlAccount[" + gl3 + "] .", glAccount3);

        GlAccountOrganization glAccountOrganization1 = leger_repos.findOne(GlAccountOrganization.class,
                leger_repos.map(GlAccountOrganization.Fields.glAccountId, gl1, GlAccountOrganization.Fields.organizationPartyId, organizationPartyId));
        assertNotNull("There is no imported GlAccountOrganization[" + gl1 + ",'Company'] .", glAccountOrganization1);
        GlAccountOrganization glAccountOrganization2 = leger_repos.findOne(GlAccountOrganization.class,
                leger_repos.map(GlAccountOrganization.Fields.glAccountId, gl2, GlAccountOrganization.Fields.organizationPartyId, organizationPartyId));
        assertNotNull("There is no imported GlAccountOrganization[" + gl2 + ",'Company'] .", glAccountOrganization2);
        GlAccountOrganization glAccountOrganization3 = leger_repos.findOne(GlAccountOrganization.class,
                leger_repos.map(GlAccountOrganization.Fields.glAccountId, gl3, GlAccountOrganization.Fields.organizationPartyId, organizationPartyId));
        assertNotNull("There is no imported GlAccountOrganization[" + gl3 + ",'Company'] .", glAccountOrganization3);

        assertEquals("GL Account[" + gl1 + "] type Id has not correct value", "CURRENT_ASSET", glAccount1.getGlAccountTypeId());
        assertEquals("GL Account[" + gl1 + "] class Id has not correct value", "CASH_EQUIVALENT", glAccount1.getGlAccountClassId());
        assertEquals("GL Account[" + gl2 + "] parent Id has not correct value", gl1, glAccount2.getParentGlAccountId());
        assertNotNull("GL Account Organization [" + gl1 + ",'Company'] fromDate is empty", glAccountOrganization1.getFromDate());

        // 2a. create an accounting transaction for Company with
        //   gl1 D 100
        //   gl2 D 200
        //   gl3 C 300

        AcctgTrans acctgTrans = new AcctgTrans();
        acctgTrans.setAcctgTransId(delegator.getNextSeqId("AcctgTrans"));
        acctgTrans.setIsPosted("N");
        leger_repos.createOrUpdate(acctgTrans);

        AcctgTransEntry acctgTransEntry1 = new AcctgTransEntry();
        acctgTransEntry1.setAcctgTransId(acctgTrans.getAcctgTransId());
        acctgTransEntry1.setAcctgTransEntrySeqId("1");
        acctgTransEntry1.setGlAccountId(gl1);
        acctgTransEntry1.setOrganizationPartyId(organizationPartyId);
        acctgTransEntry1.setAmount(BigDecimal.valueOf(100));
        acctgTransEntry1.setCurrencyUomId(org.opentaps.base.constants.UomConstants.CurrencyMeasure.USD);
        acctgTransEntry1.setDebitCreditFlag("D");
        leger_repos.createOrUpdate(acctgTransEntry1);

        AcctgTransEntry acctgTransEntry2 = new AcctgTransEntry();
        acctgTransEntry2.setAcctgTransId(acctgTrans.getAcctgTransId());
        acctgTransEntry2.setAcctgTransEntrySeqId("2");
        acctgTransEntry2.setGlAccountId(gl2);
        acctgTransEntry2.setOrganizationPartyId(organizationPartyId);
        acctgTransEntry2.setAmount(BigDecimal.valueOf(200));
        acctgTransEntry2.setCurrencyUomId(org.opentaps.base.constants.UomConstants.CurrencyMeasure.USD);
        acctgTransEntry2.setDebitCreditFlag("D");
        leger_repos.createOrUpdate(acctgTransEntry2);

        AcctgTransEntry acctgTransEntry3 = new AcctgTransEntry();
        acctgTransEntry3.setAcctgTransId(acctgTrans.getAcctgTransId());
        acctgTransEntry3.setAcctgTransEntrySeqId("3");
        acctgTransEntry3.setGlAccountId(gl3);
        acctgTransEntry3.setOrganizationPartyId(organizationPartyId);
        acctgTransEntry3.setAmount(BigDecimal.valueOf(300));
        acctgTransEntry3.setCurrencyUomId(org.opentaps.base.constants.UomConstants.CurrencyMeasure.USD);
        acctgTransEntry3.setDebitCreditFlag("C");
        leger_repos.createOrUpdate(acctgTransEntry3);

        // 2b. run postAcctgTrans for the accounting transaction and verify that it's successful

        LedgerServiceInterface ledgerService = ledger_domain.getLedgerService();
        ledgerService.setAcctgTransId(acctgTrans.getAcctgTransId());
        ledgerService.postAcctgTrans();

        AccountingTransaction changedAcctgTrans = leger_repos.getAccountingTransaction(acctgTrans.getAcctgTransId());
        assertEquals("AcctgTrans[" + acctgTrans.getAcctgTransId() + "] posted amount (" + acctgTrans.getBigDecimal("postedAmount") + ") is not equal to transaction debit total (300)", BigDecimal.valueOf(300), changedAcctgTrans.getPostedAmount());
        assertTrue("Accounting transaction [" + changedAcctgTrans.getAcctgTransId() + "] is not posted", changedAcctgTrans.isPosted());
    }

    /**
     * Verify that uploadFileForDataImport service can successifully upload records from excel file to DataImport* entities.
     * 1. Get the number of records of every DataImport* entity. Every DataImport* entity corresponds to excel file tabs i.e. Products tab - DataImportProduct .
     * 2. Run the uploadFileForDataImport service with the file from opentaps/dataimport/data/xls/OpentapsImport.xls .
     * 3. Verify the correct number of DataImport* records have been added .
     * @throws java.lang.Exception if an error occurs
     */
    public void testImportExcelFile() throws Exception {
        //obtain domains and repositories which is needed in the test
        DomainsLoader domainLoader = new DomainsLoader(new Infrastructure(dispatcher), new User(this.admin));
        DataImportDomainInterface imp_domain = domainLoader.loadDomainsDirectory().getDataImportDomain();
        ProductDataImportRepositoryInterface product_repos = imp_domain.getProductDataImportRepository();
        PartyDataImportRepositoryInterface party_repos = imp_domain.getPartyDataImportRepository();
        AccountingDataImportRepositoryInterface acc_repos = imp_domain.getAccountingDataImportRepository();

        //1. Get the number of records of DataImportProduct, _Supplier, _Customer, _GlAccount, _Inventory entities .
        int bcount1 = product_repos.findAll(DataImportProduct.class).size();
        int bcount2 = product_repos.findAll(DataImportInventory.class).size();
        int bcount3 = party_repos.findAll(DataImportSupplier.class).size();
        int bcount4 = party_repos.findAll(DataImportCustomer.class).size();
        int bcount5 = acc_repos.findAll(DataImportGlAccount.class).size();

        //2. Run the uploadFileForDataImport service with the file from opentaps/dataimport/data/xls/OpentapsImport.xls .
        String fileName = "OpentapsImport.xls";
        String folderPath = "opentaps/dataimport/data/xls/";
        String fileFormat = "EXCEL";
        String contentType = "application/vnd.ms-excel";
        ByteBuffer fileBytes = getByteBufferFromFile(folderPath + fileName);

        Map params = UtilMisc.toMap("userLogin", this.admin,
                                    "uploadedFile", fileBytes,
                                    "_uploadedFile_fileName", fileName ,
                                    "_uploadedFile_contentType", contentType,
                                    "fileFormat", fileFormat);
        String uploadFileForDataImportServiceName = "uploadFileForDataImport";
        Map<String, Object> results = dispatcher.runSync(uploadFileForDataImportServiceName, params);
        this.assertEquals("Service " + uploadFileForDataImportServiceName + " failure.", ServiceUtil.returnSuccess(), results);


        //3. Verify that recods with id that contains only decimal digits have been added.

        DataImportProduct testproduct = product_repos.findOne(DataImportProduct.class, product_repos.map(DataImportProduct.Fields.productId, decimalDigitsId));
        DataImportInventory testinventory = product_repos.findOne(DataImportInventory.class, product_repos.map(DataImportInventory.Fields.itemId, decimalDigitsId));
        DataImportSupplier testsupplier = party_repos.findOne(DataImportSupplier.class, party_repos.map(DataImportSupplier.Fields.supplierId, decimalDigitsId));
        DataImportCustomer testcustomer = party_repos.findOne(DataImportCustomer.class, party_repos.map(DataImportCustomer.Fields.customerId, decimalDigitsId));
        DataImportGlAccount testglAccount = acc_repos.findOne(DataImportGlAccount.class, acc_repos.map(DataImportGlAccount.Fields.glAccountId, decimalDigitsId));

        assertNotNull("Can't find DataImportProduct[" + decimalDigitsId + "].", testproduct);
        assertNotNull("Can't find DataImportInventory[" + decimalDigitsId + "].", testinventory);
        assertNotNull("Can't find DataImportSupplier[" + decimalDigitsId + "].", testsupplier);
        assertNotNull("Can't find DataImportCustomer[" + decimalDigitsId + "].", testcustomer);
        assertNotNull("Can't find DataImportGlAccount[" + decimalDigitsId + "].", testglAccount);

        //4. Verify the correct number of DataImportProduct, _Supplier, _Customer, _GlAccount, _Inventory have been added .
        int acount1 = product_repos.findAll(DataImportProduct.class).size();
        int acount2 = product_repos.findAll(DataImportInventory.class).size();
        int acount3 = party_repos.findAll(DataImportSupplier.class).size();
        int acount4 = party_repos.findAll(DataImportCustomer.class).size();
        int acount5 = acc_repos.findAll(DataImportGlAccount.class).size();

        assertEquals("Wrong count of records imported from excel file [" + folderPath + fileName + "] tab 'Products' to entity DataImportProduct.", 3, acount1 - bcount1);
        assertEquals("Wrong count of records imported from excel file [" + folderPath + fileName + "] tab 'Inventory' to entity DataImportInventory.", 4, acount2 - bcount2);
        assertEquals("Wrong count of records imported from excel file [" + folderPath + fileName + "] tab 'Suppliers' to entity DataImportSupplier.", 3, acount3 - bcount3);
        assertEquals("Wrong count of records imported from excel file [" + folderPath + fileName + "] tab 'Customers' to entity DataImportCustomer.", 4, acount4 - bcount4);
        assertEquals("Wrong count of records imported from excel file [" + folderPath + fileName + "] tab 'Gl Accounts' to entity DataImportGlAccount.", 16, acount5 - bcount5);

        //5. Verify fields ProductName and InternalName of entity DataImportProduct filled properly.

        DataImportProduct product1 = product_repos.findOne(DataImportProduct.class, product_repos.map(DataImportProduct.Fields.productId, "excelImport1"));
        DataImportProduct product2 = product_repos.findOne(DataImportProduct.class, product_repos.map(DataImportProduct.Fields.productId, "excelImport2"));
        DataImportProduct product3 = product_repos.findOne(DataImportProduct.class, product_repos.map(DataImportProduct.Fields.productId, decimalDigitsId));

        String productName1 = "Product name 1";
        String productName2 = "Product name 2";
        String productName3 = "Product name 3";

        assertEquals("Field productName of entity DataImportProduct[" + product1.getProductId() + "] not filled properly.", productName1, product1.getProductName());
        assertEquals("Field internalName of entity DataImportProduct[" + product1.getProductId() + "] not filled properly.", productName1, product1.getInternalName());
        assertEquals("Field productName of entity DataImportProduct[" + product2.getProductId() + "] not filled properly.", productName2, product2.getProductName());
        assertEquals("Field internalName of entity DataImportProduct[" + product2.getProductId() + "] not filled properly.", productName2, product2.getInternalName());
        assertEquals("Field productName of entity DataImportProduct[" + product3.getProductId() + "] not filled properly.", productName3, product3.getProductName());
        assertEquals("Field internalName of entity DataImportProduct[" + product3.getProductId() + "] not filled properly.", productName3, product3.getInternalName());

    }

}
