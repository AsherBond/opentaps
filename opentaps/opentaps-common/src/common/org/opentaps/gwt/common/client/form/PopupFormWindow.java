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
package org.opentaps.gwt.common.client.form;

import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.form.base.BaseFormPanel;

import com.gwtext.client.core.EventObject;
import com.gwtext.client.core.Position;
import com.gwtext.client.widgets.Button;
import com.gwtext.client.widgets.Window;
import com.gwtext.client.widgets.event.ButtonListenerAdapter;
import com.gwtext.client.widgets.layout.FitLayout;


/**
 * <code>PopupFormWindow</code> is specialized window that contains form
 * in client area and used for focused communication with the user.
 *
 * This class is abstract but force uniform look and behavior. In order to
 * create popup form we have to create its subclass and provide implementation
 * for initFields() method at least.
 *
 * Client area is instance of <code>BaseFormPanel</code>.
 *
 * @see org.opentaps.gwt.common.client.form.base.BaseFormPanel
 */
public abstract class PopupFormWindow extends Window {

    protected final BaseFormPanel innerPanel = new BaseFormPanel(Position.LEFT) {

            @Override
            public void onSuccess() {
                PopupFormWindow.this.onSuccess();
            }

            @Override
            public void onFailure() {
                PopupFormWindow.this.onFailure();
            }
        };

    private int height = 0;
    private int width  = 0;

    /**
     * Constructor that creates a windows with given size and title.
     *
     * @param title the string that appears in header of the window.
     * @param width window width in pixels.
     * @param height window height in pixels.
     */
    public PopupFormWindow(String title, int width, int height) {
        super(title, width, height);
        this.width = width;
        this.height = height;
    }

    /**
     * Constructor that creates a windows with given size and title.
     * Also this constructor allows select if this window is modal or has
     * resizable border.
     *
     * @param title the string that appears in header of the window.
     * @param width window width in pixels.
     * @param height window height in pixels.
     * @param modal <code>true</code> when the window has to be model.
     * @param resizable <code>true</code> when the window has to allow resizing.
     */
    public PopupFormWindow(String title, int width, int height, boolean modal, boolean resizable) {
        super(title, width, height, modal, resizable);
        this.width = width;
        this.height = height;
    }

    /** {@inheritDoc} */
    @Override
    protected void initComponent() {
        super.initComponent();

        // setup window properties
        setModal(true);
        setResizable(false);
        setLayout(new FitLayout());
        setPaddings(5);
        setButtonAlign(Position.RIGHT);

        setCloseAction(Window.HIDE);
        setPlain(false);

        setDefaultButton(0);

        // add action handler to buttons
        Button okButton = new Button(UtilUi.MSG.opentapsOk());
        okButton.addListener(new ButtonListenerAdapter() {
            @Override
            public void onClick(Button button, EventObject e) {
                super.onClick(button, e);
                innerPanel.getForm().submit();
            }
        });
        addButton(okButton);

        Button cancelButton = new Button(UtilUi.MSG.opentapsCancel());
        cancelButton.addListener(new ButtonListenerAdapter() {
            @Override
            public void onClick(Button button, EventObject e) {
                super.onClick(button, e);
                hide();
            }
        });
        addButton(cancelButton);

        // setup inner form
        innerPanel.setPaddings(15);
        innerPanel.setBaseCls("x-plain");
        if (width > 0) {
            innerPanel.setWidth(width);
        }
        if (height > 0) {
            innerPanel.setHeight(height);
        }

        // pass control to subclass to add functional code and widgets.
        initFields(innerPanel);

    }

    /**
     * Provide implementation for this method. Add to container and initialize the fields.
     * Don't add action buttons with meaning Ok & Cancel. They are already there.
     *
     * @param container instance of <code>BaseFormPanel</code> where you can add the fields.
     */
    protected abstract void initFields(BaseFormPanel container);

    /**
     * Instance of any PopupFormWindow subclass has to be created in two steps.
     * <code>
     *   SomeFormWindow window = new SomeFormWindow(...);
     *   window.create();
     * </code>
     */
    public void create() {
        add(innerPanel);
    }

    /**
     * This event handler is called when an form action is completed successfully.
     * Should be re-implemented in subclass to add application logic.
     */
    protected void onSuccess() {
        hide();
    }

    /**
     * This event handler is called when an form action fails.
     * Should be re-implemented in subclass to add application logic.
     */
    protected void onFailure() { }

    /**
     * Register a widget that should be notified once the form has been successfully submitted.
     * @param widget a <code>FormNotificationInterface</code>
     */
    public void register(FormNotificationInterface<Void> widget) {
        innerPanel.register(widget);
    }

}
