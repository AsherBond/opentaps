package org.opentaps.purchasing.mrp;

import org.ofbiz.service.GenericDispatcher;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;

import java.sql.Timestamp;

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
public abstract class MrpConfiguration {

    protected GenericDispatcher dispatcher = null;
    protected GenericDelegator delegator = null;
    protected String facilityId = null;

    public abstract Timestamp getPlannedEventDate(String productId, Timestamp needByDate, String inventoryEventPlannedTypeId);

}
