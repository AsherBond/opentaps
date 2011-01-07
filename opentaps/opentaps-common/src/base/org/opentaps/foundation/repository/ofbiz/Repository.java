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
package org.opentaps.foundation.repository.ofbiz;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.model.ModelEntity;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.security.Security;
import org.ofbiz.service.LocalDispatcher;
import org.opentaps.foundation.entity.EntityException;
import org.opentaps.foundation.entity.EntityFieldInterface;
import org.opentaps.foundation.entity.EntityInterface;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.entity.util.EntityListIterator;
import org.opentaps.foundation.exception.FoundationException;
import org.opentaps.foundation.infrastructure.DomainContextInterface;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.RepositoryInterface;
import org.opentaps.foundation.util.FoundationUtils;


/**
 * This is an implementation of the RepositoryInterface for ofbiz.  Since sometimes things should be stored by delegator
 * and sometimes by calling a service, this Repository will make use of both.  It will be constructed from the ofbiz infrastructure
 * class and a UserLogin GenericValue if services are to be called.  Alternatively, if you only want to use the delegator
 * in your Repository, it will can be created with just that;
 *
 */
public class Repository implements RepositoryInterface  {

    private Infrastructure infrastructure;
    private Delegator delegator;
    private LocalDispatcher dispatcher;
    private Security security;
    private User user; // a user associated with an instance of the infrastructure

    /** The options used when performing queries for a subset of fields and filtering duplicates. */
    public static final EntityFindOptions DISTINCT_FIND_OPTIONS = new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, true);
    /** The options used when performing find page queries. */
    public static final EntityFindOptions PAGE_FIND_OPTIONS = new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, false);

    /**
     * Default constructor.
     */
    public Repository() {
        //
    }

    /**
     * Use this for Repositories which will only access the database via the delegator.
     * @param delegator the delegator
     */
    public Repository(Delegator delegator) {
        setDelegator(delegator);
    }

    /**
     * Use this for domain Repositories.
     * @param infrastructure the domain infrastructure
     */
    public Repository(Infrastructure infrastructure) {
        setInfrastructure(infrastructure);
    }

    /**
     * If you want the full infrastructure including the dispatcher, then you must have the User.
     * @param infrastructure the domain infrastructure
     * @param user the domain user
     */
    public Repository(Infrastructure infrastructure, User user) {
        this(infrastructure);
        setUser(user);
    }

    /**
     * If you want the full infrastructure including the dispatcher, then you must have the User.
     * @param infrastructure the domain infrastructure
     * @param userLogin the Ofbiz <code>UserLogin</code> generic value
     */
    public Repository(Infrastructure infrastructure, GenericValue userLogin) {
        this(infrastructure);
        this.user = new User(userLogin);
    }

    /**
     * Repository copy.
     * @param repository the repository to copy
     */
    public Repository(Repository repository) {
        this.delegator = repository.getDelegator();
        this.dispatcher = repository.getDispatcher();
        this.infrastructure = repository.getInfrastructure();
        this.user = repository.getUser();
    }

    /** {@inheritDoc} */
    public void setInfrastructure(Infrastructure infrastructure) {
        this.infrastructure = infrastructure;
        this.dispatcher = infrastructure.getDispatcher();
        setDelegator(infrastructure.getDelegator());
        this.security = infrastructure.getSecurity();
    }

    /** {@inheritDoc} */
    public Infrastructure getInfrastructure() {
        return this.infrastructure;
    }

    /** {@inheritDoc} */
    public void setUser(User user) {
        this.user = user;
    }

    /** {@inheritDoc} */
    public User getUser() {
        return this.user;
    }

    /** {@inheritDoc} */
    public void setDomainContext(DomainContextInterface context) {
        this.setDomainContext(context.getInfrastructure(), context.getUser());
    }

    /** {@inheritDoc} */
    public void setDomainContext(Infrastructure infrastructure, User user) {
        this.setInfrastructure(infrastructure);
        this.setUser(user);
    }

    /**
     * Sets the delegator.
     * @param delegator the delegator
     */
    public void setDelegator(Delegator delegator) {
        this.delegator = delegator;
    }

    /**
     * Gets the delegator.
     * @return the delegator
     * @deprecated Repositories should avoid using the delegator and use the find methods instead
     */
    @Deprecated
    public Delegator getDelegator() {
        return delegator;
    }

    /**
     * Gets the dispatcher.
     * @return the dispatcher
     */
    public LocalDispatcher getDispatcher() {
        return dispatcher;
    }

    // some methods specific to the ofbiz framework.  these convenience methods facility conversion of Entity class objects
    // to and from ofbiz GenericValues and are static so that they can be accessed directly and can exist outside of the repository
    // interfaces in the domains
    /**
     * Return an instance of the entity class from a <code>GenericValue</code>.
     * @param <T> the entity class to load
     * @param entityClass the domain class that will be returned, it must implement <code>EntityInterface</code>
     * @param generic the <code>GenericValue</code> to transform
     * @return the domain object
     * @throws RepositoryException if an error occurs
     */
    public static <T extends EntityInterface> T loadFromGeneric(Class<T> entityClass, GenericValue generic) throws RepositoryException {
        try {
            return (T) FoundationUtils.loadFromMap(entityClass, generic.getAllFields());
        } catch (EntityException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Load a whole list of domain objects from a List of <code>GenericValue</code>.
     * @param <T> the entity class to load
     * @param entityClass the domain class that will be returned, it must implement <code>EntityInterface</code>
     * @param generics the list of <code>GenericValue</code> to transform
     * @return the list of domain objects
     * @throws RepositoryException if an error occurs
     */
    public static <T extends EntityInterface> List<T> loadFromGeneric(Class<T> entityClass, List<GenericValue> generics) throws RepositoryException {
        ArrayList<T> list = new ArrayList<T>();
        for (GenericValue generic : generics) {
            list.add(loadFromGeneric(entityClass, generic));
        }
        return list;
    }

    /**
     * Load a domain object from a <code>GenericValue</code> and set the repository as well.
     * @param <T> the entity class to load
     * @param entityClass the domain class that will be returned, it must implement <code>EntityInterface</code>
     * @param generic the list of <code>GenericValue</code> to transform
     * @param repository the domain repository to set to those domain objects
     * @return the list of domain objects
     * @throws RepositoryException if an error occurs
     */
    public static <T extends EntityInterface> T loadFromGeneric(Class<T> entityClass, GenericValue generic, RepositoryInterface repository) throws RepositoryException {
        if (generic == null) {
            return null;
        }

        T entity = null;

        try {

            // create the Entity object as normal
            entity = FoundationUtils.newInstance(entityClass);
            entity.initRepository(repository);
            entity.fromMap(generic.getAllFields());

        } catch (EntityException e) {
            throw new RepositoryException(e);
        }

        return entity;
    }

    /**
     * Load a whole list of domain objects from a List of <code>GenericValue</code> and setting all of their repositories.
     * @param <T> the entity class to load
     * @param entityClass the domain class that will be returned, it must implement <code>EntityInterface</code>
     * @param generics the list of <code>GenericValue</code> to transform
     * @param repository  the domain repository to set to this domain object
     * @return the list of domain objects
     * @throws RepositoryException if an error occurs
     */
    public static <T extends EntityInterface> List<T> loadFromGeneric(Class<T> entityClass, List<GenericValue> generics, RepositoryInterface repository) throws RepositoryException {
        ArrayList<T> list = new ArrayList<T>();
        for (GenericValue generic : generics) {
            list.add(loadFromGeneric(entityClass, generic, repository));
        }
        return list;
    }

    /**
     * Convert a domain object which has a repository to a <code>GenericValue</code>.
     * @param entity the domain object to convert
     * @return the converted <code>GenericValue</code>
     * @throws RepositoryException if an error occurs
     */
    public static GenericValue genericValueFromEntity(EntityInterface entity) throws RepositoryException {
        try {
            RepositoryInterface repo = entity.getBaseRepository();
            if (repo == null) {
                throw new RepositoryException("No base repository set on entity: " + entity);
            }

            ModelEntity model = repo.getInfrastructure().getDelegator().getModelReader().getModelEntity(entity.getBaseEntityName());
            return GenericValue.create(repo.getInfrastructure().getDelegator(), model, entity.toMap());
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Convert a domain object to a <code>GenericValue</code>.
     * @param delegator the Ofbiz delegator
     * @param entity the domain object to convert
     * @return the converted <code>GenericValue</code>
     * @throws RepositoryException if an error occurs
     */
    public static GenericValue genericValueFromEntity(Delegator delegator, EntityInterface entity) throws RepositoryException {
        try {
            ModelEntity model = delegator.getModelReader().getModelEntity(entity.getBaseEntityName());
            return GenericValue.create(delegator, model, entity.toMap());
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Convert a list of domain objects to a list of <code>GenericValue</code>.
     * @param delegator the Ofbiz delegator
     * @param entities the list of domain objects to convert
     * @return the list of converted <code>GenericValue</code>
     * @throws RepositoryException if an error occurs
     */
    public static List<GenericValue> genericValueFromEntity(Delegator delegator, Collection<? extends EntityInterface> entities) throws RepositoryException {
        List<GenericValue> results = new ArrayList<GenericValue>();
        if (entities.isEmpty()) {
            return results;
        }

        for (EntityInterface entity : entities) {
            results.add(genericValueFromEntity(delegator, entity));
        }
        return results;
    }

    /**
     * Convert a list of domain objects to a list of <code>GenericValue</code>.
     * @param delegator the Ofbiz delegator
     * @param entityName the name of the generic value (obsolete now)
     * @param entities the list of domain objects to convert
     * @return the list of converted <code>GenericValue</code>
     * @throws RepositoryException if an error occurs
     */
    @Deprecated
    public static List<GenericValue> genericValueFromEntity(Delegator delegator, String entityName, Collection<? extends EntityInterface> entities) throws RepositoryException {
        List<GenericValue> results = new ArrayList<GenericValue>();
        if (entities.isEmpty()) {
            return results;
        }

        for (EntityInterface entity : entities) {
            results.add(genericValueFromEntity(delegator, entity));
        }
        return results;
    }

    /**
     * {@inheritDoc}
     * Warning: the Ofbiz delegator implementation is using a broken double checked locking pattern.
     */
    public String getNextSeqId(String seqName) {
        return delegator.getNextSeqId(seqName);
    }

    /**
     * {@inheritDoc}
     * Warning: the Ofbiz delegator implementation is using a broken double checked locking pattern.
     */
    public String getNextSeqId(String seqName, long staggerMax) {
        return delegator.getNextSeqId(seqName, staggerMax);
    }

    /**
     * {@inheritDoc}
     * Warning: the Ofbiz delegator implementation is using a broken double checked locking pattern.
     */
    public String getNextSeqId(EntityInterface entity) {
        return delegator.getNextSeqId(entity.getBaseEntityName());
    }

    /**
     * {@inheritDoc}
     */
    public String getNextSubSeqId(EntityInterface entity, String sequenceFieldName, int numericPadding, int incrementBy) throws RepositoryException {
        GenericValue entityGV = genericValueFromEntity(delegator, entity);
        delegator.setNextSubSeqId(entityGV, sequenceFieldName, numericPadding, incrementBy);
        return entityGV.getString(sequenceFieldName);
    }

    /**
     * If the primary keys are not filled in, they will
     * automatically be generated using the Ofbiz primary key
     * generation algorithm.
     * If one of the keys is a date, the current date will be used.
     * {@inheritDoc}
     */
    public void create(EntityInterface entity) throws RepositoryException {
        throw new RepositoryException("Repository.create() is not implemented yet.");
    }

    /** {@inheritDoc} */
    public void remove(EntityInterface entity) throws RepositoryException {
        try {
            if (entity == null) {
                // don't remove if there is no entity
                return;
            }
            GenericValue value = Repository.genericValueFromEntity(delegator, entity);
            delegator.removeValue(value);
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    /** {@inheritDoc} */
    public void remove(Collection<? extends EntityInterface> entities) throws RepositoryException {
        try {
            List<GenericValue> values = Repository.genericValueFromEntity(delegator, entities);
            delegator.removeAll(values);
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Note that this does not pass through our {@link #create}
     * function in this class, hence no automatic PK generation takes place.
     * {@inheritDoc}
     */
    public void update(EntityInterface entity) throws RepositoryException {
        try {
            GenericValue value = Repository.genericValueFromEntity(delegator, entity);
            delegator.store(value);
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Note that this does not pass through our {@link #create}
     * function in this class, hence no automatic PK generation takes place.
     * {@inheritDoc}
     */
    public void update(Collection<? extends EntityInterface> entities) throws RepositoryException {
        try {
            List<GenericValue> values = Repository.genericValueFromEntity(delegator, entities);
            delegator.storeAll(values);
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    /** {@inheritDoc} */
    public void createOrUpdate(EntityInterface entity) throws RepositoryException {
        try {
            // TODO: support automatic PK creation in this method?  that is, if we're creating, pass to create method above?
            GenericValue value = Repository.genericValueFromEntity(delegator, entity);
            delegator.createOrStore(value);
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    /** {@inheritDoc} */
    public void createOrUpdate(Collection<? extends EntityInterface> entities) throws RepositoryException {
        try {
            List<GenericValue> values = Repository.genericValueFromEntity(delegator, entities);
            delegator.storeAll(values);
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    private <T extends EntityInterface> String getEntityBaseName(Class<T> entityClass) throws RepositoryException {
        try {
            return FoundationUtils.getEntityBaseName(entityClass);
        } catch (EntityException e) {
            throw new RepositoryException(e);
        }
    }

    /*
       These are common patterns to interact with the ofbiz delegator
       They are basically wrappers to the most common findBy.. methods
       and some of the EntityUtil
     */

    /**
     * Get the First of the given or null if the list is empty or null.
     * @param <T> the entity class
     * @param entities list of entities
     * @return the first of the list, or null if the list is empty
     */
    public static <T extends EntityInterface> T getFirst(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return null;
        }
        return entities.get(0);
    }

    /* findOne */

    /** {@inheritDoc} */
    public <T extends EntityInterface> T findOne(Class<T> entityName, Map<? extends EntityFieldInterface<? super T>, Object> pk) throws RepositoryException {
        return findOne(entityName, getEntityBaseName(entityName), pk);
    }

    /**
     * Find one entity by primary key.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param pk a map describing the primary key
     * @return the corresponding entity, or null if it is not found
     * @throws RepositoryException if an error occurs
     */
    private <T extends EntityInterface> T findOne(Class<T> entityName, String genericValueName, Map<? extends EntityFieldInterface<? super T>, Object> pk) throws RepositoryException {
        try {
            GenericValue gv = getDelegator().findByPrimaryKey(genericValueName, toSimpleMap(pk));
            return Repository.loadFromGeneric(entityName, gv, this);
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    /* findOneCache */

    /** {@inheritDoc} */
    public <T extends EntityInterface> T findOneCache(Class<T> entityName, Map<? extends EntityFieldInterface<? super T>, Object> pk) throws RepositoryException {
        return findOneCache(entityName, getEntityBaseName(entityName), pk);
    }

    /**
     * Find one entity by primary key using the cache.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param pk a map describing the primary key
     * @return the corresponding entity, or null if it is not found
     * @throws RepositoryException if an error occurs
     */
    private <T extends EntityInterface> T findOneCache(Class<T> entityName, String genericValueName, Map<? extends EntityFieldInterface<? super T>, Object> pk) throws RepositoryException {
        try {
            GenericValue gv = getDelegator().findByPrimaryKeyCache(genericValueName, toSimpleMap(pk));
            return Repository.loadFromGeneric(entityName, gv, this);
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    /* findOneNotNull, same as findOne but throws EntityNotFoundException when the entity is not found */


    /** {@inheritDoc} */
    public <T extends EntityInterface> T findOneNotNull(Class<T> entityName, Map<? extends EntityFieldInterface<? super T>, Object> pk) throws RepositoryException, EntityNotFoundException {
        return findOneNotNull(entityName, getEntityBaseName(entityName), pk);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> T findOneNotNull(Class<T> entityName, Map<? extends EntityFieldInterface<? super T>, Object> pk, String message) throws RepositoryException, EntityNotFoundException {
        return findOneNotNull(entityName, getEntityBaseName(entityName), pk, message);
    }


    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public <T extends EntityInterface> T findOneNotNull(Class<T> entityName, Map<? extends EntityFieldInterface<? super T>, Object> pk, String messageLabel, Map context) throws RepositoryException, EntityNotFoundException {
        return findOneNotNull(entityName, getEntityBaseName(entityName), pk, messageLabel, context);
    }

    /**
     * Find one entity by primary key.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param pk a map describing the primary key
     * @return the corresponding entity
     * @throws RepositoryException if an error occurs
     * @throws EntityNotFoundException if an error occurs
     */
    private <T extends EntityInterface> T findOneNotNull(Class<T> entityName, String genericValueName, Map<? extends EntityFieldInterface<? super T>, Object> pk) throws RepositoryException, EntityNotFoundException {
        return findOneNotNull(entityName, genericValueName, pk, null);
    }

    /**
     * Find one entity by primary key.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param pk a map describing the primary key
     * @param message the exception message to use to build the EntityNotFoundException
     * @return the corresponding entity
     * @throws RepositoryException if an error occurs
     * @throws EntityNotFoundException if an error occurs
     */
    private <T extends EntityInterface> T findOneNotNull(Class<T> entityName, String genericValueName, Map<? extends EntityFieldInterface<? super T>, Object> pk, String message) throws RepositoryException, EntityNotFoundException {
        try {
            GenericValue gv = getDelegator().findByPrimaryKey(genericValueName, toSimpleMap(pk));
            if (gv == null) {
                throw new EntityNotFoundException(entityName, pk, message);
            }
            return Repository.loadFromGeneric(entityName, gv, this);
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Find one entity by primary key.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param pk a map describing the primary key
     * @param messageLabel the message label to use to build the EntityNotFoundException
     * @param context the context map used to expand the messageLabel
     * @return the corresponding entity
     * @throws RepositoryException if an error occurs
     * @throws EntityNotFoundException if an error occurs
     */
    @SuppressWarnings("unchecked")
    private <T extends EntityInterface> T findOneNotNull(Class<T> entityName, String genericValueName, Map<? extends EntityFieldInterface<? super T>, Object> pk, String messageLabel, Map context) throws RepositoryException, EntityNotFoundException {
        try {
            GenericValue gv = getDelegator().findByPrimaryKey(genericValueName, toSimpleMap(pk));
            if (gv == null) {
                throw new EntityNotFoundException(entityName, pk, messageLabel, context);
            }
            return Repository.loadFromGeneric(entityName, gv, this);
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    /* findOneNotNullCache, same as findOneCache but throws EntityNotFoundException when the entity is not found */

    /** {@inheritDoc} */
    public <T extends EntityInterface> T findOneNotNullCache(Class<T> entityName, Map<? extends EntityFieldInterface<? super T>, Object> pk) throws RepositoryException, EntityNotFoundException {
        return findOneNotNullCache(entityName, getEntityBaseName(entityName), pk);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> T findOneNotNullCache(Class<T> entityName, Map<? extends EntityFieldInterface<? super T>, Object> pk, String message) throws RepositoryException, EntityNotFoundException {
        return findOneNotNullCache(entityName, getEntityBaseName(entityName), pk, message);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> T findOneNotNullCache(Class<T> entityName, Map<? extends EntityFieldInterface<? super T>, Object> pk, String messageLabel, Map<String, Object> context) throws RepositoryException, EntityNotFoundException {
        return findOneNotNullCache(entityName, getEntityBaseName(entityName), pk, messageLabel, context);
    }

    /**
     * Find one entity by primary key using the cache.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param pk a map describing the primary key
     * @return the corresponding entity
     * @throws RepositoryException if an error occurs
     * @throws EntityNotFoundException if an error occurs
     */
    private <T extends EntityInterface> T findOneNotNullCache(Class<T> entityName, String genericValueName, Map<? extends EntityFieldInterface<? super T>, Object> pk) throws RepositoryException, EntityNotFoundException {
        return findOneNotNullCache(entityName, genericValueName, pk, null);
    }

    /**
     * Find one entity by primary key using the cache.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param pk a map describing the primary key
     * @param message the exception message to use to build the EntityNotFoundException
     * @return the corresponding entity
     * @throws RepositoryException if an error occurs
     * @throws EntityNotFoundException if an error occurs
     */
    private <T extends EntityInterface> T findOneNotNullCache(Class<T> entityName, String genericValueName, Map<? extends EntityFieldInterface<? super T>, Object> pk, String message) throws RepositoryException, EntityNotFoundException {
        try {
            GenericValue gv = getDelegator().findByPrimaryKeyCache(genericValueName, toSimpleMap(pk));
            if (gv == null) {
                throw new EntityNotFoundException(entityName, pk, message);
            }
            return Repository.loadFromGeneric(entityName, gv, this);
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Find one entity by primary key using the cache.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param pk a map describing the primary key
     * @param messageLabel the message label to use to build the EntityNotFoundException
     * @param context the context map used to expand the messageLabel
     * @return the corresponding entity
     * @throws RepositoryException if an error occurs
     * @throws EntityNotFoundException if an error occurs
     */
    private <T extends EntityInterface> T findOneNotNullCache(Class<T> entityName, String genericValueName, Map<? extends EntityFieldInterface<? super T>, Object> pk, String messageLabel, Map<String, Object> context) throws RepositoryException, EntityNotFoundException {
        try {
            GenericValue gv = getDelegator().findByPrimaryKeyCache(genericValueName, toSimpleMap(pk));
            if (gv == null) {
                throw new EntityNotFoundException(entityName, pk, messageLabel, context);
            }
            return Repository.loadFromGeneric(entityName, gv, this);
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    /* find all */

    /** {@inheritDoc} */
    public <T extends EntityInterface> List<T> findAll(Class<T> entityName) throws RepositoryException {
        return findAll(entityName, getEntityBaseName(entityName), null);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> List<T> findAll(Class<T> entityName, List<String> orderBy) throws RepositoryException {
        return findAll(entityName, getEntityBaseName(entityName), orderBy);
    }

    /**
     * Find all entities.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param orderBy list of fields to order by
     * @return the list of entities found
     * @throws RepositoryException if an error occurs
     */
    private <T extends EntityInterface> List<T> findAll(Class<T> entityName, String genericValueName, List<String> orderBy) throws RepositoryException {
        try {
            List<GenericValue> gv = getDelegator().findAll(genericValueName, orderBy);
            return Repository.loadFromGeneric(entityName, gv, this);
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    /* find all cache */

    /** {@inheritDoc} */
    public <T extends EntityInterface> List<T> findAllCache(Class<T> entityName) throws RepositoryException {
        return findAllCache(entityName, getEntityBaseName(entityName), null);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> List<T> findAllCache(Class<T> entityName, List<String> orderBy) throws RepositoryException {
        return findAllCache(entityName, getEntityBaseName(entityName), orderBy);
    }

    /**
     * Find all entities using the cache.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param orderBy list of fields to order by
     * @return the list of entities found
     * @throws RepositoryException if an error occurs
     */
    private <T extends EntityInterface> List<T> findAllCache(Class<T> entityName, String genericValueName, List<String> orderBy) throws RepositoryException {
        try {
            List<GenericValue> gv = getDelegator().findAllCache(genericValueName, orderBy);
            return Repository.loadFromGeneric(entityName, gv, this);
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    /* findIterator */

    // by Map

    /** {@inheritDoc} */
    public <T extends EntityInterface> EntityListIterator<T> findIterator(Class<T> entityName, Map<? extends EntityFieldInterface<? super T>, Object> conditions) throws RepositoryException {
        return findIterator(entityName, conditions, null);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> EntityListIterator<T> findIterator(Class<T> entityName, Map<? extends EntityFieldInterface<? super T>, Object> conditions, List<String> orderBy) throws RepositoryException {
        return findIterator(entityName, getEntityBaseName(entityName), conditions, orderBy);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> EntityListIterator<T> findIterator(Class<T> entityName, Map<? extends EntityFieldInterface<? super T>, Object> conditions, List<String> fields, List<String> orderBy) throws RepositoryException {
        return findIterator(entityName, getEntityBaseName(entityName), conditions, fields, orderBy);
    }

    /**
     * Find entities by conditions.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param conditions a Map of fields -> value that the entities must all match
     * @return an <code>EntityListIterator</code> instance
     * @throws RepositoryException if an error occurs
     */
    @SuppressWarnings("unused")
    private <T extends EntityInterface> EntityListIterator<T> findIterator(Class<T> entityName, String genericValueName, Map<? extends EntityFieldInterface<? super T>, Object> conditions) throws RepositoryException {
        return findIterator(entityName, genericValueName, conditions, null);
    }

    /**
     * Find entities by conditions.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param conditions a Map of fields -> value that the entities must all match
     * @param orderBy list of fields to order by
     * @return an <code>EntityListIterator</code> instance
     * @throws RepositoryException if an error occurs
     */
    private <T extends EntityInterface> EntityListIterator<T> findIterator(Class<T> entityName, String genericValueName, Map<? extends EntityFieldInterface<? super T>, Object> conditions, List<String> orderBy) throws RepositoryException {
        try {
            return new EntityListIterator<T>(entityName, getDelegator().find(genericValueName, EntityCondition.makeCondition(toSimpleMap(conditions)), null, null, orderBy, PAGE_FIND_OPTIONS), this);
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Find entities by conditions. Only return a subset of the entity fields and filters out duplicates.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param conditions a Map of fields -> value that the entities must all match
     * @param fields list of fields to select
     * @param orderBy list of fields to order by
     * @return an <code>EntityListIterator</code> instance
     * @throws RepositoryException if an error occurs
     */
    private <T extends EntityInterface> EntityListIterator<T> findIterator(Class<T> entityName, String genericValueName, Map<? extends EntityFieldInterface<? super T>, Object> conditions, List<String> fields, List<String> orderBy) throws RepositoryException {
        try {
            return new EntityListIterator<T>(entityName, getDelegator().find(genericValueName, EntityCondition.makeCondition(toSimpleMap(conditions)), null, new HashSet<String>(fields), orderBy, DISTINCT_FIND_OPTIONS), this);
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    /* findPage */

    // by Map

    /** {@inheritDoc} */
    public <T extends EntityInterface> List<T> findPage(Class<T> entityName, Map<? extends EntityFieldInterface<? super T>, Object> conditions, int pageStart, int pageSize) throws RepositoryException {
        return findPage(entityName, conditions, null, pageStart, pageSize);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> List<T> findPage(Class<T> entityName, Map<? extends EntityFieldInterface<? super T>, Object> conditions, List<String> orderBy, int pageStart, int pageSize) throws RepositoryException {
        return findPage(entityName, getEntityBaseName(entityName), conditions, orderBy, pageStart, pageSize);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> List<T> findPage(Class<T> entityName, Map<? extends EntityFieldInterface<? super T>, Object> conditions, List<String> fields, List<String> orderBy, int pageStart, int pageSize) throws RepositoryException {
        return findPage(entityName, getEntityBaseName(entityName), conditions, fields, orderBy, pageStart, pageSize);
    }

    /**
     * Find entities by conditions.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param conditions a Map of fields -> value that the entities must all match
     * @param pageStart the index of the first entity, starting at 0
     * @param pageSize the number of entities to return at most
     * @return the partial list of entities found
     * @throws RepositoryException if an error occurs
     */
    @SuppressWarnings("unused")
    private <T extends EntityInterface> List<T> findPage(Class<T> entityName, String genericValueName, Map<? extends EntityFieldInterface<? super T>, Object> conditions, int pageStart, int pageSize) throws RepositoryException {
        return findPage(entityName, genericValueName, conditions, null, pageStart, pageSize);
    }

    /**
     * Find entities by conditions.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param conditions a Map of fields -> value that the entities must all match
     * @param orderBy list of fields to order by
     * @param pageStart the index of the first entity, starting at 0
     * @param pageSize the number of entities to return at most
     * @return the partial list of entities found
     * @throws RepositoryException if an error occurs
     */
    private <T extends EntityInterface> List<T> findPage(Class<T> entityName, String genericValueName, Map<? extends EntityFieldInterface<? super T>, Object> conditions, List<String> orderBy, int pageStart, int pageSize) throws RepositoryException {
        try {
            boolean t = TransactionUtil.begin();
            EntityListIterator<T> it = findIterator(entityName, genericValueName, conditions, orderBy);
            List<T> values = null;
            try {
                values = it.getPartialList(pageStart + 1, pageSize);
            } finally {
                it.close();
            }
            TransactionUtil.commit(t);
            return values;
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        } catch (FoundationException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Find entities by conditions. Only return a subset of the entity fields and filters out duplicates.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param conditions a Map of fields -> value that the entities must all match
     * @param fields list of fields to select
     * @param orderBy list of fields to order by
     * @param pageStart the index of the first entity, starting at 0
     * @param pageSize the number of entities to return at most
     * @return the partial list of entities found
     * @throws RepositoryException if an error occurs
     */
    private <T extends EntityInterface> List<T> findPage(Class<T> entityName, String genericValueName, Map<? extends EntityFieldInterface<? super T>, Object> conditions, List<String> fields, List<String> orderBy, int pageStart, int pageSize) throws RepositoryException {
        try {
            boolean t = TransactionUtil.begin();
            EntityListIterator<T> it = findIterator(entityName, genericValueName, conditions, fields, orderBy);
            List<T> values = null;
            try {
                values = it.getPartialList(pageStart + 1, pageSize);
            } finally {
                it.close();
            }
            TransactionUtil.commit(t);
            return values;
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        } catch (FoundationException e) {
            throw new RepositoryException(e);
        }
    }

    /* findList */

    // by Map

    /** {@inheritDoc} */
    public <T extends EntityInterface> List<T> findList(Class<T> entityName, Map<? extends EntityFieldInterface<? super T>, Object> conditions) throws RepositoryException {
        return findList(entityName, conditions, null);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> List<T> findList(Class<T> entityName, Map<? extends EntityFieldInterface<? super T>, Object> conditions, List<String> orderBy) throws RepositoryException {
        return findList(entityName, getEntityBaseName(entityName), conditions, orderBy);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> List<T> findList(Class<T> entityName, Map<? extends EntityFieldInterface<? super T>, Object> conditions, List<String> fields, List<String> orderBy) throws RepositoryException {
        return findList(entityName, getEntityBaseName(entityName), conditions, fields, orderBy);
    }

    /**
     * Find entities by conditions.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param conditions a Map of fields -> value that the entities must all match
     * @return the list of entities found
     * @throws RepositoryException if an error occurs
     */
    @SuppressWarnings("unused")
    private <T extends EntityInterface> List<T> findList(Class<T> entityName, String genericValueName, Map<? extends EntityFieldInterface<? super T>, Object> conditions) throws RepositoryException {
        return findList(entityName, genericValueName, conditions, null);
    }

    /**
     * Find entities by conditions.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param conditions a Map of fields -> value that the entities must all match
     * @param orderBy list of fields to order by
     * @return the list of entities found
     * @throws RepositoryException if an error occurs
     */
    private <T extends EntityInterface> List<T> findList(Class<T> entityName, String genericValueName, Map<? extends EntityFieldInterface<? super T>, Object> conditions, List<String> orderBy) throws RepositoryException {
        try {
            List<GenericValue> gv = getDelegator().findByAnd(genericValueName, toSimpleMap(conditions), orderBy);
            return Repository.loadFromGeneric(entityName, gv, this);
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Find entities by conditions. Only return a subset of the entity fields and filters out duplicates.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param conditions a Map of fields -> value that the entities must all match
     * @param fields list of fields to select
     * @param orderBy list of fields to order by
     * @return the list of entities found
     * @throws RepositoryException if an error occurs
     */
    private <T extends EntityInterface> List<T> findList(Class<T> entityName, String genericValueName, Map<? extends EntityFieldInterface<? super T>, Object> conditions, List<String> fields, List<String> orderBy) throws RepositoryException {
        try {
            List<GenericValue> gv = getDelegator().findByCondition(genericValueName, EntityCondition.makeCondition(toSimpleMap(conditions)), null, fields, orderBy, DISTINCT_FIND_OPTIONS);
            return Repository.loadFromGeneric(entityName, gv, this);
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    // by Map Cache

    /** {@inheritDoc} */
    public <T extends EntityInterface> List<T> findListCache(Class<T> entityName, Map<? extends EntityFieldInterface<? super T>, Object> conditions) throws RepositoryException {
        return findListCache(entityName, conditions, null);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> List<T> findListCache(Class<T> entityName, Map<? extends EntityFieldInterface<? super T>, Object> conditions, List<String> orderBy) throws RepositoryException {
        return findListCache(entityName, getEntityBaseName(entityName), conditions, orderBy);
    }

    /**
     * Find entities by conditions using the cache.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param conditions a Map of fields -> value that the entities must all match
     * @return the list of entities found
     * @throws RepositoryException if an error occurs
     */
    @SuppressWarnings("unused")
    private <T extends EntityInterface> List<T> findListCache(Class<T> entityName, String genericValueName, Map<? extends EntityFieldInterface<? super T>, Object> conditions) throws RepositoryException {
        return findListCache(entityName, genericValueName, conditions, null);
    }

    /**
     * Find entities by conditions using the cache.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param conditions a Map of fields -> value that the entities must all match
     * @param orderBy list of fields to order by
     * @return the list of entities found
     * @throws RepositoryException if an error occurs
     */
    private <T extends EntityInterface> List<T> findListCache(Class<T> entityName, String genericValueName, Map<? extends EntityFieldInterface<? super T>, Object> conditions, List<String> orderBy) throws RepositoryException {
        try {
            List<GenericValue> gv = getDelegator().findByAndCache(genericValueName, toSimpleMap(conditions), orderBy);
            return Repository.loadFromGeneric(entityName, gv, this);
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    // findIterator by List<EntityExpr>

    /** {@inheritDoc} */
    public <T extends EntityInterface> EntityListIterator<T> findIterator(Class<T> entityName, List<? extends EntityCondition> conditions) throws RepositoryException {
        return findIterator(entityName, conditions, null);
    }

    /**
     * Find entities by conditions.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param conditions a List of EntityExpr the entities must all match
     * @return an <code>EntityListIterator</code> instance
     * @throws RepositoryException if an error occurs
     */
    @SuppressWarnings("unused")
    private <T extends EntityInterface> EntityListIterator<T> findIterator(Class<T> entityName, String genericValueName, List<? extends EntityCondition> conditions) throws RepositoryException {
        return findIterator(entityName, genericValueName, conditions, null);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> EntityListIterator<T> findIterator(Class<T> entityName, List<? extends EntityCondition> conditions, List<String> orderBy) throws RepositoryException {
        return findIterator(entityName, getEntityBaseName(entityName), conditions, orderBy);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> EntityListIterator<T> findIterator(Class<T> entityName, List<? extends EntityCondition> conditions, List<String> fields, List<String> orderBy) throws RepositoryException {
        return findIterator(entityName, getEntityBaseName(entityName), conditions, fields, orderBy);
    }

    /**
     * Find entities by conditions.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param conditions a List of EntityExpr the entities must all match
     * @param orderBy list of fields to order by
     * @return an <code>EntityListIterator</code> instance
     * @throws RepositoryException if an error occurs
     */
    private <T extends EntityInterface> EntityListIterator<T> findIterator(Class<T> entityName, String genericValueName, List<? extends EntityCondition> conditions, List<String> orderBy) throws RepositoryException {
        try {
            return new EntityListIterator<T>(entityName, getDelegator().find(genericValueName, EntityCondition.makeCondition(conditions), null, null, orderBy, PAGE_FIND_OPTIONS), this);
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Find entities by conditions. Only return a subset of the entity fields and filters out duplicates.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param conditions a List of EntityExpr the entities must all match
     * @param fields the list of field to select
     * @param orderBy list of fields to order by
     * @return an <code>EntityListIterator</code> instance
     * @throws RepositoryException if an error occurs
     */
    private <T extends EntityInterface> EntityListIterator<T> findIterator(Class<T> entityName, String genericValueName, List<? extends EntityCondition> conditions, List<String> fields, List<String> orderBy) throws RepositoryException {
        try {
            return new EntityListIterator<T>(entityName, getDelegator().find(genericValueName, EntityCondition.makeCondition(conditions), null, new HashSet<String>(fields), orderBy, DISTINCT_FIND_OPTIONS), this);
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    // findPage by List<EntityExpr>

    /** {@inheritDoc} */
    public <T extends EntityInterface> List<T> findPage(Class<T> entityName, List<? extends EntityCondition> conditions, int pageStart, int pageSize) throws RepositoryException {
        return findPage(entityName, conditions, null, pageStart, pageSize);
    }

    /**
     * Find entities by conditions.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param conditions a List of EntityExpr the entities must all match
     * @param pageStart the index of the first entity, starting at 0
     * @param pageSize the number of entities to return at most
     * @return the partial list of entities found
     * @throws RepositoryException if an error occurs
     */
    @SuppressWarnings("unused")
    private <T extends EntityInterface> List<T> findPage(Class<T> entityName, String genericValueName, List<? extends EntityCondition> conditions, int pageStart, int pageSize) throws RepositoryException {
        return findPage(entityName, genericValueName, conditions, null, pageStart, pageSize);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> List<T> findPage(Class<T> entityName, List<? extends EntityCondition> conditions, List<String> orderBy, int pageStart, int pageSize) throws RepositoryException {
        return findPage(entityName, getEntityBaseName(entityName), conditions, orderBy, pageStart, pageSize);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> List<T> findPage(Class<T> entityName, List<? extends EntityCondition> conditions, List<String> fields, List<String> orderBy, int pageStart, int pageSize) throws RepositoryException {
        return findPage(entityName, getEntityBaseName(entityName), conditions, fields, orderBy, pageStart, pageSize);
    }

    /**
     * Find entities by conditions.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param conditions a List of EntityExpr the entities must all match
     * @param orderBy list of fields to order by
     * @param pageStart the index of the first entity, starting at 0
     * @param pageSize the number of entities to return at most
     * @return the partial list of entities found
     * @throws RepositoryException if an error occurs
     */
    private <T extends EntityInterface> List<T> findPage(Class<T> entityName, String genericValueName, List<? extends EntityCondition> conditions, List<String> orderBy, int pageStart, int pageSize) throws RepositoryException {
        try {
            boolean t = TransactionUtil.begin();
            EntityListIterator<T> it = findIterator(entityName, genericValueName, conditions, orderBy);
            List<T> values = null;
            try {
                values = it.getPartialList(pageStart + 1, pageSize);
            } finally {
                it.close();
            }
            TransactionUtil.commit(t);
            return values;
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        } catch (FoundationException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Find entities by conditions. Only return a subset of the entity fields and filters out duplicates.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param conditions a List of EntityExpr the entities must all match
     * @param fields the list of field to select
     * @param orderBy list of fields to order by
     * @param pageStart the index of the first entity, starting at 0
     * @param pageSize the number of entities to return at most
     * @return the partial list of entities found
     * @throws RepositoryException if an error occurs
     */
    private <T extends EntityInterface> List<T> findPage(Class<T> entityName, String genericValueName, List<? extends EntityCondition> conditions, List<String> fields, List<String> orderBy, int pageStart, int pageSize) throws RepositoryException {
        try {
            boolean t = TransactionUtil.begin();
            EntityListIterator<T> it = findIterator(entityName, genericValueName, conditions, fields, orderBy);
            List<T> values = null;
            try {
                values = it.getPartialList(pageStart + 1, pageSize);
            } finally {
                it.close();
            }
            TransactionUtil.commit(t);
            return values;
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        } catch (FoundationException e) {
            throw new RepositoryException(e);
        }
    }

    // by List<EntityExpr>

    /** {@inheritDoc} */
    public <T extends EntityInterface> List<T> findList(Class<T> entityName, List<? extends EntityCondition> conditions) throws RepositoryException {
        return findList(entityName, conditions, null);
    }

    /**
     * Find entities by conditions.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param conditions a List of EntityExpr the entities must all match
     * @return the list of entities found
     * @throws RepositoryException if an error occurs
     */
    @SuppressWarnings("unused")
    private <T extends EntityInterface> List<T> findList(Class<T> entityName, String genericValueName, List<? extends EntityCondition> conditions) throws RepositoryException {
        return findList(entityName, genericValueName, conditions, null);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> List<T> findList(Class<T> entityName, List<? extends EntityCondition> conditions, List<String> orderBy) throws RepositoryException {
        return findList(entityName, getEntityBaseName(entityName), conditions, orderBy);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> List<T> findList(Class<T> entityName, List<? extends EntityCondition> conditions, List<String> fields, List<String> orderBy) throws RepositoryException {
        return findList(entityName, getEntityBaseName(entityName), conditions, fields, orderBy);
    }

    /**
     * Find entities by conditions.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param conditions a List of EntityExpr the entities must all match
     * @param orderBy list of fields to order by
     * @return the list of entities found
     * @throws RepositoryException if an error occurs
     */
    private <T extends EntityInterface> List<T> findList(Class<T> entityName, String genericValueName, List<? extends EntityCondition> conditions, List<String> orderBy) throws RepositoryException {
        try {
            List<GenericValue> gv = getDelegator().findByAnd(genericValueName, conditions, orderBy);
            return Repository.loadFromGeneric(entityName, gv, this);
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Find entities by conditions. Only return a subset of the entity fields and filters out duplicates.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param conditions a List of EntityExpr the entities must all match
     * @param fields the list of field to select
     * @param orderBy list of fields to order by
     * @return the list of entities found
     * @throws RepositoryException if an error occurs
     */
    private <T extends EntityInterface> List<T> findList(Class<T> entityName, String genericValueName, List<? extends EntityCondition> conditions, List<String> fields, List<String> orderBy) throws RepositoryException {
        try {
            EntityConditionList<? extends EntityCondition> ecl = EntityCondition.makeCondition(conditions, EntityOperator.AND);
            List<GenericValue> gv = getDelegator().findByCondition(genericValueName, ecl, null, fields, orderBy, DISTINCT_FIND_OPTIONS);
            return Repository.loadFromGeneric(entityName, gv, this);
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    // by List<EntityExpr> cache

    /** {@inheritDoc} */
    public <T extends EntityInterface> List<T> findListCache(Class<T> entityName, List<? extends EntityCondition> conditions) throws RepositoryException {
        return findListCache(entityName, conditions, null);
    }

    /**
     * Find entities by conditions using the cache.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param conditions a List of EntityExpr the entities must all match
     * @return the list of entities found
     * @throws RepositoryException if an error occurs
     */
    @SuppressWarnings("unused")
    private <T extends EntityInterface> List<T> findListCache(Class<T> entityName, String genericValueName, List<? extends EntityCondition> conditions) throws RepositoryException {
        return findListCache(entityName, genericValueName, conditions, null);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> List<T> findListCache(Class<T> entityName, List<? extends EntityCondition> conditions, List<String> orderBy) throws RepositoryException {
        return findListCache(entityName, getEntityBaseName(entityName), conditions, orderBy);
    }

    /**
     * Find entities by conditions using the cache.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param conditions a List of EntityExpr the entities must all match
     * @param orderBy list of fields to order by
     * @return the list of entities found
     * @throws RepositoryException if an error occurs
     */
    private <T extends EntityInterface> List<T> findListCache(Class<T> entityName, String genericValueName, List<? extends EntityCondition> conditions, List<String> orderBy) throws RepositoryException {
        try {
            EntityConditionList<? extends EntityCondition> ecl = EntityCondition.makeCondition(conditions, EntityOperator.AND);
            List<GenericValue> gv = getDelegator().findByConditionCache(genericValueName, ecl, null, orderBy);
            return Repository.loadFromGeneric(entityName, gv, this);
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    // findIterator by EntityCondition

    /** {@inheritDoc} */
    public <T extends EntityInterface> EntityListIterator<T> findIterator(Class<T> entityName, EntityCondition condition) throws RepositoryException {
        return findIterator(entityName, condition, null);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> EntityListIterator<T> findIterator(Class<T> entityName, EntityCondition condition, List<String> orderBy) throws RepositoryException {
        return findIterator(entityName, getEntityBaseName(entityName), condition, orderBy);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> EntityListIterator<T> findIterator(Class<T> entityName, EntityCondition condition, List<String> fields, List<String> orderBy) throws RepositoryException {
        return findIterator(entityName, getEntityBaseName(entityName), condition, fields, orderBy);
    }

    /**
     * Find entities by conditions.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param condition the EntityCondition used to find the entities
     * @return the partial list of entities found
     * @throws RepositoryException if an error occurs
     */
    @SuppressWarnings("unused")
    private <T extends EntityInterface> EntityListIterator<T> findIterator(Class<T> entityName, String genericValueName, EntityCondition condition) throws RepositoryException {
        return findIterator(entityName, genericValueName, condition, null);
    }

    /**
     * Find entities by conditions.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param condition the EntityCondition used to find the entities
     * @param orderBy list of fields to order by
     * @return the partial list of entities found
     * @throws RepositoryException if an error occurs
     */
    private <T extends EntityInterface> EntityListIterator<T> findIterator(Class<T> entityName, String genericValueName, EntityCondition condition, List<String> orderBy) throws RepositoryException {
        try {
            return new EntityListIterator<T>(entityName, getDelegator().find(genericValueName, condition, null, null, orderBy, PAGE_FIND_OPTIONS), this);
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Find entities by conditions. Only return a subset of the entity fields and filters out duplicates.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param condition the EntityCondition used to find the entities
     * @param fields the list of field to select
     * @param orderBy list of fields to order by
     * @return the partial list of entities found
     * @throws RepositoryException if an error occurs
     */
    private <T extends EntityInterface> EntityListIterator<T> findIterator(Class<T> entityName, String genericValueName, EntityCondition condition, List<String> fields, List<String> orderBy) throws RepositoryException {
        try {
            return new EntityListIterator<T>(entityName, getDelegator().find(genericValueName, condition, null, new HashSet<String>(fields), orderBy, DISTINCT_FIND_OPTIONS), this);
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    // findPage by EntityCondition

    /** {@inheritDoc} */
    public <T extends EntityInterface> List<T> findPage(Class<T> entityName, EntityCondition condition, int pageStart, int pageSize) throws RepositoryException {
        return findPage(entityName, condition, null, pageStart, pageSize);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> List<T> findPage(Class<T> entityName, EntityCondition condition, List<String> orderBy, int pageStart, int pageSize) throws RepositoryException {
        return findPage(entityName, getEntityBaseName(entityName), condition, orderBy, pageStart, pageSize);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> List<T> findPage(Class<T> entityName, EntityCondition condition, List<String> fields, List<String> orderBy, int pageStart, int pageSize) throws RepositoryException {
        return findPage(entityName, getEntityBaseName(entityName), condition, fields, orderBy, pageStart, pageSize);
    }

    /**
     * Find entities by conditions.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param condition the EntityCondition used to find the entities
     * @param pageStart the index of the first entity, starting at 0
     * @param pageSize the number of entities to return at most
     * @return the partial list of entities found
     * @throws RepositoryException if an error occurs
     */
    @SuppressWarnings("unused")
    private <T extends EntityInterface> List<T> findPage(Class<T> entityName, String genericValueName, EntityCondition condition, int pageStart, int pageSize) throws RepositoryException {
        return findPage(entityName, genericValueName, condition, null, pageStart, pageSize);
    }

    /**
     * Find entities by conditions.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param condition the EntityCondition used to find the entities
     * @param orderBy list of fields to order by
     * @param pageStart the index of the first entity, starting at 0
     * @param pageSize the number of entities to return at most
     * @return the partial list of entities found
     * @throws RepositoryException if an error occurs
     */
    private <T extends EntityInterface> List<T> findPage(Class<T> entityName, String genericValueName, EntityCondition condition, List<String> orderBy, int pageStart, int pageSize) throws RepositoryException {
        try {
            boolean t = TransactionUtil.begin();
            EntityListIterator<T> it = findIterator(entityName, genericValueName, condition, orderBy);
            List<T> values = null;
            try {
                values = it.getPartialList(pageStart + 1, pageSize);
            } finally {
                it.close();
            }
            TransactionUtil.commit(t);
            return values;
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        } catch (FoundationException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Find entities by conditions. Only return a subset of the entity fields and filters out duplicates.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param condition the EntityCondition used to find the entities
     * @param fields the list of field to select
     * @param orderBy list of fields to order by
     * @param pageStart the index of the first entity, starting at 0
     * @param pageSize the number of entities to return at most
     * @return the partial list of entities found
     * @throws RepositoryException if an error occurs
     */
    private <T extends EntityInterface> List<T> findPage(Class<T> entityName, String genericValueName, EntityCondition condition, List<String> fields, List<String> orderBy, int pageStart, int pageSize) throws RepositoryException {
        try {
            boolean t = TransactionUtil.begin();
            EntityListIterator<T> it = findIterator(entityName, genericValueName, condition, fields, orderBy);
            List<T> values = null;
            try {
                values = it.getPartialList(pageStart + 1, pageSize);
            } finally {
                it.close();
            }
            TransactionUtil.commit(t);
            return values;
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        } catch (FoundationException e) {
            throw new RepositoryException(e);
        }
    }

    // by EntityCondition

    /** {@inheritDoc} */
    public <T extends EntityInterface> List<T> findList(Class<T> entityName, EntityCondition condition) throws RepositoryException {
        return findList(entityName, condition, null);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> List<T> findList(Class<T> entityName, EntityCondition condition, List<String> orderBy) throws RepositoryException {
        return findList(entityName, getEntityBaseName(entityName), condition, orderBy);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> List<T> findList(Class<T> entityName, EntityCondition condition, List<String> fields, List<String> orderBy) throws RepositoryException {
        return findList(entityName, getEntityBaseName(entityName), condition, fields, orderBy);
    }

    /**
     * Find entities by conditions.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param condition the EntityCondition used to find the entities
     * @return the list of entities found
     * @throws RepositoryException if an error occurs
     */
    @SuppressWarnings("unused")
    private <T extends EntityInterface> List<T> findList(Class<T> entityName, String genericValueName, EntityCondition condition) throws RepositoryException {
        return findList(entityName, genericValueName, condition, null);
    }

    /**
     * Find entities by conditions.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param condition the EntityCondition used to find the entities
     * @param orderBy list of fields to order by
     * @return the list of entities found
     * @throws RepositoryException if an error occurs
     */
    private <T extends EntityInterface> List<T> findList(Class<T> entityName, String genericValueName, EntityCondition condition, List<String> orderBy) throws RepositoryException {
        try {
            List<GenericValue> gv = getDelegator().findByCondition(genericValueName, condition, null, orderBy);
            return Repository.loadFromGeneric(entityName, gv, this);
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * Find entities by conditions. Only return a subset of the entity fields and filters out duplicates.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param condition the EntityCondition used to find the entities
     * @param fields the list of field to select
     * @param orderBy list of fields to order by
     * @return the list of entities found
     * @throws RepositoryException if an error occurs
     */
    private <T extends EntityInterface> List<T> findList(Class<T> entityName, String genericValueName, EntityCondition condition, List<String> fields, List<String> orderBy) throws RepositoryException {
        try {
            List<GenericValue> gv = getDelegator().findByCondition(genericValueName, condition, null, fields, orderBy, DISTINCT_FIND_OPTIONS);
            return Repository.loadFromGeneric(entityName, gv, this);
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    // by EntityCondition cache

    /** {@inheritDoc} */
    public <T extends EntityInterface> List<T> findListCache(Class<T> entityName, EntityCondition condition) throws RepositoryException {
        return findListCache(entityName, condition, null);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> List<T> findListCache(Class<T> entityName, EntityCondition condition, List<String> orderBy) throws RepositoryException {
        return findListCache(entityName, getEntityBaseName(entityName), condition, orderBy);
    }

    /**
     * Find entities by conditions using the cache.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param condition the EntityCondition used to find the entities
     * @return the list of entities found
     * @throws RepositoryException if an error occurs
     */
    @SuppressWarnings("unused")
    private <T extends EntityInterface> List<T> findListCache(Class<T> entityName, String genericValueName, EntityCondition condition) throws RepositoryException {
        return findListCache(entityName, genericValueName, condition, null);
    }

    /**
     * Find entities by conditions using the cache.
     * @param <T> the entity class
     * @param entityName class to find and return
     * @param genericValueName name of the entity in Ofbiz
     * @param condition the EntityCondition used to find the entities
     * @param orderBy list of fields to order by
     * @return the list of entities found
     * @throws RepositoryException if an error occurs
     */
    private <T extends EntityInterface> List<T> findListCache(Class<T> entityName, String genericValueName, EntityCondition condition, List<String> orderBy) throws RepositoryException {
        try {
            List<GenericValue> gv = getDelegator().findByConditionCache(genericValueName, condition, null, orderBy);
            return Repository.loadFromGeneric(entityName, gv, this);
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    // get related one

    /** {@inheritDoc} */
    public <T extends EntityInterface> EntityInterface getRelatedOne(String relation, T entity) throws RepositoryException {
        try {
            Class<?> relatedEntityClass = Class.forName("org.opentaps.base.entities." + relation);
            return getRelatedOne(relatedEntityClass.asSubclass(EntityInterface.class), relation, entity);
        } catch (ClassNotFoundException e) {
            throw new RepositoryException(e);
        }
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface, T2 extends EntityInterface> T getRelatedOne(Class<T> entityName, T2 entity) throws RepositoryException {
        return getRelatedOne(entityName, getEntityBaseName(entityName), entity);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface, T2 extends EntityInterface> T getRelatedOne(Class<T> entityName, String relation, T2 entity) throws RepositoryException {
        try {
            GenericValue gv = genericValueFromEntity(getDelegator(), entity);
            GenericValue related = getDelegator().getRelatedOne(relation, gv);
            return Repository.loadFromGeneric(entityName, related, this);
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    // get related one cached

    /** {@inheritDoc} */
    public <T extends EntityInterface> EntityInterface getRelatedOneCache(String relation, T entity) throws RepositoryException {
        try {
            Class<?> relatedEntityClass = Class.forName("org.opentaps.base.entities." + relation);
            return getRelatedOneCache(relatedEntityClass.asSubclass(EntityInterface.class), relation, entity);
        } catch (ClassNotFoundException e) {
            throw new RepositoryException(e);
        }
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface, T2 extends EntityInterface> T getRelatedOneCache(Class<T> entityName, T2 entity) throws RepositoryException {
        return getRelatedOneCache(entityName, getEntityBaseName(entityName), entity);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface, T2 extends EntityInterface> T getRelatedOneCache(Class<T> entityName, String relation, T2 entity) throws RepositoryException {
        try {
            GenericValue gv = genericValueFromEntity(getDelegator(), entity);
            GenericValue related = getDelegator().getRelatedOneCache(relation, gv);
            return Repository.loadFromGeneric(entityName, related, this);
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    // get related

    /** {@inheritDoc} */
    public <T extends EntityInterface> List<? extends EntityInterface> getRelated(String relation, T entity) throws RepositoryException {
        try {
            Class<?> relatedEntityClass = Class.forName("org.opentaps.base.entities." + relation);
            return getRelated(relatedEntityClass.asSubclass(EntityInterface.class), relation, entity);
        } catch (ClassNotFoundException e) {
            throw new RepositoryException(e);
        }
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> List<? extends EntityInterface> getRelated(String relation, T entity, List<String> orderBy) throws RepositoryException {
        try {
            Class<?> relatedEntityClass = Class.forName("org.opentaps.base.entities." + relation);
            return getRelated(relatedEntityClass.asSubclass(EntityInterface.class), relation, entity, orderBy);
        } catch (ClassNotFoundException e) {
            throw new RepositoryException(e);
        }
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface, T2 extends EntityInterface> List<T> getRelated(Class<T> entityName, T2 entity) throws RepositoryException {
        return getRelated(entityName, getEntityBaseName(entityName), entity);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface, T2 extends EntityInterface> List<T> getRelated(Class<T> entityName, T2 entity, List<String> orderBy) throws RepositoryException {
        return getRelated(entityName, getEntityBaseName(entityName), entity, orderBy);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface, T2 extends EntityInterface> List<T> getRelated(Class<T> entityName, String relation, T2 entity) throws RepositoryException {
        try {
            GenericValue gv = genericValueFromEntity(getDelegator(), entity);
            List<GenericValue> related = getDelegator().getRelated(relation, gv);
            return Repository.loadFromGeneric(entityName, related, this);
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface, T2 extends EntityInterface> List<T> getRelated(Class<T> entityName, String relation, T2 entity, List<String> orderBy) throws RepositoryException {
        try {
            GenericValue gv = genericValueFromEntity(getDelegator(), entity);
            List<GenericValue> related = getDelegator().getRelatedOrderBy(relation, orderBy, gv);
            return Repository.loadFromGeneric(entityName, related, this);
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    // get related cached

    /** {@inheritDoc} */
    public <T extends EntityInterface> List<? extends EntityInterface> getRelatedCache(String relation, T entity) throws RepositoryException {
        try {
            Class<?> relatedEntityClass = Class.forName("org.opentaps.base.entities." + relation);
            return getRelatedCache(relatedEntityClass.asSubclass(EntityInterface.class), relation, entity);
        } catch (ClassNotFoundException e) {
            throw new RepositoryException(e);
        }
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface, T2 extends EntityInterface> List<T> getRelatedCache(Class<T> entityName, T2 entity) throws RepositoryException {
        return getRelatedCache(entityName, getEntityBaseName(entityName), entity);
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface, T2 extends EntityInterface> List<T> getRelatedCache(Class<T> entityName, String relation, T2 entity) throws RepositoryException {
        try {
            GenericValue gv = genericValueFromEntity(getDelegator(), entity);
            List<GenericValue> related = getDelegator().getRelatedCache(relation, gv);
            return Repository.loadFromGeneric(entityName, related, this);
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
    }

    // map methods, used to construct a map of field -> condition

    private <T extends EntityInterface> Map<String, Object> toSimpleMap(Map<? extends EntityFieldInterface<? super T>, Object> map) {
        Map<String, Object> simpleMap = new HashMap<String, Object>();
        for (EntityFieldInterface<? super T> key : map.keySet()) {
            simpleMap.put(key.getName(), map.get(key));
        }
        return simpleMap;
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> Map<? extends EntityFieldInterface<? super T>, Object> map(EntityFieldInterface<? super T> key1, Object value1) {
        Map<EntityFieldInterface<? super T>, Object> map = new HashMap<EntityFieldInterface<? super T>, Object>();
        map.put(key1, value1);
        return map;
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> Map<? extends EntityFieldInterface<? super T>, Object> map(EntityFieldInterface<? super T> key1, Object value1, EntityFieldInterface<? super T> key2, Object value2) {
        Map<EntityFieldInterface<? super T>, Object> map = new HashMap<EntityFieldInterface<? super T>, Object>();
        map.put(key1, value1);
        map.put(key2, value2);
        return map;
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> Map<? extends EntityFieldInterface<? super T>, Object> map(EntityFieldInterface<? super T> key1, Object value1, EntityFieldInterface<? super T> key2, Object value2, EntityFieldInterface<? super T> key3, Object value3) {
        Map<EntityFieldInterface<? super T>, Object> map = new HashMap<EntityFieldInterface<? super T>, Object>();
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        return map;
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> Map<? extends EntityFieldInterface<? super T>, Object> map(EntityFieldInterface<? super T> key1, Object value1, EntityFieldInterface<? super T> key2, Object value2, EntityFieldInterface<? super T> key3, Object value3, EntityFieldInterface<? super T> key4, Object value4) {
        Map<EntityFieldInterface<? super T>, Object> map = new HashMap<EntityFieldInterface<? super T>, Object>();
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        map.put(key4, value4);
        return map;
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> Map<? extends EntityFieldInterface<? super T>, Object> map(EntityFieldInterface<? super T> key1, Object value1, EntityFieldInterface<? super T> key2, Object value2, EntityFieldInterface<? super T> key3, Object value3, EntityFieldInterface<? super T> key4, Object value4, EntityFieldInterface<? super T> key5, Object value5) {
        Map<EntityFieldInterface<? super T>, Object> map = new HashMap<EntityFieldInterface<? super T>, Object>();
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        map.put(key4, value4);
        map.put(key5, value5);
        return map;
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> Map<? extends EntityFieldInterface<? super T>, Object> map(EntityFieldInterface<? super T> key1, Object value1, EntityFieldInterface<? super T> key2, Object value2, EntityFieldInterface<? super T> key3, Object value3, EntityFieldInterface<? super T> key4, Object value4, EntityFieldInterface<? super T> key5, Object value5, EntityFieldInterface<? super T> key6, Object value6) {
        Map<EntityFieldInterface<? super T>, Object> map = new HashMap<EntityFieldInterface<? super T>, Object>();
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        map.put(key4, value4);
        map.put(key5, value5);
        map.put(key6, value6);
        return map;
    }

    /** {@inheritDoc} */
    public <T extends EntityInterface> Map<? extends EntityFieldInterface<? super T>, Object> map(EntityFieldInterface<? super T> key1, Object value1, EntityFieldInterface<? super T> key2, Object value2, EntityFieldInterface<? super T> key3, Object value3, EntityFieldInterface<? super T> key4, Object value4, EntityFieldInterface<? super T> key5, Object value5, EntityFieldInterface<? super T> key6, Object value6, EntityFieldInterface<? super T> key7, Object value7) {
        Map<EntityFieldInterface<? super T>, Object> map = new HashMap<EntityFieldInterface<? super T>, Object>();
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        map.put(key4, value4);
        map.put(key5, value5);
        map.put(key6, value6);
        map.put(key7, value7);
        return map;
    }
}
