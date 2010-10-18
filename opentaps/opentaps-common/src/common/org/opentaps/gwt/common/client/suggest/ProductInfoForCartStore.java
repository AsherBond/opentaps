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

/**
 * A class that retrieves product information for a product ID and make it available for other widgets.
 */
public class ProductInfoForCartStore extends EntityStaticAutocomplete {

    private String description;
    private BigDecimal unitPrice;

    /**
     * Default constructor.
     * @param productId the productId
     */
    public ProductInfoForCartStore(String productId) {
        this(productId, null);
    }

    /**
     * Default constructor.
     * @param productId the productId
     * @param quantity the quantity
     */
    public ProductInfoForCartStore(String productId, BigDecimal quantity) {
        super(OrderItemsCartLookupConfiguration.URL_FIND_PRODUCT_INFO, OrderItemsCartLookupConfiguration.INOUT_PRODUCT, false, new RecordDef(
                 new FieldDef[]{
                     new StringFieldDef(OrderItemsCartLookupConfiguration.INOUT_UNIT_PRICE),
                     new StringFieldDef(OrderItemsCartLookupConfiguration.INOUT_DESCRIPTION)
                 }
         ));
        setProductId(productId);
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
        applyFilter(OrderItemsCartLookupConfiguration.INOUT_PRODUCT, productId);
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
        String value = r.getAsString(OrderItemsCartLookupConfiguration.INOUT_UNIT_PRICE);
        if (value != null) {
            unitPrice = new BigDecimal(value);
        }
        description = r.getAsString(OrderItemsCartLookupConfiguration.INOUT_DESCRIPTION);
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
