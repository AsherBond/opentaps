/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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
package org.opentaps.common.osgi.demo;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 *
 */
public final class OSGiDemoEvents {

    public static final String MODULE = OSGiDemoEvents.class.getName();

    public static String callOSGi(HttpServletRequest request, HttpServletResponse response) {

        ServletContext servletContext = (ServletContext) request.getAttribute("servletContext");
        BundleContext bundleContext = (BundleContext) servletContext.getAttribute(BundleContext.class.getName());
        ServiceTracker echoSrvc = new ServiceTracker((BundleContext) bundleContext, Echo.class.getName(), null);
        Echo service = (Echo) echoSrvc.getService();
        if (service != null) {
            Debug.logInfo("OSGi echo service has returned string: " + service.echo("Ping..."), MODULE);
        }

        return "success";
    }
}
