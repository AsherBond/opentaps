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

import com.gwtext.client.core.EventObject;
import com.gwtext.client.data.Record;
import com.gwtext.client.widgets.Button;
import com.gwtext.client.widgets.event.ButtonListenerAdapter;
import com.gwtext.client.widgets.form.Hidden;
import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.form.base.BaseFormPanel;
import org.opentaps.gwt.common.client.lookup.UtilLookup;

/**
 * Extension of BaseFormPanel to support CUD actions.
 */
public class CreateOrUpdateEntityForm extends BaseFormPanel {

    // Buttons for the basic actions
    // - new will reset the form to the create record mode
    private final Button newButton;
    // - submit the form to create a record
    private final Button createButton;
    // - submit the form to update a record
    private final Button updateButton;
    // - submit the form to delete a record
    private final Button deleteButton;

    // special field to specify which action should be performed server side
    private final Hidden CUDActionInput;

    // track the current CUD mode
    private Mode currentMode;

    // should we show the action buttons
    private boolean useButtons;

    /** The current mode the form is in. */
    public static enum Mode {
        /** To create a new entity. */
        CREATE,
        /** To update an existing entity. */
        UPDATE,
        /** To delete an existing entity. */
        DELETE
    }

    /**
     * Constructor.
     */
    public CreateOrUpdateEntityForm() {
        this(true);
    }

    /**
     * Constructor.
     * @param useButtons is set to false, no buttons is added to the form
     */
    public CreateOrUpdateEntityForm(boolean useButtons) {
        super();
        this.useButtons = useButtons;
        currentMode = Mode.CREATE;

        CUDActionInput = new Hidden(UtilLookup.PARAM_CUD_ACTION, UtilLookup.PARAM_CUD_ACTION_CREATE);
        add(CUDActionInput);

        // add the four buttons, only the create button is visible in create mode
        createButton = new Button(UtilUi.MSG.commonAdd(), new ButtonListenerAdapter() {
                @Override public void onClick(Button button, EventObject e) {
                    create();
                }
            });
        if (this.useButtons) {
            addButton(createButton);
        }

        newButton = new Button(UtilUi.MSG.commonNew(), new ButtonListenerAdapter() {
                @Override public void onClick(Button button, EventObject e) {
                    startCreate();
                }
            });
        if (this.useButtons) {
            newButton.hide();
            addButton(newButton);
        }

        updateButton = new Button(UtilUi.MSG.commonUpdate(), new ButtonListenerAdapter() {
                @Override public void onClick(Button button, EventObject e) {
                    update();
                }
            });
        if (this.useButtons) {
            updateButton.hide();
            addButton(updateButton);
        }

        deleteButton = new Button(UtilUi.MSG.commonDelete(), new ButtonListenerAdapter() {
                @Override public void onClick(Button button, EventObject e) {
                    delete();
                }
            });
        if (this.useButtons) {
            deleteButton.hide();
            addButton(deleteButton);
        }
    }

    /**
     * Overrides the submit to call the action according to the current mode.
     * This allows submit on enter to work properly.
     */
    @Override public void submit() {
        switch (currentMode) {
        case UPDATE:
            update();
            break;
        case DELETE:
            delete();
            break;
        default:
            create();
            break;
        }
    }

    /**
     * Submit the form with action set to Create.
     */
    public void create() {
        CUDActionInput.setValue(UtilLookup.PARAM_CUD_ACTION_CREATE);
        getForm().submit();
    }

    /**
     * Submit the form with action set to Update.
     */
    public void update() {
        CUDActionInput.setValue(UtilLookup.PARAM_CUD_ACTION_UPDATE);
        getForm().submit();
    }

    /**
     * Submit the form with action set to Delete.
     */
    public void delete() {
        currentMode = Mode.DELETE;
        CUDActionInput.setValue(UtilLookup.PARAM_CUD_ACTION_DELETE);
        getForm().submit();
    }

    private void setEditMode() {
        currentMode = Mode.UPDATE;
        if (this.useButtons) {
            createButton.hide();
            newButton.show();
            updateButton.show();
            deleteButton.show();
        }
    }

    private void setCreateMode() {
        currentMode = Mode.CREATE;
        if (this.useButtons) {
            createButton.show();
            newButton.hide();
            updateButton.hide();
            deleteButton.hide();
        }
    }

    /**
     * Sets the form into Update mode and loads the record in the form.
     * Switches the relevant buttons: hides Create, shows New, Update, Delete.
     * @param record the <code>Record</code> to load
     */
    public void startEdit(Record record) {
        super.loadRecord(record);
        setEditMode();
    }

    /**
     * Sets the form into Create mode with a reset form.
     * Switches the relevant buttons: shows Create, hides New, Update, Delete.
     */
    public void startCreate() {
        reset();
        setCreateMode();
    }

    /**
     * Sets the form into Create mode with a record values.
     * Switches the relevant buttons: shows Create, hides New, Update, Delete.
     * @param record the <code>Record</code> to load
     */
    public void startCreate(Record record) {
        super.loadRecord(record);
        setCreateMode();
    }

    /**
     * Gets the form current <code>Mode</code>.
     * @return a <code>Mode</code> value
     */
    public Mode getCurrentMode() {
        return currentMode;
    }

    /**
     * Checks the given <code>Record</code> and returns whether it is for Edit or Create.
     * Returns Edit mode by default, override to differentiate records according to the context.
     * @param record the <code>Record</code>
     * @return <code>true</code> if for edit, <code>false</code> for create
     */
    public boolean isRecordForUpdate(Record record) {
        return true;
    }

    /**
     * {@inheritDoc}
     * Loads the <code>Record</code> in the form, sets the form to Edit or Create according to <code>isRecordForUpdate</code>.
     * @param record the <code>Record</code> being loaded
     */
    @Override public void loadRecord(Record record) {
        super.loadRecord(record);
        if (isRecordForUpdate(record)) {
            startEdit(record);
        } else {
            startCreate(record);
        }
    }

    // revert in create mode with a clean form after any action is performed successfully
    @Override protected void onSuccess() {
        startCreate();
    }

}
