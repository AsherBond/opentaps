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

package org.opentaps.purchasing.supplier;

import java.util.Map;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.order.shoppingcart.ShoppingCart;
import org.opentaps.common.agreement.UtilAgreement;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.party.Party;
import org.opentaps.domain.party.PartyRepositoryInterface;
import org.opentaps.foundation.action.ActionContext;
import org.opentaps.foundation.entity.Entity;
import org.opentaps.purchasing.security.PurchasingSecurity;

/**
 * SupplierActions - Java Actions for suppliers.
 */
public final class SupplierActions {

    @SuppressWarnings("unused")
    private static final String MODULE = SupplierActions.class.getName();

    private SupplierActions() { }

    /**
     * Action for the view supplier screen.
     * @param context the screen context
     * @throws GeneralException if an error occurs
     */
    public static void viewSupplier(Map<String, Object> context) throws GeneralException {

        ActionContext ac = new ActionContext(context);
        DomainsDirectory dd = DomainsDirectory.getDomainsDirectory(ac);
        PurchasingSecurity purchasingSecurity = new PurchasingSecurity(ac.getSecurity(), ac.getUserLogin());
        PartyRepositoryInterface partyRepository = dd.getPartyDomain().getPartyRepository();

        // get the current organization party
        String organizationPartyId = UtilCommon.getOrganizationPartyId(ac.getRequest());

        // get the supplier party
        String partyId = ac.getParameter("partyId");
        Party supplier = partyRepository.getPartyById(partyId);

        // make sure that the partyId is actually a SUPPLIER before trying to display it as one
        if (!supplier.isSupplier()) {
            ac.put("validView", false);
            return;
        }
        ac.put("supplierPartyId", partyId);
        ac.put("partySummary", supplier.getCompleteView());

        // put to history
        ac.put("history", UtilCommon.makeHistoryEntry(supplier.getGroupName(), "viewSupplier", UtilMisc.toList("partyId")));

        // for the viewprofile.bsh to fetch the related contact mechs
        ac.getRequest().setAttribute("displayContactMechs", "Y");

        // get the supplier notes, need to be as Maps for the ofbiz Form to work
        ac.put("notesListIt", Entity.toMaps(supplier.getNotes()));

        // get view permissions
        boolean hasUpdatePermission = false;
        boolean hasViewOrderPermission = false;
        boolean hasCreateOrderPermission = false;
        if (organizationPartyId != null) {
            hasUpdatePermission = purchasingSecurity.hasPartyRelationSecurity("PRCH_SPLR", "_UPDATE", organizationPartyId);
            hasViewOrderPermission = purchasingSecurity.hasPartyRelationSecurity("PRCH_PO", "_VIEW", organizationPartyId);
            hasCreateOrderPermission = purchasingSecurity.hasPartyRelationSecurity("PRCH_PO", "_CREATE", organizationPartyId);
        }
        ac.put("hasUpdatePermission", hasUpdatePermission);
        ac.put("hasViewOrderPermission", hasViewOrderPermission);
        ac.put("hasCreateOrderPermission", hasCreateOrderPermission);

        // whether we should display [Create Order] which destroys any existing cart or [Resume Order] to continue an order
        ShoppingCart cart = (ShoppingCart) ac.getRequest().getSession().getAttribute("shoppingCart");
        if (cart != null) {
            ac.put("continueOrder", true);
        } else {
            ac.put("continueOrder", false);
        }

        ac.put("validView", true);
    }

    /**
     * Action for the view supplier screen.
     * @param context the screen context
     * @throws GeneralException if an error occurs
     */
    public static void supplierPurchasingAgreements(Map<String, Object> context) throws GeneralException {

        ActionContext ac = new ActionContext(context);
        DomainsDirectory dd = DomainsDirectory.getDomainsDirectory(ac);
        PartyRepositoryInterface partyRepository = dd.getPartyDomain().getPartyRepository();
        String organizationPartyId = UtilCommon.getOrganizationPartyId(ac.getRequest());
        if (organizationPartyId == null) {
            return;
        }

        String supplierPartyId = ac.getParameter("partyId");
        Party supplier = partyRepository.getPartyById(supplierPartyId);

        // Check if the partyId is a SUPPLIER
        if (!supplier.isSupplier()) {
            ac.put("validView", false);
            return;
        }

        /*
         * Get list of agreements between organization and current supplier
         */
        EntityCondition condition = EntityCondition.makeCondition(EntityCondition.makeCondition("partyIdFrom", EntityOperator.EQUALS, organizationPartyId),
                                                                  EntityCondition.makeCondition("partyIdTo", EntityOperator.EQUALS, supplierPartyId),
                                                                  EntityUtil.getFilterByDateExpr());

        ac.put("agreementsPaginatorName", "supplierAgreements");
        ac.put("agreementsListBuilder", UtilAgreement.getAgreementsListBuilder(condition, UtilMisc.toList("fromDate"), ac.getLocale()));
    }

}
