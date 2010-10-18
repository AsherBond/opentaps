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

import com.gwtext.client.data.Record;
import com.gwtext.client.data.Store;
import com.gwtext.client.util.Format;
import com.gwtext.client.widgets.grid.CellMetadata;
import com.gwtext.client.widgets.grid.ColumnConfig;
import com.gwtext.client.widgets.grid.Renderer;
import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.lookup.UtilLookup;
import org.opentaps.gwt.common.client.suggest.EntityAutocomplete;

/**
 * A <code>ColumnConfig</code> that translate the value into a description string, using a store or autocompleter.
 * Its primary use is to translate an ID value to its corresponding description, for example "INV_SPROD_ITEM" to "Service Product".
 */
public class DescriptionColumnConfig extends ColumnConfig implements Renderer {

    private EntityAutocomplete translator;
    private String descriptionIndex;
    private String initialFormatter;

    /**
     * Creates a new <code>DescriptionColumnConfig</code>, where the description field is in the data in another field.
     *
     * @param header the column header label
     * @param dataIndex the data index (the field name of the Store associated with the Grid) containing the ID field
     * @param descriptionIndex the data index (the field name of the Store associated with the Grid) containing the description field
     */
    public DescriptionColumnConfig(String header, String dataIndex, String descriptionIndex) {
        super(header, dataIndex);
        this.descriptionIndex = descriptionIndex;
        setRenderer(this);
    }
    /**
     * Creates a new <code>DescriptionColumnConfig</code>, where the description field is in the data in another field and the description string
     *  is built from a String formatter that should match the autocompleter formatting.
     *
     * @param header the column header label
     * @param dataIndex the data index (the field name of the Store associated with the Grid) containing the ID field
     * @param descriptionIndex the data index (the field name of the Store associated with the Grid) containing the description field
     * @param initialFormatter the String used to format the initial displayed string, {0} is the description from the record descriptionIndex, {1} is the id from the record dataIndex
     */
    public DescriptionColumnConfig(String header, String dataIndex, String descriptionIndex, String initialFormatter) {
        super(header, dataIndex);
        this.descriptionIndex = descriptionIndex;
        this.initialFormatter = initialFormatter;
        setRenderer(this);
    }

    /**
     * Creates a new <code>DescriptionColumnConfig</code>, where the description is fetched from the given autocompleter.
     *
     * @param header the column header label
     * @param dataIndex the data index (the field name of the Store associated with the Grid) containing the ID field
     * @param autocompleter the <code>EntityAutocomplete</code> instance that contains the translations
     */
    public DescriptionColumnConfig(String header, String dataIndex, EntityAutocomplete autocompleter) {
        super(header, dataIndex);
        translator = autocompleter;
        setRenderer(this);
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
        String idValue = (String) value;

        // if a translator is given, lookup the description in its store
        if (translator != null) {
            if (!translator.isLoaded()) {
                UtilUi.debug("Not loaded yet !!");
            }

            Store ts = translator.getStore();

            // previous use of the autocompleter could have changed the store filter states
            ts.clearFilter();

            int idx = ts.find(UtilLookup.SUGGEST_ID, idValue, 0, /* anymatch */ false, /* case sensitive */ true);
            Record translatedRecord = null;
            if (idx != -1) {
                translatedRecord = ts.getAt(idx);
            }

            if (translatedRecord != null) {
                return translatedRecord.getAsString(UtilLookup.SUGGEST_TEXT);
            } else {
                return idValue;
            }
        } else if (descriptionIndex != null) {
            // else if the descriptionIndex was given try to get the description from the current record
            String description = record.getAsString(descriptionIndex);
            // formatter is only for unmodified description records, as modified records are automatically formatted by the autocompleter
            if (initialFormatter != null && !record.isModified(descriptionIndex)) {
                if (description != null) {
                    return Format.format(initialFormatter, description, (String) value);
                } else {
                    return idValue;
                }
            } else if (description != null) {
                return description;
            }
        }

        // if all fails return the ID value
        return idValue;
    }

}
