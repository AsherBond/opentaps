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
package org.opentaps.aspect.secas;

import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.DelegatorFactory;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericPK;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.opentaps.foundation.entity.hibernate.HibernateUtil;
import org.opentaps.foundation.infrastructure.Infrastructure;

/**
 * The Aspects for clear hibernate 2nd cache on ofbiz's engine CURD.
 */
public class EvictHibernateCacheAspects {

    private static final String MODULE = EvictHibernateCacheAspects.class.getName();
    private static String DELEGATOR_NAME = "default";

    /**
     * @Expression execution(* org.ofbiz.entity.Delegator+.clearAllCaches(..)) && args(distribute)
     */
    void pointcut1(boolean distribute) { }

    /**
     * @After pointcut1(distribute)
     */
    public void clearAllCaches(boolean distribute) {
        Debug.logVerbose("run clearAllCaches(boolean distribute)", MODULE);
        // clear hibernate cache
        evictHibernateCache(null, null, null);
    }

    /**
     * @Expression execution(* org.ofbiz.entity.Delegator+.clearCacheLine(..)) && args(entityName)
     */
    void pointcut2(String entityName) { }

    /**
     * @After pointcut2(entityName)
     */
    public void clearCacheLine(String entityName) {
        Debug.logVerbose("run clearCacheLine(entityName), entityName = " + entityName, MODULE);
        // clear hibernate cache
        evictHibernateCache(entityName, null, null);
    }


    /**
     * @Expression execution(* org.ofbiz.entity.Delegator+.clearCacheLine(String, Map)) && args(entityName, fields)
     */
    void pointcut3(String entityName, Map fields) { }

    /**
     * @After pointcut3(entityName, fields)
     */
    public void clearCacheLine(String entityName, Map fields) {
        Debug.logVerbose("run clearCacheLine(entityName, Map<String, ? extends Object> fields), entityName = " + entityName + ", fields = " + fields, MODULE);
        // clear hibernate cache
        evictHibernateCache(entityName, null, null);
    }

    /**
     * @Expression execution(* org.ofbiz.entity.Delegator+.clearCacheLineFlexible(org.ofbiz.entity.GenericEntity, boolean)) && args(dummyPK, distribute)
     */
    void pointcut4(GenericEntity dummyPK, boolean distribute) { }


    /**
     * @After pointcut4(dummyPK, distribute)
     */
    public void clearCacheLineFlexible(GenericEntity dummyPK, boolean distribute) {
        Debug.logVerbose("run clearCacheLineFlexible(GenericEntity dummyPK, boolean distribute), dummyPK = " + dummyPK , MODULE);
        // clear hibernate cache
        evictHibernateCache(dummyPK.getEntityName(), dummyPK, null);

    }

    /**
     * @Expression execution(* org.ofbiz.entity.Delegator+.clearCacheLineByCondition(String, org.ofbiz.entity.condition.EntityCondition, boolean)) && args(entityName, condition, distribute)
     */
    void pointcut5(String entityName, EntityCondition condition, boolean distribute) { }


    /**
     * @After pointcut5(entityName, condition, distribute)
     */
    public void clearCacheLineByCondition(String entityName, EntityCondition condition, boolean distribute) {
        Debug.logVerbose("run clearCacheLineByCondition(String entityName, EntityCondition condition, boolean distribute), entityName = " + entityName + ", condition = " + condition , MODULE);
        // clear hibernate cache
        evictHibernateCache(entityName, null, condition);
    }

    /**
     * @Expression execution(* org.ofbiz.entity.Delegator+.clearCacheLine(org.ofbiz.entity.GenericPK, boolean)) && args(primaryKey, distribute)
     */
    void pointcut6(GenericPK primaryKey, boolean distribute) { }

    /**
     * @After pointcut6(primaryKey, distribute)
     */
    public void clearCacheLine(GenericPK primaryKey, boolean distribute) {
        Debug.logVerbose("run clearCacheLine(GenericPK primaryKey, boolean distribute), primaryKey = " + primaryKey , MODULE);
        // clear hibernate cache
        evictHibernateCache(primaryKey.getEntityName(), primaryKey, null);
    }

    /**
     * @Expression execution(* org.ofbiz.entity.Delegator+.clearCacheLine(org.ofbiz.entity.GenericValue, boolean)) && args(value, distribute)
     */
    void pointcut7(GenericValue value, boolean distribute) { }

    /**
     * @After pointcut7(value, distribute)
     */
    public void clearCacheLine(GenericValue value, boolean distribute) {
        Debug.logVerbose("run clearCacheLine(GenericValue value, boolean distribute), value = " + value , MODULE);
        // clear hibernate cache
        evictHibernateCache(value.getEntityName(), value.getPrimaryKey(), null);
    }

    // Note: transaction isolation is not needed as ''evict'' occurs outside of any transaction
    private void evictHibernateCache(String entityName, GenericEntity pk, EntityCondition condition) {
        try {
            // perform the cache clearing
            Infrastructure infrastructure = new Infrastructure(getDefaultDelegator());
            if (entityName == null) {
                infrastructure.evictHibernateCache();
            } else if (condition != null) {
                List<GenericValue> removedEntities = infrastructure.getDelegator().findList(entityName, condition, null, null, null, false);
                for (GenericValue entity : removedEntities) {
                    infrastructure.evictHibernateCache(entityName, HibernateUtil.genericPkToEntityPk(entity.getPrimaryKey()));
                }
            } else if (pk != null) {
                infrastructure.evictHibernateCache(pk.getEntityName(), HibernateUtil.genericPkToEntityPk(pk));
            } else {
                infrastructure.evictHibernateCache(entityName);
            }
        } catch (Exception e) {
            Debug.logError(e, MODULE);
        }
    }

    /**
     * The method get Delegator instance.
     * @return a <code>Delegator</code> value
     */
    public Delegator getDefaultDelegator() {
        Delegator delegator = DelegatorFactory.getDelegator(DELEGATOR_NAME);
        return delegator;
    }
}
