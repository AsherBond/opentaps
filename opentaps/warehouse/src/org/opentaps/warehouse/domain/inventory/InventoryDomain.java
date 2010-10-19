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
