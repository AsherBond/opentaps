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

package org.opentaps.gwt.common.client.lookup.configuration;

import java.util.Arrays;
import java.util.List;

/**
 * Defines the interface between the server and client for the TermTypeLookupService
 * Technically not a java interface, but it defines all the constants needed on both sides
 *  which makes the code more robust.
 */
public abstract class TermTypeLookupConfiguration {

    private TermTypeLookupConfiguration() { }

    public static final String URL_SUGGEST = "gwtSuggestTermType";

    public static final String OUT_TERM_TYPE_ID = "termTypeId";
    public static final String OUT_DESCRIPTION = "description";

    public static final List<String> LIST_OUT_FIELDS = Arrays.asList(
        OUT_TERM_TYPE_ID,
        OUT_DESCRIPTION
    );
}
