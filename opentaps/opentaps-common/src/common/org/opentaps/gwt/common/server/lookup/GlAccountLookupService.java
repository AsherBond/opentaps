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

import org.opentaps.base.entities.GlAccount;
import org.opentaps.foundation.entity.EntityInterface;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.gwt.common.client.lookup.configuration.GlAccountLookupConfiguration;
import org.opentaps.gwt.common.server.HttpInputProvider;
import org.opentaps.gwt.common.server.InputProviderInterface;

/**
 * The RPC service used to populate the GL Account autocompleters widgets.
 */
public class GlAccountLookupService extends EntityLookupAndSuggestService {

    protected GlAccountLookupService(InputProviderInterface provider) {
        super(provider, GlAccountLookupConfiguration.LIST_OUT_FIELDS);
    }

    /**
     * AJAX event to suggest General Ledger Account.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the JSON response
     * @throws InfrastructureException if an error occurs
     */
    public static String suggestGlAccount(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        GlAccountLookupService service = new GlAccountLookupService(provider);
        service.suggestGlAccount();
        return json.makeSuggestResponse(GlAccountLookupConfiguration.OUT_GL_ACCOUNT_ID, service);
    }

    private List<GlAccount> suggestGlAccount() {
        return findSuggestMatchesAnyOf(GlAccount.class, GlAccountLookupConfiguration.LIST_OUT_FIELDS);
    }

    @Override
    public String makeSuggestDisplayedText(EntityInterface account) {
        StringBuilder sb = new StringBuilder();
        sb.append(account.getString(GlAccountLookupConfiguration.OUT_CODE)).append(":").append(account.getString(GlAccountLookupConfiguration.OUT_NAME));
        return sb.toString();
    }

}
