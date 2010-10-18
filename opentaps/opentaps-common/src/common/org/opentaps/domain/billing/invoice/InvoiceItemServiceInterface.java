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
package org.opentaps.domain.billing.invoice;

import java.math.BigDecimal;

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
     * @param invoiceId input parameter
     */
    public void setInvoiceId(String invoiceId);

    /**
     * Sets the required input parameter for service.
     * @param invoiceItemTypeId input parameter
     */
    public void setInvoiceItemTypeId(String invoiceItemTypeId);

    /**
     * Sets the required input parameter for service.
     * @param overrideGlAccountId input parameter
     */
    public void setOverrideGlAccountId(String overrideGlAccountId);

    /**
     * Sets the required input parameter for service.
     * @param inventoryItemId input parameter
     */
    public void setInventoryItemId(String inventoryItemId);

    /**
     * Sets the required input parameter for service.
     * @param productId input parameter
     */
    public void setProductId(String productId);

    /**
     * Sets the required input parameter for service.
     * @param productFeatureId input parameter
     */
    public void setProductFeatureId(String productFeatureId);

    /**
     * Sets the required input parameter for service.
     * @param parentInvoiceId input parameter
     */
    public void setParentInvoiceId(String parentInvoiceId);

    /**
     * Sets the required input parameter for service.
     * @param parentInvoiceItemSeqId input parameter
     */
    public void setParentInvoiceItemSeqId(String parentInvoiceItemSeqId);

    /**
     * Sets the required input parameter for service.
     * @param uomId input parameter
     */
    public void setUomId(String uomId);

    /**
     * Sets the required input parameter for service.
     * @param taxableFlag input parameter
     */
    public void setTaxableFlag(String taxableFlag);

    /**
     * Sets the required input parameter for service.
     * @param quantity input parameter
     */
    public void setQuantity(BigDecimal quantity);

    /**
     * Sets the required input parameter for service.
     * @param amount input parameter
     */
    public void setAmount(BigDecimal amount);

    /**
     * Sets the required input parameter for service.
     * @param description input parameter
     */
    public void setDescription(String description);

    /**
     * Sets the required input parameter for service.
     * @param taxAuthPartyId input parameter
     */
    public void setTaxAuthPartyId(String taxAuthPartyId);

    /**
     * Sets the required input parameter for service.
     * @param taxAuthGeoId input parameter
     */
    public void setTaxAuthGeoId(String taxAuthGeoId);

    /**
     * Sets the required input parameter for service.
     * @param taxAuthorityRateSeqId input parameter
     */
    public void setTaxAuthorityRateSeqId(String taxAuthorityRateSeqId);

    /**
     * Sets the required input parameter for service.
     * @param acctgTagEnumId1 input parameter
     */
    public void setAcctgTagEnumId1(String acctgTagEnumId1);

    /**
     * Sets the required input parameter for service.
     * @param acctgTagEnumId2 input parameter
     */
    public void setAcctgTagEnumId2(String acctgTagEnumId2);

    /**
     * Sets the required input parameter for service.
     * @param acctgTagEnumId3 input parameter
     */
    public void setAcctgTagEnumId3(String acctgTagEnumId3);

    /**
     * Sets the required input parameter for service.
     * @param acctgTagEnumId4 input parameter
     */
    public void setAcctgTagEnumId4(String acctgTagEnumId4);

    /**
     * Sets the required input parameter for service.
     * @param acctgTagEnumId5 input parameter
     */
    public void setAcctgTagEnumId5(String acctgTagEnumId5);

    /**
     * Sets the required input parameter for service.
     * @param acctgTagEnumId6 input parameter
     */
    public void setAcctgTagEnumId6(String acctgTagEnumId6);

    /**
     * Sets the required input parameter for service.
     * @param acctgTagEnumId7 input parameter
     */
    public void setAcctgTagEnumId7(String acctgTagEnumId7);

    /**
     * Sets the required input parameter for service.
     * @param acctgTagEnumId8 input parameter
     */
    public void setAcctgTagEnumId8(String acctgTagEnumId8);

    /**
     * Sets the required input parameter for service.
     * @param acctgTagEnumId9 input parameter
     */
    public void setAcctgTagEnumId9(String acctgTagEnumId9);

    /**
     * Sets the required input parameter for service.
     * @param acctgTagEnumId10 input parameter
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
