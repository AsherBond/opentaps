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
 * A Date input class.
 */
public class DateField extends com.gwtext.client.widgets.form.DateField {

    private boolean allowBlank;

    /**
     * Default constructor.
     */
    public DateField() {
        super();
        setDefaultAttributes();
    }

    /**
     * Creates a new <code>DateField</code> instance with given label and name.
     * @param fieldLabel a <code>String</code> value
     * @param fieldName a <code>String</code> value
     */
    public DateField(String fieldLabel, String fieldName) {
        super(fieldLabel, fieldName);
        setDefaultAttributes();
    }

    /**
     * Creates a new <code>NumberField</code> instance with given label, name and width.
     * @param fieldLabel a <code>String</code> value
     * @param fieldName a <code>String</code> value
     * @param fieldWidth an <code>int</code> value
     */
    public DateField(String fieldLabel, String fieldName, int fieldWidth) {
        super(fieldLabel, fieldName, fieldWidth);
        setDefaultAttributes();
    }

    /**
     * Creates a new <code>DateField</code> instance from an existing <code>DateField</code>.
     * @param dateField the <code>DateField</code> to copy from
     */
    public DateField(DateField dateField) {
        super(dateField.getFieldLabel(), dateField.getName(), dateField.getWidth());
        copyAttributes(dateField);
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

    private void copyAttributes(DateField dateField) {
        setAllowBlank(dateField.getAllowBlank());
    }
}
