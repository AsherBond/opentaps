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
package com.opensourcestrategies.financials.reports;

import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import javolution.util.FastList;
import javolution.util.FastMap;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilDate;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.domain.party.Party;
import org.opentaps.domain.party.PartyRepositoryInterface;

/**
 * Commission reports.  These are dynamic snapshots that
 * serve preliminary reports that don't require a full
 * data warehouse solution.
 */
public final class CommissionReports {

    private CommissionReports() { }

    private static String MODULE = CommissionReports.class.getName();

    /**
     * A commission report with breakdown by agent, customer, PO#,
     * invoice details, payment details and commission amounts.
     */
    public static String jasperCommissionReport(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        if (session == null) {
            UtilMessage.addError(request, "OpentapsError_MissingPaginator");
            return "error";
        }

        Delegator delegator = (Delegator) request.getAttribute("delegator");
        Locale locale = UtilHttp.getLocale(request.getSession());
        TimeZone timeZone = UtilCommon.getTimeZone(request);

        // get the report constraints
        String organizationPartyId = UtilCommon.getOrganizationPartyId(request);
        if (organizationPartyId == null) {
            UtilMessage.addError(request, "OpentapsError_OrganizationNotSet");
            return "error";
        }

        // get the dates and validate (maybe we can simplify this into a UtilCommon.getTimestampParameter() which does all this)
        String fromDateStr = UtilCommon.getParameter(request, "fromDate");
        String thruDateStr = UtilCommon.getParameter(request, "thruDate");
        Timestamp fromDate = null;
        Timestamp thruDate = null;
        if (fromDateStr != null) {
            fromDate = UtilDate.toTimestamp(fromDateStr, timeZone, locale);
        }
        if (thruDateStr != null) {
            thruDate = UtilDate.toTimestamp(thruDateStr, timeZone, locale);
        }
        if (fromDateStr != null && fromDate == null) {
            UtilMessage.addFieldError(request, "fromDate", "OpentapsFieldError_BadDateFormat", UtilMisc.toMap("format", UtilDateTime.getDateFormat(locale)));
            return "error";
        }
        if (thruDateStr != null && thruDate == null) {
            UtilMessage.addFieldError(request, "thruDate", "OpentapsFieldError_BadDateFormat", UtilMisc.toMap("format", UtilDateTime.getDateFormat(locale)));
            return "error";
        }

        // build report here
        Map<String, Object> jrParameters = new FastMap<String, Object>();
        try {
            DomainsLoader dl = new DomainsLoader(request);
            DomainsDirectory dd = dl.loadDomainsDirectory();
            PartyRepositoryInterface partyRepository = dd.getPartyDomain().getPartyRepository();

            // main constraints
            List<String> orderBy = UtilMisc.toList("agentPartyId");
            List<EntityCondition> conditions = UtilMisc.<EntityCondition>toList(
                    EntityCondition.makeCondition("invoiceTypeId", EntityOperator.EQUALS, "SALES_INVOICE"),
                    EntityCondition.makeCondition("statusId", EntityOperator.IN, UtilMisc.toList("INVOICE_READY", "INVOICE_PAID", "INVOICE_CONFIRMED")),
                    EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, organizationPartyId)
            );

            // date constraint and also put in report parameters
            if (fromDate != null) {
                conditions.add(EntityCondition.makeCondition("billingDatetime", EntityOperator.GREATER_THAN_EQUAL_TO, fromDate));
                jrParameters.put("reportFromDate", fromDateStr);
            }
            if (thruDate != null) {
                conditions.add(EntityCondition.makeCondition("billingDatetime", EntityOperator.LESS_THAN, thruDate));
                jrParameters.put("reportThruDate", thruDateStr);
            }

            // organization name
            Party orgParty = partyRepository.getPartyById(organizationPartyId);
            jrParameters.put("organizationName", orgParty.getName());

            // lookup on report entity
            List<String> fields = UtilMisc.toList("origInvoiceId", "agentPartyId", "partyId");
            fields.add("amount");
            fields.add("origAmount");
            fields.add("origPaymentAmount");
            fields.add("invoiceDate");
            fields.add("billingDatetime");
            List<Map<String, Object>> report = new FastList<Map<String, Object>>();
            List<GenericValue> data = delegator.findByCondition("AgreementBillingAndInvoiceSum", EntityCondition.makeCondition(conditions, EntityOperator.AND), fields, orderBy);

            // build the report lines
            for (GenericValue row : data) {
                Map<String, Object> reportLine = new FastMap<String, Object>();
                reportLine.putAll(row.getAllFields());

                // TODO: partyId not showing up for all fields
                Party agentParty = partyRepository.getPartyById(row.getString("agentPartyId"));
                reportLine.put("agentName", agentParty.getName() + " (" + row.getString("agentPartyId") + ")");

                Party customerParty = partyRepository.getPartyById(row.getString("partyId"));
                reportLine.put("customerName", customerParty.getName() + " (" + row.getString("partyId") + ")");

                report.add(reportLine);
            }

            // all done
            JRMapCollectionDataSource datasource = new JRMapCollectionDataSource(report);
            request.setAttribute("jrDataSource", datasource);
        } catch (GeneralException e) {
            return UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        }

        request.setAttribute("jrParameters", jrParameters);
        return "success";
    }

}
