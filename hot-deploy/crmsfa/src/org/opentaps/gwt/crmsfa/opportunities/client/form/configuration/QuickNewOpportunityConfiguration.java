/*
 * Copyright (c) 2009 Open Source Strategies, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 *
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
 */
package org.opentaps.gwt.crmsfa.opportunities.client.form.configuration;

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
