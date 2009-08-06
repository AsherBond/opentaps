package org.opentaps.common.builder;

/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 *
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
 */

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericModelException;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityListIterator;

import java.util.Collection;
import java.util.List;

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

    public static final EntityFindOptions DISTINCT_READ_OPTIONS = new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, true);

    protected String entityName = null;
    protected EntityCondition where = null;
    protected EntityCondition having = null;
    protected Collection<String> fieldsToSelect = null;
    protected List<String> orderBy = null;
    protected EntityFindOptions options = null;
    protected EntityListIterator iterator = null;
    protected GenericDelegator delegator = null;
    protected boolean transactionOpen = false;
    protected int size = 0;

    protected EntityListBuilder() { };

    /** Full constructor containing all possible fields. */
    public EntityListBuilder(String entityName, EntityCondition where, EntityCondition having, Collection<String> fieldsToSelect, List<String> orderBy, EntityFindOptions options) {
        this.entityName = entityName;
        this.where = where;
        this.having = having;
        this.fieldsToSelect = fieldsToSelect;
        this.orderBy = orderBy;
        this.options = options;
    }

    /** Distinct readonly lookup. */
    public EntityListBuilder(String entityName, EntityCondition where, List<String> orderBy) {
        this(entityName, where, null, null, orderBy, DISTINCT_READ_OPTIONS);
    }

    /** Distinct readonly lookup limited to certain fields. */
    public EntityListBuilder(String entityName, EntityCondition where, Collection<String> fieldsToSelect, List<String> orderBy) {
        this(entityName, where, null, fieldsToSelect, orderBy, DISTINCT_READ_OPTIONS);
    }

    /** Distinct readonly lookup for all values. */
    public EntityListBuilder(String entityName) {
        this(entityName, null, null, null, null, DISTINCT_READ_OPTIONS);
    }

    /** As a convenience, the delegator is set when a call to the pagination macro is made.  This saves us a parameter in the constructors. */
    public void setDelegator(GenericDelegator delegator) {
        this.delegator = delegator;
    }

    public GenericDelegator getDelegator() {
        return delegator;
    }

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

    public boolean isInitialized() {
        return iterator != null;
    }

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
     */
    public long getListSize() throws ListBuilderException {
        if (!isInitialized()) {
            initialize();
        }
        return size;
    }

    /** This builder is always deterministic. */
    public boolean isDeterministic() {
        return true;
    }


    /**
     * Gets a partial list of a given size starting from a cursor index.
     * Note that although the arguments are longs, the EntityListIterator must
     * accept ints.
     */
    public List getPartialList(long viewSize, long cursorIndex) throws ListBuilderException {
        if (!isInitialized()) {
            initialize();
        }
        try {
            // XXX Note:  EntityListIterator is a 1 based list, so we must add 1 to index
            List results = iterator.getPartialList((int) cursorIndex + 1, (int) viewSize);
            close();
            return results;
        } catch (GenericEntityException e) {
            throw new ListBuilderException(e);
        }
    }

    /**
     * When the order specification changes, we have to rebuild the iterator.
     * This is done by closing the existing iterator and then changing the
     * orderBy variable.  The iterator will be re-initialized during a later operation.
     */
    public void changeOrderBy(List<String> orderBy) {
        close();
        this.orderBy = orderBy;
    }

}
