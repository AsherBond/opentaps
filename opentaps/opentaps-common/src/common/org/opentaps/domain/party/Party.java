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
package org.opentaps.domain.party;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.opentaps.base.constants.RoleTypeConstants;
import org.opentaps.base.entities.ContactMech;
import org.opentaps.base.entities.PartyClassification;
import org.opentaps.base.entities.PartyContactMech;
import org.opentaps.base.entities.PartyContactMechPurpose;
import org.opentaps.base.entities.PartyNoteView;
import org.opentaps.base.entities.PartyRole;
import org.opentaps.base.entities.PartySummaryCRMView;
import org.opentaps.base.entities.PostalAddress;
import org.opentaps.base.entities.TelecomNumber;
import org.opentaps.domain.DomainsDirectory;
import org.opentaps.domain.billing.payment.PaymentMethod;
import org.opentaps.foundation.entity.EntityNotFoundException;
import org.opentaps.foundation.repository.RepositoryException;

/**
 * Party entity and domain.
 */
public class Party extends org.opentaps.base.entities.Party {

    private PartySummaryCRMView completeView;
    private List<PaymentMethod> paymentMethods;
    private List<PostalAddress> shippingAddresses;
    private List<PostalAddress> billingAddresses;
    private List<TelecomNumber> phoneNumbers;
    private List<ContactMech> emailAddresses;

    /** Enumeration of contact purposes. */
    public enum ContactPurpose {
        PRIMARY_ADDRESS,
        GENERAL_ADDRESS,
        BILLING_ADDRESS,
        SHIPPING_ADDRESS,
        PRIMARY_PHONE,
        FAX_NUMBER,
        BILLING_PHONE,
        PRIMARY_EMAIL
    }

    /**
     * Default constructor.
     */
    public Party() {
        super();
    }

    /** {@inheritDoc} */
    @Override
    protected void postInit() {

        try {
            completeView = getRepository().getPartySummaryCRMView(getPartyId());
        } catch (RepositoryException e) {
            Debug.logError(e.getMessage(), getClass().getName());
        } catch (EntityNotFoundException e) {
            Debug.logError(e.getMessage(), getClass().getName());
            completeView = null;
        }

    }

    /**
     * Checks if this party is an account.
     * @return a <code>Boolean</code> value
     * @throws RepositoryException if an error occurs
     */
    public Boolean isAccount() throws RepositoryException {
        for (PartyRole role : getPartyRoles()) {
            if ("ACCOUNT".equals(role.getRoleTypeId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if this party is an sub account of another account.
     * @param account
     * @return a <code>Boolean</code> value
     * @throws RepositoryException if an error occurs
     */
    public Boolean isSubAccount(Account account) throws RepositoryException {
        // a party is the sub account of another party if its PartySupplementalData.parentPartyId is the partyId of the other party
        // note that if account is not null, then it should be an ACCOUNT in the system, so we do not need to check that again here
        if ((completeView != null) && (completeView.getParentPartyId() != null) && (account != null) && (completeView.getParentPartyId().equals(account.getPartyId()))) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks if this party has the given role.
     * @param roleTypeId a <code>String</code> value
     * @return a <code>Boolean</code> value
     * @exception RepositoryException if an error occurs
     */
    public Boolean hasRole(String roleTypeId) throws RepositoryException {
        for (PartyRole role : getPartyRoles()) {
            if (roleTypeId.equals(role.getRoleTypeId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if this party is a contact.
     * @return a <code>Boolean</code> value
     * @throws RepositoryException if an error occurs
     */
    public Boolean isContact() throws RepositoryException {
        return hasRole(RoleTypeConstants.CONTACT);
    }

    /**
     * Checks if this party is a lead (or prospect).
     * @return a <code>Boolean</code> value
     * @throws RepositoryException if an error occurs
     */
    public Boolean isLead() throws RepositoryException {
        return hasRole(RoleTypeConstants.PROSPECT);
    }

    /**
     * Checks if this party is a partner.
     * @return a <code>Boolean</code> value
     * @throws RepositoryException if an error occurs
     */
    public Boolean isPartner() throws RepositoryException {
        return hasRole(RoleTypeConstants.PARTNER);
    }

    /**
     * Checks if this party is a supplier.
     * @return a <code>Boolean</code> value
     * @throws RepositoryException if an error occurs
     */
    public Boolean isSupplier() throws RepositoryException {
        return hasRole(RoleTypeConstants.SUPPLIER);
    }

    /**
     * Gets this party as an <code>Account</code>.
     * @return an <code>Account</code> value, or <code>null</code> if the party is not an account
     * @exception RepositoryException if an error occurs
     * @exception EntityNotFoundException if an error occurs
     */
    public Account asAccount() throws RepositoryException, EntityNotFoundException {
        if (!isAccount()) {
            return null;
        }

        return getRepository().getAccountById(this.getPartyId());
    }

    /**
     * Gets the first name of this party.
     * @return the first name of this party
     */
    public String getFirstName() {
        return completeView.getFirstName();
    }

    /**
     * Gets the last name of this party.
     * @return the last name of this party
     */
    public String getLastName() {
        return completeView.getLastName();
    }

    /**
     * Gets the middle name of this party.
     * @return the middle name of this party
     */
    public String getMiddleName() {
        return completeView.getMiddleName();
    }

    /**
     * Gets the group name of this party; only relevant for a Party Group.
     * @return the group name of this party
     */
    public String getGroupName() {
        return completeView.getGroupName();
    }

    /**
     * Gets the full name of this party.
     * @return the full name of this party
     */
    public String getName() {
        StringBuffer result = new StringBuffer("");

        if (completeView.getFirstName() != null && completeView.getFirstName().length() > 0) {
            result.append(completeView.getFirstName()).append(" ");
        }
        if (completeView.getMiddleName() != null && completeView.getMiddleName().length() > 0) {
            result.append(completeView.getMiddleName()).append(" ");
        }
        if (completeView.getLastName() != null && completeView.getLastName().length() > 0) {
            result.append(completeView.getLastName());
        }

        if (completeView.getGroupName() != null) {
            result.append(completeView.getGroupName());
        }

        return result.toString();
    }

    /**
     * Gets the map of names this party had at the given date.
     * @param date a <code>Timestamp</code>
     * @return  the map of names
     * @throws RepositoryException if an error occurs
     */
    public Map<String, Object> getNameForDate(Timestamp date) throws RepositoryException {
        return getRepository().getPartyNameForDate(this, date);
    }

    /**
     * Gets the Primary Address <code>PostalAddress</code> of this party.
     * @return the Primary <code>PostalAddress</code>
     */
    public PostalAddress getPrimaryAddress() {
        if (completeView.getPrimaryAddress1() == null) {
            return null;
        }
        PostalAddress pa = new PostalAddress();
        pa.setContactMechId(completeView.getPrimaryPostalAddressId());
        pa.setToName(completeView.getPrimaryToName());
        pa.setAttnName(completeView.getPrimaryAttnName());
        pa.setAddress1(completeView.getPrimaryAddress1());
        pa.setAddress2(completeView.getPrimaryAddress2());
        pa.setDirections(completeView.getPrimaryDirections());
        pa.setCity(completeView.getPrimaryCity());
        pa.setPostalCode(completeView.getPrimaryPostalCode());
        pa.setPostalCodeExt(completeView.getPrimaryPostalCodeExt());
        pa.setCountryGeoId(completeView.getPrimaryCountryGeoId());
        pa.setStateProvinceGeoId(completeView.getPrimaryStateProvinceGeoId());
        pa.setCountyGeoId(completeView.getPrimaryCountyGeoId());
        pa.setPostalCodeGeoId(completeView.getPrimaryPostalCodeGeoId());
        return pa;
    }

    /**
     * Gets the URL to the view page of this party.
     * @param externalLoginKey the externalLoginKey that is appended to the URL parameters
     * @return an absolute path URL (eg: <code>/crmsfa/control/viewAccount?partyId=10000</code>
     * @throws RepositoryException if an error occurs
     */
    public String createViewPageURL(String externalLoginKey) throws RepositoryException {
        StringBuffer uri = new StringBuffer();
        if (isAccount()) {
            uri.append("/crmsfa/control/viewAccount?");
        } else if (isContact()) {
            uri.append("/crmsfa/control/viewContact?");
        } else if (isLead()) {
            uri.append("/crmsfa/control/viewLead?");
        } else if (isPartner()) {
            uri.append("/crmsfa/control/viewPartner?");
        } else if (isSupplier()) {
            uri.append("/purchasing/control/viewSupplier?");
        } else {
            uri.append("/partymgr/control/viewprofile?");
        }
        uri.append("partyId=").append(getPartyId());
        return uri.toString();
    }

    /**
     * Gets the HTML link to the view page of this party.
     * @param externalLoginKey the externalLoginKey that is appended to the URL parameters
     * @return an HTML link (eg: <code><a href="/crmsfa/control/viewAccount?partyId=10000">name</a></code>)
     * @throws RepositoryException if an error occurs
     */
    public String createViewPageLink(String externalLoginKey) throws RepositoryException {
        // generate the contents of href=""
        String uri = createViewPageURL(externalLoginKey);
        // generate the display name
        StringBuffer text = new StringBuffer(getName());
        text.append("(").append(getPartyId()).append(")");
        // put everything together
        StringBuffer buff = new StringBuffer("<a class=\"linktext\" href=\"");
        buff.append(uri).append("\">");
        buff.append(text).append("</a>");
        return buff.toString();
    }

    /**
     * Gets the Billing Address of this party.
     * If more than one is defined, returns the oldest record.
     * @return the oldest billing <code>PostalAddress</code>
     * @throws RepositoryException if an error occurs
     */
    public PostalAddress getBillingAddress() throws RepositoryException {
        List<PostalAddress> addresses = getRepository().getPostalAddresses(this, ContactPurpose.BILLING_ADDRESS);
        return (addresses.size() > 0 ? addresses.get(0) : null);
    }

    /**
     * Gets the Primary Email of this party.
     * @return the primary email address
     */
    public String getPrimaryEmail() {
        return completeView.getPrimaryEmail();
    }

    /**
     * Gets the list of currently active Email Addresses <code>ContactMech</code> for this party.
     * @return the list of <code>ContactMech</code>
     * @throws RepositoryException if an error occurs
     */
    public List<ContactMech> getEmailAddresses() throws RepositoryException {
        if (emailAddresses == null) {
            emailAddresses = getRepository().getEmailAddresses(this);
        }
        return emailAddresses;
    }

    /**
     * Gets the list of currently active <code>TelecomNumber</code> for this party.
     * @return the list of <code>TelecomNumber</code>
     * @throws RepositoryException if an error occurs
     */
    public List<TelecomNumber> getPhoneNumbers() throws RepositoryException {
        if (phoneNumbers == null) {
            phoneNumbers = getRepository().getPhoneNumbers(this);
        }
        return phoneNumbers;
    }

    /**
     * Gets the Primary Phone <code>TelecomNumber</code> of this party.
     * If more than one is defined, returns the oldest record.
     * @return the oldest primary <code>TelecomNumber</code>
     * @throws RepositoryException if an error occurs
     */
    public TelecomNumber getPrimaryPhone() throws RepositoryException {
        List<TelecomNumber> numbers = getRepository().getPhoneNumbers(this, ContactPurpose.PRIMARY_PHONE);
        return (numbers.size() > 0 ? numbers.get(0) : null);
    }

    /**
     * Gets the Fax Number of this party.
     * If more than one is defined, returns the oldest record.
     * @return the oldest fax <code>TelecomNumber</code>
     * @throws RepositoryException if an error occurs
     */
    public TelecomNumber getFaxNumber() throws RepositoryException {
        List<TelecomNumber> numbers = getRepository().getPhoneNumbers(this, ContactPurpose.FAX_NUMBER);
        return (numbers.size() > 0 ? numbers.get(0) : null);
    }

    /**
     * Gets the <code>PartySummaryCRMView</code> of this party.
     * @return the <code>PartySummaryCRMView</code>
     */
    public PartySummaryCRMView getCompleteView() {
        return completeView;
    }

    /**
     * Gets the list of <code>PostalAddress</code> of this party that can be used as shipping address.
     * @return the list of shipping <code>PostalAddress</code>
     * @throws RepositoryException if an error occurs
     */
    public List<PostalAddress> getShippingAddresses() throws RepositoryException {
        if (shippingAddresses == null) {
            shippingAddresses = getRepository().getPostalAddresses(this, ContactPurpose.SHIPPING_ADDRESS);
        }
        return shippingAddresses;
    }

    /**
     * Gets the list of <code>PostalAddress</code> of this party that can be used as billing address.
     * @return the list of billing <code>PostalAddress</code>
     * @throws RepositoryException if an error occurs
     */
    public List<PostalAddress> getBillingAddresses() throws RepositoryException {
        if (billingAddresses == null) {
            billingAddresses = getRepository().getPostalAddresses(this, ContactPurpose.BILLING_ADDRESS);
        }
        return billingAddresses;
    }

    /**
     * Gets the list of <code>PaymentMethod</code> of this party.
     * This is a cross domain method.
     * @return the list of <code>PaymentMethod</code>
     * @throws RepositoryException if an error occurs
     */
    @Override
    public List<PaymentMethod> getPaymentMethods() throws RepositoryException {
        if (paymentMethods == null) {
            paymentMethods = getRepository().getPaymentMethods(this);
        }
        return paymentMethods;
    }

    /**
     * Get the list of <code>PartyClassification</code> of this party.
     * This is an alias for {@link org.opentaps.base.entities.Party#getPartyClassifications}.
     * @return the list of <code>PartyClassification</code>
     * @throws RepositoryException if an error occurs
     */
    public List<? extends PartyClassification> getClassifications() throws RepositoryException {
        return this.getPartyClassifications();
    }

    /**
     * Checks if this party has the given party classification.
     * @param classificationId a <code>PartyClassification</code> ID
     * @return a <code>Boolean</code> value
     * @throws RepositoryException if an error occurs
     */
    public Boolean hasClassification(String classificationId) throws RepositoryException {
        for (PartyClassification c : getClassifications()) {
            if (classificationId.equals(c.getPartyClassificationGroupId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the <code>TelecomNumber</code> by contactMechId for this party.
     * @param contactMechId a contact mech ID
     * @return a <code>TelecomNumber</code>
     * @throws RepositoryException if an error occurs
     */
    public TelecomNumber getTelecomNumberByContactMechId(String contactMechId) throws RepositoryException {
        for (int i = 0; i < getPartyContactMeches().size(); i++) {
            PartyContactMech partyContactMech = getPartyContactMeches().get(i);
            if (partyContactMech.getContactMechId().equals(contactMechId)) {
                return partyContactMech.getTelecomNumber();
            }
        }
       return null;
    }

    /**
     * Gets the <code>PartyContactMech</code> used for Asterisk for this party.
     * @return a <code>PartyContactMech</code> value
     * @throws RepositoryException if an error occurs
     */
    public PartyContactMech getAsteriskPartyContactMech() throws RepositoryException {
        PartyContactMech asteriskPartyContactMech = null;
        HashMap<String, PartyContactMech> partyContactMechTypes = new HashMap<String, PartyContactMech>();

        for (int i = 0; i < getPartyContactMeches().size(); i++) {
            PartyContactMech partyContactMech = getPartyContactMeches().get(i);
            if (partyContactMech.getContactMech().getContactMechType().getContactMechTypeId().equals("TELECOM_NUMBER")) {
                if (partyContactMech.getPartyContactMechPurposes().size() == 0) {
                    partyContactMechTypes.put("", partyContactMech);
                } else {
                    for (int k = 0; k < partyContactMech.getPartyContactMechPurposes().size(); k++) {
                        PartyContactMechPurpose partyContactMechPurpose = partyContactMech.getPartyContactMechPurposes().get(k);
                        if (partyContactMech.getExtension() != null) {
                            partyContactMechTypes.put(partyContactMechPurpose.getContactMechPurposeTypeId(), partyContactMech);
                        }
                    }
                }
            }
        }

        if (partyContactMechTypes.size() > 0) {
                if (partyContactMechTypes.containsKey("PHONE_WORK")) {
                    // first select work phone number
                    asteriskPartyContactMech = partyContactMechTypes.get("PHONE_WORK");
                } else if (partyContactMechTypes.containsKey("PRIMARY_PHONE")) {
                    // second select primary phone number
                    asteriskPartyContactMech = partyContactMechTypes.get("PRIMARY_PHONE");
                } else if (partyContactMechTypes.containsKey("PHONE_WORK_SEC")) {
                    // third select second work phone number
                    asteriskPartyContactMech = partyContactMechTypes.get("PHONE_WORK_SEC");
                } else {
                    // other, select random one
                    asteriskPartyContactMech = partyContactMechTypes.entrySet().iterator().next().getValue();
                }
        }

        return asteriskPartyContactMech;
    }

    /**
     * Gets the notes for this party ordered from most recent to oldest.
     * @return list of <code>PartyNoteView</code>
     * @throws RepositoryException if an error occurs
     */
    public List<PartyNoteView> getNotes() throws RepositoryException {
        return getRepository().getNotes(this);
    }

    protected PartyRepositoryInterface getRepository() throws RepositoryException {
        try {
            return PartyRepositoryInterface.class.cast(repository);
        } catch (ClassCastException e) {
            repository = DomainsDirectory.getDomainsDirectory(repository).getPartyDomain().getPartyRepository();
            return PartyRepositoryInterface.class.cast(repository);
        }
    }

}
