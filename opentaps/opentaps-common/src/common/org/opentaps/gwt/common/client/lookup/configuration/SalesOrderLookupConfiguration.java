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
package org.opentaps.gwt.common.client.lookup.configuration;

import java.util.Arrays;
import java.util.List;

/**
 * Defines the interface between the server and client for the SalesOrderLookupService
 * Technically not a java interface, but it defines all the constants needed on both sides
 *  which makes the code more robust.
 */
public abstract class SalesOrderLookupConfiguration {

    private SalesOrderLookupConfiguration() { }

    public static final String URL_FIND_ORDERS = "gwtFindSalesOrders";
    public static final String URL_SEARCH_ORDERS = "gwtSearchSalesOrders";

    public static final String IN_RESPONSIBILTY = "MyOrTeamResponsibility";
    public static final String MY_VALUES = "MY_VALUES";

    public static final String IN_FIND_ALL = "findAll";
    public static final String IN_DESIRED = "desiredOrdersOnly";

    public static final String INOUT_ORDER_ID = "orderId";
    public static final String IN_EXTERNAL_ID = "externalId";
    public static final String IN_PRODUCT_ID = "productId";
    public static final String INOUT_ORDER_NAME = "orderName";
    public static final String OUT_ORDER_NAME_ID = "orderNameId";
    public static final String INOUT_PARTY_ID = "partyId";
    public static final String IN_PRODUCT_STORE_ID = "productStoreId";
    public static final String INOUT_STATUS_ID = "statusId";
    public static final String INOUT_CORRESPONDING_PO_ID = "correspondingPoId";
    public static final String INOUT_ORDER_DATE = "orderDate";
    public static final String IN_FROM_DATE = "fromDate";
    public static final String IN_THRU_DATE = "thruDate";
    public static final String IN_CREATED_BY = "createdBy";
    public static final String IN_LOT_ID = "lotId";
    public static final String IN_SERIAL_NUMBER = "serialNumber";
    public static final String IN_SHIPPING_ADDRESS = "shippingAddress";
    public static final String IN_SHIPPING_CITY = "shippingCity";
    public static final String IN_SHIPPING_COUNTRY = "shippingCountry";
    public static final String IN_SHIPPING_STATE = "shippingState";
    public static final String IN_SHIPPING_POSTAL_CODE = "shippingPostalCode";
    public static final String IN_SHIPPING_TO_NAME = "shippingToName";
    public static final String IN_SHIPPING_ATTENTION_NAME = "shippingAttnName";
    public static final String OUT_GRAND_TOTAL = "grandTotal";
    public static final String OUT_CURRENCY_UOM = "currencyUom";
    public static final String OUT_ORDER_DATE_STRING = "orderDateString";
    public static final String OUT_CUSTOMER_NAME = "partyName";
    public static final String OUT_SHIP_BY_DATE_STRING = "shipByDateString";
    public static final String OUT_STATUS_DESCRIPTION = "statusDescription";
    public static final String OUT_TRACKING_CODE_ID = "trackingCodeId";

    public static final List<String> LIST_OUT_FIELDS = Arrays.asList(
            INOUT_ORDER_ID,
            INOUT_ORDER_NAME,
            OUT_ORDER_NAME_ID,
            INOUT_PARTY_ID,
            INOUT_STATUS_ID,
            INOUT_CORRESPONDING_PO_ID,
            INOUT_ORDER_DATE,
            OUT_GRAND_TOTAL,
            OUT_CURRENCY_UOM,
            OUT_ORDER_DATE_STRING,
            OUT_CUSTOMER_NAME,
            OUT_SHIP_BY_DATE_STRING,
            OUT_STATUS_DESCRIPTION,
            OUT_TRACKING_CODE_ID
    );
    public static final List<String> LIST_QUERY_FIELDS = Arrays.asList(
            INOUT_ORDER_ID,
            INOUT_ORDER_NAME,
            INOUT_PARTY_ID,
            INOUT_STATUS_ID,
            INOUT_CORRESPONDING_PO_ID,
            INOUT_ORDER_DATE,
            OUT_GRAND_TOTAL,
            OUT_CURRENCY_UOM,
            OUT_TRACKING_CODE_ID
    );

}
