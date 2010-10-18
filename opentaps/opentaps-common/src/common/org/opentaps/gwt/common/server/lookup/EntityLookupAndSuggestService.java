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

package org.opentaps.gwt.common.server.lookup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityFunction;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.foundation.entity.EntityInterface;
import org.opentaps.gwt.common.client.lookup.UtilLookup;
import org.opentaps.gwt.common.server.InputProviderInterface;

/**
 * The base service to perform entity lookups and / or suggest.
 */
public abstract class EntityLookupAndSuggestService extends EntityLookupService {

    // for autocompleters
    private String suggestQuery;

    protected EntityLookupAndSuggestService(InputProviderInterface provider, List<String> fields) {
        super(provider, fields);

        suggestQuery = provider.getParameter(UtilLookup.PARAM_SUGGEST_QUERY);
    }

    /**
     * Gets the query string that should be used as the filter for a suggest.
     * @return a <code>String</code> value
     */
    public String getSuggestQuery() {
        return suggestQuery;
    }

    /**
     * Builds the query for suggesters that will lookup the list of given fields for the given query string.
     *
     * For example:
     *  <code>findSuggestMatchesAnyOf(Product.class, UtilMisc.toList("internalName", "productName"))</code>
     *  finds all <code>Product</code> for which the "internalName" OR the "productName" matches the <code>suggestQuery</code>
     *  passed as parameter.
     *  The match is done by looking values containing the <code>suggestQuery</code>, case insensitive.
     *
     * @param <T> the entity class to return
     * @param entity the entity class to return
     * @param fields the list of fields to lookup
     * @return the list of entities found, or <code>null</code> if an error occurred
     * @throws RepositoryException if an error occurs
     */
    protected <T extends EntityInterface> List<T> findSuggestMatchesAnyOf(Class<T> entity, List<String> fields) {
        return findSuggestMatchesAnyOf(entity, getSuggestQuery(), fields);
    }

    /**
     * Builds the query for suggesters that will lookup the list of given fields for the given query string.
     *
     * For example:
     *  <code>findSuggestMatchesAnyOf(Product.class, UtilMisc.toList("internalName", "productName"))</code>
     *  finds all <code>Product</code> for which the "internalName" OR the "productName" matches the <code>suggestQuery</code>
     *  passed as parameter.
     *  The match is done by looking values containing the <code>suggestQuery</code>, case insensitive.
     *
     * @param <T> the entity class to return
     * @param entity the entity class to return
     * @param fields the list of fields to lookup
     * @param additionalFilter a condition to restrict the possible matches
     * @return the list of entities found, or <code>null</code> if an error occurred
     * @throws RepositoryException if an error occurs
     */
    protected <T extends EntityInterface> List<T> findSuggestMatchesAnyOf(Class<T> entity, List<String> fields, EntityCondition additionalFilter) {
        return findSuggestMatchesAnyOf(entity, getSuggestQuery(), fields, additionalFilter);
    }

    /**
     * Builds the query for suggesters that will lookup the list of given fields for the given query string.
     *
     * For example:
     *  <code>findSuggestMatchesAnyOf(Product.class, UtilMisc.toList("internalName", "productName"))</code>
     *  finds all <code>Product</code> for which the "internalName" OR the "productName" matches the given <code>query</code>
     *  passed as parameter.
     *  The match is done by looking values containing the <code>query</code>, case insensitive.
     *
     * @param <T> the entity class to return
     * @param entity the entity class to return
     * @param query the string to lookup
     * @param fields the list of fields to lookup
     * @return the list of entities found, or <code>null</code> if an error occurred
     */
    protected <T extends EntityInterface> List<T> findSuggestMatchesAnyOf(Class<T> entity, String query, List<String> fields) {
        return findSuggestMatchesAnyOf(entity, query, fields, null);
    }

    /**
     * Builds the query for suggesters that will lookup the list of given fields for the given query string.
     *
     * For example:
     *  <code>findSuggestMatchesAnyOf(Product.class, UtilMisc.toList("internalName", "productName"))</code>
     *  finds all <code>Product</code> for which the "internalName" OR the "productName" matches the given <code>query</code>
     *  passed as parameter.
     *  The match is done by looking values containing the <code>query</code>, case insensitive.
     *
     * @param <T> the entity class to return
     * @param entity the entity class to return
     * @param query the string to lookup
     * @param fields the list of fields to lookup
     * @param additionalFilter a condition to restrict the possible matches
     * @return the list of entities found, or <code>null</code> if an error occurred
     */
    protected <T extends EntityInterface> List<T> findSuggestMatchesAnyOf(Class<T> entity, String query, List<String> fields, EntityCondition additionalFilter) {
        Debug.logInfo("findSuggestMatchesAnyOf: entity=" + entity.getName() + ", query=" + query + ", fields=" + fields, "");
        if (query == null || fields.isEmpty()) {
            if (additionalFilter == null) {
                return findAll(entity);
            } else {
                return findList(entity, additionalFilter);
            }
        }

        List<EntityCondition> suggestConds = new ArrayList<EntityCondition>();
        for (String field : fields) {
            suggestConds.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD(field), EntityOperator.LIKE, EntityFunction.UPPER("%" + query + "%")));
        }
        EntityCondition conditions = EntityCondition.makeCondition(suggestConds, EntityOperator.OR);

        if (additionalFilter != null) {
            conditions = EntityCondition.makeCondition(EntityOperator.AND, conditions, additionalFilter);
        }
        return findList(entity, conditions);
    }

    /**
     * Makes the display string that should be displayed in the auto-completer from the entity found.
     * @param value the entity value
     * @return the display string that should be displayed in the auto-completer
     */
    public abstract String makeSuggestDisplayedText(EntityInterface value);

    /**
     * Makes extra values to be returned in the response.
     * @param value the entity value
     * @return the Map of field -> value that should be set
     */
    public Map<String, String> makeExtraSuggestValues(EntityInterface value) {
        return null;
    }
}
