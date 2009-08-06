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
