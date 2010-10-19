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

import java.util.ArrayList;

/**
 * Stores the conditions as a filter value to field in the dimension to apply 
 * to the report.
 */
public class ReportConditions {

    // this is only possible escape sequences for percent & ampersand signs for 1.7.0
    private static final String PERCENT =  "%"; //TODO changed to un-escaped value. Should be tested in pentaho environment.
    private static final String AMPERSAND = "%26";

    private ArrayList<String> conditions = new ArrayList<String>();

    /**
     * Adds a condition as a SQL expression, matches any field that contains the value.
     * @param fieldName the field name to filter
     * @param value the value to filter by
     */
    public void add(String fieldName, String value, String operator) {
        String op = 
            (operator != null && operator.length() > 0) ? operator : "LIKE";
        StringBuilder sb = new StringBuilder();
        // sanitize quotes in the value string
        String valueSafe = value.replace("'", "''");
        valueSafe = valueSafe.replace("&", AMPERSAND);
        if (operator.indexOf("LIKE") != -1) {
            sb.append(fieldName).
            append((" " + op + " '")).
            append(PERCENT).
            append(valueSafe).
            append(PERCENT).
            append("'");
        } else {
            sb.append(fieldName).
            append((" " + op + " '")).
            append(valueSafe).
            append("'");
        }
        conditions.add(sb.toString());
    }

    /**
     * Adds a condition as a SQL expression, matches any field strictly
     * equal to the value.
     * @param fieldName the field name to filter
     * @param value the value to filter by
     */
    public void add(String fieldName, Integer value, String operator) {
        String op = 
            (operator != null && operator.length() > 0) ? operator : "LIKE";
        StringBuilder sb = new StringBuilder();
        sb.append(fieldName).append(" " + op + " ").append(value.toString());
        conditions.add(sb.toString());
    }

    /**
     * Gets the complete WHERE expression accounting for all added conditions.
     * @return a <code>String</code> value
     */
    public String expression() {
        if (conditions.size() == 0) {
            return null;
        }

        // build complete WHERE expression
        StringBuilder whereExpr = new StringBuilder();
        for (int i = 0; i < conditions.size(); i++) {
            if (i == 0) {
                whereExpr.append(conditions.get(i));
            } else {
                whereExpr.append(" AND ").append(conditions.get(i));
            }
        }

        return whereExpr.toString();
    }
}
