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

import org.ofbiz.base.util.StringUtil;

public class InventoryItemTrace extends
        org.opentaps.base.entities.InventoryItemTrace {

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
