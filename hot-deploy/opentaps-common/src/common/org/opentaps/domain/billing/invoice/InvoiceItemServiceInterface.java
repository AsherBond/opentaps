/*
 * Copyright (c) 2008 - 2009 Open Source Strategies, Inc.
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
package org.opentaps.domain.billing.invoice;

import org.opentaps.foundation.service.ServiceException;
import org.opentaps.foundation.service.ServiceInterface;

/**
 * POJO service which creates invoice item using the opentaps Service foundation
 * class.
 */
public interface InvoiceItemServiceInterface extends ServiceInterface {

    /**
     * Gets the invoiceItemSeqId created by the service.
     * @return the invoiceItem Seq ID
     */
    public String getInvoiceItemSeqId();

    /**
     * Sets if the services should validate the accounting tags, defaults to <code>false</code>.
     * @param validateAccountingTags a <code>Boolean</code> value
     */
    public void setValidateAccountingTags(Boolean validateAccountingTags);

    /**
     * Sets the required input parameter for service.
     * @param invoiceItemSeqId the invoiceItem seq Id
     */
    public void setInvoiceItemSeqId(String invoiceItemSeqId);

    /**
     * Sets the required input parameter for service.
     * @param invoiceId input paramater
     */
    public void setInvoiceId(String invoiceId);

    /**
     * Sets the required input parameter for service.
     * @param invoiceItemTypeId input paramater
     */
    public void setInvoiceItemTypeId(String invoiceItemTypeId);

    /**
     * Sets the required input parameter for service.
     * @param overrideGlAccountId input paramater
     */
    public void setOverrideGlAccountId(String overrideGlAccountId);

    /**
     * Sets the required input parameter for service.
     * @param inventoryItemId input paramater
     */
    public void setInventoryItemId(String inventoryItemId);

    /**
     * Sets the required input parameter for service.
     * @param productId input paramater
     */
    public void setProductId(String productId);

    /**
     * Sets the required input parameter for service.
     * @param productFeatureId input paramater
     */
    public void setProductFeatureId(String productFeatureId);

    /**
     * Sets the required input parameter for service.
     * @param parentInvoiceId input paramater
     */
    public void setParentInvoiceId(String parentInvoiceId);

    /**
     * Sets the required input parameter for service.
     * @param parentInvoiceItemSeqId input paramater
     */
    public void setParentInvoiceItemSeqId(String parentInvoiceItemSeqId);

    /**
     * Sets the required input parameter for service.
     * @param uomId input paramater
     */
    public void setUomId(String uomId);

    /**
     * Sets the required input parameter for service.
     * @param taxableFlag input paramater
     */
    public void setTaxableFlag(String taxableFlag);

    /**
     * Sets the required input parameter for service.
     * @param quantity input paramater
     */
    public void setQuantity(Double quantity);

    /**
     * Sets the required input parameter for service.
     * @param amount input paramater
     */
    public void setAmount(Double amount);

    /**
     * Sets the required input parameter for service.
     * @param description input paramater
     */
    public void setDescription(String description);

    /**
     * Sets the required input parameter for service.
     * @param taxAuthPartyId input paramater
     */
    public void setTaxAuthPartyId(String taxAuthPartyId);

    /**
     * Sets the required input parameter for service.
     * @param taxAuthGeoId input paramater
     */
    public void setTaxAuthGeoId(String taxAuthGeoId);

    /**
     * Sets the required input parameter for service.
     * @param taxAuthorityRateSeqId input paramater
     */
    public void setTaxAuthorityRateSeqId(String taxAuthorityRateSeqId);

    /**
     * Sets the required input parameter for service.
     * @param acctgTagEnumId1 input paramater
     */
    public void setAcctgTagEnumId1(String acctgTagEnumId1);

    /**
     * Sets the required input parameter for service.
     * @param acctgTagEnumId2 input paramater
     */
    public void setAcctgTagEnumId2(String acctgTagEnumId2);

    /**
     * Sets the required input parameter for service.
     * @param acctgTagEnumId3 input paramater
     */
    public void setAcctgTagEnumId3(String acctgTagEnumId3);

    /**
     * Sets the required input parameter for service.
     * @param acctgTagEnumId4 input paramater
     */
    public void setAcctgTagEnumId4(String acctgTagEnumId4);

    /**
     * Sets the required input parameter for service.
     * @param acctgTagEnumId5 input paramater
     */
    public void setAcctgTagEnumId5(String acctgTagEnumId5);

    /**
     * Sets the required input parameter for service.
     * @param acctgTagEnumId6 input paramater
     */
    public void setAcctgTagEnumId6(String acctgTagEnumId6);

    /**
     * Sets the required input parameter for service.
     * @param acctgTagEnumId7 input paramater
     */
    public void setAcctgTagEnumId7(String acctgTagEnumId7);

    /**
     * Sets the required input parameter for service.
     * @param acctgTagEnumId8 input paramater
     */
    public void setAcctgTagEnumId8(String acctgTagEnumId8);

    /**
     * Sets the required input parameter for service.
     * @param acctgTagEnumId9 input paramater
     */
    public void setAcctgTagEnumId9(String acctgTagEnumId9);

    /**
     * Sets the required input parameter for service.
     * @param acctgTagEnumId10 input paramater
     */
    public void setAcctgTagEnumId10(String acctgTagEnumId10);

    /**
     * Service to create InvoiceItem.
     * @throws ServiceException if an error occurs
     */
    public void createInvoiceItem() throws ServiceException;

    /**
     * Service to update InvoiceItem.
     * @throws ServiceException if an error occurs
     */
    public void updateInvoiceItem() throws ServiceException;

}
