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

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.common.util.UtilCommon;
import org.opentaps.common.util.UtilMessage;

import java.sql.Timestamp;
import java.util.Map;
import java.util.List;

import javolution.util.FastList;

public class SupplierImportServices {

    public static String module = SupplierImportServices.class.getName();

    public static Map<String, Object> importSuppliers(DispatchContext dctx, Map<String, ?> context) {

        String organizationPartyId = (String) context.get("organizationPartyId");
        int imported = 0;

        try {
            // then import the tax rates for the counties
            OpentapsImporter supplierImporter = new OpentapsImporter("DataImportSupplier", dctx, new SupplierDecoder(organizationPartyId));
            imported += supplierImporter.runImport();
        } catch (GenericEntityException e) {
            return UtilMessage.createAndLogServiceError(e, module);
        }
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("importedRecords", imported);
        return result;
    }
}

// maps DataImportSupplier into a set of opentaps entities that describes the Supplier
class SupplierDecoder implements ImportDecoder {
    public static final String module = SupplierDecoder.class.getName();
    protected String organizationPartyId;

    public SupplierDecoder(String organizationPartyId) {
        this.organizationPartyId = organizationPartyId;
    }

    public List<GenericValue> decode(GenericValue entry, Timestamp importTimestamp, Delegator delegator, LocalDispatcher dispatcher, Object... args) throws Exception {
        List<GenericValue> toBeStored = FastList.newInstance();

        String baseCurrencyUomId = UtilCommon.getOrgBaseCurrency(organizationPartyId, delegator);

        Debug.logInfo("Now processing  supplier name [" + entry.get("supplierName") +"]", module);

        /***********************/
        /** Import Party data **/
        /***********************/
        // create the company with the roles
        String partyId = entry.getString("supplierId");     // use same partyId as the Supplier's supplierId
        toBeStored.addAll(UtilImport.makePartyWithRoles(partyId, "PARTY_GROUP", UtilMisc.toList("SUPPLIER"), delegator));
        GenericValue company = delegator.makeValue("PartyGroup", UtilMisc.toMap("partyId", partyId, "groupName", entry.getString("supplierName"), "isIncorporated", entry.getString("isIncorporated"), "federalTaxId", entry.getString("federalTaxId"), "requires1099", entry.getString("requires1099")));
        toBeStored.add(company);
        String primaryPartyName = org.ofbiz.party.party.PartyHelper.getPartyName(company);
        Debug.logInfo("Creating PartyGroup ["+partyId+"] for Supplier ["+entry.get("supplierId")+"].", module);

        GenericValue partySupplementalData = delegator.makeValue("PartySupplementalData", UtilMisc.toMap("partyId", partyId, "companyName", primaryPartyName));

        /*******************************************************************************************************/
        /** Import contact mechs.  Note that each contact mech will be associated with the company and person. */
        /*******************************************************************************************************/

        if (!UtilValidate.isEmpty(entry.getString("address1"))) {
            // associate this as the GENERAL_LOCATION and BILLING_LOCATION
            GenericValue contactMech = delegator.makeValue("ContactMech", UtilMisc.toMap("contactMechId", delegator.getNextSeqId("ContactMech"), "contactMechTypeId", "POSTAL_ADDRESS"));
            String postalAddressContactMechId = contactMech.getString("contactMechId");
            GenericValue mainPostalAddress = UtilImport.makePostalAddress(contactMech, entry.getString("supplierName"), "", "", entry.getString("attnName"), entry.getString("address1"), entry.getString("address2"), entry.getString("city"), entry.getString("stateProvinceGeoId"), entry.getString("postalCode"), entry.getString("postalCodeExt"), entry.getString("countryGeoId"), delegator);
            toBeStored.add(contactMech);
            toBeStored.add(mainPostalAddress);

            toBeStored.add(UtilImport.makeContactMechPurpose("GENERAL_LOCATION", mainPostalAddress, partyId, importTimestamp, delegator));
            toBeStored.add(UtilImport.makeContactMechPurpose("BILLING_LOCATION", mainPostalAddress, partyId, importTimestamp, delegator));
            toBeStored.add(delegator.makeValue("PartyContactMech", UtilMisc.toMap("contactMechId", postalAddressContactMechId, "partyId", partyId, "fromDate", importTimestamp)));
            partySupplementalData.set("primaryPostalAddressId", postalAddressContactMechId);
        }

        if (!UtilValidate.isEmpty(entry.getString("primaryPhoneNumber"))) {
            // associate this as PRIMARY_PHONE
            GenericValue contactMech = delegator.makeValue("ContactMech", UtilMisc.toMap("contactMechId", delegator.getNextSeqId("ContactMech"), "contactMechTypeId", "TELECOM_NUMBER"));
            String telecomContactMechId = contactMech.getString("contactMechId");
            GenericValue primaryNumber = UtilImport.makeTelecomNumber(contactMech, entry.getString("primaryPhoneCountryCode"), entry.getString("primaryPhoneAreaCode"), entry.getString("primaryPhoneNumber"), delegator);
            toBeStored.add(contactMech);
            toBeStored.add(primaryNumber);

            toBeStored.add(UtilImport.makeContactMechPurpose("PRIMARY_PHONE", primaryNumber, partyId, importTimestamp, delegator));
            toBeStored.add(delegator.makeValue("PartyContactMech", UtilMisc.toMap("contactMechId", telecomContactMechId, "partyId", partyId, "fromDate", importTimestamp, "extension", entry.getString("primaryPhoneExtension"))));
            partySupplementalData.set("primaryTelecomNumberId", telecomContactMechId);
        }

        if (!UtilValidate.isEmpty(entry.getString("secondaryPhoneNumber"))) {
            // this one has no contactmech purpose type
            GenericValue contactMech = delegator.makeValue("ContactMech", UtilMisc.toMap("contactMechId", delegator.getNextSeqId("ContactMech"), "contactMechTypeId", "TELECOM_NUMBER"));
            GenericValue secondaryNumber = UtilImport.makeTelecomNumber(contactMech, entry.getString("secondaryPhoneCountryCode"), entry.getString("secondaryPhoneAreaCode"), entry.getString("secondaryPhoneNumber"), delegator);
            toBeStored.add(contactMech);
            toBeStored.add(secondaryNumber);

            toBeStored.add(delegator.makeValue("PartyContactMech", UtilMisc.toMap("contactMechId", contactMech.get("contactMechId"), "partyId", partyId, "fromDate", importTimestamp, "extension", entry.getString("secondaryPhoneExtension"))));
        }

        if (!UtilValidate.isEmpty(entry.getString("faxNumber"))) {
            // associate this as FAX_NUMBER
            GenericValue contactMech = delegator.makeValue("ContactMech", UtilMisc.toMap("contactMechId", delegator.getNextSeqId("ContactMech"), "contactMechTypeId", "TELECOM_NUMBER"));
            GenericValue faxNumber = UtilImport.makeTelecomNumber(contactMech, entry.getString("faxCountryCode"), entry.getString("faxAreaCode"), entry.getString("faxNumber"), delegator);
            toBeStored.add(contactMech);
            toBeStored.add(faxNumber);

            toBeStored.add(UtilImport.makeContactMechPurpose("FAX_NUMBER", faxNumber, partyId, importTimestamp, delegator));
            toBeStored.add(delegator.makeValue("PartyContactMech", UtilMisc.toMap("contactMechId", contactMech.get("contactMechId"), "partyId", partyId, "fromDate", importTimestamp)));
        }

        if (!UtilValidate.isEmpty(entry.getString("didNumber"))) {
            // associate this as PHONE_DID
            GenericValue contactMech = delegator.makeValue("ContactMech", UtilMisc.toMap("contactMechId", delegator.getNextSeqId("ContactMech"), "contactMechTypeId", "TELECOM_NUMBER"));
            GenericValue didNumber = UtilImport.makeTelecomNumber(contactMech, entry.getString("didCountryCode"), entry.getString("didAreaCode"), entry.getString("didNumber"), delegator);
            toBeStored.add(contactMech);
            toBeStored.add(didNumber);

            toBeStored.add(UtilImport.makeContactMechPurpose("PHONE_DID", didNumber, partyId, importTimestamp, delegator));
            toBeStored.add(delegator.makeValue("PartyContactMech", UtilMisc.toMap("contactMechId", contactMech.get("contactMechId"), "partyId", partyId, "fromDate", importTimestamp, "extension", entry.getString("didExtension"))));
        }

        if (!UtilValidate.isEmpty(entry.getString("emailAddress"))) {
            // make the email address
            GenericValue emailContactMech = delegator.makeValue("ContactMech", UtilMisc.toMap("contactMechId", delegator.getNextSeqId("ContactMech"), "contactMechTypeId", "EMAIL_ADDRESS", "infoString", entry.getString("emailAddress")));
            String emailContactMechId = emailContactMech.getString("contactMechId");
            toBeStored.add(emailContactMech);

            toBeStored.add(delegator.makeValue("PartyContactMech", UtilMisc.toMap("contactMechId", emailContactMechId, "partyId", partyId, "fromDate", importTimestamp)));
            toBeStored.add(UtilImport.makeContactMechPurpose("PRIMARY_EMAIL", emailContactMech, partyId, importTimestamp, delegator));
            partySupplementalData.set("primaryEmailId", emailContactMechId);
        }

        if (!UtilValidate.isEmpty(entry.getString("webAddress"))) {
            // make the web address
            GenericValue webContactMech = delegator.makeValue("ContactMech", UtilMisc.toMap("contactMechId", delegator.getNextSeqId("ContactMech"), "contactMechTypeId", "WEB_ADDRESS", "infoString", entry.getString("webAddress")));
            toBeStored.add(webContactMech);

            toBeStored.add(delegator.makeValue("PartyContactMech", UtilMisc.toMap("contactMechId", webContactMech.get("contactMechId"), "partyId", partyId, "fromDate", importTimestamp)));
            toBeStored.add(UtilImport.makeContactMechPurpose("PRIMARY_WEB_URL", webContactMech, partyId, importTimestamp, delegator));
        }

        toBeStored.add(partySupplementalData);

        /*****************************/
        /** Import Party notes. **/
        /*****************************/

        if (!UtilValidate.isEmpty(entry.getString("note"))) {
            // make the party note
            GenericValue noteData = delegator.makeValue("NoteData", UtilMisc.toMap("noteId", delegator.getNextSeqId("NoteData"), "noteInfo", entry.getString("note"), "noteDateTime", importTimestamp));
            toBeStored.add(noteData);
            toBeStored.add(delegator.makeValue("PartyNote", UtilMisc.toMap("noteId", noteData.get("noteId"), "partyId", partyId)));
        }

        Long netPaymentDays = entry.getLong("netPaymentDays");
        if (netPaymentDays != null && netPaymentDays.longValue() > 0) {
            String agreementId = delegator.getNextSeqId("Agreement");

            Map<String, Object> agreement = UtilMisc.<String, Object>toMap("agreementId", agreementId);
            agreement.put("partyIdFrom", organizationPartyId);
            agreement.put("partyIdTo", partyId);
            agreement.put("roleTypeIdTo", "SUPPLIER");
            agreement.put("agreementTypeId", "PURCHASE_AGREEMENT");
            agreement.put("agreementDate", importTimestamp);
            agreement.put("fromDate", importTimestamp);
            agreement.put("statusId", "AGR_ACTIVE");
            agreement.put("description", "Supplier agreement" + (UtilValidate.isEmpty(primaryPartyName) ? "" : " for ") + primaryPartyName);
            toBeStored.add(delegator.makeValue("Agreement", agreement));

            int itemSeqId = 0;

            if (netPaymentDays != null && netPaymentDays.longValue() > 0) {
                itemSeqId++;
                Map<String, Object> netPaymentDaysItem = UtilMisc.<String, Object>toMap("agreementId", agreementId, "agreementItemSeqId", Integer.valueOf(itemSeqId).toString(), "agreementItemTypeId", "AGREEMENT_PAYMENT");
                toBeStored.add(delegator.makeValue("AgreementItem", netPaymentDaysItem));
                Map<String, Object> netPaymentDaysTerm = UtilMisc.toMap("agreementId", agreementId, "agreementTermId", delegator.getNextSeqId("AgreementTerm"), "termTypeId", "FIN_PAYMENT_TERM", "agreementItemSeqId", Integer.valueOf(itemSeqId).toString(), "termDays", netPaymentDays, "currencyUomId", baseCurrencyUomId);
                toBeStored.add(delegator.makeValue("AgreementTerm", netPaymentDaysTerm));
            }
        }

        entry.put("primaryPartyId", partyId);
        toBeStored.add(entry);

        return toBeStored;
    }
}
