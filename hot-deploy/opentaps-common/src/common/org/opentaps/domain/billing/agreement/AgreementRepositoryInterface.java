/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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
package org.opentaps.domain.billing.agreement;

import java.util.List;

import org.opentaps.domain.base.entities.Agreement;
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
