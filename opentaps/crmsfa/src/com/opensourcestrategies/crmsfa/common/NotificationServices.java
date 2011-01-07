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
package com.opensourcestrategies.crmsfa.common;

import java.util.*;

import com.opensourcestrategies.crmsfa.party.PartyHelper;
import javolution.util.FastMap;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;

/**
 * Services for providing notification to parties of CRM events.
 *
 * @author     <a href="mailto:cliberty@opensourcestrategies.com">Chris Liberty</a>
 * @version    $Rev$
 */
public final class NotificationServices {

    private NotificationServices() { }

    private static final String MODULE = NotificationServices.class.getName();
    public static final String resource = "CRMSFAUiLabels";
    public static final String notificationResource = "notification";

	@SuppressWarnings("unchecked")
	public static Map<String, Object> sendCrmNotificationEmails(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        String eventType = (String) context.get("eventType");
        String subject = (String) context.get("subject");
        Map<String, Object> bodyParameters = (Map<String, Object>) context.get("bodyParameters");
        List<String> notifyPartyIds = (List<String>) context.get("notifyPartyIds");
        Set<String> uniquePartyIds = new HashSet<String>(notifyPartyIds);
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        try {

            // Try the specific case first - EG: screen.location.task.add=...
            String bodyScreenUri = UtilProperties.getMessage(notificationResource, "screen.location." + eventType, locale);
            if (UtilValidate.isEmpty(bodyScreenUri)) {

                // Failing that, try the more general case - EG: screen.location.task=...
                String[] propertyElements = eventType.split(".");
                if (propertyElements.length > 0) {
                    bodyScreenUri = UtilProperties.getMessage(notificationResource, "screen.location." + propertyElements[0], locale);
                }
            }

            String fromProperty = "from." + eventType;
            String sendFrom = UtilProperties.getMessage(notificationResource, fromProperty, locale);
            if (UtilValidate.isEmpty(sendFrom) || fromProperty.equals(sendFrom)) {
                sendFrom = UtilProperties.getMessage(notificationResource, "from", locale);
            }

            for (String notifyPartyId : uniquePartyIds) {

                try {

                    // Get the party's primary email address
                    String sendTo = PartyHelper.getPrimaryEmailForParty(notifyPartyId, delegator);

                    if (sendTo == null) {
                        Debug.logError(UtilProperties.getMessage(resource, "crmsfa.sendCrmNotificationEmailsErrorNoAddress", UtilMisc.toMap("partyId", notifyPartyId, "subject", subject), locale), MODULE);
                        continue;
                    }

                    Map<String, Object> sendMailContext = new HashMap<String, Object>();
                    sendMailContext.put("bodyScreenUri", bodyScreenUri);
                    sendMailContext.put("bodyParameters", bodyParameters);
                    sendMailContext.put("sendTo", sendTo);
                    sendMailContext.put("sendFrom", sendFrom);
                    sendMailContext.put("subject", subject);
                    sendMailContext.put("contentType", "text/html");
                    sendMailContext.put("userLogin", userLogin);

                    // Call sendMailFromScreen async so that failed emails are retried
                    dispatcher.runAsync("sendMailFromScreen", sendMailContext);

                } catch (GenericServiceException e) {
                    TransactionUtil.rollback();
                    Debug.logError(e, UtilProperties.getMessage(resource, "crmsfa.sendCrmNotificationEmailsError", UtilMisc.toMap("partyId", notifyPartyId, "subject", subject), locale), MODULE);
                }

            }

        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, "CrmErrorSendCrmNotificationEmailsFail", locale, MODULE);
        }
        return ServiceUtil.returnSuccess();
    }

    public static Map<String, Object> sendCatalogRequestNotificationEmail(DispatchContext dctx, Map<String, Object> context) {

        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        Locale locale = UtilCommon.getLocale(context);
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String custRequestId = (String) context.get("custRequestId");

        boolean sendEmail = "true".equalsIgnoreCase(UtilProperties.getMessage(notificationResource, "email.marketing.catalog.sendCatalogRequestEmails", locale).trim());
        if (!sendEmail) {
            Debug.logInfo(UtilProperties.getMessage(resource, "crmsfa.sendCrmNotificationEmailsCatRqTurnedOff", UtilMisc.toMap("custRequestId", custRequestId), locale), MODULE);
            return ServiceUtil.returnSuccess();
        }

        String mailToPartyId = (String) context.get("fromPartyId");

        try {
            Map<String, Object> paramsMap = FastMap.newInstance();

            paramsMap.put("eventType", "marketing.catalog");
            paramsMap.put("subject", UtilProperties.getMessage(notificationResource, "subject.marketing.catalog", locale));
            paramsMap.put("locale", locale);
            paramsMap.put("userLogin", userLogin);
            paramsMap.put("notifyPartyIds", UtilMisc.toList(mailToPartyId));

            Map<String, Object> bodyParameters = FastMap.newInstance();

            GenericValue custRequest = delegator.findByPrimaryKey("CustRequest", UtilMisc.toMap("custRequestId", custRequestId));
            GenericValue contactMech = custRequest.getRelatedOne("FulfillContactMech");
            GenericValue postalAddress = contactMech.getRelatedOne("PostalAddress");
            String address1 = (String) postalAddress.get("address1");
            String address2 = (String) postalAddress.get("address2");
            String city = (String) postalAddress.get("city");
            String postalCode = (String) postalAddress.get("postalCode");
            String stateProvinceGeoId = (String) postalAddress.get("stateProvinceGeoId");
            GenericValue country = postalAddress.getRelatedOne("CountryGeo");
            String countryName = (String) country.get("geoName");
            GenericValue party = delegator.findByPrimaryKey("PartySummaryCRMView", UtilMisc.toMap("partyId", custRequest.get("fromPartyId")));

            bodyParameters.put("firstName", party.get("firstName"));
            bodyParameters.put("lastName", party.get("lastName"));
            bodyParameters.put("address1", address1);
            bodyParameters.put("address2", address2);
            bodyParameters.put("city", city);
            bodyParameters.put("postalCode", postalCode);
            bodyParameters.put("stateProvinceGeoId", stateProvinceGeoId);
            bodyParameters.put("countryName", countryName);

            paramsMap.put("bodyParameters", bodyParameters);

            dispatcher.runSync("crmsfa.sendCrmNotificationEmails", paramsMap);

        } catch (GenericServiceException gse) {
            return UtilMessage.createAndLogServiceError(gse, MODULE);
        } catch (GenericEntityException gee) {
            return UtilMessage.createAndLogServiceError(gee, MODULE);
        }

        return ServiceUtil.returnSuccess();
    }

}
