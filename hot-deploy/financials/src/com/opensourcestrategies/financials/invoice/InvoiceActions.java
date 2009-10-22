/*
 * Copyright (c) 2009 - 2009 Open Source Strategies, Inc.
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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import com.opensourcestrategies.financials.util.UtilFinancial;
import javolution.util.FastList;
import javolution.util.FastSet;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.collections.ResourceBundleMapWrapper;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.security.Security;
import org.ofbiz.service.LocalDispatcher;
import org.opentaps.common.util.UtilAccountingTags;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.domain.base.entities.BillingAccountAndRole;
import org.opentaps.domain.base.entities.GlAccountOrganizationAndClass;
import org.opentaps.domain.base.entities.InvoiceAdjustmentType;
import org.opentaps.domain.base.entities.InvoiceContactMech;
import org.opentaps.domain.base.entities.InvoiceType;
import org.opentaps.domain.base.entities.OrderItem;
import org.opentaps.domain.base.entities.OrderItemBilling;
import org.opentaps.domain.base.entities.PartyContactMechPurpose;
import org.opentaps.domain.base.entities.PaymentApplication;
import org.opentaps.domain.base.entities.PostalAddress;
import org.opentaps.domain.base.entities.StatusItem;
import org.opentaps.domain.base.entities.TaxAuthorityAndDetail;
import org.opentaps.domain.base.entities.TermType;
import org.opentaps.domain.billing.BillingDomainInterface;
import org.opentaps.domain.billing.invoice.Invoice;
import org.opentaps.domain.billing.invoice.InvoiceRepositoryInterface;
import org.opentaps.domain.billing.payment.Payment;
import org.opentaps.domain.organization.Organization;
import org.opentaps.domain.organization.OrganizationRepositoryInterface;
import org.opentaps.foundation.entity.Entity;
import org.opentaps.foundation.exception.FoundationException;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.repository.ofbiz.Repository;


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

        HttpServletRequest request = (HttpServletRequest) context.get("request");
        GenericDelegator delegator = (GenericDelegator) context.get("delegator");
        Locale locale = (Locale) context.get("locale");
        LocalDispatcher dispatcher = (LocalDispatcher) context.get("dispatcher");
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Security security = (Security) context.get("security");

        ResourceBundleMapWrapper uiLabelMap = UtilMessage.getUiLabels(locale);
        String organizationPartyId = UtilCommon.getOrganizationPartyId(request);
        if (organizationPartyId == null) {
            return;
        }
        context.put("organizationPartyId", organizationPartyId);

        // get the view preference from the parameter
        String useGwtParam = UtilCommon.getParameter(request, "useGwt");
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
        context.put("useGwt", useGwt);

        // get the invoice from the domain
        String invoiceId = (String) context.get("invoiceId");
        DomainsLoader dl = new DomainsLoader(new Infrastructure(dispatcher), new User(userLogin));
        BillingDomainInterface billingDomain = dl.loadDomainsDirectory().getBillingDomain();
        InvoiceRepositoryInterface invoiceRepository = billingDomain.getInvoiceRepository();
        OrganizationRepositoryInterface organizationRepository = dl.loadDomainsDirectory().getOrganizationDomain().getOrganizationRepository();

        Invoice invoice = null;
        try {
            invoice = invoiceRepository.getInvoiceById(invoiceId);
        } catch (FoundationException e) {
            Debug.logError("No invoice found with ID [" + invoiceId + "]", MODULE);
            // let the invoice == null check deal with this
        }
        if (invoice == null) {
            context.put("decoratorLocation", "component://opentaps-common/widget/screens/common/CommonScreens.xml");
            return;
        }
        context.put("invoice", invoice);

        // put to history
        InvoiceType invoiceType = invoice.getInvoiceType();
        context.put("history", UtilCommon.makeHistoryEntry(UtilMessage.expandLabel("FinancialsNavHistoryInvoice", locale, UtilMisc.toMap("invoiceId", invoiceId, "invoiceTypeName", invoiceType.get("description", locale))), "viewInvoice", UtilMisc.toList("invoiceId")));

        // get the invoice items
        context.put("invoiceItems", invoice.getInvoiceItems());

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
            p.put("statusDescription", status.get("description", locale));
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
        context.put("payments", payments);
        context.put("creditPayments", creditPayments);

        // these booleans group the invoices into tabs
        context.put("isReceipt", invoice.isReceivable());
        context.put("isDisbursement", invoice.isPayable());
        context.put("isPartner", invoice.isPartnerInvoice());

        // note Partner invoice are considered receivable invoice, so test for that first
        if (invoice.isPartnerInvoice()) {
            context.put("decoratorLocation", "component://financials/widget/financials/screens/partners/PartnerScreens.xml");
        } else if (invoice.isPayable()) {
            context.put("decoratorLocation", "component://financials/widget/financials/screens/payables/PayablesScreens.xml");
        } else if (invoice.isReceivable()) {
            context.put("decoratorLocation", "component://financials/widget/financials/screens/receivables/ReceivablesScreens.xml");
        }

        // get the accounting tags for the invoice
        if (invoice.isCommissionInvoice()) {
            context.put("tagTypes", UtilAccountingTags.getAccountingTagsForOrganization(organizationPartyId, UtilAccountingTags.COMMISSION_INVOICES_TAG, delegator));
        } else if (invoice.isSalesInvoice()) {
            context.put("tagTypes", UtilAccountingTags.getAccountingTagsForOrganization(organizationPartyId, UtilAccountingTags.SALES_INVOICES_TAG, delegator));
        } else if (invoice.isPurchaseInvoice()) {
            context.put("tagTypes", UtilAccountingTags.getAccountingTagsForOrganization(organizationPartyId, UtilAccountingTags.PURCHASE_INVOICES_TAG, delegator));
        }

        context.put("billingPartyId", invoice.getTransactionPartyId());

        // the billing address, which can be either the payment or billing location
        // TODO: this should be moved into invoice repository / Invoice
        String invoiceContactMechId = null;
        PostalAddress invoiceAddress = null;
        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                                          EntityCondition.makeCondition(InvoiceContactMech.Fields.contactMechPurposeTypeId.name(), EntityOperator.EQUALS, "BILLING_LOCATION"),
                                          EntityCondition.makeCondition(InvoiceContactMech.Fields.invoiceId.name(), EntityOperator.EQUALS, invoice.getInvoiceId()));
        InvoiceContactMech invoiceContactMech = Entity.getFirst(invoiceRepository.findList(InvoiceContactMech.class, conditions));
        if (invoiceContactMech != null) {
            invoiceContactMechId = invoiceContactMech.getContactMechId();
            invoiceAddress = invoiceRepository.findOne(PostalAddress.class, invoiceRepository.map(PostalAddress.Fields.contactMechId, invoiceContactMech.getContactMechId()));
        } else {
            // if the address is not in InvoiceContactMech, use the billing address of the party
            GenericValue invoiceAddressGV = UtilFinancial.getBillingAddress(invoice.getTransactionPartyId(), delegator);
            if (invoiceAddressGV != null) {
                invoiceAddress = Repository.loadFromGeneric(PostalAddress.class, invoiceAddressGV);
                invoiceContactMechId = invoiceAddress.getContactMechId();
            }
        }
        context.put("invoiceAddress", invoiceAddress);
        context.put("invoiceContactMechId", invoiceContactMechId);

        // update permissions
        boolean hasUpdatePermission = false;
        boolean hasAdjustmentPermission = false;
        if ((invoice.isReceivable() && security.hasEntityPermission("FINANCIALS", "_AR_INUPDT", userLogin)) || (invoice.isPayable() && security.hasEntityPermission("FINANCIALS", "_AP_INUPDT", userLogin))) {
            hasUpdatePermission = invoice.isInProcess();
            hasAdjustmentPermission = invoice.isAdjustable();
        }
        context.put("hasUpdatePermission", hasUpdatePermission);
        context.put("hasAdjustmentPermission", hasAdjustmentPermission);

        // create permission
        boolean hasCreatePermission = security.hasEntityPermission("FINANCIALS", "_AP_INCRTE", userLogin) || security.hasEntityPermission("FINANCIALS", "_AR_INCRTE", userLogin);
        context.put("hasCreatePermission", hasCreatePermission);

        // writeoff permission
        boolean hasWriteoffPermission = false;
        if ((invoice.isReceivable() && (security.hasEntityPermission("FINANCIALS", "_AR_INWRTOF", userLogin))
             || !invoice.isReceivable() && (security.hasEntityPermission("FINANCIALS", "_AP_INWRTOF", userLogin)))
            && (invoice.isReady() || invoice.isPaid())) {
            hasWriteoffPermission = true;
        }
        context.put("hasWriteoffPermission", hasWriteoffPermission);

        // update permission implies that the header and items are editable, so get some data for the forms
        if (hasUpdatePermission) {
            List<GlAccountOrganizationAndClass> glAccounts = invoiceRepository.findListCache(GlAccountOrganizationAndClass.class, invoiceRepository.map(GlAccountOrganizationAndClass.Fields.organizationPartyId, organizationPartyId), UtilMisc.toList(GlAccountOrganizationAndClass.Fields.accountCode.name()));
            context.put("glAccounts", glAccounts);
            context.put("invoiceItemTypes", invoice.getApplicableInvoiceItemTypes());

            // party's billing and payment locations
            conditions = EntityCondition.makeCondition(EntityOperator.AND,
                                   EntityCondition.makeCondition(EntityOperator.OR,
                                       EntityCondition.makeCondition(PartyContactMechPurpose.Fields.contactMechPurposeTypeId.name(), EntityOperator.EQUALS, "BILLING_LOCATION"),
                                       EntityCondition.makeCondition(PartyContactMechPurpose.Fields.contactMechPurposeTypeId.name(), EntityOperator.EQUALS, "PAYMENT_LOCATION")),
                                   EntityCondition.makeCondition(PartyContactMechPurpose.Fields.partyId.name(), EntityOperator.EQUALS, invoice.getTransactionPartyId()),
                                   EntityUtil.getFilterByDateExpr());
            List<PartyContactMechPurpose> purposes = invoiceRepository.findList(PartyContactMechPurpose.class, conditions);
            List<PostalAddress> addresses = Entity.getRelated(PostalAddress.class, purposes);
            context.put("addresses", addresses);

            // available tax authorities
            List<TaxAuthorityAndDetail> taxAuthorities = invoiceRepository.findAllCache(TaxAuthorityAndDetail.class, UtilMisc.toList(TaxAuthorityAndDetail.Fields.abbreviation.name(), TaxAuthorityAndDetail.Fields.groupName.name()));
            context.put("taxAuthorities", taxAuthorities);
        }

        // Invoice terms and term types
        context.put("invoiceTerms", invoice.getInvoiceTerms());
        List<TermType> termTypes = organizationRepository.getValidTermTypes(invoice.getInvoiceTypeId());
        context.put("termTypes", termTypes);

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
                Set<String> orderCorrespondingPOs = Entity.getDistinctFieldValues(String.class, orderItems, OrderItem.Fields.correspondingPoId);
                if (UtilValidate.isNotEmpty(orderCorrespondingPOs)) {
                    ordersList += "(";
                    boolean first = true;
                    for (String poId : orderCorrespondingPOs) {
                        if (first) {
                            ordersList += uiLabelMap.get("OpentapsPONumber") + ":";
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
            context.put("ordersList", ordersList);
        }

        // billing accounts of the from party for Accounts Payable invoices
        if (invoice.isPayable()) {
            conditions = EntityCondition.makeCondition(EntityOperator.AND,
                          EntityCondition.makeCondition(BillingAccountAndRole.Fields.partyId.name(), EntityOperator.EQUALS, invoice.getPartyIdFrom()),
                          EntityCondition.makeCondition(BillingAccountAndRole.Fields.roleTypeId.name(), EntityOperator.EQUALS, "BILL_TO_CUSTOMER"),
                          EntityUtil.getFilterByDateExpr());
            List<BillingAccountAndRole> billingAccounts = invoiceRepository.findList(BillingAccountAndRole.class, conditions, UtilMisc.toList(BillingAccountAndRole.Fields.billingAccountId.name()));
            context.put("billingAccounts", billingAccounts);
        }

        // invoice adjustment types
        if (invoice.isAdjustable()) {
            Organization organization = organizationRepository.getOrganizationById(organizationPartyId);
            List<InvoiceAdjustmentType> types = invoiceRepository.getInvoiceAdjustmentTypes(organization, invoice);
            context.put("invoiceAdjustmentTypes", types);
        }

    }
}
