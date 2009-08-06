/*
 * Copyright (c) 2008 - 2009 Open Source Strategies, Inc.
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
package org.opentaps.foundation.repository;

import org.opentaps.foundation.exception.FoundationException;

/**
 * Exception thrown by the repository classes.
 */
public class RepositoryException extends FoundationException {

    /**
     * Default constructor.
     */
    public RepositoryException() {
        super();
    }

    /**
     * Creates a new <code>RepositoryException</code> instance from an error message.
     * @param message the error message
     */
    public RepositoryException(String message) {
        super(message);
    }

    /**
     * Creates a new <code>RepositoryException</code> instance from an exception.
     * @param exception the parent exception
     */
    public RepositoryException(Throwable exception) {
        super(exception);
    }
}
