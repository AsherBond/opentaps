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

package com.opensourcestrategies.financials.invoice;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import javax.servlet.http.HttpServletRequest;

import javolution.util.FastList;
import javolution.util.FastMap;
import javolution.util.FastSet;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityFunction;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.party.party.PartyHelper;
import org.opentaps.base.constants.ContactMechPurposeTypeConstants;
import org.opentaps.base.entities.BillingAccountAndRole;
import org.opentaps.base.entities.GlAccountOrganizationAndClass;
import org.opentaps.base.entities.InvoiceAdjustmentType;
import org.opentaps.base.entities.InvoiceAndInvoiceItem;
import org.opentaps.base.entities.InvoiceItem;
import org.opentaps.base.entities.InvoiceType;
import org.opentaps.base.entities.OrderItem;
import org.opentaps.base.entities.OrderItemBilling;
import org.opentaps.base.entities.PartyContactWithPurpose;
import org.opentaps.base.entities.PaymentApplication;
import org.opentaps.base.entities.PostalAddress;
import org.opentaps.base.entities.StatusItem;
import org.opentaps.base.entities.TaxAuthorityAndDetail;
import org.opentaps.base.entities.TermType;
import org.opentaps.common.builder.EntityListBuilder;
import org.opentaps.common.builder.PageBuilder;
import org.opentaps.common.util.UtilAccountingTags;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.billing.BillingDomainInterface;
import org.opentaps.domain.billing.invoice.Invoice;
import org.opentaps.domain.billing.invoice.InvoiceRepositoryInterface;
import org.opentaps.domain.billing.payment.Payment;
import org.opentaps.domain.organization.Organization;
import org.opentaps.domain.organization.OrganizationRepositoryInterface;
import org.opentaps.domain.party.Party;
import org.opentaps.domain.party.PartyRepositoryInterface;
import org.opentaps.foundation.action.ActionContext;
import org.opentaps.foundation.entity.Entity;
import org.opentaps.foundation.exception.FoundationException;

/**
 * InvoiceActions - Java Actions for invoices.
 */
public final class InvoiceActions {

    private static final String MODULE = InvoiceActions.class.getName();

    private InvoiceActions() { }

    /**
     * Action for the view invoice screen.
     * @param context the screen context
     * @throws GeneralException if an error occurs
     */
    public static void viewInvoice(Map<String, Object> context) throws GeneralException {

        ActionContext ac = new ActionContext(context);

        HttpServletRequest request = ac.getRequest();
        Delegator delegator = ac.getDelegator();
        Locale locale = ac.getLocale();

        String organizationPartyId = UtilCommon.getOrganizationPartyId(request);
        if (organizationPartyId == null) {
            return;
        }
        ac.put("organizationPartyId", organizationPartyId);

        // get the view preference from the parameter
        String useGwtParam = ac.getParameter("useGwt");
        // get it from the database
        String useGwtPref = UtilCommon.getUserLoginViewPreference(request, "financials", "viewInvoice", "useGwt");
        boolean useGwt;
        if (useGwtParam != null) {
            useGwt = "Y".equals(useGwtParam);
            // persist the change if any
            if (useGwt) {
                useGwtParam = "Y";
            } else {
                useGwtParam = "N";
            }
            if (!useGwtParam.equals(useGwtPref)) {
                UtilCommon.setUserLoginViewPreference(request, "financials", "viewInvoice", "useGwt", useGwtParam);
            }
        } else if (useGwtPref != null) {
            useGwt = "Y".equals(useGwtPref);
        } else {
            // else default to true
            useGwt = true;
        }
        ac.put("useGwt", useGwt);

        // get the invoice from the domain
        String invoiceId = (String) ac.get("invoiceId");
        DomainsDirectory dd = DomainsDirectory.getDomainsDirectory(ac);
        BillingDomainInterface billingDomain = dd.getBillingDomain();
        InvoiceRepositoryInterface invoiceRepository = billingDomain.getInvoiceRepository();
        PartyRepositoryInterface partyRepository = dd.getPartyDomain().getPartyRepository();
        OrganizationRepositoryInterface organizationRepository = dd.getOrganizationDomain().getOrganizationRepository();

        Invoice invoice = null;
        try {
            invoice = invoiceRepository.getInvoiceById(invoiceId);
        } catch (FoundationException e) {
            Debug.logError("No invoice found with ID [" + invoiceId + "]", MODULE);
            // let the invoice == null check deal with this
        }
        if (invoice == null) {
            ac.put("decoratorLocation", "component://opentaps-common/widget/screens/common/CommonScreens.xml");
            return;
        }
        ac.put("invoice", invoice);

        // put to history
        InvoiceType invoiceType = invoice.getInvoiceType();
        ac.put("history", UtilCommon.makeHistoryEntry(UtilMessage.expandLabel("FinancialsNavHistoryInvoice", locale, UtilMisc.toMap("invoiceId", invoiceId, "invoiceTypeName", invoiceType.get("description", locale))), "viewInvoice", UtilMisc.toList("invoiceId")));

        // get the invoice items
        ac.put("invoiceItems", invoice.getInvoiceItems());
 
        // get the last invoice item, will use it as default tags for the add invoice item form
        InvoiceItem lastItem = Entity.getLast(invoice.getInvoiceItems());
        ac.put("lastItem", lastItem);

        // get the application payments, we need to fetch the payment entity too
        List<? extends PaymentApplication> paymentApplications = invoice.getPaymentApplications();
        List<Map<String, Object>> payments = new FastList<Map<String, Object>>();
        List<Map<String, Object>> creditPayments = new FastList<Map<String, Object>>();
        for (PaymentApplication pa : paymentApplications) {
            Payment payment = billingDomain.getPaymentRepository().getPaymentById(pa.getPaymentId());
            Map<String, Object> p = payment.toMap();
            p.put("paymentApplicationId", pa.getPaymentApplicationId());
            p.put("amountApplied", pa.getAmountApplied());
            StatusItem status = payment.getStatusItem();
            p.put("statusDescription", status.get(StatusItem.Fields.description.name(), locale));
            if (invoice.isReturnInvoice() && payment.isCustomerRefund() && payment.isBillingAccountPayment()) {
                // the billing account comes from the payment's other payment application
                List<? extends PaymentApplication> applications = payment.getRelated(PaymentApplication.class);
                for (PaymentApplication app : applications) {
                    if (app.getBillingAccountId() != null) {
                        p.put("billingAccountId", app.getBillingAccountId());
                        creditPayments.add(p);
                        break;
                    }
                }
            } else {
                payments.add(p);
            }
        }
        ac.put("payments", payments);
        ac.put("creditPayments", creditPayments);

        // these booleans group the invoices into tabs
        ac.put("isReceipt", invoice.isReceivable());
        ac.put("isDisbursement", invoice.isPayable());
        ac.put("isPartner", invoice.isPartnerInvoice());

        // note Partner invoice are considered receivable invoice, so test for that first
        if (invoice.isPartnerInvoice()) {
            ac.put("decoratorLocation", "component://financials/widget/financials/screens/partners/PartnerScreens.xml");
        } else if (invoice.isPayable()) {
            ac.put("decoratorLocation", "component://financials/widget/financials/screens/payables/PayablesScreens.xml");
        } else if (invoice.isReceivable()) {
            ac.put("decoratorLocation", "component://financials/widget/financials/screens/receivables/ReceivablesScreens.xml");
        }

        // get the accounting tags for the invoice
        if (invoice.isCommissionInvoice()) {
            ac.put("tagTypes", UtilAccountingTags.getAccountingTagsForOrganization(organizationPartyId, UtilAccountingTags.COMMISSION_INVOICES_TAG, delegator));
        } else if (invoice.isSalesInvoice()) {
            ac.put("tagTypes", UtilAccountingTags.getAccountingTagsForOrganization(organizationPartyId, UtilAccountingTags.SALES_INVOICES_TAG, delegator));
        } else if (invoice.isPurchaseInvoice()) {
            ac.put("tagTypes", UtilAccountingTags.getAccountingTagsForOrganization(organizationPartyId, UtilAccountingTags.PURCHASE_INVOICES_TAG, delegator));
        } else if (invoice.isReturnInvoice()) {
            ac.put("tagTypes", UtilAccountingTags.getAccountingTagsForOrganization(organizationPartyId, UtilAccountingTags.RETURN_INVOICES_TAG, delegator));
        }

        Party transactionParty = partyRepository.getPartyById(invoice.getTransactionPartyId());
        ac.put("billingPartyId", invoice.getTransactionPartyId());

        // the billing address, which can be either the payment or billing location
        String invoiceContactMechId = null;
        PostalAddress invoiceAddress = invoice.getBillingAddress();
        if (invoiceAddress != null) {
            invoiceContactMechId = invoiceAddress.getContactMechId();
        }

        ac.put("invoiceAddress", invoiceAddress);
        ac.put("invoiceContactMechId", invoiceContactMechId);

        // update permissions
        boolean hasDescriptiveUpdatePermission = false;
        boolean limitedEditOnly = false;

        boolean hasUpdatePermission = false;
        boolean hasAdjustmentPermission = false;
        if ((invoice.isReceivable() && ac.hasEntityPermission("FINANCIALS", "_AR_INUPDT")) || (invoice.isPayable() && ac.hasEntityPermission("FINANCIALS", "_AP_INUPDT"))) {
            hasUpdatePermission = invoice.isInProcess();
            hasAdjustmentPermission = invoice.isAdjustable();
            // allow update descriptive fields
            limitedEditOnly = (invoice.isReady() && "edit".equals(ac.getParameter("op")));
            hasDescriptiveUpdatePermission = invoice.isReady();
        }
        ac.put("hasUpdatePermission", hasUpdatePermission);
        ac.put("hasAdjustmentPermission", hasAdjustmentPermission);
        ac.put("limitedEditOnly", limitedEditOnly);
        ac.put("hasDescriptiveUpdatePermission", hasDescriptiveUpdatePermission);

        // create permission
        boolean hasCreatePermission = ac.hasEntityPermission("FINANCIALS", "_AP_INCRTE") || ac.hasEntityPermission("FINANCIALS", "_AR_INCRTE");
        ac.put("hasCreatePermission", hasCreatePermission);

        // writeoff permission
        boolean hasWriteoffPermission = false;
        if ((invoice.isReceivable() && (ac.hasEntityPermission("FINANCIALS", "_AR_INWRTOF"))
             || !invoice.isReceivable() && (ac.hasEntityPermission("FINANCIALS", "_AP_INWRTOF")))
            && (invoice.isReady() || invoice.isPaid())) {
            hasWriteoffPermission = true;
        }
        ac.put("hasWriteoffPermission", hasWriteoffPermission);

        // update permission implies that the header and items are editable, so get some data for the forms
        EntityCondition conditions;
        if (hasUpdatePermission || limitedEditOnly) {
            List<GlAccountOrganizationAndClass> glAccounts = invoiceRepository.findListCache(GlAccountOrganizationAndClass.class, invoiceRepository.map(GlAccountOrganizationAndClass.Fields.organizationPartyId, organizationPartyId), UtilMisc.toList(GlAccountOrganizationAndClass.Fields.accountCode.name()));
            ac.put("glAccounts", glAccounts);
            ac.put("invoiceItemTypes", invoice.getApplicableInvoiceItemTypes());

            // party's billing and payment locations
            conditions = EntityCondition.makeCondition(EntityOperator.AND,
                                   EntityCondition.makeCondition(PartyContactWithPurpose.Fields.contactMechPurposeTypeId.name(),
                                                                 EntityOperator.IN,
                                                                 UtilMisc.toList(ContactMechPurposeTypeConstants.BILLING_LOCATION,
                                                                                 ContactMechPurposeTypeConstants.PAYMENT_LOCATION,
                                                                                 ContactMechPurposeTypeConstants.GENERAL_LOCATION)),
                                   EntityCondition.makeCondition(PartyContactWithPurpose.Fields.partyId.name(), EntityOperator.EQUALS, invoice.getTransactionPartyId()),
                                   EntityUtil.getFilterByDateExpr(PartyContactWithPurpose.Fields.contactFromDate.name(), PartyContactWithPurpose.Fields.contactThruDate.name()),
                                   EntityUtil.getFilterByDateExpr(PartyContactWithPurpose.Fields.purposeFromDate.name(), PartyContactWithPurpose.Fields.purposeThruDate.name()));
            List<PartyContactWithPurpose> purposes = invoiceRepository.findList(PartyContactWithPurpose.class, conditions);
            Set<String> contactMechIds = Entity.getDistinctFieldValues(String.class, purposes, PartyContactWithPurpose.Fields.contactMechId);

            // make sure the current billing address is also listed (it may have been changed / expired for the account)
            if (UtilValidate.isNotEmpty(invoice.getContactMechId())) {
                contactMechIds.add(invoice.getContactMechId());
            }

            ac.put("addresses", invoiceRepository.findList(PostalAddress.class,
                                                           EntityCondition.makeCondition(PostalAddress.Fields.contactMechId.name(),
                                                                                         EntityOperator.IN,
                                                                                         contactMechIds)));

            // party's shipping locations
            contactMechIds = Entity.getDistinctFieldValues(String.class, transactionParty.getShippingAddresses(), PostalAddress.Fields.contactMechId);

            // make sure the current shipping address is also listed (it may have been changed / expired for the account)
            if (UtilValidate.isNotEmpty(invoice.getShippingAddress())) {
                contactMechIds.add(invoice.getShippingAddress().getContactMechId());
            }

            ac.put("shippingAddresses", invoiceRepository.findList(PostalAddress.class,
                                                           EntityCondition.makeCondition(PostalAddress.Fields.contactMechId.name(),
                                                                                         EntityOperator.IN,
                                                                                         contactMechIds)));

            // available tax authorities
            List<TaxAuthorityAndDetail> taxAuthorities = invoiceRepository.findAllCache(TaxAuthorityAndDetail.class, UtilMisc.toList(TaxAuthorityAndDetail.Fields.abbreviation.name(), TaxAuthorityAndDetail.Fields.groupName.name()));
            ac.put("taxAuthorities", taxAuthorities);
        }

        // Invoice terms and term types
        ac.put("invoiceTerms", invoice.getInvoiceTerms());
        List<TermType> termTypes = organizationRepository.getValidTermTypes(invoice.getInvoiceTypeId());
        ac.put("termTypes", termTypes);

        // Prepare string that contains list of related order ids
        List<? extends OrderItemBilling> orderItemBillings = invoice.getOrderItemBillings();
        Set<String> orderIds = new FastSet<String>();
        for (OrderItemBilling billing : orderItemBillings) {
            orderIds.add(billing.getOrderId());
        }
        String ordersList = null;
        for (String id : orderIds) {
            List<OrderItem> orderItems = invoiceRepository.findList(OrderItem.class, invoiceRepository.map(OrderItem.Fields.orderId, id));
            if (orderItems == null) {
                continue;
            }
            if (ordersList == null) {
                ordersList = id;
            } else {
                ordersList += (", " + id);
            }
            if (orderItems != null && orderItems.size() > 0) {
                // collect unique PO id
                Set<String> orderCorrespondingPOs = FastSet.<String>newInstance();
                for (OrderItem orderItem : orderItems) {
                    String correspondingPoId = orderItem.getCorrespondingPoId();
                    if (UtilValidate.isNotEmpty(correspondingPoId)) {
                        orderCorrespondingPOs.add(correspondingPoId);
                    }
                }
                if (UtilValidate.isNotEmpty(orderCorrespondingPOs)) {
                    ordersList += "(";
                    boolean first = true;
                    for (String poId : orderCorrespondingPOs) {
                        if (first) {
                            ordersList += ac.getUiLabel("OpentapsPONumber") + ":";
                        } else {
                            ordersList += ", ";
                        }
                        ordersList += poId;
                        first = false;
                    }
                    ordersList += ")";
                }
            }
        }
        if (ordersList != null) {
            ac.put("ordersList", ordersList);
        }

        // billing accounts of the from party for Accounts Payable invoices
        if (invoice.isPayable()) {
            conditions = EntityCondition.makeCondition(EntityOperator.AND,
                          EntityCondition.makeCondition(BillingAccountAndRole.Fields.partyId.name(), EntityOperator.EQUALS, invoice.getPartyIdFrom()),
                          EntityCondition.makeCondition(BillingAccountAndRole.Fields.roleTypeId.name(), EntityOperator.EQUALS, "BILL_TO_CUSTOMER"),
                          EntityUtil.getFilterByDateExpr());
            List<BillingAccountAndRole> billingAccounts = invoiceRepository.findList(BillingAccountAndRole.class, conditions, UtilMisc.toList(BillingAccountAndRole.Fields.billingAccountId.name()));
            ac.put("billingAccounts", billingAccounts);
        }

        // invoice adjustment types
        if (invoice.isAdjustable()) {
            Organization organization = organizationRepository.getOrganizationById(organizationPartyId);
            List<InvoiceAdjustmentType> types = invoiceRepository.getInvoiceAdjustmentTypes(organization, invoice);
            ac.put("invoiceAdjustmentTypes", types);
        }
    }

    /**
     * Action for the find / list invoices screen.
     * @param context the screen context
     * @throws GeneralException if an error occurs
     * @throws ParseException if an error occurs
     */
    public static void findInvoices(Map<String, Object> context) throws GeneralException, ParseException {

        final ActionContext ac = new ActionContext(context);

        final Locale locale = ac.getLocale();
        final TimeZone timeZone = ac.getTimeZone();

        // Finds invoices based on invoiceTypeId and various input parameters
        String invoiceTypeId = ac.getString("invoiceTypeId");

        // set the find form title here because of limitations with uiLabelMap, along with other variables
        String findFormTitle = "";
        boolean isReceivable = false;
        boolean isPayable = false;
        boolean isPartner = false;
        boolean enableFindByOrder = false;
        String tagsType = null;
        if ("SALES_INVOICE".equals(invoiceTypeId)) {
            findFormTitle = ac.getUiLabel("FinancialsFindSalesInvoices");
            isReceivable = true;
            enableFindByOrder = true;
            tagsType = UtilAccountingTags.LOOKUP_SALES_INVOICES_TAG;
        } else if ("PURCHASE_INVOICE".equals(invoiceTypeId)) {
            findFormTitle = ac.getUiLabel("FinancialsFindPurchaseInvoices");
            isPayable = true;
            enableFindByOrder = true;
            tagsType = UtilAccountingTags.LOOKUP_PURCHASE_INVOICES_TAG;
        } else if ("CUST_RTN_INVOICE".equals(invoiceTypeId)) {
            findFormTitle = ac.getUiLabel("FinancialsFindCustomerReturnInvoices");
            isPayable = true;
        } else if ("COMMISSION_INVOICE".equals(invoiceTypeId)) {
            findFormTitle = ac.getUiLabel("FinancialsFindCommissionInvoices");
            isPayable = true;
            tagsType = UtilAccountingTags.LOOKUP_COMMISSION_INVOICES_TAG;
        } else if ("INTEREST_INVOICE".equals(invoiceTypeId)) {
            findFormTitle = ac.getUiLabel("FinancialsFindFinanceCharges");
            isReceivable = true;
        } else if ("PARTNER_INVOICE".equals(invoiceTypeId)) {
            findFormTitle = ac.getUiLabel("FinancialsFindPartnerInvoices");
            isPartner = true;
        }
        ac.put("findFormTitle", findFormTitle);
        ac.put("isReceivable", isReceivable);
        ac.put("isPayable", isPayable);
        ac.put("isPartner", isPartner);
        ac.put("enableFindByOrder", enableFindByOrder);

        DomainsDirectory dd = DomainsDirectory.getDomainsDirectory(ac);
        BillingDomainInterface billingDomain = dd.getBillingDomain();
        InvoiceRepositoryInterface repository = billingDomain.getInvoiceRepository();

        // get the list of statuses for the parametrized form ftl
        List<StatusItem> statuses = repository.findListCache(StatusItem.class, repository.map(StatusItem.Fields.statusTypeId, "INVOICE_STATUS"), UtilMisc.toList(StatusItem.Fields.sequenceId.name()));
        List<Map<String, Object>> statusList = new FastList<Map<String, Object>>();
        for (StatusItem s : statuses) {
            Map<String, Object> status = s.toMap();
            status.put("statusDescription", s.get(StatusItem.Fields.description.name(), locale));
            statusList.add(status);
        }
        ac.put("statuses", statusList);

        // get the list of processing statuses for the parametrized form ftl
        List<StatusItem> processingStatuses = repository.findListCache(StatusItem.class, repository.map(StatusItem.Fields.statusTypeId, "INVOICE_PROCESS_STTS"), UtilMisc.toList(StatusItem.Fields.sequenceId.name()));
        List<Map<String, Object>> processingStatusList = new FastList<Map<String, Object>>();
        // add None filter for the processing status
        processingStatusList.add(UtilMisc.<String, Object>toMap(StatusItem.Fields.statusId.name(), "_NA_", "statusDescription", ac.getUiLabel("CommonNone")));
        for (StatusItem s : processingStatuses) {
            Map<String, Object> status = s.toMap();
            status.put("statusDescription", s.get(StatusItem.Fields.description.name(), locale));
            processingStatusList.add(status);
        }
        ac.put("processingStatuses", processingStatusList);

        // get the list of accounting tags for the current organization
        String organizationPartyId = UtilCommon.getOrganizationPartyId(ac.getRequest());
        if (tagsType != null) {
            ac.put("tagFilters", UtilAccountingTags.getAccountingTagFiltersForOrganization(organizationPartyId, tagsType, ac.getDelegator(), locale));
        }

        // now check if we want to actually do a find, which is triggered by performFind = Y
        if (!"Y".equals(ac.getParameter("performFind"))) {
            return;
        }

        // get the search parameters
        String partyId = ac.getParameter("partyId");
        String partyIdFrom = ac.getParameter("partyIdFrom");
        String invoiceId = ac.getParameter("invoiceId");
        String statusId = ac.getParameter("statusId");
        String processingStatusId = ac.getParameter("processingStatusId");
        String invoiceDateFrom = ac.getParameter("invoiceDateFrom");
        String invoiceDateThru = ac.getParameter("invoiceDateThru");
        String dueDateFrom = ac.getParameter("dueDateFrom");
        String dueDateThru = ac.getParameter("dueDateThru");
        String paidDateFrom = ac.getParameter("paidDateFrom");
        String paidDateThru = ac.getParameter("paidDateThru");
        String amountFrom = ac.getParameter("amountFrom");
        String amountThru = ac.getParameter("amountThru");
        String openAmountFrom = ac.getParameter("openAmountFrom");
        String openAmountThru = ac.getParameter("openAmountThru");
        String referenceNumber = ac.getParameter("referenceNumber");
        String message = ac.getParameter("message");
        String orderId = ac.getParameter("orderId");
        String itemDescription = ac.getParameter("itemDescription");

        // build search conditions
        List<EntityCondition> search = new FastList<EntityCondition>();
        if (partyId != null) {
            search.add(EntityCondition.makeCondition(InvoiceAndInvoiceItem.Fields.partyId.name(), EntityOperator.EQUALS, partyId.trim()));
        }
        if (partyIdFrom != null) {
            search.add(EntityCondition.makeCondition(InvoiceAndInvoiceItem.Fields.partyIdFrom.name(), EntityOperator.EQUALS, partyIdFrom.trim()));
        }
        if (invoiceId != null) {
            search.add(EntityCondition.makeCondition(InvoiceAndInvoiceItem.Fields.invoiceId.name(), EntityOperator.EQUALS, invoiceId.trim()));
        }
        if (statusId != null) {
            search.add(EntityCondition.makeCondition(InvoiceAndInvoiceItem.Fields.statusId.name(), EntityOperator.EQUALS, statusId.trim()));
        }
        if (processingStatusId != null) {
            // this is a special case where we want an empty status
            if ("_NA_".equals(processingStatusId)) {
                search.add(EntityCondition.makeCondition(InvoiceAndInvoiceItem.Fields.processingStatusId.name(), EntityOperator.EQUALS, null));
            } else {
                search.add(EntityCondition.makeCondition(InvoiceAndInvoiceItem.Fields.processingStatusId.name(), EntityOperator.EQUALS, processingStatusId.trim()));
            }
        }
        String dateFormat = UtilDateTime.getDateFormat(locale);
        if (invoiceDateFrom != null) {
            search.add(EntityCondition.makeCondition(InvoiceAndInvoiceItem.Fields.invoiceDate.name(), EntityOperator.GREATER_THAN_EQUAL_TO, UtilDateTime.getDayStart(UtilDateTime.stringToTimeStamp(invoiceDateFrom, dateFormat, timeZone, locale), timeZone, locale)));
        }
        if (dueDateFrom != null) {
            search.add(EntityCondition.makeCondition(InvoiceAndInvoiceItem.Fields.dueDate.name(), EntityOperator.GREATER_THAN_EQUAL_TO, UtilDateTime.getDayStart(UtilDateTime.stringToTimeStamp(dueDateFrom, dateFormat, timeZone, locale), timeZone, locale)));
        }
        if (paidDateFrom != null) {
            search.add(EntityCondition.makeCondition(InvoiceAndInvoiceItem.Fields.paidDate.name(), EntityOperator.GREATER_THAN_EQUAL_TO, UtilDateTime.getDayStart(UtilDateTime.stringToTimeStamp(paidDateFrom, dateFormat, timeZone, locale), timeZone, locale)));
        }
        if (invoiceDateThru != null) {
            search.add(EntityCondition.makeCondition(InvoiceAndInvoiceItem.Fields.invoiceDate.name(), EntityOperator.LESS_THAN_EQUAL_TO, UtilDateTime.getDayEnd(UtilDateTime.stringToTimeStamp(invoiceDateThru, dateFormat, timeZone, locale), timeZone, locale)));
        }
        if (dueDateThru != null) {
            search.add(EntityCondition.makeCondition(InvoiceAndInvoiceItem.Fields.dueDate.name(), EntityOperator.LESS_THAN_EQUAL_TO, UtilDateTime.getDayEnd(UtilDateTime.stringToTimeStamp(dueDateThru, dateFormat, timeZone, locale), timeZone, locale)));
        }
        if (paidDateThru != null) {
            search.add(EntityCondition.makeCondition(InvoiceAndInvoiceItem.Fields.paidDate.name(), EntityOperator.LESS_THAN_EQUAL_TO, UtilDateTime.getDayEnd(UtilDateTime.stringToTimeStamp(paidDateThru, dateFormat, timeZone, locale), timeZone, locale)));
        }
        if (amountFrom != null) {
            search.add(EntityCondition.makeCondition(InvoiceAndInvoiceItem.Fields.invoiceTotal.name(), EntityOperator.GREATER_THAN_EQUAL_TO, new BigDecimal(amountFrom)));
        }
        if (amountThru != null) {
            search.add(EntityCondition.makeCondition(InvoiceAndInvoiceItem.Fields.invoiceTotal.name(), EntityOperator.LESS_THAN_EQUAL_TO, new BigDecimal(amountThru)));
        }
        if (openAmountFrom != null) {
            search.add(EntityCondition.makeCondition(InvoiceAndInvoiceItem.Fields.openAmount.name(), EntityOperator.GREATER_THAN_EQUAL_TO, new BigDecimal(openAmountFrom)));
        }
        if (openAmountThru != null) {
            search.add(EntityCondition.makeCondition(InvoiceAndInvoiceItem.Fields.openAmount.name(), EntityOperator.LESS_THAN_EQUAL_TO, new BigDecimal(openAmountThru)));
        }
        if (referenceNumber != null) {
            search.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD(InvoiceAndInvoiceItem.Fields.referenceNumber.name()), EntityOperator.LIKE, EntityFunction.UPPER("%" + referenceNumber + "%")));
        }
        if (message != null) {
            search.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD(InvoiceAndInvoiceItem.Fields.invoiceMessage.name()), EntityOperator.LIKE, EntityFunction.UPPER("%" + message + "%")));
        }
        if (itemDescription != null) {
            search.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD(InvoiceAndInvoiceItem.Fields.itemDescription.name()), EntityOperator.LIKE, EntityFunction.UPPER("%" + itemDescription + "%")));
        }

        if (enableFindByOrder && orderId != null) {
            List<OrderItemBilling> orderItemBillings = repository.findList(OrderItemBilling.class, repository.map(OrderItemBilling.Fields.orderId, orderId));
            Set<String> invoiceIds = Entity.getDistinctFieldValues(String.class, orderItemBillings, OrderItemBilling.Fields.invoiceId);
            search.add(EntityCondition.makeCondition(InvoiceAndInvoiceItem.Fields.invoiceId.name(), EntityOperator.IN, invoiceIds));
        }

        if (tagsType != null) {
            search.addAll(UtilAccountingTags.buildTagConditions(organizationPartyId, tagsType, ac.getDelegator(), ac.getRequest(), UtilAccountingTags.TAG_PARAM_PREFIX, "itemAcctgTagEnumId"));
        }

        // required conditions
        search.add(EntityCondition.makeCondition(InvoiceAndInvoiceItem.Fields.invoiceTypeId.name(), EntityOperator.EQUALS, invoiceTypeId.trim()));


        // Pagination
        Set<String> fieldsToSelect = new HashSet<String>(new Invoice().getAllFieldsNames());
        fieldsToSelect.retainAll(new InvoiceAndInvoiceItem().getAllFieldsNames());
        EntityListBuilder invoiceListBuilder = new EntityListBuilder(repository, InvoiceAndInvoiceItem.class, EntityCondition.makeCondition(search, EntityOperator.AND), fieldsToSelect, UtilMisc.toList(InvoiceAndInvoiceItem.Fields.invoiceDate.desc()));
        PageBuilder<InvoiceAndInvoiceItem> pageBuilder = new PageBuilder<InvoiceAndInvoiceItem>() {
            public List<Map<String, Object>> build(List<InvoiceAndInvoiceItem> page) throws Exception {
                Delegator delegator = ac.getDelegator();
                List<Map<String, Object>> newPage = FastList.newInstance();
                for (InvoiceAndInvoiceItem invoice : page) {
                    Map<String, Object> newRow = FastMap.newInstance();
                    newRow.putAll(invoice.toMap());

                    StatusItem status = invoice.getStatusItem();
                    newRow.put("statusDescription", status.get(StatusItem.Fields.description.name(), locale));
                    StatusItem processingStatus = invoice.getProcessingStatusItem();
                    if (processingStatus != null) {
                        newRow.put("processingStatusDescription", processingStatus.get(StatusItem.Fields.description.name(), locale));
                    }

                    // Prepare string that contains list of related order ids
                    List<? extends OrderItemBilling> orderItemBillings = invoice.getOrderItemBillings();
                    Set<String> orderIds = new FastSet<String>();
                    for (OrderItemBilling billing : orderItemBillings) {
                        orderIds.add(billing.getOrderId());
                    }
                    newRow.put("orderIds", orderIds);

                    newRow.put("partyNameFrom", PartyHelper.getPartyName(delegator, invoice.getPartyIdFrom(), false));
                    newRow.put("partyName", PartyHelper.getPartyName(delegator, invoice.getPartyId(), false));

                    newRow.put("amount", invoice.getInvoiceTotal());
                    newRow.put("outstanding", invoice.getOpenAmount());

                    newPage.add(newRow);
                }
                return newPage;
            }
        };
        invoiceListBuilder.setPageBuilder(pageBuilder);

        ac.put("invoiceListBuilder", invoiceListBuilder);
    }
}
