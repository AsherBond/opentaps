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

import org.hibernate.HibernateException;
import org.hibernate.event.SaveOrUpdateEvent;
import org.hibernate.event.def.DefaultSaveEventListener;
import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.opentaps.foundation.entity.Entity;
/**
 * This is an implementation of the DefaultSaveEventListener for support eca with hibernate.
 *
 */
public class EcaSaveEventListener extends DefaultSaveEventListener {
    private static final String MODULE = EcaSaveOrUpdateEventListener.class
            .getName();
    private Delegator delegator;

    /**
     * EcaSaveEventListener constructor.
     * 
     * @param delegator
     *            <code>Delegator</code> object.
     */
    public EcaSaveEventListener(Delegator delegator) {
        this.delegator = delegator;
    }

    /**
     * Handle the given update event.
     * 
     * @param event
     *            The update event to be handled.
     */
    public void onSaveOrUpdate(SaveOrUpdateEvent event) {
        Entity entity = (Entity) event.getObject();
        try {
            // run eecas of before save event
            EcaCommonEvent.beforeSave(entity, delegator);
            // call super method to persist object
            super.onSaveOrUpdate(event);
            // run eecas of after save event
            EcaCommonEvent.afterSave(entity, delegator);
            Debug.logVerbose("Execute save/update operation for entity ["
                    + entity.getBaseEntityName() + "]" + " sucessed.", MODULE);
        } catch (GenericEntityException e) {
            String errMsg = "Failure in save/update operation for entity ["
                    + entity.getBaseEntityName() + "]: " + e.toString() + ".";
            Debug.logError(e, errMsg, MODULE);
            throw new HibernateException(e.getMessage());
        }
    }

}
