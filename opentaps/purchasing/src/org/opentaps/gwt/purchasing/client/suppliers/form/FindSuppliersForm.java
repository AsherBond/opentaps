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
package org.opentaps.gwt.purchasing.client.suppliers.form;

import com.gwtext.client.widgets.form.TextField;
import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.form.FindPartyForm;
import org.opentaps.gwt.common.client.form.base.SubFormPanel;
import org.opentaps.gwt.common.client.listviews.SupplierListView;


/**
 * Find suppliers GWT implementation.
 */
public class FindSuppliersForm extends FindPartyForm {

    private TextField supplierNameInput;
    private final SupplierListView supplierListView;

    /**
     * Default constructor.
     */
    public FindSuppliersForm() {
        super(UtilUi.MSG.supplierId(), UtilUi.MSG.findSuppliers());
        supplierListView = new SupplierListView();
        supplierListView.init();
        addListView(supplierListView);
    }

    @Override
    protected void buildFilterByNameTab(SubFormPanel p) {
        supplierNameInput = new TextField(UtilUi.MSG.supplierName(), "supplierName", getInputLength());
        p.addField(supplierNameInput);
    }

    @Override
    protected void buildFilterByAdvancedTab(SubFormPanel p) {
        filterByAdvancedTab.addField(addressInput);
        filterByAdvancedTab.addField(cityInput);
        filterByAdvancedTab.addField(countryInput);
        filterByAdvancedTab.addField(stateInput);
        filterByAdvancedTab.addField(postalCodeInput);
    }

    @Override
    protected void filterByNames() {
        supplierListView.filterBySupplierName(supplierNameInput.getText());
    }

}
