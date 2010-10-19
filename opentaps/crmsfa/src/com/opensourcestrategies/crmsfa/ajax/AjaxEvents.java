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

package com.opensourcestrategies.crmsfa.ajax;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javolution.util.FastList;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.opentaps.common.order.OrderEvents;
import org.opentaps.common.party.PartyContactHelper;
import org.opentaps.common.product.UtilProduct;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilConfig;

import com.opensourcestrategies.crmsfa.party.PartyHelper;


/**
 * Ajax events to be invoked by the controller.
 *
 * @author Leon Torres (leon@opensourcestrategies.com)
 */
public class AjaxEvents {

    public static final String module = AjaxEvents.class.getName();

    public static String getCartShipEstimatesJSON(HttpServletRequest request, HttpServletResponse response) {

        String shipGroupSeqIdStr = (String) request.getParameter("shipGroupSeqId");
        int shipGroupSeqId = 0;
        try {
            shipGroupSeqId = Integer.parseInt(shipGroupSeqIdStr);
        } catch (NumberFormatException e) {
            Debug.logError("Error parsing shipGroupSeqId " + shipGroupSeqIdStr + " in getCartShipEstimatesJSON", module);
            return null;
        }

        List shipEstimates = OrderEvents.getCartShipEstimates(request, shipGroupSeqId);
        if (shipEstimates == null) {
            return "error";
        }
        return org.opentaps.common.event.AjaxEvents.doJSONResponse(response, shipEstimates);
    }

    /** Get the list of warnings of a product. */
    public static String getProductWarningsDataJSON(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");

        List warnings = new LinkedList();
        String productId = (String) request.getParameter("productId");
        try {
            warnings = UtilProduct.getProductWarnings(delegator, productId);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            return org.opentaps.common.event.AjaxEvents.doJSONResponse(response, FastList.newInstance());
        }
        return org.opentaps.common.event.AjaxEvents.doJSONResponse(response, warnings);
    }

    public static String assignCategoryToMergeFormJSON(HttpServletRequest request, HttpServletResponse response) throws GenericEntityException {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        String mergeFormId = (String) request.getParameter("mergeFormId");
        String mergeFormCategoryId = (String) request.getParameter("mergeFormCategoryId");

        GenericValue mergeForm = null;
        // Find the given MergeForm
        try {
            mergeForm = delegator.findByPrimaryKey("MergeForm", UtilMisc.toMap("mergeFormId", mergeFormId));
        } catch (GenericEntityException e) {
            Debug.logError(e, "MergeForm [" + mergeFormId + "] not found" , module);
            return org.opentaps.common.event.AjaxEvents.doJSONResponse(response, "");
        }

        // Associate the MergeForm to the given MergeFormCategory,
        //  if the category doesn't exist it throws an FK error
        //  if the entities are already associated it throws a PK error
        try {
            delegator.create("MergeFormToCategory", UtilMisc.toMap("mergeFormId", mergeFormId, "mergeFormCategoryId", mergeFormCategoryId));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Cannot associate MergeForm [" + mergeFormId + "] and MergeFormCategory [" + mergeFormCategoryId + "]" , module);
        }

        // Find associated categories
        List mergeFormCategories = mergeForm.getRelatedMulti("MergeFormToCategory", "MergeFormCategory");

        // Find the list of assignable categories
        List categories = delegator.findAll("MergeFormCategory");

        // Remove already associated categories from the previous list
        if (mergeFormCategories != null) {
            categories.removeAll(mergeFormCategories);
        }

        return org.opentaps.common.event.AjaxEvents.doJSONResponse(response, UtilMisc.toMap("mergeFormCategories", mergeFormCategories, "categories", categories));
    }

    public static String removeCategoryFromMergeFormJSON(HttpServletRequest request, HttpServletResponse response) throws GenericEntityException {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        String mergeFormId = (String) request.getParameter("mergeFormId");
        String mergeFormCategoryId = (String) request.getParameter("mergeFormCategoryId");

        GenericValue mergeForm = null;
        // Find the given MergeForm
        try {
            mergeForm = delegator.findByPrimaryKey("MergeForm", UtilMisc.toMap("mergeFormId", mergeFormId));
        } catch (GenericEntityException e) {
            Debug.logError(e, "MergeForm [" + mergeFormId + "] not found" , module);
            return org.opentaps.common.event.AjaxEvents.doJSONResponse(response, "");
        }

        // Find the association
        GenericValue mergeFormToCategory = null;
        try {
            mergeFormToCategory = delegator.findByPrimaryKey("MergeFormToCategory", UtilMisc.toMap("mergeFormId", mergeFormId, "mergeFormCategoryId", mergeFormCategoryId));
        } catch (GenericEntityException e) {
            Debug.logError(e, "No association between MergeForm [" + mergeFormId + "] and MergeFormCategory [" + mergeFormCategoryId + "]" , module);
        }

        // Remove the association if it was found
        if (mergeFormToCategory != null) {
            mergeFormToCategory.remove();
        }

        // Find associated categories
        List mergeFormCategories = mergeForm.getRelatedMulti("MergeFormToCategory", "MergeFormCategory");

        // Find the list of assignable categories
        List categories = delegator.findAll("MergeFormCategory");

        // Remove already associated categories from the previous list
        if (mergeFormCategories != null) {
            categories.removeAll(mergeFormCategories);
        }

        return org.opentaps.common.event.AjaxEvents.doJSONResponse(response, UtilMisc.toMap("mergeFormCategories", mergeFormCategories, "categories", categories));
    }

    /**
     * Loads the given email template and perform substitutions on the subject and body according to the email context (recipient, related order / shipment / ...).
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     * @exception GenericEntityException if an error occurs
     */
    public static String getMergedFormForEmailJSON(HttpServletRequest request, HttpServletResponse response) throws GenericEntityException {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        Locale locale = UtilMisc.ensureLocale(UtilHttp.getLocale(request));
        TimeZone timeZone = UtilCommon.getTimeZone(request);
        String mergeFormId = request.getParameter("mergeFormId");
        // should tags that are not substituted left verbatim in the result, else they are blanked
        boolean leaveTags = !("false".equalsIgnoreCase(request.getParameter("reportType")) || "N".equalsIgnoreCase(request.getParameter("reportType")));
        // should tags that are not substituted highlighted in the result
        boolean highlightTags = !("false".equalsIgnoreCase(request.getParameter("highlightTags")) || "N".equalsIgnoreCase(request.getParameter("highlightTags")));

        Map<String, String> returnMap = new HashMap<String, String>();
        GenericValue mergeForm = delegator.findByPrimaryKey("MergeForm", UtilMisc.toMap("mergeFormId", mergeFormId));
        if (UtilValidate.isNotEmpty(mergeForm)) {

            String partyId = null;
            String toEmail = request.getParameter("toEmail");
            if (UtilValidate.isNotEmpty(toEmail)) {
                toEmail = toEmail.trim();

                // Find the first party which matches one of the emails for the merge context
                List<String> partyIds = PartyContactHelper.getPartyIdsMatchingEmailsInString(delegator, toEmail, ",");
                if (UtilValidate.isNotEmpty(partyIds)) {
                    partyId = partyIds.get(0);
                }
            }

            String orderId = request.getParameter("orderId");
            String shipGroupSeqId = request.getParameter("shipGroupSeqId");
            String shipmentId = request.getParameter("shipmentId");
            Map<String, String> output = PartyHelper.mergePartyWithForm(delegator, mergeFormId, partyId, orderId, shipGroupSeqId, shipmentId, locale, leaveTags, timeZone, highlightTags);

            returnMap.put("mergeFormText", output.get("mergeFormText"));
            returnMap.put("subject", output.get("subject"));
        }
        return org.opentaps.common.event.AjaxEvents.doJSONResponse(response, returnMap);
    }

    /**
     * Create a Lead given the companyName, firstName, lastName and optionally a primaryEmail.
     * @param request
     * @param response
     * @return the partyId of the created Lead on success, else the string "error"
     * @throws GenericEntityException
     */
     @SuppressWarnings("unchecked")
    public static String createLead(HttpServletRequest request, HttpServletResponse response) throws GenericEntityException {
        GenericValue userLogin = UtilCommon.getUserLogin(request);
        if (userLogin == null) {
            return "error";
        }
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");

        String companyName = (String) request.getParameter("companyName");
        String firstName = (String) request.getParameter("firstName");
        String lastName = (String) request.getParameter("lastName");
        // optional field used when creating the Lead from the email list
        String primaryEmail = (String) request.getParameter("primaryEmail");

        // call the createLead service
        Map<String, Object> callCtxt = new HashMap<String, Object>();
        callCtxt.put("userLogin", userLogin);
        callCtxt.put("companyName", companyName);
        callCtxt.put("firstName", firstName);
        callCtxt.put("lastName", lastName);
        if (UtilValidate.isNotEmpty(primaryEmail)) {
            callCtxt.put("primaryEmail", primaryEmail);
        }

        Map<String, Object> callResults = null;

        try {
            callResults = dispatcher.runSync("crmsfa.createLead", callCtxt);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return "error";
        }

        if (callResults == null) {
            return "error";
        }

        return (String) callResults.get("partyId");
    }

    /**
     * Create a Contact given the firstName, lastName and optionally a primaryEmail.
     * @param request
     * @param response
     * @return the partyId of the created Lead on success, else the string "error"
     * @throws GenericEntityException
     */
    @SuppressWarnings("unchecked")
    public static String createContact(HttpServletRequest request, HttpServletResponse response) throws GenericEntityException {
        GenericValue userLogin = UtilCommon.getUserLogin(request);
        if (userLogin == null) {
            return "error";
        }
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");

        String firstName = (String) request.getParameter("firstName");
        String lastName = (String) request.getParameter("lastName");
        // optional field used when creating the Contact from the email list
        String primaryEmail = (String) request.getParameter("primaryEmail");

        // call the createContact service
        Map<String, Object> callCtxt = new HashMap<String, Object>();
        callCtxt.put("userLogin", userLogin);
        callCtxt.put("firstName", firstName);
        callCtxt.put("lastName", lastName);
        if (UtilValidate.isNotEmpty(primaryEmail)) {
            callCtxt.put("primaryEmail", primaryEmail);
        }

        Map<String, Object> callResults = null;

        try {
            callResults = dispatcher.runSync("crmsfa.createContact", callCtxt);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return "error";
        }

        if (callResults == null) {
            return "error";
        }

        return (String) callResults.get("partyId");
    }

    /**
     * Create an Account given the accountName and optionally a primaryEmail.
     * @param request
     * @param response
     * @return the partyId of the created Lead on success, else the string "error"
     * @throws GenericEntityException
     */
    @SuppressWarnings("unchecked")
    public static String createAccount(HttpServletRequest request, HttpServletResponse response) throws GenericEntityException {
        GenericValue userLogin = UtilCommon.getUserLogin(request);
        if (userLogin == null) {
            return "error";
        }
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");

        String accountName = (String) request.getParameter("accountName");
        // optional field used when creating the Account from the email list
        String primaryEmail = (String) request.getParameter("primaryEmail");

        // call the createAccount service
        Map<String, Object> callCtxt = new HashMap<String, Object>();
        callCtxt.put("userLogin", userLogin);
        callCtxt.put("accountName", accountName);
        if (UtilValidate.isNotEmpty(primaryEmail)) {
            callCtxt.put("primaryEmail", primaryEmail);
        }

        Map<String, Object> callResults = null;

        try {
            callResults = dispatcher.runSync("crmsfa.createAccount", callCtxt);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return "error";
        }

        if (callResults == null) {
            return "error";
        }

        return (String) callResults.get("partyId");
    }

    /**
     * Create a Partner given the partnerName and optionally a primaryEmail.
     * @param request
     * @param response
     * @return the partyId of the created Lead on success, else the string "error"
     * @throws GenericEntityException
     */
    @SuppressWarnings("unchecked")
    public static String createPartner(HttpServletRequest request, HttpServletResponse response) throws GenericEntityException {
        GenericValue userLogin = UtilCommon.getUserLogin(request);
        if (userLogin == null) {
            return "error";
        }
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");

        String partnerName = UtilCommon.getParameter(request, "partnerName");
        // optional field used when creating the Partner from the email list
        String primaryEmail = UtilCommon.getParameter(request, "primaryEmail");

        // call the createPartner service
        Map<String, Object> callCtxt = new HashMap<String, Object>();
        callCtxt.put("userLogin", userLogin);
        callCtxt.put("groupName", partnerName);
        // use the value from the configuration file which is the same behavior as when creating a partner from the Create Partner screen
        callCtxt.put("organizationPartyId", UtilConfig.getPropertyValue("opentaps", "organizationPartyId"));
        if (UtilValidate.isNotEmpty(primaryEmail)) {
            callCtxt.put("primaryEmail", primaryEmail);
        }

        Map<String, Object> callResults = null;

        try {
            callResults = dispatcher.runSync("crmsfa.createPartner", callCtxt);
        } catch (GenericServiceException e) {
            Debug.logError(e, module);
            return "error";
        }

        if (callResults == null) {
            return "error";
        }

        return (String) callResults.get("partyId");
    }

    /**
     * Check the given partyId correspond to a valid Account.
     * @param request
     * @param response
     * @throws GenericEntityException
     */
    public static String checkAccount(HttpServletRequest request, HttpServletResponse response) throws GenericEntityException {
        return checkPartyWithRole(request, "ACCOUNT");
    }

    private static String checkPartyWithRole(HttpServletRequest request, String roleTypeId) throws GenericEntityException {

        Delegator delegator = (Delegator) request.getAttribute("delegator");
        String partyId =  (String) UtilCommon.getParameter(request, "partyId");
        String createdParty = (String) UtilCommon.getParameter(request, "created");

        // special case, when we are redirected after account creation
        if (partyId == null && createdParty != null) {
            return "success";
        }

        String validRoleTypeId = PartyHelper.getFirstValidRoleTypeId(partyId, UtilMisc.toList(roleTypeId), delegator);
        if ((validRoleTypeId == null) || (!validRoleTypeId.equals(roleTypeId)))  {
            return "error";
        }
        return "success";
    }
}
