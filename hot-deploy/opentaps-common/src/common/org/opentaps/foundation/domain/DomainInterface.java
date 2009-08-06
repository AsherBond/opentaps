/*
 * Copyright (c) 2007 - 2009 Open Source Strategies, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 *
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
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
