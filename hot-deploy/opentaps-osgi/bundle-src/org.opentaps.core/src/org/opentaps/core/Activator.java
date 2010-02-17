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
package org.opentaps.core;

import javax.servlet.Filter;
import javax.servlet.ServletException;

import org.apache.felix.http.api.ExtHttpService;
import org.opentaps.core.bundle.AbstractBundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;


public class Activator extends AbstractBundle {

    // the shared instance
    private static BundleActivator bundle;

    private ServiceTracker debugFilterTracker = null;

    /** {@inheritDoc} */
    public void start(final BundleContext context) throws Exception {

        bundle = this;
        super.start(context);

        debugFilterTracker = new ServiceTracker(context, ExtHttpService.class.getName(), new ServiceTrackerCustomizer() {

            public Object addingService(ServiceReference reference) {
                ExtHttpService service = (ExtHttpService) context.getService(reference);
                if (service != null) {
                    Filter filter = new DebugFilter();
                    try {
                        service.registerFilter(filter, "/.*", null, 0, null);
                    } catch (ServletException e) {
                        logError(e.getMessage(), e, null);
                        return null;
                    }
                    return filter;
                }
                return null;
            }

            public void modifiedService(ServiceReference reference, Object filter) {
            }

            public void removedService(ServiceReference reference, Object filter) {
                ExtHttpService service = (ExtHttpService) context.getService(reference);
                if (service != null) {
                    service.unregisterFilter((Filter) filter);
                }
            }
            
        });
        debugFilterTracker.open();
    }

    /** {@inheritDoc} */
    public void stop(BundleContext context) throws Exception {
        debugFilterTracker.close();

        super.stop(context);
        bundle = null;
    }

    public static Activator getInstance() {
        return (Activator) bundle;
    };

}
