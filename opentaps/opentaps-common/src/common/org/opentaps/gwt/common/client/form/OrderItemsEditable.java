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

package org.opentaps.gwt.common.client.form;

import org.opentaps.gwt.common.client.events.LoadableListener;
import org.opentaps.gwt.common.client.form.base.ListAndFormPanel;
import org.opentaps.gwt.common.client.listviews.OrderItemsEditableListView;
import org.opentaps.gwt.common.client.listviews.OrderItemsEditableListView.OrderType;


/**
 * A combination of a list of a cart order items and a form used to edit the order item records.
 */
public class OrderItemsEditable extends ListAndFormPanel<CreateOrUpdateEntityForm, OrderItemsEditableListView>  {

    private final String organizationPartyId;

    private final OrderItemsEditableListView listOrderItems;

    /**
     * Creates a new <code>OrderItemsEditable</code> instance.
     * @param type the <code>OrderType</code>
     */
    public OrderItemsEditable(OrderType type) {
        super();

        // get parameters
        organizationPartyId = getOrganizationPartyId();

        // the grid with existing items
        listOrderItems = new OrderItemsEditableListView(type, organizationPartyId);
        CreateOrUpdateEntityForm form = listOrderItems.getForm();
        addMainForm(form);
        // hide the form by default
        getMainFormPanel().hide();
        addListView(listOrderItems);

        // add a notifier
        listOrderItems.addLoadableListener(new LoadableListener() {
                public void onLoad() {
                    notifyRecordCount(listOrderItems.getOrderItemsCount());
                }
            });

    }

    private static native void notifyRecordCount(int n)/*-{
        $wnd.notifyOrderItemsCount(n);
    }-*/;

    private static native String getOrganizationPartyId()/*-{
        return $wnd.organizationPartyId;
    }-*/;

}
