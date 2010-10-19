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

import java.util.Map;

/**
 * This exception is thrown when an entity was expected to exist in the database but could not be found.
 */
public class EntityNotFoundException extends EntityException {

    private static final String DEFAULT_MESSAGE = "Entity [%1$s] not found for primary key %2$s";

    private Map<? extends EntityFieldInterface<?>, ? extends Object> primaryKey;

    /**
     * Creates a new <code>EntityNotFoundException</code> instance.
     */
    public EntityNotFoundException() {
        super();
    }

    /**
     * Creates a new <code>EntityNotFoundException</code> instance with the default error message.
     * @param entityClass the class of the entity related to this exception
     * @param primaryKey a Map of <code>String</code> to values representing the primary key that was tried
     */
    public EntityNotFoundException(Class<? extends EntityInterface> entityClass, Map<? extends EntityFieldInterface<?>, ? extends Object> primaryKey) {
        super(entityClass, String.format(DEFAULT_MESSAGE, entityClass.getName(), primaryKey));
        this.primaryKey = primaryKey;
    }

    /**
     * Creates a new <code>EntityNotFoundException</code> instance with a given error message.
     * @param entityClass the class of the entity related to this exception
     * @param message a <code>String</code> value
     */
    public EntityNotFoundException(Class<? extends EntityInterface> entityClass, String message) {
        super(entityClass, message);
    }

    /**
     * Creates a new <code>EntityNotFoundException</code> instance with a given error message.
     * @param entityClass the class of the entity related to this exception
     * @param primaryKey a Map of <code>String</code> to values representing the primary key that was tried
     * @param message a <code>String</code> value, if <code>null</code> then will use the default message
     */
    public EntityNotFoundException(Class<? extends EntityInterface> entityClass, Map<? extends EntityFieldInterface<?>, ? extends Object> primaryKey, String message) {
        super(entityClass, message == null ? String.format(DEFAULT_MESSAGE, entityClass.getName(), primaryKey) : message);
        this.primaryKey = primaryKey;
    }

    /**
     * Creates a new <code>EntityNotFoundException</code> instance from another exception.
     * @param entityClass the class of the entity related to this exception
     * @param exception a <code>Throwable</code> value
     */
    public EntityNotFoundException(Class<? extends EntityInterface> entityClass, Throwable exception) {
        super(entityClass, exception);
    }

    /**
     * Creates a new <code>EntityNotFoundException</code> instance from another exception.
     * @param entityClass the class of the entity related to this exception
     * @param primaryKey a Map of <code>String</code> to values representing the primary key that was tried
     * @param exception a <code>Throwable</code> value
     */
    public EntityNotFoundException(Class<? extends EntityInterface> entityClass, Map<? extends EntityFieldInterface<?>, ? extends Object> primaryKey, Throwable exception) {
        super(entityClass, exception);
        this.primaryKey = primaryKey;
    }

    /**
     * Creates a new <code>EntityNotFoundException</code> instance with a label message.
     * @param entityClass the class of the entity related to this exception
     * @param messageLabel the label of the message
     * @param messageContext the context for substitution in the message
     */
    public EntityNotFoundException(Class<? extends EntityInterface> entityClass, String messageLabel, Map<String, ? extends Object> messageContext) {
        super(entityClass, messageLabel, messageContext);
    }

    /**
     * Creates a new <code>EntityNotFoundException</code> instance with a label message.
     * @param entityClass the class of the entity related to this exception
     * @param primaryKey a Map of <code>String</code> to values representing the primary key that was tried
     * @param messageLabel the label of the message
     * @param messageContext the context for substitution in the message
     */
    public EntityNotFoundException(Class<? extends EntityInterface> entityClass, Map<? extends EntityFieldInterface<?>, ? extends Object> primaryKey, String messageLabel, Map<String, ? extends Object> messageContext) {
        super(entityClass, messageLabel, messageContext);
        this.primaryKey = primaryKey;
    }

    /**
     * Gets the primary key that was given when trying to retrieve the entity.
     * @return the primary key that was given when trying to retrieve the entity, or <code>null</code> if it was not supplied
     */
    public Map<? extends EntityFieldInterface<?>, ? extends Object> getPrimaryKey() {
        return primaryKey;
    }
}
