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

package com.opensourcestrategies.financials.accounts;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import javolution.util.FastMap;
import javolution.util.FastSet;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.collections.ResourceBundleMapWrapper;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.base.entities.PostalAddress;
import org.opentaps.base.entities.TelecomNumber;
import org.opentaps.domain.organization.Organization;
import org.opentaps.domain.organization.OrganizationRepositoryInterface;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.repository.RepositoryException;

public class AccountsEvents {

    public static final String module = AccountsEvents.class.getName();

    /**
     <ul type="circle">
         <lh>This Customer Statement event deals with the following information:</lh>
         <li>The date of the statement (asOfDate from request parameters).
         <li>The organizationPartyId from the session
         <li>Logo
         <li>List of partyId, the customers for whom statements are being generated.
     </ul>

     <ol>
         <lh>For each customer we obtain,</lh>
         <li>List of open invoices with their open amount
         <li>All received or completed payments whose effectiveDate is within 30 days of the asOfDate and are applied to invoices, open or closed.
         <li>Any closed invoices from the above payments (for reference)
     </ol>

     <ol>
         <lh>We also calculate,</lh>
            <li>Age date for each invoice, which is the days the invoice is past due
            <li>How much is past due 120 days, 90 days, 60 days, 30 days, and how much is current
            <li>Sum of all open amounts
     </ol>
     */
    @SuppressWarnings("unchecked")
    public static String customerStatementPDF(HttpServletRequest request, HttpServletResponse response) {


        HttpSession session = request.getSession();
        if (session == null) {
            UtilMessage.addError(request, "OpentapsError_MissingPaginator");
            return "error";
        }
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        Locale locale = UtilHttp.getLocale(request.getSession());
        TimeZone timeZone = UtilCommon.getTimeZone(request);
        ResourceBundleMapWrapper uiLabelMap = UtilMessage.getUiLabels(locale);

        String organizationPartyId = UtilCommon.getOrganizationPartyId(request);
        if (organizationPartyId == null) {
            UtilMessage.addError(request, "OpentapsError_OrganizationNotSet");
            return "error";
        }

        // statementPeriod is the interval in days for aging report: 30 means that the report will be 30, 60, 90 days
        int statementPeriod;
        String statementPeriodStr = UtilCommon.getParameter(request, "statementPeriod");
        if (statementPeriodStr == null) {
            statementPeriodStr = "30";
        }
        try {
            statementPeriod = Integer.parseInt(statementPeriodStr);
        } catch (NumberFormatException e) {
            UtilMessage.addFieldError(request, "statementPeriod", "OpentapsFieldError_BadDoubleFormat");
            return "error";
        }

        // the statement will be as of this date
        String asOfDateString = UtilCommon.getParameter(request, "asOfDate");
        Timestamp asOfDate = null;
        try {
            asOfDate = UtilDateTime.getDayStart(UtilDateTime.stringToTimeStamp(asOfDateString, UtilDateTime.getDateFormat(locale), timeZone, locale), timeZone, locale);
        } catch (ParseException e) {
            if (UtilValidate.isNotEmpty(asOfDateString)) {
                UtilMessage.addError(request, "FinancialsError_IllegalDateFormat", UtilMisc.toMap("fieldName", uiLabelMap.get("CommonFromDate")));
            }
            return "error";
        }

        try {
            // whether we're using aging date or invoice date as basis for statement
            boolean useAgingDate = "true".equals(UtilCommon.getParameter(request, "useAgingDate")) ? true : false;

            Map<String, Object> jrParameters = FastMap.newInstance();
            DomainsLoader loader = new DomainsLoader(request);
            OrganizationRepositoryInterface orgRepository = loader.loadDomainsDirectory().getOrganizationDomain().getOrganizationRepository();

            // get the partyIds using a handy utility function
            Collection<Map<String, Object>> params = UtilHttp.parseMultiFormData(UtilHttp.getParameterMap(request));
            Set<String> partyIds = FastSet.newInstance();
            for (Map param : params) {
                partyIds.add((String) param.get("partyId"));
            }

            // special map to store per party specific amounts and data.  The map is flat and the keys are partyId + parameterName
            Map<String, Object> partyData = FastMap.newInstance();

            // create a jrDataSource object which jasper reports will consume to make the report
            request.setAttribute("jrDataSource", 
                    new JRMapCollectionDataSource(
                            AccountsHelper.customerStatement(loader, organizationPartyId, partyIds, asOfDate, statementPeriod, useAgingDate, partyData, locale, timeZone)
                    )
            );

            // set up parameters for the jrxml
            jrParameters.put("as_of_date", asOfDate);
            jrParameters.put("party_data", partyData);
            jrParameters.put("statement_period", Integer.valueOf(statementPeriod));

            // get the organization data from the organization domain (which extends party)
            Organization org = orgRepository.getOrganizationById(organizationPartyId);
            TelecomNumber primaryPhone = org.getPrimaryPhone();
            TelecomNumber faxNumber = org.getFaxNumber();
            if (primaryPhone != null) {
                StringBuffer sb = new StringBuffer();
                sb.append("(").append(primaryPhone.getAreaCode()).append(") ");
                sb.append(primaryPhone.getContactNumber());
                jrParameters.put("org_phone", sb.toString());
            }
            if (primaryPhone != null) {
                StringBuffer sb = new StringBuffer();
                sb.append("(").append(faxNumber.getAreaCode()).append(") ");
                sb.append(faxNumber.getContactNumber());
                jrParameters.put("org_fax", sb.toString());
            }
            PostalAddress address = org.getPrimaryAddress();
            if (address != null) {
                jrParameters.put("org_address1", address.getAddress1());
                jrParameters.put("org_city", address.getCity());
                jrParameters.put("org_postal_code", address.getPostalCode());
                jrParameters.put("org_state_province_abbrv", address.getStateProvinceGeoId());
            }
            jrParameters.put("logo_url", org.getLogoImageUrl());

            request.setAttribute("jrParameters", jrParameters);


        } catch (GenericEntityException e) {
            UtilMessage.createAndLogEventError(request, e, locale, module);
        } catch (RepositoryException e) {
            UtilMessage.createAndLogEventError(request, e, locale, module);
        } catch (EntityNotFoundException e) {
            UtilMessage.createAndLogEventError(request, e, locale, module);
        } catch (InfrastructureException e) {
            UtilMessage.createAndLogEventError(request, e, locale, module);
        }

        return "success";
    }

}
