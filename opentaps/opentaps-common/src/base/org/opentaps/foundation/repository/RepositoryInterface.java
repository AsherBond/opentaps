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
package org.opentaps.foundation.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.ofbiz.entity.condition.EntityCondition;
import org.opentaps.foundation.entity.EntityFieldInterface;
import org.opentaps.foundation.entity.EntityInterface;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.entity.util.EntityListIterator;
import org.opentaps.foundation.infrastructure.DomainContextInterface;

/**
 * Repository as defined in "Domain Driven Design" is used to encapsulate the retrieval and persistence of Entity objects.
 * This interface defines common elements for Repositories across frameworks.
 */
public interface RepositoryInterface extends DomainContextInterface {

    /**
     * Creates the given entity in the database.
     * @param entity the entity to create
     * @throws RepositoryException if an error occurs
     */
    public void create(EntityInterface entity) throws RepositoryException;

    /**
     * Removes the given entity in the database.
     * @param entity the entity to remove
     * @throws RepositoryException if an error occurs
     */
    public void remove(EntityInterface entity) throws RepositoryException;

    /**
     * Removes the given entity in the database.
     * @param entities the list of entity to remove
     * @throws RepositoryException if an error occurs
     */
    public void remove(Collection<? extends EntityInterface> entities) throws RepositoryException;

    /**
     * Updates an entity in the database.
     * @param entity the entity to update
     * @throws RepositoryException if an error occurs
     */
    public void update(EntityInterface entity) throws RepositoryException;

    /**
     * Updates an entity in the database.
     * @param entities the list of entity to update
     * @throws RepositoryException if an error occurs
     */
    public void update(Collection<? extends EntityInterface> entities) throws RepositoryException;

    /**
     * Creates or updates an entity in the database.
     * @param entity the entity to create or update
     * @throws RepositoryException if an error occurs
     */
    public void createOrUpdate(EntityInterface entity) throws RepositoryException;

    /**
     * Creates or updates a list of entities in the database.
     * @param entities the entities to create or update
     * @throws RepositoryException if an error occurs
     */
    public void createOrUpdate(Collection<? extends EntityInterface> entities) throws RepositoryException;

    /**
     * Gets the next guaranteed unique sequence ID for the given sequence name, if the named sequence does not exist it will be created.
     * @param seqName the name of the sequence
     * @return a <code>String</code> value
     */
    public String getNextSeqId(String seqName);

    /**
     * Gets the next guaranteed unique sequence ID for the given sequence name, if the named sequence does not exist it will be created.
     * @param seqName the name of the sequence
     * @param staggerMax the maximum amount to stagger the sequenced ID, if 1 the sequence will be incremented by 1, otherwise the current sequence ID will be incremented by a value between 1 and staggerMax
     * @return a <code>String</code> value
     */
    public String getNextSeqId(String seqName, long staggerMax);

    /**
     * Gets the next guaranteed unique sequence ID for the given entity, if the named sequence does not exist it will be created.
     * @param entity the entity which name will be used as the sequence name
     * @return a <code>String</code> value
     */
    public String getNextSeqId(EntityInterface entity);

    /**
     * Gets the next incremental value for the entity sub-sequence identifier.
     * @param entity an <code>EntityInterface</code> value
     * @param sequenceFieldName the field representing the sub-sequence
     * @param numericPadding the length of the sequence string padded with 0
     * @param incrementBy the increment for the next sub-sequence compared to the highest found
     * @return a <code>String</code> value
     * @exception RepositoryException if an error occurs
     */
    public String getNextSubSeqId(EntityInterface entity, String sequenceFieldName, int numericPadding, int incrementBy) throws RepositoryException;

    /**
     * Find related entity.
     * This method tries to load a base entity which Class matches the relation name in the org.opentaps.base.entities package.
     * If no valid class is found for the relation, it raises a <code>RepositoryException</code>.
     * This method is provided for backward compatibility, it is recommended use the type safe versions.
     * @param <T> the entity class to return
     * @param relation name of the relation
     * @param entity existing entity for which to find the relation
     * @return the related entity, or null
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> EntityInterface getRelatedOne(String relation, T entity) throws RepositoryException;

    /**
     * Find related entity.
     * @param <T> the entity class to return
     * @param <T2> the entity class for which to find the relation
     * @param entityName class of the related entity return, which defines the name of the relation
     * @param entity existing entity for which to find the relation
     * @return the related entity, or null
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface, T2 extends EntityInterface> T getRelatedOne(Class<T> entityName, T2 entity) throws RepositoryException;

    /**
     * Find related entity.
     * @param <T> the entity class to return
     * @param <T2> the entity class for which to find the relation
     * @param entityName class of the related entity return, which defines the name of the relation
     * @param entity existing entity for which to find the relation
     * @param relation the name of the relation to use
     * @return the related entity, or null
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface, T2 extends EntityInterface> T getRelatedOne(Class<T> entityName, String relation, T2 entity) throws RepositoryException;

    /**
     * Find related entity using the cache.
     * This method tries to load a base entity which Class matches the relation name in the org.opentaps.base.entities package.
     * If no valid class is found for the relation, it raises a <code>RepositoryException</code>.
     * This method is provided for backward compatibility, it is recommended use the type safe versions.
     * @param <T> the entity class to return
     * @param relation name of the relation
     * @param entity existing entity for which to find the relation
     * @return the related entity, or null
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> EntityInterface getRelatedOneCache(String relation, T entity) throws RepositoryException;

    /**
     * Find related entity using the cache.
     * @param <T> the entity class to return
     * @param <T2> the entity class for which to find the relation
     * @param entityName class of the related entity return, which defines the name of the relation
     * @param entity existing entity for which to find the relation
     * @return the related entity, or null
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface, T2 extends EntityInterface> T getRelatedOneCache(Class<T> entityName, T2 entity) throws RepositoryException;

    /**
     * Find related entity using the cache.
     * @param <T> the entity class to return
     * @param <T2> the entity class for which to find the relation
     * @param entityName class of the related entity return, which defines the name of the relation
     * @param entity existing entity for which to find the relation
     * @param relation the name of the relation to use
     * @return the related entity, or null
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface, T2 extends EntityInterface> T getRelatedOneCache(Class<T> entityName, String relation, T2 entity) throws RepositoryException;

    /**
     * Find related entities.
     * This method tries to load base entities which Class matches the relation name in the org.opentaps.base.entities package.
     * If no valid class is found for the relation, it raises a <code>RepositoryException</code>.
     * This method is provided for backward compatibility, it is recommended use the type safe versions.
     * @param <T> the entity class to return
     * @param relation name of the relation
     * @param entity existing entity for which to find the relation
     * @return the related entity, or null
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> List<? extends EntityInterface> getRelated(String relation, T entity) throws RepositoryException;

    /**
     * Find related entities.
     * This method tries to load base entities which Class matches the relation name in the org.opentaps.base.entities package.
     * If no valid class is found for the relation, it raises a <code>RepositoryException</code>.
     * This method is provided for backward compatibility, it is recommended use the type safe versions.
     * @param <T> the entity class to return
     * @param relation name of the relation
     * @param entity existing entity for which to find the relation
     * @param orderBy the fields of the related entity to order the query by; may be null; optionally add a " ASC" for ascending or " DESC" for descending
     * @return the related entity, or null
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> List<? extends EntityInterface> getRelated(String relation, T entity, List<String> orderBy) throws RepositoryException;

    /**
     * Find related entities.
     * @param <T> the entity class to return
     * @param <T2> the entity class for which to find the relation
     * @param entityName class of the related entity return, which defines the name of the relation
     * @param entity existing entity for which to find the relation
     * @return the list of related entities
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface, T2 extends EntityInterface> List<T> getRelated(Class<T> entityName, T2 entity) throws RepositoryException;

    /**
     * Find related entities.
     * @param <T> the entity class to return
     * @param <T2> the entity class for which to find the relation
     * @param entityName class of the related entity return, which defines the name of the relation
     * @param entity existing entity for which to find the relation
     * @param orderBy the fields of the related entity to order the query by; may be null; optionally add a " ASC" for ascending or " DESC" for descending
     * @return the list of related entities
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface, T2 extends EntityInterface> List<T> getRelated(Class<T> entityName, T2 entity, List<String> orderBy) throws RepositoryException;

    /**
     * Find related entities.
     * @param <T> the entity class to return
     * @param <T2> the entity class for which to find the relation
     * @param entityName class of the related entity return, which defines the name of the relation
     * @param entity existing entity for which to find the relation
     * @param relation the name of the relation to use
     * @return the list of related entities
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface, T2 extends EntityInterface> List<T> getRelated(Class<T> entityName, String relation, T2 entity) throws RepositoryException;

    /**
     * Find related entities.
     * @param <T> the entity class to return
     * @param <T2> the entity class for which to find the relation
     * @param entityName class of the related entity return, which defines the name of the relation
     * @param entity existing entity for which to find the relation
     * @param relation the name of the relation to use
     * @param orderBy the fields of the related entity to order the query by; may be null; optionally add a " ASC" for ascending or " DESC" for descending
     * @return the list of related entities
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface, T2 extends EntityInterface> List<T> getRelated(Class<T> entityName, String relation, T2 entity, List<String> orderBy) throws RepositoryException;

    /**
     * Find related entities using the cache.
     * This method tries to load base entities which Class matches the relation name in the org.opentaps.base.entities package.
     * If no valid class is found for the relation, it raises a <code>RepositoryException</code>.
     * This method is provided for backward compatibility, it is recommended use the type safe versions.
     * @param <T> the entity class to return
     * @param relation name of the relation
     * @param entity existing entity for which to find the relation
     * @return the related entity, or null
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> List<? extends EntityInterface> getRelatedCache(String relation, T entity) throws RepositoryException;

    /**
     * Find related entities using the cache.
     * @param <T> the entity class to return
     * @param <T2> the entity class for which to find the relation
     * @param entityName class of the related entity return, which defines the name of the relation
     * @param entity existing entity for which to find the relation
     * @return the list of related entities
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface, T2 extends EntityInterface> List<T> getRelatedCache(Class<T> entityName, T2 entity) throws RepositoryException;

    /**
     * Find related entities using the cache.
     * @param <T> the entity class to return
     * @param <T2> the entity class for which to find the relation
     * @param entityName class of the related entity return, which defines the name of the relation
     * @param entity existing entity for which to find the relation
     * @param relation the name of the relation to use
     * @return the list of related entities
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface, T2 extends EntityInterface> List<T> getRelatedCache(Class<T> entityName, String relation, T2 entity) throws RepositoryException;

    //
    // A set of methods used to retrieve entities from the repository.
    //

    /* findOne */

    /**
     * Find one entity by primary key.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param pk a map describing the primary key
     * @return the corresponding entity, or null if it is not found
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> T findOne(Class<T> entityName, Map<? extends EntityFieldInterface<? super T>, Object> pk) throws RepositoryException;

    /* findOneCache */

    /**
     * Find one entity by primary key using the cache.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param pk a map describing the primary key
     * @return the corresponding entity, or null if it is not found
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> T findOneCache(Class<T> entityName, Map<? extends EntityFieldInterface<? super T>, Object> pk) throws RepositoryException;

    /* findOneNotNull, same as findOne but throws EntityNotFoundException when the entity is not found */

    /**
     * Find one entity by primary key.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param pk a map describing the primary key
     * @return the corresponding entity
     * @throws RepositoryException if an error occurs
     * @throws EntityNotFoundException if an error occurs
     */
    public <T extends EntityInterface> T findOneNotNull(Class<T> entityName, Map<? extends EntityFieldInterface<? super T>, Object> pk) throws RepositoryException, EntityNotFoundException;

    /**
     * Find one entity by primary key.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param pk a map describing the primary key
     * @param message the exception message to use to build the EntityNotFoundException
     * @return the corresponding entity
     * @throws RepositoryException if an error occurs
     * @throws EntityNotFoundException if an error occurs
     */
    public <T extends EntityInterface> T findOneNotNull(Class<T> entityName, Map<? extends EntityFieldInterface<? super T>, Object> pk, String message) throws RepositoryException, EntityNotFoundException;

    /**
     * Find one entity by primary key.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param pk a map describing the primary key
     * @param messageLabel the message label to use to build the EntityNotFoundException
     * @param context the context map used to expand the messageLabel
     * @return the corresponding entity
     * @throws RepositoryException if an error occurs
     * @throws EntityNotFoundException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public <T extends EntityInterface> T findOneNotNull(Class<T> entityName, Map<? extends EntityFieldInterface<? super T>, Object> pk, String messageLabel, Map context) throws RepositoryException, EntityNotFoundException;

    /* findOneNotNullCache, same as findOneCache but throws EntityNotFoundException when the entity is not found */

    /**
     * Find one entity by primary key using the cache.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param pk a map describing the primary key
     * @return the corresponding entity
     * @throws RepositoryException if an error occurs
     * @throws EntityNotFoundException if an error occurs
     */
    public <T extends EntityInterface> T findOneNotNullCache(Class<T> entityName, Map<? extends EntityFieldInterface<? super T>, Object> pk) throws RepositoryException, EntityNotFoundException;

    /**
     * Find one entity by primary key using the cache.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param pk a map describing the primary key
     * @param message the exception message to use to build the EntityNotFoundException
     * @return the corresponding entity
     * @throws RepositoryException if an error occurs
     * @throws EntityNotFoundException if an error occurs
     */
    public <T extends EntityInterface> T findOneNotNullCache(Class<T> entityName, Map<? extends EntityFieldInterface<? super T>, Object> pk, String message) throws RepositoryException, EntityNotFoundException;

    /**
     * Find one entity by primary key using the cache.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param pk a map describing the primary key
     * @param messageLabel the message label to use to build the EntityNotFoundException
     * @param context the context map used to expand the messageLabel
     * @return the corresponding entity
     * @throws RepositoryException if an error occurs
     * @throws EntityNotFoundException if an error occurs
     */
    public <T extends EntityInterface> T findOneNotNullCache(Class<T> entityName, Map<? extends EntityFieldInterface<? super T>, Object> pk, String messageLabel, Map<String, Object> context) throws RepositoryException, EntityNotFoundException;

    /* find all */

    /**
     * Find all entities.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @return the list of entities found
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> List<T> findAll(Class<T> entityName) throws RepositoryException;

    /**
     * Find all entities.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param orderBy list of fields to order by
     * @return the list of entities found
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> List<T> findAll(Class<T> entityName, List<String> orderBy) throws RepositoryException;

    /* find all cache */

    /**
     * Find all entities using the cache.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @return the list of entities found
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> List<T> findAllCache(Class<T> entityName) throws RepositoryException;

    /**
     * Find all entities using the cache.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param orderBy list of fields to order by
     * @return the list of entities found
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> List<T> findAllCache(Class<T> entityName, List<String> orderBy) throws RepositoryException;

    /* findIterator by Map of Field: value */

    /**
     * Find a iterator of entities by conditions.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param conditions a Map of fields -> value that the entities must all match
     * @return an <code>EntityListIterator</code> instance
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> EntityListIterator<T> findIterator(Class<T> entityName, Map<? extends EntityFieldInterface<? super T>, Object> conditions) throws RepositoryException;

    /**
     * Find a iterator of entities by conditions.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param conditions a Map of fields -> value that the entities must all match
     * @param orderBy list of fields to order by
     * @return an <code>EntityListIterator</code> instance
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> EntityListIterator<T> findIterator(Class<T> entityName, Map<? extends EntityFieldInterface<? super T>, Object> conditions, List<String> orderBy) throws RepositoryException;

    /**
     * Find a iterator of entities by conditions. Only return a subset of the entity fields and filters out duplicates.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param conditions a Map of fields -> value that the entities must all match
     * @param fields list of fields to select
     * @param orderBy list of fields to order by
     * @return an <code>EntityListIterator</code> instance
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> EntityListIterator<T> findIterator(Class<T> entityName, Map<? extends EntityFieldInterface<? super T>, Object> conditions, List<String> fields, List<String> orderBy) throws RepositoryException;

    /* findPage by Map of Field: value */

    /**
     * Find a page of entities by conditions.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param conditions a Map of fields -> value that the entities must all match
     * @param pageStart the index of the first entity, starting at 0
     * @param pageSize the number of entities to return at most
     * @return the partial list of entities found
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> List<T> findPage(Class<T> entityName, Map<? extends EntityFieldInterface<? super T>, Object> conditions, int pageStart, int pageSize) throws RepositoryException;

    /**
     * Find a page of entities by conditions.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param conditions a Map of fields -> value that the entities must all match
     * @param orderBy list of fields to order by
     * @param pageStart the index of the first entity, starting at 0
     * @param pageSize the number of entities to return at most
     * @return the partial list of entities found
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> List<T> findPage(Class<T> entityName, Map<? extends EntityFieldInterface<? super T>, Object> conditions, List<String> orderBy, int pageStart, int pageSize) throws RepositoryException;

    /**
     * Find a page of entities by conditions. Only return a subset of the entity fields and filters out duplicates.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param conditions a Map of fields -> value that the entities must all match
     * @param fields list of fields to select
     * @param orderBy list of fields to order by
     * @param pageStart the index of the first entity, starting at 0
     * @param pageSize the number of entities to return at most
     * @return the partial list of entities found
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> List<T> findPage(Class<T> entityName, Map<? extends EntityFieldInterface<? super T>, Object> conditions, List<String> fields, List<String> orderBy, int pageStart, int pageSize) throws RepositoryException;

    /* findList by Map of Field: value */

    /**
     * Find entities by conditions.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param conditions a Map of fields -> value that the entities must all match
     * @return the list of entities found
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> List<T> findList(Class<T> entityName, Map<? extends EntityFieldInterface<? super T>, Object> conditions) throws RepositoryException;

    /**
     * Find entities by conditions.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param conditions a Map of fields -> value that the entities must all match
     * @param orderBy list of fields to order by
     * @return the list of entities found
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> List<T> findList(Class<T> entityName, Map<? extends EntityFieldInterface<? super T>, Object> conditions, List<String> orderBy) throws RepositoryException;

    /**
     * Find entities by conditions. Only return a subset of the entity fields and filters out duplicates.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param conditions a Map of fields -> value that the entities must all match
     * @param fields list of fields to select
     * @param orderBy list of fields to order by
     * @return the list of entities found
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> List<T> findList(Class<T> entityName, Map<? extends EntityFieldInterface<? super T>, Object> conditions, List<String> fields, List<String> orderBy) throws RepositoryException;

    /* findListCache by Map of Field: value */

    /**
     * Find entities by conditions using the cache.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param conditions a Map of fields -> value that the entities must all match
     * @return the list of entities found
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> List<T> findListCache(Class<T> entityName, Map<? extends EntityFieldInterface<? super T>, Object> conditions) throws RepositoryException;

    /**
     * Find entities by conditions using the cache.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param conditions a Map of fields -> value that the entities must all match
     * @param orderBy list of fields to order by
     * @return the list of entities found
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> List<T> findListCache(Class<T> entityName, Map<? extends EntityFieldInterface<? super T>, Object> conditions, List<String> orderBy) throws RepositoryException;

    /* findIterator by list of Conditions */

    /**
     * Find a iterator of entities by conditions.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param conditions a List of EntityExpr the entities must all match
     * @return an <code>EntityListIterator</code> instance
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> EntityListIterator<T> findIterator(Class<T> entityName, List<? extends EntityCondition> conditions) throws RepositoryException;

    /**
     * Find a iterator of entities by conditions.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param conditions a List of EntityExpr the entities must all match
     * @param orderBy list of fields to order by
     * @return an <code>EntityListIterator</code> instance
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> EntityListIterator<T> findIterator(Class<T> entityName, List<? extends EntityCondition> conditions, List<String> orderBy) throws RepositoryException;

    /**
     * Find a iterator of entities by conditions. Only return a subset of the entity fields and filters out duplicates.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param conditions a List of EntityExpr the entities must all match
     * @param fields list of fields to select
     * @param orderBy list of fields to order by
     * @return an <code>EntityListIterator</code> instance
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> EntityListIterator<T> findIterator(Class<T> entityName, List<? extends EntityCondition> conditions, List<String> fields, List<String> orderBy) throws RepositoryException;

    /**
     * Find a iterator of entities by conditions.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param condition the EntityCondition used to find the entities
     * @return an <code>EntityListIterator</code> instance
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> EntityListIterator<T> findIterator(Class<T> entityName, EntityCondition condition) throws RepositoryException;

    /**
     * Find a iterator of entities by conditions.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param condition the EntityCondition used to find the entities
     * @param orderBy list of fields to order by
     * @return an <code>EntityListIterator</code> instance
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> EntityListIterator<T> findIterator(Class<T> entityName, EntityCondition condition, List<String> orderBy) throws RepositoryException;

    /**
     * Find a iterator of entities by conditions. Only return a subset of the entity fields and filters out duplicates.
     * @param <T> the entity class
     * @param entityName class to find
     * @param condition the EntityCondition used to find the entities
     * @param fields the list of field to select
     * @param orderBy list of fields to order by
     * @return an <code>EntityListIterator</code> instance
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> EntityListIterator<T> findIterator(Class<T> entityName, EntityCondition condition, List<String> fields, List<String> orderBy) throws RepositoryException;

    /* findPage by list of Conditions */

    /**
     * Find a page of entities by conditions.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param conditions a List of EntityExpr the entities must all match
     * @param pageStart the index of the first entity, starting at 0
     * @param pageSize the number of entities to return at most
     * @return the partial list of entities found
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> List<T> findPage(Class<T> entityName, List<? extends EntityCondition> conditions, int pageStart, int pageSize) throws RepositoryException;

    /**
     * Find a page of entities by conditions.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param conditions a List of EntityExpr the entities must all match
     * @param orderBy list of fields to order by
     * @param pageStart the index of the first entity, starting at 0
     * @param pageSize the number of entities to return at most
     * @return the partial list of entities found
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> List<T> findPage(Class<T> entityName, List<? extends EntityCondition> conditions, List<String> orderBy, int pageStart, int pageSize) throws RepositoryException;

    /**
     * Find a page of entities by conditions. Only return a subset of the entity fields and filters out duplicates.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param conditions a List of EntityExpr the entities must all match
     * @param fields list of fields to select
     * @param orderBy list of fields to order by
     * @param pageStart the index of the first entity, starting at 0
     * @param pageSize the number of entities to return at most
     * @return the partial list of entities found
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> List<T> findPage(Class<T> entityName, List<? extends EntityCondition> conditions, List<String> fields, List<String> orderBy, int pageStart, int pageSize) throws RepositoryException;

    /**
     * Find a page of entities by conditions.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param condition the EntityCondition used to find the entities
     * @param pageStart the index of the first entity, starting at 0
     * @param pageSize the number of entities to return at most
     * @return the partial list of entities found
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> List<T> findPage(Class<T> entityName, EntityCondition condition, int pageStart, int pageSize) throws RepositoryException;

    /**
     * Find a page of entities by conditions.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param condition the EntityCondition used to find the entities
     * @param orderBy list of fields to order by
     * @param pageStart the index of the first entity, starting at 0
     * @param pageSize the number of entities to return at most
     * @return the partial list of entities found
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> List<T> findPage(Class<T> entityName, EntityCondition condition, List<String> orderBy, int pageStart, int pageSize) throws RepositoryException;

    /**
     * Find a page of entities by conditions. Only return a subset of the entity fields and filters out duplicates.
     * @param <T> the entity class
     * @param entityName class to find
     * @param condition the EntityCondition used to find the entities
     * @param fields the list of field to select
     * @param orderBy list of fields to order by
     * @param pageStart the index of the first entity, starting at 0
     * @param pageSize the number of entities to return at most
     * @return the partial list of entities found
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> List<T> findPage(Class<T> entityName, EntityCondition condition, List<String> fields, List<String> orderBy, int pageStart, int pageSize) throws RepositoryException;

    /* findList by list of Conditions */

    /**
     * Find entities by conditions.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param conditions a List of EntityExpr the entities must all match
     * @return the list of entities found
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> List<T> findList(Class<T> entityName, List<? extends EntityCondition> conditions) throws RepositoryException;

    /**
     * Find entities by conditions.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param conditions a List of EntityExpr the entities must all match
     * @param orderBy list of fields to order by
     * @return the list of entities found
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> List<T> findList(Class<T> entityName, List<? extends EntityCondition> conditions, List<String> orderBy) throws RepositoryException;

    /**
     * Find entities by conditions. Only return a subset of the entity fields and filters out duplicates.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param conditions a List of EntityExpr the entities must all match
     * @param fields list of fields to select
     * @param orderBy list of fields to order by
     * @return the list of entities found
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> List<T> findList(Class<T> entityName, List<? extends EntityCondition> conditions, List<String> fields, List<String> orderBy) throws RepositoryException;

    /**
     * Find entities by conditions using the cache.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param conditions a List of EntityExpr the entities must all match
     * @return the list of entities found
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> List<T> findListCache(Class<T> entityName, List<? extends EntityCondition> conditions) throws RepositoryException;

    /**
     * Find entities by conditions using the cache.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param conditions a List of EntityExpr the entities must all match
     * @param orderBy list of fields to order by
     * @return the list of entities found
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> List<T> findListCache(Class<T> entityName, List<? extends EntityCondition> conditions, List<String> orderBy) throws RepositoryException;

    /**
     * Find entities by conditions.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param condition the EntityCondition used to find the entities
     * @return the list of entities found
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> List<T> findList(Class<T> entityName, EntityCondition condition) throws RepositoryException;

    /**
     * Find entities by conditions.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param condition the EntityCondition used to find the entities
     * @param orderBy list of fields to order by
     * @return the list of entities found
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> List<T> findList(Class<T> entityName, EntityCondition condition, List<String> orderBy) throws RepositoryException;

    /**
     * Find entities by conditions. Only return a subset of the entity fields and filters out duplicates.
     * @param <T> the entity class
     * @param entityName class to find
     * @param condition the EntityCondition used to find the entities
     * @param fields the list of field to select
     * @param orderBy list of fields to order by
     * @return the list of entities found
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> List<T> findList(Class<T> entityName, EntityCondition condition, List<String> fields, List<String> orderBy) throws RepositoryException;

    /**
     * Find entities by conditions using the cache.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param condition the EntityCondition used to find the entities
     * @return the list of entities found
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> List<T> findListCache(Class<T> entityName, EntityCondition condition) throws RepositoryException;

    /**
     * Find entities by conditions using the cache.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param condition the EntityCondition used to find the entities
     * @param orderBy list of fields to order by
     * @return the list of entities found
     * @throws RepositoryException if an error occurs
     */
    public <T extends EntityInterface> List<T> findListCache(Class<T> entityName, EntityCondition condition, List<String> orderBy) throws RepositoryException;

    // map methods, used to construct a map of field -> condition

    /**
     * Constructs a <code>Map</code> of Field Conditions to be used in the find methods.
     * @param <T> the entity class the condition is for
     * @param key1 the field
     * @param value1 the field value
     * @return a condition map to be used in the find methods
     */
    public <T extends EntityInterface> Map<? extends EntityFieldInterface<? super T>, Object> map(EntityFieldInterface<? super T> key1, Object value1);

    /**
     * Constructs a <code>Map</code> of Field Conditions to be used in the find methods.
     * @param <T> the entity class the condition is for
     * @param key1 the field
     * @param value1 the field value
     * @param key2 the field
     * @param value2 the field value
     * @return a condition map to be used in the find methods
     */
    public <T extends EntityInterface> Map<? extends EntityFieldInterface<? super T>, Object> map(EntityFieldInterface<? super T> key1, Object value1, EntityFieldInterface<? super T> key2, Object value2);

    /**
     * Constructs a <code>Map</code> of Field Conditions to be used in the find methods.
     * @param <T> the entity class the condition is for
     * @param key1 the field
     * @param value1 the field value
     * @param key2 the field
     * @param value2 the field value
     * @param key3 the field
     * @param value3 the field value
     * @return a condition map to be used in the find methods
     */
    public <T extends EntityInterface> Map<? extends EntityFieldInterface<? super T>, Object> map(EntityFieldInterface<? super T> key1, Object value1, EntityFieldInterface<? super T> key2, Object value2, EntityFieldInterface<? super T> key3, Object value3);

    /**
     * Constructs a <code>Map</code> of Field Conditions to be used in the find methods.
     * @param <T> the entity class the condition is for
     * @param key1 the field
     * @param value1 the field value
     * @param key2 the field
     * @param value2 the field value
     * @param key3 the field
     * @param value3 the field value
     * @param key4 the field
     * @param value4 the field value
     * @return a condition map to be used in the find methods
     */
    public <T extends EntityInterface> Map<? extends EntityFieldInterface<? super T>, Object> map(EntityFieldInterface<? super T> key1, Object value1, EntityFieldInterface<? super T> key2, Object value2, EntityFieldInterface<? super T> key3, Object value3, EntityFieldInterface<? super T> key4, Object value4);

    /**
     * Constructs a <code>Map</code> of Field Conditions to be used in the find methods.
     * @param <T> the entity class the condition is for
     * @param key1 the field
     * @param value1 the field value
     * @param key2 the field
     * @param value2 the field value
     * @param key3 the field
     * @param value3 the field value
     * @param key4 the field
     * @param value4 the field value
     * @param key5 the field
     * @param value5 the field value
     * @return a condition map to be used in the find methods
     */
    public <T extends EntityInterface> Map<? extends EntityFieldInterface<? super T>, Object> map(EntityFieldInterface<? super T> key1, Object value1, EntityFieldInterface<? super T> key2, Object value2, EntityFieldInterface<? super T> key3, Object value3, EntityFieldInterface<? super T> key4, Object value4, EntityFieldInterface<? super T> key5, Object value5);

    /**
     * Constructs a <code>Map</code> of Field Conditions to be used in the find methods.
     * @param <T> the entity class the condition is for
     * @param key1 the field
     * @param value1 the field value
     * @param key2 the field
     * @param value2 the field value
     * @param key3 the field
     * @param value3 the field value
     * @param key4 the field
     * @param value4 the field value
     * @param key5 the field
     * @param value5 the field value
     * @param key6 the field
     * @param value6 the field value
     * @return a condition map to be used in the find methods
     */
    public <T extends EntityInterface> Map<? extends EntityFieldInterface<? super T>, Object> map(EntityFieldInterface<? super T> key1, Object value1, EntityFieldInterface<? super T> key2, Object value2, EntityFieldInterface<? super T> key3, Object value3, EntityFieldInterface<? super T> key4, Object value4, EntityFieldInterface<? super T> key5, Object value5, EntityFieldInterface<? super T> key6, Object value6);

    /**
     * Constructs a <code>Map</code> of Field Conditions to be used in the find methods.
     * @param <T> the entity class the condition is for
     * @param key1 the field
     * @param value1 the field value
     * @param key2 the field
     * @param value2 the field value
     * @param key3 the field
     * @param value3 the field value
     * @param key4 the field
     * @param value4 the field value
     * @param key5 the field
     * @param value5 the field value
     * @param key6 the field
     * @param value6 the field value
     * @param key7 the field
     * @param value7 the field value
     * @return a condition map to be used in the find methods
     */
    public <T extends EntityInterface> Map<? extends EntityFieldInterface<? super T>, Object> map(EntityFieldInterface<? super T> key1, Object value1, EntityFieldInterface<? super T> key2, Object value2, EntityFieldInterface<? super T> key3, Object value3, EntityFieldInterface<? super T> key4, Object value4, EntityFieldInterface<? super T> key5, Object value5, EntityFieldInterface<? super T> key6, Object value6, EntityFieldInterface<? super T> key7, Object value7);
}
