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

import com.gwtext.client.data.Record;
import org.opentaps.gwt.common.client.form.base.ListAndFormPanel;
import org.opentaps.gwt.common.client.listviews.EntityEditableListView;

/**
 * Base class for the common List view + Create or update form pattern.
 * @param <TLIST> the list view class that is contained in this widget
 * @see org.opentaps.gwt.financials.invoices.client.form.InvoiceItemsForm
 */
public abstract class CreateOrUpdateEntityListForm<TLIST extends EntityEditableListView> extends ListAndFormPanel<CreateOrUpdateEntityForm, TLIST> {

    // form used to create / update records
    private final CreateOrUpdateEntityForm mainForm;

    private static final String DEFAULT_CREATE_TITLE = "Create new record";
    private static final String DEFAULT_UPDATE_TITLE = "Update record";

    // title to be used in create mode, defaults to "Create new record" but should be overridden according to the context, ex: "Create new Invoice Item"
    private String createFormTitle;
    // title to be used in update mode, defaults to "Update record" but should be overridden according to the context, ex: "Update Invoice Item"
    private String updateFormTitle;

    /**
     * Constructor.
     */
    public CreateOrUpdateEntityListForm() {
        this(DEFAULT_CREATE_TITLE, DEFAULT_UPDATE_TITLE);
    }

    /**
     * Constructor with static titles.
     * @param createTitle the create title for the form
     * @param updateTitle the update title for the form
     */
    public CreateOrUpdateEntityListForm(String createTitle, String updateTitle) {
        super(ListAndFormPanel.Layout.FORM_ON_LEFT_COLLAPSIBLE);

        this.createFormTitle = createTitle;
        this.updateFormTitle = updateTitle;
        setFormTitle(createFormTitle);

        // override some event handlers in the form
        mainForm = new CreateOrUpdateEntityForm() {
                // set the form in update mode, startEdit can be overridden is more actions are needed
                @Override public void loadRecord(Record record) {
                    super.loadRecord(record);
                    if (getCurrentMode().equals(Mode.UPDATE)) {
                        setFormTitle(getUpdateFormTitle(record));
                    } else {
                        setFormTitle(getCreateFormTitle());
                    }

                }

                // revert in create mode with a clean form after any action is performed successfully
                @Override protected void onSuccess() {
                    super.onSuccess();
                    getMainForm().getForm().reset();
                    setFormTitle(createFormTitle);
                }

                // expose this to this class for overriding
                @Override public boolean isRecordForUpdate(Record record) {
                    return isFormRecordForUpdate(this, record);
                }
            };
        addMainForm(mainForm);
    }

    /**
     * Checks the given <code>Record</code> and returns whether it is for Edit or Create.
     * Returns Edit mode by default, override to differentiate records according to the context.
     * @param form the <code>CreateOrUpdateEntityForm</code>, so you can call it's default implementation <code>isRecordForUpdate</code> instead
     * @param record the <code>Record</code>
     * @return <code>true</code> if for edit, <code>false</code> for create
     */
    protected boolean isFormRecordForUpdate(CreateOrUpdateEntityForm form, Record record) {
        return form.isRecordForUpdate(record);
    }

    /**
     * Gets the string that should be used as the form title when a new record is being created.
     * @return a <code>String</code> that should be used as the form title
     */
    protected String getCreateFormTitle() {
        return createFormTitle;
    }

    /**
     * Gets the string that should be used as the form title when a record is loaded.
     * Defaults to "Update record", but should be overridden according to the context, ex: "Update Invoice Item #1".
     * @param record the <code>Record</code> being loaded
     * @return a <code>String</code> that should be used as the form title
     */
    protected String getUpdateFormTitle(Record record) {
        return updateFormTitle;
    }

    /**
     * Sets the title of the form to use in create mode.
     * Defaults to "Create new record", but should be set according to the context, ex: "Create new Invoice Item".
     * @param title the title of the form to use in create mode
     */
    protected void setCreateFormTitle(String title) {
        createFormTitle = title;
        if (mainForm.getCurrentMode() == CreateOrUpdateEntityForm.Mode.CREATE) {
            setFormTitle(title);
        }
    }

}
