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

import com.gwtext.client.core.SortDir;
import com.gwtext.client.data.Record;
import com.gwtext.client.data.Store;
import com.gwtext.client.data.StringFieldDef;
import com.gwtext.client.widgets.grid.CellMetadata;
import com.gwtext.client.widgets.grid.Renderer;
import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.lookup.UtilLookup;
import org.opentaps.gwt.common.client.lookup.configuration.SearchLookupConfiguration;

/**
 * Generic list of search results.
 * Application specific search widgets should override <code>getType</code> for localization
 * and <code>getViewUrl</code> to provide links to the results view page.
 */
public class SearchResultsListView extends BaseSearchResultsListView {

    /**
     * Default constructor.
     * @param url the URL of the search service
     */
    public SearchResultsListView(String url) {
        super();
        setDefaultPageSize(50);

        Renderer renderer = new Renderer() {
                public String render(Object value, CellMetadata cellMetadata, Record record, int rowIndex, int colNum, Store store) {
                    return renderSearchResult(record);
                }
            };

        Renderer typeRenderer = new Renderer() {
                public String render(Object value, CellMetadata cellMetadata, Record record, int rowIndex, int colNum, Store store) {
                    return getType(record);
                }
            };

        makeColumn("Type", new StringFieldDef(SearchLookupConfiguration.RESULT_TYPE)).setRenderer(typeRenderer);
        addFieldDefinition(new StringFieldDef(SearchLookupConfiguration.RESULT_ID));
        addFieldDefinition(new StringFieldDef(SearchLookupConfiguration.RESULT_TITLE));
        addFieldDefinition(new StringFieldDef(SearchLookupConfiguration.RESULT_REAL_ID));
        addFieldDefinition(new StringFieldDef(SearchLookupConfiguration.RESULT_DESCRIPTION));

        makeColumn("Text", renderer);

        String groupTpl = "{text} ({[values.rs.length]} {[values.rs.length > 1 ?  \"" + UtilUi.MSG.searchItems() + "\" : \"" + UtilUi.MSG.searchItem() + "\"]})";
        setGrouping(SearchLookupConfiguration.RESULT_TYPE, groupTpl);

        configure(url, UtilLookup.SUGGEST_ID, SortDir.ASC);

        setColumnHidden(SearchLookupConfiguration.RESULT_TYPE, true);
        setHideColumnHeader(true);
    }

    /**
     * Gets the <code>RESULT_TYPE</code> of a search result record.
     * This implementation returns the type as passed by the server, if the types are
     *  known, it is better to translate them into UI labels for localization.
     * @param rec a <code>Record</code> value
     * @return the search result type <code>String</code>, or null if it was empty
     */
    public String getType(Record rec) {
        if (rec.isEmpty(SearchLookupConfiguration.RESULT_TYPE)) {
            return null;
        }
        return rec.getAsString(SearchLookupConfiguration.RESULT_TYPE);
    }

    /**
     * Gets the <code>RESULT_REAL_ID</code> of a search result record.
     * @param rec a <code>Record</code> value
     * @return the search result real id <code>String</code>, or null if it was empty
     */
    public String getRealId(Record rec) {
        if (rec.isEmpty(SearchLookupConfiguration.RESULT_REAL_ID)) {
            return null;
        }
        return rec.getAsString(SearchLookupConfiguration.RESULT_REAL_ID);
    }

    /**
     * Gets the URL to the view page for the search result.
     * This implementation always returns null, application specific search widget
     *  need to override this to provide a link to the view page based on the type and real ID.
     * If this returns null, then no link is rendered by the <code>renderSearchResult</code>.
     * @param rec a <code>Record</code> value
     * @return the search result view page URL <code>String</code>, or null not to provide a link
     */
    public String getViewUrl(Record rec) {
        return null;
    }

    /**
     * Gets the <code>RESULT_DESCRIPTION</code> of a search result record.
     * @param rec a <code>Record</code> value
     * @return the search description <code>String</code>, or null if it was empty
     */
    public String getDescription(Record rec) {
        if (rec.isEmpty(SearchLookupConfiguration.RESULT_DESCRIPTION)) {
            return null;
        }
        return rec.getAsString(SearchLookupConfiguration.RESULT_DESCRIPTION);
    }

    /**
     * Gets the <code>RESULT_TITLE</code> of a search result record.
     * @param rec a <code>Record</code> value
     * @return the search title <code>String</code>, or null if it was empty
     */
    public String getTitle(Record rec) {
        if (rec.isEmpty(SearchLookupConfiguration.RESULT_TITLE)) {
            return null;
        }
        return rec.getAsString(SearchLookupConfiguration.RESULT_TITLE);
    }

    /**
     * Renders a search result.
     * The default implementation is to use <code>getType</code> <code>getTitle</code> <code>getViewUrl</code> and <code>getDescription</code>,
     *  where a link is generated only of <code>getViewUrl</code> return non null.
     <code>
     <a class="linktext" target="_blank" href="{getViewUrl}">{getType}: {getTitle}</a>
     <br/>{getDescription}
     </code>
     * Application specific search widgets can override this if a more complex formatting is needed or there are additional fields to handle.
     * @param record the search result <code>Record</code> to render
     * @return a <code>String</code> value
     */
    public String renderSearchResult(Record record) {
        String type = getType(record);
        String viewUrl = getViewUrl(record);
        String title = getTitle(record);
        String description = getDescription(record);
        StringBuilder sb = new StringBuilder();
        if (viewUrl != null) {
            sb.append("<a class=\"linktext\" target=\"_blank\" href=\"").append(viewUrl).append("\">");
        }
        sb.append(type).append(": ").append(title);
        if (viewUrl != null) {
            sb.append("</a>");
        }
        if (description != null) {
            sb.append("<br/>").append(description);
        }
        return sb.toString();
    }

}

