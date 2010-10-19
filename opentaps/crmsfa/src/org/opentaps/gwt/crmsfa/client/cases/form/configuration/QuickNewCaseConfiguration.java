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

package org.opentaps.gwt.crmsfa.client.cases.form.configuration;
/**
 * Configuration class for quick new case.
 */
public abstract class QuickNewCaseConfiguration {

    public static final String URL = "/crmsfa/control/gwtQuickNewCase";

    public static final String SUBJECT = "custRequestName";
    public static final String ACCOUNT_PARTY_ID = "accountPartyId";
    public static final String CASE_TYPE_ID = "custRequestTypeId";
    public static final String PRIORITY = "priority";

    // configure the default settings for crmsfa.createCase
    public static final String DEFAULT_CASE_TYPE_ID = "RF_SUPPORT";
    public static final String DEFAULT_PRIORITY = "5";
}
