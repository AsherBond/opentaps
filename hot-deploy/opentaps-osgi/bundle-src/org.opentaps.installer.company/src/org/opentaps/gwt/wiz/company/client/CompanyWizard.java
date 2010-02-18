/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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
package org.opentaps.gwt.wiz.company.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootPanel;
import com.gwtext.client.core.EventObject;
import com.gwtext.client.core.Margins;
import com.gwtext.client.core.RegionPosition;
import com.gwtext.client.util.Format;
import com.gwtext.client.util.JSON;
import com.gwtext.client.widgets.Button;
import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.event.ButtonListenerAdapter;
import com.gwtext.client.widgets.layout.BorderLayout;
import com.gwtext.client.widgets.layout.BorderLayoutData;
import com.gwtext.client.widgets.layout.FitLayout;
import com.gwtext.client.widgets.layout.HorizontalLayout;


/**
 *
 *
 */
public class CompanyWizard implements EntryPoint {

    String NAV = "/osgi/InstNav";

    /** {@inheritDoc} */
    public void onModuleLoad() {
        Panel form = new Panel();
        form.setWidth(400);
        form.setHeight(300);
        form.setTitle("Organization Settings");

        form.setLayout(new BorderLayout());

        Panel mainForm = new Panel();
        mainForm.setLayout(new FitLayout());

        BorderLayoutData mainFormData = new BorderLayoutData(RegionPosition.CENTER);
        mainFormData.setMargins(new Margins(5, 5, 5, 5));
        mainFormData.setCMargins(new Margins(5, 5, 5, 5));
        form.add(mainForm, mainFormData);

        Panel buttons = new Panel();
        buttons.setCollapsible(false);
        buttons.setLayout(new HorizontalLayout(5));

        Button prev = new Button("Prev");
        prev.addListener(new ButtonListenerAdapter() {

            public void onClick(Button button, EventObject e) {
                CompanyWizard.this.requestUri("prev");
            }

        });
        buttons.add(prev);
        Button next = new Button("Next");
        next.addListener(new ButtonListenerAdapter() {

            public void onClick(Button button, EventObject e) {
                CompanyWizard.this.requestUri("next");
            }

        });
        buttons.add(next);
        buttons.add(new Button("Finish"));

        BorderLayoutData buttonsData = new BorderLayoutData(RegionPosition.SOUTH);
        buttonsData.setSplit(false);
        buttonsData.setMargins(new Margins(0, 0, 0, 0));
        buttonsData.setCMargins(new Margins(0, 0, 0, 0));
        form.add(buttons, buttonsData);

        RootPanel.get().add(form);
    }

    public void requestUri(String direction) {
        RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, URL.encode(NAV));
        builder.setHeader("Content-type",  "application/x-www-form-urlencoded");
        builder.setRequestData(Format.format("stepId={0}&direction={1}", "company", direction));
        builder.setCallback(new RequestCallback() {

            public void onError(Request request, Throwable exception) {
                Window.alert(exception.getMessage());
            }

            public void onResponseReceived(Request request, Response response) {
                if (200 == response.getStatusCode()) {
                    JavaScriptObject jsObj = JSON.decode(response.getText());
                    JSONObject jsonObj = new JSONObject(jsObj);
                    JSONValue nextAction = jsonObj.get("nextAction");
                    if (nextAction != null && !"null".equals(nextAction.toString())) {
                        Window.Location.replace(nextAction.isString().stringValue());
                    } else {
                        Window.alert("No way!");
                    }
                } else {
                    // Handle the error.  Can get the status text from response.getStatusText()
                    Window.alert("Error: " + response.getStatusText());
                }
            }
        });

        try {
            builder.send();
        } catch (RequestException e) {
            Window.alert(e.getMessage());
        }
    }

}
