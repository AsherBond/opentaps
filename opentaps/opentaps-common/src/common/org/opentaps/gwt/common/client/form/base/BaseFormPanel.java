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

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.ui.Widget;
import com.gwtext.client.core.Connection;
import com.gwtext.client.core.EventObject;
import com.gwtext.client.core.Position;
import com.gwtext.client.data.Record;
import com.gwtext.client.widgets.Button;
import com.gwtext.client.widgets.Component;
import com.gwtext.client.widgets.event.ButtonListenerAdapter;
import com.gwtext.client.widgets.form.Field;
import com.gwtext.client.widgets.form.Form;
import com.gwtext.client.widgets.form.FormPanel;
import com.gwtext.client.widgets.form.TextField;
import com.gwtext.client.widgets.form.event.FieldListenerAdapter;
import com.gwtext.client.widgets.form.event.FormListener;
import com.gwtext.client.widgets.form.event.FormPanelListenerAdapter;
import com.gwtext.client.widgets.layout.LayoutData;
import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.form.FormNotificationInterface;
import org.opentaps.gwt.common.client.form.ServiceErrorReader;
import org.opentaps.gwt.common.client.form.field.FieldInterface;
import org.opentaps.gwt.common.client.listviews.EntityEditableListView;
import org.opentaps.gwt.common.client.lookup.UtilLookup;

/**
 * Provides utility methods to build a <code>FormPanel</code>.
 */
public class BaseFormPanel extends FormPanel implements FormListener {

    private static final String MODULE = BaseFormPanel.class.getName();

    private final FieldListenerAdapter submitOnEnterKey;
    private final FieldListenerAdapter fieldChangedListener;
    private List<FormNotificationInterface<?>> registeredWidgets;

    // should the loaded recorded be updated every time a field changes
    private EntityEditableListView syncWithList;
    // track the last loaded record for synchronization
    private Record loadedRecord;
    private int loadedRecordIndex = -1;

    // on some special cases we use the form only has an internal element and not actually display it on screen
    // then another Component can be used as the target of masking / unmasking effects
    private Component effectTarget;

    private boolean syncing = false;

    /**
     * Constructor giving the <code>FormPanel</code> label position.
     * @param formPosition a <code>Position</code> value
     */
    public BaseFormPanel(final Position formPosition) {
        super(formPosition);

        registeredWidgets = new ArrayList<FormNotificationInterface<?>>();

        // set the default timeout from the configuration
        setTimeout(UtilLookup.getAjaxDefaultTimeout());

        // this is used to submit when the user press the enter key
        // instead of having to manually click the button
        submitOnEnterKey = new FieldListenerAdapter() {
                @Override public void onSpecialKey(Field field, EventObject e) {
                    if (e.getKey() == EventObject.ENTER) {
                        submit();
                    }
                }
            };
        // track each filed onChange event, used to sync the data back to a grid if the form is bound to one
        fieldChangedListener = new FieldListenerAdapter() {
                @Override public void onChange(Field field, Object newVal, Object oldVal) {
                    onFieldChange(field);
                }
            };
        addFormListener(this);
        effectTarget = this;
        setMethod(Connection.POST);

        // copy parameters value from the HTTP request in the corresponding fields
        addListener(new FormPanelListenerAdapter() {
                @Override public void onRender(Component c) {
                    for (Field f : getFields()) {
                        UtilUi.getAndSetUrlParameter(f);
                    }
                }
            });
    }

    /**
     * Default Constructor, defaults to labels positioned on the left side of the inputs and aligned on the right.
     */
    public BaseFormPanel() {
        this(Position.RIGHT);
    }

    /**
     * Sets the <code>Component</code> that should be the target of the form special effects (masking, unmasking, activity indicator).
     * On some special cases we use the form only has an internal element and not actually display it on screen
     *  then another <code>Component</code> must be used as the target.
     *
     * @param target a <code>Component</code> value
     */
    public void setEffectsTarget(Component target) {
        effectTarget = target;
    }

    /**
     * Sets the bind list view so the form can synchronize the changes.
     * Data changed in this form will be synchronized back to the corresponding grid row.
     * @param list an <code>EntityEditableListView</code> value
     */
    public void setBindedList(EntityEditableListView list) {
        syncWithList = list;
    }

    /**
     * Default submit action, submits the form, this should be overridden if the form is not meant to be POSTed.
     */
    public void submit() {
        getForm().submit();
    }

    /**
     * Resets the form and stops the Record synchronization with field change.
     */
    public void reset() {
        loadedRecord = null;
        getForm().reset();
    }

    /**
     * Loads a <code>Record</code> in the form.
     * @param record the <code>Record</code> being loaded
     */
    public void loadRecord(Record record) {
        // special case of summary record
        UtilUi.logDebug("Loading record: " + UtilUi.toString(record), MODULE, "loadRecord");
        if (UtilUi.isSummary(record)) {
            UtilUi.logWarning("Loaded a summary record, should not be editable in this form.", MODULE, "loadRecord");
            loadedRecord = null;
            loadedRecordIndex = -1;
            hide();
        } else {
            loadedRecord = record;
            getForm().loadRecord(record);
            show();
        }
    }

    /**
     * Loads a <code>Record</code> in the form.
     * @param record the <code>Record</code> being loaded
     * @param listStoreIndex the index in the list store, in case of form/list binding
     */
    public final void loadRecord(Record record, int listStoreIndex) {
        loadedRecordIndex = listStoreIndex;
        loadRecord(record);
    }

    /**
     * Fired after a field was changed in this form, the default implementation synchronizes the changes with the loaded record.
     * @param field the <code>Field</code>
     */
    public void onFieldChange(Field field) {
        if (syncing) {
            return;
        }

        if (syncWithList != null && loadedRecord != null && loadedRecordIndex >= 0) {
            UtilUi.logInfo("Field [" + field.getName() + "] changed and require sync", MODULE, "onFieldChange");
            // avoid the syncing to trigger the on change events
            syncing = true;
            getForm().updateRecord(loadedRecord);
            syncWithList.updateRecord(loadedRecordIndex, loadedRecord);
            syncing = false;
        }
    }

    /**
     * Gets the <code>FieldListenerAdapter</code> for handling the enter key.
     * @return a <code>FieldListenerAdapter</code> value
     */
    public FieldListenerAdapter getSubmitOnEnterHandler() {
        return submitOnEnterKey;
    }

    /**
     * Creates a button with the given label with its default handler set to submit the form without adding it to the form.
     * @param label the button label
     * @return the added <code>Button</code>, for further configuration
     * @see #addStandardSubmitButton
     */
    public Button makeStandardSubmitButton(String label) {
        Button createButton = new Button(label, new ButtonListenerAdapter() {
                @Override public void onClick(Button button, EventObject e) {
                    submit();
                }
            });
        return createButton;
    }

    /**
     * Creates and adds a button with the given label with its default handler set to submit the form.
     * @param label the button label
     * @return the added <code>Button</code>, for further configuration if needed
     * @see #makeStandardSubmitButton
     */
    public Button addStandardSubmitButton(String label) {
        Button createButton = makeStandardSubmitButton(label);
        addButton(createButton);
        return createButton;
    }

    /**
     * Sets the form standard listeners (submit on enter, ...) to the given <code>FieldInterface</code>.
     * Use only when the field was not added with the {@link #addField} method.
     * @param field a <code>FieldInterface</code> value
     */
    public void setFieldListeners(FieldInterface field) {
        field.addListener(submitOnEnterKey);
        field.addListener(fieldChangedListener);
    }

    /**
     * Sets the form standard listeners (submit on enter, ...) to the given <code>Field</code>.
     * Use only when the field was not added with the {@link #addField} method.
     * @param field a <code>Field</code> value
     */
    public void setFieldListeners(Field field) {
        field.addListener(submitOnEnterKey);
        field.addListener(fieldChangedListener);
    }

    /**
     * Adds a field to the form.
     * This also automatically sets the submit on enter event handler.
     * @param field a <code>Field</code> value
     */
    public void addField(Field field) {
        setFieldListeners(field);
        add(field);
    }

    /**
     * Adds a field to the form taking into account given layout data.
     * This also automatically sets the submit on enter event handler.
     * @param field a <code>Field</code> value
     * @param layoutData a <code>LayoutData</code> value
     */
    public void addField(Field field, LayoutData layoutData) {
        setFieldListeners(field);
        add(field, layoutData);
    }

    /**
     * Adds a field to the form.
     * This also automatically sets the submit on enter event handler.
     * @param field a <code>FieldInterface</code> value
     */
    public void addField(FieldInterface field) {
        setFieldListeners(field);
        add((Widget) field);
    }

    /**
     * Adds a required field to the form.
     * This also automatically sets the submit on enter event handler and sets the proper CSS class to the label.
     * @param field a <code>Field</code> value
     */
    public void addRequiredField(Field field) {
        setFieldListeners(field);
        add(field, UtilUi.REQUIRED_FIELD_DATA);
    }

    /**
     * Adds a required field to the form.
     * This also automatically sets the submit on enter event handler.
     * @param field a <code>FieldInterface</code> value
     */
    public void addRequiredField(FieldInterface field) {
        setFieldListeners(field);
        add((Widget) field, UtilUi.REQUIRED_FIELD_DATA);
    }

    /**
     * Adds a required field to the form.
     * This also automatically sets the submit on enter event handler and sets the proper CSS class to the label.
     * @param field a <code>TextField</code> value
     */
    public void addRequiredField(TextField field) {
        field.setAllowBlank(false);
        setFieldListeners(field);
        add(field, UtilUi.REQUIRED_FIELD_DATA);
    }

    /**
     * Checks if this form is valid and is able to be submitted.
     * This default implementation checks that all the fields are valid according to their validation logic.
     * Override this method in sub classes that need more complex validation.
     * @return a <code>boolean</code> value
     */
    public boolean isValid() {
        return getForm().isValid();
    }

    /**
     * Default handler for the Form event that is executed before sending the action.
     * This default implementation calls {@link #isValid()} for the form validation, and then mask the form while displaying a Loading... message.
     * The Form will no be able to respond to any user input until the mask is cleared which is normally done in the {@link #onActionComplete} or {@link #onActionFailed} handlers.
     * @param f the <code>Form</code>
     * @return true if the action should proceed, false if it should be aborted
     */
    public boolean doBeforeAction(Form f) {
        if (!isValid()) {
            return false;
        }
        // disable the form with a special effect and a message indicating something is going on
        effectTarget.getEl().mask(UtilUi.MSG.loading());
        return true;
    }

    /**
     * Default handler for the Form event that is executed on successful completion of the action.
     * This default implementation unmask the form, reset its values and calls {@link #notifySuccess}.
     * @param f the <code>Form</code>
     * @param httpStatus an <code>int</code> value
     * @param responseText a <code>String</code> value
     */
    public void onActionComplete(Form f, int httpStatus, String responseText) {
        effectTarget.getEl().unmask();

        if (showErrorMessageIfAny(httpStatus, responseText)) {
            //if an error occurred do not reset the form
            return;
        }

        reset();
        notifySuccess();
        onSuccess();
    }

    /**
     * Default handler for the Form event that is executed if the action fails.
     * This default implementation unmask the form displays the error message returned by the server in a popup.
     * @param f the <code>Form</code>
     * @param httpStatus an <code>int</code> value
     * @param responseText a <code>String</code> value
     */
    public void onActionFailed(Form f, int httpStatus, String responseText) {
        effectTarget.getEl().unmask();
        showErrorMessageIfAny(httpStatus, responseText);
    }

    /**
     * Checks the server response and show the appropriate error message according to this form context.
     * @param httpStatus the server HTTP response code
     * @param responseText the server response
     * @return <code>true</code> if an error occurred and was displayed to the user
     */
    public boolean showErrorMessageIfAny(int httpStatus, String responseText) {
        return ServiceErrorReader.showErrorMessageIfAny(httpStatus, responseText, this.getAttribute("url"));
    }

    /**
     * Default handler for the Form event that is executed on successful completion of the action and used for inter widget notification.
     * @see #onActionComplete
     */
    public void notifySuccess() {
        for (FormNotificationInterface<?> widget : registeredWidgets) {
            widget.notifySuccess(null);
        }
    }

    /**
     * Placeholder handler for the Form event that is executed on successful submission.
     * This can be overridden to implement extra action that should take place on successful submission.
     * @see #onActionComplete
     */
    protected void onSuccess() { }

    /**
     * Placeholder handler for the Form event that is executed on failed submission.
     * This can be overridden to implement extra action that should take place on failed submission.
     * @see #onActionFailed
     */
    protected void onFailure() { }

    /**
     * Register a widget that should be notified once the form has been successfully submitted.
     * @param widget a <code>FormNotificationInterface</code>
     */
    public void register(FormNotificationInterface<?> widget) {
        registeredWidgets.add(widget);
    }

}
