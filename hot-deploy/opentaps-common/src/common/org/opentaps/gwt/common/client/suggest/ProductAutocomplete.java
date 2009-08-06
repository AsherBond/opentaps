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

package org.opentaps.gwt.common.client.suggest;

import org.opentaps.gwt.common.client.lookup.configuration.ProductLookupConfiguration;

/**
 * A ComboBox that autocompletes General Ledger Account.
 */
public class ProductAutocomplete extends EntityAutocomplete {

    /**
     * Clone constructor, copy its configuration from the given <code>ProductAutocomplete</code>.
     * @param autocompleter the <code>ProductAutocomplete</code> to clone
     */
    public ProductAutocomplete(ProductAutocomplete autocompleter) {
        super(autocompleter);
    }

    /**
     * Default constructor without form related parameters, to be used for editor.
     */
    public ProductAutocomplete() {
        super(ProductLookupConfiguration.URL_SUGGEST, ProductLookupConfiguration.OUT_PRODUCT_ID);
    }

    /**
     * Default constructor.
     * @param fieldLabel the field label
     * @param name the field name used in the form
     * @param fieldWidth the field size in pixels
     */
    public ProductAutocomplete(String fieldLabel, String name, int fieldWidth) {
        super(fieldLabel, name, fieldWidth, ProductLookupConfiguration.URL_SUGGEST, ProductLookupConfiguration.OUT_PRODUCT_ID);
    }
}
