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
package org.opentaps.financials.domain.ledger;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilValidate;
import org.opentaps.base.entities.EncumbranceDetail;
import org.opentaps.base.entities.EncumbranceSnapshot;
import org.opentaps.domain.ledger.EncumbranceRepositoryInterface;
import org.opentaps.foundation.entity.hibernate.Session;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.ofbiz.Repository;

public class EncumbranceRepository extends Repository implements  EncumbranceRepositoryInterface {

    private static final String MODULE = EncumbranceRepository.class.getName();

    public EncumbranceRepository() {
        super();
    }

    /** {@inheritDoc} */
    public void createEncumbranceDetail(Session session, EncumbranceDetail detail) {
        session.save(detail);
    }

    /** {@inheritDoc} */
    public void createEncumbranceSnapshot(EncumbranceSnapshot snapshot, List<EncumbranceDetail> details, Session session) throws RepositoryException {

            session.save(snapshot);
            session.flush();

            if (UtilValidate.isNotEmpty(details)) {
                for (EncumbranceDetail detail : details) {
                    detail.setEncumbranceSnapshotId(snapshot.getEncumbranceSnapshotId());
                    createEncumbranceDetail(session, detail);
                }
            }

    }

    /** {@inheritDoc} */
    public List<EncumbranceDetail> getEncumbranceDetails(String organizationPartyId, Map<String, String> accountingTags) throws RepositoryException {
        return getEncumbranceDetails(organizationPartyId, accountingTags, UtilDateTime.nowTimestamp());
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public List<EncumbranceDetail> getEncumbranceDetails(String organizationPartyId, Map<String, String> accountingTags, Timestamp asOfDate) throws RepositoryException {
        Session session = null;
        List<EncumbranceDetail> encumbranceDetails = null;

        try {
            session = getInfrastructure().getSession();

            // retrieve max snapshot time under asOfDate
            Criteria lastSnapshotDate = session.createCriteria(EncumbranceSnapshot.class);
            lastSnapshotDate.add(Restrictions.le(EncumbranceSnapshot.Fields.snapshotDatetime.getName(), asOfDate));
            lastSnapshotDate.setProjection(Projections.max(EncumbranceSnapshot.Fields.snapshotDatetime.getName()));
            List<Timestamp> snapshotMaxDate = lastSnapshotDate.list();
            Timestamp ts = snapshotMaxDate.get(0);
            if (ts == null) {
                Debug.logWarning("There is no encumbrance snapshot created before " + asOfDate.toString(), MODULE);
                return new ArrayList<EncumbranceDetail>();
            }
            Debug.logInfo("Using encumbrance snapshot from " + ts.toString(), MODULE);

            Criteria snapshot = session.createCriteria(EncumbranceSnapshot.class);
            snapshot.add(Restrictions.eq(EncumbranceSnapshot.Fields.snapshotDatetime.getName(), ts));
            List<EncumbranceSnapshot> snapshots = snapshot.list();

            String snapshotId = snapshots.get(0).getEncumbranceSnapshotId();

            Criteria details = session.createCriteria(EncumbranceDetail.class);
            details.add(Restrictions.eq(String.format("id.%1$s", EncumbranceDetail.Fields.encumbranceSnapshotId.getName()), snapshotId));
            details.add(Restrictions.eq(EncumbranceDetail.Fields.organizationPartyId.getName(), organizationPartyId));
            buildAccountingTagConditions(details, accountingTags);
            details.addOrder(Order.asc(String.format("id.%1$s", EncumbranceDetail.Fields.encumbranceDetailSeqId.getName())));
            encumbranceDetails = details.list();

        } catch (InfrastructureException e) {
            throw new RepositoryException(e.getMessage());
        } catch (HibernateException e) {
            throw new RepositoryException(e.getMessage());
        } finally {
            if (session != null) {
                session.close();
            }
        }

        return encumbranceDetails;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public BigDecimal getTotalEncumberedValue(String organizationPartyId,  Map<String, String> accountingTags, Timestamp asOfDate) throws RepositoryException {
        Session session = null;
        BigDecimal encumberedValueTotal = null;

        try {
            session = getInfrastructure().getSession();

            // retrieve max snapshot time under asOfDate
            Criteria lastSnapshotDate = session.createCriteria(EncumbranceSnapshot.class);
            lastSnapshotDate.add(Restrictions.le(EncumbranceSnapshot.Fields.snapshotDatetime.getName(), asOfDate));
            lastSnapshotDate.setProjection(Projections.max(EncumbranceSnapshot.Fields.snapshotDatetime.getName()));
            List<Timestamp> snapshotMaxDate = lastSnapshotDate.list();
            Timestamp ts = snapshotMaxDate.get(0);
            if (ts == null) {
                Debug.logWarning("There is no encumbrance snapshot created before " + asOfDate.toString(), MODULE);
                return null;
            }
            Debug.logInfo("Using encumbrance snapshot from " + ts.toString(), MODULE);

            Criteria snapshot = session.createCriteria(EncumbranceSnapshot.class);
            snapshot.add(Restrictions.eq(EncumbranceSnapshot.Fields.snapshotDatetime.getName(), ts));
            List<EncumbranceSnapshot> snapshots = snapshot.list();

            String snapshotId = snapshots.get(0).getEncumbranceSnapshotId();

            Criteria encumberedValueCriteria = session.createCriteria(EncumbranceDetail.class);
            encumberedValueCriteria.add(Restrictions.eq(String.format("id.%1$s", EncumbranceDetail.Fields.encumbranceSnapshotId.getName()), snapshotId));
            encumberedValueCriteria.add(Restrictions.eq(EncumbranceDetail.Fields.organizationPartyId.getName(), organizationPartyId));
            buildAccountingTagConditions(encumberedValueCriteria, accountingTags);
            encumberedValueCriteria.setProjection(Projections.sum(EncumbranceDetail.Fields.encumberedAmount.getName()));
            List<BigDecimal> totals = encumberedValueCriteria.list();
            encumberedValueTotal = totals.get(0);

        } catch (InfrastructureException e) {
            throw new RepositoryException(e.getMessage());
        } catch (HibernateException e) {
        	// return the RepositoryException with the message of exception
            throw new RepositoryException(e.getMessage());
        } finally {
            if (session != null) {
                session.close();
            }
        }

        return encumberedValueTotal;
    }

    /**
     * Utility method that adds criteria for accounting tags.
     *
     * @param conditions a criteria where we have to add new criterions
     * @param accountingTags accounting tags and values. We expects a <code>Map</code> where
     * tags mapped to values in following way: acctgTagEnumId[1-10] -> NULL_TAG | a value
     */
    private void buildAccountingTagConditions(Criteria conditions, Map<String, String> accountingTags) {
        if (UtilValidate.isEmpty(accountingTags)) {
            return;
        }

        Set<String> tagNames = accountingTags.keySet();
        for (String tag : tagNames) {
            String value = accountingTags.get(tag);
            conditions.add("NULL_TAG".equals(value) ? Restrictions.isNull(tag) : Restrictions.eq(tag, value));
        }
    }
}
