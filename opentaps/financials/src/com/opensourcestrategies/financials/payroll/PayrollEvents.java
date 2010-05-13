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

package com.opensourcestrategies.financials.payroll;

import java.util.*;

import net.sf.json.JSONObject;
import net.sf.json.JSONArray;
import net.sf.json.util.JSONBuilder;
import net.sf.json.util.JSONUtils;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.entity.util.*;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;

import org.opentaps.common.event.AjaxEvents;
import com.opensourcestrategies.financials.payroll.PayrollHelper;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import javolution.util.FastList;

public class PayrollEvents {

    public static final String module = PayrollEvents.class.getName();

    /*************************************************************************/
    /**                                                                     **/
    /**                      Payroll JSON Requests                          **/
    /**                                                                     **/
    /*************************************************************************/


    /** Gets a list of payment (paycheck) options that are associated with a given partyId (employee). */
    public static String getPaymentTypeDataJSON(HttpServletRequest request, HttpServletResponse response) {
    	HttpSession session = (HttpSession)request.getSession();
        GenericDelegator delegator = (GenericDelegator) request.getAttribute("delegator");
        
        String organizationPartyId = (String)session.getAttribute("organizationPartyId");
        String partyId = (String) request.getParameter("partyId");

        try {
        	PayrollHelper payrollHelper = new PayrollHelper(organizationPartyId, delegator);
        	Collection paymentTypeOptions = (Collection)EntityUtil.getRelated("PaymentType", (List)payrollHelper.getAvailablePaycheckTypes(UtilMisc.toList(partyId)));
            return AjaxEvents.doJSONResponse(response, paymentTypeOptions);
        } catch (GenericEntityException e) {
            return AjaxEvents.doJSONResponse(response, FastList.newInstance());
        }
    }
    
}
