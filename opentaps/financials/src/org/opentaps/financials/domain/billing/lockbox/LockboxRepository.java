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
package org.opentaps.financials.domain.billing.lockbox;

import java.util.List;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.base.entities.EftAccount;
import org.opentaps.base.entities.PaymentMethodAndEftAccount;
import org.opentaps.domain.billing.invoice.Invoice;
import org.opentaps.domain.billing.invoice.InvoiceRepositoryInterface;
import org.opentaps.domain.billing.lockbox.LockboxBatch;
import org.opentaps.domain.billing.lockbox.LockboxBatchItem;
import org.opentaps.domain.billing.lockbox.LockboxBatchItemDetail;
import org.opentaps.domain.billing.lockbox.LockboxRepositoryInterface;
import org.opentaps.domain.party.Party;
import org.opentaps.domain.party.PartyRepositoryInterface;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.ofbiz.Repository;

/**
 * Repository for Lockbox entities to handle interaction of domain with the entity engine (database) and the service engine.
 */
public class LockboxRepository extends Repository implements LockboxRepositoryInterface {

    private static final String MODULE = LockboxRepository.class.getName();

    private InvoiceRepositoryInterface invoiceRepository;
    private PartyRepositoryInterface partyRepository;

    /**
     * Default constructor.
     */
    public LockboxRepository() {
        super();
    }

    /** {@inheritDoc} */
    public LockboxBatch getBatchById(String lockboxBatchId) throws RepositoryException, EntityNotFoundException {
        return findOneNotNull(LockboxBatch.class, map(LockboxBatch.Fields.lockboxBatchId, lockboxBatchId));
    }

    /** {@inheritDoc} */
    public LockboxBatchItem getBatchItemById(String lockboxBatchId, String itemSeqId) throws RepositoryException, EntityNotFoundException {
        return findOneNotNull(LockboxBatchItem.class, map(LockboxBatchItem.Fields.lockboxBatchId, lockboxBatchId, LockboxBatchItem.Fields.itemSeqId, itemSeqId));
    }

    /** {@inheritDoc} */
    public LockboxBatchItemDetail getBatchItemDetailById(String lockboxBatchId, String itemSeqId, String detailSeqId) throws RepositoryException, EntityNotFoundException {
        return findOneNotNull(LockboxBatchItemDetail.class, map(LockboxBatchItemDetail.Fields.lockboxBatchId, lockboxBatchId, LockboxBatchItemDetail.Fields.itemSeqId, itemSeqId, LockboxBatchItemDetail.Fields.detailSeqId, detailSeqId));
    }

    /** {@inheritDoc} */
    public List<LockboxBatch> getPendingBatches() throws RepositoryException {
        return findList(LockboxBatch.class, EntityCondition.makeCondition(LockboxBatch.Fields.outstandingAmount.getName(), EntityOperator.GREATER_THAN, 0));
    }

    /** {@inheritDoc} */
    public Invoice getRelatedInvoice(LockboxBatchItemDetail detail) throws RepositoryException {
        try {
            return getInvoiceRepository().getInvoiceById(detail.getInvoiceNumber());
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    /** {@inheritDoc} */
    public Party getRelatedCustomer(LockboxBatchItemDetail detail) throws RepositoryException {
        return findOne(Party.class, map(Party.Fields.partyId, detail.getCustomerId()));
    }

    /** {@inheritDoc} */
    public boolean isHashExistent(String hash) throws RepositoryException {
        return UtilValidate.isNotEmpty(findList(LockboxBatch.class, map(LockboxBatch.Fields.fileHashMark, hash)));
    }

    /** {@inheritDoc} */
    public List<LockboxBatch> getBatchesByHash(String hash) throws RepositoryException {
        return findList(LockboxBatch.class, map(LockboxBatch.Fields.fileHashMark, hash));
    }

    /** {@inheritDoc} */
    public PaymentMethodAndEftAccount getPaymentMethod(String accountNumber, String routingNumber) throws RepositoryException {
        EntityCondition conditions = EntityCondition.makeCondition(EntityOperator.AND,
                                       EntityCondition.makeCondition(PaymentMethodAndEftAccount.Fields.routingNumber.name(), EntityOperator.EQUALS, routingNumber),
                                       EntityUtil.getFilterByDateExpr());
        List<PaymentMethodAndEftAccount> results = findList(PaymentMethodAndEftAccount.class, conditions);
        if (results.size() > 1) {
            Debug.logWarning("Found more than one active PaymentMethodAndEftAccount for given account number and routing number [" + routingNumber + "]", MODULE);
        }

        if (results.isEmpty()) {
            Debug.logWarning("Did not find any PaymentMethodAndEftAccount", MODULE);
            return null;
        } else {
            for (PaymentMethodAndEftAccount eft : results) {
                // TODO: #921 Encrypted fields are not available in view entities
                EftAccount eftAcc = eft.getEftAccount();
                if (accountNumber.equals(eftAcc.getAccountNumber())) {
                    return eft;
                }
            }
            Debug.logWarning("No matching EFT account for the given account number.", MODULE);
            return null;
        }
    }

    protected InvoiceRepositoryInterface getInvoiceRepository() throws RepositoryException {
        if (invoiceRepository == null) {
            invoiceRepository = DomainsDirectory.getDomainsDirectory(this).getBillingDomain().getInvoiceRepository();
        }
        return invoiceRepository;
    }

    protected PartyRepositoryInterface getPartyRepository() throws RepositoryException {
        if (partyRepository == null) {
            partyRepository = DomainsDirectory.getDomainsDirectory(this).getPartyDomain().getPartyRepository();
        }
        return partyRepository;
    }

}
