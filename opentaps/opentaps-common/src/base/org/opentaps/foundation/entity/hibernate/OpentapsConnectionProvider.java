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
package org.opentaps.foundation.entity.hibernate;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.connection.ConnectionProvider;
import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.jdbc.ConnectionFactory;

/**
 * A strategy for obtaining JDBC connections.
 * <br><br>
 * Implementors might also implement connection pooling.<br>
 * <br>
 * The <tt>ConnectionProvider</tt> interface is not intended to be
 * exposed to the application. Instead it is used internally by
 * Hibernate to obtain connections.<br>
 * <br>
 * Implementors should provide a public default constructor.
 */
public class OpentapsConnectionProvider implements ConnectionProvider {
    private String helperName = null;
    private static final String module = OpentapsConnectionProvider.class.getName();

    /**
     * getter method of helperName.
     * @return a <code>String</code> value
     */
    public String getHelperName() {
        return helperName;
    }

    /**
     * setter method of helperName.
     * @param helperName a <code>String</code> value
     */
    public void setHelperName(String helperName) {
        this.helperName = helperName;
    }

    /**
     * Initialize the connection provider from given properties.
     * @param properties <tt>SessionFactory</tt> properties
     * @throws HibernateException if an error occurs
     */
    public void configure(Properties properties) throws HibernateException {
        if (helperName == null) {
            helperName = (String) properties.get("ofbiz.helperName");
        }
        if (helperName == null) {
            throw new HibernateException("helperName not configured.");
        }
        Debug.logInfo("ofbiz.helperName: " + helperName, module);
    }

    /**
     * Grab a connection, with the autocommit mode specified by
     * <tt>hibernate.connection.autocommit</tt>.
     * @return a JDBC connection
     * @throws SQLException if an error occurs
     */
    public Connection getConnection() throws SQLException {
        try {
            return ConnectionFactory.getConnection(helperName);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
            throw new SQLException(e.getMessage());
        }
    }

    /**
     * Dispose of a used connection.
     * @param connection a JDBC connection
     * @throws SQLException if an error occurs
     */
    public void closeConnection(Connection connection) throws SQLException {
        connection.close();
    }

    /**
     * Does this connection provider support aggressive release of JDBC
     * connections and re-acquistion of those connections (if need be) later?
     * @return a <code>boolean</code> value
     */
    public boolean supportsAggressiveRelease() {
        return false;
    }

    /**
     * Release all resources held by this provider.
     * @throws HibernateException if an error occurs
     */
    public void close() throws HibernateException {
    }
}