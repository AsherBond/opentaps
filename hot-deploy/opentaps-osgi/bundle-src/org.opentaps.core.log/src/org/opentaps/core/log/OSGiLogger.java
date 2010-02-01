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
package org.opentaps.core.log;

import org.ofbiz.base.util.Debug;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogService;


/**
 * OSGiLogger subscribes itself as receiver for <code>LogEntry</code> objects from the <code>LogReaderService</code> and
 * puts arriving messages into common log.
 */
public class OSGiLogger implements LogListener {

    /**
     * An array to convert OSGi LogService severity code to
     * one used in OfBiz.<br>
     * The scheme is: severityMap[LogService.&lt;SEVERITY CONSTANT>] = Debug.&lt;OFBIZ SEVERITY CONSTANT>
     */
    int[] severityMap = {
            -1,                 // no mapping
            Debug.ERROR,        // LogService.ERROR
            Debug.WARNING,      // LogService.WARNING
            Debug.INFO,         // LogService.INFO
            Debug.VERBOSE       // LogService.DEBUG, as no exact match 
    };

    /** {@inheritDoc} */
    public void logged(LogEntry entry) {
        int severity = entry.getLevel();
        // ensure we are within array bounds
        if (severity < 1 || severity > 4) {
            severity = LogService.LOG_INFO; // unknown, use reasonable default value
        }

        // add bundle name and service name (if exists) to logged message
        ServiceReference sref = entry.getServiceReference();
        String message = (sref == null ?
                String.format("[%1$s] %2$s", entry.getBundle().toString(), entry.getMessage()) :
                    String.format("[%1$s:%3$s] %2$s", entry.getBundle().toString(), entry.getMessage(), sref.toString())
        );
 
        // use OfBiz log wrapper
        Debug.log(severityMap[entry.getLevel()], entry.getException(), message, "OSGiLogger");
    }

}
