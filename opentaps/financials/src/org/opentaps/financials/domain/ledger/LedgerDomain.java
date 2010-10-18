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
package org.opentaps.financials.domain.ledger;

import org.opentaps.domain.ledger.EncumbranceRepositoryInterface;
import org.opentaps.domain.ledger.LedgerDomainInterface;
import org.opentaps.foundation.domain.Domain;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;

/**
 * Ledger domain.
 */
public class LedgerDomain extends Domain implements LedgerDomainInterface {

    /** {@inheritDoc} */
    public LedgerRepository getLedgerRepository() throws RepositoryException {
        return instantiateRepository(LedgerRepository.class);
    }

    /** {@inheritDoc} */
    public LedgerService getLedgerService() throws ServiceException {
        return instantiateService(LedgerService.class);
    }

    /** {@inheritDoc} */
    public InvoiceLedgerService getInvoiceLedgerService() throws ServiceException {
        return instantiateService(InvoiceLedgerService.class);
    }

    /** {@inheritDoc} */
    public EncumbranceRepositoryInterface getEncumbranceRepository() throws RepositoryException {
        return instantiateRepository(EncumbranceRepository.class);
    }

}
