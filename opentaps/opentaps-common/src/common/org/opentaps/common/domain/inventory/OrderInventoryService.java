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
package org.opentaps.common.domain.inventory;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.GenericServiceException;
import org.opentaps.domain.DomainService;
import org.opentaps.base.constants.EnumerationConstants;
import org.opentaps.base.constants.InventoryItemTypeConstants;
import org.opentaps.base.constants.ProductTypeConstants;
import org.opentaps.base.constants.StatusItemConstants;
import org.opentaps.base.entities.Facility;
import org.opentaps.base.entities.InventoryTransfer;
import org.opentaps.base.entities.OrderItemShipGrpInvRes;
import org.opentaps.base.entities.ProductFacility;
import org.opentaps.base.services.CancelOrderItemShipGrpInvResService;
import org.opentaps.base.services.CompleteInventoryTransferOldService;
import org.opentaps.base.services.CreateInventoryItemDetailService;
import org.opentaps.base.services.CreateInventoryItemService;
import org.opentaps.base.services.DecomposeInventoryItemService;
import org.opentaps.base.services.GetOrderItemShipGroupEstimatedShipDateService;
import org.opentaps.base.services.ReserveOrderItemInventoryService;
import org.opentaps.base.services.UpdateInventoryTransferService;
import org.opentaps.domain.inventory.InventoryDomainInterface;
import org.opentaps.domain.inventory.InventoryItem;
import org.opentaps.domain.inventory.InventoryRepositoryInterface;
import org.opentaps.domain.inventory.InventoryRepositoryInterface.FacilityLocationType;
import org.opentaps.domain.inventory.InventoryRepositoryInterface.InventoryReservationOrder;
import org.opentaps.domain.inventory.OrderInventoryServiceInterface;
import org.opentaps.domain.inventory.PicklistAndBinAndItem;
import org.opentaps.domain.order.Order;
import org.opentaps.domain.order.OrderDomainInterface;
import org.opentaps.domain.order.OrderItemShipGroup;
import org.opentaps.domain.order.OrderRepositoryInterface;
import org.opentaps.domain.product.Product;
import org.opentaps.domain.product.ProductDomainInterface;
import org.opentaps.domain.product.ProductRepositoryInterface;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.ofbiz.Repository;
import org.opentaps.foundation.service.ServiceException;


/** {@inheritDoc} */
public class OrderInventoryService extends DomainService implements OrderInventoryServiceInterface {

    private static final String MODULE = OrderInventoryServiceInterface.class.getName();

    private String inventoryTransferId;

    private String inventoryItemId;
    private String priorityOrderId;
    private String priorityOrderItemSeqId;

    private String productId;
    private String orderId;
    private String orderItemSeqId;
    private String shipGroupSeqId;
    private BigDecimal quantity;
    private Timestamp reservedDatetime;
    private String requireInventory;
    private String reserveOrderEnumId;
    private Long sequenceId;
    private String priority;
    private String facilityId;
    private String containerId;
    private BigDecimal quantityNotReserved;
    private boolean ignoreAddressFacilitySetting = false;

    /**
     * Default constructor.
     */
    public OrderInventoryService() {
        super();
    }

    /**
     * Constructor with domain objects.
     * @param infrastructure an <code>Infrastructure</code> value
     * @param user an <code>User</code> value
     * @param locale a <code>Locale</code> value
     * @throws ServiceException if an error occurs
     */
    public OrderInventoryService(Infrastructure infrastructure, User user, Locale locale) throws ServiceException {
        super(infrastructure, user, locale);
    }

    /** {@inheritDoc} */
    public void setInventoryItemId(String inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
    }

    /** {@inheritDoc} */
    public void setPriorityOrderId(String priorityOrderId) {
        this.priorityOrderId = priorityOrderId;
    }

    /** {@inheritDoc} */
    public void setPriorityOrderItemSeqId(String priorityOrderItemSeqId) {
        this.priorityOrderItemSeqId = priorityOrderItemSeqId;
    }

    /** {@inheritDoc} */
    public void balanceInventoryItems() throws ServiceException {
        try {
            InventoryDomainInterface inventoryDomain = getDomainsDirectory().getInventoryDomain();
            InventoryRepositoryInterface inventoryRepository = inventoryDomain.getInventoryRepository();

            InventoryItem item = inventoryRepository.getInventoryItemById(this.inventoryItemId);

            List<InventoryItem> itemsWithNegativeATP = item.getSimilarInventoryItemsWithNegativeATP();

            // add the current inventory item to the list even if it has positive ATP, because there could be backorders on it
            // and a the variance might have changed its ATP from negative to positive
            if (!itemsWithNegativeATP.contains(item)) {
                itemsWithNegativeATP.add(item);
            }

            // reservations will get classified among those two lists
            List<OrderItemShipGrpInvRes> normalReservations = FastList.newInstance();
            List<OrderItemShipGrpInvRes> privilegedReservations = FastList.newInstance();

            for (InventoryItem i : itemsWithNegativeATP) {
                List<OrderItemShipGrpInvRes> reservations = i.getOrderItemShipGroupInventoryReservations();

                for (OrderItemShipGrpInvRes res : reservations) {
                    List<PicklistAndBinAndItem> picklistItems = inventoryRepository.getOpenPicklistBinItems(res);
                    // only process the reservations that are not in a open picklist
                    if (!picklistItems.isEmpty()) {
                        continue;
                    }

                    Debug.logInfo("balanceInventoryItems: Order #" + res.getOrderId() + " was not found on any picklist for this item [" + res.getInventoryItemId() + "]", MODULE);
                    if (res.getOrderId() == priorityOrderId && res.getOrderItemSeqId() == priorityOrderItemSeqId) {
                        privilegedReservations.add(res);
                    } else {
                        normalReservations.add(res);
                    }
                }
            }

            // if no reservations remain stop here
            if (privilegedReservations.isEmpty() && normalReservations.isEmpty()) {
                Debug.logInfo("balanceInventoryItems: No reservation found related to inventory item: " + item, MODULE);
                return;
            }

            // sort both list by reserved date and sequence id, then concatenate them in sortedReservations list
            Comparator<OrderItemShipGrpInvRes> reservationComparator = new Comparator<OrderItemShipGrpInvRes>() {
                public int compare(OrderItemShipGrpInvRes a, OrderItemShipGrpInvRes b) {
                    int compareDate = a.getReservedDatetime().compareTo(b.getReservedDatetime());
                    if (compareDate != 0) {
                        return compareDate;
                    }
                    // for equal date compare the sequence id (they might be null)
                    Long seqA = a.getSequenceId();
                    Long seqB = b.getSequenceId();
                    if (seqA != null) {
                        return seqA.compareTo(seqB);
                    } else if (seqB != null) {
                        return seqB.compareTo(seqA);
                    }
                    // if both sequence null
                    return 0;
                }
            };
            Collections.sort(privilegedReservations, reservationComparator);
            Collections.sort(normalReservations, reservationComparator);
            List<OrderItemShipGrpInvRes> sortedReservations = FastList.newInstance();
            sortedReservations.addAll(privilegedReservations);
            sortedReservations.addAll(normalReservations);

            // if the inventory item is serialized we just need the first reservation
            if (item.isSerialized()) {
                OrderItemShipGrpInvRes o = sortedReservations.get(0);
                sortedReservations = FastList.newInstance();
                sortedReservations.add(o);
            }

            // Cancel all reservations
            for (OrderItemShipGrpInvRes res : sortedReservations) {
                Debug.logInfo("balanceInventoryItems: Canceling reservation on inventory [" + res.getInventoryItemId() + "] for product [" + item.getProductId() + "] for order item [" + res.getOrderId() + ":" + res.getOrderItemSeqId() + "] quantity [" + res.getQuantity() + "]; facility [" + item.getFacilityId() + "]", MODULE);
                CancelOrderItemShipGrpInvResService service = new CancelOrderItemShipGrpInvResService();
                service.setInOrderId(res.getOrderId());
                service.setInOrderItemSeqId(res.getOrderItemSeqId());
                service.setInInventoryItemId(res.getInventoryItemId());
                service.setInShipGroupSeqId(res.getShipGroupSeqId());
                runSync(service);
            }

            // re reserve the canceled items
            for (OrderItemShipGrpInvRes res : sortedReservations) {
                setOrderId(res.getOrderId());
                setOrderItemSeqId(res.getOrderItemSeqId());
                setProductId(item.getProductId());
                setQuantity(res.getQuantity());
                setReservedDatetime(res.getReservedDatetime());
                setReserveOrderEnumId(res.getReserveOrderEnumId());
                setRequireInventory("N");
                setSequenceId(res.getSequenceId());
                setShipGroupSeqId(res.getShipGroupSeqId());
                Debug.logInfo("balanceInventoryItems: Re-reserving product [" + item.getProductId() + "] for order item [" + res.getOrderId() + ":" + res.getOrderItemSeqId() + "] quantity [" + res.getQuantity() + "]; facility [" + item.getFacilityId() + "]", MODULE);
                if (UtilValidate.isNotEmpty(item.getFacilityId())) {
                    setFacilityId(item.getFacilityId());
                    // to be sure the re reservation is on the same facility
                    ignoreAddressFacilitySetting = true;
                } else {
                    Debug.logWarning("balanceInventoryItems: In balanceInventoryItems there is no facilityId, so reserving from any facility for order item [" + res.getOrderId() + ":" + res.getOrderItemSeqId() + "]", MODULE);
                    // this can reserve against any facility
                    ignoreAddressFacilitySetting = false;
                }
                reserveProductInventory();
            }

        } catch (GeneralException ex) {
            throw new ServiceException(ex);
        }
    }

    /** {@inheritDoc} */
    public void setProductId(String productId) {
        this.productId = productId;
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
    public void setShipGroupSeqId(String shipGroupSeqId) {
        this.shipGroupSeqId = shipGroupSeqId;
    }

    /** {@inheritDoc} */
    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    /** {@inheritDoc} */
    public void setReservedDatetime(Timestamp reservedDatetime) {
        this.reservedDatetime = reservedDatetime;
    }

    /** {@inheritDoc} */
    public void setRequireInventory(String requireInventory) {
        this.requireInventory = requireInventory;
    }

    /** {@inheritDoc} */
    public void setReserveOrderEnumId(String reserveOrderEnumId) {
        this.reserveOrderEnumId = reserveOrderEnumId;
    }

    /** {@inheritDoc} */
    public void setSequenceId(Long sequenceId) {
        this.sequenceId = sequenceId;
    }

    /** {@inheritDoc} */
    public BigDecimal getQuantityNotReserved() {
        return quantityNotReserved;
    }

    /** {@inheritDoc} */
    public void setFacilityId(String facilityId) {
        this.facilityId = facilityId;
    }

    /** {@inheritDoc} */
    public void setPriority(String priority) {
        this.priority = priority;
    }

    /**
     * Calculates the promiseDatetime for specified inventory item.
     *
     * @param inventoryItem an <code>InventoryItem</code> value
     * @param orderDate a <code>Timestamp</code> value
     * @param repo an <code>InventoryRepositoryInterface</code> value
     * @return
     *     Number of days to ship defined for product or, if it doesn't exist for product, facility.
     *     Returns default 30 days if value is specified neither for product nor facility.
     * @throws RepositoryException if an error occurs
     */
    private Timestamp calculatePromisedDatetime(InventoryItem inventoryItem, Timestamp orderDate, InventoryRepositoryInterface repo) throws RepositoryException {
        Long daysToShip = null;
        try {
            ProductFacility productFacility = inventoryItem.getProductFacility();
            if (productFacility != null) {
                daysToShip = productFacility.getDaysToShip();
            }
        } catch (RepositoryException e) {
            Debug.logWarning(e, MODULE);
        }

        if (daysToShip == null) {
            try {
                daysToShip = inventoryItem.getFacility().getDefaultDaysToShip();
            } catch (RepositoryException e) {
                Debug.logWarning(e, MODULE);
            }
        }
        if (daysToShip == null) {
            daysToShip = Long.valueOf(30);
        }

        return UtilDateTime.adjustTimestamp(orderDate, Calendar.DAY_OF_YEAR, daysToShip.intValue(), timeZone, locale);
    }

    /**
     * Reserves certain inventory item.
     *
     * @param inventoryItem InventoryItem object
     * @param order Order object
     * @param repo Inventory repository
     * @return
     *    Last non-serialized inventory item for use if inventory is not required for purchase.
     * @throws GenericServiceException
     * @throws RepositoryException
     * @throws GenericEntityException
     * @throws ServiceException
     */
    private InventoryItem reserveInventoryItem(InventoryItem inventoryItem, Order order, InventoryRepositoryInterface repo) throws GenericServiceException, RepositoryException, GenericEntityException, ServiceException {

        // only do something with this inventoryItem if there is more inventory to reserve
        if (quantityNotReserved.compareTo(BigDecimal.ZERO) > 0) {
            // get the promiseDatetime
            Timestamp promisedDatetime = calculatePromisedDatetime(inventoryItem, order.getOrderDate(), repo);
            Debug.logInfo("reserveInventoryItem: still has " + quantityNotReserved + " to reserve.", MODULE);

            if (inventoryItem.isSerialized()) {
                if (inventoryItem.isAvailableToPromise()) {
                    // change status on inventoryItem
                    inventoryItem.setStatusId(StatusItemConstants.InvSerializedStts.INV_PROMISED);
                    GenericValue invItemValue = Repository.genericValueFromEntity(getInfrastructure().getDelegator(), inventoryItem);
                    invItemValue.store();

                    // store OrderItemShipGrpInvRes record
                    ReserveOrderItemInventoryService service = new ReserveOrderItemInventoryService();
                    service.setInOrderId(orderId);
                    service.setInOrderItemSeqId(orderItemSeqId);
                    service.setInShipGroupSeqId(shipGroupSeqId);
                    service.setInInventoryItemId(inventoryItem.getInventoryItemId());
                    service.setInReserveOrderEnumId(reserveOrderEnumId);
                    service.setInReservedDatetime(reservedDatetime);
                    service.setInPromisedDatetime(promisedDatetime);
                    service.setInQuantity(BigDecimal.ONE);
                    if (UtilValidate.isNotEmpty(sequenceId)) {
                        service.setInSequenceId(sequenceId);
                    }
                    Debug.logInfo("reserveInventoryItem: reserving 1 SERIALIZED_INV_ITEM of " + inventoryItem.getProductId() + " on inventory " + inventoryItem.getInventoryItemId() + ".", MODULE);
                    runSync(service);

                    quantityNotReserved = quantityNotReserved.subtract(BigDecimal.ONE);
                }
            } else { // non-serialized
                if (UtilValidate.isNotEmpty(inventoryItem.getAvailableToPromiseTotal())) {
                    // reduce ATP on inventoryItem if availableToPromise greater than 0, if not the code at the end of this method will handle it
                    if (inventoryItem.getAvailableToPromiseTotal().compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal deductAmount;
                        if (quantityNotReserved.compareTo(inventoryItem.getAvailableToPromiseTotal()) > 0) {
                            deductAmount = inventoryItem.getAvailableToPromiseTotal();
                        } else {
                            deductAmount = quantityNotReserved;
                        }

                        // instead of updating InventoryItem, add an InventoryItemDetail
                        CreateInventoryItemDetailService service = new CreateInventoryItemDetailService();
                        service.setInOrderId(orderId);
                        service.setInOrderItemSeqId(orderItemSeqId);
                        service.setInInventoryItemId(inventoryItem.getInventoryItemId());
                        service.setInAvailableToPromiseDiff(deductAmount.negate());
                        runSync(service);

                        // create OrderItemShipGrpInvRes record
                        ReserveOrderItemInventoryService service2 = new ReserveOrderItemInventoryService();
                        service2.setInOrderId(orderId);
                        service2.setInOrderItemSeqId(orderItemSeqId);
                        service2.setInInventoryItemId(inventoryItem.getInventoryItemId());
                        service2.setInShipGroupSeqId(shipGroupSeqId);
                        service2.setInReserveOrderEnumId(reserveOrderEnumId);
                        service2.setInReservedDatetime(reservedDatetime);
                        service2.setInQuantity(deductAmount);
                        service2.setInPromisedDatetime(promisedDatetime);
                        if (UtilValidate.isNotEmpty(sequenceId)) {
                            service2.setInSequenceId(sequenceId);
                        }
                        Debug.logInfo("reserveInventoryItem: reserving " + deductAmount + " NON_SERIAL_INV_ITEM of " + inventoryItem.getProductId() + " on inventory " + inventoryItem.getInventoryItemId() + ".", MODULE);
                        runSync(service2);

                        quantityNotReserved = quantityNotReserved.subtract(deductAmount);
                    }
                }

                // keep track of the last non-serialized inventory item for use if inventory is not required for purchase
                return inventoryItem;
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    public void reserveProductInventory() throws ServiceException {

        try {

            ProductDomainInterface productDomain = getDomainsDirectory().getProductDomain();
            ProductRepositoryInterface productRepository = productDomain.getProductRepository();

            OrderDomainInterface orderDomain = getDomainsDirectory().getOrderDomain();
            OrderRepositoryInterface orderRepository = orderDomain.getOrderRepository();

            InventoryDomainInterface inventoryDomain = getDomainsDirectory().getInventoryDomain();
            InventoryRepositoryInterface inventoryRepository = inventoryDomain.getInventoryRepository();

            Product product = productRepository.getProductById(productId);

            // skip non-physical products
            if (!product.isPhysical()) {
                quantityNotReserved = BigDecimal.ZERO;
                return;
            }

            Order order = orderRepository.getOrderById(orderId);

            /*
             * Check first to see if the item shipping destination is to
             * be reserved from a specific facility.
             */
            Facility facility = null;
            if (!ignoreAddressFacilitySetting) {
                OrderItemShipGroup shipGroup = order.getOrderItemShipGroup(shipGroupSeqId);
                facility = orderRepository.getProductStoreFacilityByAddress(shipGroup, order.getProductStore());
                if (facility != null) {
                    setFacilityId(facility.getFacilityId());
                }
            }

            if (facility == null && UtilValidate.isNotEmpty(facilityId)) {
                facility = inventoryRepository.getFacilityById(facilityId);
            }

            // Use facility reserve order if it exists
            if (facility != null) {
                String facilityReserveOrderId = facility.getInventoryReserveOrderEnumId();
                if (UtilValidate.isNotEmpty(facilityReserveOrderId) && !facilityReserveOrderId.equals(reserveOrderEnumId)) {
                    setReserveOrderEnumId(facilityReserveOrderId);
                    Debug.logInfo("Reserve order of facility [" + facility.getFacilityId() + "] overrides one from attribute. Actual order is " + facilityReserveOrderId, MODULE);
                }
            }

            /*
             * before we do the find, put together the orderBy list based on which reserveOrderEnumId
             * is specified.
             */
            InventoryReservationOrder reservationOrder = null;
            if (EnumerationConstants.InvResOrder.INVRO_FIFO_REC.equals(reserveOrderEnumId)) {
                reservationOrder = InventoryReservationOrder.FIFO_RECEIVED;
            } else if (EnumerationConstants.InvResOrder.INVRO_GUNIT_COST.equals(reserveOrderEnumId)) {
                reservationOrder = InventoryReservationOrder.GREATER_UNIT_COST;
            } else if (EnumerationConstants.InvResOrder.INVRO_LUNIT_COST.equals(reserveOrderEnumId)) {
                reservationOrder = InventoryReservationOrder.LESS_UNIT_COST;
            } else if (EnumerationConstants.InvResOrder.INVRO_FIFO_EXP.equals(reserveOrderEnumId)) {
                reservationOrder = InventoryReservationOrder.FIFO_EXPIRE;
            } else if (EnumerationConstants.InvResOrder.INVRO_LIFO_EXP.equals(reserveOrderEnumId)) {
                reservationOrder = InventoryReservationOrder.LIFO_EXPIRE;
            } else if (EnumerationConstants.InvResOrder.INVRO_LIFO_REC.equals(reserveOrderEnumId)) {
                reservationOrder = InventoryReservationOrder.LIFO_RECEIVED;
            } else {
                reserveOrderEnumId = EnumerationConstants.InvResOrder.INVRO_FIFO_REC;
                reservationOrder = InventoryReservationOrder.FIFO_RECEIVED;
            }

            quantityNotReserved = quantity;
            InventoryItem lastNonSerInventoryItem = null;

            Debug.logInfo("reserveProductInventory: parameters for inventory items lookup, product [" + product.getProductId() + "] reservationOrder [" + reservationOrder + "] facilityId [" + facilityId + "] containerId [" + containerId + "]", MODULE);

            /*
             * first reserve against InventoryItems in FLT_PICKLOC type locations,
             * then FLT_BULK locations, then InventoryItems with no locations.
             */
            Debug.logInfo("reserveProductInventory: still has " + quantityNotReserved + " looking for inventory in PRIMARY locations", MODULE);
            List<InventoryItem> inventoryItems = inventoryRepository.getInventoryItems(product, FacilityLocationType.PRIMARY, reservationOrder, facilityId, containerId);
            if (UtilValidate.isNotEmpty(inventoryItems)) {
                for (InventoryItem inventory : inventoryItems) {
                    lastNonSerInventoryItem = reserveInventoryItem(inventory, order, inventoryRepository);
                }
            }

            // try the FLT_BULK locations if we need more items
            Debug.logInfo("reserveProductInventory: still has " + quantityNotReserved + " looking for inventory in BULK locations", MODULE);
            if (quantityNotReserved.compareTo(BigDecimal.ZERO) > 0) {
                inventoryItems = inventoryRepository.getInventoryItems(product, FacilityLocationType.BULK, reservationOrder, facilityId, containerId);
                if (UtilValidate.isNotEmpty(inventoryItems)) {
                    for (InventoryItem inventory : inventoryItems) {
                        lastNonSerInventoryItem = reserveInventoryItem(inventory, order, inventoryRepository);
                    }
                }
            }

            Debug.logInfo("reserveProductInventory: still has " + quantityNotReserved + " looking for inventory in other locations", MODULE);
            if (quantityNotReserved.compareTo(BigDecimal.ZERO) > 0) {
                inventoryItems = inventoryRepository.getInventoryItems(product, null, reservationOrder, facilityId, containerId);
                if (UtilValidate.isNotEmpty(inventoryItems)) {
                    for (InventoryItem inventory : inventoryItems) {
                        lastNonSerInventoryItem = reserveInventoryItem(inventory, order, inventoryRepository);
                    }
                }
            }
            Debug.logInfo("reserveProductInventory: still has " + quantityNotReserved, MODULE);

            // NOTE: We're not rounding inventory quantities right now to allow for fractional inventory counts

            /*
             * if inventory is not required for purchase and quantityNotReserved != 0:
             *   - subtract the remaining quantityNotReserved from the availableToPromise of the last non-serialized inventory item
             *   - or if none was found create a non-ser InventoryItem with availableToPromise = -quantityNotReserved
             */

            if (quantityNotReserved.compareTo(BigDecimal.ZERO) != 0 && !requireInventory.equals("Y")) {
                if (UtilValidate.isNotEmpty(lastNonSerInventoryItem)) {
                    Debug.logInfo("reserveProductInventory: still has " + quantityNotReserved + ", now using the last found inventory: " + lastNonSerInventoryItem.getInventoryItemId(), MODULE);

                    // check if the inventory item is pending transfer, and if so do not reserve this inventory item
                    BigDecimal pendingTransferQty = lastNonSerInventoryItem.getPendingInventoryTransferQuantity();
                    Debug.logVerbose("For lastNonSerInventoryItem: " + lastNonSerInventoryItem, MODULE);
                    Debug.logVerbose("quantityNotReserved = " + quantityNotReserved, MODULE);
                    Debug.logVerbose("pendingTransferQty = " + pendingTransferQty, MODULE);
                    Debug.logVerbose("qoh = " + lastNonSerInventoryItem.getNetQOH(), MODULE);

                    BigDecimal toReserve;
                    // if there are pending transfers reserve up to the remaining QOH else it is possible to over reserve on that inventory item
                    if (pendingTransferQty.compareTo(BigDecimal.ZERO) > 0) {
                        toReserve = lastNonSerInventoryItem.getNetQOH().subtract(pendingTransferQty);
                        toReserve = toReserve.min(quantityNotReserved);
                    } else {
                        toReserve = quantityNotReserved;
                    }
                    Debug.logVerbose("toReserve (backordered) = " + toReserve, MODULE);

                    if (toReserve.compareTo(BigDecimal.ZERO) > 0) {
                        // subtract quantityNotReserved from the availableToPromise of existing inventory item
                        // instead of updating InventoryItem, add an InventoryItemDetail
                        CreateInventoryItemDetailService service = new CreateInventoryItemDetailService();
                        service.setInOrderId(orderId);
                        service.setInOrderItemSeqId(orderItemSeqId);
                        service.setInInventoryItemId(lastNonSerInventoryItem.getInventoryItemId());
                        service.setInShipGroupSeqId(shipGroupSeqId);
                        service.setInAvailableToPromiseDiff(toReserve.negate());
                        runSync(service);

                        // calculate the promiseDatetime
                        Timestamp promisedDatetime = calculatePromisedDatetime(lastNonSerInventoryItem, order.getOrderDate(), inventoryRepository);

                        // reserve order item inventory
                        ReserveOrderItemInventoryService service2 = new ReserveOrderItemInventoryService();
                        service2.setInOrderId(orderId);
                        service2.setInOrderItemSeqId(orderItemSeqId);
                        service2.setInInventoryItemId(lastNonSerInventoryItem.getInventoryItemId());
                        service2.setInShipGroupSeqId(shipGroupSeqId);
                        service2.setInReserveOrderEnumId(reserveOrderEnumId);
                        service2.setInReservedDatetime(reservedDatetime);
                        service2.setInQuantity(toReserve);
                        service2.setInQuantityNotAvailable(toReserve);
                        service2.setInPromisedDatetime(promisedDatetime);
                        service2.setInPriority(priority);
                        service2.setInSequenceId(sequenceId);
                        runSync(service2);

                        quantityNotReserved = quantityNotReserved.subtract(toReserve);
                    }

                }
                // if still has quantityNotReserved
                if (quantityNotReserved.compareTo(BigDecimal.ZERO) != 0) {
                    // no non-serial inventory item, create a non-serial InventoryItem with availableToPromise = -quantityNotReserved
                    Map<String, Object> createInventoryItemInMap = FastMap.newInstance();

                    // the createInventoryItem service is run by the the system user here
                    CreateInventoryItemService service = new CreateInventoryItemService();
                    service.setInProductId(productId);
                    if (UtilValidate.isNotEmpty(facilityId)) {
                        service.setInFacilityId(facilityId);
                    }
                    service.setInInventoryItemTypeId(InventoryItemTypeConstants.NON_SERIAL_INV_ITEM);
                    runSync(service, getInfrastructure().getSystemUser());

                    String inventoryItemOutId = service.getOutInventoryItemId();
                    InventoryItem newNonSerInventoryItem = inventoryRepository.getInventoryItemById(inventoryItemOutId);

                    // also create a detail record with the quantities
                    CreateInventoryItemDetailService service2 = new CreateInventoryItemDetailService();
                    service2.setInOrderId(orderId);
                    service2.setInOrderItemSeqId(orderItemSeqId);
                    service2.setInInventoryItemId(newNonSerInventoryItem.getInventoryItemId());
                    service2.setInShipGroupSeqId(shipGroupSeqId);
                    service2.setInAvailableToPromiseDiff(quantityNotReserved.negate());
                    runSync(service2);

                    // calculate the promiseDatetime
                    Timestamp promisedDatetime = calculatePromisedDatetime(newNonSerInventoryItem, order.getOrderDate(), inventoryRepository);

                    // create OrderItemShipGrpInvRes record
                    ReserveOrderItemInventoryService service3 = new ReserveOrderItemInventoryService();
                    service3.setInOrderId(orderId);
                    service3.setInOrderItemSeqId(orderItemSeqId);
                    service3.setInInventoryItemId(newNonSerInventoryItem.getInventoryItemId());
                    service3.setInShipGroupSeqId(shipGroupSeqId);
                    service3.setInReserveOrderEnumId(reserveOrderEnumId);
                    service3.setInReservedDatetime(reservedDatetime);
                    service3.setInQuantity(quantityNotReserved);
                    service3.setInQuantityNotAvailable(quantityNotReserved);
                    service3.setInPromisedDatetime(promisedDatetime);
                    service3.setInPriority(priority);
                    service3.setInSequenceId(sequenceId);
                    runSync(service3);

                }
            }

        } catch (RepositoryException e) {
            throw new ServiceException(e.getMessage());
        } catch (EntityNotFoundException e) {
            throw new ServiceException(e.getMessage());
        } catch (GenericServiceException e) {
            throw new ServiceException(e.getMessage());
        } catch (GenericEntityException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    /** {@inheritDoc} */
    public void reReserveProductInventory() throws ServiceException {

        try {
            BigDecimal originalQuantity = quantity;
            String toFacilityId = facilityId;

            InventoryDomainInterface inventoryDomain = getDomainsDirectory().getInventoryDomain();
            InventoryRepositoryInterface inventoryRepository = inventoryDomain.getInventoryRepository();

            List<OrderItemShipGrpInvRes> reservations =
                inventoryRepository.getOrderItemShipGroupInventoryReservations(orderId, orderItemSeqId, inventoryItemId, shipGroupSeqId);

            // cancel reservations
            for (OrderItemShipGrpInvRes reservItem : reservations) {
                CancelOrderItemShipGrpInvResService service = new CancelOrderItemShipGrpInvResService(getUser());
                service.setInOrderId(reservItem.getOrderId());
                service.setInOrderItemSeqId(reservItem.getOrderItemSeqId());
                service.setInInventoryItemId(reservItem.getInventoryItemId());
                service.setInShipGroupSeqId(reservItem.getShipGroupSeqId());
                service.runSync(getInfrastructure());
                if (service.isError()) {
                    throw new ServiceException(service.getErrorMessage());
                }
            }

            BigDecimal remainderQty = originalQuantity;

            // iterate over reservations and renew them redistributing between previous or new facility,
            // depending on requested quantity.
            for (OrderItemShipGrpInvRes reservItem : reservations) {
                BigDecimal quantityFromNewFacility = BigDecimal.ZERO;
                BigDecimal quantityFromOldFacility = null;
                org.opentaps.base.entities.InventoryItem inventory = reservItem.getInventoryItem();

                if (remainderQty.compareTo(reservItem.getQuantity()) < 0) {
                    // specified quantity less than reserved earlier, we have to reserve
                    // this quantity of product from new facility and rest of product
                    // from previous facility.
                    quantityFromNewFacility = remainderQty;
                    quantityFromOldFacility = reservItem.getQuantity().subtract(remainderQty);
                } else if (remainderQty.compareTo(reservItem.getQuantity()) == 0) {
                    // all quantity of product should be reserved from new facility
                    quantityFromNewFacility = originalQuantity;
                } else {
                    // we can't request re-reservation for quantity of product that greater than
                    // original value.
                    throw new ServiceException("OpentapsError_ReservedMoreThanRequested", null);
                }

                // While re-reservation reserveProductInventory service has to
                // ignore facility associated with Ship-to address of the order.
                // Set ignoreAddressFacilitySetting flag to achieve this.
                ignoreAddressFacilitySetting = true;

                // reserve required quantity of a product from new facility
                setProductId(inventory.getProductId());
                setFacilityId(toFacilityId);
                setShipGroupSeqId(reservItem.getShipGroupSeqId());
                setQuantity(quantityFromNewFacility);
                setReservedDatetime(reservItem.getReservedDatetime());
                setRequireInventory("N");
                setReserveOrderEnumId(reservItem.getReserveOrderEnumId());
                setSequenceId(reservItem.getSequenceId());
                reserveProductInventory();

                if (quantityFromOldFacility != null) {
                    // renew reservation for remaining quantity from original facility
                    setFacilityId(inventory.getFacilityId());
                    setQuantity(quantityFromOldFacility);
                    reserveProductInventory();
                }

                remainderQty = remainderQty.subtract(quantityFromNewFacility);
            }

        } catch (RepositoryException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    /** {@inheritDoc} */
    public void setInventoryTransferId(String inventoryTransferId) {
        this.inventoryTransferId = inventoryTransferId;
    }

    /** {@inheritDoc} */
    public void completeInventoryTransfer() throws ServiceException {
        // we need to be sure the balanceInventoryItems service is called when the transfer is completed
        // normally completeInventoryTransfer and balanceInventoryItems are both called as secas when the inventoryTransfer status
        // is set to COMPLETED
        // so this checks the current status, and either call updateInventoryTransfer or the old completeInventoryTransfer service
        try {
            InventoryDomainInterface inventoryDomain = getDomainsDirectory().getInventoryDomain();
            InventoryRepositoryInterface inventoryRepository = inventoryDomain.getInventoryRepository();
            InventoryTransfer transfer = inventoryRepository.getInventoryTransferById(inventoryTransferId);

            if (transfer.getStatusId().equals(StatusItemConstants.InventoryXferStts.IXF_COMPLETE)) {
                CompleteInventoryTransferOldService service = new CompleteInventoryTransferOldService(getUser());
                service.setInInventoryTransferId(inventoryTransferId);
                service.runSync(getInfrastructure());
            } else {
                UpdateInventoryTransferService service = new UpdateInventoryTransferService(getUser());
                service.setInInventoryTransferId(inventoryTransferId);
                service.setInStatusId(StatusItemConstants.InventoryXferStts.IXF_COMPLETE);
                service.setInInventoryItemId(transfer.getInventoryItemId());
                service.runSync(getInfrastructure());
            }

        } catch (GeneralException ex) {
            throw new ServiceException(ex);
        }
    }

    /** {@inheritDoc} */
    public void disassemblePurchasingPackage() throws ServiceException {
        try {
            InventoryDomainInterface inventoryDomain = getDomainsDirectory().getInventoryDomain();
            InventoryRepositoryInterface inventoryRepository = inventoryDomain.getInventoryRepository();
            InventoryItem item = inventoryRepository.getInventoryItemById(this.inventoryItemId);
            // if received PURCH_PKG_AUTO product into invertorym, then split it to components
            if (ProductTypeConstants.Good.PURCH_PKG_AUTO.equals(item.getProduct().getProductTypeId())) {
                DecomposeInventoryItemService service = new DecomposeInventoryItemService(getUser());
                service.setInInventoryItemId(this.inventoryItemId);
                service.runSync(getInfrastructure());
            }
        } catch (GeneralException ex) {
            throw new ServiceException(ex);
        }
    }

    /** {@inheritDoc} */
    public void setOrderItemShipGroupEstimatedShipDate() throws ServiceException {
        try {
            OrderDomainInterface orderDomain = getDomainsDirectory().getOrderDomain();
            OrderRepositoryInterface orderRepository = orderDomain.getOrderRepository();
            // get the ship group to update, this should fail it the ship group does not exists
            Order order = orderRepository.getOrderById(orderId);
            OrderItemShipGroup shipGroup = order.getOrderItemShipGroup(shipGroupSeqId);
            // calculate the ship group estimatedShipDate from all its reservations
            GetOrderItemShipGroupEstimatedShipDateService service = new GetOrderItemShipGroupEstimatedShipDateService(getUser());
            service.setInOrderId(orderId);
            service.setInShipGroupSeqId(shipGroupSeqId);
            service.runSync(getInfrastructure());
            Timestamp estimatedShipDate = service.getOutEstimatedShipDate();

            // update the ship group estimatedShipDate
            shipGroup.setEstimatedShipDate(estimatedShipDate);
            orderRepository.update(shipGroup);
        } catch (GeneralException ex) {
            throw new ServiceException(ex);
        }
    }
}
