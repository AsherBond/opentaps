/*
 * Copyright (c) 2006 - 2010 Open Source Strategies, Inc.
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
package org.opentaps.bundle.demo;

import javax.servlet.ServletException;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

public class Activator implements BundleActivator, ServiceListener {

    private static final String HTTP_SERVICE_CLASSNAME = "org.osgi.service.http.HttpService";
    private static final String DEMO_SRVLT_ALIAS = "/demo";

    private BundleContext context = null;

    /** {@inheritDoc} */
    public void start(BundleContext context) throws Exception {

        // keep context for further operations
        this.context = context;

        /*
         * Register our servlet if HttpService is available in startup time or
         * set up listener to do registration as soon as the service is ready.
         */
        ServiceReference sRef = context.getServiceReference(HttpService.class.getName());
        if (sRef != null) {
            registerServlet(sRef);
        } else {
            context.addServiceListener(this, String.format("(%1$s=%2$s)", Constants.OBJECTCLASS, HTTP_SERVICE_CLASSNAME));
        }
    }

    /** {@inheritDoc} */
    public void stop(BundleContext context) throws Exception {
        // do nothing
    }

    /**
     * Register servlet with <code>HttpService</code>.
     * 
     * @param sRef a <code>HttpService</code> service reference
     * @throws ServletException
     * @throws NamespaceException
     */
    private void registerServlet(ServiceReference sRef) throws ServletException, NamespaceException {
        if (sRef != null) {
            HttpService service = (HttpService) context.getService(sRef);
            service.registerServlet(DEMO_SRVLT_ALIAS, new DemoServlet(), null, null);
        }
    }

    /** {@inheritDoc} */
    public void serviceChanged(ServiceEvent event) {
        if (event.getType() == ServiceEvent.REGISTERED) {
            try {
                registerServlet(event.getServiceReference());
            } catch (ServletException e) {
                //TODO: log error
                e.printStackTrace();
            } catch (NamespaceException e) {
                //TODO: log error
                e.printStackTrace();
            }
        }
    }

}
