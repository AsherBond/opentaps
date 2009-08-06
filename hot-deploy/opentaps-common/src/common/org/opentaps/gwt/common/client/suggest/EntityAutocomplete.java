/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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

package org.opentaps.gwt.common.client.suggest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gwtext.client.core.SortDir;
import com.gwtext.client.core.Template;
import com.gwtext.client.core.UrlParam;
import com.gwtext.client.data.FieldDef;
import com.gwtext.client.data.HttpProxy;
import com.gwtext.client.data.JsonReader;
import com.gwtext.client.data.Record;
import com.gwtext.client.data.RecordDef;
import com.gwtext.client.data.Store;
import com.gwtext.client.data.StringFieldDef;
import com.gwtext.client.data.event.StoreListenerAdapter;
import com.gwtext.client.widgets.form.ComboBox;
import com.gwtext.client.widgets.form.Field;
import com.gwtext.client.widgets.form.event.ComboBoxCallback;
import com.gwtext.client.widgets.form.event.ComboBoxListenerAdapter;
import com.gwtext.client.widgets.form.event.FieldListenerAdapter;
import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.events.LoadableListener;
import org.opentaps.gwt.common.client.listviews.EntityEditableListView;
import org.opentaps.gwt.common.client.lookup.UtilLookup;

/**
 * Base class for ComboBox autocompleters.
 */
public abstract class EntityAutocomplete extends ComboBox {

    private static final String MODULE = EntityAutocomplete.class.getName();

    /** Default width of the widget in pixels. */
    public static final int DEFAULT_WIDTH = 350;

    private Store store;
    private int pageSize;
    private Mode currentMode;
    private String url;
    private JsonReader reader;
    private boolean loaded = false;
    private Map<String, String> filters = new HashMap<String, String>();

    private boolean focused = false;
    private String lastQueried;
    private String lastLoaded;

    // a way to bind an autocompleter back to a Grid specific cell
    // so that the async event loading the value after the store loads
    // can trigger the grid cellValueChanged event
    private Record boundToRecord = null;
    private String boundToRecordField;
    private EntityEditableListView boundToRecordGrid;
    private int boundToRecordRow;
    private int boundToRecordColumn;

    private List<LoadableListener> listeners = new ArrayList<LoadableListener>();

    /** The default record def used for parsing the autocompleter data. */
    public static final RecordDef DEFAULT_RECORD_DEF = new RecordDef(
                 new FieldDef[]{
                     new StringFieldDef(UtilLookup.SUGGEST_ID),
                     new StringFieldDef(UtilLookup.SUGGEST_TEXT)
                 }
         );

    /**
     * Clone constructor, copy its configuration from the given <code>EntityAutocomplete</code>.
     * @param autocompleter the <code>EntityAutocomplete</code> to clone
     */
    public EntityAutocomplete(EntityAutocomplete autocompleter) {
        this(autocompleter.getFieldLabel(), autocompleter.getName(), autocompleter.getWidth());
        // copy the store
        store = new Store(new HttpProxy(autocompleter.getUrl()), autocompleter.getReader(), true);
        store.setSortInfo(autocompleter.getStore().getSortState());
        store.addStoreListener(new StoreListenerAdapter() {
                @Override public void onLoad(Store store, Record[] records) {
                    onStoreLoad(store, records);
                }

                @Override public void onLoadException(Throwable error) {
                    onStoreLoadError(error);
                }
            });
        // init
        init(autocompleter);
        // copy the filters
        applyFilters(autocompleter);
    }

    protected EntityAutocomplete(String label, String name, int width) {
        super(label, name, width);
    }

    /**
     * Editor constructor, do not specify form related parameters.
     * @param url the URL of the service used to query
     */
    public EntityAutocomplete(String url) {
        this(url, null);
    }

    /**
     * Editor constructor, do not specify form related parameters.
     * @param url the URL of the service used to query
     * @param defaultSortField the field to sort by initially
     */
    public EntityAutocomplete(String url, String defaultSortField) {
        this(null, null, 1, url, defaultSortField);
    }

    /**
     * Editor constructor, do not specify form related parameters.
     * @param url the URL of the service used to query
     * @param defaultSortField the field to sort by initially
     * @param recordDef a record def, default to autocompleter default: DEFAULT_RECORD_DEF
     */
    public EntityAutocomplete(String url, String defaultSortField, RecordDef recordDef) {
        this(null, null, 1, url, defaultSortField, SortDir.ASC, recordDef);
    }

    /**
     * Constructor.
     * @param fieldLabel the field label
     * @param name the field name used in the form
     * @param fieldWidth the field size in pixels
     * @param url the URL of the service used to query
     */
    public EntityAutocomplete(String fieldLabel, String name, int fieldWidth, String url) {
        this(fieldLabel, name, fieldWidth, url, null, null);
    }

    /**
     * Constructor.
     * @param fieldLabel the field label
     * @param name the field name used in the form
     * @param fieldWidth the field size in pixels
     * @param url the URL of the service used to query
     * @param defaultSortField the field to sort by initially
     */
    public EntityAutocomplete(String fieldLabel, String name, int fieldWidth, String url, String defaultSortField) {
        this(fieldLabel, name, fieldWidth, url, defaultSortField, SortDir.ASC);
    }

    /**
     * Constructor.
     * @param fieldLabel the field label
     * @param name the field name used in the form
     * @param fieldWidth the field size in pixels
     * @param url the URL of the service used to query
     * @param defaultSortField the field to sort by initially
     * @param defaultSortDirection the default sort direction
     */
    public EntityAutocomplete(String fieldLabel, String name, int fieldWidth, String url, String defaultSortField, SortDir defaultSortDirection) {
        this(fieldLabel, name, fieldWidth, url, defaultSortField, defaultSortDirection, DEFAULT_RECORD_DEF);
    }

    /**
     * Constructor.
     * @param fieldLabel the field label
     * @param name the field name used in the form
     * @param fieldWidth the field size in pixels
     * @param url the URL of the service used to query
     * @param defaultSortField the field to sort by initially
     * @param defaultSortDirection the default sort direction
     * @param recordDef a record def, default to autocompleter default: DEFAULT_RECORD_DEF
     */
    public EntityAutocomplete(String fieldLabel, String name, int fieldWidth, String url, String defaultSortField, SortDir defaultSortDirection, RecordDef recordDef) {
        super(fieldLabel, name, fieldWidth);

        this.url = url;
        final HttpProxy dataProxy = new HttpProxy(url);

        // error icon does not display right with the handle, using qtip instead
        setFieldMsgTarget("qtip");

        reader = new JsonReader(recordDef);
        reader.setRoot(UtilLookup.JSON_ROOT);
        reader.setId(UtilLookup.JSON_ID);
        reader.setTotalProperty(UtilLookup.JSON_TOTAL);
        store = new Store(dataProxy, reader, true);
        store.addStoreListener(new StoreListenerAdapter() {
                @Override public void onLoad(Store store, Record[] records) {
                    onStoreLoad(store, records);
                }

                @Override public void onLoadException(Throwable error) {
                    onStoreLoadError(error);
                }
            });
        if (defaultSortField != null) {
            store.setDefaultSort(defaultSortField, defaultSortDirection);
        }
        init(store);
    }

    protected void onStoreLoad(Store store, Record[] records) {
        loaded = true;
        // assume the event respond to last queried
        lastLoaded = lastQueried;
        UtilUi.logDebug("Store loaded for: " + UtilUi.toString(this) + ", lastQueried = " + lastQueried, MODULE, "onStoreLoad");
        // check if the current value is found in the store, but only if the field is not being edited
        if (!focused) {
            checkValueFound("onStoreLoad");
        }
        // send notification to listeners
        for (LoadableListener l : listeners) {
            l.onLoad();
        }
    }

    protected void onStoreLoadError(Throwable error) {
        UtilUi.logError("Store load error [" + error + "] for: " + UtilUi.toString(this), MODULE, "onStoreLoad");
    }

    /**
     * Gets a copy of the currently applied filters.
     * @return a copied Map of the currently applied filters
     */
    public Map<String, String> getFilters() {
        return new HashMap<String, String>(filters);
    }

    /**
     * Copies the applied filters from another <code>EntityAutocomplete</code>.
     * @param autocompleter the autocompleter to copy the filters from
     */
    protected void applyFilters(EntityAutocomplete autocompleter) {
        applyFilters(autocompleter.getFilters());
    }

    /**
     * Applies an additional filters by adding a parameter, value pair to the query.
     * @param filters a <code>Map</code> of field name: value to filter by
     */
    protected void applyFilters(Map<String, String> filters) {
        for (String field : filters.keySet()) {
            applyFilter(field, filters.get(field));
        }
    }

    /**
     * Applies an additional filter by adding a parameter, value pair to the query.
     * This is used for example to filter using another UI element value.
     * @param fieldToFilter the name of the field
     * @param filter the value to filter by
     */
    protected void applyFilter(String fieldToFilter, String filter) {
        filters.put(fieldToFilter, filter);
        List<UrlParam> params = new ArrayList<UrlParam>();
        for (String f : filters.keySet()) {
            params.add(new UrlParam(f, filters.get(f)));
        }
        UrlParam[] urlParams = new UrlParam[params.size()];
        getStore().setBaseParams(params.toArray(urlParams));
    }

    /**
     * Reloads the store at the first page.
     * Can be used for example after applying a filter.
     */
    public void loadFirstPage() {
        List<UrlParam> params = new ArrayList<UrlParam>();
        params.add(new UrlParam("start", 0));
        params.add(new UrlParam("limit", pageSize));
        UrlParam[] urlParams = new UrlParam[params.size()];
        getStore().reload(params.toArray(urlParams));
    }

    /**
     * Links this autocompleter to another combo box.
     * When the target combo box changes, its value is used as a
     *  filter on the given field, and the store reloaded.
     * @param comboBox
     * @param fieldToFilter
     */
    protected void linkToComboBox(ComboBox comboBox, final String fieldToFilter) {
        // initialize the filter to an empty value
        applyFilter(fieldToFilter, "");
        // make the link
        comboBox.addListener(new ComboBoxListenerAdapter() {

                @Override public void onSelect(ComboBox cb, Record record, int index) {
                    setValue("");
                    String val = cb.getValue();
                    if (val == null) {
                        applyFilter(fieldToFilter, "");
                    } else {
                        applyFilter(fieldToFilter, val);
                    }
                    loadFirstPage();
                }

            });
    }

    /**
     * Checks if the store has been loaded (loading is asynchronous).
     * @return a <code>boolean</code> value
     */
    public boolean isLoaded() {
        return loaded;
    }

    protected void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    /**
     * Makes the autocompleter static by forcing the load of all available records.
     */
    public void makeStatic() {
        setPageSize(0);
        applyFilter(UtilLookup.PARAM_NO_PAGER, "Y");
        setMode(LOCAL);
        loadFirstPage();
    }

    @Override
    public void setPageSize(int p) {
        super.setPageSize(p);
        pageSize = p;
    }

    /**
     * Gets the current page size.
     * @return the current page size
     */
    public int getPageSize() {
        return pageSize;
    }

    @Override
    public void setMode(Mode mode) {
        super.setMode(mode);
        currentMode = mode;
    }

    /**
     * Gets the current mode, either LOCAL or REMOTE.
     * @return the current mode
     */
    public Mode getMode() {
        return currentMode;
    }

    /**
     * Gets the URL where the store gets it's data.
     * @return the URL string
     */
    public String getUrl() {
        return url;
    }

    /**
     * Gets the <code>JsonReader</code> used to read the server response.
     * @return the <code>JsonReader</code>
     */
    public JsonReader getReader() {
        return reader;
    }

    @Override
    public void setAllowBlank(boolean allowBlank) {
        super.setAllowBlank(allowBlank);
        if (allowBlank) {
            applyFilter(UtilLookup.PARAM_NO_BLANK, "N");
        } else {
            applyFilter(UtilLookup.PARAM_NO_BLANK, "Y");
        }
    }

    /**
     * Registers a <code>LoadableListener</code>.
     * @param listener a <code>LoadableListener</code> value
     */
    public void addLoadableListener(LoadableListener listener) {
        listeners.add(listener);
    }

    protected void init(EntityAutocomplete autocompleter) {
        init(store, autocompleter.getMode(), autocompleter.getPageSize());
    }

    protected void init(Store store, EntityAutocomplete autocompleter) {
        init(store, autocompleter.getMode(), autocompleter.getPageSize());
    }

    protected void init(Store store) {
        init(store, REMOTE, UtilLookup.DEFAULT_SUGGEST_PAGE_SIZE);
    }

    protected void init(Store store, Mode mode, int pageSize) {
        // start autocompleting after the first char
        setMinChars(1);
        setStore(store);
        setTriggerAction(ComboBox.ALL);
        setMode(mode);
        setEmptyText(UtilUi.MSG.suggestEmpty());
        setLoadingText(UtilUi.MSG.suggestSearching());
        // type ahead may change the input already typed which can be confusing
        // as it replace the text with the first match, which is not necessarily
        // only matching the beginning of the string
        // the service could also match from other fields than the one displayed in
        // the combo box
        setTypeAhead(false);
        setSelectOnFocus(true);
        setPageSize(pageSize);
        // allow the user to resize the popup list
        setListWidth(DEFAULT_WIDTH);
        setResizable(true);
        // although it would be nice to use, it does not allow to set the value
        // back to blank
        setForceSelection(false);
        setDisplayField(UtilLookup.SUGGEST_TEXT);
        setValueField(UtilLookup.SUGGEST_ID);
        // this template allows empty values to render correctly in the drop down by adding a nbsp
        setTpl(new Template("<div class=\"x-combo-list-item\">{" + UtilLookup.SUGGEST_TEXT + "}&nbsp;</div>"));

        // add a listener to check what value the combo box is querying for
        addListener(new ComboBoxListenerAdapter() {
                @Override public boolean doBeforeQuery(ComboBox comboBox, ComboBoxCallback cb) {
                    lastQueried = comboBox.getRawValue();
                    return true;
                }
            });

        // there seems to be a bug: when editing the text to "" it will not trigger the
        // selection of the empty value, this is a workaround
        addListener(new FieldListenerAdapter() {
                @Override public void onFocus(Field field) {
                    setFocused(true);
                }

                @Override public void onBlur(Field f) {
                    setFocused(false);
                }

                @Override public void onChange(Field field, Object newVal, Object oldVal) {
                    UtilUi.logDebug("Changed " + UtilUi.toString(this) + " from [" + oldVal + "] to [" + newVal + "]", MODULE, "onChange");
                }
            });
    }

    /**
     * Binds the autocompleter to a <code>Record</code> so when the store load if a value can be set it will update the bound <code>Record</code>.
     * @param record a <code>Record</code> value
     * @param field the name of the <code>Record</code> field
     * @param grid an <code>EntityEditableListView</code> value
     * @param row an <code>int</code> value
     * @param col an <code>int</code> value
     */
    public void bindToRecord(Record record, String field, EntityEditableListView grid, int row, int col) {
        boundToRecord = record;
        boundToRecordField = field;
        boundToRecordGrid = grid;
        boundToRecordRow = row;
        boundToRecordColumn = col;
    }

    /**
     * Sets the internal focused flag.
     * Used to determine if the editor is currently focused and if it should auto match its value to the store or not.
     * When un-focusing the editor checks its current value against the store, allowing to match the user entered ID to a store record and sets the proper values.
     * @param focused a <code>boolean</code> value
     */
    public void setFocused(boolean focused) {
        UtilUi.logDebug("Setting focus flag for: " + UtilUi.toString(this) + " to : " + focused, MODULE, "setFocused");
        this.focused = focused;

        String rawValue = getRawValue();
        String value = getValue();
        UtilUi.logDebug("Got raw value for: " + UtilUi.toString(this) + " : " + rawValue + ", current value : " + value, MODULE, "setFocused");

        // if we are unfocusing, then try to find the value and set it.   This allows the user to enter a value, then not wait for auto complete and move on to the next field.
        if (!focused) {
            boolean found = false;

            // if the raw value is empty, clear the selection now
            if ("".equals(rawValue)) {
                setValue("");
                found = true;
            }

            if (!found) {
                checkValueFound("onBlur");
            }

            UtilUi.logDebug("Got raw value for: " + UtilUi.toString(this) + " : " + getRawValue() + ", current value : " + getValue(), MODULE, "onBlur");
        }
    }

    private void checkValueFound(String originMethod) {

        String rawValue = getRawValue();
        String value = getValue();
        boolean found = false;

        // store need to be obtained with getStore() to work with derived classes
        Store s = getStore();

        // first check if the current raw value / value is matching a record in the store
        if (!found && !UtilUi.isEmpty(value) && !UtilUi.isEmpty(rawValue)) {
            int idx = s.find(UtilLookup.SUGGEST_TEXT, rawValue, 0, /* anymatch */ false, /* case sensitive */ true);
            UtilUi.logDebug("Found match index = " + idx, MODULE, originMethod);
            if (idx > -1) {
                UtilUi.logDebug(" Found match for the description with : " + rawValue, MODULE, originMethod);
                Record rec = s.getRecordAt(idx);
                String recId = rec.getAsString(UtilLookup.SUGGEST_ID);
                String recDescription = rec.getAsString(UtilLookup.SUGGEST_TEXT);
                if (value.equals(recId)) {
                    UtilUi.logDebug(UtilUi.toString(this) + " [" + rawValue + "] is a perfect match in the store, this was selected from the drop-down.", MODULE, originMethod);
                    found = true;
                } else {
                    UtilUi.logDebug(UtilUi.toString(this) + " [" + rawValue + "] is matching a record description in the store, but the corresponding id does not match the value : " + value + "; setting the value.", MODULE, originMethod);
                    setValues(recId, recDescription);
                    found = true;
                }
            } else {
                UtilUi.logDebug(UtilUi.toString(this) + " [" + rawValue + "] is not matching any record description in the store.", MODULE, originMethod);
            }
        }

        // then check if the raw value matches any ID in the store now, this happens when the user types an ID
        if (!found && !UtilUi.isEmpty(rawValue)) {
            int idx = s.find(UtilLookup.SUGGEST_ID, rawValue, 0, /* anymatch */ false, /* case sensitive */ false);
            if (idx > -1) {
                UtilUi.logDebug(" Found match for the ID with : " + rawValue, MODULE, originMethod);
                Record rec = s.getRecordAt(idx);
                String recId = rec.getAsString(UtilLookup.SUGGEST_ID);
                String recDescription = rec.getAsString(UtilLookup.SUGGEST_TEXT);
                UtilUi.logDebug(UtilUi.toString(this) + " [" + rawValue + "] is matching record ID [" + recId + "] in the store, setting the corresponding description [" + recDescription + "] and value.", MODULE, originMethod);
                setValues(recId, recDescription);
                found = true;
            } else {
                // if the store already loaded the result for this value
                if (rawValue.equals(lastQueried) && rawValue.equals(lastLoaded)) {
                    UtilUi.logDebug(UtilUi.toString(this) + " " + rawValue + " is not matching any record ID in the store and the query was made, assuming this is a bad value and resetting.", MODULE, originMethod);
                    setValues("", "");
                } else {
                    UtilUi.logDebug(UtilUi.toString(this) + " " + rawValue + " is not matching any record ID in the store, but the query was not made yet.", MODULE, originMethod);
                }
            }
        }
    }

    private void setValues(String id, String description) {
        String oldValue = getValue();
        setValue(id);
        setRawValue(description);
        if (boundToRecord != null) {
            UtilUi.logDebug(UtilUi.toString(this) + " setting id [" + id + "] description [" + description + "] to the bound record field [" + boundToRecordField + "].", MODULE, "setValues");
            boundToRecord.set(boundToRecordField, id);
            boundToRecord.set(boundToRecordField + UtilLookup.DESCRIPTION_SUFFIX, description);
            boundToRecordGrid.cellValueChanged(boundToRecord, boundToRecordField, oldValue, boundToRecordRow, boundToRecordColumn);
        }

    }
}
