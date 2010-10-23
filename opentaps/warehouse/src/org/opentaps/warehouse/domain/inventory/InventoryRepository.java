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
package org.opentaps.warehouse.domain.inventory;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
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
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityFindOptions;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.base.entities.Facility;
import org.opentaps.base.entities.InventoryItemAndLocation;
import org.opentaps.base.entities.InventoryItemDetail;
import org.opentaps.base.entities.InventoryItemTraceDetail;
import org.opentaps.base.entities.InventoryItemValueHistory;
import org.opentaps.base.entities.InventoryItemVariance;
import org.opentaps.base.entities.InventoryTransfer;
import org.opentaps.base.entities.ItemIssuance;
import org.opentaps.base.entities.Lot;
import org.opentaps.base.entities.OrderItemShipGrpInvRes;
import org.opentaps.base.entities.PhysicalInventory;
import org.opentaps.base.entities.WorkEffort;
import org.opentaps.base.entities.WorkEffortInventoryAssign;
import org.opentaps.base.entities.WorkEffortInventoryProduced;
import org.opentaps.domain.inventory.InventoryItem;
import org.opentaps.domain.inventory.InventoryItemTrace;
import org.opentaps.domain.inventory.InventoryRepositoryInterface;
import org.opentaps.domain.inventory.PicklistAndBinAndItem;
import org.opentaps.domain.product.Product;
import org.opentaps.domain.product.ProductRepositoryInterface;
import org.opentaps.foundation.entity.Entity;
import org.opentaps.foundation.entity.EntityFieldInterface;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.ofbiz.Repository;


/** {@inheritDoc} */
public class InventoryRepository extends Repository implements InventoryRepositoryInterface {

    private ProductRepositoryInterface productRepository;
    private static final String MODULE = InventoryRepository.class.getName();

    /**
     * Default constructor.
     */
    public InventoryRepository() {
        super();
    }

    /**
     * Use this for Repositories which will only access the database via the delegator.
     * @param delegator the delegator
     */
    public InventoryRepository(Delegator delegator) {
        super(delegator);
    }

    /**
     * Use this for domain Repositories.
     * @param infrastructure the domain infrastructure
     * @param user the domain user
     * @throws RepositoryException if an error occurs
     */
    public InventoryRepository(Infrastructure infrastructure, User user) throws RepositoryException {
        super(infrastructure, user);
    }

    /** {@inheritDoc} */
    public org.opentaps.domain.inventory.InventoryItem getInventoryItemById(String inventoryItemId) throws RepositoryException, EntityNotFoundException {
        return (org.opentaps.domain.inventory.InventoryItem) getInventoryItemById(inventoryItemId, org.opentaps.domain.inventory.InventoryItem.class);
    }

    /** {@inheritDoc} */
    public org.opentaps.base.entities.InventoryItem getInventoryItemById(
            String inventoryItemId,
            Class<? extends org.opentaps.base.entities.InventoryItem> clazz)
            throws RepositoryException, EntityNotFoundException {

        return findOneNotNull(clazz, map(org.opentaps.base.entities.InventoryItem.Fields.inventoryItemId, inventoryItemId), "InventoryItem [" + inventoryItemId + "] not found");
    }

    /** {@inheritDoc} */
    public Facility getFacilityById(String facilityId) throws RepositoryException, EntityNotFoundException {
        return findOneNotNull(Facility.class, map(Facility.Fields.facilityId, facilityId), "Facility entity [" + facilityId + "] not found");
    }

    /** {@inheritDoc} */
    public List<InventoryItem> getInventoryItemsForProductId(String productId) throws RepositoryException {
        return findList(InventoryItem.class, map(InventoryItem.Fields.productId, productId));
    }

    /** {@inheritDoc} */
    public List<InventoryItem> getInventoryItemsWithNegativeATP(String facilityId, String productId) throws RepositoryException {
        EntityCondition conditions1 = EntityCondition.makeCondition(EntityOperator.OR,
                EntityCondition.makeCondition("availableToPromiseTotal", EntityOperator.EQUALS, null),
                EntityCondition.makeCondition("availableToPromiseTotal", EntityOperator.LESS_THAN, BigDecimal.ZERO));
        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                conditions1,
                EntityCondition.makeCondition("facilityId", EntityOperator.EQUALS, facilityId),
                EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId),
                EntityCondition.makeCondition("inventoryItemTypeId", EntityOperator.EQUALS, "NON_SERIAL_INV_ITEM"));
        return findList(InventoryItem.class, conditions);
    }

    /** {@inheritDoc} */
    public List<InventoryItem> getInventoryItemsWithNegativeATP(InventoryItem inventoryItem) throws RepositoryException {
        return getInventoryItemsWithNegativeATP(inventoryItem.getFacilityId(), inventoryItem.getProductId());
    }

    /** {@inheritDoc} */
    public List<OrderItemShipGrpInvRes> getOrderItemShipGroupInventoryReservations(InventoryItem inventoryItem) throws RepositoryException {
        if (inventoryItem.getQuantityOnHandTotal() != inventoryItem.getAvailableToPromiseTotal()) {
            return findList(OrderItemShipGrpInvRes.class, map(OrderItemShipGrpInvRes.Fields.inventoryItemId, inventoryItem.getInventoryItemId()));
        } else {
            return new ArrayList<OrderItemShipGrpInvRes>();
        }
    }

    /** {@inheritDoc} */
    public List<OrderItemShipGrpInvRes> getOrderItemShipGroupInventoryReservations(
            String orderId, String orderItemSeqId, String inventoryItemId, String shipGroupSeqId)
            throws RepositoryException {

        // Doesn't include InventoryItemId to conditions if it is null.
        Map<EntityFieldInterface<? super OrderItemShipGrpInvRes>, Object> conditions = FastMap.newInstance();
        conditions.put(OrderItemShipGrpInvRes.Fields.orderId, orderId);
        conditions.put(OrderItemShipGrpInvRes.Fields.orderItemSeqId, orderItemSeqId);
        if (UtilValidate.isNotEmpty(inventoryItemId)) {
            conditions.put(OrderItemShipGrpInvRes.Fields.inventoryItemId, inventoryItemId);
        }
        if (UtilValidate.isNotEmpty(shipGroupSeqId)) {
            conditions.put(OrderItemShipGrpInvRes.Fields.shipGroupSeqId, shipGroupSeqId);
        }

        return findList(OrderItemShipGrpInvRes.class, conditions);
    }

    /** {@inheritDoc} */
    public List<PicklistAndBinAndItem> getOpenPicklistBinItems(OrderItemShipGrpInvRes reservation) throws RepositoryException {
        return getOpenPicklistBinItems(reservation.getOrderId(), reservation.getShipGroupSeqId(), reservation.getOrderItemSeqId(), reservation.getInventoryItemId());
    }

    /** {@inheritDoc} */
    public List<PicklistAndBinAndItem> getOpenPicklistBinItems(String orderId, String shipGroupSeqId, String orderItemSeqId, String inventoryItemId) throws RepositoryException {
        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId),
                EntityCondition.makeCondition("shipGroupSeqId", EntityOperator.EQUALS, shipGroupSeqId),
                EntityCondition.makeCondition("orderItemSeqId", EntityOperator.EQUALS, orderItemSeqId),
                EntityCondition.makeCondition("inventoryItemId", EntityOperator.EQUALS, inventoryItemId),
                EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, PicklistAndBinAndItem.STATUS_PICKED),
                EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, PicklistAndBinAndItem.STATUS_CANCELLED));
        return findList(PicklistAndBinAndItem.class, conditions);
    }

    /** {@inheritDoc} */
    public List<InventoryItem> getInventoryItems(Product product, FacilityLocationType locationType, InventoryReservationOrder method, String facilityId, String containerId) throws RepositoryException {

        String type = null;
        if (locationType != null) {
            if (locationType.equals(FacilityLocationType.PRIMARY)) {
                type = "FLT_PICKLOC";
            } else if (locationType.equals(FacilityLocationType.BULK)) {
                type = "FLT_BULK";
            }
        }

        List<String> orderBy = FastList.newInstance();
        if (InventoryReservationOrder.GREATER_UNIT_COST.equals(method)) {
            orderBy.add("unitCost DESC");
        } else if (InventoryReservationOrder.LESS_UNIT_COST.equals(method)) {
            orderBy.add("unitCost ASC");
        } else if (InventoryReservationOrder.FIFO_EXPIRE.equals(method)) {
            orderBy.add("expireDate ASC");
        } else if (InventoryReservationOrder.LIFO_EXPIRE.equals(method)) {
            orderBy.add("expireDate DESC");
        } else if (InventoryReservationOrder.LIFO_RECEIVED.equals(method)) {
            orderBy.add("datetimeReceived DESC");
        } else {
            orderBy.add("datetimeReceived ASC");
        }

        // once the method order by set, get the higher ATP first
        //orderBy.add("availableToPromiseTotal DESC");
        // then at equal ATP get the higher QOH
        //orderBy.add("quantityOnHandTotal DESC");
        // finally add order by ID, we should get the latest inventory at the end
        orderBy.add("inventoryItemId ASC");

        List<EntityCondition> conditionList = FastList.newInstance();
        conditionList.add(EntityCondition.makeCondition("productId", EntityOperator.EQUALS, product.getProductId()));
        if (UtilValidate.isNotEmpty(facilityId)) {
            conditionList.add(EntityCondition.makeCondition("facilityId", EntityOperator.EQUALS, facilityId));
        }
        if (UtilValidate.isNotEmpty(containerId)) {
            conditionList.add(EntityCondition.makeCondition("containerId", EntityOperator.EQUALS, containerId));
        }
        if (UtilValidate.isNotEmpty(type)) {
            conditionList.add(EntityCondition.makeCondition("locationTypeEnumId", EntityOperator.EQUALS, type));
        }

        List<InventoryItemAndLocation> values = findList(InventoryItemAndLocation.class, conditionList, Arrays.asList("inventoryItemId"));
        if (UtilValidate.isEmpty(values)) {
            return null;
        }

        List<InventoryItem> result = findList(InventoryItem.class, EntityCondition.makeCondition("inventoryItemId", EntityOperator.IN, Entity.getDistinctFieldValues(values, InventoryItemAndLocation.Fields.inventoryItemId)), orderBy);

        if (UtilValidate.isEmpty(result)) {
            return null;
        }
        return result;
    }

    /** {@inheritDoc} */
    public List<InventoryTransfer> getPendingInventoryTransfers(InventoryItem inventoryItem) throws RepositoryException {
        return findList(InventoryTransfer.class, map(InventoryTransfer.Fields.inventoryItemId, inventoryItem.getInventoryItemId(), InventoryTransfer.Fields.statusId, "IXF_REQUESTED"));
    }

    /** {@inheritDoc} */
    public Product getRelatedProduct(InventoryItem inventoryItem) throws RepositoryException {
        try {
            return getProductRepository().getProductById(inventoryItem.getProductId());
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    protected ProductRepositoryInterface getProductRepository() throws RepositoryException {
        if (productRepository == null) {
            productRepository = DomainsDirectory.getDomainsDirectory(this).getProductDomain().getProductRepository();
        }
        return productRepository;
    }

    /** {@inheritDoc} */
    public Lot getLotById(String lotId) throws RepositoryException, EntityNotFoundException {
        return findOne(Lot.class, map(Lot.Fields.lotId, lotId));
    }

    /** {@inheritDoc} */
    public InventoryItemTrace createInventoryTrace(org.opentaps.base.entities.InventoryItem inventoryItem)  throws RepositoryException, InfrastructureException {

        InventoryItemTrace inventoryItemTrace = new InventoryItemTrace();
        inventoryItemTrace.setInventoryItemTraceId(getNextSeqId("InventoryItemTrace"));
        inventoryItemTrace.setInventoryItemId(inventoryItem.getInventoryItemId());
        inventoryItemTrace.setLotId(inventoryItem.getLotId());
        inventoryItemTrace.setRunDatetime(UtilDateTime.nowTimestamp());
        inventoryItemTrace.setRunByUserLogin(getUser().getUserId());

        createOrUpdate(inventoryItemTrace);

        return inventoryItemTrace;
    }

    /** {@inheritDoc} */
    public void createInventoryTraceEvent(InventoryItemTraceDetail event, InventoryItemTrace traceEntry) throws RepositoryException {
        createOrUpdate(event);

        // if the item has just been received, transferred, or otherwise popped into existence somehow (UNKNOWN), then the inventoryItemId is stored in toInventoryItemId
        // otherwise, it's in inventoryItemId
        String inventoryItemIdForVariance = event.getInventoryItemId();
        List<String> usageTypeIdsForToInventoryItemId = UtilMisc.toList("TRANSFER", "RECEIPT", "UNKNOWN");
        if (usageTypeIdsForToInventoryItemId.contains(event.getInventoryItemUsageTypeId())) {
            inventoryItemIdForVariance = event.getToInventoryItemId();
        }

        // find the inventory item's variance
        List<InventoryItemVariance> variances = findList(InventoryItemVariance.class, EntityCondition.makeCondition("inventoryItemId", EntityOperator.EQUALS, inventoryItemIdForVariance), Arrays.asList("physicalInventoryId"));
        Debug.logVerbose("Found inventory item ID [" + event.getToInventoryItemId() + "] variances :" + variances, MODULE);

        // record all the variances which have not already been recorded.  We want each inventory variance to show up only once, ie the first time it is recorded
        if (UtilValidate.isNotEmpty(variances)) {
            for (InventoryItemVariance variance : variances) {
                // find existing trace details for this inventory variance.  Just physical inventory ID should be enough--each one should be unique
                List<InventoryItemTraceDetail> existingVarianceTraceDetails =  findList(InventoryItemTraceDetail.class,
                        EntityCondition.makeCondition(InventoryItemTraceDetail.Fields.physicalInventoryId.getName(), EntityOperator.EQUALS, variance.getPhysicalInventoryId()));
                Debug.logVerbose("condition: " + EntityCondition.makeCondition(InventoryItemTraceDetail.Fields.physicalInventoryId, EntityOperator.EQUALS, variance.getPhysicalInventoryId()), MODULE);
                Debug.logVerbose("existing variance trace details: " + existingVarianceTraceDetails, MODULE);

                if (UtilValidate.isEmpty(existingVarianceTraceDetails)) {
                    // retrieve physical inventory
                    PhysicalInventory physicalInventory = variance.getPhysicalInventory();

                    // create separate details for every physical variance
                    InventoryItemTraceDetail adjustmentEvent = new InventoryItemTraceDetail();
                    adjustmentEvent.setInventoryItemTraceId(event.getInventoryItemTraceId());
                    adjustmentEvent.setInventoryItemTraceSeqId(traceEntry.getNextSeqNum());
                    adjustmentEvent.setTraceLevel(event.getTraceLevel() + 1);  // put variance one level lower
                    adjustmentEvent.setInventoryItemId(event.getToInventoryItemId());
                    adjustmentEvent.setUsageDatetime(physicalInventory.getPhysicalInventoryDate());
                    adjustmentEvent.setInventoryItemUsageTypeId("VARIANCE");
                    adjustmentEvent.setQuantity(variance.getQuantityOnHandVar());
                    adjustmentEvent.setPhysicalInventoryId(variance.getPhysicalInventoryId());
                    adjustmentEvent.setVarianceReasonId(variance.getVarianceReasonId());

                    Debug.logVerbose("Added adjustment event [" + adjustmentEvent + "]", MODULE);

                    createOrUpdate(adjustmentEvent);
                } else {
                    Debug.logVerbose("Inventory item id [" + variance.getInventoryItemId() + "] physical inventory ID [" + variance.getPhysicalInventoryId() + "] has already been recorded in InventoryItemTraceDetail and will be skipped for " + event , MODULE);
                }
            }
        }
    }

    /**
     * Return inventory transfer with most lower date.
     * @param xfrs list of <code>InventoryTransfer</code>
     * @return Instance of <code>InventoryTransfer</code>
     */
    private InventoryTransfer getFirstInventoryTransfer(List<? extends InventoryTransfer> xfrs) {

        Timestamp received = null;
        InventoryTransfer firstTransfer = null;

        for (InventoryTransfer transfer : xfrs) {
            Timestamp receiveDate = transfer.getReceiveDate();
            if (received == null) {
                received = receiveDate;
                firstTransfer = transfer;
                continue;
            }

            if (receiveDate.compareTo(received) >= 0) {
                continue;
            }

            received = receiveDate;
            firstTransfer = transfer;
        }

        return firstTransfer;
    }

    /** {@inheritDoc} */
    public List<InventoryItemTraceDetail> getDerivativeInventoryTraceEvents(org.opentaps.base.entities.InventoryItem inventoryItem) throws RepositoryException {
        List<InventoryItemTraceDetail> details = FastList.newInstance();

        String currentInventoryItemId = inventoryItem.getInventoryItemId();

        // find inventory item that were created by splitting
        List<InventoryItem> directChilds =
            findList(InventoryItem.class,
                    Arrays.asList(EntityCondition.makeCondition("parentInventoryItemId", EntityOperator.EQUALS, currentInventoryItemId)));
        Debug.logVerbose("***directChilds from InventoryItem.parentInventoryItemId " + directChilds, MODULE);

        if (UtilValidate.isNotEmpty(directChilds)) {
            for (InventoryItem item : directChilds) {
                InventoryItemTraceDetail detailEvent = new InventoryItemTraceDetail();
                detailEvent.setInventoryItemId(currentInventoryItemId);
                detailEvent.setInventoryItemUsageTypeId("TRANSFER");
                detailEvent.setToInventoryItemId(item.getInventoryItemId());
                List<? extends InventoryItemDetail> inventoryItemDetails = item.getInventoryItemDetails();
                if (UtilValidate.isNotEmpty(inventoryItemDetails)) {
                    String seqId = null;
                    InventoryItemDetail firstRecord = null;
                    for (InventoryItemDetail inventoryItemDetail : inventoryItemDetails) {
                        if (seqId == null || inventoryItemDetail.getInventoryItemDetailSeqId().compareTo(seqId) < 0) {
                            firstRecord = inventoryItemDetail;
                            seqId = inventoryItemDetail.getInventoryItemDetailSeqId();
                        }
                    }
                    detailEvent.setQuantity(firstRecord.getQuantityOnHandDiff());
                }
                InventoryTransfer transfer = getFirstInventoryTransfer(item.getInventoryTransfers());
                if (transfer != null) {
                    detailEvent.setUsageDatetime(transfer.getReceiveDate());
                    detailEvent.setInventoryTransferId(transfer.getInventoryTransferId());
                }
                details.add(detailEvent);
            }
        }

        Debug.logVerbose("details list after adding inventory transfers: " + details, MODULE);

        // find issuances against orders.  These have been shipped.
        List<ItemIssuance> issuances =
            findList(ItemIssuance.class,
                    Arrays.asList(EntityCondition.makeCondition("inventoryItemId", EntityOperator.EQUALS, inventoryItem.getInventoryItemId())));

        if (UtilValidate.isNotEmpty(issuances)) {
            for (ItemIssuance issuance : issuances) {
                InventoryItemTraceDetail detailEvent = new InventoryItemTraceDetail();
                detailEvent.setInventoryItemId(inventoryItem.getInventoryItemId());
                detailEvent.setInventoryItemUsageTypeId("ORDER_ISSUED");
                detailEvent.setItemIssuanceId(issuance.getItemIssuanceId());
                detailEvent.setQuantity(issuance.getQuantity());
                detailEvent.setUsageDatetime(issuance.getIssuedDateTime());
                details.add(detailEvent);
            }
        }

        Debug.logVerbose("details list after adding item issuances to orders (shipped): " + details, MODULE);

        // find reservations against orders.  These have not been shipped.
        List<OrderItemShipGrpInvRes> orderReservations =
            findList(OrderItemShipGrpInvRes.class,
                    Arrays.asList(EntityCondition.makeCondition("inventoryItemId", EntityOperator.EQUALS, inventoryItem.getInventoryItemId())));

        if (UtilValidate.isNotEmpty(orderReservations)) {
            for (OrderItemShipGrpInvRes orderReservation : orderReservations) {
                InventoryItemTraceDetail detailEvent = new InventoryItemTraceDetail();
                detailEvent.setInventoryItemId(inventoryItem.getInventoryItemId());
                detailEvent.setInventoryItemUsageTypeId("ORDER_RESERVED");
                detailEvent.setOrderId(orderReservation.getOrderId());
                detailEvent.setOrderItemSeqId(orderReservation.getOrderItemSeqId());
                detailEvent.setShipGroupSeqId(orderReservation.getShipGroupSeqId());
                detailEvent.setQuantity(orderReservation.getQuantity());
                detailEvent.setUsageDatetime(orderReservation.getReservedDatetime());
                details.add(detailEvent);
            }
        }

        Debug.logVerbose("details list after adding reservations to orders (unshipped): " + details, MODULE);

        // find inventories which are used in manufacturing
        List<WorkEffortInventoryAssign> assignedToProduction =
            findList(WorkEffortInventoryAssign.class,
                    Arrays.asList(EntityCondition.makeCondition("inventoryItemId", EntityOperator.EQUALS, inventoryItem.getInventoryItemId())));

        if (UtilValidate.isNotEmpty(assignedToProduction)) {
            for (WorkEffortInventoryAssign assignedItem : assignedToProduction) {
                WorkEffort productionRunTask = assignedItem.getWorkEffort();
                if (productionRunTask != null) {
                    WorkEffort productionRunHeader = productionRunTask.getParentWorkEffort();
                    if (productionRunHeader != null) {
                        List<WorkEffortInventoryProduced> producedItems =
                            findList(WorkEffortInventoryProduced.class,
                                    Arrays.asList(EntityCondition.makeCondition("workEffortId", EntityOperator.EQUALS, productionRunHeader.getWorkEffortId())));

                        if (UtilValidate.isNotEmpty(producedItems)) {
                            for (WorkEffortInventoryProduced producedItem : producedItems) {
                                InventoryItemTraceDetail detailEvent = new InventoryItemTraceDetail();
                                detailEvent.setInventoryItemId(inventoryItem.getInventoryItemId());
                                detailEvent.setToInventoryItemId(producedItem.getInventoryItemId());
                                detailEvent.setInventoryItemUsageTypeId("MANUF_RAW_MAT");
                                detailEvent.setAssignWorkEffortId(assignedItem.getWorkEffortId());
                                detailEvent.setProducedWorkEffortId(producedItem.getWorkEffortId());
                                detailEvent.setUsageDatetime(productionRunHeader.getActualStartDate());
                                List<? extends InventoryItemDetail> producedInventoryItemDetails = producedItem.getInventoryItemDetails();
                                if (UtilValidate.isNotEmpty(producedInventoryItemDetails)) {
                                    String seqId = null;
                                    InventoryItemDetail firstRecord = null;
                                    for (InventoryItemDetail inventoryItemDetail : producedInventoryItemDetails) {
                                        if (seqId == null || inventoryItemDetail.getInventoryItemDetailSeqId().compareTo(seqId) < 0) {
                                            firstRecord = inventoryItemDetail;
                                            seqId = inventoryItemDetail.getInventoryItemDetailSeqId();
                                        }
                                    }
                                    detailEvent.setQuantity(firstRecord.getQuantityOnHandDiff());
                                }
                                details.add(detailEvent);
                            }
                        }
                    }
                }
            }
        }

        Debug.logVerbose("details list after adding inventory assigned to manufacturing: " + details, MODULE);

        return (details.size() > 0) ? details : null;
    }

    /** {@inheritDoc} */
    public InventoryItemTraceDetail getSoughtTraceEntry(String inventoryItemId, boolean forward) throws RepositoryException {
        EntityCondition conditionList = EntityCondition.makeCondition(EntityOperator.OR,
                        EntityCondition.makeCondition(InventoryItemTraceDetail.Fields.inventoryItemId.getName(), EntityOperator.EQUALS, inventoryItemId),
                        EntityCondition.makeCondition(InventoryItemTraceDetail.Fields.toInventoryItemId.getName(), EntityOperator.EQUALS, inventoryItemId));
        // find all detail records that corresponds to given inventory item id
        List<InventoryItemTraceDetail> traceDetails = findList(InventoryItemTraceDetail.class, conditionList);

        // ORDER_ISSUED has no toInventoryItemId value, handle this special case for backward search
        if (!forward) {
            for (InventoryItemTraceDetail entry : traceDetails) {
                if ("ORDER_ISSUED".equals(entry.getInventoryItemUsageTypeId()) && inventoryItemId.equals(entry.getInventoryItemId())) {
                    return entry;
                }
            }
        }

        // find trace detail record where toInventoryItemId equals to givent inventory item id.
        for (InventoryItemTraceDetail entry : traceDetails) {
            // handle ORDER_ISSUED case where no toInventoryItemId value
            if (forward && "ORDER_ISSUED".equals(entry.getInventoryItemUsageTypeId()) && inventoryItemId.equals(entry.getInventoryItemId())) {
                return entry;
            }
            String toInventoryItemId = entry.getToInventoryItemId();
            if (UtilValidate.isEmpty(toInventoryItemId)) {
                continue;
            }
            if (toInventoryItemId.equals(inventoryItemId)) {
                return entry;
            }
        }

        return null;
    }

    /** {@inheritDoc} */
    public List<InventoryItemTraceDetail> findTraceEventAdjustments(InventoryItemTraceDetail traceDetail, boolean desc) throws RepositoryException {
        String inventoryItemId = "ORDER_ISSUED".equals(traceDetail.getInventoryItemUsageTypeId()) ? traceDetail.getInventoryItemId() : traceDetail.getToInventoryItemId();
        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                EntityCondition.makeCondition(InventoryItemTraceDetail.Fields.inventoryItemTraceId.getName(), EntityOperator.EQUALS, traceDetail.getInventoryItemTraceId()),
                EntityCondition.makeCondition(InventoryItemTraceDetail.Fields.inventoryItemUsageTypeId.getName(), EntityOperator.EQUALS, "VARIANCE"),
                EntityCondition.makeCondition(InventoryItemTraceDetail.Fields.inventoryItemId.getName(), EntityOperator.EQUALS, inventoryItemId));
        return findList(InventoryItemTraceDetail.class, conditions, Arrays.asList(String.format("inventoryItemTraceSeqId %1$s", desc ? "DESC" : "ASC")));
    }

    /** {@inheritDoc} */
    public List<InventoryItemTraceDetail> collectTraceEventsBackward(InventoryItemTraceDetail traceDetail) throws RepositoryException {
        List<InventoryItemTraceDetail> ret = FastList.newInstance();
        ret.add(traceDetail);
        List<InventoryItemTraceDetail> variances = findTraceEventAdjustments(traceDetail, true);
        if (UtilValidate.isNotEmpty(variances)) {
            ret.addAll(variances);
        }

        EntityCondition conditions;

        if ("RECEIPT".equals(traceDetail.getInventoryItemUsageTypeId())) {
            return ret;
        }

        // MANUF_RAW_MAT may has more than one parent and neighbor, we have to collect all of them
        if ("MANUF_RAW_MAT".equals(traceDetail.getInventoryItemUsageTypeId())) {
            // collect neighbors
            EntityCondition neighborConditions = EntityCondition.makeCondition(
                    EntityCondition.makeCondition(InventoryItemTraceDetail.Fields.inventoryItemUsageTypeId.getName(), EntityOperator.EQUALS, "MANUF_RAW_MAT"),
                    EntityCondition.makeCondition(InventoryItemTraceDetail.Fields.toInventoryItemId.getName(), EntityOperator.EQUALS, traceDetail.getToInventoryItemId()));
            List<InventoryItemTraceDetail> mfctRecords = findList(InventoryItemTraceDetail.class, neighborConditions);
            ret.clear();
            ret.addAll(mfctRecords);

            // collect parents
            conditions = EntityCondition.makeCondition(
                    EntityCondition.makeCondition(InventoryItemTraceDetail.Fields.toInventoryItemId.getName(), EntityOperator.EQUALS, traceDetail.getToInventoryItemId()),
                    EntityCondition.makeCondition(InventoryItemTraceDetail.Fields.inventoryItemUsageTypeId.getName(), EntityOperator.NOT_EQUAL, "VARIANCE")
            );
            List<InventoryItemTraceDetail> rawMaterials = findList(InventoryItemTraceDetail.class, conditions);
            List<String> rawMaterialIds = FastList.newInstance();
            for (InventoryItemTraceDetail material : rawMaterials) {
                rawMaterialIds.add(material.getInventoryItemId());
            }
            conditions = EntityCondition.makeCondition(
                    EntityCondition.makeCondition(InventoryItemTraceDetail.Fields.toInventoryItemId.getName(), EntityOperator.IN, rawMaterialIds),
                    EntityCondition.makeCondition(InventoryItemTraceDetail.Fields.toInventoryItemId.getName(), EntityOperator.NOT_EQUAL, null),
                    EntityCondition.makeCondition(InventoryItemTraceDetail.Fields.inventoryItemUsageTypeId.getName(), EntityOperator.NOT_EQUAL, "VARIANCE")
            );
        } else {
            conditions = EntityCondition.makeCondition(
                    EntityCondition.makeCondition(InventoryItemTraceDetail.Fields.toInventoryItemId.getName(), EntityOperator.EQUALS, traceDetail.getInventoryItemId()),
                    EntityCondition.makeCondition(InventoryItemTraceDetail.Fields.toInventoryItemId.getName(), EntityOperator.NOT_EQUAL, null),
                    EntityCondition.makeCondition(InventoryItemTraceDetail.Fields.inventoryItemUsageTypeId.getName(), EntityOperator.NOT_EQUAL, "VARIANCE")
            );
        }

        // get usage log recursively
        List<InventoryItemTraceDetail> childEvents = findList(InventoryItemTraceDetail.class, conditions);
        for (InventoryItemTraceDetail current : childEvents) {
            ret.addAll(collectTraceEventsBackward(current));
        }

        return ret;
    }

    /** {@inheritDoc} */
    public List<InventoryItemTraceDetail> collectTraceEventsForward(InventoryItemTraceDetail traceDetail) throws RepositoryException {
        List<InventoryItemTraceDetail> ret = FastList.newInstance();
        ret.add(traceDetail);
        List<InventoryItemTraceDetail> variances = findTraceEventAdjustments(traceDetail, false);
        if (UtilValidate.isNotEmpty(variances)) {
            ret.addAll(variances);
        }

        EntityCondition conditions;

        // ORDER_ISSUED always is final step in forward direction
        String usageTypeId = traceDetail.getInventoryItemUsageTypeId();
        if ("ORDER_ISSUED".equals(usageTypeId) || "ORDER_RESERVED".equals(usageTypeId)) {
            return ret;
        }

        conditions = EntityCondition.makeCondition(
                EntityCondition.makeCondition(InventoryItemTraceDetail.Fields.inventoryItemId.getName(), EntityOperator.EQUALS, traceDetail.getToInventoryItemId()),
                EntityCondition.makeCondition(InventoryItemTraceDetail.Fields.inventoryItemTraceId.getName(), EntityOperator.EQUALS, traceDetail.getInventoryItemTraceId()),
                EntityCondition.makeCondition(InventoryItemTraceDetail.Fields.inventoryItemUsageTypeId.getName(), EntityOperator.NOT_EQUAL, "VARIANCE")
        );

        // get usage log recursively
        List<InventoryItemTraceDetail> childEvents = findList(InventoryItemTraceDetail.class, conditions);
        for (InventoryItemTraceDetail current : childEvents) {
            ret.addAll(collectTraceEventsForward(current));
        }

        return ret;
    }

    /**
     * Finds the last recorded <code>InventoryItemValueHistory</code> from the database.
     * @param inventoryItem an <code>InventoryItem</code>
     * @return the <code>InventoryItemValueHistory</code> found
     * @throws RepositoryException if an error occurs
     */
    public InventoryItemValueHistory getLastInventoryItemValueHistoryByInventoryItem(
            InventoryItem inventoryItem) throws RepositoryException {
        List<String> orderBy = FastList.newInstance();
        orderBy.add("dateTime DESC");
        orderBy.add("inventoryItemValueHistId DESC");
        EntityFindOptions findOpt = new EntityFindOptions();
        findOpt.setMaxRows(1);
        EntityCondition cond = EntityCondition.makeCondition(
                                    EntityCondition.makeCondition("inventoryItemId", EntityOperator.EQUALS, inventoryItem.getInventoryItemId()),
                                    EntityCondition.makeCondition("unitCost", EntityOperator.NOT_EQUAL, null),
                                    EntityCondition.makeCondition("dateTime", EntityOperator.LESS_THAN, inventoryItem.getTimestamp("lastUpdatedStamp")));
        List<InventoryItemValueHistory> inventoryItemValueHistories = findList(InventoryItemValueHistory.class, cond, orderBy);
        if (UtilValidate.isNotEmpty(inventoryItemValueHistories)) {
            return inventoryItemValueHistories.get(0);
        }
        return null;
    }

    /** {@inheritDoc} */
    public InventoryTransfer getInventoryTransferById(String inventoryTransferId) throws RepositoryException, EntityNotFoundException {
        return findOneNotNull(InventoryTransfer.class, map(InventoryTransfer.Fields.inventoryTransferId, inventoryTransferId));
    }

}
