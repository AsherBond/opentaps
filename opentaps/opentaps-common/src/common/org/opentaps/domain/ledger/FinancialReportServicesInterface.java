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

import java.sql.Timestamp;

import org.opentaps.foundation.service.ServiceException;
import org.opentaps.foundation.service.ServiceInterface;


public interface FinancialReportServicesInterface extends ServiceInterface {

    /**
     * Internal organization, required input attribute.
     * @param organizationPartyId the company identifier
     */
    public void setOrganizationPartyId(String organizationPartyId);

    /**
     * <code>Timestamp</code> to create snapshot of encumbrances, optional input attribute.
     * @param snapshotTime Timestamp value
     */
    public void setSnapshotDatetime(Timestamp snapshotDatetime);

    /**
     * Beginning of period to analyze accounting transactions for encumbrances, optional input attribute.
     * @param startDatetime
     */
    public void setStartDatetime(Timestamp startDatetime);

    /**
     * Brief comments.
     * @param comments Max 255 chars length string
     */
    public void setComments(String comments);

    /**
     * More extensive snapshot description.
     * @param description Any length string.
     */
    public void setDescription(String description);

    /**
     * Collects and put to GlAccountTransEntryFact entity summary data for comparison budget 
     * versus actual transactions and encumbrances in financial reports.</br>
     * Implements <code>financials.createGlAccountTransEntryFact</code> service. 
     * @throws ServiceException
     */
    public void createGlAccountTransEntryFact() throws ServiceException;

    /**
     * Implements financials.collectEncumbranceAndTransEntryFacts.
     * Wraps financials.createGlAccountTransEntryFact & createEncumbranceSnapshotAndDetail services.
     * @throws ServiceException
     */
    public void collectEncumbranceAndTransEntryFacts() throws ServiceException;
}