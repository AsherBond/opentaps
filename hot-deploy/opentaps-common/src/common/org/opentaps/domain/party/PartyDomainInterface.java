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
package org.opentaps.domain.party;

import org.opentaps.foundation.domain.DomainInterface;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;

/**
 * This is the interface of the Party domain.
 */
public interface PartyDomainInterface extends DomainInterface {

    /**
     * Returns the party repository.
     * @return a <code>PartyRepositoryInterface</code> value
     * @throws RepositoryException if an error occurs
     */
    public PartyRepositoryInterface getPartyRepository() throws RepositoryException;

    /**
     * Returns the account search service.
     * @return a <code>AccountSearchServiceInterface</code> value
     * @throws ServiceException if an error occurs
     */
    public AccountSearchServiceInterface getAccountSearchService() throws ServiceException;

    /**
     * Returns the contact search service.
     * @return a <code>ContactSearchServiceInterface</code> value
     * @throws ServiceException if an error occurs
     */
    public ContactSearchServiceInterface getContactSearchService() throws ServiceException;

    /**
     * Returns the lead search service.
     * @return a <code>LeadSearchServiceInterface</code> value
     * @throws ServiceException if an error occurs
     */
    public LeadSearchServiceInterface getLeadSearchService() throws ServiceException;

    /**
     * Returns the supplier search service.
     * @return a <code>SupplierSearchServiceInterface</code> value
     * @throws ServiceException if an error occurs
     */
    public SupplierSearchServiceInterface getSupplierSearchService() throws ServiceException;
}
