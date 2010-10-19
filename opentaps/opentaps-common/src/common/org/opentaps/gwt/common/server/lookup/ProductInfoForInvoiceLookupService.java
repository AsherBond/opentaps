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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.opentaps.base.entities.SupplierProduct;
import org.opentaps.domain.billing.invoice.Invoice;
import org.opentaps.domain.billing.invoice.InvoiceRepositoryInterface;
import org.opentaps.domain.product.Product;
import org.opentaps.domain.product.ProductRepositoryInterface;
import org.opentaps.domain.purchasing.PurchasingRepositoryInterface;
import org.opentaps.foundation.entity.Entity;
import org.opentaps.foundation.exception.FoundationException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.gwt.common.client.lookup.configuration.ProductInfoForInvoiceLookupConfiguration;
import org.opentaps.gwt.common.server.HttpInputProvider;
import org.opentaps.gwt.common.server.InputProviderInterface;

/**
 * The RPC service used to retrieve the product info for a product ID.
 */
public class ProductInfoForInvoiceLookupService extends EntityLookupService {

    private static final String MODULE = AccountingTagConfigurationLookupService.class.getName();

    private String productId;
    private String invoiceId;
    private ProductRepositoryInterface repository;

    protected ProductInfoForInvoiceLookupService(InputProviderInterface provider) throws RepositoryException {
        super(provider, ProductInfoForInvoiceLookupConfiguration.LIST_OUT_FIELDS);
        productId = getProvider().getParameter(ProductInfoForInvoiceLookupConfiguration.INOUT_PRODUCT_ID);
        invoiceId = getProvider().getParameter(ProductInfoForInvoiceLookupConfiguration.IN_INVOICE_ID);
        // auto set the product repository as the service repository
        repository = getDomainsDirectory().getProductDomain().getProductRepository();
        setRepository(repository);
    }

    /**
     * AJAX event to fetch the product info.
     * @param request a <code>HttpServletRequest</code> value
     * @param response a <code>HttpServletResponse</code> value
     * @return the resulting JSON response
     * @throws FoundationException if an error occurs
     */
    public static String getProductInfoForInvoice(HttpServletRequest request, HttpServletResponse response)  throws FoundationException {
        InputProviderInterface provider = new HttpInputProvider(request);
        JsonResponse json = new JsonResponse(response);
        ProductInfoForInvoiceLookupService service = new ProductInfoForInvoiceLookupService(provider);
        service.findProductInfoForInvoice();
        return json.makeLookupResponse(ProductInfoForInvoiceLookupConfiguration.INOUT_PRODUCT_ID, service, request.getSession(true).getServletContext());
    }

    public static class ProductInfoForInvoice extends Entity {
        private String productId;
        private String description;
        private String unitPrice;
        private String invoiceItemTypeId;
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getUnitPrice() { return unitPrice; }
        public void setUnitPrice(String unitPrice) { this.unitPrice = unitPrice; }
        public String getInvoiceItemTypeId() { return invoiceItemTypeId; }
        public void setInvoiceItemTypeId(String invoiceItemTypeId) { this.invoiceItemTypeId = invoiceItemTypeId; }
        @Override public Map<String, Object> toMap() {
            Map<String, Object> res = new HashMap<String, Object>();
            res.put(ProductInfoForInvoiceLookupConfiguration.INOUT_PRODUCT_ID, productId);
            res.put(ProductInfoForInvoiceLookupConfiguration.OUT_DESCRIPTION, description);
            res.put(ProductInfoForInvoiceLookupConfiguration.OUT_UNIT_PRICE, unitPrice);
            res.put(ProductInfoForInvoiceLookupConfiguration.OUT_INVOICE_ITEM_TYPE_ID, invoiceItemTypeId);
            return res;
        }
    }

    /**
     * Finds the product info for the given product id.
     * @return a list of one <code>Map</code> representing the product info
     */
    private List<ProductInfoForInvoice> findProductInfoForInvoice() {
        if (productId == null) {
            Debug.logError("Missing required parameter productId", MODULE);
            return null;
        }
        if (invoiceId == null) {
            Debug.logError("Missing required parameter invoiceId", MODULE);
            return null;
        }

        try {
            InvoiceRepositoryInterface invoiceRepository = getDomainsDirectory().getBillingDomain().getInvoiceRepository();
            Product product = repository.getProductById(productId);
            Invoice invoice = invoiceRepository.getInvoiceById(invoiceId);
            ProductInfoForInvoice info = new ProductInfoForInvoice();
            // if it is purchase invoice, then get unitPrice from lastPrice of SupplierProduct
            if ("PURCHASE_INVOICE".equals(invoice.getInvoiceTypeId())) {
                String quantityStr = getProvider().getParameter(ProductInfoForInvoiceLookupConfiguration.INOUT_QUANTITY);
                if (quantityStr == null) {
                    Debug.logError("Missing required parameter quantity for purchase order item", MODULE);
                    return null;
                }
                BigDecimal quantity = new BigDecimal(quantityStr);
                PurchasingRepositoryInterface purchasingRepository = getDomainsDirectory().getPurchasingDomain().getPurchasingRepository();
                SupplierProduct supplierProduct = purchasingRepository.getSupplierProduct(invoice.getPartyIdFrom(), productId, quantity, invoice.getCurrencyUomId());
                String description = "";
                String supplierProductName = "";
                if (supplierProduct != null) {
                    BigDecimal lastPrice = supplierProduct.getLastPrice();
                    if (lastPrice != null) {
                        // retrieve lastPrice as price
                        info.setUnitPrice(lastPrice.toString());
                        Debug.logInfo("Found lastPrice " + lastPrice.toString() + " for SupplierProduct with party [" + invoice.getPartyIdFrom() + "], product [" + productId + "], quantity [" + quantity + "] and currency [" + invoice.getCurrencyUomId() + "]", MODULE);
                    } else {
                        Debug.logInfo("No lastPrice found for SupplierProduct with party [" + invoice.getPartyIdFrom() + "], product [" + productId + "], quantity [" + quantity + "] and currency [" + invoice.getCurrencyUomId() + "]", MODULE);
                    }
                    description = supplierProduct.getSupplierProductId() + " ";
                    supplierProductName = supplierProduct.getSupplierProductName();
                }
                // retrieve description for product
                if (UtilValidate.isNotEmpty(supplierProductName)) {
                    description += supplierProductName;
                } else {
                    description += product.getProductName();
                }
                info.setDescription(description);
            } else {
                // sales invoice
                String quantityStr = getProvider().getParameter(ProductInfoForInvoiceLookupConfiguration.INOUT_QUANTITY);
                BigDecimal quantity = null;
                if (quantityStr != null) {
                    // if set quantity, then unitPrice from customer/quantity pricing for sales invoices
                    quantity = new BigDecimal(quantityStr);
                    info.setUnitPrice(repository.getUnitPrice(product, quantity, invoice.getCurrencyUomId(), invoice.getPartyId()).toString());
                    Debug.logInfo("Set unitPrice " + info.getUnitPrice() + " for Sale Invoice Item with party [" + invoice.getPartyId() + "], product [" + productId + "], quantity [" + quantity + "] and currency [" + invoice.getCurrencyUomId() + "]", MODULE);
               } else {
                    // else get product listprice as UnitPrice
                    info.setUnitPrice(repository.getUnitPrice(product, invoice.getCurrencyUomId()).toString());
                    Debug.logInfo("Set unitPrice " + info.getUnitPrice() + " for Sale Invoice Item product [" + productId + "], and currency [" + invoice.getCurrencyUomId() + "]", MODULE);
                }
                info.setDescription(product.getProductName());
            }
            info.setInvoiceItemTypeId(invoiceRepository.getInvoiceItemTypeIdForProduct(invoice, product));

            List<ProductInfoForInvoice> results = new ArrayList<ProductInfoForInvoice>(1);
            results.add(info);
            setResultTotalCount(1);
            setResults(results);
            return results;
        } catch (FoundationException e) {
            storeException(e);
            return null;
        }
    }
}
