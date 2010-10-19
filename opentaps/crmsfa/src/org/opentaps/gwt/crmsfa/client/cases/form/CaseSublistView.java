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
package org.opentaps.gwt.crmsfa.client.cases.form;

import org.opentaps.base.constants.RoleTypeConstants;
import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.listviews.CaseListView;
import org.opentaps.gwt.common.client.lookup.configuration.CaseLookupConfiguration;

import com.gwtext.client.core.SortDir;
import com.gwtext.client.data.Record;
import com.gwtext.client.data.Store;
import com.gwtext.client.data.StringFieldDef;
import com.gwtext.client.widgets.grid.CellMetadata;
import com.gwtext.client.widgets.grid.ColumnConfig;
import com.gwtext.client.widgets.grid.Renderer;

public class CaseSublistView extends CaseListView {

    private String partyId;

    public CaseSublistView(String partyId) {
        super();
        this.partyId = partyId;
        init();
    }

    /** {@inheritDoc} */
    @Override
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

        ColumnConfig columnSubject = makeLinkColumn(UtilUi.MSG.partySubject(), idDefinition, new StringFieldDef(CaseLookupConfiguration.INOUT_CUST_REQUEST_NAME), entityViewUrl, true);
        columnSubject.setWidth(280);
        columnSubject.setRenderer(new Renderer() {
            public String render(Object value, CellMetadata cellMetadata, Record record, int rowIndex, int colNum, Store store) {
                // bold priority field if record is updated
                String updated = record.getAsString(CaseLookupConfiguration.OUT_UPDATED);
                String custRequestId = record.getAsString(CaseLookupConfiguration.INOUT_CUST_REQUEST_ID);
                String subject = record.getAsString(CaseLookupConfiguration.INOUT_CUST_REQUEST_NAME);
                if ("Y".equals(updated)) {
                    return "<b><a class=\"linktext\" href='/crmsfa/control/viewCase?custRequestId=" + custRequestId + "'>" + subject + " (" + custRequestId +")</a></b>";
                } else {
                    return "<a class=\"linktext\" href='/crmsfa/control/viewCase?custRequestId=" + custRequestId + "'>" + subject + " (" + custRequestId + ")</a>";
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

    public void filterForContact() {
        setFilter(CaseLookupConfiguration.IN_PARTY_ID_FROM, partyId);
        setFilter(CaseLookupConfiguration.IN_ROLE_TYPE_FROM, RoleTypeConstants.CONTACT);
    }

    public void filterForAccount() {
        setFilter(CaseLookupConfiguration.IN_PARTY_ID_FROM, partyId);
        setFilter(CaseLookupConfiguration.IN_ROLE_TYPE_FROM, RoleTypeConstants.ACCOUNT);
    }
}
