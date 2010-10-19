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

package org.opentaps.gwt.common.client.form.field;

/**
 * Interface for fields that need to post-process their value.
 * This is needed to properly link the updated value back to a grid record when used
 *  as a cell editor.
 */
public interface ValuePostProcessedInterface {

    /**
     * Gets the post processed value from the old and new value.
     * @param oldValue an <code>Object</code> value
     * @param newValue an <code>Object</code> value
     * @return the new value, or <code>null</code> to indicate the post processing could not succeed (used to cancel the change)
     */
    public String getPostProcessedValue(Object oldValue, Object newValue);
}
