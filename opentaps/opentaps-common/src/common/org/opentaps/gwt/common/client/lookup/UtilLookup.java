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

package org.opentaps.gwt.common.client.lookup;

import org.opentaps.gwt.common.client.UtilUi;

/**
 * Defines the common elements used for client / server communications.
 */
public abstract class UtilLookup {

    private UtilLookup() { }

    /** The default page size for list views and lookup services that feed them. */
    public static final int DEFAULT_LIST_PAGE_SIZE = 10;
    /** The default page size for autocompleters and lookup services that feed them. */
    public static final int DEFAULT_SUGGEST_PAGE_SIZE = 10;

    /** Delimiter used for batch posting (should match the UtilHttp.MULTI_ROW_DELIMITER from ofbiz). */
    public static final String MULTI_ROW_DELIMITER = "_o_";
    /** Parameter indicating a row from a batch should be posted (normally the widget would only post rows that should be posted so this should always be "Y"). */
    public static final String ROW_SUBMIT_PREFIX = "_rowSubmit_o_";

    /** The name of the parameter for the pagination start page. */
    public static final String PARAM_PAGER_START = "start";
    /** The name of the parameter for the pagination limit record. */
    public static final String PARAM_PAGER_LIMIT = "limit";
    /** The name of the parameter for the sort field. */
    public static final String PARAM_SORT_FIELD = "sort";
    /** The name of the parameter for the sort direction. */
    public static final String PARAM_SORT_DIRECTION = "dir";
    /** The name of the parameter for suggest queries. */
    public static final String PARAM_SUGGEST_QUERY = "query";
    /** The name of the parameter for disabling the pagination. */
    public static final String PARAM_NO_PAGER = "noPager";
    /** The name of the parameter for disabling the insertion of a blank field in the response to suggest queries. */
    public static final String PARAM_NO_BLANK = "noBlank";
    /** The name of the parameter for enabling Excel exportation instead of a JSON response. */
    public static final String PARAM_EXPORT_EXCEL = "isExportToExcel";

    /** The name of the parameter for specifying the Create/Update/Delete action. */
    public static final String PARAM_CUD_ACTION = "_CUD_ACTION";
    /** The value of the parameter for specifying the Delete action. */
    public static final String PARAM_CUD_ACTION_DELETE = "DELETE";
    /** The value of the parameter for specifying the Create action. */
    public static final String PARAM_CUD_ACTION_CREATE = "CREATE";
    /** The value of the parameter for specifying the Update action. */
    public static final String PARAM_CUD_ACTION_UPDATE = "UPDATE";

    /** Identify the array used to feed list views and autocompleters. */
    public static final String JSON_ROOT = "items";
    /** Identify the name of the field to use as the id field for list views and autocompleters. */
    public static final String JSON_ID = "identifier";
    /** Identify the number of record in the items array. */
    public static final String JSON_TOTAL = "total";
    /** Identify the success flag of a service response (either true or false for error). */
    public static final String JSON_SUCCESS = "success";
    /** Identify the array of values returned by a service. */
    public static final String JSON_SUCCESS_RESPONSE = "response";
    /** Identify the array of form field errors. */
    public static final String JSON_ERRORS = "errors";
    /** Identify the name of the form field that has an error. */
    public static final String JSON_ERROR_FIELD_NAME = "id";
    /** Identify the error message for a form field, or an exception message. */
    public static final String JSON_ERROR_MESSAGE = "msg";
    /** Identify the array of exception raised by a service. */
    public static final String JSON_ERROR_EXCEPTION = "exception";
    /** Identify the OfBiz error message */
    public static final String JSON_SERVICE_ERROR_MESSAGE = "_ERROR_MESSAGE_";

    /** Error message associated to missing fields. */
    public static final String JSON_ERROR_MISSING_FIELD = "missing";
    /** Error message associated to extra fields. */
    public static final String JSON_ERROR_EXTRA_FIELD = "extra";

    /** Identify the field to use as submit value for an autocompleter. */
    public static final String SUGGEST_ID = "id";
    /** Identify the string to display in the autocompleter. */
    public static final String SUGGEST_TEXT = "text";
    /** The max number or records returned to an autocompleter. */
    public static final int SUGGEST_MAX_RESULTS = 200;

    /** The field name suffix for descriptions, a field name 'foo' can have its value to display in 'fooDescription'. */
    public static final String DESCRIPTION_SUFFIX = "Description";

    /**
     * Checks if the auto submit URL parameter was provided.
     * @return a <code>boolean</code> value
     */
    public static boolean hasAutoSubmitParameter() {
        return "Y".equalsIgnoreCase(UtilUi.getUrlParameter("performFind"));
    }

    /**
     * Gets the configured default timeout (in ms) for ajax requests.
     * This is configured in <code>opentaps.properties</code>, or defaults to 10000.
     * @return the timeout value in ms
     */
    public static native int getAjaxDefaultTimeout() /*-{
        return $wnd.ajaxDefaultTimeOut ? $wnd.ajaxDefaultTimeOut : 10000;
    }-*/;

}
