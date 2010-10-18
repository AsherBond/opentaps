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
package org.opentaps.foundation.infrastructure;

/**
 * Domain objects that provide an <code>Infrastructure</code> and a <code>User</code>.
 */
public interface DomainContextInterface {

    /**
     * Sets the <code>Infrastructure</code> instance used in this instance.
     * @param infrastructure an <code>Infrastructure</code> value
     */
    public void setInfrastructure(Infrastructure infrastructure);

    /**
     * Gets the <code>Infrastructure</code> instance associated to this instance.
     * @return an <code>Infrastructure</code> value
     */
    public Infrastructure getInfrastructure();

    /**
     * Gets the <code>User</code> instance associated to this instance.
     * @return an <code>User</code> value
     */
    public User getUser();

    /**
     * Sets the <code>User</code> associated to this instance.
     * @param user an <code>User</code> value
     */
    public void setUser(User user);

    /**
     * Sets the domain context for this instance.
     * @param context a <code>DomainContextInterface</code> value
     */
    public void setDomainContext(DomainContextInterface context);

    /**
     * Sets the domain context for this instance.
     * @param infrastructure an <code>Infrastructure</code> value
     * @param user an <code>User</code> value
     */
    public void setDomainContext(Infrastructure infrastructure, User user);

}
