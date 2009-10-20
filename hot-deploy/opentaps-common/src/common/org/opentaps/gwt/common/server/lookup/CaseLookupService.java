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
package org.opentaps.gwt.common.server.lookup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.opentaps.common.util.UtilView;
import org.opentaps.domain.base.entities.CustRequestAndPartyRelationshipAndRole;
import org.opentaps.foundation.entity.EntityInterface;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.gwt.common.client.lookup.configuration.CaseLookupConfiguration;
import org.opentaps.gwt.common.server.HttpInputProvider;
import org.opentaps.gwt.common.server.InputProviderInterface;
/**
 * The RPC service used to populate the CaseListview.
 */
public class CaseLookupService extends EntityLookupAndSuggestService {

    private static final String MODULE = CaseLookupService.class.getName();
    private static List<String> BY_ADVANCED_FILTERS = Arrays.asList(CaseLookupConfiguration.INOUT_PRIORITY,
            CaseLookupConfiguration.INOUT_STATUS_ID,
            CaseLookupConfiguration.INOUT_CUST_REQUEST_TYPE_ID,
            CaseLookupConfiguration.INOUT_CUST_REQUEST_NAME
        );
    /**
     * Creates a new <code>CaseLookupService</code> instance.
     * @param provider an <code>InputProviderInterface</code> value
     */
    public CaseLookupService(InputProviderInterface provider) {
        super(provider, CaseLookupConfiguration.LIST_OUT_FIELDS);
    }

    /**
     * AJAX event to perform lookups on Accounts.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the resulting JSON response
     * @throws InfrastructureException if an error occurs
     */
    public static String findCases(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        CaseLookupService service = new CaseLookupService(provider);
        service.findCases();
        return json.makeLookupResponse(CaseLookupConfiguration.INOUT_CUST_REQUEST_ID, service, request.getSession(true).getServletContext());
    }

    /**
     * Finds a list of <code>Account</code>.
     * @return the list of <code>Account</code>, or <code>null</code> if an error occurred
     */
    public List<CustRequestAndPartyRelationshipAndRole> findCases() {
        List<CustRequestAndPartyRelationshipAndRole> custRequests;
        EntityCondition prefCond = null;
        String partyId = null;
        String userLoginId = null;
        if (getProvider().getUser().getOfbizUserLogin() != null) {
            partyId = getProvider().getUser().getOfbizUserLogin().getString("partyId");
            userLoginId = getProvider().getUser().getOfbizUserLogin().getString("userLoginId");
        } else {
            Debug.logError("Current session do not have any UserLogin set.", MODULE);
        }
        EntityCondition takerCond = new EntityConditionList(UtilMisc.toList(
                new EntityExpr("partyId", EntityOperator.EQUALS, partyId),
                new EntityExpr("roleTypeId", EntityOperator.EQUALS, "REQ_TAKER")
                ), EntityOperator.AND);

        // or condition to find all cases for all accounts and contacts which the userLogin can view
        EntityCondition roleCond = new EntityConditionList(UtilMisc.toList(
                    new EntityExpr("roleTypeIdFrom", EntityOperator.EQUALS, "ACCOUNT"),
                    new EntityExpr("roleTypeIdFrom", EntityOperator.EQUALS, "CONTACT")
                    ), EntityOperator.OR);
        EntityCondition accountContactCond = new EntityConditionList(UtilMisc.toList(
                roleCond,
                new EntityExpr("partyIdTo", EntityOperator.EQUALS, partyId),
                EntityUtil.getFilterByDateExpr() // filter out expired accounts and contacts
                ), EntityOperator.AND);
        prefCond = accountContactCond;
        // select parties assigned to current user or his team according to view preferences.
        if (getProvider().parameterIsPresent(CaseLookupConfiguration.IN_RESPONSIBILTY)) {
                String viewPref = getProvider().getParameter(CaseLookupConfiguration.IN_RESPONSIBILTY);
                // condition to find all cases where userLogin is the request taker
                // decide which condition to use based on preferences (default is team)
                if (CaseLookupConfiguration.MY_VALUES.equals(viewPref)) {
                    prefCond = takerCond;
                }
        }
        EntityCondition condition = new EntityConditionList(
                UtilMisc.toList(
                    // exclude these case statuses
                    new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "CRQ_COMPLETED"),
                    new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "CRQ_REJECTED"),
                    new EntityExpr("statusId", EntityOperator.NOT_EQUAL, "CRQ_CANCELLED"),
                    // catalog requests should not be counted as cases
                    new EntityExpr("custRequestTypeId", EntityOperator.NOT_EQUAL, "RF_CATALOG"),
                    // the my or team preference condition
                    prefCond
                    ), EntityOperator.AND);
        if (getProvider().oneParameterIsPresent(BY_ADVANCED_FILTERS)) {
            custRequests = findCaseBy(CustRequestAndPartyRelationshipAndRole.class, condition, BY_ADVANCED_FILTERS);
        } else {
            custRequests = findAllCases(CustRequestAndPartyRelationshipAndRole.class, condition);
        }
        try {
            // make value for updated field
            for (CustRequestAndPartyRelationshipAndRole custRequestAndPartyRelationshipAndRole : custRequests) {
                boolean isUpdated = UtilView.isUpdatedSinceLastView(getProvider().getInfrastructure().getDelegator(), custRequestAndPartyRelationshipAndRole.getCustRequestId(), userLoginId);
                custRequestAndPartyRelationshipAndRole.setUpdated(isUpdated ? "Y" : "N");
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        return custRequests;
    }

    @Override
    public String makeSuggestDisplayedText(EntityInterface value) {
        StringBuffer sb = new StringBuffer();
        String custRequestName = value.getString("custRequestName");
        String custRequestId = value.getString("custRequestId");
        if (UtilValidate.isNotEmpty(custRequestName)) {
            sb.append(custRequestName);
        }
        sb.append(" (").append(custRequestId).append(")");

        return sb.toString();
    }

    private <T extends EntityInterface> List<T> findCaseBy(Class<T> entity, EntityCondition condition, List<String> filters) {
        List<EntityCondition> conds = new ArrayList<EntityCondition>();
        conds.add(condition);
        return findListWithFilters(entity, conds, filters);
    }

    private <T extends EntityInterface> List<T> findAllCases(Class<T> entity, EntityCondition condition) {
        List<EntityCondition> conds = new ArrayList<EntityCondition>();
        conds.add(condition);
        return findList(entity, new EntityConditionList(conds, EntityOperator.AND));
    }
}
