/*
 * Copyright (c) 2007 - 2009 Open Source Strategies, Inc.
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
package org.opentaps.foundation.entity.hibernate;

import java.sql.Types;

import org.hibernate.Hibernate;
import org.hibernate.dialect.MySQLDialect;

/**
 * This class just add Types.LONGVARCHAR support for MySQLDialect.
 *
 */
public class OpentapsMySQLDialect extends MySQLDialect {
    /**
     * OpentapsMySQLDialect constructor.
     *
     */
    public OpentapsMySQLDialect() {
        super();
        // add LONGVARCHAR mapping
        registerHibernateType(Types.LONGVARCHAR, Hibernate.TEXT.getName());
    }
}
