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
package org.opentaps.domain.billing.financials;

import org.opentaps.foundation.service.ServiceException;
import org.opentaps.foundation.service.ServiceInterface;

/**
 * POJO service which take a regular snapshot of customer/vendor/commission balances
 * so that we can track their outstanding balances over time. using the opentaps Service foundation
 * class.
 */
public interface AccountBalanceHistoryServiceInterface  extends ServiceInterface {
    /**
     * Service to create snapshot of customer/vendor/commission balances.
     * @throws ServiceException if an error occurs
     */
    public void captureAccountBalancesSnapshot() throws ServiceException;
}
