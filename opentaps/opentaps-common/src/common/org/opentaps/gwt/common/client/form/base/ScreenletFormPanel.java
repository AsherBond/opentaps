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

package org.opentaps.gwt.common.client.form.base;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.gwtext.client.core.Position;
import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.event.PanelListenerAdapter;
import org.opentaps.base.constants.EnumerationConstants;
import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.services.ViewPreferenceService;

/**
 * Provides utility methods to build a <code>FormPanel</code> to be presented as a screenlet.
 */
public class ScreenletFormPanel extends BaseFormPanel {

    private static final String MODULE = ScreenletFormPanel.class.getName();

    private String lastSyncedState = null;
    private Integer inputLength = UtilUi.SCREENLET_INPUT_LENGTH;

    /**
     * Constructor giving the <code>FormPanel</code> label position.
     * @param formPosition a <code>Position</code> value
     * @param title a <code>String</code> value
     */
    public ScreenletFormPanel(final Position formPosition, final String title) {
        super(formPosition);
        setTitle(title);
        setFrame(true);
        setCollapsible(true);
        setTitleCollapse(true);

        UtilUi.logInfo("has pref type id?", MODULE, "before ViewPreferenceService::get");
        if (getPreferenceTypeId() != null) {
            UtilUi.logInfo("has pref type id: " + getPreferenceTypeId(), MODULE, "before ViewPreferenceService::get");
            setCollapsed(true);
            ViewPreferenceService.get(getPreferenceTypeId(), new AsyncCallback<String>() {
                    public void onSuccess(String result) {
                        UtilUi.logInfo("got result: " + result, MODULE, "ViewPreferenceService::get");
                        if (EnumerationConstants.GwtTglPreference.GWT_EXPANDED.equals(result)) {
                            UtilUi.logInfo("expanding...", MODULE, "ViewPreferenceService::get");
                            lastSyncedState = result;
                            expand();
                        } else {
                            lastSyncedState = EnumerationConstants.GwtTglPreference.GWT_COLLAPSED;
                            collapse();
                        }
                        UtilUi.logInfo("lastSyncedState = " + lastSyncedState, MODULE, "ViewPreferenceService::get");
                    }

                    // assume collapsed
                    public void onFailure(Throwable caught) {
                        UtilUi.logError(caught.toString(), MODULE, "ViewPreferenceService::onFailure");
                    }
                });

            addListener(new PanelListenerAdapter() {
                    @Override public void onCollapse(Panel panel) {
                        saveState(EnumerationConstants.GwtTglPreference.GWT_COLLAPSED);
                    }
                    @Override public void onExpand(Panel panel) {
                        saveState(EnumerationConstants.GwtTglPreference.GWT_EXPANDED);
                    }
                });
        }
    }

    /**
     * Default Constructor, defaults to labels positioned on the left side of the inputs and aligned on the right.
     * @param title a <code>String</code> value
     */
    public ScreenletFormPanel(final String title) {
        this(Position.RIGHT, title);
    }

    private void saveState(String state) {
        if (lastSyncedState != null) {
            final String newState = state;
            UtilUi.logInfo("setting new state : " + newState, MODULE, "ViewPreferenceService::set");
            ViewPreferenceService.set(getPreferenceTypeId(), newState, new AsyncCallback<Void>() {
                    public void onSuccess(Void v) {
                        UtilUi.logInfo("finished successfully", MODULE, "ViewPreferenceService::set");
                        lastSyncedState = newState;
                    }

                    public void onFailure(Throwable caught) {
                        UtilUi.logError(caught.toString(), MODULE, "ViewPreferenceService::onFailure");
                    }
                });
        }
    }


    /**
     * Gets the View preference type for this screenelt, used for synchronizing the toggle state of the screenlet if not <code>null.</code>
     * Reimplement to return non <code>null</code> values.
     * @return the ViewPreference viewPrefTypeId
     */
    public String getPreferenceTypeId() {
        return null;
    }

    /**
     * Sets the input length used in this widget.
     * @param length the input length used in this widget
     */
    public void setInputLength(Integer length) {
        inputLength = length;
    }

    /**
     * Gets the input length used in this widget.
     * @return the input length used in this widget
     */
    public Integer getInputLength() {
        return inputLength;
    }
}
