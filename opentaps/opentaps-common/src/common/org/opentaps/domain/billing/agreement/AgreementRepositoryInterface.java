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
package org.opentaps.domain.billing.agreement;

import java.util.List;

import org.opentaps.base.entities.Agreement;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.RepositoryInterface;

/**
 * Repository for Invoices to handle interaction of Invoice-related domain with the entity engine (database) and the service engine.
 */
public interface AgreementRepositoryInterface extends RepositoryInterface {

    /**
     * Gets agreements for a supplier.
     * @param partyId supplier identifier
     * @param organizationPartyId company identifier, agreement party from
     * @return
     *     List of <code>Agreement</code> where given supplier is contracting party.
     * @throws RepositoryException 
     */
    public List<? extends Agreement> getSupplierAgreements(String partyId, String organizationPartyId) throws RepositoryException;

}
