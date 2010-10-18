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

import org.opentaps.gwt.common.client.UtilUi;

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
        init();
        setDefaultAttributes();
    }

    /**
     * Creates a new <code>DateField</code> instance with given label and name.
     * @param fieldLabel a <code>String</code> value
     * @param fieldName a <code>String</code> value
     */
    public DateField(String fieldLabel, String fieldName) {
        super(fieldLabel, fieldName);
        init();
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
        init();
        setDefaultAttributes();
    }

    /**
     * Creates a new <code>DateField</code> instance from an existing <code>DateField</code>.
     * @param dateField the <code>DateField</code> to copy from
     */
    public DateField(DateField dateField) {
        super(dateField.getFieldLabel(), dateField.getName(), dateField.getWidth());
        init();
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
     * Gets the date as a <code>String</code> formatted according to <code>UtilUi.DATE_FORMAT</code>.
     * @return a formatted string of the date value
     */
    @Override public String getText() {
        return UtilUi.toDateString(this.getValue());
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

    private void init() {
        // error icon does not display right with the handle, using qtip instead
        setFieldMsgTarget("qtip");
    }
}
