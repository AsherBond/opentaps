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

package org.opentaps.gwt.crmsfa.accounts.client.form.configuration;

/**
 * Defines the interface between the server and client for the QuickNewAccountService
 * Technically not a java interface, but it defines all the constantes needed on both sides
 *  which makes the code more robust.
 */
public abstract class QuickNewAccountConfiguration {

    private QuickNewAccountConfiguration() { }

    public static final String URL = "/crmsfa/control/gwtQuickNewAccount";

    public static final String IN_ACCOUNT_NAME = "accountName";
    public static final String IN_PHONE_COUNTRY_CODE = "primaryPhoneCountryCode";
    public static final String IN_PHONE_AREA_CODE = "primaryPhoneAreaCode";
    public static final String IN_PHONE_NUMBER = "primaryPhoneNumber";
    public static final String IN_EMAIL_ADDRESS = "primaryEmail";

    public static final String OUT_ACCOUNT_PARTY_ID = "partyId";

}
