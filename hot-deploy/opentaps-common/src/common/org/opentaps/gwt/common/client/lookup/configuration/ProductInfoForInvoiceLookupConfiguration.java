/*
 * Copyright (c) 2009 - 2009 Open Source Strategies, Inc.
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

package org.opentaps.gwt.common.client.lookup.configuration;

import java.util.Arrays;
import java.util.List;

/**
 * Defines the interface between the server and client for the ProductInfoLookupService
 * Technically not a java interface, but it defines all the constants needed on both sides
 *  which makes the code more robust.
 */
public abstract class ProductInfoForInvoiceLookupConfiguration {

    private ProductInfoForInvoiceLookupConfiguration() { }

    public static final String URL_FIND_PRODUCT_INFO = "gwtGetProductInfoForInvoice";

    public static final String INOUT_PRODUCT_ID = "productId";
    public static final String IN_INVOICE_ID = "invoiceId";
    public static final String INOUT_QUANTITY = "quantity";

    public static final String OUT_UNIT_PRICE = "unitPrice";
    public static final String OUT_DESCRIPTION = "description";
    public static final String OUT_INVOICE_ITEM_TYPE_ID = "invoiceItemTypeId";

    public static final List<String> LIST_OUT_FIELDS = Arrays.asList(
        INOUT_PRODUCT_ID,
        OUT_DESCRIPTION,
        OUT_INVOICE_ITEM_TYPE_ID,
        OUT_UNIT_PRICE
    );
}
