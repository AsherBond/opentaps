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
import org.opentaps.gwt.common.client.lookup.configuration.SalesOrderLookupConfiguration;

import com.gwtext.client.core.SortDir;
import com.gwtext.client.data.StringFieldDef;
import com.gwtext.client.widgets.grid.ColumnConfig;

/**
 * Class for the Find sales order form + list view pattern.
 */
public class SalesOrderListView extends EntityListView {

    /**
     * Default constructor.
     */
    public SalesOrderListView() {
        super();
    }

    /**
     * Constructor giving a title for this list view, which is displayed in the UI.
     * @param title the title label for this list view.
     */
    public SalesOrderListView(String title) {
        super(title);
    }

    /**
     * Placeholder to remind extended classes that on of the init methods must be called.
     */
    public void init() {
        init(SalesOrderLookupConfiguration.URL_FIND_ORDERS, "/crmsfa/control/orderview?orderId={0}", UtilUi.MSG.orderOrderId());
    }

    /**
     * Configures the list columns and interaction with the server request that populates it.
     * Constructs the column model and JSON reader for the list with the default columns for Party and extra columns, as well as a link for a view page.
     * @param entityFindUrl the URL of the request to populate the list
     * @param entityViewUrl the URL linking to the entity view page with a placeholder for the ID. The ID column will use it to provide a link to the view page for each record. For example <code>/crmsfa/control/viewContact?partyId={0}</code>. This is optional, if <code>null</code> then no link will be provided
     * @param idLabel the label of the ID column, which depends of the entity that is listed
     */
    protected void init(String entityFindUrl, String entityViewUrl, String idLabel) {
        StringFieldDef idDefinition = new StringFieldDef(SalesOrderLookupConfiguration.INOUT_ORDER_ID);

        ColumnConfig columnOrderDate = makeColumn(UtilUi.MSG.orderOrderDate(), new StringFieldDef(SalesOrderLookupConfiguration.OUT_ORDER_DATE_STRING));
        columnOrderDate.setWidth(100);

        ColumnConfig columnOrderNameId = makeLinkColumn(UtilUi.MSG.crmOrderNameID(), idDefinition, new StringFieldDef(SalesOrderLookupConfiguration.OUT_ORDER_NAME_ID), entityViewUrl, true);
        columnOrderNameId.setWidth(150);

        ColumnConfig columnCorrespondingPoId = makeColumn(UtilUi.MSG.opentapsPONumber(), new StringFieldDef(SalesOrderLookupConfiguration.INOUT_CORRESPONDING_PO_ID));
        columnCorrespondingPoId.setWidth(80);

        ColumnConfig columnCustomer = makeColumn(UtilUi.MSG.crmCustomer(), new StringFieldDef(SalesOrderLookupConfiguration.OUT_CUSTOMER_NAME));
        columnCustomer.setWidth(120);

        ColumnConfig columnStatus = makeColumn(UtilUi.MSG.commonStatus(), new StringFieldDef(SalesOrderLookupConfiguration.OUT_STATUS_DESCRIPTION));
        columnStatus.setWidth(100);

        ColumnConfig columnShipByDate = makeColumn(UtilUi.MSG.orderShipBeforeDate(), new StringFieldDef(SalesOrderLookupConfiguration.OUT_SHIP_BY_DATE_STRING));
        columnShipByDate.setWidth(100);
        // the ship by date is not directly sortable
        columnShipByDate.setSortable(false);

        ColumnConfig columnAmount = makeCurrencyColumn(UtilUi.MSG.orderAmount(), new StringFieldDef(SalesOrderLookupConfiguration.OUT_CURRENCY_UOM), new StringFieldDef(SalesOrderLookupConfiguration.OUT_GRAND_TOTAL));
        columnAmount.setWidth(100);

        ColumnConfig columnTrackingCodeId = makeColumn(UtilUi.MSG.orderTrackingCode(), new StringFieldDef(SalesOrderLookupConfiguration.OUT_TRACKING_CODE_ID));
        columnTrackingCodeId.setWidth(100);
        columnTrackingCodeId.setHidden(true);

        configure(entityFindUrl, SalesOrderLookupConfiguration.INOUT_ORDER_DATE, SortDir.DESC);
    }


    /**
     * Filters the records of the list by showing only those belonging to the user making the request.
     * @param viewPref a <code>Boolean</code> value
     */
    public void filterMyOrTeamParties(String viewPref) {
        setFilter(SalesOrderLookupConfiguration.IN_RESPONSIBILTY, viewPref);
    }

    /**
     * Filters the records of the list by order name matching the given order name.
     * @param orderName a <code>String</code> value
     */
    public void filterByOrderName(String orderName) {
        setFilter(SalesOrderLookupConfiguration.INOUT_ORDER_NAME, orderName);
    }

    /**
     * Filters the records of the list by order Id matching the given orderId.
     * @param orderId a <code>String</code> value
     */
    public void filterByOrderId(String orderId) {
        setFilter(SalesOrderLookupConfiguration.INOUT_ORDER_ID, orderId);
    }

    /**
     * Filters the records of the list by order Id matching the given orderId.
     * @param externalId a <code>String</code> value
     */
    public void filterByExternalId(String externalId) {
        setFilter(SalesOrderLookupConfiguration.IN_EXTERNAL_ID, externalId);
    }

    /**
     * Filters the records of the list by customer Id matching the given orderId.
     * @param customerId a <code>String</code> value
     */
    public void filterByCustomerId(String customerId) {
        setFilter(SalesOrderLookupConfiguration.INOUT_PARTY_ID, customerId);
    }

    /**
     * Filters the records of the list by product store Id matching the given productStoreId.
     * @param productStoreId a <code>String</code> value
     */
    public void filterByProductStoreId(String productStoreId) {
        setFilter(SalesOrderLookupConfiguration.IN_PRODUCT_STORE_ID, productStoreId);
    }

    /**
     * Filters the records of the list by order status Id matching the given statusId.
     * @param statusId a <code>String</code> value
     */
    public void filterByStatusId(String statusId) {
        setFilter(SalesOrderLookupConfiguration.INOUT_STATUS_ID, statusId);
    }

    /**
     * Filters the records of the list by corresponding Po Id matching the given correspondingPoId.
     * @param correspondingPoId a <code>String</code> value
     */
    public void filterByCorrespondingPoId(String correspondingPoId) {
        setFilter(SalesOrderLookupConfiguration.INOUT_CORRESPONDING_PO_ID, correspondingPoId);
    }

    /**
     * Filters the records of the list by from Date matching the given fromDate.
     * @param fromDate a <code>String</code> value
     */
    public void filterByFromDate(String fromDate) {
        setFilter(SalesOrderLookupConfiguration.IN_FROM_DATE, fromDate);
    }


    /**
     * Filters the records of the list by thru Date matching the given thruDate.
     * @param thruDate a <code>String</code> value
     */
    public void filterByThruDate(String thruDate) {
        setFilter(SalesOrderLookupConfiguration.IN_THRU_DATE, thruDate);
    }

    /**
     * Filters the records of the list by created by matching the given createdBy.
     * @param createdBy a <code>String</code> value
     */
    public void filterByCreatedBy(String createdBy) {
        setFilter(SalesOrderLookupConfiguration.IN_CREATED_BY, createdBy);
    }

    /**
     * Filters the records of the list by lot Id matching the given lotId.
     * @param lotId a <code>String</code> value
     */
    public void filterByLotId(String lotId) {
        setFilter(SalesOrderLookupConfiguration.IN_LOT_ID, lotId);
    }

    /**
     * Filters the records of the list by serial number matching the given serialNumber.
     * @param serialNumber a <code>String</code> value
     */
    public void filterBySerialNumber(String serialNumber) {
        setFilter(SalesOrderLookupConfiguration.IN_SERIAL_NUMBER, serialNumber);
    }

    /**
     * Filters the records of the list if include the inactive orders.
     * @param findAll a <code>boolean</code> value
     */
    public void filterIncludeInactiveOrders(boolean findAll) {
        setFilter(SalesOrderLookupConfiguration.IN_FIND_ALL, findAll ? "Y" : "N");
    }

    /**
     * Filters the records of the list by address of the party matching the given sub string.
     * @param address a <code>String</code> value
     */
    public void filterByShippingAddress(String address) {
        setFilter(SalesOrderLookupConfiguration.IN_SHIPPING_ADDRESS, address);
    }

    /**
     * Filters the records of the list by country of the party matching the given country id.
     * @param countryGeoId a <code>String</code> value
     */
    public void filterByShippingCountry(String countryGeoId) {
        setFilter(SalesOrderLookupConfiguration.IN_SHIPPING_COUNTRY, countryGeoId);
    }

    /**
     * Filters the records of the list by state / province of the party matching the given state / province id.
     * @param stateProvinceGeoId a <code>String</code> value
     */
    public void filterByShippingStateProvince(String stateProvinceGeoId) {
        setFilter(SalesOrderLookupConfiguration.IN_SHIPPING_STATE, stateProvinceGeoId);
    }

    /**
     * Filters the records of the list by city of the party matching the given sub string.
     * @param city a <code>String</code> value
     */
    public void filterByShippingCity(String city) {
        setFilter(SalesOrderLookupConfiguration.IN_SHIPPING_CITY, city);
    }

    /**
     * Filters the records of the list by postal code of the party matching the given sub string.
     * @param postalCode a <code>String</code> value
     */
    public void filterByShippingPostalCode(String postalCode) {
        setFilter(SalesOrderLookupConfiguration.IN_SHIPPING_POSTAL_CODE, postalCode);
    }

    /**
     * Filters the records of the list by the to name of the party matching the given sub string.
     * @param toName a <code>String</code> value
     */
    public void filterByShippingToName(String toName) {
        setFilter(SalesOrderLookupConfiguration.IN_SHIPPING_TO_NAME, toName);
    }

    /**
     * Filters the records of the list by the attention name of the party matching the given sub string.
     * @param attnName a <code>String</code> value
     */
    public void filterByShippingAttnName(String attnName) {
        setFilter(SalesOrderLookupConfiguration.IN_SHIPPING_ATTENTION_NAME, attnName);
    }

    /**
     * Filters the records of the list by product Id matching the given productId.
     * @param productId a <code>String</code> value
     */
    public void filterByProductId(String productId) {
        setFilter(SalesOrderLookupConfiguration.IN_PRODUCT_ID, productId);
    }
}
