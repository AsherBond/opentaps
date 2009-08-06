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
package org.opentaps.domain.purchasing;

import org.opentaps.domain.purchasing.planning.PlanningRepositoryInterface;
import org.opentaps.domain.purchasing.planning.requirement.RequirementServiceInterface;
import org.opentaps.foundation.domain.DomainInterface;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;

/**
 * This is the interface of the Shipping domain.
 */
public interface PurchasingDomainInterface extends DomainInterface {

    /**
     * Returns the planning repository instance.
     * @return a <code>PlanningRepositoryInterface</code> value
     * @throws RepositoryException if an error occurs
     */
    public PlanningRepositoryInterface getPlanningRepository() throws RepositoryException;

    /**
     * Returns the requirement service instance.
     * @return an <code>RequirementServiceInterface</code> value
     * @throws ServiceException if an error occurs
     */
    public RequirementServiceInterface getRequirementService() throws ServiceException;

    /**
     * Returns the purchasing repository instance.
     * @return a <code>PurchasingRepositoryInterface</code> value
     * @throws RepositoryException if an error occurs
     */
    public PurchasingRepositoryInterface getPurchasingRepository() throws RepositoryException;
}
