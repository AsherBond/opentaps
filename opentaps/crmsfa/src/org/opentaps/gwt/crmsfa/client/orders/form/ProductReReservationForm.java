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

import java.util.Set;

import com.google.gwt.i18n.client.Dictionary;
import com.gwtext.client.data.SimpleStore;
import com.gwtext.client.data.Store;
import com.gwtext.client.widgets.form.ComboBox;
import com.gwtext.client.widgets.form.Hidden;
import com.gwtext.client.widgets.form.TextField;
import com.gwtext.client.widgets.layout.AnchorLayoutData;
import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.form.PopupFormWindow;
import org.opentaps.gwt.common.client.form.base.BaseFormPanel;


/**
 * Implementation of popup form that collects user input and calls <code>reReserveProductInventory</code> service.
 */
public class ProductReReservationForm extends PopupFormWindow {

    private String[][] facilityList;
    private String orderId;
    private String orderItemSeqId;
    private String inventoryItemId;
    private String shipGroupSeqId;
    private String quantity;

    private static final int LABEL_WIDTH = 70;
    private static final int FORM_WIDTH  = 300;
    private static final int FORM_HEIGHT = 180;

    /**
     * Keep arguments for later use, initialize list of facilities.
     *
     * @param title Window title
     * @param facilities collection of facilities as pairs of <code>facilitId</code> and <code>facilityName</code>
     * @param orderId order identifier value.
     * @param orderItemSeqId order item sequence number
     * @param inventoryItemId the inventory item identifier value
     * @param shipGroupSeqId the ship group identifier value
     * @param quantity the quantity reserved
     */
    public ProductReReservationForm(String title, Dictionary facilities, String orderId, String orderItemSeqId, String inventoryItemId, String shipGroupSeqId, String quantity) {
        super(title, FORM_WIDTH, FORM_HEIGHT);
        Set<String> facilityIds = facilities.keySet();
        facilityList = new String[facilityIds.size()][2];
        int i = 0;
        for (String facilityId : facilityIds) {
            facilityList[i][0] = facilityId;
            facilityList[i][1] = facilities.get(facilityId);
            i++;
        }
        this.orderId = orderId;
        this.orderItemSeqId = orderItemSeqId;
        this.inventoryItemId = inventoryItemId;
        this.shipGroupSeqId = shipGroupSeqId;
        this.quantity = quantity;
    }

    /* (non-Javadoc)
     * @see org.opentaps.gwt.common.client.form.PopupFormWindow#initFields(org.opentaps.gwt.common.client.form.base.BaseFormPanel)
     */
    @Override
    protected void initFields(BaseFormPanel container) {
        // creates store for drop-down where user can select target facility
        Store facilityStore = new SimpleStore(new String[] {"facilityId", "facilityName"}, facilityList);
        facilityStore.load();

        // add list of facilities
        ComboBox cb = new ComboBox();
        cb.setFieldLabel(UtilUi.MSG.productNewFacility());
        cb.setStore(facilityStore);
        cb.setDisplayField("facilityName");
        cb.setValueField("facilityId");
        cb.setHiddenName("facilityId");
        cb.setMode(ComboBox.LOCAL);
        cb.setTriggerAction(ComboBox.ALL);
        cb.setSelectOnFocus(true);
        container.addField(cb, new AnchorLayoutData("100%"));

        // add field for product quantity
        TextField quantityCtrl = new TextField(UtilUi.MSG.orderQuantity(), "quantity");
        if (quantity != null) {
            quantityCtrl.setValue(quantity.toString());
        }
        container.addField(quantityCtrl);

        // store order and its item ids in hidden fields
        container.addField(new Hidden("orderId", orderId));
        container.addField(new Hidden("orderItemSeqId", orderItemSeqId));
        container.addField(new Hidden("inventoryItemId", inventoryItemId));
        container.addField(new Hidden("shipGroupSeqId", shipGroupSeqId));
    }

    /* (non-Javadoc)
     * @see org.opentaps.gwt.common.client.form.PopupFormWindow#initComponent()
     */
    @Override
    protected void initComponent() {

        innerPanel.setLabelWidth(LABEL_WIDTH);
        // form action
        innerPanel.setUrl("gwtReReserveProduct");

        super.initComponent();
    }

}
