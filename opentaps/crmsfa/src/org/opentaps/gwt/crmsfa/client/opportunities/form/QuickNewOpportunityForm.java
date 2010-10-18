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
package org.opentaps.gwt.crmsfa.client.opportunities.form;

import com.gwtext.client.core.Position;
import com.gwtext.client.widgets.form.TextField;
import org.opentaps.base.constants.ViewPrefTypeConstants;
import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.form.base.ScreenletFormPanel;
import org.opentaps.gwt.common.client.form.field.DateField;
import org.opentaps.gwt.common.client.form.field.NumberField;
import org.opentaps.gwt.common.client.suggest.AccountOrQualifiedLeadPartyAutocomplete;
import org.opentaps.gwt.common.client.suggest.SalesOpportunityStageAutocomplete;
import org.opentaps.gwt.crmsfa.client.opportunities.form.configuration.QuickNewOpportunityConfiguration;

/**
 * Form class for quick create opportunity.
 */
public class QuickNewOpportunityForm extends ScreenletFormPanel {

    private AccountOrQualifiedLeadPartyAutocomplete accountOrQualifiedLeadPartyIdInput;
    private TextField opportunityNameInput;
    private SalesOpportunityStageAutocomplete opportunityStageInput;
    private NumberField estimatedAmountInput;
    private DateField estimatedCloseDateInput;

    /**
     * Constructor.
     */
    public QuickNewOpportunityForm() {
        this(UtilUi.MSG.crmCreateOpportunity());
    }

    /**
     * Constructor specifying the title.
     * @param title a <code>String</code> value
     */
    public QuickNewOpportunityForm(String title) {

        // label at the top
        super(Position.TOP, title);

        // the URL, linked to opentaps controller
        setUrl(QuickNewOpportunityConfiguration.URL);

        // accountOrLeadPartyId is a required field and will be the AccountOrLeadPartyAutocomplete
        accountOrQualifiedLeadPartyIdInput = new AccountOrQualifiedLeadPartyAutocomplete(UtilUi.MSG.crmAccountOrLeadParty(), QuickNewOpportunityConfiguration.ACCOUNT_OR_LEAD_PARTY_ID, getInputLength());
        addRequiredField(accountOrQualifiedLeadPartyIdInput);

        // opportunity name is a required text input field
        opportunityNameInput = new TextField(UtilUi.MSG.crmOpportunityName(), QuickNewOpportunityConfiguration.OPPORTUNITY_NAME, getInputLength());
        addRequiredField(opportunityNameInput);

        // opportunity stage is a required SalesOpportunityStageAutocomplete input field
        opportunityStageInput = new SalesOpportunityStageAutocomplete(UtilUi.MSG.crmInitialStage(), QuickNewOpportunityConfiguration.OPPORTUNITY_STAGE_ID, getInputLength());
        // set SOSTG_PROSPECT as default value
        opportunityStageInput.setValue("SOSTG_PROSPECT");
        addRequiredField(opportunityStageInput);

        // subject is a NumberField input field
        estimatedAmountInput = new NumberField(UtilUi.MSG.crmOpportunityAmount(), QuickNewOpportunityConfiguration.ESTIMATED_AMOUNT, getInputLength());
        addField(estimatedAmountInput);

        // estimated closeDate is a required DateField input field
        estimatedCloseDateInput = new DateField(UtilUi.MSG.crmEstimatedCloseDate(), QuickNewOpportunityConfiguration.ESTIMATED_CLOSE_DATE, getInputLength());
        addRequiredField(estimatedCloseDateInput);

        // add the button with ui label
        addStandardSubmitButton(UtilUi.MSG.crmCreateOpportunity());
    }

    @Override public String getPreferenceTypeId() {
        return ViewPrefTypeConstants.GWT_QK_OPPORTUNITY;
    }
}
