/*
 * Copyright (c) 2008 - 2009 Open Source Strategies, Inc.
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
package org.opentaps.domain.order;

import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.Set;
import java.sql.Timestamp;

import org.opentaps.foundation.entity.Entity;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.repository.RepositoryException;

import org.opentaps.domain.base.entities.OrderShipmentInfoSummary;
import org.opentaps.domain.party.Party;

/**
 * Order Item Ship Group entity.
 */
public class OrderItemShipGroup extends org.opentaps.domain.base.entities.OrderItemShipGroup {

    private Party supplier;
    private Timestamp estimatedShipDate;
    private List<Shipment> primaryShipments;
    private List<OrderItemShipGrpInvRes> shipGroupInventoryReservations;
    private List<OrderShipmentInfoSummary> shipmentInfoSummaries;
    private Set<Map<String, Object>> groupedShipmentInfoSummaries;

    /**
     * Default constructor.
     */
    public OrderItemShipGroup() {
        super();
    }

    /**
     * Get the specification object which contains enumerations and logical checking for Orders.
     * @return the <code>OrderSpecificationInterface</code>
     */
    public OrderSpecificationInterface getOrderSpecification() {
        return getRepository().getOrderSpecification();
    }

    /**
     * Is this order item ship group set with an "unknown" address.
     * @return a <code>Boolean</code> value
     */
    public Boolean hasUnknownPostalAddress() {
        return getOrderSpecification().getUnknownShippingAddress().equals(getContactMechId());
    }

    /**
     * Gets the estimated ship date base on the promisedDate of the related <code>OrderItemShipGrpInvRes</code>.
     * @return the estimated ship date
     * @throws RepositoryException if an error occurs
     */
    public Timestamp getEstimatedShipDate() throws RepositoryException {
        if (estimatedShipDate == null) {
            Timestamp result = null;
            for (OrderItemShipGrpInvRes res : getInventoryReservations()) {
                if (res.getPromisedDatetime() == null) {
                    continue;
                }

                if (result == null) {
                    result = res.getPromisedDatetime();
                } else if ("Y".equals(getMaySplit())) {
                    if (res.getPromisedDatetime().before(result)) { result = res.getPromisedDatetime(); }
                } else {
                    if (res.getPromisedDatetime().after(result)) { result = res.getPromisedDatetime(); }
                }
            }
            estimatedShipDate = result;
        }
        return estimatedShipDate;
    }

    /**
     * Gets the supplier <code>Party</code> for this order item ship group.
     * @return the supplier party domain object
     * @exception RepositoryException if an error occurs
     */
    public Party getSupplier() throws RepositoryException {
        if (supplier == null && getSupplierPartyId() != null) {
            try {
                supplier = getRepository().getPartyById(getSupplierPartyId());
            } catch (EntityNotFoundException e) {
                // remove this object supplierPartyId in case we want recall this method
                setSupplierPartyId(null);
            }
        }
        return supplier;
    }

    /**
     * Gets the primary shipments for this order item ship group.
     * Returns the order domain object instead of the base entity.
     * @return list of <code>OrderShipment</code>
     * @throws RepositoryException if an error occurs
     */
    @Override
    public List<Shipment> getPrimaryShipments() throws RepositoryException {
        if (primaryShipments == null) {
            primaryShipments = getRelated(Shipment.class, "PrimaryShipment");
        }
        return primaryShipments;
    }

    /**
     * Gets the inventory reservations for this order item ship group.
     * Returns the order domain object instead of the base entity.
     * @return list of <code>OrderItemShipGrpInvRes</code>
     * @throws RepositoryException if an error occurs
     */
    @Override
    public List<OrderItemShipGrpInvRes> getOrderItemShipGrpInvReses() throws RepositoryException {
        if (shipGroupInventoryReservations == null) {
            shipGroupInventoryReservations = getRelated(OrderItemShipGrpInvRes.class, "OrderItemShipGrpInvRes");
        }
        return shipGroupInventoryReservations;
    }

    /**
     * Gets the inventory reservations for this order item ship group.
     * This is an alias for {@link #getOrderItemShipGrpInvReses}
     * @return list of <code>OrderItemShipGrpInvRes</code>
     * @throws RepositoryException if an error occurs
     */
    public List<OrderItemShipGrpInvRes> getInventoryReservations() throws RepositoryException {
        return this.getOrderItemShipGrpInvReses();
    }

    /**
     * Gets the shipments info summary for this order item ship group.
     * @return list of <code>OrderShipmentInfoSummary</code>
     * @throws RepositoryException if an error occurs
     * @see #getGroupedShipmentInfoSummaries
     */
    public List<OrderShipmentInfoSummary> getShipmentInfoSummaries() throws RepositoryException {
        if (shipmentInfoSummaries == null) {
            shipmentInfoSummaries = getRepository().getRelatedOrderShipmentInfoSummaries(this);
        }
        return shipmentInfoSummaries;
    }

    /**
     * Gets a subset of the list of <code>OrderShipmentInfoSummary</code>.
     * Groups the values by:
     * - shipmentPackageSeqId
     * - trackingCode
     * - boxNumber
     * - carrierPartyId
     * @return a <code>Set</code> containing the grouped <code>OrderShipmentInfoSummary</code>
     * @throws RepositoryException if an error occurs
     * @see #getShipmentInfoSummaries
     */
    public Set<Map<String, Object>> getGroupedShipmentInfoSummaries() throws RepositoryException {
        if (groupedShipmentInfoSummaries == null) {
            groupedShipmentInfoSummaries = Entity.getDistinctFieldValues(getShipmentInfoSummaries(), Arrays.asList("shipmentPackageSeqId", "trackingCode", "boxNumber", "carrierPartyId"));
        }
        return groupedShipmentInfoSummaries;
    }

    private OrderRepositoryInterface getRepository() {
        return OrderRepositoryInterface.class.cast(repository);
    }
}
