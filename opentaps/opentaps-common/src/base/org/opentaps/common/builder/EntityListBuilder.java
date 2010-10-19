package org.opentaps.common.builder;

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

import java.util.Collection;
import java.util.List;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericModelException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityListIterator;
import org.opentaps.foundation.entity.EntityException;
import org.opentaps.foundation.entity.EntityInterface;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.RepositoryInterface;
import org.opentaps.foundation.repository.ofbiz.Repository;
import org.opentaps.foundation.util.FoundationUtils;

/**
 * Basic builder to look up entities and view entities using
 * delegator.findListIteratorByCondition().  Constructing this
 * builder is similar to constructing a findListIteratorByCondition()
 * method call.  It is meant to replace the use of EntityListIterator
 * for building lists.
 *
 * There are three ways to use this class:  Directly, by extending, or
 * by using the PageBuilder system.  The first two methods are
 * discouraged, as this class is designed to work from within the larger
 * pagination framework, which uses PageBuilders.  For more information
 * about PageBuilders, please see PageBuilder.java and the pagination
 * documentation.
 */
public class EntityListBuilder extends AbstractListBuilder {

    /** Option to pass to the delegator methods to return distinct results. */
    public static final EntityFindOptions DISTINCT_READ_OPTIONS = new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, true);

    protected String entityName = null;
    protected Class<? extends EntityInterface> entityClass = null;
    protected RepositoryInterface repository = null;
    protected EntityCondition where = null;
    protected EntityCondition having = null;
    protected Collection<String> fieldsToSelect = null;
    protected List<String> orderBy = null;
    protected EntityFindOptions options = null;
    protected EntityListIterator iterator = null;
    protected Delegator delegator = null;
    protected boolean transactionOpen = false;
    protected int size = 0;

    protected EntityListBuilder() { };

    /**
     * Full constructor for Delegator based lists, containing all possible fields.
     * @param entityName a <code>String</code> value
     * @param where an <code>EntityCondition</code> value
     * @param having an <code>EntityCondition</code> value
     * @param fieldsToSelect a <code>List</code> value
     * @param orderBy a <code>List</code> value
     * @param options an <code>EntityFindOptions</code> value
     */
    public EntityListBuilder(String entityName, EntityCondition where, EntityCondition having, Collection<String> fieldsToSelect, List<String> orderBy, EntityFindOptions options) {
        this.entityName = entityName;
        this.where = where;
        this.having = having;
        this.fieldsToSelect = fieldsToSelect;
        this.orderBy = orderBy;
        this.options = options;
    }

    /**
     * Distinct readonly lookup.
     * @param entityName a <code>String</code> value
     * @param where an <code>EntityCondition</code> value
     * @param orderBy a <code>List</code> value
     */
    public EntityListBuilder(String entityName, EntityCondition where, List<String> orderBy) {
        this(entityName, where, null, null, orderBy, DISTINCT_READ_OPTIONS);
    }

    /**
     * Distinct readonly lookup limited to certain fields.
     * @param entityName a <code>String</code> value
     * @param where an <code>EntityCondition</code> value
     * @param fieldsToSelect a <code>List</code> value
     * @param orderBy a <code>List</code> value
     */
    public EntityListBuilder(String entityName, EntityCondition where, Collection<String> fieldsToSelect, List<String> orderBy) {
        this(entityName, where, null, fieldsToSelect, orderBy, DISTINCT_READ_OPTIONS);
    }

    /**
     * Distinct readonly lookup for all values.
     * @param entityName a <code>String</code> value
     */
    public EntityListBuilder(String entityName) {
        this(entityName, null, null, null, null, DISTINCT_READ_OPTIONS);
    }

    // domain constructor

    /**
     * Full constructor for domain based lists, containing all possible fields.
     * @param repository the <code>RepositoryInterface</code> to use
     * @param entityClass the entity to find
     * @param where an <code>EntityCondition</code> value
     * @param having an <code>EntityCondition</code> value
     * @param fieldsToSelect a <code>List</code> value
     * @param orderBy a <code>List</code> value
     * @param options an <code>EntityFindOptions</code> value
     * @exception ListBuilderException if an error occurs
     */
    public EntityListBuilder(RepositoryInterface repository, Class<? extends EntityInterface> entityClass, EntityCondition where, EntityCondition having, Collection<String> fieldsToSelect, List<String> orderBy, EntityFindOptions options) throws ListBuilderException {
        this.entityClass = entityClass;
        try {
            this.entityName = FoundationUtils.getEntityBaseName(entityClass);
        } catch (EntityException e) {
            throw new ListBuilderException("Field 'entityClass' cannot be read as an entity Class.  Please make sure it is.");
        }
        this.repository = repository;
        this.where = where;
        this.having = having;
        this.fieldsToSelect = fieldsToSelect;
        this.orderBy = orderBy;
        this.options = options;
    }

    /**
     * Distinct readonly lookup.
     * @param repository the <code>RepositoryInterface</code> to use
     * @param entityClass the entity to find
     * @param where an <code>EntityCondition</code> value
     * @param orderBy a <code>List</code> value
     * @exception ListBuilderException if an error occurs
     */
    public EntityListBuilder(RepositoryInterface repository, Class<? extends EntityInterface> entityClass, EntityCondition where, List<String> orderBy) throws ListBuilderException {
        this(repository, entityClass, where, null, null, orderBy, DISTINCT_READ_OPTIONS);
    }

    /**
     * Distinct readonly lookup limited to certain fields.
     * @param repository the <code>RepositoryInterface</code> to use
     * @param entityClass the entity to find
     * @param where an <code>EntityCondition</code> value
     * @param fieldsToSelect a <code>List</code> value
     * @param orderBy a <code>List</code> value
     * @exception ListBuilderException if an error occurs
     */
    public EntityListBuilder(RepositoryInterface repository, Class<? extends EntityInterface> entityClass, EntityCondition where, Collection<String> fieldsToSelect, List<String> orderBy) throws ListBuilderException {
        this(repository, entityClass, where, null, fieldsToSelect, orderBy, DISTINCT_READ_OPTIONS);
    }

    /**
     * Distinct readonly lookup for all values.
     * @param repository the <code>RepositoryInterface</code> to use
     * @param entityClass the entity to find
     * @exception ListBuilderException if an error occurs
     */
    public EntityListBuilder(RepositoryInterface repository, Class<? extends EntityInterface> entityClass) throws ListBuilderException {
        this(repository, entityClass, null, null, null, null, DISTINCT_READ_OPTIONS);
    }


    /**
     * As a convenience, the delegator is set when a call to the pagination macro is made.  This saves us a parameter in the constructors.
     * @param delegator a <code>Delegator</code> value
     */
    public void setDelegator(Delegator delegator) {
        this.delegator = delegator;
    }

    /**
     * Gets the current Delegator instance.
     * @return a <code>Delegator</code> value
     */
    public Delegator getDelegator() {
        return delegator;
    }

    /**
     * Initializes this <code>EntityListBuilder</code>.
     * This opens the EntityListIterator and gets the total number of results.
     * @exception ListBuilderException if an error occurs
     */
    public void initialize() throws ListBuilderException {
        if (isInitialized()) {
            return;
        }
        try {
            transactionOpen = TransactionUtil.begin();
            iterator = delegator.findListIteratorByCondition(entityName, where, having, fieldsToSelect, orderBy, options);
            determineSize();
        } catch (GenericModelException e) {
            // if one of the fields is not correct, then try throwing away orderBy and fieldsToSelect and try again
            // ideally we should use ModelEntity to validate in the constructor of EntityListBuilder but in real life this is ok
            try {
                Debug.logWarning("Exception while trying to query [" + entityName + "] with where conditions [" + where + "] having conditions [" + having + "] fields to select [" + fieldsToSelect + "] order by [" + orderBy + "]: " + e.getMessage(), module);
                iterator = delegator.findListIteratorByCondition(entityName, where, having, null, null, options);
                determineSize();
            } catch (GenericEntityException ex) {
                throw new ListBuilderException(ex);
            }
        } catch (GenericEntityException e) {
            throw new ListBuilderException(e);
        }
    }

    protected void determineSize() throws GenericEntityException {
        if (iterator.last()) {
            size = iterator.currentIndex();
            iterator.beforeFirst();
        }
    }

    /**
     * Checks if this <code>EntityListBuilder</code> has been initialized.
     * @return a <code>boolean</code> value
     */
    public boolean isInitialized() {
        return iterator != null;
    }

    /**
     * Closes this <code>EntityListBuilder</code> if it has been initialized.
     */
    public void close() {
        if (isInitialized()) {
            try {
                iterator.close();
                TransactionUtil.commit(transactionOpen);
                iterator = null;
            } catch (GenericEntityException e) {
                // this is already logged I think
            }
        }
    }

    /**
     * Gets the size of the list.  After using this function, you will have
     * to close the builder manually unless you get data from the list.
     * (An operation that automatically closes the list).  The size is
     * cached by the initialize() function, so calling this multiple times
     * in a row should be safe.
     * @return a <code>long</code> value
     * @exception ListBuilderException if an error occurs
     */
    public long getListSize() throws ListBuilderException {
        if (!isInitialized()) {
            initialize();
        }
        return size;
    }

    /**
     * This builder is always deterministic.
     * @return a <code>boolean</code> value
     */
    public boolean isDeterministic() {
        return true;
    }


    /**
     * Gets a partial list of a given size starting from a cursor index.
     * Note that although the arguments are longs, the EntityListIterator must
     * accept ints.
     * @param viewSize a <code>long</code> value
     * @param cursorIndex a <code>long</code> value
     * @return a <code>List</code> value
     * @exception ListBuilderException if an error occurs
     */
    public List getPartialList(long viewSize, long cursorIndex) throws ListBuilderException {
        if (!isInitialized()) {
            initialize();
        }
        try {
            // XXX Note:  EntityListIterator is a 1 based list, so we must add 1 to index
            List<GenericValue> results = iterator.getPartialList((int) cursorIndex + 1, (int) viewSize);
            close();
            // convert to domain entities if needed
            if (repository != null && entityClass != null) {
                List domainResults = null;
                try {
                    domainResults = Repository.loadFromGeneric(entityClass, results, repository);
                } catch (RepositoryException e) {
                    Debug.logError(e, module);
                }
                if (domainResults != null) {
                    return domainResults;
                }
            }

            return results;
        } catch (GenericEntityException e) {
            throw new ListBuilderException(e);
        }
    }

    /**
     * When the order specification changes, we have to rebuild the iterator.
     * This is done by closing the existing iterator and then changing the
     * orderBy variable.  The iterator will be re-initialized during a later operation.
     * @param orderBy the new order by list
     */
    public void changeOrderBy(List<String> orderBy) {
        close();
        this.orderBy = orderBy;
    }

}
