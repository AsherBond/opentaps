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
package org.opentaps.domain.ledger;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.opentaps.base.entities.EncumbranceDetail;
import org.opentaps.base.entities.EncumbranceSnapshot;
import org.opentaps.foundation.entity.hibernate.Session;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.RepositoryInterface;

/**
 * Interface for encumbrance related entities.
 */
public interface EncumbranceRepositoryInterface extends RepositoryInterface {

    /**
     * Create an encumbrance snapshot and details wrapping them up to single transaction.
     * This method doesn't care about snapshot -> details relations. It assumes supplied snapshot object
     * is true parent of details.
     *
     * @param snapshot an instance of <code>EncumbranceSnapshot</code> class
     * @param details list of <code>EncumbranceDetail</code> objects
     * @param session Hibernate session object
     * @throws RepositoryException if an error occurs
     */
    public void createEncumbranceSnapshot(EncumbranceSnapshot snapshot, List<EncumbranceDetail> details, Session session) throws RepositoryException;

    /**
     * Create an encumbrance detail.
     *
     * @param session a session that should be used to save detail.
     * @param detail an instance of <code>EncumbranceDetail</code>
     */
    public void createEncumbranceDetail(Session session, EncumbranceDetail detail);

    /**
     * Gets all encumbrance details for the organization from the latest encumbrance snapshot prior to the asOfDate.
     *
     * @param organizationPartyId company identifier
     * @param accountingTags accountingTags; null for all the encumbrance details for all accounting tags; Map of tag -> NULL_TAG for tags which are null (ie, not set)
     * @param asOfDate desired snapshot date
     * @return List of encumbrance snapshot details.
     * @throws RepositoryException if an error occurs
     */
    public List<EncumbranceDetail> getEncumbranceDetails(String organizationPartyId, Map<String, String> accountingTags, Timestamp asOfDate) throws RepositoryException;

    /**
     * Gets all encumbrance details for the organization from the latest encumbrance snapshot.
     *
     * @param organizationPartyId company identifier
     * @param accountingTags accountingTags; null for all the encumbrance details for all accounting tags; Map of tag -> NULL_TAG for tags which are null (ie, not set)
     * @return List of encumbrance snapshot details.
     * @throws RepositoryException if an error occurs
     */
    public List<EncumbranceDetail> getEncumbranceDetails(String organizationPartyId, Map<String, String> accountingTags) throws RepositoryException;

    /**
     * Gets the total encumbered value subject to the conditions, same specifications as getEncumbranceDetails.
     *
     * @param organizationPartyId company identifier
     * @param accountingTags accountingTags; null for all the encumbrance details for all accounting tags; Map of tag -> NULL_TAG for tags which are null (ie, not set)
     * @param asOfDate desired snapshot date
     * @return Total value as add up of EncumberedDetail.encemberedValue.
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal getTotalEncumberedValue(String organizationPartyId, Map<String, String> accountingTags, Timestamp asOfDate) throws RepositoryException;
}
