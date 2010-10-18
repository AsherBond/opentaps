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

package org.opentaps.gwt.common.client.services;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.gwtext.client.data.FieldDef;
import com.gwtext.client.data.JsonReader;
import com.gwtext.client.data.Record;
import com.gwtext.client.data.RecordDef;
import com.gwtext.client.data.StringFieldDef;
import org.opentaps.gwt.common.client.lookup.UtilLookup;

/**
 * An async service to get or set a View Preference.
 */
public final class ViewPreferenceService extends Service {

    private static final String GET_FIELD = "viewPrefValue";

    private static final JsonReader GET_READER;
    static {
        GET_READER = new JsonReader(new RecordDef(new FieldDef[]{new StringFieldDef(GET_FIELD)}));
        GET_READER.setRoot(UtilLookup.JSON_SUCCESS_RESPONSE);
        GET_READER.setTotalProperty(UtilLookup.JSON_TOTAL);
    }

    private ViewPreferenceService() {
        super();
        // don't pop up error message-box automatically
        setAutoPopupErrors(Boolean.FALSE);
    }

    /**
     * Gets the <code>ViewPreference</code> for the current user and the given type.
     * May return null if the preference is not found or not set.
     * @param viewPrefTypeId the type of the view preference to get
     * @param cb an <code>AsyncCallback</code> instance which is called with the result
     */
    public static void get(String viewPrefTypeId, final AsyncCallback<String> cb) {
        AsyncCallback<Record> reqCb = new AsyncCallback<Record>() {
            public void onSuccess(Record rec) {
                String parsed = null;
                if (rec != null) {
                    parsed = rec.getAsString(GET_FIELD);
                }
                cb.onSuccess(parsed);
            }
            public void onFailure(Throwable t) {
                cb.onFailure(t);
            }
        };
        ViewPreferenceService service = new ViewPreferenceService();
        service.setUrl("gwtGetViewPreference");
        service.setReader(GET_READER);
        service.setData("viewPrefTypeId=" + viewPrefTypeId);
        service.request(reqCb);
    }

    /**
     * Sets the <code>ViewPreference</code> for the current user, given type and given value.
     * @param viewPrefTypeId the type of the view preference to set
     * @param value the view preference value to set
     * @param cb an <code>AsyncCallback</code> instance which is called with the result
     */
    public static void set(String viewPrefTypeId, String value, final AsyncCallback<Void> cb) {
        AsyncCallback<Record> reqCb = new AsyncCallback<Record>() {
            public void onSuccess(Record rec) {
                cb.onSuccess(null);
            }
            public void onFailure(Throwable t) {
                cb.onFailure(t);
            }
        };
        ViewPreferenceService service = new ViewPreferenceService();
        service.setUrl("gwtSetViewPreference");
        service.setData("viewPrefTypeId=" + viewPrefTypeId + "&viewPrefValue=" + value);
        service.request(reqCb);
    }
}
