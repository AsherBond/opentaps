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
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
 */

package org.opentaps.amazon.tests;

import java.util.List;

import org.opentaps.domain.order.Order;
import org.opentaps.domain.order.OrderRepositoryInterface;
import org.opentaps.tests.OpentapsTestCase;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.base.util.UtilMisc;

/**
 * Tests for the Amazon integration.
 */
public class AmazonTests extends OpentapsTestCase {

    private static final String MODULE = AmazonTests.class.getName();

    @Override
    public void setUp() throws Exception {
        super.setUp();

        // reset the Amazon demo data status flags so that the tests can run each time
        List<GenericValue> amazonProducts = delegator.findAll("AmazonProduct");
        for (GenericValue amazonProduct : amazonProducts) {
            amazonProduct.set("statusId", "AMZN_PROD_CREATED");
            amazonProduct.set("postFailures", new Long(0));
            amazonProduct.store();
        }
        List<GenericValue> amazonOrders = delegator.findAll("AmazonOrder");
        for (GenericValue amazonOrder : amazonOrders) {
            amazonOrder.set("statusId", "AMZN_ORDR_CREATED");
            amazonOrder.store();
        }

    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * A very basic test of the product publishing service, which uses AmazonProduct.
     * @throws Exception if an error occurs
     */
    public void testPublishAmazonProducts() throws Exception {
        runAndAssertServiceSuccess("opentaps.amazon.publishProductsToAmazon", UtilMisc.toMap("userLogin", admin));
    }

    /**
     * Test Amazon order importing works for orders which have been extracted and stored in opentaps.
     * @throws Exception if an error occurs
     */
    public void testImportAmazonOrders() throws Exception {
        OrderRepositoryInterface orderRepository = orderDomain.getOrderRepository();

        // TODO: Use new SalesOrderSearchRepository
        // TODO: also test inventory reservation (ATP decreases for these products
        List<GenericValue> orders = delegator.findByAnd("OrderHeader", UtilMisc.toMap("productStoreId", "AMAZON"));
        for (GenericValue orderGV : orders) {
            Order order = orderRepository.getOrderById(orderGV.getString("orderId"));
            if ("TEST-AMNZ-9876543210".equals(order.getOrderId())) {
                assertEquals("Total for TEST-AMNZ-9876543210 is not correct", order.getTotal(), "43.98");
            } else if ("TEST-AMNZ-9876543211".equals(order.getOrderId())) {
                assertEquals("Total for TEST-AMNZ-9876543211 is not correct", order.getTotal(), "26.99");
            }
        }
    }
}
