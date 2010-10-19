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
package org.opentaps.financials.domain.billing.financials;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.opensourcestrategies.financials.accounts.AccountsHelper;
import javolution.util.FastList;
import org.hibernate.HibernateException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericEntityException;
import org.opentaps.domain.DomainService;
import org.opentaps.base.entities.AccountBalanceHistory;
import org.opentaps.domain.billing.financials.AccountBalanceHistoryServiceInterface;
import org.opentaps.domain.organization.Organization;
import org.opentaps.domain.organization.OrganizationRepositoryInterface;
import org.opentaps.foundation.entity.hibernate.Session;
import org.opentaps.foundation.entity.hibernate.Transaction;
import org.opentaps.foundation.infrastructure.InfrastructureException;
import org.opentaps.foundation.repository.RepositoryException;
import org.opentaps.foundation.service.ServiceException;

/**
 * POJO implementation of services which create snapshot of customer/vendor/commission balances.
 * opentaps Service foundation class.
 */
public class AccountBalanceHistoryService extends DomainService implements AccountBalanceHistoryServiceInterface {

    private static final String MODULE = AccountBalanceHistoryService.class.getName();
    // session object, using to store/search pojos.
    private Session session;
    // timestamp of snapshot
    private Timestamp asOfDatetime;

    /** {@inheritDoc} */
    public void captureAccountBalancesSnapshot() throws ServiceException {
        Transaction tx = null;
        try {
            OrganizationRepositoryInterface organizationRepositoryInterface = getDomainsDirectory().getOrganizationDomain().getOrganizationRepository();
            session = getInfrastructure().getSession();
            List<Organization> allValidOrganizations = organizationRepositoryInterface.getAllValidOrganizations();
            List<Map<String, Object>> allBalances = FastList.newInstance();
            asOfDatetime = UtilDateTime.nowTimestamp();
            for (Organization organization : allValidOrganizations) {
                // use AccountsHelper.getBalancesForAllCustomers to get Balances For Customers
                Map<String, BigDecimal> balancesForAllCustomers = AccountsHelper.getBalancesForAllCustomers(organization.getPartyId(), "ACTUAL", asOfDatetime, getInfrastructure().getDelegator());
                allBalances.add(UtilMisc.toMap("organizationPartyId", organization.getPartyId(), "balanceTypeEnumId", "CUSTOMERS", "balances", balancesForAllCustomers));

                // use AccountsHelper.getBalancesForAllVendors to get Balances For Suppliers
                Map<String, BigDecimal> balancesForAllVendors = AccountsHelper.getBalancesForAllVendors(organization.getPartyId(), "ACTUAL", asOfDatetime, getInfrastructure().getDelegator());
                allBalances.add(UtilMisc.toMap("organizationPartyId", organization.getPartyId(), "balanceTypeEnumId", "SUPPLIERS", "balances", balancesForAllVendors));

                // use AccountsHelper.getBalancesForAllCommissions  to get Balances For Commissions
                Map<String, BigDecimal> balancesForAllCommissions = AccountsHelper.getBalancesForAllCommissions(organization.getPartyId(), "ACTUAL", asOfDatetime, getInfrastructure().getDelegator());
                allBalances.add(UtilMisc.toMap("organizationPartyId", organization.getPartyId(), "balanceTypeEnumId", "COMMISSIONS", "balances", balancesForAllCommissions));
            }
            //store balance to database
            tx = session.beginTransaction();
            for (Map<String, Object> balancesMap : allBalances) {
                String organizationPartyId = (String) balancesMap.get("organizationPartyId");
                String balanceTypeEnumId = (String) balancesMap.get("balanceTypeEnumId");
                Map<String, BigDecimal> balances = (Map<String, BigDecimal>) balancesMap.get("balances");
                createSnapshotForBalance(organizationPartyId, balanceTypeEnumId, balances);
            }

            session.flush();
            tx.commit();
        } catch (RepositoryException e) {
            Debug.logError(e, MODULE);
            throw new ServiceException(e);
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            throw new ServiceException(e);
        } catch (InfrastructureException e) {
            Debug.logError(e, MODULE);
            throw new ServiceException(e);
        } catch (HibernateException e) {
        	// return the ServiceException with the message of exception
            throw new ServiceException(e.getMessage());
        } finally {
            if (session != null) {
                session.close();
            }
        }
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
