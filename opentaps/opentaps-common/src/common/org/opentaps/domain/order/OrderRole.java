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
package org.opentaps.domain.order;


import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.repository.RepositoryException;

import org.opentaps.domain.party.Party;

public class OrderRole extends org.opentaps.base.entities.OrderRole {

    protected Party party = null;

    public OrderRole() {
        super();
    }

    public Party getParty() throws RepositoryException {
        if (party == null) {
            try {
                party = getRepository().getPartyById(this.getPartyId());
            } catch (EntityNotFoundException e) {
                party = null;
            }
        }
        return party;
    }

    private OrderRepositoryInterface getRepository() {
        return OrderRepositoryInterface.class.cast(repository);
    }
}
