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

package org.opentaps.gwt.common.client.listviews;

/**
 * The base class for tables that list entities and that support AJAX
 * sorting, pagination, and filtering.
 * This is just an <code>EntityEditableListView</code> with <code>setEditable(false)</code> and <code>setUsePagingToolbar(true)</code>.
 */
public abstract class EntityListView extends EntityEditableListView {

    /**
     * Default constructor.
     */
    public EntityListView() {
        this(null);
    }

    /**
     * Constructor giving a title for this list view, which is displayed in the UI.
     * @param title the title of the list
     */
    public EntityListView(String title) {
        super(title);

        setEditable(false);
        setUsePagingToolbar(true);
    }
}
