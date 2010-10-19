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
package org.opentaps.domain;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.ofbiz.base.util.UtilValidate;


/**
 * <p>Register opentaps domains in cached directory.<br>
 * Directory file must be somewhere on classpath, typically in config directory.
 * Its name should be set in context parameter with name "domainDirectory".</p>
 * 
 * <p>web.xml example:</p>
 * <code>
 *   &lt;context-param><br>
 *   &nbsp;&nbsp;&lt;param-name>domainDirectory&lt;/param-name><br>
 *   &nbsp;&nbsp;&lt;param-value>webform-domains-directory.xml&lt;/param-value><br>
 *   &lt;/context-param><br>
 *<br>
 *   &lt;listener><br>
 *   &nbsp;&nbsp;&lt;listener-class>org.opentaps.domain.DomainsListener&lt;/listener-class><br>
 *   &lt;/listener><br>
 * </code>
 */
public class DomainsListener implements ServletContextListener {

    @Override
    public void contextDestroyed(ServletContextEvent event) {
    }

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext context = event.getServletContext();
        // register component specific domain directory if file name
        // is set in context parameter
        String directoryFile = context.getInitParameter("domainDirectory");
        if (UtilValidate.isNotEmpty(directoryFile)) {
            DomainsLoader.registerDomainDirectory(directoryFile);
        };
    }

}
