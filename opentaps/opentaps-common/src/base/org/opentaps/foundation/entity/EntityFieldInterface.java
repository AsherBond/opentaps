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
package org.opentaps.foundation.entity;

/**
 * Interface of the entities fields enumeration.
 * The parametrization makes possible to match the type of enumeration to use when retrieving a given entity.
 * @param <T> the entity class it is related too
 */
public interface EntityFieldInterface<T> {

    /**
     * Gets the field name.
     * @return the field name
     */
    public String getName();

    /**
     * Gets the SQL order by string to order by this field ASC.
     * @return SQL order by string field name + ASC
     */
    public String asc();

    /**
     * Gets the SQL order by string to order by this field DESC.
     * @return SQL order by string field name + DESC
     */
    public String desc();

}
