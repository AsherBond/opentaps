/*
 * Copyright (c) 2009 Open Source Strategies, Inc.
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
package org.opentaps.gwt.common.client.config;

import com.google.gwt.i18n.client.Dictionary;

/**
 * Wrapper class around a specific GWT Dictionary
 * named "OpentapsConfig" which is defined in header.ftl.
 *
 * This class provides access to application wide global
 * constants defined in opentaps.properties.
 *
 * @see <a href="http://google-web-toolkit.googlecode.com/svn/javadoc/1.4/com/google/gwt/i18n/client/Dictionary.html">GWT Dictionary</a>
 */
public class OpentapsConfig {

    private static final String DEFAULT_COUNTRY_CODE_KEY = "defaultCountryCode";
    private static final String DEFAULT_COUNTRY_GEO_KEY = "defaultCountryGeoId";
    private static final String DEFAULT_CURRENCY_KEY = "defaultCurrencyUomId";
    private static final String ICON_CALLIN_EVENT = "callInEventIcon";
    private static final String APPLICATION_NAME_KEY = "applicationName";

    private static Dictionary dictionary = Dictionary.getDictionary("OpentapsConfig");

    /**
     * Creates a new <code>OpentapsConfig</code> instance.
     */
    public OpentapsConfig() {
    }

    /**
     * Gets the default country code for phone numbers from opentaps.properties.
     * @return a <code>String</code> representation of the country code or null if not configured
     */
    public String getDefaultCountryCode() {
        return dictionary.get(DEFAULT_COUNTRY_CODE_KEY);
    }

    /**
     * Gets the default country geo ID for country selects.
     * @return a <code>String</code> of the country geoId
     */
    public String getDefaultCountryGeoId() {
        return dictionary.get(DEFAULT_COUNTRY_GEO_KEY);
    }

    /**
     * Gets the default currency uom ID.
     * @return a <code>String</code> of the currency uomId
     */
    public String getDefaultCurrencyUomId() {
        return dictionary.get(DEFAULT_CURRENCY_KEY);
    }
    
    /**
     * Gets the icon for notification call in, defined in asterisk.properties.
     * @return a <code>String</code> representation of the country code or null if not configured
     */
    public String getCallInEventIcon() {
        return dictionary.get(ICON_CALLIN_EVENT);
    }

    /**
     * Get the name of the current application (e.g., "crmsfa").
     * @return a <code>String</code> of the application name
     */
    public String getApplicationName() {
        return dictionary.get(APPLICATION_NAME_KEY);
    }

}
