/*
 * Copyright (c) 2008-2009 Open Source Strategies, Inc.
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

import com.google.gwt.i18n.client.Messages;

/**
 * Localized strings used in the <code>QueryBuilder</code> UI.
 */
public interface QueryBuilderMessages extends Messages {

    /** Label for the Run Report button. */
    String runReport();
    /** Label for the run In Background button. */
    String runReportInBackground();

    /** Label for the output format selection, PDF. */
    String ReportOutputPDF();
    /** Label for the output format selection, XLS. */
    String ReportOutputXLS();
    /** Label for the output format selection, HTML. */
    String ReportOutputHTML();
    /** Label for the output format selection, CSV. */
    String ReportOutputCSV();
    /** Label for the output format selection, RTF. */
    String ReportOutputRTF();
    /** Label for the output format selection, ODT. */
    String ReportOutputODT();
    /** Label for the output format selection, XML. */
    String ReportOutputXML();

    String AvailableConditions();
    String SelectedConditions();

    String ConditionProductID();
    String ConditionProductName();
    String ConditionCategory();
    String ConditionBrand();
    String ConditionCustomer();
    String ConditionCompany();
    String ConditionPostalCode();
    String ConditionYear();

    String DimensionProduct();
    String DimensionCustomer();
    String DimensionDate();

    String ParameterParameterName();
    String ParameterValue();
    String ParameterName();
    String ParameterDimension();
    String ParameterOperator();

    String exprLike();
    String exprNotLike();
    String exprGreater();
    String exprGreaterEqualsTo();
    String exprLess();
    String exprLessEqualsTo();
    String exprEqualsTo();
    String exprNotEqualsTo();

    String ErrorIntegerRequired(String name);

    /** Message on RPC error. */
    String ErrorServerCommunication();
}
