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
package org.opentaps.domain.manufacturing;

import java.math.BigDecimal;

import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.RepositoryInterface;

/**
 * Repository for Manufacturing to handle interaction of Manufacturing-related domain with the entity engine (database) and the service engine.
 */
public interface ManufacturingRepositoryInterface extends RepositoryInterface {

    /**
     * Finds a <code>ProductionRun</code> by ID from the database.
     * @param workEffortId the work effort ID for the <code>ProductionRun</code>
     * @return a <code>ProductionRun</code> value
     * @throws RepositoryException if an error occurs
     * @throws EntityNotFoundException if no <code>ProductionRun</code> is found, or if the entity found for the given ID is not a Production Run Header
     */
    public ProductionRun getProductionRun(String workEffortId) throws RepositoryException, EntityNotFoundException;

    /**
     * Finds the production run cost for the given <code>ProductionRun</code>.
     * @param productionRun the <code>ProductionRun</code>
     * @return the total cost of the production run
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal getProductionRunCost(ProductionRun productionRun) throws RepositoryException;

}
