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
/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/

/* This file has been modified by Open Source Strategies, Inc. */
package org.opentaps.common.content;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;

import javolution.util.FastMap;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.base.entities.ReportRegistry;
import org.opentaps.common.invoice.InvoiceEvents;
import org.opentaps.common.order.OrderEvents;
import org.opentaps.common.party.PartyHelper;
import org.opentaps.common.party.PartyNotFoundException;
import org.opentaps.common.party.PartyReader;
import org.opentaps.common.quote.QuoteEvents;
import org.opentaps.common.reporting.UtilReports;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.domain.billing.invoice.Invoice;
import org.opentaps.domain.billing.invoice.InvoiceRepositoryInterface;
import org.opentaps.domain.order.Order;
import org.opentaps.domain.order.OrderRepositoryInterface;
import org.opentaps.foundation.entity.hibernate.Query;
import org.opentaps.foundation.entity.hibernate.Session;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.infrastructure.User;


/**
 * Common email services such as send quote email, etc.
 *
 */
public final class EmailServices {

    private EmailServices() { }

    private static String MODULE = EmailServices.class.getName();
    public static List<String> TEAM_MEMBER_ROLES = UtilMisc.toList("ACCOUNT_MANAGER", "ACCOUNT_REP", "CUST_SERVICE_REP");
    public static List<String> CLIENT_PARTY_ROLES = UtilMisc.toList("ACCOUNT", "CONTACT", "PROSPECT", "PARTNER");
    public static String QUOTE_JRXML_REPORT_ID = "SALESQUOTE";
    public static String SALES_ORDER_JRXML_REPORT_ID = "SALESORDER";
    public static String PURCHASING_ORDER_JRXML_REPORT_ID = "PRUCHORDER";
    public static String INVOICE_JRXML_REPORT_ID = "FININVOICE";
    public static String QUOTE_ERROR_LABEL = "OpentapsError_CreateQuoteEmailFail";
    public static String SALES_ORDER_ERROR_LABEL = "OpentapsError_CreateSalesOrderEmailFail";
    public static String PURCHASING_ORDER_ERROR_LABEL = "OpentapsError_CreatePurchasingOrderEmailFail";
    public static String INVOICE_ERROR_LABEL = "OpentapsError_CreateInvoiceEmailFail";


    /**
     * get jrxml location by reportId.
     * @param dispatcher a <code>LocalDispatcher</code> instance
     * @param reportId a <code>String</code> value
     * @throws InfrastructureException if an error occurs
     * @throws SQLException if an error occurs
     * @return jrxml location
     */
    @SuppressWarnings("unchecked")
    public static String getJrxmlLocation(LocalDispatcher dispatcher, String reportId) throws InfrastructureException, SQLException {
        // open a session
        Infrastructure infrastructure = new Infrastructure(dispatcher);
        Session sess = infrastructure.getSession();
        String hql = "from ReportRegistry eo where eo.id.reportId = :reportId";
        Query query = sess.createQuery(hql);
        query.setString("reportId", reportId);
        List<ReportRegistry> list = query.list();
        if (list.size() == 0) {
            return null;
        }
        ReportRegistry reportRegistry = list.get(0);
        return reportRegistry.getReportLocation();
    }

    /**
     * create pend email for send quote.
     * @param dctx a <code>DispatchContext</code> instance
     * @param context a <code>Map</code> instance
     * @return a <code>Map</code> instance
     */
    @SuppressWarnings("unchecked")
    public static Map prepareQuoteEmail(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        String quoteId = (String) context.get("quoteId");
        String sendTo = (String) context.get("sendTo");
        String subject = (String) context.get("subject");
        String content = (String) context.get("content");
        // the file name of output pdf
        String reportName = "quote_" + quoteId + ".pdf";
        String jrxml = null;
        try {
            jrxml = getJrxmlLocation(dispatcher, QUOTE_JRXML_REPORT_ID);
        } catch (InfrastructureException e) {
            Debug.logError(e, "Problem getting jrxml location", MODULE);
        } catch (SQLException e) {
            Debug.logError(e, "Problem getting jrxml location", MODULE);
        }
        if (jrxml == null) {
            return ServiceUtil.returnFailure(UtilMessage.expandLabel("OpentapsError_ReportNotFound", locale, UtilMisc.toMap("location", QUOTE_JRXML_REPORT_ID)));
        }

        // get the quote and store
        GenericValue quote = null;
        try {
            quote = delegator.findByPrimaryKey("Quote", UtilMisc.toMap("quoteId", quoteId));
        } catch (GenericEntityException e) {
            Debug.logError(e, "Problem getting Quote", MODULE);
        }

        if (quote == null) {
            return ServiceUtil.returnFailure(UtilMessage.expandLabel("OpentapsError_QuoteNotFound", locale, UtilMisc.toMap("quoteId", quoteId)));
        }
        try {
            // generate pdf by jasper export
            subject = UtilValidate.isEmpty(subject) ? UtilMessage.expandLabel("OpentapsQuoteEmailSubject", locale, UtilMisc.toMap("quoteId", quoteId)) : subject;
            Map jasperParameters = QuoteEvents.prepareQuoteReportParameters(delegator, dispatcher, userLogin, locale, quoteId);
            Map<String, Object> jrParameters = (Map<String, Object>) jasperParameters.get("jrParameters");
            JRMapCollectionDataSource jrDataSource = (JRMapCollectionDataSource) jasperParameters.get("jrDataSource");
            Map parameters = createPendEmail(dctx, context, jrxml, reportName, jrParameters, jrDataSource, quote.getString("partyId"), sendTo, subject, content);
            Map result = ServiceUtil.returnSuccess();
            result.putAll(parameters);
            return result;
        } catch (GeneralException e) {
            return UtilMessage.createAndLogServiceError(e, QUOTE_ERROR_LABEL, locale, MODULE);
        } catch (JRException e) {
            return UtilMessage.createAndLogServiceError(e, QUOTE_ERROR_LABEL, locale, MODULE);
        } catch (IOException e) {
            return UtilMessage.createAndLogServiceError(e, QUOTE_ERROR_LABEL, locale, MODULE);
        }
    }

    /**
     * create pend email for send quote.
     * @param dctx a <code>DispatchContext</code> instance
     * @param context a <code>Map</code> instance
     * @return a <code>Map</code> instance
     */
    @SuppressWarnings("unchecked")
    public static Map prepareInvoiceEmail(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        String invoiceId = (String) context.get("invoiceId");
        String sendTo = (String) context.get("sendTo");
        String subject = (String) context.get("subject");
        String content = (String) context.get("content");
        TimeZone timeZone = UtilCommon.getTimeZone(context);
        // get jrxml report location
        String jrxml = null;
        try {
            jrxml = getJrxmlLocation(dispatcher, INVOICE_JRXML_REPORT_ID);
        } catch (InfrastructureException e) {
            Debug.logError(e, "Problem getting jrxml location", MODULE);
        } catch (SQLException e) {
            Debug.logError(e, "Problem getting jrxml location", MODULE);
        }
        if (jrxml == null) {
            return ServiceUtil.returnFailure(UtilMessage.expandLabel("OpentapsError_ReportNotFound", locale, UtilMisc.toMap("location", INVOICE_JRXML_REPORT_ID)));
        }
        // the file name of output pdf
        String reportName = "invoice_" + invoiceId + ".pdf";

        try {
            // generate pdf by jasper export
            subject = UtilValidate.isEmpty(subject) ? UtilMessage.expandLabel("OpentapsInvoiceEmailSubject", locale, UtilMisc.toMap("invoiceId", invoiceId)) : subject;
            DomainsLoader dl = new DomainsLoader(new Infrastructure(dispatcher), new User(userLogin));
            DomainsDirectory domains = dl.loadDomainsDirectory();
            InvoiceRepositoryInterface invr = domains.getBillingDomain().getInvoiceRepository();
            // load invoice object and put it to report parameters
            Invoice invoice = invr.getInvoiceById(invoiceId);
            if (invoice == null) {
                return ServiceUtil.returnFailure(UtilMessage.expandLabel("OpentapsError_InvoiceNotFound", locale, UtilMisc.toMap("invoiceId", invoiceId)));
            }
            String organizationPartyId = invoice.isSalesInvoice() ? invoice.getPartyIdFrom() : invoice.getPartyId();
            Map jasperParameters = InvoiceEvents.prepareInvoiceReportParameters(dl, delegator, dispatcher, timeZone, userLogin, locale, invoiceId, organizationPartyId);
            Map<String, Object> jrParameters = (Map<String, Object>) jasperParameters.get("jrParameters");
            JRMapCollectionDataSource jrDataSource = (JRMapCollectionDataSource) jasperParameters.get("jrDataSource");
            Map parameters = createPendEmail(dctx, context, jrxml, reportName, jrParameters, jrDataSource, invoice.getString("partyId"), sendTo, subject, content);
            Map result = ServiceUtil.returnSuccess();
            result.putAll(parameters);
            return result;
        } catch (GeneralException e) {
            Debug.logError(e, MODULE);
            return UtilMessage.createAndLogServiceError(e, INVOICE_ERROR_LABEL, locale, MODULE);
        } catch (IOException e) {
            Debug.logError(e, MODULE);
            return UtilMessage.createAndLogServiceError(e, INVOICE_ERROR_LABEL, locale, MODULE);
        } catch (JRException e) {
            Debug.logError(e, MODULE);
            return UtilMessage.createAndLogServiceError(e, INVOICE_ERROR_LABEL, locale, MODULE);
        }
    }

    /**
     * create pend email for send sales order.
     * @param dctx a <code>DispatchContext</code> instance
     * @param context a <code>Map</code> instance
     * @return a <code>Map</code> instance
     */
    @SuppressWarnings("unchecked")
    public static Map prepareSalesOrderEmail(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        String orderId = (String) context.get("orderId");
        String sendTo = (String) context.get("sendTo");
        String subject = (String) context.get("subject");
        String content = (String) context.get("content");
        boolean withAttachment = !"Y".equals(context.get("skipAttachment"));

        // the file name of output pdf
        String reportName = "order_" + orderId + ".pdf";
        // get jrxml report location
        String jrxml = null;
        if (withAttachment) {
            try {
                jrxml = getJrxmlLocation(dispatcher, SALES_ORDER_JRXML_REPORT_ID);
            } catch (InfrastructureException e) {
                Debug.logError(e, "Problem getting jrxml location", MODULE);
            } catch (SQLException e) {
                Debug.logError(e, "Problem getting jrxml location", MODULE);
            }
            if (jrxml == null) {
                return ServiceUtil.returnFailure(UtilMessage.expandLabel("OpentapsError_ReportNotFound", locale, UtilMisc.toMap("location", SALES_ORDER_JRXML_REPORT_ID)));
            }
        }

        // get the quote and store
        try {
            DomainsLoader dl = new DomainsLoader(new Infrastructure(dispatcher), new User(userLogin));
            DomainsDirectory domains = dl.loadDomainsDirectory();
            OrderRepositoryInterface orderRepository = domains.getOrderDomain().getOrderRepository();
            // load order object and put it to report parameters
            Order order = orderRepository.getOrderById(orderId);
            String organizationPartyId = order.getBillFromPartyId();
            String partyId = order.getBillToPartyId();
            if (order == null) {
                return ServiceUtil.returnFailure(UtilMessage.expandLabel("OpentapsError_OrderNotFound", locale, UtilMisc.toMap("orderId", orderId)));
            }

            // generate pdf by jasper export
            subject = UtilValidate.isEmpty(subject) ? UtilMessage.expandLabel("OpentapsSalesOrderEmailSubject", locale, UtilMisc.toMap("orderId", orderId)) : subject;
            Map jasperParameters = OrderEvents.prepareOrderReportParameters(dl, delegator, dispatcher, userLogin, locale, orderId, organizationPartyId);
            Map<String, Object> jrParameters = (Map<String, Object>) jasperParameters.get("jrParameters");
            JRMapCollectionDataSource jrDataSource = (JRMapCollectionDataSource) jasperParameters.get("jrDataSource");
            Map parameters = createPendEmail(dctx, context, jrxml, reportName, jrParameters, jrDataSource, partyId, sendTo, subject, content, withAttachment);
            // associate work effort with order.
            String workEffortId = (String) parameters.get("workEffortId");
            associateWorkEffortAndOrder(dispatcher, delegator, userLogin, orderId, workEffortId);
            Map result = ServiceUtil.returnSuccess();
            result.putAll(parameters);
            return result;
        } catch (GeneralException e) {
            Debug.logError(e, MODULE);
            return UtilMessage.createAndLogServiceError(e, SALES_ORDER_ERROR_LABEL, locale, MODULE);
        } catch (IOException e) {
            Debug.logError(e, MODULE);
            return UtilMessage.createAndLogServiceError(e, SALES_ORDER_ERROR_LABEL, locale, MODULE);
        } catch (JRException e) {
            Debug.logError(e, MODULE);
            return UtilMessage.createAndLogServiceError(e, SALES_ORDER_ERROR_LABEL, locale, MODULE);
        }
    }

    /**
     * create pend email for send purchasing order.
     * @param dctx a <code>DispatchContext</code> instance
     * @param context a <code>Map</code> instance
     * @return a <code>Map</code> instance
     */
    @SuppressWarnings("unchecked")
    public static Map preparePurchasingOrderEmail(DispatchContext dctx, Map context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        String orderId = (String) context.get("orderId");
        String subject = (String) context.get("subject");
        String content = (String) context.get("content");
        // the file name of output pdf
        String reportName = "order_" + orderId + ".pdf";
        // get jrxml report location
        String jrxml = null;
        String sendTo = (String) context.get("sendTo");
        try {
            jrxml = getJrxmlLocation(dispatcher, PURCHASING_ORDER_JRXML_REPORT_ID);
        } catch (InfrastructureException e) {
            Debug.logError(e, "Problem getting jrxml location", MODULE);
        } catch (SQLException e) {
            Debug.logError(e, "Problem getting jrxml location", MODULE);
        }
        if (jrxml == null) {
            return ServiceUtil.returnFailure(UtilMessage.expandLabel("OpentapsError_ReportNotFound", locale, UtilMisc.toMap("location", PURCHASING_ORDER_JRXML_REPORT_ID)));
        }

        // get the quote and store
        try {
            DomainsLoader dl = new DomainsLoader(new Infrastructure(dispatcher), new User(userLogin));
            DomainsDirectory domains = dl.loadDomainsDirectory();
            OrderRepositoryInterface orderRepository = domains.getOrderDomain().getOrderRepository();
            // load order object and put it to report parameters
            Order order = orderRepository.getOrderById(orderId);
            String organizationPartyId = order.getBillToPartyId();
            String partyId = order.getBillFromPartyId();
            if (order == null) {
                return ServiceUtil.returnFailure(UtilMessage.expandLabel("OpentapsError_OrderNotFound", locale, UtilMisc.toMap("orderId", orderId)));
            }

            // generate pdf by jasper export
            subject = UtilValidate.isEmpty(subject) ? UtilMessage.expandLabel("OpentapsPurchaseOrderEmailSubject", locale, UtilMisc.toMap("orderId", orderId)) : subject;
            Map jasperParameters = OrderEvents.prepareOrderReportParameters(dl, delegator, dispatcher, userLogin, locale, orderId, organizationPartyId);
            Map<String, Object> jrParameters = (Map<String, Object>) jasperParameters.get("jrParameters");
            JRMapCollectionDataSource jrDataSource = (JRMapCollectionDataSource) jasperParameters.get("jrDataSource");
            Map parameters = createPendEmail(dctx, context, jrxml, reportName, jrParameters, jrDataSource, partyId, sendTo, subject, content);
            // associate work effort with order.
            String workEffortId = (String) parameters.get("workEffortId");
            associateWorkEffortAndOrder(dispatcher, delegator, userLogin, orderId, workEffortId);
            Map result = ServiceUtil.returnSuccess();
            result.putAll(parameters);
            return result;
        } catch (GeneralException e) {
            Debug.logError(e, MODULE);
            return UtilMessage.createAndLogServiceError(e, PURCHASING_ORDER_ERROR_LABEL, locale, MODULE);
        } catch (IOException e) {
            Debug.logError(e, MODULE);
            return UtilMessage.createAndLogServiceError(e, PURCHASING_ORDER_ERROR_LABEL, locale, MODULE);
        } catch (JRException e) {
            Debug.logError(e, MODULE);
            return UtilMessage.createAndLogServiceError(e, PURCHASING_ORDER_ERROR_LABEL, locale, MODULE);
        }
    }

    /**
     * associate work effort with order.
     * @param dispatcher a <code>LocalDispatcher</code> instance
     * @param delegator a <code>Delegator</code> instance
     * @param userLogin a <code>GenericValue</code> instance
     * @param orderId id of the Order
     * @param workEffortId id of the WorkEffort
     * @throws GenericServiceException if an error occurs
     * @throws GenericEntityException if an error occurs
     */
    private static void associateWorkEffortAndOrder(LocalDispatcher dispatcher, Delegator delegator, GenericValue userLogin, String orderId, String workEffortId) throws GenericEntityException, GenericServiceException {
        // Associate the work effort any orders
        if (UtilValidate.isNotEmpty(orderId)) {
            GenericValue orderHeaderWorkEffort = delegator.findByPrimaryKey("OrderHeaderWorkEffort", UtilMisc.toMap("orderId", orderId, "workEffortId", workEffortId));
            if (UtilValidate.isEmpty(orderHeaderWorkEffort)) {
                dispatcher.runSync("createOrderHeaderWorkEffort", UtilMisc.toMap("orderId", orderId, "workEffortId", workEffortId, "userLogin", userLogin));
            }
        }
    }

    /**
     * create pend email for send jasper pdf.
     * @param dctx a <code>DispatchContext</code> instance
     * @param context a <code>Map</code> instance
     * @param jrxml japser source file
     * @param reportName japser report name
     * @param jrParameters a <code>Map<String, Object></code> instance
     * @param jrDataSource a <code>JRMapCollectionDataSource</code> instance
     * @param toPartyId partyId of receiver
     * @param sendTo target email, optional
     * @param subject subject of email
     * @param content content of email
     * @return workEffort Id
     * @throws GenericServiceException if an error occurs
     * @throws GenericEntityException if an error occurs
     * @throws JRException if an error occurs
     * @throws IOException if an error occurs
     * @throws PartyNotFoundException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public static Map createPendEmail(DispatchContext dctx, Map context, String jrxml, String reportName, Map<String, Object> jrParameters, JRMapCollectionDataSource jrDataSource, String toPartyId, String sendTo, String subject, String content) throws GenericEntityException, GenericServiceException, JRException, IOException, PartyNotFoundException {
        return createPendEmail(dctx, context, jrxml, reportName, jrParameters, jrDataSource, toPartyId, sendTo, subject, content, true);
    }

    /**
     * create pend email for send jasper pdf.
     * @param dctx a <code>DispatchContext</code> instance
     * @param context a <code>Map</code> instance
     * @param jrxml japser source file
     * @param reportName japser report name
     * @param jrParameters a <code>Map<String, Object></code> instance
     * @param jrDataSource a <code>JRMapCollectionDataSource</code> instance
     * @param toPartyId partyId of receiver
     * @param sendTo target email, optional
     * @param subject subject of email
     * @param content content of email
     * @param withAttachment flag to indicate the attachment should be set
     * @return workEffort Id
     * @throws GenericServiceException if an error occurs
     * @throws GenericEntityException if an error occurs
     * @throws JRException if an error occurs
     * @throws IOException if an error occurs
     * @throws PartyNotFoundException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public static Map createPendEmail(DispatchContext dctx, Map context, String jrxml, String reportName, Map<String, Object> jrParameters, JRMapCollectionDataSource jrDataSource, String toPartyId, String sendTo, String subject, String content, boolean withAttachment) throws GenericEntityException, GenericServiceException, JRException, IOException, PartyNotFoundException {
        Map<String, Object> parameters = FastMap.newInstance();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        String toEmail = (sendTo == null || sendTo.equals("")) ? "" : sendTo;

        // generate pdf report
        if (withAttachment) {
            String author = org.ofbiz.party.party.PartyHelper.getPartyName(delegator, userLogin.getString("partyId"), false);
            UtilReports.generatePdf(jrParameters, jrDataSource, locale, jrxml, UtilReports.ContentType.PDF, reportName, author);
            GenericValue party = delegator.findByPrimaryKey("Party", UtilMisc.toMap("partyId", toPartyId));
            if (UtilValidate.isEmpty(toEmail)) {
                PartyReader partyReader = new PartyReader(party);
                toEmail = partyReader.getEmail();
                if (UtilValidate.isEmpty(toEmail)) {
                    throw new GenericServiceException("Cannot find any email address for [" + party.getString("partyId") + "], please create it first.");
                }
            }
        }

        //create a CommunicationEvent by service
        String serviceName = "createCommunicationEvent";
        ModelService service = dctx.getModelService(serviceName);
        Map input = service.makeValid(context, "IN");

        // Retrieve, validate and parse the To addresses (assumed to be comma-delimited)
        String validToAddresses = null;
        Set toAddresses = getValidEmailAddressesFromString(toEmail, false);
        if (!UtilValidate.isEmpty(toAddresses)) {
            validToAddresses = StringUtil.join(UtilMisc.toList(toAddresses), ",");
            input.put("toString", validToAddresses);
        }

        // Search for contactMechIdTo using the passed in To email addresses - use the first found
        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                         EntityCondition.makeCondition("infoString", EntityOperator.IN, UtilMisc.toList(toAddresses)),
                         EntityCondition.makeCondition("partyId", toPartyId));

        GenericValue partyContactMechTo = EntityUtil.getFirst(EntityUtil.filterByDate(delegator.findByCondition("PartyAndContactMech", conditions, null, null)));
        if (UtilValidate.isNotEmpty(partyContactMechTo)) {
            input.put("contactMechIdTo", partyContactMechTo.getString("contactMechId"));
            input.put("partyIdTo", partyContactMechTo.getString("partyId"));
            input.put("roleTypeIdTo", partyContactMechTo.getString("roleTypeId"));
        }

        // create a PENDING CommunicationEvent
        input.put("entryDate", UtilDateTime.nowTimestamp());
        input.put("contactMechTypeId", "EMAIL_ADDRESS");
        input.put("communicationEventTypeId", "EMAIL_COMMUNICATION");
        input.put("statusId", "COM_PENDING");
        input.put("subject", subject);
        input.put("partyIdFrom", userLogin.getString("partyId"));
        input.put("roleTypeIdFrom", PartyHelper.getFirstValidRoleTypeId(userLogin.getString("partyId"), TEAM_MEMBER_ROLES, delegator));
        if (UtilValidate.isNotEmpty(content)) {
            input.put("content", content);
        }
        Map serviceResults = dispatcher.runSync(serviceName, input);

        if (ServiceUtil.isError(serviceResults)) {
            throw new GenericServiceException(ServiceUtil.getErrorMessage(serviceResults));
        }

        // get the communication event id if the common event was created
        String communicationEventId = (String) serviceResults.get("communicationEventId");
        parameters.put("communicationEventId", communicationEventId);

        if (withAttachment) {
            File pdfFile = new File(UtilReports.OUT_PATH + reportName);
            ByteBuffer uploadedFile = ByteBuffer.wrap(getBytesFromFile(pdfFile));
            // Populate the context for the DataResource/Content/CommEventContentAssoc creation service
            input = new HashMap();
            input.put("userLogin", userLogin);
            input.put("contentName", reportName);
            input.put("uploadedFile", uploadedFile);
            input.put("_uploadedFile_fileName", reportName);
            input.put("_uploadedFile_contentType", "application/pdf");

            serviceResults = dispatcher.runSync("uploadFile", input);
            // delete the generate pdf after upload
            Debug.logInfo("delete pdf [" + UtilReports.OUT_PATH + reportName + "] after upload.", MODULE);
            pdfFile.delete();

            if (ServiceUtil.isError(serviceResults)) {
                throw new GenericServiceException(ServiceUtil.getErrorMessage(serviceResults));
            }
            String contentId = (String) serviceResults.get("contentId");
            if (UtilValidate.isNotEmpty(contentId)) {
                serviceResults = dispatcher.runSync("createCommEventContentAssoc", UtilMisc.toMap("contentId", contentId, "communicationEventId", communicationEventId,
                                                                                                  "sequenceNum", new Long(1), "userLogin", userLogin));
                if (ServiceUtil.isError(serviceResults)) {
                    throw new GenericServiceException(ServiceUtil.getErrorMessage(serviceResults));
                }
            } else {
                throw new GenericServiceException("Upload file ran successfully for [" + reportName + "] but no contentId was returned");
            }
        }

        // Create or update a scheduled (TASK_STARTED) TASK WorkEffort to save this email
        input = UtilMisc.toMap("workEffortTypeId", "TASK", "currentStatusId", "TASK_STARTED", "userLogin", userLogin);
        input.put("actualStartDate", context.get("datetimeStarted"));
        if (UtilValidate.isEmpty(input.get("actualStartDate"))) {
            input.put("actualStartDate", UtilDateTime.nowTimestamp());
        }
        input.put("workEffortName", subject);
        input.put("workEffortPurposeTypeId", "WEPT_TASK_EMAIL");
        serviceResults = dispatcher.runSync("createWorkEffort", input);
        if (ServiceUtil.isError(serviceResults)) {
            throw new GenericServiceException(ServiceUtil.getErrorMessage(serviceResults));
        }
        String workEffortId = (String) serviceResults.get("workEffortId");
        parameters.put("workEffortId", workEffortId);
       // create an association between the task and common event (safe even if existing)
        input = UtilMisc.toMap("userLogin", userLogin, "communicationEventId", communicationEventId, "workEffortId", workEffortId);
        serviceResults = dispatcher.runSync("createCommunicationEventWorkEff", input);
        if (ServiceUtil.isError(serviceResults)) {
            throw new GenericServiceException(ServiceUtil.getErrorMessage(serviceResults));
        }


        // Get all the current partyContactMech records related to any of the involved email addresses
        List emailAddresses = new ArrayList(toAddresses);
        List partyAndContactMechs = delegator.findByCondition("PartyAndContactMech", EntityCondition.makeCondition(EntityOperator.AND, EntityCondition.makeCondition("infoString", EntityOperator.IN, emailAddresses)), null, UtilMisc.toList("fromDate"));
        partyAndContactMechs = EntityUtil.filterByDate(partyAndContactMechs, true);

        // Create separate lists for the to, CC and BCC addresses
        EntityCondition filterConditions = EntityCondition.makeCondition(EntityOperator.AND, EntityCondition.makeCondition("infoString", EntityOperator.IN, toAddresses));
        List partyAndContactMechsTo = EntityUtil.filterByCondition(partyAndContactMechs, filterConditions);
        associateCommunicationEventWorkEffortAndParties(partyAndContactMechsTo, communicationEventId, "EMAIL_RECIPIENT_TO", workEffortId, delegator, dispatcher, userLogin);
        return parameters;
    }



    /**
     * Parse a comma-delimited string of email addresses and validate each.
     * @param emailAddressString Comma-delimited string of email addresses
     * @param requireDot if require dot, ignored, not supported by <code>UtilValidate</code> anymore, TODO: see if that breaks anything
     * @return Set of valid email addresses
     */
    @Deprecated private static Set<String> getValidEmailAddressesFromString(String emailAddressString, boolean requireDot) {
        Set<String> emailAddresses = new TreeSet<String>();
        if (UtilValidate.isNotEmpty(emailAddressString)) {
            String[] emails = emailAddressString.split(",");
            for (int x = 0; x < emails.length; x++) {
                if (!UtilValidate.isEmail(emails[x])) {
                    Debug.logInfo("Ignoring invalid email address: " + emails[x], MODULE);
                    continue;
                }
                emailAddresses.add(UtilValidate.stripWhitespace(emails[x]));
            }
        }
        return emailAddresses;
    }

    /**
     * get byte[] from a file.
     *
     * @param f a <code>File</code> value
     * @throws Exception if an error occurs
     * @return a <code>byte[]</code> value
     * @throws IOException if an error occurs
     */
    private static byte[] getBytesFromFile(File f) throws IOException {
        if (f == null) {
            return null;
        }

        FileInputStream stream = new FileInputStream(f);
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        byte[] b = new byte[1024];
        int n;
        while ((n = stream.read(b)) != -1) {
            out.write(b, 0, n);
        }
        stream.close();
        out.close();
        return out.toByteArray();
    }

    /**
     * Creates a CommunicationEventRole and WorkeffortPartyAssignment (if the party has a CRM role) for each party in the list.
     * @param partyAndContactMechs a <code>List</code> instance
     * @param communicationEventId a <code>String</code> value
     * @param roleTypeId a <code>String</code> value
     * @param workEffortId a <code>String</code> value
     * @param delegator a <code>Delegator</code> value
     * @param dispatcher a <code>LocalDispatcher</code> value
     * @param userLogin a <code>GenericValue</code> value
     * @throws GenericServiceException if an error occurs
     * @throws GenericEntityException if an error occurs
     */
    private static void associateCommunicationEventWorkEffortAndParties(List<GenericValue> partyAndContactMechs, String communicationEventId, String roleTypeId, String workEffortId, Delegator delegator, LocalDispatcher dispatcher, GenericValue userLogin)
                   throws GenericEntityException, GenericServiceException {
        if (UtilValidate.isNotEmpty(partyAndContactMechs)) {

            Map<String, Object> serviceResults = null;
            Map<String, Object> input = null;

            List<String> validRoleTypeIds = new ArrayList<String>(TEAM_MEMBER_ROLES);
            validRoleTypeIds.addAll(CLIENT_PARTY_ROLES);

            Set<String> partyIds = new HashSet<String>(EntityUtil.<String>getFieldListFromEntityList(partyAndContactMechs, "partyId", true));
            Set<String> emailAddresses = new HashSet<String>(EntityUtil.<String>getFieldListFromEntityList(partyAndContactMechs, "infoString", true));      // for looking for the owner of this activity against an email

            for (String partyId : partyIds) {

                // Add a CommunicationEventRole for the party, if one doesn't already exist
                long commEventRoles = delegator.findCountByAnd("CommunicationEventRole", UtilMisc.toMap("communicationEventId", communicationEventId, "partyId", partyId, "roleTypeId", roleTypeId));
                if (commEventRoles == 0) {
                    serviceResults = dispatcher.runSync("ensurePartyRole", UtilMisc.toMap("partyId", partyId, "roleTypeId", roleTypeId, "userLogin", userLogin));
                    if (ServiceUtil.isError(serviceResults)) {
                        Debug.logError(ServiceUtil.getErrorMessage(serviceResults), MODULE);
                        throw new GenericServiceException(ServiceUtil.getErrorMessage(serviceResults));
                    }

                    // Use the first PartyAndContactMech for that partyId in the partyAndContactMech list
                    EntityCondition filterConditions = EntityCondition.makeCondition(EntityOperator.AND,
                                                                                     EntityCondition.makeCondition("partyId", partyId),
                                                                                     EntityCondition.makeCondition("contactMechId", EntityOperator.NOT_EQUAL, null));
                    GenericValue partyAndContactMech = EntityUtil.getFirst(EntityUtil.filterByCondition(partyAndContactMechs, filterConditions));

                    // Create the communicationEventRole
                    serviceResults = dispatcher.runSync("createCommunicationEventRole", UtilMisc.toMap("communicationEventId", communicationEventId, "partyId", partyId, "roleTypeId", roleTypeId, "contactMechId", partyAndContactMech.getString("contactMechId"), "userLogin", userLogin));
                    if (ServiceUtil.isError(serviceResults)) {
                        Debug.logError(ServiceUtil.getErrorMessage(serviceResults), MODULE);
                        throw new GenericServiceException(ServiceUtil.getErrorMessage(serviceResults));
                    }
                }

                if (UtilValidate.isNotEmpty(workEffortId)) {
                    // Assign the party to the workeffort if they have a CRM role, and if they aren't already assigned
                    List<GenericValue> workEffortPartyAssignments = delegator.findByAnd("WorkEffortPartyAssignment", UtilMisc.toMap("partyId", partyId, "workEffortId", workEffortId));
                    workEffortPartyAssignments = EntityUtil.filterByDate(workEffortPartyAssignments);
                    if (UtilValidate.isEmpty(workEffortPartyAssignments)) {
                        String crmRoleTypeId = PartyHelper.getFirstValidRoleTypeId(partyId, validRoleTypeIds, delegator);
                        if (crmRoleTypeId == null) {
                            Debug.logWarning("No valid roles found for partyId [" + partyId + "], so it will not be assigned to activity " + workEffortId, MODULE);
                        } else {
                            // if this party is an internal party (crmsfa user), the activity does not have an owner yet, and
                            // this current party is associated with any of the email addresses as "Owner of Received Emails", then
                            // the party is the owner
                            // note that this means the activity can only have one owner at a time
                            if (TEAM_MEMBER_ROLES.contains(crmRoleTypeId) && (UtilValidate.isEmpty(getActivityOwner(workEffortId, delegator)))) {
                                if (UtilValidate.isNotEmpty(org.opentaps.common.party.PartyHelper.getCurrentContactMechsForParty(partyId, "EMAIL_ADDRESS", "RECEIVE_EMAIL_OWNER",
                                        UtilMisc.toList(EntityCondition.makeCondition("infoString", EntityOperator.IN, emailAddresses)), delegator))) {
                                    crmRoleTypeId = "CAL_OWNER";
                                    Debug.logInfo("Will be assigning [" + partyId + "] as owner of [" + workEffortId + "]", MODULE);
                                }
                            }

                            input = UtilMisc.toMap("partyId", partyId, "workEffortId", workEffortId, "roleTypeId", crmRoleTypeId, "statusId", "PRTYASGN_ASSIGNED", "userLogin", userLogin);
                            serviceResults = dispatcher.runSync("assignPartyToWorkEffort", input);
                            if (ServiceUtil.isError(serviceResults)) {
                                Debug.logError(ServiceUtil.getErrorMessage(serviceResults), MODULE);
                                throw new GenericServiceException(ServiceUtil.getErrorMessage(serviceResults));
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Gets owner party id of activity.
     * @param workEffortId a <code>String</code> value
     * @param delegator a <code>Delegator</code> value
     * @return owner party found for activity
     * @throws GenericEntityException if an error occurs
     */
    private static GenericValue getActivityOwner(String workEffortId, Delegator delegator) throws GenericEntityException {
        List<GenericValue> ownerParties = EntityUtil.filterByDate(getActivityParties(delegator, workEffortId, UtilMisc.toList("CAL_OWNER")));
        if (UtilValidate.isEmpty(ownerParties)) {
            Debug.logWarning("No owner parties found for activity [" + workEffortId + "]", MODULE);
            return null;
        } else if (ownerParties.size() > 1) {
            Debug.logWarning("More than one owner party found for activity [" + workEffortId + "].  Only the first party will be returned, but the parties are " + EntityUtil.getFieldListFromEntityList(ownerParties, "partyId", false), MODULE);
        }

        return EntityUtil.getFirst(ownerParties);

    }

    /**
     * gets all unexpired parties related to the work effort. The result is a list of WorkEffortPartyAssignments containing the partyIds we need.
     * @param delegator a <code>Delegator</code> instance
     * @param workEffortId a <code>String</code> value
     * @param partyRoles a <code>List</code> instance
     * @return relate parties for activity
     * @throws GenericEntityException if an error occurs
     */
    private static List<GenericValue> getActivityParties(Delegator delegator, String workEffortId, List<String> partyRoles) throws GenericEntityException {
        // add each role type id (ACCOUNT, CONTACT, etc) to an OR condition list
        List<EntityCondition> roleCondList = new ArrayList<EntityCondition>();
        for (Iterator<String> iter = partyRoles.iterator(); iter.hasNext();) {
            String roleTypeId = iter.next();
            roleCondList.add(EntityCondition.makeCondition("roleTypeId", roleTypeId));
        }
        EntityCondition roleEntityCondList = EntityCondition.makeCondition(roleCondList, EntityOperator.OR);

        // roleEntityCondList AND workEffortId = ${workEffortId} AND filterByDateExpr
        EntityCondition mainCondList = EntityCondition.makeCondition(EntityOperator.AND,
                    roleEntityCondList,
                    EntityCondition.makeCondition("workEffortId", workEffortId),
                    EntityUtil.getFilterByDateExpr());

        return delegator.findByCondition("WorkEffortPartyAssignment", mainCondList, null,
                null,
                null, // fields to order by (unimportant here)
                new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, true));

    }

}
