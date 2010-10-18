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
 * Object representing an Lockbox Batch Item, which is an imported Payment (check) from a Lockbox Batch.
 */
public class LockboxBatchItem extends org.opentaps.base.entities.LockboxBatchItem {

    private LockboxBatch lockboxBatch;
    private List<LockboxBatchItemDetail> lockboxBatchItemDetails;
    private List<LockboxBatchItemDetail> validLockboxBatchItemDetails;
    private BigDecimal amountToApplyTotal;
    private BigDecimal cashDiscountTotal;

    /**
     * Default constructor.
     */
    public LockboxBatchItem() {
        super();
    }

    /**
     * Constructor with a repository.
     * @param repository a <code>LockboxRepositoryInterface</code> value
     */
    public LockboxBatchItem(LockboxRepositoryInterface repository) {
        super();
        initRepository(repository);
    }

    /**
     * Gets the outstanding amount for this lockbox batch item.
     * @return a <code>BigDecimal</code> value
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal getOutstandingAmount() throws RepositoryException {
        return getCheckAmount().subtract(getAmountToApplyTotal());
    }

    /**
     * Gets the total amount applied for this lockbox batch item.
     * @return a <code>BigDecimal</code> value
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal getAmountToApplyTotal() throws RepositoryException {
        if (amountToApplyTotal == null) {
            amountToApplyTotal = BigDecimal.ZERO;
            for (LockboxBatchItemDetail detail : getLockboxBatchItemDetails()) {
                if (detail.canApply()) {
                    amountToApplyTotal = amountToApplyTotal.add(detail.getAmountToApply());
                }
            }
        }
        return amountToApplyTotal;
    }

    /**
     * Gets the total amount of cash discount for this lockbox batch item.
     * @return a <code>BigDecimal</code> value
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal getCashDiscountTotal() throws RepositoryException {
        if (cashDiscountTotal == null) {
            cashDiscountTotal = BigDecimal.ZERO;
            for (LockboxBatchItemDetail detail : getLockboxBatchItemDetails()) {
                if (detail.canApply()) {
                    cashDiscountTotal = cashDiscountTotal.add(detail.getCashDiscount());
                }
            }
        }
        return cashDiscountTotal;
    }

    /**
     * Gets the total amount of cash discount and amount to apply for this lockbox batch item.
     * @return a <code>BigDecimal</code> value
     * @throws RepositoryException if an error occurs
     */
    public BigDecimal getTotal() throws RepositoryException {
        return getCashDiscountTotal().add(getAmountToApplyTotal());
    }

    /**
     * Gets the parent <code>LockboxBatch</code> for this <code>LockboxBatchItem</code>.
     * Returns the order domain object instead of the base entity.
     * @return the <code>LockboxBatch</code>
     * @throws RepositoryException if an error occurs
     */
    @Override
    public LockboxBatch getLockboxBatch() throws RepositoryException {
        if (lockboxBatch == null) {
            lockboxBatch = getRelatedOne(LockboxBatch.class);
        }
        return lockboxBatch;
    }

    /**
     * Gets the list of related <code>LockboxBatchItemDetail</code> for this <code>LockboxBatchItem</code>.
     * Returns the order domain object instead of the base entity.
     * @return the list of <code>LockboxBatchItemDetail</code>
     * @throws RepositoryException if an error occurs
     */
    @Override
    public List<LockboxBatchItemDetail> getLockboxBatchItemDetails() throws RepositoryException {
        if (lockboxBatchItemDetails == null) {
            lockboxBatchItemDetails = getRelated(LockboxBatchItemDetail.class, Arrays.asList(LockboxBatchItemDetail.Fields.detailSeqId.asc()));
        }
        return lockboxBatchItemDetails;
    }

    /**
     * Gets the list of related <code>LockboxBatchItemDetail</code> than can be applied for this <code>LockboxBatchItem</code>.
     * Returns the order domain object instead of the base entity.
     * @return the list of <code>LockboxBatchItemDetail</code>
     * @throws RepositoryException if an error occurs
     */
    public List<LockboxBatchItemDetail> getValidLockboxBatchItemDetails() throws RepositoryException {
        if (validLockboxBatchItemDetails == null) {
            validLockboxBatchItemDetails = new ArrayList<LockboxBatchItemDetail>();
            for (LockboxBatchItemDetail detail : getLockboxBatchItemDetails()) {
                if (detail.canApply()) {
                    validLockboxBatchItemDetails.add(detail);
                }
            }
        }
        return validLockboxBatchItemDetails;
    }

    /**
     * Checks if this line has errors.
     * @return a <code>Boolean</code> value
     * @throws RepositoryException if an error occurs
     */
    public Boolean hasError() throws RepositoryException {
        for (LockboxBatchItemDetail detail : getLockboxBatchItemDetails()) {
            if (detail.getStatus().isError()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if this line is balanced, meaning that the total of its applicable details equals the check amount.
     * @return a <code>Boolean</code> value
     * @exception RepositoryException if an error occurs
     */
    public Boolean isBalanced() throws RepositoryException {
        return getCheckAmount().compareTo(getAmountToApplyTotal()) == 0;
    }

    /**
     * Checks if this line is ready to be applied.
     * This only check that the line is balanced.
     * @return a <code>Boolean</code> value
     * @exception RepositoryException if an error occurs
     */
    public Boolean isReady() throws RepositoryException {
        return isBalanced();
    }

    /**
     * Checks if this line is already applied.
     * The line is considered applied if one of its detail line is applied.
     * @return a <code>Boolean</code> value
     * @exception RepositoryException if an error occurs
     */
    public Boolean isApplied() throws RepositoryException {
        for (LockboxBatchItemDetail detail : getLockboxBatchItemDetails()) {
            if (detail.isApplied()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the total amount that would be applied to the given invoice number.
     * @param invoiceNumber an invoice number, that may or may not be in the system
     * @return the total amount applied through all the <code>LockboxBatchItemDetail</code>
     * @exception RepositoryException if an error occurs
     */
    public BigDecimal getTotalAppliedToInvoice(String invoiceNumber) throws RepositoryException {
        BigDecimal result = BigDecimal.ZERO;
        for (LockboxBatchItemDetail detail : getLockboxBatchItemDetails()) {
            if (invoiceNumber.equals(detail.getInvoiceNumber())) {
                result = result.add(detail.getTotal());
            }
        }
        return result;
    }

    private LockboxRepositoryInterface getRepository() {
        return LockboxRepositoryInterface.class.cast(repository);
    }
}
