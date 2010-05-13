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
 * Installer service interface. <br>
 * Manages installation steps and user navigation.
 */
public interface OSSInstaller {

    /**
     * Next step URL in installer flow.
     * @param stepId Current step identifier.
     * @return URL
     */
    public String nextUri(String stepId);

    /**
     * Previous step URL in installer flow.
     * @param stepId Current step identifier.
     * @return URL
     */
    public String prevUri(String stepId);

    /**
     * Perform installation calling every step perform() method in
     * the order defined by its sequence property.
     */
    public void run();
}
