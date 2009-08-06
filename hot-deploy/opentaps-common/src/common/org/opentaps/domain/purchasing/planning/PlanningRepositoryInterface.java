/*
 * Copyright (c) 2009 - 2009 Open Source Strategies, Inc.
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
package org.opentaps.domain.purchasing.planning;

import java.util.List;

import org.opentaps.domain.base.entities.Requirement;
import org.opentaps.domain.base.entities.RequirementRole;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.RepositoryInterface;

/**
 * Repository for Planning to handle interaction of Planning-related domain with the entity engine (database) and the service engine.
 */
public interface PlanningRepositoryInterface extends RepositoryInterface {

    /**
     * Finds the a <code>Requirement</code> by ID from the database.
     * @param requirementId the <code>Requirement</code> ID
     * @return the <code>Requirement</code> found
     * @throws RepositoryException if an error occurs
     * @throws EntityNotFoundException no <code>Requirement</code> is found for the given id
     */
    public Requirement getRequirementById(String requirementId) throws RepositoryException, EntityNotFoundException;

    /**
     * Finds the active Supplier <code>RequirementRole</code> for the given <code>Requirement</code>.
     * @param requirement the <code>Requirement</code>
     * @return the list of active Supplier <code>RequirementRole</code> found
     * @throws RepositoryException if an error occurs
     */
    public List<RequirementRole> getSupplierRequirementRoles(Requirement requirement) throws RepositoryException;

    /**
     * Finds the active Supplier <code>RequirementRole</code> for the given <code>Requirement</code> and matching the given supplier id.
     * @param requirement the <code>Requirement</code>
     * @param supplierId a supplier ID
     * @return the list of active Supplier <code>RequirementRole</code> found
     * @throws RepositoryException if an error occurs
     */
    public List<RequirementRole> getSupplierRequirementRoles(Requirement requirement, String supplierId) throws RepositoryException;

}
