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

package org.opentaps.gwt.common.server.form;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.service.ServiceValidationException;
import org.opentaps.common.event.AjaxEvents;
import org.opentaps.gwt.common.client.lookup.UtilLookup;

/**
 * A response wrapper to convert GWT service call results into a JSON response.
 */
public class JsonResponse {

    private static final String MODULE = JsonResponse.class.getName();

    private HttpServletResponse response;

    /**
     * Creates a new <code>JsonResponse</code> instance.
     * @param response a <code>HttpServletResponse</code> value
     */
    public JsonResponse(HttpServletResponse response) {
        this.response = response;
    }

    /**
     * Makes a standard JSON response from a service result <code>Map</code>.
     * @param callResults the service call result <code>Map</code>
     * @return JSON response string
     */
    public String makeResponse(Map<String, Object> callResults) {

        if (ServiceUtil.isError(callResults) || ServiceUtil.isFailure(callResults)) {
            return makeSimpleErrorResponse(ServiceUtil.getErrorMessage(callResults));
        }

        Map<String, Object> retval = new HashMap<String, Object>();
        retval.put(UtilLookup.JSON_SUCCESS, true);
        retval.put(UtilLookup.JSON_TOTAL, 1);
        JSONArray jsonArray = new JSONArray();
        jsonArray.element(JSONObject.fromObject(callResults));
        retval.put(UtilLookup.JSON_SUCCESS_RESPONSE, jsonArray);

        return AjaxEvents.doJSONResponse(response, JSONObject.fromObject(retval));
    }

    /**
     * Makes a standard JSON error response from just an error message.
     * @param errorMessage the error message <code>String</code>
     * @return JSON response string
     */
    public String makeSimpleErrorResponse(String errorMessage) {

        Map<String, Object> retval = new HashMap<String, Object>();
        retval.put(UtilLookup.JSON_SUCCESS, false);
        retval.put(UtilLookup.JSON_TOTAL, 1);
        JSONArray jsonArray = new JSONArray();
        jsonArray.element(UtilMisc.toMap(UtilLookup.JSON_ERROR_MESSAGE, errorMessage));
        retval.put(UtilLookup.JSON_ERROR_EXCEPTION, jsonArray);

        return AjaxEvents.doJSONResponse(response, JSONObject.fromObject(retval));
    }

   /**
     * Make a standard error JSON response from a <code>Throwable</code>.
     * @param exception the <code>Throwable</code>
     * @return JSON response string
     */
    public String makeResponse(Throwable exception) {
        if (exception instanceof CustomServiceValidationException) {
            return makeResponse((CustomServiceValidationException) exception);
        } else if (exception instanceof ServiceValidationException) {
            return makeResponse((ServiceValidationException) exception);
        }

        Debug.logError(exception, MODULE);
        return makeSimpleErrorResponse(formatExceptionMessage(exception));
    }

    /**
     * Make a standard error JSON response from a <code>ServiceValidationException</code>.
     * @param exception the <code>ServiceValidationException</code>
     * @return JSON response string
     */
    @SuppressWarnings("unchecked")
    protected String makeResponse(ServiceValidationException exception) {
        List<String> missingFields = exception.getMissingFields();
        List<String> extraFields = exception.getExtraFields();
        Debug.logError("ServiceValidationException: missing fields [" + missingFields + "] extra fields: [" + extraFields + "]", MODULE);

        if (missingFields.isEmpty() && extraFields.isEmpty()) {
            return makeSimpleErrorResponse(formatExceptionMessage(exception));
        }

        JSONArray jsonArray = new JSONArray();
        for (String f : missingFields) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(UtilLookup.JSON_ERROR_FIELD_NAME, f);
            jsonObject.put(UtilLookup.JSON_ERROR_MESSAGE, UtilLookup.JSON_ERROR_MISSING_FIELD);
            jsonArray.element(jsonObject);
        }

        for (String f : extraFields) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(UtilLookup.JSON_ERROR_FIELD_NAME, f);
            jsonObject.put(UtilLookup.JSON_ERROR_MESSAGE, UtilLookup.JSON_ERROR_EXTRA_FIELD);
            jsonArray.element(jsonObject);
        }

        Map<String, Object> retval = new HashMap<String, Object>();
        retval.put(UtilLookup.JSON_SUCCESS, false);
        retval.put(UtilLookup.JSON_ERRORS, jsonArray);

        return AjaxEvents.doJSONResponse(response, JSONObject.fromObject(retval));
    }

    /**
     * Make a standard error JSON response from a <code>CustomServiceValidationException</code>.
     * @param exception the <code>CustomServiceValidationException</code>
     * @return JSON response string
     */
    @SuppressWarnings("unchecked")
    protected String makeResponse(CustomServiceValidationException exception) {
        List<String> missingFields = exception.getMissingFields();
        List<String> extraFields = exception.getExtraFields();
        Map<String, String> customFieldsErrors = exception.getCustomFieldsErrors();
        Debug.logError("CustomServiceValidationException: missing fields [" + missingFields + "]; extra fields: [" + extraFields + "]; custom fields errors: [" + customFieldsErrors + "]", MODULE);

        if (missingFields.isEmpty() && extraFields.isEmpty() && customFieldsErrors.isEmpty()) {
            return makeSimpleErrorResponse(formatExceptionMessage(exception));
        }

        JSONArray jsonArray = new JSONArray();
        for (String f : missingFields) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(UtilLookup.JSON_ERROR_FIELD_NAME, f);
            jsonObject.put(UtilLookup.JSON_ERROR_MESSAGE, UtilLookup.JSON_ERROR_MISSING_FIELD);
            jsonArray.element(jsonObject);
        }

        for (String f : extraFields) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(UtilLookup.JSON_ERROR_FIELD_NAME, f);
            jsonObject.put(UtilLookup.JSON_ERROR_MESSAGE, UtilLookup.JSON_ERROR_EXTRA_FIELD);
            jsonArray.element(jsonObject);
        }

        for (String f : customFieldsErrors.keySet()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(UtilLookup.JSON_ERROR_FIELD_NAME, f);
            jsonObject.put(UtilLookup.JSON_ERROR_MESSAGE, customFieldsErrors.get(f));
            jsonArray.element(jsonObject);
        }

        Map<String, Object> retval = new HashMap<String, Object>();
        retval.put(UtilLookup.JSON_SUCCESS, false);
        retval.put(UtilLookup.JSON_ERRORS, jsonArray);

        return AjaxEvents.doJSONResponse(response, JSONObject.fromObject(retval));
    }

    /**
     * Gets the details of an exception and returns it as an HTML <code>String</code>.
     * @param t a <code>Throwable</code> value
     * @return a <code>String</code> value
     */
    private String formatExceptionMessage(Throwable t) {
        Debug.logInfo("Formatting exception: " + t.getMessage(), MODULE);
        StringBuffer sb = new StringBuffer(t.getMessage());
        String lastReason = t.getMessage();
        Throwable lastException = t;
        int maxDepth = 10;
        while (t != null && maxDepth > 0) {
            if (t instanceof GeneralException) {
                // GeneralException do not build the Cause but use a custom nested variable
                t = ((GeneralException) t).getNested();
            } else if (t instanceof SQLException) {
                // SQLException may have details in getNextException
                t = ((SQLException) t).getNextException();
            } else {
                t = t.getCause();
            }
            if (t == null || t == lastException) {
                break;
            }
            maxDepth--;
            if (lastReason.equals(t.getMessage())) {
                // avoid repeating itself as it is not helpful
                Debug.logInfo("Skipping cause: " + t.getMessage(), MODULE);
                continue;
            }
            Debug.logInfo("Adding cause: " + t.getMessage(), MODULE);

            sb.append("\n\nCaused by:\n").append(t.getMessage());
            lastReason = t.getMessage();
        }
        // for extra detail this would print the complete stack trace, but it is way too detailed
        // and the box could overflow the screen
        /*for (StackTraceElement element : t.getStackTrace()) {
            sb.append("\n").append(element);
            }*/

        return sb.toString();
    }

}
