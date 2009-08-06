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
