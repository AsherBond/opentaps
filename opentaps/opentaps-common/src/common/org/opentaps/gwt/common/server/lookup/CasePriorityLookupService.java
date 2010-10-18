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

import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.base.entities.Enumeration;
import org.opentaps.foundation.entity.EntityInterface;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.gwt.common.client.lookup.configuration.CasePriorityLookupConfiguration;
import org.opentaps.gwt.common.server.HttpInputProvider;
import org.opentaps.gwt.common.server.InputProviderInterface;
/**
 * The RPC service used to populate Case Priority autocompleters widgets.
 */
public class CasePriorityLookupService extends EntityLookupAndSuggestService {

    protected CasePriorityLookupService(InputProviderInterface provider) {
        super(provider,
              Arrays.asList(CasePriorityLookupConfiguration.OUT_ENUM_CODE,
                      CasePriorityLookupConfiguration.OUT_DESCRIPTION,
                      CasePriorityLookupConfiguration.OUT_SEQUENCE_ID));
    }

    /**
     * AJAX event to suggest Case Priority.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the resulting JSON response
     * @throws InfrastructureException if an error occurs
     */
    public static String suggestCasePriorities(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        CasePriorityLookupService service = new CasePriorityLookupService(provider);
        service.suggestPriorities();
        return json.makeSuggestResponse(CasePriorityLookupConfiguration.OUT_ENUM_CODE, service);
    }

    /**
     * Gets all Case priorities.
     * @return the list of party classifications <code>Enumeration</code>
     */
    public List<Enumeration> suggestPriorities() {
        return findList(Enumeration.class, EntityCondition.makeCondition(Enumeration.Fields.enumTypeId.name(), EntityOperator.EQUALS, "PRIORITY_LEV"));
    }

    @Override
    public String makeSuggestDisplayedText(EntityInterface suggestPriority) {
        return suggestPriority.getString(CasePriorityLookupConfiguration.OUT_DESCRIPTION);
    }
}
