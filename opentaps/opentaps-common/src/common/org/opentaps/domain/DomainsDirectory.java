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
package org.opentaps.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.opentaps.domain.activities.ActivitiesDomainInterface;
import org.opentaps.domain.billing.BillingDomainInterface;
import org.opentaps.domain.crmsfa.teams.CrmTeamDomainInterface;
import org.opentaps.domain.dataimport.DataImportDomainInterface;
import org.opentaps.domain.inventory.InventoryDomainInterface;
import org.opentaps.domain.ledger.LedgerDomainInterface;
import org.opentaps.domain.manufacturing.ManufacturingDomainInterface;
import org.opentaps.domain.order.OrderDomainInterface;
import org.opentaps.domain.organization.OrganizationDomainInterface;
import org.opentaps.domain.party.PartyDomainInterface;
import org.opentaps.domain.product.ProductDomainInterface;
import org.opentaps.domain.purchasing.PurchasingDomainInterface;
import org.opentaps.domain.search.SearchDomainInterface;
import org.opentaps.domain.security.SecurityDomainInterface;
import org.opentaps.domain.shipping.ShippingDomainInterface;
import org.opentaps.domain.voip.VoipDomainInterface;
import org.opentaps.domain.webapp.WebAppDomainInterface;
import org.opentaps.foundation.domain.DomainInterface;
import org.opentaps.foundation.infrastructure.DomainContextInterface;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;

public class DomainsDirectory implements DomainContextInterface {

    // these are public so you can refer to domains as literals outside this class well
    // note that these string values aren't important -- it's the names of the set methods that need to match the domains-directory.xml's
    // Spring bean definitions
    public static final String BILLING_DOMAIN = "BillingDomain";
    public static final String CRMSFA_DOMAIN = "CrmsfaDomain";
    public static final String PURCHASING_DOMAIN = "PurchasingDomain";
    public static final String MANUFACTURING_DOMAIN = "ManufacturingDomain";
    public static final String SHIPPING_DOMAIN = "ShippingDomain";
    public static final String ORGANIZATION_DOMAIN = "OrganizationDomain";
    public static final String LEDGER_DOMAIN = "LedgerDomain";
    public static final String PRODUCT_DOMAIN = "ProductDomain";
    public static final String INVENTORY_DOMAIN = "InventoryDomain";
    public static final String PARTY_DOMAIN = "PartyDomain";
    public static final String ORDER_DOMAIN = "OrderDomain";
    public static final String SEARCH_DOMAIN = "SearchDomain";
    public static final String VOIP_DOMAIN = "VoipDomain";
    public static final String WEBAPP_DOMAIN = "WebappDomain";
    public static final String DATA_IMPORT_DOMAIN = "DataImportDomain";
    public static final String SECURITY_DOMAIN = "SecurityDomain";
    public static final String ACTIVITIES_DOMAIN = "ActivitiesDomain";
    public static final String CRM_TEAMS_DOMAIN = "CrmTeamsDomain";

    // Holds a map of Domain Key (the name of the domain) to DomainInterfaces.
    private Map<String, DomainInterface> domainDirectories = new HashMap<String, DomainInterface>();
    private Infrastructure infrastructure;
    private User user;

    /**
     * Default constructor.
     */
    public DomainsDirectory() { }

    /**
    * Clone constructor.
     * Copy the map of registered domains.
     * @param dd a <code>DomainsDirectory</code> value
     */
    public DomainsDirectory(DomainsDirectory dd) {
        this.domainDirectories = new HashMap<String, DomainInterface>(dd.domainDirectories);
    }

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
        return new DomainsLoader(object.getInfrastructure(), object.getUser()).getDomainsDirectory();
    }

    /**
     * Adds the specified domain to the directory.  May be retrieved by using
     * the specified name.  If a domain is already registered for the specified
     * name, the previous domain will be unregistered and returned.
     *
     * @param name The name of the domain.
     * @param newDomain The DomainInterface for the domain.
     * @return previous DomainInterface associated with specified key, or <tt>null</tt> if there was no previous.
     */
    public DomainInterface addDomain(String name, DomainInterface newDomain) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("null is not a valid domain name key");
        }
        if (newDomain == null) {
            throw new IllegalArgumentException("Cannot add a null domain");
        }
        domainDirectories.put(name, newDomain);
        return getDomain(name);
    }
    /**
     * Return a Set of the names of registered domains.
     *
     * @return Set of the names of registered domains.
     */
    public Set<String> listDomains() {
        return domainDirectories.keySet();
    }

    /**
     * Returns the DomainInterface registered to the specified name.
     *
     * @param name The name of the domain.
     * @return the DomainInterface registered for the specified name, or
     *         <tt>null</tt> if no such domain is registered.
     */
    public DomainInterface getDomain(String name) {
        DomainInterface domain = domainDirectories.get(name);
        if (domain != null) {
            // for some reason these need to be set here again, or they will come up as null
            domain.setInfrastructure(getInfrastructure());
            domain.setUser(getUser());
        }
        return domain;
    }


    public void setBillingDomain(BillingDomainInterface domain) {
        addDomain(BILLING_DOMAIN, domain);
    }

    public BillingDomainInterface getBillingDomain() {
        return (BillingDomainInterface) getDomain(BILLING_DOMAIN);
    }

    public void setOrderDomain(OrderDomainInterface domain) {
        addDomain(ORDER_DOMAIN, domain);
    }

    public OrderDomainInterface getOrderDomain() {
        return (OrderDomainInterface) getDomain(ORDER_DOMAIN);
    }

    public PartyDomainInterface getPartyDomain() {
        return (PartyDomainInterface) getDomain(PARTY_DOMAIN);
    }

    public void setPartyDomain(PartyDomainInterface partyDomain) {
        addDomain(PARTY_DOMAIN, partyDomain);
    }

    public InventoryDomainInterface getInventoryDomain() {
        return (InventoryDomainInterface) getDomain(INVENTORY_DOMAIN);
    }

    public void setInventoryDomain(InventoryDomainInterface inventoryDomain) {
        addDomain(INVENTORY_DOMAIN, inventoryDomain);
    }

    public ProductDomainInterface getProductDomain() {
        return (ProductDomainInterface) getDomain(PRODUCT_DOMAIN);
    }

    public void setProductDomain(ProductDomainInterface productDomain) {
        addDomain(PRODUCT_DOMAIN, productDomain);
    }

    public CrmTeamDomainInterface getCrmTeamDomain() {
        return (CrmTeamDomainInterface) getDomain(CRM_TEAMS_DOMAIN);
    }

    public void setCrmTeamDomain(CrmTeamDomainInterface crmTeamDomain) {
        addDomain(CRM_TEAMS_DOMAIN, crmTeamDomain);
    }

    public LedgerDomainInterface getLedgerDomain() {
        return (LedgerDomainInterface) getDomain(LEDGER_DOMAIN);
    }

    public void setLedgerDomain(LedgerDomainInterface ledgerDomain) {
        addDomain(LEDGER_DOMAIN, ledgerDomain);
    }

    public OrganizationDomainInterface getOrganizationDomain() {
        return (OrganizationDomainInterface) getDomain(ORGANIZATION_DOMAIN);
    }

    public void setOrganizationDomain(OrganizationDomainInterface organizationDomain) {
        addDomain(ORGANIZATION_DOMAIN, organizationDomain);
    }

    public ShippingDomainInterface getShippingDomain() {
        return (ShippingDomainInterface) getDomain(SHIPPING_DOMAIN);
   }

    public void setShippingDomain(ShippingDomainInterface shippingDomain) {
        addDomain(SHIPPING_DOMAIN, shippingDomain);
    }

    public ManufacturingDomainInterface getManufacturingDomain() {
        return (ManufacturingDomainInterface) getDomain(MANUFACTURING_DOMAIN);
    }

    public void setManufacturingDomain(ManufacturingDomainInterface manufacturingDomain) {
        addDomain(MANUFACTURING_DOMAIN, manufacturingDomain);
    }

    public PurchasingDomainInterface getPurchasingDomain() {
        return (PurchasingDomainInterface) getDomain(PURCHASING_DOMAIN);
    }

    public void setPurchasingDomain(PurchasingDomainInterface purchasingDomain) {
        addDomain(PURCHASING_DOMAIN, purchasingDomain);
    }

    public VoipDomainInterface getVoipDomain() {
        return (VoipDomainInterface) getDomain(VOIP_DOMAIN);
    }

    public void setVoipDomain(VoipDomainInterface voipDomain) {
        addDomain(VOIP_DOMAIN, voipDomain);
    }

    public SearchDomainInterface getSearchDomain() {
        return (SearchDomainInterface) getDomain(SEARCH_DOMAIN);

    }

    public void setSearchDomain(SearchDomainInterface searchDomain) {
        addDomain(SEARCH_DOMAIN, searchDomain);
    }

    public WebAppDomainInterface getWebAppDomain() {
        return (WebAppDomainInterface) getDomain(WEBAPP_DOMAIN);
    }

    public void setWebAppDomain(WebAppDomainInterface webAppDomain) {
        addDomain(WEBAPP_DOMAIN, webAppDomain);
    }

    public DataImportDomainInterface getDataImportDomain() {
        return (DataImportDomainInterface) getDomain(DATA_IMPORT_DOMAIN);
    }

    public void setDataImportDomain(DataImportDomainInterface dataImportDomain) {
        addDomain(DATA_IMPORT_DOMAIN, dataImportDomain);
    }

    public SecurityDomainInterface getSecurityDomain() {
        return (SecurityDomainInterface) getDomain(SECURITY_DOMAIN);
    }

    public void setSecurityDomain(SecurityDomainInterface securityDomain) {
        addDomain(SECURITY_DOMAIN, securityDomain);
    }

    public ActivitiesDomainInterface getActivitiesDomain() {
        return (ActivitiesDomainInterface) getDomain(ACTIVITIES_DOMAIN);
    }
    
    public void setActivitiesDomain(ActivitiesDomainInterface activitiesDomain) {
        addDomain(ACTIVITIES_DOMAIN, activitiesDomain);
    }
}
