/*
 * Copyright (c) 2007 - 2009 Open Source Strategies, Inc.
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
