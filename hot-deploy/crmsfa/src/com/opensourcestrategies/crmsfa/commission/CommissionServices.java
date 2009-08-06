/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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

package com.opensourcestrategies.crmsfa.commission;

import javolution.util.FastList;
import javolution.util.FastSet;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.agreement.AgreementInvoiceFactory;
import org.opentaps.common.agreement.UtilAgreement;

import java.math.BigDecimal;
import java.util.*;

/**
 * CommissionServices - Services for commissions
 *
 * @author     <a href="mailto:leon@opentaps.org">Leon Torres</a> 
 */
public class CommissionServices {

    public static String module = CommissionServices.class.getName();

    private static BigDecimal ZERO = new BigDecimal("0");
    private static int decimals = UtilNumber.getBigDecimalScale("invoice.decimals");
    private static int rounding = UtilNumber.getBigDecimalRoundingMode("invoice.rounding");
    public static final String resource = "FinancialsUiLabels";

    /*
     * The values of the agreement are captured all in one view entity:  AgreementAndItemAndTerm.
     * If an agent gets a commission for all orders, then term is COMM_ORDER_ROLE with roleTypeId COMMISSION_AGENT.
     * If an agent gets commission on orders with a customer, then term is COMM_PARTY_APPL with partyId DemoCustCompany 
     */
    public static Map createCommissionInvoices(DispatchContext dctx, Map context) {
        GenericDelegator delegator = dctx.getDelegator();
        Security security = dctx.getSecurity();

        String parentInvoiceId = (String) context.get("invoiceId");
        List invoiceIds = FastList.newInstance();
        Map results = ServiceUtil.returnSuccess();
        try {
            GenericValue parentInvoice = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", parentInvoiceId));

            // At the moment, we only support sales invoices.  This service should not cause an error if an unsupported invoice 
            // is supplied because SECAs that implement this service may not be able to check the type.
            if (!"SALES_INVOICE".equals(parentInvoice.get("invoiceTypeId"))) {
                Debug.logInfo("Invoice ["+parentInvoiceId+"] is not a sales invoice and will not be commissioned.", module);
                results.put("invoiceIds", invoiceIds);
                return results;
            }

            Set<String> agentIds = FastSet.newInstance();

            // get any agents with InvoiceRole
            List<GenericValue> agents = parentInvoice.getRelatedByAnd("InvoiceRole", UtilMisc.toMap("roleTypeId", "COMMISSION_AGENT"));
            for (GenericValue agent : agents) {
                agentIds.add( agent.getString("partyId") );
            }

            // get any agents that can earn commission for this party
            agentIds.addAll( UtilAgreement.getCommissionAgentIdsForCustomer(parentInvoice.getString("partyIdFrom"), parentInvoice.getString("partyId"), delegator) );

            // create a commission invoice for each commission agent associated with the invoice
            for (String agentId : agentIds) {
                List agentInvoiceIds = createCommissionInvoicesForAgent(dctx, context, parentInvoice, agentId);
                invoiceIds.addAll(agentInvoiceIds);
            }

            results.put("invoiceIds", invoiceIds);
            return results;
        } catch (GeneralException e) {
            Debug.logError(e, module);
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    private static List createCommissionInvoicesForAgent(DispatchContext dctx, Map context, GenericValue parentInvoice, String agentPartyId) throws GeneralException {
        GenericDelegator delegator = dctx.getDelegator();
        String organizationPartyId = parentInvoice.getString("partyIdFrom");
        List invoiceIds = FastList.newInstance();

        // process each unexpired agreement separately
        List conditions = UtilMisc.toList(
                new EntityExpr("partyIdFrom", EntityOperator.EQUALS, organizationPartyId),
                new EntityExpr("roleTypeIdFrom", EntityOperator.EQUALS, "INTERNAL_ORGANIZATIO"),
                new EntityExpr("partyIdTo", EntityOperator.EQUALS, agentPartyId),
                new EntityExpr("roleTypeIdTo", EntityOperator.EQUALS, "COMMISSION_AGENT"),
                new EntityExpr("agreementTypeId", EntityOperator.EQUALS, "COMMISSION_AGREEMENT"),
                EntityUtil.getFilterByDateExpr()
                );
        conditions.add(new EntityExpr("statusId", EntityOperator.EQUALS, "AGR_ACTIVE"));
        List agreements = delegator.findByAnd("Agreement", conditions, UtilMisc.toList("fromDate ASC"));
        if (agreements.size() == 0) return invoiceIds;
        for (Iterator iter = agreements.iterator(); iter.hasNext(); ) {
            GenericValue agreement = (GenericValue) iter.next();
            if (UtilAgreement.isInvoiceCoveredByAgreement(parentInvoice, agreement) && ! UtilAgreement.isCommissionEarnedOnPayment(agreement)) {
                Map results = AgreementInvoiceFactory.createInvoiceFromAgreement(dctx, context, agreement, Arrays.asList(parentInvoice),
                        "COMMISSION_INVOICE", "COMMISSION_AGENT", parentInvoice.getString("currencyUomId"), true, false);
                if (ServiceUtil.isError(results)) {
                    Debug.logWarning("Failed to create commission invoice line item for agent ["+agentPartyId+"] from agreement ["+agreement.get("agreementId")+"]: " + ServiceUtil.getErrorMessage(results), module);
                    continue;
                }
                String invoiceId = (String) results.get("invoiceId");
                if (invoiceId != null) invoiceIds.add( invoiceId );
            }
        }
        return invoiceIds;
    }
}
