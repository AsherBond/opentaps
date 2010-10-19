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
