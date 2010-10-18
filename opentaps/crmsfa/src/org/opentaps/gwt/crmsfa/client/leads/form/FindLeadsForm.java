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
        super(UtilUi.MSG.crmLeadId(), UtilUi.MSG.crmFindLeads());
        leadListView = makeLeadListView();
    }

    /**
     * Builds and add the list view in the form.
     * @return a <code>LeadListView</code> value
     */
    protected LeadListView makeLeadListView() {
        LeadListView v = new LeadListView();
        v.init();
        addListView(v);
        return v;
    }

    @Override
    protected void buildFilterByNameTab(SubFormPanel p) {
        firstNameInput = new TextField(UtilUi.MSG.partyFirstName(), "firstName", getInputLength());
        lastNameInput = new TextField(UtilUi.MSG.partyLastName(), "lastName", getInputLength());
        companyNameInput = new TextField(UtilUi.MSG.crmCompanyName(), "companyName", getInputLength());
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
