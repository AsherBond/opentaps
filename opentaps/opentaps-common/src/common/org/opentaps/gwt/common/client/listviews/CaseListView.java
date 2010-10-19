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

import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.lookup.configuration.CaseLookupConfiguration;

import com.gwtext.client.core.SortDir;
import com.gwtext.client.data.Record;
import com.gwtext.client.data.Store;
import com.gwtext.client.data.StringFieldDef;
import com.gwtext.client.widgets.grid.CellMetadata;
import com.gwtext.client.widgets.grid.ColumnConfig;
import com.gwtext.client.widgets.grid.Renderer;
/**
 * class for the Find case form + list view pattern.
 *
*/
public class CaseListView  extends EntityListView {

    /**
     * Default constructor.
     */
    public CaseListView() {
        super();
    }

    /**
     * Constructor giving a title for this list view, which is displayed in the UI.
     * @param title the title label for this list view.
     */
    public CaseListView(String title) {
        super(title);
    }

    /**
     * Placeholder to remind extended classes that on of the init methods must be called.
     */
    public void init() {

        init(CaseLookupConfiguration.URL_FIND_CASES, "/crmsfa/control/viewCase?custRequestId={0}", UtilUi.MSG.crmCaseId());
    }

    /**
     * Configures the list columns and interaction with the server request that populates it.
     * Constructs the column model and JSON reader for the list with the default columns for Party and extra columns, as well as a link for a view page.
     * @param entityFindUrl the URL of the request to populate the list
     * @param entityViewUrl the URL linking to the entity view page with a placeholder for the ID. The ID column will use it to provide a link to the view page for each record. For example <code>/crmsfa/control/viewContact?partyId={0}</code>. This is optional, if <code>null</code> then no link will be provided
     * @param idLabel the label of the ID column, which depends of the entity that is listed
     */
    protected void init(String entityFindUrl, String entityViewUrl, String idLabel) {

        // add party id as the first column
        StringFieldDef idDefinition = new StringFieldDef(CaseLookupConfiguration.INOUT_CUST_REQUEST_ID);
        ColumnConfig columnPriority = makeColumn(UtilUi.MSG.commonPriority(), new StringFieldDef(CaseLookupConfiguration.INOUT_PRIORITY));
        columnPriority.setWidth(50);
        columnPriority.setRenderer(new Renderer() {
            public String render(Object value, CellMetadata cellMetadata, Record record, int rowIndex, int colNum, Store store) {
                // bold priority field if record is updated
                String updated = record.getAsString(CaseLookupConfiguration.OUT_UPDATED);
                String priority = record.getAsString(CaseLookupConfiguration.INOUT_PRIORITY);
                if ("Y".equals(updated) && priority != null) {
                    return "<b>" + priority + "</b>";
                } else {
                    return priority;
                }
            }
        });

        ColumnConfig columnId = makeLinkColumn(idLabel, idDefinition, idDefinition, entityViewUrl, true);
        columnId.setWidth(80);
        columnId.setRenderer(new Renderer() {
            public String render(Object value, CellMetadata cellMetadata, Record record, int rowIndex, int colNum, Store store) {
                // bold priority field if record is updated
                String updated = record.getAsString(CaseLookupConfiguration.OUT_UPDATED);
                String custRequestId = record.getAsString(CaseLookupConfiguration.INOUT_CUST_REQUEST_ID);
                if ("Y".equals(updated)) {
                    return "<b><a class=\"linktext\" href='/crmsfa/control/viewCase?custRequestId=" + custRequestId + "'>" + custRequestId + "</a></b>";
                } else {
                    return "<a class=\"linktext\" href='/crmsfa/control/viewCase?custRequestId=" + custRequestId + "'>" + custRequestId + "</a>";
                }
            }
        });

        ColumnConfig columnSubject = makeLinkColumn(UtilUi.MSG.partySubject(), idDefinition, new StringFieldDef(CaseLookupConfiguration.INOUT_CUST_REQUEST_NAME), entityViewUrl, true);
        columnSubject.setWidth(200);
        columnSubject.setRenderer(new Renderer() {
            public String render(Object value, CellMetadata cellMetadata, Record record, int rowIndex, int colNum, Store store) {
                // bold priority field if record is updated
                String updated = record.getAsString(CaseLookupConfiguration.OUT_UPDATED);
                String custRequestId = record.getAsString(CaseLookupConfiguration.INOUT_CUST_REQUEST_ID);
                String subject = record.getAsString(CaseLookupConfiguration.INOUT_CUST_REQUEST_NAME);
                if ("Y".equals(updated)) {
                    return "<b><a class=\"linktext\" href='/crmsfa/control/viewCase?custRequestId=" + custRequestId + "'>" + subject + "</a></b>";
                } else {
                    return "<a class=\"linktext\" href='/crmsfa/control/viewCase?custRequestId=" + custRequestId + "'>" + subject + "</a>";
                }
            }
        });

        ColumnConfig columnStatus = makeColumn(UtilUi.MSG.commonStatus(), new StringFieldDef(CaseLookupConfiguration.OUT_STATUS));
        columnStatus.setWidth(80);
        columnStatus.setRenderer(new Renderer() {
            public String render(Object value, CellMetadata cellMetadata, Record record, int rowIndex, int colNum, Store store) {
                // bold status field if record is updated
                String updated = record.getAsString(CaseLookupConfiguration.OUT_UPDATED);
                String status = record.getAsString(CaseLookupConfiguration.OUT_STATUS);
                if ("Y".equals(updated) && status != null) {
                    return "<b>" + status + "</b>";
                } else {
                    return status;
                }
            }
        });

        ColumnConfig columnType = makeColumn(UtilUi.MSG.commonType(), new StringFieldDef(CaseLookupConfiguration.OUT_CUST_REQUEST_TYPE));
        columnType.setWidth(80);
        columnType.setRenderer(new Renderer() {
            public String render(Object value, CellMetadata cellMetadata, Record record, int rowIndex, int colNum, Store store) {
                // bold status field if record is updated
                String updated = record.getAsString(CaseLookupConfiguration.OUT_UPDATED);
                String custRequestType = record.getAsString(CaseLookupConfiguration.OUT_CUST_REQUEST_TYPE);
                if ("Y".equals(updated) && custRequestType != null) {
                    return "<b>" + custRequestType + "</b>";
                } else {
                    return custRequestType;
                }
            }
        });

        ColumnConfig columnReason = makeColumn(UtilUi.MSG.crmReason(), new StringFieldDef(CaseLookupConfiguration.OUT_REASON));
        columnReason.setWidth(80);
        columnReason.setRenderer(new Renderer() {
            public String render(Object value, CellMetadata cellMetadata, Record record, int rowIndex, int colNum, Store store) {
                // bold status field if record is updated
                String updated = record.getAsString(CaseLookupConfiguration.OUT_UPDATED);
                String reason = record.getAsString(CaseLookupConfiguration.OUT_REASON);
                if ("Y".equals(updated) && reason != null) {
                    return "<b>" + reason + "</b>";
                } else {
                    return reason;
                }
            }
        });

        // a column for status id
        makeColumn("", new StringFieldDef(CaseLookupConfiguration.INOUT_STATUS_ID)).setHidden(true);
        getColumn().setFixed(true);
        // a column for updated
        makeColumn("", new StringFieldDef(CaseLookupConfiguration.OUT_UPDATED)).setHidden(true);
        getColumn().setFixed(true);

        configure(entityFindUrl, CaseLookupConfiguration.INOUT_PRIORITY, SortDir.DESC);

    }

    /**
     * Filters the records of the list by showing only those belonging to the user making the request.
     * @param viewPref a <code>Boolean</code> value
     */
    public void filterMyOrTeamParties(String viewPref) {
        setFilter(CaseLookupConfiguration.IN_RESPONSIBILTY, viewPref);
    }

    /**
     * Filters the records of the list by Customer Request Id matching the given sub string.
     * @param custRequestId a <code>String</code> value
     */
    public void filterByCustRequestId(String custRequestId) {
        setFilter(CaseLookupConfiguration.INOUT_CUST_REQUEST_ID, custRequestId);
    }

    /**
     * Filters the records of the list by case priority matching the given priority.
     * @param priority a <code>String</code> value
     */
    public void filterByPriority(String priority) {
        setFilter(CaseLookupConfiguration.INOUT_PRIORITY, priority);
    }

    /**
     * Filters the records of the list by case status matching the given statusId.
     * @param statusId a <code>String</code> value
     */
    public void filterByStatusId(String statusId) {
        setFilter(CaseLookupConfiguration.INOUT_STATUS_ID, statusId);
    }

    /**
     * Filters the records of the list by case Customer Request Type matching the given custRequestTypeId.
     * @param custRequestTypeId a <code>String</code> value
     */
    public void filterByCustRequestTypeId(String custRequestTypeId) {
        setFilter(CaseLookupConfiguration.INOUT_CUST_REQUEST_TYPE_ID, custRequestTypeId);
    }

    /**
     * Filters the records of the list by Customer Request Id matching the given sub string.
     * @param custRequestName a <code>String</code> value
     */
    public void filterByCustRequestName(String custRequestName) {
        setFilter(CaseLookupConfiguration.INOUT_CUST_REQUEST_NAME, custRequestName);
    }

    @Override protected String getRowExtraClass(Record record, int index, String extraInfo) {
        String statusId = record.getAsString(CaseLookupConfiguration.INOUT_STATUS_ID);
        String classStyle = "case_" + statusId;
        return classStyle;
    }
}
