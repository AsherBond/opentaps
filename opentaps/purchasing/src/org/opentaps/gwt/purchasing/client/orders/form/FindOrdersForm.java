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
package org.opentaps.gwt.purchasing.client.orders.form;

import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.form.Field;
import com.gwtext.client.widgets.form.TextField;
import com.gwtext.client.widgets.form.event.FieldListenerAdapter;
import com.gwtext.client.widgets.layout.ColumnLayout;
import com.gwtext.client.widgets.layout.ColumnLayoutData;
import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.form.FindEntityForm;
import org.opentaps.gwt.common.client.form.base.SubFormPanel;
import org.opentaps.gwt.common.client.form.field.CheckboxField;
import org.opentaps.gwt.common.client.form.field.DateField;
import org.opentaps.gwt.common.client.listviews.PurchaseOrderListView;
import org.opentaps.gwt.common.client.suggest.OrderStatusAutocomplete;
import org.opentaps.gwt.common.client.suggest.ProductAutocomplete;
import org.opentaps.gwt.common.client.suggest.SupplierAutocomplete;

/**
 * Form class for find order in purchasing.
 */
public class FindOrdersForm extends FindEntityForm<PurchaseOrderListView> {

    private final SubFormPanel filterPanel;
    // Order Id
    private final TextField orderIdInput;
    // Order Name
    private final TextField orderNameInput;
    // Product Pattern
    private final ProductAutocomplete productPatternInput;
    // Supplier
    private final SupplierAutocomplete supplierInput;
    // Status
    private final OrderStatusAutocomplete orderStatusInput;
    // From Date
    private final DateField fromDateInput;
    // Thru Date
    private final DateField thruDateInput;
    // Created By
    private final TextField createdByInput;

    private final PurchaseOrderListView orderListView;

    // find all option
    private final CheckboxField findAllInput;

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
        orderNameInput = new TextField(UtilUi.MSG.orderOrderName(), "orderName", getInputLength());
        productPatternInput = new ProductAutocomplete(UtilUi.MSG.productProduct(), "externalId", getInputLength());
        supplierInput = new SupplierAutocomplete(UtilUi.MSG.productSupplier(), "partyIdSearch", getInputLength());
        orderStatusInput = new OrderStatusAutocomplete(UtilUi.MSG.commonStatus(), "statusId", getInputLength());
        fromDateInput = new DateField(UtilUi.MSG.commonFromDate(), "fromDate", getInputLength());
        thruDateInput = new DateField(UtilUi.MSG.commonThruDate(), "thruDate", getInputLength());
        createdByInput = new TextField(UtilUi.MSG.commonCreatedBy(), "createdBy", getInputLength());
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
        // hide the tab bar since we only use one tab
        getMainForm().hideTabBar();

        Panel mainPanel = new Panel();
        mainPanel.setLayout(new ColumnLayout());

        SubFormPanel columnOnePanel = new SubFormPanel(getMainForm());
        SubFormPanel columnTwoPanel = new SubFormPanel(getMainForm());

        mainPanel.add(columnOnePanel, new ColumnLayoutData(.5));
        mainPanel.add(columnTwoPanel, new ColumnLayoutData(.5));

        columnOnePanel.addField(orderIdInput);
        columnTwoPanel.addField(orderNameInput);

        columnOnePanel.addField(supplierInput);
        columnTwoPanel.add(UtilUi.makeBlankFormCell());

        columnOnePanel.addField(productPatternInput);
        columnTwoPanel.add(UtilUi.makeBlankFormCell());

        columnOnePanel.addField(orderStatusInput);
        columnTwoPanel.add(UtilUi.makeBlankFormCell());

        columnOnePanel.addField(fromDateInput);
        columnTwoPanel.addField(thruDateInput);

        columnOnePanel.addField(createdByInput);
        columnTwoPanel.add(UtilUi.makeBlankFormCell());

        columnOnePanel.addField(findAllInput);
        columnTwoPanel.add(UtilUi.makeBlankFormCell());

        filterPanel.add(mainPanel);

        orderListView = new PurchaseOrderListView();
        orderListView.setAutoLoad(autoLoad);
        orderListView.init();
        addListView(orderListView);
    }

    @Override protected void filter() {
        getListView().clearFilters();
        if (!findAllInput.getValue() && isEmpty(orderIdInput.getText()) && isEmpty(productPatternInput.getText())
                && isEmpty(orderNameInput.getText()) && isEmpty(supplierInput.getText())
                && isEmpty(orderStatusInput.getText())
                && isEmpty(fromDateInput.getText()) && isEmpty(thruDateInput.getText())
                && isEmpty(createdByInput.getText())) {
            UtilUi.errorMessage(UtilUi.MSG.atLeastOnFieldRequiredToSearch());
            return;
        }
        getListView().filterByOrderId(orderIdInput.getText());
        getListView().filterByProductPattern(productPatternInput.getText());
        getListView().filterByOrderName(orderNameInput.getText());
        getListView().filterBySupplierId(supplierInput.getText());
        getListView().filterByStatusId(orderStatusInput.getText());
        getListView().filterByFromDate(fromDateInput.getText());
        getListView().filterByThruDate(thruDateInput.getText());
        getListView().filterByCreatedBy(createdByInput.getText());
        getListView().filterHasIncludeInactiveOrders(true);
        getListView().applyFilters();
    }
    
    private static boolean isEmpty(String text) {
        if (text == null || "".equals(text)) {
            return true;
        }
        return false;
    }

}
