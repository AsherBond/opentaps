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

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Represents a value for a condition.
 */
public class ValueDef implements IsSerializable {

    private String value;
    private String conditionId;

    /**
     * Default constructor.
     */
    public ValueDef() { }

    /**
     * Creates a new <code>ValueDef</code> instance with the given values.
     * @param conditionId a <code>String</code> value
     * @param value a <code>String</code> value
     */
    public ValueDef(String conditionId, String value) {
        this.conditionId = conditionId;
        this.value = value;
    }

    /**
     * Gets the value.
     * @return a <code>String</code> value
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value.
     * @param value a <code>String</code> value
     */
    public void setValue(String value) {
       this.value = value;
    }

    /**
     * Gets the condition identifier.
     * @return a <code>String</code> value
     */
    public String getConditionId() {
        return conditionId;
    }

    /**
     * Sets the condition identifier.
     * @param conditionId a <code>String</code> value
     */
    public void setConditionId(String conditionId) {
        this.conditionId = conditionId;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getConditionId() + " = " + getValue();
    }

}
