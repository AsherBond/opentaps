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
package org.opentaps.common.jndi;

import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;
import javax.sql.DataSource;

import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.jdbc.ConnectionFactory;

/**
 * The wrapped data source.
 */
public class DataSourceImpl implements Referenceable, DataSource, Serializable, ObjectFactory {

    private static final long serialVersionUID = 1L;

    private static final Class[] STRING_ARG = { "".getClass() };
    private static final Class[] INT_ARG = { Integer.TYPE };
    private static final Class[] BOOLEAN_ARG = { Boolean.TYPE };

    private Reference reference;
    private String datasourceName = "";

    public DataSourceImpl()
    {
    }   

    public DataSourceImpl(String datasourceName)
    {
        this.datasourceName = datasourceName;
    }

    /**
     * Gets datasource name.
     */
    public String getDatasourceName() {
        return this.datasourceName;
    }

    /**
     * Sets datasource name.
     */
    public void setDatasourceName(String datasourceName) {
        this.datasourceName = datasourceName;
    }   

    /** {@inheritDoc} */
    public Connection getConnection() throws SQLException {
        try {
            return (Connection) ConnectionFactory.getConnection(datasourceName);
        } catch (GenericEntityException gee) {
            throw new SQLException(gee.getLocalizedMessage());
        }
    }

    /** {@inheritDoc} */
    public Connection getConnection(String username, String password) throws SQLException {
        return null;
    }

    /** {@inheritDoc} */
    public int getLoginTimeout() {
        return 0;
    }

    /** {@inheritDoc} */
    public void setLoginTimeout(int seconds) { }

    /** {@inheritDoc} */
    public PrintWriter getLogWriter() {
        return null;
    }

    /** {@inheritDoc} */
    public void setLogWriter(PrintWriter out) {}

    /**
     * Returns true if the impl has closed.
     */
    boolean isClosed() {
        return false;
    }

    /** {@inheritDoc} */
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
        Reference ref = (Reference)obj;
        String classname = ref.getClassName();

        Object ds = Class.forName(classname).newInstance();

        for (Enumeration<RefAddr> e = ref.getAll(); e.hasMoreElements(); ) {
            RefAddr attribute = (RefAddr) e.nextElement();
            String propertyName = attribute.getType();
            String value = (String) attribute.getContent();
            String methodName = "set" + propertyName.substring(0,1).toUpperCase(java.util.Locale.ENGLISH) + propertyName.substring(1);
            Method m;
            Object argValue;

            try {
                m = ds.getClass().getMethod(methodName, STRING_ARG);
                argValue = value;
            } catch (NoSuchMethodException nsme) {
                try {
                    m = ds.getClass().getMethod(methodName, INT_ARG);
                    argValue = Integer.valueOf(value);
                } catch (NoSuchMethodException nsme2) {
                    m = ds.getClass().getMethod(methodName, BOOLEAN_ARG);
                    argValue = Boolean.valueOf(value);
                }
            }
            m.invoke(ds, new Object[] { argValue });
        }
        return ds;
    }

    /** {@inheritDoc} */
    public final Reference getReference() throws NamingException
    {
        // These fields will be set by the JNDI server when it decides to
        // materialize a data source. 
        // This hack must is not robust and should be handled in the next revision.
        String className = this.getClass().getName();
        Reference ref = new Reference(className, className, null);

        // Look for all the getXXX methods in the class that take no arguments.
        Method[] methods = this.getClass().getMethods();

        for (int i = 0; i < methods.length; i++) {

            Method m = methods[i];

            // only look for simple getter methods.
            if (m.getParameterTypes().length != 0)
                continue;

            // only non-static methods
            if (Modifier.isStatic(m.getModifiers()))
                continue;

            // Only getXXX methods
            String methodName = m.getName();
            if ((methodName.length() < 5) || !methodName.startsWith("get"))
                continue;

            Class<?> returnType = m.getReturnType();
            if (Integer.TYPE.equals(returnType) || STRING_ARG[0].equals(returnType) || Boolean.TYPE.equals(returnType)) {

                // setSomeProperty
                // 01234
                String propertyName = methodName.substring(3,4).toLowerCase(java.util.Locale.ENGLISH).concat(methodName.substring(4));
                try {
                    Object ov = m.invoke(this, null);

                    //Need to check for nullability for all the properties, otherwise
                    //rather than null, "null" string gets stored in jndi.
                    if (ov != null) {
                        ref.add(new StringRefAddr(propertyName, ov.toString()));
                    }
                } catch (IllegalAccessException iae) {
                } catch (InvocationTargetException ite) {
                }
            }
        }

        return ref;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public boolean isWrapperFor(final Class iface) throws SQLException {
        return false;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public Object unwrap(final Class iface) throws SQLException {
        throw new SQLException("Not implemented");
    }  

}