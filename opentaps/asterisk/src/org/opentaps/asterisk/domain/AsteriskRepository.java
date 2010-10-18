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
package org.opentaps.asterisk.domain;

import java.util.Set;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.opentaps.asterisk.AsteriskUtil;
import org.opentaps.common.party.PartyHelper;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.base.entities.ExternalUser;
import org.opentaps.base.entities.TelecomNumber;
import org.opentaps.domain.party.Party;
import org.opentaps.domain.party.PartyDomainInterface;
import org.opentaps.domain.party.PartyRepositoryInterface;
import org.opentaps.domain.voip.VoipRepositoryInterface;
import org.opentaps.foundation.infrastructure.Infrastructure;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.infrastructure.User;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.ofbiz.Repository;

/** {@inheritDoc} */
public class AsteriskRepository extends Repository implements VoipRepositoryInterface {
    private static final String MODULE = AsteriskRepository.class.getName();
    private static final String ASTERISK_USERTYPE_ID = "ASTERISK";

    /**
     * Default constructor.
     */
    public AsteriskRepository() {
        super();
    }

    /**
     * If you want the full infrastructure including the dispatcher, then you must have the User.
     *
     * @param infrastructure the domain infrastructure
     * @param userLogin the Ofbiz <code>UserLogin</code> generic value
     * @throws RepositoryException if an error occurs
     */
    public AsteriskRepository(Infrastructure infrastructure, GenericValue userLogin)
            throws RepositoryException {
        super(infrastructure, userLogin);
    }

    /** {@inheritDoc} */
    public ExternalUser getVoipExtensionForUser(User user) throws RepositoryException {
        PartyDomainInterface partyDomain = DomainsDirectory.getDomainsDirectory(this).getPartyDomain();
        PartyRepositoryInterface repository = partyDomain.getPartyRepository();
        ExternalUser externalUser;
        try {
            externalUser = repository.getExternalUserForUser(ASTERISK_USERTYPE_ID, user);
        } catch (InfrastructureException e) {
            throw new RepositoryException(e);
        }
        return externalUser;
    }

    /** {@inheritDoc} */
    public String getCallInPartyLink(User user) throws RepositoryException {
        PartyDomainInterface partyDomain = DomainsDirectory.getDomainsDirectory(this).getPartyDomain();
        PartyRepositoryInterface repository = partyDomain.getPartyRepository();
        // retrieve login user's extension phone number
        try {
            ExternalUser externalUser = getVoipExtensionForUser(user);
            String extension = "";
            if (externalUser != null && externalUser.getExternalUserId() != null) {
                extension = externalUser.getExternalUserId();
            }
            if (!extension.equals("")) {
                // get the last in-bound number to this extension
                String callInNumber = AsteriskUtil.getInstance().getLastCallTo(extension);
                if (!UtilValidate.isEmpty(callInNumber)) {
                    Debug.logInfo("call in phone no:" + callInNumber + ".", MODULE);
                    // retrieve parties by phone number
                    Set<Party> parties = repository.getPartyByPhoneNumber(callInNumber);
                    if (parties.size() > 0) {
                        // construct pop-up window url string
                        Party party = parties.iterator().next();
                        String link = PartyHelper.createViewPageLink(party.getPartyId(), repository.getInfrastructure().getDelegator(), null);
                        Debug.logInfo("Find Call in Party, PartyId=" + party.getPartyId() + " ,url=" + link, MODULE);
                        return link;
                    }
                    Debug.logInfo("PhoneNumber:" + callInNumber + " cannot found any match party", MODULE);
                } else {
                    Debug.logInfo("Cannot find any call in number.", MODULE);
                }
            } else {
                Debug.logError("Current UserLogin do not have any extension set.", MODULE);
            }
        } catch (GenericEntityException e) {
            throw new RepositoryException(e);
        }
        return "error";
    }

    /** {@inheritDoc} */
    public void makeOutgoingCall(User user, String countryCode, String areaCode, String phoneNumber) throws RepositoryException {
        try {
            PartyDomainInterface partyDomain = DomainsDirectory.getDomainsDirectory(this).getPartyDomain();
            PartyRepositoryInterface repository = partyDomain.getPartyRepository();
            ExternalUser externalUser = repository.getExternalUserForUser(ASTERISK_USERTYPE_ID, user);
            String dialNo = AsteriskUtil.getInstance().getDialOutNumber(countryCode, areaCode, phoneNumber);
            if (externalUser != null && UtilValidate.isNotEmpty(externalUser.getExternalUserId())) {
                // retrieve asterisk extenstion number
                String fromExtension = externalUser.getExternalUserId();
                // call asterisk function with your extension number and target number
                Debug.logInfo("Making call via Asterisk, from [" + fromExtension + "], to ["  + dialNo + "]", MODULE);
                AsteriskUtil.getInstance().call(fromExtension, dialNo);
            }
        } catch (InfrastructureException e) {
            throw new RepositoryException(e);
        }
    }

    /** {@inheritDoc} */
    public void makeOutgoingCall(User user, TelecomNumber telecomNumber) throws RepositoryException {
        try {
            PartyDomainInterface partyDomain = DomainsDirectory.getDomainsDirectory(this).getPartyDomain();
            PartyRepositoryInterface repository = partyDomain.getPartyRepository();
            ExternalUser externalUser = repository.getExternalUserForUser(ASTERISK_USERTYPE_ID, user);
            String dialNo = AsteriskUtil.getInstance().getDialOutNumber(telecomNumber);
            if (externalUser != null && UtilValidate.isNotEmpty(externalUser.getExternalUserId())) {
                // retrieve asterisk extenstion number
                String fromExtension = externalUser.getExternalUserId();
                // call asterisk function with your extension number and target number
                Debug.logInfo("Making call via Asterisk, from [" + fromExtension + "], to ["  + dialNo + "]", MODULE);
                AsteriskUtil.getInstance().call(fromExtension, dialNo);
            }
        } catch (InfrastructureException e) {
            throw new RepositoryException(e);
        }
    }

}
