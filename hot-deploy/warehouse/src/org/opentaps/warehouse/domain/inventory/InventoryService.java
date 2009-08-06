/*
 * Copyright (c) 2007 - 2009 Open Source Strategies, Inc.
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
package org.opentaps.warehouse.domain.inventory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.domain.base.entities.InventoryTransfer;
import org.opentaps.domain.inventory.InventoryDomainInterface;
import org.opentaps.domain.inventory.InventoryItem;
import org.opentaps.domain.inventory.InventoryRepositoryInterface;
import org.opentaps.domain.inventory.InventoryServiceInterface;
import org.opentaps.domain.inventory.InventorySpecification;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.Service;
import org.opentaps.foundation.service.ServiceException;

/**
 * Inventory management services implemented with POJO Java engine using the opentaps Service foundation class.
 */
public class InventoryService extends Service implements InventoryServiceInterface {

    @SuppressWarnings("unused")
    private static final String MODULE = InventoryService.class.getName();

    private String productId = null;

    private BigDecimal quantityOnHandTotal = null;
    private BigDecimal availableToPromiseTotal = null;

    private String inventoryItemId;
    private BigDecimal xferQty;
    private String inventoryTransferId;

    /**
     * Default constructor.
     */
    public InventoryService() {
        super();
    }

    /** {@inheritDoc} */
    public void setProductId(String productId) {
        this.productId = productId;
    }

    /** {@inheritDoc} */
    public void setUseCache(Boolean useCache) {
    }

    /** {@inheritDoc} */
    public Double getQuantityOnHandTotal() {
        return this.quantityOnHandTotal.doubleValue();
    }

    /** {@inheritDoc} */
    public Double getAvailableToPromiseTotal() {
        return this.availableToPromiseTotal.doubleValue();
    }

    /** {@inheritDoc} */
    public void getProductInventoryAvailable() throws ServiceException {
        try {
            InventoryDomainInterface inventoryDomain = getDomainsDirectory().getInventoryDomain();
            InventoryRepositoryInterface inventoryRepository = inventoryDomain.getInventoryRepository();

            List<InventoryItem> items = inventoryRepository.getInventoryItemsForProductId(productId);

            availableToPromiseTotal = BigDecimal.ZERO;
            quantityOnHandTotal = BigDecimal.ZERO;

            for (InventoryItem item : items) {
                availableToPromiseTotal = availableToPromiseTotal.add(item.getNetATP());
                quantityOnHandTotal = quantityOnHandTotal.add(item.getNetQOH());
            }

        } catch (GeneralException ex) {
            throw new ServiceException(ex);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public void cancelInventoryTransfer() throws ServiceException {
        InventoryRepositoryInterface inventoryRepository = null;
        org.opentaps.domain.base.entities.InventoryItem originInventoryItem = null;
        org.opentaps.domain.base.entities.InventoryItem destinationInventoryItem = null;
        InventoryTransfer transfer = null;

        try {
            inventoryRepository = domains.getInventoryDomain().getInventoryRepository();

            transfer = inventoryRepository.getInventoryTransferById(inventoryTransferId);
            // the transfer destination InventoryItem
            destinationInventoryItem = transfer.getInventoryItem();
            // the transfer origin InventoryItem
            originInventoryItem = inventoryRepository.getInventoryItemById(destinationInventoryItem.getParentInventoryItemId(), org.opentaps.domain.base.entities.InventoryItem.class);

            String inventoryType = destinationInventoryItem.getInventoryItemTypeId();

            // re-set the fields on the item
            if ("NON_SERIAL_INV_ITEM".equals(inventoryType)) {
                // add an adjusting InventoryItemDetail so set ATP back to QOH: ATP = ATP + (QOH - ATP), diff = QOH - ATP
                BigDecimal atp = destinationInventoryItem.getAvailableToPromiseTotal();
                if (atp == null) {
                    atp = BigDecimal.ZERO;
                }
                BigDecimal qoh = destinationInventoryItem.getQuantityOnHandTotal();
                if (qoh == null) {
                    qoh = BigDecimal.ZERO;
                }

                Map<String, Object> createDetailMap =
                    UtilMisc.toMap(
                            "availableToPromiseDiff", atp.negate().doubleValue(),
                            "quantityOnHandDiff", qoh.negate().doubleValue(),
                            "inventoryItemId", destinationInventoryItem.getInventoryItemId(),
                            "userLogin", getUser().getOfbizUserLogin()
                    );

                Map<String, ?> result = runSync("createInventoryItemDetail", createDetailMap);
                if (ServiceUtil.isError(result)) {
                    throw new ServiceException("Inventory Item Detail create problem in cancel inventory transfer");
                }

                createDetailMap =
                    UtilMisc.toMap(
                            "availableToPromiseDiff", qoh.doubleValue(),
                            "quantityOnHandDiff", qoh.doubleValue(),
                            "inventoryItemId", originInventoryItem.getInventoryItemId(),
                            "userLogin", getUser().getOfbizUserLogin()
                    );

                result = runSync("createInventoryItemDetail", createDetailMap);
                if (ServiceUtil.isError(result)) {
                    throw new ServiceException("Inventory Item Detail create problem in cancel inventory transfer");
                }

                // refresh original inventory item because underlying table is implicitly updated with ATP/QOH 
                originInventoryItem = inventoryRepository.getInventoryItemById(originInventoryItem.getInventoryItemId());

                // update received time
                originInventoryItem.setDatetimeReceived(UtilDateTime.nowTimestamp());
                inventoryRepository.update(originInventoryItem);

            } else if (inventoryType.equals("SERIALIZED_INV_ITEM")) {
                destinationInventoryItem.setStatusId("INV_AVAILABLE");
                // store the entity
                inventoryRepository.update(destinationInventoryItem);
            }

            // set the inventory transfer record to complete
            transfer.setStatusId("IXF_CANCELLED");
            inventoryRepository.update(transfer);

        } catch (RepositoryException e) {
            throw new ServiceException(e);
        } catch (EntityNotFoundException e) {
            throw new ServiceException(e);
        }

    }

    /** {@inheritDoc} */
    public void setInventoryItemId(String inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
    }

    /** {@inheritDoc} */
    public String getInventoryItemId() {
        return inventoryItemId;
    }

    /** {@inheritDoc} */
    public void setXferQty(Double xferQty) {
        this.xferQty = BigDecimal.valueOf(xferQty);
    }

    /** {@inheritDoc} */
    public void prepareInventoryTransfer() throws ServiceException {

        try {
            InventoryDomainInterface inventoryDomain = getDomainsDirectory().getInventoryDomain();
            InventoryRepositoryInterface inventoryRepository = inventoryDomain.getInventoryRepository();

            // find the source InventoryItem
            InventoryItem sourceInventoryItem = inventoryRepository.getInventoryItemById(inventoryItemId);

            // by default return the source InventoryItem, unless we have to split
            InventoryItem destinationInventoryItem = sourceInventoryItem;

            // check the quantity is available for transfer
            if (sourceInventoryItem.isSerialized()) {
                if (!sourceInventoryItem.isAvailableToPromise()) {
                    throw new ServiceException("Serialized inventory is not available for transfer.");
                }

                // ensure the transferred inventory will not be touched by setting its status
                sourceInventoryItem.setStatusId(InventorySpecification.INVENTORY_ITEM_STATUS_BEING_TRANSFERED);
                inventoryRepository.update(sourceInventoryItem);
            } else {
                // for non serialized inventory, check its ATP quantity
                BigDecimal atp = sourceInventoryItem.getAvailableToPromiseTotal();
                if (atp.compareTo(xferQty) < 0) {
                    throw new ServiceException("The request transfer amount is not available, the available to promise [" + sourceInventoryItem.getAvailableToPromiseTotal() + "] is not sufficient for the desired transfer quantity [" + xferQty + "] on the Inventory Item with ID " + inventoryItemId);
                }

                // create a new InventoryItem with same general settings as the source
                Map<String, Object> input = createInputMap();
                // clone without ofbiz stamp fields
                input.putAll(sourceInventoryItem.toMapNoStamps());
                // set the parent ID for tracking the origin of the split item
                input.put("parentInventoryItemId", inventoryItemId);
                // remove the PK, it is generated by the service
                input.remove("inventoryItemId");
                // remove the ATP QOH
                input.remove("availableToPromiseTotal");
                input.remove("quantityOnHandTotal");
                input.remove("oldAvailableToPromiseTotal");
                input.remove("oldQuantityOnHandTotal");
                // convert BigDecimal to back their expected values
                input.put("unitCost", sourceInventoryItem.getUnitCost().doubleValue());
                Map<String, Object> tmpResult = runSync("createInventoryItem", input);
                String newInventoryItemId = (String) tmpResult.get("inventoryItemId");

                // create the InventoryItemDetail for the transfer destination
                input = createInputMap();
                input.put("availableToPromiseDiff", xferQty.doubleValue());
                input.put("quantityOnHandDiff", xferQty.doubleValue());
                input.put("inventoryItemId", newInventoryItemId);
                runSync("createInventoryItemDetail", input);

                // create the InventoryItemDetail for the transfer source
                input = createInputMap();
                input.put("availableToPromiseDiff", xferQty.negate().doubleValue());
                input.put("quantityOnHandDiff", xferQty.negate().doubleValue());
                input.put("inventoryItemId", inventoryItemId);
                runSync("createInventoryItemDetail", input);

                // we should return the split InventoryItem
                destinationInventoryItem = inventoryRepository.getInventoryItemById(newInventoryItemId);

                // ensure the transferred inventory will not be touched by making its ATP zero
                atp = destinationInventoryItem.getAvailableToPromiseTotal();
                if (atp.signum() != 0) {
                    input = createInputMap();
                    input.put("availableToPromiseDiff", atp.negate().doubleValue());
                    input.put("inventoryItemId", destinationInventoryItem.getInventoryItemId());
                    runSync("createInventoryItemDetail", input);
                }
            }

            inventoryItemId = destinationInventoryItem.getInventoryItemId();

        } catch (GeneralException ex) {
            throw new ServiceException(ex);
        }
    }

    /** {@inheritDoc} */
    public void setInventoryTransferId(String inventoryTransferId) {
        this.inventoryTransferId = inventoryTransferId;
    }

}
