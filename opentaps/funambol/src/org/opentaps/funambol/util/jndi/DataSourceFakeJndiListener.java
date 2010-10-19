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

package org.opentaps.funambol.util.jndi;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;

import mz.co.dbl.siga.framework.workflow.MapBasedJndiInitialContextFactory;

/**
 * This listener sets and unsets JNDI settings in the JVM, before any Funambol work is performed for an incoming HTTP request
 *  
 * Should be configured in web.xml to load AFTER any other listener
 * 
 * @author Cameron Smith, www.database.co.mz
 */
public class DataSourceFakeJndiListener extends DataSourceFakeJndiBase implements ServletRequestListener
{
    public void requestInitialized(ServletRequestEvent arg0)
    {
        _oldInitialContextFactory = System.setProperty(InitialContext.INITIAL_CONTEXT_FACTORY, MapBasedJndiInitialContextFactory.class.getName());
    }

    public void requestDestroyed(ServletRequestEvent arg0)
    {
        setSysPropIfNotNull(Context.INITIAL_CONTEXT_FACTORY, _oldInitialContextFactory);
    }
}
