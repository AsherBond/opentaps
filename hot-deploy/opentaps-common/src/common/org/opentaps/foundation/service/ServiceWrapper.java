/*
 * Copyright (c) 2009 - 2009 Open Source Strategies, Inc.
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
package org.opentaps.foundation.service;

import java.util.Map;

/**
 * This is the base class for the pojo service wrappers.
 * They wrap the input / output <code>Map</code> and give
 *  type safe accessors to the parameters.
 */
public abstract class ServiceWrapper {

    /**
     * Gets the service name as used by the service engine.
     * @return the service engine name
     */
    public abstract String name();

    /**
     * Gets the service input <code>Map</code> (can be passed to the dispatcher).
     * @return the service input <code>Map</code>
     */
    public abstract Map<String, Object> inputMap();

    /**
     * Gets the service output <code>Map</code>.
     * @return the service output <code>Map</code>
     */
    public abstract Map<String, Object> outputMap();

    /**
     * Sets all fields from the given input <code>Map</code>.
     * @param mapValue the service input <code>Map</code>
     */
    public abstract void putAllInput(Map<String, Object> mapValue);

    /**
     * Sets all fields from the given output <code>Map</code>.
     * @param mapValue the service output <code>Map</code>
     */
    public abstract void putAllOutput(Map<String, Object> mapValue);

}
