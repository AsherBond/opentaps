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

import com.gwtext.client.data.BooleanFieldDef;
import com.gwtext.client.data.Converter;

/**
 * A <code>FieldDef</code> that should be used to read Y/N strings into <code>boolean</code> values.
 */
public class StringBooleanFieldDef extends BooleanFieldDef {

    /**
     * Constructor.
     * @param name the field name
     */
    public StringBooleanFieldDef(String name) {
        super(name, null, new BooleanConverter());
    }

    private static class BooleanConverter implements Converter {
        public String format(String raw) {
            if ("Y".equalsIgnoreCase(raw)) {
                return "true";
            } else {
                return "false";
            }

        }
    }

}
