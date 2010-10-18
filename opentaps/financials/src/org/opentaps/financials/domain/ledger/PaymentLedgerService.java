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
import java.util.List;

import org.opentaps.domain.DomainService;
import org.opentaps.base.constants.AcctgTransTypeConstants;
import org.opentaps.base.constants.GlAccountTypeConstants;
import org.opentaps.base.entities.InvoiceGlAccountType;
import org.opentaps.base.entities.PaymentApplication;
import org.opentaps.domain.billing.invoice.Invoice;
import org.opentaps.domain.billing.invoice.InvoiceRepositoryInterface;
import org.opentaps.domain.billing.invoice.InvoiceServiceInterface;
import org.opentaps.domain.billing.payment.Payment;
import org.opentaps.domain.billing.payment.PaymentRepositoryInterface;
import org.opentaps.domain.ledger.*;
import org.opentaps.domain.organization.Organization;
import org.opentaps.domain.organization.OrganizationRepositoryInterface;
import org.opentaps.domain.party.Account;
import org.opentaps.domain.party.Party;
import org.opentaps.domain.party.PartyRepositoryInterface;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;

/** {@inheritDoc} */
public class PaymentLedgerService extends DomainService implements PaymentLedgerServiceInterface {

    protected String paymentId;

    protected PaymentRepositoryInterface paymentRepository;
    protected PartyRepositoryInterface partyRepository;
    protected InvoiceRepositoryInterface invoiceRepository;
    protected LedgerRepositoryInterface ledgerRepository;
    protected OrganizationRepositoryInterface organizationRepository;
    protected LedgerSpecificationInterface ledgerSpecification;
    protected InvoiceServiceInterface invoiceService;
    protected Invoice invoice = null;

    /**
     * Default constructor.
     */
    public PaymentLedgerService() {
        super();
    }

    /** {@inheritDoc} */
    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    /** {@inheritDoc} */
    public void reconcileParentSubAccountPayment() throws ServiceException {
        try {
            paymentRepository = getPaymentRepository();
            invoiceRepository = getInvoiceRepository();
            partyRepository = getPartyRepository();
            ledgerRepository = getLedgerRepository();
            organizationRepository = getOrganizationRepository();
            ledgerSpecification = getLedgerSpecification();

            // get the payment and its applications, the parties of the transaction
            Payment payment = paymentRepository.getPaymentById(paymentId);
            List<? extends PaymentApplication> paymentApplications = payment.getPaymentApplications();
            String organizationPartyId = payment.getOrganizationPartyId();
            String transactionPartyId = payment.getTransactionPartyId();
            Organization organization = organizationRepository.getOrganizationById(organizationPartyId);

            // check that the transaction party of this payment is actually an Account, so that it could have sub accounts
            // if not, then this service can just end
            Account transactionAccount = partyRepository.getAccountById(transactionPartyId);
            if ((transactionAccount == null) || (!transactionAccount.isAccount())) {
                setSuccessMessage("FinancialsPaymentNotFromAccount");
                return;
            }

            // get the account for the balancing entry of the transaction
            GeneralLedgerAccount balancingGlAccount = ledgerRepository.getDefaultLedgerAccount(GlAccountTypeConstants.PARENT_SUB_BAL_ACCT, organizationPartyId);

            // loop through all the payment applications, and see if the invoice transaction party is a sub account of the payment transaction party
            for (PaymentApplication paymentApplication : paymentApplications) {
                 if (paymentApplication.getInvoiceId() != null) {
                     Invoice invoice = invoiceRepository.getInvoiceById(paymentApplication.getInvoiceId());
                     Party invoiceTransactionParty = partyRepository.getPartyById(invoice.getTransactionPartyId());
                     if ((invoiceTransactionParty != null) && (invoiceTransactionParty.isSubAccount(transactionAccount))) {
                         // get the invoice gl account type, ie ACCOUNTS_RECEIVABLE
                         InvoiceGlAccountType invoiceGlAccountType = ledgerRepository.getInvoiceGlAccountType(organizationPartyId, invoice.getInvoiceTypeId());
                         GeneralLedgerAccount invoiceGlAccount = ledgerRepository.getDefaultLedgerAccount(invoiceGlAccountType.getInvoiceGlAccountTypeId(), organizationPartyId);

                         // convert currency if needed
                         BigDecimal transactionAmount = organization.convertUom(paymentApplication.getAmountApplied(), payment.getCurrencyUomId());

                         // create a transaction which offsets the parent account's accounts
                         // for example, it would DR Accounts Receivable, CR Balancing Account for the parent (payment transaction party)
                         String acctgTransactionPartyId = payment.getTransactionPartyId(); // this is the party of the payment, or the parent account
                         AccountingTransaction acctgTrans = new AccountingTransaction();
                         acctgTrans.setInvoiceId(paymentApplication.getInvoiceId());
                         acctgTrans.setPaymentId(paymentId);
                         acctgTrans.setPartyId(acctgTransactionPartyId);
                         acctgTrans.setAcctgTransTypeId(AcctgTransTypeConstants.ACCOUNT_BALANCING);

                         // first one is the debit, second one is the credit
                         acctgTrans = ledgerRepository.createSimpleTransaction(acctgTrans, invoiceGlAccount, balancingGlAccount, organizationPartyId, transactionAmount, acctgTransactionPartyId);

                         // now create a transaction which offsets the child account's accounts
                         // for example, it would DR Balancing Account, CR Accounts Receivable for the child account (invoice transaction party)
                         acctgTransactionPartyId = invoiceTransactionParty.getPartyId(); // this is the party of the invoice, or the child account
                         acctgTrans = new AccountingTransaction();
                         acctgTrans.setInvoiceId(paymentApplication.getInvoiceId());
                         acctgTrans.setPaymentId(paymentId);
                         acctgTrans.setPartyId(acctgTransactionPartyId);
                         acctgTrans.setAcctgTransTypeId(AcctgTransTypeConstants.ACCOUNT_BALANCING);

                         acctgTrans = ledgerRepository.createSimpleTransaction(acctgTrans, balancingGlAccount, invoiceGlAccount, organizationPartyId, transactionAmount, acctgTransactionPartyId);
                     }
                 }
            }

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

    private PartyRepositoryInterface getPartyRepository() throws RepositoryException  {
        return getDomainsDirectory().getPartyDomain().getPartyRepository();
    }

    private PaymentRepositoryInterface getPaymentRepository() throws RepositoryException  {
        return getDomainsDirectory().getBillingDomain().getPaymentRepository();
    }
}
