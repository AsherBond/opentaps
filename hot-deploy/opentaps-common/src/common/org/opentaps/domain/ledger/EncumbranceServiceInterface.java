/*
 * Copyright (c) 2009 Open Source Strategies, Inc.
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
package org.opentaps.domain.ledger;

import java.sql.Timestamp;

import org.opentaps.foundation.service.ServiceException;
import org.opentaps.foundation.service.ServiceInterface;

/**
 * Interface for encumbrance related entities.
 */
public interface EncumbranceServiceInterface extends ServiceInterface {

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
     * This service goes through active purchase orders and posted transactions of the gl account type ENCUMBRANCE
     * and creates an encumbrance snapshot with all the detail records.
     * @throws ServiceException
     */
    public void createEncumbranceSnapshotAndDetail() throws ServiceException;

}