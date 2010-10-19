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

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.TransactionHelper;
import org.hibernate.id.Configurable;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.enhanced.AccessCallback;
import org.hibernate.id.enhanced.Optimizer;
import org.hibernate.id.enhanced.OptimizerFactory;
import org.hibernate.jdbc.util.FormatStyle;
import org.hibernate.type.Type;
import org.hibernate.util.PropertiesHelper;
import org.hibernate.util.StringHelper;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.model.ModelUtil;
import org.opentaps.foundation.entity.Entity;

/**
 * the IdentifierGenerator which for all opentaps POJOs generate Id.
 */
public class OpentapsIdentifierGenerator extends TransactionHelper implements PersistentIdentifierGenerator, Configurable {
    private static final String MODULE = OpentapsIdentifierGenerator.class.getName();
    private static final String OPT_PARAM = "optimizer";
    // sequence table
    private static final String SEQUENCE_TABLE_NAME = "SEQUENCE_VALUE_ITEM";
    // sequence name
    private static final String SEQUENCE_TYPE_COLUMN = "SEQ_NAME";
    // value length
    private static final int SEQUENCE_MAX_LENGTH = 20;
    // value column name
    private static final String SEQUENCE_VALUE_COLUMN = "SEQ_ID";
    // seq initial value
    public static final int SEQUENCE_INIT_VALUE = 10000;
    // increment size
    private static final int DEF_INCREMENT_SIZE = 1;
    private int incrementSize = DEF_INCREMENT_SIZE;

    private String selectQuery;
    private String insertQuery;
    private String updateQuery;
    
    private String idField;

    private Optimizer optimizer;
    //log access count
    private long accessCount = 0;
    // sequence type
    private String sequenceType;
    // return java type
    private Type identifierType;

    /**
     * Return a key unique to the underlying database objects. Prevents us from
     * trying to create/remove them multiple times.
     *
     * @return Object an identifying key for this generator
     */
    public Object generatorKey() {
        return SEQUENCE_TABLE_NAME;
    }

    /**
     * Configure this instance, given the value of parameters
     * specified by the user as <tt>&lt;param&gt;</tt> elements.
     * This method is called just once, following instantiation.
     *
     * @param type the identifier type, such as GenericGenerator
     * @param params param values, keyed by parameter name
     * @param dialect database dialect
     * @throws MappingException if an error occurs
     */
    public void configure(Type type, Properties params, Dialect dialect) throws MappingException {
        identifierType = type;
        // retrieve entity name, like org.opentaps.base.entities.TestEntity
        String entityName = params.getProperty("entity_name");
        String targetColumn = params.getProperty("target_column");
        idField = ModelUtil.dbNameToVarName(targetColumn);
        // get entity short name, like TestEntity
        sequenceType = entityName.substring(entityName.lastIndexOf(".") + 1);
        // get sequence query, for get current value of sequence
        this.selectQuery = buildSelectQuery(dialect);
        // update sequence query, for update sequence value = value + 1
        this.updateQuery = buildUpdateQuery();
        // insert sequence query, if not exist sequence in sequence table, then will use this sql to insert a record.
        this.insertQuery = buildInsertQuery();

        String defOptStrategy = incrementSize <= 1 ? OptimizerFactory.NONE : OptimizerFactory.POOL;
        //get optimization strategy
        String optimizationStrategy = PropertiesHelper.getString(OPT_PARAM, params, defOptStrategy);
        optimizer = OptimizerFactory.buildOptimizer(optimizationStrategy, Long.class, incrementSize);
    }

    /**
     * Returns the select query by database dialect.
     * @param dialect a <code>Dialect</code>
     * @return the <code>String</code> query string.
     */
    protected String buildSelectQuery(Dialect dialect) {
        final String alias = "tbl";
        String query = "select " + StringHelper.qualify(alias, SEQUENCE_VALUE_COLUMN)
                + " from " + SEQUENCE_TABLE_NAME + ' ' + alias
                + " where " + StringHelper.qualify(alias, SEQUENCE_TYPE_COLUMN) + "=?";
        HashMap<String, LockMode> lockMap = new HashMap<String, LockMode>();
        lockMap.put(alias, LockMode.UPGRADE);
        Map<String, String[]> updateTargetColumnsMap = Collections.singletonMap(alias, new String[] {SEQUENCE_VALUE_COLUMN});
        return dialect.applyLocksToSql(query, lockMap, updateTargetColumnsMap);
    }

    /**
     * Returns the update query by database dialect.
     * @param dialect a <code>Dialect</code>
     * @return the <code>String</code> query string.
     */
    protected String buildUpdateQuery() {
        return "update " + SEQUENCE_TABLE_NAME
               + " set " + SEQUENCE_VALUE_COLUMN + "=? "
               + " where " + SEQUENCE_VALUE_COLUMN + "=? and " + SEQUENCE_TYPE_COLUMN + "=?";
    }

    /**
     * Returns the insert query by database dialect.
     * @param dialect a <code>Dialect</code>
     * @return the <code>String</code> query string.
     */
    protected String buildInsertQuery() {
        return "insert into " + SEQUENCE_TABLE_NAME + " (" + SEQUENCE_TYPE_COLUMN + ", " + SEQUENCE_VALUE_COLUMN + ") " + " values (?,?)";
    }

    /**
     * Returns the new sequence.
     * @param session a <code>SessionImplementor</code>
     * @return the <code>Serializable</code> sequence value.
     */
    public synchronized Serializable generate(final SessionImplementor session, Object obj) {
    	if (obj instanceof Entity) {
    		Entity entity = (Entity) obj;
    		String idValue = entity.getString(idField);
    		if (UtilValidate.isNotEmpty(idValue)) {
    			return idValue;
    		}
    	}
        Long seq = (Long) optimizer.generate(
                new AccessCallback() {
                    public long getNextValue() {
                        // get next sequence
                        return ((Number) doWorkInNewTransaction(session)).longValue();
                    }
                }
       );
        // change format to String.
        DecimalFormat df = new DecimalFormat("0");
        Debug.logVerbose("Generate sequence " + seq + "," + df.format(seq) + " for " + identifierType.getName(), MODULE);
        return df.format(seq);
    }

    /**
     * do work in a same transaction.
     * @param conn a <code>Connection</code>
     * @param sql a <code>String</code> SQL query
     * @return the <code>Serializable</code> sequence value.
     * @throws SQLException if an error occurs
     */
    @Override
    public Serializable doWorkInCurrentTransaction(Connection conn, String sql) throws SQLException {
        int result;
        int rows;
        do {
            // set log level
            SQL_STATEMENT_LOGGER.logStatement(selectQuery, FormatStyle.BASIC);
            // create jdbc query
            PreparedStatement selectPS = conn.prepareStatement(selectQuery);
            try {
                selectPS.setString(1, sequenceType);
                ResultSet selectRS = selectPS.executeQuery();
                if (!selectRS.next()) {
                    // if not exist sequence, then create one in table
                    PreparedStatement insertPS = null;
                    try {
                        // set result as initialValue
                        result = SEQUENCE_INIT_VALUE;
                        SQL_STATEMENT_LOGGER.logStatement(insertQuery, FormatStyle.BASIC);
                        insertPS = conn.prepareStatement(insertQuery);
                        insertPS.setString(1, sequenceType);
                        insertPS.setLong(2, result);
                        insertPS.execute();
                    } finally {
                        if (insertPS != null) {
                            insertPS.close();
                        }
                    }
                } else {
                    // read sequence value
                    result = selectRS.getInt(1);
                }
                selectRS.close();
            } catch (SQLException sqle) {
                Debug.logError("could not read or init a hi value", MODULE);
                throw sqle;
            } finally {
                selectPS.close();
            }

            SQL_STATEMENT_LOGGER.logStatement(updateQuery, FormatStyle.BASIC);
            PreparedStatement updatePS = conn.prepareStatement(updateQuery);
            try {
                // if the values in the source are to be incremented according to the defined increment size; otherwise add 1
                long newValue = optimizer.applyIncrementSizeToSourceValues() ? result + incrementSize : result + 1;
                updatePS.setLong(1, newValue);
                updatePS.setLong(2, result);
                updatePS.setString(3, sequenceType);
                //update sequence value
                rows = updatePS.executeUpdate();
            } catch (SQLException sqle) {
                Debug.logError("could not updateQuery hi value in: " + SEQUENCE_TABLE_NAME, MODULE);
                throw sqle;
            } finally {
                updatePS.close();
            }
        }
        while (rows == 0);
        //log access count
        accessCount++;
        return new Integer(result);
    }

    /**
     * The SQL required to create the underlying database objects.
     *
     * @param dialect The dialect against which to generate the create command(s)
     * @return The create sql
     * @throws HibernateException problem creating the sql
     */
    public String[] sqlCreateStrings(Dialect dialect) throws HibernateException {
        // build create table ddl sql with specific dialect
        return new String[] {
                new StringBuffer()
                        .append(dialect.getCreateTableString())
                        .append(' ')
                        .append(SEQUENCE_TABLE_NAME)
                        .append(" (")
                        .append(SEQUENCE_TYPE_COLUMN)
                        .append(' ')
                        .append(dialect.getTypeName(Types.VARCHAR, SEQUENCE_MAX_LENGTH, 0, 0))
                        .append(" not null ")
                        .append(",  ")
                        .append(SEQUENCE_VALUE_COLUMN)
                        .append(' ')
                        .append(dialect.getTypeName(Types.BIGINT))
                        .append(", primary key (")
                        .append(SEQUENCE_TYPE_COLUMN)
                        .append(")) ")
                        .toString()
        };
    }

    /**
     * The SQL required to remove the underlying database objects.
     *
     * @param dialect The dialect against which to generate the drop sql
     * @return The drop command(s)
     * @throws HibernateException problem creating the drop sql
     */
    public String[] sqlDropStrings(Dialect dialect) throws HibernateException {
        // build drop table ddl sql with specific dialect
        StringBuffer sqlDropString = new StringBuffer().append("drop table ");
        if (dialect.supportsIfExistsBeforeTableName()) {
            sqlDropString.append("if exists ");
        }
        sqlDropString.append(SEQUENCE_TABLE_NAME).append(dialect.getCascadeConstraintsString());
        if (dialect.supportsIfExistsAfterTableName()) {
            sqlDropString.append(" if exists");
        }
        return new String[] {sqlDropString.toString()};
    }
}
