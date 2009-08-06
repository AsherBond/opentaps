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

package org.opentaps.gwt.crmsfa.cases.client.form.configuration;

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
