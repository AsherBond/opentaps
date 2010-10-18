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

package org.opentaps.gwt.common.client.form;

import com.gwtext.client.data.Store;
import com.gwtext.client.data.JsonReader;
import com.gwtext.client.data.RecordDef;
import com.gwtext.client.data.FieldDef;
import com.gwtext.client.data.Record;
import org.opentaps.gwt.common.client.lookup.UtilLookup;

/**
 * An utility class to make reading JSON response from ajax services easier.
 */
public class ServiceSuccessReader {

    private Store store;

    /**
     * Default Constructor, initialize a reader for a response JSON with the given fields.
     * @param fields the definition of the fields expected in the JSON response
     */
    public ServiceSuccessReader(FieldDef[] fields) {
        RecordDef recordDef = new RecordDef(fields);
        JsonReader reader = new JsonReader(recordDef);
        reader.setRoot(UtilLookup.JSON_SUCCESS_RESPONSE);
        reader.setTotalProperty(UtilLookup.JSON_TOTAL);
        store = new Store(reader);

    }

    /**
     * Constructor, initialize a reader for a response JSON with one field.
     * @param field the definition of the only field expected in the JSON response
     */
    public ServiceSuccessReader(FieldDef field) {
        this(new FieldDef[]{field});
    }

    /**
     * Reads the given JSON string.
     * @param json a <code>String</code> value
     */
    public void readResponse(String json) {
        store.loadJsonData(json, false);
    }

    /**
     * Gets a record from the previously read response by {@link #readResponse}.
     * @param idx the row index
     * @return a <code>Record</code> value
     */
    public Record getRecordAt(int idx) {
        return store.getRecordAt(idx);
    }

    /**
     * Gets the field with the given name for the first record.
     * This is a shortcut method as many responses only contain one row.
     * @param fieldName the name of the field to retrieve
     * @return a <code>String</code> value
     */
    public String getAsString(String fieldName) {
        return store.getRecordAt(0).getAsString(fieldName);
    }

    /**
     * Gets the field with the given name for the record at the given row index.
     * @param fieldName the name of the field to retrieve
     * @param idx the row index
     * @return a <code>String</code> value
     */
    public String getAsString(String fieldName, int idx) {
        return store.getRecordAt(idx).getAsString(fieldName);
    }

}
