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

import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.ToolTip;
import com.gwtext.client.widgets.form.Label;
import com.gwtext.client.widgets.form.TextField;
import com.gwtext.client.widgets.form.event.FieldListenerAdapter;
import com.gwtext.client.widgets.layout.FormLayout;
import com.gwtext.client.widgets.layout.HorizontalLayout;
import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.config.OpentapsConfig;

/**
 * A Phone number input divided in three sub inputs for country code,
 * area code and the phone number.
 */
public class PhoneNumberField extends Panel implements FieldInterface {

    private Panel fieldPanel;
    private TextField countryCodeInput;
    private TextField areaCodeInput;
    private TextField numberInput;

    /** default field name for the country code input. */
    public static final String COUNTRY_CODE_NAME = "phoneCountryCode";
    /** default name for the area code input. */
    public static final String AREA_CODE_NAME = "phoneAreaCode";
    /** default name for the phone number input. */
    public static final String NUMBER_NAME = "phoneNumber";

    private static final int INPUT_PX_MARGIN = 2;
    private static final int CODES_INPUT_CHAR_LENGTH = 3;
    private static final int CODES_INPUT_PX_LENGTH = 28;
    private static final int PHONE_NUMBER_INPUT_CHAR_LENGTH = 20;

    /** Regex for the allowed input, if a new char makes the field content do not match it won't be allowed and cannot be input. */
    private static final String INPUT_MASK = "^[0-9 ()#+-]+$";

    /**
     * Constructor.
     * Sub inputs names are set to their default.
     * @param fieldLabel the field label
     * @param fieldWidth the field size in pixels
     */
    public PhoneNumberField(String fieldLabel, int fieldWidth) {
        this(fieldLabel, COUNTRY_CODE_NAME, AREA_CODE_NAME, NUMBER_NAME, fieldWidth);
    }

    /**
     * Constructor.
     * Sub inputs names are set to their default.
     * @param fieldLabel the field label
     * @param labelWidth the label width in pixels (the widget does not inherit this from its containing form)
     * @param fieldWidth the field size in pixels
     */
    public PhoneNumberField(String fieldLabel, int labelWidth, int fieldWidth) {
        this(fieldLabel, COUNTRY_CODE_NAME, AREA_CODE_NAME, NUMBER_NAME, labelWidth, fieldWidth);
    }

    /**
     * Constructor.
     * Allows to override the sub inputs names.
     * @param fieldLabel the field label
     * @param countryCodeName the field name used in the form
     * @param areaCodeName the field name used in the form
     * @param numberName the field name used in the form
     * @param fieldWidth the field size in pixels
     */
    public PhoneNumberField(String fieldLabel, String countryCodeName, String areaCodeName, String numberName, int fieldWidth) {
        this(fieldLabel, countryCodeName, areaCodeName, numberName, -1, fieldWidth);
    }

    /**
     * Constructor.
     * Allows to override the sub inputs names.
     * @param fieldLabel the field label
     * @param countryCodeName the field name used in the form
     * @param areaCodeName the field name used in the form
     * @param numberName the field name used in the form
     * @param labelWidth the label width in pixels (the widget does not inherit this from its containing form)
     * @param fieldWidth the field size in pixels
     */
    public PhoneNumberField(String fieldLabel, String countryCodeName, String areaCodeName, String numberName, int labelWidth, int fieldWidth) {
        super();

        FormLayout layout = new FormLayout();
        // did does not seem to do anything but we leave it here as it should be the proper way to set the label width
        // see bellow
        if (labelWidth > 0) {
            layout.setLabelWidth(labelWidth);
        }
        setLayout(layout);

        setBorder(false);
        setCls("x-form-item");

        fieldPanel = new Panel();
        fieldPanel.setBorder(false);
        fieldPanel.setLayout(new HorizontalLayout(INPUT_PX_MARGIN));
        fieldPanel.setCls("x-form-element");

        Label lbl = new Label(fieldLabel + ":");
        lbl.setCls("x-form-item-label");
        if (labelWidth > 0) {
            // we have to correct the field and label position manually here
            // TODO: this may need adjustment in some situations as it is kind of hackish
            // 1. set the Label width so that it aligns with other labels (same right offset)
            lbl.setWidth(labelWidth + INPUT_PX_MARGIN + 1);
            // 2. adjust the input position so that it aligns with other inputs (same right offset)
            fieldPanel.setMargins(0, 2, 0, 0);
        }

        countryCodeInput = createInput(fieldLabel, countryCodeName, UtilUi.MSG.partyCountryCode(), CODES_INPUT_CHAR_LENGTH, CODES_INPUT_PX_LENGTH);

        // all phone number fields should have a default country code
        OpentapsConfig config = new OpentapsConfig();
        countryCodeInput.setValue(config.getDefaultCountryCode());

        areaCodeInput = createInput(fieldLabel, areaCodeName, UtilUi.MSG.partyAreaCode(), CODES_INPUT_CHAR_LENGTH, CODES_INPUT_PX_LENGTH);

        int numberWidth = fieldWidth - (CODES_INPUT_PX_LENGTH + INPUT_PX_MARGIN) * 2;
        numberInput = createInput(fieldLabel, numberName, UtilUi.MSG.partyPhoneNumber(), PHONE_NUMBER_INPUT_CHAR_LENGTH, numberWidth);

        fieldPanel.add(countryCodeInput);
        fieldPanel.add(areaCodeInput);
        fieldPanel.add(numberInput);

        add(lbl);
        add(fieldPanel);
    }

    private TextField createInput(String fieldLabel, String inputName, String tooltip, int charLength, int pxLength) {
        TextField input = new TextField(fieldLabel, inputName, pxLength);
        input.setMaxLength(charLength);
        input.setHideLabel(true);
        input.setMaskRe(INPUT_MASK);
        input.setFieldMsgTarget("qtip");
        ToolTip tt = new ToolTip(tooltip);
        tt.applyTo(input);
        return input;
    }

    /**
     * Assigns the same FieldListenerAdapter to the three input fields.
     * @param listener a <code>FieldListenerAdapter</code> value
     */
    public void addListener(FieldListenerAdapter listener) {
        countryCodeInput.addListener(listener);
        areaCodeInput.addListener(listener);
        numberInput.addListener(listener);
    }

    /**
     * Gets the value for the country code input.
     * @return a <code>String</code> value
     */
    public String getCountryCode() {
        return countryCodeInput.getText();
    }

    /**
     * Gets the value for the area code input.
     * @return a <code>String</code> value
     */
    public String getAreaCode() {
        return areaCodeInput.getText();
    }

    /**
     * Gets the value for the phone number input.
     * @return a <code>String</code> value
     */
    public String getNumber() {
        return numberInput.getText();
    }

}
