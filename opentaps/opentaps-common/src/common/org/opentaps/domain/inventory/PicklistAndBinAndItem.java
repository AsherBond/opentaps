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
package org.opentaps.domain.inventory;

import org.opentaps.foundation.repository.RepositoryException;

import java.util.List;

public class PicklistAndBinAndItem extends org.opentaps.base.entities.PicklistAndBinAndItem {

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
