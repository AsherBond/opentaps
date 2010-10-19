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

package org.opentaps.gwt.crmsfa.client.contacts.form;

import com.gwtext.client.core.Position;
import com.gwtext.client.widgets.form.TextField;
import com.gwtext.client.widgets.form.VType;
import org.opentaps.base.constants.ViewPrefTypeConstants;
import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.form.base.ScreenletFormPanel;
import org.opentaps.gwt.common.client.form.field.PhoneNumberField;
import org.opentaps.gwt.common.client.security.Permission;
import org.opentaps.gwt.common.client.suggest.AccountAutocomplete;
import org.opentaps.gwt.crmsfa.client.contacts.form.configuration.QuickNewContactConfiguration;

/**
 * Form for quick creation of new contacts, only providing a few important fields.
 */
public class QuickNewContactForm extends ScreenletFormPanel {

    private TextField firstNameInput;
    private TextField lastNameInput;
    private TextField emailInput;
    private AccountAutocomplete accountNameInput;
    private PhoneNumberField phoneInput;

    /**
     * Constructor.
     */
    public QuickNewContactForm() {
        this(UtilUi.MSG.crmCreateContact());
    }

    /**
     * Constructor specifying the title.
     * @param title a <code>String</code> value
     */
    public QuickNewContactForm(String title) {

        super(Position.TOP, title);

        if (!Permission.hasPermission(Permission.CRMSFA_CONTACT_CREATE)) {
            return;
        }

        setUrl(QuickNewContactConfiguration.URL);     // this sets the action of the form
        firstNameInput = new TextField(UtilUi.MSG.partyFirstName(), QuickNewContactConfiguration.IN_FIRST_NAME, getInputLength());
        addRequiredField(firstNameInput);

        lastNameInput = new TextField(UtilUi.MSG.partyLastName(), QuickNewContactConfiguration.IN_LAST_NAME, getInputLength());
        addRequiredField(lastNameInput);

        accountNameInput = new AccountAutocomplete(UtilUi.MSG.crmAccount(), QuickNewContactConfiguration.IN_ACCOUNT_PARTY_ID, getInputLength());
        addField(accountNameInput);

        phoneInput = new PhoneNumberField(UtilUi.MSG.partyPhoneNumber(), QuickNewContactConfiguration.IN_PHONE_COUNTRY_CODE, QuickNewContactConfiguration.IN_PHONE_AREA_CODE, QuickNewContactConfiguration.IN_PHONE_NUMBER, getInputLength());
        addField(phoneInput);

        emailInput = new TextField(UtilUi.MSG.partyEmailAddress(), QuickNewContactConfiguration.IN_EMAIL_ADDRESS, getInputLength());
        emailInput.setVtype(VType.EMAIL);
        addField(emailInput);

        addStandardSubmitButton(UtilUi.MSG.crmCreateContact());
    }

    @Override public String getPreferenceTypeId() {
        return ViewPrefTypeConstants.GWT_QK_CONTACT;
    }

}
