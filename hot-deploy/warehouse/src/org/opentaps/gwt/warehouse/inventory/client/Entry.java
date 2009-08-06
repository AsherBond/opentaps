/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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
