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

import javolution.util.FastList;

import java.util.*;

/**
 * ListBuilder for a List of Maps or GenericValues.  This builder can
 * actually handle any kind of List, but it will only perform sorting operations
 * if the elements are Maps, hence the name of the class.
 *
 * To use this kind of builder, simply pass in a List to the list argument of
 * the paginate macro.
 */
public class MapListBuilder extends AbstractListBuilder {

    protected List list = null;
    protected int size = 0;
    protected Comparator comparator = null;

    protected MapListBuilder() {};

    /** Sets up the list, preserving the initial order. */
    public MapListBuilder(List list) throws ListBuilderException {
        this(list, null);
    }

    /** Sets up the list, but sorts it according to the given orderBy. */
    public MapListBuilder(List list, List orderBy) throws ListBuilderException {
        if (list == null) list = FastList.newInstance();

        this.list = list;
        this.size = list.size();

        changeOrderBy(orderBy);
    }

    // TODO:   Maybe remove these from interface ListBuilder?
    public void initialize() throws ListBuilderException {}
    public boolean isInitialized() {
        return true;
    }

    /** Free up resources when session expires. */
    public void close() {
        list = null;
        comparator = null;
    }

    public long getListSize() {
        return size;
    }

    /** This builder is always deterministic. */
    public boolean isDeterministic() {
        return true;
    }

    /**
     * Gets a partial list of a given size starting from a cursor index.
     */
    public List getPartialList(long viewSize, long cursorIndex) throws ListBuilderException {
        int from = (int) cursorIndex;
        int thru = (int) (cursorIndex + viewSize);
        if (thru > size) thru = size; // only bound check we need due to validation of cursorIndex
        return list.subList(from, thru);
    }

    /** Change the order means rebuilding the comparator and sorting the list using it. */
    public void changeOrderBy(List orderBy) {
        if (orderBy == null || orderBy.size() == 0) {
            comparator = null;
        } else {
            this.comparator = new MapComparator(orderBy);
            Collections.sort(this.list, comparator);
        }
    }

    /**
     * Special map comparator similar to OrderByList, but the values are assumed to be Maps that
     * behave like GenericEntities.  That is, this list builder uses a comparator that treats
     * the list elements as data from a relational table.
     */
    public class MapComparator implements Comparator {
        protected List orderBy;

        public MapComparator(List orderBy) {
            this.orderBy = orderBy;
        }

        public List getOrderBy() {
            return orderBy;
        }

        /** Compares one Map against another as if they were rows from a relational table. */
        public int compare(Object leftObj, Object rightObj) {

            // if one of the objects is not a map, then we consider them equal
            if (! (leftObj instanceof Map && rightObj instanceof Map)) return 0;

            int result = 0;
            Map left = (Map) leftObj;
            Map right = (Map) rightObj;

            // compare each field until an inequality is found
            for (Iterator iter = orderBy.iterator(); result == 0 && iter.hasNext(); ) {
                String orderField = (String) iter.next();

                // determine if it is descending
                boolean descending = orderField.endsWith(" DESC");

                // extract the field name if descending
                if (descending) orderField = orderField.substring(0, orderField.indexOf(" DESC"));

                Object leftValue = left.get(orderField);
                Object rightValue = right.get(orderField);

                // note that null values are largest and we're assuming ascending value for now
                if (leftValue == null && rightValue != null) result = 1;
                else if (rightValue == null && leftValue != null) result = -1;
                else if (leftValue instanceof Comparable && rightValue instanceof Comparable) {
                    result = ((Comparable) leftValue).compareTo(rightValue);
                } else {
                    // one of the objects is not comparable, so treat them as equal so they don't get sorted
                    result = 0;
                }

                // swap the sort result if descending
                if (descending) result = -result;
            }

            return result;
        }

        /** Compares two MapComparators by checking if the orderBy lists are equal. */
        public boolean equals(Object other) {
            if (! (other instanceof MapComparator)) return false;
            return orderBy.equals(((MapComparator) other).getOrderBy());
        }
    }
}
