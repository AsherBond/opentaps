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
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
/* This file has been modified by Open Source Strategies, Inc. */
package org.opentaps.foundation.entity.hibernate;

import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.transform.ToListResultTransformer;

/**
 * Opentaps Transformer, use for transformer query result to entity.
 */
public final class OpentapsTransformer {
    /**
     * Constructor.
     */
    private OpentapsTransformer() {}

    /**
     * Each row of results is a <tt>Map</tt> from alias to values/entities.
     */
    public static final AliasToEntityMapResultTransformer ALIAS_TO_ENTITY_MAP =
            AliasToEntityMapResultTransformer.INSTANCE;

    /**
     * Each row of results is a <tt>List</tt>.
     */
    public static final ToListResultTransformer TO_LIST = ToListResultTransformer.INSTANCE;

    /**
     * Creates a resulttransformer that will inject aliased values into.
     * instances of Class via property methods or fields.
     * @param target a <code>Class</code> value
     * @return a <code>ResultTransformer</code> value
     */
    public static ResultTransformer aliasToBean(Class target) {
        return new OpentapsAliasToBeanResultTransformer(target);
    }

}
