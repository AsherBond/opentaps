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
package org.opentaps.domain.activities;

import java.util.List;

import org.opentaps.base.entities.ActivityFact;
import org.opentaps.foundation.repository.RepositoryInterface;

/**
 * Repository for Activities to handle interaction of Activities-related domain with 
 * the entity engine (database) and the service engine.
 */
public interface ActivityFactRepositoryInterface extends RepositoryInterface {
    
    /**
     * 
     * @param roleTypeId
     */
    public void setTargetRoleTypeId(String roleTypeId);
    
    /**
     * 
     * @param partyIds
     */
    public void setAllowedTargetPartyIds(List<String> partyIds);
    
    /**
     * 
     * @param dateDimId
     */
    public void setDateDimensionId(String dateDimId);
    
    /**
     * 
     * @param partyId
     */
    public void setTargetPartyId(String partyId);
    
    /**
     * 
     * @param roleTypeId
     */
    public void setTeamMemeberRoleTypeId(String memberRoleTypeId);
    
    /**
     * 
     * @return
     */
    public List<ActivityFact> findActivityFacts();
    
    /**
     * 
     * @return
     */
    public long getEmailActivityCount();
    /**
     * 
     * @return
     */
    public long getPhoneCallActivityCount();
    
    /**
     * 
     * @return
     */
    public long getVisitCallActivityCount();
    
    /**
     * 
     * @return
     */
    public long getOtherActivityCount();
    
    /**
     * 
     * @return
     */
    public long getTotalActivityCount();
}
