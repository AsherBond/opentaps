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
package org.opentaps.gwt.warehouse.inventory.client;

import org.opentaps.gwt.common.client.BaseEntry;
import org.opentaps.gwt.warehouse.inventory.client.form.TraceInventoryApp;

import com.google.gwt.user.client.ui.RootPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Entry extends BaseEntry {

    private static final String TRACK_INVENTORY = "trackInventoryForm";

    /**
     * This is the entry point method.
     * It is loaded for page where the meta tag is found
     */
    public void onModuleLoad() {

        if (RootPanel.get(TRACK_INVENTORY) != null) {
            loadTraceInventoryApplication();
        }
    }

    private void loadTraceInventoryApplication() {
        TraceInventoryApp inventoryTracker = new TraceInventoryApp();
        RootPanel.get(TRACK_INVENTORY).add(inventoryTracker.getMainPanel());
    }  
}
