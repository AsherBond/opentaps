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
package org.opentaps.installer.network;

import java.util.Properties;

import org.opentaps.core.bundle.AbstractBundle;
import org.opentaps.installer.network.model.impl.NetworkStepImpl;
import org.opentaps.installer.service.InstallerStep;
import org.opentaps.installer.service.OSSInstaller;
import org.opentaps.installer.util.ResourceCustomizer;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;

public class Activator extends AbstractBundle {

    private ServiceTracker wizardHttpSrvcTracker;
    private ServiceTracker staticHttpSrvcTracker;

    /** {@inheritDoc} */
    public void start(BundleContext context) throws Exception {

        super.start(context);

        // register GWT applications under alias /networkWiz as soon as
        // HttpService is available.
        wizardHttpSrvcTracker = new ServiceTracker(context, HttpService.class.getName(), new ResourceCustomizer(
                "/org.opentaps.gwt.wiz.network.network", "/networkWiz", context));
        wizardHttpSrvcTracker.open();

        // register static pages under alias /networkWiz/pages as soon as
        // HttpService is available.
        staticHttpSrvcTracker = new ServiceTracker(context, HttpService.class.getName(), new ResourceCustomizer(
                "/static", "/networkWiz/pages", context));
        staticHttpSrvcTracker.open();

        // register services
        InstallerStep stepImpl = new NetworkStepImpl();
        Properties props = new Properties();
        props.put(OSSInstaller.STEP_ID_PROP, "network");
        props.put(OSSInstaller.SEQUENCE_PROP, Integer.valueOf(40));
        context.registerService(InstallerStep.class.getName(), stepImpl, props);
 
    }

    /** {@inheritDoc} */
    public void stop(BundleContext context) throws Exception {
        wizardHttpSrvcTracker.close();
        staticHttpSrvcTracker.close();

        super.stop(context);
    }

}
