/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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
