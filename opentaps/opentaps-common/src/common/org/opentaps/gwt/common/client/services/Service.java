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

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.gwtext.client.data.JsonReader;
import com.gwtext.client.data.Record;
import com.gwtext.client.data.Store;
import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.form.ServiceErrorReader;
import org.opentaps.gwt.common.client.lookup.UtilLookup;

/**
 * Base class for the services.
 */
public class Service {

    private static final String MODULE = Service.class.getName();

    private String url;
    private String data;
    private Boolean autoPopupErrors = Boolean.TRUE;
    private Store store;

    public Service() {
        super();
    }

    /**
     * Sets the Url where the data is posted.
     * @param url a <code>String</code> value
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Sets the data to Post, this is optional.
     * @param data a <code>String</code> value
     */
    public void setData(String data) {
        this.data = data;
    }

    /**
     * Sets the reader to use to parse the response, this is optional.
     * @param reader a <code>JsonReader</code> value
     */
    public void setReader(JsonReader reader) {
        this.store = new Store(reader);
    }

    /**
     * Sets to False if the errors should be managed in the client, else any error
     *  is automatically popup to the user.
     * Defaults to True.
     * @param autoPopup a <code>Boolean</code> value
     */
    public void setAutoPopupErrors(Boolean autoPopup) {
        this.autoPopupErrors = autoPopup;
    }

    /**
     * Sends the request and get the result asynchronously.
     * @param cb an <code>AsyncCallback</code> instance for the returned <code>Record</code>
     */
    public void request(final AsyncCallback<Record> cb) {
        RequestBuilder request = new RequestBuilder(RequestBuilder.POST, GWT.getHostPageBaseURL() + url);
        request.setHeader("Content-type", "application/x-www-form-urlencoded");
        request.setRequestData(data);
        request.setTimeoutMillis(UtilLookup.getAjaxDefaultTimeout());
        request.setCallback(new RequestCallback() {
                public void onError(Request request, Throwable exception) {
                    // display error message
                    if (autoPopupErrors) {
                        UtilUi.errorMessage(exception.toString());
                    } else {
                        UtilUi.logError("onError, error = " + exception.toString(), MODULE, "request");
                    }
                    cb.onFailure(exception);
                }
                public void onResponseReceived(Request request, Response response) {
                    UtilUi.logInfo("onResponseReceived, response = " + response, MODULE, "request");
                    String err = ServiceErrorReader.getErrorMessageIfAny(response, url);
                    if (err == null) {
                        if (store != null) {
                            store.loadJsonData(response.getText(), false);
                            Record rec = null;
                            if (store.getTotalCount() > 0) {
                                rec = store.getRecordAt(0);
                            }
                            cb.onSuccess(rec);
                        }
                    } else {
                        if (autoPopupErrors) {
                            UtilUi.errorMessage(err);
                        } else {
                            UtilUi.logError("onResponseReceived, error = " + err, MODULE, "request");
                        }
                        cb.onFailure(new RequestException(err));
                    }
                }
            });
        try {
            UtilUi.logInfo("posting request", MODULE, "request");
            request.send();
        } catch (RequestException e) {
            if (autoPopupErrors) {
                UtilUi.errorMessage(e.toString());
            } else {
                UtilUi.logError("Caught RequestException, error = " + e.toString(), MODULE, "request");
            }
            cb.onFailure(e);
        }
    }

}
