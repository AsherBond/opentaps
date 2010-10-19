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
