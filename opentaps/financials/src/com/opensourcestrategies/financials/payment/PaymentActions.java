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

package com.opensourcestrategies.financials.payment;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import javax.servlet.http.HttpServletRequest;

import com.opensourcestrategies.financials.util.UtilFinancial;
import javolution.util.FastList;
import javolution.util.FastMap;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityFunction;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.party.party.PartyHelper;
import org.opentaps.base.constants.PaymentTypeConstants;
import org.opentaps.base.constants.StatusTypeConstants;
import org.opentaps.base.entities.PaymentAndPaymentApplication;
import org.opentaps.base.entities.PaymentMethod;
import org.opentaps.base.entities.PaymentMethodType;
import org.opentaps.base.entities.PaymentType;
import org.opentaps.base.entities.StatusItem;
import org.opentaps.common.builder.EntityListBuilder;
import org.opentaps.common.builder.PageBuilder;
import org.opentaps.common.util.UtilAccountingTags;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilDate;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.billing.BillingDomainInterface;
import org.opentaps.domain.billing.payment.Payment;
import org.opentaps.domain.billing.payment.PaymentRepositoryInterface;
import org.opentaps.domain.organization.Organization;
import org.opentaps.domain.organization.OrganizationRepositoryInterface;
import org.opentaps.foundation.action.ActionContext;

/**
 * PaymentActions - Java Actions for payments.
 */
public final class PaymentActions {

    private static final String MODULE = PaymentActions.class.getName();

    private PaymentActions() { }

    /**
     * Action for the find / list payments screen.
     * @param context the screen context
     * @throws GeneralException if an error occurs
     * @throws ParseException if an error occurs
     */
    public static void findPayments(Map<String, Object> context) throws GeneralException, ParseException {

        final ActionContext ac = new ActionContext(context);

        final HttpServletRequest request = ac.getRequest();
        final Delegator delegator = ac.getDelegator();
        final Locale locale = ac.getLocale();
        final TimeZone timeZone = ac.getTimeZone();

        final String organizationPartyId = UtilCommon.getOrganizationPartyId(request);
        if (organizationPartyId == null) {
            Debug.logError("No organizationPartyId set in the current request.", MODULE);
            return;
        }
        ac.put("organizationPartyId", organizationPartyId);

        DomainsDirectory dd = DomainsDirectory.getDomainsDirectory(ac);
        BillingDomainInterface billingDomain = dd.getBillingDomain();
        PaymentRepositoryInterface paymentRepository = billingDomain.getPaymentRepository();
        OrganizationRepositoryInterface organizationRepository = dd.getOrganizationDomain().getOrganizationRepository();

        Organization organization = organizationRepository.getOrganizationById(organizationPartyId);


        // this gets overrided later according to the payment type
        ac.put("decoratorLocation", "component://opentaps-common/widget/screens/common/CommonScreens.xml");

        // set the disbursement flag which is used to set the partyIdFrom/To to that of the organization on the find payment form as a hidden field
        // also set a default status id which is based on whether it's a disbursement (SENT) or not (RECEIVED), but this is overriden by parameters.statusId
        ac.put("headerItem", "receivables");
        // for accounting tag filtering
        String tagsType = UtilAccountingTags.LOOKUP_RECEIPT_PAYMENT_TAG;
        boolean findDisbursement = false;
        String findPaymentTypeId = ac.getParameter("findPaymentTypeId");
        if ("DISBURSEMENT".equals(findPaymentTypeId)) {
            findDisbursement = true;
            tagsType = UtilAccountingTags.LOOKUP_DISBURSEMENT_PAYMENT_TAG;
            ac.put("headerItem", "payables");
        }
        ac.put("findDisbursement", findDisbursement);

        // get the list of paymentMethods, PaymentMethodTypes, paymentTypes

        List<String> supportedPaymentTypes = null;
        if (findDisbursement) {
            ac.put("decoratorLocation", "component://financials/widget/financials/screens/payables/PayablesScreens.xml");
            ac.put("headerItem", "payables");
            ac.put("paymentMethodList", organization.getRelated(PaymentMethod.class, UtilMisc.toList("paymentMethodTypeId")));
            supportedPaymentTypes = Arrays.asList(PaymentTypeConstants.Disbursement.CUSTOMER_REFUND,
                                                  PaymentTypeConstants.Disbursement.VENDOR_PAYMENT,
                                                  PaymentTypeConstants.Disbursement.VENDOR_PREPAY,
                                                  PaymentTypeConstants.Disbursement.COMMISSION_PAYMENT,
                                                  PaymentTypeConstants.TaxPayment.SALES_TAX_PAYMENT,
                                                  PaymentTypeConstants.TaxPayment.INCOME_TAX_PAYMENT,
                                                  PaymentTypeConstants.TaxPayment.PAYROLL_TAX_PAYMENT);
        } else {
            ac.put("decoratorLocation", "component://financials/widget/financials/screens/receivables/ReceivablesScreens.xml");
            ac.put("headerItem", "receivables");
            ac.put("paymentMethodTypeList", UtilFinancial.getSimpleCustomerPaymentMethodTypes(delegator));
            supportedPaymentTypes = Arrays.asList(PaymentTypeConstants.Receipt.INTEREST_RECEIPT,
                                                  PaymentTypeConstants.Receipt.VENDOR_CREDIT_RCPT,
                                                  PaymentTypeConstants.Receipt.CUSTOMER_PAYMENT,
                                                  PaymentTypeConstants.Receipt.CUSTOMER_DEPOSIT);
        }
        List<PaymentType> paymentTypeList = paymentRepository.findListCache(PaymentType.class, EntityCondition.makeCondition(PaymentType.Fields.paymentTypeId.name(), EntityOperator.IN, supportedPaymentTypes));
        ac.put("paymentTypeList", paymentTypeList);

        List<StatusItem> statusList = paymentRepository.findListCache(StatusItem.class, paymentRepository.map(StatusItem.Fields.statusTypeId, StatusTypeConstants.PMNT_STATUS), UtilMisc.toList(StatusItem.Fields.sequenceId.desc()));
        ac.put("statusList", statusList);

        // get the accounting tags for the select inputs
        if (tagsType != null) {
            ac.put("tagFilters", UtilAccountingTags.getAccountingTagFiltersForOrganization(organizationPartyId, tagsType, delegator, locale));
        }

        // possible fields we're searching by
        String paymentId = ac.getParameter("paymentId");
        String partyIdFrom = ac.getParameter("partyIdFrom");
        String partyIdTo = ac.getParameter("partyIdTo");
        String paymentTypeId = ac.getParameter("paymentTypeId");
        String paymentMethodId = ac.getParameter("paymentMethodId");
        String paymentMethodTypeId = ac.getParameter("paymentMethodTypeId");
        String paymentRefNum = ac.getParameter("paymentRefNum");
        String statusId = ac.getParameter("statusId");
        Timestamp fromDate = UtilDate.toTimestamp(ac.getParameter("fromDate"), timeZone, locale);
        Timestamp thruDate = UtilDate.toTimestamp(ac.getParameter("thruDate"), timeZone, locale);
        String amountFrom = ac.getParameter("amountFrom");
        String amountThru = ac.getParameter("amountThru");
        String openAmountFrom = ac.getParameter("openAmountFrom");
        String openAmountThru = ac.getParameter("openAmountThru");

        // construct search conditions
        List<EntityCondition> searchConditions = new FastList<EntityCondition>();
        if (paymentId != null) {
            searchConditions.add(EntityCondition.makeCondition(Payment.Fields.paymentId.name(), EntityOperator.EQUALS, paymentId));
        }

        // force one of the party to the organization according to the payment type
        if (findDisbursement) {
            searchConditions.add(EntityCondition.makeCondition(Payment.Fields.partyIdFrom.name(), EntityOperator.EQUALS, organizationPartyId));
            if (partyIdTo != null) {
                searchConditions.add(EntityCondition.makeCondition(Payment.Fields.partyIdTo.name(), EntityOperator.EQUALS, partyIdTo));
            }
        } else {
            searchConditions.add(EntityCondition.makeCondition(Payment.Fields.partyIdTo.name(), EntityOperator.EQUALS, organizationPartyId));
            if (partyIdFrom != null) {
                searchConditions.add(EntityCondition.makeCondition(Payment.Fields.partyIdFrom.name(), EntityOperator.EQUALS, partyIdFrom));
            }
        }

        if (paymentTypeId != null) {
            searchConditions.add(EntityCondition.makeCondition(Payment.Fields.paymentTypeId.name(), EntityOperator.EQUALS, paymentTypeId));
        } else {
            searchConditions.add(EntityCondition.makeCondition(Payment.Fields.paymentTypeId.name(), EntityOperator.IN, supportedPaymentTypes));
        }

        if (findDisbursement) {
            if (paymentMethodId != null) {
                searchConditions.add(EntityCondition.makeCondition(Payment.Fields.paymentMethodId.name(), EntityOperator.EQUALS, paymentMethodId));
            }
        } else {
            if (paymentMethodTypeId != null) {
                searchConditions.add(EntityCondition.makeCondition(Payment.Fields.paymentMethodTypeId.name(), EntityOperator.EQUALS, paymentMethodTypeId));
            }
        }

        if (paymentRefNum != null) {
            // make sure the look up is case insensitive
            searchConditions.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD(Payment.Fields.paymentRefNum.name()), EntityOperator.LIKE, EntityFunction.UPPER(paymentRefNum + "%")));
        }

        if (statusId != null) {
            searchConditions.add(EntityCondition.makeCondition(Payment.Fields.statusId.name(), EntityOperator.EQUALS, statusId));
        }
        if (fromDate != null) {
            searchConditions.add(EntityCondition.makeCondition(Payment.Fields.effectiveDate.name(), EntityOperator.GREATER_THAN_EQUAL_TO, fromDate));
        }
        if (thruDate != null) {
            searchConditions.add(EntityCondition.makeCondition(Payment.Fields.effectiveDate.name(), EntityOperator.LESS_THAN_EQUAL_TO, thruDate));
        }
        if (amountFrom != null) {
            searchConditions.add(EntityCondition.makeCondition(Payment.Fields.amount.name(), EntityOperator.GREATER_THAN_EQUAL_TO, new BigDecimal(amountFrom)));
        }
        if (amountThru != null) {
            searchConditions.add(EntityCondition.makeCondition(Payment.Fields.amount.name(), EntityOperator.LESS_THAN_EQUAL_TO, new BigDecimal(amountThru)));
        }
        if (openAmountFrom != null) {
            searchConditions.add(EntityCondition.makeCondition(Payment.Fields.openAmount.name(), EntityOperator.GREATER_THAN_EQUAL_TO, new BigDecimal(openAmountFrom)));
        }
        if (openAmountThru != null) {
            searchConditions.add(EntityCondition.makeCondition(Payment.Fields.openAmount.name(), EntityOperator.LESS_THAN_EQUAL_TO, new BigDecimal(openAmountThru)));
        }

        if (tagsType != null) {
            // if the organization is using allocatePaymentTagsToApplications then the tags are on the payment applications,
            // else they are on the Payments
            if (organization.allocatePaymentTagsToApplications()) {
                searchConditions.addAll(UtilAccountingTags.buildTagConditions(organizationPartyId, tagsType, delegator, request, UtilAccountingTags.TAG_PARAM_PREFIX, "applAcctgTagEnumId"));
            } else {
                searchConditions.addAll(UtilAccountingTags.buildTagConditions(organizationPartyId, tagsType, delegator, request));
            }
        }

        // Pagination
        Set<String> fieldsToSelect = new HashSet<String>(new Payment().getAllFieldsNames());
        fieldsToSelect.retainAll(new PaymentAndPaymentApplication().getAllFieldsNames());
        EntityListBuilder paymentListBuilder = new EntityListBuilder(paymentRepository, PaymentAndPaymentApplication.class, EntityCondition.makeCondition(searchConditions, EntityOperator.AND), fieldsToSelect, UtilMisc.toList(PaymentAndPaymentApplication.Fields.effectiveDate.desc()));
        PageBuilder<PaymentAndPaymentApplication> pageBuilder = new PageBuilder<PaymentAndPaymentApplication>() {
            public List<Map<String, Object>> build(List<PaymentAndPaymentApplication> page) throws Exception {
                Delegator delegator = ac.getDelegator();
                List<Map<String, Object>> newPage = FastList.newInstance();
                for (PaymentAndPaymentApplication payment : page) {
                    Map<String, Object> newRow = FastMap.newInstance();
                    newRow.putAll(payment.toMap());

                    StatusItem status = payment.getStatusItem();
                    newRow.put("statusDescription", status.get(StatusItem.Fields.description.name(), locale));
                    PaymentMethodType meth = payment.getPaymentMethodType();
                    if (meth != null) {
                        newRow.put("paymentMethodDescription", meth.get(PaymentMethodType.Fields.description.name(), locale));
                    }

                    newRow.put("partyNameFrom", PartyHelper.getPartyName(delegator, payment.getPartyIdFrom(), false));
                    newRow.put("partyNameTo", PartyHelper.getPartyName(delegator, payment.getPartyIdTo(), false));

                    newPage.add(newRow);
                }
                return newPage;
            }
        };
        paymentListBuilder.setPageBuilder(pageBuilder);
        ac.put("paymentListBuilder", paymentListBuilder);
    }

}
