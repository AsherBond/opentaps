/*
 * Copyright (c) 2008-2009 Open Source Strategies, Inc.
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
package org.opentaps.analytics.gwt.query.client;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.WindowResizeListener;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.RootPanel;
import com.gwtext.client.core.EventObject;
import com.gwtext.client.core.Margins;
import com.gwtext.client.core.RegionPosition;
import com.gwtext.client.core.SortDir;
import com.gwtext.client.core.TextAlign;
import com.gwtext.client.data.ArrayReader;
import com.gwtext.client.data.BooleanFieldDef;
import com.gwtext.client.data.FieldDef;
import com.gwtext.client.data.GroupingStore;
import com.gwtext.client.data.MemoryProxy;
import com.gwtext.client.data.Record;
import com.gwtext.client.data.RecordDef;
import com.gwtext.client.data.SimpleStore;
import com.gwtext.client.data.SortState;
import com.gwtext.client.data.Store;
import com.gwtext.client.data.StoreTraversalCallback;
import com.gwtext.client.data.StringFieldDef;
import com.gwtext.client.widgets.Button;
import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.ToolbarButton;
import com.gwtext.client.widgets.Viewport;
import com.gwtext.client.widgets.event.ButtonListenerAdapter;
import com.gwtext.client.widgets.form.ComboBox;
import com.gwtext.client.widgets.form.event.ComboBoxCallback;
import com.gwtext.client.widgets.form.event.ComboBoxListenerAdapter;
import com.gwtext.client.widgets.grid.CellMetadata;
import com.gwtext.client.widgets.grid.ColumnConfig;
import com.gwtext.client.widgets.grid.ColumnModel;
import com.gwtext.client.widgets.grid.EditorGridPanel;
import com.gwtext.client.widgets.grid.GridEditor;
import com.gwtext.client.widgets.grid.GridPanel;
import com.gwtext.client.widgets.grid.GridView;
import com.gwtext.client.widgets.grid.GroupingView;
import com.gwtext.client.widgets.grid.Renderer;
import com.gwtext.client.widgets.grid.RowSelectionModel;
import com.gwtext.client.widgets.grid.event.GridCellListenerAdapter;
import com.gwtext.client.widgets.grid.event.GridListenerAdapter;
import com.gwtext.client.widgets.grid.event.GridRowListenerAdapter;
import com.gwtext.client.widgets.layout.BorderLayout;
import com.gwtext.client.widgets.layout.BorderLayoutData;
import com.gwtext.client.widgets.layout.FitLayout;
import com.gwtext.client.widgets.menu.CheckItem;
import com.gwtext.client.widgets.menu.Menu;
import com.gwtext.client.widgets.menu.event.CheckItemListenerAdapter;

/**
 * Mark conditions w/ specified ID unselected and return them to
 * common pull of conditions.
 */
class UnselectCallback implements StoreTraversalCallback {
    private String id = null;

    public UnselectCallback(String id) {
        this.id = id;
    }

    public boolean execute(Record record) {
        if (id.equals(record.getAsString("id"))) {
            record.set("selected", false);
            return false;
        }
        return true;
    }

}

/**
 * QueryBuilder GWT application.
 * This application allow select report conditions, assign its values
 * and export report to one of the possible formats.
 */
public class QueryBuilder implements EntryPoint {

    public static final String QUERY_BUILDER_ID = "queryBuilderWidget";

    public static Panel borderPanel;

    /** Localized strings accessor. */
    private QueryBuilderMessages messages = GWT.create(QueryBuilderMessages.class);

    /**
     * Defines the record for available conditions.
     * <ol>
     *   <lh>Fields as follow:</lh><br/>
     *   <li><b>id</b>: condition unique identifier.
     *   <li><b>selected</b>: the condition is filtered off if this field <code>true</code>.
     *   <li><b>fieldName</b>: exact database name for the condition.
     *   <li><b>label</b>: condition friendly name.
     *   <li><b>dimension</b>: all available conditions are grouped by this field.
     *   <li><b>value</b>: actually value is always String and has to be converted according to javaType.
     *   <li><b>javaType</b>: Java type of value, used to convert value.
     * </ol>
     */
    private RecordDef recordDef = new RecordDef(
            new FieldDef[] {
                    new StringFieldDef("id"),
                    new BooleanFieldDef("selected"),
                    new StringFieldDef("fieldName"),
                    new StringFieldDef("label"),
                    new StringFieldDef("dimension"),
                    new StringFieldDef("value"),
                    new StringFieldDef("javaType"),
                    new StringFieldDef("operator")
            }
    );

    /** The reader for the available conditions store. */
    private ArrayReader availableConditionsReader = new ArrayReader(recordDef);

    /** Stores the available conditions, grouped by dimension. */
    private GroupingStore availableConditionsStore;

    /** Views the available conditions, grouped by dimension. */
    private GridPanel availableConditionsGrid;

    /** Filter the available conditions list removing the ones 
        that have been selected. */
    private StoreTraversalCallback availableConditionsVisibilityFilter = new StoreTraversalCallback() {
        public boolean execute(Record record) {
            return !record.getAsBoolean("selected");
        }
    };

    /**
     * Layout of the table of selected conditions.
     * Columns are: the name of the parameter, the value used to filter by,
     * and a placeholder for the delete button.
     */
    private ColumnConfig[] selectionColumns = new ColumnConfig[] {
            new ColumnConfig(messages.ParameterParameterName(), "label", 300),
            new ColumnConfig(messages.ParameterOperator(), "operator", 150),
            new ColumnConfig(messages.ParameterValue(), "value", 200),
            new ColumnConfig()
    };

    private ArrayReader reader1 = new ArrayReader(recordDef);

    /** Stores the selected conditions. */
    private Store selectedConditionsStore;

    /** Views the selected conditions. */
    private GridPanel selectedConditionsGrid;

    /** Value of the report format, PDF, will be given as parameter to 
        the URL generating the report . */
    public static final String FORMAT_PDF = "pdf";
    /** Value of the report format, HTML, will be given as parameter to
        the URL generating the report. */
    public static final String FORMAT_HTML = "html";
    /** Value of the report format, XLS, will be given as parameter to
        the URL generating the report. */
    public static final String FORMAT_XLS = "xls";
    /** Value of the report format, RTF, will be given as parameter to
        the URL generating the report. */
    public static final String FORMAT_RTF = "rtf";
    /** Value of the report format, ODT, will be given as parameter to
        the URL generating the report. */
    public static final String FORMAT_ODT = "odt";
    /** Value of the report format, CSV, will be given as parameter to
        the URL generating the report. */
    public static final String FORMAT_CSV = "csv";
    /** Value of the report format, XML, will be given as parameter to
        the URL generating the report. */
    public static final String FORMAT_XML = "xml";

    /** Map report format to MIME type. */
    public static final String[][] mimeTypeMap = {
        {"pdf", "application/pdf"},
        {"html", "text/html"},
        {"xls", "application/vnd.ms-excel"},
        {"rtf", "application/rtf"},
        {"odt", "application/vnd.oasis.opendocument.text"},
        {"csv", "text/csv"},
        {"xml", "text/xml"}
    };

    /** Button used to select the report format. */
    private ToolbarButton reportFormatButton;

    /** Stores the currently selected report format. */
    private String reportFormatValue;

    /** Action identifier. */
    public static final String RUN_REPORT_ID = "_qb_runReport";
    /** Action identifier. */
    public static final String RUN_REPORT_IN_BACKGROUND_ID = "_qb_runReportInBackground";

    /** Client RPC proxy. */
    private QueryBuilderServiceAsync srvc = null;

    /** List of possible values for condition, used in combobox editor. */
    private SimpleStore storePossibValues = new SimpleStore("value", new String[] {});
    private RecordDef recordPossibValues = 
        new RecordDef(new FieldDef[] {new StringFieldDef("value")});

    /** List of possible operators for comparison. */
    private SimpleStore storeOperators = new SimpleStore("operator", new String[] {});
    private RecordDef recordOperators = 
        new RecordDef(new FieldDef[] {new StringFieldDef("operator")});

    private Map<String, List<String>> conditionValues = 
        new HashMap<String, List<String>>();

    /** Callback used to populate the available conditions store. */
    private AsyncCallback<List<ConditionDef>> availableConditionsStoreCallback = 
        new AsyncCallback<List<ConditionDef>>() {

        public void onFailure(Throwable caught) {
            Window.alert(messages.ErrorServerCommunication() + " " + caught.getMessage());
        }

        public void onSuccess(List<ConditionDef> result) {
            for (ConditionDef item : result) {
                Record r = recordDef.createRecord(item.toArray());
                availableConditionsStore.add(r);
            }
        }

    };

    /** Callback used to autocomplete the value input for a selected condition. */
    private AsyncCallback<List<ValueDef>> autoCompleteCallback = new AsyncCallback<List<ValueDef>>() {

        public void onFailure(Throwable caught) {
            Window.alert(messages.ErrorServerCommunication() + " " + caught.getMessage());
        }

        public void onSuccess(List<ValueDef> result) {
            if (result == null || result.size() == 0) {
                return;
            }
            ValueDef vdef = result.get(0);
            List<String> values = new ArrayList<String>();
            for (ValueDef item : result) {
                values.add(item.getValue());
            }
            QueryBuilder.this.conditionValues.put(vdef.getConditionId(), values);
        }

    };

    private boolean isConditionsValid = true;

    /**
     * GWT Entry method.
     */
    public void onModuleLoad() {

        RootPanel queryBuilderPanel = RootPanel.get(QUERY_BUILDER_ID);
        if (queryBuilderPanel != null) {

            srvc = (QueryBuilderServiceAsync) GWT.create(QueryBuilderService.class);
            ServiceDefTarget endpoint = (ServiceDefTarget) srvc;
            String serviceUrl = null;
            if (isOpentaps()) {
                serviceUrl = "/analytics/control/gwtrpc/" + "QueryBuilderService";
            } else {
                serviceUrl = GWT.getModuleBaseURL() + "QueryBuilderService";
            }
            endpoint.setServiceEntryPoint(serviceUrl);

            // main page container
            Panel mainFrame = new Panel();
            mainFrame.setBorder(false);
            mainFrame.setPaddings(0);
            mainFrame.setLayout(new FitLayout());

            borderPanel = new Panel();
            borderPanel.setLayout(new BorderLayout());

            initializeBottomTools(borderPanel);

            //----------------------------------------------------------
            // available conditions panel (on the left side of the page)
            Panel westPanel = new Panel();
            westPanel.setTitle(messages.AvailableConditions());
            westPanel.setCollapsible(true);
            westPanel.setWidth(250);
            westPanel.setLayout(new FitLayout());

            availableConditionsStore = new GroupingStore();
            availableConditionsStore.setReader(availableConditionsReader);
            availableConditionsStore.setDataProxy(new MemoryProxy(new Object[][]{}));
            // present the conditions grouped by dimension, sorted alphabetically by label
            availableConditionsStore.setSortInfo(new SortState("label", SortDir.ASC));
            availableConditionsStore.setGroupField("dimension");
            availableConditionsStore.load();
            // fill store using the RPC service
            srvc.getAvailableConditions(
                    Window.Location.getParameter("report"), 
                    availableConditionsStoreCallback
            );
            availableConditionsStore.filterBy(availableConditionsVisibilityFilter);

            // configure the list view of available conditions
            ColumnConfig[] columns = new ColumnConfig[]{
                    new ColumnConfig(messages.ParameterName(), "label", 160, true, null, "label"),
                    new ColumnConfig(messages.ParameterDimension(), "dimension", 0, true, null, "dimension") // this column has a width of 0, it will be hidden
            };
            ColumnModel columnModel = new ColumnModel(columns);

            availableConditionsGrid = new GridPanel();
            availableConditionsGrid.setStore(availableConditionsStore);
            availableConditionsGrid.setColumnModel(columnModel);
            availableConditionsGrid.setFrame(false);
            availableConditionsGrid.setStripeRows(false);
            availableConditionsGrid.setAutoExpandColumn("dimension");
            availableConditionsGrid.setHideColumnHeader(true);
            availableConditionsGrid.setEnableDragDrop(false);
            availableConditionsGrid.setSelectionModel(new RowSelectionModel(true)); // disable multiple row selection

            GroupingView availableConditionsView = new GroupingView();
            availableConditionsView.setForceFit(true);
            availableConditionsView.setHideGroupedColumn(true);
            availableConditionsView.setScrollOffset(0);
            availableConditionsGrid.setView(availableConditionsView);

            westPanel.add(availableConditionsGrid);

            // add the panel in the main layout
            BorderLayoutData westData = new BorderLayoutData(RegionPosition.WEST);
            westData.setSplit(isOpentaps() ? false : true);
            westData.setMinSize(175);
            westData.setMaxSize(400);
            westData.setMargins(new Margins(5, 5, 5, 5));
            westData.setCMargins(new Margins(5, 5, 5, 5));
            borderPanel.add(westPanel, westData);

            //--------------------------
            // selected conditions panel
            Panel centerPanel = new Panel();
            centerPanel.setAutoScroll(true);
            centerPanel.setLayout(new FitLayout());

            selectedConditionsGrid = new EditorGridPanel();
            selectedConditionsGrid.setSelectionModel(new RowSelectionModel(true)); // disable multiple row selection
            selectedConditionsGrid.setTitle(messages.SelectedConditions());
            selectedConditionsGrid.setEnableColumnHide(false);
            selectedConditionsGrid.setEnableColumnMove(false);
            selectedConditionsGrid.setEnableHdMenu(false);
            selectedConditionsStore = 
                new Store(new MemoryProxy(new Object[][]{}), reader1);
            selectedConditionsStore.load();

            storePossibValues.load();

            ComboBox cbValue = new ComboBox();
            cbValue.setStore(storePossibValues);
            cbValue.setDisplayField("value");
            cbValue.setMode(ComboBox.LOCAL);
            cbValue.setSelectOnFocus(true);
            cbValue.setHideTrigger(true);

            cbValue.addListener(new ComboBoxListenerAdapter() {
                @Override
                public boolean doBeforeQuery(ComboBox comboBox, ComboBoxCallback cb) {
                    storePossibValues.removeAll();
                    queryConditionValues();
                    return super.doBeforeQuery(comboBox, cb);
                }
            });

            storeOperators.load();

            ComboBox cbOperator = new ComboBox();
            cbOperator.setStore(storeOperators);
            cbOperator.setDisplayField("operator");
            cbOperator.setMode(ComboBox.LOCAL);
            cbOperator.setSelectOnFocus(true);
            cbOperator.setReadOnly(true);

            cbOperator.addListener(new ComboBoxListenerAdapter() {
                @Override
                public boolean doBeforeQuery(ComboBox comboBox, ComboBoxCallback cb) {
                    Record current = 
                        selectedConditionsGrid.getSelectionModel().getSelected();

                    if (current != null) {
                        String type = current.getAsString("javaType");
                        if (type != null && type.length() > 0) {
                            storeOperators.removeAll();
                            storeOperators.add(recordOperators.createRecord(new String[] {ConditionDef.OPERATORS.EQUALS.getOperatorName()}));
                            storeOperators.add(recordOperators.createRecord(new String[] {ConditionDef.OPERATORS.NOT_EQUALS.getOperatorName()}));
                            storeOperators.add(recordOperators.createRecord(new String[] {ConditionDef.OPERATORS.GREATER.getOperatorName()}));
                            storeOperators.add(recordOperators.createRecord(new String[] {ConditionDef.OPERATORS.GREATER_OR_EQUALS.getOperatorName()}));
                            storeOperators.add(recordOperators.createRecord(new String[] {ConditionDef.OPERATORS.LESS.getOperatorName()}));
                            storeOperators.add(recordOperators.createRecord(new String[] {ConditionDef.OPERATORS.LESS_OR_EQUALS.getOperatorName()}));
                            if ("java.lang.String".equals(type)) {
                                storeOperators.add(recordOperators.createRecord(new String[] {ConditionDef.OPERATORS.LIKE.getOperatorName()}));
                                storeOperators.add(recordOperators.createRecord(new String[] {ConditionDef.OPERATORS.DONTLIKE.getOperatorName()}));
                            };
                        }
                    }

                    return super.doBeforeQuery(comboBox, cb);
                }
            });

            selectionColumns[1].setEditor(new GridEditor(cbOperator));
            selectionColumns[2].setEditor(new GridEditor(cbValue));

            selectionColumns[3].setAlign(TextAlign.LEFT);
            selectionColumns[3].setWidth(26);
            selectionColumns[3].setFixed(true);
            selectionColumns[3].setSortable(false);
            selectionColumns[3].setRenderer(new Renderer() {
                public String render(Object value, CellMetadata cellMetadata, Record record, int rowIndex, int colNum, Store store) {
                    return "<img width=\"15\" height=\"15\" class=\"checkbox\" src=\"" + GWT.getModuleBaseURL() + "/delete.png\"/>";
                }
            });

            ColumnModel selectionColumnModel = new ColumnModel(selectionColumns);
            selectedConditionsGrid.setColumnModel(selectionColumnModel);
            selectedConditionsGrid.setStore(selectedConditionsStore);
            selectedConditionsGrid.setEnableDragDrop(false);
            GridView selectedConditionsView = new GridView();
            selectedConditionsGrid.setView(selectedConditionsView);

            centerPanel.add(selectedConditionsGrid);

            BorderLayoutData centerData = new BorderLayoutData(RegionPosition.CENTER);
            centerData.setMargins(5, 0, 5, 5);
            borderPanel.add(centerPanel, centerData);
            mainFrame.add(borderPanel);

            //-------------------------------------------------
            // add event handlers for mouse and keyboard inputs

            availableConditionsGrid.addGridRowListener(new GridRowListenerAdapter() {

                @Override
                public void onRowDblClick(GridPanel grid, int rowIndex, EventObject e) {
                    QueryBuilder.this.addCondition();
                    super.onRowDblClick(grid, rowIndex, e);
                }
            });

            availableConditionsGrid.addGridListener(new GridListenerAdapter() {
                @Override
                public void onKeyPress(EventObject e) {
                    if (e.getKey() == EventObject.ENTER) {
                        QueryBuilder.this.addCondition();
                    }
                    super.onKeyPress(e);
                }
            });

            selectedConditionsGrid.addGridListener(new GridListenerAdapter() {

                @Override
                public void onKeyPress(EventObject e) {
                    if (e.getKey() == EventObject.DELETE) {
                        QueryBuilder.this.removeCondition();
                    }
                    super.onKeyPress(e);
                }

            });

            selectedConditionsGrid.addGridCellListener(new GridCellListenerAdapter() {
                @Override
                public void onCellClick(GridPanel grid, int rowIndex, int colindex, EventObject e) {
                    if (colindex == 3) {
                        QueryBuilder.this.removeCondition();
                    }
                }
            });

            if (isOpentaps()) {

                Window.addWindowResizeListener(new WindowResizeListener() {

                    public void onWindowResized(int width, int height) {
                        borderPanel.doLayout();
                    }

                });
                installFixLayoutCallback();
                queryBuilderPanel.add(borderPanel);
                borderPanel.setHeight(680);
            } else {
                new Viewport(mainFrame);
            }
        }
    }

    private void addReportOutputOption(final String label, final String format, Menu menu) {
        addReportOutputOption(label, format, menu, false);
    }

    private void addReportOutputOptionSelected(final String label, final String format, Menu menu) {
        addReportOutputOption(label, format, menu, true);
        reportFormatValue = format;
    }

    private void addReportOutputOption(final String label, final String format, Menu menu, Boolean selected) {
        CheckItem item = new CheckItem();
        item.setText(label);
        item.setChecked(selected);
        item.setGroup("format");
        item.addListener(new CheckItemListenerAdapter() {
            @Override
            public void onCheckChange(CheckItem item, boolean checked) {
                // set the label of the selector button to this item label and 
                // store the new format value
                if (reportFormatButton != null && checked) {
                    reportFormatButton.setText(item.getText());
                    reportFormatValue = format;
                }
            }
        });
        menu.addItem(item);
    }

    private void addRunReportButton(String label, String buttonId, Panel toolbar) {
        ToolbarButton toolbarButton = new ToolbarButton(label);
        toolbarButton.setId(buttonId);
        toolbarButton.addListener(new ButtonListenerAdapter() {
            @Override
            public void onClick(Button button, EventObject e) {
                QueryBuilder.this.onDoReport(button);
            }

        });
        toolbar.addButton(toolbarButton);
    }

    /**
     * Builds the toolbar containing a selector for the report output format
     * and two action buttons to run the report either directly or in the background.
     * @param container a <code>Panel</code> value
     */
    private Panel initializeBottomTools(Panel container) {

        Panel toolbar = new Panel();
        toolbar.setCollapsible(false);
        toolbar.setLayout(new FitLayout());

        Menu radioMenu = new Menu();
        radioMenu.setShadow(true);
        radioMenu.setMinWidth(20);

        addReportOutputOptionSelected(messages.ReportOutputPDF(), FORMAT_PDF, radioMenu);
        addReportOutputOption(messages.ReportOutputHTML(), FORMAT_HTML, radioMenu);
        addReportOutputOption(messages.ReportOutputXLS(), FORMAT_XLS, radioMenu);
        addReportOutputOption(messages.ReportOutputRTF(), FORMAT_RTF, radioMenu);
        addReportOutputOption(messages.ReportOutputODT(), FORMAT_ODT, radioMenu);
        addReportOutputOption(messages.ReportOutputCSV(), FORMAT_CSV, radioMenu);
        addReportOutputOption(messages.ReportOutputXML(), FORMAT_XML, radioMenu);
        reportFormatButton = new ToolbarButton(messages.ReportOutputPDF(), radioMenu);
        toolbar.addButton(reportFormatButton);

        addRunReportButton(messages.runReport(), RUN_REPORT_ID, toolbar);
        if (!isOpentaps()) {
            addRunReportButton(messages.runReportInBackground(), RUN_REPORT_IN_BACKGROUND_ID, toolbar);
        }

        BorderLayoutData southData = new BorderLayoutData(isOpentaps() ? RegionPosition.NORTH : RegionPosition.SOUTH);
        southData.setSplit(false);
        southData.setMargins(new Margins(0, 0, 0, 0));
        southData.setCMargins(new Margins(0, 0, 0, 0));
        container.add(toolbar, southData);

        return toolbar;
    };

    /**
     * Adds a condition from the available conditions.
     */
    protected void addCondition() {
        Record selectedRecord = 
            availableConditionsGrid.getSelectionModel().getSelected();

        if (selectedRecord == null) {
            return;
        }

        selectedRecord.set("selected", true);
        availableConditionsStore.commitChanges();
        availableConditionsStore.filterBy(availableConditionsVisibilityFilter);

        selectedConditionsStore.add(selectedRecord.copy());
        srvc.getConditionValues(selectedRecord.getAsString("id"), autoCompleteCallback);
    }

    /**
     * Removes one of the selected conditions.
     */
    protected void removeCondition() {
        Record recordToRemove = 
            selectedConditionsGrid.getSelectionModel().getSelected();
        if (recordToRemove == null) {
            return;
        }

        String id = recordToRemove.getAsString("id");
        if (id != null) {
            conditionValues.remove(id);
        }
        selectedConditionsStore.remove(recordToRemove);

        UnselectCallback cb = 
            new UnselectCallback(recordToRemove.getAsString("id"));
        availableConditionsStore.clearFilter();
        availableConditionsStore.each(cb);
        availableConditionsStore.filterBy(availableConditionsVisibilityFilter);
    }

    /**
     * Gets the auto completion data for the current condition value.
     */
    protected void queryConditionValues() {
        Record current = 
            selectedConditionsGrid.getSelectionModel().getSelected();

        if (current == null) {
            return;
        }

        String id = current.getAsString("id");
        if (id == null) {
            return;
        }

        List<String> values = conditionValues.get(id);

        if (values != null && values.size() > 0) {
            for (String value : values) {
                storePossibValues.add(recordPossibValues.createRecord(new String[] {value}));
            }
        }

        return;
    }

    /**
     * Event handler, called when user click one of the <b>Report</b> toolbar 
     * buttons.
     * Builds the URL that displays the report with the filters as parameters.
     * @param button a <code>Button</code> value
     */
    protected void onDoReport(Button button) {

        final ReportConditions reportConditions = new ReportConditions();

        selectedConditionsStore.each(new StoreTraversalCallback() {
            public boolean execute(Record record) {
                String javaType = record.getAsString("javaType");
                String value = record.getAsString("value");
                String op = record.getAsString("operator");
                String fieldName = record.getAsString("fieldName");
                if (value != null && value.length() > 0) {
                    if ("java.lang.String".equals(javaType)) {
                        reportConditions.add(
                                fieldName, 
                                value, 
                                (op != null) ? ConditionDef.findOperator(op) : "LIKE"
                        );
                    } else if ("java.lang.Integer".equals(javaType)) {
                        try {
                            reportConditions.add(
                                    fieldName, 
                                    Integer.valueOf(value), 
                                    (op != null) ? ConditionDef.findOperator(op) : "="
                            );
                        } catch (NumberFormatException e) {
                            Window.alert(messages.ErrorIntegerRequired(record.getAsString("label")));
                            isConditionsValid = false;
                        }
                    }
                }
                return true;
            }
        });

        if (isConditionsValid) {

            String targetUrl = null;
            if (!isOpentaps()) {
                String solution = Window.Location.getParameter("solution");
                String path = Window.Location.getParameter("path");
                String report = Window.Location.getParameter("report");

                targetUrl = Window.Location.getProtocol() + "//" + Window.Location.getHost() + "/pentaho/ViewAction?";
                targetUrl += ("solution=" + solution);
                targetUrl += ("&path=" + path);
                targetUrl += ("&action=" + report + ".xaction");
                targetUrl += ("&outputType=" + reportFormatValue);
            } else {
                String reportId = Window.Location.getParameter("reportId");

                targetUrl = Window.Location.getProtocol() + "//" + Window.Location.getHost() + "/crmsfa/control/runReport?";
                targetUrl += ("&reportId=" + URL.encodeComponent(reportId));
                String reportMimeType = null;
                for (String[] mimeType : mimeTypeMap) {
                    if (mimeType[0].equals(reportFormatValue)) {
                        reportMimeType = mimeType[1];
                        break;
                    }
                }
                targetUrl += ("&reportType=" + URL.encodeComponent(reportMimeType));
                targetUrl += ("&jndiDS=analytics");
            }

            String whereExpression = reportConditions.expression();
            if (whereExpression != null && whereExpression.length() > 0) {
                targetUrl += ("&queryConditions=" + URL.encodeComponent(whereExpression));
            }

            // run export
            if (RUN_REPORT_ID.equals(button.getId())) {
                Window.Location.assign(targetUrl);
            } else if (RUN_REPORT_IN_BACKGROUND_ID.equals(button.getId())) {
                targetUrl += "&background=true";
                Window.Location.assign(targetUrl);
            }
        }
    }

    private static native boolean isOpentaps() /*-{
        return $wnd.environment == 'opentaps';
    }-*/;

    /**
     * Callback function that can be used from external JavaScript.<br>
     * It force re-layout of main panel and should be called from page onload handler. 
     */
    public static void fixLayout() {
        QueryBuilder.borderPanel.doLayout();
    }

    /**
     * Assign <code>fixLayout()</code> method to external global name.
     */
    public static native void installFixLayoutCallback() /*-{
        $wnd.fixLayout =
          @org.opentaps.analytics.gwt.query.client.QueryBuilder::fixLayout();
    }-*/;
}
