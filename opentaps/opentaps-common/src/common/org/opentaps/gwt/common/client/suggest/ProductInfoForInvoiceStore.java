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

package org.opentaps.gwt.common.client.suggest;

import java.math.BigDecimal;

import com.gwtext.client.data.FieldDef;
import com.gwtext.client.data.Record;
import com.gwtext.client.data.RecordDef;
import com.gwtext.client.data.Store;
import com.gwtext.client.data.StringFieldDef;

import org.opentaps.gwt.common.client.lookup.configuration.OrderItemsCartLookupConfiguration;
import org.opentaps.gwt.common.client.lookup.configuration.ProductInfoForInvoiceLookupConfiguration;

/**
 * A class that retrieves product information for a product ID and make it available for other widgets.
 */
public class ProductInfoForInvoiceStore extends EntityStaticAutocomplete {

    private String description;
    private String invoiceItemTypeId;
    private BigDecimal unitPrice;

    /**
     * Default constructor.
     * @param productId the productId
     * @param invoiceId the invoice for which to get the product info
     * @param quantity of product for which to get the product info
     */
    public ProductInfoForInvoiceStore(String productId, String invoiceId, BigDecimal quantity) {
        super(ProductInfoForInvoiceLookupConfiguration.URL_FIND_PRODUCT_INFO, ProductInfoForInvoiceLookupConfiguration.INOUT_PRODUCT_ID, false, new RecordDef(
                 new FieldDef[]{
                     new StringFieldDef(ProductInfoForInvoiceLookupConfiguration.OUT_UNIT_PRICE),
                     new StringFieldDef(ProductInfoForInvoiceLookupConfiguration.OUT_DESCRIPTION),
                     new StringFieldDef(ProductInfoForInvoiceLookupConfiguration.OUT_INVOICE_ITEM_TYPE_ID)
                 }
         ));
        setProductId(productId);
        setInvoiceId(invoiceId);
        if (quantity != null) {
            setQuantity(quantity);
        }
        loadFirstPage();
    }

    /**
     * Sets the product.
     * @param productId a <code>String</code> value
     */
    public void setProductId(String productId) {
        applyFilter(ProductInfoForInvoiceLookupConfiguration.INOUT_PRODUCT_ID, productId);
    }

    /**
     * Sets the invoice.
     * @param invoiceId a <code>String</code> value
     */
    public void setInvoiceId(String invoiceId) {
        applyFilter(ProductInfoForInvoiceLookupConfiguration.IN_INVOICE_ID, invoiceId);
    }

    /**
     * Sets the quantity.
     * @param quantity a <code>BigDecimal</code> value
     */
    public void setQuantity(BigDecimal quantity) {
        applyFilter(OrderItemsCartLookupConfiguration.INOUT_QUANTITY, quantity.toString());
    }

    // parse the product info when it has been loaded
    @Override protected void onStoreLoad(Store store, Record[] records) {
        super.onStoreLoad(store, records);
        // 0 indexed record is the permission records
        Record r = getStore().getRecordAt(1);
        String value = r.getAsString(ProductInfoForInvoiceLookupConfiguration.OUT_UNIT_PRICE);
        if (value != null) {
            unitPrice = new BigDecimal(value);
        }
        description = r.getAsString(ProductInfoForInvoiceLookupConfiguration.OUT_DESCRIPTION);
        invoiceItemTypeId = r.getAsString(ProductInfoForInvoiceLookupConfiguration.OUT_INVOICE_ITEM_TYPE_ID);
    }

    /**
     * Gets the loaded product description.
     * @return the product description
     */
    public String getDescription() {
        if (!isLoaded()) {
            return "NOT LOADED";
        }
        return description;
    }

    /**
     * Gets the loaded product invoice item type.
     * @return the product invoice item type
     */
    public String getInvoiceItemTypeId() {
        if (!isLoaded()) {
            return "NOT LOADED";
        }
        return invoiceItemTypeId;
    }

    /**
     * Gets the loaded product unit price.
     * @return the product unit price
     */
    public BigDecimal getUnitPrice() {
        if (!isLoaded()) {
            return null;
        }
        return unitPrice;
    }
}
