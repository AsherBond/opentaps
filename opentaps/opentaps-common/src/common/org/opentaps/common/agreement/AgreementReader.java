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
package org.opentaps.common.agreement;

import java.math.BigDecimal;
import java.util.List;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.util.EntityUtil;

/**
 * Agreement Reader.  This object follows the Reader pattern, which provides a convenience API
 * for accessing information about an agreement.
 * @author <a href="mailto:leon@opensourcestrategies.com">Leon Torres</a>
 */
public class AgreementReader {

    private static final String MODULE = AgreementReader.class.getName();

    protected GenericValue agreement = null;
    protected String agreementId = null;
    protected List<GenericValue> agreementTerms = null;
    protected Delegator delegator = null;

    protected AgreementReader() { }

    public AgreementReader(GenericValue agreement) throws GenericEntityException {
        if (agreement == null) {
            throw new GenericEntityException("Agreement not found.");
        }

        this.delegator = agreement.getDelegator();
        if ("Agreement".equals(agreement.getEntityName())) {
            this.agreement = agreement;
        } else {
            this.agreement = delegator.findByPrimaryKey("Agreement", UtilMisc.toMap("agreementId", agreement.get("agreementId")));
        }
        this.agreementId = agreement.getString("agreementId");
        this.agreementTerms = EntityUtil.filterByDate(this.agreement.getRelated("AgreementTerm"));
    }

    public AgreementReader(String agreementId, Delegator delegator) throws GenericEntityException {
        this(delegator.findByPrimaryKey("Agreement", UtilMisc.toMap("agreementId", agreementId)));
    }

    public String getAgreementId() {
        return agreementId;
    }

    public String getPartyIdFrom() {
        return agreement.getString("partyIdFrom");
    }

    public String getPartyIdTo() {
        return agreement.getString("partyIdTo");
    }

    /** Gets a term from the agreement, if it exists.  Otherwise returns null. */
    public GenericValue getTerm(String termTypeId) {
        if (UtilValidate.isEmpty(termTypeId)) {
            throw new IllegalArgumentException("Called AgreementReader.hasTerm(termTypeId) with null or empty termTypeId.");
        }
        for (GenericValue term : agreementTerms) {
            if (termTypeId.equals(term.get("termTypeId"))) {
                return term;
            }
        }
        return null;
    }

    /** Gets the term by termTypeId, otherwise throws IllegalArgumentException if it doesn't exist. */
    public GenericValue getTermOrFail(String termTypeId) {
        GenericValue term = getTerm(termTypeId);
        if (term == null) {
            throw new IllegalArgumentException("No such agreement term [" + termTypeId + "] exists in agreement [" + agreementId + "].");
        }
        return term;
    }

    public boolean hasTerm(String termTypeId) {
        GenericValue term = getTerm(termTypeId);
        return term != null;
    }

    public BigDecimal getTermValueBigDecimal(String termTypeId) {
        GenericValue term = getTermOrFail(termTypeId);
        return term.getBigDecimal("termValue");
    }

    public Double getTermValueDouble(String termTypeId) {
        GenericValue term = getTermOrFail(termTypeId);
        return term.getDouble("termValue");
    }

    public String getTermCurrency(String termTypeId) {
        GenericValue term = getTermOrFail(termTypeId);
        return term.getString("currencyUomId");
    }

    /**
     * Finds agreement for any status.
     */
    public static AgreementReader findAgreement(String partyIdFrom, String partyIdTo, String agreementTypeId, String termTypeId, Delegator delegator) throws GenericEntityException {
        return findAgreement(partyIdFrom, partyIdTo, agreementTypeId, termTypeId, null, delegator);
    }

    /**
     * Finds an agreement with the given type and term and returns a reader for it.  If more than one agreement exists, then
     * this will return the earliest defined agreement and log a warning about there being conflicting terms.
     * TODO: this doesn't enforce agreement roles.
     */
    public static AgreementReader findAgreement(String partyIdFrom, String partyIdTo, String agreementTypeId, String termTypeId, String statusId, Delegator delegator) throws GenericEntityException {
        List<EntityCondition> conditions = UtilMisc.toList(
                EntityCondition.makeCondition("agreementTypeId", agreementTypeId),
                EntityCondition.makeCondition("partyIdFrom", partyIdFrom),
                EntityCondition.makeCondition("partyIdTo", partyIdTo),
                EntityCondition.makeCondition("termTypeId", termTypeId),
                EntityUtil.getFilterByDateExpr()
        );
        if (statusId != null) {
            conditions.add(EntityCondition.makeCondition("statusId", statusId));
        }
        List<GenericValue> agreements = delegator.findByAnd("AgreementAndItemAndTerm", conditions, UtilMisc.toList("fromDate ASC"));
        if (agreements.size() == 0) {
            return null;
        }
        if (agreements.size() > 1) {
            Debug.logWarning("Duplicate agreements found:  Agreement type [" + agreementTypeId + "] from [" + partyIdFrom + "] to [" + partyIdTo + "] and term type [" + termTypeId + "]", MODULE);
        }
        return new AgreementReader(EntityUtil.getFirst(agreements));
    }
}
