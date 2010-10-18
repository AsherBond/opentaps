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
package org.opentaps.common.domain.security;

import java.util.List;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.opentaps.base.entities.PartyRelationshipAndPermission;
import org.opentaps.domain.DomainRepository;
import org.opentaps.domain.security.SecurityRepositoryInterface;
import org.opentaps.foundation.repository.RepositoryException;

public class SecurityRepository extends DomainRepository implements SecurityRepositoryInterface{

    public List<PartyRelationshipAndPermission> getPartyRelationshipAndPermission(String partyIdTo, String securityModule, String securityOperation) throws RepositoryException{       
        
        EntityCondition filterByDateCondition = EntityUtil.getFilterByDateExpr();
        EntityCondition operationConditon = EntityCondition.makeCondition(EntityOperator.OR,
                                EntityCondition.makeCondition("permissionId", EntityOperator.EQUALS, securityModule + "_MANAGER"),
                                EntityCondition.makeCondition("permissionId", EntityOperator.EQUALS, securityModule + securityOperation));
        EntityCondition searchConditions = EntityCondition.makeCondition(EntityOperator.AND,
                                EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, partyIdTo),
                                operationConditon,
                                filterByDateCondition);
        
        return this.findList(PartyRelationshipAndPermission.class, searchConditions);
            
    }

}
