/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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

import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.lookup.configuration.OpportunityLookupConfiguration;

import com.google.gwt.i18n.client.NumberFormat;
import com.gwtext.client.core.SortDir;
import com.gwtext.client.data.Record;
import com.gwtext.client.data.Store;
import com.gwtext.client.data.StringFieldDef;
import com.gwtext.client.widgets.grid.CellMetadata;
import com.gwtext.client.widgets.grid.ColumnConfig;
import com.gwtext.client.widgets.grid.Renderer;
/**
 * class for the Find opportunity form + list view pattern.
 *
*/
public class SalesOpportunityListView  extends EntityListView {
    private static final String MODULE = SalesOpportunityListView.class.getName();
    /**
     * Default constructor.
     */
    public SalesOpportunityListView() {
        super();
    }

    /**
     * Constructor giving a title for this list view, which is displayed in the UI.
     * @param title the title label for this list view.
     */
    public SalesOpportunityListView(String title) {
        super(title);
    }

    /**
     * Placeholder to remind extended classes that on of the init methods must be called.
     */
    public void init() {

        init(OpportunityLookupConfiguration.URL_FIND_OPPORTUNITIES, "/crmsfa/control/viewOpportunity?salesOpportunityId={0}", UtilUi.MSG.crmOpportunityId());
    }

    /**
     * Configures the list columns and interaction with the server request that populates it.
     * Constructs the column model and JSON reader for the list with the default columns for Party and extra columns, as well as a link for a view page.
     * @param entityFindUrl the URL of the request to populate the list
     * @param entityViewUrl the URL linking to the entity view page with a placeholder for the ID. The ID column will use it to provide a link to the view page for each record. For example <code>/crmsfa/control/viewContact?partyId={0}</code>. This is optional, if <code>null</code> then no link will be provided
     * @param idLabel the label of the ID column, which depends of the entity that is listed
     */
    protected void init(String entityFindUrl, String entityViewUrl, String idLabel) {

        StringFieldDef idDefinition = new StringFieldDef(OpportunityLookupConfiguration.INOUT_SALES_OPPORTUNITY_ID);
        ColumnConfig columnId = makeLinkColumn(idLabel, idDefinition, idDefinition, entityViewUrl, true);
        columnId.setWidth(100);

        ColumnConfig columnOpportunityName = makeLinkColumn(UtilUi.MSG.crmOpportunityName(), idDefinition, new StringFieldDef(OpportunityLookupConfiguration.INOUT_OPPORTUNITY_NAME), entityViewUrl, true);
        columnOpportunityName.setWidth(180);

        ColumnConfig columnOpportunityStage = makeColumn(UtilUi.MSG.crmStage(), new StringFieldDef(OpportunityLookupConfiguration.OUT_OPPORTUNITY_STAGE));
        columnOpportunityStage.setWidth(60);

        ColumnConfig columnOpportunityAmount = makeColumn(UtilUi.MSG.commonAmount(), new StringFieldDef(OpportunityLookupConfiguration.OUT_ESTIMATED_AMOUNT));
        columnOpportunityAmount.setWidth(80);
        columnOpportunityAmount.setRenderer(new Renderer() {
            // render estimatedAmount as currency
            public String render(Object value, CellMetadata cellMetadata, Record record, int rowIndex, int colNum, Store store) {
                String estimatedAmount = record.getAsString(OpportunityLookupConfiguration.OUT_ESTIMATED_AMOUNT);
                if (estimatedAmount == null) return estimatedAmount;
                NumberFormat fmt = NumberFormat.getCurrencyFormat();
                String formatted = fmt.format(new BigDecimal(estimatedAmount).doubleValue());
                return formatted;
            }
        });

        ColumnConfig columnEstimatedCloseDate = makeColumn(UtilUi.MSG.crmEstClosed(), new StringFieldDef(OpportunityLookupConfiguration.OUT_ESTIMATED_CLOSE_DATE_STRING));
        columnEstimatedCloseDate.setWidth(90);


        ColumnConfig columnPartyFromLink = makeColumn(UtilUi.MSG.crmOpportunityFor(), new StringFieldDef(OpportunityLookupConfiguration.OUT_PARTY_FROM_LINK));
        columnPartyFromLink.setWidth(240);

        makeColumn("", new StringFieldDef(OpportunityLookupConfiguration.OUT_CURRENCY_UOM_ID)).setHidden(true);
        getColumn().setFixed(true);

        configure(entityFindUrl, OpportunityLookupConfiguration.INOUT_SALES_OPPORTUNITY_ID, SortDir.DESC);

    }


    /**
     * Filters the records of the list by showing only those belonging to the user making the request.
     * @param viewPref a <code>Boolean</code> value
     */
    public void filterMyOrTeamParties(String viewPref) {
        setFilter(OpportunityLookupConfiguration.IN_RESPONSIBILTY, viewPref);
    }

    /**
     * Filters the records of the list by opportunity name matching the given opportunity name.
     * @param opportunityName a <code>String</code> value
     */
    public void filterByOpportunityName(String opportunityName) {
        setFilter(OpportunityLookupConfiguration.INOUT_OPPORTUNITY_NAME, opportunityName);
    }

    /**
     * Filters the records of the list by opportunity stage matching the given opportunityStageId.
     * @param opportunityStageId a <code>String</code> value
     */
    public void filterByOpportunityStageId(String opportunityStageId) {
        setFilter(OpportunityLookupConfiguration.INOUT_OPPORTUNITY_STAGE_ID, opportunityStageId);
    }

    /**
     * Filters the records of the list by type enum matching the given typeEnumId.
     * @param typeEnumId a <code>String</code> value
     */
    public void filterByTypeEnumId(String typeEnumId) {
        setFilter(OpportunityLookupConfiguration.INOUT_TYPE_ENUM_ID, typeEnumId);
    }


}
