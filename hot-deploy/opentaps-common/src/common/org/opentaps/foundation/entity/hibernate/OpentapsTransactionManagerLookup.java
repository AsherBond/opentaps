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

import java.util.Properties;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.hibernate.HibernateException;
import org.hibernate.transaction.TransactionManagerLookup;
import org.ofbiz.entity.transaction.TransactionFactory;

/**
 * This is an implementation of the TransactionManagerLookup for transcation with hibernate.
 *
 */
public class OpentapsTransactionManagerLookup implements TransactionManagerLookup {

    /**
     * Get JTA Transaction Manager.
     *
     * @param props The configuration properties.
     * @return The JTA.
     *
     * @throws HibernateException Indicates problem locating.
     */
    public TransactionManager getTransactionManager(Properties props)
            throws HibernateException {
        TransactionManager manager = TransactionFactory.getTransactionManager();
        if (manager == null) {
            throw new HibernateException(
                    "Could not obtain Ofbiz transaction manager instance");
        }
        return manager;
    }

    /**
     * Return the JNDI namespace of the JTA for this platform or <tt>null</tt>;
     * optional operation.
     *
     * @return The JNDI namespace where we can locate the
     */
    public String getUserTransactionName() {
        return "java:comp/UserTransaction";
    }

    /**
     * Determine an identifier for the given transaction appropriate for use in caching/lookup usages.
     * <p/>
     * Generally speaking the transaction itself will be returned here.  This method was added specifically
     * for use in WebSphere and other unfriendly JEE containers (although WebSphere is still the only known
     * such brain-dead, sales-driven impl).
     *
     * @param transaction The transaction to be identified.
     * @return An appropropriate identifier
     */
    public Object getTransactionIdentifier(Transaction transaction) {
        // TODO Auto-generated method stub
        return null;
    }


}