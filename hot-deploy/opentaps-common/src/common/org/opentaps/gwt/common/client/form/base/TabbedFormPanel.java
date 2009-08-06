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

package org.opentaps.gwt.common.client.form.base;

import com.gwtext.client.core.Position;
import com.gwtext.client.widgets.TabPanel;

/**
 * Provides utility methods to build a tabbed <code>FormPanel</code>.
 */
public class TabbedFormPanel extends BaseFormPanel {

    private final TabPanel tabPanel;
    private int innerPadding = 15;

    /**
     * Constructor giving the <code>FormPanel</code> label position.
     * @param formPosition a <code>Position</code> value
     */
    public TabbedFormPanel(final Position formPosition) {
        super(formPosition);

        tabPanel = new TabPanel();
        tabPanel.setActiveTab(0);
        tabPanel.setResizeTabs(true);
        add(tabPanel);
    }

    /**
     * Default Constructor, defaults to labels positioned on the left side of the inputs and aligned on the right.
     */
    public TabbedFormPanel() {
        this(Position.RIGHT);
    }

    /**
     * Gets the <code>TabPanel</code> for direct access.
     * @return a <code>TabPanel</code> value
     */
    public TabPanel getTabPanel() {
        return tabPanel;
    }

    /**
     * Describe <code>addTab</code> method here.
     * @param title a <code>String</code> value
     * @return a <code>Panel</code> value
     */
    public SubFormPanel addTab(String title) {
        SubFormPanel p = new SubFormPanel(this);
        p.setTitle(title);
        p.setBorder(false);
        p.setAutoHeight(true);
        p.setPaddings(innerPadding);

        tabPanel.add(p);

        return p;
    }

    /**
     * Gets the tabbed form panel inner padding, which is the padding used inside each tab.
     * @return an <code>int</code> value
     */
    public int getInnerPadding() {
        return innerPadding;
    }

    /**
     * Sets the tabbed form panel inner padding, which is the padding used inside each tab.
     * This setting does not affect already added tabs.
     * @param padding an <code>int</code> value
     */
    public void setInnerPadding(int padding) {
        innerPadding = padding;
    }
}
