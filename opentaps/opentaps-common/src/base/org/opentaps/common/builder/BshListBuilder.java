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
package org.opentaps.common.builder;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import bsh.NameSpace;
import bsh.Primitive;
import bsh.This;
import bsh.UtilEvalError;
import javolution.util.FastList;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityFindOptions;
import org.opentaps.foundation.entity.EntityException;
import org.opentaps.foundation.entity.EntityInterface;
import org.opentaps.foundation.repository.RepositoryInterface;
import org.opentaps.foundation.util.FoundationUtils;

/**
 * Builder that is constructed from a beanshell closure.
 * In a typical bsh file you can define a method such as the following,
 *
 * <pre>
 * buildOrderList(delegator, statusId) {
 *
 *     // set up the entity conditions like this
 *     entityName = "OrderHeader";
 *     where = UtilMisc.toMap("statusId", statusId);
 *
 *     // this is an implementation of PageBuilder.build(), which will be used to post-process
 *     // a page of data, which in this case is a list of OrderHeader
 *     build(page) {
 *         newPage = FastList.newInstance();
 *
 *         for (order : page) {
 *
 *             // make a new map for the row to replace the OrderHeader
 *             newRow = FastMap.newInstance();
 *             newRow.putAll( order.getAllFields() );
 *
 *             // grab some extra data
 *             newRow.put("billToPartyName", PartyHelper.getPartyName(delegator, order.getString("billToPartyId"), false));
 *             newPage.add(newRow);
 *         }
 *         return newPage; // this page will be displayed to user instead
 *     }
 *     return this; // important return statement, won't work without it
 * }
 *
 * // call the function so that it finds all created orders, then put it in context
 * context.put("orderListBuilder", buildOrderList(delegator, "ORDER_CREATED"));
 *
 * // in FTL, we would pass it to the paginate macro as the list argument
 * &lt;@paginate name="orderList" list=orderListBuilder /&gt;
 * </pre>
 */
public class BshListBuilder extends EntityListBuilder {

    private static final String ERR = "Cannot set up pagination from bsh closure:  ";

    public BshListBuilder(This closure, Delegator delegator) throws ListBuilderException {
        this.delegator = delegator;

        try {
            NameSpace ns = closure.getNameSpace();

            // this is pretty cool, we can construct the page builder interface directly from the closure
            this.pageBuilder = getPageBuilder(ns, closure);

            // get the variables
            this.entityName = getString(ns, "entityName");
            this.entityClass = getClass(ns, "entityClass");
            this.repository = getRepository(ns, "repository");
            this.where = getCondition(ns, "where");
            this.having = getCondition(ns, "having");
            this.fieldsToSelect = getCollection(ns, "fieldsToSelect");
            this.orderBy = getList(ns, "orderBy");
            this.options = getOptions(ns, "options");

            // now validate what we need
            if (entityName == null && entityClass == null) {
                throw new ListBuilderException(ERR + "Field 'entityName' or 'entityClass' must be defined.  Please make sure it is.");
            }
            if (entityName == null) {
                try {
                    entityName = FoundationUtils.getEntityBaseName(entityClass);
                } catch (EntityException e) {
                    throw new ListBuilderException(ERR + "Field 'entityClass' cannot be read as an entity Class.  Please make sure it is.");
                }
            }

            if (where == null) {
                throw new ListBuilderException(ERR + "Field 'where' must be defined.  Please make sure it is.");
            }

            // collect the closure since we don't need it anymore
            closure = null;

        } catch (UtilEvalError e) {
            throw new ListBuilderException(ERR + e.getMessage());
        }
    }

    // TODO this is kind of weak, we can grab method by their signatures, but it's unclear what an unsigned argument would resolve to
    private PageBuilder getPageBuilder(NameSpace ns, This closure) throws UtilEvalError {
        String[] methodNames = ns.getMethodNames();
        for (int i = 0; i < methodNames.length; i++) {
            if ("build".equals(methodNames[i])) {
                return (PageBuilder) closure.getInterface(PageBuilder.class);
            }
        }
        return null;
    }


    private Object getVariable(NameSpace ns, String name) throws UtilEvalError {
        Object var = ns.getVariable(name);
        if (var == null || var == Primitive.VOID) {
            return null;
        }
        return var;
    }

    private String getString(NameSpace ns, String name) throws ListBuilderException, UtilEvalError {
        Object obj = getVariable(ns, "entityName");
        if (obj == null) {
            return null;
        }
        if (obj instanceof String) {
            return (String) obj;
        }
        throw new ListBuilderException(ERR + "Field '" + name + "' must be a String.  I was passed " + obj.getClass().getName() + ".");
    }

    private EntityCondition getCondition(NameSpace ns, String name) throws ListBuilderException, UtilEvalError {
        Object obj = getVariable(ns, name);
        if (obj == null) {
            return null;
        }
        if (obj instanceof EntityCondition) {
            return (EntityCondition) obj;
        }
        if (obj instanceof List) {
            return EntityCondition.makeCondition((List) obj, EntityOperator.AND);
        }
        if (obj instanceof Map) {
            Map map = (Map) obj;
            List<EntityCondition> conditions = FastList.newInstance();
            for (Iterator iter = map.keySet().iterator(); iter.hasNext();) {
                String key = (String) iter.next();
                Object value = map.get(key);
                conditions.add(EntityCondition.makeCondition(key, value));
            }
            return EntityCondition.makeCondition(conditions, EntityOperator.AND);
        }
        throw new ListBuilderException(ERR + "Field '" + name + "' must be an EntityCondition, a Map representation of one, or a List representation of one.  I was passed " + obj.getClass().getName() + ".");
    }

    private List getList(NameSpace ns, String name) throws ListBuilderException, UtilEvalError {
        Object obj = getVariable(ns, name);
        if (obj == null) {
            return null;
        }
        if (obj instanceof List) {
            return (List) obj;
        }
        throw new ListBuilderException(ERR + "Field '" + name + "' must be a List.  I was passed " + obj.getClass().getName() + ".");
    }

    private Class getClass(NameSpace ns, String name) throws ListBuilderException, UtilEvalError {
        Object obj = getVariable(ns, name);
        if (obj == null) {
            return null;
        }
        if (obj instanceof Class) {
            return (Class) obj;
        }
        throw new ListBuilderException(ERR + "Field '" + name + "' must be a Class.  I was passed " + obj.getClass().getName() + ".");
    }

    private RepositoryInterface getRepository(NameSpace ns, String name) throws ListBuilderException, UtilEvalError {
        Object obj = getVariable(ns, name);
        if (obj == null) {
            return null;
        }
        if (obj instanceof RepositoryInterface) {
            return (RepositoryInterface) obj;
        }
        throw new ListBuilderException(ERR + "Field '" + name + "' must be a RepositoryInterface.  I was passed " + obj.getClass().getName() + ".");
    }

    private Collection getCollection(NameSpace ns, String name) throws ListBuilderException, UtilEvalError {
        Object obj = getVariable(ns, name);
        if (obj == null) {
            return null;
        }
        if (obj instanceof Collection) {
            return (Collection) obj;
        }
        throw new ListBuilderException(ERR + "Field '" + name + "' must be a Collection.  I was passed " + obj.getClass().getName() + ".");
    }

    private EntityFindOptions getOptions(NameSpace ns, String name) throws ListBuilderException, UtilEvalError {
        Object obj = getVariable(ns, name);
        if (obj == null) {
            return DISTINCT_READ_OPTIONS;
        }
        if (obj instanceof EntityFindOptions) {
            return (EntityFindOptions) obj;
        }
        throw new ListBuilderException(ERR + "Field '" + name + "' must be an EntityFindOption.  I was passed " + obj.getClass().getName() + ".");
    }
}
