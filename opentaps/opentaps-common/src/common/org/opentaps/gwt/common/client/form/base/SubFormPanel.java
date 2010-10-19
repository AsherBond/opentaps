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

package org.opentaps.gwt.common.client.form.base;

import com.google.gwt.user.client.ui.Widget;

import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.form.Field;
import com.gwtext.client.widgets.form.TextField;
import com.gwtext.client.widgets.form.event.FieldListenerAdapter;
import com.gwtext.client.widgets.layout.FormLayout;

import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.form.field.FieldInterface;

/**
 * Provides utility methods to build a tabbed <code>FormPanel</code>.
 */
public class SubFormPanel extends Panel {

    private final FieldListenerAdapter submitOnEnterKey;

    /**
     * Default Constructor.
     * @param base the parent <code>BaseFormPanel</code>
     */
    public SubFormPanel(BaseFormPanel base) {
        super();
        setLayout(new FormLayout());
        submitOnEnterKey = base.getSubmitOnEnterHandler();
    }

    /**
     * Adds a field to the form.
     * This also automatically sets the submit on enter event handler.
     * @param field a <code>Field</code> value
     */
    public void addField(Field field) {
        field.addListener(submitOnEnterKey);
        add(field);
    }

    /**
     * Adds a field to the form.
     * This also automatically sets the submit on enter event handler.
     * @param field a <code>FieldInterface</code> value
     */
    public void addField(FieldInterface field) {
        field.addListener(submitOnEnterKey);
        add((Widget) field);
    }

    /**
     * Adds a required field to the form.
     * This also automatically sets the submit on enter event handler and sets the proper CSS class to the label.
     * @param field a <code>Field</code> value
     */
    public void addRequiredField(Field field) {
        field.addListener(submitOnEnterKey);
        add(field, UtilUi.REQUIRED_FIELD_DATA);
    }

    /**
     * Adds a required field to the form.
     * This also automatically sets the submit on enter event handler.
     * @param field a <code>FieldInterface</code> value
     */
    public void addRequiredField(FieldInterface field) {
        field.addListener(submitOnEnterKey);
        add((Widget) field, UtilUi.REQUIRED_FIELD_DATA);
    }

    /**
     * Adds a required field to the form.
     * This also automatically sets the submit on enter event handler and sets the proper CSS class to the label.
     * @param field a <code>TextField</code> value
     */
    public void addRequiredField(TextField field) {
        field.setAllowBlank(false);
        field.addListener(submitOnEnterKey);
        add(field, UtilUi.REQUIRED_FIELD_DATA);
    }
}
