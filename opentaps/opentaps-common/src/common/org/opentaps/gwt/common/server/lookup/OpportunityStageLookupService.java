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

import org.opentaps.base.entities.SalesOpportunityStage;
import org.opentaps.foundation.entity.EntityInterface;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.gwt.common.client.lookup.configuration.OpportunityStageLookupConfiguration;
import org.opentaps.gwt.common.server.HttpInputProvider;
import org.opentaps.gwt.common.server.InputProviderInterface;
/**
 * The RPC service used to populate Opportunity Priority autocompleters widgets.
 */
public class OpportunityStageLookupService extends EntityLookupAndSuggestService {

    protected OpportunityStageLookupService(InputProviderInterface provider) {
        super(provider,
              Arrays.asList(OpportunityStageLookupConfiguration.OUT_OPPORTUNITY_STAGE_ID,
                      OpportunityStageLookupConfiguration.OUT_DESCRIPTION,
                      OpportunityStageLookupConfiguration.OUT_SEQUENCE_NUM));
    }

    /**
     * AJAX event to suggest Opportunity Priority.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the resulting JSON response
     * @throws InfrastructureException if an error occurs
     */
    public static String suggestOpportunityStages(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        OpportunityStageLookupService service = new OpportunityStageLookupService(provider);
        service.suggestOpportunityStage();
        return json.makeSuggestResponse(OpportunityStageLookupConfiguration.OUT_OPPORTUNITY_STAGE_ID, service);
    }

    /**
     * Gets all Opportunity stages.
     * @return the list of Opportunity status <code>StatusItem</code>
     */
    public List<SalesOpportunityStage> suggestOpportunityStage() {
        return findAll(SalesOpportunityStage.class);
    }

    @Override
    public String makeSuggestDisplayedText(EntityInterface suggest) {
        return suggest.getString(OpportunityStageLookupConfiguration.OUT_DESCRIPTION);
    }
}
