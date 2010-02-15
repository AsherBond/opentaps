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
package org.opentaps.installer.util;

import org.opentaps.installer.Activator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTrackerCustomizer;


/**
 *
 *
 */
public class ResourceCustomizer implements ServiceTrackerCustomizer {

    private BundleContext context;
    private String alias;
    private String resources;

    public ResourceCustomizer(String resources, String alias, BundleContext context) {
        if (resources == null || alias == null || context == null) {
            throw new IllegalArgumentException();
        }

        this.context = context;
        this.alias = alias;
        this.resources = resources;
    }

    /** {@inheritDoc} */
    public Object addingService(ServiceReference ref) {
        HttpService service = (HttpService) context.getService(ref);
        try {
            service.registerResources(alias, resources, null);
            Activator.logInfo(String.format("Resource directory %1$s is registered as alias %2$s.", resources, alias), null, null);
        } catch (NamespaceException e) {
            Activator.logError(e.getMessage(), e, null);
        }
        return alias;
    }

    /** {@inheritDoc} */
    public void modifiedService(ServiceReference ref, Object alias) {
        // do nothing
    }

    /** {@inheritDoc} */
    public void removedService(ServiceReference ref, Object alias) {
        HttpService service = (HttpService) context.getService(ref);
        service.unregister((String) alias);
        Activator.logInfo(String.format("Resources under %1$s are unregistered and no longer available.", alias), null, null);
    }

}
