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
 * Domain class for CRMSFA account.
 */
public class Account extends Party {

    private Set<Account> subAccounts;

    /**
     * Default constructor.
     */
    public Account() {
        super();
    }

    /**
     * Finds the list of sub <code>Account</code> for this <code>Account</code>.
     * @return the list of sub <code>Account</code> for this parent <code>Account</code>
     * @throws RepositoryException if an error occurs
     */
    public Set<Account> getSubAccounts()  throws RepositoryException {
        if (subAccounts == null) {
            subAccounts = getRepository().getSubAccounts(this);
        }
        return subAccounts;
    }

    /**
     * Returns unique contacts of the account.
     * @return set of <code>Contact</code> of this account
     * @throws RepositoryException if an error occurs
     */
    public Set<Contact> getContacts() throws RepositoryException {
        return getRepository().getContacts(this);
    }
}
