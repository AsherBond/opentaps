/*
 * Copyright (c) 2007 - 2009 Open Source Strategies, Inc.
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
package org.opentaps.warehouse.domain.inventory;

import org.opentaps.domain.inventory.InventoryDomainInterface;
import org.opentaps.domain.inventory.InventoryRepositoryInterface;
import org.opentaps.domain.inventory.InventoryServiceInterface;
import org.opentaps.foundation.domain.Domain;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;

/**
 * This is an implementation of the Inventory domain.
 */
public class InventoryDomain extends Domain implements InventoryDomainInterface {

    /** {@inheritDoc} */
    public InventoryRepositoryInterface getInventoryRepository() throws RepositoryException {
        return instantiateRepository(InventoryRepository.class);
    }

    /** {@inheritDoc} */
    public InventoryServiceInterface getInventoryService() throws ServiceException {
        return instantiateService(InventoryService.class);
    }

}
