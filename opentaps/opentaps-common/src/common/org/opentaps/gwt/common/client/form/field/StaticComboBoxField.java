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

import com.gwtext.client.core.Template;
import com.gwtext.client.widgets.form.ComboBox;
import com.gwtext.client.data.Store;
import com.gwtext.client.data.SimpleStore;

/**
 * A simple combo box where values are known at build time.
 * It is meant to be used like a traditional Select.
 */
public class StaticComboBoxField extends ComboBox {

    private static final String OPTION_ID = "id";
    private static final String OPTION_VALUE = "value";

    /**
     * Constructor.
     * @param fieldLabel the field label
     * @param name the field name used in the form
     * @param data the pairs of id / value options in order
     * @param fieldWidth the field size in pixels
     */
    public StaticComboBoxField(String fieldLabel, String name, String[] data, int fieldWidth) {
        this(fieldLabel, name, data, fieldWidth, false);
    }

    /**
     * Constructor.
     * @param fieldLabel the field label
     * @param name the field name used in the form
     * @param data the pairs of id / value options in order
     * @param fieldWidth the field size in pixels
     * @param showBlank if set to true, inserts a blank option as the first option
     */
    public StaticComboBoxField(String fieldLabel, String name, String[] data, int fieldWidth, boolean showBlank) {
        super(fieldLabel, name, fieldWidth);

        int optionTotal = data.length / 2;
        if (showBlank) {
            optionTotal++;
        }

        int optionCounter = 0;
        String[][] values = new String[optionTotal][2];

        if (showBlank) {
            values[optionCounter][0] = "";
            values[optionCounter][1] = "";
            optionCounter++;
        }

        for (int i = 0; optionCounter < optionTotal; i += 2, optionCounter++) {
            values[optionCounter][0] = data[i];
            values[optionCounter][1] = data[i + 1];
        }

        final Store store = new SimpleStore(new String[]{OPTION_ID, OPTION_VALUE}, values);
        store.load();

        // this template allows empty values to render correctly in the drop down by adding a nbsp
        setTpl(new Template("<div class=\"x-combo-list-item\">{" + OPTION_VALUE + "}&nbsp;</div>"));
        setTypeAhead(true);
        setEditable(false);
        setForceSelection(true);
        setMode(ComboBox.LOCAL);
        setTriggerAction(ComboBox.ALL);
        setValueField(OPTION_ID);
        setDisplayField(OPTION_VALUE);
        setStore(store);
    }

}
