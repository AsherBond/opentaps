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

package com.opensourcestrategies.financials.payroll;

import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javolution.util.FastList;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.opentaps.common.event.AjaxEvents;
import org.opentaps.common.util.UtilCommon;

public class PayrollEvents {

    public static final String module = PayrollEvents.class.getName();

    /*************************************************************************/
    /**                                                                     **/
    /**                      Payroll JSON Requests                          **/
    /**                                                                     **/
    /*************************************************************************/


    /** Gets a list of payment (paycheck) options that are associated with a given partyId (employee). */
    public static String getPaymentTypeDataJSON(HttpServletRequest request, HttpServletResponse response) {
    	Delegator delegator = (Delegator) request.getAttribute("delegator");

        String organizationPartyId = UtilCommon.getOrganizationPartyId(request);
        String partyId = (String) request.getParameter("partyId");

        try {
        	PayrollHelper payrollHelper = new PayrollHelper(organizationPartyId, delegator);
        	Collection<GenericValue> paymentTypeOptions = (Collection<GenericValue>) EntityUtil.getRelated("PaymentType", (List<GenericValue>) payrollHelper.getAvailablePaycheckTypes(UtilMisc.toList(partyId)));
            return AjaxEvents.doJSONResponse(response, paymentTypeOptions);
        } catch (GenericEntityException e) {
            return AjaxEvents.doJSONResponse(response, FastList.newInstance());
        }
    }
    
}
