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
package org.opentaps.installer;

import javax.servlet.ServletException;

import org.opentaps.core.bundle.AbstractBundle;
import org.opentaps.installer.service.Constants;
import org.opentaps.installer.service.InstallerNavigation;
import org.opentaps.installer.service.InstallerStep;
import org.opentaps.installer.service.OSSInstaller;
import org.opentaps.installer.service.impl.OSSInstallerImpl;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;


/**
 * Customizer object that registers/unregisters <code>InstallerNavigation</code> servlet
 * with <code>HttpService</code>.
 */
class NavigationServletCustomizer implements ServiceTrackerCustomizer {

    private static final String INSTALLER_NAVIGATION_ALIAS = "/InstNav";

    private BundleContext context;

    public NavigationServletCustomizer(BundleContext context) {
        this.context = context;
    }

    public Object addingService(ServiceReference reference) {
        try {
            HttpService service = (HttpService) context.getService(reference);
            if (service != null) {
                String parentWebApp = context.getProperty(Constants.URL_PREFIX);
                service.registerServlet(
                        String.format("%1$s%2$s", (parentWebApp == null || parentWebApp.length() == 0) ? "" : parentWebApp, INSTALLER_NAVIGATION_ALIAS),
                        new InstallerNavigation(), null, null);
            }
        } catch (ServletException e) {
            Activator.getInstance().logError(e.getMessage(), e, null);
        } catch (NamespaceException e) {
            Activator.getInstance().logError(e.getMessage(), e, null);
        }
        return null;
    }

    public void modifiedService(ServiceReference reference, Object alias) {
        // do nothing
    }

    public void removedService(ServiceReference reference, Object alias) {
        HttpService service = (HttpService) context.getService(reference);
        if (service != null) {
            service.unregister(INSTALLER_NAVIGATION_ALIAS);
        }
    }
}

/**
 * Core Installer bundle activator.
 */
public class Activator extends AbstractBundle {

    // the shared instance
    private static BundleActivator bundle;

    private ServiceTracker navHttpSrvcTracker;
    private ServiceTracker installerTracker;
    private ServiceTracker stepsTracker;

    /** {@inheritDoc} */
    public void start(BundleContext context) throws Exception {

        bundle = this;
        super.start(context);

        navHttpSrvcTracker = new ServiceTracker(context, HttpService.class.getName(), new NavigationServletCustomizer(context));
        navHttpSrvcTracker.open();

        // register main service that manages installation flow and start tracking it.
        OSSInstaller installer = new OSSInstallerImpl();
        context.registerService(OSSInstaller.class.getName(), installer, null);

        installerTracker = new ServiceTracker(context, OSSInstaller.class.getName(), null);
        installerTracker.open();

        // start tracking installation step services 
        Filter stepsFlt = context.createFilter("(objectClass=" + InstallerStep.class.getName() + ")");
        stepsTracker = new ServiceTracker(context, stepsFlt, null);
        stepsTracker.open();
    }

    /** {@inheritDoc} */
    public void stop(BundleContext context) throws Exception {

        // stop tracking services
        stepsTracker.close();
        navHttpSrvcTracker.close();
        installerTracker.close();

        super.stop(context);
        bundle = null;
    }

    public static Activator getInstance() {
        return (Activator) bundle;
    }

    /**
     * Returns <code>OSSInstaller</code> service.
     * @return An <code>OSSInstaller</code> implementation.
     */
    public OSSInstaller getInstaller() {
        return (OSSInstaller) installerTracker.getService();
    }

    /**
     * Returns installation step service references.
     * @return Array of the service references.
     */
    public ServiceReference[] findInstSteps() {
        return stepsTracker.getServiceReferences();
    }

    /**
     * Retrieve step service object.
     * @param reference A service reference.
     * @return An implementation of the <code>InstallerStep</code> interface.
     */
    public InstallerStep findStep(ServiceReference reference) {
        return (InstallerStep) stepsTracker.getService(reference);
    }
}
