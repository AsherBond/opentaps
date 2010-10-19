package org.opentaps.common.builder;

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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;

/**
 * An example of creating a custom list builder.  This is a list
 * of status items with all CANCELLED ones filtered out and with
 * the StatusType.description added to the list by hand using
 * getRelatedOneCache().
 *
 */
public class CustomStatusListBuilder extends EntityListBuilder {

    /**
     * There's nothing to parametrize for this class other than the delegator,
     * so we can just set static values for the lookup conditions.
     */
    public CustomStatusListBuilder(Delegator delegator) {
        this.delegator = delegator;
        this.entityName = "StatusItem";
        this.where = EntityCondition.makeCondition("statusId", EntityOperator.NOT_LIKE, "%CANCELLED%");
        this.orderBy = UtilMisc.toList("statusTypeId", "description"); // this defines a default order by
        this.options = DISTINCT_READ_OPTIONS;
    }

    /**
     * The default getListSize() and getCompleteList() functions in EntityListBuilder are perfectly
     * fine the way they are.  We need only to overload this method.
     */
    public List getPartialList(long viewSize, long cursorIndex) throws ListBuilderException {
        if (!isInitialized()) {
            initialize();
        }
        try {
            List results = FastList.newInstance();
            List statusItems = iterator.getPartialList((int) cursorIndex, (int) viewSize);

            for (Iterator<GenericValue> iter = statusItems.iterator(); iter.hasNext();) {
                GenericValue item = iter.next();
                GenericValue type = item.getRelatedOneCache("StatusType");

                Map result = FastMap.newInstance();
                result.putAll(item.getAllFields());
                result.put("statusTypeDescription", type.get("description"));
                results.add(result);
            }

            close();
            return results;
        } catch (GenericEntityException e) {
            throw new ListBuilderException(e);
        }
    }

}
