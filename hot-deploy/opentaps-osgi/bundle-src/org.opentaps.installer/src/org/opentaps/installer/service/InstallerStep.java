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
package org.opentaps.installer.service;

/**
 * Installation step service interface.<br>
 * The service collects model data, able to perform or rollback all actions
 * required at this stage.
 */
public interface InstallerStep {

    /**
     * Service property.<br>
     * Installation step identifier that may be an arbitrary string.
     */
    public static final String STEP_ID_PROP = "step.id";

    /**
     * Service property.<br>
     * The sequence number that defines the place of the step in the overall sequence.<br>
     * 0 > Integer < 9999.
     */
    public static final String SEQUENCE_PROP = "sequence";

    /**
     * Perform installation in the step after all required data are collected.
     */
    public void perform();

    /**
     * Rollback installation in the step if user has canceled process. 
     */
    public void rollback();

    /**
     * Return to caller step URL.
     * @return URL as string that can be assigned to Window.location (JavaScript) property,
     */
    public String actionUrl();
}
