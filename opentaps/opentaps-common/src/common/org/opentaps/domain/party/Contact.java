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
package org.opentaps.domain.party;


import java.util.Set;

import org.opentaps.foundation.repository.RepositoryException;

/**
 * Domain class for CRMSFA contact.
 */
public class Contact extends Party {

    /**
     * Default constructor.
     */
    public Contact() {
        super();
    }

    /**
     * Gets the list of unique accounts associated to this contact.
     * @return the <code>Set</code> of <code>Accounts</code> associated to this contact
     * @throws RepositoryException if an error occurs
     */
    public Set<Account> getAccounts() throws RepositoryException {
        return getRepository().getAccounts(this);
    }
}
