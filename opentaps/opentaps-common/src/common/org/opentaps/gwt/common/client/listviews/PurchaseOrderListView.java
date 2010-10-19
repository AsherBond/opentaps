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
package org.opentaps.gwt.common.client.listviews;

import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.lookup.configuration.PurchaseOrderLookupConfiguration;

import com.gwtext.client.core.SortDir;
import com.gwtext.client.data.Record;
import com.gwtext.client.data.Store;
import com.gwtext.client.data.StringFieldDef;
import com.gwtext.client.widgets.grid.CellMetadata;
import com.gwtext.client.widgets.grid.ColumnConfig;
import com.gwtext.client.widgets.grid.Renderer;

/**
 * Class for the Find purchase order form + list view pattern.
 */
public class PurchaseOrderListView extends EntityListView {

    private static final String MODULE = PurchaseOrderListView.class.getName();

    /**
     * Default constructor.
     */
    public PurchaseOrderListView() {
        super();
    }

    /**
     * Constructor giving a title for this list view, which is displayed in the UI.
     * @param title the title label for this list view.
     */
    public PurchaseOrderListView(String title) {
        super(title);
    }

    /**
     * Placeholder to remind extended classes that on of the init methods must be called.
     */
    public void init() {
        init(PurchaseOrderLookupConfiguration.URL_FIND_ORDERS, "/purchasing/control/orderview?orderId={0}", UtilUi.MSG.orderOrderId());
    }

    /**
     * Configures the list columns and interaction with the server request that populates it.
     * Constructs the column model and JSON reader for the list with the default columns for Party and extra columns, as well as a link for a view page.
     * @param entityFindUrl the URL of the request to populate the list
     * @param entityViewUrl the URL linking to the entity view page with a placeholder for the ID. The ID column will use it to provide a link to the view page for each record. For example <code>/crmsfa/control/viewContact?partyId={0}</code>. This is optional, if <code>null</code> then no link will be provided
     * @param idLabel the label of the ID column, which depends of the entity that is listed
     */
    protected void init(String entityFindUrl, String entityViewUrl, String idLabel) {
        StringFieldDef idDefinition = new StringFieldDef(PurchaseOrderLookupConfiguration.INOUT_ORDER_ID);

        ColumnConfig columnOrderDate = makeColumn(UtilUi.MSG.orderOrderDate(), new StringFieldDef(PurchaseOrderLookupConfiguration.OUT_ORDER_DATE_STRING));
        columnOrderDate.setWidth(100);

        ColumnConfig columnOrderNameId = makeLinkColumn(UtilUi.MSG.crmOrderNameID(), idDefinition, new StringFieldDef(PurchaseOrderLookupConfiguration.OUT_ORDER_NAME_ID), entityViewUrl, true);
        columnOrderNameId.setWidth(150);

        ColumnConfig columnSupplierName = makeColumn(UtilUi.MSG.productSupplier(), new StringFieldDef(PurchaseOrderLookupConfiguration.OUT_SUPPLIER_NAME));
        columnSupplierName.setWidth(120);
        columnSupplierName.setRenderer(new Renderer() {
            public String render(Object value, CellMetadata cellMetadata, Record record, int rowIndex, int colNum, Store store) {
                // bold priority field if record is updated
                String supplierName = record.getAsString(PurchaseOrderLookupConfiguration.OUT_SUPPLIER_NAME);
                String supplierPartyId = record.getAsString(PurchaseOrderLookupConfiguration.INOUT_PARTY_ID);
                return supplierName + " (" + supplierPartyId + ")";
            }
        });

        ColumnConfig columnStatus = makeColumn(UtilUi.MSG.commonStatus(), new StringFieldDef(PurchaseOrderLookupConfiguration.OUT_STATUS_DESCRIPTION));
        columnStatus.setWidth(80);

        ColumnConfig columnAmount = makeCurrencyColumn(UtilUi.MSG.orderAmount(), new StringFieldDef(PurchaseOrderLookupConfiguration.OUT_CURRENCY_UOM), new StringFieldDef(PurchaseOrderLookupConfiguration.OUT_GRAND_TOTAL));
        columnAmount.setWidth(80);

        // a column for supplier party Id
        makeColumn("", new StringFieldDef(PurchaseOrderLookupConfiguration.INOUT_PARTY_ID)).setHidden(true);
        getColumn().setFixed(true);

        configure(entityFindUrl, PurchaseOrderLookupConfiguration.INOUT_ORDER_DATE, SortDir.DESC);
    }

    /**
     * Filters the records of the list by order name matching the given order name.
     * @param orderName a <code>String</code> value
     */
    public void filterByOrderName(String orderName) {
        setFilter(PurchaseOrderLookupConfiguration.INOUT_ORDER_NAME, orderName);
    }

    /**
     * Filters the records of the list by order Id matching the given orderId.
     * @param orderId a <code>String</code> value
     */
    public void filterByOrderId(String orderId) {
        setFilter(PurchaseOrderLookupConfiguration.INOUT_ORDER_ID, orderId);
    }


    /**
     * Filters the records of the list by supplier Id matching the given orderId.
     * @param supplierId a <code>String</code> value
     */
    public void filterBySupplierId(String supplierId) {
        setFilter(PurchaseOrderLookupConfiguration.INOUT_PARTY_ID, supplierId);
    }

    /**
     * Filters the records of the list by order status Id matching the given statusId.
     * @param statusId a <code>String</code> value
     */
    public void filterByStatusId(String statusId) {
        setFilter(PurchaseOrderLookupConfiguration.INOUT_STATUS_ID, statusId);
    }

    /**
     * Filters the records of the list by from Date matching the given fromDate.
     * @param fromDate a <code>String</code> value
     */
    public void filterByFromDate(String fromDate) {
        setFilter(PurchaseOrderLookupConfiguration.IN_FROM_DATE, fromDate);
    }

    /**
     * Filters the records of the list by thru Date matching the given thruDate.
     * @param thruDate a <code>String</code> value
     */
    public void filterByThruDate(String thruDate) {
        setFilter(PurchaseOrderLookupConfiguration.IN_THRU_DATE, thruDate);
    }

    /**
     * Filters the records of the list by created by matching the given createdBy.
     * @param createdBy a <code>String</code> value
     */
    public void filterByCreatedBy(String createdBy) {
        setFilter(PurchaseOrderLookupConfiguration.IN_CREATED_BY, createdBy);
    }

    /**
     * Filters the records of the list by given product pattern.
     * @param productPattern a <code>String</code> value
     */
    public void filterByProductPattern(String productPattern) {
        setFilter(PurchaseOrderLookupConfiguration.IN_PRODUCT_PARTTERN, productPattern);
    }

    /**
     * Filters the records of the list if include the inactive orders.
     * @param findAll a <code>boolean</code> value
     */
    public void filterHasIncludeInactiveOrders(boolean findAll) {
        setFilter(PurchaseOrderLookupConfiguration.IN_FIND_ALL, findAll ? "Y" : "N");
    }

}
