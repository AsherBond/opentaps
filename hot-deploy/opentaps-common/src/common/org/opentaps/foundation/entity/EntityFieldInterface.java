/*
 * Copyright (c) 2008 - 2009 Open Source Strategies, Inc.
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
