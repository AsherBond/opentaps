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
import org.hibernate.event.PersistEvent;
import org.hibernate.event.def.DefaultPersistEventListener;
import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.opentaps.foundation.entity.Entity;

/**
 * This is an implementation of the DefaultPersistEventListener for support eca with hibernate.
 *
 */
public class EcaPersistEventListener extends DefaultPersistEventListener {
    private static final String MODULE = EcaPersistEventListener.class
            .getName();
    private Delegator delegator;

    /**
     * EcaPersistEventListener constructor.
     *
     * @param delegator
     *            <code>Delegator</code> object.
     */
    public EcaPersistEventListener(Delegator delegator) {
        this.delegator = delegator;
    }

    /**
     * Handle the given create event.
     *
     * @param event
     *            The create event to be handled.
     * @throws HibernateException
     *             if an error occurs
     */
    public void onPersist(PersistEvent event) throws HibernateException {
        Entity entity = (Entity) event.getObject();
        try {
            //run eecas of before save event
            EcaCommonEvent.beforeSave(entity, delegator);
            // call super method to persist object
            super.onPersist(event);
            //run eecas of after save event
            EcaCommonEvent.afterSave(entity, delegator);
            Debug.logVerbose("Execute persist operation for entity [" + entity.getBaseEntityName() + "]" + " sucessed.", MODULE);
        } catch (GenericEntityException e) {
            String errMsg = "Failure in persist operation for entity [" + entity.getBaseEntityName() + "]: " + e.toString()
                    + ".";
            Debug.logError(e, errMsg, MODULE);
            throw new HibernateException(e.getMessage());
        }
    }

}
