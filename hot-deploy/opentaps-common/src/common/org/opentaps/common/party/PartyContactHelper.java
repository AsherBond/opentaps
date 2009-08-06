/*
 * Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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
/* Copyright (c) 2005-2006 Open Source Strategies, Inc. */

package org.opentaps.common.party;

import java.util.*;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.util.EntityUtil;

/**
 * This class is a series of convenience methods to help extract particular types of contact information, so it's
 * easier to get the Primary Phone Number, Email Address, Postal Address of a party without having to work with the
 * highly normalized OFBIZ contact data model.  Mostly to help out with form widgets
 *  
 * @author sichen@opensourcestrategies.com
 * 
 */

public class PartyContactHelper {
    
    public static final String module = PartyContactHelper.class.getName();
    
    /**
     * This is the base method and returns a List of PartyContactWithPurpose for the chosen parameters.  getActiveOnly
     * @param partyId
     * @param contactMechTypeId
     * @param contactMechPurposeTypeId will be used if not null
     * @param getActiveOnly get only active ones (filter out expired contacts and expired contact purposes)
     * @param delegator
     * @return
     */
    public static List getContactMechsByPurpose(String partyId, String contactMechTypeId, String contactMechPurposeTypeId, 
            boolean getActiveOnly, GenericDelegator delegator) throws GenericEntityException {
        List conditions = UtilMisc.toList( new EntityExpr("partyId", EntityOperator.EQUALS, partyId) );
        if (contactMechTypeId != null) {
            conditions.add( new EntityExpr("contactMechTypeId", EntityOperator.EQUALS, contactMechTypeId) );
        }
        if (contactMechPurposeTypeId != null) {
            conditions.add( new EntityExpr("contactMechPurposeTypeId", EntityOperator.EQUALS, contactMechPurposeTypeId) );
        }
        if (getActiveOnly) {
            conditions.add( EntityUtil.getFilterByDateExpr("contactFromDate", "contactThruDate") );
            conditions.add( EntityUtil.getFilterByDateExpr("purposeFromDate", "purposeThruDate") );
        }
        List potentialContactMechs = delegator.findByAnd("PartyContactWithPurpose", conditions, UtilMisc.toList("contactFromDate DESC"));
        
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
     * @param partyId 
     * @param contactMechPurposeTypeId purpose of phone number
     * @param getActiveOnly 
     * @return Map of TelecomNumber fields + "extension"

     */
    public static Map getTelecomNumberMapByPurpose(String partyId, String contactMechPurposeTypeId,
            boolean getActiveOnly, GenericDelegator delegator) throws GenericEntityException {

        List possibleTelecomNumbers = getContactMechsByPurpose(partyId, "TELECOM_NUMBER", contactMechPurposeTypeId, getActiveOnly, delegator);
        if ((possibleTelecomNumbers == null) || (possibleTelecomNumbers.size() == 0)) {
            Debug.logInfo("No suitable phone number found for [" + partyId + "] with purpose [" 
                    + contactMechPurposeTypeId + "] and getActiveOnly = [" + getActiveOnly + "]", module);
            return null;
        }
        GenericValue contactMech = (GenericValue) possibleTelecomNumbers.get(0);
        GenericValue telecomNumber = contactMech.getRelatedOne("TelecomNumber");
        if (telecomNumber == null) {
            Debug.logInfo("No telecom number was related to contact mech [" + contactMech + "]", module);                
            return null;
        }

        // build the return map
        Map returnMap = telecomNumber.getAllFields();

        // get the extension, which is in the PartyContactMech entity
        List possiblePartyContactMechs = contactMech.getRelatedByAnd("PartyContactMech", UtilMisc.toMap("partyId", partyId));
        if (possiblePartyContactMechs.size() > 0) {
            GenericValue partyContactMech = (GenericValue) possiblePartyContactMechs.get(0);
            returnMap.put("extension", partyContactMech.get("extension"));
        }

        return returnMap;
    }

    /**
     * Helper method to get the first telecom number of a party as a GenericValue for a given contactMechPurposeTypeId.
     * (ie, PHONE_WORK, PRIMARY_PHONE, etc.)  If there are many, the first in the List is returned.  
     * If there are none, then a null is returned. This is deprecated, use getTelecomNumberMapByPurpose() instead.
     *
     * @param partyId 
     * @param contactMechPurposeTypeId purpose of phone number
     * @param getActiveOnly 
     * @return GenericValue TelecomNumber
     * @deprecated
     */
    public static GenericValue getTelecomNumberValueByPurpose(String partyId, String contactMechPurposeTypeId,
            boolean getActiveOnly, GenericDelegator delegator) throws GenericEntityException {

        List possibleTelecomNumbers = getContactMechsByPurpose(partyId, "TELECOM_NUMBER", contactMechPurposeTypeId, getActiveOnly, delegator);
        if ((possibleTelecomNumbers == null) || (possibleTelecomNumbers.size() == 0)) {
            Debug.logInfo("No suitable phone number found for [" + partyId + "] with purpose [" 
                    + contactMechPurposeTypeId + "] and getActiveOnly = [" + getActiveOnly + "]", module);
            return null;
        }
        GenericValue contactMech = (GenericValue) possibleTelecomNumbers.get(0);
        GenericValue telecomNumber = contactMech.getRelatedOne("TelecomNumber");
        if (telecomNumber == null) {
            Debug.logInfo("No telecom number was related to contact mech [" + contactMech + "]", module);                
        }
        return telecomNumber;
    }

    /**
     * A helper method to return the telecom number of a party as a String for a given contactMechPurposeTypeId 
     * (ie, PHONE_WORK, PRIMARY_PHONE, etc.)  If there are many, the first in the List is returned.  
     * (If you need to work with the whole List, use getContactMechByPurpose.  
     *  
     * @param partyId 
     * @param contactMechPurposeTypeId purpose of phone number
     * @param getActiveOnly 
     * @return String phone number in 1 (123) 4567890 format or null if there is no phone number 
     */
    public static String getTelecomNumberByPurpose(String partyId, String contactMechPurposeTypeId, 
            boolean getActiveOnly, GenericDelegator delegator) throws GenericEntityException {
        
        StringBuffer buff = new StringBuffer();
        
        Map telecomNumber = getTelecomNumberMapByPurpose(partyId, contactMechPurposeTypeId, getActiveOnly, delegator);

        if (telecomNumber == null) return null;

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
     * Same as above but only returns currently active phone numbers
     * 
     * @param partyId
     * @param contactMechPurposeTypeId
     * @return
     */
    public static String getTelecomNumberByPurpose(String partyId, String contactMechPurposeTypeId, GenericDelegator delegator) throws GenericEntityException { 
        return getTelecomNumberByPurpose(partyId, contactMechPurposeTypeId, true, delegator);
    }
    
    /**
     * The point of this method is to get the ContactMech.infoString which is the value for any ContactMech of the type of the electronic address.
     * For example, fetching the contact mech type WEB_ADDRESS with purpose PRIMARY_WEB_URL might result in "http://example.domain"
     * Returns the infoString from the first one of specified type/purpose 
     * @param partyId
     * @param contactMechTypeId
     * @param contactMechPurposeTypeId
     * @param getActiveOnly
     * @param delegator
     * @return
     * @throws GenericEntityException
     */
    public static String getElectronicAddressByPurpose(String partyId, String contactMechTypeId, String contactMechPurposeTypeId, 
            boolean getActiveOnly, GenericDelegator delegator) throws GenericEntityException {
        
        List possibleAddresses = getContactMechsByPurpose(partyId, contactMechTypeId, contactMechPurposeTypeId, getActiveOnly, delegator);
        if ((possibleAddresses != null) && (possibleAddresses.size() > 0)) {
            GenericValue contactMech = (GenericValue) possibleAddresses.get(0);
            if (contactMech != null) {
                return contactMech.getString("infoString");
            } else {
                Debug.logInfo("No [" + contactMechTypeId + "] related to partyId [" + partyId + "] with purpose [" + contactMechPurposeTypeId + "] and getActiveOnly = [" + getActiveOnly + "]", module);
            }
        }
        
        return null;
    }
    
    /**
     * Same as above but only returns active electronic addresses
     * @param partyId
     * @param contactMechTypeId
     * @param contactMechPurposeTypeId
     * @param delegator
     * @return 
     * @throws GenericEntityException
     */
    public static String getElectronicAddressByPurpose(String partyId, String contactMechTypeId, String contactMechPurposeTypeId, 
            GenericDelegator delegator) throws GenericEntityException {
        return getElectronicAddressByPurpose(partyId, contactMechTypeId, contactMechPurposeTypeId, true, delegator);   
    }
    
    /**
     * This method returns a GenericValue rather than a String because a PostalAddress is fairly complicated, and the user may want to 
     * format it himself in a FTL page
     * @param partyId
     * @param contactMechPurposeTypeId
     * @param getActiveOnly
     * @param delegator
     * @return First PostalAddress of the specified contactMechPurposeTypeId
     * @throws GenericEntityException
     */
    public static GenericValue getPostalAddressValueByPurpose(String partyId, String contactMechPurposeTypeId, 
            boolean getActiveOnly, GenericDelegator delegator) throws GenericEntityException {
        List possibleAddresses = getContactMechsByPurpose(partyId, "POSTAL_ADDRESS", contactMechPurposeTypeId, getActiveOnly, delegator);
        
        if ((possibleAddresses != null) && (possibleAddresses.size() > 0)) {
            GenericValue contactMech = ((GenericValue) possibleAddresses.get(0)).getRelatedOne("ContactMech");
            if (contactMech != null) {
                return contactMech.getRelatedOne("PostalAddress");
            } else {
                Debug.logInfo("No Postal Address related to partyId [" + partyId + "] with purpose [" + contactMechPurposeTypeId + "] and getActiveOnly = [" + getActiveOnly + "]", module);
            }
        } 
        return null;
    }
    
    
    /**
     * This is a commonly used shorthand display for a postal address.  Only supports currently active addresses.
     * 
     * @param partyId
     * @param contactMechPurposeTypeId
     * @param delegator
     * @return Abbreviated string for postal address of the contactMechPurposeTypeId.  Currently just City, ST.  null if no postal address.
     * @throws GenericEntityException
     */
    public static String getAbbrevPostalAddressByPurpose(String partyId, String contactMechPurposeTypeId, 
            GenericDelegator delegator) throws GenericEntityException {
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
            Debug.logInfo("No Postal Address related to partyId [" + partyId + "] with purpose [" + contactMechPurposeTypeId + "]", module);
        }
        
        if (abbrevPostalAddress.equals("")) {
            return null;
        } else {
            return abbrevPostalAddress;
        }
    }

    /**
     * Provides a list of partyIds matching any email addresses in the input string. The input string is split by the provided delimiter, if provided.
     * @param delegator
     * @param possibleEmailString
     * @param delimiter
     * @return
     * @throws GenericEntityException
     */
    public static List<String> getPartyIdsMatchingEmailsInString(GenericDelegator delegator, String possibleEmailString, String delimiter) throws GenericEntityException {
        Set<String> partyIds = new LinkedHashSet();
        String[] possibleEmails = {possibleEmailString};
        if (delimiter != null) possibleEmails = possibleEmailString.split("\\s*" + delimiter + "\\s*");
        for (String possibleEmail : possibleEmails) {
            EntityConditionList filterConditions = new EntityConditionList(UtilMisc.toList(new EntityExpr("infoString", EntityOperator.EQUALS, possibleEmail), EntityUtil.getFilterByDateExpr()), EntityOperator.AND);
            List pcms = delegator.findByCondition("PartyAndContactMech", filterConditions, null, Arrays.asList("fromDate DESC"));
            if (pcms != null) partyIds.addAll(EntityUtil.getFieldListFromEntityList(pcms, "partyId", false));
        }
        return UtilMisc.toList(partyIds);        
    }
}
