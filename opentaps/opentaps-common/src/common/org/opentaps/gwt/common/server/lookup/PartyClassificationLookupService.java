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

import java.util.Arrays;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opentaps.base.entities.PartyClassificationGroup;
import org.opentaps.foundation.entity.EntityInterface;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.gwt.common.client.lookup.configuration.PartyClassificationLookupConfiguration;
import org.opentaps.gwt.common.server.HttpInputProvider;
import org.opentaps.gwt.common.server.InputProviderInterface;

/**
 * The RPC service used to populate PartyClassification autocompleters widgets.
 */
public class PartyClassificationLookupService extends EntityLookupAndSuggestService {

    protected PartyClassificationLookupService(InputProviderInterface provider) {
        super(provider,
              Arrays.asList(PartyClassificationLookupConfiguration.OUT_CLASSIFICATION_ID,
                            PartyClassificationLookupConfiguration.OUT_DESCRIPTION));
    }

    /**
     * AJAX event to suggest PartyClassifications.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the resulting JSON response
     * @throws InfrastructureException if an error occurs
     */
    public static String suggestClassifications(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        PartyClassificationLookupService service = new PartyClassificationLookupService(provider);
        service.suggestClassifications();
        return json.makeSuggestResponse(PartyClassificationLookupConfiguration.OUT_CLASSIFICATION_ID, service);
    }

    /**
     * Gets all party classifications.
     * @return the list of party classifications <code>GenericValue</code>
     */
    public List<PartyClassificationGroup> suggestClassifications() {
        return findAll(PartyClassificationGroup.class);
    }

    @Override
    public String makeSuggestDisplayedText(EntityInterface partyClassification) {
        return partyClassification.getString(PartyClassificationLookupConfiguration.OUT_DESCRIPTION);
    }
}
