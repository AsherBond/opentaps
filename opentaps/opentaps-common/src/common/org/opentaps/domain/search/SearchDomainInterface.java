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
package org.opentaps.domain.search;

import org.opentaps.domain.search.communication.CaseSearchServiceInterface;
import org.opentaps.domain.search.order.PurchaseOrderSearchServiceInterface;
import org.opentaps.domain.search.order.SalesOpportunitySearchServiceInterface;
import org.opentaps.domain.search.order.SalesOrderSearchServiceInterface;
import org.opentaps.domain.search.party.AccountSearchServiceInterface;
import org.opentaps.domain.search.party.ContactSearchServiceInterface;
import org.opentaps.domain.search.party.LeadSearchServiceInterface;
import org.opentaps.domain.search.party.SupplierSearchServiceInterface;
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
     * Returns the sales order search service.
     * @return a <code>SalesOrderSearchServiceInterface</code> value
     * @throws ServiceException if an error occurs
     */
    public SalesOrderSearchServiceInterface getSalesOrderSearchService() throws ServiceException;

    /**
     * Returns the purchase order search service.
     * @return a <code>PurchaseOrderSearchServiceInterface</code> value
     * @throws ServiceException if an error occurs
     */
    public PurchaseOrderSearchServiceInterface getPurchaseOrderSearchService() throws ServiceException;

    /**
     * Returns the case search service.
     * @return a <code>CaseSearchServiceInterface</code> value
     * @throws ServiceException if an error occurs
     */
    public CaseSearchServiceInterface getCaseSearchService() throws ServiceException;


    /**
     * Returns the common search repository.
     * @return a <code>SearchRepositoryInterface</code> value
     * @throws RepositoryException if an error occurs
     */
    public SearchRepositoryInterface getSearchRepository() throws RepositoryException;
}
