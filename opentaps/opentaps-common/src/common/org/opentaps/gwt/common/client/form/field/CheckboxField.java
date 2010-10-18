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
 * A Checkbox input.
 */
public class CheckboxField extends com.gwtext.client.widgets.form.Checkbox {

    /**
     * Default constructor.
     */
    public CheckboxField() {
        super();
    }

    /**
     * Creates a new <code>CheckboxField</code> instance with given label and name.
     * @param fieldLabel a <code>String</code> value
     * @param fieldName a <code>String</code> value
     */
    public CheckboxField(String fieldLabel, String fieldName) {
        super(fieldLabel, fieldName);
    }

    /**
     * Creates a new <code>CheckboxField</code> instance from an existing <code>CheckboxField</code>.
     * @param checkboxField the <code>CheckboxField</code> to copy from
     */
    public CheckboxField(CheckboxField checkboxField) {
        super(checkboxField.getFieldLabel(), checkboxField.getName());
    }

}
