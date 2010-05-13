/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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

package com.opensourcestrategies.crmsfa.common;

import com.opensourcestrategies.crmsfa.security.CrmsfaSecurity;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.security.Security;

import org.opentaps.common.event.AjaxEvents;
import org.opentaps.common.util.UtilMessage;

import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author Fabien Carrion (gkfabs@opensourcestrategies.com)
 */
public class NoteEvents {

    public static final String module = NoteEvents.class.getName();

    private static String getPermission(String modulePermission) {
        if ("CRMSFA_LEADS".equals(modulePermission)) {
            return "CRMSFA_LEAD";
        } else if ("CRMSFA_ACCOUNT".equals(modulePermission)) {
            return "CRMSFA_ACCOUNT";
        } else if ("CRMSFA_CONTACT".equals(modulePermission)) {
            return "CRMSFA_CONTACT";
        }

        return "";
    }

    public static String saveNotesJSON(HttpServletRequest request, HttpServletResponse response) {
        Locale locale = UtilMisc.ensureLocale( UtilHttp.getLocale(request));
        GenericDelegator delegator = (GenericDelegator)request.getAttribute("delegator");
        Security security = (Security) request.getAttribute("security");
        HttpSession session = request.getSession();
        GenericValue userLogin = (GenericValue)session.getAttribute("userLogin");

        String counter = (String) request.getParameter("counter");
        String noteId = (String) request.getParameter("noteId");
        String text = (String) request.getParameter("text");
        // a note may be for a Party or a Case
        String partyId = (String) request.getParameter("partyId");
        String custRequestId = (String) request.getParameter("custRequestId");

        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("counter", "-1");
        resp.put("text", "Error");

        if (UtilValidate.isNotEmpty(partyId)) {
            // make sure userLogin has permission (module, operation) for this party
            String modulePermission = getPermission((String) request.getParameter("modulePermission"));
            if (!CrmsfaSecurity.hasPartyRelationSecurity(security, modulePermission, "_UPDATE", userLogin, partyId)) {
                resp.put("text", UtilMessage.expandLabel("CrmErrorPermissionDenied", locale));
                return AjaxEvents.doJSONResponse(response, resp);
            }
        } else if (UtilValidate.isNotEmpty(custRequestId)) {
            // make sure userLogin has permission (module, operation) for this case
            if (!CrmsfaSecurity.hasCasePermission(security, "_UPDATE", userLogin, custRequestId)) {
                resp.put("text", UtilMessage.expandLabel("CrmErrorPermissionDenied", locale));
                return AjaxEvents.doJSONResponse(response, resp);
            }
        } else {
            // this error should never happen except due to programming error on the webpage
            resp.put("text", UtilMessage.expandLabel("OpentapsError_EditNoteFail", locale));
            return AjaxEvents.doJSONResponse(response, resp);
        }

        try {
            // get the note entity
            GenericValue note = delegator.findByPrimaryKey("NoteData", UtilMisc.toMap("noteId", noteId));
            note.setString("noteInfo", text);
            note.store();
        } catch (GenericEntityException e) {
            resp.put("text", UtilMessage.expandLabel("OpentapsError_EditNoteFail", locale));
            return AjaxEvents.doJSONResponse(response, resp);
        }

        resp.put("counter", counter);
        resp.put("text", text);

        return AjaxEvents.doJSONResponse(response, resp);
    }

    public static String deleteNotesJSON(HttpServletRequest request, HttpServletResponse response) {
        Locale locale = UtilMisc.ensureLocale( UtilHttp.getLocale(request));
        GenericDelegator delegator = (GenericDelegator)request.getAttribute("delegator");
        Security security = (Security) request.getAttribute("security");
        HttpSession session = request.getSession();
        GenericValue userLogin = (GenericValue)session.getAttribute("userLogin");

        String counter = (String) request.getParameter("counter");
        String noteId = (String) request.getParameter("noteId");
        // a note may be for a Party or a Case
        String partyId = (String) request.getParameter("partyId");
        String custRequestId = (String) request.getParameter("custRequestId");

        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("counter", "-1");
        resp.put("text", "Error");

        // to find and deleted the relation entity
        Map conditions;
        String relationEntityName;

        if (UtilValidate.isNotEmpty(partyId)) {
            // make sure userLogin has permission (module, operation) for this party
            String modulePermission = getPermission((String) request.getParameter("modulePermission"));
            if (!CrmsfaSecurity.hasPartyRelationSecurity(security, modulePermission, "_UPDATE", userLogin, partyId)) {
                resp.put("text", UtilMessage.expandLabel("CrmErrorPermissionDenied", locale));
                return AjaxEvents.doJSONResponse(response, resp);
            }
            // set conditions
            conditions = UtilMisc.toMap("noteId", noteId, "partyId", partyId);
            relationEntityName = "PartyNote";
        } else if (UtilValidate.isNotEmpty(custRequestId)) {
            // make sure userLogin has permission (module, operation) for this case
            if (!CrmsfaSecurity.hasCasePermission(security, "_UPDATE", userLogin, custRequestId)) {
                resp.put("text", UtilMessage.expandLabel("CrmErrorPermissionDenied", locale));
                return AjaxEvents.doJSONResponse(response, resp);
            }
            // set conditions
            conditions = UtilMisc.toMap("noteId", noteId, "custRequestId", custRequestId);
            relationEntityName = "CustRequestNote";
        } else {
            // this error should never happen except due to programming error on the webpage
            resp.put("text", UtilMessage.expandLabel("OpentapsError_DeleteNoteFail", locale));
            return AjaxEvents.doJSONResponse(response, resp);
        }

        try {
            // delete the relation entity
            GenericValue relationEntity = delegator.findByPrimaryKey(relationEntityName, conditions);
            relationEntity.remove();
            
            // delete the note entity
            GenericValue note = delegator.findByPrimaryKey("NoteData", UtilMisc.toMap("noteId", noteId));
            note.remove();
        } catch (GenericEntityException e) {
            resp.put("text", UtilMessage.expandLabel("OpentapsError_DeleteNoteFail", locale));
            return AjaxEvents.doJSONResponse(response, resp);
        }

        resp.put("counter", counter);

        return AjaxEvents.doJSONResponse(response, resp);
    }
}
