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
package org.opentaps.gwt.crmsfa.client.opportunities.form.configuration;

/**
 * Configuration class for quick new opportunity.
 */
public abstract class QuickNewOpportunityConfiguration {
    public static final String URL = "/crmsfa/control/gwtQuickNewOpportunity";

    public static final String OPPORTUNITY_NAME = "opportunityName";
    public static final String ACCOUNT_OR_LEAD_PARTY_ID = "accountOrLeadPartyId";
    public static final String LEAD_PARTY_ID = "leadPartyId";
    public static final String OPPORTUNITY_STAGE_ID = "opportunityStageId";
    public static final String ESTIMATED_AMOUNT = "estimatedAmount";
    public static final String ESTIMATED_CLOSE_DATE = "estimatedCloseDate";

}
