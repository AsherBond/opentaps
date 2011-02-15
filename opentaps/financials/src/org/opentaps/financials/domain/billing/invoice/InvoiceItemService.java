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
package org.opentaps.financials.domain.billing.invoice;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilObject;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericEntityException;
import org.opentaps.domain.DomainService;
import org.opentaps.base.entities.InvoiceItem;
import org.opentaps.base.entities.SupplierProduct;
import org.opentaps.domain.billing.invoice.Invoice;
import org.opentaps.domain.billing.invoice.InvoiceItemServiceInterface;
import org.opentaps.domain.billing.invoice.InvoiceRepositoryInterface;
import org.opentaps.domain.organization.AccountingTagConfigurationForOrganizationAndUsage;
import org.opentaps.domain.product.Product;
import org.opentaps.domain.product.ProductRepositoryInterface;
import org.opentaps.domain.purchasing.PurchasingRepositoryInterface;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;

/**
 * POJO implementation of services which create/update invoice item using the
 * opentaps Service foundation class.
 */
public class InvoiceItemService extends DomainService implements InvoiceItemServiceInterface {

    private static final String MODULE = InvoiceItemService.class.getName();
    private static int INVOICE_ITEM_PADDING = 4;
    private String invoiceId;
    private Boolean validateAccountingTags = false;

    private String invoiceItemSeqId;
    private String invoiceItemTypeId;
    private String overrideGlAccountId;
    private String inventoryItemId;
    private String productId;
    private String productFeatureId;
    private String parentInvoiceId;
    private String parentInvoiceItemSeqId;
    private String uomId;
    private String taxableFlag;
    private BigDecimal quantity;
    private BigDecimal amount;
    private String description;
    private String taxAuthPartyId;
    private String taxAuthGeoId;
    private String taxAuthorityRateSeqId;
    private String acctgTagEnumId1;
    private String acctgTagEnumId2;
    private String acctgTagEnumId3;
    private String acctgTagEnumId4;
    private String acctgTagEnumId5;
    private String acctgTagEnumId6;
    private String acctgTagEnumId7;
    private String acctgTagEnumId8;
    private String acctgTagEnumId9;
    private String acctgTagEnumId10;

    private List<String> parametersAlreadySet = new ArrayList<String>();

    /**
     * Default constructor.
     */
    public InvoiceItemService() {
        super();
    }

    /** {@inheritDoc} */
    public String getInvoiceItemSeqId() {
        return invoiceItemSeqId;
    }

    /** {@inheritDoc} */
    public void setValidateAccountingTags(Boolean validateAccountingTags) {
        this.validateAccountingTags = validateAccountingTags;
    }

    /** {@inheritDoc} */
    public void setAcctgTagEnumId1(String acctgTagEnumId1) {
        this.acctgTagEnumId1 = acctgTagEnumId1;
        parametersAlreadySet.add("acctgTagEnumId1");
    }

    /** {@inheritDoc} */
    public void setAcctgTagEnumId10(String acctgTagEnumId10) {
        this.acctgTagEnumId10 = acctgTagEnumId10;
        parametersAlreadySet.add("acctgTagEnumId10");
    }

    /** {@inheritDoc} */
    public void setAcctgTagEnumId2(String acctgTagEnumId2) {
        this.acctgTagEnumId2 = acctgTagEnumId2;
        parametersAlreadySet.add("acctgTagEnumId2");
    }

    /** {@inheritDoc} */
    public void setAcctgTagEnumId3(String acctgTagEnumId3) {
        this.acctgTagEnumId3 = acctgTagEnumId3;
        parametersAlreadySet.add("acctgTagEnumId3");
    }

    /** {@inheritDoc} */
    public void setAcctgTagEnumId4(String acctgTagEnumId4) {
        this.acctgTagEnumId4 = acctgTagEnumId4;
        parametersAlreadySet.add("acctgTagEnumId4");
    }

    /** {@inheritDoc} */
    public void setAcctgTagEnumId5(String acctgTagEnumId5) {
        this.acctgTagEnumId5 = acctgTagEnumId5;
        parametersAlreadySet.add("acctgTagEnumId5");
    }

    /** {@inheritDoc} */
    public void setAcctgTagEnumId6(String acctgTagEnumId6) {
        this.acctgTagEnumId6 = acctgTagEnumId6;
        parametersAlreadySet.add("acctgTagEnumId6");
    }

    /** {@inheritDoc} */
    public void setAcctgTagEnumId7(String acctgTagEnumId7) {
        this.acctgTagEnumId7 = acctgTagEnumId7;
        parametersAlreadySet.add("acctgTagEnumId7");
    }

    /** {@inheritDoc} */
    public void setAcctgTagEnumId8(String acctgTagEnumId8) {
        this.acctgTagEnumId8 = acctgTagEnumId8;
        parametersAlreadySet.add("acctgTagEnumId8");
    }

    /** {@inheritDoc} */
    public void setAcctgTagEnumId9(String acctgTagEnumId9) {
        this.acctgTagEnumId9 = acctgTagEnumId9;
        parametersAlreadySet.add("acctgTagEnumId9");
    }

    /** {@inheritDoc} */
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
        parametersAlreadySet.add("amount");
    }

    /** {@inheritDoc} */
    public void setDescription(String description) {
        this.description = description;
        parametersAlreadySet.add("description");
    }

    /** {@inheritDoc} */
    public void setInventoryItemId(String inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
        parametersAlreadySet.add("inventoryItemId");
    }

    /** {@inheritDoc} */
    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
        parametersAlreadySet.add("invoiceId");
    }

    /** {@inheritDoc} */
    public void setInvoiceItemSeqId(String invoiceItemSeqId) {
        this.invoiceItemSeqId = invoiceItemSeqId;
        parametersAlreadySet.add("invoiceItemSeqId");
    }

    /** {@inheritDoc} */
    public void setInvoiceItemTypeId(String invoiceItemTypeId) {
        this.invoiceItemTypeId = invoiceItemTypeId;
        parametersAlreadySet.add("invoiceItemTypeId");
    }

    /** {@inheritDoc} */
    public void setOverrideGlAccountId(String overrideGlAccountId) {
        this.overrideGlAccountId = overrideGlAccountId;
        parametersAlreadySet.add("overrideGlAccountId");
    }

    /** {@inheritDoc} */
    public void setParentInvoiceId(String parentInvoiceId) {
        this.parentInvoiceId = parentInvoiceId;
        parametersAlreadySet.add("parentInvoiceId");
    }

    /** {@inheritDoc} */
    public void setParentInvoiceItemSeqId(String parentInvoiceItemSeqId) {
        this.parentInvoiceItemSeqId = parentInvoiceItemSeqId;
        parametersAlreadySet.add("parentInvoiceItemSeqId");
    }

    /** {@inheritDoc} */
    public void setProductFeatureId(String productFeatureId) {
        this.productFeatureId = productFeatureId;
        parametersAlreadySet.add("productFeatureId");
    }

    /** {@inheritDoc} */
    public void setProductId(String productId) {
        this.productId = productId;
        parametersAlreadySet.add("productId");
    }

    /** {@inheritDoc} */
    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
        parametersAlreadySet.add("quantity");
    }

    /** {@inheritDoc} */
    public void setTaxAuthGeoId(String taxAuthGeoId) {
        this.taxAuthGeoId = taxAuthGeoId;
        parametersAlreadySet.add("taxAuthGeoId");
    }

    /** {@inheritDoc} */
    public void setTaxAuthPartyId(String taxAuthPartyId) {
        this.taxAuthPartyId = taxAuthPartyId;
        parametersAlreadySet.add("taxAuthPartyId");
    }

    /** {@inheritDoc} */
    public void setTaxAuthorityRateSeqId(String taxAuthorityRateSeqId) {
        this.taxAuthorityRateSeqId = taxAuthorityRateSeqId;
        parametersAlreadySet.add("taxAuthorityRateSeqId");
    }

    /** {@inheritDoc} */
    public void setTaxableFlag(String taxableFlag) {
        this.taxableFlag = taxableFlag;
        parametersAlreadySet.add("taxableFlag");
    }

    /** {@inheritDoc} */
    public void setUomId(String uomId) {
        this.uomId = uomId;
        parametersAlreadySet.add("uomId");
    }

    /** {@inheritDoc} */
    public void createInvoiceItem() throws ServiceException {
        try {
            if (productId != null) {
                setInvoiceItemDefaultProperties();
            }
            // create InvoiceItem by hibernate
            InvoiceRepositoryInterface invoiceRepository = getDomainsDirectory().getBillingDomain().getInvoiceRepository();
            Invoice invoice = invoiceRepository.getInvoiceById(invoiceId);
            InvoiceItem invoiceItem = new InvoiceItem();
            invoiceItem.initRepository(invoiceRepository);
            // write parameters of service to invoiceItem object
            setParametersToInvoiceItem(invoiceItem);
            // validate tags parameters if necessary
            if (validateAccountingTags) {
                List<AccountingTagConfigurationForOrganizationAndUsage> missings = invoiceRepository.validateTagParameters(invoice, invoiceItem);
                if (!missings.isEmpty()) {
                    throw new ServiceException("OpentapsError_ServiceErrorRequiredTagNotFound", UtilMisc.toMap("tagName", missings.get(0).getDescription()));
                }
            }
            // if invoiceItemSeqId is null, then get next seq for it.
            if (invoiceItem.getInvoiceItemSeqId() == null) {
                invoiceItem.setNextSubSeqId(InvoiceItem.Fields.invoiceItemSeqId.name(), INVOICE_ITEM_PADDING);
            }
            invoiceRepository.createOrUpdate(invoiceItem);
            Debug.logInfo("create InvoiceItem InvoiceId : [" + invoiceItem.getInvoiceId() + "], invoiceItemSeqId : [" + invoiceItem.getInvoiceItemSeqId() + "]", MODULE);
            setInvoiceItemSeqId(invoiceItem.getInvoiceItemSeqId());
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            Debug.logError(e, MODULE);
            throw new ServiceException(e);
        }
    }

    /** {@inheritDoc} */
    public void updateInvoiceItem() throws ServiceException {
        // Call the updateInvoiceItem service
        // search the InvoiceItem by hibernate
        try {
            InvoiceRepositoryInterface invoiceRepository = getDomainsDirectory().getBillingDomain().getInvoiceRepository();
            Invoice invoice = invoiceRepository.getInvoiceById(invoiceId);
            InvoiceItem invoiceItem = invoiceRepository.getInvoiceItemById(invoiceId, invoiceItemSeqId);
            // check if the productNumber is updated, when yes retrieve product
            // description and price
            if (!UtilObject.equalsHelper(invoiceItem.getProductId(), productId)) {
                if (productId != null) {
                    setInvoiceItemDefaultProperties();
                }
            }
            // write parameters of service to invoiceItem object
            setParametersToInvoiceItem(invoiceItem);
            // validate tags parameters if necessary
            if (validateAccountingTags) {
                List<AccountingTagConfigurationForOrganizationAndUsage> missings = invoiceRepository.validateTagParameters(invoice, invoiceItem);
                if (!missings.isEmpty()) {
                    throw new ServiceException("OpentapsError_ServiceErrorRequiredTagNotFound", UtilMisc.toMap("tagName", missings.get(0).getDescription()));
                }
            }
            invoiceRepository.update(invoiceItem);
            Debug.logInfo("update InvoiceItem InvoiceId : [" + invoiceItem.getInvoiceId() + "], invoiceItemSeqId : [" + invoiceItem.getInvoiceItemSeqId() + "]", MODULE);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            Debug.logError(e, MODULE);
            throw new ServiceException(e);
        }
    }

    /**
     * Set parameters of service to <code>InvoiceItem</code>.
     * @param invoiceItem a <code>InvoiceItem</code> value
     */
    private void setParametersToInvoiceItem(InvoiceItem invoiceItem) {
        // write the field to invoiceItem when it changed
        if (parametersAlreadySet.contains("acctgTagEnumId1")) {
            invoiceItem.setAcctgTagEnumId1(acctgTagEnumId1);
        }
        if (parametersAlreadySet.contains("acctgTagEnumId2")) {
            invoiceItem.setAcctgTagEnumId2(acctgTagEnumId2);
        }
        if (parametersAlreadySet.contains("acctgTagEnumId3")) {
            invoiceItem.setAcctgTagEnumId3(acctgTagEnumId3);
        }
        if (parametersAlreadySet.contains("acctgTagEnumId4")) {
            invoiceItem.setAcctgTagEnumId4(acctgTagEnumId4);
        }
        if (parametersAlreadySet.contains("acctgTagEnumId5")) {
            invoiceItem.setAcctgTagEnumId5(acctgTagEnumId5);
        }
        if (parametersAlreadySet.contains("acctgTagEnumId6")) {
            invoiceItem.setAcctgTagEnumId6(acctgTagEnumId6);
        }
        if (parametersAlreadySet.contains("acctgTagEnumId7")) {
            invoiceItem.setAcctgTagEnumId7(acctgTagEnumId7);
        }
        if (parametersAlreadySet.contains("acctgTagEnumId8")) {
            invoiceItem.setAcctgTagEnumId8(acctgTagEnumId8);
        }
        if (parametersAlreadySet.contains("acctgTagEnumId9")) {
            invoiceItem.setAcctgTagEnumId9(acctgTagEnumId9);
        }
        if (parametersAlreadySet.contains("acctgTagEnumId10")) {
            invoiceItem.setAcctgTagEnumId10(acctgTagEnumId10);
        }
        if (parametersAlreadySet.contains("amount")) {
            invoiceItem.setAmount(invoiceItem.convertToBigDecimal(amount));
        }
        if (parametersAlreadySet.contains("quantity")) {
            invoiceItem.setQuantity(invoiceItem.convertToBigDecimal(quantity));
        }
        if (parametersAlreadySet.contains("description")) {
            invoiceItem.setDescription(description);
        }
        if (parametersAlreadySet.contains("inventoryItemId")) {
            invoiceItem.setInventoryItemId(inventoryItemId);
        }
        if (parametersAlreadySet.contains("invoiceId")) {
            invoiceItem.setInvoiceId(invoiceId);
        }
        if (parametersAlreadySet.contains("invoiceItemSeqId")) {
            invoiceItem.setInvoiceItemSeqId(invoiceItemSeqId);
        }
        if (parametersAlreadySet.contains("invoiceItemTypeId")) {
            invoiceItem.setInvoiceItemTypeId(invoiceItemTypeId);
        }
        if (parametersAlreadySet.contains("overrideGlAccountId")) {
            invoiceItem.setOverrideGlAccountId(overrideGlAccountId);
        }
        if (parametersAlreadySet.contains("parentInvoiceId")) {
            invoiceItem.setParentInvoiceId(parentInvoiceId);
        }
        if (parametersAlreadySet.contains("parentInvoiceItemSeqId")) {
            invoiceItem.setParentInvoiceItemSeqId(parentInvoiceItemSeqId);
        }
        if (parametersAlreadySet.contains("productFeatureId")) {
            invoiceItem.setProductFeatureId(productFeatureId);
        }
        if (parametersAlreadySet.contains("productId")) {
            invoiceItem.setProductId(productId);
        }
        if (parametersAlreadySet.contains("taxAuthGeoId")) {
            invoiceItem.setTaxAuthGeoId(taxAuthGeoId);
        }
        if (parametersAlreadySet.contains("taxableFlag")) {
            invoiceItem.setTaxableFlag(taxableFlag);
        }
        if (parametersAlreadySet.contains("taxAuthPartyId")) {
            invoiceItem.setTaxAuthPartyId(taxAuthPartyId);
        }
        if (parametersAlreadySet.contains("taxAuthorityRateSeqId")) {
            invoiceItem.setTaxAuthorityRateSeqId(taxAuthorityRateSeqId);
        }
    }

    /**
     * Set default value to <code>InvoiceItem</code> .
     *
     * @throws RepositoryException if an exception occurs
     * @throws EntityNotFoundException if an exception occurs
     * @throws GenericEntityException if an exception occurs
     * @throws ServiceException if an exception occurs
     */
    private void setInvoiceItemDefaultProperties() throws GenericEntityException, RepositoryException, EntityNotFoundException, ServiceException {
        // set the repositories needed
        InvoiceRepositoryInterface invoiceRepository = getDomainsDirectory().getBillingDomain().getInvoiceRepository();
        ProductRepositoryInterface productRepository = getDomainsDirectory().getProductDomain().getProductRepository();
        PurchasingRepositoryInterface purchasingRepository = getDomainsDirectory().getPurchasingDomain().getPurchasingRepository();
        // if productId is not null, then
        if (productId != null) {
            // load product and invoice domain object
            Product product = productRepository.getProductById(productId);
            Invoice invoice = invoiceRepository.getInvoiceById(invoiceId);

            // If invoiceItemTypeId is null, then use the ProductInvoiceItemType
            // to fill in the invoiceItemTypeId (see below for this entity)
            if (invoiceItemTypeId == null) {
                String newInvoiceItemTypeId = invoiceRepository.getInvoiceItemTypeIdForProduct(invoice, product);
                Debug.logInfo("set new InvoiceItemTypeId [" + newInvoiceItemTypeId + "]", MODULE);
                setInvoiceItemTypeId(newInvoiceItemTypeId);
            }

            if (invoice.isSalesInvoice()) {
                // If description is null, then use Product productName to fill in
                // the description
                if (description == null) {
                    setDescription(product.getProductName());
                }
                // If price is null, then call calculateProductPrice and fill in the
                // default name
                if (amount == null) {
                    // get the price of the product
                    String currencyUomId = uomId;
                    if (currencyUomId == null) {
                        currencyUomId = invoice.getCurrencyUomId();
                    }
                    // set amount
                    if (quantity != null) {
                        BigDecimal price = productRepository.getUnitPrice(product, BigDecimal.valueOf(quantity.doubleValue()), invoice.getCurrencyUomId(), invoice.getPartyId());
                        setAmount(price);
                        Debug.logVerbose("Set unitPrice " + this.amount + " for Sale Invoice Item with party [" + invoice.getPartyId() + "], product [" + productId + "], quantity [" + quantity + "] and currency [" + invoice.getCurrencyUomId() + "]", MODULE);
                    } else {
                        setAmount(productRepository.getUnitPrice(product, invoice.getCurrencyUomId()));
                        Debug.logVerbose("Set unitPrice " + this.amount + " for Sale Invoice Item product [" + productId + "], and currency [" + invoice.getCurrencyUomId() + "]", MODULE);
                    }
                }
            } else if (invoice.isPurchaseInvoice()) {
                // if the type is purchase invoice, then use the getSupplierProduct and set:
                // 1. amount = SupplierProduct.lastPrice
                // 2. description = SupplierProduct.supplierProductId + " " + SuppierProduct.supplierProductName
                SupplierProduct supplierProduct = purchasingRepository.getSupplierProduct(invoice.getPartyIdFrom(), productId, invoice.convertToBigDecimal(quantity), invoice.getCurrencyUomId());
                //if supplierProduct not null
                if (supplierProduct != null) {
                    // If description is null and supplierProductName not null, then use SupplierProduct productName to fill in
                    // the description
                    if (UtilValidate.isEmpty(description)) {
                        if (UtilValidate.isNotEmpty(supplierProduct.getSupplierProductName())) {
                            setDescription(supplierProduct.getSupplierProductId() + " " + supplierProduct.getSupplierProductName());
                        } else {
                            setDescription(product.getProductName());
                        }
                    }
                    // If price is null, then use SupplierProduct lastPrice to fill in
                    // default name
                    if (amount == null) {
                        // set amount
                        setAmount(supplierProduct.getLastPrice());
                    }
                }
            }
        }
    }
}
