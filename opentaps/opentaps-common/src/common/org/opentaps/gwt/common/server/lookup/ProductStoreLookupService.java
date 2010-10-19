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

import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.base.entities.ProductStore;
import org.opentaps.foundation.entity.EntityInterface;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.gwt.common.client.lookup.configuration.ProductStoreLookupConfiguration;
import org.opentaps.gwt.common.server.HttpInputProvider;
import org.opentaps.gwt.common.server.InputProviderInterface;

/**
 * The RPC service used to populate Product Store autocompleters widgets.
 */
public class ProductStoreLookupService extends EntityLookupAndSuggestService {

    protected ProductStoreLookupService(InputProviderInterface provider) {
        super(provider,
              Arrays.asList(ProductStoreLookupConfiguration.OUT_PRODUCT_STORE_ID,
                      ProductStoreLookupConfiguration.OUT_STORE_NAME));
    }

    /**
     * AJAX event to suggest Case Priority.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the resulting JSON response
     * @throws InfrastructureException if an error occurs
     */
    public static String suggestProductStores(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        ProductStoreLookupService service = new ProductStoreLookupService(provider);
        service.suggestProductStores();
        return json.makeSuggestResponse(ProductStoreLookupConfiguration.OUT_PRODUCT_STORE_ID, service);
    }

    /**
     * Gets all Case status.
     * @return the list of Case status <code>StatusItem</code>
     */
    public List<ProductStore> suggestProductStores() {
        String organizationPartyId = UtilProperties.getPropertyValue("opentaps", "organizationPartyId");
        return findList(ProductStore.class, EntityCondition.makeCondition(ProductStore.Fields.payToPartyId.name(), EntityOperator.EQUALS, organizationPartyId));
    }

    @Override
    public String makeSuggestDisplayedText(EntityInterface suggestStatus) {
        return suggestStatus.getString(ProductStoreLookupConfiguration.OUT_STORE_NAME);
    }
}
