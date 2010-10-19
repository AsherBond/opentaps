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

import org.opentaps.foundation.exception.FoundationException;

import java.util.Map;

/**
 * Base class for entity related exceptions.
 */
public class EntityException extends FoundationException {

    private Class<? extends EntityInterface> entityClass;

    /**
     * Creates a new <code>EntityException</code> instance.
     */
    public EntityException() {
        super();
    }

    /**
     * Creates a new <code>EntityException</code> instance with a given error message.
     * @param entityClass the class of the entity related to this exception
     */
    public EntityException(Class<? extends EntityInterface> entityClass) {
        super();
        this.entityClass = entityClass;
    }

    /**
     * Creates a new <code>EntityException</code> instance with a given error message.
     * @param entityClass the class of the entity related to this exception
     * @param message a <code>String</code> value
     */
    public EntityException(Class<? extends EntityInterface> entityClass, String message) {
        super(message);
        this.entityClass = entityClass;
    }

    /**
     * Creates a new <code>EntityException</code> instance from another exception.
     * @param entityClass the class of the entity related to this exception
     * @param exception a <code>Throwable</code> value
     */
    public EntityException(Class<? extends EntityInterface> entityClass, Throwable exception) {
        super(exception);
        this.entityClass = entityClass;
    }

    /**
     * Creates a new <code>EntityException</code> instance with a label message.
     * @param entityClass the class of the entity related to this exception
     * @param messageLabel the label of the message
     * @param messageContext the context for substitution in the message
     */
    public EntityException(Class<? extends EntityInterface> entityClass, String messageLabel, Map<String, ? extends Object> messageContext) {
        super(messageLabel, messageContext);
        this.entityClass = entityClass;
    }

    /**
     * Gets the entity class that is related to this exception.
     * @return the entity class that is related to this exception
     */
    public Class<? extends EntityInterface> getEntityClass() {
        return entityClass;
    }
}
