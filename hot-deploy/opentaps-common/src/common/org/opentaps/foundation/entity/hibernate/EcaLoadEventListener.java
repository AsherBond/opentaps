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
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.event.LoadEvent;
import org.hibernate.event.LoadEventListener;
import org.hibernate.event.def.DefaultLoadEventListener;
import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericPK;
import org.ofbiz.entity.model.ModelEntity;

/**
 * This is an implementation of the DefaultLoadEventListener for support eca with hibernate.
 */
public class EcaLoadEventListener extends DefaultLoadEventListener {
    private static final String MODULE = EcaLoadEventListener.class.getName();
    private GenericDelegator delegator;

    /**
     * EcaLoadEventListener constructor.
     *
     * @param delegator
     *            <code>GenericDelegator</code> object.
     */
    public EcaLoadEventListener(GenericDelegator delegator) {
        this.delegator = delegator;
    }

    /**
     * Handle the given load event.
     *
     * @param event a <code>LoadEvent</code> value
     * @param loadType a <code>LoadEventListener.LoadType</code> value
     * @throws HibernateException if an error occurs
     */
    public void onLoad(LoadEvent event, LoadEventListener.LoadType loadType) throws HibernateException {
        String entityClassName = event.getEntityClassName();
        if (entityClassName.indexOf(".") > 0) {
            entityClassName = entityClassName.substring(entityClassName.lastIndexOf(".") + 1);
        }
        Serializable object = event.getEntityId();
        Map fields = new HashMap<String, Object>();
        ModelEntity modelEntity = delegator.getModelEntity(entityClassName);
        List<String> pkNames = modelEntity.getPkFieldNames();

        try {
            if (pkNames.size() > 1) {
                for (String pkName : pkNames) {
                    fields.put(pkName, HibernateUtil.getFieldValue(object, pkName));
                }
            } else if (pkNames.size() == 1) {
                fields.put(pkNames.get(0), object);
            }
            GenericPK primaryKey = delegator.makePK(entityClassName, fields);
            //run eecas of before save event
            EcaCommonEvent.beforeLoad(primaryKey, delegator);
            // call super method to persist object
            super.onLoad(event, loadType);
            //run eecas of after save event
            EcaCommonEvent.afterLoad(primaryKey, delegator);
            Debug.logVerbose("Execute load operation for entity [" + entityClassName + "]" + " sucessed.", MODULE);
        } catch (GenericEntityException e) {
            String errMsg = "Failure in load operation for entity [" + entityClassName + "]: " + e.toString() + ".";
            Debug.logError(e, errMsg, MODULE);
            throw new HibernateException(e.getMessage());
        } catch (SecurityException e) {
            String errMsg = "Failure in load operation for entity [" + entityClassName + "]: " + e.toString() + ".";
            Debug.logError(e, errMsg, MODULE);
            throw new HibernateException(e.getMessage());
        } catch (IllegalArgumentException e) {
            String errMsg = "Failure in load operation for entity [" + entityClassName + "]: " + e.toString() + ".";
            Debug.logError(e, errMsg, MODULE);
            throw new HibernateException(e.getMessage());
        } catch (IllegalAccessException e) {
            String errMsg = "Failure in load operation for entity [" + entityClassName + "]: " + e.toString() + ".";
            Debug.logError(e, errMsg, MODULE);
            throw new HibernateException(e.getMessage());
        } catch (InvocationTargetException e) {
            String errMsg = "Failure in load operation for entity [" + entityClassName + "]: " + e.toString() + ".";
            Debug.logError(e, errMsg, MODULE);
            throw new HibernateException(e.getMessage());
        }
    }


}
