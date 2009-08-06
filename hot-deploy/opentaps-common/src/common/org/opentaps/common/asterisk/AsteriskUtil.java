/*
 * Copyright (c) 2008 - 2009 Open Source Strategies, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 *
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
 */

/**
 * Author: spark
 * Email: spark.sun@live.cn
 * Create Date: 11/19/2008
 * Usage: Asterisk Util Class
 */

package org.opentaps.common.asterisk;

import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

import org.asteriskjava.manager.AuthenticationFailedException;
import org.asteriskjava.manager.ManagerConnection;
import org.asteriskjava.manager.ManagerConnectionFactory;
import org.asteriskjava.manager.ManagerConnectionState;
import org.asteriskjava.manager.ManagerEventListener;
import org.asteriskjava.manager.TimeoutException;
import org.asteriskjava.manager.action.OriginateAction;
import org.asteriskjava.manager.action.StatusAction;
import org.asteriskjava.manager.event.DialEvent;
import org.asteriskjava.manager.event.ManagerEvent;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.opentaps.domain.base.entities.TelecomNumber;

/**
 * Integrates with an Asterisk server.
 */
public final class AsteriskUtil {

    private static final String MODULE = AsteriskUtil.class.getName();
    private static final int TIMEOUT = 30000; // 30 seconds timeout

    private ManagerConnection managerConnection;        // use Asterisk manager connection API
    private String asteriskExternalCode = "";           // prefix for dialing external (to asterisk) numbers
    // stores the calls, where the key is the call destination number, and value its origin number
    private HashMap<String, String> calls = new HashMap<String, String>();
    private static AsteriskUtil asteriskUtil;           // simple instance for every exectuation
    private String asteriskCountryCode = "";            // current country code
    private String asteriskAreaCode = "";               // current area code
    private String asteriskPhoneNumber = "";            // current phone number
    private String asteriskInternationalPrefix = "";    // the number prefix that you want to dial foreign number
    private String asteriskAreaPrefix = "";             // the number prefix that you want to dial other city number
    private String alwaysDialAreaCode = "";             // if always add area code to dial
    private String alwaysDialCountryCode = "";          // if always add country code to dial
    private AsteriskUtil() {
        reload();
    }

    /**
     * Gets the last registered number that called the given number.
     * Note: numbers can be phone numbers, asterisk extensions, etc ...
     * @param calledNumber the called number for which the last caller is returned
     * @return last registered number that called the given number
     */
    public String getLastCallTo(String calledNumber) {
        if (calls.containsKey(calledNumber)) {
            return calls.remove(calledNumber);
        } else {
            return "";
        }
    }

    /**
     * Reloads asterisk config properties.
     */
    public void reload() {
        // retrieve asterisk manager configuration here
        Properties configProperties = UtilProperties.getProperties("asterisk.properties");
        // asterisk host address
        String host = (String) configProperties.get("asterisk.host");
        // asterisk manager username
        String username = (String) configProperties.get("asterisk.username");
        // asterisk manager password
        String password = (String) configProperties.get("asterisk.password");
        // asterisk dial out prefix, calls between internal extensions are made by dialing the extension number
        // whereas external calls need a prefix before the number you want dial, such as 9
        asteriskExternalCode = (String) configProperties.get("asterisk.outbound.prev");
        // the country code of asterisk server, can be specified to avoid dialing national numbers using the country code
        asteriskCountryCode = UtilProperties.getPropertyValue("asterisk.properties", "asterisk.countryCode", "");
        // the area code of asterisk server, can be specified to avoid dialing local area numbers using the area code
        asteriskAreaCode = UtilProperties.getPropertyValue("asterisk.properties", "asterisk.areaCode", "");
        // the current phone number of the asterisk server (the phone number of the phone line connected to the asterisk server)
        asteriskPhoneNumber = UtilProperties.getPropertyValue("asterisk.properties", "asterisk.phoneNumber", "");
        // the prefix used to dial international phone numbers, usually "011" or "00" ... depends on the origin country
        asteriskInternationalPrefix = UtilProperties.getPropertyValue("asterisk.properties", "asterisk.outbound.foreign", "");
        // the prefix used to dial numbers out of the local area, for example to dial other cities / states
        asteriskAreaPrefix = UtilProperties.getPropertyValue("asterisk.properties", "asterisk.outbound.area", "");
        // if always add area code to dial, even though it was the same area code.
        alwaysDialAreaCode = UtilProperties.getPropertyValue("asterisk.properties", "asterisk.alwaysDialAreaCode", "Y");
        // if always add country code to dial, even though it was the same country code, it used in some voip provider.
        alwaysDialCountryCode = UtilProperties.getPropertyValue("asterisk.properties", "asterisk.alwaysDialCountryCode", "Y");
        // logs out an existing connection
        if (managerConnection != null) {
            if (managerConnection.getState().equals(ManagerConnectionState.CONNECTED)) {
                managerConnection.logoff();
            }
        }

        ManagerConnectionFactory factory = new ManagerConnectionFactory(host, username, password);

        this.managerConnection = factory.createManagerConnection();
        // connect to Asterisk and log in
        try {
            managerConnection.login();
            managerConnection.addEventListener(new ManagerEventListener() {
                    public void onManagerEvent(ManagerEvent event) {
                        // add a listener to handle incoming and outgoing calls
                        // e.getCallerId() is the calling phone number. such as 8605758672106
                        // e.getDestination() is the destination of call, include asterisk communication, such as SIP/825-09caf850
                        if (event instanceof DialEvent) {
                            DialEvent e = (DialEvent) event;
                            String destinationNumber = retrieveNumber(e.getDestination());
                            Debug.logInfo("Call from:" + e.getCallerId() + ", to:" + destinationNumber, MODULE);
                            calls.put(destinationNumber, e.getCallerId());
                        }
                    }
                });
            managerConnection.sendAction(new StatusAction());
        } catch (IllegalStateException e) {
            Debug.logError(e, "Error reloading asterisk server manager connection", MODULE);
        } catch (IOException e) {
            Debug.logError(e, "Error reloading asterisk server manager connection", MODULE);
        } catch (AuthenticationFailedException e) {
            Debug.logError(e, "Error reloading asterisk server manager connection", MODULE);
        } catch (TimeoutException e) {
            Debug.logError(e, "Error reloading asterisk server manager connection", MODULE);
        }
    }

    /**
     * Parses the asterisk channel string and return the number (internal extension number or phone number).
     * Examples of channel strings: <code>SIP/825-09caf850</code>, <code>Local/917908,2</code>.
     * @param channelString the String returned by Asterisk <code>DialEvent</code> Listener
     * @return the asterisk internal extension, eg: from <code>SIP/825-09caf850</code> returns <code>825</code>
     */
    private static String retrieveNumber(String channelString) {
        String no = channelString;
        // cut everything left of / and <
        int idx = no.indexOf("/");
        if (idx > 0) {
            no = no.substring(idx);
        }
        idx = no.indexOf("<");
        if (idx > 0) {
            no = no.substring(idx);
        }
        // cut everything right of - and >
        idx = no.indexOf("-");
        if (idx > 0) {
            no = no.substring(1, idx);
        }
        idx = no.indexOf(">");
        if (idx > 0) {
            no = no.substring(1, idx);
        }
        return no;
    }

    /**
     * Gets an <code>AsteriskUtil</code> instance.
     * @return an instance of <code>AsteriskUtil</code>
     */
    public static AsteriskUtil getInstance() {
        if (asteriskUtil == null) {
            asteriskUtil = new AsteriskUtil();
        }
        return asteriskUtil;
    }

    /**
     * click call function
     * example: call(809, 011862062881234) 809 is my extension number, 011862062881234 is the number that I want to dial.
     * this function will first call your extension(parameter callFrom), then will call the target phone number(parameter callTo) after you pickup the telephone.
     * @param callFrom the Asterisk extension from
     * @param callTo the phone number to call
     */
    public void call(String callFrom, String callTo)  {

        OriginateAction originateAction;
        originateAction = new OriginateAction();
        originateAction.setChannel("Local/" + callFrom + "@from-internal");
        originateAction.setContext("from-internal");
        originateAction.setExten(asteriskExternalCode + callTo);
        originateAction.setCallerId(callFrom);
        originateAction.setPriority(new Integer(1));
        originateAction.setTimeout(new Long(TIMEOUT)); // 30 seconds timeout
        Debug.logInfo("Calling from: " + originateAction.getChannel() + ", to: " + originateAction.getExten(), MODULE);

        // send the originate action and wait for a maximum of 30 seconds for Asterisk
        // to send a reply
        try {
            managerConnection.sendAction(originateAction, TIMEOUT);
        } catch (IllegalArgumentException e) {
            Debug.logError(e, "Error outbound calling", MODULE);
        } catch (IllegalStateException e) {
            Debug.logError(e, "Error outbound calling", MODULE);
        } catch (IOException e) {
            Debug.logError(e, "Error outbound calling", MODULE);
        } catch (TimeoutException e) {
            Debug.logError(e, "Error outbound calling", MODULE);
        }

    }

    /**
     * Gets the number that asterisk will dial for the given <code>TelecomNumber</code>.
     * @param toTelecomNumber a <code>TelecomNumber</code> to dial
     * @return the number that asterisk will dial
     */
    public String getDialOutNumber(TelecomNumber toTelecomNumber) {
        return getDialOutNumber(toTelecomNumber.getCountryCode(), toTelecomNumber.getAreaCode(), toTelecomNumber.getContactNumber());
    }

    /**
     * Gets the number that asterisk will dial for the given phoneCountryCode/phoneAreaCode/phoneNumber.
     * @param phoneCountryCode phone country code to dial
     * @param phoneAreaCode phone area code to dial
     * @param phoneNumber phone number to dial
     * @return the number that asterisk will dial
     */
    public String getDialOutNumber(String phoneCountryCode, String phoneAreaCode, String phoneNumber) {

        String dialNumber = "";
        // check if the dialed phone number is not in the same country as the asterisk server
        // then adds the international prefix + the destination country code
        if (UtilValidate.isNotEmpty(asteriskCountryCode) && UtilValidate.isNotEmpty(phoneCountryCode) && !asteriskCountryCode.equals(phoneCountryCode)) {
            // checks if the country code already contains the international prefix
            if (phoneCountryCode.startsWith(asteriskInternationalPrefix)) {
                dialNumber = phoneCountryCode;
            } else {
                // else add both the international prefix + country code
                dialNumber = asteriskInternationalPrefix + phoneCountryCode;
            }
        } else if (alwaysDialCountryCode.equals("Y")) {
            // add country code if even though it was the same country code.
            dialNumber = asteriskCountryCode;
        }

        // check if the dialed phone number is not in the same area as the asterisk server
        // then adds the area prefix + the destination area code
        if ((UtilValidate.isNotEmpty(asteriskAreaCode) && UtilValidate.isNotEmpty(phoneAreaCode) && !asteriskAreaCode.equals(phoneAreaCode))
              || alwaysDialAreaCode.equals("Y")) {
            // if no international prefix is used, use the area code directly
            String targetAreaCode = UtilValidate.isNotEmpty(phoneAreaCode) ? phoneAreaCode : asteriskAreaCode;
            if (dialNumber.equals("")) {
                dialNumber = targetAreaCode;
            } else {
                // else since an international prefix was used, there is no need to use an area prefix so:
                // if the area prefix is empty or if the area code do not include the area prefix, we can use the area code directly
                if (UtilValidate.isEmpty(asteriskAreaPrefix) || !targetAreaCode.startsWith(asteriskAreaPrefix)) {
                    dialNumber += targetAreaCode;
                } else {
                    // else, we need to remove the prefix
                    dialNumber += targetAreaCode.substring(asteriskAreaPrefix.length());
                }
            }
        }

        // finally, the main phone number, check that we are not trying to dial the asterisk server
        if (dialNumber.length() == 0 && asteriskPhoneNumber.equals(phoneNumber)) {
            return "";
        }

        dialNumber += phoneNumber;
        // remove all non-digit char
        return dialNumber.replaceAll("\\D", "");
    }
}
