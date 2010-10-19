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
/* Copyright (c) Open Source Strategies, Inc. */

/*
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.opensourcestrategies.crmsfa.common;

import java.util.Map;
import java.util.Locale;

import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.security.Security;

import com.opensourcestrategies.crmsfa.security.CrmsfaSecurity;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;

/**
 * Note services. The service documentation is in services_notes.xml.
 *
 * @author     <a href="mailto:leon@opensourcestrategies.com">Leon Torres</a>
 * @version    $Rev: 314 $
 */
public final class NoteServices {

    private NoteServices() { }

    private static final String MODULE = NoteServices.class.getName();

    /**
     * Creates an Account note, check permissions first.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map createAccountNote(DispatchContext dctx, Map context) {
        return createNoteWithPermission(dctx, context, "CRMSFA_ACCOUNT", "_UPDATE");
    }

    /**
     * Creates a Contact note, check permissions first.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map createContactNote(DispatchContext dctx, Map context) {
        return createNoteWithPermission(dctx, context, "CRMSFA_CONTACT", "_UPDATE");
    }

    /**
     * Creates a Lead note, check permissions first.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map createLeadNote(DispatchContext dctx, Map context) {
        return createNoteWithPermission(dctx, context, "CRMSFA_LEAD", "_UPDATE");
    }

    /**
     * Creates a Partner note, check permissions first.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map createPartnerNote(DispatchContext dctx, Map context) {
        return createNoteWithPermission(dctx, context, "CRMSFA_PARTNER", "_UPDATE");
    }

    /**
     * Common createNote method that does everything necessary for note creation.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @param module - Check that user has permission in this module
     * @param operation - Check that user has permission to perform this action
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    private static Map createNoteWithPermission(DispatchContext dctx, Map context, String module, String operation) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        // what party this note is for
        String partyId = (String) context.get("partyId");

        // make sure userLogin has permission (module, operation) for this party
        if (!CrmsfaSecurity.hasPartyRelationSecurity(security, module, operation, userLogin, partyId)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
        }

        // create the note
        try {
            Map serviceResult = dispatcher.runSync("createPartyNote", context);
            if (ServiceUtil.isError(serviceResult)) {
                return UtilMessage.createAndLogServiceError(serviceResult, "CrmErrorCreateNoteFail", locale, MODULE);
            }
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorCreateNoteFail", locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    /**
     * Creates a Case note, check permissions first.
     * @param dctx a <code>DispatchContext</code> value
     * @param context a <code>Map</code> value
     * @return a <code>Map</code> value
     */
    @SuppressWarnings("unchecked")
    public static Map createCaseNote(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        // what CustRequest this note is for
        String custRequestId = (String) context.get("custRequestId");

        // make sure userLogin has permission for this case
        if (!CrmsfaSecurity.hasCasePermission(security, "_UPDATE", userLogin, custRequestId)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
        }

        // create the note
        try {
            Map serviceResult = dispatcher.runSync("createCustRequestNote", context);
            if (ServiceUtil.isError(serviceResult)) {
                return UtilMessage.createAndLogServiceError(serviceResult, "CrmErrorCreateNoteFail", locale, MODULE);
            }
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorCreateNoteFail", locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }
}
