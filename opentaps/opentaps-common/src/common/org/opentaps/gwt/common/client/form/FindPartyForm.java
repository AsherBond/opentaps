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

package org.opentaps.gwt.common.client.form;

import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.form.TextField;
import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.form.base.SubFormPanel;
import org.opentaps.gwt.common.client.form.field.PhoneNumberField;
import org.opentaps.gwt.common.client.listviews.PartyListView;
import org.opentaps.gwt.common.client.suggest.CountryAutocomplete;
import org.opentaps.gwt.common.client.suggest.PartyClassificationAutocomplete;
import org.opentaps.gwt.common.client.suggest.StateAutocomplete;


/**
 * Base class for the common Find party form + list view pattern.
 *
 * @see org.opentaps.gwt.common.client.form.FindAccountsForm
 * @see org.opentaps.gwt.common.client.form.FindContactsForm
 * @see org.opentaps.gwt.crmsfa.client.leads.form.FindLeadsForm
 * @see org.opentaps.gwt.crmsfa.client.partners.form.FindPartnersForm
 * @see org.opentaps.gwt.purchasing.client.suppliers.form.FindSuppliersForm
 */
public abstract class FindPartyForm extends FindEntityForm<PartyListView> {

    private final SubFormPanel filterByIdAndNameTab;
    private final TextField idInput;

    private final SubFormPanel filterByPhoneTab;
    private final PhoneNumberField phoneInput;

    private final SubFormPanel filterByEmailTab;
    private final TextField emailInput;

    protected final SubFormPanel filterByAdvancedTab;
    protected final PartyClassificationAutocomplete classificationInput;
    protected final TextField addressInput;
    protected final TextField cityInput;
    protected final CountryAutocomplete countryInput;
    protected final StateAutocomplete stateInput;
    protected final TextField postalCodeInput;
    protected final TextField toNameInput;
    protected final TextField attnNameInput;

    /**
     * Constructor.  The order in which the tab widgets are created reflects the
     * order in which they appear from left to right.
     *
     * @param idLabel the label for the main column of the list view, the column listing the party identifiers
     * @param findButtonLabel the label for the find button of the filter form
     */
    public FindPartyForm(String idLabel, String findButtonLabel) {
        super(findButtonLabel);

        // Filter by ID and Name
        filterByIdAndNameTab = getMainForm().addTab(UtilUi.MSG.findByIdAndName());
        idInput = new TextField(idLabel, "id", getInputLength());
        filterByIdAndNameTab.addField(idInput);
        // adds the name fields, implemented in the sub classes
        buildFilterByNameTab(filterByIdAndNameTab);


        // Filter by Phone Number
        filterByPhoneTab = getMainForm().addTab(UtilUi.MSG.findByPhone());
        phoneInput = new PhoneNumberField(UtilUi.MSG.partyPhoneNumber(), getLabelLength(), getInputLength());
        filterByPhoneTab.addField(phoneInput);

        // Filter by Email Address
        filterByEmailTab = getMainForm().addTab(UtilUi.MSG.findByEmail());
        emailInput = new TextField(UtilUi.MSG.emailAddress(), "emailAddress", getInputLength());
        filterByEmailTab.addField(emailInput);

        classificationInput = new PartyClassificationAutocomplete(UtilUi.MSG.classification(), "classification", getInputLength());
        toNameInput = new TextField(UtilUi.MSG.partyToName(), "toName", getInputLength());
        attnNameInput = new TextField(UtilUi.MSG.partyAttentionName(), "attnName", getInputLength());
        addressInput = new TextField(UtilUi.MSG.partyAddressLine1(), "address", getInputLength());
        cityInput = new TextField(UtilUi.MSG.partyCity(), "city", getInputLength());
        postalCodeInput = new TextField(UtilUi.MSG.partyPostalCode(), "postalCode", getInputLength());
        countryInput = new CountryAutocomplete(UtilUi.MSG.partyCountry(), "country", getInputLength());
        stateInput = new StateAutocomplete(UtilUi.MSG.partyState(), "state", countryInput, getInputLength());

        // Build the filter by advanced tab
        filterByAdvancedTab = getMainForm().addTab(UtilUi.MSG.findByAdvanced());
        buildFilterByAdvancedTab(filterByAdvancedTab);
    }

    /**
     * Builds the tab in the filter form used to filter by name.
     * Since different party types use different fields for names, the implementation will be
     * defined in the sub classes.
     * @param p the tab <code>SubFormPanel</code> that this method should populate
     */
    protected abstract void buildFilterByNameTab(SubFormPanel p);

    /**
     * Filters by the name fields.
     * Since different party types use different fields for names, the implementation will be
     * defined in the sub classes.
     */
    protected abstract void filterByNames();

    /**
     * Basic advanced tab contains all the filters created in constructor.
     * TODO this isn't ideal but gets the job done.
     * @param p the tab <code>SubFormPanel</code> that this method should populate
     */
    protected void buildFilterByAdvancedTab(SubFormPanel p) {
        filterByAdvancedTab.addField(classificationInput);
        filterByAdvancedTab.addField(toNameInput);
        filterByAdvancedTab.addField(attnNameInput);
        filterByAdvancedTab.addField(addressInput);
        filterByAdvancedTab.addField(cityInput);
        filterByAdvancedTab.addField(countryInput);
        filterByAdvancedTab.addField(stateInput);
        filterByAdvancedTab.addField(postalCodeInput);
    }

    protected void filterById() {
        getListView().filterByPartyId(idInput.getText());
    }

    protected void filterByPhoneNumber() {
        getListView().filterByPhoneCountryCode(phoneInput.getCountryCode());
        getListView().filterByPhoneAreaCode(phoneInput.getAreaCode());
        getListView().filterByPhoneNumber(phoneInput.getNumber());
    }

    protected void filterByEmailAddress() {
        getListView().filterByEmailAddress(emailInput.getText());
    }

    protected void filterByAdvanced() {
        getListView().filterByClassification(classificationInput.getText());
        getListView().filterByAddress(addressInput.getText());
        getListView().filterByCity(cityInput.getText());
        getListView().filterByCountry(countryInput.getText());
        getListView().filterByStateProvince(stateInput.getText());
        getListView().filterByPostalCode(postalCodeInput.getText());
        getListView().filterByToName(toNameInput.getText());
        getListView().filterByAttnName(attnNameInput.getText());
    }

    @Override protected void filter() {
        getListView().clearFilters();
        Panel p = getMainForm().getTabPanel().getActiveTab();
        if (p == filterByIdAndNameTab) {
            filterById();
            filterByNames();
        } else if (p == filterByPhoneTab) {
            filterByPhoneNumber();
        } else if (p == filterByEmailTab) {
            filterByEmailAddress();
        } else if (p == filterByAdvancedTab) {
            filterByAdvanced();
        }
        getListView().applyFilters();
    }

}
