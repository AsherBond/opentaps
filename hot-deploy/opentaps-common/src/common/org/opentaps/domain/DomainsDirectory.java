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
package org.opentaps.domain;

import org.opentaps.domain.billing.BillingDomainInterface;
import org.opentaps.domain.inventory.InventoryDomainInterface;
import org.opentaps.domain.ledger.LedgerDomainInterface;
import org.opentaps.domain.order.OrderDomainInterface;
import org.opentaps.domain.organization.OrganizationDomainInterface;
import org.opentaps.domain.party.PartyDomainInterface;
import org.opentaps.domain.product.ProductDomainInterface;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.domain.shipping.ShippingDomainInterface;
import org.opentaps.domain.manufacturing.ManufacturingDomainInterface;
import org.opentaps.domain.purchasing.PurchasingDomainInterface;


public class DomainsDirectory {

    private BillingDomainInterface billingDomain;
    private OrderDomainInterface orderDomain;
    private PartyDomainInterface partyDomain;
    private InventoryDomainInterface inventoryDomain;
    private ProductDomainInterface productDomain;
    private LedgerDomainInterface ledgerDomain;
    private OrganizationDomainInterface organizationDomain;
    private ShippingDomainInterface shippingDomain;
    private ManufacturingDomainInterface manufacturingDomain;
    private PurchasingDomainInterface purchasingDomain;

    private Infrastructure infrastructure;
    private User user;

    public Infrastructure getInfrastructure() {
        return infrastructure;
    }

    public void setInfrastructure(Infrastructure infrastructure) {
        this.infrastructure = infrastructure;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setBillingDomain(BillingDomainInterface domain) {
        billingDomain = domain;
    }

    public BillingDomainInterface getBillingDomain() {
        // for some unknown reason, these don't get set when the domain is loaded, so they need to be set here, or they'll be null
        billingDomain.setInfrastructure(this.infrastructure);
        billingDomain.setUser(this.user);
        return billingDomain;
    }

    public void setOrderDomain(OrderDomainInterface domain) {
        orderDomain = domain;
    }

    public OrderDomainInterface getOrderDomain() {
        // for some unknown reason, these don't get set when the domain is loaded, so they need to be set here, or they'll be null
        orderDomain.setInfrastructure(this.infrastructure);
        orderDomain.setUser(this.user);
        return orderDomain;
    }

    public PartyDomainInterface getPartyDomain() {
        partyDomain.setInfrastructure(infrastructure);
        partyDomain.setUser(user);
        return partyDomain;
    }

    public void setPartyDomain(PartyDomainInterface partyDomain) {
        this.partyDomain = partyDomain;
    }

    public InventoryDomainInterface getInventoryDomain() {
        inventoryDomain.setInfrastructure(infrastructure);
        inventoryDomain.setUser(user);
        return inventoryDomain;
    }

    public void setInventoryDomain(InventoryDomainInterface inventoryDomain) {
        this.inventoryDomain = inventoryDomain;
    }

    public ProductDomainInterface getProductDomain() {
        productDomain.setInfrastructure(infrastructure);
        productDomain.setUser(user);
        return productDomain;
    }

    public void setProductDomain(ProductDomainInterface productDomain) {
        this.productDomain = productDomain;
    }

    public LedgerDomainInterface getLedgerDomain() {
        ledgerDomain.setInfrastructure(infrastructure);
        ledgerDomain.setUser(user);
        return ledgerDomain;
    }

    public void setLedgerDomain(LedgerDomainInterface ledgerDomain) {
        this.ledgerDomain = ledgerDomain;
    }

    public OrganizationDomainInterface getOrganizationDomain() {
        organizationDomain.setInfrastructure(infrastructure);
        organizationDomain.setUser(user);
        return organizationDomain;
    }

    public void setOrganizationDomain(OrganizationDomainInterface organizationDomain) {
        this.organizationDomain = organizationDomain;
    }

    public ShippingDomainInterface getShippingDomain() {
        shippingDomain.setInfrastructure(infrastructure);
        shippingDomain.setUser(user);
        return shippingDomain;
    }

    public void setShippingDomain(ShippingDomainInterface shippingDomain) {
        this.shippingDomain = shippingDomain;
    }

    public ManufacturingDomainInterface getManufacturingDomain() {
        manufacturingDomain.setInfrastructure(infrastructure);
        manufacturingDomain.setUser(user);
        return manufacturingDomain;
    }

    public void setManufacturingDomain(ManufacturingDomainInterface manufacturingDomain) {
        this.manufacturingDomain = manufacturingDomain;
    }

    public PurchasingDomainInterface getPurchasingDomain() {
        purchasingDomain.setInfrastructure(infrastructure);
        purchasingDomain.setUser(user);
        return purchasingDomain;
    }

    public void setPurchasingDomain(PurchasingDomainInterface purchasingDomain) {
        this.purchasingDomain = purchasingDomain;
    }
}
