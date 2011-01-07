package org.opentaps.purchasing.mrp;

import org.ofbiz.service.GenericDispatcher;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;

import java.sql.Timestamp;

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
public abstract class MrpConfiguration {

    protected GenericDispatcher dispatcher = null;
    protected Delegator delegator = null;
    protected String facilityId = null;

    public abstract Timestamp getPlannedEventDate(String productId, Timestamp needByDate, String inventoryEventPlannedTypeId);

}
