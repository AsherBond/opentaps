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

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javolution.util.FastList;
import javolution.util.FastSet;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.domain.DomainService;
import org.opentaps.base.entities.InventoryItem;
import org.opentaps.base.entities.InventoryItemTraceDetail;
import org.opentaps.base.entities.InventoryTransfer;
import org.opentaps.base.entities.Lot;
import org.opentaps.base.entities.ShipmentReceipt;
import org.opentaps.base.entities.WorkEffort;
import org.opentaps.base.entities.WorkEffortInventoryAssign;
import org.opentaps.base.entities.WorkEffortInventoryProduced;
import org.opentaps.domain.inventory.InventoryItemTrace;
import org.opentaps.domain.inventory.InventoryRepositoryInterface;
import org.opentaps.domain.inventory.TraceInventoryServiceInterface;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.ofbiz.Repository;
import org.opentaps.foundation.service.ServiceException;

/**
 * Inventory tracing service(s) implemented with POJO Java engine using the opentaps Service foundation class.
 */
public class TraceInventoryService extends DomainService implements TraceInventoryServiceInterface {

    private static final String MODULE = TraceInventoryService.class.getName();

    private String inventoryItemId;
    private String lotId;
    private String traceDirection;
    private List<List<InventoryItemTraceDetail>> usageLog;

    /**
     * During analyze InventoryItemTraceDetail separate logs are created for
     * each inventory item assigned to given lot. Logs that have common root
     * inventory item are merged if GROUP_USAGE_LOGS and appear as united group
     * in listing.
     */
    private static final boolean GROUP_USAGE_LOGS = true;

    /**
     * Default constructor.
     */
    public TraceInventoryService() {
        super();
    }

    /** {@inheritDoc} */
    public void traceInventoryUsage() throws ServiceException {

        // we need either inventoryItemId or lotId
        if (UtilValidate.isEmpty(inventoryItemId)) {
            if (UtilValidate.isEmpty(lotId)) {
                throw new ServiceException("WarehouseError_MissingAttributesForTrace", null);
            }
        }

        try {

            // delete previous records
            clearInventoryTrace();

            // build new base of usage events for inventoryItemId or lotId
            buildInventoryTrace();

            // retrieve forward or backward usage info and return in usageLog
            if ("FORWARD".equals(traceDirection)) {
                traceInventoryUsageForward();
            } else if ("BACKWARD".equals(traceDirection)) {
                traceInventoryUsageBackward();
            } else {
                // traceDirection is required attribute, but if value is wrong throw exception.
                throw new ServiceException(new IllegalArgumentException());
            }

        } catch (Exception e) {
            Debug.logError(e, MODULE);
            usageLog = null;
            addSuccessMessage(e.getMessage());
        }

        return;
    }

    /*
     * Recursively go over all child of given inventory items and create trace events for each level.
     */
    private List<InventoryItemTraceDetail> parseForward(InventoryItem item, long level, InventoryItemTrace inventoryItemTrace, InventoryRepositoryInterface repo) throws RepositoryException, EntityNotFoundException {

        List<InventoryItemTraceDetail> results = FastList.newInstance();
        InventoryItem currentItem = item;
        long currentLevel = level;
        long nextLevel = level + 1;

        // gets trace events for inventory items that are direct child of current item
        List<InventoryItemTraceDetail> child = repo.getDerivativeInventoryTraceEvents(currentItem);
        Debug.logVerbose("***Got child items for item [" + item.getInventoryItemId() + "]", MODULE);
        if (UtilValidate.isNotEmpty(child)) {
            for (InventoryItemTraceDetail event : child) {
                Debug.logVerbose("****child item for [" + item.getInventoryItemId() + "] is [" + event.getInventoryItemId() + "]", MODULE);
                event.setInventoryItemTraceId(inventoryItemTrace.getInventoryItemTraceId());
                event.setInventoryItemTraceSeqId(inventoryItemTrace.getNextSeqNum());
                event.setTraceLevel(currentLevel);
                Debug.logVerbose("created trace event for child event [" + event + "]", MODULE);
                repo.createInventoryTraceEvent(event, inventoryItemTrace); // store event in database

                String toInventoryItemId = event.getToInventoryItemId();
                if (UtilValidate.isNotEmpty(toInventoryItemId)) {  // ORDER_ISSUED event has no toInventoryItemId
                    InventoryItem next = repo.getInventoryItemById(event.getToInventoryItemId());
                    Debug.logVerbose("****Found next item [" + next.getInventoryItemId() + "] for item [" + item.getInventoryItemId() + "]", MODULE);
                    // recursively call itself to get next level
                    List<InventoryItemTraceDetail> results1 = parseForward(next, nextLevel, inventoryItemTrace, repo);
                    if (UtilValidate.isNotEmpty(results1)) {
                        results.addAll(results1);
                    }
                }
            }
        }

        return results;
    }

    /**
     * Method recursively checks all parents for given inventory item and build collection of original items.
     * For example, if a product was manufactured from raw materials or transferred from another warehouse,
     * this method would find the original raw materials/inventory items.
     *
     * @param inventoryItem <code>InventoryItem</code> that is initial for look up.
     * @param repository inventory repository instance
     * @return Collection of unique <code>InventoryItem</code>
     * @throws RepositoryException
     * @throws EntityNotFoundException
     */
    private Set<InventoryItem> findRootItems(InventoryItem inventoryItem, InventoryRepositoryInterface repository) throws RepositoryException, EntityNotFoundException {
        Set<InventoryItem> results = FastSet.newInstance();
        List<InventoryItem> parents = FastList.newInstance();

        String parentInventoryItemId = inventoryItem.getParentInventoryItemId();
        if (UtilValidate.isNotEmpty(parentInventoryItemId)) {
            // direct parent found
            parents.add(repository.getInventoryItemById(parentInventoryItemId, InventoryItem.class));
        } else {
            List<? extends WorkEffortInventoryProduced> produced = inventoryItem.getWorkEffortInventoryProduceds();
            if (UtilValidate.isNotEmpty(produced)) {
                // given item is product of manufacturing
                for (WorkEffortInventoryProduced producedItem : produced) {
                    WorkEffort productionRunHeader = producedItem.getWorkEffort();
                    if (productionRunHeader != null) {
                        List<? extends WorkEffort> productionRunTasks = productionRunHeader.getChildWorkEfforts();
                        for (WorkEffort task : productionRunTasks) {
                            List<? extends WorkEffortInventoryAssign> inventoryAssigns = task.getWorkEffortInventoryAssigns();
                            if (UtilValidate.isNotEmpty(inventoryAssigns)) {
                                // all original materials found
                                for (WorkEffortInventoryAssign assignment : inventoryAssigns) {
                                    InventoryItem current = assignment.getInventoryItem();
                                    parents.add(current);
                                }
                            }
                        }
                    }
                }
            } else {
                // no parent, root inventory item
                results.add(inventoryItem);
            }
        }

        // for all found parents recursively find their parents
        for (InventoryItem item : parents) {
            results.addAll(findRootItems(item, repository));
        }

        return results;
    }

    public void printInventoryItems(Collection<InventoryItem> items) {
        for (InventoryItem item : items) {
            Debug.logVerbose("***" + item.getInventoryItemId(), MODULE);
        }
    }

    /** {@inheritDoc} */
    public void buildInventoryTrace() throws ServiceException {

        Set<InventoryItem> inventoryItems = FastSet.newInstance();
        Set<InventoryItem> rootItems = FastSet.newInstance();

        try {
            InventoryRepositoryInterface invRepository = getDomainsDirectory().getInventoryDomain().getInventoryRepository();

            // build initial list of inventory items for tracing
            if (UtilValidate.isNotEmpty(inventoryItemId)) { // given inventory item ID
                inventoryItems.add(invRepository.getInventoryItemById(inventoryItemId, InventoryItem.class));
            } else if (UtilValidate.isNotEmpty(lotId)) { // given lot ID
                Lot lot = invRepository.getLotById(lotId);
                inventoryItems.addAll(lot.getInventoryItems());
            } else { // nothing specified in service attributes, build all items
                inventoryItems.addAll(((Repository) invRepository).findAll(InventoryItem.class));
            }

            Debug.logVerbose("Tracing started with inventory items:", MODULE);
            printInventoryItems(inventoryItems);

            // some of the items isn't an root item but derivative item created after transfer or
            // product of manufacturing. Loop below builds collection where inventory items all are
            // first level items.
            for (InventoryItem item : inventoryItems) {
                List<? extends InventoryTransfer> xfrs = item.getInventoryTransfers();
                List<? extends WorkEffortInventoryProduced> inventoryProduced = item.getWorkEffortInventoryProduceds();
                if (UtilValidate.isNotEmpty(inventoryProduced) || UtilValidate.isNotEmpty(xfrs))
                {
                    Debug.logVerbose("Found root items: ", MODULE);
                    printInventoryItems(findRootItems(item, invRepository));
                    // find original inventory items
                    rootItems.addAll(findRootItems(item, invRepository));
                    continue;
                }
                rootItems.add(item);
            }

            // Build inventory tracing history. An inventory item can be received from a shipment or
            // loaded from external source
            for (InventoryItem item : rootItems) {

                InventoryItemTrace itemTrace = invRepository.createInventoryTrace(item);
                InventoryItemTraceDetail traceEvent;

                List<? extends ShipmentReceipt> receipts = item.getShipmentReceipts();
                if (UtilValidate.isEmpty(receipts)) { // handle item that has no receipt
                    traceEvent = new InventoryItemTraceDetail();
                    traceEvent.setInventoryItemTraceId(itemTrace.getInventoryItemTraceId());
                    traceEvent.setTraceLevel(1L);
                    traceEvent.setToInventoryItemId(item.getInventoryItemId());
                    traceEvent.setInventoryItemUsageTypeId("UNKNOWN");
                    traceEvent.setUsageDatetime(item.getDatetimeReceived());
                    traceEvent.setInventoryItemTraceId(itemTrace.getInventoryItemTraceId());
                    traceEvent.setInventoryItemTraceSeqId(itemTrace.getNextSeqNum());
                    invRepository.createInventoryTraceEvent(traceEvent, itemTrace);
                    Debug.logVerbose("Created trace event = " + traceEvent, MODULE);
                    parseForward(item, 2, itemTrace, invRepository);
                } else {
                    for (ShipmentReceipt receipt : receipts) { // handle shipment receipts
                        traceEvent = new InventoryItemTraceDetail();
                        traceEvent.setInventoryItemTraceId(itemTrace.getInventoryItemTraceId());
                        traceEvent.setTraceLevel(1L);
                        traceEvent.setToInventoryItemId(item.getInventoryItemId());
                        traceEvent.setInventoryItemUsageTypeId("RECEIPT");
                        traceEvent.setUsageDatetime(receipt.getDatetimeReceived());
                        traceEvent.setReceiptId(receipt.getReceiptId());
                        traceEvent.setInventoryItemTraceId(itemTrace.getInventoryItemTraceId());
                        traceEvent.setInventoryItemTraceSeqId(itemTrace.getNextSeqNum());
                        traceEvent.setQuantity(receipt.getQuantityAccepted());
                        invRepository.createInventoryTraceEvent(traceEvent, itemTrace);
                        Debug.logVerbose("Created trace event = " + traceEvent, MODULE);
                        parseForward(item, 2, itemTrace, invRepository);
                    }
                }

            }

        } catch (RepositoryException e) {
            throw new ServiceException(e);
        } catch (EntityNotFoundException e) {
            throw new ServiceException(e);
        } catch (InfrastructureException e) {
            throw new ServiceException(e);
        }
    }

    /** {@inheritDoc} */
    public void clearInventoryTrace() throws ServiceException {
        Delegator delegator = getInfrastructure().getDelegator();

        try {
            EntityCondition dumbCondition = EntityCondition.makeCondition("inventoryItemTraceId", EntityOperator.NOT_EQUAL, "0");

            // remove all records
            delegator.removeByCondition("InventoryItemTraceDetail", dumbCondition);
            delegator.removeByCondition("InventoryItemTrace", dumbCondition);

        } catch (GenericEntityException e) {
            throw new ServiceException(e.getMessage());
        }
    }

    /** {@inheritDoc} */
    public void traceInventoryUsageForward() throws ServiceException {

        Set<InventoryItem> inventoryItems = FastSet.newInstance();
        List<List<InventoryItemTraceDetail>> usageCollection = FastList.newInstance();

        try {
            InventoryRepositoryInterface invRpst = getDomainsDirectory().getInventoryDomain().getInventoryRepository();

            // build initial list of inventory items for tracing
            if (UtilValidate.isNotEmpty(inventoryItemId)) { // given inventory item ID
                inventoryItems.add(invRpst.getInventoryItemById(inventoryItemId));
            } else if (UtilValidate.isNotEmpty(lotId)) { // given lot ID
                Lot lot = invRpst.getLotById(lotId);
                inventoryItems.addAll(lot.getInventoryItems());
            } else { // nothing specified in service attributes, throw error
                throw new ServiceException("WarehouseError_MissingAttributesForTrace");
            }

            Comparator<InventoryItemTraceDetail> traceLogComparator = new Comparator<InventoryItemTraceDetail>() {
                public int compare(InventoryItemTraceDetail o1, InventoryItemTraceDetail o2) {
                    return o1.getTraceLevel().compareTo(o2.getTraceLevel());
                }
            };

            for (InventoryItem item : inventoryItems) {
                InventoryItemTraceDetail traceDetail = null;
                if (traceDetail == null) {
                    // map inventory item to corresponding InventoryItemTraceDetail
                    traceDetail = invRpst.getSoughtTraceEntry(item.getInventoryItemId(), true);
                    if (traceDetail == null) {
                        continue;
                    }
                }

                // gets usage log
                List<InventoryItemTraceDetail> results = invRpst.collectTraceEventsForward(traceDetail);
                Debug.logInfo("Get InventoryItemId [" + item.getInventoryItemId() + "]" + " trace.size() : " + results.size() + ", 1st InventoryItemTraceId/InventoryItemTraceSeqId : "
                        + (results.size() > 0 ? results.get(0).getInventoryItemTraceId() + "/" + results.get(0).getInventoryItemTraceSeqId() : ""), MODULE);
                // sort usage log by log level
                if (!GROUP_USAGE_LOGS) {
                    Collections.sort(results, traceLogComparator);
                }

                usageCollection.add(results);
            }

        } catch (RepositoryException e) {
            throw new ServiceException(e);
        } catch (EntityNotFoundException e) {
            throw new ServiceException(e);
        }

        if (usageLog == null) {
            usageLog = FastList.newInstance();
        }
        usageLog.addAll(groupInventoryLogs(usageCollection, true));
    }

    /** {@inheritDoc} */
    public void traceInventoryUsageBackward() throws ServiceException {
        Set<InventoryItem> inventoryItems = FastSet.newInstance();
        List<List<InventoryItemTraceDetail>> usageCollection = FastList.newInstance();

        try {
            InventoryRepositoryInterface invRpst = getDomainsDirectory().getInventoryDomain().getInventoryRepository();

            // build initial list of inventory items for tracing
            if (UtilValidate.isNotEmpty(inventoryItemId)) { // given inventory item ID
                inventoryItems.add(invRpst.getInventoryItemById(inventoryItemId));
            } else if (UtilValidate.isNotEmpty(lotId)) { // given lot ID
                Lot lot = invRpst.getLotById(lotId);
                inventoryItems.addAll(lot.getInventoryItems());
            } else { // nothing specified in service attributes, throw error
                throw new ServiceException("WarehouseError_MissingAttributesForTrace");
            }

            Comparator<InventoryItemTraceDetail> traceLogComparator = new Comparator<InventoryItemTraceDetail>() {
                public int compare(InventoryItemTraceDetail o1, InventoryItemTraceDetail o2) {
                    return o2.getTraceLevel().compareTo(o1.getTraceLevel());
                }
            };

            for (InventoryItem item : inventoryItems) {
                InventoryItemTraceDetail traceDetail = null;
                if (traceDetail == null) {
                    // map inventory item to corresponding InventoryItemTraceDetail
                    traceDetail = invRpst.getSoughtTraceEntry(item.getInventoryItemId(), false);
                    if (traceDetail == null) {
                        continue;
                    }
                }

                // get usage log
                List<InventoryItemTraceDetail> results = UtilMisc.toList(UtilMisc.toSet(invRpst.collectTraceEventsBackward(traceDetail)));

                // sort usage log by log level
                if (!GROUP_USAGE_LOGS) {
                    Collections.sort(results, traceLogComparator);
                }

                usageCollection.add(UtilMisc.toList(results));
            }

        } catch (RepositoryException e) {
            throw new ServiceException(e);
        } catch (EntityNotFoundException e) {
            throw new ServiceException(e);
        }

        if (usageLog == null) {
            usageLog = FastList.newInstance();
        }
        usageLog.addAll(groupInventoryLogs(usageCollection, false));
    }

    /**
     * Group inner lists by their items <code>inventoryItemTraceId</code>.
     * @param logs list of logs
     * @param forward affects sort direction
     * @return
     *    Regrouped logs
     */
    private List<List<InventoryItemTraceDetail>> groupInventoryLogs(List<List<InventoryItemTraceDetail>> logs, boolean forward) {

        List<List<InventoryItemTraceDetail>> finalCollection = FastList.newInstance();

        if (!GROUP_USAGE_LOGS || UtilValidate.isEmpty(logs)) {
            return logs;
        }

        Set<String> traceIds = FastSet.newInstance();
        for (List<InventoryItemTraceDetail> c : logs) {
            InventoryItemTraceDetail detailItem = c.get(0);
            if (detailItem != null) {
                traceIds.add(detailItem.getInventoryItemTraceId());
            }
        }

        final boolean direction = forward;
        Comparator<InventoryItemTraceDetail> traceLogComparator = new Comparator<InventoryItemTraceDetail>() {
            public int compare(InventoryItemTraceDetail o1, InventoryItemTraceDetail o2) {
                return (direction ? o1.getTraceLevel().compareTo(o2.getTraceLevel()) : o2.getTraceLevel().compareTo(o1.getTraceLevel()));
            }
        };

        for (String id : traceIds) {
            List<InventoryItemTraceDetail> detailsUnderCommonRoot = FastList.newInstance();
            for (List<InventoryItemTraceDetail> c : logs) {
                InventoryItemTraceDetail detailItem = c.get(0);
                if (id.equals(detailItem.getInventoryItemTraceId())) {
                    detailsUnderCommonRoot.addAll(c);
                }
            }
            if (UtilValidate.isNotEmpty(detailsUnderCommonRoot)) {
                Collections.sort(detailsUnderCommonRoot, traceLogComparator);
                finalCollection.add(detailsUnderCommonRoot);
            }
        }

        return finalCollection;
    }

    /** {@inheritDoc} */
    public List<List<InventoryItemTraceDetail>> getUsageLog() {
        return usageLog;
    }

    /** {@inheritDoc} */
    public void setInventoryItemId(String inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
    }

    /** {@inheritDoc} */
    public void setLotId(String lotId) {
        this.lotId = lotId;
    }

    /** {@inheritDoc} */
    public void setTraceDirection(String direction) {
        traceDirection = direction;
    }

}
