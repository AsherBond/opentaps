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

package org.opentaps.gwt.crmsfa.client.cases.form;

import com.gwtext.client.core.Position;
import com.gwtext.client.widgets.form.Hidden;
import com.gwtext.client.widgets.form.TextField;
import org.opentaps.base.constants.ViewPrefTypeConstants;
import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.form.base.ScreenletFormPanel;
import org.opentaps.gwt.common.client.suggest.AccountAutocomplete;
import org.opentaps.gwt.crmsfa.client.cases.form.configuration.QuickNewCaseConfiguration;

/**
 * Form for quick creation of cases.
 */
public class QuickNewCaseForm extends ScreenletFormPanel {

    private TextField subjectInput;
    private AccountAutocomplete accountNameInput;

    /**
     * Constructor.
     */
    public QuickNewCaseForm() {
        this(UtilUi.MSG.crmCreateCase());
    }

    /**
     * Constructor specifying the title.
     * @param title a <code>String</code> value
     */
    public QuickNewCaseForm(String title) {

        // label at the top
        super(Position.TOP, title);

        // the URL, linked to opentaps controller
        setUrl(QuickNewCaseConfiguration.URL);

        // subject is a required text input field
        subjectInput = new TextField(UtilUi.MSG.partySubject(), QuickNewCaseConfiguration.SUBJECT, getInputLength());
        addRequiredField(subjectInput);

        // this parameter is required for the crmsfa.createCase service
        addField(new Hidden(QuickNewCaseConfiguration.CASE_TYPE_ID, QuickNewCaseConfiguration.DEFAULT_CASE_TYPE_ID));
        // this is not required, but we're setting it to a default value for completeness
        addField(new Hidden(QuickNewCaseConfiguration.PRIORITY, QuickNewCaseConfiguration.DEFAULT_PRIORITY));

        // account is a required field and will be the account autocomplete
        accountNameInput = new AccountAutocomplete(UtilUi.MSG.crmAccount(), QuickNewCaseConfiguration.ACCOUNT_PARTY_ID, getInputLength());
        addRequiredField(accountNameInput);

        // add the button with ui label
        addStandardSubmitButton(UtilUi.MSG.crmCreateCase());
    }

    @Override public String getPreferenceTypeId() {
        return ViewPrefTypeConstants.GWT_QK_CASE;
    }

}
