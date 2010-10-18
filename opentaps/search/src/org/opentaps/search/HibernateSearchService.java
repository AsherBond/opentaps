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
package org.opentaps.search;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.search.FullTextQuery;
import org.opentaps.domain.search.CommonSearchService;

/**
 * Base implementation of the search services using Hibernate search.
 */
public abstract class HibernateSearchService extends CommonSearchService {

    /** The default placeholder in Lucene queries. */
    public static final String DEFAULT_PLACEHOLDER = "?";
    /** The regular expression recognizing the default placeholder in Lucene queries. */
    public static final String DEFAULT_PLACEHOLDER_REGEX = "\\?";

    private ArrayList<String> projectedFields;

    /**
     * Adds a field to the query projection, unless it was already set on the projection.
     * Must be called before {@link #search}.
     * @param field a <code>String</code> value
     * @return the index of the field in the results as returned by {@link #getResults}
     */
    protected Integer addQueryProjectedField(String field) {
        if (projectedFields == null) {
            prepareQueryProjectedFields();
        }

        if (!projectedFields.contains(field)) {
            projectedFields.add(field);
        }
        return projectedFields.indexOf(field);
    }

    /**
     * Gets the index of a projected field previously added with {@link #addQueryProjectedField}.
     * @param field a <code>String</code> value
     * @return the index of the field in the results as returned by {@link #getResults}, or <code>-1</code> if the field is not defined in the projection
     */
    public Integer getQueryProjectedFieldIndex(String field) {
        if (projectedFields == null) {
            prepareQueryProjectedFields();
        }

        return projectedFields.indexOf(field);
    }

    /**
     * Gets the Set of projected fields for the query.
     * @return a Set of <code>FullTextQuery.OBJECT_CLASS</code>, <code>FullTextQuery.ID</code>
     */
    public Set<String> getQueryProjectedFields() {
        Set<String> fields = new LinkedHashSet<String>();
        fields.addAll(Arrays.asList(FullTextQuery.OBJECT_CLASS, FullTextQuery.ID));
        return fields;
    }

    /**
     * Adds all fields from {@link #getQueryProjectedFields}.
     */
    protected void prepareQueryProjectedFields() {
        if (projectedFields == null) {
            projectedFields = new ArrayList<String>();
        }

        for (String field : getQueryProjectedFields()) {
            addQueryProjectedField(field);
        }
    }
}
