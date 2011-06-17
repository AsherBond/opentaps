/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import javolution.util.FastList;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.opentaps.base.constants.RoleTypeConstants;
import org.opentaps.base.entities.BillingAccountAndRole;
import org.opentaps.base.entities.Party;
import org.opentaps.common.party.PartyHelper;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.billing.BillingDomainInterface;
import org.opentaps.domain.billing.invoice.InvoiceRepositoryInterface;
import org.opentaps.foundation.action.ActionContext;

public class AccountsActions {

    private static final String MODULE = AccountsActions.class.getName();

    public AccountsActions() { };

    public static void findCustomerBillAccount(Map<String, Object> context) throws GeneralException {
        ActionContext ac = new ActionContext(context);
        HttpServletRequest request = ac.getRequest();

        String customerPartyId  = request.getParameter("customerPartyId");
        String billingAccountId = request.getParameter("billingAccountId");
        String performFind = request.getParameter("performFind");

        List<BillingAccountAndRole> billingAccounts = null;
        List<Map<String, Object>> itemList = FastList.<Map<String, Object>>newInstance();

        if (performFind != null && performFind.equalsIgnoreCase("Y")) {
            List<EntityCondition> conditions = UtilMisc.toList(
                    EntityCondition.makeCondition(BillingAccountAndRole.Fields.roleTypeId.name(), RoleTypeConstants.BILL_TO_CUSTOMER),
                    EntityUtil.getFilterByDateExpr(UtilDateTime.nowTimestamp(), BillingAccountAndRole.Fields.accountFromDate.name(), BillingAccountAndRole.Fields.accountThruDate.name())
                    );

            if (billingAccountId != null && !billingAccountId.isEmpty()) {
                conditions.add(EntityCondition.makeCondition(BillingAccountAndRole.Fields.billingAccountId.name(), billingAccountId));
            }

            if (customerPartyId != null && !customerPartyId.isEmpty()) {
                conditions.add(EntityCondition.makeCondition(BillingAccountAndRole.Fields.partyId.name(), customerPartyId));
            }

            EntityCondition condition = EntityCondition.makeCondition(conditions, EntityOperator.AND);

            DomainsDirectory dd = DomainsDirectory.getDomainsDirectory(ac);
            BillingDomainInterface billingDomain = dd.getBillingDomain();
            InvoiceRepositoryInterface invoiceRepository = billingDomain.getInvoiceRepository();

            billingAccounts = invoiceRepository.findList(BillingAccountAndRole.class, condition);

            if (billingAccounts != null) {
                for (BillingAccountAndRole account : billingAccounts) {
                    Party party = account.getParty();
                    Map<String, Object> itemLine = account.toMap();
                    if (party != null) {
                        String partyName = PartyHelper.getPartyName(party);
                        itemLine.put("partyName", partyName);
                    }

                    itemList.add(itemLine);
                }
            }
        }

        ac.put("billingAccounts", itemList);
    }

}
