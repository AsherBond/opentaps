/*
 * Copyright (c) 2009 - 2009 Open Source Strategies, Inc.
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

package org.opentaps.gwt.common.server.form;

import org.ofbiz.service.ServiceValidationException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * An extension of <code>ServiceValidationException</code> that also support custom field error messages.
 */
public class CustomServiceValidationException extends ServiceValidationException {

    private Map<String, String> customFieldsErrors = new HashMap<String, String>();

    /**
     * Creates a new <code>CustomServiceValidationException</code> instance.
     * @param missingFields the list of missing fields
     * @param extraFields the list of extra fields
     * @param customFieldsErrors the <code>Map</code> of fields and their custom error messages
     */
    public CustomServiceValidationException(List<String> missingFields, List<String> extraFields, Map<String, String> customFieldsErrors) {
        super("Validation Error", null, missingFields, extraFields, "IN");

        if (customFieldsErrors != null) {
            this.customFieldsErrors = customFieldsErrors;
        }
    }

    /**
     * Gets the <code>Map</code> of fields and their custom error messages.
     * @return the <code>Map</code> of fields and their custom error messages
     */
    public Map<String, String> getCustomFieldsErrors() {
        return customFieldsErrors;
    }
}
