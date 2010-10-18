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

package org.opentaps.gwt.purchasing.client;

import com.google.gwt.user.client.ui.RootPanel;
import org.opentaps.gwt.common.client.BaseEntry;
import org.opentaps.gwt.common.client.form.MultiSearchForm;
import org.opentaps.gwt.common.client.form.OrderItemsEditable;
import org.opentaps.gwt.common.client.listviews.OrderItemsEditableListView.OrderType;
import org.opentaps.gwt.common.client.listviews.PurchaseOrderSearchListView;
import org.opentaps.gwt.common.client.listviews.SupplierSearchListView;
import org.opentaps.gwt.purchasing.client.orders.form.FindOrdersForm;
import org.opentaps.gwt.purchasing.client.suppliers.form.FindSuppliersForm;
import org.opentaps.gwt.purchasing.client.suppliers.listviews.SupplierOpenOrdersListView;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Entry extends BaseEntry {
    private static final String SEARCH_ID = "gwtSearch";
    private static final String ORDER_ITEMS_ID = "orderItemsEntryGrid";
    private static final String OPEN_ORDERS_ID = "openOrders";
    private static final String FIND_ORDERS_ID = "findOrders";
    private static final String FIND_SUPPLIERS_ID = "findSuppliers";
    private static final String SUPPLIER_ORDERS = "supplierOpenOrdersSubsection";

    private static final int PARTIES_PAGE_SIZE = 20;

    private FindOrdersForm findOrdersForm;
    private FindOrdersForm openOrdersForm;
    private FindSuppliersForm findSuppliersForm;

    private MultiSearchForm multiCrmsfaSearch;

    /**
     * This is the entry point method.
     * It is loaded for page where the meta tag is found.
     */
    public void onModuleLoad() {
        loadOrdersWidgets();
        if (RootPanel.get(FIND_SUPPLIERS_ID) != null) {
            loadFindSuppliers();
        }
        if (RootPanel.get(SEARCH_ID) != null) {
            multiCrmsfaSearch = new MultiSearchForm();
            multiCrmsfaSearch.addResultsGrid(new SupplierSearchListView());
            multiCrmsfaSearch.addResultsGrid(new PurchaseOrderSearchListView());
            RootPanel.get(SEARCH_ID).add(multiCrmsfaSearch);
        }

        if (RootPanel.get(SUPPLIER_ORDERS) != null) {
            loadSupplierOrders();
        }
    }

    private void loadOrderItems() {
        OrderItemsEditable orderItemsEditable = new OrderItemsEditable(OrderType.PURCHASE);
        RootPanel.get(ORDER_ITEMS_ID).add(orderItemsEditable.getMainPanel());
    }

    private void loadOrdersWidgets() {
        if (RootPanel.get(OPEN_ORDERS_ID) != null) {
            loadOpenOrders();
        }
        if (RootPanel.get(FIND_ORDERS_ID) != null) {
            loadFindOrders();
        }
        if (RootPanel.get(ORDER_ITEMS_ID) != null) {
            loadOrderItems();
        }
    }

    private void loadFindOrders() {
        findOrdersForm = new FindOrdersForm(false);
        RootPanel.get(FIND_ORDERS_ID).add(findOrdersForm.getMainPanel());
    }

    private void loadOpenOrders() {
        openOrdersForm = new FindOrdersForm(false);
        openOrdersForm.hideFilters();
        openOrdersForm.getListView().filterHasIncludeInactiveOrders(false);
        openOrdersForm.getListView().applyFilters();
        RootPanel.get(OPEN_ORDERS_ID).add(openOrdersForm.getMainPanel());
    }

    private void loadFindSuppliers() {
        findSuppliersForm = new FindSuppliersForm();
        findSuppliersForm.getListView().setPageSize(PARTIES_PAGE_SIZE);
        findSuppliersForm.getListView().applyFilters();
        RootPanel.get(FIND_SUPPLIERS_ID).add(findSuppliersForm.getMainPanel());
    }

    private void loadSupplierOrders() {
        SupplierOpenOrdersListView orders = new SupplierOpenOrdersListView(getPartyId());
        RootPanel.get(SUPPLIER_ORDERS).add(orders);
    }

    /**
     * Retrieve JS variable <code>partyId</code>.
     * @return the <code>partyId</code>
     */
    private static native String getPartyId()/*-{
        return $wnd.partyId;
    }-*/;
}
