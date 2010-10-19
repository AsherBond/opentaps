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

package org.opentaps.gwt.financials.invoices.client.form;

import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.events.LoadableListener;
import org.opentaps.gwt.common.client.form.CreateOrUpdateEntityForm;
import org.opentaps.gwt.common.client.form.base.ListAndFormPanel;
import org.opentaps.gwt.common.client.listviews.InvoiceItemEditableListView;

/**
 * A combination of a list of current invoice items and a form used to edit the invoice item records.
 */
public class InvoiceItemsEditable extends ListAndFormPanel<CreateOrUpdateEntityForm, InvoiceItemEditableListView>  {

    private final String organizationPartyId;
    private final String invoiceId;
    private final String invoiceTypeId;

    private final InvoiceItemEditableListView listInvoiceItems;

    /**
     * Creates a new <code>InvoiceItemsEditable</code> instance.
     */
    public InvoiceItemsEditable() {
        super();

        // get parameters
        organizationPartyId = getOrganizationPartyId();
        invoiceId = getInvoiceId();
        invoiceTypeId = getInvoiceTypeId();

        // the grid with existing items
        listInvoiceItems = new InvoiceItemEditableListView(UtilUi.MSG.accountingInvoiceItems(), invoiceId, organizationPartyId, invoiceTypeId);
        CreateOrUpdateEntityForm form = listInvoiceItems.getForm();
        addMainForm(form);
        // hide the form by default
        getMainFormPanel().hide();
        addListView(listInvoiceItems);

        // add a notifier
        listInvoiceItems.addLoadableListener(new LoadableListener() {
                public void onLoad() {
                    notifyRecordCount(listInvoiceItems.getInvoiceItemsCount());
                }
            });

    }

    private static native void notifyRecordCount(int n)/*-{
        $wnd.notifyInvoiceItemsCount(n);
    }-*/;

    private static native String getOrganizationPartyId()/*-{
        return $wnd.organizationPartyId;
    }-*/;

    private static native String getInvoiceId()/*-{
        return $wnd.invoiceId;
    }-*/;

    private static native String getInvoiceTypeId()/*-{
        return $wnd.invoiceTypeId;
    }-*/;

}
