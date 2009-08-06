/*
 * Copyright (c) 2009 Open Source Strategies, Inc.
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
package org.opentaps.gwt.crmsfa.cases.client;

import org.opentaps.gwt.common.client.BaseEntry;
import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.crmsfa.cases.client.form.QuickNewCaseForm;

import com.google.gwt.user.client.ui.RootPanel;

public class Entry extends BaseEntry {

	private static final String QUICK_CREATE_CASE_ID = "quickNewCase";
	private QuickNewCaseForm quickNewCaseForm = null;
    /**
     * This is the entry point method.
     * It is loaded for page where the meta tag is found.
     */
    public void onModuleLoad() {
    	if (RootPanel.get(QUICK_CREATE_CASE_ID) != null) {
    		loadQuickNewCase();
    	}
    }
    
    private void loadQuickNewCase() {
    	quickNewCaseForm = new QuickNewCaseForm();
        RootPanel.get(QUICK_CREATE_CASE_ID).add(quickNewCaseForm);
    }
}