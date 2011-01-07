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
package org.opentaps.common.domain.order;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.common.DataModelConstants;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.order.shoppingcart.ShoppingCartItem;
import org.opentaps.base.constants.ContactMechPurposeTypeConstants;
import org.opentaps.base.constants.StatusItemConstants;
import org.opentaps.base.entities.ContactMech;
import org.opentaps.base.entities.ContactMechPurposeType;
import org.opentaps.base.entities.Enumeration;
import org.opentaps.base.entities.Facility;
import org.opentaps.base.entities.FacilityContactMechPurpose;
import org.opentaps.base.entities.OrderAdjustmentType;
import org.opentaps.base.entities.OrderContactMech;
import org.opentaps.base.entities.OrderHeader;
import org.opentaps.base.entities.OrderHeaderNoteView;
import org.opentaps.base.entities.OrderItemAssoc;
import org.opentaps.base.entities.OrderItemBilling;
import org.opentaps.base.entities.OrderItemShipGroupAssoc;
import org.opentaps.base.entities.OrderShipmentInfoSummary;
import org.opentaps.base.entities.OrderStatus;
import org.opentaps.base.entities.PartyContactMechPurpose;
import org.opentaps.base.entities.PostalAddress;
import org.opentaps.base.entities.ProdCatalog;
import org.opentaps.base.entities.ProductStoreCatalog;
import org.opentaps.base.entities.ProductStoreFacilityByAddress;
import org.opentaps.base.entities.ProductStoreShipmentMeth;
import org.opentaps.base.entities.ProductStoreShipmentMethView;
import org.opentaps.base.entities.ReturnItemResponse;
import org.opentaps.base.entities.TelecomNumber;
import org.opentaps.base.services.ChangeOrderItemStatusService;
import org.opentaps.base.services.GetReturnableItemsService;
import org.opentaps.common.domain.order.OrderSpecification.OrderTypeEnum;
import org.opentaps.common.order.shoppingcart.OpentapsShoppingCart;
import org.opentaps.common.util.UtilAccountingTags;
import org.opentaps.domain.DomainRepository;
import org.opentaps.domain.billing.invoice.Invoice;
import org.opentaps.domain.billing.payment.Payment;
import org.opentaps.domain.billing.payment.PaymentGatewayResponse;
import org.opentaps.domain.billing.payment.PaymentMethod;
import org.opentaps.domain.order.Order;
import org.opentaps.domain.order.OrderAdjustment;
import org.opentaps.domain.order.OrderItem;
import org.opentaps.domain.order.OrderItemShipGroup;
import org.opentaps.domain.order.OrderItemShipGrpInvRes;
import org.opentaps.domain.order.OrderPaymentPreference;
import org.opentaps.domain.order.OrderRepositoryInterface;
import org.opentaps.domain.order.OrderRole;
import org.opentaps.domain.order.OrderSpecificationInterface;
import org.opentaps.domain.order.ProductStore;
import org.opentaps.domain.order.Return;
import org.opentaps.domain.order.ReturnItem;
import org.opentaps.domain.organization.AccountingTagConfigurationForOrganizationAndUsage;
import org.opentaps.domain.organization.OrganizationRepositoryInterface;
import org.opentaps.domain.party.Party;
import org.opentaps.domain.party.PartyRepositoryInterface;
import org.opentaps.domain.product.Product;
import org.opentaps.domain.product.ProductRepositoryInterface;
import org.opentaps.foundation.entity.Entity;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.entity.hibernate.Query;
import org.opentaps.foundation.entity.hibernate.Session;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;


/**
 * Implementation of the order repository.
 */
public class OrderRepository extends DomainRepository implements OrderRepositoryInterface {

   private PartyRepositoryInterface partyRepository;
    private ProductRepositoryInterface productRepository;
    private OrganizationRepositoryInterface organizationRepository;
    private OrderSpecificationInterface orderSpecification = new OrderSpecification();

    /**
     * Default constructor.
     */
    public OrderRepository() {
        super();
    }

     /**
     * If you want the full infrastructure including the dispatcher, then you must have the User.
     * @param infrastructure the domain infrastructure
     * @param userLogin the Ofbiz <code>UserLogin</code> generic value
     * @throws RepositoryException if an error occurs
     */
    public OrderRepository(Infrastructure infrastructure, GenericValue userLogin) throws RepositoryException {
        super(infrastructure, userLogin);
    }

    /**
     * Returns the <code>OrderSpecification</code> implementation defined in this package.
     * @return an <code>OrderSpecificationInterface</code> value
     */
    public OrderSpecificationInterface getOrderSpecification() {
        return orderSpecification;
    }

    /** {@inheritDoc} */
    public Order getOrderById(String orderId) throws RepositoryException, EntityNotFoundException {
        return findOneNotNull(Order.class, map(Order.Fields.orderId, orderId), "OpentapsError_OrderNotFound", UtilMisc.toMap("orderId", orderId));
    }

    /** {@inheritDoc} */
    public List<Order> getOrdersByExternalId(String externalId) throws RepositoryException {
        return findList(Order.class, map(Order.Fields.externalId, externalId));
    }

    /** {@inheritDoc} */
    public Order getOrderByExternalId(String externalId) throws RepositoryException, EntityNotFoundException {
        List<Order> orders = getOrdersByExternalId(externalId);
        // if no orders are found, the EntityNotFoundException should have already been thrown, so just check multiple orders case
        if (UtilValidate.isEmpty(orders)) {
            throw new EntityNotFoundException(Order.class, "No order found for external ID [" + externalId + "]");
        } else if ((orders != null) && (orders.size() > 1)) {
            throw new EntityNotFoundException(Order.class, "[" + orders.size() + "] orders found for reference ID [" + externalId + "]");
        }
        return orders.get(0);
    }

    /** {@inheritDoc} */
    public List<OrderAdjustmentType> getOrderAdjustmentTypes() throws RepositoryException {
        return findAll(OrderAdjustmentType.class, Arrays.asList("description"));
    }

    /** {@inheritDoc} */
    public Enumeration getRelatedTransactionCode(PaymentGatewayResponse response) throws RepositoryException {
        return findOne(Enumeration.class, map(Enumeration.Fields.enumId, response.getTransCodeEnumId()));
    }

    /** {@inheritDoc} */
    public OrderItem getOrderItem(Order order, String orderItemSeqId) throws RepositoryException, EntityNotFoundException {
        return findOneNotNull(OrderItem.class, map(OrderItem.Fields.orderId, order.getOrderId(), OrderItem.Fields.orderItemSeqId, orderItemSeqId));
    }

    /** {@inheritDoc} */
    public List<OrderItemAssoc> getRelatedOrderItemAssocsTo(OrderItem orderItem) throws RepositoryException {
        return findList(OrderItemAssoc.class, map(OrderItemAssoc.Fields.orderId, orderItem.getOrderId(), OrderItemAssoc.Fields.orderItemSeqId, orderItem.getOrderItemSeqId()));
    }

    /** {@inheritDoc} */
    public List<OrderItemAssoc> getRelatedOrderItemAssocsFrom(OrderItem orderItem) throws RepositoryException {
        return findList(OrderItemAssoc.class, map(OrderItemAssoc.Fields.toOrderId, orderItem.getOrderId(), OrderItemAssoc.Fields.toOrderItemSeqId, orderItem.getOrderItemSeqId()));
    }

    /** {@inheritDoc} */
    public Return getRelatedReturn(ReturnItem returnItem) throws RepositoryException {
        return findOne(Return.class, map(Return.Fields.returnId, returnItem.getReturnId()));
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public Map<OrderItem, Map> getReturnableItemsMap(Order order) throws RepositoryException {
        Map<OrderItem, Map> returnableItems = new HashMap<OrderItem, Map>();
        try {
            GetReturnableItemsService service = new GetReturnableItemsService();
            service.setInOrderId(order.getOrderId());
            service.runSync(getInfrastructure());
            if (service.getOutReturnableItems() != null) {
                returnableItems = service.getOutReturnableItems();
            }
        } catch (ServiceException e) {
            throw new RepositoryException(e);
        }
        return returnableItems;
    }

    /** {@inheritDoc} */
    public List<OrderItem> getRelatedValidOrderItems(Order order) throws RepositoryException {
        EntityConditionList<EntityExpr> conditions = EntityCondition.makeCondition(EntityOperator.AND,
            EntityCondition.makeCondition("orderId", order.getOrderId()),
            EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, OrderSpecification.OrderItemStatusEnum.CANCELLED.getStatusId()),
            EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, OrderSpecification.OrderItemStatusEnum.REJECTED.getStatusId())
        );
        return findList(OrderItem.class, conditions);
    }

    /** {@inheritDoc} */
    public List<OrderItem> getRelatedValidOrderItems(Order order, OrderItemShipGroup shipGroup) throws RepositoryException {
        List<? extends OrderItemShipGroupAssoc> assocs = shipGroup.getOrderItemShipGroupAssocs();
        List<OrderItem> items = new ArrayList<OrderItem>();
        for (OrderItemShipGroupAssoc assoc : assocs) {
            BigDecimal qty = assoc.getQuantity();
            BigDecimal canceled = assoc.getCancelQuantity();
            if (canceled != null) {
                qty = qty.subtract(canceled);
            }
            if (qty.signum() != 0) {
                OrderItem assocItem = assoc.getRelatedOne(OrderItem.class, "OrderItem");
                if (!assocItem.isCancelled() && !assocItem.isRejected()) {
                    items.add(assocItem);
                }
            }
        }
        return items;
    }

    /** {@inheritDoc} */
    public List<OrderAdjustment> getRelatedNonItemOrderAdjustments(Order order) throws RepositoryException {
        EntityConditionList<EntityCondition> conditions = EntityCondition.makeCondition(EntityOperator.AND,
            EntityCondition.makeCondition("orderId", order.getOrderId()),
            EntityCondition.makeCondition(EntityOperator.OR,
               EntityCondition.makeCondition("orderItemSeqId", null),
               EntityCondition.makeCondition("orderItemSeqId", DataModelConstants.SEQ_ID_NA),
               EntityCondition.makeCondition("orderItemSeqId", ""))
        );
        return findList(OrderAdjustment.class, conditions);
    }

    /** {@inheritDoc} */
    public List<OrderHeaderNoteView> getRelatedOrderNotes(Order order) throws RepositoryException {
        return findList(OrderHeaderNoteView.class, map(OrderHeaderNoteView.Fields.orderId, order.getOrderId()), Arrays.asList("-noteDateTime"));
    }

    /** {@inheritDoc} */
    public OrderStatus getRelatedOrderStatus(Order order) throws RepositoryException {
        return getFirst(getRelatedOrderStatuses(order));
    }

    /** {@inheritDoc} */
    public List<OrderStatus> getRelatedOrderStatuses(Order order) throws RepositoryException {
        EntityConditionList<EntityCondition> conditions = EntityCondition.makeCondition(EntityOperator.AND,
            EntityCondition.makeCondition("orderId", order.getOrderId()),
            EntityCondition.makeCondition(EntityOperator.OR,
                EntityCondition.makeCondition("orderItemSeqId", null),
                EntityCondition.makeCondition("orderItemSeqId", DataModelConstants.SEQ_ID_NA),
                EntityCondition.makeCondition("orderItemSeqId", "")),
            // In ofbiz 09.04 whenever order payments are processed, an order status is stored.
            // This will filter out the order statuses which are actually payment statuses.
            EntityCondition.makeCondition("orderPaymentPreferenceId", null)
        );
        return findList(OrderStatus.class, conditions, Arrays.asList("-statusDatetime"));
    }

    /** {@inheritDoc} */
    public List<OrderStatus> getRelatedOrderStatuses(OrderItem orderItem) throws RepositoryException {
        return findList(OrderStatus.class, map(OrderStatus.Fields.orderId, orderItem.getOrderId(), OrderStatus.Fields.orderItemSeqId, orderItem.getOrderItemSeqId()), Arrays.asList("-statusDatetime"));
    }

    /** {@inheritDoc} */
    public List<OrderShipmentInfoSummary> getRelatedOrderShipmentInfoSummaries(OrderItemShipGroup shipGroup) throws RepositoryException {
        return findList(OrderShipmentInfoSummary.class, map(OrderShipmentInfoSummary.Fields.orderId, shipGroup.getOrderId(), OrderShipmentInfoSummary.Fields.shipGroupSeqId, shipGroup.getShipGroupSeqId()));
    }

    /** {@inheritDoc} */
    public List<Payment> getRelatedPayments(Order order) throws RepositoryException {
        return getRelatedPayments(order, null);
    }

    /** {@inheritDoc} */
    public List<Payment> getRelatedPayments(Order order, OrderPaymentPreference orderPaymentPreference) throws RepositoryException {

        List<OrderPaymentPreference> prefs = null;
        List<Payment> payments = new ArrayList<Payment>();

        if (orderPaymentPreference == null) {
            prefs = order.getOrderPaymentPreferences();
        } else {
            prefs = new ArrayList<OrderPaymentPreference>(1);
            prefs.add(orderPaymentPreference);
        }

        if (prefs == null) {
            return payments;
        }

        for (OrderPaymentPreference pref : prefs) {
            payments.addAll(findList(Payment.class, map(Payment.Fields.paymentPreferenceId, pref.getOrderPaymentPreferenceId())));
        }

        return payments;
    }

    /** {@inheritDoc} */
    public OrderRole getRelatedOrderRoleByTypeId(Order order, List<String> roleTypeIds) throws RepositoryException {
        return getFirst(getRelatedOrderRolesByTypeId(order, roleTypeIds));
    }

    /** {@inheritDoc} */
    public List<OrderRole> getRelatedOrderRolesByTypeId(Order order, List<String> roleTypeIds) throws RepositoryException {
        return findList(OrderRole.class, Arrays.asList(
                EntityCondition.makeCondition("orderId", order.getOrderId()),
                EntityCondition.makeCondition("roleTypeId", EntityOperator.IN, roleTypeIds)));
    }

    /** {@inheritDoc} */
    public List<Invoice> getRelatedInvoices(Order order) throws RepositoryException {
        List<? extends OrderItemBilling> billings = order.getOrderItemBillings();
        List<String> invoiceIds = new ArrayList<String>();
        for (OrderItemBilling b : billings) {
            if (b.getInvoiceId() != null) {
                invoiceIds.add(b.getInvoiceId());
            }
        }
        return findList(Invoice.class, EntityCondition.makeCondition("invoiceId", EntityOperator.IN, invoiceIds));
    }

    /** {@inheritDoc} */
    public List<ContactMech> getRelatedContactMechs(Order order) throws RepositoryException  {
        List<? extends OrderContactMech> orderContactMechs = order.getOrderContactMeches();
        Set<Object> contactMechIds = Entity.getDistinctFieldValues(orderContactMechs, OrderContactMech.Fields.contactMechId);
        return findList(ContactMech.class, EntityCondition.makeCondition("contactMechId", EntityOperator.IN, contactMechIds));
    }

    /** {@inheritDoc} */
    public List<TelecomNumber> getRelatedPhoneNumbers(Order order, Party party) throws RepositoryException {
        List<ContactMech> contactMechs = getRelatedContactMechs(order);
        Set<Object> contactMechIds = Entity.getDistinctFieldValues(contactMechs, ContactMech.Fields.contactMechId);
        List<TelecomNumber> partyPhoneNumbers = party.getPhoneNumbers();
        contactMechIds.addAll(Entity.getDistinctFieldValues(partyPhoneNumbers, TelecomNumber.Fields.contactMechId));
        return findList(TelecomNumber.class, EntityCondition.makeCondition("contactMechId", EntityOperator.IN, contactMechIds));
    }

    /** {@inheritDoc} */
    public List<ContactMechPurposeType> getRelatedContactMechPurposeTypes(ContactMech contactMech, Order order, Party party) throws RepositoryException {
        // add purpose types from the party
        Set<Object> purposeTypeIds = Entity.getDistinctFieldValues(contactMech.getPartyContactMechPurposes(), PartyContactMechPurpose.Fields.contactMechPurposeTypeId);
        // add purpose types from the order
        purposeTypeIds.addAll(Entity.getDistinctFieldValues(findList(OrderContactMech.class, map(OrderContactMech.Fields.orderId, order.getOrderId(), OrderContactMech.Fields.contactMechId, contactMech.getContactMechId())), OrderContactMech.Fields.contactMechPurposeTypeId));
        // find the list
        return findList(ContactMechPurposeType.class, EntityCondition.makeCondition("contactMechPurposeTypeId", EntityOperator.IN, purposeTypeIds));
    }

    /** {@inheritDoc} */
    public List<PostalAddress> getRelatedFacilityOriginAddresses(Order order) throws RepositoryException  {
        Facility originFacility = order.getOriginFacility();
        if (originFacility != null) {
            List<FacilityContactMechPurpose> facilityContactMechs = findList(FacilityContactMechPurpose.class, Arrays.asList(
                                         EntityCondition.makeCondition(FacilityContactMechPurpose.Fields.facilityId.name(), originFacility.getFacilityId()),
                                         EntityCondition.makeCondition(FacilityContactMechPurpose.Fields.contactMechPurposeTypeId.name(), ContactMechPurposeTypeConstants.SHIP_ORIG_LOCATION),
                                         EntityUtil.getFilterByDateExpr()));
            return findList(PostalAddress.class, EntityCondition.makeCondition(PostalAddress.Fields.contactMechId.name(), EntityOperator.IN, Entity.getDistinctFieldValues(facilityContactMechs, FacilityContactMechPurpose.Fields.contactMechId)));
        } else {
            return new ArrayList<PostalAddress>();
        }
    }

    /** {@inheritDoc} */
    public List<PostalAddress> getRelatedShippingAddresses(Order order) throws RepositoryException  {
        List<OrderContactMech> orderContactMechs = getOrderContactMechs(order, ContactMechPurposeTypeConstants.SHIPPING_LOCATION);
        return findList(PostalAddress.class, EntityCondition.makeCondition(PostalAddress.Fields.contactMechId.name(), EntityOperator.IN, Entity.getDistinctFieldValues(orderContactMechs, OrderContactMech.Fields.contactMechId)));
    }

    /** {@inheritDoc} */
    public List<PostalAddress> getRelatedBillingAddresses(Order order) throws RepositoryException {
        List<OrderContactMech> orderContactMechs = getOrderContactMechs(order, ContactMechPurposeTypeConstants.BILLING_LOCATION);
        return findList(PostalAddress.class, EntityCondition.makeCondition(PostalAddress.Fields.contactMechId.name(), EntityOperator.IN, Entity.getDistinctFieldValues(orderContactMechs, OrderContactMech.Fields.contactMechId)));
    }

    /** {@inheritDoc} */
    public List<PaymentGatewayResponse> getRelatedPaymentGatewayResponse(OrderPaymentPreference orderPaymentPreference, String transCodeEnumId) throws RepositoryException {
        return findList(PaymentGatewayResponse.class, map(PaymentGatewayResponse.Fields.orderPaymentPreferenceId, orderPaymentPreference.getOrderPaymentPreferenceId(), PaymentGatewayResponse.Fields.transCodeEnumId, transCodeEnumId));
    }

    /** {@inheritDoc} */
    public List<PaymentMethod> getRelatedPaymentMethods(Party party) throws RepositoryException {
        return findList(PaymentMethod.class, Arrays.asList(EntityCondition.makeCondition(PaymentMethod.Fields.partyId.name(), party.getPartyId()),
                                                           EntityUtil.getFilterByDateExpr()));
    }

    /** {@inheritDoc} */
    public List<ProductStoreShipmentMeth> getRelatedProductStoreShipmentMeths(ProductStore productStore) throws RepositoryException {
        return findList(ProductStoreShipmentMeth.class, map(ProductStoreShipmentMeth.Fields.productStoreId, productStore.getProductStoreId()));
    }

    /** {@inheritDoc} */
    public List<ProductStoreShipmentMethView> getRelatedProductStoreShipmentMethViews(ProductStore productStore) throws RepositoryException {
        return findList(ProductStoreShipmentMethView.class, map(ProductStoreShipmentMethView.Fields.productStoreId, productStore.getProductStoreId()));
    }

    /** {@inheritDoc} */
    public List<ProdCatalog> getRelatedProdCatalogs(ProductStore productStore) throws RepositoryException {
        List<ProductStoreCatalog> productStoreCatalogs = findList(ProductStoreCatalog.class, map(ProductStoreCatalog.Fields.productStoreId, productStore.getProductStoreId()), Arrays.asList("sequenceNum"));
        return findList(ProdCatalog.class, EntityCondition.makeCondition(ProdCatalog.Fields.prodCatalogId.name(), EntityOperator.IN, Entity.getDistinctFieldValues(productStoreCatalogs, ProductStoreCatalog.Fields.prodCatalogId)), Arrays.asList(ProdCatalog.Fields.catalogName.name()));
    }

    /** {@inheritDoc} */
    public Product getRelatedProduct(OrderItem orderItem) throws RepositoryException {
        try {
            return getProductRepository().getProductById(orderItem.getProductId());
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    // this might be an example of how to deal with method explosion while still hiding the ERP specific string keys like "ITEM_COMPLETED"
    /** {@inheritDoc} */
    public void changeOrderItemStatus(OrderItem orderItem, String statusId) throws RepositoryException {
        try {
            ChangeOrderItemStatusService service = new ChangeOrderItemStatusService(getUser());
            service.setInOrderId(orderItem.getOrderId());
            service.setInOrderItemSeqId(orderItem.getOrderItemSeqId());
            service.setInStatusId(statusId);
            service.runSync(getInfrastructure());
            if (service.isError()) {
                throw new RepositoryException(service.getErrorMessage());
            }
        } catch (ServiceException e) {
            throw new RepositoryException(e);
        }
    }

    /** {@inheritDoc} */
    public Facility getProductStoreFacilityByAddress(OrderItemShipGroup shipGroup, ProductStore store) throws RepositoryException {
        ProductStoreFacilityByAddress addressFacility = findOne(ProductStoreFacilityByAddress.class, map(ProductStoreFacilityByAddress.Fields.productStoreId,  store.getProductStoreId(), ProductStoreFacilityByAddress.Fields.contactMechId, shipGroup.getContactMechId()));
        if (addressFacility == null) {
            return null;
        }
        return addressFacility.getFacility();
    }

    private List<OrderContactMech> getOrderContactMechs(Order order, String role) throws RepositoryException {
        return findList(OrderContactMech.class, map(OrderContactMech.Fields.orderId, order.getOrderId(), OrderContactMech.Fields.contactMechPurposeTypeId, role));
    }

    /** {@inheritDoc} */
    public Party getPartyById(String partyId) throws RepositoryException, EntityNotFoundException {
        return getPartyRepository().getPartyById(partyId);
    }

    /** {@inheritDoc} */
    public Set<Party> getPartyByIds(List<String> partyIds) throws RepositoryException {
        return getPartyRepository().getPartyByIds(partyIds);
    }

    protected PartyRepositoryInterface getPartyRepository() throws RepositoryException {
        if (partyRepository == null) {
            partyRepository = getDomainsDirectory().getPartyDomain().getPartyRepository();
        }
        return partyRepository;
    }

    protected ProductRepositoryInterface getProductRepository() throws RepositoryException {
        if (productRepository == null) {
            productRepository = getDomainsDirectory().getProductDomain().getProductRepository();
        }
        return productRepository;
    }

    /** {@inheritDoc} */
    public List<OrderItem> getOpenOrderItems(Order order) throws RepositoryException {
        if (order == null) {
            throw new RepositoryException(new IllegalArgumentException());
        }

        EntityConditionList<EntityExpr> conditionList =
            EntityCondition.makeCondition(EntityOperator.AND,
                            EntityCondition.makeCondition(OrderItem.Fields.statusId.getName(), EntityOperator.NOT_IN,
                                    Arrays.asList(StatusItemConstants.OrderItemStatus.ITEM_CANCELLED, StatusItemConstants.OrderItemStatus.ITEM_COMPLETED)
                            ),
                            EntityCondition.makeCondition(OrderItem.Fields.orderId.getName(), order.getOrderId())
                    );
        return findList(OrderItem.class, conditionList);
    }

    /** {@inheritDoc} */
    public List<Order> getOpenOrders(String organizationPartyId, OrderTypeEnum orderType) throws RepositoryException {
        if (UtilValidate.isEmpty(organizationPartyId) || orderType == null) {
            throw new RepositoryException(new IllegalArgumentException());
        }

        EntityConditionList<EntityExpr> conditions = EntityCondition.makeCondition(EntityOperator.AND,
                EntityCondition.makeCondition(OrderHeader.Fields.statusId.getName(), EntityOperator.NOT_IN, Arrays.asList(StatusItemConstants.OrderStatus.ORDER_COMPLETED, StatusItemConstants.OrderStatus.ORDER_CANCELLED, StatusItemConstants.OrderStatus.ORDER_UNDELIVERABLE)),
                EntityCondition.makeCondition(OrderHeader.Fields.orderTypeId.getName(), orderType.getOrderTypeId()),
                EntityCondition.makeCondition((orderType == OrderTypeEnum.SALES ? OrderHeader.Fields.billFromPartyId.getName() : OrderHeader.Fields.billToPartyId.getName()), organizationPartyId)
        );

        return findList(Order.class, conditions);
    }

    /** {@inheritDoc} */
    public List<AccountingTagConfigurationForOrganizationAndUsage> validateTagParameters(Order order, OrderItem item) throws RepositoryException {
        String organizationPartyId = order.getOrganizationParty().getPartyId();
        String accountingTagUsageTypeId = UtilAccountingTags.SALES_ORDER_TAG;
        if (order.isPurchaseOrder()) {
            accountingTagUsageTypeId = UtilAccountingTags.PURCHASE_ORDER_TAG;
        }
        return getOrganizationRepository().validateTagParameters(item, organizationPartyId, accountingTagUsageTypeId);
    }

    /** {@inheritDoc} */
    public List<AccountingTagConfigurationForOrganizationAndUsage> validateTagParameters(OpentapsShoppingCart cart, ShoppingCartItem item) throws RepositoryException {
        String organizationPartyId = cart.getOrganizationPartyId();
        String accountingTagUsageTypeId = cart.isSalesOrder() ? UtilAccountingTags.SALES_ORDER_TAG : UtilAccountingTags.PURCHASE_ORDER_TAG;
        // get all tags
        Map<String, String> tags = new HashMap<String, String>();
        for (int i = 1; i <= UtilAccountingTags.TAG_COUNT; i++) {
            String tag = UtilAccountingTags.TAG_PARAM_PREFIX + i;
            tags.put(tag, (String) item.getAttribute(tag));
        }
        List<AccountingTagConfigurationForOrganizationAndUsage> missing = getOrganizationRepository().validateTagParameters(tags, organizationPartyId, accountingTagUsageTypeId, UtilAccountingTags.TAG_PARAM_PREFIX);
        // get the validated accounting tags and set them back as cart item attributes
        for (int i = 1; i <= UtilAccountingTags.TAG_COUNT; i++) {
            String tag = UtilAccountingTags.TAG_PARAM_PREFIX + i;
            item.setAttribute(tag, tags.get(tag));
        }
        return missing;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public List<AccountingTagConfigurationForOrganizationAndUsage> validateTagParameters(OpentapsShoppingCart cart, Map tags, String prefix, String productId) throws RepositoryException {
        String organizationPartyId = cart.getOrganizationPartyId();
        String accountingTagUsageTypeId = cart.isSalesOrder() ? UtilAccountingTags.SALES_ORDER_TAG : UtilAccountingTags.PURCHASE_ORDER_TAG;
        return getOrganizationRepository().validateTagParameters(tags, organizationPartyId, accountingTagUsageTypeId, prefix);
    }

    protected OrganizationRepositoryInterface getOrganizationRepository() throws RepositoryException {
        if (organizationRepository == null) {
            organizationRepository = getDomainsDirectory().getOrganizationDomain().getOrganizationRepository();
        }
        return organizationRepository;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public List<OrderItemShipGrpInvRes> getBackOrderedInventoryReservations(String productId, String facilityId) throws RepositoryException {
        String hql = "from OrderItemShipGrpInvRes eo where eo.inventoryItem.productId = :productId and eo.inventoryItem.facilityId = :facilityId and eo.quantityNotAvailable is not null order by eo.reservedDatetime, eo.sequenceId";
        Session session = null;
        try {
            session = getInfrastructure().getSession();
            Query query = session.createQuery(hql);
            query.setParameter("productId", productId);
            query.setParameter("facilityId", facilityId);
            List<org.opentaps.base.entities.OrderItemShipGrpInvRes> items = query.list();
            List<OrderItemShipGrpInvRes> resultSet = new ArrayList<OrderItemShipGrpInvRes>();
            // change entity from org.opentaps.base.entities.OrderItemShipGrpInvRes to org.opentaps.domain.order.OrderItemShipGrpInvRes
            for (org.opentaps.base.entities.OrderItemShipGrpInvRes item : items) {
                OrderItemShipGrpInvRes entity = new OrderItemShipGrpInvRes();
                entity.initRepository(this);
                entity.fromMap(item.toMap());
                resultSet.add(entity);
            }
            return resultSet;
        } catch (InfrastructureException e) {
            throw new RepositoryException(e);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    /** {@inheritDoc} */
    public void updateOrderAddress(String orderId, String contactMechId, String purposeTypeId) throws RepositoryException {
        // remove all contact mechs with given purpose for order
        List<OrderContactMech> contactMechs = findList(OrderContactMech.class,
                map(OrderContactMech.Fields.orderId, orderId, OrderContactMech.Fields.contactMechPurposeTypeId, purposeTypeId));
        for (OrderContactMech contactMech : contactMechs) {
            remove(contactMech);
        }

        if (UtilValidate.isNotEmpty(contactMechId)) {
            // add new one
            OrderContactMech orderContactMech = new OrderContactMech();
            orderContactMech.setOrderId(orderId);
            orderContactMech.setContactMechId(contactMechId);
            orderContactMech.setContactMechPurposeTypeId(purposeTypeId);
            createOrUpdate(orderContactMech);
        }
    }

    /** {@inheritDoc} */
    public ReturnItemResponse getReturnItemResponseById(String returnItemResponseId) throws RepositoryException, EntityNotFoundException {
        return findOneNotNull(ReturnItemResponse.class, map(ReturnItemResponse.Fields.returnItemResponseId, returnItemResponseId), "OpentapsError_ReturnItemResponseNotFound", UtilMisc.toMap("returnItemResponseId", returnItemResponseId));
    }
}
