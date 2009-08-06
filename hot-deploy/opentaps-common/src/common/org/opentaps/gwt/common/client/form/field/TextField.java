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
 * A Text input.
 */
public class TextField extends com.gwtext.client.widgets.form.TextField {

    private boolean allowBlank;

    /**
     * Default constructor.
     */
    public TextField() {
        super();
        setDefaultAttributes();
    }

    /**
     * Creates a new <code>TextField</code> instance with given label and name.
     * @param fieldLabel a <code>String</code> value
     * @param fieldName a <code>String</code> value
     */
    public TextField(String fieldLabel, String fieldName) {
        super(fieldLabel, fieldName);
        setDefaultAttributes();
    }

    /**
     * Creates a new <code>TextField</code> instance with given label, name and width.
     * @param fieldLabel a <code>String</code> value
     * @param fieldName a <code>String</code> value
     * @param fieldWidth an <code>int</code> value
     */
    public TextField(String fieldLabel, String fieldName, int fieldWidth) {
        super(fieldLabel, fieldName, fieldWidth);
        setDefaultAttributes();
    }

    /**
     * Creates a new <code>TextField</code> instance from an existing <code>TextField</code>.
     * @param textField the <code>TextField</code> to copy from
     */
    public TextField(TextField textField) {
        this(textField.getFieldLabel(), textField.getName(), textField.getWidth());
        copyAttributes(textField);
    }

    /**
     * Sets the field allow blank properties, which allow blank values to be valid.
     * Defaults to <code>true</code>.
     * @param allow the new allow blank properties value
     */
    @Override public void setAllowBlank(boolean allow) {
        this.allowBlank = allow;
        super.setAllowBlank(allow);
    }

    /**
     * Gets this field allow blank properties.
     * Defaults to <code>true</code>.
     * @return the allow blank properties value
     */
    public boolean getAllowBlank() {
        return allowBlank;
    }

    private void setDefaultAttributes() {
        setAllowBlank(true);
    }

    private void copyAttributes(TextField textField) {
        setAllowBlank(textField.getAllowBlank());
    }
}
