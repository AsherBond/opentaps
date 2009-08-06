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
package org.opentaps.domain.ledger;

import org.opentaps.foundation.exception.FoundationException;

import java.util.Locale;
import java.util.Map;

/**
 * Thrown whern there are issues with the ledger. 
 */
public class LedgerException extends FoundationException {

    public LedgerException() {
        super();
    }

    public LedgerException(Throwable throwable) {
        super(throwable);
    }

    public LedgerException(String string) {
        super(string);
    }

    public LedgerException(String messageLabel, Locale locale) {
        super(messageLabel, locale);
    }

    public LedgerException(String messageLabel, Map messageContext) {
        super(messageLabel, messageContext);
    }

    public LedgerException(String messageLabel, Map messageContext, Locale locale) {
        super(messageLabel, messageContext, locale);
    }
}
