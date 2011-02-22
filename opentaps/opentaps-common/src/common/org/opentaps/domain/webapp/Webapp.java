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

import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.opentaps.base.entities.OpentapsWebApps;

/**
 * A webapp webapp object.
 */
public class Webapp extends OpentapsWebApps implements WebElementInterface {

    private boolean isDisabled = false;
    private boolean isHidden = false;

    /**
     * Default constructor.
     */
    public Webapp() {
        super();
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
        return isHidden;
    }
    /** {@inheritDoc} */
    public void setHidden(boolean isHidden) {
        this.isHidden = isHidden;
    }

    /** {@inheritDoc} */
    @Override
    protected void postInit() {
        // set a default value to isHidden according to the hide DB field
        if ("Y".equalsIgnoreCase(this.getHide())) {
            isHidden = true;
        }
    }

    /** {@inheritDoc} */
    public void expandFields(Map<String, Object> context) {
        if (UtilValidate.isNotEmpty(getLinkUrl())) {
            setLinkUrl(FlexibleStringExpander.expandString(getLinkUrl(), context, Locale.getDefault()));
        }
    }
}
