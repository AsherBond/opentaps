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
package org.opentaps.dataimport.netsuite;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;
import org.opentaps.dataimport.CustomerDecoder;
import org.opentaps.domain.party.PhoneNumber;

/**
 * Transforms NetSuiteCustomer rows into opentaps model.
 */
public class NetSuiteCustomerDecoder extends CustomerDecoder {

    // three minute timeout for servcies
    public static final int serviceTimeout = 180;

    // Default tax authority if the customer is taxable
    protected String defaultTaxAuthGeoId = null;
    protected List<GenericValue> defaultTaxParties = null;

    /**
     * Same concept as CustomerDecoder(Map context)
     */
    public NetSuiteCustomerDecoder(Map context, Delegator delegator) throws GeneralException {
        super(context);
        this.defaultTaxAuthGeoId = (String) context.get("defaultTaxAuthGeoId");

        // verify the tax auth geo exists and has some parties
        defaultTaxParties = delegator.findByAnd("TaxAuthority", UtilMisc.toMap("taxAuthGeoId", defaultTaxAuthGeoId));
        if (defaultTaxParties.size() == 0) {
            throw new GeneralException("No tax authorities found for defaultTaxAuthGeoId ["+defaultTaxAuthGeoId+"].  Aborting import.");
        }
    }

    /**
     * Each NetSuiteCustomer record corresponds to either a contact (person with first and last names), an account (company),
     * or a contact *AND* an account.  Furthermore, there is an account hierarchy represented via the parentId field, but
     * this is ignored for the time being.
     *
     * The only issue with this method is that if a company is present in multiple customer records, it must have the same
     * exact name.  If the name differs, say by a typo or an abbreviation, then this import routine will not be able to
     * detect it.  Two accounts for the same company will be created and it is left to the users to merge the duplicates
     * together.
     */
    public List<GenericValue> decode(GenericValue entry, Timestamp importTimestamp, Delegator delegator, LocalDispatcher dispatcher, Object... args) throws Exception {
        List<GenericValue> toBeStored = new FastList<GenericValue>();
        String partyId = getPartyIdFromNetSuiteCustomerId(entry.getString("customerId"));
        boolean isContact = isContact(entry);

        // detect the presence of a company name
        String accountName = entry.getString("companyName");
        if (! isContact && accountName == null) {
            // in some cases the company name is stored in customerName, but only if it is not a contact
            accountName = entry.getString("customerName");
        }

        // contacts that also have accounts need to look up the account and associate it (or create a new one)
        boolean hasAccount = isContact && accountName != null;

        // create input map for either crmsfa.createContact or crmsfa.createAccount
        Map input = UtilMisc.toMap("partyId", partyId, "userLogin", userLogin);
        if (isContact) {
            input.put("firstName", entry.get("firstName"));
            input.put("lastName", entry.get("lastName"));
        } else {
            if (accountName == null) {
                throw new IllegalArgumentException("Customer ["+partyId+"] is a company but no company name can be found in the companyName or customerName fields.");
            }
            input.put("accountName", accountName);
        }

        // if we're also creating an account, check if it exists first or create it right now (without the contact information)
        String accountPartyId = null;
        if (hasAccount) {
            GenericValue account = EntityUtil.getFirst( delegator.findByAnd("PartyGroup", UtilMisc.toMap("groupName", accountName)) );
            if (account != null) {
                accountPartyId = account.getString("partyId");
            } else {
                Map results = dispatcher.runSync("crmsfa.createAccount", UtilMisc.toMap("userLogin", userLogin, "accountName", accountName), serviceTimeout, false);
                if (ServiceUtil.isError(results)) {
                    throw new GeneralException(ServiceUtil.getErrorMessage(results));
                }
                accountPartyId = (String) results.get("partyId");
            }
            
            // now we add it as the initial account for the contact, so it is automatically associated
            input.put("accountPartyId", accountPartyId);
        }

        // add some more data to the input map
        input.put("primaryEmail", entry.getString("email"));

        // add the primary phone to the input map
        PhoneNumber primaryPhone = entry.get("phone") != null ? new PhoneNumber(entry.getString("phone")) : null;
        if (primaryPhone != null) {
            input.put("primaryPhoneCountryCode", primaryPhone.getCountryCode());
            input.put("primaryPhoneAreaCode", primaryPhone.getAreaCode());
            input.put("primaryPhoneNumber", primaryPhone.getContactNumber());
        }

        // create the contact or account (this is within the transaction, so if there are future problems then this will be rolled back)
        Map results = dispatcher.runSync(isContact ? "crmsfa.createContact" : "crmsfa.createAccount", input, serviceTimeout, false);
        if (ServiceUtil.isError(results)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(results));
        }

        // if we created an account, then make sure it's set (note that if an account + contact is created, this doesn't happen and the accountPartyId comes from a few stanzas up)
        if (! isContact) accountPartyId = (String) results.get("partyId");

        // handle the rest of the phone numbers
        createPhoneNumber(partyId, "PHONE_HOME", entry.getString("homePhone"), dispatcher);
        createPhoneNumber(partyId, "PHONE_MOBILE", entry.getString("mobilePhone"), dispatcher);
        createPhoneNumber(partyId, "PRIMARY_PHONE", entry.getString("altphone"), dispatcher); // map altphone onto another primary number
        createPhoneNumber(partyId, "FAX_NUMBER", entry.getString("fax"), dispatcher);

        // create the altemail as OTHER_EMAIL
        if (entry.get("altemail") != null) {
            input = UtilMisc.toMap("partyId", partyId, "userLogin", userLogin, "contactMechPurposeTypeId", "OTHER_EMAIL", "emailAddress", entry.get("altemail"));
            results = dispatcher.runSync("createPartyEmailAddress", input, serviceTimeout, false);
            if (ServiceUtil.isError(results)) {
              throw new GeneralException(ServiceUtil.getErrorMessage(results));
            }
        }

        // get the party supplemental data, we'll need to update some fields
        GenericValue supplemental = delegator.findByPrimaryKey("PartySupplementalData", UtilMisc.toMap("partyId", partyId));

        // set the industry for the party based on what should have been imported already
        if (entry.get("customerTypeId") != null) {
            GenericValue customerType = delegator.findByPrimaryKeyCache("NetSuiteCustomerType", UtilMisc.toMap("customerTypeId", entry.get("customerTypeId")));
            if (customerType == null || customerType.get("enumId") == null) {
                throw new IllegalArgumentException("Customer ["+partyId+"] can't be imported because its type ["+entry.get("customerTypeId")+"] has not been impoted yet.");
            }
            supplemental.put("industryEnumId", customerType.get("enumId"));
        }

        // set the currency for customer
        String currencyUomId = getCurrencyUomId(entry);
        supplemental.put("currencyUomId", currencyUomId);

        // create the AR balances
        toBeStored.addAll(createBalances(partyId, entry.getBigDecimal("openbalance"), importTimestamp, currencyUomId, delegator));

        // make the customer tax exempt in the customer's state using resalenumber as the tax ID
        // TODO: make the geo lookup more robust, such as the one in the address decoder (state might be in Canada or Australia for instance)
        String resaleNumber = entry.getString("resalenumber");
        String state = entry.getString("stateProvince");
        if (resaleNumber != null) {
            if (resaleNumber.length() > 20) throw new IllegalArgumentException("Customer ["+partyId+"] has resale number longer than 20 characters.  Cannot fit this into tax information.");
            if (state == null) throw new IllegalArgumentException("Cannot create tax info:  Customer ["+partyId+"] is tax exempt, but the customer's state is unknown.");
            List<GenericValue> taxAuths = delegator.findByAnd("TaxAuthority", UtilMisc.toMap("taxAuthGeoId", state.trim()));
            for (GenericValue taxAuth : taxAuths) {
                GenericValue taxInfo = delegator.makeValue("PartyTaxAuthInfo");
                taxInfo.put("partyId", partyId);
                taxInfo.put("taxAuthGeoId", taxAuth.get("taxAuthGeoId"));
                taxInfo.put("taxAuthPartyId", taxAuth.get("taxAuthPartyId"));
                taxInfo.put("fromDate", importTimestamp);
                taxInfo.put("partyTaxId", resaleNumber);
                taxInfo.put("isExempt", "Y");
                toBeStored.add(taxInfo);
            }
        }

        // make the customer tax exempt in the default tax authorities TODO: make the state the actual geoId when the above TODO is fixed
        if ("No".equals(entry.get("istaxable")) && (state == null || state != null && !defaultTaxAuthGeoId.equals(state))) {
           for (GenericValue taxAuth : defaultTaxParties) {
                GenericValue taxInfo = delegator.makeValue("PartyTaxAuthInfo");
                taxInfo.put("partyId", partyId);
                taxInfo.put("taxAuthGeoId", defaultTaxAuthGeoId);
                taxInfo.put("taxAuthPartyId", taxAuth.get("taxAuthPartyId"));
                taxInfo.put("fromDate", importTimestamp);
                taxInfo.put("isExempt", "Y");
                toBeStored.add(taxInfo);
            }
        }

        /* TODO we're not doing this yet, the parent data is convoluted
        String parentPartyId = entry.getString("parentId");
        if (parentPartyId != null) {
            parentPartyId = getPartyIdFromNetSuiteCustomerId(parentPartyId);
            String parentRoleId = PartyHelper.getFirstValidInternalPartyRoleTypeId(parentPartyId, delegator);
            if (parentRoleId == null) {
                // if no CRMSFA role, then we know it has not been imported yet
                throw new IllegalArgumentException("Parent customer has not been imported yet.  Parent ID is ["+entry.getString("parentId")+"].");
            }
            if (isContact && "ACCOUNT".equals(parentRoleId)) {
                // create the party relationship between the Contact and the Account
                PartyHelper.createNewPartyToRelationship(parentPartyId, partyId, "CONTACT", "CONTACT_REL_INV",
                    null, UtilMisc.toList("ACCOUNT"), false, userLogin, delegator, dispatcher);
            } else {
                // otherwise we are an account, so we just need to set the parent party
                supplemental.put("parentPartyId", parentPartyId);
            }
        }
        */

        // we'll need to figure out what the name is for the sales agreement
        String name = isContact ? entry.getString("firstName") + " " + entry.getString("lastName") : accountName;
        toBeStored.addAll(createSalesAgreement(entry, partyId, name, importTimestamp, delegator));

        // wrap up some necessary steps
        entry.put("contactPartyId", isContact ? partyId : null);
        entry.put("accountPartyId", accountPartyId);
        toBeStored.add(entry);
        toBeStored.add(supplemental);

        return toBeStored;
    }

    /**
     * Determines whether an entry is a contact using the following heuristic:
     *
     * If the record has a first name and last name, then the entry is probably a Contact,
     * but this is not guaranteed since some bad data can put the company name in these fields.
     * We then look at the isPerson field and if it's Yes, then we assume it's a contact.
     */
    public boolean isContact(GenericValue entry) {
        return (entry.get("firstName") != null && entry.get("lastName") != null && "Yes".equals(entry.get("isPerson")));
    }

    public void createPhoneNumber(String partyId, String purposeId, String number, LocalDispatcher dispatcher) throws GeneralException {
        if (number == null) return;
        PhoneNumber phone = new PhoneNumber(number);
        Map input = UtilMisc.toMap("partyId", partyId, "contactMechPurposeTypeId", purposeId, "userLogin", userLogin);
        input.put("countryCode", phone.getCountryCode());
        input.put("areaCode", phone.getAreaCode());
        input.put("contactNumber", phone.getContactNumber());
        Map results = dispatcher.runSync("createPartyTelecomNumber", input, serviceTimeout, false);
        if (ServiceUtil.isError(results)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(results));
        }
    }

    // TODO more currency
    public String getCurrencyUomId(GenericValue entry) {
        String currencyId = entry.getString("currencyId");
        if ("1".equals(currencyId)) return "USD";
        if ("3".equals(currencyId)) return "CAD";
        return "USD";
    }

    /** In the case of netsuite, we can have a credit limit, payment terms or both. TODO: what about is inactive field? */
    public boolean canCreateSalesAgreement(GenericValue entry) {
        Double creditLimit = entry.getDouble("creditlimit");
        if (creditLimit == null) creditLimit = 0.0;
        String paymentTermsId = entry.getString("paymentTermsId");
        return (creditLimit > 0.0 || paymentTermsId != null);
    }

    /** We'll have to overload this and set up the terms according to our NetSuitePaymentTerm entity and the NetSuiteCustomer.creditlimit field. */
    public List<GenericValue> createSalesAgreementTerms(GenericValue entry, String agreementId, Delegator delegator) throws GenericEntityException {
        List<GenericValue> toBeStored = new FastList<GenericValue>();
        int seqId = 1;
        String customerCurrencyUomId = getCurrencyUomId(entry);

        // create a credit limit, if it exists
        BigDecimal creditLimit = entry.getBigDecimal("creditlimit");
        toBeStored.addAll(createAgreementCreditLimitTerm(agreementId, customerCurrencyUomId, seqId++, delegator, creditLimit));

        // if we have payment terms for the customer, convert the discount terms and the net payment days term
        String paymentTermsId = entry.getString("paymentTermsId");
        if (paymentTermsId != null) {
            GenericValue paymentTerms = delegator.findByPrimaryKeyCache("NetSuitePaymentTerm", UtilMisc.toMap("paymentTermsId", paymentTermsId));
            if (paymentTerms == null) {
                throw new IllegalArgumentException("Could not find payment terms ["+paymentTermsId+"] for customer ["+entry.get("customerId")+"].  Perhaps it was not imported yet?");
            }
            Long netPaymentDays = paymentTerms.getLong("daysUntilDue");
            Long discountDays = paymentTerms.getLong("discountDays");
            BigDecimal discountRate = paymentTerms.getBigDecimal("percentageDiscount");

            // use the same seqId for both, that way the two terms are part of the same agreement item
            toBeStored.addAll(createAgreementNetPaymentDaysTerm(agreementId, customerCurrencyUomId, seqId, delegator, netPaymentDays));
            toBeStored.addAll(createAgreementDiscountTerm(agreementId, customerCurrencyUomId, seqId, delegator, discountRate, discountDays));
        }

        return toBeStored;
    }

    /** Append NS to the customer Id for use in opentaps. */
    public static String getPartyIdFromNetSuiteCustomerId(String customerId) {
        if (customerId == null || customerId.trim().length() == 0) return null;
        return "NS" + customerId.trim();
    }
    
}
