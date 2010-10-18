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
 * Defines the interface between the server and client for the PurchaseOrderLookupService
 * Technically not a java interface, but it defines all the constants needed on both sides
 *  which makes the code more robust.
 */
public abstract class PurchaseOrderLookupConfiguration {

    private PurchaseOrderLookupConfiguration() { }

    public static final String URL_FIND_ORDERS = "gwtFindPurchasingOrders";
    public static final String URL_SEARCH_ORDERS = "gwtSearchPurchasingOrders";

    public static final String IN_FIND_ALL = "findAll";

    public static final String INOUT_ORDER_ID = "orderId";
    public static final String IN_PRODUCT_PARTTERN = "productPattern";
    public static final String INOUT_ORDER_NAME = "orderName";
    public static final String OUT_ORDER_NAME_ID = "orderNameId";
    public static final String INOUT_PARTY_ID = "partyId";
    public static final String INOUT_STATUS_ID = "statusId";
    public static final String INOUT_ORDER_DATE = "orderDate";
    public static final String IN_FROM_DATE = "fromDate";
    public static final String IN_THRU_DATE = "thruDate";
    public static final String IN_CREATED_BY = "createdBy";
    public static final String OUT_GRAND_TOTAL = "grandTotal";
    public static final String OUT_CURRENCY_UOM = "currencyUom";
    public static final String OUT_ORDER_DATE_STRING = "orderDateString";
    public static final String OUT_SUPPLIER_NAME = "partyName";
    public static final String OUT_STATUS_DESCRIPTION = "statusDescription";

    public static final List<String> LIST_OUT_FIELDS = Arrays.asList(
            INOUT_ORDER_ID,
            INOUT_ORDER_NAME,
            OUT_ORDER_NAME_ID,
            INOUT_PARTY_ID,
            INOUT_STATUS_ID,
            INOUT_ORDER_DATE,
            OUT_GRAND_TOTAL,
            OUT_CURRENCY_UOM,
            OUT_ORDER_DATE_STRING,
            OUT_SUPPLIER_NAME,
            OUT_STATUS_DESCRIPTION
    );
    public static final List<String> LIST_QUERY_FIELDS = Arrays.asList(
            INOUT_ORDER_ID,
            INOUT_ORDER_NAME,
            INOUT_PARTY_ID,
            INOUT_STATUS_ID,
            INOUT_ORDER_DATE,
            OUT_GRAND_TOTAL,
            OUT_CURRENCY_UOM
    );

}
