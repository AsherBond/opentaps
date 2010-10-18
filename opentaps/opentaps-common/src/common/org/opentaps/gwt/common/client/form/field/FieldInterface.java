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

import com.gwtext.client.widgets.form.event.FieldListenerAdapter;

/**
 * Common interface that all custom fields should implement in order to work
 *  with our utility classes and methods.
 */
public interface FieldInterface {

    /**
     * Assigns a <code>FieldListenerAdapter</code> to the input field.
     * If the field is in fact composed of multiple inputs, each input have the adapter set.
     * @param listener a <code>FieldListenerAdapter</code> value
     */
    public void addListener(FieldListenerAdapter listener);
}
