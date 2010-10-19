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
package org.opentaps.gwt.crmsfa.client.cases.form;

import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.form.FindEntityForm;
import org.opentaps.gwt.common.client.form.base.SubFormPanel;
import org.opentaps.gwt.common.client.listviews.CaseListView;
import org.opentaps.gwt.common.client.suggest.CasePriorityAutocomplete;
import org.opentaps.gwt.common.client.suggest.CaseStatusAutocomplete;
import org.opentaps.gwt.common.client.suggest.CustRequestTypeAutocomplete;

import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.form.TextField;
/**
 * A combination of a case list view and a tabbed form used to filter that list view.
 */
public class FindCasesForm extends FindEntityForm<CaseListView> {

    protected final SubFormPanel filterByAdvancedTab;
    protected final CasePriorityAutocomplete casePriorityInput;
    protected final CaseStatusAutocomplete caseStatusInput;
    protected final CustRequestTypeAutocomplete custRequestTypeInput;
    protected final TextField subjectInput;
    private final CaseListView caseListView;

    /**
     * Default constructor.
     */
    public FindCasesForm() {
        this(true);
    }

    /**
     * Constructor with autoLoad parameter, use this constructor if some filters need to be set prior to loading the grid data.
     * @param autoLoad sets the grid autoLoad parameter, set to <code>false</code> if some filters need to be set prior to loading the grid data
     */
    public FindCasesForm(boolean autoLoad) {
        super(UtilUi.MSG.crmFindCases());
        casePriorityInput = new CasePriorityAutocomplete(UtilUi.MSG.commonPriority(), "priority", getInputLength());
        caseStatusInput = new CaseStatusAutocomplete(UtilUi.MSG.commonStatus(), "statusId", getInputLength());
        custRequestTypeInput = new CustRequestTypeAutocomplete(UtilUi.MSG.commonType(), "custRequestTypeId", getInputLength());
        subjectInput = new TextField(UtilUi.MSG.partySubject(), "custRequestName", getInputLength());

        // Build the filter by advanced tab
        filterByAdvancedTab = getMainForm().addTab(UtilUi.MSG.findByAdvanced());
        filterByAdvancedTab.addField(subjectInput);
        filterByAdvancedTab.addField(casePriorityInput);
        filterByAdvancedTab.addField(caseStatusInput);
        filterByAdvancedTab.addField(custRequestTypeInput);
        caseListView = new CaseListView();
        caseListView.setAutoLoad(autoLoad);
        caseListView.init();
        addListView(caseListView);
    }

    protected void filterByAdvanced() {
        getListView().filterByCustRequestName(subjectInput.getText());
        getListView().filterByCustRequestTypeId(custRequestTypeInput.getText());
        getListView().filterByStatusId(caseStatusInput.getText());
        getListView().filterByPriority(casePriorityInput.getText());
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
