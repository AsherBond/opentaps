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

package org.opentaps.gwt.crmsfa.client.leads.form;

import com.gwtext.client.core.Position;
import com.gwtext.client.widgets.form.TextField;
import com.gwtext.client.widgets.form.VType;
import org.opentaps.base.constants.ViewPrefTypeConstants;
import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.form.base.ScreenletFormPanel;
import org.opentaps.gwt.common.client.form.field.PhoneNumberField;
import org.opentaps.gwt.common.client.security.Permission;
import org.opentaps.gwt.crmsfa.client.leads.form.configuration.QuickNewLeadConfiguration;

/**
 * Form for quick creation of new leads, only providing a few important fields.
 */
public class QuickNewLeadForm extends ScreenletFormPanel {

    private TextField companyNameInput;
    private TextField firstNameInput;
    private TextField lastNameInput;
    private TextField emailInput;
    private PhoneNumberField phoneInput;

    /**
     * Constructor.
     */
    public QuickNewLeadForm() {
        this(UtilUi.MSG.crmCreateLead());
    }

    /**
     * Constructor specifying the title.
     * @param title a <code>String</code> value
     */
    public QuickNewLeadForm(String title) {

        super(Position.TOP, title);

        if (!Permission.hasPermission(Permission.CRMSFA_LEAD_CREATE)) {
            return;
        }

        setUrl(QuickNewLeadConfiguration.URL);     // this sets the action of the form
        companyNameInput = new TextField(UtilUi.MSG.crmCompanyName(), QuickNewLeadConfiguration.IN_COMPANY_NAME, getInputLength());
        addRequiredField(companyNameInput);

        firstNameInput = new TextField(UtilUi.MSG.partyFirstName(), QuickNewLeadConfiguration.IN_FIRST_NAME, getInputLength());
        addRequiredField(firstNameInput);

        lastNameInput = new TextField(UtilUi.MSG.partyLastName(), QuickNewLeadConfiguration.IN_LAST_NAME, getInputLength());
        addRequiredField(lastNameInput);

        phoneInput = new PhoneNumberField(UtilUi.MSG.partyPhoneNumber(), QuickNewLeadConfiguration.IN_PHONE_COUNTRY_CODE, QuickNewLeadConfiguration.IN_PHONE_AREA_CODE, QuickNewLeadConfiguration.IN_PHONE_NUMBER, getInputLength());
        addField(phoneInput);

        emailInput = new TextField(UtilUi.MSG.partyEmailAddress(), QuickNewLeadConfiguration.IN_EMAIL_ADDRESS, getInputLength());
        emailInput.setVtype(VType.EMAIL);
        addField(emailInput);

        addStandardSubmitButton(UtilUi.MSG.crmCreateLead());
    }

    @Override public String getPreferenceTypeId() {
        return ViewPrefTypeConstants.GWT_QK_LEAD;
    }

}
