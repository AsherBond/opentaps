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

/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/

/* This file may contain code which has been modified from that included with the Apache-licensed OFBiz application */
/* This file has been modified by Open Source Strategies, Inc. */

package org.opentaps.common.template.freemarker;

import java.io.*;
import java.net.URL;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import freemarker.core.Environment;
import freemarker.core.Macro;
import freemarker.ext.beans.BeanModel;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.*;
import org.ofbiz.base.location.FlexibleLocation;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.template.FreeMarkerWorker;

/**
 * Utility methods for working with FTL templates.
 *
 * @author     <a href="mailto:cliberty@opensourcestrategies.com">Chris Liberty</a>
 * @version    $Rev$
 */
public abstract class FreemarkerUtil {

    private static final String module = FreemarkerUtil.class.getName();
    public static final String errorResource = "OpentapsErrorLabels";

    // Utility class should not be instantiated.
    private FreemarkerUtil() { }

    /**
     * Imports macros from a template at an arbitrary location into the environment of the
     *  currently executing Freemarker template processing environment. Macros are referenceable
     *  either by <@macroName.../> or <@baseNameOfImportTemplate.macroName.../> syntax
     *
     * @param location String in the OFBiz component:// format
     */
    @SuppressWarnings("unchecked")
    public static void addFreeMarkerMacrosFromLocation(String location) {

        Environment env = Environment.getCurrentEnvironment();
        Locale locale = env.getLocale();

        String errorMessage = null;

        Template template = getTemplate(location, env, locale);

        // Import the macros in the template - this imports into a namespace with the ID templateName
        errorMessage = UtilProperties.getMessage(errorResource, "OpentapsError_FreemarkerUtilImportError", UtilMisc.toMap("location", location), locale);
        Environment.Namespace importNamespace = null;
        try {
            importNamespace = env.importLib(template, template.getName());
        } catch (TemplateException e) {
            Debug.logError(errorMessage + e.getMessage(), module);
        } catch (IOException e) {
            Debug.logError(errorMessage + e.getMessage(), module);
        }

        // Add each macro in the imported template to the global namespace of the parent template, for convenience. Note that this
        //  only works because the macros now exist in their own namespace in the parent template, so by adding them to the global
        //  namespace we're actually referencing the macro in the new namespace. Skipping the step above results in macros which
        //  are unable to access the data model.
        Environment.Namespace globalNamespace = env.getGlobalNamespace();
        Map macros = template.getMacros();
        Iterator macroIt = macros.keySet().iterator();
        while (macroIt.hasNext()) {
            String macroName = (String) macroIt.next();
            Macro macro = (Macro) macros.get(macroName);
            globalNamespace.put(macroName, macro);
        }
    }

    /**
     * Gets a template from the cache or load it.  Also puts the template objects in the environment if it exists.
     * @param location location of the template, such as a component:// path
     * @param env an <code>Environment</code> value to set in the template
     * @param locale the <code>Locale</code> to use for error messages
     * @return a <code>Template</code> value
     */
    public static Template getTemplate(String location, Environment env, Locale locale) {
        Template template = (Template) FreeMarkerWorker.cachedTemplates.get(location);
        if (UtilValidate.isEmpty(template)) {
            template = loadTemplate(location, env, locale);
        }
        return template;
    }

    /**
     * As above, but no environment to set.
     * @param location location of the template, such as a component:// path
     * @param locale the <code>Locale</code> to use for error messages
     * @return a <code>Template</code> value
     * @see #getTemplate(String, Environment, Locale)
     */
    public static Template getTemplate(String location, Locale locale) {
        return getTemplate(location, null, locale);
    }

    /**
     * Loads a template into the cache.
     * @param location location of the template, such as a component:// path
     * @param env an <code>Environment</code> value to set in the template
     * @param locale the <code>Locale</code> to use for error messages
     * @return a <code>Template</code> value
     */
    public static Template loadTemplate(String location, Environment env, Locale locale) {
        Template template = null;

        // Open the file at the argument location
        URL templateURL = null;
        Reader locationReader = null;
        String errorMessage = UtilProperties.getMessage(errorResource, "OpentapsError_FreemarkerUtilReadError", UtilMisc.toMap("location", location), locale);
        try {
            templateURL = FlexibleLocation.resolveLocation(location, null);
            locationReader = new InputStreamReader(templateURL.openStream());
        } catch (IOException e) {
            Debug.logError(errorMessage + e.getMessage(), module);
        }

        File locationDirFile = new File(templateURL.toExternalForm());
        String templateName = locationDirFile.getName().replaceAll("\\..*$", "");

        // Parse the template
        Configuration cfg = null;
        errorMessage = UtilProperties.getMessage(errorResource, "OpentapsError_FreemarkerUtilParseError", UtilMisc.toMap("location", location), locale);
        try {
            cfg = FreeMarkerWorker.getDefaultOfbizConfig();
            template = new Template(templateName, locationReader, cfg);
            if (env != null) {
                env.importLib(template, templateName);  // env is the current executing template, and importLib will import the template into it and will map namespace -> macro.
            }
        } catch (TemplateException e) {
            Debug.logError(errorMessage + e.getMessage(), module);
        } catch (IOException e) {
            Debug.logError(errorMessage + e.getMessage(), module);
        }

        // Update the FTL cache
        FreeMarkerWorker.cachedTemplates.put(location, template);

        return template;
    }

    /**
     * Renders an FTL template inline. The child FTL template inherits the context of the parent.
     * The purpose is really to pass the location and hence the responsibility for locating the file back to OFBIZ using the component:// notation,
     * rather than have freemarker look for the file.
     *
     * @param location a location String in the OFBiz component:// format
     * @return the rendered string for the child template
     */
    @SuppressWarnings("unchecked")
    public static String includeFTLByLocation(String location) {
        Writer writer = new StringWriter();
        Environment env = Environment.getCurrentEnvironment();
        Locale locale = env.getLocale();
        Map context = (Map) FreeMarkerWorker.getWrappedObject("context", env);
        try {
            FreeMarkerWorker.renderTemplateAtLocation(location, context, writer);
        } catch (IOException e) {
            String errorMessage = UtilProperties.getMessage(errorResource, "OpentapsError_FreemarkerUtilReadError", UtilMisc.toMap("location", location), locale);
            Debug.logError(errorMessage + e.getMessage(), module);
        } catch (TemplateException e) {
            String errorMessage = UtilProperties.getMessage(errorResource, "OpentapsError_FreemarkerUtilParseError", UtilMisc.toMap("location", location), locale);
            Debug.logError(errorMessage + e.getMessage(), module);
        }
        return writer.toString();
    }

    /**
     * Creates a default freemarker configuration.  After this, it is a good idea to set the cache storage and loader.
     * @return the default <code>Configuration</code>
     * @exception TemplateException if an error occurs
     */
    public static Configuration createDefaultConfiguration() throws TemplateException {
        Environment env = Environment.getCurrentEnvironment();
        Locale locale = env.getLocale();
        Configuration config = new Configuration();
        config.setObjectWrapper(BeansWrapper.getDefaultInstance());
        config.setSetting("datetime_format", UtilDateTime.getDateTimeFormat(locale));
        config.setSetting("date_format", UtilDateTime.getDateFormat(locale));
        config.setSetting("time_format", UtilDateTime.getTimeFormat(locale));
        config.setSetting("number_format", "0.##########");
        return config;
    }

    /**
     * Gets an object argument from the environment, otherwise returns null.
     * It will return the wrapped object of a <code>BeanModel</code> or a <code>WrappingTemplateModel</code>.
     * This is a low level API and should not be used directly.  Instead, try something
     * like {@link #getMap}.
     * @param env the <code>Environment</code> to load from
     * @param name the object name to load from the environment
     * @return an <code>Object</code> value, or <code>null</code>
     * @exception TemplateModelException if an error occurs
     * @see #getMap
     * @see #getString
     */
    public static Object getObject(Environment env, String name) throws TemplateModelException {
        Object obj = env.getVariable(name);
        if (obj instanceof BeanModel) {
            return ((BeanModel) obj).getWrappedObject();
        }
        if (obj instanceof WrappingTemplateModel) {
            return obj;
        }
        return null;
    }

    /**
     * Gets a <code>String</code> argument from the environment.  This will return empty strings as null.
     * @param env the <code>Environment</code> to load from
     * @param name the object name to load from the environment
     * @return a <code>String</code> value, or <code>null</code> if the string was not found or empty
     * @exception TemplateModelException if an error occurs
     */
    public static String getString(Environment env, String name) throws TemplateModelException {
        Object obj = env.getVariable(name);
        if (obj instanceof TemplateScalarModel) {
            String string = ((TemplateScalarModel) obj).getAsString();
            if (UtilValidate.isEmpty(string)) {
                return null;
            }
            return string;
        }
        return null;
    }

    /**
     * Gets a <code>boolean</code> argument from the environment.  Follows the rules for <code>Boolean.valueOf()</code> to convert <code>TemplateScalarModel</code>.
     * @param env the <code>Environment</code> to load from
     * @param name the object name to load from the environment
     * @param defaultValue the value to return if the <code>boolean</code> was not found in the environment
     * @return a <code>boolean</code> value, or <code>defaultValue</code> if it was not found
     * @exception TemplateModelException if an error occurs
     */
    public static boolean getBoolean(Environment env, String name, boolean defaultValue) throws TemplateModelException {
        Object obj = env.getVariable(name);
        if (obj == null) {
            return defaultValue;
        }
        if (obj instanceof TemplateScalarModel) {
            String boolString = ((TemplateScalarModel) obj).getAsString();
            Boolean bool = Boolean.valueOf(boolString);
            return bool.booleanValue();
        }
        if (obj instanceof TemplateBooleanModel) {
            return ((TemplateBooleanModel) obj).getAsBoolean();
        }
        return defaultValue;
    }

    /**
     * Gets a <code>Map</code> argument from the environment.
     * @param env the <code>Environment</code> to load from
     * @param name the object name to load from the environment
     * @return a <code>Map</code> value, or <code>null</code> if it was not found
     * @exception TemplateModelException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public static Map getMap(Environment env, String name) throws TemplateModelException {
        Object obj = getObject(env, name);
        if (obj instanceof Map) {
            return (Map) obj;
        }
        if (obj instanceof SimpleHash) {
            return ((SimpleHash) obj).toMap();
        }
        // TODO: SimpleMapModel doesn't seem to be used in opentaps, but we should implement just in case
        return null;
    }

    /**
     * Renders a template fragment. If leaveTags is true, it will leave tags in place and highlighted in red if their data is missing.
     * @param templateIdString a <code>String</code> value
     * @param template the template code text (a <code>String</code> containing the Freemarker code)
     * @param context the context <code>Map</code> to use for rendering the template
     * @param outWriter the output <code>Writer</code> where the template gets rendered
     * @param leaveTags if set to <code>true</code>, undefined variables will be displayed as the variable name; else if set to <code>false</code>, undefined variables will be displayed as empty strings
     * @param highlightTags if set to <code>true</code> and leaveTags is set to <code>true</code>, undefined variables will be displayed as the variable name in red
     * @exception TemplateException if an error occurs
     * @exception IOException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public static void renderTemplateWithTags(String templateIdString, String template, Map context, Writer outWriter, boolean leaveTags, boolean highlightTags) throws TemplateException, IOException {

        // For each ${beginList:something}...${endList:something} pair, replace the terminators with proper <#list somethings as something> and </#list>
        String listName = null;
        Matcher matcher = Pattern.compile("(?s)\\$\\{beginList:([\\p{L}\\p{Lu}]+)\\}(.*)\\$\\{endList:\\1\\}").matcher(template);
        StringBuffer sb = new StringBuffer();
        int start = 0;
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                listName = matcher.group(1);
                sb.append(template.substring(start, matcher.start()));
                sb.append("<#list ");
                sb.append(listName);
                sb.append("s?default([])");
                sb.append(" as ");
                sb.append(listName);
                sb.append(">");
                if (matcher.group(2) != null) {
                    sb.append(matcher.group(2));
                }
                sb.append("</#list>");
                start = matcher.end();
            }
        }
        sb.append(template.substring(start));
        template = sb.toString();

        // matches the FTL place holders eg: ${someVariable}
        final String ftlPlaceHolderRegex = "\\$\\{(.*?)\\}";
        if (leaveTags) {
            if (highlightTags) {
                // replaces ${someVariable} by ${someVariable?default('<span style="color:ff0000">someVariable</span>')} to avoid template errors: if someVariable is not defined the string "someVariable" will be displayed in red
                template = template.replaceAll(ftlPlaceHolderRegex, "\\${$1?default(\"<span style=\\\\\"color:#ff0000\\\\\">\" + r\"\\${$1}</span>\")}");
            } else {
                // replaces ${someVariable} by ${someVariable?default("someVariable")} to avoid template errors: if someVariable is not defined the string "someVariable" will be displayed
                template = template.replaceAll(ftlPlaceHolderRegex, "\\${$1?default(r\"\\${$1}\")}");
            }
        } else {
            // replaces ${someVariable} by ${someVariable?if_exists} to avoid template errors: if someVariable is not defined nothing will be displayed
            template = template.replaceAll(ftlPlaceHolderRegex, "\\${$1?if_exists}");
        }
        FreeMarkerWorker.renderTemplate(templateIdString, template, context, outWriter);
    }
}
