/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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
