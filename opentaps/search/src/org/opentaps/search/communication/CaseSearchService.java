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
package org.opentaps.search.communication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.base.constants.OpentapsConfigurationTypeConstants;
import org.opentaps.base.constants.SecurityPermissionConstants;
import org.opentaps.base.entities.CustRequest;
import org.opentaps.base.entities.CustRequestRole;
import org.opentaps.base.entities.CustRequestRolePk;
import org.opentaps.base.entities.PartyRelationshipAndPermission;
import org.opentaps.base.entities.UserLogin;
import org.opentaps.domain.order.OrderRepositoryInterface;
import org.opentaps.domain.search.SearchDomainInterface;
import org.opentaps.domain.search.SearchRepositoryInterface;
import org.opentaps.domain.search.SearchResult;
import org.opentaps.domain.search.communication.CaseSearchServiceInterface;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;
import org.opentaps.search.HibernateSearchRepository;
import org.opentaps.search.HibernateSearchService;
import org.opentaps.search.party.PartySearch;

/**
 * The implementation of the Case search service.
 */
public class CaseSearchService extends HibernateSearchService implements CaseSearchServiceInterface {
    
    private static final String MODULE = CaseSearchService.class.getName();

    /** Common set of class to query for Case related search. */
    public static final Set<Class<?>> CASE_CLASSES = new HashSet<Class<?>>(Arrays.asList(
            CustRequestRole.class
        ));

    private List<CustRequest> cases = new ArrayList<CustRequest>();

    /** {@inheritDoc} */
    public List<CustRequest> getCases() {
        return cases;
    }

    /** {@inheritDoc} */
    public String getQueryString() {
        StringBuilder sb = new StringBuilder();
        // filter canceled (lost) Sales Opportunities
        sb.append("(+(");
        makeCaseQuery(sb);
        PartySearch.makePartyGroupFieldsQuery(sb);
        PartySearch.makePersonFieldsQuery(sb);
        sb.append("))");
        return sb.toString();
    }

    /**
     * Builds the Lucene query for a Case related search.
     * @param sb the string builder instance currently building the query
     */
    public static void makeCaseQuery(StringBuilder sb) {
        for (String f : Arrays.asList(CustRequest.Fields.custRequestId.name(), CustRequest.Fields.custRequestName.name(), CustRequest.Fields.description.name())) {
            sb.append("custRequest.").append(f).append(":").append(HibernateSearchRepository.DEFAULT_PLACEHOLDER).append(" ");
        }
    }

    /** {@inheritDoc} */
    public Set<Class<?>> getClassesToQuery() {
        return CASE_CLASSES;
    }

    /** {@inheritDoc} */
    public void search() throws ServiceException {
        try {
            SearchDomainInterface searchDomain = getDomainsDirectory().getSearchDomain();
            SearchRepositoryInterface searchRepository = searchDomain.getSearchRepository();
            search(searchRepository);
        } catch (RepositoryException e) {
            throw new ServiceException(e);
        }
    }

    /** {@inheritDoc} */
    public void readSearchResults(List<SearchResult> results) throws ServiceException {

        try {
            OrderRepositoryInterface repository = getDomainsDirectory().getOrderDomain().getOrderRepository();
            // get the entities from the search results
            Set<String> caseIds = new HashSet<String>();
            for (SearchResult o : results) {
                Class<?> c = o.getResultClass();
                if (c.equals(CustRequestRole.class)) {
                    CustRequestRolePk pk = (CustRequestRolePk) o.getResultPk();
                    
                    if("Y".equals(this.getInfrastructure().getConfigurationValue(OpentapsConfigurationTypeConstants.CRMSFA_FIND_SEC_FILTER))){                            
                        // Checking security if user has permisson to view case.                            
                        if(this.hasUserSecurityPermissionToViewCase(repository, pk.getPartyId()) == true){                                         
                            caseIds.add(pk.getCustRequestId());                               
                        }                            
                    }else{
                        caseIds.add(pk.getCustRequestId());
                    }
                }
            }

            // apply pagination here
            setResultSize(caseIds.size());
            if (!caseIds.isEmpty()) {
                if (!usePagination()) {
                    cases = repository.findList(CustRequest.class, EntityCondition.makeCondition(CustRequest.Fields.custRequestId.name(), EntityOperator.IN, caseIds));
                } else if (getPageStart() < caseIds.size() && getPageSize() > 0) {
                    cases = repository.findPage(CustRequest.class, EntityCondition.makeCondition(CustRequest.Fields.custRequestId.name(), EntityOperator.IN, caseIds), getPageStart(), getPageSize());
                } else {
                    cases = new ArrayList<CustRequest>();
                }
            } else {
                cases = new ArrayList<CustRequest>();
            }
        } catch (InfrastructureException e) {
            throw new ServiceException(e);
        } catch (RepositoryException e) {
            throw new ServiceException(e);
        }
    }

    private boolean hasUserSecurityPermissionToViewCase(OrderRepositoryInterface repository, String casePartyId) throws RepositoryException {
        boolean has = false;
        
        String userPartyId = this.getUser().getOfbizUserLogin().getString(UserLogin.Fields.partyId.name());
                            
        EntityCondition permissionCond = EntityCondition.makeCondition(EntityOperator.AND,
            EntityCondition.makeCondition(PartyRelationshipAndPermission.Fields.partyIdFrom.name(), EntityOperator.EQUALS, userPartyId),
            EntityCondition.makeCondition(PartyRelationshipAndPermission.Fields.partyIdTo.name(), EntityOperator.EQUALS, casePartyId),
            EntityCondition.makeCondition(PartyRelationshipAndPermission.Fields.permissionId.name(), EntityOperator.EQUALS, SecurityPermissionConstants.CRMSFA_CASE_VIEW),
            EntityCondition.makeCondition(EntityOperator.OR,
                    EntityCondition.makeCondition(PartyRelationshipAndPermission.Fields.fromDate.name(), EntityOperator.EQUALS, null),
                    EntityCondition.makeCondition(PartyRelationshipAndPermission.Fields.fromDate.name(), EntityOperator.LESS_THAN, UtilDateTime.nowTimestamp())
                    ),
            EntityCondition.makeCondition(EntityOperator.OR,
                    EntityCondition.makeCondition(PartyRelationshipAndPermission.Fields.thruDate.name(), EntityOperator.EQUALS, null),
                    EntityCondition.makeCondition(PartyRelationshipAndPermission.Fields.thruDate.name(), EntityOperator.GREATER_THAN, UtilDateTime.nowTimestamp())
                    )
        );

        List<PartyRelationshipAndPermission> permission = repository.findList(PartyRelationshipAndPermission.class, permissionCond);

        if(permission.size() > 0){         

            has = true;

            Debug.logInfo("User ["+userPartyId+"] has security permission to view case of party ["+casePartyId+"].", MODULE);

        }else{

            Debug.logInfo("User ["+userPartyId+"] has not passed security to view case of party ["+casePartyId+"].", MODULE);

        }

        return has;
    }
}
