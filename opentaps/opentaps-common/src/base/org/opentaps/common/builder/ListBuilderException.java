package org.opentaps.common.builder;

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

import org.ofbiz.base.util.GeneralException;

/**
 * These exceptions will be printed on the screen in lieu of the list.
 * Only throw this if something serious has happened.
 */
public class ListBuilderException extends GeneralException {
    protected Throwable cause = null;

    public ListBuilderException() {
        super();
    }

    public ListBuilderException(Throwable e) {
        super(e);
        cause = e; // workaround for breaking of getCause() in parent class
    }

    public ListBuilderException(String message) {
        super(message);
    }

    // workarount for breaking of getCause() in parent
    public Throwable getCause() {
        return cause;
    }
}


