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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import com.opensourcestrategies.crmsfa.party.PartyHelper;
import javolution.util.FastList;
import javolution.util.FastMap;
import org.apache.commons.validator.GenericValidator;
import org.ofbiz.base.util.*;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.LocalDispatcher;
import org.opentaps.common.util.UtilCommon;

/**
 * maps DataImportCustomer into a set of opentaps entities that describes the Customer
 * TODO: break this up big method into several logical steps, each implemented in class methods.
 * This will allow for much easier re-use for custom imports.
 */
public class CustomerDecoder implements ImportDecoder {
    private static final String MODULE = CustomerDecoder.class.getName();
    protected String initialResponsiblePartyId;
    protected String initialResponsibleRoleTypeId;
    protected String organizationPartyId;
    protected String arGlAccountId;
    protected String offsettingGlAccountId;
    protected GenericValue userLogin;

    /**
     * Creates a customer decoder from an input context.
     * This will automatically extract the class variables it needs and
     * validate for the existence of GL accounts and CRMSFA roles.
     * If there is a problem, a GeneralException is thrown.
     */
    public CustomerDecoder(Map<String, ?> context) throws GeneralException {
        this.initialResponsiblePartyId = (String) context.get("initialResponsiblePartyId");
        this.initialResponsibleRoleTypeId = (String) context.get("initialResponsibleRoleTypeId");
        this.organizationPartyId = (String) context.get("organizationPartyId");
        this.arGlAccountId = (String) context.get("arGlAccountId");
        this.offsettingGlAccountId = (String) context.get("offsettingGlAccountId");
        this.userLogin = (GenericValue) context.get("userLogin");

        validate();
    }

    // validates the accounts and ensures the initial responsible party has a CRMSFA role
    public void validate() throws GeneralException {
        Delegator delegator = userLogin.getDelegator();

        // first validate the existence of the accounts
        GenericValue glAccountOrganization = null;
        if (UtilValidate.isNotEmpty(arGlAccountId)) {
            glAccountOrganization = delegator.findByPrimaryKey("GlAccountOrganization", UtilMisc.toMap("glAccountId", arGlAccountId, "organizationPartyId", organizationPartyId));
            if (glAccountOrganization == null) {
                throw new GeneralException("Cannot import: organization [" + organizationPartyId + "] does not have Accounts Receivable General Ledger account [" + arGlAccountId + "] defined in GlAccountOrganization.");
            }
        }
        if (UtilValidate.isNotEmpty(offsettingGlAccountId)) {
            glAccountOrganization = delegator.findByPrimaryKey("GlAccountOrganization", UtilMisc.toMap("glAccountId", offsettingGlAccountId, "organizationPartyId", organizationPartyId));
            if (glAccountOrganization == null) {
                throw new GeneralException("Cannot import: organization [" + organizationPartyId + "] does not have offsetting General Ledger account [" + offsettingGlAccountId + "] defined in GlAccountOrganization.");
            }
        }

        // next ensure the role of the initial responsible party
        this.initialResponsibleRoleTypeId = PartyHelper.getFirstValidTeamMemberRoleTypeId(initialResponsiblePartyId, delegator);
        if (initialResponsibleRoleTypeId == null) {
            throw new GeneralException("Cannot import customers: No internal CRM role found for party [" + initialResponsiblePartyId + "]");
        }
    }

    public List<GenericValue> decode(GenericValue entry, Timestamp importTimestamp, Delegator delegator, LocalDispatcher dispatcher, Object... args) throws Exception {
        List<GenericValue> toBeStored = FastList.newInstance();

        String baseCurrencyUomId = UtilCommon.getOrgBaseCurrency(organizationPartyId, delegator);

        /***********************/
        /** Import Party data **/
        /***********************/

        // create the Person and Party with the roles for each depending on whether companyName or lastName is present
        String companyPartyId = null;
        String personPartyId = null;
        String primaryPartyId = null;  // this will be the partyId most other artifacts are associated with.  If company is present, it will be company, otherwise person
        String primaryPartyName = null;
        GenericValue partySupplementalData = null; // this will be the party Supplemental Data to keep the primary contact mech
        if ((entry.get("lastName") != null) && !("".equals(entry.getString("lastName")))) {
            personPartyId = delegator.getNextSeqId("Party");
            toBeStored.addAll(UtilImport.makePartyWithRoles(personPartyId, "PERSON", UtilMisc.toList("CONTACT", "BILL_TO_CUSTOMER"), delegator));
            GenericValue person = delegator.makeValue("Person", UtilMisc.toMap("partyId", personPartyId, "firstName", entry.getString("firstName"), "lastName", entry.getString("lastName")));
            toBeStored.add(person);
            Map<String, Object> partyRelationship = UtilMisc.toMap("partyIdTo", initialResponsiblePartyId, "roleTypeIdTo", initialResponsibleRoleTypeId, "partyIdFrom", personPartyId, "roleTypeIdFrom", "CONTACT", "partyRelationshipTypeId", "RESPONSIBLE_FOR", "fromDate", importTimestamp);
            partyRelationship.put("securityGroupId", "CONTACT_OWNER");
            toBeStored.add(delegator.makeValue("PartyRelationship", partyRelationship));
            primaryPartyId = personPartyId;
            primaryPartyName = org.ofbiz.party.party.PartyHelper.getPartyName(person);
            Debug.logInfo("Creating Person [" + personPartyId + "] for Customer [" + entry.get("customerId") + "].", MODULE);
        }
        if ((entry.get("companyName") != null) && !("".equals(entry.getString("companyName")))) {
            companyPartyId = delegator.getNextSeqId("Party");
            toBeStored.addAll(UtilImport.makePartyWithRoles(companyPartyId, "PARTY_GROUP", UtilMisc.toList("ACCOUNT", "BILL_TO_CUSTOMER"), delegator));
            GenericValue partyGroup = delegator.makeValue("PartyGroup", UtilMisc.toMap("partyId", companyPartyId, "groupName", entry.getString("companyName")));
            toBeStored.add(partyGroup);
            Map<String, Object> partyRelationship = UtilMisc.toMap("partyIdTo", initialResponsiblePartyId, "roleTypeIdTo", initialResponsibleRoleTypeId, "partyIdFrom", companyPartyId, "roleTypeIdFrom", "ACCOUNT", "partyRelationshipTypeId", "RESPONSIBLE_FOR", "fromDate", importTimestamp);
            partyRelationship.put("securityGroupId", "ACCOUNT_OWNER");
            toBeStored.add(delegator.makeValue("PartyRelationship", partyRelationship));
            // make the person a Contact of the company Account
            if (UtilValidate.isNotEmpty(personPartyId)) {
                partyRelationship = UtilMisc.toMap("partyIdFrom", personPartyId, "roleTypeIdFrom", "CONTACT", "partyRelationshipTypeId", "CONTACT_REL_INV",
                                                   "partyIdTo", companyPartyId, "roleTypeIdTo", "ACCOUNT", "fromDate", importTimestamp);
                toBeStored.add(delegator.makeValue("PartyRelationship", partyRelationship));
            }

            primaryPartyId = companyPartyId;
            primaryPartyName = org.ofbiz.party.party.PartyHelper.getPartyName(partyGroup);
            Debug.logInfo("Creating PartyGroup [" + companyPartyId + "] for Customer [" + entry.get("customerId") + "].", MODULE);
        }

        if (primaryPartyId == null) {
            Debug.logWarning("No person or company associated with customer [" + entry.get("customerId") + "]", MODULE);
            return null;
        }

        // associate person with company
        if ((companyPartyId != null) && (personPartyId != null)) {
            Map<String, Object> partyRelationship = UtilMisc.toMap("partyIdTo", companyPartyId, "roleTypeIdTo", "ACCOUNT", "partyIdFrom", personPartyId, "roleTypeIdFrom", "CONTACT", "partyRelationshipTypeId", "CONTACT_REL_INV", "fromDate", importTimestamp);
            toBeStored.add(delegator.makeValue("PartyRelationship", partyRelationship));
        }


        /*******************************************************************************************************/
        /** Import contact mechs.  Note that each contact mech will be associated with the company and person. */
        /*******************************************************************************************************/


        String billingContactMechId = null;  // for later use with things that need billing address
        if (!UtilValidate.isEmpty(entry.getString("address1"))) {
            // associate this as the GENERAL_LOCATION and BILLING_LOCATION
            GenericValue contactMech = delegator.makeValue("ContactMech", UtilMisc.toMap("contactMechId", delegator.getNextSeqId("ContactMech"), "contactMechTypeId", "POSTAL_ADDRESS"));
            GenericValue mainPostalAddress = UtilImport.makePostalAddress(contactMech, entry.getString("companyName"), entry.getString("firstName"), entry.getString("lastName"), entry.getString("attnName"), entry.getString("address1"), entry.getString("address2"), entry.getString("city"), entry.getString("stateProvinceGeoId"), entry.getString("postalCode"), entry.getString("postalCodeExt"), entry.getString("countryGeoId"), delegator);
            toBeStored.add(contactMech);
            toBeStored.add(mainPostalAddress);
            if (personPartyId != null) {
                toBeStored.add(UtilImport.makeContactMechPurpose("GENERAL_LOCATION", mainPostalAddress, personPartyId, importTimestamp, delegator));
                toBeStored.add(UtilImport.makeContactMechPurpose("BILLING_LOCATION", mainPostalAddress, personPartyId, importTimestamp, delegator));
                toBeStored.add(delegator.makeValue("PartyContactMech", UtilMisc.toMap("contactMechId", contactMech.get("contactMechId"), "partyId", personPartyId, "fromDate", importTimestamp)));
                toBeStored.add(UtilImport.makePartySupplementalData(partySupplementalData, personPartyId, "primaryPostalAddressId", mainPostalAddress, delegator));
            }
            if (companyPartyId != null) {
                toBeStored.add(UtilImport.makeContactMechPurpose("GENERAL_LOCATION", mainPostalAddress, companyPartyId, importTimestamp, delegator));
                toBeStored.add(UtilImport.makeContactMechPurpose("BILLING_LOCATION", mainPostalAddress, companyPartyId, importTimestamp, delegator));
                toBeStored.add(delegator.makeValue("PartyContactMech", UtilMisc.toMap("contactMechId", contactMech.get("contactMechId"), "partyId", companyPartyId, "fromDate", importTimestamp)));
                toBeStored.add(UtilImport.makePartySupplementalData(partySupplementalData, companyPartyId, "primaryPostalAddressId", mainPostalAddress, delegator));
            }
            billingContactMechId = contactMech.getString("contactMechId");
        }

        if (!UtilValidate.isEmpty(entry.getString("shipToAddress1"))) {
            // associate this as SHIPPING_LOCATION
            GenericValue contactMech = delegator.makeValue("ContactMech", UtilMisc.toMap("contactMechId", delegator.getNextSeqId("ContactMech"), "contactMechTypeId", "POSTAL_ADDRESS"));
            GenericValue secondaryPostalAddress = UtilImport.makePostalAddress(contactMech, entry.getString("shipToCompanyName"), entry.getString("shipToFirstName"), entry.getString("shipToLastName"), entry.getString("shipToAttnName"), entry.getString("shipToAddress1"), entry.getString("shipToAddress2"), entry.getString("shipToCity"), entry.getString("shipToStateProvinceGeoId"), entry.getString("shipToPostalCode"), entry.getString("shipToPostalCodeExt"), entry.getString("shipToCountryGeoId"), delegator);
            toBeStored.add(contactMech);
            toBeStored.add(secondaryPostalAddress);
            if (personPartyId != null) {
                toBeStored.add(UtilImport.makeContactMechPurpose("SHIPPING_LOCATION", secondaryPostalAddress, personPartyId, importTimestamp, delegator));
                toBeStored.add(delegator.makeValue("PartyContactMech", UtilMisc.toMap("contactMechId", contactMech.get("contactMechId"), "partyId", personPartyId, "fromDate", importTimestamp)));
            }
            if (companyPartyId != null) {
                toBeStored.add(UtilImport.makeContactMechPurpose("SHIPPING_LOCATION", secondaryPostalAddress, companyPartyId, importTimestamp, delegator));
                toBeStored.add(delegator.makeValue("PartyContactMech", UtilMisc.toMap("contactMechId", contactMech.get("contactMechId"), "partyId", companyPartyId, "fromDate", importTimestamp)));
            }
        }

        if (!UtilValidate.isEmpty(entry.getString("primaryPhoneNumber"))) {
            // associate this as PRIMARY_PHONE
            GenericValue contactMech = delegator.makeValue("ContactMech", UtilMisc.toMap("contactMechId", delegator.getNextSeqId("ContactMech"), "contactMechTypeId", "TELECOM_NUMBER"));
            GenericValue primaryNumber = UtilImport.makeTelecomNumber(contactMech, entry.getString("primaryPhoneCountryCode"), entry.getString("primaryPhoneAreaCode"), entry.getString("primaryPhoneNumber"), delegator);
            toBeStored.add(contactMech);
            toBeStored.add(primaryNumber);
            if (personPartyId != null) {
                toBeStored.add(UtilImport.makeContactMechPurpose("PRIMARY_PHONE", primaryNumber, personPartyId, importTimestamp, delegator));
                toBeStored.add(delegator.makeValue("PartyContactMech", UtilMisc.toMap("contactMechId", contactMech.get("contactMechId"), "partyId", personPartyId, "fromDate", importTimestamp, "extension", entry.getString("primaryPhoneExtension"))));
                toBeStored.add(UtilImport.makePartySupplementalData(partySupplementalData, personPartyId, "primaryTelecomNumberId", primaryNumber, delegator));
            }
            if (companyPartyId != null) {
                toBeStored.add(UtilImport.makeContactMechPurpose("PRIMARY_PHONE", primaryNumber, companyPartyId, importTimestamp, delegator));
                toBeStored.add(delegator.makeValue("PartyContactMech", UtilMisc.toMap("contactMechId", contactMech.get("contactMechId"), "partyId", companyPartyId, "fromDate", importTimestamp, "extension", entry.getString("primaryPhoneExtension"))));
                toBeStored.add(UtilImport.makePartySupplementalData(partySupplementalData, companyPartyId, "primaryTelecomNumberId", primaryNumber, delegator));
            }
        }

        if (!UtilValidate.isEmpty(entry.getString("secondaryPhoneNumber"))) {
            // this one has no contactmech purpose type
            GenericValue contactMech = delegator.makeValue("ContactMech", UtilMisc.toMap("contactMechId", delegator.getNextSeqId("ContactMech"), "contactMechTypeId", "TELECOM_NUMBER"));
            GenericValue secondaryNumber = UtilImport.makeTelecomNumber(contactMech, entry.getString("secondaryPhoneCountryCode"), entry.getString("secondaryPhoneAreaCode"), entry.getString("secondaryPhoneNumber"), delegator);
            toBeStored.add(contactMech);
            toBeStored.add(secondaryNumber);
            if (personPartyId != null) {
                toBeStored.add(delegator.makeValue("PartyContactMech", UtilMisc.toMap("contactMechId", contactMech.get("contactMechId"), "partyId", personPartyId, "fromDate", importTimestamp, "extension", entry.getString("secondaryPhoneExtension"))));
            }
            if (companyPartyId != null) {
                toBeStored.add(delegator.makeValue("PartyContactMech", UtilMisc.toMap("contactMechId", contactMech.get("contactMechId"), "partyId", companyPartyId, "fromDate", importTimestamp, "extension", entry.getString("secondaryPhoneExtension"))));
            }
        }

        if (!UtilValidate.isEmpty(entry.getString("faxNumber"))) {
            // associate this as FAX_NUMBER
            GenericValue contactMech = delegator.makeValue("ContactMech", UtilMisc.toMap("contactMechId", delegator.getNextSeqId("ContactMech"), "contactMechTypeId", "TELECOM_NUMBER"));
            GenericValue faxNumber = UtilImport.makeTelecomNumber(contactMech, entry.getString("faxCountryCode"), entry.getString("faxAreaCode"), entry.getString("faxNumber"), delegator);
            toBeStored.add(contactMech);
            toBeStored.add(faxNumber);
            if (personPartyId != null) {
                toBeStored.add(UtilImport.makeContactMechPurpose("FAX_NUMBER", faxNumber, personPartyId, importTimestamp, delegator));
                toBeStored.add(delegator.makeValue("PartyContactMech", UtilMisc.toMap("contactMechId", contactMech.get("contactMechId"), "partyId", personPartyId, "fromDate", importTimestamp)));
            }
            if (companyPartyId != null) {
                toBeStored.add(UtilImport.makeContactMechPurpose("FAX_NUMBER", faxNumber, companyPartyId, importTimestamp, delegator));
                toBeStored.add(delegator.makeValue("PartyContactMech", UtilMisc.toMap("contactMechId", contactMech.get("contactMechId"), "partyId", companyPartyId, "fromDate", importTimestamp)));
            }
        }

        if (!UtilValidate.isEmpty(entry.getString("didNumber"))) {
            // associate this as PHONE_DID
            GenericValue contactMech = delegator.makeValue("ContactMech", UtilMisc.toMap("contactMechId", delegator.getNextSeqId("ContactMech"), "contactMechTypeId", "TELECOM_NUMBER"));
            GenericValue didNumber = UtilImport.makeTelecomNumber(contactMech, entry.getString("didCountryCode"), entry.getString("didAreaCode"), entry.getString("didNumber"), delegator);
            toBeStored.add(contactMech);
            toBeStored.add(didNumber);
            if (personPartyId != null) {
                toBeStored.add(UtilImport.makeContactMechPurpose("PHONE_DID", didNumber, personPartyId, importTimestamp, delegator));
                toBeStored.add(delegator.makeValue("PartyContactMech", UtilMisc.toMap("contactMechId", contactMech.get("contactMechId"), "partyId", personPartyId, "fromDate", importTimestamp, "extension", entry.getString("didExtension"))));
            }
            if (companyPartyId != null) {
                toBeStored.add(UtilImport.makeContactMechPurpose("PHONE_DID", didNumber, companyPartyId, importTimestamp, delegator));
                toBeStored.add(delegator.makeValue("PartyContactMech", UtilMisc.toMap("contactMechId", contactMech.get("contactMechId"), "partyId", companyPartyId, "fromDate", importTimestamp, "extension", entry.getString("didExtension"))));
            }
        }

        if (!UtilValidate.isEmpty(entry.getString("emailAddress"))) {
            // make the email address
            GenericValue emailContactMech = delegator.makeValue("ContactMech", UtilMisc.toMap("contactMechId", delegator.getNextSeqId("ContactMech"), "contactMechTypeId", "EMAIL_ADDRESS", "infoString", entry.getString("emailAddress")));
            toBeStored.add(emailContactMech);
            if (personPartyId != null) {
                toBeStored.add(delegator.makeValue("PartyContactMech", UtilMisc.toMap("contactMechId", emailContactMech.get("contactMechId"), "partyId", personPartyId, "fromDate", importTimestamp)));
                toBeStored.add(UtilImport.makeContactMechPurpose("PRIMARY_EMAIL", emailContactMech, personPartyId, importTimestamp, delegator));
                toBeStored.add(UtilImport.makePartySupplementalData(partySupplementalData, personPartyId, "primaryEmailId", emailContactMech, delegator));
            }
            if (companyPartyId != null) {
                toBeStored.add(delegator.makeValue("PartyContactMech", UtilMisc.toMap("contactMechId", emailContactMech.get("contactMechId"), "partyId", companyPartyId, "fromDate", importTimestamp)));
                toBeStored.add(UtilImport.makeContactMechPurpose("PRIMARY_EMAIL", emailContactMech, companyPartyId, importTimestamp, delegator));
                toBeStored.add(UtilImport.makePartySupplementalData(partySupplementalData, companyPartyId, "primaryEmailId", emailContactMech, delegator));
            }
        }

        if (!UtilValidate.isEmpty(entry.getString("webAddress"))) {
            // make the web address
            GenericValue webContactMech = delegator.makeValue("ContactMech", UtilMisc.toMap("contactMechId", delegator.getNextSeqId("ContactMech"), "contactMechTypeId", "WEB_ADDRESS", "infoString", entry.getString("webAddress")));
            toBeStored.add(webContactMech);
            if (personPartyId != null) {
                toBeStored.add(delegator.makeValue("PartyContactMech", UtilMisc.toMap("contactMechId", webContactMech.get("contactMechId"), "partyId", personPartyId, "fromDate", importTimestamp)));
                toBeStored.add(UtilImport.makeContactMechPurpose("PRIMARY_WEB_URL", webContactMech, personPartyId, importTimestamp, delegator));
            }
            if (companyPartyId != null) {
                toBeStored.add(delegator.makeValue("PartyContactMech", UtilMisc.toMap("contactMechId", webContactMech.get("contactMechId"), "partyId", companyPartyId, "fromDate", importTimestamp)));
                toBeStored.add(UtilImport.makeContactMechPurpose("PRIMARY_WEB_URL", webContactMech, companyPartyId, importTimestamp, delegator));
            }
        }


        /*****************************/
        /** Import Party notes. **/
        /*****************************/

        if (!UtilValidate.isEmpty(entry.getString("note"))) {
            // make the party note
            if (personPartyId != null) {
                GenericValue noteData = delegator.makeValue("NoteData", UtilMisc.toMap("noteId", delegator.getNextSeqId("NoteData"), "noteInfo", entry.getString("note"), "noteParty", initialResponsiblePartyId, "noteDateTime", importTimestamp));
                toBeStored.add(noteData);
                toBeStored.add(delegator.makeValue("PartyNote", UtilMisc.toMap("noteId", noteData.get("noteId"), "partyId", personPartyId)));
            }
            if (companyPartyId != null) {
                GenericValue noteData = delegator.makeValue("NoteData", UtilMisc.toMap("noteId", delegator.getNextSeqId("NoteData"), "noteInfo", entry.getString("note"), "noteParty", initialResponsiblePartyId, "noteDateTime", importTimestamp));
                toBeStored.add(noteData);
                toBeStored.add(delegator.makeValue("PartyNote", UtilMisc.toMap("noteId", noteData.get("noteId"), "partyId", companyPartyId)));
            }
        }


        /*****************************/
        /** Import Pricing data. **/
        /*****************************/

        if (!UtilValidate.isEmpty(entry.getString("discount"))) {
            BigDecimal discount = entry.getBigDecimal("discount").abs().negate();
            discount = discount.movePointRight(2);
            // Apply price rule only to company
            if (companyPartyId != null) {
                // productPriceRule
                String productPriceRuleId = delegator.getNextSeqId("ProductPriceRule");
                String priceRuleName = "Imported rule for ";
                priceRuleName += UtilValidate.isEmpty(entry.get("companyName")) ? "partyId: " + companyPartyId : entry.getString("companyName");
                toBeStored.add(delegator.makeValue("ProductPriceRule", UtilMisc.toMap("productPriceRuleId", productPriceRuleId, "ruleName", priceRuleName, "isSale", "N", "fromDate", importTimestamp)));
                // productPriceCond
                toBeStored.add(delegator.makeValue("ProductPriceCond", UtilMisc.toMap("productPriceRuleId", productPriceRuleId, "productPriceCondSeqId", UtilFormatOut.formatPaddedNumber(1, 2), "inputParamEnumId", "PRIP_PARTY_ID", "operatorEnumId", "PRC_EQ", "condValue", companyPartyId)));
                // productPriceAction
                toBeStored.add(delegator.makeValue("ProductPriceAction", UtilMisc.toMap("productPriceRuleId", productPriceRuleId, "productPriceActionSeqId", UtilFormatOut.formatPaddedNumber(1, 2), "productPriceActionTypeId", "PRICE_POL", "amount", discount)));
            }
        }


        /***********************************/
        /** Import Party Classifications. **/
        /***********************************/

        if (!UtilValidate.isEmpty(entry.getString("partyClassificationTypeId"))) {
            // Apply classification only to partyGroup
            if (companyPartyId != null) {
                String partyClassificationTypeId = entry.getString("partyClassificationTypeId");
                if (delegator.findByPrimaryKey("PartyClassificationType", UtilMisc.toMap("partyClassificationTypeId", partyClassificationTypeId)) != null) {
                    GenericValue partyClassificationGroup = EntityUtil.getFirst(delegator.findByAnd("PartyClassificationGroup", UtilMisc.toMap("partyClassificationTypeId", partyClassificationTypeId)));
                    if (partyClassificationGroup != null) {
                        toBeStored.add(delegator.makeValue("PartyClassification", UtilMisc.toMap("partyId", companyPartyId, "partyClassificationGroupId", partyClassificationGroup.getString("partyClassificationGroupId"), "fromDate", importTimestamp)));
                    } else {
                        Debug.logInfo("No partyClassificationGroups exist for partyClassificationId" + partyClassificationTypeId + ", ignoring for customerId " + entry.getString("customerId"), MODULE);
                    }
                } else {
                    Debug.logInfo("partyClassificationTypeId" + partyClassificationTypeId + "does not exist, ignoring for customerId " + entry.getString("customerId"), MODULE);
                }
            }
        }

        // associate party with DONOTSHIP_CUSTOMERS classification group if disableShipping is set.
        String disableShipping = entry.getString("disableShipping");
        if (UtilValidate.isNotEmpty(disableShipping) && "Y".equals(disableShipping)) {
            Map<String, Object> partyClassification = null;
            if (UtilValidate.isNotEmpty(companyPartyId)) {
                partyClassification = UtilMisc.toMap("partyId", companyPartyId, "partyClassificationGroupId", "DONOTSHIP_CUSTOMERS", "fromDate", importTimestamp);
                toBeStored.add(delegator.makeValue("PartyClassification", partyClassification));
            }
            if (UtilValidate.isNotEmpty(personPartyId)) {
                partyClassification = UtilMisc.toMap("partyId", personPartyId, "partyClassificationGroupId", "DONOTSHIP_CUSTOMERS", "fromDate", importTimestamp);
                toBeStored.add(delegator.makeValue("PartyClassification", partyClassification));
            }
        }

        /*****************************/
        /** Import Accounting data. **/
        /*****************************/


        if (!UtilValidate.isEmpty(entry.getString("creditCardNumber"))) {
            // we need a person with a first and last name, otherwise the import data is malformed
            if (personPartyId == null && UtilValidate.isEmpty(entry.getString("firstName")) && UtilValidate.isEmpty(entry.getString("lastName"))) {
                Debug.logWarning("Failed to import Credit Card for Party ["+primaryPartyId+"]:  First and Last name missing for customer ["+entry.get("customerId")+"].", MODULE);
            } else {
                // associate this with primaryPartyId as a PaymentMethod of CREDIT_CARD type
                GenericValue paymentMethod = delegator.makeValue("PaymentMethod", UtilMisc.toMap("paymentMethodId", delegator.getNextSeqId("PaymentMethod"), "paymentMethodTypeId", "CREDIT_CARD", "partyId", primaryPartyId, "fromDate", importTimestamp));
                toBeStored.add(paymentMethod);

                // translate the credit card data into a form acceptable to CreditCard
                String cardNumber = UtilValidate.stripCharsInBag(entry.getString("creditCardNumber"), UtilValidate.creditCardDelimiters);
                String cardType = UtilValidate.getCardType(cardNumber);
                String expireDate = UtilImport.decodeExpireDate(entry.getString("creditCardExpDate"));
                if (expireDate == null) {
                    Debug.logWarning("Failed to decode creditCardExpDate ["+entry.getString("creditCardExpDate")+"] into form MM/YYYY for customer ["+entry.get("customerId")+"].", MODULE);
                } else {
                    Map<String, Object> input = UtilMisc.<String, Object>toMap("paymentMethodId", paymentMethod.get("paymentMethodId"), "cardNumber", cardNumber, "cardType", cardType, "expireDate", expireDate);
                    input.put("firstNameOnCard", entry.get("firstName"));
                    input.put("lastNameOnCard", entry.get("lastName"));
                    input.put("companyNameOnCard", entry.get("companyName"));
                    input.put("contactMechId", billingContactMechId);
                    toBeStored.add(delegator.makeValue("CreditCard", input));
                }
            }
        }

        toBeStored.addAll(createBalances(primaryPartyId, entry.getBigDecimal("outstandingBalance"), importTimestamp, baseCurrencyUomId, delegator));
        toBeStored.addAll(createSalesAgreement(entry, primaryPartyId, primaryPartyName, importTimestamp, delegator));

        // save the primary party Id
        entry.put("primaryPartyId", primaryPartyId);
        toBeStored.add(entry);

        return toBeStored;
    }

    /**
     * Checks if we can create a balance.  The balance from the entry must be non zero and the
     * AR and offsetting accounts must exist.
     */
    public boolean canCreateBalance(BigDecimal balance) {
        if (balance == null || balance.signum() == 0) {
            return false;
        }
        return (! UtilValidate.isEmpty(arGlAccountId)) && (! UtilValidate.isEmpty(offsettingGlAccountId));
    }

    /**
     * Creates AR balances if a balance exists and the accounts are specified.
     * @return List containing the balance entities or an empty list if no balances are to be created.
     */
    public List<GenericValue> createBalances(String partyId, BigDecimal balance, Timestamp importTimestamp, String currencyUomId, Delegator delegator) {
        List<GenericValue> toBeStored = new FastList<GenericValue>();
        if (! canCreateBalance(balance)) return toBeStored;

        // create an AcctgTrans, DR arGlAccountId, CR offsettingGlAccountId for the amount of outstandingBalance
        Map<String, Object> input = UtilMisc.toMap("acctgTransTypeId", "INTERNAL_ACCTG_TRANS", "glFiscalTypeId", "ACTUAL",
                                   "transactionDate", importTimestamp, "partyId", partyId);
        input.put("acctgTransId", delegator.getNextSeqId("AcctgTrans"));
        input.put("isPosted", "N");
        input.put("createdByUserLogin", userLogin.get("userLoginId"));
        input.put("lastModifiedByUserLogin", userLogin.get("userLoginId"));
        GenericValue acctgTrans = delegator.makeValue("AcctgTrans", input);
        toBeStored.add(acctgTrans);

        // acctg trans entry input data for both DR and CR
        Map<String, Object> acctgTransEntryInput = FastMap.newInstance();
        acctgTransEntryInput.put("acctgTransId", acctgTrans.get("acctgTransId"));
        acctgTransEntryInput.put("amount", balance);
        acctgTransEntryInput.put("partyId", partyId);
        acctgTransEntryInput.put("organizationPartyId", organizationPartyId);
        acctgTransEntryInput.put("currencyUomId", currencyUomId);
        acctgTransEntryInput.put("reconcileStatusId", "AES_NOT_RECONCILED");

        // DR arGlAccountId
        acctgTransEntryInput.put("acctgTransEntrySeqId", UtilFormatOut.formatPaddedNumber(1, 6));
        acctgTransEntryInput.put("glAccountId", arGlAccountId);
        acctgTransEntryInput.put("debitCreditFlag", "D");
        toBeStored.add(delegator.makeValue("AcctgTransEntry", acctgTransEntryInput));

        // CR offsettingGlAccountId
        acctgTransEntryInput.put("acctgTransEntrySeqId", UtilFormatOut.formatPaddedNumber(2, 6));
        acctgTransEntryInput.put("glAccountId", offsettingGlAccountId);
        acctgTransEntryInput.put("debitCreditFlag", "C");
        toBeStored.add(delegator.makeValue("AcctgTransEntry", acctgTransEntryInput));

        return toBeStored;
    }

    /**
     * Whether we should create a sales agreement for this record.  Overload if the details vary.
     * In the case of vanilla importCustomers, an agreement is created for a credit limit, a net
     * payment days term, or both.
     */
    public boolean canCreateSalesAgreement(GenericValue entry) {
        BigDecimal creditLimit = entry.getBigDecimal("creditLimit");
        Long netPaymentDays = entry.getLong("netPaymentDays");

        // make the logic simpler by normalizing null to 0
        if (creditLimit == null) {
            creditLimit = BigDecimal.ZERO;
        }
        if (netPaymentDays == null) netPaymentDays = 0L;

        return (creditLimit.signum() > 0 || netPaymentDays > 0);
    }

    /**
     * Create the sales agreement and terms between the given partyId (with a partyName) and the organization.
     * Entry point which should be called from decode() method.  To customize the way agreements are generated
     * due to field and data differences, overload canCreateSalesAgreement() and createSalesAgreementTerms().
     */
    public List<GenericValue> createSalesAgreement(GenericValue entry, String partyId, String partyName, Timestamp importTimestamp, Delegator delegator) throws GenericEntityException {
        List<GenericValue> toBeStored = new FastList<GenericValue>();
        if (! canCreateSalesAgreement(entry)) return toBeStored;

        String agreementId = delegator.getNextSeqId("Agreement");

        GenericValue agreement = delegator.makeValue("Agreement");
        agreement.put("agreementId", agreementId);
        agreement.put("partyIdFrom", organizationPartyId);
        agreement.put("partyIdTo", partyId);
        agreement.put("agreementTypeId", "SALES_AGREEMENT");
        agreement.put("agreementDate", importTimestamp);
        agreement.put("fromDate", importTimestamp);
        agreement.put("statusId", "AGR_ACTIVE");
        agreement.put("description", "Sales agreement" + (GenericValidator.isBlankOrNull(partyName) ? "" : " for ") + partyName);
        toBeStored.add(agreement);

        toBeStored.addAll(createSalesAgreementTerms(entry, agreementId, delegator));
        return toBeStored;
    }

    /**
     * Invoked from createSalesAgreement(), generates the terms of the agreement.  Overload if details vary.
     * In the case of vanilla importCustomers, it will generate a credit limit term if the creditLimit field is positive,
     * and a net payment days term if the netPaymentDays field is positive.
     */
    public List<GenericValue> createSalesAgreementTerms(GenericValue entry, String agreementId, Delegator delegator) throws GenericEntityException {
        List<GenericValue> toBeStored = new FastList<GenericValue>();

        BigDecimal creditLimit = entry.getBigDecimal("creditLimit");
        Long netPaymentDays = entry.getLong("netPaymentDays");
        String customerCurrencyUomId = entry.getString("currencyUomId");
        int seqId = 1;

        toBeStored.addAll(createAgreementCreditLimitTerm(agreementId, customerCurrencyUomId, seqId++, delegator, creditLimit));
        toBeStored.addAll(createAgreementNetPaymentDaysTerm(agreementId, customerCurrencyUomId, seqId++, delegator, netPaymentDays));

        return toBeStored;
    }

    /**
     * Simplifies the creation of a term/item combination.  Specify the agreement type, term type, term value, term days and currency.
     * You might want to use one of the more specific methods such as createAgreementCreditLimitTerm() to minimize errors.
     */
    public List<GenericValue> createAgreementTerm(String agreementId, String agreementTypeId, String termTypeId, BigDecimal termValue, Long termDays, String currencyUomId, int seqId, Delegator delegator) {
        List<GenericValue> toBeStored = new FastList<GenericValue>();

        GenericValue item = delegator.makeValue("AgreementItem");
        item.put("agreementId", agreementId);
        item.put("agreementItemSeqId", Integer.valueOf(seqId).toString());
        item.put("agreementItemTypeId", agreementTypeId);
        toBeStored.add(item);

        GenericValue term = delegator.makeValue("AgreementTerm");
        term.put("agreementTermId", delegator.getNextSeqId("AgreementTerm"));
        term.put("agreementId", agreementId);
        term.put("termTypeId", termTypeId);
        term.put("agreementItemSeqId", Integer.valueOf(seqId).toString());
        term.put("termValue", termValue);
        term.put("termDays", termDays);
        term.put("currencyUomId", currencyUomId);
        toBeStored.add(term);

        return toBeStored;
    }

    /**
     * Helper function to generate a credit limit term.  Only creates term if the credit limit is positive.
     * Used by createSalesAgreementTerms().
     */
    public List<GenericValue> createAgreementCreditLimitTerm(String agreementId, String customerCurrencyUomId, int seqId, Delegator delegator, BigDecimal creditLimit) {
        // get currency for customer record or from opentaps.properties
        // TODO why not just throw an illegal argument exception and have the importer fix the data?
        if (UtilValidate.isEmpty(customerCurrencyUomId)) {
            customerCurrencyUomId = UtilProperties.getPropertyValue("opentaps", "defaultCurrencyUomId");
            Debug.logWarning("No currency specified for credit limit of agreement [" + agreementId + "], using [" + customerCurrencyUomId + "] from opentaps.properties", MODULE);
        }
        if (creditLimit != null && creditLimit.signum() > 0) {
            return createAgreementTerm(agreementId, "AGREEMENT_CREDIT", "CREDIT_LIMIT", creditLimit, null, customerCurrencyUomId, seqId, delegator);
        }
        return new FastList<GenericValue>();
    }

    /**
     * Helper function to generate a net payment days term.  Only creates term if therre are a positive number of days.
     * Used by createSalesAgreementTerms().
     */
    public List<GenericValue> createAgreementNetPaymentDaysTerm(String agreementId, String customerCurrencyUomId, int seqId, Delegator delegator, Long netPaymentDays) {
        if (netPaymentDays != null && netPaymentDays > 0) {
            return createAgreementTerm(agreementId, "AGREEMENT_PAYMENT", "FIN_PAYMENT_TERM", null, netPaymentDays, customerCurrencyUomId, seqId, delegator);
        }
        return new FastList<GenericValue>();
    }


    /**
     * Helper function to generate a percentage discount term.  Make sure that the percentage is represented as a decimal number
     * and not a whole number.
     * Used by createSalesAgreementTerms().
     * TODO: This term isn't really used anywhere.
     * TODO: In validating discount rate, throw illegal argument if it's not valid
     */
    public List<GenericValue> createAgreementDiscountTerm(String agreementId, String customerCurrencyUomId, int seqId, Delegator delegator, BigDecimal discountRate, Long discountDays) {
        List<GenericValue> toBeStored = new FastList<GenericValue>();
        if (discountRate != null && discountRate.signum() > 0) {
            return createAgreementTerm(agreementId, "AGREEMENT_PAYMENT", "FIN_PAYMENT_DISC", discountRate, discountDays, customerCurrencyUomId, seqId, delegator);
        }
        return toBeStored;
    }

}
