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
/* This file has been modified by Open Source Strategies, Inc. */

package org.opentaps.foundation.entity.util;


import java.util.List;
import java.util.ListIterator;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralRuntimeException;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.opentaps.foundation.entity.EntityInterface;
import org.opentaps.foundation.exception.FoundationException;
import org.opentaps.foundation.repository.RepositoryInterface;
import org.opentaps.foundation.repository.ofbiz.Repository;


/**
 * Generic Entity Cursor List Iterator for Handling Cursored DB Results.
 * @param <T> the class of the Entity managed by this iterator
 */
public class EntityListIterator<T extends EntityInterface> implements ListIterator<T> {

    private static final String MODULE = EntityListIterator.class.getName();

    private final Class<T> classReturned;
    private final org.ofbiz.entity.util.EntityListIterator iterator;
    private final RepositoryInterface repository;

    /**
     * Creates a new <code>EntityListIterator</code> instance.
     * @param classReturned a <code>Class<T></code> value
     * @param iterator an <code>org.ofbiz.entity.util.EntityListIterator</code> value
     * @param repository a <code>RepositoryInterface</code> value
     */
    public EntityListIterator(Class<T> classReturned, org.ofbiz.entity.util.EntityListIterator iterator, RepositoryInterface repository) {
        this.classReturned = classReturned;
        this.repository = repository;
        this.iterator = iterator;
    }

    /**
     * Sets the cursor position to just after the last result so that <code>previous()</code> will return the last result.
     * @exception FoundationException if an error occurs
     */
    public void afterLast() throws FoundationException {
        try {
            iterator.afterLast();
        } catch (GenericEntityException e) {
            throw new FoundationException(e);
        }
    }

    /**
     * Sets the cursor position to just before the first result so that <code>next()</code> will return the first result.
     * @exception FoundationException if an error occurs
     */
    public void beforeFirst() throws FoundationException {
        try {
            iterator.beforeFirst();
        } catch (GenericEntityException e) {
            throw new FoundationException(e);
        }
    }

    /**
     * Sets the cursor position to last result; if result set is empty returns false.
     * @return a <code>boolean</code> value
     * @exception FoundationException if an error occurs
     */
    public boolean last() throws FoundationException {
        try {
            return iterator.last();
        } catch (GenericEntityException e) {
            throw new FoundationException(e);
        }
    }

    /**
     * Sets the cursor position to last result; if result set is empty returns false.
     * @return a <code>boolean</code> value
     * @exception FoundationException if an error occurs
     */
    public boolean first() throws FoundationException {
        try {
            return iterator.first();
        } catch (GenericEntityException e) {
            throw new FoundationException(e);
        }
    }

    /**
     * Closes the iterator and release the DB connection.
     * @exception FoundationException if an error occurs
     */
    public void close() throws FoundationException {
        try {
            iterator.close();
        } catch (GenericEntityException e) {
            throw new FoundationException(e);
        }
    }

    /**
     * NOTE: Calling this method does return the current value, but so does calling next() or previous(), so calling one of those AND this method will cause the value to be created twice.
     * @return an entity value
     * @exception FoundationException if an error occurs
     */
    public T currentValue() throws FoundationException {
        try {
            GenericValue v = iterator.currentGenericValue();
            return Repository.loadFromGeneric(classReturned, v, repository);
        } catch (GenericEntityException e) {
            throw new FoundationException(e);
        }
    }

    /**
     * Gets the current index in the iterator.
     * @return an <code>int</code> value
     * @exception FoundationException if an error occurs
     */
    public int currentIndex() throws FoundationException {
        try {
            return iterator.currentIndex();
        } catch (GenericEntityException e) {
            throw new FoundationException(e);
        }
    }

    /**
     * Performs the same function as the ResultSet.absolute method;
     * if rowNum is positive, goes to that position relative to the beginning of the list;
     * if rowNum is negative, goes to that position relative to the end of the list;
     * a rowNum of 1 is the same as first(); a rowNum of -1 is the same as last().
     * @param rowNum an <code>int</code> value
     * @return a <code>boolean</code> value
     * @exception FoundationException if an error occurs
     */
    public boolean absolute(int rowNum) throws FoundationException {
        try {
            return iterator.absolute(rowNum);
        } catch (GenericEntityException e) {
            throw new FoundationException(e);
        }
    }

    /**
     * Performs the same function as the ResultSet.relative method;
     * if rows is positive, goes forward relative to the current position;
     * if rows is negative, goes backward relative to the current position.
     * @param rows an <code>int</code> value
     * @return a <code>boolean</code> value
     * @exception FoundationException if an error occurs
     */
    public boolean relative(int rows) throws FoundationException {
        try {
            return iterator.relative(rows);
        } catch (GenericEntityException e) {
            throw new FoundationException(e);
        }
    }

    /**
     * PLEASE NOTE: Because of the nature of the JDBC ResultSet interface this method can be very inefficient; it is much better to just use next() until it returns null
     * For example, you could use the following to iterate through the results in an EntityListIterator:
     * <code>
     *      SomeEntity nextValue = null;
     *      while ((nextValue = this.next()) != null) { ... }
     * </code>
     * @return a <code>boolean</code> value
     */
    public boolean hasNext() {
        return iterator.hasNext();
    }

    /**
     * PLEASE NOTE: Because of the nature of the JDBC ResultSet interface this method can be very inefficient; it is much better to just use previous() until it returns null.
     * @return a <code>boolean</code> value
     */
    public boolean hasPrevious() {
        return iterator.hasPrevious();
    }

    /**
     * Moves the cursor to the next position and returns the T object for that position; if there is no next, returns null
     * For example, you could use the following to iterate through the results in an EntityListIterator:
     * <code>
     *      SomeEntity nextValue = null;
     *      while ((nextValue = this.next()) != null) { ... }
     * </code>
     * @return an entity value
     */
    public T next() {
        try {
            GenericValue v = iterator.next();
            return Repository.loadFromGeneric(classReturned, v, repository);
        } catch (FoundationException e) {
            try {
                this.close();
            } catch (FoundationException e1) {
                Debug.logError(e1, "Error auto-closing the EntityListIterator on error: " + e1.toString(), MODULE);
            }
            Debug.logWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString(), MODULE);
            throw new GeneralRuntimeException("Error creating Entity", e);
        }
    }

    /**
     * Returns the index of the next result, but does not guarantee that there will be a next result.
     * @return an <code>int</code> value
     */
    public int nextIndex() {
        return iterator.nextIndex();
    }

    /**
     * Moves the cursor to the previous position and returns the entity object for that position; if there is no previous, returns null.
     * @return an entity value
     */
    public T previous() {
        try {
            GenericValue v = iterator.previous();
            return Repository.loadFromGeneric(classReturned, v, repository);
        } catch (FoundationException e) {
            try {
                this.close();
            } catch (FoundationException e1) {
                Debug.logError(e1, "Error auto-closing the EntityListIterator on error: " + e1.toString(), MODULE);
            }
            Debug.logWarning("Warning: auto-closed EntityListIterator because of exception: " + e.toString(), MODULE);
            throw new GeneralRuntimeException("Error creating Entity", e);
        }
    }

    /**
     * Returns the index of the previous result, but does not guarantee that there will be a previous result.
     * @return an <code>int</code> value
     */
    public int previousIndex() {
        return iterator.previousIndex();
    }

    /**
     * Sets the result set fetch size.
     * @param rows an <code>int</code> value
     * @exception FoundationException if an error occurs
     */
    public void setFetchSize(int rows) throws FoundationException {
        try {
            iterator.setFetchSize(rows);
        } catch (GenericEntityException e) {
            throw new FoundationException(e);
        }
    }

    /**
     * Gets the complete list of results.
     * @return a <code>List<T></code> value
     * @exception FoundationException if an error occurs
     */
    public List<T> getCompleteList() throws FoundationException {
        try {
            List<GenericValue> v = iterator.getCompleteList();
            return Repository.loadFromGeneric(classReturned, v, repository);
        } catch (GenericEntityException e) {
            throw new FoundationException(e);
        }
    }

    /**
     * Gets a partial list of results starting at start and containing at most number elements.
     * Start is a one based value, ie 1 is the first element.
     * @param start an <code>int</code> value
     * @param number an <code>int</code> value
     * @return a <code>List<T></code> value
     * @exception FoundationException if an error occurs
     */
    public List<T> getPartialList(int start, int number) throws FoundationException {
        try {
            List<GenericValue> v = iterator.getPartialList(start, number);
            return Repository.loadFromGeneric(classReturned, v, repository);
        } catch (GenericEntityException e) {
            throw new FoundationException(e);
        }
    }

    /**
     * Gets the complete result size, to be called after <code>getPartialList()</code>
     * if the total number of results is needed (for pagination for example).
     *
     * @return an <code>int</code> value
     * @exception FoundationException if an error occurs
     */
    public int getResultsSizeAfterPartialList() throws FoundationException {
        try {
            return iterator.getResultsSizeAfterPartialList();
        } catch (GenericEntityException e) {
            throw new FoundationException(e);
        }
    }

    /**
     * Not implemented.
     * @param obj a <code>T</code> value
     */
    public void add(T obj) {
        throw new GeneralRuntimeException("CursorListIterator currently only supports read-only access");
    }

    /**
     * Not implemented.
     */
    public void remove() {
        throw new GeneralRuntimeException("CursorListIterator currently only supports read-only access");
    }

    /**
     * Not implemented.
     * @param obj a <code>T</code> value
     */
    public void set(T obj) {
        throw new GeneralRuntimeException("CursorListIterator currently only supports read-only access");
    }
}
