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

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import javax.servlet.http.HttpServletRequest;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.cache.UtilCache;
import org.ofbiz.base.util.collections.ResourceBundleMapWrapper;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.service.ServiceUtil;

/**
 * UtilMessage contains methods that simplify the problem of generating <b>useful</b> messsages for the end user.
 * The intent is to make it very easy for the developer to create errors and use them wherever they need to be
 * displayed, while sticking to a standard methodology.
 *
 * <h2>Kinds of Messages</h2>
 * <ul>
 * <li><b>simple</b> message:  Message that is constructed from a ui label, such as OpentapsError_PermissionDenied</li>
 * <li><b>parametrized</b> message:  Message constructed from a ui label containing ${} that get expanded from a context</li>
 * <li><b>generic</b> message: A generic error message constructed from a standard label (see GENERIC_*_LABEL).</li>
 * <li><b>service</b> message:  Message constructed from a service result map using ServiceUtil.getErrorMessage()</li>
 * <li><b>exception</b> message:  Message constructed from an Exceiption using getMessage()</li>
 * <li><b>toplevel</b> message:  Message that appears in the main message area at the top of a screen.</li>
 * <li><b>field</b> message:  Message that is keyed to opentapsErrors.field.${fieldName} and can be displayed using macros.</li>
 * </ul>
 *
 * The most basic way to generate a message is to use the expandLabel() functions, which take a uiLabel and expands it into
 * a localized message.  You may wish to use this when constructing messages for service successes, localized notes,
 * and so on.
 *
 * <h2>opentapsErrors</h2>
 *
 * At every request, there is an opentapsErrors map stored in the request attributes.  It contains two kinds of errors:
 * <b>toplevel</b> and <b>field</b>.  Toplevel errors are a list of error strings that get rendered at the top of
 * the screen, where it is hard to miss.  Field errors are keyed to a field name or arbitrary key and can be used to
 * display an error string next to a form field or anywhere on the page.
 *
 * To add a <b>toplevel</b> error, use one of the addError() methods.  To add a <b>field</b> error, use one of the
 * addFieldError() methods.  You would generally use these methods within a beanshell file or Event method.
 *
 * Alternatively, if you wish to add a special set of error messages for some special purposes, you can simply obtain
 * the opentapsErrors map and add your errors as desired.
 *
 * <h2>Event and Service Messages</h2>
 *
 * There are a number of event and service related convenience methods that capture common patterns.  They generally
 * expand a label and log it as an error.  There are methods that accept exceptions to log, and also service results.
 * The service methods generally return ServiceUtil.returnError() with the error message.  Event methods usually return
 * "error".  Thus you can use these functions as return points.
 *
 * The event error messages are usually added to the <b>toplevel</b> map.  Alternatively, you may wish to use the
 * addError() and addFieldError() methods to create errors.
 *
 * @author <a href="mailto:leon@opensourcestrategies.com">Leon Torres</a>
 */
public final class UtilMessage {

    private UtilMessage() { }

    private static final String MODULE = UtilMessage.class.getName();

    // the string to return to tell the framework that an error occurred during an event
    private static final String EVENT_ERROR = "error";

    // uiLabelMap used by by the opentaps applications, keyed by Locale (these don't have to be synchronized since they are read only)
    private static UtilCache UI_LABELS = UtilCache.createUtilCache("opentaps.ui.labels", 0, 0);

    /** Generic 'internal error' message.  Use this if the user would be confused by the error, then log the details. */
    public static final String GENERIC_ERROR_LABEL = "OpentapsError_Internal";

    /** Generic 'permission denied' message. */
    public static final String GENERIC_PERMISSION_DENIED_LABEL = "OpentapsError_PermissionDenied";

    /** Generic 'this field is required' message. */
    public static final String GENERIC_REQUIRED_LABEL = "OpentapsFieldError_Required";

    /** Defines the uiLabels to load in order.  CommonUiLabels will be loaded first.  These are available to all opentaps applications. */
    public static Properties UI_LABEL_PROPERTIES = UtilProperties.getProperties("LabelConfiguration.properties");
    /** Get the uiLabelMap for User Interface labels. */
    public static ResourceBundleMapWrapper getUiLabels(Locale locale) {
        ResourceBundleMapWrapper localizedLabels = (ResourceBundleMapWrapper) UI_LABELS.get(locale);
        if (localizedLabels == null) {
            synchronized (UI_LABELS) {
                UI_LABELS.clear(); // only way to prevent subsequent simultaneous request from duplicating the stack
                localizedLabels = (ResourceBundleMapWrapper) UtilProperties.getResourceBundleMap("CommonUiLabels", locale);
                Iterator<Entry<Object, Object>> it = UI_LABEL_PROPERTIES.entrySet().iterator();
                while (it.hasNext()) {
                    Entry<Object, Object> entry = it.next();
                    try {

                        localizedLabels.addBottomResourceBundle(UtilProperties.getResourceBundle((String) entry.getKey(), locale));
                    } catch (IllegalArgumentException e) {
                        Debug.logWarning(e.getMessage(), MODULE);
                    }
                }

                try {
                    // reports imported from analytics and their metadata use own resource bundle
                    // in non-typical location.
                    localizedLabels.addBottomResourceBundle(UtilProperties.getResourceBundle("org/opentaps/analytics/locale/messages", locale));
                } catch (IllegalArgumentException e) {
                    Debug.logWarning("Resource bundle for analytics not found.", MODULE);
                }

                UI_LABELS.put(locale, localizedLabels);
            }
        }
        return localizedLabels;
    }

    /**
     * Expands a label into a localized message.
     * @param label the label to expand
     * @param locale a <code>Locale</code> value
     * @return the expanded label for the given <code>Locale</code>
     */
    public static String expandLabel(String label, Locale locale) {
        ResourceBundleMapWrapper labels = getUiLabels(locale);
        return (String) labels.get(label);
    }

    /**
     * Expands a label into a localized message with parameters.
     * @param label the label to expand
     * @param locale a <code>Locale</code> value
     * @param context the context <code>Map</code> to use when expanding the label
     * @return the expanded label for the given <code>Locale</code>
     */
    @SuppressWarnings("unchecked")
    public static String expandLabel(String label, Locale locale, Map context) {
        String message = expandLabel(label, locale);
        return FlexibleStringExpander.expandString(message, context, locale);
    }

    /**
     * Expands a label into a localized message with parameters.
     * @param label the label to expand
     * @param context the context <code>Map</code> to use when expanding the label
     * @param locale a <code>Locale</code> value
     * @return the expanded label for the given <code>Locale</code>
     */
    @SuppressWarnings("unchecked")
    public static String expandLabel(String label, Map context, Locale locale) {
        String message = expandLabel(label, locale);
        return FlexibleStringExpander.expandString(message, context, locale);
    }

    /**
     * Gets the standard internal error message.
     * @param locale a <code>Locale</code> value
     * @return the standard internal error message in the given <code>Locale</code>
     */
    public static String getInternalError(Locale locale) {
        return expandLabel(GENERIC_ERROR_LABEL, locale);
    }

    /**
     * Gets the standard permission denied error message.
     * @param locale a <code>Locale</code> value
     * @return the standard permission denied error message in the given <code>Locale</code>
     */
    public static String getPermissionDeniedError(Locale locale) {
        return expandLabel(GENERIC_PERMISSION_DENIED_LABEL, locale);
    }


    /*************************************************************************/
    /**                                                                     **/
    /**                         opentapsErrors                              **/
    /**                                                                     **/
    /*************************************************************************/


    /**
     * Initializes the opentapsError structure.
     * @return a <code>Map</code> with "toplevel" and "field" keys
     */
    @SuppressWarnings("unchecked")
    private static Map createOpentapsErrors() {
        Map errors = FastMap.newInstance();
        errors.put("toplevel", FastList.newInstance());
        errors.put("field", FastMap.newInstance());
        return errors;
    }

    /**
     * Gets the current opentapsError from request attribute or create a new one (and store in attribute).
     * You may wish to use this method to add special error messages.  It is mostly used by the framework.
     * @param request a <code>HttpServletRequest</code> value
     * @return a <code>Map</code> with "toplevel" and "field" keys
     */
    @SuppressWarnings("unchecked")
    public static Map getOpentapsErrors(HttpServletRequest request) {
        Map errors = (Map) request.getAttribute("opentapsErrors");
        if (errors == null) {
            errors = createOpentapsErrors();
            request.setAttribute("opentapsErrors", errors);
        }
        return errors;
    }

    /**
     * Adds a <b>toplevel</b> error string to opentapsErrors.  This method should be avoided since it does not localize.
     * @param request a <code>HttpServletRequest</code> value
     * @param error the error message string
     * @see #addError(HttpServletRequest, String)
     */
    @SuppressWarnings("unchecked")
    public static void addToplevelOpentapsError(HttpServletRequest request, String error) {
        Map errors = getOpentapsErrors(request);
        List top = (List) errors.get("toplevel");
        top.add(error);
    }

    /**
     * Adds a <b>field</b> error string to opentapsErrors.  This method should be avoided since it does not localize.
     * @param request a <code>HttpServletRequest</code> value
     * @param fieldName the name of the field for which the error occurred
     * @param error the error message string
     * @see #addFieldError(HttpServletRequest, String, String)
     */
    @SuppressWarnings("unchecked")
    public static void addFieldOpentapsError(HttpServletRequest request, String fieldName, String error) {
        Map errors = getOpentapsErrors(request);
        Map fieldMap = (Map) errors.get("field");
        fieldMap.put(fieldName, error);
    }

    /**
     * Adds a <b>simple</b> error message that will appear in the main message area.
     * @param request a <code>HttpServletRequest</code> value
     * @param label the label to expand
     */
    public static void addError(HttpServletRequest request, String label) {
        Locale locale = UtilHttp.getLocale(request);
        addToplevelOpentapsError(request, expandLabel(label, locale));
    }

    /**
     * Adds a <b>parameterized</b> error message that will appear in the main message area.
     * @param request a <code>HttpServletRequest</code> value
     * @param label the label to expand
     * @param context the context <code>Map</code> to use when expanding the label
     */
    @SuppressWarnings("unchecked")
    public static void addError(HttpServletRequest request, String label, Map context) {
        Locale locale = UtilHttp.getLocale(request);
        addToplevelOpentapsError(request, expandLabel(label, locale, context));
    }

    /**
     * Adds a <b>generic</b> permission denied error message that will appear in the main message area.
     * @param request a <code>HttpServletRequest</code> value
     */
    public static void addPermissionDeniedError(HttpServletRequest request) {
        Locale locale = UtilHttp.getLocale(request);
        addToplevelOpentapsError(request, expandLabel(GENERIC_PERMISSION_DENIED_LABEL, locale));
    }

    /**
     * Adds a <b>simple field</b> error message that can be placed next to the offending field.
     * To retrieve it:  opentapsErrors.field.${fieldName} or use <@displayError name="${fieldName}" />.
     * @param request a <code>HttpServletRequest</code> value
     * @param fieldName the name of the field for which the error occurred
     * @param label the label to expand
     */
    public static void addFieldError(HttpServletRequest request, String fieldName, String label) {
        Locale locale = UtilHttp.getLocale(request);
        addFieldOpentapsError(request, fieldName, expandLabel(label, locale));
    }

    /**
     * Adds a <b>parameterized field</b> error message that can be placed next to the offending field.
     * To retrieve it:  opentapsErrors.field.${fieldName} or use <@displayError name="${fieldName}" />.
     * @param request a <code>HttpServletRequest</code> value
     * @param fieldName the name of the field for which the error occurred
     * @param label the label to expand
     * @param context the context <code>Map</code> to use when expanding the label
     */
    @SuppressWarnings("unchecked")
    public static void addFieldError(HttpServletRequest request, String fieldName, String label, Map context) {
        Locale locale = UtilHttp.getLocale(request);
        addFieldOpentapsError(request, fieldName, expandLabel(label, locale, context));
    }

    /**
     * Adds a <b>generic field</b> error message that the field is required.  The generic label is GENERIC_REQUIRED_LABEL.
     * To retrieve it:  opentapsErrors.field.${fieldName} or use <@displayError name="${fieldName}" />.
     * @param request a <code>HttpServletRequest</code> value
     * @param fieldName the name of the field for which the error occurred
     */
    public static void addRequiredFieldError(HttpServletRequest request, String fieldName) {
        Locale locale = UtilHttp.getLocale(request);
        addFieldOpentapsError(request, fieldName, expandLabel(GENERIC_REQUIRED_LABEL, locale));
    }

    /**
     * Adds a <b>generic</b> internal error message that will appear in main message area.
     * @param request a <code>HttpServletRequest</code> value
     */
    public static void addInternalError(HttpServletRequest request) {
        Locale locale = UtilHttp.getLocale(request);
        addToplevelOpentapsError(request, expandLabel(GENERIC_ERROR_LABEL, locale));
    }


    /************************************************************************/
    /**                                                                    **/
    /**                         Event Messages                             **/
    /**                                                                    **/
    /************************************************************************/

    /**
     * Adds a list of <b>simple</b> error messages to <b>toplevel</b> and logs it at level ERROR.
     * Returns "error" as a convenience.
     * @param request a <code>HttpServletRequest</code> value
     * @param labels the label to expand
     * @param locale a <code>Locale</code> value
     * @param module a <code>String</code> value, normally the name of class where the error occurred
     * @return the event error string which tell the framework that an error occurred, ie: "error"
     */
    public static String createAndLogEventErrors(HttpServletRequest request, List<String> labels, Locale locale, String module) {
        for (String label : labels) {
            String errorMsg = expandLabel(label, locale);
            Debug.log(Debug.ERROR, null, errorMsg, module, MODULE);
            addToplevelOpentapsError(request, errorMsg);
        }
        return EVENT_ERROR;
    }

    /**
     * Adds a <b>simple</b> error message to <b>toplevel</b> and logs it at level ERROR.
     * Returns "error" as a convenience.
     * @param request a <code>HttpServletRequest</code> value
     * @param message the error message
     * @param module a <code>String</code> value, normally the name of class where the error occurred
     * @return the event error string which tell the framework that an error occurred, ie: "error"
     */
    public static String createAndLogEventError(HttpServletRequest request, String message, String module) {
        Debug.log(Debug.ERROR, null, message, module, MODULE);
        addToplevelOpentapsError(request, message);
        return EVENT_ERROR;
    }

    /**
     * Adds a <b>simple</b> error message to <b>toplevel</b> and logs it at level ERROR.
     * Returns "error" as a convenience.
     * @param request a <code>HttpServletRequest</code> value
     * @param label the label to expand
     * @param locale a <code>Locale</code> value
     * @param module a <code>String</code> value, normally the name of class where the error occurred
     * @return the event error string which tell the framework that an error occurred, ie: "error"
     */
    public static String createAndLogEventError(HttpServletRequest request, String label, Locale locale, String module) {
        String errorMsg = expandLabel(label, locale);
        Debug.log(Debug.ERROR, null, errorMsg, module, MODULE);
        addToplevelOpentapsError(request, errorMsg);
        return EVENT_ERROR;
    }

    /**
     * Adds a <b>parameterized</b> error message to <b>toplevel</b> and logs it at level ERROR.
     * Returns "error" as a convenience.
     * @param request a <code>HttpServletRequest</code> value
     * @param label the label to expand
     * @param context the context <code>Map</code> to use when expanding the label
     * @param locale a <code>Locale</code> value
     * @param module a <code>String</code> value, normally the name of class where the error occurred
     * @return a <code>String</code> value
     */
    @SuppressWarnings("unchecked")
    public static String createAndLogEventError(HttpServletRequest request, String label, Map context, Locale locale, String module) {
        String errorMsg = expandLabel(label, locale, context);
        Debug.log(Debug.ERROR, null, errorMsg, module, MODULE);
        addToplevelOpentapsError(request, errorMsg);
        return EVENT_ERROR;
    }

    /**
     * Adds an <b>exception</b> error message to <b>toplevel</b> and logs the exception.
     * Returns "error" as a convenience.
     * @param request a <code>HttpServletRequest</code> value
     * @param e the <code>Exception</code> to log
     * @param locale a <code>Locale</code> value
     * @param module a <code>String</code> value, normally the name of class where the error occurred
     * @return a <code>String</code> value
     */
    public static String createAndLogEventError(HttpServletRequest request, Exception e, Locale locale, String module) {
        Debug.log(Debug.ERROR, e, e.getMessage(), module, MODULE);
        addToplevelOpentapsError(request, e.getMessage());
        return EVENT_ERROR;
    }

    /**
     * Adds a <b>service</b> error message to <b>toplevel</b> and logs it as an ERROR.
     * Returns "error" as a convenience.
     * @param request a <code>HttpServletRequest</code> value
     * @param serviceResults the <code>Map</code> returned by the service engine
     * @param locale a <code>Locale</code> value
     * @param module a <code>String</code> value, normally the name of class where the error occurred
     * @return a <code>String</code> value
     */
    @SuppressWarnings("unchecked")
    public static String createAndLogEventError(HttpServletRequest request, Map serviceResults, Locale locale, String module) {
        String errorMsg = ServiceUtil.getErrorMessage(serviceResults);
        Debug.log(Debug.ERROR, null, errorMsg, module, MODULE);
        addToplevelOpentapsError(request, errorMsg);
        return EVENT_ERROR;
    }


    /************************************************************************/
    /**                                                                    **/
    /**                        Service Messages                            **/
    /**                                                                    **/
    /************************************************************************/

    /**
     * Return a <b>simple</b> service success.
     * @param label the label to expand
     * @param locale a <code>Locale</code> value
     * @return a service success <code>Map</code> response
     */
    public static Map<String, Object> createServiceSuccess(String label, Locale locale) {
        return ServiceUtil.returnSuccess(expandLabel(label, locale));
    }

    /**
     * Return a <b>parameterized</b> service success.
     * @param label the label to expand
     * @param locale a <code>Locale</code> value
     * @param context the context <code>Map</code> to use when expanding the label
     * @return a service success <code>Map</code> response
     */
    public static Map<String, Object> createServiceSuccess(String label, Locale locale, Map<String, ?> context) {
        return ServiceUtil.returnSuccess(expandLabel(label, locale, context));
    }

    /**
     * Return a <b>simple</b> service error.
     * @param label the label to expand
     * @param locale a <code>Locale</code> value
     * @return a service error <code>Map</code> response
     */
    public static Map<String, Object> createServiceError(String label, Locale locale) {
        return ServiceUtil.returnError(expandLabel(label, locale));
    }

    /**
     * Return a <b>parameterized</b> service error.
     * @param label the label to expand
     * @param locale a <code>Locale</code> value
     * @param context the context <code>Map</code> to use when expanding the label
     * @return a service error <code>Map</code> response
     */
    public static Map<String, Object> createServiceError(String label, Locale locale, Map<String, ?> context) {
        return ServiceUtil.returnError(expandLabel(label, locale, context));
    }

    /**
     * Returns a service error with a <b>simple</b> error message.  Also logs it at level ERROR.
     * @param label the label to expand
     * @param locale a <code>Locale</code> value
     * @param module a <code>String</code> value, normally the name of class where the error occurred
     * @return a service error <code>Map</code> response
     */
    public static Map<String, Object> createAndLogServiceError(String label, Locale locale, String module) {
        String errorMsg = expandLabel(label, locale);
        Debug.log(Debug.ERROR, null, errorMsg, module, MODULE);
        return ServiceUtil.returnError(errorMsg);
    }

    /**
     * Returns a service failure with a <b>simple</b> error message.  Also logs it at Warning level.
     * @param errorMsg the message to log and return
     * @param module a <code>String</code> value, normally the name of class where the error occurred
     * @return a service failure <code>Map</code> response
     */
    public static Map<String, Object> createAndLogServiceFailure(String errorMsg, String module) {
        Debug.log(Debug.WARNING, null, errorMsg, module, MODULE);
        return ServiceUtil.returnFailure(errorMsg);
    }

    /**
     * Returns a service failure with a <b>simple</b> error message.  Also logs it at Warning level.
     * @param label the label to expand
     * @param locale a <code>Locale</code> value
     * @param module a <code>String</code> value, normally the name of class where the error occurred
     * @return a service failure <code>Map</code> response
     */
    public static Map<String, Object> createAndLogServiceFailure(String label, Locale locale, String module) {
        String errorMsg = expandLabel(label, locale);
        Debug.log(Debug.WARNING, null, errorMsg, module, MODULE);
        return ServiceUtil.returnFailure(errorMsg);
    }

    /**
     * Returns a service success with a <b>simple</b> error message.  Also logs it at Info level.
     * @param label the label to expand
     * @param locale a <code>Locale</code> value
     * @param module a <code>String</code> value, normally the name of class where the error occurred
     * @return a service success <code>Map</code> response
     */
    public static Map<String, Object> createAndLogServiceSuccess(String label, Locale locale, String module) {
        String errorMsg = expandLabel(label, locale);
        Debug.log(Debug.INFO, null, errorMsg, module, MODULE);
        return ServiceUtil.returnSuccess(errorMsg);
    }

    /**
     * Returns a service error with a <b>compound</b> error message constructed from two <b>simple</b> error messages.
     * The message is (prefixLabel + " " + suffixLabel).  Also logs it at level ERROR.
     * @param prefixLabel a prefix label to expand
     * @param suffixLabel a suffix label to expand
     * @param locale a <code>Locale</code> value
     * @param module a <code>String</code> value, normally the name of class where the error occurred
     * @return a service error <code>Map</code> response
     */
    public static Map<String, Object> createAndLogServiceError(String prefixLabel, String suffixLabel, Locale locale, String module) {
        String errorMsg = expandLabel(prefixLabel, locale) + " " +  expandLabel(suffixLabel, locale);
        Debug.log(Debug.ERROR, null, errorMsg, module, MODULE);
        return ServiceUtil.returnError(errorMsg);
    }

    /**
     * Returns a service error with a <b>service</b>.  The message is constructed with ServiceUtil.getErrorMessage(serviceResult)
     * Also logs it at level ERROR.
     * @param serviceResult the <code>Map</code> returned by the service engine
     * @param module a <code>String</code> value, normally the name of class where the error occurred
     * @return a service error <code>Map</code> response
     */
    public static Map<String, Object> createAndLogServiceError(Map<String, Object> serviceResult, String module) {
        String errorMsg = ServiceUtil.getErrorMessage(serviceResult);
        Debug.log(Debug.ERROR, null, errorMsg, module, MODULE);
        return ServiceUtil.returnError(errorMsg);
    }

    /**
     * Returns a service error with a <b>compound</b> error message constructed from a <b>simple</b> error messsage and
     * a service error message.  The message is (label + " " + serviceError). Also logs it at level ERROR.
     * @param serviceResult the <code>Map</code> returned by the service engine
     * @param label the label to expand
     * @param locale a <code>Locale</code> value
     * @param module a <code>String</code> value, normally the name of class where the error occurred
     * @return a service error <code>Map</code> response
     */
    public static Map<String, Object> createAndLogServiceError(Map<String, Object> serviceResult, String label, Locale locale, String module) {
        String errorMsg = expandLabel(label, locale) + " " + ServiceUtil.getErrorMessage(serviceResult);
        Debug.log(Debug.ERROR, null, errorMsg, module, MODULE);
        return ServiceUtil.returnError(errorMsg);
    }
    /**
     * Returns a service error with a <b>compound</b> error message constructed from a <b>simple</b> error message and
     * an exception message.  The message is (label + " " + e.getMessage()).  Also logs it at level ERROR.
     * @param e the <code>Exception</code> to log
     * @param message a <code>String</code> value
     * @param module a <code>String</code> value, normally the name of class where the error occurred
     * @return a service error <code>Map</code> response
     */
    public static Map<String, Object> createAndLogServiceError(Exception e, String message, String module) {
        String errorMsg = message + " " + e.getMessage();
        Debug.log(Debug.ERROR, e, errorMsg, module, MODULE);
        return ServiceUtil.returnError(errorMsg);
    }

    /**
     * Returns a service error with a <b>compound</b> error message constructed from a <b>simple</b> error message and
     * an exception message.  The message is (message + " " + e.getMessage()).  Also logs it at level ERROR.
     * @param e the <code>Exception</code> to log
     * @param label the label to expand
     * @param locale a <code>Locale</code> value
     * @param module a <code>String</code> value, normally the name of class where the error occurred
     * @return a service error <code>Map</code> response
     */
    public static Map<String, Object> createAndLogServiceError(Exception e, String label, Locale locale, String module) {
        String errorMsg = expandLabel(label, locale) + " " + e.getMessage();
        Debug.log(Debug.ERROR, e, errorMsg, module, MODULE);
        return ServiceUtil.returnError(errorMsg);
    }

    /**
     * Returns a service error with an error message and logs the passed in message.
     * @param message a <code>String</code> value
     * @param module a <code>String</code> value, normally the name of class where the error occurred
     * @return a service error <code>Map</code> response
     */
    public static Map<String, Object> createAndLogServiceError(String message, String module) {
        Debug.logError(message, module);
        return ServiceUtil.returnError(message);
    }

    /**
     * Returns a service error with an <b>exception</b> error message and logs the passed in exception.
     * @param e the <code>Exception</code> to log
     * @param module a <code>String</code> value, normally the name of class where the error occurred
     * @return a service error <code>Map</code> response
     */
    public static Map<String, Object> createAndLogServiceError(Exception e, String module) {
        Debug.log(Debug.ERROR, e, e.getMessage(), module, MODULE);
        return ServiceUtil.returnError(e.getMessage());
    }

    /**
     * Returns a service error with a <b>generic</b> error message and logs the passed in exception.
     * The generic error is defined by GENERIC_ERROR_LABEL.
     * @param e the <code>Exception</code> to log
     * @param locale a <code>Locale</code> value
     * @param module a <code>String</code> value, normally the name of class where the error occurred
     * @return a service error <code>Map</code> response
     */
    public static Map<String, Object> createAndLogServiceError(Exception e, Locale locale, String module) {
        Debug.log(Debug.ERROR, e, e.getMessage(), module, MODULE);
        return ServiceUtil.returnError(expandLabel(GENERIC_ERROR_LABEL, locale) + ": " + e.getMessage());
    }

    /**
     * Returns a service error with a <b>parametrized</b> error message.  Also logs it at level ERROR.
     * @param label the label to expand
     * @param context the context <code>Map</code> to use when expanding the label
     * @param locale a <code>Locale</code> value
     * @param module a <code>String</code> value, normally the name of class where the error occurred
     * @return a service error <code>Map</code> response
     */
    public static Map<String, Object> createAndLogServiceError(String label, Map<String, ?> context, Locale locale, String module) {
        String errorMsg = expandLabel(label, locale, context);
        Debug.log(Debug.ERROR, null, errorMsg, module, MODULE);
        return ServiceUtil.returnError(errorMsg);
    }

    /**
     * Returns a service failure with a <b>parametrized</b> error message.  Also logs it at Warning level.
     * @param label the label to expand
     * @param context the context <code>Map</code> to use when expanding the label
     * @param locale a <code>Locale</code> value
     * @param module a <code>String</code> value, normally the name of class where the error occurred
     * @return a service failure <code>Map</code> response
     */
    public static Map<String, Object> createAndLogServiceFailure(String label, Map<String, ?> context, Locale locale, String module) {
        String errorMsg = expandLabel(label, locale, context);
        Debug.log(Debug.WARNING, null, errorMsg, module, MODULE);
        return ServiceUtil.returnFailure(errorMsg);
    }

    /**
     * Logs a <b>generic</b> service message at Info level.
     * @param label the label to expand
     * @param locale a <code>Locale</code> value
     * @param module a <code>String</code> value, normally the name of class where the error occurred
     */
    public static void logServiceInfo(String label, Locale locale, String module) {
        Debug.log(Debug.INFO, null, expandLabel(label, locale), module, MODULE);
    }

    /**
     * Logs a <b>parameterized</b> service message at Info level.
     * @param label the label to expand
     * @param context the context <code>Map</code> to use when expanding the label
     * @param locale a <code>Locale</code> value
     * @param module a <code>String</code> value, normally the name of class where the error occurred
     */
    @SuppressWarnings("unchecked")
    public static void logServiceInfo(String label, Map context, Locale locale, String module) {
        Debug.log(Debug.INFO, null, expandLabel(label, locale, context), module, MODULE);
    }

    /**
     * Logs a <b>generic</b> service message at Warning level.
     * @param label the label to expand
     * @param locale a <code>Locale</code> value
     * @param module a <code>String</code> value, normally the name of class where the error occurred
     */
    public static void logServiceWarning(String label, Locale locale, String module) {
        Debug.log(Debug.WARNING, null, expandLabel(label, locale), module, MODULE);
    }

    /**
     * Logs a <b>parameterized</b> service message at Warning level.
     * @param label the label to expand
     * @param context the context <code>Map</code> to use when expanding the label
     * @param locale a <code>Locale</code> value
     * @param module a <code>String</code> value, normally the name of class where the error occurred
     */
    @SuppressWarnings("unchecked")
    public static void logServiceWarning(String label, Map context, Locale locale, String module) {
        Debug.log(Debug.WARNING, null, expandLabel(label, locale, context), module, MODULE);
    }

    /**
     * Logs a <b>generic</b> service message at Error level.
     * @param label the label to expand
     * @param locale a <code>Locale</code> value
     * @param module a <code>String</code> value, normally the name of class where the error occurred
     */
    public static void logServiceError(String label, Locale locale, String module) {
        Debug.log(Debug.ERROR, null, expandLabel(label, locale), module, MODULE);
    }

    /**
     * Logs a <b>parameterized</b> service message at Error level.
     * @param label the label to expand
     * @param context the context <code>Map</code> to use when expanding the label
     * @param locale a <code>Locale</code> value
     * @param module a <code>String</code> value, normally the name of class where the error occurred
     */
    @SuppressWarnings("unchecked")
    public static void logServiceError(String label, Map context, Locale locale, String module) {
        Debug.log(Debug.ERROR, null, expandLabel(label, locale, context), module, MODULE);
    }

    /**
     * Logs a <b>generic</b> service message with an Exception at Error level.
     * @param e the <code>Exception</code> to log
     * @param label the label to expand
     * @param locale a <code>Locale</code> value
     * @param module a <code>String</code> value, normally the name of class where the error occurred
     */
    public static void logServiceError(Exception e, String label, Locale locale, String module) {
        Debug.log(Debug.ERROR, e, expandLabel(label, locale), module, MODULE);
    }

    /**
     * Logs a <b>parameterized</b> service message with an Exception at Error level.
     * @param e the <code>Exception</code> to log
     * @param label the label to expand
     * @param context the context <code>Map</code> to use when expanding the label
     * @param locale a <code>Locale</code> value
     * @param module a <code>String</code> value, normally the name of class where the error occurred
     */
    @SuppressWarnings("unchecked")
    public static void logServiceError(Exception e, String label, Map context, Locale locale, String module) {
        Debug.log(Debug.ERROR, e, expandLabel(label, locale, context), module, MODULE);
    }


    /************************************************************************/
    /**                                                                    **/
    /**                        Miscellaneous                               **/
    /**                                                                    **/
    /************************************************************************/


    /**
     * Returns a <b>generic</b> internal error message writer.
     * @param out a <code>Writer</code> value
     * @param locale a <code>Locale</code> value
     * @return a <code>Writer</code> value
     */
    public static Writer getErrorWriter(Writer out, Locale locale) {
        return getErrorWriter(out, GENERIC_ERROR_LABEL, locale);
    }

    /**
     * Log an exception at level ERROR and return a <b>generic</b> error message writer.
     * @param out a <code>Writer</code> value
     * @param e an <code>Exception</code> value
     * @param locale a <code>Locale</code> value
     * @param module a <code>String</code> value, normally the name of class where the error occurred
     * @return a <code>Writer</code> value
     */
    public static Writer getErrorWriter(Writer out, Exception e, Locale locale, String module) {
        Debug.log(Debug.ERROR, e, e.getMessage(), module, MODULE);
        return getErrorWriter(out, GENERIC_ERROR_LABEL, locale);
    }

    /**
     * Returns a <b>simple</b> error message writer.
     * @param out a <code>Writer</code> value
     * @param label the label to expand
     * @param locale a <code>Locale</code> value
     * @return a <code>Writer</code> value
     */
    public static Writer getErrorWriter(Writer out, String label, Locale locale) {
        return getErrorWriter(out, label, null, locale);
    }

    /**
     * Returns a <b>parameterized</b> error message writer.
     * @param out a <code>Writer</code> value
     * @param label the label to expand
     * @param context the context <code>Map</code> to use when expanding the label
     * @param locale a <code>Locale</code> value
     * @return a <code>Writer</code> value
     */
    @SuppressWarnings("unchecked")
    public static Writer getErrorWriter(final Writer out, final String label, final Map context, final Locale locale) {

        return new Writer(out) {
            private final String errorMsg = (context == null ? expandLabel(label, locale) : expandLabel(label, locale, context));

            @Override public void write(char[] cbuf, int off, int len) throws IOException {
            }

            @Override public void flush() throws IOException {
                out.write(errorMsg);
                out.flush();
            }

            @Override public void close() throws IOException {
            }
        };
    }


}
