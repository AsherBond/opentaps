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

package org.opentaps.gwt.common.client;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.Location;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import com.gwtext.client.data.Converter;
import com.gwtext.client.data.Record;
import com.gwtext.client.widgets.MessageBox;
import com.gwtext.client.widgets.MessageBoxConfig;
import com.gwtext.client.widgets.form.Field;
import com.gwtext.client.widgets.layout.FormLayoutData;
import org.opentaps.gwt.common.client.messages.CommonMessages;
import org.opentaps.gwt.common.client.suggest.EntityAutocomplete;

/**
 * Defines some utility methods.
 */
public abstract class UtilUi {

    private static final String MODULE = UtilUi.class.getName();

    // used to convert date field values
    private static final String DATE_FORMAT = "MM/dd/yy";
    private static final String DATE_TIME_FORMAT = "MM/dd/yy HH:mm:ss.S";

    private static final Logger LOG = Logger.getLogger("OpentapsGwt");

    private UtilUi() { }

    /** The UI message interface. */
    public static final CommonMessages MSG = (CommonMessages) GWT.create(CommonMessages.class);

    /** Default style for screenlet widgets such as {@link org.opentaps.gwt.common.client.form.base.ScreenletFormPanel}. */
    public static final String SCREENLET_STYLE = "gwt-screenlet";
    /** Default style for the form header in screenlet widgets such as {@link org.opentaps.gwt.common.client.form.base.ScreenletFormPanel}. */
    public static final String SCREENLET_HEADER_STYLE = "gwt-screenlet-header";
    /** Default style for the form body in screenlet widgets such as {@link org.opentaps.gwt.common.client.form.base.ScreenletFormPanel}. */
    public static final String SCREENLET_BODY_STYLE = "gwt-screenlet-body";
    /** Default style applied to required form fields. */
    public static final String REQUIRED_FIELD_STYLE = "requiredField";
    /** A <code>FormLayoutData</code> used by the base forms objects when adding required fields which gives the appropriate CSS. */
    public static final FormLayoutData REQUIRED_FIELD_DATA =  new FormLayoutData();
    static { REQUIRED_FIELD_DATA.setItemCls(UtilUi.REQUIRED_FIELD_STYLE); }

    /** Default label length used in the forms, in pixels. */
    public static final Integer LABEL_LENGTH = 150;
    /** Default input length used in the forms, in pixels. */
    public static final Integer INPUT_LENGTH = 220;
    /** Default input length used in the forms in screenlet panels, in pixels. */
    public static final Integer SCREENLET_INPUT_LENGTH = 120;
    /** Default width of the form containers, in pixels. */
    public static final Integer FORM_CONTAINER_WIDTH = 450;
    /** Needed to workaround panel bug. */
    public static final Integer FORM_PADDING = 12;
    /** Default width of the list views, in pixels. */
    public static final Integer LIST_CONTAINER_WIDTH = 900;
    /** Default vertical spacing of composed list + form widgets, in pixels. */
    public static final Integer CONTAINERS_VERTICAL_SPACING = 15;

    /** A special record field can be set to "Y" to indicate the row is a summary and should be treated as such. */
    public static final String SUMMARY_ROW_INDICATOR_FIELD = "isSummary";

    /** The excel export icon. */
    public static final String ICON_EXCEL = "/opentaps_images/buttons/spreadsheet.png";
    /** The save icon. */
    public static final String ICON_SAVE = "/images/dojo/src/widget/templates/buttons/save.gif";
    /** The delete icon. */
    public static final String ICON_DELETE = "/images/dojo/src/widget/templates/buttons/cancel.gif";

    /**
     * Redirects to the given URL.
     * @param url a <code>String</code> value
     */
    public static native void redirect(String url)/*-{
        $wnd.location = url;
    }-*/;

    /**
     * Checks if the current URL has the given parameter.
     * @param parameter a <code>String</code> value
     * @return a <code>Boolean</code> value
     */
    public static Boolean hasUrlParameter(String parameter) {
        Map<String, List<String>> params = Location.getParameterMap();
        return params.containsKey(parameter);
    }

    /**
     * Gets a parameter from the current URL.
     * @param parameter a <code>String</code> value
     * @return the parameter value
     */
    public static String getUrlParameter(String parameter) {
        return Location.getParameter(parameter);
    }

    /**
     * Gets a parameter from the current URL for the given Field.
     * @param field the field to get the value from the HTTP parameter
     * @return the parameter value
     */
    public static String getUrlParameter(Field field) {
        return getUrlParameter(field.getName());
    }

    /**
     * Gets a parameter from the current URL and set it in the given Field.
     * @param field the field to set the value from the HTTP parameter
     * @return the parameter value set
     */
    public static String getAndSetUrlParameter(Field field) {
        String p = getUrlParameter(field.getName());
        if (p != null) {
            field.setValue(p);
        }
        return p;
    }

    /**
     * Popups a debug dialog with the given message.
     * This may be more useful than <code>errorMessage</code> strictly for debugging.
     * @param str a <code>String</code> value
     */
    public static void debug(String str) {
        Window.alert(str);
        logDebug(str, MODULE, "debug");
    }

    /**
     * Popups a debug dialog with the given message.
     * This may be more useful than <code>errorMessage</code> strictly for debugging.
     * @param list a List of <code>Object</code>
     */
    public static void debug(List<Object> list) {
        logDebug(toString(list), MODULE, "debug");
        if (list == null) {
            Window.alert("Null list.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Debugging list of ").append(list.size()).append(" objects.\n\n").append(toString(list));
        Window.alert(sb.toString());
    }

    /**
     * Popups a debug dialog with the given message.
     * This may be more useful than <code>errorMessage</code> strictly for debugging.
     * @param list an array of <code>Record</code>
     */
    public static void debug(Record[] list) {
        logDebug(toString(list), MODULE, "debug");
        if (list == null) {
            Window.alert("Null list of records.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Debugging list of ").append(list.length).append(" records.\n\n").append(toString(list));
        Window.alert(sb.toString());
    }

    /**
     * Popups a debug dialog with the given message.
     * This may be more useful than <code>errorMessage</code> strictly for debugging.
     * @param prefix a <code>String</code> to display
     * @param record a <code>Record</code>
     */
    public static void debug(String prefix, Record record) {
        String pre = prefix;
        if (pre != null) {
            pre += ": ";
        }

        logDebug(pre + toString(record), MODULE, "debug");
        if (record == null) {
            Window.alert(pre + "Null record.");
        }

        StringBuilder sb = new StringBuilder(pre);
        sb.append("Debugging record ").append(record.getId()).append(".\n\n").append(toString(record));
        Window.alert(sb.toString());
    }

    /**
     * Popups a debug dialog with the given message.
     * This may be more useful than <code>errorMessage</code> strictly for debugging.
     * @param record a <code>Record</code>
     */
    public static void debug(Record record) {
        debug(null, record);
    }

    /**
     * Popups a debug dialog with the given message.
     * This may be more useful than <code>errorMessage</code> strictly for debugging.
     * @param list a List of <code>Object</code>
     */
    public static void debug(Object[] list) {
        logDebug(toString(list), MODULE, "debug");
        if (list == null) {
            Window.alert("Null list.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Debugging list of ").append(list.length).append(" objects.\n\n").append(toString(list));
        Window.alert(sb.toString());
    }

    /**
     * Popups an error dialog with the given error message.
     * @param error a <code>String</code> value
     */
    public static void errorMessage(String error) {
        errorMessage(error, null);
    }

    /**
     * Pop up an error dialog with the given error message, with a callback to execute once the dialog is closed.
     * @param error a <code>String</code> value
     * @param cb a <code>MessageBox.PromptCallback</code> value
     */
    public static void errorMessage(String error, MessageBox.PromptCallback cb) {
        errorMessage(error, cb, "<no class>", "<no method>");
    }

    /**
     * Pops up an error dialog with the given error message.
     * @param error a <code>String</code> value
     * @param module the class name of the object from which the log is from
     * @param method the method name of the object from which the log is from
     */
    public static void errorMessage(String error, String module, String method) {
        errorMessage(error, null, module, method);
    }

    /**
     * Pops up an error dialog with the given error message, with a callback to execute once the dialog is closed.
     * @param error a <code>String</code> value
     * @param cb a <code>MessageBox.PromptCallback</code> value
     * @param module the class name of the object from which the log is from
     * @param method the method name of the object from which the log is from
     */
    public static void errorMessage(String error, MessageBox.PromptCallback cb, String module, String method) {
        logError(error, module, method);
        MessageBoxConfig config = new MessageBoxConfig();
        config.setTitle("Error");
        config.setMsg("The following error occurred:<br/>" + error);
        config.setButtons(MessageBox.OK);
        config.setIconCls(MessageBox.ERROR);
        if (cb != null) {
            config.setCallback(cb);
        }
        MessageBox.show(config);
    }

    /**
     * Popups a confirm dialog with the given message, with a callback to execute once the dialog is closed.
     * @param title a <code>String</code> value
     * @param message a <code>String</code> value
     * @param buttons the message buttons configuration eg: OK, OKCANCEL, etc ...
     * @param cb a <code>MessageBox.PromptCallback</code> value
     */
    public static void confirmMessage(String title, String message, MessageBox.Button buttons, MessageBox.PromptCallback cb) {
        MessageBoxConfig config = new MessageBoxConfig();
        config.setTitle(title);
        config.setMsg(message);
        config.setButtons(buttons);
        config.setIconCls(MessageBox.QUESTION);
        if (cb != null) {
            config.setCallback(cb);
        }
        MessageBox.show(config);
    }

    /**
     * Identifies a summary record. Summary records are treated a special records that are not rendered like other records and
     * do not trigger record action buttons (update/create/delete).
     * @param record the <code>Record</code> to test
     * @return <code>true</code> if the record is a summary record
     */
    public static boolean isSummary(Record record) {
        return record != null && "Y".equals(record.getAsString(UtilUi.SUMMARY_ROW_INDICATOR_FIELD));
    }

    /**
     * Converts a <code>JsArray</code> to a Java <code>Map</code>.
     * This is used for example when a RPC service returns a list of entities, since the standard reader cannot parse it.
     * For example if the RPC response includes a field like <code>myEntities: [list of EntityInterface]</code>, this can be read with
     * <code>UtilUi.jsonObjectsToMaps((JsArray) record.getAsObject("myEntities", Arrays.asList(field1, field2, ...)));</code>
     * assuming that the reader as a <code>ObjectFieldDef("myEntities")</code> as part of it field definitions.
     *
     * @param array the <code>JsArray</code>
     * @param fields the list of field names to extract in the <code>Map</code>
     * @return a <code>List</code> of <code>Map</code>, or <code>null</code> if the <code>JsArray</code> was <code>null</code>
     */
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> jsonObjectsToMaps(JsArray array, List<String> fields) {
        if (array == null) {
            return null;
        }

        List<Map<String, Object>> res = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < getJsonArrayLength(array); i++) {
            Map<String, Object> map = new HashMap<String, Object>();
            for (String field : fields) {
                map.put(field, getJsonObjectValue(array, i, field));
            }
            res.add(map);
        }
        return res;
    }

    @SuppressWarnings("unchecked")
    private static native int getJsonArrayLength(JsArray array)/*-{
        return array.length;
    }-*/;

    @SuppressWarnings("unchecked")
    private static native Object getJsonObjectValue(JsArray array, int index, String field)/*-{
        return array[index][field];
    }-*/;

    /**
     * Checks if the given String is empty or null.
     * @param s a <code>String</code> value
     * @return a <code>boolean</code> value
     */
    public static boolean isEmpty(String s) {
        return (s == null) || (s.length() == 0);
    }

    /**
     * Checks if the given String is a number.
     * @param s a <code>String</code> value
     * @return a <code>boolean</code> value
     */
    public static native boolean isNumber(String s) /*-{
        return (s - 0) == s && s.length > 0;
    }-*/;

    /**
     * Tries to covnert a <code>String</code> into a <code>BigDecimal</code>, else return <code>defaultValue</code>.
     * @param s a <code>String</code> value
     * @param defaultValue a <code>BigDecimal</code> value
     * @return a <code>BigDecimal</code> value
     */
    public static BigDecimal asBigDecimal(String s, BigDecimal defaultValue) {
        if (UtilUi.isNumber(s)) {
            try {
                return new BigDecimal(s);
            } catch (Exception e) {
                UtilUi.logDebug("[" + s + "] is NOT a valid number: " + e.getMessage(), MODULE, "asBigDecimal");
                return null;
            }
        } else {
            UtilUi.logDebug("[" + s + "] is NOT a valid number", MODULE, "asBigDecimal");
            return null;
        }
    }

    /**
     * Tries to covnert a <code>String</code> into a <code>BigDecimal</code>, else return <code>null</code>.
     * @param s a <code>String</code> value
     * @return a <code>BigDecimal</code> value
     */
    public static BigDecimal asBigDecimal(String s) {
        return asBigDecimal(s, null);
    }

    // Common toString methods, useful for debugging and logging

    /**
     * Formats a list of objects into a string value.
     * @param list an <code>Iterable</code> of objects
     * @return a <code>String</code> value
     */
    public static String toString(Iterable<Object> list) {
        StringBuilder sb = new StringBuilder("[");
        for (Object o : list) {
            sb.append(toString(o)).append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Formats an array of Record into a string value.
     * @param list an array of <code>Record</code>
     * @return a <code>String</code> value
     */
    public static String toString(Record[] list) {
        StringBuilder sb = new StringBuilder("[");
        for (Record r : list) {
            sb.append(toString(r)).append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Formats an object into a string value.
     * @param o an <code>Object</code> value
     * @return the <code>String</code> value of the given object, or the string "NULL" if the object was null
     */
    public static String toString(Object o) {
        if (o == null) {
            return "NULL";
        }
        return o.toString();
    }

    /**
     * Formats a Record into a string value.
     * @param rec a <code>Record</code> value
     * @return a <code>String</code> value
     */
    public static String toString(Record rec) {
        if (rec == null) {
            return "NULL";
        }
        StringBuilder sb = new StringBuilder("[");
        sb.append(" - ").append(rec.getId()).append(" = {");
        for (String s : rec.getFields()) {
            sb.append(s).append(": ").append(rec.getAsObject(s)).append(", ");
        }
        sb.append("}]\n");
        return sb.toString();
    }

    /**
     * Formats an EntityAutocomplete into a string value.
     * @param autocompleter a <code>EntityAutocomplete</code> value
     * @return a <code>String</code> value
     */
    public static String toString(EntityAutocomplete autocompleter) {
        StringBuilder sb = new StringBuilder("[");
        sb.append(autocompleter.getName()).append("{").append(autocompleter.getId()).append("}@").append(autocompleter.getUrl()).append("]");
        return sb.toString();
    }

    // Common methods for the logger

    private static String formatLog(String message, String module, String method) {
        StringBuilder sb = new StringBuilder("[");
        sb.append(module).append("::").append(method).append("] ").append(message);
        return sb.toString();
    }

    /**
     * Logs a message at the DEBUG level.
     * @param message a <code>String</code> value
     * @param module the class name of the object from which the log is from
     * @param method the method name of the object from which the log is from
     */
    public static void logDebug(String message, String module, String method) {
        LOG.log(Level.FINE, formatLog(message, module, method));
    }

    /**
     * Logs a message and a <code>Throwable</code> at the DEBUG level.
     * @param message a <code>String</code> value
     * @param t a <code>Throwable</code> value
     * @param module the class name of the object from which the log is from
     * @param method the method name of the object from which the log is from
     */
    public static void logDebug(String message, Throwable t, String module, String method) {
        LOG.log(Level.FINE, formatLog(message, module, method), t);
    }

    /**
     * Logs a message at the INFO level.
     * @param message a <code>String</code> value
     * @param module the class name of the object from which the log is from
     * @param method the method name of the object from which the log is from
     */
    public static void logInfo(String message, String module, String method) {
        LOG.log(Level.INFO, formatLog(message, module, method));
    }

    /**
     * Logs a message and a <code>Throwable</code> at the INFO level.
     * @param message a <code>String</code> value
     * @param t a <code>Throwable</code> value
     * @param module the class name of the object from which the log is from
     * @param method the method name of the object from which the log is from
     */
    public static void logInfo(String message,  Throwable t, String module, String method) {
        LOG.log(Level.INFO, formatLog(message, module, method), t);
    }

    /**
     * Logs a message at the WARNING level.
     * @param message a <code>String</code> value
     * @param module the class name of the object from which the log is from
     * @param method the method name of the object from which the log is from
     */
    public static void logWarning(String message, String module, String method) {
        LOG.log(Level.WARNING, formatLog(message, module, method));
    }

    /**
     * Logs a message and a <code>Throwable</code> at the WARNING level.
     * @param message a <code>String</code> value
     * @param t a <code>Throwable</code> value
     * @param module the class name of the object from which the log is from
     * @param method the method name of the object from which the log is from
     */
    public static void logWarning(String message, Throwable t, String module, String method) {
        LOG.log(Level.WARNING, formatLog(message, module, method), t);
    }

    /**
     * Logs a message at the ERROR level.
     * @param message a <code>String</code> value
     * @param module the class name of the object from which the log is from
     * @param method the method name of the object from which the log is from
     */
    public static void logError(String message, String module, String method) {
        LOG.log(Level.SEVERE, formatLog(message, module, method));
    }

    /**
     * Logs a message and a <code>Throwable</code> at the ERROR level.
     * @param message a <code>String</code> value
     * @param t a <code>Throwable</code> value
     * @param module the class name of the object from which the log is from
     * @param method the method name of the object from which the log is from
     */
    public static void logError(String message, Throwable t, String module, String method) {
        LOG.log(Level.SEVERE, formatLog(message, module, method), t);
    }

    /**
     * Formats a Date into a string value using the default DATE_FORMAT.
     * @param date a <code>Date</code> value
     * @return a <code>String</code> value
     */
    public static String toDateString(Date date) {
        if (date == null) {
            return null;
        }
        return DateTimeFormat.getFormat(DATE_FORMAT).format(date);
    }

    /**
     * Formats a Date into a string value using the default DATE_TIME_FORMAT.
     * @param date a <code>Date</code> value
     * @return a <code>String</code> value
     */
    public static String toDateTimeString(Date date) {
        if (date == null) {
            return null;
        }
        return DateTimeFormat.getFormat(DATE_TIME_FORMAT).format(date);
    }

    /**
     * Creates a blank widget which has the proper spacing to be used in a Form.
     * @return a <code>Widget</code> value
     */
    public static Widget makeBlankFormCell() {
        return new HTML("<span class='gwt-blank-field'>&nbsp;</span>");
    }

    /**
     * Removes trailing zeros after the 'comma' from a string representing a number.
     * For example converts "100.00" into "100", "100.00100" into "100.001", etc ...
     * @param number a <code>String</code> value
     * @return a <code>String</code> value
     */
    public static String removeTrailingZeros(String number) {
        // convert XX.XX000 into XX.XX
        String rep = number.replaceFirst("^(-?\\d+\\.0*[^0]+)0*\\s*$", "$1");
        // convert XX.000 into XX
        rep = rep.replaceFirst("^(-?\\d+)\\.0*$", "$1");
        return rep;
    }

    /**
     * A converter using {@link #removeTrailingZeros} to be used in grids or other widgets processing data records.
     */
    public static final Converter CLEAN_TRAILING_ZERO_CONVERTER = new Converter() {
            // trim trailing zeros
            public String format(String input) {
                return removeTrailingZeros(input);
            }
        };

}
