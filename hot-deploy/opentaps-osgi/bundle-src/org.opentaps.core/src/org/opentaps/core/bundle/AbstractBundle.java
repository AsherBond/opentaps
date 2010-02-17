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
package org.opentaps.core.bundle;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;


/**
 * 
 *
 */
public abstract class AbstractBundle implements BundleActivator {

    // LogService tracker
    private ServiceTracker logTracker;

    /** {@inheritDoc} */
    public void start(BundleContext context) throws Exception {
        // start tracking services
        logTracker = new ServiceTracker(context, LogService.class.getName(), null);
        logTracker.open();
    }

    /** {@inheritDoc} */
    public void stop(BundleContext context) throws Exception {

        // stop tracking services
        logTracker.close();
    }

    public void log(int severity, String message, Throwable e, ServiceReference sref) {
        LogService service = (LogService) logTracker.getService();
        if (service != null) {
            service.log(sref, severity, message, e);
        }
    }

    public void logError(String message, Throwable e, ServiceReference sref) {
        log(LogService.LOG_ERROR, message, e, sref);
    }

    public void logDebug(String message, Throwable e, ServiceReference sref) {
        log(LogService.LOG_DEBUG, message, e, sref);
    }
 
    public void logWarning(String message, Throwable e, ServiceReference sref) {
        log(LogService.LOG_WARNING, message, e, sref);
    }
 
    public void logInfo(String message, Throwable e, ServiceReference sref) {
        log(LogService.LOG_WARNING, message, e, sref);
    }
}
