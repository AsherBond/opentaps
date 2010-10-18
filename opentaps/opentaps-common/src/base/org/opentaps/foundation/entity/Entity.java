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

/*******************************************************************************
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
 *******************************************************************************/

// A small part of this file came from Apache Ofbiz and has been modified by Open Source Strategies, Inc.

package org.opentaps.foundation.entity;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.RepositoryInterface;

/** {@inheritDoc}. */
public class Entity implements EntityInterface {

    private static final String MODULE = Entity.class.getName();

    /** List the fields used by the Ofbiz entity engine. */
    public static final List<String> STAMP_FIELDS = Arrays.asList("lastUpdatedStamp", "lastUpdatedTxStamp", "createdStamp", "createdTxStamp");

    protected boolean isView = false;
    protected String baseEntityName = "NOT CONFIGURED";
    protected String resourceName;
    protected List<String> primaryKeyNames;
    protected List<String> nonPrimaryKeyNames;
    protected List<String> allFieldsNames;
    protected RepositoryInterface repository;

    /** Map the fields used by the hibernate view entity engine. */
    public static Map<String, Map<String, String>> fieldMapColumns = new TreeMap<String, Map<String, String>>();

    /**
     * This method is called before object fields receive values in <code>fromMap()</code><br/>
     * Default implementation does nothing.
     */
    protected void preInit() { }

    /** {@inheritDoc} */
    public void fromMap(Map<String, Object> mapValue) {
        preInit();
        postInit();
        // this should be extended by the sub-classes to match their actual fields to a Map
    }

    /** {@inheritDoc} */
    public void fromEntity(EntityInterface entity) {
        fromMap(entity.toMap());
    }

    /**
     * This method is called after object fields receive values in <code>fromMap()</code><br/>
     * Default implementation does nothing.
     */
    protected void postInit() { }

    /** {@inheritDoc} */
    public Map<String, Object> toMap() {
        // this should also be extended by the sub-classes to match their actual fields to a Map
        return new HashMap<String, Object>();
    }

    /**
     * Returns a <code>List</code> of <code>Map</code> from a List of entities by calling their <code>toMap()</code> method.
     * @param entities a <code>Iterable<? extends EntityInterface></code> value
     * @return a <code>List<Map<String,Object>></code> value
     */
    public static List<Map<String, Object>> toMaps(Iterable<? extends EntityInterface> entities) {
        List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
        for (EntityInterface e : entities) {
            maps.add(e.toMap());
        }
        return maps;
    }

    /**
     * Returns a <code>Map</code> with only the listed fields.
     * One application is to build a Set of entities that have distinct values for a sub set of their fields
     * @param fields list of field names to include
     * @return a <code>Map</code> with only the listed fields
     * @see #getDistinctFieldValues
     */
    public Map<String, Object> toMap(Iterable<String> fields) {
        Map<String, Object> map = new HashMap<String, Object>();
        for (String field : fields) {
            map.put(field, get(field));
        }
        return map;
    }

    /**
     * Returns a <code>Map</code> with of this entity without the Ofbiz fields.
     * @return a <code>Map</code> with of this entity without the Ofbiz fields
     */
    public Map<String, Object> toMapNoStamps() {
        Map<String, Object> map = toMap();
        for (String field : STAMP_FIELDS) {
            map.remove(field);
        }
        return map;
    }

    /**
     * Builds a <code>Set</code> of entities that have distinct values for a sub set of their fields.
     * The resulting <code>Set</code> values are ordered the same as the input list of entities.
     * @param entities a list of entities
     * @param fields a list of fields to consider when comparing the entities
     * @return the resulting <code>Set</code> of entities that are distinct for the given list of fields
     */
    public static Set<Map<String, Object>> getDistinctFieldValues(Iterable<? extends EntityInterface> entities, Iterable<String> fields) {
        Set<Map<String, Object>> distinctEntities = new LinkedHashSet<Map<String, Object>>();
        for (EntityInterface e : entities) {
            distinctEntities.add(e.toMap(fields));
        }
        return distinctEntities;
    }

    /**
     * Builds a <code>Set</code> of distinct entity values for one their fields.
     * The resulting <code>Set</code> values are ordered the same as the input list of entities.
     * @param <T> the entity class
     * @param entities a list of entities
     * @param field the field to consider when comparing the entities
     * @return the resulting <code>Set</code> of entities that are distinct for the given field
     */
    public static <T extends EntityInterface> Set<Object> getDistinctFieldValues(Iterable<T> entities, EntityFieldInterface<? super T> field) {
        Set<Object> distinctValues = new LinkedHashSet<Object>();
        for (EntityInterface e : entities) {
            distinctValues.add(e.get(field.getName()));
        }
        return distinctValues;
    }

    /**
     * Builds a <code>Set</code> of distinct entity values for one their fields.
     * The resulting <code>Set</code> values are ordered the same as the input list of entities.
     * @param <T> the entity class
     * @param <T2> the field class
     * @param fieldType the field class
     * @param entities a list of entities
     * @param field the field to consider when comparing the entities
     * @return the resulting <code>Set</code> of entities that are distinct for the given field
     */
    public static <T extends EntityInterface, T2 extends Object> Set<T2> getDistinctFieldValues(Class<T2> fieldType, Iterable<T> entities, EntityFieldInterface<? super T> field) {
        return getDistinctFieldValues(fieldType, entities, field.getName());
    }

    /**
     * Builds a <code>Set</code> of distinct entity values for one their fields.
     * The resulting <code>Set</code> values are ordered the same as the input list of entities.
     * @param <T> the entity class
     * @param <T2> the field class
     * @param fieldType the field class
     * @param entities a list of entities
     * @param fieldName the field to consider when comparing the entities
     * @return the resulting <code>Set</code> of entities that are distinct for the given field
     */
    @SuppressWarnings("unchecked")
    public static <T extends EntityInterface, T2 extends Object> Set<T2> getDistinctFieldValues(Class<T2> fieldType, Iterable<T> entities, String fieldName) {
        Set<T2> distinctValues = new LinkedHashSet<T2>();
        for (EntityInterface e : entities) {
            distinctValues.add((T2) e.get(fieldName));
        }
        return distinctValues;
    }

    /**
     * Builds a <code>List</code> of entity values for one their fields.
     * The resulting <code>List</code> is ordered the same as the input list of entities.
     * @param <T> the entity class
     * @param entities a list of entities
     * @param field the field to get
     * @return the resulting <code>List</code> of field values
     */
    public static <T extends EntityInterface> List<Object> getFieldValues(Iterable<T> entities, EntityFieldInterface<? super T> field) {
        List<Object> values = new ArrayList<Object>();
        for (EntityInterface e : entities) {
            values.add(e.get(field.getName()));
        }
        return values;
    }

    /**
     * Builds a <code>List</code> of entity values for one their fields.
     * The resulting <code>List</code> is ordered the same as the input list of entities.
     * @param <T> the entity class
     * @param <T2> the field class
     * @param fieldType the field class
     * @param entities a list of entities
     * @param field the field to get
     * @return the resulting <code>List</code> of field values
     */
    public static <T extends EntityInterface, T2 extends Object> List<T2> getFieldValues(Class<T2> fieldType, Iterable<T> entities, EntityFieldInterface<? super T> field) {
        return getFieldValues(fieldType, entities, field.getName());
    }

    /**
     * Builds a <code>List</code> of entity values for one their fields.
     * The resulting <code>List</code> is ordered the same as the input list of entities.
     * @param <T> the entity class
     * @param <T2> the field class
     * @param fieldType the field class
     * @param entities a list of entities
     * @param fieldName the field to get
     * @return the resulting <code>List</code> of field values
     */
    @SuppressWarnings("unchecked")
    public static <T extends EntityInterface, T2 extends Object> List<T2> getFieldValues(Class<T2> fieldType, Iterable<T> entities, String fieldName) {
        List<T2> distinctValues = new ArrayList<T2>();
        for (EntityInterface e : entities) {
            distinctValues.add((T2) e.get(fieldName));
        }
        return distinctValues;
    }

    /**
     * Builds a <code>Map</code> from a <code>List</code> of entity values for one their fields.
     * This group each entities by their distinct field values.
     * @param <T> the entity class
     * @param <K> the key class
     * @param keyType the key class, should match the entity field value class it is being grouped by
     * @param entities a list of entities
     * @param field the field to group by
     * @return the resulting <code>Map</code> of entities grouped by field values
     */
    public static <T extends EntityInterface, K extends Object> Map<K, List<T>> groupByFieldValues(Class<K> keyType, Iterable<T> entities, EntityFieldInterface<? super T> field) {
        return groupByFieldValues(keyType, entities, field.getName());
    }

    /**
     * Builds a <code>Map</code> from a <code>List</code> of entity values for one their fields.
     * This group each entities by their distinct field values.
     * @param <T> the entity class
     * @param <K> the key class
     * @param keyType the key class, should match the entity field value class it is being grouped by
     * @param entities a list of entities
     * @param fieldName the field name to group by
     * @return the resulting <code>Map</code> of entities grouped by field values
     */
    public static <T extends EntityInterface, K extends Object> Map<K, List<T>> groupByFieldValues(Class<K> keyType, Iterable<T> entities, String fieldName) {
        Map<K, List<T>> grouped = new HashMap<K, List<T>>();
        for (T e : entities) {
            K key = (K) e.get(fieldName);
            List<T> values = grouped.get(key);
            if (values == null) {
                values = new ArrayList<T>();
                grouped.put(key, values);
            }
            values.add(e);
        }
        return grouped;
    }

    /**
     * Builds a <code>Map</code> from two <code>List</code> of entities that share a common field value.
     * This checks each entity field against the keySet keyField, and group the entities by the corresponding key entity.
     *
     * For example:
     * <code>groupByFieldValues(Order.class, List<OrderItem>, OrderItem.Fields.orderId, List<Order>, Order.Fields.orderId)</code> builds
     * a <code>Map</code> of <code>Order</code> to <code>List<OrderItem></code> belonging to that <code>Order</code>.
     *
     * This is meant as a more efficient way to build such a <code>Map</code> than having to iterate on a list of key entities and fetching its related
     * entities, as this only requires two fetches from the database.
     *
     * @param <T> the entity class
     * @param <K> the key class
     * @param keyType the key class, should match the entity field value class it is being grouped by
     * @param entities a list of entities to group
     * @param field the field of <code>entities</code> to group by
     * @param keySet a list of grouping entities
     * @param keyField the field of <code>keySet</code> to group by
     * @return the resulting <code>Map</code> of entities grouped by key entity
     */
    public static <T extends EntityInterface, K extends EntityInterface> Map<K, List<T>> groupByFieldValues(Class<K> keyType, Iterable<T> entities, EntityFieldInterface<? super T> field, Iterable<K> keySet, EntityFieldInterface<? super K> keyField) {
        return groupByFieldValues(keyType, entities, field.getName(), keySet, keyField.getName());
    }

    /**
     * Builds a <code>Map</code> from two <code>List</code> of entities that share a common field value.
     * This checks each entity field against the keySet keyField, and group the entities by the corresponding key entity.
     *
     * For example:
     * <code>groupByFieldValues(Order.class, List<OrderItem>, OrderItem.Fields.orderId, List<Order>, Order.Fields.orderId)</code> builds
     * a <code>Map</code> of <code>Order</code> to <code>List<OrderItem></code> belonging to that <code>Order</code>.
     *
     * This is meant as a more efficient way to build such a <code>Map</code> than having to iterate on a list of key entities and fetching its related
     * entities, as this only requires two fetches from the database.
     *
     * @param <T> the entity class
     * @param <K> the key class
     * @param keyType the key class, should match the entity field value class it is being grouped by
     * @param entities a list of entities to group
     * @param fieldName the field name of <code>entities</code> to group by
     * @param keySet a list of grouping entities
     * @param keyFieldName the field name of <code>keySet</code> to group by
     * @return the resulting <code>Map</code> of entities grouped by key entity
     */
    public static <T extends EntityInterface, K extends EntityInterface> Map<K, List<T>> groupByFieldValues(Class<K> keyType, Iterable<T> entities, String fieldName, Iterable<K> keySet, String keyFieldName) {
        Map<K, List<T>> grouped = new HashMap<K, List<T>>();
        for (T e : entities) {
            Object fieldValue = e.get(fieldName);
            for (K key : keySet) {
                Object keyFieldValue = key.get(keyFieldName);
                if (keyFieldValue.equals(fieldValue)) {
                    List<T> values = grouped.get(key);
                    if (values == null) {
                        values = new ArrayList<T>();
                        grouped.put(key, values);
                    }
                    values.add(e);
                }
            }
        }
        return grouped;
    }

    /**
     * Gets the first entity from the given List.
     * @param <T> the entity class
     * @param entities a list of entities
     * @return the first of the given list or <code>null</code> if the list is <code>null</code> or empty
     */
    public static <T extends EntityInterface> T getFirst(List<T> entities) {
        if ((entities != null) && (entities.size() > 0)) {
            return entities.get(0);
        } else {
            return null;
        }
    }

    /**
     * Gets the last entity from the given List.
     * @param <T> the entity class
     * @param entities a list of entities
     * @return the last of the given list or <code>null</code> if the list is <code>null</code> or empty
     */
    public static <T extends EntityInterface> T getLast(List<T> entities) {
        if ((entities != null) && (entities.size() > 0)) {
            return entities.get(entities.size() - 1);
        } else {
            return null;
        }
    }

    /**
     * Gets the only entity from the given List, throws an exception if more than one is present in the List.
     * @param <T> the entity class
     * @param entities a list of entities
     * @return the only entity of the given list or <code>null</code> if the list is <code>null</code> or empty
     * @throws IllegalArgumentException if the given List has more than one element
     */
    public static <T extends EntityInterface> T getOnly(List<T> entities) throws IllegalArgumentException {
        if (entities != null) {
            if (entities.size() <= 0) {
                return null;
            }
            if (entities.size() == 1) {
                return entities.get(0);
            } else {
                throw new IllegalArgumentException("Passed List had more than one value.");
            }
        } else {
            return null;
        }
    }

    /**
     * Sums the values of the given field for the given list of entities.
     * <code>Null</code> values are considered to be <code>0</code>.
     * @param <T> the entity class
     * @param entities a list of entities to sum
     * @param field the field of <code>entities</code> to sum by
     * @return the sum of the <code>fieldName</code> values
     */
    public static <T extends EntityInterface> BigDecimal sumFieldValues(Iterable<T> entities, EntityFieldInterface<? super T> field) {
        return sumFieldValues(entities, field.getName());
    }

    /**
     * Sums the values of the given field for the given list of entities.
     * <code>Null</code> values are considered to be <code>0</code>.
     * @param <T> the entity class
     * @param entities a list of entities to sum
     * @param fieldName the field name of <code>entities</code> to sum by
     * @return the sum of the <code>fieldName</code> values
     */
    public static <T extends EntityInterface> BigDecimal sumFieldValues(Iterable<T> entities, String fieldName) {
        BigDecimal sum = BigDecimal.ZERO;
        for (T e : entities) {
            BigDecimal v = e.getBigDecimal(fieldName);
            if (v != null) {
                sum = sum.add(v);
            }
        }
        return sum;
    }

    /** {@inheritDoc} */
    public Object get(String fieldName) {
        return toMap().get(fieldName);
    }

    /**
     * Provides backward compatibility with ofbiz way of localizing entity strings
     * by returning localized value of field fieldName for null resource.
     * @param fieldName the field to retrieve
     * @param locale the user locale
     * @return the value for that field, localized if possible
    */
    public Object get(String fieldName, Locale locale) {
        return get(fieldName, null, locale);
    }

    /**
     * Provides backward compatibility with ofbiz way of localizing entity strings
     * by returning the localized value of the field with fieldName (ie, "invoiceTypeId") with resource (ie, "FinancialsUiLabels").
     * @param fieldName the field to retrieve
     * @param resource a specific resource file to use
     * @param locale the user locale
     * @return the value for that field, localized if poss
     */
    public Object get(String fieldName, String resource, Locale locale) {
        Object fieldValue = null;
        try {
            fieldValue = get(fieldName);
        } catch (IllegalArgumentException e) {
            fieldValue = null;
        }

        if (UtilValidate.isEmpty(resource)) {
            resource = getResourceName();
            // still empty? return the fieldValue
            if (UtilValidate.isEmpty(resource)) {
                //Debug.logError("Empty resource name for entity " + getBaseEntityName(), module);
                return fieldValue;
            }
        }

        ResourceBundle bundle = null;
        try {
            bundle = UtilProperties.getResourceBundle(resource, locale);
        } catch (IllegalArgumentException e) {
            bundle = null;
        }
        if (bundle == null) {
            Debug.logWarning("Tried to getResource value for field named " + fieldName + " but no resource was found with the name " + resource + " in the locale " + locale, MODULE);
            return fieldValue;
        }

        StringBuffer keyBuffer = new StringBuffer();
        // start with the Entity Name
        keyBuffer.append(getBaseEntityName());
        // next add the Field Name
        keyBuffer.append('.');
        keyBuffer.append(fieldName);
        // finish off by adding the PK or the value if no PK is set
        if (getPrimaryKeyNames() != null) {
            for (String pk : getPrimaryKeyNames()) {
                keyBuffer.append('.');
                keyBuffer.append(get(pk));
            }
        } else {
            keyBuffer.append('.');
            keyBuffer.append(fieldValue);
        }

        String bundleKey = keyBuffer.toString();

        Object resourceValue = null;
        try {
            resourceValue = bundle.getObject(bundleKey);
        } catch (MissingResourceException e) {
            Debug.logWarning("Could not find resource value : " + bundleKey, MODULE);
        }
        if (resourceValue == null) {
            return fieldValue;
        } else {
            return resourceValue;
        }
    }

    /** {@inheritDoc} */
    public String getString(String fieldName) {
        return (String) get(fieldName);
    }

    /** {@inheritDoc} */
    public Boolean getBoolean(String fieldName) {
        return (Boolean) get(fieldName);
    }

    /** {@inheritDoc} */
    public Double getDouble(String fieldName) {
        return (Double) get(fieldName);
    }

    /** {@inheritDoc} */
    public Float getFloat(String fieldName) {
        return (Float) get(fieldName);
    }

    /** {@inheritDoc} */
    public Long getLong(String fieldName) {
        return (Long) get(fieldName);
    }

    /** {@inheritDoc} */
    public BigDecimal getBigDecimal(String fieldName) {
        return (BigDecimal) get(fieldName);
    }

    /** {@inheritDoc} */
    public Timestamp getTimestamp(String fieldName) {
        return (Timestamp) get(fieldName);
    }

    /** {@inheritDoc} */
    public void set(String fieldName, Object value) {
        // get the Map of all fields, change the one we're setting, and then set all fields to the modified map
        Map<String, Object> mapValue = toMap();
        mapValue.put(fieldName, value);
        fromMap(mapValue);
    }

    /**
     * This is a special method for converting a <code>Map</code> field's value to <code>BigDecimal</code>, because the ofbiz entity engine defines
     * floating point types as <code>Double</code>, so by default they are cast as <code>Double</code>.
     * @param value the value object to convert to a <code>BigDecimal</code>
     * @return the <code>BigDecimal</code> value or <code>null</code>
     */
    public BigDecimal convertToBigDecimal(Object value) {
        if (value instanceof Double) {
            return BigDecimal.valueOf(((Double) value).doubleValue());
        } else if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        } else {
            // this should not happen
            return null;
        }
    }

    /** {@inheritDoc} */
    public void initRepository(RepositoryInterface repository) {
        this.repository = repository;
    }

    /** {@inheritDoc} */
    public RepositoryInterface getBaseRepository() {
        return this.repository;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        return hashCode() == obj.hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        int hash = 5381;

        Map<String, Object> fields = toMap();
        assert fields != null;

        // build reference string
        String str = getClass().getName();
        Iterator<Object> iterator = fields.values().iterator();
        while (iterator.hasNext()) {
            Object s = iterator.next();
            if (s != null) {
                str += s.toString();
            }
        }

        // calculate hash of string
        for (int i = 0; i < str.length(); i++) {
           hash = ((hash << 5) + hash) + str.charAt(i);
        }

        return hash;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return ("Entity [" + getBaseEntityName() + "] with fields " + toMap());
    }

    /** {@inheritDoc} */
    public String getBaseEntityName() {
        return baseEntityName;
    }

    /**
     * Gets the resource used to localized values for this entity.
     * @return the resource name
     */
    public String getResourceName() {
        return resourceName;
    }

    /**
     * Sets the resource used to localized values for this entity.
     * @param resourceName the resource name
     */
    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    /** {@inheritDoc} */
    public boolean isView() {
        return isView;
    }

    /** {@inheritDoc} */
    public List<String> getPrimaryKeyNames() {
        return primaryKeyNames;
    }

    /** {@inheritDoc} */
    public List<String> getNonPrimaryKeyNames() {
        return nonPrimaryKeyNames;
    }

    /** {@inheritDoc} */
    public List<String> getAllFieldsNames() {
        return allFieldsNames;
    }

    /** {@inheritDoc} */
    public String getNextSeqId() {
        return repository.getNextSeqId(this);
    }

    /** {@inheritDoc} */
    public void setNextSubSeqId(String sequenceFieldName) throws RepositoryException {
        setNextSubSeqId(sequenceFieldName, 5);
    }

    /** {@inheritDoc} */
    public void setNextSubSeqId(String sequenceFieldName, int numericPadding) throws RepositoryException {
        setNextSubSeqId(sequenceFieldName, numericPadding, 1);
    }

    /** {@inheritDoc} */
    public void setNextSubSeqId(String sequenceFieldName, int numericPadding, int incrementBy) throws RepositoryException {
        set(sequenceFieldName, repository.getNextSubSeqId(this, sequenceFieldName, numericPadding, incrementBy));
    }

    // set field values from a Map

    /** {@inheritDoc} */
    public void setAllFields(Map<String, Object> fields) {
        setAllFields(fields, true);
    }

    /** {@inheritDoc} */
    public void setAllFields(Map<String, Object> fields, boolean setIfEmpty) {
        setAllFields(fields, setIfEmpty, null);
    }

    /** {@inheritDoc} */
    public void setNonPKFields(Map<String, Object> fields) {
        setNonPKFields(fields, true);
    }

    /** {@inheritDoc} */
    public void setNonPKFields(Map<String, Object> fields, boolean setIfEmpty) {
        setAllFields(fields, setIfEmpty, Boolean.FALSE);
    }

    /** {@inheritDoc} */
    public void setPKFields(Map<String, Object> fields) {
        setPKFields(fields, true);
    }

    /** {@inheritDoc} */
    public void setPKFields(Map<String, Object> fields, boolean setIfEmpty) {
        setAllFields(fields, setIfEmpty, Boolean.TRUE);
    }

    /** {@inheritDoc} */
    public void setAllFields(Map<String, Object> fields, boolean setIfEmpty, Boolean pks) {
        if (fields == null) {
            return;
        }

        Set<String> fieldsToSet = new HashSet<String>();

        if (pks == null) {
            fieldsToSet.addAll(getAllFieldsNames());
        } else if (pks == Boolean.TRUE) {
            fieldsToSet.addAll(getPrimaryKeyNames());
        } else {
            fieldsToSet.addAll(getNonPrimaryKeyNames());
        }

        for (String fieldName : fieldsToSet) {
            if (fields.containsKey(fieldName)) {
                Object value = fields.get(fieldName);
                if (setIfEmpty) {
                    // if it's an empty string, set to null
                    if (value != null && value instanceof String && ((String) value).length() == 0) {
                        this.set(fieldName, null);
                    } else {
                        this.set(fieldName, value);
                    }
                } else {
                    // do not set if null or empty
                    if (value != null) {
                        if (value instanceof String) {
                            if (((String) value).length() > 0) {
                                this.set(fieldName, value);
                            }
                        } else {
                            this.set(fieldName, value);
                        }
                    }
                }
            }
        }
    }

    // get related methods

    /** {@inheritDoc} */
    public EntityInterface getRelatedOne(String relation) throws RepositoryException {
        if (repository == null) {
            return null;
        }
        return repository.getRelatedOne(relation, this);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> T getRelatedOne(Class<T> entityName) throws RepositoryException {
        if (repository == null) {
            return null;
        }
        return repository.getRelatedOne(entityName, this);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> T getRelatedOne(Class<T> entityName, String relation) throws RepositoryException {
        if (repository == null) {
            return null;
        }
        return repository.getRelatedOne(entityName, relation, this);
    }

    /** {@inheritDoc} */
    public EntityInterface getRelatedOneCache(String relation) throws RepositoryException {
        if (repository == null) {
            return null;
        }
        return repository.getRelatedOneCache(relation, this);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> T getRelatedOneCache(Class<T> entityName) throws RepositoryException {
        if (repository == null) {
            return null;
        }
        return repository.getRelatedOneCache(entityName, this);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> T getRelatedOneCache(Class<T> entityName, String relation) throws RepositoryException {
        if (repository == null) {
            return null;
        }
        return repository.getRelatedOneCache(entityName, relation, this);
    }

    /** {@inheritDoc} */
    public List<? extends EntityInterface> getRelated(String relation) throws RepositoryException {
        if (repository == null) {
            return null;
        }
        return repository.getRelated(relation, this);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> List<T> getRelated(Class<T> entityName) throws RepositoryException {
        if (repository == null) {
            return null;
        }
        return repository.getRelated(entityName, this);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> List<T> getRelated(Class<T> entityName, List<String> orderBy) throws RepositoryException {
        if (repository == null) {
            return null;
        }
        return repository.getRelated(entityName, this, orderBy);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> List<T> getRelated(Class<T> entityName, String relation) throws RepositoryException {
        if (repository == null) {
            return null;
        }
        return repository.getRelated(entityName, relation, this);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> List<T> getRelated(Class<T> entityName, String relation, List<String> orderBy) throws RepositoryException {
        if (repository == null) {
            return null;
        }
        return repository.getRelated(entityName, relation, this, orderBy);
    }

    /** {@inheritDoc} */
    public List<? extends EntityInterface> getRelatedCache(String relation) throws RepositoryException {
        if (repository == null) {
            return null;
        }
        return repository.getRelatedCache(relation, this);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> List<T> getRelatedCache(Class<T> entityName) throws RepositoryException {
        if (repository == null) {
            return null;
        }
        return repository.getRelatedCache(entityName, this);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> List<T> getRelatedCache(Class<T> entityName, String relation) throws RepositoryException {
        if (repository == null) {
            return null;
        }
        return repository.getRelatedCache(entityName, relation, this);
    }

    /**
     * Gets the list of related entities to the given list of entity.
     * @param <T> class of entity to return
     * @param <T2> class of the given list of entity
     * @param entityName the name of the related entities
     * @param entities the list of entities to get the related from
     * @return a list of <code>EntityInterface</code> value
     * @throws RepositoryException if an error occurs
     */
    public static <T extends EntityInterface, T2 extends EntityInterface> List<T> getRelated(Class<T> entityName, Iterable<T2> entities) throws RepositoryException {
        if (entities == null) {
            return null;
        }
        List<T> result = new ArrayList<T>();
        RepositoryInterface repo = null;
        for (EntityInterface entity : entities) {
            repo = entity.getBaseRepository();
            if (repo == null) {
                return null;
            }
            result.addAll(entity.getRelated(entityName));
        }

        return result;
    }

    /**
     * Gets the list of related entities to the given list of entity using the cache.
     * @param <T> class of entity to return
     * @param <T2> class of the given list of entity
     * @param entityName the name of the related entities
     * @param entities the list of entities to get the related from
     * @return a list of <code>EntityInterface</code> value
     * @throws RepositoryException if an error occurs
     */
    public static <T extends EntityInterface, T2 extends EntityInterface> List<T> getRelatedCache(Class<T> entityName, Iterable<T2> entities) throws RepositoryException {
        if (entities == null) {
            return null;
        }
        List<T> result = new ArrayList<T>();
        RepositoryInterface repo = null;
        for (EntityInterface entity : entities) {
            repo = entity.getBaseRepository();
            if (repo == null) {
                return null;
            }
            result.addAll(entity.getRelatedCache(entityName));
        }

        return result;
    }

}
