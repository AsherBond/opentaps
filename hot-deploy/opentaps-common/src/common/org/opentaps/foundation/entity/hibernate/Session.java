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

import java.io.Serializable;
import java.sql.Connection;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.transaction.NotSupportedException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.Filter;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.ReplicationMode;
import org.hibernate.SQLQuery;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.jdbc.Work;
import org.hibernate.stat.SessionStatistics;
import org.hibernate.transform.Transformers;
import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionFactory;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.EntityCrypto;
import org.opentaps.foundation.entity.Entity;

/**
 * Opentaps Session which wraps the org.hibernate.Session, With the following differences:
 * <ul>
 * <li>when the session is closed, the JDBC connection is also automatically
 * closed</li>
 * <li>when a Query is created, this Session will check if the query is on an
 * entity engine view entity and construct the Query from native SQL first</li>
 * </ul>
 */
public class Session implements org.hibernate.Session {
    private static final String MODULE = Session.class.getName();
    // JDBC fetch size
    public final static int FETCH_SIZE = 100;

    private org.hibernate.Session hibernateSession;
    private GenericDelegator delegator;
    // encrypt control
    private EntityCrypto crypto;


    /**
     * Session constructor.
     *
     * @param hibernateSession a <code>org.hibernate.Session</code> object.
     * @param delegator a <code>GenericDelegator</code> object.
     */
    public Session(org.hibernate.Session hibernateSession, GenericDelegator delegator) {
        this.hibernateSession = hibernateSession;
        this.delegator = delegator;
        this.crypto = new EntityCrypto(delegator);
    }

    /**
     * Begin JTA UserTranscation.
     * 
     * @throws HibernateException
     *             if an error occurs
     * @return boolean
     */
    public boolean begin() throws HibernateException {

        try {
            return TransactionUtil.begin();
        } catch (GenericTransactionException e) {
            // TODO Auto-generated catch block
            Debug.logError(e, MODULE);
            throw new HibernateException(
                    "cause GenericTransactionException in call TransactionUtil.begin().");
        }
    }

    /**
     * Commit JTA UserTranscation.
     * 
     * @param beganTransaction
     *            an <code>boolean</code> value
     * @throws HibernateException
     *             if an error occurs
     */
    public void commit(boolean beganTransaction) throws HibernateException {

        try {
            TransactionUtil.commit(beganTransaction);
        } catch (GenericTransactionException e) {
            Debug.logError(e, MODULE);
            throw new HibernateException(
                    "cause GenericTransactionException in call TransactionUtil.commit(boolean).");
        }
    }

    /**
     * Get JTA UserTranscation.  The JTA UserTransaction is the one from the transaction manager defined in the ofbiz
     * entityengine.xml, such as:
     * <pre>
     *  <transaction-factory class="org.ofbiz.geronimo.GeronimoTransactionFactory"/>
     * </pre>
     * 
     * @throws HibernateException
     *             if an error occurs
     * @return UserTransaction
     */
    public UserTransaction beginUserTransaction() throws HibernateException {

        UserTransaction userTransaction = TransactionFactory
                .getUserTransaction();
        try {
            Debug.logVerbose("[Session.beginUserTransaction] current UserTransaction status : " + TransactionUtil.getTransactionStateString(userTransaction.getStatus()), MODULE);
            //if current status of jta is STATUS_ACTIVE or STATUS_MARKED_ROLLBACK, we needn't begin it again.
            if (userTransaction.getStatus() != Status.STATUS_ACTIVE && userTransaction.getStatus() != Status.STATUS_MARKED_ROLLBACK) {
                userTransaction.begin();
            }
        } catch (NotSupportedException e) {
            Debug.logError(e, MODULE);
            throw new HibernateException(
                    "cause NotSupportedException in call TransactionFactory.getUserTransaction().");
        } catch (SystemException e) {
            Debug.logError(e, MODULE);
            throw new HibernateException(
                    "cause SystemException in call TransactionFactory.getUserTransaction().");
        }
        return userTransaction;
    }

    public Transaction beginTransaction() throws HibernateException {

        return hibernateSession.beginTransaction();
    }

    public void cancelQuery() throws HibernateException {

        hibernateSession.cancelQuery();
    }

    public void clear() {

        hibernateSession.clear();
    }

    /**
     * Close the Hibernate Session and the Connection
     * 
     * @return
     * @throws HibernateException
     */
    public Connection close() throws HibernateException {
        Connection con = hibernateSession.close();
        return con;
    }

    public Connection connection() throws HibernateException {

        return hibernateSession.connection();
    }

    public boolean contains(Object object) {

        return hibernateSession.contains(object);
    }

    public Criteria createCriteria(Class persistentClass) {

        return hibernateSession.createCriteria(persistentClass);
    }

    public Criteria createCriteria(String entityName) {

        return hibernateSession.createCriteria(entityName);
    }

    public Criteria createCriteria(Class persistentClass, String alias) {

        return hibernateSession.createCriteria(persistentClass, alias);
    }

    public Criteria createCriteria(String entityName, String alias) {

        return hibernateSession.createCriteria(entityName, alias);
    }

    public Query createFilter(Object collection, String queryString)
            throws HibernateException {
        org.hibernate.Query hibernateQuery = hibernateSession.createFilter(collection, queryString);
        Query query = new Query(hibernateQuery, HibernateUtil.retrieveSimpleClassName(queryString), HibernateUtil.getEncryptParametersByQueryString(queryString, delegator), crypto);
        return query;
    }



    /**
     * Creates a Hibernate Query from the queryString. Check if the queryString
     * is for an entity which is an entity engine view-entity and if it is,
     * transfrom the HQL to native SQL first, then return it as the Query. If
     * not, just return the Query from the queryString.
     *
     * @param queryString a <code>String</code> value.
     * @return a <code>Query</code> value
     * @throws HibernateException if an error occurs
     */
    public Query createQuery(String queryString) throws HibernateException {
        try {
            // Now check if the entity is a view-entity
            Entity entity = HibernateUtil.getEntityInstanceByQueryString(queryString);
            // encryptFields of this GenericValue
            List<String> encryptParameters = HibernateUtil.getEncryptParametersByQueryString(queryString, delegator);
            if (entity.isView()) {
                // if it is a view-entity, we should transform hql to native sql
                // query.
                String nameQueryString = getNamedQuery(
                        "select" + entity.getClass().getSimpleName() + "s")
                        .getQueryString();
                String sqlString = nameQueryString
                        + " "
                        + HibernateUtil.hqlToSql(queryString, HibernateUtil.retrieveClassName(queryString),
                                HibernateUtil.retrieveClassAlias(queryString),
                                entity.fieldMapColumns.get(entity.getClass()
                                        .getSimpleName()));
                Debug.logInfo("Querying [" + entity.getBaseEntityName()
                        + "] with query [" + sqlString + "]", MODULE);
                org.hibernate.Query hibernateQuery = hibernateSession.createSQLQuery(sqlString);
                // set result transformer to change result to the class of entity
                hibernateQuery.setResultTransformer(OpentapsTransformer.aliasToBean(entity
                        .getClass()));
                Query query = new Query(hibernateQuery, entity.getBaseEntityName(), encryptParameters, crypto);
                return query;
            } else {
                // normal hql, should create a hibernate query and return it.
                org.hibernate.Query hibernateQuery =  hibernateSession.createQuery(queryString);
                Query query = new Query(hibernateQuery, entity.getBaseEntityName(), encryptParameters, crypto);
                return query;
            }
        } catch (InstantiationException e) {
            Debug.logError(e, MODULE);
            throw new HibernateException(e.getMessage());
        } catch (IllegalAccessException e) {
            Debug.logError(e, MODULE);
            throw new HibernateException(e.getMessage());
        } catch (ClassNotFoundException e) {
            Debug.logError(e, MODULE);
            throw new HibernateException(e.getMessage());
        }
    }

    public SQLQuery createSQLQuery(String queryString)
            throws HibernateException {

        return hibernateSession.createSQLQuery(queryString);
    }

    public void delete(Object object) throws HibernateException {

        hibernateSession.delete(object);
    }

    public void delete(String entityName, Object object)
            throws HibernateException {

        hibernateSession.delete(entityName, object);
    }

    public void disableFilter(String filterName) {

        hibernateSession.disableFilter(filterName);
    }

    /**
     * disconnect the Connection of this Session
     * 
     * @return
     * @throws HibernateException
     */
    public Connection disconnect() throws HibernateException {
        // Do not Re-factor to share code with close(): they are different
        // methods inside of hibernate
        Connection con = hibernateSession.disconnect();
        return con;
    }

    public void doWork(Work work) throws HibernateException {

        hibernateSession.doWork(work);
    }

    public Filter enableFilter(String filterName) {

        return hibernateSession.enableFilter(filterName);
    }

    public void evict(Object object) throws HibernateException {

        hibernateSession.equals(object);
    }

    public void flush() throws HibernateException {

        hibernateSession.flush();
    }

    public Object get(Class clazz, Serializable id) throws HibernateException {

        return hibernateSession.get(clazz, id);
    }

    public Object get(String entityName, Serializable id)
            throws HibernateException {

        return hibernateSession.get(entityName, id);
    }

    public Object get(Class clazz, Serializable id, LockMode lockMode)
            throws HibernateException {

        return hibernateSession.get(clazz, id, lockMode);
    }

    public Object get(String entityName, Serializable id, LockMode lockMode)
            throws HibernateException {

        return hibernateSession.get(entityName, id, lockMode);
    }

    public CacheMode getCacheMode() {

        return hibernateSession.getCacheMode();
    }

    public LockMode getCurrentLockMode(Object object) throws HibernateException {

        return hibernateSession.getCurrentLockMode(object);
    }

    public Filter getEnabledFilter(String filterName) {

        return hibernateSession.getEnabledFilter(filterName);
    }

    public EntityMode getEntityMode() {

        return hibernateSession.getEntityMode();
    }

    public String getEntityName(Object object) throws HibernateException {

        return hibernateSession.getEntityName(object);
    }

    public FlushMode getFlushMode() {

        return hibernateSession.getFlushMode();
    }

    public Serializable getIdentifier(Object object) throws HibernateException {

        return hibernateSession.getIdentifier(object);
    }

    public Query getNamedQuery(String queryName) throws HibernateException {
        org.hibernate.Query hibernateQuery =  hibernateSession.getNamedQuery(queryName);
        Query query = new Query(hibernateQuery, null, new ArrayList(), crypto);
        return query;
    }

    public org.hibernate.Session getSession(EntityMode entityMode) {

        return hibernateSession.getSession(entityMode);
    }

    public SessionFactory getSessionFactory() {

        return hibernateSession.getSessionFactory();
    }

    public SessionStatistics getStatistics() {

        return hibernateSession.getStatistics();
    }

    public Transaction getTransaction() {

        return hibernateSession.getTransaction();
    }

    public boolean isConnected() {

        return hibernateSession.isConnected();
    }

    public boolean isDirty() throws HibernateException {

        return hibernateSession.isDirty();
    }

    public boolean isOpen() {

        return hibernateSession.isOpen();
    }

    public Object load(Class theClass, Serializable id)
            throws HibernateException {
        List<String> encryptFields = HibernateUtil.getEncryptFieldsByClassName(theClass.getCanonicalName(), delegator);
        Entity entity = (Entity) hibernateSession.load(theClass, id);
        HibernateUtil.decryptField(theClass.getSimpleName(), encryptFields, crypto, entity);
        return entity;
    }

    public Object load(String entityName, Serializable id)
            throws HibernateException {
        List<String> encryptFields = HibernateUtil.getEncryptFieldsByClassName(entityName, delegator);
        Entity entity = (Entity) hibernateSession.load(entityName, id);
        HibernateUtil.decryptField(entityName, encryptFields, crypto, entity);
        return entity;
    }

    public void load(Object object, Serializable id) throws HibernateException {
        List<String> encryptFields = HibernateUtil.getEncryptFieldsByClassName(object.getClass().getCanonicalName(), delegator);
        hibernateSession.load(object, id);
        Entity entity = (Entity) object;
        HibernateUtil.decryptField(object.getClass().getSimpleName(), encryptFields, crypto, entity);
    }

    public Object load(Class theClass, Serializable id, LockMode lockMode)
            throws HibernateException {
        List<String> encryptFields = HibernateUtil.getEncryptFieldsByClassName(theClass.getCanonicalName(), delegator);
        Entity entity = (Entity) hibernateSession.load(theClass, id, lockMode);
        HibernateUtil.decryptField(theClass.getSimpleName(), encryptFields, crypto, entity);
        return entity;
    }

    public Object load(String entityName, Serializable id, LockMode lockMode)
            throws HibernateException {
        List<String> encryptFields = HibernateUtil.getEncryptFieldsByClassName(entityName, delegator);
        Entity entity = (Entity) hibernateSession.load(entityName, id, lockMode);
        HibernateUtil.decryptField(entityName, encryptFields, crypto, entity);
        return entity;
    }

    public void lock(Object object, LockMode lockMode)
            throws HibernateException {

        hibernateSession.lock(object, lockMode);
    }

    public void lock(String entityName, Object object, LockMode lockMode)
            throws HibernateException {

        hibernateSession.lock(entityName, object, lockMode);
    }

    public Object merge(Object object) throws HibernateException {

        return hibernateSession.merge(object);
    }

    public Object merge(String entityName, Object object)
            throws HibernateException {

        return hibernateSession.merge(entityName, object);
    }

    public void persist(Object object) throws HibernateException {

        hibernateSession.persist(object);
    }

    public void persist(String entityName, Object object)
            throws HibernateException {

        hibernateSession.persist(entityName, object);
    }

    public void reconnect() throws HibernateException {

        hibernateSession.reconnect();
    }

    public void reconnect(Connection connection) throws HibernateException {

        hibernateSession.reconnect(connection);
    }

    public void refresh(Object object) throws HibernateException {

        hibernateSession.refresh(object);
    }

    public void refresh(Object object, LockMode lockMode)
            throws HibernateException {

        hibernateSession.refresh(object, lockMode);
    }

    public void replicate(Object object, ReplicationMode replicationMode)
            throws HibernateException {

        hibernateSession.replicate(object, replicationMode);
    }

    public void replicate(String entityName, Object object,
            ReplicationMode replicationMode) throws HibernateException {

        hibernateSession.replicate(entityName, object, replicationMode);
    }

    public Serializable save(Object object) throws HibernateException {

        return hibernateSession.save(object);
    }

    public Serializable save(String entityName, Object object)
            throws HibernateException {

        return hibernateSession.save(entityName, object);
    }

    public void saveOrUpdate(Object object) throws HibernateException {

        hibernateSession.saveOrUpdate(object);
    }

    public void saveOrUpdate(String entityName, Object object)
            throws HibernateException {

        hibernateSession.saveOrUpdate(entityName, object);
    }

    public void setCacheMode(CacheMode cacheMode) {

        hibernateSession.setCacheMode(cacheMode);
    }

    public void setFlushMode(FlushMode flushMode) {

        hibernateSession.setFlushMode(flushMode);
    }

    public void setReadOnly(Object entity, boolean readOnly) {

        hibernateSession.setReadOnly(entity, readOnly);
    }

    public void update(Object object) throws HibernateException {

        hibernateSession.update(object);
    }

    public void update(String entityName, Object object)
            throws HibernateException {

        hibernateSession.update(entityName, object);
    }

    /**
     * Get the next guaranteed unique seq id from the sequence with the given sequence name.
     *@param seqName The name of the sequence to get the next seq id from
     *@return next seq id for the given sequence name
     */
    public String getNextSeqId(String seqName) {
        //open new transcation to do this job
        org.hibernate.Session newSession = getSessionFactory().openSession();
        // change format to String.
        DecimalFormat df = new DecimalFormat("0000");
        String nextSeqId = df.format(HibernateUtil.getNextSeqId(newSession, seqName));
        Debug.logInfo("Generate seqId [" + nextSeqId + "] for " + seqName, MODULE);
        return nextSeqId;
    }

    /**
     * Gets the next incremental value for the entity sub-sequence identifier.
     * @param entity an <code>EntityInterface</code> value
     * @param sequenceFieldName the field representing the sub-sequence
     * @param numericPadding the length of the sequence string padded with 0
     * @param incrementBy the increment for the next sub-sequence compared to the highest found
     * @return a <code>String</code> value
     * @exception HibernateException if an error occurs
     */
    public String getNextSubSeqId(Entity entity, String sequenceFieldName, int numericPadding, int incrementBy) throws HibernateException {
        GenericValue entityGV;
        try {
            entityGV = HibernateUtil.entityToGenericValue(entity, delegator);
            delegator.setNextSubSeqId(entityGV, sequenceFieldName, numericPadding, incrementBy);
            return entityGV.getString(sequenceFieldName);
        } catch (GenericEntityException e) {
            throw new HibernateException(e);
        }
    }

    /**
     * Get the original hibernate session.
     * @return next seq id for the given sequence name
     */
    public org.hibernate.Session getHibernateSession() {
        return this.hibernateSession;
    }

}
