/*
 * Copyright (c) Open Source Strategies, Inc.
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

import com.google.gwt.user.client.rpc.RemoteService;

/**
 * RPC service to read the configured filters available for each dimension
 * and autocomplete values.
 */
public interface QueryBuilderService extends RemoteService {

    /**
     * Gets the list of <code>ConditionDef</code> configured for
     * particular report.
     * @return the list of <code>ConditionDef</code> configured
     */
    public List<ConditionDef> getAvailableConditions(String report);

    /**
     * Gets the list of values applicable for the condition field.
     * @param conditionId a <code>String</code> value
     * @return the list of values
     */
    public List<ValueDef> getConditionValues(String conditionId);
}
