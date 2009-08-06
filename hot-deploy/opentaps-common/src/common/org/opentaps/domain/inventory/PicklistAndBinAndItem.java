/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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
package org.opentaps.domain.inventory;

import org.opentaps.foundation.repository.RepositoryException;

import java.util.List;

public class PicklistAndBinAndItem extends org.opentaps.domain.base.entities.PicklistAndBinAndItem {

    public static final String STATUS_CANCELLED = "PICKLIST_CANCELLED";
    public static final String STATUS_PICKED = "PICKLIST_PICKED";

    public PicklistAndBinAndItem() {
        super();
    }

    private InventoryRepositoryInterface getRepository() {
        return InventoryRepositoryInterface.class.cast(repository);
    }

    public Boolean isOpen() {
        return !(isPicked() || isCancelled());
    }

    public Boolean isPicked() {
        return STATUS_PICKED.equals(this.getStatusId());
    }

    public Boolean isCancelled() {
        return STATUS_CANCELLED.equals(this.getStatusId());
    }
    
    
    
}
