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
import org.opentaps.gwt.common.client.lookup.configuration.TermTypeLookupConfiguration;
import org.opentaps.gwt.common.server.HttpInputProvider;
import org.opentaps.gwt.common.server.InputProviderInterface;
import org.opentaps.base.entities.TermType;
import org.opentaps.foundation.entity.EntityInterface;

/**
 * The RPC service used to populate the Term Type autocompleters widgets.
 */
public class TermTypeLookupService extends EntityLookupAndSuggestService {

    protected TermTypeLookupService(InputProviderInterface provider) {
        super(provider, TermTypeLookupConfiguration.LIST_OUT_FIELDS);
    }

    /**
     * AJAX event to suggest Term Type.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the JSON response
     * @throws InfrastructureException if an error occurs
     */
    public static String suggestTermType(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        TermTypeLookupService service = new TermTypeLookupService(provider);
        service.suggestTermType();
        return json.makeSuggestResponse(TermTypeLookupConfiguration.OUT_TERM_TYPE_ID, service);
    }

    /**
     * Suggests a list of <code>TermType</code>.
     * @return the list of <code>TermType</code>, or <code>null</code> if an error occurred
     */
    private List<TermType> suggestTermType() {
        return findSuggestMatchesAnyOf(TermType.class, TermTypeLookupConfiguration.LIST_OUT_FIELDS);
    }

    @Override
    public String makeSuggestDisplayedText(EntityInterface termType) {
        return termType.getString(TermTypeLookupConfiguration.OUT_DESCRIPTION);
    }

}
