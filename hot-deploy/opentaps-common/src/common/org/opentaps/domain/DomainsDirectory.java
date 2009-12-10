/*
 * Copyright (c) 2007 - 2009 Open Source Strategies, Inc.
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
package org.opentaps.domain;

import org.opentaps.domain.billing.BillingDomainInterface;
import org.opentaps.domain.inventory.InventoryDomainInterface;
import org.opentaps.domain.ledger.LedgerDomainInterface;
import org.opentaps.domain.manufacturing.ManufacturingDomainInterface;
import org.opentaps.domain.order.OrderDomainInterface;
import org.opentaps.domain.organization.OrganizationDomainInterface;
import org.opentaps.domain.party.PartyDomainInterface;
import org.opentaps.domain.product.ProductDomainInterface;
import org.opentaps.domain.purchasing.PurchasingDomainInterface;
import org.opentaps.domain.search.SearchDomainInterface;
import org.opentaps.domain.shipping.ShippingDomainInterface;
import org.opentaps.domain.voip.VoipDomainInterface;
import org.opentaps.domain.webapp.WebAppDomainInterface;
import org.opentaps.foundation.infrastructure.DomainContextInterface;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;

public class DomainsDirectory implements DomainContextInterface {

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
    private VoipDomainInterface voipDomain;
    private SearchDomainInterface searchDomain;
    private WebAppDomainInterface webAppDomain;

    private Infrastructure infrastructure;
    private User user;

    /** {@inheritDoc} */
    public Infrastructure getInfrastructure() {
        return infrastructure;
    }

    /** {@inheritDoc} */
    public void setInfrastructure(Infrastructure infrastructure) {
        this.infrastructure = infrastructure;
    }

    /** {@inheritDoc} */
    public User getUser() {
        return user;
    }

    /** {@inheritDoc} */
    public void setUser(User user) {
        this.user = user;
    }

    /** {@inheritDoc} */
    public void setDomainContext(DomainContextInterface context) {
        this.setDomainContext(context.getInfrastructure(), context.getUser());
    }

    /** {@inheritDoc} */
    public void setDomainContext(Infrastructure infrastructure, User user) {
        this.setInfrastructure(infrastructure);
        this.setUser(user);
    }


    /**
     * Builds a <code>DomainsDirectory</code> instance from a <code>DomainContextInterface</code>.
     * @param object a <code>DomainContextInterface</code> instance
     * @return the domain directory
     */
    public static DomainsDirectory getDomainsDirectory(DomainContextInterface object) {
        return new DomainsLoader(object.getInfrastructure(), object.getUser()).loadDomainsDirectory();
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

    public VoipDomainInterface getVoipDomain() {
        voipDomain.setInfrastructure(infrastructure);
        voipDomain.setUser(user);
        return voipDomain;
    }

    public void setVoipDomain(VoipDomainInterface voipDomain) {
        this.voipDomain = voipDomain;
    }

    public SearchDomainInterface getSearchDomain() {
        searchDomain.setInfrastructure(infrastructure);
        searchDomain.setUser(user);
        return searchDomain;
    }

    public void setSearchDomain(SearchDomainInterface searchDomain) {
        this.searchDomain = searchDomain;
    }
    
    public WebAppDomainInterface getWebAppDomain() {
        webAppDomain.setInfrastructure(infrastructure);
        webAppDomain.setUser(user);
        return webAppDomain;
    }

    public void setWebAppDomain(WebAppDomainInterface webAppDomain) {
        this.webAppDomain = webAppDomain;
    }

}
