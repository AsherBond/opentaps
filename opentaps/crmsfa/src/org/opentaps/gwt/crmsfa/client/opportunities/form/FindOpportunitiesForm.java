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

import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.form.FindEntityForm;
import org.opentaps.gwt.common.client.form.base.SubFormPanel;
import org.opentaps.gwt.common.client.form.field.CheckboxField;
import org.opentaps.gwt.common.client.listviews.SalesOpportunityListView;
import org.opentaps.gwt.common.client.suggest.SalesOpportunityStageAutocomplete;
import org.opentaps.gwt.common.client.suggest.SalesOpportunityTypeAutocomplete;

import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.form.TextField;
/**
 * A combination of a case list view and a tabbed form used to filter that list view.
 */
public class FindOpportunitiesForm extends FindEntityForm<SalesOpportunityListView> {

    protected final SubFormPanel filterByAdvancedTab;
    protected final SalesOpportunityStageAutocomplete opportunityStageInput;
    protected final SalesOpportunityTypeAutocomplete typeEnumInput;
    protected final TextField opportunityNameInput;
 // find all option
    private final CheckboxField findAllInput;
    private final SalesOpportunityListView salesOpportunityListView;

    /**
     * Default constructor.
     */
    public FindOpportunitiesForm() {
        this(true);
    }

    /**
     * Constructor with autoLoad parameter, use this constructor if some filters need to be set prior to loading the grid data.
     * @param autoLoad sets the grid autoLoad parameter, set to <code>false</code> if some filters need to be set prior to loading the grid data
     */
    public FindOpportunitiesForm(boolean autoLoad) {
        super(UtilUi.MSG.crmFindOpportunities());
        opportunityStageInput = new SalesOpportunityStageAutocomplete(UtilUi.MSG.crmStage(), "opportunityStageId", getInputLength());
        typeEnumInput = new SalesOpportunityTypeAutocomplete(UtilUi.MSG.commonType(), "typeEnumId", getInputLength());
        opportunityNameInput = new TextField(UtilUi.MSG.crmOpportunityName(), "opportunityName", getInputLength());
        findAllInput = new CheckboxField(UtilUi.MSG.commonFindAll(), "findAll");

        // Build the filter by advanced tab
        filterByAdvancedTab = getMainForm().addTab(UtilUi.MSG.findByAdvanced());
        filterByAdvancedTab.addField(opportunityNameInput);
        filterByAdvancedTab.addField(opportunityStageInput);
        filterByAdvancedTab.addField(typeEnumInput);
        filterByAdvancedTab.addField(findAllInput);
        salesOpportunityListView = new SalesOpportunityListView();
        salesOpportunityListView.setAutoLoad(autoLoad);
        salesOpportunityListView.init();
        addListView(salesOpportunityListView);
    }

    protected void filterByAdvanced() {
        getListView().filterByOpportunityName(opportunityNameInput.getText());
        getListView().filterByOpportunityStageId(opportunityStageInput.getText());
        getListView().filterByTypeEnumId(typeEnumInput.getText());
        if (opportunityStageInput.getText() == null || "".equals(opportunityStageInput.getText())) {
            getListView().filterHasIncludeInactiveOpportunities(findAllInput.getValue());
        } else {
            getListView().filterHasIncludeInactiveOpportunities(true);
        }
    }

    @Override protected void filter() {
        getListView().clearFilters();
        Panel p = getMainForm().getTabPanel().getActiveTab();
        if (p == filterByAdvancedTab) {
            filterByAdvanced();
        }
        getListView().applyFilters();
    }

}
