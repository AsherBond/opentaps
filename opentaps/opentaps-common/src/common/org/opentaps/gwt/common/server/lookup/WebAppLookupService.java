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

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import javolution.util.FastList;

import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.webapp.control.LoginWorker;
import org.opentaps.base.entities.OpentapsWebApps;
import org.opentaps.domain.webapp.WebAppRepositoryInterface;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.gwt.common.client.lookup.configuration.WebAppLookupConfiguration;
import org.opentaps.gwt.common.server.HttpInputProvider;
import org.opentaps.gwt.common.server.InputProviderInterface;
/**
 * The RPC service used to support voip integration works.
 */
public class WebAppLookupService  extends EntityLookupService {

    private static final String MODULE = WebAppLookupService.class.getName();
    protected WebAppLookupService(InputProviderInterface provider) {
        super(provider, WebAppLookupConfiguration.LIST_OUT_FIELDS);
    }

    /**
     * AJAX event to perform lookups on purchasing Orders.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the resulting JSON response
     * @throws InfrastructureException if an error occurs
     */
    public static String findWebApps(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        WebAppLookupService service = new WebAppLookupService(provider);
       // LoginWorker also to require this, so here it is:
        HttpSession session = request.getSession(true);
        GenericValue userLogin = (GenericValue) session.getAttribute("userLogin");
        request.setAttribute("userLogin", userLogin);
        // get current application id
        String currentApplicationId = UtilHttp.getApplicationName(request);
        String externalLoginKey = LoginWorker.getExternalLoginKey(request);
        service.findWebApps(currentApplicationId, externalLoginKey);
        return json.makeLookupResponse(WebAppLookupConfiguration.OUT_APPLICATION_ID, service, request.getSession(true).getServletContext());
    }
    
    public List<? extends OpentapsWebApps> findWebApps(String currentApplicationId, String externalLoginKey) {
        try {
            List<OpentapsWebApps> sortedWebapps = new FastList();
            WebAppRepositoryInterface webAppRepository = getDomainsDirectory().getWebAppDomain().getWebAppRepository();
            List<? extends OpentapsWebApps> webapps = webAppRepository.getWebApps(getProvider().getUser());
            for (OpentapsWebApps webapp : webapps) {
                OpentapsWebApps returnWebApp = new OpentapsWebApps();
                // prepare the OpentapsWebApps for return
                returnWebApp.fromEntity(webapp);

                // add ext login parameter for single sign on
                if (UtilValidate.isNotEmpty(externalLoginKey)) {
                    returnWebApp.setLinkUrl(returnWebApp.getLinkUrl() + "?externalLoginKey=" + externalLoginKey);
                }
                if (returnWebApp.getApplicationId().equals(currentApplicationId)) {
                    // set empty string to link
                    returnWebApp.setLinkUrl("");
                }
                sortedWebapps.add(returnWebApp);
            }

            // add other items
            setResultTotalCount(sortedWebapps.size());
            setResults(sortedWebapps);
            return sortedWebapps;
        } catch (RepositoryException e) {
            storeException(e);
            return null;
        }
    }
    
    
}
