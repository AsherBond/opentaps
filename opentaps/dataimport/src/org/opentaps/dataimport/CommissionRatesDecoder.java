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
package org.opentaps.dataimport;

import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.Debug;
import org.opentaps.common.util.UtilCommon;

import java.util.List;
import java.util.Map;
import java.sql.Timestamp;

import javolution.util.FastList;

/**
 * importCustomerCommissions: input parameter organizationPartyId
 *
 * for each DataImportCustomerCommissionRate(DICCR)
 * 1.  get DataImportCustomer.primaryPartyId for customerId
 * 2.  call ensurePartyRole service to create COMMISSION_AGENT role for DICCR.primaryPartyId
 * 3.  get baseCurrencyUomId for organizationPartyId with UtilCommon.getOrgBaseCurrency
 * 3.  create new Agreement with agreementTypeId=COMMISSION_AGREEMENT, partyIdFrom=organizationPartyId, roleTypeIdFrom=INTERNAL_ORGANIZATIO,
 *     partyIdTo=DICCR.primaryPartyId, roleTypeIdTo=COMMISSION_AGENT, description=Commission Agreement for ${Party Name for DICCR.primaryPartyId}
 *     fromDate=nowTimestamp, statusId=AGR_ACTIVE
 * 4.  create new AgreementItem under Agreement from (3) with agreementItemTypeId=COMM_RATES, currencyUomId=organizationPartyId baseCurrency, agreementText="Flat ${DICCR.commisionRate}% for ${Party Name for DICCR.primaryPartyId}"
 * 5.  create new AgreementTerm under AgreementItem from (4) with termTypeId=COMM_RATES and termValue=DICCR.commissionRate
 * 6.  create new AgreementItem under Agreement from (3) with agreementItemTypeId=COMM_CUSTOMERS, agreementText="Applies to all listed customers"
 * 7.  for each DataImportCustomerCommissionCustomers (DICCC) where customerId=DICCR.customerId
 * 7(a)  Get DataImportCustomer.primaryPartyId for DICCC.toCustomerId
 * 7(b)  create new AgreementTerm under AgreementItem from (6) with agreementItemTypeId=COMM_PARTY_APPL, partyId=DICCC.partyId
 */
public class CommissionRatesDecoder implements ImportDecoder {
    public static final String module = CommissionRatesDecoder.class.getName();
    protected String organizationPartyId;

    public CommissionRatesDecoder(String organizationPartyId) {
        this.organizationPartyId = organizationPartyId;
    }

    // first of the custom arguments is the organizationPartyId
    public List<GenericValue> decode(GenericValue entry, Timestamp importTimestamp, Delegator delegator, LocalDispatcher dispatcher, Object... args) throws Exception {
        List<GenericValue> toBeStored = FastList.newInstance();

        // 1.  get DataImportCustomer.primaryPartyId for customerId
        GenericValue dataImportCustomer = entry.getRelatedOne("DataImportCustomer");

        // 2.  call ensurePartyRole service to create COMMISSION_AGENT role for DICCR.primaryPartyId
        Map<String, Object> input = UtilMisc.<String, Object>toMap("partyId", dataImportCustomer.getString("primaryPartyId"));
        input.put("roleTypeId", "COMMISSION_AGENT");
        Map<String, Object> output = dispatcher.runSync("ensurePartyRole", input);
        if (ServiceUtil.isError(output)) {
            TransactionUtil.rollback();
            Debug.logWarning("Fail to import customer commission["+dataImportCustomer.getString("primaryPartyId")+"] because data was bad.  Check preceding warnings for reason.", module);
            return null;
        }

        // 3.  get baseCurrencyUomId for organizationPartyId with UtilCommon.getOrgBaseCurrency
        String baseCurrencyUomId = UtilCommon.getOrgBaseCurrency(organizationPartyId, delegator);
        String partyName = org.ofbiz.party.party.PartyHelper.getPartyName(delegator, dataImportCustomer.getString("primaryPartyId"), false);

        // 3.  create new Agreement with agreementTypeId=COMMISSION_AGREEMENT, partyIdFrom=organizationPartyId, roleTypeIdFrom=INTERNAL_ORGANIZATIO,
        //     partyIdTo=DICCR.primaryPartyId, roleTypeIdTo=COMMISSION_AGENT, description=Commission Agreement for ${Party Name for DICCR.primaryPartyId}
        //     fromDate=nowTimestamp, statusId=AGR_ACTIVE
        GenericValue agreement = delegator.makeValue("Agreement");
        agreement.put("agreementId", delegator.getNextSeqId("Agreement"));
        agreement.put("agreementTypeId", "COMMISSION_AGREEMENT");
        agreement.put("partyIdFrom", organizationPartyId);
        agreement.put("roleTypeIdFrom", "INTERNAL_ORGANIZATIO");
        agreement.put("partyIdTo", dataImportCustomer.getString("primaryPartyId"));
        agreement.put("roleTypeIdTo", "COMMISSION_AGENT");
        agreement.put("description", "Commission Agreement for " + partyName);
        agreement.put("fromDate", importTimestamp);
        agreement.put("statusId", "AGR_ACTIVE");
        toBeStored.add(agreement);

        // 4.  create new AgreementItem under Agreement from (3) with agreementItemTypeId=COMM_RATES, currencyUomId=organizationPartyId baseCurrency, agreementText="Flat ${DICCR.commisionRate}% for ${Party Name for DICCR.primaryPartyId}"
        GenericValue agreementItem = delegator.makeValue("AgreementItem");
        agreementItem.put("agreementId", agreement.getString("agreementId"));
        agreementItem.put("agreementItemSeqId", delegator.getNextSeqId("AgreementItem"));
        agreementItem.put("agreementItemTypeId", "COMM_RATES");
        agreementItem.put("currencyUomId", baseCurrencyUomId);
        agreementItem.put("agreementText", "Flat " + entry.getDouble("commissionRate") + "% for " + partyName);
        toBeStored.add(agreementItem);

        // 5.  create new AgreementTerm under AgreementItem from (4) with termTypeId=FLAT_COMMISSION and termValue=DICCR.commissionRate
        GenericValue agreementTerm = delegator.makeValue("AgreementTerm");
        agreementTerm.put("agreementTermId", delegator.getNextSeqId("AgreementTerm"));
        agreementTerm.put("agreementId", agreement.getString("agreementId"));
        agreementTerm.put("agreementItemSeqId", agreementItem.getString("agreementItemSeqId"));
        agreementTerm.put("termTypeId", "FLAT_COMMISSION");
        agreementTerm.put("termValue", entry.getDouble("commissionRate"));
        toBeStored.add(agreementTerm);

        // 6.  create new AgreementItem under Agreement from (3) with agreementItemTypeId=COMM_CUSTOMERS, agreementText="Applies to all listed customers"
        agreementItem = delegator.makeValue("AgreementItem");
        agreementItem.put("agreementId", agreement.getString("agreementId"));
        agreementItem.put("agreementItemSeqId", delegator.getNextSeqId("AgreementItem"));
        agreementItem.put("agreementItemTypeId", "COMM_CUSTOMERS");
        agreementItem.put("agreementText", "Applies to all listed customers");
        toBeStored.add(agreementItem);

        input = UtilMisc.<String, Object>toMap("customerId", entry.getString("customerId"));
        List<GenericValue> dataImportCommissionCustomers = delegator.findByAnd("DataImportCommissionCustomers", input);

        // 7.  for each DataImportCommissionCustomers (DICCC) where customerId=DICCR.customerId
        for(GenericValue dataImportCommissionCustomer : dataImportCommissionCustomers) {
            // 7(a)  Get DataImportCustomer.primaryPartyId for DICCC.toCustomerId
            GenericValue dataImportToCustomer = dataImportCommissionCustomer.getRelatedOne("CustomerDataImportCustomer");

            // 7(b)  create new AgreementTerm under AgreementItem from (6) with agreementItemTypeId=COMM_PARTY_APPL, partyId=DICCC.partyId
            agreementTerm = delegator.makeValue("AgreementTerm");
            agreementTerm.put("agreementTermId", delegator.getNextSeqId("AgreementTerm"));
            agreementTerm.put("agreementId", agreement.getString("agreementId"));
            agreementTerm.put("agreementItemSeqId", agreementItem.getString("agreementItemSeqId"));
            agreementTerm.put("termTypeId", "COMM_PARTY_APPL");
            agreementTerm.put("partyId", dataImportToCustomer.getString("primaryPartyId"));
            toBeStored.add(agreementTerm);
        }

        return toBeStored;
    }

}
