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

package org.opentaps.gwt.common.client.suggest;

import org.opentaps.gwt.common.client.lookup.configuration.InvoiceItemTypeLookupConfiguration;

/**
 * A ComboBox that autocompletes General Ledger Account.
 */
public class InvoiceItemTypeAutocomplete extends EntityStaticAutocomplete {

    /**
     * Clone constructor, copy its configuration from the given <code>InvoiceItemTypeAutocomplete</code>.
     * @param autocompleter the <code>InvoiceItemTypeAutocomplete</code> to clone
     */
    public InvoiceItemTypeAutocomplete(InvoiceItemTypeAutocomplete autocompleter) {
        super(autocompleter);
    }

    /**
     * Default constructor.
     * @param organizationPartyId the organizationPartyId to filter by
     * @param invoiceTypeId the invoiceTypeId to filter by
     */
    public InvoiceItemTypeAutocomplete(String organizationPartyId, String invoiceTypeId) {
        super(InvoiceItemTypeLookupConfiguration.URL_SUGGEST, InvoiceItemTypeLookupConfiguration.OUT_SEQUENCE, false);
        setOrganizationPartyId(organizationPartyId);
        setInvoiceTypeId(invoiceTypeId);
        loadFirstPage();
    }

    /**
     * Default constructor.
     * @param fieldLabel the field label
     * @param name the field name used in the form
     * @param fieldWidth the field size in pixels
     */
    public InvoiceItemTypeAutocomplete(String fieldLabel, String name, int fieldWidth) {
        super(fieldLabel, name, fieldWidth, InvoiceItemTypeLookupConfiguration.URL_SUGGEST, InvoiceItemTypeLookupConfiguration.OUT_SEQUENCE, false);
    }

    /**
     * Default constructor.
     * @param fieldLabel the field label
     * @param name the field name used in the form
     * @param fieldWidth the field size in pixels
     * @param organizationPartyId the organizationPartyId to filter by
     */
    public InvoiceItemTypeAutocomplete(String fieldLabel, String name, int fieldWidth, String organizationPartyId) {
        super(fieldLabel, name, fieldWidth, InvoiceItemTypeLookupConfiguration.URL_SUGGEST, InvoiceItemTypeLookupConfiguration.OUT_SEQUENCE, false);
        setOrganizationPartyId(organizationPartyId);
        loadFirstPage();
    }

    /**
     * Default constructor.
     * @param fieldLabel the field label
     * @param name the field name used in the form
     * @param fieldWidth the field size in pixels
     * @param organizationPartyId the organizationPartyId to filter by
     * @param invoiceTypeId the invoiceTypeId to filter by
     */
    public InvoiceItemTypeAutocomplete(String fieldLabel, String name, int fieldWidth, String organizationPartyId, String invoiceTypeId) {
        super(fieldLabel, name, fieldWidth, InvoiceItemTypeLookupConfiguration.URL_SUGGEST, InvoiceItemTypeLookupConfiguration.OUT_SEQUENCE, false);
        setOrganizationPartyId(organizationPartyId);
        setInvoiceTypeId(invoiceTypeId);
        loadFirstPage();
    }

    /**
     * Sets the organization to lookup for.
     * @param organizationPartyId a <code>String</code> value
     */
    public void setOrganizationPartyId(String organizationPartyId) {
        applyFilter(InvoiceItemTypeLookupConfiguration.IN_ORGANIZATION, organizationPartyId);
    }

    /**
     * Sets the invoice type to lookup for.
     * @param invoiceTypeId a <code>String</code> value
     */
    public void setInvoiceTypeId(String invoiceTypeId) {
        applyFilter(InvoiceItemTypeLookupConfiguration.IN_INVOICE_TYPE, invoiceTypeId);
    }
}
