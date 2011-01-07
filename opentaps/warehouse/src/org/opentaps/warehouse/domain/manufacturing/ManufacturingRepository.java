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
package org.opentaps.warehouse.domain.manufacturing;

import java.math.BigDecimal;

import org.ofbiz.entity.Delegator;
import org.opentaps.base.services.GetProductionRunCostService;
import org.opentaps.domain.manufacturing.ManufacturingRepositoryInterface;
import org.opentaps.domain.manufacturing.ProductionRun;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.ofbiz.Repository;
import org.opentaps.foundation.service.ServiceException;

/** {@inheritDoc} */
public class ManufacturingRepository extends Repository implements ManufacturingRepositoryInterface {

    /**
     * Default constructor.
     */
    public ManufacturingRepository() {
        super();
    }

    /**
     * Use this for Repositories which will only access the database via the delegator.
     * @param delegator the delegator
     */
    public ManufacturingRepository(Delegator delegator) {
        super(delegator);
    }

    /**
     * Use this for domain Repositories.
     * @param infrastructure the domain infrastructure
     * @param user the domain user
     * @throws RepositoryException if an error occurs
     */
    public ManufacturingRepository(Infrastructure infrastructure, User user) throws RepositoryException {
        super(infrastructure, user);
    }

    /** {@inheritDoc} */
    public ProductionRun getProductionRun(String workEffortId) throws RepositoryException, EntityNotFoundException {
        ProductionRun productionRun = findOneNotNull(ProductionRun.class, map(ProductionRun.Fields.workEffortId, workEffortId), "Production Run [" + workEffortId + "] not found.");
        // TODO: should this accept Production run tasks as well ?
        if (!"PROD_ORDER_HEADER".equals(productionRun.getWorkEffortTypeId())) {
            throw new EntityNotFoundException(ProductionRun.class, "Production Run [" + workEffortId + "] not found (a Work effort that is not a Production Run but of type " + productionRun.getWorkEffortTypeId() + " was found for the given ID).");
        }
        return productionRun;
    }

    /** {@inheritDoc} */
    public BigDecimal getProductionRunCost(ProductionRun productionRun) throws RepositoryException {
        try {
            GetProductionRunCostService service = new GetProductionRunCostService(getUser());
            service.setInWorkEffortId(productionRun.getWorkEffortId());
            service.runSync(getInfrastructure());
            if (service.isError()) {
                throw new RepositoryException(service.getErrorMessage());
            }
            return service.getOutTotalCost();
        } catch (ServiceException e) {
            throw new RepositoryException(e);
        }
    }

}
