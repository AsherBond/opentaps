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

package org.opentaps.gwt.common.server.lookup;

import java.util.ArrayList;
import java.util.List;

import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
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
        if (query == null || fields.isEmpty()) {
            return findAll(entity);
        }

        List<EntityCondition> suggestConds = new ArrayList<EntityCondition>();
        for (String field : fields) {
            suggestConds.add(new EntityExpr(field, true, EntityOperator.LIKE, "%" + query + "%", true));
        }
        return findList(entity, new EntityConditionList(suggestConds, EntityOperator.OR));
    }

    /**
     * Makes the display string that should be displayed in the auto-completer from the entity found.
     * @param value the entity value
     * @return the display string that should be displayed in the auto-completer
     */
    public abstract String makeSuggestDisplayedText(EntityInterface value);
}
