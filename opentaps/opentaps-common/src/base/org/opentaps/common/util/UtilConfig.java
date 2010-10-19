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
package org.opentaps.common.util;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;

/**
 * Configuration utilities for opentaps applications.
 *
 * TODO: probably want to synch some of these and see if we can put the caches in ofbiz cache system instead
 */
public class UtilConfig {

    public static final String module = UtilConfig.class.getName();

    private static Map configCache = null;
    private static Map javascriptFileCache = null;
    private static Map stylesheetFileCache = null;
    private static Map addressFunctionCache = null;

    private static String addressFormatKey = "opentaps.formatter.address.";

    // constants to work w/ organization stored in user's preferences
    public static final String SET_ORGANIZATION_FORM = "selectOrganizationForm";
    public static final String SET_FACILITY_FORM = "selectFacilityForm";
    public static final String OPTION_DEF_ORGANIZATION = "organizationPartyId";
    public static final String OPTION_DEF_FACILITY = "facilityId";

    /**
     * This constant used as application name in case user preferences value may be used
     * across all components
     */
    public static final String SYSTEM_WIDE = "opentaps";

    static {
        configCache = FastMap.newInstance();
        Properties opentaps = UtilProperties.getProperties("opentaps.properties");
        configCache.put("opentaps", opentaps);
    }

    /**
     * Gets the configuration properties for an application.  The configuration
     * properties are a union of the properties in opentaps.properties and
     * in ${opentapsApplicationName}.properties.  The intent is to provide
     * a simple, inheritable property system.
     */
    public static Map getConfigProperties(String opentapsApplicationName) {
        Properties configProperties = (Properties) configCache.get(opentapsApplicationName);
        synchronized (configCache) {
            if (configProperties == null) {
                configProperties = new Properties();
                Properties tProps = UtilProperties.getProperties("opentaps.properties");
                // if exist opentaps.properties, then put them into config properties
                if (tProps != null) {
                    configProperties.putAll(tProps);
                }
                tProps = UtilProperties.getProperties(opentapsApplicationName + ".properties");
                 // if exist application's properties, then put them into config properties
                if (tProps != null) {
                    configProperties.putAll(tProps);
                }
                configCache.put(opentapsApplicationName, configProperties);
            }
        }
        return configProperties;
    }

    private static List<String> getCachedFiles(String opentapsApplicationName, Map cache, String property) {
        if (cache == null) {
            cache = FastMap.newInstance();
            String[] values = UtilProperties.getPropertyValue("opentaps.properties", property).split("\\s*,\\s*");
            List<String> files = FastList.<String>newInstance();
            files.addAll(Arrays.asList(values));
            files.remove("");
            cache.put("opentaps", files);
        }

        List<String> files = (List<String>) cache.get(opentapsApplicationName);
        if (files == null) {
            files = FastList.newInstance();
            String[] values = UtilProperties.getPropertyValue(opentapsApplicationName + ".properties", property).split("\\s*,\\s*");
            files.addAll((List<String>) cache.get("opentaps")); // Add opentaps ones first, so they get loaded first
            files.addAll(Arrays.asList(values));
            files.remove("");
            cache.put(opentapsApplicationName, files);
        }
        return files;
    }

    /** Gets a list of javascript files for the application. */
    public static List<String> getJavascriptFiles(String opentapsApplicationName, Locale locale) {
        List<String> javascriptFiles = getCachedFiles(opentapsApplicationName, javascriptFileCache, "opentaps.files.javascript");

        /*
         * Localization scripts for jscalendar are handled separately under some rules:
         * - all three calendar scripts loads in required order
         * - what script from lang/calendar-??.js will be loaded depend on user's locale
         * - we are trying to find lang/calendar-??-utf8.js first, lang/calendar-??.js than.
         * - if there are no both, fall back to default lang/calendar-en.js
         */
        String jscalendarFiles = UtilProperties.getPropertyValue("opentaps.properties", "opentaps.files.javascript.jscalendar");
        if (UtilValidate.isNotEmpty(jscalendarFiles)) {
            String language = locale != null ? locale.getLanguage() : "en";
            String[] files = FlexibleStringExpander.expandString(jscalendarFiles, UtilMisc.toMap("language", language)).split("\\s*,\\s*");
            for (String js : files) {
                if (js.indexOf("-" + language) != -1) {

                    // check lang/calendar-??-utf8.js
                    String fileUtf8 = js.replace("/opentaps_js", "opentaps/opentaps-common/webapp/js");
                    File ch = new File(fileUtf8);
                    if (ch.exists()) {
                        javascriptFiles.add(js);
                        continue;
                    }

                    // check lang/calendar-??.js
                    String fileOem = fileUtf8.replace("-utf8", "");
                    ch = new File(fileOem);
                    if (ch.exists()) {
                        js = js.replace("-utf8", "");
                        javascriptFiles.add(js);
                        continue;
                    }

                    // load defaults lang/calendar-en.js
                    js = js.replace(language + "-utf8", "en");
                    javascriptFiles.add(js);
                    continue;
                }

                javascriptFiles.add(js);
            }
        }

        return javascriptFiles;
    }

    /** Gets a list of css files for the application. */
    public static List<String> getStylesheetFiles(String opentapsApplicationName) {
        return getCachedFiles(opentapsApplicationName, stylesheetFileCache, "opentaps.files.stylesheets");
    }

    public static String getSectionBgColor(String opentapsApplicationName, String sectionName) {
        String color = UtilProperties.getPropertyValue(opentapsApplicationName + ".properties", opentapsApplicationName + ".theme.color.background." + sectionName);
        if (UtilValidate.isNotEmpty(color)) {
            return color;
        }
        return "#000099";
    }

    public static String getSectionFgColor(String opentapsApplicationName, String sectionName) {
        String color = UtilProperties.getPropertyValue(opentapsApplicationName + ".properties", opentapsApplicationName + ".theme.color.foreground." + sectionName);
        if (UtilValidate.isNotEmpty(color)) {
            return color;
        }
        return "white";
    }

    /**
     * Returns the name of the FTL function that will render a country's postal address.
     * If the format is not defined, a default format will be used.  It reads the
     * opentaps.formatter.address.${geoId} property to determine the name of the function.
     * These properties must be defined in opentaps.properties.
     */
    public static String getAddressFormattingFunction(String countryGeoId) {
        if (addressFunctionCache == null) {
            addressFunctionCache = FastMap.newInstance();
            Map properties = getConfigProperties("opentaps");
            for (Iterator<String> iter = properties.keySet().iterator(); iter.hasNext();) {
                String key = iter.next();
                if (!key.startsWith(addressFormatKey)) {
                    continue;
                }

                String geoId = key.substring(addressFormatKey.length(), key.length());
                String functionName = (String) properties.get(key);
                addressFunctionCache.put(geoId, functionName);
            }
        }
        String functionName = (String) addressFunctionCache.get(countryGeoId);
        return (functionName == null ? "displayAddressDefault" : functionName);
    }

    /**
     * Gets a property value as a trimmed string from the application's property file.
     * If it fails to find one, then it will try to get it from opentaps.properties.
     * Returns null if the string was empty.
     */
    public static String getPropertyValue(String opentapsApplicationName, String property) {
        String value = UtilProperties.getPropertyValue(opentapsApplicationName + ".properties", property);
        if (UtilValidate.isEmpty(value)) {
            value = UtilProperties.getPropertyValue("opentaps.properties", property);
        }
        return (UtilValidate.isEmpty(value) ? null : value.trim());
    }

    /**
     * Gets a property value as an int from the application's property file.
     * If it fails to find one, then it will try to get it from opentaps.properties.
     */
    public static int getPropertyInt(String opentapsApplicationName, String property, int defaultValue) {
        String intValue = getPropertyValue(opentapsApplicationName, property);
        if (intValue != null) {
            try {
                return Integer.parseInt(intValue);
            } catch (NumberFormatException e) { }
        }
        return defaultValue;
    }

    /**
     * Gets a property value as a boolean from the application's property file.
     * If it fails to find one, then it will try to get it from opentaps.properties.
     * True values begin with y, Y, t or T.  False values begin with n, N, f or F.
     */
    public static boolean getPropertyBoolean(String opentapsApplicationName, String property, boolean defaultValue) {
        String boolValue = getPropertyValue(opentapsApplicationName, property);
        if (boolValue != null) {
            boolValue = boolValue.toLowerCase();
            if (boolValue.startsWith("y") || boolValue.startsWith("t")) {
                return true;
            }
            if (boolValue.startsWith("n") || boolValue.startsWith("f")) {
                return false;
            }
        }
        return defaultValue;
    }

    /**
     * Retrieves a Map of properties from the application property file and from opentaps.properties.
     * It looks for property keys that have the same prefix.  The suffix of the key will be the map key.
     * For example, if you have defined the following properties in crmsfa.properties,
     *
     * fruit.apple = This is an Applie
     * fruit.pear = This is a Pear
     *
     * And also the following in opentaps.properties,
     *
     * fruit.orange = An Orange
     * fruit.banana = Banana
     *
     * Then getPropertyMap("crmsfa", "fruit") will return the following map,
     *
     * { {"apple", "This is an Apple"}, {"pear", "This is a Pear"}, {"orange", "An Orange"}, {"banana", "Banana"} }
     */
    public static Map getPropertyMap(String opentapsApplicationName, String prefix) {
        Map map = FastMap.newInstance();
        String prefixWithPeriod = prefix + ".";
        int size = prefixWithPeriod.length();
        Map properties = getConfigProperties(opentapsApplicationName);
        for (Iterator<String> iter = properties.keySet().iterator(); iter.hasNext();) {
            String propertyKey = iter.next();

            int index = propertyKey.indexOf(prefixWithPeriod);
            if (index == -1 || propertyKey.length() == size) {
                continue;
            }

            String key = propertyKey.substring(propertyKey.lastIndexOf('.') + 1, propertyKey.length()).trim();
            if (UtilValidate.isEmpty(key)) {
                continue;
            }

            String value = (String) properties.get(propertyKey);
            if (value != null) {
                value = value.trim();
            }
            if (UtilValidate.isNotEmpty(value)) {
                map.put(key, properties.get(propertyKey));
            }
        }
        return map;
    }

    /**
     * Setup organization in session if some organization stored in user preferences.
     */
    public static void checkDefaultOrganization(HttpServletRequest request) {

        HttpSession session = request.getSession();
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");

        if (userLogin == null) {
            return;
        }

        String organizationPartyId = null;

        try {

            organizationPartyId = UtilCommon.getUserLoginViewPreference(request, "opentaps", SET_ORGANIZATION_FORM, OPTION_DEF_ORGANIZATION);
            if (UtilValidate.isEmpty(organizationPartyId)) {
                return;
            }

            GenericValue organization = delegator.findByPrimaryKeyCache("PartyGroup", UtilMisc.toMap("partyId", organizationPartyId));
            if (organization == null) {
                return;
            }

            session.setAttribute("organizationParty", organization);
            session.setAttribute("organizationPartyId", organizationPartyId);
            session.setAttribute("applicationContextSet", Boolean.TRUE);

        } catch (GenericEntityException e) {
            Debug.logError(e, "Error while retrieve default organization", module);
            return;
        }
    }
}
