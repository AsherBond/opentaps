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

import java.util.Arrays;
import java.util.List;

/**
 * Defines the interface between the server and client for the OpportunityLookupService
 * Technically not a java interface, but it defines all the constants needed on both sides
 *  which makes the code more robust.
 */
public abstract class OpportunityLookupConfiguration {

    private OpportunityLookupConfiguration() { }

    public static final String URL_FIND_OPPORTUNITIES = "gwtFindOpportunities";
    public static final String URL_SEARCH_OPPORTUNITIES = "gwtSearchSalesOpportunities";

    public static final String IN_RESPONSIBILTY = "MyOrTeamResponsibility";
    public static final String MY_VALUES = "MY_VALUES";
    public static final String TEAM_VALUES = "TEAM_VALUES";

    public static final String IN_FIND_ALL = "findAll";
    public static final String IN_ROLE_TYPE_FROM = "roleTypeIdFrom";

    public static final String INOUT_OPPORTUNITY_NAME = "opportunityName";
    public static final String INOUT_SALES_OPPORTUNITY_ID = "salesOpportunityId";
    public static final String INOUT_COMPOSITE_OPPORTUNITY_NAME = "opportunityNameAndId";
    public static final String INOUT_OPPORTUNITY_STAGE_ID = "opportunityStageId";
    public static final String INOUT_TYPE_ENUM_ID = "typeEnumId";
    public static final String INOUT_DESCRIPTION = "description";
    public static final String INOUT_PARTY_ID_FROM_ID = "partyIdFrom";
    public static final String OUT_ESTIMATED_AMOUNT = "estimatedAmount";
    public static final String OUT_PARTY_FROM_LINK = "partyFromLink";
    public static final String OUT_ESTIMATED_CLOSE_DATE = "estimatedCloseDate";
    public static final String OUT_ESTIMATED_CLOSE_DATE_STRING = "estimatedCloseDateString";
    public static final String OUT_ESTIMATED_PROBABILITY = "estimatedProbability";
    public static final String OUT_CURRENCY_UOM_ID = "currencyUomId";
    public static final String OUT_OPPORTUNITY_STAGE = "opportunityStage";

    public static final List<String> LIST_OUT_FIELDS = Arrays.asList(
            INOUT_OPPORTUNITY_NAME,
            INOUT_SALES_OPPORTUNITY_ID,
            INOUT_OPPORTUNITY_STAGE_ID,
            INOUT_TYPE_ENUM_ID,
            INOUT_DESCRIPTION,
            OUT_OPPORTUNITY_STAGE,
            INOUT_PARTY_ID_FROM_ID,
            OUT_ESTIMATED_AMOUNT,
            OUT_PARTY_FROM_LINK,
            OUT_ESTIMATED_CLOSE_DATE,
            OUT_ESTIMATED_CLOSE_DATE_STRING,
            OUT_ESTIMATED_PROBABILITY,
            OUT_CURRENCY_UOM_ID
    );

}
