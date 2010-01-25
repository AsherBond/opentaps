/*
 * Copyright (c) 2010 - 2010 Open Source Strategies, Inc.
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

import java.math.BigDecimal;

import com.gwtext.client.widgets.form.Field;
import com.gwtext.client.widgets.form.event.FieldListenerAdapter;
import org.opentaps.gwt.common.client.UtilUi;

/**
 * A special Number input which can be used to hold integers and decimals values,
 *  and that accepts modifier such as +/-XXX%, or --/++XXX inputs that applies
 *  to the previous value.
 * For example: if the current value is 1000 and the user enters -10% the value
 *  is then set to 900.
 */
public class ModifierOrNumberField extends TextField implements ValuePostProcessedInterface {

    private static final String MODULE = ModifierOrNumberField.class.getName();
    private static final String VAL_REGEX = "^(\\+\\+|--|-|\\+)?[0-9]+(\\.?[0-9]*|%)?$";
    private static final String INPUT_REGEX = "[0-9%.+-]+";

    /**
     * Default constructor.
     */
    public ModifierOrNumberField() {
        super();
        init();
    }

    /**
     * Creates a new <code>NumberField</code> instance with given label and name.
     * @param fieldLabel a <code>String</code> value
     * @param fieldName a <code>String</code> value
     */
    public ModifierOrNumberField(String fieldLabel, String fieldName) {
        super(fieldLabel, fieldName);
        init();
    }

    /**
     * Creates a new <code>NumberField</code> instance with given label, name and width.
     * @param fieldLabel a <code>String</code> value
     * @param fieldName a <code>String</code> value
     * @param fieldWidth an <code>int</code> value
     */
    public ModifierOrNumberField(String fieldLabel, String fieldName, int fieldWidth) {
        super(fieldLabel, fieldName, fieldWidth);
        init();
    }

    /**
     * Creates a new <code>NumberField</code> instance from an existing <code>NumberField</code>.
     * @param numberField the <code>NumberField</code> to copy from
     */
    public ModifierOrNumberField(ModifierOrNumberField numberField) {
        super(numberField.getFieldLabel(), numberField.getName(), numberField.getWidth());
        init();
    }

    private void init() {
        setRegex(VAL_REGEX);
        setMaskRe(INPUT_REGEX);
        setSelectOnFocus(true);
        addListener(new FieldListenerAdapter() {
                @Override public void onChange(Field field, Object newVal, Object oldVal) {
                    UtilUi.logDebug("ModifierOrNumberField: changed event from [" + oldVal  + "] to [" + newVal + "]", MODULE, "onChange");
                    String calcNewValue = getPostProcessedValue(oldVal, newVal);
                    setRawValue(calcNewValue);
                    setValue(calcNewValue);
                }
            });
    }

    /**
     * Calculate the new value based on the previous input and the new input.
     *
     * @param oldValue a <code>String</code> value
     * @param newValue a <code>String</code> value
     * @return a <code>String</code> value
     */
    public static String calculateNewStringValue(String oldValue, String newValue) {
        UtilUi.logDebug("ModifierOrNumberField: calculating the real new value, changed from [" + oldValue  + "] to [" + newValue + "]", MODULE, "calculateNewStringValue");

        // nothing to do if both values are equal
        if (newValue == null && oldValue == null) {
            return oldValue;
        } else if (newValue != null && newValue.equals(oldValue)) {
            return oldValue;
        }

        // read the previous value, it should have been a valid number
        // or null (empty)
        BigDecimal oldNumber = BigDecimal.ZERO;
        if (!UtilUi.isEmpty(oldValue)) {
            oldValue = oldValue.trim();
            UtilUi.logDebug("Parsing old value [" + oldValue + "]", MODULE, "calculateNewStringValue");
            try {
                oldNumber = new BigDecimal(oldValue);
            } catch (NumberFormatException e) {
                oldNumber = BigDecimal.ZERO;
            }
        }
        UtilUi.logDebug("Parsed old value [" + oldValue  + "] as [" + oldNumber + "]", MODULE, "calculateNewStringValue");

        // read the new value, which may be a number or a modifier
        BigDecimal newNumber = null;
        if (!UtilUi.isEmpty(newValue)) {
            newValue = newValue.trim();
            UtilUi.logDebug("Parsing new value [" + newValue + "]", MODULE, "calculateNewStringValue");
            // check if it is an absolute modifier amount: ++444 --444
            if (newValue.startsWith("++") || newValue.startsWith("--")) {
                // validate the rest as number
                newNumber = UtilUi.asBigDecimal(newValue.substring(2));
                if (newNumber != null) {
                    if (newValue.startsWith("--")) {
                        newNumber = newNumber.negate();
                    }
                    newNumber = oldNumber.add(newNumber);
                    UtilUi.logDebug("Calculated new value [" + newNumber + "]", MODULE, "calculateNewStringValue");
                }
            } else if (newValue.endsWith("%")) {
                // this is a percent (relative) modifier amount: +12% -12% 12%
                newValue = newValue.substring(0, newValue.length() - 1);
                // strip the sign if present
                if (newValue.startsWith("+") || newValue.startsWith("-")) {
                    // to be user friendly also support ++ / -- prefix here
                    if (newValue.startsWith("++") || newValue.startsWith("--")) {
                        newNumber = UtilUi.asBigDecimal(newValue.substring(2));
                    } else {
                        newNumber = UtilUi.asBigDecimal(newValue.substring(1));
                    }
                } else {
                    newNumber = UtilUi.asBigDecimal(newValue);
                }
                if (newNumber != null) {
                    if (newValue.startsWith("-")) {
                        newNumber = newNumber.negate();
                    }
                    // keep the decimals, only round after the percentage is applied to the current price
                    BigDecimal modifier = newNumber.divide(new BigDecimal("100"), 100, BigDecimal.ROUND_HALF_EVEN);
                    // round half even, and to 2 decimals (for a valid price)
                    modifier = oldNumber.multiply(modifier).setScale(2, BigDecimal.ROUND_HALF_EVEN);
                    newNumber = oldNumber.add(modifier);
                    UtilUi.logDebug("Calculated new value [" + newNumber + "]", MODULE, "calculateNewStringValue");
                }
            } else {
                // try to parse it as a normal number
                newNumber = UtilUi.asBigDecimal(newValue);
                if (newNumber != null) {
                    // this is a valid number, nothing to do here
                    return newNumber.toString();
                } else {
                    // force it to ZERO
                    newNumber = BigDecimal.ZERO;
                }
            }
        }

        if (newNumber != null) {
            UtilUi.logDebug("Setting new value [" + newNumber.toString() + "]", MODULE, "calculateNewStringValue");
            return newNumber.toString();
        } else {
            UtilUi.logError("Could not calculate a valid new value, setting the old value instead [" + oldValue + "]", MODULE, "calculateNewStringValue");
            return null;
        }
    }

    /** {@inheritDoc} */
    public String getPostProcessedValue(Object oldValue, Object newValue) {
        return calculateNewStringValue((String) oldValue, (String) newValue);
    }
}
