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
package org.opentaps.common.event;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.base.entities.TelecomNumber;
import org.opentaps.domain.party.Party;
import org.opentaps.domain.party.PartyRepositoryInterface;
import org.opentaps.domain.voip.VoipRepositoryInterface;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.repository.RepositoryException;

/**
 * Voip events to be invoked by the controller.
 */
public class VoipEvents {
    public static final String MODULE = VoipEvents.class.getName();
    
    /**
     * This method will make a outgoing call via voip provider.
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

        try {
            DomainsLoader domainLoader = new DomainsLoader(request);
            VoipRepositoryInterface voipRepository = domainLoader.loadDomainsDirectory().getVoipDomain().getVoipRepository();
            PartyRepositoryInterface partyRepository = domainLoader.loadDomainsDirectory().getPartyDomain().getPartyRepository();
         // get asteriskUser by current userLogin
            if (UtilValidate.isNotEmpty(primaryPhoneNumber)) {
                // if have primaryPhoneNumber parameter
                Debug.logInfo(userLogin.getString("userLoginId") + " makeOutgoingCall [" + primaryPhoneCountryCode + "-"  + primaryPhoneAreaCode + "-" + primaryPhoneNumber + "]", MODULE);
                voipRepository.makeOutgoingCall(new User(userLogin), primaryPhoneCountryCode, primaryPhoneAreaCode, primaryPhoneNumber);
            } else {
                // else is use internalPartyId + contactMechIdTo to call this function
                // retrieve outbound phone number
                Party toParty = partyRepository.getPartyById(partyIdTo);
                TelecomNumber toTelecomNumber = toParty.getTelecomNumberByContactMechId(contactMechIdTo);
                if (toTelecomNumber != null) {
                    Debug.logInfo(userLogin.getString("userLoginId") + " makeOutgoingCall [" + partyIdTo + ","  + contactMechIdTo + "]", MODULE);
                    voipRepository.makeOutgoingCall(new User(userLogin), toTelecomNumber);
                }
            }
            return "sucess";

        } catch (InfrastructureException e) {
            Debug.logError(e, MODULE);
            return "error";
        } catch (RepositoryException e) {
            Debug.logError(e, MODULE);
            return "error";
        } catch (EntityNotFoundException e) {
            Debug.logError(e, MODULE);
            return "error";
        }
    }
}
