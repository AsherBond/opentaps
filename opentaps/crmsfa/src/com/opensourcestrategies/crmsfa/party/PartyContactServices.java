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
package com.opensourcestrategies.crmsfa.party;

import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.security.Security;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;

/**
 * PartyContact services. The service documentation is in services_party.xml.
 *
 * @author     <a href="mailto:leon@opensourcestrategies.com">Leon Torres</a>
 * @version    $Rev: 488 $
 */
public class PartyContactServices {

    public static final String MODULE = PartyContactServices.class.getName();

    public static Map<String, Object> createBasicContactInfoForParty(DispatchContext dctx, Map<String, ?> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Security security = dctx.getSecurity();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);
        Map<String, Object> serviceResults = null; // for collecting service results
        Map<String, Object> results = ServiceUtil.returnSuccess();  // for returning the contact mech IDs when finished

        // security
        if (!security.hasEntityPermission("PARTYMGR", "_PCM_CREATE", userLogin)) {
            return UtilMessage.createAndLogServiceError("CrmErrorPermissionDenied", locale, MODULE);
        }

        // input
        String partyId = (String) context.get("partyId");
        String primaryEmail = (String) context.get("primaryEmail");
        String primaryWebUrl = (String) context.get("primaryWebUrl");
        String primaryPhoneCountryCode = (String) context.get("primaryPhoneCountryCode");
        String primaryPhoneAreaCode = (String) context.get("primaryPhoneAreaCode");
        String primaryPhoneNumber = (String) context.get("primaryPhoneNumber");
        String primaryPhoneExtension = (String) context.get("primaryPhoneExtension");
        String primaryPhoneAskForName = (String) context.get("primaryPhoneAskForName");
        String generalToName = (String) context.get("generalToName");
        String generalAttnName = (String) context.get("generalAttnName");
        String generalAddress1 = (String) context.get("generalAddress1");
        String generalAddress2 = (String) context.get("generalAddress2");
        String generalCity = (String) context.get("generalCity");
        String generalStateProvinceGeoId = (String) context.get("generalStateProvinceGeoId");
        String generalPostalCode = (String) context.get("generalPostalCode");
        String generalPostalCodeExt = (String) context.get("generalPostalCodeExt");
        String generalCountryGeoId = (String) context.get("generalCountryGeoId");

        try {
            // create primary email
            if ((primaryEmail != null) && !primaryEmail.equals("")) {
                serviceResults = dispatcher.runSync("createPartyEmailAddress", UtilMisc.toMap("partyId", partyId, "userLogin", userLogin,
                        "contactMechTypeId", "EMAIL_ADDRESS", "contactMechPurposeTypeId", "PRIMARY_EMAIL", "emailAddress", primaryEmail));
                if (ServiceUtil.isError(serviceResults)) {
                    return serviceResults;
                }
                results.put("primaryEmailContactMechId", serviceResults.get("contactMechId"));
            }

            // create primary web url
            if ((primaryWebUrl != null) && !primaryWebUrl.equals("")) {
                serviceResults = dispatcher.runSync("createPartyContactMech", UtilMisc.toMap("partyId", partyId, "userLogin", userLogin,
                        "contactMechTypeId", "WEB_ADDRESS", "contactMechPurposeTypeId", "PRIMARY_WEB_URL", "infoString", primaryWebUrl));
                if (ServiceUtil.isError(serviceResults)) {
                    return serviceResults;
                }
                results.put("primaryWebUrlContactMechId", serviceResults.get("contactMechId"));
            }

            // create primary telecom number
            if (((primaryPhoneNumber != null) && !primaryPhoneNumber.equals(""))) {
                Map<String, Object> input = UtilMisc.<String, Object>toMap("partyId", partyId, "userLogin", userLogin, "contactMechPurposeTypeId", "PRIMARY_PHONE");
                input.put("countryCode", primaryPhoneCountryCode);
                input.put("areaCode", primaryPhoneAreaCode);
                input.put("contactNumber", primaryPhoneNumber);
                input.put("extension", primaryPhoneExtension);
                input.put("askForName", primaryPhoneAskForName);
                serviceResults = dispatcher.runSync("createPartyTelecomNumber", input);
                if (ServiceUtil.isError(serviceResults)) {
                    return serviceResults;
                }
                results.put("primaryPhoneContactMechId", serviceResults.get("contactMechId"));
            }

            // create general correspondence postal address
            if ((generalAddress1 != null) && !generalAddress1.equals("")) {
                Map<String, Object> input = UtilMisc.<String, Object>toMap("partyId", partyId, "userLogin", userLogin, "contactMechPurposeTypeId", "GENERAL_LOCATION");
                input.put("toName", generalToName);
                input.put("attnName", generalAttnName);
                input.put("address1", generalAddress1);
                input.put("address2", generalAddress2);
                input.put("city", generalCity);
                input.put("stateProvinceGeoId", generalStateProvinceGeoId);
                input.put("postalCode", generalPostalCode);
                input.put("postalCodeExt", generalPostalCodeExt);
                input.put("countryGeoId", generalCountryGeoId);
                serviceResults = dispatcher.runSync("createPartyPostalAddress", input);
                if (ServiceUtil.isError(serviceResults)) {
                    return serviceResults;
                }
                String contactMechId = (String) serviceResults.get("contactMechId");
                results.put("generalAddressContactMechId", contactMechId);

                // also make this address the SHIPPING_LOCATION
                input = UtilMisc.<String, Object>toMap("partyId", partyId, "userLogin", userLogin, "contactMechId", contactMechId, "contactMechPurposeTypeId", "SHIPPING_LOCATION");
                serviceResults = dispatcher.runSync("createPartyContactMechPurpose", input);
                if (ServiceUtil.isError(serviceResults)) {
                    return serviceResults;
                }
            }

        } catch (GenericServiceException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorCreateBasicContactInfoFail", locale, MODULE);
        }
        return results;
    }

}
