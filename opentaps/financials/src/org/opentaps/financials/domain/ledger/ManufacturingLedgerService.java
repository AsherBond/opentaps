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

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilNumber;
import org.opentaps.domain.DomainService;
import org.opentaps.base.constants.AcctgTransTypeConstants;
import org.opentaps.base.constants.GlAccountTypeConstants;
import org.opentaps.base.constants.WorkEffortTypeConstants;
import org.opentaps.base.entities.Facility;
import org.opentaps.domain.ledger.AccountingTransaction;
import org.opentaps.domain.ledger.GeneralLedgerAccount;
import org.opentaps.domain.ledger.LedgerDomainInterface;
import org.opentaps.domain.ledger.LedgerException;
import org.opentaps.domain.ledger.LedgerRepositoryInterface;
import org.opentaps.domain.ledger.ManufacturingLedgerServiceInterface;
import org.opentaps.domain.manufacturing.ManufacturingDomainInterface;
import org.opentaps.domain.manufacturing.ManufacturingRepositoryInterface;
import org.opentaps.domain.manufacturing.ProductionRun;
import org.opentaps.domain.organization.Organization;
import org.opentaps.domain.organization.OrganizationDomainInterface;
import org.opentaps.domain.organization.OrganizationRepositoryInterface;
import org.opentaps.foundation.service.ServiceException;

/**
 * POJO Service class for services that interact with the ledger.
 */
public class ManufacturingLedgerService extends DomainService implements ManufacturingLedgerServiceInterface {

    private static String module = ManufacturingLedgerService.class.getName();

    private String productionRunId;
    private String acctgTransId;

    private static int decimals = -1;
    private static int rounding = -1;
    static {
        decimals = UtilNumber.getBigDecimalScale("invoice.decimals");
        rounding = UtilNumber.getBigDecimalRoundingMode("invoice.rounding");
    }

    /**
     * Default constructor.
     */
    public ManufacturingLedgerService() {
        super();
    }

    /** {@inheritDoc} */
    public void setProductionRunId(String productionRunId) {
        this.productionRunId = productionRunId;
    }

    /** {@inheritDoc} */
    public String getAcctgTransId() {
        return this.acctgTransId;
    }

    /**
     * {@inheritDoc}
     * 1. verifies that the workEffortId is a production run (workEffortTypeId = PROD_ORDER_HEADER)
     * 2. calculates the total cost of this production run from getProductionRunCost
     * 3. finds all the inventory items produced by the production run and calculate the value of all the inventory items produced from the quantity * unitCost of each inventory item
     * 4. calculate the production run variance = total cost of production run - total value of inventory items, and create and post acctg trans which Debit MFG EXPENSE VARIANCE, Credit WIP INVENTORY for this production run variance
     */
    public void postProductionRunCostVarianceToGl() throws ServiceException {
        try {
            OrganizationDomainInterface organizationDomain = getDomainsDirectory().getOrganizationDomain();
            OrganizationRepositoryInterface organizationRepository = organizationDomain.getOrganizationRepository();
            ManufacturingDomainInterface manufacturingDomain = getDomainsDirectory().getManufacturingDomain();
            ManufacturingRepositoryInterface manufacturingRepository = manufacturingDomain.getManufacturingRepository();
            LedgerDomainInterface ledgerDomain = getDomainsDirectory().getLedgerDomain();
            LedgerRepositoryInterface ledgerRepository = ledgerDomain.getLedgerRepository();

            // 1. verifies that the workEffortId is a production run (workEffortTypeId = PROD_ORDER_HEADER)
            ProductionRun productionRun = manufacturingRepository.getProductionRun(productionRunId);
            if (!WorkEffortTypeConstants.PROD_ORDER_HEADER.equals(productionRun.getWorkEffortTypeId())) {
                throw new LedgerException("Work effort [" + productionRunId + "] is not a Production Run header (PROD_ORDER_HEADER)");
            }

            Facility facility = productionRun.getFacility();
            String ownerPartyId = facility.getOwnerPartyId();

            // 1.1. if the Organization does not use standard costing, log a message and return
            Organization organization = organizationRepository.getOrganizationById(ownerPartyId);
            if (!organization.usesStandardCosting()) {
                Debug.logInfo("The Organization [" + ownerPartyId + "] does not use standard costing, stopping here.", module);
                return;
            }

            // 2. Calculates the total cost of this production run from getProductionRunCost
            BigDecimal productionRunCost = productionRun.getTotalCost();

            // 3. finds all the inventory items produced by the production run and calculate the value of all the inventory items produced
            //  from the quantity * unitCost of each inventory item
            BigDecimal itemsProducedValue = productionRun.getItemsProducedTotalValue();

            // 4. calculate the production run variance = total cost of production run - total value of inventory items,
            // and create and post acctg trans which Debit MFG EXPENSE VARIANCE, Credit WIP INVENTORY for this production run variance
            BigDecimal variance = productionRunCost.subtract(itemsProducedValue).setScale(decimals, rounding);

            GeneralLedgerAccount creditAccount = ledgerRepository.getDefaultLedgerAccount(GlAccountTypeConstants.WIP_INVENTORY, ownerPartyId);
            GeneralLedgerAccount debitAccount = ledgerRepository.getDefaultLedgerAccount(GlAccountTypeConstants.MfgExpense.MFG_EXPENSE_VARIANCE, ownerPartyId);

            AccountingTransaction acctgTrans = new AccountingTransaction();
            acctgTrans.setAcctgTransTypeId(AcctgTransTypeConstants.MANUFACTURING_ATX);
            acctgTrans.setWorkEffortId(productionRun.getWorkEffortId());
            acctgTrans = ledgerRepository.createSimpleTransaction(acctgTrans, debitAccount, creditAccount, ownerPartyId, variance, null);

            this.acctgTransId = acctgTrans.getAcctgTransId();
        } catch (GeneralException e) {
            throw new ServiceException(e);
        }
    }

}
