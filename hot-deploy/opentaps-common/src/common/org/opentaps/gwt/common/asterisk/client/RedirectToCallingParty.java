/*
 * Copyright (c) 2007 - 2009 Open Source Strategies, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 *
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
 */
package org.opentaps.gwt.common.asterisk.client;

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
 * the Entry point classes of Asterisk Receive Call In.
 */
public class RedirectToCallingParty extends BaseEntry {

    private String remoteUrl = "gwtCallInAccount";
    private String checkFrequencySecondsUrl = "gwtAsteriskCheckFrequency";
    private String asteriskNotificationDiv = "gwtAsteriskNotification";

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
                        // TODO Auto-generated method stub
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
                                            RootPanel.get(asteriskNotificationDiv).clear();
                                            OpentapsConfig config = new OpentapsConfig();
                                            HTML callInTips = new HTML(UtilUi.MSG.callInDisplayMessage(config.getCallInEventIcon(), returnText));
                                            RootPanel.get(asteriskNotificationDiv).add(callInTips);
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
