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

import java.io.IOException;

import javax.naming.Context;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * This servlet cleans up JNDI settings in the JVM (which DataSourceFakeJndiPreparer altered), so that no other OFBiz component is affected
 *  
 * Should be configured in web.xml to load AFTER any other Servlet
 * 
 * @author Cameron Smith, www.database.co.mz
 */
public class DataSourceFakeJndiCleaner extends DataSourceFakeJndiBase implements Servlet
{    
    public void destroy() { }

    public ServletConfig getServletConfig() { return null; }

    public String getServletInfo() { return null; }

    /**
    * Resets JVM default settings for JNDI, like they were before ServletContext startup.
     */
    public void init(ServletConfig arg0) throws ServletException
    {
        setSysPropIfNotNull(Context.INITIAL_CONTEXT_FACTORY, _oldInitialContextFactory);
    }

    public void service(ServletRequest arg0, ServletResponse arg1) throws ServletException, IOException { }
}
