/*
 * Copyright (c) 2009 Open Source Strategies, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 *
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
 */

package org.opentaps.gwt.crmsfa.cases.client.form;

import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.form.base.ScreenletFormPanel;
import org.opentaps.gwt.common.client.suggest.AccountAutocomplete;
import org.opentaps.gwt.crmsfa.cases.client.form.configuration.QuickNewCaseConfiguration;

import com.gwtext.client.core.Position;
import com.gwtext.client.widgets.form.TextField;
import com.gwtext.client.widgets.form.Hidden;

public class QuickNewCaseForm extends ScreenletFormPanel {

    private TextField subjectInput;
    private AccountAutocomplete accountNameInput;
    private static final Integer INPUT_LENGTH = 135;

    public QuickNewCaseForm() {

    	// label at the top
        super(Position.TOP, UtilUi.MSG.crmNewCase());

        // the URL, linked to opentaps controller
        setUrl(QuickNewCaseConfiguration.URL);

        // subject is a required text input field 
        subjectInput = new TextField(UtilUi.MSG.opentapsSubject(), QuickNewCaseConfiguration.SUBJECT, INPUT_LENGTH);
        addRequiredField(subjectInput);
        
        // this parameter is required for the crmsfa.createCase service
        addField(new Hidden(QuickNewCaseConfiguration.CASE_TYPE_ID, QuickNewCaseConfiguration.DEFAULT_CASE_TYPE_ID));
        // this is not required, but we're setting it to a default value for completeness
        addField(new Hidden(QuickNewCaseConfiguration.PRIORITY, QuickNewCaseConfiguration.DEFAULT_PRIORITY));

        // account is a required field and will be the account autocomplete
        accountNameInput = new AccountAutocomplete(UtilUi.MSG.account(), QuickNewCaseConfiguration.ACCOUNT_PARTY_ID, INPUT_LENGTH);
        addRequiredField(accountNameInput);

        // add the button with ui label
        addStandardSubmitButton(UtilUi.MSG.crmCreateCase());
    }

}
