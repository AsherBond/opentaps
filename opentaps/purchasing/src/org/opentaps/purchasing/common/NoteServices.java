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

package org.opentaps.purchasing.common;

import java.util.Map;
import java.util.Locale;

import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.security.Security;
import org.ofbiz.base.util.Debug;

import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.purchasing.security.PurchasingSecurity;

/**
 * Note services. The service documentation is in services.xml.
 *
 * @author     <a href="mailto:asser.professional@yahoo.com">Asser Hassanain</a>
 * @version    $Rev: 3783 $
 */

public class NoteServices {

    public static final String module = NoteServices.class.getName();

    public static Map createSupplierNote(DispatchContext dctx, Map context) {
        return createNoteWithPermission(dctx, context, "PRCH_SPLR", "_UPDATE");
    }

    /* TODO: This should become a general purpose, "application-agnostic", security-checking */
    /* service that can be used to create notes (and perhaps other entity types) from any    */
    /* opentaps application, perhaps using the general OpentapsSecurity object (although we  */
    /* have to make sure that it is safe to use its hasPartyRelationSecurity routine).       */
    /*                                                                                       */
    /* The idea is that we have a service that accepts the same parameters as the            */
    /* createPartyNote service, but with two additional parameters: module and operation,    */
    /* which indicate the type of security we want to check.  Then application-specific      */
    /* note-creation services can be built upon this service directly as a(n almost)         */
    /* one-liner service definition.  This will also be modular, as the bulk of the logic    */
    /* will be in this service.                                                              */

    /**
     * Common createNote method that does everything necessary for note creation.
     * @param   dctx - Dispatch context for service
     * @param   context - Specific parameters and environment for this service call
     * @param   module - Check that user has permission in this module
     * @param   operation - Check that user has permission to perform this action
     */
    private static Map createNoteWithPermission(DispatchContext dctx, Map context, String module, String operation) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        PurchasingSecurity purchasingSecurity = new PurchasingSecurity(security, userLogin);
        String organizationId = (String)dctx.getAttribute("organizationId");

        // Is the above statement the right way to get a session attribute?
        Debug.logInfo("######## organizationId = " + organizationId, module);

        // what party this note is for
        String partyId = (String) context.get("partyId");

        // make sure userLogin has permission (module, operation) for this party
        if (!purchasingSecurity.hasPartyRelationSecurity(module, operation, partyId)) {
            return UtilMessage.createAndLogServiceError("OpentapsError_PermissionDenied", locale, module);
        }

        // create the note
        try {
            Map serviceResult = dispatcher.runSync("createPartyNote", context);
            if (ServiceUtil.isError(serviceResult)) {
                return UtilMessage.createAndLogServiceError(serviceResult, "OpentapsError_CreateNoteFail", locale, module);
            }
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "OpentapsError_CreateNoteFail", locale, module);
        }
        return ServiceUtil.returnSuccess();
    }
}
