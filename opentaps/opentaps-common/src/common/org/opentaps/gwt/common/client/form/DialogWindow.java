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

import java.util.ArrayList;
import java.util.List;

import org.opentaps.gwt.common.client.UtilUi;

import com.gwtext.client.core.EventObject;
import com.gwtext.client.core.Position;
import com.gwtext.client.widgets.Button;
import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.Window;
import com.gwtext.client.widgets.event.ButtonListenerAdapter;
import com.gwtext.client.widgets.layout.FitLayout;

public abstract class DialogWindow extends Window {

    Panel innerPanel = new Panel();

    List<DialogCallbackInterface> finishingCallbacks = new ArrayList<DialogCallbackInterface>(); 

    public DialogWindow() {
        super();
    }

    public DialogWindow(String title, int width, int height) {
        super(title, width, height, true, false);
    }

    public DialogWindow(String title) {
        super(title);
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
                if (canClose()) {
                    hide();

                    // fire onOk event
                    for (DialogCallbackInterface handler : finishingCallbacks) {
                        handler.onOk();
                    }
                }
            }
        });
        addButton(okButton);

        Button cancelButton = new Button(UtilUi.MSG.opentapsCancel());
        cancelButton.addListener(new ButtonListenerAdapter() {
            @Override
            public void onClick(Button button, EventObject e) {
                super.onClick(button, e);
                hide();

                // fire onCancel event
                for (DialogCallbackInterface handler : finishingCallbacks) {
                    handler.onCancel();
                }
            }
        });
        addButton(cancelButton);

        // setup inner form
        innerPanel.setPaddings(15);
        innerPanel.setBaseCls("x-plain");

        // pass control to subclass to add functional code and widgets.
        initFields(innerPanel);

    }

    /**
     * Provide implementation for this method. Add to container and initialize the fields.
     * Don't add action buttons with meaning Ok & Cancel. They are already there.
     *
     * @param container instance of <code>Panel</code> where you can add the fields.
     */
    protected abstract void initFields(Panel container);

    /**
     * Instance of any DialogWindow subclass has to be created in two steps.
     * <code>
     *   SomeWindow window = new SomeWindow(...);
     *   window.create();
     * </code>
     */
    public void create() {
        add(innerPanel);
    }

    /**
     * Called when button <code>Ok</code> is pressed to ensure dialog window can be closed.<br/>
     * Rewrite this method if you need store values of dialog widgets into variables.
     * 
     * @return <code>true</code> if window can be closed.
     */
    protected boolean canClose() {
        return true;
    }

    /**
     * Add dialog end listener.
     * @param handler instance of <code>DialogCallbackInterface</code>
     */
    public void addDialogListener(DialogCallbackInterface handler) {
        finishingCallbacks.add(handler);
    }

    /**
     * Remove dialog end listener.
     * @param handler instance of <code>DialogCallbackInterface</code>
     */
    public void removeDialogListener(DialogCallbackInterface handler) {
        if (finishingCallbacks.contains(handler)) {
            finishingCallbacks.remove(handler);
        }
    }
}
