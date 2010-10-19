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
package org.opentaps.gwt.crmsfa.client.orders.form;

import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.form.FindEntityForm;
import org.opentaps.gwt.common.client.form.base.SubFormPanel;
import org.opentaps.gwt.common.client.form.field.CheckboxField;
import org.opentaps.gwt.common.client.form.field.DateField;
import org.opentaps.gwt.common.client.listviews.SalesOrderListView;
import org.opentaps.gwt.common.client.suggest.CountryAutocomplete;
import org.opentaps.gwt.common.client.suggest.CustomerAutocomplete;
import org.opentaps.gwt.common.client.suggest.LotAutocomplete;
import org.opentaps.gwt.common.client.suggest.OrderStatusAutocomplete;
import org.opentaps.gwt.common.client.suggest.ProductAutocomplete;
import org.opentaps.gwt.common.client.suggest.ProductStoreAutocomplete;
import org.opentaps.gwt.common.client.suggest.StateAutocomplete;

import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.form.Field;
import com.gwtext.client.widgets.form.TextField;
import com.gwtext.client.widgets.form.event.FieldListenerAdapter;
import com.gwtext.client.widgets.layout.ColumnLayout;
import com.gwtext.client.widgets.layout.ColumnLayoutData;

/**
 * Form class for find order in crmsfa.
 */
public class FindOrdersForm extends FindEntityForm<SalesOrderListView> {

    private final SubFormPanel filterPanel;
    private final SubFormPanel filterByAdvancedTab;
    // Order Id
    private final TextField orderIdInput;
    // External ID
    private final TextField externalIdInput;
    // Order Name
    private final TextField orderNameInput;
    // Customer
    private final CustomerAutocomplete customerInput;
    // Lookup  Product Store
    private final ProductStoreAutocomplete productStoreInput;
    // Status
    private final OrderStatusAutocomplete orderStatusInput;
    // PO #
    private final TextField correspondingPoIdInput;
    // From Date
    private final DateField fromDateInput;
    // Thru Date
    private final DateField thruDateInput;
    // Created By
    private final TextField createdByInput;
    // Lot ID
    private final LotAutocomplete lotInput;
    // Serial Number
    private final TextField serialNumberInput;
    // find all option
    private final CheckboxField findAllInput;
    // Lookup  Product
    private final ProductAutocomplete productInput;

    private final SalesOrderListView orderListView;
    private final TextField shippingAddressInput;
    private final TextField shippingCityInput;
    private final CountryAutocomplete shippingCountryInput;
    private final StateAutocomplete shippingStateInput;
    private final TextField shippingPostalCodeInput;
    private final TextField shippingToNameInput;
    private final TextField shippingAttnNameInput;

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

        // change the form dimensions to accommodate two columns
        setLabelLength(100);
        setInputLength(180);
        setFormWidth(675);

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
        productInput = new ProductAutocomplete(UtilUi.MSG.orderProduct(), "productId", getInputLength());

        findAllInput = new CheckboxField(UtilUi.MSG.commonFindAll(), "findAll");

        // add a listener to disable the find all option if a status is specified
        // since this option will be ignored
        orderStatusInput.addListener(new FieldListenerAdapter() {
                @Override public void onChange(Field field, Object newVal, Object oldVal) {
                    if (orderStatusInput.getText() != null && !"".equals(orderStatusInput.getText())) {
                        findAllInput.setValue(false);
                    }
                }
            });
        // and vice versa if find all is selected clear the status input
        findAllInput.addListener(new FieldListenerAdapter() {
                @Override public void onChange(Field field, Object newVal, Object oldVal) {
                    if (findAllInput.getValue()) {
                        orderStatusInput.setValue("");
                    }
                }
            });

        // Build the filter tab
        filterPanel = getMainForm().addTab(UtilUi.MSG.crmFindOrders());
        Panel mainPanel = new Panel();
        mainPanel.setLayout(new ColumnLayout());

        SubFormPanel columnOnePanel = new SubFormPanel(getMainForm());
        SubFormPanel columnTwoPanel = new SubFormPanel(getMainForm());

        mainPanel.add(columnOnePanel, new ColumnLayoutData(.5));
        mainPanel.add(columnTwoPanel, new ColumnLayoutData(.5));

        columnOnePanel.addField(orderIdInput);
        columnTwoPanel.addField(externalIdInput);

        columnOnePanel.addField(correspondingPoIdInput);
        columnTwoPanel.addField(orderNameInput);

        columnOnePanel.addField(customerInput);
        columnTwoPanel.addField(productStoreInput);

        columnOnePanel.addField(orderStatusInput);
        columnTwoPanel.add(UtilUi.makeBlankFormCell());

        columnOnePanel.addField(productInput);
        columnTwoPanel.add(UtilUi.makeBlankFormCell());

        columnOnePanel.addField(lotInput);
        columnTwoPanel.addField(serialNumberInput);

        columnOnePanel.addField(fromDateInput);
        columnTwoPanel.addField(thruDateInput);

        columnOnePanel.addField(createdByInput);
        columnTwoPanel.add(UtilUi.makeBlankFormCell());

        columnOnePanel.addField(findAllInput);
        columnTwoPanel.add(UtilUi.makeBlankFormCell());
        filterPanel.add(mainPanel);
        
        shippingToNameInput = new TextField(UtilUi.MSG.partyToName(), "toName", getInputLength());
        shippingAttnNameInput = new TextField(UtilUi.MSG.partyAttentionName(), "attnName", getInputLength());
        shippingAddressInput = new TextField(UtilUi.MSG.partyAddressLine1(), "address", getInputLength());
        shippingCityInput = new TextField(UtilUi.MSG.partyCity(), "city", getInputLength());
        shippingPostalCodeInput = new TextField(UtilUi.MSG.partyPostalCode(), "postalCode", getInputLength());
        shippingCountryInput = new CountryAutocomplete(UtilUi.MSG.partyCountry(), "country", getInputLength());
        shippingStateInput = new StateAutocomplete(UtilUi.MSG.partyState(), "state", shippingCountryInput, getInputLength());
        // Build the filter by advanced tab
        filterByAdvancedTab = getMainForm().addTab(UtilUi.MSG.findByShippingAddress());
        Panel advancedPanel = new Panel();
        advancedPanel.setLayout(new ColumnLayout());

        SubFormPanel columnOneAdvancedPanel = new SubFormPanel(getMainForm());
        SubFormPanel columnTwoAdvancedPanel = new SubFormPanel(getMainForm());

        advancedPanel.add(columnOneAdvancedPanel, new ColumnLayoutData(.5));
        advancedPanel.add(columnTwoAdvancedPanel, new ColumnLayoutData(.5));
        
        columnOneAdvancedPanel.addField(shippingToNameInput);
        columnTwoAdvancedPanel.addField(shippingAttnNameInput);
        
        columnOneAdvancedPanel.addField(shippingAddressInput);
        columnTwoAdvancedPanel.add(UtilUi.makeBlankFormCell());

        columnOneAdvancedPanel.addField(shippingCityInput);
        columnTwoAdvancedPanel.add(UtilUi.makeBlankFormCell());

        columnOneAdvancedPanel.addField(shippingCountryInput);
        columnTwoAdvancedPanel.addField(shippingStateInput);
        
        columnOneAdvancedPanel.addField(shippingPostalCodeInput);
        columnTwoAdvancedPanel.add(UtilUi.makeBlankFormCell());

        filterByAdvancedTab.add(advancedPanel);

        orderListView = new SalesOrderListView();
        orderListView.setAutoLoad(autoLoad);
        orderListView.init();
        addListView(orderListView);
    }

    @Override protected void filter() {
        getListView().clearFilters();
        Panel p = getMainForm().getTabPanel().getActiveTab();
        getListView().filterIncludeInactiveOrders(true);
        // null check to define the default panel, as it happens on page load
        // with the auto submit set by URL parameter
        if (p == null || p == filterPanel) {
            if (!findAllInput.getValue() && isEmpty(orderIdInput.getText()) && isEmpty(externalIdInput.getText())
                    && isEmpty(orderNameInput.getText()) && isEmpty(customerInput.getText())
                    && isEmpty(productStoreInput.getText()) && isEmpty(orderStatusInput.getText())
                    && isEmpty(correspondingPoIdInput.getText()) && isEmpty(serialNumberInput.getText())
                    && isEmpty(fromDateInput.getText()) && isEmpty(thruDateInput.getText())
                    && isEmpty(createdByInput.getText()) && isEmpty(lotInput.getText())
                    && isEmpty(productInput.getText())) {
                UtilUi.errorMessage(UtilUi.MSG.atLeastOnFieldRequiredToSearch());
                return;
            }
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
            getListView().filterByProductId(productInput.getText());
        } else if (p == filterByAdvancedTab) {
            if (isEmpty(shippingAddressInput.getText()) && isEmpty(shippingCityInput.getText())
                    && isEmpty(shippingCountryInput.getText()) && isEmpty(shippingStateInput.getText())
                    && isEmpty(shippingPostalCodeInput.getText()) && isEmpty(shippingToNameInput.getText())
                    && isEmpty(shippingAttnNameInput.getText())) {
                UtilUi.errorMessage(UtilUi.MSG.atLeastOnFieldRequiredToSearch());
                return;
            }
            getListView().filterByShippingAddress(shippingAddressInput.getText());
            getListView().filterByShippingCity(shippingCityInput.getText());
            getListView().filterByShippingCountry(shippingCountryInput.getText());
            getListView().filterByShippingStateProvince(shippingStateInput.getText());
            getListView().filterByShippingPostalCode(shippingPostalCodeInput.getText());
            getListView().filterByShippingToName(shippingToNameInput.getText());
            getListView().filterByShippingAttnName(shippingAttnNameInput.getText());
        }
        getListView().applyFilters();
    }
    
    private static boolean isEmpty(String text) {
        if (text == null || "".equals(text)) {
            return true;
        }
        return false;
    }

}
