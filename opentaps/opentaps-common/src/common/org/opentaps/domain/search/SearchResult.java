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
package org.opentaps.domain.search;

/**
 * Represents a search result as returned by the {@link SearchServiceInterface}.
 */
public class SearchResult {

    private Class<?> resultClass;
    private Object resultPk;

    /**
     * Creates a new <code>SearchResult</code> instance.
     * @param rClass the result <code>Class</code>
     * @param rPk the result indentifier, either a String or a PK object
     */
    public SearchResult(Class<?> rClass, Object rPk) {
        this.resultClass = rClass;
        this.resultPk = rPk;
    }

    /**
     * Gets this result class.
     * @return this result <code>Class</code>
     */
    public Class<?> getResultClass() {
        return this.resultClass;
    }

    /**
     * Gets this result idnetifier.
     * @return this result identifier either a <code>String</code> or a PK object
     */
    public Object getResultPk() {
        return this.resultPk;
    }

}
