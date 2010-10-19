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

package org.opentaps.gwt.common.client.lookup.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Defines the interface between the server and client for the InvoiceItemLookupService
 * Technically not a java interface, but it defines all the constants needed on both sides
 *  which makes the code more robust.
 */
public abstract class AccountingTagLookupConfiguration {

    private AccountingTagLookupConfiguration() { }

    public static final String URL_FIND_ACCOUNTING_TAGS = "gwtFindAccountingTagsConfiguration";

    public static final String BASE_ACCOUNTING_TAG = "acctgTagEnumId";
    public static final int NUM_ACCOUNTING_TAG = 10;

    public static final String IN_ORGANIZATION_PARTY_ID = "organizationPartyId";
    public static final String IN_TAG_USAGE_TYPE_ID = "accountingTagUsageTypeId";

    public static final String OUT_TAG_INDEX = "index";
    public static final String OUT_TAG_TYPE = "type";
    public static final String OUT_TAG_DESCRIPTION = "description";
    public static final String OUT_TAG_VALUES = "tagValues";

    public static final String OUT_TAG_VALUE_ID = "enumId";
    public static final String OUT_TAG_VALUE_DESCRIPTION = "description";

    public static final List<String> LIST_OUT_FIELDS = Arrays.asList(
        OUT_TAG_INDEX,
        OUT_TAG_TYPE,
        OUT_TAG_DESCRIPTION,
        OUT_TAG_VALUES
    );

    public static final List<String> ACCOUNTING_TAGS_FIELDS = new ArrayList<String>();
    static {
        for (int i = 1; i <= NUM_ACCOUNTING_TAG; i++) {
            ACCOUNTING_TAGS_FIELDS.add(BASE_ACCOUNTING_TAG + i);
        }
    }

    public static List<String> addAccountingTagsToFieldList(List<String> fields) {
        List<String> results = new ArrayList<String>();
        results.addAll(fields);
        results.addAll(ACCOUNTING_TAGS_FIELDS);
        return results;
    }
}
