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
package org.opentaps.domain.inventory;

import org.ofbiz.base.util.StringUtil;

public class InventoryItemTrace extends
        org.opentaps.domain.base.entities.InventoryItemTrace {

    private long detailsSeqNumber = 0;
    private final int SEQ_NUM_MIN_LEN = 5;

    /**
     * This property is next pre-formated number that can used by any new <code>InventoryItemTraceDetail</code> object
     * to assign sequence number to itself.
     *  
     * @return Next sequence number as padded number with minimal length 5
     */
    public String getNextSeqNum() {
        detailsSeqNumber++;
        return StringUtil.padNumberString(Long.valueOf(detailsSeqNumber).toString(), SEQ_NUM_MIN_LEN);
    }
}
