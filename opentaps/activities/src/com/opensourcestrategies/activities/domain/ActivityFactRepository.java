/*
 * Copyright (c) 2010 Open Source Strategies, Inc.
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
package com.opensourcestrategies.activities.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.base.entities.ActivityFact;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.domain.activities.ActivityFactRepositoryInterface;
import org.opentaps.foundation.entity.Entity;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.ofbiz.Repository;

/** {@inheritDoc} */
public class ActivityFactRepository extends Repository implements ActivityFactRepositoryInterface {

    private static final String MODULE = ActivityFactRepository.class.getName();

    private String targetRoleTypeId = null;
    private List<String> allowedTargetPartyIds = null;
    private long dateDimId = 0;
    private String teamMemberPartyId = null;
    private String targetPartyId = null;
    private String teamMemberRoleTypeId = null;

    private List<ActivityFact> listActivityFact = null;

    /**
     * Default constructor.
     */
    public ActivityFactRepository() {
        super();
    }

    /** {@inheritDoc} */
    public void setTargetRoleTypeId(String roleTypeId) {
        targetRoleTypeId = roleTypeId;
    }

    /** {@inheritDoc} */
    public void setAllowedTargetPartyIds(List<String> partyIds) {
        allowedTargetPartyIds = partyIds;
    }

    /** {@inheritDoc} */
    public void setDateDimensionId(long dateDimId) {
        this.dateDimId = dateDimId; 
    }

    /** {@inheritDoc} */
    public void setTeamMemberPartyId(String partyId) {
        teamMemberPartyId = partyId; 
    }

    /** {@inheritDoc} */
    public void setTargetPartyId(String partyId) {
        targetPartyId = partyId;
    }

    /** {@inheritDoc} */
    public void setTeamMemeberRoleTypeId(String memberRoleTypeId) {
        this.teamMemberRoleTypeId = memberRoleTypeId;
    }

    /** {@inheritDoc} */
    public List<ActivityFact> findActivityFacts() throws RepositoryException {
        DomainsLoader domainLoader = new DomainsLoader(getInfrastructure(), getUser());

        EntityCondition condition = EntityCondition.makeCondition(
                EntityCondition.makeCondition(ActivityFact.Fields.targetPartyId.name(), targetPartyId),
                EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyId.name(), teamMemberPartyId),
                EntityCondition.makeCondition(ActivityFact.Fields.targetPartyRoleTypeId.name(), targetRoleTypeId),
                EntityCondition.makeCondition(ActivityFact.Fields.teamMemberPartyRoleTypeId.name(), teamMemberRoleTypeId),
                EntityCondition.makeCondition(ActivityFact.Fields.dateDimId.name(), dateDimId));
        listActivityFact = domainLoader.getDomainsDirectory().getPartyDomain().getPartyRepository().findList(ActivityFact.class, condition);

        return listActivityFact;
    }

    /** {@inheritDoc} */
    public Map<String, List<ActivityFact>> findLeadsActivitiesGroupedBy(ActivityFact.Fields groupedByField) throws RepositoryException {
        List<ActivityFact> prospectActivityFacts = null;
        DomainsLoader domainLoader = new DomainsLoader(getInfrastructure(), getUser());

        EntityCondition condition = EntityCondition.makeCondition(
                ActivityFact.Fields.targetPartyRoleTypeId.name(), targetRoleTypeId);
        if (allowedTargetPartyIds != null) {
            condition = EntityCondition.makeCondition(condition,
                    EntityCondition.makeCondition(ActivityFact.Fields.targetPartyId.name(), EntityOperator.IN, allowedTargetPartyIds));
        }

        prospectActivityFacts = domainLoader.getDomainsDirectory().getPartyDomain().getPartyRepository().findList(
                ActivityFact.class, condition, Arrays.asList(ActivityFact.Fields.targetPartyId.name(), 
                    ActivityFact.Fields.dateDimId.name()), Arrays.asList(ActivityFact.Fields.targetPartyId.asc(),
                            ActivityFact.Fields.dateDimId.desc()));

        return Entity.groupByFieldValues(String.class, prospectActivityFacts, groupedByField);
    }

    /** {@inheritDoc} */
    public long getEmailActivityCount() throws RepositoryException {
        long count = 0;

        if(listActivityFact == null) {
            findActivityFacts();
        }

        if(listActivityFact.size() > 0) {
            count = listActivityFact.get(0).getEmailActivityCount();
        }

        return count;
    }

    /** {@inheritDoc} */
    public long getPhoneCallActivityCount() throws RepositoryException {
        long count = 0;

        if(listActivityFact == null) {
            findActivityFacts();
        }

        if(listActivityFact.size() > 0) {
            count = listActivityFact.get(0).getPhoneCallActivityCount();
        }

        return count;
    }

    /** {@inheritDoc} */
    public long getVisitActivityCount() throws RepositoryException {
        long count = 0;

        if(listActivityFact == null) {
            findActivityFacts();
        }

        if(listActivityFact.size() > 0) {
            count = listActivityFact.get(0).getVisitActivityCount();
        }

        return count;
    }

    /** {@inheritDoc} */
    public long getOtherActivityCount() throws RepositoryException {
        long count = 0;

        if(listActivityFact == null) {
            findActivityFacts();
        }

        if(listActivityFact.size() > 0) {
            count = listActivityFact.get(0).getOtherActivityCount();
        }

        return count;
    }

    /** {@inheritDoc} */
    public long getTotalActivityCount() throws RepositoryException {
        long count = 0;

        if(listActivityFact == null) {
            findActivityFacts();
        }

        if(listActivityFact.size() > 0){
            count += listActivityFact.get(0).getEmailActivityCount()+
                     listActivityFact.get(0).getPhoneCallActivityCount()+
                     listActivityFact.get(0).getVisitActivityCount()+
                     listActivityFact.get(0).getOtherActivityCount();
        }

        return count;
    }
}