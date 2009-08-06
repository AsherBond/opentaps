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

import org.opentaps.gwt.common.client.UtilUi;

/**
 * Provides utility methods to build a <code>FormPanel</code> to be presented as a screenlet.
 */
public class ScreenletFormPanel extends BaseFormPanel {

    /**
     * Constructor giving the <code>FormPanel</code> label position.
     * @param formPosition a <code>Position</code> value
     * @param title a <code>String</code> value
     */
    public ScreenletFormPanel(final Position formPosition, final String title) {
        super(formPosition);
        setTitle(title);
        setBaseCls(UtilUi.SCREENLET_STYLE);
        setTabCls(UtilUi.SCREENLET_HEADER_STYLE);
        setBodyStyle(UtilUi.SCREENLET_BODY_STYLE);
        setCollapsible(true);
    }

    /**
     * Default Constructor, defaults to labels positioned on the left side of the inputs and aligned on the right.
     * @param title a <code>String</code> value
     */
    public ScreenletFormPanel(final String title) {
        this(Position.RIGHT, title);
    }
}
