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
package org.opentaps.search;

import org.opentaps.domain.search.SearchDomainInterface;
import org.opentaps.domain.search.SearchRepositoryInterface;
import org.opentaps.domain.search.communication.CaseSearchServiceInterface;
import org.opentaps.domain.search.order.PurchaseOrderSearchServiceInterface;
import org.opentaps.domain.search.order.SalesOpportunitySearchServiceInterface;
import org.opentaps.domain.search.order.SalesOrderSearchServiceInterface;
import org.opentaps.domain.search.party.AccountSearchServiceInterface;
import org.opentaps.domain.search.party.ContactSearchServiceInterface;
import org.opentaps.domain.search.party.LeadSearchServiceInterface;
import org.opentaps.domain.search.party.SupplierSearchServiceInterface;
import org.opentaps.foundation.domain.Domain;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;
import org.opentaps.search.communication.CaseSearchService;
import org.opentaps.search.order.PurchaseOrderSearchService;
import org.opentaps.search.order.SalesOpportunitySearchService;
import org.opentaps.search.order.SalesOrderSearchService;
import org.opentaps.search.party.AccountSearchService;
import org.opentaps.search.party.ContactSearchService;
import org.opentaps.search.party.LeadSearchService;
import org.opentaps.search.party.SupplierSearchService;

/**
 * This is an hibernate implementation of the Search domain.
 */
public class SearchDomain extends Domain implements SearchDomainInterface {

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
    public SalesOpportunitySearchServiceInterface getSalesOpportunitySearchService() throws ServiceException {
        return instantiateService(SalesOpportunitySearchService.class);
    }

    /** {@inheritDoc} */
    public SalesOrderSearchServiceInterface getSalesOrderSearchService() throws ServiceException {
        return instantiateService(SalesOrderSearchService.class);
    }

    /** {@inheritDoc} */
    public PurchaseOrderSearchServiceInterface getPurchaseOrderSearchService() throws ServiceException {
        return instantiateService(PurchaseOrderSearchService.class);
    }

    /** {@inheritDoc} */
    public SupplierSearchServiceInterface getSupplierSearchService() throws ServiceException {
        return instantiateService(SupplierSearchService.class);
    }

    /** {@inheritDoc} */
    public CaseSearchServiceInterface getCaseSearchService() throws ServiceException {
        return instantiateService(CaseSearchService.class);
    }

    /** {@inheritDoc} */
    public SearchRepositoryInterface getSearchRepository() throws RepositoryException {
        return instantiateRepository(HibernateSearchRepository.class);
    }


}
