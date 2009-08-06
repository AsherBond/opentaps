/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 *
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
 */

package org.opentaps.gwt.common.server.lookup;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opentaps.domain.base.entities.ProductAndGoodIdentification;
import org.opentaps.foundation.entity.EntityInterface;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.gwt.common.client.lookup.configuration.ProductLookupConfiguration;
import org.opentaps.gwt.common.server.HttpInputProvider;
import org.opentaps.gwt.common.server.InputProviderInterface;

/**
 * The RPC service used to populate the Product autocompleters widgets.
 */
public class ProductLookupService extends EntityLookupAndSuggestService {

    protected ProductLookupService(InputProviderInterface provider) {
        super(provider, ProductLookupConfiguration.LIST_OUT_FIELDS);
    }

    /**
     * AJAX event to suggest Product.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the JSON response
     * @throws InfrastructureException if an error occurs
     */
    public static String suggestProduct(HttpServletRequest request, HttpServletResponse response) throws InfrastructureException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        ProductLookupService service = new ProductLookupService(provider);
        service.suggestProduct();
        return json.makeSuggestResponse(ProductLookupConfiguration.OUT_PRODUCT_ID, service);
    }

    /**
     * Suggests a list of <code>Product</code>.
     * @return the list of <code>Product</code>, or <code>null</code> if an error occurred
     */
    public List<ProductAndGoodIdentification> suggestProduct() {
        return findSuggestMatchesAnyOf(ProductAndGoodIdentification.class, ProductLookupConfiguration.LIST_LOOKUP_FIELDS);
    }

    @Override
    public String makeSuggestDisplayedText(EntityInterface product) {
        StringBuilder sb = new StringBuilder();
        sb.append(product.getString(ProductLookupConfiguration.OUT_PRODUCT_ID)).append(":").append(product.getString(ProductLookupConfiguration.OUT_INTERNAL_NAME));
        return sb.toString();
    }

}
