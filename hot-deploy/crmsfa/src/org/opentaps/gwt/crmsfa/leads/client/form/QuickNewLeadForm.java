/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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

package org.opentaps.gwt.crmsfa.leads.client.form;

import com.gwtext.client.core.Position;
import com.gwtext.client.widgets.form.TextField;
import com.gwtext.client.widgets.form.VType;
import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.form.base.ScreenletFormPanel;
import org.opentaps.gwt.common.client.form.field.PhoneNumberField;
import org.opentaps.gwt.common.client.security.Permission;
import org.opentaps.gwt.crmsfa.leads.client.form.configuration.QuickNewLeadConfiguration;

/**
 * Form for quick creation of new leads, only providing a few important fields.
 */
public class QuickNewLeadForm extends ScreenletFormPanel {

    private TextField companyNameInput;
    private TextField firstNameInput;
    private TextField lastNameInput;
    private TextField emailInput;
    private PhoneNumberField phoneInput;

    private static final Integer INPUT_LENGTH = 135;

    /**
     * Constructor.
     */
    public QuickNewLeadForm() {

        super(Position.TOP, UtilUi.MSG.createLead());

        if (!Permission.hasPermission(Permission.CRMSFA_LEAD_CREATE)) {
            return;
        }

        setUrl(QuickNewLeadConfiguration.URL);     // this sets the action of the form
        companyNameInput = new TextField(UtilUi.MSG.companyName(), QuickNewLeadConfiguration.IN_COMPANY_NAME, INPUT_LENGTH);
        addRequiredField(companyNameInput);

        firstNameInput = new TextField(UtilUi.MSG.firstName(), QuickNewLeadConfiguration.IN_FIRST_NAME, INPUT_LENGTH);
        addRequiredField(firstNameInput);

        lastNameInput = new TextField(UtilUi.MSG.lastName(), QuickNewLeadConfiguration.IN_LAST_NAME, INPUT_LENGTH);
        addRequiredField(lastNameInput);

        phoneInput = new PhoneNumberField(UtilUi.MSG.phoneNumber(), QuickNewLeadConfiguration.IN_PHONE_COUNTRY_CODE, QuickNewLeadConfiguration.IN_PHONE_AREA_CODE, QuickNewLeadConfiguration.IN_PHONE_NUMBER, INPUT_LENGTH);
        addField(phoneInput);

        emailInput = new TextField(UtilUi.MSG.emailAddress(), QuickNewLeadConfiguration.IN_EMAIL_ADDRESS, INPUT_LENGTH);
        emailInput.setVtype(VType.EMAIL);
        addField(emailInput);

        addStandardSubmitButton(UtilUi.MSG.createLead());
    }

}
