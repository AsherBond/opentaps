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
package org.opentaps.search.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.search.FullTextQuery;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.base.constants.RoleTypeConstants;
import org.opentaps.base.entities.PartyGroup;
import org.opentaps.base.entities.PartyRole;
import org.opentaps.base.entities.PartyRolePk;
import org.opentaps.domain.party.PartyRepositoryInterface;
import org.opentaps.domain.search.SupplierSearchServiceInterface;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;

/**
 * The implementation of the Account search service.
 */
public class SupplierSearchService extends SearchService implements SupplierSearchServiceInterface {

    private List<PartyGroup> suppliers = null;

    /** {@inheritDoc} */
    public List<PartyGroup> getSuppliers() {
        return suppliers;
    }

    /** {@inheritDoc} */
    public void makeQuery(StringBuilder sb) {
        PartySearch.makePartyGroupQuery(sb, RoleTypeConstants.SUPPLIER);
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
        suppliers = filterSearchResults(getResults());
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public List<PartyGroup> filterSearchResults(List<Object[]> results) throws ServiceException {
        try {
            PartyRepositoryInterface partyRepository = getDomainsDirectory().getPartyDomain().getPartyRepository();
            // get the entities from the search results
            Set<String> supplierIds = new HashSet<String>();
            int classIndex = getQueryProjectedFieldIndex(FullTextQuery.OBJECT_CLASS);
            int idIndex = getQueryProjectedFieldIndex(FullTextQuery.ID);
            if (classIndex < 0 || idIndex < 0) {
                throw new ServiceException("Incompatible result projection, classIndex = " + classIndex + ", idIndex = " + idIndex);
            }
    
            for (Object[] o : results) {
                Class c = (Class) o[classIndex];
                if (c.equals(PartyRole.class)) {
                    PartyRolePk pk = (PartyRolePk) o[idIndex];
                    if (RoleTypeConstants.SUPPLIER.equals(pk.getRoleTypeId())) {
                        supplierIds.add(pk.getPartyId());
                    }
                }
            }

            if (!supplierIds.isEmpty()) {
                return partyRepository.findList(PartyGroup.class, EntityCondition.makeCondition(PartyGroup.Fields.partyId.name(), EntityOperator.IN, supplierIds));
            } else {
                return new ArrayList<PartyGroup>();
            }
        } catch (RepositoryException e) {
            throw new ServiceException(e);
        } catch (GeneralException ex) {
            throw new ServiceException(ex);
        }
    }
}
