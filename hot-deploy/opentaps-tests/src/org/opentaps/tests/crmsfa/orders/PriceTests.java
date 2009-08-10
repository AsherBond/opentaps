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

package org.opentaps.tests.crmsfa.orders;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.opentaps.domain.product.Product;
import org.opentaps.domain.product.ProductRepositoryInterface;
import org.opentaps.tests.OpentapsTestCase;


public class PriceTests extends OpentapsTestCase {

    protected GenericValue DemoSalesManager = null;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        DemoSalesManager = delegator.findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", "DemoSalesManager"));
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        DemoSalesManager = null;
    }

    /**
     * Calculates price adjusted to price rule for GZ-1000 and compare it with expected price.
     *
     * @throws GenericEntityException if an error occurs
     */
    public void testPartyClassPricing() throws GenericEntityException {
        String productId = "GZ-1000";
        GenericValue product = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId));

        Map<String, Object> callCtxt = new HashMap<String, Object>();
        callCtxt.put("product", product);
        callCtxt.put("partyId", "DemoPrivilegedCust");
        callCtxt.put("productStoreId", "9000");
        callCtxt.put("currencyUomId", "USD");
        callCtxt.put("productPricePurposeId", "PURCHASE");
        callCtxt.put("userLogin", DemoSalesManager);
        Map<String, Object> privilegedCustResult = runAndAssertServiceSuccess("calculateProductPrice", callCtxt);

        Double price = (Double) privilegedCustResult.get("price");
        Double expectedPrice = (Double) privilegedCustResult.get("listPrice") * 0.75;
        assertEquals(expectedPrice.doubleValue(), price.doubleValue(), 0);
    }

    /**
     * Calculates price adjusted to price rule for GZ-1000 and compare it with expected price.
     * Sames as <code>testPartyClassPricing</code> but tests the domain repository method.
     * @throws GeneralException if an error occurs
     */
    public void testPartyClassPricingDomain() throws GeneralException {
        String productId = "GZ-1000";
        ProductRepositoryInterface repository = domainsDirectory.getProductDomain().getProductRepository();
        Product product = repository.getProductById(productId);
        GenericValue product2 = delegator.findByPrimaryKey("Product", UtilMisc.toMap("productId", productId));

        Map<String, Object> callCtxt = new HashMap<String, Object>();
        callCtxt.put("product", product2);
        callCtxt.put("partyId", "DemoPrivilegedCust");
        callCtxt.put("productStoreId", "9000");
        callCtxt.put("currencyUomId", "USD");
        callCtxt.put("productPricePurposeId", "PURCHASE");
        callCtxt.put("userLogin", DemoSalesManager);
        Map<String, Object> privilegedCustResult = runAndAssertServiceSuccess("calculateProductPrice", callCtxt);

        BigDecimal price = repository.getUnitPrice(product, new BigDecimal("1.0"), "USD", "DemoPrivilegedCust");
        BigDecimal expectedPrice = BigDecimal.valueOf((Double) privilegedCustResult.get("listPrice")).multiply(new BigDecimal("0.75"));

        assertEquals("Domain method to get unit price for party returned unexpected price.", expectedPrice, price);
    }
}
