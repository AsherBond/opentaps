package org.opentaps.dataimport;

import javolution.util.FastList;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;

import java.sql.Timestamp;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;

/**
 * Utility functions for importing.
 */
public class UtilImport {

    public static final String module = UtilImport.class.getName();

    /**
     * For each role in the given list of roles, checks if it is already defined.  Otherwise, creates a new PartyRole
     * value for it (but does not store it yet).  The resulting values can be stored at once in storeAll().
     */
    public static List<GenericValue> ensurePartyRoles(String partyId, List<String> roleTypeIds, Delegator delegator) throws GenericEntityException {
        List<GenericValue> roles = FastList.newInstance();
        Map input = UtilMisc.toMap("partyId", partyId);
        for (String roleTypeId : roleTypeIds) {
            input.put("roleTypeId", roleTypeId);
            List<GenericValue> myRoles = delegator.findByAnd("PartyRole", input);
            if (myRoles.size() == 0) {
                roles.add(delegator.makeValue("PartyRole", input));
            }
        }
        return roles;
    }

    // makes a Map of format PostalAddress
    @SuppressWarnings("unchecked")
    public static GenericValue makePostalAddress(GenericValue contactMech, String companyName, String firstName, String lastName, String attnName, String address1, String address2, String city, String stateGeoCode, String postalCode, String postalCodeExt, String countryGeoCode, Delegator delegator) {
        Map<String, Object> postalAddress = FastMap.newInstance();

        // full name of the person built from first and last name
        String fullName = "";
        if (!UtilValidate.isEmpty(firstName)) {
            fullName = firstName + " " + lastName;
        } else if (UtilValidate.isEmpty(lastName)) {
            fullName = lastName;
        }

        if (!UtilValidate.isEmpty(companyName)) {
            postalAddress.put("toName", companyName);
        } else {
            postalAddress.put("toName", fullName);
        }

        postalAddress.put("attnName", attnName);
        postalAddress.put("contactMechId", contactMech.get("contactMechId"));
        postalAddress.put("address1", address1);
        postalAddress.put("address2", address2);
        postalAddress.put("city", city);
        postalAddress.put("stateProvinceGeoId", stateGeoCode); 
        postalAddress.put("postalCode", postalCode);
        postalAddress.put("postalCodeExt", postalCodeExt);
        postalAddress.put("countryGeoId", countryGeoCode); 

        return delegator.makeValue("PostalAddress", postalAddress);
    }

    // make a TelecomNumber
    @SuppressWarnings("unchecked")
    public static GenericValue makeTelecomNumber(GenericValue contactMech, String countryCode, String areaCode, String contactNumber, Delegator delegator) {
        Map<String, Object> telecomNumber = FastMap.newInstance();
        telecomNumber.put("contactMechId", contactMech.get("contactMechId"));
        telecomNumber.put("countryCode", countryCode);
        telecomNumber.put("areaCode", areaCode);
        telecomNumber.put("contactNumber", contactNumber);
        return delegator.makeValue("TelecomNumber", telecomNumber);
    }

    @SuppressWarnings("unchecked")
    public static GenericValue makeContactMechPurpose(String contactMechPurposeTypeId, GenericValue contactMech, String partyId, Timestamp now, Delegator delegator) {
        Map<String, Object> partyContactMechPurpose = FastMap.newInstance();
        partyContactMechPurpose.put("partyId", partyId);
        partyContactMechPurpose.put("fromDate", now);
        partyContactMechPurpose.put("contactMechId", contactMech.get("contactMechId"));
        partyContactMechPurpose.put("contactMechPurposeTypeId", contactMechPurposeTypeId);
        return delegator.makeValue("PartyContactMechPurpose", partyContactMechPurpose);
    }

    @SuppressWarnings("unchecked")
    public static List<GenericValue> makePartyWithRoles(String partyId, String partyTypeId, List<String> roleTypeIds, Delegator delegator) {
        List<GenericValue> partyValues = FastList.newInstance();
        partyValues.add(delegator.makeValue("Party", UtilMisc.toMap("partyId", partyId, "partyTypeId", partyTypeId)));
        for (Iterator<String> rti = roleTypeIds.iterator(); rti.hasNext(); ) {
            String nextRoleTypeId = (String) rti.next();        
            partyValues.add(delegator.makeValue("PartyRole", UtilMisc.toMap("partyId", partyId, "roleTypeId", nextRoleTypeId)));
        }
        return partyValues;
    }

    @SuppressWarnings("unchecked")
    public static GenericValue makePartySupplementalData(GenericValue partySupplementalData, String partyId, String fieldToUpdate, GenericValue contactMech, Delegator delegator) {

        if (partySupplementalData == null) {
            // create a new partySupplementalData
            Map<String, String> input = UtilMisc.toMap("partyId", partyId, fieldToUpdate, contactMech.getString("contactMechId"));
            return delegator.makeValue("PartySupplementalData", input);
        }

        // create or update the field
        partySupplementalData.set(fieldToUpdate, contactMech.get("contactMechId"));
        return null;
    }

    /** Decodes "0211" to "02/2011". If the input data is bad, then this returns null. */
    public static String decodeExpireDate(String importDate) {
        if (importDate.length() != 4) return null;
        StringBuffer expireDate = new StringBuffer(importDate.substring(0,2));
        expireDate.append("/20");   // hopefully code will not survive into the 22nd century...
        expireDate.append(importDate.substring(2,4));
        return expireDate.toString();
    }


}
