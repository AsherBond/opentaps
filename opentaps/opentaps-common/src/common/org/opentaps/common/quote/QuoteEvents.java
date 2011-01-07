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
package org.opentaps.common.quote;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.party.contact.ContactHelper;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.base.constants.ContactMechPurposeTypeConstants;
import org.opentaps.base.constants.ContactMechTypeConstants;
import org.opentaps.base.constants.ProductTypeConstants;
import org.opentaps.common.party.PartyContactHelper;
import org.opentaps.common.party.PartyReader;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;

/**
 * Common quote events such as generate quote report, etc.
 */
public final class QuoteEvents {

    private QuoteEvents() { }

    private static String MODULE = QuoteEvents.class.getName();

    /**
     * Prepare jasper parameters for running quote report.
     * @param delegator a <code>Delegator</code> value
     * @param dispatcher a <code>LocalDispatcher</code> value
     * @param userLogin a <code>GenericValue</code> value
     * @param locale a <code>Locale</code> value
     * @param quoteId a <code>String</code> value
     * @return the event response <code>String</code>
     * @exception GeneralException if an error occurs
     */
    public static Map<String, Object> prepareQuoteReportParameters(Delegator delegator, LocalDispatcher dispatcher, GenericValue userLogin, Locale locale, String quoteId) throws GeneralException {
        Map<String, Object> parameters = FastMap.newInstance();

        String organizationPartyId = null;
        //  placeholder for report parameters
        Map<String, Object> jrParameters = FastMap.<String, Object>newInstance();

        GenericValue quote = delegator.findByPrimaryKey("Quote", UtilMisc.toMap("quoteId", quoteId));
        GenericValue productStore = quote.getRelatedOne("ProductStore");
        if (productStore != null && productStore.get("payToPartyId") != null) {
            organizationPartyId = productStore.getString("payToPartyId");
        }
        GenericValue party = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", quote.getString("partyId")));

        // prepare company information
        Map<String, Object> organizationInfo = UtilCommon.getOrganizationHeaderInfo(organizationPartyId, delegator);
        GenericValue organizationAddress = (GenericValue) organizationInfo.get("organizationPostalAddress");
        jrParameters.put("organizationLogoImageUrl", organizationInfo.get("organizationLogoImageUrl"));
        jrParameters.put("organizationCompanyName", organizationInfo.get("organizationCompanyName"));
        jrParameters.put("organizationAddress1", organizationAddress.get("address1"));
        jrParameters.put("organizationAddress2", organizationAddress.get("address2"));
        jrParameters.put("organizationCity", organizationAddress.get("city"));
        jrParameters.put("organizationPostalCode", organizationAddress.get("postalCode"));
        jrParameters.put("countryName", organizationInfo.get("countryName"));
        jrParameters.put("stateProvinceAbbrv", organizationInfo.get("stateProvinceAbbrv"));
        jrParameters.put("quoteId", quoteId);
        jrParameters.put("quoteStatus", quote.getRelatedOne("StatusItem").getString("description"));
        PartyReader pr = new PartyReader(organizationPartyId, delegator);
        jrParameters.put("website", pr.getWebsite());
        jrParameters.put("email", pr.getEmail());
        jrParameters.put("primaryPhone", PartyContactHelper.getTelecomNumberByPurpose(organizationPartyId, ContactMechPurposeTypeConstants.PRIMARY_PHONE, true, delegator));
        jrParameters.put("primaryFax", PartyContactHelper.getTelecomNumberByPurpose(organizationPartyId, ContactMechPurposeTypeConstants.FAX_NUMBER, true, delegator));
        GenericValue backAccount = pr.getBackAccount();
        // get parameters of eftAccountBank and pass it to JR
        if (backAccount != null) {
            jrParameters.put("eftAccountBankName", backAccount.getString("bankName"));
            jrParameters.put("eftAccountBankRoutingNumber", backAccount.getString("routingNumber"));
            jrParameters.put("eftAccountBankAccountNumber", backAccount.getString("accountNumber"));
        }
        // get name of quote.party and pass it to JR
        Map<String, Object> quotePartyNameResult = dispatcher.runSync("getPartyNameForDate", UtilMisc.toMap("partyId", quote.get("partyId"), "compareDate", quote.get("issueDate"), "lastNameFirst", "Y", "userLogin", userLogin));
        if (ServiceUtil.isError(quotePartyNameResult) || ServiceUtil.isFailure(quotePartyNameResult)) {
            throw new GenericServiceException(ServiceUtil.getErrorMessage(quotePartyNameResult));
        }
        String quotePartyName = (String) quotePartyNameResult.get("fullName");
        jrParameters.put("quotePartyName", quotePartyName);
        jrParameters.put("issueDate", quote.getTimestamp("issueDate"));
        // get party's address information and pass it to JR
        GenericValue address = EntityUtil.getFirst((List<GenericValue>) ContactHelper.getContactMech(party, ContactMechPurposeTypeConstants.GENERAL_LOCATION, ContactMechTypeConstants.POSTAL_ADDRESS, false));
        if (address != null) {
            GenericValue toPostalAddress = address.getRelatedOne("PostalAddress");
            jrParameters.put("address1", toPostalAddress.getString("address1"));
            jrParameters.put("address2", toPostalAddress.getString("address2"));
            jrParameters.put("city", toPostalAddress.getString("city"));
            jrParameters.put("stateProvinceGeoId", toPostalAddress.getString("stateProvinceGeoId"));
            jrParameters.put("postalCode", toPostalAddress.getString("postalCode"));
        }
        jrParameters.put("quoteName", quote.getString("quoteName"));
        jrParameters.put("description", quote.getString("description"));
        jrParameters.put("currencyUomId", quote.getString("currencyUomId"));
        jrParameters.put("validFromDate", quote.getTimestamp("validFromDate"));
        jrParameters.put("validThruDate", quote.getTimestamp("validThruDate"));
        // the list which using for store product item
        List<Map<String, Object>> reportList = FastList.<Map<String, Object>>newInstance();
        // the list which using for store charge item
        List<Map<String, Object>> chargeList = FastList.<Map<String, Object>>newInstance();
        // retrieve quote item info and pass it to JR, add as report data
        List<GenericValue> quoteItems = quote.getRelated("QuoteItem", UtilMisc.toList("quoteItemSeqId"));
        for (GenericValue quoteItem : quoteItems) {
            GenericValue product = quoteItem.getRelatedOne("Product");
            if (quoteItem.getBigDecimal("quantity") != null && quoteItem.getBigDecimal("quoteUnitPrice") != null) {
                Map<String, Object> reportLine = FastMap.<String, Object>newInstance();
                reportLine.put("quoteItemSeqId", quoteItem.getString("quoteItemSeqId"));
                reportLine.put("productId", quoteItem.getString("productId"));
                reportLine.put("productName", quoteItem.getString("description"));
                reportLine.put("itemDescription", quoteItem.getString("description"));
                reportLine.put("comments", quoteItem.getString("comments"));
                BigDecimal quoteItemAmount = null;
                // set quoteItemAmount=quantity * quoteUnitPrice when both quantity and quoteUnitPrice not null
                if (quoteItem.getBigDecimal("quoteUnitPrice") != null && quoteItem.getBigDecimal("quoteUnitPrice") != null) {
                    quoteItemAmount = quoteItem.getBigDecimal("quantity").multiply(quoteItem.getBigDecimal("quoteUnitPrice"));
                }
                reportLine.put("quantity", quoteItem.getBigDecimal("quantity"));
                reportLine.put("quoteUnitPrice", quoteItem.getBigDecimal("quoteUnitPrice"));
                reportLine.put("quoteItemAmount", quoteItemAmount);
                reportLine.put("isQuoteItem", new Boolean(true));
                // put it into report data collection
                if (product == null || !ProductTypeConstants.Service.SERVICE.equals(product.getString("productTypeId"))) {
                    // add as report item
                    reportList.add(reportLine);
                } else {
                    // if is SERVICE, then put it into chargeList
                    chargeList.add(reportLine);
                }
            } else {
                List<GenericValue> quoteItemOptions = quoteItem.getRelated("QuoteItemOption", UtilMisc.toList("quoteItemOptionSeqId"));
                int optionSeq = 1;
                for (GenericValue quoteItemOption : quoteItemOptions) {
                    Map<String, Object> reportOptionLine = FastMap.<String, Object>newInstance();
                    reportOptionLine.put("quoteItemSeqId", quoteItem.getString("quoteItemSeqId"));
                    reportOptionLine.put("productId", quoteItem.getString("productId"));
                    reportOptionLine.put("productName", quoteItem.getString("description"));
                    reportOptionLine.put("itemDescription", "" + optionSeq);
                    reportOptionLine.put("comments", quoteItem.getString("comments"));
                    optionSeq++;
                    BigDecimal quoteItemAmount = null;
                    // set quoteItemAmount=quantity * quoteUnitPrice when both quantity and quoteUnitPrice not null
                    if (quoteItemOption.getBigDecimal("quantity") != null && quoteItemOption.getBigDecimal("quoteUnitPrice") != null) {
                        quoteItemAmount = quoteItemOption.getBigDecimal("quantity").multiply(quoteItemOption.getBigDecimal("quoteUnitPrice"));
                    }
                    reportOptionLine.put("quantity", quoteItemOption.getBigDecimal("quantity"));
                    reportOptionLine.put("quoteUnitPrice", quoteItemOption.getBigDecimal("quoteUnitPrice"));
                    reportOptionLine.put("quoteItemAmount", quoteItemAmount);
                    reportOptionLine.put("isQuoteItem", new Boolean(false));
                    // put it into report data collection
                    if (product == null || !"SERVICE".equals(product.getString("productTypeId"))) {
                        // add as report item
                        reportList.add(reportOptionLine);
                    } else {
                        // if is SERVICE, then put it into chargeList
                        chargeList.add(reportOptionLine);
                    }
                }
            }
        }
        Debug.logVerbose("There have " + chargeList.size() + " SERVICE item", MODULE);
        Debug.logVerbose("There have " + reportList.size() + " PRODUCT item", MODULE);
        if (UtilValidate.isNotEmpty(chargeList)) {
            // if chargeData not empty, then pass it to JR
            jrParameters.put("chargeList", new JRMapCollectionDataSource(chargeList));
        }
        JRMapCollectionDataSource jrDataSource = new JRMapCollectionDataSource(reportList);
        parameters.put("jrDataSource", jrDataSource);
        parameters.put("jrParameters", jrParameters);
        return parameters;
    }

    /**
     * Prepare data and parameters for running quote report.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the event response <code>String</code>
     */
    public static String prepareQuoteReport(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        Locale locale = UtilHttp.getLocale(request);
        String quoteId = UtilCommon.getParameter(request, "quoteId");

        try {
            // get parameter for jasper
            Map<String, Object> jasperParameters = prepareQuoteReportParameters(delegator, dispatcher, userLogin, locale, quoteId);
            request.setAttribute("jrParameters", jasperParameters.get("jrParameters"));
            request.setAttribute("jrDataSource", jasperParameters.get("jrDataSource"));

        } catch (GeneralException e) {
            UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        }

        return "success";
    }

}
