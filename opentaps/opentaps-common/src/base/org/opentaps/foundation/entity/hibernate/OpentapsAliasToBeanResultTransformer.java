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

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
/* This file has been modified by Open Source Strategies, Inc. */

package org.opentaps.foundation.entity.hibernate;

import java.math.BigDecimal;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.property.ChainedPropertyAccessor;
import org.hibernate.property.PropertyAccessor;
import org.hibernate.property.PropertyAccessorFactory;
import org.hibernate.property.Setter;
import org.hibernate.transform.ResultTransformer;

/**
 * Opentaps AliasToBeanResultTransformer, use for transformer query result to entity.
 */
public class OpentapsAliasToBeanResultTransformer implements ResultTransformer {
    private final Class resultClass;
    private final PropertyAccessor propertyAccessor;
    private Setter[] setters;

    /**
     * Constructor.
     * @param resultClass a <code>Class</code> value
     */
    public OpentapsAliasToBeanResultTransformer(Class resultClass) {
        if (resultClass == null) {
            throw new IllegalArgumentException("resultClass cannot be null");
        }
        this.resultClass = resultClass;
        propertyAccessor = new ChainedPropertyAccessor(new PropertyAccessor[] {
                PropertyAccessorFactory.getPropertyAccessor(resultClass, null),
                PropertyAccessorFactory.getPropertyAccessor("field") });
    }

    /**
     * {@inheritDoc}
     * transform tuple to properties of target object.
     * @param tuple a <code>Object[]</code> value
     * @param aliases a <code>String[]</code> value
     */
    public Object transformTuple(Object[] tuple, String[] aliases) {
        Object result;
        try {
            // init setters for result
            if (setters == null) {
                setters = new Setter[aliases.length];
                for (int i = 0; i < aliases.length; i++) {
                    String alias = aliases[i];
                    if (alias != null) {
                        setters[i] = propertyAccessor.getSetter(resultClass,
                                alias);
                    }
                }
            }
            // create new object instance for transform
            result = resultClass.newInstance();
            // iterator all aliases to set value of properties
            for (int i = 0; i < aliases.length; i++) {
                if (setters[i] != null) {
                    Class parameterClass = setters[i].getMethod().getParameterTypes()[0];
                    // if the value type not equal setter parameter type, then do special process
                    if (tuple[i] != null && !tuple[i].getClass().equals(parameterClass)) {
                        // if field is BigDecimal, then change value to BigDecimal before set
                        if (parameterClass.equals(BigDecimal.class)) {
                            setters[i].set(result, convertToBigDecimal(tuple[i]), null);
                        }
                    } else {
                        setters[i].set(result, tuple[i], null);
                    }
                }
            }
        } catch (InstantiationException e) {
            throw new HibernateException("Could not instantiate resultclass: "
                    + resultClass.getName());
        } catch (IllegalAccessException e) {
            throw new HibernateException("Could not instantiate resultclass: "
                    + resultClass.getName());
        }

        return result;
    }


    /**
    * This is a special method for converting a object value to <code>BigDecimal</code>, because the query result might
    * return double or string types.
    * @param value the value object to convert to a <code>BigDecimal</code>
    * @return the <code>BigDecimal</code> value or <code>null</code>
    */
    public BigDecimal convertToBigDecimal(Object value) {
        if (value instanceof Double) {
            return BigDecimal.valueOf(((Double) value).doubleValue());
        } else if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        } else if (value instanceof String) {
            return new BigDecimal((String) value);
        } else {
            // this should not happen
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public List transformList(List collection) {
        return collection;
    }

    /**
     * Gets the hashcode of class.
     * @return a <code>int</code> value
     */
    public int hashCode() {
        int result;
        result = resultClass.hashCode();
        result = 31 * result + propertyAccessor.hashCode();
        return result;
    }
}
