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
package org.opentaps.domain.ledger;

import org.opentaps.foundation.domain.DomainInterface;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;

/**
 * Interface for General Ledger system of accounting.
 */
public interface LedgerDomainInterface extends DomainInterface {

    /**
     * Returns the ledger repository instance.
     * @return a <code>LedgerRepositoryInterface</code> value
     * @throws RepositoryException if an error occurs
     */
    public LedgerRepositoryInterface getLedgerRepository() throws RepositoryException;

    /**
     * Returns the ledger service instance.
     * @return an <code>LedgerServiceInterface</code> value
     * @throws ServiceException if an error occurs
     */
    public LedgerServiceInterface getLedgerService() throws ServiceException;

    /**
     * Returns the invoice ledger service instance.
     * @return an <code>InvoiceLedgerServiceInterface</code> value
     * @throws ServiceException if an error occurs
     */
    public InvoiceLedgerServiceInterface getInvoiceLedgerService() throws ServiceException;

    /**
     * Returns the encumbrance repository instance.
     * @return an instance of class that implements <code>EncumbranceRepositoryInterface</code>
     * @throws RepositoryException if an error occurs
     */
    public EncumbranceRepositoryInterface getEncumbranceRepository() throws RepositoryException;
}
