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

import java.util.List;

import org.ofbiz.base.util.Debug;
import org.opentaps.base.entities.ActivityFact;
import org.opentaps.domain.activities.ActivityFactRepositoryInterface;
import org.opentaps.foundation.repository.ofbiz.Repository;

/** {@inheritDoc} */
public class ActivityFactRepository extends Repository implements ActivityFactRepositoryInterface {

    private static final String MODULE = ActivityFactRepository.class.getName();
    
    private String roleTypeId = null;
    private List<String> partyIds = null;
    private String dateDimId = null;
    private String partyId = null;
    private String memberRoleTypeId = null;
    private List<ActivityFact> listActivityFact = null;
    
    /**
     * Default constructor.
     */
    public ActivityFactRepository() {
        super();
    }

    /** {@inheritDoc} */
    public void setTargetRoleTypeId(String roleTypeId) {
        this.roleTypeId = roleTypeId;
    }
    
    /** {@inheritDoc} */
    public void setAllowedTargetPartyIds(List<String> partyIds) {
        this.partyIds = partyIds;
    }
    
    /** {@inheritDoc} */
    public void setDateDimensionId(String dateDimId) {
        this.dateDimId = dateDimId; 
    }
    
    /** {@inheritDoc} */
    public void setTargetPartyId(String partyId) {
        this.partyId = partyId;
    }
    
    /** {@inheritDoc} */
    public void setTeamMemeberRoleTypeId(String memberRoleTypeId) {
        this.memberRoleTypeId = memberRoleTypeId;
    }
    
    /** {@inheritDoc} */
    public List<ActivityFact> findActivityFacts() {
        listActivityFact = null;
        
        Debug.logInfo("Run  findActivityFacts", MODULE);
        
        return listActivityFact;
    }
    
    /** {@inheritDoc} */
    public long getEmailActivityCount() {
        long count = 0;
        
        return count;
    }
    
    /** {@inheritDoc} */
    public long getPhoneCallActivityCount() {
        long count = 0;

        return count;
    }
    
    /** {@inheritDoc} */
    public long getVisitCallActivityCount() {
        long count = 0;

        return count;
    }
    
    /** {@inheritDoc} */
    public long getOtherActivityCount() {
        long count = 0;
        
        return count;
    }
    
    /** {@inheritDoc} */
    public long getTotalActivityCount() {
        long count = 0;
        
        return count;
    }
}