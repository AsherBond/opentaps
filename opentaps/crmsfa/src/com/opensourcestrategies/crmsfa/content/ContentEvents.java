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

package com.opensourcestrategies.crmsfa.content;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.ofbiz.base.util.Base64;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.content.data.DataResourceWorker;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.security.Security;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;

import com.opensourcestrategies.crmsfa.security.CrmsfaSecurity;

/**
 * Content servlet methods.
 *
 * @author Leon Torres (leon@opensourcestrategies.com)
 */
public final class ContentEvents {

    private ContentEvents() { }

    private static final String MODULE = ContentEvents.class.getName();

    /**
     * Download content for a party.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the <code>String</code> content value.
     */
    public static String downloadPartyContent(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        Security security = (Security) request.getAttribute("security");
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        Locale locale = UtilHttp.getLocale(request);
        String contentId = request.getParameter("contentId");
        String partyId = request.getParameter("partyId");

        try {
            GenericValue dataResource = getDataResource(request);
            if (dataResource == null) {
                return UtilMessage.createAndLogEventError(request, "CrmErrorContentNotFound", UtilMisc.toMap("contentId", contentId), locale, MODULE);
            }

            // get the module corresponding to the internal party
            String module = CrmsfaSecurity.getSecurityModuleOfInternalParty(partyId, delegator);
            if (module == null) {
                return UtilMessage.createAndLogEventError(request, "CrmErrorPermissionDenied", locale, MODULE);
            }

            // ensure association exists between our party and content (ignore role because we're using module to check for security)
            List<EntityCondition> conditions = UtilMisc.<EntityCondition>toList(
                    EntityCondition.makeCondition("contentId", contentId),
                    EntityCondition.makeCondition("partyId", partyId),
                    EntityUtil.getFilterByDateExpr());
            GenericValue association = EntityUtil.getFirst(delegator.findByAnd("ContentRole", conditions));
            if (association == null) {
                return UtilMessage.createAndLogEventError(request, "CrmErrorPermissionDenied", locale, MODULE);
            }

            // check if userLogin has permission to view
            if (!CrmsfaSecurity.hasPartyRelationSecurity(security, module, "_VIEW", userLogin, partyId)) {
                return UtilMessage.createAndLogEventError(request, "CrmErrorPermissionDenied", locale, MODULE);
            }

            return serveDataResource(request, response, dataResource);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        }
    }

    /**
     * Download content for a case.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the <code>String</code> content value.
     */
    public static String downloadCaseContent(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        Security security = (Security) request.getAttribute("security");
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        Locale locale = UtilHttp.getLocale(request);
        String contentId = request.getParameter("contentId");
        String custRequestId = request.getParameter("custRequestId");

        try {
            GenericValue dataResource = getDataResource(request);
            if (dataResource == null) {
                return UtilMessage.createAndLogEventError(request, "CrmErrorContentNotFound", UtilMisc.toMap("contentId", contentId), locale, MODULE);
            }

            // ensure association exists between case and content
            List<EntityCondition> conditions = UtilMisc.<EntityCondition>toList(
                    EntityCondition.makeCondition("contentId", contentId),
                    EntityCondition.makeCondition("custRequestId", custRequestId),
                    EntityUtil.getFilterByDateExpr());
            GenericValue association = EntityUtil.getFirst(delegator.findByAnd("CustRequestContent", conditions));
            if (association == null) {
                return UtilMessage.createAndLogEventError(request, "CrmErrorPermissionDenied", locale, MODULE);
            }

            // check if the userLogin has case view permission
            if (!CrmsfaSecurity.hasCasePermission(security, "_VIEW", userLogin, custRequestId)) {
                return UtilMessage.createAndLogEventError(request, "CrmErrorPermissionDenied", locale, MODULE);
            }

            return serveDataResource(request, response, dataResource);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        }
    }

    /**
     * Download content for an opportunity.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the <code>String</code> content value.
     */
    public static String downloadOpportunityContent(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        Security security = (Security) request.getAttribute("security");
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        Locale locale = UtilHttp.getLocale(request);
        String contentId = request.getParameter("contentId");
        String salesOpportunityId = request.getParameter("salesOpportunityId");

        try {
            GenericValue dataResource = getDataResource(request);
            if (dataResource == null) {
                return UtilMessage.createAndLogEventError(request, "CrmErrorContentNotFound", UtilMisc.toMap("contentId", contentId), locale, MODULE);
            }

            // ensure association exists between case and content
            List<EntityCondition> conditions = UtilMisc.<EntityCondition>toList(
                    EntityCondition.makeCondition("contentId", contentId),
                    EntityCondition.makeCondition("salesOpportunityId", salesOpportunityId),
                    EntityUtil.getFilterByDateExpr()
                    );
            GenericValue association = EntityUtil.getFirst(delegator.findByAnd("SalesOpportunityContent", conditions));
            if (association == null) {
                return UtilMessage.createAndLogEventError(request, "CrmErrorPermissionDenied", locale, MODULE);
            }

            // check if the userLogin has case view permission
            if (!CrmsfaSecurity.hasOpportunityPermission(security, "_VIEW", userLogin, salesOpportunityId)) {
                return UtilMessage.createAndLogEventError(request, "CrmErrorPermissionDenied", locale, MODULE);
            }

            return serveDataResource(request, response, dataResource);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        }
    }

    /**
     * Download content for an activity.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the <code>String</code> content value.
     */
    public static String downloadActivityContent(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        Security security = (Security) request.getAttribute("security");
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        Locale locale = UtilHttp.getLocale(request);
        String contentId = request.getParameter("contentId");
        String workEffortId = request.getParameter("workEffortId");

        try {
            GenericValue dataResource = getDataResource(request);
            if (dataResource == null) {
                return UtilMessage.createAndLogEventError(request, "CrmErrorContentNotFound", UtilMisc.toMap("contentId", contentId), locale, MODULE);
            }

            // ensure association exists between case and content
            List<EntityCondition> conditions = UtilMisc.<EntityCondition>toList(
                    EntityCondition.makeCondition("contentId", contentId),
                    EntityCondition.makeCondition("workEffortId", workEffortId),
                    EntityUtil.getFilterByDateExpr()
                    );
            GenericValue association = EntityUtil.getFirst(delegator.findByAnd("WorkEffortContent", conditions));
            if (association == null) {
                return UtilMessage.createAndLogEventError(request, "CrmErrorPermissionDenied", locale, MODULE);
            }

            // check if the userLogin has case view permission
            if (!CrmsfaSecurity.hasActivityPermission(security, "_VIEW", userLogin, workEffortId)) {
                return UtilMessage.createAndLogEventError(request, "CrmErrorPermissionDenied", locale, MODULE);
            }

            return serveDataResource(request, response, dataResource);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        }
    }


    /** Get the contentId and verify that we have something to download */
    private static GenericValue getDataResource(HttpServletRequest request) throws GenericEntityException {
        Delegator delegator = (Delegator) request.getAttribute("delegator");

        GenericValue content = delegator.findByPrimaryKey("Content", UtilMisc.toMap("contentId", request.getParameter("contentId")));
        if (content == null) {
            return null;
        }

        GenericValue dataResource = content.getRelatedOne("DataResource");
        if (dataResource == null || !"LOCAL_FILE".equals(dataResource.get("dataResourceTypeId"))) {
            return null;
        }
        return dataResource;
    }

    /** Find the file and write it to the client stream. */
    private static String serveDataResource(HttpServletRequest request, HttpServletResponse response, GenericValue dataResource) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        HttpSession session = request.getSession();
        Locale locale = UtilHttp.getLocale(request);
        ServletContext application = session.getServletContext();
        Map<String, Object> input = UtilMisc.<String, Object>toMap("contentId", request.getParameter("contentId"));
        try {
            String fileLocation = dataResource.getString("objectInfo");
            String fileName = dataResource.getString("dataResourceName");

            // the file name needs to be UTF8 urlencoded for the content disposition HTTP header
            fileName = "=?UTF-8?B?" + new String(Base64.base64Encode(fileName.getBytes("UTF-8")), "UTF-8") + "?=";

            if (UtilValidate.isEmpty(fileLocation)) {
                return UtilMessage.createAndLogEventError(request, "CrmErrorContentNotFound", input, locale, MODULE);
            }

            // test if the file exists here, due to strange bugs with DataResourceWorker.streamDataResource
            File file = new File(fileLocation);
            if (!file.exists()) {
                return UtilMessage.createAndLogEventError(request, "CrmErrorContentNotFound", input, locale, MODULE);
            }

            // Set the headers so that the browser treats content as a download (this could be changed to use the mimeTypeId of the content for in-browser display)
            response.setContentType("application/x-download");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

            // write the file to the client browser
            OutputStream os = response.getOutputStream();
            DataResourceWorker.streamDataResource(os, delegator, dataResource.getString("dataResourceId"), "", application.getInitParameter("webSiteId"), UtilHttp.getLocale(request), application.getRealPath("/"));
            os.flush();
            return "success";
        } catch (GeneralException e) {
            return UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        } catch (IOException e) {
            return UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        }
    }

    /**
     * Download content for an order.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the <code>String</code> content value.
     */
    public static String downloadOrderContent(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        Security security = (Security) request.getAttribute("security");
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        Locale locale = UtilHttp.getLocale(request);
        String contentId = request.getParameter("contentId");
        String orderId = request.getParameter("orderId");

        try {
            GenericValue dataResource = getDataResource(request);
            if (dataResource == null) {
                return UtilMessage.createAndLogEventError(request, "CrmErrorContentNotFound", UtilMisc.toMap("contentId", contentId), locale, MODULE);
            }

            // ensure association exists between case and content
            List<EntityCondition> conditions = UtilMisc.<EntityCondition>toList(
                    EntityCondition.makeCondition("contentId", contentId),
                    EntityCondition.makeCondition("orderId", orderId),
                    EntityUtil.getFilterByDateExpr());
            GenericValue association = EntityUtil.getFirst(delegator.findByAnd("OrderHeaderContent", conditions));
            if (association == null) {
                return UtilMessage.createAndLogEventError(request, "CrmErrorPermissionDenied", locale, MODULE);
            }

            // check if the userLogin has case view permission
            if (!CrmsfaSecurity.hasOrderPermission(security, "_VIEW", userLogin, orderId)) {
                return UtilMessage.createAndLogEventError(request, "CrmErrorPermissionDenied", locale, MODULE);
            }

            return serveDataResource(request, response, dataResource);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        }
    }

    /**
     * Downloads quote content.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     */
    public static String downloadQuoteContent(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        Security security = (Security) request.getAttribute("security");
        Locale locale = UtilHttp.getLocale(request);

        String contentId = UtilCommon.getParameter(request, "contentId");
        String quoteId = UtilCommon.getParameter(request, "quoteId");

        try {
            GenericValue dataResource = getDataResource(request);
            if (dataResource == null) {
                return UtilMessage.createAndLogEventError(request, "CrmErrorContentNotFound", UtilMisc.toMap("contentId", contentId), locale, MODULE);
            }

            // ensure association exists between quote and content
            List<EntityCondition> conditions = UtilMisc.toList(
                    EntityCondition.makeCondition("contentId", contentId),
                    EntityCondition.makeCondition("quoteId", quoteId),
                    EntityUtil.getFilterByDateExpr());
            GenericValue association = EntityUtil.getFirst(delegator.findByAnd("QuoteContent", conditions));
            if (association == null) {
                return UtilMessage.createAndLogEventError(request, "CrmErrorPermissionDenied", locale, MODULE);
            }

            // check if the userLogin has quote view permission
            if (!security.hasEntityPermission("CRMSFA_QUOTE", "_VIEW", session)) {
                return UtilMessage.createAndLogEventError(request, "CrmErrorPermissionDenied", locale, MODULE);
            }

            return serveDataResource(request, response, dataResource);

        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        }
    }

}
