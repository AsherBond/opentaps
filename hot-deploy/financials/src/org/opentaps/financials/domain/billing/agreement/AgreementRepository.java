/*
 * Copyright (c) 2007-2009 Open Source Strategies, Inc.
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
package org.opentaps.financials.domain.billing.agreement;

import java.util.List;

import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.opentaps.domain.base.entities.Agreement;
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
