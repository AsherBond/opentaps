/*
 * Copyright (c) 2009 Open Source Strategies, Inc.
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
package org.opentaps.gwt.crmsfa.client.search.form;

import com.gwtext.client.data.Record;
import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.listviews.SearchResultsListView;

/**
 * .
 */
public class CrmsfaSearchResultsListView extends SearchResultsListView {

    /**
     * Default constructor.
     */
    public CrmsfaSearchResultsListView() {
        super("gwtCrmsfaSearch");
    }

    @Override public String getType(Record rec) {
        String type = super.getType(rec);
        if ("SalesOpportunity".equals(type)) {
            return UtilUi.MSG.crmOpportunity();
        } else if ("Account".equals(type)) {
            return UtilUi.MSG.crmAccount();
        } else if ("Contact".equals(type)) {
            return UtilUi.MSG.crmContact();
        }  else if ("Lead".equals(type)) {
            return UtilUi.MSG.crmLead();
        } else {
            return type;
        }
    }

    @Override public String getViewUrl(Record rec) {
        String type = super.getType(rec);
        String id = getRealId(rec);
        if ("SalesOpportunity".equals(type)) {
            return "viewOpportunity?salesOpportunityId=" + id;
        } else if ("Account".equals(type)) {
            return "viewAccount?partyId=" + id;
        } else if ("Contact".equals(type)) {
            return "viewContact?partyId=" + id;
        }  else if ("Lead".equals(type)) {
            return "viewLead?partyId=" + id;
        } else {
            return null;
        }
    }

}

