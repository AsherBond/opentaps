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

package org.opentaps.gwt.crmsfa.client.contacts.form;

import org.opentaps.gwt.common.client.UtilUi;
import org.opentaps.gwt.common.client.form.ServiceErrorReader;
import org.opentaps.gwt.common.client.listviews.ContactListView;
import org.opentaps.gwt.common.client.lookup.configuration.PartyLookupConfiguration;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.gwtext.client.core.EventObject;
import com.gwtext.client.core.SortDir;
import com.gwtext.client.data.Record;
import com.gwtext.client.data.Store;
import com.gwtext.client.data.StringFieldDef;
import com.gwtext.client.util.Format;
import com.gwtext.client.widgets.grid.CellMetadata;
import com.gwtext.client.widgets.grid.ColumnConfig;
import com.gwtext.client.widgets.grid.GridPanel;
import com.gwtext.client.widgets.grid.Renderer;
import com.gwtext.client.widgets.grid.event.GridCellListenerAdapter;


/**
 * A list of contacts for a given account.
 */
public class ContactsSublistView extends ContactListView {

    private static final String MODULE = ContactsSublistView.class.getName();

    private final String entityId;
    private boolean isOpportunity = false;
    private Integer deleteColumnIndex;

    /**
     * Constructor with autoLoad parameter, use this constructor if some filters need to be set prior to loading the grid data.
     * @param id the Id of the Account to list the contacts for or an opportunity
     * @param isParentOpportunity ID is sales opportunity identifier if true, account otherwise
     * @param autoLoad sets the grid autoLoad parameter, set to <code>false</code> if some filters need to be set prior to loading the grid data
     */
    public ContactsSublistView(String id, boolean isParentOpportunity, boolean autoLoad) {
        super();
        this.entityId = id;
        isOpportunity = isParentOpportunity;
        setHeader(false);
        setAutoLoad(autoLoad);
        init();
    }

    /** {@inheritDoc} */
    @Override
    public void init() {

        String entityViewUrl = "/crmsfa/control/viewContact?partyId={0}";
        StringFieldDef idDefinition = new StringFieldDef(PartyLookupConfiguration.INOUT_PARTY_ID);

        makeLinkColumn(UtilUi.MSG.crmContactId(), idDefinition, entityViewUrl, true);
        makeLinkColumn(UtilUi.MSG.crmContactName(), idDefinition, new StringFieldDef(PartyLookupConfiguration.INOUT_FRIENDLY_PARTY_NAME), entityViewUrl, true);
        makeColumn(UtilUi.MSG.partyCity(), new StringFieldDef(PartyLookupConfiguration.INOUT_CITY));
        makeColumn(UtilUi.MSG.crmPrimaryEmail(), new StringFieldDef(PartyLookupConfiguration.INOUT_EMAIL));
        makeColumn(UtilUi.MSG.crmPrimaryPhone(), new StringFieldDef(PartyLookupConfiguration.INOUT_FORMATED_PHONE_NUMBER));
        makeColumn(UtilUi.MSG.partyToName(), new StringFieldDef(PartyLookupConfiguration.INOUT_TO_NAME));
        makeColumn(UtilUi.MSG.partyAttentionName(), new StringFieldDef(PartyLookupConfiguration.INOUT_ATTENTION_NAME));
        makeColumn(UtilUi.MSG.partyAddressLine1(), new StringFieldDef(PartyLookupConfiguration.INOUT_ADDRESS));
        makeColumn(UtilUi.MSG.partyAddressLine2(), new StringFieldDef(PartyLookupConfiguration.OUT_ADDRESS_2));
        makeColumn(UtilUi.MSG.partyState(), new StringFieldDef(PartyLookupConfiguration.INOUT_STATE));
        makeColumn(UtilUi.MSG.partyCountry(), new StringFieldDef(PartyLookupConfiguration.INOUT_COUNTRY));
        makeColumn(UtilUi.MSG.partyPostalCode(), new StringFieldDef(PartyLookupConfiguration.INOUT_POSTAL_CODE));
        makeColumn(UtilUi.MSG.crmPostalCodeExt(), new StringFieldDef(PartyLookupConfiguration.OUT_POSTAL_CODE_EXT));

        // add Remove button if user has required permission
        if (hasRemovePermission(isOpportunity)) {
            deleteColumnIndex = getCurrentColumnIndex();
            ColumnConfig config = makeColumn("", new Renderer() {
                public String render(Object value, CellMetadata cellMetadata, Record record, int rowIndex, int colNum, Store store) {
                    return Format.format("<img width=\"15\" height=\"15\" class=\"checkbox\" src=\"{0}\"/>", UtilUi.ICON_DELETE);
                }
            });
            config.setWidth(26);
            config.setResizable(false);
            config.setFixed(true);
            config.setSortable(false);

            addGridCellListener(new GridCellListenerAdapter() {
                private final String actionUrl = isOpportunity ?
                        "/crmsfa/control/removeContactFromOpportunityAJX"
                        : "/crmsfa/control/removeContactFromAccountAJX";

                /** {@inheritDoc} */
                @Override
                public void onCellClick(GridPanel grid, int rowIndex, int colindex, EventObject e) {
                    if (colindex == ContactsSublistView.this.deleteColumnIndex) {
                        String contactPartyId = getStore().getRecordAt(rowIndex).getAsString("partyId");
                        RequestBuilder request = new RequestBuilder(RequestBuilder.POST, actionUrl);
                        request.setHeader("Content-type", "application/x-www-form-urlencoded");
                        if (isOpportunity) {
                            request.setRequestData(Format.format("salesOpportunityId={0}&contactPartyId={1}", ContactsSublistView.this.entityId, contactPartyId));
                        } else {
                            request.setRequestData(Format.format("partyId={0}&accountPartyId={0}&contactPartyId={1}", ContactsSublistView.this.entityId, contactPartyId));
                        }
                        request.setCallback(new RequestCallback() {
                            public void onError(Request request, Throwable exception) {
                                // display error message
                                markGridNotBusy();
                                UtilUi.errorMessage(exception.toString());
                            }
                            public void onResponseReceived(Request request, Response response) {
                                // if it is a correct response, reload the grid
                                markGridNotBusy();
                                UtilUi.logInfo("onResponseReceived, response = " + response, MODULE, "ContactListView.init()");
                                if (!ServiceErrorReader.showErrorMessageIfAny(response, actionUrl)) {
                                    // commit store changes
                                    getStore().reload();
                                    loadFirstPage();
                                }
                            }
                        });

                        try {
                            markGridBusy();
                            UtilUi.logInfo("posting batch", MODULE, "ContactListView.init()");
                            request.send();
                        } catch (RequestException re) {
                            // display error message
                            UtilUi.errorMessage(e.toString(), MODULE, "ContactListView.init()");
                        }

                    }
                }

            });
        }

        configure(PartyLookupConfiguration.URL_FIND_CONTACTS, PartyLookupConfiguration.INOUT_PARTY_ID, SortDir.ASC);

        // by default, hide non essential columns
        setColumnHidden(PartyLookupConfiguration.INOUT_PARTY_ID, true);
        setColumnHidden(PartyLookupConfiguration.INOUT_STATE, true);
        setColumnHidden(PartyLookupConfiguration.INOUT_COUNTRY, true);
        setColumnHidden(PartyLookupConfiguration.INOUT_TO_NAME, true);
        setColumnHidden(PartyLookupConfiguration.INOUT_ATTENTION_NAME, true);
        setColumnHidden(PartyLookupConfiguration.INOUT_ADDRESS, true);
        setColumnHidden(PartyLookupConfiguration.OUT_ADDRESS_2, true);
        setColumnHidden(PartyLookupConfiguration.INOUT_POSTAL_CODE, true);
        setColumnHidden(PartyLookupConfiguration.OUT_POSTAL_CODE_EXT, true);
    }

    /**
     * Contact list in view opportunity must has Remove button if user has
     * CRMSFA_OPP_UPDATE permission. For view account screen required permission
     * is CRMSFA_ACCOUNT_UPDATE.
     * @param isOpportunity <code>true</code> if widget is used on view opportunity page
     * @return put Remove button as last column if user has permission to remove contacts.
     */
    private native boolean hasRemovePermission(boolean isOpportunity)/*-{
        if (isOpportunity && $wnd.securityUser.CRMSFA_OPP_UPDATE) {
            return true;
        } else if ($wnd.securityUser.CRMSFA_ACCOUNT_UPDATE) {
            return true;
        };

        return false;
    }-*/;
}
