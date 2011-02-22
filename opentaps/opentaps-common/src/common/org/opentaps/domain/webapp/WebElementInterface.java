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
package org.opentaps.domain.webapp;

import java.util.Map;

/**
 * Common interface for the web elements (webapp, tab, shortcut group, shortcut).
 */
public interface WebElementInterface {

    /**
     * Checks if the element is disabled, in which case it may still be displayed as an inactive element.
     * @return a <code>boolean</code> value
     */
    public boolean isDisabled();
    /**
     * Disable or enable the element.
     * @param isDisabled a <code>boolean</code> value
     */
    public void setDisabled(boolean isDisabled);

    /**
     * Checks if the element is hidden, in which case it should not be displayed.
     * @return a <code>boolean</code> value
     */
    public boolean isHidden();
    /**
     * Hide or show the element.
     * @param isHidden a <code>boolean</code> value
     */
    public void setHidden(boolean isHidden);

    /**
     * Expands the fields like URL, labels, etc .. according to the given context.
     * @param context a context <code>Map</code>
     */
    public void expandFields(Map<String, Object> context);

}
