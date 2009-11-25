/*
 * Copyright (c) 2009 - 2009 Open Source Strategies, Inc.
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
package org.opentaps.domain.search;

import org.opentaps.foundation.domain.DomainInterface;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;

/**
 * This is the interface of the Search domain.
 */
public interface SearchDomainInterface extends DomainInterface {
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
    
    /**
     * Returns the sales opportunity search service.
     * @return a <code>SalesOpportunitySearchServiceInterface</code> value
     * @throws ServiceException if an error occurs
     */
    public SalesOpportunitySearchServiceInterface getSalesOpportunitySearchService() throws ServiceException;

    /**
     * Returns the sales order search repository.
     * @return a <code>SalesOrderSearchRepositoryInterface</code> value
     * @throws ServiceException if an error occurs
     */
    public SalesOrderSearchRepositoryInterface getSalesOrderSearchRepository() throws RepositoryException;
    
    /**
     * Returns the common search repository.
     * @return a <code>SearchRepositoryInterface</code> value
     * @throws ServiceException if an error occurs
     */
    public SearchRepositoryInterface getSearchRepository() throws RepositoryException;
}
