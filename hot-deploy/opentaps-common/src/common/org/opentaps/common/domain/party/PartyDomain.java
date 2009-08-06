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
package org.opentaps.common.domain.party;

import org.opentaps.domain.party.AccountSearchServiceInterface;
import org.opentaps.domain.party.ContactSearchServiceInterface;
import org.opentaps.domain.party.LeadSearchServiceInterface;
import org.opentaps.domain.party.PartyDomainInterface;
import org.opentaps.domain.party.SupplierSearchServiceInterface;
import org.opentaps.foundation.domain.Domain;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;

/**
 * This is an implementation of the Party domain.
 */
public class PartyDomain extends Domain implements PartyDomainInterface {

    /** {@inheritDoc} */
    public PartyRepository getPartyRepository() throws RepositoryException {
        return instantiateRepository(PartyRepository.class);
    }

    /** {@inheritDoc} */
    public AccountSearchServiceInterface getAccountSearchService() throws ServiceException {
        return instantiateService(AccountSearchService.class);
    }

    /** {@inheritDoc} */
    public ContactSearchServiceInterface getContactSearchService() throws ServiceException {
        return instantiateService(ContactSearchService.class);
    }

    /** {@inheritDoc} */
    public LeadSearchServiceInterface getLeadSearchService() throws ServiceException {
        return instantiateService(LeadSearchService.class);
    }

    /** {@inheritDoc} */
    public SupplierSearchServiceInterface getSupplierSearchService() throws ServiceException {
        return instantiateService(SupplierSearchService.class);
    }

}
