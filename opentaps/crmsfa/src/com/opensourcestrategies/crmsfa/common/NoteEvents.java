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

package com.opensourcestrategies.crmsfa.common;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.opensourcestrategies.crmsfa.security.CrmsfaSecurity;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.security.Security;
import org.ofbiz.service.LocalDispatcher;
import org.opentaps.common.event.AjaxEvents;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.foundation.infrastructure.Infrastructure;

/**
 * Ajax events to edit or delete notes.
 * @author Fabien Carrion (gkfabs@opensourcestrategies.com)
 */
public final class NoteEvents {

    private NoteEvents() { }

    private static final String MODULE = NoteEvents.class.getName();

    /**
     * Saves a note.
     *
     * This event accepts the following parameters:<ul>
     *  <li><b>noteId</b>: the ID of the note</li>
     *  <li><b>text</b>: the content of the note</li>
     *  <li>one of <b>partyId</b> <b>custRequestId</b>: for a party note or a case note respectively</li>
     *  <li><b>modulePermission</b>: the module in which the event is called (eg: CRMSFA_LEAD, CRMSFA_CONTACT, ...), used to check base permissions</li>
     *  <li><b>counter</b>: an index value that will also be included in the response (to identify the element)</li>
     * </ul>
     *
     * The response contains:<ul>
     *  <li><b>text</b>: the content of note, or an error message</li>
     *  <li><b>counter</b>: same as the input value (to identify the element), or <code>-1</code> if the reponse is an error</li>
     * </ul>
     *
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     */
    public static String saveNotesJSON(HttpServletRequest request, HttpServletResponse response) {
        Locale locale = UtilMisc.ensureLocale(UtilHttp.getLocale(request));
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Infrastructure infrastructure = new Infrastructure(dispatcher);
        Security security = (Security) request.getAttribute("security");
        HttpSession session = request.getSession();
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");

        String counter = request.getParameter("counter");
        String noteId = request.getParameter("noteId");
        String text = request.getParameter("text");
        // a note may be for a Party or a Case
        String partyId = request.getParameter("partyId");
        String custRequestId = request.getParameter("custRequestId");

        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("counter", "-1");
        resp.put("text", "Error");

        try {
            // get the note entity
            GenericValue note = delegator.findByPrimaryKey("NoteData", UtilMisc.toMap("noteId", noteId));

            if (!CrmsfaSecurity.hasNotePermission(security, request.getParameter("modulePermission"), "_UPDATE", userLogin, note, partyId, custRequestId)) {
                resp.put("text", UtilMessage.expandLabel("CrmErrorPermissionDenied", locale));
                return AjaxEvents.doJSONResponse(response, resp);
            }

            // update the note
            note.setString("noteInfo", text);
            note.store();
        } catch (GeneralException e) {
            resp.put("text", UtilMessage.expandLabel("OpentapsError_EditNoteFail", locale));
            return AjaxEvents.doJSONResponse(response, resp);
        }

        resp.put("counter", counter);
        resp.put("text", text);

        return AjaxEvents.doJSONResponse(response, resp);
    }

    /**
     * Deletes a note.
     *
     * This event accepts the following parameters:<ul>
     *  <li><b>noteId</b>: the ID of the note</li>
     *  <li>one of <b>partyId</b> <b>custRequestId</b>: for a party note or a case note respectively</li>
     *  <li><b>modulePermission</b>: the module in which the event is called (eg: CRMSFA_LEAD, CRMSFA_CONTACT, ...), used to check base permissions</li>
     *  <li><b>counter</b>: an index value that will also be included in the response (to identify the element)</li>
     * </ul>
     *
     * The response contains:<ul>
     *  <li><b>text</b>: the content of note, or an error message</li>
     *  <li><b>counter</b>: same as the input value (to identify the element), or <code>-1</code> if the reponse is an error</li>
     * </ul>
     *
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     */
    public static String deleteNotesJSON(HttpServletRequest request, HttpServletResponse response) {
        Locale locale = UtilMisc.ensureLocale(UtilHttp.getLocale(request));
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Infrastructure infrastructure = new Infrastructure(dispatcher);
        Security security = (Security) request.getAttribute("security");
        HttpSession session = request.getSession();
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");

        String counter = request.getParameter("counter");
        String noteId = request.getParameter("noteId");
        // a note may be for a Party or a Case
        String partyId = request.getParameter("partyId");
        String custRequestId = request.getParameter("custRequestId");

        Map<String, Object> resp = new HashMap<String, Object>();
        resp.put("counter", "-1");
        resp.put("text", "Error");

        // to find and delete the relation entity
        Map<String, String> conditions;
        String relationEntityName;

        try {
            // get the note entity
            GenericValue note = delegator.findByPrimaryKey("NoteData", UtilMisc.toMap("noteId", noteId));

            if (!CrmsfaSecurity.hasNotePermission(security, request.getParameter("modulePermission"), "_DELETE", userLogin, note, partyId, custRequestId)) {
                resp.put("text", UtilMessage.expandLabel("CrmErrorPermissionDenied", locale));
                return AjaxEvents.doJSONResponse(response, resp);
            }

            if (UtilValidate.isNotEmpty(partyId)) {
                conditions = UtilMisc.toMap("noteId", noteId, "partyId", partyId);
                relationEntityName = "PartyNote";
            } else if (UtilValidate.isNotEmpty(custRequestId)) {
                conditions = UtilMisc.toMap("noteId", noteId, "custRequestId", custRequestId);
                relationEntityName = "CustRequestNote";
            } else {
                resp.put("text", UtilMessage.expandLabel("OpentapsError_DeleteNoteFail", locale));
                return AjaxEvents.doJSONResponse(response, resp);
            }

            // delete the relation entity
            GenericValue relationEntity = delegator.findByPrimaryKey(relationEntityName, conditions);
            relationEntity.remove();

            // delete the note
            note.remove();

        } catch (GeneralException e) {
            resp.put("text", UtilMessage.expandLabel("OpentapsError_DeleteNoteFail", locale));
            return AjaxEvents.doJSONResponse(response, resp);
        }

        resp.put("counter", counter);

        return AjaxEvents.doJSONResponse(response, resp);
    }
}
