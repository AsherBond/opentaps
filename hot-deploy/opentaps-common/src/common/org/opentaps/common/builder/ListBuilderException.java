package org.opentaps.common.builder;

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


