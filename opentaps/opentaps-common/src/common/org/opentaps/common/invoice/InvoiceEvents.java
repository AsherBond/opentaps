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
package org.opentaps.common.invoice;

import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.opentaps.common.party.PartyContactHelper;
import org.opentaps.common.party.PartyNotFoundException;
import org.opentaps.common.party.PartyReader;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilConfig;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.base.entities.InvoiceAdjustment;
import org.opentaps.base.entities.InvoiceAdjustmentType;
import org.opentaps.base.entities.InvoiceTerm;
import org.opentaps.base.entities.PartyAcctgPreference;
import org.opentaps.base.entities.Payment;
import org.opentaps.base.entities.PaymentApplication;
import org.opentaps.base.entities.PaymentMethodType;
import org.opentaps.base.entities.PostalAddress;
import org.opentaps.base.entities.TermType;
import org.opentaps.domain.billing.invoice.Invoice;
import org.opentaps.domain.billing.invoice.InvoiceRepositoryInterface;
import org.opentaps.domain.organization.Organization;
import org.opentaps.domain.organization.OrganizationRepositoryInterface;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.repository.RepositoryException;

/**
 * InvoiceEvents - Java Servlet events for invoices.
 */
public final class InvoiceEvents {

    private InvoiceEvents() { }

    private static final String MODULE = InvoiceEvents.class.getName();
    /**
     * Prepare data and parameters for running invoice report.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the event response <code>String</code>
     */
    @SuppressWarnings("unchecked")
    public static String prepareInvoiceReport(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        Locale locale = UtilHttp.getLocale(request);
        TimeZone timeZone = UtilCommon.getTimeZone(request);

        String invoiceId = UtilCommon.getParameter(request, "invoiceId");
        String organizationPartyId = (String) request.getSession().getAttribute("organizationPartyId");
        if (UtilValidate.isEmpty(organizationPartyId)) {
            organizationPartyId = UtilCommon.getParameter(request, "organizationPartyId");
            if (UtilValidate.isEmpty(organizationPartyId)) {
                organizationPartyId = UtilConfig.getPropertyValue("opentaps", "organizationPartyId");
                if (UtilValidate.isEmpty(organizationPartyId)) {
                    UtilMessage.createAndLogEventError(request, "FinancialsError_CannotPrintInvoiceWoOrganizationPartyId", UtilMisc.toMap("invoiceId", invoiceId), locale, MODULE);
                }
            }
        }

        //  placeholder for report parameters
        try {
            DomainsLoader dl = new DomainsLoader(request);
            Map jasperParameters = prepareInvoiceReportParameters(dl, delegator, dispatcher, timeZone, userLogin, locale, invoiceId, organizationPartyId);
            request.setAttribute("jrParameters", jasperParameters.get("jrParameters"));
            request.setAttribute("jrDataSource", jasperParameters.get("jrDataSource"));
        } catch (GenericEntityException e) {
            UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        } catch (PartyNotFoundException e) {
            UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        } catch (InfrastructureException e) {
            UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        } catch (EntityNotFoundException e) {
            UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        } catch (RepositoryException e) {
            UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        } catch (GenericServiceException e) {
            UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        }

        return "success";
    }

    /**
     * Prepare jasper parameters for running invoice report.
     * @param dl a <code>DomainsLoader</code> value
     * @param delegator a <code>Delegator</code> value
     * @param dispatcher a <code>LocalDispatcher</code> value
     * @param timeZone a <code>TimeZone</code> value
     * @param userLogin a <code>GenericValue</code> value
     * @param locale a <code>Locale</code> value
     * @param invoiceId a <code>String</code> value
     * @param organizationPartyId a <code>String</code> value
     * @return the event response <code>String</code>
     * @throws GenericServiceException if an error occurs
     * @throws GenericEntityException if an error occurs
     * @throws PartyNotFoundException if an error occurs
     * @throws RepositoryException if an error occurs
     * @throws EntityNotFoundException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public static Map prepareInvoiceReportParameters(DomainsLoader dl, Delegator delegator, LocalDispatcher dispatcher, TimeZone timeZone, GenericValue userLogin, Locale locale, String invoiceId, String organizationPartyId) throws GenericServiceException, GenericEntityException, PartyNotFoundException, RepositoryException, EntityNotFoundException {
        Map<String, Object> parameters = FastMap.newInstance();
        //  placeholder for report parameters
        Map<String, Object> jrParameters = FastMap.newInstance();


        // prepare company information
        Map<String, Object> organizationInfo = UtilCommon.getOrganizationHeaderInfo(organizationPartyId, delegator);
        jrParameters.putAll(organizationInfo);
        PartyReader pr = new PartyReader(organizationPartyId, delegator);
        jrParameters.put("website", pr.getWebsite());
        jrParameters.put("primaryPhone", PartyContactHelper.getTelecomNumberByPurpose(organizationPartyId, "PRIMARY_PHONE", true, delegator));
        jrParameters.put("primaryFax", PartyContactHelper.getTelecomNumberByPurpose(organizationPartyId, "FAX_NUMBER", true, delegator));

        DomainsDirectory domains = dl.loadDomainsDirectory();
        InvoiceRepositoryInterface invr = domains.getBillingDomain().getInvoiceRepository();
        OrganizationRepositoryInterface ori =  domains.getOrganizationDomain().getOrganizationRepository();
        Organization organization = ori.getOrganizationById(organizationPartyId);
        PartyAcctgPreference partyAcctgPreference = organization.getPartyAcctgPreference();
        // if groupSalesTaxOnInvoicePdf equals Y, then Group all the sales invoice's sales tax items together as one line item on the sales invoice PDF
        String groupSalesTaxOnInvoicePdf = partyAcctgPreference == null ? "N" : (partyAcctgPreference.getGroupSalesTaxOnInvoicePdf() == null ? "N" : partyAcctgPreference.getGroupSalesTaxOnInvoicePdf());

        // load invoice object and put it to report parameters
        Invoice invoice = invr.getInvoiceById(invoiceId);
        jrParameters.put("invoice", invoice);

        // create invoice type indications
        boolean isReceipt = invoice.isSalesInvoice() || invoice.isInterestInvoice();
        boolean isDisbursement = invoice.isPurchaseInvoice() || invoice.isCommissionInvoice() || invoice.isReturnInvoice();
        boolean isPartner = invoice.isPartnerInvoice();
        jrParameters.put("isReceipt", Boolean.valueOf(isReceipt));
        jrParameters.put("isDisbursement", Boolean.valueOf(isDisbursement));
        jrParameters.put("isPartner", Boolean.valueOf(isPartner));

        // the billing address, which can be either the payment or billing location
        PostalAddress invoiceAddress = invoice.getBillingAddress();
        if (invoiceAddress != null) {
            jrParameters.put("invoiceAddress", invoiceAddress);
        } else {
            // get party name that appears in place of address if there is no billing address
            Map<String, Object> results = dispatcher.runSync("getPartyNameForDate", UtilMisc.toMap("partyId", (isReceipt || isPartner) ? invoice.getPartyId() : invoice.getPartyIdFrom(), "compareDate", invoice.getInvoiceDate(), "userLogin", userLogin));
            String billingPartyName = (String) results.get("fullName");
            jrParameters.put("defaultBillingPartyName", billingPartyName);
        }

        // the shipping address
        PostalAddress shippingAddress = invoice.getShippingAddress();
        if (shippingAddress != null) {
            jrParameters.put("shippingAddress", shippingAddress);
        }

        // Retrieve invoice items.  Unfortunately we can't use InvoiceItem objects for now due to
        // complicated requirements that affect items appearance in final document.
        List<Map<String, Object>> invoiceItems = InvoiceHelper.getInvoiceLinesForPresentation(delegator, invoice.getInvoiceId(), new Boolean(groupSalesTaxOnInvoicePdf.equals("Y")));
        // extraInfo & extraDetails maps mainly used by InvoiceFirstLine subreport.
        Map<String, Object> extraInfo = FastMap.newInstance();
        List<Map<String, String>> extraDetails = FastList.newInstance();

        if (UtilValidate.isNotEmpty(invoiceItems)) {
            Map<String, Object> firstLine = invoiceItems.get(0);

            if (UtilValidate.isNotEmpty(invoice.getReferenceNumber())) {
                extraInfo.put("referenceNumber", invoice.getReferenceNumber());
            }
            GenericValue orderItem = (GenericValue) firstLine.get("orderItem");
            if (UtilValidate.isNotEmpty(orderItem)) {
                extraInfo.put("orderId", orderItem.getString("orderId"));
                extraInfo.put("correspondingPoId", orderItem.getString("correspondingPoId"));
            }

            String shipmentId = (String) firstLine.get("shipmentId");
            if (UtilValidate.isNotEmpty(shipmentId)) {
                extraInfo.put("shipmentId", firstLine.get("shipmentId"));
                Timestamp createdDate = (Timestamp) firstLine.get("createdDate");
                extraInfo.put(
                        "shippedViaMessage",
                        UtilMessage.expandLabel(
                                "FinancialsShippedVia", locale, UtilMisc.toMap(
                                        "createdDate", createdDate != null ? UtilDateTime.timeStampToString(createdDate, UtilDateTime.getDateFormat(locale), timeZone, locale) : "",
                                                "carrierPartyId", firstLine.get("carrierPartyId"),
                                                "shipmentMethodTypeId", firstLine.get("shipmentMethodTypeId")
                                )
                        )
                );
            }

            // list of payment methods
            List<GenericValue> orderPaymentList = (List<GenericValue>) firstLine.get("orderPaymentList");
            if (UtilValidate.isNotEmpty(orderPaymentList)) {
                for (GenericValue payment : orderPaymentList) {
                    GenericValue paymentMethodType = payment.getRelatedOne("PaymentMethodType");
                    if (UtilValidate.isNotEmpty(paymentMethodType)) {
                        extraDetails.add(UtilMisc.<String, String>toMap("title", UtilMessage.expandLabel("FinancialsPaymentMethod", locale), "message", paymentMethodType.get("description", locale)));
                    }
                }
            }

            // Invoice terms and term types
            List<? extends InvoiceTerm> invoiceTermList = invoice.getInvoiceTerms(); //TODO: sort the list by invoiceTermId;
            for (InvoiceTerm invoiceTerm : invoiceTermList) {
                TermType termType = invoiceTerm.getTermType();
                if (UtilValidate.isNotEmpty(termType) && "FINANCIAL_TERM".equals(termType.getParentTypeId())) {
                    extraDetails.add(UtilMisc.toMap("title", UtilMessage.expandLabel("FinancialPaymentTerm", locale), "message", UtilMessage.expandLabel("TermType_" + invoiceTerm.getTermTypeId(), locale, invoiceTerm.toMap())));
                }
            }

            // dueDate
            Timestamp dueDate = invoice.getDueDate();
            if (dueDate != null) {
                extraDetails.add(UtilMisc.toMap("title", UtilMessage.expandLabel("AccountingDueDate", locale), "message", UtilDateTime.timeStampToString(dueDate, UtilDateTime.getDateFormat(locale), timeZone, locale)));
            }

            // tracking codes
            List<String> trackingCodes = (List<String>) firstLine.get("trackingCodes");
            if (UtilValidate.isNotEmpty(trackingCodes) && UtilValidate.isNotEmpty(extraInfo.get("shipmentId"))) {
                for (String trackingCode : trackingCodes) {
                    extraDetails.add(UtilMisc.toMap("title", UtilMessage.expandLabel("OpentapsTrackingCodes", locale), "message", trackingCode));
                }
            }

        }

        // prepare payment list data source for InvoicePayments subreport
        List<Map<String, Object>> paymentList = FastList.newInstance();
        List<? extends PaymentApplication> paymentAppls = invoice.getPaymentApplications();
        for (PaymentApplication paymentAppl : paymentAppls) {
            Map<String, Object> paymentLine = FastMap.newInstance();
            paymentLine.put("amountApplied", paymentAppl.getAmountApplied());
            Payment payment = paymentAppl.getPayment();
            if (payment != null) {
                paymentLine.put("effectiveDate", payment.getEffectiveDate());
                paymentLine.put("paymentRefNum", payment.getPaymentRefNum());
                PaymentMethodType paymentMethod = payment.getPaymentMethodType();
                if (paymentMethod != null) {
                    paymentLine.put("method", paymentMethod.getDescription());
                }
            }
            paymentList.add(paymentLine);
        }
        if (UtilValidate.isNotEmpty(paymentList)) {
            jrParameters.put("paymentList", new JRMapCollectionDataSource(paymentList));
        }

        // prepare adjustment list data source for InvoiceAdjustments subreport
        List<Map<String, Object>> adjustmentList = FastList.newInstance();
        List<? extends InvoiceAdjustment> invAdjustments = invoice.getInvoiceAdjustments();
        for (InvoiceAdjustment adj : invAdjustments) {
            Map<String, Object> adjLine = FastMap.newInstance();
            adjLine.putAll(adj.toMap());
            InvoiceAdjustmentType adjType = adj.getRelatedOne(InvoiceAdjustmentType.class);
            if (adjType != null) {
                adjLine.put("adjustmentType", adjType.getDescription());
            }
            adjustmentList.add(adjLine);
        }
        if (UtilValidate.isNotEmpty(adjustmentList)) {
            jrParameters.put("adjustmentList", new JRMapCollectionDataSource(adjustmentList));
        }

        // Payments section title depend on invoice type.
        if (isReceipt) {
            jrParameters.put("paymentListLabel", UtilMessage.expandLabel("AccountingPaymentsReceived", locale));
        } else if (isDisbursement) {
            jrParameters.put("paymentListLabel", UtilMessage.expandLabel("FinancialsPaymentsSent", locale));
        }

        // pass report parameters to JasperReports engine
        if (UtilValidate.isNotEmpty(extraDetails)) {
            jrParameters.put("extraDetails", new JRMapCollectionDataSource(extraDetails));
        }
        if (UtilValidate.isNotEmpty(extraInfo)) {
            jrParameters.put("firstLine", extraInfo);
        }

        JRMapCollectionDataSource jrDataSource = new JRMapCollectionDataSource(invoiceItems);
        parameters.put("jrDataSource", jrDataSource);
        parameters.put("jrParameters", jrParameters);
        return parameters;
    }


}
