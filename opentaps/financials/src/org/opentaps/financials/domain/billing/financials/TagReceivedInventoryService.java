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
package org.opentaps.financials.domain.billing.financials;

import org.ofbiz.base.util.Debug;
import org.opentaps.domain.DomainService;
import org.opentaps.domain.billing.financials.TagReceivedInventoryServiceInterface;
import org.opentaps.domain.inventory.InventoryItem;
import org.opentaps.domain.inventory.InventoryRepositoryInterface;
import org.opentaps.domain.order.Order;
import org.opentaps.domain.order.OrderItem;
import org.opentaps.domain.order.OrderRepositoryInterface;
import org.opentaps.foundation.exception.FoundationException;
import org.opentaps.foundation.service.ServiceException;

/**
 * Service to tag a inventory received from a PO.
 */
public class TagReceivedInventoryService extends DomainService implements TagReceivedInventoryServiceInterface {

    private static final String MODULE = TagReceivedInventoryService.class.getName();

    private String orderId;
    private String orderItemSeqId;
    private String inventoryItemId;

    /** {@inheritDoc} */
    public void tagReceivedInventoryFromOrder() throws ServiceException {
        try {
            OrderRepositoryInterface orderRepository = getDomainsDirectory().getOrderDomain().getOrderRepository();
            InventoryRepositoryInterface inventoryRepository = getDomainsDirectory().getInventoryDomain().getInventoryRepository();
            Order order = orderRepository.getOrderById(orderId);
            if (!order.isPurchaseOrder()) {
                Debug.logWarning("Received inventory from an order that is not a purchase order [" + orderId + "]", MODULE);
                return;
            }
            OrderItem orderItem = orderRepository.getOrderItem(order, orderItemSeqId);
            InventoryItem inventoryItem = inventoryRepository.getInventoryItemById(inventoryItemId);
            inventoryItem.setAcctgTagEnumId1(orderItem.getAcctgTagEnumId1());
            inventoryItem.setAcctgTagEnumId2(orderItem.getAcctgTagEnumId2());
            inventoryItem.setAcctgTagEnumId3(orderItem.getAcctgTagEnumId3());
            inventoryItem.setAcctgTagEnumId4(orderItem.getAcctgTagEnumId4());
            inventoryItem.setAcctgTagEnumId5(orderItem.getAcctgTagEnumId5());
            inventoryItem.setAcctgTagEnumId6(orderItem.getAcctgTagEnumId6());
            inventoryItem.setAcctgTagEnumId7(orderItem.getAcctgTagEnumId7());
            inventoryItem.setAcctgTagEnumId8(orderItem.getAcctgTagEnumId8());
            inventoryItem.setAcctgTagEnumId9(orderItem.getAcctgTagEnumId9());
            inventoryItem.setAcctgTagEnumId10(orderItem.getAcctgTagEnumId10());
            inventoryRepository.update(inventoryItem);
        } catch (FoundationException e) {
            Debug.logError(e, MODULE);
            throw new ServiceException(e);
        }
    }

    /** {@inheritDoc} */
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    /** {@inheritDoc} */
    public void setOrderItemSeqId(String orderItemSeqId) {
        this.orderItemSeqId = orderItemSeqId;
    }

    /** {@inheritDoc} */
    public void setInventoryItemId(String inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
    }
}
