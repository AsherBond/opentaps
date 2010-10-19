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
/* Copyright (c) Open Source Strategies, Inc. */

package org.opentaps.common.party;

import java.util.*;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;

/**
 * This class is a series of convenience methods to help extract particular types of contact information, so it's
 * easier to get the Primary Phone Number, Email Address, Postal Address of a party without having to work with the
 * highly normalized OFBIZ contact data model.  Mostly to help out with form widgets.
 *
 * @author sichen@opensourcestrategies.com
 *
 */
public final class PartyContactHelper {

    private PartyContactHelper() { }

    private static final String MODULE = PartyContactHelper.class.getName();

    /**
     * This is the base method and returns a List of PartyContactWithPurpose for the chosen parameters.  getActiveOnly
     * @param partyId the party ID
     * @param contactMechTypeId the contact mech type ID
     * @param contactMechPurposeTypeId will be used if not null
     * @param getActiveOnly get only active ones (filter out expired contacts and expired contact purposes)
     * @param delegator a <code>Delegator</code> value
     * @return the list of contact mech <code>GenericValue</code> for the given party id matching the given purpose ID
     * @throws GenericEntityException if an error occurs
     */
    public static List<GenericValue> getContactMechsByPurpose(String partyId, String contactMechTypeId, String contactMechPurposeTypeId, boolean getActiveOnly, Delegator delegator) throws GenericEntityException {
        List<EntityCondition> conditions = UtilMisc.<EntityCondition>toList(EntityCondition.makeCondition("partyId", partyId));
        if (contactMechTypeId != null) {
            conditions.add(EntityCondition.makeCondition("contactMechTypeId", contactMechTypeId));
        }
        if (contactMechPurposeTypeId != null) {
            conditions.add(EntityCondition.makeCondition("contactMechPurposeTypeId", contactMechPurposeTypeId));
        }
        if (getActiveOnly) {
            conditions.add(EntityUtil.getFilterByDateExpr("contactFromDate", "contactThruDate"));
            conditions.add(EntityUtil.getFilterByDateExpr("purposeFromDate", "purposeThruDate"));
        }
        List<GenericValue> potentialContactMechs = delegator.findByAnd("PartyContactWithPurpose", conditions, UtilMisc.toList("contactFromDate DESC"));

        return potentialContactMechs;
    }

    /**
     * Helper method to get the first telecom number of a party as a GenericValue for a given
     * contactMechPurposeTypeId. (ie, PHONE_WORK, PRIMARY_PHONE, etc.)  If there are many, the
     * first in the List is returned.  If there are none, then a null is returned.
     *
     * The difference between this and getTelecomNumberValueByPurpose() is that this returns a Map that
     * includes the extension of the phone number.  This is due to the datamodel; Extensions are stored in
     * PartyContactMech. Use this method instead of getTelecomNumberValueByPurpose().
     *
     * @param partyId the party ID
     * @param contactMechPurposeTypeId purpose of phone number
     * @param getActiveOnly flag to return only the currently active telecom numbers
     * @param delegator a <code>Delegator</code> value
     * @return Map of TelecomNumber fields + "extension"
     * @throws GenericEntityException if an error occurs
     */
    public static Map<String, Object> getTelecomNumberMapByPurpose(String partyId, String contactMechPurposeTypeId, boolean getActiveOnly, Delegator delegator) throws GenericEntityException {

        List<GenericValue> possibleTelecomNumbers = getContactMechsByPurpose(partyId, "TELECOM_NUMBER", contactMechPurposeTypeId, getActiveOnly, delegator);
        if ((possibleTelecomNumbers == null) || (possibleTelecomNumbers.size() == 0)) {
            Debug.logInfo("No suitable phone number found for [" + partyId + "] with purpose ["
                    + contactMechPurposeTypeId + "] and getActiveOnly = [" + getActiveOnly + "]", MODULE);
            return null;
        }
        GenericValue contactMech = possibleTelecomNumbers.get(0);
        GenericValue telecomNumber = contactMech.getRelatedOne("TelecomNumber");
        if (telecomNumber == null) {
            Debug.logInfo("No telecom number was related to contact mech [" + contactMech + "]", MODULE);
            return null;
        }

        // build the return map
        Map<String, Object> returnMap = telecomNumber.getAllFields();

        // get the extension, which is in the PartyContactMech entity
        List<GenericValue> possiblePartyContactMechs = contactMech.getRelatedByAnd("PartyContactMech", UtilMisc.toMap("partyId", partyId));
        if (possiblePartyContactMechs.size() > 0) {
            GenericValue partyContactMech = possiblePartyContactMechs.get(0);
            returnMap.put("extension", partyContactMech.get("extension"));
        }

        return returnMap;
    }

    /**
     * Helper method to get the first telecom number of a party as a GenericValue for a given contactMechPurposeTypeId.
     * (ie, PHONE_WORK, PRIMARY_PHONE, etc.)  If there are many, the first in the List is returned.
     * If there are none, then a null is returned. This is deprecated, use getTelecomNumberMapByPurpose() instead.
     *
     * @param partyId the party ID
     * @param contactMechPurposeTypeId purpose of phone number
     * @param getActiveOnly flag to return only the currently active telecom numbers
     * @param delegator a <code>Delegator</code> value
     * @return GenericValue TelecomNumber
     * @throws GenericEntityException if an error occurs
     * @deprecated
     */
    public static GenericValue getTelecomNumberValueByPurpose(String partyId, String contactMechPurposeTypeId, boolean getActiveOnly, Delegator delegator) throws GenericEntityException {

        List<GenericValue> possibleTelecomNumbers = getContactMechsByPurpose(partyId, "TELECOM_NUMBER", contactMechPurposeTypeId, getActiveOnly, delegator);
        if ((possibleTelecomNumbers == null) || (possibleTelecomNumbers.size() == 0)) {
            Debug.logInfo("No suitable phone number found for [" + partyId + "] with purpose ["
                    + contactMechPurposeTypeId + "] and getActiveOnly = [" + getActiveOnly + "]", MODULE);
            return null;
        }
        GenericValue contactMech = possibleTelecomNumbers.get(0);
        GenericValue telecomNumber = contactMech.getRelatedOne("TelecomNumber");
        if (telecomNumber == null) {
            Debug.logInfo("No telecom number was related to contact mech [" + contactMech + "]", MODULE);
        }
        return telecomNumber;
    }

    /**
     * A helper method to return the telecom number of a party as a String for a given contactMechPurposeTypeId
     * (ie, PHONE_WORK, PRIMARY_PHONE, etc.)  If there are many, the first in the List is returned.
     * (If you need to work with the whole List, use getContactMechByPurpose.
     *
     * @param partyId the party ID
     * @param contactMechPurposeTypeId purpose of phone number
     * @param getActiveOnly flag to return only the currently active telecom numbers
     * @param delegator a <code>Delegator</code> value
     * @return String phone number in 1 (123) 4567890 format or null if there is no phone number
     * @throws GenericEntityException if an error occurs
     */
    public static String getTelecomNumberByPurpose(String partyId, String contactMechPurposeTypeId, boolean getActiveOnly, Delegator delegator) throws GenericEntityException {

        StringBuffer buff = new StringBuffer();

        Map<String, Object> telecomNumber = getTelecomNumberMapByPurpose(partyId, contactMechPurposeTypeId, getActiveOnly, delegator);

        if (telecomNumber == null) {
            return null;
        }

        if (telecomNumber.get("countryCode") != null) {
            buff.append((String) telecomNumber.get("countryCode")).append(" ");
        }
        if (telecomNumber.get("areaCode") != null) {
            buff.append("(").append((String) telecomNumber.get("areaCode")).append(") ");
        }
        if (telecomNumber.get("contactNumber") != null) {
            buff.append((String) telecomNumber.get("contactNumber"));
        }
        if (telecomNumber.get("extension") != null) {
            buff.append(" x").append((String) telecomNumber.get("extension"));
        }

        // done to standardize all API methods to return null when empty
        return (buff.length() == 0 ? null : buff.toString());
    }

    /**
     * Same as above but only returns currently active phone numbers.
     * A helper method to return the telecom number of a party as a String for a given contactMechPurposeTypeId
     * (ie, PHONE_WORK, PRIMARY_PHONE, etc.)  If there are many, the first in the List is returned.
     * (If you need to work with the whole List, use getContactMechByPurpose.
     *
     * @param partyId the party ID
     * @param contactMechPurposeTypeId purpose of phone number
     * @param delegator a <code>Delegator</code> value
     * @return String phone number in 1 (123) 4567890 format or null if there is no phone number
     * @throws GenericEntityException if an error occurs
     */
    public static String getTelecomNumberByPurpose(String partyId, String contactMechPurposeTypeId, Delegator delegator) throws GenericEntityException {
        return getTelecomNumberByPurpose(partyId, contactMechPurposeTypeId, true, delegator);
    }

    /**
     * The point of this method is to get the ContactMech.infoString which is the value for any ContactMech of the type of the electronic address.
     * For example, fetching the contact mech type WEB_ADDRESS with purpose PRIMARY_WEB_URL might result in "http://example.domain"
     * Returns the infoString from the first one of specified type/purpose
     * @param partyId the party ID
     * @param contactMechTypeId the contact mech type ID
     * @param contactMechPurposeTypeId purpose of electronic address
     * @param getActiveOnly flag to return only the currently active electronic addresses
     * @param delegator a <code>Delegator</code> value
     * @return the first matching electronic address string
     * @throws GenericEntityException if an error occurs
     */
    public static String getElectronicAddressByPurpose(String partyId, String contactMechTypeId, String contactMechPurposeTypeId, boolean getActiveOnly, Delegator delegator) throws GenericEntityException {

        List<GenericValue> possibleAddresses = getContactMechsByPurpose(partyId, contactMechTypeId, contactMechPurposeTypeId, getActiveOnly, delegator);
        if ((possibleAddresses != null) && (possibleAddresses.size() > 0)) {
            GenericValue contactMech = possibleAddresses.get(0);
            if (contactMech != null) {
                return contactMech.getString("infoString");
            } else {
                Debug.logInfo("No [" + contactMechTypeId + "] related to partyId [" + partyId + "] with purpose [" + contactMechPurposeTypeId + "] and getActiveOnly = [" + getActiveOnly + "]", MODULE);
            }
        }

        return null;
    }

    /**
     * Same as above but only returns active electronic addresses.
     * The point of this method is to get the ContactMech.infoString which is the value for any ContactMech of the type of the electronic address.
     * For example, fetching the contact mech type WEB_ADDRESS with purpose PRIMARY_WEB_URL might result in "http://example.domain"
     * Returns the infoString from the first one of specified type/purpose
     * @param partyId the party ID
     * @param contactMechTypeId the contact mech type ID
     * @param contactMechPurposeTypeId purpose of electronic address
     * @param delegator a <code>Delegator</code> value
     * @return the first matching electronic address string
     * @throws GenericEntityException if an error occurs
     */
    public static String getElectronicAddressByPurpose(String partyId, String contactMechTypeId, String contactMechPurposeTypeId, Delegator delegator) throws GenericEntityException {
        return getElectronicAddressByPurpose(partyId, contactMechTypeId, contactMechPurposeTypeId, true, delegator);
    }

    /**
     * This method returns a GenericValue rather than a String because a PostalAddress is fairly complicated, and the user may want to
     * format it himself in a FTL page.
     * @param partyId the party ID
     * @param contactMechPurposeTypeId purpose of postal address
     * @param getActiveOnly flag to return only the currently active electronic addresses
     * @param delegator a <code>Delegator</code> value
     * @return First PostalAddress of the specified contactMechPurposeTypeId
     * @throws GenericEntityException if an error occurs
     */
    public static GenericValue getPostalAddressValueByPurpose(String partyId, String contactMechPurposeTypeId, boolean getActiveOnly, Delegator delegator) throws GenericEntityException {
        List<GenericValue> possibleAddresses = getContactMechsByPurpose(partyId, "POSTAL_ADDRESS", contactMechPurposeTypeId, getActiveOnly, delegator);

        if ((possibleAddresses != null) && (possibleAddresses.size() > 0)) {
            GenericValue contactMech = possibleAddresses.get(0).getRelatedOne("ContactMech");
            if (contactMech != null) {
                return contactMech.getRelatedOne("PostalAddress");
            } else {
                Debug.logInfo("No Postal Address related to partyId [" + partyId + "] with purpose [" + contactMechPurposeTypeId + "] and getActiveOnly = [" + getActiveOnly + "]", MODULE);
            }
        }
        return null;
    }


    /**
     * This is a commonly used shorthand display for a postal address.  Only supports currently active addresses.
     *
     * @param partyId the party ID
     * @param contactMechPurposeTypeId purpose of postal address
     * @param delegator a <code>Delegator</code> value
     * @return Abbreviated string for postal address of the contactMechPurposeTypeId.  Currently just City, ST.  null if no postal address.
     * @throws GenericEntityException if an error occurs
     */
    public static String getAbbrevPostalAddressByPurpose(String partyId, String contactMechPurposeTypeId, Delegator delegator) throws GenericEntityException {
        GenericValue postalAddress = getPostalAddressValueByPurpose(partyId, contactMechPurposeTypeId, true, delegator);

        String abbrevPostalAddress = "";
        if (postalAddress != null) {
            if (postalAddress.getString("city") != null) {
                abbrevPostalAddress += postalAddress.getString("city") + ", ";
            }
            if (postalAddress.getString("stateProvinceGeoId") != null) {
                abbrevPostalAddress += postalAddress.getString("stateProvinceGeoId");
            }
        } else {
            Debug.logInfo("No Postal Address related to partyId [" + partyId + "] with purpose [" + contactMechPurposeTypeId + "]", MODULE);
        }

        if (abbrevPostalAddress.equals("")) {
            return null;
        } else {
            return abbrevPostalAddress;
        }
    }

    /**
     * Provides a list of partyIds matching any email addresses in the input string. The input string is split by the provided delimiter, if provided.
     * @param delegator a <code>Delegator</code> value
     * @param possibleEmailString a list of email addresses separated by the given delimiter
     * @param delimiter to split the given possibleEmailString
     * @return a list of partyIds matching any email addresses in the input string
     * @throws GenericEntityException if an error occurs
     */
    public static List<String> getPartyIdsMatchingEmailsInString(Delegator delegator, String possibleEmailString, String delimiter) throws GenericEntityException {
        Set<String> partyIds = new LinkedHashSet<String>();
        String[] possibleEmails = {possibleEmailString};
        if (delimiter != null) {
            possibleEmails = possibleEmailString.split("\\s*" + delimiter + "\\s*");
        }
        for (String possibleEmail : possibleEmails) {
            EntityCondition filterConditions = EntityCondition.makeCondition(EntityOperator.AND,
                                                                             EntityCondition.makeCondition("infoString", possibleEmail),
                                                                             EntityUtil.getFilterByDateExpr());
            List<GenericValue> pcms = delegator.findByCondition("PartyAndContactMech", filterConditions, null, Arrays.asList("fromDate DESC"));
            if (pcms != null) {
                partyIds.addAll(EntityUtil.<String>getFieldListFromEntityList(pcms, "partyId", false));
            }
        }
        return UtilMisc.toList(partyIds);
    }
}
