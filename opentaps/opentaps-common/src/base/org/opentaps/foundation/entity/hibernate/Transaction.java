package org.opentaps.foundation.entity.hibernate;

import javax.transaction.Synchronization;

import org.hibernate.HibernateException;

public class Transaction  implements org.hibernate.Transaction {
    private static final String MODULE = Transaction.class.getName();
    private org.hibernate.Transaction hibernateTransaction = null;
    
    /**
     * Transaction constructor.
     *
     * @param hibernateSession a <code>org.hibernate.Session</code> object.
     * @param delegator a <code>Delegator</code> object.
     */
    public Transaction(org.hibernate.Transaction hibernateTransaction) {
        this.hibernateTransaction = hibernateTransaction;
    }
    
	public void begin() throws HibernateException {
		hibernateTransaction.begin();
	}

	public void commit() throws HibernateException {
		try {
			hibernateTransaction.commit();
		} catch (HibernateException e) {
            if (hibernateTransaction != null && hibernateTransaction.isActive()) {
            	hibernateTransaction.rollback();
            }
            throw new HibernateException(HibernateUtil.getHibernateExceptionCause(e));
        }
	}

	public boolean isActive() throws HibernateException {
		return hibernateTransaction.isActive();
	}

	public void registerSynchronization(Synchronization sync)
			throws HibernateException {
		hibernateTransaction.registerSynchronization(sync);
	}

	public void rollback() throws HibernateException {
		hibernateTransaction.rollback();
	}

	public void setTimeout(int seconds) {
		hibernateTransaction.setTimeout(seconds);
	}

	public boolean wasCommitted() throws HibernateException {
		return hibernateTransaction.wasCommitted();
	}

	public boolean wasRolledBack() throws HibernateException {
		return hibernateTransaction.wasRolledBack();
	}
}
