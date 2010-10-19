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
package org.opentaps.foundation.entity;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Map;

import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.RepositoryInterface;
import java.util.List;

/**
 * An Entity as defined in "Model Driven Design", p. 89, is an Object
 * with a unique identity.  Here, our Entity interface is meant to wrap a GenericValue inside of a
 * Java object (POJO), or alternatively make a POJO look like a GenericValue to the rest of the
 * ofbiz-based infrastructure.  You can instantiate it with a GenericValue, then use the same .get(fieldName)
 * and .set(fieldName, value) methods to access it as if it were a GenericValue but also extend it with
 * object oriented methods.  Alternatively, you can implement your Entity as a POJO and then just furnish it
 * with .get(fieldName).
 */
public interface EntityInterface {

    /**
     * Generic getter, in case you want to work with the entity as a Map-like construct.
     * @param fieldName the field to retrieve
     * @return the value for that field
     */
    public Object get(String fieldName);

    /**
     * Generic <code>String</code> getter, in case you want to work with the entity as a Map-like construct.
     * @param fieldName the field to retrieve
     * @return the <code>String</code> value for that field
     */
    public String getString(String fieldName);

    /**
     * Generic <code>Boolean</code> getter, in case you want to work with the entity as a Map-like construct.
     * @param fieldName the field to retrieve
     * @return the <code>Boolean</code> value for that field
     */
    public Boolean getBoolean(String fieldName);

    /**
     * Generic <code>Double</code> getter, in case you want to work with the entity as a Map-like construct.
     * @param fieldName the field to retrieve
     * @return the <code>Double</code> value for that field
     */
    public Double getDouble(String fieldName);

    /**
     * Generic <code>Float</code> getter, in case you want to work with the entity as a Map-like construct.
     * @param fieldName the field to retrieve
     * @return the <code>Float</code> value for that field
     */
    public Float getFloat(String fieldName);

    /**
     * Generic <code>Long</code> getter, in case you want to work with the entity as a Map-like construct.
     * @param fieldName the field to retrieve
     * @return the <code>Long</code> value for that field
     */
    public Long getLong(String fieldName);

    /**
     * Generic <code>BigDecimal</code> getter, in case you want to work with the entity as a Map-like construct.
     * @param fieldName the field to retrieve
     * @return the <code>BigDecimal</code> value for that field
     */
    public BigDecimal getBigDecimal(String fieldName);

    /**
     * Generic <code>Timestamp</code> getter, in case you want to work with the entity as a Map-like construct.
     * @param fieldName the field to retrieve
     * @return the <code>Timestamp</code> value for that field
     */
    public Timestamp getTimestamp(String fieldName);

    /**
     * Generic setter, in case you want to work with the entity as a Map-like construct.
     * @param fieldName the field to set
     * @param value the value to set
     */
    public void set(String fieldName, Object value);

    /** {@inheritDoc} */
    public boolean equals(Object obj);

    /** {@inheritDoc} */
    public int hashCode();

    /**
     * Converts to a <code>Map</code>.
     * @return a <code>Map</code> associating the field names and their value
     */
    public Map<String, Object> toMap();

    /**
     * Converts to a <code>Map</code> only including a subset of the fields.
     * @param fields the list of field to include in the <code>Map</code>
     * @return a <code>Map</code> associating the field names and their value
     */
    public Map<String, Object> toMap(Iterable<String> fields);

    /**
     * Converts to a <code>Map</code> without the Ofbiz fields.
     * @return a <code>Map</code> with of this entity without the Ofbiz fields
     */
    public Map<String, Object> toMapNoStamps();

    /**
     * Converts from a <code>Map</code>.
     * @param mapValue the <code>Map</code> to convert from
     */
    public void fromMap(Map<String, Object> mapValue);

    /**
     * Converts from another <code>EntityInterface</code>, which may or may not be the same type of entity.
     * @param entity the <code>EntityInterface</code> to convert from
     */
    public void fromEntity(EntityInterface entity);

    /**
     * Gets the base entity name.
     * @return the base entity name
     */
    public String getBaseEntityName();

    /**
     * Checks if this entity is a view entity.
     * View entities are an aggregation of various base entities.
     * @return a <code>boolean</code> value
     */
    public boolean isView();

    /**
     * Gets the next guaranteed unique sequence ID for this entity, if the named sequence does not exist it will be created.
     * @return a <code>String</code> value
     */
    public String getNextSeqId();

    /**
     * Sets this entity given sub-sequence field to the next available value.
     * @param sequenceFieldName the field representing the sub-sequence
     * @exception RepositoryException if an error occurs
     */
    public void setNextSubSeqId(String sequenceFieldName) throws RepositoryException;

    /**
     * Sets this entity given sub-sequence field to the next available value.
     * @param sequenceFieldName the field representing the sub-sequence
     * @param numericPadding the length of the sequence string padded with 0
     * @exception RepositoryException if an error occurs
     */
    public void setNextSubSeqId(String sequenceFieldName, int numericPadding) throws RepositoryException;

    /**
     * Describe <code>setNextSubSeqId</code> method here.
     * @param sequenceFieldName the field representing the sub-sequence
     * @param numericPadding the length of the sequence string padded with 0
     * @param incrementBy the increment for the next sub-sequence compared to the highest found
     * @exception RepositoryException if an error occurs
     */
    public void setNextSubSeqId(String sequenceFieldName, int numericPadding, int incrementBy) throws RepositoryException;

    /**
     * Sets the repository for this entity.
     * @param repository a <code>RepositoryInterface</code> value
     */
    public void initRepository(RepositoryInterface repository);

    /**
     * Gets the base repository for this entity, the one that was set by <code>initRepository</code>.
     * @return the entity <code>RepositoryInterface</code> value
     */
    public RepositoryInterface getBaseRepository();

    /**
     * Get the list of fields forming the primary key for this entity.
     * @return the list of fields names forming the primary key
     */
    public List<String> getPrimaryKeyNames();

    /**
     * Get the list of fields not forming the primary key for this entity.
     * @return the list of fields names not forming the primary key
     */
    public List<String> getNonPrimaryKeyNames();

    /**
     * Get the list of fields for this entity.
     * @return the list of fields
     */
    public List<String> getAllFieldsNames();

    /**
     * Sets fields on this entity from the <code>Map</code> of fields passed in.
     * @param fields the fields <code>Map</code> to get the values from
     */
    public void setAllFields(Map<String, Object> fields);

    /**
     * Sets fields on this entity from the <code>Map</code> of fields passed in.
     * @param fields The fields <code>Map</code> to get the values from
     * @param setIfEmpty Used to specify whether empty/null values in the field <code>Map</code> should over-write non-empty values in this entity
     */
    public void setAllFields(Map<String, Object> fields, boolean setIfEmpty);

    /**
     * Sets all non PKs fields on this entity from the <code>Map</code> of fields passed in.
     * @param fields the fields <code>Map</code> to get the values from
     */
    public void setNonPKFields(Map<String, Object> fields);

    /**
     * Sets all non PKs fields on this entity from the <code>Map</code> of fields passed in.
     * @param fields the fields <code>Map</code> to get the values from
     * @param setIfEmpty Used to specify whether empty/null values in the field <code>Map</code> should over-write non-empty values in this entity
     */
    public void setNonPKFields(Map<String, Object> fields, boolean setIfEmpty);

    /**
     * Sets all PKs fields on this entity from the <code>Map</code> of fields passed in.
     * @param fields The fields <code>Map</code> to get the values from
     */
    public void setPKFields(Map<String, Object> fields);

    /**
     * Sets all PKs fields on this entity from the <code>Map</code> of fields passed in.
     * @param fields the fields Map to get the values from
     * @param setIfEmpty used to specify whether empty/null values in the field <code>Map</code> should over-write non-empty values in this entity
     */
    public void setPKFields(Map<String, Object> fields, boolean setIfEmpty);

    /**
     * Sets fields on this entity from the <code>Map</code> of fields passed in.
     * @param fields the fields <code>Map</code> to get the values from
     * @param setIfEmpty used to specify whether empty/null values in the field <code>Map</code> should over-write non-empty values in this entity
     * @param pks if null, get all values, if TRUE just get the PKs, if FALSE just get the non-PKs
     */
    public void setAllFields(Map<String, Object> fields, boolean setIfEmpty, Boolean pks);

    /**
     * Gets the entity related to this entity by the given relation.
     * @param relation the name of the relation between the two entities
     * @return an <code>EntityInterface</code> value
     * @throws RepositoryException if an error occurs
     */
    public EntityInterface getRelatedOne(String relation) throws RepositoryException;

    /**
     * Gets the entity related to this entity, where the relation name match the related entity name.
     * @param <T> class of entity to return
     * @param entityName the name the related entity
     * @return an <code>EntityInterface</code> value
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> T getRelatedOne(Class<T> entityName) throws RepositoryException;

    /**
     * Gets the entity related to this entity, given both the related entity name and the relation name.
     * @param <T> class of entity to return
     * @param entityName the name the related entity
     * @param relation the name of the relation between the two entities
     * @return an <code>EntityInterface</code> value
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> T getRelatedOne(Class<T> entityName, String relation) throws RepositoryException;

    /**
     * Gets the entity related to this entity by the given relation using the cache.
     * @param relation the name of the relation between the two entities
     * @return an <code>EntityInterface</code> value
     * @throws RepositoryException if an error occurs
     */
    public EntityInterface getRelatedOneCache(String relation) throws RepositoryException;

    /**
     * Gets the entity related to this entity using the cache, where the relation name match the related entity name.
     * @param <T> class of entity to return
     * @param entityName the name the related entity
     * @return an <code>EntityInterface</code> value
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> T getRelatedOneCache(Class<T> entityName) throws RepositoryException;

    /**
     * Gets the entity related to this entity using the cache, given both the related entity name and the relation name.
     * @param <T> class of entity to return
     * @param entityName the name the related entity
     * @param relation the name of the relation between the two entities
     * @return an <code>EntityInterface</code> value
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> T getRelatedOneCache(Class<T> entityName, String relation) throws RepositoryException;

    /**
     * Gets the list of related entities to this entity by the given relation name.
     * @param relation the name of the relation between the two entities
     * @return a list of <code>EntityInterface</code> value
     * @throws RepositoryException if an error occurs
     */
    public List<? extends EntityInterface> getRelated(String relation) throws RepositoryException;

    /**
     * Gets the list of related entities to this entity, where the relation name match the related entity name.
     * @param <T> class of entity to return
     * @param entityName the name of the related entities
     * @return a list of <code>EntityInterface</code> value
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> List<T> getRelated(Class<T> entityName) throws RepositoryException;

    /**
     * Gets the list of related entities to this entity, where the relation name match the related entity name.
     * @param <T> class of entity to return
     * @param entityName the name of the related entities
     * @param orderBy the fields of the related entity to order the query by; may be null; optionally add a " ASC" for ascending or " DESC" for descending
     * @return a list of <code>EntityInterface</code> value
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> List<T> getRelated(Class<T> entityName, List<String> orderBy) throws RepositoryException;

    /**
     * Gets the list of related entities to this entity, given both the related entity name and the relation name.
     * @param <T> class of entity to return
     * @param entityName the name of the related entities
     * @param relation the name of the relation between the entities
     * @return a list of <code>EntityInterface</code> value
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> List<T> getRelated(Class<T> entityName, String relation) throws RepositoryException;

    /**
     * Gets the list of related entities to this entity, given both the related entity name and the relation name.
     * @param <T> class of entity to return
     * @param entityName the name of the related entities
     * @param relation the name of the relation between the entities
     * @param orderBy the fields of the related entity to order the query by; may be null; optionally add a " ASC" for ascending or " DESC" for descending
     * @return a list of <code>EntityInterface</code> value
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> List<T> getRelated(Class<T> entityName, String relation, List<String> orderBy) throws RepositoryException;

    /**
     * Gets the list of related entities to this entity by the given relation name using the cache.
     * @param relation the name of the relation between the two entities
     * @return a list of <code>EntityInterface</code> value
     * @throws RepositoryException if an error occurs
     */
    public List<? extends EntityInterface> getRelatedCache(String relation) throws RepositoryException;

    /**
     * Gets the list of related entities to this entity using the cache, where the relation name match the related entity name.
     * @param <T> class of entity to return
     * @param entityName the name of the related entities
     * @return a list of <code>EntityInterface</code> value
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> List<T> getRelatedCache(Class<T> entityName) throws RepositoryException;

    /**
     * Gets the list of related entities to this entity using the cache, given both the related entity name and the relation name.
     * @param <T> class of entity to return
     * @param entityName the name of the related entities
     * @param relation the name of the relation between the entities
     * @return a list of <code>EntityInterface</code> value
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> List<T> getRelatedCache(Class<T> entityName, String relation) throws RepositoryException;
}
