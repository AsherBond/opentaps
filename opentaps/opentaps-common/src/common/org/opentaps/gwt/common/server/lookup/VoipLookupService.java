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
package org.opentaps.gwt.common.server.lookup;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.entity.GenericEntityException;
import org.opentaps.common.query.QueryException;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.domain.voip.VoipDomainInterface;
import org.opentaps.domain.voip.VoipRepositoryInterface;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.gwt.common.client.lookup.configuration.PartyLookupConfiguration;
import org.opentaps.gwt.common.server.HttpInputProvider;
import org.opentaps.gwt.common.server.InputProviderInterface;
/**
 * The RPC service used to support voip integration works.
 */
public class VoipLookupService  extends EntityLookupService {

    private static final String MODULE = VoipLookupService.class.getName();

    protected VoipLookupService(InputProviderInterface provider) {
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
        VoipLookupService service = new VoipLookupService(provider);
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
    public static String checkFrequencySeconds(HttpServletRequest request, HttpServletResponse response) {
        String checkFrequencySeconds = "";
        if (UtilProperties.getPropertyValue("voip.properties", "voip.enabled", "N").equals("Y")) {
            checkFrequencySeconds = UtilProperties.getPropertyValue("voip.properties", "voip.checkFrequencySeconds", "");
        }
        String result = writeSimpleStringResponse(response, checkFrequencySeconds);
        return result;
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
        // check if enabled voip
        if (UtilProperties.getPropertyValue("voip.properties", "voip.enabled", "N").equals("Y")) {
            if (getProvider().getUser() != null) {
                DomainsLoader domainLoader = new DomainsLoader(getProvider().getInfrastructure(), getProvider().getUser());
                VoipDomainInterface voipDomain = domainLoader.loadDomainsDirectory().getVoipDomain();
                VoipRepositoryInterface repository = voipDomain.getVoipRepository();
                // retrieve login user's extension phone number
                String link =  repository.getCallInPartyLink(getProvider().getUser());
                return link;
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