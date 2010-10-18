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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.opentaps.base.entities.InvoiceItemTypeAndOrgGlAccount;
import org.opentaps.domain.billing.invoice.InvoiceRepositoryInterface;
import org.opentaps.foundation.entity.EntityInterface;
import org.opentaps.foundation.exception.FoundationException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.gwt.common.client.lookup.configuration.InvoiceItemTypeLookupConfiguration;
import org.opentaps.gwt.common.server.HttpInputProvider;
import org.opentaps.gwt.common.server.InputProviderInterface;
import org.opentaps.base.entities.InvoiceItemType;

/**
 * The RPC service used to populate the Invoice Item Type autocompleters widgets.
 */
public class InvoiceItemTypeLookupService extends EntityLookupAndSuggestService {

    private static String MODULE = InvoiceItemTypeLookupService.class.getName();

    private String organizationPartyId;
    private String invoiceTypeId;
    private InvoiceRepositoryInterface repository;

    protected InvoiceItemTypeLookupService(InputProviderInterface provider) throws RepositoryException {
        super(provider, InvoiceItemTypeLookupConfiguration.LIST_OUT_FIELDS);

        organizationPartyId = getProvider().getParameter(InvoiceItemTypeLookupConfiguration.IN_ORGANIZATION);
        invoiceTypeId = getProvider().getParameter(InvoiceItemTypeLookupConfiguration.IN_INVOICE_TYPE);
        // auto set the invoice repository as the service repository
        repository = getDomainsDirectory().getBillingDomain().getInvoiceRepository();
        setRepository(repository);
    }

    /**
     * AJAX event to suggest Invoice Item Type.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the JSON response
     * @throws FoundationException if an error occurs
     */
    public static String suggestInvoiceItemType(HttpServletRequest request, HttpServletResponse response) throws FoundationException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        InvoiceItemTypeLookupService service = new InvoiceItemTypeLookupService(provider);
        service.suggestInvoiceItemType();
        return json.makeSuggestResponse(InvoiceItemTypeLookupConfiguration.OUT_TYPE_ID, service);
    }

    /**
     * Finds invoice item types that apply to the given organization and invoice type, for which the description matches the input.
     * @return the JSON response
     */
    private List<InvoiceItemType> suggestInvoiceItemType() {
        if (organizationPartyId == null) {
            Debug.logError("Missing required parameter organizationPartyId", MODULE);
            return null;
        }
        if (invoiceTypeId == null) {
            Debug.logError("Missing required parameter invoiceTypeId", MODULE);
            return null;
        }

        try {
            List<InvoiceItemType> types = repository.getApplicableInvoiceItemTypes(invoiceTypeId, organizationPartyId);
            return paginateResults(types);
        } catch (RepositoryException e) {
            storeException(e);
            return null;
        }

    }

    @Override
    public String makeSuggestDisplayedText(EntityInterface invoiceItemType) {
        return invoiceItemType.getString(InvoiceItemTypeLookupConfiguration.OUT_DESCRIPTION);
    }

}
