/*
 * Copyright (c) 2009 - 2009 Open Source Strategies, Inc.
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

import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntity;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericPK;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.service.GenericDispatcher;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;

/**
 * The Aspects for clear hibernate 2nd cache on ofbiz's engine CURD.
 */
public class EvdictHibernateCacheAspects {

    private static final String MODULE = EvdictHibernateCacheAspects.class.getName();
    private static String DELEGATOR_NAME = "default";
    private static String runAsUser = "system";

    /**
     * @Expression execution(* org.ofbiz.entity.GenericDelegator.clearAllCaches(..)) && args(distribute)
     */
    void pointcut1(boolean distribute) { }

    /**
     * @After pointcut1(distribute)
     */
    public void clearAllCaches(boolean distribute) {
        Debug.logVerbose("run clearAllCaches(boolean distribute)", MODULE);
        // clear hibernate cache
        Map<String, Object> inputParams = UtilMisc.<String, Object>toMap();
        try {
            GenericValue userLoginToRunAs = getDelegator().findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", runAsUser));
            if (userLoginToRunAs != null) {
                inputParams.put("userLogin", userLoginToRunAs);
            } else {
                return;
            }
            LocalDispatcher dispatcher = getDispatcher();
            dispatcher.runSync("opentaps.evictHibernateCache", inputParams, -1, true);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
    }

    /**
     * @Expression execution(* org.ofbiz.entity.GenericDelegator.clearCacheLine(..)) && args(entityName)
     */
    void pointcut2(String entityName) { }

    /**
     * @After pointcut2(entityName)
     */
    public void clearCacheLine(String entityName) {
        Debug.logVerbose("run clearCacheLine(entityName), entityName = " + entityName, MODULE);
        // clear hibernate cache
        Map<String, Object> inputParams = UtilMisc.<String, Object>toMap("entityName", entityName);
        try {
            GenericValue userLoginToRunAs = getDelegator().findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", runAsUser));
            if (userLoginToRunAs != null) {
                inputParams.put("userLogin", userLoginToRunAs);
            } else {
                return;
            }
            LocalDispatcher dispatcher = getDispatcher();
            dispatcher.runSync("opentaps.evictHibernateCache", inputParams, -1, true);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
    }


    /**
     * @Expression execution(* org.ofbiz.entity.GenericDelegator.clearCacheLine(String, Map)) && args(entityName, fields)
     */
    void pointcut3(String entityName, Map fields) { }

    /**
     * @After pointcut3(entityName, fields)
     */
    public void clearCacheLine(String entityName, Map fields) {
        Debug.logVerbose("run clearCacheLine(entityName, Map<String, ? extends Object> fields), entityName = " + entityName + ", fields = " + fields, MODULE);
        // clear hibernate cache
        Map<String, Object> inputParams = UtilMisc.<String, Object>toMap("entityName", entityName);
        try {
            GenericValue userLoginToRunAs = getDelegator().findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", runAsUser));
            if (userLoginToRunAs != null) {
                inputParams.put("userLogin", userLoginToRunAs);
            } else {
                return;
            }
            LocalDispatcher dispatcher = getDispatcher();
            dispatcher.runSync("opentaps.evictHibernateCache", inputParams, -1, true);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
    }

    /**
     * @Expression execution(* org.ofbiz.entity.GenericDelegator.clearCacheLineFlexible(org.ofbiz.entity.GenericEntity, boolean)) && args(dummyPK, distribute)
     */
    void pointcut4(GenericEntity dummyPK, boolean distribute) { }


    /**
     * @After pointcut4(dummyPK, distribute)
     */
    public void clearCacheLineFlexible(GenericEntity dummyPK, boolean distribute) {
        Debug.logVerbose("run clearCacheLineFlexible(GenericEntity dummyPK, boolean distribute), dummyPK = " + dummyPK , MODULE);
        // clear hibernate cache
        Map<String, Object> inputParams = UtilMisc.<String, Object>toMap("entityName", dummyPK.getEntityName(), "pk", dummyPK);
        try {
            GenericValue userLoginToRunAs = getDelegator().findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", runAsUser));
            if (userLoginToRunAs != null) {
                inputParams.put("userLogin", userLoginToRunAs);
            } else {
                return;
            }
            LocalDispatcher dispatcher = getDispatcher();
            dispatcher.runSync("opentaps.evictHibernateCache", inputParams, -1, true);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }

    }

    /**
     * @Expression execution(* org.ofbiz.entity.GenericDelegator.clearCacheLineByCondition(String, org.ofbiz.entity.condition.EntityCondition, boolean)) && args(entityName, condition, distribute)
     */
    void pointcut5(String entityName, EntityCondition condition, boolean distribute) { }


    /**
     * @After pointcut5(entityName, condition, distribute)
     */
    public void clearCacheLineByCondition(String entityName, EntityCondition condition, boolean distribute) {
        Debug.logVerbose("run clearCacheLineByCondition(String entityName, EntityCondition condition, boolean distribute), entityName = " + entityName + ", condition = " + condition , MODULE);
        // clear hibernate cache
        Map<String, Object> inputParams = UtilMisc.<String, Object>toMap("entityName", entityName, "condition", condition);
        try {
            GenericValue userLoginToRunAs = getDelegator().findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", runAsUser));
            if (userLoginToRunAs != null) {
                inputParams.put("userLogin", userLoginToRunAs);
            } else {
                return;
            }
            LocalDispatcher dispatcher = getDispatcher();
            dispatcher.runSync("opentaps.evictHibernateCache", inputParams, -1, true);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
    }

    /**
     * @Expression execution(* org.ofbiz.entity.GenericDelegator.clearCacheLine(org.ofbiz.entity.GenericPK, boolean)) && args(primaryKey, distribute)
     */
    void pointcut6(GenericPK primaryKey, boolean distribute) { }

    /**
     * @After pointcut6(primaryKey, distribute)
     */
    public void clearCacheLine(GenericPK primaryKey, boolean distribute) {
        Debug.logVerbose("run clearCacheLine(GenericPK primaryKey, boolean distribute), primaryKey = " + primaryKey , MODULE);
        // clear hibernate cache
        Map<String, Object> inputParams = UtilMisc.<String, Object>toMap("entityName", primaryKey.getEntityName(), "pk", primaryKey);
        try {
            GenericValue userLoginToRunAs = getDelegator().findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", runAsUser));
            if (userLoginToRunAs != null) {
                inputParams.put("userLogin", userLoginToRunAs);
            } else {
                return;
            }
            LocalDispatcher dispatcher = getDispatcher();
            dispatcher.runSync("opentaps.evictHibernateCache", inputParams, -1, true);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
    }

    /**
     * @Expression execution(* org.ofbiz.entity.GenericDelegator.clearCacheLine(org.ofbiz.entity.GenericValue, boolean)) && args(value, distribute)
     */
    void pointcut7(GenericValue value, boolean distribute) { }

    /**
     * @After pointcut7(value, distribute)
     */
    public void clearCacheLine(GenericValue value, boolean distribute) {
        Debug.logVerbose("run clearCacheLine(GenericValue value, boolean distribute), value = " + value , MODULE);
        // clear hibernate cache
        Map<String, Object> inputParams = UtilMisc.<String, Object>toMap("entityName", value.getEntityName(), "pk", value.getPrimaryKey());
        try {
            GenericValue userLoginToRunAs = getDelegator().findByPrimaryKeyCache("UserLogin", UtilMisc.toMap("userLoginId", runAsUser));
            if (userLoginToRunAs != null) {
                inputParams.put("userLogin", userLoginToRunAs);
            } else {
                return;
            }
            LocalDispatcher dispatcher = getDispatcher();
            dispatcher.runSync("opentaps.evictHibernateCache", inputParams, -1, true);
        } catch (GenericServiceException e) {
            Debug.logError(e, MODULE);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
    }

    /**
     * The method get GenericDelegator instance.
     * @return a <code>GenericDelegator</code> value
     */
    public GenericDelegator getDelegator() {
        GenericDelegator delegator = GenericDelegator.getGenericDelegator(DELEGATOR_NAME);
        return delegator;
    }

    /**
     * The method get LocalDispatcher instance.
     * @return a <code>LocalDispatcher</code> value
     */
    public LocalDispatcher getDispatcher() {
        LocalDispatcher dispatcher = GenericDispatcher.getLocalDispatcher(DELEGATOR_NAME, getDelegator());
        return dispatcher;
    }
}
