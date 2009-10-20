package org.opentaps.common.event;
/*
 * Copyright (c) 2008 - 2009 Open Source Strategies, Inc.
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

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.opentaps.common.asterisk.AsteriskUtil;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.domain.base.entities.AsteriskUser;
import org.opentaps.domain.base.entities.TelecomNumber;
import org.opentaps.domain.party.Party;
import org.opentaps.domain.party.PartyDomainInterface;
import org.opentaps.domain.party.PartyRepositoryInterface;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.repository.RepositoryException;

/**
 * Asterisk events to be invoked by the controller.
 */
public class AsteriskEvents {
    public static final String module = AsteriskEvents.class.getName();
    /**
     * This method will make a outgoing call via asterisk.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     */
    public static String makeOutgoingCall(HttpServletRequest request, HttpServletResponse response) {
        Map parameters = UtilHttp.getParameterMap(request);
        //retrieve userLogin from request
        GenericValue userLogin = (GenericValue) request.getAttribute("userLogin");
        if (userLogin == null) {
            HttpSession session = request.getSession();
            if (session != null) {
                userLogin = (GenericValue) session.getAttribute("userLogin");
            }
        }
        // get request parameter, we can use internalPartyId + contactMechIdTo to call this function,
        // or use primaryPhoneCountryCode + primaryPhoneAreaCode + primaryPhoneNumber to call this function.
        String partyIdTo = (String) parameters.get("internalPartyId");
        String contactMechIdTo = (String) parameters.get("contactMechIdTo");
        String primaryPhoneCountryCode = (String) parameters.get("primaryPhoneCountryCode");
        String primaryPhoneAreaCode = (String) parameters.get("primaryPhoneAreaCode");
        String primaryPhoneNumber = (String) parameters.get("primaryPhoneNumber");
        if (UtilValidate.isNotEmpty(primaryPhoneNumber)) {
            // if have primaryPhoneNumber parameter
            Debug.logInfo(userLogin.getString("userLoginId") + " makeOutgoingCall [" + primaryPhoneCountryCode + "-"  + primaryPhoneAreaCode + "-" + primaryPhoneNumber + "]", module);
        } else {
            // else is use internalPartyId + contactMechIdTo to call this function
            Debug.logInfo(userLogin.getString("userLoginId") + " makeOutgoingCall [" + partyIdTo + ","  + contactMechIdTo + "]", module);
        }
        String fromExtension = "";
        try {
            DomainsLoader domainLoader = new DomainsLoader(request);
            PartyDomainInterface partyDomain = domainLoader.loadDomainsDirectory().getPartyDomain();
            PartyRepositoryInterface repository = partyDomain.getPartyRepository();
            // get asteriskUser by current userLogin
            AsteriskUser asteriskUser = repository.getAsteriskUserForUser(new User(userLogin));
            String dialNo = "";
            if (asteriskUser != null && UtilValidate.isNotEmpty(asteriskUser.getExtension())) {
                // retrieve asterisk extenstion number
                fromExtension = asteriskUser.getExtension();
                if (UtilValidate.isNotEmpty(contactMechIdTo)) {
                    // retrieve dial number by contactMechIdTo + partyIdTo parameters
                        if (!fromExtension.equals("")) {
                            // retrieve outbound phone number
                            Party toParty = repository.getPartyById(partyIdTo);
                            TelecomNumber toTelecomNumber = toParty.getTelecomNumberByContactMechId(contactMechIdTo);
                            if (toTelecomNumber != null) {
                                // generate the number which to dial
                                dialNo = AsteriskUtil.getInstance().getDialOutNumber(toTelecomNumber);
                            }
                        }
                } else if (UtilValidate.isNotEmpty(primaryPhoneNumber)) {
                    // retrieve dial number by primaryPhoneCountryCode + primaryPhoneAreaCode + primaryPhoneNumber parameters
                    // generate the number which to dial
                    dialNo = AsteriskUtil.getInstance().getDialOutNumber(primaryPhoneCountryCode, primaryPhoneAreaCode, primaryPhoneNumber);
                }
                if (!dialNo.equals("")) {
                    // call asterisk function with your extension number and target number
                    Debug.logInfo("Making call via Asterisk, from [" + fromExtension + "], to ["  + dialNo + "]", module);
                    AsteriskUtil.getInstance().call(fromExtension, dialNo);
                } else {
                    // can't find any phone number to dial
                    return "error";
                }
            }

        } catch (InfrastructureException e) {
            return UtilMessage.createAndLogEventError(request, e, UtilHttp.getLocale(request), module);
        } catch (RepositoryException e) {
            return UtilMessage.createAndLogEventError(request, e, UtilHttp.getLocale(request), module);
        } catch (EntityNotFoundException e) {
            return UtilMessage.createAndLogEventError(request, e, UtilHttp.getLocale(request), module);
        }
        return "success";
    }
}
