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

package org.opentaps.domain.billing.lockbox;

import java.math.BigDecimal;
import java.nio.ByteBuffer;

import org.opentaps.foundation.service.ServiceException;

/**
 * Lockbox POJO service to import Lockbox files into opentaps.
 */
public interface LockboxServiceInterface {

    /**
     * Sets the uploaded file data, required parameter for {@link #uploadLockboxFile}.
     * @param uploadedFile a <code>ByteBuffer</code> value
     */
    public void setUploadedFile(ByteBuffer uploadedFile);

    /**
     * Sets the file name, required parameter for {@link #uploadLockboxFile}.
     * @param fileName a <code>String</code> value
     */
    public void set_uploadedFile_fileName(String fileName);

    /**
     * Sets the file content type, required parameter for {@link #uploadLockboxFile}.
     * @param contentType a <code>String</code> value
     */
    public void set_uploadedFile_contentType(String contentType);

    /**
     * Uploads a Lockbox file into Opentaps.
     * @throws ServiceException if an error occurs
     * @see #setOrganizationPartyId required input parameter <code>organizationId</code>
     * @see #set_uploadedFile_fileName required input parameter <code>fileName</code>
     * @see #set_uploadedFile_contentType required input parameter <code>contentType</code>
     */
    public void uploadLockboxFile() throws ServiceException;

    /**
     * Sets the lockbox batch id, required parameter for {@link #addLockboxBatchItemDetail} and {@link #updateLockboxBatchItemDetail}.
     * @param lockboxBatchId a <code>String</code> value
     */
    public void setLockboxBatchId(String lockboxBatchId);

    /**
     * Sets the lockbox item sequence id, required parameter for {@link #addLockboxBatchItemDetail} and {@link #updateLockboxBatchItemDetail}.
     * @param itemSeqId a <code>String</code> value
     */
    public void setItemSeqId(String itemSeqId);

    /**
     * Sets the lockbox item detail sequence id, required parameter for {@link #updateLockboxBatchItemDetail}.
     * @param detailSeqId a <code>String</code> value
     */
    public void setDetailSeqId(String detailSeqId);

    /**
     * Sets the organization party id for which the payments will be applied, required parameter for {@link #processLockboxBatch} and {@link #uploadLockboxFile}.
     * @param organizationPartyId a <code>String</code> value
     */
    public void setOrganizationPartyId(String organizationPartyId);

    /**
     * Sets the lockbox item detail customer id, optional parameter for {@link #addLockboxBatchItemDetail}.
     * If no party id is given then the service will expect an invoice id.
     * @param partyId a <code>String</code> value
     */
    public void setPartyId(String partyId);

    /**
     * Sets the lockbox item detail invoice id, optional parameter for {@link #addLockboxBatchItemDetail}.
     * If no invoice id is given then the service will expect a party id.
     * @param invoiceId a <code>String</code> value
     */
    public void setInvoiceId(String invoiceId);

    /**
     * Sets the lockbox item detail amount to apply, required parameter for {@link #addLockboxBatchItemDetail} and {@link #updateLockboxBatchItemDetail}.
     * @param amountToApply a <code>BigDecimal</code> value
     */
    public void setAmountToApply(BigDecimal amountToApply);

    /**
     * Sets the lockbox item detail cash discount, required parameter for {@link #addLockboxBatchItemDetail} and {@link #updateLockboxBatchItemDetail}.
     * @param cashDiscount a <code>BigDecimal</code> value
     */
    public void setCashDiscount(BigDecimal cashDiscount);

    /**
     * Routes the action to either create a new <code>LockboxBatchItemDetail</code> or update an existing <code>LockboxBatchItemDetail</code>.
     * @throws ServiceException if an error occurs
     * @see #addLockboxBatchItemDetail
     * @see #updateLockboxBatchItemDetail
     */
    public void lockboxBatchItemDetailAction() throws ServiceException;

    /**
     * Adds a <code>LockboxBatchItemDetail</code> to an existing <code>LockboxBatchItem</code>.
     * @throws ServiceException if an error occurs
     * @see #setLockboxBatchId required input <code>lockboxBatchId</code>
     * @see #setItemSeqId required input <code>itemSeqId</code>
     * @see #setAmountToApply required input <code>amountToApply</code>
     * @see #setCashDiscount required input <code>cashDiscount</code>
     * @see #setPartyId optional input <code>partyId</code>
     * @see #setInvoiceId optional input <code>invoiceId</code>
     */
    public void addLockboxBatchItemDetail() throws ServiceException;

    /**
     * Update a <code>LockboxBatchItemDetail</code> amount to apply and cash discount, and if it was user entered allows to update the line amount.
     * @throws ServiceException if an error occurs
     * @see #setLockboxBatchId required input <code>lockboxBatchId</code>
     * @see #setItemSeqId required input <code>itemSeqId</code>
     * @see #setDetailSeqId required input <code>detailSeqId</code>
     * @see #setAmountToApply required input <code>amountToApply</code>
     * @see #setCashDiscount required input <code>cashDiscount</code>
     */
    public void updateLockboxBatchItemDetail() throws ServiceException;

    /**
     * Validate and process all the ready lines from a <code>LockboxBatch</code>.
     * @throws ServiceException if an error occurs
     * @see #setLockboxBatchId required input parameter <code>lockboxBatchId</code>
     * @see #setOrganizationPartyId required input parameter <code>organizationId</code>
     */
    public void processLockboxBatch() throws ServiceException;
}
