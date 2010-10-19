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

import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.gwt.common.client.lookup.configuration.TaxAuthorityLookupConfiguration;
import org.opentaps.gwt.common.server.HttpInputProvider;
import org.opentaps.gwt.common.server.InputProviderInterface;
import org.opentaps.base.entities.TaxAuthorityAndDetail;
import org.opentaps.foundation.entity.EntityInterface;

/**
 * The RPC service used to populate the Tax Authority autocompleters widgets.
 */
public class TaxAuthorityLookupService extends EntityLookupAndSuggestService {

    protected TaxAuthorityLookupService(InputProviderInterface provider) {
        super(provider, TaxAuthorityLookupConfiguration.LIST_OUT_FIELDS);
    }

    /**
     * AJAX event to suggest Tax Authorities.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the JSON response
     * @throws InfrastructureException if an error occurs
     */
    public static String suggestTaxAuthorities(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        TaxAuthorityLookupService service = new TaxAuthorityLookupService(provider);
        service.suggestTaxAuthority();
        return json.makeSuggestResponse(TaxAuthorityLookupConfiguration.OUT_TAX_ID, service);
    }

    /**
     * Suggests a list of <code>TaxAuthority</code>.
     * @return the list of <code>TaxAuthority</code>, or <code>null</code> if an error occurred
     */
    public List<TaxAuthorityAndDetail> suggestTaxAuthority() {
        return findSuggestMatchesAnyOf(TaxAuthorityAndDetail.class, TaxAuthorityLookupConfiguration.LIST_LOOKUP_FIELDS);
    }

    @Override
    public String makeSuggestDisplayedText(EntityInterface taxAuth) {
        return taxAuth.getString(TaxAuthorityLookupConfiguration.OUT_TAX_NAME);
    }

}
