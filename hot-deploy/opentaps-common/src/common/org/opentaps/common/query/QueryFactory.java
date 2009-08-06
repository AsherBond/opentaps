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
package org.opentaps.common.query;

import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.jdbc.ConnectionFactory;

import java.sql.SQLException;

/**
 * A factory class for building <code>Query</code> objects.
 * @see Query
 */
public class QueryFactory {

    protected static final String DEFAULT_ENTITY_GROUP_NAME = "org.ofbiz";   // Most entities are defined with this group name

    protected GenericDelegator delegator = null;
    protected String entityGroupName = null;    // the group name in entitygroup.xml
    protected String helperName = null;         // the data source name in entityengine.xml, like localderby, localpostgres, etc.

    /**
     * Creates a new <code>QueryFactory</code> instance.
     */
    public QueryFactory() {
        // nothing to do
    }

    /**
     * Creates a new <code>QueryFactory</code> instance for the DEFAULT_ENTITY_GROUP_NAME, org.ofbiz.
     * @param delegator a <code>GenericDelegator</code> value
     */
    public QueryFactory(GenericDelegator delegator) {
        this(delegator, DEFAULT_ENTITY_GROUP_NAME);
    }

    /**
     * Creates a new <code>QueryFactory</code> instance for the given entity group name.
     * @param delegator a <code>GenericDelegator</code> value
     * @param entityGroupName the entity group name, ie: "org.ofbiz"
     */
    public QueryFactory(GenericDelegator delegator, String entityGroupName) {
        this.delegator = delegator;
        this.entityGroupName = entityGroupName;
        this.helperName = delegator.getGroupHelperName(entityGroupName);
    }

    /**
     * Creates a <code>Query</code> object from the SQL string.
     * @param sql an SQL query string
     * @return a <code>Query</code> instance
     * @throws QueryException if an error occurs
     */
    public Query createQuery(String sql) throws QueryException {
        Query query = new Query();
        query.setDelegator(this.delegator);
        try {
            query.setConnection(ConnectionFactory.getConnection(this.helperName));
            query.setStatement(sql);
        } catch (SQLException e) {
            throw new QueryException(e);
        } catch (GenericEntityException e) {
            throw new QueryException(e);
        }

        return query;
    }


}
