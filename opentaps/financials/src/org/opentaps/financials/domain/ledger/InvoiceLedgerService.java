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
package org.opentaps.financials.domain.ledger;

import java.math.BigDecimal;

import org.opentaps.domain.DomainService;
import org.opentaps.base.entities.InvoiceAdjustment;
import org.opentaps.base.entities.InvoiceAdjustmentGlAccount;
import org.opentaps.base.entities.InvoiceGlAccountType;
import org.opentaps.domain.billing.invoice.Invoice;
import org.opentaps.domain.billing.invoice.InvoiceRepositoryInterface;
import org.opentaps.domain.billing.invoice.InvoiceServiceInterface;
import org.opentaps.domain.ledger.*;
import org.opentaps.domain.organization.Organization;
import org.opentaps.domain.organization.OrganizationRepositoryInterface;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;

/** {@inheritDoc} */
public class InvoiceLedgerService extends DomainService implements InvoiceLedgerServiceInterface {

    protected String invoiceId;
    protected String invoiceAdjustmentId;

    protected InvoiceRepositoryInterface invoiceRepository;
    protected LedgerRepositoryInterface ledgerRepository;
    protected OrganizationRepositoryInterface organizationRepository;
    protected LedgerSpecificationInterface ledgerSpecification;
    protected InvoiceServiceInterface invoiceService;
    protected Invoice invoice = null;

    /**
     * Default constructor.
     */
    public InvoiceLedgerService() {
        super();
    }

    /** {@inheritDoc} */
    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    /** {@inheritDoc} */
    public void setInvoiceAdjustmentId(String invoiceAdjustmentId) {
        this.invoiceAdjustmentId = invoiceAdjustmentId;
    }

    /** {@inheritDoc} */
    public void postInvoiceAdjustmentToLedger() throws ServiceException {
        try {
            invoiceRepository = getInvoiceRepository();
            ledgerRepository = getLedgerRepository();
            organizationRepository = getOrganizationRepository();
            ledgerSpecification = getLedgerSpecification();

            // get the adjustment and the invoice
            InvoiceAdjustment invoiceAdjustment = invoiceRepository.getInvoiceAdjustmentById(invoiceAdjustmentId);
            invoice = invoiceRepository.getInvoiceById(invoiceAdjustment.getInvoiceId());

            // get the configurations for GL account posting
            String organizationPartyId = invoice.getOrganizationPartyId();
            InvoiceAdjustmentGlAccount invoiceAdjustmentGlAccount = ledgerRepository.getInvoiceAdjustmentGlAccount(organizationPartyId, invoice.getInvoiceTypeId(), invoiceAdjustment.getInvoiceAdjustmentTypeId());
            InvoiceGlAccountType invoiceGlAccountType = ledgerRepository.getInvoiceGlAccountType(organizationPartyId, invoice.getInvoiceTypeId());
            GeneralLedgerAccount debitGlAccount = null;
            GeneralLedgerAccount creditGlAccount = null;
            if (invoice.isReceivable()) {
                // ie: for a sales invoice, DR AR Adjustment, CR Accounts Receivable
                debitGlAccount = ledgerRepository.getLedgerAccount(invoiceAdjustmentGlAccount.getGlAccountId(), organizationPartyId);
                creditGlAccount = ledgerRepository.getDefaultLedgerAccount(invoiceGlAccountType.getInvoiceGlAccountTypeId(), organizationPartyId);
            } else {
                // ie: for a purchase invoice, DR Accounts Payable, CR AP Adjustment
                debitGlAccount = ledgerRepository.getDefaultLedgerAccount(invoiceGlAccountType.getInvoiceGlAccountTypeId(), organizationPartyId);
                creditGlAccount = ledgerRepository.getLedgerAccount(invoiceAdjustmentGlAccount.getGlAccountId(), organizationPartyId);
            }

            // create the accounting transactions for this adjustment
            String transactionPartyId = invoice.getTransactionPartyId();

            AccountingTransaction acctgTrans = new AccountingTransaction();
            acctgTrans.setInvoiceId(invoiceAdjustment.getInvoiceId());
            acctgTrans.setInvoiceAdjustmentId(invoiceAdjustmentId); // so we can uniquely identify this adjustment, if we ever need to revert it
            acctgTrans.setPaymentId(invoiceAdjustment.getPaymentId());
            acctgTrans.setAcctgTransTypeId(invoiceAdjustmentGlAccount.getAcctgTransTypeId());

            // convert currency if needed
            Organization organization = organizationRepository.getOrganizationById(organizationPartyId);
            BigDecimal transactionAmount = organization.convertUom(invoiceAdjustment.getAmount(), invoice.getCurrencyUomId());

            // we want the negative value of the transaction to be posted, because the adjustments are negative values
            // ie a cash discount which reduces the invoice amount by $3 is -3.0
            transactionAmount = transactionAmount.negate();

            acctgTrans = ledgerRepository.createSimpleTransaction(acctgTrans, debitGlAccount, creditGlAccount, organizationPartyId, transactionAmount, transactionPartyId);

        } catch (Exception ex) {
            throw new ServiceException(ex);
        }
    }

    /** {@inheritDoc} */
    public void postInvoiceWriteoffToGl() throws ServiceException {
        try {
            // get the invoice here so we can set the adjustment amount to its outstanding amount
            invoiceRepository = getInvoiceRepository();
            invoice = invoiceRepository.getInvoiceById(invoiceId);

            invoiceService = getInvoiceService();
            invoiceService.setInvoiceId(invoice.getInvoiceId());
            // need to post the invoice write off amount as an adjustment in the  negative amount of the invoice outstanding value
            invoiceService.setAdjustmentAmount(invoice.getOpenAmount().negate());
            invoiceService.setInvoiceAdjustmentTypeId(getLedgerSpecification().getAdjustmentTypeIdForWriteOff());
            invoiceService.createInvoiceAdjustment();
        } catch (Exception ex) {
            throw new ServiceException(ex);
        }
    }

    private LedgerRepositoryInterface getLedgerRepository() throws RepositoryException {
        return getDomainsDirectory().getLedgerDomain().getLedgerRepository();
    }

    private LedgerSpecificationInterface getLedgerSpecification() throws RepositoryException  {
        return getLedgerRepository().getSpecification();
    }

    private OrganizationRepositoryInterface getOrganizationRepository() throws RepositoryException  {
        return getDomainsDirectory().getOrganizationDomain().getOrganizationRepository();
    }

    private InvoiceRepositoryInterface getInvoiceRepository() throws RepositoryException  {
        return getDomainsDirectory().getBillingDomain().getInvoiceRepository();
    }

    private InvoiceServiceInterface getInvoiceService() throws ServiceException {
        return getDomainsDirectory().getBillingDomain().getInvoiceService();
    }
}
