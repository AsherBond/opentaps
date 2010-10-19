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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.opentaps.base.constants.RoleTypeConstants;
import org.opentaps.base.entities.PartyRelationship;
import org.opentaps.base.entities.SalesOpportunityAndPartyRelationshipAndStage;
import org.opentaps.common.party.PartyHelper;
import org.opentaps.common.util.ConvertMapToString;
import org.opentaps.common.util.ICompositeValue;
import org.opentaps.foundation.entity.EntityInterface;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.gwt.common.client.lookup.configuration.OpportunityLookupConfiguration;
import org.opentaps.gwt.common.server.HttpInputProvider;
import org.opentaps.gwt.common.server.InputProviderInterface;

/**
 * The RPC service used to populate the SalesOpportunityListview.
 */
public class OpportunityLookupService extends EntityLookupAndSuggestService {

    private static final String MODULE = OpportunityLookupService.class.getName();
    private static List<String> BY_ADVANCED_FILTERS = Arrays.asList(
            OpportunityLookupConfiguration.INOUT_OPPORTUNITY_NAME,
            OpportunityLookupConfiguration.INOUT_OPPORTUNITY_STAGE_ID,
            OpportunityLookupConfiguration.INOUT_TYPE_ENUM_ID,
            OpportunityLookupConfiguration.INOUT_PARTY_ID_FROM_ID,
            OpportunityLookupConfiguration.IN_ROLE_TYPE_FROM
    );

    /**
     * Creates a new <code>CaseLookupService</code> instance.
     * @param provider an <code>InputProviderInterface</code> value
     */
    public OpportunityLookupService(InputProviderInterface provider) {
        super(provider, OpportunityLookupConfiguration.LIST_OUT_FIELDS);
    }

    /**
     * AJAX event to perform lookups on Accounts.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the resulting JSON response
     * @throws InfrastructureException if an error occurs
     */
    public static String findOpportunities(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        OpportunityLookupService service = new OpportunityLookupService(provider);
        service.findOpportunities();
        return json.makeLookupResponse(OpportunityLookupConfiguration.INOUT_SALES_OPPORTUNITY_ID, service, request.getSession(true).getServletContext());
    }

    /**
     * Finds a list of <code>Account</code>.
     * @return the list of <code>Account</code>, or <code>null</code> if an error occurred
     */
    public List<SalesOpportunityAndPartyRelationshipAndStage> findOpportunities() {

        /**
         * Sales opportunity name formatter.<br>
         * Format opportunity name as <code>${opportunityName} (${salesOpportunityId})</code>.</br>
         * Field may be sorted by opportunity name.
         */
        class OpportunityNameAndIdSortable extends ConvertMapToString implements ICompositeValue {

            @Override
            public String convert(Map<String, ?> value) {
                return UtilValidate.isNotEmpty(value) ? String.format("%1$s (%2$s)", value.get("opportunityName"), value.get("salesOpportunityId")) : null;
            }

            public LinkedHashSet<String> getFields() {
                LinkedHashSet<String> sortableFields = new LinkedHashSet<String>(3);
                sortableFields.add(OpportunityLookupConfiguration.INOUT_OPPORTUNITY_NAME);
                return sortableFields;
            }
            
        }

        // keep rules for calculated fields
        Map<String, ConvertMapToString> calcField = FastMap.<String, ConvertMapToString>newInstance();
        calcField.put(OpportunityLookupConfiguration.INOUT_COMPOSITE_OPPORTUNITY_NAME, new OpportunityNameAndIdSortable());
        makeCalculatedField(calcField);

        List<SalesOpportunityAndPartyRelationshipAndStage> opportunities;
        String partyId = null;
        List<EntityCondition> combinedConditions = null;
        if (getProvider().getUser().getOfbizUserLogin() != null) {
            partyId = getProvider().getUser().getOfbizUserLogin().getString("partyId");
        } else {
            Debug.logError("Current session do not have any UserLogin set.", MODULE);
        }

        EntityCondition filterStageConditions = null;
        String findAll = getProvider().getParameter(OpportunityLookupConfiguration.IN_FIND_ALL);
        if (UtilValidate.isEmpty(findAll) || "N".equals(findAll)) {
            filterStageConditions = EntityCondition.makeCondition(EntityOperator.OR,
                EntityCondition.makeCondition("opportunityStageId", EntityOperator.EQUALS, null),
                EntityCondition.makeCondition(EntityOperator.AND,
                        EntityCondition.makeCondition("opportunityStageId", EntityOperator.NOT_EQUAL, null),
                        EntityCondition.makeCondition("opportunityStageId", EntityOperator.NOT_EQUAL, "SOSTG_CLOSED"),
                        EntityCondition.makeCondition("opportunityStageId", EntityOperator.NOT_EQUAL, "SOSTG_LOST"))
                );
        }
        // select parties assigned to current user or his team according to view preferences.
        if (getProvider().parameterIsPresent(OpportunityLookupConfiguration.IN_RESPONSIBILTY)) {
                String viewPref = getProvider().getParameter(OpportunityLookupConfiguration.IN_RESPONSIBILTY);
                // condition to find all cases where userLogin is the request taker
                // decide which condition to use based on preferences (default is team)
                if (OpportunityLookupConfiguration.MY_VALUES.equals(viewPref)) {
                    combinedConditions = UtilMisc.toList(
                            EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, partyId),
                            EntityCondition.makeCondition("partyRelationshipTypeId", EntityOperator.EQUALS, "RESPONSIBLE_FOR"),
                            EntityCondition.makeCondition(EntityOperator.OR,
                                    EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.EQUALS, "PROSPECT"),
                                    EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.EQUALS, "ACCOUNT")),
                            EntityUtil.getFilterByDateExpr()); // filter out expired accounts
                } else {
                    // strategy: find all the accounts of the internalPartyId, then find all the opportunities of those accounts
                    EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                                EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, partyId),
                                EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.IN, UtilMisc.toList("ACCOUNT", "PROSPECT")),
                                EntityCondition.makeCondition(EntityOperator.OR,
                                        EntityCondition.makeCondition("partyRelationshipTypeId", EntityOperator.EQUALS, "RESPONSIBLE_FOR"),
                                        EntityCondition.makeCondition("partyRelationshipTypeId", EntityOperator.EQUALS, "ASSIGNED_TO")),
                                EntityUtil.getFilterByDateExpr());
                    try {
                        List<PartyRelationship> accounts = getRepository().findList(PartyRelationship.class, conditions);
                        List<String> accountIds = new ArrayList<String>();
                        for (PartyRelationship account : accounts) {
                            accountIds.add(account.getPartyIdFrom());
                        }
                        // if no accounts are found, then return a null
                        if (accountIds.size() < 1) {
                            return null;
                        }
                        // build the condition to find opportunitied belonging to these accounts
                        combinedConditions = UtilMisc.<EntityCondition>toList(
                                EntityCondition.makeCondition("partyIdFrom", EntityOperator.IN, accountIds),
                                EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.IN, UtilMisc.toList("ACCOUNT", "PROSPECT")));
                    } catch (RepositoryException e) {
                        Debug.logError(e, MODULE);
                        return null;
                    }
                }
        } else {
            combinedConditions = UtilMisc.toList(
                    EntityCondition.makeCondition(EntityOperator.OR,
                            EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.EQUALS, RoleTypeConstants.PROSPECT),
                            EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.EQUALS, RoleTypeConstants.ACCOUNT),
                            EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.EQUALS, RoleTypeConstants.CONTACT)),
                    EntityUtil.getFilterByDateExpr()); // filter out expired accounts
        }

        // if additional conditions are passed in, add them as well
        if (filterStageConditions != null) {
            combinedConditions.add(filterStageConditions);
        }
        EntityCondition condition = EntityCondition.makeCondition(combinedConditions, EntityOperator.AND);
        if (getProvider().oneParameterIsPresent(BY_ADVANCED_FILTERS)) {
            opportunities = findOpportunitiesBy(SalesOpportunityAndPartyRelationshipAndStage.class, condition, BY_ADVANCED_FILTERS);
        } else {
            opportunities = findAllOpportunities(SalesOpportunityAndPartyRelationshipAndStage.class, condition);
        }

        // make custom field
        try {
            String externalLoginKey = getProvider().getParameter("externalLoginKey");
            for (SalesOpportunityAndPartyRelationshipAndStage opportunity : opportunities) {
                opportunity.setPartyFromLink(PartyHelper.createViewPageLink(opportunity.getPartyIdFrom(), getProvider().getInfrastructure().getDelegator(), externalLoginKey));
                // prepare date in localized format
                String estimatedCloseDateString = opportunity.getEstimatedCloseDate() == null ? "" : UtilDateTime.toDateString(opportunity.getEstimatedCloseDate());
                opportunity.setEstimatedCloseDateString(estimatedCloseDateString);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return null;
        }
        return opportunities;
    }


    @Override
    public String makeSuggestDisplayedText(EntityInterface value) {
        StringBuffer sb = new StringBuffer();
        String opportunityName = value.getString("opportunityName");
        String salesOpportunityId = value.getString("salesOpportunityId");
        if (UtilValidate.isNotEmpty(opportunityName)) {
            sb.append(opportunityName);
        }
        sb.append(" (").append(salesOpportunityId).append(")");

        return sb.toString();
    }

    private <T extends EntityInterface> List<T> findOpportunitiesBy(Class<T> entity, EntityCondition condition, List<String> filters) {
        List<EntityCondition> conds = new ArrayList<EntityCondition>();
        conds.add(condition);
        return findListWithFilters(entity, conds, filters);
    }

    private <T extends EntityInterface> List<T> findAllOpportunities(Class<T> entity, EntityCondition condition) {
        List<EntityCondition> conds = new ArrayList<EntityCondition>();
        conds.add(condition);
        return findList(entity, EntityCondition.makeCondition(conds, EntityOperator.AND));
    }
}
