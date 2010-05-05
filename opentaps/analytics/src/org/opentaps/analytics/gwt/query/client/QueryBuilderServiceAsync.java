/*
 * Copyright (c) 2008 Open Source Strategies, Inc.
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
package org.opentaps.analytics.gwt.query.client;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * RPC service callback to read the configured filters available for each 
 * dimension and autocomplete values.
 */
public interface QueryBuilderServiceAsync {

    /**
     * Gets the list of <code>ConditionDef</code> configured for 
     * particular report.
     * @param callback the async callback
     */
    public void getAvailableConditions(String report, AsyncCallback<List<ConditionDef>> callback);

    /**
     * Gets the list of values applicable for the condition field.
     * @param conditionId a <code>String</code> value
     * @param callback the async callback
     */
    public void getConditionValues(String conditionId, AsyncCallback<List<ValueDef>> callback);

};
