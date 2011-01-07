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

import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.util.Map;

import org.hibernate.HibernateException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericModelException;
import org.ofbiz.entity.GenericPK;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.eca.EntityEcaHandler;
import org.ofbiz.entity.model.ModelEntity;
import org.opentaps.foundation.entity.Entity;

/**
 * This is class which provide common eca event codes to other EventListener.
 */
public final class EcaCommonEvent {

    private static final String MODULE = EcaCommonEvent.class.getName();

    private EcaCommonEvent() { }

    /**
     * execute the eccas of before save event, and return GenericValue.
     * this method should be call before save event in EcaPersistEventListener\EcaSaveOrUpdateEventListener\EcaSaveEventListener
     * @param entity an <code>Entity</code> value.
     * @param delegator an <code>Delegator</code> value.
     * @throws GenericEntityException if an error occurs
     * @return GenericValue
     */
    public static GenericValue beforeSave(Entity entity, Delegator delegator) throws GenericEntityException {
        if (entity == null) {
            return null;
        }
        GenericValue value = HibernateUtil.entityToGenericValue(entity, delegator);
        // get eca event map
        Map ecaEventMap = delegator.getEntityEcaHandler().getEntityEventMap(value.getEntityName());
        // 1. first is validate event
        if (ecaEventMap != null) {
            delegator.getEntityEcaHandler().evalRules(
                    EntityEcaHandler.OP_STORE, ecaEventMap,
                    EntityEcaHandler.EV_VALIDATE, value, false);
        }
        // 2. next is cache clear event
        if (ecaEventMap != null) {
            delegator.getEntityEcaHandler().evalRules(
                    EntityEcaHandler.OP_STORE, ecaEventMap,
                    EntityEcaHandler.EV_CACHE_CLEAR, value, false);
        }

        // 3. before save, next is run event
        if (ecaEventMap != null) {
            delegator.getEntityEcaHandler().evalRules(
                    EntityEcaHandler.OP_STORE, ecaEventMap,
                    EntityEcaHandler.EV_RUN, value, false);
            // refresh the valueObject to get the new version
        }

        //auto encrypt fields
        delegator.encryptFields(value);

        // refresh the valueObject to get the new version
        // in my experience, for hibernate lazy load reason, this function
        // would be complex.
        HibernateUtil.refreshPojoByGenericValue(entity, value);

        // automatically stores for timestamps with every entity persisted to the database
        try {
            if (HibernateUtil.fieldExists(entity, ModelEntity.CREATE_STAMP_FIELD, Timestamp.class)) {
                // just store it when save new Entity
                if (HibernateUtil.getFieldValue(entity, ModelEntity.CREATE_STAMP_FIELD) == null) {
                    HibernateUtil.setFieldValue(entity, ModelEntity.CREATE_STAMP_FIELD, UtilDateTime.nowTimestamp());
                }
            }
            if (HibernateUtil.fieldExists(entity, ModelEntity.CREATE_STAMP_TX_FIELD, Timestamp.class)) {
                // just store it when save new Entity
                if (HibernateUtil.getFieldValue(entity, ModelEntity.CREATE_STAMP_TX_FIELD) == null) {
                    HibernateUtil.setFieldValue(entity, ModelEntity.CREATE_STAMP_TX_FIELD, UtilDateTime.nowTimestamp());
                }
            }
            if (HibernateUtil.fieldExists(entity, ModelEntity.STAMP_FIELD, Timestamp.class)) {
                HibernateUtil.setFieldValue(entity, ModelEntity.STAMP_FIELD, UtilDateTime.nowTimestamp());
            }
            if (HibernateUtil.fieldExists(entity, ModelEntity.STAMP_TX_FIELD, Timestamp.class)) {
                HibernateUtil.setFieldValue(entity, ModelEntity.STAMP_TX_FIELD, UtilDateTime.nowTimestamp());
            }
        } catch (IllegalArgumentException e) {
            String errMsg = "Failure in update timestamps for entity [" + HibernateUtil.getEntityClassName(entity) + "]: " + e.toString() + ".";
            Debug.logError(e, errMsg, MODULE);
            throw new HibernateException(e.getMessage());
        } catch (NoSuchMethodException e) {
            String errMsg = "Failure in update timestamps for entity [" + HibernateUtil.getEntityClassName(entity) + "]: " + e.toString() + ".";
            Debug.logError(e, errMsg, MODULE);
            throw new HibernateException(e.getMessage());
        } catch (IllegalAccessException e) {
            String errMsg = "Failure in update timestamps for entity [" + HibernateUtil.getEntityClassName(entity) + "]: " + e.toString() + ".";
            Debug.logError(e, errMsg, MODULE);
            throw new HibernateException(e.getMessage());
        } catch (InvocationTargetException e) {
            String errMsg = "Failure in update timestamps for entity [" + HibernateUtil.getEntityClassName(entity) + "]: " + e.toString() + ".";
            Debug.logError(e, errMsg, MODULE);
            throw new HibernateException(e.getMessage());
        }
        return value;

    }


    /**
     * execute the eccas of after save event, and return GenericValue.
     * this method should be call after save event in EcaPersistEventListener\EcaSaveOrUpdateEventListener\EcaSaveEventListener
     * @param entity an <code>Entity</code> value.
     * @param delegator an <code>Delegator</code> value.
     * @throws GenericEntityException if an error occurs
     * @return GenericValue
     */
    public static GenericValue afterSave(Entity entity, Delegator delegator) throws GenericEntityException {
        if (entity == null) {
            return null;
        }
        GenericValue value = HibernateUtil.entityToGenericValue(entity, delegator);

        // always clear cache before the operation
        delegator.clearCacheLine(value);

        // get eca event map
        Map ecaEventMap = delegator.getEntityEcaHandler().getEntityEventMap(value.getEntityName());
        if (ecaEventMap != null) {
            delegator.getEntityEcaHandler().evalRules(
                    EntityEcaHandler.OP_STORE, ecaEventMap,
                    EntityEcaHandler.EV_RETURN, value, false);
        }
        return value;
    }

    /**
     * execute the eccas of before delete event.
     * this method should be call before delete event in EcaDeleteEventListener
     * @param entity an <code>Entity</code> value.
     * @param delegator an <code>Delegator</code> value.
     * @throws GenericEntityException if an error occurs
     */
    public static void beforeDelete(Entity entity, Delegator delegator) throws GenericEntityException {
        if (entity == null) {
            return;
        }
        try {
            GenericValue value = HibernateUtil.entityToGenericValue(entity, delegator);
            // get eca event map
            Map ecaEventMap = delegator.getEntityEcaHandler().getEntityEventMap(value.getEntityName());
            // 1. first is validate event
            if (ecaEventMap != null) {
                delegator.getEntityEcaHandler().evalRules(
                        EntityEcaHandler.OP_REMOVE, ecaEventMap,
                        EntityEcaHandler.EV_VALIDATE, value, false);
            }
            // 2. next is cache clear event
            if (ecaEventMap != null) {
                delegator.getEntityEcaHandler().evalRules(
                        EntityEcaHandler.OP_REMOVE, ecaEventMap,
                        EntityEcaHandler.EV_CACHE_CLEAR, value, false);
            }
            // 3. before remove is run event
            if (ecaEventMap != null) {
                delegator.getEntityEcaHandler().evalRules(
                        EntityEcaHandler.OP_REMOVE, ecaEventMap,
                        EntityEcaHandler.EV_RUN, value, false);
            }
        } catch (org.hibernate.ObjectNotFoundException e) {
            String errMsg = "Failure when run eca of beforeDelete [" + HibernateUtil.getEntityClassName(entity) + "]: " + e.toString() + ".";
            Debug.logError(e, errMsg, MODULE);
        }
    }

    /**
     * execute the eccas of after delete event.
     * this method should be call after delete event in EcaDeleteEventListener
     * @param entity an <code>Entity</code> value.
     * @param delegator an <code>Delegator</code> value.
     * @throws GenericEntityException if an error occurs
     */
    public static void afterDelete(Entity entity, Delegator delegator) throws GenericEntityException {
        if (entity == null) {
            return;
        }
        try {
            GenericValue value = HibernateUtil.entityToGenericValue(entity, delegator);

            // always clear cache before the operation
            delegator.clearCacheLine(value);

            // get eca event map
            Map ecaEventMap = delegator.getEntityEcaHandler().getEntityEventMap(value.getEntityName());
            if (ecaEventMap != null) {
                delegator.getEntityEcaHandler().evalRules(
                        EntityEcaHandler.OP_REMOVE, ecaEventMap,
                        EntityEcaHandler.EV_RETURN, value, false);
            }
        } catch (org.hibernate.ObjectNotFoundException e) {
            String errMsg = "Failure when run eca of afterDelete [" + HibernateUtil.getEntityClassName(entity) + "]: " + e.toString() + ".";
            Debug.logError(e, errMsg, MODULE);
        }
    }

    /**
     * execute the eccas of before load event.
     * this method should be call before load event in EcaLoadEventListener
     * @param primaryKey an <code>GenericPK</code> value.
     * @param delegator an <code>Delegator</code> value.
     * @throws GenericEntityException if an error occurs
     */
    public static void beforeLoad(GenericPK primaryKey, Delegator delegator) throws GenericEntityException {
        if (primaryKey == null) {
            return;
        }
        // get eca event map
        Map ecaEventMap = delegator.getEntityEcaHandler().getEntityEventMap(primaryKey.getEntityName());
        // 1. first is validate event
        if (ecaEventMap != null) {
            delegator.getEntityEcaHandler().evalRules(
                    EntityEcaHandler.OP_FIND, ecaEventMap,
                    EntityEcaHandler.EV_VALIDATE, primaryKey, false);
        }
        // 2. next is cache clear event
        if (!primaryKey.isPrimaryKey()) {
            throw new GenericModelException("[Delegator.findByPrimaryKey] Passed primary key is not a valid primary key: " + primaryKey);
        }
        if (ecaEventMap != null) {
            delegator.getEntityEcaHandler().evalRules(EntityEcaHandler.OP_FIND, ecaEventMap, EntityEcaHandler.EV_RUN, primaryKey, false);
        }

    }

    /**
     * execute the eccas of after load event.
     * this method should be call after load event in EcaLoadEventListener
     * @param primaryKey an <code>GenericPK</code> value.
     * @param delegator an <code>Delegator</code> value.
     * @throws GenericEntityException if an error occurs
     */
    public static void afterLoad(GenericPK primaryKey, Delegator delegator) throws GenericEntityException {
        if (primaryKey == null) {
            return;
        }
        // get eca event map
        Map ecaEventMap = delegator.getEntityEcaHandler().getEntityEventMap(primaryKey.getEntityName());
        if (ecaEventMap != null) {
            delegator.getEntityEcaHandler().evalRules(
                    EntityEcaHandler.OP_FIND, ecaEventMap,
                    EntityEcaHandler.EV_RETURN, primaryKey, false);
        }

    }



}
