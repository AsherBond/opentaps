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

import org.hibernate.search.FullTextQuery;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.base.entities.SalesOpportunity;
import org.opentaps.domain.order.OrderRepositoryInterface;
import org.opentaps.domain.search.SalesOpportunitySearchServiceInterface;
import org.opentaps.domain.search.SearchRepositoryInterface;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;
import org.opentaps.search.SearchService;
import org.opentaps.search.party.PartySearch;
import org.opentaps.base.constants.SalesOpportunityStageConstants;

/**
 * The implementation of the Sales Opportunity search service.
 */
public class SalesOpportunitySearchService extends SearchService implements SalesOpportunitySearchServiceInterface {

    /** Common set of class to query for Party related search. */
    @SuppressWarnings("unchecked")
    public static final Set<Class> SALES_OPPORTUNITY_CLASSES = new HashSet<Class>(Arrays.asList(
            SalesOpportunity.class
        ));

    private List<SalesOpportunity> salesOpportunities = null;

    /** {@inheritDoc} */
    public List<SalesOpportunity> getSalesOpportunities() {
        return salesOpportunities;
    }

    /** {@inheritDoc} */
    public void makeQuery(StringBuilder sb) {
        // filter canceled (lost) Sales Opportunities
        sb.append("( -").append(SalesOpportunity.Fields.opportunityStageId.name()).append(":\"").append(SalesOpportunityStageConstants.SOSTG_LOST).append("\" +(");
        makeSalesOpportunityQuery(sb);
        PartySearch.makePartyGroupFieldsQuery(sb);
        PartySearch.makePersonFieldsQuery(sb);
        sb.append("))");
    }

    /**
     * Builds the Lucene query for a SalesOpportunity related search.
     * @param sb the string builder instance currently building the query
     */
    public static void makeSalesOpportunityQuery(StringBuilder sb) {
        for (String f : Arrays.asList(SalesOpportunity.Fields.salesOpportunityId.name(), SalesOpportunity.Fields.opportunityName.name(), SalesOpportunity.Fields.description.name())) {
            sb.append(f).append(":").append(SearchRepositoryInterface.DEFAULT_PLACEHOLDER).append(" ");
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public Set<Class> getClassesToQuery() {
        return SALES_OPPORTUNITY_CLASSES;
    }

    /** {@inheritDoc} */
    public void search() throws ServiceException {
        StringBuilder sb = new StringBuilder();
        makeQuery(sb);
        searchInEntities(getClassesToQuery(), sb.toString());
        salesOpportunities = filterSearchResults(getResults());
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public List<SalesOpportunity> filterSearchResults(List<Object[]> results) throws ServiceException {

        try {
            OrderRepositoryInterface orderRepository = getDomainsDirectory().getOrderDomain().getOrderRepository();
            // get the entities from the search results
            Set<String> salesOpportunityIds = new HashSet<String>();
            int classIndex = getQueryProjectedFieldIndex(FullTextQuery.OBJECT_CLASS);
            int idIndex = getQueryProjectedFieldIndex(FullTextQuery.ID);
            if (classIndex < 0 || idIndex < 0) {
                throw new ServiceException("Incompatible result projection, classIndex = " + classIndex + ", idIndex = " + idIndex);
            }

            for (Object[] o : results) {
                Class c = (Class) o[classIndex];
                if (c.equals(SalesOpportunity.class)) {
                    String pk = (String) o[idIndex];
                    salesOpportunityIds.add(pk);
                }
            }

            if (!salesOpportunityIds.isEmpty()) {
                return orderRepository.findList(SalesOpportunity.class, EntityCondition.makeCondition(SalesOpportunity.Fields.salesOpportunityId.name(), EntityOperator.IN, salesOpportunityIds));
            } else {
                return new ArrayList<SalesOpportunity>();
            }
        } catch (RepositoryException e) {
            throw new ServiceException(e);
        } catch (GeneralException ex) {
            throw new ServiceException(ex);
        }
    }
}
