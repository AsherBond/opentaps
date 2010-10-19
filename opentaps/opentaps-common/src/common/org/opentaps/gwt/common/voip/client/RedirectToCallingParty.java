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
package org.opentaps.gwt.common.voip.client;

import java.util.Date;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootPanel;
import org.opentaps.gwt.common.client.BaseEntry;
import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.config.OpentapsConfig;
import com.google.gwt.core.client.GWT;

/**
 * the Entry point classes of Voip Receive Call In.
 */
public class RedirectToCallingParty extends BaseEntry {

    private String remoteUrl = "gwtCallInAccount";
    private String checkFrequencySecondsUrl = "gwtVoipCheckFrequency";
    private String voipkNotificationDiv = "gwtVoipNotification";

    /**
     * This is the entry point method.
     * It is loaded for page where the meta tag is found
     */
    public void onModuleLoad() {
        init();
    }

    protected void init() {
        try {
            //add random number avoid cache page
            RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(GWT.getHostPageBaseURL() + checkFrequencySecondsUrl +  "?now=" + new Date().getTime()));
            builder.sendRequest(null, new RequestCallback() {
                    public void onError(Request request, Throwable exception) {
                        // display error message
                        UtilUi.errorMessage(exception.toString());
                    }

                    public void onResponseReceived(Request request, Response response) {
                        if (response.getStatusCode() == Response.SC_OK) {
                            String returnText = response.getText();
                            if (!returnText.equals("")) {
                                setCheckInBoundTimer(Integer.parseInt(returnText));
                            }
                        }
                    }

                });
        } catch (RequestException e) {
            // display error message
            UtilUi.errorMessage(e.toString());
        }
    }

    /**
     * Sets the check in-bound timer.
     * @param seconds the number of seconds between checking in-bound calls
     */
    private void setCheckInBoundTimer(int seconds) {
        Timer timer = new Timer() {
                @Override public void run() {
                    try {
                        //add random number avoid cache page
                        RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(GWT.getHostPageBaseURL() + remoteUrl +  "?now=" + new Date().getTime()));
                        builder.sendRequest(null, new RequestCallback() {
                                public void onError(Request request, Throwable exception) {
                                    // display error message
                                    UtilUi.errorMessage(exception.toString());
                                }
                                public void onResponseReceived(Request request, Response response) {
                                    // when get correct response, clear the div insideHeadertext and display call in link.
                                    if (response.getStatusCode() == Response.SC_OK) {
                                        String returnText = response.getText();
                                        if (!returnText.equals("")) {
                                            RootPanel.get(voipkNotificationDiv).clear();
                                            OpentapsConfig config = new OpentapsConfig();
                                            HTML callInTips = new HTML(UtilUi.MSG.callInDisplayMessage(config.getCallInEventIcon(), returnText));
                                            RootPanel.get(voipkNotificationDiv).add(callInTips);
                                        }
                                    }
                                }
                            });
                    } catch (RequestException e) {
                        // display error message
                        UtilUi.errorMessage(e.toString());
                    }
                }
            };
        // Schedule the timer for every second
        timer.scheduleRepeating(seconds * 1000);
    }
}
