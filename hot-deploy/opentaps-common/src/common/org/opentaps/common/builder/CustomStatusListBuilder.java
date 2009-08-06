package org.opentaps.common.builder;

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

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;

import java.util.List;
import java.util.Iterator;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;

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
    public CustomStatusListBuilder(GenericDelegator delegator) {
        this.delegator = delegator;
        this.entityName = "StatusItem";
        this.where = new EntityExpr("statusId", EntityOperator.NOT_LIKE, "%CANCELLED%");
        this.orderBy = UtilMisc.toList("statusTypeId", "description"); // this defines a default order by
        this.options = DISTINCT_READ_OPTIONS;
    }

    /**
     * The default getListSize() and getCompleteList() functions in EntityListBuilder are perfectly
     * fine the way they are.  We need only to overload this method.
     */
    public List getPartialList(long viewSize, long cursorIndex) throws ListBuilderException {
        if (! isInitialized()) initialize();
        try {
            List results = FastList.newInstance();
            List statusItems = iterator.getPartialList((int) cursorIndex, (int) viewSize);

            for (Iterator iter = statusItems.iterator(); iter.hasNext(); ) {
                GenericValue item = (GenericValue) iter.next();
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
