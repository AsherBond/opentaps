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
package org.opentaps.search.order;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.base.constants.SalesOpportunityStageConstants;
import org.opentaps.base.entities.SalesOpportunity;
import org.opentaps.base.entities.SalesOpportunityRole;
import org.opentaps.base.entities.SalesOpportunityRolePk;
import org.opentaps.domain.order.OrderRepositoryInterface;
import org.opentaps.domain.search.SearchDomainInterface;
import org.opentaps.domain.search.SearchRepositoryInterface;
import org.opentaps.domain.search.SearchResult;
import org.opentaps.domain.search.order.SalesOpportunitySearchServiceInterface;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;
import org.opentaps.search.HibernateSearchRepository;
import org.opentaps.search.HibernateSearchService;
import org.opentaps.search.party.PartySearch;

/**
 * The implementation of the Sales Opportunity search service.
 */
public class SalesOpportunitySearchService extends HibernateSearchService implements SalesOpportunitySearchServiceInterface {

    /** Common set of class to query for Sales Opportunity related search. */
    public static final Set<Class<?>> SALES_OPPORTUNITY_CLASSES = new HashSet<Class<?>>(Arrays.asList(
            SalesOpportunityRole.class
        ));

    private List<SalesOpportunity> salesOpportunities = new ArrayList<SalesOpportunity>();

    /** {@inheritDoc} */
    public List<SalesOpportunity> getSalesOpportunities() {
        return salesOpportunities;
    }

    /** {@inheritDoc} */
    public String getQueryString() {
        StringBuilder sb = new StringBuilder();
        // filter canceled (lost) Sales Opportunities
        sb.append("( -salesOpportunity.").append(SalesOpportunity.Fields.opportunityStageId.name()).append(":\"").append(SalesOpportunityStageConstants.SOSTG_LOST).append("\" +(");
        makeSalesOpportunityQuery(sb);
        PartySearch.makePartyGroupFieldsQuery(sb);
        PartySearch.makePersonFieldsQuery(sb);
        sb.append("))");
        return sb.toString();
    }

    /**
     * Builds the Lucene query for a SalesOpportunity related search.
     * @param sb the string builder instance currently building the query
     */
    public static void makeSalesOpportunityQuery(StringBuilder sb) {
        for (String f : Arrays.asList(SalesOpportunity.Fields.salesOpportunityId.name(), SalesOpportunity.Fields.opportunityName.name(), SalesOpportunity.Fields.description.name())) {
            sb.append("salesOpportunity.").append(f).append(":").append(HibernateSearchRepository.DEFAULT_PLACEHOLDER).append(" ");
        }
    }

    /** {@inheritDoc} */
    public Set<Class<?>> getClassesToQuery() {
        return SALES_OPPORTUNITY_CLASSES;
    }

    /** {@inheritDoc} */
    public void search() throws ServiceException {
        try {
            SearchDomainInterface searchDomain = getDomainsDirectory().getSearchDomain();
            SearchRepositoryInterface searchRepository = searchDomain.getSearchRepository();
            search(searchRepository);
        } catch (RepositoryException e) {
            throw new ServiceException(e);
        }
    }

    /** {@inheritDoc} */
    public void readSearchResults(List<SearchResult> results) throws ServiceException {

        try {
            OrderRepositoryInterface orderRepository = getDomainsDirectory().getOrderDomain().getOrderRepository();
            // get the entities from the search results
            Set<String> salesOpportunityIds = new HashSet<String>();
            for (SearchResult o : results) {
                Class<?> c = o.getResultClass();
                if (c.equals(SalesOpportunityRole.class)) {
                    SalesOpportunityRolePk pk = (SalesOpportunityRolePk) o.getResultPk();
                    salesOpportunityIds.add(pk.getSalesOpportunityId());
                }
            }

            // apply pagination here
            setResultSize(salesOpportunityIds.size());
            if (!salesOpportunityIds.isEmpty() && getPageStart() < salesOpportunityIds.size() && getPageSize() > 0) {
                salesOpportunities = orderRepository.findPage(SalesOpportunity.class, EntityCondition.makeCondition(SalesOpportunity.Fields.salesOpportunityId.name(), EntityOperator.IN, salesOpportunityIds), getPageStart(), getPageSize());
            } else {
                salesOpportunities = new ArrayList<SalesOpportunity>();
            }
        } catch (RepositoryException e) {
            throw new ServiceException(e);
        }
    }
}
