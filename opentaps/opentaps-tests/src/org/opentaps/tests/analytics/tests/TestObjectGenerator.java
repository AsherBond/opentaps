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

package org.opentaps.tests.analytics.tests;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
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
import org.opentaps.common.order.SalesOrderFactory;

/**
 * Collection of helper methods for generation of various opentaps objects for testing purpose.
 */
@SuppressWarnings("unchecked")
public class TestObjectGenerator {

    private static final String MODULE = TestObjectGenerator.class.getName();

    private Delegator delegator = null;
    private LocalDispatcher dispatcher = null;
    private GenericValue demoSalesManager = null;
    private GenericValue admin = null;

    private static final List<String> GREEK        = Arrays.asList("Alpha", "Beta", "Gamma", "Delta", "Epsilon", "Zeta", "Eta", "Theta", "Iota", "Kappa", "Lambda", "Mu", "Nu", "Xi", "Omicron", "Pi", "Rho", "Sigma", "Tau", "Upsilon", "Phi", "Chi", "Psi", "Omega");
    private static final List<String> NAMES        = Arrays.asList("Jacob", "Emily", "Michael", "Emma", "Joshua", "Madison", "Ethan", "Isabella", "Matthew", "Ava", "Daniel", "Abigail", "Christopher", "Olivia", "Andrew", "Hannah", "Anthony", "Sophia", "William", "Samantha", "Tyler", "Nicholas");
    private static final List<String> PARTICLES    = Arrays.asList("Fermion", "Boson", "Quark", "Electron", "Positron", "Muon", "Lepton", "Photon", "Graviton", "Gluon", "Higgs", "Hadron", "Baryon", "Nucleon", "Neutrino", "Neutron", "Proton", "Phonon", "Exciton", "Plasmon", "Polariton", "Polaron", "Magnon");
    private static final List<String> CORPTYPES    = Arrays.asList("Industries", "Corp.", "Inc.", "Ltd.", "Enterprises", "Syndicates", "Partners", "and Sons", "Gmbh.", "Visions", "Soft", "Consultants", "Services");
    private static final List<String> STREET_TYPES = Arrays.asList("Blvd", "St", "Ln", "Ave", "Rd", "Alley", "Place", "Way");

    private static final String EMAIL_SUFFIX = "@opentaps.org";

    List<GenericValue> postalCodesCache = null;

    /**
     * Public constructor.
     *
     * @param delegator a <code>Delegator</code> value
     * @param dispatcher a <code>LocalDispatcher</code> value
     * @exception GenericEntityException if an error occurs
     */
    public TestObjectGenerator(Delegator delegator, LocalDispatcher dispatcher) throws GenericEntityException {
        if (delegator == null || dispatcher == null) {
            throw new IllegalArgumentException();
        }

        this.delegator = delegator;
        this.dispatcher = dispatcher;

        demoSalesManager = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesManager"));
        admin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "admin"));
    }

    /**
     * Returns random integer in range from 0 and up to highBound - 1. Other methods may use
     * this integer as list/array index.
     * <p>
     * Typical example:<br>
     * &nbsp;<code>list.get(getRandomIndex(list.size()));</code>
     *
     * @param highBound Array/list size.
     * @return Random integer within given range.
     */
    private int getRandomIndex(int highBound) {
        return (int) ((highBound * Math.random()) - 1);
    }

    /**
     * Returns random <code>Timestamp</code> within range from <code>fromDate</code> to <code>thruDate</code>.
     * or current time if <code>fromDate</code> isn't provided.
     *
     * @param fromDate Begin of interval
     * @param thruDate End of interval
     * @return Random <code>Timestamp</code>
     */
    public Timestamp getRandomTime(Timestamp fromDate, Timestamp thruDate) {
        long fromTime = fromDate != null ? fromDate.getTime() : 0;
        long thruTime = thruDate != null ? thruDate.getTime() : UtilDateTime.nowTimestamp().getTime();
        if (fromTime == 0) {
            return UtilDateTime.nowTimestamp();
        }

        double randomNumber = Math.random();
        return new Timestamp(fromTime + ((long) ((thruTime - fromTime) * randomNumber)));
    }

    /**
     * Returns random name from predefined list of English names.
     *
     * @return Name
     */
    public String getFirstName() {
        return NAMES.get(getRandomIndex(NAMES.size()));
    }

    /**
     * Returns random name from predefined list of English names. Method obtains a name
     * calling getFirstName so far.
     *
     * @return Name
     */
    public String getLastName() {
        return getFirstName();
    }

    /**
     * Returns random company name that is combined from different elements of PARTICLES & CORPTYPES arrays.
     *
     * @return Random company name.
     */
    public String getCompanyName() {
        return String.format("%1$s %2$s", PARTICLES.get(getRandomIndex(PARTICLES.size())), CORPTYPES.get(getRandomIndex(CORPTYPES.size())));
    }

    /**
     * Method constructs and returns an address. Be aware that postal code, country, state are from
     * TestGeoData entity and they represent real objects. Street address is completely made-up.
     *
     * @return <code>Map</code> with address elements. This <code>Map</code> could be added to <code>crmsfa.createAccount</code> context.
     * @exception GenericServiceException if an error occurs
     */
    public Map getAddress() throws GenericServiceException {
        if (UtilValidate.isEmpty(postalCodesCache)) {
            try {
                postalCodesCache = delegator.findByCondition("TestGeoData", EntityCondition.makeCondition("city", EntityOperator.NOT_EQUAL, null), null, null);
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
                return null;
            }
            if (UtilValidate.isEmpty(postalCodesCache)) {
                throw new GenericServiceException("TestGeoData entity is empty. Ensure you have loaded seed data.");
            }
        }

        GenericValue postalCode = postalCodesCache.get(getRandomIndex(postalCodesCache.size()));

        Map<String, Object> address = FastMap.newInstance();
        address.put("generalAddress1", String.format("%1$d %2$s %3$s", getRandomIndex(10000), GREEK.get(getRandomIndex(GREEK.size())), STREET_TYPES.get(getRandomIndex(STREET_TYPES.size()))));
        address.put("generalCity", postalCode.getString("city"));
        address.put("generalStateProvinceGeoId", postalCode.getString("stateGeoId"));
        address.put("generalPostalCode", postalCode.getString("postalCode"));
        address.put("generalCountryGeoId", postalCode.getString("countryGeoId"));
        Debug.logInfo("Getting address: " + postalCode, MODULE);

        return address;
    }

    /**
     * Generates random US based phone number.
     *
     * @return Returns <code>Map</code> with country code(always 1), and randomly generated area code & phone number.<br>
     * This <code>Map</code> could be added to <code>crmsfa.createAccount</code> context
     */
    public Map<String, String> getPhone() {
        Map<String, String> phone = FastMap.newInstance();
        phone.put("primaryPhoneCountryCode", "1");
        phone.put("primaryPhoneAreaCode", String.format("%1$d%2$d%3$d", (getRandomIndex(8) + 2), getRandomIndex(10), getRandomIndex(10)));
        phone.put("primaryPhoneNumber", String.format("%1$d%2$d%3$d-%4$d%5$d%6$d%7$d", (getRandomIndex(9) + 1), (getRandomIndex(9) + 1), (getRandomIndex(9) + 1), (getRandomIndex(9) + 1), (getRandomIndex(9) + 1), (getRandomIndex(9) + 1), (getRandomIndex(9) + 1)));
        return phone;
    }

    /**
     * Returns random email address in <code>EMAIL_SUFFIX</code> domain.
     *
     * @return Email
     */
    public String getEmail() {
        return (PARTICLES.get(getRandomIndex(PARTICLES.size())) + EMAIL_SUFFIX);
    }

    /**
     * Creates a number of parties at current time.
     * @param count number of Accounts to create
     * @return List of <code>party ID</code>
     * @see org.opentaps.tests.analytics.tests.TestObjectGenerator#getAccounts(int count, Timestamp fromDate, Timestamp thruDate)
     * @throws GenericServiceException if an error occurs
     * @throws GenericEntityException if an error occurs
     */
    public List<String> getAccounts(int count) throws GenericServiceException, GenericEntityException {
        return getAccounts(count, null, null);
    };

    /**
     * Method creates <code>count</code> randomly generated parties with <code>createdDate</code>
     * within range from <code>fromDate</code> to <code>thruDate</code>.
     *
     * @param count Requested quantity of parties to create.
     * @param fromDate Begin of time range. May be <code>null</code> but in this case all accounts will
     * be created at current time.
     * @param thruDate End of time range. May be <code>null</code>. In this case it takes on a value
     * equals to current time.
     * @return List of <code>party ID</code>
     * @throws GenericServiceException if an error occurs
     * @throws GenericEntityException if an error occurs
     */
    public List<String> getAccounts(int count, Timestamp fromDate, Timestamp thruDate) throws GenericServiceException, GenericEntityException {
        List<String> returns = FastList.newInstance();
        for (int c = 0; c < count; c++) {
            Debug.logInfo("*** Generating account " + c + " out of " + count, MODULE);

            Map<String, Object> callCtxt = FastMap.newInstance();
            // since each getCompanyName() gets a new random value, we have to store it to use it for the company name and the address
            String companyName = getCompanyName();
            callCtxt.put("userLogin", demoSalesManager);
            callCtxt.put("accountName", companyName);
            callCtxt.put("primaryEmail", getEmail());
            callCtxt.putAll(getPhone());
            callCtxt.putAll(getAddress());
            callCtxt.put("generalToName", companyName);

            Map<String, Object> results = dispatcher.runSync("crmsfa.createAccount", callCtxt);
            if (ServiceUtil.isError(results)) {
                return null;
            }

            String partyId = (String) results.get("partyId");

            // change createDate to random date within given lag
            GenericValue party = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", partyId));
            if (UtilValidate.isNotEmpty(party)) {
                party.set("createdDate", getRandomTime(fromDate, thruDate));
                party.store();
            }

            // prepare list of industries
            List<GenericValue> industries = delegator.findByAnd("Enumeration", UtilMisc.toMap("enumTypeId", "PARTY_INDUSTRY"));
            GenericValue partySupplementalData = delegator.findByPrimaryKey("PartySupplementalData", UtilMisc.toMap("partyId", partyId));
            if (UtilValidate.isNotEmpty(partySupplementalData)) {
                partySupplementalData.set("industryEnumId", industries.get(getRandomIndex(industries.size())).getString("enumId"));
                partySupplementalData.store();
            } else {
                partySupplementalData = delegator.makeValue("PartySupplementalData");
                partySupplementalData.set("partyId", partyId);
                partySupplementalData.set("industryEnumId", industries.get(getRandomIndex(industries.size())).getString("enumId"));
                partySupplementalData.create();
            }

            returns.add(partyId);
        }
        return returns;
    };

    /**
     * Creates a number of parties at current time.
     * @see org.opentaps.tests.analytics.tests.TestObjectGenerator#getContacts(int count, Timestamp fromDate, Timestamp thruDate)
     * @throws GenericServiceException if an error occurs
     * @throws GenericEntityException if an error occurs
     */
    public List<String> getContacts(int count) throws GenericServiceException, GenericEntityException {
        return getContacts(count, null, null);
    };

    /**
     * Method creates <code>count</code> randomly generated parties with <code>createdDate</code>
     * within range from <code>fromDate</code> to <code>thruDate</code>.
     *
     * @param count Requested quantity of parties to create.
     * @param fromDate Begin of time range. May be <code>null</code> but in this case all contacts will
     * be created at current time.
     * @param thruDate End of time range. May be <code>null</code>. In this case it takes on a value
     * equals to current time.
     * @return List of <code>party ID</code>
     * @throws GenericServiceException if an error occurs
     * @throws GenericEntityException if an error occurs
     */
    public List<String> getContacts(int count, Timestamp fromDate, Timestamp thruDate) throws GenericServiceException, GenericEntityException {
        List<String> returns = FastList.newInstance();
        for (int c = 0; c < count; c++) {
            Debug.logInfo("*** Generating contact " + c + " out of " + count, MODULE);

            Map<String, Object> callCtxt = FastMap.newInstance();
            // since each get__Name() gets a new random value, we have to store it to use it for the company name and the address
            String firstName =  getFirstName();
            String lastName = getLastName();
            callCtxt.put("userLogin", demoSalesManager);
            callCtxt.put("firstName", firstName);
            callCtxt.put("lastName", lastName);
            callCtxt.put("primaryEmail", getEmail());
            callCtxt.putAll(getPhone());
            callCtxt.putAll(getAddress());
            callCtxt.put("generalToName", firstName + " " + lastName);

            Map<String, Object> results = dispatcher.runSync("crmsfa.createContact", callCtxt);
            if (ServiceUtil.isError(results)) {
                return null;
            }

            String partyId = (String) results.get("partyId");

            // change createDate to random date within given lag
            GenericValue party = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", partyId));
            if (UtilValidate.isNotEmpty(party)) {
                party.set("createdDate", getRandomTime(fromDate, thruDate));
                party.store();
            }

            returns.add(partyId);
        }
        return returns;
    }

    /**
     * Creates a number of orders at current time.
     * @see org.opentaps.tests.analytics.tests.TestObjectGenerator#getOrders(int count, String organizationPartyId, Timestamp fromDate, Timestamp thruDate, List productIds)
     * @throws GenericServiceException if an error occurs
     * @throws GenericEntityException if an error occurs
     */
    public List<String> getOrders(int count, String organizationPartyId) throws GenericEntityException, GenericServiceException {
        return getOrders(count, organizationPartyId, null, null, null);
    }

    /**
     * Creates <code>count</code> of orders and approve it. All these orders will be in time range from
     * <code>fromDate</code> to <code>thruDate</code> and between <code>organizationPartyId</code> and
     * an random account which is created by calling <code>getAccounts</code> method.
     *
     * @param count Requested quantity of orders to create.
     * @param organizationPartyId Organization
     * @param fromDate Begin of time range. May be <code>null</code> but in this case all orders will
     * be created at current time.
     * @param thruDate End of time range. May be <code>null</code>. In this case it takes on a value
     * equals to current time.
     * @return List of <code>party ID</code>
     * @throws GenericServiceException if an error occurs
     * @throws GenericEntityException if an error occurs
     */
    public List<String> getOrders(int count, String organizationPartyId, Timestamp fromDate, Timestamp thruDate, List<String> productIds) throws GenericServiceException, GenericEntityException {
        String productStoreId = "9000";
        List<String> partyIds = getAccounts(count / 2, fromDate, thruDate);
        List<GenericValue> parties = delegator.findByCondition("Party", EntityCondition.makeCondition("partyId", EntityOperator.IN, partyIds), Arrays.asList("partyId", "createdDate"), null);
        List<GenericValue> products = FastList.newInstance();
        if (productIds == null) {
            products.add(delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", "GZ-1000")));
        } else {
            products.addAll(delegator.findByCondition("Product", EntityCondition.makeCondition("productId", EntityOperator.IN, productIds), null, null));
        }
        List<String> returns = FastList.newInstance();
        for (int c = 0; c < count; c++) {
            Debug.logInfo("*** Generating order " + c + " out of " + count, MODULE);

            // Ensure used account has date less or equals to order date.
            Timestamp orderDate = UtilDateTime.nowTimestamp();
            String selectedPartyId = null;
            Timestamp tempDate = getRandomTime(fromDate, thruDate);
            List<GenericValue> filteredParties = EntityUtil.filterByCondition(parties, EntityCondition.makeCondition("createdDate", EntityOperator.LESS_THAN_EQUAL_TO, tempDate));
            if (filteredParties.size() > 0) {
                selectedPartyId = filteredParties.get(getRandomIndex(filteredParties.size())).getString("partyId");
                orderDate = tempDate;
            } else {
                selectedPartyId = partyIds.get(getRandomIndex(partyIds.size()));
            }

            SalesOrderFactory orderFactory = new SalesOrderFactory(delegator, dispatcher, demoSalesManager, organizationPartyId, selectedPartyId, productStoreId);
            int productPerOrder = getRandomIndex(10) + 1;
            for (int i = 0; i < productPerOrder; i++) {
                orderFactory.addProduct(products.get(getRandomIndex(products.size())), new BigDecimal((int) (5 * Math.random())));
            }

            orderFactory.storeOrder(orderDate);
            orderFactory.approveOrder();
            String orderId = orderFactory.getOrderId();
            setRandomCommissionAgent(orderId);
            returns.add(orderId);
        }
        return returns;
    }

    /**
     * Creates <code>count</code> of randomly generated product categories.
     *
     * @param count How many categories to create
     * @param fromDate Categories creation date
     * @return List of product category identifiers.
     */
    public List<String> getProductCategory(int count, Timestamp fromDate) {

        List<String> productCategoryIds = FastList.newInstance();

        try {

            for (int i = 0; i < count; i++) {
                Map<String, Object> context = FastMap.newInstance();
                context.put("userLogin", admin);
                context.put("productCategoryTypeId", "CATALOG_CATEGORY");
                context.put("categoryName", String.format("Category %1$d", i));

                Map<String, Object> results = dispatcher.runSync("createProductCategory", context);

                String productCategoryId = (String) results.get("productCategoryId");
                if (UtilValidate.isNotEmpty(productCategoryId)) {
                    productCategoryIds.add(productCategoryId);
                    dispatcher.runSync("addProductCategoryToCategory", UtilMisc.toMap("productCategoryId", productCategoryId, "parentProductCategoryId", "CATALOG1", "fromDate", fromDate, "userLogin", admin));
                }

            }

        } catch (GenericServiceException e) {
            Debug.logError("Unexpected error during product categories generation with message: " + e.getMessage(), MODULE);
            return null;
        }

        return productCategoryIds;
    }

    /**
     * Creates <code>count</code> of randomly generated products with random default price
     * in range from $1 to $200.
     *
     * @param count How many products to create
     * @param inCategories Put new products under one of the specified categories
     * @param fromDate Products creation date
     * @return List of product identifiers
     */
    public List<String> getProduct(int count, List<String> inCategories, Timestamp fromDate) {

        List<String> productIds = FastList.newInstance();

        try {

            for (int i = 0; i < count; i++) {
                Map<String, Object> context = FastMap.newInstance();
                context.put("userLogin", admin);
                context.put("productTypeId", "FINISHED_GOOD");
                context.put("internalName", String.format("Product %1$d", i));
                context.put("productName", String.format("Product %1$d", i));

                Map<String, Object> results = dispatcher.runSync("createProduct", context);

                String productId = (String) results.get("productId");
                if (UtilValidate.isNotEmpty(productId)) {

                    Map<String, Object> callCtxt = FastMap.newInstance();
                    callCtxt.put("userLogin", admin);
                    callCtxt.put("productId", productId);
                    callCtxt.put("productPriceTypeId", "DEFAULT_PRICE");
                    callCtxt.put("productPricePurposeId", "PURCHASE");
                    callCtxt.put("currencyUomId", "USD");
                    callCtxt.put("fromDate", fromDate);
                    callCtxt.put("price", new BigDecimal(getRandomIndex(200)));
                    callCtxt.put("productStoreGroupId", "_NA_");
                    dispatcher.runSync("createProductPrice", callCtxt);

                    productIds.add(productId);
                }

            }

            if (UtilValidate.isNotEmpty(inCategories)) {
                int i = 0;
                int prodsPerCategory = (int) (productIds.size() / inCategories.size());
                for (String productCategoryId : inCategories) {

                    int boundary = i + prodsPerCategory;
                    for (; i < boundary && i < productIds.size(); i++) {
                        Map<String, Object> context = FastMap.newInstance();
                        context.putAll(
                                UtilMisc.toMap(
                                        "userLogin", admin,
                                        "productCategoryId", productCategoryId,
                                        "productId", productIds.get(i),
                                        "fromDate", fromDate
                                )
                        );

                        dispatcher.runAsync("addProductToCategory", context);
                    }

                }
            }

        } catch (GenericServiceException e) {
            Debug.logError("Unexpected error during products generation with message: " + e.getMessage(), MODULE);
            return null;
        }

        return productIds;
    }

    private void setRandomCommissionAgent(String orderId) throws GenericServiceException, GenericEntityException {
        // find a party with role salesrep (by default no party has role commission agent)
        List<GenericValue> parties = delegator.findByAnd("PartyRole", UtilMisc.toMap("roleTypeId", "COMMISSION_AGENT"));
        if (UtilValidate.isEmpty(parties)) {
            Debug.logWarning("No COMMISSION_AGENT party found, not setting any for the order.", MODULE);
            return;
        }

        List<String> partyIds = EntityUtil.getFieldListFromEntityList(parties, "partyId", true);
        String selectedPartyId = partyIds.get(getRandomIndex(partyIds.size()));
        // give it the COMMISSION_AGENT role
        dispatcher.runSync("ensurePartyRole", UtilMisc.toMap("userLogin", demoSalesManager, "partyId", selectedPartyId, "roleTypeId", "COMMISSION_AGENT"));
        // set it as COMMISSION_AGENT for the given order
        delegator.create("OrderRole", UtilMisc.toMap("orderId", orderId, "partyId", selectedPartyId, "roleTypeId", "COMMISSION_AGENT"));
    }
}
