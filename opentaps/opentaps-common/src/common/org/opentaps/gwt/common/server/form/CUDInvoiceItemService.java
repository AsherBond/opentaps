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

package org.opentaps.gwt.common.server.form;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.service.GenericServiceException;
import org.opentaps.gwt.common.client.lookup.configuration.InvoiceItemLookupConfiguration;
import org.opentaps.gwt.common.server.HttpInputProvider;
import org.opentaps.gwt.common.server.InputProviderInterface;

/**
 * The RPC service used to create, update or delete an Invoice Item.
 */
public class CUDInvoiceItemService extends GenericCUDService {

    private static final String MODULE = CUDInvoiceItemService.class.getName();

    private String invoiceId;
    private String invoiceSeqId;

    protected CUDInvoiceItemService(InputProviderInterface provider) {
        super(provider);
    }

    @Override
    public void validate() {
        // the CUD action is always required, checks it is present and valid
        validateCUDAction();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Map interceptParameters(Map params) {
        // We need convert the checkbox value into Y/N, since the UI will submit "on" when checked
        // or "true" from the record definition
        // the returned Map is passed directly as the service parameter Map
        String val = (String) params.get(InvoiceItemLookupConfiguration.INOUT_TAXABLE);
        String val2;
        if ("on".equalsIgnoreCase(val) || "true".equalsIgnoreCase(val)) {
            val2 = "Y";
        } else {
            val2 = "N";
        }
        params.put(InvoiceItemLookupConfiguration.INOUT_TAXABLE, val2);
        return params;
    }

    @Override
    protected Map<String, Object> callCreateService() throws GenericServiceException {
        invoiceId = getProvider().getParameter(InvoiceItemLookupConfiguration.INOUT_INVOICE_ID);
        invoiceSeqId = getProvider().getParameter(InvoiceItemLookupConfiguration.INOUT_ITEM_SEQUENCE);
        Debug.logInfo("Trying to create a new invoice item for invoice [" + invoiceId + "]", MODULE);
        if (invoiceSeqId != null) {
            throw new GenericServiceException("Cannot give invoiceSeqId for creation of an Invoice Item.");
        }
        return callService("createInvoiceItem");
    }

    @Override
    protected Map<String, Object> callUpdateService() throws GenericServiceException {
        invoiceId = getProvider().getParameter(InvoiceItemLookupConfiguration.INOUT_INVOICE_ID);
        invoiceSeqId = getProvider().getParameter(InvoiceItemLookupConfiguration.INOUT_ITEM_SEQUENCE);
        Debug.logInfo("Trying to update the invoice item [" + invoiceSeqId + "] for invoice [" + invoiceId + "]", MODULE);
        if (invoiceSeqId == null) {
            throw new GenericServiceException("InvoiceSeqId is required for Update of an Invoice Item.");
        }
        return callService("updateInvoiceItem");
    }

    @Override
    protected Map<String, Object> callDeleteService() throws GenericServiceException {
        invoiceId = getProvider().getParameter(InvoiceItemLookupConfiguration.INOUT_INVOICE_ID);
        invoiceSeqId = getProvider().getParameter(InvoiceItemLookupConfiguration.INOUT_ITEM_SEQUENCE);
        Debug.logInfo("Trying to delete the invoice item [" + invoiceSeqId + "] for invoice [" + invoiceId + "]", MODULE);
        if (invoiceSeqId == null) {
            throw new GenericServiceException("InvoiceSeqId is required for Delete of an Invoice Item.");
        }
        return callService("removeInvoiceItem");
    }

    /**
     * AJAX event for Invoice Item CUD.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the JSON response
     */
    public static String postInvoiceItem(HttpServletRequest request, HttpServletResponse response) {
        JsonResponse json = new JsonResponse(response);
        try {
            InputProviderInterface provider = new HttpInputProvider(request);
            CUDInvoiceItemService service = new CUDInvoiceItemService(provider);
            return json.makeResponse(service.call());
        } catch (Throwable e) {
            return json.makeResponse(e);
        }
    }

    /**
     * AJAX event for batch Invoice Item CUD.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the JSON response
     */
    public static String postInvoiceItemBatch(HttpServletRequest request, HttpServletResponse response) {
        JsonResponse json = new JsonResponse(response);
        try {
            InputProviderInterface provider = new HttpInputProvider(request);
            CUDInvoiceItemService service = new CUDInvoiceItemService(provider);
            return json.makeResponse(service.callServiceBatch());
        } catch (Throwable e) {
            return json.makeResponse(e);
        }
    }

}
