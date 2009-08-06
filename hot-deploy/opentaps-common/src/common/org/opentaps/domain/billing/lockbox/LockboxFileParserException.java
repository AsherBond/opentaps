/*
 * Copyright (c) 2007 - 2009 Open Source Strategies, Inc.
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

package org.opentaps.domain.billing.lockbox;

import org.opentaps.foundation.exception.FoundationException;


/**
 * Exception related to the parsing of Lockbox files.
 */
public class LockboxFileParserException extends FoundationException {

    /**
     * Default constructor.
     */
    public LockboxFileParserException() { }

    /**
     * Constructor with message.
     * @param msg a <code>String</code> value
     */
    public LockboxFileParserException(String msg) {
        super(msg);
    }

    /**
     * Constructor with exception.
     * @param e an <code>Exception</code> value
     */
    public LockboxFileParserException(Exception e) {
        super(e);
    }
}
