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
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import org.opentaps.foundation.repository.RepositoryException;

/**
 * Object representing an Lockbox Batch.
 */
public class LockboxBatch extends org.opentaps.base.entities.LockboxBatch {

    private List<LockboxBatchItem> lockboxBatchItems;
    private List<LockboxBatchItem> readyLockboxBatchItems;

    /**
     * Default constructor.
     */
    public LockboxBatch() {
        super();
    }

    /**
     * Constructor with a repository.
     * @param repository a <code>LockboxRepositoryInterface</code> value
     */
    public LockboxBatch(LockboxRepositoryInterface repository) {
        super();
        initRepository(repository);
    }

    /**
     * Gets the list of related <code>LockboxBatchItem</code> for this <code>LockboxBatch</code>.
     * Returns the order domain object instead of the base entity.
     * @return the list of <code>LockboxBatchItem</code>
     * @throws RepositoryException if an error occurs
     */
    @Override
    public List<LockboxBatchItem> getLockboxBatchItems() throws RepositoryException {
        if (lockboxBatchItems == null) {
            lockboxBatchItems = getRelated(LockboxBatchItem.class, Arrays.asList(LockboxBatchItem.Fields.itemSeqId.asc()));
        }
        return lockboxBatchItems;
    }

    /**
     * Gets the list of <code>LockboxBatchItem</code> that are ready and have not been applied.
     * @return the list of <code>LockboxBatchItem</code> that are ready and have not been applied
     * @throws RepositoryException if an error occurs
     */
    public List<LockboxBatchItem> getLockboxBatchItemsReadyToApply() throws RepositoryException {
        if (readyLockboxBatchItems == null) {
            readyLockboxBatchItems = new ArrayList<LockboxBatchItem>();
            for (LockboxBatchItem item : getLockboxBatchItems()) {
                if (item.isReady() && !item.isApplied()) {
                    readyLockboxBatchItems.add(item);
                }
            }
        }
        return readyLockboxBatchItems;
    }

    /**
     * Gets the total amount that would be applied to the given invoice number, this does not account for already applied payments.
     * @param invoiceNumber an invoice number, that may or may not be in the system
     * @return the total amount applied through all the <code>LockboxBatchItemDetail</code>
     * @exception RepositoryException if an error occurs
     */
    public BigDecimal getTotalAppliedToInvoice(String invoiceNumber) throws RepositoryException {
        BigDecimal result = BigDecimal.ZERO;
        for (LockboxBatchItem item : getLockboxBatchItems()) {
            if (!item.isApplied()) {
                result = result.add(item.getTotalAppliedToInvoice(invoiceNumber));
            }
        }
        return result;
    }
}
