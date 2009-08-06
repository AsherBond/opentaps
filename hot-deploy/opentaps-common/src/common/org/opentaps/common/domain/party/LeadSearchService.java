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
package org.opentaps.common.domain.party;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.search.FullTextQuery;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.domain.base.entities.PartyRole;
import org.opentaps.domain.base.entities.PartyRolePk;
import org.opentaps.domain.party.Lead;
import org.opentaps.domain.party.LeadSearchServiceInterface;
import org.opentaps.domain.party.PartyRepositoryInterface;
import org.opentaps.domain.search.SearchService;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;

/**
 * The implementation of the Lead search service.
 */
public class LeadSearchService extends SearchService implements LeadSearchServiceInterface {

    private List<Lead> leads = null;

    /** {@inheritDoc} */
    public List<Lead> getLeads() {
        return leads;
    }

    /** {@inheritDoc} */
    public void makeQuery(StringBuilder sb) {
        PartySearch.makePersonQuery(sb, "PROSPECT");
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public Set<Class> getClassesToQuery() {
        return PartySearch.PARTY_CLASSES;
    }

    /** {@inheritDoc} */
    public void search() throws ServiceException {
        StringBuilder sb = new StringBuilder();
        makeQuery(sb);
        searchInEntities(getClassesToQuery(), sb.toString());

        try {
            PartyRepositoryInterface partyRepository = getDomainsDirectory().getPartyDomain().getPartyRepository();
            leads = filterSearchResults(getResults(), partyRepository);
        } catch (RepositoryException e) {
            throw new ServiceException(e);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public List<Lead> filterSearchResults(List<Object[]> results, PartyRepositoryInterface repository) throws ServiceException {

        // get the entities from the search results
        Set<String> leadIds = new HashSet<String>();
        int classIndex = getQueryProjectedFieldIndex(FullTextQuery.OBJECT_CLASS);
        int idIndex = getQueryProjectedFieldIndex(FullTextQuery.ID);
        if (classIndex < 0 || idIndex < 0) {
            throw new ServiceException("Incompatible result projection, classIndex = " + classIndex + ", idIndex = " + idIndex);
        }

        for (Object[] o : results) {
            Class c = (Class) o[classIndex];
            if (c.equals(PartyRole.class)) {
                PartyRolePk pk = (PartyRolePk) o[idIndex];
                if ("PROSPECT".equals(pk.getRoleTypeId())) {
                    leadIds.add(pk.getPartyId());
                }
            }
        }

        try {
            if (!leadIds.isEmpty()) {
                return repository.findList(Lead.class, new EntityExpr(Lead.Fields.partyId.name(), EntityOperator.IN, leadIds));
            } else {
                return new ArrayList<Lead>();
            }
        } catch (GeneralException ex) {
            throw new ServiceException(ex);
        }
    }
}
