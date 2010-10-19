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

package org.opentaps.gwt.common.client.form.field;

/**
 * A Hidden input.
 */
public class HiddenField extends com.gwtext.client.widgets.form.Hidden {

    /**
     * Default constructor.
     */
    public HiddenField() {
        super();
    }

    /**
     * Creates a new <code>HiddenField</code> instance with given name and empty value.
     * @param fieldName a <code>String</code> value
     */
    public HiddenField(String fieldName) {
        super(fieldName, "");
    }

    /**
     * Creates a new <code>HiddenField</code> instance with given name and value.
     * @param fieldName a <code>String</code> value
     * @param value a <code>String</code> value
     */
    public HiddenField(String fieldName, String value) {
        super(fieldName, value);
    }

    /**
     * Creates a new <code>HiddenField</code> instance from an existing <code>HiddenField</code>.
     * @param hiddenField the <code>HiddenField</code> to copy from
     */
    public HiddenField(HiddenField hiddenField) {
        super(hiddenField.getName(), hiddenField.getRawValue());
    }

}
