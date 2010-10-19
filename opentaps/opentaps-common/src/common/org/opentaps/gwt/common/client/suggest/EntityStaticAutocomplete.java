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

package org.opentaps.gwt.common.client.suggest;

import com.gwtext.client.core.SortDir;
import com.gwtext.client.data.Record;
import com.gwtext.client.data.RecordDef;
import com.gwtext.client.data.Store;
import com.gwtext.client.data.event.StoreListenerAdapter;
import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.lookup.UtilLookup;

/**
 * Base class for ComboBox autocompleters that do not require paging and server side filtering.
 * This class is best suited for building combobox for which the number of possible values is low and do not require pagination.
 */
public abstract class EntityStaticAutocomplete extends EntityAutocomplete {

    private static final String MODULE = EntityAutocomplete.class.getName();

    /**
     * Clone constructor, copy its configuration from the given <code>EntityStaticAutocomplete</code>.
     * @param autocompleter the <code>EntityStaticAutocomplete</code> to clone
     */
    public EntityStaticAutocomplete(EntityStaticAutocomplete autocompleter) {
        super(autocompleter.getFieldLabel(), autocompleter.getName(), autocompleter.getWidth());
        // init with same store
        Store store = autocompleter.getStore();
        // plug its load listener here as well
        store.addStoreListener(new StoreListenerAdapter() {
                @Override public void onLoad(Store store, Record[] records) {
                    onStoreLoad(store, records);
                }

                @Override public void onLoadException(Throwable error) {
                    onStoreLoadError(error);
                }
            });
        if (autocompleter.isLoaded()) {
            UtilUi.logInfo("Store was already loaded: " + UtilUi.toString(this), MODULE, "<Constructor>");
            setLoaded(true);
        }

        init(store, autocompleter);
        // copy the filters
        applyFilters(autocompleter);
    }

    /**
     * Constructor.
     * @param url the URL of the service used to query
     */
    public EntityStaticAutocomplete(String url) {
        this(url, null);
    }

    /**
     * Constructor.
     * @param url the URL of the service used to query
     * @param defaultSortField the field to sort by initially
     */
    public EntityStaticAutocomplete(String url, String defaultSortField) {
        this(url, defaultSortField, true);
    }

    /**
     * Constructor.
     * @param url the URL of the service used to query
     * @param defaultSortField the field to sort by initially
     * @param loadNow should the data be loaded from the server here, set to false if you need to set additional filter before the loading should happen
     */
    public EntityStaticAutocomplete(String url, String defaultSortField, boolean loadNow) {
        this(null, null, 1, url, defaultSortField, loadNow);
    }

    /**
     * Constructor.
     * @param url the URL of the service used to query
     * @param defaultSortField the field to sort by initially
     * @param loadNow should the data be loaded from the server here, set to false if you need to set additional filter before the loading should happen
     * @param recordDef a record def, default to autocompleter default: DEFAULT_RECORD_DEF
     */
    public EntityStaticAutocomplete(String url, String defaultSortField, boolean loadNow, RecordDef recordDef) {
        this(null, null, 1, url, defaultSortField, SortDir.ASC, loadNow, recordDef);
    }

    /**
     * Constructor.
     * @param fieldLabel the field label
     * @param name the field name used in the form
     * @param fieldWidth the field size in pixels
     * @param url the URL of the service used to query
     */
    public EntityStaticAutocomplete(String fieldLabel, String name, int fieldWidth, String url) {
        this(fieldLabel, name, fieldWidth, url, null, null);
    }

    /**
     * Constructor.
     * @param fieldLabel the field label
     * @param name the field name used in the form
     * @param fieldWidth the field size in pixels
     * @param url the URL of the service used to query
     * @param defaultSortField the field to sort by initially
     */
    public EntityStaticAutocomplete(String fieldLabel, String name, int fieldWidth, String url, String defaultSortField) {
        this(fieldLabel, name, fieldWidth, url, defaultSortField, SortDir.ASC);
    }

    /**
     * Constructor.
     * @param fieldLabel the field label
     * @param name the field name used in the form
     * @param fieldWidth the field size in pixels
     * @param url the URL of the service used to query
     * @param defaultSortField the field to sort by initially
     * @param loadNow should the data be loaded from the server here, set to false if you need to set additional filter before the loading should happen
     */
    public EntityStaticAutocomplete(String fieldLabel, String name, int fieldWidth, String url, String defaultSortField, boolean loadNow) {
        this(fieldLabel, name, fieldWidth, url, defaultSortField, SortDir.ASC, loadNow);
    }

    /**
     * Constructor.
     * @param fieldLabel the field label
     * @param name the field name used in the form
     * @param fieldWidth the field size in pixels
     * @param url the URL of the service used to query
     * @param defaultSortField the field to sort by initially
     * @param defaultSortDirection the default sort direction
     */
    public EntityStaticAutocomplete(String fieldLabel, String name, int fieldWidth, String url, String defaultSortField, SortDir defaultSortDirection) {
        this(fieldLabel, name, fieldWidth, url, defaultSortField, defaultSortDirection, true);
    }

    /**
     * Constructor.
     * @param fieldLabel the field label
     * @param name the field name used in the form
     * @param fieldWidth the field size in pixels
     * @param url the URL of the service used to query
     * @param defaultSortField the field to sort by initially
     * @param defaultSortDirection the default sort direction
     * @param loadNow should the data be loaded from the server here, set to false if you need to set additional filter before the loading should happen
     */
    public EntityStaticAutocomplete(String fieldLabel, String name, int fieldWidth, String url, String defaultSortField, SortDir defaultSortDirection, boolean loadNow) {
        this(fieldLabel, name, fieldWidth, url, defaultSortField, defaultSortDirection, loadNow, DEFAULT_RECORD_DEF);
    }

    /**
     * Constructor.
     * @param fieldLabel the field label
     * @param name the field name used in the form
     * @param fieldWidth the field size in pixels
     * @param url the URL of the service used to query
     * @param defaultSortField the field to sort by initially
     * @param defaultSortDirection the default sort direction
     * @param loadNow should the data be loaded from the server here, set to false if you need to set additional filter before the loading should happen
     * @param recordDef a record def, default to autocompleter default: DEFAULT_RECORD_DEF
     */
    public EntityStaticAutocomplete(String fieldLabel, String name, int fieldWidth, String url, String defaultSortField, SortDir defaultSortDirection, boolean loadNow, RecordDef recordDef) {
        super(fieldLabel, name, fieldWidth, url, defaultSortField, defaultSortDirection, recordDef);
        // loading data now
        if (loadNow) {
            loadFirstPage();
        }
    }

    @Override protected void init(Store store) {
        init(store, LOCAL, 0);
        // disable server side paging and return all records
        applyFilter(UtilLookup.PARAM_NO_PAGER, "Y");
    }
}
