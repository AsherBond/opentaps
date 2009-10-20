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

import com.gwtext.client.core.SortDir;
import com.gwtext.client.data.StringFieldDef;
import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.lookup.configuration.PartyLookupConfiguration;

/**
 * Generic list of Parties.
 */
public abstract class PartyListView extends EntityListView {

    /**
     * Default constructor.
     */
    public PartyListView() {
        super();
    }

    /**
     * Constructor giving a title for this list view, which is displayed in the UI.
     * @param title the title label for this list view.
     */
    public PartyListView(String title) {
        super(title);
    }

    /**
     * Placeholder to remind extended classes that on of the init methods must be called.
     */
    public abstract void init();

    /**
     * Configures the list columns and interaction with the server request that populates it.
     * Constructs the column model and JSON reader for the list with the default columns for Party and extra columns.
     * @param entityFindUrl the URL of the request to populate the list
     * @param partyIdLabel the label of the ID column, which depends of the entity that is listed
     * @param nameColumns the list of extra columns to insert after the ID column, in the order they should appear. By default the party list provides ID, Country, State, City, Postal code
     */
    protected void init(String entityFindUrl, String partyIdLabel, String[] nameColumns) {
        init(entityFindUrl, null, partyIdLabel, nameColumns);
    }

    /**
     * Configures the list columns and interaction with the server request that populates it.
     * Constructs the column model and JSON reader for the list with the default columns for Party and extra columns, as well as a link for a view page.
     * @param entityFindUrl the URL of the request to populate the list
     * @param entityViewUrl the URL linking to the entity view page with a placeholder for the ID. The ID column will use it to provide a link to the view page for each record. For example <code>/crmsfa/control/viewContact?partyId={0}</code>. This is optional, if <code>null</code> then no link will be provided
     * @param partyIdLabel the label of the ID column, which depends of the entity that is listed
     * @param nameColumns the list of extra columns to insert after the ID column, in the order they should appear. By default the party list provides ID, Country, State, City, Postal code
     */
    protected void init(String entityFindUrl, String entityViewUrl, String partyIdLabel, String[] nameColumns) {
        final int nameColumnsCount = nameColumns.length / 2;

        // add party id as the first column
        StringFieldDef idDefinition = new StringFieldDef(PartyLookupConfiguration.INOUT_PARTY_ID);
        if (entityViewUrl != null) {
            makeLinkColumn(partyIdLabel, idDefinition, entityViewUrl, true);
        } else {
            makeColumn(partyIdLabel, idDefinition);
        }

        // add custom name fields
        for (int i = 0; i < nameColumnsCount; i++) {
            if (entityViewUrl != null) {
                // make them clickable
                makeLinkColumn(nameColumns[2 * i + 1] , idDefinition, new StringFieldDef(nameColumns[2 * i]), entityViewUrl, true);
            } else {
                makeColumn(nameColumns[2 * i + 1], new StringFieldDef(nameColumns[2 * i]));
            }
        }

        // add remaining fields
        makeColumn(UtilUi.MSG.toName(), new StringFieldDef(PartyLookupConfiguration.OUT_TO_NAME));
        makeColumn(UtilUi.MSG.attnName(), new StringFieldDef(PartyLookupConfiguration.OUT_ATTENTION_NAME));
        makeColumn(UtilUi.MSG.address(), new StringFieldDef(PartyLookupConfiguration.INOUT_ADDRESS));
        makeColumn(UtilUi.MSG.address2(), new StringFieldDef(PartyLookupConfiguration.OUT_ADDRESS_2));
        makeColumn(UtilUi.MSG.city(), new StringFieldDef(PartyLookupConfiguration.INOUT_CITY));
        makeColumn(UtilUi.MSG.stateOrProvince(), new StringFieldDef(PartyLookupConfiguration.INOUT_STATE));
        makeColumn(UtilUi.MSG.country(), new StringFieldDef(PartyLookupConfiguration.INOUT_COUNTRY));
        makeColumn(UtilUi.MSG.postalCode(), new StringFieldDef(PartyLookupConfiguration.INOUT_POSTAL_CODE));
        makeColumn(UtilUi.MSG.postalCodeExt(), new StringFieldDef(PartyLookupConfiguration.OUT_POSTAL_CODE_EXT));
        makeColumn(UtilUi.MSG.phoneNumber(), new StringFieldDef(PartyLookupConfiguration.INOUT_FORMATED_PHONE_NUMBER));
        makeColumn(UtilUi.MSG.emailAddress(), new StringFieldDef(PartyLookupConfiguration.OUT_EMAIL));

        configure(entityFindUrl, PartyLookupConfiguration.INOUT_PARTY_ID, SortDir.ASC);

        // by default, hide non essential columns
        setColumnHidden(PartyLookupConfiguration.INOUT_COUNTRY, true);
        setColumnHidden(PartyLookupConfiguration.OUT_TO_NAME, true);
        setColumnHidden(PartyLookupConfiguration.OUT_ATTENTION_NAME, true);
        setColumnHidden(PartyLookupConfiguration.OUT_ADDRESS_2, true);
        setColumnHidden(PartyLookupConfiguration.INOUT_POSTAL_CODE, true);
        setColumnHidden(PartyLookupConfiguration.OUT_POSTAL_CODE_EXT, true);
    }

    /**
     * Filters the records of the list by showing only those belonging to the user making the request.
     * @param viewPref a <code>Boolean</code> value
     */
    public void filterMyOrTeamParties(String viewPref) {
        setFilter(PartyLookupConfiguration.IN_RESPONSIBILTY, viewPref);
    }

    /**
     * Filters the records of the list by party id matching the given sub string.
     * @param partyId a <code>String</code> value
     */
    public void filterByPartyId(String partyId) {
        setFilter(PartyLookupConfiguration.INOUT_PARTY_ID, partyId);
    }

    /**
     * Filters the records of the list by party classification matching the given classification.
     * @param classification a <code>String</code> value
     */
    public void filterByClassification(String classification) {
        setFilter(PartyLookupConfiguration.IN_CLASSIFICATION, classification);
    }

    /**
     * Filters the records of the list by address of the party matching the given sub string.
     * @param address a <code>String</code> value
     */
    public void filterByAddress(String address) {
        setFilter(PartyLookupConfiguration.INOUT_ADDRESS, address);
    }

    /**
     * Filters the records of the list by country of the party matching the given country id.
     * @param countryGeoId a <code>String</code> value
     */
    public void filterByCountry(String countryGeoId) {
        setFilter(PartyLookupConfiguration.INOUT_COUNTRY, countryGeoId);
    }

    /**
     * Filters the records of the list by state / province of the party matching the given state / province id.
     * @param stateProvinceGeoId a <code>String</code> value
     */
    public void filterByStateProvince(String stateProvinceGeoId) {
        setFilter(PartyLookupConfiguration.INOUT_STATE, stateProvinceGeoId);
    }

    /**
     * Filters the records of the list by city of the party matching the given sub string.
     * @param city a <code>String</code> value
     */
    public void filterByCity(String city) {
        setFilter(PartyLookupConfiguration.INOUT_CITY, city);
    }

    /**
     * Filters the records of the list by postal code of the party matching the given sub string.
     * @param postalCode a <code>String</code> value
     */
    public void filterByPostalCode(String postalCode) {
        setFilter(PartyLookupConfiguration.INOUT_POSTAL_CODE, postalCode);
    }

    /**
     * Filters the records of the list by phone country code of the party matching the given sub string.
     * @param countryCode a <code>String</code> value
     */
    public void filterByPhoneCountryCode(String countryCode) {
        setFilter(PartyLookupConfiguration.INOUT_PHONE_COUNTRY_CODE, countryCode);
    }

    /**
     * Filters the records of the list by phone area code of the party matching the given sub string.
     * @param areaCode a <code>String</code> value
     */
    public void filterByPhoneAreaCode(String areaCode) {
        setFilter(PartyLookupConfiguration.INOUT_PHONE_AREA_CODE, areaCode);
    }

    /**
     * Filters the records of the list by phone number of the party matching the given sub string.
     * @param number a <code>String</code> value
     */
    public void filterByPhoneNumber(String number) {
        setFilter(PartyLookupConfiguration.INOUT_PHONE_NUMBER, number);
    }

}
