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

import com.google.gwt.user.client.ui.Widget;
import com.gwtext.client.core.RegionPosition;
import com.gwtext.client.widgets.BoxComponent;
import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.event.PanelListenerAdapter;
import com.gwtext.client.widgets.layout.BorderLayout;
import com.gwtext.client.widgets.layout.BorderLayoutData;
import com.gwtext.client.widgets.layout.FitLayout;
import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.events.LoadableListenerAdapter;
import org.opentaps.gwt.common.client.listviews.EntityEditableListView;

/**
 * Base class for panels composed of a list and a form.
 * @param <TFORM> the form class that is contained in this widget
 * @param <TLIST> the list view class that is contained in this widget
 */
public abstract class ListAndFormPanel<TFORM extends BaseFormPanel, TLIST extends EntityEditableListView> {

    private static final String MODULE = ListAndFormPanel.class.getName();

    private Integer labelLength = UtilUi.LABEL_LENGTH;
    private Integer inputLength = UtilUi.INPUT_LENGTH;
    private Integer formWidth = UtilUi.FORM_CONTAINER_WIDTH;
    private Integer listWidth = UtilUi.LIST_CONTAINER_WIDTH;
    public Integer listAndFormSpacing = UtilUi.CONTAINERS_VERTICAL_SPACING;
    private Integer minHeight = 0;

    private final Panel mainPanel;
    private final Panel mainListContainer;
    private final Panel mainFormContainer;
    private Panel spacer = null;
    private TLIST listView;
    private TFORM mainForm;
    private Layout currentLayout;

    /** Selects the layout for the container. */
    public static enum Layout {
        /** Puts the form on top of the list. */
        FORM_ON_TOP,
        /** Puts the list on top of the form. */
        LIST_ON_TOP,
        /** Puts the form on the left as a collapsible element. */
        FORM_ON_LEFT_COLLAPSIBLE
    }

    /**
     * Constructor with form on top.
     */
    public ListAndFormPanel() {
        this(Layout.FORM_ON_TOP);
    }

    /**
     * Constructor.
     * @param layout select the layout for the container
     */
    public ListAndFormPanel(Layout layout) {
        currentLayout = layout;

        // contain the form panel and the entity list
        // with a vertical layout and spacing between the list and the form
        mainPanel = new Panel();
        mainPanel.setBorder(false);

        mainFormContainer = new Panel();
        mainFormContainer.setWidth(getFormWidth());
        mainFormContainer.setBorder(false);
        mainFormContainer.setFrame(true);
        mainFormContainer.setCollapsible(true);

        mainListContainer = new Panel();
        mainListContainer.setBorder(false);

        switch (currentLayout) {
        case FORM_ON_LEFT_COLLAPSIBLE:
            mainPanel.setLayout(new BorderLayout());
            BorderLayoutData left = new BorderLayoutData(RegionPosition.WEST);
            left.setFloatable(false);
            left.setSplit(true);
            left.setCollapseModeMini(true);
            mainFormContainer.setLayout(new FitLayout());
            mainPanel.add(mainFormContainer, left);
            break;
        case LIST_ON_TOP:
            mainPanel.add(mainListContainer);
            mainPanel.add(makeVerticalSpacer(getListAndFormSpacing()));
            mainPanel.add(mainFormContainer);
            break;
        default:
            mainPanel.add(mainFormContainer);
            mainPanel.add(makeVerticalSpacer(getListAndFormSpacing()));
            mainPanel.add(mainListContainer);
            break;
        }
    }

    private Panel makeVerticalSpacer(int size) {
        spacer = new Panel();
        spacer.setBorder(false);
        spacer.setHeight(size);
        spacer.setBodyStyle("background:inherit");
        return spacer;
    }

    /**
     * Sets the form title.
     * @param formContainerTitle the title for the form container
     */
    public void setFormTitle(String formContainerTitle) {
        mainFormContainer.setTitle(formContainerTitle);
    }

    /**
     * Sets the container minimal height.
     * @param minHeight the new minimal height
     */
    public void setMinHeight(Integer minHeight) {
        this.minHeight = minHeight;
    }

    /**
     * Sets the label length used in this widget.
     * @param length the label length used in this widget
     */
    public void setLabelLength(Integer length) {
        labelLength = length;
        if (getMainForm() != null) {
            getMainForm().setLabelWidth(getLabelLength());
        }
    }

    /**
     * Gets the label length used in this widget.
     * @return the label length used in this widget
     */
    public Integer getLabelLength() {
        return labelLength;
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

    /**
     * Sets the spacing between the list and the form used in this widget.
     * @param spacing the spacing between the list and the form used in this widget
     */
    public void setListAndFormSpacing(Integer spacing) {
        listAndFormSpacing = spacing;
    }

    /**
     * Gets the spacing between the list and the form used in this widget.
     * @return the spacing between the list and the form used in this widget
     */
    public Integer getListAndFormSpacing() {
        return listAndFormSpacing;
    }

    /**
     * Sets the form width used in this widget.
     * @param width the form width used in this widget
     */
    public void setFormWidth(Integer width) {
        formWidth = width;
        if (getMainForm() != null) {
            getMainForm().setWidth(getFormInnerWidth());
        }
        if (getMainFormPanel() != null) {
            getMainFormPanel().setWidth(getFormWidth());
        }
    }

    /**
     * Gets the form width used in this widget.
     * @return the form width used in this widget
     */
    public Integer getFormWidth() {
        return formWidth;
    }

    /**
     * Gets the form inner width used in this widget.
     * @return the form inner width used in this widget
     */
    public Integer getFormInnerWidth() {
        return formWidth - UtilUi.FORM_PADDING;
    }

    /**
     * Gets the list width used in this widget.
     * @return the list width used in this widget
     */
    public Integer getListWidth() {
        return listWidth;
    }

    /**
     * Gets the spacer panel.
     * @return the spacer <code>Panel</code>
     */
    public final Panel getSpacerPanel() {
        return this.spacer;
    }

    /**
     * Gets the main panel.
     * @return the main <code>Panel</code>
     */
    public final Panel getMainPanel() {
        return this.mainPanel;
    }

    /**
     * Gets the main form panel.
     * @return the main form <code>Panel</code>
     */
    public final Panel getMainFormPanel() {
        return this.mainFormContainer;
    }

    /**
     * Adds the main form to this widget and integrate it to its layout.
     * @param mainForm a <code>BaseFormPanel</code>
     */
    protected void addMainForm(TFORM mainForm) {
        mainForm.setFrame(false);
        mainForm.setBorder(false);
        mainForm.setLabelWidth(getLabelLength());
        mainForm.setWidth(getFormInnerWidth());
        mainFormContainer.add(mainForm);
        this.mainForm = mainForm;
        if (currentLayout == Layout.FORM_ON_LEFT_COLLAPSIBLE) {
            mainFormContainer.setWidth(getFormWidth());
            mainFormContainer.setAnimCollapse(false);
            mainFormContainer.collapse();
            setResizeHandlers(mainForm, "mainForm");
        }
    }

    /**
     * Gets the main form.
     * @return the <code>TFORM</code>
     */
    public TFORM getMainForm() {
        return mainForm;
    }

    /**
     * Adds the list view to this widget and integrate it to its layout.
     * This method should be called from inherited classes.
     * @param listView an <code>EntityListView</code>
     */
    protected void addListView(final TLIST listView) {
        this.listView = listView;
        if (currentLayout == Layout.FORM_ON_LEFT_COLLAPSIBLE) {
            listView.addLoadableListener(new LoadableListenerAdapter() {
                    @Override public void onLoad() {
                        UtilUi.logDebug("List loaded", MODULE, "onLoad");
                        // a hack to force proper display when minHeight is used
                        if (minHeight > 0) {
                            mainPanel.setHeight(1);
                        }
                        resize();
                    }
                });
            setResizeHandlers(listView, "listView");
            BorderLayoutData center = new BorderLayoutData(RegionPosition.CENTER);
            mainPanel.add(listView, center);
        } else {
            mainListContainer.add((Widget) this.listView);
        }
    }

    /**
     * Gets the entity list view.
     * @return the <code>EntityListView</code>
     */
    public TLIST getListView() {
        return this.listView;
    }

    // needed when using the BorderLayout because the main container would not resize automatically according to the list height
    // adjust the main container height to fit both the form and the list
    private void resize() {
        int h = mainForm.getHeight();
        int h2 = listView.getHeight();
        int mainHeight = mainPanel.getHeight();
        UtilUi.logDebug("Main panel height = " + mainHeight + " form Height = " + h + " list height = " + h2, MODULE, "resize");
        int targetHeight = mainHeight;
        if (h > h2) {
            targetHeight = h;
        } else {
            targetHeight = h2;
        }
        if (targetHeight < minHeight) {
            UtilUi.logDebug("Target height less than configured minimum height " + minHeight, MODULE, "resize");
            targetHeight = minHeight;
        }
        if (mainHeight != targetHeight && !(targetHeight < mainHeight)) {
            UtilUi.logDebug("Setting Main panel height to " + targetHeight, MODULE, "resize");
            mainPanel.setHeight(targetHeight + mainPanel.getFrameHeight());
        } else {
            UtilUi.logDebug("Main panel height already set to " + mainHeight, MODULE, "resize");
        }
    }

    // needed when using the BorderLayout because the main container would not resize automatically according to the list height
    private void setResizeHandlers(Panel panel, final String name) {
        panel.addListener(new PanelListenerAdapter() {
                @Override public void onResize(BoxComponent component, int adjWidth, int adjHeight, int rawWidth, int rawHeight) {
                    UtilUi.logDebug("onResize set panel [" + name + "] height to " + rawHeight, MODULE, "onResize");
                    resize();
                }
            });
    }
}
