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
package org.opentaps.common.jndi;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.sql.DataSource;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.DelegatorFactory;

public class JNDIContextListener implements ServletContextListener {

    public static final String MODULE = JNDIContextListener.class.getName();     

    /** {@inheritDoc} */
    public void contextInitialized(ServletContextEvent event) {

        try {

            Delegator delegator =  DelegatorFactory.getDelegator("default");
            InitialContext ctx = new InitialContext();

            String dataSourceName = delegator.getGroupHelperName("org.ofbiz");
            if (UtilValidate.isNotEmpty(dataSourceName)) {
                DataSourceImpl operational = new DataSourceImpl(dataSourceName);  
                ctx.rebind("java:operational", (DataSource) operational);
            }

            dataSourceName = delegator.getGroupHelperName("org.opentaps.analytics");
            if (UtilValidate.isNotEmpty(dataSourceName)) {
                DataSourceImpl analytics = new DataSourceImpl(dataSourceName);  
                ctx.rebind("java:analytics", (DataSource) analytics);
            }

            dataSourceName = delegator.getGroupHelperName("org.opentaps.testing");
            if (UtilValidate.isNotEmpty(dataSourceName)) {
                DataSourceImpl analytics = new DataSourceImpl(dataSourceName);  
                ctx.rebind("java:testing", (DataSource) analytics);
            }

        } catch (NamingException e) {
            Debug.logError(e, "Error binding analytics datasources to JNDI server", MODULE);        
        }

    }

    /** {@inheritDoc} */
    public void contextDestroyed(ServletContextEvent event) {

        try {

            new InitialContext().unbind("java:operational");
            new InitialContext().unbind("java:analytics");
            new InitialContext().unbind("java:testing");

        } catch (NamingException e) {
            Debug.logError(e, "Error unbinding analytics datasources from JNDI server", MODULE);
        }      
    }

}
