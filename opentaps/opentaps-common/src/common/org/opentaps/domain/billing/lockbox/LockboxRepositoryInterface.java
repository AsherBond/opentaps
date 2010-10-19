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

import java.util.List;

import org.opentaps.base.entities.PaymentMethodAndEftAccount;
import org.opentaps.domain.billing.invoice.Invoice;
import org.opentaps.domain.party.Party;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.RepositoryInterface;


/**
 * Repository for Lockbox entities to handle interaction of domain with the entity engine (database) and the service engine.
 */
public interface LockboxRepositoryInterface extends RepositoryInterface {

    /**
     * Finds a <code>LockboxBatch</code> by ID from the database.
     * @param lockboxBatchId the batch ID
     * @return the <code>LockboxBatch</code>
     * @throws RepositoryException if an error occurs
     * @throws EntityNotFoundException no <code>LockboxBatch</code> is found for the given id
     */
    public LockboxBatch getBatchById(String lockboxBatchId) throws RepositoryException, EntityNotFoundException;

    /**
     * Finds a <code>LockboxBatchItem</code> by ID from the database.
     * @param lockboxBatchId the batch ID
     * @param itemSeqId the batch item sequence ID
     * @return the <code>LockboxBatchItem</code>
     * @throws RepositoryException if an error occurs
     * @throws EntityNotFoundException no <code>LockboxBatchItem</code> is found for the given id
     */
    public LockboxBatchItem getBatchItemById(String lockboxBatchId, String itemSeqId) throws RepositoryException, EntityNotFoundException;

    /**
     * Finds a <code>LockboxBatchItemDetail</code> by ID from the database.
     * @param lockboxBatchId the batch ID
     * @param itemSeqId the batch item sequence ID
     * @param detailSeqId the batch item detail sequence ID
     * @return the <code>LockboxBatchItem</code>
     * @throws RepositoryException if an error occurs
     * @throws EntityNotFoundException no <code>LockboxBatchItemDetail</code> is found for the given id
     */
    public LockboxBatchItemDetail getBatchItemDetailById(String lockboxBatchId, String itemSeqId, String detailSeqId) throws RepositoryException, EntityNotFoundException;

    /**
     * Finds the list of <code>LockboxBatch</code> for which the pending amount to apply is not zero.
     * @return list of pending <code>LockboxBatch</code>
     * @throws RepositoryException if an error occurs
     */
    public List<LockboxBatch> getPendingBatches() throws RepositoryException;

    /**
     * Finds the <code>Invoice</code> related to this <code>LockboxBatchItemDetail</code>, if the invoice number match one in the system.
     * @param detail a <code>LockboxBatchItemDetail</code> value
     * @return an <code>Invoice</code> value, or <code>null</code> if none is found matching the invoice number
     * @throws RepositoryException if an error occurs
     */
    public Invoice getRelatedInvoice(LockboxBatchItemDetail detail) throws RepositoryException;

    /**
     * Finds the <code>Party</code> related to this <code>LockboxBatchItemDetail</code>, if the customer number match one in the system.
     * @param detail a <code>LockboxBatchItemDetail</code> value
     * @return a <code>Party</code> value, or <code>null</code> if none is found matching the customer number
     * @throws RepositoryException if an error occurs
     */
    public Party getRelatedCustomer(LockboxBatchItemDetail detail) throws RepositoryException;

    /**
     * Verify if some has code exists in <code>LockboxBatch</code> entity.
     * @param hash SHA hash (digest)
     * @return <code>true</code> if the given hash exists in another <code>LockboxBatch</code>. Used when testing for duplicates.
     * @throws RepositoryException if an error occurs
     */
    public boolean isHashExistent(String hash) throws RepositoryException;

    /**
     * Finds the list of <code>LockboxBatch</code> for which the fileHashMark equals to hash.
     * @param hash SHA hash (digest)
     * @return list of <code>LockboxBatch</code>
     * @throws RepositoryException if an error occurs
     */
    public List<LockboxBatch> getBatchesByHash(String hash) throws RepositoryException;

    /**
     * Gets a <code>PaymentMethodAndEftAccount</code> for the given account and routing number.
     * @param routingNumber a <code>String</code> value
     * @param accountNumber a <code>String</code> value
     * @return a <code>PaymentMethodAndEftAccount</code> value
     * @exception RepositoryException if an error occurs
     */
    public PaymentMethodAndEftAccount getPaymentMethod(String accountNumber, String routingNumber) throws RepositoryException;
}
