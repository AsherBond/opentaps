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

package org.opentaps.gwt.crmsfa.client.accounts.form;

import com.gwtext.client.core.Position;
import com.gwtext.client.widgets.form.TextField;
import com.gwtext.client.widgets.form.VType;
import org.opentaps.base.constants.ViewPrefTypeConstants;
import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.form.base.ScreenletFormPanel;
import org.opentaps.gwt.common.client.form.field.PhoneNumberField;
import org.opentaps.gwt.common.client.security.Permission;
import org.opentaps.gwt.crmsfa.client.accounts.form.configuration.QuickNewAccountConfiguration;

/**
 * Form for quick creation of new accounts, only providing a few important fields.
 */
public class QuickNewAccountForm extends ScreenletFormPanel {

    private TextField accountNameInput;
    private TextField emailInput;
    private PhoneNumberField phoneInput;

    /**
     * Constructor.
     */
    public QuickNewAccountForm() {
        this(UtilUi.MSG.crmCreateAccount());
    }

    /**
     * Constructor specifying the title.
     * @param title a <code>String</code> value
     */
    public QuickNewAccountForm(String title) {

        super(Position.TOP, title);

        if (!Permission.hasPermission(Permission.CRMSFA_ACCOUNT_CREATE)) {
            return;
        }

        setUrl(QuickNewAccountConfiguration.URL);     // this sets the action of the form
        accountNameInput = new TextField(UtilUi.MSG.crmAccountName(), QuickNewAccountConfiguration.IN_ACCOUNT_NAME, getInputLength());
        addRequiredField(accountNameInput);

        phoneInput = new PhoneNumberField(UtilUi.MSG.partyPhoneNumber(), QuickNewAccountConfiguration.IN_PHONE_COUNTRY_CODE, QuickNewAccountConfiguration.IN_PHONE_AREA_CODE, QuickNewAccountConfiguration.IN_PHONE_NUMBER, getInputLength());
        addField(phoneInput);

        emailInput = new TextField(UtilUi.MSG.partyEmailAddress(), QuickNewAccountConfiguration.IN_EMAIL_ADDRESS, getInputLength());
        emailInput.setVtype(VType.EMAIL);
        addField(emailInput);

        addStandardSubmitButton(UtilUi.MSG.crmCreateAccount());
    }

    @Override public String getPreferenceTypeId() {
        return ViewPrefTypeConstants.GWT_QK_ACCOUNT;
    }

}
