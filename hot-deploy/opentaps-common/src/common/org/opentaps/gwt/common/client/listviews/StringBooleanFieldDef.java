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

package org.opentaps.gwt.common.client.listviews;

import com.gwtext.client.data.BooleanFieldDef;
import com.gwtext.client.data.Converter;

/**
 * A <code>FieldDef</code> that should be used to read Y/N strings into <code>boolean</code> values.
 */
public class StringBooleanFieldDef extends BooleanFieldDef {

    /**
     * Constructor.
     * @param name the field name
     */
    public StringBooleanFieldDef(String name) {
        super(name, null, new BooleanConverter());
    }

    private static class BooleanConverter implements Converter {
        public String format(String raw) {
            if ("Y".equalsIgnoreCase(raw)) {
                return "true";
            } else {
                return "false";
            }

        }
    }

}
