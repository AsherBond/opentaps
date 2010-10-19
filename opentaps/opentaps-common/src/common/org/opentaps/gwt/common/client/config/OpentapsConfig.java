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
    private static final String UI_NAV_SHOW_TOP_MENU = "showTopNavMenu";
    private static final String APPLICATION_NAME_KEY = "applicationName";

    private static Dictionary dictionary = Dictionary.getDictionary("OpentapsConfig");

    /**
     * Creates a new <code>OpentapsConfig</code> instance.
     */
    public OpentapsConfig() {
    }

    protected String getConfigValue(String key) {
        try {
            return dictionary.get(key);
        } catch (java.util.MissingResourceException e) {
            return null;
        }
    }

    /**
     * Gets the default country code for phone numbers from opentaps.properties.
     * @return a <code>String</code> representation of the country code or null if not configured
     */
    public String getDefaultCountryCode() {
        return getConfigValue(DEFAULT_COUNTRY_CODE_KEY);
    }

    /**
     * Gets the default country geo ID for country selects.
     * @return a <code>String</code> of the country geoId or null if not configured
     */
    public String getDefaultCountryGeoId() {
        return getConfigValue(DEFAULT_COUNTRY_GEO_KEY);
    }

    /**
     * Gets the default currency uom ID.
     * @return a <code>String</code> of the currency uomId or null if not configured
     */
    public String getDefaultCurrencyUomId() {
        return getConfigValue(DEFAULT_CURRENCY_KEY);
    }

    /**
     * Gets the icon for notification call in, defined in asterisk.properties.
     * @return a <code>String</code> representation of the country code or null if not configured
     */
    public String getCallInEventIcon() {
        return getConfigValue(ICON_CALLIN_EVENT);
    }

    /**
     * Gets if the top navigation menu should be displayed.
     * @return a <code>Boolean</code>, default to <code>False</code> if not configured
     */
    public Boolean getShowTopNavMenu() {
        return "Y".equals(getConfigValue(UI_NAV_SHOW_TOP_MENU));
    }

    /**
     * Get the name of the current application (e.g., "crmsfa").
     * @return a <code>String</code> of the application name or null if not configured
     */
    public String getApplicationName() {
        return getConfigValue(APPLICATION_NAME_KEY);
    }

}
