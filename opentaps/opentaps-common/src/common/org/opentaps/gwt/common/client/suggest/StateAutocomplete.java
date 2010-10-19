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

import org.opentaps.gwt.common.client.lookup.configuration.CountryStateLookupConfiguration;

/**
 * A ComboBox that autocompletes States or Regions.
 */
public class StateAutocomplete extends EntityAutocomplete {

    /**
     * Default constructor.
     * @param fieldLabel the field label
     * @param name the field name used in the form
     * @param fieldWidth the field size in pixels
     */
    public StateAutocomplete(String fieldLabel, String name, int fieldWidth) {
        super(fieldLabel, name, fieldWidth, CountryStateLookupConfiguration.URL_SUGGEST_STATES, CountryStateLookupConfiguration.OUT_GEO_NAME);
    }

    /**
     * Constructor for a state autocompleter linked to an existing country autocompleter.
     * This state autocompleter will suggest its values according to the selected country.
     * @param fieldLabel the field label
     * @param name the field name used in the form
     * @param countryField the instance of the existing country autocompleter to link to
     * @param fieldWidth the field size in pixels
     */
    public StateAutocomplete(String fieldLabel, String name, CountryAutocomplete countryField, int fieldWidth) {
        this(fieldLabel, name, fieldWidth);
        linkToComboBox(countryField, CountryStateLookupConfiguration.IN_COUNTRY_FOR_STATE);
    }
}
