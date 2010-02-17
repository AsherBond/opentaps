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
package org.opentaps.installer.company;

import java.util.Properties;

import javax.servlet.ServletException;

import org.opentaps.core.bundle.AbstractBundle;
import org.opentaps.installer.company.model.CompanyStepModel;
import org.opentaps.installer.company.model.impl.CompanyStepImpl;
import org.opentaps.installer.service.InstallerNavigation;
import org.opentaps.installer.service.InstallerStep;
import org.opentaps.installer.service.OSSInstaller;
import org.opentaps.installer.util.ResourceCustomizer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;


public class Activator extends AbstractBundle {

    // the shared instance
    private static BundleActivator bundle;

    private ServiceTracker wizardHttpSrvcTracker;
    private ServiceTracker staticHttpSrvcTracker;
    private ServiceTracker navHttpSrvcTracker;

    /** {@inheritDoc} */
    public void start(final BundleContext context) throws Exception {

        bundle = this;
        super.start(context);

        // register GWT applications under alias /companyWiz as soon as
        // HttpService is available.
        wizardHttpSrvcTracker = new ServiceTracker(context, HttpService.class.getName(), new ResourceCustomizer(
                "/org.opentaps.gwt.wiz.company.company", "/companyWiz", context));
        wizardHttpSrvcTracker.open();

        // register static pages under alias /companyWiz/pages as soon as
        // HttpService is available.
        staticHttpSrvcTracker = new ServiceTracker(context, HttpService.class.getName(), new ResourceCustomizer(
                "/static", "/companyWiz/pages", context));
        staticHttpSrvcTracker.open();

        navHttpSrvcTracker = new ServiceTracker(context, HttpService.class.getName(), new ServiceTrackerCustomizer() {

            public Object addingService(ServiceReference ref) {
                HttpService service = (HttpService) context.getService(ref);
                if (service != null) {
                    try {
                        service.registerServlet("/companyWiz/InstNav", new InstallerNavigation(), null, null);
                    } catch (ServletException e) {
                        logError(e.getMessage(), e, null);
                        return null;
                    } catch (NamespaceException e) {
                        logError(e.getMessage(), e, null);
                        return null;
                    }
                }
                return null;
            }

            public void modifiedService(ServiceReference ref, Object alias) {
                // do nothing
            }

            public void removedService(ServiceReference ref, Object alias) {
                HttpService service = (HttpService) context.getService(ref);
                if (service != null) {
                    service.unregister("//companyWiz/InstNav");
                }
            }
            
        });
        navHttpSrvcTracker.open();

        // register services
        CompanyStepModel stepImpl = new CompanyStepImpl();
        Properties props = new Properties();
        props.put(OSSInstaller.STEP_ID_PROP, "company");
        props.put(OSSInstaller.SEQUENCE_PROP, Integer.valueOf(10));
        context.registerService(InstallerStep.class.getName(), stepImpl, props);
    }

    /** {@inheritDoc} */
    public void stop(BundleContext context) throws Exception {
        wizardHttpSrvcTracker.close();
        staticHttpSrvcTracker.close();
        navHttpSrvcTracker.close();

        super.stop(context);
        bundle = null;
    }

    public static Activator getInstance() {
        return (Activator) bundle;
    };

}
