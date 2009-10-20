/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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
package org.opentaps.gwt.common.client.lookup.configuration;

import java.util.Arrays;
import java.util.List;

/**
 * Defines the interface between the server and client for the CaseLookupService
 * Technically not a java interface, but it defines all the constants needed on both sides
 *  which makes the code more robust.
 */
public abstract class CaseLookupConfiguration {

    private CaseLookupConfiguration() { }

    public static final String URL_FIND_CASES = "gwtFindCases";

    public static final String IN_RESPONSIBILTY = "MyOrTeamResponsibility";
    public static final String MY_VALUES = "MY_VALUES";
    public static final String TEAM_VALUES = "TEAM_VALUES";

    public static final String INOUT_CUST_REQUEST_ID = "custRequestId";
    public static final String INOUT_PRIORITY = "priority";
    public static final String INOUT_STATUS_ID = "statusId";
    public static final String INOUT_CUST_REQUEST_TYPE_ID = "custRequestTypeId";
    public static final String INOUT_CUST_REQUEST_NAME = "custRequestName";
    public static final String OUT_STATUS = "status";
    public static final String OUT_CUST_REQUEST_TYPE = "custRequestType";
    public static final String OUT_REASON = "reason";
    public static final String OUT_UPDATED = "updated";

    public static final List<String> LIST_OUT_FIELDS = Arrays.asList(
        INOUT_PRIORITY,
        INOUT_CUST_REQUEST_ID,
        INOUT_STATUS_ID,
        INOUT_CUST_REQUEST_TYPE_ID,
        INOUT_CUST_REQUEST_NAME,
        OUT_STATUS,
        OUT_CUST_REQUEST_TYPE,
        OUT_REASON,
        OUT_UPDATED
    );

}
