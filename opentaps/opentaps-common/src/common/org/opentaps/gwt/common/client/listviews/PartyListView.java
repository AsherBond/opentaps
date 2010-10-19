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

import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.lookup.configuration.PartyLookupConfiguration;

import com.gwtext.client.core.SortDir;
import com.gwtext.client.data.Record;
import com.gwtext.client.data.Store;
import com.gwtext.client.data.StringFieldDef;
import com.gwtext.client.util.Format;
import com.gwtext.client.widgets.grid.CellMetadata;
import com.gwtext.client.widgets.grid.ColumnConfig;
import com.gwtext.client.widgets.grid.Renderer;

/**
 * Generic list of Parties.
 */
public abstract class PartyListView extends EntityListView {

    boolean ignoreLinkColumn = false;

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
    protected void init(String entityFindUrl, final String entityViewUrl, String partyIdLabel, String[] nameColumns) {
        final int nameColumnsCount = nameColumns.length / 2;

        // add party id as the first column
        StringFieldDef idDefinition = new StringFieldDef(PartyLookupConfiguration.INOUT_PARTY_ID);
        if (entityViewUrl != null && !ignoreLinkColumn) {
            makeLinkColumn(partyIdLabel, idDefinition, entityViewUrl, true);
            makeLinkColumn(UtilUi.MSG.crmContactName(), idDefinition, new StringFieldDef(PartyLookupConfiguration.INOUT_FRIENDLY_PARTY_NAME), entityViewUrl, true);
        } else {
            makeColumn(partyIdLabel, idDefinition);
            makeColumn(UtilUi.MSG.crmContactName(), new StringFieldDef(PartyLookupConfiguration.INOUT_FRIENDLY_PARTY_NAME));
        }

        // add custom name fields
        for (int i = 0; i < nameColumnsCount; i++) {
            if (entityViewUrl != null && !ignoreLinkColumn) {
                // make them clickable
                makeLinkColumn(nameColumns[2 * i + 1] , idDefinition, new StringFieldDef(nameColumns[2 * i]), entityViewUrl, true);
            } else {
                makeColumn(nameColumns[2 * i + 1], new StringFieldDef(nameColumns[2 * i]));
            }
        }

        // add remaining fields
        makeColumn(UtilUi.MSG.partyToName(), new StringFieldDef(PartyLookupConfiguration.INOUT_TO_NAME));
        makeColumn(UtilUi.MSG.partyAttentionName(), new StringFieldDef(PartyLookupConfiguration.INOUT_ATTENTION_NAME));
        makeColumn(UtilUi.MSG.partyAddressLine1(), new StringFieldDef(PartyLookupConfiguration.INOUT_ADDRESS));
        makeColumn(UtilUi.MSG.partyAddressLine2(), new StringFieldDef(PartyLookupConfiguration.OUT_ADDRESS_2));
        makeColumn(UtilUi.MSG.partyCity(), new StringFieldDef(PartyLookupConfiguration.INOUT_CITY));
        makeColumn(UtilUi.MSG.partyState(), new StringFieldDef(PartyLookupConfiguration.INOUT_STATE));
        makeColumn(UtilUi.MSG.partyCountry(), new StringFieldDef(PartyLookupConfiguration.INOUT_COUNTRY));
        makeColumn(UtilUi.MSG.partyPostalCode(), new StringFieldDef(PartyLookupConfiguration.INOUT_POSTAL_CODE));
        makeColumn(UtilUi.MSG.crmPostalCodeExt(), new StringFieldDef(PartyLookupConfiguration.OUT_POSTAL_CODE_EXT));

        ColumnConfig columnPhone = makeLinkColumn(UtilUi.MSG.partyPhoneNumber(), idDefinition, new StringFieldDef(PartyLookupConfiguration.INOUT_FORMATED_PHONE_NUMBER), entityViewUrl, true);
        columnPhone.setRenderer(new Renderer() {
            public String render(Object value, CellMetadata cellMetadata, Record record, int rowIndex, int colNum, Store store) {
                // bold priority field if record is updated
                String voipEnabled = record.getAsString(PartyLookupConfiguration.OUT_VOIP_ENABLED);
                String formatedPrimaryPhone = record.getAsString(PartyLookupConfiguration.INOUT_FORMATED_PHONE_NUMBER);
                String primaryCountryCode = record.getAsString(PartyLookupConfiguration.INOUT_PHONE_COUNTRY_CODE);
                String primaryAreaCode = record.getAsString(PartyLookupConfiguration.INOUT_PHONE_AREA_CODE);
                String primaryContactNumber = record.getAsString(PartyLookupConfiguration.INOUT_PHONE_NUMBER);
                if ("Y".equals(voipEnabled)) {
                    String link = "<a class=\"linktext\" href=\"javascript:opentaps.makeOutgoingCall('" + primaryCountryCode + "','" + primaryAreaCode + "','" + primaryContactNumber + "');\">" + formatedPrimaryPhone + "</a>";
                    return link;
                } else {
                    return formatedPrimaryPhone;
                }
            }
        });

        ColumnConfig columnEmail = makeLinkColumn(UtilUi.MSG.partyEmailAddress(), idDefinition, new StringFieldDef(PartyLookupConfiguration.INOUT_EMAIL), entityViewUrl, true);
        columnEmail.setRenderer(new Renderer() {
            public String render(Object value, CellMetadata cellMetadata, Record record, int rowIndex, int colNum, Store store) {
                // bold priority field if record is updated
                String email = record.getAsString(PartyLookupConfiguration.INOUT_EMAIL);
                String contactMechIdTo = record.getAsString(PartyLookupConfiguration.OUT_EMAIL_CONTACT_MECH_ID);
                String internalPartyId = record.getAsString(PartyLookupConfiguration.INOUT_PARTY_ID);
                int actionPos = entityViewUrl.lastIndexOf("/");
                String viewAction = entityViewUrl.substring(actionPos + 1, entityViewUrl.indexOf("?"));
                if (contactMechIdTo == null || "".equals(contactMechIdTo)) {
                    return email;
                } else {
                    String url = "<a class=\"linktext\" href='writeEmail?contactMechIdTo=" + contactMechIdTo + "&internalPartyId=" + internalPartyId + "&donePage=" + viewAction + "'>" + email + "</a>";
                    return Format.format(url, internalPartyId);
                }
            }
        });


        // hidden columns
        makeColumn("", new StringFieldDef(PartyLookupConfiguration.INOUT_PHONE_COUNTRY_CODE)).setHidden(true);
        getColumn().setFixed(true);
        makeColumn("", new StringFieldDef(PartyLookupConfiguration.INOUT_PHONE_AREA_CODE)).setHidden(true);
        getColumn().setFixed(true);
        makeColumn("", new StringFieldDef(PartyLookupConfiguration.INOUT_PHONE_NUMBER)).setHidden(true);
        getColumn().setFixed(true);
        makeColumn("", new StringFieldDef(PartyLookupConfiguration.OUT_EMAIL_CONTACT_MECH_ID)).setHidden(true);
        getColumn().setFixed(true);
        makeColumn("", new StringFieldDef(PartyLookupConfiguration.OUT_VOIP_ENABLED)).setHidden(true);
        getColumn().setFixed(true);
        makeColumn("", new StringFieldDef(PartyLookupConfiguration.INOUT_FORMATED_PHONE_NUMBER)).setHidden(true);
        getColumn().setFixed(true);
        makeColumn("", new StringFieldDef(PartyLookupConfiguration.INOUT_EMAIL)).setHidden(true);
        getColumn().setFixed(true);

        configure(entityFindUrl, PartyLookupConfiguration.INOUT_PARTY_ID, SortDir.ASC);

        // by default, hide non essential columns
        setColumnHidden(PartyLookupConfiguration.INOUT_FRIENDLY_PARTY_NAME, true);
        setColumnHidden(PartyLookupConfiguration.INOUT_COUNTRY, true);
        setColumnHidden(PartyLookupConfiguration.INOUT_TO_NAME, true);
        setColumnHidden(PartyLookupConfiguration.INOUT_ATTENTION_NAME, true);
        setColumnHidden(PartyLookupConfiguration.OUT_ADDRESS_2, true);
        setColumnHidden(PartyLookupConfiguration.INOUT_POSTAL_CODE, true);
        setColumnHidden(PartyLookupConfiguration.OUT_POSTAL_CODE_EXT, true);
        setColumnHidden(PartyLookupConfiguration.INOUT_ADDRESS, true);

    }

    /**
     * Set <code>ignoreLinkColumn</code> value.
     * Use when we don't need hyperlinks in ID or name columns, e.g. the list view is
     * embed in another GWT window and you want to handle selection in listener.
     * @param flag new value
     */
    public void setIgnoreLinkColumn(boolean flag) {
        ignoreLinkColumn = flag;
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

    /**
     * Filters the records of the list by the to name of the party matching the given sub string.
     * @param toName a <code>String</code> value
     */
    public void filterByToName(String toName) {
        setFilter(PartyLookupConfiguration.INOUT_TO_NAME, toName);
    }

    /**
     * Filters the records of the list by the attention name of the party matching the given sub string.
     * @param attnName a <code>String</code> value
     */
    public void filterByAttnName(String attnName) {
        setFilter(PartyLookupConfiguration.INOUT_ATTENTION_NAME, attnName);
    }

    /**
     * Filters out the disabled parties, default to <code>false</code>.
     * @param filter a <code>boolean</code> value
     */
    public void filterOutDisabledParties(boolean filter) {
        setFilter(PartyLookupConfiguration.IN_ACTIVE_PARTIES_ONLY, filter ? "Y" : "N", true);
    }

    /**
     * Filters the records of the list by email of the party matching the given sub string.
     * @param number a <code>String</code> value
     */
    public void filterByEmailAddress(String email) {
        setFilter(PartyLookupConfiguration.INOUT_EMAIL, email);
    }

}
