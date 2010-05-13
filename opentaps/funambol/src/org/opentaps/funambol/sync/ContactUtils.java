package org.opentaps.funambol.sync;

import java.util.HashMap;
import java.util.Map;

import mz.co.dbl.siga.framework.base.NotConfiguredException;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilValidate;

import com.funambol.common.pim.contact.Address;
import com.funambol.common.pim.contact.Contact;

public abstract class ContactUtils
{
	//Relation types which we'll have with clients
	public static final String RELATION_TYPE_UNKNOWN = "";
	public static final String RELATION_TYPE_LEAD = "Lead";
	public static final String RELATION_TYPE_CONTACT = "Contact";
	public static final String RELATION_TYPE_ACCOUNT = "Account";
	
	//Email mappings <opentaps, outlookPlug>
	private static Map<String,String> emailTypesMapping = new HashMap<String,String>();  
    
    private static Map<String, String> contactTypeEmptyFields = new HashMap<String,String>();
    static
    {
        contactTypeEmptyFields.put("TELECOM_NUMBER", "contactNumber");
        contactTypeEmptyFields.put("EMAIL_ADDRESS", "infoString");
        contactTypeEmptyFields.put("WEB_ADDRESS", "infoString");
    }
	
	/**
	 * Checks the type of relation: Lead, Contac, Account
	 * @param contact - instance of funambol's <code>Contact</code>
	 * @return <code>byte</code> value the type of relation eg.: <code>ContactUtils.RELATION_TYPE_LEAD</code>
	 */
	public static String contactType(Contact contact) throws GeneralException
	{
		if(isAccount(contact)){return RELATION_TYPE_ACCOUNT;}
		else
		if(isLead(contact)){return RELATION_TYPE_LEAD;}
		else
		if(isContact(contact)){return RELATION_TYPE_CONTACT;}
		else
		{
			throw new GeneralException("Relation not supported. The supported are "
					+RELATION_TYPE_ACCOUNT+", "
					+RELATION_TYPE_CONTACT+" and "
					+RELATION_TYPE_LEAD);
		}
	}
	
	//we have a name if at least one of the first or last name is set.
	private static boolean hasName(Contact contact)
	{
		String fName = contact.getName().getFirstName().getPropertyValueAsString();
		String lName = contact.getName().getLastName().getPropertyValueAsString();
		return UtilValidate.isNotEmpty(fName) || UtilValidate.isNotEmpty(lName);
	}
	//we have a name if display name is set.
	private static boolean hasDisplayName(Contact contact)
	{
		String dName = contact.getName().getDisplayName().getPropertyValueAsString();
		return UtilValidate.isNotEmpty(dName);
	}
	//Lead if the contact has both the name and company
	public static boolean isLead(Contact contact)
	{
		String company = contact.getBusinessDetail().getCompany().getPropertyValueAsString();
		
		if(hasName(contact)&&
				!UtilValidate.isEmpty(company))
		{
			return true;
		}
		
		return  false;   
	}
	//Account if the contact has only company but no name
	public static boolean isAccount(Contact contact)
	{
		String company = contact.getBusinessDetail().getCompany().getPropertyValueAsString();
		
		if(!hasName(contact)&&
				!UtilValidate.isEmpty(company))
		{
			return true;
		}
		
		return  false;   
	}
	//Contact if the param Contact has only name but not company
	public static boolean isContact(Contact contact)
	{
		String company = contact.getBusinessDetail().getCompany().getPropertyValueAsString();
		
		if((hasName(contact) || hasDisplayName(contact))&&
				UtilValidate.isEmpty(company))
		{
			return true;
		}
		
		return  false;   
	}
		    
    /**
     * Checks if a certain Address has a country-code
     * 
     * @param contact - funambol contact
     * @param purposeType - OT partyContactMechPurposeTypeId
     * @return true if the address with the given purposeType has a non-blank country code
     */
    public static boolean hasCountry(Contact contact, String purposeType)
    {
        String country = "";
        
        if("GENERAL_LOCATION".equals(purposeType))
        {
            country = contact.getBusinessDetail().getAddress().getCountry().getPropertyValueAsString();
        }
        else if("HOME_LOCATION".equals(purposeType))
        {
            country = contact.getPersonalDetail().getAddress().getCountry().getPropertyValueAsString();
        }
        else if("OTHER_LOCATION".equals(purposeType))
        {
            country = contact.getPersonalDetail().getOtherAddress().getCountry().getPropertyValueAsString();
        }
        
        return !UtilValidate.isEmpty(country);
    }
    
    /**
     * Indicate whether or not the data can effectively be considered empty, based on the given contact type
     * WARNING: this method only works for Phone, Web or Email contact types
     * 
     * @param params will be used to call an updateXXX service
     * @param contactType contactMechTypeId
     * @return true if the data is logically empty
     */
    public static boolean isEmpty(Map params, String contactType)
    {
        return UtilValidate.isEmpty(params.get(contactTypeEmptyFields.get(contactType)));
    }
    
    /** 
     * @param address should be AFTER addressCompatibilization has been called
     * @return true if the given address has only empty or default values for all its fields
     */
    public static boolean isEmpty(Address address, String defaultCountryGeoId, String defaultStateGeoId_US, String defaultEmptyString)
    {
        //we start by assuming true - it only needs one field with a real value, to count as non-empty
        
        //1.1 country
        if(!defaultCountryGeoId.equals(address.getCountry().getPropertyValueAsString())) { return false; } //we have a country
        
        //1.2 state - if we got here, should be different from default
        if(!defaultStateGeoId_US.equals(address.getState().getPropertyValueAsString())) { return false; }  //we have a state
        
        //1.3 address lines
        if(!defaultEmptyString.equals(address.getStreet().getPropertyValueAsString().trim())) { return false; }  //we have at least some street info
                    
        //1.4 postal code (some OT services do not allow it to be empty)
        if(!defaultEmptyString.equals(address.getPostalCode().getPropertyValueAsString().trim())) { return false; }
        
        //1.5 city (some OT services do not allow it to be empty)
        if(!defaultEmptyString.equals(address.getCity().getPropertyValueAsString())) { return false; }
        
        //2. if we got to here really this address has no useful info!
        return true;
    }
 
    /**
     * Find the address subrecord for the given purpose
     */
    public static Address getAddressForPurpose(Contact contact, String purpose)
    {
        if("GENERAL_LOCATION".equals(purpose)) { return contact.getBusinessDetail().getAddress(); }
        else if("HOME_LOCATION".equals(purpose)) { return contact.getPersonalDetail().getAddress(); }
        else if("OTHER_LOCATION".equals(purpose)) { return contact.getPersonalDetail().getOtherAddress(); }
        else { throw new NotConfiguredException("No address mapped to this purpose: " + purpose); }
    }
}
