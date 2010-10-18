/*
 * Copyright (c) Open Source Strategies, Inc.
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
package org.opentaps.foundation.domain;

import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;

/**
 * This interface defines what a Domain should have.  Generally each Domain returns its own factories, repositories, services, etc.,
 * but the DomainInterface can hold commonly accessed things like User, Infrastructure, so you don't have to set them over and over
 * again.
 */
public interface DomainInterface {

    /**
     * Sets the domain <code>Infrastructure</code>.
     * @param infrastructure an <code>Infrastructure</code> value
     */
    public void setInfrastructure(Infrastructure infrastructure);

    /**
     * Gets the domain <code>Infrastructure</code>.
     * @return an <code>Infrastructure</code> value
     */
    public Infrastructure getInfrastructure();

    /**
     * Sets the domain <code>User</code>.
     * @param user an <code>User</code> value
     */
    public void setUser(User user);

    /**
     * Gets the domain <code>User</code>.
     * @return an <code>User</code> value
     */
    public User getUser();

    /**
     * Sets both the domain <code>Infrastructure</code> and <code>User</code>.
     * @param infrastructure an <code>Infrastructure</code> value
     * @param user an <code>User</code> value
     * @see #setUser
     * @see #setInfrastructure
     */
    public void setInfrastructureAndUser(Infrastructure infrastructure, User user);
}
