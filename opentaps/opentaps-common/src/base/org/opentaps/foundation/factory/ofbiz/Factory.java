package org.opentaps.foundation.factory.ofbiz;

import org.opentaps.foundation.factory.FactoryInterface;
import org.opentaps.foundation.factory.FactoryException;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.security.Security;

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

/**
 * This is an implementation of the FactoryInterface for ofbiz.  In most cases in ofbiz, creating this involves calling services
 * with the dispatcher, so you need to supply the Infrastructure with delegator and dispatcher.
 *
 */
public class Factory implements FactoryInterface {

    protected Infrastructure infrastructure = null;
    protected Delegator delegator = null;
    protected LocalDispatcher dispatcher = null;
    protected Security security = null;
    protected GenericValue userLogin = null; // a user associated with an instance of the infrastructure

    /**
     * @param infrastructure
     * @param userLogin
     */
    public Factory(Infrastructure infrastructure, GenericValue userLogin) throws FactoryException {
        this.infrastructure = infrastructure;
        this.dispatcher = infrastructure.getDispatcher();
        this.delegator = infrastructure.getDelegator();
        this.security = infrastructure.getSecurity();
        this.userLogin = userLogin;
    }

    public Delegator getDelegator() {
        return delegator;
    }
}

