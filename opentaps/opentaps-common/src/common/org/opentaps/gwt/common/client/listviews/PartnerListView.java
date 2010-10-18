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

/**
 * A list of Partners.
 */
public class PartnerListView extends PartyListView {

    /**
     * Default constructor.
     * Set the title to the default account list title.
     */
    public PartnerListView() {
        super();
        // the msg is built in the contructor of EntityListView
        // so we cannot pas the title directly
        setTitle(UtilUi.MSG.partnerList());
    }

    /**
     * Constructor giving a title for this list view, which is displayed in the UI.
     * @param title the title of the list
     */
    public PartnerListView(String title) {
        super(title);
        init();
    }

    @Override
    public void init() {

        init(PartyLookupConfiguration.URL_FIND_PARTNERS, "/crmsfa/control/viewPartner?partyId={0}", UtilUi.MSG.partnerId(), new String[]{
                PartyLookupConfiguration.INOUT_GROUP_NAME, UtilUi.MSG.partnerName()
            });
    }

    /**
     * Filters the records of the list by name of the partner matching the given sub string.
     * @param partnerName a <code>String</code> value
     */
    public void filterByPartnerName(String partnerName) {
        setFilter(PartyLookupConfiguration.INOUT_GROUP_NAME, partnerName);
    }

}
