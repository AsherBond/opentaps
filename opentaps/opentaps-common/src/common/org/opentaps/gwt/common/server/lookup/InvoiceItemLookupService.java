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

import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.condition.EntityCondition;
import org.opentaps.base.entities.InvoiceItemAndDescriptions;
import org.opentaps.domain.billing.invoice.Invoice;
import org.opentaps.domain.billing.invoice.InvoiceRepositoryInterface;
import org.opentaps.foundation.exception.FoundationException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.gwt.common.client.lookup.configuration.InvoiceItemLookupConfiguration;
import org.opentaps.gwt.common.server.HttpInputProvider;
import org.opentaps.gwt.common.server.InputProviderInterface;

/**
 * The RPC service used to populate the InvoiceItemListEditor.
 */
public class InvoiceItemLookupService extends EntityLookupService {

    private static final String MODULE = InvoiceItemLookupService.class.getName();

    private String invoiceId;
    private InvoiceRepositoryInterface repository;

    protected InvoiceItemLookupService(InputProviderInterface provider) throws RepositoryException {
        super(provider, InvoiceItemLookupConfiguration.LIST_OUT_FIELDS);
        invoiceId = getProvider().getParameter(InvoiceItemLookupConfiguration.INOUT_INVOICE_ID);
        // auto set the invoice repository as the service repository
        repository = getDomainsDirectory().getBillingDomain().getInvoiceRepository();
        setRepository(repository);
    }

    /**
     * AJAX event to perform lookups on Invoice Items.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the resulting JSON response
     * @throws FoundationException if an error occurs
     */
    public static String findInvoiceItems(HttpServletRequest request, HttpServletResponse response)  throws FoundationException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        InvoiceItemLookupService service = new InvoiceItemLookupService(provider);
        service.findInvoiceItems();
        return json.makeLookupResponse(InvoiceItemLookupConfiguration.INOUT_ITEM_SEQUENCE, service, request.getSession(true).getServletContext());
    }

    /**
     * Finds invoice items that apply to the given invoice.
     * @return the list of <code>InvoiceItem</code>, or <code>null</code> if an error occurred
     * @throws FoundationException if an error occurs
     */
    public List<InvoiceItemAndDescriptions> findInvoiceItems() throws FoundationException {
        // invoiceId is required
        if (invoiceId == null) {
            Debug.logError("Missing required parameter invoiceId", MODULE);
            return null;
        }
        // find the invoice
        Invoice invoice = repository.getInvoiceById(invoiceId);
        getGlobalPermissions().setAll(invoice.isModifiable());

        return findList(InvoiceItemAndDescriptions.class, EntityCondition.makeCondition("invoiceId", invoiceId));
    }
}

