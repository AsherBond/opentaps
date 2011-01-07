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
package org.opentaps.dataimport.netsuite;

import org.opentaps.dataimport.ImportDecoder;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.base.util.UtilMisc;

import java.util.List;
import java.util.Map;
import java.sql.Timestamp;

import javolution.util.FastList;

/**
 * TODO: this might be useful as a generic enumeration importer, so long as the description field is named "description" in the import entity
 */
class NetSuiteEnumDecoder implements ImportDecoder {

    protected String enumTypeId;

    public NetSuiteEnumDecoder(String enumTypeId) {
        this.enumTypeId = enumTypeId;
    }

    public List<GenericValue> decode(GenericValue entry, Timestamp importTimestamp, Delegator delegator, LocalDispatcher dispatcher, Object... args) throws Exception {
        List<GenericValue> toBeStored = new FastList<GenericValue>();
        String description = entry.getString("description").trim();

        Map findMap = UtilMisc.toMap("description", description, "enumTypeId", enumTypeId);
        GenericValue industryEnum = EntityUtil.getFirst( delegator.findByAnd("Enumeration", findMap) );
        if (industryEnum == null) {
            industryEnum = delegator.makeValue("Enumeration", findMap);
            industryEnum.put("enumId", delegator.getNextSeqId("Enumeration"));
            // TODO: not sure about the enumCode and sequenceId fields
            toBeStored.add(industryEnum);
        }
        entry.put("enumId", industryEnum.get("enumId"));
        toBeStored.add(entry);
        return toBeStored;
    }
}
