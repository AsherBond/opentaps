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


import com.gwtext.client.widgets.form.TextField;
import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.form.FindPartyForm;
import org.opentaps.gwt.common.client.form.base.SubFormPanel;
import org.opentaps.gwt.common.client.listviews.LeadListView;

/**
 * A combination of a leads list view and a tabbed form used to filter that list view.
 */
public class FindLeadsForm extends FindPartyForm {

    private TextField firstNameInput;
    private TextField lastNameInput;
    private TextField companyNameInput;
    private final LeadListView leadListView;

    /**
     * Default constructor.
     */
    public FindLeadsForm() {
        super(UtilUi.MSG.leadId(), UtilUi.MSG.findLeads());
        leadListView = new LeadListView();
        leadListView.init();
        addListView(leadListView);
    }

    @Override
    protected void buildFilterByNameTab(SubFormPanel p) {
        firstNameInput = new TextField(UtilUi.MSG.firstName(), "firstName", getInputLength());
        lastNameInput = new TextField(UtilUi.MSG.lastName(), "lastName", getInputLength());
        companyNameInput = new TextField(UtilUi.MSG.companyName(), "companyName", getInputLength());
        p.addField(firstNameInput);
        p.addField(lastNameInput);
        p.addField(companyNameInput);
    }

    @Override
    protected void filterByNames() {
        leadListView.filterByFirstName(firstNameInput.getText());
        leadListView.filterByLastName(lastNameInput.getText());
        leadListView.filterByCompanyName(companyNameInput.getText());
    }

}
