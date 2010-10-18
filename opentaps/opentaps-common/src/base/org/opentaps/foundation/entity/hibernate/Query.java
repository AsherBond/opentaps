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

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.Type;
import org.ofbiz.entity.util.EntityCrypto;
import org.opentaps.foundation.entity.Entity;

/**
 * Opentaps Query which wraps the org.hibernate.Query, With the following
 * differences:
 * <ul>
 * <li>when call the setString method, we will decrypt field value automatically.</li>
 * </ul>
 */
public class Query implements org.hibernate.Query {
    private org.hibernate.Query hibernateQuery;
    // the parameters which need encrypt
    private List<String> encryptParameters;
    //crypt control
    private EntityCrypto crypto;
    // entity name
    private String entityName;
    private static final String MODULE = Query.class.getName();

    /**
     * Query constructor.
     *
     * @param hibernateQuery a <code>org.hibernate.Query</code> object.
     * @param entityName a <code>String<String></code> object.
     * @param encryptParameters a <code>List<String></code> object.
     * @param crypto a <code>EntityCrypto</code> object.
     */
    public Query(org.hibernate.Query hibernateQuery, String entityName, List<String> encryptParameters, EntityCrypto crypto) {
        this.hibernateQuery = hibernateQuery;
        this.encryptParameters = encryptParameters;
        this.crypto = crypto;
        this.entityName = entityName;
    }

    /**
     * crypt the encrypt parameter.
     *
     * @param parameterName a <code>String</code> value
     * @param parameterValue a <code>String<String></code> value
     * @return crypt value
     */
    private String encryptParameter(String parameterName, String parameterValue) {
        return HibernateUtil.getCryptoParameter(entityName, encryptParameters, crypto, parameterName, parameterValue);
    }

    /**
     * crypt the encrypt parameter.
     *
     * @param parameterName a <code>String</code> value
     * @param parameterValue a <code>Object<String></code> value
     * @return crypt value
     */
    private Object encryptParameter(String parameterName, Object parameterValue) {
        return HibernateUtil.getCryptoParameter(entityName, encryptParameters, crypto, parameterName, parameterValue);
    }

    public int executeUpdate() throws HibernateException {

        return hibernateQuery.executeUpdate();
    }

    public String[] getNamedParameters() throws HibernateException {

        return hibernateQuery.getNamedParameters();
    }

    public String getQueryString() {

        return hibernateQuery.getQueryString();
    }

    public String[] getReturnAliases() throws HibernateException {

        return hibernateQuery.getReturnAliases();
    }

    public Type[] getReturnTypes() throws HibernateException {

        return hibernateQuery.getReturnTypes();
    }

    public Iterator iterate() throws HibernateException {

        return hibernateQuery.iterate();
    }

    public List list() throws HibernateException {
        List<Entity> list = hibernateQuery.list();
        //decrypt all return entities
        if (entityName != null && encryptParameters.size() > 0) {
            for (Entity entity : list) {
                HibernateUtil.decryptField(entityName, encryptParameters, crypto, entity);
            }
        }
        return list;
    }

    public ScrollableResults scroll() throws HibernateException {

        return hibernateQuery.scroll();
    }

    public ScrollableResults scroll(ScrollMode paramScrollMode)
            throws HibernateException {

        return hibernateQuery.scroll(paramScrollMode);
    }

    public org.hibernate.Query setBigDecimal(int paramInt,
            BigDecimal paramBigDecimal) {

        return hibernateQuery.setBigDecimal(paramInt, paramBigDecimal);
    }

    public org.hibernate.Query setBigDecimal(String paramString,
            BigDecimal paramBigDecimal) {

        return hibernateQuery.setBigDecimal(paramString, paramBigDecimal);
    }

    public org.hibernate.Query setBigInteger(int paramInt,
            BigInteger paramBigInteger) {

        return hibernateQuery.setBigInteger(paramInt, paramBigInteger);
    }

    public org.hibernate.Query setBigInteger(String paramString,
            BigInteger paramBigInteger) {

        return hibernateQuery.setBigInteger(paramString, paramBigInteger);
    }

    public org.hibernate.Query setBinary(int paramInt, byte[] paramArrayOfByte) {

        return hibernateQuery.setBinary(paramInt, paramArrayOfByte);
    }

    public org.hibernate.Query setBinary(String paramString,
            byte[] paramArrayOfByte) {

        return hibernateQuery.setBinary(paramString, paramArrayOfByte);
    }

    public org.hibernate.Query setBoolean(int paramInt, boolean paramBoolean) {

        return hibernateQuery.setBoolean(paramInt, paramBoolean);
    }

    public org.hibernate.Query setBoolean(String paramString,
            boolean paramBoolean) {

        return hibernateQuery.setBoolean(paramString, paramBoolean);
    }

    public org.hibernate.Query setByte(int paramInt, byte paramByte) {

        return hibernateQuery.setByte(paramInt, paramByte);
    }

    public org.hibernate.Query setByte(String paramString, byte paramByte) {

        return hibernateQuery.setByte(paramString, paramByte);
    }

    public org.hibernate.Query setCacheMode(CacheMode paramCacheMode) {

        return hibernateQuery.setCacheMode(paramCacheMode);
    }

    public org.hibernate.Query setCacheRegion(String paramString) {

        return hibernateQuery.setCacheRegion(paramString);
    }

    public org.hibernate.Query setCacheable(boolean paramBoolean) {

        return hibernateQuery.setCacheable(paramBoolean);
    }

    public org.hibernate.Query setCalendar(int paramInt, Calendar paramCalendar) {

        return hibernateQuery.setCalendar(paramInt, paramCalendar);
    }

    public org.hibernate.Query setCalendar(String paramString,
            Calendar paramCalendar) {

        return hibernateQuery.setCalendar(paramString, paramCalendar);
    }

    public org.hibernate.Query setCalendarDate(int paramInt,
            Calendar paramCalendar) {

        return hibernateQuery.setCalendarDate(paramInt, paramCalendar);
    }

    public org.hibernate.Query setCalendarDate(String paramString,
            Calendar paramCalendar) {

        return hibernateQuery.setCalendarDate(paramString, paramCalendar);
    }

    public org.hibernate.Query setCharacter(int paramInt, char paramChar) {

        return hibernateQuery.setCharacter(paramInt, paramChar);
    }

    public org.hibernate.Query setCharacter(String paramString, char paramChar) {

        return hibernateQuery.setCharacter(paramString, paramChar);
    }

    public org.hibernate.Query setComment(String paramString) {

        return hibernateQuery.setComment(paramString);
    }

    public org.hibernate.Query setDate(int paramInt, Date paramDate) {

        return hibernateQuery.setDate(paramInt, paramDate);
    }

    public org.hibernate.Query setDate(String paramString, Date paramDate) {

        return hibernateQuery.setDate(paramString, paramDate);
    }

    public org.hibernate.Query setDouble(int paramInt, double paramDouble) {

        return hibernateQuery.setDouble(paramInt, paramDouble);
    }

    public org.hibernate.Query setDouble(String paramString, double paramDouble) {

        return hibernateQuery.setDouble(paramString, paramDouble);
    }

    public org.hibernate.Query setEntity(int paramInt, Object paramObject) {

        return hibernateQuery.setEntity(paramInt, paramObject);
    }

    public org.hibernate.Query setEntity(String paramString, Object paramObject) {

        return hibernateQuery.setEntity(paramString, paramObject);
    }

    public org.hibernate.Query setFetchSize(int paramInt) {

        return hibernateQuery.setFetchSize(paramInt);
    }

    public org.hibernate.Query setFirstResult(int paramInt) {

        return hibernateQuery.setFirstResult(paramInt);
    }

    public org.hibernate.Query setFloat(int paramInt, float paramFloat) {

        return hibernateQuery.setFloat(paramInt, paramFloat);
    }

    public org.hibernate.Query setFloat(String paramString, float paramFloat) {

        return hibernateQuery.setFloat(paramString, paramFloat);
    }

    public org.hibernate.Query setFlushMode(FlushMode paramFlushMode) {

        return hibernateQuery.setFlushMode(paramFlushMode);
    }

    public org.hibernate.Query setInteger(int paramInt1, int paramInt2) {

        return hibernateQuery.setInteger(paramInt1, paramInt2);
    }

    public org.hibernate.Query setInteger(String paramString, int paramInt) {

        return hibernateQuery.setInteger(paramString, paramInt);
    }

    public org.hibernate.Query setLocale(int paramInt, Locale paramLocale) {

        return hibernateQuery.setLocale(paramInt, paramLocale);
    }

    public org.hibernate.Query setLocale(String paramString, Locale paramLocale) {

        return hibernateQuery.setLocale(paramString, paramLocale);
    }

    public org.hibernate.Query setLockMode(String paramString,
            LockMode paramLockMode) {

        return hibernateQuery.setLockMode(paramString, paramLockMode);
    }

    public org.hibernate.Query setLong(int paramInt, long paramLong) {

        return hibernateQuery.setLong(paramInt, paramLong);
    }

    public org.hibernate.Query setLong(String paramString, long paramLong) {

        return hibernateQuery.setLong(paramString, paramLong);
    }

    public org.hibernate.Query setMaxResults(int paramInt) {

        return hibernateQuery.setMaxResults(paramInt);
    }

    public org.hibernate.Query setParameter(int paramInt, Object paramObject)
            throws HibernateException {
        return hibernateQuery.setParameter(paramInt, encryptParameter("" + paramInt, paramObject));
    }

    public org.hibernate.Query setParameter(String paramString,
            Object paramObject) throws HibernateException {
        return hibernateQuery.setParameter(paramString, encryptParameter(paramString, paramObject));
    }

    public org.hibernate.Query setParameter(int paramInt, Object paramObject,
            Type paramType) {

        return hibernateQuery.setParameter(paramInt, encryptParameter("" + paramInt, paramObject), paramType);
    }

    public org.hibernate.Query setParameter(String paramString,
            Object paramObject, Type paramType) {

        return hibernateQuery.setParameter(paramString, encryptParameter(paramString, paramObject), paramType);
    }

    public org.hibernate.Query setParameterList(String paramString,
            Collection paramCollection) throws HibernateException {

        return hibernateQuery.setParameterList(paramString, paramCollection);
    }

    public org.hibernate.Query setParameterList(String paramString,
            Object[] paramArrayOfObject) throws HibernateException {

        return hibernateQuery.setParameterList(paramString, paramArrayOfObject);
    }

    public org.hibernate.Query setParameterList(String paramString,
            Collection paramCollection, Type paramType)
            throws HibernateException {

        return hibernateQuery.setParameterList(paramString, paramCollection, paramType);
    }

    public org.hibernate.Query setParameterList(String paramString,
            Object[] paramArrayOfObject, Type paramType)
            throws HibernateException {

        return hibernateQuery.setParameterList(paramString, paramArrayOfObject, paramType);
    }

    public org.hibernate.Query setParameters(Object[] paramArrayOfObject,
            Type[] paramArrayOfType) throws HibernateException {

        return hibernateQuery.setParameters(paramArrayOfObject, paramArrayOfType);
    }

    public org.hibernate.Query setProperties(Object paramObject)
            throws HibernateException {

        return hibernateQuery.setProperties(paramObject);
    }

    public org.hibernate.Query setProperties(Map paramMap)
            throws HibernateException {

        return hibernateQuery.setProperties(paramMap);
    }

    public org.hibernate.Query setReadOnly(boolean paramBoolean) {

        return hibernateQuery.setReadOnly(paramBoolean);
    }

    public org.hibernate.Query setResultTransformer(
            ResultTransformer paramResultTransformer) {

        return hibernateQuery.setResultTransformer(paramResultTransformer);
    }

    public org.hibernate.Query setSerializable(int paramInt,
            Serializable paramSerializable) {

        return hibernateQuery.setSerializable(paramInt, paramSerializable);
    }

    public org.hibernate.Query setSerializable(String paramString,
            Serializable paramSerializable) {

        return hibernateQuery.setSerializable(paramString, paramSerializable);
    }

    public org.hibernate.Query setShort(int paramInt, short paramShort) {

        return hibernateQuery.setShort(paramInt, paramShort);
    }

    public org.hibernate.Query setShort(String paramString, short paramShort) {

        return hibernateQuery.setShort(paramString, paramShort);
    }

    public org.hibernate.Query setString(int paramInt, String paramString) {

        return hibernateQuery.setString(paramInt, encryptParameter("" + paramInt, paramString));
    }

    public org.hibernate.Query setString(String paramString1,
            String paramString2) {

        return hibernateQuery.setString(paramString1, encryptParameter(paramString1, paramString2));
    }

    public org.hibernate.Query setText(int paramInt, String paramString) {

        return hibernateQuery.setText(paramInt, encryptParameter("" + paramInt, paramString));
    }

    public org.hibernate.Query setText(String paramString1, String paramString2) {

        return hibernateQuery.setText(paramString1, encryptParameter(paramString1, paramString2));
    }

    public org.hibernate.Query setTime(int paramInt, Date paramDate) {

        return hibernateQuery.setTime(paramInt, paramDate);
    }

    public org.hibernate.Query setTime(String paramString, Date paramDate) {

        return hibernateQuery.setTime(paramString, paramDate);
    }

    public org.hibernate.Query setTimeout(int paramInt) {

        return hibernateQuery.setTimeout(paramInt);
    }

    public org.hibernate.Query setTimestamp(int paramInt, Date paramDate) {

        return hibernateQuery.setTimestamp(paramInt, paramDate);
    }

    public org.hibernate.Query setTimestamp(String paramString, Date paramDate) {

        return hibernateQuery.setTimestamp(paramString, paramDate);
    }

    public Object uniqueResult() throws HibernateException {

        return hibernateQuery.uniqueResult();
    }

}
