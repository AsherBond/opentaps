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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.gwtext.client.core.EventObject;
import com.gwtext.client.core.Function;
import com.gwtext.client.core.SortDir;
import com.gwtext.client.core.UrlParam;
import com.gwtext.client.data.BooleanFieldDef;
import com.gwtext.client.data.FieldDef;
import com.gwtext.client.data.GroupingStore;
import com.gwtext.client.data.HttpProxy;
import com.gwtext.client.data.JsonReader;
import com.gwtext.client.data.Record;
import com.gwtext.client.data.RecordDef;
import com.gwtext.client.data.Store;
import com.gwtext.client.data.StringFieldDef;
import com.gwtext.client.data.event.StoreListener;
import com.gwtext.client.widgets.Button;
import com.gwtext.client.widgets.PagingToolbar;
import com.gwtext.client.widgets.ToolTip;
import com.gwtext.client.widgets.ToolbarButton;
import com.gwtext.client.widgets.event.ButtonListenerAdapter;
import com.gwtext.client.widgets.form.ComboBox;
import com.gwtext.client.widgets.form.Field;
import com.gwtext.client.widgets.form.NumberField;
import com.gwtext.client.widgets.form.event.ComboBoxListenerAdapter;
import com.gwtext.client.widgets.form.event.FieldListenerAdapter;
import com.gwtext.client.widgets.grid.ColumnConfig;
import com.gwtext.client.widgets.grid.ColumnModel;
import com.gwtext.client.widgets.grid.EditorGridPanel;
import com.gwtext.client.widgets.grid.GridEditor;
import com.gwtext.client.widgets.grid.GridPanel;
import com.gwtext.client.widgets.grid.GroupingView;
import com.gwtext.client.widgets.grid.Renderer;
import com.gwtext.client.widgets.grid.RowParams;
import com.gwtext.client.widgets.grid.RowSelectionModel;
import com.gwtext.client.widgets.grid.event.EditorGridListenerAdapter;
import com.gwtext.client.widgets.grid.event.GridCellListenerAdapter;
import com.gwtext.client.widgets.grid.event.RowSelectionListenerAdapter;
import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.events.LoadableListener;
import org.opentaps.gwt.common.client.events.LoadableListenerAdapter;
import org.opentaps.gwt.common.client.form.FormNotificationInterface;
import org.opentaps.gwt.common.client.form.ServiceErrorReader;
import org.opentaps.gwt.common.client.form.base.BaseFormPanel;
import org.opentaps.gwt.common.client.form.field.ValuePostProcessedInterface;
import org.opentaps.gwt.common.client.lookup.Permissions;
import org.opentaps.gwt.common.client.lookup.UtilLookup;
import org.opentaps.gwt.common.client.suggest.EntityAutocomplete;
import org.opentaps.gwt.common.client.suggest.EntityStaticAutocomplete;

/**
 * The base class for tables that list entities and that support AJAX
 * sorting, pagination, filtering, and in-place edition.
 */
public abstract class EntityEditableListView extends EditorGridPanel implements FormNotificationInterface<Object>, StoreListener {

    private static final String MODULE = EntityEditableListView.class.getName();

    private PagingToolbar pagingToolbar;
    private ColumnModel columnModel;
    private Map<String, String> filters = new HashMap<String, String>();
    private Map<String, String> stickyFilters = new HashMap<String, String>();

    private HttpProxy proxy;
    private JsonReader reader;
    private String queryUrl;
    private GroupingStore store;
    private RecordDef recordDef;
    private RowSelectionModel selectionModel = new RowSelectionModel(true);

    private Button saveAllButton;
    private Button revertButton;

    private Set<String> recordPrimaryKeyFields;
    private Set<FieldDef> fieldDefinitions = new HashSet<FieldDef>();
    private List<ColumnConfig> columnConfigs = new ArrayList<ColumnConfig>();
    private List<LinkColumnConfig> lookupColumns = new ArrayList<LinkColumnConfig>();

    // keeps track of the autocompleters that we should wait to be loaded before loading the grid
    // see the timers below
    private List<EntityAutocomplete> autocompletes = new ArrayList<EntityAutocomplete>();

    // used to store the latest combo box changed display value
    // and so that once a cell is edited, the autocompleter display string is displayed instead of the real value
    private boolean displayStringChanged = false;
    private String displayString;

    // if the buttons are created with makeCreateUpdateColumn / makeDeleteColumn those will have the column index set
    // they are used in the cell click handler to figure out which button was clicked
    private int createUpdateIndex = -1;
    private int deleteIndex = -1;

    // default values for creating a new row
    // use setDefaultValue to override the default nulls
    // use setFirstEditableColumn to set which column editor should open after a new row was inserted
    private Object[] defaultValuesArray;
    private Map<String, Object> defaultValues = new HashMap<String, Object>();

    // use canCreateNewRow to allow record creation
    private Boolean canCreateNewRow;

    // the global permissions as parsed from the service response
    private Permissions globalPermissions;

    // set this to false to force a non editable grid
    private boolean editable = true;

    // set this to true to use a paging toolbar
    private boolean usePagingToolbar = false;

    // set to auto insert a summary row on load
    private boolean useSummaryRow = false;

    // set this to false if you need to apply filters before loading the data in order to avoid loading the data twice
    private boolean autoLoad = true;

    // the default page size when the grid and pager are initialized
    private int defaultPageSize = UtilLookup.DEFAULT_LIST_PAGE_SIZE;
    // the pagingToolbar.setPageSize method is not working properly, so we use this value as a workaround
    private int pageSize = -1;
    private NumberField pageSizeField;

    private List<LoadableListener> listeners = new ArrayList<LoadableListener>();

    // the URL to post batch data to, set when adding the save all button
    private String saveAllUrl;
    // additional data to be added to each record when batch posted
    private Map<String, String> additionalBatchData;

    // store Records for batch delete actions
    private List<Record> toDeleteRecords = new ArrayList<Record>();

    // a way to lock cells to prevent the user to edit them, normally used when those cells
    // are waiting to be filled by an Ajax event
    private Set<Cell> lockedCells = new HashSet<Cell>();

    // record autocompleter editors to columns
    private Map<String, EntityAutocomplete> columnToAutocompleter = new HashMap<String, EntityAutocomplete>();

    // record post processed editors to columns
    private Map<String, ValuePostProcessedInterface> columnToPostProcessed = new HashMap<String, ValuePostProcessedInterface>();

    private boolean loaded = false;
    private boolean loadNow = false;

    // if set the grid will use a GroupingStore and GroupingView
    private String groupField = null;
    private String groupTemplate = null;

    /** An internal class representing a Cell in the Grid. */
    public static class Cell {
        private Integer rowIndex;
        private Integer colIndex;
        /**
         * Creates a new <code>Cell</code> instance.
         * @param rowIndex the row coordinate
         * @param colIndex the column coordinate
         */
        public Cell(int rowIndex, int colIndex) {
            this.rowIndex = rowIndex;
            this.colIndex = colIndex;
        }
        /**
         * Gets the row coordinate.
         * @return an <code>int</code> value
         */
        public int getRowIndex() { return this.rowIndex; }
        /**
         * Gets the column coordinate.
         * @return an <code>int</code> value
         */
        public int getColIndex() { return this.colIndex; }
        @Override public int hashCode() {
            return rowIndex.hashCode() * 31 + colIndex.hashCode();
        }
        @Override public boolean equals(Object o) {
            if (o == null || !(o instanceof Cell)) {
                return false;
            }
            Cell c = (Cell) o;
            return (c.getRowIndex() == getRowIndex() && c.getColIndex() == getColIndex());
        }
        @Override public String toString() {
            return "Cell [" + rowIndex + ", " + colIndex + "]";
        }
    }

    /**
     * Default constructor.
     */
    public EntityEditableListView() {
        this(null);
    }

    /**
     * Constructor giving a title for this list view, which is displayed in the UI.
     * @param title the title of the list
     */
    public EntityEditableListView(String title) {
        if (title != null) {
            setTitle(title);
        }

        setFrame(true);
        setStripeRows(true);
        setAutoHeight(true);
        setCollapsible(true);

        setSelectionModel(selectionModel);
        setClicksToEdit(1);

        setLoadMask(true);
        // note: for some reason setLoadMask(String message) does not work, the underlying code is actually doing something wrong
        // so we use this instead and reset the default CSS class
        setLoadMask(UtilUi.MSG.loading(), "x-mask-loading");
    }

    /**
     * Configures this list view according to previously created column.
     * @param url the URL used to populate the list view
     * @param defaultSortField the name of field to sort by default
     * @see #makeColumn
     */
    protected void configure(String url, String defaultSortField) {
        configure(url, defaultSortField, SortDir.ASC);
    }

    /**
     * Configures this list view according to previously created column.
     * @param url the URL used to populate the list view
     * @param defaultSortField the name of field to sort by default
     * @param defaultSortDirection the default sort direction
     * @see #makeColumn
     */
    protected void configure(String url, String defaultSortField, SortDir defaultSortDirection) {
        configure(makeRecordDef(), makeColumnModel(), url, defaultSortField, defaultSortDirection);
    }

    protected void configure(RecordDef recordDef, ColumnModel columnModel, String url, String defaultSortField) {
        configure(recordDef, columnModel, url, defaultSortField, SortDir.ASC);
    }

    protected void configure(RecordDef recordDef, ColumnModel columnModel, String url, String defaultSortField, SortDir defaultSortDirection) {

        this.recordDef = recordDef;
        this.columnModel = columnModel;
        this.queryUrl = url;
        reader = new JsonReader(recordDef);
        reader.setRoot(UtilLookup.JSON_ROOT);
        reader.setId(UtilLookup.JSON_ID);
        reader.setTotalProperty(UtilLookup.JSON_TOTAL);

        proxy = new HttpProxy(queryUrl);
        store = new GroupingStore(proxy, reader, true);
        if (groupField != null) {
            store.setGroupField(groupField);
        }
        store.setDefaultSort(defaultSortField, defaultSortDirection);

        setStore(store);
        setColumnModel(columnModel);

        store.addStoreListener(this);

        if (usePagingToolbar) {
            makePagingToolbar(true);
        }

        addEditorGridListener(new EditorGridListenerAdapter() {

                // check permissions before edit
                @Override public boolean doBeforeEdit(GridPanel grid, Record record, String field, Object value, int rowIndex, int colIndex) {
                    if (isEditableCell(rowIndex, colIndex, field, value)) {
                        // If the cell is editable, then check if the cell is if associated with an Autocompleter
                        if (columnToAutocompleter.containsKey(field)) {
                            // if it is, then  bind the auto completer to this cell,  so that if it needs to set the record value
                            // later, such as after the user tabs out of it, i.e. onBlur(...),  it will know
                            EntityAutocomplete autocomplete = columnToAutocompleter.get(field);
                            autocomplete.bindToRecord(record, field, EntityEditableListView.this, rowIndex, colIndex);
                        }

                        return true;
                    } else {
                        return false;
                    }
                }

                @Override public boolean doValidateEdit(GridPanel grid, Record record, String field, Object value, Object originalValue, int rowIndex, int colIndex) {
                    if (columnToPostProcessed.containsKey(field)) {
                        ValuePostProcessedInterface postProcessed = columnToPostProcessed.get(field);
                        String realNewValue = postProcessed.getPostProcessedValue(originalValue, value);
                        if (realNewValue == null) {
                            return false;
                        }
                    }
                    return true;
                }

                // set cell edit handler, to sync displayed value when changed from a non-static autocompleter
                @Override public void onAfterEdit(GridPanel grid, Record record, String field, Object newValue, Object oldValue, int rowIndex, int colIndex) {
                    UtilUi.logDebug("Finished editing cell [" + rowIndex + "/" + colIndex + "], field: " + field + ", from " + oldValue + " to " + newValue + ", displayStringChanged = " + displayStringChanged, MODULE, "onAfterEdit");
                    if (displayStringChanged) {
                        // copy displayString to the description field of the edited record
                        record.set(field + UtilLookup.DESCRIPTION_SUFFIX, displayString);
                        displayStringChanged = false;
                    }
                    // check if the value should be post processed
                    if (columnToPostProcessed.containsKey(field)) {
                        ValuePostProcessedInterface postProcessed = columnToPostProcessed.get(field);
                        String realNewValue = postProcessed.getPostProcessedValue(oldValue, newValue);
                        newValue = realNewValue;
                        if (realNewValue != null) {
                            record.set(field, realNewValue);
                        }
                    }
                    // trigger the event handler method
                    cellValueChanged(record, field, oldValue, rowIndex, colIndex);
                }
            });

        // set cell click handler for update / create and delete columns
        addGridCellListener(new GridCellListenerAdapter() {
                @Override public void onCellClick(GridPanel grid, int rowIndex, int colindex, EventObject e) {
                    // check the grid global flag
                    if (!editable) {
                        return;
                    }

                    Record rec = store.getRecordAt(rowIndex);
                    if (rec == null) {
                        return;
                    }

                    if (UtilUi.isSummary(rec)) {
                        return;
                    }
                    if (Permissions.canUpdate(rec) && colindex == createUpdateIndex) {
                        doUpdateCreateAction(rec);
                    } else if (Permissions.canDelete(rec) && colindex == deleteIndex) {
                        doDeleteAction(rec);
                    }
                }
            });

        if (autoLoad) {
            UtilUi.logDebug("Auto loading data.", MODULE, "configure");
            loadFirstPage();
        }

        GroupingView view = new GroupingView() {
                @Override public String getRowClass(Record record, int index, RowParams rowParams, Store store) {
                    return getGridViewRowClass(record, index, rowParams, store);
                }
            };
        if (groupTemplate != null) {
            view.setGroupTextTpl(groupTemplate);
        }
        view.setHideGroupedColumn(true);
        view.setEnableRowBody(true);
        view.setForceFit(true);
        view.setAutoFill(true);
        setView(view);
    }

    private String getGridViewRowClass(Record record, int index, RowParams rowParams, Store store) {
        String body = getRowBody(record, index);
        String extraClass = getRowExtraClass(record, index, body);
        String style = getRowBodyStyle(record, index, body);
        if (body != null && !"".equals(body.trim())) {
            body = "<span style=\"margin-left:15px\">" + body + "</span>";
            rowParams.setBody(body);
            if (extraClass != null) {
                extraClass = "x-grid3-row-expanded " + extraClass;
            } else {
                extraClass = "x-grid3-row-expanded";
            }
        } else {
            rowParams.setBody("");
        }
        if (style != null) {
                        rowParams.setBodyStyle(style);
        }
        return extraClass;
    }

    @Override
    public void reconfigure(Store store, ColumnModel model) {
        super.reconfigure(store, model);
    }

    /**
     * Reconfigures the store and keep the current column model.
     * @param store a <code>Store</code> value
     */
    public void reconfigure(Store store) {
        reconfigure(store, columnModel);
    }

    /**
     * Set the grouping option.
     * @param groupField the field to group the results by, must be one of the Column.
     */
    public void setGrouping(String groupField) {
        this.groupField = groupField;
    }

    /**
     * Set the grouping option.
     * @param groupField the field to group the results by, must be one of the Column.
     * @param groupTemplate the template used to format the groups header
     */
    public void setGrouping(String groupField, String groupTemplate) {
        this.groupField = groupField;
        this.groupTemplate = groupTemplate;
    }

    /**
     * Sets the default page size, need to be called before {@link #configure}.
     * @param size the default page size for this list
     */
    public void setDefaultPageSize(int size) {
        this.defaultPageSize = size;
    }

    /**
     * Clears the filters of this grid.
     */
    public void clearFilters() {
        clearFilters(false);
    }

    /**
     * Clears the filters of this grid.
     * @param clearStickyFilters set to true in order to also clear the sticky filters
     */
    public void clearFilters(boolean clearStickyFilters) {
        for (String k : filters.keySet()) {
            filters.put(k, "");
        }
        if (!clearStickyFilters) {
            for (String k : stickyFilters.keySet()) {
                filters.put(k, stickyFilters.get(k));
            }
        }
    }

    protected void setFilter(String columnName, String value) {
        filters.put(columnName, value);
    }

    protected void setFilter(String columnName, String value, boolean sticky) {
        setFilter(columnName, value);
        if (sticky) {
            stickyFilters.put(columnName, value);
        }
    }

    /**
     * Applies the filters of this grid and reload at the first page.
     */
    public void applyFilters() {
        applyFilters(true);
    }

    /**
     * Applies the filters of this grid.
     * @param resetPager should the grid reloads at the first page
     */
    public void applyFilters(boolean resetPager) {
        List<UrlParam> params = new ArrayList<UrlParam>();
        for (String k : filters.keySet()) {
            params.add(new UrlParam(k, filters.get(k)));
        }
        UrlParam[] urlParams = new UrlParam[params.size()];
        store.setBaseParams(params.toArray(urlParams));
        if (resetPager) {
            UtilUi.logDebug("Applied filters, load requested.", MODULE, "applyFilters");
            loadFirstPage();
        }
    }

    /**
     * Loads the grid data.
     */
    public void loadFirstPage() {
        loadNow = true;
        // if all the registered autocompleters are already loaded, we can load now
        // else setting loadNow to true will trigger the load automatically once they are all loaded.
        if (!loadIfReady()) {
            UtilUi.logDebug("Waiting some required autocompleters to load, deferring loading data.", MODULE, "loadFirstPage");
        }
    }

    /**
     * Resets the pager setting to the first page and reloads the store associated to this list view.
     */
    private void loadFirstPageAsync() {
        List<UrlParam> params = new ArrayList<UrlParam>();
        // if the pager is disabled explicitly, pass the NO_PAGER option to the service so it knows not to paginate the results
        // else pass the paging parameters as defined in the pagingToolbar (user given defaultPageSize is set in the pagingToolbar at this point)
        if (!usePagingToolbar) {
            params.add(new UrlParam(UtilLookup.PARAM_NO_PAGER, "Y"));
        } else if (pagingToolbar != null) {
            params.add(new UrlParam(UtilLookup.PARAM_PAGER_START, 0));
            if (pageSize <= 0) {
                pageSize = defaultPageSize;
            }
            params.add(new UrlParam(UtilLookup.PARAM_PAGER_LIMIT, pageSize));
        }
        UrlParam[] urlParams = new UrlParam[params.size()];
        store.reload(params.toArray(urlParams));
    }

    private boolean checkAllLoaded() {
        // check is all autocompleters are loaded
        for (EntityAutocomplete autocomplete : autocompletes) {
            if (!autocomplete.isLoaded()) {
                return false;
            }
        }
        return true;
    }

    private boolean loadIfReady() {
        // check if we should load now
        if (!loadNow) {
            return false;
        }
        if (checkAllLoaded()) {
            UtilUi.logDebug("All required autocompleters ready, loading data.", MODULE, "loadIfReady");
            loadFirstPageAsync();
            return true;
        }
        return false;
    }

    /**
     * Registers an autocompleter, the grid will wait for it to be loaded before loading its data.
     * This is useful when some of the data displayed depend on other stores to be loaded to render properly.
     * @param autocompleter an <code>EntityAutocomplete</code> value
     */
    public void registerAutocompleter(EntityAutocomplete autocompleter) {
        if (autocompleter != null) {
            autocompleter.addLoadableListener(new LoadableListenerAdapter() {
                    @Override public void onLoad() {
                        loadIfReady();
                    }
                });
            autocompletes.add(autocompleter);
        }
    }

    /**
     * Gets the last added <code>ColumnConfig</code>.
     * @return the last added <code>ColumnConfig</code>
     */
    public ColumnConfig getColumn() {
        return columnConfigs.get(columnConfigs.size() - 1);
    }

    /**
     * Gets the column index by ID.
     * @param id a <code>String</code> value
     * @return an <code>int</code> value, <code>-1</code> if the column was not found
     */
    public int getColumnIndex(String id) {
        ColumnModel m = getColumnModel();
        for (int i = 0; i < m.getColumnCount(); i++) {
            if (id.equals(m.getDataIndex(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Sets the hidden flag for the column with given ID.
     * @param id the column ID
     * @param hidden a <code>boolean</code> value
     */
    public void setColumnHidden(String id, boolean hidden) {
        int index = getColumnIndex(id);
        if (index >= 0) {
            getColumnModel().setHidden(index, hidden);
        }
    }

    /**
     * Creates a display column for this list view prior to configuring it.
     * This method internally creates the necessary corresponding <code>ColumnConfig</code>.
     * @param label the column title label
     * @param renderer the <code>Renderer</code> instance
     * @return the created <code>ColumnConfig</code> instance
     * @see #makeEditableColumn
     * @see #makeLinkColumn
     */
    protected ColumnConfig makeColumn(String label, Renderer renderer) {
        ColumnConfig col = new ColumnConfig();
        col.setHeader(label);
        col.setRenderer(renderer);
        columnConfigs.add(col);
        return col;
    }

    /**
     * Creates a data column for this list view prior to configuring it.
     * This method internally creates the necessary corresponding <code>ColumnConfig</code>.
     * @param label the column title label
     * @param definition a <code>FieldDef</code> value
     * @return the created <code>ColumnConfig</code> instance
     * @see #makeEditableColumn
     * @see #makeLinkColumn
     */
    protected ColumnConfig makeColumn(String label, FieldDef definition) {
        return makeEditableColumn(label, definition, (GridEditor) null);
    }

    /**
     * Creates a data column for this list view prior to configuring it.
     * This method internally creates the necessary corresponding <code>ColumnConfig</code>.
     * @param label the column title label
     * @param definition a <code>FieldDef</code> value
     * @param field a <code>Field</code> instance from which to create the <code>GridEditor</code>
     * @return the created <code>ColumnConfig</code> instance
     * @see #makeColumn
     * @see #makeLinkColumn
     */
    protected ColumnConfig makeEditableColumn(String label, FieldDef definition, Field field) {
        if (field instanceof ValuePostProcessedInterface) {
            columnToPostProcessed.put(definition.getName(), (ValuePostProcessedInterface) field);
        }

        return makeEditableColumn(label, definition, new GridEditor(field));
    }

    /**
     * Creates a data column for this list view prior to configuring it.
     * This method internally creates the necessary corresponding <code>ColumnConfig</code>.
     * @param label the column title label
     * @param definition a <code>FieldDef</code> value
     * @param staticAutocomplete an <code>EntityStaticAutocomplete</code> instance from which to create the <code>GridEditor</code>, also serves as the translator
     * @return the created <code>ColumnConfig</code> instance
     * @see #makeColumn
     * @see #makeLinkColumn
     */
    protected ColumnConfig makeEditableColumn(String label, FieldDef definition, EntityStaticAutocomplete staticAutocomplete) {
        staticAutocomplete.setEmptyText("");
        return makeEditableColumn(label, definition, new GridEditor(staticAutocomplete), staticAutocomplete, true);
    }

    /**
     * Creates a data column for this list view prior to configuring it.
     * This method internally creates the necessary corresponding <code>ColumnConfig</code>.
     * @param label the column title label
     * @param definition a <code>FieldDef</code> value
     * @param autocomplete an <code>EntityAutocomplete</code> instance from which to create the <code>GridEditor</code>, will also sync the display value to the description field
     * @return the created <code>ColumnConfig</code> instance
     * @see #makeColumn
     * @see #makeLinkColumn
     */
    protected ColumnConfig makeEditableColumn(String label, FieldDef definition, EntityAutocomplete autocomplete) {
        return makeEditableColumn(label, definition, autocomplete, (String) null);
    }

    /**
     * Creates a data column for this list view prior to configuring it.
     * This method internally creates the necessary corresponding <code>ColumnConfig</code>.
     * @param label the column title label
     * @param definition a <code>FieldDef</code> value
     * @param autocomplete an <code>EntityAutocomplete</code> instance from which to create the <code>GridEditor</code>, will also sync the display value to the description field
     * @param initialFormatter the String used to format the displayed string, {0} is the description from the record descriptionIndex, {1} is the id from the record dataIndex
     * @return the created <code>ColumnConfig</code> instance
     * @see #makeColumn
     * @see #makeLinkColumn
     */
    protected ColumnConfig makeEditableColumn(String label, FieldDef definition, EntityAutocomplete autocomplete, String initialFormatter) {
        autocomplete.setEmptyText("");
        ColumnConfig col = makeEditableColumn(label, definition, new GridEditor(autocomplete), null, true, initialFormatter);
        columnToAutocompleter.put(definition.getName(), autocomplete);
        autocomplete.addListener(new ComboBoxListenerAdapter() {
                @Override public void onSelect(ComboBox comboBox, Record record, int index) {
                    // get the display value, we don't know which cell was edited, so save the value for later
                    displayString = record.getAsString(UtilLookup.SUGGEST_TEXT);
                    // marked it changed so the grid cell edit event will know to get it
                    displayStringChanged = true;
                    UtilUi.logDebug("An autocompleter changed, got displayString = " + displayString, MODULE, "onSelect");
                }
            });
        return col;
    }

    /**
     * Creates a data column for this list view prior to configuring it.
     * This method internally creates the necessary corresponding <code>ColumnConfig</code>.
     * @param label the column title label
     * @param definition a <code>FieldDef</code> value
     * @param autocomplete an <code>EntityAutocomplete</code> instance from which to create the <code>GridEditor</code>
     * @param staticAutocomplete an <code>EntityAutocomplete</code> instance from which to create the translator, it will have to be made static meaning that all records will be fetched so this is not recommended
     * @return the created <code>ColumnConfig</code> instance
     * @see #makeColumn
     * @see #makeLinkColumn
     */
    protected ColumnConfig makeEditableColumn(String label, FieldDef definition, EntityAutocomplete autocomplete, EntityAutocomplete staticAutocomplete) {
        autocomplete.setEmptyText("");
        staticAutocomplete.makeStatic();
        return makeEditableColumn(label, definition, new GridEditor(autocomplete), staticAutocomplete, true);
    }

    /**
     * Creates a data column for this list view prior to configuring it.
     * This method internally creates the necessary corresponding <code>ColumnConfig</code>.
     * @param label the column title label
     * @param definition a <code>FieldDef</code> value
     * @param editor a <code>GridEditor</code> instance
     * @return the created <code>ColumnConfig</code> instance
     * @see #makeColumn
     * @see #makeLinkColumn
     */
    protected ColumnConfig makeEditableColumn(String label, FieldDef definition, GridEditor editor) {
        return makeEditableColumn(label, definition, editor, null, false);
    }

    /**
     * Creates a data column for this list view prior to configuring it.
     * This method internally creates the necessary corresponding <code>ColumnConfig</code>.
     * @param label the column title label
     * @param definition a <code>FieldDef</code> value
     * @param editor a <code>GridEditor</code> instance
     * @param autocomplete the <code>EntityAutocomplete</code> instance serving as the translator
     * @param useDescriptionColumn a flag to indicate if we should use a description column config
     * @return the created <code>ColumnConfig</code> instance
     * @see #makeColumn
     * @see #makeLinkColumn
     */
    private ColumnConfig makeEditableColumn(String label, FieldDef definition, GridEditor editor, EntityAutocomplete autocomplete, boolean useDescriptionColumn) {
        return makeEditableColumn(label, definition, editor, autocomplete, useDescriptionColumn, null);
    }

    /**
     * Creates a data column for this list view prior to configuring it.
     * This method internally creates the necessary corresponding <code>ColumnConfig</code>.
     * @param label the column title label
     * @param definition a <code>FieldDef</code> value
     * @param editor a <code>GridEditor</code> instance
     * @param autocomplete the <code>EntityAutocomplete</code> instance serving as the translator
     * @param useDescriptionColumn a flag to indicate if we should use a description column config
     * @param initialFormatter the String used to format the initial displayed string, {0} is the description from the record descriptionIndex, {1} is the id from the record dataIndex
     * @return the created <code>ColumnConfig</code> instance
     * @see #makeColumn
     * @see #makeLinkColumn
     */
    private ColumnConfig makeEditableColumn(String label, FieldDef definition, GridEditor editor, EntityAutocomplete autocomplete, boolean useDescriptionColumn, String initialFormatter) {
        fieldDefinitions.add(definition);

        ColumnConfig col;
        if (useDescriptionColumn) {
            if (autocomplete != null) {
                registerAutocompleter(autocomplete);
                col = new DescriptionColumnConfig(label, definition.getName(), autocomplete);
            } else {
                String descriptionField = definition.getName() + UtilLookup.DESCRIPTION_SUFFIX;
                fieldDefinitions.add(new StringFieldDef(descriptionField));
                if (initialFormatter != null) {
                    col = new DescriptionColumnConfig(label, definition.getName(), descriptionField, initialFormatter);
                } else {
                    col = new DescriptionColumnConfig(label, definition.getName(), descriptionField);
                }
            }
        } else {
            col = new ColumnConfig(label, definition.getName());
        }

        col.setId(definition.getName());

        if (editor != null) {
            col.setEditor(editor);
        }
        columnConfigs.add(col);
        return col;
    }

    protected void addFieldDefinition(FieldDef definition) {
        fieldDefinitions.add(definition);
    }

    /**
     * Creates a data column for this list view prior to configuring it which renders as a link to the given URL.
     * This method internally creates the necessary corresponding <code>ColumnConfig</code>.
     * @param label the column title label
     * @param valueDefinition a <code>FieldDef</code> for the field containing the amount
     * @param currencyCode the currency code string (a 3 chars code)
     * @return the created <code>ColumnConfig</code> instance
     * @see #makeColumn
     */
    protected ColumnConfig makeCurrencyColumn(String label, FieldDef valueDefinition, String currencyCode) {
        if (fieldDefinitions == null) {
            fieldDefinitions = new HashSet<FieldDef>();
        }
        if (columnConfigs == null) {
            columnConfigs = new ArrayList<ColumnConfig>();
        }

        fieldDefinitions.add(valueDefinition);

        CurrencyColumnConfig col = new CurrencyColumnConfig(label, valueDefinition.getName());
        col.setCurrencyCode(currencyCode);
        col.setId(valueDefinition.getName());
        columnConfigs.add(col);
        return col;
    }

    /**
     * Creates a data column for this list view prior to configuring it.
     * This method internally creates the necessary corresponding <code>ColumnConfig</code>.
     * @param label the column title label
     * @param currencyDefinition a <code>FieldDef</code> for the field containing the currency code
     * @param valueDefinition a <code>FieldDef</code> for the field containing the amount
     * @return the created <code>ColumnConfig</code> instance
     * @see #makeColumn
     */
    protected ColumnConfig makeCurrencyColumn(String label, FieldDef currencyDefinition, FieldDef valueDefinition) {
        if (fieldDefinitions == null) {
            fieldDefinitions = new HashSet<FieldDef>();
        }
        if (columnConfigs == null) {
            columnConfigs = new ArrayList<ColumnConfig>();
        }

        fieldDefinitions.add(valueDefinition);

        // the currency field definition might not have been added yet, this is safe since fieldDefinitions is a Set
        fieldDefinitions.add(currencyDefinition);

        CurrencyColumnConfig col = new CurrencyColumnConfig(label, currencyDefinition.getName(), valueDefinition.getName());
        col.setId(valueDefinition.getName());
        columnConfigs.add(col);
        return col;
    }

    /**
     * Creates a data column for this list view prior to configuring it which renders as a link to the given URL.
     * This method internally creates the necessary corresponding <code>ColumnConfig</code>.
     * @param label the column title label
     * @param valueDefinition a <code>FieldDef</code> value
     * @param url the URL to be used for making the link, a placeholder can be used in the string for the field data. For example <code>/crmsfa/control/viewContact?partyId={0}</code>
     * @return the created <code>ColumnConfig</code> instance
     * @see #makeColumn
     */
    protected ColumnConfig makeLinkColumn(String label, FieldDef valueDefinition, String url) {
        return makeLinkColumn(label, valueDefinition, url, false);
    }

    /**
     * Creates a data column for this list view prior to configuring it which renders as a link to the given URL.
     * This method internally creates the necessary corresponding <code>ColumnConfig</code>.
     * @param label the column title label
     * @param valueDefinition a <code>FieldDef</code> value
     * @param url the URL to be used for making the link, a placeholder can be used in the string for the field data. For example <code>/crmsfa/control/viewContact?partyId={0}</code>
     * @param lookup if <code>true</code> the link will be replaced by a javascript call that set the value to return when the widget is used as a lookup
     * @return the created <code>ColumnConfig</code> instance
     * @see #makeColumn
     */
    protected ColumnConfig makeLinkColumn(String label, FieldDef valueDefinition, String url, boolean lookup) {
        return makeLinkColumn(label, valueDefinition, valueDefinition, url, lookup);
    }

    /**
     * Creates a data column for this list view prior to configuring it which renders as a link to the given URL.
     * This method internally creates the necessary corresponding <code>ColumnConfig</code>.
     * @param label the column title label
     * @param idDefinition a <code>FieldDef</code> value
     * @param valueDefinition a <code>FieldDef</code> value
     * @param url the URL to be used for making the link, a placeholder can be used in the string for the ID data. For example <code>/crmsfa/control/viewContact?partyId={0}</code>
     * @return the created <code>ColumnConfig</code> instance
     * @see #makeColumn
     */
    protected ColumnConfig makeLinkColumn(String label, FieldDef idDefinition, FieldDef valueDefinition, String url) {
        return makeLinkColumn(label, idDefinition, valueDefinition, url, false);
    }

    /**
     * Creates a data column for this list view prior to configuring it.
     * This method internally creates the necessary corresponding <code>ColumnConfig</code>.
     * @param label the column title label
     * @param idDefinition a <code>FieldDef</code> value
     * @param valueDefinition a <code>FieldDef</code> value
     * @param url the URL to be used for making the link, a placeholder can be used in the string for the ID data. For example <code>/crmsfa/control/viewContact?partyId={0}</code>
     * @param lookup if <code>true</code> the link will be replaced by a javascript call that set the value to return when the widget is used as a lookup
     * @return the created <code>ColumnConfig</code> instance
     * @see #makeColumn
     */
    protected ColumnConfig makeLinkColumn(String label, FieldDef idDefinition, FieldDef valueDefinition, String url, boolean lookup) {
        if (fieldDefinitions == null) {
            fieldDefinitions = new HashSet<FieldDef>();
        }
        if (columnConfigs == null) {
            columnConfigs = new ArrayList<ColumnConfig>();
        }

        fieldDefinitions.add(valueDefinition);

        // the ID field definition might not have been added yet, this is safe since fieldDefinitions is a Set
        fieldDefinitions.add(idDefinition);

        LinkColumnConfig col = new LinkColumnConfig(label, idDefinition.getName(), valueDefinition.getName(), url, lookup);
        col.setId(valueDefinition.getName());
        if (lookup) {
            lookupColumns.add(col);
        }

        columnConfigs.add(col);
        return col;
    }

    /**
     * Adds a reload button to the grid for reverting any changes made.
     */
    protected void makeReloadButton() {
        revertButton = new Button(UtilUi.MSG.revert(), new ButtonListenerAdapter() {
                @Override public void onClick(Button button, EventObject e) {
                    loadFirstPage();
                }
            });
        addButton(revertButton);
    }

    /**
     * Adds a save all button to the grid for batch commit.
     * @param url the URL to post the batch data to
     */
    protected void makeSaveAllButton(String url) {
        makeSaveAllButton(url, null);
    }

    /**
     * Adds a save all button to the grid for batch commit.
     * @param url the URL to post the batch data to
     * @param additionalBatchData extra data that should be attache to each record when batch posting
     */
    protected void makeSaveAllButton(String url, Map<String, String> additionalBatchData) {
        this.additionalBatchData = additionalBatchData;
        saveAllButton = new Button(UtilUi.MSG.saveAll(), new ButtonListenerAdapter() {
                @Override public void onClick(Button button, EventObject e) {
                    doBatchAction();
                }
            });
        addButton(saveAllButton);
        saveAllUrl = url;
    }

    /**
     * Creates the update button column, which can do update / create buttons.
     * @param idFieldName the field used as ID for the record, the button will be a create button if the id is <code>null</code>, else it will be an update button
     * @see #makeColumn
     */
    protected void makeCreateUpdateColumn(String idFieldName) {
        createUpdateIndex = columnConfigs.size();
        columnConfigs.add(new CreateUpdateColumnConfig(idFieldName));
    }

    /**
     * Creates the delete button column.
     * @param idFieldName the field used as ID for the record, the button will simply delete the row if the id is <code>null</code>, else it will have to post a delete request
     * @see #makeColumn
     */
    protected void makeDeleteColumn(String idFieldName) {
        deleteIndex = columnConfigs.size();
        columnConfigs.add(new DeleteColumnConfig(idFieldName));
    }

    /**
     * Creates the create / update and delete button columns.
     * @param idFieldName the field used as ID for the record
     * @see #makeCreateUpdateColumn
     * @see #makeDeleteColumn
     */
    protected void makeCUDColumns(String idFieldName) {
        makeCreateUpdateColumn(idFieldName);
        makeDeleteColumn(idFieldName);
    }

    /**
     * Sets the flag to allow creation of new records using this grid, must also have the CREATE permission returned by the service not <code>False</code>. Defaults to <code>false</code>.
     * @param flag a <code>boolean</code> value
     */
    protected void setCanCreateNewRow(boolean flag) {
        canCreateNewRow = flag;
    }

    /**
     * Sets the editable mode for this grid. Defaults to <code>true</code>.
     * To act like a simple list view set this to <code>false</code>.
     * Note: this must be set before the data is loaded.
     * @param flag a <code>boolean</code> value
     */
    public void setEditable(boolean flag) {
        editable = flag;
    }

    /**
     * Sets the data auto loading flag for this grid, if <code>true</code> the data is loaded as soon as the columns are configured, if you need to apply filters set this to <code>false</code>. Defaults to <code>true</code>.
     * Note: obviously this must be set before the data is loaded.
     * @param flag a <code>boolean</code> value
     */
    public void setAutoLoad(boolean flag) {
        autoLoad = flag;
    }

    /**
     * Sets the grid to use a summary row. Defaults to <code>false</code>.
     * Note: this must be set before the grid data is loaded.
     * @param flag a <code>boolean</code> value
     */
    public void setUseSummaryRow(boolean flag) {
        useSummaryRow = flag;
    }

    /**
     * Sets the grid to use a paging toolbar. Defaults to <code>false</code>.
     * Note: this must be set before the grid columns are configured.
     * @param flag a <code>boolean</code> value
     */
    public void setUsePagingToolbar(boolean flag) {
        usePagingToolbar = flag;
    }

    /**
     * Creates the pagination toolbar with the excel export button.
     * @param exportToExcel option to create the excel export button or not
     */
    private void makePagingToolbar(boolean exportToExcel) {

        pagingToolbar = new PagingToolbar(store);
        pagingToolbar.setPageSize(defaultPageSize);
        pagingToolbar.setDisplayInfo(true);
        pagingToolbar.setDisplayMsg(UtilUi.MSG.pagerDisplayMessage());
        pagingToolbar.setEmptyMsg(UtilUi.MSG.pagerDisplayEmpty());
        pagingToolbar.setFirstText(UtilUi.MSG.pagerFirstPage());
        pagingToolbar.setLastText(UtilUi.MSG.pagerLastPage());
        pagingToolbar.setNextText(UtilUi.MSG.pagerNextPage());
        pagingToolbar.setPrevText(UtilUi.MSG.pagerPreviousPage());
        pagingToolbar.setRefreshText(UtilUi.MSG.refresh());
        pagingToolbar.setBeforePageText(UtilUi.MSG.pagerBeforePage());
        pagingToolbar.setAfterPageText(UtilUi.MSG.pagerAfterPage());

        pageSizeField = new NumberField();
        pageSizeField.setAllowDecimals(false);
        pageSizeField.setWidth(40);
        pageSizeField.setValue(Integer.valueOf(pagingToolbar.getPageSize()));
        pageSizeField.setSelectOnFocus(true);
        pageSizeField.addListener(new FieldListenerAdapter() {
                @Override public void onSpecialKey(Field field, EventObject e) {
                    if (e.getKey() == EventObject.ENTER) {
                        changePageSize(pageSizeField);
                    }
                }
            });

        pagingToolbar.doOnRender(new Function() {
            public void execute() {
                pagingToolbar.getRefreshButton().addListener(new ButtonListenerAdapter() {
                    public void onClick(Button button, EventObject e) {
                        changePageSize(pageSizeField);
                    }
                });
            }
        });

        final ToolTip toolTip = new ToolTip(UtilUi.MSG.pagerEnterPageSize());
        toolTip.applyTo(pageSizeField);
        pagingToolbar.addField(pageSizeField);
        pagingToolbar.addText(UtilUi.MSG.pagerPageSize());

        if (exportToExcel) {
            pagingToolbar.addSeparator();
            final ToolbarButton exportToExcelButton = new ToolbarButton(UtilUi.MSG.pagerExportToExcel(), new ButtonListenerAdapter() {
                    @Override public void onClick(Button button, EventObject e) {
                        String url = queryUrl + "?" + UtilLookup.PARAM_EXPORT_EXCEL + "=Y";
                        // pass the filter parameters, the excel spreadsheet content will match the list view content
                        for (String k : filters.keySet()) {
                            url += "&" + k + "=" + filters.get(k);
                        }
                        // pass the sorting info
                        url += "&" + UtilLookup.PARAM_SORT_FIELD + "=" + getStore().getSortState().getField();
                        url += "&" + UtilLookup.PARAM_SORT_DIRECTION + "=" + getStore().getSortState().getDirection().getDirection();
                        // pass the column info, since the user can hide and reorder columns, the excel spreadsheet will match the list view configuration
                        ColumnModel m = getColumnModel();
                        for (int i = 0; i < m.getColumnCount(); i++) {
                            // call to getDataIndex may rise error for column w/o underlying
                            // data field, e.g. column that renders a button.
                            try {
                                url += "&_" + m.getDataIndex(i) + "_idx=" + i;
                            } catch (Exception ex) {
                                UtilUi.logWarning("Column with index " + Integer.valueOf(i).toString() + " was skipped due to an exception.", MODULE, "exportToExcelButton.onClick");
                                continue;
                            }
                        }
                        UtilUi.logInfo("url : " + url, MODULE, "exportToExcelButton.onClick");
                        UtilUi.redirect(url);
                    }
                }, UtilUi.ICON_EXCEL);
            pagingToolbar.addButton(exportToExcelButton);
        }
        setBottomToolbar(pagingToolbar);
    }

    /**
     * Sets the page size for this list.
     * @param pageSize an integer value
     */
    public void setPageSize(int pageSize) {
        // do not allow 0 as a page size
        if (pageSize > 0) {
            this.pageSize = pageSize;
            pagingToolbar.setPageSize(pageSize);
            pageSizeField.setValue(pageSize);
        } else {
            UtilUi.logError("NOT setting negative list page size " + pageSize, MODULE, "setPageSize");
        }
    }

    /**
     * Change page size value to pageSizeField.getValue().
     * @param pageSizeField a <code>NumberField</code> value
     */
    private void changePageSize(NumberField pageSizeField) {
        // Seems using getValue().intValue() sometimes does not work
        String pageSizeString = pageSizeField.getRawValue();
        int pageSize;
        if (UtilUi.isEmpty(pageSizeString)) {
            pageSize = defaultPageSize;
        } else {
            pageSize = Integer.valueOf(pageSizeString);
        }
        // do not allow 0 as a page size
        if (pageSize > 0) {
            pageSizeField.setValue(pageSize);
            this.pageSize = pageSize;
            pagingToolbar.setPageSize(pageSize);
        } else {
            pageSizeField.setValue(Integer.valueOf(pagingToolbar.getPageSize()));
        }
    }

    /**
     * Checks if the store has been loaded (loading is asynchronous).
     * @return a <code>boolean</code> value
     */
    public boolean isLoaded() {
        return loaded;
    }

    protected ColumnModel makeColumnModel() {
        ColumnModel model = new ColumnModel(columnConfigs.toArray(new ColumnConfig[columnConfigs.size()]));
        // allow sort by default on ready only grids (as it wont conflict with cell editors)
        model.setDefaultSortable(!editable);
        return model;
    }

    protected RecordDef makeRecordDef() {
        // add the definition needed to support summary records
        addFieldDefinition(new StringFieldDef(UtilUi.SUMMARY_ROW_INDICATOR_FIELD));
        // add permissions related record definitions
        addFieldDefinition(new BooleanFieldDef(Permissions.CREATE_FIELD_NAME));
        addFieldDefinition(new BooleanFieldDef(Permissions.UPDATE_FIELD_NAME));
        addFieldDefinition(new BooleanFieldDef(Permissions.DELETE_FIELD_NAME));
        return new RecordDef(fieldDefinitions.toArray(new FieldDef[fieldDefinitions.size()]));
    }

    /** {@inheritDoc} */
    public void notifySuccess(Object obj) {
        loadFirstPage();
    }

    /**
     * Sets the list view for a lookup.
     */
    public void setLookupMode() {
        for (LinkColumnConfig lookupColumn : lookupColumns) {
            lookupColumn.setLookupMode();
        }
    }

    /**
     * Binds the list view to the given <code>BaseFormPanel</code> so that when a record is selected in the list the form content gets populated by the corresponding data,
     *  and inversely when a field is updated in the form.
     * Note that the form field names and the data field names must match.
     * @param formPanel a <code>BaseFormPanel</code> value
     */
    public void bindToForm(final BaseFormPanel formPanel) {
        if (formPanel == null) {
            return;
        }

        formPanel.setBindedList(this);

        selectionModel.addListener(new RowSelectionListenerAdapter() {
                @Override public void onRowSelect(RowSelectionModel sm, int rowIndex, Record record) {
                    formPanel.loadRecord(record, rowIndex);
                }
            });
    }

    /**
     * Update the record at the given index with the values of the given record.
     * @param index the index in this grid store
     * @param record a <code>Record</code> value with the new values
     */
    public void updateRecord(int index, Record record) {
        if (index < 0 || index > getStore().getCount()) {
            return;
        }
        Record rec = getStore().getAt(index);
        if (rec == null) {
            return;
        }
        // synchronize the fields values, not only get the fields from record which
        // may have less fields than rec
        for (String f : record.getModifiedFields()) {
            rec.set(f, record.getAsObject(f));
        }

    }

    private void regenDefaultValuesArray() {
        if (recordDef != null) {
            List<Object> values = new ArrayList<Object>();
            for (FieldDef fd : recordDef.getFields()) {
                String fn = fd.getName();
                if (defaultValues.containsKey(fn)) {
                    values.add(defaultValues.get(fn));
                } else {
                    // handle default permissions
                    if (Permissions.CREATE_FIELD_NAME.equals(fn)) {
                        values.add(true);
                    } else if (Permissions.UPDATE_FIELD_NAME.equals(fn)) {
                        values.add(true);
                    } else if (Permissions.DELETE_FIELD_NAME.equals(fn)) {
                        values.add(false);
                    } else {
                        values.add(null);
                    }
                }

            }
            defaultValuesArray = values.toArray();
        }
    }

    /**
     * Sets the default value for the given field, used when creating a new row.
     * Defaults to <code>null</code>.
     * @param field the field name, corresponding to its <code>RecordDef</code>
     * @param value an <code>Object</code> value
     */
    protected void setDefaultValue(String field, Object value) {
        defaultValues.put(field, value);
        if (defaultValuesArray != null) {
            regenDefaultValuesArray();
        }
    }

    /**
     * Gets the first summary found in the Store.
     * @return a <code>Record</code> value
     */
    protected Record getSummaryRecord() {
        for (Record rec : store.getRecords()) {
            if (UtilUi.isSummary(rec)) {
                return rec;
            }
        }
        return null;
    }

    /**
     * Inserts a summary row at the end of the list, this can be used by subclasses to display summary information for each columns.
     * @return the created <code>Record</code> object or null if no record could be created
     */
    protected Record addSummaryRow() {
        UtilUi.logDebug("Adding summary row.", MODULE, "addSummaryRow");
        HashMap<String, Object> values = new HashMap<String, Object>();
        values.put(UtilUi.SUMMARY_ROW_INDICATOR_FIELD, "Y");
        return addRow(values);
    }

    /**
     * Inserts a new row at the end of the list with the default values IF the create permission flag is set or the grid has the <code>canCreateNewRow</code> flag to true.
     * @return the created <code>Record</code> object or null if no record could be created
     */
    protected Record addRowIfCreatePermission() {
        // check the grid editable flag first, then the canCreateNewRow, then permissions
        if (editable && canCreateNewRow) {
            if (globalPermissions.canCreate()) {
                return addRow();
            }
        }
        return null;
    }

    /**
     * Inserts a new row at the end of the list with the default values IF the create permission flag is set or the grid has the <code>canCreateNewRow</code> flag to true.
     * @param index the index where to insert the row, use negative for an index relative to the end of the list
     * @return the created <code>Record</code> object or null if no record could be created
     */
    protected Record addRowIfCreatePermission(int index) {
        // check the grid editable flag first, then the canCreateNewRow, then permissions
        if (editable && canCreateNewRow) {
            if (globalPermissions.canCreate()) {
                return addRow(index);
            }
        }
        return null;
    }

    /**
     * Inserts a new row at the end of the list with the default values.
     * @return the created <code>Record</code> object or null if no record could be created
     */
    protected Record addRow() {
        return addRow(store.getCount());
    }

    /**
     * Inserts a new row at the given index of the list with the default values.
     * @param index the index where to insert the row, use negative for an index relative to the end of the list
     * @return the created <code>Record</code> object or null if no record could be created
     */
    protected Record addRow(int index) {
        if (defaultValuesArray == null) {
            regenDefaultValuesArray();
        }
        // allow negative index as relative to the end of the list
        if (index < 0) {
            index = store.getCount() + index;
        }
        Record newRecord = recordDef.createRecord(defaultValuesArray);
        store.insert(index, newRecord);
        return newRecord;
    }

    /**
     * Inserts a new row at the given index of the list with given values.
     * @param values a <code>Map</code> of fieldName: value
     * @param index the index where to insert the row, use negative for an index relative to the end of the list
     * @return the created <code>Record</code> object or null if no record could be created
     */
    protected Record addRow(int index, Map<String, Object> values) {
        // allow negative index as relative to the end of the list
        if (index < 0) {
            index = store.getCount() + index;
        }

        if (recordDef != null) {
            List<Object> val = new ArrayList<Object>();
            for (FieldDef fd : recordDef.getFields()) {
                String fn = fd.getName();
                if (values.containsKey(fn)) {
                    val.add(values.get(fn));
                } else {
                    val.add(null);
                }
            }
            Record newRecord = recordDef.createRecord(val.toArray());
            store.insert(index, newRecord);
            return newRecord;
        }
        return null;
    }

    /**
     * Inserts a new row at the end of the list with given values.
     * @param values a <code>Map</code> of fieldName: value
     * @return the created <code>Record</code> object or null if no record could be created
     */
    protected Record addRow(Map<String, Object> values) {
        return addRow(store.getCount(), values);
    }

    /**
     * Handles the save all batch action, this takes all records that need
     * to be created, update or deleted and send them in one request.
     * The posted data is the same format as for a <code>service-multi</code>.
     */
    protected void doBatchAction() {
        UtilUi.logInfo("doBatchAction ...", MODULE, "doBatchAction");
        String data = makeBatchPostData();
        if (data == null) {
            UtilUi.logInfo("nothing to do", MODULE, "doBatchAction");
            return;
        }

        RequestBuilder request = new RequestBuilder(RequestBuilder.POST, GWT.getHostPageBaseURL() + saveAllUrl);
        request.setHeader("Content-type", "application/x-www-form-urlencoded");
        request.setRequestData(data);
        request.setTimeoutMillis(UtilLookup.getAjaxDefaultTimeout());
        request.setCallback(new RequestCallback() {
                public void onError(Request request, Throwable exception) {
                    // display error message
                    markGridNotBusy();
                    UtilUi.errorMessage(exception.toString());
                }
                public void onResponseReceived(Request request, Response response) {
                    // if it is a correct response, reload the grid
                    markGridNotBusy();
                    UtilUi.logInfo("onResponseReceived, response = " + response, MODULE, "doBatchAction");
                    if (!ServiceErrorReader.showErrorMessageIfAny(response, saveAllUrl)) {
                        // commit store changes
                        getStore().commitChanges();
                        loadFirstPage();
                    }
                }
            });
        try {
            markGridBusy();
            UtilUi.logInfo("posting batch", MODULE, "doBatchAction");
            request.send();
        } catch (RequestException e) {
            // display error message
            UtilUi.errorMessage(e.toString(), MODULE, "doBatchAction");
        }
    }

    private String makeBatchPostData() {
        StringBuilder sb = new StringBuilder();
        int index = 0;
        index = makeBatchPostData(index, UtilLookup.PARAM_CUD_ACTION_CREATE, getRecordsToCreate(), sb);
        index = makeBatchPostData(index, UtilLookup.PARAM_CUD_ACTION_UPDATE, getRecordsToUpdate(), sb);
        index = makeBatchPostData(index, UtilLookup.PARAM_CUD_ACTION_DELETE, getRecordsToDelete(), sb);
        if (index == 0) {
            return null;
        }
        sb.append("&").append("_rowCount=").append(index);
        return sb.toString();
    }

    private int makeBatchPostData(int index, String action, List<Record> records, StringBuilder sb) {
        for (Record record : records) {
            if (index > 0) {
                sb.append("&");
            }
            // set the submit flag
            sb.append(UtilLookup.ROW_SUBMIT_PREFIX).append(index).append("=").append("Y");
            // add the action, so the service knows what to do with the data
            sb.append("&").append(UtilLookup.PARAM_CUD_ACTION).append(UtilLookup.MULTI_ROW_DELIMITER).append(index).append("=").append(URL.encodeQueryString(action));
            for (String field : record.getFields()) {
                // remove client-side permissions
                if (Permissions.isPermissionField(field)) {
                    continue;
                }
                sb.append("&").append(field).append(UtilLookup.MULTI_ROW_DELIMITER).append(index).append("=");
                if (!record.isEmpty(field)) {
                    sb.append(URL.encodeQueryString(record.getAsString(field)));
                }
            }
            // add additional fields that may be required in the service
            if (additionalBatchData != null) {
                for (String extraField : additionalBatchData.keySet()) {
                    sb.append("&").append(extraField).append(UtilLookup.MULTI_ROW_DELIMITER).append(index).append("=").append(URL.encodeQueryString(additionalBatchData.get(extraField)));
                }
            }
            index++;
        }
        return index;
    }

    /**
     * Sets the fields that define a <code>Record</code> primary key.
     * The main use is to check if a <code>Record</code> exists, which implies
     * the primary key fields are all non empty.
     * @param fields the list of fields composing the primary key in a <code>Record</code>
     */
    public void setRecordPrimaryKeyFields(Collection<String> fields) {
        recordPrimaryKeyFields = new HashSet<String>();
        recordPrimaryKeyFields.addAll(fields);
    }

    /**
     * Determines if a given <code>Record</code> exists in the application.
     * For example this is used to determine if a record should be posted to be
     * Created or Updated.
     * @param record a <code>Record</code> value
     * @return a <code>boolean</code> value
     */
    protected boolean recordExists(Record record) {
        for (String f : recordPrimaryKeyFields) {
            if (record.isEmpty(f)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Handles the create or update action on the given row, this uses {@link #recordExists}
     * to determine if the action should be a Create or Update.
     * @param record the <code>Record</code> to update or create
     */
    private void doUpdateCreateAction(Record record) {
        if (recordExists(record)) {
            doUpdateAction(record);
        } else {
            doCreateAction(record);
        }
    }

    /**
     * Handles the update action on the given row.
     * Can override to do some immediate action with the <code>Record</code>.
     * @param record the <code>Record</code> to update or create
     */
    protected void doUpdateAction(Record record) {
    }

    /**
     * Handles the create action on the given row.
     * Can override to do some immediate action with the <code>Record</code>.
     * @param record the <code>Record</code> to update or create
     */
    protected void doCreateAction(Record record) {
    }

    /**
     * Handles the delete action on the given row.
     * Can override to do some immediate action with the <code>Record</code>, else the default implementation
     * is to store the <code>Record</code> if it exists for later batch action and removes it from the grid.
     * @param record the <code>Record</code> to delete
     */
    protected void doDeleteAction(Record record) {
        getStore().remove(record);
        if (recordExists(record)) {
            toDeleteRecords.add(record);
        } else {
            // commit the record else the grid will keep it in its cache
            record.commit();
        }
    }

    /**
     * Gets the list of <code>Record</code> that have been marked for deletion.
     * This can be used if the {@link #doDeleteAction} was not overridden to immediately delete the record
     * to do batch delete instead.
     * @return the <code>List</code> of <code>Record</code> that were marked for deletion
     */
    protected List<Record> getRecordsToDelete() {
        for (Record rec : toDeleteRecords) {
            UtilUi.logInfo("To DELETE: " + UtilUi.toString(rec), MODULE, "getRecordsToDelete");
        }

        return toDeleteRecords;
    }

    /**
     * Gets the list of <code>Record</code> that should be created.
     * This can be used to do batch action.
     * @return the <code>List</code> of <code>Record</code> that should be created
     */
    protected List<Record> getRecordsToCreate() {
        List<Record> toCreate = new ArrayList<Record>();
        for (Record rec : getStore().getModifiedRecords()) {
            if (!recordExists(rec)) {
                UtilUi.logInfo("To CREATE: " + UtilUi.toString(rec), MODULE, "getRecordsToCreate");
                toCreate.add(rec);
            }
        }
        return toCreate;
    }

    /**
     * Gets the list of <code>Record</code> that should be updated.
     * This can be used to do batch action.
     * @return the <code>List</code> of <code>Record</code> that should be updated.
     */
    protected List<Record> getRecordsToUpdate() {
        List<Record> toUpdate = new ArrayList<Record>();
        for (Record rec : getStore().getModifiedRecords()) {
            if (recordExists(rec)) {
                UtilUi.logInfo("To UPDATE: " + UtilUi.toString(rec), MODULE, "getRecordsToUpdate");
                toUpdate.add(rec);
            }
        }
        return toUpdate;
    }

    /**
     * Populates the grid rows extra info, this should return the HTML code to insert as a secondary row for a given record.
     * Default implementation returns <code>null</code>.
     * @param record the row <code>Record</code>
     * @param index the row index
     * @return the HTML to include in the extra row, if <code>null</code> or empty it won't be visible
     */
    protected String getRowBody(Record record, int index) {
        return null;
    }

    /**
     * Sets a custom CSS style to a row.
     * Default implementation returns <code>null</code>.
     * @param record the row <code>Record</code>
     * @param index the row index
     * @param extraInfo the extra content if any
     * @return a CSS style
     */
    protected String getRowBodyStyle(Record record, int index, String extraInfo) {
        return null;
    }

    /**
     * Sets a custom CSS class to a row.
     * Default implementation returns <code>null</code>.
     * @param record the row <code>Record</code>
     * @param index the row index
     * @param extraInfo the extra content if any
     * @return a String that is appended to the normal class of the row
     */
    protected String getRowExtraClass(Record record, int index, String extraInfo) {
        return null;
    }

    /**
     * Registers a <code>LoadableListener</code>.
     * @param listener a <code>LoadableListener</code> value
     */
    public void addLoadableListener(LoadableListener listener) {
        listeners.add(listener);
    }

    protected void notifyLoad() {
        loaded = true;
        for (LoadableListener l : listeners) {
            l.onLoad();
        }
    }

    // those two methods are for cell locking / unlocking

    /**
     * Locks a cell with the given coordinates so that it cannot be edited.
     * @param rowIndex an <code>int</code> value
     * @param colIndex an <code>int</code> value
     */
    public void lockCell(int rowIndex, int colIndex) {
        Cell c = new Cell(rowIndex, colIndex);
        UtilUi.logDebug("Locking " + c, MODULE, "lockCell");
        lockedCells.add(c);
    }

    /**
     * Unlocks a cell with the given coordinates so that it can be edited again.
     * @param rowIndex an <code>int</code> value
     * @param colIndex an <code>int</code> value
     */
    public void unlockCell(int rowIndex, int colIndex) {
        Cell c = new Cell(rowIndex, colIndex);
        UtilUi.logDebug("Unlocking " + c, MODULE, "unlockCell");
        lockedCells.remove(c);
    }

    /**
     * Unlocks all locked cell so that they can be edited again.
     */
    public void unlockAllCells() {
        for (Cell c : lockedCells) {
            UtilUi.logDebug("Unlocking " + c, MODULE, "unlockCell");
        }
        lockedCells.clear();
    }

    /**
     * A place holder event handler for cell edition for sub classes.
     * @param record the <code>Record</code> that was modified
     * @param field the <code>String</code> in the record that was modified
     * @param oldValue the field value before it was modified, an <code>Object</code> value
     * @param rowIndex an <code>int</code> value
     * @param colIndex an <code>int</code> value
     */
    public void cellValueChanged(Record record, String field, Object oldValue, int rowIndex, int colIndex) { }

    // those method are from the StoreListener interface

    /** {@inheritDoc} */
    public boolean doBeforeLoad(Store store) {
        return true;
    }

    /** {@inheritDoc} */
    public void onAdd(Store store, Record[] records, int index) {
        UtilUi.logInfo("onAdd, index = " + index, MODULE, "onAdd");
        // we have to trigger a resize so the container can expand with the grid
        syncSize();
    }

    /** {@inheritDoc} */
    public void onClear(Store store) {
    }

    /** {@inheritDoc} */
    public void onDataChanged(Store store) {
        UtilUi.logInfo("onDataChanged", MODULE, "onDataChanged");
    }

    /** {@inheritDoc}
     * The default implementation is to automatically add a new record if the permission is set. */
    public void onLoad(Store store, Record[] records) {
        UtilUi.logInfo("onLoad", MODULE, "onLoad");
        // reset the list of records to delete
        toDeleteRecords = new ArrayList<Record>();
        // find the first record that is always included for permissions
        Record globalPermissionsRecord = records[0];
        globalPermissions = new Permissions(globalPermissionsRecord);
        store.remove(globalPermissionsRecord);

        // if the grid is not editable, or if global permissions do not have create / update or delete, hide those columns
        boolean noUpdateCreate = !editable || (!globalPermissions.canCreate() && !globalPermissions.canUpdate());
        boolean noDelete = !editable || !globalPermissions.canDelete();

        UtilUi.logInfo("noUpdateCreate = " + noUpdateCreate + ", noDelete = " + noDelete, MODULE, "onLoad");

        if (createUpdateIndex > 0) {
            getColumnModel().setHidden(createUpdateIndex, noUpdateCreate);
        }

        if (deleteIndex > 0) {
            getColumnModel().setHidden(deleteIndex, noDelete);
        }

        // if cannot create / update and delete, hide the Save all button
        if (saveAllButton != null) {
            if (noUpdateCreate && noDelete) {
                saveAllButton.hide();
            } else {
                saveAllButton.show();
            }
        }

        addRowIfCreatePermission();

        if (useSummaryRow) {
            addSummaryRow();
        }

        // unlock cells
        unlockAllCells();

        // now we are all loaded (internal autocompleters had to be loaded for the grid to load)
        notifyLoad();
    }

    /** {@inheritDoc} */
    public void onLoadException(Throwable error) {
    }

    /** {@inheritDoc} */
    public void onRemove(Store store, Record record, int index) {
    }

    /** {@inheritDoc} */
    public void onUpdate(Store store, Record record, Record.Operation operation) {
        UtilUi.logInfo("onUpdate : " + operation.getOperation() + " : " + UtilUi.toString(record), MODULE, "onUpdate");
        // check if we are editing the new record line
        if (operation == Record.EDIT && !recordExists(record)) {
            // if canDelete is already set no need to done anything else
            // else we insert a blank record and set canDelete
            if (!Permissions.canDelete(record)) {
                Permissions.setCanDelete(true, record);
                addRowIfCreatePermission(-1);
            }
        }
    }

    /**
     * Marks the grid as busy.
     * Should be used when some Asynchronous events are running and some action cannot be performed until they are finished.
     */
    public void markGridBusy() {
        UtilUi.logInfo("grid busy", MODULE, "markGridBusy");
        getEl().mask(UtilUi.MSG.loading());
    }

    /**
     * Marks the grid as not busy.
     * Should be used when Asynchronous finished running and actions can be performed on the grid freely.
     */
    public void markGridNotBusy() {
        UtilUi.logInfo("grid not busy", MODULE, "markGridNotBusy");
        getEl().unmask();
    }

    /**
     * Checks if a cell at given coordinates is editable.
     * @param rowIndex an <code>int</code> value
     * @param colIndex an <code>int</code> value
     * @return a <code>boolean</code> value
     */
    public boolean isEditableCell(int rowIndex, int colIndex) {
        // check the grid global flag
        if (!editable) {
            return false;
        }

        // check for locked cell
        Cell c = new Cell(rowIndex, colIndex);
        if (lockedCells.contains(c)) {
            UtilUi.logDebug("Cell is locked " + c, MODULE, "isEditableCell");
            return false;
        }

        // check row (record) permission
        return Permissions.canUpdate(store.getAt(rowIndex));
    }

    /**
     * Checks if a cell at given coordinates is editable. Also this method can be overridden
     * to analyze cell field name and its value. Sometimes we may need to allow/disallow editing
     * based on the existing cell value. By default just calls <code>isEditableCell(int, int)</code>.
     * @param rowIndex an <code>int</code> value
     * @param colIndex an <code>int</code> value
     * @param field a field name
     * @param value this is current value in the cell
     * @return a <code>boolean</code> value
     */
    public boolean isEditableCell(int rowIndex, int colIndex, String field, Object value) {
        return isEditableCell(rowIndex, colIndex);
    }

    /**
     * Gets the row index of the next editable cell for the given column index, starting from below the given current row index..
     * @param currentRow an <code>int</code> value
     * @param cellColIndex an <code>int</code> value
     * @return the row index found, or <code>-1</code> if it reaches the end of the list
     */
    public int getNextEditableCell(int currentRow, int cellColIndex) {
        for (int i = currentRow + 1; i < store.getCount(); i++) {
            if (isEditableCell(i, cellColIndex)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Starts editing the next editable cell for the given column index, starting from below the given current row index.
     * Does nothing if no editable cell is found.
     * @param currentRow an <code>int</code> value
     * @param cellColIndex an <code>int</code> value
     * @see #getNextEditableCell
     */
    public void startEditingNextEditableCell(int currentRow, int cellColIndex) {
        int i = getNextEditableCell(currentRow, cellColIndex);
        if (i >= 0) {
            stopEditing();
            startEditing(i, cellColIndex);
        }
    }

    protected int getCurrentColumnIndex() {
        return columnConfigs.size();
    }
}
