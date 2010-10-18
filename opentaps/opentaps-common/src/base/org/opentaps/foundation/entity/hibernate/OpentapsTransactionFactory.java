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
package org.opentaps.foundation.entity.hibernate;

import javax.transaction.UserTransaction;

import org.hibernate.transaction.JTATransactionFactory;
import org.ofbiz.entity.transaction.TransactionFactory;

/**
 * This is an implementation of the JTATransactionFactory for transcation factory with hibernate.
 *
 */
public class OpentapsTransactionFactory extends JTATransactionFactory {
    /**
     * because default ofbiz not use jndi to get JTA, so overwrite this function to get JTA from ofbiz TransactionFactory.
     * @return a <code>UserTransaction</code> value
     */
    protected UserTransaction getUserTransaction() {
        return TransactionFactory.getUserTransaction();
    }
}
