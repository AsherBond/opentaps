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

import org.opentaps.gwt.common.client.lookup.configuration.GlAccountLookupConfiguration;

/**
 * A ComboBox that autocompletes General Ledger Account.
 */
public class GlAccountAutocomplete extends EntityAutocomplete {

    /**
     * Clone constructor, copy its configuration from the given <code>GlAccountAutocomplete</code>.
     * @param autocompleter the <code>GlAccountAutocomplete</code> to clone
     */
    public GlAccountAutocomplete(GlAccountAutocomplete autocompleter) {
        super(autocompleter);
    }

    /**
     * Default constructor.
     */
    public GlAccountAutocomplete() {
        super(GlAccountLookupConfiguration.URL_SUGGEST, GlAccountLookupConfiguration.OUT_CODE);
    }

    /**
     * Default constructor.
     * @param fieldLabel the field label
     * @param name the field name used in the form
     * @param fieldWidth the field size in pixels
     */
    public GlAccountAutocomplete(String fieldLabel, String name, int fieldWidth) {
        super(fieldLabel, name, fieldWidth, GlAccountLookupConfiguration.URL_SUGGEST, GlAccountLookupConfiguration.OUT_CODE);
    }
}
