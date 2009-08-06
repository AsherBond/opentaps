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
package org.opentaps.gwt.purchasing.suppliers.client.form;

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
