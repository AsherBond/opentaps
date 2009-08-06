/*
 * Copyright (c) 2007 - 2009 Open Source Strategies, Inc.
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
package org.opentaps.financials.domain.billing.financials;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.entity.GenericEntityException;
import org.opentaps.domain.base.entities.AccountBalanceHistory;
import org.opentaps.domain.billing.financials.AccountBalanceHistoryServiceInterface;
import org.opentaps.domain.organization.Organization;
import org.opentaps.domain.organization.OrganizationRepositoryInterface;
import org.opentaps.foundation.entity.hibernate.Session;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.Service;
import org.opentaps.foundation.service.ServiceException;

import com.opensourcestrategies.financials.accounts.AccountsHelper;

/**
 * POJO implementation of services which create snapshot of customer/vendor/commission balances.
 * opentaps Service foundation class.
 */
public class AccountBalanceHistoryService extends Service implements AccountBalanceHistoryServiceInterface {

    private static final String MODULE = AccountBalanceHistoryService.class.getName();
    // session object, using to store/search pojos.
    private Session session;
    // timestamp of snapshot
    private Timestamp asOfDatetime;

    /** {@inheritDoc} */
    public void captureAccountBalancesSnapshot() throws ServiceException {
        try {
            OrganizationRepositoryInterface organizationRepositoryInterface = domains.getOrganizationDomain().getOrganizationRepository();
            session = getInfrastructure().getSession();
            List<Organization> allValidOrganizations = organizationRepositoryInterface.getAllValidOrganizations();
            asOfDatetime = UtilDateTime.nowTimestamp();
            for (Organization organization : allValidOrganizations) {
                // use AccountsHelper.getBalancesForAllCustomers to get Balances For Customers
                Map<String, BigDecimal> balances = AccountsHelper.getBalancesForAllCustomers(organization.getPartyId(), "ACTUAL", asOfDatetime, getInfrastructure().getDelegator());
                createSnapshotForBalance(organization.getPartyId(), "CUSTOMERS", balances);

                // use AccountsHelper.getBalancesForAllVendors to get Balances For Suppliers
                balances = AccountsHelper.getBalancesForAllVendors(organization.getPartyId(), "ACTUAL", asOfDatetime, getInfrastructure().getDelegator());
                createSnapshotForBalance(organization.getPartyId(), "SUPPLIERS", balances);

                // use AccountsHelper.getBalancesForAllCommissions  to get Balances For Commissions
                balances = AccountsHelper.getBalancesForAllCommissions(organization.getPartyId(), "ACTUAL", asOfDatetime, getInfrastructure().getDelegator());
                createSnapshotForBalance(organization.getPartyId(), "COMMISSIONS", balances);
            }
            session.flush();
        } catch (RepositoryException e) {
            Debug.logError(e, MODULE);
            throw new ServiceException(e);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            throw new ServiceException(e);
        } catch (InfrastructureException e) {
            Debug.logError(e, MODULE);
            throw new ServiceException(e);
        }
//        finally {
//            session.close();
//        }
    }

    /**
     * Create snapshot for a balance <code>Map</code> .
     *
     * @param organizationPartyId a <code>String</code> value
     * @param balanceTypeEnumId a <code>String</code> value
     * @param balances a <code>Map<String, BigDecimal></code> value
     */
    private void createSnapshotForBalance(String organizationPartyId, String balanceTypeEnumId, Map<String, BigDecimal> balances) {
        Iterator<Entry<String, BigDecimal>> it = balances.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, BigDecimal> entry = it.next();
            AccountBalanceHistory accountBalanceHistory = new AccountBalanceHistory();
            accountBalanceHistory.setAsOfDatetime(asOfDatetime);
            accountBalanceHistory.setPartyId(entry.getKey());
            accountBalanceHistory.setBalanceTypeEnumId(balanceTypeEnumId);
            accountBalanceHistory.setOrganizationPartyId(organizationPartyId);
            accountBalanceHistory.setTotalBalance(entry.getValue());
            session.save(accountBalanceHistory);
        }
    }
}
