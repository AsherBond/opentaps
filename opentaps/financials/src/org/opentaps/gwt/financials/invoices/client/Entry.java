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

package org.opentaps.gwt.financials.invoices.client;

import com.google.gwt.user.client.ui.RootPanel;
import org.opentaps.gwt.common.client.BaseEntry;
import org.opentaps.gwt.financials.invoices.client.form.InvoiceItemsEditable;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Entry extends BaseEntry {

    private InvoiceItemsEditable invoiceItemsEditable;

    private static final String INVOICE_ITEMS_ID = "invoiceItems";

    /**
     * This is the entry point method.
     * It is loaded for page where the meta tag is found.
     */
    public void onModuleLoad() {

        if (RootPanel.get(INVOICE_ITEMS_ID) != null) {
            loadInvoiceItems();
        }
    }

    private void loadInvoiceItems() {
        invoiceItemsEditable = new InvoiceItemsEditable();
        RootPanel.get(INVOICE_ITEMS_ID).add(invoiceItemsEditable.getMainPanel());
    }

}
