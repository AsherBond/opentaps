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
package org.opentaps.purchasing.domain.planning;

import java.util.List;

import javolution.util.FastList;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.opentaps.base.entities.Requirement;
import org.opentaps.base.entities.RequirementRole;
import org.opentaps.domain.purchasing.planning.PlanningRepositoryInterface;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.ofbiz.Repository;

/** {@inheritDoc} */
public class PlanningRepository extends Repository implements PlanningRepositoryInterface {

    /**
     * Default constructor.
     */
    public PlanningRepository() {
        super();
    }

     /**
     * If you want the full infrastructure including the dispatcher, then you must have the User.
     * @param infrastructure the domain infrastructure
     * @param userLogin the Ofbiz <code>UserLogin</code> generic value
     * @throws RepositoryException if an error occurs
     */
    public PlanningRepository(Infrastructure infrastructure, GenericValue userLogin) throws RepositoryException {
        super(infrastructure, userLogin);
    }

    /** {@inheritDoc} */
    public Requirement getRequirementById(String requirementId) throws RepositoryException, EntityNotFoundException {
        return findOneNotNull(Requirement.class, map(Requirement.Fields.requirementId, requirementId), "PurchRequirementNotExist", UtilMisc.toMap("requirementId", requirementId));
    }

    /** {@inheritDoc} */
    public List<RequirementRole> getSupplierRequirementRoles(Requirement requirement) throws RepositoryException {
        List<EntityCondition> conditions = new FastList<EntityCondition>();
        conditions.add(EntityCondition.makeCondition(RequirementRole.Fields.requirementId.name(), EntityOperator.EQUALS, requirement.getRequirementId()));
        conditions.add(EntityCondition.makeCondition(RequirementRole.Fields.roleTypeId.name(), EntityOperator.EQUALS, "SUPPLIER"));
        conditions.add(EntityUtil.getFilterByDateExpr());
        EntityCondition conditionList = EntityCondition.makeCondition(conditions, EntityOperator.AND);

        return findList(RequirementRole.class, conditionList);
    }

    /** {@inheritDoc} */
    public List<RequirementRole> getSupplierRequirementRoles(Requirement requirement, String supplierId) throws RepositoryException {
        List<EntityCondition> conditions = new FastList<EntityCondition>();
        conditions.add(EntityCondition.makeCondition(RequirementRole.Fields.requirementId.name(), EntityOperator.EQUALS, requirement.getRequirementId()));
        conditions.add(EntityCondition.makeCondition(RequirementRole.Fields.roleTypeId.name(), EntityOperator.EQUALS, "SUPPLIER"));
        conditions.add(EntityCondition.makeCondition(RequirementRole.Fields.partyId.name(), EntityOperator.EQUALS, supplierId));
        conditions.add(EntityUtil.getFilterByDateExpr());
        EntityCondition conditionList = EntityCondition.makeCondition(conditions, EntityOperator.AND);

        return findList(RequirementRole.class, conditionList);
    }
}

