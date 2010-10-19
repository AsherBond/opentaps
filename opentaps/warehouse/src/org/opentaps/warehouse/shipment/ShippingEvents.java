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
package org.opentaps.warehouse.shipment;

import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.security.Security;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.DomainsLoader;
import org.opentaps.base.entities.Facility;
import org.opentaps.domain.billing.BillingDomainInterface;
import org.opentaps.domain.billing.invoice.Invoice;
import org.opentaps.domain.inventory.InventoryDomainInterface;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.warehouse.facility.UtilWarehouse;
import org.opentaps.warehouse.security.WarehouseSecurity;

public final class ShippingEvents {

    private static final String MODULE = ShippingEvents.class.getName();

    /**
     * Before run invoice report from warehouse we should ensure invoice is sales invoice and put
     * facility owner party as organization party.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return a <code>String</code> value
     */
    public static String checkInvoiceReportPreconditions(HttpServletRequest request, HttpServletResponse response) {
        GenericValue userLogin = (GenericValue) request.getSession().getAttribute("userLogin");
        Locale locale = UtilHttp.getLocale(request);
        Security security = (Security) request.getAttribute("security");

        // ensure warehouse invoice view
        String facilityId = UtilCommon.getParameter(request, "facilityId");
        if (UtilValidate.isEmpty(facilityId)) {
            facilityId = UtilWarehouse.getFacilityId(request);
        }
        if (UtilValidate.isEmpty(facilityId)) {
            return UtilMessage.createAndLogEventError(request, "Facility ID is not set in the request.", MODULE);
        }

        WarehouseSecurity wsecurity = new WarehouseSecurity(security, userLogin, facilityId);
        if (!wsecurity.hasFacilityPermission("WRHS_INVOICE_VIEW")) {
            return UtilMessage.createAndLogEventError(request, "Permission WRHS_INVOICE_VIEW denied.", MODULE);
        }

        String invoiceId = UtilCommon.getParameter(request, "invoiceId");

        try {
            DomainsLoader dl = new DomainsLoader(request);
            DomainsDirectory directory = dl.loadDomainsDirectory();
            BillingDomainInterface billingDomain = directory.getBillingDomain();
            InventoryDomainInterface inventoryDomain = directory.getInventoryDomain();

            Invoice invoice = billingDomain.getInvoiceRepository().getInvoiceById(invoiceId);
            if (!invoice.isSalesInvoice()) {
                return UtilMessage.createAndLogEventError(request, "Invoice [" + invoiceId + "] is not a sales invoice.", MODULE);
            }

            Facility facility = inventoryDomain.getInventoryRepository().getFacilityById(facilityId);
            request.getSession().setAttribute("organizationPartyId", facility.getOwnerPartyId());

        } catch (EntityNotFoundException e) {
            UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        } catch (RepositoryException e) {
            UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        } catch (InfrastructureException e) {
            UtilMessage.createAndLogEventError(request, e, locale, MODULE);
        }

        // all tests pass, so allow view
        return "success";
    }

}
