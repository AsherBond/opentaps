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

import org.opentaps.foundation.infrastructure.User;

/**
 * This is the base class for the pojo service wrappers.
 * They wrap the input / output <code>Map</code> and give
 *  type safe accessors to the parameters.
 * This extends the base <code>ServiceWrapper</code> to provide accessor
 *  for the <code>User</code> which is used by services which require authentication.
 * The generated input map is automatically populated with the <code>User</code> if it was given.
 */
public abstract class ServiceWrapperWithAuth extends ServiceWrapper {

    private User user;

    /**
     * Creates a new <code>ServiceWrapperWithAuth</code> instance.
     */
    public ServiceWrapperWithAuth() {
        super();
    }

    /**
     * Creates a new <code>ServiceWrapperWithAuth</code> instance.
     * @param user an <code>User</code> value
     */
    public ServiceWrapperWithAuth(User user) {
        super();
        this.setUser(user);
    }

    /**
     * Gets the <code>User</code> instance of this service.
     * @return an <code>User</code> value
     */
    public User getUser() {
        return this.user;
    }

    /**
     * Sets the <code>User</code> instance that can be used when running the service.
     * @param user an <code>User</code> value
     */
    public void setUser(User user) {
        this.user = user;
    }
}
