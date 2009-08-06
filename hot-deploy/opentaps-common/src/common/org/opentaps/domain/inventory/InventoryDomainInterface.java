/*
 * Copyright (c) 2008 - 2009 Open Source Strategies, Inc.
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
package org.opentaps.domain.inventory;

import org.opentaps.foundation.domain.DomainInterface;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;

/**
 * This is the interface of the Inventory domain.
 */
public interface InventoryDomainInterface extends DomainInterface {

    /**
     * Returns the inventory repository instance.
     * @return a <code>InventoryRepositoryInterface</code> value
     * @throws RepositoryException if an error occurs
     */
    public InventoryRepositoryInterface getInventoryRepository() throws RepositoryException;

    /**
     * Returns the inventory service instance.
     * @return an <code>InventoryServiceInterface</code> value
     * @throws ServiceException if an error occurs
     */
    public InventoryServiceInterface getInventoryService() throws ServiceException;
}
