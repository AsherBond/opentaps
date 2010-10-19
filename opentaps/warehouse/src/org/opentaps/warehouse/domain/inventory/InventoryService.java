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
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.opentaps.domain.DomainService;
import org.opentaps.base.entities.InventoryTransfer;
import org.opentaps.base.services.CreateInventoryItemDetailService;
import org.opentaps.base.services.CreateInventoryItemService;
import org.opentaps.domain.inventory.InventoryDomainInterface;
import org.opentaps.domain.inventory.InventoryItem;
import org.opentaps.domain.inventory.InventoryRepositoryInterface;
import org.opentaps.domain.inventory.InventoryServiceInterface;
import org.opentaps.domain.inventory.InventorySpecification;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;

/**
 * Inventory management services implemented with POJO Java engine using the opentaps Service foundation class.
 */
public class InventoryService extends DomainService implements InventoryServiceInterface {

    @SuppressWarnings("unused")
    private static final String MODULE = InventoryService.class.getName();

    private String productId = null;
    private String statusId = null;
    private String facilityId = null;

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
    public void setStatusId(String statusId) {
        this.statusId = statusId;
    }

    /** {@inheritDoc} */
    public void setFacilityId(String facilityId) {
        this.facilityId = facilityId;
    }

    /** {@inheritDoc} */
    public void setUseCache(Boolean useCache) {
    }

    /** {@inheritDoc} */
    public BigDecimal getQuantityOnHandTotal() {
        return this.quantityOnHandTotal;
    }

    /** {@inheritDoc} */
    public BigDecimal getAvailableToPromiseTotal() {
        return this.availableToPromiseTotal;
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
                //TODO: it is not a efficient method to filter inventory item by facilityId/statusId, we should add another method in inventoryRepository
                // to get match result directly.
                // filter other status item
                if (statusId != null && !item.getStatusId().equals(statusId)) {
                    continue;
                }
                // filter other facilityId item
                if (facilityId != null && !item.getFacilityId().equals(facilityId)) {
                    continue;
                }
                availableToPromiseTotal = availableToPromiseTotal.add(item.getNetATP());
                quantityOnHandTotal = quantityOnHandTotal.add(item.getNetQOH());
            }

        } catch (GeneralException ex) {
            throw new ServiceException(ex);
        }
    }

    /** {@inheritDoc} */
    public void cancelInventoryTransfer() throws ServiceException {
        InventoryRepositoryInterface inventoryRepository = null;
        org.opentaps.base.entities.InventoryItem originInventoryItem = null;
        org.opentaps.base.entities.InventoryItem destinationInventoryItem = null;
        InventoryTransfer transfer = null;

        try {
            inventoryRepository = getDomainsDirectory().getInventoryDomain().getInventoryRepository();

            transfer = inventoryRepository.getInventoryTransferById(inventoryTransferId);
            // the transfer destination InventoryItem
            destinationInventoryItem = transfer.getInventoryItem();
            // the transfer origin InventoryItem
            originInventoryItem = inventoryRepository.getInventoryItemById(destinationInventoryItem.getParentInventoryItemId(), org.opentaps.base.entities.InventoryItem.class);

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

                CreateInventoryItemDetailService service = new CreateInventoryItemDetailService();
                service.setInAvailableToPromiseDiff(atp.negate());
                service.setInQuantityOnHandDiff(qoh.negate());
                service.setInInventoryItemId(destinationInventoryItem.getInventoryItemId());
                runSync(service);

                service = new CreateInventoryItemDetailService();
                service.setInAvailableToPromiseDiff(qoh);
                service.setInQuantityOnHandDiff(qoh);
                service.setInInventoryItemId(originInventoryItem.getInventoryItemId());
                runSync(service);

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
    public void setXferQty(BigDecimal xferQty) {
        this.xferQty = xferQty;
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
                CreateInventoryItemService service = new CreateInventoryItemService();
                // clone without ofbiz stamp fields
                Map<String, Object> cloned = sourceInventoryItem.toMapNoStamps();
                // remove the PK, it is generated by the service
                cloned.remove("inventoryItemId");
                // remove the ATP QOH
                cloned.remove("availableToPromiseTotal");
                cloned.remove("quantityOnHandTotal");
                cloned.remove("oldAvailableToPromiseTotal");
                cloned.remove("oldQuantityOnHandTotal");
                service.putAllInput(cloned);
                // set the parent ID for tracking the origin of the split item
                service.setInParentInventoryItemId(inventoryItemId);
                // convert BigDecimal to back their expected values
                service.setInUnitCost(sourceInventoryItem.getUnitCost());
                runSync(service);
                String newInventoryItemId = service.getOutInventoryItemId();

                // create the InventoryItemDetail for the transfer destination
                CreateInventoryItemDetailService service2 = new CreateInventoryItemDetailService();
                service2.setInAvailableToPromiseDiff(xferQty);
                service2.setInQuantityOnHandDiff(xferQty);
                service2.setInInventoryItemId(newInventoryItemId);
                runSync(service2);

                // create the InventoryItemDetail for the transfer source
                service2 = new CreateInventoryItemDetailService();
                service2.setInAvailableToPromiseDiff(xferQty.negate());
                service2.setInQuantityOnHandDiff(xferQty.negate());
                service2.setInInventoryItemId(inventoryItemId);
                runSync(service2);

                // we should return the split InventoryItem
                destinationInventoryItem = inventoryRepository.getInventoryItemById(newInventoryItemId);

                // ensure the transferred inventory will not be touched by making its ATP zero
                atp = destinationInventoryItem.getAvailableToPromiseTotal();
                if (atp.signum() != 0) {
                    service2 = new CreateInventoryItemDetailService();
                    service2.setInAvailableToPromiseDiff(atp.negate());
                    service2.setInInventoryItemId(destinationInventoryItem.getInventoryItemId());
                    runSync(service2);
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
