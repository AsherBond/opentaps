/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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
package org.opentaps.gwt.crmsfa.orders.client;

import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import com.gwtext.client.widgets.Panel;
import org.opentaps.gwt.common.client.BaseEntry;
import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.form.FormNotificationInterface;
import org.opentaps.gwt.common.client.form.OrderItemsEditable;
import org.opentaps.gwt.common.client.listviews.OrderItemsEditableListView.OrderType;
import org.opentaps.gwt.crmsfa.orders.client.form.ProductReReservationForm;


/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Entry extends BaseEntry {

    private static final String RE_RESERVE_DIALOG = "reReserveItemDialog";
    private static final String ORDER_ITEMS_ID = "orderItemsEntryGrid";

    /**
     * This is the entry point method.
     * It is loaded for page where the meta tag is found
     */
    public void onModuleLoad() {

        /*
         * Order view page may contains number of <div/> with id in form reReserveItemDialog_0_1
         * where first digit is order item index and second one is inventory index for that order
         * item index.
         *
         *  Try to get elements with all reasonable identifiers and install link widgets into them.
         */
        RootPanel currentPanel;
        Integer orderItemIndex = 0;
        Integer inventoryItemIndex = 0;

        while (true) {
            String indexedId = RE_RESERVE_DIALOG + "_" + orderItemIndex.toString() + "_" + inventoryItemIndex.toString();

            // try to find the indexed div in the page
            currentPanel = RootPanel.get(indexedId);

            if (currentPanel != null) {
                // insert the button to re-reserve inventory
                loadReReserveDialog(currentPanel, orderItemIndex, inventoryItemIndex);
                // go to next inventory item
                inventoryItemIndex++;
            } else {
                // if the inventory item index is zero, it means we have no more div to find
                if (inventoryItemIndex == 0) {
                    break;
                } else {
                    // else go to next order item
                    inventoryItemIndex = 0;
                    orderItemIndex++;
                }
            }
        }

        if (RootPanel.get(ORDER_ITEMS_ID) != null) {
            loadOrderItems();
        }

    }

    private void loadOrderItems() {
        OrderItemsEditable orderItemsEditable = new OrderItemsEditable(OrderType.SALES);
        RootPanel.get(ORDER_ITEMS_ID).add(orderItemsEditable.getMainPanel());
    }

    /**
     * Add link beside inventory item and open form for re-reservation on click.
     * @param container the container <code>RootPanel</code>
     * @param orderItemIndex an <code>Integer</code> value
     * @param oisgrIndex an <code>Integer</code> value
     */
    private void loadReReserveDialog(RootPanel container, Integer orderItemIndex, Integer oisgrIndex) {
        Panel panel = new Panel();
        panel.setBorder(false);

        Dictionary facilities = Dictionary.getDictionary("facilityList");
        Dictionary widgetParameters = Dictionary.getDictionary("reReservationWidgetParameters");

        final ProductReReservationForm window = new ProductReReservationForm(
                UtilUi.MSG.opentapsReReserveProduct(),
                facilities,
                getOrderId(),
                widgetParameters.get("orderItemSeqId_" + orderItemIndex.toString()),
                widgetParameters.get("inventoryItemId_" + orderItemIndex.toString() + "_" + oisgrIndex.toString()),
                widgetParameters.get("shipGroupSeqId_" + orderItemIndex.toString() + "_" + oisgrIndex.toString()),
                widgetParameters.get("quantity_" + orderItemIndex.toString() + "_" + oisgrIndex.toString())
        );
        window.create();

        window.register(new FormNotificationInterface() {
            public void notifySuccess() {
                Window.Location.replace(Window.Location.getHref());
            }
        });

        Hyperlink embedLink = new Hyperlink(UtilUi.MSG.opentapsReReserve(), null);
        embedLink.setStyleName("buttontext");
        embedLink.addClickListener(new ClickListener() {

            public void onClick(Widget sender) {
                window.show();
            }
        });

        panel.add(embedLink);
        container.add(panel);
    }

    RootPanel getNextRootPanel(int lastOrderItemIndex, int lastInventoryItemIndex) {
        return null;
    }

    /**
     * Retrieve JS variable <code>orderId</code>.
     * @return the <code>orderId</code>
     */
    public static native String getOrderId() /*-{
        return $wnd.orderId;
    }-*/;

}
