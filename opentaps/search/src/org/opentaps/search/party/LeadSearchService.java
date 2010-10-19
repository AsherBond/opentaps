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
package org.opentaps.search.party;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.base.constants.OpentapsConfigurationTypeConstants;
import org.opentaps.base.constants.RoleTypeConstants;
import org.opentaps.base.constants.SecurityPermissionConstants;
import org.opentaps.base.entities.PartyRelationshipAndPermission;
import org.opentaps.base.entities.PartyRole;
import org.opentaps.base.entities.PartyRolePk;
import org.opentaps.base.entities.UserLogin;
import org.opentaps.domain.party.Lead;
import org.opentaps.domain.party.PartyRepositoryInterface;
import org.opentaps.domain.search.SearchDomainInterface;
import org.opentaps.domain.search.SearchRepositoryInterface;
import org.opentaps.domain.search.SearchResult;
import org.opentaps.domain.search.party.LeadSearchServiceInterface;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.RepositoryInterface;
import org.opentaps.foundation.service.ServiceException;
import org.opentaps.search.HibernateSearchService;

/**
 * The implementation of the Lead search service.
 */
public class LeadSearchService extends HibernateSearchService implements LeadSearchServiceInterface {

    private static final String MODULE = LeadSearchService.class.getName();
    
    private List<Lead> leads = null;

    /** {@inheritDoc} */
    public List<Lead> getLeads() {
        return leads;
    }

    /** {@inheritDoc} */
    public String getQueryString() {
        StringBuilder sb = new StringBuilder();
        PartySearch.makeLeadQuery(sb);
        return sb.toString();
    }

    /** {@inheritDoc} */
    public Set<Class<?>> getClassesToQuery() {
        return PartySearch.PARTY_CLASSES;
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
 
            PartyRepositoryInterface partyRepository = getDomainsDirectory().getPartyDomain().getPartyRepository();
            // get the entities from the search results
            Set<String> leadIds = new HashSet<String>();
            for (SearchResult o : results) {
                Class<?> c = o.getResultClass();
                if (c.equals(PartyRole.class)) {
                    PartyRolePk pk = (PartyRolePk) o.getResultPk();
                    
                    if(RoleTypeConstants.PROSPECT.equals(pk.getRoleTypeId())){
                     
                        if("Y".equals(this.getInfrastructure().getConfigurationValue(OpentapsConfigurationTypeConstants.CRMSFA_FIND_SEC_FILTER))){                            
                            // Checking security if user has permisson to view lead.                            
                            if(this.hasUserSecurityPermissionToViewLead(partyRepository, pk.getPartyId()) == true){                                         
                                leadIds.add(pk.getPartyId());                               
                            }                            
                        }else{
                            leadIds.add(pk.getPartyId());
                        }
                        
                    }
                    
                }
            }

            // apply pagination here
            setResultSize(leadIds.size());
            if (!leadIds.isEmpty()) {
                if (!usePagination()) {
                    leads = partyRepository.findList(Lead.class, EntityCondition.makeCondition(Lead.Fields.partyId.name(), EntityOperator.IN, leadIds));
                } else if (getPageStart() < leadIds.size() && getPageSize() > 0) {
                    leads = partyRepository.findPage(Lead.class, EntityCondition.makeCondition(Lead.Fields.partyId.name(), EntityOperator.IN, leadIds), getPageStart(), getPageSize());
                } else {
                    leads = new ArrayList<Lead>();
                }
            } else {
                leads = new ArrayList<Lead>();
            }
            
        } catch (InfrastructureException e) {
            throw new ServiceException(e);
        } catch (RepositoryException e) {
            throw new ServiceException(e);
        }
    }
    
    private boolean hasUserSecurityPermissionToViewLead(RepositoryInterface repository, String leadPartyId) throws RepositoryException{
        boolean has = false;
        
        String userPartyId = this.getUser().getOfbizUserLogin().getString(UserLogin.Fields.partyId.name());
                            
        EntityCondition permissionCond = EntityCondition.makeCondition(EntityOperator.AND,
            EntityCondition.makeCondition(PartyRelationshipAndPermission.Fields.partyIdFrom.name(), EntityOperator.EQUALS, leadPartyId),
            EntityCondition.makeCondition(PartyRelationshipAndPermission.Fields.partyIdTo.name(), EntityOperator.EQUALS, userPartyId),
            EntityCondition.makeCondition(PartyRelationshipAndPermission.Fields.permissionId.name(), EntityOperator.EQUALS, SecurityPermissionConstants.CRMSFA_LEAD_VIEW),
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

            Debug.logInfo("User ["+userPartyId+"] has security permission to view lead ["+leadPartyId+"].", MODULE);

        }else{

            Debug.logInfo("User ["+userPartyId+"] has not passed security to view lead ["+leadPartyId+"].", MODULE);

        }

        return has;
    }
}
