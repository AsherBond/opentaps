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

package com.opensourcestrategies.crmsfa.voip;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilCommon;

/**
 * Services for working with VoIP systems
 *
 * @author     <a href="mailto:cliberty@opensourcestrategies.com">Chris Liberty</a>
 * @version    $Rev: 600 $
 */
public class VoIPServices {

    public static final String module = VoIPServices.class.getName();
    public static final String errorResource = "CRMSFAErrorLabels";
    public static final String resource = "VoIP";

    /**
     * Retrieves the latest call for the user from the FacetPhone server. Returns an error only if VoIP.properties is misconfigured -
     *  otherwise returns failure if something goes wrong. Assumes FacetPhone userId is equal to OFBiz userLoginId.
     * @param dctx DispatchContext
     * @param context Map
     * @return Map, possibly containing a string representing the latest call for the user
     */
    public static Map retrieveLatestCallFromFacetPhoneServer( DispatchContext dctx, Map context) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = UtilCommon.getLocale(context);

        // FacetPhone server requires and sends an EOT after each query and response
        byte endOfTransmission = 0x04;

        // Make sure FacetPhone integration is turned on
        boolean facetPhoneIntegrate = "true".equalsIgnoreCase( UtilProperties.getPropertyValue("VoIP", "facetPhone.integrate", ""));
        if (! facetPhoneIntegrate) return ServiceUtil.returnSuccess();

        // Check the configuration settings
        String facetPhoneServerIP = UtilProperties.getPropertyValue("VoIP", "facetPhone.server.connect.ipAddress");
        if ( UtilValidate.isEmpty(facetPhoneServerIP)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(errorResource, "CrmErrorPropertyNotConfigured", UtilMisc.toMap("propertyName", "facetPhone.server.connect.ipAddress", "fileName", resource + ".properties"), locale));
        }

        String facetPhoneServerPortStr = UtilProperties.getPropertyValue("VoIP", "facetPhone.server.connect.port", "6500");
        int facetPhoneServerPort = Integer.parseInt(facetPhoneServerPortStr);

        String facetPhoneServerTimeoutStr = UtilProperties.getPropertyValue("VoIP", "facetPhone.server.connect.timeout", "10000");
        int facetPhoneServerTimeout = Integer.parseInt(facetPhoneServerTimeoutStr);

        String queryString = UtilProperties.getMessage("VoIP", "facetPhone.cid.queryString", userLogin, locale);
        if (UtilValidate.isEmpty(queryString)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(errorResource, "CrmErrorPropertyNotConfigured", UtilMisc.toMap("propertyName", "facetPhone.cid.queryString", "fileName", resource + ".properties"), locale));
        }

        Socket socket = null;
        BufferedOutputStream out = null;
        InputStreamReader in = null;
        StringBuffer latestCallDataBuf = new StringBuffer();
        try {

            // Connect to the FacetPhone server
            socket = new Socket() ;
            socket.connect(new InetSocketAddress(facetPhoneServerIP, facetPhoneServerPort), facetPhoneServerTimeout);
            out = new BufferedOutputStream(socket.getOutputStream());
            in = new InputStreamReader(socket.getInputStream());

            // Send the query to retrieve the latest call for the userLoginId
            out.write(queryString.getBytes());
            out.write(endOfTransmission);
            out.flush();

            // The FacetPhone server doesn't make any response at all (not even an EOT) if the user
            //  isn't recognized or has never had a call, so only wait a quarter of a second for a response
            long startTime = System.currentTimeMillis();
            int responseTimeout = 250;
            while (System.currentTimeMillis() < startTime + responseTimeout) {
                if (in.ready()) {
                    int responseByte = in.read();
                    if (responseByte != -1 && responseByte != endOfTransmission) {
                        latestCallDataBuf.append((char) responseByte);
                    }
                    if (responseByte == endOfTransmission) {
                        break;
                    }
                }
            }
        } catch ( IOException e) {
            String errorMessage = UtilProperties.getMessage(errorResource, "CrmErrorVoIPUnableToConnectToFacetPhone" + ": " + e.getMessage(), locale);
            Debug.logError(e, module);
            return ServiceUtil.returnFailure(errorMessage);
        } finally {
            try {
                out.close();
                in.close();
                socket.close() ;
            } catch (IOException e) {
                Debug.logError(e, module);
            }
        }

        String latestCallData = latestCallDataBuf.toString();
        Map result = null;
        if (UtilValidate.isNotEmpty(latestCallData)) {
            result = ServiceUtil.returnSuccess();
            result.put("latestCallData", latestCallData);
        } else {
            String errorMessage = UtilProperties.getMessage(errorResource, "CrmErrorVoIPErrorResponseFromFacetPhone", locale);
            result = ServiceUtil.returnFailure(errorMessage);
            Debug.logError(errorMessage, module);
        }
        return result;
    }

    /**
     * Retrieves and parses the incoming number of the latest call for the user from the FacetPhone server, using the retrieveLatestCallFromFacetPhoneServer service.
     *
     * @param dctx
     * @param context
     * @return
     */
    public static Map getCurrentIncomingNumberFromFacetPhoneServer( DispatchContext dctx, Map context) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        LocalDispatcher dispatcher = (LocalDispatcher) dctx.getDispatcher();
        Locale locale = UtilCommon.getLocale(context);

        Map result = ServiceUtil.returnSuccess();

        String callStateRegexp = UtilProperties.getPropertyValue("VoIP", "facetPhone.cid.callState.regexp");
        if (UtilValidate.isEmpty(callStateRegexp)) {
            String message = UtilProperties.getMessage(errorResource, "CrmErrorPropertyNotConfigured", UtilMisc.toMap("propertyName", "facetPhone.cid.callState.regexp", "fileName", resource + ".properties"), locale);
            Debug.logError(message, module);
            return ServiceUtil.returnError(message);
        }

        String numberIdentifyRegexp = UtilProperties.getPropertyValue("VoIP", "facetPhone.cid.number.identify.regexp");
        if (UtilValidate.isEmpty(numberIdentifyRegexp)) {
            String message = UtilProperties.getMessage(errorResource, "CrmErrorPropertyNotConfigured", UtilMisc.toMap("propertyName", "facetPhone.cid.number.identify.regexp", "fileName", resource + ".properties"), locale);
            Debug.logError(message, module);
            return ServiceUtil.returnError(message);
        }
        String numberParseRegexp = UtilProperties.getPropertyValue("VoIP", "voip.number.parse.regexp");
        if (UtilValidate.isEmpty(numberParseRegexp)) {
            String message = UtilProperties.getMessage(errorResource, "CrmErrorPropertyNotConfigured", UtilMisc.toMap("propertyName", "voip.number.parse.regexp", "fileName", resource + ".properties"), locale);
            Debug.logError(message, module);
            return ServiceUtil.returnError(message);
        }

        // Call the retrieveLatestCallFromFacetPhoneServer service
        String latestCallData = null;
        Map retrieveLatestCallFromFacetPhoneServerMap = null;
        try {
            retrieveLatestCallFromFacetPhoneServerMap = dispatcher.runSync("retrieveLatestCallFromFacetPhoneServer", UtilMisc.toMap("userLogin", userLogin, "locale", locale));
        } catch( GenericServiceException e ) {
            Debug.logError(ServiceUtil.getErrorMessage(retrieveLatestCallFromFacetPhoneServerMap), module);
            return ServiceUtil.returnFailure(ServiceUtil.getErrorMessage(retrieveLatestCallFromFacetPhoneServerMap));
        }
        if (ServiceUtil.isError(retrieveLatestCallFromFacetPhoneServerMap) || ServiceUtil.isFailure(retrieveLatestCallFromFacetPhoneServerMap)) {
            Debug.logError(ServiceUtil.getErrorMessage(retrieveLatestCallFromFacetPhoneServerMap), module);
            return ServiceUtil.returnFailure(ServiceUtil.getErrorMessage(retrieveLatestCallFromFacetPhoneServerMap));
        }

        latestCallData = (String) retrieveLatestCallFromFacetPhoneServerMap.get("latestCallData");
        if (UtilValidate.isEmpty(latestCallData)) {
            String errorMessage = UtilProperties.getMessage(errorResource, "CrmErrorVoIPErrorLatestCallFromFacetPhone", locale);
            Debug.logError(errorMessage, module);
            return ServiceUtil.returnFailure(errorMessage);
        }

        // Check the state of the call by retrieving it from the latestCallData via regular expression - probably in the form <state=(completed|active)>
        String state = "";
        Matcher matcher = Pattern.compile(callStateRegexp).matcher(latestCallData);
        while (matcher.find()) {
            if (matcher.group(1) != null) state = matcher.group(1);
        }

        // Ignore the results if there's no active call
        if (! "active".equalsIgnoreCase(state)) {
            String message = UtilProperties.getMessage(errorResource, "CrmErrorVoIPErrorNoCurrentCall", userLogin, locale);
            Debug.logVerbose(message, module);
            return ServiceUtil.returnSuccess();
        }

        // Get the caller's number by retrieving it from the latestCallData via regular expression
        matcher = Pattern.compile(numberIdentifyRegexp).matcher(latestCallData);
        String number = null;
        while (matcher.find()) {
            if (matcher.group(1) != null) number = matcher.group(1);
        }

        // Ignore the results if there's no number for the call
        if (UtilValidate.isEmpty(number)) {
            String message = UtilProperties.getMessage(errorResource, "CrmErrorVoIPErrorNoNumberForCurrentCall", userLogin, locale);
            Debug.logVerbose(message, module);
            return ServiceUtil.returnSuccess();
        }

        matcher = Pattern.compile(numberParseRegexp).matcher(number);
        int phoneNumberPatternCountryCodeGroup = Integer.parseInt(UtilProperties.getPropertyValue(resource, "voip.number.parse.regexp.group.countryCode"));
        int phoneNumberPatternAreaCodeGroup = Integer.parseInt(UtilProperties.getPropertyValue(resource, "voip.number.parse.regexp.group.areaCode"));
        int phoneNumberPatternPhoneNumberGroup = Integer.parseInt(UtilProperties.getPropertyValue(resource, "voip.number.parse.regexp.group.phoneNumber"));
        if (matcher.matches()) {
            if (UtilValidate.isNotEmpty(matcher.group(phoneNumberPatternCountryCodeGroup))) result.put("countryCode", matcher.group(phoneNumberPatternCountryCodeGroup));
            if (UtilValidate.isNotEmpty(matcher.group(phoneNumberPatternAreaCodeGroup))) result.put("areaCode", matcher.group(phoneNumberPatternAreaCodeGroup));
            if (UtilValidate.isNotEmpty(matcher.group(phoneNumberPatternPhoneNumberGroup))) result.put("contactNumber", matcher.group(phoneNumberPatternPhoneNumberGroup));
        } else {
            String message = UtilProperties.getMessage(errorResource, "CrmErrorVoIPErrorNumberFromFacetPhone", UtilMisc.toMap("latestCallData", latestCallData), locale);
            Debug.logWarning(message, module);
            result.put("contactNumber", number);
        }

        return result;

    }
}
