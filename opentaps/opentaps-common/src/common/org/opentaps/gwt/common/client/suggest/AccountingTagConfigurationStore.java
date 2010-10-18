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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.JsArray;
import com.gwtext.client.data.FieldDef;
import com.gwtext.client.data.IntegerFieldDef;
import com.gwtext.client.data.ObjectFieldDef;
import com.gwtext.client.data.Record;
import com.gwtext.client.data.RecordDef;
import com.gwtext.client.data.Store;
import com.gwtext.client.data.StringFieldDef;
import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.form.field.StaticComboBoxField;
import org.opentaps.gwt.common.client.lookup.configuration.AccountingTagLookupConfiguration;

/**
 * A class that retrieves the accounting tag configuration for an organization and usage type and make it available for other widgets.
 */
public class AccountingTagConfigurationStore extends EntityStaticAutocomplete {

    private static final String MODULE = AccountingTagConfigurationStore.class.getName();

    private Map<Integer, Map<String, Object>> indexedConfiguration = new HashMap<Integer, Map<String, Object>>();

    /**
     * Default constructor.
     * @param organizationPartyId the organizationPartyId
     * @param accountingTagUsageTypeId the usage type
     */
    public AccountingTagConfigurationStore(String organizationPartyId, String accountingTagUsageTypeId) {
        super(AccountingTagLookupConfiguration.URL_FIND_ACCOUNTING_TAGS, AccountingTagLookupConfiguration.OUT_TAG_INDEX, false, new RecordDef(
                 new FieldDef[]{
                     new IntegerFieldDef(AccountingTagLookupConfiguration.OUT_TAG_INDEX),
                     new StringFieldDef(AccountingTagLookupConfiguration.OUT_TAG_TYPE),
                     new StringFieldDef(AccountingTagLookupConfiguration.OUT_TAG_DESCRIPTION),
                     new ObjectFieldDef(AccountingTagLookupConfiguration.OUT_TAG_VALUES)
                 }
         ));
        setOrganizationPartyId(organizationPartyId);
        setAccountingTagUsageTypeId(accountingTagUsageTypeId);
        loadFirstPage();
    }

    // parse the tag configuration when it has been loaded
    // and builds the index configuration
    @SuppressWarnings("unchecked")
    @Override protected void onStoreLoad(Store store, Record[] records) {
        super.onStoreLoad(store, records);
        // index the configuration per index
        indexedConfiguration.clear();
        for (int i = 0; i < getStore().getCount(); i++) {
            Record r = getStore().getRecordAt(i);
            Integer recIndex = r.getAsInteger(AccountingTagLookupConfiguration.OUT_TAG_INDEX);
            Map<String, Object> conf = new HashMap<String, Object>();
            conf.put(AccountingTagLookupConfiguration.OUT_TAG_DESCRIPTION, r.getAsString(AccountingTagLookupConfiguration.OUT_TAG_DESCRIPTION));
            conf.put(AccountingTagLookupConfiguration.OUT_TAG_TYPE, r.getAsString(AccountingTagLookupConfiguration.OUT_TAG_TYPE));
            conf.put(AccountingTagLookupConfiguration.OUT_TAG_VALUES, UtilUi.jsonObjectsToMaps((JsArray) r.getAsObject(AccountingTagLookupConfiguration.OUT_TAG_VALUES), Arrays.asList(AccountingTagLookupConfiguration.OUT_TAG_VALUE_ID, AccountingTagLookupConfiguration.OUT_TAG_VALUE_DESCRIPTION)));
            indexedConfiguration.put(recIndex, conf);
        }
    }

    /**
     * Sets the organization.
     * @param organizationPartyId a <code>String</code> value
     */
    public void setOrganizationPartyId(String organizationPartyId) {
        applyFilter(AccountingTagLookupConfiguration.IN_ORGANIZATION_PARTY_ID, organizationPartyId);
    }

    /**
     * Sets the tag usage type.
     * @param accountingTagUsageTypeId a <code>String</code> value
     */
    public void setAccountingTagUsageTypeId(String accountingTagUsageTypeId) {
        applyFilter(AccountingTagLookupConfiguration.IN_TAG_USAGE_TYPE_ID, accountingTagUsageTypeId);
    }

    /**
     * Gets the tag type name for the given index.
     * @param index the tag index, corresponding to the index of the tag field
     * @return the tag type name for the given index, either the tag description or tag type ID if the description was empty
     */
    public String getTagDescription(Integer index) {
        if (!isLoaded() || indexedConfiguration.isEmpty()) {
            return "NOT LOADED " + index;
        }

        Map<String, Object> conf = indexedConfiguration.get(index);
        if (conf == null) {
            return "No configuration for tag index " + index;
        }

        String description = (String) conf.get(AccountingTagLookupConfiguration.OUT_TAG_DESCRIPTION);
        if (description == null) {
            description = (String) conf.get(AccountingTagLookupConfiguration.OUT_TAG_TYPE);
        }
        return description;
    }

    /**
     * Gets the tag type name for the given index.
     * @param index the tag index, corresponding to the index of the tag field
     * @param valueId the current value ID of the tag
     * @return the tag name for the given index and ID, either the tag description or tag ID if the description was empty
     */
    @SuppressWarnings("unchecked")
    public String getTagValueDescription(Integer index, String valueId) {
        if (valueId == null) {
            return null;
        }

        if (!isLoaded() || indexedConfiguration.isEmpty()) {
            UtilUi.logError("Tag data was not loaded.", MODULE, "getTagValueDescription");
            return valueId;
        }

        Map<String, Object> conf = indexedConfiguration.get(index);
        if (conf == null) {
            UtilUi.logError("No configuration was found for tag index: " + index, MODULE, "getTagValueDescription");
            return valueId;
        }
        List<Map<String, Object>> array = (List<Map<String, Object>>) conf.get(AccountingTagLookupConfiguration.OUT_TAG_VALUES);
        for (Map<String, Object> map : array) {
            String myId = (String) map.get(AccountingTagLookupConfiguration.OUT_TAG_VALUE_ID);
            if (valueId.equals(myId)) {
                return (String) map.get(AccountingTagLookupConfiguration.OUT_TAG_VALUE_DESCRIPTION);
            }
        }
        return valueId;
    }

    /**
     * Gets the indexed tag configuration as a <code>Map</code> of <code>tagIndex: {field: value}</code> where the configuration fields are
     * <code>OUT_TAG_DESCRIPTION</code> the description string for this tag index,
     * <code>OUT_TAG_TYPE</code> the tag type id,
     * <code>OUT_TAG_VALUES</code> the list of possible tag values for this tag index.
     * @return the indexed tag configuration
     */
    public Map<Integer, Map<String, Object>> getIndexedConfiguration() {
        return indexedConfiguration;
    }

    /**
     * Gets the tag values for the given index, the values are a <code>Map</code> where the fields are
     * <code>OUT_TAG_VALUE_DESCRIPTION</code> the description string for this tag value,
     * <code>OUT_TAG_VALUE_ID</code> the tag id.
     * @param index the tag index
     * @return the <code>List</code> of tag values for the given index
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getTagValues(Integer index) {
        Map<String, Object> conf = indexedConfiguration.get(index);
        return (List<Map<String, Object>>) conf.get(AccountingTagLookupConfiguration.OUT_TAG_VALUES);
    }

    /**
     * Builds a <code>StaticComboBoxField</code> form field for selecting a tag value for the given tag index.
     * The field data includes the valid tag values for the given index.
     * @param index the tag index
     * @param fieldWidth the field width of the built input field
     * @return a <code>StaticComboBoxField</code> value
     */
    public StaticComboBoxField makeTagSelector(Integer index, int fieldWidth) {
        List<String> dataList = new ArrayList<String>();
        List<Map<String, Object>> tagValues = getTagValues(index);
        if (tagValues == null) {
            return null;
        }

        for (Map<String, Object> tagValue : tagValues) {
            dataList.add((String) tagValue.get(AccountingTagLookupConfiguration.OUT_TAG_VALUE_ID));
            dataList.add((String) tagValue.get(AccountingTagLookupConfiguration.OUT_TAG_VALUE_DESCRIPTION));
        }
        String[] data = new String[dataList.size()];
        return new StaticComboBoxField(getTagDescription(index), AccountingTagLookupConfiguration.BASE_ACCOUNTING_TAG + index, dataList.toArray(data), fieldWidth, true);
    }

    /**
     * Returns the usage type ID for a given object type ID, eg: PURCHASE_INVOICE ...
     * @param objTypeId the object type ID, eg: PURCHASE_INVOICE, SALES_ORDER, ...
     * @return a <code>String</code> value
     */
    public static String getUsageTypeFor(String objTypeId) {
        if ("COMMISSION_INVOICE".equals(objTypeId)) {
            return "COMM_INV_ITEMS";
        } else if ("PURCHASE_INVOICE".equals(objTypeId)) {
            return "PRCH_INV_ITEMS";
        } else if ("SALES_INVOICE".equals(objTypeId)) {
            return "SALES_INV_ITEMS";
        } else if ("SALES_ORDER".equals(objTypeId)) {
            return "SALES_ORDER_ITEMS";
        } else if ("PURCHASE_ORDER".equals(objTypeId)) {
            return "PRCH_ORDER_ITEMS";
        }

        return null;
    }
}
