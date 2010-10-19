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

package org.opentaps.gwt.crmsfa.client.leads.form.configuration;

/**
 * Defines the interface between the server and client for the QuickNewLeadService
 * Technically not a java interface, but it defines all the constantes needed on both sides
 *  which makes the code more robust.
 */
public abstract class QuickNewLeadConfiguration {

    private QuickNewLeadConfiguration() { }

    public static final String URL = "/crmsfa/control/gwtQuickNewLead";

    public static final String IN_COMPANY_NAME = "companyName";
    public static final String IN_FIRST_NAME = "firstName";
    public static final String IN_LAST_NAME = "lastName";
    public static final String IN_PHONE_COUNTRY_CODE = "primaryPhoneCountryCode";
    public static final String IN_PHONE_AREA_CODE = "primaryPhoneAreaCode";
    public static final String IN_PHONE_NUMBER = "primaryPhoneNumber";
    public static final String IN_EMAIL_ADDRESS = "primaryEmail";

    public static final String OUT_LEAD_PARTY_ID = "partyId";

}
