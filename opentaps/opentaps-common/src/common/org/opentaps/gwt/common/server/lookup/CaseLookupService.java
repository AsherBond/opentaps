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
package org.opentaps.gwt.common.server.lookup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.opentaps.base.entities.CustRequestAndPartyRelationshipAndRole;
import org.opentaps.common.util.UtilView;
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
     * AJAX event to perform lookups on customer requests.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the resulting JSON response
     * @throws InfrastructureException if an error occurs
     */
    public static String findCases(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        CaseLookupService service = new CaseLookupService(provider);
        if (provider.parameterIsPresent(CaseLookupConfiguration.IN_PARTY_ID_FROM) && provider.parameterIsPresent(CaseLookupConfiguration.IN_ROLE_TYPE_FROM)) {
            service.findCasesForParty();
        } else {
            service.findCases();
        }
        return json.makeLookupResponse(CaseLookupConfiguration.INOUT_CUST_REQUEST_ID, service, request.getSession(true).getServletContext());
    }

    /**
     * Finds a list of cases.
     * @return the list of cases, or <code>null</code> if an error occurred
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

        EntityCondition takerCond = EntityCondition.makeCondition(EntityOperator.AND,
                EntityCondition.makeCondition("partyId", partyId),
                EntityCondition.makeCondition("roleTypeId", "REQ_TAKER"));

        // or condition to find all cases for all accounts and contacts which the userLogin can view
        EntityCondition roleCond = EntityCondition.makeCondition(EntityOperator.OR,
                EntityCondition.makeCondition("roleTypeIdFrom", "ACCOUNT"),
                EntityCondition.makeCondition("roleTypeIdFrom", "CONTACT"));

        EntityCondition accountContactCond = EntityCondition.makeCondition(EntityOperator.AND,
                roleCond,
                EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, partyId),
                EntityUtil.getFilterByDateExpr());

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
        EntityCondition condition = EntityCondition.makeCondition(EntityOperator.AND,
                // exclude these case statuses
                EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "CRQ_COMPLETED"),
                EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "CRQ_REJECTED"),
                EntityCondition.makeCondition("statusId", EntityOperator.NOT_EQUAL, "CRQ_CANCELLED"),
                // catalog requests should not be counted as cases
                EntityCondition.makeCondition("custRequestTypeId", EntityOperator.NOT_EQUAL, "RF_CATALOG"),
                // the my or team preference condition
                prefCond);
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

    /**
     * Finds a list of cases for particular contact or account.
     * @return the list of cases, or <code>null</code> if an error occurred
     */
    public List<CustRequestAndPartyRelationshipAndRole> findCasesForParty() {
        String userLoginId = null;
        if (getProvider().getUser().getOfbizUserLogin() != null) {
            userLoginId = getProvider().getUser().getOfbizUserLogin().getString("userLoginId");
        } else {
            Debug.logError("Current session do not have any UserLogin set.", MODULE);
        }

        EntityCondition conditions = EntityCondition.makeCondition(
                EntityCondition.makeCondition("statusId", EntityOperator.NOT_IN, Arrays.asList("CRQ_COMPLETED", "CRQ_REJECTED", "CRQ_CANCELLED")),
                EntityCondition.makeCondition("roleTypeIdFrom", getProvider().getParameter(CaseLookupConfiguration.IN_ROLE_TYPE_FROM)),
                EntityCondition.makeCondition("partyIdFrom", getProvider().getParameter(CaseLookupConfiguration.IN_PARTY_ID_FROM))
        );

        List<CustRequestAndPartyRelationshipAndRole> custRequests =
            findAllCases(CustRequestAndPartyRelationshipAndRole.class, conditions);

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
        return findList(entity, EntityCondition.makeCondition(conds, EntityOperator.AND));
    }
}
