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
package org.opentaps.installer.service.impl;

import org.opentaps.installer.Activator;
import org.opentaps.installer.service.InstallerStep;
import org.opentaps.installer.service.OSSInstaller;
import org.osgi.framework.ServiceReference;


/**
 *
 *
 */
public class OSSInstallerImpl implements OSSInstaller {

    /** {@inheritDoc} */
    public String nextUri(String stepId) {
        ServiceReference[] steps = Activator.findInstSteps();

        Integer currentSeqNum = null;
        for (ServiceReference step : steps) {
            String id = (String) step.getProperty(STEP_ID_PROP);
            if (stepId.equals(id)) {
                currentSeqNum = (Integer) step.getProperty(SEQUENCE_PROP);
                break;
            }
        }

        if (currentSeqNum == null) {
            //TODO add error handling
        }

        ServiceReference nextStep =  null;
        Integer temp = Integer.valueOf(9999);
        for (ServiceReference step : steps) {
            Integer sequence = (Integer) step.getProperty(SEQUENCE_PROP);
            if (sequence == null) {
                //TODO: log message
                continue;
            }
            if (sequence.compareTo(currentSeqNum) > 0) {
                if (sequence.compareTo(temp) < 0) {
                    temp = sequence;
                    nextStep = step;
                }
            }
        }

        if (nextStep != null) {
            InstallerStep service = Activator.getInstance().findStep(nextStep);
            if (service != null) {
                return service.actionUrl();
            }
        };

        return null;
    }

    /** {@inheritDoc} */
    public String prevUri(String stepId) {
        ServiceReference[] steps = Activator.findInstSteps();

        Integer currentSeqNum = null;
        for (ServiceReference step : steps) {
            String id = (String) step.getProperty(STEP_ID_PROP);
            if (stepId.equals(id)) {
                currentSeqNum = (Integer) step.getProperty(SEQUENCE_PROP);
                break;
            }
        }

        if (currentSeqNum == null) {
            //TODO add error handling
        }

        ServiceReference nextStep =  null;
        Integer temp = Integer.valueOf(0);
        for (ServiceReference step : steps) {
            Integer sequence = (Integer) step.getProperty(SEQUENCE_PROP);
            if (sequence == null) {
                //TODO: log message
                continue;
            }
            if (sequence.compareTo(currentSeqNum) < 0) {
                if (sequence.compareTo(temp) > 0) {
                    temp = sequence;
                    nextStep = step;
                }
            }
        }

        if (nextStep != null) {
            InstallerStep service = Activator.getInstance().findStep(nextStep);
            if (service != null) {
                return service.actionUrl();
            }
        };

        return null;
    }

    /** {@inheritDoc} */
    public void run() {
    }

}
