/*
 * Copyright (c) 2009 - 2009 Open Source Strategies, Inc.
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
package org.opentaps.gwt.crmsfa.orders.client.form;

import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.form.FindEntityForm;
import org.opentaps.gwt.common.client.form.base.SubFormPanel;
import org.opentaps.gwt.common.client.form.field.DateField;
import org.opentaps.gwt.common.client.listviews.OrderListView;
import org.opentaps.gwt.common.client.suggest.CustomerAutocomplete;
import org.opentaps.gwt.common.client.suggest.LotAutocomplete;
import org.opentaps.gwt.common.client.suggest.OrderStatusAutocomplete;
import org.opentaps.gwt.common.client.suggest.ProductStoreAutocomplete;

import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.form.TextField;

/**
 * Form class for find order in crmsfa.
 */
public class FindOrdersForm extends FindEntityForm<OrderListView> {

    protected final SubFormPanel filterByAdvancedTab;
    // Order Id
    protected final TextField orderIdInput;
    // External ID
    protected final TextField externalIdInput;
    // Order Name
    protected final TextField orderNameInput;
    // Customer
    protected final CustomerAutocomplete customerInput;
    // Lookup  Product Store
    protected final ProductStoreAutocomplete productStoreInput;
    // Status
    protected final OrderStatusAutocomplete orderStatusInput;
    // PO #
    protected final TextField correspondingPoIdInput;
    // From Date
    protected DateField fromDateInput;
    // Thru Date
    protected DateField thruDateInput;
    // Created By
    protected final TextField createdByInput;
    // Lot ID
    protected final LotAutocomplete lotInput;
    // Serial Number
    protected final TextField serialNumberInput;


    private final OrderListView orderListView;

    /**
     * Default constructor.
     */
    public FindOrdersForm() {
        this(true);
    }

    /**
     * Constructor with autoLoad parameter, use this constructor if some filters need to be set prior to loading the grid data.
     * @param autoLoad sets the grid autoLoad parameter, set to <code>false</code> if some filters need to be set prior to loading the grid data
     */
    public FindOrdersForm(boolean autoLoad) {
        super(UtilUi.MSG.crmFindOrders());
        orderIdInput = new TextField(UtilUi.MSG.orderOrderId(), "orderId", getInputLength());
        externalIdInput = new TextField(UtilUi.MSG.orderExternalId(), "externalId", getInputLength());
        orderNameInput = new TextField(UtilUi.MSG.orderOrderName(), "orderName", getInputLength());
        customerInput = new CustomerAutocomplete(UtilUi.MSG.crmCustomer(), "partyIdSearch", getInputLength());
        productStoreInput = new ProductStoreAutocomplete(UtilUi.MSG.orderProductStore(), "productStoreId", getInputLength());
        orderStatusInput = new OrderStatusAutocomplete(UtilUi.MSG.commonStatus(), "statusId", getInputLength());
        correspondingPoIdInput = new TextField(UtilUi.MSG.opentapsPONumber(), "correspondingPoId", getInputLength());
        fromDateInput = new DateField(UtilUi.MSG.commonFromDate(), "fromDate", getInputLength());
        thruDateInput = new DateField(UtilUi.MSG.commonThruDate(), "thruDate", getInputLength());
        createdByInput = new TextField(UtilUi.MSG.commonCreatedBy(), "createdBy", getInputLength());
        lotInput = new LotAutocomplete(UtilUi.MSG.productLotId(), "lotId", getInputLength());
        serialNumberInput = new TextField(UtilUi.MSG.productSerialNumber(), "serialNumber", getInputLength());
        // Build the filter by advanced tab
        filterByAdvancedTab = getMainForm().addTab(UtilUi.MSG.findByAdvanced());
        filterByAdvancedTab.addField(orderIdInput);
        filterByAdvancedTab.addField(externalIdInput);
        filterByAdvancedTab.addField(orderNameInput);
        filterByAdvancedTab.addField(customerInput);
        filterByAdvancedTab.addField(productStoreInput);
        filterByAdvancedTab.addField(orderStatusInput);
        filterByAdvancedTab.addField(correspondingPoIdInput);
        filterByAdvancedTab.addField(fromDateInput);
        filterByAdvancedTab.addField(thruDateInput);
        filterByAdvancedTab.addField(createdByInput);
        filterByAdvancedTab.addField(lotInput);
        filterByAdvancedTab.addField(serialNumberInput);

        orderListView = new OrderListView();
        orderListView.setAutoLoad(autoLoad);
        orderListView.init();
        addListView(orderListView);
    }

    protected void filterByAdvanced() {
        getListView().filterByOrderId(orderIdInput.getText());
        getListView().filterByExternalId(externalIdInput.getText());
        getListView().filterByOrderName(orderNameInput.getText());
        getListView().filterByCustomerId(customerInput.getText());
        getListView().filterByProductStoreId(productStoreInput.getText());
        getListView().filterByStatusId(orderStatusInput.getText());
        getListView().filterByCorrespondingPoId(correspondingPoIdInput.getText());
        getListView().filterByFromDate(fromDateInput.getText());
        getListView().filterByThruDate(thruDateInput.getText());
        getListView().filterByCreatedBy(createdByInput.getText());
        getListView().filterByLotId(lotInput.getText());
        getListView().filterBySerialNumber(serialNumberInput.getText());
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
