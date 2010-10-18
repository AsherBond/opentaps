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
package org.opentaps.foundation.entity.util;

import java.util.Comparator;
import java.util.List;

import org.opentaps.foundation.entity.EntityInterface;

/**
 * A simple entity comparator for sort entities in collection.
 * Example : if we want sort orders list by orderId asc and orderName desc, you can write these codes as following
 *
 * Collections.sort(orders, new EntityComparator("orderId,orderName desc"));
 */
public class EntityComparator implements Comparator<EntityInterface> {

    private String orderBy;
    private static final int SORT_ASCENDING = 1;
    private static final int SORT_DESCENDING = -1;

    /**
     * Creates a new <code>EntityComparator</code> instance.
     * @param orderBy an order <code>String</code> value
     */
    public EntityComparator(String orderBy) {
        this.orderBy = orderBy;
    }

    /**
     * Creates a new <code>EntityComparator</code> instance.
     * @param orderByList an order <code>List</code> value
     */
    public EntityComparator(List<String> orderByList) {
        orderBy = "";
        for (String string : orderByList) {
            orderBy = "".equals(orderBy) ? string : orderBy + "," + string;
        }
    }

    /**
     * Compares two entities and return the result.
     * @param entity1 a <code>EntityInterface</code> value
     * @param entity2 a <code>EntityInterface</code> value
     * @return a <code>int</code> value
     */
    public int compare(EntityInterface entity1, EntityInterface entity2) {
        // remove extra space from orign string
        String[] fields = orderBy.replaceAll("\\s+", " ").trim().split(",");
        for (int i = 0; i < fields.length; i++) {
            // remove extra space
            String fieldString = fields[i].trim();
            String[] field = fieldString.split(" ");
            String fieldName = field[0];
            int fieldSortType = SORT_ASCENDING;
            if (field.length > 1) {
                if (field[1].trim().equalsIgnoreCase("DESC")) {
                    fieldSortType = SORT_DESCENDING;
                }
            }
            int result = compare(entity1, entity2, fieldName, fieldSortType);
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }

    /**
     * Compares the order fields of two entities and return the result.
     * @param o1 a <code>EntityInterface</code> value
     * @param o2 a <code>EntityInterface</code> value
     * @param fieldName a <code>String</code> value
     * @param sortType a <code>int</code> value
     * @return a <code>int</code> value
     */
    public int compare(EntityInterface o1, EntityInterface o2, String fieldName, int sortType) {
        if (o1 == null && o2 == null) {
            return 0;
        }
        if (o1 == null && o2 != null) {
            return -1 * sortType;
        }
        if (o1 != null && o2 == null) {
            return sortType;
        }
        if (o1.get(fieldName) == null && o2.get(fieldName) == null) {
            return 0;
        }
        if (o1.get(fieldName) == null && o2.get(fieldName) != null) {
            return -1 * sortType;
        }
        if (o1.get(fieldName) != null && o2.get(fieldName) == null) {
            return sortType;
        }
        return sortType * ((Comparable) o1.get(fieldName)).compareTo(o2.get(fieldName));
    }
}
