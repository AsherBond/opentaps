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

package org.opentaps.amazon.tests;

import java.util.List;

import org.opentaps.base.entities.AmazonOrder;
import org.opentaps.base.entities.AmazonProduct;

import org.opentaps.domain.order.Order;
import org.opentaps.domain.order.OrderRepositoryInterface;
import org.opentaps.domain.product.ProductRepositoryInterface;
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
        // "borrowing" the ProductRepositoryInterface for now
        ProductRepositoryInterface productRepository = productDomain.getProductRepository();
        List<AmazonProduct> amazonProducts = productRepository.findAll(AmazonProduct.class);
        for (AmazonProduct amazonProduct : amazonProducts) {
            amazonProduct.setStatusId("AMZN_PROD_CREATED");
            amazonProduct.setPostFailures(new Long(0));
            productRepository.createOrUpdate(amazonProduct);
        }
        List<AmazonOrder> amazonOrders = productRepository.findAll(AmazonOrder.class);
        for (AmazonOrder amazonOrder : amazonOrders) {
            amazonOrder.setStatusId("AMZN_ORDR_CREATED");
            productRepository.createOrUpdate(amazonOrder);
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

        // TODO: Use new SalesOrderLookupRepository
        // TODO: also test inventory reservation (ATP decreases for these products

        // based on opentaps/amazon/data/AmazonDemoSetup.xml
        List<GenericValue> orders = delegator.findByAnd("OrderHeader", UtilMisc.toMap("productStoreId", "AMAZON"));
        for (GenericValue orderGV : orders) {
            Order order = orderRepository.getOrderById(orderGV.getString("orderId"));
            // these are from opentaps/amazon/data/AmazonDemoData.xml
            if ("TEST-AMNZ-9876543210".equals(order.getOrderId())) {
                assertEquals("Total for TEST-AMNZ-9876543210 is not correct", order.getTotal(), "43.98");
            } else if ("TEST-AMNZ-9876543211".equals(order.getOrderId())) {
                assertEquals("Total for TEST-AMNZ-9876543211 is not correct", order.getTotal(), "26.99");
            }
        }
    }
}
