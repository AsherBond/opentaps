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
/* Copyright (c) Open Source Strategies, Inc. */

/*
 *  $Id:$
 *
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.opensourcestrategies.crmsfa.util;

import javolution.util.FastMap;
import org.ofbiz.base.util.*;
import org.ofbiz.base.util.collections.ResourceBundleMapWrapper;
import org.ofbiz.common.login.LoginServices;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.party.contact.ContactHelper;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import com.opensourcestrategies.crmsfa.party.PartyHelper;

/**
 * UtilCommon - A place for common crmsfa helper methods
 *
 * @author     <a href="mailto:leon@opensourcestrategies.com">Leon Torres</a>
 * @version    $Rev: 586 $
 */
public class UtilCommon {

    public static final String module = UtilCommon.class.getName();
    public static final String uiResource = "CRMSFAUiLabels";
    public static final String errorResource = "CRMSFAErrorLabels";
    public static final String opentapsErrorResource = "OpentapsErrorLabels";

    // uiLabelMap sets used by CRMSFA, keyed by Locale
    private static Map UI_LABELS = FastMap.newInstance();
    private static Map ERROR_LABELS = FastMap.newInstance();



    /** Get the uiLabelMap for User Interface labels.  Useful for code that doesn't have access to the global context. */
    public static ResourceBundleMapWrapper getUiLabels(Locale locale) {
        ResourceBundleMapWrapper localizedLabels = (ResourceBundleMapWrapper) UI_LABELS.get(locale);
        if (localizedLabels == null) {
            localizedLabels = (ResourceBundleMapWrapper) UtilProperties.getResourceBundleMap(uiResource, locale);
            localizedLabels.addBottomResourceBundle(UtilProperties.getResourceBundle("PartyUiLabels", locale));
            localizedLabels.addBottomResourceBundle(UtilProperties.getResourceBundle("CommonUiLabels", locale));
            UI_LABELS.put(locale, localizedLabels);
        }
        return localizedLabels;
    }

    /** Get the uiLabelMap for errors.  Useful for code that doesn't have access to the global context. */
    public static ResourceBundleMapWrapper getErrorLabels(Locale locale) {
        ResourceBundleMapWrapper localizedLabels = (ResourceBundleMapWrapper) ERROR_LABELS.get(locale);
        if (localizedLabels == null) {
            localizedLabels = (ResourceBundleMapWrapper) UtilProperties.getResourceBundleMap(opentapsErrorResource, locale);
            localizedLabels.addBottomResourceBundle(errorResource);
            ERROR_LABELS.put(locale, localizedLabels);
        }
        return localizedLabels;
    }


    /************************************************************************/
    /**                          Time Methods                              **/
    /************************************************************************/



    /**
     * Returns a true if the time period has passed or is closed.
     * @param customTimePeriodId a <code>String</code> value
     * @param delegator a <code>Delegator</code> value
     * @return a <code>boolean</code> value
     * @exception GenericEntityException if an error occurs
     */
    public static boolean isTimePeriodOpen(String customTimePeriodId, Delegator delegator) throws GenericEntityException {

        // first check time period -- is closed or passed?  if so, cannot create a forecast
        GenericValue timePeriod = delegator.findByPrimaryKeyCache("CustomTimePeriod", UtilMisc.toMap("customTimePeriodId", customTimePeriodId));
        if (timePeriod == null) {
            return false;
        }
        if (timePeriod.getString("isClosed") == null) {
            Debug.logWarning("Time period [" + customTimePeriodId + "] has no isClosed flag set--please set it", module);
            return false; // really sholdn't have this case, so let's return a false
        } else if (timePeriod.getString("isClosed").equals("Y")) {
            return false;
        }
        if (timePeriod.getDate("thruDate").before(UtilDateTime.toDate(UtilDateTime.toDateTimeString(UtilDateTime.nowTimestamp())))) {
            Debug.logInfo(timePeriod.getDate("thruDate") + " is before " + UtilDateTime.nowTimestamp() + " so time period [" + customTimePeriodId + "] is not open", module );
            return false;
        }

        return true;
    }

    /************************************************************************/
    /**                       Miscellaneous Methods                        **/
    /************************************************************************/

    /**
     * Returns the Role of the Party, either contact, lead, account or partner.
     * Returns Error if Party not found.
     */
    public static String getPartyRole(HttpServletRequest request, HttpServletResponse response) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");

        String partyId = (String) request.getParameter("partyId");
        if (UtilValidate.isEmpty(partyId)) {
            return "error";
        } else {
            String role = null;
            try {
                role = PartyHelper.getFirstValidInternalPartyRoleTypeId(partyId, delegator);
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return "error";
            }
            if (UtilValidate.isEmpty(role)) {
                return "";
            } else {
                return role.toLowerCase();
            }
        }
    }

    /**
     * Helper method to format a percent string for use in the form widget.
     * For now, this uses only the forecast percents, but it could be generalized.
     */
    public static String toPercent(Number number) {
        return UtilNumber.toPercentString(number,
                com.opensourcestrategies.crmsfa.forecasts.UtilForecast.BD_FORECAST_PERCENT_DECIMALS - 2,
                com.opensourcestrategies.crmsfa.forecasts.UtilForecast.BD_FORECAST_PERCENT_ROUNDING);
    }


}
