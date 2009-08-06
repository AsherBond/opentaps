/*
 * Copyright (c) 2009 - 2009 Open Source Strategies, Inc.
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
package org.opentaps.common.domain.order;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.search.FullTextQuery;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.common.domain.party.PartySearch;
import org.opentaps.domain.base.entities.SalesOpportunity;
import org.opentaps.domain.order.OrderRepositoryInterface;
import org.opentaps.domain.order.SalesOpportunitySearchServiceInterface;
import org.opentaps.domain.search.SearchService;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;

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
        sb.append("( -opportunityStageId:\"SOSTG_LOST\" +(");
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
        for (String f : Arrays.asList("salesOpportunityId", "opportunityName", "description")) {
            sb.append(f).append(":").append(SearchService.DEFAULT_PLACEHOLDER).append(" ");
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

        try {
            OrderRepositoryInterface orderRepository = getDomainsDirectory().getOrderDomain().getOrderRepository();
            salesOpportunities = filterSearchResults(getResults(), orderRepository);
        } catch (RepositoryException e) {
            throw new ServiceException(e);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public List<SalesOpportunity> filterSearchResults(List<Object[]> results, OrderRepositoryInterface repository) throws ServiceException {

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

        try {
            if (!salesOpportunityIds.isEmpty()) {
                return repository.findList(SalesOpportunity.class, new EntityExpr(SalesOpportunity.Fields.salesOpportunityId.name(), EntityOperator.IN, salesOpportunityIds));
            } else {
                return new ArrayList<SalesOpportunity>();
            }
        } catch (GeneralException ex) {
            throw new ServiceException(ex);
        }
    }
}
