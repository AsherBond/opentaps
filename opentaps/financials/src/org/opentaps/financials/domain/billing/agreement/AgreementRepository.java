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
package org.opentaps.financials.domain.billing.agreement;

import java.util.List;

import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.opentaps.base.entities.Agreement;
import org.opentaps.domain.billing.agreement.AgreementRepositoryInterface;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.ofbiz.Repository;

/**
 * Repository for Agreement entities to handle interaction of domain with the entity engine (database) and the service engine.
 */
public class AgreementRepository extends Repository implements AgreementRepositoryInterface {

    /** {@inheritDoc}
     * @throws RepositoryException */
    public List<? extends Agreement> getSupplierAgreements(String partyId, String organizationPartyId) throws RepositoryException {
        return findList(Agreement.class, EntityCondition.makeCondition(EntityOperator.AND,
                EntityCondition.makeCondition("agreementTypeId", EntityOperator.EQUALS, "PURCHASE_AGREEMENT"),
                EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "AGR_ACTIVE"),
                EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, organizationPartyId),
                EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, partyId),
                EntityUtil.getFilterByDateExpr()));
    }
}
