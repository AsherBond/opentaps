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
package org.opentaps.common.query;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityConfException;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.model.ModelField;
import org.ofbiz.entity.model.ModelFieldTypeReader;
import org.ofbiz.entity.model.ModelGroupReader;
import org.ofbiz.entity.model.ModelUtil;
import org.ofbiz.entity.util.EntityListIterator;

/**
 * A class to perform SQL queries an return <code>GenericValues</code>.
 * @see QueryFactory
 */
public class Query {

    // the delegator and entity helpers are only used to cast results to GenericValues
    protected Delegator delegator = null;
    protected Connection connection = null;
    private PreparedStatement preparedStatement = null;
    protected ResultSet resultSet = null;
    protected ResultSetMetaData resultSetMetaData = null;
    protected List<String> columnNames = null;
    protected List<Map<String, Object>> resultRows = null;       // holds List of resultRows after query has been executed

    /**
     * Creates a new <code>Query</code> instance.
     */
    public Query() {
        // nothing needs to happen
    }

    /**
     * Sets the <code>Connection</code>.
     * @param connection a <code>Connection</code> value
     */
    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    /**
     * Sets the <code>Delegator</code>.
     * @param delegator a <code>Delegator</code> value
     */
    public void setDelegator(Delegator delegator) {
        this.delegator = delegator;
    }

    /**
     * Clears query results.  Will close the resultSet if it is not null.
     * @throws QueryException if an error occurs
     */
    public void clearQueryResults() throws QueryException {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                throw new QueryException(e);
            }
        }
        resultSet = null;
        resultSetMetaData = null;
        columnNames = null;
        resultRows = null;
    }

    /**
     * Creates a PreparedStatement from the SQL string and clears the query results in preparation for another query.
     * @param sqlStatement a <code>String</code> representing an SQL statement
     * @throws QueryException if an error occurs
     */
    public void setStatement(String sqlStatement) throws QueryException {
        if (connection == null) {
            throw new QueryException("No connection has been configured for Query");
        } else {
            try {
                preparedStatement = connection.prepareStatement(sqlStatement);
                clearQueryResults();
            } catch (SQLException ex) {
                throw new QueryException(ex.getMessage());
            }
        }
    }

    /**
     * Gets the <code>PreparedStatement</code> represented by this query.
     * @return a <code>PreparedStatement</code> value
     * @deprecated
     */
    public PreparedStatement getStatement() {
        return preparedStatement;
    }

    /**
     * Execute the prepared query and set its resultSet, resultSetMetaData, and the column names of the result.
     * ResultSet IS NOT closed after this method is called.
     * @throws QueryException if an error occurs
     */
    public void doQuery() throws QueryException {
        if (preparedStatement == null) {
            throw new QueryException(UtilProperties.getPropertyValue("OpentapsErrorLabels", "OpentapsError_NoPreparedStatement"));
        } else {
            try {
                if (execute()) {
                    // product of execute() is ResultSet
                    clearQueryResults();
                    resultSet = getResultSet();
                    resultSetMetaData = resultSet.getMetaData();
                    columnNames = getColumnNames(resultSetMetaData);
                }
            } catch (SQLException ex) {
                throw new QueryException(ex);
            }
        }
    }

    /**
     * Converts the column names of a <code>ResultSetMetaData</code> from SQL format to Java format, ie "ORDER_ITEM_TYPE_ID" to "orderItemTypeId".
     *
     * @param resultSetMetaData a <code>ResultSetMetaData</code> to convert
     * @return the <code>List</code> of column names converted
     * @throws QueryException if an error occurs
     */
    public static List<String> getColumnNames(ResultSetMetaData resultSetMetaData) throws QueryException {
        try {
            int numberColumns = resultSetMetaData.getColumnCount();
            List<String> columnNames = FastList.newInstance();
            // RSM columns index start with 1
            for (int i = 1; i <= numberColumns; i++) {
                columnNames.add(ModelUtil.dbNameToVarName(resultSetMetaData.getColumnName(i)));
            }
        return columnNames;
        } catch (SQLException ex) {
            throw new QueryException(ex);
        }
    }

    /**
     * Gets the <code>ResultSetMetaData</code> for this query.
     * @return a <code>ResultSetMetaData</code> value
     */
    public ResultSetMetaData getResultSetMetaData() {
        return resultSetMetaData;
    }

    /**
     * Gets the column names for this query.
     * @return a <code>List</code> of column names
     */
    public List<String> getColumns() {
        return columnNames;
    }

    /**
     * If the query has not been run, runs the query, then returns the query result as a <code>List</code> or <code>Map</code>.
     * If it has been run previously, just returns the <code>List</code>.
     * ResultSet IS closed after this method is called.
     * @return the results as a <code>List</code> of <code>Map</code> (list of rows)
     * @throws QueryException if an error occurs
     * @see #iterator
     */
    public List<Map<String, Object>> list() throws QueryException {
        // if there is no resultSet yet, then the query must not have run
        if (resultSet == null) {
            doQuery();
        }
        // if there is a resultSet but the rows aren't stored yet, then parse the rows
        if ((resultSet != null) && (resultRows == null)) {
            try {
                resultRows = FastList.newInstance();
                // check that there are columns to the results
                int numberColumns = resultSetMetaData.getColumnCount();
                if (numberColumns < 1) {
                    throw new QueryException("There are fewer than one column for the Query");
                }
                // get the columns
                List<String> columns = getColumns();
                // loop through and create a List of Maps with keys being column names
                while (resultSet.next()) {
                    Map<String, Object> row = FastMap.newInstance();
                    // the List of columns is a Java List which starts at 0, but JDBC resultSet getObject(..) starts at 1, so we need to offset the index of the columns
                    for (int i = 1; i <= numberColumns; i++) {
                        int j = i - 1;
                        row.put(columns.get(j), resultSet.getObject(i));
                    }
                    resultRows.add(row);
                }
                resultSet.close();
            } catch (SQLException e) {
                throw new QueryException(e);
            }
        }

        // since we're done and have saved the results, let's close it for now to keep open connections down
        return resultRows;

    }

    /**
     * Returns the results as an <code>Iterator</code> instead of a <code>List</code>.
     * @return the results as a <code>Iterator</code> of <code>Map</code> (list of rows)
     * @throws QueryException if an error occurs
     * @see #list
     */
    public Iterator<Map<String, Object>> iterator() throws QueryException {
        List<Map<String, Object>> rows = list();
        if (UtilValidate.isEmpty(rows)) {
            throw new QueryException("No list found for Query");
        }
        return rows.iterator();
    }

    /**
     * Returns the first row of the results.
     * @return the first row of the results
     * @throws QueryException if an error occurs
     */
    public Map<String, Object> firstResult() throws QueryException {
        List<Map<String, Object>> rows = list();
        if (UtilValidate.isEmpty(rows)) {
            throw new QueryException("No list found for Query");
        }
        return rows.get(0);
    }

    /**
     * For an entity name, ie "StatusItem", returns its entitygroup.xml name, "org.ofbiz".
     * @param delegator a <code>Delegator</code> value
     * @param entityName the entity name, ie "StatusItem"
     * @return the entitygroup.xml name for the given entity name, ie "org.ofbiz"
     * @throws GenericEntityConfException if an error occurs
     */
    public static String getHelperName(Delegator delegator, String entityName) throws GenericEntityConfException {
        // wow, is this a round trip or what?
        ModelGroupReader mgr = ModelGroupReader.getModelGroupReader(delegator.getDelegatorName());
        String groupName = mgr.getEntityGroupName(entityName, delegator.getDelegatorName());
        return delegator.getGroupHelperName(groupName);
    }

    /**
     * Returns a <code>List</code> of <code>ModelField</code> from the given fieldNames (ie, "statusId") and the given entityName (ie, "StatusItem").
     * @param delegator a <code>Delegator</code> value
     * @param entityName the entity name, ie "StatusItem"
     * @param fieldNames a <code>List</code> of field name
     * @return the <code>List</code> of <code>ModelField</code> for the given entity name and list of field names
     */
    public static List<ModelField> getModelFieldsFromNames(Delegator delegator, String entityName, List<String> fieldNames) {
        ModelEntity modelEntity = delegator.getModelEntity(entityName);
        List<ModelField> modelFields = FastList.newInstance();
        for (Object fieldName : fieldNames) {
            modelFields.add(modelEntity.getField((String) fieldName));
        }
        return modelFields;
    }

    /**
     * If this query has not been run, runs the query first; otherwise, casts results of this query as list of <code>GenericValue</code> for the given entityName (ie, "StatusItem")
     * and the given fields to be cast in entityFieldNames (ie, {"statusId", "description", "statusTypeId"}).
     * ResultSet IS NOT closed after this method is called.
     * @param entityName the entity name, ie "StatusItem"
     * @param entityFieldNames the <code>List</code> of field name of the entity that should be included
     * @return an <code>EntityListIterator</code>
     * @throws QueryException if an error occurs
     * @throws GenericEntityException if an error occurs
     * @see #entityListIterator(String)
     */
    public EntityListIterator entityListIterator(String entityName, List<String> entityFieldNames) throws QueryException, GenericEntityException {
        if (delegator == null) {
            throw new QueryException("Cannot cast a query to GenericValue without a delegator");
        }
        // if there is no resultSet yet, then the query must not have run
        if (resultSet == null) {
            doQuery();
        }

        return new EntityListIterator(resultSet, delegator.getModelEntity(entityName),
                getModelFieldsFromNames(delegator, entityName, entityFieldNames), ModelFieldTypeReader.getModelFieldTypeReader(getHelperName(delegator, entityName)));
    }

    /**
     *If this query has not been run, runs the query first; otherwise, casts results of this query as list of <code>GenericValue</code> for the given entityName (ie, "StatusItem")
     * with all the entity fields.
     * ResultSet IS NOT closed after this method is called.
     * @param entityName the entity name, ie "StatusItem"
     * @return an <code>EntityListIterator</code>
     * @throws QueryException if an error occurs
     * @throws GenericEntityException if an error occurs
     * @see #entityListIterator(String, List)
     */
    public EntityListIterator entityListIterator(String entityName) throws QueryException, GenericEntityException {
        if (delegator == null) {
            throw new QueryException("Cannot cast a query to GenericValue without a delegator");
        }
        // if there is no resultSet yet, then the query must not have run
        if (resultSet == null) {
            doQuery();
        }

        // get a list of all ModelFields from the model entity
        ModelEntity modelEntity = delegator.getModelEntity(entityName);
        if (modelEntity == null) {
            throw new QueryException("Cannot cast [" + entityName + "] to a ModelEntity");
        }
        return new EntityListIterator(resultSet, delegator.getModelEntity(entityName),
                modelEntity.getFieldsUnmodifiable(), ModelFieldTypeReader.getModelFieldTypeReader(getHelperName(delegator, entityName)));

    }

    /**
     * Converts an <code>EntityListIterator</code> into a <code>List</code> of <code>GenericValue</code>.
     * @param eli an <code>EntityListIterator</code>
     * @return a <code>List</code> of <code>GenericValue</code>
     * @throws QueryException if an error occurs
     * @throws GenericEntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    private List<GenericValue> eliToList(EntityListIterator eli) throws QueryException, GenericEntityException {
        if (eli == null) {
            throw new QueryException("No EntityListIterator returned for Query");
        }
        List<GenericValue> values = eli.getCompleteList();
        // this should close the ResultSet and matches the behavior of .toList() method above
        eli.close();
        return values;
    }

    /**
     * Calls {@link #entityListIterator(String, List)} and returns a <code>List</code> of <code>GenericValue</code> entities.
     * ResultSet IS closed after this method is called.
     * @param entityName the entity name, ie "StatusItem"
     * @param entityFieldNames the <code>List</code> of field name of the entity that should be included
     * @return a <code>List</code> of <code>GenericValue</code>
     * @throws QueryException if an error occurs
     * @throws GenericEntityException if an error occurs
     */

    public List<GenericValue> entitiesList(String entityName, List<String> entityFieldNames) throws QueryException, GenericEntityException {
        EntityListIterator eli = entityListIterator(entityName, entityFieldNames);
        return eliToList(eli);
    }

    /**
     * Calls {@link #entityListIterator(String, List)} and returns a <code>List</code> of <code>GenericValue</code> entities.
     * ResultSet IS closed after this method is called.
     * @param entityName the entity name, ie "StatusItem"
     * @return a <code>List</code> of <code>GenericValue</code>
     * @throws QueryException if an error occurs
     * @throws GenericEntityException if an error occurs
     */
    public List<GenericValue> entitiesList(String entityName) throws QueryException, GenericEntityException {
        EntityListIterator eli = entityListIterator(entityName);
        return eliToList(eli);
    }

    @Override
    protected void finalize() throws Throwable {
        if (this.resultSet != null) {
            this.resultSet.close();
        }
        super.finalize();
    }

    /**
     * A little method to check that <code>PreparedStatement</code> operations below can be performed.
     * @throws IllegalStateException if an error occurs
     */
    private void checkValidity() throws IllegalStateException {
        if (preparedStatement == null) {
            throw new IllegalStateException(UtilProperties.getPropertyValue("OpentapsErrorLabels", "OpentapsError_NoPreparedStatement"));
        }
    }

    /*
     * All methods below are implementation of PreparedStatement interface.
     * They actually wrap calls to the PreparedStatement interface to the internal statement object stored in preparedStatement.
     */

    /**
     * From {@link java.sql.Statement#getResultSet()}.
     * @return a <code>ResultSet</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.Statement#getResultSet()
     */
    public ResultSet getResultSet() throws SQLException {
        if (preparedStatement == null) {
            throw new IllegalStateException(UtilProperties.getPropertyValue("OpentapsErrorLabels", "OpentapsError_NoPreparedStatement"));
        }

        if (resultSet == null) {
            return preparedStatement.getResultSet();
        } else {
            return resultSet;
        }
    }

    /**
     * From {@link java.sql.PreparedStatement#addBatch()}.
     * @exception SQLException if an error occurs
     * @see java.sql.PreparedStatement#addBatch()
     */
    public void addBatch() throws SQLException {
        checkValidity();
        preparedStatement.addBatch();
    }

    /**
     * From {@link java.sql.PreparedStatement#clearParameters()}.
     * @exception SQLException if an error occurs
     * @see java.sql.PreparedStatement#clearParameters()
     */
    public void clearParameters() throws SQLException {
        checkValidity();
        preparedStatement.clearParameters();
    }

    /**
     * From {@link java.sql.PreparedStatement#execute()}.
     * @return a <code>boolean</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.PreparedStatement#execute()
     */
    public boolean execute() throws SQLException {
        checkValidity();
        return preparedStatement.execute();
    }

    /**
     * From {@link java.sql.PreparedStatement#executeUpdate()}.
     * @return an <code>int</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.PreparedStatement#executeUpdate()
     */
    public int executeUpdate() throws SQLException {
        checkValidity();
        return preparedStatement.executeUpdate();
    }

    /**
     * From {@link java.sql.PreparedStatement#getMetaData()}.
     * @return a <code>ResultSetMetaData</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.PreparedStatement#getMetaData()
     */
    public ResultSetMetaData getMetaData() throws SQLException {
        checkValidity();
        return preparedStatement.getMetaData();
    }

    /**
     * From {@link java.sql.PreparedStatement#getParameterMetaData()}.
     * @return a <code>ParameterMetaData</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.PreparedStatement#getParameterMetaData()
     */
    public ParameterMetaData getParameterMetaData() throws SQLException {
        checkValidity();
        return preparedStatement.getParameterMetaData();
    }

    /**
     * From {@link java.sql.PreparedStatement#setArray(int, java.sql.Array)}.
     * @param i an <code>int</code> value
     * @param x an <code>Array</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.PreparedStatement#setArray(int, java.sql.Array)
     */
    public void setArray(int i, Array x) throws SQLException {
        checkValidity();
        preparedStatement.setArray(i, x);
    }

    /**
     * From {@link java.sql.PreparedStatement#setAsciiStream(int, java.io.InputStream, int)}.
     * @param parameterIndex an <code>int</code> value
     * @param x an <code>InputStream</code> value
     * @param length an <code>int</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.PreparedStatement#setAsciiStream(int, java.io.InputStream, int)
     */
    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
        checkValidity();
        preparedStatement.setAsciiStream(parameterIndex, x, length);
    }

    /**
     * From {@link java.sql.PreparedStatement#setBigDecimal(int, java.math.BigDecimal)}.
     * @param parameterIndex an <code>int</code> value
     * @param x a <code>BigDecimal</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.PreparedStatement#setBigDecimal(int, java.math.BigDecimal)
     */
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        checkValidity();
        preparedStatement.setBigDecimal(parameterIndex, x);
    }

    /**
     * From {@link java.sql.PreparedStatement#setBinaryStream(int, java.io.InputStream, int)}.
     * @param parameterIndex an <code>int</code> value
     * @param x an <code>InputStream</code> value
     * @param length an <code>int</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.PreparedStatement#setBinaryStream(int, java.io.InputStream, int)
     */
    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
        checkValidity();
        preparedStatement.setBinaryStream(parameterIndex, x, length);
    }

    /**
     * From {@link java.sql.PreparedStatement#setBlob(int, java.sql.Blob)}.
     * @param i an <code>int</code> value
     * @param x a <code>Blob</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.PreparedStatement#setBlob(int, java.sql.Blob)
     */
    public void setBlob(int i, Blob x) throws SQLException {
        checkValidity();
        preparedStatement.setBlob(i, x);
    }

    /**
     * From {@link java.sql.PreparedStatement#setBoolean(int, boolean)}.
     * @param parameterIndex an <code>int</code> value
     * @param x a <code>boolean</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.PreparedStatement#setBoolean(int, boolean)
     */
    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        checkValidity();
        preparedStatement.setBoolean(parameterIndex, x);
    }

    /**
     * From {@link java.sql.PreparedStatement#setByte(int, byte)}.
     * @param parameterIndex an <code>int</code> value
     * @param x a <code>byte</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.PreparedStatement#setByte(int, byte)
     */
    public void setByte(int parameterIndex, byte x) throws SQLException {
        checkValidity();
        preparedStatement.setByte(parameterIndex, x);
    }

    /**
     * From {@link java.sql.PreparedStatement#setBytes(int, byte[])}.
     * @param parameterIndex an <code>int</code> value
     * @param x a <code>byte</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.PreparedStatement#setBytes(int, byte[])
     */
    public void setBytes(int parameterIndex, byte[] x) throws SQLException {
        checkValidity();
        preparedStatement.setBytes(parameterIndex, x);
    }

    /**
     * From {@link java.sql.PreparedStatement#setCharacterStream(int, java.io.Reader, int)}.
     * @param parameterIndex an <code>int</code> value
     * @param reader a <code>Reader</code> value
     * @param length an <code>int</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.PreparedStatement#setCharacterStream(int, java.io.Reader, int)
     */
    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
        checkValidity();
        preparedStatement.setCharacterStream(parameterIndex, reader, length);
    }

    /**
     * From {@link java.sql.PreparedStatement#setClob(int, java.sql.Clob)}.
     * @param i an <code>int</code> value
     * @param x a <code>Clob</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.PreparedStatement#setClob(int, java.sql.Clob)
     */
    public void setClob(int i, Clob x) throws SQLException {
        checkValidity();
        preparedStatement.setClob(i, x);
    }

    /**
     * From {@link java.sql.PreparedStatement#setDate(int, java.sql.Date)}.
     * @param parameterIndex an <code>int</code> value
     * @param x a <code>Date</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.PreparedStatement#setDate(int, java.sql.Date)
     */
    public void setDate(int parameterIndex, Date x) throws SQLException {
        checkValidity();
        preparedStatement.setDate(parameterIndex, x);
    }

    /**
     * From {@link java.sql.PreparedStatement#setDate(int, java.sql.Date, java.util.Calendar)}.
     * @param parameterIndex an <code>int</code> value
     * @param x a <code>Date</code> value
     * @param cal a <code>Calendar</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.PreparedStatement#setDate(int, java.sql.Date, java.util.Calendar)
     */
    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
        checkValidity();
        preparedStatement.setDate(parameterIndex, x, cal);
    }

    /**
     * From {@link java.sql.PreparedStatement#setDouble(int, double)}.
     * @param parameterIndex an <code>int</code> value
     * @param x a <code>double</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.PreparedStatement#setDouble(int, double)
     */
    public void setDouble(int parameterIndex, double x) throws SQLException {
        checkValidity();
        preparedStatement.setDouble(parameterIndex, x);
    }

    /**
     * From {@link java.sql.PreparedStatement#setFloat(int, float)}.
     * @param parameterIndex an <code>int</code> value
     * @param x a <code>float</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.PreparedStatement#setFloat(int, float)
     */
    public void setFloat(int parameterIndex, float x) throws SQLException {
        checkValidity();
        preparedStatement.setFloat(parameterIndex, x);
    }

    /**
     * From {@link java.sql.PreparedStatement#setInt(int, int)}.
     * @param parameterIndex an <code>int</code> value
     * @param x an <code>int</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.PreparedStatement#setInt(int, int)
     */
    public void setInt(int parameterIndex, int x) throws SQLException {
        checkValidity();
        preparedStatement.setInt(parameterIndex, x);
    }

    /**
     * From {@link java.sql.PreparedStatement#setLong(int, long)}.
     * @param parameterIndex an <code>int</code> value
     * @param x a <code>long</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.PreparedStatement#setLong(int, long)
     */
    public void setLong(int parameterIndex, long x) throws SQLException {
        checkValidity();
        preparedStatement.setLong(parameterIndex, x);
    }

    /**
     * From {@link java.sql.PreparedStatement#setNull(int, int)}.
     * @param parameterIndex an <code>int</code> value
     * @param sqlType an <code>int</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.PreparedStatement#setNull(int, int)
     */
    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        checkValidity();
        preparedStatement.setNull(parameterIndex, sqlType);
    }

    /**
     * From {@link java.sql.PreparedStatement#setNull(int, int, java.lang.String)}.
     * @param paramIndex an <code>int</code> value
     * @param sqlType an <code>int</code> value
     * @param typeName a <code>String</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.PreparedStatement#setNull(int, int, java.lang.String)
     */
    public void setNull(int paramIndex, int sqlType, String typeName) throws SQLException {
        checkValidity();
        preparedStatement.setNull(paramIndex, sqlType, typeName);
    }

    /**
     * From {@link java.sql.PreparedStatement#setObject(int, java.lang.Object)}.
     * @param parameterIndex an <code>int</code> value
     * @param x an <code>Object</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.PreparedStatement#setObject(int, java.lang.Object)
     */
    public void setObject(int parameterIndex, Object x) throws SQLException {
        checkValidity();
        preparedStatement.setObject(parameterIndex, x);
    }

    /**
     * From {@link java.sql.PreparedStatement#setObject(int, java.lang.Object, int)}.
     * @param parameterIndex an <code>int</code> value
     * @param x an <code>Object</code> value
     * @param targetSqlType an <code>int</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.PreparedStatement#setObject(int, java.lang.Object, int)
     */
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
        checkValidity();
        preparedStatement.setObject(parameterIndex, x, targetSqlType);
    }

    /**
     * From {@link java.sql.PreparedStatement#setObject(int, java.lang.Object, int, int)}.
     * @param parameterIndex an <code>int</code> value
     * @param x an <code>Object</code> value
     * @param targetSqlType an <code>int</code> value
     * @param scale an <code>int</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.PreparedStatement#setObject(int, java.lang.Object, int, int)
     */
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scale) throws SQLException {
        checkValidity();
        preparedStatement.setObject(parameterIndex, x, targetSqlType, scale);
    }

    /**
     * From {@link java.sql.PreparedStatement#setRef(int, java.sql.Ref)}.
     * @param i an <code>int</code> value
     * @param x a <code>Ref</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.PreparedStatement#setRef(int, java.sql.Ref)
     */
    public void setRef(int i, Ref x) throws SQLException {
        checkValidity();
        preparedStatement.setRef(i, x);
    }

    /**
     * From {@link java.sql.PreparedStatement#setShort(int, short)}.
     * @param parameterIndex an <code>int</code> value
     * @param x a <code>short</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.PreparedStatement#setShort(int, short)
     */
    public void setShort(int parameterIndex, short x) throws SQLException {
        checkValidity();
        preparedStatement.setShort(parameterIndex, x);
    }

    /**
     * From {@link java.sql.PreparedStatement#setString(int, java.lang.String)}.
     * @param parameterIndex an <code>int</code> value
     * @param x a <code>String</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.PreparedStatement#setString(int, java.lang.String)
     */
    public void setString(int parameterIndex, String x) throws SQLException {
        checkValidity();
        preparedStatement.setString(parameterIndex, x);
    }

    /**
     * From {@link java.sql.PreparedStatement#setTime(int, java.sql.Time)}.
     * @param parameterIndex an <code>int</code> value
     * @param x a <code>Time</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.PreparedStatement#setTime(int, java.sql.Time)
     */
    public void setTime(int parameterIndex, Time x) throws SQLException {
        checkValidity();
        preparedStatement.setTime(parameterIndex, x);
    }

    /**
     * From {@link java.sql.PreparedStatement#setTime(int, java.sql.Time, java.util.Calendar)}.
     * @param parameterIndex an <code>int</code> value
     * @param x a <code>Time</code> value
     * @param cal a <code>Calendar</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.PreparedStatement#setTime(int, java.sql.Time, java.util.Calendar)
     */
    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
        checkValidity();
        preparedStatement.setTime(parameterIndex, x, cal);
    }

    /**
     * From {@link java.sql.PreparedStatement#setTimestamp(int, java.sql.Timestamp)}.
     * @param parameterIndex an <code>int</code> value
     * @param x a <code>Timestamp</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.PreparedStatement#setTimestamp(int, java.sql.Timestamp)
     */
    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
        checkValidity();
        preparedStatement.setTimestamp(parameterIndex, x);
    }

    /**
     * From {@link java.sql.PreparedStatement#setTimestamp(int, java.sql.Timestamp, java.util.Calendar)}.
     * @param parameterIndex an <code>int</code> value
     * @param x a <code>Timestamp</code> value
     * @param cal a <code>Calendar</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.PreparedStatement#setTimestamp(int, java.sql.Timestamp, java.util.Calendar)
     */
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
        checkValidity();
        preparedStatement.setTimestamp(parameterIndex, x, cal);
    }

    /**
     * From {@link java.sql.PreparedStatement#setURL(int, java.net.URL)}.
     * @param parameterIndex an <code>int</code> value
     * @param x an <code>URL</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.PreparedStatement#setURL(int, java.net.URL)
     */
    public void setURL(int parameterIndex, URL x) throws SQLException {
        checkValidity();
        preparedStatement.setURL(parameterIndex, x);
    }

    /**
     * From {@link java.sql.PreparedStatement#setUnicodeStream(int, java.io.InputStream, int)}.
     * @param parameterIndex an <code>int</code> value
     * @param x an <code>InputStream</code> value
     * @param length an <code>int</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.PreparedStatement#setUnicodeStream(int, java.io.InputStream, int)
     */
    @SuppressWarnings("deprecation")
    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
        checkValidity();
        preparedStatement.setUnicodeStream(parameterIndex, x, length);
    }

    /**
     * From {@link java.sql.Statement#addBatch(java.lang.String)}.
     * @param sql a <code>String</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.Statement#addBatch(java.lang.String)
     */
    public void addBatch(String sql) throws SQLException {
        checkValidity();
        preparedStatement.addBatch(sql);
    }

    /**
     * From {@link java.sql.Statement#cancel()}.
     * @exception SQLException if an error occurs
     * @see java.sql.Statement#cancel()
     */
    public void cancel() throws SQLException {
        checkValidity();
        preparedStatement.cancel();
    }

    /**
     * From {@link java.sql.Statement#clearBatch()}.
     * @exception SQLException if an error occurs
     * @see java.sql.Statement#clearBatch()
     */
    public void clearBatch() throws SQLException {
        checkValidity();
        preparedStatement.clearBatch();
    }

    /**
     * From {@link java.sql.Statement#clearWarnings()}.
     * @exception SQLException if an error occurs
     * @see java.sql.Statement#clearWarnings()
     */
    public void clearWarnings() throws SQLException {
        checkValidity();
        preparedStatement.clearWarnings();
    }

    /**
     * From {@link java.sql.Statement#close()}.
     * @exception SQLException if an error occurs
     * @see java.sql.Statement#close()
     */
    public void close() throws SQLException {
        checkValidity();
        preparedStatement.close();
        //  Make sure the connection is closed.  see http://sourceforge.net/forum/message.php?msg_id=7490472
        connection.close();
    }

    /**
     * From {@link java.sql.Statement#execute(java.lang.String)}.
     * @param sql a <code>String</code> value
     * @return a <code>boolean</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.Statement#execute(java.lang.String)
     */
    public boolean execute(String sql) throws SQLException {
        checkValidity();
        return preparedStatement.execute(sql);
    }

    /**
     * From {@link java.sql.Statement#execute(java.lang.String, int)}.
     * @param sql a <code>String</code> value
     * @param autoGeneratedKeys an <code>int</code> value
     * @return a <code>boolean</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.Statement#execute(java.lang.String, int)
     */
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        checkValidity();
        return preparedStatement.execute(sql, autoGeneratedKeys);
    }

    /**
     * From {@link java.sql.Statement#execute(java.lang.String, int[])}.
     * @param sql a <code>String</code> value
     * @param columnIndexes an <code>int</code> value
     * @return a <code>boolean</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.Statement#execute(java.lang.String, int[])
     */
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        checkValidity();
        return preparedStatement.execute(sql, columnIndexes);
    }

    /**
     * From {@link java.sql.Statement#execute(java.lang.String, java.lang.String[])}.
     * @param sql a <code>String</code> value
     * @param columnNames a <code>String</code> value
     * @return a <code>boolean</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.Statement#execute(java.lang.String, java.lang.String[])
     */
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        checkValidity();
        return preparedStatement.execute(sql, columnNames);
    }

    /**
     * From {@link java.sql.Statement#executeBatch()}.
     * @return an <code>int[]</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.Statement#executeBatch()
     */
    public int[] executeBatch() throws SQLException {
        checkValidity();
        return preparedStatement.executeBatch();
    }

    /**
     * From {@link java.sql.Statement#executeQuery(java.lang.String)}.
     * @param sql a <code>String</code> value
     * @return a <code>ResultSet</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.Statement#executeQuery(java.lang.String)
     */
    public ResultSet executeQuery(String sql) throws SQLException {
        checkValidity();
        return preparedStatement.executeQuery(sql);
    }

    /**
     * From {@link java.sql.Statement#executeUpdate(java.lang.String)}.
     * @param sql a <code>String</code> value
     * @return an <code>int</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.Statement#executeUpdate(java.lang.String)
     */
    public int executeUpdate(String sql) throws SQLException {
        checkValidity();
        return preparedStatement.executeUpdate(sql);
    }

    /**
     * From {@link java.sql.Statement#executeUpdate(java.lang.String, int)}.
     * @param sql a <code>String</code> value
     * @param autoGeneratedKeys an <code>int</code> value
     * @return an <code>int</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.Statement#executeUpdate(java.lang.String, int)
     */
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        checkValidity();
        return preparedStatement.executeUpdate(sql, autoGeneratedKeys);
    }

    /**
     * From {@link java.sql.Statement#executeUpdate(java.lang.String, int[])}.
     * @param sql a <code>String</code> value
     * @param columnIndexes an <code>int</code> value
     * @return an <code>int</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.Statement#executeUpdate(java.lang.String, int[])
     */
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        return preparedStatement.executeUpdate(sql, columnIndexes);
    }

    /**
     * From {@link java.sql.Statement#executeUpdate(java.lang.String, java.lang.String[])}.
     * @param sql a <code>String</code> value
     * @param columnNames a <code>String</code> value
     * @return an <code>int</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.Statement#executeUpdate(java.lang.String, java.lang.String[])
     */
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        checkValidity();
        return preparedStatement.executeUpdate(sql, columnNames);
    }

    /**
     * From {@link java.sql.Statement#getConnection()}.
     * @return a <code>Connection</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.Statement#getConnection()
     */
    public Connection getConnection() throws SQLException {
        checkValidity();
        return preparedStatement.getConnection();
    }

    /**
     * From {@link java.sql.Statement#getFetchDirection()}.
     * @return an <code>int</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.Statement#getFetchDirection()
     */
    public int getFetchDirection() throws SQLException {
        checkValidity();
        return preparedStatement.getFetchDirection();
    }

    /**
     * From {@link java.sql.Statement#getFetchSize()}.
     * @return an <code>int</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.Statement#getFetchSize()
     */
    public int getFetchSize() throws SQLException {
        checkValidity();
        return preparedStatement.getFetchSize();
    }

    /**
     * From {@link java.sql.Statement#getGeneratedKeys()}.
     * @return a <code>ResultSet</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.Statement#getGeneratedKeys()
     */
    public ResultSet getGeneratedKeys() throws SQLException {
        checkValidity();
        return preparedStatement.getGeneratedKeys();
    }

    /**
     * From {@link java.sql.Statement#getMaxFieldSize()}.
     * @return an <code>int</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.Statement#getMaxFieldSize()
     */
    public int getMaxFieldSize() throws SQLException {
        checkValidity();
        return preparedStatement.getMaxFieldSize();
    }

    /**
     * From {@link java.sql.Statement#getMaxRows()}.
     * @return an <code>int</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.Statement#getMaxRows()
     */
    public int getMaxRows() throws SQLException {
        checkValidity();
        return preparedStatement.getMaxRows();
    }

    /**
     * From {@link java.sql.Statement#getMoreResults()}.
     * @return a <code>boolean</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.Statement#getMoreResults()
     */
    public boolean getMoreResults() throws SQLException {
        checkValidity();
        return preparedStatement.getMoreResults();
    }

    /**
     * From {@link java.sql.Statement#getMoreResults(int)}.
     * @param current an <code>int</code> value
     * @return a <code>boolean</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.Statement#getMoreResults(int)
     */
    public boolean getMoreResults(int current) throws SQLException {
        checkValidity();
        return preparedStatement.getMoreResults(current);
    }

    /**
     * From {@link java.sql.Statement#getQueryTimeout()}.
     * @return an <code>int</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.Statement#getQueryTimeout()
     */
    public int getQueryTimeout() throws SQLException {
        checkValidity();
        return preparedStatement.getQueryTimeout();
    }

    /**
     * From {@link java.sql.Statement#getResultSetConcurrency()}.
     * @return an <code>int</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.Statement#getResultSetConcurrency()
     */
    public int getResultSetConcurrency() throws SQLException {
        checkValidity();
        return preparedStatement.getResultSetConcurrency();
    }

    /**
     * From {@link java.sql.Statement#getResultSetHoldability()}.
     * @return an <code>int</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.Statement#getResultSetHoldability()
     */
    public int getResultSetHoldability() throws SQLException {
        checkValidity();

        return preparedStatement.getResultSetHoldability();
    }

    /**
     * From {@link java.sql.Statement#getResultSetType()}.
     * @return an <code>int</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.Statement#getResultSetType()
     */
    public int getResultSetType() throws SQLException {
        checkValidity();

        return preparedStatement.getResultSetType();
    }

    /**
     * From {@link java.sql.Statement#getUpdateCount()}.
     * @return an <code>int</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.Statement#getUpdateCount()
     */
    public int getUpdateCount() throws SQLException {
        checkValidity();

        return preparedStatement.getUpdateCount();
    }

    /**
     * From {@link java.sql.Statement#getWarnings()}.
     * @return a <code>SQLWarning</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.Statement#getWarnings()
     */
    public SQLWarning getWarnings() throws SQLException {
        checkValidity();

        return preparedStatement.getWarnings();
    }

    /**
     * From {@link java.sql.Statement#setCursorName(java.lang.String)}.
     * @param name a <code>String</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.Statement#setCursorName(java.lang.String)
     */
    public void setCursorName(String name) throws SQLException {
        checkValidity();

        preparedStatement.setCursorName(name);
    }

    /**
     * From {@link java.sql.Statement#setEscapeProcessing(boolean)}.
     * @param enable a <code>boolean</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.Statement#setEscapeProcessing(boolean)
     */
    public void setEscapeProcessing(boolean enable) throws SQLException {
        checkValidity();

        preparedStatement.setEscapeProcessing(enable);
    }

    /**
     * From {@link java.sql.Statement#setFetchDirection(int)}.
     * @param direction an <code>int</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.Statement#setFetchDirection(int)
     */
    public void setFetchDirection(int direction) throws SQLException {
        checkValidity();

        preparedStatement.setFetchDirection(direction);
    }

    /**
     * From {@link java.sql.Statement#setFetchSize(int)}.
     * @param rows an <code>int</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.Statement#setFetchSize(int)
     */
    public void setFetchSize(int rows) throws SQLException {
        checkValidity();

        preparedStatement.setFetchSize(rows);
    }

    /**
     * From {@link java.sql.Statement#setMaxFieldSize(int)}.
     * @param max an <code>int</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.Statement#setMaxFieldSize(int)
     */
    public void setMaxFieldSize(int max) throws SQLException {
        checkValidity();

        preparedStatement.setMaxFieldSize(max);
    }

    /**
     * From {@link java.sql.Statement#setMaxRows(int)}.
     * @param max an <code>int</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.Statement#setMaxRows(int)
     */
    public void setMaxRows(int max) throws SQLException {
        checkValidity();

        preparedStatement.setMaxRows(max);
    }

    /**
     * From {@link java.sql.Statement#setQueryTimeout(int)}.
     * @param seconds an <code>int</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.Statement#setQueryTimeout(int)
     */
    public void setQueryTimeout(int seconds) throws SQLException {
        checkValidity();
        preparedStatement.setQueryTimeout(seconds);
    }

    /**
     * From {@link java.sql.PreparedStatement#executeQuery()}.
     * @return a <code>ResultSet</code> value
     * @exception SQLException if an error occurs
     * @see java.sql.PreparedStatement#executeQuery()
     */
    public ResultSet executeQuery() throws SQLException {
        checkValidity();
        return preparedStatement.executeQuery();
    }

}
