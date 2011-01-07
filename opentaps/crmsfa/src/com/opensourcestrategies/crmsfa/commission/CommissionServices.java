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

package com.opensourcestrategies.crmsfa.commission;

import java.math.BigDecimal;
import java.util.*;

import javolution.util.FastList;
import javolution.util.FastSet;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilNumber;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.agreement.AgreementInvoiceFactory;
import org.opentaps.common.agreement.UtilAgreement;

/**
 * CommissionServices - Services for commissions.
 *
 * @author     <a href="mailto:leon@opentaps.org">Leon Torres</a>
 */
public final class CommissionServices {

    private CommissionServices() { }

    private static String MODULE = CommissionServices.class.getName();

    /*
     * The values of the agreement are captured all in one view entity:  AgreementAndItemAndTerm.
     * If an agent gets a commission for all orders, then term is COMM_ORDER_ROLE with roleTypeId COMMISSION_AGENT.
     * If an agent gets commission on orders with a customer, then term is COMM_PARTY_APPL with partyId DemoCustCompany
     */
    public static Map<String, Object> createCommissionInvoices(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();

        String parentInvoiceId = (String) context.get("invoiceId");
        List<String> invoiceIds = FastList.newInstance();
        Map<String, Object> results = ServiceUtil.returnSuccess();
        try {
            GenericValue parentInvoice = delegator.findByPrimaryKey("Invoice", UtilMisc.toMap("invoiceId", parentInvoiceId));

            // At the moment, we only support sales invoices.  This service should not cause an error if an unsupported invoice
            // is supplied because SECAs that implement this service may not be able to check the type.
            if (!"SALES_INVOICE".equals(parentInvoice.get("invoiceTypeId"))) {
                Debug.logInfo("Invoice [" + parentInvoiceId + "] is not a sales invoice and will not be commissioned.", MODULE);
                results.put("invoiceIds", invoiceIds);
                return results;
            }

            Set<String> agentIds = FastSet.newInstance();

            // get any agents with InvoiceRole
            List<GenericValue> agents = parentInvoice.getRelatedByAnd("InvoiceRole", UtilMisc.toMap("roleTypeId", "COMMISSION_AGENT"));
            for (GenericValue agent : agents) {
                agentIds.add(agent.getString("partyId"));
            }

            // get any agents that can earn commission for this party
            agentIds.addAll(UtilAgreement.getCommissionAgentIdsForCustomer(parentInvoice.getString("partyIdFrom"), parentInvoice.getString("partyId"), delegator));

            // create a commission invoice for each commission agent associated with the invoice
            for (String agentId : agentIds) {
                List<String> agentInvoiceIds = createCommissionInvoicesForAgent(dctx, context, parentInvoice, agentId);
                invoiceIds.addAll(agentInvoiceIds);
            }

            results.put("invoiceIds", invoiceIds);
            return results;
        } catch (GeneralException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }
    }

    private static List<String> createCommissionInvoicesForAgent(DispatchContext dctx, Map<String, Object> context, GenericValue parentInvoice, String agentPartyId) throws GeneralException {
        Delegator delegator = dctx.getDelegator();
        String organizationPartyId = parentInvoice.getString("partyIdFrom");
        List<String> invoiceIds = FastList.newInstance();

        // process each unexpired agreement separately
        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, organizationPartyId),
                EntityCondition.makeCondition("roleTypeIdFrom", EntityOperator.EQUALS, "INTERNAL_ORGANIZATIO"),
                EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, agentPartyId),
                EntityCondition.makeCondition("roleTypeIdTo", EntityOperator.EQUALS, "COMMISSION_AGENT"),
                EntityCondition.makeCondition("agreementTypeId", EntityOperator.EQUALS, "COMMISSION_AGREEMENT"),
                EntityUtil.getFilterByDateExpr(),
                EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "AGR_ACTIVE")
                );
        List<GenericValue> agreements = delegator.findList("Agreement", conditions, null, UtilMisc.toList("fromDate ASC"), null, false);
        if (agreements.size() == 0) {
            return invoiceIds;
        }
        for (GenericValue agreement : agreements) {
            if (UtilAgreement.isInvoiceCoveredByAgreement(parentInvoice, agreement) && !UtilAgreement.isCommissionEarnedOnPayment(agreement)) {
                Map<String, Object> results = AgreementInvoiceFactory.createInvoiceFromAgreement(dctx, context, agreement, Arrays.asList(parentInvoice),
                        "COMMISSION_INVOICE", "COMMISSION_AGENT", parentInvoice.getString("currencyUomId"), true, false);
                if (ServiceUtil.isError(results)) {
                    Debug.logWarning("Failed to create commission invoice line item for agent [" + agentPartyId + "] from agreement [" + agreement.get("agreementId") + "]: " + ServiceUtil.getErrorMessage(results), MODULE);
                    continue;
                }
                String invoiceId = (String) results.get("invoiceId");
                if (invoiceId != null) {
                    invoiceIds.add(invoiceId);
                }
            }
        }
        return invoiceIds;
    }
}
