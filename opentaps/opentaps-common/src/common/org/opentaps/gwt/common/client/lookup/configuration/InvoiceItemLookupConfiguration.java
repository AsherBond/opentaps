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
 * Defines the interface between the server and client for the InvoiceItemLookupService
 * Technically not a java interface, but it defines all the constants needed on both sides
 *  which makes the code more robust.
 */
public abstract class InvoiceItemLookupConfiguration {

    private InvoiceItemLookupConfiguration() { }

    public static final String URL_FIND_INVOICE_ITEMS = "gwtFindInvoiceItems";
    public static final String URL_POST_INVOICE_ITEMS = "gwtPostInvoiceItems";
    public static final String URL_POST_INVOICE_ITEMS_BATCH = "gwtPostInvoiceItemsBatch";

    public static final String INOUT_INVOICE_ID = "invoiceId";
    public static final String INOUT_ITEM_SEQUENCE = "invoiceItemSeqId";
    public static final String INOUT_ITEM_TYPE = "invoiceItemTypeId";
    public static final String INOUT_DESCRIPTION = "description";
    public static final String INOUT_GL_ACCOUNT = "overrideGlAccountId";
    public static final String INOUT_PRODUCT = "productId";
    public static final String INOUT_QUANTITY = "quantity";
    public static final String INOUT_AMOUNT = "amount";
    public static final String INOUT_TAXABLE = "taxableFlag";
    public static final String INOUT_TAX_AUTH = "taxAuthPartyId";
    public static final String OUT_TAX_AUTH_DESCRIPTION = "taxAuthPartyIdDescription";
    public static final String OUT_PRODUCT_DESCRIPTION = "productIdDescription";
    public static final String OUT_GL_ACCOUNT_DESCRIPTION = "overrideGlAccountIdDescription";

    public static final List<String> LIST_OUT_FIELDS = AccountingTagLookupConfiguration.addAccountingTagsToFieldList(Arrays.asList(
        INOUT_INVOICE_ID,
        INOUT_ITEM_SEQUENCE,
        INOUT_ITEM_TYPE,
        INOUT_DESCRIPTION,
        INOUT_GL_ACCOUNT,
        INOUT_PRODUCT,
        INOUT_QUANTITY,
        INOUT_AMOUNT,
        INOUT_TAXABLE,
        INOUT_TAX_AUTH,
        OUT_TAX_AUTH_DESCRIPTION,
        OUT_PRODUCT_DESCRIPTION,
        OUT_GL_ACCOUNT_DESCRIPTION
    ));

    /**
     * Defines which invoice types should allow product entry for its items.
     */
    public static final List<String> WITH_PRODUCT_INVOICE_TYPES = Arrays.asList(
        "SALES_INVOICE",
        "PURCHASE_INVOICE"
    );

}
