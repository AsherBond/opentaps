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
 *  $Id:$
 *
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
package com.opensourcestrategies.crmsfa.cases;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.opensourcestrategies.crmsfa.security.CrmsfaSecurity;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.base.constants.RoleTypeConstants;
import org.opentaps.base.services.CreateCustRequestRoleService;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.service.ServiceException;

/**
 * Cases services. The service documentation is in services_cases.xml.
 *
 * @author     <a href="mailto:leon@opensourcestrategies.com">Leon Torres</a>
 * @version    $Rev: 488 $
 */
public final class CasesServices {

    private CasesServices() { }

    private static final String MODULE = CasesServices.class.getName();

    public static Map<String, Object> createCase(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        // verify that a partyId of some sort was supplied
        String accountPartyId = (String) context.get("accountPartyId");
        String contactPartyId = (String) context.get("contactPartyId");
        if (accountPartyId == null && contactPartyId == null) {
            return UtilMessage.createAndLogServiceError("CrmErrorCreateCaseFail", "CrmErrorCreateCaseFailNoAcctCont", locale, MODULE);
        }
        try {
            // create the cust request
            context.put("statusId", "CRQ_SUBMITTED");
            ModelService modelService = dctx.getModelService("createCustRequest");
            Map<String, Object> caseParams = modelService.makeValid(context, "IN");

            // CustRequest.fromPartyId is not used by the CRM/SFA application, which is designed to handle multiple parties
            // but we'll fill it for consistency with OFBiz and use contactPartyId first then accountPartyId
            if (contactPartyId != null) {
                caseParams.put("fromPartyId", contactPartyId);
            } else {
                caseParams.put("fromPartyId", accountPartyId);
            }

            Map<String, Object> serviceResults = dispatcher.runSync("createCustRequest", caseParams);
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorCreateCaseFail", locale, MODULE);
            }
            String custRequestId = (String) serviceResults.get("custRequestId");

            // create the account role if an account is supplied, but only if user has CRMSFA_CREATE_CASE permission on that account
            if (accountPartyId != null) {
                if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_CASE", "_CREATE", userLogin, accountPartyId)) {
                    return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
                }
                serviceResults = dispatcher.runSync("createCustRequestRole",
                        UtilMisc.toMap("custRequestId", custRequestId, "partyId", accountPartyId, "roleTypeId", "ACCOUNT", "userLogin", userLogin));
                if (ServiceUtil.isError(serviceResults)) {
                    return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorCreateCaseFail", locale, MODULE);
                }
            }

            // create the contact role if a contact is supplied, but only if user has CRMSFA_CASE_CREATE permission on that contact
            if (contactPartyId != null) {
                if (!CrmsfaSecurity.hasPartyRelationSecurity(security, "CRMSFA_CASE", "_CREATE", userLogin, contactPartyId)) {
                    return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
                }
                serviceResults = dispatcher.runSync("createCustRequestRole",
                        UtilMisc.toMap("custRequestId", custRequestId, "partyId", contactPartyId, "roleTypeId", "CONTACT", "userLogin", userLogin));
                if (ServiceUtil.isError(serviceResults)) {
                    return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorCreateCaseFail", locale, MODULE);
                }
            }

            /*
             * If the logged in partyId that is creating the request is not equal to the fromPartyId
             * then we associate it to the request as the request taker.
             * This is not done if they are the same e.g. a logged in customer that is creating a request for its
             * own sake.
             */
            String userPartyId = userLogin.getString("partyId");
            if (UtilValidate.isNotEmpty(userPartyId) && !userPartyId.equals(caseParams.get("fromPartyId"))) {
                CreateCustRequestRoleService createCustReqRoleSrvc = new CreateCustRequestRoleService(new User(userLogin));
                createCustReqRoleSrvc.setInCustRequestId(custRequestId);
                createCustReqRoleSrvc.setInPartyId(userPartyId);
                createCustReqRoleSrvc.setInRoleTypeId(RoleTypeConstants.REQ_TAKER);
                createCustReqRoleSrvc.runSync(new Infrastructure(dispatcher));
            }

            // create the note if a note is supplied
            String note = (String) context.get("note");
            if (note != null) {
                serviceResults = dispatcher.runSync("createCustRequestNote", UtilMisc.toMap("custRequestId", custRequestId, "note", note, "userLogin", userLogin));
                if (ServiceUtil.isError(serviceResults)) {
                    return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorCreateCaseFail", locale, MODULE);
                }
                String noteId = (String) serviceResults.get("noteId");

                // create a note association with the account and contact parties
                if (accountPartyId != null) {
                    serviceResults = dispatcher.runSync("createPartyNote", UtilMisc.toMap("partyId", accountPartyId, "noteId", noteId, "userLogin", userLogin));
                    if (ServiceUtil.isError(serviceResults)) {
                        return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorCreateCaseFail", locale, MODULE);
                    }
                }
                if (contactPartyId != null) {
                    serviceResults = dispatcher.runSync("createPartyNote", UtilMisc.toMap("partyId", contactPartyId, "noteId", noteId, "userLogin", userLogin));
                    if (ServiceUtil.isError(serviceResults)) {
                        return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorCreateCaseFail", locale, MODULE);
                    }
                }
            }

            // check if a WorkEffort Id was supplied, if so assign it to this Case
            String workEffortId = (String) context.get("workEffortId");
            if (workEffortId != null) {
                serviceResults = dispatcher.runSync("crmsfa.updateActivityAssociation", UtilMisc.toMap("workEffortId", workEffortId, "custRequestId", custRequestId, "userLogin", userLogin));
                if (ServiceUtil.isError(serviceResults)) {
                    return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorCreateCaseFail", locale, MODULE);
                }
            }

            // return the custRequestId
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("custRequestId", custRequestId);
            if (workEffortId != null) {
                result.put("workEffortId", workEffortId);
            }
            return result;
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorCreateCaseFail", locale, MODULE);
        } catch (ServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorCreateCaseFail", locale, MODULE);
        } catch (IllegalArgumentException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorCreateCaseFail", locale, MODULE);
        }
    }

    public static Map<String, Object> updateCase(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String custRequestId = (String) context.get("custRequestId");

        try {
            // first make sure userLogin has update permission
            if (!CrmsfaSecurity.hasCasePermission(security, "_UPDATE", userLogin, custRequestId)) {
                return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
            }

            // if the status is being set to close, check if user has permission to
            String statusId = (String) context.get("statusId");
            if (statusId.equals("CRQ_CANCELLED") || statusId.equals("CRQ_COMPLETED") || statusId.equals("CRQ_REJECTED")) {
                if (!CrmsfaSecurity.hasCasePermission(security, "_CLOSE", userLogin, custRequestId)) {
                    return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
                }
            }

            // update the cust request
            ModelService modelService = dctx.getModelService("updateCustRequest");
            Map<String, Object> serviceResults = dispatcher.runSync("updateCustRequest", modelService.makeValid(context, "IN"));
            if (ServiceUtil.isError(serviceResults)) {
                return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorUpdateCaseFail", locale, MODULE);
            }

            // create a note if a note is supplied
            String note = (String) context.get("note");
            if (note != null) {
                serviceResults = dispatcher.runSync("createCustRequestNote", UtilMisc.toMap("custRequestId", custRequestId, "note", note, "userLogin", userLogin));
                if (ServiceUtil.isError(serviceResults)) {
                    return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorUpdateCaseFail", locale, MODULE);
                }
                String noteId = (String) serviceResults.get("noteId");

                // associate this note with each case account and contact
                List<GenericValue> parties = UtilCase.getCaseAccountsAndContacts(delegator, custRequestId);
                for (GenericValue party : parties) {
                    serviceResults = dispatcher.runSync("createPartyNote", UtilMisc.toMap("partyId", party.get("partyId"), "noteId", noteId, "userLogin", userLogin));
                    if (ServiceUtil.isError(serviceResults)) {
                        return UtilMessage.createAndLogServiceError(serviceResults, "CrmErrorUpdateCaseFail", locale, MODULE);
                    }
                }
            }
            return ServiceUtil.returnSuccess();
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorUpdateCaseFail", locale, MODULE);
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorUpdateCaseFail", locale, MODULE);
        }
    }

    public static Map<String, Object> closeCase(DispatchContext dctx, Map<String, Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        String custRequestId = (String) context.get("custRequestId");

        try {
            // check if user has permission to close
            if (!CrmsfaSecurity.hasCasePermission(security, "_CLOSE", userLogin, custRequestId)) {
                return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
            }
            // close by setting status to CRQ_COMPLETED
            Map<String, Object> serviceResults = dispatcher.runSync("updateCustRequest",
                    UtilMisc.toMap("custRequestId", custRequestId, "statusId", "CRQ_COMPLETED", "userLogin", userLogin));
            if (ServiceUtil.isError(serviceResults)) {
                return serviceResults;
            }
            return ServiceUtil.returnSuccess();
        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorUpdateCaseFail", locale, MODULE);
        }
    }
}
