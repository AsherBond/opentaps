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
package com.opensourcestrategies.financials.payment.gateway;

import javolution.util.FastMap;
import org.ofbiz.accounting.thirdparty.authorizedotnet.AuthorizeResponse;
import org.ofbiz.base.util.*;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.ServiceUtil;

import java.util.Map;

/**
 * Gateway services for eCheck.net using Advanced Integration Method (AIM) API.
 * These services provide a way to capture and refund checks electronically.
 *
 * If the payment.authorizedotnet.test property is set to TRUE (or true), then
 * the services will log the request and response data.  This could contain
 * sensitive customer data, so it serves as a debugging mode separate from the
 * log levels in OFBiz.
 *
 * Note that the certification URL should be used if test mode is on, although
 * test mode could be used with a production URL to identify problems on production.
 *
 * @author <a href="mailto:leon@opensourcestrategies.com">Leon Torres</a>
 */
public class AimEcheckServices {

    public static final String module = AimEcheckServices.class.getName();

    /**
     * If ProductStorePaymentSetting.paymentPropertiesPath is not set, then
     * the default config file is payment.properties.  Note that the properties
     * used by this class are identical to those used by AIMPaymentServices.java,
     * so you may re-use the same credentials or split them as you wish.
     */
    public static final String DEFAULT_RESOURCE = "payment.properties";

    /**
     * Authorizes and captures an EFT transaction.  If the authorization or capture
     * fails due to problems with the transaction, this service will result in a
     * failure.
     *
     * If this service results in an error, it means that the service is not propery
     * configured and will not work until the issues are resolved.
     */
    public static Map authorizeAndCaptureEft(DispatchContext dctx, Map context) {
        Delegator delegator = dctx.getDelegator();
        String resource = getResource(context);
        Double amount = (Double) context.get("processAmount");
        GenericValue eftAccount = (GenericValue) context.get("eftAccount");
        String currencyUomId = (String) context.get("currency");

        try {
            Map request = buildRequestHeader(resource);
            request.putAll( buildRequest(eftAccount, amount, currencyUomId, "AUTH_CAPTURE") );
            request.putAll( buildCustomerRequest(delegator, context) );
            request.putAll( buildOrderRequest(context) );

            AuthorizeResponse response = processRequest(request, resource);

            // process the response
            Map results = ServiceUtil.returnSuccess();
            if (response == null) {
                results.put("authResult", Boolean.FALSE);
                results.put("authRefNum", AuthorizeResponse.ERROR);
                results.put("processAmount", new Double(0.0));
            } else if (AuthorizeResponse.APPROVED.equals(response.getResponseCode())) {
                results.put("authResult", Boolean.TRUE);
                results.put("authFlag", response.getReasonCode());
                results.put("authMessage", response.getReasonText());
                results.put("authCode", response.getResponseField(AuthorizeResponse.AUTHORIZATION_CODE));
                results.put("authRefNum", response.getResponseField(AuthorizeResponse.TRANSACTION_ID));
                results.put("processAmount", new Double(response.getResponseField(AuthorizeResponse.AMOUNT)));
            } else {
                results.put("authResult", Boolean.FALSE);
                results.put("authFlag", response.getReasonCode());
                results.put("authMessage", response.getReasonText());
                results.put("authCode", response.getResponseField(AuthorizeResponse.AUTHORIZATION_CODE));
                results.put("authRefNum", AuthorizeResponse.ERROR);
                results.put("processAmount", new Double(0.0));
            }

            if (isTestMode(resource)) {
                Debug.logInfo("eCheck.NET AUTH_CAPTURE results: " + results, module);
            }
            return results;
        } catch (GenericEntityException e) {
            String message = "Entity engine error when attempting to authorize and capture EFT via eCheck.net: " + e.getMessage();
            Debug.logError(e, message, module);
            return ServiceUtil.returnError(message);
        } catch (GenericServiceException e) {
            String message = "Service error when attempting to authorize and capture EFT via eCheck.net.  This is a configuration problem that must be fixed before this service can work properly: " + e.getMessage();
            Debug.logError(e, message, module);
            return ServiceUtil.returnError(message);
        }
    }

    public static Map refundEft(DispatchContext dctx, Map context) {
        String resource = getResource(context);
        Double amount = (Double) context.get("refundAmount");
        GenericValue eftAccount = (GenericValue) context.get("eftAccount");
        String currencyUomId = (String) context.get("currency");

        try {
            Map request = buildRequestHeader(resource);
            request.putAll( buildRequest(eftAccount, amount, currencyUomId, "REFUND") );
            AuthorizeResponse response = processRequest(request, resource);

            // process the response
            Map results = ServiceUtil.returnSuccess();
            if (response == null) {
                results.put("refundResult", Boolean.FALSE);
                results.put("refundRefNum", AuthorizeResponse.ERROR);
                results.put("refundAmount", new Double(0.0));
            } else if (AuthorizeResponse.APPROVED.equals(response.getResponseCode())) {
                results.put("refundResult", Boolean.TRUE);
                results.put("refundFlag", response.getReasonCode());
                results.put("refundMessage", response.getReasonText());
                results.put("refundCode", response.getResponseField(AuthorizeResponse.AUTHORIZATION_CODE));
                results.put("refundRefNum", response.getResponseField(AuthorizeResponse.TRANSACTION_ID));
                results.put("refundAmount", new Double(response.getResponseField(AuthorizeResponse.AMOUNT)));
            } else {
                results.put("refundResult", Boolean.FALSE);
                results.put("refundFlag", response.getReasonCode());
                results.put("refundMessage", response.getReasonText());
                results.put("refundCode", response.getResponseField(AuthorizeResponse.AUTHORIZATION_CODE));
                results.put("refundRefNum", AuthorizeResponse.ERROR);
                results.put("refundAmount", new Double(0.0));
            }

            if (isTestMode(resource)) {
                Debug.logInfo("eCheck.NET REFUND results: " + results, module);
            }
            return results;
        } catch (GenericServiceException e) {
            String message = "Service error when attempting to refund an EFT via eCheck.net.  This is a configuration problem that must be fixed before this service can work properly: " + e.getMessage();
            Debug.logError(e, message, module);
            return ServiceUtil.returnError(message);
        }
    }

    /**
     * Processes the request and returns an AuthorizeResponse.  This service causes a GenericServiceException if
     * there is a fatal confguration error that must be addressed.
     */
    private static AuthorizeResponse processRequest(Map request, String resource) throws GenericServiceException {
        boolean testMode = isTestMode(resource);
        String url = UtilProperties.getPropertyValue(resource, "payment.authorizedotnet.url");
        if (UtilValidate.isEmpty(url)) {
            throw new GenericServiceException("Authorize.NET transaction URL not configured.  Please ensure payment.authorizedotnet.test is defined in " + resource);
        }

        Debug.logInfo("Sending eCheck.NET request type "+request.get("x_type"), module);
        if (testMode) {
            Debug.logInfo("Request URL: " + url, module);
            Debug.logInfo("Request Map: " + request, module);
        }

        // post the request to the url
        String responseString = null;
        try {
            HttpClient client = new HttpClient(url, request);
            client.setClientCertificateAlias("AUTHORIZE_NET");
            responseString = client.post();
        } catch (HttpClientException e) {
            Debug.logError(e, "Failed to send eCheck.NET request due to client exception: " + e.getMessage(), module);
            return null;
        }

        if (testMode) {
            Debug.logInfo("Response from eCheck.NET: " + responseString, module);
        }

        return new AuthorizeResponse(responseString);
    }


    /*************************************************************************/
    /***                                                                   ***/
    /***                    Request Map Buildling                          ***/
    /***                                                                   ***/
    /*************************************************************************/


    /**
     * Gets the request fields that are configured in the properties file, such
     * as the merchant key and password.  This handles version as well.  If
     * some critical data is missing, this throws GenericServiceException. 
     */
    private static Map buildRequestHeader(String resource) throws GenericServiceException {
        Map request = FastMap.newInstance();

        String login = UtilProperties.getPropertyValue(resource, "payment.authorizedotnet.login");
        if (UtilValidate.isEmpty(login)) {
            Debug.logWarning("Authorize.NET login not configured.  Please ensure payment.authorizedotnet.login is defined in " + resource, module);
        }

        String password = UtilProperties.getPropertyValue(resource, "payment.authorizedotnet.password");
        if (UtilValidate.isEmpty(password)) {
            Debug.logWarning("Authorize.NET password not configured.  Please ensure payment.authorizedotnet.password is defined in " + resource, module);
        }

        String delimited = UtilProperties.getPropertyValue(resource, "payment.authorizedotnet.delimited");
        String delimiter = UtilProperties.getPropertyValue(resource, "payment.authorizedotnet.delimiter");
        String emailcustomer = UtilProperties.getPropertyValue(resource, "payment.authorizedotnet.emailcustomer");
        String emailmerchant = UtilProperties.getPropertyValue(resource, "payment.authorizedotnet.emailmerchant");
        String transdescription = UtilProperties.getPropertyValue(resource, "payment.authorizedotnet.transdescription");

        request.put("x_login", login);
        request.put("x_password", password);
        request.put("x_delim_data", delimited);
        request.put("x_delim_char", delimiter);
        request.put("x_email_customer", emailcustomer);
        request.put("x_email_merchant", emailmerchant);
        request.put("x_description", transdescription);
        request.put("x_relay_response", "FALSE");

        String version = UtilProperties.getPropertyValue(resource, "payment.authorizedotnet.version", "3.0");
        String tkey = UtilProperties.getPropertyValue(resource, "payment.authorizedotnet.trankey");

        // transaction key is only supported in 3.1
        if ("3.1".equals(version) && UtilValidate.isNotEmpty(tkey)) {
            Debug.logWarning("Version 3.1 of Authorize.NET requires a transaction key.  Please define payment.authorizedotnet.trankey in " + resource, module);
            Debug.logWarning("Reverting to version 3.0 of Authorize.NET", module);
            version = "3.0";
        }

        request.put("x_version", version);
        request.put("x_tran_key", tkey);

        return request;
    }

    /**
     * Populates request fields for a given eft account, amount, and transaction type.  The
     * transaction type will be one of "AUTH_CAPTURE" or "REFUND".  If there is some critical
     * data missing, this throws GenericServiceException.
     */
    private static Map buildRequest(GenericValue eftAccount, Double amount, String currencyUomId, String transactionType) {
        Map request = FastMap.newInstance();

        request.put("x_method", "ECHECK");
        request.put("x_amount", amount.toString());
        request.put("x_currency_code", currencyUomId);
        request.put("x_type", transactionType);
        request.put("x_bank_aba_code", translateRoutingNumber(eftAccount));
        request.put("x_bank_acct_num", translateAccountNumber(eftAccount));
        request.put("x_bank_acct_type", translateAccountType(eftAccount));
        request.put("x_bank_name", eftAccount.get("bankName"));

        // use the company name of the account, otherwise the customer name
        String nameOnAccount = eftAccount.getString("companyNameOnAccount");
        if (UtilValidate.isEmpty(nameOnAccount)) {
            nameOnAccount = eftAccount.getString("nameOnAccount");
        }
        request.put("x_bank_acct_name", nameOnAccount);

        return request;
    }

    /**
     * Creates request fields for a customer, such as email and postal addresses.
     */
    private static Map buildCustomerRequest(Delegator delegator, Map context) throws GenericEntityException {
        Map request = FastMap.newInstance();
        GenericValue customer = (GenericValue) context.get("billToParty");
        GenericValue address = (GenericValue) context.get("billingAddress");
        GenericValue email = (GenericValue) context.get("billToEmail");

        if (customer != null && "Person".equals(customer.getEntityName())) {
            request.put("x_first_name", customer.get("firstName"));
            request.put("x_last_name", customer.get("lastName"));
        }
        if (customer != null && "PartyGroup".equals(customer.getEntityName())) {
            request.put("x_company", customer.get("groupName"));
        }
        if (address != null) {
            request.put("x_address", address.get("address1"));
            request.put("x_city", address.get("city"));
            request.put("x_zip", address.get("postalCode"));
            GenericValue country = address.getRelatedOneCache("CountryGeo");
            if (country != null) {
                request.put("x_country", country.get("geoCode"));
                if ("USA".equals(country.get("geoId"))) {
                    request.put("x_state", address.get("stateProvinceGeoId"));
                }
            }
        }
        if (email != null && UtilValidate.isNotEmpty(email.getString("infoString"))) {
            request.put("x_email", email.get("infoString"));
        }

        return request;
    }

    /**
     * Creates request fields for an order, such as the items and quantities.
     */
    private static Map buildOrderRequest(Map context) {
        Map request = FastMap.newInstance();
        GenericValue pref = (GenericValue) context.get("orderPaymentPreference");
        if (pref != null) request.put("x_invoice_num", "Order " + pref.get("orderId"));
        return request;
    }


    /*************************************************************************/
    /***                                                                   ***/
    /***                       Helper Functions                            ***/
    /***                                                                   ***/
    /*************************************************************************/

    
    /** Determines what properties file to use for the request. */
    private static String getResource(Map context) {
        String resource = (String) context.get("paymentConfig");
        return (resource == null ? DEFAULT_RESOURCE : resource);
    }

    // assumes production mode if the payment.authorizedotnet.test property is missing
    private static boolean isTestMode(String resource) {
        String boolValue = UtilProperties.getPropertyValue(resource, "payment.authorizedotnet.test", "false");
        boolValue = boolValue.toLowerCase();
        if (boolValue.startsWith("y") || boolValue.startsWith("t")) return true;
        if (boolValue.startsWith("n") || boolValue.startsWith("f")) return false;
        return false;
    }

    private static String translateRoutingNumber(GenericValue eftAccount) {
        String routingNumber = eftAccount.getString("routingNumber").replaceAll("\\D", "");
        if (routingNumber.length() > 9) {
            Debug.logWarning("EftAccount with paymentMethodId ["+eftAccount.get("paymentMethodId")+"] has a routingNumber larger than 9 digits.  Truncating to 9.", module);
            return routingNumber.substring(0, 9);
        }
        return routingNumber;
    }

    private static String translateAccountNumber(GenericValue eftAccount) {
        String accountNumber = eftAccount.getString("accountNumber").replaceAll("\\D", "");
        if (accountNumber.length() > 20) {
            Debug.logWarning("EftAccount with paymentMethodId ["+eftAccount.get("paymentMethodId")+"] has a accountNumber larger than 20 digits.  Truncating to 20.", module);
            return accountNumber.substring(0, 20);
        }
        return accountNumber;
    }

    private static String translateAccountType(GenericValue eftAccount) {
        String type = eftAccount.getString("accountType");
        if (UtilValidate.isEmpty(type)) {
            Debug.logWarning("EftAccount with paymentMethodId ["+eftAccount.get("paymentMethodId")+"] does not have an account type defined.  Assuming CHECKING.", module);
            return "CHECKING";
        }
        type = type.toUpperCase();

        if (type.contains("BUSINESS")) return "BUSINESSCHECKING";
        if (type.contains("SAVING")) return "SAVINGS";
        return "CHECKING";
    }

}
