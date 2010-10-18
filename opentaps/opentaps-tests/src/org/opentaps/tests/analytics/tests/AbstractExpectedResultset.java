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
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/* This file has been modified by Open Source Strategies, Inc. */

package org.opentaps.tests.analytics.tests;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.ListIterator;

import javolution.util.FastList;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.jdbc.ConnectionFactory;
import org.ofbiz.entity.model.ModelUtil;
import org.ofbiz.service.GenericServiceException;
import org.opentaps.common.util.UtilConfig;

/**
 * This class is just a bridge between expected result object and analytics testing database.<br>
 * Implements <code>ListIterator</code>
 * <p>
 * Usage example:<br><code>
 * class SomeObject {<br>
 * &nbsp;public SomeObject();<br>
 * &nbsp;public String getProperty();<br>
 * &nbsp;public void setProperty(String);<br>
 * }<br>
 * <p>
 * results = new AbstractExpectedResultset<SomeObject>("SomeObjectResults", SomeObject.class)<br>
 * results.reset();<br>
 * ... // working with collection of SomeObject<br>
 * results.store(); // store collection in table "some_object_results"<br>
 * </code>
 * 
 * <code>SomeObject</code> getters may return following types: <code>String, int, Integer, float, Float, double, Double, boolean, Boolean, Timestamp, Date, Time</code>
 */
public class AbstractExpectedResultset<T extends Object> implements ListIterator<T> {

    public static final String module = AbstractExpectedResultset.class.getName();

    Class<T> classInfo = null;

    String jdbcDriver = null;
    String jdbcUrl = null;
    String jdbcUsername = null;
    String jdbcPassword = null;

    String tableName = null;

    Connection connection = null;
    PreparedStatement insertStmt = null;

    public FastList encResults = FastList.newInstance();
    ListIterator<T> iter = null;


    /**
     * Public constructor

     * @param name Table name that conforms entity engine conventions.
     * @param clazz <T> class.
     * @throws GenericServiceException 
     */
    @SuppressWarnings("unchecked")
    public AbstractExpectedResultset(String name, Class<T> clazz) throws GenericServiceException {

        tableName = ModelUtil.javaNameToDbName(name);

        classInfo = clazz;

        iter = (ListIterator<T>)encResults.listIterator();

        jdbcDriver = UtilConfig.getPropertyValue("analytics_testing", "opentaps.analytics_testing.driver");
        jdbcUrl = UtilConfig.getPropertyValue("analytics_testing", "opentaps.analytics_testing.url");
        jdbcUsername = UtilConfig.getPropertyValue("analytics_testing", "opentaps.analytics_testing.username");
        jdbcPassword = UtilConfig.getPropertyValue("analytics_testing", "opentaps.analytics_testing.password");
        if (UtilValidate.isEmpty(jdbcDriver) || UtilValidate.isEmpty(jdbcUrl) || UtilValidate.isEmpty(jdbcUsername) || UtilValidate.isEmpty(jdbcPassword)) {
            throw new GenericServiceException("No connection string is defined to connect to analytics database");
        }

        try {

            Class.forName(jdbcDriver);
            connection = ConnectionFactory.getConnection(jdbcUrl, jdbcUsername, jdbcPassword);

            StringBuffer sqlBuf = new StringBuffer("INSERT INTO ");
            sqlBuf.append(tableName);
            sqlBuf.append(" (");

            List<String> fields = FastList.newInstance();

            Method[] methods = classInfo.getMethods();
            for (Method method : methods) {
                String fieldName = method.getName();
                if (!fieldName.startsWith("get") || fieldName.startsWith("getClass"))
                    continue;

                fieldName = ModelUtil.javaNameToDbName(fieldName.substring(3));
                fields.add(fieldName);
            }

            for (int i = 0; i < fields.size(); i++) {
                sqlBuf.append(fields.get(i));
                if (i < (fields.size() - 1))
                    sqlBuf.append(", ");
            }

            sqlBuf.append(") VALUES (");

            for (int i = 0; i < fields.size(); i++) {
                sqlBuf.append("?");
                if (i < (fields.size() - 1))
                    sqlBuf.append(", ");
            }

            sqlBuf.append(")");

            insertStmt = connection.prepareStatement(sqlBuf.toString());

        } catch (ClassNotFoundException cnfe) {
            throw new GenericServiceException(cnfe);
        } catch (SQLException sqle) {
            throw new GenericServiceException(sqle);
        }
    }

    /* (non-Javadoc)
     * @see java.util.ListIterator#add(java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    public void add(T obj) {
        iter.add(obj);
    }

    /* (non-Javadoc)
     * @see java.util.ListIterator#hasNext()
     */
    public boolean hasNext() {
        return iter.hasNext();
    }

    /* (non-Javadoc)
     * @see java.util.ListIterator#hasPrevious()
     */
    public boolean hasPrevious() {
        return iter.hasPrevious();
    }

    /* (non-Javadoc)
     * @see java.util.ListIterator#next()
     */
    public T next() {
        return iter.next();
    }

    /* (non-Javadoc)
     * @see java.util.ListIterator#nextIndex()
     */
    public int nextIndex() {
        return iter.nextIndex();
    }

    /* (non-Javadoc)
     * @see java.util.ListIterator#previous()
     */
    public T previous() {
        return iter.previous();
    }

    /* (non-Javadoc)
     * @see java.util.ListIterator#previousIndex()
     */
    public int previousIndex() {
        return iter.previousIndex();
    }

    /* (non-Javadoc)
     * @see java.util.ListIterator#remove()
     */
    public void remove() {
        iter.remove();
    }

    /* (non-Javadoc)
     * @see java.util.ListIterator#set(java.lang.Object)
     */
    public void set(T obj) {
        iter.set(obj);
    }

    /**
     * Set iterator to initial state.
     */
    @SuppressWarnings("unchecked")
    public void reset() {
        iter = encResults.listIterator();
    }

    /**
     * Store collection of objects T in database.
     * @throws GenericServiceException 
     */
    @SuppressWarnings("unchecked")
    public void store() throws GenericServiceException {
        try {
            cleanTable();
            for (Object obj : encResults) {
                insertRow((T) obj);
            }
        } catch (IllegalArgumentException iae) {
            throw new GenericServiceException(iae);
        } catch (SQLException sqle) {
            throw new GenericServiceException(sqle);
        } catch (IllegalAccessException iace) {
            throw new GenericServiceException(iace);
        } catch (InvocationTargetException ite) {
            throw new GenericServiceException(ite);
        } catch (SecurityException se) {
            throw new GenericServiceException(se);
        }
    }

    /**
     * Delete all rows in table
     * @throws SQLException 
     * @throws GenericServiceException 
     */
    private void cleanTable() throws SQLException, GenericServiceException {

        Statement stmt = null;

        String message = "Empty table [" + tableName + "]";
        Debug.logInfo(message, module);

        StringBuffer sqlBuf = new StringBuffer("DELETE FROM ");
        sqlBuf.append(tableName);
        if (Debug.verboseOn()) Debug.logVerbose("[deleteTable] sql=" + sqlBuf.toString(), module);
        stmt = connection.createStatement();
        stmt.executeUpdate(sqlBuf.toString());

        try {
                if (stmt != null) stmt.close();
        } catch (SQLException e) {
                throw new GenericServiceException(e);
        }
    }

    /**
     * @param obj
     * @throws IllegalArgumentException
     * @throws SQLException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    @SuppressWarnings("unchecked")
    private void insertRow(T obj) throws IllegalArgumentException, SQLException, IllegalAccessException, InvocationTargetException {

        StringBuffer sqlBuf = new StringBuffer("INSERT INTO ");
        sqlBuf.append(tableName);
        sqlBuf.append(" (");
        
        List<Method> fields = FastList.newInstance();

        Method[] methods = classInfo.getMethods();
        for (Method method : methods) {
            String fieldName = method.getName();
            if (!fieldName.startsWith("get") || fieldName.startsWith("getClass"))
                continue;

            fieldName = ModelUtil.javaNameToDbName(fieldName.substring(3));

            fields.add(method);
            
        }
        
        for (int i = 0; i < fields.size(); i++) {
            Method m = fields.get(i);
            String typeName = m.getReturnType().getSimpleName();
            if ("String".equals(typeName)) {
                insertStmt.setString(i + 1, (String) m.invoke(obj, new Object[0]));
            } else if ("int".equals(typeName) || "Integer".equals(typeName)) {
                insertStmt.setInt(i + 1, (Integer) m.invoke(obj, new Object[0]));
            } else if ("float".equals(typeName) || "Float".equals(typeName)) {
                insertStmt.setFloat(i + 1, (Float)m.invoke(obj, new Object[0]));
            } else if ("double".equals(typeName) || "Double".equals(typeName)) {
                insertStmt.setDouble(i + 1, (Double)m.invoke(obj, new Object[0]));
            } else if ("boolean".equals(typeName) || "Boolean".equals(typeName)) {
                insertStmt.setBoolean(i + 1, (Boolean)m.invoke(obj, new Object[0]));
            } else if ("Timestamp".equals(typeName)) {
                insertStmt.setTimestamp(i + 1, (Timestamp)m.invoke(obj, new Object[0]));
            } else if ("Date".equals(typeName)) {
                insertStmt.setDate(i + 1, (java.sql.Date)m.invoke(obj, new Object[0]));
            } else if ("Time".equals(typeName)) {
                insertStmt.setTime(i + 1, (java.sql.Time)m.invoke(obj, new Object[0]));
            }
            
        }
        
        insertStmt.executeUpdate();
    }
}
