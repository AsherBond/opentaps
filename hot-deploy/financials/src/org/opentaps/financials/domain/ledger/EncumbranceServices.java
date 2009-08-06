/*
 * Copyright (c) 2008 - 2009 Open Source Strategies, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Honest Public License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Honest Public License for more details.
 *
 * You should have received a copy of the Honest Public License
 * along with this program; if not, write to Funambol,
 * 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
 */
package org.opentaps.financials.domain.ledger;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import javolution.util.FastList;

import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.ofbiz.accounting.util.UtilAccounting;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericEntityException;
import org.opentaps.common.domain.order.OrderSpecification;
import org.opentaps.domain.base.entities.AcctgTransEntry;
import org.opentaps.domain.base.entities.EncumbranceDetail;
import org.opentaps.domain.base.entities.EncumbranceSnapshot;
import org.opentaps.domain.ledger.AccountingTransaction;
import org.opentaps.domain.ledger.EncumbranceRepositoryInterface;
import org.opentaps.domain.ledger.EncumbranceServiceInterface;
import org.opentaps.domain.ledger.GeneralLedgerAccount;
import org.opentaps.domain.ledger.LedgerRepositoryInterface;
import org.opentaps.domain.order.Order;
import org.opentaps.domain.order.OrderItem;
import org.opentaps.domain.order.OrderRepositoryInterface;
import org.opentaps.foundation.entity.hibernate.Session;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.repository.ofbiz.Repository;
import org.opentaps.foundation.service.Service;
import org.opentaps.foundation.service.ServiceException;

public class EncumbranceServices extends Service implements EncumbranceServiceInterface {

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
                    encumbrance.setEncumbranceDetailTypeId("ENCUMB_PURCHASING");
                    encumbrance.setPartyId(order.getBillFromPartyId());
                    encumbrance.setOrderId(item.getOrderId());
                    encumbrance.setOrderItemSeqId(item.getOrderItemSeqId());
                    encumbrance.setOriginalQuantity(item.getQuantity());
                    encumbrance.setInvoicedQuantity(item.getInvoicedQuantity());
                    encumbrance.setCancelQuantity(item.getCancelQuantity());
                    encumbrance.setUnitAmount(item.getUnitPrice());
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
                    encumbranceDetails.add(encumbrance);
                }

                // add encumbrance from order adjustments
                BigDecimal adjAmount = order.getUninvoicedNonItemAdjustmentValue();
                if (adjAmount != null && adjAmount.compareTo(BigDecimal.ZERO) != 0) {
                    EncumbranceDetail encumbrance = new EncumbranceDetail();
                    encumbrance.setEncumbranceDetailTypeId("ENCUMB_PURCHASING");
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
                        encumbrance.setEncumbranceDetailTypeId("ENCUMB_MANUAL");
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
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
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
