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
package org.opentaps.domain.order;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ofbiz.order.shoppingcart.ShoppingCartItem;
import org.opentaps.common.domain.order.OrderSpecification;
import org.opentaps.common.order.shoppingcart.OpentapsShoppingCart;
import org.opentaps.base.entities.*;
import org.opentaps.domain.billing.invoice.Invoice;
import org.opentaps.domain.billing.payment.Payment;
import org.opentaps.domain.billing.payment.PaymentGatewayResponse;
import org.opentaps.domain.billing.payment.PaymentMethod;
import org.opentaps.domain.organization.AccountingTagConfigurationForOrganizationAndUsage;
import org.opentaps.domain.party.Party;
import org.opentaps.domain.product.Product;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.RepositoryInterface;

/**
 * Repository for Orders to handle interaction of Order-related domain with the entity engine (database) and the service engine.
 */
public interface OrderRepositoryInterface extends RepositoryInterface {

    /**
     * Gets a specific implementation of an order specification.
     * Use this method to set a new order domain's specification.
     * @return the <code>OrderSpecificationInterface</code>
     */
    public OrderSpecificationInterface getOrderSpecification();

    /**
     * Finds an <code>Order</code> by ID from the database.
     * @param orderId the order ID
     * @return the <code>Order</code> found
     * @throws RepositoryException if an error occurs
     * @throws EntityNotFoundException no <code>Order</code> is found for the given id
     */
    public Order getOrderById(String orderId) throws RepositoryException, EntityNotFoundException;

    /**
     * Find orders which have the specified external ID, which is usually an order Id from an external integration system.
     * @param externalId the external order id
     * @return a list of <code>Order</code>
     * @throws RepositoryException if an error occurs
     */
    public List<Order> getOrdersByExternalId(String externalId) throws RepositoryException;

    /**
     * Finds and returns one Order matching the externalId.  If more than one is found, it will return an EntityNotFoundException.
     * @param externalId the external order id
     * @return an <code>Order</code>
     * @throws RepositoryException if an error occurs
     * @throws EntityNotFoundException if no or more than one <code>Order</code> is found matching the given external id
     */
    public Order getOrderByExternalId(String externalId) throws RepositoryException, EntityNotFoundException;

    /**
     * Finds all the <code>OrderAdjustmentType</code> ordered by description.
     * @return the list of <code>OrderAdjustmentType</code>
     * @throws RepositoryException if an error occurs
     */
    public List<OrderAdjustmentType> getOrderAdjustmentTypes() throws RepositoryException;

    /**
     * Finds the <code>Return</code> related to the given <code>ReturnItem</code>.
     * @param returnItem a <code>ReturnItem</code>
     * @return the <code>Return</code>
     * @throws RepositoryException if an error occurs
     */
    public Return getRelatedReturn(ReturnItem returnItem) throws RepositoryException;

    /**
     * Finds the returnable <code>OrderItem</code> for to the given <code>Order</code>.
     * @param order an <code>Order</code>
     * @return a <code>Map</code> associating the returnable <code>OrderItem</code> and a return info <code>Map</code>
     * @throws RepositoryException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public Map<OrderItem, Map> getReturnableItemsMap(Order order) throws RepositoryException;

    /**
     * Finds a specific order item in the order.
     * @param order an <code>Order</code>
     * @param orderItemSeqId the order item sequence id in the given order
     * @return an <code>OrderItem</code> value
     * @throws RepositoryException if an error occurs
     * @throws EntityNotFoundException no <code>OrderItem</code> is found for the given sequence id in the given order
     */
    public OrderItem getOrderItem(Order order, String orderItemSeqId) throws RepositoryException, EntityNotFoundException;;

    /**
     * Finds the list of <code>OrderItemAssoc</code> linked to the given <code>OrderItem</code>.
     * @param orderItem an <code>OrderItem</code>
     * @return list of <code>OrderItemAssoc</code>, might be empty but not null
     * @throws RepositoryException if an error occurs
     */
    public List<OrderItemAssoc> getRelatedOrderItemAssocsTo(OrderItem orderItem) throws RepositoryException;

    /**
     * Finds the list of <code>OrderItemAssoc</code> linked from the given <code>OrderItem</code>.
     * @param orderItem an <code>OrderItem</code>
     * @return list of <code>OrderItemAssoc</code>, might be empty but not null
     * @throws RepositoryException if an error occurs
     */
    public List<OrderItemAssoc> getRelatedOrderItemAssocsFrom(OrderItem orderItem) throws RepositoryException;

    /**
     * Finds the list of <code>OrderItem</code> related to the given <code>Order</code> that are not cancelled or rejected.
     * @param order an <code>Order</code>
     * @return list of <code>OrderItem</code>, might be empty but not null
     * @throws RepositoryException if an error occurs
     */
    public List<OrderItem> getRelatedValidOrderItems(Order order) throws RepositoryException;


    /**
     * Finds the list of <code>OrderItem</code> related to the given <code>Order</code> and <code>OrderItemShipGroup</code> that are not cancelled or rejected.
     * @param order an <code>Order</code>
     * @param shipGroup an <code>OrderItemShipGroup</code>
     * @return list of <code>OrderItem</code>, might be empty but not null
     * @throws RepositoryException if an error occurs
     */
    public List<OrderItem> getRelatedValidOrderItems(Order order, OrderItemShipGroup shipGroup) throws RepositoryException;

    /**
     * Finds the list of <code>OrderHeaderNoteView</code> related to the given <code>Order</code> and ordered from most recent to oldest.
     * @param order an <code>Order</code>
     * @return the list of <code>OrderHeaderNoteView</code>
     * @throws RepositoryException if an error occurs
     */
    public List<OrderHeaderNoteView> getRelatedOrderNotes(Order order) throws RepositoryException;

    /**
     * Finds the latest <code>OrderStatus</code> related to the given <code>Order</code>.
     * @param order an <code>Order</code>
     * @return latest <code>OrderStatus</code>
     * @throws RepositoryException if an error occurs
     */
    public OrderStatus getRelatedOrderStatus(Order order) throws RepositoryException;

    /**
     * Finds the list of <code>OrderStatus</code> related to the given <code>Order</code> and ordered from most recent to oldest.
     * @param order an <code>Order</code>
     * @return list of <code>OrderStatus</code>, might be empty but not null
     * @throws RepositoryException if an error occurs
     */
    public List<OrderStatus> getRelatedOrderStatuses(Order order) throws RepositoryException;

    /**
     * Finds the list of <code>OrderStatus</code> related to the given <code>OrderItem</code> and ordered from most recent to oldest.
     * @param orderItem an <code>OrderItem</code>
     * @return list of <code>OrderStatus</code>, might be empty but not null
     * @throws RepositoryException if an error occurs
     */
    public List<OrderStatus> getRelatedOrderStatuses(OrderItem orderItem) throws RepositoryException;

    /**
     * Finds the list of <code>Payment</code> related to the given <code>Order</code>.
     * @param order an <code>Order</code>
     * @return list of <code>Payment</code>, might be empty but not null
     * @throws RepositoryException if an error occurs
     */
    public List<Payment> getRelatedPayments(Order order) throws RepositoryException;

    /**
     * Finds the list of <code>Payment</code> related to the given <code>Order</code> and <code>OrderPaymentPreference</code>.
     * @param order an <code>Order</code>
     * @param orderPaymentPreference an <code>OrderPaymentPreference</code>
     * @return list of <code>Payment</code>, might be empty but not null
     * @throws RepositoryException if an error occurs
     */
    public List<Payment> getRelatedPayments(Order order, OrderPaymentPreference orderPaymentPreference) throws RepositoryException;

    /**
     * Finds the Transaction Code <code>Enumeration</code> related to the given <code>PaymentGatewayResponse</code>.
     * @param response a <code>PaymentGatewayResponse</code>
     * @return the Transaction Code <code>Enumeration</code>
     * @throws RepositoryException if an error occurs
     */
    public Enumeration getRelatedTransactionCode(PaymentGatewayResponse response) throws RepositoryException;

    /**
     * Finds the list <code>OrderShipmentInfoSummary</code> related to the given <code>OrderItemShipGroup</code>.
     * @param shipGroup an <code>OrderItemShipGroup</code>
     * @return the list <code>OrderShipmentInfoSummary</code>, might be empty but not null
     * @throws RepositoryException if an error occurs
     */
    public List<OrderShipmentInfoSummary> getRelatedOrderShipmentInfoSummaries(OrderItemShipGroup shipGroup) throws RepositoryException;

    /**
     * Finds the list <code>OrderAdjustment</code> related to the given <code>Order</code>
     *  that are not related to any of its <code>OrderItem</code>.
     * @param order an <code>Order</code>
     * @return the list <code>OrderAdjustment</code>, might be empty but not null
     * @throws RepositoryException if an error occurs
     */
    public List<OrderAdjustment> getRelatedNonItemOrderAdjustments(Order order) throws RepositoryException;

    /**
     * Finds the first <code>OrderRole</code> related to the given <code>Order</code> and matching the given role type.
     * @param order an <code>Order</code> to which the <code>OrderRole</code> is related to
     * @param roleTypeIds list of the role types of the <code>OrderRole</code>
     * @return the <code>OrderRole</code>
     * @throws RepositoryException if an error occurs
     */
    public OrderRole getRelatedOrderRoleByTypeId(Order order, List<String> roleTypeIds) throws RepositoryException;

    /**
     * Finds the list of <code>OrderRole</code> related to the given <code>Order</code> and matching the given role type.
     * @param order an <code>Order</code> to which the <code>OrderRole</code> is related to
     * @param roleTypeIds list of the role type of the <code>OrderRole</code>
     * @return the <code>OrderRole</code>
     * @throws RepositoryException if an error occurs
     */
    public List<OrderRole> getRelatedOrderRolesByTypeId(Order order, List<String> roleTypeIds) throws RepositoryException;

    /**
     * Finds the list of <code>Invoice</code> related to the given <code>Order</code>.
     * @param order an <code>Order</code>
     * @return list of <code>Invoice</code>, might be empty but not null
     * @throws RepositoryException if an error occurs
     */
    public List<Invoice> getRelatedInvoices(Order order) throws RepositoryException;

    /**
     * Finds the list of <code>ContactMech</code>related to the given <code>Order</code>.
     * @param order an <code>Order</code>
     * @return the list of <code>ContactMech</code>
     * @throws RepositoryException if an error occurs
     */
    public List<ContactMech> getRelatedContactMechs(Order order) throws RepositoryException;

    /**
     * Finds the list of <code>TelecomNumber</code> associated to the given order and party.
     * This actually merge the order phone numbers with the active phone numbers from the given party.
     * @param order an <code>Order</code>
     * @param party a <code>Party</code>
     * @return the list of <code>TelecomNumber</code>
     * @throws RepositoryException if an error occurs
     */
    public List<TelecomNumber> getRelatedPhoneNumbers(Order order, Party party) throws RepositoryException;

    /**
     * Finds the list of <code>ContactMechPurposeType</code> for the given <code>ContactMech</code> from both the given <code>Order</code> and <code>Party</code>.
     * @param contactMech a <code>ContactMech</code>
     * @param order an <code>Order</code>
     * @param party a <code>Party</code>
     * @return the list of <code>ContactMechPurposeType</code>
     * @throws RepositoryException if an error occurs
     */
    public List<ContactMechPurposeType> getRelatedContactMechPurposeTypes(ContactMech contactMech, Order order, Party party) throws RepositoryException;

    /**
     * Finds the list of <code>PostalAddress</code> that are shipping origin addresses for the given <code>Order</code> origin <code>Facility</code>.
     * @param order an <code>Order</code>
     * @return the list of <code>PostalAddress</code>
     * @throws RepositoryException if an error occurs
     */
    public List<PostalAddress> getRelatedFacilityOriginAddresses(Order order) throws RepositoryException;

    /**
     * Finds the list of <code>PostalAddress</code> that are shipping addresses for the given <code>Order</code>.
     * @param order an <code>Order</code>
     * @return the list of <code>PostalAddress</code>
     * @throws RepositoryException if an error occurs
     */
    public List<PostalAddress> getRelatedShippingAddresses(Order order) throws RepositoryException;

    /**
     *  Finds the list of <code>PostalAddress</code> that are billing addresses for the given <code>Order</code>.
     * @param order an <code>Order</code>
     * @return the list of <code>PostalAddress</code>
     * @throws RepositoryException if an error occurs
     */
    public List<PostalAddress> getRelatedBillingAddresses(Order order) throws RepositoryException;

    /**
     * Gets the list of <code>PaymentMethod</code> for the given <code>Party</code>.
     * @param party a <code>Party</code>
     * @return list of <code>PaymentMethod</code>
     * @throws RepositoryException if an error occurs
     */
    public List<PaymentMethod> getRelatedPaymentMethods(Party party) throws RepositoryException;

    /**
     * Finds the list of <code>PaymentGatewayResponse</code> for the given <code>OrderPaymentPreference</code> and transaction code <code>Enumeration</code> ID.
     * @param orderPaymentPreference an <code>OrderPaymentPreference</code>
     * @param transCodeEnumId the transaction code <code>Enumeration</code> ID the response should be in
     * @return list of <code>PaymentGatewayResponse</code>, might be empty but not null
     * @throws RepositoryException if an error occurs
     */
    public List<PaymentGatewayResponse> getRelatedPaymentGatewayResponse(OrderPaymentPreference orderPaymentPreference, String transCodeEnumId) throws RepositoryException;

    /**
     * Finds the list of <code>ProductStoreShipmentMeth</code> related to the given <code>ProductStore</code>.
     * @param productStore a <code>ProductStore</code>
     * @return the list of <code>ProductStoreShipmentMeth</code>
     * @throws RepositoryException if an error occurs
     */
    public List<ProductStoreShipmentMeth> getRelatedProductStoreShipmentMeths(ProductStore productStore) throws RepositoryException;

    /**
     * Finds the list of <code>ProductStoreShipmentMethView</code> related to the given <code>ProductStore</code>.
     * @param productStore a <code>ProductStore</code>
     * @return the list of <code>ProductStoreShipmentMethView</code>
     * @throws RepositoryException if an error occurs
     */
    public List<ProductStoreShipmentMethView> getRelatedProductStoreShipmentMethViews(ProductStore productStore) throws RepositoryException;

    /**
     * Finds the list of <code>ProdCatalog</code> for the given <code>ProductStore</code>.
     * @param productStore a <code>ProductStore</code>
     * @return the list of <code>ProdCatalog</code>
     * @throws RepositoryException if an error occurs
     */
    public List<ProdCatalog> getRelatedProdCatalogs(ProductStore productStore) throws RepositoryException;

    /**
     * Finds the <code>Product</code> related to the given <code>OrderItem</code>.
     * @param orderItem an <code>OrderItem</code>
     * @return the list of <code>OrderItem</code>
     * @throws RepositoryException if an error occurs
     */
    public Product getRelatedProduct(OrderItem orderItem) throws RepositoryException;

    /**
     * Describe <code>changeOrderItemStatus</code> method here.
     *
     * @param orderItem an <code>OrderItem</code> value
     * @param statusId a <code>String</code> value
     * @exception RepositoryException if an error occurs
     */
    public void changeOrderItemStatus(OrderItem orderItem, String statusId) throws RepositoryException;

    /**
     * Finds a <code>Party</code> by ID from the database.
     * @param partyId the party ID
     * @return the <code>Party</code> found
     * @throws RepositoryException if an error occurs
     * @throws EntityNotFoundException no <code>Party</code> is found for the given id
     */
    public Party getPartyById(String partyId) throws RepositoryException, EntityNotFoundException;

    /**
     * Finds the list of <code>Party</code> by ID from the database.
     * @param partyIds the list of party ID
     * @return the list of <code>Party</code> found
     * @throws RepositoryException if an error occurs
     */
    public Set<Party> getPartyByIds(List<String> partyIds) throws RepositoryException;

    /**
     * Finds if a particular address from a given store is assigned to a specific facility.
     * @param shipGroup the <code>OrderItemShipGroup</code> containing the shipping address
     * @param store the <code>ProductStore</code> the shipping address is associated with
     * @return a <code>Facility</code> for the address or null
     * @throws RepositoryException if an error occurs
     */
    public Facility getProductStoreFacilityByAddress(OrderItemShipGroup shipGroup, ProductStore store) throws RepositoryException;

    /**
     * Finds the list of <code>Order</code> that are open (not COMPLETED, UNDELIVERABLE or CANCELLED)
     * for the order type.
     * @param organizationPartyId the company identifier
     * @param orderType one of OrderSpecification.OrderTypeEnum constants
     * @return list of open orders
     * @throws RepositoryException if an error occurs
     */
    public List<Order> getOpenOrders(String organizationPartyId, OrderSpecification.OrderTypeEnum orderType) throws RepositoryException;

    /**
     * Finds the list of <code>OrderItem</code> for an <code>Order</code> that are still open.
     * @param order an order instance
     * @return list of open items for the order
     * @throws RepositoryException if an error occurs
     */
    public List<OrderItem> getOpenOrderItems(Order order) throws RepositoryException;

    /**
     * Validates the accounting tags for an <code>OrderItem</code>.
     * @param order an order instance
     * @param item the order item to validate the tags for
     * @return a list of <code>AccountingTagConfigurationForOrganizationAndUsage</code> that are missing
     * @throws RepositoryException if an error occurs
     */
    public List<AccountingTagConfigurationForOrganizationAndUsage> validateTagParameters(Order order, OrderItem item) throws RepositoryException;

    /**
     * Validates the accounting tags for a cart item.
     * This is mostly used during order entry.
     * @param cart an <code>OpentapsShoppingCart</code> instance
     * @param tags the <code>Map</code> of accounting tags to validate
     * @param prefix the prefix of the Map keys corresponding to the accounting tags
     * @param productId the product of the cart item
     * @return a list of <code>AccountingTagConfigurationForOrganizationAndUsage</code> that are missing
     * @throws RepositoryException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public List<AccountingTagConfigurationForOrganizationAndUsage> validateTagParameters(OpentapsShoppingCart cart, Map tags, String prefix, String productId) throws RepositoryException;

    /**
     * Validates the accounting tags for a cart item.
     * @param cart an <code>OpentapsShoppingCart</code> instance
     * @param item the <code>ShoppingCartItem</code> to validate the tags for
     * @return a list of <code>AccountingTagConfigurationForOrganizationAndUsage</code> that are missing
     * @throws RepositoryException if an error occurs
     */
    public List<AccountingTagConfigurationForOrganizationAndUsage> validateTagParameters(OpentapsShoppingCart cart, ShoppingCartItem item) throws RepositoryException;

    /**
     * Finds the OrderItemShipGrpInvRes by product id/facility id.  Returns empty list if none found.
     * @param productId a product id
     * @param facilityId a facility id
     * @return OrderItemShipGrpInvRes list
     * @throws RepositoryException if an error occurs
     */
    public List<OrderItemShipGrpInvRes> getBackOrderedInventoryReservations(String productId, String facilityId) throws RepositoryException;

    /**
     * Remove all <code>OrderConatctMech</code> with given purpose and order identifier and add new entity.
     * @param orderId an order identifier
     * @param contactMechId a contact mech id that should be postal address or null
     * @param purposeTypeId contact mech purpose type
     * @throws RepositoryException if an error occurs
     */
    public void updateOrderAddress(String orderId, String contactMechId, String purposeTypeId) throws RepositoryException;


    /**
     * Finds an <code>ReturnItemResponse</code> by ID from the database.
     * @param returnItemResponseId the ReturnItemResponse ID
     * @return the <code>ReturnItemResponse</code> found
     * @throws RepositoryException if an error occurs
     * @throws EntityNotFoundException no <code>ReturnItemResponse</code> is found for the given id
     */
    public ReturnItemResponse getReturnItemResponseById(String returnItemResponseId) throws RepositoryException, EntityNotFoundException;
}
