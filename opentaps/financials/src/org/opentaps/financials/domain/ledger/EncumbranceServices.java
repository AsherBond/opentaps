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
import java.sql.Timestamp;
import java.util.List;

import javolution.util.FastList;

import org.hibernate.HibernateException;
import org.ofbiz.accounting.util.UtilAccounting;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericEntityException;
import org.opentaps.base.constants.EncumbranceDetailTypeConstants;
import org.opentaps.base.constants.GlAccountTypeConstants;
import org.opentaps.base.constants.InvoiceTypeConstants;
import org.opentaps.base.entities.AcctgTransEntry;
import org.opentaps.base.entities.EncumbranceDetail;
import org.opentaps.base.entities.EncumbranceSnapshot;
import org.opentaps.base.entities.InvoiceItemType;
import org.opentaps.base.entities.InvoiceItemTypeGlAccount;
import org.opentaps.base.entities.ProductGlAccount;
import org.opentaps.common.domain.order.OrderSpecification;
import org.opentaps.domain.DomainService;
import org.opentaps.domain.billing.invoice.InvoiceRepositoryInterface;
import org.opentaps.domain.ledger.AccountingTransaction;
import org.opentaps.domain.ledger.EncumbranceRepositoryInterface;
import org.opentaps.domain.ledger.EncumbranceServiceInterface;
import org.opentaps.domain.ledger.GeneralLedgerAccount;
import org.opentaps.domain.ledger.LedgerRepositoryInterface;
import org.opentaps.domain.order.Order;
import org.opentaps.domain.order.OrderItem;
import org.opentaps.domain.order.OrderRepositoryInterface;
import org.opentaps.foundation.entity.hibernate.Session;
import org.opentaps.foundation.entity.hibernate.Transaction;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.ofbiz.Repository;
import org.opentaps.foundation.service.ServiceException;

public class EncumbranceServices extends DomainService implements EncumbranceServiceInterface {
	private static final String MODULE = EncumbranceServices.class.getName();

    private String organizationPartyId;
    private Timestamp startDatetime = null;
    private Timestamp snapshotDatetime;
    private String comments;
    private String description;

    /** {@inheritDoc} */
    public void createEncumbranceSnapshotAndDetail() throws ServiceException {
        Session session = null;
        Transaction tx = null;

        // use current time if snapshot time isn't provided
        Timestamp moment = snapshotDatetime == null ? UtilDateTime.nowTimestamp() : snapshotDatetime;

        List<EncumbranceDetail> encumbranceDetails = FastList.newInstance();

        try {
            session = infrastructure.getSession();

            InvoiceRepositoryInterface invoiceRepository = getDomainsDirectory().getBillingDomain().getInvoiceRepository();

            // retrieve open purchase orders and items
            OrderRepositoryInterface orderRepository = getDomainsDirectory().getOrderDomain().getOrderRepository();
            List<Order> openOrders = orderRepository.getOpenOrders(organizationPartyId, OrderSpecification.OrderTypeEnum.PURCHASE);
            for (Order order : openOrders) {
                // add encumrances from opened order items
                List<OrderItem> orderItems = orderRepository.getOpenOrderItems(order);
                if (UtilValidate.isEmpty(orderItems)) {
                    continue;
                }

                for (OrderItem item : orderItems) {

                    EncumbranceDetail encumbrance = new EncumbranceDetail();
                    encumbrance.setEncumbranceDetailTypeId(EncumbranceDetailTypeConstants.ENCUMB_PURCHASING);
                    encumbrance.setPartyId(order.getBillFromPartyId());
                    encumbrance.setOrderId(item.getOrderId());
                    encumbrance.setOrderItemSeqId(item.getOrderItemSeqId());
                    encumbrance.setOriginalQuantity(item.getQuantity());
                    encumbrance.setInvoicedQuantity(item.getInvoicedQuantity());
                    encumbrance.setCancelQuantity(item.getCancelQuantity());
                    BigDecimal unitAmount = item.getUnitPrice();
                    if (unitAmount == null) {
                        unitAmount = BigDecimal.ZERO;
                    }
                    encumbrance.setUnitAmount(unitAmount);
                    // encumberedQuantity = quantity - MAX(canceledQuantity, invoicedQuantity)
                    encumbrance.setEncumberedQuantity(
                            item.getQuantity().subtract(
                                    BigDecimal.valueOf(
                                            Math.max(item.getCancelQuantity().doubleValue(), item.getInvoicedQuantity().doubleValue())
                                    )
                            )
                    );
                    // encumberedAmount = encumberedQuantity * unitAmount)
                    encumbrance.setEncumberedAmount(encumbrance.getEncumberedQuantity().multiply(encumbrance.getUnitAmount()));
                    encumbrance.setAcctgTagEnumId1(item.getAcctgTagEnumId1());
                    encumbrance.setAcctgTagEnumId2(item.getAcctgTagEnumId2());
                    encumbrance.setAcctgTagEnumId3(item.getAcctgTagEnumId3());
                    encumbrance.setAcctgTagEnumId4(item.getAcctgTagEnumId4());
                    encumbrance.setAcctgTagEnumId5(item.getAcctgTagEnumId5());
                    encumbrance.setAcctgTagEnumId6(item.getAcctgTagEnumId6());
                    encumbrance.setAcctgTagEnumId7(item.getAcctgTagEnumId7());
                    encumbrance.setAcctgTagEnumId8(item.getAcctgTagEnumId8());
                    encumbrance.setAcctgTagEnumId9(item.getAcctgTagEnumId9());
                    encumbrance.setAcctgTagEnumId10(item.getAcctgTagEnumId10());

                    String glAccountId = null;
                    // try to find corresponding GL account id checking product configuration first
                    ProductGlAccount productGlAccount =
                        invoiceRepository.findOne(ProductGlAccount.class, invoiceRepository.map(
                                ProductGlAccount.Fields.productId, item.getProductId(),
                                ProductGlAccount.Fields.organizationPartyId, organizationPartyId,
                                ProductGlAccount.Fields.glAccountTypeId, GlAccountTypeConstants.Expense.EXPENSE
                        ));
                    if (productGlAccount != null) {
                        glAccountId = productGlAccount.getGlAccountId();
                    }

                    // or find corresponding GL account by means of establishing relation between
                    // order item type and invoice item type and its gl account for organization.
                    InvoiceItemType invoiceItemType = null;
                    if (glAccountId == null) {
                        invoiceItemType = invoiceRepository.getInvoiceItemType(item, InvoiceTypeConstants.PURCHASE_INVOICE);
                        List<? extends InvoiceItemTypeGlAccount> invItemTypeGlAccts = invoiceItemType.getInvoiceItemTypeGlAccounts();
                        if (UtilValidate.isNotEmpty(invItemTypeGlAccts)) {
                            for (InvoiceItemTypeGlAccount acct : invItemTypeGlAccts) {
                                if (organizationPartyId.equals(acct.getOrganizationPartyId())) {
                                    glAccountId = acct.getGlAccountId();
                                    break;
                                }
                            }
                        }
                    }

                    // default if not found
                    if (glAccountId == null && invoiceItemType != null) {
                        glAccountId = invoiceItemType.getDefaultGlAccountId();
                    }
                    encumbrance.setGlAccountId(glAccountId);

                    encumbranceDetails.add(encumbrance);
                }

                // add encumbrance from order adjustments
                BigDecimal adjAmount = order.getUninvoicedNonItemAdjustmentValue();
                if (adjAmount != null && adjAmount.compareTo(BigDecimal.ZERO) != 0) {
                    EncumbranceDetail encumbrance = new EncumbranceDetail();
                    encumbrance.setEncumbranceDetailTypeId(EncumbranceDetailTypeConstants.ENCUMB_PURCHASING);
                    encumbrance.setPartyId(order.getBillFromPartyId());
                    encumbrance.setOrderId(order.getOrderId());
                    encumbrance.setEncumberedAmount(adjAmount);
                    encumbranceDetails.add(encumbrance);
                }
            }

            // retrieve posted transaction with fiscal type ENCUMBRANCE for given time period
            LedgerRepositoryInterface ledgerRepository = getDomainsDirectory().getLedgerDomain().getLedgerRepository();
            List<AccountingTransaction> transactions = ledgerRepository.getPostedTransactions(organizationPartyId, "ENCUMBRANCE", startDatetime, moment);
            for (AccountingTransaction acctgTrans : transactions) {
                List<AcctgTransEntry> entries = ledgerRepository.getTransactionEntries(acctgTrans.getAcctgTransId());
                if (UtilValidate.isEmpty(entries)) {
                    continue;
                }

                for (AcctgTransEntry entry : entries) {
                    GeneralLedgerAccount account = ledgerRepository.getLedgerAccount(entry.getGlAccountId(), organizationPartyId);
                    if (UtilAccounting.isExpenseAccount(Repository.genericValueFromEntity(account))) {
                        // an expense GL account on ENCUMBRANCE transaction has to be recorded as encumbrance.
                        EncumbranceDetail encumbrance = new EncumbranceDetail();
                        encumbrance.setEncumbranceDetailTypeId(EncumbranceDetailTypeConstants.ENCUMB_MANUAL);
                        encumbrance.setPartyId(acctgTrans.getPartyId());
                        encumbrance.setAcctgTransId(entry.getAcctgTransId());
                        encumbrance.setAcctgTransEntryId(entry.getAcctgTransEntrySeqId());
                        encumbrance.setDebitCreditFlag(entry.getDebitCreditFlag());
                        encumbrance.setGlAccountId(entry.getGlAccountId());
                        // encumberedAmount = transaction entry amount
                        encumbrance.setEncumberedAmount(entry.getAmount());
                        encumbrance.setAcctgTagEnumId1(entry.getAcctgTagEnumId1());
                        encumbrance.setAcctgTagEnumId2(entry.getAcctgTagEnumId2());
                        encumbrance.setAcctgTagEnumId3(entry.getAcctgTagEnumId3());
                        encumbrance.setAcctgTagEnumId4(entry.getAcctgTagEnumId4());
                        encumbrance.setAcctgTagEnumId5(entry.getAcctgTagEnumId5());
                        encumbrance.setAcctgTagEnumId6(entry.getAcctgTagEnumId6());
                        encumbrance.setAcctgTagEnumId7(entry.getAcctgTagEnumId7());
                        encumbrance.setAcctgTagEnumId8(entry.getAcctgTagEnumId8());
                        encumbrance.setAcctgTagEnumId9(entry.getAcctgTagEnumId9());
                        encumbrance.setAcctgTagEnumId10(entry.getAcctgTagEnumId10());
                        encumbranceDetails.add(encumbrance);
                    }
                }
            }

            if (UtilValidate.isNotEmpty(encumbranceDetails)) {
                // create parent EncumbranceSnapshot entity
                EncumbranceSnapshot snapshot = new EncumbranceSnapshot();
                snapshot.setSnapshotDatetime(moment);
                snapshot.setCreatedByUserLoginId(getUser().getUserId());
                if (UtilValidate.isNotEmpty(comments)) {
                    snapshot.setComments(comments);
                }
                if (UtilValidate.isNotEmpty(description)) {
                    snapshot.setDescription(description);
                }

                long seqNum = 1;
                // store encumbrance details
                for (EncumbranceDetail encumbrance : encumbranceDetails) {
                    encumbrance.setOrganizationPartyId(organizationPartyId);
                    encumbrance.setEncumbranceDetailSeqId(StringUtil.padNumberString(Long.valueOf(seqNum).toString(), 5));
                    seqNum++;
                }

                EncumbranceRepositoryInterface encumbRepository = getDomainsDirectory().getLedgerDomain().getEncumbranceRepository();
                tx = session.beginTransaction();
                encumbRepository.createEncumbranceSnapshot(snapshot, encumbranceDetails, session);
                tx.commit();
            }

        } catch (RepositoryException e) {
            throw new ServiceException(e.getMessage());
        } catch (GenericEntityException e) {
            throw new ServiceException(e.getMessage());
        } catch (InfrastructureException e) {
            throw new ServiceException(e.getMessage());
        } catch (HibernateException e) {
        	// return the ServiceException with the message of exception
            throw new ServiceException(e.getMessage());
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    /** {@inheritDoc} */
    public void setOrganizationPartyId(String organizationPartyId) {
        this.organizationPartyId = organizationPartyId;
    }

    /** {@inheritDoc} */
    public void setSnapshotDatetime(Timestamp snapshotDatetime) {
        this.snapshotDatetime = snapshotDatetime;
    }

    /** {@inheritDoc} */
    public void setStartDatetime(Timestamp startDatetime) {
        this.startDatetime = startDatetime;
    }

    /** {@inheritDoc} */
    public void setComments(String comments) {
        this.comments = comments;
    }

    /** {@inheritDoc} */
    public void setDescription(String description) {
        this.description = description;
    }

}
