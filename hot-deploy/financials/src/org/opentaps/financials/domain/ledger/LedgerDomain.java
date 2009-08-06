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
