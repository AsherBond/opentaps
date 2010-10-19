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
package org.opentaps.warehouse.domain.shipping;

import org.opentaps.domain.shipping.ShippingDomainInterface;
import org.opentaps.domain.shipping.ShippingRepositoryInterface;
import org.opentaps.foundation.domain.Domain;
import org.opentaps.foundation.repository.RepositoryException;

/**
 * This is an implementation of the Shipping domain.
 */
public class ShippingDomain extends Domain implements ShippingDomainInterface {

    /** {@inheritDoc} */
    public ShippingRepositoryInterface getShippingRepository() throws RepositoryException {
        return instantiateRepository(ShippingRepository.class);
    }
}
