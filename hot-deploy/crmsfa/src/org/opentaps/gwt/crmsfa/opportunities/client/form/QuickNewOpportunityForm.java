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
package org.opentaps.gwt.crmsfa.opportunities.client.form;

import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.form.base.ScreenletFormPanel;
import org.opentaps.gwt.common.client.form.field.DateField;
import org.opentaps.gwt.common.client.form.field.NumberField;
import org.opentaps.gwt.common.client.suggest.AccountAutocomplete;
import org.opentaps.gwt.common.client.suggest.LeadAutocomplete;
import org.opentaps.gwt.common.client.suggest.SalesOpportunityStageAutocomplete;
import org.opentaps.gwt.crmsfa.opportunities.client.form.configuration.QuickNewOpportunityConfiguration;

import com.gwtext.client.core.Position;
import com.gwtext.client.widgets.form.TextField;
/**
 * Form class for quick create opportunity.
 */
public class QuickNewOpportunityForm extends ScreenletFormPanel {

    private AccountAutocomplete accountInput;
    private LeadAutocomplete leadInput;
    private TextField opportunityNameInput;
    private SalesOpportunityStageAutocomplete opportunityStageInput;
    private NumberField estimatedAmountInput;
    private DateField estimatedCloseDateInput;
    private static final Integer INPUT_LENGTH = 135;

    public QuickNewOpportunityForm() {

        // label at the top
        super(Position.TOP, UtilUi.MSG.crmNewOpportunity());

        // the URL, linked to opentaps controller
        setUrl(QuickNewOpportunityConfiguration.URL);

        // account is a required field and will be the account autocomplete
        accountInput = new AccountAutocomplete(UtilUi.MSG.crmAccount(), QuickNewOpportunityConfiguration.ACCOUNT_PARTY_ID, INPUT_LENGTH);
        addField(accountInput);

        // lead is a required field and will be the lead autocomplete
        leadInput = new LeadAutocomplete(UtilUi.MSG.crmLead(), QuickNewOpportunityConfiguration.LEAD_PARTY_ID, INPUT_LENGTH);
        addField(leadInput);

        // opportunity name is a required text input field
        opportunityNameInput = new TextField(UtilUi.MSG.crmOpportunityName(), QuickNewOpportunityConfiguration.OPPORTUNITY_NAME, INPUT_LENGTH);
        addRequiredField(opportunityNameInput);

        // opportunity stage is a required SalesOpportunityStageAutocomplete input field
        opportunityStageInput = new SalesOpportunityStageAutocomplete(UtilUi.MSG.crmInitialStage(), QuickNewOpportunityConfiguration.OPPORTUNITY_STAGE_ID, INPUT_LENGTH);
        addRequiredField(opportunityStageInput);

        // subject is a NumberField input field
        estimatedAmountInput = new NumberField(UtilUi.MSG.crmEstimatedAmount(), QuickNewOpportunityConfiguration.ESTIMATED_AMOUNT, INPUT_LENGTH);
        addField(estimatedAmountInput);

        // subject is a required DateField input field
        estimatedCloseDateInput = new DateField(UtilUi.MSG.crmEstimatedCloseDate(), QuickNewOpportunityConfiguration.ESTIMATED_CLOSE_DATE, INPUT_LENGTH);
        addRequiredField(estimatedCloseDateInput);

        // add the button with ui label
        addStandardSubmitButton(UtilUi.MSG.commonCreate());
    }

}