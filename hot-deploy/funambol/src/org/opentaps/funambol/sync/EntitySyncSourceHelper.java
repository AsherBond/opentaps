/* Copyright (c) 2006 - 2009 Open Source Strategies, Inc.
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


package org.opentaps.funambol.sync;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityListIterator;

import com.funambol.common.pim.common.TypifiedProperty;
import com.funambol.common.pim.contact.Contact;
import com.funambol.framework.logging.FunambolLogger;
import com.funambol.framework.logging.FunambolLoggerFactory;

/**
 * ONE instance of this class should be used per sync - it is NOT thread-safe
 * 
 * @author Cameron Smith, Eurico da Silva - Database, Lda - www.database.co.mz
 *
 */
public class EntitySyncSourceHelper 
{
	private static FunambolLogger log = FunambolLoggerFactory.getLogger("funambol.server");
	    
    //variables representing other data or structures needed to perform the sync
    private static String createdStamp = "createdStamp";
    private static String lastUpdatedStamp = "lastUpdatedStamp";
    private static Map<String, String> purposeAndContactMechType=initPurposeTypeAndContactMechType();
    private static Map purposeTypePropertyTypeRelation = initPurposeTypePropertyTypeRelation();
    
    /**
     * Get all the items from the given EntityListIterator and build up a list of the related Partys - then close the iterator
     * 
     * TODO: use next() instead of hasNext(), apparently it would be much more efficient
     * 
     * @param it must contain entities which have the field partyId
     * @param relation name of the relation as per Entity Data Maintenance screen
     * @return
     * @throws GenericEntityException if any of the relations cannot be found, or are not of type ONE
     */
	public static List<GenericValue> getPartyListFromIterator(EntityListIterator it) throws GenericEntityException
    {
    	List<GenericValue> gvs = new ArrayList<GenericValue>();
    	    	
    	try
    	{
            GenericValue entity = null;
			while((entity = (GenericValue)it.next()) != null)  //we cannot use getRelatedOne because the entities may be view entities without relationships
			{				
                gvs.add(entity.getDelegator().findByPrimaryKey("Party", UtilMisc.toMap("partyId", entity.get("partyId"))));
			}
		}
    	finally { it.close(); }
    	
    	return gvs;
    }

    /**
     * @return a subset of all[Parties|task|any other entity] which were created > since
     * 
     * TODO: we should be able to calculate this once only, if since is the same for all data subsets (NEW, UPDATED, etc).
     */
	public static List<GenericValue> getNewEntities(List<GenericValue> allEntities, Timestamp since)
    {
		List<GenericValue> created = new LinkedList<GenericValue>();
    	for(GenericValue gValue: allEntities)
		{
			if(gValue.getTimestamp(createdStamp).after(since))
			{
				created.add(gValue);
			}
		}
    	
    	return created;
    }
	
    /**
     * Only works for Party because of subcriteria
     */
	public static List<GenericValue> getUpdatedParties(List<GenericValue> allParties, Timestamp since) throws GenericEntityException
	{
		List<GenericValue> updatedParties = new LinkedList<GenericValue>();  //2.1 create new empty list
		for(GenericValue party: allParties)  //2.2 for part in allParties
		{
			if(wasPartyUpdated(party, since))
			{
				updatedParties.add(party);
			}
		}
		return updatedParties;
	}		
	
	public static boolean wasPartyUpdated(GenericValue party, Timestamp since) throws GenericEntityException
	{
        //2.2.1 the party itself may have been updated (but not created) since last sync
		if(wasUpdated(party, since)) { return true;	}
		
        //2.2.2 failing that, the Person or PartyGroup may have been updated (e.g. surname)
		if(wasUpdated(party.getRelatedOne("Person"), since)) { return true; }
        if(wasUpdated(party.getRelatedOne("PartyGroup"), since)) { return true; }
		if(wasUpdated(party.getRelatedOne("PartySupplementalData"), since))
		{
			return true; 
		}
		
        //2.2.3 failing that too, only a new PCM would count as an update
		return wereAnyUpdated(party.getRelated("PartyContactMech"), since);
	}
	
	/**
     * Only works for Party because of subcriteria
     */
	public static List<GenericValue> getUpdatedTasks(List<GenericValue> allTasks, Timestamp since) throws GenericEntityException
	{
		List<GenericValue> updatedTasks = new LinkedList<GenericValue>();  //2.1 create new empty list
		for(GenericValue task : allTasks)  //2.2 for part in allParties
		{
			if(wasUpdated(task, since))
			{
				updatedTasks.add(task);
			}
		}
		return updatedTasks;
	}		
	
    /**
     * Warning - this method is designed for subrecords, so also counts as new, anything which has just been created 
     */
	private static boolean wereAnyUpdated(List<GenericValue> genericValues, Timestamp since)
    {
    	for(GenericValue gValue: genericValues)
		{
    		if(wasUpdatedOrCreated(gValue, since)) {	return true; }
		}
    	
        //if we got here, nothing has changed
    	return false;
    }
	
     public static boolean wasCreated(GenericValue gValue, Timestamp since)
     {
         if(since==null) { return true; }  //TODO: DSS gives null here on first sync, but is returning true the correct behaviour

         return gValue != null && gValue.getTimestamp(createdStamp).after(since);  
     }
    
    public static boolean wasUpdatedOrCreated(GenericValue gValue, Timestamp since)
    {
        if(since==null) { return true; }  //TODO: DSS gives null here on first sync, but is returning true the correct behaviour
        
        return gValue != null &&
               gValue.getTimestamp(createdStamp).after(since) ||
               gValue.getTimestamp(lastUpdatedStamp).after(since);  
    }
    
	protected static boolean wasUpdated(GenericValue gValue, Timestamp since)
	{
		if(since==null) { return true; }  //TODO: DSS gives null here on first sync, but is returning true the correct behaviour
		
		return gValue != null &&
               gValue.getTimestamp(createdStamp).before(since) &&
               gValue.getTimestamp(lastUpdatedStamp).after(since);	
	}
	
	public static void prepateServiceCreateRelationshipMap(Contact contact, Map<String, String> params)
	{
		boolean isContact = ContactUtils.isContact(contact);
		boolean isLead = ContactUtils.isLead(contact);
		boolean isAccount = ContactUtils.isAccount(contact);
		
		if( isContact|| isLead)
		{
                    params.put("firstName", contact.getName().getFirstName().getPropertyValueAsString());
                    params.put("lastName", contact.getName().getLastName().getPropertyValueAsString());
		}

		if(isContact)
		{
                    String firstName = contact.getName().getFirstName().getPropertyValueAsString();
                    String lastName = contact.getName().getLastName().getPropertyValueAsString();
                    if (UtilValidate.isEmpty(lastName)) {
                        lastName = contact.getName().getDisplayName().getPropertyValueAsString();
                    }
                    if (UtilValidate.isEmpty(lastName) && UtilValidate.isNotEmpty(firstName)) {
                        lastName = "Unknown";
                    }
                    params.put("firstName", firstName);
                    params.put("lastName", lastName);
		}

		if(isAccount|| isLead)
		{
                    params.put(isAccount?"groupName":"companyName",
					contact.getBusinessDetail().getCompany().getPropertyValueAsString()
					);
		}
	}

    /**
     * Return the given String, normalized for ease of comparison.
     * Currently the normalization comprises the following:
     *  <ol>
     *   <li>Trim leading and trailing whitespace</li>
     *   <li>Uppercase all letters</li>
     *   <li>Remove all non alphanumeric characters</li>
     *  </ol>
     */
    public static String normalize(String string)
    {
        return normalize(string, "[^0-9A-Z]");
    }
    
    /**
     * Return the given String, normalized for ease of comparison.
     * Currently the normalization comprises the following:
     *  <ol>
     *   <li>Trim leading and trailing whitespace</li>
     *   <li>Uppercase all letters</li>
     *   <li>Remove all characters which match<code>removeChars</code></li>
     *  </ol>
     */
    public static String normalize(String string, String removeChars)
    {
        if(string == null) { return null; }   //guard against bad parameters
        return StringUtils.trim(string).toUpperCase().replaceAll(removeChars, "");
    }
    
    public static String getPurposeAndContactMechType(String purpose)
    {
    	return purposeAndContactMechType.get(purpose);
    }
    
    public static String getPropertyFromPurpose(String purpose, List<TypifiedProperty> properties)
    {    	
    	if(properties!=null)
    	{
	    	for(TypifiedProperty prop : properties)
	    	{
	    		if(purposeTypePropertyTypeRelation.get(purpose).equals(prop.getPropertyType()))
	    		{
	    			return prop.getPropertyValueAsString();
	    		}
	    	}
    	}
    	else
    	{
    		log.warn("contact email list is null");
    	}
    	return "";
    }
    
    private static Map<String, Set<String>> initContactMechTypeAndPurposeType()
    {
    	Map<String, Set<String>> pm = new HashMap<String, Set<String>>();
    	
    	Set<String> purposes = new HashSet<String>();
    	purposes.add("PRIMARY_EMAIL");
    	purposes.add("OTHER_EMAIL");
    	purposes.add("OTHER_EMAIL_SEC");
    	pm.put("EMAIL_ADDRESS",new HashSet<String>(purposes));
    	purposes.clear();
    	
    	purposes.add("WEB_ADDRESS");
    	pm.put("PRIMARY_WEB_URL",new HashSet<String>(purposes));
    	purposes.clear();
    	
    	purposes.add("PRIMARY_PHONE");
    	purposes.add("PHONE_MOBILE");
    	purposes.add("PHONE_CAR");
    	purposes.add("PHONE_WORK");
    	purposes.add("PHONE_HOME");
    	purposes.add("PHONE_HOME_SEC");
    	purposes.add("FAX_NUMBER");
    	purposes.add("FAX_NUMBER_SEC");
    	pm.put("TELECOM_NUMBER",new HashSet<String>(purposes));
    	purposes.clear();
    	
    	purposes.add("GENERAL_LOCATION");
    	purposes.add("HOME_LOCATION");
    	purposes.add("OTHER_LOCATION");
    	pm.put("POSTAL_ADDRESS", new HashSet<String>(purposes));
    	purposes.clear();    	
    	return pm;
    }
    
    private static Map<String, String> initPurposeTypeAndContactMechType()
    {
    	Map<String, String> pm = new HashMap<String, String>();
    	
    	pm.put("PRIMARY_EMAIL","EMAIL_ADDRESS");
    	pm.put("OTHER_EMAIL","EMAIL_ADDRESS");
    	pm.put("OTHER_EMAIL_SEC","EMAIL_ADDRESS");
    	
    	pm.put("PRIMARY_WEB_URL","WEB_ADDRESS");
    	
    	pm.put("PRIMARY_PHONE","TELECOM_NUMBER");
    	pm.put("PHONE_MOBILE","TELECOM_NUMBER");
    	pm.put("PHONE_CAR","TELECOM_NUMBER");
    	pm.put("PHONE_WORK","TELECOM_NUMBER");
    	pm.put("PHONE_HOME","TELECOM_NUMBER");
    	pm.put("PHONE_HOME_SEC","TELECOM_NUMBER");
    	pm.put("FAX_NUMBER","TELECOM_NUMBER");
    	pm.put("FAX_NUMBER_SEC","TELECOM_NUMBER");
    	
    	pm.put("GENERAL_LOCATION","POSTAL_ADDRESS");
    	pm.put("HOME_LOCATION","POSTAL_ADDRESS");
    	pm.put("OTHER_LOCATION","POSTAL_ADDRESS");
    	
    	return pm;
    }
    private static Map<String, String> initPurposeTypePropertyTypeRelation()
    {
    	Map<String, String> pm = new HashMap<String, String>();
    	pm.put("PRIMARY_EMAIL", "Email1Address");
    	pm.put("OTHER_EMAIL", "OtherEmail2Address");
    	//pm.put("OTHER_EMAIL_SEC", "Email3Address");
    	pm.put("PRIMARY_WEB_URL", "WebPage");
    	return pm;
    }
}
