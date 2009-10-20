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
package org.opentaps.gwt.common.server.lookup;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericEntityException;
import org.opentaps.common.asterisk.AsteriskUtil;
import org.opentaps.common.party.PartyHelper;
import org.opentaps.common.query.QueryException;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.domain.base.entities.AsteriskUser;
import org.opentaps.domain.base.entities.UserLogin;
import org.opentaps.domain.party.Party;
import org.opentaps.domain.party.PartyDomainInterface;
import org.opentaps.domain.party.PartyRepositoryInterface;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.gwt.common.client.lookup.configuration.PartyLookupConfiguration;
import org.opentaps.gwt.common.server.HttpInputProvider;
import org.opentaps.gwt.common.server.InputProviderInterface;

/**
 * The RPC service used to support asterisk pop-up works.
 */
public class AsteriskLookupService extends EntityLookupService {

    private static final String MODULE = AsteriskLookupService.class.getName();

    protected AsteriskLookupService(InputProviderInterface provider) {
        super(provider, PartyLookupConfiguration.LIST_OUT_FIELDS);
    }

    /**
     * AJAX event to perform lookups Account by call in phone no, return link of call in GWT.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the link of call in GWT
     * @throws InfrastructureException if an error occurs
     */
    public static String callInAccount(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        AsteriskLookupService service = new AsteriskLookupService(provider);
        String result = "error";
        try {
            result = writeSimpleStringResponse(response, service.findCallInParty());
        } catch (InfrastructureException e) {
            Debug.logError(e, "Error retrieve Inbound Phone Number, Exception:" + e.getMessage(), MODULE);
        } catch (EntityNotFoundException e) {
            Debug.logError(e, "Error retrieve Inbound Phone Number, Exception:" + e.getMessage(), MODULE);
        } catch (RepositoryException e) {
            Debug.logError(e, "Error retrieve Inbound Phone Number, Exception:" + e.getMessage(), MODULE);
        } catch (QueryException e) {
            Debug.logError(e, "Error retrieve Inbound Phone Number, Exception:" + e.getMessage(), MODULE);
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error retrieve Inbound Phone Number, Exception:" + e.getMessage(), MODULE);
        } catch (SQLException e) {
            Debug.logError(e, "Error retrieve Inbound Phone Number, Exception:" + e.getMessage(), MODULE);
        }

        return result;
    }


    /**
     * Gets the frequency of in-bound checks, in seconds.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return in-bound check frequency in seconds
     */
    public static String asteriskCheckFrequencySeconds(HttpServletRequest request, HttpServletResponse response) {
        String checkFrequencySeconds = "";
        Debug.logInfo("asterisk.enabled :" + UtilProperties.getPropertyValue("asterisk.properties", "asterisk.enabled", "N"), MODULE);
        Debug.logInfo("asterisk.checkFrequencySeconds :" + UtilProperties.getPropertyValue("asterisk.properties", "asterisk.checkFrequencySeconds", ""), MODULE);
        if (UtilProperties.getPropertyValue("asterisk.properties", "asterisk.enabled", "N").equals("Y")) {
            checkFrequencySeconds = UtilProperties.getPropertyValue("asterisk.properties", "asterisk.checkFrequencySeconds", "");
        }
        String result = writeSimpleStringResponse(response, checkFrequencySeconds);
        return result;
    }

    /**
     * Gets the userLogin corresponding to the given extension.
     * @param extension an asterisk extension value
     * @return userLogin
     * @throws InfrastructureException if an error occurs
     * @throws RepositoryException if an error occurs
     */
    public UserLogin getUserByExtension(String extension) throws InfrastructureException, RepositoryException {
        if (UtilValidate.isEmpty(extension)) {
            return null;
        }
        DomainsLoader domainLoader = new DomainsLoader(getProvider().getInfrastructure(), getProvider().getUser());
        PartyDomainInterface partyDomain = domainLoader.loadDomainsDirectory().getPartyDomain();
        PartyRepositoryInterface repository = partyDomain.getPartyRepository();
        return repository.getUserLoginByExtension(extension);

    }

    /**
     * Finds the Account matching the current call in.
     * @return the party view url
     * @throws InfrastructureException if an error occurs
     * @throws RepositoryException if an error occurs
     * @throws EntityNotFoundException if an error occurs
     * @throws SQLException if an error occurs
     * @throws GenericEntityException if an error occurs
     * @throws QueryException if an error occurs
     */
    public String findCallInParty() throws InfrastructureException, EntityNotFoundException, RepositoryException, QueryException, GenericEntityException, SQLException {
        // check if enabled asterisk
        if (UtilProperties.getPropertyValue("asterisk.properties", "asterisk.enabled", "N").equals("Y")) {
            if (getProvider().getUser() != null) {
                DomainsLoader domainLoader = new DomainsLoader(getProvider().getInfrastructure(), getProvider().getUser());
                PartyDomainInterface partyDomain = domainLoader.loadDomainsDirectory().getPartyDomain();
                PartyRepositoryInterface repository = partyDomain.getPartyRepository();
                // retrieve login user's extension phone number
                AsteriskUser asteriskUser = repository.getAsteriskUserForUser(getProvider().getUser());
                String extension = "";
                if (asteriskUser != null && asteriskUser.getExtension() != null) {
                    extension = asteriskUser.getExtension();
                }
                if (!extension.equals("")) {
                    // get the last in-bound number to this extension
                    String callInNumber = AsteriskUtil.getInstance().getLastCallTo(extension);
                    if (!callInNumber.equals("")) {
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

            } else {
                Debug.logError("Current session do not have any UserLogin set.", MODULE);
            }
        }
        return "error";
    }

    /**
     * Write simple string into response.
     * @param response a <code>HttpServletResponse</code> value
     * @param returnString a <code>String</code> value
     * @return write status
     */
    private static String writeSimpleStringResponse(HttpServletResponse response, String returnString) {
        String result = "error";
        if (!returnString.equals("error")) {
            response.setHeader("Cache-Control", "no-store"); // HTTP 1.1
            response.setHeader("Pragma", "no-cache"); // HTTP 1.0
            response.setDateHeader("Expires", 0); // prevents caching at the proxy server
            response.setContentLength(returnString.length());
            Writer out;
            try {
                out = response.getWriter();
                out.write(returnString);
                out.flush();
                result = "sucess";
            } catch (IOException e) {
                Debug.logError(e, "Failed to get response writer", MODULE);
            }
        }
        return result;
    }
}
