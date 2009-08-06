/*
 * Copyright (c) 2008 - 2009 Open Source Strategies, Inc.
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

import org.opentaps.foundation.service.ServiceException;
import org.opentaps.foundation.service.ServiceInterface;

/**
 * Interface for ledger services.
 */
public interface ManufacturingLedgerServiceInterface extends ServiceInterface {

    /**
     * Sets the <code>productionRunId</code> required input parameter.
     * @param productionRunId the productionRunId to set
     */
    public void setProductionRunId(String productionRunId);

    /**
     * Gets the ID of the accounting transaction created.
     * @return the ID of the accounting transaction
     */
    public String getAcctgTransId();

    /**
     * Service to post a production run variance, only used if the organization uses standard costing.
     * This is called after a production run is completed and posts the difference
     * between the production run cost and the value of the inventory items produced.
     * @throws ServiceException if an error occurs
     * @see #setProductionRunId required input parameter <code>productionRunId</code>
     * @see #getAcctgTransId optional output parameter <code>acctgTransId</code>
     */
    public void postProductionRunCostVarianceToGl() throws ServiceException;

}
