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

package org.opentaps.domain.billing.lockbox;

import org.opentaps.foundation.exception.FoundationException;


/**
 * Exception related to the parsing of Lockbox files.
 */
@SuppressWarnings("serial")
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
