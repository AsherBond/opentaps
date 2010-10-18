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
