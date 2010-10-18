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
package org.opentaps.gwt.crmsfa.client.opportunities.form;

import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.listviews.SalesOpportunityListView;
import org.opentaps.gwt.common.client.lookup.configuration.OpportunityLookupConfiguration;

import com.gwtext.client.core.SortDir;
import com.gwtext.client.data.StringFieldDef;
import com.gwtext.client.widgets.grid.ColumnConfig;


public class OpportunitiesSublistView extends SalesOpportunityListView {

    private final String entityViewUrl = "/crmsfa/control/viewOpportunity?salesOpportunityId={0}";
    private final String entityFindUrl = OpportunityLookupConfiguration.URL_FIND_OPPORTUNITIES;

    public OpportunitiesSublistView() {
        super();
        setAutoLoad(false);
        init();
    }

    /** {@inheritDoc} */
    public void init() {
        StringFieldDef idDefinition = new StringFieldDef(OpportunityLookupConfiguration.INOUT_SALES_OPPORTUNITY_ID);
        makeLinkColumn(UtilUi.MSG.crmOpportunityId(), idDefinition, idDefinition, entityViewUrl, true);

        ColumnConfig columnOpportunityName = makeLinkColumn(UtilUi.MSG.crmOpportunityName(), idDefinition, new StringFieldDef(OpportunityLookupConfiguration.INOUT_COMPOSITE_OPPORTUNITY_NAME), entityViewUrl, true);
        columnOpportunityName.setWidth(300);
        makeColumn(UtilUi.MSG.crmStage(), new StringFieldDef(OpportunityLookupConfiguration.OUT_OPPORTUNITY_STAGE));
        makeCurrencyColumn(UtilUi.MSG.crmOpportunityAmount(), new StringFieldDef(OpportunityLookupConfiguration.OUT_CURRENCY_UOM_ID), new StringFieldDef(OpportunityLookupConfiguration.OUT_ESTIMATED_AMOUNT));
        makeColumn(UtilUi.MSG.crmEstimatedCloseDate(), new StringFieldDef(OpportunityLookupConfiguration.OUT_ESTIMATED_CLOSE_DATE_STRING));
        makeColumn("", new StringFieldDef(OpportunityLookupConfiguration.OUT_CURRENCY_UOM_ID)).setHidden(true);
        getColumn().setFixed(true);

        configure(entityFindUrl, OpportunityLookupConfiguration.OUT_ESTIMATED_CLOSE_DATE, SortDir.ASC);

        // hide invisible columns
        setColumnHidden(OpportunityLookupConfiguration.INOUT_SALES_OPPORTUNITY_ID, true);
    }

}
