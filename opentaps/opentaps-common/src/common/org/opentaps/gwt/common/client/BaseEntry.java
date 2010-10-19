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

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.core.client.GWT;

/**
 * Defines UncaughtExceptionHandler in GWT Base Entry.
 */
public abstract class BaseEntry implements EntryPoint {
    static {
        GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {
            public void onUncaughtException(Throwable throwable) {
              String text = "Uncaught exception: ";
              while (throwable != null) {
                StackTraceElement[] stackTraceElements = throwable.getStackTrace();
                text += throwable.toString() + "\n";
                for (int i = 0; i < stackTraceElements.length; i++) {
                  text += "    at " + stackTraceElements[i] + "\n";
                }
                throwable = throwable.getCause();
                if (throwable != null) {
                  text += "Caused by: " + throwable.getCause();
                }
              }
              UtilUi.errorMessage(text);
            }
          });
        }

    /**
     * Retrieve the <code>Dictionary</code> containing the custom parameters for the given GWT widget.
     * @param widgetId the ID of the widget
     * @return a <code>Dictionary</code> value if it was given, <code>null</code> otherwise
     */
    protected static Dictionary getWidgetParameters(String widgetId) {
        try {
            return Dictionary.getDictionary(widgetId);
        } catch (java.util.MissingResourceException e) {
            return null;
        }
    }

    /**
     * Retrieve a custom parameter for the given GWT widget.
     * @param widgetId the ID of the widget
     * @param parameterName the name of the parameter
     * @return the parameter value if it was given, <code>null</code> otherwise
     */
    protected static String getWidgetParameter(String widgetId, String parameterName) {
        Dictionary dictionary = getWidgetParameters(widgetId);
        if (dictionary == null) {
            return null;
        }
        try {
            return dictionary.get(parameterName);
        } catch (java.util.MissingResourceException e) {
            return null;
        }
    }
}
