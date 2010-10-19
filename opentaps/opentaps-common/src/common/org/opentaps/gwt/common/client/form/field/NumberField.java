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
 * A Number input which can be used to hold integers and decimals values.
 */
public class NumberField extends com.gwtext.client.widgets.form.NumberField {

    private boolean allowBlank;
    private boolean allowDecimals;
    private boolean allowNegative;
    private int decimalPrecision;

    /**
     * Default constructor.
     */
    public NumberField() {
        super();
        setDefaultAttributes();
    }

    /**
     * Creates a new <code>NumberField</code> instance with given label and name.
     * @param fieldLabel a <code>String</code> value
     * @param fieldName a <code>String</code> value
     */
    public NumberField(String fieldLabel, String fieldName) {
        super(fieldLabel, fieldName);
        setDefaultAttributes();
    }

    /**
     * Creates a new <code>NumberField</code> instance with given label, name and width.
     * @param fieldLabel a <code>String</code> value
     * @param fieldName a <code>String</code> value
     * @param fieldWidth an <code>int</code> value
     */
    public NumberField(String fieldLabel, String fieldName, int fieldWidth) {
        super(fieldLabel, fieldName, fieldWidth);
        setDefaultAttributes();
    }

    /**
     * Creates a new <code>NumberField</code> instance from an existing <code>NumberField</code>.
     * @param numberField the <code>NumberField</code> to copy from
     */
    public NumberField(NumberField numberField) {
        super(numberField.getFieldLabel(), numberField.getName(), numberField.getWidth());
        copyAttributes(numberField);
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

    /**
     * Sets the field allow negative properties, which allow negative values to be valid.
     * Defaults to <code>true</code>.
     * @param allow the new allow negative properties value
     */
    @Override public void setAllowNegative(boolean allow) {
        this.allowNegative = allow;
        super.setAllowNegative(allow);
    }

    /**
     * Gets this field allow negative properties.
     * Defaults to <code>true</code>.
     * @return the allow negative properties value
     */
    public boolean getAllowNegative() {
        return allowNegative;
    }

    /**
     * Sets the field allow decimals properties, which allow decimals values to be valid.
     * Defaults to <code>true</code>.
     * @param allow the new allow decimals properties value
     */
    @Override public void setAllowDecimals(boolean allow) {
        this.allowDecimals = allow;
        super.setAllowDecimals(allow);
    }

    /**
     * Gets this field decimal precision properties.
     * Defaults to <code>2</code>.
     * @return the max number of decimals
     */
    public int getDecimalPrecision() {
        return decimalPrecision;
    }

    /**
     * Sets the field decimals precision properties.
     * Defaults to <code>2</code>.
     * @param precision the new decimals precision properties value
     */
    @Override public void setDecimalPrecision(int precision) {
        this.decimalPrecision = precision;
        super.setDecimalPrecision(precision);
    }


    /**
     * Gets this field allow decimals properties.
     * Defaults to <code>true</code>.
     * @return the allow decimals properties value
     */
    public boolean getAllowDecimals() {
        return allowDecimals;
    }

    private void setDefaultAttributes() {
        setAllowBlank(true);
        setAllowDecimals(true);
        setAllowNegative(true);
        setDecimalPrecision(2);
    }

    private void copyAttributes(NumberField numberField) {
        setAllowBlank(numberField.getAllowBlank());
        setAllowDecimals(numberField.getAllowDecimals());
        setAllowNegative(numberField.getAllowNegative());
        setDecimalPrecision(numberField.getDecimalPrecision());
    }
}
