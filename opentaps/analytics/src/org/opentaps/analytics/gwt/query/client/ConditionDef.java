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
 * Represents a filter condition for the query.
 */
public class ConditionDef implements IsSerializable {

    /**
     * Supported comparison operators w/ utility methods.
     */
    public static enum OPERATORS {
        LIKE,
        DONTLIKE,
        GREATER,
        GREATER_OR_EQUALS,
        LESS,
        LESS_OR_EQUALS,
        EQUALS,
        NOT_EQUALS;

        public String getOperatorName() {
            if ("LIKE".equals(toString())) {
                return "CONTAINS";
            } else if ("DONTLIKE".equals(toString())) {
                return "DOES NOT CONTAIN";
            } else if ("GREATER".equals(toString())) {
                return ">";
            } else if ("GREATER_OR_EQUALS".equals(toString())) {
                return ">=";
            } else if ("LESS".equals(toString())) {
                return "<";
            } else if ("LESS_OR_EQUALS".equals(toString())) {
                return "<=";
            } else if ("EQUALS".equals(toString())) {
                return "=";
            } else if ("NOT_EQUALS".equals(toString())) {
                return "<>";
            } else {
                return toString();
            }
        }

        public String getOperator() {
            if ("LIKE".equals(toString())) {
                return "LIKE";
            } else if ("DONTLIKE".equals(toString())) {
                return "NOT LIKE";
            } else if ("GREATER".equals(toString())) {
                return ">";
            } else if ("GREATER_OR_EQUALS".equals(toString())) {
                return ">=";
            } else if ("LESS".equals(toString())) {
                return "<";
            } else if ("LESS_OR_EQUALS".equals(toString())) {
                return "<=";
            } else if ("EQUALS".equals(toString())) {
                return "=";
            } else if ("NOT_EQUALS".equals(toString())) {
                return "<>";
            } else {
                return toString();
            }
        }
    }

    private String id;
    private Boolean selected;
    private String fieldName;
    private String label;
    private String dimension;
    private String value;
    private String javaType;
    private String operator;

    /**
     * Default constructor.
     */
    public ConditionDef() { }

    /**
     * Constructor used when reading a <code>ConditionDef</code> 
     * from configuration.
     *
     * @param id a unique identifier for the condition
     * @param selected is the condition selected for the report
     * @param fieldName the field name in the dimension table
     * @param label used for naming the condition in the UI
     * @param dimension the dimension table this condition is used for
     * @param value the current value the condition will filter for
     * @param javaType the fully qualified name of the java class the 
     * <code>value</code> represents
     */
    public ConditionDef(String id, Boolean selected, String fieldName,
            String label, String dimension, String value, String javaType) {
        setId(id);
        setSelected(selected);
        setFieldName(fieldName);
        setLabel(label);
        setDimension(dimension);
        setValue(value);
        setJavaType(javaType);
    }

    /**
     * Sets this condition identifier.
     * @param id a <code>String</code> value
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets this condition identifier.
     * @return a <code>String</code> value
     */
    public String getId() {
        return id;
    }

    /**
     * Sets if this condition has been selected by the user to be used when the report will be generated.
     * @param selected a <code>Boolean</code> value
     */
    public void setSelected(Boolean selected) {
        this.selected = selected;
    }

    /**
     * Gets if this condition has been selected by the user to be used when the report will be generated.
     * @return a <code>Boolean</code> value
     */
    public Boolean getSelected() {
        return selected;
    }

    /**
     * Sets the field name in the dimension table for this condition.
     * @param fieldName a <code>String</code> value
     */
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    /**
     * Gets the field name in the dimension table for this condition.
     * @return a <code>String</code> value
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Sets the label used in the UI for this condition.
     * @param label a <code>String</code> value
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Gets the label used in the UI for this condition.
     * @return a <code>String</code> value
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the name of dimension table used for this condition.
     * @param dimension a <code>String</code> value
     */
    public void setDimension(String dimension) {
        this.dimension = dimension;
    }

    /**
     * Gets the name of dimension table used for this condition.
     * @return a <code>String</code> value
     */
    public String getDimension() {
        return dimension;
    }

    /**
     * Sets the value that should be applied for this condition when 
     * generating the report.
     * @param value a <code>String</code> value
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets the value that should be applied for this condition when 
     * generating the report.
     * @return a <code>String</code> value
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the fully qualified java class name the value should represent
     * for this condition.
     * @param javaType a <code>String</code> value
     */
    public void setJavaType(String javaType) {
        this.javaType = javaType;
        operator = ConditionDef.OPERATORS.EQUALS.getOperatorName();
    }

    /**
     * Gets the fully qualified java class name the value should represent
     * for this condition.
     * @return a <code>String</code> value
     */
    public String getJavaType() {
        return javaType;
    }

    /**
     * Gets default expression for a condition.
     * @return a <code>String</code> value
     */
    public String getOperatorName() {
        return operator;
    }

    /**
     * Returns mapping of friendly operator name to operator in SQl notation
     * @param name
     * @return
     */
    public static String findOperator(String name) {
        OPERATORS[] values = OPERATORS.values();
        for (OPERATORS value : values) {
            if (name.equals(value.getOperatorName())) {
                return value.getOperator();
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ConditionDef: {");
        sb.append("id: ");
        sb.append(getId());
        sb.append(", selected: ");
        sb.append(getSelected());
        sb.append(", fieldName: ");
        sb.append(getFieldName());
        sb.append(", label: ");
        sb.append(getLabel());
        sb.append(", dimension: ");
        sb.append(getDimension());
        sb.append(", value: ");
        sb.append(getValue());
        sb.append(", javaType: ");
        sb.append(getJavaType());
        sb.append(", operator: ");
        sb.append(getOperatorName());
        sb.append("}");
        return sb.toString();
    }

    /**
     * Serializes this condition in an <code>Array</code>.
     * Fields are in order: <code>id</code>, <code>selected</code>,
     * <code>fieldName</code>, <code>label</code>, <code>dimension</code>,
     * <code>value</code>, <code>javaType</code>, <code>expr</code>.
     * @return an <code>Object[]</code> value
     */
    public Object[] toArray() {
        return new Object[] {
                getId(),
                getSelected(),
                getFieldName(),
                getLabel(),
                getDimension(),
                getValue(),
                getJavaType(),
                getOperatorName()
        };
    }

}
