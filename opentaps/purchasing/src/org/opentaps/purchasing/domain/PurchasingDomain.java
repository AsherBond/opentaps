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
package org.opentaps.purchasing.domain;

import org.opentaps.domain.purchasing.PurchasingDomainInterface;
import org.opentaps.domain.purchasing.PurchasingRepositoryInterface;
import org.opentaps.domain.purchasing.planning.PlanningRepositoryInterface;
import org.opentaps.domain.purchasing.planning.requirement.RequirementServiceInterface;
import org.opentaps.foundation.domain.Domain;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;
import org.opentaps.purchasing.domain.planning.PlanningRepository;
import org.opentaps.purchasing.domain.planning.requirement.RequirementService;

/**
 * This is an implementation of the Purchasing domain.
 */
public class PurchasingDomain extends Domain implements PurchasingDomainInterface {

    /** {@inheritDoc} */
    public PlanningRepositoryInterface getPlanningRepository() throws RepositoryException {
        return instantiateRepository(PlanningRepository.class);
    }

    /** {@inheritDoc} */
    public RequirementServiceInterface getRequirementService() throws ServiceException {
        return instantiateService(RequirementService.class);
    }

    /** {@inheritDoc} */
    public PurchasingRepositoryInterface getPurchasingRepository()
            throws RepositoryException {
        return instantiateRepository(PurchasingRepository.class);
    }

}
