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

package com.opensourcestrategies.financials.invoice;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import javolution.util.FastList;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityFunction;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;

/**
 * InvoiceEvents - Java Servlet events for invoices.
 */
public final class InvoiceEvents {

    private InvoiceEvents() { }

    private static final String MODULE = InvoiceEvents.class.getName();

    @SuppressWarnings("unchecked")
    public static String invoiceOrderItems(HttpServletRequest request, HttpServletResponse response) {
        try {
            Collection orderData = UtilHttp.parseMultiFormData(UtilHttp.getParameterMap(request));
            LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
            HttpSession session = request.getSession();
            GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
            Locale locale = UtilHttp.getLocale(request);

                Map results = dispatcher.runSync("invoiceSuppliesOrWorkEffortOrderItems", UtilMisc.toMap("orderData", orderData, "userLogin", userLogin));
                if (UtilCommon.isSuccess(results)) {
                    return "success";
                } else {
                    return UtilMessage.createAndLogEventError(request, results, locale, MODULE);
                }
        } catch (GenericServiceException ex) {
            return ex.getMessage();
        }
    }

    /**
     * Since ofbiz doesn't support the combination of row submit with a list of items that get passed
     * into the string-list-suffix argument of a service, we have to do this.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the event response <code>String</code>
     */
    public static String createPartnerSalesInvoice(HttpServletRequest request, HttpServletResponse response) {
        try {
            TransactionUtil.begin();
            String results = createPartnerSalesInvoiceInternal(request, response);
            TransactionUtil.commit();
            return results;
        } catch (GenericTransactionException e) {
            return UtilMessage.createAndLogEventError(request, e, UtilHttp.getLocale(request), MODULE);
        }
    }

    @SuppressWarnings("unchecked")
    private static String createPartnerSalesInvoiceInternal(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        Locale locale = UtilHttp.getLocale(request);
        Map parameters = UtilHttp.getParameterMap(request);

        // get the selected invoiceIds
        List<String> invoiceIds = FastList.newInstance();
        Collection<Map<String, Object>> data = UtilHttp.parseMultiFormData(parameters);
        for (Map<String, Object> row : data) {
            invoiceIds.add((String) row.get("invoiceId"));
        }

        // call our service and put the invoiceId in the request
        Map input = UtilMisc.toMap("userLogin", userLogin);
        input.put("invoiceIds", invoiceIds);
        input.put("agreementId", parameters.get("agreementId"));
        input.put("organizationPartyId", parameters.get("organizationPartyId"));
        input.put("partnerPartyId", parameters.get("partnerPartyId"));
        try {
            Map results = dispatcher.runSync("opentaps.createPartnerSalesInvoice", input);
            if (ServiceUtil.isError(results)) {
                return UtilMessage.createAndLogEventError(request, results, locale, MODULE);
            }
            String invoiceId = (String) results.get("invoiceId");
            if (UtilValidate.isEmpty("invoiceId")) {
                UtilMessage.addError(request, "FinancialsError_NoPartnerSalesInvoiceCreated");
                return "error";
            }
            request.setAttribute("invoiceId", invoiceId);
            return "success";
        } catch (GeneralException e) {
            return UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        }
    }

    /**
     * A forceComplete event that checks if the referenceNumber already exists in the system for the invoice parties.
     * TODO: The forceComplete pattern is fairly common and can probably be wrapped in an object.  At the very least document this!
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the event response <code>String</code>
     */
    @SuppressWarnings("unchecked")
    public static String referenceNumberCheck(HttpServletRequest request, HttpServletResponse response) {
        boolean forceComplete = "true".equals(UtilCommon.getParameter(request, "forceComplete"));
        String oldRefNum = UtilCommon.getParameter(request, "oldRefNum");
        String referenceNumber = UtilCommon.getParameter(request, "referenceNumber");

        if (forceComplete) { // see InventoryEvents:116 for explanation
            request.setAttribute("forceComplete", "false");
            // user may have modified the reference number since last conflict
            if (referenceNumber == null || referenceNumber.equals(oldRefNum)) {
                return "success";
            }
        }
        Delegator delegator = (Delegator) request.getAttribute("delegator");

        try {
            // check if reference number is being set
            if (referenceNumber == null) {
                return "success";
            }
            request.setAttribute("oldRefNum", referenceNumber);

            // match the reference number
            List<EntityCondition> conditions = UtilMisc.<EntityCondition>toList(EntityCondition.makeCondition(EntityFunction.UPPER("referenceNumber"), EntityOperator.LIKE, EntityFunction.UPPER(referenceNumber)));

            String partyIdFrom = null;
            String partyId = null;
            String invoiceId = UtilCommon.getParameter(request, "invoiceId");
            if (invoiceId != null) {
                Map invoiceFind = UtilMisc.toMap("invoiceId", invoiceId);
                GenericValue invoice = delegator.findByPrimaryKey("Invoice", invoiceFind);
                if (invoice == null) {
                    return UtilMessage.createAndLogEventError(request, "FinancialsError_InvoiceNotFound", invoiceFind, UtilHttp.getLocale(request), MODULE);
                }
                // check if the reference number has not changed
                if (invoice.get("referenceNumber") != null && referenceNumber.equalsIgnoreCase(invoice.getString("referenceNumber"))) {
                    return "success";
                }
                partyIdFrom = invoice.getString("partyIdFrom");
                partyId = invoice.getString("partyId");
                // don't match self
                conditions.add(EntityCondition.makeCondition("invoiceId", EntityOperator.NOT_EQUAL, invoiceId));
            } else {
                partyIdFrom = UtilCommon.getParameter(request, "partyIdFrom");
                partyId = UtilCommon.getParameter(request, "partyId");
            }

            // party matching conditions
            conditions.add(EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, partyIdFrom));
            conditions.add(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId));

            List<GenericValue> matches = delegator.findByAnd("Invoice", conditions, UtilMisc.toList("invoiceDate DESC"));
            if (matches.size() > 0) {
                GenericValue match = matches.get(0);
                UtilMessage.addError(request, "FinancialsConfirmInvoiceRefNumber", UtilMisc.toMap("invoiceId", match.get("invoiceId")));
                request.setAttribute("forceComplete", "true");
                return "error";
            }
        } catch (GeneralException e) {
            return UtilMessage.createAndLogEventError(request, e, UtilHttp.getLocale(request), MODULE);
        }

        return "success";
    }

}
