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

import com.gwtext.client.util.Format;
import com.gwtext.client.data.Record;
import com.gwtext.client.data.Store;
import com.gwtext.client.widgets.grid.ColumnConfig;
import com.gwtext.client.widgets.grid.Renderer;
import com.gwtext.client.widgets.grid.CellMetadata;

/**
 * A ColumnConfig that displays a HTML link.
 * Adapted for id columns that need to link to a view page.
 */
public class LinkColumnConfig extends ColumnConfig implements Renderer {

    private String urlPattern;
    private boolean lookupMode = false;
    private boolean canLookup = false;
    private String idIndex;

    /**
     * Create a new LinkColumnConfig.
     *
     * @param header the column header
     * @param dataIndex the data index (the field name of the Store associated with the Grid)
     * @param urlPattern the url to link to with {0} placeholder where the value should be put
     */
    public LinkColumnConfig(String header, String dataIndex, String urlPattern) {
        this(header, dataIndex, dataIndex, urlPattern, false);
    }

    /**
     * Create a new LinkColumnConfig.
     *
     * @param header the column header
     * @param idIndex the ID index (the field name of the Store associated with the Grid)
     * @param dataIndex the data index (the field name of the Store associated with the Grid)
     * @param urlPattern the url to link to with {0} placeholder where the ID should be put, and {1} where the value should be put
     */
    public LinkColumnConfig(String header, String idIndex, String dataIndex, String urlPattern) {
        this(header, idIndex, dataIndex, urlPattern, false);
    }

    /**
     * Create a new LinkColumnConfig that can also be used for a lookup.
     *
     * @param header the column header
     * @param dataIndex the data index (the field name of the Store associated with the Grid)
     * @param urlPattern the url to link to with {0} placeholder where the value should be put
     * @param lookup if <code>true</code> the link will be replaced by a javascript call that set the value to return when the widget is used as a lookup
     */
    public LinkColumnConfig(String header, String dataIndex, String urlPattern, boolean lookup) {
        this(header, dataIndex, dataIndex, urlPattern, lookup);
    }

    /**
     * Create a new LinkColumnConfig that can also be used for a lookup.
     *
     * @param header the column header
     * @param idIndex the ID index (the field name of the Store associated with the Grid)
     * @param dataIndex the data index (the field name of the Store associated with the Grid)
     * @param urlPattern the url to link to with {0} placeholder where the ID should be put, and {1} where the value should be put
     * @param lookup if <code>true</code> the link will be replaced by a javascript call that set the value to return when the widget is used as a lookup
     */
    public LinkColumnConfig(String header, String idIndex, String dataIndex, String urlPattern, boolean lookup) {
        super(header, dataIndex);
        this.idIndex = idIndex;
        this.urlPattern = urlPattern;
        this.setRenderer(this);
        this.canLookup = lookup;
    }

    /**
     * Create a new LinkColumnConfig.
     *
     * @param header the column header
     * @param dataIndex the data index (the field name of the Store associated with the Grid)
     * @param width the column width
     * @param urlPattern the url to link to with {0} placeholder where the value should be put
     */
    public LinkColumnConfig(String header, String dataIndex, int width, String urlPattern) {
        this(header, dataIndex, width, false, urlPattern);
    }

    /**
     * Create a new LinkColumnConfig.
     *
     * @param header the column header
     * @param dataIndex the data index (the field name of the Store associated with the Grid)
     * @param width the column width
     * @param sortable true for sortable
     * @param urlPattern the url to link to with {0} placeholder where the value should be put
     */
    public LinkColumnConfig(String header, String dataIndex, int width,  boolean sortable, String urlPattern) {
        super(header, dataIndex, width, sortable);
        this.urlPattern = urlPattern;
        this.setRenderer(this);
    }

    /**
     * The implementation of the <code>Renderer</code> interface that produce the content of the cell.
     * @param value the value of the current record for this cell
     * @param cellMetadata a <code>CellMetadata</code> value
     * @param record the current <code>Record</code> value
     * @param rowIndex the row index of the current record
     * @param colNum the column index of this cell
     * @param store a <code>Store</code> value
     * @return the cell content as an HTML string
     */
    public String render(Object value, CellMetadata cellMetadata, Record record, int rowIndex, int colNum, Store store) {
        if (value == null) {
            return "";
        }

        if (lookupMode) {
            return Format.format("<a class=\"linktext\" href=\"javascript:set_value('{0}');\">{1}</a>", record.getAsString(idIndex), (String) value);
        } else {
            return Format.format("<a class=\"linktext\" href=\"" + urlPattern + "\">{1}</a>", record.getAsString(idIndex), (String) value);
        }
    }

    /**
     * Sets the link to be rendered for a lookup.
     */
    public void setLookupMode() {
        if (canLookup) {
            this.lookupMode = true;
        }
    }

}
