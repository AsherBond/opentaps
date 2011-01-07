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

package org.opentaps.gwt.common.client.listviews;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.form.CreateOrUpdateEntityForm;
import org.opentaps.gwt.common.client.form.field.ModifierOrNumberField;
import org.opentaps.gwt.common.client.form.field.NumberField;
import org.opentaps.gwt.common.client.form.field.TextField;
import org.opentaps.gwt.common.client.lookup.configuration.AccountingTagLookupConfiguration;
import org.opentaps.gwt.common.client.lookup.configuration.OrderItemsCartLookupConfiguration;
import org.opentaps.gwt.common.client.suggest.AccountingTagConfigurationStore;
import org.opentaps.gwt.common.client.suggest.ProductForCartAutocomplete;
import org.opentaps.gwt.common.client.suggest.ProductInfoForCartStore;

import com.gwtext.client.core.SortDir;
import com.gwtext.client.data.Record;
import com.gwtext.client.data.Store;
import com.gwtext.client.data.StringFieldDef;
import com.gwtext.client.widgets.form.Field;
import com.gwtext.client.widgets.grid.CellMetadata;
import com.gwtext.client.widgets.grid.Renderer;

/**
 * List of Order Items.
 */
public class OrderItemsEditableListView extends EntityEditableListView {

    private static final String MODULE = OrderItemsEditableListView.class.getName();
    private static final Integer INPUT_LENGTH = 150;

    /** Defines the possible order types. */
    public static enum OrderType {
        /** For a Purchase Order. */
        PURCHASE("PURCHASE_ORDER"),
        /** For a Sales Order. */
        SALES("SALES_ORDER");
        private final String value;
        /** Gets the order type string value.
         * @return the order type string value
         */
        public String getValue() {
            return this.value;
        }
        private OrderType(String value) {
            this.value = value;
        }
    }

    private final ProductForCartAutocomplete productEditor;
    private final TextField descriptionEditor;
    private final NumberField quantityEditor;
    private final ModifierOrNumberField priceEditor;

    private final String organizationPartyId;
    private final OrderType orderType;

    private CreateOrUpdateEntityForm form;
    private AccountingTagConfigurationStore tagConfiguration;

    /**
     * Constructor.
     * @param type the <code>OrderType</code>
     * @param organizationPartyId a <code>String</code> value
     */
    public OrderItemsEditableListView(OrderType type, String organizationPartyId) {
        this(UtilUi.MSG.orderOrderItems(), type, organizationPartyId);
    }

    /**
     * Constructor giving a title for this list view, which is displayed in the UI.
     * @param title the title label for this list view
     * @param type the <code>OrderType</code>
     * @param organizationPartyId a <code>String</code> value
     */
    public OrderItemsEditableListView(String title, OrderType type, String organizationPartyId) {
        super(title);

        this.orderType = type;
        this.organizationPartyId = organizationPartyId;

        descriptionEditor = new TextField(UtilUi.MSG.commonDescription(), OrderItemsCartLookupConfiguration.INOUT_DESCRIPTION, INPUT_LENGTH);
        productEditor = new ProductForCartAutocomplete(UtilUi.MSG.productProduct(), OrderItemsCartLookupConfiguration.INOUT_PRODUCT, INPUT_LENGTH);
        quantityEditor = new NumberField(UtilUi.MSG.commonQuantity(), OrderItemsCartLookupConfiguration.INOUT_QUANTITY, INPUT_LENGTH);
        priceEditor = new ModifierOrNumberField(UtilUi.MSG.commonUnitPrice(), OrderItemsCartLookupConfiguration.INOUT_UNIT_PRICE, INPUT_LENGTH);

        form = new CreateOrUpdateEntityForm(false) {
                @Override protected void onSuccess() {
                    loadFirstPage();
                    super.onSuccess();
                }
            };

        // redirect form effects to the grid
        form.setEffectsTarget(this);
        form.setBorder(false);
        form.setUrl(OrderItemsCartLookupConfiguration.URL_POST_ORDER_ITEMS);
        form.addField(productEditor);
        form.addField(quantityEditor);
        // form is hidden by default, will show when a valid record is selected
        // and hide again if an invalid record is selected
        form.setHideMode("visibility");
        form.setVisible(false);
        init();
        this.bindToForm(form);
    }

    protected void init() {
        // load the accounting tag configuration
        String accountingTagUsageTypeId = AccountingTagConfigurationStore.getUsageTypeFor(orderType.getValue());
        if (accountingTagUsageTypeId != null) {
            tagConfiguration = new AccountingTagConfigurationStore(organizationPartyId, accountingTagUsageTypeId) {
                    @Override protected void onStoreLoad(Store store, Record[] records) {
                        super.onStoreLoad(store, records);
                        // we can only build the tag input fields once this is loaded
                        makeTagInputs(this);
                    }
                };
            registerAutocompleter(tagConfiguration);
        }

        // triggers a new empty record when the grid is loaded so that it is possible to add order items
        setCanCreateNewRow(true);

        // set the primary key fields to distinguish records to be created / to be updated
        setRecordPrimaryKeyFields(Arrays.asList(OrderItemsCartLookupConfiguration.INOUT_ITEM_SEQUENCE));

        // id column
        makeColumn("#", new StringFieldDef(OrderItemsCartLookupConfiguration.INOUT_ITEM_SEQUENCE)).setHidden(true);
        getColumn().setFixed(true);

        // a column for promo flag
        makeColumn("", new StringFieldDef(OrderItemsCartLookupConfiguration.INOUT_IS_PROMO)).setHidden(true);
        getColumn().setFixed(true);

        // product column
        makeEditableColumn(UtilUi.MSG.productProduct(), new StringFieldDef(OrderItemsCartLookupConfiguration.INOUT_PRODUCT), new ProductForCartAutocomplete(productEditor), "{1}:{0}");
        // description column
        makeEditableColumn(UtilUi.MSG.commonDescription(), new StringFieldDef(OrderItemsCartLookupConfiguration.INOUT_DESCRIPTION), new TextField(descriptionEditor)).setWidth(200);
        // quantity column
        makeEditableColumn(UtilUi.MSG.commonQuantity(), new StringFieldDef(OrderItemsCartLookupConfiguration.INOUT_QUANTITY, null, UtilUi.CLEAN_TRAILING_ZERO_CONVERTER), new NumberField(quantityEditor)).setWidth(60);
        getColumn().setFixed(true);
        // unit price
        makeEditableColumn(UtilUi.MSG.commonUnitPrice(), new StringFieldDef(OrderItemsCartLookupConfiguration.INOUT_UNIT_PRICE, null, UtilUi.CLEAN_TRAILING_ZERO_CONVERTER), new ModifierOrNumberField(priceEditor)).setWidth(60);
        getColumn().setFixed(true);
        // item adjustments, only for sales order carts
        if (orderType == OrderType.SALES) {
            makeColumn(UtilUi.MSG.orderAdjustments(), new StringFieldDef(OrderItemsCartLookupConfiguration.INOUT_ADJUSTMENT, null, UtilUi.CLEAN_TRAILING_ZERO_CONVERTER)).setWidth(75);
            getColumn().setFixed(true);
        }

        // the subtotal columns, not linked to the store data but renders according to other cells
        // for each record this calculates the order item subtotal, and for the summary row the order total
        makeColumn(UtilUi.MSG.commonTotal(), new Renderer() {
                public String render(Object value, CellMetadata cellMetadata, Record record, int rowIndex, int colNum, Store store) {
                    // render the invoice total in the summary record
                    if (UtilUi.isSummary(record)) {
                        return "<b>" + getOrderTotal(store.getRecords()).toString() + "</b>";
                    } else {
                        return getItemSubtotal(record).toString();
                    }
                }
            }).setWidth(65);
        getColumn().setFixed(true);

        // the delete column, fixed by default
        makeDeleteColumn(OrderItemsCartLookupConfiguration.INOUT_ITEM_SEQUENCE);

        // use global commit action
        Map<String, String> additionalBatchData = new HashMap<String, String>();
        makeSaveAllButton(OrderItemsCartLookupConfiguration.URL_POST_ORDER_ITEMS_BATCH, additionalBatchData);
        // add a reload button allowing to revert any non committed changes
        makeReloadButton();

        // we need to apply filters before the data is loaded
        setAutoLoad(false);

        // set the use of a summary row
        setUseSummaryRow(true);

        // add the definition needed to support accounting tags
        for (String field : AccountingTagLookupConfiguration.ACCOUNTING_TAGS_FIELDS) {
            addFieldDefinition(new StringFieldDef(field));
        }

        // configures the editable grid now
        configure(OrderItemsCartLookupConfiguration.URL_FIND_ORDER_ITEMS, OrderItemsCartLookupConfiguration.INOUT_ITEM_SEQUENCE, SortDir.ASC);

        // add the final parameter so that the store can load the items for the invoice
        applyFilters();

    }

    private void makeTagInputs(AccountingTagConfigurationStore store) {
        Map<Integer, Map<String, Object>> configuration = store.getIndexedConfiguration();
        for (Integer index : configuration.keySet()) {
            Field f = store.makeTagSelector(index, INPUT_LENGTH);
            if (f != null) {
                form.addField(f);
            }
        }
        form.doLayout();
    }

    /**
     * Counts the number of real and persisted order item records in this grid.
     * @return an <code>int</code> value
     */
    public int getOrderItemsCount() {
        int n = 0;
        for (Record record : getStore().getRecords()) {
            if (!UtilUi.isSummary(record) && !record.isEmpty(OrderItemsCartLookupConfiguration.INOUT_ITEM_SEQUENCE)) {
                n++;
            }
        }
        return n;
    }

    // happen when a specific record field has been edited
    @Override public void cellValueChanged(final Record record, final String field, final Object oldValue, final int rowIndex, final int colIndex) {

        final String productId = record.getAsString(OrderItemsCartLookupConfiguration.INOUT_PRODUCT);
        final String quantityStr = record.getAsString(OrderItemsCartLookupConfiguration.INOUT_QUANTITY);
        boolean qtyChanged = (OrderItemsCartLookupConfiguration.INOUT_QUANTITY.equals(field) && !quantityStr.equals(oldValue));
        boolean productChanged = (OrderItemsCartLookupConfiguration.INOUT_PRODUCT.equals(field) && !productId.equals(oldValue));

        // check that quantity really changed in its numeric value
        if (qtyChanged) {
            BigDecimal quantity = null;
            BigDecimal quantityOld = null;
            if (!UtilUi.isEmpty(quantityStr)) {
                quantity = new BigDecimal(quantityStr);
            }
            if (!UtilUi.isEmpty((String) oldValue)) {
                quantityOld = new BigDecimal((String) oldValue);
            }
            if (quantity != null && quantityOld != null && quantity.compareTo(quantityOld) == 0) {
                qtyChanged = false;
            } else if (quantity == null && quantityOld == null) {
                qtyChanged = false;
            }
        }

        // if no value changed return there
        if (!qtyChanged && !productChanged) {
            return;
        }

        // do not preload info for PO since the actual product info depends on the quantity selected
        if (orderType == OrderType.PURCHASE) {
            // only react if the product ID and quantity are both provided
            if (!UtilUi.isEmpty(productId) && !UtilUi.isEmpty(quantityStr)) {
                loadProductInfoForCart(record, rowIndex, productChanged, qtyChanged);
            } else {
                // set focus to the quantity field, the description will be set once the quantity is set
                stopEditing();
                startEditing(rowIndex, getColumnIndex(OrderItemsCartLookupConfiguration.INOUT_QUANTITY));
            }
        } else {
            // only react if the product ID is provided
            if (!UtilUi.isEmpty(productId)) {
                loadProductInfoForCart(record, rowIndex, productChanged, qtyChanged);
            }
        }
    }

    private void loadProductInfoForCart(final Record record, final int rowIndex, final boolean productChanged, final boolean qtyChanged) {

        final String productId = record.getAsString(OrderItemsCartLookupConfiguration.INOUT_PRODUCT);
        final BigDecimal quantity;
        if (!record.isEmpty(OrderItemsCartLookupConfiguration.INOUT_QUANTITY)) {
            quantity = new BigDecimal(record.getAsString(OrderItemsCartLookupConfiguration.INOUT_QUANTITY));
        } else {
            quantity = null;
        }

        // lock the columns that will get populated
        lockCell(rowIndex, getColumnIndex(OrderItemsCartLookupConfiguration.INOUT_DESCRIPTION));
        lockCell(rowIndex, getColumnIndex(OrderItemsCartLookupConfiguration.INOUT_UNIT_PRICE));
        UtilUi.logDebug("Loading product info for " + productId + " x " + quantity, MODULE, "loadProductInfoForCart");
        markGridBusy();
        new ProductInfoForCartStore(productId, quantity) {
            @Override protected void onStoreLoad(Store store, Record[] records) {
                super.onStoreLoad(store, records);
                // set the loaded values if needed
                record.set(OrderItemsCartLookupConfiguration.INOUT_UNIT_PRICE, UtilUi.removeTrailingZeros(this.getUnitPrice().toString()));
                // change the description only if the product Id changed
                // or if it was empty
                if (productChanged || record.isEmpty(OrderItemsCartLookupConfiguration.INOUT_DESCRIPTION)) {
                    record.set(OrderItemsCartLookupConfiguration.INOUT_DESCRIPTION, this.getDescription());
                }
                unlockCell(rowIndex, getColumnIndex(OrderItemsCartLookupConfiguration.INOUT_DESCRIPTION));
                unlockCell(rowIndex, getColumnIndex(OrderItemsCartLookupConfiguration.INOUT_UNIT_PRICE));
                markGridNotBusy();
                // select the next cell
                // on sales order we load the info after the product id given, check the quantity
                if (record.isEmpty(OrderItemsCartLookupConfiguration.INOUT_QUANTITY)) {
                    stopEditing();
                    startEditing(rowIndex, getColumnIndex(OrderItemsCartLookupConfiguration.INOUT_QUANTITY));
                } else {
                    startEditingNextEditableCell(rowIndex, getColumnIndex(OrderItemsCartLookupConfiguration.INOUT_PRODUCT));
                }
            }

            @Override public void onStoreLoadError(Throwable error) {
                unlockCell(rowIndex, getColumnIndex(OrderItemsCartLookupConfiguration.INOUT_DESCRIPTION));
                unlockCell(rowIndex, getColumnIndex(OrderItemsCartLookupConfiguration.INOUT_UNIT_PRICE));
                // make grid not busy after encounter an error, else will display "wait a while..." pop window endless.
                markGridNotBusy();
                UtilUi.logError("Store load error [" + error + "]", MODULE, "onStoreLoadError");
            }
        };
    }

    // note: only refreshing the summary row lead to bizarre duplicated row onRemove
    private void resetDisplayedTotal() {
        getView().refresh();
    }

    private BigDecimal getOrderTotal(Record[] records) {
        BigDecimal total = new BigDecimal("0");
        for (Record rec : records) {
            total = total.add(getItemSubtotal(rec));
        }
        return total.setScale(2, BigDecimal.ROUND_HALF_EVEN);
    }

    private BigDecimal getItemSubtotal(Record record) {
        BigDecimal qty = null;
        if (!record.isEmpty(OrderItemsCartLookupConfiguration.INOUT_QUANTITY)) {
            qty = UtilUi.asBigDecimal(record.getAsString(OrderItemsCartLookupConfiguration.INOUT_QUANTITY));
        }
        if (qty == null) {
            qty = new BigDecimal("1.0");
        }

        BigDecimal adjustments = null;
        if (!record.isEmpty(OrderItemsCartLookupConfiguration.INOUT_ADJUSTMENT)) {
            adjustments = UtilUi.asBigDecimal(record.getAsString(OrderItemsCartLookupConfiguration.INOUT_ADJUSTMENT));
        }
        if (adjustments == null) {
            adjustments = BigDecimal.ZERO;
        }


        BigDecimal amount = null;
        if (!record.isEmpty(OrderItemsCartLookupConfiguration.INOUT_UNIT_PRICE)) {
            amount = UtilUi.asBigDecimal(record.getAsString(OrderItemsCartLookupConfiguration.INOUT_UNIT_PRICE));
        }
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }

        return qty.multiply(amount).add(adjustments).setScale(2, BigDecimal.ROUND_HALF_EVEN);
    }

    // happens when a cell is edited
    @Override public void onUpdate(Store store, final Record record, Record.Operation operation) {
        super.onUpdate(store, record, operation);

        // other operation are only used internally
        if (operation == Record.EDIT) {
            resetDisplayedTotal();
        }
    }

    // happens when a row is marked for deletion, as it is removed from the grid
    @Override protected void doDeleteAction(Record record) {
        super.doDeleteAction(record);
        resetDisplayedTotal();
    }

    @Override protected String getRowExtraClass(Record record, int index, String extraInfo) {
        String isPromo = record.getAsString(OrderItemsCartLookupConfiguration.INOUT_IS_PROMO);
        if ("Y".equals(isPromo)) {
            return "promoItemRow";
        }
        return null;
    }

    @Override
    public boolean isEditableCell(int rowIndex, int colIndex, String field, Object value) {
        // we can not change order item product after it was selected (have value)
        return (OrderItemsCartLookupConfiguration.INOUT_PRODUCT.equals(field) && !UtilUi.isEmpty((String) value)) ? false : super.isEditableCell(rowIndex, colIndex, field, value);
    }

    /**
     * Gets the form linked to the grid.
     * @return a <code>CreateOrUpdateEntityForm</code> value
     */
    public CreateOrUpdateEntityForm getForm() {
        return form;
    }

}
