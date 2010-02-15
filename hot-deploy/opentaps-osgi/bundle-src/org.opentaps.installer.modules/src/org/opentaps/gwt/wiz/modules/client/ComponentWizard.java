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
package org.opentaps.gwt.wiz.modules.client;

import org.opentaps.gwt.common.client.BaseEntry;

import com.google.gwt.user.client.ui.RootPanel;
import com.gwtext.client.core.Margins;
import com.gwtext.client.core.RegionPosition;
import com.gwtext.client.widgets.Button;
import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.layout.BorderLayout;
import com.gwtext.client.widgets.layout.BorderLayoutData;
import com.gwtext.client.widgets.layout.FitLayout;
import com.gwtext.client.widgets.layout.HorizontalLayout;


/**
 *
 *
 */
public class ComponentWizard extends BaseEntry {

    /** {@inheritDoc} */
    public void onModuleLoad() {
        Panel form = new Panel();
        form.setWidth(400);
        form.setHeight(300);
        form.setTitle("Application Components");

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

        buttons.add(new Button("Prev"));
        buttons.add(new Button("Next"));
        buttons.add(new Button("Finish"));

        BorderLayoutData buttonsData = new BorderLayoutData(RegionPosition.SOUTH);
        buttonsData.setSplit(false);
        buttonsData.setMargins(new Margins(0, 0, 0, 0));
        buttonsData.setCMargins(new Margins(0, 0, 0, 0));
        form.add(buttons, buttonsData);

        RootPanel.get().add(form);
    }

}
