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
import org.opentaps.gwt.common.client.form.field.CheckboxField;
import org.opentaps.gwt.common.client.form.field.HiddenField;
import org.opentaps.gwt.common.client.form.field.NumberField;
import org.opentaps.gwt.common.client.form.field.TextField;
import org.opentaps.gwt.common.client.lookup.configuration.AccountingTagLookupConfiguration;
import org.opentaps.gwt.common.client.lookup.configuration.InvoiceItemLookupConfiguration;
import org.opentaps.gwt.common.client.suggest.AccountingTagConfigurationStore;
import org.opentaps.gwt.common.client.suggest.GlAccountAutocomplete;
import org.opentaps.gwt.common.client.suggest.InvoiceItemTypeAutocomplete;
import org.opentaps.gwt.common.client.suggest.ProductForCartAutocomplete;
import org.opentaps.gwt.common.client.suggest.ProductInfoForInvoiceStore;
import org.opentaps.gwt.common.client.suggest.TaxAuthorityAutocomplete;

import com.gwtext.client.core.SortDir;
import com.gwtext.client.data.Record;
import com.gwtext.client.data.Store;
import com.gwtext.client.data.StringFieldDef;
import com.gwtext.client.widgets.form.Checkbox;
import com.gwtext.client.widgets.form.Field;
import com.gwtext.client.widgets.form.event.CheckboxListenerAdapter;
import com.gwtext.client.widgets.grid.CellMetadata;
import com.gwtext.client.widgets.grid.Renderer;

/**
 * List of Invoice Items.
 */
public class InvoiceItemEditableListView extends EntityEditableListView {

    private static final Integer INPUT_LENGTH = 150;

    private String invoiceId;
    private String organizationPartyId;
    private String invoiceTypeId;

    private CreateOrUpdateEntityForm form;
    private AccountingTagConfigurationStore tagConfiguration;
    private boolean hasProductInput;

    private final InvoiceItemTypeAutocomplete itemTypeEditor;
    private final TextField descriptionEditor;
    private final GlAccountAutocomplete glAccountEditor;
    private final ProductForCartAutocomplete productEditor;
    private final NumberField quantityEditor;
    private final NumberField amountEditor;
    private final TaxAuthorityAutocomplete taxAuthEditor;
    private final CheckboxField taxableEditor;

    /**
     * Constructor giving a title for this list view, which is displayed in the UI.
     * @param title the title label for this list view.
     * @param invoiceId a <code>String</code> value
     * @param organizationPartyId a <code>String</code> value
     * @param invoiceTypeId a <code>String</code> value
     */
    public InvoiceItemEditableListView(String title, String invoiceId, String organizationPartyId, String invoiceTypeId) {
        super(title);
        this.invoiceId = invoiceId;
        this.organizationPartyId = organizationPartyId;
        this.invoiceTypeId = invoiceTypeId;
        this.hasProductInput = InvoiceItemLookupConfiguration.WITH_PRODUCT_INVOICE_TYPES.contains(invoiceTypeId);

        itemTypeEditor = new InvoiceItemTypeAutocomplete(UtilUi.MSG.commonType(), InvoiceItemLookupConfiguration.INOUT_ITEM_TYPE, INPUT_LENGTH, organizationPartyId, invoiceTypeId);
        descriptionEditor = new TextField(UtilUi.MSG.commonDescription(), InvoiceItemLookupConfiguration.INOUT_DESCRIPTION, INPUT_LENGTH);
        glAccountEditor = new GlAccountAutocomplete(UtilUi.MSG.accountingGlAccount(), InvoiceItemLookupConfiguration.INOUT_GL_ACCOUNT, INPUT_LENGTH);
        productEditor = new ProductForCartAutocomplete(UtilUi.MSG.productProduct(), InvoiceItemLookupConfiguration.INOUT_PRODUCT, INPUT_LENGTH);
        quantityEditor = new NumberField(UtilUi.MSG.commonQuantity(), InvoiceItemLookupConfiguration.INOUT_QUANTITY, INPUT_LENGTH);
        quantityEditor.setAllowNegative(false);
        amountEditor = new NumberField(UtilUi.MSG.commonAmount(), InvoiceItemLookupConfiguration.INOUT_AMOUNT, INPUT_LENGTH);
        taxAuthEditor = new TaxAuthorityAutocomplete(UtilUi.MSG.accountingTaxAuthority(), InvoiceItemLookupConfiguration.INOUT_TAX_AUTH, INPUT_LENGTH);
        taxableEditor = new CheckboxField(UtilUi.MSG.financialsIsTaxable(), InvoiceItemLookupConfiguration.INOUT_TAXABLE);
        taxableEditor.addListener(new CheckboxListenerAdapter() {
                @Override public void onCheck(Checkbox field, boolean checked) {
                    // note: changing the field visibility does not trigger a re-size event
                    // so we need a workaround
                    if (checked) {
                        taxAuthEditor.show();
                    } else {
                        taxAuthEditor.hide();
                    }
                    form.syncSize();
                }
            });

        form = new CreateOrUpdateEntityForm(false) {
                @Override protected void onSuccess() {
                    loadFirstPage();
                    super.onSuccess();
                }
            };
        // redirect form effects to the grid
        form.setEffectsTarget(this);
        form.setBorder(false);
        form.setUrl(InvoiceItemLookupConfiguration.URL_POST_INVOICE_ITEMS);
        form.addField(new HiddenField(InvoiceItemLookupConfiguration.INOUT_INVOICE_ID, invoiceId));
        form.addField(new HiddenField(InvoiceItemLookupConfiguration.INOUT_ITEM_SEQUENCE));
        form.addField(itemTypeEditor);
        form.addField(descriptionEditor);
        form.addField(glAccountEditor);
        if (hasProductInput) {
            form.addField(productEditor);
        }
        form.addField(quantityEditor);
        form.addField(amountEditor);
        form.addField(taxableEditor);
        form.addField(taxAuthEditor);
        // form is hidden by default, will show when a valid record is selected
        // and hide again if an invalid record is selected
        form.setHideMode("visibility");
        form.setVisible(false);
        init();
        this.bindToForm(form);
    }

    protected void init() {
        // load the accounting tag configuration
        String accountingTagUsageTypeId = AccountingTagConfigurationStore.getUsageTypeFor(invoiceTypeId);
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

        // triggers a new empty record when the grid is loaded so that it is possible to add invoice items
        setCanCreateNewRow(true);

        // set the primary key fields to distinguish records to be created / to be updated
        setRecordPrimaryKeyFields(Arrays.asList(InvoiceItemLookupConfiguration.INOUT_ITEM_SEQUENCE));

        // id column, made smaller and fixed as the string displayed is always the same length
        makeLinkColumn("#", new StringFieldDef(InvoiceItemLookupConfiguration.INOUT_ITEM_SEQUENCE), "/financials/control/updateInvoiceItemForm?invoiceId=" + invoiceId + "&invoiceItemSeqId={0}").setWidth(45);
        getColumn().setFixed(true);

        makeEditableColumn(UtilUi.MSG.commonType(), new StringFieldDef(InvoiceItemLookupConfiguration.INOUT_ITEM_TYPE), new InvoiceItemTypeAutocomplete(itemTypeEditor));
        if (hasProductInput) {
            makeEditableColumn(UtilUi.MSG.productProduct(), new StringFieldDef(InvoiceItemLookupConfiguration.INOUT_PRODUCT), new ProductForCartAutocomplete(productEditor), "{1}:{0}");
        }

        // description column, made larger
        makeEditableColumn(UtilUi.MSG.commonDescription(), new StringFieldDef(InvoiceItemLookupConfiguration.INOUT_DESCRIPTION), new TextField(descriptionEditor)).setWidth(200);

        // GL account column, hidden by default
        makeEditableColumn(UtilUi.MSG.accountingGlAccount(), new StringFieldDef(InvoiceItemLookupConfiguration.INOUT_GL_ACCOUNT), new GlAccountAutocomplete(glAccountEditor), "{1}:{0}").setHidden(true);
        // tax columns, hidden by default
        makeEditableColumn(UtilUi.MSG.financialsIsTaxable(), new StringBooleanFieldDef(InvoiceItemLookupConfiguration.INOUT_TAXABLE), new CheckboxField(taxableEditor)).setWidth(65);
        getColumn().setHidden(true);
        makeEditableColumn(UtilUi.MSG.accountingTaxAuthority(), new StringFieldDef(InvoiceItemLookupConfiguration.INOUT_TAX_AUTH), new TaxAuthorityAutocomplete(taxAuthEditor)).setHidden(true);

        // quantity and amount, made smaller
        makeEditableColumn(UtilUi.MSG.commonQuantity(), new StringFieldDef(InvoiceItemLookupConfiguration.INOUT_QUANTITY), new NumberField(quantityEditor)).setWidth(60);
        getColumn().setFixed(true);
        makeEditableColumn(UtilUi.MSG.commonAmount(), new StringFieldDef(InvoiceItemLookupConfiguration.INOUT_AMOUNT), new NumberField(amountEditor)).setWidth(60);
        getColumn().setFixed(true);

        // the subtotal columns, not linked to the store data but renders according to other cells
        // for each record this calculates the invoice item subtotal, and for the summary row the invoice total
        makeColumn(UtilUi.MSG.commonTotal(), new Renderer() {
                public String render(Object value, CellMetadata cellMetadata, Record record, int rowIndex, int colNum, Store store) {
                    // render the invoice total in the summary record
                    if (UtilUi.isSummary(record)) {
                        return "<b>" + getInvoiceTotal(store.getRecords()).toString() + "</b>";
                    } else {
                        return getItemSubtotal(record).toString();
                    }
                }
            }).setWidth(65);
        getColumn().setFixed(true);

        // the delete column, fixed by default
        makeDeleteColumn(InvoiceItemLookupConfiguration.INOUT_ITEM_SEQUENCE);

        // use global commit action, add the invoice id to each record since it is not in the record data
        Map<String, String> additionalBatchData = new HashMap<String, String>();
        additionalBatchData.put(InvoiceItemLookupConfiguration.INOUT_INVOICE_ID, invoiceId);
        makeSaveAllButton(InvoiceItemLookupConfiguration.URL_POST_INVOICE_ITEMS_BATCH, additionalBatchData);
        // add a reload button allowing to revert any non committed changes
        makeReloadButton();

        // set default values for new records
        setDefaultValue(InvoiceItemLookupConfiguration.INOUT_TAXABLE, Boolean.FALSE);

        // we need to apply filters before the data is loaded
        setAutoLoad(false);

        // set the use of a summary row
        setUseSummaryRow(true);

        // add the definition needed to support accounting tags
        for (String field : AccountingTagLookupConfiguration.ACCOUNTING_TAGS_FIELDS) {
            addFieldDefinition(new StringFieldDef(field));
        }

        // configures the editable grid now
        configure(InvoiceItemLookupConfiguration.URL_FIND_INVOICE_ITEMS, InvoiceItemLookupConfiguration.INOUT_ITEM_SEQUENCE, SortDir.ASC);

        // add the final parameter so that the store can load the items for the invoice
        setFilter(InvoiceItemLookupConfiguration.INOUT_INVOICE_ID, invoiceId);
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

    // note: only refreshing the summary row lead to bizarre duplicated row onRemove
    private void resetDisplayedTotal() {
        getView().refresh();
    }

    private BigDecimal getInvoiceTotal(Record[] records) {
        BigDecimal total = new BigDecimal("0");
        for (Record rec : records) {
            total = total.add(getItemSubtotal(rec));
        }
        return total.setScale(2, BigDecimal.ROUND_HALF_EVEN);
    }

    private BigDecimal getItemSubtotal(Record record) {
        BigDecimal qty;
        if (record.isEmpty(InvoiceItemLookupConfiguration.INOUT_QUANTITY)) {
            qty = new BigDecimal("1.0");
        } else {
            qty = new BigDecimal(record.getAsString(InvoiceItemLookupConfiguration.INOUT_QUANTITY));
        }
        BigDecimal amount;
        if (record.isEmpty(InvoiceItemLookupConfiguration.INOUT_AMOUNT)) {
            amount = new BigDecimal("0.0");
        } else {
            amount = new BigDecimal(record.getAsString(InvoiceItemLookupConfiguration.INOUT_AMOUNT));
        }
        return qty.multiply(amount).setScale(2, BigDecimal.ROUND_HALF_EVEN);
    }

    // happen when a specific record field has been edited
    @Override public void cellValueChanged(final Record record, final String field, final Object oldValue, final int rowIndex, final int colIndex) {

        // check if the product ID is filled (it may be empty for non product items)
        if (record.isEmpty(InvoiceItemLookupConfiguration.INOUT_PRODUCT)) {
            return;
        }

        // only react if the product ID changed
        BigDecimal quantity = null;
        final String productId = record.getAsString(InvoiceItemLookupConfiguration.INOUT_PRODUCT);
        final String quantityStr = record.getAsString(InvoiceItemLookupConfiguration.INOUT_QUANTITY);
        boolean qtyChanged = (InvoiceItemLookupConfiguration.INOUT_QUANTITY.equals(field) && !quantityStr.equals(oldValue));
        boolean productChanged = (InvoiceItemLookupConfiguration.INOUT_PRODUCT.equals(field) && !productId.equals(oldValue));
        // check that quantity really changed in its numeric value
        if (qtyChanged) {
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

        // lock the columns that will get populated
        lockCell(rowIndex, getColumnIndex(InvoiceItemLookupConfiguration.INOUT_ITEM_TYPE));
        lockCell(rowIndex, getColumnIndex(InvoiceItemLookupConfiguration.INOUT_DESCRIPTION));
        lockCell(rowIndex, getColumnIndex(InvoiceItemLookupConfiguration.INOUT_AMOUNT));

        // remove focus from the current cell, until the loading is complete. this is to avoid the user starting to edit a cell but it's value get overwritten
        getCellSelectionModel().clearSelections();
        stopEditing();
        new ProductInfoForInvoiceStore(productId, invoiceId, quantity) {
            @Override protected void onStoreLoad(Store store, Record[] records) {
                super.onStoreLoad(store, records);
                // set the loaded values
                record.set(InvoiceItemLookupConfiguration.INOUT_ITEM_TYPE, this.getInvoiceItemTypeId());
                record.set(InvoiceItemLookupConfiguration.INOUT_DESCRIPTION, this.getDescription());
                if (this.getUnitPrice() != null) {
                    record.set(InvoiceItemLookupConfiguration.INOUT_AMOUNT, this.getUnitPrice().toString());
                }
                unlockCell(rowIndex, getColumnIndex(InvoiceItemLookupConfiguration.INOUT_ITEM_TYPE));
                unlockCell(rowIndex, getColumnIndex(InvoiceItemLookupConfiguration.INOUT_DESCRIPTION));
                unlockCell(rowIndex, getColumnIndex(InvoiceItemLookupConfiguration.INOUT_AMOUNT));
                // focus on the quantity cell most likely to be edited next if it is empty
                if (record.isEmpty(InvoiceItemLookupConfiguration.INOUT_QUANTITY)) {
                    startEditing(rowIndex, getColumnIndex(InvoiceItemLookupConfiguration.INOUT_QUANTITY));
                } else {
                    // else just focus on the next column
                    startEditing(rowIndex, colIndex + 1);
                }
            }

            @Override public void onStoreLoadError(Throwable error) {
                unlockCell(rowIndex, getColumnIndex(InvoiceItemLookupConfiguration.INOUT_ITEM_TYPE));
                unlockCell(rowIndex, getColumnIndex(InvoiceItemLookupConfiguration.INOUT_DESCRIPTION));
                unlockCell(rowIndex, getColumnIndex(InvoiceItemLookupConfiguration.INOUT_AMOUNT));
            }
        };
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

    @Override protected String getRowBody(Record record, int index) {
        String body = "";
        for (int i = 1; i <= AccountingTagLookupConfiguration.NUM_ACCOUNTING_TAG; i++) {
            String f = AccountingTagLookupConfiguration.BASE_ACCOUNTING_TAG + i;
            if (!record.isEmpty(f)) {
                body += " <b>" + tagConfiguration.getTagDescription(i) + ":</b>&nbsp; " + tagConfiguration.getTagValueDescription(i, record.getAsString(f));
            }
        }
        return body;
    }

    /**
     * Counts the number of real and persisted invoice item records in this grid.
     * @return an <code>int</code> value
     */
    public int getInvoiceItemsCount() {
        int n = 0;
        for (Record record : getStore().getRecords()) {
            if (!UtilUi.isSummary(record) && !record.isEmpty(InvoiceItemLookupConfiguration.INOUT_ITEM_SEQUENCE)) {
                n++;
            }
        }
        return n;
    }

    /**
     * Gets the form linked to the grid.
     * @return a <code>CreateOrUpdateEntityForm</code> value
     */
    public CreateOrUpdateEntityForm getForm() {
        return form;
    }
}
