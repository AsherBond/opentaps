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

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.opentaps.base.entities.OpentapsShortcutGroup;


/**
 * OpentapsShortcutGroup entity.
 */
public class ShortcutGroup extends OpentapsShortcutGroup implements WebElementInterface {

    private List<? extends Shortcut> allowedShortcuts;
    private boolean isDisabled = false;
    private boolean isHidden = false;

    /**
     * Default constructor.
     */
    public ShortcutGroup() {
        super();
    }

    public List<? extends Shortcut> getAllowedShortcuts() {
        return allowedShortcuts;
    }

    public void setAllowedShortcuts(List<? extends Shortcut> allowedShortcuts) {
        this.allowedShortcuts = allowedShortcuts;
    }

    /** {@inheritDoc} */
    public boolean isDisabled() {
        return isDisabled;
    }
    /** {@inheritDoc} */
    public void setDisabled(boolean isDisabled) {
        this.isDisabled = isDisabled;
    }

    /** {@inheritDoc} */
    public boolean isHidden() {
        return isHidden || (isDisabled() && !showAsDisabled());
    }
    /** {@inheritDoc} */
    public void setHidden(boolean isHidden) {
        this.isHidden = isHidden;
    }

    /**
     * Checks if this element should be shown as disabled.
     * Only returns true if is disabled and has it's showIfDisabled field set to "Y".
     * @return a <code>boolean</code> value
     */
    public boolean showAsDisabled() {
        return isDisabled() && "Y".equalsIgnoreCase(this.getShowIfDisabled());
    }

    /** {@inheritDoc} */
    public void expandFields(Map<String, Object> context) {
        if (UtilValidate.isNotEmpty(getUiLabel())) {
            setUiLabel(FlexibleStringExpander.expandString(getUiLabel(), context, Locale.getDefault()));
        }
    }
}
