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

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.opentaps.domain.DomainService;
import org.opentaps.base.entities.PaymentMethodAndEftAccount;
import org.opentaps.base.services.CreatePaymentApplicationService;
import org.opentaps.base.services.FinancialsCreatePaymentService;
import org.opentaps.base.services.SetPaymentStatusService;
import org.opentaps.domain.billing.BillingDomainInterface;
import org.opentaps.domain.billing.invoice.Invoice;
import org.opentaps.domain.billing.invoice.InvoiceRepositoryInterface;
import org.opentaps.domain.billing.invoice.InvoiceServiceInterface;
import org.opentaps.domain.billing.lockbox.LockboxBatch;
import org.opentaps.domain.billing.lockbox.LockboxBatchItem;
import org.opentaps.domain.billing.lockbox.LockboxBatchItemDetail;
import org.opentaps.domain.billing.lockbox.LockboxRepositoryInterface;
import org.opentaps.domain.billing.lockbox.LockboxServiceInterface;
import org.opentaps.domain.ledger.GeneralLedgerAccount;
import org.opentaps.domain.ledger.LedgerRepositoryInterface;
import org.opentaps.domain.party.Party;
import org.opentaps.domain.party.PartyRepositoryInterface;
import org.opentaps.foundation.service.ServiceException;

/** {@inheritDoc} */
public class LockboxService extends DomainService implements LockboxServiceInterface {

    private static final String MODULE = LockboxService.class.getName();

    // for the uploadLockboxFile service
    private String fileName;
    private String contentType;
    private ByteBuffer uploadedFile;
    // for the other services
    private String lockboxBatchId;
    private String itemSeqId;
    private String detailSeqId;
    private String partyId;
    private String invoiceId;
    private BigDecimal amountToApply;
    private BigDecimal cashDiscount;
    private String organizationPartyId;

    /**
     * Default constructor.
     */
    public LockboxService() {
        super();
    }

    /** {@inheritDoc} */
    public void setOrganizationPartyId(String organizationPartyId) {
        this.organizationPartyId = organizationPartyId;
    }

    /** {@inheritDoc} */
    public void setUploadedFile(ByteBuffer uploadedFile) {
        this.uploadedFile = uploadedFile;
    }

    /** {@inheritDoc} */
    public void set_uploadedFile_fileName(String fileName) {
        this.fileName = fileName;
    }

    /** {@inheritDoc} */
    public void set_uploadedFile_contentType(String contentType) {
        this.contentType = contentType;
    }

    /** {@inheritDoc} */
    public void uploadLockboxFile() throws ServiceException {
        try {

            BillingDomainInterface domain = getDomainsDirectory().getBillingDomain();
            LockboxRepositoryInterface repository = domain.getLockboxRepository();
            PartyRepositoryInterface partyRepository = getDomainsDirectory().getPartyDomain().getPartyRepository();
            Party organization = partyRepository.getPartyById(organizationPartyId);

            // mainly a test first
            Debug.logInfo("Uploading file [" + fileName + "] with content-type [" + contentType + "]", MODULE);

            // parse the data, this throws an exception if the data is inconsistent
            LockboxFileParser parser = new LockboxFileParser();
            parser.parse(new String(uploadedFile.array()));

            String fileNameStripped = fileName;
            int extIdx = fileNameStripped.lastIndexOf('.');
            if (extIdx >= 0) {
                fileNameStripped = fileNameStripped.substring(0, extIdx);
            }

            // checks if content of the file is identical to one of the files processed earlier.
            // this check prevents repeated import.
            if (repository.isHashExistent(parser.getHash())) {
                List<LockboxBatch> lb = repository.getBatchesByHash(parser.getHash());
                for (LockboxBatch b : lb) {
                    String batchId = b.getBatchId();
                    if (UtilValidate.isNotEmpty(batchId) && batchId.startsWith(fileNameStripped)) {
                        // identical file with the same name was imported already
                        throw new ServiceException("FinancialsServiceErrorLockboxFileAlreadyImported", UtilMisc.toMap("fileName", fileName));
                    }
                }
                // identical file with different name was imported earlier
                throw new ServiceException("FinancialsServiceErrorLockboxEquivalentFile", UtilMisc.toMap("fileName", fileName));
            }

            // check the routing numbers parsed in the file
            Map<String, String> accountAndRoutingNumbers = parser.getAccountAndRoutingNumbers();
            if (accountAndRoutingNumbers.size() > 1) {
                Debug.logWarning("More than one routing numbers was found in the uploaded file (found " + accountAndRoutingNumbers.size() + ")", MODULE);
            }

            for (String accountNumber : accountAndRoutingNumbers.keySet()) {
                String routingNumber = accountAndRoutingNumbers.get(accountNumber);
                // check the routing number correspond to an account of one organization
                PaymentMethodAndEftAccount account = repository.getPaymentMethod(accountNumber, routingNumber);
                Debug.logInfo("Found account [" + account + "] for routingNumber: " + routingNumber, MODULE);
                if (account == null || !organizationPartyId.equals(account.getPartyId())) {
                    throw new ServiceException("FinancialsServiceErrorLockboxNotOrgRoutingNumber", UtilMisc.toMap("routingNumber", routingNumber, "organizationName", organization.getName(), "organizationPartyId", organizationPartyId));
                }
            }


            // complete the primary keys before storing in the database
            List<LockboxBatch> batches = parser.getLockboxBatches();
            List<LockboxBatchItem> batchItems = parser.getLockboxBatchItems();
            List<LockboxBatchItemDetail> batchItemDetails = parser.getLockboxBatchItemDetails();

            // keep track of created batches
            List<String> createdBatchIds = new ArrayList<String>();

            // all batches are guarantee to have unique batch ids by the parser
            // set the lockboxBatchId for those entities according to these
            // set the user that uploaded the file, finally store the entities in the DB
            for (LockboxBatch batch : batches) {
                batch.initRepository(repository);
                String lockboxBatchId = batch.getNextSeqId();
                String batchId = batch.getBatchId();
                // set the batch id as the file name + batch number
                String newBatchId = fileNameStripped + batchId;
                batch.setLockboxBatchId(lockboxBatchId);
                createdBatchIds.add(lockboxBatchId);
                batch.setBatchId(newBatchId);
                batch.setCreatedByUserLoginId(getUser().getOfbizUserLogin().getString("userLoginId"));
                repository.createOrUpdate(batch);

                for (LockboxBatchItem item : batchItems) {
                    if (item.getLockboxBatchId().equals(batchId)) {
                        item.initRepository(repository);
                        item.setLockboxBatchId(lockboxBatchId);
                        repository.createOrUpdate(item);
                    }
                }

                for (LockboxBatchItemDetail item : batchItemDetails) {
                    if (item.getLockboxBatchId().equals(batchId)) {
                        item.initRepository(repository);
                        item.setLockboxBatchId(lockboxBatchId);
                        item.setIsUserEntered("N");
                        repository.createOrUpdate(item);
                    }
                }
            }

            // try to auto apply the details which have no error
            for (String lockboxBatchId : createdBatchIds) {
                LockboxBatch batch = repository.getBatchById(lockboxBatchId);
                for (LockboxBatchItem item : batch.getLockboxBatchItems()) {
                    // check that the check amounts are at least equal to their applications
                    BigDecimal applicationsTotal = BigDecimal.ZERO;
                    for (LockboxBatchItemDetail detail : item.getLockboxBatchItemDetails()) {
                        applicationsTotal = applicationsTotal.add(detail.getInvoiceAmount());
                    }
                    if (applicationsTotal.compareTo(item.getCheckAmount()) > 0) {
                        throw new ServiceException("FinancialsServiceErrorLockboxApplicationsExceedCheckAmount", UtilMisc.toMap("applicationsTotal", applicationsTotal, "checkAmount", item.getCheckAmount()));
                    }

                    if (!item.hasError()) {
                        for (LockboxBatchItemDetail detail : item.getLockboxBatchItemDetails()) {
                            setAmountToApply(detail.getInvoiceAmount());
                            setLockboxBatchId(lockboxBatchId);
                            setItemSeqId(detail.getItemSeqId());
                            setDetailSeqId(detail.getDetailSeqId());
                            setCashDiscount(BigDecimal.ZERO);
                            try {
                                updateLockboxBatchItemDetail();
                            } catch (ServiceException e) {
                                Debug.logWarning("Could not auto apply detail : " + detail + "\nReason: + " + e.getMessage(), MODULE);
                            }
                        }
                    }
                }
            }

            setSuccessMessage("FinancialsUploadLockboxFileSuccessful", UtilMisc.toMap("fileName", fileName));

        } catch (GeneralException ex) {
            throw new ServiceException(ex);
        }
    }

    /** {@inheritDoc} */
    public void setLockboxBatchId(String lockboxBatchId) {
        this.lockboxBatchId = lockboxBatchId;
    }

    /** {@inheritDoc} */
    public void setItemSeqId(String itemSeqId) {
        this.itemSeqId = itemSeqId;
    }

    /** {@inheritDoc} */
    public void setDetailSeqId(String detailSeqId) {
        this.detailSeqId = detailSeqId;
    }

    /** {@inheritDoc} */
    public void setPartyId(String partyId) {
        this.partyId = partyId;
    }

    /** {@inheritDoc} */
    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    /** {@inheritDoc} */
    public void setAmountToApply(BigDecimal amountToApply) {
        this.amountToApply = amountToApply;
    }

    /** {@inheritDoc} */
    public void setCashDiscount(BigDecimal cashDiscount) {
        this.cashDiscount = cashDiscount;
    }

    /** {@inheritDoc} */
    public void addLockboxBatchItemDetail() throws ServiceException {
        try {

            BillingDomainInterface domain = getDomainsDirectory().getBillingDomain();
            LockboxRepositoryInterface repository = domain.getLockboxRepository();
            InvoiceRepositoryInterface invoiceRepository = domain.getInvoiceRepository();

            // get the parent LockboxBatchItem, this throws an error if not found
            repository.getBatchItemById(lockboxBatchId, itemSeqId);

            // if an invoice id is given get the system invoice. Invalid id results in an error
            Invoice invoice = null;
            if (invoiceId != null) {
                invoice = invoiceRepository.getInvoiceById(invoiceId);
            }

            // if a valid invoice id is given and no party id, set the party id to the invoice
            // customer id
            if (partyId == null && invoice != null) {
                partyId = invoice.getPartyId();
            }

            // create new detail
            LockboxBatchItemDetail detail = new LockboxBatchItemDetail(repository);
            detail.setLockboxBatchId(lockboxBatchId);
            detail.setItemSeqId(itemSeqId);
            detail.setNextSubSeqId(LockboxBatchItemDetail.Fields.detailSeqId.getName(), 2);
            detail.setIsUserEntered("Y");
            detail.setInvoiceAmount(amountToApply.add(cashDiscount));
            // set both the invoice and customer numbers
            detail.setInvoiceNumber(invoiceId);
            detail.setCustomerId(partyId);
            // check that the given party and invoice are valid
            if (detail.getStatus().isError()) {
                throw new ServiceException(detail.getStatus().getMessage());
            }
            // store the detail line
            repository.createOrUpdate(detail);

            detailSeqId = detail.getDetailSeqId();

            // call the applyLockboxBatchItemDetail service
            updateLockboxBatchItemDetail();

        } catch (GeneralException ex) {
            throw new ServiceException(ex);
        }

        setSuccessMessage("FinancialsLockboxUserApplicationCreated");
    }

    /** {@inheritDoc} */
    public void updateLockboxBatchItemDetail() throws ServiceException {
        try {

            BillingDomainInterface domain = getDomainsDirectory().getBillingDomain();
            LockboxRepositoryInterface repository = domain.getLockboxRepository();

            // get the LockboxBatchItemDetail, this throws an error if not found
            LockboxBatchItemDetail detail = repository.getBatchItemDetailById(lockboxBatchId, itemSeqId, detailSeqId);

            // check if this line can be updated
            if (!detail.canUpdate()) {
                throw new ServiceException("FinancialsServiceErrorLockboxCannotApplyThisLine", null);
            }

            boolean toRemove = (amountToApply.compareTo(BigDecimal.ZERO) == 0 && cashDiscount.compareTo(BigDecimal.ZERO) == 0);

            // cannot apply negative amounts
            if (amountToApply.signum() < 0) {
                throw new ServiceException("FinancialsServiceErrorLockboxNegativeAmount", null);
            }


            // user entered lines can have their invoice amount updated or be removed
            if (detail.isUserEntered()) {
                // remove the line if amount to apply and cash discount is zero
                if (toRemove) {
                    setSuccessMessage("FinancialsLockboxUserApplicationRemoved");
                    repository.remove(detail);
                    return;
                }

                detail.setInvoiceAmount(amountToApply);
            }

            // only allow to set the amount to 0 or the line total amount
            if (!toRemove && detail.getInvoiceAmount().compareTo(amountToApply) != 0) {
                throw new ServiceException("FinancialsServiceErrorLockboxCannotApplyDifferentAmount", null);
            }

            detail.setAmountToApply(amountToApply);
            detail.setCashDiscount(cashDiscount);

            // update the detail
            repository.update(detail);

            if (toRemove) {
                setSuccessMessage("FinancialsLockboxAmountRemoved");
            } else {
                setSuccessMessage("FinancialsLockboxAmountApplied");
            }

        } catch (GeneralException ex) {
            throw new ServiceException(ex);
        }
    }

    /** {@inheritDoc} */
    public void lockboxBatchItemDetailAction() throws ServiceException {
        if (detailSeqId != null) {
            updateLockboxBatchItemDetail();
        } else {
            addLockboxBatchItemDetail();
        }
    }

    /** {@inheritDoc} */
    public void processLockboxBatch() throws ServiceException {
        try {

            BillingDomainInterface domain = getDomainsDirectory().getBillingDomain();
            LockboxRepositoryInterface repository = domain.getLockboxRepository();
            PartyRepositoryInterface partyRepository = getDomainsDirectory().getPartyDomain().getPartyRepository();
            LedgerRepositoryInterface ledgerRepository = getDomainsDirectory().getLedgerDomain().getLedgerRepository();

            // get the LockboxBatch, this throws an error if not found
            LockboxBatch batch = repository.getBatchById(lockboxBatchId);

            List<LockboxBatchItem> items = batch.getLockboxBatchItemsReadyToApply();
            if (items.isEmpty()) {
                throw new ServiceException("FinancialsServiceErrorLockboxNoReadyLine", null);
            }

            // get the organization to apply the payment to
            Party org = partyRepository.getPartyById(organizationPartyId);

            // create payments and applications
            //  since a LockboxBatchItem can apply to multiple customers and the LockboxBatchItem itself do not have the customer information
            //  we create one payment and on payment application per LockboxBatchItemDetail
            for (LockboxBatchItem item : items) {
                for (LockboxBatchItemDetail detail : item.getValidLockboxBatchItemDetails()) {
                    // ignore application if amount is zero
                    if (detail.getTotal().compareTo(BigDecimal.ZERO) == 0) {
                        continue;
                    }

                    // create the Payment
                    FinancialsCreatePaymentService service = new FinancialsCreatePaymentService();
                    service.setInPaymentTypeId("CUSTOMER_PAYMENT");
                    service.setInPaymentMethodTypeId("COMPANY_CHECK");
                    // payment against invoice or just a customer account
                    if (detail.getInvoice() != null) {
                        service.setInPartyIdTo(detail.getInvoice().getPartyIdFrom());
                        service.setInPartyIdFrom(detail.getInvoice().getPartyId());
                        service.setInCurrencyUomId(detail.getInvoice().getCurrencyUomId());
                    } else {
                        service.setInPartyIdTo(org.getPartyId());
                        service.setInPartyIdFrom(detail.getCustomer().getPartyId());
                        service.setInCurrencyUomId(org.getPartyAcctgPreference().getBaseCurrencyUomId());
                    }
                    // receive ONLY the amount to apply, the cash discount will be accounted as an invoice adjustment
                    service.setInAmount(detail.getAmountToApply());
                    service.setInStatusId("PMNT_NOT_PAID");
                    service.setInEffectiveDate(batch.getDatetimeEntered());
                    // add some references
                    service.setInPaymentRefNum(item.getCheckNumber());
                    service.setInComments(expandLabel("FinancialsLockboxCommentPayment", UtilMisc.toMap("batchId", batch.getBatchId())));
                    // set the payment method, so that when the payment is posted, it will debit directly to the checking account and not the undeposited receipts account
                    if (item.getAccountNumber() != null) {
                        PaymentMethodAndEftAccount targetPaymentMethod = repository.getPaymentMethod(item.getAccountNumber(), item.getRoutingNumber());
                        if (targetPaymentMethod != null) {
                            service.setInPaymentMethodId(targetPaymentMethod.getPaymentMethodId());
                        } else {
                            Debug.logWarning("No payment method found for lockbox batch [" + item.getLockboxBatchId() + "] and seq [" + item.getItemSeqId() + "]", MODULE);
                        }
                    } else {
                        Debug.logWarning("No account number found for lockbox batch [" + item.getLockboxBatchId() + "] and seq [" + item.getItemSeqId() + "]", MODULE);
                    }

                    // call the createPayment service
                    runSync(service);

                    // get the created payment id
                    String paymentId = service.getOutPaymentId();

                    if (detail.getInvoice() != null) {

                        // create the adjustment for cash discount, only for positive cash discounts
                        if (detail.getCashDiscount().signum() > 0) {
                            InvoiceServiceInterface invoiceService = getInvoiceService();
                            invoiceService.setInvoiceId(detail.getInvoice().getInvoiceId());
                            invoiceService.setInvoiceAdjustmentTypeId("CASH_DISCOUNT");
                            // the cash discount on the LockboxItemDetail has an opposite sign:
                            // $10 means a cash discount of $10, so that's InvoiceAdjustment.adjustmentAmount = -10
                            invoiceService.setAdjustmentAmount(detail.getCashDiscount().negate());
                            invoiceService.setPaymentId(paymentId);
                            invoiceService.setComment(expandLabel("FinancialsLockboxCommentCashDiscount", UtilMisc.toMap("batchId", batch.getBatchId())));
                            invoiceService.createInvoiceAdjustment();
                        }

                        // for negative cash discount, apply it to the configured GL Account (GL account type LOCKBOX_CASH_DISC)
                        if (detail.getCashDiscount().signum() < 0) {
                            // this will throw an EntityNotFoundException if the GL account is not found
                            GeneralLedgerAccount lockboxGlAccount = ledgerRepository.getDefaultLedgerAccount("LOCKBOX_CASH_DISC", org.getPartyId());
                            // create the PaymentApplication
                            CreatePaymentApplicationService service2 = new CreatePaymentApplicationService();
                            service2.setInPaymentId(paymentId);
                            service2.setInOverrideGlAccountId(lockboxGlAccount.getGlAccountId());
                            // the cash discount on the LockboxItemDetail has an opposite sign:
                            // -$10 means to apply 10$ of the payment to the GL account
                            service2.setInAmountApplied(detail.getCashDiscount().negate());
                            // call the createPaymentApplication service
                            runSync(service2);
                        }

                        // create the PaymentApplication
                        CreatePaymentApplicationService service2 = new CreatePaymentApplicationService();
                        service2.setInPaymentId(paymentId);
                        service2.setInInvoiceId(detail.getInvoice().getInvoiceId());
                        service2.setInAmountApplied(detail.getAmountToApplyToInvoice());
                        // call the createPaymentApplication service
                        runSync(service2);

                        // get the created payment application id
                        String paymentApplicationId = service2.getOutPaymentApplicationId();

                        detail.setPaymentApplicationId(paymentApplicationId);

                    } else if (detail.getCashDiscount().signum() > 0) {
                        throw new ServiceException("FinancialsServiceErrorLockboxCashDiscountOnlyForInvoices", null);
                    }

                    detail.setPaymentId(paymentId);
                    repository.update(detail);

                    // set payment as received
                    SetPaymentStatusService service2 = new SetPaymentStatusService();
                    service2.setInPaymentId(paymentId);
                    service2.setInStatusId("PMNT_RECEIVED");
                    // call the setPaymentStatus service
                    runSync(service2);

                    // decrease the pending amount for the batch
                    batch.setOutstandingAmount(batch.getOutstandingAmount().subtract(detail.getAmountToApply()));
                }
                // finally update the batch outstanding amount
                repository.update(batch);
            }
        } catch (GeneralException ex) {
            throw new ServiceException(ex);
        }

        setSuccessMessage("FinancialsProcessLockboxBatchSuccess");
    }

    private InvoiceServiceInterface getInvoiceService() throws ServiceException {
        return getDomainsDirectory().getBillingDomain().getInvoiceService();
    }
}
