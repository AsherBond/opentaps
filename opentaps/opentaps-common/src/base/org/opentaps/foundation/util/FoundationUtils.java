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
package org.opentaps.foundation.util;

import org.ofbiz.base.util.UtilValidate;
import org.opentaps.foundation.entity.EntityInterface;
import org.opentaps.foundation.entity.EntityException;
import org.opentaps.foundation.exception.FoundationException;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Foundation utility class handling entity maintenance related methods.
 */
public final class FoundationUtils {

    private static String ENTITY_MAP_SETTER_NAME = "fromMap";      // name of method in Entity class and sub-classes to set values from a Map

    private FoundationUtils() { }

    /**
     * Standardize accessor method names.  If fieldName is orderId, will return "prefix" + OrderId.
     * @param prefix prefix to the method name, for example "get" or "set"
     * @param fieldName name of the field the accessor method access
     * @return the accessor method name
     * @throws FoundationException if the given field name is empty
     */
    public static String accessorMethodName(String prefix, String fieldName) throws FoundationException {
        if (UtilValidate.isEmpty(fieldName)) {
            throw new FoundationException("methodName called for null or empty fieldName");
        } else {
            return prefix + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        }
    }

    /**
     * Standardize getter method names.
     * @param fieldName name of the field the accessor method access
     * @return the getter method name
     * @throws FoundationException if the given field name is empty
     */
    public static String getterName(String fieldName) throws FoundationException {
        return accessorMethodName("get", fieldName);
    }


    /**
     * Standardize setter method names.
     * @param fieldName name of the field the accessor method access
     * @return the setter method name
     * @throws FoundationException if the given field name is empty
     */
    public static String setterName(String fieldName) throws FoundationException {
        return accessorMethodName("set", fieldName);
    }

    /**
     * Creates a new instance of the entityClass and populates it with the values from mapValue, using
     * the fromMap(Map mapValue) method defined in all opentaps Entities.
     * @param <T> the class of the entity object to instance
     * @param entityClass the class of the entity object to instance
     * @param mapValue values loaded in the instanced entity
     * @return an entity instance of the given type initialized with the given values
     * @throws EntityException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public static <T extends EntityInterface> T loadFromMap(Class<T> entityClass, Map<String, Object> mapValue) throws EntityException {
        try {
            T newEntity = entityClass.newInstance();
            Class[] methodParams = {java.util.Map.class};
            // java.lang.reflection.Class getDeclaredMethod only looks to methods declared in the class
            // getMethod will look at methods inherited from the superclass as well, such as those in the base entities
            Method method = entityClass.getMethod(ENTITY_MAP_SETTER_NAME, methodParams);
            Object[] invokeParams = {mapValue};
            method.invoke(newEntity, invokeParams);
            return newEntity;
        } catch (Throwable t) {
            throw new EntityException(entityClass, t);
        }
    }

    /**
     * Creates and returns new instance of Entity.
     * @param <T> the class of the entity object to instance
     * @param entityClass the class of the entity object to instance
     * @return an entity instance of the given type initialized with the given values
     * @throws EntityException if an error occurs
     */
    public static <T extends EntityInterface> T newInstance(Class<T> entityClass) throws EntityException {
        T newEntity = null;
        try {
            newEntity = entityClass.newInstance();
        } catch (Throwable t) {
            throw new EntityException(entityClass, t);
        }
        return newEntity;
    }

    /**
     * Gets an entity base.
     * @param <T> the class of the entity to get the base name for
     * @param entityClass the class of the entity object to instance
     * @return the entity base name
     * @throws EntityException if an error occurs
     */
    public static <T extends EntityInterface> String getEntityBaseName(Class<T> entityClass) throws EntityException {
        T entity = newInstance(entityClass);
        return entity.getBaseEntityName();
    }

}

